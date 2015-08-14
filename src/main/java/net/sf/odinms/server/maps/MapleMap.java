package net.sf.odinms.server.maps;

import java.awt.Point;
import java.awt.Rectangle;
import java.util.*;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.atomic.AtomicInteger;
import net.sf.odinms.client.*;
import net.sf.odinms.client.anticheat.CheatingOffense;
import net.sf.odinms.client.messages.MessageCallback;
import net.sf.odinms.client.status.MapleMonsterStat;
import net.sf.odinms.client.status.MonsterStatusEffect;
import net.sf.odinms.net.MaplePacket;
import net.sf.odinms.net.channel.ChannelDescriptor;
import net.sf.odinms.net.channel.ChannelServer;
import net.sf.odinms.net.channel.MaplePvp;
import net.sf.odinms.net.world.MapleParty;
import net.sf.odinms.net.world.MaplePartyCharacter;
import net.sf.odinms.scripting.map.MapScriptManager;
import net.sf.odinms.server.*;
import net.sf.odinms.server.life.MapleLifeFactory;
import net.sf.odinms.server.life.MapleMonster;
import net.sf.odinms.server.life.MapleNPC;
import net.sf.odinms.server.life.SpawnPoint;
import net.sf.odinms.tools.MaplePacketCreator;
import net.sf.odinms.tools.Randomizer;
import org.apache.log4j.Logger;

public class MapleMap {

    private static final List<MapleMapObjectType> rangedMapobjectTypes = Arrays.asList(
            MapleMapObjectType.ITEM,
            MapleMapObjectType.MONSTER,
            MapleMapObjectType.DOOR,
            MapleMapObjectType.SUMMON,
            MapleMapObjectType.REACTOR,
            MapleMapObjectType.机械传送门);
    private static Logger log = Logger.getLogger(MapleMap.class);
    /**
     * Holds a mapping of all oid -> MapleMapObject on this map. mapobjects is
     * NOT a synchronized collection since it has to be synchronized together
     * with runningOid that's why all access to mapobjects have to be done
     * trough an explicit synchronized block
     */
    private LinkedList<SpawnPoint> monsterSpawn = new LinkedList<SpawnPoint>();
    private MapleMapCore core = new MapleMapCore(this);
    private HashMap<Integer, MaplePortal> portals = new HashMap<Integer, MaplePortal>();
    public AtomicInteger spawnedMonstersOnMap = new AtomicInteger(0);
    private MapleFootholdTree footholds = null;
    private ChannelDescriptor channel;
    private int mapid, partyBonusRate = 0, returnMapId, forcedReturnMap = 999999999, timeLimit, dropLife = 30000 // 以毫秒为单位掉落后消失的时间
            , decHP = 0, protectItem = 0,
            fieldType, timeMobId;
    private float monsterRate, origMobRate;
    private boolean dropsDisabled = false, clock, boat, docked, everlast = false, town, allowShops, hasEvent, lootable = true, canEnter = true, canExit = true, cannotInvincible = false, canVipRock = true, muted;
    private String mapName, streetName, UserEnter, FirstUserEnter, timeMobMessage = "";
    private MapleMapEffect mapEffect = null;
    private MapleMapTimer mapTimer = null;
    private ScheduledFuture<?> spawnWorker = null;
    private ScheduledFuture<?> poisonSchedule;
    private MapleMapTask task;
    private short top = 0, bottom = 0, left = 0, right = 0;
    private MapleNodes nodes;
    private MapleOfflinePlayer offlinePlayer = new MapleOfflinePlayer(this);

    public MapleMap() {
    }

    public MapleMap(int mapid, ChannelDescriptor channel, int returnMapId, float monsterRate) {
        //  this.mapid = mapid;
        // this.channel = channel;
        // this.returnMapId = returnMapId;
        // this.monsterRate = monsterRate;
        // this.origMobRate = monsterRate;

        // if (monsterRate > 0) {
        //     spawnWorker = TimerManager.getInstance().register(new RespawnWorker(), 7000);//刷怪时间 以毫秒
        // }

        this.mapid = mapid;
        this.channel = channel;
        this.returnMapId = returnMapId;
        if (monsterRate > 0) {
            this.monsterRate = monsterRate;
            boolean greater1 = monsterRate > 1.0;
            this.monsterRate = (float) Math.abs(1.0 - this.monsterRate);
            this.monsterRate = this.monsterRate / 2.0f;
            if (greater1) {
                this.monsterRate = 1.0f + this.monsterRate;
            } else {
                this.monsterRate = 1.0f - this.monsterRate;
            }
            TimerManager.getInstance().register(new RespawnWorker(), 5000);
        }
    }

    public LinkedList<SpawnPoint> getMonsterSpawn() {
        return monsterSpawn;
    }

    public void setMonsterSpawn(LinkedList<SpawnPoint> monsterSpawn) {
        this.monsterSpawn = monsterSpawn;
    }

    public HashMap<Integer, MaplePortal> getPortals_() {
        return portals;
    }

    public void setPortals(HashMap<Integer, MaplePortal> portals) {
        this.portals = portals;
    }

    public void setSpawnedMonstersOnMap(AtomicInteger spawnedMonstersOnMap) {
        this.spawnedMonstersOnMap = spawnedMonstersOnMap;
    }

    public int getMapid() {
        return mapid;
    }

    public void setMapid(int mapid) {
        this.mapid = mapid;
    }

    public int getDropLife() {
        return dropLife;
    }

    public void setDropLife(int dropLife) {
        this.dropLife = dropLife;
    }

    public int getDecHP() {
        return decHP;
    }

    public void setDecHP(int decHP) {
        this.decHP = decHP;
    }

    public int getProtectItem() {
        return protectItem;
    }

    public void setProtectItem(int protectItem) {
        this.protectItem = protectItem;
    }

    public float getOrigMobRate() {
        return origMobRate;
    }

    public void setOrigMobRate(float origMobRate) {
        this.origMobRate = origMobRate;
    }

    public boolean isDropsDisabled() {
        return dropsDisabled;
    }

    public void setDropsDisabled(boolean dropsDisabled) {
        this.dropsDisabled = dropsDisabled;
    }

    public boolean isHasEvent() {
        return hasEvent;
    }

    public void setHasEvent(boolean hasEvent) {
        this.hasEvent = hasEvent;
    }

    public boolean isCanVipRock() {
        return canVipRock;
    }

    public void setCanVipRock(boolean canVipRock) {
        this.canVipRock = canVipRock;
    }

    public MapleMapEffect getMapEffect() {
        return mapEffect;
    }

    public void setMapEffect(MapleMapEffect mapEffect) {
        this.mapEffect = mapEffect;
    }

    public MapleMapTimer getMapTimer() {
        return mapTimer;
    }

    public void setMapTimer(MapleMapTimer mapTimer) {
        this.mapTimer = mapTimer;
    }

    public ScheduledFuture<?> getSpawnWorker() {
        return spawnWorker;
    }

    public void setSpawnWorker(ScheduledFuture<?> spawnWorker) {
        this.spawnWorker = spawnWorker;
    }

    public ScheduledFuture<?> getPoisonSchedule() {
        return poisonSchedule;
    }

    public void setPoisonSchedule(ScheduledFuture<?> poisonSchedule) {
        this.poisonSchedule = poisonSchedule;
    }

    public MapleNodes getNodes() {
        return nodes;
    }

    public void setNodes(MapleNodes nodes) {
        this.nodes = nodes;
    }

    public MapleMap(int mapid) {
        this.mapid = mapid;
    }

    public MapleMapTask getTask() {
        return task;
    }

    public void setTask(MapleMapTask task) {
        this.task = task;
    }

    public void respawn() {
        if (core.GetMapObjectCount(MapleMapObjectType.PLAYER) == 0) {
            return;
        }
        int numShouldSpawn = (this.monsterSpawn.size() - this.spawnedMonstersOnMap.get()) * Math.round(this.monsterRate);
        if (numShouldSpawn > 0) {
            List<SpawnPoint> randomSpawn = new ArrayList<SpawnPoint>(monsterSpawn);
            Collections.shuffle(randomSpawn);
            int spawned = 0;
            for (SpawnPoint spawnPoint : randomSpawn) {
                if (spawnPoint.shouldSpawn()) {
                    spawnPoint.spawnMonster(MapleMap.this);
                    spawned++;
                }
                if (spawned >= numShouldSpawn) {
                    break;
                }
            }
        }
    }

    /**
     * 计算方法暂时如下.
     *
     * @试运行: 一天24小时 1小时：41点 1小时等于60分钟 60分钟划分为6份 每份10分钟 每10分钟1次 每次7点
     * @按等级计算公式： int givNx = ((jsNx * chr.getLevel()/2) + Nx);
     * @备注信息： 以上计算方法结果如下： 100级玩家/每天最多获得1000点券。
     */
    public void AutoNx(final int jsNx) {
        for (MapleCharacter chr : core.GetMapPlayers()) {
            int Nx;
            Nx = 1;
            int givNx = ((jsNx) + Nx);
            chr.modifyCSPoints(1, givNx);
            chr.UpdateCash();
            chr.getClient().getSession().write(MaplePacketCreator.serverNotice(5, "[系统奖励] 在线时间奖励获得 [" + givNx + "] 点券."));
        }
    }

    /**
     * 市场泡点
     *
     * @param jsexp 经验倍率
     * @param isnx 是否是点卷
     * @param cspointrate 点卷倍率
     */
    public void AutoGain(final int jsexp, final boolean isnx, final int cspointrate) {
        for (MapleCharacter chr : core.GetMapPlayers()) {
            int givexp = 0;
            givexp = ((jsexp * chr.getLevel()) + chr.getClient().getChannelServer().getExpRate()) * (chr.getVip() + 2);
            givexp *= 2;
            chr.modifyCSPoints(isnx ? 0 : 1, (chr.getVip() + 2) * cspointrate);
            chr.gainExp(givexp, false, true);
            chr.UpdateCash();
            chr.setDojoPoints(chr.getDojoPoints() + (chr.getVip() + 2) * cspointrate);
            chr.getClient().getSession().write(MaplePacketCreator.serverNotice(5, "[系统奖励] 获得 [" + givexp + "] 点经验.[" + (chr.getVip() + 2) * cspointrate + "点]修为点[会员将加成]"));
            /*   if (GameConstants.秋秋冒险岛 && chr.getVip() > 2) {
             chr.GainMoney((chr.getVip() - 2));
             chr.setDojoPoints(chr.getDojoPoints() + (chr.getVip() - 2));
             chr.getClient().getSession().write(MaplePacketCreator.serverNotice(5, "[系统奖励] 获得 [" + (chr.getVip() - 2) + "] 点元宝," + "会员额外修为获得[" + (chr.getVip() - 2) + "]点修为."));
             }*/
        }
    }

    public boolean canEnter() {
        return canEnter;
    }

    public boolean canExit() {
        return canExit;
    }

    public void setCanEnter(boolean b) {
        canEnter = b;
    }

    public void setCanExit(boolean b) {
        canExit = b;
    }

    public void toggleDrops() {
        dropsDisabled = !dropsDisabled;
    }

    public int getId() {
        return mapid;
    }

    public MapleMap getReturnMap() {
        return ChannelServer.getInstance(channel).getMapFactory().getMap(returnMapId);
    }

