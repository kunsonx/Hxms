/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package net.sf.odinms.server.maps;

import java.awt.Rectangle;
import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import net.sf.odinms.client.MapleCharacter;
import net.sf.odinms.database.DatabaseConnection;
import org.apache.log4j.Logger;

/**
 * CREATEFILE:2012年3月9日21:40:21
 *
 * @author HXMS
 */
public class MapleMapCore {

    private static final Logger log = Logger.getLogger(MapleMapCore.class);
    private static int MAX_PLAYER_ID = 0;
    /**
     * STATIC OBJECTS.
     *
     */
    private final transient ReentrantReadWriteLock Lock = new ReentrantReadWriteLock(true);
    private final List<MapleCharacter> players;
    private final Map<Integer, MapleMapObject> objects;
    private final MapleMap map;
    private final java.util.concurrent.atomic.AtomicInteger atomic = new AtomicInteger();

    static {
        try {
            java.sql.Connection con = DatabaseConnection.getConnection();
            java.sql.PreparedStatement ps = con.prepareStatement("select max(id)+10000 from characters");
            java.sql.ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                MAX_PLAYER_ID = rs.getInt(1);
                log.info("最大ID为：" + MAX_PLAYER_ID);
            } else {
                MAX_PLAYER_ID = 10000000;
            }
            rs.close();
            ps.close();
            con.close();
        } catch (SQLException ex) {
            log.error("读取错误：", ex);
        }
    }

    public MapleMapCore(MapleMap map) {
        this(32, map);
    }

    public MapleMapCore(int capacity, MapleMap map) {
        objects = new java.util.concurrent.ConcurrentHashMap<Integer, MapleMapObject>();
        players = new CopyOnWriteArrayList<MapleCharacter>();
        this.map = map;
        atomic.set(MAX_PLAYER_ID);
    }

    public synchronized void addMapObject(MapleMapObject mapobject) {
        if (mapobject == null) {
            return;
        }
        try {
            Lock.writeLock().lock();

            if (mapobject.getOwnerMap() != null) {
                mapobject.getOwnerMap().getCore().removeMapObject(mapobject);
            }

            int oid;
            if (mapobject instanceof MapleCharacter) {
                oid = ((MapleCharacter) mapobject).getId();
                players.add((MapleCharacter) mapobject);
            } else {
                oid = atomic.incrementAndGet();
                while (objects.containsKey(oid)) {
                    oid = atomic.incrementAndGet();
                }
                mapobject.setObjectId(oid);
            }

            objects.put(oid, mapobject);
            mapobject.setOwnerMap(map);

        } finally {
            Lock.writeLock().unlock();
        }
    }

    public synchronized void removeMapObject(MapleMapObject obj) {
        if (obj == null) {
            return;
        }
        try {
            Lock.writeLock().lock();
            if (objects.containsKey(obj.getObjectId())
                    || obj.getOwnerMap() == map) {
                if (objects.get(obj.getObjectId()) == obj) {//实例比对
                    objects.remove(obj.getObjectId());
                }
                if (obj instanceof MapleCharacter) {
                    players.remove((MapleCharacter) obj);
                }
                obj.setOwnerMap(null);
            } else {
                if (log.isDebugEnabled()) {
                    log.debug("尝试删除的地图对象不存在：" + obj.getObjectId());
                }
            }
        } finally {
            Lock.writeLock().unlock();
        }
    }

    /**
     * 地图对象列表。 [保留函数]
     *
     * @param type
     * @return
     */
    public List<MapleMapObject> GetMapObjects(MapleMapObjectType type) {
        try {
            Lock.readLock().lock();
            List<MapleMapObject> list = new ArrayList<MapleMapObject>();
            for (MapleMapObject mapleMapObject : objects.values()) {
                if (mapleMapObject != null && mapleMapObject.getType().equals(type)) {
                    list.add(mapleMapObject);
                }
            }
            return list;
        } finally {
            Lock.readLock().unlock();
        }
    }

    /**
     * 地图对象计数
     *
     * @param type
     * @return
     */
    public int GetMapObjectCount(MapleMapObjectType type) {
        try {
            Lock.readLock().lock();
            int count = 0;
            if (type.isPlayer()) {
                count = players.size();
            } else {
                for (MapleMapObject mapleMapObject : values()) {
                    if (mapleMapObject != null && mapleMapObject.getType().equals(type)) {
                        count++;
                    }
                }
            }
            return count;
        } finally {
            Lock.readLock().unlock();
        }
    }

    /**
     * 玩家数量。
     *
     * @return
     */
    public int GetPlayerCount() {
        return GetMapObjectCount(MapleMapObjectType.PLAYER);
    }

    /**
     * 地图玩家列表。
     *
     * @param type
     * @return
     */
    public List<MapleCharacter> GetMapPlayers() {
        try {
            Lock.readLock().lock();
            return Collections.unmodifiableList(players);
        } finally {
            Lock.readLock().unlock();
        }
    }

    /**
     * 赋值。
     *
     * @return
     */
    public Collection<MapleMapObject> values() {
        try {
            Lock.readLock().lock();
            return Collections.unmodifiableCollection(objects.values());
        } finally {
            Lock.readLock().unlock();
        }
    }

    public boolean contains(int oid) {
        return objects.containsKey(oid);
    }

    /**
     * 访问单对象.
     *
     * @param i
     * @return
     */
    public MapleMapObject get(int i) {
        try {
            Lock.readLock().lock();
            return objects.get(i);
        } finally {
            Lock.readLock().unlock();
        }
    }

    public int size() {
        try {
            Lock.readLock().lock();
            return objects.size();
        } finally {
            Lock.readLock().unlock();
        }
    }

    public List<MapleMapObject> getMapObjectsInRect(Rectangle box, List<MapleMapObjectType> types) {
        try {
            Lock.readLock().lock();
            List<MapleMapObject> ret = new ArrayList<MapleMapObject>();
            for (MapleMapObject l : objects.values()) {
                if (types.contains(l.getType())) {
                    if (box.contains(l.getPosition())) {
                        ret.add(l);
                    }
                }
            }
            return ret;
        } finally {
            Lock.readLock().unlock();
        }
    }

    public List<MapleMapObject> getMapObjectsInRect(Rectangle box, MapleMapObjectType types) {
        try {
            Lock.readLock().lock();
            List<MapleMapObject> ret = new ArrayList<MapleMapObject>();
            for (MapleMapObject l : objects.values()) {
                if (types.equals(l.getType())) {
                    if (box.contains(l.getPosition())) {
                        ret.add(l);
                    }
                }
            }
            return ret;
        } finally {
            Lock.readLock().unlock();
        }
    }
}
