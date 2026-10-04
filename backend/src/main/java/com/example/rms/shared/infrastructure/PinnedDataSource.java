package com.example.rms.shared.infrastructure;
import com.example.rms.shared.application.GraphTrace;

import com.zaxxer.hikari.HikariDataSource;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import org.springframework.jdbc.datasource.AbstractDataSource;

/**
 * Transaction-scoped JPA handles cannot return the leased physical connection.
 * The executor alone owns lease close/eviction after transaction completion.
 */
public final class PinnedDataSource extends AbstractDataSource {
    private record Lease(Connection raw, Connection jpaHandle) { }
    private final HikariDataSource pool;
    private final ThreadLocal<Lease> lease = new ThreadLocal<>();

    public PinnedDataSource(HikariDataSource pool) { this.pool = pool; }

    public void pin(Connection raw, GraphTrace trace) {
        if (lease.get() != null) throw new IllegalStateException("NESTED_GRAPH_LEASE");
        Connection handle = (Connection) Proxy.newProxyInstance(Connection.class.getClassLoader(),
            new Class<?>[]{Connection.class}, (proxy, method, args) -> {
                if (method.getDeclaringClass() == Object.class) {
                    return switch (method.getName()) {
                        case "equals" -> proxy == args[0];
                        case "hashCode" -> System.identityHashCode(proxy);
                        case "toString" -> "PinnedJPAConnectionHandle";
                        default -> method.invoke(raw, args);
                    };
                }
                if (method.getName().equals("close")) {
                    trace.event("JPA_HANDLE_CLOSE_SUPPRESSED");
                    return null;
                }
                try {
                    Object result = method.invoke(raw, args);
                    if (method.getName().equals("commit")) trace.event("COMMIT_COMPLETED");
                    if (method.getName().equals("rollback")) trace.event("ROLLBACK_COMPLETED");
                    return result;
                } catch (InvocationTargetException e) {
                    throw e.getCause();
                }
            });
        lease.set(new Lease(raw, handle));
    }

    public boolean isPinned() { return lease.get() != null; }
    public void unpin() { lease.remove(); }

    @Override public Connection getConnection() throws SQLException {
        Lease current = lease.get();
        return current == null ? pool.getConnection() : current.jpaHandle();
    }
    @Override public <T>T unwrap(Class<T> type)throws SQLException { return type.isInstance(this)?type.cast(this):pool.unwrap(type); }
    @Override public boolean isWrapperFor(Class<?> type)throws SQLException { return type.isInstance(this) || pool.isWrapperFor(type); }
    @Override public Connection getConnection(String username, String password) throws SQLException {
        throw new SQLFeatureNotSupportedException("Use configured trusted DataSource credentials");
    }
}
