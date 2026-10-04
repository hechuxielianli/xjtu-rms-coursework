package com.example.rms.shared.domain;
import java.nio.charset.StandardCharsets;
/** Shared exact original-value UTF-8 BCrypt boundary, not a password strength policy. */
public final class PasswordInputPolicy {
    private PasswordInputPolicy() {}
    public static String validate(String raw) {
        if(raw==null || raw.isBlank()) throw new RmsException(ErrorCode.INVALID_INPUT);
        int bytes=raw.getBytes(StandardCharsets.UTF_8).length;
        if(bytes<1 || bytes>72) throw new RmsException(ErrorCode.INVALID_INPUT);
        return raw;
    }
}
