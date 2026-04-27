package com.example.demo.exception;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import java.util.Map;

@ControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<?> handleDuplicateEntry(DataIntegrityViolationException ex) {
        // Instead of a 500 crash, we return a 400 Bad Request with a message
        return ResponseEntity.badRequest().body(Map.of(
                "error", "Conflict",
                "message", "This phone number is already in use."
        ));
    }
}