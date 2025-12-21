package com.ikn.ums.googlemeet.exception;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import lombok.extern.slf4j.Slf4j;

@ControllerAdvice
@Slf4j
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    // Custom Exceptions

    @ExceptionHandler(EntityNotFoundException.class)
    public ResponseEntity<String> handleEntityNotFoundException(EntityNotFoundException ex) {
        log.error("[{}] {} | CAUSE: {}", 
                ex.getErrorCode(),
                ex.getErrorMessage(),
                ex.getMessage(),
                ex);

        return new ResponseEntity<>(ex.getErrorCode(), HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(EmptyInputException.class)
    public ResponseEntity<String> handleEmptyInput(EmptyInputException ex) {
        log.error("[{}] {} | CAUSE: {}", 
                ex.getErrorCode(),
                ex.getErrorMessage(),
                ex.getMessage(),
                ex);

        return new ResponseEntity<>(ex.getErrorCode(), HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<String> handleBusinessException(BusinessException ex) {
        log.error("[{}] {} | CAUSE: {}", 
                ex.getErrorCode(),
                ex.getErrorMessage(),
                ex.getMessage(),
                ex);

        return new ResponseEntity<>(ex.getErrorCode(), HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @ExceptionHandler(ControllerException.class)
    public ResponseEntity<String> handleControllerException(ControllerException ex) {
        log.error("[{}] {} | CAUSE: {}", 
                ex.getErrorCode(),
                ex.getErrorMessage(),
                ex.getMessage(),
                ex);

        return new ResponseEntity<>(ex.getErrorCode(), HttpStatus.INTERNAL_SERVER_ERROR);
    }


    protected ResponseEntity<Object> handleHttpRequestMethodNotSupported(
            HttpRequestMethodNotSupportedException ex,
            HttpHeaders headers,
            HttpStatus status,
            WebRequest request) {

        log.error("[ERR_METHOD_NOT_ALLOWED] HTTP method not supported | CAUSE: {}", 
                ex.getMessage(),
                ex);

        return new ResponseEntity<>("ERR_METHOD_NOT_ALLOWED", HttpStatus.METHOD_NOT_ALLOWED);
    }

    // Global Fallback Exception

    @ExceptionHandler(Exception.class)
    public ResponseEntity<String> handleGlobalException(Exception ex) {
        log.error("[ERR_INTERNAL_SERVER_ERROR] Unhandled exception | CAUSE: {}", 
                ex.getMessage(),
                ex);

        return new ResponseEntity<>("ERR_INTERNAL_SERVER_ERROR", HttpStatus.INTERNAL_SERVER_ERROR);
    }
}
