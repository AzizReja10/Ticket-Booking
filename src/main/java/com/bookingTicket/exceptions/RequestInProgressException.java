package com.bookingTicket.exceptions;

public class RequestInProgressException extends RuntimeException {
    public RequestInProgressException() {
        super("Another request for this booking is already in progress");
    }
}
