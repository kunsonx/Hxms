/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package net.sf.odinms.server.maps;

import java.awt.Point;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import net.sf.odinms.database.DatabaseConnection;
import net.sf.odinms.net.channel.ChannelDescriptor;
import net.sf.odinms.provider.MapleDataTool;
import net.sf.odinms.server.MaplePortal;
import net.sf.odinms.server.life.AbstractLoadedMapleLife;
import net.sf.odinms.server.life.MapleLifeFactory;
import net.sf.odinms.server.life.MapleMonster;
import net.sf.odinms.server.life.MapleNPC;
import net.sf.odinms.tools.Randomizer;
import org.apache.log4j.Logger;

/**
 *
 * @author Admin
 */
public class MapleMapInfo {

    private static Logger log = Logger.getLogger(MapleMapInfo.class);
    private int mapid,
            returnMapId,
            timeMobId,
            bossid,
            partyBonusRate,
            decHP,
            HPDecProtect,
            forcedReturnMap,
            fl,
            timeLimit,
            fieldType;
    private float monsterRate;
    private String FirstUserEnter,
            UserEnter,
            timeMobMessage,
            msg;
    private List<MaplePortal> portals = new ArrayList<MaplePortal>();
    private List<MapleFoothold> allFootholds = new ArrayList<MapleFoothold>();
    private Point lBound, uBound;
    private List<MapleMapCreateLifeInfo> lifeInfos = new ArrayList<MapleMapCreateLifeInfo>();
    private List<MapleMapCreateReactorInfo> reactorInfos = new ArrayList<MapleMapCreateReactorInfo>();
    private MapleNodes nodes;
    private String mapName, streetName;
    private boolean hasClock, everlast, isTown, allowShops, hasBoat;

    private MapleMapInfo() {
    }

    public MapleMapInfo(int mapid, int returnMapId, float monsterRate) {
        this.mapid = mapid;
        this.returnMapId = returnMapId;
        this.monsterRate = monsterRate;
    }

    void addPortal(MaplePortal myPortal) {
        portals.add(myPortal);
    }

    public int getMapid() {
        return mapid;
    }

    public void setMapid(int mapid) {
        this.mapid = mapid;
    }

    public int getReturnMapId() {
        return returnMapId;
    }

    public void setReturnMapId(int returnMapId) {
        this.returnMapId = returnMapId;
    }

    public int getTimeMobId() {
        return timeMobId;
    }

    public void setTimeMobId(int timeMobId) {
        this.timeMobId = timeMobId;
    }

    public float getMonsterRate() {
        return monsterRate;
    }

    public void setMonsterRate(float monsterRate) {
        this.monsterRate = monsterRate;
    }

    public String getFirstUserEnter() {
        return FirstUserEnter;
    }

    public void setFirstUserEnter(String FirstUserEnter) {
        this.FirstUserEnter = FirstUserEnter;
    }

    public String getUserEnter() {
        return UserEnter;
    }

    public void setUserEnter(String UserEnter) {
        this.UserEnter = UserEnter;
    }

    public String getTimeMobMessage() {
        return timeMobMessage;
    }

    public void setTimeMobMessage(String timeMobMessage) {
        this.timeMobMessage = timeMobMessage;
    }

    public List<MaplePortal> getPortals() {
        return portals;
    }

    public void setPortals(List<MaplePortal> portals) {
        this.portals = portals;
    }

    public int getBossid() {
        return bossid;
    }

    public void setBossid(int bossid) {
        this.bossid = bossid;
    }

    public int getPartyBonusRate() {
        return partyBonusRate;
    }

    public void setPartyBonusRate(int partyBonusRate) {
        this.partyBonusRate = partyBonusRate;
    }

    public int getDecHP() {
        return decHP;
    }

    public void setDecHP(int decHP) {
        this.decHP = decHP;
    }

    public int getHPDecProtect() {
        return HPDecProtect;
    }

    public void setHPDecProtect(int HPDecProtect) {
        this.HPDecProtect = HPDecProtect;
    }

    public int getForcedReturnMap() {
        return forcedReturnMap;
    }

    public void setForcedReturnMap(int forcedReturnMap) {
        this.forcedReturnMap = forcedReturnMap;
    }

    public int getFl() {
        return fl;
    }

    public void setFl(int fl) {
        this.fl = fl;
    }

    public int getTimeLimit() {
        return timeLimit;
    }

