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
package net.sf.odinms.scripting.portal;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import javax.script.*;
import net.sf.odinms.client.MapleClient;
import net.sf.odinms.server.MaplePortal;

public class PortalScriptManager {

    private static final org.apache.log4j.Logger log = org.apache.log4j.Logger.getLogger(PortalScriptManager.class);
    private static PortalScriptManager instance = new PortalScriptManager();
    private Map<String, PortalScript> scripts = new HashMap<String, PortalScript>();
    private ScriptEngineFactory sef;

    private PortalScriptManager() {
        ScriptEngineManager sem = new ScriptEngineManager();
        sef = sem.getEngineByName("javascript").getFactory();
    }

    public static PortalScriptManager getInstance() {
        return instance;
    }

    private PortalScript getPortalScript(MapleClient c, String scriptName) {
        if (scripts.containsKey(scriptName)) {
            return scripts.get(scriptName);
        }
        File scriptFile = new File("scripts/portal/" + scriptName + ".js");
        if (!scriptFile.exists()) {
            log.info("" + scriptName + ".js 未找到~请到服务端Portal内添加.");
            //scripts.put(scriptName, null);
            return null;
        }
        FileReader fr = null;
        ScriptEngine portal = sef.getScriptEngine();
        try {
            log.info("Portal:" + scriptName + ".js 已执行");
            fr = new FileReader(scriptFile);
            CompiledScript compiled = ((Compilable) portal).compile(fr);
            compiled.eval();
        } catch (ScriptException e) {
            log.error("THROW", e);
        } catch (IOException e) {
            log.error("THROW", e);
        } finally {
            if (fr != null) {
                try {
                    fr.close();
                } catch (IOException e) {
                    log.error("ERROR CLOSING", e);
                }
            }
        }
        PortalScript script = ((Invocable) portal).getInterface(PortalScript.class);
        scripts.put(scriptName, script);
        return script;
    }
    // rhino is thread safe so this should be fine without synchronisation

    public boolean executePortalScript(MaplePortal portal, MapleClient c) {
        PortalScript script = getPortalScript(c, portal.getScriptName());
        try {
            if (script != null && !c.getPlayer().getBlockedPortals().contains(portal.getScriptName())) {
                return script.enter(new PortalPlayerInteraction(c, portal));
            } else {
                return false;
            }
        } catch (Exception e) {
            log.error("门脚本计算错误：" + portal.getScriptName() + ".js  错误内容是：" + e.getMessage());
            return false;
        }
    }

    public void clearScripts() {
        scripts.clear();
    }
}
