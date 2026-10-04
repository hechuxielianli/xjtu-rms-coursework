package com.example.rms.requirement.infrastructure.persistence;
import java.io.Serializable;
import java.util.Objects;
public class RequirementTagId implements Serializable {
    private static final long serialVersionUID=1L;
    public Long requirementId;
    public Long tagId;
    public RequirementTagId() {}
    public RequirementTagId(Long requirementId,Long tagId) {this.requirementId=requirementId; this.tagId=tagId;}
    @Override public boolean equals(Object o) { return o instanceof RequirementTagId other && Objects.equals(requirementId,other.requirementId) && Objects.equals(tagId,other.tagId); }
    @Override public int hashCode() { return Objects.hash(requirementId,tagId); }
}
