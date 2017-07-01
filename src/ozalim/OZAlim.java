package ozalim;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedList;
import java.util.Properties;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.impl.HttpSolrClient;
import org.apache.solr.client.solrj.request.AbstractUpdateRequest;
import org.apache.solr.client.solrj.request.ContentStreamUpdateRequest;
import org.apache.solr.client.solrj.response.SolrPingResponse;
import org.apache.solr.common.params.ModifiableSolrParams;
import org.apache.solr.common.util.NamedList;

/*******************
 *
 * @author tondeurh
 *******************/
public class OZAlim {

    //Objets SolR
    SolrClient client;
    SolrPingResponse ping;
    String urlString;
    ContentStreamUpdateRequest req;
    
    //Objets OZAlim
    LinkedList<Document> document=new LinkedList();
    boolean debug=false;
    String scanpath="//blc087/Demat";
    String bal="1";
    
//logger par les api java
    static final Logger logger = Logger.getLogger(OZAlim.class.getName());

    
    //Objets BDD
    DataBase db;
    String jdbcurl="";
    String dbuser="arnumuser";
    String dbpwd="65xmMAX4";
    String sql="";
    String sqlupdate="";
    
    //constantes
    int STATUSOK=0;
    
    
    /***********************************************************
     * logger_init
     * procedure qui permet d'initialiser le stream des log vers
     * un fichier externe sur disque....
     ************************************************************/
    private void logger_init(){
             //parametrage du logger
        try {
            logger.setUseParentHandlers(false);
            FileHandler fh = new FileHandler("OZAlim%u.log", 0, 1, true);
            fh.setFormatter(new SimpleFormatter());
            logger.addHandler(fh);
        } catch (IOException ioe) {
            Logger.getLogger(OZAlim.class.getName()).log(Level.SEVERE, null, ioe);
        }
    }

    
    /*****************************************
     * @param args the command line arguments
     * @throws java.io.IOException
     *****************************************/
    public static void main(String[] args) throws IOException 
    {
        try 
        {
            OZAlim oZAlim = new OZAlim();
        } catch (SolrServerException ex) 
        {
            logger.log(Level.SEVERE, "Main entry point!", ex);
        }
    }

   
    /**********************************************************
     * Constructeur 
     * @throws org.apache.solr.client.solrj.SolrServerException
     * @throws java.io.IOException
     **********************************************************/
    public OZAlim() throws SolrServerException, IOException 
    {
        //initialisation du logger
        //permet de gerer le fichier log...
        logger_init();
        
        lire_properties();
        client=new HttpSolrClient.Builder(urlString).build();
        
        ping=client.ping();
        if (test_ping()==STATUSOK)
        {
            alimenter();
        }        
    } //fin du constructeur
       
  
    /*********************
     * tester si le status de la connexion est OK
     * @return 
     *********************/
    private int test_ping() 
    {
        int status=ping.getStatus();
        if (debug) logger.log(Level.INFO, "status du ping = {0}", status);
        return status;
    }

//    private void question() throws SolrServerException, IOException 
//    {
//        ModifiableSolrParams solrParams = new ModifiableSolrParams();
//        solrParams.add("q", "*:*");
//        QueryResponse out = client.query(solrParams);
//        System.out.println(out.getResponse().toString());
//    }
    
    
/******************************
 * 
 * @throws IOException
 * @throws SolrServerException 
 ******************************/
 private void alimenter() throws SolrServerException 
    {
        String auteur, path,titre,uf,patient,categorie,datecreation,iddocument;
        //lister tous les fichiers à alimenter a partir de la BDD
        //connect database
        db=new DataBase(DataBase.ORADRIV);
        if (debug) logger.log(Level.INFO,"Chargement driver Oracle OK "+db.ORADRIV);
        //connecter la base de données
        db.connect_db(jdbcurl, dbuser, dbpwd);
        if (debug) logger.log(Level.INFO,"Connection à la base de données OK");
        //requeter selon la mention sql du properties
        db.query(sql);
        if (debug) logger.log(Level.INFO, "requete OK {0}", sql);
        //recuperer tous les éléments
        try {
            while (db.getResultSet().next()){
                auteur=db.getResultSet().getString(1);
                if (auteur==null) auteur="inconnu";
                path=db.getResultSet().getString(2);
                if (path==null) path="vide";
                titre=db.getResultSet().getString(3);
                if (titre==null) titre="sans titre";
                uf=db.getResultSet().getString(4);
                if (uf==null) uf="0000";
                patient=db.getResultSet().getString(5);
                if (patient==null) patient="patient inconnu";
                categorie=db.getResultSet().getString(6);
                if (categorie==null) categorie="Sans catégorie";
                datecreation=db.getResultSet().getString(7);
                if (datecreation==null) datecreation="date création inconnue";
                iddocument=db.getResultSet().getString(8);
                if (iddocument==null) iddocument="0";
                document.add(new Document(auteur,path,titre,uf,patient,categorie,datecreation,iddocument));
                if (debug) logger.log(Level.INFO, "info \n{0}\n{1}\n{2}\n{3}\n{4}\n{5}\n{6}", new Object[]{auteur, path, titre, uf, patient, categorie, datecreation});
            }
            //fermer DB
            db.close_db();
            if (debug) logger.log(Level.INFO,"Fermeture de la base de données");
        } catch (SQLException ex) {
            logger.log(Level.SEVERE, "erreur DB...", ex);
        }
        
        //methode d'indexation des documents dans solr
        indexer_documents();
    } //fin alimentation
    
    
    /**********************************************************
     * permet de lire le fichier propriétés qui doit comporter 
     * le même nom que l'application 
     **********************************************************/
    private void lire_properties() {
        try {
            Properties pp = new Properties();
            pp.load(new FileReader("oZAlim.properties"));
            //base de données ArNum
            jdbcurl = pp.getProperty("ArNumUrl", "jdbc:oracle:thin:@nts85.ch-v.net:1521:ARNUM");
            dbuser=pp.getProperty("user", "arnumuser");
            dbpwd=pp.getProperty("pwd", "65xmMAX4");
            
            sql=pp.getProperty("sql", "no sql");
            sqlupdate=pp.getProperty("sqlupdate", "no update");
            
            //mode debug pour les test en dev
            debug = Boolean.valueOf(pp.getProperty("debug", "false"));
            
            //chemin source des fichiers
            scanpath = pp.getProperty("scanpath", "//bl087/Demat");

            //url de connexion solr
             urlString=pp.getProperty("solR","http://localhost:8983/solr/BiblioHT");
        } catch (FileNotFoundException ex) {
            logger.log(Level.SEVERE, "Fichier oZAlim.properties non trouvé", ex);
        } catch (IOException ex) {
            logger.log(Level.SEVERE, "Erreur de lecture du fichier oZAlim.properties", ex);
        }
    } //fin lire_properties

    
    /*********************************
     * methode d'indexation des documents
     * dans solr insertion des literaux
     *********************************/
    private void indexer_documents() 
    {   
        if (debug) logger.log(Level.INFO,"Debut indexation des documents");
        //pour chaque fichier raliser l'alimentation
        document.stream().filter((doc) -> (doc.getPath().compareTo("vide")!=0)).forEach((doc) -> {
            try {
                ModifiableSolrParams params=new ModifiableSolrParams();
                //ajouter l'auteur de type literal TBLDOCUMENTS.AUTEUR
                params.add("literal.auteur",doc.getAuteur());
                //ajouter le titre de type literal TBLDOCUMENTS.DESCDOC
                params.add("literal.titre",doc.getTitre());
                //ajouter le path du fichier TBLDOCUMENTS.NOMDOCUMENT
                params.add("literal.nomdocument",doc.getPath());
                params.add("literal.chemin",scanpath+"/"+doc.getPath());
                //ajouter UF
                params.add("literal.uf",doc.getUf());
                //ajouter patient chaine construire par la requete
                params.add("literal.patient",doc.getPatient());
                //ajouter la date au format iso8601
                LocalDateTime dateTime = LocalDateTime.parse(doc.getDateCreation(), DateTimeFormatter.ofPattern("dd/MM/yy HH:mm:ss"));
                params.add("literal.datecreationdoc",dateTime.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
                params.add("extractOnly","false");
                
                if (debug) logger.log(Level.INFO,params.toQueryString());
                
                //utiliser le profil update/extract du maaged-schema solr du core
                req = new ContentStreamUpdateRequest("/update/extract");
                req.addFile(new File(scanpath+"/"+doc.getPath()),doc.getCategorie());
                req.setParams(params);
                if (debug) logger.log(Level.INFO,"Query solr OK");
                //commit l'analyse
                //req.setAction(AbstractUpdateRequest.ACTION.COMMIT, true, true);

                if (debug) logger.log(Level.INFO,"Commit solr OK");
               //valeur par défaut de la bal
                bal="1";
                
                    if (debug)
                    {
                        NamedList<Object> result = client.request(req);
                        logger.log(Level.INFO, "Result index : {0}", result); 
                    }
      
            } catch (IOException ex) {
                logger.log(Level.SEVERE, "Erreur indexation document...", ex);
                bal="2";
            } catch (SolrServerException ex) {
                logger.log(Level.SEVERE, "Solr Server erreur", ex);
                bal="3";
            }
            finally
            {
                    //mettre à jour la bal arnum
                    //connecter la base de données
                    db.connect_db(jdbcurl, dbuser, dbpwd);
                    db.update("UPDATE "+sqlupdate+" SET GEN='"+bal+"', DATEGEN=sysdate WHERE IDDOCUMENT='"+doc.getIddocument()+"'");
                    if (debug) logger.log(Level.INFO, "UPDATE {0} SET GEN=''{1}'', DATEGEN=sysdate WHERE IDDOCUMENT=''{2}''", new Object[]{sqlupdate, bal, doc.getIddocument()});
                    //fermer la connexion
                    db.close_db(); 
            }
        }); //verifier que le chemin du fichier est ?valide? => !=vide
   
         //commit l'analyse en fin de liste...
         req.setAction(AbstractUpdateRequest.ACTION.COMMIT, true, true);
    }

}