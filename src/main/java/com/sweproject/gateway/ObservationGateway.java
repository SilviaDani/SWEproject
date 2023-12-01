package com.sweproject.gateway;

import com.sweproject.model.*;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Time;
import java.sql.Timestamp;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;

public class ObservationGateway extends Gateway {

    public void insertObservation(ArrayList<String> subjects, Type type, LocalDateTime startDate, LocalDateTime endDate){
        ResultSet rs = null;
        try {
            setConnection();
            Timestamp startDate_ = Timestamp.valueOf(startDate);
            Timestamp endDate_ = null;
            if (endDate != null) {
                endDate_ = Timestamp.valueOf(endDate);
            }
            if (endDate == null) {
                statement.execute("INSERT INTO events (IS_RELEVANT, TYPE, START_DATE) VALUES (" + 1 + ", '" + type.getName() + "', TO_TIMESTAMP('" + startDate_ + "', 'YYYY-MM-DD HH24:MI:SS.FF3'))");
            } else if(type.getName().equals("Environment") || (type.getName().equals("Contact"))){
                float riskLevel = type.getName().equals("Environment")?((Environment)type).getRiskLevel():((Contact)type).getRiskLevel();
               // System.out.println("riskLevel: "+riskLevel);
                statement.execute("INSERT INTO events (IS_RELEVANT, TYPE, START_DATE, end_date, risk_level) VALUES (" + 1 + ", '" + type.getName() + "',  TO_TIMESTAMP('" + startDate_+ "', 'YYYY-MM-DD HH24:MI:SS.FF3'), TO_TIMESTAMP('" + endDate_ + "', 'YYYY-MM-DD HH24:MI:SS.FF3')," + riskLevel+")");
            }
            else{
                statement.execute("INSERT INTO events (IS_RELEVANT, TYPE, START_DATE, end_date) VALUES (" + 1 + ", '" + type.getName() + "', TO_TIMESTAMP('" + startDate_ + "', 'YYYY-MM-DD HH24:MI:SS.FF3'),TO_TIMESTAMP('" + endDate_ + "', 'YYYY-MM-DD HH24:MI:SS.FF3'))");
            }

            rs = statement.executeQuery("SELECT max(id) as id FROM events");
            if (rs.next()) {
                int id = rs.getInt("id");
                for (String sub : subjects)
                    statement.execute("INSERT INTO observations (id, fiscalCode) VALUES ('" + id + "', '" + sub.toUpperCase()+ "')");
            } else {
                System.out.println("Errore");
            }
        }catch (SQLException e){
            e.printStackTrace();
        } finally {
            closeConnection(rs);
        }

    }

    public ArrayList<HashMap<String, Object>> getRelevantObservations(String FC){
        ResultSet rs = null;
        ArrayList<HashMap<String, Object>> arrayList = null;
        try {
            setConnection();
            rs = statement.executeQuery("SELECT * FROM events join observations on events.ID = observations.id where fiscalCode = " + "'" + FC.toUpperCase() + "' and IS_RELEVANT = 1");
            arrayList = convertResultSet(rs);
        }catch(SQLException e){
            e.printStackTrace();
        }finally {
            closeConnection(rs);
        }
        return arrayList;
    }

    public ArrayList<HashMap<String, Object>> getRelevantSymptomsObservations(String FC, LocalDateTime start_time_analysis){
        ResultSet rs = null;
        ArrayList<HashMap<String, Object>> arrayList = null;
        try {
            setConnection();
            rs = statement.executeQuery("SELECT * FROM events join observations on events.ID = observations.id where fiscalCode = " + "'" + FC.toUpperCase() + "' and IS_RELEVANT = 1 and type = 'Symptoms' and START_DATE > TO_TIMESTAMP('" + Timestamp.valueOf(start_time_analysis) + "','YYYY-MM-DD HH24:MI:SS.FF3') order by START_DATE");
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
            statement.execute("update events set IS_RELEVANT = 0 where ID = " + "'" + selectedObservationID + "'");
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
            rs = statement.executeQuery("SELECT * FROM events join observations on events.ID = observations.id where fiscalCode = " + "'" + FC.toUpperCase() + "'");
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
            rs = statement.executeQuery("SELECT fiscalCode FROM observations where fiscalCode != '"+ FC +"' and id = '" + id+ "'");
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

    public ArrayList<HashMap<String, Object>> getEnvironmentObservations(String FC, int samples){
        LocalDateTime right_now = LocalDateTime.now();
        LocalDateTime now = right_now.truncatedTo(ChronoUnit.SECONDS);
        LocalDateTime start_time_analysis = now.minusHours(samples);
        //System.out.println(start_time_analysis);
        ResultSet rs = null;
        ArrayList<HashMap<String, Object>> arrayList = null;
        try {
            setConnection();
            rs = statement.executeQuery("SELECT * FROM events join observations on events.ID = observations.id where fiscalCode = '" + FC.toUpperCase() + "' and type = 'Environment' and START_DATE > TO_TIMESTAMP('" + Timestamp.valueOf(start_time_analysis) + "','YYYY-MM-DD HH24:MI:SS.FF3') order by START_DATE");
            arrayList = convertResultSet(rs);
        }catch(SQLException e){
            e.printStackTrace();
        }finally {
            closeConnection(rs);
        }
        return arrayList;
    }

    public ArrayList<HashMap<String, Object>> getContactObservations(String FC, ArrayList<String> subjects, int samples){
        LocalDateTime right_now = LocalDateTime.now();
        LocalDateTime now = right_now.truncatedTo(ChronoUnit.SECONDS);
        LocalDateTime start_time_analysis = now.minusHours(samples);
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
            rs = statement.executeQuery("SELECT distinct UPPER(obs2.fiscalCode) as FISCALCODE, START_DATE, RISK_LEVEL FROM (observations obs1 join observations obs2 on obs1.ID = obs2.ID) join events on events.ID = obs1.ID where obs1.FISCALCODE = '" + FC.toUpperCase() + "' and UPPER(obs2.FISCALCODE) IN (" + subjectList + ") and START_DATE > TO_TIMESTAMP('" + Timestamp.valueOf(start_time_analysis) + "','YYYY-MM-DD HH24:MI:SS.FF3') order by START_DATE");
            arrayList = convertResultSet(rs);
            //System.out.println(arrayList.size());
        }catch(SQLException e){
            e.printStackTrace();
        }finally {
            closeConnection(rs);
        }
        return arrayList;
    }

    public ArrayList<HashMap<String, Object>> getTestObservations(String FC, LocalDateTime start_time_analysis){
        ResultSet rs = null;
        ArrayList<HashMap<String, Object>> arrayList = null;
        try {
            setConnection();
            rs = statement.executeQuery("SELECT * FROM events join observations on events.ID = observations.id where fiscalCode = '" + FC + "' and type like 'Covid_test%' and START_DATE > TO_TIMESTAMP('" + Timestamp.valueOf(start_time_analysis) + "','YYYY-MM-DD HH24:MI:SS.FF3') order by START_DATE");
            arrayList = convertResultSet(rs);
        }catch(SQLException e){
            e.printStackTrace();
        }finally {
            closeConnection(rs);
        }
        return arrayList;
    }
}
