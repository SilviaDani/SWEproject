package com.sweproject.main;

import org.checkerframework.checker.units.qual.A;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.*;

class NotifierTest {
    Notifier notifier;
    @BeforeEach
    void setUp() {
        notifier = new Notifier();
    }

    @Test
    @DisplayName("Test: create an observation")
    void createObservationTest() {
        Subject subject1 = new Subject();
        Subject subject2 = new Subject();
        ArrayList<Subject> subjects = new ArrayList<>();
        subjects.add(subject1);
        subjects.add(subject2);
        Type symptoms = new Symptoms();
        TimeRecord time1 = new Date(LocalDateTime.of(2022, 7, 23, 12,14));
        ArrayList<Observation> observations = new ArrayList<>();
        observations.add(new Observation(subjects, symptoms, time1));
        notifier.createObservation(subjects, symptoms, time1);

        Type contact = new Contact(subjects, 0.1f);
        TimeRecord time2 = new TimeInterval(new Date(LocalDateTime.of(2022, 2, 3, 11, 21)), new Date(LocalDateTime.of(2022, 2, 3, 21, 49)));
        observations.add(new Observation(subjects, contact, time2));
        notifier.createObservation(subjects, contact, time2);

        assertObservationsEquals(observations, notifier.getSubject().getObservationRecord());
    }

    private void assertObservationsEquals(ArrayList<Observation> expected, ArrayList<Observation> actual){
        assertEquals(expected.size(), actual.size());
        for(int i = 0; i<expected.size(); i++){
            assertEquals(expected.get(i).getSubjects(), actual.get(i).getSubjects());
            assertEquals(expected.get(i).getTimeRecord(), actual.get(i).getTimeRecord());
            assertEquals(expected.get(i).isRelevant(), actual.get(i).isRelevant());
            assertEquals(expected.get(i).getType(), actual.get(i).getType());
        }
    }

    @Test
    void getProbabilityOfBeingInfected() {
    }
}