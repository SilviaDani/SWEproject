package com.sweproject.dao;

import com.sweproject.model.*;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;

public class ObservationDAO extends DAO {

    public void insertObservation(ArrayList<Subject> subjects, Type type, LocalDateTime startDate, LocalDateTime endDate){
        ResultSet rs = null;
        try {
            setConnection();
            if (endDate == null) {
                statement.execute("INSERT INTO `events` (`is_relevant`, `type`, `start_date`) VALUES ('" + 1 + "', '" + type.getName() + "', '" + startDate + "')");
            } else {
                statement.execute("INSERT INTO `events` (`is_relevant`, `type`, `start_date`, `end_date`) VALUES ('" + 1 + "', '" + type.getName() + "', '" + startDate + "','" + endDate + "')");
            }

            rs = statement.executeQuery("SELECT max(`id`) as `id` FROM `events`");
            if (rs.next()) {
                int id = rs.getInt("id");
                for (Subject sub : subjects)
                    statement.execute("INSERT INTO `observations` (`id`, `fiscalCode`) VALUES ('" + id + "', '" + sub.getFiscalCode() + "')");
            } else {
                System.out.println("Errore");//FIXME
            }
        }catch (SQLException e){
            e.printStackTrace();
        }finally {
            closeConnection(rs);
        }

    }

    public ArrayList<HashMap<String, Object>> getRelevantObservations(String FC){
        ResultSet rs = null;
        ArrayList<HashMap<String, Object>> arrayList = null;
        try {
            setConnection();
            rs = statement.executeQuery("SELECT * FROM `events` join `observations` on events.ID = observations.id where `fiscalCode` = " + "'" + FC + "' and `is_relevant` = 1");
            arrayList = convertResultSet(rs);
        }catch(SQLException e){
            e.printStackTrace();
        }finally {
            closeConnection(rs);
        }
        return arrayList;
    }

    public void changeRelevance(String selectedObservationID){
        try {
            setConnection();
            statement.execute("update `events` set is_relevant = 0 where ID = " + "'" + selectedObservationID + "'");
        }catch (SQLException e){
            e.printStackTrace();
        }finally {
            closeConnection();
        }
    }
}
