package com.example.similarproducts.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.reactive.function.client.WebClientRequestException;
import org.springframework.web.reactive.function.client.WebClientResponseException;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ProductNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public void handleNotFound() {
        // 404 sin cuerpo, según el contrato
    }
    
    @ExceptionHandler({WebClientRequestException.class, WebClientResponseException.class})
    @ResponseStatus(HttpStatus.BAD_GATEWAY)
    public void handleUpstreamFailure() {
    }
}
