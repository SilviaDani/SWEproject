package com.sweproject.model;

import com.sweproject.dao.ObservationDAO;
import com.sweproject.model.Observation;
import com.sweproject.model.Subject;
import com.sweproject.model.TimeRecord;
import com.sweproject.model.Type;

import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.ArrayList;

public class Notifier {
    private Subject subject;
    private ObservationDAO observationDAO;

    public Notifier(String fiscalCode, String name, String surname) {
        subject = new Subject(fiscalCode, name,surname);
        observationDAO = new ObservationDAO();
    }

    public void addObservation(ArrayList<String> cluster, Type eventType, LocalDateTime startDate, LocalDateTime endDate) throws SQLException {
        /* Observation observation = new Observation(cluster, type, timeRecord);
        subject.addObservation(observation);*/
        observationDAO.insertObservation(cluster, eventType, startDate, endDate);
    }


    public void getProbabilityOfBeingInfected(){//FIXME
        subject.getProbabilityOfBeingInfected();
    }

    //for testing
    public Subject getSubject() {
        return subject;
    }

    public String getName() {
        return subject.getName();
    }
    public String getFiscalCode(){
        return subject.getFiscalCode();
    }
}
