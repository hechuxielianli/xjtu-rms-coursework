package com.example.rms.shared.domain;
/** Carries a safe code only; never incorporates request values, credentials or SQL messages. */
public final class RmsException extends RuntimeException {
    private final ErrorCode code;
    private final Long currentLockVersion;
    public RmsException(ErrorCode code) { this(code,null); }
    public RmsException(ErrorCode code,Long currentLockVersion) { super(code.name());this.code=code;this.currentLockVersion=currentLockVersion; }
    public ErrorCode code() { return code; }
    public Long currentLockVersion() { return currentLockVersion; }
}
