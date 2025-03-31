package com.modulith.infraagentteam.infra.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(NoPrException.class)
    public void handleNoPrException(NoPrException e) {
        log.error("No PR found in the payload");
    }
}
