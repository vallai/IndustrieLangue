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
import java.util.Set;

/**
 *
 * @author Christian SCHMIDT
 */
public class AEF {

    Automaton automaton;

    public AEF() {
    }

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

    public CharSequence[] encodageAnalysesMorphologiques(ArrayList<String[]> lemmes) {
        CharSequence[] lemmesEncodes = new CharSequence[lemmes.size()];
        // Pour chaque entrée
        int i = 0;
        for (String[] entree : lemmes) {
            String forme = entree[0];
            String lemme = entree[1];
            String traits = entree[2];

            // On cherche le nombre de caractères communs
            int nbCommun = 0;
            while ((forme.length() >= nbCommun) && (lemme.length() >= nbCommun) && forme.substring(0, nbCommun).matches(lemme.substring(0, nbCommun))) {
                nbCommun++;
            }

            // Pour en déduire le nombre de caractère à enlever et la terminaison
            int nbCaractereAEnlever = forme.length() - (nbCommun - 1);
            String terminaison = lemme.substring(nbCommun - 1);
            // Construction de la chaine
            lemmesEncodes[i] = (CharSequence) (forme + (char) 0 + (char) nbCaractereAEnlever + terminaison + (char) 0 + traits);
            i++;
//            System.out.println("forme : " + forme + "  lemme : " + lemme + "  traits : " + traits + " nbCar : " + nbCaractereAEnlever + " Terminaison : " + terminaison);
        }
        return lemmesEncodes;
    }

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

    public void analyserMot(String mot) {
        State e = automaton.getInitialState();
        int i = 0;
        while ((e != null) && (i < mot.length())) {
            e = transiter(e, mot.charAt(i));
            System.out.print(mot.charAt(i));
            i++;

        }
        if (e != null) {  // Fin du mot
            e = transiter(e, (char) 0);
//            System.out.println(e);
            if (e != null) { // C'est le mot en entier
//                String[] listeAnalyseMorpho = recolterAnalyseMorph(mot, e);
                System.out.println(" -> Le mot existe  :)");
            }
        } else {
            System.out.println(" -> Le mot n'existe pas :(");
        }
        // Retourner la liste des analyses possible
    }

    public State transiter(State e, char c) {
        Set<Transition> transitions = e.getTransitions();
        for (Transition transition : transitions) {
            if (c >= transition.getMin() && c <= transition.getMax()) {
                return transition.getDest();
            }
        }
        return null; // il n'y a pas de transition
    }

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        AEF aef = new AEF();
        if (args.length == 2) {
            System.out.println("==== Construction de l'AEF ====");

            // Extraction du dictionnaire
            System.out.print("Lecture du dictionnaire ......... ");
            ArrayList<String[]> lignes = aef.lireFichier(args[0]);
            System.out.println("OK");

            // Encodage des analyses morphologiques
            System.out.print("Encodage des analyses morphologiques ......... ");
            CharSequence[] analyses = aef.encodageAnalysesMorphologiques(lignes);
            System.out.println("OK");

            // Génération de l'AEF
            System.out.print("Génération de l'AEF ......... ");
            aef.automaton = Automaton.makeStringUnion(analyses);
            System.out.println("OK");

            // Sauvegarde de l'AEF
            System.out.print("Sauvegarde de l'AEF ......... ");
            aef.sauvegarderAutomaton(args[1]);
            System.out.println("OK");

        } else {
            if (args.length == 3 && args[0].matches("-test")) {
                System.out.println("==== Test de l'AEF ====");

                // Chargement de l'AEF
                System.out.print("Chargement de l'AEF ......... ");
                aef.automaton = aef.chargerAutomaton(args[1]);
                System.out.println("OK");

                // Test du mot
                System.out.print("Test du mot \"" + args[2] + "\" ......... ");
                aef.analyserMot(args[2]);
            } else {
                System.out.println("Usage :\nCompilation du dictionnaire : java -jar T1.jar <dictionnaire entrée> <dictionnaire sortie>\nTest de l'AEF : java -jar T1.jar -test <dictionnaire AEF> <mot>");
            }
        }
    }
}
