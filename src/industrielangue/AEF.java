package industrielangue;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import dk.brics.automaton.*;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author Christian SCHMIDT & Gaëtan REMOND
 */
public class AEF {

    private Automaton automaton;
    private final int nbAttributs = 31;
    
    public AEF() {
        automaton = null;
    }

    /////////////////////////////////////////////////////////////////////////////
    ////////////////////         GENERATION DE L'AEF         ////////////////////
    /////////////////////////////////////////////////////////////////////////////
    /**
     * Lit un fichier donné en paramètre et retourne une liste des lignes
     * découpées selon les tabulations
     *
     * @param fichier à lire
     * @return Liste des lignes découpées selon les tabulations
     */
    public ArrayList<String[]> lireFichier(String fichier) {
        BufferedReader br;
        String ligne;
        ArrayList<String[]> lignes = new ArrayList<>();
        try {
            br = new BufferedReader(new FileReader(fichier));
            while ((ligne = br.readLine()) != null) {
                String[] tmp = ligne.split("\t");
                if (tmp.length != 3) {
                    System.out.println("Erreur de construction du dictionnaire");
                    System.exit(1);
                }
                lignes.add(tmp);
            }
            br.close();
        } catch (FileNotFoundException ex) {
            System.out.println("Problème d'ouverture : " + ex.getMessage());
            System.exit(1);
        } catch (IOException ex) {
            System.out.println("Problème de lecture : " + ex.getMessage());
            System.exit(1);
        }
        return lignes;
    }

    /**
     * Construit la suite de caractères pour la création de l'AEF selon la forme
     * : "forme + 0 + nbCaractereAEnlever + terminaison + 0 + traits"
     *
     * @param lignes
     * @return lignesEncodees
     */
    public CharSequence[] encodageAnalysesMorphologiques(ArrayList<String[]> lignes) {
        CharSequence[] lignesEncodees = new CharSequence[lignes.size()];
        // Pour chaque entrée
        int i = 0;
        for (String[] ligne : lignes) {
            String forme = ligne[0];
            String lemme = ligne[1];
            String traits = ligne[2];

            // On cherche le nombre de caractères communs
            int nbCommun = 0;
            while ((forme.length() >= nbCommun) && (lemme.length() >= nbCommun) && forme.substring(0, nbCommun).equals(lemme.substring(0, nbCommun))) {
                nbCommun++;
            }

            // Pour en déduire le nombre de caractère à enlever et la terminaison
            int nbCaractereAEnlever = forme.length() - (nbCommun - 1);
            String terminaison = lemme.substring(nbCommun - 1);
            // Construction de la chaine
            lignesEncodees[i] = (CharSequence) (forme + (char) 0 + (char) nbCaractereAEnlever + terminaison + (char) 0 + traits);
            i++;
//            System.out.println("forme : " + forme + "  lemme : " + lemme + "  traits : " + traits + " nbCar : " + nbCaractereAEnlever + " Terminaison : " + terminaison);
        }
        return lignesEncodees;
    }

    /**
     * Sauvegarde de l'automaton dans le fichier donné en paramètre
     *
     * @param fichier
     */
    public void sauvegarderAutomaton(String fichier) {
        File file;
        FileOutputStream fop;
        try {
            file = new File(fichier);
            fop = new FileOutputStream(file);

            if (!file.exists()) {
                file.createNewFile();
            }
            automaton.store(fop);
            fop.flush();
            fop.close();
        } catch (IOException ex) {
            System.out.println("Problème d'écriture : " + ex.getMessage());
        }
    }

    /////////////////////////////////////////////////////////////////////////////
    ///////////////////////         TEST D'UN MOT         ///////////////////////
    /////////////////////////////////////////////////////////////////////////////
    /**
     * Chargement de l'automaton du fichier donné en paramètre
     *
     * @param fichier
     * @return automaton
     */
    public Automaton chargerAutomaton(String fichier) {
        File file = new File(fichier);
        FileInputStream fis;
        Automaton automat = null;

        try {
            fis = new FileInputStream(file);
            automat = Automaton.load(fis);
        } catch (IOException | ClassNotFoundException ex) {
            System.out.println("Problème de lecture : " + ex.getMessage());
            System.exit(1);
        }
        return automat;
    }


    /**
     * Affichage des analyses morphologiques
     *
     * @param analyses
     */
    public void afficherAnalysesMorphologique(AnalyseMorphologique[] analyses) {
        if (analyses != null) {
            System.out.println("Lemme\tTraits\n------------------------------");
            for (AnalyseMorphologique analyse : analyses) {
                System.out.println(analyse.getLemme() + "\t" + analyse.getTraits());
            }
        } else {
            System.out.println("Le mot demandé n'a pas été trouvé");
        }
    }

