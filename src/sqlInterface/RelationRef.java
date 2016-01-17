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
public class RelationRef {
    private int relId;
    private String type;
    private int ref;
    private String role;

    public int getRelId() {
        return relId;
    }

    public void setRelId(int relId) {
        this.relId = relId;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public int getRef() {
        return ref;
    }

    public void setRef(int ref) {
        this.ref = ref;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    @Override
    public String toString() {
        return "RelationRef{" + "relId=" + relId + ", type=" + type + ", ref=" + ref + ", role=" + role + '}' + "\n";
    }

}
