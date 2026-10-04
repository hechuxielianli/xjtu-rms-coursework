package com.example.rms.shared.domain;
public final class InputPolicy {
    private InputPolicy() {}
    public static String nonBlank(String raw,int maxCodePoints) {
        if(raw==null || raw.isBlank() || raw.codePointCount(0,raw.length())>maxCodePoints) throw new RmsException(ErrorCode.INVALID_INPUT);
        return raw;
    }
    public static long decimalId(String raw) { return decimal(raw,1); }
    public static long lockVersion(String raw) { return decimal(raw,0); }
    public static String nullableText(String raw,int max) { if(raw!=null && raw.codePointCount(0,raw.length())>max)throw new RmsException(ErrorCode.INVALID_INPUT);return raw; }
    public static String oneOf(String raw,String... choices) { for(String choice:choices)if(choice.equals(raw))return raw;throw new RmsException(ErrorCode.INVALID_INPUT); }
    public static void expected(long expected,long current) { if(expected!=current)throw new RmsException(ErrorCode.LOCK_VERSION_CONFLICT,current); }
    private static long decimal(String raw,long minimum) {
        if(raw==null || !raw.matches("0|[1-9][0-9]{0,18}")) throw new RmsException(ErrorCode.INVALID_INPUT);
        try { long value=Long.parseLong(raw);if(value<minimum) throw new RmsException(ErrorCode.INVALID_INPUT);return value; }
        catch(NumberFormatException e) { throw new RmsException(ErrorCode.INVALID_INPUT); }
    }
}