    public int getReturnMapId() {
        return returnMapId;
    }

    public int getForcedReturnId() {
        return forcedReturnMap;
    }

    public MapleMap getForcedReturnMap() {
        return ChannelServer.getInstance(channel).getMapFactory().getMap(forcedReturnMap);
    }

    public void setForcedReturnMap(int map) {
        this.forcedReturnMap = map;
    }

    public int getTimeLimit() {
        return timeLimit;
    }

    public void setTimeLimit(int timeLimit) {
        this.timeLimit = timeLimit;
    }

    public boolean getMuted() {
        return muted;
    }

    public void setMuted(boolean isMuted) {
        this.muted = isMuted;
    }

    public boolean isLootable() {
        return lootable;
    }

    public void setLootable(boolean loot) {
        this.lootable = loot;
    }

    public int getCurrentPartyId() {
        for (MapleCharacter chr : this.getCharacters()) {
            if (chr.getPartyId() != -1) {
                return chr.getPartyId();
            }
        }
        return -1;
    }

    public void addMapObject(MapleMapObject mapobject) {
        core.addMapObject(mapobject);
    }

    public void broadcastGMMessage(MapleCharacter source, MaplePacket packet, boolean repeatToSource) {
        broadcastGMMessage(repeatToSource ? null : source, packet, Double.POSITIVE_INFINITY, source.getPosition());
    }

    private void broadcastGMMessage(final MapleCharacter source, final MaplePacket packet, final double rangeSq, final Point rangedFrom) {
        for (MapleCharacter chr : core.GetMapPlayers()) {
            if (chr != source && chr.getGm() > 0) {
                if (rangeSq < Double.POSITIVE_INFINITY) {
                    if (rangedFrom.distanceSq(chr.getPosition()) <= rangeSq) {
                        chr.getClient().getSession().write(packet);
                    }
                } else {
                    chr.getClient().getSession().write(packet);
                }
            }
        }
    }

    private void spawnAndAddRangedMapObject(final MapleMapObject mapobject, final DelayedPacketCreation packetbakery, final SpawnCondition condition) {
        //log.debug("设置的objectid"+runningOid);
            /*
         * if(mapobject.getType() == MapleMapObjectType.MONSTER) {
         * List<MapleMapObject> summons =
         * getMapObjectsInRange(mapobject.getPosition(),
         * Double.POSITIVE_INFINITY, Arrays.asList(MapleMapObjectType.SUMMON));
         * for (MapleMapObject summon : summons) { MapleSummon summonOne =
         * (MapleSummon) summon; if(summonOne.getSkill() == 机械师.加速器) {
         * MapleCharacter player = summonOne.getOwner(); MapleMonster monster =
         * (MapleMonster) mapobject; ISkill skill =
         * SkillFactory.getSkill(summonOne.getSkill()); int skillLevel =
         * player.getSkillLevel(skill); MapleStatEffect effect =
         * skill.getEffect(skillLevel); MonsterStatusEffect monsterStatusEffect
         * = new MonsterStatusEffect(effect.getMonsterStati(), skill, false);
         * monster.applyStatus(player, monsterStatusEffect, false,
         * effect.getDuration()); log.debug("有加速器");
         * log.debug("mapobject的id"+monster.getId());
         * log.debug("mapobject的oid"+monster.getObjectId()); } } }
         */
        core.addMapObject(mapobject);
        for (MapleCharacter chr : core.GetMapPlayers()) {
            if (condition == null || condition.canSpawn(chr)) {
                if (chr.getPosition().distanceSq(mapobject.getPosition()) <= MapleCharacter.MAX_VIEW_RANGE_SQ) {
                    packetbakery.sendPackets(chr.getClient());
                    chr.addVisibleMapObject(mapobject);
                }
            }
        }
    }

    public void spawnMesoDrop(final int meso, Point position, final MapleMapObject dropper, final MapleCharacter owner, final boolean ffaLoot) {
        TimerManager tMan = TimerManager.getInstance();
        final Point droppos = calcDropPos(position, position);
        final MapleMapItem mdrop = new MapleMapItem(meso, droppos, dropper, owner);
        spawnAndAddRangedMapObject(mdrop, new DelayedPacketCreation() {
            @Override
            public void sendPackets(MapleClient c) {
                c.getSession().write(MaplePacketCreator.dropMesoFromMapObject(meso, mdrop.getObjectId(), dropper.getObjectId(),
                        ffaLoot ? 0 : owner.getId(), dropper.getPosition(), droppos, (byte) 1));
            }
        }, null);
        tMan.schedule(new ExpireMapItemJob(mdrop), dropLife);
    }

    public void mapMessage(int type, String message) {
        broadcastMessage(MaplePacketCreator.serverNotice(type, message));
    }

    public void removeMapObject(MapleMapObject obj) {
        core.removeMapObject(obj);
    }

    private Point calcPointBelow(Point initial) {
        MapleFoothold fh = footholds.findBelow(initial);
        if (fh == null) {
            return null;
        }
        int dropY = fh.getY1();
        if (!fh.isWall() && fh.getY1() != fh.getY2()) {
            double s1 = Math.abs(fh.getY2() - fh.getY1());
            double s2 = Math.abs(fh.getX2() - fh.getX1());
            double s4 = Math.abs(initial.x - fh.getX1());
            double alpha = Math.atan(s2 / s1);
            double beta = Math.atan(s1 / s2);
            double s5 = Math.cos(alpha) * (s4 / Math.cos(beta));
            if (fh.getY2() < fh.getY1()) {
                dropY = fh.getY1() - (int) s5;
            } else {
                dropY = fh.getY1() + (int) s5;
            }
        }
        return new Point(initial.x, dropY);
    }

    private Point calcDropPos(Point initial, Point fallback) {
        Point ret = calcPointBelow(new Point(initial.x, initial.y - 99));
        if (ret == null) {
            return fallback;
        }
        return ret;
    }

    private void dropFromMonster(MapleCharacter dropOwner, MapleMonster monster) {
        if (dropsDisabled || monster.dropsDisabled()) {
            return;
        }

        MapleItemInformationProvider ii = MapleItemInformationProvider.getInstance();

        final int maxDrops = monster.getMaxDrops(dropOwner);
        final boolean explosive = monster.isExplosive();

        List<Integer> toDrop = new ArrayList<Integer>();

        for (int i = 0; i < maxDrops; i++) {
            toDrop.add(monster.getDrop(dropOwner));
        }

        //加入


        if (dropOwner.getEventInstance() == null) {
            int chance = (int) (Math.random() * 100);
            if (chance < 5) { //10%的几率爆枫叶
                toDrop.add(4001126);
            }
            chance = (int) (Math.random() * 100);
            if (chance < 1) { //1%的几率爆永恒的谜之蛋
                toDrop.add(4280000);
            }
            chance = (int) (Math.random() * 100);
            if (chance < 2) { //2%的几率爆重生的谜之蛋
                toDrop.add(4280001);
            }
            chance = (int) (Math.random() * 100);
            if (chance < 1) { //1%的几率爆彩虹枫叶
                toDrop.add(4032733);
            }

            chance = (int) (Math.random() * 1000);
            if (chance < 5) {//5%的机率爆高级潜能附加卷轴
                toDrop.add(2049400);
            }

            chance = (int) (Math.random() * 1000);
            if (chance < 3) {//3%的机率爆高级装备强化卷轴
                toDrop.add(2049300);
            }

            chance = (int) (Math.random() * 1000);
            if (chance < 10) {//5%的机率爆潜能附加卷轴
                toDrop.add(2049401);
            }

            chance = (int) (Math.random() * 1000);
            if (chance < 10) {//5%的机率爆装备强化卷轴
                toDrop.add(2049301);
            }

            chance = (int) (Math.random() * 1000);
            if (chance < 10) {//5%的机率爆枫叶水晶球
                toDrop.add(4032056);
            }
        }

        /*
         * if (monster.getId() == 8810018) { toDrop.add(2290096); //force add
         * one MW per HT }
         */
        Set<Integer> alreadyDropped = new HashSet<Integer>();
        int htpendants = 0;
        int htstones = 0;
        for (int i = 0; i < toDrop.size(); i++) {
            if (toDrop.get(i) == 1122000) {
                if (htpendants > 3) {
                    toDrop.set(i, -1);
                } else {
                    htpendants++;
                }
            } else if (toDrop.get(i) == 4001094) {
                if (htstones > 2) {
                    toDrop.set(i, -1);
                } else {
                    htstones++;
                }
            } else if (alreadyDropped.contains(toDrop.get(i)) && !explosive) {
                toDrop.remove(i);
                i--;
            } else {
                alreadyDropped.add(toDrop.get(i));
            }
        }

        if (toDrop.size() > maxDrops) {
            toDrop = toDrop.subList(0, maxDrops);
        }

        Point[] toPoint = new Point[toDrop.size()];
        int shiftDirection = 0;
        int shiftCount = 0;

        int curX = Math.min(Math.max(monster.getPosition().x - 25 * (toDrop.size() / 2), footholds.getMinDropX() + 25),
                footholds.getMaxDropX() - toDrop.size() * 25);
        int curY = Math.max(monster.getPosition().y, footholds.getY1());
        while (shiftDirection < 3 && shiftCount < 1000) {
            if (shiftDirection == 1) {
                curX += 25;
            } else if (shiftDirection == 2) {
                curX -= 25;
            }
            for (int i = 0; i < toDrop.size(); i++) {
                MapleFoothold wall = footholds.findWall(new Point(curX, curY), new Point(curX + toDrop.size() * 25, curY));
                if (wall != null) {
                    if (wall.getX1() < curX) {
                        shiftDirection = 1;
                        shiftCount++;
                        break;
                    } else if (wall.getX1() == curX) {
                        if (shiftDirection == 0) {
                            shiftDirection = 1;
                        }
                        shiftCount++;
                        break;
                    } else {
                        shiftDirection = 2;
                        shiftCount++;
                        break;
                    }
                } else if (i == toDrop.size() - 1) {
                    shiftDirection = 3;
                }
                final Point dropPos = calcDropPos(new Point(curX + i * 25, curY), new Point(monster.getPosition()));
                toPoint[i] = new Point(curX + i * 25, curY);
                final int drop = toDrop.get(i);

                if (drop == -1) {
                    final int mesoRate = dropOwner.getClient().getChannelServer().getMesoRate();
                    double mesoDecrease = Math.pow(0.93, monster.getExp() / 300.0);
                    if (mesoDecrease > 1.0) {
                        mesoDecrease = 1.0;
                    }
                    int tempmeso = Math.min(30000, (int) (mesoDecrease * (monster.getExp()) * (1.0 + Math.random() * 20) / 10.0));
                    if (dropOwner.getBuffedValue(MapleBuffStat.MESOUP) != null) {
                        tempmeso = (int) (tempmeso * dropOwner.getBuffedValue(MapleBuffStat.MESOUP).doubleValue() / 100.0);
                    }

                    final int meso = tempmeso;

                    if (meso > 0) {
                        final MapleMonster dropMonster = monster;
                        final MapleCharacter dropChar = dropOwner;
                        TimerManager.getInstance().schedule(new Runnable() {
                            public void run() {
                                spawnMesoDrop(meso * mesoRate, dropPos, dropMonster, dropChar, explosive);
                            }
                        }, monster.getAnimationTime("die1"));
                    }
                } else {
                    IItem idrop;
                    MapleInventoryType type = ii.getInventoryType(drop);
                    if (type.equals(MapleInventoryType.EQUIP)) {
                        Equip nEquip = ii.randomizeStats((Equip) ii.getEquipById(drop));
                        idrop = nEquip;
                    } else {
                        idrop = new Item(drop, (byte) 0, (short) 1);
                        if (ii.isArrowForBow(drop) || ii.isArrowForCrossBow(drop)) { // Randomize quantity for certain items
                            idrop.setQuantity((short) (1 + 100 * Math.random()));
                        } else if (ii.isThrowingStar(drop) || ii.isBullet(drop)) {
                            idrop.setQuantity((short) (1));
                        }
                    }

                    idrop.log("Created as a drop from monster " + monster.getObjectId() + " (" + monster.getId() + ") at " + dropPos.toString() + " on map " + mapid, false);

                    final MapleMapItem mdrop = new MapleMapItem(idrop, dropPos, monster, dropOwner);
                    final MapleMapObject dropMonster = monster;
                    final MapleCharacter dropChar = dropOwner;
                    final TimerManager tMan = TimerManager.getInstance();

                    tMan.schedule(new Runnable() {
                        public void run() {
                            spawnAndAddRangedMapObject(mdrop, new DelayedPacketCreation() {
                                public void sendPackets(MapleClient c) {
                                    c.getSession().write(MaplePacketCreator.dropItemFromMapObject(drop, mdrop.getObjectId(), dropMonster.getObjectId(), explosive ? 0 : dropChar.getId(), dropMonster.getPosition(), dropPos, (byte) 1));
                                    activateItemReactors(mdrop);
                                }
                            }, null);

                            tMan.schedule(new ExpireMapItemJob(mdrop), dropLife);
                        }
                    }, monster.getAnimationTime("die1"));

                }
            }
        }
    }

