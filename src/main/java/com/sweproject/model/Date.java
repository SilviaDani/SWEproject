package com.sweproject.model;
import java.time.LocalDateTime;

public class Date extends TimeRecord{
    private LocalDateTime dateTime;

    public Date(LocalDateTime dateTime) {
        this.dateTime = dateTime;
    }
}
