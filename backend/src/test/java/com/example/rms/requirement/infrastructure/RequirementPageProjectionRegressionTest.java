package com.example.rms.requirement.infrastructure;

import com.example.rms.requirement.application.RequirementQuery;
import com.example.rms.requirement.infrastructure.persistence.*;
import com.example.rms.shared.domain.Paging;
import jakarta.persistence.*;
import java.lang.reflect.Field;
import java.time.Instant;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/** REG-QUERY-001: page assembly preserves fields/order and never invokes the per-detail tag query. */
class RequirementPageProjectionRegressionTest {
    private static final Instant TIME=Instant.parse("2026-10-04T00:00:00Z");
    private static void id(Object entity,String field,long id)throws Exception {
        Field key=entity.getClass().getDeclaredField(field);key.setAccessible(true);key.set(entity,id);
    }
    private static RequirementEntity requirement(long key)throws Exception {
        var row=RequirementEntity.draft("R-"+key,"Title "+key,"Description","SYSTEM","FUNCTIONAL","MEDIUM","Source","Reason","Acceptance",4,TIME);
        id(row,"requirementId",key);return row;
    }
    private static TagEntity tag(long key)throws Exception {
        var row=TagEntity.create("Tag "+key,"Description",4,TIME);id(row,"tagId",key);return row;
    }
    @ParameterizedTest @ValueSource(ints={1,20})
    void pageUsesOneBatchRegardlessOfRowCountAndPreservesProjection(int size)throws Exception {
        EntityManager em=mock(EntityManager.class);
        @SuppressWarnings("unchecked") TypedQuery<RequirementEntity> rows=mock(TypedQuery.class,RETURNS_SELF);
        @SuppressWarnings("unchecked") TypedQuery<Long> count=mock(TypedQuery.class,RETURNS_SELF);
        @SuppressWarnings("unchecked") TypedQuery<Object[]> tags=mock(TypedQuery.class,RETURNS_SELF);
        when(em.createQuery(anyString(),eq(RequirementEntity.class))).thenReturn(rows);
        when(em.createQuery(anyString(),eq(Long.class))).thenReturn(count);
        when(em.createQuery(anyString(),eq(Object[].class))).thenReturn(tags);
        List<RequirementEntity> page=new ArrayList<>();
        for(int index=0;index<size;index++)page.add(requirement(10+index));
        when(rows.getResultList()).thenReturn(page);
        when(count.getSingleResult()).thenReturn(42L);
        List<Object[]> links=new ArrayList<>();
        links.add(new Object[]{10L,tag(7)});links.add(new Object[]{10L,tag(9)});
        if(size>1)links.add(new Object[]{10L+size-1,tag(3)});
        when(tags.getResultList()).thenReturn(links);

        var result=new JpaRequirementStore(em).list(new RequirementQuery(new Paging(2,size),null,null,null,null,null,null));
        assertEquals(size,result.items().size());assertEquals(42,result.totalElements());
        assertEquals(2,result.page());assertEquals(size,result.size());
        assertEquals(page.stream().map(RequirementEntity::getRequirementId).toList(),result.items().stream().map(r->r.requirementId()).toList());
        assertEquals(List.of(7L,9L),result.items().getFirst().tags().stream().map(t->t.tagId()).toList());
        for(int index=0;index<size;index++) {
            var projected=result.items().get(index);
            assertEquals("Title "+(10+index),projected.title());assertEquals("Description",projected.description());
            assertEquals("Source",projected.source());assertEquals("Reason",projected.rationale());assertEquals("Acceptance",projected.acceptanceCriteria());
            assertEquals("DRAFT",projected.status());assertEquals(4,projected.creatorId());assertNull(projected.currentVersionId());
            assertEquals(TIME,projected.createdAt());assertEquals(0,projected.lockVersion());
            if(index>0 && index<size-1)assertTrue(projected.tags().isEmpty());
        }
        if(size>1)assertEquals(List.of(3L),result.items().getLast().tags().stream().map(t->t.tagId()).toList());
        verify(tags).setParameter("ids",page.stream().map(RequirementEntity::getRequirementId).toList());
        verify(tags,times(1)).getResultList();
        verify(em,never()).createQuery(anyString(),eq(TagEntity.class));
        verify(rows).setFirstResult(2*size);verify(rows).setMaxResults(size);
    }
    @Test void emptyPageSkipsBatchAndRetainsTotal() {
        EntityManager em=mock(EntityManager.class);
        @SuppressWarnings("unchecked") TypedQuery<RequirementEntity> rows=mock(TypedQuery.class,RETURNS_SELF);
        @SuppressWarnings("unchecked") TypedQuery<Long> count=mock(TypedQuery.class,RETURNS_SELF);
        when(em.createQuery(anyString(),eq(RequirementEntity.class))).thenReturn(rows);
        when(em.createQuery(anyString(),eq(Long.class))).thenReturn(count);
        when(rows.getResultList()).thenReturn(List.of());when(count.getSingleResult()).thenReturn(8L);
        var result=new JpaRequirementStore(em).list(new RequirementQuery(new Paging(1,20),null,null,null,null,null,null));
        assertTrue(result.items().isEmpty());assertEquals(8,result.totalElements());
        verify(em,never()).createQuery(anyString(),eq(Object[].class));
        verify(em,never()).createQuery(anyString(),eq(TagEntity.class));
    }
    @Test void singleDetailStillUsesItsExistingTagProjection()throws Exception {
        EntityManager em=mock(EntityManager.class);
        @SuppressWarnings("unchecked") TypedQuery<TagEntity> tags=mock(TypedQuery.class,RETURNS_SELF);
        when(em.find(RequirementEntity.class,10L)).thenReturn(requirement(10));
        when(em.createQuery(anyString(),eq(TagEntity.class))).thenReturn(tags);
        when(tags.getResultList()).thenReturn(List.of(tag(7),tag(9)));
        var detail=new JpaRequirementStore(em).read(10);
        assertEquals("Title 10",detail.title());
        assertEquals(List.of(7L,9L),detail.tags().stream().map(t->t.tagId()).toList());
        verify(tags).setParameter("id",10L);
        verify(em,never()).createQuery(anyString(),eq(Object[].class));
    }
}
