package industrielangue;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import dk.brics.automaton.*;
import java.util.Set;

/**
 *
 * @author Christian SCHMIDT
 */
public class AEF {
    
    Automaton automaton;

    public AEF() {
    }

    public ArrayList<String> lireFichier(String fichier) {
        BufferedReader br;
        String ligne;
        ArrayList<String> lignes = new ArrayList<>();
        try {
            br = new BufferedReader(new FileReader(fichier));
            while ((ligne = br.readLine()) != null) {
//                encodageLemme(ligne);
                lignes.add(ligne);
            }
            br.close();
        } catch (FileNotFoundException ex) {
            System.out.println("Problème d'ouverture : " + ex.getMessage());
        } catch (IOException ex) {
            System.out.println("Problème de lecture : " + ex.getMessage());
        }
        return lignes;
    }

    public ArrayList<String[]> recupererLemmes(ArrayList<String> lemmes) {
        ArrayList<String[]> lemmesEncodes = new ArrayList<>();
        for (String lemme : lemmes) {
            lemmesEncodes.add(lemme.split("\t"));
//            if (lemme.split("\t").length != 3) {
//                System.out.println(lemme);
//            }
        }
        return lemmesEncodes;
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
//            System.out.println(lemmesEncodes[i]);
            i++;
//            System.out.println("forme : " + forme + "  lemme : " + lemme + "  traits : " + traits + " nbCar : " + nbCaractereAEnlever + " Terminaison : " + terminaison);
        }
        return lemmesEncodes;
    }
    
    public void analyserMot(String mot) {
        State e = automaton.getInitialState();
        int i =0;
        while ((e!=null) && (i<mot.length())) {
            e = transiter(e,mot.charAt(i));
            System.out.print(mot.charAt(i));
            i++;
            
        }
        if (e!=null) {  // Fin du mot
            e = transiter(e, (char)0);
//            System.out.println(e);
            if (e!=null) { // C'est le mot en entier
//                listeAnalyseMorpho = recolterAnalyseMorph(mot, e);
                System.out.println(" -> Le mot existe  :)");
            } 
        } else {
                System.out.println(" -> Le mot n'existe pas :(");
            } 
        // Retourner la liste des analyses possible
    }
    
    public State transiter(State e, char c) {
        Set<Transition> transitions = e.getTransitions();
        for (Transition transition:transitions) {
            if (c>=transition.getMin() && c<=transition.getMax()) {
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
        // Extraction du dictionnaire
        System.out.print("Lecture du dictionnaire ......... ");
        ArrayList<String> lignes = aef.lireFichier("dico2.tsv");
        System.out.println("OK");

        // Recuperation des lemmes
        System.out.print("Recuperation des lemmes ......... ");
        ArrayList<String[]> lemmes = aef.recupererLemmes(lignes);
        System.out.println("OK");

        // Encodage des analyses morphologiques
        System.out.print("Encodage des analyses morphologiques ......... ");
        CharSequence[] lemmesEncodes = aef.encodageAnalysesMorphologiques(lemmes);
        System.out.println("OK");
        
        // Compilation en AEF
        System.out.print("Compilation en AEF ......... ");
        aef.automaton = Automaton.makeStringUnion(lemmesEncodes);
        System.out.println("OK");
        
        // Tests
        aef.analyserMot("pyrolitique");
    }
}
