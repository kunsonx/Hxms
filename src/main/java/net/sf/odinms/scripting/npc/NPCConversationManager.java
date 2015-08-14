package net.sf.odinms.scripting.npc;

import java.awt.Point;
import java.sql.*;
import java.util.*;
import java.util.Map.Entry;
import net.sf.odinms.client.*;
import net.sf.odinms.client.messages.ServerNoticeMapleClientMessageCallback;
import net.sf.odinms.database.DatabaseConnection;
import net.sf.odinms.net.channel.ChannelServer;
import net.sf.odinms.net.channel.handler.DueyActionHandler;
import net.sf.odinms.net.world.MaplePartyCharacter;
import net.sf.odinms.net.world.guild.MapleGuild;
import net.sf.odinms.scripting.AbstractPlayerInteraction;
import net.sf.odinms.scripting.event.EventManager;
import net.sf.odinms.server.*;
import net.sf.odinms.server.life.MapleLifeFactory;
import net.sf.odinms.server.life.MapleMonster;
import net.sf.odinms.server.life.MapleMonsterStats;
import net.sf.odinms.server.maps.MapleMap;
import net.sf.odinms.server.maps.MapleMapFactory;
import net.sf.odinms.server.maps.MapleMapObject;
import net.sf.odinms.server.maps.MapleMapObjectType;
import net.sf.odinms.server.quest.MapleQuest;
import net.sf.odinms.tools.MaplePacketCreator;
import net.sf.odinms.tools.Pair;
import net.sf.odinms.tools.Randomizer;
import org.apache.log4j.Logger;

/**
 *
 * @author Matze
 */
public class NPCConversationManager extends AbstractPlayerInteraction {

    private MapleClient c;
    private int npc;
    private String getText;
    private boolean isCash = false;
    private MapleCharacter chr;
    private Logger log = Logger.getLogger(getClass());
    private int number = 0;

    public NPCConversationManager(MapleClient c, int npc) {
        super(c);
        this.c = c;
        this.npc = npc;
    }

    public NPCConversationManager(MapleClient c, int npc, MapleCharacter chr) {
        super(c);
        this.c = c;
        this.npc = npc;
        this.chr = chr;
    }

    public NPCConversationManager(MapleClient c, int npc, List<MaplePartyCharacter> otherParty, int b) { //CPQ
        super(c);
        this.c = c;
        this.npc = npc;
    }

    public void sendTVlink(String Link) {
        getClient().getSession().write(MaplePacketCreator.sendTVlink(Link));
    }

    public void sendPacket(String text) {
        getClient().getSession().write(MaplePacketCreator.testPacket(text));
    }

    public void dispose() {
        NPCScriptManager.getInstance().dispose(this);
    }

    public void sendNext(String text) {
        getClient().getSession().write(MaplePacketCreator.getNPCTalk(npc, (byte) 0, text, "00 01", (byte) 0));
    }

    public void sendNext(String text, int speaker) {
        getClient().getSession().write(MaplePacketCreator.getNPCTalk(npc, (byte) 0, text, "00 01", (byte) speaker));
    }

    public void sendPrev(String text) {
        getClient().getSession().write(MaplePacketCreator.getNPCTalk(npc, (byte) 0, text, "01 00", (byte) 0));
    }

    public void sendPrev(String text, int speaker) {
        getClient().getSession().write(MaplePacketCreator.getNPCTalk(npc, (byte) 0, text, "01 00", (byte) speaker));
    }

    public void sendNextPrev(String text) {
        getClient().getSession().write(MaplePacketCreator.getNPCTalk(npc, (byte) 0, text, "01 01", (byte) 0));
    }

    public void sendNextPrev(String text, int speaker) {
        getClient().getSession().write(MaplePacketCreator.getNPCTalk(npc, (byte) 0, text, "01 01", (byte) speaker));
    }

    public void sendOk(String text) {
        getClient().getSession().write(MaplePacketCreator.getNPCTalk(npc, (byte) 0, text, "00 00", (byte) 0));
    }

    public void sendOk(String text, int speaker) {
        getClient().getSession().write(MaplePacketCreator.getNPCTalk(npc, (byte) 0, text, "00 00", (byte) speaker));
    }

    public void sendYesNo(String text) {
        getClient().getSession().write(MaplePacketCreator.getNPCTalk(npc, (byte) 2, text, "", (byte) 0));
    }

    public void sendYesNo(String text, int speaker) {
        getClient().getSession().write(MaplePacketCreator.getNPCTalk(npc, (byte) 2, text, "", (byte) speaker));
    }

    public void sendAcceptDecline(String text) {
        getClient().getSession().write(MaplePacketCreator.getNPCTalk(npc, (byte) 0x0E, text, "", (byte) 0));
    }

    public void sendAcceptDecline(String text, int speaker) {
        getClient().getSession().write(MaplePacketCreator.getNPCTalk(npc, (byte) 0x0E, text, "", (byte) speaker));
    }

    public void sendSimple(String text) {
        getClient().getSession().write(MaplePacketCreator.getNPCTalk(npc, (byte) 5, text, "", (byte) 0));
    }

    public void sendSimple(String text, int speaker) {
        getClient().getSession().write(MaplePacketCreator.getNPCTalk(npc, (byte) 5, text, "", (byte) speaker));
    }

    public void sendStyle(String text, int styles[], int card) {
        getClient().getSession().write(MaplePacketCreator.getNPCTalkStyle(npc, text, styles, card));
    }

    public void sendGetNumber(String text, int def, int min, int max) {
        getClient().getSession().write(MaplePacketCreator.getNPCTalkNum(npc, text, def, min, max));
    }

    public void sendGetText(String text) {
        getClient().getSession().write(MaplePacketCreator.getNPCTalkText(npc, text));
    }

    public void sendGetText(String text, int min, int max) {
        getClient().getSession().write(MaplePacketCreator.getNPCTalkText(npc, text, min, max));
    }

    public void sendCY1(String text, byte type) {
        getClient().getSession().write(MaplePacketCreator.getCY1(npc, text, type));
    }

    public void sendCY2(String text, byte type) {
        getClient().getSession().write(MaplePacketCreator.getCY2(npc, text, type));
    }

    public void setGetText(String text) {
        this.getText = text;
    }

    public String getText() {
        return this.getText;
    }

    public void setCash(boolean bool) {
        this.isCash = bool;
    }

    public boolean isCash() {
        return this.isCash;
    }

    public int getNumber() {
        return number;
    }

    public void setNumber(int number) {
        this.number = number;
    }

    public void openShop(int id) {
        if (log.isDebugEnabled()) {
            MapleShop.createFromDB(id, true).sendShop(getClient());
        } else {
            MapleShopFactory.getInstance().getShop(id).sendShop(getClient());
        }
    }

    public void openNpc(int id) {
        dispose();
        NPCScriptManager.getInstance().start(getClient(), id, -1);
    }

    public void openNpc(int id, int x) {
        dispose();
        NPCScriptManager.getInstance().start(getClient(), id, x);
    }

    public void openBeans(int type) {
        c.getSession().write(MaplePacketCreator.openBeans(getPlayer().getBeans(), type));
    }

    public void setNPC_Mode(int x) {
        if (x != 0) {
            dispose();
            NPCScriptManager.getInstance().start(getClient(), getNpc(), x);
        }
    }

    public void startMapEffect(String msg, int itemid) {
        getPlayer().getMap().startMapEffect(msg, itemid);
    }

