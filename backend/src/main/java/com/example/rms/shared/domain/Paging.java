package com.example.rms.shared.domain;
public record Paging(int page,int size) {
    public Paging { if(page<0 || size<1 || size>100 || (long)page*size>Integer.MAX_VALUE)throw new RmsException(ErrorCode.INVALID_INPUT); }
    public int offset() { return page*size; }
}
