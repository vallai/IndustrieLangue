package industrielangue;

import java.util.ArrayList;

/**
 *
 * @author Gat
 */
public class PerceptClassifier {
    private final String[] differentesClass = {"E", "D-Org", "D-Pers", "D-Loc", "I-Org", "I-Pers", "I-Loc"};
    private final int nbAttributs = 31;
    private Texte texte;
    public PerceptClassifier(Texte tokens) {
        this.texte = tokens;
    }
    

    /**
     * Méthode d'apprentissage par perceptron
     *
     * @return int[][] matrice d'apprentissage
     */
    public String perceptronTrain() {
        // Calcul du vecteur invariable
        texte.calculVecteurs();
        int[][] futureMatrice = initMatrice();
        int[][] currentMatrice = initMatrice();
        int nbIterations = 10;
        int compteurBon = 0, compteurMauvais = 0;
        for (int i = 0; i < nbIterations; i++) {
            compteurBon = 0;
            compteurMauvais = 0;
            // Calcul du vecteur variable
            texte.majVecteurs();
            for (Token token : texte.getTokens()) {
                int classMax = calculClassMax(currentMatrice, token);
                token.setC_res(differentesClass[classMax]);

                if (!token.getC_res().equals(token.getC_ref())) {
                    recalculMatrice(futureMatrice, token);
                    compteurMauvais++;
                } else {
                    compteurBon++;
                }
            }
            currentMatrice = futureMatrice;
        }
        float precision = (float) compteurBon / (float) (compteurBon + compteurMauvais);
        System.out.println("Précision : " + precision);
        return matriceToString(currentMatrice);
    }
    
    /**
     * Méthode d'annotation par percetron simpl
     *
     * @param matrice la matrice utilisée pour calculer le argmax
     * @return
     */
    public String perceptronAnnot(int[][] matrice) {
        texte.calculVecteurs();
        String res = "";
        int compteurBon = 0, compteurMauvais = 0;
        texte.majVecteurs();
        for (Token token : texte.getTokens()) {
            int classMax = calculClassMax(matrice, token);
            token.setC_res(differentesClass[classMax]);
            res += token.getForme() + "\t" + token.getC_res() + "\n";

            if (token.getC_ref() != null) {
                if (!token.getC_res().equals(token.getC_ref())) {
                    compteurMauvais++;
                } else {
                    compteurBon++;
                }
            }
        }
        if (compteurBon + compteurMauvais > texte.getTokens().size() - 10) {
            float precision = (float) compteurBon / (float) (compteurBon + compteurMauvais);
            System.out.println("Précision : " + precision);
        }
        return res;
    }
    
    /**
     * Met à jour la matrice
     *
     * @param matrice matrice à mettre à jour
     * @param token token possédant le c_res et c_ref à modifier
     */
    private void recalculMatrice(int[][] matrice, Token token) {
        boolean[] attributs = token.getAttributs();
        for (int i = 0; i < attributs.length; i++) {
            if (attributs[i]) {
                matrice[token.getC_resToInt()][i]--;
                matrice[token.getC_refToInt()][i]++;
            }
        }
//        afficheMatrice(matrice);
    }

    /**
     * Affiche la matrice passée en paramètre dans le terminal
     *
     * @param matrice matrice à afficher
     */
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

    /**
     * Transforme la matrice passée en paramètre en string pour l'écrire dans un
     * fichier
     *
     * @param matrice matrice à passer en string
     * @return la matrice sous forme de string
     */
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
    
    /**
     * Initialise la matrice
     *
     * @return retourne la matrice avec tous les pids à 1
     */
    private int[][] initMatrice() {
        int[][] matrice = new int[7][nbAttributs];
        for (int[] matrice1 : matrice) {
            for (int j = 0; j < matrice1.length; j++) {
                matrice1[j] = 1;
            }
        }
        return matrice;
    }
    
    /**
     * Calcul la class qui a le plus de chance d'être la bone pour un token
     * donné paramètre
     *
     * @param matrice matrice de poids
     * @param token token donc on veut le argmax
     * @return
     */
    private int calculClassMax(int[][] matrice, Token token) {
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
            if (max < maxs[i]) {
                indexMax = i;
                max = maxs[i];
            }
        }
        return indexMax;
    }
}
