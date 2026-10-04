package com.example.rms.shared.infrastructure;
import com.fasterxml.jackson.core.*;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import java.io.IOException;
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
import org.springframework.context.annotation.*;
@Configuration
public class JsonConfiguration {
    @Bean Jackson2ObjectMapperBuilderCustomizer strictJson() { return builder -> {
        builder.featuresToEnable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES,DeserializationFeature.FAIL_ON_NULL_FOR_PRIMITIVES);
        builder.serializerByType(Long.class,ToStringSerializer.instance);builder.serializerByType(Long.TYPE,ToStringSerializer.instance);
        builder.deserializerByType(String.class,new JsonDeserializer<String>() {
            @Override public String deserialize(JsonParser p,DeserializationContext ctx)throws IOException {
                if(!p.hasToken(JsonToken.VALUE_STRING))throw JsonMappingException.from(p,"String required");return p.getText();
            }
        });
        JsonDeserializer<Long> decimalString=new JsonDeserializer<>() {
            @Override public Long deserialize(JsonParser p,DeserializationContext ctx)throws IOException {
                if(!p.hasToken(JsonToken.VALUE_STRING)) throw JsonMappingException.from(p,"Decimal string required");
                String value=p.getText();if(!value.matches("0|[1-9][0-9]{0,18}"))throw JsonMappingException.from(p,"Decimal string required");
                try { return Long.parseLong(value); } catch(NumberFormatException e) { throw JsonMappingException.from(p,"Decimal string outside signed BIGINT range"); }
            }
        };
        builder.deserializerByType(Long.class,decimalString);builder.deserializerByType(Long.TYPE,decimalString);
    }; }
}
