package com.sweproject.dao;

import com.sweproject.model.*;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;

public class ObservationDAO extends DAO {

    public void insertObservation(ArrayList<String> subjects, Type type, LocalDateTime startDate, LocalDateTime endDate){
        ResultSet rs = null;
        try {
            setConnection();
            if (endDate == null) {
                statement.execute("INSERT INTO `events` (`is_relevant`, `type`, `start_date`) VALUES ('" + 1 + "', '" + type.getName() + "', '" + startDate + "')");
            } else if(type.getName().equals("Environment") || (type.getName().equals("Contact"))){
                float riskLevel = type.getName().equals("Environment")?((Environment)type).getRiskLevel():((Contact)type).getRiskLevel();
               // System.out.println("riskLevel: "+riskLevel);
                statement.execute("INSERT INTO `events` (`is_relevant`, `type`, `start_date`, `end_date`, `risk_level`) VALUES ('" + 1 + "', '" + type.getName() + "', '" + startDate + "','" + endDate + "','" + riskLevel+"')");
            }
            else{
                statement.execute("INSERT INTO `events` (`is_relevant`, `type`, `start_date`, `end_date`) VALUES ('" + 1 + "', '" + type.getName() + "', '" + startDate + "','" + endDate + "')");
            }

            rs = statement.executeQuery("SELECT max(`id`) as `id` FROM `events`");
            if (rs.next()) {
                int id = rs.getInt("id");
                for (String sub : subjects)
                    statement.execute("INSERT INTO `observations` (`id`, `fiscalCode`) VALUES ('" + id + "', '" + sub+ "')");
            } else {
                System.out.println("Errore");
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

    public ArrayList<HashMap<String, Object>> getObservations(String FC){
        ResultSet rs = null;
        ArrayList<HashMap<String, Object>> arrayList = null;
        try {
            setConnection();
            rs = statement.executeQuery("SELECT * FROM `events` join `observations` on events.ID = observations.id where `fiscalCode` = " + "'" + FC + "'");
            arrayList = convertResultSet(rs);
        }catch(SQLException e){
            e.printStackTrace();
        }finally {
            closeConnection(rs);
        }
        return arrayList;
    }

    public ArrayList<HashMap<String, Object>> getRelatedObservations(String FC, String id){
        //dato un FC cerca tutti i cluster dove tale FC compare e restituisce le osservazioni dei membri dei cluster
        ResultSet rs = null;
        ArrayList<HashMap<String, Object>> arrayList = null;
        try {
            arrayList = new ArrayList<>();
            setConnection();
            rs = statement.executeQuery("SELECT `fiscalCode` FROM `observations` where `fiscalCode` != '"+ FC +"' and `id` = '" + id+ "'");
            ArrayList<HashMap<String, Object>> fiscalCodes = convertResultSet(rs);
            for(int i = 0; i < fiscalCodes.size(); i++){
                ArrayList<HashMap<String, Object>> obs = getObservations(fiscalCodes.get(i).get("fiscalCode").toString());
                if(obs != null)
                    arrayList.addAll(obs);
            }
        }catch(SQLException e){
            e.printStackTrace();
        }finally {
            closeConnection(rs);
        }
        return arrayList;
    }

    public ArrayList<HashMap<String, Object>> getEnvironmentObservations(String FC){
        LocalDateTime right_now = LocalDateTime.now();
        LocalDateTime now = right_now.truncatedTo(ChronoUnit.SECONDS);
        LocalDateTime start_time_analysis = now.minusDays(6);
        ResultSet rs = null;
        ArrayList<HashMap<String, Object>> arrayList = null;
        try {
            setConnection();
            rs = statement.executeQuery("SELECT * FROM `events` join `observations` on events.ID = observations.id where `fiscalCode` = '" + FC + "' and `type` = 'Environment' and start_date > '" + start_time_analysis + "' order by start_date");
            arrayList = convertResultSet(rs);
        }catch(SQLException e){
            e.printStackTrace();
        }finally {
            closeConnection(rs);
        }
        return arrayList;
    }

    public ArrayList<HashMap<String, Object>> getContactObservations(String FC, ArrayList<String> subjects){
        LocalDateTime right_now = LocalDateTime.now();
        LocalDateTime now = right_now.truncatedTo(ChronoUnit.SECONDS);
        LocalDateTime start_time_analysis = now.minusDays(6);
        ResultSet rs = null;
        ArrayList<HashMap<String, Object>> arrayList = null;
        String subjectList = "";
        try {
            setConnection();
            for (int i=0; i<subjects.size(); i++){
                if (i < subjects.size() - 1){
                    subjectList += "'" + subjects.get(i).toUpperCase() + "', ";
                }
                else
                    subjectList += "'" + subjects.get(i).toUpperCase() + "'";
            }
            rs = statement.executeQuery("SELECT distinct UPPER(obs2.`fiscalCode`) as 'fiscalCode', `start_date`, `risk_level` FROM (`observations` obs1 join `observations` obs2 on obs1.`id` = obs2.`id`) join `events` on events.ID = obs1.`id` where obs1.`fiscalCode` = '" + FC + "' and UPPER(obs2.`fiscalCode`) IN (" + subjectList + ") and `start_date` > '" + start_time_analysis + "' order by start_date");
            arrayList = convertResultSet(rs);
            //System.out.println(arrayList.size());
        }catch(SQLException e){
            e.printStackTrace();
        }finally {
            closeConnection(rs);
        }
        return arrayList;
    }

}
