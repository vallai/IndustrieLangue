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
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author Christian SCHMIDT & Gaëtan REMOND
 */
public class AEF {

    Automaton automaton;

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
            while ((forme.length() >= nbCommun) && (lemme.length() >= nbCommun) && forme.substring(0, nbCommun).matches(lemme.substring(0, nbCommun))) {
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
     * Recherche le mot donné en paramètre et retourne une liste d'analyses
     * morphologique
     *
     * @param mot
     * @return analyses morphologique
     */
    public AnalyseMorphologique[] analyserMot(String mot) {
        State e = automaton.getInitialState();
        int i = 0;
        while ((e != null) && (i < mot.length())) {
            e = transiter(e, mot.charAt(i));
            i++;
        }
        if (e != null) {  // Fin du mot
            e = transiter(e, (char) 0);
            if (e != null) { // C'est le mot en entier
                return (recolterAnalyseMorph(mot, e));
            }
        }
        return null;
    }

    /**
     * Transite dans l'automate à la recherche du prochain caractère
     *
     * @param etat actuel
     * @param caractere recherché
     * @return etat suivant
     */
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
     * Récupère les analyses morphologiques d'un mot
     *
     * @param mot
     * @param etat correspondant au caractere 0
     * @return analyses morphologiques
     */
    public AnalyseMorphologique[] recolterAnalyseMorph(String mot, State e) {
        // On récupère les chaines complètes
        ArrayList<String> liste = new ArrayList();
        for (Transition transition : e.getTransitions()) {
            String analyse = "";
            analyse += transition.getMin();
            liste = recolterCaracteres(analyse, liste, transition.getDest());
        }

        // On les convertit en AnalyseMorphologique
        AnalyseMorphologique[] listeAnalyses = new AnalyseMorphologique[liste.size()];
        for (int i = 0; i < liste.size(); i++) {
            String lemme = mot.substring(0, mot.length() - (int) liste.get(i).charAt(0));
            String traits = "";
            boolean debutTraits = false;

            for (int j = 1; j < liste.get(i).length(); j++) {
                char c = liste.get(i).charAt(j);
                if (c != (char) 0) {
                    if (debutTraits) {
                        traits += c;
                    } else {
                        lemme += c;
                    }
                } else {
                    debutTraits = true;
                }
            }
            listeAnalyses[i] = new AnalyseMorphologique(lemme, traits);
        }
        return listeAnalyses;
    }

    /**
     * Méthode recursive pour recuperer tous les caractères à partir d'un état
     *
     * @param analyse
     * @param liste
     * @param e
     * @return chaines de caractères recupérées
     */
    public ArrayList<String> recolterCaracteres(String analyse, ArrayList<String> liste, State e) {
        String svg = analyse;

        for (Transition transition : e.getTransitions()) {
            analyse = svg + transition.getMin();
            if (transition.getDest().getTransitions().isEmpty()) {
                liste.add(analyse);
            }
            liste = recolterCaracteres(analyse, liste, transition.getDest()); // Appel recursif
        }
        return liste;
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
    
    public String lireFichierTexte(String fichier) {
        String texte = "";
        BufferedReader br;
        String ligne;
        try {
            br = new BufferedReader(new FileReader(fichier));
            while ((ligne = br.readLine()) != null) {
                texte += ligne;
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

    /**
     * Analyse un texte et retourne les analyses morphologiques de chaque mot sous la forme :
     * i-lg forme lemme traits
     * @param texte
     * @return 
     */
    public String analyserTexte(String texte) {
        String analyseTexte = "";
        int index = 0;

        Token token = lire_token(texte, index);
        while (token != null) {
            String affichageToken = token.getOffset() + "-" + token.getForme().length() + "\t" + token.getForme();
            if (token.getAnalyses() == null) {
                analyseTexte += affichageToken + "\n";
            } else {
                for (AnalyseMorphologique analyse : token.getAnalyses()) {
                    analyseTexte += affichageToken + "\t" + analyse.getLemme() + "\t" + analyse.getTraits() + "\n";
                }
            }
            analyseTexte += "\n";
            index = token.getOffset() + token.getForme().length();
            token = lire_token(texte, index);
        }
        return analyseTexte;
    }

    /**
     * Transite dans l'AEF pour trouver chaque mot selon la stratégie du 
     * mot le plus long
     * @param texte
     * @param index
     * @return 
     */
    public Token lire_token(String texte, int index) {
        State e = automaton.getInitialState();
        int i = index;

        State svgEtat = null;
        int svgIndex = 0;

        // On transite sur le texte selon la strategie du mot le plus long
        while (e != null && i < texte.length()) {
            e = transiter(e, texte.charAt(i));

            if (e != null) { // Fin de la correspondance des caractères
                // Si l'etat suivant est 0 -> fin du mot donc sauvegarde de l'etat et de l'indice
                State etat0 = transiter(e, (char) 0);
                if (etat0 != null) {
                    svgEtat = etat0;
                    svgIndex = i;
                }
            }
            i++;
        }

        if (svgEtat != null) { // Mot reconnu
            int offset = index;
            String forme = texte.substring(index, svgIndex + 1);

            if (svgIndex + 1 < texte.length()) { // Fin du texte au caractère suivant ?
                if (!isLettre(texte.charAt(svgIndex + 1))) { // Si le caractère suivant n'est pas une lettre -> Mot connu
                    return new Token(offset, forme, recolterAnalyseMorph(forme, svgEtat));
                } else {
                    String mot = texte.substring(index);
                    int lastIndex = mot.indexOf(" ");
                    // Si il y a une ponctuation on la retire
                    if (!isLettre(mot.charAt(lastIndex - 1))) {
                        mot = mot.substring(0, lastIndex - 1);
                    } else {
                        mot = mot.substring(0, lastIndex);
                    }
                    return new Token(index, "" + mot, null);
                }
            } else { // Fin du texte
                return new Token(offset, forme, recolterAnalyseMorph(forme, svgEtat));
            }
        } else {
            if (i == texte.length()) { // Fin du texte
                return null;
            } else {
                if (!isLettre(texte.charAt(index))) { // Si le caractere n'est pas une lettre, on avance
                    return lire_token(texte, i);
                } else { // Mot inconnu
                    return new Token(index, "" + texte.charAt(index), null);
                }
            }
        }
    }

    /**
     * Verifie si le caractère est une lettre
     * @param c
     * @return 
     */
    public boolean isLettre(char c) {
        String tmp = c + "";
        return tmp.matches("[a-zA-ZáàâäãåçéèêëíìîïñóòôöõúùûüýÿæœÁÀÂÄÃÅÇÉÈÊËÍÌÎÏÑÓÒÔÖÕÚÙÛÜÝŸÆŒ]");
    }

    public static void main(String[] args) {
        AEF aef = new AEF();
        if (args.length == 2) {
            System.out.println("==== Segmentation et analyse morphologique de textes ====");

            System.out.print("Chargement de l'AEF ......... ");
            aef.automaton = aef.chargerAutomaton(args[0]);
            System.out.println("OK");

            System.out.print("Lecture du texte ......... ");
            String texte = aef.lireFichierTexte(args[1]);
            System.out.println("OK");

            System.out.print("Analyse du texte ......... ");
            String analyse = aef.analyserTexte(texte.toLowerCase());
            System.out.println("OK");
            
            System.out.print("Ecriture du fichier ......... ");
            aef.ecrireFichierTexte(analyse, args[1]+".t2");
            System.out.println("OK");
            
//            System.out.println(analyse);
        }
    }
}
