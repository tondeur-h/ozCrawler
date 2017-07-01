package ozalim;

import java.io.IOException;
import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;
import oracle.jdbc.OracleTypes;


/********************
 * @author tondeur-h
 * @version 2015
 ********************/

/****************************************************************************
 * La Classe DataBase, permet la gestion des accés a la base de données.
 * Elle propose le chargement du Driver, la méthode de connection à la base
 * les méthodes pour exécuter une requete de type SELECT, INSERT UPDATE
 * et les méthodes d'appel de procédures stockées, avec 0, 1 ou 2 paramétres.
 ****************************************************************************/
public class DataBase {

//variables gestion SGBD
    private Connection con;
    private Statement st;
    private ResultSet rs;
    private CallableStatement cst; //appel de procédures

    private String url, user, password;
    private String driverDB;


    public static final String ORADRIV="oracle.jdbc.driver.OracleDriver";

//logger par les api java utile pour le log fichier
static final Logger logger = Logger.getLogger(DataBase.class.getName());


    /**
     * construction de la classe Database, il attend en paramétre
     * la chaine de chargement du driver, le constructeur charge le driver
     * et prépare le fichier log sur disque.
     * @param driver String
     */
    public DataBase(String driver) {
        //chargement du drivers DB
        try{
            driverDB=driver;
            //charger le drivers DB pour toute la session Scandoc
            Class.forName(driverDB);
        }catch (ClassNotFoundException ex){
            ex.printStackTrace();
        }

            //parametrage du logger redirection vers un fichier
            try{
                logger.setUseParentHandlers(false);
                FileHandler fh=new FileHandler("OZAlim%u.log",0,1,true);
                fh.setFormatter(new SimpleFormatter());
                logger.addHandler(fh);
            }catch (IOException ioe){Logger.getLogger(DataBase.class.getName()).log(Level.SEVERE, null, ioe);}

    } //fin du constructeur


    /*********************************************************
     * Connection a la base de données
     * @param jdbcurl String
     * @param dbuser String
     * @param dbpwd String
     * @return boolean
     *
     *********************************************************/
     public boolean connect_db(String jdbcurl, String dbuser,String dbpwd){
         setUrl(jdbcurl);
         setUser(dbuser);
         setPassword(dbpwd);

         try {
            con = DriverManager.getConnection(getUrl(), getUser(), getPassword());
        } catch (SQLException ex){ex.printStackTrace();logger.log(Level.SEVERE,"Erreur connection DB",ex);return false;}
    return true;
     } //fin connect_db


     /*********************************
      * Execution d'une requete SELECT
      * avec retour dans un ResulSet
      * @param sql String
      * @return ResulSet
      *********************************/
     public ResultSet query(String sql){
         try {
            st = con.createStatement();
            rs=st.executeQuery(sql);
            return rs;
        } catch (SQLException ex){logger.log(Level.SEVERE,"Erreur requete DB",ex);}
     return null;
     }


/**********************************************
 * Execution d'une requete INSERT et UPDATE
 * retour du nb insert ou 0 si Update
 * @param sql String
 * @return int
 *********************************************/
     public int update(String sql){
         try {
            st = con.createStatement();
            int resultat=st.executeUpdate(sql);
            con.commit();
            return resultat;
        } catch (SQLException ex){logger.log(Level.SEVERE,"Erreur requete DB",ex);}
     return -1;
     }


     /************************************************
      * Appel d'une procédure stockée sans parametres
      * format appel fct="{call maproc()}"
      * @param fct String
      * @return ResultSet
      ************************************************/
      public ResultSet Callfunction(String fct){
         try {
             cst = con.prepareCall(fct);
             cst.registerOutParameter(1, OracleTypes.CURSOR);

           // execute getDBUSERCursor store procedure
            cst.executeUpdate();

                //récupération des ResultSet
                rs = (ResultSet) cst.getObject(1);
            return rs;

         } catch (SQLException ex){logger.log(Level.SEVERE,"Erreur requete DB",ex);}
     return null;
     }


     /************************************************
      * Appel d'une procédure stockée Avec 1 parametre chaine
      * varchar ou varchar2 compatible String(java)
      * format appel fct="{call maproc(?)}"
      * @param fct String
     * @param parametre String
      * @return ResultSet
      ************************************************/
     public ResultSet Callfunction(String fct,String parametre){
         try {
           cst = con.prepareCall(fct);
           cst.setString(1, parametre);
           cst.registerOutParameter(2, OracleTypes.CURSOR);


           // execute fct store procedure
            cst.executeUpdate();

                //récupération des ResultSet
                rs = (ResultSet) cst.getObject(2);
            return rs;

        } catch (SQLException ex){logger.log(Level.SEVERE,"Erreur requete DB",ex);}
     return null;
     }


