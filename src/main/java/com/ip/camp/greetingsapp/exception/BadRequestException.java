package com.ip.camp.greetingsapp.exception;

public class BadRequestException extends RuntimeException {

    public BadRequestException(final String message) {
        super(message);
    }
}
