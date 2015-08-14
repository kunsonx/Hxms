/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package net.sf.odinms.scripting;

import net.sf.odinms.client.ConstantTable;
import net.sf.odinms.client.MapleCharacter;

/**
 *
 * @author Administrator
 */
public class ScriptManager {

    private MapleCharacter player;

    public ScriptManager(MapleCharacter player) {
        this.player = player;
    }

    public MapleCharacter getPlayer() {
        return player;
    }

    public int getOnlineTime() {
        player.reloadOnlieTime();
        return Integer.parseInt(player.getAttribute().getAttribute().get(ConstantTable.getCurrentDay() + ConstantTable._S_ONLINE_MINUTE));
    }

    public String getToDayAttribute(String key) {
        return player.getAttribute().getAttribute().get(ConstantTable.getCurrentDay() + key);
    }

    public void setToDayAttribute(String key, String v) {
        player.getAttribute().getAttribute().put(ConstantTable.getCurrentDay() + key, v);
    }

    public void clearGameProperty(String key) {
        player.getAttribute().getPlayerdata().remove(key);
    }
}
