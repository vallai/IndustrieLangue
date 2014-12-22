package industrielangue;

import dk.brics.automaton.Automaton;
import dk.brics.automaton.State;
import dk.brics.automaton.Transition;
import java.util.ArrayList;
import java.util.Set;

/**
 *
 * @author Gat
 */
public class Texte {

    private ArrayList<Token> tokens;
    Automaton automaton;
    private final int nbAttributs = 31;

    /**
     * Analyse un texte et retourne les analyses morphologiques de chaque mot
     * sous la forme : i-lg forme lemme traits
     *
     * @param texte
     * @return
     */
    public Texte(String texte, Automaton automaton) {
        this.automaton = automaton;
        tokens = new ArrayList<>();
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
     * Verifie si le caractère est une lettre
     *
     * @param c
     * @return
     */
    public boolean isLettre(char c) {
        String tmp = c + "";
        return tmp.matches("[a-zA-ZáàâäãåçéèêëíìîïñóòôöõúùûüýÿæœÁÀÂÄÃÅÇÉÈÊËÍÌÎÏÑÓÒÔÖÕÚÙÛÜÝŸÆŒ,'()]");
    }

    public ArrayList<Token> getTokens() {
        return tokens;
    }

    /**
     * Calcul les attributs de chaque toen qui changent au cours du temps
     */
    public void majVecteurs() {
        for (int i = 0; i < tokens.size(); i++) {
            Token currentToken = tokens.get(i);
            boolean[] traits = currentToken.getAttributs();
            if (i >= 1) {
                Token precToken = tokens.get(i - 1);
                // avavav dernier précédé d'un D-Org
                traits[nbAttributs - 4] = precToken.getC_res().equals("D-Org");
                //avav dernier précédé d'un D-Pers
                traits[nbAttributs - 3] = precToken.getC_res().equals("D-Pers");
                //avant dernier précédé d'un D-Loc
                traits[nbAttributs - 2] = precToken.getC_res().equals("D-Loc");
                // dernier précédé d'un D-Loc
                traits[nbAttributs - 1] = precToken.getC_res().equals("E");//-
            }
            currentToken.setAttributs(traits);
        }
    }

    /**
     * Calcul les attributs de chaque token qui ne changent pas au cours des
     * itération
     */
    public void calculVecteurs() {
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
                traits[nbAttributs - 4] = false;
//                //28 précédé d'un D-Pers
                traits[nbAttributs - 3] = false;
//                //29 précédé d'un D-Loc
                traits[nbAttributs - 2] = false;
                //30 précédé d'un D-Loc
                traits[nbAttributs - 1] = false;

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
}