    public void setTimeLimit(int timeLimit) {
        this.timeLimit = timeLimit;
    }

    public int getFieldType() {
        return fieldType;
    }

    public void setFieldType(int fieldType) {
        this.fieldType = fieldType;
    }

    public String getMsg() {
        return msg;
    }

    public void setMsg(String msg) {
        this.msg = msg;
    }

    public List<MapleFoothold> getAllFootholds() {
        return allFootholds;
    }

    public void setAllFootholds(List<MapleFoothold> allFootholds) {
        this.allFootholds = allFootholds;
    }

    public Point getlBound() {
        return lBound;
    }

    public void setlBound(Point lBound) {
        this.lBound = lBound;
    }

    public Point getuBound() {
        return uBound;
    }

    public void setuBound(Point uBound) {
        this.uBound = uBound;
    }

    public List<MapleMapCreateLifeInfo> getLifeInfos() {
        return lifeInfos;
    }

    public void setLifeInfos(List<MapleMapCreateLifeInfo> lifeInfos) {
        this.lifeInfos = lifeInfos;
    }

    public MapleNodes getNodes() {
        return nodes;
    }

    public void setNodes(MapleNodes nodes) {
        this.nodes = nodes;
    }

    public String getMapName() {
        return mapName;
    }

    public void setMapName(String mapName) {
        this.mapName = mapName;
    }

    public String getStreetName() {
        return streetName;
    }

    public void setStreetName(String streetName) {
        this.streetName = streetName;
    }

    public boolean isHasClock() {
        return hasClock;
    }

    public void setHasClock(boolean hasClock) {
        this.hasClock = hasClock;
    }

    public boolean isEverlast() {
        return everlast;
    }

    public void setEverlast(boolean everlast) {
        this.everlast = everlast;
    }

    public boolean isIsTown() {
        return isTown;
    }

    public void setIsTown(boolean isTown) {
        this.isTown = isTown;
    }

    public boolean isAllowShops() {
        return allowShops;
    }

    public void setAllowShops(boolean allowShops) {
        this.allowShops = allowShops;
    }

    public boolean isHasBoat() {
        return hasBoat;
    }

    public void setHasBoat(boolean hasBoat) {
        this.hasBoat = hasBoat;
    }

    public List<MapleMapCreateReactorInfo> getReactorInfos() {
        return reactorInfos;
    }

    public void setReactorInfos(List<MapleMapCreateReactorInfo> reactorInfos) {
        this.reactorInfos = reactorInfos;
    }