    public boolean damageMonster(MapleCharacter chr, MapleMonster monster, int damage) {
        if (monster.getId() == 8500000 || monster.getId() == 8800000) {
            SpeedRankings.setStartTime(monster.getId() == 8500000 ? 1 : 0, monster.getId(), System.currentTimeMillis());
        }
        if (monster.getId() == 8800000) {
            Collection<MapleMapObject> objects = chr.getMap().getMapObjects();
            for (MapleMapObject object : objects) {
                MapleMonster mons = chr.getMap().getMonsterByOid(object.getObjectId());
                if (mons != null) {
                    if (mons.getId() >= 8800003 && mons.getId() <= 8800010) {
                        return true;
                    }
                }
            }
        }
        if (monster.isAlive()) {
            boolean killMonster = false;
            synchronized (monster) {
                if (!monster.isAlive()) {
                    return false;
                }
                if (damage > 0) {
                    int monsterhp = monster.getHp();
                    monster.damage(chr, damage, true);
                    if (!monster.isAlive()) { // 怪物已经死亡
                        killMonster(monster, chr, true);

                        if (monster.getId() >= 8810002 && monster.getId() <= 8810009) {
                            for (MapleMapObject object : chr.getMap().getMapObjects()) {
                                MapleMonster mons = chr.getMap().getMonsterByOid(object.getObjectId());
                                if (mons != null) {
                                    if (mons.getId() == 8810018 || mons.getId() == 8810026) {
                                        damageMonster(chr, mons, monsterhp);
                                    }
                                }
                            }
                        } else if ((monster.getId() >= 8820002 && monster.getId() <= 8820006) || (monster.getId() >= 8820015 && monster.getId() <= 8820018)) {
                            for (MapleMapObject object : chr.getMap().getMapObjects()) {
                                MapleMonster mons = chr.getMap().getMonsterByOid(object.getObjectId());
                                if (mons != null) {
                                    if (mons.getId() >= 8820010 && mons.getId() <= 8820014) {
                                        damageMonster(chr, mons, monsterhp);
                                    }
                                }
                            }
                        }

                    } else if (monster.getId() >= 8810002 && monster.getId() <= 8810009) {
                        for (MapleMapObject object : chr.getMap().getMapObjects()) {
                            MapleMonster mons = chr.getMap().getMonsterByOid(object.getObjectId());
                            if (mons != null) {
                                if (mons.getId() == 8810018 || mons.getId() == 8810026) {
                                    damageMonster(chr, mons, damage);
                                }
                            }
                        }
                    } else if ((monster.getId() >= 8820002 && monster.getId() <= 8820006) || (monster.getId() >= 8820015 && monster.getId() <= 8820018)) {
                        for (MapleMapObject object : chr.getMap().getMapObjects()) {
                            MapleMonster mons = chr.getMap().getMonsterByOid(object.getObjectId());
                            if (mons != null) {
                                if (mons.getId() >= 8820010 && mons.getId() <= 8820014) {
                                    damageMonster(chr, mons, damage);
                                }
                            }
                        }
                    }
                }
            }
            if (killMonster) {
                killMonster(monster, chr, true);
            }
            return true;
        }
        return false;
    }

    public void killMonster(final MapleMonster monster, final MapleCharacter chr, final boolean withDrops) {
        killMonster(monster, chr, withDrops, false, 1);
    }

    public void killMonster(final MapleMonster monster, final MapleCharacter chr, final boolean withDrops, final boolean secondTime) {
        killMonster(monster, chr, withDrops, secondTime, 1);
    }

    public void killMonster(int monsId) {
        List<MapleMapObject> lmmo = new ArrayList<MapleMapObject>(getMapObjects());
        for (MapleMapObject mmo : lmmo) {
            if (mmo instanceof MapleMonster) {
                if (((MapleMonster) mmo).getId() == monsId) {
                    this.killMonster((MapleMonster) mmo, (MapleCharacter) getAllPlayers().get(0), false);
                }
            }
        }
    }

    public void killMonster(int monsId, MapleCharacter trigger) {
        List<MapleMapObject> lmmo = new ArrayList<MapleMapObject>(getMapObjects());
        for (MapleMapObject mmo : lmmo) {
            if (mmo instanceof MapleMonster) {
                if (((MapleMonster) mmo).getId() == monsId) {
                    this.killMonster((MapleMonster) mmo, trigger, false);
                }
            }
        }
    }

    @SuppressWarnings("static-access")
    public void killMonster(final MapleMonster monster, final MapleCharacter chr, final boolean withDrops, final boolean secondTime, int animation) {
        if (chr.getCheatTracker().checkHPLoss()) {
            chr.getCheatTracker().registerOffense(CheatingOffense.ATTACK_WITHOUT_GETTING_HIT);
        }
        StringBuilder names = new StringBuilder();
        if (monster.getId() == 8500002 || monster.getId() == 8800002) {
            if (chr.getParty() != null) {
                MapleParty party = chr.getParty();
                List<MapleCharacter> partymems = party.getPartyMembers();
                for (int i = 0; i < partymems.size(); i++) {
                    names.append(partymems.get(i).getName());
                    names.append(", ");
                }
            } else {
                names.append(chr.getName());
            }
        }
        if (monster.getId() == 8810018 && !secondTime) {
            TimerManager.getInstance().schedule(new Runnable() {
                @Override
                public void run() {
                    killMonster(monster, chr, withDrops, true, 1);
                    killAllMonsters();
                }
            }, 3000);
            return;
        }
        //chr.increaseEquipExp(monster.getExp()); //给予道具经验
        if (monster.getBuffToGive() > -1) {
            broadcastMessage(MaplePacketCreator.showOwnBuffEffect(monster.getBuffToGive(), 11));
            MapleItemInformationProvider mii = MapleItemInformationProvider.getInstance();
            final MapleStatEffect statEffect = mii.getItemEffect(monster.getBuffToGive());

            for (MapleCharacter character : core.GetMapPlayers()) {
                if (character.isAlive()) {
                    statEffect.applyTo(character);
                    broadcastMessage(MaplePacketCreator.showBuffeffect(character.getId(), monster.getBuffToGive(), 11, (byte) 3, 1));
                }
            }


        }
        if (monster.getId() == 8810018) {
            for (MapleCharacter c : this.getCharacters()) {
                c.finishAchievement(26);
            }
        }
        if (chr.getMapId() >= 925020010 && chr.getMapId() <= 925033804) {
            for (MapleCharacter c : this.getCharacters()) {
                c.DoJoKill();
            }
        }
        spawnedMonstersOnMap.decrementAndGet();
        monster.setHp(0);
        broadcastMessage(MaplePacketCreator.killMonster(monster.getObjectId(), true), monster.getPosition());
        removeMapObject(monster);
        if (monster.getId() >= 8800003 && monster.getId() <= 8800010) {
            boolean makeZakReal = true;
            Collection<MapleMapObject> objects = getMapObjects();
            for (MapleMapObject object : objects) {
                MapleMonster mons = getMonsterByOid(object.getObjectId());
                if (mons != null) {
                    if (mons.getId() >= 8800003 && mons.getId() <= 8800010) {
                        makeZakReal = false;
                    }
                }
            }
            if (makeZakReal) {
                for (MapleMapObject object : objects) {
                    MapleMonster mons = chr.getMap().getMonsterByOid(object.getObjectId());
                    if (mons != null) {
                        if (mons.getId() == 8800000 || mons.getId() == 8800100) {
                            makeMonsterReal(mons);
                            updateMonsterController(mons);
                            break;
                        }
                    }
                }
            }
        }
        MapleCharacter dropOwner = monster.killBy(chr);
        if (withDrops && !monster.dropsDisabled()) {
            if (dropOwner == null) {
                dropOwner = chr;
            }
            dropFromMonster(dropOwner, monster);
        }
    }

    //吞噬用
    public void killMonster2(final MapleMonster monster, final MapleCharacter chr) {
        //chr.increaseEquipExp(monster.getExp()); //给予道具经验
        spawnedMonstersOnMap.decrementAndGet();
        monster.setHp(0);
        broadcastMessage(MaplePacketCreator.SpecialKillMonster(monster.getObjectId(), chr.getId()), monster.getPosition());
        removeMapObject(monster);
    }

    public void killAllMonsters() {
        for (MapleMapObject monstermo : getMapObjectsInRange(new Point(0, 0), Double.POSITIVE_INFINITY, Arrays.asList(MapleMapObjectType.MONSTER))) {
            MapleMonster monster = (MapleMonster) monstermo;
            spawnedMonstersOnMap.decrementAndGet();
            monster.setHp(0);
            broadcastMessage(MaplePacketCreator.killMonster(monster.getObjectId(), true), monster.getPosition());
            removeMapObject(monster);
        }
    }

    public List<MapleMapObject> getAllPlayers() {
        return getMapObjectsInRange(new Point(0, 0), Double.POSITIVE_INFINITY, Arrays.asList(MapleMapObjectType.PLAYER));
    }

    public void destroyReactor(int oid) {
        final MapleReactor reactor = getReactorByOid(oid);
        TimerManager tMan = TimerManager.getInstance();
        broadcastMessage(MaplePacketCreator.destroyReactor(reactor));
        reactor.setAlive(false);
        removeMapObject(reactor);
        reactor.setTimerActive(false);
        if (reactor.getDelay() > 0) {
            tMan.schedule(new Runnable() {
                @Override
                public void run() {
                    respawnReactor(reactor);
                }
            }, reactor.getDelay());
        }
    }

