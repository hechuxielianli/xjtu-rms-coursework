package com.example.rms.shared.api;

import com.example.rms.shared.application.HttpWriteFailureObserver;
import com.example.rms.shared.domain.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** CON-002: MVC classification only. Authenticated real HTTP probes remain the end-to-end evidence. */
class ProtocolErrorRegressionTest {
    private MockMvc mvc;
    @BeforeEach void setup() {
        var errors=new ApiErrors(new ObjectMapper());
        mvc=MockMvcBuilders.standaloneSetup(new ProbeController()).setControllerAdvice(new GlobalErrorHandler(errors,mock(HttpWriteFailureObserver.class))).build();
    }
    @RestController static class ProbeController {
        record Body(String value) {}
        @GetMapping("/protocol/session") public String session() { return "ok"; }
        @PostMapping(path="/protocol/body",consumes="application/json") public Body body(@RequestBody Body value) { return value; }
        @GetMapping("/protocol/error/{code}") public void business(@PathVariable String code) { throw new RmsException(ErrorCode.valueOf(code)); }
        @GetMapping("/protocol/internal") public void internal() { throw new IllegalStateException("private SQL internal detail"); }
    }
    @Test void unsupportedMethodIsStableClientError()throws Exception {
        mvc.perform(post("/protocol/session")).andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("INVALID_INPUT")).andExpect(header().string("Cache-Control","no-store"));
    }
    @Test void unsupportedMediaTypeIsStableClientError()throws Exception {
        mvc.perform(post("/protocol/body").contentType(MediaType.TEXT_PLAIN).content("anything")).andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("INVALID_INPUT"));
    }
    @Test void malformedJsonRemains400AndValidJsonStillSucceeds()throws Exception {
        mvc.perform(post("/protocol/body").contentType(MediaType.APPLICATION_JSON).content("{" )).andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("INVALID_INPUT"));
        mvc.perform(post("/protocol/body").contentType(MediaType.APPLICATION_JSON).content("{\"value\":\"okay\"}")).andExpect(status().isOk()).andExpect(jsonPath("$.value").value("okay"));
    }
    @Test void authForbiddenConflictAndInternalErrorsRetainSeparateCodes()throws Exception {
        mvc.perform(get("/protocol/error/UNAUTHENTICATED")).andExpect(status().isUnauthorized()).andExpect(jsonPath("$.code").value("UNAUTHENTICATED"));
        mvc.perform(get("/protocol/error/FORBIDDEN")).andExpect(status().isForbidden()).andExpect(jsonPath("$.code").value("FORBIDDEN"));
        mvc.perform(get("/protocol/error/STATE_CONFLICT")).andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("STATE_CONFLICT"));
        mvc.perform(get("/protocol/internal")).andExpect(status().isInternalServerError()).andExpect(jsonPath("$.code").value("INTERNAL_ERROR")).andExpect(jsonPath("$.message").value("INTERNAL_ERROR")).andExpect(jsonPath("$.exception").doesNotExist()).andExpect(jsonPath("$.stackTrace").doesNotExist());
    }
}
