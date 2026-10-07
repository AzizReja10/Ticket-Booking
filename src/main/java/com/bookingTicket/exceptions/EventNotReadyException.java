package com.bookingTicket.exceptions;

public class EventNotReadyException extends RuntimeException{

    public EventNotReadyException()
    {
        super("Event stock not initialised");
    }
}
