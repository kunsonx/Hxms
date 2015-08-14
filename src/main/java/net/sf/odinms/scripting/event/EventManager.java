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
package net.sf.odinms.scripting.event;

import java.util.*;
import java.util.concurrent.ScheduledFuture;
import javax.script.Invocable;
import javax.script.ScriptException;
import net.sf.odinms.client.MapleCharacter;
import net.sf.odinms.net.channel.ChannelServer;
import net.sf.odinms.net.world.MapleParty;
import net.sf.odinms.server.MapleSquad;
import net.sf.odinms.server.TimerManager;
import net.sf.odinms.server.maps.MapleMap;
import net.sf.odinms.tools.MaplePacketCreator;
import org.apache.log4j.Logger;

/**
 *
 * @author Matze
 */
public class EventManager {

    private Logger log = Logger.getLogger(EventManager.class);
    private Invocable iv;
    private ChannelServer cserv;
    private Map<String, EventInstanceManager> instances = new HashMap<String, EventInstanceManager>();
    private Properties props = new Properties();
    private String name;

    public EventManager(ChannelServer cserv, Invocable iv, String name) {
        this.iv = iv;
        this.cserv = cserv;
        this.name = name;
    }

    public String GetSN() {
        return cserv.getServerName();
    }

    public void handlerException(Exception exception, String methodName) {
        log.error("在事件脚本中，尝试调用函数：" + methodName, exception);
    }

    public void cancel() {
        try {
            iv.invokeFunction("cancelSchedule", (Object) null);
        } catch (ScriptException ex) {
            handlerException(ex, "cancelSchedule");
        } catch (NoSuchMethodException ex) {
            handlerException(ex, "cancelSchedule");
        }
    }

    public void schedule(final String methodName, long delay) {
        TimerManager.getInstance().schedule(new Runnable() {

            @Override
            public void run() {
                try {
                    iv.invokeFunction(methodName, (Object) null);
                } catch (ScriptException ex) {
                    handlerException(ex, methodName);
                } catch (NoSuchMethodException ex) {
                    handlerException(ex, methodName);
                }
            }
        }, delay);
    }

    public ScheduledFuture<?> scheduleAtTimestamp(final String methodName, long timestamp) {
        return TimerManager.getInstance().scheduleAtTimestamp(new Runnable() {

            public void run() {
                try {
                    iv.invokeFunction(methodName, (Object) null);
                } catch (ScriptException ex) {
                    handlerException(ex, methodName);
                } catch (NoSuchMethodException ex) {
                    handlerException(ex, methodName);
                }
            }
        }, timestamp);
    }

    public ChannelServer getChannelServer() {
        return cserv;
    }

    public EventInstanceManager getInstance(String name) {
        return instances.get(name);
    }

    public Collection<EventInstanceManager> getInstances() {
        return Collections.unmodifiableCollection(instances.values());
    }

    public EventInstanceManager newInstance(String name) {
        EventInstanceManager ret = new EventInstanceManager(this, name);
        instances.put(name, ret);
        return ret;
    }

    public void disposeInstance(String name) {
        instances.remove(name);
    }

    public Invocable getIv() {
        return iv;
    }

    public void setProperty(String key, String value) {
        props.setProperty(key, value);
    }

    public String getProperty(String key) {
        return props.getProperty(key);
    }

    public String getName() {
        return name;
    }

    //PQ method: starts a PQ
    public void startInstance(MapleParty party, MapleMap map) {
        try {
            EventInstanceManager eim = (EventInstanceManager) (iv.invokeFunction("setup", (Object) null));
            eim.registerParty(party, map);
        } catch (ScriptException ex) {
            handlerException(ex, "setup");
        } catch (NoSuchMethodException ex) {
            handlerException(ex, "setup");
        }
    }

    public void startInstance(MapleParty party, MapleMap map, boolean partyid) {
        try {
            EventInstanceManager eim;
            if (partyid) {
                eim = (EventInstanceManager) (iv.invokeFunction("setup", party.getId()));
            } else {
                eim = (EventInstanceManager) (iv.invokeFunction("setup", (Object) null));
            }
            eim.registerParty(party, map);
        } catch (ScriptException ex) {
            handlerException(ex, "setup");
        } catch (NoSuchMethodException ex) {
            handlerException(ex, "setup");
        }
    }

    public void startInstance(MapleSquad squad, MapleMap map) {
        try {
            EventInstanceManager eim = (EventInstanceManager) (iv.invokeFunction("setup", squad.getLeader().getId()));
            eim.registerSquad(squad, map);
        } catch (ScriptException ex) {
            handlerException(ex, "setup");
        } catch (NoSuchMethodException ex) {
            handlerException(ex, "setup");
        }
    }

    //non-PQ method for starting instance
    public void startInstance(EventInstanceManager eim, String leader) {
        try {
            iv.invokeFunction("setup", eim);
            eim.setProperty("leader", leader);
        } catch (ScriptException ex) {
            handlerException(ex, "setup");
        } catch (NoSuchMethodException ex) {
            handlerException(ex, "setup");
        }
    }

    //returns EventInstanceManager
    public EventInstanceManager startEventInstance(MapleParty party, MapleMap map, boolean partyid) {
        try {
            EventInstanceManager eim;
            if (partyid) {
                eim = (EventInstanceManager) (iv.invokeFunction("setup", party.getId()));
            } else {
                eim = (EventInstanceManager) (iv.invokeFunction("setup", (Object) null));
            }
            eim.registerParty(party, map);
            return eim;
        } catch (ScriptException ex) {
            handlerException(ex, "setup");
        } catch (NoSuchMethodException ex) {
            handlerException(ex, "setup");
        }
        return null;
    }

    public void autoLianjie() {
        Collection<MapleCharacter> chrs = this.cserv.getPlayerStorage().getAllCharacters();
        for (MapleCharacter chr : chrs) {
            int Lianjie = chr.Lianjie() * 2;
            chr.getClient().getSession().write(MaplePacketCreator.serverNotice(5, "[系统提示]：当前游戏在线玩家人数为：[" + Lianjie + "] 人"));
        }
    }

    public void autoNx() {
        Collection<MapleCharacter> chrs = this.cserv.getPlayerStorage().getAllCharacters();
        String xx = null;
        for (MapleCharacter chr : chrs) {
            int giveNX = 10;
            if ((chr.getMapId() == 910000000) && (chr.getClient().getChannel() == 1)) {
                chr.gainNX(giveNX);
                chr.getClient().getSession().write(MaplePacketCreator.serverNotice(5, "[泡点系统]：恭喜你获得 --泡点奖励--[" + giveNX + "]--点卷!"));
            }
        }
    }
}
