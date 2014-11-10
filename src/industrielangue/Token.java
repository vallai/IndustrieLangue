package industrielangue;

/**
 *
 * @author Christian SCHMIDT
 */
public class Token {

    private int offset;
    private String forme;
    private AnalyseMorphologique[] analyses;

    public Token(int offset, String forme, AnalyseMorphologique[] analyses) {
        this.offset = offset;
        this.forme = forme;
        this.analyses = analyses;
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
}
