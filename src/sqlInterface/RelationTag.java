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
public class RelationTag {
    private int rel_id;
    private String key;
    private String value;

    public int getRel_id() {
        return rel_id;
    }

    public void setRel_id(int rel_id) {
        this.rel_id = rel_id;
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    @Override
    public String toString() {
        return "RelationTag{" + "rel_id=" + rel_id + ", key=" + key + ", value=" + value + '}' + "\n";
    }

}
