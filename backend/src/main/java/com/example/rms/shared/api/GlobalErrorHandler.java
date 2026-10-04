package com.example.rms.shared.api;
import com.example.rms.shared.domain.*;
import com.example.rms.shared.application.HttpWriteFailureObserver;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
@RestControllerAdvice
public class GlobalErrorHandler {
    private final ApiErrors errors;
    private final HttpWriteFailureObserver failures;
    public GlobalErrorHandler(ApiErrors errors,HttpWriteFailureObserver failures) { this.errors=errors;this.failures=failures; }
    private ResponseEntity<ApiError> response(HttpServletRequest request,ErrorCode code,Long revision) { return ResponseEntity.status(code.status()).header("Cache-Control","no-store").body(errors.body(request,code,revision)); }
    @ExceptionHandler(RmsException.class) ResponseEntity<ApiError> rms(RmsException e,HttpServletRequest r) { return response(r,e.code(),e.currentLockVersion()); }
    @ExceptionHandler({HttpMessageNotReadableException.class,MethodArgumentNotValidException.class,MethodArgumentTypeMismatchException.class,org.springframework.web.bind.MissingServletRequestParameterException.class,jakarta.validation.ConstraintViolationException.class,org.springframework.web.HttpRequestMethodNotSupportedException.class,org.springframework.web.HttpMediaTypeNotSupportedException.class})
    ResponseEntity<ApiError> input(Exception e,HttpServletRequest r) { failures.inputFailed(r.getMethod(),r.getRequestURI());return response(r,ErrorCode.INVALID_INPUT,null); }
    @ExceptionHandler(ObjectOptimisticLockingFailureException.class) ResponseEntity<ApiError> optimistic(Exception e,HttpServletRequest r) { return response(r,ErrorCode.LOCK_VERSION_CONFLICT,null); }
    @ExceptionHandler(DataIntegrityViolationException.class) ResponseEntity<ApiError> integrity(Exception e,HttpServletRequest r) { return response(r,ErrorCode.DUPLICATE,null); }
    @ExceptionHandler({org.springframework.web.servlet.resource.NoResourceFoundException.class,org.springframework.web.servlet.NoHandlerFoundException.class}) ResponseEntity<ApiError> missing(Exception e,HttpServletRequest r) { return response(r,ErrorCode.NOT_FOUND,null); }
    @ExceptionHandler(Exception.class) ResponseEntity<ApiError> internal(Exception e,HttpServletRequest r) { return response(r,ErrorCode.INTERNAL_ERROR,null); }
}
