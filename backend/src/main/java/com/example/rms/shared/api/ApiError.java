package com.example.rms.shared.api;
import com.fasterxml.jackson.annotation.JsonInclude;
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ApiError(String code,String message,String correlationId,String currentLockVersion) {}
