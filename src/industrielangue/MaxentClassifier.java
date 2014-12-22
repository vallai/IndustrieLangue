/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package industrielangue;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import opennlp.maxent.BasicEventStream;
import opennlp.maxent.GIS;
import opennlp.maxent.PlainTextByLineDataStream;
import opennlp.maxent.io.GISModelWriter;
import opennlp.maxent.io.SuffixSensitiveGISModelWriter;
import opennlp.model.AbstractModel;
import opennlp.model.Event;
import opennlp.model.EventStream;
import opennlp.model.GenericModelReader;
import opennlp.model.ListEventStream;


/**
 *
 * @author sam
 */
public class MaxentClassifier {

    public static boolean USE_SMOOTHING = false;
    public static double SMOOTHING_OBSERVATION = 0.1;
    AbstractModel model;
    List<Event> instances;

    public MaxentClassifier() {
        this.reset();
    }

    public final void reset() {
        if (this.instances == null) {
            this.instances = new ArrayList<Event>();
        } else {
            this.instances.clear();
        }
    }

    /**
     * Train from file
     *
     * @param dataPath
     * @param real
     * @param type
     * @throws FileNotFoundException
     * @throws IOException
     */
    public void trainOnDataFile(String dataPath)
            throws FileNotFoundException, IOException {
        File file = new File(dataPath);
        System.out.println("Entraînement avec les données du fichier : "
                + file.getAbsolutePath());
        FileReader datafr = new FileReader(file);
        EventStream es;
        es = new BasicEventStream(new PlainTextByLineDataStream(datafr));
        GIS.SMOOTHING_OBSERVATION = SMOOTHING_OBSERVATION;
        model = GIS.trainModel(es, USE_SMOOTHING);
    }

    public void addInstance(String features, String outcome) {
        this.instances.add(new Event(outcome, features.split(" |\t")));
    }

    public void trainOnInstances() throws IOException {
        ListEventStream evstream = new ListEventStream(this.instances);
        this.model = GIS.trainModel(evstream, 100, 1);
    }

    public void saveModel(String modelPath) throws IOException {
        File outputFile = new File(modelPath);
        GISModelWriter wr = new SuffixSensitiveGISModelWriter(model, outputFile);
        wr.persist();
    }

    public void loadModel(String modelPath) throws IOException {
        this.model = new GenericModelReader(new File(modelPath)).getModel();
    }

    public double[] predict(String features) {
        return this.model.eval(features.split(" |\t"));
    }

    public String getBestPrediction(String features) {
        double[] predictions = this.predict(features);
        return model.getBestOutcome(predictions);
    }

    public static void main(String[] args) throws IOException {
        MaxentClassifier cl = new MaxentClassifier();
        cl.addInstance("Henry=true Beckham=true", "arsenal");
        cl.addInstance("Henry=true Beckham=false", "arsenal");
        cl.addInstance("Henry=false Beckham=true", "MU");
        cl.addInstance("Henry=false Beckham=false", "MU");
        cl.trainOnInstances();
        String ex = "home=man_united Henry=false Beckham=false";
        String oc = cl.getBestPrediction(ex);
        System.out.println(ex + " --> " + oc);
    }
}
