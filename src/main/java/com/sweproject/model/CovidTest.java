package com.sweproject.model;

import org.apache.commons.math3.distribution.NormalDistribution;
import org.apache.commons.math3.distribution.WeibullDistribution;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Random;

public class CovidTest extends Type {
    private CovidTestType type;
    private boolean isPositive = false;
    private double sensitivity = 0;
    private double specificity = 0;
    private double precision = 0;
    static double correctionValueForAntigen = 0;
    static double correctionValueForMolecular = 0;

    public CovidTest(CovidTestType type) {
        this.type = type;
        Random r = new Random();
        if (type == CovidTestType.ANTIGEN) {
            sensitivity = 0.70 + r.nextFloat()*(0.86 - 0.70); //70-86 % dichiarato dal produttore, riferito ai test di fine 2020. I test di ultima generazione hanno risultati sovrapponibili a quelli dei test molecolari
            specificity = 0.95 + r.nextFloat()*(0.97-0.95); //95-97% dichiarato dal produttore
            precision = 0.70 + r.nextFloat()*(0.86 - 0.70);
        }
        else if (type == CovidTestType.MOLECULAR) {
            sensitivity = 0.95;
            specificity = 0.98;
            precision = 0.95;
        }

        //find correction values
        if(correctionValueForAntigen == 0 || correctionValueForMolecular == 0) {
            WeibullDistribution wMol = new WeibullDistribution(1.7, 8.0);
            WeibullDistribution wAnt = new WeibullDistribution(2.0, 4.0);
            NormalDistribution n1 = new NormalDistribution(72.85, 0.45);
            NormalDistribution n2 = new NormalDistribution(84.85, 0.45);
            double increment  = 0.1;
            double upperBound = 90;
            double x = 60;
            double maxMol = 0, maxAnt = 0;
            while(x<upperBound){
                double densityMol = n1.density(x);
                double densityAnt = n2.density(x);
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
    public double getSensitivity(){
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
        NormalDistribution n;
        Random r = new Random();
        LocalDateTime tWhereTestStartsWorking;
        double correctionValue = 0;
        if(this.type == CovidTestType.MOLECULAR){
            /*w = new WeibullDistribution(1.7, 8.0);
            tWhereTestStartsWorking = LocalDateTime.from(contactDate).plusHours((long)(48 + r.nextFloat() * 48)); //in media dopo 5 giorni compaiono i sintomi, il test diventa rilevante dopo 2-4 giorni che è avvenuto il contagio*/
            n = new NormalDistribution(72.85, 0.45);
            tWhereTestStartsWorking = LocalDateTime.from((contactDate).plusHours((long)n.sample()));
            correctionValue = correctionValueForMolecular;
        }else if(this.type == CovidTestType.ANTIGEN){
            /*w = new WeibullDistribution(2, 4);
            tWhereTestStartsWorking = LocalDateTime.from(contactDate).plusHours((long)(60 + r.nextFloat() * 48)); //il test antigenico diventa rilevante poco dopo*/
            n = new NormalDistribution(84.85, 0.45);
            tWhereTestStartsWorking = LocalDateTime.from((contactDate).plusHours((long)n.sample()));
            correctionValue = correctionValueForAntigen;
        }else{
            throw new Exception("Covid test type not implemented");
        }
        double delta = ChronoUnit.HOURS.between(tWhereTestStartsWorking, testDate)/24.f;
        //System.out.println(delta + " delta");
        double testEvidence = 1;
        if(delta>0){
            // senza falsi positivi e negativi
           if(this.isPositive)
                testEvidence = n.density(delta) * correctionValue * this.precision;
            else
                testEvidence = 1 - n.density(delta) * correctionValue * this.precision;
        }
        return testEvidence;
    }
}
//https://www.cochrane.org/CD013705/INFECTN_how-accurate-are-rapid-antigen-tests-diagnosing-covid-19#:~:text=In%20people%20with%20confirmed%20COVID,cases%20had%20positive%20antigen%20tests).
//https://www.iss.it/en/primo-piano/-/asset_publisher/3f4alMwzN1Z7/content/diagnosticare-covid-19-gli-strumenti-a-disposizione.-il-punto-dell-iss
//https://www.ars.toscana.it/geotermia-e-salute/2-articoli/4403-coronavirus-come-dove-quando-fare-tamponi-e-test-sierologici.html
//https://www.my-personaltrainer.it/salute/tampone-molecolare-covid-19.html
//https://www.dottorsartori.it/index.php/articoli-di-informazione-sanitaria/276-coronavirus-attenzione-ai-falsi-negativi-del-tampone.html


