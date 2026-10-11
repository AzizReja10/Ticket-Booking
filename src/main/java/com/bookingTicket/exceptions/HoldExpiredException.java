package com.bookingTicket.exceptions;

public class HoldExpiredException extends RuntimeException {
    public HoldExpiredException() {
        super("hold not found or expired");
    }
}
