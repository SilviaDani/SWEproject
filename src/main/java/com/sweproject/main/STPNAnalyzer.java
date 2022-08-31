package com.sweproject.main;

import com.sweproject.dao.ObservationDAO;
import org.oristool.petrinet.PetriNet;
import org.oristool.petrinet.Transition;

import java.util.ArrayList;
import java.util.HashMap;

public class STPNAnalyzer {
    private ObservationDAO observationDAO;
    private ArrayList<String> alreadyAnalizedCodes = new ArrayList<>();

    public STPNAnalyzer() {
        this.observationDAO = new ObservationDAO();
    }

    private boolean haveAlreadyBeenAnalized(String FC){
        for(String code : alreadyAnalizedCodes){
            if(code.equals(FC))
                return true;
        }
        alreadyAnalizedCodes.add(FC);
        return false;
    }

    public void makeModel(String fiscalCode){
        PetriNet pn = new PetriNet();
        ArrayList<HashMap<String, Object>> arrayList = observationDAO.getObservations(fiscalCode);
        ArrayList<HashMap<String, Object>> obs = new ArrayList<>();
        for(int i = 0; i < arrayList.size(); i++){
            if (arrayList.get(i).get("type").toString().equals("Contact") && !haveAlreadyBeenAnalized(arrayList.get(i).get("fiscalCode").toString())) {
                 obs.addAll(observationDAO.getRelatedObservations(arrayList.get(i).get("fiscalCode").toString(), arrayList.get(i).get("id").toString()));
            }
        }
        arrayList.addAll(obs);
        for(int i = 0; i<arrayList.size(); i++){
            System.out.println(arrayList.get(i).get("fiscalCode")+" "+arrayList.get(i).get("type"));
            if (arrayList.get(i).get("type").toString().equals("Contact")){
                Transition transitionContact = pn.addTransition("transition"+i);
                //transitionContact.addFeature();
                //pn.addPrecondition(,transitionContact);
                //pn.addPostcondition(transitionContact, );
                //TODO model contact as transition(?)
            }
            else if (arrayList.get(i).get("type").toString().equals("Symptoms")){
                //TODO model symptoms
            }else {
                //TODO model covid test
            }
        }
        System.out.println("Model created");
    }

    public void analyze(){
        //TODO
        System.out.println("Sto analizzando");
    }
}
