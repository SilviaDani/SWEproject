package com.sweproject.main;

import com.sweproject.controller.Tracer;
import com.sweproject.model.*;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.*;

class TracerTest {

    Tracer tracer;

    @BeforeEach
    void setUp() {
        //TODO : tracer = new Tracer();
    }
    @Test
    public void createPrescriptionTest(){
        /* TODO Subject subject = new Subject();
        Date d1 = new Date(LocalDateTime.of(2022,7,23,13,1));
        CovidTest test1 = new CovidTest(CovidTestType.MOLECULAR);
        tracer.createPrescription(subject, d1, test1);
        ArrayList<Prescription> prescriptions = new ArrayList<>();
        prescriptions.add(new Prescription(subject, d1, test1, tracer));

        Date d2 = new Date(LocalDateTime.of(2021, 9, 12, 21,44));
        CovidTest test2 = new CovidTest(CovidTestType.ANTIGEN);
        prescriptions.add(new Prescription(subject, d2, test2, tracer));
        tracer.createPrescription(subject, d2, test2);
        assertPrescriptionsEquals(prescriptions, subject.getPrescriptionRecord());*/
    }

    private void assertPrescriptionsEquals(ArrayList<Prescription> expected, ArrayList<Prescription> actual){
        assertEquals(expected.size(), actual.size());
        for(int i = 0; i<expected.size(); i++){
            assertEquals(expected.get(i).getSubject(), actual.get(i).getSubject());
            assertEquals(expected.get(i).getDate(), actual.get(i).getDate());
            assertEquals(expected.get(i).getCovidTest(), actual.get(i).getCovidTest());
            assertEquals(expected.get(i).getTracer(), actual.get(i).getTracer());
        }
    }

    @Test
    public void editObservationTest(){
        /* TODO Subject subject1 = new Subject();
        Subject subject2 = new Subject();
        ArrayList<Subject> subjects = new ArrayList<>();
        subjects.add(subject1);
        subjects.add(subject2);
        Observation observation = new Observation(subjects, new Symptoms(), new Date(LocalDateTime.of(2022, 7, 23,13,13)));
        assertTrue(observation.isRelevant());
        tracer.editObservation(observation);
        assertFalse(observation.isRelevant());*/
    }


    @AfterEach
    void tearDown() {
    }
}