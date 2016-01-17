/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package sqlInterface;
import java.io.File;
import java.io.FileNotFoundException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
/**
 *
 * @author aleks
 */
public class DbManager {
    private int numAttemptsCon;
    private Connection con = null;
    private PreparedStatement relationTagByValue;
    private PreparedStatement relationTagByRelId;
    private PreparedStatement relationRefsByRelID;
    private ResultSet resultQueries = null;
    //TODO надо применить какойто паттерн. (сейчас для добавления запроса надо вставлять много кода в разных местах)
    public DbManager(){
        
        try {
            Class.forName("org.sqlite.JDBC");
        } catch (ClassNotFoundException ex) {
            Logger.getLogger(DbManager.class.getName()).log(Level.SEVERE, null, ex);
        }
            System.out.println("library is load");

    }

    public void connectToDB(String dbFilePath) throws FileNotFoundException{
        
        File file = new File(dbFilePath);
        if(!(file.exists()) || !(file.isFile())){
            throw new FileNotFoundException(dbFilePath);
        }   
        try {
            con = DriverManager.getConnection("jdbc:sqlite:" + dbFilePath);
            System.out.println("connection is created");
        }catch (UnsatisfiedLinkError ex){
            numAttemptsCon++;
            if(numAttemptsCon < 5){  
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException ex1) {
                    Logger.getLogger(DbManager.class.getName()).log(Level.SEVERE, null, ex1);
                }
                connectToDB(dbFilePath);
            }
            else{
                System.out.println("nuber of attempts connections = " + numAttemptsCon);
                throw ex;
            }
        }catch (SQLException ex) {
            Logger.getLogger(DbManager.class.getName()).log(Level.SEVERE, null, ex);
        }
        //TODO проверить таблицу на валидность
        try {
            relationTagByValue = con.prepareStatement(DbConstants.SELECT_RELATION_TAGS_BY_VALUE);
            relationTagByRelId = con.prepareStatement(DbConstants.SELECT_RELATION_TAGS_BY_REL_ID);
            relationRefsByRelID = con.prepareStatement(DbConstants.SELECT_RELATION_REFS_BY_REL_ID);
        } catch (SQLException ex) {
            Logger.getLogger(DbManager.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public void createTablePoiRelTag(){
        try {
            Statement statement = this.con.createStatement();
            statement.executeUpdate(DbConstants.CREATE_POI_RELATION_TAGS);
            //statement.executeQuery(DbConstants.CREATE_POI_RELATION_TAGS);//если использовать это то выскакивает сообще что не возвращается ResultSet
            statement.executeUpdate(DbConstants.INSERT_TAGS_ASSOCIATED_STREET);
            statement.close();
        } catch (SQLException ex) {
            Logger.getLogger(DbManager.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public List<RelationTag> selectRelationTagsByValue(String findValue){
        List<RelationTag> listTags = new ArrayList<RelationTag>();
        try {
            this.relationTagByValue.clearParameters();
            this.relationTagByValue.setString(1, "%" + findValue + "%");
            this.resultQueries = relationTagByValue.executeQuery();
        while(resultQueries.next()){
            RelationTag tag = new RelationTag();
            tag.setRel_id(resultQueries.getInt("rel_id"));
            tag.setKey(resultQueries.getString("k"));
            tag.setValue(resultQueries.getString("v"));
            listTags.add(tag);
        }

        } catch (SQLException ex) {
            Logger.getLogger(DbManager.class.getName()).log(Level.SEVERE, null, ex);
        }
        return listTags;
    }

    public List<RelationTag> selectRelationTagsByRelId(int relId){
        List<RelationTag> listTags = new ArrayList<RelationTag>();
        try {
            this.relationTagByRelId.clearParameters();
            this.relationTagByRelId.setInt(1, relId);
            this.resultQueries = relationTagByRelId.executeQuery();
        while(resultQueries.next()){
            RelationTag tag = new RelationTag();
            tag.setRel_id(resultQueries.getInt("rel_id"));
            tag.setKey(resultQueries.getString("k"));
            tag.setValue(resultQueries.getString("v"));
            listTags.add(tag);
        }

        } catch (SQLException ex) {
            Logger.getLogger(DbManager.class.getName()).log(Level.SEVERE, null, ex);
        }
        return listTags;
    }

    public List<RelationRef> selectRelationRefsByRelID(int relID){
        List<RelationRef> listRefs = new ArrayList<RelationRef>();
        try {
            relationRefsByRelID.clearParameters();
            relationRefsByRelID.setInt(1, relID);
            resultQueries = relationRefsByRelID.executeQuery();
            while(resultQueries.next()){
                RelationRef ref = new RelationRef();
                ref.setRelId(resultQueries.getInt("rel_id"));
                ref.setType(resultQueries.getString("type"));
                ref.setRef(resultQueries.getInt("ref"));
                ref.setRole(resultQueries.getString("role"));
                listRefs.add(ref);
            }
        } catch (SQLException ex) {
            Logger.getLogger(DbManager.class.getName()).log(Level.SEVERE, null, ex);
        }

        return listRefs;
    }

    public void closeDB() {
        try {
            relationTagByValue.close();
            relationRefsByRelID.close();
            relationTagByRelId.close();
        } catch (SQLException ex) {
            Logger.getLogger(DbManager.class.getName()).log(Level.SEVERE, null, ex);
        }
        if (resultQueries != null) {
            try {
                resultQueries.close();
            } catch (SQLException ex) {
                Logger.getLogger(DbManager.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        if (con != null) {
            try {
                con.close();
            } catch (SQLException ex) {
                Logger.getLogger(DbManager.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        
    }

}
