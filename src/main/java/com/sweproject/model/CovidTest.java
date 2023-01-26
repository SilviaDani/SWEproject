package com.sweproject.model;

import org.apache.commons.math3.distribution.WeibullDistribution;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Random;

public class CovidTest extends Type {
    private CovidTestType type;
    private boolean isPositive = false;
    private float sensitivity = 0f;
    private float specificity = 0f;
    static double correctionValueForAntigen = 0;
    static double correctionValueForMolecular = 0;

    public CovidTest(CovidTestType type) {
        this.type = type;
        Random r = new Random();
        if (type == CovidTestType.ANTIGEN) {
            sensitivity = 0.70f + r.nextFloat()*(0.86f - 0.70f); //70-86 % dichiarato dal produttore, riferito ai test di fine 2020. I test di ultima generazione hanno risultati sovrapponibili a quelli dei test molecolari
            specificity = 0.95f + r.nextFloat()*(0.97f-0.95f); //95-97% dichiarato dal produttore
        }
        else if (type == CovidTestType.MOLECULAR) {
            sensitivity = 0.95f;
            specificity = 0.98f;
        }

        //find correction values
        if(correctionValueForAntigen == 0 || correctionValueForMolecular == 0) {
            WeibullDistribution wMol = new WeibullDistribution(1.7, 8.0);
            WeibullDistribution wAnt = new WeibullDistribution(2.0, 4.0);
            double increment  = 0.1;
            double upperBound = 10;
            double x = 0;
            double maxMol = 0, maxAnt = 0;
            while(x<upperBound){
                double densityMol = wMol.density(x);
                double densityAnt = wAnt.density(x);
                if(densityMol > maxMol)
                    maxMol = densityMol;
                if(densityAnt > maxAnt){
                    maxAnt = densityAnt;
                }
                x += increment;
            }
            correctionValueForMolecular = 1/maxMol;
            correctionValueForAntigen = 1/maxAnt;

        }

    }
    public CovidTest(CovidTestType type, boolean isPositive){
        this(type);
        this.isPositive = isPositive;
    }
    public float getSensitivity(){
        return sensitivity;
    }
    public boolean getPositivity(){
        return isPositive;
    }

    @Override
    public String getName() {
        return "Covid_test-" + type + "-" + isPositive;
    }

    public CovidTestType getTestType() {
        return type;
    }

    public boolean isPositive() {
        return isPositive;
    }

    public double isInfected(LocalDateTime contactDate, LocalDateTime testDate) throws Exception {
        WeibullDistribution w;
        Random r = new Random();
        LocalDateTime tWhereTestStartsWorking;
        double correctionValue = 0;
        if(this.type == CovidTestType.MOLECULAR){
            w = new WeibullDistribution(1.7, 8.0);
            tWhereTestStartsWorking = LocalDateTime.from(contactDate).plusHours((long)(48 + r.nextFloat() * 48)); //in media dopo 5 giorni compaiono i sintomi, il test diventa rilevante dopo 2-4 giorni che è avvenuto il contagio
            correctionValue = correctionValueForMolecular;
        }else if(this.type == CovidTestType.ANTIGEN){
            w = new WeibullDistribution(2, 4);
            tWhereTestStartsWorking = LocalDateTime.from(contactDate).plusHours((long)(60 + r.nextFloat() * 48)); //il test antigenico diventa rilevante poco dopo
            correctionValue = correctionValueForAntigen;
        }else{
            throw new Exception("Covid test type not implemented");
        }
        double delta = ChronoUnit.HOURS.between(tWhereTestStartsWorking, testDate)/24.f;
        System.out.println(delta + " delta");
        double testEvidence = 1;
        if(delta>0){
            // senza falsi positivi e negativi
            if(this.isPositive)
                testEvidence = w.density(delta) * correctionValue * this.sensitivity;
            else
                testEvidence = 1 - w.density(delta) * correctionValue * this.specificity;
        }
        return testEvidence;
    }
}
//https://www.cochrane.org/CD013705/INFECTN_how-accurate-are-rapid-antigen-tests-diagnosing-covid-19#:~:text=In%20people%20with%20confirmed%20COVID,cases%20had%20positive%20antigen%20tests).
//https://www.iss.it/en/primo-piano/-/asset_publisher/3f4alMwzN1Z7/content/diagnosticare-covid-19-gli-strumenti-a-disposizione.-il-punto-dell-iss
//https://www.ars.toscana.it/geotermia-e-salute/2-articoli/4403-coronavirus-come-dove-quando-fare-tamponi-e-test-sierologici.html
//https://www.my-personaltrainer.it/salute/tampone-molecolare-covid-19.html
//https://www.dottorsartori.it/index.php/articoli-di-informazione-sanitaria/276-coronavirus-attenzione-ai-falsi-negativi-del-tampone.html


