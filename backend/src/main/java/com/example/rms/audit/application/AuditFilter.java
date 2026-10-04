package com.example.rms.audit.application;
import com.example.rms.shared.domain.*;
import java.time.*;
import java.time.format.DateTimeParseException;
/** The frozen query has only optional UTC range and exact action, never arbitrary SQL/sort. */
public record AuditFilter(Instant from,Instant to,String action) {
    public static AuditFilter parse(String from,String to,String action) {
        Instant lower=time(from),upper=time(to);
        if(lower!=null && upper!=null && lower.isAfter(upper))throw new RmsException(ErrorCode.INVALID_INPUT);
        return new AuditFilter(lower,upper,InputPolicy.nullableText(action,64));
    }
    private static Instant time(String value) {
        if(value==null)return null;
        try { return OffsetDateTime.parse(value).toInstant(); }
        catch(DateTimeParseException e) { throw new RmsException(ErrorCode.INVALID_INPUT); }
    }
}
