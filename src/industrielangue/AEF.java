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

/**
 * Class de chargement du dictionnaire
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

    public Automaton getAutomaton() {
        return automaton;
    }

    public void setAutomaton(Automaton automaton) {
        this.automaton = automaton;
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
    public void chargerAutomaton(String fichier) {
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
        automaton = automat;
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
    ///////////////         T4 : Apprentissage automatique        ///////////////
    /////////////////////////////////////////////////////////////////////////////

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
}
