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

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import javax.script.ScriptException;

import net.sf.odinms.client.MapleCharacter;
import net.sf.odinms.database.DatabaseConnection;
import net.sf.odinms.net.channel.ChannelServer;
import net.sf.odinms.net.world.MapleParty;
import net.sf.odinms.net.world.MaplePartyCharacter;
import net.sf.odinms.server.MapleSquad;
import net.sf.odinms.server.TimerManager;
import net.sf.odinms.server.life.MapleMonster;
import net.sf.odinms.server.maps.MapleMap;
import net.sf.odinms.server.maps.MapleMapFactory;
import org.apache.log4j.Logger;

/**
 *
 * @author Matze
 */
public class EventInstanceManager {

    private static Logger log = Logger.getLogger(EventInstanceManager.class);
    private List<MapleCharacter> chars = new LinkedList<MapleCharacter>();
    private List<MapleMonster> mobs = new LinkedList<MapleMonster>();
    private Map<MapleCharacter, Integer> killCount = new HashMap<MapleCharacter, Integer>();
    private Lock kclLock = new ReentrantLock(true);
    private EventManager em;
    private MapleMapFactory mapFactory;
    private String name;
    private Properties props = new Properties();
    private long timeStarted = 0;
    private long eventTime = 0;
    //  private ArrayList<ScheduledFuture> eventDSQ = new ArrayList<ScheduledFuture>();

    public EventInstanceManager(EventManager em, String name) {
        this.em = em;
        this.name = name;
        mapFactory = new MapleMapFactory();
        mapFactory.setChannel(em.getChannelServer().getDescriptor());
    }

    public void registerPlayer(MapleCharacter chr) {
        if (chr != null && chr.getEventInstance() == null) {
            try {
                chars.add(chr);
                chr.setEventInstance(this);
                em.getIv().invokeFunction("playerEntry", this, chr);
            } catch (ScriptException ex) {
                log.error("事件脚本解析错误：", ex);
            } catch (NoSuchMethodException ex) {
                log.error("事件脚本未搜索到方法：playerEntry", ex);
            }
        }
    }

    public void startEventTimer(long time) {
        timeStarted = System.currentTimeMillis();
        eventTime = time;
    }

    public boolean isTimerStarted() {
        return eventTime > 0 && timeStarted > 0;
    }

    public String getEventTimeString() {
        Calendar t = Calendar.getInstance();
        t.setTime(new Date(getTimeLeft()));
        return t.get(Calendar.HOUR) + "时" + t.get(Calendar.MINUTE) + "分" + t.get(Calendar.SECOND) + "秒";
    }

    public long getTimeLeft() {
        return eventTime - (System.currentTimeMillis() - timeStarted);
    }

    public void registerParty(MapleParty party, MapleMap map) {
        for (MaplePartyCharacter pc : party.getMembers()) {
            MapleCharacter c = map.getCharacterById(pc.getId());
            registerPlayer(c);
        }
    }

    public void registerSquad(MapleSquad squad, MapleMap map) {
        for (MapleCharacter player : squad.getMembers()) {
            if (map.getCharacterById(player.getId()) != null) {
                registerPlayer(player);
            }
        }
    }

    public void unregisterPlayer(MapleCharacter chr) {
        chars.remove(chr);
        chr.setEventInstance(null);
    }

    public int getPlayerCount() {
        return chars.size();
    }

    public List<MapleCharacter> getPlayers() {
        return new ArrayList<MapleCharacter>(chars);
    }

    public void registerMonster(MapleMonster mob) {
        mobs.add(mob);
        mob.setEventInstance(this);
    }

    public void unregisterMonster(MapleMonster mob) {
        mobs.remove(mob);
        mob.setEventInstance(null);
        if (mobs.isEmpty()) {
            try {
                em.getIv().invokeFunction("allMonstersDead", this);
            } catch (ScriptException ex) {
                log.error("事件脚本解析错误：", ex);
            } catch (NoSuchMethodException ex) {
                log.error("事件脚本未搜索到方法：allMonstersDead", ex);
            }
        }
    }

    public void playerKilled(MapleCharacter chr) {
        try {
            em.getIv().invokeFunction("playerDead", this, chr);
        } catch (ScriptException ex) {
            log.error("事件脚本解析错误：", ex);
        } catch (NoSuchMethodException ex) {
            log.error("事件脚本未搜索到方法：playerDead", ex);
        }
    }

    public boolean revivePlayer(MapleCharacter chr) {
        try {
            Object b = em.getIv().invokeFunction("playerRevive", this, chr);
            if (b instanceof Boolean) {
                return (Boolean) b;
            }
        } catch (ScriptException ex) {
            log.error("事件脚本解析错误：", ex);
        } catch (NoSuchMethodException ex) {
            log.error("事件脚本未搜索到方法：playerRevive", ex);
        }
        return true;
    }

    public void playerDisconnected(MapleCharacter chr) {
        try {
            em.getIv().invokeFunction("playerDisconnected", this, chr);
        } catch (ScriptException ex) {
            log.error("事件脚本解析错误：", ex);
        } catch (NoSuchMethodException ex) {
            log.error("事件脚本未搜索到方法：playerDisconnected", ex);
        }
    }