    public void resetReactors() {
        for (MapleMapObject object : core.values()) {
            if (object.getType().equals(MapleMapObjectType.REACTOR)) {
                ((MapleReactor) object).setState((byte) 0);
                ((MapleReactor) object).setTimerActive(false);
                broadcastMessage(MaplePacketCreator.triggerReactor(((MapleReactor) object), 0));
            }
        }
    }

    /*
     * command to reset all item-reactors in a map to state 0 for GM/NPC use -
     * not tested (broken reactors get removed from mapobjects when destroyed)
     * Should create instances for multiple copies of non-respawning reactors...
     */
    public void setReactorState() {
        for (MapleMapObject object : core.values()) {
            if (object.getType().equals(MapleMapObjectType.REACTOR)) {
                ((MapleReactor) object).setState((byte) 1);
                broadcastMessage(MaplePacketCreator.triggerReactor(((MapleReactor) object), 1));
            }
        }
    }

    /*
     * command to shuffle the positions of all reactors in a map for PQ purposes
     * (such as ZPQ/LMPQ)
     */
    public void shuffleReactors() {
        final List<Point> points = new ArrayList<Point>();
        for (MapleMapObject object : core.values()) {
            if (object.getType().equals(MapleMapObjectType.REACTOR)) {
                points.add(object.getPosition());
            }
        }
        Collections.shuffle(points);
        for (MapleMapObject object : core.values()) {
            if (object.getType().equals(MapleMapObjectType.REACTOR)) {
                object.setPosition(points.remove(points.size() - 1));
            }
        }
    }

    /**
     * Automagically finds a new controller for the given monster from the chars
     * on the map...
     *
     * @param monster
     */
    public void updateMonsterController(MapleMonster monster) {
        if (!monster.isAlive()) {
            return;
        }
        synchronized (monster) {
            if (!monster.isAlive()) {
                return;
            }
            if (monster.getController() != null) {
                // monster has a controller already, check if he's still on this map
                if (monster.getController().getMap() != this) {
                    log.warn("Monstercontroller wasn't on same map");
                    monster.getController().stopControllingMonster(monster);
                } else {
                    // controller is on the map, monster has an controller, everything is fine
                    return;
                }
            }
            int mincontrolled = -1;
            MapleCharacter newController = null;
            for (MapleCharacter chr : core.GetMapPlayers()) {
                if (!chr.isHidden() && (chr.getControlledMonsters().size() < mincontrolled || mincontrolled == -1)) {
                    if (!chr.getName().equals("FaekChar")) { 
                        mincontrolled = chr.getControlledMonsters().size();
                        newController = chr;
                    }
                }
            }

            if (newController != null) { // was a new controller found? (if not no one is on the map)
                if (monster.isFirstAttack()) {
                    newController.controlMonster(monster, true);
                    monster.setControllerHasAggro(true);
                    monster.setControllerKnowsAboutAggro(true);
                } else {
                    newController.controlMonster(monster, false);
                }
            }
        }
    }

    public Collection<MapleMapObject> getMapObjects() {
        return Collections.unmodifiableCollection(core.values());
    }

    public boolean containsNPC(int npcid) {
        for (MapleMapObject obj : core.GetMapObjects(MapleMapObjectType.NPC)) {
            if (obj.getType() == MapleMapObjectType.NPC) {
                if (((MapleNPC) obj).getId() == npcid) {
                    return true;
                }
            }
        }
        return false;
    }

    public MapleMapObject getMapObject(int oid) {
        return core.get(oid);
    }

    /**
     * returns a monster with the given oid, if no such monster exists returns
     * null
     *
     * @param oid
     * @return
     */
    public MapleMonster getMonsterByOid(int oid) {
        MapleMapObject mmo = getMapObject(oid);
        if (mmo == null) {
            return null;
        }
        if (mmo.getType() == MapleMapObjectType.MONSTER) {
            return (MapleMonster) mmo;
        }
        return null;
    }

    public void spawnMonsterOnGroudBelow(MapleMonster mob, java.awt.Point pos) {
        spawnMonsterOnGroundBelow(mob, pos);
    }

    public MapleMonster getMonsterById(int id) {
        for (MapleMapObject obj : core.GetMapObjects(MapleMapObjectType.MONSTER)) {
            if (obj.getType() == MapleMapObjectType.MONSTER) {
                if (((MapleMonster) obj).getId() == id) {
                    return (MapleMonster) obj;
                }
            }
        }
        return null;
    }

    public MapleReactor getReactorByOid(int oid) {
        MapleMapObject mmo = getMapObject(oid);
        if (mmo == null) {
            return null;
        }
        if (mmo.getType() == MapleMapObjectType.REACTOR) {
            return (MapleReactor) mmo;
        }
        return null;
    }

    public MapleReactor getReactorByName(String name) {
        for (MapleMapObject obj : core.GetMapObjects(MapleMapObjectType.REACTOR)) {
            if (obj.getType() == MapleMapObjectType.REACTOR) {
                if (((MapleReactor) obj).getName().equals(name)) {
                    return (MapleReactor) obj;
                }
            }
        }
        return null;
    }

    public void spawnMonsterOnGroundBelow(MapleMonster mob, Point pos) {
        Point spos = new Point(pos.x, pos.y - 1);
        spos = calcPointBelow(spos);
        spos.y -= 1;
        mob.setPosition(spos);
        spawnMonster(mob);
    }

    public void spawnFakeMonsterOnGroundBelow(MapleMonster mob, Point pos) {
        Point spos = new Point(pos.x, pos.y - 1);
        spos = calcPointBelow(spos);
        spos.y -= 1;
        mob.setPosition(spos);
        spawnFakeMonster(mob);
    }

    public void spawnRevives(final MapleMonster monster) {
        //log.debug("spawnAndAddRangedMapObject调用1");
        monster.setMap(this);
        spawnAndAddRangedMapObject(monster, new DelayedPacketCreation() {
            @Override
            public void sendPackets(MapleClient c) {
                c.getSession().write(MaplePacketCreator.spawnMonster(monster, false));
            }
        }, null);
        updateMonsterController(monster);
        spawnedMonstersOnMap.incrementAndGet();
    }

    public synchronized void spawnMonster(final MapleMonster monster) {
        //命令召唤怪和服务端刷新怪物都是这个函数
        if (core.GetPlayerCount() == 0 && (!isPQMap())) {
            return;
        }
        monster.setMap(this);
        int removeAfter = monster.getRemoveAfter();
        if (removeAfter > 0) {
            TimerManager.getInstance().schedule(new Runnable() {
                @Override
                public void run() {
                    if (getAllPlayers().size() > 0) {
                        killMonster(monster, (MapleCharacter) getAllPlayers().get(0), false, false, 3);
                    }
                }
            }, removeAfter);
        }
        /*
         * List<MapleMapObject> summons =
         * getMapObjectsInRange(monster.getPosition(), Double.POSITIVE_INFINITY,
         * Arrays.asList(MapleMapObjectType.SUMMON)); for (MapleMapObject summon
         * : summons) { MapleSummon summonOne = (MapleSummon) summon;
         * if(summonOne.getSkill() == 机械师.加速器) { MapleCharacter player =
         * summonOne.getOwner(); ISkill skill =
         * SkillFactory.getSkill(summonOne.getSkill()); int skillLevel =
         * player.getSkillLevel(skill); MapleStatEffect effect =
         * skill.getEffect(skillLevel); MonsterStatusEffect monsterStatusEffect
         * = new MonsterStatusEffect(effect.getMonsterStati(), skill, false);
         * monster.applyStatus(player, monsterStatusEffect, false,
         * effect.getDuration()); log.debug("有加速器");
         * log.debug("mapobject的id"+monster.getId());
         * log.debug("mapobject的oid"+monster.getObjectId()); break; } }
         */
        //log.debug("spawnAndAddRangedMapObject调用2");
        spawnAndAddRangedMapObject(monster, new DelayedPacketCreation() {
            public void sendPackets(MapleClient c) {
                c.getSession().write(MaplePacketCreator.spawnMonster(monster, true));
            }
        }, null);
        updateMonsterController(monster);
        spawnedMonstersOnMap.incrementAndGet();
    }

    public void spawnDojoMonster(final MapleMonster monster) {
        Point[] pts = {new Point(140, 0), new Point(190, 7), new Point(187, 7)};
        spawnMonsterWithEffect(monster, 15, pts[Randomizer.getInstance().nextInt(3)]);
    }

    public void spawnMonsterWithEffect(final MapleMonster monster, final int effect, Point pos) {
        try {
            monster.setMap(this);
            Point spos = new Point(pos.x, pos.y - 1);
            spos = calcPointBelow(spos);
            spos.y--;
            monster.setPosition(spos);
            if (mapid < 925020000 || mapid > 925030000) {
                monster.disableDrops();
            }
            //log.debug("spawnAndAddRangedMapObject调用3");
            spawnAndAddRangedMapObject(monster, new DelayedPacketCreation() {
                public void sendPackets(MapleClient c) {
                    c.getSession().write(MaplePacketCreator.spawnMonster(monster, true, effect));
                }
            }, null);
            if (monster.hasBossHPBar()) {
                broadcastMessage(monster.makeBossHPBarPacket(), monster.getPosition());
            }
            updateMonsterController(monster);
            spawnedMonstersOnMap.incrementAndGet();
        } catch (Exception e) {
        }
    }

    public synchronized void spawnFakeMonster(final MapleMonster monster) {
        monster.setMap(this);
        monster.setFake(true);
        //log.debug("spawnAndAddRangedMapObject调用4");
        spawnAndAddRangedMapObject(monster, new DelayedPacketCreation() {
            @Override
            public void sendPackets(MapleClient c) {
                c.getSession().write(MaplePacketCreator.spawnFakeMonster(monster, 0));
            }
        }, null);
        spawnedMonstersOnMap.incrementAndGet();
    }

    public synchronized void makeMonsterReal(final MapleMonster monster) {
        monster.setFake(false);
        broadcastMessage(MaplePacketCreator.makeMonsterReal(monster));
        //updateMonsterController(monster);
    }

    public void spawnReactor(final MapleReactor reactor) {
        //常用这个
        reactor.setMap(this);
        spawnAndAddRangedMapObject(reactor, new DelayedPacketCreation() {
            public void sendPackets(MapleClient c) {
                c.getSession().write(reactor.makeSpawnData());
            }
        }, null);
    }

    private void respawnReactor(final MapleReactor reactor) {
        reactor.setState((byte) 0);
        reactor.setAlive(true);
        spawnReactor(reactor);
    }

    public void spawnDoor(final MapleDoor door) {
        spawnAndAddRangedMapObject(door, new DelayedPacketCreation() {
            public void sendPackets(MapleClient c) {
                c.getSession().write(MaplePacketCreator.spawnDoor(door.getOwner().getId(), door.getTargetPosition(), false));
                if (door.getOwner().getParty() != null && (door.getOwner() == c.getPlayer() || door.getOwner().getParty().containsMember(new MaplePartyCharacter(c.getPlayer())))) {
                    c.getSession().write(MaplePacketCreator.partyPortal(door.getTown().getId(), door.getTarget().getId(), door.getTargetPosition()));
                }
                c.getSession().write(MaplePacketCreator.spawnPortal(door.getTown().getId(), door.getTarget().getId(), door.getTargetPosition()));
                c.getSession().write(MaplePacketCreator.enableActions());
            }
        }, new SpawnCondition() {
            public boolean canSpawn(MapleCharacter chr) {
                return chr.getMapId() == door.getTarget().getId() || chr == door.getOwner() && chr.getParty() == null;
            }
        });
    }

