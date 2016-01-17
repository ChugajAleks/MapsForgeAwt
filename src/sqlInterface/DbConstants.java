/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package sqlInterface;

/**
 *
 * @author Алекс
 */
public class DbConstants {
    public static final String SELECT_RELATION_TAGS_BY_VALUE= "SELECT  rel_id, sub, k, v  FROM osm_relation_tags  WHERE  v  LIKE  ?";
    public static final String SELECT_RELATION_TAGS_BY_REL_ID= "SELECT  rel_id, sub, k, v  FROM osm_relation_tags  WHERE  rel_id = ?";
    public static final String SELECT_RELATION_REFS_BY_REL_ID= "SELECT rel_id, type, ref, role FROM osm_relation_refs WHERE rel_id = ?";
    public static final String CREATE_POI_RELATION_TAGS = "CREATE TABLE if not exists 'poi_relation_tags' ('rel_id' INTEGER, 'k' TEXT, 'v' TEXT)";
    public static final String INSERT_TAGS_ASSOCIATED_STREET = "INSERT INTO poi_relation_tags " +
                        "SELECT osm_relation_tags.rel_id, osm_relation_tags.k, osm_relation_tags.v FROM osm_relation_tags " +
                        "WHERE (rel_id IN (SELECT rel_id FROM osm_relation_tags WHERE v LIKE '%associatedStreet%') " +
                        "AND v NOT LIKE '%associatedStreet%')";
    public static final String DELETE_TAGS_ASSOCIATED_STREET = "DELETE FROM osm_relation_tags " +
                        "WHERE rel_id IN (SELECT rel_id FROM osm_relation_tags " +
                        "WHERE v LIKE '%associatedStreet%')";
}
