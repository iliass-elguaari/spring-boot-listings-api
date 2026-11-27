package com.example.listings;

import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;

// Every bad request gets the same shape, one message per problem:
// {"errors": {"monthlyRent": "monthlyRent cannot be negative"}}
@RestControllerAdvice
public class ApiErrorHandler {

    private static ResponseEntity<Map<String, Map<String, String>>> badRequest(Map<String, String> errors) {
        return ResponseEntity.badRequest().body(Map.of("errors", errors));
    }

    // Invalid fields in a JSON body (@Valid on POST and PUT)
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Map<String, String>>> handleInvalidBody(MethodArgumentNotValidException e) {
        Map<String, String> errors = new LinkedHashMap<>();
        for (FieldError fe : e.getBindingResult().getFieldErrors()) {
            errors.putIfAbsent(fe.getField(), fe.getDefaultMessage());
        }
        return badRequest(errors);
    }

    // Invalid query parameters, e.g. ?maxRent=-5
    @ExceptionHandler(HandlerMethodValidationException.class)
    public ResponseEntity<Map<String, Map<String, String>>> handleInvalidParams(HandlerMethodValidationException e) {
        Map<String, String> errors = new LinkedHashMap<>();
        e.getParameterValidationResults().forEach(result -> {
            String name = result.getMethodParameter().getParameterName();
            result.getResolvableErrors().forEach(err -> errors.putIfAbsent(name, err.getDefaultMessage()));
        });
        return badRequest(errors);
    }

    // Body that isn't valid JSON, or has the wrong type in a field (e.g. "monthlyRent": "cheap")
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Map<String, Map<String, String>>> handleUnreadableBody(HttpMessageNotReadableException e) {
        return badRequest(Map.of("body", "request body must be valid JSON with address, monthlyRent and bedrooms"));
    }
}
