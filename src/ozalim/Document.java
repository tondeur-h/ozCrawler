package ozalim;

/**
 *
 * @author tondeurh
 */
public class Document {

    public String getIddocument() {
        return iddocument;
    }

    public void setIddocument(String iddocument) {
        this.iddocument = iddocument;
    }

    private String iddocument;
    private String auteur;
    private String path;
    private String titre;
    private String uf;
    private String patient;
    private String categorie;
    private String dateCreation;

    public String getAuteur() {
        return auteur;
    }

    public void setAuteur(String auteur) {
        this.auteur = auteur;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public String getTitre() {
        return titre;
    }

    public void setTitre(String titre) {
        this.titre = titre;
    }

    public String getUf() {
        return uf;
    }

    public void setUf(String uf) {
        this.uf = uf;
    }

    public String getPatient() {
        return patient;
    }

    public void setPatient(String patient) {
        this.patient = patient;
    }

    public String getCategorie() {
        return categorie;
    }

    public void setCategorie(String categorie) {
        this.categorie = categorie;
    }

    public String getDateCreation() {
        return dateCreation;
    }

    public void setDateCreation(String dateCreation) {
        this.dateCreation = dateCreation;
    }

    public Document(String auteur, String path, String titre, String uf, String patient, String categorie, String dateCreation,String iddocument) {
        this.iddocument=iddocument;
        this.auteur = auteur;
        this.path = path;
        this.titre = titre;
        this.uf = uf;
        this.patient = patient;
        this.categorie = categorie;
        this.dateCreation = dateCreation;
    }
    
}
