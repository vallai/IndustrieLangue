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
import java.util.Set;

/**
 * @author Christian SCHMIDT & Gaëtan REMOND
 */
public class AEF {

    Automaton automaton;
    ArrayList<Token> tokens = new ArrayList<>();
    private int nbAttributs = 35;
    private String[] differentesClass = {"E", "D-Org", "D-Pers", "D-Loc", "I-Org", "I-Pers", "I-Loc"};
    
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
        String cs = (c + "").toLowerCase();
        c = cs.toCharArray()[0];
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
    /**
     * Lit un fichier texte encodé en UTF8
     *
     * @param fichier
     * @return
     */
    public String lireFichierTexte(String fichier) {
        String texte = "";
        BufferedReader br;
        String ligne;
        try {
            br = new BufferedReader(new InputStreamReader(new FileInputStream(fichier), "UTF8"));
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
        texte = texte.replace(" E", " E ");
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

    /**
     * Analyse un texte et retourne les analyses morphologiques de chaque mot
     * sous la forme : i-lg forme lemme traits
     *
     * @param texte
     * @return
     */
    public String analyserTexte(String texte) {
        String analyseTexte = "";
        int index = 0;

        Token precToken = null;
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

            if (token.getForme().equals("E")
                    || token.getForme().equals("DORG")
                    || token.getForme().equals("IORG")
                    || token.getForme().equals("DPERS")
                    || token.getForme().equals("IPERS")
                    || token.getForme().equals("DLOC")
                    || token.getForme().equals("ILOC")) {
                tokens.get(tokens.size() - 1).setC_ref(token.getForme());
            } else {
                tokens.add(token);
            }
            token = lire_token(texte, index);
        }
        return analyseTexte;
    }

    /**
     * Transite dans l'AEF pour trouver chaque mot selon la stratégie du mot le
     * plus long
     *
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
                    return new Token(offset, forme, recolterAnalyseMorph(forme, svgEtat), nbAttributs);
                } else {
                    String mot = texte.substring(index);
                    int lastIndex = mot.indexOf(" ");
                    // Si il y a une ponctuation on la retire
                    if (!isLettre(mot.charAt(lastIndex - 1))) {
                        mot = mot.substring(0, lastIndex - 1);
                    } else {
                        mot = mot.substring(0, lastIndex);
                    }
                    return new Token(index, "" + mot, null, nbAttributs);
                }
            } else { // Fin du texte
                return new Token(offset, forme, recolterAnalyseMorph(forme, svgEtat), nbAttributs);
            }
        } else {
            if (i == texte.length()) { // Fin du texte
                return null;
            } else {
                if (!isLettre(texte.charAt(index))) { // Si le caractere n'est pas une lettre, on avance
                    return lire_token(texte, i);
                } else { // Mot inconnu
                    return new Token(index, "" + texte.charAt(index), null, nbAttributs);
                }
            }
        }
    }

    /**
     * Verifie si le caractère est une lettre
     *
     * @param c
     * @return
     */
    public boolean isLettre(char c) {
        String tmp = c + "";
        return tmp.matches("[a-zA-ZáàâäãåçéèêëíìîïñóòôöõúùûüýÿæœÁÀÂÄÃÅÇÉÈÊËÍÌÎÏÑÓÒÔÖÕÚÙÛÜÝŸÆŒ,'()]");
    }

    /////////////////////////////////////////////////////////////////////////////
    ///////////////         T4 : Apprentissage automatique        ///////////////
    /////////////////////////////////////////////////////////////////////////////
    public String perceptronSimple() {
        this.calculVecteurs();
//        for (int i = 0; i < 100; i++) {
            return boucle(10);
//        }
    }

    private String boucle(int fin) {
        int[][] currentMatrice = initMatrice();
        int[][] futureMatrice = initMatrice();

        
        for (int i = 0; i < fin; i++) {

            int compteurBon = 0, compteurMauvais = 0, enDetect = 0, enNonDetect = 0,
                    fauxEnDetect = 0, mauvaisEnDetect = 0, bonEnDetect = 0;
            this.majVecteurs();
            for (Token token : tokens) {
                int classMax = calculClassMax(currentMatrice, token);
                token.setC_res(differentesClass[classMax]);

                if (!token.getC_res().equals(token.getC_ref())) {
                    recalculMatrice(futureMatrice, token);
                    if (i == fin-1) {
                        System.out.println(token.toString());
                    }
                    compteurMauvais++;
                } else {
                    compteurBon++;
                }
                if (!token.getC_res().equals("E") && !token.getC_ref().equals("E")) {
                    enDetect++;
                    if (!token.getC_res().equals(token.getC_ref())) {
                        mauvaisEnDetect++;
                    } else {
                        bonEnDetect++;
                    }
                } else {
                    if (!token.getC_res().equals("E") && token.getC_ref().equals("E")) {
                        fauxEnDetect++;
                    } else {
                        if (token.getC_res().equals("E") && !token.getC_ref().equals("E")) {
                            enNonDetect++;
                        }
                    }
                }

                if (i == fin - 1) {
//                    System.out.println(token.toString());
                }
            }
//            if (i == fin - 1) {
            float precision = (float)compteurBon / (float)(compteurBon+compteurMauvais);
                System.out.println("");
                System.out.println("itération " + i);
                System.out.println("///////////////////////////////////////////////////////////// " + precision);
                System.out.println("bon " + compteurBon + " mauvais " + compteurMauvais);
                System.out.println("EN detect " + enDetect + " \nEN non detect " + enNonDetect + " \nFaux EN detect " + fauxEnDetect 
                        + " \nMauvais EN detect " + mauvaisEnDetect+ " \nBon EN detect " + bonEnDetect);
//            }
            currentMatrice = futureMatrice;
        }
        return matriceToString(currentMatrice);
    }
    /*
     Traits
     0  commence par une majuscule et n'est pas précédé d'un point
     1  est entièrement en majuscule
     2  n'est pas dans le dictionnaire
     3  est quelque chose qui peut être de type Nom:[Mas|Fem]+SG
     4  est quelque chose qui peut être de type Adj:[Mas|Fem|InvGen]+SG
     5  est quelque chose qui peut être de type Con
     6  est quelque chose qui peut être de type Pre
     //    
     7  précédé de quelque chose qui peut être un Det
     8  précédé de quelque chose qui peut être un adjectif
     9  précédé de quelque chose qui peut être une Pre
     10   précédé de quelque chose qui peut être un Con
     11   précédé de quelque chose qui peut être un Nom:[Mas|Fem]+SG
     12   précédé de quelque chose qui peut être un Ver:[a-zA-Z]{4}+SG+P3
     13   précédé de quelque chose qui n'est pas dans le dictionnaire

     14   précédé de quelque chose qui commence par une majuscule
     15 précédé d'un apostrophe
     16  précédé d'une virgule
     17 précédé d'une parenthère ouvrante
     18  précédé de M.
     19  précédé de MM.
    
     20  suivi d'une virgule
     21 suivi d'une parenthère fermante
     22 suivi de quelque chose qui peut être un Det
     23 suivi de quelque chose qui peut être un Con
     24 suivi de quelque chose qui peut être un Adj:[Mas|Fem|InvGen]+SG
     25 suivi de quelque chose qui peut être un Ver:[a-zA-Z]{4}+SG+P3
     26 suivi de quelque chose qui peut être une Pre
     27 précédé d'un D-Org
     28 précédé d'un D-Pers
     29 précédé d'un D-Loc
     30 précédé d'un E
     */

    private void calculVecteurs() {
        for (int i = 0; i < tokens.size(); i++) {
            Token currentToken = tokens.get(i);
            boolean[] traits = currentToken.getAttributs();

            // 1  est entièrement en majuscule
            traits[1] = currentToken.getForme().matches("[A-Z]*"); //+
            // 2  n'est pas dans le dictionnaire
            traits[2] = currentToken.getAnalyses() == null;//-

            if (currentToken.getAnalyses() != null) {
                for (AnalyseMorphologique analyse : currentToken.getAnalyses()) {
                    // 3  est quelque chose qui peut être de type Nom:[Mas|Fem]+SG
                    traits[3] = (!traits[3]) ? analyse.getTraits().matches("Nom:(Mas|Fem)[+]SG") : true;//-
                    // 4  est quelque chose qui peut être de type Adj:[Mas|Fem|InvGen]+SG
                    traits[4] = (!traits[4]) ? analyse.getTraits().matches("Adj:(Mas|Fem|InvGen)[+]SG") : true;//-
                    // 5  est quelque chose qui peut être de type Con
                    traits[5] = (!traits[5]) ? analyse.getTraits().equals("Con") : true;//-
                    // 6  est quelque chose qui peut être de type Pre
                    traits[6] = (!traits[6]) ? analyse.getTraits().equals("Pre") : true;//-
                }
            }
            if (i >= 1) {
                Token precToken = tokens.get(i - 1);
                if (precToken.getAnalyses() != null) {
                    for (AnalyseMorphologique analyse : precToken.getAnalyses()) {
                        // 7  précédé de quelque chose qui peut être un Det
                        traits[7] = (!traits[7]) ? analyse.getTraits().equals("Det") : true;//-
                        // 8  précédé de quelque chose qui peut être un adjectif
                        traits[8] = (!traits[8]) ? analyse.getTraits().matches("Adj.*") : true;//-
                        // 9  précédé de quelque chose qui peut être une Pre
                        traits[9] = (!traits[9]) ? analyse.getTraits().equals("Pre") : true;//-
                        //10   précédé de quelque chose qui peut être un Con
                        traits[10] = (!traits[10]) ? analyse.getTraits().equals("Con") : true;//-
                        //11   précédé de quelque chose qui peut être un Nom:[Mas|Fem]+SG
                        traits[11] = (!traits[11]) ? analyse.getTraits().matches("Nom:(Mas|Fem)[+]SG") : true;//-
                        //12   précédé de quelque chose qui peut être un Ver:[a-zA-Z]{4}+SG+P3
                        traits[12] = (!traits[12]) ? analyse.getTraits().matches("Ver:[a-zA-Z]{4}[+]SG[+]P3") : true;//-
                    }
                } else {
                    //13   précédé de quelque chose qui n'est pas dans le dictionnaire
                    traits[13] = true;//-
                }
                //0 commence par une majuscule et n'est pas précédé d'un point
                traits[0] = currentToken.getForme().substring(0, 1).matches("[A-Z]") && (!precToken.getForme().equals("."));//-
                // 15 précédé d'un apostrophe
                traits[15] = precToken.getForme().equals("'");//-
                // 16  précédé d'une virgule
                traits[16] = precToken.getForme().equals(",");//-
                // 17 précédé d'une parenthère ouvrante
                traits[17] = precToken.getForme().equals("(");//=

                //27 précédé d'un D-Org
                traits[27] = false;
//                //28 précédé d'un D-Pers
                traits[28] = false;
//                //29 précédé d'un D-Loc
                traits[29] = false;
                //33 précédé d'un D-Loc
                traits[33] = false;

                if (i >= 2) {
                    Token precPrecToken = tokens.get(i - 2);
                    //18  précédé de M.
                    traits[18] = precPrecToken.getForme().equals("M") && precToken.getForme().equals(".");//-
                    //19  précédé de MM.
                    traits[19] = precPrecToken.getForme().equals("MM") && precToken.getForme().equals(".");//-
                    //14   précédé de quelque chose qui commence par une majuscule
                    traits[14] = precToken.getForme().substring(0, 1).matches("[A-Z]") && (!precPrecToken.getForme().equals("."));//-
                }
            }
            if (i < tokens.size() - 1) {
                Token nextToken = tokens.get(i + 1);
                // 20  suivi d'une virgule
                traits[20] = nextToken.getForme().equals(",");
                // 21 suivi d'une parenthère fermante
                traits[21] = nextToken.getForme().equals(")");//-
                if (nextToken.getAnalyses() != null) {
                    for (AnalyseMorphologique analyse : nextToken.getAnalyses()) {
                        //22 suivi de quelque chose qui peut être un Det
                        traits[22] = (!traits[22]) ? analyse.getTraits().equals("Det") : true;//-
                        //23 suivi de quelque chose qui peut être un Con
                        traits[23] = (!traits[23]) ? analyse.getTraits().equals("Con") : true;
                        //24 suivi de quelque chose qui peut être un Adj:[Mas|Fem|InvGen]+SG
                        traits[24] = (!traits[24]) ? analyse.getTraits().matches("Adj:(Mas|Fem|InvGen)[+]SG") : true;
                        //25 suivi de quelque chose qui peut être un Ver:[a-zA-Z]{4}+SG+P3
                        traits[25] = (!traits[25]) ? analyse.getTraits().matches("Ver:[a-zA-Z]{4}[+]SG[+]P3") : true;
                        //26 suivi de quelque chose qui peut être une Pre
                        traits[26] = (!traits[26]) ? analyse.getTraits().equals("Pre") : true;
                    }
                }
            }
            currentToken.setAttributs(traits);
        }
    }
    
    private void majVecteurs() {
        for (int i = 0; i < tokens.size(); i++) {
            Token currentToken = tokens.get(i);
            boolean[] traits = currentToken.getAttributs();
            if (i >= 1) {
                Token precToken = tokens.get(i - 1);
                // 27 précédé d'un D-Org
                traits[27] = precToken.getC_res().equals("D-Org");
                //28 précédé d'un D-Pers
                traits[28] = precToken.getC_res().equals("D-Pers");
                //29 précédé d'un D-Loc
                traits[29] = precToken.getC_res().equals("D-Loc");
                // 33 précédé d'un D-Loc
                traits[30] = precToken.getC_res().equals("E");//-
            }
            currentToken.setAttributs(traits);
        }
    }

    public int recalculMatrice(int[][] matrice, Token token) {
        boolean[] attributs = token.getAttributs();
        for (int i = 0; i < attributs.length; i++) {
            if (attributs[i]) {
                matrice[token.getC_resToInt()][i]--;
                matrice[token.getC_refToInt()][i]++;
            }
        }
//        afficheMatrice(matrice);
        return 0;
    }

    private void afficheMatrice(int[][] matrice) {
        for (int i = 0; i < nbAttributs; i++) {
            System.out.print(i + "\t");

        }
        System.out.println("");
        for (int[] ligne : matrice) {
            for (int valeur : ligne) {
                System.out.print(valeur + "\t");
            }
            System.out.println("");
        }
    }

    private String matriceToString(int[][] matrice) {
        String stringMatrice = "";
        for (int[] ligne : matrice) {
            for (int valeur : ligne) {
                stringMatrice += valeur + "\t";
            }
            stringMatrice += "\n";
        }
        return stringMatrice;
    }

    public int calculClassMax(int[][] matrice, Token token) {
        boolean[] attributs = token.getAttributs();
        int[] maxs = new int[7];
        int indexMax = 0;
        int max = 0;
        for (int i = 0; i < maxs.length; i++) {
            for (int j = 0; j < matrice[i].length; j++) {
                if (attributs[j]) {
                    maxs[i] += matrice[i][j];
                }
            }
//            System.out.println(maxs[i]);
            if (max < maxs[i]) {
                indexMax = i;
                max = maxs[i];
//                System.out.println(max + " - " + maxs[i]);
            }
//            System.out.println("indexMax = " + indexMax + " max = " + max);
        }
        return indexMax;
    }

    public int[][] initMatrice() {
        int[][] matrice = new int[7][nbAttributs];
        for (int[] matrice1 : matrice) {
            for (int j = 0; j < matrice1.length; j++) {
                matrice1[j] = 1;
            }
        }
        return matrice;
    }

    public static void main(String[] args) {
        AEF aef = new AEF();
        if (args.length == 5 && args[0].equals("-train")) {
            if (args[1].equals("-percept")) {
                System.out.println("==== Segmentation et analyse morphologique de textes ====");

                System.out.print("Chargement de l'AEF ......... ");
                aef.automaton = aef.chargerAutomaton("../dico.aef");
                System.out.println("OK");

                System.out.print("Lecture du texte ......... ");
                String texte = aef.lireFichierTexte(args[2]);
                System.out.println("OK");

                System.out.print("Parsage du texte ......... ");
                String texteParse = aef.parserTexte(texte);
                System.out.println("OK");

                System.out.print("Analyse du texte ......... ");
                String analyse = aef.analyserTexte(texteParse);
//            String analyse = aef.analyserTexte(texte.toLowerCase());
                System.out.println("OK");

                System.out.print("Apprentissage du texte ......... ");
                System.out.println("");
                String enPercept = aef.perceptronSimple();
                System.out.println("OK");

                System.out.print("Ecriture du fichier ......... ");
//            aef.ecrireFichierTexte(analyse, args[1] + ".t2");
                aef.ecrireFichierTexte(enPercept, args[4]);
                System.out.println("OK");
//            System.out.println(analyse);
            } else {
                if (args[1].equals("-maxent")) {
                    System.out.println("méthode -maxent pas encore implémentée");
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
//            aef.perceptronSimple();
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
