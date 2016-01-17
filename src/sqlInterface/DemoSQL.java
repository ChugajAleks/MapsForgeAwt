/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package sqlInterface;

import java.io.FileNotFoundException;
import java.util.List;

/**
 *
 * @author aleks
 */
public class DemoSQL {
    public static void main(String[] args) throws FileNotFoundException {
        List<RelationRef> listRefs = null;
        List<RelationTag> listTags1 = null;
        DbManager manager = new DbManager();
        manager.connectToDB("Kram.sqlite");
        manager.createTablePoiRelTag();
        List<RelationTag> listTags = manager.selectRelationTagsByValue("Парковая");
        for (RelationTag listTag : listTags) {
            System.out.println(listTag);
            System.out.println("_______________");
            listRefs = manager.selectRelationRefsByRelID(listTag.getRel_id());

            System.out.println(listRefs);
        }

        manager.closeDB();
    }

}