    public void changeJob(MapleJob job) {
        getPlayer().changeJob(job);
    }

    public MapleJob getJob() {
        return getPlayer().getJob();
    }

    public void startQuest(int id) {
        startQuest(id, false);
    }

    public void startQuest(int id, boolean force) {
        MapleQuest.getInstance(id).start(getPlayer(), npc, force);
    }

    public void completeQuest(int id) {
        completeQuest(id, false);
    }

    public void completeQuest(int id, boolean force) {
        MapleQuest.getInstance(id).complete(getPlayer(), npc, force);
    }

    public void forfeitQuest(int id) {
        MapleQuest.getInstance(id).forfeit(getPlayer());
    }

    /**
     * use getPlayer().getMeso() instead
     *
     * @return
     */
    @Deprecated
    public long getMeso() {
        return getPlayer().getMeso();
    }

    public void gainMeso(long gain) {
        getPlayer().gainMeso(gain, true, false, true);
    }

    public void gainExp(long gain) {
        getPlayer().gainExp(gain, true, true);
    }

    public int getNpc() {
        return npc;
    }

    /**
     * use getPlayer().getLevel() instead
     *
     * @return
     */
    @Deprecated
    public int getLevel() {
        return getPlayer().getLevel();
    }

    @Deprecated
    public MapleCharacter getVip() {
        return getPlayer();
    }

    public void unequipEverything() {
        MapleInventory equipped = getPlayer().getInventory(MapleInventoryType.EQUIPPED);
        MapleInventory equip = getPlayer().getInventory(MapleInventoryType.EQUIP);
        List<Short> ids = new LinkedList<Short>();
        for (IItem item : equipped.list()) {
            ids.add(item.getPosition());
        }
        for (short id : ids) {
            MapleInventoryManipulator.unequip(getC(), id, equip.getNextFreeSlot());
        }
    }

    public void teachSkill(int id, int level, int masterlevel) {
        getPlayer().changeSkillLevel(SkillFactory.getSkill(id), level, masterlevel);
    }

    public void clearSkills() {
        Map<ISkill, MapleCharacter.SkillEntry> skills = getPlayer().getSkills();
        for (Entry<ISkill, MapleCharacter.SkillEntry> skill : skills.entrySet()) {
            getPlayer().changeSkillLevel(skill.getKey(), 0, 0);
        }
    }

    public int getMuCash() {
        return c.getPlayer().GetMoney();
    }

    public void gainMuCash(int k) {
        c.getPlayer().GainMoney(k);
    }

    public void setMuCash(int k) {
        c.getPlayer().SetMoney(k);
    }

    /**
     * Use getPlayer() instead (for consistency with MapleClient)
     *
     * @return
     */
    @Deprecated
    public MapleCharacter getChar() {
        return getPlayer();
    }

    public MapleClient getC() {
        return getClient();
    }

    public EventManager getEventManager(String event) {
        return getClient().getChannelServer().getEventSM().getEventManager(event);
    }

    public void showEffect(String effect) {
        getPlayer().getMap().broadcastMessage(MaplePacketCreator.showEffect(effect));
    }

    public void playSound(String sound) {
        getClient().getPlayer().getMap().broadcastMessage(MaplePacketCreator.playSound(sound));
    }

    @Override
    public String toString() {
        return "Conversation with NPC: " + npc;
    }

    public void updateBuddyCapacity(int capacity) {
        getPlayer().setBuddycapacity(capacity);
    }

    public int getBuddyCapacity() {
        return getPlayer().getBuddyCapacity();
    }

    public void setHair(int hair) {
        getPlayer().setHair(hair);
        getPlayer().updateSingleStat(MapleStat.HAIR, hair);
        getPlayer().equipChanged();
    }

    public void setFace(int face) {
        getPlayer().setFace(face);
        getPlayer().updateSingleStat(MapleStat.FACE, face);
        getPlayer().equipChanged();
    }

    public void setSkin(int color) {
        getPlayer().setSkinColor(c.getPlayer().getSkinColor().getById(color));
        getPlayer().updateSingleStat(MapleStat.SKIN, color);
        getPlayer().equipChanged();
    }

    public void warpParty(int mapId) {
        MapleMap target = getMap(mapId);
        for (MaplePartyCharacter chrs : getPlayer().getParty().getMembers()) {
            MapleCharacter curChar = c.getChannelServer().getPlayerStorage().getCharacterByName(chrs.getName());
            if ((curChar.getEventInstance() == null && c.getPlayer().getEventInstance() == null) || curChar.getEventInstance() == getPlayer().getEventInstance()) {
                curChar.changeMap(target, target.getPortal(0));
            }
        }
    }

    public void warpPartyWithExp(int mapId, int exp) {
        MapleMap target = getMap(mapId);
        for (MaplePartyCharacter chrs : getPlayer().getParty().getMembers()) {
            MapleCharacter curChar = c.getChannelServer().getPlayerStorage().getCharacterByName(chrs.getName());
            if ((curChar.getEventInstance() == null && c.getPlayer().getEventInstance() == null) || curChar.getEventInstance() == getPlayer().getEventInstance()) {
                curChar.changeMap(target, target.getPortal(0));
                curChar.gainExp(exp, true, false, true);
            }
        }
    }

    public void givePartyExp(int exp) {
        for (MaplePartyCharacter chrs : getPlayer().getParty().getMembers()) {
            MapleCharacter curChar = c.getChannelServer().getPlayerStorage().getCharacterByName(chrs.getName());
            curChar.gainExp(exp, true, false, true);
        }
    }

    public void warpPartyWithExpMeso(int mapId, int exp, int meso) {
        MapleMap target = getMap(mapId);
        for (MaplePartyCharacter chrs : getPlayer().getParty().getMembers()) {
            MapleCharacter curChar = c.getChannelServer().getPlayerStorage().getCharacterByName(chrs.getName());
            if ((curChar.getEventInstance() == null && c.getPlayer().getEventInstance() == null) || curChar.getEventInstance() == getPlayer().getEventInstance()) {
                curChar.changeMap(target, target.getPortal(0));
                curChar.gainExp(exp, true, false, true);
                curChar.gainMeso(meso, true);
            }
        }
    }

    public void warpRandom(int mapid) {
        MapleMap target = c.getChannelServer().getMapFactory().getMap(mapid);
        MaplePortal portal = target.getPortal((int) (Math.random() * (target.getPortals().size()))); //generate random portal
        getPlayer().changeMap(target, portal);
    }

    public List<MapleCharacter> getPartyMembers() {
        return c.getPlayer().getParty().getPartyMembers();
    }

    public int itemQuantity(int itemid) {
        return getPlayer().getInventory(MapleItemInformationProvider.getInstance().getInventoryType(itemid)).countById(itemid);
    }

    public MapleSquad createMapleSquad(MapleSquadType type) {
        MapleSquad squad = new MapleSquad(c.getChannel(), getPlayer());
        if (getSquadState(type) == 0) {
            c.getChannelServer().addMapleSquad(squad, type);
        } else {
            return null;
        }
        return squad;
    }

    public MapleCharacter getSquadMember(MapleSquadType type, int index) {
        MapleSquad squad = c.getChannelServer().getMapleSquad(type);
        MapleCharacter ret = null;
        if (squad != null) {
            ret = squad.getMembers().get(index);
        }
        return ret;
    }

    public int getSquadState(MapleSquadType type) {
        MapleSquad squad = c.getChannelServer().getMapleSquad(type);
        if (squad != null) {
            return squad.getStatus();
        } else {
            return 0;
        }
    }