    /**
     *
     * @param chr
     * @param mob
     */
    public void monsterKilled(MapleCharacter chr, MapleMonster mob) {
        try {
            kclLock.lock();
            int inc = ((Double) em.getIv().invokeFunction("monsterValue", this, mob.getId())).intValue();
            killCount.put(chr, killCount.containsKey(chr) ? killCount.get(chr) + inc : inc);
        } catch (ScriptException ex) {
            log.error("事件脚本解析错误：", ex);
        } catch (NoSuchMethodException ex) {
            log.error("事件脚本未搜索到方法：monsterValue", ex);
        } finally {
            kclLock.unlock();
        }
    }

    public int getKillCount(MapleCharacter chr) {
        try {
            kclLock.lock();
            return killCount.containsKey(chr) ? killCount.get(chr) : 0;
        } finally {
            kclLock.unlock();
        }
    }

    public void dispose() {
        chars.clear();
        mobs.clear();
        try {
            kclLock.lock();
            killCount.clear();
        } finally {
            kclLock.unlock();
        }
        mapFactory = null;
        em.disposeInstance(name);
        em = null;
    }

    public MapleMapFactory getMapFactory() {
        return mapFactory;
    }

    public ScheduledFuture<?> schedule(final String methodName, long delay) {
        return TimerManager.getInstance().schedule(new Runnable() {
            @Override
            public void run() {
                try {
                    if (em == null && mapFactory == null) {
                        log.debug("[事件脚本]事件已注销.");
                    } else if (em != null) {
                        em.getIv().invokeFunction(methodName, EventInstanceManager.this);
                    } else {
                        log.warn("[事件脚本]警告：定时函数 无法执行,可能已注销事件. [原因]：EventManager 对象已为空值.");
                    }
                } catch (NullPointerException npe) {
                    log.error("事件脚本解析错误 方法名为：" + methodName, npe);
                } catch (ScriptException ex) {
                    log.error("事件脚本解析错误 方法名为：" + methodName, ex);
                } catch (NoSuchMethodException ex) {
                    log.error("事件脚本未搜索到方法：" + methodName, ex);
                }
            }
        }, delay);
    }

    public String getName() {
        return name;
    }

    public void saveWinner(MapleCharacter chr) {
        try {
            PreparedStatement ps = DatabaseConnection.getConnection().prepareStatement("INSERT INTO eventstats (event, instance, characterid, channel) VALUES (?, ?, ?, ?)");
            ps.setString(1, em.getName());
            ps.setString(2, getName());
            ps.setInt(3, chr.getId());
            ps.setInt(4, chr.getClient().getChannel());
            ps.executeUpdate();
            ps.getConnection().close();
            ps.close();
        } catch (SQLException ex) {
            log.error("SQLException:", ex);
        }
    }

    public MapleMap getMapInstance(int mapId) {
        boolean wasLoaded = mapFactory.isMapLoaded(mapId);
        MapleMap map = mapFactory.getMap(mapId);
        if (!wasLoaded) {
            if (em.getProperty("shuffleReactors") != null && em.getProperty("shuffleReactors").equals("true")) {
                map.shuffleReactors();
            }
        }
        return map;
    }

    public void setProperty(String key, String value) {
        props.setProperty(key, value);
    }

    public Object setProperty(String key, String value, boolean prev) {
        return props.setProperty(key, value);
    }

    public String getProperty(String key) {
        return props.getProperty(key);
    }

    public void leftParty(MapleCharacter chr) {
        try {
            em.getIv().invokeFunction("leftParty", this, chr);
        } catch (ScriptException ex) {
            log.error("事件脚本解析错误：", ex);
        } catch (NoSuchMethodException ex) {
            log.error("事件脚本未搜索到方法：leftParty", ex);
        }
    }

    public void disbandParty() {
        try {
            em.getIv().invokeFunction("disbandParty", this);
        } catch (ScriptException ex) {
            log.error("事件脚本解析错误：", ex);
        } catch (NoSuchMethodException ex) {
            log.error("事件脚本未搜索到方法：disbandParty", ex);
        }
    }

    //Separate function to warp players to a "finish" map, if applicable
    public void finishPQ() {
        try {
            em.getIv().invokeFunction("clearPQ", this);
        } catch (ScriptException ex) {
            log.error("事件脚本解析错误：", ex);
        } catch (NoSuchMethodException ex) {
            log.error("事件脚本未搜索到方法：clearPQ", ex);
        }
    }

    public void removePlayer(MapleCharacter chr) {
        try {
            em.getIv().invokeFunction("playerExit", this, chr);
        } catch (ScriptException ex) {
            log.error("事件脚本解析错误：", ex);
        } catch (NoSuchMethodException ex) {
            log.error("事件脚本未搜索到方法：playerExit", ex);
        }
    }

    public boolean isLeader(MapleCharacter chr) {
        return (chr.getParty().getLeader().getId() == chr.getId());
    }

    public void saveAllBossQuestPoints(int bossPoints) {
        for (MapleCharacter character : chars) {
            int points = character.getBossPoints();
            character.setBossPoints(points + bossPoints);
        }
    }

    public void saveBossQuestPoints(int bossPoints, MapleCharacter character) {
        int points = character.getBossPoints();
        character.setBossPoints(points + bossPoints);
    }
}