    public void spawnDoor2(final MapleDoor2 door) {
        spawnAndAddRangedMapObject(door, new DelayedPacketCreation() {
            public void sendPackets(final MapleClient c) {
                if (door.getTarget().getId() == c.getPlayer().getMapId() || door.getOwner() == c.getPlayer()) {
                    broadcastMessage(MaplePacketCreator.传送门的效果(door.getOwner().getId(), door.getTargetPosition(), door.isFirst()));
                    broadcastMessage(MaplePacketCreator.传送门的传送点(door.getTargetPosition()));
                    /*
                     * if(door.isFirst()) log.info("召唤机械师的门A"); else
                     * log.info("召唤机械师的门B");
                     */
                }
            }
        }, null);
    }

    public void spawnSummon(final MapleSummon summon) {
        spawnAndAddRangedMapObject(summon, new DelayedPacketCreation() {
            @Override
            public void sendPackets(MapleClient c) {
                //log.debug("召唤Summon");
                int skillLevel = summon.getOwner().getSkillLevel(SkillFactory.getSkill(summon.getSkill()));
                c.getSession().write(MaplePacketCreator.spawnSpecialMapObject(summon, skillLevel));
            }
        }, null);
    }

    public void spawnLove(final MapleLove love) {
        addMapObject(love);
        broadcastMessage(love.makeSpawnData());
        TimerManager tMan = TimerManager.getInstance();
        tMan.schedule(new Runnable() {
            @Override
            public void run() {
                removeMapObject(love);
                broadcastMessage(love.makeDestroyData());
            }
        }, 1000 * 60 * 60);
    }

    public void spawnMist(final MapleMist mist, final int duration, boolean fake) {
        if (this.hasEvent) { //no mists on events
            return;
        }
        addMapObject(mist);
        broadcastMessage(mist.makeSpawnData());
        TimerManager tMan = TimerManager.getInstance();
        Runnable poisonTask = new Runnable() {
            @Override
            public void run() {
                if (mist.isPoison()) {
                    //log.debug("烟雾带毒");
                    for (MapleMapObject mo : getMapObjectsInBox(mist.getBox(), Collections.singletonList(MapleMapObjectType.MONSTER))) {
                        if (mist.makeChanceResult()) {
                            MonsterStatusEffect poisonEffect = new MonsterStatusEffect(Collections.singletonMap(MapleMonsterStat.POISON, 1), mist.getSourceSkill(), false);
                            ((MapleMonster) mo).applyStatus(mist.getOwner(), poisonEffect, true, duration);
                        }
                    }
//                } else if (mist.getSourceId() == 龙神.极光恢复) {
//                    double multiplier = (double) SkillFactory.getSkill(mist.getSourceId()).getEffect(mist.getOwner().getSkillLevel(mist.getSourceId())).getX() / 100;
//                    for (MapleMapObject mo : getMapObjectsInBox(mist.getBox(), Collections.singletonList(MapleMapObjectType.PLAYER))) {
//                        final MapleCharacter chr = (MapleCharacter) mo;
//                        if (chr.getPartyId() == mist.getOwner().getPartyId()) {
//                            chr.addMP((int) (chr.getMaxMp() * multiplier));
//                        }
//                    }
                } else if (mist.isItemMist()) {
                    int itemid = mist.getSourceId();
                    if (itemid == 5281000) {
                        itemid = 2022327; //臭屁
                    } else if (itemid == 5281000) {
                        itemid = 2022327; //花香
                    }
                    mist.getOwner().giveItemBuff(itemid);
                }
            }
        };
        poisonSchedule = tMan.register(poisonTask, 2000, 2500);
        tMan.schedule(new Runnable() {
            @Override
            public void run() {
                removeMapObject(mist);
                if (poisonSchedule != null) {
                    poisonSchedule.cancel(false);
                }
                broadcastMessage(mist.makeDestroyData());
            }
        }, duration);
    }

    public List<MapleMapObject> getMapObjectsInBox(Rectangle box, List<MapleMapObjectType> types) {
        List<MapleMapObject> ret = new LinkedList<MapleMapObject>();
        for (MapleMapObject l : core.values()) {
            if (types.contains(l.getType())) {
                if (box.contains(l.getPosition())) {
                    ret.add(l);
                }
            }
        }
        return ret;
    }

    public void disappearingItemDrop(final MapleMapObject dropper, final MapleCharacter owner, final IItem item, Point pos) {
        final Point droppos = calcDropPos(pos, pos);
        final MapleMapItem drop = new MapleMapItem(item, droppos, dropper, owner);
        broadcastMessage(MaplePacketCreator.dropItemFromMapObject(item.getItemId(), drop.getObjectId(), 0, 0, dropper.getPosition(), droppos, (byte) 3), drop.getPosition());
    }

    public void spawnItemDrop(final MapleMapObject dropper, final MapleCharacter owner, final IItem item, Point pos, final boolean ffaDrop, final boolean expire) {
        TimerManager tMan = TimerManager.getInstance();
        final Point droppos = calcDropPos(pos, pos);
        final MapleMapItem drop = new MapleMapItem(item, droppos, dropper, owner);
        spawnAndAddRangedMapObject(drop, new DelayedPacketCreation() {
            public void sendPackets(MapleClient c) {
                c.getSession().write(MaplePacketCreator.dropItemFromMapObject(item.getItemId(), drop.getObjectId(), 0, ffaDrop ? 0 : owner.getId(),
                        dropper.getPosition(), droppos, (byte) 1));
            }
        }, null);
        broadcastMessage(MaplePacketCreator.dropItemFromMapObject(item.getItemId(), drop.getObjectId(), 0, ffaDrop ? 0
                : owner.getId(), dropper.getPosition(), droppos, (byte) 0), drop.getPosition());

        if (expire) {
            tMan.schedule(new ExpireMapItemJob(drop), dropLife);
        }

        activateItemReactors(drop);
    }

    private class TimerDestroyWorker implements Runnable {

        @Override
        public void run() {
            if (mapTimer != null) {
                int warpMap = mapTimer.warpToMap();
                int minWarp = mapTimer.minLevelToWarp();
                int maxWarp = mapTimer.maxLevelToWarp();
                mapTimer = null;
                if (warpMap != -1) {
                    MapleMap map2wa2 = ChannelServer.getInstance(channel).getMapFactory().getMap(warpMap);
                    String warpmsg = "你即将被传送到 " + map2wa2.getStreetName() + " : " + map2wa2.getMapName();
                    broadcastMessage(MaplePacketCreator.serverNotice(6, warpmsg));
                    Collection<MapleCharacter> cmc = new LinkedHashSet<MapleCharacter>(getCharacters());
                    for (MapleCharacter chr : cmc) {
                        try {
                            if (chr.getLevel() >= minWarp && chr.getLevel() <= maxWarp) {
                                chr.changeMap(map2wa2, map2wa2.getPortal(0));
                            } else {
                                chr.getClient().getSession().write(MaplePacketCreator.serverNotice(5, "You are not at least level " + minWarp + " or you are higher than level " + maxWarp + "."));
                            }
                        } catch (Exception ex) {
                            chr.getClient().getSession().write(MaplePacketCreator.serverNotice(5, "传送时出现问题，请联系GM！"));
                        }
                    }
                }
            }
        }
    }

    public void addMapTimer(int duration) {
        ScheduledFuture<?> sf0f = TimerManager.getInstance().schedule(new TimerDestroyWorker(), duration * 1000);
        mapTimer = new MapleMapTimer(sf0f, duration, -1, -1, -1);

        broadcastMessage(mapTimer.makeSpawnData());
    }

    public void addMapTimer(int duration, int mapToWarpTo) {
        ScheduledFuture<?> sf0f = TimerManager.getInstance().schedule(new TimerDestroyWorker(), duration * 1000);
        mapTimer = new MapleMapTimer(sf0f, duration, mapToWarpTo, 0, 256);
        broadcastMessage(mapTimer.makeSpawnData());
    }

    public void addMapTimer(int duration, int mapToWarpTo, int minLevelToWarp) {
        ScheduledFuture<?> sf0f = TimerManager.getInstance().schedule(new TimerDestroyWorker(), duration * 1000);
        mapTimer = new MapleMapTimer(sf0f, duration, mapToWarpTo, minLevelToWarp, 256);
        broadcastMessage(mapTimer.makeSpawnData());
    }

    public void addMapTimer(int duration, int mapToWarpTo, int minLevelToWarp, int maxLevelToWarp) {
        ScheduledFuture<?> sf0f = TimerManager.getInstance().schedule(new TimerDestroyWorker(), duration * 1000);
        mapTimer = new MapleMapTimer(sf0f, duration, mapToWarpTo, minLevelToWarp, maxLevelToWarp);
        broadcastMessage(mapTimer.makeSpawnData());
    }

    public void clearMapTimer() {
        if (mapTimer != null) {
            mapTimer.getSF0F().cancel(true);
        }
        mapTimer = null;
    }

    private void activateItemReactors(MapleMapItem drop) {
        IItem item = drop.getItem();
        final TimerManager tMan = TimerManager.getInstance(); //check for reactors on map that might use this item
        for (MapleMapObject o : core.values()) {
            if (o.getType() == MapleMapObjectType.REACTOR) {
                if (((MapleReactor) o).getStats() != null
                        && ((MapleReactor) o).getReactorType() == 100) {
                    if (((MapleReactor) o).getReactItem().getLeft() == item.getItemId() && ((MapleReactor) o).getReactItem().getRight() <= item.getQuantity()) {
                        Rectangle area = ((MapleReactor) o).getArea();

                        if (area.contains(drop.getPosition())) {
                            MapleClient ownerClient = null;
                            if (drop.getOwner() != null) {
                                ownerClient = drop.getOwner().getClient();
                            }
                            MapleReactor reactor = (MapleReactor) o;
                            if (!reactor.isTimerActive()) {
                                tMan.schedule(new ActivateItemReactor(drop, reactor, ownerClient), 5000);
                                reactor.setTimerActive(true);
                            }
                        }
                    }
                }
            }
        }
    }

    public void AriantPQStart() {
        int i = 1;
        for (MapleCharacter chars2 : this.getCharacters()) {
            broadcastMessage(MaplePacketCreator.updateAriantPQRanking(chars2.getName(), 0, false));
            broadcastMessage(MaplePacketCreator.serverNotice(0, MaplePacketCreator.updateAriantPQRanking(chars2.getName(), 0, false).toString()));
            if (this.getCharacters().size() > i) {
                broadcastMessage(MaplePacketCreator.updateAriantPQRanking(null, 0, true));
                broadcastMessage(MaplePacketCreator.serverNotice(0, MaplePacketCreator.updateAriantPQRanking(chars2.getName(), 0, true).toString()));
            }
            i++;
        }
    }

    public void startMapEffect(String msg, int itemId) {
        if (mapEffect != null) {
            return;
        }
        mapEffect = new MapleMapEffect(msg, itemId);
        broadcastMessage(mapEffect.makeStartData());
        TimerManager tMan = TimerManager.getInstance();
        tMan.schedule(new Runnable() {
            @Override
            public void run() {
                broadcastMessage(mapEffect.makeDestroyData());
                mapEffect = null;
            }
        }, 30000);
    }

