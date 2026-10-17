package com.bookingTicket.exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(SoldOutException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public String soldOut(SoldOutException e) { return e.getMessage(); }

    @ExceptionHandler(AlreadyBookedException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public String alreadyBooked(AlreadyBookedException e) { return e.getMessage(); }

    @ExceptionHandler(RequestInProgressException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public String inProgress(RequestInProgressException e) { return e.getMessage(); }

    @ExceptionHandler(HoldExpiredException.class)
    @ResponseStatus(HttpStatus.GONE)
    public String holdExpired(HoldExpiredException e) { return e.getMessage(); }

    @ExceptionHandler(EventNotReadyException.class)
    @ResponseStatus(HttpStatus.SERVICE_UNAVAILABLE)
    public String notReady(EventNotReadyException e) { return e.getMessage(); }
}
