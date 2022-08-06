package com.sweproject.dao;

import com.sweproject.model.*;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;

public class ObservationDAO extends DAO {

    public void insertObservation(Observation observation) {

    }

    public void insertObservation(ArrayList<Subject> subjects, Type type, LocalDateTime startDate, LocalDateTime endDate) throws SQLException {
        if (startDate == null){
            //it's a covid test
            statement.execute("INSERT INTO `events` (`is_relevant`, `type`, `end_date`) VALUES ('" + 1 + "', '" + type.getName() + "', '" + endDate + "')");
        } else if (endDate == null){
            //subject is still symptomatic
            statement.execute("INSERT INTO `events` (`is_relevant`, `type`, `start_date`) VALUES ('" + 1 + "', '" + type.getName() + "', '" + startDate + "')");
        }else{
            statement.execute("INSERT INTO `events` (`is_relevant`, `type`, `start_date`, `end_date`) VALUES ('" + 1 + "', '" + type.getName() + "', '" + startDate + "','" + endDate + "')");
        }

        ResultSet rs = statement.executeQuery("SELECT max(`id`) as `id` FROM `events`");
        if (rs.next()) {
            int id = rs.getInt("id");
            for (Subject sub : subjects)
                statement.execute("INSERT INTO `observations` (`id`, `fiscalCode`) VALUES ('" + id + "', '" + sub.getFiscalCode() + "')");
        } else {
            System.out.println("Errore");//FIXME
        }
        //TODO getObservation() ???
    }

    public void insertObservation(ArrayList<Subject> subjects, Type type, TimeRecord timeRecord) throws SQLException { //TODO testare
        Timestamp start_date, end_date;
        if (timeRecord instanceof Date) {
            Date date = (Date) timeRecord; //FIXME magari si può evitare(?)
            start_date = null;
            end_date = date.getDate();
            statement.execute("INSERT INTO `events` (`is_relevant`, `type`, `end_date`) VALUES ('" + 1 + "', '" + type.getName() + "', '" + end_date + "')");
        } else {
            TimeInterval timeInterval = (TimeInterval) timeRecord;
            start_date = timeInterval.getInitialDate();
            end_date = timeInterval.getFinalDate();
            statement.execute("INSERT INTO `events` (`is_relevant`, `type`, `start_date`, `end_date`) VALUES ('" + 1 + "', '" + type.getName() + "', '" + start_date + "','" + end_date + "')");
        }

        ResultSet rs = statement.executeQuery("SELECT max(`id`) as `id` FROM `events`");
        if (rs.next()) {
            int id = rs.getInt("id");
            for (Subject sub : subjects)
                statement.execute("INSERT INTO `observations` (`id`, `fiscalCode`) VALUES ('" + id + "', '" + sub.getFiscalCode() + "')");
        } else {
            System.out.println("Errore");//FIXME
        }
        //TODO getObservation() ???
    }
}