    public MapleMap getMap(ChannelDescriptor channel) {
        MapleMap map = new MapleMap(mapid, channel, returnMapId, monsterRate);
        map.setFirstUserEnter(FirstUserEnter);
        map.setUserEnter(UserEnter);
        map.setTimeMobId(timeMobId);
        map.setTimeMobMessage(timeMobMessage);
        for (MaplePortal portal : portals) {
            map.addPortal(portal);
        }
        MapleFootholdTree fTree = new MapleFootholdTree(lBound, uBound);
        for (MapleFoothold foothold : getAllFootholds()) {
            fTree.insert(foothold);
        }
        map.setFootholds(fTree);
        if (map.getTop() == 0) {
            map.setTop((short) lBound.y);
        }
        if (map.getBottom() == 0) {
            map.setBottom((short) uBound.y);
        }
        if (map.getLeft() == 0) {
            map.setLeft((short) lBound.x);
        }
        if (map.getRight() == 0) {
            map.setRight((short) uBound.x);
        }
        List<Point> herbRocks = new ArrayList<Point>();
        int lowestLevel = 200, highestLevel = 0;
        AbstractLoadedMapleLife myLife;
        for (MapleMapCreateLifeInfo lifeInfo : getLifeInfos()) {
            myLife = lifeInfo.getLife();
            if (myLife instanceof MapleMonster) {
                final MapleMonster mob = (MapleMonster) myLife;
                int mobTime = lifeInfo.getMobtime();
                if (mobTime == -1) { //不是不召唤 而是-1的时候只召唤一次
                    map.spawnMonster(mob);
                } else {
                    herbRocks.add(map.addMonsterSpawn(mob,
                            mobTime).getPosition());
                }
                if (mob.getLevel() > highestLevel && !mob.isBoss()) {
                    highestLevel = mob.getLevel();
                }
                if (mob.getLevel() < lowestLevel && !mob.isBoss()) {
                    lowestLevel = mob.getLevel();
                }
            } else if (myLife instanceof MapleNPC) {
                map.addMapObject(myLife);
            }
        }
        map.setPartyBonusRate(partyBonusRate);
        map.setNodes(nodes);
        for (MapleMapCreateReactorInfo reactorInfo : getReactorInfos()) {
            MapleReactor reactor = reactorInfo.getReactor();
            map.spawnReactor(reactor);
        }
        map.setMapName(mapName);
        map.setStreetName(streetName);
        map.setClock(hasClock);
        map.setEverlast(everlast);
        map.setTown(isTown);
        map.setAllowShops(allowShops);
        map.setHPDec(decHP);
        map.setHPDecProtect(HPDecProtect);
        map.setForcedReturnMap(forcedReturnMap);
        map.setBoat(hasBoat);
        map.setFieldLimit(fl);
        map.setTimeLimit(timeLimit);
        map.setFieldType(fieldType);

        try {
            Connection con = DatabaseConnection.getConnection();
            PreparedStatement ps = con.prepareStatement("SELECT * FROM spawns WHERE mid = ?");
            ps.setInt(1, mapid);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                int id = rs.getInt("idd");
                int f = rs.getInt("f");
                boolean hide = false;
                String type = rs.getString("type");
                int fh_ = rs.getInt("fh");
                int cy = rs.getInt("cy");
                int rx0 = rs.getInt("rx0");
                int rx1 = rs.getInt("rx1");
                int x = rs.getInt("x");
                int y = rs.getInt("y");
                int mobTime = rs.getInt("mobtime");
                myLife = loadLife(id, f, hide, fh_, cy, rx0, rx1, x, y, type);
                if (type.equals("n")) {
                    map.addMapObject(myLife);
                } else if (type.equals("m")) {
                    MapleMonster monster = (MapleMonster) myLife;
                    map.addMonsterSpawn(monster, mobTime);
                }
            }
            rs.close();
            ps.close();
            con.close();
        } catch (SQLException e) {
            log.info(e.toString());
        }


        if (herbRocks.size() > 0 && highestLevel >= 30 && map.getFirstUserEnter().equals("") && map.getUserEnter().equals("")) {
            final List<Integer> allowedSpawn = new ArrayList<Integer>(24);
            allowedSpawn.add(100011);
            allowedSpawn.add(200011);
            if (highestLevel >= 100) {
                for (int i = 0; i < 10; i++) {
                    for (int x = 0; x < 4; x++) { //to make heartstones rare
                        allowedSpawn.add(100000 + i);
                        allowedSpawn.add(200000 + i);
                    }
                }
            } else {
                for (int i = (lowestLevel % 10 > highestLevel % 10 ? 0 : (lowestLevel % 10)); i < (highestLevel % 10); i++) {
                    for (int x = 0; x < 4; x++) { //to make heartstones rare
                        allowedSpawn.add(100000 + i);
                        allowedSpawn.add(200000 + i);
                    }
                }
            }
            final int numSpawn = Randomizer.getInstance().nextInt(allowedSpawn.size()) / 6; //0-7
            for (int i = 0; i < numSpawn && !herbRocks.isEmpty(); i++) {
                final int idd = allowedSpawn.get(Randomizer.getInstance().nextInt(allowedSpawn.size()));
                final int theSpawn = Randomizer.getInstance().nextInt(herbRocks.size());
                final MapleReactor myReactor = new MapleReactor(MapleReactorFactory.getReactor(idd), idd);
                myReactor.setPosition(herbRocks.get(theSpawn));
                myReactor.setDelay(idd % 100 == 11 ? 60000 : 5000); //in the reactor's wz
                map.spawnReactor(myReactor);
                herbRocks.remove(theSpawn);
            }
        }

        return map;
    }

    private AbstractLoadedMapleLife loadLife(int id, int f, boolean hide, int fh, int cy, int rx0, int rx1, int x, int y, String type) {
        AbstractLoadedMapleLife myLife = MapleLifeFactory.getLife(id, type);
        myLife.setCy(cy);
        myLife.setF(f);
        myLife.setFh(fh);
        myLife.setRx0(rx0);
        myLife.setRx1(rx1);
        myLife.setPosition(new Point(x, y));
        myLife.setHide(hide);
        return myLife;
    }
}
