package com.maxcopias.service;

public class ExcepcionAlmacenamientoArchivos extends RuntimeException {

    public ExcepcionAlmacenamientoArchivos(String message) {
        super(message);
    }

    public ExcepcionAlmacenamientoArchivos(String message, Throwable cause) {
        super(message, cause);
    }
}