    /////////////////////////////////////////////////////////////////////////////
    //////////////////         T2 : Lecture d'un fichier        /////////////////
    /////////////////////////////////////////////////////////////////////////////
    /**
     * Lit un fichier texte encodé en UTF8
     *
     * @param fichier
     * @return
     */
    public String lireFichierTexte(String fichier, String separateur) {
        String texte = "";
        BufferedReader br;
        String ligne;
        try {
            br = new BufferedReader(new InputStreamReader(new FileInputStream(fichier), "UTF8"));
            while ((ligne = br.readLine()) != null) {
                texte += ligne + separateur;
            }
            br.close();
        } catch (FileNotFoundException ex) {
            System.out.println("Problème d'ouverture : " + ex.getMessage());
            System.exit(1);
        } catch (IOException ex) {
            System.out.println("Problème de lecture : " + ex.getMessage());
            System.exit(1);
        }
        return texte;
    }

    /**
     * parse un texte
     *
     * @param texte
     * @return le texte parsé
     */
    public String parserTexte(String texte) {
        texte = texte.replace(",", " ,");
        texte = texte.replace(".", " . ");
        texte = texte.replace("(", " ( ");
        texte = texte.replace(")", " ) ");
        texte = texte.replace("'", " ' ");
        texte = texte.replace(" E\n", " E \n");
        texte = texte.replace(" D-ORG", " DORG ");
        texte = texte.replace(" I-ORG", " IORG ");
        texte = texte.replace(" D-PERS", " DPERS ");
        texte = texte.replace(" I-PERS", " IPERS ");
        texte = texte.replace(" D-LOC", " DLOC ");
        texte = texte.replace(" I-LOC", " ILOC ");
        return texte;
    }

    /**
     * Ecrit un fichier texte encodé en UTF8
     *
     * @param texte
     * @param fichier
     */
    public void ecrireFichierTexte(String texte, String fichier) {
        try {
            PrintWriter pw = new PrintWriter(fichier, "UTF-8");
            pw.print(texte);
            pw.close();
        } catch (FileNotFoundException ex) {
            System.out.println("Problème de fichier : " + ex.getMessage());
            System.exit(1);
        } catch (UnsupportedEncodingException ex) {
            System.out.println("Problème d'encodage : " + ex.getMessage());
            System.exit(1);
        }
    }

    /////////////////////////////////////////////////////////////////////////////
    ///////////////         T4 : Apprentissage automatique        ///////////////
    /////////////////////////////////////////////////////////////////////////////


    /**
     * Permet de transformer la matrice récupéré dans le fichier au format
     * string en un int[][]
     *
     * @param matriceString string à tranformer
     * @return matrice au format int[][]
     */
    private int[][] stringToMatrice(String matriceString) {
        int[][] matrice = new int[7][nbAttributs];
        String[] lignes = matriceString.split("\n");
        for (int i = 0; i < lignes.length; i++) {
            String[] cases = lignes[i].split("\t");
            for (int j = 0; j < cases.length; j++) {
                matrice[i][j] = Integer.parseInt(cases[j]);
            }
        }
        return matrice;
    }

    private final String[] nomTraits = {"MAJ1", "MAJTOUT", "INCONNU", "CAT_NOM", "CAT_ADJ", "CAT_CON", "CAT_PRE",
        "-1CAT_DET", "-1CAT_ADJ", "-1CAT_PRE", "-1CAT_CON", "-1CAT_NOM", "-1CAT_VER", "-1INCONNU",
        "-1APO", "-1VIRG", "-1PARO", "-1M.", "-1MM.", "-1MAJ1",
        "+1CAT_VIRG", "+1CAT_PARF", "+1CAT_DET", "+1CAT_CON", "+1CAT_ADJ", "+1CAT_VER", "+1CAT_PRE"};

    public MaxentClassifier AjouterInstances(Texte texte) {
        MaxentClassifier cl = new MaxentClassifier();

        texte.calculVecteurs();
        for (Token token : texte.getTokens()) {
            String attributs = "";
            boolean[] traits = token.getAttributs();
            for (int i = 0; i < traits.length - 4; i++) {
                attributs += nomTraits[i] + "=" + traits[i] + " ";
            }
            cl.addInstance(attributs, token.getC_ref());
        }
        return cl;
    }

    public String meilleuresPredictions(Texte texte, MaxentClassifier cl) {
        String res = "";
        texte.calculVecteurs();
        int bonPred = 0, mauvaisePred = 0;
        for (Token token : texte.getTokens()) {
            String attributs = "";
            boolean[] traits = token.getAttributs();
            for (int i = 0; i < traits.length - 4; i++) {
                attributs += nomTraits[i] + "=" + traits[i] + " ";
            }
            String c_res = cl.getBestPrediction(attributs);
            if (c_res.equals(token.getC_ref())) {
                bonPred++;
            } else {
                mauvaisePred++;
            }
            res += token.getForme() + "\t" + c_res + "\n";
        }
        float precision = (float) bonPred / (float) (bonPred + mauvaisePred);
        if (precision != 0) {
            System.out.println("Précision : " + precision);
        }
        return res;
    }

