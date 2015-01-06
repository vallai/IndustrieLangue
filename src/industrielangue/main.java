package industrielangue;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Christian SCHMIDT & Gaëtan REMOND
 */
public class main {

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        if ((args.length == 5 && args[0].equals("-train")) || (args.length == 6 && args[0].equals("-annot"))) {
            AEF aef = new AEF();
            aef.chargerAutomaton("../dico.aef");

            if (args.length == 5 && args[0].equals("-train")) {
                String texte = AccesFichiers.lireFichierTexte(args[2], " ");
                String texteParse = AccesFichiers.parserTexte(texte);
                Texte tokens = new Texte(texteParse, aef.getAutomaton());

                if (args[1].equals("-percept")) {
                    PerceptClassifier pc = new PerceptClassifier(tokens);
                    pc.perceptronTrain();
                    pc.saveModel(args[4]);
                    System.out.println("Fichier " + args[4] + " généré");
                } else if (args[1].equals("-maxent")) {
                    MaxentClassifier cl = aef.AjouterInstances(tokens);
                    try {
                        cl.trainOnInstances();
                        cl.saveModel(args[4]);
                        System.out.println("Fichier " + args[4] + " généré");
                    } catch (IOException ex) {
                        Logger.getLogger(AEF.class.getName()).log(Level.SEVERE, null, ex);
                    }
                }

            } else if (args.length == 6 && args[0].equals("-annot")) {
                String texte = AccesFichiers.lireFichierTexte(args[5], " ");
                String texteParse = AccesFichiers.parserTexte(texte);
                Texte tokens = new Texte(texteParse, aef.getAutomaton());

                String texteAnnote;
                switch (args[1]) {
                    case "-percept":
                        PerceptClassifier pc = new PerceptClassifier(tokens);
                        pc.loadModel(args[3]);
                        texteAnnote = pc.perceptronAnnot();

                        AccesFichiers.ecrireFichierTexte(texteAnnote, args[5].replace(".txt", "_annote.txt"));
                        System.out.println("Fichier " + args[5].replace(".txt", "_annote.txt") + " généré");
                        break;
                    case "-maxent":
                        MaxentClassifier cl = new MaxentClassifier();
                        try {
                            cl.loadModel(args[3]);
                            texteAnnote = aef.meilleuresPredictions(tokens, cl);

                            AccesFichiers.ecrireFichierTexte(texteAnnote, args[5].replace(".txt", "_annote.txt"));
                            System.out.println("Fichier " + args[5].replace(".txt", "_annote.txt") + " généré");
                        } catch (IOException ex) {
                            Logger.getLogger(AEF.class.getName()).log(Level.SEVERE, null, ex);
                        }
                        break;
                }
            }
        } else {
            System.out.println("Usage : java -jar T4.jar -annot [-percept|-maxent] –m <model MOD> –text <texte TXT>");
            System.out.println("Usage : java -jar T4.jar -train [-percept|-maxent] <en_train TXT> –m <model MOD>");
        }
    }

}
