package com.example.rms.shared.domain;
/** Eight immutable values; shared vocabulary carries no owner entity or web DTO. */
public record ContentSnapshot(String title,String description,String level,String kind,String priority,String source,String rationale,String acceptanceCriteria) {
    public void requireComplete() {
        for(String value:new String[]{title,description,level,kind,priority,source,rationale,acceptanceCriteria})if(value==null || value.isBlank())throw new RmsException(ErrorCode.INCOMPLETE_CONTENT);
        InputPolicy.nonBlank(title,200);InputPolicy.oneOf(level,"BUSINESS","USER","SYSTEM");InputPolicy.oneOf(kind,"FUNCTIONAL","QUALITY","CONSTRAINT");InputPolicy.oneOf(priority,"LOW","MEDIUM","HIGH","CRITICAL");
    }
}