     /************************************************
      * Appel d'une procédure stockée Avec 1 parametre entier
      * Integer ou Number(*,0) compatible integer(java)
      * format appel fct="{call maproc(?)}"
      * @param fct String
     * @param parametre int
      * @return ResultSet
      ************************************************/
public ResultSet Callfunction(String fct,int parametre){
         try {
            cst = con.prepareCall(fct);
            cst.setInt(1, parametre);
            cst.registerOutParameter(2, OracleTypes.CURSOR);


           // execute getDBUSERCursor store procedure
            cst.executeUpdate();

                //récupération des ResultSet
                rs = (ResultSet) cst.getObject(2);
            return rs;

        } catch (SQLException ex){logger.log(Level.SEVERE,"Erreur requete DB",ex);}
     return null;
     }

/*************************************************************
      * Insertion données dans la base de données 3 paramétres
      * @param procdb
      * @param p1
      * @param p2
      * @param p3
      *************************************************************/
     public void CallfunctionNR(String procdb, String p1, String p2, String p3) {
        try {
            cst = con.prepareCall(procdb);
            cst.setString(1, p1);
            cst.setString(2, p2);
            cst.setString(3, p3);

           //cst.registerOutParameter(9, OracleTypes.CURSOR);
            // execute fct store procedure
            cst.executeUpdate();

                //récupération des ResultSet
            //    result = (ResultSet) callableStat.getObject(2);
            //return result;
        } catch (SQLException ex) {
            logger.log(Level.SEVERE, "Erreur requete DB", ex);
        }
        //return null;
    }


     /************************************************
      * Appel d'une procédure stockée Avec 1 parametre date
      * date ou timeStamp compatible java.sql.Date(java)
      * format appel fct="{call maproc(?)}"
      * @param fct String
     * @param parametre java.sql.Date
      * @return ResultSet
      ************************************************/
public ResultSet Callfunction(String fct,java.sql.Date parametre){
         try {
            cst = con.prepareCall(fct);
            cst.setDate(1, parametre);
            cst.registerOutParameter(2, OracleTypes.CURSOR);


           // execute getDBUSERCursor store procedure
            cst.executeUpdate();

                //récupération des ResultSet
                rs = (ResultSet) cst.getObject(2);
            return rs;

        } catch (SQLException ex){logger.log(Level.SEVERE,"Erreur requete DB",ex);}
     return null;
     }


     /***************************************
      * retourne le ResulSet courant ou null
      * @return ResulSet
      ***************************************/
     public ResultSet getResultSet(){
         return rs;
     }


      /***************************************
      * Fermer le ResulSet
      ***************************************/
     public void closeResultSet(){
        try {
           if (rs!=null) {rs.close();}
        } catch (SQLException ex) {
            Logger.getLogger(DataBase.class.getName()).log(Level.SEVERE, null, ex);
        }
     }


     /***************************************
      * Fermer le Statement
      ***************************************/
     public void closeStatement(){
         try {
            if (st!=null) {st.close();}
        } catch (SQLException ex) {
            Logger.getLogger(DataBase.class.getName()).log(Level.SEVERE, null, ex);
        }
     }

    /***************************************
     * Fermer le CallStatement
     ***************************************/
     public void closeCallStatement(){
         try {
            if (cst!=null){cst.close();}
        } catch (SQLException ex) {
            Logger.getLogger(DataBase.class.getName()).log(Level.SEVERE, null, ex);
        }
     }

    /***************************************
    * Fermer la connexion
    ***************************************/
     public void closeConnexion(){
         try {
            if (con!=null){con.close();}
        } catch (SQLException ex) {
            Logger.getLogger(DataBase.class.getName()).log(Level.SEVERE, null, ex);
        }
     }

    /**************************************************************
     *    Fermer la connection base de données (tout)
     **************************************************************/
    public void close_db(){
        closeResultSet();
        closeStatement();
        closeCallStatement();
        closeConnexion();
    } // fin clode_db


    /******************
     * Lecture de l'URL
     * @return String
     ******************/
    public String getUrl() {
        return url;
    }


    /****************************
     * fixer l'url
     * @param url String
     ****************************/
    public void setUrl(String url) {
        this.url = url;
    }


    /*******************
     * Lecture du nom utilisateur
     * @return String
     *******************/
    public String getUser() {
        return user;
    }


    /*****************************
     * fixer le nom de l'utilisateur
     * @param user String
     *****************************/
    public void setUser(String user) {
        this.user = user;
    }


    /*************************
     * Lecture du mot de passe
     * @return String
     *************************/
    public String getPassword() {
        return password;
    }


    /**************************************
     * fixer le mot de passe
     * @param password String
     **************************************/
    public void setPassword(String password) {
        this.password = password;
    }


    /***********************
     * Lecture de la chaine du driver
     * @return String
     ***********************/
    public String getDriverDB() {
        return driverDB;
    }

} //fin classe DataBase