    public static void main(String[] args) {
        AEF aef = new AEF();
        if ((args.length == 5 && args[0].equals("-train")) || (args.length == 6 && args[0].equals("-annot"))) {
            aef.automaton = aef.chargerAutomaton("../dico.aef");

            if (args.length == 5 && args[0].equals("-train")) {
                String texte = aef.lireFichierTexte(args[2], " ");
                String texteParse = aef.parserTexte(texte);
                Texte tokens = new Texte(texteParse, aef.automaton);

                if (args[1].equals("-percept")) {
                    PerceptClassifier pc = new PerceptClassifier(tokens);
                    String enPercept = pc.perceptronTrain();
                    aef.ecrireFichierTexte(enPercept, args[4]);
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
                String texte = aef.lireFichierTexte(args[5], " ");
                String texteParse = aef.parserTexte(texte);
                Texte tokens = new Texte(texteParse, aef.automaton);
                
                if (args[1].equals("-percept")) {
                    PerceptClassifier pc = new PerceptClassifier(tokens);
                    String matriceString = aef.lireFichierTexte(args[3], "\n");
                    int[][] matrice = aef.stringToMatrice(matriceString);
                    String texteAnnote = pc.perceptronAnnot(matrice);
                    aef.ecrireFichierTexte(texteAnnote, args[5].replace(".txt", "_annote.txt"));
                    System.out.println("Fichier " + args[5].replace(".txt", "_annote.txt") + " généré");
                } else if (args[1].equals("-maxent")) {

                    MaxentClassifier cl = new MaxentClassifier();
                    try {
                        cl.loadModel(args[3]);
                        String texteAnnote = aef.meilleuresPredictions(tokens, cl);
                        aef.ecrireFichierTexte(texteAnnote, args[5].replace(".txt", "_annote.txt"));
                        System.out.println("Fichier " + args[5].replace(".txt", "_annote.txt") + " généré");
                    } catch (IOException ex) {
                        Logger.getLogger(AEF.class.getName()).log(Level.SEVERE, null, ex);
                    }
                }
            }
        } else {
            System.out.println("Usage : java -jar T4.jar -annot [-percept|-maxent] –m <model MOD> –text <texte TXT>");
            System.out.println("Usage : java -jar T4.jar -train [-percept|-maxent] <en_train TXT> –m <model MOD>");
        }

    }

//    main de la T2
//    public static void main(String[] args) {
//        AEF aef = new AEF();
//        if (args.length == 2) {
//            System.out.println("==== Segmentation et analyse morphologique de textes ====");
//
//            System.out.print("Chargement de l'AEF ......... ");
//            aef.automaton = aef.chargerAutomaton(args[0]);
//            System.out.println("OK");
//
//            System.out.print("Lecture du texte ......... ");
//            String texte = aef.lireFichierTexte(args[1]);
//            System.out.println("OK");
//
//            System.out.print("Analyse du texte ......... ");
//            String analyse = aef.analyserTexte(texte);
////            String analyse = aef.analyserTexte(texte.toLowerCase());
//            System.out.println("OK");
//
//            System.out.print("Apprentissage du texte ......... ");
//            System.out.println("");
//            aef.perceptronTrain();
//            System.out.println("OK");
//
//            System.out.print("Ecriture du fichier ......... ");
////            aef.ecrireFichierTexte(analyse, args[1] + ".t2");
////            System.out.println("OK");
//            System.out.println("DECOMMANTER");
////            System.out.println(analyse);
//        } else {
//            System.out.println("Usage : java -jar T2.jar <dictionnaire AEF> <fichier texte>");
//        }
//    }
//    public static void main(String[] args) {
//        AEF aef = new AEF();
//        if (args.length == 2) {
//            System.out.println("==== Construction de l'AEF ====");
//
//            // Lecture du dictionnaire
//            System.out.print("Lecture du dictionnaire ......... ");
//            ArrayList<String[]> lignes = aef.lireFichier(args[0]);
//            System.out.println("OK");
//
//            // Encodage des analyses morphologiques
//            System.out.print("Encodage des analyses morphologiques ......... ");
//            CharSequence[] analyses = aef.encodageAnalysesMorphologiques(lignes);
//            System.out.println("OK");
//
//            // Génération de l'AEF
//            System.out.print("Génération de l'AEF ......... ");
//            aef.automaton = Automaton.makeStringUnion(analyses);
//            System.out.println("OK");
//
//            // Sauvegarde de l'AEF
//            System.out.print("Sauvegarde de l'AEF ......... ");
//            aef.sauvegarderAutomaton(args[1]);
//            System.out.println("OK");
//
//        } else {
//            if (args.length == 3 && args[0].matches("-test")) {
//                System.out.println("==== Test de l'AEF ====");
//
//                // Chargement de l'AEF
//                System.out.print("Chargement de l'AEF ......... ");
//                aef.automaton = aef.chargerAutomaton(args[1]);
//                System.out.println("OK");
//
//                // Test du mot
//                System.out.print("Test du mot \"" + args[2] + "\" ......... ");
//                AnalyseMorphologique[] analyses = aef.analyserMot(args[2].toLowerCase());
//                System.out.println("OK\n");
//
//                // Affichage
//                aef.afficherAnalysesMorphologique(analyses);
//            } else {
//                System.out.println("Usage :\nCompilation du dictionnaire : java -jar T1.jar <dictionnaire entrée> <dictionnaire sortie>\nTest de l'AEF : java -jar T1.jar -test <dictionnaire AEF> <mot>");
//            }
//        }
//    }
}
