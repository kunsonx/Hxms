/*
 This file is part of the OdinMS Maple Story Server
 Copyright (C) 2008 Patrick Huy <patrick.huy@frz.cc> 
 Matthias Butz <matze@odinms.de>
 Jan Christian Meyer <vimes@odinms.de>

 This program is free software: you can redistribute it and/or modify
 it under the terms of the GNU Affero General Public License version 3
 as published by the Free Software Foundation. You may not use, modify
 or distribute this program under any other version of the
 GNU Affero General Public License.

 This program is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU Affero General Public License for more details.

 You should have received a copy of the GNU Affero General Public License
 along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package net.sf.odinms.scripting.npc;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.script.Invocable;
import javax.script.ScriptEngine;
import net.sf.odinms.client.MapleClient;
import net.sf.odinms.net.world.MaplePartyCharacter;
import net.sf.odinms.scripting.AbstractScriptManager;

/**
 *
 * @author Matze
 */
public class NPCScriptManager extends AbstractScriptManager {

    private Map<MapleClient, NPCConversationManager> cms = new HashMap<MapleClient, NPCConversationManager>();
    private Map<MapleClient, NPCScript> scripts = new HashMap<MapleClient, NPCScript>();
    private static NPCScriptManager instance = new NPCScriptManager();

    public synchronized static NPCScriptManager getInstance() {
        return instance;
    }

    public ScriptEngine start(MapleClient c, int npc, int x) {
        NPCConversationManager cm = new NPCConversationManager(c, npc);
        try {
            if (c.getPlayer().isGM()) {
                c.getPlayer().dropMessage("[系统提示]您已经建立与NPC:" + npc + "的对话。"); //增加个流水号，方便管理员调试。
            }
            if (cms.containsKey(c)) {
                return null;
            }
            cms.put(c, cm);
            Invocable iv;
            if (x == -1) {
                iv = getInvocable("npc/" + npc + ".js", c);
            } else {
                iv = getInvocable("npc/" + npc + "_" + x + ".js", c);
            }
            if (iv == null || NPCScriptManager.getInstance() == null) {
                cm.sendOk("我是个空置的Npc，如果你有好的建议请联系我们的管理员.\r\n我的ID编号: #r" + npc + "#k.");
                cm.dispose();
                return null;
            }
            engine.put("cm", cm);
            NPCScript ns = iv.getInterface(NPCScript.class);
            scripts.put(c, ns);
            ns.start();
        } catch (Exception e) {
            cm.sendOk("#e#r脚本出现错误，请联系GM进行解决（也许是GM在调试哟，嘿嘿）");
            log.error("错误的NPC：" + npc, e);
            /*
             * WriteToFile re = new
             * WriteToFile("D:\\ErrorScript\\NpcErrorScript.txt");
             * re.WriteFile("NPCid: " + npc + "\r\n错误原因: " + e + "\r\n原因位置:" +
             * e.getCause());
             */
            dispose(c);
            cms.remove(c);
        }
        return engine;
    }

    public void start(String filename, MapleClient c, int npc, List<MaplePartyCharacter> chars) { // CPQ start
        try {
            NPCConversationManager cm = new NPCConversationManager(c, npc, chars, 0);
            cm.dispose();
            if (cms.containsKey(c)) {
                return;
            }
            cms.put(c, cm);
            Invocable iv = getInvocable("npc/" + filename + ".js", c);
            NPCScriptManager npcsm = NPCScriptManager.getInstance();
            if (iv == null || NPCScriptManager.getInstance() == null || npcsm == null) {
                cm.dispose();
                return;
            }
            engine.put("cm", cm);
            NPCScript ns = iv.getInterface(NPCScript.class);
            scripts.put(c, ns);
            ns.start(chars);
        } catch (Exception e) {
            log.error("Error executing NPC script " + filename, e);
            /*
             * WriteToFile re = new
             * WriteToFile("D:\\ErrorScript\\NpcErrorScript.txt");
             * re.WriteFile("NPCid:"+filename + "\r\n错误原因:"+e + "\r\n原因位置:" +
             * e.getCause());
             */
            dispose(c);
            cms.remove(c);

        }
    }

    public void action(MapleClient c, byte mode, byte type, int selection) {
        NPCScript ns = scripts.get(c);
        if (ns != null) {
            try {
                ns.action(mode, type, selection);
            } catch (Exception e) {
                if (c != null && e != null) {
                    log.error("Error executing NPC script : " + (c.getCM() == null ? "Unknown" : c.getCM().getNpc()), e);
                    dispose(c);
                }
                //WriteToFile re = new WriteToFile("D:\\ErrorScript\\NpcErrorScript.txt");
                //re.WriteFile("NPCid:"+c.getCM().getNpc()
                // + "\r\n错误原因:"+e
                //+ "\r\n原因位置:" + e.getCause());
            }
        }
    }

    public void dispose(NPCConversationManager cm) {
        MapleClient c = cm.getC();
        cms.remove(c);
        scripts.remove(c);
        resetContext("npc/" + cm.getNpc() + ".js", c);
    }

    public void dispose(MapleClient c) {
        NPCConversationManager npccm = cms.get(c);
        if (npccm != null) {
            dispose(npccm);
        }
    }

    public NPCConversationManager getCM(MapleClient c) {
        return cms.get(c);
    }
}