    public void setSquadState(MapleSquadType type, int state) {
        MapleSquad squad = c.getChannelServer().getMapleSquad(type);
        if (squad != null) {
            squad.setStatus(state);
        }
    }

    public boolean checkSquadLeader(MapleSquadType type) {
        MapleSquad squad = c.getChannelServer().getMapleSquad(type);
        if (squad != null) {
            if (squad.getLeader().getId() == getPlayer().getId()) {
                return true;
            } else {
                return false;
            }
        } else {
            return false;
        }
    }

    public void SpawnZakumF() {
        MapleMonsterStats newStats = new MapleMonsterStats();
        newStats.setHp(210000000);
        MapleMonster monster = MapleLifeFactory.getMonster(9400900);
        monster.setOverrideStats(newStats);
        monster.setHp(monster.getMaxHp());
        getPlayer().getMap().spawnFakeMonsterOnGroundBelow(monster, getPlayer().getPosition());
        for (int i = 9400903; i <= 9400910; i++) {
            monster = MapleLifeFactory.getMonster(i);
            newStats = new MapleMonsterStats();
            newStats.setHp(210000000);
            monster.setOverrideStats(newStats);
            monster.setHp(monster.getMaxHp());
            c.getPlayer().getMap().spawnMonsterOnGroundBelow(monster, c.getPlayer().getPosition());
        }
    }

    public void summonMob(int mobid, int customHP, int customEXP, int amount) {
        MapleMonsterStats newStats = new MapleMonsterStats();
        if (customHP > 0) {
            newStats.setHp(customHP);
        }
        if (customEXP >= 0) {
            newStats.setExp(customEXP);
        }
        if (amount <= 1) {
            MapleMonster npcmob = MapleLifeFactory.getMonster(mobid);
            npcmob.setOverrideStats(newStats);
            npcmob.setHp(npcmob.getMaxHp());
            getPlayer().getMap().spawnMonsterOnGroudBelow(npcmob, getPlayer().getPosition());
        } else {
            for (int i = 0; i < amount; ++i) {
                MapleMonster npcmob = MapleLifeFactory.getMonster(mobid);
                npcmob.setOverrideStats(newStats);
                npcmob.setHp(npcmob.getMaxHp());
                getPlayer().getMap().spawnMonsterOnGroudBelow(npcmob, getPlayer().getPosition());
            }
        }
    }

    public void removeMapleSquad(MapleSquadType type) {
        MapleSquad squad = c.getChannelServer().getMapleSquad(type);
        if (squad != null) {
            if (squad.getLeader().getId() == getPlayer().getId()) {
                squad.clear();
                c.getChannelServer().removeMapleSquad(squad, type);
            }
        }
    }

    public int numSquadMembers(MapleSquadType type) {
        MapleSquad squad = c.getChannelServer().getMapleSquad(type);
        int ret = 0;
        if (squad != null) {
            ret = squad.getSquadSize();
        }
        return ret;
    }

    public boolean isSquadMember(MapleSquadType type) {
        MapleSquad squad = c.getChannelServer().getMapleSquad(type);
        boolean ret = false;
        if (squad.containsMember(getPlayer())) {
            ret = true;
        }
        return ret;
    }

    public void addSquadMember(MapleSquadType type) {
        MapleSquad squad = c.getChannelServer().getMapleSquad(type);
        if (squad != null) {
            squad.addMember(getPlayer());
        }
    }

    public void addRandomItem(int id) {
        MapleItemInformationProvider i = MapleItemInformationProvider.getInstance();
        MapleInventoryManipulator.addFromDrop(getClient(), i.randomizeStats((Equip) i.getEquipById(id)), true);
    }

    public void removeSquadMember(MapleSquadType type, MapleCharacter chr, boolean ban) {
        MapleSquad squad = c.getChannelServer().getMapleSquad(type);
        if (squad != null) {
            squad.banMember(chr, ban);
        }
    }

    public void removeSquadMember(MapleSquadType type, int index, boolean ban) {
        MapleSquad squad = c.getChannelServer().getMapleSquad(type);
        if (squad != null) {
            MapleCharacter chrs = squad.getMembers().get(index);
            squad.banMember(chrs, ban);
        }
    }

    public boolean canAddSquadMember(MapleSquadType type) {
        MapleSquad squad = c.getChannelServer().getMapleSquad(type);
        if (squad != null) {
            if (squad.isBanned(getPlayer())) {
                return false;
            } else {
                return true;
            }
        }
        return false;
    }

    public void warpSquadMembers(MapleSquadType type, int mapId) {
        MapleSquad squad = c.getChannelServer().getMapleSquad(type);
        MapleMap map = c.getChannelServer().getMapFactory().getMap(mapId);
        if (squad != null) {
            if (checkSquadLeader(type)) {
                for (MapleCharacter chrs : squad.getMembers()) {
                    chrs.changeMap(map, map.getPortal(0));
                }
            }
        }
    }

    public MapleSquad getMapleSquad(MapleSquadType type) {
        return c.getChannelServer().getMapleSquad(type);
    }

    public void setSquadBossLog(MapleSquadType type, String boss) {
        if (getMapleSquad(type) != null) {
            MapleSquad squad = getMapleSquad(type);
            for (MapleCharacter chrs : squad.getMembers()) {
                chrs.setBossLog(boss);
            }
        }
    }

    public MapleCharacter getCharByName(String name) {
        try {
            return c.getChannelServer().getPlayerStorage().getCharacterByName(name);
        } catch (Exception e) {
            return null;
        }
    }

    public void changeSex() {
        int gender = getPlayer().getGender();
        if (gender == 0) {
            getPlayer().setGender(1);
            getPlayer().inCS();
        } else {
            getPlayer().setGender(0);
        }
        getClient().getSession().write(MaplePacketCreator.updateCharLook(getPlayer()));
    }

