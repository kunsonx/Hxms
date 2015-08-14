/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package net.sf.odinms.client;

/**
 *
 * @author 岚殇
 */

public class MapleUseMount {
    private int usingSkillId, usingItemId;
    private MapleCharacter chr;
         
    MapleUseMount(MapleCharacter chr, int usingItemId, int usingSkillId){
        this.chr = chr;
        this.usingItemId = usingItemId;
        this.usingSkillId = usingSkillId;
    }
    
    public int getUsingSkillId() {
        return usingSkillId;
    }
    
    public void setUsingSkillId(int i) {
        this.usingSkillId = i;
    }
    
    public int getUsingItemId() {
        return usingItemId;
    }
    
    public void setUsingItemId(int i) {
        this.usingItemId = i;
    }
}