    /**
     * Adds a player to this map and sends nescessary data
     *
     * @param chr
     */
    public void addPlayer(MapleCharacter chr) {
        core.addMapObject(chr);
        offlinePlayer.onAddPlayer(chr);
        sendObjectPlacement(chr.getClient());
        for (MaplePet pet : chr.getPets()) {
            if (pet != null) {
                chr.getClient().getSession().write(MaplePacketCreator.showPet(chr, pet, false, false));
            }
        }
        if (hasForcedEquip()) {
            chr.getClient().getSession().write(MaplePacketCreator.showForcedEquip());
        }
        if (chr.getMapId() >= 140090100 && chr.getMapId() <= 140090500 || chr.getJob().getId() == 1000 && chr.getMapId() != 130030000) {
            // chr.getClient().getSession().write(MaplePacketCreator.spawnTutorialSummon(1));
        }
        if (!UserEnter.equals("")) {
            MapScriptManager.getInstance().getMapScript(chr.getClient(), UserEnter, false);
        }
        if (!FirstUserEnter.equals("")) {
            if (getCharacters().size() == 1) {
                MapScriptManager.getInstance().getMapScript(chr.getClient(), FirstUserEnter, true);
            }
        }
        if (mapEffect != null) {
            mapEffect.sendStartData(chr.getClient());
        }
        if (chr.getChalkboard() != null) {
            chr.getClient().getSession().write(MaplePacketCreator.useChalkboard(chr, false));
        }
        /*
         * if (chr.getEnergy() >= 10000) { broadcastMessage(chr,
         * (MaplePacketCreator.giveForeignEnergyCharge(chr.getId(), 10000))); }
         */
        if (getTimeLimit() > 0 && getForcedReturnMap() != null) {
            chr.getClient().getSession().write(MaplePacketCreator.getClock(getTimeLimit()));
            chr.startMapTimeLimitTask(this, this.getForcedReturnMap());
        }
        if (mapTimer != null) {
            chr.getClient().getSession().write(MaplePacketCreator.getClock(mapTimer.getTimeLeft()));
        }
        if (chr.getEventInstance() != null && chr.getEventInstance().isTimerStarted()) {
            chr.getClient().getSession().write(MaplePacketCreator.getClock((int) (chr.getEventInstance().getTimeLeft() / 1000)));
        }
        if (hasClock()) {
            Calendar cal = Calendar.getInstance();
            chr.getClient().getSession().write((MaplePacketCreator.getClockTime(cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE), cal.get(Calendar.SECOND))));
        }
        if (hasBoat() == 2) {
            chr.getClient().getSession().write((MaplePacketCreator.boatPacket(true)));
        } else if (hasBoat() == 1 && (chr.getMapId() != 200090000 || chr.getMapId() != 200090010)) {
            chr.getClient().getSession().write(MaplePacketCreator.boatPacket(false));
        }
        if (chr.getAndroid() != null) {
            chr.getAndroid().setPos(chr.getPosition());
            broadcastMessage(chr, MaplePacketCreator.spawnAndroid(chr, chr.getAndroid()));
        }

