package industrielangue;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;

/**
 * Class static qui permet la manipulation de fichier
 * @author Christian SCHMIDT & Gaëtan REMOND
 */
public class AccesFichiers {

    /**
     * Lit un fichier texte encodé en UTF8
     *
     * @param fichier
     * @return
     */
    public static String lireFichierTexte(String fichier, String separateur) {
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
    public static String parserTexte(String texte) {
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
    public static void ecrireFichierTexte(String texte, String fichier) {
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
}
