package com.example.rms.shared.application;
import java.util.List;
public record PageData<T>(List<T> items,int page,int size,long totalElements) {
    public PageData { items=List.copyOf(items); }
}
