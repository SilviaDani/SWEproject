package com.sweproject.model;

import java.sql.Timestamp;

public class TimeInterval extends TimeRecord{
    private Date initialDate;
    private Date finalDate;

    public TimeInterval(Date initialDate, Date finalDate) {
        this.initialDate = initialDate;
        this.finalDate = finalDate;
    }

    public Timestamp getInitialDate() {
        return initialDate.getDate();
    }

    public Timestamp getFinalDate() {
       return finalDate.getDate();
    }
}
