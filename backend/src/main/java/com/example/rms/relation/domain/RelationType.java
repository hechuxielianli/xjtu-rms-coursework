package com.example.rms.relation.domain;
import com.example.rms.shared.domain.*;
public enum RelationType {
    DEPENDS_ON, REFINES, DERIVED_FROM, CONFLICTS_WITH, DUPLICATES, RELATES_TO;
    public boolean symmetric() { return this==CONFLICTS_WITH || this==DUPLICATES || this==RELATES_TO; }
    public static RelationType parse(String raw) { try{return valueOf(raw);}catch(IllegalArgumentException|NullPointerException e){throw new RmsException(ErrorCode.INVALID_INPUT);} }
}
