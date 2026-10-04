package com.example.rms.shared.api;
import com.example.rms.shared.domain.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.*;
import java.io.IOException;
import java.util.UUID;
import org.springframework.stereotype.Component;
@Component
public class ApiErrors {
    public static final String CORRELATION="rms.correlationId";
    private final ObjectMapper mapper;
    public ApiErrors(ObjectMapper mapper) { this.mapper=mapper; }
    public ApiError body(HttpServletRequest request,ErrorCode code,Long revision) {
        Object existing=request.getAttribute(CORRELATION);
        String id=existing instanceof String value?value:UUID.randomUUID().toString();
        return new ApiError(code.name(),code.name(),id,revision==null?null:Long.toString(revision));
    }
    public void write(HttpServletRequest request,HttpServletResponse response,ErrorCode code)throws IOException {
        response.setStatus(code.status());response.setContentType("application/json");response.setCharacterEncoding("UTF-8");response.setHeader("Cache-Control","no-store");
        mapper.writeValue(response.getOutputStream(),body(request,code,null));
    }
}
