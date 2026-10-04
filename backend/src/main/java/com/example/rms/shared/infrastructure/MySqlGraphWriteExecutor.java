package com.example.rms.shared.infrastructure;
import com.example.rms.shared.application.*;
import com.example.rms.shared.domain.*;
import com.zaxxer.hikari.HikariDataSource;
import jakarta.persistence.EntityManager;
import java.sql.*;
import org.springframework.beans.factory.annotation.*;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.*;
@Component
public final class MySqlGraphWriteExecutor implements GraphWriteExecutor {
    public static final String LOCK_NAME="rms_requirement_graph";
    @FunctionalInterface public interface LockRelease { Number release(Connection c)throws SQLException; }
    private final HikariDataSource pool;private final PinnedDataSource pinned;private final TransactionTemplate transaction;private final EntityManager em;private final LockRelease release;
    @Autowired public MySqlGraphWriteExecutor(@Qualifier("rmsGraphPool") HikariDataSource pool,PinnedDataSource pinned,PlatformTransactionManager manager,EntityManager em) { this(pool,pinned,manager,em,c->query(c,"SELECT RELEASE_LOCK(?)",LOCK_NAME)); }
    /** Infrastructure seam for real cleanup-fault developer checks; never exposed through HTTP. */
    public MySqlGraphWriteExecutor(HikariDataSource pool,PinnedDataSource pinned,PlatformTransactionManager manager,EntityManager em,LockRelease release) { if(!(manager instanceof JpaTransactionManager jpa) || jpa.getDataSource()!=pinned)throw new IllegalStateException("GRAPH_TRANSACTION_DATASOURCE_MISMATCH");this.pool=pool;this.pinned=pinned;this.em=em;this.release=release;transaction=new TransactionTemplate(manager);transaction.setName("RMS_GRAPH_WRITE");transaction.setTimeout(30); }
    @Override public void requireActive() { if(!pinned.isPinned() || !TransactionSynchronizationManager.isActualTransactionActive())throw new IllegalStateException("GRAPH_WRITE_REQUIRES_EXECUTOR"); }
    @Override public <T>T execute(Work<T> work) { return execute(new GraphTrace(),work); }
    public <T>T execute(GraphTrace trace,Work<T> work) {
        if(TransactionSynchronizationManager.isActualTransactionActive() || pinned.isPinned())throw new IllegalStateException("GRAPH_EXECUTOR_REQUIRES_NO_OUTER_TRANSACTION");
        Connection raw;try{raw=pool.getConnection();}catch(SQLException e){throw new RmsException(ErrorCode.INTERNAL_ERROR);}
        boolean acquired=false;Throwable primary=null;
        try {
            pinned.pin(raw,trace);trace.graphConnectionId=query(raw,"SELECT CONNECTION_ID()").longValue();trace.event("PHYSICAL_CONNECTION_PINNED");trace.event("ATTEMPTING_GET_LOCK");long started=System.nanoTime();Number value=query(raw,"SELECT GET_LOCK(?,10)",LOCK_NAME);trace.getLockElapsedNanos=System.nanoTime()-started;trace.getLockResult=value==null?null:value.intValue();
            if(value==null)throw new RmsException(ErrorCode.INTERNAL_ERROR);if(value.intValue()!=1)throw new RmsException(ErrorCode.GRAPH_BUSY);acquired=true;trace.event("LOCK_ACQUIRED");
            T result=transaction.execute(status->{trace.event("TRANSACTION_CALLBACK_ENTERED");trace.jpaConnectionId=((Number)em.createNativeQuery("SELECT CONNECTION_ID()").getSingleResult()).longValue();if(trace.graphConnectionId!=trace.jpaConnectionId)throw new IllegalStateException("GRAPH_CONNECTION_MISMATCH");try{T answer=work.run();em.flush();trace.event("JPA_FLUSH_COMPLETED");return answer;}catch(Exception e){throw e instanceof RuntimeException runtime?runtime:new GraphWorkException(e);}});
            trace.event("SPRING_TRANSACTION_COMPLETION_AND_CLEANUP_RETURNED");return result;
        } catch(RuntimeException|Error|SQLException e) {
            boolean committed=committed(trace);primary=committed?new GraphOutcomeException(true,e):e;trace.event("BUSINESS_OR_TRANSACTION_FAILURE");if(primary instanceof RuntimeException r)throw r;if(primary instanceof Error error)throw error;throw new RmsException(ErrorCode.INTERNAL_ERROR);
        } finally {
            Throwable cleanup=null;
            if(acquired)try{Number value=release.release(raw);trace.releaseResult=value==null?null:value.intValue();if(value==null || value.intValue()!=1)throw new SQLException("GRAPH_LOCK_RELEASE_NOT_OWNED");trace.event("RELEASE_LOCK_COMPLETED");}catch(SQLException|RuntimeException e){cleanup=e;trace.event("RELEASE_LOCK_FAILURE");evict(raw,trace,e);}
            pinned.unpin();
            try{raw.close();trace.event(trace.evicted?"EVICTED_HANDLE_CLOSED_NOT_NORMAL_POOL_RETURN":"POOL_RETURNED");}catch(SQLException|RuntimeException e){if(cleanup==null)cleanup=e;else cleanup.addSuppressed(e);if(!trace.evicted)evict(raw,trace,e);}
            if(cleanup!=null){var outcome=new GraphOutcomeException(committed(trace),cleanup);if(primary!=null)primary.addSuppressed(outcome);else throw outcome;}
        }
    }
    private static boolean committed(GraphTrace trace) { return trace.events().stream().anyMatch(e->"COMMIT_COMPLETED".equals(e.get("name"))); }
    private void evict(Connection raw,GraphTrace trace,Throwable failure) {
        trace.evicted=true;Connection physical=null;try{physical=raw.unwrap(Connection.class);}catch(SQLException e){failure.addSuppressed(e);}
        try{pool.evictConnection(raw);trace.event("CONNECTION_EVICTED_FROM_POOL");}catch(RuntimeException e){failure.addSuppressed(e);}
        if(physical!=null)try{physical.abort(Runnable::run);trace.event("PHYSICAL_CONNECTION_ABORTED");}catch(SQLException e){failure.addSuppressed(e);}
        org.slf4j.LoggerFactory.getLogger(MySqlGraphWriteExecutor.class).warn("Graph connection cleanup failed; connection {} evicted, committed={}",trace.graphConnectionId,committed(trace));
    }
    public static Number query(Connection c,String sql,Object...args)throws SQLException { try(var statement=c.prepareStatement(sql)){for(int i=0;i<args.length;i++)statement.setObject(i+1,args[i]);try(var rows=statement.executeQuery()){if(!rows.next())throw new SQLException("EXPECTED_SCALAR");return (Number)rows.getObject(1);}} }
    public static final class GraphWorkException extends RuntimeException { GraphWorkException(Exception cause){super("GRAPH_WORK_FAILED",cause);} }
}
