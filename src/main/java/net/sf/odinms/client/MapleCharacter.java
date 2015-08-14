package net.sf.odinms.client;

import java.awt.Point;
import java.lang.ref.WeakReference;
import java.rmi.RemoteException;
import java.sql.*;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.*;
import java.util.Map.Entry;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import net.sf.odinms.client.anticheat.CheatTracker;
import net.sf.odinms.client.skills.*;
import net.sf.odinms.database.DatabaseConnection;
import net.sf.odinms.database.DatabaseException;
import net.sf.odinms.net.MaplePacket;
import net.sf.odinms.net.channel.ChannelServer;
import net.sf.odinms.net.channel.handler.DueyActionHandler.Actions;
import net.sf.odinms.net.login.LoginServer;
import net.sf.odinms.net.world.*;
import net.sf.odinms.net.world.guild.MapleGuild;
import net.sf.odinms.net.world.guild.MapleGuildCharacter;
import net.sf.odinms.net.world.remote.WorldChannelInterface;
import net.sf.odinms.scripting.ScriptManager;
import net.sf.odinms.scripting.event.EventInstanceManager;
import net.sf.odinms.server.*;
import net.sf.odinms.server.constants.InventoryConstants.Items.Flags;
import net.sf.odinms.server.life.MapleAndroid;
import net.sf.odinms.server.life.MapleMonster;
import net.sf.odinms.server.life.MobSkill;
import net.sf.odinms.server.maps.*;
import net.sf.odinms.server.miniGame.MapleRPSGame;
import net.sf.odinms.server.movement.LifeMovementFragment;
import net.sf.odinms.server.playerinteractions.HiredMerchant;
import net.sf.odinms.server.playerinteractions.IPlayerInteractionManager;
import net.sf.odinms.server.playerinteractions.MaplePlayerShop;
import net.sf.odinms.server.quest.MapleCustomQuest;
import net.sf.odinms.server.quest.MapleQuest;
import net.sf.odinms.tools.Guild.MapleGuild_Msg;
import net.sf.odinms.tools.MaplePacketCreator;
import net.sf.odinms.tools.Pair;
import net.sf.odinms.tools.Randomizer;
import org.apache.log4j.Logger;
import org.hibernate.LockMode;
import org.hibernate.Session;
import org.hibernate.Transaction;