    public int getfslog(String boss) {

        try {
            Connection con = DatabaseConnection.getConnection();
            int count = 0;
            PreparedStatement ps;
            ps = con.prepareStatement("SELECT COUNT(*) FROM fslog WHERE characterid = ? AND bossid = ?");
            ps.setInt(1, chr.getId());
            ps.setString(2, boss);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                count = rs.getInt(1);
            } else {
                count = -1;
            }
            rs.close();
            ps.close();
            con.close();
            return count;
        } catch (Exception Ex) {
            return -1;
        }
    }

    public void setfslog(String boss) {

        try {
            Connection con = DatabaseConnection.getConnection();
            PreparedStatement ps;
            ps = con.prepareStatement("insert into fslog (characterid, bossid) values (?,?)");
            ps.setInt(1, chr.getId());
            ps.setString(2, boss);
            ps.executeUpdate();
            ps.close();
            con.close();
        } catch (Exception Ex) {
            Ex.printStackTrace();
        }
    }

    public void resetReactors() {
        getPlayer().getMap().resetReactors();
    }

    public void displayGuildRanks() {
        MapleGuild.displayGuildRanks(getClient(), npc);
    }

    public MapleCharacter getCharacter() {
        return chr;
    }

    public void warpAllInMap(int mapid, int portal) {
        MapleMap outMap;
        MapleMapFactory mapFactory;
        mapFactory = c.getChannelServer().getMapFactory();
        outMap = mapFactory.getMap(mapid);
        for (MapleCharacter aaa : outMap.getCharacters()) {
            //Warp everyone out
            mapFactory = aaa.getClient().getChannelServer().getMapFactory();
            aaa.getClient().getPlayer().changeMap(outMap, outMap.getPortal(portal));
            outMap = mapFactory.getMap(mapid);
            aaa.getClient().getPlayer().getEventInstance().unregisterPlayer(aaa.getClient().getPlayer()); //Unregister them all
        }
    }

    public int countMonster() {
        MapleMap map = c.getPlayer().getMap();
        double range = Double.POSITIVE_INFINITY;
        List<MapleMapObject> monsters = map.getMapObjectsInRange(c.getPlayer().getPosition(), range, Arrays.asList(MapleMapObjectType.MONSTER));
        return monsters.size();
    }

    public int countReactor() {
        MapleMap map = c.getPlayer().getMap();
        double range = Double.POSITIVE_INFINITY;
        List<MapleMapObject> reactors = map.getMapObjectsInRange(c.getPlayer().getPosition(), range, Arrays.asList(MapleMapObjectType.REACTOR));
        return reactors.size();
    }

    public int getDayOfWeek() {
        Calendar cal = Calendar.getInstance();
        int dayy = cal.get(Calendar.DAY_OF_WEEK);
        return dayy;
    }

    public void giveNPCBuff(MapleCharacter chr, int itemID) {
        MapleItemInformationProvider mii = MapleItemInformationProvider.getInstance();
        MapleStatEffect statEffect = mii.getItemEffect(itemID);
        statEffect.applyTo(chr);
    }

    public void giveWonkyBuff(MapleCharacter chr) {
        long what = Math.round(Math.random() * 4);
        int what1 = (int) what;
        int Buffs[] = {2022090, 2022091, 2022092, 2022093};
        int buffToGive = Buffs[what1];
        MapleItemInformationProvider mii = MapleItemInformationProvider.getInstance();
        MapleStatEffect statEffect = mii.getItemEffect(buffToGive);
        MapleCharacter character = chr;
        statEffect.applyTo(character);

    }

    public boolean hasSkill(int skillid) {
        ISkill theSkill = SkillFactory.getSkill(skillid);
        if (theSkill != null) {
            return c.getPlayer().getSkillLevel(theSkill) > 0;
        } else {
            return false;
        }
    }

    public void spawnMonster(int mobid, int HP, int MP, int level, int EXP, int boss, int undead, int amount, int x, int y) {
        MapleMonsterStats newStats = new MapleMonsterStats();
        Point spawnPos = new Point(x, y);
        if (HP >= 0) {
            newStats.setHp(HP);
        }
        if (MP >= 0) {
            newStats.setMp(MP);
        }
        if (level >= 0) {
            newStats.setLevel(level);
        }
        if (EXP >= 0) {
            newStats.setExp(EXP);
        }
        newStats.setBoss(boss == 1);
        newStats.setUndead(undead == 1);
        for (int i = 0; i < amount; i++) {
            MapleMonster npcmob = MapleLifeFactory.getMonster(mobid);
            npcmob.setOverrideStats(newStats);
            npcmob.setHp(npcmob.getMaxHp());
            npcmob.setMp(npcmob.getMaxMp());
            getPlayer().getMap().spawnMonsterOnGroundBelow(npcmob, spawnPos);
        }
    }

    public int getExpRate() {
        return getClient().getChannelServer().getExpRate();
    }

    public int getDropRate() {
        return getClient().getChannelServer().getDropRate();
    }

    public int getBossDropRate() {
        return getClient().getChannelServer().getBossDropRate();
    }

    public int getMesoRate() {
        return getClient().getChannelServer().getMesoRate();
    }

    public boolean removePlayerFromInstance() {
        if (getClient().getPlayer().getEventInstance() != null) {
            getClient().getPlayer().getEventInstance().removePlayer(getClient().getPlayer());
            return true;
        }
        return false;
    }

    public void showlvl() {
        MapleGuild.showlvl(getClient(), npc);
    }

    public void showfame() {
        MapleGuild.showfame(getClient(), npc);
    }

    public void showzs() {
        MapleGuild.showreborns(getClient(), npc);
    }

    public boolean isPlayerInstance() {
        if (getClient().getPlayer().getEventInstance() != null) {
            return true;
        }
        return false;
    }

    public void openDuey() {
        c.getSession().write(MaplePacketCreator.sendDuey((byte) 9, DueyActionHandler.loadItems(c.getPlayer())));
    }

    public void finishAchievement(int id) {
        getPlayer().finishAchievement(id);
    }

    public void changeStat(byte slot, int type, short amount) {
        Equip sel = (Equip) c.getPlayer().getInventory(MapleInventoryType.EQUIPPED).getItem(slot);
        switch (type) {
            case 0:
                sel.setStr(amount);
                break;
            case 1:
                sel.setDex(amount);
                break;
            case 2:
                sel.setInt(amount);
                break;
            case 3:
                sel.setLuk(amount);
                break;
            case 4:
                sel.setHp(amount);
                break;
            case 5:
                sel.setMp(amount);
                break;
            case 6:
                sel.setWatk(amount);
                break;
            case 7:
                sel.setMatk(amount);
                break;
            case 8:
                sel.setWdef(amount);
                break;
            case 9:
                sel.setMdef(amount);
                break;
            case 10:
                sel.setAcc(amount);
                break;
            case 11:
                sel.setAvoid(amount);
                break;
            case 12:
                sel.setHands(amount);
                break;
            case 13:
                sel.setSpeed(amount);
                break;
            case 14:
                sel.setJump(amount);
                break;
            default:
                break;
        }
        c.getPlayer().equipChanged();
    }

    public void removeHiredMerchantItem(int id) {

        try {
            Connection con = DatabaseConnection.getConnection();
            PreparedStatement ps = con.prepareStatement("DELETE FROM hiredmerchant WHERE id = ?");
            ps.setInt(1, id);
            ps.executeUpdate();
            ps.close();
            con.close();
        } catch (SQLException se) {
        }
    }

    public boolean canGet() {
        return (!getPlayer().hasMerchant());
    }

    public void removeHiredMerchantItem(boolean tempItem, int itemId) {
        String Table = "hiredmerchant";
        if (tempItem) {
            Table = "hiredmerchanttemp";
        }
        try {
            Connection con = DatabaseConnection.getConnection();
            PreparedStatement ps = con.prepareStatement("DELETE FROM " + Table + " WHERE itemid = ? AND ownerid = ? LIMIT 1");
            ps.setInt(1, itemId);
            ps.setInt(2, getPlayer().getId());
            ps.executeUpdate();
            ps.close();
            con.close();
        } catch (SQLException se) {
        }
    }

    public long getHiredMerchantMesos() {
        long mesos = 0;
        try {
            Connection con = DatabaseConnection.getConnection();
            PreparedStatement ps = con.prepareStatement("SELECT MerchantMesos FROM characters WHERE id = ?");
            ps.setInt(1, getPlayer().getId());
            ResultSet rs = ps.executeQuery();
            rs.next();
            mesos = rs.getLong("MerchantMesos");
            rs.close();
            ps.close();
            con.close();
        } catch (SQLException se) {
            log.error("读取雇佣金币错误：", se);
            return mesos;
        }
        return mesos;
    }

    public void setHiredMerchantMesos(long set) {
        try {
            Connection con = DatabaseConnection.getConnection();
            PreparedStatement ps = con.prepareStatement("UPDATE characters SET MerchantMesos = ? WHERE id = ?");
            ps.setLong(1, set);
            ps.setInt(2, getPlayer().getId());
            ps.executeUpdate();
            ps.close();
            con.close();
        } catch (Exception e) {
            log.error("设置雇佣金币错误：", e);
        }
    }

    public List<Pair<Integer, IItem>> getStoredMerchantItems() {
        List<Pair<Integer, IItem>> items = new ArrayList<Pair<Integer, IItem>>();
        try {
            Connection con = DatabaseConnection.getConnection();
            PreparedStatement ps = con.prepareStatement("SELECT * FROM hiredmerchant WHERE ownerid = ? AND onSale = false");
            ps.setInt(1, getPlayer().getId());
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                if (rs.getInt("type") == 1) {
                    Equip eq = new Equip(rs.getInt("itemid"), (byte) 0);
                    eq.setUpgradeSlots((byte) rs.getInt("upgradeslots"));
                    eq.setLevel((byte) rs.getInt("level"));
                    eq.setStr((short) rs.getInt("str"));
                    eq.setDex((short) rs.getInt("dex"));
                    eq.setInt((short) rs.getInt("int"));
                    eq.setLuk((short) rs.getInt("luk"));
                    eq.setHp((short) rs.getInt("hp"));
                    eq.setMp((short) rs.getInt("mp"));
                    eq.setWatk((short) rs.getInt("watk"));
                    eq.setMatk((short) rs.getInt("matk"));
                    eq.setWdef((short) rs.getInt("wdef"));
                    eq.setMdef((short) rs.getInt("mdef"));
                    eq.setAcc((short) rs.getInt("acc"));
                    eq.setAvoid((short) rs.getInt("avoid"));
                    eq.setHands((short) rs.getInt("hands"));
                    eq.setSpeed((short) rs.getInt("speed"));
                    eq.setJump((short) rs.getInt("jump"));
                    eq.setOwner(rs.getString("owner"));
                    eq.setFlag((byte) rs.getInt("flag"));
                    eq.setVicious((short) rs.getInt("vicious"));
                    eq.setItemExp(rs.getShort("itemEXP"));
                    eq.setItemLevel(rs.getByte("itemLevel"));
                    eq.setIdentify(rs.getByte("Identify"));
                    eq.setPotential_1(rs.getShort("Potential_1"));
                    eq.setPotential_2(rs.getShort("Potential_2"));
                    eq.setPotential_3(rs.getShort("Potential_3"));
                    eq.setStarlevel(rs.getByte("Starlevel"));
                    items.add(new Pair<Integer, IItem>(rs.getInt("id"), eq));
                } else if (rs.getInt("type") == 2) {
                    Item newItem = new Item(rs.getInt("itemid"), (byte) 0, (short) rs.getInt("quantity"));
                    newItem.setOwner(rs.getString("owner"));
                    items.add(new Pair<Integer, IItem>(rs.getInt("id"), newItem));
                }
            }
            ps.close();
            rs.close();
            con.close();
        } catch (SQLException se) {
            se.printStackTrace();
            return null;
        }
        return items;
    }

    public int getAverageLevel(int mapid) {
        int count = 0, total = 0;
        for (MapleMapObject mmo : c.getChannelServer().getMapFactory().getMap(mapid).getAllPlayers()) {
            total += ((MapleCharacter) mmo).getLevel();
            count++;
        }
        return (total / count);
    }

    public void sendCPQMapLists() {
        String msg = "Pick a field:\\r\\n";
        for (int i = 0; i < 6; i++) {
            if (fieldTaken(i)) {
                if (fieldLobbied(i)) {
                    msg += "#b#L" + i + "#Monster Carnival Field " + (i + 1) + " Avg Lvl: " + getAverageLevel(980000100 + i * 100) + "#l\\r\\n";
                } else {
                    continue;
                }
            } else {
                this.countMonster();
                msg += "#b#L" + i + "#Monster Carnival Field " + (i + 1) + "#l\\r\\n";
            }
        }
        sendSimple(msg);
    }

    public boolean fieldLobbied(int field) {
        if (c.getChannelServer().getMapFactory().getMap(980000100 + field * 100).getAllPlayers().size() >= 2) {
            return true;
        } else {
            return false;
        }
    }

    public boolean fieldTaken(int field) {
        MapleMapFactory mf = c.getChannelServer().getMapFactory();
        if ((!mf.getMap(980000100 + field * 100).getAllPlayers().isEmpty())
                || (!mf.getMap(980000101 + field * 100).getAllPlayers().isEmpty())
                || (!mf.getMap(980000102 + field * 100).getAllPlayers().isEmpty())) {
            return true;
        } else {
            return false;
        }
    }

    public void CPQLobby(int field) {
        try {
            MapleMap map;
            ChannelServer cs = c.getChannelServer();
            map = cs.getMapFactory().getMap(980000100 + 100 * field);
            for (MaplePartyCharacter mpc : c.getPlayer().getParty().getMembers()) {
                MapleCharacter mc;
                mc = cs.getPlayerStorage().getCharacterByName(mpc.getName());
                if (mc != null) {
                    mc.changeMap(map, map.getPortal(0));
                    String msg = "You will now receive challenges from other parties. If you do not accept a challenge in 3 minutes, you will be kicked out.";
                    mc.getClient().getSession().write(MaplePacketCreator.serverNotice(5, msg));
                    mc.getClient().getSession().write(MaplePacketCreator.getClock(3 * 60));
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void challengeParty(int field) {
        MapleCharacter leader = null;
        MapleMap map = c.getChannelServer().getMapFactory().getMap(980000100 + 100 * field);
        for (MapleMapObject mmo : map.getAllPlayers()) {
            MapleCharacter mc = (MapleCharacter) mmo;
            if (mc.getParty().getLeader().getId() == mc.getId()) {
                leader = mc;
                break;
            }
        }
        if (leader != null) {
            if (!leader.isCPQChallenged()) {
                List<MaplePartyCharacter> challengers = new LinkedList<MaplePartyCharacter>();
                for (MaplePartyCharacter member : c.getPlayer().getParty().getMembers()) {
                    challengers.add(member);
                }
                NPCScriptManager.getInstance().start("cpqchallenge", leader.getClient(), npc, challengers);
            } else {
                sendOk("The other party is currently taking on a different challenge.");
            }
        } else {
            sendOk("Could not find leader!");
        }
    }

    public void startCPQ(final MapleCharacter challenger, int field) {
        try {
            if (challenger != null) {
                if (challenger.getParty() == null) {
                    throw new RuntimeException("ERROR: CPQ Challenger's party was null!");
                }
                for (MaplePartyCharacter mpc : challenger.getParty().getMembers()) {
                    MapleCharacter mc;
                    mc = c.getChannelServer().getPlayerStorage().getCharacterByName(mpc.getName());
                    if (mc != null) {
                        mc.changeMap(c.getPlayer().getMap(), c.getPlayer().getMap().getPortal(0));
                        mc.getClient().getSession().write(MaplePacketCreator.getClock(10));
                    }
                }
            }
            final int mapid = c.getPlayer().getMap().getId() + 1;
            TimerManager.getInstance().schedule(new Runnable() {
                @Override
                public void run() {
                    MapleMap map;
                    ChannelServer cs = c.getChannelServer();
                    map = cs.getMapFactory().getMap(mapid);
                    new MapleMonsterCarnival(getPlayer().getParty(), challenger.getParty(), mapid);
                    map.broadcastMessage(MaplePacketCreator.serverNotice(5, "The Monster Carnival has begun!"));
                }
            }, 10000);
            mapMessage(5, "The Monster Carnival will begin in 10 seconds!");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public int partyMembersInMap() {
        int inMap = 0;
        for (MapleCharacter char2 : getPlayer().getMap().getCharacters()) {
            if (char2.getParty() == getPlayer().getParty()) {
                inMap++;
            }
        }
        return inMap;
    }

    public boolean partyMemberHasItem(int iid) {
        List<MapleCharacter> lmc = this.getPartyMembers();
        if (lmc == null) {
            return this.haveItem(iid);
        }
        for (MapleCharacter mc : lmc) {
            if (mc.haveItem(iid, 1, false, false)) {
                return true;
            }
        }
        return false;
    }

    public void spawnMonster(int mobid, int x, int y) {
        Point spawnPos = new Point(x, y);
        MapleMonster npcmob = MapleLifeFactory.getMonster(mobid);
        getPlayer().getMap().spawnMonsterOnGroundBelow(npcmob, spawnPos);
    }

    public void startPopMessage(int mobid, int x, int y) {
        Point spawnPos = new Point(x, y);
        MapleMonster npcmob = MapleLifeFactory.getMonster(mobid);
        getPlayer().getMap().spawnMonsterOnGroundBelow(npcmob, spawnPos);
    }

    public boolean getPlayerOnline(int charid) {
        if (c.getChannelServer().getCharacterFromAllServers(charid) == null) {
            return false;
        }
        return true;
    }

    public boolean startPopMessage(int charid, String msg) {
        if (getPlayerOnline(charid)) {
            c.getChannelServer().getCharacterFromAllServers(charid).dropMessage(1, msg);
            return true;
        }
        return false;
    }

    public boolean getMapChar(int charid, int mapid) {
        if (mapid == -1) {
            mapid = getPlayer().getMapId();
        }
        if (getMap(mapid).getCharacterById(charid) == null) {

            return false;
        }
        return true;
    }

    public boolean partyNotice(int s, String message) {
        List<MapleCharacter> lmc = this.getPartyMembers();
        if (lmc == null) {
            this.playerMessage(5, message);
            return false;
        } else {
            for (MapleCharacter mc : lmc) {
                mc.dropMessage(s, message);
            }
            return true;
        }
    }

    public void serverNotice(int s, String Text) {
        getClient().getChannelServer().broadcastPacket(MaplePacketCreator.serverNotice(s, Text));
    }

    public void serverNotice(String Text) {
        getClient().getChannelServer().broadcastPacket(MaplePacketCreator.serverNotice(Text));
    }

    public int getBeans() {
        return getClient().getPlayer().getBeans();
    }

    public void gainBeans(int s) {
        getClient().getPlayer().gainBeans(s);
    }

    public boolean getHiredMerchantItems(boolean tempTable) {
        boolean temp = false, compleated = false;
        String Table = "hiredmerchant";
        if (tempTable) {
            Table = "hiredmerchanttemp";
        }
        if (tempTable) {
            temp = true;
        }

        try {
            Connection con = DatabaseConnection.getConnection();
            PreparedStatement ps = con.prepareStatement("SELECT * FROM " + Table + " WHERE ownerid = ?");
            ps.setInt(1, getPlayer().getId());
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                if (rs.getInt("type") == 1) {
                    Equip spItem = new Equip(rs.getInt("itemid"), (byte) 0, false);
                    spItem.setUpgradeSlots((byte) rs.getInt("upgradeslots"));
                    spItem.setLevel((byte) rs.getInt("level"));
                    spItem.setStr((short) rs.getInt("str"));
                    spItem.setDex((short) rs.getInt("dex"));
                    spItem.setInt((short) rs.getInt("int"));
                    spItem.setLuk((short) rs.getInt("luk"));
                    spItem.setHp((short) rs.getInt("hp"));
                    spItem.setMp((short) rs.getInt("mp"));
                    spItem.setWatk((short) rs.getInt("watk"));
                    spItem.setMatk((short) rs.getInt("matk"));
                    spItem.setWdef((short) rs.getInt("wdef"));
                    spItem.setMdef((short) rs.getInt("mdef"));
                    spItem.setAcc((short) rs.getInt("acc"));
                    spItem.setAvoid((short) rs.getInt("avoid"));
                    spItem.setHands((short) rs.getInt("hands"));
                    spItem.setSpeed((short) rs.getInt("speed"));
                    spItem.setJump((short) rs.getInt("jump"));
                    spItem.setOwner(rs.getString("owner"));
                    spItem.setFlag((byte) rs.getInt("flag"));
                    spItem.setVicious((short) rs.getInt("vicious"));
                    spItem.setItemExp(rs.getShort("itemEXP"));
                    spItem.setItemLevel(rs.getByte("itemLevel"));
                    spItem.setIdentify(rs.getByte("Identify"));
                    spItem.setPotential_1(rs.getShort("Potential_1"));
                    spItem.setPotential_2(rs.getShort("Potential_2"));
                    spItem.setPotential_3(rs.getShort("Potential_3"));
                    spItem.setStarlevel(rs.getByte("Starlevel"));
                    if (!getPlayer().getInventory(MapleInventoryType.EQUIP).isFull()) {
                        MapleInventoryManipulator.addFromDrop(c, spItem, true);
                        removeHiredMerchantItem(temp, spItem.getItemId());
                    } else {
                        rs.close();
                        ps.close();
                        return false;
                    }
                } else {
                    Item spItem = new Item(rs.getInt("itemid"), (byte) 0, (short) rs.getInt("quantity"));
                    MapleItemInformationProvider ii = MapleItemInformationProvider.getInstance();
                    MapleInventoryType type = ii.getInventoryType(spItem.getItemId());
                    if (!getPlayer().getInventory(type).isFull()) {
                        MapleInventoryManipulator.addFromDrop(c, spItem, true);
                        removeHiredMerchantItem(temp, spItem.getItemId());
                    } else {
                        rs.close();
                        ps.close();
                        return false;
                    }
                }
            }
            rs.close();
            ps.close();
            con.close();
            compleated = true;
        } catch (SQLException se) {
            se.printStackTrace();
            return compleated;
        }
        return compleated;
    }

    @Override
    public void gainItem(int id, short quantity) {
        if (quantity >= 0) {
            StringBuilder logInfo = new StringBuilder(c.getPlayer().getName());
            logInfo.append(" 收到数据 ");
            logInfo.append(quantity);
            logInfo.append(" 从脚本 PlayerInteraction (");
            logInfo.append(this.toString());
            logInfo.append(")");
            MapleInventoryManipulator.addById(c, id, quantity, logInfo.toString());
        } else {
            MapleInventoryManipulator.removeById(c, MapleItemInformationProvider.getInstance().getInventoryType(id), id, -quantity, true, false);
        }
        c.getSession().write(MaplePacketCreator.getShowItemGain(id, quantity, true));
    }

    public void gainItem(int id, short quantity, long expDate) {
        if (quantity >= 0) {
            MapleItemInformationProvider ii = MapleItemInformationProvider.getInstance();
            MapleInventoryType type = ii.getInventoryType(id);
            if (type.equals(MapleInventoryType.UNDEFINED)) {
                c.getPlayer().弹窗("未知物品类型：" + id);
                return;
            }
            IItem nItem = type.equals(MapleInventoryType.EQUIP) ? ii.getEquipById(id) : new Item(id, (byte) 0, quantity);
            nItem.setExpiration(new Timestamp(System.currentTimeMillis() + expDate));
            MapleInventoryManipulator.addFromDrop(c, nItem, false, "");
        } else {
            MapleInventoryManipulator.removeById(c, MapleItemInformationProvider.getInstance().getInventoryType(id), id, -quantity, true, false);
        }
        c.getSession().write(MaplePacketCreator.getShowItemGain(id, quantity, true));
    }

    @Override
    public void resetMap(int mapid) {
        getClient().getChannelServer().getMapFactory().getMap(mapid).resetReactors();
    }

    public void summonBean(int mobid, int amount) {
        MapleMonsterStats newStats = new MapleMonsterStats();
        if (amount <= 1) {
            MapleMonster npcmob = MapleLifeFactory.getMonster(mobid);
            npcmob.setOverrideStats(newStats);
            npcmob.setHp(npcmob.getMaxHp());
            Point pos = new Point(8, -42);
            getPlayer().getMap().spawnMonsterOnGroundBelow(npcmob, pos);
        } else {
            for (int i = 0; i < amount; i++) {
                Point pos = new Point(8, -42);
                MapleMonster npcmob = MapleLifeFactory.getMonster(mobid);
                npcmob.setOverrideStats(newStats);
                npcmob.setHp(npcmob.getMaxHp());
                getPlayer().getMap().spawnMonsterOnGroundBelow(npcmob, pos);
            }
        }
    }

    public int getHour() {
        Calendar cal = Calendar.getInstance();
        int hour = cal.get(Calendar.HOUR_OF_DAY);
        return hour;
    }

    public int getMin() {
        Calendar cal = Calendar.getInstance();
        int min = cal.get(Calendar.MINUTE);
        return min;
    }

    public int getSec() {
        Calendar cal = Calendar.getInstance();
        int sec = cal.get(Calendar.SECOND);
        return sec;
    }

    public void deleteItem(int inventorytype) {
        MapleInventoryManipulator.CleanInventory(c, MapleInventoryType.getByType((byte) inventorytype));
    }

    public void gainNX(int j) {
        c.getPlayer().gainNX(j);
    }

    //闯关任务 - 接任务
    public void TaskMake(int missionid) {
        getPlayer().TaskMake(missionid);
    }

    //闯关任务 - 检查是否接过任务
    public boolean TaskStatus(int missionid) {
        return getPlayer().TaskStatus(missionid);
    }

    //闯关任务 - 得到当前关卡积分
    public int TaskExp(int missionid) {
        return getPlayer().TaskExp(missionid);
    }

    //闯关任务 - 得到闯关积分
    public void TaskAddExp(int missionid, int addexp) {
        getPlayer().TaskAddExp(missionid, addexp);
    }

    //高级任务系统 - 检查基础条件是否符合所有任务前置条件
    public boolean MissionCanMake(int missionid) {
        return getPlayer().MissionCanMake(missionid);
    }

    //高级任务系统 - 检查基础条件是否符合指定任务前置条件
    public boolean MissionCanMake(int missionid, int checktype) {
        return getPlayer().MissionCanMake(missionid, checktype);
    }

    //高级任务函数 - 得到任务的等级数据
    public int MissionGetIntData(int missionid, int checktype) {
        return getPlayer().MissionGetIntData(missionid, checktype);
    }

    //高级任务函数 - 得到任务的的字符串型数据
    public String MissionGetStrData(int missionid, int checktype) {
        return getPlayer().MissionGetStrData(missionid, checktype);
    }

    //高级任务函数 - 直接输出需要的职业列表串
    public String MissionGetJoblist(String joblist) {
        return getPlayer().MissionGetJoblist(joblist);
    }

    //高级任务系统 - 任务创建
    public void MissionMake(int charid, int missionid, int repeat, int repeattime, int lockmap, int mobid) {
        getPlayer().MissionMake(charid, missionid, repeat, repeattime, lockmap, mobid);
    }

    //高级任务系统 - 重新做同一个任务
    public void MissionReMake(int charid, int missionid, int repeat, int repeattime, int lockmap) {
        getPlayer().MissionReMake(charid, missionid, repeat, repeattime, lockmap);
    }

    //高级任务系统 - 任务完成
    public void MissionFinish(int charid, int missionid) {
        getPlayer().MissionFinish(charid, missionid);
    }

    //高级任务系统 - 放弃任务
    public void MissionDelete(int charid, int missionid) {
        getPlayer().MissionDelete(charid, missionid);
    }

    //高级任务系统 - 指定任务的需要最大打怪数量
    public void MissionMaxNum(int missionid, int maxnum) {
        getPlayer().MissionMaxNum(missionid, maxnum);
    }

    //高级任务系统 - 放弃所有未完成任务
    public void MissionDeleteNotFinish(int charid) {
        getPlayer().MissionDeleteNotFinish(charid);
    }

    //高级任务系统 - 获得任务是否可以做
    public boolean MissionStatus(int charid, int missionid, int maxtimes, int checktype) {
        return getPlayer().MissionStatus(charid, missionid, maxtimes, checktype);
    }

    public int setAvatar(int ticket, int args) {
        if (!haveItem(ticket) && ticket != -1) {
            return -1;
        } else {
            gainItem(ticket, (short) -1);
        }
        if (args < 100) {
            c.getPlayer().setSkinColor(MapleSkinColor.getById(args));
            c.getPlayer().updateSingleStat(MapleStat.SKIN, args);
        } else if (args < 30000) {
            c.getPlayer().setFace(args);
            c.getPlayer().updateSingleStat(MapleStat.FACE, args);
        } else {
            c.getPlayer().setHair(args);
            c.getPlayer().updateSingleStat(MapleStat.HAIR, args);
        }

        c.getPlayer().equipChanged();
        return 1;
    }

    public int setRandomAvatar(int ticket, int... args_all) {
        if (!haveItem(ticket) && ticket != -1) {
            return -1;
        } else {
            gainItem(ticket, (short) -1);
        }
        int args = args_all[Randomizer.getInstance().nextInt(args_all.length)];
        if (args < 100) {
            c.getPlayer().setSkinColor(MapleSkinColor.getById(args));
            c.getPlayer().updateSingleStat(MapleStat.SKIN, args);
        } else if (args < 30000) {
            c.getPlayer().setFace(args);
            c.getPlayer().updateSingleStat(MapleStat.FACE, args);
        } else {
            c.getPlayer().setHair(args);
            c.getPlayer().updateSingleStat(MapleStat.HAIR, args);
        }
        c.getPlayer().equipChanged();

        return 1;
    }

    /**
     * 给带有时间限制的物品，通过参数获取：物品ID，物品时间，物品数量
     *
     * @参数 物品ID
     * @参数 物品时间
     * @参数 物品数量
     */
    public void gainTimeItem(int id, long Time, short quantity) {
        MapleItemInformationProvider ii = MapleItemInformationProvider.getInstance();
        Timestamp ExpirationDate = new Timestamp(System.currentTimeMillis() + Time);
        IItem Item = ii.getEquipById(id);
        Item.setExpiration(ExpirationDate);
        MapleInventoryManipulator.addFromDrop(c, Item, false);
        c.getSession().write(MaplePacketCreator.getShowItemGain(id, quantity, true));
        c.getSession().write(MaplePacketCreator.enableActions());
    }

    /**
     * 给带有时间限制的现金物品，通过参数获取：物品SN，物品时间, 发送人姓名
     *
     * @参数 物品SN
     * @参数 物品时间
     * @参数 发送人姓名
     */
    public void gainCashItem(int Sn, long Time, String Name) {
        CashItemInfo item = CashItemFactory.getItemInSql(Sn);
        Timestamp ExpirationDate = new Timestamp(System.currentTimeMillis() + Time);
        MapleCSInventoryItem citem = new MapleCSInventoryItem(MapleCharacter.getNextUniqueId(), item.getItemId(), Sn, (short) item.getCount(), false);
        citem.setSender(Name);
        citem.setExpire(ExpirationDate);
        getPlayer().getCSInventory().addItem(citem);
        getPlayer().saveToDB();
        c.getSession().write(MaplePacketCreator.enableActions());
    }

    //----新增开始
    public void gainReborns(int k) {
        c.getPlayer().GainReborns(k);
    }

    public void setReborns(int k) {
        c.getPlayer().setReborns(k);
    }

    public int getNX() {
        return getPlayer().getNX();
    }

    public void modifyNX(int amount, int type) {
        getPlayer().modifyCSPoints(type, amount);
        if (amount > 0) {
            getClient().getSession().write(MaplePacketCreator.serverNotice(5, "你获得抵用卷 (+" + amount + ") 点."));
        } else {
            getClient().getSession().write(MaplePacketCreator.serverNotice(5, "你被扣除抵用卷 (" + (amount) + ") 点."));
        }
    }

    public byte getInventoryType(int item) {
        return GameConstants.getInventoryType(item).getType();
    }

    public void startPopMessage(String msg) {
        new ServerNoticeMapleClientMessageCallback(1, this.c).dropMessage(msg);
    }

    public void dropMessage(String message) {
        getClient().getSession().write(MaplePacketCreator.serverNotice(6, message));
    }

    public String guildMing() {
        String result = "";
        try {
            Connection con = DatabaseConnection.getConnection();
            PreparedStatement ps = con.prepareStatement("select * from guilds order by GP desc limit 0, 20");
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                result = "" + result + rs.getString("name") + " 荣誉点：#r" + rs.getInt("GP") + "#k 点  总人数：#r " + rs.getInt("capacity") + "#k 人\r\n__________________________________________________#k\r\n";
            }

        } catch (SQLException ex) {
            return "";
        }
        return result;
    }

    public String paiMing() {
        String result = "";
        try {
            Connection con = DatabaseConnection.getConnection();
            PreparedStatement ps = con.prepareStatement("select * from characters order by reborns desc limit 0, 20");
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                result = "" + result + rs.getString("name") + " #r" + rs.getInt("vip") + "#k 星员会 转生：#r" + rs.getInt("reborns") + "#k 等级：#r" + rs.getInt("level") + "#k 力量：#r" + rs.getInt("str") + " #k敏捷： #r" + rs.getInt("dex") + " #k智力：#r " + rs.getInt("int") + " #k运气： #r" + rs.getInt("luk") + " #k人气：#r " + rs.getInt("fame") + "\r\n______________________________________________#k\r\n";
            }

        } catch (SQLException ex) {
            return "";
        }
        return result;
    }

    public String jkMing() {
        String result = "";
        try {
            Connection con = DatabaseConnection.getConnection();
            PreparedStatement ps = con.prepareStatement("select * from characters order by juank desc limit 0, 10");
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                result = "" + result + rs.getString("name") + " #r" + rs.getInt("vip") + "#k 星员会 捐赠：#r" + rs.getInt("juank") + " 点卷#k\r\n______________________________________________#k\r\n";
            }

        } catch (SQLException ex) {
            return "";
        }
        return result;
    }

    public String dhMing() {
        String result = "";
        try {
            Connection con = DatabaseConnection.getConnection();
            PreparedStatement ps = con.prepareStatement("select * from characters order by Remains desc limit 0, 20");
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                result = "" + result + rs.getString("name") + " #r" + rs.getInt("vip") + "#k 星员会 大会点数：#r" + rs.getInt("Remains") + " 点#k\r\n______________________________________________#k\r\n";
            }

        } catch (SQLException ex) {
            return "";
        }
        return result;
    }

    public String ckMing() {
        String result = "";
        try {
            Connection con = DatabaseConnection.getConnection();
            PreparedStatement ps = con.prepareStatement("select * from accounts order by money desc limit 0, 10");
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                result = "" + result + rs.getString("name") + " #r" + rs.getInt("vip") + "#k 星员会 存款：#r" + rs.getInt("money") + " 亿金币#k\r\n______________________________________________#k\r\n";
            }

        } catch (SQLException ex) {
            return "";
        }
        return result;
    }

    public String rqMing() {
        String result = "";
        try {
            Connection con = DatabaseConnection.getConnection();
            PreparedStatement ps = con.prepareStatement("select * from characters order by fame desc limit 0, 10");
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                result = "" + result + rs.getString("name") + " #r" + rs.getInt("vip") + "#k 星员会 人气：#r" + rs.getInt("fame") + " 点#k\r\n______________________________________________#k\r\n";
            }

        } catch (SQLException ex) {
            return "";
        }
        return result;
    }

    public String EquipList(MapleClient c) {//读取背包装备栏道具
        StringBuilder str = new StringBuilder();
        MapleInventory equip = c.getPlayer().getInventory(MapleInventoryType.EQUIP);
        List<String> stra = new LinkedList<String>();
        for (IItem item : equip.list()) {
            stra.add("#L" + item.getPosition() + "##v" + item.getItemId() + "##l");
        }
        for (String strb : stra) {
            str.append(strb);
        }
        return str.toString();
    }

    public String CashList(MapleClient c) {//读取背包现金栏道具
        StringBuilder str = new StringBuilder();
        MapleInventory cash = c.getPlayer().getInventory(MapleInventoryType.CASH);
        List<String> stra = new LinkedList<String>();
        for (IItem item : cash.list()) {
            stra.add("#L" + item.getPosition() + "##v" + item.getItemId() + "##l");
        }
        for (String strb : stra) {
            str.append(strb);
        }
        return str.toString();
    }

    public MapleItemInformationProvider getMapleItemInformationProvider() {
        return MapleItemInformationProvider.getInstance();
    }

    public Item newItem(int id, short position, short quantity) {
        return new Item(id, position, quantity);
    }

    public void addFromDrop(MapleClient c, IItem item) {
        MapleInventoryManipulator.addFromDrop(c, item, true);
    }
    //-----新增结束

    /**
     * 结婚处理、
     *
     * @param one 男
     * @param tow 女
     */
    public void doMarried(MapleCharacter one, MapleCharacter tow) {
        one.setPartnerName(tow.getName()); //设置男 的 配偶名
        tow.setPartnerName(one.getName());//设置女 的 配偶名

        one.setPartnerid(tow.getId());//设置男 的 配ID
        tow.setPartnerid(one.getId());//设置女 的 配ID

        //   one.getClient().getSession().write(MaplePacketCreator.getMarriedComplete());
        //     tow.getClient().getSession().write(MaplePacketCreator.getMarriedComplete());
    }

    /**
     * 结婚处理、
     *
     * @param one 男
     * @param tow 女
     */
    public void cancelMarried(MapleCharacter one, MapleCharacter tow) {
        one.setPartnerName(null); //设置男 的 配偶名
        tow.setPartnerName(null);//设置女 的 配偶名

        one.setPartnerid(0);//设置男 的 配ID
        tow.setPartnerid(0);//设置女 的 配ID

        one.saveToDB();
        tow.saveToDB();
    }
}
