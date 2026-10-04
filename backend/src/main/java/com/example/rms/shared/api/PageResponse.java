package com.example.rms.shared.api;
import com.example.rms.shared.application.PageData;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.core.JsonGenerator;
import java.io.IOException;
import java.util.List;
import java.util.function.Function;
/** Counts remain numeric; global signed-BIGINT identity serialization does not apply here. */
public record PageResponse<T>(List<T> items,int page,int size,@JsonSerialize(using=CountSerializer.class) long totalElements) {
    public static final class CountSerializer extends JsonSerializer<Long> { @Override public void serialize(Long value,JsonGenerator generator,SerializerProvider serializers)throws IOException { generator.writeNumber(value); } }
    public static <A,B> PageResponse<B> from(PageData<A> data,Function<A,B> convert) { return new PageResponse<>(data.items().stream().map(convert).toList(),data.page(),data.size(),data.totalElements()); }
}
