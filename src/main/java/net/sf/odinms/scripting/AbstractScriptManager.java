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
package net.sf.odinms.scripting;

import java.io.File;
import java.io.FileReader;
import java.io.Reader;
import javax.script.Invocable;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import net.sf.odinms.client.MapleClient;

/**
 *
 * @author Matze
 */
public abstract class AbstractScriptManager {

    protected ScriptEngine engine;
    private ScriptEngineManager sem = new ScriptEngineManager();
    protected static final org.apache.log4j.Logger log = org.apache.log4j.Logger.getLogger(AbstractScriptManager.class);

    protected AbstractScriptManager() {
    }

    protected Invocable getInvocable(String path, MapleClient c) {
        try {
            path = "scripts/" + path;
            engine = null;
            if (c != null) {
                engine = c.getScriptEngine(path);
            }
            if (engine == null) {
                FileScriptManager.FileData fd = FileScriptManager.getData(path);
                Reader reader = fd.getReader();
                if (reader == null) {
                    return null;
                }
                engine = sem.getEngineByName("javascript");
                engine.eval(reader);
                reader.close();

                fd = FileScriptManager.getData("scripts/global.js");
                reader = fd.getReader();
                if (reader != null) {
                    engine.eval(reader);
                    reader.close();
                }
                

                if (c != null) {
                    c.setScriptEngine(path, engine);
                }
            }
            if (c != null && engine != null && c.getPlayer() != null) {
                engine.put("player", c.getPlayer());
            }
            return (Invocable) engine;
        } catch (Exception e) {
            log.error("Error executing script. Script file: " + path + ".", e);
            return null;
        }
    }

    protected void resetContext(String path, MapleClient c) {
        path = "scripts/" + path;
        c.removeScriptEngine(path);
    }
}