package com.bookingTicket.exceptions;

public class AlreadyBookedException extends RuntimeException{
    public AlreadyBookedException()
    {
        super("user already booked this event");
    }
}
