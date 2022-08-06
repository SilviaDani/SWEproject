package com.sweproject.model;
import java.sql.Timestamp;
import java.time.LocalDateTime;

public class Date extends TimeRecord{
    private LocalDateTime dateTime;

    public Date(LocalDateTime dateTime) {
        this.dateTime = dateTime;
    }

    public Timestamp getDate() {
        return Timestamp.valueOf(dateTime);
    }
}