public class MapleCharacter extends AbstractAnimatedMapleMapObject implements
        InventoryContainer {

    public class HiredMerchantInventory implements MapleItemsNameSpace {

        private List<IItem> items = new ArrayList<IItem>();
        private long meso = 0;

        public void addItem(IItem item) {
            items.add(item);
        }

        public void removeItem(IItem item) {
            items.remove(item);
        }

        public Iterator<IItem> getIterator() {
            return items.iterator();
        }

        public long getMeso() {
            return meso;
        }

        public void GainMeso(int m) {
            meso += m;
        }

        public int Size() {
            return items.size();
        }

        @Override
        public MapleItemsNameSpaceType GetSpaceType() {
            return MapleItemsNameSpaceType.HiredMerchant;
        }

        @Override
        public Collection<IItem> AllItems() {
            return Collections.unmodifiableList(items);
        }
    }
    private static final AtomicLong UniqueIdNumber = new AtomicLong(1);
    private static final Logger log = Logger.getLogger(MapleCharacter.class);
    public static final double MAX_VIEW_RANGE_SQ = 850 * 850;
    /**
     * 实例变量
     */
    public ArrayList<Integer> ares_data_infoid = new ArrayList<Integer>();
    private int world;
    private int accountid;
    private int rank;
    private int rankMove;
    private int jobRank;
    private int jobRankMove;
    private int familyId;
    private String name;
    private int level;
    private int vip, reborns, ShuoHua, SD, ZhongJi, fs, jh, Present;
    private int str, dex, luk, int_, cleardamages;
    private AtomicLong exp = new AtomicLong();
    private int hp, maxhp;
    private int mp, maxmp;
    private int mpApUsed, hpApUsed;
    private int hair, face;
    private AtomicLong meso = new AtomicLong();
    private int remainingAp;
    private Integer[] remainingsp = new Integer[]{0, 0, 0, 0, 0, 0, 0, 0, 0,
        0, 0};
    private int savedLocations[];
    private int fame;
    private long lastfametime;
    private List<Integer> lastmonthfameids;
    // local stats represent current stats of the player to avoid expensive
    // operations
    private transient int localmaxhp, localmaxmp;
    private transient int localstr, localdex, localluk, localint_;
    private transient int magic, watk;
    private transient double speedMod, jumpMod;
    private transient int localmaxbasedamage;
    private int id;
    private MapleClient client;
    private MapleMap map;
    private int initialSpawnPoint;
    // mapid is only used when calling getMapId() with map == null, it is not
    // updated when running in channelserver mode
    private int mapid;
    private int cygnusLinkId = 0;
    private MapleShop shop = null;
    private SkillMacro[] skillMacros = new SkillMacro[5];
    private MapleTrade trade = null;
    private MapleSkinColor skinColor = MapleSkinColor.NORMAL;
    private MapleJob job = MapleJob.BEGINNER;
    private int gender;
    private int GMLevel;
    private boolean invincible;
    private boolean hidden = false;
    private boolean canDoor = true;
    private int chair;
    private int itemEffect;
    private int APQScore;
    private MapleParty party;
    private EventInstanceManager eventInstance = null;
    private MapleInventory[] inventory;
    private MapleStorage storage = null;
    private HiredMerchantInventory hiredmerchantinventory = new HiredMerchantInventory();
    private Map<MapleQuest, MapleQuestStatus> quests;
    private Set<MapleMonster> controlled = new LinkedHashSet<MapleMonster>();
    private List<MapleMapObject> visibleMapObjects = new java.util.concurrent.CopyOnWriteArrayList<MapleMapObject>();
    private final Map<ISkill, SkillEntry> skills = new LinkedHashMap<ISkill, SkillEntry>();
    private MaplePlayerBuffManager buffManager;
    // private Map<MapleBuffStat, MapleBuffStatValueHolder> effects = new ConcurrentSkipListMap<MapleBuffStat, MapleBuffStatValueHolder>();
    private Map<Integer, MapleKeyBinding> keymap = new LinkedHashMap<Integer, MapleKeyBinding>();
    private List<MapleDoor> doors = new ArrayList<MapleDoor>();
    private ArrayList<Integer> excluded = new ArrayList<Integer>();
    private BuddyList buddylist;
    private Map<Integer, MapleCoolDownValueHolder> coolDowns = new LinkedHashMap<Integer, MapleCoolDownValueHolder>();
    // anticheat related information
    private CheatTracker anticheat;
    private ScheduledFuture<?> dragonBloodSchedule;
    private ScheduledFuture<?> mapTimeLimitTask = null;
    // guild related information
    private int guildid;
    private int guildrank, allianceRank;
    private MapleGuildCharacter mgc = null;
    // cash shop related information
    private int paypalnx;
    private int money;
    private int maplepoints;
    private int cardnx;
    // misc information
    private List<MapleDisease> diseases = new ArrayList<MapleDisease>();
    private boolean incs;
    private boolean inmts;
    private MapleMessenger messenger = null;
    int messengerposition = 4;
    private int face_adorn = 0;
    private ScheduledFuture<?> hpDecreaseTask;
    // 由于黑骑士的灵魂召唤兽系列已经改为封包控制 所以弃用线程变量
    // private ScheduledFuture<?> beholderHealingSchedule;
    // private ScheduledFuture<?> beholderBuffSchedule;
    private ScheduledFuture<?> BerserkSchedule;
    private MapleCSInventory csinventory;
    public SummonMovementType getMovementType;
    private String chalktext; // Chalkboard
    private int team;
    private int canTalk;
    private int zakumLvl; // zero means they havent started yet
    // marriage
    private int marriageQuestLevel;
    private List<LifeMovementFragment> lastres;
    // enable/disable smegas - player command
    private boolean smegaEnabled = true;
    private long afkTimer = 0;
    private long loggedInTimer = 0;
    private int currentPage = 0, currentType = 0, currentTab = 1;
    // private int energybar = 0;
    ScheduledFuture energyDecrease = null;
    private int hppot = 0;
    private int mppot = 0;
    private int bossPoints;
    private int bossRepeats;
    private long nextBQ = 0;
    private boolean playerNPC;
    private int battleshipHP;
    private MapleMount maplemount;
    private List<Integer> finishedAchievements = new ArrayList<Integer>();
    private boolean banned = false; // Prevent evading GM police with ccing
    private boolean needsParty = false;
    private int needsPartyMinLevel;
    private int needsPartyMaxLevel;
    // CPQ
    private boolean CPQChallenged = false;
    private int CP = 0;
    private int totalCP = 0;
    private MapleMonsterCarnival monsterCarnival;
    private int CPQRanking = 0;
    private int autoHpPot, autoMpPot;
    private Point lastPortalPoint;
    private boolean partyInvite;
    private long lastSave;
    private boolean muted;
    Calendar unmuteTime = null;
    private Map<Long, MapleStatEffect> buffsToCancel = new HashMap<Long, MapleStatEffect>();
    private boolean questDebug = false;
    private List<String> mapletips = new ArrayList<String>();
    // 商店
    private IPlayerInteractionManager interaction = null;
    // 武陵道场
    private int vanquisherStage;
    private int vanquisherKills;
    private int dojoPoints;
    private int lastDojoStage;
    private int dojoEnergy;
    private long dojoFinish;
    private boolean finishedDojoTutorial;
    private Map<Integer, String> entered = new LinkedHashMap<Integer, String>();
    // 宠物
    private MaplePet[] pets = new MaplePet[3];
    // WarningSystem
    private int Warning;
    // 战神Combo
    private int combo = 0;
    private int lastAttack;
    private int clfb;
    public static boolean tutorial = false;
    public ArrayList<String> ares_data = new ArrayList<String>();
    public ArrayList<String> area_data = new ArrayList<String>();
    private List<String> blockedPortals = new ArrayList<String>();
    private int energyPoint = 0;
    private ScheduledFuture<?> energyChargeSchedule;
    private long lasttime = 976867676767L;
    private boolean CanChangeMap = true;
    private int armorTimes = 0; // 祝福护甲防御次数
    // 重构召唤兽
    // private Map<Integer, MapleSummon> summons = new LinkedHashMap<Integer,
    // MapleSummon>();
    private Map<Integer, List<MapleSummon>> summons = new HashMap<Integer, List<MapleSummon>>(); // 更改召唤兽类型
    private MapleSummon mainsumons, ringsumons;
    // 机械传送门
    private List<MapleDoor2> doors2 = new ArrayList<MapleDoor2>();
    // 取消传送门
    // 先出来的门
    private ScheduledFuture<?> door2_1;
    // 后出来的门
    private ScheduledFuture<?> door2_2;
    private int 吞噬的monsteroid = 0;
    // 设置正在使用的骑宠
    private MapleUseMount useMount;
    // 倾向系统
    // 领袖气质 感性 洞察力 意志 手技 魅力
    private int charisma, sense, insight, volition, hands, charm;
    // TDS = today's = 今天的
    private int TDS_charisma, TDS_sense, TDS_insight, TDS_volition, TDS_hands,
            TDS_charm;
    // Pvp
    private int pkLevel, battlePoint, battleExp;
    // 结婚
    private boolean isMarried;
    private String partnerName;
    private int marryid;
    private int partnerid; // 配偶id
    // 打豆豆
    private int beans;
    private int beansNum;
    private int beansRange;
    private boolean canSetBeansNum;
    // 能否使用进阶灵气
    private boolean 能否使用进阶灵气 = true;
    /**
     * 戒指效果定时器。
     */
    private ScheduledFuture<?> ringFuture;
    /**
     * 特定技能HP上限倍率。
     */
    private double skillhprrate = 0.0;
    private int power = 0;//尖兵电力
    private boolean powerOpen = false;
    private long powerTime = System.currentTimeMillis();
    private Lock move_lock = new ReentrantLock(true);
    private Timestamp necklace_expansion;
    private int pvpkills, pvpdeaths;
    private MapleAndroid android;
    private MapleCharAttribute attribute;
    private MapleRPSGame RPSGame;
    private ScriptManager script = new ScriptManager(this);

    static {
        try {
            Connection con = DatabaseConnection.getConnection();
            PreparedStatement ps = con.prepareStatement("SELECT MAX(uniqueid)+1 FROM items");
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                UniqueIdNumber.set(rs.getLong(1));
            }
            rs.close();
            ps.close();
            con.close();
        } catch (Exception e) {
            log.error("init Unid Faild");
        }
    }

    private MapleCharacter() {
        setStance(0);
        inventory = new MapleInventory[MapleInventoryType.values().length];
        for (MapleInventoryType type : MapleInventoryType.values()) {
            inventory[type.ordinal()] = new MapleInventory(type, (byte) 100);
        }

        savedLocations = new int[SavedLocationType.values().length];
        for (int i = 0; i < SavedLocationType.values().length; i++) {
            savedLocations[i] = -1;
        }

        quests = new LinkedHashMap<MapleQuest, MapleQuestStatus>();
        anticheat = new CheatTracker(this);
        setPosition(new Point(0, 0));
        buffManager = new MaplePlayerBuffManager(this);
    }

    public MapleCharacter(MapleClient client, int id) {
        this();
        this.client = client;
        this.id = id;
    }

    public List<MapleItemsNameSpace> GetSaveNameSpaces() {
        return Arrays.asList(getInventory(MapleInventoryType.UNDEFINED),
                getInventory(MapleInventoryType.EQUIPPED),
                getInventory(MapleInventoryType.EQUIP),
                getInventory(MapleInventoryType.USE),
                getInventory(MapleInventoryType.ETC),
                getInventory(MapleInventoryType.SETUP),
                getInventory(MapleInventoryType.CASH), storage, csinventory,
                hiredmerchantinventory);
    }

    public boolean CanChangeMap() {
        return this.CanChangeMap;
    }

    public void setCanChangeMap(boolean can) {
        this.CanChangeMap = can;
    }

    public void dropMessage(String message) {
        client.getSession().write(
                MaplePacketCreator.serverNotice(isGM() ? 6 : 5, message));
    }

    public void dropMessage(int type, String message) {
        client.getSession().write(
                MaplePacketCreator.serverNotice(type, message));
    }

    public void changeMap(MapleMap to) {
        changeMap(to, to.getPortal(0));
    }

    public void changeMap(final MapleMap to, final MaplePortal pto) {
        if (to.getId() == 100000200 || to.getId() == 211000100
                || to.getId() == 220000300) {
            changeMapInternal(to, pto.getPosition(),
                    MaplePacketCreator.getWarpToMap(to.getId(),
                    pto.getId() - 2, this));
        } else {
            changeMapInternal(to, pto.getPosition(),
                    MaplePacketCreator.getWarpToMap(to.getId(), pto.getId(),
                    this));
        }

    }

    public void changeMap(final MapleMap to, final Point pos) {
        changeMapInternal(to, pos,
                MaplePacketCreator.getWarpToMap(to.getId(), 0x80, this));

    }

    public void changeMapBanish(int mapid, String portal, String msg) {
        dropMessage(5, msg);
        MapleMap map_ = client.getChannelServer().getMapFactory().getMap(mapid);
        changeMap(map_, map_.getPortal(portal));
    }

    public MapleCharacter getThis() {
        return this;
    }

    public int getCombo() {
        return this.combo;
    }

    public int setCombo(int combo) {
        return (this.combo = combo);
    }

    public int upCombo(int coun) {
        return (this.combo += coun);
    }

    public IPlayerInteractionManager getInteraction() {
        return interaction;
    }

    public void setInteraction(IPlayerInteractionManager box) {
        interaction = box;
    }

    public void message(String m) {
        dropMessage(5, m);
    }

    public int addDojoPointsByMap() {
        int pts = 0;
        if (dojoPoints < 17000) {
            pts = 1 + ((getMap().getId() - 1) / 100 % 100) / 6;
            if (party != null) {
                int highest = level, lowest = level;
                for (MaplePartyCharacter mpc : party.getMembers()) {
                    if (mpc.getLevel() > highest) {
                        highest = mpc.getLevel();
                    } else if (mpc.getLevel() < lowest) {
                        lowest = mpc.getLevel();
                    }
                }
                pts += (highest - lowest < 30) ? 0 : -pts;
            } else {
                pts++;
            }
            this.dojoPoints += pts;
        }
        return pts;
    }

    public void addExcluded(int x) {
        excluded.add(x);
    }

    public void showMapleTips() {
        for (String s : mapletips) {
            client.getSession().write(MaplePacketCreator.serverNotice(5, s));
        }
    }

    public boolean getFinishedDojoTutorial() {
        return finishedDojoTutorial;
    }

    public void setFinishedDojoTutorial(boolean v) {
        this.finishedDojoTutorial = v;
    }

    //<editor-fold defaultstate="collapsed" desc="Hibernate专属函数">   
    private void setId(int id) {
        this.id = id;
    }

    private void setEquipSlots(int value) {
        MapleInventoryType type = MapleInventoryType.EQUIP;
        inventory[type.ordinal()] = new MapleInventory(type, (byte) value);
    }

    private int getEquipSlots() {
        MapleInventoryType type = MapleInventoryType.EQUIP;
        return inventory[type.ordinal()].getSlots();
    }

    private void setUseSlots(int value) {
        MapleInventoryType type = MapleInventoryType.USE;
        inventory[type.ordinal()] = new MapleInventory(type, (byte) value);
    }

    private int getUseSlots() {
        MapleInventoryType type = MapleInventoryType.USE;
        return inventory[type.ordinal()].getSlots();
    }

    private void setSetupSlots(int value) {
        MapleInventoryType type = MapleInventoryType.SETUP;
        inventory[type.ordinal()] = new MapleInventory(type, (byte) value);
    }

    private int getSetupSlots() {
        MapleInventoryType type = MapleInventoryType.SETUP;
        return inventory[type.ordinal()].getSlots();
    }

    private void setEtcSlots(int value) {
        MapleInventoryType type = MapleInventoryType.ETC;
        inventory[type.ordinal()] = new MapleInventory(type, (byte) value);
    }

    private int getEtcSlots() {
        MapleInventoryType type = MapleInventoryType.ETC;
        return inventory[type.ordinal()].getSlots();
    }

    private void setCashSlots(int value) {
        MapleInventoryType type = MapleInventoryType.CASH;
        inventory[type.ordinal()] = new MapleInventory(type, (byte) value);
    }

    private int getCashSlots() {
        MapleInventoryType type = MapleInventoryType.CASH;
        return inventory[type.ordinal()].getSlots();
    }

    public void setName(String str) {
        this.name = str;
    }
    //get方法 已存在

    public void setLevel(int lvl) {
        this.level = Math.min(lvl, ExpTable.getMaxLevel());
    }
    //get方法 已存在

    public void setJh(int i) {
        this.jh = i;
    }

    private int getZhongJi() {
        return ZhongJi;
    }

    private void setSp(String string) {
        String[] tmp = string.split(",");
        for (int i = 0; i < tmp.length; i++) {
            remainingsp[i] = Integer.valueOf(tmp[i]);
        }
    }

    private String getSp() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < remainingsp.length; i++) {
            sb.append(remainingsp[i].toString());
            if (i + 1 != remainingsp.length) {
                sb.append(",");
            }
        }
        return sb.toString();
    }

    private void setMeso(long value) {
        this.meso.set(value);
    }

    private int getSkincolor() {
        return this.skinColor.getId();
    }

    private void setSkincolor(int value) {
        this.skinColor = MapleSkinColor.getById(value);
    }
    //</editor-fold>

    public synchronized static MapleCharacter loadCharFromDB(int charid,
            MapleClient client, boolean channelserver) {
        MapleCharacter ret = new MapleCharacter();
        try {
            DatabaseConnection.GetLock(charid).lock();
            ret.client = client;
            ret.id = charid;
            Connection con = DatabaseConnection.getConnection();

            PreparedStatement ps = con.prepareStatement("SELECT * FROM characters WHERE id = ?");
            ps.setInt(1, charid);
            ResultSet rs = ps.executeQuery();
            if (!rs.next()) {
                throw new RuntimeException("连接角色没有找到 (角色没有找到)");
            }
            ret.inventory[MapleInventoryType.EQUIP.ordinal()] = new MapleInventory(
                    MapleInventoryType.EQUIP, (byte) rs.getInt("equipSlots"));
            ret.inventory[MapleInventoryType.USE.ordinal()] = new MapleInventory(
                    MapleInventoryType.USE, (byte) rs.getInt("useSlots"));
            ret.inventory[MapleInventoryType.SETUP.ordinal()] = new MapleInventory(
                    MapleInventoryType.SETUP, (byte) rs.getInt("setupSlots"));
            ret.inventory[MapleInventoryType.ETC.ordinal()] = new MapleInventory(
                    MapleInventoryType.ETC, (byte) rs.getInt("etcSlots"));
            ret.inventory[MapleInventoryType.CASH.ordinal()] = new MapleInventory(
                    MapleInventoryType.CASH, (byte) rs.getInt("cashSlots"));
            ret.name = rs.getString("name");
            ret.level = Math.min(rs.getInt("level"), ExpTable.getMaxLevel());
            ret.vip = rs.getInt("vip");
            ret.fs = rs.getInt("fs");
            ret.jh = rs.getInt("jh");
            ret.ZhongJi = rs.getInt("ZhongJi");
            ret.ShuoHua = rs.getInt("ShouHua");
            ret.reborns = rs.getInt("reborns");
            ret.SD = rs.getInt("SD");
            ret.fame = rs.getInt("fame");
            ret.str = rs.getInt("str");
            ret.dex = rs.getInt("dex");
            ret.int_ = rs.getInt("int");
            ret.luk = rs.getInt("luk");
            ret.exp.set(Math.max(
                    0,
                    Math.min(Math.max(0, rs.getLong("exp")),
                    ExpTable.getExpNeededForLevel(ret.level) - 2)));
            ret.hp = rs.getInt("hp");
            ret.maxhp = rs.getInt("maxhp");
            ret.mp = rs.getInt("mp");
            ret.maxmp = rs.getInt("maxmp");

            ret.hpApUsed = rs.getInt("hpApUsed");
            ret.mpApUsed = rs.getInt("mpApUsed");

            ret.remainingAp = rs.getInt("ap");
            String[] tmp = rs.getString("sp").split(",");
            for (int i = 0; i < tmp.length; i++) {
                ret.remainingsp[i] = Integer.valueOf(tmp[i]);
            }

            ret.meso.set(Math.max(0, rs.getLong("meso")));

            ret.GMLevel = rs.getInt("gm");

            int mountexp = rs.getInt("mountexp");
            int mountlevel = rs.getInt("mountlevel");
            int mounttiredness = rs.getInt("mounttiredness");

            ret.skinColor = MapleSkinColor.getById(rs.getInt("skincolor"));
            ret.gender = rs.getInt("gender");
            ret.job = MapleJob.getById(rs.getInt("job"));
            ret.beans = rs.getInt("beans");

            ret.hiredmerchantinventory.meso = rs.getLong("merchantmesos");

            // 总倾向
            ret.charisma = rs.getInt("charisma");
            ret.sense = rs.getInt("sense");
            ret.insight = rs.getInt("insight");
            ret.volition = rs.getInt("volition");
            ret.hands = rs.getInt("hands");
            ret.charm = rs.getInt("charm");
            // 今日增加的倾向
            ret.TDS_charisma = rs.getInt("TDS_charisma");
            ret.TDS_sense = rs.getInt("TDS_sense");
            ret.TDS_insight = rs.getInt("TDS_insight");
            ret.TDS_volition = rs.getInt("TDS_volition");
            ret.TDS_hands = rs.getInt("TDS_hands");
            ret.TDS_charm = rs.getInt("TDS_charm");
            // Pvp
            ret.pkLevel = rs.getInt("pkLevel");
            ret.battlePoint = rs.getInt("battlePoint");
            ret.battleExp = rs.getInt("battleExp");
            ret.canTalk = rs.getInt("cantalk"); // cantalk

            ret.marryid = rs.getInt("marryid"); // marriage
            ret.partnerid = rs.getInt("partnerid");
            ret.setPartnerName(rs.getString("partnerName"));
            ret.marriageQuestLevel = rs.getInt("marriagequest");
            ret.zakumLvl = rs.getInt("zakumLvl");
            ret.hair = rs.getInt("hair");
            ret.face = rs.getInt("face");
            ret.finishedDojoTutorial = rs.getInt("finishedDojoTutorial") == 1;
            ret.dojoPoints = rs.getInt("dojoPoints");
            ret.lastDojoStage = rs.getInt("lastDojoStage");
            ret.vanquisherKills = rs.getInt("vanquisherKills");
            ret.vanquisherStage = rs.getInt("vanquisherStage");

            ret.accountid = rs.getInt("accountid");

            ret.mapid = rs.getInt("map");
            ret.initialSpawnPoint = rs.getInt("spawnpoint");
            ret.world = rs.getInt("world");

            ret.rank = rs.getInt("rank");
            ret.rankMove = rs.getInt("rankMove");
            ret.jobRank = rs.getInt("jobRank");
            ret.jobRankMove = rs.getInt("jobRankMove");

            ret.familyId = rs.getInt("familyId");
            ret.guildid = rs.getInt("guildid");
            ret.guildrank = rs.getInt("guildrank");
            if (ret.guildid > 0) {
                ret.mgc = new MapleGuildCharacter(ret);
            }
            ret.allianceRank = rs.getInt("alliancerank");
            ret.Warning = rs.getInt("Warning");
            int buddyCapacity = rs.getInt("buddyCapacity");
            ret.buddylist = new BuddyList(buddyCapacity);

            ret.autoHpPot = rs.getInt("autoHpPot");
            ret.autoMpPot = rs.getInt("autoMpPot");

            ret.bossPoints = rs.getInt("bosspoints");
            ret.bossRepeats = rs.getInt("bossrepeats");

            ret.nextBQ = rs.getLong("nextBQ");
            ret.muted = rs.getInt("muted") == 1 ? true : false;
            ret.playerNPC = rs.getInt("playerNPC") > 0 ? true : false;
            ret.face_adorn = rs.getInt("face_adorn");
            ret.necklace_expansion = rs.getTimestamp("necklace_expansion");
            ret.clfb = rs.getInt("clfb");
            Calendar c = Calendar.getInstance();
            c.setTime(new java.util.Date(rs.getLong("unmutetime")));
            ret.unmuteTime = c;
            if (channelserver) {
                MapleMapFactory mapFactory = client.getChannelServer().getMapFactory();
                ret.map = mapFactory.getMap(ret.mapid);
                if (ret.map == null) { // char is on a map that doesn't exist
                    // warp it to henesys
                    ret.map = mapFactory.getMap(100000000);
                } else if (ret.map.getForcedReturnId() != 999999999) {
                    ret.map = mapFactory.getMap(ret.map.getForcedReturnId());
                }
                MaplePortal portal = ret.map.getPortal(ret.initialSpawnPoint);
                if (portal == null) {
                    portal = ret.map.getPortal(0); // char is on a spawnpoint
                    // that doesn't exist -
                    // select the first
                    // spawnpoint instead
                    ret.initialSpawnPoint = 0;
                }
                ret.setPosition(portal.getPosition());

                int partyid = rs.getInt("party");
                if (partyid >= 0) {
                    try {
                        MapleParty party = client.getChannelServer().getWorldInterface().getParty(partyid);
                        if (party != null
                                && party.getMemberById(ret.id) != null) {
                            ret.party = party;
                        }
                    } catch (RemoteException e) {
                        ServerExceptionHandler.HandlerRemoteException(e);
                        client.getChannelServer().reconnectWorld();
                    }
                }

                int messengerid = rs.getInt("messengerid");
                int position = rs.getInt("messengerposition");
                if (messengerid > 0 && position < 4 && position > -1) {
                    try {
                        WorldChannelInterface wci = client.getChannelServer().getWorldInterface();
                        MapleMessenger messenger = wci.getMessenger(messengerid);
                        if (messenger != null) {
                            ret.messenger = messenger;
                            ret.messengerposition = position;
                        }
                    } catch (RemoteException e) {
                        ServerExceptionHandler.HandlerRemoteException(e);
                        client.getChannelServer().reconnectWorld();
                    }
                }

                ret.attribute = MapleCharAttribute.loadFromDatabase(charid);
                ret.attribute.setPlayer(ret);
            }

            rs.close();
            ps.close();

            ps = con.prepareStatement("SELECT * FROM accounts WHERE id = ?");
            ps.setInt(1, ret.accountid);
            rs = ps.executeQuery();
            if (rs.next()) {
                ret.getClient().setAccountName(rs.getString("name"));
                ret.paypalnx = rs.getInt("paypalNX");
                ret.maplepoints = rs.getInt("mPoints");
                ret.cardnx = rs.getInt("cardNX");
                ret.money = rs.getInt("money");
                ret.Present = rs.getInt("Present");
                if (channelserver) {
                    int waitaddnx = rs.getInt("wait_paypalNX");
                    if (waitaddnx > 0) {
                        ret.money += waitaddnx;
                        ps = con.prepareStatement("update accounts set wait_paypalNX = 0 where id = ?");
                        ps.setInt(1, ret.accountid);
                        ps.executeUpdate();
                    }
                }
            }
            rs.close();
            ps.close();

            ret.buddylist.loadFromDb(charid, con);
            Timestamp currenttime = new Timestamp(System.currentTimeMillis());

            if (ret.necklace_expansion != null
                    && currenttime.after(ret.necklace_expansion)) {
                ret.necklace_expansion = null;
                ret.mapletips.add("项链扩充已过期。");
            }

            ret.storage = new MapleStorage(ret.accountid, con);
            ret.csinventory = new MapleCSInventory(ret);

            CallableStatement call = con.prepareCall("call getItems(? , ?)");
            call.setInt(1, ret.accountid);
            call.setInt(2, charid);
            call.execute();
            rs = call.getResultSet();
            while (rs.next()) {
                IItem to = ret.LoadItem(rs, MapleItemsNameSpaceType.Storages, currenttime,
                        channelserver);
                if (to == null) {
                    continue;
                }
                ret.storage.AddItem(to);
            }
            rs.close();
            if (call.getMoreResults()) {
                rs = call.getResultSet();
                while (rs.next()) {
                    MapleItemsNameSpaceType nameSpaceType = MapleItemsNameSpaceType.Unknown;
                    switch (rs.getInt("space")) {
                        case -1:
                            nameSpaceType = MapleItemsNameSpaceType.Inventory;
                            break;
                        case -2:
                        case -3:
                            nameSpaceType = MapleItemsNameSpaceType.CsInventory;
                            break;
                    }
                    IItem to = ret.LoadItem(rs, nameSpaceType, currenttime,
                            channelserver);
                    if (to == null) {
                        continue;
                    }
                    if (nameSpaceType.equals(MapleItemsNameSpaceType.Inventory)) {
                        ret.getInventory(
                                MapleInventoryType.getByType(rs.getByte("type"))).addFromDB(to);
                    } else if (nameSpaceType.equals(MapleItemsNameSpaceType.CsInventory)) {
                        ret.csinventory.addFromDb(to, rs, rs.getInt("space") == -3);
                    }
                }
                rs.close();
            }
            call.close();

            /*         for (int i = 0; i < ITEMS_SQL.size(); i++) {
             String sql_ = ITEMS_SQL.get(i);
             MapleItemsNameSpaceType nameSpaceType = MapleItemsNameSpaceType.Unknown;
             ps = con.prepareStatement(sql_);
             switch (i) {
             case 0:
             nameSpaceType = MapleItemsNameSpaceType.Inventory;
             ps.setInt(1, charid);
             break;
             case 1:
             nameSpaceType = MapleItemsNameSpaceType.Storages;
             ps.setInt(1, ret.accountid);
             break;
             case 2:
             case 3:
             nameSpaceType = MapleItemsNameSpaceType.CsInventory;
             ps.setInt(1, charid);
             break;
             }
             rs = ps.executeQuery();
             while (rs.next()) {
             IItem to = ret.LoadItem(rs, nameSpaceType, currenttime,
             channelserver);
             if (to == null) {
             continue;
             }
             if (nameSpaceType.equals(MapleItemsNameSpaceType.Inventory)) {
             ret.getInventory(
             MapleInventoryType.getByType(rs.getByte("type"))).addFromDB(to);
             } else if (nameSpaceType.equals(MapleItemsNameSpaceType.CsInventory)) {
             ret.csinventory.addFromDb(to, rs, i == 3);
             } else if (nameSpaceType.equals(MapleItemsNameSpaceType.Storages)) {
             ret.storage.AddItem(to);
             } else if (nameSpaceType.equals(MapleItemsNameSpaceType.HiredMerchant)) {
             ret.hiredmerchantinventory.addItem(to);
             } else {
             log.error("未知空间名：" + rs.getByte("type"));
             }
             }
             rs.close();
             ps.close();
             }*/
            if (channelserver) {
                ps = con.prepareStatement("SELECT * FROM queststatus WHERE characterid = ?");
                ps.setInt(1, charid);
                rs = ps.executeQuery();
                PreparedStatement pse = con.prepareStatement("SELECT * FROM queststatusmobs WHERE queststatusid = ?");
                while (rs.next()) {
                    MapleQuest q = MapleQuest.getInstance(rs.getInt("quest"));
                    MapleQuestStatus status = new MapleQuestStatus(
                            q,
                            MapleQuestStatus.Status.getById(rs.getInt("status")));
                    long cTime = rs.getLong("time");
                    if (cTime > -1) {
                        status.setCompletionTime(cTime * 1000);
                    }
                    status.setForfeited(rs.getInt("forfeited"));
                    ret.quests.put(q, status);
                    pse.setInt(1, rs.getInt("queststatusid"));
                    ResultSet rsMobs = pse.executeQuery();
                    if (rsMobs.next()) {
                        status.setMobKills(rsMobs.getInt("mob"),
                                rsMobs.getInt("count"));
                    }
                    rsMobs.close();
                }
                rs.close();
                ps.close();
                pse.close();

                ps = con.prepareStatement("SELECT * FROM skills WHERE characterid = ?");
                ps.setInt(1, charid);
                rs = ps.executeQuery();
                while (rs.next()) {
                    int skillid = rs.getInt("skillid");
                    int skilllevel = rs.getInt("skilllevel");
                    int masterlevel = rs.getInt("masterlevel");
                    ISkill sf = SkillFactory.getSkill(skillid);
                    if (sf == null) {
                        continue;
                    }
                    skilllevel = Math.min(skilllevel, sf.getMaxLevel());
                    if (sf.hasMastery()) {
                        masterlevel = Math.max(skilllevel, masterlevel);
                    } else {
                        masterlevel = 0;
                    }
                    // 不允许加载
                    if (GameConstants.勇士的意志技能系(skillid)
                            || GameConstants.isAngelRingSkill(skillid)
                            || GameConstants.banskill.contains(skillid)) {
                        continue;
                    }
                    Timestamp time = rs.getTimestamp("expiredate");
                    if (time == null
                            || (time != null && !currenttime.after(time))) {
                        // log.info("载入技能：" + SkillFactory.getSkillName(skillid)
                        // + ".技能等级；" + skilllevel + ".能力等级：" + masterlevel);
                        ret.skills.put(sf, new SkillEntry(skilllevel,
                                masterlevel, time));
                    }
                }
                rs.close();
                ps.close();

                // 加载精灵的祝福
                ps = con.prepareStatement("SELECT * FROM characters WHERE accountid = ? ORDER BY level DESC");
                ps.setInt(1, ret.accountid);
                rs = ps.executeQuery();

                if (rs.next()) {
                    if (rs.getInt("id") != charid) {
                        ret.linkedName = rs.getString("name");
                    }
                }
                ps.close();
                rs.close();

                // 加载女皇的祝福
                ps = con.prepareStatement("SELECT * FROM skillmacros WHERE characterid = ?");
                ps.setInt(1, charid);
                rs = ps.executeQuery();
                while (rs.next()) {
                    int skill1 = rs.getInt("skill1");
                    int skill2 = rs.getInt("skill2");
                    int skill3 = rs.getInt("skill3");
                    String name = rs.getString("name");
                    int shout = rs.getInt("shout");
                    int position = rs.getInt("position");
                    SkillMacro macro = new SkillMacro(skill1, skill2, skill3,
                            name, shout);
                    ret.skillMacros[position] = macro;
                }
                rs.close();
                ps.close();

                ps = con.prepareStatement("SELECT `key`,`type`,`action` FROM keymap WHERE characterid = ?");
                ps.setInt(1, charid);
                rs = ps.executeQuery();
                while (rs.next()) {
                    int key = rs.getInt("key");
                    int type = rs.getInt("type");
                    int action = rs.getInt("action");
                    ret.keymap.put(Integer.valueOf(key), new MapleKeyBinding(
                            type, action));
                }
                rs.close();
                ps.close();

                ps = con.prepareStatement("SELECT `locationtype`,`map` FROM savedlocations WHERE characterid = ?");
                ps.setInt(1, charid);
                rs = ps.executeQuery();
                while (rs.next()) {
                    String locationType = rs.getString("locationtype");
                    int mapid = rs.getInt("map");
                    ret.savedLocations[SavedLocationType.valueOf(
                            locationType.toUpperCase()).ordinal()] = mapid;
                }
                rs.close();
                ps.close();

                ps = con.prepareStatement("SELECT `characterid_to`,`when` FROM famelog WHERE characterid = ? AND DATEDIFF(NOW(),`when`) < 30");
                ps.setInt(1, charid);
                rs = ps.executeQuery();
                ret.lastfametime = 0;
                ret.lastmonthfameids = new ArrayList<Integer>(31);
                while (rs.next()) {
                    ret.lastfametime = Math.max(ret.lastfametime, rs.getTimestamp("when").getTime());
                    ret.lastmonthfameids.add(Integer.valueOf(rs.getInt("characterid_to")));
                }
                rs.close();
                ps.close();

                ps = con.prepareStatement("SELECT ares_data FROM char_ares_info WHERE charid = ?");
                ps.setInt(1, charid);
                rs = ps.executeQuery();
                while (rs.next()) {
                    ret.ares_data.add(rs.getString("ares_data"));
                }
                rs.close();
                ps.close();

                ps = con.prepareStatement("SELECT skillid, starttime,length FROM cooldowns WHERE characterid = ?");
                ps.setInt(1, charid);
                rs = ps.executeQuery();
                while (rs.next()) {
                    if (rs.getLong("length") + rs.getLong("starttime")
                            - System.currentTimeMillis() <= 0) {
                        continue;
                    }
                    ret.giveCoolDowns(rs.getInt("skillid"),
                            rs.getLong("starttime"), rs.getLong("length"));
                }
                rs.close();
                ps.close();

                ps = con.prepareStatement("DELETE FROM cooldowns WHERE characterid = ?");
                ps.setInt(1, charid);
                ps.executeUpdate();
                ps.close();
            }

            String achsql = "SELECT * FROM achievements WHERE accountid = ?";
            ps = con.prepareStatement(achsql);
            ps.setInt(1, ret.accountid);
            rs = ps.executeQuery();
            while (rs.next()) {
                ret.finishedAchievements.add(rs.getInt("achievementid"));
            }
            rs.close();
            ps.close();

            int skillid = ret.getJobType() + 1004; // 修改了getJobType函数
            if (ret.getInventory(MapleInventoryType.EQUIPPED).getItem(
                    (byte) -18) != null) {
                ret.maplemount = new MapleMount(ret, ret.getInventory(MapleInventoryType.EQUIPPED).getItem((byte) -18).getItemId(), skillid);
                ret.maplemount.setExp(mountexp);
                ret.maplemount.setLevel(mountlevel);
                ret.maplemount.setTiredness(mounttiredness);
                ret.maplemount.setActive(false);
            } else {
                ret.maplemount = new MapleMount(ret, 0, skillid);
                ret.maplemount.setExp(mountexp);
                ret.maplemount.setLevel(mountlevel);
                ret.maplemount.setTiredness(mounttiredness);
                ret.maplemount.setActive(false);
            }
            ret.recalcLocalStats();
            ret.silentEnforceMaxHpMp();
            ISkill ship = SkillFactory.getSkill(5221006);
            ret.battleshipHP = (ret.getSkillLevel(ship) * 4000)
                    + ((ret.getLevel() - 120) * 2000);
            ret.loggedInTimer = System.currentTimeMillis();
            con.close();
        } catch (SQLException ex) {
            ex.printStackTrace();
        } finally {
            DatabaseConnection.GetLock(charid).unlock();
        }
        return ret;
    }

    private IItem LoadItem(ResultSet rs, MapleItemsNameSpaceType nameSpaceType,
            Timestamp currenttime, boolean channelserver) throws SQLException {
        MapleInventoryType type;
        if (!nameSpaceType.equals(MapleItemsNameSpaceType.Inventory)) {
            type = MapleItemInformationProvider.getInstance().getInventoryType(
                    rs.getInt("itemid"));
        } else {
            type = MapleItemsNameSpaceType.GetInventoryTypeByDbId(rs.getInt("type"));
        }
        Timestamp expiration = rs.getTimestamp("expiredate");
        IItem to;
        if ((type.equals(MapleInventoryType.EQUIP) || type.equals(MapleInventoryType.EQUIPPED))) {
            int itemid = rs.getInt("itemid");
            Equip equip = new Equip(itemid, (short) rs.getInt("position"));
            if ((equip.友谊戒指() || equip.恋人戒指())
                    && rs.getLong("partnerUniqueid") == 0) {
                log.info("戒指信息损坏。放弃载入。");
                return null;
            }
            if (rs.getLong("partnerUniqueid") > 0) {
                equip = MapleRing.loadFromDb(itemid, rs.getShort("position"),
                        rs.getLong("uniqueid"), rs);
            } else {
                equip.setOwner(rs.getString("owner"));
                equip.setQuantity((short) rs.getInt("quantity"));
                equip.setAcc((short) rs.getInt("acc"));
                equip.setAvoid((short) rs.getInt("avoid"));
                equip.setDex((short) rs.getInt("dex"));
                equip.setHands((short) rs.getInt("hands"));
                equip.setHp((short) rs.getInt("hp"));
                equip.setInt((short) rs.getInt("int"));
                equip.setJump((short) rs.getInt("jump"));
                equip.setLuk((short) rs.getInt("luk"));
                equip.setMatk((short) rs.getInt("matk"));
                equip.setMdef((short) rs.getInt("mdef"));
                equip.setMp((short) rs.getInt("mp"));
                equip.setSpeed((short) rs.getInt("speed"));
                equip.setStr((short) rs.getInt("str"));
                equip.setWatk((short) rs.getInt("watk"));
                equip.setWdef((short) rs.getInt("wdef"));
                equip.setUpgradeSlots((byte) rs.getInt("upgradeslots"));
                equip.setLevel((byte) rs.getInt("level"));
                equip.setFlag(rs.getInt("flag"));
                equip.setVicious((short) rs.getInt("vicious"));
                equip.setUniqueId(rs.getLong("uniqueid"));
                // 潜能
                equip.setPotential_1((short) rs.getInt("Potential_1"));
                equip.setPotential_2((short) rs.getInt("Potential_2"));
                equip.setPotential_3((short) rs.getInt("Potential_3"));
                equip.setIdentify((byte) rs.getInt("Identify"));
                equip.setStarlevel((byte) rs.getInt("Starlevel"));
                equip.setIdentified((byte) rs.getInt("Identified"));
                // 道具等级 道具经验值 耐久度
                equip.setItemLevel((byte) rs.getInt("ItemLevel"));
                equip.setItemExp((int) rs.getInt("ItemExp"));
                equip.setItemSkill((byte) rs.getInt("ItemSkill"));
                equip.setDurability((int) rs.getInt("Durability"));
                equip.setPvpWatk(rs.getInt("pvpWatk"));
                equip.setUnlockTime(rs.getTimestamp("unlocktime"));
                if (equip.getUnlockTime() != null
                        && currenttime.after(equip.getUnlockTime())) {
                    equip.CanceFlag(Flags.锁定);
                    equip.setUnlockTime(null);
                }
                if (equip.getUniqueid() > 0) {
                    if (equip.getItemId() / 10000 == 166) {
                        MapleAndroid ring = MapleAndroid.loadFromDb(equip.getItemId(), equip.getUniqueid());
                        if (ring != null) {
                            equip.setAndroid(ring);
                        }
                    }
                }
            }
            to = equip;
        } else {
            if (GameConstants.isPet(rs.getInt("itemid"))) {
                int index = rs.getInt("slot") - 1;
                MaplePet pet = MaplePet.loadFromDb(rs.getInt("itemid"),
                        rs.getShort("position"), rs.getLong("uniqueid"), rs);
                if (index > -1 && channelserver) {
                    Point pos = getPosition();
                    pos.y -= 12;
                    pet.setPos(pos);
                    pet.setFh(getMap().getFootholds().findBelow(pet.getPos()).getId());
                    pet.setStance(0);
                    pets[index] = pet;
                    pet.setSlot(index);
                    pet.StartFullnessSchedule(this);
                }
                to = pet;
            } else {
                to = new Item(rs.getInt("itemid"),
                        (byte) rs.getInt("position"),
                        (short) rs.getInt("quantity"));
                to.setUniqueId(rs.getLong("uniqueid"));
            }
            to.setOwner(rs.getString("owner"));
            to.setFlag(rs.getInt("flag"));
        }
        if (expiration != null) {
            if (!currenttime.after(expiration)) {
                to.setExpiration(expiration);
            } else {
                mapletips.add("道具 ["
                        + MapleItemInformationProvider.getInstance().getName(
                        rs.getInt("itemid")) + "] 已过期道具被清除了。");
                return null;
            }
        }
        return to;
    }

    public static MapleCharacter getDefault(MapleClient client) {
        MapleCharacter ret = new MapleCharacter();
        ret.client = client;
        ret.inventory[MapleInventoryType.EQUIP.ordinal()] = new MapleInventory(
                MapleInventoryType.EQUIP, (byte) 96);
        ret.inventory[MapleInventoryType.USE.ordinal()] = new MapleInventory(
                MapleInventoryType.USE, (byte) 96);
        ret.inventory[MapleInventoryType.SETUP.ordinal()] = new MapleInventory(
                MapleInventoryType.SETUP, (byte) 96);
        ret.inventory[MapleInventoryType.ETC.ordinal()] = new MapleInventory(
                MapleInventoryType.ETC, (byte) 96);
        ret.inventory[MapleInventoryType.CASH.ordinal()] = new MapleInventory(
                MapleInventoryType.CASH, (byte) 96);
        ret.hp = 50;
        ret.maxhp = 50;
        ret.mp = 50;
        ret.maxmp = 50;
        ret.map = null;
        ret.exp.set(0);
        ret.GMLevel = 0;
        ret.vip = 0;
        ret.jh = 0;
        ret.fs = 0;
        ret.reborns = 0;
        ret.SD = 0;
        ret.ShuoHua = 0;
        ret.ZhongJi = 0;
        ret.job = MapleJob.BEGINNER;
        ret.meso.set(200000);// 设置创建角色金币为20W
        ret.mapid = LoginServer.getInstance().getInitMapId();
        ret.level = 1;
        ret.accountid = client.getAccID();
        ret.buddylist = new BuddyList(20);
        ret.maplemount = null;
        ret.CP = 0;
        ret.totalCP = 0;
        ret.team = -1;
        for (int i = 0; i < ret.remainingsp.length; i++) {
            ret.remainingsp[i] = 0;
        }
        try {
            Connection con = DatabaseConnection.getConnection();
            PreparedStatement ps;
            ps = con.prepareStatement("SELECT * FROM accounts WHERE id = ?");
            ps.setInt(1, ret.accountid);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                ret.getClient().setAccountName(rs.getString("name"));
                ret.paypalnx = rs.getInt("paypalNX");
                ret.maplepoints = rs.getInt("mPoints");
                ret.cardnx = rs.getInt("cardNX");
                ret.money = rs.getInt("money");
            }
            rs.close();
            ps.close();
            con.close();
        } catch (SQLException e) {
            log.error("ERROR", e);
        }
        ret.incs = false;
        ret.inmts = false;
        ret.APQScore = 0;
        ret.allianceRank = 5;
        ret.keymap.put(Integer.valueOf(2), new MapleKeyBinding(4, 10));
        ret.keymap.put(Integer.valueOf(3), new MapleKeyBinding(4, 12));
        ret.keymap.put(Integer.valueOf(4), new MapleKeyBinding(4, 13));
        ret.keymap.put(Integer.valueOf(5), new MapleKeyBinding(4, 18));
        ret.keymap.put(Integer.valueOf(6), new MapleKeyBinding(4, 24));
        ret.keymap.put(Integer.valueOf(7), new MapleKeyBinding(4, 21));
        ret.keymap.put(Integer.valueOf(16), new MapleKeyBinding(4, 8));
        ret.keymap.put(Integer.valueOf(17), new MapleKeyBinding(4, 5));
        ret.keymap.put(Integer.valueOf(18), new MapleKeyBinding(4, 0));
        ret.keymap.put(Integer.valueOf(19), new MapleKeyBinding(4, 4));
        ret.keymap.put(Integer.valueOf(23), new MapleKeyBinding(4, 1));
        ret.keymap.put(Integer.valueOf(25), new MapleKeyBinding(4, 19));
        ret.keymap.put(Integer.valueOf(26), new MapleKeyBinding(4, 14));
        ret.keymap.put(Integer.valueOf(27), new MapleKeyBinding(4, 15));
        ret.keymap.put(Integer.valueOf(29), new MapleKeyBinding(5, 52));
        ret.keymap.put(Integer.valueOf(31), new MapleKeyBinding(4, 2));
        ret.keymap.put(Integer.valueOf(34), new MapleKeyBinding(4, 17));
        ret.keymap.put(Integer.valueOf(35), new MapleKeyBinding(4, 11));
        ret.keymap.put(Integer.valueOf(37), new MapleKeyBinding(4, 3));
        ret.keymap.put(Integer.valueOf(38), new MapleKeyBinding(4, 20));
        ret.keymap.put(Integer.valueOf(40), new MapleKeyBinding(4, 16));
        ret.keymap.put(Integer.valueOf(41), new MapleKeyBinding(4, 23));
        ret.keymap.put(Integer.valueOf(43), new MapleKeyBinding(4, 9));
        ret.keymap.put(Integer.valueOf(44), new MapleKeyBinding(5, 50));
        ret.keymap.put(Integer.valueOf(45), new MapleKeyBinding(5, 51));
        ret.keymap.put(Integer.valueOf(46), new MapleKeyBinding(4, 6));
        ret.keymap.put(Integer.valueOf(48), new MapleKeyBinding(4, 22));
        ret.keymap.put(Integer.valueOf(50), new MapleKeyBinding(4, 7));
        ret.keymap.put(Integer.valueOf(56), new MapleKeyBinding(5, 53));
        ret.keymap.put(Integer.valueOf(57), new MapleKeyBinding(5, 54));
        ret.keymap.put(Integer.valueOf(59), new MapleKeyBinding(6, 100));
        ret.keymap.put(Integer.valueOf(60), new MapleKeyBinding(6, 101));
        ret.keymap.put(Integer.valueOf(61), new MapleKeyBinding(6, 102));
        ret.keymap.put(Integer.valueOf(62), new MapleKeyBinding(6, 103));
        ret.keymap.put(Integer.valueOf(63), new MapleKeyBinding(6, 104));
        ret.keymap.put(Integer.valueOf(64), new MapleKeyBinding(6, 105));
        ret.keymap.put(Integer.valueOf(65), new MapleKeyBinding(6, 106));
        ret.recalcLocalStats();
        return ret;
    }

    public Lock getDatabaseLock() {
        return DatabaseConnection.GetLock(id);
    }

    public synchronized void saveToDB() {
        saveToDB(true);
    }

    public synchronized void saveToDB(boolean update) {
        Connection con = null;
        int t = 0;

        try {
            if (update) {
                getDatabaseLock().lock();
            }
            con = DatabaseConnection.getConnection();
            t = con.getTransactionIsolation();
            con.setTransactionIsolation(Connection.TRANSACTION_READ_UNCOMMITTED);
            con.setAutoCommit(false);
            PreparedStatement ps;
            if (update) {
                // 这个是更新数据 保存角色数据时用
                ps = con.prepareStatement("UPDATE characters SET "
                        + "level = ?, fame = ?, str = ?, dex = ?, luk = ?, `int` = ?, exp = ?, hp = ?, mp = ?, maxhp = ?, "
                        + "maxmp = ?, sp = ?, ap = ?, gm = ?, skincolor = ?, gender = ?, job = ?, hair = ?, face = ?, map = ?, "
                        + "meso = ?, hpApUsed = ?, mpApUsed = ?, spawnpoint = ?, party = ?, buddyCapacity = ?, autoHpPot = ?, autoMpPot = ?, messengerid = ?, messengerposition = ?, "
                        + "marryid = ?, partnerid = ?, cantalk = ?, zakumlvl = ?, marriagequest = ?, bosspoints = ?, bossrepeats = ?, nextBQ = ?, playerNPC = ?, alliancerank = ?, "
                        + "muted = ?, unmutetime = ?, equipSlots = ?, useSlots = ?, setupSlots = ?, etcSlots = ?, cashSlots = ?, mountlevel = ?, mountexp = ?, mounttiredness = ?, "
                        + "dojoPoints = ?, lastDojoStage = ?, finishedDojoTutorial = ?, vanquisherStage = ?, vanquisherKills = ?, Warning = ?, beans = ?, charisma = ?, sense = ?, insight = ?, "
                        + "volition = ?, hands = ?, charm = ?, TDS_charisma = ?, TDS_sense = ?, TDS_insight = ?, TDS_volition = ?, TDS_hands = ?, TDS_charm = ?, pkLevel = ?, "
                        + "battlePoint = ?, battleExp = ?, partnerName = ?, face_adorn = ?,"
                        + "jh = ?, fs = ?, SD = ?, vip = ?,reborns = ?,necklace_expansion = ?,clfb=?, pvpkills=?, pvpdeaths=?, ShouHua=?"
                        + " WHERE id = ?");
            } else {
                // 这个是设置数据 在创建角色时用的
                ps = con.prepareStatement("INSERT INTO characters ("
                        + "level, fame, str, dex, luk, `int`, exp, hp, mp, maxhp, "
                        + "maxmp, sp, ap, gm, skincolor, gender, job, hair, face, map, "
                        + "meso, hpApUsed, mpApUsed, spawnpoint, party, buddyCapacity, autoHpPot, autoMpPot, messengerid, messengerposition, "
                        + "marryid, partnerid, cantalk, zakumlvl, marriagequest, bosspoints, bossrepeats, nextBQ, playerNPC, alliancerank, "
                        + "muted, unmutetime, equipSlots, useSlots, setupSlots, etcSlots, cashSlots, mountlevel, mountexp, mounttiredness, "// ..OK
                        + "dojopoints, lastDojoStage, finishedDojoTutorial, vanquisherStage, vanquisherKills, Warning, beans, charisma, sense, insight, "
                        + "volition, hands, charm, TDS_charisma, TDS_sense, TDS_insight, TDS_volition, TDS_hands, TDS_charm, pkLevel, "
                        + "battlePoint, battleExp, partnerName, face_adorn,"
                        + "accountid, name, world, clfb, pvpkills, pvpdeaths) VALUES "
                        + "(?, ?, ?, ?, ?, ?, ?, ?, ?, ?, "
                        + "?, ?, ?, ?, ?, ?, ?, ?, ?, ?, "
                        + "?, ?, ?, ?, ?, ?, ?, ?, ?, ?, "
                        + "?, ?, ?, ?, ?, ?, ?, ?, ?, ?, "
                        + "?, ?, ?, ?, ?, ?, ?, ?, ?, ?, "
                        + "?, ?, ?, ?, ?, ?, ?, ?, ?, ?, "
                        + "?, ?, ?, ?, ?, ?, ?, ?, ?, ?, "
                        + "?, ?, ?, ?, ?, ?, ?, ?, ?, ?)", PreparedStatement.RETURN_GENERATED_KEYS);
            }
            ps.setInt(1, level);
            ps.setInt(2, fame);
            ps.setInt(3, str);
            ps.setInt(4, dex);
            ps.setInt(5, luk);
            ps.setInt(6, int_);
            ps.setLong(7, exp.get());
            ps.setInt(8, hp);
            ps.setInt(9, mp);
            ps.setInt(10, maxhp);

            ps.setInt(11, maxmp);
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < remainingsp.length; i++) {
                sb.append(remainingsp[i].toString());
                if (i + 1 != remainingsp.length) {
                    sb.append(",");
                }
            }
            ps.setString(12, sb.toString());
            ps.setInt(13, remainingAp);
            ps.setInt(14, GMLevel);
            ps.setInt(15, skinColor.getId());
            ps.setInt(16, gender);
            ps.setInt(17, job == null ? 0 : job.getId());
            ps.setInt(18, hair);
            ps.setInt(19, face);
            ps.setInt(
                    20,
                    (map != null) ? (map.getForcedReturnId() != 999999999) ? map.getForcedReturnId() : map.getId()
                    : mapid);

            ps.setLong(21, meso.get());
            ps.setInt(22, hpApUsed);
            ps.setInt(23, mpApUsed);
            if (map == null) {
                ps.setInt(24, 0);
            } else {
                MaplePortal closest = map.findClosestSpawnpoint(getPosition());
                if (closest != null) {
                    ps.setInt(24, closest.getId());
                } else {
                    ps.setInt(24, 0);
                }
            }
            ps.setInt(25, (party != null) ? party.getId() : 0);
            ps.setInt(26, buddylist.getCapacity());
            ps.setInt(
                    27,
                    (autoHpPot != 0 && getItemAmount(autoHpPot) >= 1) ? autoHpPot
                    : 0);
            ps.setInt(
                    28,
                    (autoMpPot != 0 && getItemAmount(autoMpPot) >= 1) ? autoMpPot
                    : 0);
            ps.setInt(29, (messenger != null) ? messenger.getId() : 0);
            ps.setInt(30, (messenger != null) ? messengerposition : 0);

            ps.setInt(31, marryid);
            ps.setInt(32, partnerid);
            ps.setInt(33, canTalk);
            ps.setInt(34, (zakumLvl <= 2) ? zakumLvl : 2); // Don't let
            // zakumLevel exceed
            // three ;
            ps.setInt(35, marriageQuestLevel);
            ps.setInt(36, bossPoints);
            ps.setInt(37, bossRepeats);
            ps.setLong(38, nextBQ);
            ps.setInt(39, playerNPC ? 1 : 0);
            ps.setInt(40, allianceRank);

            ps.setInt(41, muted ? 1 : 0);
            ps.setLong(42,
                    unmuteTime == null ? 0 : unmuteTime.getTimeInMillis());
            ps.setInt(43, getInventory(MapleInventoryType.EQUIP).getSlots());
            ps.setInt(44, getInventory(MapleInventoryType.USE).getSlots());
            ps.setInt(45, getInventory(MapleInventoryType.SETUP).getSlots());
            ps.setInt(46, getInventory(MapleInventoryType.ETC).getSlots());
            ps.setInt(47, getInventory(MapleInventoryType.CASH).getSlots());
            ps.setInt(48, (maplemount != null) ? maplemount.getLevel() : 1);
            ps.setInt(49, (maplemount != null) ? maplemount.getExp() : 0);
            ps.setInt(50, (maplemount != null) ? maplemount.getTiredness() : 0);

            ps.setInt(51, dojoPoints);
            ps.setInt(52, lastDojoStage);
            ps.setInt(53, finishedDojoTutorial ? 1 : 0);
            ps.setInt(54, vanquisherStage);
            ps.setInt(55, vanquisherKills);
            ps.setInt(56, Warning);
            ps.setInt(57, beans);
            // 总倾向
            ps.setInt(58, charisma);
            ps.setInt(59, sense);
            ps.setInt(60, insight);

            ps.setInt(61, volition);
            ps.setInt(62, hands);
            ps.setInt(63, charm);
            // 今天的倾向
            ps.setInt(64, TDS_charisma);
            ps.setInt(65, TDS_sense);
            ps.setInt(66, TDS_insight);
            ps.setInt(67, TDS_volition);
            ps.setInt(68, TDS_hands);
            ps.setInt(69, TDS_charm);
            // Pvp
            ps.setInt(70, pkLevel);

            ps.setInt(71, battlePoint);
            ps.setInt(72, battleExp);
            // 配偶名字
            ps.setString(73, getPartnerName());
            // 脸饰
            ps.setInt(74, face_adorn);
            if (update) {
                // "jh = ?, fs = ?, SD = ?, vip = ?"
                ps.setInt(75, jh);
                ps.setInt(76, fs);
                ps.setInt(77, SD);
                ps.setInt(78, vip);
                ps.setInt(79, reborns);
                ps.setTimestamp(80, necklace_expansion);
                ps.setInt(81, clfb);
                ps.setInt(82, pvpkills);
                ps.setInt(83, pvpdeaths);
                ps.setInt(84, ShuoHua);
                ps.setInt(85, id);
            } else {
                ps.setInt(75, accountid);
                ps.setString(76, name);
                ps.setInt(77, world); // TODO store world somewhere ;)
                ps.setInt(78, clfb);
                ps.setInt(79, pvpkills);
                ps.setInt(80, pvpdeaths);
            }
            int updateRows = ps.executeUpdate();
            if (!update) {
                ResultSet rs = ps.getGeneratedKeys();
                if (rs.next()) {
                    this.id = rs.getInt(1);
                } else {
                    throw new DatabaseException("Inserting char failed.");
                }
            } else if (updateRows < 1) {
                throw new DatabaseException("Character not in database (" + id
                        + ")");
            }
            ps.close();

            ps = con.prepareStatement("DELETE FROM skillmacros WHERE characterid = ?");
            ps.setInt(1, id);
            ps.executeUpdate();
            ps.close();
            for (int i = 0; i < 5; i++) {
                SkillMacro macro = skillMacros[i];
                if (macro != null) {
                    ps = con.prepareStatement("INSERT INTO skillmacros (characterid, skill1, skill2, skill3, name, shout, position) VALUES (?, ?, ?, ?, ?, ?, ?)");
                    ps.setInt(1, id);
                    ps.setInt(2, macro.getSkill1());
                    ps.setInt(3, macro.getSkill2());
                    ps.setInt(4, macro.getSkill3());
                    ps.setString(5, macro.getName());
                    ps.setInt(6, macro.getShout());
                    ps.setInt(7, i);
                    ps.executeUpdate();
                    ps.close();
                }
            }
            // 删除所有物品不删除赠送物品。

            if (update) {
                /*ps = con.prepareStatement("DELETE FROM items_inventoryinfo WHERE characterid = ?");
                 ps.setInt(1, id);
                 ps.executeUpdate();
                 ps.close();
                 ps = con.prepareStatement("DELETE FROM items_storageinfo WHERE accountid = ?");
                 ps.setInt(1, accountid);
                 ps.executeUpdate();
                 ps.close();
                 ps = con.prepareStatement("DELETE FROM items_csinventoryinfo WHERE characterid = ?");
                 ps.setInt(1, id);
                 ps.executeUpdate();
                 ps.close();*/
                CallableStatement call = con.prepareCall("call deleteItems(?,?)");
                call.setInt(1, id);
                call.setInt(2, accountid);
                call.execute();
                call.close();
                if (storage != null) {
                    storage.saveToDB(con);
                }
            }

            CallableStatement create = con.prepareCall("call CreateItem(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)");
            create.setInt(1, id);
            for (MapleItemsNameSpace mapleItemsNameSpace : GetSaveNameSpaces()) {
                if (mapleItemsNameSpace == null) {
                    continue;
                }
                for (IItem iItem : mapleItemsNameSpace.AllItems()) {
                    create.setInt(2, iItem.getItemId());
                    create.setInt(3, iItem.getPosition());
                    create.setInt(4, iItem.getQuantity());
                    create.setString(5, iItem.getOwner() != null ? iItem.getOwner() : "");
                    create.setTimestamp(6, iItem.getExpiration());
                    if (MapleItemInformationProvider.getInstance().isCash(
                            iItem.getItemId())) {
                        if (iItem.getUniqueid() == 0) {
                            iItem.setUniqueId(getNextUniqueId());
                        }
                    } else {
                        if (iItem.getItemId() / 10000 != 166) {
                            iItem.setUniqueId(0);
                        }
                    }
                    create.setLong(7, iItem.getUniqueid());
                    create.setInt(8, iItem.getFlag());
                    create.setString(9, iItem.getLog().toString());
                    if (iItem instanceof Equip) {
                        Equip equip = (Equip) iItem;
                        create.setInt(10, equip.getUpgradeSlots());
                        create.setInt(11, equip.getLevel());
                        create.setInt(12, equip.getStr());
                        create.setInt(13, equip.getDex());
                        create.setInt(14, equip.getInt());
                        create.setInt(15, equip.getLuk());
                        create.setInt(16, equip.getHp());
                        create.setInt(17, equip.getMp());
                        create.setInt(18, equip.getWatk());
                        create.setInt(19, equip.getMatk());
                        create.setInt(20, equip.getWdef());
                        create.setInt(21, equip.getMdef());
                        create.setInt(22, equip.getAcc());
                        create.setInt(23, equip.getAvoid());
                        create.setInt(24, equip.getHands());
                        create.setInt(25, equip.getSpeed());
                        create.setInt(26, equip.getJump());
                        create.setInt(27, equip.getVicious());
                        create.setInt(28, equip.getPotential_1());
                        create.setInt(29, equip.getPotential_2());
                        create.setInt(30, equip.getPotential_3());
                        create.setInt(31, equip.getIdentify());
                        create.setInt(32, equip.getIdentified());
                        create.setInt(33, equip.getStarlevel());
                        create.setInt(34, equip.getItemLevel());
                        create.setInt(35, equip.getItemExp());
                        create.setInt(36, equip.getItemSkill());
                        create.setInt(37, equip.getDurability());
                        create.setInt(38, equip.getPvpWatk());
                        create.setTimestamp(39, equip.getUnlockTime());
                    } else {
                        for (int i = 10; i < 39; i++) {
                            create.setNull(i, java.sql.Types.INTEGER);
                        }
                        create.setNull(39, Types.DATE);
                    }
                    if (iItem instanceof MapleRing) {
                        MapleRing mapleRing = (MapleRing) iItem;
                        create.setLong(40, mapleRing.getPartnerUniqueId());
                        create.setInt(41, mapleRing.getPartnerChrId());
                        create.setString(42, mapleRing.getPartnerName());
                    } else {
                        create.setNull(40, Types.BIGINT);
                        create.setNull(41, Types.INTEGER);
                        create.setNull(42, Types.VARCHAR);
                    }
                    if (iItem instanceof MaplePet) {
                        MaplePet maplePet = (MaplePet) iItem;
                        create.setString(43, maplePet.getName());
                        create.setInt(44, maplePet.getLevel());
                        create.setInt(45, maplePet.getCloseness());
                        create.setInt(46, maplePet.getFullness());
                        create.setInt(47, maplePet.getSlot() + 1);
                    } else {
                        create.setNull(43, java.sql.Types.VARCHAR);
                        create.setNull(44, java.sql.Types.INTEGER);
                        create.setNull(45, java.sql.Types.INTEGER);
                        create.setNull(46, java.sql.Types.INTEGER);
                        create.setNull(47, java.sql.Types.INTEGER);
                    }

                    if (mapleItemsNameSpace.GetSpaceType().equals(
                            MapleItemsNameSpaceType.CsInventory)) {
                        Item tItem = (Item) iItem;
                        create.setInt(48, iItem.getSN());
                        create.setString(49, tItem.getSender());
                        create.setString(50, tItem.getMessage());
                    } else {
                        create.setNull(48, java.sql.Types.INTEGER);
                        create.setNull(49, java.sql.Types.VARCHAR);
                        create.setNull(50, java.sql.Types.VARCHAR);
                    }

                    switch (mapleItemsNameSpace.GetSpaceType()) {
                        case Inventory:
                            create.setInt(51, -1);
                            create.setInt(52, ((MapleInventory) mapleItemsNameSpace).getType().type);
                            break;
                        case Storages:
                            create.setInt(51, accountid);
                            create.setInt(52, 0);
                            break;
                        case CsInventory:
                            create.setInt(51, -2);
                            create.setInt(52, 0);
                            break;
                    }
                    create.addBatch();
                }
            }
            create.executeBatch();
            create.close();

            /*
             ps = con.prepareStatement("INSERT INTO items (itemid, position, quantity, owner, expiredate, uniqueid, flag, log) VALUES (?, ?, ?, ?, ?, ?, ?, ?)");
             PreparedStatement pse = con.prepareStatement("INSERT INTO items_equip VALUES "
             + "(?, ?, ?, ?, ?, ?, ?, ?, ?, ?, "
             + "?, ?, ?, ?, ?, ?, ?, ?, ?, ?, "
             + "?, ?, ?, ?, ?, ?, ?, ?, ?, ?, " + "?)");
             PreparedStatement ps_cs = con.prepareStatement("INSERT INTO items_cs VALUES (?, ?, ?, ?)");
             PreparedStatement ps_pet = con.prepareStatement("INSERT INTO items_pet VALUES (?, ?, ?, ?, ?, ?)");
             PreparedStatement ps_ring = con.prepareStatement("INSERT INTO items_ring VALUES (?, ?, ?, ?)");
             PreparedStatement ps_in_info = con.prepareStatement("INSERT INTO `items_inventoryinfo` VALUES (?, ?, ?)"),
             ps_storage_info = con.prepareStatement("INSERT INTO `items_storageinfo` VALUES (?, ?)"),
             ps_csin_info = con.prepareStatement("INSERT INTO `items_csinventoryinfo` VALUES (?, ?)");
             ps_in_info.setInt(1, id);
             ps_storage_info.setInt(1, accountid);
             ps_csin_info.setInt(1, id);
             for (MapleItemsNameSpace mapleItemsNameSpace : GetSaveNameSpaces()) {
             if (mapleItemsNameSpace == null) {
             continue;
             }
             for (IItem iItem : mapleItemsNameSpace.AllItems()) {
             ps.setInt(1, iItem.getItemId());
             ps.setInt(2, iItem.getPosition());
             ps.setInt(3, iItem.getQuantity());
             ps.setString(4, iItem.getOwner());
             ps.setTimestamp(5, iItem.getExpiration());
             if (MapleItemInformationProvider.getInstance().isCash(
             iItem.getItemId())) {
             if (iItem.getUniqueid() == 0) {
             iItem.setUniqueId(getNextUniqueId());
             }
             } else {
             if (iItem.getItemId() / 10000 != 166) {
             iItem.setUniqueId(0);
             }
             }
             ps.setLong(6, iItem.getUniqueid());
             ps.setInt(7, iItem.getFlag());
             ps.setString(8, iItem.getLog().toString());
             int rows = ps.executeUpdate();
             ResultSet rs = ps.getGeneratedKeys();
             long itemsI_id = -1;
             if (rs.next()) {
             itemsI_id = rs.getLong(1);
             } else {
             throw new DatabaseException("Inserting char failed.");
             }
             if (iItem instanceof Equip) {
             Equip equip = (Equip) iItem;
             pse.setLong(1, itemsI_id);
             pse.setInt(2, equip.getUpgradeSlots());
             pse.setInt(3, equip.getLevel());
             pse.setInt(4, equip.getStr());
             pse.setInt(5, equip.getDex());
             pse.setInt(6, equip.getInt());
             pse.setInt(7, equip.getLuk());
             pse.setInt(8, equip.getHp());
             pse.setInt(9, equip.getMp());
             pse.setInt(10, equip.getWatk());
             pse.setInt(11, equip.getMatk());
             pse.setInt(12, equip.getWdef());
             pse.setInt(13, equip.getMdef());
             pse.setInt(14, equip.getAcc());
             pse.setInt(15, equip.getAvoid());
             pse.setInt(16, equip.getHands());
             pse.setInt(17, equip.getSpeed());
             pse.setInt(18, equip.getJump());
             pse.setInt(19, equip.getVicious());
             pse.setInt(20, equip.getPotential_1());
             pse.setInt(21, equip.getPotential_2());
             pse.setInt(22, equip.getPotential_3());
             pse.setInt(23, equip.getIdentify());
             pse.setInt(24, equip.getIdentified());
             pse.setInt(25, equip.getStarlevel());
             pse.setInt(26, equip.getItemLevel());
             pse.setInt(27, equip.getItemExp());
             pse.setInt(28, equip.getItemSkill());
             pse.setInt(29, equip.getDurability());
             pse.setInt(30, equip.getPvpWatk());
             pse.setTimestamp(31, equip.getUnlockTime());
             pse.addBatch();
             }
             if (mapleItemsNameSpace.GetSpaceType().equals(
             MapleItemsNameSpaceType.CsInventory)) {
             Item tItem = (Item) iItem;
             ps_cs.setLong(1, itemsI_id);
             ps_cs.setInt(2, iItem.getSN());
             ps_cs.setString(3, tItem.getSender());
             ps_cs.setString(4, tItem.getMessage());
             ps_cs.addBatch();
             }
             if (iItem instanceof MaplePet) {
             MaplePet maplePet = (MaplePet) iItem;
             ps_pet.setLong(1, itemsI_id);
             ps_pet.setString(2, maplePet.getName());
             ps_pet.setInt(3, maplePet.getLevel());
             ps_pet.setInt(4, maplePet.getCloseness());
             ps_pet.setInt(5, maplePet.getFullness());
             ps_pet.setInt(6, maplePet.getSlot() + 1);
             ps_pet.addBatch();
             }
             if (iItem instanceof MapleRing) {
             MapleRing mapleRing = (MapleRing) iItem;
             ps_ring.setLong(1, itemsI_id);
             ps_ring.setLong(2, mapleRing.getPartnerUniqueId());
             ps_ring.setInt(3, mapleRing.getPartnerChrId());
             ps_ring.setString(4, mapleRing.getPartnerName());
             ps_ring.addBatch();
             }
             switch (mapleItemsNameSpace.GetSpaceType()) {
             case Inventory:
             ps_in_info.setLong(2, itemsI_id);
             ps_in_info.setInt(3,
             ((MapleInventory) mapleItemsNameSpace).getType().type);
             ps_in_info.addBatch();
             break;
             case Storages:
             ps_storage_info.setLong(2, itemsI_id);
             ps_storage_info.addBatch();
             break;
             case CsInventory:
             ps_csin_info.setLong(2, itemsI_id);
             ps_csin_info.addBatch();
             break;
             }
             }
             }
             ps.close();
             pse.executeBatch();
             pse.close();
             ps_cs.executeBatch();
             ps_cs.close();
             ps_pet.executeBatch();
             ps_pet.close();
             ps_ring.executeBatch();
             ps_ring.close();

             ps_in_info.executeBatch();
             ps_storage_info.executeBatch();
             ps_csin_info.executeBatch();
             ps_in_info.close();
             ps_storage_info.close();
             ps_csin_info.close();*/
            deleteWhereCharacterId(con,
                    "DELETE FROM queststatus WHERE characterid = ?");
            ps = con.prepareStatement(
                    "INSERT INTO queststatus (`queststatusid`, `characterid`, `quest`, `status`, `time`, `forfeited`) VALUES (DEFAULT, ?, ?, ?, ?, ?)",
                    Statement.RETURN_GENERATED_KEYS);
            PreparedStatement pse_ = con.prepareStatement("INSERT INTO queststatusmobs VALUES (?, ?, ?)");
            ps.setInt(1, id);
            for (MapleQuestStatus q : quests.values()) {
                ps.setInt(2, q.getQuest().getId());
                ps.setInt(3, q.getStatus().getId());
                ps.setInt(4, (int) (q.getCompletionTime() / 1000));
                ps.setInt(5, q.getForfeited());
                ps.executeUpdate();
                ResultSet rs = ps.getGeneratedKeys();
                rs.next();
                for (int mob : q.getMobKills().keySet()) {
                    pse_.setInt(1, rs.getInt(1));
                    pse_.setInt(2, mob);
                    pse_.setInt(3, q.getMobKills(mob));
//                    pse.executeUpdate();
                }
                rs.close();
            }
            ps.close();
            pse_.close();

            deleteWhereCharacterId(con,
                    "DELETE FROM skills WHERE characterid = ?");
            ps = con.prepareStatement("INSERT INTO skills VALUES (?, ?, ?, ?, ?)");
            ps.setInt(2, id);
            for (Entry<ISkill, SkillEntry> skill : skills.entrySet()) {
                ps.setInt(1, skill.getKey().getId());
                ps.setInt(3, skill.getValue().skillevel);
                ps.setInt(4, skill.getValue().masterlevel);
                ps.setTimestamp(5, skill.getValue().expiredate);
                ps.executeUpdate();
            }
            ps.close();

            deleteWhereCharacterId(con,
                    "DELETE FROM keymap WHERE characterid = ?");
            ps = con.prepareStatement("INSERT INTO keymap (characterid, `key`, `type`, `action`) VALUES (?, ?, ?, ?)");
            ps.setInt(1, id);
            for (Entry<Integer, MapleKeyBinding> keybinding : keymap.entrySet()) {
                ps.setInt(2, keybinding.getKey().intValue());
                ps.setInt(3, keybinding.getValue().getType());
                ps.setInt(4, keybinding.getValue().getAction());
                ps.executeUpdate();
            }
            ps.close();

            deleteWhereCharacterId(con,
                    "DELETE FROM savedlocations WHERE characterid = ?");
            ps = con.prepareStatement("INSERT INTO savedlocations (characterid, `locationtype`, `map`) VALUES (?, ?, ?)");
            ps.setInt(1, id);
            for (SavedLocationType savedLocationType : SavedLocationType.values()) {
                if (savedLocations[savedLocationType.ordinal()] != -1) {
                    ps.setString(2, savedLocationType.name());
                    ps.setInt(3, savedLocations[savedLocationType.ordinal()]);
                    ps.executeUpdate();
                }
            }
            ps.close();

            deleteWhereCharacterId(con,
                    "DELETE FROM buddies WHERE characterid = ? AND pending = 0");
            ps = con.prepareStatement("INSERT INTO buddies (characterid, `buddyid`, `group`, `pending`) VALUES (?, ?, ?, 0)");
            ps.setInt(1, id);
            for (BuddylistEntry entry : buddylist.getBuddies()) {
                if (entry.isVisible()) {
                    ps.setInt(2, entry.getCharacterId());
                    ps.setString(3, entry.getGroup());
                    ps.executeUpdate();
                }
            }
            ps.close();

            ps = con.prepareStatement("UPDATE accounts SET `paypalNX` = ?, `mPoints` = ?, `cardNX` = ?, `money` = ?, Present = ? WHERE id = ?");
            ps.setInt(1, paypalnx);
            ps.setInt(2, maplepoints);
            ps.setInt(3, cardnx);
            ps.setInt(4, money);
            ps.setInt(5, Present);
            ps.setInt(6, client.getAccID());
            ps.executeUpdate();
            ps.close();

            if (update) {
                ps = con.prepareStatement("DELETE FROM achievements WHERE accountid = ?");
                ps.setInt(1, accountid);
                ps.executeUpdate();
                ps.close();

                for (Integer achid : finishedAchievements) {
                    ps = con.prepareStatement("INSERT INTO achievements(charid, achievementid, accountid) VALUES(?, ?, ?)");
                    ps.setInt(1, id);
                    ps.setInt(2, achid);
                    ps.setInt(3, accountid);
                    ps.executeUpdate();
                    ps.close();
                }
            }

            deleteWhereCharacterId(con,
                    "DELETE FROM cooldowns WHERE characterid = ?");
            if (getAllCooldowns().size() > 0) {
                ps = con.prepareStatement("INSERT INTO cooldowns (characterid, skillid, starttime, length) VALUES (?, ?, ?, ?)");
                ps.setInt(1, getId());
                for (PlayerCoolDownValueHolder cooling : getAllCooldowns()) {
                    ps.setInt(2, cooling.skillId);
                    ps.setLong(3, cooling.startTime);
                    ps.setLong(4, cooling.length);
                    ps.executeUpdate();

                }
                ps.close();
            }

            con.commit();

        } catch (Exception e) {
            log.error(
                    MapleClient.getLogMessage(this, "[charsave] 角色数据保存数据库失败"),
                    e);
            try {
                con.rollback();
                // saveToDB(update);
            } catch (SQLException exception) {
                log.error(
                        MapleClient.getLogMessage(this, "[charsave] 被捕捉的回滚错误"),
                        e);
            }
        } finally {
            try {
                con.setAutoCommit(true);
                con.setTransactionIsolation(t);
                con.close();
            } catch (SQLException e) {
                log.error(MapleClient.getLogMessage(this,
                        "[charsave] Error going back to autocommit mode"), e);
            }
            if (update) {
                getDatabaseLock().unlock();
            }
        }
        if (update) {
            reloadOnlieTime();
        }
    }

    private void deleteWhereCharacterId(Connection con, String sql)
            throws SQLException {
        PreparedStatement ps = con.prepareStatement(sql);
        ps.setInt(1, id);
        ps.executeUpdate();
        ps.close();
    }

    public MapleQuestStatus getQuest(MapleQuest quest) {
        if (!quests.containsKey(quest)) {
            return new MapleQuestStatus(quest,
                    MapleQuestStatus.Status.NOT_STARTED);
        }
        return quests.get(quest);
    }

    public void updateQuest(MapleQuestStatus quest) {
        quests.put(quest.getQuest(), quest);
        if (quest.getQuest().getId() == 4760) {
            client.getSession().write(
                    MaplePacketCreator.completeQuest(this, (short) quest.getQuest().getId()));
            client.getSession().write(
                    MaplePacketCreator.updateQuestInfo(this, (short) quest.getQuest().getId(), quest.getNpc(), (byte) 10));
            getMap().broadcastMessage(this,
                    MaplePacketCreator.showSpecialEffect(0x0C), true);
        } else if (!(quest.getQuest() instanceof MapleCustomQuest)) {
            if (quest.getStatus().equals(MapleQuestStatus.Status.STARTED)) {// 入门...
                client.getSession().write(
                        MaplePacketCreator.startQuest(this, (short) quest.getQuest().getId()));
                client.getSession().write(MaplePacketCreator.updateQuestInfo(this,
                        (short) quest.getQuest().getId(),
                        quest.getNpc(), (byte) 10));
            } else if (quest.getStatus().equals(
                    MapleQuestStatus.Status.COMPLETED)) {// 已完成
                client.getSession().write(
                        MaplePacketCreator.completeQuest(this, (short) quest.getQuest().getId()));
            } else if (quest.getStatus().equals(
                    MapleQuestStatus.Status.NOT_STARTED)) {// 未开始
                client.getSession().write(
                        MaplePacketCreator.forfeitQuest(this, (short) quest.getQuest().getId()));
            }
        }
    }

    public static int getIdByName(String name, int world) {
        int result = -1;
        try {
            Connection con = DatabaseConnection.getConnection();
            PreparedStatement ps = con.prepareStatement("SELECT id FROM characters WHERE name = ? AND world = ?");
            ps.setString(1, name);
            ps.setInt(2, world);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                result = rs.getInt("id");
            }
            rs.close();
            ps.close();
            con.close();
        } catch (SQLException e) {
            log.error("ERROR", e);
        }
        return result;
    }

    public boolean isActiveBuffedValue(int skillid) {
        /*  LinkedList<MapleBuffStatValueHolder> allBuffs = new LinkedList<MapleBuffStatValueHolder>(
         effects.values());
         for (MapleBuffStatValueHolder mbsvh : allBuffs) {
         if (mbsvh.effect.isSkill() && mbsvh.effect.getSourceId() == skillid
         && !isGM()) {
         return true;
         }
         }
         return false;*/
        return buffManager.isActiveBuffedValue(skillid) && !isGM();
    }

    public Integer getBuffedValue(MapleBuffStat effect) {
        /*   MapleBuffStatValueHolder mbsvh = effects.get(effect);
         if (mbsvh == null) {
         return null;
         }
         return Integer.valueOf(mbsvh.value);*/
        return buffManager.getBuffedValue(effect);
    }

    public boolean isBuffFrom(MapleBuffStat stat, ISkill skill) {
        /*   MapleBuffStatValueHolder mbsvh = effects.get(stat);
         if (mbsvh == null) {
         return false;
         }
         return mbsvh.effect.isSkill()
         && mbsvh.effect.getSourceId() == skill.getId();*/
        return buffManager.isBuffFrom(stat, skill.getId());
    }

    public int getBuffSource(MapleBuffStat stat) {
        /*  MapleBuffStatValueHolder mbsvh = effects.get(stat);
         if (mbsvh == null) {
         return -1;
         }

         return mbsvh.effect.getSourceId();*/
        return buffManager.getBuffSource(stat);
    }

    public List<MapleStatEffect> getBuffEffects() {
        /*   ArrayList<MapleStatEffect> almseret = new ArrayList<MapleStatEffect>();
         HashSet<Integer> hs = new HashSet<Integer>();
         for (MapleBuffStatValueHolder mbsvh : effects.values()) {
         if (mbsvh != null && mbsvh.effect != null) {
         Integer nid = Integer.valueOf(mbsvh.effect.isSkill() ? mbsvh.effect.getSourceId() : -mbsvh.effect.getSourceId());
         if (!hs.contains(nid)) {
         almseret.add(mbsvh.effect);
         hs.add(nid);
         }
         }
         }
         return almseret;*/
        return buffManager.getBuffEffects();
    }

    public int getItemQuantity(int itemid, boolean checkEquipped) {
        MapleInventoryType type = MapleItemInformationProvider.getInstance().getInventoryType(itemid);
        MapleInventory iv = inventory[type.ordinal()];
        int possesed = iv.countById(itemid);
        if (checkEquipped) {
            possesed += inventory[MapleInventoryType.EQUIPPED.ordinal()].countById(itemid);
        }

        return possesed;
    }

    public void setBuffedValue(MapleBuffStat effect, int value) {
        /*     MapleBuffStatValueHolder mbsvh = effects.get(effect);
         if (mbsvh == null) {
         return;
         }
         mbsvh.value = value;*/
        buffManager.setBuffedValue(effect, value);
    }

    public Long getBuffedStarttime(MapleBuffStat effect) {
        /*  MapleBuffStatValueHolder mbsvh = effects.get(effect);
         if (mbsvh == null) {
         return null;
         }
         return Long.valueOf(mbsvh.startTime);*/
        return buffManager.getBuffedStarttime(effect);
    }

    public MapleStatEffect getStatForBuff(MapleBuffStat effect) {
        /*     MapleBuffStatValueHolder mbsvh = effects.get(effect);
         if (mbsvh == null) {
         return null;
         }
         return mbsvh.effect;*/
        return buffManager.getStatForBuff(effect);
    }

    private void prepareDragonBlood(final MapleStatEffect bloodEffect) {
        if (dragonBloodSchedule != null) {
            dragonBloodSchedule.cancel(false);
        }
        dragonBloodSchedule = TimerManager.getInstance().register(
                new Runnable() {
                    @Override
                    public void run() {
                        if (getHp() > bloodEffect.getX()) {
                            addHP(-bloodEffect.getX());
                            getClient().getSession().write(
                            MaplePacketCreator.龙之魂光效());
                            getMap().broadcastMessage(
                            MapleCharacter.this,
                            MaplePacketCreator.玩家的龙之魂(getId(), 1311008,
                            7, getSkillLevel(1311008)), false);
                        } else {
                            dragonBloodSchedule.cancel(false);
                            dragonBloodSchedule = null;
                        }
                    }
                }, 4000, 4000);
    }

    public MaplePet getPet(int index) {
        return pets[index];
    }

    public void addPet(MaplePet pet, boolean lead) {
        for (int i = 0; i < pets.length; i++) {
            if (pets[i] == null) {
                pet.setSlot(i);
                pets[i] = pet;
                break;
            }
        }
    }

    public void removePet(MaplePet pet) {
        int index = pet.getSlot();
        if (pets[index] != null) {
            pets[index].CancelFullnessSchedule();
            getClient().getSession().write(
                    MaplePacketCreator.updatePet(pets[index]));
            pets[index].setSlot(-1);
            pets[index] = null;
        }
        if (pet != null) {
            pet.CancelFullnessSchedule();
            pet.setSlot(-1);
        }
    }

    public int getNoPets() {
        // return pets.size();
        int i = 0;
        for (MaplePet maplePet : pets) {
            if (maplePet != null) {
                i++;
            }
        }
        return i;
    }

    public int getPetSlot(MaplePet pet) {
        if (pet != null) {
            for (int i = 0; i < pets.length; i++) {
                if (pets[i] != null
                        && pets[i].getUniqueid() == pet.getUniqueid()) {
                    return i;
                }
            }
        }
        return -1;
    }

    public int getPetByUniqueId(int uniqueid) {
        /*
         * for (int i = 0; i < pets.size(); i++) { if (pets.get(i) != null) { if
         * (pets.get(i).getUniqueId() == uniqueid) { return i; } } }
         */
        for (int i = 0; i < pets.length; i++) {
            MaplePet maplePet = pets[i];
            if (maplePet != null) {
                if (maplePet.getUniqueid() == uniqueid) {
                    return i;
                }
            }
        }
        return -1;
    }

    public MaplePet[] getPets() {
        return pets;
    }

    public List<MaplePet> getPetsList() {
        ArrayList<MaplePet> ___Pets = new ArrayList<MaplePet>();
        for (MaplePet maplePet : this.pets) {
            if (maplePet != null) {
                ___Pets.add(maplePet);
            }
        }
        return ___Pets;
    }

    public void unequipAllPets() {
        for (MaplePet pett : pets) {
            unequipPet(pett);
        }
    }

    public void unequipPet(MaplePet pet) {
        unequipPet(pet, false);
    }

    public void unequipPet(MaplePet pet, boolean hunger) {
        getMap().broadcastMessage(this,
                MaplePacketCreator.showPet(this, pet, true, hunger), true);
        removePet(pet);
        getClient().getSession().write(MaplePacketCreator.updatePet(pet));
        getClient().getSession().write(MaplePacketCreator.enableActions());
    }

    /*
     * public void MaxAllSkill() { if (this.job.NoBeginner()) { for (Integer
     * integer : SkillFactory.getSkills(this.job.getId())) {
     * changeSkillLevel(SkillFactory.getSkill(integer),
     * SkillFactory.getSkill(integer).getMaxLevel(), 0); } }
     *
     * }
     */
    public void startMapEffect(String msg, int itemId) {
        startMapEffect(msg, itemId, 30000);
    }

    public void startMapEffect(String msg, int itemId, int duration) {
        final MapleMapEffect mapEffect = new MapleMapEffect(msg, itemId);
        getClient().getSession().write(mapEffect.makeStartData());
        TimerManager.getInstance().schedule(new Runnable() {
            @Override
            public void run() {
                getClient().getSession().write(mapEffect.makeDestroyData());
            }
        }, duration);
    }

    public void startMapTimeLimitTask(final MapleMap from, final MapleMap to) {
        if (to.getTimeLimit() > 0 && from != null) {
            final MapleCharacter chr = this;
            mapTimeLimitTask = TimerManager.getInstance().register(
                    new Runnable() {
                        @Override
                        public void run() {
                            MaplePortal pfrom;
                            if (MapleItemInformationProvider.getInstance().isMiniDungeonMap(from.getId())) {
                                pfrom = from.getPortal("MD00");
                            } else {
                                pfrom = from.getPortal(0);
                            }
                            if (pfrom != null) {
                                chr.changeMap(from, pfrom);
                            }
                        }
                    }, from.getTimeLimit() * 1000, from.getTimeLimit() * 1000);
        }
    }

    public void cancelMapTimeLimitTask() {
        if (mapTimeLimitTask != null) {
            mapTimeLimitTask.cancel(false);
        }
    }

    public void registerEffect(MapleStatEffect effect, long starttime,
            ScheduledFuture<?> schedule, List<Pair<MapleBuffStat, Integer>> stat) {
        if (effect.isHide()) {
            this.hidden = true;
            getMap().broadcastMessage(this,
                    MaplePacketCreator.removePlayerFromMap(getId()), false);
        } else if (effect.isDragonBlood()) {
            // 龙之魂
            prepareDragonBlood(effect);
        } else if (effect.isBerserk()) {
            // checkBerserk();
        }
        /*
         * else if (effect.isBeholder()) { prepareBeholderEffect(); }
         */
        /*  for (Pair<MapleBuffStat, Integer> statup : effect.getStatups()) {
         effects.put(statup.getLeft(), new MapleBuffStatValueHolder(effect,
         starttime, schedule, statup.getRight().intValue()));
         }*/
        buffManager.registerEffect(effect, starttime, schedule, stat);
        recalcLocalStats();
    }

    public void removeAllCooldownsExcept(int id) {
        for (MapleCoolDownValueHolder mcvh : coolDowns.values()) {
            if (mcvh.skillId != id) {
                coolDowns.remove(mcvh.skillId);
            }
        }
    }

    private List<MapleBuffStat> getBuffStats(MapleStatEffect effect,
            long startTime) {
        /*  List<MapleBuffStat> stats = new ArrayList<MapleBuffStat>();
         try {
         for (Entry<MapleBuffStat, MapleBuffStatValueHolder> stateffect : effects.entrySet()) {
         MapleBuffStatValueHolder mbsvh = stateffect.getValue();
         if (mbsvh != null
         && (mbsvh.effect.sameSource(effect) && (startTime == -1 || startTime == mbsvh.startTime))) {
         stats.add(stateffect.getKey());
         }
         }
         } catch (ConcurrentModificationException e) {
         }

         return stats;*/
        return buffManager.getBuffStats(effect, startTime);
    }

    /*  private void deregisterBuffStats(List<MapleBuffStat> stats) {
     List<MapleBuffStatValueHolder> effectsToCancel = new ArrayList<MapleBuffStatValueHolder>(
     stats.size());
     for (MapleBuffStat stat : stats) {
     MapleBuffStatValueHolder mbsvh = effects.get(stat);
     if (mbsvh != null) {
     effects.remove(stat);
     boolean addMbsvh = true;
     for (MapleBuffStatValueHolder contained : effectsToCancel) {
     if (mbsvh.startTime == contained.startTime
     && contained.effect == mbsvh.effect) {
     addMbsvh = false;
     }
     }
     if (addMbsvh) {
     effectsToCancel.add(mbsvh);
     }

     }
     }
     for (MapleBuffStatValueHolder cancelEffectCancelTasks : effectsToCancel) {
     if (getBuffStats(cancelEffectCancelTasks.effect,
     cancelEffectCancelTasks.startTime).isEmpty()) {
     cancelEffectCancelTasks.schedule.cancel(false);
     }
     }
     }*/
    public void checkCancelBuffStat(List<MaplePlayerBuffManager.MapleBuffEffect> list) {
        for (MaplePlayerBuffManager.MapleBuffEffect mbe : list) {
            if (mbe.getStat() == MapleBuffStat.DRAGONBLOOD
                    && dragonBloodSchedule != null) {
                dragonBloodSchedule.cancel(false);
                dragonBloodSchedule = null;
            }
        }
    }

    /**
     * @param effect
     * @param overwrite when overwrite is set no data is sent and all the
     * Buffstats in the StatEffect are deregistered
     * @param startTime
     */
    public void cancelEffect(MapleStatEffect effect, boolean overwrite,
            long startTime) {
        List<MapleBuffStat> buffstats;
        if (!overwrite) {
            buffstats = getBuffStats(effect, startTime);
        } else {
            List<Pair<MapleBuffStat, Integer>> statups = effect.getStatups();
            buffstats = new ArrayList<MapleBuffStat>(statups.size());
            for (Pair<MapleBuffStat, Integer> statup : statups) {
                buffstats.add(statup.getLeft());
            }
        }
        //  deregisterBuffStats(buffstats);
        buffManager.deregisterBuffStats(buffManager.getBuff(effect));
        if (effect.isMagicDoor()) {
            // remove for all on maps
            if (!getDoors().isEmpty()) {
                MapleDoor door = getDoors().iterator().next();
                for (MapleCharacter chr : door.getTarget().getCharacters()) {
                    door.sendDestroyData(chr.getClient());
                }
                for (MapleCharacter chr : door.getTown().getCharacters()) {
                    door.sendDestroyData(chr.getClient());
                }
                for (MapleDoor destroyDoor : getDoors()) {
                    door.getTarget().removeMapObject(destroyDoor);
                    door.getTown().removeMapObject(destroyDoor);
                }
                clearDoors();
                silentPartyUpdate();
            }
        } else {
            int summonId = effect.getSourceId();
            MapleSummon summon = getSummon(summonId);
            if (summon != null) {
                // log.debug("在cancelBuff里取消召唤兽");
                getMap().broadcastMessage(
                        MaplePacketCreator.removeSpecialMapObject(summon, true));
                getMap().removeMapObject(summon);
                removeVisibleMapObject(summon);
                // getSummons().remove(summonId);
                removeSummon(summonId);
            }
        }

        if (effect.isMonsterRiding()) {
            useMount = null; // 取消骑宠
        }
        /*
         * if (summon.getSkill() == 1321007) { if (beholderHealingSchedule !=
         * null) { beholderHealingSchedule.cancel(false);
         * beholderHealingSchedule = null; } if (beholderBuffSchedule != null) {
         * beholderBuffSchedule.cancel(false); beholderBuffSchedule = null; }
         * private ScheduledFuture<?> 灵魂祝福物防; private ScheduledFuture<?> 灵魂祝福魔防;
         * private ScheduledFuture<?> 灵魂祝福回避; private ScheduledFuture<?> 灵魂祝福命中;
         * private ScheduledFuture<?> 灵魂祝福攻击;
         *
         * log.debug("取消灵魂祝福进程"); if (灵魂祝福物防 != null) { 灵魂祝福物防.cancel(false);
         * 灵魂祝福物防 = null; } if (灵魂祝福魔防 != null) { 灵魂祝福魔防.cancel(false); 灵魂祝福魔防 =
         * null; } if (灵魂祝福回避 != null) { 灵魂祝福回避.cancel(false); 灵魂祝福回避 = null; }
         * if (灵魂祝福命中 != null) { 灵魂祝福命中.cancel(false); 灵魂祝福命中 = null; } if
         * (灵魂祝福攻击 != null) { 灵魂祝福攻击.cancel(false); 灵魂祝福攻击 = null; } }
         */
        if (!overwrite) {
            // cancelPlayerBuffs(buffstats, effect.getSourceId(), effect.isRefreshstyle());
            cancelPlayerBuffs(buffstats, effect);
            if (effect.isHide()
                    && (MapleCharacter) getMap().getMapObject(getObjectId()) != null) {
                this.hidden = false;
                for (MapleCharacter mapleCharacter : getMap().getCharacters()) {
                    if (mapleCharacter != this && !mapleCharacter.isHidden()) {
                        sendSpawnData(mapleCharacter.getClient());
                    }
                }
            }
        }
    }

    public void cancelBuffStats(MapleBuffStat stat) {
        //    List<MapleBuffStat> buffStatList = Arrays.asList(stat);
        //    deregisterBuffStats(buffStatList);
        //  if (effects.containsKey(stat)) {
        //        cancelPlayerBuffs(buffStatList, 0, effects.get(stat).effect.isRefreshstyle());
        //    }
        ArrayList<MapleBuffStat> list = new ArrayList<MapleBuffStat>();
        MapleStatEffect effect = buffManager.deregisterBuffStats(buffManager.getBuff(stat), list);
        //  cancelPlayerBuffs(list, effect.getSourceId(), effect.isRefreshstyle());
        cancelPlayerBuffs(list, effect);
    }

    public void cancelEffectFromBuffStat(MapleBuffStat stat) {
        /* if (effects.get(stat) != null) {
         cancelEffect(effects.get(stat).effect, false, -1);
         }*/
        MaplePlayerBuffManager.MapleBuff buff = buffManager.getBuff(stat);
        if (buff != null) {
            cancelEffect(buff.getEffect(), false, -1);
        }
    }

    private void cancelPlayerBuffs(List<MapleBuffStat> buffstats, MapleStatEffect effect) {
        if (getClient().getChannelServer().getPlayerStorage().getCharacterById(getId()) != null) { // are we still connected
            // ?
            recalcLocalStats();
            enforceMaxHpMp();
            getClient().getSession().write(
                    MaplePacketCreator.cancelBuff(buffstats, effect.getSourceId()));
            if (effect.isRefreshstyle()) {
                getMap().broadcastMessage(
                        this,
                        MaplePacketCreator.cancelForeignBuff(getId(), buffstats,
                        this, effect), false);
            }
        }
    }

    /**
     * 清理玩家技能。
     */
    public void dispel() {
        /*     LinkedList<MapleBuffStatValueHolder> allBuffs = new LinkedList<MapleBuffStatValueHolder>(
         effects.values());
         for (MapleBuffStatValueHolder mbsvh : allBuffs) {
         if (mbsvh.effect.isSkill()) {
         cancelEffect(mbsvh.effect, false, mbsvh.startTime);
         }
         }*/
        buffManager.dispel();
    }

    public void cancelAllBuffs() {
        /*   LinkedList<MapleBuffStatValueHolder> allBuffs = new LinkedList<MapleBuffStatValueHolder>(
         effects.values());
         for (MapleBuffStatValueHolder mbsvh : allBuffs) {
         cancelEffect(mbsvh.effect, false, mbsvh.startTime);
         }*/
        buffManager.cancelAllBuffs();
    }

    public void cancelMorphs() {
        /*       LinkedList<MapleBuffStatValueHolder> allBuffs = new LinkedList<MapleBuffStatValueHolder>(
         effects.values());
         for (MapleBuffStatValueHolder mbsvh : allBuffs) {
         if (mbsvh.effect.isMorph() && mbsvh.effect.getSourceId() != 5111005
         && mbsvh.effect.getSourceId() != 5121003) {
         cancelEffect(mbsvh.effect, false, mbsvh.startTime);
         }
         }*/
        buffManager.cancelMorphs();
    }

    public void silentGiveBuffs(List<PlayerBuffValueHolder> buffs) {
        for (PlayerBuffValueHolder mbsvh : buffs) {
            mbsvh.effect.silentApplyBuff(this, mbsvh.startTime, mbsvh.getStat());
        }
    }

    public void giveItemBuff(int itemID) {
        MapleItemInformationProvider mii = MapleItemInformationProvider.getInstance();
        MapleStatEffect statEffect = mii.getItemEffect(itemID);
        if (statEffect != null) {
            statEffect.applyTo(this);
        }
    }

    public List<PlayerBuffValueHolder> getAllBuffs() {
        /*  List<PlayerBuffValueHolder> ret = new ArrayList<PlayerBuffValueHolder>();
         for (MapleBuffStatValueHolder mbsvh : effects.values()) {
         ret.add(new PlayerBuffValueHolder(mbsvh.startTime, mbsvh.effect));
         }
         return ret;*/
        return buffManager.getAllBuffs();
    }

    public void cancelMagicDoor() {
        /* LinkedList<MapleBuffStatValueHolder> allBuffs = new LinkedList<MapleBuffStatValueHolder>(
         effects.values());
         for (MapleBuffStatValueHolder mbsvh : allBuffs) {
         if (mbsvh.effect.isMagicDoor()) {
         cancelEffect(mbsvh.effect, false, mbsvh.startTime);
         }
         }*/
        buffManager.cancelMagicDoor();
    }

    /*
     * public void handleEnergyChargeGain() { ISkill energycharge =
     * SkillFactory.getSkill(5110001); int energyChargeSkillLevel =
     * getSkillLevel(energycharge); if (energyChargeSkillLevel <= 0) {
     * energycharge = SkillFactory.getSkill(15100004); energyChargeSkillLevel =
     * getSkillLevel(energycharge); } MapleStatEffect ceffect = null; ceffect =
     * energycharge.getEffect(energyChargeSkillLevel); TimerManager tMan =
     * TimerManager.getInstance(); if (energyChargeSkillLevel > 0) { if
     * (energybar < 10000) { energybar = (energybar + 102); if (energybar >
     * 10000) { energybar = 10000; }
     * getClient().getSession().write(MaplePacketCreator
     * .giveEnergyCharge(energybar));
     * getClient().getSession().write(MaplePacketCreator
     * .showOwnBuffEffect(energycharge.getId(), 2));
     * getMap().broadcastMessage(this, MaplePacketCreator.showBuffeffect(id,
     * energycharge.getId(), 2)); if (energybar == 10000) {
     * getMap().broadcastMessage(this,
     * MaplePacketCreator.giveForeignEnergyCharge(id, energybar)); } } if
     * (energybar >= 10000 && energybar < 11000) { energybar = 15000; final
     * MapleCharacter chr = this; tMan.schedule(new Runnable() {
     *
     * @Override public void run() {
     * getClient().getSession().write(MaplePacketCreator.giveEnergyCharge(0));
     * getMap().broadcastMessage(chr,
     * MaplePacketCreator.giveForeignEnergyCharge(id, energybar)); energybar =
     * 0; } }, ceffect.getDuration()); }
     *
     * } }
     */
    // 获得斗气
    public void handleOrbgain() {
        MapleStatEffect ceffect;
        int advComboSkillLevel = getSkillLevel(1120003); // 冒险家进阶斗气
        int 魂骑士进阶斗气等级 = getSkillLevel(11110005);
        if (advComboSkillLevel > 0) {
            ceffect = SkillFactory.getSkill(1120003).getEffect(
                    advComboSkillLevel);
        } else if (魂骑士进阶斗气等级 > 0) { // 骑士团进阶斗气
            advComboSkillLevel = 魂骑士进阶斗气等级;
            ceffect = SkillFactory.getSkill(11110005).getEffect(
                    advComboSkillLevel);
        } else { // 无进阶斗气
            int 斗气集中等级 = getSkillLevel(1111002);
            if (斗气集中等级 > 0) {
                ceffect = SkillFactory.getSkill(1111002).getEffect(斗气集中等级);
            } else {
                斗气集中等级 = getSkillLevel(11111001);
                ceffect = SkillFactory.getSkill(11111001).getEffect(斗气集中等级);
            }
        }
        // 因为buff的值为1的时候是无斗气的 满斗气的话Buff的值是6 但是x节点的最大值是5 所以要+1
        if (getBuffedValue(MapleBuffStat.COMBO) < ceffect.getX() + 1) {
            int neworbcount = getBuffedValue(MapleBuffStat.COMBO) + 1; // 递增1
            if (advComboSkillLevel > 0 && ceffect.makeChanceResult()) {
                // 有几率获得2个斗气
                if (neworbcount < ceffect.getX() + 1) {
                    neworbcount++; // 如果有进阶斗气就满足makeChanceResult函数[即存在prop节点]
                    // 有几率一次增加2个气
                }
            }
            List<Pair<MapleBuffStat, Integer>> stat = Collections.singletonList(new Pair<MapleBuffStat, Integer>(
                    MapleBuffStat.COMBO, neworbcount));
            setBuffedValue(MapleBuffStat.COMBO, neworbcount);
            int duration = ceffect.getDuration();
            duration += (int) ((getBuffedStarttime(MapleBuffStat.COMBO) - System.currentTimeMillis())); // 时间递减
            int effectlevel = getSkillLevel(1111002);
            if (effectlevel > 0) {
                getClient().getSession().write(
                        MaplePacketCreator.giveBuff(1111002, duration, stat, this));
            } else {
                effectlevel = getSkillLevel(11111001);
                if (effectlevel > 0) {
                    getClient().getSession().write(
                            MaplePacketCreator.giveBuff(11111001, duration,
                            stat, this));
                }
            }
            // 前面的参数 this 是在给地图可视范围内的人看到自己的斗气状态 但是giveForeignBuff不作用于自己身上
            getMap().broadcastMessage(this,
                    MaplePacketCreator.giveForeignBuff(this, stat, ceffect),
                    false);
        }
    }

    // 消耗斗气
    public void handleOrbconsume() {
        ISkill Bcombo = SkillFactory.getSkill(1111002); // 勇士的斗气
        if (getSkillLevel(Bcombo) == 0) {
            Bcombo = SkillFactory.getSkill(11111001); // 魂骑士的斗气
        }
        int Cskillid = Bcombo.getId();
        MapleStatEffect ceffect = Bcombo.getEffect(getSkillLevel(Bcombo));
        List<Pair<MapleBuffStat, Integer>> stat = Collections.singletonList(new Pair<MapleBuffStat, Integer>(
                MapleBuffStat.COMBO, 1));
        setBuffedValue(MapleBuffStat.COMBO, 1); // buff.right = 气 + 1;
        int duration = ceffect.getDuration();
        duration += (int) (getBuffedStarttime(MapleBuffStat.COMBO) - System.currentTimeMillis()); // 对时间进行递减
        if (Cskillid == 11111001) // 修改 原来是根据skilllevel判断
        {
            getClient().getSession().write(
                    MaplePacketCreator.giveBuff(11111001, duration, stat, this));
        } else {
            getClient().getSession().write(
                    MaplePacketCreator.giveBuff(1111002, duration, stat, this));
        }

        getMap().broadcastMessage(this,
                MaplePacketCreator.giveForeignBuff(this, stat, ceffect), false);
    }

    private void silentEnforceMaxHpMp() {
        setMp(getMp());
        setHp(getHp(), true);
    }

    private void enforceMaxHpMp() {
        List<Pair<MapleStat, Number>> stats = new ArrayList<Pair<MapleStat, Number>>(
                2);
        if (getMp() > getCurrentMaxMp()) {
            setMp(getMp());
            stats.add(new Pair<MapleStat, Number>(MapleStat.MP, Integer.valueOf(getMp())));
        }
        if (getHp() > getCurrentMaxHp()) {
            setHp(getHp());
            stats.add(new Pair<MapleStat, Number>(MapleStat.HP, Integer.valueOf(getHp())));
        }
        if (stats.size() > 0) {
            getClient().getSession().write(
                    MaplePacketCreator.updatePlayerStats(stats, this));
        }
    }

    public MapleMap getMap() {
        return map;
    }

    /**
     * only for tests
     *
     * @param newmap
     */
    public void setMap(MapleMap newmap) {
        this.map = newmap;
    }

    public int getMapId() {
        if (map != null) {
            return map.getId();
        }
        return mapid;
    }

    public int getInitialSpawnPoint() {
        return initialSpawnPoint;
    }

    public void setInitialSpawnPoint(int value) {
        this.initialSpawnPoint = value;
    }

    public List<LifeMovementFragment> getLastRes() {
        return this.lastres;
    }

    public void setLastRes(List<LifeMovementFragment> lastres) {
        this.lastres = lastres;
    }

    public int getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public int getLevel() {
        return level;
    }

    // money
    public int GetMoney() {
        /*
         * int getvip1 = 0; try { Connection con =
         * DatabaseConnection.getConnection();
         *
         * PreparedStatement ps = con.prepareStatement("SELECT money from
         * accounts where id=?"); ps.setInt(1, this.getClient().getAccID());
         * ResultSet rs = ps.executeQuery(); if (rs.next()) { getvip1 =
         * rs.getInt("money"); }
         *
         * ps.close(); rs.close(); } catch (SQLException e) {
         * e.printStackTrace(); } this.money = (byte) getvip1;
         */
        return this.money;
    }

    /**
     * 函数已屏蔽。
     *
     * @return
     */
    public int getVip() {
        /*
         * int getvip1 = 0; try { Connection con =
         * DatabaseConnection.getConnection();
         *
         * PreparedStatement ps = con.prepareStatement("SELECT vip from
         * characters where id=?"); ps.setInt(1, getId()); ResultSet rs =
         * ps.executeQuery(); if (rs.next()) { getvip1 = rs.getInt("vip"); }
         *
         * ps.close(); rs.close(); } catch (SQLException e) {
         * e.printStackTrace(); } this.vip = (byte) getvip1; return this.vip;
         */
        return this.vip;
    }

    public int getShuoHua() {
        /*
         * int getvip1 = 0; try { Connection con =
         * DatabaseConnection.getConnection();
         *
         * PreparedStatement ps = con.prepareStatement("SELECT ShuoHua from
         * characters where id=?"); ps.setInt(1, getId()); ResultSet rs =
         * ps.executeQuery(); if (rs.next()) { getvip1 = rs.getInt("ShuoHua"); }
         *
         * ps.close(); rs.close(); } catch (SQLException e) {
         * e.printStackTrace(); } this.ShuoHua = getvip1;
         */
        return this.ShuoHua;
    }

    public void doReborns() {
        reborns++;
    }

    public int getReborns() {
        /*
         * int getvip1 = 0; try { Connection con =
         * DatabaseConnection.getConnection();
         *
         * PreparedStatement ps = con.prepareStatement("SELECT reborns from
         * characters where id=?"); ps.setInt(1, getId()); ResultSet rs =
         * ps.executeQuery(); if (rs.next()) { getvip1 = rs.getInt("reborns"); }
         *
         * ps.close(); rs.close(); } catch (SQLException e) {
         * e.printStackTrace(); } this.reborns = getvip1;
         */
        return this.reborns;
    }

    public int getSD() {
        /*
         * int getvip1 = 0; try { Connection con =
         * DatabaseConnection.getConnection();
         *
         * PreparedStatement ps = con.prepareStatement("SELECT SD from
         * characters where id=?"); ps.setInt(1, getId()); ResultSet rs =
         * ps.executeQuery(); if (rs.next()) { getvip1 = rs.getInt("SD"); }
         *
         * ps.close(); rs.close(); } catch (SQLException e) {
         * e.printStackTrace(); } this.SD = getvip1;
         */
        return this.SD;
    }

    public void setVip(int k) {
        /*
         * Connection con = DatabaseConnection.getConnection(); try {
         * PreparedStatement ps = con.prepareStatement("UPDATE `characters` SET
         * `vip` = ? where id = ?"); ps.setInt(1, k); ps.setInt(2,
         * this.getId()); ps.executeUpdate(); ps.close(); } catch (Exception e)
         * { e.printStackTrace(); }
         */
        this.vip = k;
    }

    public void Setfs(int k) {
        /*
         * Connection con = DatabaseConnection.getConnection(); try {
         * PreparedStatement ps = con.prepareStatement("UPDATE `characters` SET
         * `fs` = ? where id = ?"); ps.setInt(1, k); ps.setInt(2, this.getId());
         * ps.executeUpdate(); ps.close(); } catch (Exception e) {
         * e.printStackTrace(); }
         */
        this.fs = k;
    }

    public void GainVip(int k) {
        /*
         * Connection con = DatabaseConnection.getConnection(); try {
         * PreparedStatement ps = con.prepareStatement("UPDATE `characters` SET
         * `vip` = ? where id = ?"); ps.setInt(1, this.vip + k); ps.setInt(2,
         * this.getId()); ps.executeUpdate(); ps.close(); } catch (Exception e)
         * { e.printStackTrace(); }
         */
        this.vip += k;
    }

    public void setShuoHua(int k) {
        /*
         * Connection con = DatabaseConnection.getConnection(); try {
         * PreparedStatement ps = con.prepareStatement("UPDATE `characters` SET
         * `shuohua` = ? where id = ?"); ps.setInt(1, k); ps.setInt(2,
         * this.getId()); ps.executeUpdate(); ps.close(); } catch (Exception e)
         * { e.printStackTrace(); }
         */
        this.ShuoHua = k;
    }

    public void GainShuoHua(int k) {
        /*
         * Connection con = DatabaseConnection.getConnection(); try {
         * PreparedStatement ps = con.prepareStatement("UPDATE `characters` SET
         * `shuohua` = ? where id = ?"); ps.setInt(1, this.ShuoHua + k);
         * ps.setInt(2, this.getId()); ps.executeUpdate(); ps.close(); } catch
         * (Exception e) { e.printStackTrace(); }
         */
        this.ShuoHua += k;
    }

    public void SetMoney(int k) {
        /*
         * Connection con = DatabaseConnection.getConnection(); try {
         * PreparedStatement ps = con.prepareStatement("UPDATE `accounts` SET
         * `money` = ? where id = ?"); ps.setInt(1, k); ps.setInt(2,
         * this.getClient().getAccID()); ps.executeUpdate(); ps.close(); } catch
         * (Exception e) { e.printStackTrace(); }
         */
        this.money = k;
    }

    public void GainMoney(int k) {
        /*
         * Connection con = DatabaseConnection.getConnection(); try {
         * PreparedStatement ps = con.prepareStatement("UPDATE `accounts` SET
         * `money` = ? where id = ?"); ps.setInt(1, this.money + k);
         * ps.setInt(2, this.getClient().getAccID()); ps.executeUpdate();
         * ps.close(); } catch (Exception e) { e.printStackTrace(); }
         */
        this.money += k;
    }

    public void setReborns(int k) {
        /*
         * Connection con = DatabaseConnection.getConnection(); try {
         * PreparedStatement ps = con.prepareStatement("UPDATE `characters` SET
         * `reborns` = ? where id = ?"); ps.setInt(1, k); ps.setInt(2,
         * this.getId()); ps.executeUpdate(); ps.close(); } catch (Exception e)
         * { e.printStackTrace(); }
         */
        this.reborns = k;
    }

    public void GainReborns(int k) {
        /*
         * Connection con = DatabaseConnection.getConnection(); try {
         * PreparedStatement ps = con.prepareStatement("UPDATE `characters` SET
         * `reborns` = ? where id = ?"); ps.setInt(1, this.reborns + k);
         * ps.setInt(2, this.getId()); ps.executeUpdate(); ps.close(); } catch
         * (Exception e) { e.printStackTrace(); }
         */
        this.reborns += k;
    }

    public void setSD(int k) {
        /*
         * Connection con = DatabaseConnection.getConnection(); try {
         * PreparedStatement ps = con.prepareStatement("UPDATE `characters` SET
         * `SD` = ? where id = ?"); ps.setInt(1, k); ps.setInt(2, this.getId());
         * ps.executeUpdate(); ps.close(); } catch (Exception e) {
         * e.printStackTrace(); }
         */
        this.SD = k;
    }

    public void GainSD(int k) {
        /*
         * Connection con = DatabaseConnection.getConnection(); try {
         * PreparedStatement ps = con.prepareStatement("UPDATE `characters` SET
         * `SD` = ? where id = ?"); ps.setInt(1, this.SD + k); ps.setInt(2,
         * this.getId()); ps.executeUpdate(); ps.close(); } catch (Exception e)
         * { e.printStackTrace(); }
         */
        this.SD += k;
    }

    public void setZhongJi(int k) {
        /*
         * Connection con = DatabaseConnection.getConnection(); try {
         * PreparedStatement ps = con.prepareStatement("UPDATE `characters` SET
         * `ZhongJi` = ? where id = ?"); ps.setInt(1, k); ps.setInt(2,
         * this.getId()); ps.executeUpdate(); ps.close(); } catch (Exception e)
         * { e.printStackTrace(); }
         */
        this.ZhongJi = k;
    }

    public void GainZhongJi(int k) {
        /*
         * Connection con = DatabaseConnection.getConnection(); try {
         * PreparedStatement ps = con.prepareStatement("UPDATE `characters` SET
         * `ZhongJi` = ? where id = ?"); ps.setInt(1, this.ZhongJi + k);
         * ps.setInt(2, this.getId()); ps.executeUpdate(); ps.close(); } catch
         * (Exception e) { e.printStackTrace(); }
         */
        this.ZhongJi += k;
    }

    public int getDojoEnergy() {
        return dojoEnergy;
    }

    public int getDojoPoints() {
        return dojoPoints;
    }

    public int getLastDojoStage() {
        return lastDojoStage;
    }

    public int getRank() {
        return rank;
    }

    public void setRank(int value) {
        this.rank = value;
    }

    public int getRankMove() {
        return rankMove;
    }

    private void setRankMove(int value) {
        this.rankMove = value;
    }

    public void setfsbLog(String boss) {
        try {
            Connection con = DatabaseConnection.getConnection();
            PreparedStatement ps;
            ps = con.prepareStatement("insert into bosslog (characterid, bossid) values (?,?)");
            ps.setInt(1, id);
            ps.setString(2, boss);
            ps.executeUpdate();
            ps.close();
            con.close();
        } catch (Exception Ex) {
            log.error("Error while insert bosslog.", Ex);
        }
    }

    public int getfs() {
        return fs;
    }

    public void setfs(int fs) {
        this.fs = fs;
    }

    public String getFSString() {
        if (getfs() > 0 && getfs() <= GameConstants.fsStrings.length) {
            return GameConstants.fsStrings[getfs() - 1];
        } else {
            return "[凡人]";
        }
    }

    public int getjh() {
        return jh;
    }

    public int getJobRank() {
        return jobRank;
    }

    private void setJobRank(int value) {
        this.jobRank = value;
    }

    public int getJobRankMove() {
        return jobRankMove;
    }

    private void setJobRankMove(int value) {
        this.jobRankMove = value;
    }

    public int getJobType() {
        int a = 0;
        if (isKnights()) {
            a = 10000000;
        } else if (isAran()) {
            a = 20000000;
        } else if (isEvan()) {
            a = 20010000;
        } else if (isResistance()) {
            a = 30000000;
        }
        return a;
    }

    public int getAPQScore() {
        return APQScore;
    }

    public int getFame() {
        return fame;
    }

    public MapleFamilyEntry getFamily() {
        return MapleFamily.getMapleFamily(this);
    }

    public int getFamilyId() {
        return familyId;
    }

    public ArrayList<Integer> getExcluded() {
        return excluded;
    }

    public int getStr() {
        return str;
    }

    public int getStr_() {
        return str;
    }

    public int getDex() {
        return dex;
    }

    public int getLuk() {
        return luk;
    }

    public int getInt() {
        return int_;
    }

    public int getInt_() {
        return int_;
    }

    public MapleClient getClient() {
        return client;
    }

    public void SendPacket(MaplePacket packet) {
        getClient().getSession().write(packet);
    }

    public long getExp() {
        return exp.get();
    }

    public int getHp() {
        return hp;
    }

    public int getHp_() {
        return hp;
    }

    public int getMaxhp() {
        return maxhp;
    }

    public int getMaxhp_() {
        return maxhp;
    }

    public int getMp() {
        return mp;
    }

    public int getMp_() {
        return mp;
    }

    public int getMaxmp() {
        return maxmp;
    }

    public int getMaxmp_() {
        return maxmp;
    }

    public int getRemainingAp() {
        return remainingAp;
    }

    public int getRemainingSp() {
        return this.remainingsp[0 > this.job.GetMaxSpSlots() ? 0 : this.job.GetMaxSpSlots()];
    }

    public int getRemainingSp(int index) {
        return remainingsp[index];
    }

    public int getMpApUsed() {
        return mpApUsed;
    }

    public void setMpApUsed(int mpApUsed) {
        this.mpApUsed = mpApUsed;
    }

    public int getHpApUsed() {
        return hpApUsed;
    }

    public boolean isHidden() {
        return hidden;
    }

    public void setDojoEnergy(int x) {
        this.dojoEnergy = x;
    }

    public void setDojoPoints(int x) {
        this.dojoPoints = x;
    }

    public void setLastDojoStage(int x) {
        this.lastDojoStage = x;
    }

    public void setDojoStart() {
        int stage = (map.getId() / 100) % 100;
        this.dojoFinish = System.currentTimeMillis()
                + ((stage > 36 ? 15 : stage / 6 + 5)) * 60000;
    }

    public void setFinishedDojoTutorial() {
        this.finishedDojoTutorial = true;
    }

    public void setHidden(boolean hidden) {
        this.hidden = hidden;
    }

    public void setHpApUsed(int hpApUsed) {
        this.hpApUsed = hpApUsed;
    }

    public MapleSkinColor getSkinColor() {
        return skinColor;
    }

    public MapleJob getJob() {
        return job;
    }

    public int getGender() {
        return gender;
    }

    public int getHair() {
        return hair;
    }

    public int getFace() {
        return face;
    }

    public void setFamilyId(int familyId) {
        this.familyId = familyId;
    }

    public void setStr(int str) {
        this.str = str;
        recalcLocalStats();
    }

    public void setStr_(int str) {
        this.str = str;
    }

    public void setDex(int dex) {
        this.dex = dex;
        recalcLocalStats();
    }

    public void setDex_(int dex) {
        this.dex = dex;
    }

    public int getDex_() {
        return this.dex;
    }

    public void setLuk(int luk) {
        this.luk = luk;
        recalcLocalStats();
    }

    public void setLuk_(int luk) {
        this.luk = luk;
    }

    public int getLuk_() {
        return this.luk;
    }

    public void setInt(int int_) {
        this.int_ = int_;
        recalcLocalStats();
    }

    public void setInt_(int int_) {
        this.int_ = int_;
    }

    public int setInt_() {
        return this.int_;
    }

    public void setExp(long exp) {
        this.exp.set(Math.max(
                0,
                Math.min(Math.max(0, exp),
                ExpTable.getExpNeededForLevel(level) - 2)));
    }

    public void setJob(int job) {
        this.job = MapleJob.getById(job);
        if (this.job.equals(MapleJob.精灵的基础)) {
            skills.put(SkillFactory.getSkill(20020109), new SkillEntry(1, 0));
            skills.put(SkillFactory.getSkill(20020111), new SkillEntry(1, 0));
            skills.put(SkillFactory.getSkill(20020112), new SkillEntry(1, 0));
        } else if (this.job.equals(MapleJob.恶魔猎手的基础)) {
            skills.put(SkillFactory.getSkill(30010110), new SkillEntry(1, 0));
            skills.put(SkillFactory.getSkill(30010185), new SkillEntry(1, 0));
            skills.put(SkillFactory.getSkill(30010111), new SkillEntry(1, 0));
            setFace_Adorn(1012278);//1012278
        } else if (this.job.equals(MapleJob.trailblazer)) {
            skills.put(SkillFactory.getSkill(30021236), new SkillEntry(1, 0));
            skills.put(SkillFactory.getSkill(30021237), new SkillEntry(1, 0));
            skills.put(SkillFactory.getSkill(30020232), new SkillEntry(1, 0));
            setFace_Adorn(1012361);
        }
    }

    public void setMaxhp(int hp) {
        this.maxhp = hp;
        recalcLocalStats();
    }

    public void setMaxhp_(int hp) {
        this.maxhp = hp;
    }

    public void setMaxmp(int mp) {
        this.maxmp = mp;
        recalcLocalStats();
    }

    public void setMaxmp_(int mp) {
        this.maxmp = mp;
    }

    public void setHair(int hair) {
        this.hair = hair;
    }

    public void setFace(int face) {
        this.face = face;
    }

    public void setFame(int fame) {
        this.fame = fame;
    }

    public void setAPQScore(int score) {
        this.APQScore = score;
    }

    public void setRemainingAp(int remainingAp) {
        this.remainingAp = remainingAp;
    }

    public void setRemainingSp(int remainingSp) {
        this.remainingsp[0 > this.job.GetMaxSpSlots() ? 0 : this.job.GetMaxSpSlots()] = remainingSp;
    }

    public void setRemainingSp(int remainingSp, int index) {
        this.remainingsp[index] = remainingSp;
    }

    public void setSkinColor(MapleSkinColor skinColor) {
        this.skinColor = skinColor;
    }

    public void setGender(int gender) {
        this.gender = gender;
    }

    public void setGm(int gmlevel) {
        this.GMLevel = gmlevel;
    }

    public CheatTracker getCheatTracker() {
        return anticheat;
    }

    public BuddyList getBuddylist() {
        return buddylist;
    }

    public int getAutoHpPot() {
        return autoHpPot;
    }

    public void setAutoHpPot(int itemId) {
        autoHpPot = itemId;
    }

    public int getAutoMpPot() {
        return autoMpPot;
    }

    public void setAutoMpPot(int itemId) {
        autoMpPot = itemId;
    }

    public void addFame(int famechange) {
        this.fame += famechange;
    }

    public Point getLastPortalPoint() {
        return lastPortalPoint;
    }

    public void resetLastPortalPoint() {
        this.lastPortalPoint = null;
    }

    public boolean hasEntered(String script, int mapId) {
        if (entered.containsKey(mapId)) {
            if (entered.get(mapId).equals(script)) {
                return true;
            }
        }
        return false;
    }

    public boolean hasEntered(String script) {
        for (int mapId : entered.keySet()) {
            if (entered.get(mapId).equals(script)) {
                return true;
            }
        }
        return false;
    }

    public void enteredScript(String script, int mapid) {
        if (!entered.containsKey(mapid)) {
            entered.put(mapid, script);
        }
    }

    public void resetEnteredScript() {
        if (entered.containsKey(map.getId())) {
            entered.remove(map.getId());
        }
    }

    public void resetEnteredScript(int mapId) {
        if (entered.containsKey(mapId)) {
            entered.remove(mapId);
        }
    }

    public void resetEnteredScript(String script) {
        for (int mapId : entered.keySet()) {
            if (entered.get(mapId).equals(script)) {
                entered.remove(mapId);
            }
        }
    }

    public boolean changeMapOffline(String victim, int mapId) {
        try {
            Connection con = DatabaseConnection.getConnection();
            PreparedStatement ps = con.prepareStatement("UPDATE characters SET map = ?, spawnpoint = ? WHERE name = ?");
            ps.setInt(1, mapId);
            ps.setInt(2, 0);
            ps.setString(3, victim);
            ps.executeUpdate();
            ps.close();
            con.close();
        } catch (SQLException ex) {
            return false;
        }
        return true;
    }

    private void changeMapInternal(final MapleMap to, final Point pos,
            MaplePacket warpPacket) {
        warpPacket.setOnSend(new Runnable() {
            @Override
            public void run() {
                IPlayerInteractionManager interaction = MapleCharacter.this.getInteraction();
                if (interaction != null) {
                    if (interaction.isOwner(MapleCharacter.this)) {
                        if (interaction.getShopType() == 2) {
                            interaction.removeAllVisitors(3, 1);
                            interaction.closeShop(((MaplePlayerShop) interaction).returnItems(getClient()));
                        } else if (interaction.getShopType() == 1) {
                            getClient().getSession().write(
                                    MaplePacketCreator.shopVisitorLeave(0));
                            if (interaction.getItems().isEmpty()) {
                                interaction.removeAllVisitors(3, 1);
                                interaction.closeShop(((HiredMerchant) interaction).returnItems(getClient()));
                            }
                        } else if (interaction.getShopType() == 3
                                || interaction.getShopType() == 4) {
                            interaction.removeAllVisitors(3, 1);
                        }
                    } else {
                        interaction.removeVisitor(MapleCharacter.this);
                    }
                }
                MapleCharacter.this.setInteraction(null);
                map.removePlayer(MapleCharacter.this);
                if (getClient().getChannelServer().getPlayerStorage().getCharacterById(getId()) != null) {
                    map = to;
                    setPosition(pos);
                    to.addPlayer(MapleCharacter.this);
                    lastPortalPoint = getPosition();
                    if (party != null) {
                        silentPartyUpdate();
                        getClient().getSession().write(
                                MaplePacketCreator.updateParty(getClient().getChannel(), party,
                                PartyOperation.SILENT_UPDATE, null));
                        updatePartyMemberHP();
                    }
                    if (getMap().getHPDec() > 0) {
                        hpDecreaseTask = TimerManager.getInstance().schedule(
                                new Runnable() {
                                    @Override
                                    public void run() {
                                        doHurtHp();
                                    }
                                }, 10000);
                    }
                    if (to.getId() == 980000301) { // todo: all CPq map id's
                        setTeam(MapleCharacter.rand(0, 1));
                        getClient().getSession().write(
                                MaplePacketCreator.startMonsterCarnival(getTeam()));
                    }
                }
            }
        });
        getClient().getSession().write(warpPacket);
    }

    public void leaveMap() {
        // 取消可视map object 和控制的怪物
        controlled.clear();
        getMove_lock().lock();
        try {
            visibleMapObjects.clear();
        } finally {
            getMove_lock().unlock();
        }
        if (chair != 0) {
            chair = 0;
        }
        if (hpDecreaseTask != null) {
            hpDecreaseTask.cancel(false);
        }
        cancelDoor2(); // 取消机械传送门进程
    }

    public void doHurtHp() {
        if (this.getInventory(MapleInventoryType.EQUIPPED).findById(
                getMap().getHPDecProtect()) != null) {
            return;
        }
        addHP(-getMap().getHPDec());
        hpDecreaseTask = TimerManager.getInstance().schedule(new Runnable() {
            @Override
            public void run() {
                doHurtHp();
            }
        }, 10000);
    }

    public void changeJob(MapleJob newJob) {
        // this.job = newJob;
        setJob(newJob.getId());
        String jobstring = this.job.toString();
        for (Iterator<Entry<ISkill, SkillEntry>> it = skills.entrySet().iterator(); it.hasNext();) {
            Entry<ISkill, SkillEntry> entry = it.next();
            if (!String.valueOf(entry.getKey().getId()).startsWith(jobstring)
                    && entry.getValue().masterlevel > 0
                    && entry.getValue().skillevel == 0) {// 前绰不是新JOBID
                it.remove();
                // System.out.println("删除技能：" + entry.getKey().getId());
            }
        }
        if (this.job.equals(MapleJob.Dual_Blade_5)) {
            changeSkillLevel(SkillFactory.getSkill(4341009), 0, 30);// 幽灵一击
        }

        updateSingleStat(MapleStat.JOB, newJob.getId());
        //this.setRemainingSp(this.getRemainingSp() + 1);
        if (newJob.getId() % 10 == 2) {
            //      this.setRemainingSp(this.getRemainingSp() + 2);
        }
        giveMasteryLevel(job.getId()); // 给4转技能上限
        switch (this.job.getId()) {
            // 转职增加HP/MP
            case 110: // 剑客
            case 1110:// 魂骑士
                maxhp += rand(300, 350);
                break;
            case 120: // 准骑士
            case 130: // 枪骑士
            case 2110:// 战神
                maxmp += rand(100, 150);
                break;
            case 210: // 火毒
            case 220: // 冰雷
            case 230: // 牧师
            case 1210:// 炎术士
                maxmp += rand(450, 500);
                break;
            case 310: // 弓手
            case 320: // 弩手
            case 1310:// 风灵使者
            case 410: // 标飞
            case 420: // 刀飞
            case 1410:// 夜行者
            case 510: // 拳手
            case 520: // 枪手
            case 530: // 神炮手
            case 1510:// 奇袭者
                maxhp += rand(300, 350);
                maxmp += rand(150, 200);
                break;
            case 2210:// 龙神2转
            case 2211:// 龙神3转
            case 2212:// 龙神4转
            case 2213:// 龙神5转
            case 2214:// 龙神6转
                maxmp += rand(100, 150);
                break;
            case 431: // 双刀2转
                maxhp += rand(150, 200);
                maxmp += rand(50, 100);
                break;
            case 432: // 双刀3转
            case 433: // 双刀4转
                maxhp += rand(300, 350);
                maxmp += rand(150, 200);
                break;

            case 2300:
            case 2310:
            case 2311:
            case 2312:
            case 3210: // 战法2转
            case 3211: // 战法3转
            case 3212: // 战法4转
                maxhp += 200;
                maxmp += 100;
                break;
            // 1转强制更改HP/MP
            case 100: // 战士
            case 1100:// 魂骑士
            case 2100:// 战神
                maxhp = 444 + (level - 10) * 16;
                maxmp = 113 + (level - 10) * 12;
                break;
            case 200: // 魔法师
            case 1200:// 炎术士
                maxhp = 162 + (level - 8) * 16;
                maxmp = 253 + (level - 8) * 16;
                break;
            case 300: // 弓箭手
            case 1300:// 风灵使者
            case 400: // 飞侠
            case 1400:// 夜行者
            case 500: // 海盗
            case 501: // 神炮手
            case 1500:// 奇袭者
            case 430: // 双刀1转
            case 3300:// 弩骑
            case 3500:// 机械师
                maxhp = 344 + (level - 10) * 16;
                maxmp = 163 + (level - 10) * 12;
                break;
            case 2200: // 龙神1转
                maxhp = 194 + (level - 10) * 16;
                maxmp = 263 + (level - 10) * 16;
                break;
            case 3200:// 战法
                maxhp = 344 + (level - 10) * 16;
                maxmp = 285 + (level - 10) * 12;
                break;
            case 3001:
            case 3100:
            case 3110:
            case 3111:
            case 3112:
                maxhp += rand(64, 68);
            default:
                // maxhp = 300 + (level - 10) * 16;
                // maxmp = 255 + (level - 10) * 12;
                maxhp += 300;
                maxmp += 255;
                break;
        }
        if (maxhp > 99999) {
            maxhp = 99999;
        }
        if (maxmp > 99999) {
            maxmp = 99999;
        }
        if (getJob().IsDemonHunter()) {
            maxmp = 10;
        }
        setHp(maxhp);
        setMp(maxmp);
        List<Pair<MapleStat, Number>> statup = new ArrayList<Pair<MapleStat, Number>>();
        statup.add(new Pair<MapleStat, Number>(MapleStat.MAXHP, Integer.valueOf(maxhp)));
        statup.add(new Pair<MapleStat, Number>(MapleStat.MAXMP, Integer.valueOf(maxmp)));
        recalcLocalStats();
        /*
         * if(isEvan()){ givedragonSP(getdragonz(), +3);
         * getClient().getSession()
         * .write(MaplePacketCreator.updateEvanSP(this)); } else if
         * (isResistance()){ gainBigBangSP(getBigBangJob(), + 3);
         * getClient().getSession
         * ().write(MaplePacketCreator.updateResistanceSP(this)); } else {
         * statup.add(new Pair(MapleStat.AVAILABLESP,
         * Integer.valueOf(this.remainingSp))); }
         */
        getClient().getSession().write(
                MaplePacketCreator.updatePlayerStats(statup, this));
        getMap().broadcastMessage(this,
                MaplePacketCreator.showJobChange(getId()), false);
        silentPartyUpdate();
        guildUpdate();
        sendDemonAvengerPacket();
    }

    public void gainAp(int ap) {
        this.remainingAp += ap;
        updateSingleStat(MapleStat.AVAILABLEAP, this.remainingAp);
    }

    public void apReset() {
        List<Pair<MapleStat, Number>> statups = new ArrayList<Pair<MapleStat, Number>>();
        int Str = 4, Dex = 4, Int = 4, Luk = 4;
        if (job.isA(MapleJob.WARRIOR)) {
            Str = 35;
        }
        if (job.isA(MapleJob.MAGICIAN)) {
            Int = 20;
        }
        if (job.isA(MapleJob.BOWMAN)) {
            Dex = 25;
        }
        if (job.isA(MapleJob.THIEF)) {
            Dex = 25;
        }
        if (job.isA(MapleJob.PIRATE)) {
            Dex = 20;
        }
        int ap = getStr() + getDex() + getInt() + getLuk() + getRemainingAp()
                - (Dex + Str + Int + Luk);
        setStr(Str);
        setDex(Dex);
        setInt(Int);
        setLuk(Luk);
        setRemainingAp(ap);
        statups.add(new Pair<MapleStat, Number>(MapleStat.STR, getStr()));
        statups.add(new Pair<MapleStat, Number>(MapleStat.DEX, getDex()));
        statups.add(new Pair<MapleStat, Number>(MapleStat.INT, getInt()));
        statups.add(new Pair<MapleStat, Number>(MapleStat.LUK, getLuk()));
        statups.add(new Pair<MapleStat, Number>(MapleStat.AVAILABLEAP,
                getRemainingAp()));
        getClient().getSession().write(
                net.sf.odinms.tools.MaplePacketCreator.updatePlayerStats(
                statups, this));
    }

    public void ClearAllSkills() {
        getBuffManager().cancelAllBuffs();
        synchronized (skills) {
            ArrayList<Integer> skillsList = new ArrayList<Integer>();
            for (ISkill iSkill : skills.keySet()) {
                if (!MapleStatEffect.isMonsterRiding(iSkill.getId())) {
                    skillsList.add(iSkill.getId());
                }
            }
            Integer[] tmparray = skillsList.toArray(new Integer[0]);
            int[] skill = new int[tmparray.length];
            for (int i = 0; i < tmparray.length; i++) {
                Integer tmpInteger = tmparray[i];
                skill[i] = tmpInteger;
            }
            this.getClient().getSession().write(MaplePacketCreator.updateSkill(skill,
                    new int[skill.length], new int[skill.length],
                    new Timestamp[skill.length]));
            for (int i : skill) {
                skills.remove(SkillFactory.getSkill(i));
            }
        }
    }

    public void changeSkillLevel(ISkill skill, int newLevel, int newMasterlevel) {
        changeSkillLevel(skill, newLevel, newMasterlevel, null);
    }

    public void changeSkillLevel(ISkill skill, int newLevel,
            int newMasterlevel, Timestamp eTimestamp) {
        if (skill == null) {
            return;
        }
        newLevel = Math.max(0, newLevel);
        newMasterlevel = Math.max(0, newMasterlevel);
        // 排除负数
        if (newLevel == 0 && newMasterlevel == 0) {
            if (skills.containsKey(skill)) {
                skills.remove(skill);
            }
        } else {
            // 不允许学习。
            // 不允许学习戒指技能。
            if (GameConstants.勇士的意志技能系(skill.getId())
                    || GameConstants.isAngelRingSkill(skill.getId())) {
                return;
            }
            // 临时封技能表。
            if (GameConstants.banskill.contains(skill.getId())) {
                return;
            }

            newLevel = Math.min(newLevel, skill.getMaxLevel());
            // 等级越界。
            if (skill.hasMastery()) {
                newMasterlevel = Math.max(newMasterlevel,
                        skill.getMasterLevel());
                // 不低于WZ精通等级.
                newMasterlevel = Math.min(newMasterlevel, skill.getMaxLevel());
                // 不超过最大等级
                newMasterlevel = Math.max(newMasterlevel, newLevel);
                // 有多少技能等级.就有多少精通。
            } else {
                newMasterlevel = 0;
                // 不适用能力等级
            }
            skills.put(skill, new SkillEntry(newLevel, newMasterlevel,
                    eTimestamp));
        }
        if (skill != null && isAddHpMpSkill(skill.getId())) {
            recalcLocalStats();
        }
        this.getClient().getSession().write(MaplePacketCreator.updateSkill(skill.getId(), newLevel,
                newMasterlevel, eTimestamp));
    }

    public void setHpPot(int itemid) {
        this.hppot = itemid;
    }

    public void setMpPot(int itemid) {
        this.mppot = itemid;
    }

    // 反抗者技能更新
    public void changeResistanceSkillLevel(ISkill skill, int newLevel,
            int newMasterlevel) {
        if (skill.getId() == 32001001) {
            ISkill skil1 = SkillFactory.getSkill(32001008);
            ISkill skil2 = SkillFactory.getSkill(32001009);
            ISkill skil3 = SkillFactory.getSkill(32001010);
            ISkill skil4 = SkillFactory.getSkill(32001011);
            int maxlevel1 = skil1.getMaxLevel();
            int maxlevel2 = skil2.getMaxLevel();
            int maxlevel3 = skil3.getMaxLevel();
            int maxlevel4 = skil4.getMaxLevel();
            int curLevel1 = this.getSkillLevel(skil1);
            int curLevel2 = this.getSkillLevel(skil2);
            int curLevel3 = this.getSkillLevel(skil3);
            int curLevel4 = this.getSkillLevel(skil4);

            skills.put(skil1, new SkillEntry(curLevel1 + 1, maxlevel1));
            skills.put(skil2, new SkillEntry(curLevel2 + 1, maxlevel2));
            skills.put(skil3, new SkillEntry(curLevel3 + 1, maxlevel3));
            skills.put(skil4, new SkillEntry(curLevel4 + 1, maxlevel4));
        }
    }

    public int getResistanceSkill(int skillid) {
        int cd = skillid / 10000;
        if (cd == 3200 || cd == 3300 || cd == 3500) {
            return 1;
        } else if (cd == 3210 || cd == 3310 || cd == 3510) {
            return 2;
        } else if (cd == 3211 || cd == 3311 || cd == 3511) {
            return 3;
        } else if (cd == 3212 || cd == 3312 || cd == 3512) {
            return 4;
        } else {
            return 0;
        }

    }

    public int getHpPot() {
        return this.hppot;
    }

    public int getMpPot() {
        return this.mppot;
    }

    public void setHp(int newhp) {
        setHp(newhp, false);
    }

    public void setHp_(int newhp) {
        this.hp = newhp;
    }

    public void setHp(int newhp, boolean silent) {
        /*    if (GameConstants.CheckMapAndServer(getMapId())) {
         return;
         }*/

        int oldHp = hp;
        int thp = newhp;
        if (thp < 0) {
            thp = 0;
        }
        if (thp > localmaxhp) {
            thp = localmaxhp;
        }
        this.hp = thp;

        if (!silent) {
            updatePartyMemberHP();
        }
        /*
         * log.debug("oldHp:" +oldHp); log.debug("localmaxhp:" +localmaxhp);
         * log.debug("Hp:" +hp);
         */
        if (oldHp > hp && !isAlive()) {
            playerDead();
        }
        // this.checkBerserk();
    }

    public void setHp__(int newhp, boolean silent) {

        int oldHp = hp;
        int thp = newhp;
        if (thp < 0) {
            thp = 0;
        }
        if (thp > localmaxhp) {
            thp = localmaxhp;
        }
        this.hp = thp;

        if (!silent) {
            updatePartyMemberHP();
        }
        /*
         * log.debug("oldHp:" +oldHp); log.debug("localmaxhp:" +localmaxhp);
         * log.debug("Hp:" +hp);
         */
        if (oldHp > hp && !isAlive()) {
            playerDead();
        }
        // this.checkBerserk();
    }

    private void playerDead() {
        if (getEventInstance() != null) {
            getEventInstance().playerKilled(this);
        }

        /*if (effects.containsKey(MapleBuffStat.神秘的运气)) {
         addHP(maxhp);
         }*/
        dispelSkill(0);
        cancelAllDebuffs();
        cancelMorphs();

        if (getMainsumons() != null) {
            getMap().broadcastMessage(
                    MaplePacketCreator.removeSpecialMapObject(getMainsumons(),
                    true));
            getMap().removeMapObject(getMainsumons());
            removeVisibleMapObject(getMainsumons());
            getSummons().remove(getMainsumons().getSkill());
            setMainsumons(null);
        }

        int[] charmID = {5130000, 5130002, 5131000, 4031283, 4140903};
        MapleCharacter player = getClient().getPlayer();
        int possesed = 0;
        int i;

        // Check for charms
        for (i = 0; i < charmID.length; i++) {
            int quantity = getItemQuantity(charmID[i], false);
            if (possesed == 0 && quantity > 0) {
                possesed = quantity;
                break;
            }
        }

        if (possesed > 0 && !getMap().hasEvent()) {
            possesed -= 1;
            getClient().getSession().write(
                    MaplePacketCreator.serverNotice(5,
                    "因使用了 [护身符] 死亡后您的经验不会减少！剩余 (" + possesed + " 个)"));
            MapleInventoryManipulator.removeById(getClient(),
                    MapleItemInformationProvider.getInstance().getInventoryType(charmID[i]), charmID[i], 1, true,
                    false);
        } else if (getMap().hasEvent()) {
            getClient().getSession().write(
                    MaplePacketCreator.serverNotice(5, "在任务地图中死亡，您的经验值不会减少。"));
        } else {
            if (player.getJob() != MapleJob.BEGINNER
                    || player.getJob() != MapleJob.KNIGHT
                    || player.getJob() != MapleJob.Aran) {
                // Lose XP
                long XPdummy = ExpTable.getExpNeededForLevel(player.getLevel() + 1);
                if (player.getMap().isTown()) {
                    XPdummy *= 0.01;
                }
                if (XPdummy == ExpTable.getExpNeededForLevel(player.getLevel() + 1)) {
                    if (player.getLuk() <= 100 && player.getLuk() > 8) {
                        XPdummy *= 0.10 - (player.getLuk() * 0.0005);
                    } else if (player.getLuk() < 8) {
                        XPdummy *= 0.10; // Otherwise they lose about 9 percent
                    } else {
                        XPdummy *= 0.10 - (100 * 0.0005);
                    }
                }
                if ((player.getExp() - XPdummy) > 0) {
                    player.gainExp(-XPdummy, false, false);
                } else {
                    player.gainExp(-player.getExp(), false, false);
                }
            }
        }
        if (getBuffedValue(MapleBuffStat.MORPH) != null) {
            cancelEffectFromBuffStat(MapleBuffStat.MORPH);
        }
        if (getBuffedValue(MapleBuffStat.骑宠1) != null) {
            cancelEffectFromBuffStat(MapleBuffStat.骑宠1);
        }
        client.getSession().write(MaplePacketCreator.enableActions());
    }

    public void updatePartyMemberHP() {
        if (party != null) {
            int channel = client.getChannel();
            for (MaplePartyCharacter partychar : party.getMembers()) {
                if (partychar.getMapid() == getMapId()
                        && partychar.getChannel() == channel) {
                    MapleCharacter other = client.getChannelServer().getPlayerStorage().getCharacterByName(partychar.getName());
                    if (other != null) {
                        other.getClient().getSession().write(MaplePacketCreator.updatePartyMemberHP(
                                getId(), this.hp, localmaxhp));
                    }
                }
            }
        }
    }

    public void receivePartyMemberHP() {
        if (party != null) {
            int channel = client.getChannel();
            for (MaplePartyCharacter partychar : party.getMembers()) {
                if (partychar.getMapid() == getMapId()
                        && partychar.getChannel() == channel) {
                    MapleCharacter other = client.getChannelServer().getPlayerStorage().getCharacterByName(partychar.getName());
                    if (other != null) {
                        getClient().getSession().write(
                                MaplePacketCreator.updatePartyMemberHP(
                                other.getId(), other.getHp(),
                                other.getCurrentMaxHp()));
                    }
                }
            }
        }
    }

    public void setMp(int newmp) {
        int tmp = newmp;
        if (tmp < 0) {
            tmp = 0;
        }
        if (tmp > localmaxmp) {
            tmp = localmaxmp;
        }
        this.mp = tmp;
    }

    public void setMp_(int newmp) {
        this.mp = newmp;
    }

    /**
     * Convenience function which adds the supplied parameter to the current hp
     * then directly does a updateSingleStat.
     *
     * @see MapleCharacter#setHp(int)
     * @param delta
     */
    public void addHP(int delta) {
        setHp(hp + delta);
        updateSingleStat(MapleStat.HP, hp);
    }

    public void addHP_(int delta) {
        setHp__(hp + delta, false);
        updateSingleStat(MapleStat.HP, hp);
    }

    /**
     * Convenience function which adds the supplied parameter to the current mp
     * then directly does a updateSingleStat.
     *
     * @see MapleCharacter#setMp(int)
     * @param delta
     */
    public void addMP(int delta) {
        setMp(mp + delta);
        updateSingleStat(MapleStat.MP, mp);
    }

    public void addMPHP(int hpDiff, int mpDiff) {
        setHp(hp + hpDiff);
        setMp(mp + mpDiff);
        List<Pair<MapleStat, Number>> stats = new ArrayList<Pair<MapleStat, Number>>();
        stats.add(new Pair<MapleStat, Number>(MapleStat.HP, Integer.valueOf(hp)));
        stats.add(new Pair<MapleStat, Number>(MapleStat.MP, Integer.valueOf(mp)));
        MaplePacket updatePacket = MaplePacketCreator.updatePlayerStats(stats,
                this);
        client.getSession().write(updatePacket);
    }

    /**
     * Updates a single stat of this MapleCharacter for the client. This method
     * only creates and sends an update packet, it does not update the stat
     * stored in this MapleCharacter instance.
     *
     * @param stat
     * @param newval
     * @param itemReaction
     */
    public void updateSingleStat(MapleStat stat, Number newval,
            boolean itemReaction) {
        Pair<MapleStat, Number> statpair = new Pair<MapleStat, Number>(stat,
                newval);
        MaplePacket updatePacket = MaplePacketCreator.updatePlayerStats(
                Collections.singletonList(statpair), itemReaction, this);
        client.getSession().write(updatePacket);
    }

    public void updateSingleStat(List<Pair<MapleStat, Number>> stats,
            boolean itemReaction) {
        MaplePacket updatePacket = MaplePacketCreator.updatePlayerStats(stats,
                itemReaction, this);
        client.getSession().write(updatePacket);
    }

    public void updateSingleStat(List<Pair<MapleStat, Number>> stats) {
        updateSingleStat(stats, false);
    }

    public void updateSingleStat(MapleStat stat, Number newval) {
        updateSingleStat(stat, newval, false);
    }

    public void gainExp(long gain, boolean show, boolean inChat) {
        gainExp(gain, show, inChat, true, false);
    }

    public void gainExp(long gain, boolean show, boolean inChat, boolean fish) {
        gainExp(gain, show, inChat, true, fish);
    }

    public int getMaxLevel() {
        return isCygnus() ? 180 : ExpTable.getMaxLevel();
    }

    public boolean isCygnus() {
        return getJobType_1() == 1;
    }

    public int getJobType_1() {
        return job.getId() / 1000;
    }

    public int getJobid() { // 得到职业ID
        return job.getId();
    }

    private void setJobid(int value) {
        this.job = MapleJob.getById(value);
    }

    public void gainExp(long gain, boolean show, boolean inChat, boolean white,
            boolean fish) {
        if (level < getMaxLevel()) {
            /*if (this.exp.get() + gain > Integer.MAX_VALUE) {
             long gainFirst = ExpTable.getExpNeededForLevel(level)
             - this.exp.get();
             gain -= gainFirst + 1;
             gainExp(gainFirst + 1, false, inChat, white);
             }*/
            updateSingleStat(MapleStat.EXP, this.exp.addAndGet(gain));
            if (show && gain != 0 && !fish) {
                client.getSession().write(
                        MaplePacketCreator.getShowExpGain(gain > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) gain, inChat, white));
            }
            if (fish) {
                client.getSession().write(
                        MaplePacketCreator.getShowExpGain(gain > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) gain, inChat, white));
                // client.getSession().write(MaplePacketCreator.getShowFishGain(gain,
                // 2));
            }
            if (exp.get() >= ExpTable.getExpNeededForLevel(level)) {
                levelUp();
                long need = ExpTable.getExpNeededForLevel(level);
                if (exp.get() >= need) {
                    setExp(need - 1);
                    updateSingleStat(MapleStat.EXP, need);
                }
            }
        }
    }

    public void silentPartyUpdate() {
        if (party != null) {
            try {
                getClient().getChannelServer().getWorldInterface().updateParty(party.getId(),
                        PartyOperation.SILENT_UPDATE,
                        new MaplePartyCharacter(MapleCharacter.this));
            } catch (RemoteException e) {
                ServerExceptionHandler.HandlerRemoteException(e);
                getClient().getChannelServer().reconnectWorld();
            }
        }
    }

    // public void gainExp(int gain, boolean show, boolean inChat) {
    // gainExp(gain, show, inChat, true);
    // }
    public boolean isGM() {
        return GMLevel > 0;
    }

    public int getGm() {
        return GMLevel;
    }

    public boolean hasGMLevel(int level) {
        return GMLevel >= level;
    }

    public MapleInventory getInventory(MapleInventoryType type) {
        return inventory[type.ordinal()];
    }

    public MapleShop getShop() {
        return shop;
    }

    public void setShop(MapleShop shop) {
        this.shop = shop;
    }

    public long getMeso() {
        return meso.get();
    }

    public int getSavedLocation(SavedLocationType type) {
        return savedLocations[type.ordinal()];
    }

    public void saveLocation(SavedLocationType type) {
        savedLocations[type.ordinal()] = getMapId();
    }

    public void clearSavedLocation(SavedLocationType type) {
        savedLocations[type.ordinal()] = -1;
    }

    public void gainMeso(long gain, boolean show) {
        gainMeso(gain, show, false, false);
    }

    public void gainMeso(long gain, boolean show, boolean enableActions) {
        gainMeso(gain, show, enableActions, false);
    }

    public void gainMeso(long gain, boolean show, boolean enableActions,
            boolean inChat) {
        if (meso.get() + gain < 0) {
            client.getSession().write(MaplePacketCreator.enableActions());
            return;
        }
        long newVal = meso.addAndGet(gain);
        if (newVal < 0 || meso.get() > GameConstants.MAX_MESO) {// 加爆了。
            meso.set(GameConstants.MAX_MESO);
            newVal = GameConstants.MAX_MESO;
            弹窗("你身上的钱差一点爆。");
        }
        updateSingleStat(MapleStat.MESO, newVal, enableActions);
        if (show) {
            client.getSession().write(
                    MaplePacketCreator.getShowMesoGain(gain, inChat));
        }
    }

    /**
     * Adds this monster to the controlled list. The monster must exist on the
     * Map.
     *
     * @param monster
     */
    public void controlMonster(MapleMonster monster, boolean aggro) {
        monster.setController(this);
        controlled.add(monster);
        client.getSession().write(
                MaplePacketCreator.controlMonster(monster, false, aggro));
    }

    public void stopControllingMonster(MapleMonster monster) {
        controlled.remove(monster);
    }

    public void checkMonsterAggro(MapleMonster monster) {
        if (!monster.isControllerHasAggro()) {
            if (monster.getController() == this) {
                monster.setControllerHasAggro(true);
            } else {
                monster.switchController(this, true);
            }
        }
    }

    public Collection<MapleMonster> getControlledMonsters() {
        return Collections.unmodifiableCollection(controlled);
    }

    public int getNumControlledMonsters() {
        return controlled.size();
    }

    @Override
    public String toString() {
        return "玩家: " + this.name;
    }

    public int getAccountid() {
        return accountid;
    }

    public void setAccountid(int value) {
        this.accountid = value;
    }

    public void mobKilled(int id) {
        for (MapleQuestStatus q : quests.values()) {
            if (MapleQuest.getInstance(q.getQuest().getId()).nullCompleteQuestData()) {
                reloadQuest(MapleQuest.getInstance(q.getQuest().getId()));
            }
            if (q.getStatus() == MapleQuestStatus.Status.COMPLETED
                    || q.getQuest().canComplete(this, null)) {
                continue;
            }
            if (q.mobKilled(id) && !(q.getQuest() instanceof MapleCustomQuest)) {
                client.getSession().write(
                        MaplePacketCreator.updateQuestMobKills(q));
                if (q.getQuest().canComplete(this, null)) {
                    client.getSession().write(
                            MaplePacketCreator.getShowQuestCompletion(q.getQuest().getId()));
                }
            }
        }
    }

    public final List<MapleQuestStatus> getStartedQuests() {
        List<MapleQuestStatus> ret = new LinkedList<MapleQuestStatus>();
        for (MapleQuestStatus q : quests.values()) {
            if (q.getStatus() == MapleQuestStatus.Status.COMPLETED) {
                continue;
            } else {
                if (q.getStatus().equals(MapleQuestStatus.Status.STARTED)
                        && !(q.getQuest() instanceof MapleCustomQuest)) {
                    ret.add(q);
                }
            }
        }
        return Collections.unmodifiableList(ret);
    }

    public final List<MapleQuestStatus> getCompletedQuests() {
        List<MapleQuestStatus> ret = new LinkedList<MapleQuestStatus>();
        for (MapleQuestStatus q : quests.values()) {
            if (q.getStatus().equals(MapleQuestStatus.Status.COMPLETED)) {// &&
                // !(q.getQuest()
                // instanceof
                // MapleCustomQuest)
                ret.add(q);
            }
        }
        return Collections.unmodifiableList(ret);
    }

    public void reloadQuest(MapleQuest quest) {
        int questId = quest.getId();
        MapleQuestStatus qs = getQuest(quest);
        quests.remove(quest);
        MapleQuest.remove(questId);
        MapleQuest q = MapleQuest.getInstance(questId);
        quests.put(q, qs);
    }

    public Map<ISkill, SkillEntry> getSkills() {
        return Collections.unmodifiableMap(skills);
    }

    public void dispelSkill(int skillid) {
        /*  LinkedList<MapleBuffStatValueHolder> allBuffs = new LinkedList<MapleBuffStatValueHolder>(
         effects.values());
         for (MapleBuffStatValueHolder mbsvh : allBuffs) {
         if (skillid == 0) { // 人物死亡时
         if (mbsvh.effect.isSkill()
         && (mbsvh.effect.getSourceId() % 20000000 == 1004 || dispelSkills(mbsvh.effect.getSourceId()))) {
         cancelEffect(mbsvh.effect, false, mbsvh.startTime);
         }
         } else if (mbsvh.effect.isSkill()
         && mbsvh.effect.getSourceId() == skillid) {
         cancelEffect(mbsvh.effect, false, mbsvh.startTime);
         }
         }*/
        buffManager.dispelSkill(skillid);
    }

    public static boolean dispelSkills(int skillid) {
        switch (skillid) {
            case 1004:
            case 1321007:
            case 2121005:
            case 2221005:
            case 2311006:
            case 2321003:
            case 3111002:
            case 3111005:
            case 3211002:
            case 3211005:
            case 4111002:
            case 11001004:
            case 12001004:
            case 13001004:
            case 14001005:
            case 15001004:
            case 12111004:
            case 20001004:
                return true;
            default:
                return false;
        }
    }

    public int getSkillLevel(int skill) {
        if (GameConstants.isAngelRingSkill(skill)) {
            return 1;
        }
        SkillEntry ret = skills.get(SkillFactory.getSkill(skill));
        if (ret == null) {
            return 0;
        }
        return ret.skillevel;
    }

    public int getSkillLevel(ISkill skill) {
        if (skill != null && GameConstants.isAngelRingSkill(skill.getId())) {
            return 1;
        }
        SkillEntry ret = skills.get(skill);
        if (skill != null
                && (skill.getId() == 1009 || skill.getId() == 10001009
                || skill.getId() == 1010 || skill.getId() == 1011
                || skill.getId() == 10001010 || skill.getId() == 10001011)) {
            return 1;
        } else if (ret == null) {
            return 0;
        }
        return ret.skillevel;
    }

    public int getMasterLevel(ISkill skill) {
        SkillEntry ret = skills.get(skill);
        if (ret == null) {
            return 0;
        }
        return ret.masterlevel;
    }

    // the equipped inventory only contains equip... I hope
    public int getTotalDex() {
        return localdex;
    }

    public int getTotalInt() {
        return localint_;
    }

    public int getTotalStr() {
        return localstr;
    }

    public int getTotalLuk() {
        return localluk;
    }

    public int getTotalMagic() {
        return magic;
    }

    public double getSpeedMod() {
        return speedMod;
    }

    public double getJumpMod() {
        return jumpMod;
    }

    public int getTotalWatk() {
        return watk;
    }

    private static int rand(int lbound, int ubound) {
        return (int) ((Math.random() * (ubound - lbound + 1)) + lbound);
    }

    public void levelUp() {
        ISkill improvingMaxHP = null;
        int improvingMaxHPLevel = 0;
        ISkill improvingMaxMP = null;
        int improvingMaxMPLevel = 0;
        if (this.job.IsDoubleCrossbows()) {// 双弩
            if (!skills.containsKey(SkillFactory.getSkill(20020111))) {
                changeSkillLevel(SkillFactory.getSkill(20020111), 1, 0);
            }
            if (!skills.containsKey(SkillFactory.getSkill(20020112))) {
                changeSkillLevel(SkillFactory.getSkill(20020112), 1, 0);
            }
        }
        if (job.getId() >= 1000 && job.getId() <= 1512 && getLevel() < 70) {
            remainingAp += 1;
        }

        remainingAp += 5;
      //  remainingAp += Math.max(getVip() - 1, 0);

        int jobid = job.getId();
        switch (jobid) {
            // 新手
            case 0:
            case 1000:
            case 2000:
            case 2001:
            case 3000:
                maxhp += rand(12, 16);
                maxmp += rand(10, 12);
                break;
            // 战士
            case 100:
            case 110:
            case 111:
            case 112:
            case 120:
            case 121:
            case 122:
            case 130:
            case 131:
            case 132:
            case 1100:
            case 1110:
            case 1111:
            case 1112:
                maxhp += rand(64, 68);
                maxmp += rand(4, 6);
                break;
            // 法师
            case 200:
            case 210:
            case 211:
            case 212:
            case 220:
            case 221:
            case 222:
            case 230:
            case 231:
            case 232:
            case 1200:
            case 1210:
            case 1211:
            case 1212:
                maxhp += rand(10, 14);
                maxmp += rand(42, 44);
                break;
            // 弓 飞 弩骑
            case 300:
            case 310:
            case 311:
            case 312:
            case 320:
            case 321:
            case 322:
            case 400:
            case 410:
            case 411:
            case 412:
            case 420:
            case 421:
            case 422:
            case 430:
            case 431:
            case 432:
            case 433:
            case 434:
            case 1300:
            case 1310:
            case 1311:
            case 1312:
            case 1400:
            case 1410:
            case 1411:
            case 1412:
            case 3300:
            case 3310:
            case 3311:
            case 3312:
                maxhp += rand(20, 24);
                maxmp += rand(14, 16);
                break;
            // 一转海盗 枪手 机械师
            case 500:
            case 520:
            case 521:
            case 522:
            case 3500:
            case 3510:
            case 3511:
            case 3512:
                maxhp += rand(22, 26);
                maxmp += rand(18, 22);
                break;
            // 拳手
            case 510:
            case 511:
            case 512:
            case 1500:
            case 1510:
            case 1511:
            case 1512:
                maxhp += rand(52, 56);
                maxmp += rand(18, 22);
                break;
            // 战神
            case 2100:
            case 2110:
            case 2111:
            case 2112:
                maxhp += rand(44, 48);
                maxmp += rand(4, 8);
                break;
            // 龙神
            case 2200:
            case 2210:
            case 2211:
            case 2212:
            case 2213:
            case 2214:
            case 2215:
            case 2216:
            case 2217:
            case 2218:
                maxhp += rand(16, 20);
                maxmp += rand(35, 39);
                break;
            // 战法
            case 3200:
            case 3210:
            case 3211:
            case 3212:
                maxhp += rand(34, 38);
                maxmp += rand(22, 24);
                break;
            case 3001:
            case 3100:
            case 3110:
            case 3111:
            case 3112:
                maxhp += rand(64, 68);
                break;
            default:
                maxhp += rand(24, 38);
                maxmp += rand(12, 24);
                break;
        }
        if (improvingMaxHPLevel > 0) {
            maxhp += improvingMaxHP.getEffect(improvingMaxHPLevel).getX();
        }
        if (improvingMaxMPLevel > 0) {
            maxmp += improvingMaxMP.getEffect(improvingMaxMPLevel).getX();
        }
        if (!getJob().IsDemonHunter()) {
            maxmp += getTotalInt() / 10; // 所有职业适用的智力对于Mp的修正公式
        }
        if (getJob().IsDemonHunter()) {
            maxmp = 10;
        }
        exp.addAndGet(-ExpTable.getExpNeededForLevel(level + 1));
        level += 1;
        exp.set(0);

        if (level == 200) {
            MaplePacket packet = MaplePacketCreator.serverNotice(6, "[祝贺] "
                    + getName() + " 玩家历经苦难，终于到达200级。大家祝贺他吧！");
            try {
                getClient().getChannelServer().getWorldInterface().broadcastMessage(getName(), packet.getBytes());
            } catch (RemoteException e) {
                ServerExceptionHandler.HandlerRemoteException(e);
                getClient().getChannelServer().reconnectWorld();
            }
        }
        maxhp = Math.min(99999, maxhp);// 087之后上限为9w9
        maxmp = Math.min(99999, maxmp);
        setHp(maxhp);
        setMp(maxmp);
        List<Pair<MapleStat, Number>> statup = new ArrayList<Pair<MapleStat, Number>>();
        statup.add(new Pair<MapleStat, Number>(MapleStat.AVAILABLEAP, Integer.valueOf(remainingAp)));
        statup.add(new Pair<MapleStat, Number>(MapleStat.MAXHP, Integer.valueOf(maxhp)));
        statup.add(new Pair<MapleStat, Number>(MapleStat.MAXMP, Integer.valueOf(maxmp)));
        statup.add(new Pair<MapleStat, Number>(MapleStat.HP, Integer.valueOf(maxhp)));
        statup.add(new Pair<MapleStat, Number>(MapleStat.MP, Integer.valueOf(maxmp)));
        statup.add(new Pair<MapleStat, Number>(MapleStat.EXP, exp.get()));
        statup.add(new Pair<MapleStat, Number>(MapleStat.LEVEL, Integer.valueOf(level)));
        if ((isEvan()) && (this.level >= 10)) {
            if (this.level == 10) {
                changeJob(MapleJob.Evan_1);
                evanFirstAdvance();
                resetStats();// 将之前的属性点改变为4
            } else if (this.level == 20) {
                changeJob(MapleJob.Evan_2);
                evanFirstAdvance();
            } else if (this.level == 30) {
                changeJob(MapleJob.Evan_3);
                evanFirstAdvance();
            } else if (this.level == 40) {
                changeJob(MapleJob.Evan_4);
                evanFirstAdvance();
            } else if (this.level == 50) {
                changeJob(MapleJob.Evan_5);
                evanFirstAdvance();
            } else if (this.level == 60) {
                changeJob(MapleJob.Evan_6);
                evanFirstAdvance();
            } else if (this.level == 80) {
                changeJob(MapleJob.Evan_7);
                evanFirstAdvance();
            } else if (this.level == 100) {
                changeJob(MapleJob.Evan_8);
                evanFirstAdvance();
            } else if (this.level == 120) {
                changeJob(MapleJob.Evan_9);
                evanFirstAdvance();
            } else if (this.level == 160) {
                changeJob(MapleJob.Evan_10);
                evanFirstAdvance();
            }
        }
        if (job != MapleJob.BEGINNER && job != MapleJob.Evan
                && job != MapleJob.Resistance) {
            /*
             * if (isEvan()) { givedragonSP(getdragonz(), +3);
             * getClient().getSession
             * ().write(MaplePacketCreator.updateEvanSP(this)); } else if
             * (isResistance()) { gainBigBangSP(getBigBangJob(), +3);
             * getClient()
             * .getSession().write(MaplePacketCreator.updateResistanceSP(this));
             * } else { remainingSp += 3; statup.add(new Pair<MapleStat,
             * Integer>(MapleStat.AVAILABLESP, Integer.valueOf(remainingSp))); }
             */
            //  setRemainingSp(getRemainingSp() + 3);
            //    statup.add(new Pair<MapleStat, Integer>(MapleStat.AVAILABLESP,
            //             Integer.valueOf(getRemainingSp())));
        }
        getClient().getSession().write(
                MaplePacketCreator.updatePlayerStats(statup, false, this));
        getMap().broadcastMessage(this,
                MaplePacketCreator.showLevelup(getId()), false);
        recalcLocalStats();
        silentPartyUpdate();
        guildUpdate();
    }

    private void evanFirstAdvance() {
        if (this.level == 10) {
            // this.client.getSession().write(MaplePacketCreator.updateSp(2));
            // this.client.getSession().write(MaplePacketCreator.changeJob((byte)1));
            // this.client.getSession().write(MaplePacketCreator.changeJob((byte)2));
            // this.client.getSession().write(MaplePacketCreator.changeJob((byte)3));
            // this.client.getSession().write(MaplePacketCreator.changeJob((byte)4));
            this.client.getSession().write(
                    MaplePacketCreator.evanTutorial("UI/tutorial/evan/14/0",
                    this.getId()));
            this.client.getSession().write(
                    MaplePacketCreator.龙神信息("孵化器里的蛋中孵化出了幼龙。"));
            this.client.getSession().write(
                    MaplePacketCreator.龙神信息("获得了可以提升龙的技能的3点SP。"));
            this.client.getSession().write(MaplePacketCreator.龙神信息("背包栏增加了。"));
            this.client.getSession().write(
                    MaplePacketCreator.龙神信息("幼龙好像想说话。点击幼龙，和它说话吧"));
        }
        getMap().broadcastMessage(this, MaplePacketCreator.spawnDragon(this),
                true);// 召唤龙龙
        if (this.level >= 20) {
            dropMessage(5, "龙长大了！");
            dropMessage(5, "龙可以使用新的技能了。");
        }
        //  setRemainingSp(getRemainingSp() + 3);
        //  updateSingleStat(MapleStat.AVAILABLESP, getRemainingSp());
        // givedragonSP(getdragonz(), +1);
        // getClient().getSession().write(MaplePacketCreator.updateEvanSP(this));
    }

    public void resetStats() {
        int totAp = getStr() + getDex() + getLuk() + getInt()
                + getRemainingAp();
        setStr(4);
        setDex(4);
        setLuk(4);
        setInt(4);
        setRemainingAp(totAp - 16);
        updateSingleStat(MapleStat.STR, 4);
        updateSingleStat(MapleStat.DEX, 4);
        updateSingleStat(MapleStat.LUK, 4);
        updateSingleStat(MapleStat.INT, 4);
        updateSingleStat(MapleStat.AVAILABLEAP, this.remainingAp);
        this.client.getSession().write(MaplePacketCreator.enableActions());
    }

    public int getDragonLevelWithJobId(int jobId) {
        if (MapleJob.isEvan(jobId)) {
            if (jobId != 2200 && jobId != 2001) {
                return jobId - 2210 + 2;
            } else if (jobId == 2001) {
                return 0;
            } else {
                return 1;
            }
        } else {
            int jobType = jobId % 100;
            if (jobId != 3000) {
                if (jobType == 0) {
                    return 1;
                } else if (jobType == 10) {
                    return 2;
                } else if (jobType == 11) {
                    return 3;
                } else if (jobType == 12) {
                    return 4;
                }
            } else {
                return 0;
            }
        }
        return 0;
    }

    public void changeKeybinding(int key, MapleKeyBinding keybinding) {
        if (keybinding.getType() != 0) {
            keymap.put(Integer.valueOf(key), keybinding);
        } else {
            keymap.remove(Integer.valueOf(key));
        }
    }

    public void sendKeymap() {
        getClient().getSession().write(MaplePacketCreator.getKeymap(keymap));
    }

    public void sendMacros() {
        boolean macros = false;
        for (int i = 0; i < 5; i++) {
            if (skillMacros[i] != null) {
                macros = true;
            }
        }
        if (macros) {
            getClient().getSession().write(
                    MaplePacketCreator.getMacros(skillMacros));
        }
    }

    public void updateMacros(int position, SkillMacro updateMacro) {
        skillMacros[position] = updateMacro;
    }

    public int getWarning() {
        return Warning;
    }

    public void setWarning(int Warning) {
        this.Warning = Warning;
    }

    public void gainWarning(boolean warningEnabled) {
        Warning++;
        if (warningEnabled == true) {
            弹窗("这是你的第" + Warning + "次警告！请注意在游戏中勿使用非法程序！或尝试利用任何BUG！");
            if (Warning > 10) {
                setWarning(0);
                ban("警告次数超额。(10次.)");
            }
        }
    }

    public int getFh() {
        if (getMap().getFootholds().findBelow(this.getPosition()) == null) {
            return 0;
        } else {
            return getMap().getFootholds().findBelow(this.getPosition()).getId();
        }
    }

    public void tempban(String reason, Calendar duration, int greason) {
        if (lastmonthfameids == null) {
            throw new RuntimeException(
                    "Trying to ban a non-loaded character (testhack)");
        }
        tempban(reason, duration, greason, client.getAccID());
        banned = true;
        client.disconnect();
        client.getSession().close(false);
    }

    public static boolean tempban(String reason, Calendar duration,
            int greason, int accountid) {
        try {
            Connection con = DatabaseConnection.getConnection();
            PreparedStatement ps = con.prepareStatement("UPDATE accounts SET tempban = ?, banreason = ?, greason = ? WHERE id = ?");
            Timestamp TS = new Timestamp(duration.getTimeInMillis());
            ps.setTimestamp(1, TS);
            ps.setString(2, reason);
            ps.setInt(3, greason);
            ps.setInt(4, accountid);
            ps.executeUpdate();
            ps.close();
            con.close();
            return true;
        } catch (SQLException ex) {
            log.error("Error while tempbanning", ex);
        }
        return false;
    }

    public void ban(String reason) {
        if (lastmonthfameids == null) {
            throw new RuntimeException(
                    "Trying to ban a non-loaded character (testhack)");
        }
        try {
            Connection con = DatabaseConnection.getConnection();
            PreparedStatement ps = con.prepareStatement("UPDATE accounts SET banned = ?, banreason = ? WHERE id = ?");
            ps.setInt(1, 1);
            ps.setString(2, reason);
            ps.setInt(3, accountid);
            ps.executeUpdate();
            ps.close();
            con.close();
        } catch (SQLException ex) {
            log.error("Error while banning", ex);
        }
        banned = true;
        client.disconnect();
        client.getSession().close(false);
    }

    public void Dci() {
        for (ChannelServer cs : client.getChannelServers()) {
            for (MapleMap _mp : cs.getMapFactory().getMaps().values()) {
                _mp.removePlayer(this);
            }
            cs.removePlayer(this);
        }
        client.disconnect();
    }

    public static boolean ban(String id, String reason, boolean account) {
        try {
            Connection con = DatabaseConnection.getConnection();
            PreparedStatement ps;
            if (id.matches("/[0-9]{1,3}\\..*")) {
                ps = con.prepareStatement("INSERT INTO ipbans VALUES (DEFAULT, ?)");
                ps.setString(1, id);
                ps.executeUpdate();
                ps.close();
                return true;
            }
            if (account) {
                ps = con.prepareStatement("SELECT id FROM accounts WHERE name = ?");
            } else {
                ps = con.prepareStatement("SELECT accountid FROM characters WHERE name = ?");
            }
            boolean ret = false;
            ps.setString(1, id);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                int accountId = rs.getInt(1);
                PreparedStatement psb = con.prepareStatement("UPDATE accounts SET banned = 1, banreason = ? WHERE id = ?");
                psb.setString(1, reason);
                psb.setInt(2, accountId);
                psb.executeUpdate();
                psb.close();

                psb = con.prepareStatement("SELECT ip FROM iplog WHERE accountid = ? ORDER by login DESC LIMIT 1");
                psb.setInt(1, accountId);
                ResultSet rsb = psb.executeQuery();
                rsb.next();
                String to = "/" + rsb.getString("ip");
                rsb.close();
                psb.close();

                psb = con.prepareStatement("SELECT ip FROM ipbans WHERE ip = ?");
                psb.setString(1, to);
                rsb = psb.executeQuery();
                if (!rsb.next()) {
                    PreparedStatement psc = con.prepareStatement("INSERT INTO ipbans VALUES (DEFAULT, ?)");
                    psc.setString(1, to);
                    psc.executeUpdate();
                    psc.close();
                }
                rsb.close();
                psb.close();

                psb = con.prepareStatement("SELECT macs FROM accounts WHERE id = ?");
                psb.setInt(1, accountId);
                rsb = psb.executeQuery();
                rsb.next();
                String macAddress = rsb.getString("macs");
                if (!macAddress.matches("")) {
                    String macs[] = macAddress.split(", ");
                    for (int i = 0; i < macs.length; i++) {
                        PreparedStatement psc = con.prepareStatement("SELECT mac FROM macbans WHERE mac = ?");
                        psc.setString(1, macs[i]);
                        ResultSet rsc = psc.executeQuery();
                        if (!rsc.next()) {
                            PreparedStatement psd = con.prepareStatement("INSERT INTO macbans (mac) VALUES (?)");
                            psd.setString(1, macs[i]);
                            psd.executeUpdate();
                            psd.close();
                        }
                        rsc.close();
                        psc.close();
                    }
                }
                rsb.close();
                psb.close();
                ret = true;
            }
            rs.close();
            ps.close();
            con.close();
            return ret;
        } catch (SQLException ex) {
            log.error("Error while banning", ex);
        }
        return false;
    }

    public static int getAccIdFromCharName(String name) {
        int ret = -1;
        try {
            Connection con = DatabaseConnection.getConnection();
            PreparedStatement ps = con.prepareStatement("SELECT accountid FROM characters WHERE name = ?");
            ps.setString(1, name);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                ret = rs.getInt("accountid");
            }
            rs.close();
            ps.close();
            con.close();
        } catch (SQLException e) {
            log.error("ERROR", e);
        }
        return ret;
    }

    /**
     * Oid of players is always = the cid
     *
     * @return
     */
    @Override
    public int getObjectId() {
        return getId();
    }

    @Override
    public void setObjectId(int value) {
    }

    public MapleStorage getStorage() {
        return storage;
    }

    public int getCurrentMaxHp() {
        return localmaxhp;
    }

    public int getCurrentMaxMp() {
        return localmaxmp;
    }

    public int getCurrentMaxBaseDamage() {
        return localmaxbasedamage;
    }

    public int calculateMaxBaseDamage(int watk) {
        int maxbasedamage;
        if (watk == 0) {
            maxbasedamage = 1;
        } else {
            IItem weapon_item = getInventory(MapleInventoryType.EQUIPPED).getItem((byte) -11);
            boolean barefists = weapon_item == null
                    && (getJob().isA(MapleJob.PIRATE) || getJob().isA(
                    MapleJob.THIEF_KNIGHT));
            if (weapon_item != null || getJob().isA(MapleJob.PIRATE)
                    || getJob().isA(MapleJob.THIEF_KNIGHT)) {
                MapleWeaponType weapon = barefists ? MapleWeaponType.KNUCKLE
                        : MapleItemInformationProvider.getInstance().getWeaponType(weapon_item.getItemId());
                int mainstat;
                int secondarystat;
                if (weapon == MapleWeaponType.BOW
                        || weapon == MapleWeaponType.CROSSBOW) {
                    mainstat = localdex;
                    secondarystat = localstr;
                } else if ((getJob().isA(MapleJob.THIEF) || getJob().isA(
                        MapleJob.NIGHT_KNIGHT))
                        && (weapon == MapleWeaponType.CLAW || weapon == MapleWeaponType.DAGGER)) {
                    mainstat = localluk;
                    secondarystat = localdex + localstr;
                } else if ((getJob().isA(MapleJob.PIRATE) || getJob().isA(
                        MapleJob.THIEF_KNIGHT))
                        && (weapon == MapleWeaponType.GUN)) {
                    mainstat = localdex;
                    secondarystat = localstr;
                } else if ((getJob().isA(MapleJob.PIRATE) || getJob().isA(
                        MapleJob.THIEF_KNIGHT))
                        && (weapon == MapleWeaponType.KNUCKLE)) {
                    mainstat = localstr;
                    secondarystat = localdex;
                } else {
                    mainstat = localstr;
                    secondarystat = localdex;
                }
                maxbasedamage = (int) (((weapon.getMaxDamageMultiplier()
                        * mainstat + secondarystat) / 100.0) * watk);
                maxbasedamage += 10;
            } else {
                maxbasedamage = 0;
            }
        }
        return maxbasedamage;
    }

    public void addVisibleMapObject(MapleMapObject mo) {
        move_lock.lock();
        try {
            if (!visibleMapObjects.contains(mo)) {
                visibleMapObjects.add(mo);
            }
        } finally {
            move_lock.unlock();
        }
    }

    public void removeVisibleMapObject(MapleMapObject mo) {
        move_lock.lock();
        try {
            if (visibleMapObjects.contains(mo)) {
                visibleMapObjects.remove(mo);
            }
        } finally {
            move_lock.unlock();
        }
    }

    public boolean isMapObjectVisible(MapleMapObject mo) {
        return visibleMapObjects.contains(mo);
    }

    public List<MapleMapObject> getVisibleMapObjects() {
        return visibleMapObjects;
    }

    public Lock getMove_lock() {
        return move_lock;
    }

    public boolean isAlive() {
        return this.hp > 0;
    }

    public int getCygnusLinkId() {
        return cygnusLinkId;
    }

    public boolean hasBattleShip() {
        /*   try {
         LinkedList<MapleBuffStatValueHolder> allBuffs = new LinkedList<MapleBuffStatValueHolder>(
         effects.values());
         for (MapleBuffStatValueHolder mbsvh : allBuffs) {
         if (mbsvh.effect.getSourceId() == 5221006) {
         return true;
         }
         }
         } catch (Exception ex) {
         return false;
         }
         return false;*/
        return buffManager.hasBuff(5221006);
    }

    @Override
    public void sendDestroyData(MapleClient client) {
        client.getSession().write(
                MaplePacketCreator.removePlayerFromMap(this.getObjectId()));
    }

    @Override
    public void sendSpawnData(MapleClient client) {
        if ((this.isHidden() && client.getPlayer().getGm() > 0)
                || !this.isHidden()) {
            client.getSession().write(
                    MaplePacketCreator.spawnPlayerMapobject(this));
            if (this.isEvan() && this.getJob().getId() != 2001)// 为了防止召唤玩家的时候无法显示龙龙
            {
                // this.getMap().broadcastMessage(this,
                // MaplePacketCreator.SummonDragon(this, this.getId()), true);
                client.getSession().write(
                        MaplePacketCreator.SummonDragon(this, this.getId()));
            }

            if (getMap().getId() != 910000000) {
                for (MaplePet pett : getPets()) {
                    if (pett != null) {
                        client.getSession().write(
                                MaplePacketCreator.showPet(this, pett, false, false));
                    }
                }
            }


            /*
             * MapleSummon summon = this.getMainsumons(); if (summon != null) {
             * summon.setPosition(this.getPosition());
             * client.getSession().write(
             * MaplePacketCreator.spawnSpecialMapObject(summon,
             * getSkillLevel(SkillFactory.getSkill(summon.getSkill())), true));
             * client
             * .getPlayer().getMap().updateMapObjectVisibility(client.getPlayer
             * (), summon); }
             *
             * summon = this.getRingsumons(); if (summon != null) {
             * summon.setPosition(this.getPosition());
             * client.getSession().write(
             * MaplePacketCreator.spawnSpecialMapObject(summon,
             * getSkillLevel(SkillFactory.getSkill(summon.getSkill())), true));
             * client
             * .getPlayer().getMap().updateMapObjectVisibility(client.getPlayer
             * (), summon); }
             */
            for (List<MapleSummon> integer : summons.values()) {
                for (MapleSummon mapleSummon : integer) {
                    if (this.client == client) {
                        getMap().addMapObject(mapleSummon);
                    }
                    mapleSummon.setPosition(this.getPosition());
                    client.getSession().write(MaplePacketCreator.spawnSpecialMapObject(
                            mapleSummon, getSkillLevel(SkillFactory.getSkill(mapleSummon.getSkill()))));
                    client.getPlayer().getMap().updateMapObjectVisibility(client.getPlayer(),
                            mapleSummon);
                }
            }
            if (android != null && getInventory(MapleInventoryType.EQUIPPED).getItem((byte) -35) != null) {
                client.getSession().write(MaplePacketCreator.spawnAndroid(this, android));
            }
            /*
             * if (hasBuffer(MapleBuffStat.天使戒指)) {
             * client.getSession().write(MaplePacketCreator
             * .giveForeignBuff(this,
             * effects.get(MapleBuffStat.天使戒指).effect.statups,
             * effects.get(MapleBuffStat.天使戒指).effect)); }
             */
        }
    }

    public synchronized void recalcLocalStats() {
        int oldmaxhp = localmaxhp;
        localmaxhp = getMaxhp();
        localmaxmp = getMaxmp();
        localdex = getDex();
        localint_ = getInt();
        localstr = getStr();
        localluk = getLuk();
        int speed = 100;
        int jump = 100;
        magic = localint_;
        watk = 0;
        for (IItem item : getInventory(MapleInventoryType.EQUIPPED)) {

            Equip equip = (Equip) item;
            localmaxhp += equip.getHp();
            localmaxmp += equip.getMp();
            localdex += equip.getDex();
            localint_ += equip.getInt();
            localstr += equip.getStr();
            localluk += equip.getLuk();
            magic += equip.getMatk() + equip.getInt();
            watk += equip.getWatk();
            speed += equip.getSpeed();
            jump += equip.getJump();

            if (equip.getItemId() / 10000 == 166 && equip.getAndroid() != null && getAndroid() == null) {
                setAndroid(equip.getAndroid());
            }
        }
        IItem weapon = getInventory(MapleInventoryType.EQUIPPED).getItem(
                (byte) -11);
        if (weapon == null && getJob().isA(MapleJob.PIRATE)) { // Barefists
            watk += 8;
        }
        magic = Math.min(magic, 2000);
        // 技能增加HP MP上限在这里添加
		/*
         * Integer hbhp = getBuffedValue(MapleBuffStat.HYPERBODYHP); if (hbhp !=
         * null) { localmaxhp += (hbhp.doubleValue() / 100) * localmaxhp; }
         * Integer hbmp = getBuffedValue(MapleBuffStat.HYPERBODYMP); if (hbmp !=
         * null) { localmaxmp += (hbmp.doubleValue() / 100) * localmaxmp; }
         */
        skillAddMaxHp();
        skillAddMaxMp();
        localmaxhp = Math.min(99999, localmaxhp);
        localmaxmp = Math.min(99999, localmaxmp);
        Integer watkbuff = getBuffedValue(MapleBuffStat.WATK);
        if (watkbuff != null) {
            watk += watkbuff.intValue();
        }
        if (job.isA(MapleJob.BOWMAN)) {
            ISkill expert = null;
            if (job.isA(MapleJob.CROSSBOWMASTER)) {
                expert = SkillFactory.getSkill(3220004);
            } else if (job.isA(MapleJob.BOWMASTER)) {
                expert = SkillFactory.getSkill(3120005);
            }
            if (expert != null) {
                int boostLevel = getSkillLevel(expert);
                if (boostLevel > 0) {
                    watk += expert.getEffect(boostLevel).getX();
                }
            }
        }
        Integer matkbuff = getBuffedValue(MapleBuffStat.MATK);
        if (matkbuff != null) {
            magic += matkbuff.intValue();
        }
        Integer speedbuff = getBuffedValue(MapleBuffStat.SPEED);
        if (speedbuff != null) {
            speed += speedbuff.intValue();
        }
        Integer jumpbuff = getBuffedValue(MapleBuffStat.JUMP);
        if (jumpbuff != null) {
            jump += jumpbuff.intValue();
        }
        if (speed > 140) {
            speed = 140;
        }
        if (jump > 123) {
            jump = 123;
        }
        speedMod = speed / 100.0;
        jumpMod = jump / 100.0;
        Integer mount = getBuffedValue(MapleBuffStat.骑宠1);
        if (mount != null) {
            jumpMod = 1.23;
            switch (mount.intValue()) {
                case 1:
                    speedMod = 1.5;
                    break;
                case 2:
                    speedMod = 1.7;
                    break;
                case 3:
                    speedMod = 1.8;
                    break;
                case 5:
                    speedMod = 1.0;
                    jumpMod = 1.0;
                    break;
                default:
                    speedMod = 2.0;
            }
        }
        Integer mWarrior = getBuffedValue(MapleBuffStat.MAPLE_WARRIOR);
        if (mWarrior != null) {
            localstr += (mWarrior.doubleValue() / 100) * localstr;
            localdex += (mWarrior.doubleValue() / 100) * localdex;
            localint_ += (mWarrior.doubleValue() / 100) * localint_;
            localluk += (mWarrior.doubleValue() / 100) * localluk;
        }
        localmaxbasedamage = calculateMaxBaseDamage(watk);
        if (oldmaxhp != 0 && oldmaxhp != localmaxhp) {
            updatePartyMemberHP();
        }


        /*
         * if (getJob().IsDemonHunter()) { localmaxmp = 120; }
         */
    }

    public void Mount(int id, int skillid) {
        maplemount = new MapleMount(this, id, skillid);
    }

    public MapleMount getMount() {
        return maplemount;
    }

    public void equipChanged() {
        getMap().broadcastMessage(this,
                MaplePacketCreator.updateCharLook(this), false);
        recalcLocalStats();
        enforceMaxHpMp();
        if (getClient().getPlayer().getMessenger() != null) {
            WorldChannelInterface wci = client.getChannelServer().getWorldInterface();
            try {
                wci.updateMessenger(getClient().getPlayer().getMessenger().getId(), getClient().getPlayer().getName(),
                        getClient().getChannel());
            } catch (RemoteException e) {
                ServerExceptionHandler.HandlerRemoteException(e);
                getClient().getChannelServer().reconnectWorld();
            }
        }
    }

    public FameStatus canGiveFame(MapleCharacter from) {
        if (lastfametime >= System.currentTimeMillis() - 60 * 60 * 24 * 1000) {
            return FameStatus.NOT_TODAY;
        } else if (lastmonthfameids.contains(Integer.valueOf(from.getId()))) {
            return FameStatus.NOT_THIS_MONTH;
        } else {
            return FameStatus.OK;
        }
    }

    public void hasGivenFame(MapleCharacter to) {
        lastfametime = System.currentTimeMillis();
        lastmonthfameids.add(Integer.valueOf(to.getId()));

        try {
            Connection con = DatabaseConnection.getConnection();
            PreparedStatement ps = con.prepareStatement("INSERT INTO famelog (characterid, characterid_to) VALUES (?, ?)");
            ps.setInt(1, getId());
            ps.setInt(2, to.getId());
            ps.executeUpdate();
            ps.close();
            con.close();
        } catch (SQLException e) {
            log.error("ERROR writing famelog for char " + getName() + " to "
                    + to.getName(), e);
        }
    }

    public MapleParty getParty() {
        return party;
    }

    public int getPartyId() {
        return (party != null ? party.getId() : 0);
    }

    public void setPartyId(int id) {
        ChannelServer cs = client.getChannelServer();
        if (id >= 0 && cs != null) {
            try {
                MapleParty party_MapleParty = cs.getWorldInterface().getParty(id);
                if (party_MapleParty != null
                        && party_MapleParty.getMemberById(id) != null) {
                    this.party = party_MapleParty;
                }
            } catch (RemoteException e) {
                ServerExceptionHandler.HandlerRemoteException(e);
            }
        }
    }

    public boolean getPartyInvited() {
        return partyInvite;
    }

    public void setPartyInvited(boolean invite) {
        this.partyInvite = invite;
    }

    public long getLastSave() {
        return lastSave;
    }

    public void setLastSave(long lastSave) {
        this.lastSave = lastSave;
    }

    public boolean isMuted() {
        if (Calendar.getInstance().after(unmuteTime)) {
            muted = false;
        }
        return muted;
    }

    private boolean getMuted() {
        return this.muted;
    }

    public void setMuted(boolean mute) {
        this.muted = mute;
    }

    public void setUnmuteTime(Calendar time) {
        unmuteTime = time;
    }

    public Calendar getUnmuteTime() {
        return this.unmuteTime;
    }

    private long getUnmutetime() {
        return unmuteTime.getTime().getTime();
    }

    private void setUnmutetime(long value) {
        unmuteTime = Calendar.getInstance();
        unmuteTime.setTime(new java.util.Date(value));
    }

    public int getWorld() {
        return world;
    }

    public void setWorld(int world) {
        this.world = world;
    }

    public void setParty(MapleParty party) {
        this.party = party;
    }

    public int getVanquisherKills() {
        return vanquisherKills;
    }

    public int getVanquisherStage() {
        return vanquisherStage;
    }

    public void setVanquisherKills(int x) {
        this.vanquisherKills = x;
    }

    public void setVanquisherStage(int x) {
        this.vanquisherStage = x;
    }

    public MapleTrade getTrade() {
        return trade;
    }

    public void setTrade(MapleTrade trade) {
        this.trade = trade;
    }

    public EventInstanceManager getEventInstance() {
        return eventInstance;
    }

    public void setEventInstance(EventInstanceManager eventInstance) {
        this.eventInstance = eventInstance;
    }

    public void addDoor(MapleDoor door) {
        doors.add(door);
    }

    public void clearDoors() {
        doors.clear();
    }

    public List<MapleDoor> getDoors() {
        return new ArrayList<MapleDoor>(doors);
    }

    public boolean canDoor() {
        return canDoor;
    }

    public void disableDoor() {
        canDoor = false;
        TimerManager.getInstance().schedule(new Runnable() {
            @Override
            public void run() {
                canDoor = true;
            }
        }, 5000);
    }

    public MapleInventory[] getAllInventories() {
        return inventory;
    }

    /*
     * public Map<Integer, MapleSummon> getSummons() { return summons; }
     */
    public int getChair() {
        return chair;
    }

    public int getItemEffect() {
        return itemEffect;
    }

    public void setChair(int chair) {
        this.chair = chair;
    }

    public void setItemEffect(int itemEffect) {
        this.itemEffect = itemEffect;
    }

    @Override
    public Collection<MapleInventory> allInventories() {
        return Arrays.asList(inventory);
    }

    @Override
    public MapleMapObjectType getType() {
        return MapleMapObjectType.PLAYER;
    }

    public MapleGuild getGuild() {
        try {
            return getClient().getChannelServer().getWorldInterface().getGuild(getGuildid(), mgc);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public int getGuildid() {
        return guildid;
    }

    private void setGuildid(int value) {
        this.guildid = value;
        if (guildid > 0) {
            mgc = new MapleGuildCharacter(this);
        }
    }

    public int getGuildrank() {
        return guildrank;
    }

    private void setGuildrank(int value) {
        this.guildrank = value;
    }

    public void setGuildId(int _id) {
        guildid = _id;
        if (guildid > 0) {
            if (mgc == null) {
                mgc = new MapleGuildCharacter(this);
            } else {
                mgc.setGuildId(guildid);
            }
        } else {
            mgc = null;
        }
    }

    public void setGuildRank(int _rank) {
        guildrank = _rank;
        if (mgc != null) {
            mgc.setGuildRank(_rank);
        }
    }

    public MapleGuildCharacter getMGC() {
        return mgc;
    }

    public void guildUpdate() {
        if (this.guildid <= 0) {
            return;
        }

        mgc.setLevel(this.level);
        mgc.setJobId(this.job.getId());

        try {
            this.client.getChannelServer().getWorldInterface().memberLevelJobUpdate(this.mgc);
        } catch (RemoteException e) {
            ServerExceptionHandler.HandlerRemoteException(e);
        }
    }
    private NumberFormat nf = new DecimalFormat("#,###,###,###");

    public String guildCost() {
        return nf.format(MapleGuild.CREATE_GUILD_COST);
    }

    public String emblemCost() {
        return nf.format(MapleGuild.CHANGE_EMBLEM_COST);
    }

    public String capacityCost() {
        return nf.format(MapleGuild.INCREASE_CAPACITY_COST);
    }

    public void genericGuildMessage(int code) {
        this.client.getSession().write(
                MapleGuild_Msg.genericGuildMessage((byte) code));
    }

    public void disbandGuild() {
        if (guildid <= 0 || guildrank != 1) {
            log.warn(this.name
                    + " tried to disband and he/she is either not in a guild or not leader.");
            return;
        }

        try {
            client.getChannelServer().getWorldInterface().disbandGuild(this.guildid);
        } catch (Exception e) {
            log.error("Error while disbanding guild.", e);
        }
    }

    public void increaseGuildCapacity() {
        if (this.getMeso() < MapleGuild.INCREASE_CAPACITY_COST) {
            client.getSession().write(
                    MaplePacketCreator.serverNotice(1,
                    "You do not have enough mesos."));
            return;
        }

        if (this.guildid <= 0) {
            log.info(this.name
                    + " is trying to increase guild capacity without being in the guild.");
            return;
        }

        try {
            client.getChannelServer().getWorldInterface().increaseGuildCapacity(this.guildid);
        } catch (Exception e) {
            log.error("Error while increasing capacity.", e);
            return;
        }

        this.gainMeso(-MapleGuild.INCREASE_CAPACITY_COST, true, false, true);
    }

    public void saveGuildStatus() {

        try {
            Connection con = DatabaseConnection.getConnection();
            PreparedStatement ps = con.prepareStatement("UPDATE characters SET guildid = ?, guildrank = ? WHERE id = ?");
            ps.setInt(1, this.guildid);
            ps.setInt(2, this.guildrank);
            ps.setInt(3, this.id);
            ps.execute();
            ps.close();
            con.close();
        } catch (SQLException se) {
            log.error("SQL error: " + se.getLocalizedMessage(), se);
        }
    }

    public void setAllianceRank(int rank) {
        allianceRank = rank;
        if (mgc != null) {
            mgc.setAllianceRank(rank);
        }
    }

    public int getAlliancerank() {
        return this.allianceRank;
    }

    private void setAlliancerank(int value) {
        this.allianceRank = value;
    }

    /**
     * Allows you to change someone's NXCash, Maple Points, and Gift Tokens!
     *
     * Created by Acrylic/Penguins
     *
     * @param type : 0 = NX, 1 = MP, 2 = GT
     * @param quantity : how much to modify it by. Negatives subtract points,
     * Positives add points.
     */
    public void modifyCSPoints(int type, int quantity) {
        switch (type) {
            case 0:
                this.paypalnx += quantity;
                break;
            case 1:
                this.maplepoints += quantity;
                break;
            case 4:
                this.cardnx += quantity;
                break;
        }
        UpdateCash();
    }

    public void gainNX(int quantity) {
        this.paypalnx += quantity;
        UpdateCash();
    }

    public int getCSPoints(int type) {
        switch (type) {
            case 0:
                return this.paypalnx;
            case 1:
                return this.maplepoints;
            case 4:
                return this.cardnx;
            default:
                return 0;
        }
    }

    public int getNX() {
        return this.paypalnx;
    }

    private void setNX(int value) {
        this.paypalnx = value;
    }

    public int getBankCash() {
        return 0;
    }

    public boolean haveItem(int itemid, int quantity, boolean checkEquipped,
            boolean exact) {
        // if exact is true, then possessed must be EXACTLY equal to quantity.
        // else, possessed can be >= quantity
        MapleInventoryType type = MapleItemInformationProvider.getInstance().getInventoryType(itemid);
        MapleInventory iv = inventory[type.ordinal()];
        int possessed = iv.countById(itemid);
        if (checkEquipped) {
            possessed += inventory[MapleInventoryType.EQUIPPED.ordinal()].countById(itemid);
        }
        if (exact) {
            return possessed == quantity;
        } else {
            return possessed >= quantity;
        }
    }

    public boolean haveItem(int[] itemids, int quantity, boolean exact) {
        for (int itemid : itemids) {
            MapleInventoryType type = MapleItemInformationProvider.getInstance().getInventoryType(itemid);
            MapleInventory iv = inventory[type.ordinal()];
            int possessed = iv.countById(itemid);
            possessed += inventory[MapleInventoryType.EQUIPPED.ordinal()].countById(itemid);
            if (possessed >= quantity) {
                if (exact) {
                    if (possessed == quantity) {
                        return true;
                    }
                } else {
                    return true;
                }
            }
        }
        return false;
    }

    public int getItemAmount(int itemid) {
        MapleInventoryType type = MapleItemInformationProvider.getInstance().getInventoryType(itemid);
        MapleInventory iv = inventory[type.ordinal()];
        int possesed = iv.countById(itemid);
        return possesed;
    }

    public MapleCSInventory getCSInventory() {
        if (csinventory == null) {
            csinventory = new MapleCSInventory(this);
        }
        return csinventory;
    }

    public boolean isResistance_3500() {
        int jobs = getJob().getId();
        if (jobs == 3500 || jobs <= 3512) {
            return true;
        }
        return false;
    }

    /**
     * @return the beansNum
     */
    public int getBeansNum() {
        return beansNum;
    }

    /**
     * @param beansNum the beansNum to set
     */
    public void setBeansNum(int beansNum) {
        this.beansNum = beansNum;
    }

    /**
     * @return the beansRange
     */
    public int getBeansRange() {
        return beansRange;
    }

    /**
     * @param beansRange the beansRange to set
     */
    public void setBeansRange(int beansRange) {
        this.beansRange = beansRange;
    }

    /**
     * @return the canSetBeansNum
     */
    public boolean isCanSetBeansNum() {
        return canSetBeansNum;
    }

    /**
     * @param canSetBeansNum the canSetBeansNum to set
     */
    public void setCanSetBeansNum(boolean canSetBeansNum) {
        this.canSetBeansNum = canSetBeansNum;
    }

    /**
     * @return the 能否使用进阶灵气
     */
    public boolean is能否使用进阶灵气() {
        return 能否使用进阶灵气;
    }

    /**
     * @param 能否使用进阶灵气 the 能否使用进阶灵气 to set
     */
    public void set能否使用进阶灵气(boolean 能否使用进阶灵气) {
        this.能否使用进阶灵气 = 能否使用进阶灵气;
    }
    private boolean godmode;

    public boolean hasGodmode() {
        return godmode;
    }

    public void setGodmode(boolean onoff) {
        this.godmode = onoff;
    }
    private boolean noEnergyChargeDec = false;

    public void toggleNoEnergyChargeDec() {
        noEnergyChargeDec = !noEnergyChargeDec;
    }

    public boolean isNoEnergyChargeDec() {
        return noEnergyChargeDec && isGM();
    }

    public boolean isLeet() {
        return leetness;
    }
    private boolean leetness;

    public void setLeetness(boolean setTo) {
        leetness = setTo;
    }
    private byte gmtext = 0;

    public void setGMText(byte text) {
        gmtext = text;
    }

    public int getGMText() {
        return gmtext;
    }
    private String linkedName;

    public String getLinkedName() {
        return linkedName;
    }

    public boolean isLinked() {
        return linkedName != null;
    }

    /*
     * private static class MapleBuffStatValueHolder {
     *
     * public MapleStatEffect effect; public long startTime; public int value;
     * public ScheduledFuture<?> schedule;
     *
     * public MapleBuffStatValueHolder(MapleStatEffect effect, long startTime,
     * ScheduledFuture<?> schedule, int value) { super(); this.effect = effect;
     * this.startTime = startTime; this.schedule = schedule; this.value = value;
     * } }
     */
    public static class MapleCoolDownValueHolder {

        public int skillId;
        public long startTime;
        public long length;
        public ScheduledFuture<?> timer;

        public MapleCoolDownValueHolder(int skillId, long startTime,
                long length, ScheduledFuture<?> timer) {
            super();
            this.skillId = skillId;
            this.startTime = startTime;
            this.length = length;
            this.timer = timer;
        }
    }

    public static class SkillEntry {

        public int skillevel;
        public int masterlevel;
        public Timestamp expiredate = null;

        public SkillEntry(int skillevel, int masterlevel) {
            this.skillevel = skillevel;
            this.masterlevel = masterlevel;
        }

        public SkillEntry(int skillevel, int masterlevel, Timestamp expiredate) {
            this(skillevel, masterlevel);
            this.expiredate = expiredate;
        }

        @Override
        public String toString() {
            return skillevel + ":" + masterlevel;
        }

        @Override
        public boolean equals(Object obj) {
            if (obj instanceof SkillEntry) {
                SkillEntry skillEntry = (SkillEntry) obj;
                return skillEntry.hashCode() == this.hashCode();
            }
            return false;
        }

        @Override
        public int hashCode() {
            int hash = 7;
            hash = 31 * hash + this.skillevel;
            hash = 31 * hash + this.masterlevel;
            hash = 31
                    * hash
                    + (this.expiredate != null ? this.expiredate.hashCode() : 0);
            return hash;
        }
    }

    public void expirationTask() {
        Timestamp currenttime2 = new Timestamp(System.currentTimeMillis());
        List<IItem> toberemove = new ArrayList<IItem>();
        for (MapleInventory inv : inventory) {
            for (IItem item : inv.list()) {
                Timestamp expiration = item.getExpiration();
                if (expiration != null) {
                    if (!currenttime2.after(expiration)) {
                        client.getSession().write(MaplePacketCreator.itemExpired(item.getItemId()));
                        toberemove.add(item);
                    }
                }
            }
            for (IItem item : toberemove) {
                MapleInventoryManipulator.removeFromSlot(client, inv.getType(),
                        item.getPosition(), item.getQuantity(), true);
            }
            toberemove.clear();
        }
    }

    public enum FameStatus {

        OK, NOT_TODAY, NOT_THIS_MONTH
    }

    public void forceUpdateItem(MapleInventoryType type, IItem item) {
        client.getSession().write(
                MaplePacketCreator.clearInventoryItem(type, item.getPosition(),
                false));
        client.getSession().write(
                MaplePacketCreator.addInventorySlot(type, item, false));
    }

    public int getBuddyCapacity() {
        return buddylist.getCapacity();
    }

    private void setBuddyCapacity(int value) {
        this.buddylist = new BuddyList(value);
    }

    public void setBuddycapacity(int capacity) {
        buddylist.setCapacity(capacity);
        client.getSession().write(
                MaplePacketCreator.updateBuddyCapacity(capacity));
    }

    public MapleMessenger getMessenger() {
        return messenger;
    }

    public void setMessenger(MapleMessenger messenger) {
        this.messenger = messenger;
    }

    public int getMessenger_() {
        return 0;
    }

    public void setMessenger_(int id) {
        if (id > 0) {
            try {
                WorldChannelInterface wci = client.getChannelServer().getWorldInterface();
                MapleMessenger messenger_ = wci.getMessenger(id);
                if (messenger_ != null) {
                    this.messenger = messenger_;
                }
            } catch (RemoteException e) {
                ServerExceptionHandler.HandlerRemoteException(e);
                client.getChannelServer().reconnectWorld();
            }
        }
    }

    public void startCygnusIntro() {
        client.getSession().write(MaplePacketCreator.CygnusIntroDisableUI(true));
        client.getSession().write(MaplePacketCreator.CygnusIntroLock(true));
        saveLocation(SavedLocationType.CYGNUSINTRO);

        MapleMap introMap = client.getChannelServer().getMapFactory().getMap(913040000);
        changeMap(introMap, introMap.getPortal(0));

        TimerManager.getInstance().schedule(new Runnable() {
            @Override
            public void run() {
                client.getSession().write(
                        MaplePacketCreator.CygnusIntroDisableUI(false));
                client.getSession().write(
                        MaplePacketCreator.CygnusIntroLock(false));
            }
        }, 54 * 1000);
    }

    public void startCygnusIntro_3() {
        client.getSession().write(MaplePacketCreator.CygnusIntroDisableUI(true));
        client.getSession().write(MaplePacketCreator.CygnusIntroLock(true));
        saveLocation(SavedLocationType.CYGNUSINTRO);

        MapleMap introMap = client.getChannelServer().getMapFactory().getMap(0);
        changeMap(introMap, introMap.getPortal(0));

        TimerManager.getInstance().schedule(new Runnable() {
            @Override
            public void run() {
                client.getSession().write(
                        MaplePacketCreator.CygnusIntroDisableUI(false));
                client.getSession().write(
                        MaplePacketCreator.CygnusIntroLock(false));
            }
        }, 15 * 1000);
    }

    public void checkMessenger() {
        if (messenger != null && messengerposition < 4
                && messengerposition > -1) {
            try {
                WorldChannelInterface wci = client.getChannelServer().getWorldInterface();
                MapleMessengerCharacter messengerplayer = new MapleMessengerCharacter(
                        client.getPlayer(), messengerposition);
                wci.silentJoinMessenger(messenger.getId(), messengerplayer,
                        messengerposition);
                wci.updateMessenger(getClient().getPlayer().getMessenger().getId(), getClient().getPlayer().getName(),
                        getClient().getChannel());
            } catch (RemoteException e) {
                ServerExceptionHandler.HandlerRemoteException(e);
                client.getChannelServer().reconnectWorld();
            }
        }
    }

    public int getMessengerPosition() {
        return messengerposition;
    }

    public void setMessengerPosition(int position) {
        if (position < 4 && position > -1 && messenger != null) {
            this.messengerposition = position;
        }
    }

    public int hasEXPCard() {
        int[] expCards = {5210000, 5210001, 5210002, 5210003, 5210004,
            5210005, 5211000, 5211001, 5211002, 5211047};
        MapleInventory iv = getInventory(MapleInventoryType.CASH);
        MapleItemInformationProvider ii = MapleItemInformationProvider.getInstance();
        for (Integer id2 : expCards) {
            if (iv.countById(id2) > 0) {
                if (ii.isExpOrDropCardTime(id2)) {
                    return 2;
                }
            }
        }
        return 1;
    }

    public int hasDropCard() {
        int[] dropCards = {5360000, 5360014, 5360015, 5360016};
        MapleInventory iv = getInventory(MapleInventoryType.CASH);
        MapleItemInformationProvider ii = MapleItemInformationProvider.getInstance();
        for (Integer id3 : dropCards) {
            if (iv.countById(id3) > 0) {
                if (ii.isExpOrDropCardTime(id3)) {
                    return 2;
                }
            }
        }
        return 1;
    }

    public boolean getNXCodeValid(String code, boolean validcode)
            throws SQLException {
        Connection con = DatabaseConnection.getConnection();
        PreparedStatement ps = con.prepareStatement("SELECT `valid` FROM nxcode WHERE code = ?");
        ps.setString(1, code);
        ResultSet rs = ps.executeQuery();
        while (rs.next()) {
            validcode = rs.getInt("valid") == 0 ? false : true;
        }
        rs.close();
        ps.close();
        con.close();
        return validcode;
    }

    public int getNXCodeType(String code) throws SQLException {
        int type = -1;
        Connection con = DatabaseConnection.getConnection();
        PreparedStatement ps = con.prepareStatement("SELECT `type` FROM nxcode WHERE code = ?");
        ps.setString(1, code);
        ResultSet rs = ps.executeQuery();
        while (rs.next()) {
            type = rs.getInt("type");
        }
        rs.close();
        ps.close();
        con.close();
        return type;
    }

    public int getNXCodeItem(String code) throws SQLException {
        int item = -1;
        Connection con = DatabaseConnection.getConnection();
        PreparedStatement ps = con.prepareStatement("SELECT `item` FROM nxcode WHERE code = ?");
        ps.setString(1, code);
        ResultSet rs = ps.executeQuery();
        while (rs.next()) {
            item = rs.getInt("item");
        }
        rs.close();
        ps.close();
        con.close();
        return item;
    }

    public void setNXCodeUsed(String code) throws SQLException {
        Connection con = DatabaseConnection.getConnection();
        PreparedStatement ps = con.prepareStatement("UPDATE nxcode SET `valid` = 0 WHERE code = ?");
        ps.setString(1, code);
        ps.executeUpdate();
        ps = con.prepareStatement("UPDATE nxcode SET `user` = ? WHERE code = ?");
        ps.setString(1, this.getName());
        ps.setString(2, code);
        ps.executeUpdate();
        ps.close();
        con.close();
    }

    public void setInCS(boolean inCS) {
        this.incs = inCS;
    }

    public boolean inCS() {
        return this.incs;
    }

    /*
     * public int getEnergy() { return energybar; }
     */
    public void setInMTS(boolean inMTS) {
        this.inmts = inMTS;
    }

    public boolean inMTS() {
        return this.inmts;
    }

    public void addCooldown(int skillId, long startTime, long length,
            ScheduledFuture<?> timer) {
        if (!hasGMLevel(5)) {
            if (this.coolDowns.containsKey(Integer.valueOf(skillId))) {
                this.coolDowns.remove(skillId);
            }
            this.coolDowns.put(Integer.valueOf(skillId),
                    new MapleCoolDownValueHolder(skillId, startTime, length,
                    timer));
        } else {
            getClient().getSession().write(
                    MaplePacketCreator.skillCooldown(skillId, 0));
        }
    }

    public void giveCoolDowns(final int skillid, long starttime, long length) {
        int time = (int) ((length + starttime) - System.currentTimeMillis());
        ScheduledFuture<?> timer = TimerManager.getInstance().schedule(
                new CancelCooldownAction(this, skillid), time);
        addCooldown(skillid, System.currentTimeMillis(), time, timer);
    }

    public void removeCooldown(int skillId) {
        if (this.coolDowns.containsKey(Integer.valueOf(skillId))) {
            this.coolDowns.remove(Integer.valueOf(skillId));
        }
        getClient().getSession().write(
                MaplePacketCreator.skillCooldown(skillId, 0));
    }

    public boolean skillisCooling(int skillId) {
        return this.coolDowns.containsKey(Integer.valueOf(skillId));
    }

    public List<PlayerCoolDownValueHolder> getAllCooldowns() {
        List<PlayerCoolDownValueHolder> ret = new ArrayList<PlayerCoolDownValueHolder>();
        for (MapleCoolDownValueHolder mcdvh : coolDowns.values()) {
            ret.add(new PlayerCoolDownValueHolder(mcdvh.skillId,
                    mcdvh.startTime, mcdvh.length));
        }
        return ret;
    }

    public static class CancelCooldownAction implements Runnable {

        private int skillId;
        private WeakReference<MapleCharacter> target;

        public CancelCooldownAction(MapleCharacter target, int skillId) {
            this.target = new WeakReference<MapleCharacter>(target);
            this.skillId = skillId;
        }

        @Override
        public void run() {
            MapleCharacter realTarget = target.get();
            if (realTarget != null) {
                realTarget.removeCooldown(skillId);
            }
        }
    }

    public void giveDebuff(MapleDisease disease, MobSkill skill) {
        synchronized (diseases) {
            if (isAlive() && !isActiveBuffedValue(2321005)
                    && !diseases.contains(disease) && diseases.size() < 2) {
                diseases.add(disease);
                List<Pair<MapleDisease, Integer>> debuff = Collections.singletonList(new Pair<MapleDisease, Integer>(disease,
                        Integer.valueOf(skill.getX())));
                long mask = 0;
                for (Pair<MapleDisease, Integer> statup : debuff) {
                    mask |= statup.getLeft().getValue();
                }
                getClient().getSession().write(
                        MaplePacketCreator.giveDebuff(mask, debuff, skill));
                getMap().broadcastMessage(
                        this,
                        MaplePacketCreator.giveForeignDebuff(id, mask, debuff,
                        skill), false);

                if (isAlive() && diseases.contains(disease)) {
                    final MapleCharacter character = this;
                    final MapleDisease disease_ = disease;
                    TimerManager.getInstance().schedule(new Runnable() {
                        @Override
                        public void run() {
                            if (character.diseases.contains(disease_)) {
                                dispelDebuff(disease_);
                            }
                        }
                    }, skill.getDuration());
                }
            }
        }
    }

    public List<MapleDisease> getDiseases() {
        return diseases;
    }

    public void dispelDebuff(MapleDisease debuff) {
        if (diseases.contains(debuff)) {
            diseases.remove(debuff);
            long mask = debuff.getValue();
            getClient().getSession().write(
                    MaplePacketCreator.cancelDebuff(mask));
            getMap().broadcastMessage(this,
                    MaplePacketCreator.cancelForeignDebuff(id, mask), false);
        }
    }

    public void dispelDebuffs() {
        List<MapleDisease> ret = new LinkedList<MapleDisease>(diseases);
        for (MapleDisease disease : ret) {
            if (!disease.equals(MapleDisease.SEDUCE)
                    && !disease.equals(MapleDisease.STUN)) {
                diseases.remove(disease);
                long mask = disease.getValue();
                getClient().getSession().write(
                        MaplePacketCreator.cancelDebuff(mask));
                getMap().broadcastMessage(this,
                        MaplePacketCreator.cancelForeignDebuff(id, mask), false);
            }
        }
    }

    public void dispelDebuffsi() {
        List<MapleDisease> ret = new LinkedList<MapleDisease>(diseases);
        for (MapleDisease disease : ret) {
            if (!disease.equals(MapleDisease.SEAL)) {
                diseases.remove(disease);
                long mask = disease.getValue();
                getClient().getSession().write(
                        MaplePacketCreator.cancelDebuff(mask));
                getMap().broadcastMessage(this,
                        MaplePacketCreator.cancelForeignDebuff(id, mask), false);
            }
        }
    }

    public void cancelAllDebuffs() {
        List<MapleDisease> ret = new LinkedList<MapleDisease>(diseases);
        for (MapleDisease disease : ret) {
            diseases.remove(disease);
            long mask = disease.getValue();
            getClient().getSession().write(
                    MaplePacketCreator.cancelDebuff(mask));
            getMap().broadcastMessage(this,
                    MaplePacketCreator.cancelForeignDebuff(id, mask), false);
        }
    }

    public void setMapId(int PmapId) {
        this.mapid = PmapId;
        ChannelServer cs = client.getChannelServer();
        if (cs != null) {
            MapleMapFactory mapFactory = cs.getMapFactory();
            map = mapFactory.getMap(mapid);
            if (map == null) { // char is on a map that doesn't exist
                // warp it to henesys
                map = mapFactory.getMap(100000000);
            } else if (map.getForcedReturnId() != 999999999) {
                map = mapFactory.getMap(map.getForcedReturnId());
            }
            MaplePortal portal = map.getPortal(initialSpawnPoint);
            if (portal == null) {
                portal = map.getPortal(0); // char is on a spawnpoint
                // that doesn't exist -
                // select the first
                // spawnpoint instead
                initialSpawnPoint = 0;
            }
            setPosition(portal.getPosition());
        }
    }

    public void sendNote(String to, String msg) throws SQLException {
        Connection con = DatabaseConnection.getConnection();
        PreparedStatement ps = con.prepareStatement("INSERT INTO notes (`to`, `from`, `message`, `timestamp`) VALUES (?, ?, ?, ?)");
        ps.setString(1, to);
        ps.setString(2, this.getName());
        ps.setString(3, msg);
        ps.setLong(4, System.currentTimeMillis());
        ps.executeUpdate();
        ps.close();
        con.close();
    }

    public void showNote() {
        try {
            Connection con = DatabaseConnection.getConnection();
            PreparedStatement ps = con.prepareStatement("SELECT * FROM notes WHERE `to` = ?",
                    ResultSet.TYPE_SCROLL_SENSITIVE,
                    ResultSet.CONCUR_UPDATABLE);
            ps.setString(1, this.getName());
            ResultSet rs = ps.executeQuery();
            rs.last();
            int count = rs.getRow();
            rs.first();
            client.getSession().write(MaplePacketCreator.showNotes(rs, count));
            ps.close();
            rs.close();
            con.close();
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
    }

    public void deleteNote(int id) throws SQLException {
        Connection con = DatabaseConnection.getConnection();
        PreparedStatement ps = con.prepareStatement("DELETE FROM notes WHERE `id`=?");
        ps.setInt(1, id);
        ps.executeUpdate();
        ps.close();
        con.close();
    }

    public void showDojoClock() {
        int stage = (map.getId() / 100) % 100;
        long time;
        if (stage % 6 == 0) {
            time = ((stage > 36 ? 15 : stage / 6 + 5)) * 60;
        } else {
            time = (dojoFinish - System.currentTimeMillis()) / 1000;
        }
        client.getSession().write(MaplePacketCreator.getClock((int) time));
        TimerManager.getInstance().schedule(new Runnable() {
            @Override
            public void run() {
                client.getPlayer().changeMap(
                        client.getChannelServer().getMapFactory().getMap(925020000));
            }
        }, time * 1000 + 3000); // let the TIMES UP display for 3 seconds, then
        // warp

    }

    public void checkBerserk() {
        if (BerserkSchedule != null) {
            BerserkSchedule.cancel(false);
        }
        final MapleCharacter chr = this;
        ISkill BerserkX = SkillFactory.getSkill(1320006);
        final int skilllevel = getSkillLevel(BerserkX);
        if (chr.getJob().equals(MapleJob.DARKKNIGHT) && skilllevel >= 1) {
            MapleStatEffect ampStat = BerserkX.getEffect(skilllevel);
            int x = ampStat.getX();
            int HP = chr.getHp();
            int MHP = chr.getMaxhp();
            int ratio = HP * 100 / MHP;
            BerserkSchedule = TimerManager.getInstance().register(
                    new Runnable() {
                        @Override
                        public void run() {
                            // getClient().getSession().write(MaplePacketCreator.showOwnBerserk(skilllevel,
                            // Berserk));
                            // getMap().broadcastMessage(MapleCharacter.this,
                            // MaplePacketCreator.showBerserk(getId(),
                            // skilllevel, Berserk), false);
                        }
                    }, 5000, 3000);
        }
    }

    /*
     * private void prepareBeholderEffect() {
     *
     * if (beholderHealingSchedule != null) {
     * beholderHealingSchedule.cancel(false); } if (beholderBuffSchedule !=
     * null) { beholderBuffSchedule.cancel(false); }
     *
     * ISkill bHealing = SkillFactory.getSkill(1320008); if
     * (getSkillLevel(bHealing) > 0) { final MapleStatEffect healEffect =
     * bHealing.getEffect(getSkillLevel(bHealing)); beholderHealingSchedule =
     * TimerManager.getInstance().register(new Runnable() {
     *
     * @Override public void run() { addHP(healEffect.getHp());
     * getClient().getSession
     * ().write(MaplePacketCreator.showOwnBuffEffect(1321007, 2));
     * getMap().broadcastMessage(MapleCharacter.this,
     * MaplePacketCreator.summonSkill(getId(), 1321007, 5), true);
     * getMap().broadcastMessage(MapleCharacter.this,
     * MaplePacketCreator.showBuffeffect(getId(), 1321007, 2, (byte) 3), false);
     * } }, healEffect.getX() * 1000, healEffect.getX() * 1000); } //灵魂祝福 ISkill
     * bBuffing = SkillFactory.getSkill(1320009); if (getSkillLevel(bBuffing) >
     * 0) { final MapleStatEffect buffEffect =
     * bBuffing.getEffect(getSkillLevel(bBuffing)); beholderBuffSchedule =
     * TimerManager.getInstance().register(new Runnable() { @Override public
     * void run() { buffEffect.applyTo(MapleCharacter.this);
     * getClient().getSession
     * ().write(MaplePacketCreator.beholderAnimation(getId(), 1320009));
     * getMap().broadcastMessage(MapleCharacter.this,
     * MaplePacketCreator.summonSkill(getId(), 1321007, (int) (Math.random() *
     * 3) + 6), true); getMap().broadcastMessage(MapleCharacter.this,
     * MaplePacketCreator.showBuffeffect(getId(), 1321007, 2, (byte) 3), false);
     * } }, buffEffect.getX() * 1000, buffEffect.getX() * 1000); } }
     */

    /*
     * private ScheduledFuture<?> 灵魂祝福物防; private ScheduledFuture<?> 灵魂祝福魔防;
     * private ScheduledFuture<?> 灵魂祝福回避; private ScheduledFuture<?> 灵魂祝福命中;
     * private ScheduledFuture<?> 灵魂祝福攻击;
     */
    /*
     * public void 灵魂祝福处理(MapleBuffStat type, int delay){ final MapleBuffStat
     * Buff = type; if(type == MapleBuffStat.EWdef) { if(灵魂祝福物防 != null)
     * //灵魂祝福物防.cancel(true); log.debug(灵魂祝福物防.cancel(true)); 灵魂祝福物防 =
     * TimerManager.getInstance().schedule(new Runnable() { public void run() {
     * cancelBuffStats(Buff); } }, delay); } else if(type ==
     * MapleBuffStat.EMdef) { if(灵魂祝福魔防 != null) //灵魂祝福魔防.cancel(true);
     * log.debug(灵魂祝福魔防.cancel(true)); 灵魂祝福魔防 =
     * TimerManager.getInstance().schedule(new Runnable() { public void run() {
     * cancelBuffStats(Buff); } }, delay); } else if(type ==
     * MapleBuffStat.AVOID) { if(灵魂祝福回避 != null) //灵魂祝福回避.cancel(true);
     * log.debug(灵魂祝福回避.cancel(true)); 灵魂祝福回避 =
     * TimerManager.getInstance().schedule(new Runnable() { public void run() {
     * cancelBuffStats(Buff); } }, delay); } else if(type == MapleBuffStat.ACC)
     * { if(灵魂祝福命中 != null) //灵魂祝福命中.cancel(true);
     * log.debug(灵魂祝福命中.cancel(true)); 灵魂祝福命中 =
     * TimerManager.getInstance().schedule(new Runnable() { public void run() {
     * cancelBuffStats(Buff); } }, delay); } else if(type ==
     * MapleBuffStat.EWatk) { if(灵魂祝福攻击 != null) //灵魂祝福攻击.cancel(true);
     * log.debug(灵魂祝福攻击.cancel(true)); 灵魂祝福攻击 =
     * TimerManager.getInstance().schedule(new Runnable() { public void run() {
     * cancelBuffStats(Buff); } }, delay); } }
     */
    public void setChalkboard(String text) {
        if (interaction != null) {
            return;
        }
        this.chalktext = text;
        if (chalktext == null) {
            getMap().broadcastMessage(
                    MaplePacketCreator.useChalkboard(this, true));
        } else {
            getMap().broadcastMessage(
                    MaplePacketCreator.useChalkboard(this, false));
        }
    }

    public String getChalkboard() {
        return this.chalktext;
    }

    public int getMarriageQuestLevel() {
        return marriageQuestLevel;
    }

    public void setMarriageQuestLevel(int nf) {
        marriageQuestLevel = nf;
    }

    public void addMarriageQuestLevel() {
        marriageQuestLevel += 1;
    }

    public void subtractMarriageQuestLevel() {
        marriageQuestLevel -= 1;
    }

    public void setCanTalk(int yesno) {
        this.canTalk = yesno;
    }

    public int getCanTalk() {
        return this.canTalk;
    }

    public void setZakumLvl(int level) {
        this.zakumLvl = level;
    }

    public int getZakumLvl() {
        return this.zakumLvl;
    }

    public void addZakumLevel() {
        this.zakumLvl += 1;
    }

    public void subtractZakumLevel() {
        this.zakumLvl -= 1;
    }

    public void setMarryid(int mmm) {
        this.marryid = mmm;
    }

    public void setPartnerid(int pem) {
        this.partnerid = pem;
    }

    public int getPartnerid() {
        return partnerid;
    }

    public int getMarryid() {
        return marryid;
    }

    public MapleCharacter getPartner() {
        MapleCharacter test = this.getClient().getChannelServer().getPlayerStorage().getCharacterById(partnerid);
        if (test != null) {
            return test;
        }
        return null;
    }

    public int countItem(int itemid) {
        MapleInventoryType type = MapleItemInformationProvider.getInstance().getInventoryType(itemid);
        MapleInventory iv = inventory[type.ordinal()];
        int possesed = iv.countById(itemid);
        return possesed;
    }

    public void changePage(int page) {
        this.currentPage = page;
    }

    public void changeTab(int tab) {
        this.currentTab = tab;
    }

    public void changeType(int type) {
        this.currentType = type;
    }

    public int getCurrentPage() {
        return currentPage;
    }

    public int getCurrentTab() {
        return currentTab;
    }

    public int getCurrentType() {
        return currentType;
    }

    public void unstick() {
        try {
            // saveToDB(true); // After some thought, maybe we shouldn't do this
            getClient().getSession().close(false);
            Connection con = DatabaseConnection.getConnection();
            PreparedStatement ps = con.prepareStatement("UPDATE accounts SET loggedin = 0 WHERE id = ?");
            ps.setInt(1, accountid);
            ps.executeUpdate();
            ps.close();
            PreparedStatement ps2 = con.prepareStatement("UPDATE characters SET loggedin = 0 WHERE accountid = ?");
            ps2.setInt(1, id);
            ps2.executeUpdate();
            ps2.close();
            con.close();
        } catch (Exception e) {
        }
    }

    public boolean getSmegaEnabled() {
        return this.smegaEnabled;
    }

    public void setSmegaEnabled(boolean x) {
        this.smegaEnabled = x;
    }

    public void resetAfkTimer() {
        this.afkTimer = System.currentTimeMillis();
    }

    public long getAfkTimer() {
        return System.currentTimeMillis() - this.afkTimer;
    }

    public long getLoggedInTimer() {
        return System.currentTimeMillis() - this.loggedInTimer;
    }

    /*
     * private boolean noEnergyChargeDec = false;
     *
     * public void toggleNoEnergyChargeDec() { noEnergyChargeDec =
     * !noEnergyChargeDec; }
     *
     * public boolean isNoEnergyChargeDec() { return noEnergyChargeDec &&
     * isGM(); }
     *
     *
     * public void handleEnergyChargeGain(int amt, final boolean gm) {
     *
     * if (!gm && isNoEnergyChargeDec()) { return; }
     *
     * ISkill energycharge = SkillFactory.getSkill(5110001); int
     * energyChargeSkillLevel = getSkillLevel(energycharge); MapleStatEffect
     * ceffect = energycharge.getEffect(energyChargeSkillLevel); TimerManager
     * tMan = TimerManager.getInstance(); if (energyDecrease != null) {
     * energyDecrease.cancel(false); } if (energyChargeSkillLevel > 0) { if
     * (energybar < 10000) { energybar = (energybar + amt); if (energybar >
     * 10000) { energybar = 10000; }
     * getClient().getSession().write(MaplePacketCreator
     * .giveEnergyCharge(energybar));
     * getClient().getSession().write(MaplePacketCreator
     * .showOwnBuffEffect(5110001, 2));
     * getMap().broadcastMessage(MaplePacketCreator.showBuffeffect(id, 5110001,
     * 2, (byte) 3));
     *
     * if (energybar == 10000) {
     * getMap().broadcastMessage(MaplePacketCreator.giveForeignEnergyCharge(id,
     * energybar)); }
     *
     * if (!gm) { energyDecrease = tMan.register(new Runnable() { @Override
     * public void run() {
     *
     * if (energybar < 10000 && !isNoEnergyChargeDec()) { if ((energybar - 102)
     * < 0) { energybar = 0; if (energyDecrease != null) {
     * energyDecrease.cancel(false); } } else { energybar = (energybar - 102); }
     * getClient
     * ().getSession().write(MaplePacketCreator.giveEnergyCharge(energybar)); }
     *
     * } }, 10000, 10000); } else { if (energyDecrease != null &&
     * !energyDecrease.isCancelled()) { energyDecrease.cancel(false); }
     * energyDecrease = null; } } if (energybar >= 10000 && energybar < 11000) {
     * energybar = 15000; final MapleCharacter chr = this; if (!gm) {
     * tMan.schedule(new Runnable() {
     *
     * @Override public void run() {
     * getClient().getSession().write(MaplePacketCreator.giveEnergyCharge(0));
     * getMap().broadcastMessage(MaplePacketCreator.giveForeignEnergyCharge(id,
     * energybar)); energybar = 0; } }, ceffect.getDuration()); } }
     *
     * } }
     */
    public void leaveParty() {
        WorldChannelInterface wci = client.getChannelServer().getWorldInterface();
        MaplePartyCharacter partyplayer = new MaplePartyCharacter(this);
        if (party != null) {
            try {
                if (partyplayer.equals(party.getLeader())) { // disband
                    wci.updateParty(party.getId(), PartyOperation.DISBAND,
                            partyplayer);
                    if (getEventInstance() != null) {
                        getEventInstance().disbandParty();
                    }
                } else {
                    wci.updateParty(party.getId(), PartyOperation.LEAVE,
                            partyplayer);
                    if (getEventInstance() != null) {
                        getEventInstance().leftParty(this);
                    }
                }
            } catch (RemoteException e) {
                ServerExceptionHandler.HandlerRemoteException(e);
                getClient().getChannelServer().reconnectWorld();
            }
            setParty(null);
        }
    }

    public int getBossQuestRepeats() {
        return bossRepeats;
    }

    public void setBossQuestRepeats(int repeats) {
        bossRepeats = repeats;
    }

    public void updateBossQuestRepeats() {
        if (Calendar.getInstance().getTimeInMillis() > nextBQ) {
            setBossQuestRepeats(0);
        }
    }

    private long getNextBQ() {
        return nextBQ;
    }

    private void setNextBQ(long nextBQ) {
        this.nextBQ = nextBQ;
    }

    public void updateNextBossQuest() {
        this.nextBQ = Calendar.getInstance().getTimeInMillis()
                + (1000 * 60 * 60 * 24);
    }

    public String getNextBossQuest() {
        return new Timestamp(this.nextBQ).toString();
    }

    public void setBossPoints(int points) {
        bossPoints = points;
    }

    public int getBossPoints() {
        return bossPoints;
    }

    public void setAchievementFinished(int id) {
        finishedAchievements.add(id);
    }

    public boolean achievementFinished(int achievementid) {
        return finishedAchievements.contains(achievementid);
    }

    public void finishAchievement(int id) {
        if (!achievementFinished(id)
                || MapleAchievements.getInstance().getById(id).isRepeatable()) {
            if (isAlive()) {
                MapleAchievement ma = MapleAchievements.getInstance().getById(
                        id);
                if (ma != null) {
                    ma.finishAchievement(this);
                }
            }
        }
    }

    public List<Integer> getFinishedAchievements() {
        return finishedAchievements;
    }

    public boolean hasMerchant() {
        return client.getChannelServer().hasHiredMerchant(id);
    }

    public boolean tempHasItems() {
        boolean value = false;
        try {
            Connection con = DatabaseConnection.getConnection();
            PreparedStatement ps = con.prepareStatement("SELECT ownerid FROM hiredmerchanttemp WHERE ownerid = ?");
            ps.setInt(1, getId());
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                value = true;
            }
            rs.close();
            ps.close();
            con.close();
        } catch (SQLException se) {
            log.error("tempHasItems sql", se);
        }
        return value;
    }

    public int getBossLog(String boss) {
        try {
            Connection con = DatabaseConnection.getConnection();
            int count;
            PreparedStatement ps;
            ps = con.prepareStatement("SELECT COUNT(*) FROM bosslog WHERE characterid = ? AND bossid = ? AND lastattempt >= subtime(CURRENT_TIMESTAMP, '1 0:0:0.0')");
            ps.setInt(1, id);
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
            log.error("Error while read bosslog.", Ex);
            return -1;
        }
    }

    public int getBossLogCount(String boss) {
        int count = -1;
        try {
            Connection con = DatabaseConnection.getConnection();
            PreparedStatement ps = con.prepareStatement("SELECT COUNT(*) FROM bosslog WHERE characterid = ? AND bossid = ?");
            ps.setInt(1, this.id);
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
        } catch (Exception Ex) {
            log.error("Error while read bosslog.", Ex);
        }
        return count;
    }

    public void setBossLog(String boss) {
        try {
            Connection con = DatabaseConnection.getConnection();
            PreparedStatement ps;
            ps = con.prepareStatement("insert into bosslog (characterid, bossid) values (?,?)");
            ps.setInt(1, id);
            ps.setString(2, boss);
            ps.executeUpdate();
            ps.close();
            con.close();
        } catch (Exception Ex) {
            log.error("Error while insert bosslog.", Ex);
        }
    }

    public void createPlayerNPC() {
        getPlayerNPC().createPlayerNPC(this, getPlayerNPCMapId());
    }

    public int getPlayerNPCMapId() {
        int jobId = getJob().getId();
        if (jobId >= 100 && jobId <= 132) {
            return 102000003;
        } else if (jobId >= 200 && jobId <= 232) {
            return 101000003;
        } else if (jobId >= 300 && jobId <= 322) {
            return 100000201;
        } else if (jobId >= 400 && jobId <= 422) {
            return 103000003;
        } else if (jobId >= 500 && jobId <= 532) {
            return 120000000;
        } else {
            return 104000000;
        }
    }

    public MaplePlayerNPC getPlayerNPC() {
        MaplePlayerNPC pnpc = new MaplePlayerNPC(this);
        return pnpc;
    }

    public String getJobName() {
        return job.getJobNameAsString();
    }

    public boolean hasPlayerNPC() {
        return playerNPC;
    }

    public boolean getPlayerNpc() {
        return playerNPC;
    }

    public void setPlayerNpc(boolean playerNPC) {
        this.playerNPC = playerNPC;
    }

    public void handleBattleShipHpLoss(int damage) {
        ISkill ship = SkillFactory.getSkill(5221006);
        int maxshipHP = (getSkillLevel(ship) * 4000)
                + ((getLevel() - 120) * 2000);
        MapleStatEffect effect = ship.getEffect(getSkillLevel(ship));
        battleshipHP -= damage;
        if (getBattleShipHP() <= 0) {
            dispelSkill(5221006);
            ScheduledFuture<?> timer = TimerManager.getInstance().schedule(
                    new CancelCooldownAction(this, 5221006),
                    effect.getCooldown() * 1000);
            addCooldown(5221006, System.currentTimeMillis(),
                    effect.getCooldown() * 1000, timer);
            battleshipHP = maxshipHP;
            getClient().getSession().write(
                    MaplePacketCreator.skillCooldown(5221006,
                    effect.getCooldown()));
            try {
                dropMessage("Your Battle Ship has been destroyed by the monster with incredible force!");
            } catch (NullPointerException npe) {
            }
        }
        getClient().getSession().write(
                MaplePacketCreator.updateBattleShipHP(this.getId(),
                this.battleshipHP));
    }

    public int getBattleShipHP() {
        return battleshipHP;
    }

    public int setBattleShipHP(int set) {
        return battleshipHP = set;
    }

    public List<Integer> getTRockMaps(int type) {
        List<Integer> rockmaps = new LinkedList<Integer>();
        try {
            PreparedStatement ps;
            if (type == 1) {
                ps = DatabaseConnection.getConnection().prepareStatement(
                        "SELECT mapid FROM trocklocations WHERE characterid = ? AND type = ? LIMIT 10");
            } else {
                ps = DatabaseConnection.getConnection().prepareStatement(
                        "SELECT mapid FROM trocklocations WHERE characterid = ? AND type = ? LIMIT 5");
            }
            ps.setInt(1, id);
            ps.setInt(2, type);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                rockmaps.add(rs.getInt("mapid"));
            }
            rs.close();
            ps.getConnection().close();
            ps.close();
        } catch (SQLException se) {
            return null;
        }
        return rockmaps;
    }

    public void checkDuey() {

        try {
            Connection con = DatabaseConnection.getConnection();
            PreparedStatement ps = con.prepareStatement("SELECT * FROM dueypackages WHERE receiverid = ? AND alerted = 0");
            ps.setInt(1, getId());
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                PreparedStatement ps2 = con.prepareStatement("UPDATE dueypackages SET alerted = 1 WHERE receiverid = ?");
                ps2.setInt(1, getId());
                ps2.executeUpdate();
                ps2.close();
                getClient().getSession().write(
                        MaplePacketCreator.sendDueyMessage(Actions.TOCLIENT_PACKAGE_MSG.getCode()));
            }
            rs.close();
            ps.close();
            con.close();
        } catch (SQLException SQLe) {
            SQLe.printStackTrace();
        }
    }

    public boolean isCPQChallenged() {
        return CPQChallenged;
    }

    public void setCPQChallenged(boolean CPQChallenged) {
        this.CPQChallenged = CPQChallenged;
    }

    public int getCP() {
        return CP;
    }

    public void gainCP(int gain) {
        if (gain > 0) {
            this.setTotalCP(this.getTotalCP() + gain);
        }
        this.setCP(this.getCP() + gain);
        if (this.getParty() != null) {
            this.getMonsterCarnival().setCP(
                    this.getMonsterCarnival().getCP(team) + gain, team);
            if (gain > 0) {
                this.getMonsterCarnival().setTotalCP(
                        this.getMonsterCarnival().getTotalCP(team)
                        + gain, team);
            }
        }
        if (this.getCP() > this.getTotalCP()) {
            this.setTotalCP(this.getCP());
        }
        this.getClient().getSession().write(MaplePacketCreator.CPUpdate(false, this.getCP(),
                this.getTotalCP(), getTeam()));
        if (this.getParty() != null && getTeam() != -1) {
            this.getMap().broadcastMessage(
                    MaplePacketCreator.CPUpdate(true, this.getMonsterCarnival().getCP(team),
                    this.getMonsterCarnival().getTotalCP(team),
                    getTeam()));
        } else {
            log.warn(getName() + " is either not in a party or .. team: "
                    + getTeam());
        }
    }

    public void setTotalCP(int a) {
        this.totalCP = a;
    }

    public void setCP(int a) {
        this.CP = a;
    }

    public int getTotalCP() {
        return totalCP;
    }

    public void resetCP() {
        this.CP = 0;
        this.totalCP = 0;
        this.monsterCarnival = null;
    }

    public MapleMonsterCarnival getMonsterCarnival() {
        return monsterCarnival;
    }

    public void setMonsterCarnival(MapleMonsterCarnival monsterCarnival) {
        this.monsterCarnival = monsterCarnival;
    }

    public int getTeam() {
        return team;
    }

    public void setTeam(int team) {
        this.team = team;
    }

    public int getCPQRanking() {
        return CPQRanking;
    }

    public void setCPQRanking(int newCPQRanking) {
        this.CPQRanking = newCPQRanking;
    }

    public boolean isBanned() {
        return banned;
    }

    public boolean needsParty() {
        return needsParty;
    }

    public int getNeedsPartyMaxLevel() {
        return needsPartyMaxLevel;
    }

    public int getNeedsPartyMinLevel() {
        return needsPartyMinLevel;
    }

    public void setNeedsParty(boolean bool, int minlvl, int maxlvl) {
        needsParty = bool;
        needsPartyMinLevel = minlvl;
        needsPartyMaxLevel = maxlvl;
    }

    public boolean hasPlayerShopTicket() {
        int[] itemids = new int[6]; // list of playerstore coupons
        for (int Id = 0; Id <= 5; Id++) {
            itemids[Id] = (Id + 5140000);
        }
        return haveItem(itemids, 1, false);
    }

    public boolean hasHiredMerchantTicket() {
        int[] itemids = new int[13]; // list of hired merchant store coupons
        for (int Id = 0; Id <= 12; Id++) {
            itemids[Id] = (Id + 5030000);
        }
        return haveItem(itemids, 1, false);
    }

    public static int getIdByName(String name) {
        int ret = -1;
        try {
            Connection con = DatabaseConnection.getConnection();
            PreparedStatement ps;
            ps = con.prepareStatement("SELECT id FROM characters WHERE name = ?");
            ps.setString(1, name);
            ResultSet rs = ps.executeQuery();
            if (!rs.next()) {
                ps.close();
                ret = -1;
            }
            ret = rs.getInt("id");
            ps.close();
            rs.close();
            con.close();
        } catch (SQLException e) {
            log.error("ERROR", e);
        }
        return ret;
    }

    public static int getAccountIdByName(String name) {
        return getAccIdFromCharName(name);
    }

    public void addToCancelBuffPackets(MapleStatEffect effect, long startTime) {
        buffsToCancel.put(startTime, effect);
    }

    public void cancelSavedBuffs() {
        Set keys = buffsToCancel.keySet();
        Object[] keysarray = keys.toArray();
        long key = 0;
        for (Object o : keysarray) {
            key = (Long) o;
            cancelEffect(buffsToCancel.get(key), false, key);
        }
        buffsToCancel.clear();
    }

    public boolean isQuestDebug() {
        return questDebug && isGM();
    }

    public void toggleQuestDebug() {
        questDebug = !questDebug;
    }

    public void UpdateCash() {
        getClient().getSession().write(MaplePacketCreator.showCharCash(this));
    }

    public final int getNumQuest() {
        int i = 0;
        for (MapleQuestStatus q : quests.values()) {
            if (q.getStatus() == MapleQuestStatus.Status.COMPLETED
                    && !(q.getQuest() instanceof MapleCustomQuest)) {
                i++;
            }
        }
        return i;
    }

    public static long getNextUniqueId() {
        return UniqueIdNumber.getAndIncrement();
    }

    public boolean getInvincible() {
        return invincible;
    }

    public void setInvincible(boolean set) {
        this.invincible = set;
    }

    public void DoJoKill() {
        getClient().getSession().write(
                MaplePacketCreator.environmentChange("Dojang/clear", 4));
        getClient().getSession().write(
                MaplePacketCreator.environmentChange("dojang/end/clear", 3));
    }

    public boolean hasEnergyCharge() {
        int skillId;
        if (getJob() == MapleJob.MARAUDER) {
            skillId = 5110001;
        } else if (getJob() == MapleJob.THIEF_KNIGHT_2) {
            skillId = 15100004;
        } else {
            return false;
        }
        if (getSkillLevel(skillId) > 0) {
            return true;
        }
        return false;
    }

    public int getEnergyPoint() {
        return energyPoint;
    }

    public void setEnergyPoint(int i) {
        energyPoint = i;
        List<Pair<MapleBuffStat, Integer>> statups = Collections.singletonList(new Pair<MapleBuffStat, Integer>(
                MapleBuffStat.能量获得, energyPoint));
        /*   getClient().getSession().write(
         MaplePacketCreator.givePirateBuff(0, 200, statups));*/
    }

    public boolean 是拳手(int jobid) {
        if (jobid == 500 || jobid == 510 || jobid == 511 || jobid == 512) {
            return true;
        }
        return false;
    }

    public boolean 是奇袭者(int jobid) {
        if (jobid >= 1500 && jobid <= 1512) {
            return true;
        }
        return false;
    }

    public void increaseEnergyCharge(int numMonsters) {
        // 增加能量 能量获得
        if (energyPoint < 10000 && numMonsters > 0) {
            if (energyChargeSchedule != null) {
                this.energyChargeSchedule.cancel(false);
                this.energyChargeSchedule = null;
            }
            int skillId;
            if (是拳手(getJob().getId())) {
                skillId = 5110001;
            } else if (是奇袭者(getJob().getId())) {
                skillId = 15100004;
            } else {
                return;
            }
            ISkill skill = SkillFactory.getSkill(skillId);
            int skillLevel = getSkillLevel(skill);
            int x = 0;
            if (skillLevel > 0) {
                x = skill.getEffect(skillLevel).getX();
            }
            int toAdd = x * numMonsters;
            this.energyPoint += toAdd;
            if (energyPoint >= 10000) {
                this.energyPoint = 10000;
                skill.getEffect(skillLevel).applyTo(this);
                return;
            } else {
                List<Pair<MapleBuffStat, Integer>> statups = Collections.singletonList(new Pair<MapleBuffStat, Integer>(
                        MapleBuffStat.能量获得, energyPoint));
                //             getClient().getSession().write(
                //                     MaplePacketCreator.givePirateBuff(0, 200, statups));
                getClient().getSession().write(
                        MaplePacketCreator.showOwnBuffEffect(skillId, 2,
                        getSkillLevel(skillId))); // 显示能量获得的效果(有光聚集过来)
                getMap().broadcastMessage(
                        this,
                        MaplePacketCreator.综合技能状态(id, skillId, 2,
                        getSkillLevel(skillId)), false);
            }
            // 能量递减
            // this.energyChargeSchedule =
            // TimerManager.getInstance().register(new
            // ReduceEnergyChargeAction(this), 10000, 10000);
            energyChargeSchedule = TimerManager.getInstance().register(
                    new Runnable() {
                        @Override
                        public void run() {
                            energyPoint -= 200;
                            List<Pair<MapleBuffStat, Integer>> statups = Collections.singletonList(new Pair<MapleBuffStat, Integer>(
                            MapleBuffStat.能量获得, energyPoint));
                            //                       getClient().getSession().write(
                            //                              MaplePacketCreator.givePirateBuff(0, 200,
                            //                                statups));
                            if (energyPoint <= 0) {
                                energyPoint = 0;
                                energyChargeSchedule.cancel(false);
                            }
                        }
                    }, 10000, 10000);
        }
    }

    public boolean inIntro() {
        return tutorial;
    }

    public void blockPortal(String scriptName) {
        if (!blockedPortals.contains(scriptName) && scriptName != null) {
            blockedPortals.add(scriptName);
        }
        // getClient().getSession().write(MaplePacketCreator.blockedPortal());
    }

    public void unblockPortal(String scriptName) {
        if (blockedPortals.contains(scriptName) && scriptName != null) {
            blockedPortals.remove(scriptName);
        }
    }

    public List getBlockedPortals() {
        return blockedPortals;
    }

    public boolean getAranIntroState(String mode) {
        if (ares_data.contains(mode)) {
            return true;
        }
        return false;
    }

    public int setLastAttack(int attackZ) {
        return lastAttack = attackZ;
    }

    public int getLastAttack() {
        return lastAttack;
    }

    public boolean inTutorialMap() {
        if (getMap().getId() >= 914000000 && getMapId() <= 914010200
                || getMapId() >= 108000700 && getMapId() <= 140090500
                || getMapId() >= 0 && getMapId() <= 2000001) {
            return true;
        }
        return false;
    }

    public void addAreaData(int quest, String data) {
        if (!this.ares_data.contains(data)) {
            this.ares_data.add(data);
            try {
                Connection con = DatabaseConnection.getConnection();
                PreparedStatement ps = con.prepareStatement("INSERT INTO char_ares_info VALUES (DEFAULT, ?, ?, ?)");
                ps.setInt(1, getId());
                ps.setInt(2, quest);
                ps.setString(3, data);
                ps.executeUpdate();
                ps.close();
                con.close();
            } catch (SQLException ex) {
                log.error("Arsa date error", ex);
            }
        }
    }

    public void removeAreaData() {
        this.ares_data.clear();
        try {
            Connection con = DatabaseConnection.getConnection();
            PreparedStatement ps = con.prepareStatement("DELETE FROM char_ares_info WHERE charid = ?");
            ps.setInt(1, getId());
            ps.executeUpdate();
            ps.close();
            con.close();
        } catch (SQLException ex) {
            log.error("Arsa date error", ex);
            ex.printStackTrace();
        }
    }

    /*
     * public void maxAllSkills() { MapleDataProvider dataProvider =
     * MapleDataProviderFactory.getDataProvider(new
     * File(System.getProperty("net.sf.odinms.wzpath") + "/" + "String.wz"));
     * MapleData skilldData = dataProvider.getData("Skill.img"); for (MapleData
     * skill_ : skilldData.getChildren()) { try { ISkill skill =
     * SkillFactory.getSkill(Integer.parseInt(skill_.getName())); if (GMLevel >
     * 0) { changeSkillLevel(skill, skill.getMaxLevel(), skill.getMaxLevel()); }
     * } catch (NumberFormatException nfe) { break; } catch
     * (NullPointerException npe) { continue; } } }
     */
    public long getLasttime() {
        return this.lasttime;
    }

    public void setLasttime(long lasttime) {
        this.lasttime = lasttime;
    }

    public ScheduledFuture<?> getMapTimeLimitTask() {
        return mapTimeLimitTask;
    }

    // 定义双刀职业
    public boolean isSD() {
        int jobs = getJob().getId();
        if (jobs >= 430 && jobs <= 434) {
            return true;
        }
        return false;
    }

    // 定义添加龙神职业的方法
    public boolean isEvan() {
        int jobs = getJob().getId();
        if (jobs == 2001 || jobs >= 2200 && jobs <= 2218) {
            return true;
        }
        return false;
    }

    // 定义双弩职业
    public boolean isSN() {
        int jobs = getJob().getId();
        if (jobs >= 2300 && jobs <= 2312) {
            return true;
        }
        return false;
    }

    // 定义恶魔猎手职业
    public boolean isEMLS() {
        int jobs = getJob().getId();
        if (jobs >= 3100 && jobs <= 3112) {
            return true;
        }
        return false;
    }

    public int getDragonSkill(int skillid) { // 取得指定龙神技能是第几转
        int a = skillid / 10000;
        if (a == 2200) {
            a = 1;
        } else {
            a -= 2208;
        }
        return a;
    }

    public int getBigBangJob() {
        int jobs = getJob().getId();
        if (jobs == 3200 || jobs == 3300 || jobs == 3500) {
            return 1;
        } else if (jobs == 3210 || jobs == 3310 || jobs == 3510) {
            return 2;
        } else if (jobs == 3211 || jobs == 3311 || jobs == 3511) {
            return 3;
        } else if (jobs == 3212 || jobs == 3312 || jobs == 3512) {
            return 4;
        }
        return 0;
    }

    public int getBigBang(int skillid) {
        if ((skillid >= 32000000 && skillid < 32100000)
                || (skillid >= 33000000 && skillid < 33100000)
                || (skillid >= 35000000 && skillid < 35100000)) {
            return 1;
        } else if ((skillid >= 32100000 && skillid < 32110000)
                || (skillid >= 33100000 && skillid < 33110000)
                || (skillid >= 35100000 && skillid < 35110000)) {
            return 2;
        } else if ((skillid >= 32110000 && skillid < 32120000)
                || (skillid >= 33110000 && skillid < 33120000)
                || (skillid >= 35110000 && skillid < 35120000)) {
            return 3;
        } else if ((skillid >= 32120000 && skillid < 32130000)
                || (skillid >= 33120000 && skillid < 33130000)
                || (skillid >= 35120000 && skillid < 35130000)) {
            return 4;
        }
        return 0;
    }

    public boolean isResistance() { // 反抗者
        int jobs = getJob().getId();
        if (jobs >= 3000 && jobs <= 3600) {
            return true;
        }
        return false;
    }

    public boolean getCygnusBless() {
        SkillEntry ret1 = skills.get(SkillFactory.getSkill(12));
        SkillEntry ret2 = skills.get(SkillFactory.getSkill(10000012));
        SkillEntry ret3 = skills.get(SkillFactory.getSkill(20000012));
        SkillEntry ret4 = skills.get(SkillFactory.getSkill(20010012));
        SkillEntry ret5 = skills.get(SkillFactory.getSkill(30000012));
        if (ret1 != null || ret2 != null || ret3 != null || ret4 != null
                || ret5 != null) {
            return true;
        }
        return false;
    }

    public void increaseEquipExp(int mobexp) {
        MapleItemInformationProvider mii = MapleItemInformationProvider.getInstance();
        for (IItem item : getInventory(MapleInventoryType.EQUIPPED).list()) {
            Equip nEquip = (Equip) item;
            String itemName = mii.getName(nEquip.getItemId());
            if (itemName == null) {
                continue;
            }
            // log.debug("执行1");
            if ((itemName.contains("重生") && nEquip.getItemLevel() < 4)
                    || itemName.contains("永恒") && nEquip.getItemLevel() < 6) {
                // log.debug("执行2");
                nEquip.gainItemExp(client, mobexp, itemName.contains("永恒"));
            }
        }
    }

    public boolean isAran() {
        int jobs = getJob().getId();
        if (jobs == 2000 || jobs >= 2100 && jobs <= 2112) {
            return true;
        }
        return false;
    }

    public void handleComboGain() {
        if (combo <= 30000) {
            combo += 1;
            if (combo > 30000) {
                combo = 30000;
            }
        }
        if (isAran() || getJob().getId() == 900) {
            getClient().getSession().write(
                    MaplePacketCreator.Combo_Effect(combo));
        }
    }

    public void handle_JiShaDian_Gain() {
        if (combo < 5 && (getJobid() == 422)) {
            combo += 1;
            getClient().getSession().write(MaplePacketCreator.giveBuff(combo, 0,
                    MapleStatEffect.侠盗本能, this));
        }
    }

    public void setArmorTimes(int i) {
        armorTimes = i;
    }

    public void gainArmorTimes(int i) {
        armorTimes += i;
    }

    public int getArmorTimes() {
        return armorTimes;
    }

    public boolean isMechinic() { // 机械师
        int jobs = getJob().getId();
        if (jobs >= 3500 && jobs <= 3600) {
            return true;
        }
        return false;
    }

    public MaplePlayerBuffManager getBuffManager() {
        return buffManager;
    }

    public boolean isMapler() {
        int jobs = getJob().getId();
        if (jobs >= 0 && jobs < 1000) {
            return true;
        }
        return false;
    }

    public boolean isKnights() {
        int jobs = getJob().getId();
        if (jobs >= 1000 && jobs < 2000) {
            return true;
        }
        return false;
    }

    // 不消耗标/子弹的远程攻击技能
    public boolean useNoProjectile(int skillid) {
        return getBuffedValue(MapleBuffStat.SHADOW_CLAW) != null // 无形箭
                || getBuffedValue(MapleBuffStat.SOULARROW) != null // 暗器伤人
                || skillid == 战神.斗气爆裂
                || skillid == 战神.幻影狼牙
                || skillid == 战神.钻石星辰
                || skillid == 魂骑士.灵魂之刃
                || skillid == 夜行者.吸血 || skillid == 奇袭者.鲨鱼波
                || skillid == 拳手.超能量
                || skillid == 弩骑.吞噬_攻击 || skillid == 弓手.强弓 || isMechinic();
    }

    public MapleSummon getSummon(int skillid) {
        List<MapleSummon> valueList = summons.get(skillid);
        if (valueList == null) {
            // log.debug("目前不存在召唤兽");
            return null;
        }
        return valueList.get(0);
    }

    public void removeSummon(int skillid) {
        List<MapleSummon> valueList = summons.get(skillid);
        if (valueList.size() == 1) {
            // 如果这个排列里只有一个召唤兽数据了 但是又要remove 就把整个键值对取消
            // 因为remove之后技能对应的召唤兽数据也是空的 没必要留着
            summons.remove(skillid);
            // log.debug("取消整个skillid对应的summon");
        } else {
            // 这里严谨来说不应该这样去除 应该判断召唤兽的objectid再去除
            // 但实在是麻烦 这个按顺序去除一般100个才会出现一次问题
            // 基本没问题 所以暂时就这样吧
            summons.get(skillid).remove(0);
            // log.debug("取消整个skillid对应的summon其中一个");
        }
    }

    public void putSummon(int skillid, MapleSummon summon) {
        List<MapleSummon> valueList = summons.get(skillid);
        List<MapleSummon> ret = new LinkedList<MapleSummon>();
        ret.add(summon);
        if (valueList == null) {
            // log.debug("添加未存在的召唤兽数据");
            summons.put(skillid, ret);
        } else {
            // log.debug("添加已存在的召唤兽数据");
            summons.get(skillid).add(summon);
        }
    }

    // 重构召唤兽
    public synchronized Map<Integer, List<MapleSummon>> getSummons() {
        return summons;
    }

    public void addDoor2(MapleDoor2 door) {
        doors2.add(door);
    }

    public void clearDoors2() {
        doors2.clear();
    }

    public void reRemoveDoors2(MapleDoor2 door) {
        doors2.remove(door);
    }

    public List<MapleDoor2> getDoors2() {
        return new ArrayList<MapleDoor2>(doors2);
    }

    public void cancelDoor2(final MapleDoor2 door, int time) {
        if (door.isFirst()) {
            door2_1 = TimerManager.getInstance().schedule(new Runnable() {
                @Override
                public void run() {
                    // log.debug("取消机械的门A");
                    reRemoveDoors2(door);
                    getMap().removeMapObject(door);
                    getMap().broadcastMessage(door.makeDestroyData(client));
                }
            }, time); // 多久之后取消
        } else {
            door2_2 = TimerManager.getInstance().schedule(new Runnable() {
                @Override
                public void run() {
                    // log.debug("取消机械的门B");
                    reRemoveDoors2(door);
                    getMap().removeMapObject(door);
                    getMap().broadcastMessage(door.makeDestroyData(client));
                }
            }, time); // 多久之后取消
        }
    }

    // 取消用于取消机械传送门的进程
    public void cancelDoor2() {
        // log.info("全部传送门线程取消");
        if (door2_1 != null) {
            door2_1.cancel(false);
        }
        if (door2_2 != null) {
            door2_2.cancel(false);
        }
    }

    public void cancelDoor2_1() {
        // log.info("取消机械师的门A线程");
        if (door2_1 != null) {
            door2_1.cancel(false);
        }
    }

    public void cancelDoor2_2() {
        // log.info("取消机械师的门B线程");
        if (door2_2 != null) {
            door2_2.cancel(false);
        }
    }

    public int getSummonAmount(int skillid) {
        List<MapleSummon> valueList = summons.get(skillid);
        int i = 0;
        if (valueList != null) {
            i = valueList.size();
            // log.debug("召唤兽数量为："+i);
        } else {
            // log.debug("召唤兽数量为0");
        }
        return i;
    }

    public void 设置吞噬的怪id(int i) {
        吞噬的monsteroid = i;
    }

    public int 获得吞噬的怪的id() {
        int i = 吞噬的monsteroid;
        吞噬的monsteroid = 0;
        return i;
    }

    public MapleUseMount getUsingMount() {
        return useMount;
    }

    public void setUsingMount(int itemid, int skillid) {
        useMount = new MapleUseMount(this, itemid, skillid);
    }

    /**
     * @return the charisma
     */
    public int getCharisma() {
        return charisma;
    }

    /**
     * @param charisma the charisma to set
     */
    public void setCharisma(int charisma) {
        this.charisma = charisma;
    }

    /**
     * @return the sense
     */
    public int getSense() {
        return sense;
    }

    /**
     * @param sense the sense to set
     */
    public void setSense(int sense) {
        this.sense = sense;
    }

    /**
     * @return the insight
     */
    public int getInsight() {
        return insight;
    }

    /**
     * @param insight the insight to set
     */
    public void setInsight(int insight) {
        this.insight = insight;
    }

    /**
     * @return the volition
     */
    public int getVolition() {
        return volition;
    }

    /**
     * @param volition the volition to set
     */
    public void setVolition(int volition) {
        this.volition = volition;
    }

    /**
     * @return the hands
     */
    public int getHands() {
        return hands;
    }

    /**
     * @param hands the hands to set
     */
    public void setHands(int hands) {
        this.hands = hands;
    }

    /**
     * @return the charm
     */
    public int getCharm() {
        return charm;
    }

    /**
     * @param charm the charm to set
     */
    public void setCharm(int charm) {
        this.charm = charm;
    }

    /**
     * @return the TDS_charisma
     */
    public int getTDS_charisma() {
        return TDS_charisma;
    }

    /**
     * @param TDS_charisma the TDS_charisma to set
     */
    public void setTDS_charisma(int TDS_charisma) {
        this.TDS_charisma = TDS_charisma;
    }

    /**
     * @return the TDS_sense
     */
    public int getTDS_sense() {
        return TDS_sense;
    }

    /**
     * @param TDS_sense the TDS_sense to set
     */
    public void setTDS_sense(int TDS_sense) {
        this.TDS_sense = TDS_sense;
    }

    /**
     * @return the TDS_insight
     */
    public int getTDS_insight() {
        return TDS_insight;
    }

    /**
     * @param TDS_insight the TDS_insight to set
     */
    public void setTDS_insight(int TDS_insight) {
        this.TDS_insight = TDS_insight;
    }

    /**
     * @return the TDS_volition
     */
    public int getTDS_volition() {
        return TDS_volition;
    }

    /**
     * @param TDS_volition the TDS_volition to set
     */
    public void setTDS_volition(int TDS_volition) {
        this.TDS_volition = TDS_volition;
    }

    /**
     * @return the TDS_hands
     */
    public int getTDS_hands() {
        return TDS_hands;
    }

    /**
     * @param TDS_hands the TDS_hands to set
     */
    public void setTDS_hands(int TDS_hands) {
        this.TDS_hands = TDS_hands;
    }

    /**
     * @return the TDS_charm
     */
    public int getTDS_charm() {
        return TDS_charm;
    }

    /**
     * @param TDS_charm the TDS_charm to set
     */
    public void setTDS_charm(int TDS_charm) {
        this.TDS_charm = TDS_charm;
    }

    /**
     * @return the pkLevel
     */
    public int getPkLevel() {
        return pkLevel;
    }

    /**
     * @param pkLevel the pkLevel to set
     */
    public void setPkLevel(int pkPoint) {
        this.pkLevel = pkPoint;
    }

    /**
     * @return the battlePoint
     */
    public int getBattlePoint() {
        return battlePoint;
    }

    /**
     * @param battlePoint the battlePoint to set
     */
    public void setBattlePoint(int battlePoint) {
        this.battlePoint = battlePoint;
    }

    /**
     * @return the battleExp
     */
    public int getBattleExp() {
        return battleExp;
    }

    /**
     * @param battleExp the battleExp to set
     */
    public void setBattleExp(int battleExp) {
        this.battleExp = battleExp;
    }

    /**
     * @return the isMarred
     */
    public boolean isMarried() {
        return isMarried;
    }

    /**
     * @param isMarred the isMarred to set
     */
    public void setMarried(boolean isMarried) {
        this.isMarried = isMarried;
    }

    /**
     * @return the partnerName
     */
    public String getPartnerName() {
        return partnerName;
    }

    /**
     * @param partnerName the partnerName to set
     */
    public void setPartnerName(String partnerName) {
        this.partnerName = partnerName;
    }

    public MapleSummon getRingsumons() {
        return ringsumons;
    }

    public void setRingsumons(MapleSummon ringsumons) {
        this.ringsumons = ringsumons;
    }

    public MapleSummon getMainsumons() {
        return mainsumons;
    }

    public void setMainsumons(MapleSummon mainsumons) {
        if (mainsumons == null && buffManager.hasBufferStat(MapleBuffStat.射手_精神连接)) {
            cancelBuffStats(MapleBuffStat.射手_精神连接);
        }
        if (mainsumons != null) {
            switch (mainsumons.getSkill()) {
                case 3111005: {// 火凤凰
                    int skilllevel = getSkillLevel(3120006);
                    if (skilllevel > 0) {
                        SkillFactory.getSkill(3120006).getEffect(skilllevel).applyTo(this);// 激活技能 精神连接。
                    }
                    break;
                }
                case 3211005: {// 冰凤凰
                    int skilllevel = getSkillLevel(3220005);
                    // log.debug("释放了冰凤凰。");
                    if (skilllevel > 0) {
                        // log.debug("应用了冰凤凰。");
                        SkillFactory.getSkill(3220005).getEffect(skilllevel).applyTo(this);// 激活技能 精神连接。
                    }
                    break;
                }
            }
        }
        this.mainsumons = mainsumons;
    }

    // 设置4转技能上限
    public void giveMasteryLevel(int jobid) {
        if (this.job.NoBeginner()) {
            for (int i : SkillFactory.getSkills(jobid)) {
                ISkill baseskill = SkillFactory.getSkill(i);
                if (baseskill.hasMastery()) {
                    changeSkillLevel(baseskill, 0, baseskill.getMasterLevel());
                }
            }
        }
        saveToDB(true);
    }

    public int getBeans() {
        return this.beans;
    }

    public void gainBeans(int s) {
        this.beans += s;
    }

    public void setBeans(int s) {
        this.beans = s;
    }

    public void skillAddMaxHp() {
        int[] 提升百分比 = {4100006, 4200006, 4310004, 3120011, 3220009, 5100009,
            11000005, 15100007, 1000006, 31000003, 4210013, 4110008, 61100007, 61110007}; // mhpR
        int[] 提升数值 = {35001002, 35120000, 5221006}; // emhp
        // 33121006, 32111004, 9001008 //buff x
        // 33001001}; //buff z

        double addHpr = 1; // 按百分比增加Hp
        int addHp = 0; // 按数值增加Hp
        int skilllevel = 0;

        /*   if (effects.containsKey(MapleBuffStat.古老意志_体力)
         && effects.get(MapleBuffStat.古老意志_体力).effect.getSourceId() == 23121004) {
         addHp += effects.get(MapleBuffStat.古老意志_体力).effect.getEmhp();
         }*/
        if (buffManager.hasBuff(23121004)) {
            addHp += buffManager.getBuff(23121004).getEffect().getEmhp();
        }
        if (buffManager.hasBufferStat(MapleBuffStat.黑暗变形_HP增加)) {
            addHpr += (getBuffedValue(MapleBuffStat.黑暗变形_HP增加) / 100.0);
        }

        skilllevel = getSkillLevel(5310007);// 生命强化。
        if ((this.job.getId() == 531 || this.job.getId() == 532)
                && skilllevel != 0) {// 炮手。
            addHpr += (skilllevel / 100.0);
        }

        if (buffManager.hasBufferStat(MapleBuffStat.幸运骰子)) {
            addHpr += skillhprrate;
        }

        // mhpR节点处理
        for (int skillid1 : 提升百分比) {
            skilllevel = getSkillLevel(skillid1);
            if (skilllevel > 0) {
                addHpr += SkillFactory.getSkill(skillid1).getEffect(skilllevel).getmHpR();
            }
        }

        if (buffManager.hasBufferStat(MapleBuffStat.射手_精神连接)) {// 计算 射手_精神连接。
            addHpr += buffManager.getBuff(MapleBuffStat.射手_精神连接).getEffect().getmHpR();
        }

        // emhp节点处理
        for (int skillid2 : 提升数值) {
            skilllevel = getSkillLevel(skillid2);
            if (skillid2 == 机械师.终极机甲 && skilllevel > 0
                    || getUsingMount() != null
                    && getUsingMount().getUsingSkillId() == skillid2) {
                addHp += SkillFactory.getSkill(skillid2).getEffect(skilllevel).geteMHp();
            }
        }

        // x z节点处理
        // 美洲豹骑士
        if (getBuffedValue(MapleBuffStat.骑宠1) != null
                && this.getJob().getId() / 100 == 33) {
            addHpr += SkillFactory.getSkill(33001001).getEffect(getSkillLevel(33001001)).getZR();
        }
        Integer value = getBuffedValue(MapleBuffStat.转化);
        if (value != null) {
            addHpr += value.doubleValue() / 100;
        } else {
            value = getBuffedValue(MapleBuffStat.暴走HP);
            if (value != null) {
                addHpr += value.doubleValue() / 100;
            }
        }
        value = getBuffedValue(MapleBuffStat.HYPERBODYHP);
        if (value != null
                && getBuffSource(MapleBuffStat.HYPERBODYHP) != 31121005) {
            addHpr += value.doubleValue() / 100;
        }

        this.localmaxhp *= addHpr;
        this.localmaxhp += addHp;
    }

    public void skillAddMaxMp() {
        int[] 提升百分比 = {2000006, 12000005}; // mmpR
        int[] 提升数值 = {35001002, 35120000, 5221006}; // emhp

        double addMpr = 1; // 按百分比增加Hp
        int addMp = 0; // 按数值增加Hp
        int skilllevel = 0;

        // mhpR节点处理
        for (int skillid1 : 提升百分比) {
            skilllevel = getSkillLevel(skillid1);
            if (skilllevel > 0) {
                addMpr += SkillFactory.getSkill(skillid1).getEffect(skilllevel).getmMpR();
            }
        }

        // emhp节点处理
        for (int skillid2 : 提升数值) {
            skilllevel = getSkillLevel(skillid2);
            if (skillid2 == 机械师.终极机甲 && skilllevel > 0
                    || getUsingMount() != null
                    && getUsingMount().getUsingSkillId() == skillid2) {
                addMp += SkillFactory.getSkill(skillid2).getEffect(skilllevel).geteMMp();
            }
        }
        Integer value = getBuffedValue(MapleBuffStat.HYPERBODYMP);
        if (value != null) {
            addMpr += value.doubleValue() / 100;
        }
        this.localmaxmp *= addMpr;
        this.localmaxmp += addMp;
    }

    public boolean isAddHpMpSkill(int skillid) {
        switch (skillid) {
            case 5100009:
            case 11000005:
            case 15100007:
            case 1000006:// HP增加
            case 4100006:
            case 4200006:
            case 4310004:
            case 3120011:
            case 3220009:
            case 35120000:
            case 2000006:
            case 12000005:// MP增加
                return true;
            default:
                return false;
        }
    }

    public void 黄字公告(String text) {
        client.getSession().write(MaplePacketCreator.serverNotice(text));
    }

    public void 弹窗(String text) {
        client.getSession().write(MaplePacketCreator.serverNotice(1, text));
    }

    public int getcleardamage() {
        return cleardamages;
    }

    public boolean hasBufferStat(MapleBuffStat stat) {
        return getBuffManager().hasBufferStat(stat);
    }

    public int Lianjie() {
        PreparedStatement ps;
        ResultSet re;
        int count = 0;
        try {
            Connection con = DatabaseConnection.getConnection();
            ps = con.prepareStatement("SELECT count(*) as cc FROM accounts WHERE loggedin = 2");
            re = ps.executeQuery();
            while (re.next()) {
                count = re.getInt("cc");
            }
            re.close();
            ps.close();
            con.close();
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
        return count;
    }

    public ScheduledFuture<?> getRingFuture() {
        return ringFuture;
    }

    public void setRingFuture(ScheduledFuture<?> ringFuture) {
        this.ringFuture = ringFuture;
    }

    // 高级任务系统 - 检查基础条件是否符合所有任务前置条件
    public boolean MissionCanMake(final int missionid) {
        boolean ret = true;
        for (int i = 1; i < 5; i++) {
            if (!MissionCanMake(missionid, i)) { // 检查每一个任务条件是否满足
                ret = false;
            }
        }
        return ret;
    }

    // 高级任务系统 - 检查基础条件是否符合指定任务前置条件
    public boolean MissionCanMake(final int missionid, final int checktype) {
        // checktype
        // 1 检查等级范围
        // 2 检查职业
        // 3 检查物品
        // 4 检查前置任务
        boolean ret = false;
        int minlevel = -1, maxlevel = -1; // 默认不限制接任务的等级范围
        String joblist = "all", itemlist = "none", prelist = "none"; // 默认所有职业可以接，默认不需要任何前置物品和任务
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            Connection con = DatabaseConnection.getConnection();
            ps = con.prepareStatement("SELECT minlevel,maxlevel,joblist,itemlist,prelist FROM missionlist WHERE missionid = ?");
            ps.setInt(1, missionid);
            rs = ps.executeQuery();
            if (rs.next()) {
                minlevel = rs.getInt("minlevel");
                maxlevel = rs.getInt("maxlevel");
                joblist = rs.getString("joblist");
                itemlist = rs.getString("itemlist");
                prelist = rs.getString("prelist");
            }
            rs.close();
            ps.close();
            // 判断检查条件是否吻合
            switch (checktype) {
                case 1: // 判断级别是否符合要求
                    if (minlevel > -1 && maxlevel > -1) { // 双范围检查
                    if (this.getLevel() >= minlevel
                            && this.getLevel() <= maxlevel) {
                        ret = true;
                    }
                } else if (minlevel > -1 && maxlevel == -1) { // 只有最小限制
                    if (this.getLevel() >= minlevel) {
                        ret = true;
                    }
                } else if (minlevel == -1 && maxlevel > -1) { // 只有最大限制
                    if (this.getLevel() <= maxlevel) {
                        ret = true;
                    }
                } else if (minlevel == -1 && maxlevel == -1) { // 如果是默认值-1，表示任何等级都可以接
                    ret = true;
                }
                    break;
                case 2: // 检查职业是否符合要求
                    if (joblist.equals("all")) { // 所有职业多可以接
                    ret = true;
                } else {
                    for (int i : StringtoInt(joblist)) {
                        if (this.getJob().getId() == i) { // 只要自己的职业ID在这个清单里，就是符合要求，立即跳出检查
                            ret = true;
                            break;
                        }
                    }
                }
                    break;
                case 3: // 检查前置物品是否有
                    if (itemlist.equals("none")) { // 没有前置物品要求
                    ret = true;
                } else {
                    for (int i : StringtoInt(itemlist)) {
                        if (!this.haveItem(id, 1, false, false)) { // 如果没有清单里要求的物品，立即跳出检查
                            ret = false;
                            break;
                        }
                    }
                }
                    break;
                case 4: // 检查前置任务是否有完成
                    if (prelist.equals("none")) { // 前置任务是否完成
                    ret = true;
                } else {
                    for (int i : StringtoInt(prelist)) {
                        if (!MissionStatus(this.getId(), i, 0, 1)) { // 如果要求的前置任务没完成或从来没接过，立即跳出检查
                            ret = false;
                            break;
                        }
                    }
                }
                    break;
            }
            con.close();
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
        return ret;
    }

    // 高级任务函数 - 得到任务的等级数据
    public int MissionGetIntData(final int missionid, final int checktype) {
        // checktype
        // 1 最小等级
        // 2 最大等级
        int ret = -1;
        int minlevel = -1, maxlevel = -1;
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            Connection con = DatabaseConnection.getConnection();
            ps = con.prepareStatement("SELECT minlevel,maxlevel FROM missionlist WHERE missionid = ?");
            ps.setInt(1, missionid);
            rs = ps.executeQuery();
            if (rs.next()) {
                minlevel = rs.getInt("minlevel");
                maxlevel = rs.getInt("maxlevel");
            }
            rs.close();
            ps.close();
            // 判断检查条件是否吻合
            switch (checktype) {
                case 1:
                    ret = minlevel;
                    break;
                case 2:
                    ret = maxlevel;
                    break;
            }
            con.close();
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
        return ret;
    }

    // 高级任务函数 - 得到任务的的字符串型数据
    public String MissionGetStrData(final int missionid, final int checktype) {
        // checktype
        // 1 任务名称
        // 2 职业列表
        // 3 物品列表
        // 4 前置任务列表
        String ret = "";
        String missionname = "", joblist = "all", itemlist = "none", prelist = "none"; // 默认所有职业可以接，默认不需要任何前置物品和任务
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            Connection con = DatabaseConnection.getConnection();
            ps = con.prepareStatement("SELECT missionname,joblist,itemlist,prelist FROM missionlist WHERE missionid = ?");
            ps.setInt(1, missionid);
            rs = ps.executeQuery();
            if (rs.next()) {
                missionname = rs.getString("missionname");
                joblist = rs.getString("joblist");
                itemlist = rs.getString("itemlist");
                prelist = rs.getString("prelist");
            }
            rs.close();
            ps.close();
            // 判断检查条件是否吻合
            switch (checktype) {
                case 1:
                    ret = missionname;
                    break;
                case 2:
                    ret = joblist;
                    break;
                case 3:
                    ret = itemlist;
                    break;
                case 4:
                    ret = prelist;
                    break;
            }
            con.close();
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
        return ret;
    }

    // 高级任务函数 - 直接输出需要的职业列表串
    public String MissionGetJoblist(final String joblist) {
        String ret = "", jobname = "";
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            Connection con = DatabaseConnection.getConnection();
            for (int i : StringtoInt(joblist)) {
                ps = con.prepareStatement("SELECT * FROM joblist WHERE id = ?");
                ps.setInt(1, i);
                rs = ps.executeQuery();
                if (rs.next()) {
                    jobname = jobname + "," + rs.getString("jobname");
                }
                rs.close();
                ps.close();
            }
            con.close();
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
        return ret;
    }

    // 高级任务系统 - 任务创建
    public void MissionMake(final int charid, final int missionid,
            final int repeat, final int repeattime, final int lockmap,
            final int mobid) {
        PreparedStatement ps = null;
        try {
            Connection con = DatabaseConnection.getConnection();
            ps = con.prepareStatement("INSERT INTO missionstatus VALUES (DEFAULT, ?, ?, ?, ?, ?, 0, DEFAULT, 0, 0, ?, 0, 0)");
            ps.setInt(1, missionid);
            ps.setInt(2, charid);
            ps.setInt(3, repeat);
            ps.setInt(4, repeattime);
            ps.setInt(5, lockmap);
            ps.setInt(6, mobid);
            ps.executeUpdate();
            ps.close();
            con.close();
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
    }

    // 高级任务系统 - 重新做同一个任务
    public void MissionReMake(final int charid, final int missionid,
            final int repeat, final int repeattime, final int lockmap) {
        PreparedStatement ps = null;
        int finish = 0;
        try {
            Connection con = DatabaseConnection.getConnection();
            ps = con.prepareStatement("UPDATE missionstatus SET `repeat` = ?, repeattime = ?, lockmap = ?, finish = ?, minnum = 0 WHERE missionid = ? and charid = ?");
            ps.setInt(1, repeat);
            ps.setInt(2, repeattime);
            ps.setInt(3, lockmap);
            ps.setInt(4, finish);
            ps.setInt(5, missionid);
            ps.setInt(6, charid);
            ps.executeUpdate();
            ps.close();
            con.close();
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
    }

    // 高级任务系统 - 任务完成
    public void MissionFinish(final int charid, final int missionid) {
        PreparedStatement ps = null;
        try {
            Connection con = DatabaseConnection.getConnection();
            ps = con.prepareStatement("UPDATE missionstatus SET finish = 1, lastdate = CURRENT_TIMESTAMP(), times = times+1, lockmap = 0 WHERE missionid = ? and charid = ?");
            ps.setInt(1, missionid);
            ps.setInt(2, charid);
            ps.executeUpdate();
            ps.close();
            con.close();
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
    }

    // 高级任务系统 - 放弃任务
    public void MissionDelete(final int charid, final int missionid) {
        PreparedStatement ps = null;
        try {
            Connection con = DatabaseConnection.getConnection();
            ps = con.prepareStatement("DELETE FROM missionstatus WHERE missionid = ? and charid = ?");
            ps.setInt(1, missionid);
            ps.setInt(2, charid);
            ps.executeUpdate();
            ps.close();
            con.close();
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
    }

    // 高级任务系统 - 增加指定任务的打怪数量
    public void MissionAddMinNum(final int missionid, final int addnum) {
        PreparedStatement ps = null;
        try {
            Connection con = DatabaseConnection.getConnection();
            ps = con.prepareStatement("UPDATE missionstatus SET `minnum` = `minnum` + ? WHERE missionid = ? and charid = ?");
            ps.setInt(1, addnum);
            ps.setInt(2, missionid);
            ps.setInt(3, this.getId());
            ps.executeUpdate();
            ps.close();
            con.close();
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
    }

    // 高级任务系统 - 指定任务的需要最大打怪数量
    public void MissionMaxNum(final int missionid, final int maxnum) {
        PreparedStatement ps = null;
        try {
            Connection con = DatabaseConnection.getConnection();
            ps = con.prepareStatement("UPDATE missionstatus SET `maxnum` = ? WHERE missionid = ? and charid = ?");
            ps.setInt(1, maxnum);
            ps.setInt(2, missionid);
            ps.setInt(3, this.getId());
            ps.executeUpdate();
            ps.close();
            con.close();
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
    }

    public void MissionMob(final int mobid) {
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            Connection con = DatabaseConnection.getConnection();
            ps = con.prepareStatement("SELECT * FROM missionstatus WHERE charid = ? and mobid = ?");
            ps.setInt(1, this.getId());
            ps.setInt(2, mobid);
            rs = ps.executeQuery();
            while (rs.next()) { // 所有属于相同怪的任务，都会自动加
                if (rs.getInt("minnum") < rs.getInt("maxnum")) { // 打怪数小于需要的总数
                    MissionAddMinNum(rs.getInt("missionid"), 1); // 给这个打怪数加1
                    this.dropMessage(
                            5,
                            "高级任务 ["
                            + MissionGetStrData(rs.getInt("missionid"),
                            1) + "-" + rs.getInt("missionid")
                            + "]  完成条件 [" + rs.getInt("minnum") + "/"
                            + rs.getInt("maxnum") + "]");
                } else {
                    MissionFinish(this.getId(), rs.getInt("missionid")); // 自动完成任务
                    this.startMapEffect(
                            "高级任务 ["
                            + MissionGetStrData(rs.getInt("missionid"),
                            1) + "-" + rs.getInt("missionid")
                            + "]  任务完成！", 5120025); // 任务完成有提示
                }

            }
            rs.close();
            ps.close();
            con.close();
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
    }

    // 高级任务系统 - 放弃所有未完成任务
    public void MissionDeleteNotFinish(final int charid) {
        PreparedStatement ps = null;
        try {
            Connection con = DatabaseConnection.getConnection();
            ps = con.prepareStatement("DELETE FROM missionstatus WHERE finish = 0 and charid = ?");
            ps.setInt(1, charid);
            ps.executeUpdate();
            ps.close();
            con.close();
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
    }

    // 高级任务系统 - 获得任务是否可以做
    public boolean MissionStatus(final int charid, final int missionid,
            final int maxtimes, final int checktype) {
        // 0 检查此任务是否被完成了
        // 1 检查此任务是否允许重复做
        // 2 检查此任务重复做的时间间隔是否到
        // 3 检查此任务是否到达最大的任务次数
        // 4 检查是否接过此任务，即是否第一次做这个任务
        // 5 检查是否接了锁地图传送的任务
        boolean ret = false; // 默认是可以做
        int MissionMake = 0; // 默认是没有接过此任务
        long now = 0;
        long t = 0;
        Timestamp lastdate;

        int repeat = 0;
        int repeattime = 0;
        int finish = 0;
        int times = 0;
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            Connection con = DatabaseConnection.getConnection();
            if (checktype == 5) {
                ps = con.prepareStatement("SELECT * FROM missionstatus WHERE lockmap = 1 and charid = ?");
                ps.setInt(1, charid);
            } else {
                ps = con.prepareStatement("SELECT * FROM missionstatus WHERE missionid = ? and charid = ?");
                ps.setInt(1, missionid);
                ps.setInt(2, charid);
            }
            rs = ps.executeQuery();
            if (rs.next()) {
                lastdate = rs.getTimestamp("lastdate");
                repeat = rs.getInt("repeat");
                repeattime = rs.getInt("repeattime");
                finish = rs.getInt("finish");
                times = rs.getInt("times");
                t = lastdate.getTime();
                now = System.currentTimeMillis();
                MissionMake = 1; // 标明这个任务已经接过了
            }
            rs.close();
            ps.close();
            // 判断检查状态类型
            switch (checktype) {
                case 0:
                    if (finish == 1) {
                    ret = true;
                }
                    break;
                case 1:
                    if (repeat == 1) {
                    ret = true;
                }
                    break;
                case 2:
                    if (now - t > repeattime) { // 判断如果有没有到指定的重复做任务间隔时间
                    // 已经到了间隔时间
                    ret = true;
                }
                    break;
                case 3:
                    if (times >= maxtimes) {
                    // 任务到达最大次数
                    ret = true;
                }
                    break;
                case 4:
                    if (MissionMake == 1) {
                    // 此任务已经接过了
                    ret = true;
                }
                    break;
                case 5:
                    if (MissionMake == 1) {
                    // 已经接了锁地图的任务
                    ret = true;
                }
            }
            con.close();
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
        return ret;
    }

    // happypack 闯关任务 - 接任务
    public void TaskMake(final int missionid) {
        PreparedStatement ps = null;
        try {
            Connection con = DatabaseConnection.getConnection();
            ps = con.prepareStatement("INSERT INTO missionstatus VALUES (DEFAULT, ?, ?, 0, 0, 0, 0, DEFAULT, 0, 0, 0, 0, 0)");
            ps.setInt(1, missionid);
            ps.setInt(2, this.getId());
            ps.executeUpdate();
            ps.close();
            con.close();
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
    }

    // happypack 闯关任务 - 检查是否接过任务
    public boolean TaskStatus(final int missionid) {
        boolean ret = false; // 默认是没有接过任务
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            Connection con = DatabaseConnection.getConnection();
            ps = con.prepareStatement("SELECT * FROM missionstatus WHERE missionid = ? and charid = ?");
            ps.setInt(1, missionid);
            ps.setInt(2, this.getId());
            rs = ps.executeQuery();
            if (rs.next()) {
                ret = true; // 标明这个任务已经接过了
            }
            rs.close();
            ps.close();
            con.close();
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
        return ret;
    }

    // happypack 闯关任务 - 得到当前关卡积分
    public int TaskExp(final int missionid) {
        int ret = 0; // 默认是0积分
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            Connection con = DatabaseConnection.getConnection();
            ps = con.prepareStatement("SELECT * FROM missionstatus WHERE missionid = ? and charid = ?");
            ps.setInt(1, missionid);
            ps.setInt(2, getId());
            rs = ps.executeQuery();
            if (rs.next()) {
                ret = rs.getInt("exp"); // 得到积分
            }
            rs.close();
            ps.close();
            con.close();
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
        return ret;
    }

    // happypack 闯关任务 - 得到闯关积分
    public void TaskAddExp(int missionid, int addexp) {
        PreparedStatement ps = null;
        try {
            Connection con = DatabaseConnection.getConnection();
            ps = con.prepareStatement("UPDATE missionstatus SET `exp` = `exp` + ? WHERE missionid = ? and charid = ?");
            ps.setInt(1, addexp);
            ps.setInt(2, missionid);
            ps.setInt(3, this.getId());
            ps.executeUpdate();
            ps.close();
            con.close();
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
    }

    public Integer[] StringtoInt(final String str) {
        ArrayList<Integer> list = new ArrayList<Integer>();
        StringTokenizer toKenizer = new StringTokenizer(str, ",");
        while (toKenizer.hasMoreTokens()) {
            list.add(Integer.parseInt(toKenizer.nextToken()));
        }
        return (Integer[]) list.toArray();
    }

    public double getSkillhprrate() {
        return skillhprrate;
    }

    public void setSkillhprrate(double skillhprrate) {
        this.skillhprrate = skillhprrate;
    }

    public HiredMerchantInventory getHiredmerchantinventory() {
        return hiredmerchantinventory;
    }

    private long getMerchantmesos() {
        return hiredmerchantinventory.meso;
    }

    private void setMerchantmesos(long value) {
        this.hiredmerchantinventory.meso = value;
    }

    public int getPresent() {
        return Present;
    }

    public void setPresent(int Present) {
        this.Present = Present;
    }

    public int getFace_Adorn() {
        return face_adorn;
    }

    public void setFace_Adorn(int face_adorn) {
        this.face_adorn = face_adorn;
    }

    public Timestamp getNecklace_Expansion() {
        return necklace_expansion;
    }

    public void setNecklace_Expansion(Timestamp necklace_expansion) {
        this.necklace_expansion = necklace_expansion;
    }

    public int getClfb() {
        return clfb;
    }

    public void setClfb(int clfb) {
        this.clfb = clfb;
    }

    public int getPvpkills() {
        return pvpkills;
    }

    public void setPvpkills(int pvpkills) {
        this.pvpkills = pvpkills;
    }

    public int getPvpdeaths() {
        return pvpdeaths;
    }

    public void setPvpdeaths(int pvpdeaths) {
        this.pvpdeaths = pvpdeaths;
    }

    public void gainPvpKill() {
        pvpkills++;
    }

    public void gainPvpDeath() {
        pvpdeaths++;
    }

    public MapleAndroid getAndroid() {
        return android;
    }

    public boolean checkHearts() {
        if (getInventory(MapleInventoryType.EQUIPPED).getItem((byte) -35) != null) {
            return true;
        }
        return false;
    }

    public void setAndroid(MapleAndroid a) {
        if (checkHearts()) {
            this.android = a;
            if (map != null && a != null) {
                map.broadcastMessage(MaplePacketCreator.spawnAndroid(this, a));
                map.broadcastMessage(MaplePacketCreator.showAndroidEmotion(this.getId(), Randomizer.getInstance().nextInt(17) + 1));
            }
        }
    }

    public void removeAndroid() {
        if (map != null) {
            map.broadcastMessage(MaplePacketCreator.deactivateAndroid(this.id));
        }
        android = null;
    }

    public void updateAndroid(int size, int itemId) {
        if (map != null) {
            map.broadcastMessage(MaplePacketCreator.updateAndroidLook(this.getId(), size, itemId));
        }
    }

    public MapleCharAttribute getAttribute() {
        return attribute;
    }

    public boolean equalsJob(MapleJob job) {
        return getJob().equals(job);
    }

    public int getEquipPed(int slot) {
        MapleInventory equip = getInventory(MapleInventoryType.EQUIPPED);//工厂模式写的
        IItem cWeapon = equip.getItem((byte) slot);
        return cWeapon != null ? cWeapon.getItemId() : 0;
    }

    public MapleRPSGame getRPSGame() {
        return RPSGame;
    }

    public void setRPSGame(MapleRPSGame RPSGame) {
        this.RPSGame = RPSGame;
    }

    public void sendAddAttackLimit() {
        if (client.getChannelServer().isUseAddMaxAttack()) {
            SendPacket(MaplePacketCreator.giveBuff(-2022118, -1498229862, Collections.singletonList(Pair.Create(MapleBuffStat.攻击上限, 1000000000)), this));
        }
    }

    public void sendDemonAvengerPacket() {
        if (job.IsDemonAvenger()) {
            SendPacket(MaplePacketCreator.giveBuff(0, 1643685997, MapleStatEffect.恶魔复仇者, this));
        }
    }

    public ScriptManager getScript() {
        return script;
    }

    public synchronized void reloadOnlieTime() {
        try {
            String _str = getAttribute().getAttribute().get(ConstantTable.getCurrentDay() + ConstantTable._S_ONLINE_MINUTE);
            if (_str == null) {
                _str = "0";
            }
            Long logintime = getAttribute().getDataValue(ConstantTable._PLAYER_DATA_LOGINTIME);
            if (logintime == null) {
                logintime = System.currentTimeMillis();
            }
            long time = System.currentTimeMillis() - logintime;
            time = time / 1000 / 60;
            _str = new Integer((Integer.parseInt(_str) + (int) time)).toString();
            getAttribute().getAttribute().put(ConstantTable.getCurrentDay() + ConstantTable._S_ONLINE_MINUTE, _str);
            getAttribute().setDataValue(ConstantTable._PLAYER_DATA_LOGINTIME, System.currentTimeMillis());
        } catch (Exception e) {
            log.error("建立断开事件错误：", e);
        }
    }

    public int getPower() {
        return power;
    }

    public void setPower(int power) {
        this.power = Math.min(power, 20);
        SendPacket(MaplePacketCreator.giveBuff(30020232, -436000866, Arrays.asList(Pair.Create(MapleBuffStat.尖兵电力, power)), this));
    }

    public void reducePower(int power) {
        setPower(this.power - power);
    }

    public boolean isPowerOpen() {
        return powerOpen;
    }

    public void setPowerOpen() {
        this.powerOpen = !powerOpen;
        SendPacket(MaplePacketCreator.giveBuff(0, 36000, Arrays.asList(Pair.Create(MapleBuffStat.尖兵电池时间, 0)), this));
        SendPacket(MaplePacketCreator.getShowSkillEffectCode(powerOpen ? 0x36 : 0x37));
    }

    public long getPowerTime() {
        return powerTime;
    }

    public void setPowerTime(long powerTime) {
        this.powerTime = powerTime;
    }

    public void checkPower() {
        //如果你是尖兵。这里会为你家电
        if (getJob().IsTrailblazer()) {
            if ((System.currentTimeMillis() - powerTime) >= 4000) {
                powerTime = System.currentTimeMillis();
                int _powerI = getPower();
                if (_powerI < 20) {
                    setPower(_powerI + 2);//一次给你加2个吧
                }
            }
        }
    }

    public void initPower() {
        if (getJob().IsTrailblazer()) {
            setPower(0);
            SendPacket(MaplePacketCreator.giveBuff(0, 36000, Arrays.asList(Pair.Create(MapleBuffStat.尖兵电池时间, 0)), this));//给你10个小时电池够不够用
        }
    }

    public void setAttribute(MapleCharAttribute attribute) {
        this.attribute = attribute;
    }
}
