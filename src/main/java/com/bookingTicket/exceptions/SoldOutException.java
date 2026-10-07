package com.bookingTicket.exceptions;

public class SoldOutException extends RuntimeException {
    public SoldOutException() {
        super("Sold Out");
    }
}
