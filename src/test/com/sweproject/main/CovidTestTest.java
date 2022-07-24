package com.sweproject.main;

import com.sweproject.model.CovidTest;
import com.sweproject.model.CovidTestType;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class CovidTestTest {

    @Test
    public void constructorTest(){
        CovidTest covidTest1 = new CovidTest(CovidTestType.ANTIGEN);
        assertEquals(0.653f, covidTest1.getSensitivity());
        assertFalse(covidTest1.getPositivity());

        CovidTest covidTest2 = new CovidTest(CovidTestType.MOLECULAR);
        assertEquals(0.918f, covidTest2.getSensitivity());
        assertFalse(covidTest2.getPositivity());

        CovidTest covidTest3 = new CovidTest(CovidTestType.ANTIGEN, true);
        assertEquals(0.653f, covidTest3.getSensitivity());
        assertTrue(covidTest3.getPositivity());

        CovidTest covidTest4 = new CovidTest(CovidTestType.MOLECULAR, true);
        assertEquals(0.918f, covidTest4.getSensitivity());
        assertTrue(covidTest4.getPositivity());
    }

    @BeforeEach
    void setUp() {
    }

    @AfterEach
    void tearDown() {
    }
}