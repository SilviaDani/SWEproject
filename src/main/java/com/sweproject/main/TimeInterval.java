package com.sweproject.main;

public class TimeInterval extends TimeRecord{
    private Date initialDate;
    private Date finalDate;

    public TimeInterval(Date initialDate, Date finalDate) {
        this.initialDate = initialDate;
        this.finalDate = finalDate;
    }
}
