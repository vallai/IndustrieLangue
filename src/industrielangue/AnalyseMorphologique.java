package industrielangue;

/**
 *
 * @author Christian SCHMIDT
 */
public class AnalyseMorphologique {
    private String lemme;
    private String traits;

    public AnalyseMorphologique() {
        this.lemme = null;
        this.traits = null;
    }

    public AnalyseMorphologique(String lemme, String traits) {
        this.lemme = lemme;
        this.traits = traits;
    }

    public String getLemme() {
        return lemme;
    }

    public void setLemme(String lemme) {
        this.lemme = lemme;
    }

    public String getTraits() {
        return traits;
    }

    public void setTraits(String traits) {
        this.traits = traits;
    }
}