        chr.receivePartyMemberHP();//检测组队Hp。
        chr.getClient().getSession().write(MaplePacketCreator.showMapEffect("maplemap/enter/" + this.mapid));//显示地图名
    }

    public void removePlayer(MapleCharacter chr) {
        removeMapObject(chr);
        if (core.GetPlayerCount() == 0 && task != null) {
            task.OnAllPlayerLevae(this);
        }
        broadcastMessage(MaplePacketCreator.removePlayerFromMap(chr.getId()));
        for (MapleMonster monster : chr.getControlledMonsters()) {
            monster.setController(null);
            monster.setControllerHasAggro(false);
            monster.setControllerKnowsAboutAggro(false);
            updateMonsterController(monster);
        }
        chr.leaveMap();
        chr.cancelMapTimeLimitTask();
        //取消召唤兽 一般切换地图时用
        for (List<MapleSummon> summons : chr.getSummons().values()) {
            for (MapleSummon summon : summons) {
                if (summon.isStationary()) {
                    //           chr.cancelBuffStats(MapleBuffStat.PUPPET);
                } else {
                    if (!summon.isBuffSummon()) {
                        chr.getSummons().remove(summon.getSkill());
                        broadcastMessage(MaplePacketCreator.removeSpecialMapObject(summon, true));
                        chr.removeVisibleMapObject(summon);
                    }
                    removeMapObject(summon);
                }
            }
        }
        //换地图时消除机械的传送门
        for (MapleDoor2 door2 : chr.getDoors2()) {
            broadcastMessage(MaplePacketCreator.取消传送门(chr.getId(), door2.isFirst()));
            removeMapObject(door2);
        }
        chr.clearDoors2(); //清除传送门集合里的所有数据


        /*
         * if(poisonSchedule != null) { //去除烟雾数据 poisonSchedule.cancel(false);
         * poisonSchedule = null; }
         */
    }

    public MaplePortal findClosestSpawnpoint(Point from) {
        MaplePortal closest = null;
        double shortestDistance = Double.POSITIVE_INFINITY;
        for (MaplePortal portal : portals.values()) {
            double distance = portal.getPosition().distanceSq(from);
            if (portal.getType() >= 0 && portal.getType() <= 2 && distance < shortestDistance && portal.getTargetMapId() == 999999999) {
                closest = portal;
                shortestDistance = distance;
            }
        }
        return closest;
    }

    /**
     * Broadcast a message to everyone in the map
     *
     * @param packet
     */
    public void broadcastMessage(MaplePacket packet) {
        broadcastMessage(null, packet, Double.POSITIVE_INFINITY, null);
    }

    public void broadcastMessage(final MapleCharacter source, final MaplePacket packet) {
        for (MapleCharacter chr : core.GetMapPlayers()) {
            if (chr != source) {
                chr.getClient().getSession().write(packet);
            }
        }
    }

    /**
     * Nonranged. Repeat to source according to parameter.
     *
     * @param source
     * @param packet
     * @param repeatToSource
     */
    public void broadcastMessage(MapleCharacter source, MaplePacket packet, boolean repeatToSource) {
        broadcastMessage(repeatToSource ? null : source, packet, Double.POSITIVE_INFINITY, source.getPosition());
    }

    /**
     * Ranged and repeat according to parameters.
     *
     * @param source
     * @param packet
     * @param repeatToSource
     * @param ranged
     */
    public void broadcastMessage(MapleCharacter source, MaplePacket packet, boolean repeatToSource, boolean ranged) {
        broadcastMessage(repeatToSource ? null : source, packet, ranged ? MapleCharacter.MAX_VIEW_RANGE_SQ : Double.POSITIVE_INFINITY, source.getPosition());
    }

    /**
     * Always ranged from Point.
     *
     * @param packet
     * @param rangedFrom
     */
    public void broadcastMessage(MaplePacket packet, Point rangedFrom) {
        broadcastMessage(null, packet, MapleCharacter.MAX_VIEW_RANGE_SQ, rangedFrom);
    }

    /**
     * Always ranged from point. Does not repeat to source.
     *
     * @param source
     * @param packet
     * @param rangedFrom
     */
    public void broadcastMessage(MapleCharacter source, MaplePacket packet, Point rangedFrom) {
        broadcastMessage(source, packet, MapleCharacter.MAX_VIEW_RANGE_SQ, rangedFrom);
    }

    private void broadcastMessage(final MapleCharacter source, final MaplePacket packet, final double rangeSq, final Point rangedFrom) {
        for (MapleCharacter chr : core.GetMapPlayers()) {
            if (chr != source) {
                if (rangeSq < Double.POSITIVE_INFINITY) {
                    if (rangedFrom.distanceSq(chr.getPosition()) <= rangeSq) {
                        chr.getClient().getSession().write(packet);
                    }
                } else {
                    chr.getClient().getSession().write(packet);
                }
            }
        }
    }

    private boolean isNonRangedType(MapleMapObjectType type) {
        switch (type) {
            case NPC:
            case PLAYER:
            case MIST:
            case HIRED_MERCHANT:
            case LOVE:
                return true;
        }
        return false;
    }

    private void sendObjectPlacement(MapleClient mapleClient) {

        /**
         * 让我能看见他们代码段
         */
        for (MapleMapObject o : core.values()) {
            if (isNonRangedType(o.getType())) {
                o.sendSpawnData(mapleClient);
            } else if (o.getType() == MapleMapObjectType.MONSTER) {
                updateMonsterController((MapleMonster) o);
            }
        }

        MapleCharacter chr = mapleClient.getPlayer();

        if (chr != null) {
            for (MapleMapObject o
                    : getMapObjectsInRange(chr.getPosition(), MapleCharacter.MAX_VIEW_RANGE_SQ, rangedMapobjectTypes)) {
                if (o.getType() == MapleMapObjectType.REACTOR) {
                    if (((MapleReactor) o).isAlive()) {
                        o.sendSpawnData(chr.getClient());
                        chr.addVisibleMapObject(o);
                    }
                } else {
                    o.sendSpawnData(chr.getClient());
                    chr.addVisibleMapObject(o);
                }
            }
        } else {
            log.info("sendObjectPlacement invoked with null char");
        }

        /**
         * 让他们能看见我的代码段.
         */
        for (MapleCharacter mapleCharacter : getCharacters()) {
            if (mapleCharacter != chr) {
                chr.sendSpawnData(mapleCharacter.getClient());
            }
        }
    }

    public List<MapleMapObject> getMapObjectsInRange(Point from, double rangeSq, List<MapleMapObjectType> types) {
        List<MapleMapObject> ret = new LinkedList<MapleMapObject>();
        for (MapleMapObject l : core.values()) {
            if (types.contains(l.getType())) {
                if (from.distanceSq(l.getPosition()) <= rangeSq) {
                    ret.add(l);
                }
            }
        }
        return ret;
    }

    public List<MapleMapObject> getItemsInRange(Point from, double rangeSq) {
        final List<MapleMapObject> ret = new LinkedList<MapleMapObject>();
        for (MapleMapObject l : core.values()) {
            if (l.getType() == MapleMapObjectType.ITEM) {
                if (from.distanceSq(l.getPosition()) <= rangeSq) {
                    ret.add(l);
                }
            }
        }
        return ret;
    }

    public List<MapleMapObject> getMapObjectsInRect(Rectangle box, List<MapleMapObjectType> types) {
        return core.getMapObjectsInRect(box, types);
    }

    public List<MapleMapObject> getMapObjectsInRect(Rectangle box, MapleMapObjectType types) {
        return core.getMapObjectsInRect(box, types);
    }

    public List<MapleCharacter> getPlayersInRect(final Rectangle box, final List<MapleCharacter> chr) {
        final List<MapleCharacter> character = new LinkedList<MapleCharacter>();
        for (MapleCharacter a : core.GetMapPlayers()) {
            if (chr.contains(a.getClient().getPlayer())) {
                if (box.contains(a.getPosition())) {
                    character.add(a);
                }
            }
        }
        return character;
    }

    public List<MapleCharacter> getPlayersInRect(final Rectangle box) {
        final List<MapleCharacter> character = new LinkedList<MapleCharacter>();
        for (MapleCharacter a : core.GetMapPlayers()) {
            if (box.contains(a.getPosition())) {
                character.add(a);
            }
        }
        return character;
    }

    public void addPortal(MaplePortal myPortal) {
        portals.put(myPortal.getId(), myPortal);
    }

    public MaplePortal getPortal(String portalname) {
        for (MaplePortal port : portals.values()) {
            if (port.getName().equals(portalname)) {
                return port;
            }
        }
        return null;
    }

    public Collection<MaplePortal> getPortals() {
        return Collections.unmodifiableCollection(portals.values());
    }

    public MaplePortal getPortal(int portalid) {
        return portals.get(portalid);
    }

    public void setFootholds(MapleFootholdTree footholds) {
        this.footholds = footholds;
    }

    public MapleFootholdTree getFootholds() {
        return footholds;
    }

    /**
     * not threadsafe, please synchronize yourself
     *
     * @param monster
     */
    public SpawnPoint addMonsterSpawn(MapleMonster monster, int mobTime) {
        Point newpos = calcPointBelow(monster.getPosition());
        newpos.y -= 1;
        SpawnPoint sp = new SpawnPoint(monster, newpos, mobTime);

        monsterSpawn.add(sp);
        return sp;
    }

    public float getMonsterRate() {
        return monsterRate;
    }

    public Collection<MapleCharacter> getCharacters() {
        return core.GetMapPlayers();
    }

    public MapleCharacter getCharacterById(int id) {
        for (MapleCharacter c : getCharacters()) {
            if (c.getId() == id) {
                return c;
            }
        }
        return null;
    }

    public MapleCharacter getCharacterByName(String name) {
        for (MapleCharacter chr : getCharacters()) {
            if (chr.getName().equals(name)) {
                return chr;
            }
        }
        return null;
    }

    public void updateMapObjectVisibility(MapleCharacter chr, MapleMapObject mo) {
        if (!chr.isMapObjectVisible(mo)) { // monster entered view range
            if (mo.getType() == MapleMapObjectType.SUMMON || mo.getPosition().distanceSq(chr.getPosition()) <= MapleCharacter.MAX_VIEW_RANGE_SQ) {
                chr.addVisibleMapObject(mo);
                mo.sendSpawnData(chr.getClient());
            }
        } else { //怪物离开了可见范围
            if (mo.getType() != MapleMapObjectType.SUMMON && mo.getPosition().distanceSq(chr.getPosition()) > MapleCharacter.MAX_VIEW_RANGE_SQ) {
                chr.removeVisibleMapObject(mo);
                mo.sendDestroyData(chr.getClient());
            }
        }
    }

    public void moveMonster(final MapleMonster monster, Point reportedPos) {
        monster.setPosition(reportedPos);
        for (MapleCharacter chr : core.GetMapPlayers()) {
            updateMapObjectVisibility(chr, monster);
        }
    }

    public void movePlayer(final MapleCharacter player, Point newPosition) {
        player.setPosition(newPosition);
        try {
            player.getMove_lock().lock();
            for (MapleMapObject mo : player.getVisibleMapObjects()) {
                if (mo != null) {
                    if (core.get(mo.getObjectId()) == mo) {
                        if (!player.isMapObjectVisible(mo)) { // monster entered view range
                            if (mo.getType() == MapleMapObjectType.SUMMON || mo.getPosition().distanceSq(player.getPosition()) <= MapleCharacter.MAX_VIEW_RANGE_SQ) {
                                player.addVisibleMapObject(mo);
                                mo.sendSpawnData(player.getClient());
                            }
                        } else { //怪物离开了可见范围
                            if (mo.getType() != MapleMapObjectType.SUMMON && mo.getPosition().distanceSq(player.getPosition()) > MapleCharacter.MAX_VIEW_RANGE_SQ) {
                                player.removeVisibleMapObject(mo);
                                mo.sendDestroyData(player.getClient());
                            }
                        }
                    } else {
                        player.removeVisibleMapObject(mo);
                    }
                }
            }
        } finally {
            player.getMove_lock().unlock();
        }

        for (MapleMapObject mo : getMapObjectsInRange(player.getPosition(), MapleCharacter.MAX_VIEW_RANGE_SQ, rangedMapobjectTypes)) {
            if (mo != null) {
                if (!player.isMapObjectVisible(mo)) {
                    mo.sendSpawnData(player.getClient());
                    player.addVisibleMapObject(mo);
                }
            }
        }

        if (getId() == 240040611) { // 九灵巢穴
            if (getMapObjectsInRange(player.getPosition(), 25000, Arrays.asList(MapleMapObjectType.REACTOR)).size() > 0) {
                MapleReactor reactor = getReactorById(2408004);
                if (reactor.getState() == 0) {
                    reactor.hitReactor(player.getClient());
                }
            }
        }


        /*
         * for (ListIterator<MapleMapObject> it =
         * player.getVisibleMapObjects().listIterator(); it.hasNext();) {
         * MapleMapObject mo = it.next(); if (mo != null) { if
         * (mapobjects.get(mo.getObjectId()) == mo) { if
         * (!player.isMapObjectVisible(mo)) { // monster entered view range if
         * (mo.getType() == MapleMapObjectType.SUMMON ||
         * mo.getPosition().distanceSq(player.getPosition()) <=
         * MapleCharacter.MAX_VIEW_RANGE_SQ) { it.add(mo);
         * mo.sendSpawnData(player.getClient()); } } else { //怪物离开了可见范围 if
         * (mo.getType() != MapleMapObjectType.SUMMON &&
         * mo.getPosition().distanceSq(player.getPosition()) >
         * MapleCharacter.MAX_VIEW_RANGE_SQ) { it.remove();
         * mo.sendDestroyData(player.getClient()); } } } else { it.remove(); } }
         * }
         */

    }

    public void setSpawnRateMulti(int sr) {
        if (sr == 0) {
            return;
        }
        boolean decSpawn = sr < 0;
        if (decSpawn) {
            this.monsterRate *= (-sr);
        } else {
            this.monsterRate /= sr;
        }

    }

    public float getSpawnRate() {
        return this.monsterRate;
    }

    public float getOrigSpawnRate() {
        return this.origMobRate;
    }

    public void setSpawnRate(float sr) {
        this.monsterRate = sr;

    }

    public void resetSpawnRate() {
        this.monsterRate = this.origMobRate;

    }

    public boolean isSpawnRateModified() {
        return this.monsterRate != this.origMobRate;
    }

    public void resetSpawn() {
        if (spawnWorker != null) {
            spawnWorker.cancel(true);
        }
        if (this.monsterRate > 0) {
            spawnWorker = TimerManager.getInstance().register(new RespawnWorker(), 10000);
        }
    }

    public void spawnDebug(MessageCallback mc) {
        mc.dropMessage("Spawndebug...");
        mc.dropMessage("Mapobjects in map: " + core.size() + " \"spawnedMonstersOnMap\": "
                + spawnedMonstersOnMap + " spawnpoints: " + monsterSpawn.size()
                + " maxRegularSpawn: " + getMaxRegularSpawn() + " spawnRate: " + this.monsterRate + " original spawnRate: " + this.origMobRate);
        int numMonsters = 0;
        for (MapleMapObject mo : core.values()) {
            if (mo instanceof MapleMonster) {
                numMonsters++;
            }
        }
        mc.dropMessage("actual monsters: " + numMonsters);
    }

    public void spawnPB() {
        killAllMonsters();
        spawnMonsterwithpos(MapleLifeFactory.getMonster(8820009), new Point(7, -42));
    }

    public void spawnMonsterwithpos(MapleMonster mob, Point pos) {
        mob.setPosition(pos);
        spawnMonster(mob);
    }

    private int getMaxRegularSpawn() {
        return (int) (monsterSpawn.size() / monsterRate);
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

    public void setClock(boolean hasClock) {
        this.clock = hasClock;
    }

    public boolean hasClock() {
        return clock;
    }

    public void setTown(boolean isTown) {
        this.town = isTown;
    }

    public boolean isTown() {
        return town;
    }

    public void setAllowShops(boolean allowShops) {
        this.allowShops = allowShops;
    }

    public boolean allowShops() {
        return allowShops;
    }

    public void setStreetName(String streetName) {
        this.streetName = streetName;
    }

    public void setEverlast(boolean everlast) {
        this.everlast = everlast;
    }

    public boolean getEverlast() {
        return everlast;
    }

    public int getSpawnedMonstersOnMap() {
        return spawnedMonstersOnMap.get();
    }

    private class ExpireMapItemJob implements Runnable {

        private MapleMapItem mapitem;

        public ExpireMapItemJob(MapleMapItem mapitem) {
            this.mapitem = mapitem;
        }

        @Override
        public void run() {
            if (mapitem != null && mapitem == getMapObject(mapitem.getObjectId())) {
                synchronized (mapitem) {
                    if (mapitem.isPickedUp()) {
                        return;
                    }
                    MapleMap.this.broadcastMessage(MaplePacketCreator.removeItemFromMap(mapitem.getObjectId(), 0, 0),
                            mapitem.getPosition());
                    MapleMap.this.removeMapObject(mapitem);
                    mapitem.setPickedUp(true);
                }
            }
        }
    }

    private class ActivateItemReactor implements Runnable {

        private MapleMapItem mapitem;
        private MapleReactor reactor;
        private MapleClient c;

        public ActivateItemReactor(MapleMapItem mapitem, MapleReactor reactor, MapleClient c) {
            this.mapitem = mapitem;
            this.reactor = reactor;
            this.c = c;
        }

        @Override
        public void run() {
            if (mapitem != null && mapitem == getMapObject(mapitem.getObjectId())) {
                synchronized (mapitem) {
                    TimerManager tMan = TimerManager.getInstance();
                    if (mapitem.isPickedUp()) {
                        return;
                    }
                    MapleMap.this.broadcastMessage(MaplePacketCreator.removeItemFromMap(mapitem.getObjectId(), 0, 0), mapitem.getPosition());
                    MapleMap.this.removeMapObject(mapitem);
                    reactor.hitReactor(c);
                    reactor.setTimerActive(false);
                    if (reactor.getDelay() > 0) { //This shit is negative.. Fix?
                        tMan.schedule(new Runnable() {
                            @Override
                            public void run() {
                                reactor.setState((byte) 0);
                                broadcastMessage(MaplePacketCreator.triggerReactor(reactor, 0));
                            }
                        }, reactor.getDelay());
                    }
                }
            }
        }
    }

    /*
     * private class RespawnWorker implements Runnable { @Override public void
     * run() { if (characters.size() == 0) { return; } else { int numShouldSpawn
     * = getMaxRegularSpawn() - spawnedMonstersOnMap.get(); if (numShouldSpawn >
     * 0) { List<SpawnPoint> randomSpawn = new
     * ArrayList<SpawnPoint>(monsterSpawn); Collections.shuffle(randomSpawn);
     * int spawned = 0; for (SpawnPoint spawnPoint : randomSpawn) { if
     * (spawnPoint.shouldSpawn()) { spawnPoint.spawnMonster(MapleMap.this);
     * spawned++; } if (spawned >= numShouldSpawn) { break; } } } } } }
     *
     */
    private class RespawnWorker implements Runnable {

        @Override
        public void run() {
            int playersOnMap = core.GetPlayerCount();

            if (playersOnMap == 0) {
                return;
            }

            int ispawnedMonstersOnMap = spawnedMonstersOnMap.get();
            int numShouldSpawn = (int) Math.round(Math.random() * ((2 + playersOnMap / 1.1 + (getMaxRegularSpawn() - ispawnedMonstersOnMap) / 4.0)));
            if (numShouldSpawn + ispawnedMonstersOnMap > getMaxRegularSpawn()) {
                numShouldSpawn = getMaxRegularSpawn() - ispawnedMonstersOnMap;
            }

            if (numShouldSpawn <= 0) {
                return;
            }

            // k find that many monsters that need respawning and respawn them O.o
            List<SpawnPoint> randomSpawn = new ArrayList<SpawnPoint>(monsterSpawn);
            Collections.shuffle(randomSpawn);
            int spawned = 0;
            for (SpawnPoint spawnPoint : randomSpawn) {
                if (spawnPoint.shouldSpawn()) {
                    spawnPoint.spawnMonster(MapleMap.this);
                    spawned++;
                }
                if (spawned >= numShouldSpawn) {
                    break;
                }
            }
        }
    }

    private static interface DelayedPacketCreation {

        void sendPackets(MapleClient c);
    }

    private static interface SpawnCondition {

        boolean canSpawn(MapleCharacter chr);
    }

    public int getHPDec() {
        return decHP;
    }

    public void setHPDec(int delta) {
        decHP = delta;
    }

    public int getHPDecProtect() {
        return this.protectItem;
    }

    public void setHPDecProtect(int delta) {
        this.protectItem = delta;
    }

    public int hasBoat() {
        if (boat && docked) {
            return 2;
        } else if (boat) {
            return 1;
        } else {
            return 0;
        }
    }

    public void setBoat(boolean hasBoat) {
        this.boat = hasBoat;
    }

    public void setDocked(boolean isDocked) {
        this.docked = isDocked;
    }

    public void setEvent(boolean hasEvent) {
        this.hasEvent = hasEvent;
    }

    public boolean hasEvent() {
        return hasEvent;
    }

    public MapleMapCore getCore() {
        return core;
    }

    public int countReactorsOnMap() {
        int count = 0;
        Collection<MapleMapObject> mmos = this.getMapObjects();
        for (MapleMapObject mmo : mmos) {
            if (mmo instanceof MapleReactor) {
                count++;
            }
        }
        return count;
    }

    public int countMobOnMap() {
        int count = 0;
        Collection<MapleMapObject> mmos = this.getMapObjects();
        for (MapleMapObject mmo : mmos) {
            if (mmo instanceof MapleMonster) {
                count++;
            }
        }
        return count;
    }

    public int countMobOnMap(int monsterid) {
        int count = 0;
        Collection<MapleMapObject> mmos = this.getMapObjects();
        for (MapleMapObject mmo : mmos) {
            if (mmo instanceof MapleMonster) {
                MapleMonster monster = (MapleMonster) mmo;
                if (monster.getId() == monsterid) {
                    count++;
                }
            }
        }
        return count;
    }

    public MapleReactor getReactorById(int id) {
        for (MapleMapObject obj : core.values()) {
            if (obj.getType() == MapleMapObjectType.REACTOR) {
                if (((MapleReactor) obj).getId() == id) {
                    return (MapleReactor) obj;
                }
            }
        }
        return null;
    }

    public boolean isPQMap() { //Does NOT include CPQ maps
        int tmapid = this.getId();
        if ((tmapid > 922010000 && tmapid < 922011100) || (tmapid >= 103000800 && tmapid < 103000890)) { //kpq + lpq only atm
            return true;
        }
        return false;
    }

    public boolean isCPQMap() {
        switch (this.getId()) {
            case 980000101:
            case 980000201:
            case 980000301:
            case 980000401:
            case 980000501:
            case 980000601:
                return true;
            default:
                return false;
        }
    }

    public boolean isBlueCPQMap() {
        switch (this.getId()) {
            case 980000501:
            case 980000601:
                return true;
            default:
                return false;
        }
    }

    public boolean isPurpleCPQMap() {
        switch (this.getId()) {
            case 980000301:
            case 980000401:
                return true;
            default:
                return false;
        }
    }

    public void addClock(int seconds) {
        broadcastMessage(MaplePacketCreator.getClock(seconds));
    }

    public boolean cannotInvincible() {
        return cannotInvincible;
    }

    public void setCannotInvincible(boolean b) {
        cannotInvincible = b;
    }

    public void setFieldLimit(int fl) {
        canVipRock = !FieldLimit.CANNOTVIPROCK.check(fl);
    }

    public boolean canVipRock() {
        return canVipRock;
    }

    public void setFirstUserEnter(String onFirstUserEnter) {
        this.FirstUserEnter = onFirstUserEnter;
    }

    public void setUserEnter(String onUserEnter) {
        this.UserEnter = onUserEnter;
    }

    public void killAllBoogies() {
        List<MapleMapObject> monsters = getMapObjectsInRange(new Point(0, 0), Double.POSITIVE_INFINITY, Arrays.asList(MapleMapObjectType.MONSTER));
        for (MapleMapObject monstermo : monsters) {
            MapleMonster monster = (MapleMonster) monstermo;
            if (monster.getId() == 3230300 || monster.getId() == 3230301 || monster.getName().toLowerCase().contains("boogie")) {
                spawnedMonstersOnMap.decrementAndGet();
                monster.setHp(0);
                broadcastMessage(MaplePacketCreator.killMonster(monster.getObjectId(), true), monster.getPosition());
                removeMapObject(monster);
            }
        }
        this.broadcastMessage(MaplePacketCreator.serverNotice(6, "As the rock crumbled, Jr. Boogie fell in great pain and disappeared."));
    }

    public String getUserEnter() {
        return UserEnter;
    }

    public String getFirstUserEnter() {
        return FirstUserEnter;
    }

    private boolean hasForcedEquip() {
        return fieldType == 81 || fieldType == 82;
    }

    public void setFieldType(int fieldType) {
        this.fieldType = fieldType;
    }

    public void setTimeMobId(int id) {
        this.timeMobId = id;
    }

    public void setTimeMobMessage(String message) {
        this.timeMobMessage = message;
    }

    public int getTimeMobId() {
        return timeMobId;
    }

    public String getTimeMobMessage() {
        return timeMobMessage;
    }

    public MaplePortal findClosestPortal(Point from) {
        MaplePortal closest = null;
        double shortestDistance = Double.POSITIVE_INFINITY;
        for (MaplePortal portal : portals.values()) {
            double distance = portal.getPosition().distanceSq(from);
            if (distance < shortestDistance) {
                closest = portal;
                shortestDistance = distance;
            }
        }
        return closest;
    }

    public MaplePortal getRandomSpawnpoint() {
        List<MaplePortal> spawnPoints = new ArrayList<MaplePortal>();
        for (MaplePortal portal : portals.values()) {
            if (portal.getType() >= 0 && portal.getType() <= 2) {
                spawnPoints.add(portal);
            }
        }
        MaplePortal portal = spawnPoints.get(new Random().nextInt(spawnPoints.size()));
        return portal != null ? portal : getPortal(0);
    }

    public void clearDrops(MapleCharacter player, boolean command) {
        List<MapleMapObject> items = player.getMap().getMapObjectsInRange(player.getPosition(), Double.POSITIVE_INFINITY, Arrays.asList(MapleMapObjectType.ITEM));
        for (MapleMapObject i : items) {
            player.getMap().removeMapObject(i);
            player.getMap().broadcastMessage(MaplePacketCreator.removeItemFromMap(i.getObjectId(), 0, player.getId()));
        }
        if (command) {
            player.message("Items Destroyed: " + items.size());
        }
    }

    public int mobCount() {
        return getMapObjectsInRange(new java.awt.Point(0, 0), (1.0D / 0.0D), Arrays.asList(new MapleMapObjectType[]{MapleMapObjectType.MONSTER})).size();
    }

    public int playerCount() {
        List players = getMapObjectsInRange(new java.awt.Point(0, 0), (1.0D / 0.0D), Arrays.asList(new MapleMapObjectType[]{MapleMapObjectType.PLAYER}));
        int count = players.size();
        return count;
    }

    public int gethiredmerchant() {
        return getMapObjectsInRange(new java.awt.Point(0, 0), (1.0D / 0.0D), Arrays.asList(new MapleMapObjectType[]{MapleMapObjectType.HIRED_MERCHANT})).size();
    }

    public final List<MapleMapObject> getAllMonster() {
        return getMapObjectsInRange(new Point(0, 0), Double.POSITIVE_INFINITY, Arrays.asList(MapleMapObjectType.MONSTER));
    }

    public final void killAllMonsters(final boolean animate) {
        for (final MapleMapObject monstermo : getAllMonster()) {
            final MapleMonster monster = (MapleMonster) monstermo;
            spawnedMonstersOnMap.decrementAndGet();
            monster.setHp(0);
            broadcastMessage(MaplePacketCreator.killMonster(monster.getObjectId(), animate ? 1 : 0));
            removeMapObject(monster);
        }
    }

    public Collection<MapleCharacter> getNearestPvpChar(Point attacker, double maxRange, double maxHeight, Collection<MapleCharacter> chr) {
        Collection character = new LinkedList();
        for (MapleCharacter a : this.core.GetMapPlayers()) {
            if (chr.contains(a.getClient().getPlayer())) {
                Point attackedPlayer = a.getPosition();
                MaplePortal Port = a.getMap().findClosestSpawnpoint(a.getPosition());
                Point nearestPort = Port.getPosition();
                double safeDis = attackedPlayer.distance(nearestPort);
                double distanceX = attacker.distance(attackedPlayer.getX(), attackedPlayer.getY());
                if (MaplePvp.isLeft && attacker.x > attackedPlayer.x && distanceX < maxRange && distanceX > 2.0 && attackedPlayer.y >= attacker.y - maxHeight && attackedPlayer.y <= attacker.y + maxHeight && safeDis > 2.0) {
                    character.add(a);
                }

                if (MaplePvp.isRight && attacker.x < attackedPlayer.x && distanceX < maxRange && distanceX > 2.0 && attackedPlayer.y >= attacker.y - maxHeight && attackedPlayer.y <= attacker.y + maxHeight && safeDis > 2.0) {
                    character.add(a);
                }
            }
        }
        return character;
    }

    public short getTop() {
        return top;
    }

    public void setTop(short top) {
        this.top = top;
    }

    public short getBottom() {
        return bottom;
    }

    public void setBottom(short bottom) {
        this.bottom = bottom;
    }

    public short getLeft() {
        return left;
    }

    public void setLeft(short left) {
        this.left = left;
    }

    public short getRight() {
        return right;
    }

    public void setRight(short right) {
        this.right = right;
    }

    public int getPartyBonusRate() {
        return partyBonusRate;
    }

    public void setPartyBonusRate(int ii) {
        this.partyBonusRate = ii;
    }

    public MapleOfflinePlayer getOfflinePlayer() {
        return offlinePlayer;
    }

    public void setOfflinePlayer(MapleOfflinePlayer offlinePlayer) {
        this.offlinePlayer = offlinePlayer;
        this.offlinePlayer.setMap(this);
    }

    protected ChannelServer getChannelServer() {
        return ChannelServer.getInstance(channel);
    }
}
