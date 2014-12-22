package industrielangue;

/**
 *
 * @author Christian SCHMIDT
 */
public class Token {

    private int offset;
    private String forme;
    private AnalyseMorphologique[] analyses;
    private boolean[] attributs;
//    {false, false, false, false, false, false, false, false, false, false, false, false, false};
    private String c_res = "E";
    private String c_ref = null;

    public Token(int offset, String forme, AnalyseMorphologique[] analyses, int nbAttributs) {
        this.offset = offset;
        this.forme = forme;
        this.analyses = analyses;
        this.attributs = new boolean[nbAttributs];
    }

    public int getOffset() {
        return offset;
    }

    public void setOffset(int offset) {
        this.offset = offset;
    }

    public String getForme() {
        return forme;
    }

    public void setForme(String forme) {
        this.forme = forme;
    }

    public AnalyseMorphologique[] getAnalyses() {
        return analyses;
    }

    public void setAnalyses(AnalyseMorphologique[] analyses) {
        this.analyses = analyses;
    }

    public boolean[] getAttributs() {
        return attributs;
    }

    public void setAttributs(boolean[] attributs) {
        this.attributs = attributs;
    }

    public String getC_res() {
        return c_res;
    }

    public int getC_resToInt() {
        switch (c_res) {
            case "E":
                return 0;
            case "D-Org":
                return 1;
            case "D-Pers":
                return 2;
            case "D-Loc":
                return 3;
            case "I-Org":
                return 4;
            case "I-Pers":
                return 5;
            case "I-Loc":
                return 6;
        }
        return -1;
    }

    public void setC_res(String c_res) {
        this.c_res = c_res;
    }

    public String getC_ref() {
        return c_ref;
    }

    public int getC_refToInt() {
        switch (c_ref) {
            case "E":
                return 0;
            case "D-Org":
                return 1;
            case "D-Pers":
                return 2;
            case "D-Loc":
                return 3;
            case "I-Org":
                return 4;
            case "I-Pers":
                return 5;
            case "I-Loc":
                return 6;
        }
        return -1;
    }

    public void setC_ref(String c_ref) {
        switch (c_ref) {
            case "E":
                this.c_ref = "E";
                break;
            case "DORG":
                this.c_ref = "D-Org";
                break;
            case "DPERS":
                this.c_ref = "D-Pers";
                break;
            case "DLOC":
                this.c_ref = "D-Loc";
                break;
            case "IORG":
                this.c_ref = "I-Org";
                break;
            case "IPERS":
                this.c_ref = "I-Pers";
                break;
            case "ILOC":
                this.c_ref = "I-Loc";
                break;
        }
    }
    
    @Override
    public String toString() {
        String retour = this.offset + "\t" + this.forme + "\t";
        retour += (forme.length() < 8) ? "\t" : "";
        int index = 5;
        if (this.analyses != null) {
            for (AnalyseMorphologique analyse : analyses) {
//                retour += analyse.getTraits().substring(0, 3) + "\t";
//                retour += analyse.getTraits() + "\t";
                index--;
            }
        }
        for (int i = 0; i < index; i++) {
//            retour += "\t";

        }
//        for (int i = 0; i < this.attributs.length; i++) {
//            retour += ((this.attributs[i])?"1":"0") + ",";
//        }
        retour += " " + this.c_res;
        retour += " " + this.c_ref;
        
//        if (this.analyses != null) {
//            for (AnalyseMorphologique analyse : analyses) {
////                retour += analyse.getTraits().substring(0, 3) + "\t";
//                retour += "\t" +analyse.getTraits() + "\t";
//                index--;
//            }
//        }
        return retour; //To change body of generated methods, choose Tools | Templates.
    }

}
