package com.example.rms.requirement.infrastructure.persistence;

import jakarta.persistence.*;
import java.time.Instant;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/** Frozen Baseline 1.3 persistence projection; IDs cross modules as values, never entities. */
@Entity
@Table(name="Tag")
public class TagEntity {
    protected TagEntity() {}
    @Id
    @GeneratedValue(strategy=GenerationType.IDENTITY)
    @Column(name="tagId", nullable=false, updatable=false)
    private Long tagId;
    @Column(name="name", nullable=false, length=50)
    private String name;
    @Column(name="description", nullable=true, length=500)
    private String description;
    @Column(name="createdBy", nullable=false, updatable=false)
    private Long createdBy;
    @JdbcTypeCode(SqlTypes.TIMESTAMP)
    @Column(name="createdAt", nullable=false, columnDefinition="datetime(6)", updatable=false)
    private Instant createdAt;
    public Long getTagId() { return tagId; }
    public String getName() { return name; }
    public String getDescription() { return description; }
    public Long getCreatedBy() { return createdBy; }
    public Instant getCreatedAt() { return createdAt; }
    public static TagEntity create(String name,String description,long actorId,Instant now) { TagEntity t=new TagEntity();t.name=name;t.description=description;t.createdBy=actorId;t.createdAt=now;return t; }
    public void edit(String name,String description) { this.name=name;this.description=description; }
}
