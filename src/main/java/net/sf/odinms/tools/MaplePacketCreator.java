package net.sf.odinms.tools;

import java.awt.Point;
import java.awt.Rectangle;
import java.net.InetAddress;
import java.rmi.RemoteException;
import java.sql.*;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.Map.Entry;

import net.sf.odinms.client.*;
import net.sf.odinms.client.IEquip.ScrollResult;
import net.sf.odinms.client.skills.*;
import net.sf.odinms.client.status.MapleMonsterStat;
import net.sf.odinms.database.DatabaseConnection;
import net.sf.odinms.net.MaplePacket;
import net.sf.odinms.net.SendPacketOpcode;
import net.sf.odinms.net.channel.Setting;
import net.sf.odinms.net.channel.handler.BeansGame1Handler.Beans;
import net.sf.odinms.net.channel.handler.DamageParseHandler;
import net.sf.odinms.net.channel.handler.PlayerInteractionHandler;
import net.sf.odinms.net.channel.handler.SummonDamageHandler.SummonAttackEntry;
import net.sf.odinms.net.login.LoginServer;
import net.sf.odinms.net.login.remote.ChannelLoadInfo;
import net.sf.odinms.net.world.MapleParty;
import net.sf.odinms.net.world.MaplePartyCharacter;
import net.sf.odinms.net.world.PartyOperation;
import net.sf.odinms.net.world.PlayerCoolDownValueHolder;
import net.sf.odinms.net.world.guild.MapleAlliance;
import net.sf.odinms.net.world.guild.MapleGuild;
import net.sf.odinms.net.world.guild.MapleGuildCharacter;
import net.sf.odinms.net.world.guild.MapleGuildSummary;
import net.sf.odinms.server.*;
import net.sf.odinms.server.life.MapleAndroid;
import net.sf.odinms.server.life.MapleMonster;
import net.sf.odinms.server.life.MapleNPC;
import net.sf.odinms.server.life.MobSkill;
import net.sf.odinms.server.maps.MapleMist;
import net.sf.odinms.server.maps.MapleReactor;
import net.sf.odinms.server.maps.MapleSummon;
import net.sf.odinms.server.miniGame.MapleRPSGame;
import net.sf.odinms.server.movement.LifeMovementFragment;
import net.sf.odinms.server.playerinteractions.*;
import net.sf.odinms.server.skill.MapleForeignBuffSkill;
import net.sf.odinms.tools.data.output.LittleEndianWriter;
import net.sf.odinms.tools.data.output.MaplePacketLittleEndianWriter;

public class MaplePacketCreator {

    private static org.apache.log4j.Logger log = org.apache.log4j.Logger.getLogger(MaplePacketCreator.class);
    private final static byte[] CHAR_INFO_MAGIC = new byte[]{(byte) 0xff, (byte) 0xc9, (byte) 0x9a, 0x3b};
    public static final List<Pair<MapleStat, Number>> EMPTY_STATUPDATE = Collections.emptyList();
    /**
     * 永久时间：物品信息的过期时间。【00 40 E0 FD 3B 37 4F 01】【1900/1/1 0:00:00】
     */
    public final static long FINAL_TIME = -2209017600000L;//3439785600000 、、
    /**
     * 永久时间：物品信息的解锁时间。【00 80 05 BB 46 E6 17 02】【2079/1/1 0:00:00】
     */
    public final static long FINAL_LOCKITEMTIME = 3439728000000L;
    private static boolean show = false;
    private static int sysTime = 获取当前时间();
    private static final MapleItemInformationProvider ii = MapleItemInformationProvider.getInstance();
    private static final String[] HELLO_STRINGS = {"MapLogin", "MapLogin1", "MapLogin2", "MapLogin3"};
    //登录界面

    public static MaplePacket getHello(short mapleVersion, String secondaryVersion, byte[] sendIv, byte[] recvIv, boolean testServer) {

        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter(16);
        //0D 00 5D 00 00 00 6E 4C F3 0D 09 2D 55 7E 04
        mplew.writeShort(0x0E);  //0E是代表盛大版本的标志,其他服不一定是0D
        mplew.writeShort(mapleVersion);
        mplew.writeMapleAsciiString(secondaryVersion);
        mplew.write(recvIv);
        mplew.write(sendIv);
        mplew.write(testServer ? 5 : 4);
        return mplew.getPacket();
    }

    public static MaplePacket getHelloto() {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.SEND_LINK.getValue());//093修改
        mplew.writeMapleAsciiString(HELLO_STRINGS[(int) (Math.random() * HELLO_STRINGS.length)]);
        mplew.writeInt(sysTime); //当前系统时间的年月日时
        mplew.write(1);
        return mplew.getPacket();
    }

    public static int 获取当前时间() {
        String 当前时间 = new SimpleDateFormat("yyyyMMddHH").format(new java.util.Date());
        return Integer.valueOf(当前时间);
    }

    /**
     * 发送一个ping包
     *
     * @return The packet.
     */
    public static MaplePacket getPing() {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter(16);
        mplew.writeShort(SendPacketOpcode.PING.getValue());
        return mplew.getPacket();
    }

    /**
     * **
     * 获得一个登陆失败的包.
     *
     * @param 原因
     * @return
     */
    public static MaplePacket getLoginFailed(int reason) {

        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter(16);
        mplew.writeShort(SendPacketOpcode.LOGIN_STATUS.getValue());
        mplew.writeInt(reason);
        mplew.writeShort(0);
        return mplew.getPacket();
    }

    /**
     * **
     * 封某个帐号的包
     *
     * @param 类型
     * @return
     */
    public static MaplePacket getPermBan(byte reason) {

        // 00 00 02 00 01 01 01 01 01 00
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter(16);

        mplew.writeShort(SendPacketOpcode.LOGIN_STATUS.getValue());
        mplew.writeShort(0x02); // 账号已经被禁止 is banned
        mplew.write(0x0);
        mplew.write(reason);
        mplew.write(HexTool.getByteArrayFromHexString("01 01 01 01 00"));

        return mplew.getPacket();
    }

    //临时封号的包
    /**
     * *
     * 临时封号的包
     *
     * @param 时间
     * @param 类型
     * @return
     */
    public static MaplePacket getTempBan(long timestampTill, byte reason) {

        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter(17);

        mplew.writeShort(SendPacketOpcode.LOGIN_STATUS.getValue());
        mplew.write(0x02);
        mplew.write(HexTool.getByteArrayFromHexString("00 00 00 00 00")); // Account is banned
        mplew.write(reason);
        mplew.writeLong(timestampTill); // Tempban date is handled as a 64-bit long, number of 100NS intervals since 1/1/1601. Lulz.

        return mplew.getPacket();
    }

    //得到了成功的身份证认证,并将请求包。 （这个是登陆的很重要啊!!!）
    public static MaplePacket getAuthSuccess(MapleClient c) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.LOGIN_STATUS.getValue());
        mplew.write(0);
        mplew.writeZero(4);//未知ID
        mplew.write((byte) c.getGender());
        mplew.writeShort(0);
        mplew.writeInt(0);
        mplew.write(1);
        mplew.writeShort(0);
        mplew.writeZero(6);
        mplew.write(1);
        mplew.writeZero(11);
        mplew.write(1);
        mplew.writeSome(0x6, 0x1, 0x1, 0x0, 0x1, 0x1, 0x1, 0x1);
        mplew.writeZero(6);
        mplew.writeSome(0x1, 0x1, 0x1, 0);
        mplew.writeMapleAsciiString(String.valueOf(c.getAccID()));
        mplew.writeMapleAsciiString(c.getAccountName());//账号
        mplew.write(1); //0提示没填身份证
        mplew.write(1); //098
        mplew.write(0); //098

        return mplew.getPacket();
    }

    public static MaplePacket licenseRequest() {

        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.LOGIN_STATUS.getValue());
        mplew.write((byte) 0x16);
        return mplew.getPacket();
    }

    public static MaplePacket licenseResult() {

        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.LICENSE_RESULT.getValue());
        mplew.write(1);
        return mplew.getPacket();
    }

    //性别更换
    public static MaplePacket genderChanged(MapleClient c) {

        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter(3);

        mplew.writeShort(SendPacketOpcode.GENDER_SET.getValue());
        mplew.write(0);
        mplew.writeMapleAsciiString(c.getAccountName());
        mplew.writeMapleAsciiString(String.valueOf(c.getAccID()));

        return mplew.getPacket();
    }

    //性别选择
    public static MaplePacket genderNeeded(MapleClient c) {

        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter(3);

        mplew.writeShort(SendPacketOpcode.CHOOSE_GENDER.getValue());
        mplew.writeMapleAsciiString(c.getAccountName());

        return mplew.getPacket();
    }

    public static MaplePacket pinOperation(byte mode) {

        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter(3);

        mplew.writeShort(SendPacketOpcode.PIN_OPERATION.getValue());
        mplew.write(mode);

        return mplew.getPacket();
    }

    public static MaplePacket pinAssigned() {

        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter(3);

        mplew.writeShort(SendPacketOpcode.PIN_ASSIGNED.getValue());
        mplew.write(0);

        return mplew.getPacket();
    }

    //服务器列表
    public static MaplePacket getServerList(int serverId, String serverName, ChannelLoadInfo channelLoad) {


        int lastChannel = Math.max(channelLoad.getChannelCount(serverId), 1);

        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.SERVERLIST.getValue());
        mplew.writeShort(serverId);
        mplew.writeMapleAsciiString(serverName);
        mplew.write(LoginServer.getInstance().getFlag());
        mplew.writeMapleAsciiString(LoginServer.getInstance().getEventMessage());
        mplew.writeShort(0x64); // 093修改
        mplew.writeShort(0x64); // 093修改
        mplew.write(lastChannel);
        mplew.writeInt(400);
        int load;
        for (int i = 1; i <= lastChannel; i++) {
            load = channelLoad.getChannelValue(serverId, i);
            mplew.writeMapleAsciiString(serverName + "-" + i);
            mplew.writeInt(load);
            mplew.write(serverId);
            mplew.writeShort(i - 1);
        }
        mplew.writeZero(6);
        return mplew.getPacket();
    }

    /**
     * 得到一个包说服务器列表就结束了。
     *
     * @return The end of server list packet.
     */
    public static MaplePacket getEndOfServerList() {

        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.SERVERLIST.getValue());
        mplew.writeShort(-1);
        mplew.writeLong(0);
        mplew.write();

        return mplew.getPacket();
    }

    /**
     * 得到了包服务器状态信息。
     *
     * Possible values for
     * <code>status</code>:<br> 0 - Normal<br> 1 - Highly populated<br> 2 - Full
     *
     * @param status The server status.
     * @return The server status packet.
     */
    public static MaplePacket getServerStatus(int status) {

        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.SERVERSTATUS.getValue());
        mplew.write(status);

        return mplew.getPacket();
    }

    //得到一个包告诉客户通道服务器的IP位址。
    public static MaplePacket getServerIP(InetAddress inetAddr, int port, int clientId) {

        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.SERVER_IP.getValue());
        mplew.writeShort(0);
        mplew.write(inetAddr.getAddress());
        mplew.writeShort(port);
        // 0x13 = numchannels?
        mplew.writeInt(clientId); // this gets repeated to the channel server
        mplew.write(1);
        mplew.writeZero(4);
        return mplew.getPacket();
    }

    //更换频道
    //Received CHANGE_CHANNEL [0013] (9)
    //[13 00] [01] [DD] [E7 82] [6D] [89 21]
    //[13 00] [01] [CA] [66 36] [7D] [8A 21]
    //[13 00] [01] [CA] [66 36] [7E] [89 21]
    public static MaplePacket getChannelChange(InetAddress inetAddr, int port) {

        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.CHANGE_CHANNEL.getValue());
        mplew.write(1);
        byte[] addr = inetAddr.getAddress();
        mplew.write(addr);
        mplew.writeShort(port);
        mplew.writeZero(2);
        return mplew.getPacket();
    }

    //获取角色列表 
    public static MaplePacket getCharList(MapleClient c, int serverId) {
        List<MapleCharacter> chars = c.loadCharacters(serverId);

        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.CHARLIST.getValue());
        mplew.writeZero(1);
        mplew.write((byte) chars.size()); //目前角色个数
        for (MapleCharacter chr : chars) {
            addCharEntry(mplew, chr);//角色进入
        }
        mplew.writeShort(0);
        mplew.writeInt(c.getMaxCharSlot());//最大角色数
        mplew.writeZero(4);
        mplew.writeInt(-1);
        mplew.writeZero(8 * 19 + 3);

        //      mplew.writeHex("F6 37 CE 01 10 D1 43 00 00 86 5C EE 5F 0E 00 00 00 25 EB 21 01 55 3B 3D 01 01 00 25 EB 21 01 55 3B 3D 01 02 00 25 EB 21 01 55 3B 3D 01 03 00 25 EB 21 01 55 3B 3D 01 04 00 25 EB 21 01 55 3B 3D 01 05 00 25 EB 21 01 55 3B 3D 01 06 00 25 EB 21 01 55 3B 3D 01 07 00 25 EB 21 01 55 3B 3D 01 08 00 25 EB 21 01 55 3B 3D 01 09 00 25 EB 21 01 55 3B 3D 01 0A 00 25 EB 21 01 55 3B 3D 01 0B 00 25 EB 21 01 55 3B 3D 01 0C 00 25 EB 21 01 55 3B 3D 01 0D 00 25 EB 21 01 55 3B 3D 01");
        return mplew.getPacket();
    }

    //分配新职业的SP技能点1
    public static void addRemainingSp(MaplePacketLittleEndianWriter mplew, MapleCharacter chr) {
        if (chr.getJob().IsExtendSPJob()) {
            List<Pair<Integer, Integer>> list = new ArrayList<Pair<Integer, Integer>>();
            for (int i = 0; i < chr.getJob().GetMaxSpSlots(); i++) {
                int sp = chr.getRemainingSp(i + 1);
                list.add(new Pair<Integer, Integer>(i + 1, sp));
            }
            mplew.write(list.size());
            for (Pair<Integer, Integer> pair : list) {
                mplew.write(pair.getLeft());
                mplew.writeInt(pair.getRight());
            }
        } else {
            mplew.writeShort(chr.getRemainingSp());
        }

        /*
         * if (chr.isEvan()) { mplew.write(chr.getdragonz());//取得龙神几转 for (int i
         * = 0; i < chr.getdragonz(); i++) { int sp = chr.getdragonSP(i + 1); if
         * (sp != 0) { mplew.write(i + 1); mplew.write(sp); } } } else if
         * (chr.isResistance()) { int BB = chr.getBigBangJob(); mplew.write(BB);
         * for (int i = 0; i < BB; i++) { int sp = chr.getBigBangSP(i + 1); if
         * (sp != 0) { mplew.write(i + 1); mplew.write(sp); } } } else {
         * //普通职业技能点 mplew.writeShort(chr.getRemainingSp()); //剩余SP }
         */
    }

    //建立角色---
    private static void addCharStats(MaplePacketLittleEndianWriter mplew, MapleCharacter chr) {
        mplew.writeInt(chr.getId()); // 角色ID
        mplew.writeMapleNameString(chr.getName()); //自动填充到13位
        mplew.write(chr.getGender()); // 性别 (0 = 男, 1 = 女)
        mplew.write(2);//107?
        mplew.write(chr.getSkinColor().getId()); // 皮肤
        mplew.writeInt(chr.getFace()); // 脸型
        mplew.writeInt(chr.getHair()); // 发型
        mplew.write(chr.getLevel()); // 等级
        mplew.writeShort(chr.getJob().getId()); // job
        mplew.writeShort(chr.getStr()); // str
        mplew.writeShort(chr.getDex()); // dex
        mplew.writeShort(chr.getInt()); // int
        mplew.writeShort(chr.getLuk()); // 运气
        mplew.writeInt(chr.getHp()); // 当前HP
        mplew.writeInt(chr.getMaxhp()); // 最大HP
        mplew.writeInt(chr.getMp()); // 当前MP
        mplew.writeInt(chr.getMaxmp()); //最大MP
        mplew.writeShort(chr.getRemainingAp()); // 剩余AP
        addRemainingSp(mplew, chr);
        mplew.writeLong(chr.getExp()); //当前经验     
        mplew.writeInt(chr.getFame()); //人气 093修改 short -> int
        mplew.writeInt(0);//110新 
        mplew.writeLong(DateUtil.getFileTimestamp(System.currentTimeMillis()));//不知道什么时间.上次登录时间？
        mplew.writeInt(chr.getMapId()); // 当前地图ID
        mplew.write(chr.getInitialSpawnPoint()); // spawnpoint
        mplew.writeShort(0);
        if (chr.getJob().IsDemonHunter() || chr.getJob().IsTrailblazer()) {
            mplew.writeInt(chr.getFace_Adorn());
        }
        mplew.write(0);
        mplew.writeInt(sysTime); //年月日时
        //倾向系统 领袖气质 感性 洞察力 意志 手技 魅力
        //charisma, sense, insight, volition, hands, charm;
        mplew.writeInt(0); //领袖气质
        mplew.writeInt(0); //洞察力
        mplew.writeInt(0); //意志
        mplew.writeInt(0); //手技
        mplew.writeInt(0); //感性
        mplew.writeInt(0); //魅力
        mplew.writeShort(0); //今天的领袖气质
        mplew.writeShort(0); //今天的洞察力
        mplew.writeShort(0); //今天的意志
        mplew.writeShort(0); //今天的手技
        mplew.writeShort(0); //今天的感性
        mplew.writeShort(0); //今天的魅力

        mplew.write(0);
        mplew.writeHex("00 40 E0 FD 3B 37 4F 01"); //不知道。
        mplew.writeInt(0);
        mplew.writeInt(10);
        mplew.write(0);
        mplew.write(6); //原来是5
        mplew.writeInt(7);
        mplew.write(0);
        mplew.writeHex("00 3B 37 4F 01 00 40 E0"); //不知道。
        mplew.write(0xFD);
        mplew.writeZero(8 * 10 + 6 + 8 + 4);
    }

    private static void addCharLook(MaplePacketLittleEndianWriter mplew, MapleCharacter chr, boolean mega) {
        mplew.write(chr.getGender());//性别
        mplew.write(chr.getSkinColor().getId()); //肤色
        mplew.writeInt(chr.getFace()); // 脸型
        mplew.writeShort(chr.getJob().getId());//093新增 jobid
        mplew.writeShort(0);
        mplew.write(mega ? 0 : 1);
        mplew.writeInt(chr.getHair()); //发型
        MapleInventory equip = chr.getInventory(MapleInventoryType.EQUIPPED);//工厂模式写的
        Map<Byte, Integer> myEquip = new LinkedHashMap<Byte, Integer>();
        Map<Byte, Integer> maskedEquip = new LinkedHashMap<Byte, Integer>();
        synchronized (equip) {
            for (IItem item : equip.list()) { //遍历装备列表
                byte pos = (byte) (item.getPosition() * -1);//定义装备的位置pos
                if (pos < 100 && myEquip.get(pos) == null) {
                    myEquip.put(pos, item.getItemId());
                } else if ((pos > 100/*
                         * || pos == -128
                         */) && pos != 111) {
                    //} else if ((pos > 100 || pos == -128) && pos != 111) { //原:pos > 100 && pos != 111
                    pos -= 100;
                    if (myEquip.get(pos) != null) {
                        maskedEquip.put(pos, myEquip.get(pos));
                    }
                    myEquip.put(pos, item.getItemId());
                } else if (myEquip.get(pos) != null) {
                    maskedEquip.put(pos, item.getItemId());
                }
            }
            //非点装
            for (Entry<Byte, Integer> entry : myEquip.entrySet()) {
                mplew.write(entry.getKey());//装备栏的位置
                mplew.writeInt(entry.getValue());//装备ID
            }
            mplew.write(-1);
            //点装
            for (Entry<Byte, Integer> entry : maskedEquip.entrySet()) {
                mplew.write(entry.getKey());//pos
                mplew.writeInt(entry.getValue());//itemid
            }
            mplew.write(-1);
            //加载图腾
            for (int i = 0; i < 3; i++) {
                IItem tt = equip.getItem((short) (-5000 + i * -1));
                if (tt != null) {
                    mplew.write(i);
                    mplew.writeInt(tt.getItemId());
                }
            }
            mplew.write(-1);
            IItem cWeapon = equip.getItem((byte) -111);

            //点装武器
            mplew.writeInt(cWeapon != null ? cWeapon.getItemId() : 0);

            cWeapon = equip.getItem((byte) -11);

            //武器
            mplew.writeInt(cWeapon != null ? cWeapon.getItemId() : 0);

            cWeapon = equip.getItem((byte) -10);

            //附武器
            mplew.writeInt(cWeapon != null ? cWeapon.getItemId() : 0);

            mplew.write(0);//谁告诉我这个是什么意思。

            //检测是否有宠物
            for (int i = 0; i < 3; i++) {
                if (chr.getPet(i) != null) {
                    mplew.writeInt(chr.getPet(i).getItemId());//宠物ID
                } else {
                    mplew.writeInt(0);
                }
            }

            if (chr.getJob().IsDemonHunter() || chr.getJob().IsTrailblazer()) {
                mplew.writeInt(chr.getFace_Adorn());
            }

        }
    }

    //背包的方法
    private static void addInventoryInfo(MaplePacketLittleEndianWriter mplew, MapleCharacter chr) {
        mplew.write(chr.getLinkedName() != null ? 1 : 0);//精灵的祝福
        if (chr.getLinkedName() != null) {
            mplew.writeMapleAsciiString(chr.getLinkedName());
        }
        mplew.write(chr.getLinkedName() != null ? 1 : 0);//女皇的祝福
        if (chr.getLinkedName() != null) {
            mplew.writeMapleAsciiString(chr.getLinkedName());
        }
        mplew.write(0);//新增
        mplew.writeLong(chr.getMeso()); // 金币已经变成8位啦。
        mplew.writeInt(chr.getId());
        mplew.writeInt(chr.getBeans()); //豆豆
        mplew.writeInt(chr.getCSPoints(1));//抵用卷
        mplew.writeInt(0);//??98
        mplew.write(chr.getInventory(MapleInventoryType.EQUIP).getSlots()); // 装备栏容量 游戏内为18个则封包数值为18
        mplew.write(chr.getInventory(MapleInventoryType.USE).getSlots()); // 消耗栏容量
        mplew.write(chr.getInventory(MapleInventoryType.SETUP).getSlots()); // 设置栏容量
        mplew.write(chr.getInventory(MapleInventoryType.ETC).getSlots()); // 其他栏容量
        mplew.write(chr.getInventory(MapleInventoryType.CASH).getSlots()); // 特殊栏容量 游戏内为18个则封包数值为30 
        mplew.writeLong(DateUtil.getFileTimestamp(chr.getNecklace_Expansion() == null ? FINAL_TIME : chr.getNecklace_Expansion().getTime())); //扩充时间
        //mplew.writeHex("00 40 E0 FD 3B 37 4F 01");
        //下面是关于装备的
        MapleInventory iv = chr.getInventory(MapleInventoryType.EQUIPPED);
        Collection<IItem> equippedC = iv.list();
        List<Item> equipped = new ArrayList<Item>(equippedC.size());
        synchronized (iv) {
            for (IItem item : equippedC) {
                if (((Item) item).getPosition() > -100) { //非点装
                    equipped.add((Item) item);
                }
            }
        }
        Collections.sort(equipped);
        for (Item item : equipped) {
            addItemInfo(mplew, item);
        }

        mplew.writeShort(0); //身上的装备加载结束

        equipped.clear();
        synchronized (iv) {
            for (IItem item : equippedC) {
                if (((Item) item).getPosition() < -100 && ((Item) item).getPosition() > -1000) { //点装
                    equipped.add((Item) item);
                }
            }
        }
        Collections.sort(equipped);
        for (Item item : equipped) {
            addItemInfo(mplew, item);
        }

        mplew.writeShort(0); //身上的点装加载结束

        iv = chr.getInventory(MapleInventoryType.EQUIP);
        synchronized (iv) {
            for (IItem item : iv.list()) {
                addItemInfo(mplew, item);
            }
        }

        mplew.writeShort(0); //装备栏加载结束

        //---------------------------------
        //pos(-1100, -1000] 龙神装备范围
        //---------------------------------
        if (chr.isEvan()) { //是龙神才加载
            iv = chr.getInventory(MapleInventoryType.EQUIPPED);
            synchronized (iv) {
                for (IItem item : iv.list()) {
                    if (((Item) item).getPosition() > -1100 && ((Item) item).getPosition() <= -1000) {
                        addItemInfo(mplew, item);
                        ////log.debug("加载龙神装备"+item.getItemId());
                    }
                }
            }
        }

        mplew.writeShort(0); //龙神装备加载结束
        //---------------------------------
        //pos(-1200, -1100] 机械师装备范围
        //---------------------------------
        if (chr.isMechinic()) { //是机械师才加载
            iv = chr.getInventory(MapleInventoryType.EQUIPPED);
            synchronized (iv) {
                for (IItem item : iv.list()) {
                    if (((Item) item).getPosition() >= -1200 && ((Item) item).getPosition() <= -1100) {
                        addItemInfo(mplew, item);
                        ////log.debug("加载机械师装备"+item.getItemId());
                    }
                }
            }
        }

        mplew.writeShort(0); //机械师装备加载结束
        //---------------------------------
        //pos(-1300, -1200] 安卓装备范围
        //---------------------------------
        iv = chr.getInventory(MapleInventoryType.EQUIPPED);
        synchronized (iv) {
            for (IItem item : iv.list()) {

                if (((Item) item).getPosition() > -1300 && ((Item) item).getPosition() <= -1200) {
                    addItemInfo(mplew, item);
                    ////log.debug("加载安卓cash装备"+item.getItemId());
                }
            }
        }

        mplew.writeShort(0); //智能机器人装备加载结束
        //---------------------------------
        //---------------------------------

        iv = chr.getInventory(MapleInventoryType.EQUIPPED);
        synchronized (iv) {
            for (IItem item : iv.list()) {
                if (((Item) item).getPosition() <= -5000
                        && ((Item) item).getPosition() >= -5003) {
                    addItemInfo(mplew, item);
                }
            }
        }
        mplew.writeShort(0);//图腾
        //---------------------------------
        //---------------------------------
        mplew.writeShort(0);//不知道？
        //---------------------------------
        mplew.writeInt(0);//109新增 4 字节
        //---------------------------------
        //---------------------------------


        iv = chr.getInventory(MapleInventoryType.USE);
        synchronized (iv) {
            for (IItem item : iv.list()) {
                addItemInfo(mplew, item);
            }
        }
        mplew.write(0); //消耗栏加载结束
        //---------------------------------
        //---------------------------------
        iv = chr.getInventory(MapleInventoryType.SETUP);
        synchronized (iv) {
            for (IItem item : iv.list()) {
                addItemInfo(mplew, item);
            }
        }
        mplew.write(0); //设置栏加载结束
        //---------------------------------
        //---------------------------------
        iv = chr.getInventory(MapleInventoryType.ETC);
        synchronized (iv) {
            for (IItem item : iv.list()) {
                addItemInfo(mplew, item);
            }
        }
        mplew.write(0); //其他栏加载结束
        //---------------------------------
        //---------------------------------
        iv = chr.getInventory(MapleInventoryType.CASH);
        synchronized (iv) {
            for (IItem item : iv.list()) {
                addItemInfo(mplew, item);
            }
        }
        mplew.write(0); //现金栏加载结束
        mplew.writeLong(0);//矿产包
    }

    //技能生成
    private static void addSkillRecord(MaplePacketLittleEndianWriter mplew, MapleCharacter chr) {
        Map<ISkill, MapleCharacter.SkillEntry> skills = chr.getSkills();
        int x = 0;
        for (Entry<ISkill, MapleCharacter.SkillEntry> skill : skills.entrySet()) { //遍历输出技能
            int id = skill.getKey().getId();
            if (id == 32001008 || id == 32001009 || id == 32001010 || id == 32001011) {
                x += 1;
            }
        }
        x = skills.size() - x;
        mplew.writeShort(x);//角色技能个数
        for (Entry<ISkill, MapleCharacter.SkillEntry> skill : skills.entrySet()) { //遍历输出技能
            int skillid = skill.getKey().getId();
            if (skillid == 32001008 || skillid == 32001009 || skillid == 32001010 || skillid == 32001011) {
                continue;
            } else {
                mplew.writeInt(skillid);//技能ID
                mplew.writeInt(skill.getValue().skillevel);//技能等级
                DateUtil.addSkillExpirationTime(mplew, skill.getValue().expiredate);
                if (skill.getKey().hasMastery() && !skill.getKey().NoMastery()) {
                    mplew.writeInt(skill.getValue().masterlevel);
                }
            }
        }
        //    mplew.writeHex("2B 00 E0 7A A4 03 0A 00 00 00 00 80 05 BB 46 E6 17 02 14 9E A4 03 02 00 00 00 00 80 05 BB 46 E6 17 02 02 00 00 00 50 A2 A4 03 0A 00 00 00 00 80 05 BB 46 E6 17 02 0A 00 00 00 49 87 93 03 00 00 00 00 00 80 05 BB 46 E6 17 02 0D 9E A4 03 01 00 00 00 00 80 05 BB 46 E6 17 02 01 00 00 00 F1 A1 A4 03 07 00 00 00 00 80 05 BB 46 E6 17 02 0A 00 00 00 8D CD A2 03 01 00 00 00 00 80 05 BB 46 E6 17 02 F5 76 A4 03 01 00 00 00 00 80 05 BB 46 E6 17 02 51 A2 A4 03 0A 00 00 00 00 80 05 BB 46 E6 17 02 0A 00 00 00 E5 4F A4 03 01 00 00 00 00 80 05 BB 46 E6 17 02 F6 76 A4 03 05 00 00 00 00 80 05 BB 46 E6 17 02 DA 7A A4 03 02 00 00 00 00 80 05 BB 46 E6 17 02 E6 4F A4 03 0A 00 00 00 00 80 05 BB 46 E6 17 02 CA 53 A4 03 14 00 00 00 00 80 05 BB 46 E6 17 02 07 9E A4 03 0A 00 00 00 00 80 05 BB 46 E6 17 02 0A 00 00 00 F7 76 A4 03 0A 00 00 00 00 80 05 BB 46 E6 17 02 DB 7A A4 03 14 00 00 00 00 80 05 BB 46 E6 17 02 FF 76 A4 03 02 00 00 00 00 80 05 BB 46 E6 17 02 E7 4F A4 03 0A 00 00 00 00 80 05 BB 46 E6 17 02 DB 87 93 03 01 00 00 00 00 80 05 BB 46 E6 17 02 0C 87 93 03 00 00 00 00 00 80 05 BB 46 E6 17 02 28 CD A2 03 14 00 00 00 00 80 05 BB 46 E6 17 02 8C C3 C9 01 00 00 00 00 00 80 05 BB 46 E6 17 02 C0 8B 93 03 01 00 00 00 00 80 05 BB 46 E6 17 02 4C A2 A4 03 0A 00 00 00 00 80 05 BB 46 E6 17 02 0A 00 00 00 DC 7A A4 03 14 00 00 00 00 80 05 BB 46 E6 17 02 E8 4F A4 03 01 00 00 00 00 80 05 BB 46 E6 17 02 CC 53 A4 03 14 00 00 00 00 80 05 BB 46 E6 17 02 3C 7B A4 03 14 00 00 00 00 80 05 BB 46 E6 17 02 2C 54 A4 03 01 00 00 00 00 80 05 BB 46 E6 17 02 C1 8B 93 03 01 00 00 00 00 80 05 BB 46 E6 17 02 E9 4F A4 03 02 00 00 00 00 80 05 BB 46 E6 17 02 2A CD A2 03 0F 00 00 00 00 80 05 BB 46 E6 17 02 3D 7B A4 03 01 00 00 00 00 80 05 BB 46 E6 17 02 0A 9E A4 03 01 00 00 00 00 80 05 BB 46 E6 17 02 01 00 00 00 2D 54 A4 03 01 00 00 00 00 80 05 BB 46 E6 17 02 F6 A1 A4 03 01 00 00 00 00 80 05 BB 46 E6 17 02 0A 00 00 00 C2 8B 93 03 01 00 00 00 00 80 05 BB 46 E6 17 02 4E A2 A4 03 01 00 00 00 00 80 05 BB 46 E6 17 02 0A 00 00 00 FA 76 A4 03 01 00 00 00 00 80 05 BB 46 E6 17 02 DE 87 93 03 B2 7A 08 00 00 80 05 BB 46 E6 17 02 0B 9E A4 03 0A 00 00 00 00 80 05 BB 46 E6 17 02 0A 00 00 00 0C 9E A4 03 01 00 00 00 00 80 05 BB 46 E6 17 02 0A 00 00 00 ");
        mplew.writeShort(chr.getAllCooldowns().size());//冷却时间
        for (PlayerCoolDownValueHolder cooling : chr.getAllCooldowns()) {
            mplew.writeInt(cooling.skillId);
            int timeLeft = (int) (cooling.length + cooling.startTime - System.currentTimeMillis());
            mplew.writeShort(timeLeft / 1000);
        }
        mplew.writeShort(0);//109
    }

    private static void addCharEntry(MaplePacketLittleEndianWriter mplew, MapleCharacter chr) {
        addCharStats(mplew, chr); //角色状态
        addCharLook(mplew, chr, false); //角色外观
        mplew.write(0);
    }

    //任务信息
    private static void addQuestRecord(MaplePacketLittleEndianWriter mplew, MapleCharacter chr) {
        List<MapleQuestStatus> started = chr.getStartedQuests();
        //以下是任务
        mplew.writeShort(started.size());
        for (MapleQuestStatus q : started) {   //检测是否接过任务
            mplew.writeShort(q.getQuest().getId()); //任务ID
            String killStr = "";
            for (int kills : q.getMobKills().values()) {  //检测是否使用需要打怪的
                killStr += StringUtil.getLeftPaddedStr(String.valueOf(kills), '0', 3);
            }
            mplew.writeMapleAsciiString(killStr);
        }

        /**
         * V1.01数据段。
         */
        mplew.write(1);

        java.util.Date date = new java.util.Date();
        List<MapleQuestStatus> completed = chr.getCompletedQuests();
        mplew.writeShort(completed.size());  //完成了多少
        for (MapleQuestStatus q : completed) { //检测是否完成了任务
            mplew.writeShort(q.getQuest().getId());//093 Short --> Int
            date.setTime(q.getCompletionTime());
            mplew.writeInt((date.getYear() % 100) * 100000000
                    + (date.getMonth() + 1) * 1000000
                    + date.getDate() * 10000
                    + (date.getHours()) * 100
                    + date.getMinutes());
            //mplew.writeLong(DateUtil.getFileTimestamp(q.getCompletionTime())); //8位 即取封包内40 D5 C2 43 95 90 CB 01
        }

    }

    //戒指信息?
    private static void addRingInfo(MaplePacketLittleEndianWriter mplew, MapleCharacter chr) {
        MapleInventory iv = chr.getInventory(MapleInventoryType.EQUIPPED);
        Collection<IItem> equippedC = iv.list();
        List<IEquip> 恋人戒指 = new ArrayList<IEquip>();
        List<IEquip> 友谊戒指 = new ArrayList<IEquip>();
        boolean 结婚戒指 = false;
        for (IItem item : equippedC) {
            if (item.友谊戒指()) {
                友谊戒指.add((MapleRing) item);
            } else if (item.恋人戒指()) {
                恋人戒指.add((MapleRing) item);
            } else if (item.结婚戒指()) {
                结婚戒指 = true;
            }
        }
        iv = chr.getInventory(MapleInventoryType.EQUIP);
        for (IItem item : iv.list()) {
            if (item.友谊戒指()) {
                友谊戒指.add((MapleRing) item);
            } else if (item.恋人戒指()) {
                恋人戒指.add((MapleRing) item);
            } else if (item.结婚戒指()) {
                结婚戒指 = true;
            }
        }
        mplew.writeShort(0);//迷你小游戏 翻牌 五子棋
        mplew.writeShort(恋人戒指.size());
        for (IEquip ring : 恋人戒指) {
            mplew.writeInt(ring.getPartnerId()); //37 3A 0A 00
            mplew.writeMapleNameString(ring.getPartnerName()); //对方的名字 且自动填充到13位 C2 B7 B7 C9 D7 A5 B0 FC CA AE BA C5 00
            mplew.writeLong(ring.getUniqueid()); //B6 22 49 00
            mplew.writeLong(ring.getPartnerUniqueId()); //B7 22 49 00
        }
        mplew.writeShort(友谊戒指.size());
        for (IEquip ring : 友谊戒指) {
            mplew.writeInt(ring.getPartnerId()); //37 3A 0A 00
            mplew.writeMapleNameString(ring.getPartnerName()); //对方的名字 且自动填充到13位 C2 B7 B7 C9 D7 A5 B0 FC CA AE BA C5 00
            mplew.writeLong(ring.getUniqueid()); //D4 26 49 00 
            mplew.writeLong(ring.getPartnerUniqueId()); //D4 26 49 00 
            mplew.writeInt(ring.getItemId());//E0 FA 10 00 四叶草
        }
        if (结婚戒指 //&& chr.isMarried()
                ) {
            mplew.writeShort(1);
            /*   mplew.writeInt(chr.getPartnerid());
             mplew.writeMapleNameString(chr.getPartnerName()); //对方的名字 且自动填充到13位 C2 B7 B7 C9 D7 A5 B0 FC CA AE BA C5 00
             mplew.writeInt(chr.getId()); //7C 40 0A 00
             mplew.writeInt(chr.getPartnerid()); //配偶cid 37 3A 0A 00*/
            mplew.writeInt(1);
            mplew.writeInt(chr.getId()); //7C 40 0A 00
            mplew.writeInt(chr.getPartnerid()); //配偶cid 37 3A 0A 00*/
            mplew.writeShort(1);
            mplew.writeInt(1112300);
            mplew.writeInt(1112300);
            mplew.writeMapleNameString(chr.getName());
            mplew.writeMapleNameString(chr.getPartnerName()); //对方的名字 且自动填充到13位 C2 B7 B7 C9 D7 A5 B0 FC CA AE BA C5 00
        } else {
            mplew.writeShort(0);
        }
    }

    private static void addTeleportRockRecord(MaplePacketLittleEndianWriter mplew, MapleCharacter chr) {
        List<Integer> maps = chr.getTRockMaps(0);
        for (int map : maps) {
            mplew.writeInt(map);
        }
        for (int i = maps.size(); i < 5; i++) {
            mplew.write(CHAR_INFO_MAGIC);
        }
        maps = chr.getTRockMaps(1);
        for (int map : maps) {
            mplew.writeInt(map);
        }
        for (int i = maps.size(); i < 23; i++) {
            mplew.write(CHAR_INFO_MAGIC);
        }
        mplew.writeShort(chr.area_data.size() + chr.getAttribute().getToAreaData());
        for (int i = 0; i < chr.area_data.size(); i++) {
            mplew.writeShort(i);
            mplew.writeMapleAsciiString(chr.area_data.get(i));
        }
        chr.getAttribute().writeAreaData(mplew);
    }

    /**
     * 得到人物信息,为一个角色。
     *
     * @param chr The character to get info about.
     * @return The character info packet.
     */
    public static MaplePacket getCharInfo(MapleCharacter chr) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.WARP_TO_MAP.getValue());
        mplew.write(HexTool.getByteArrayFromHexString("01 00 01 00 00 00 00 00 00 00"));
        mplew.writeInt(chr.getClient().getChannel() - 1);
        mplew.write(HexTool.getByteArrayFromHexString("00 00 00 00 00 00 01 00 00 00 00 01 00 00")); //新增Int
        for (int i = 0; i < 3; i++) {
            mplew.writeInt(Randomizer.getInstance().nextInt());
        }
        mplew.writeLong(-1);
        mplew.writeZero(19);
        addCharStats(mplew, chr);//人物信息
        mplew.write(chr.getBuddylist().getCapacity());
        addInventoryInfo(mplew, chr);//装备
        mplew.writeZero(9);//093新增
        mplew.write(1);
        addSkillRecord(mplew, chr);//技能
        mplew.write(1);
        addQuestRecord(mplew, chr);//任务
        addRingInfo(mplew, chr); //戒指
        addTeleportRockRecord(mplew, chr);//传送石
        /*
         * 弩骑多加 32 (1个字节) //26个0 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00
         * 00 00 00 00
         *
         * v092 05 00 //size 7A 1F 47 00 E4 59 00 00 00 0A 00 B8 0B 90 C6 26 82
         * D4 08 CC 01 FF FF FF FF 00 40 E0 FD 3B 37 4F 01 7A 1F 47 00 3D 5A 00
         * 00 00 0A 00 E4 0C D0 6F FC AE D4 08 CC 01 FF FF FF FF 00 40 E0 FD 3B
         * 37 4F 01 7A 1F 47 00 F0 59 00 00 00 1E 00 E4 0C 10 0C 5E 30 E3 08 CC
         * 01 FF FF FF FF 00 40 E0 FD 3B 37 4F 01 7A 1F 47 00 FA 59 00 00 00 48
         * 00 EE 0C F0 67 8E A6 BB 0C CC 01 FF FF FF FF 00 40 E0 FD 3B 37 4F 01
         * 7A 1F 47 00 0A 5A 00 00 00 7B 00 EF 0C 50 76 3A 34 0F 11 CC 01 FF FF
         * FF FF 00 40 E0 FD 3B 37 4F 01
         *
         * v093 05 00 //size 7A 1F 47 00 E4 59 00 00 00 0A 00 B8 0B 90 C6 26 82
         * D4 08 CC 01 FF FF FF FF 00 40 E0 FD 3B 37 4F 01 00 00 00 00 7A 1F 47
         * 00 3D 5A 00 00 00 0A 00 E4 0C D0 6F FC AE D4 08 CC 01 FF FF FF FF 00
         * 40 E0 FD 3B 37 4F 01 00 00 00 00 7A 1F 47 00 F0 59 00 00 00 1E 00 E4
         * 0C 10 0C 5E 30 E3 08 CC 01 FF FF FF FF 00 40 E0 FD 3B 37 4F 01 00 00
         * 00 00 7A 1F 47 00 FA 59 00 00 00 48 00 EE 0C F0 67 8E A6 BB 0C CC 01
         * FF FF FF FF 00 40 E0 FD 3B 37 4F 01 00 00 00 00 7A 1F 47 00 0A 5A 00
         * 00 00 7B 00 EF 0C 50 76 3A 34 0F 11 CC 01 FF FF FF FF 00 40 E0 FD 3B
         * 37 4F 01 00 00 00 00
         *
         */

        if (chr.getJob().getId() / 100 == 33) {
            mplew.write(50);
            mplew.writeZero(20);
        }
        AddSkillTree(chr, mplew);

        mplew.writeZero(2);
        mplew.write(1);
        mplew.writeZero(7);
        mplew.writeInt(1);

        mplew.writeZero(21);
        mplew.writeLong(DateUtil.getFileTimestamp(System.currentTimeMillis() + (1000 * 60 * 60 * 12)));

        mplew.writeZero(8 * 9);

        mplew.writeZero(5);
        mplew.writeZero(8);
        mplew.writeZero(4);
        mplew.write(1);
        mplew.writeZero(4);

        mplew.writeZero(8 * 8);

        mplew.writeLong(DateUtil.getFileTimestamp(System.currentTimeMillis()));
        mplew.writeInt(0x64);
        mplew.writeZero(4);
        return mplew.getPacket();
    }

    public static void AddSkillTree(MapleCharacter chr, MaplePacketLittleEndianWriter mplew) {
        mplew.writeZero(8 * 9);
    }
    /*
     * private static void addMonsterBookInfo(MaplePacketLittleEndianWriter
     * mplew, MapleCharacter chr) { mplew.writeShort(chr.ares_data.size()); for
     * (int i = 0; i < chr.ares_data.size(); i++) {
     * mplew.writeShort(chr.ares_data_infoid.get(i));
     * mplew.writeMapleAsciiString(chr.ares_data.get(i)); } mplew.write(new
     * byte[7]); mplew.writeInt(chr.getMonsterBookCover()); // cover
     * mplew.write(0); Map<Integer, Integer> cards =
     * chr.getMonsterBook().getCards(); mplew.writeShort(cards.size()); for
     * (Entry<Integer, Integer> all : cards.entrySet()) {
     * mplew.writeShort(all.getKey() % 10000); // Id
     * mplew.write(all.getValue()); // Level } }
     */

    /**
     * 得到一个空的状态更新
     *
     * @return The empy stat update packet.
     */
    public static MaplePacket enableActions() {
        /*
         * Throwable ex = new Throwable();
         *
         * StackTraceElement[] stackElements = ex.getStackTrace();
         *
         * if (stackElements != null) { for (int i = 0; i <
         * stackElements.length; i++) {
         * log.debug(stackElements[i].getClassName());
         * log.debug(stackElements[i].getFileName());
         * log.debug(stackElements[i].getLineNumber());
         * log.debug(stackElements[i].getMethodName());
         * log.debug("-----------------------------------"); } }
         */

        return updatePlayerStats(EMPTY_STATUPDATE, true, null);
    }

    /**
     * 获取更新为指定的属性。
     *
     * @param stats The stats to update.
     * @return The stat update packet.
     */
    public static MaplePacket updatePlayerStats(List<Pair<MapleStat, Number>> stats, MapleCharacter chr) {

        return updatePlayerStats(stats, false, chr);
    }

    public static MaplePacket updatePlayerStats(List<Pair<MapleStat, Number>> stats, boolean itemReaction, MapleCharacter chr) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.UPDATE_STATS.getValue());
        if (itemReaction) { //如果是使用物品更新角色
            mplew.write(1);
        } else {
            mplew.write(0);
        }
        int updateMask = 0;
        for (Pair<MapleStat, Number> statupdate : stats) {
            updateMask |= statupdate.getLeft().getValue();
        }
        List<Pair<MapleStat, Number>> mystats = stats;
        if (mystats.size() > 1) {
            Collections.sort(mystats, new Comparator<Pair<MapleStat, Number>>() {
                @Override
                public int compare(Pair<MapleStat, Number> o1, Pair<MapleStat, Number> o2) {
                    int val1 = o1.getLeft().getValue();
                    int val2 = o2.getLeft().getValue();
                    return (val1 < val2 ? -1 : (val1 == val2 ? 0 : 1));
                }
            });
        }
        mplew.writeLong(updateMask);
        for (Pair<MapleStat, Number> statupdate : mystats) {
            if (statupdate.getLeft().getValue() >= 1) {
                if (statupdate.getLeft().getValue() == MapleStat.SKIN.getValue()) {
                    mplew.write(statupdate.getRight().intValue());
                } else if (statupdate.getLeft().getValue() == MapleStat.LEVEL.getValue()
                        || statupdate.getLeft().getValue() == MapleStat.Pk等级.getValue()) {
                    mplew.write(statupdate.getRight().shortValue());
                } else if (statupdate.getLeft().getValue() == MapleStat.JOB.getValue()) {
                    mplew.writeShort(statupdate.getRight().shortValue());
                } else if (statupdate.getLeft().getValue() == MapleStat.FACE.getValue()
                        || statupdate.getLeft().getValue() == MapleStat.HAIR.getValue()) {
                    mplew.writeInt(statupdate.getRight().intValue());
                } else if (statupdate.getLeft().getValue() == MapleStat.STR.getValue()
                        || statupdate.getLeft().getValue() == MapleStat.DEX.getValue()
                        || statupdate.getLeft().getValue() == MapleStat.INT.getValue()
                        || statupdate.getLeft().getValue() == MapleStat.LUK.getValue()
                        || statupdate.getLeft().getValue() == MapleStat.AVAILABLEAP.getValue()
                        || statupdate.getLeft().getValue() == MapleStat.AVAILABLESP.getValue()) {
                    if (statupdate.getLeft().getValue() == MapleStat.AVAILABLESP.getValue()) {
                        addRemainingSp(mplew, chr);
                    } else {
                        mplew.writeShort(statupdate.getRight().shortValue());
                    }
                } else if (statupdate.getLeft().getValue() == MapleStat.HP.getValue()
                        || statupdate.getLeft().getValue() == MapleStat.MAXHP.getValue()
                        || statupdate.getLeft().getValue() == MapleStat.MP.getValue()
                        || statupdate.getLeft().getValue() == MapleStat.MAXMP.getValue()
                        || statupdate.getLeft().getValue() == MapleStat.FAME.getValue()
                        || statupdate.getLeft().getValue() == MapleStat.领袖气质.getValue()
                        || statupdate.getLeft().getValue() == MapleStat.洞察力.getValue()
                        || statupdate.getLeft().getValue() == MapleStat.意志.getValue()
                        || statupdate.getLeft().getValue() == MapleStat.手技.getValue()
                        || statupdate.getLeft().getValue() == MapleStat.感性.getValue()
                        || statupdate.getLeft().getValue() == MapleStat.魅力.getValue()
                        || statupdate.getLeft().getValue() == MapleStat.战斗经验值.getValue()
                        || statupdate.getLeft().getValue() == MapleStat.保有BP.getValue()) {
                    mplew.writeInt(statupdate.getRight().intValue());
                } else if (statupdate.getLeft().getValue() == MapleStat.MESO.getValue()
                        || statupdate.getLeft().getValue() == MapleStat.EXP.getValue()) {
                    mplew.writeLong(statupdate.getRight().longValue());
                } else { //更新宠物.经验.兵法书和宠物
                    mplew.writeInt(statupdate.getRight().intValue());//091ok
                }
            }
        }
        mplew.writeShort(0);
        return mplew.getPacket();
    }

    //传送到地图即切换地图
    public static MaplePacket getWarpToMap(int to, int spawnPoint, MapleCharacter chr) {

        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.WARP_TO_MAP.getValue());
        mplew.write(HexTool.getByteArrayFromHexString("01 00 01 00 00 00 01 00 00 00"));
        mplew.writeInt(chr.getClient().getChannel() - 1);
        mplew.writeInt(0);
        mplew.writeShort(0);//97
        mplew.write(2);//97
        mplew.writeInt(0);
        mplew.writeInt(0);
        mplew.writeInt(to); //传送的地图ID
        mplew.write(spawnPoint);
        mplew.writeInt(chr.getHp());
        mplew.writeLong(DateUtil.getFileTimestamp(System.currentTimeMillis()));
        mplew.writeInt(0x64);
        mplew.writeZero(4);

        return mplew.getPacket();
    }

    //召唤门
    public static MaplePacket spawnPortal(int townId, int targetId, Point pos) {

        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.SPAWN_PORTAL.getValue());
        mplew.writeInt(townId);
        mplew.writeInt(targetId);
        mplew.writeInt(2311002);
        if (pos != null) {
            mplew.writeShort(pos.x);
            mplew.writeShort(pos.y);
        }

        return mplew.getPacket();
    }

    public static MaplePacket spawnDoor(int oid, Point pos, boolean town) {

        // [D3 00] [01] [93 AC 00 00] [6B 05] [37 03]
        //
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.SPAWN_DOOR.getValue());
        mplew.write(town ? 1 : 0);
        mplew.writeInt(oid);
        mplew.writeShort(pos.x);
        mplew.writeShort(pos.y);

        return mplew.getPacket();
    }

    public static MaplePacket removeDoor(int oid, boolean town) {

        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        if (town) {
            mplew.writeShort(SendPacketOpcode.SPAWN_PORTAL.getValue());
            mplew.writeInt(999999999);
            mplew.writeInt(999999999);
        } else {
            mplew.writeShort(SendPacketOpcode.REMOVE_DOOR.getValue());
            mplew.write(/*
                     * town ? 1 :
                     */0);
            mplew.writeInt(oid);
        }

        return mplew.getPacket();
    }

    //召唤召唤兽
    public static MaplePacket spawnSpecialMapObject(MapleSummon summon, int skillLevel) {

        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        /*
         * 20 01 5F 00 1F 00 //cid A5 41 01 00 //objid 5D 78 2F 00 //skillid 8E
         * //player level 0F //skill level 8E FF //x 8B 00 //y 04 08 00 04
         * //召唤兽类型 summon.getMovementType().getValue() 01 //召唤兽状态 status 01
         * //召唤兽是不是活的 地雷 活 00 //技能是不是傀儡召唤技能
         */
        /*
         * 傀儡召唤 20 01 DF 8A 4D 00 80 0B 18 00 0E 3D 42 00 95 01 2A 00 5E FE
         *
         * 04 11 00
         *
         * 00 00 01 01
         *
         * 00 00 89 4E 00 00 00 E2 81 00 00 01 DF 4D 0F 00 02 73 71 0F 00 03 60
         * 98 0F 00 04 7F BF 0F 00 05 CD E6 0F 00 06 A6 34 10 00 07 3D 5C 10 00
         * 08 32 83 10 00 09 80 D1 10 00 0A 38 7A 14 00 0B 54 53 14 00 0C 55 F9
         * 10 00 11 40 1F 11 00 12 B0 05 1D 00 13 C0 2C 1D 00 1A 24 6E 11 00 1D
         * 3A 46 11 00 1E 36 94 11 00 FF 01 75 4B 0F 00 02 DF 71 0F 00 03 5F 98
         * 0F 00 04 50 BF 0F 00 05 EE DE 0F 00 06 0A 2D 10 00 07 2B 5C 10 00 08
         * 86 83 10 00 09 7E D1 10 00 FF 85 F9 19 00
         *
         * 00 00 00 00 00 00 00 00 00 00 00 00
         */
        //log.info("打印调用方：", new Throwable());
        mplew.writeShort(SendPacketOpcode.SPAWN_SPECIAL_MAPOBJECT.getValue());
        mplew.writeInt(summon.getOwner().getId());
        mplew.writeInt(summon.getObjectId());
        mplew.writeInt(summon.getSkill());
        mplew.write(summon.getOwner().getLevel()); //人物等级
        //mplew.write(summon.getSkill() == 35121011 ? 1 : skillLevel);
        mplew.write(summon.getSkill() == 35121011 ? 1 : skillLevel);
        mplew.writeShort(summon.getPosition().x);
        mplew.writeShort(summon.getPosition().y);
        mplew.write(summon.getOtherVal3());
        mplew.write(summon.getOtherVal()); //这个值也是每个召唤兽都不同 需要在summon类设置个变量储存
        mplew.write(summon.getOtherVal4());
        mplew.write(summon.getMovementType().getValue());
        mplew.write(summon.getStatus());
        mplew.write(summon.getActionstats());
        mplew.write((summon.isPuppet() || summon.isRingSummon()) ? 1 : 0);//木偶
        mplew.write(summon.getSkill() == 4341006 ? 0x1 : 0);//097
        if (summon.isRobot()) { //磁场
            //log.debug("召唤兽是磁场");
            mplew.write(0);
        }
        if (summon.getSkill() == 4341006) { //傀儡召唤
            addCharLook(mplew, summon.getOwner(), true);
        }
        ////log.debug("召唤兽封包"+mplew.getPacket());
        return mplew.getPacket();
    }

    public static void addCharLook2(MaplePacketLittleEndianWriter mplew, MapleCharacter chr, boolean mega) {

        mplew.write(chr.getGender());
        mplew.write(chr.getSkinColor().getId());
        mplew.writeInt(chr.getFace());
        mplew.writeInt(chr.getJob().getId());
        mplew.write(0);
        mplew.writeInt(chr.getHair());
        addCharEquips(mplew, chr.getInventory(MapleInventoryType.EQUIPPED), chr);
        mplew.writeInt(0);
        mplew.writeInt(0);
        mplew.writeInt(0);
    }

    public static void addCharEquips(MaplePacketLittleEndianWriter mplew, MapleInventory equip, MapleCharacter chr) {

        Map<Byte, Integer> myEquip = new LinkedHashMap<Byte, Integer>();
        Map<Byte, Integer> maskedEquip = new LinkedHashMap<Byte, Integer>();
        synchronized (equip) {
            for (IItem item : equip.list()) {
                byte pos = (byte) (item.getPosition() * -1);
                if (pos < 100 && myEquip.get(pos) == null) {
                    ////log.debug("1：" + pos);
                    myEquip.put(pos, item.getItemId());
                } else if ((pos > 100/*
                         * || pos == -128
                         */) && pos != 111) {
                    pos -= 100;
                    if (myEquip.get(pos) != null) {
                        ////log.debug("2：" + pos);
                        maskedEquip.put(pos, myEquip.get(pos));
                    }
                    ////log.debug("3：" + pos);
                    myEquip.put(pos, item.getItemId());
                } else if (myEquip.get(pos) != null) {
                    ////log.debug("4：" + pos);
                    maskedEquip.put(pos, item.getItemId());
                }
            }
            for (Entry<Byte, Integer> entry : myEquip.entrySet()) { //装备
                mplew.write(entry.getKey());
                mplew.writeInt(entry.getValue());
            }
            mplew.write(0xFF);
            for (Entry<Byte, Integer> entry : maskedEquip.entrySet()) { //饰品
                mplew.write(entry.getKey());
                mplew.writeInt(entry.getValue());
            }
            mplew.write(0xFF);
            IItem cWeapon = equip.getItem((byte) -111);
            mplew.writeInt(cWeapon != null ? cWeapon.getItemId() : 0);
        }
    }

    /**
     * Gets a packet to remove a special map object.
     *
     * @param summon
     * @param animated Animated removal?
     * @return The packet removing the object.
     */
    public static MaplePacket removeSpecialMapObject(MapleSummon summon, boolean animated) {

        // [86 00] [6A 4D 27 00] 33 1F 00 00 02
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.REMOVE_SPECIAL_MAPOBJECT.getValue());
        mplew.writeInt(summon.getOwner().getId());
        mplew.writeInt(summon.getObjectId());
        mplew.write(animated ? summon.getRemoveStatus() : 1);
        return mplew.getPacket();
    }

    //生成身上的装备方法
    protected static void addItemInfo(MaplePacketLittleEndianWriter mplew, IItem item) {
        addItemInfo(mplew, item, false, false, false);
    }

    protected static void addItemInfo(MaplePacketLittleEndianWriter mplew, IItem item, boolean zeroPosition, boolean leaveOut, boolean AddFormDorp) {
        addItemInfo(mplew, item, zeroPosition, leaveOut, false, AddFormDorp, false);
    }

    protected static void addItemInfo(MaplePacketLittleEndianWriter mplew, IItem item, boolean AddFormDorp) {
        addItemInfo(mplew, item, false, false, false, AddFormDorp, AddFormDorp);
    }

    protected static void addItemInfoNoPos(MaplePacketLittleEndianWriter mplew, IItem item) {
        addItemInfo(mplew, item, false, false, false, true, true);
    }

    private static void addItemInfo(MaplePacketLittleEndianWriter mplew, IItem item, boolean zeroPosition, boolean leaveOut, boolean cs, boolean AddFormDorp, boolean nopos) {//, boolean AddFormDorp
       /*
         * if (item.getUniqueId() > 0) { if (item.getItemId() >= 5000000 &&
         * item.getItemId() <= 5000100) { //褐色小猫 addPetItemInfo(mplew, item,
         * zeroPosition, leaveOut, cs);//宠物装备 } else if (item.友谊戒指() ||
         * item.恋人戒指()) { addRingItemInfo(mplew, item, zeroPosition, leaveOut,
         * cs);//戒指装备 } else { addCashItemInfo(mplew, item, zeroPosition,
         * leaveOut, cs);//点装 } } else { if
         * (MapleItemInformationProvider.getInstance().isCash(item.getItemId()))
         * //093新增 由于物品也需要 { addCashItemInfo(mplew, item, zeroPosition,
         * leaveOut, true);//点装 } else { addNormalItemInfo(mplew, item,
         * zeroPosition, leaveOut, AddFormDorp, nopos);//普通装备 } }
         */
        addNormalItemInfo(mplew, item, zeroPosition, leaveOut, AddFormDorp, nopos);//普通装备
    }

    //普通物品的生成
    private static void addNormalItemInfo(MaplePacketLittleEndianWriter mplew, IItem item, boolean zeroPosition, boolean leaveOut, boolean AddFormDorp, boolean nopos) {
        boolean iscash = ii.isCash(item.getItemId());
        short pos = item.getPosition();
        if (!nopos) {
            if (zeroPosition && !leaveOut) {  //判断是否是起始位置
                mplew.write(0);
            } else if (pos <= -1) {
                pos *= -1;
                if (pos > 100 && pos < 1000) {
                    mplew.write(pos - 100);
                } else {
                    if (pos >= 1200) {
                        mplew.writeShort(pos);
                    } else {
                        mplew.write(pos);
                    }
                }
            } else {
                mplew.write(pos);  //装备部位
            }
            if (item.getType() == IItem.EQUIP && !AddFormDorp && pos < 1200) {
                if (pos >= 1000 && pos < 1100) { //龙神
                    mplew.write(3);
                    ////log.debug("加载龙神装备"+item.getItemId());
                } else if (pos >= 1100 && pos < 1200) { //机械师
                    mplew.write(4);
                } else { //普通装备
                    mplew.write(0);//生成装备列表时出现这个 但拣装备时不出现
                }
                //安卓装备在cashitem加载
            }
        }
        mplew.write(item.getType());  //装备类型?
        mplew.writeInt(item.getItemId());  //装备ID?
        mplew.write(iscash ? 1 : 0);// 如果不是点券道具则继续 只返回0和1
        if (iscash) {
            mplew.writeLong(item.getUniqueid() == 0 ? -1 : item.getUniqueid());
        }
        mplew.writeLong(DateUtil.getFileTimestamp(item.getExpiration() == null ? FINAL_LOCKITEMTIME : item.getExpiration().getTime()));
        mplew.writeInt(-1);//新增
        if (item.getType() == IItem.EQUIP) {
            Equip equip;
            equip = (Equip) item;

            equip.writePacket(mplew);
            mplew.writeInt(4);//110变更
            mplew.write(-1);
            mplew.writeMapleAsciiString(equip.getOwner()); //拥有者名字 没名字发 short 0
            mplew.write(equip.getIdentify());//1 未鉴定   0 C级 0x11 B级 0x12 A级 0x13 S级
            mplew.write(equip.getStarlevel());//星级
            mplew.writeShort(equip.getPotential_1());
            mplew.writeShort(equip.getPotential_2());
            mplew.writeShort(equip.getPotential_3());
            mplew.writeInt(0);
            mplew.writeShort(0);//同系列外形后4位数字
            mplew.writeShort(0);//107 ukon
            mplew.writeShort(0);//109
            for (int i = 0; i < 6; i++) {
                mplew.write(-1);
            }
            if (!iscash) {
                mplew.writeInt(-1); //BA 1C 00 00 B4 00 00 00
                mplew.writeInt(-1);
            }
            mplew.writeLong(DateUtil.getFileTimestamp(equip.getUnlockTime() == null ? FINAL_TIME : equip.getUnlockTime().getTime()));
            mplew.writeInt(-1);
            /**
             * V101新数据。
             */
            mplew.writeLong(0);
            mplew.writeLong(DateUtil.getFileTimestamp(FINAL_TIME));
            mplew.writeLong(0);
            mplew.writeLong(0);
            mplew.writeShort(0);//105++

        } else if (item.getType() == IItem.ITEM) { //如果是物品道具
            mplew.writeShort(item.getQuantity()); //道具数量
            mplew.writeMapleAsciiString(item.getOwner());//道具拥有者
            mplew.writeShort(item.getFlag());
            if (ii.isThrowingStar(item.getItemId()) || ii.isBullet(item.getItemId())) {
                mplew.write(HexTool.getByteArrayFromHexString("02 00 00 00 54 00 00 34"));
            }
        } else {
            MaplePet pet = (MaplePet) item;
            mplew.writeMapleNameString(pet.getName()); //自动填充到13位
            mplew.write(pet.getLevel()); //宠物等级
            mplew.writeShort(pet.getCloseness()); //宠物亲密度
            mplew.write(pet.getFullness()); //宠物饥饿度
            mplew.writeLong(DateUtil.getFileTimestamp(item.getExpiration() == null ? FINAL_TIME : item.getExpiration().getTime()));
            mplew.writeShort(0);//??
            mplew.writeHex("5F 00");//宠物技能
            mplew.writeZero(6);
            mplew.write(pet.getSlot() + 1);
            mplew.writeInt(0);
            mplew.writeInt(-1);
            mplew.writeZero(14);
        }
    }

    //返回错误信息
    public static MaplePacket getRelogResponse() {

        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter(3);
        mplew.writeShort(SendPacketOpcode.RELOG_RESPONSE.getValue());
        mplew.write(1);

        return mplew.getPacket();
    }

    public static MaplePacket serverNotice(String message) {
        return serverMessage(6, 0, message, false, false, null);
    }
    //服务器公告

    public static MaplePacket serverMessage(String message) {
        return serverMessage(4, 0, message, true, false, null);
    }

    public static MaplePacket serverNotice(int type, String message) {
        return serverMessage(type, 0, message, false, false, null);
    }

    public static MaplePacket serverNotice(int type, int channel, String message) {
        return serverMessage(type, channel, message, false, false, null);
    }

    public static MaplePacket serverNotice(int type, int channel, String message, boolean smegaEar) {
        return serverMessage(type, channel, message, false, smegaEar, null);
    }

    public static MaplePacket serverNotice(PhoneType type, int channel, String message, boolean smegaEar) {
        return serverMessage(type.getValue(), channel, message, false, smegaEar, null);
    }

    /**
     * 服务器公告。
     *
     * @param type 类型 - [0 - 弹窗] [1 - 蓝色条] [2 - 红喇叭] [3 - 高质量喇叭] [5 - 系统公告] [8-9
     * - 道具喇叭] [0xf - 蛋糕喇叭] [0x0a - 缤纷喇叭] [0x10 - 馅饼喇叭] [0x11 - 心脏高级喇叭] [0x12 -
     * 白骨高级喇叭] [0x13 - 红色抽奖公告(迷你蛋)] [0x14 - 绿色抽奖公告] [0x16 - 服务器公告]
     * @param channel
     * @param message
     * @param servermessage
     * @param megaEar
     * @return
     */
    public static MaplePacket serverMessage(int type, int channel, String message, boolean servermessage, boolean megaEar, Item showItem) {
        PhoneType phoneType = PhoneType.get(type);

        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.SERVERMESSAGE.getValue());
        mplew.write(type);//消息类型
        if (servermessage) {
            mplew.write(1);
        }
        mplew.writeMapleAsciiString(message);
        if (phoneType != null && phoneType.equals(PhoneType.绿色抽奖公告)) {
            mplew.writeInt(showItem.getItemId());
        }
        if (phoneType != null && phoneType.writeChannel()) {
            mplew.write(channel - 1); // channel
            mplew.write(megaEar ? 1 : 0);
        }
        if (type == 6) {
            mplew.writeInt(0);
        }
        if (phoneType != null && phoneType.equals(PhoneType.绿色抽奖公告)) {
            mplew.writeZero(6);
            mplew.write(0);
            //addItemInfo(mplew, showItem, true);
        }

        return mplew.getPacket();
    }

    /**
     * 红字喇叭
     *
     * @param channel
     * @param message
     * @param item
     * @param megaEar
     * @return
     */
    public static MaplePacket getItemMega(int channel, String message, IItem item) {

        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.SERVERMESSAGE.getValue());
        mplew.write(PhoneType.红色抽奖公告.getValue());
        mplew.writeMapleAsciiString(message);
        mplew.writeInt(item.getItemId());
        mplew.writeInt(channel - 1); // channel
        mplew.writeInt(0); // ?
        addItemInfo(mplew, item, true);
        return mplew.getPacket();
    }

    public static MaplePacket getItemMegas(int channel, String message, IItem item, boolean megaEar) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.SERVERMESSAGE.getValue());

        mplew.write(PhoneType.绿色抽奖公告.getValue());
        mplew.writeMapleAsciiString(message);
        mplew.writeInt(item.getItemId());
        mplew.writeInt(channel - 1); // channel
        mplew.writeInt(0); // ?
        addItemInfo(mplew, item, true);
        return mplew.getPacket();
    }

    /**
     * 绿字喇叭
     *
     * @param channel
     * @param message
     * @param item
     * @param megaEar
     * @return
     */
    public static MaplePacket getItemMegaI(int channel, String message, IItem item) {

        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.SERVERMESSAGE.getValue());
        mplew.write(PhoneType.绿色抽奖公告.getValue());
        mplew.writeMapleAsciiString(message);
        mplew.writeInt(item.getItemId());
        mplew.writeInt(channel - 1); // channel
        mplew.writeInt(0); // ?
        addItemInfo(mplew, item, true);
        return mplew.getPacket();
    }

    /**
     * 道具喇叭和高质量喇叭
     *
     * @param type
     * @param channel
     * @param message
     * @param item
     * @param showEar
     * @return
     */
    public static MaplePacket getMegaphone(PhoneType type, int channel, String message, IItem item, boolean showEar) {

        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.SERVERMESSAGE.getValue());
        mplew.write(type.getValue());
        mplew.writeMapleAsciiString(message);
        if (type == PhoneType.高质量喇叭) {
            mplew.write(channel - 1);
            mplew.write(showEar ? 1 : 0);
        } else if (type == PhoneType.道具喇叭) {
            mplew.write(channel - 1);
            mplew.write(showEar ? 1 : 0);
            mplew.write(item == null ? 0 : 1);
            if (item != null) {
                addNormalItemInfo(mplew, item, false, false, true, true);
            }
        }
        return mplew.getPacket();
    }

    /**
     * 缤纷喇叭。
     *
     * @param channel
     * @param message
     * @param megaEar
     * @return
     */
    public static MaplePacket getMultiMega(int channel, List<String> message, boolean megaEar) {

        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.SERVERMESSAGE.getValue());
        mplew.write(PhoneType.缤纷喇叭.getValue());
        mplew.writeMapleAsciiString(message.get(0));
        mplew.write(message.size());
        for (int i = 0; i < message.size() - 1; i++) {
            mplew.writeMapleAsciiString(message.get(i + 1));
        }
        mplew.write((byte) (channel - 1)); // channel
        mplew.write(megaEar ? 1 : 0);
        return mplew.getPacket();
    }

    /**
     * 用户使用情景喇叭.
     *
     * @param chr
     * @param channel
     * @param itemId
     * @param message
     * @param ear
     * @return
     */
    public static MaplePacket getAvatarMega(MapleCharacter chr, int channel, int itemId, List<String> message, boolean ear) {

        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.AVATAR_MEGA.getValue());
        mplew.writeInt(itemId);//喇叭ID
        mplew.writeMapleAsciiString(chr.getName());//使用者姓名
        for (String s : message) {
            mplew.writeMapleAsciiString(s);
        }
        mplew.writeInt(channel - 1); // channel
        mplew.write(ear ? 1 : 0);
        addCharLook(mplew, chr, true);//角色外观

        return mplew.getPacket();
    }

    /**
     * Gets a NPC spawn packet.
     *
     * @param life The NPC to spawn.
     * @return The NPC spawn packet.
     */
    public static MaplePacket spawnNPC(MapleNPC life) {

        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.SPAWN_NPC.getValue());
        mplew.writeInt(life.getObjectId());
        mplew.writeInt(life.getId());
        mplew.writeShort(life.getPosition().x);
        //     mplew.writeMapleAsciiString("123");
        mplew.writeShort(life.getCy());
        mplew.write(life.getF() == 1 ? 0 : 1);
        mplew.writeShort(life.getFh());
        mplew.writeShort(life.getRx0());

        mplew.writeShort(life.getRx1());
        mplew.write(1);


        return mplew.getPacket();
    }

    public static MaplePacket spawnNPCRequestController(MapleNPC life, boolean show2) {

        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.SPAWN_NPC_REQUEST_CONTROLLER.getValue());
        mplew.write(1);
        mplew.writeInt(life.getObjectId());
        mplew.writeInt(life.getId());
        mplew.writeShort(life.getPosition().x);
        mplew.writeShort(life.getCy());
        mplew.write(life.getF() == 1 ? 0 : 1);
        mplew.writeShort(life.getFh());
        mplew.writeShort(life.getRx0());
        mplew.writeShort(life.getRx1());
        mplew.write(show2 ? 1 : 0);

        return mplew.getPacket();
    }

    public static MaplePacket removeNPC(int objid) {

        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.REMOVE_NPC.getValue());
        mplew.writeInt(objid);
        return mplew.getPacket();
    }

    public static MaplePacket spawnMonster(MapleMonster life, boolean newSpawn) {
        return spawnMonsterInternal(life, false, newSpawn, false, 0, false);
    }

    /**
     * Gets a spawn monster packet.
     *
     * @param life The monster to spawn.
     * @param newSpawn Is it a new spawn?
     * @param effect The spawn effect.
     * @return The spawn monster packet.
     */
    public static MaplePacket spawnMonster(MapleMonster life, boolean newSpawn, int effect) {
        return spawnMonsterInternal(life, false, newSpawn, false, effect, false);
    }

    /**
     * Gets a control monster packet.
     *
     * @param life The monster to give control to.
     * @param newSpawn Is it a new spawn?
     * @param aggro Aggressive monster?
     * @return The monster control packet.
     */
    public static MaplePacket controlMonster(MapleMonster life, boolean newSpawn, boolean aggro) {
        return spawnMonsterInternal(life, true, newSpawn, aggro, 0, false);
    }
    /*
     * public static MaplePacket makeMonsterInvisible(MapleMonster life) {
     * return spawnMonsterInternal(life, true, false, false, 0, true); }
     *
     */

    /**
     * Internal function to handler monster spawning and controlling.
     *
     * @param life The mob to perform operations with.
     * @param requestController Requesting control of mob?
     * @param newSpawn New spawn (fade in?)
     * @param aggro Aggressive mob?
     * @param effect The spawn effect to use.
     * @return The spawn/control packet.
     */
    private static MaplePacket spawnMonsterInternal(MapleMonster life, boolean requestController, boolean newSpawn, boolean aggro, int effect, boolean makeInvis) {
        //if(show)//log.debug("调用的函数："+new Throwable().getStackTrace()[0]); //显示调用的类 函数名 函数所在行
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        if (makeInvis) {
            //log.info("召唤怪物requestController2:"+life.getObjectId());
            //透明怪物 武陵道场打死boss之后抓到此包
            mplew.writeShort(SendPacketOpcode.SPAWN_MONSTER_CONTROL.getValue());
            mplew.write(0);
            mplew.writeInt(life.getObjectId());
            return mplew.getPacket();
        }
        if (requestController) {
            //log.info("召唤怪物requestController2:"+life.getObjectId());
            mplew.writeShort(SendPacketOpcode.SPAWN_MONSTER_CONTROL.getValue());
            if (aggro) {
                mplew.write(2);
            } else {
                mplew.write(1);
            }
        } else {
            //log.info("召唤怪物SPAWN_MONSTER:"+life.getObjectId());
            mplew.writeShort(SendPacketOpcode.SPAWN_MONSTER.getValue());
        }
        mplew.writeInt(life.getObjectId());
        mplew.write(1);
        mplew.writeInt(life.getId());//怪物ID
        mplew.writeZero(33); //093新增 109+4
        mplew.write(HexTool.getByteArrayFromHexString("E0 13 08 00 00 00 00 88"));//97
        mplew.writeZero(40);
        mplew.writeZero(7);//97
        mplew.writeShort(life.getPosition().x);
        mplew.writeShort(life.getPosition().y);
        mplew.write(life.getStance());
        mplew.writeShort(life.getStartFh());
        mplew.writeShort(life.getFh());
        //这里应该是无论有无有effect都是要发送5个字节给客户端..
        if (effect > 0) {
            mplew.write(effect);
            mplew.write(0);
            mplew.writeShort(0);
            if (effect == 15) { //武陵道场召唤效果
                mplew.write(0);
            }
        }
        if (newSpawn) { //新召唤出来的怪有一个渐出效果
            mplew.write(-2);
        } else { //进入地图时原本存在的怪就直接出现
            mplew.write(-1);
        }
        mplew.write(-1);
        mplew.writeInt(0);
        mplew.writeZero(16);
        for (int i = 0; i < 5; i++) {
            mplew.write(-1);
        }
        mplew.writeZero(9);
        //log.error("怪物封包:"+mplew.getPacket());
        return mplew.getPacket();
    }

    /**
     * Handles monsters not being targettable, such as Zakum's first body.
     *
     * @param life The mob to spawn as non-targettable.
     * @param effect The effect to show when spawning.
     * @return The packet to spawn the mob as non-targettable.
     */
    public static MaplePacket spawnFakeMonster(MapleMonster life, int effect) {
        //if(show)//log.debug("调用的函数："+new Throwable().getStackTrace()[0]); //显示调用的类 函数名 函数所在行
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        // log.info((new StringBuilder()).append("SPAWN_MONSTER_CONTROL 假的ID:").append(life.getObjectId()).toString());
        mplew.writeShort(SendPacketOpcode.SPAWN_MONSTER_CONTROL.getValue());
        mplew.write(2); //1 - 不攻击状态 2 - 攻击状态
        mplew.writeInt(life.getObjectId());
        mplew.write(life.getController() == null ? 5 : 1);
        mplew.writeInt(life.getId());
        mplew.writeZero(33); //093新增 109+4
        mplew.write(HexTool.getByteArrayFromHexString("E0 13 08 00"));//97
        mplew.writeInt(0x88000000);
        mplew.writeZero(40);
        mplew.writeZero(7);//97
        mplew.writeShort(life.getPosition().x);
        mplew.writeShort(life.getPosition().y);
        mplew.write(life.getStance());
        mplew.writeShort(life.getStartFh());
        mplew.writeShort(life.getFh());
        if (effect > 0) {
            mplew.write(effect);
            mplew.write(0);
            mplew.writeShort(0);
        }
        mplew.write(-2); //立刻出现是 -1 有出现效果是 -2
        //   mplew.write(-1);
      /*  mplew.writeShort(-1);//97
         mplew.write(0xF);
         mplew.writeZero(7);
         mplew.write(-1);//97*/

        mplew.write(-1);
        mplew.write(0xF);
        mplew.writeZero(19);
        for (int i = 0; i < 5; i++) {
            mplew.write(-1);
        }
        mplew.writeZero(4);
        mplew.write();
        mplew.writeZero(4);
        return mplew.getPacket();
    }

    /**
     * Makes a monster previously spawned as non-targettable, targettable.
     *
     * @param life The mob to make targettable.
     * @return The packet to make the mob targettable.
     */
    /*
     * public static MaplePacket makeMonsterReal(MapleMonster life) {
     *
     * MaplePacketLittleEndianWriter mplew = new
     * MaplePacketLittleEndianWriter();
     *
     * mplew.writeShort(SendPacketOpcode.SPAWN_MONSTER.getValue());
     * mplew.writeInt(life.getObjectId()); mplew.write(1);
     * mplew.writeInt(life.getId());//怪物ID mplew.write(new byte[24]);//97
     * mplew.write(0); mplew.write(HexTool.getByteArrayFromHexString("E0 03 00
     * 00"));//97 mplew.writeInt(0x88000000);//096新增 mplew.write(new
     * byte[40]);//096新增 //mplew.write(new byte[20]);//97 //mplew.writeInt(0);
     * //mplew.write(136);
     *
     * mplew.writeInt(0); //mplew.writeShort(0);
     * mplew.writeShort(life.getPosition().x);
     * mplew.writeShort(life.getPosition().y); mplew.write(life.getStance());
     * mplew.writeShort(life.getStartFh()); mplew.writeShort(life.getFh());
     * mplew.writeShort(-1); mplew.writeInt(0); mplew.write(-1);//096
     *
     * return mplew.getPacket(); }
     */
    public static MaplePacket makeMonsterReal(MapleMonster life) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.SPAWN_MONSTER.getValue());
        mplew.writeInt(life.getObjectId());
        mplew.write(1);
        mplew.writeInt(life.getId());
        mplew.writeZero(33); //093新增 109+4
        mplew.write(HexTool.getByteArrayFromHexString("E0 13 08 00"));
        mplew.writeInt(-2013265920);
        mplew.writeZero(40);
        mplew.writeZero(7);//97

        mplew.writeShort(life.getPosition().x);
        mplew.writeShort(life.getPosition().y);
        mplew.write(life.getStance());
        mplew.writeShort(life.getStartFh());
        mplew.writeShort(life.getFh());
        mplew.writeShort(-1);
        mplew.writeInt(0);
        mplew.writeZero(16);
        for (int i = 0; i < 5; i++) {
            mplew.write(-1);
        }
        mplew.writeZero(9);

        return mplew.getPacket();
    }

    /**
     * Gets a stop control monster packet.
     *
     * @param oid The ObjectID of the monster to stop controlling.
     * @return The stop control monster packet.
     */
    public static MaplePacket stopControllingMonster(int oid) {

        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.SPAWN_MONSTER_CONTROL.getValue());
        mplew.write(0);
        mplew.writeInt(oid);

        return mplew.getPacket();
    }

    /**
     * Gets a response to a move monster packet.
     *
     * @param objectid The ObjectID of the monster being moved.
     * @param moveid The movement ID.
     * @param currentMp The current MP of the monster.
     * @param useSkills Can the monster use skills?
     * @return The move response packet.
     */
    public static MaplePacket moveMonsterResponse(int objectid, short moveid, int currentMp, boolean useSkills) {
        return moveMonsterResponse(objectid, moveid, currentMp, useSkills, 0, 0);
    }

    /**
     * Gets a response to a move monster packet.
     *
     * @param objectid The ObjectID of the monster being moved.
     * @param moveid The movement ID.
     * @param currentMp The current MP of the monster.
     * @param useSkills Can the monster use skills?
     * @param skillId The skill ID for the monster to use.
     * @param skillLevel The level of the skill to use.
     * @return The move response packet.
     */
    public static MaplePacket moveMonsterResponse(int objectid, short moveid, int currentMp, boolean useSkills, int skillId, int skillLevel) {

        //1F 01 [A1 82 B2 05] [10 00] [00] [0A 00] [00] [00]
        //4F 01 [16 38 0C 00] [01 00] [00] [0A 00] [00] [00] 00 00 00 00 //093新增int
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.MOVE_MONSTER_RESPONSE.getValue());
        mplew.writeInt(objectid);
        mplew.writeShort(moveid);
        mplew.write(useSkills ? 1 : 0);
        mplew.writeShort(currentMp);
        mplew.write(skillId);
        mplew.write(skillLevel);
        mplew.writeInt(0);//新增
        return mplew.getPacket();
    }

    /**
     * 普通聊天获取数据包。
     *
     * @param cidfrom The character ID who sent the chat.
     * @param text The text of the chat.
     * @param whiteBG
     * @param show
     * @return The general chat packet.
     */
    //[B3 00] [E5 B3 29 00] [00] 08 00 CE D2 CA C7 C4 E3 C2 E8 00
    public static MaplePacket getChatText(int cidfrom, String text, boolean whiteBG, int show2) {

        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.CHATTEXT.getValue());
        mplew.writeInt(cidfrom);
        mplew.write(whiteBG ? 1 : 0);//字是否是白色背景
        mplew.writeMapleAsciiString(text);//说话的内容
        mplew.write(show2);
        return mplew.getPacket();
    }

    /**
     * 增加经验值。
     *
     * @param gain The amount of EXP gained.获取经验的数量
     * @param inChat In the chat box?
     * @param white White text or yellow?白色或者蓝色
     * @return The exp gained packet.
     */
    public static MaplePacket getShowExpGain(int gain, boolean inChat, boolean white) {

        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.SHOW_STATUS_INFO.getValue());
        mplew.write(3); // 0 = 显示未能找到掉在地上的部分金额 1 2=没效果 4 = 获得SP 3 = 经验, 5 = 人气, 6 = 金币, 7 = 家族CP
        mplew.write(white ? 1 : 0);
        mplew.writeInt(gain);
        mplew.write(inChat ? 1 : 0);
        mplew.writeZero(29);//其他函数必须多几个字节
        return mplew.getPacket();
    }

    /**
     * 增加人气
     *
     * @param gain How many fame gained.
     * @return The meso gain packet.
     */
    public static MaplePacket getShowFameGain(int gain) {

        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.SHOW_STATUS_INFO.getValue());
        mplew.write(5);
        mplew.writeInt(gain);

        return mplew.getPacket();
    }

    //金币
    public static MaplePacket getShowMesoGain(int gain) {
        return getShowMesoGain(gain, false);
    }

    /**
     * 这个包已经失效
     *
     * Gets a packet telling the client to show a meso gain. 增加金币
     *
     * @param gain How many mesos gained.
     * @param inChat Show in the chat window?
     * @return The meso gain packet.
     */
    public static MaplePacket getShowMesoGain(long gain, boolean inChat) {

        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.SHOW_STATUS_INFO.getValue());
        if (!inChat) {
            mplew.write(0);
            mplew.write(1);
            mplew.write(0);
        } else {
            mplew.write(6);
        }
        mplew.writeLong(gain);//094修改

        return mplew.getPacket();
    }

    // 物品道具
    public static MaplePacket getShowItemGain(int itemId, short quantity) {
        return getShowItemGain(itemId, quantity, false);
    }

    /**
     * Gets a packet telling the client to show an item gain. 获得物品
     *
     * @param itemId The ID of the item gained.
     * @param quantity The number of items gained.
     * @param inChat Show in the chat window?
     * @return The item gain packet.
     */
    public static MaplePacket getShowItemGain(int itemId, short quantity, boolean inChat) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        /*
         * 23 01 05 02 02 C4 41 00 1B 00 00 00 24 FA 10 00 01 00 00 00
         */
        if (inChat) {
            mplew.writeShort(SendPacketOpcode.SHOW_ITEM_GAIN_INCHAT.getValue());
            mplew.write(5);
            mplew.write(1);//物品数量
            mplew.writeInt(itemId);//物品ID
            mplew.writeInt(quantity);//物品数量
        } else {
            mplew.writeShort(SendPacketOpcode.SHOW_STATUS_INFO.getValue());
            mplew.writeShort(0);
            mplew.writeInt(itemId);//物品ID
            mplew.writeInt(quantity);//获得物品的数量
            mplew.writeInt(0);
            mplew.writeInt(0);
        }

        return mplew.getPacket();
    }

    public static MaplePacket killMonster(int oid, boolean animation) {
        return killMonster(oid, animation ? 1 : 0);
        //   return killMonster(oid, Randomizer.getInstance().nextInt(2));
    }

    /**
     * 杀死怪物的方法
     *
     * @param oid The objectID of the killed monster.
     * @param animation 0 = disapear, 1 = fade out, 2+ = special
     * @return The kill monster packet.
     */
    public static MaplePacket killMonster(int oid, int animation) {


        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.KILL_MONSTER.getValue());
        mplew.writeInt(oid);
        mplew.write(animation); //死的类型 死的时候显示的动画
        return mplew.getPacket();
    }

    //吞噬技能的杀怪效果
    public static MaplePacket SpecialKillMonster(int oid, int characterid) {

        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.KILL_MONSTER.getValue());
        mplew.writeInt(oid);
        mplew.write(4); //死的类型 死的时候显示的动画
        mplew.writeInt(characterid);
        return mplew.getPacket();
    }

    /**
     * Gets a packet telling the client to show mesos coming out of a map
     * object.
     *
     * @param amount The amount of mesos.
     * @param itemoid The ObjectID of the dropped mesos.
     * @param dropperoid The OID of the dropper.
     * @param ownerid The ID of the drop owner.
     * @param dropfrom Where to drop from.
     * @param dropto Where the drop lands.
     * @param mod ?
     * @return The drop mesos packet.
     */
    public static MaplePacket dropMesoFromMapObject(int amount, int itemoid, int dropperoid, int ownerid, Point dropfrom, Point dropto, byte mod) {
        return dropItemFromMapObjectInternal(amount, itemoid, dropperoid, ownerid, dropfrom, dropto, mod, true);
    }

    /**
     * Gets a packet telling the client to show an item coming out of a map
     * object.
     *
     * @param itemid The ID of the dropped item.
     * @param itemoid The ObjectID of the dropped item.
     * @param dropperoid The OID of the dropper.
     * @param ownerid The ID of the drop owner.
     * @param dropfrom Where to drop from.
     * @param dropto Where the drop lands.
     * @param mod ?
     * @return The drop mesos packet.
     */
    public static MaplePacket dropItemFromMapObject(int itemid, int itemoid, int dropperoid, int ownerid, Point dropfrom, Point dropto, byte mod) {
        return dropItemFromMapObjectInternal(itemid, itemoid, dropperoid, ownerid, dropfrom, dropto, mod, false);
    }

    /**
     * Internal function to get a packet to tell the client to drop an item onto
     * the map.
     *
     * @param itemid The ID of the item to drop.
     * @param itemoid The ObjectID of the dropped item.
     * @param dropperoid The OID of the dropper.
     * @param ownerid The ID of the drop owner.
     * @param dropfrom Where to drop from.
     * @param dropto Where the drop lands.
     * @param mod ?
     * @param mesos Is the drop mesos?
     * @return The item drop packet.
     */
    public static MaplePacket dropItemFromMapObjectInternal(int itemid, int itemoid, int dropperoid, int ownerid, Point dropfrom, Point dropto, byte mod, boolean mesos) {

        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        //44 01 [01] [0F 00 00 00] [00] [8C 4A 0F 00] [E5 B3 29 00] [00] [DC 00] [A2 00] [00 00 00 00] [DC 00] [A2 00] [00] [00] [00] [80] [05 BB 46 E6 17 02 00 00]
        /*
         * 44 01 01 BB 06 00 00 00 90 14 25 00 27 52 00 00 00 6E 09 D3 01 00 00
         * 00 00 6E 09 D3 01 00 00 00 80 05 BB 46 E6 17 02 00 00 //time
         */
        mplew.writeShort(SendPacketOpcode.DROP_ITEM_FROM_MAPOBJECT.getValue());
        mplew.write(mod);//MOB
        mplew.writeInt(itemoid);
        mplew.write(mesos ? 1 : 0); // 1 = 金币, 0 = 物品
        mplew.writeInt(itemid);//物品ID
        mplew.writeInt(ownerid); // owner charid
        mplew.write(0);
        mplew.writeShort(dropto.x);
        mplew.writeShort(dropto.y);
        if (mod != 2) {
            mplew.writeInt(0);
            mplew.writeShort(dropfrom.x);
            mplew.writeShort(dropfrom.y);
        } else {
            mplew.writeInt(dropperoid);
        }
        mplew.writeZero(3);
        if (!mesos) {
            mplew.writeLong(DateUtil.getFileTimestamp(System.currentTimeMillis()));
        }
        if (mod != 2) {
            mplew.write(1); //fuck knows
            mplew.write(0); //PET Meso pickup
        }
        mplew.writeZero(3);

        return mplew.getPacket();
    }

    /*
     * (non-javadoc)  make MapleCharacter a mapobject, remove the need for
     * passing oid here.
     */
    /**
     * Gets a packet spawning a player as a mapobject to other clients.
     *
     * @param chr The character to spawn to other clients.
     * @return The spawn player packet.
     */
    private static final Random RAND = new Random();

    public static MaplePacket spawnPlayerMapobject(final MapleCharacter chr) {

        //Exception ex = new Exception();
        //log.info("刷新玩家：", ex);

        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.SPAWN_PLAYER.getValue());
        mplew.writeInt(chr.getId());
        mplew.write(chr.getLevel());
        mplew.writeMapleAsciiString(chr.getName());
        mplew.writeShort(0);//好像是关于结婚的。
        String 飞升 = chr.getFSString();
        String 转身 = "";
        String 会员 = "";
        //String 结婚 = "";
        //String 名字 = "永恒";

        //加载飞升
        if (chr.getReborns() >= 0) {
            转身 = "[" + chr.getReborns() + "]轉";

            //加载会员 
        }
        if (chr.getVip() == 1) {
            会员 = "[章魚童子]";
        } else if (chr.getVip() == 2) {
            会员 = "[章魚触须]";
        } else if (chr.getVip() == 3) {
            会员 = "[章魚墨汁]";
        } else if (chr.getVip() == 4) {
            会员 = "[章魚脑袋]";
        } else if (chr.getVip() == 5) {
            会员 = "[章魚心脏]";
        } else if (chr.getVip() == 6) {
            会员 = "[章魚大使]";
        }
        /*
         * //加载结婚 } if (chr.getjh() == 0) { 结婚 = "[未婚]"; } else if (chr.getjh()
         * == 1) { 结婚 = "[已婚]"; }
         */

        MapleGuildSummary gs = (chr.getGuildid() <= 0) ? null : chr.getClient().getChannelServer().getGuildSummary(chr.getGuildid());

        String format = "[No.:%d]%s%s%s%s";
        format = String.format(format, chr.getId(), 会员, gs != null ? gs.getName() : "暂无家族", 转身, 飞升);
        if (gs != null) {
            mplew.writeMapleAsciiString(format);
            mplew.writeShort(gs.getLogoBG());
            mplew.write(gs.getLogoBGColor());
            mplew.writeShort(gs.getLogo());
            mplew.write(gs.getLogoColor());
            mplew.writeZero(2);
        } else {

            mplew.writeMapleAsciiString(format);
            mplew.writeZero(8);
        }


        mplew.write(0);
        mplew.write(0);
        mplew.write(-1);//093新增


        AddPlayerStats(chr, mplew);

        mplew.writeZero(30);


        int CHAR_MAGIC_SPAWN = RAND.nextInt();//这里也要4个字节

        mplew.writeInt(CHAR_MAGIC_SPAWN); //1
        mplew.writeZero(11);

        mplew.writeInt(CHAR_MAGIC_SPAWN); //2
        mplew.writeZero(11);

        mplew.writeInt(CHAR_MAGIC_SPAWN); //3
        mplew.writeZero(3);
        mplew.writeInt((chr.getUsingMount() != null) ? chr.getUsingMount().getUsingItemId() : 0);
        mplew.writeInt((chr.getUsingMount() != null) ? chr.getUsingMount().getUsingSkillId() : 0);

        mplew.writeInt(CHAR_MAGIC_SPAWN); //4
        mplew.writeZero(9);

        mplew.writeInt(CHAR_MAGIC_SPAWN);//5

        /*
         * mplew.write(0); //093修改 mplew.write(chr.isMarried() ? 1 : 0); //093修改
         * mplew.writeInt(0);
         */
        mplew.writeZero(16);

        mplew.writeInt(CHAR_MAGIC_SPAWN);//6
        mplew.writeZero(17);

        mplew.writeInt(CHAR_MAGIC_SPAWN);//7
        mplew.writeZero(11);

        mplew.writeInt(CHAR_MAGIC_SPAWN);//8
        mplew.write(0);
        mplew.writeShort((short) chr.getJob().getId());
        mplew.writeShort(0);
        addCharLook(mplew, chr, true);
        mplew.writeZero(20);//092: 12  093: 20 096：24
        mplew.writeInt(Math.min(chr.getInventory(MapleInventoryType.CASH).countById(5110000), 100));
        mplew.writeInt(chr.getItemEffect());
        mplew.writeLong(0);
        mplew.writeLong(0); //097
        mplew.writeInt(0);//098
        mplew.writeLong(0); //097
        mplew.writeInt(-1);
        mplew.write(0);
        mplew.writeInt(chr.getChair());
        mplew.writeInt(0);//?
        mplew.writeShort(chr.getPosition().x);
        mplew.writeShort(chr.getPosition().y);
        mplew.write(chr.getStance());
        mplew.writeShort(chr.getFh());
        mplew.write(0);
        mplew.writeInt(chr.getMount() != null ? chr.getMount().getLevel() : 1);
        mplew.writeInt(chr.getMount() != null ? chr.getMount().getExp() : 0);
        mplew.writeInt(chr.getMount() != null ? chr.getMount().getTiredness() : 0);
        mplew.write(0);
        mplew.write(chr.getChalkboard() != null ? 1 : 0);
        if (chr.getChalkboard() != null) {
            mplew.writeMapleAsciiString(chr.getChalkboard()); //有黑板的时候这里占2个 00
        }
        addRings(mplew, chr, false);//戒指效果函数
        mplew.writeInt(0);//107?
        mplew.writeZero(11);//?新职业没有就38？
        return mplew.getPacket();
    }

    private static void addKuangLongStats(final MapleCharacter chr, MaplePacketLittleEndianWriter mplew) {
        boolean _hasSkill = chr.getBuffManager().hasBuff(61120007);
        if (_hasSkill) {
            mplew.write();
            mplew.writeShort(chr.getBuffManager().getBuffedValue(MapleBuffStat.KUANGLONG_JIANBI));
            mplew.writeInt(chr.getBuffManager().getBuffSource(MapleBuffStat.KUANGLONG_JIANBI));
            mplew.writeZero(3);
            mplew.writeInt(2);
            mplew.writeInt(5);
            mplew.writeInt(chr.getEquipPed(-11));
            mplew.writeInt(5);
        }
    }

    private static void AddPlayerStats(final MapleCharacter chr, MaplePacketLittleEndianWriter mplew) {
        List<MapleBuffStat> buffStats = MapleBuffStat.getSpawnList(chr);
        for (int i = GameConstants.MAX_PLAYER_STATUS; i > 0; i--) {
            int value = 0;

            if (i == 4) {
                value += 4096;
            } else if (i == 7) {
                value += 163840;
            }

            for (MapleBuffStat buffstat : buffStats) {
                if (buffstat.getPosition() == i) {
                    value += (buffstat.getValue(true, true));
                }
            }
            mplew.writeInt(value);
        }

        for (int i = 0; i < GameConstants.MAX_PLAYER_STATUS; i++) {
            for (MapleBuffStat buffstat : buffStats) {
                if (buffstat.getPosition() == i) {
                    buffstat.getSerializeSpawn().Serialize(mplew, chr);
                }
            }
        }
        mplew.writeInt(-1);
    }

    //适用于 updatecharlook spawnPlayerMapobject
    private static void addRings(MaplePacketLittleEndianWriter mplew, MapleCharacter chr, boolean isUpdateCharlook) {
        MapleInventory iv = chr.getInventory(MapleInventoryType.EQUIPPED);
        Collection<IItem> equippedC = iv.list();
        boolean 结婚戒指 = false;
        List<IEquip> 恋人戒指 = new ArrayList<IEquip>();
        List<IEquip> 友谊戒指 = new ArrayList<IEquip>();
        for (IItem item : equippedC) {
            if (item.友谊戒指()) {
                友谊戒指.add((IEquip) item);
            } else if (item.恋人戒指()) {
                恋人戒指.add((IEquip) item);
            } else if (item.结婚戒指()) {
                结婚戒指 = true;
            }
        }
        mplew.write(恋人戒指.size() > 0);
        if (恋人戒指.size() > 0) {
            mplew.writeInt(恋人戒指.size());
            for (IEquip ring : 恋人戒指) {
                mplew.writeLong(ring.getUniqueid()); //戒指的uniqueid
                mplew.writeLong(ring.getPartnerUniqueId()); //对方戒指的uniqueid 对应Ring表里的partnerRingId
                mplew.writeInt(ring.getItemId());
            }
        }
        mplew.write(友谊戒指.size() > 0);
        if (友谊戒指.size() > 0) {
            mplew.writeInt(友谊戒指.size());
            for (IEquip ring : 友谊戒指) {
                mplew.writeLong(ring.getUniqueid()); //戒指的uniqueid
                mplew.writeLong(ring.getPartnerUniqueId()); //对方戒指的uniqueid 对应Ring表里的partnerRingId
                mplew.writeInt(ring.getItemId());
            }
        }
        boolean 带结婚戒指 = 结婚戒指;
        mplew.write(带结婚戒指 ? 1 : 0);//带结婚戒指 是1  否0
        if (带结婚戒指) {
            mplew.writeInt(chr.getId());//自己的cid
            mplew.writeInt(chr.getPartnerid());//配偶的cid
            mplew.writeInt(1112300);//1112300	月长石戒指1克拉	
        }
        mplew.writeInt(0);
        if (!isUpdateCharlook) {
            mplew.writeShort(0);
        }
    }

    //人物表情
    public static MaplePacket facialExpression(MapleCharacter from, int expression) {

        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.FACIAL_EXPRESSION.getValue());
        mplew.writeInt(from.getId());
        mplew.writeInt(expression);
        mplew.writeInt(-1);
        mplew.write(0);
        return mplew.getPacket();
    }

    private static void serializeMovementList(LittleEndianWriter lew, List<LifeMovementFragment> moves) {
        lew.writeInt(0);//094
        lew.write(moves.size()); //移动的次数
        for (LifeMovementFragment move : moves) {
            move.serialize(lew);
        }
    }

    public static MaplePacket movePlayer(int cid, List<LifeMovementFragment> moves) {

        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.MOVE_PLAYER.getValue());
        mplew.writeInt(cid);
        mplew.writeInt(0);
        serializeMovementList(mplew, moves);

        return mplew.getPacket();
    }

    public static MaplePacket moveSummon(int cid, int oid, Point startPos, List<LifeMovementFragment> moves) {

        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.MOVE_SUMMON.getValue());
        mplew.writeInt(cid);
        mplew.writeInt(oid);
        mplew.writeShort(startPos.x);
        mplew.writeShort(startPos.y);
        serializeMovementList(mplew, moves);

        return mplew.getPacket();
    }

    public static MaplePacket moveDragon(int cid, Point startPos, List<LifeMovementFragment> moves) {

        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        /*
         * E4 00 1F 0C 74 00 31 00 91 05 00 00 00 00 01 00 EE FF 91 05 FF FF 00
         * 00 00 00 00 00 00 00 03 FE 01
         */
        mplew.writeShort(SendPacketOpcode.DRAGON_MOVE.getValue());
        mplew.writeInt(cid);
        mplew.writeShort(startPos.x);
        mplew.writeShort(startPos.y);
        serializeMovementList(mplew, moves);
        return mplew.getPacket();
    }

    public static MaplePacket moveMonster(boolean useskill, int skill, int skill_1, int skill_2, int skill_3, int skill_4, int oid, Point startPos, Point nextPos, List<LifeMovementFragment> moves) {

        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.MOVE_MONSTER.getValue());
        mplew.writeInt(oid);
        mplew.write(useskill); // 0
        mplew.write(skill); // -1
        mplew.write(skill_1); // 0
        mplew.write(skill_2); // 0
        mplew.write(skill_3); // 0
        mplew.write(skill_4); // 0
        mplew.writeShort(0);
        mplew.writePos(startPos);
        //mplew.writePos(nextPos);
        mplew.writeInt(0);
        serializeMovementList2(mplew, moves);
        return mplew.getPacket();
    }

    private static void serializeMovementList2(LittleEndianWriter lew, List<LifeMovementFragment> moves) {
        lew.write(moves.size()); //移动的次数
        for (LifeMovementFragment move : moves) {
            move.serialize(lew);
        }
    }

    public static MaplePacket summonAttack(MapleCharacter chr, int summonOid, int newStance, List<SummonAttackEntry> allDamage, int numAttackMonsturAndCount) {

        //45 01 [21 DF 49 00] [C2 7C 05 00] A6 84 01 43 19 A3 00 07 0A 3C 00 00 00
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.SUMMON_ATTACK.getValue());
        mplew.writeInt(chr.getId());
        mplew.writeInt(summonOid);
        mplew.write(chr.getLevel()); //093新增
        mplew.write(newStance);
        mplew.write(numAttackMonsturAndCount);
        for (SummonAttackEntry attackEntry : allDamage) {
            mplew.writeInt(attackEntry.getMonsterOid()); // oid
            //mplew.write(1); //093去除
            mplew.write(7); //093修改
            mplew.writeInt(attackEntry.getDamage()); // damage
        }
        mplew.write(0); //093新增
        return mplew.getPacket();
    }

    //召唤兽打怪
    public static MaplePacket damageSummon(int cid, int summonOid, int damage, int monsterIdFrom, int unkByte) {

        // 77 00 [29 1D 02 00] [FA FE 30 00] 00 [10 00 00 00] [BF 70 8F 00] 00
        // 4A 01 [7A 1F 47 00[ [C8 0C 01 00] 00 [69 01 00 00] [0C 7C 92 00] 01
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.DAMAGE_SUMMON.getValue());
        mplew.writeInt(cid);
        mplew.writeInt(summonOid);
        mplew.write(0);
        mplew.writeInt(damage);
        mplew.writeInt(monsterIdFrom);
        mplew.write(unkByte);
        return mplew.getPacket();
    }

    public static MaplePacket Combo_Effect(int combo) {

        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.COMBO_EFFECE.getValue());
        mplew.writeInt(combo);

        return mplew.getPacket();
    }

    /*
     * //近距离攻击 public static MaplePacket closeRangeAttack(int cid, int skill,
     * int stance, int numAttackedAndDamage, List<Pair<Integer, List<Integer>>>
     * damage, int speed, int pos, int skilllevel) {
     *
     * MaplePacketLittleEndianWriter mplew = new
     * MaplePacketLittleEndianWriter();
     * mplew.writeShort(SendPacketOpcode.CLOSE_RANGE_ATTACK.getValue()); if
     * (skill == 4211006) { // 金钱炸弹 addMesoExplosion(mplew, cid, skill, stance,
     * numAttackedAndDamage, 0, damage, speed, pos); } else {
     * addAttackBody(mplew, cid, skill, stance, numAttackedAndDamage, 0, damage,
     * speed, pos, skilllevel); } return mplew.getPacket(); }
     */
    public static MaplePacket closeRangeAttack(DamageParseHandler.AttackInfo attackInfo, MapleCharacter chr) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.CLOSE_RANGE_ATTACK.getValue());
        if (attackInfo.skill == 4211006) { // 金钱炸弹
            addMesoExplosion(mplew, chr.getId(), attackInfo.skill, attackInfo.stance, attackInfo.numAttackedAndDamage, 0, attackInfo.allDamage, attackInfo.speed, attackInfo.pos);
        } else {
            addAttackBody(mplew, chr, attackInfo, 0);
        }
        return mplew.getPacket();
    }
    /*
     * //远程攻击 public static MaplePacket rangedAttack(int cid, int skill, int
     * stance, int numAttackedAndDamage, int projectile, List<Pair<Integer,
     * List<Integer>>> damage, int speed, Short x, Short y, int pos, int
     * skilllevel, int charge) {
     *
     * // 7E 00 30 75 00 00 01 00 97 04 0A CB 72 1F 00 // D3 00 [57 4B 02 00 01
     * 00 00 00 1F 80 06 00 E0 6E 1F 00 00 00 00 00
     * MaplePacketLittleEndianWriter mplew = new
     * MaplePacketLittleEndianWriter();
     * mplew.writeShort(SendPacketOpcode.RANGED_ATTACK.getValue());
     * addAttackBody(mplew, cid, skill, stance, numAttackedAndDamage,
     * projectile, damage, speed, pos, skilllevel); mplew.writeInt(charge);
     * return mplew.getPacket(); }
     */

    public static MaplePacket rangedAttack(DamageParseHandler.AttackInfo attackInfo, MapleCharacter chr, int p) {

        // 7E 00 30 75 00 00 01 00 97 04 0A CB 72 1F 00
        // D3 00 [57 4B 02 00 01 00 00 00 1F 80 06 00 E0 6E 1F 00 00 00 00 00
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.RANGED_ATTACK.getValue());
        addAttackBody(mplew, chr, attackInfo, p);
        mplew.writeInt(attackInfo.charge);
        return mplew.getPacket();
    }

    /*
     * //魔法师攻击 public static MaplePacket magicAttack(int cid, int skill, int
     * stance, int numAttackedAndDamage, List<Pair<Integer, List<Integer>>>
     * damage, int charge, int speed, int pos, int skilllevel) {
     *
     * MaplePacketLittleEndianWriter mplew = new
     * MaplePacketLittleEndianWriter();
     *
     * mplew.writeShort(SendPacketOpcode.MAGIC_ATTACK.getValue());
     * addAttackBody(mplew, atta); // if (charge != -1) {
     * mplew.writeInt(charge); // }
     *
     * return mplew.getPacket(); }
     */
    public static MaplePacket magicAttack(DamageParseHandler.AttackInfo attackInfo, MapleCharacter chr) {

        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.MAGIC_ATTACK.getValue());
        addAttackBody(mplew, chr, attackInfo, 0);
        //   if (charge != -1) {
        if (SkillFactory.getSkill(attackInfo.skill).hasCharge()) {
            mplew.writeInt(attackInfo.charge);
        }
        //   }

        return mplew.getPacket();
    }

    /*
     * //攻击的母方法 private static void addAttackBody(LittleEndianWriter lew, int
     * cid, int skill, int stance, int numAttackedAndDamage, int projectile,
     * List<Pair<Integer, List<Integer>>> damage, int speed, int pos, int
     * skilllevel) { lew.writeInt(cid); lew.write(numAttackedAndDamage);
     * lew.write(0x04);//0x04//角色等级 if (skill > 0) { lew.write(0xff); // too low
     * and some skills don't work (?) lew.writeInt(skill); } else {
     * lew.write(0); } lew.write(0);//连击数 lew.write(pos); lew.write(stance);
     * lew.write(speed); lew.write(0); lew.writeInt(projectile);
     *
     * for (Pair<Integer, List<Integer>> oned : damage) { if (oned.getRight() !=
     * null) { lew.writeInt(oned.getLeft().intValue()); lew.write(0x07);//091
     * for (Integer eachd : oned.getRight()) { lew.writeInt(skill == 3221007 ?
     * eachd.intValue() + 0x80000000 : eachd.intValue()); } } } }
     */
    private static void addAttackBody(LittleEndianWriter lew, MapleCharacter chr, DamageParseHandler.AttackInfo attackInfo, int projectile) {
        lew.writeInt(chr.getId());
        lew.write(attackInfo.numAttackedAndDamage);
        lew.write(chr.getLevel());//0x04//角色等级
        if (attackInfo.skill > 0) {
            int level = chr.getSkillLevel(attackInfo.skill);
            //level = level >= attackInfo.skill ? level : attackInfo.skill;
            level = Math.max(1, level);
            lew.write(level); // too low and some skills don't work (?)
            lew.writeInt(attackInfo.skill);
        } else {
            lew.write(0);
        }
        if (attackInfo.mode.equals(DamageParseHandler.AttackMode.RANGE) && attackInfo.pos != 0x1b) {
            lew.write(0);
        }
        lew.write(0);//109
        lew.write(attackInfo.aranCombo);//连击数
        lew.write(attackInfo.pos);
        lew.write(attackInfo.stance);
        lew.write(attackInfo.speed);
        lew.write(attackInfo.unk1);

        lew.writeInt(projectile);

        for (Pair<Integer, List<Integer>> oned : attackInfo.allDamage) {
            if (oned.getRight() != null) {
                lew.writeInt(oned.getLeft().intValue());
                lew.writeShort(7);//091
                for (Integer eachd : oned.getRight()) {
                    lew.writeInt(attackInfo.skill == 3221007 ? eachd.intValue() + 0x80000000 : eachd.intValue());
                }
            }
        }
        if (attackInfo.mode.equals(DamageParseHandler.AttackMode.RANGE)) {
            lew.writeInt(0);
        }
        //  System.out.println(lew.toString());
    }

    private static void addMesoExplosion(LittleEndianWriter lew, int cid, int skill, int stance, int numAttackedAndDamage, int projectile, List<Pair<Integer, List<Integer>>> damage, int speed, int pos) {

        // BC 00 90 E5 2F 00 00 5A 1A 3E 41 40 00 00 3F 00 03 0A 00 00 00 00 //078
        lew.writeInt(cid);
        lew.write(numAttackedAndDamage);
        lew.write(0x5A);
        lew.write(0x1A);
        lew.writeInt(skill);
        lew.write(0);
        lew.write(pos);
        lew.write(stance);
        lew.write(speed);
        lew.write(0x0A);
        lew.writeInt(projectile);

        for (Pair<Integer, List<Integer>> oned : damage) {
            if (oned.getRight() != null) {
                lew.writeInt(oned.getLeft().intValue());
                lew.write(0xFF);
                lew.write(oned.getRight().size());
                for (Integer eachd : oned.getRight()) {
                    lew.writeInt(eachd.intValue());
                }
            }
        }

    }

    //NPC商店
    public static MaplePacket getNPCShop(MapleClient c, int sid, List<MapleShopItem> items) {

        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.OPEN_NPC_SHOP.getValue());
        mplew.writeInt(0);
        mplew.writeInt(sid); //商店ID
        mplew.write(0);//108
        mplew.writeInt(sysTime);
        mplew.write(0);//096
        mplew.writeShort(items.size()); // 物品数量
        mplew.writeZero(8);
        for (MapleShopItem item : items) {
            mplew.writeInt(item.getItemId());//物品ID
            mplew.writeInt(item.getPrice());//物品价格
            mplew.writeInt(0);//如果不是扣除Meso 而是扣除item获得物品的话这里就填扣除的itemid
            mplew.writeInt(0);//要扣除的item数量
            mplew.writeLong(0);
            mplew.write(0);
            mplew.writeInt(0);//97
            mplew.writeInt(0);//98
            mplew.writeInt(0);//98
            mplew.writeLong(DateUtil.getFileTimestamp(FINAL_TIME));
            mplew.writeLong(DateUtil.getFileTimestamp(FINAL_LOCKITEMTIME));
            mplew.writeInt(0);
            mplew.writeMapleAsciiString("1900010100");//有效期？
            mplew.writeMapleAsciiString("2079010100");
            if (!ii.isThrowingStar(item.getItemId()) && !ii.isBullet(item.getItemId())) { //非海星标和子弹
                mplew.writeZero(5);
                mplew.writeShort(1); // 物品个数
                mplew.writeShort(item.getBuyable()); //道具是可买的 价格？
                mplew.write(0);//97
            } else {
                mplew.writeZero(11);
                // o.O getPrice有时返回unitPrice价格不是这个
                mplew.writeShort(BitTools.doubleToShortBits(ii.getPrice(item.getItemId())));
                mplew.writeShort(ii.getSlotMax(c, item.getItemId())); //最大数量
                mplew.write();
            }
            mplew.writeZero(56);
        }
        // mplew.write(0);//97
        return mplew.getPacket();
    }

    /**
     * code (8 = sell, 0 = buy, 0x20 = due to an error the trade did not happen
     * o.o) 确认商店交易
     *
     * @param code
     * @return
     */
    public static MaplePacket confirmShopTransaction(byte code) {

        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.CONFIRM_SHOP_TRANSACTION.getValue());
        // mplew.writeShort(0xE6); // 47 E4
        mplew.write(code); // recharge == 8?
        mplew.write(0);//97
        mplew.write();//107

        return mplew.getPacket();
    }

    /*
     * 19 reference 00 01 00 = new while adding 01 01 00 = add from drop 00 01
     * 01 = update count 00 01 03 = clear slot 01 01 02 = move to empty slot 01
     * 02 03 = move and merge 01 02 01 = move and merge with rest 增加物品到背包
     */
    public static MaplePacket addInventorySlot(MapleInventoryType type, IItem item) {
        return addInventorySlot(type, item, false);
    }

    //增加物品到背包
    public static MaplePacket addInventorySlot(MapleInventoryType type, IItem item, boolean fromDrop) {

        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.MODIFY_INVENTORY_ITEM.getValue());
        if (fromDrop) {
            mplew.write(1);
        } else {
            mplew.write(0);
        }
        mplew.write(HexTool.getByteArrayFromHexString("01 00 00")); // add mode
        mplew.write(type.getType()); // iv type
        mplew.write(item.getPosition()); // slot id
        addItemInfo(mplew, item, true, false, true);

        return mplew.getPacket();
    }

    //更新背包位置
    public static MaplePacket updateInventorySlot(MapleInventoryType type, IItem item) {
        return updateInventorySlot(type, item, false);
    }

    public static MaplePacket updateInventorySlot(MapleInventoryType type, IItem item, boolean fromDrop) {

        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.MODIFY_INVENTORY_ITEM.getValue());
        if (fromDrop) {
            mplew.write(1);
        } else {
            mplew.write(0);
        }
        mplew.write(HexTool.getByteArrayFromHexString("01 00 01")); // update
        // mode
        mplew.write(type.getType()); // iv type
        mplew.writeShort(item.getPosition()); // slot id
        mplew.writeShort(item.getQuantity());

        return mplew.getPacket();
    }

    //移动背包物品
    public static MaplePacket moveInventoryItem(MapleInventoryType type, short src, short dst) {
        return moveInventoryItem(type, src, dst, (byte) -1);
    }

    public static MaplePacket moveInventoryItem(MapleInventoryType type, short src, short dst, byte equipIndicator) {

        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.MODIFY_INVENTORY_ITEM.getValue());
        mplew.write(HexTool.getByteArrayFromHexString("01 01 00 02"));
        mplew.write(type.getType());
        mplew.writeShort(src);
        mplew.writeShort(dst);
        if (equipIndicator != -1) {
            mplew.write(equipIndicator);
        }
        ////log.debug("移动装备包："+mplew.getPacket());
        return mplew.getPacket();
    }

    //整理物品(暂时没这功能)
    public static MaplePacket moveAndMergeInventoryItem(MapleInventoryType type, short src, short dst, short total) {

        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.MODIFY_INVENTORY_ITEM.getValue());
        mplew.write(HexTool.getByteArrayFromHexString("01 02 00 03"));
        mplew.write(type.getType());
        mplew.writeShort(src);
        mplew.write(1); // merge mode?
        mplew.write(type.getType());
        mplew.writeShort(dst);
        mplew.writeShort(total);

        return mplew.getPacket();
    }

    //集合物品(暂时没这功能)
    public static MaplePacket moveAndMergeWithRestInventoryItem(MapleInventoryType type, short src, short dst, short srcQ, short dstQ) {

        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.MODIFY_INVENTORY_ITEM.getValue());
        mplew.write(HexTool.getByteArrayFromHexString("01 02 00 01"));
        mplew.write(type.getType());
        mplew.writeShort(src);
        mplew.writeShort(srcQ);
        mplew.write(HexTool.getByteArrayFromHexString("01"));
        mplew.write(type.getType());
        mplew.writeShort(dst);
        mplew.writeShort(dstQ);

        return mplew.getPacket();
    }

    //清除背包物品
    public static MaplePacket clearInventoryItem(MapleInventoryType type, short slot, boolean fromDrop) {

        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.MODIFY_INVENTORY_ITEM.getValue());
        mplew.write(fromDrop ? 1 : 0);
        mplew.write(HexTool.getByteArrayFromHexString("01 00 03"));
        mplew.write(type.getType());
        mplew.writeShort(slot);

        return mplew.getPacket();
    }

    public static MaplePacket InventoryActions(boolean enable, List<InventoryActionsInfo> actions) {

        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.MODIFY_INVENTORY_ITEM.getValue());
        mplew.write(enable);
        mplew.writeShort(actions.size());
        for (InventoryActionsInfo triple : actions) {
            mplew.write(triple.action.getValue());
            mplew.write(triple.type.getType()); // iv type
            switch (triple.action) {
                case ADD:
                    mplew.write(triple.item.getPosition()); // slot id
                    addItemInfo(mplew, triple.item, true, false, true);
                    break;
                case UPDATEQUANTITY:
                    mplew.writeShort(triple.item.getPosition()); // slot id
                    mplew.writeShort(triple.item.getQuantity());
                    break;
                case UPDATEPOSITION:
                    mplew.writeShort(triple.src);
                    mplew.writeShort(triple.dst);
                    break;
                case DELETE:
                    mplew.writeShort(triple.item.getPosition());
                    break;
            }
        }
        return mplew.getPacket();
    }

    public static MaplePacket scrolledItem(IItem scroll, IItem item, boolean destroyed) {

        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.MODIFY_INVENTORY_ITEM.getValue());
        mplew.write(1); // fromdrop always true
        mplew.write(destroyed ? 2 : 3);
        mplew.write(0);//?
        mplew.write(scroll.getQuantity() > 0 ? 1 : 3);
        mplew.write(MapleInventoryType.USE.getType());
        mplew.writeShort(scroll.getPosition());
        if (scroll.getQuantity() > 0) {
            mplew.writeShort(scroll.getQuantity());
        }
        mplew.write(3);
        if (!destroyed) { //如果没有被破坏
            mplew.write(MapleInventoryType.EQUIP.getType());
            mplew.writeShort(item.getPosition());
            mplew.write(0);
        }
        mplew.write(MapleInventoryType.EQUIP.getType());
        mplew.writeShort(item.getPosition());
        if (!destroyed) {
            addItemInfo(mplew, item, true);
        }
        mplew.write(1);
        return mplew.getPacket();
    }

    public static MaplePacket getScrollEffect(int chr, ScrollResult scrollSuccess, boolean legendarySpirit, int itemid, int equipid) {

        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.SHOW_SCROLL_EFFECT.getValue());
        mplew.writeInt(chr);
        switch (scrollSuccess) {
            case SUCCESS:
                mplew.writeShort(1);
                mplew.writeInt(itemid);
                mplew.writeInt(equipid);
                mplew.write(legendarySpirit ? 1 : 0);
                break;
            case FAIL:
                mplew.writeShort(0);
                mplew.writeInt(itemid);
                mplew.writeInt(equipid);
                mplew.write(legendarySpirit ? 1 : 0);
                break;
            case CURSE:
                mplew.writeShort(2);
                mplew.writeInt(itemid);
                mplew.writeInt(equipid);
                mplew.write(legendarySpirit ? 1 : 0);
                break;
            default:
                throw new IllegalArgumentException("effect in illegal range");
        }
        return mplew.getPacket();
    }

    //移除玩家从地图
    public static MaplePacket removePlayerFromMap(int cid) {

        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.REMOVE_PLAYER_FROM_MAP.getValue());
        mplew.writeInt(cid);
        return mplew.getPacket();
    }

    /**
     * animation: 0 - expire<br/> 1 - without animation<br/> 2 - pickup<br/> 4 -
     * explode<br/> cid is ignored for 0 and 1
     *
     * @param oid
     * @param animation
     * @param cid
     * @return
     */
    //移除道具从地图
    public static MaplePacket removeItemFromMap(int oid, int animation, int cid) {
        return removeItemFromMap(oid, animation, cid, false, 0);
    }

    /**
     * animation: 0 - expire<br/> 1 - without animation<br/> 2 - pickup<br/> 4 -
     * explode<br/> cid is ignored for 0 and 1.<br /><br />Flagging pet as true
     * will make a pet pick up the item.
     *
     * @param oid
     * @param animation
     * @param cid
     * @param pet
     * @param slot
     * @return
     */
    public static MaplePacket removeItemFromMap(int oid, int animation, int cid, boolean pet, int slot) {

        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        //79 01 [05] [A5 3D 00 00] [47 D1 08 00] [00 00 00 00]
        mplew.writeShort(SendPacketOpcode.REMOVE_ITEM_FROM_MAP.getValue());
        mplew.write(animation); //0 - 渐隐 1 - 立刻消失 4 - 金钱炸效果 5 - 宠物捡起来
        mplew.writeInt(oid);
        if (animation >= 2) {
            mplew.writeInt(cid);
            if (pet) {
                mplew.writeInt(slot); //093修改 以前是byte
            }
        }

        return mplew.getPacket();
    }

    public static MaplePacket updateCharLook(MapleCharacter chr) {

        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.UPDATE_CHAR_LOOK.getValue());
        mplew.writeInt(chr.getId());
        mplew.write(1);
        addCharLook(mplew, chr, false);
        addRings(mplew, chr, true);//戒指效果函数
        return mplew.getPacket();
    }

    //显示龙龙的SP
    public static MaplePacket changeJob(byte mode) {

        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.CHANGE_JOB.getValue());
        mplew.write(mode);
        mplew.write(28);
        return mplew.getPacket();
    }

    //召唤龙龙
    public static MaplePacket spawnDragon(MapleCharacter chr) {

        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        //E3 00 [1F 0C 74 00] [7C 00 00 00] [91 05 00 00] [04] [00 00] [A7 08]
        mplew.writeShort(SendPacketOpcode.DRAGON_SPAWN.getValue());
        mplew.writeInt(chr.getId());
        mplew.writeInt(chr.getPosition().x);
        mplew.writeInt(chr.getPosition().y);
        mplew.writeShort(0);
        mplew.write(0);
        mplew.writeShort((short) chr.getJob().getId());
        return mplew.getPacket();
    }

    public static MaplePacket 龙神信息(String path) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.SHOW_STATUS_INFO.getValue());
        mplew.write(11);
        mplew.writeMapleAsciiString(path);
        return mplew.getPacket();
    }

    public static MaplePacket evanTutorial(String path, int cid) {

        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.NPC_TALK.getValue());
        mplew.write(4);
        mplew.writeInt(cid);
        mplew.write(1);
        mplew.write(1);
        mplew.write(1);
        mplew.writeMapleAsciiString(path);
        return mplew.getPacket();
    }

    public static MaplePacket evanTutorial(String path) {

        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.NPC_TALK.getValue());
        mplew.write(4);
        mplew.writeInt(0);
        mplew.write(1);
        mplew.write(1);
        mplew.write(1);
        mplew.writeMapleAsciiString(path);
        return mplew.getPacket();
    }

    public static MaplePacket updateSp(int Sp) {

        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.UPDATE_SKILLS.getValue());
        mplew.writeShort(0);
        mplew.write(0);
        mplew.write(Sp);
        return mplew.getPacket();
    }

    public static MaplePacket getShowSpGain(MapleCharacter chr, int gain) {

        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.SHOW_STATUS_INFO.getValue());
        mplew.write(4);
        mplew.writeShort(chr.getJob().getId());
        mplew.write(gain);
        return mplew.getPacket();
    }

    //丢下道具
    public static MaplePacket dropInventoryItem(MapleInventoryType type, short src) {

        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.MODIFY_INVENTORY_ITEM.getValue());
        // mplew.writeShort(0x19);
        mplew.write(HexTool.getByteArrayFromHexString("01 01 00 03"));
        mplew.write(type.getType());
        mplew.writeShort(src);
        if (src < 0) {
            mplew.write(1);
        }
        return mplew.getPacket();
    }

    //更新掉落装备
    public static MaplePacket dropInventoryItemUpdate(MapleInventoryType type, IItem item) {

        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.MODIFY_INVENTORY_ITEM.getValue());
        mplew.write(HexTool.getByteArrayFromHexString("01 01 00 01"));
        mplew.write(type.getType());
        mplew.writeShort(item.getPosition());
        mplew.writeShort(item.getQuantity());

        return mplew.getPacket();
    }

    public static MaplePacket GiveDf(int oid, int give) {

        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.GIVE_DF_FROM_OBJECT.getValue());
        mplew.write(1);
        mplew.writeInt(oid);
        mplew.write(1);
        mplew.writeInt(Randomizer.getInstance().nextInt());
        mplew.writeInt(give);
        mplew.write(0);

        return mplew.getPacket();
    }

    public static MaplePacket damagePlayer(int skill, int monsteridfrom, int cid, int damage) {
        return damagePlayer(skill, monsteridfrom, cid, damage, 0, 0, false, 0, false, 0, 0, 0);
    }

    //玩家的伤害
    public static MaplePacket damagePlayer(int skill, int monsteridfrom, int cid, int damage, int fake, int direction, boolean pgmr, int pgmr_1, boolean is_pg, int oid, int pos_x, int pos_y) {
        // D9 00 [60 B5 6C 00] FF [01 00 00 00]      [05 87 01 00] 01 00 00 01 00 00 00
        // F5 00 [9F 1E 44 00] FF [30 04 00 00] [00] [17 7C 92 00] [00 00 00 00] [30 04 00 00]
        //01 01 [4B DF 22 00] FF 01 00 00 00 00 04 87 01 00 00 00 00 00 00 00 00 00 00 00 00 01 00 00 00
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.DAMAGE_PLAYER.getValue());
        mplew.writeInt(cid);
        mplew.write(skill);
        mplew.writeInt(damage);
        mplew.write(0); //093新增
        if ((skill != -4) && (skill != -3)) {
            mplew.writeInt(monsteridfrom);
            if (pgmr) {
                mplew.write(0);
                mplew.write(pgmr_1);
                mplew.write(0);
                mplew.write((is_pg) ? 1 : 0);
                mplew.writeInt(oid);
                mplew.write(7);
                mplew.writeShort(pos_x);
                mplew.writeShort(pos_y);
                mplew.write(0);
            } else {
                mplew.writeInt(direction);
            }
        }
        mplew.writeZero(7);//097
        mplew.writeInt(damage);
        if (fake > 0) {
            mplew.writeInt(fake);
        }
        return mplew.getPacket();
    }

    public static MaplePacket genericGuildMessage(byte code) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.GUILD_OPERATION.getValue());
        mplew.write(code);

        return mplew.getPacket();
    }
    //检查人物名反馈

    public static MaplePacket charNameResponse(String charname, boolean nameUsed) {

        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.CHAR_NAME_RESPONSE.getValue());
        mplew.writeMapleAsciiString(charname);
        mplew.write(nameUsed ? 1 : 0);

        return mplew.getPacket();
    }

    //增加新人物089这里没出错
    public static MaplePacket addNewCharEntry(MapleCharacter chr, boolean worked) {

        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.ADD_NEW_CHAR_ENTRY.getValue());
        mplew.write(worked ? 0 : 1);
        addCharStats(mplew, chr); //角色状态
        addCharLook(mplew, chr, false); //角色外观
        return mplew.getPacket();
    }

    /**
     * 开始任务
     */
    public static MaplePacket startQuest(MapleCharacter c, short quest) {

        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.SHOW_STATUS_INFO.getValue());
        mplew.write(1);
        mplew.writeShort(quest);
        mplew.writeShort(1);
        mplew.write(0);

        return mplew.getPacket();
    }

    //角色信息
    public static MaplePacket charInfo(MapleCharacter chr) {
        /*
         * 43 00 96 54 22 00 24 A4 01 0A 00 00 00 00 00 00 01 00 2D 00 00 FF
         *
         * 00
         *
         * 00
         *
         * 00
         *
         * 00
         *
         * 02 91 2F 31 01 96 2F 31 01
         *
         * 00 00 00 00 00 00 00 00 00 00 00 00
         *
         * 01 00 00 00 D0 ED 2D 00
         *
         * 00 00 00 00
         */

        /*
         * 3F 00 79 1F 47 00 //人物ID 7A //人物等级 B8 0D //职业ID 0A //pk等级 00 00 00 00
         * //人气度 00 //是否结婚 00 //093新增 01 00 2D //家族名 显示的内容是"-" 00 00 //联盟名 无内容
         * FF
         *
         * 00 //宠物结束 00 //无坐骑 00 //购物车size 00 //093 E5 6D 11 00 //正在佩戴的勋章的ID 04
         * 00 //总勋章数 F5 74 //获得勋章的任务 F6 74 //获得勋章的任务 F7 74 //获得勋章的任务 F8 74
         * //获得勋章的任务
         *
         * 00 00 00 00 00 00 //6个byte应该是对应倾向的图像
         *
         * 00 00 00 00 //椅子size 03 00 00 00 //勋章size E4 6D 11 00 //勋章列表的勋章itemid
         * E2 6D 11 00 //勋章列表的勋章itemid E3 6D 11 00 //勋章列表的勋章itemid
         */

        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.CHAR_INFO.getValue());
        mplew.writeInt(chr.getId());
        mplew.write(chr.getLevel());
        mplew.writeShort(chr.getJob().getId());
        mplew.writeShort(0);
        mplew.write(chr.getPkLevel());//093新增 Pk等级(1 - 10)
        mplew.writeInt(chr.getFame());//人气 从092的 Short --> Int
        mplew.write(0);
        mplew.write(0); //副职业个数
        String guildName = "-";
        String allianceName = "";
        MapleGuildSummary gs = chr.getClient().getChannelServer().getGuildSummary(chr.getGuildid());
        if (chr.getGuildid() > 0 && gs != null) {
            guildName = gs.getName();
            try {
                MapleAlliance alliance = chr.getClient().getChannelServer().getWorldInterface().getAlliance(gs.getAllianceId());
                if (alliance != null) {
                    allianceName = alliance.getName();
                }
            } catch (RemoteException e) {
                ServerExceptionHandler.HandlerRemoteException(e);
                chr.getClient().getChannelServer().reconnectWorld();
            }
        }
        mplew.writeMapleAsciiString(guildName);//家族 [01 00 2D]
        mplew.writeMapleAsciiString(allianceName);//联盟 [00 00]
        mplew.write(-1);//主宠物格子。
        //宠物

        mplew.write(0);
        List<MaplePet> pets = chr.getPetsList();
        mplew.write(pets.size() > 0 ? 1 : 0);
        for (MaplePet pet : pets) {
            mplew.write(1);
            mplew.writeInt(pet.getSlot());
            mplew.writeInt(pet.getItemId()); //宠物ID
            mplew.writeMapleAsciiString(pet.getName());//宠物名
            mplew.write(pet.getLevel()); //等级
            mplew.writeShort(pet.getCloseness()); //亲密度
            mplew.write(pet.getFullness()); //饥饿度
            mplew.write(HexTool.getByteArrayFromHexString("5F 00"));
            if (chr.getInventory(MapleInventoryType.EQUIPPED).getItem((byte) -114) != null) {
                mplew.writeInt(chr.getInventory(MapleInventoryType.EQUIPPED).getItem((byte) -114).getItemId());
            } else {
                mplew.writeInt(0);
            }
            mplew.writeInt(-1);
        }

        mplew.write(0);
        //坐骑
        if (chr.getMount() != null
                && chr.getInventory(MapleInventoryType.EQUIPPED).getItem((byte) -18) != null //是否有骑宠
                && chr.getInventory(MapleInventoryType.EQUIPPED).getItem((byte) -19) != null //是否有鞍子
                && chr.getInventory(MapleInventoryType.EQUIPPED).getItem((byte) -18).getItemId() == chr.getMount().getItemId()) {
            mplew.write(chr.getMount().getId()); //mount
            mplew.writeInt(chr.getMount().getLevel()); //level
            mplew.writeInt(chr.getMount().getExp()); //exp
            mplew.writeInt(chr.getMount().getTiredness()); //tiredness
        } else {
            mplew.write(0); //无坐骑
        }

        //购物车
        try {
            Connection con = DatabaseConnection.getConnection();
            PreparedStatement ps = con.prepareStatement("SELECT * FROM wishlist WHERE charid = ?");
            ps.setInt(1, chr.getId());
            ResultSet rs = ps.executeQuery();
            int i = 0;
            while (rs.next()) {
                i++;
            }
            mplew.write(i); //购物车size
            rs.close();
            ps.close();
            con.close();
        } catch (SQLException e) {
            log.info("Error getting wishlist data:", e);
        }

        try {
            Connection con = DatabaseConnection.getConnection();
            PreparedStatement ps = con.prepareStatement("SELECT * FROM wishlist WHERE charid = ? ORDER BY sn DESC");
            ps.setInt(1, chr.getId());
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                mplew.writeInt(rs.getInt("sn"));
            }
            rs.close();
            ps.close();
            con.close();
        } catch (SQLException e) {
            log.info("Error getting wishlist data:", e);
        }



        //mplew.writeInt(0); //正在佩戴的勋章itemid
        IItem 正在带的勋章 = chr.getInventory(MapleInventoryType.EQUIPPED).getItem((byte) -26);
        if (正在带的勋章 != null) {
            mplew.writeInt(正在带的勋章.getItemId());
        } else {
            mplew.writeInt(0);
        }

        mplew.writeShort(0);//size 共完成了多少个勋章任务
        //mplew.writeShort(x);//questid 对应上面的size 发送的是勋章任务的id 去QuestInf.img.xml里找
        mplew.write(0); //领袖气质
        mplew.write(0); //洞察力
        mplew.write(0); //意志
        mplew.write(0); //手技
        mplew.write(0); //感性
        mplew.write(0); //魅力
        mplew.writeLong(0);//107
        MapleInventory iv = chr.getInventory(MapleInventoryType.SETUP);
        MapleInventory iv2 = chr.getInventory(MapleInventoryType.EQUIP);
        List<Item> chairItems = new ArrayList<Item>();
        for (IItem item : iv.list()) {
            if (item.getItemId() >= 3010000 && item.getItemId() <= 3020001) {
                chairItems.add((Item) item);
            }
        }
        List<Item> medalItems = new ArrayList<Item>();
        for (IItem item : iv2.list()) {
            if (item.getItemId() >= 1142000 && item.getItemId() < 1152000) {
                medalItems.add((Item) item);
            }
        }
        //椅子列表
        mplew.writeInt(chairItems.size());
        for (IItem item : chairItems) {
            mplew.writeInt(item.getItemId());
        }
        //勋章列表
        mplew.writeInt(medalItems.size());
        for (IItem item : medalItems) {
            mplew.writeInt(item.getItemId());
        }

        ////log.debug("BOX包："+mplew.getPacket());
        return mplew.getPacket();
    }

    /**
     * 丢弃任务
     */
    public static MaplePacket forfeitQuest(MapleCharacter c, short quest) {

        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.SHOW_STATUS_INFO.getValue());
        mplew.write(1);
        mplew.writeShort(quest);
        mplew.writeShort(0);
        mplew.write(0);
        mplew.writeInt(0);
        mplew.writeInt(0);
        return mplew.getPacket();
    }

    /**
     * 完成任务
     *
     * @param c
     * @param quest
     * @return
     */
    public static MaplePacket completeQuest(MapleCharacter c, short quest) {

        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.SHOW_STATUS_INFO.getValue());
        mplew.write(1);
        mplew.writeShort(quest);
        mplew.write(2);
        mplew.writeShort(0);//093++
        mplew.writeLong(DateUtil.getFileTimestamp(System.currentTimeMillis()));

        return mplew.getPacket();
    }

    /**
     * 更新任务信息
     *
     * @param c
     * @param quest
     * @param npc
     * @param progress
     * @return
     */
    public static MaplePacket updateQuestInfo(MapleCharacter c, short quest, int npc, byte progress) {

        // [EE 00] [0A] [F1 03] [45 2F 00 00] [00 00 00 00] //089
        // [0F 01] [0A] [00 2B] [B5 65 8A 00] [00 00 00 00] //093
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.UPDATE_QUEST_INFO.getValue());
        mplew.write(progress);
        mplew.writeShort(quest);
        mplew.writeInt(npc);
        mplew.writeInt(0);
        return mplew.getPacket();
    }

    //更新任务
    public static MaplePacket updateQuest(int quest, String status) {

        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.SHOW_STATUS_INFO.getValue());
        mplew.write(1);
        mplew.writeShort(quest);
        mplew.write(1);
        mplew.writeMapleAsciiString(status);
        return mplew.getPacket();
    }

    //更新完成任务
    public static MaplePacket updateQuestFinish(short quest, int npc, short nextquest) {

        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.UPDATE_QUEST_INFO.getValue());
        mplew.write(8);
        mplew.writeShort(quest);
        mplew.writeInt(npc);
        mplew.writeShort(nextquest);
        mplew.writeShort(0);
        return mplew.getPacket();
    }

    public static <E extends Buffstat> void maskBuffStats(List<E> buffstats, MaplePacketLittleEndianWriter mplew, boolean foreign, boolean give) {
        maskBuffStats(buffstats, mplew, foreign, give, null);
    }

    public static <E extends Buffstat> void maskBuffStats(List<E> buffstats, MaplePacketLittleEndianWriter mplew, boolean foreign, boolean give, MapleForeignBuffSkill foreignBuffSkill) {
        for (int i = GameConstants.MAX_STATUS; i > 0; i--) {
            int value = 0;
            for (E buffstat : buffstats) {
                if (buffstat.getPosition() == i && (foreignBuffSkill == null || foreignBuffSkill.hasStat(buffstat))) {
                    value += (buffstat.getValue(foreign, give));
                }
            }
            mplew.writeInt(value);
        }
    }

    public static void maskBuffStats(MaplePacketLittleEndianWriter mplew, List<Pair<MapleBuffStat, Integer>> buffstats, boolean foreign, boolean give) {
        maskBuffStats(mplew, buffstats, foreign, give, null);
    }

    public static void maskBuffStats(MaplePacketLittleEndianWriter mplew, List<Pair<MapleBuffStat, Integer>> buffstats, boolean foreign, boolean give, MapleForeignBuffSkill foreignBuffSkill) {
        for (int i = GameConstants.MAX_STATUS; i > 0; i--) {
            int value = 0;
            for (Pair<MapleBuffStat, Integer> pair : buffstats) {
                if (pair.left.getPosition() == i && (foreignBuffSkill == null || foreignBuffSkill.hasStat(pair.left))) {
                    value += (pair.left.getValue(foreign, give));
                }
            }
            mplew.writeInt(value);
        }
    }

    public static void appledBuffStats(List<MapleBuffStat> buffstats, MaplePacketLittleEndianWriter mplew, int skillid) {

        for (int i = 0; i < buffstats.size(); i++) {
            MapleBuffStat mapleBuffStat = buffstats.get(i);
            switch (mapleBuffStat) {
                case MAPLE_WARRIOR:
                    i = buffstats.size();
                    mplew.write();
                    break;
                /* case 火焰咆哮_攻击力:
                 i = buffstats.size();
                 mplew.writeInt(0);
                 break;
                 case 月光祝福_命中率:
                 case 幸运的保护_HP:
                 i = buffstats.size();
                 mplew.writeLong(0);
                 break;*/
            }
            if (mapleBuffStat.getFootIndex() > 0) {
                mplew.writeInt(0);
            }
        }

        switch (skillid) {
            case 61120008:
                mplew.writeLong(0);
                break;
        }

        int a = cancelBuff(skillid);
        if (a > 0) {
            mplew.write(a);
        }


    }

    public static void appledBuffStats(MaplePacketLittleEndianWriter mplew, List<Pair<MapleBuffStat, Integer>> buffstats) {
        boolean exitfor = false;
        for (int i = 0; i < buffstats.size(); i++) {
            Pair<MapleBuffStat, Integer> pair = buffstats.get(i);
            switch (pair.left) {
                case MAPLE_WARRIOR:
                    mplew.write();
                    exitfor = true;
                    break;
                case 狂龙蓄气:
                    mplew.writeInt(0);
                    exitfor = true;
                    break;
            }
            if (exitfor) {
                break;
            }
        }
    }

    public static MaplePacket giveBuff(int buffid, int bufflength, List<Pair<MapleBuffStat, Integer>> statups, MapleCharacter chr) {

        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.GIVE_BUFF.getValue());
        maskBuffStats(mplew, statups, false, true);
        for (Pair<MapleBuffStat, Integer> statup : statups) {
            if (!statup.left.isFoot()) {
                int a = ((Integer) statup.getRight());
                if (MapleStatEffect.HasIntValue(buffid)) {
                    mplew.writeInt(a);
                } else {
                    mplew.writeShort(a);
                }
                mplew.writeInt(buffid);//技能ID
                mplew.writeInt(bufflength);//持续时间
            }
        }

        mplew.writeShort(0);

        for (Pair<MapleBuffStat, Integer> pair : statups) {
            switch (pair.left) {
                case 尖兵电池时间:
                    mplew.writeInt(chr.isPowerOpen() ? 1 : 0);
                    mplew.writeInt(7200);
                    mplew.writeInt(6);
                    mplew.writeInt(bufflength);
                    break;
            }
        }
        mplew.writeShort(0);
        for (Pair<MapleBuffStat, Integer> pair : statups) {
            switch (pair.left) {
                case COMBO:
                    mplew.write(5);
                    mplew.writeInt(153601);
                    mplew.write();
                    return mplew.getPacket();
            }
        }
        mplew.write(0);//093新增


        for (Pair<MapleBuffStat, Integer> pair : statups) {
            if (pair.left.isFoot()) {
                switch (pair.left) {
                    case 尖兵电池时间:
                        break;
                    case 坐骑状态:
                        mplew.writeInt(pair.right);
                        mplew.writeInt(buffid);
                        mplew.writeInt(0);
                        mplew.writeShort(0);
                    case 神秘代码_总伤害增加:
                        mplew.write(1);
                    default:
                        mplew.writeInt(pair.left.getFootIndex());
                        mplew.writeInt(buffid);
                        mplew.writeInt(pair.right);
                        mplew.writeInt(-1);
                        mplew.writeInt(bufflength);
                        break;
                }
            } else {
                switch (pair.left) {
                    case KUANGLONG_JIANBI:
                        mplew.writeInt(2);
                        mplew.writeInt(5);
                        mplew.writeInt(chr.getEquipPed(-11));
                        mplew.writeInt(5);
                        mplew.writeZero(20);
                        break;
                    case 圣歌祈祷_无视防御力:
                        mplew.writeInt(0);
                        break;
                    case 恶魔复仇者:
                        mplew.writeInt(24534);
                        break;
                }
            }
        }

        mplew.writeShort(0);//范围 组队技能
        mplew.write();
        mplew.write();//107
        int a = giveBuff(buffid);
        if (a > 0) {
            mplew.write(a);
        }
        appledBuffStats(mplew, statups);//根据buffstat追加字节
        return mplew.getPacket();
    }

    public static int giveBuff(int buffid) {
        int a = 0;
        switch (buffid) {
            case 1121000:
            case 1221000:
            case 1321000:
            case 2121000:
            case 2221000:
            case 2321000:
            case 3121000:
            case 3221000:
            case 4121000:
            case 4221000:
            case 5121000:
            case 5221000:
            case 21121000:
            case 4341000:
            case 22171000:
            case 32121007:
            case 33121007:
            case 35121007:
            case 1002:
            case 10001002:
            case 20001002:
            case 9001001:
            case 14101003:
            case 8000:
            case 10008000:
            case 20008000:
            case 20018000:
            case 30008000:
            case 4101004:
            case 4201003:
            case 5101007:
            case 双刀.暗影轻功:
            case 风灵使者.风影漫步:
                a = 5;
                break;
            case 32101003: //黄色灵气
            case 战法.进阶黄色灵气:
                a = 0x1F;
                break;
            case 33121006: //暴走状态
            case -2022458: //神兽的祝福
            case 5301003://猴子魔法
            case 5320008://超级猴子魔法
                a = 6;
                break;
            case 5111005: //超人变形
            case 5121003: //超级变身
            case 13111005:// 风灵使者 - 信天翁
            case 15111002:// 奇袭者 - 超级变身
                a = 7;
                break;
            case 战神.冰雪矛:
                a = 2;
                break;
            case 23121005://双弩。冒险勇士。
            case 31121004://恶魔的
            case -2002010://速度药丸
            case 5721000://传人勇士
            case 24121008://幻影 勇士
            case 4001005://轻功
            case 4301003://暗影轻功
            case 51121005://米哈尔勇士
            case 61120008:
                a = 1;
                break;
            case -2003516:
                a = 3;
                break;
        }
        return a;
    }

    public static MaplePacket cancelBuff(List<MapleBuffStat> statups, int skillid) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.CANCEL_BUFF.getValue());
        maskBuffStats(statups,
                mplew, false, false);
        appledBuffStats(statups, mplew, skillid);
        if (MapleStatEffect.isMonsterRiding(skillid)) {
            mplew.writeShort(289);
        }
        return mplew.getPacket();
    }

    public static int cancelBuff(int skillid) {
        int a = 0;
        switch (skillid) {
            case 5301003://猴子魔法
            case 5320008://超级猴子魔法
                a = 6;
                break;
            case 30001001: //潜入
                a = 0x11;
                break;
            case 32101003: //黄色灵气
            case 战法.进阶黄色灵气:
                a = 0x26;
                break;
            case 5111005: //超人变形
            case 5121003: //超级变身
            case 13111005:// 风灵使者 - 信天翁
            case 15111002:// 奇袭者 - 超级变身
                a = 9;
                break;
            case 33121006: //暴走状态
                a = 8;
                break;
            case 4321000: //龙卷风
            case 5001005: //疾驰
            case 1121000:
            case 1221000:
            case 1321000:
            case 2121000:
            case 2221000:
            case 2321000:
            case 3121000:
            case 3221000:
            case 4121000:
            case 4221000:
            case 5121000:
            case 5221000:
            case 21121000:
            case 4341000:
            case 22171000:
            case 32121007:
            case 33121007:
            case 35121007:
            case 1002:
            case 10001002:
            case 20001002:
            case 20011002:
            case 9001001:
            case 14101003:
            case 8000:
            case 10008000:
            case 20008000:
            case 20018000:
            case 30008000:
            case 4101004:
            case 4201003:
            case 5101007:
            case 双刀.暗影轻功:
            case 风灵使者.风影漫步:
                a = 5;
                break;
            case 23121005://双弩。冒险勇士。
            case 31121004://恶魔勇士
            case -2002010://速度药丸:
            case 24121008://幻影冒险岛勇士 
            case 4001005://轻功
            case 4301003://暗影轻功
            case 51121005://米哈尔勇士
            case 61120008://终极变身
            case 61120007://剑壁
            case 1111002: // 斗气集中
            case 11111001:// 斗气集中
            case 4111002: //影分身
            case 4211008://影分身
            case 14111000://影分身
                a = 1;
                break;
            /*
             * default: if (MapleStatEffect.isMonsterRiding(skillid)) { a = 3; }
             * break;
             */
            case 2003516:
                a = 6;
                break;
        }
        return a;
    }

    public static MaplePacket giveForeignBuff(MapleCharacter c, List<Pair<MapleBuffStat, Integer>> statups, MapleStatEffect effect) {

        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        // [C5 00] [12 01 2B 00] [00 00 00 00 00 00 00 00] [00 00 00 00 80 00 00 00] [0A 00] [00 00] [00] //077
        // [C5 00] [1F 01 2B 00] [00 00 00 00 00 00 00 00] [00 00 00 00 80 00 00 00] [0A 00] [00 00] [00]
        // [C9 00] [1D CE 4A 00] [00 00 00 00 00 00 00 00] [02 00 00 00 80 00 00 00] [28 E8] [03 00] [00 00] [00]
        mplew.writeShort(SendPacketOpcode.GIVE_FOREIGN_BUFF.getValue());
        mplew.writeInt(c.getId());
        maskBuffStats(mplew, statups, true, true, effect.getForeign());
        for (Pair<MapleBuffStat, Integer> statup : statups) {
            /* if (!statup.left.isFoot()) {
             if (effect.getSourceId() == 2003516 && statup.getLeft().equals(MapleBuffStat.SPEED)) {
             mplew.write(((Integer) statup.getRight()));
             mplew.writeShort(statups.get(2).getRight());
             mplew.writeInt((effect.isSkill() ? effect.getSourceId() : -effect.getSourceId()));//技能ID
             break;
             }
             short a = ((Integer) statup.getRight()).shortValue();
             if (effect.isShadowPartner()) {
             a = (short) effect.getX();
             }
             mplew.writeShort(a == 1002 ? 1002 : a);
             mplew.writeInt((effect.isSkill() ? effect.getSourceId() : -effect.getSourceId()));//技能ID
             mplew.writeInt(effect.getDuration());//持续时间
             }*/
            if (effect.getForeign().hasStat(statup.left)) {
                effect.getForeign().applyStat(mplew, statup.getLeft(), statup.getRight());
            }
        }
        if (effect.getForeign().hasStats()) {
            mplew.writeInt((effect.isSkill() ? effect.getSourceId() : -effect.getSourceId()));//技能ID
        }

        mplew.writeZero(3);
        addForeignData(c, statups, effect, mplew);
        mplew.writeZero(16);//106新增

        addForeignStats(c, statups, effect, mplew);
        mplew.writeShort(0);//范围 组队技能
        //有些技能最后多一个包
        int a = giveBuff(effect.getSourceId());
        if (a > 0) {
            mplew.write(a);
        }
        //log.info("普通技能封包:"+mplew.getPacket());
        return mplew.getPacket();
    }

    private static void addForeignData(MapleCharacter c, List<Pair<MapleBuffStat, Integer>> statups, MapleStatEffect effect, MaplePacketLittleEndianWriter mplew) {
        switch (effect.getSourceId()) {
            case 61120007:
                mplew.writeInt(2);
                mplew.writeInt(5);
                mplew.writeInt(c.getEquipPed(-11));
                mplew.writeInt(5);
                mplew.writeInt(0);
                break;
        }
    }

    private static void addForeignStats(MapleCharacter c, List<Pair<MapleBuffStat, Integer>> statups, MapleStatEffect effect, MaplePacketLittleEndianWriter mplew) {
        for (Pair<MapleBuffStat, Integer> p : statups) {
            if (p.left.isFoot()) {
                switch (p.left) {
                    case 坐骑状态:
                        mplew.writeInt(p.right);
                        mplew.writeInt(effect.getSourceId());
                        mplew.writeInt(0);
                        mplew.write(0);
                        break;
                    case SHADOWPARTNER:
                        mplew.writeZero(3);
                        break;
                    default:
                        break;
                }
            }
        }
    }

    public static MaplePacket cancelForeignBuff(int cid, List<MapleBuffStat> statups, MapleCharacter chr, MapleStatEffect effect) {

        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.CANCEL_FOREIGN_BUFF.getValue());
        mplew.writeInt(chr.getId());
        maskBuffStats(statups, mplew, true, false, effect.getForeign());
        appdCancelForeignBuff(effect.getSourceId(), mplew);
        return mplew.getPacket();
    }

    private static void appdCancelForeignBuff(int skillid, MaplePacketLittleEndianWriter mplew) {
        int a = cancelBuff(skillid);
        if (MapleStatEffect.isMonsterRiding(skillid)) {
            mplew.write(1);
        }
        if (a > 0) {
            mplew.write(a);
        }
    }

    public static MaplePacket testPacket() {
        //A4 00 02 F1 F8 4D 00 0A
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.write(Setting.getcashpacket());

        return mplew.getPacket();
    }

    public static MaplePacket testPacket(String test) {
        //A4 00 02 F1 F8 4D 00 0A
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.write(HexTool.getByteArrayFromHexString(test));

        return mplew.getPacket();
    }

    public static MaplePacket testPacket(byte[] test) {
        //A4 00 02 F1 F8 4D 00 0A
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.write(test);

        return mplew.getPacket();
    }

    //减益buff,怪给角色
    public static MaplePacket giveDebuff(long mask, List<Pair<MapleDisease, Integer>> statups, MobSkill skill) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.GIVE_BUFF.getValue());
        mplew.writeInt(0);
        //093 有4个mask
        mplew.writeLong(0);
        mplew.writeLong(0);
        mplew.writeLong(0);
        mplew.writeLong(mask);
        for (Pair<MapleDisease, Integer> statup : statups) {
            mplew.writeShort(statup.getRight().shortValue());
            mplew.writeShort(skill.getSkillId());//技能ID
            mplew.writeShort(skill.getSkillLevel());//技能等级
            mplew.writeInt((int) skill.getDuration());//持续时间
        }
        mplew.writeInt(0); // ??? wk charges have 600 here o.o
        mplew.write(3);//093修改
        mplew.write(0);
        mplew.write(5);//093修改
        return mplew.getPacket();
    }

    public static MaplePacket cancelDebuff(long mask) {
        /*
         * 25 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 40 07
         */

        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.CANCEL_BUFF.getValue());
        mplew.writeInt(0);
        //093 有4个mask
        mplew.writeLong(0);
        mplew.writeLong(0);
        mplew.writeLong(0);
        mplew.writeLong(mask);
        mplew.write(5);//97
        return mplew.getPacket();
    }

    public static MaplePacket giveForeignDebuff(int cid, long mask, List<Pair<MapleDisease, Integer>> statups, MobSkill skill) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.GIVE_FOREIGN_BUFF.getValue());
        mplew.writeInt(cid);
        //093 有4个mask
        mplew.writeLong(0);
        mplew.writeLong(0);
        mplew.writeLong(0);
        mplew.writeLong(mask);
        for (Pair<MapleDisease, Integer> statup : statups) {
            mplew.writeShort(statup.getRight().shortValue());
            mplew.writeShort(skill.getSkillId());//技能ID
            mplew.writeShort(skill.getSkillLevel());//技能等级
            mplew.writeInt((int) skill.getDuration());//持续时间
        }
        mplew.writeShort(0); // ??? wk charges have 600 here o.o
        mplew.writeShort(900);
        mplew.write(3);//093修改
        return mplew.getPacket();
    }

    public static MaplePacket cancelForeignDebuff(int cid, long mask) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.CANCEL_FOREIGN_BUFF.getValue());
        mplew.writeInt(cid);
        mplew.writeLong(0);
        mplew.writeLong(0);
        mplew.writeLong(0);
        mplew.writeLong(mask);
        return mplew.getPacket();
    }

    public static MaplePacket updateGP(int gid, int GP) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.GUILD_OPERATION.getValue());
        mplew.write(0x48);
        mplew.writeInt(gid);
        mplew.write(HexTool.getByteArrayFromHexString("D9 0A 32 00"));
        mplew.writeInt(GP);

        return mplew.getPacket();
    }

    public static MaplePacket showGuildRanks3(int npcid, ResultSet rs) throws SQLException {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.GUILD_OPERATION.getValue());
        mplew.write(0x50);
        mplew.writeInt(npcid);
        if (!rs.last()) //no guilds o.o
        {
            mplew.writeInt(0);
            return mplew.getPacket();
        }
        mplew.writeInt(rs.getRow());  //number of entries
        rs.beforeFirst();
        while (rs.next()) {
            mplew.writeMapleAsciiString(rs.getString("name"));
            mplew.writeInt(rs.getInt("level"));
            mplew.writeInt(rs.getInt("str"));
            mplew.writeInt(rs.getInt("dex"));
            mplew.writeInt(rs.getInt("int"));
            mplew.writeInt(rs.getInt("luk"));
        }
        return mplew.getPacket();
    }

    public static MaplePacket showGuildRanks8(int npcid, ResultSet rs) throws SQLException {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.GUILD_OPERATION.getValue());
        mplew.write(0x50);
        mplew.writeInt(npcid);
        if (!rs.last()) //no guilds o.o
        {
            mplew.writeInt(0);
            return mplew.getPacket();
        }
        mplew.writeInt(rs.getRow());  //number of entries
        rs.beforeFirst();
        while (rs.next()) {
            mplew.writeMapleAsciiString(rs.getString("name"));
            mplew.writeInt(rs.getInt("jh"));
            mplew.writeInt(rs.getInt("money"));
            mplew.writeInt(rs.getInt("loggedin"));
            mplew.writeInt(rs.getInt("greason"));
            mplew.writeInt(rs.getInt("banned"));
        }
        return mplew.getPacket();
    }

    public static MaplePacket showGuildRanks9(int npcid, ResultSet rs) throws SQLException {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.GUILD_OPERATION.getValue());
        mplew.write(0x50);
        mplew.writeInt(npcid);
        if (!rs.last()) //no guilds o.o
        {
            mplew.writeInt(0);
            return mplew.getPacket();
        }
        mplew.writeInt(rs.getRow());  //number of entries
        rs.beforeFirst();
        while (rs.next()) {
            mplew.writeMapleAsciiString(rs.getString("name"));
            mplew.writeInt(rs.getInt("jj"));
            mplew.writeInt(rs.getInt("GP"));
            mplew.writeInt(rs.getInt("logo"));
            mplew.writeInt(rs.getInt("logocolor"));
            mplew.writeInt(rs.getInt("logo"));
        }
        return mplew.getPacket();
    }

    public static MaplePacket showGuildRanks2(int npcid, ResultSet rs) throws SQLException {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.GUILD_OPERATION.getValue());
        mplew.write(0x50);
        mplew.writeInt(npcid);
        //if (!rs.last()) //no guilds o.o
        if (true) {
            mplew.writeInt(0);
            return mplew.getPacket();
        }
        mplew.writeInt(rs.getRow());  //number of entries
        rs.beforeFirst();
        while (rs.next()) {
            mplew.writeMapleAsciiString(rs.getString("name"));
            mplew.writeInt(rs.getInt("fame"));
            mplew.writeInt(rs.getInt("str"));
            mplew.writeInt(rs.getInt("dex"));
            mplew.writeInt(rs.getInt("int"));
            mplew.writeInt(rs.getInt("luk"));
        }
        return mplew.getPacket();
    }

    public static MaplePacket showGuildRanks5(int npcid, ResultSet rs) throws SQLException {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.GUILD_OPERATION.getValue());
        mplew.write(0x50);
        mplew.writeInt(npcid);
        if (!rs.last()) //no guilds o.o
        {
            mplew.writeInt(0);
            return mplew.getPacket();
        }
        mplew.writeInt(rs.getRow());  //number of entries
        rs.beforeFirst();
        while (rs.next()) {
            mplew.writeMapleAsciiString(rs.getString("name"));
            mplew.writeInt(rs.getInt("reborns"));
            mplew.writeInt(rs.getInt("str"));
            mplew.writeInt(rs.getInt("dex"));
            mplew.writeInt(rs.getInt("int"));
            mplew.writeInt(rs.getInt("luk"));
        }
        return mplew.getPacket();
    }

    //玩家商店聊天
    public static MaplePacket getPlayerShopChat(MapleCharacter c, String chat, byte slot) {

        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.PLAYER_INTERACTION.getValue());
        mplew.write(HexTool.getByteArrayFromHexString("06 08"));
        mplew.write(slot);
        mplew.writeMapleAsciiString(c.getName() + " : " + chat);
        return mplew.getPacket();
    }

    public static MaplePacket getPlayerShopChat(MapleCharacter c, String chat, boolean owner) {

        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.PLAYER_INTERACTION.getValue());
        mplew.write(PlayerInteractionHandler.Action.CHAT.getCode());
        mplew.write(HexTool.getByteArrayFromHexString("0F 00"));
        mplew.writeMapleAsciiString(c.getName() + " : " + chat);
        return mplew.getPacket();
    }

    //玩家商店新来的访问者
    public static MaplePacket getPlayerShopNewVisitor(MapleCharacter c, int slot) {

        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.PLAYER_INTERACTION.getValue());
        mplew.write(0x04);
        mplew.write(slot);
        addCharLook(mplew, c, false);
        //mplew.writeInt(0);
        mplew.writeMapleAsciiString(c.getName());
        return mplew.getPacket();
    }

    public static MaplePacket getPlayerShopRemoveVisitor(int slot) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.PLAYER_INTERACTION.getValue());
        mplew.write(0x0A);
        mplew.write(slot);
        return mplew.getPacket();
    }

    //移除玩家商店新来的访问者
    public static MaplePacket getTradePartnerAdd(MapleCharacter c) {

        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.PLAYER_INTERACTION.getValue());
        mplew.writeSome(0xA, 0x1);
        addCharLook(mplew, c, false);
        mplew.writeMapleAsciiString(c.getName());
        mplew.writeShort(c.getJobid());
        return mplew.getPacket();
    }

    // 交易邀请
    public static MaplePacket getTradeInvite(MapleCharacter c, boolean isTrade) {

        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.PLAYER_INTERACTION.getValue());
        mplew.writeSome(0xc, isTrade ? 0x4 : 0x3);
        //  mplew.write(HexTool.getByteArrayFromHexString("0B 04"));
        mplew.writeMapleAsciiString(c.getName());
        mplew.writeInt(c.getId());
        return mplew.getPacket();
    }

    public static MaplePacket getTradeMesoSet(byte number, long meso) {

        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.PLAYER_INTERACTION.getValue());
        mplew.write(PlayerInteractionHandler.Action.SET_MESO.getCode());
        mplew.write(number);
        mplew.writeLong(meso);
        return mplew.getPacket();
    }

    public static MaplePacket getTradeItemAdd(byte number, IItem item) {

        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.PLAYER_INTERACTION.getValue());
        mplew.write(PlayerInteractionHandler.Action.SET_ITEMS.getCode());
        mplew.write(number);
        addItemInfo(mplew, item, false, false, true);
        return mplew.getPacket();
    }

    public static MaplePacket getPlayerShopItemUpdate(MaplePlayerShop shop) {

        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.PLAYER_INTERACTION.getValue());
        mplew.write(0x15);
        mplew.write(shop.getItems().size());
        for (MaplePlayerShopItem item : shop.getItems()) {
            mplew.writeShort(item.getBundles());
            mplew.writeShort(item.getItem().getQuantity());
            mplew.writeLong(item.getPrice());
            addItemInfo(mplew, item.getItem(), true, true, false);
        }

        return mplew.getPacket();
    }

    /**
     *
     * @param c
     * @param shop
     * @param owner
     * @return
     */
    public static MaplePacket getPlayerShop(MapleClient c, MaplePlayerShop shop, boolean owner) {

        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.PLAYER_INTERACTION.getValue());
        mplew.write(HexTool.getByteArrayFromHexString("05 04 04"));
        mplew.write(owner ? 0 : 1);
        mplew.write(0);
        addCharLook(mplew, shop.getMCOwner(), false);
        mplew.writeMapleAsciiString(shop.getMCOwner().getName());
        mplew.write(1);
        addCharLook(mplew, shop.getMCOwner(), false);
        mplew.writeMapleAsciiString(shop.getMCOwner().getName());
        mplew.write(0xFF);
        mplew.writeMapleAsciiString(shop.getDescription());
        List<MaplePlayerShopItem> items = shop.getItems();
        mplew.write(0x10);
        mplew.write(items.size());
        for (MaplePlayerShopItem item : items) {
            mplew.writeShort(item.getBundles());
            mplew.writeShort(item.getItem().getQuantity());
            mplew.writeLong(item.getPrice());
            addItemInfo(mplew, item.getItem(), true, true, false);
        }
        return mplew.getPacket();
    }

    //交易
    public static MaplePacket getTradeStart(MapleClient c, MapleInteractionRoom trade, byte number, boolean Istrade) {

        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.PLAYER_INTERACTION.getValue());
        mplew.writeSome(0xB,
                Istrade ? 0x4 : 0x3,
                0x2);
        //mplew.write(HexTool.getByteArrayFromHexString("0A 04 02"));
        mplew.write(number);
        if (number == 1) {
            mplew.write(0);
            addCharLook(mplew, trade.getPartner().getChr(), false);
            mplew.writeMapleAsciiString(trade.getPartner().getChr().getName());
            mplew.writeShort(trade.getPartner().getChr().getJobid());
        }
        mplew.write(number);

        addCharLook(mplew, c.getPlayer(), false);
        mplew.writeMapleAsciiString(c.getPlayer().getName());
        mplew.writeShort(c.getPlayer().getJob().getId());
        mplew.write(0xFF);

        return mplew.getPacket();
    }

    public static MaplePacket getTradeConfirmation() {

        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.PLAYER_INTERACTION.getValue());
        mplew.write(PlayerInteractionHandler.Action.CONFIRM.getCode());
        return mplew.getPacket();
    }

    public static MaplePacket getTradeCompletion(byte number) {

        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.PLAYER_INTERACTION.getValue());
        mplew.write(PlayerInteractionHandler.Action.EXIT.getCode());
        mplew.write(number);
        mplew.write(8);

        return mplew.getPacket();
    }

    public static MaplePacket getTradeCancel(byte number) {

        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.PLAYER_INTERACTION.getValue());
        mplew.write(PlayerInteractionHandler.Action.EXIT.getCode());
        mplew.write(number);
        mplew.write(2);

        return mplew.getPacket();
    }

    public static MaplePacket getRPSGameStart() {
        MaplePacketLittleEndianWriter mplew = MakeMplew(SendPacketOpcode.PLAYER_INTERACTION);
        mplew.write(PlayerInteractionHandler.Action.RPSGameStart.getCode());
        return mplew.getPacket();
    }

    public static MaplePacket getRPSGameResult(int win, MapleRPSGame.SELECT sel) {
        MaplePacketLittleEndianWriter mplew = MakeMplew(SendPacketOpcode.PLAYER_INTERACTION);
        mplew.write(PlayerInteractionHandler.Action.RPSGameResult.getCode());
        mplew.write(win);//是否赢
        mplew.write(sel.getValue());//对方的选择




        return mplew.getPacket();
    }

    public static MaplePacket removeCharBox(MapleCharacter c) {

        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.UPDATE_CHAR_BOX.getValue());
        mplew.writeInt(c.getId());
        mplew.write(0);
        return mplew.getPacket();
    }

    public static MaplePacket getNPCTalk(int npc, byte msgType, String talk, String endBytes) {

        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.NPC_TALK.getValue());
        mplew.write(4); // ?
        mplew.writeInt(npc);//NPC代码
        mplew.write(msgType);
        mplew.write(0); // ?
        mplew.writeMapleAsciiString(talk);
        mplew.write(HexTool.getByteArrayFromHexString(endBytes));

        return mplew.getPacket();
    }

    public static MaplePacket getNPCTalk(int npc, byte msgType, String talk, String endBytes, byte speaker) {

        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.NPC_TALK.getValue());
        mplew.write(4); // ?
        mplew.writeInt(npc);
        mplew.write(msgType);
        mplew.write(speaker);
        mplew.writeMapleAsciiString(talk);
        mplew.write(HexTool.getByteArrayFromHexString(endBytes));
        return mplew.getPacket();
    }

    public static MaplePacket getNPCTalkStyle(int npc, String talk, int styles[], int card) {

        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.NPC_TALK.getValue());
        mplew.write(4); // ?
        mplew.writeInt(npc);
        mplew.writeShort(9); //type
        mplew.writeMapleAsciiString(talk);
        mplew.write(styles.length);
        for (int i = 0; i < styles.length; i++) {
            mplew.writeInt(styles[i]);
        }
        mplew.writeInt(card);
        return mplew.getPacket();
    }

    public static MaplePacket getNPCTalkNum(int npc, String talk, int def, int min, int max) {

        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.NPC_TALK.getValue());
        mplew.write(4); // ?
        mplew.writeInt(npc);
        mplew.writeShort(4);
        mplew.writeMapleAsciiString(talk);
        mplew.writeInt(def);
        mplew.writeInt(min);
        mplew.writeInt(max);
        mplew.writeInt(0);

        return mplew.getPacket();
    }

    public static MaplePacket getNPCTalkText(int npc, String talk) {

        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.NPC_TALK.getValue());
        mplew.write(4); // ?
        mplew.writeInt(npc);
        mplew.writeShort(3);
        mplew.writeMapleAsciiString(talk);
        mplew.writeShort(0);
        mplew.writeInt(0);//最少

        return mplew.getPacket();
    }

    public static MaplePacket showLevelup(int cid) {
        return showForeignEffect(cid, 0);
    }

    public static MaplePacket showJobChange(int cid) {
        return showForeignEffect(cid, 0x0c);
    }

    public static MaplePacket showForeignEffect(int cid, int effect) {

        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.SHOW_FOREIGN_EFFECT.getValue());
        mplew.writeInt(cid); // charid
        mplew.write(effect); // 0 = Level up, 8 = ?(RESIST), 0x0c = job change, 0x0d = Quest Complete
        return mplew.getPacket();
    }

    public static MaplePacket showBuffeffect(int cid, int skillid, int effectid) {
        return showBuffeffect(cid, skillid, effectid, (byte) 3, false);
    }

    public static MaplePacket 综合技能状态(int cid, int skillid, int effectid, int skillie) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.SHOW_FOREIGN_EFFECT.getValue());
        mplew.writeInt(cid); // ?
        mplew.write(effectid);
        mplew.writeInt(skillid);
        mplew.write(skillie);
        return mplew.getPacket();
    }

    public static MaplePacket showBuffeffect(int cid, int skillid, int effectid, byte direction, int skilllevel) {

        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.SHOW_FOREIGN_EFFECT.getValue());
        mplew.writeInt(cid); // ?
        mplew.write(effectid);
        mplew.writeInt(skillid);
        mplew.write(120);//玩家等级
        mplew.write(skilllevel);//技能等级
        if (direction != (byte) 3) {
            mplew.write(direction);
        }
        //mplew.write(1); //093增加
        return mplew.getPacket();
    }

    public static MaplePacket showBuffeffect(int cid, int skillid, int effectid, byte direction, boolean morph) {

        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.SHOW_FOREIGN_EFFECT.getValue());
        mplew.writeInt(cid);
        if (morph) {
            mplew.write(1);
            mplew.writeInt(skillid);
            mplew.write(direction);
        }
        mplew.write(effectid);
        mplew.writeInt(skillid);
        mplew.write(1);
        if (direction != (byte) 3) {
            mplew.write(direction);
        }

        return mplew.getPacket();
    }

    public static MaplePacket 龙之魂光效() {

        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        //09 01 07 20 01 14 00 龙之魂 
        mplew.writeShort(SendPacketOpcode.SHOW_ITEM_GAIN_INCHAT.getValue());
        mplew.write(7);
        mplew.writeInt(1311008);
        return mplew.getPacket();
    }

    public static MaplePacket 玩家的龙之魂(int cid, int skillid, int effectid, int skillid_Level) {
        if (show) {
            log.debug("调用的函数：" + new Throwable().getStackTrace()[0]); //显示调用的类 函数名 函数所在行
        }
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.SHOW_FOREIGN_EFFECT.getValue());
        mplew.writeInt(cid); // ?
        mplew.write(effectid);
        mplew.writeShort(288);
        mplew.writeShort(skillid_Level);
        return mplew.getPacket();
    }

    public static MaplePacket showOwnBuffEffect(int skillid, int effectid) {
        return showOwnBuffEffect(skillid, effectid, 1);
    }

    public static MaplePacket showOwnBuffEffect(int skillid, int effectid, int skilllevel) {

        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        //EE 00 02 63 C0 17 02 01 显示治疗机器人给人补血时候的特效
        //EE 00 02 F1 F8 4D 00 01 显示海盗获得能量的一个特效
        //09 01 02 2F 28 14 00 0A 显示灵魂祝福的召唤兽用buff的特效
        mplew.writeShort(SendPacketOpcode.SHOW_ITEM_GAIN_INCHAT.getValue());
        mplew.write(effectid);
        mplew.writeInt(skillid);
        mplew.write(0); //Ver0.78?
        mplew.write(skilllevel); // probably buff level but we don't know it and it doesn't really matter
        return mplew.getPacket();
    }

    public static MaplePacket showOwnBuffEffect_(int skillid, int effectid, int skilllevel) {

        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        //EE 00 02 63 C0 17 02 01 显示治疗机器人给人补血时候的特效
        //EE 00 02 F1 F8 4D 00 01 显示海盗获得能量的一个特效
        //09 01 02 2F 28 14 00 0A 显示灵魂祝福的召唤兽用buff的特效
        mplew.writeShort(SendPacketOpcode.SHOW_ITEM_GAIN_INCHAT.getValue());
        mplew.write(effectid);
        mplew.writeInt(skillid);
        mplew.write(skilllevel); // probably buff level but we don't know it and it doesn't really matter
        return mplew.getPacket();
    }

    public static MaplePacket showOwnBerserk(int skilllevel, boolean Berserk) { // 恶龙附身？

        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.SHOW_ITEM_GAIN_INCHAT.getValue());
        mplew.write(1);
        mplew.writeInt(1320006);
        mplew.write(skilllevel);
        mplew.write(Berserk ? 1 : 0);

        return mplew.getPacket();
    }

    public static MaplePacket showBerserk(int cid, int skilllevel, boolean Berserk) { // 恶龙附身？

        // [99 00] [5D 94 27 00] [01] [46 24 14 00] [14] [01]
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.SHOW_FOREIGN_EFFECT.getValue());
        mplew.writeInt(cid);
        mplew.write(1);
        mplew.writeInt(1320006);
        mplew.write(skilllevel);
        mplew.write(Berserk ? 1 : 0);

        return mplew.getPacket();
    }

    public static MaplePacket beholderAnimation(int cid, int skillid) {

        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.SHOW_FOREIGN_EFFECT.getValue());
        mplew.writeInt(cid);
        mplew.writeInt(skillid);
        mplew.writeShort(135);

        return mplew.getPacket();
    }

    /**
     * 刷新玩家技能。
     *
     * @param skillid
     * @param level
     * @param masterlevel
     * @param expiration
     * @return
     */
    public static MaplePacket updateSkill(int skillid, int level, int masterlevel, Timestamp expiration) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.UPDATE_SKILLS.getValue());
        mplew.writeShort(1);
        mplew.writeShort(1);
        mplew.writeInt(skillid);//技能ID
        mplew.writeInt(level);//技能等级
        mplew.writeInt(masterlevel);//可学习等级
        DateUtil.addSkillExpirationTime(mplew, expiration);
        mplew.write(0);
        return mplew.getPacket();
    }

    /**
     * 刷新玩家技能。
     *
     * @param skillid
     * @param level
     * @param masterlevel
     * @param expiration
     * @return
     */
    public static MaplePacket updateSkill(int[] skillid, int level[], int[] masterlevel, Timestamp[] expiration) {
        if (skillid != null && level != null && masterlevel != null && expiration != null
                && (skillid.length == level.length)) {
            MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
            mplew.writeShort(SendPacketOpcode.UPDATE_SKILLS.getValue());
            mplew.writeShort(1);
            mplew.writeShort(skillid.length);
            for (int i = 0; i < skillid.length; i++) {
                int j = skillid[i];
                int t = level[i];
                int m = masterlevel[i];
                Timestamp e = expiration[i];
                mplew.writeInt(j);//技能ID
                mplew.writeInt(t);//技能等级
                mplew.writeInt(m);//可学习等级
                DateUtil.addSkillExpirationTime(mplew, e);
            }
            mplew.write(0);
            return mplew.getPacket();
        }
        return null;
    }

    /*
     *
     * private static void addExpirationTime(MaplePacketLittleEndianWriter
     * mplew, long time) { addExpirationTime(mplew, time, true); }
     *
     * private static void addExpirationTime(MaplePacketLittleEndianWriter
     * mplew, long time, boolean addzero) { if (addzero) { mplew.write(0); }
     * mplew.write(ITEM_MAGIC); if (time == -1) { mplew.writeInt(400967355);
     * mplew.write(2); } else { mplew.writeInt(getItemTimestamp(time));
     * mplew.write(1); } }
     *
     * public static int getItemTimestamp(long realTimestamp) { int time = (int)
     * ((realTimestamp - REAL_YEAR2000) / 1000 / 60); // convert to minutes
     * return (int) (time * 35.762787) + ITEM_YEAR2000; } private final static
     * byte[] ITEM_MAGIC = new byte[]{(byte) 0x80, 0x05}; private final static
     * int ITEM_YEAR2000 = -1085019342; private final static long REAL_YEAR2000
     * = 946681229830L;
     */
    //更新任务杀怪
    public static MaplePacket updateQuestMobKills(MapleQuestStatus status) {

        // 21 00 01 FB 03 01 03 00 30 30 31
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.SHOW_STATUS_INFO.getValue());
        mplew.write(1);
        mplew.writeShort(status.getQuest().getId());
        mplew.write(1);
        String killStr = "";
        for (int kills : status.getMobKills().values()) {
            killStr += StringUtil.getLeftPaddedStr(String.valueOf(kills), '0', 3);
        }
        mplew.writeMapleAsciiString(killStr);
        mplew.writeInt(0);
        mplew.writeInt(0);

        return mplew.getPacket();
    }

    public static MaplePacket getShowQuestCompletion(int id) {

        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.SHOW_QUEST_COMPLETION.getValue());
        mplew.writeShort(id);

        return mplew.getPacket();
    }

    //键盘排序
    public static MaplePacket getKeymap(Map<Integer, MapleKeyBinding> keybindings) {

        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.KEYMAP.getValue());
        mplew.write(0);

        for (int x = 0; x < 90; x++) {
            MapleKeyBinding binding = keybindings.get(Integer.valueOf(x));
            if (binding != null) {
                mplew.write(binding.getType());
                mplew.writeInt(binding.getAction());
            } else {
                mplew.write(0);
                mplew.writeInt(0);
            }
        }

        return mplew.getPacket();
    }

    //自动吃药
    public static MaplePacket sendAutoHpPot(int itemId) {

        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.AUTO_HP_POT.getValue());
        mplew.writeInt(itemId);

        return mplew.getPacket();
    }

    //自动吃蓝
    public static MaplePacket sendAutoMpPot(int itemId) {

        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.AUTO_MP_POT.getValue());
        mplew.writeInt(itemId);

        return mplew.getPacket();
    }

    //悄悄话
    public static MaplePacket getWhisper(String sender, int channel, String text) {

        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.WHISPER.getValue());
        mplew.write(18);
        mplew.writeMapleAsciiString(sender);
        mplew.writeShort(channel - 1);
        mplew.writeMapleAsciiString(text);

        return mplew.getPacket();
    }

    /**
     * 悄悄话
     *
     * @param target name of the target character
     * @param reply error code: 0x0 = cannot find char, 0x1 = success
     * @return the MaplePacket
     */
    public static MaplePacket getWhisperReply(String target, byte reply) {

        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.WHISPER.getValue());
        mplew.write(10);
        mplew.writeMapleAsciiString(target);
        mplew.write(reply);

        return mplew.getPacket();
    }

    //背包满了
    public static MaplePacket getInventoryFull() {

        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        //右下角显示[不能捡取物品。]
        mplew.writeShort(SendPacketOpcode.MODIFY_INVENTORY_ITEM.getValue());
        mplew.write(1);//1
        mplew.write(0);
        mplew.write(0);

        return mplew.getPacket();
    }

    public static MaplePacket getShowInventoryFull() {
        return getShowInventoryStatus(0xFF);
    }

    public static MaplePacket showItemUnavailable() {
        return getShowInventoryStatus(0xFE);
    }

    public static MaplePacket getShowInventoryStatus(int mode) {

        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.SHOW_STATUS_INFO.getValue());
        mplew.write(0);
        mplew.write(mode);
        //93去除
        //mplew.writeInt(0);
        //mplew.writeInt(0);

        return mplew.getPacket();
    }

    //仓库
    public static MaplePacket getStorage(int npcId, byte slots, long meso, MapleStorage storage) {

        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.OPEN_STORAGE.getValue());
        mplew.write(0x16);
        mplew.writeInt(npcId);
        mplew.write(slots);
        mplew.writeShort(0x7E);
        mplew.writeShort(0);
        mplew.writeInt(0);
        mplew.writeLong(meso);

        List<IItem> current = storage.AllEnumMap().get(MapleInventoryType.EQUIP);
        mplew.write(current.size());
        for (IItem iItem : current) {
            addItemInfo(mplew, iItem, true);
        }

        current = storage.AllEnumMap().get(MapleInventoryType.USE);
        mplew.write(current.size());
        for (IItem iItem : current) {
            addItemInfo(mplew, iItem, true);
        }

        current = storage.AllEnumMap().get(MapleInventoryType.SETUP);
        mplew.write(current.size());
        for (IItem iItem : current) {
            addItemInfo(mplew, iItem, true);
        }

        current = storage.AllEnumMap().get(MapleInventoryType.ETC);
        mplew.write(current.size());
        for (IItem iItem : current) {
            addItemInfo(mplew, iItem, true);
        }

        current = storage.AllEnumMap().get(MapleInventoryType.CASH);
        mplew.write(current.size());
        for (IItem iItem : current) {
            addItemInfo(mplew, iItem, true);
        }

        /*
         * mplew.write(0); mplew.write((byte) items.size()); for (IItem item :
         * items) { addItemInfo(mplew, item, true, true, true); }
         * mplew.writeZero(3);
         */
        //mplew.writeInt(0);

        return mplew.getPacket();
    }

    //仓库满了
    public static MaplePacket getStorageFull() {

        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.OPEN_STORAGE.getValue());
        mplew.write(0x11);

        return mplew.getPacket();
    }

    //金币仓库
    public static MaplePacket mesoStorage(byte slots, long meso) {

        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.OPEN_STORAGE.getValue());
        mplew.write(0x13);
        mplew.write(slots);
        mplew.writeShort(2);
        mplew.writeShort(0);
        mplew.writeInt(0);
        mplew.writeLong(meso);

        return mplew.getPacket();
    }

    //存入仓库
    public static MaplePacket storeStorage(byte slots, MapleInventoryType type, Collection<IItem> items) {

        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.OPEN_STORAGE.getValue());
        mplew.write(0xD);
        mplew.write(slots);
        mplew.writeShort(type.getBitfieldEncoding());
        mplew.writeShort(0);
        mplew.writeInt(0);
        mplew.write(items.size());
        for (IItem item : items) {
            //mplew.write(ii.getInventoryType(item.getItemId()).getType());
            addItemInfo(mplew, item, true);
        }

        return mplew.getPacket();
    }

    //仓库取出
    public static MaplePacket takeOutStorage(byte slots, MapleInventoryType type, Collection<IItem> items) {

        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.OPEN_STORAGE.getValue());
        mplew.write(0x9);
        mplew.write(slots);
        mplew.writeShort(type.getBitfieldEncoding());
        mplew.writeShort(0);
        mplew.writeInt(0);
        mplew.write(items.size());
        for (IItem item : items) {
            //mplew.write(ii.getInventoryType(item.getItemId()).getType());
            addItemInfo(mplew, item, true);
        }

        return mplew.getPacket();
    }

    //显示怪物HP
    public static MaplePacket showMonsterHP(int oid, int remhppercentage) {

        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        //[1F 01] [2B 37 94 00] 50
        //[29 01] [E2 3C 6C 01] 5A
        mplew.writeShort(SendPacketOpcode.SHOW_MONSTER_HP.getValue());
        mplew.writeInt(oid);
        mplew.write(remhppercentage);

        return mplew.getPacket();
    }

    //显示BOSS HP
    public static MaplePacket showBossHP(int oid, int currHP, int maxHP, byte tagColor, byte tagBgColor) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.BOSS_ENV.getValue());
        mplew.write(6);
        mplew.writeInt(oid);
        mplew.writeInt(currHP);
        mplew.writeInt(maxHP);
        mplew.write(tagColor);
        mplew.write(tagBgColor);

        return mplew.getPacket();
    }

    //人气反馈
    public static MaplePacket giveFameResponse(int mode, String charname, int newfame) {

        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.FAME_RESPONSE.getValue());
        mplew.write(0);
        mplew.writeMapleAsciiString(charname);
        mplew.write(mode);//1 add
        mplew.writeInt(newfame);
        return mplew.getPacket();
    }

    /**
     * status can be: <br> 0: ok, use giveFameResponse<br> 1: the username is
     * incorrectly entered<br> 2: users under level 15 are unable to toggle with
     * fame.<br> 3: can't raise or drop fame anymore today.<br> 4: can't raise
     * or drop fame for this character for this month anymore.<br> 5: received
     * fame, use receiveFame()<br> 6: level of fame neither has been raised nor
     * dropped due to an unexpected error
     *
     * @param status
     * @return
     */
    public static MaplePacket giveFameErrorResponse(int status) {

        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.FAME_RESPONSE.getValue());
        mplew.write(status);

        return mplew.getPacket();
    }

    public static MaplePacket receiveFame(int mode, String charnameFrom) {

        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.FAME_RESPONSE.getValue());
        mplew.write(5);
        mplew.writeMapleAsciiString(charnameFrom);
        mplew.write(mode);

        return mplew.getPacket();
    }

    //开启组队
    public static MaplePacket partyCreated(MapleParty party) {

        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        //3D 00 [08] [DC 40] [01 00] [FF C9 9A 3B] [FF C9 9A 3B] [00 00 00 00] 6D 1D 01 25
        mplew.writeShort(SendPacketOpcode.PARTY_OPERATION.getValue());
        mplew.write(0x0C);//099 - [0A] TO [0C]
        mplew.writeInt(party.getId());
        mplew.writeInt(999999999);
        mplew.writeInt(999999999);
        mplew.writeInt(0);
        mplew.write(HexTool.getByteArrayFromHexString("E6 17 FF FF 01"));
        return mplew.getPacket();
    }

    //组队邀请
    public static MaplePacket partyInvite(MapleCharacter from) {

        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
//3C 00 [04] [96 4B 00 00] [0A 00 CB A7 C6 F8 B5 C4 BF F1 CF E8] 78 00 00 00 B2 01 00 00 00
        mplew.writeShort(SendPacketOpcode.PARTY_OPERATION.getValue());
        mplew.write(4);
        mplew.writeInt(from.getParty().getId());
        mplew.writeMapleAsciiString(from.getName());
        mplew.writeInt(from.getLevel());
        mplew.writeInt(from.getJob().getId());
        mplew.writeInt(0);//104 new
        mplew.write(0);
        return mplew.getPacket();
    }

    /**
     * 10: A beginner can't create a party. 1/11/14/19: Your request for a party
     * didn't work due to an unexpected error. 13: You have yet to join a party.
     * 16: Already have joined a party. 17: The party you're trying to join is
     * already in full capacity. 19: Unable to find the requested character in
     * this channel.
     *
     * @param message
     * @return
     */
    //组队消息
    public static MaplePacket partyStatusMessage(int message) {

        // 32 00 08 DA 14 00 00 FF C9 9A 3B FF C9 9A 3B 22 03 6E 67
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.PARTY_OPERATION.getValue());
        mplew.write(message);
        return mplew.getPacket();
    }

    /**
     * 23: 'Char' have denied request to the party.
     *
     * @param message
     * @param charname
     * @return
     */
    public static MaplePacket partyStatusMessage(int message, String charname) {
        // 32 00 08 DA 14 00 00 FF C9 9A 3B FF C9 9A 3B 22 03 6E 67

        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.PARTY_OPERATION.getValue());
        mplew.write(message);
        mplew.writeMapleAsciiString(charname);

        return mplew.getPacket();
    }

    //加入组队状态
    private static void addPartyStatus(int forchannel, MapleParty party, MaplePacketLittleEndianWriter lew) {
        List<MaplePartyCharacter> partymembers = new ArrayList<MaplePartyCharacter>(party.getMembers());

        for (MaplePartyCharacter partychar : partymembers) {
            lew.writeInt(partychar.getId());
        }
        for (int i = 0; i < 6 - partymembers.size(); i++) {
            lew.writeInt(0);
        }
        //填充编号

        for (MaplePartyCharacter partychar : partymembers) {
            lew.writeMapleNameString(partychar.getName());
        }
        for (int i = 0; i < 6 - partymembers.size(); i++) {
            lew.writeZero(13);
        }
        //填充角色名


        for (MaplePartyCharacter partychar : partymembers) {
            lew.writeInt(partychar.getJobId());
        }
        for (int i = 0; i < 6 - partymembers.size(); i++) {
            lew.writeInt(0);
        }
        //填充职业编号

        for (int i = 0; i < 6; i++) {
            lew.writeInt(0);
        }
        //104 未知新填充

        for (MaplePartyCharacter partychar : partymembers) {
            lew.writeInt(partychar.getLevel());
        }
        for (int i = 0; i < 6 - partymembers.size(); i++) {
            lew.writeInt(0);
        }
        //填充等级

        for (MaplePartyCharacter partychar : partymembers) {
            if (partychar.isOnline()) {
                lew.writeInt(partychar.getChannel() - 1);
            } else {
                lew.writeInt(-2);
            }
        }
        for (int i = 0; i < 6 - partymembers.size(); i++) {
            lew.writeInt(-2);
        }
        //填充频道

        for (int i = 0; i < 6; i++) {
            lew.writeInt(0);
        }
        lew.writeInt(party.getLeader().getId());

        for (MaplePartyCharacter partychar : partymembers) {
            if (partychar.getChannel() == forchannel) {
                lew.writeInt(partychar.getMapid());
            } else {
                lew.writeInt(999999999);
            }
        }
        for (int i = 0; i < 6 - partymembers.size(); i++) {
            lew.writeInt(0);
        }
        //填充所在地图

        /*
         * for (MaplePartyCharacter partychar : partymembers) { lew.writeHex("FF
         * C9 9A 3B FF C9 9A 3B"); lew.writeInt(0); lew.writeHex("5D 20 0D 0A 5B
         * 5D 20 53"); }
         */
        for (int i = 0; i < 6; i++) {
            lew.writeZero(8 + 4 + 8);
        }
        lew.write(1);
    }

    public static MaplePacket updateParty(int forChannel, MapleParty party, PartyOperation op, MaplePartyCharacter target) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.PARTY_OPERATION.getValue());
        switch (op) {
            case DISBAND://解散
            case EXPEL://驱逐
            case LEAVE://离开
                mplew.write(0x10);//0c
                mplew.writeInt(party.getId());
                mplew.writeInt(target.getId());

                if (op == PartyOperation.DISBAND) {
                    mplew.write(0);
                    mplew.writeInt(party.getId());
                } else {
                    mplew.write(1);
                    if (op == PartyOperation.EXPEL) {
                        mplew.write(1);
                    } else {
                        mplew.write(0);
                    }
                    mplew.writeMapleAsciiString(target.getName());
                    addPartyStatus(forChannel, party, mplew);
                }
                break;
            case JOIN://加入 097 update
                mplew.write(0x13);
                mplew.writeInt(party.getId());
                mplew.writeMapleAsciiString(target.getName());
                addPartyStatus(forChannel, party, mplew);
                // addJoinPartyTail(mplew);
                break;
            case SILENT_UPDATE://下线 097 update
            case LOG_ONOFF:
                mplew.write(0xB);
                mplew.writeInt(party.getId());
                addPartyStatus(forChannel, party, mplew);
                break;
            case CHANGE_LEADER://换队长 097 update
                mplew.write(0x23);
                mplew.writeInt(target.getId());
                mplew.write(0);
                break;

        }
        return mplew.getPacket();
    }

    //组队端口
    public static MaplePacket partyPortal(int townId, int targetId, Point position) {

        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.PARTY_OPERATION.getValue());
        mplew.writeShort(0x23);
        mplew.writeInt(townId);
        mplew.writeInt(targetId);
        mplew.writeShort(position.x);
        mplew.writeShort(position.y);

        return mplew.getPacket();
    }

    public static MaplePacket updatePartyMemberHP(int cid, int curhp, int maxhp) {

        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.UPDATE_PARTYMEMBER_HP.getValue());
        mplew.writeInt(cid);
        mplew.writeInt(curhp);
        mplew.writeInt(maxhp);

        return mplew.getPacket();
    }

    /**
     * mode: 0 buddychat好友聊天; 1 partychat组队聊天; 2 guildchat家族聊天; 3 Alliance
     * chat联盟聊天 4 远征队。
     *
     * @param name
     * @param chattext
     * @param mode
     * @return
     */
    public static MaplePacket multiChat(String name, String chattext, int mode) {

        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.MULTICHAT.getValue());
        mplew.write(mode);
        mplew.writeMapleAsciiString(name);
        mplew.writeMapleAsciiString(chattext);

        return mplew.getPacket();
    }

    //<<<<<<< 以更高效的方式改写 玩家BUFF系统的 MARK头数据方法  2013年4月29日 02:44:02
    /**
     * 编写头MArK位数据,更高效的写入
     */
    public static void markMonsterStat(MaplePacketLittleEndianWriter mplew, Map<MapleMonsterStat, Integer> stats) {
        //编辑mark到数组
        int[] values = new int[GameConstants.MAX_MONSTER_STATUS];
        for (MapleMonsterStat stat : stats.keySet()) {
            if (stat.getPosition() > 0) {
                values[stat.getPosition() - 1] += stat.getMarkValue();
            }
        }
        //写数组
        for (int i = GameConstants.MAX_MONSTER_STATUS; i > 0; i--) {
            mplew.writeInt(values[i - 1]);
        }

    }

    //应用怪物状态 添加怪物状态
    public static MaplePacket applyMonsterStatus(MapleMonster mob, MapleCharacter chr, int oid, Map<MapleMonsterStat, Integer> stats, int skill, int delay, MobSkill mobskill) {

        /*
         * 战神之审判 有毒buff的要分开处理 51 01 F5 8F 6A 00 00 00 00 00 00 00 00 00 00 00 00
         * 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 01 00 08 01 00
         * FB 1C 42 01 16 00
         *
         * 01 9F 1E 44 00 //cid FB 1C 42 01 //skillid FE 1F 00 00 //每次扣除的血量 E8
         * 03 00 00 //隔多久扣血一次[毫秒] 57 0E 62 1E //? 06 00 00 00 //毒的期间扣血多少次
         *
         * 56 04 02 01
         */
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.APPLY_MONSTER_STATUS.getValue());
        mplew.writeInt(oid);

        markMonsterStat(mplew, stats);

        for (MapleMonsterStat stat : stats.keySet()) {
            Integer val = stats.get(stat);

            if (stat.equals(MapleMonsterStat.POISON)) {//毒的特殊写法.语法暂定
                mplew.write(1);
                mplew.writeInt(chr.getId());
                mplew.writeInt(skill);
                mplew.writeInt(stats.get(MapleMonsterStat.POISON));//获取每次扣除的血量
                mplew.writeInt(1000);//隔多久扣血一次[毫秒]
                mplew.writeInt(0);
                mplew.writeInt(10);//毒的期间扣血多少次
                mplew.writeZero(6);//?
            } else {
                mplew.writeShort(val);
                mplew.writeShort(0);//97
                if (mobskill != null) {
                    mplew.writeShort(mobskill.getSkillId());
                    mplew.writeShort(mobskill.getSkillLevel());
                } else {
                    mplew.writeInt(skill);
                }
                mplew.writeShort(0);//?109
                if (stats.size() > 1) {
                    mplew.writeShort(delay);
                }
                //注意 这个函数中没有buff的持续时间
                mplew.writeShort(delay);
            }
        }


        //这4个不知道是什么
        mplew.write(0x02);

        if (skill != 机械师.加速器) {
            mplew.write(1);
        }
        if (checkHaveGivePacket(skill)) //有些技能最后没有这一位
        {
            mplew.write(1);
        }
        ////log.debug("给怪物BUFF的封包："+mplew.toString());
        return mplew.getPacket();
    }

    public static boolean checkHaveGivePacket(int skillid) {

        switch (skillid) {
            case 4121004: //忍者伏击
            case 4221004:
            case 4121003: //挑衅
            case 4221003:
            case 21101003://抗压
            case 4341003: //怪物炸弹
            case 22161002://鬼刻符
            case 1111007: //魔击无效
            case 1211009:
            case 1311007:
            case 机械师.加速器:
            case 火毒.毒雾术:
                return false;
        }
        return true;
    }

    // 取消怪物状态
    public static MaplePacket cancelMonsterStatus(int cid, int oid, Map<MapleMonsterStat, Integer> stats, int skillid) {

        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        /*
         * 52 01 E7 2E 42 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00
         * 00 00 00 00 00 00 00 00 00 00 00 80 00 00 00 01 02
         */
        /*
         * 冰冻术 52 01 01 87 33 01 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00
         * 00 00 00 00 00 00 00 00 00 00 00 00 00 01 00 00 03 02
         */
        mplew.writeShort(SendPacketOpcode.CANCEL_MONSTER_STATUS.getValue());
        mplew.writeInt(oid);
        markMonsterStat(mplew, stats);
        for (MapleMonsterStat stat : stats.keySet()) {
            if (stat.equals(MapleMonsterStat.POISON)) {
                mplew.writeInt(0);//?
                mplew.writeInt(1);
                mplew.writeInt(cid);
                mplew.writeInt(skillid);
            } else {
                mplew.write(3);
            }
        }
        mplew.write(2);
        /*
         * if (checkHaveCancelPacket(skillid)) //有些技能多了这一位 { mplew.write(2); }
         */
        return mplew.getPacket();
    }

    public static boolean checkHaveCancelPacket(int skillid) {

        switch (skillid) {
            case 火毒.毒雾术:
            case 机械师.加速器:
                return true;
        }
        return false;
    }
    //时钟

    public static MaplePacket getClock(int time) { // time in seconds

        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.CLOCK.getValue());
        mplew.write(2); // clock type. if you send 3 here you have to send another byte (which does not matter at all) before the timestamp
        mplew.writeInt(time);

        return mplew.getPacket();
    }

    public static MaplePacket getClockTime(int hour, int min, int sec) { // Current Time

        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.CLOCK.getValue());
        mplew.write(1); // Clock-Type
        mplew.write(hour);
        mplew.write(min);
        mplew.write(sec);
        return mplew.getPacket();
    }

    //召唤LOVE
    public static MaplePacket spawnLove(int oid, int itemid, String name, String msg, Point pos, int ft) {

        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.SPAWN_LOVE.getValue());
        mplew.writeInt(oid);
        mplew.writeInt(itemid);
        mplew.writeMapleAsciiString(msg);
        mplew.writeMapleAsciiString(name);
        mplew.writeShort(pos.x);
        mplew.writeShort(ft);
        return mplew.getPacket();
    }

    //移除LOVE
    public static MaplePacket removeLove(int oid) {
//DB 01 00 02 00 00 00
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.REMOVE_LOVE.getValue());
        mplew.write();
        mplew.writeInt(oid);
        return mplew.getPacket();
    }

    public static MaplePacket spawnFace(int skill) {

        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(0xCF);
        mplew.writeInt(skill);
        return mplew.getPacket();
    }

    //召唤烟雾
    public static MaplePacket spawnMist(MapleMist mist) {

        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        Rectangle position = mist.getBox();
        mplew.writeShort(SendPacketOpcode.SPAWN_MIST.getValue());
        mplew.writeInt(mist.getObjectId());
        mplew.writeInt(mist.getMistValue1());
        mplew.writeInt(mist.isMobMist() ? mist.getMobOwner().getId() : mist.getOwner().getId());
        mplew.writeInt(mist.isItemMist() ? mist.getSourceId() : mist.isMobMist() ? mist.getMobSkill().getSkillId() : mist.getSourceSkill().getId());
        mplew.write(mist.isItemMist() ? 0 : mist.isMobMist() ? mist.getMobSkill().getSkillLevel() : mist.getOwner().getSkillLevel(mist.getSourceSkill()));
        mplew.writeShort(mist.getMistValue2());
        mplew.writeInt(position.x);
        mplew.writeInt(position.y);
        mplew.writeInt(position.x + position.width);
        mplew.writeInt(position.y + position.height);
        mplew.writeZero(16);
        ////log.debug("烟雾值1: "+(mist.isMobMist() ? 0 : mist.isPoison() ? 1 : mist.isItemMist() ? 3 : 2));
        //ystem.out.println("itemid: "+mist.getSourceId());
        ////log.debug("烟雾值2: "+(mist.isMobMist() ? 0 : mist.isItemMist() ? 3 : 8));
        //log.debug("烟雾值1: "+mist.getMistValue1());
        //log.debug("烟雾值2: "+mist.getMistValue2());
        return mplew.getPacket();
    }

    //臭屁 花香
    public static MaplePacket spawnCashMist(MapleCharacter chr, int itemid) {

        int x = chr.getPosition().x;
        int y = chr.getPosition().y;
        int x1 = -110 + x;
        int y1 = -82 + y;
        int x2 = 110 + x;
        int y2 = 83 + y;
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.SPAWN_MIST.getValue());
        mplew.writeInt(104);
        mplew.writeInt(3);
        mplew.writeInt(chr.getId());
        mplew.writeInt(itemid);
        mplew.write(0);
        mplew.writeShort(3);
        mplew.writeInt(x1);
        mplew.writeInt(y1);
        mplew.writeInt(x2);
        mplew.writeInt(y2);
        mplew.writeZero(16);
        return mplew.getPacket();
    }

    //移除烟雾
    public static MaplePacket removeMist(MapleMist mist) {

        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.REMOVE_MIST.getValue());
        mplew.writeInt(mist.getObjectId());
        /* if (!mist.isMobMist() && mist.getSourceSkill().getId() == 2111003) {// 致命毒雾
         mplew.write(0);
         }*/
        mplew.write();//有些技能需要这个0


        return mplew.getPacket();
    }

    //怪物伤害
    public static MaplePacket damageMonster(int oid, int damage) {

        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.DAMAGE_MONSTER.getValue());
        mplew.writeInt(oid);
        mplew.write(0);
        mplew.writeInt(damage);

        return mplew.getPacket();
    }

    //怪物伤害
    public static MaplePacket healMonster(int oid, int heal) {

        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.DAMAGE_MONSTER.getValue());
        mplew.writeInt(oid);
        mplew.write(0);
        mplew.writeInt(-heal);

        return mplew.getPacket();
    }

    //更新好友列表
    public static MaplePacket updateBuddylist(Collection<BuddylistEntry> buddylist) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.BUDDYLIST.getValue());
        mplew.write(0x0A);
        mplew.write(buddylist.size());
        for (BuddylistEntry buddy : buddylist) {
            //  if (buddy.isVisible()) {
            mplew.writeInt(buddy.getCharacterId()); // cid
            mplew.WriteOfMaxByteCountString(buddy.getName(), 13);
            mplew.write(0);
            mplew.writeInt(buddy.isVisible() ? buddy.getChannel() - 1 : -1);

            mplew.WriteOfMaxByteCountString(buddy.getName(), 17);
            //    }
        }
        for (int x = 0; x < buddylist.size(); x++) {
            mplew.writeInt(0);
        }
        return mplew.getPacket();
    }

    //好友列表消息
    public static MaplePacket buddylistMessage(byte message) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();


        mplew.writeShort(SendPacketOpcode.BUDDYLIST.getValue());
        mplew.write(message);

        return mplew.getPacket();
    }

    //申请加好友
    public static MaplePacket requestBuddylistAdd(int cidFrom, String nameFrom, int charlevel, int channel, int jobid) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.BUDDYLIST.getValue());
        mplew.write(9);
        mplew.writeInt(cidFrom);
        mplew.writeMapleAsciiString(nameFrom);
        mplew.writeInt(charlevel);
        mplew.writeInt(jobid); //char job
        mplew.writeInt(0);
        mplew.writeInt(cidFrom);
        mplew.writeMapleNameString(nameFrom);
        mplew.write(1);
        mplew.writeInt(channel - 1);
        mplew.writeAsciiString("未指定群组");
        mplew.writeLong(0);
        return mplew.getPacket();
    }

    //更新好友的频道
    public static MaplePacket updateBuddyChannel(int characterid, int channel) {
        // 38 00 14 F5 C5 58 00 00 06 00 00 00
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();


        mplew.writeShort(SendPacketOpcode.BUDDYLIST.getValue());
        mplew.write(0x14);
        mplew.writeInt(characterid);
        mplew.write(0);
        mplew.writeInt(channel);

        return mplew.getPacket();
    }

    //显示物品效果
    public static MaplePacket itemEffect(int characterid, int itemid) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();


        mplew.writeShort(SendPacketOpcode.SHOW_ITEM_EFFECT.getValue());

        mplew.writeInt(characterid);
        mplew.writeInt(itemid);

        return mplew.getPacket();
    }

    // public static MaplePacket itemEffects(int characterid, int itemid) {
    //  MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
    //  log.info("该死的增益效果");
    //  mplew.writeShort(SendPacketOpcode.SHOW_ITEM_GAIN_INCHAT.getValue());
    //   mplew.writeInt(characterid);
    //   mplew.writeInt(itemid);
    //  mplew.writeLong(0);
    //  return mplew.getPacket();
    //  }
    //物品效果能力
    /**
     * 刷新好友上限
     *
     * @param capacity
     * @return
     */
    public static MaplePacket updateBuddyCapacity(int capacity) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.BUDDYLIST.getValue());
        mplew.write(0x15);
        mplew.write(capacity);

        return mplew.getPacket();
    }

    //椅子效果
    public static MaplePacket showChair(int characterid, int itemid) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();


        mplew.writeShort(SendPacketOpcode.SHOW_CHAIR.getValue());

        mplew.writeInt(characterid);
        mplew.writeInt(itemid);
        mplew.writeInt(0);

        return mplew.getPacket();
    }

    //取消椅子
    public static MaplePacket cancelChair() {
        return cancelChair(-1);
    }

    public static MaplePacket cancelChair(int id) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.CANCEL_CHAIR.getValue());
        if (id == -1) {
            mplew.write(0);
        } else {
            mplew.write(1);
            mplew.writeShort(id);
        }
        return mplew.getPacket();
    }

    // is there a way to spawn reactors non-animated?
    public static MaplePacket spawnReactor(MapleReactor reactor) {

        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        Point pos = reactor.getPosition();
        mplew.writeShort(SendPacketOpcode.REACTOR_SPAWN.getValue());
        mplew.writeInt(reactor.getObjectId());
        mplew.writeInt(reactor.getId());
        mplew.write(reactor.getState());
        mplew.writeShort(pos.x);
        mplew.writeShort(pos.y);
        mplew.write(reactor.getFacingDirection()); // stance
        mplew.writeMapleAsciiString(reactor.getName());

        return mplew.getPacket();
    }

    public static MaplePacket triggerReactor(MapleReactor reactor, int stance) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        Point pos = reactor.getPosition();

        /*
         * 86 01 [E8 05 FA 00] [01] [F5 08] [69 FF] 89 01 00 09 86 01 E8 05 FA
         * 00 02 F5 08 69 FF 89 01 00 04 86 01 E8 05 FA 00 03 F5 08 69 FF 89 01
         * 00 04
         */
        mplew.writeShort(SendPacketOpcode.REACTOR_HIT.getValue());
        mplew.writeInt(reactor.getObjectId());
        mplew.write(reactor.getState());
        mplew.writeShort(pos.x);
        mplew.writeShort(pos.y);
        mplew.writeShort(stance);
        mplew.write(0);
        mplew.write(7);
        return mplew.getPacket();
    }

    public static MaplePacket destroyReactor(MapleReactor reactor) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        Point pos = reactor.getPosition();


        mplew.writeShort(SendPacketOpcode.REACTOR_DESTROY.getValue());
        mplew.writeInt(reactor.getObjectId());
        mplew.write(reactor.getState());
        mplew.writeShort(pos.x);
        mplew.writeShort(pos.y);

        return mplew.getPacket();
    }

    public static MaplePacket musicChange(String song) {
        return environmentChange(song, 7);
    }

    public static MaplePacket showEffect(String effect) {
        return environmentChange(effect, 4);
    }

    public static MaplePacket playSound(String sound) {
        return environmentChange(sound, 5);
    }

    //BOSS血条
    public static MaplePacket environmentChange(String env, int mode) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        //1F 01 B9 00 00 00 08 00 01 0A 00 00 00
        mplew.writeShort(SendPacketOpcode.BOSS_ENV.getValue());
        mplew.write(mode);
        mplew.writeMapleAsciiString(env);

        return mplew.getPacket();
    }

    //开始地图效果
    public static MaplePacket startMapEffect(String msg, int itemid, boolean active) {

        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.MAP_EFFECT.getValue());
        mplew.write(active ? 0 : 1);

        mplew.writeInt(itemid);
        if (active) {
            mplew.writeMapleAsciiString(msg);
        }

        return mplew.getPacket();
    }

    public static MaplePacket removeMapEffect() {

        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.MAP_EFFECT.getValue());
        mplew.write(0);
        mplew.writeInt(0);

        return mplew.getPacket();
    }

    //家族
    private static void getGuildInfo(MaplePacketLittleEndianWriter mplew, MapleGuild guild) {

        mplew.writeInt(guild.getId());
        mplew.writeMapleAsciiString(guild.getName());
        for (int i = 1; i <= 5; i++) {
            mplew.writeMapleAsciiString(guild.getRankTitle(i));
        }
        Collection<MapleGuildCharacter> members = guild.getMembers();
        mplew.write(members.size());
        //then it is the size of all the members
        for (MapleGuildCharacter mgc : members) //and each of their character ids o_O
        {
            mplew.writeInt(mgc.getId());
        }
        for (MapleGuildCharacter mgc : members) {
            //mplew.writeAsciiString(StringUtil.getRightPaddedStr(mgc.getName(), '\0', 13));
            mplew.WriteOfMaxByteCountString(mgc.getName(), 13);
            mplew.writeInt(mgc.getJobId());
            mplew.writeInt(mgc.getLevel());
            mplew.writeInt(mgc.getGuildRank());
            mplew.writeInt(mgc.isOnline() ? 1 : 0);
            mplew.writeInt(guild.getSignature());
            mplew.writeInt(mgc.getAllianceRank());
        }
        mplew.writeInt(guild.getCapacity());
        mplew.writeShort(guild.getLogoBG());
        mplew.write(guild.getLogoBGColor());
        mplew.writeShort(guild.getLogo());
        mplew.write(guild.getLogoColor());
        mplew.writeMapleAsciiString(guild.getNotice());
        mplew.writeInt(guild.getGP());
        mplew.writeInt(guild.getAllianceId());
    }

    public static void addThread(MaplePacketLittleEndianWriter mplew, ResultSet rs) throws SQLException {

        mplew.writeInt(rs.getInt("localthreadid"));
        mplew.writeInt(rs.getInt("postercid"));
        mplew.writeMapleAsciiString(rs.getString("name"));
        mplew.writeLong(DateUtil.getFileTimestamp(rs.getLong("timestamp")));
        mplew.writeInt(rs.getInt("icon"));
        mplew.writeInt(rs.getInt("replycount"));
    }

    public static MaplePacket BBSThreadList(ResultSet rs, int start) throws SQLException {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();


        mplew.writeShort(SendPacketOpcode.BBS_OPERATION.getValue());
        mplew.write(0x06);
        if (!rs.last()) {
            //no result at all
            mplew.write(0);
            mplew.writeInt(0);
            mplew.writeInt(0);
            return mplew.getPacket();
        }
        int threadCount = rs.getRow();
        if (rs.getInt("localthreadid") == 0) { //has a notice
            mplew.write(1);
            addThread(mplew, rs);
            threadCount--; //one thread didn't count (because it's a notice)
        } else {
            mplew.write(0);
        }
        if (!rs.absolute(start + 1)) { //seek to the thread before where we start
            rs.first(); //uh, we're trying to start at a place past possible
            start = 0;
            // //log.debug("Attempting to start past threadCount");
        }
        mplew.writeInt(threadCount);
        mplew.writeInt(Math.min(10, threadCount - start));
        for (int i = 0; i < Math.min(10, threadCount - start); i++) {
            addThread(mplew, rs);
            rs.next();
        }

        return mplew.getPacket();
    }

    public static MaplePacket showThread(int localthreadid, ResultSet threadRS, ResultSet repliesRS) throws SQLException, RuntimeException {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();


        mplew.writeShort(SendPacketOpcode.BBS_OPERATION.getValue());
        mplew.write(0x07);
        mplew.writeInt(localthreadid);
        mplew.writeInt(threadRS.getInt("postercid"));
        mplew.writeLong(DateUtil.getFileTimestamp(threadRS.getLong("timestamp")));
        mplew.writeMapleAsciiString(threadRS.getString("name"));
        mplew.writeMapleAsciiString(threadRS.getString("startpost"));
        mplew.writeInt(threadRS.getInt("icon"));
        if (repliesRS != null) {
            int replyCount = threadRS.getInt("replycount");
            mplew.writeInt(replyCount);
            int i;
            for (i = 0; i < replyCount && repliesRS.next(); i++) {
                mplew.writeInt(repliesRS.getInt("replyid"));
                mplew.writeInt(repliesRS.getInt("postercid"));
                mplew.writeLong(DateUtil.getFileTimestamp(repliesRS.getLong("timestamp")));
                mplew.writeMapleAsciiString(repliesRS.getString("content"));
            }
            if (i != replyCount || repliesRS.next()) {
                //in the unlikely event that we lost count of replyid
                throw new RuntimeException(String.valueOf(threadRS.getInt("threadid")));
                /**
                 * we need to fix the database and stop the packet sending or
                 * else it'll probably error 38 whoever tries to read it there
                 * is ONE case not checked, and that's when the thread has a
                 * replycount of 0 and there is one or more replies to the
                 * thread in bbs_replies
                 */
            }
        } else {
            mplew.writeInt(0); //0 replies
        }
        return mplew.getPacket();
    }

    //技能效果
    public static MaplePacket skillEffect(MapleCharacter from, int skillId, int level, byte flags, int speed, int op) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        //D0 00 [B9 08 01 00] [0B 3D 42 00] [01] [C4] [00] [02]

        //F2 00 7A 1F 47 00 F1 62 F9 01 01 20 00 06
        mplew.writeShort(SendPacketOpcode.SKILL_EFFECT.getValue());
        ////log.debug("技能效果");
        mplew.writeInt(from.getId());
        mplew.writeInt(skillId);
        mplew.write(level);
        mplew.write(flags);
        mplew.write(speed);
        mplew.write(op);

        return mplew.getPacket();
    }

    //取消技能效果
    public static MaplePacket skillCancel(MapleCharacter from, int skillId) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        //F4 00 7A 1F 47 00 F1 62 F9 01
        mplew.writeShort(SendPacketOpcode.CANCEL_SKILL_EFFECT.getValue());
        ////log.debug("取消技能效果");
        mplew.writeInt(from.getId());
        mplew.writeInt(skillId);

        return mplew.getPacket();
    }

    //磁铁效果
    public static MaplePacket showMagnet(int mobid, byte success) { // Monster Magnet
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();


        mplew.writeShort(SendPacketOpcode.SHOW_MAGNET.getValue());
        mplew.writeInt(mobid);
        mplew.write(success);

        return mplew.getPacket();
    }

    /**
     * Sends a player hint.
     *
     * @param hint The hint it's going to send.
     * @param width How tall the box is going to be.
     * @param height How long the box is going to be.
     * @return The player hint packet.
     */
    //DF 00 [60 B5 6C 00 01 60] [18 23] [00 01] [01] //085
    public static MaplePacket sendHint(String hint, int width, int height) {

        if (width < 1) {
            width = hint.getBytes().length * 10;
            if (width < 40) {
                width = 40;
            }
        }
        if (height < 5) {
            height = 5;
        }
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.PLAYER_HINT.getValue());
        mplew.writeMapleAsciiString(hint);
        mplew.writeShort(width);
        mplew.writeShort(height);
        mplew.write(1);

        return mplew.getPacket();
    }

    //聊天招待
    public static MaplePacket messengerInvite(String from, int messengerid) {//招待邀请
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();


        mplew.writeShort(SendPacketOpcode.MESSENGER.getValue());
        mplew.write(0x03);
        mplew.writeMapleAsciiString(from);
        mplew.write(0x05);
        mplew.writeInt(messengerid);
        mplew.write(0x00);

        return mplew.getPacket();
    }

    public static MaplePacket addMessengerPlayer(String from, MapleCharacter chr, int position, int channel) {//招待添加玩家
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();


        mplew.writeShort(SendPacketOpcode.MESSENGER.getValue());
        mplew.write(0x00);
        mplew.write(position);
        addCharLook(mplew, chr, true);
        mplew.writeMapleAsciiString(from);
        mplew.write(channel);
        mplew.write(0x00);
        mplew.writeInt(0);

        return mplew.getPacket();
    }

    public static MaplePacket removeMessengerPlayer(int position) {//移除玩家(玩家离开)
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();


        mplew.writeShort(SendPacketOpcode.MESSENGER.getValue());
        mplew.write(0x02);
        mplew.write(position);

        return mplew.getPacket();
    }

    public static MaplePacket updateMessengerPlayer(String from, MapleCharacter chr, int position, int channel) {//更新招待玩家
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();


        mplew.writeShort(SendPacketOpcode.MESSENGER.getValue());
        mplew.write(0x07);
        mplew.write(position);
        addCharLook(mplew, chr, true);
        //   mplew.writeMapleAsciiString(from);
       /* mplew.write(channel);
         mplew.write(0x00);*/

        return mplew.getPacket();
    }

    public static MaplePacket joinMessenger(int position) {//加入招待
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.MESSENGER.getValue());
        mplew.write(0x01);
        mplew.write(position);

        return mplew.getPacket();
    }

    public static MaplePacket messengerChat(String user, String text) {//招待聊天
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();


        mplew.writeShort(SendPacketOpcode.MESSENGER.getValue());
        mplew.write(0x06);
        mplew.writeMapleAsciiString(text);
        mplew.writeMapleAsciiString(user);


        return mplew.getPacket();
    }

    public static MaplePacket messengerNote(String text, int mode, int mode2) {//招待说明
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();


        mplew.writeShort(SendPacketOpcode.MESSENGER.getValue());
        mplew.write(mode);
        mplew.writeMapleAsciiString(text);
        mplew.write(mode2);

        return mplew.getPacket();
    }

    public static MaplePacket warpCS(MapleClient c) {
        return getcashinfo(c, false);
    }

    public static MaplePacket warpMTS(MapleClient c) {
        return getcashinfo(c, true);
    }

    //进入商场拍卖1
    public static MaplePacket getcashinfo(MapleClient c, boolean MTS) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        MapleCharacter chr = c.getPlayer();
        mplew.writeShort(MTS ? SendPacketOpcode.MTS_OPEN.getValue() : SendPacketOpcode.CS_OPEN.getValue());

        mplew.writeLong(-1);
        mplew.writeZero(19);// 093增加
        addCharStats(mplew, chr);//角色信息
        mplew.write(chr.getBuddylist().getCapacity()); //最大好友14
        addInventoryInfo(mplew, chr);//背包信息
        mplew.writeZero(42);
        for (int i = 0; i < 28; i++) {
            mplew.writeInt(999999999); //代替缩地石A部分
        }
        mplew.writeShort(0); //代替缩地石B部分
        if (chr.getJob().getId() / 100 == 33) {
            mplew.write(50);
            mplew.write(new byte[20]);
        }
        mplew.writeZero(257);
        return mplew.getPacket();
    }

    public static MaplePacket getshopitemsinfo(MapleClient c, boolean MTS) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.CS_INTO.getValue());
        //   mplew.writeMapleAsciiString(c.getAccountName());
        if (MTS) {
            mplew.writeInt(0);
            mplew.write(HexTool.getByteArrayFromHexString("77 75 68 61 6F 88 13 00 00 07 00 00 00 2C 01 00 00 18 00 00 00 A8 00 00 00 C0 E8 EE 37 C0 E8 CB 01"));
        } else {
            mplew.write(Setting.getcashpacket());
        }

        //log.debug("warpCS2");
        return mplew.getPacket();
    }

    //现金装备
    public static void toCashItem(MaplePacketLittleEndianWriter mplew, int sn, int type1, int type2) {
        // E1 9C 98 00 00 06 00 00 00 - Globe Cap

        mplew.writeInt(sn);
        mplew.write(0);
        mplew.write(type1);
        mplew.writeShort(0);
        mplew.write(type2);
    }

    public static void toCashItem(MaplePacketLittleEndianWriter mplew, int sn, int type0, int type1, int type2) {

        mplew.writeInt(sn);
        mplew.write(type0);
        mplew.write(type1);
        mplew.writeShort(0);
        mplew.write(type2);
    }

    //商城更新点券和抵用券
    public static MaplePacket showNXMapleTokens(MapleCharacter chr) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();


        mplew.writeShort(SendPacketOpcode.CS_UPDATE.getValue());
        mplew.writeInt(chr.getCSPoints(0)); // 点券
        mplew.writeInt(chr.getCSPoints(1)); // 抵用券

        return mplew.getPacket();
    }

    //购买商城物品
    public static MaplePacket showBoughtCSItem(MapleClient c, MapleCSInventoryItem item) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.CS_OPERATION.getValue());
        mplew.write(0x67);//
        mplew.writeLong(item.getUniqueId());
        mplew.writeInt(c.getAccID());
        mplew.writeInt(0);
        mplew.writeInt(item.getItemId());//物品ID
        mplew.writeInt(item.getSn());
        mplew.writeShort(item.getQuantity());
        //mplew.writeZero(10);//00 07 9F BD 81 7C 00 00 00 00 
        mplew.writeMapleNameString(item.getSender());
        mplew.writeLong(item.getExpire() == null ? DateUtil.getFileTimestamp(FINAL_TIME) : DateUtil.getFileTimestamp(item.getExpire().getTime()));
        /*
         * 1E 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 01 01 AC 67 6B 00 00
         * 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 40 E0 FD 3B 37 4F 01
         * 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00
         */
        mplew.writeZero(32 + 6 + 24 + 4);
        return mplew.getPacket();
    }

    //购买任务物品
    public static MaplePacket showBoughtCSQuestItem(short position, int itemid) {
        // 3F 01 6F 01 00 00 00 01 00 09 00 D7 82 3D 00
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.CS_OPERATION.getValue());
        mplew.write(0x9b);//90
        mplew.writeInt(1);
        mplew.writeShort(1);
        mplew.writeShort(position);
        mplew.writeInt(itemid);
        return mplew.getPacket();
    }

    //抵用券购买的物品
    public static MaplePacket showCouponRedeemedItem(int itemid) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.CS_OPERATION.getValue());
        mplew.writeShort(0x43);//??3C
        mplew.writeInt(0);
        mplew.writeInt(1);
        mplew.writeShort(1);
        mplew.writeShort(0x1A);
        mplew.writeInt(itemid);//物品ID
        mplew.writeInt(0);
        return mplew.getPacket();
    }

    //商城送礼
    public static MaplePacket getGiftFinish(String name, int itemid, short amount) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.CS_OPERATION.getValue());
        mplew.write(0x6A);
        mplew.writeMapleAsciiString(name);
        mplew.writeInt(itemid);
        mplew.writeShort(amount);
        return mplew.getPacket();
    }

    public static MaplePacket showCannotToMe() { //显示 不能向自己的帐号赠送
        //23 01 3E A7 C0 62 00 00 00 00 00 01 FA 3C 00 00 00 00 00 CA 4A 0F 00 2B 2D 31 01 01 00 00 00 60 4B 40 00 F4 AC 77 00 00 00 00 00 CD 77 3C AB 25 CA 01 00 00 00 00 00 00 00 00
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.CS_OPERATION.getValue());
        mplew.write(0x6F);//5E
        mplew.write(0x06);//093
        return mplew.getPacket();
    }

    public static MaplePacket showCheckName() { //显示 检查角色名
        //23 01 3E A7 C0 62 00 00 00 00 00 01 FA 3C 00 00 00 00 00 CA 4A 0F 00 2B 2D 31 01 01 00 00 00 60 4B 40 00 F4 AC 77 00 00 00 00 00 CD 77 3C AB 25 CA 01 00 00 00 00 00 00 00 00
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.CS_OPERATION.getValue());
        mplew.write(0x6F);//5e
        mplew.write(0x07);//093
        return mplew.getPacket();
    }

    public static MaplePacket showNecklace_Expansion(int days) { //显示 购买项链扩充成功
        //40 02 6F 00 00 07 00
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.CS_OPERATION.getValue());
        mplew.write(0x78);
        mplew.writeShort(0);
        mplew.writeShort(days);
        return mplew.getPacket();
    }

    public static MaplePacket showCheckPartner() {//显示检查赠送戒指对象
        //85 02 97 1A
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.CS_OPERATION.getValue());
        mplew.write(0x9C);
        mplew.write(0x1A);
        return mplew.getPacket();
    }

    //发到道具到购物车 089ok
    public static MaplePacket sendWishList(int characterid) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        //7E 01 48 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00
        //9C 01 49 01 00 FE 9A 6F 01 00 00 00 00 DC F5 19 00 00 00 00 00 A7 A6 4F 00 03 CF 7B 05 01 00 C3 B0 CF D5 B5 BA D4 CB D3 AA D4 B1 00 00 80 05 BB 46 E6 17 02 00 00
        //00 00 00 00 00 00 04 00 06 00 00 00 00 00
        mplew.writeShort(SendPacketOpcode.CS_OPERATION.getValue());
        mplew.write(0x61);
        int i = 10;
        try {
            Connection con = DatabaseConnection.getConnection();
            PreparedStatement ps = con.prepareStatement("SELECT sn FROM wishlist WHERE charid = ? LIMIT 10");
            ps.setInt(1, characterid);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                mplew.writeInt(rs.getInt("sn"));
                i--;
            }
            rs.close();
            ps.close();
            con.close();
        } catch (SQLException se) {
            log.info("Error getting wishlist data:", se);
        }
        while (i > 0) {
            mplew.writeInt(0);
            i--;
        }
        return mplew.getPacket();
    }

    //更新购物车（删或增关注物品）089ok
    public static MaplePacket updateWishList(int characterid) {
        //9C 01 4D 46 9F 98 00 58 9F 98 00 57 9F 98 00 59 9F 98 00 20 4A CB 01 40 9F 98 00 CF 1F 9A 00 00 00 00 00 00 00 00 00 00 00 00 00
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.CS_OPERATION.getValue());
        mplew.write(0x65);
        int i = 10;
        try {
            Connection con = DatabaseConnection.getConnection();
            PreparedStatement ps = con.prepareStatement("SELECT sn FROM wishlist WHERE charid = ? LIMIT 10");
            ps.setInt(1, characterid);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                mplew.writeInt(rs.getInt("sn"));
                i--;
            }
            rs.close();
            ps.close();
            con.close();
        } catch (SQLException se) {
            log.info("Error getting wishlist data:", se);
        }

        while (i > 0) {
            mplew.writeInt(0);
            i--;
        }
        return mplew.getPacket();
    }

    //错误的兑换码
    public static MaplePacket wrongCouponCode() {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.CS_OPERATION.getValue());
        mplew.write(0x6B);//60
        mplew.write(0x00);

        return mplew.getPacket();
    }

    //<editor-fold defaultstate="collapsed" desc=" 寻找玩家相关 ">
    public static MaplePacket getFindReplyWithCS(String target, int mode) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.WHISPER.getValue());
        mplew.write(mode + 4);
        mplew.writeMapleAsciiString(target);
        mplew.write(2);
        mplew.writeInt(-1);
        return mplew.getPacket();
    }

    public static MaplePacket getFindReplyWithMap(String target, int mapid, int mode) {

        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.WHISPER.getValue());
        mplew.write(mode + 4);
        mplew.writeMapleAsciiString(target);
        mplew.write(1);
        mplew.writeInt(mapid);

        return mplew.getPacket();
    }

    public static MaplePacket getFindReply(String target, int channel, int mode) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.WHISPER.getValue());
        mplew.write(mode + 4);
        mplew.writeMapleAsciiString(target);
        mplew.write(3);
        mplew.writeInt(channel - 1);

        return mplew.getPacket();
    }
    //</editor-fold>

    //更新宠物ok
    public static MaplePacket updatePet(MaplePet pet) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        //if(show)//log.debug("调用的函数："+new Throwable().getStackTrace()[0]); //显示调用的类 函数名 函数所在行
        mplew.writeShort(SendPacketOpcode.MODIFY_INVENTORY_ITEM.getValue());
        mplew.write(0);
        mplew.write(2);
        mplew.write(0);
        mplew.write(3);

        mplew.write(5);
        mplew.writeShort(pet.getPosition());

        mplew.write(0);

        mplew.write(5);
        mplew.writeShort(pet.getPosition());

        addItemInfo(mplew, pet, true);
        /*
         * mplew.write(3); mplew.writeInt(pet.getItemId());//宠物ID
         * mplew.write(1); mplew.writeLong(pet.getUniqueId());
         * mplew.write(HexTool.getByteArrayFromHexString("00 80 05 BB 46 E6 17
         * 02")); mplew.writeInt(-1);//093新增
         * mplew.writeMapleNameString(pet.getName()); //自动填充到13位
         * mplew.write(pet.getLevel());//宠物等级
         * mplew.writeShort(pet.getCloseness());//亲密度
         * mplew.write(pet.getFullness());//饱满度
         * mplew.writeLong(DateUtil.getFileTimestamp(pet.getExpiration() == null
         * ? FINAL_TIME : pet.getExpiration().getTime())); mplew.writeShort(0);
         * mplew.write(HexTool.getByteArrayFromHexString("5F 00 00 00"));
         * mplew.writeInt(0); mplew.write(alive ? 1 : 0); mplew.writeInt(0);
         */

        return mplew.getPacket();
    }

    public static MaplePacket showPet(MapleCharacter chr, MaplePet pet, boolean remove) {
        return showPet(chr, pet, remove, false);
    }

    //显示宠物ok
    public static MaplePacket showPet(MapleCharacter chr, MaplePet pet, boolean remove, boolean hunger) {
        //log.debug("调用的函数：" + new Throwable().getStackTrace()[0]); //显示调用的类 函数名 函数所在行
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.SPAWN_PET.getValue());
        mplew.writeInt(chr.getId());
        mplew.write(chr.getPetSlot(pet));
        if (remove) {
            ////log.debug("取消宠物");
            mplew.writeInt(0);
            mplew.write(hunger ? 1 : 0);
        } else {
            ////log.debug("召唤宠物");
            mplew.writeInt(0x1000000); //093修改 原来都是发byte 简化一下
            mplew.write(0);
            mplew.writeInt(pet.getItemId());//宠物ID
            mplew.writeMapleAsciiString(pet.getName());//宠物名
            mplew.writeLong(pet.getUniqueid());
            mplew.writeShort(pet.getPos().x);//X
            mplew.writeShort(pet.getPos().y);//Y
            mplew.write(pet.getStance());//姿势
            mplew.writeShort(pet.getFh());
            mplew.writeInt(-1);
            mplew.writeShort(100);//109
        }

        return mplew.getPacket();
    }

    //宠物移动
    public static MaplePacket movePet(int cid, Point startPos, int slot, List<LifeMovementFragment> moves, int xb, int yb) {
        //if(show)//log.debug("调用的函数："+new Throwable().getStackTrace()[0]); //显示调用的类 函数名 函数所在行
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.MOVE_PET.getValue());
        mplew.writeInt(cid);
        mplew.writeInt(slot);
        mplew.writePos(startPos);
        serializeMovementList(mplew, moves);
        /*
         * if (mplew.size() == 99) { mplew.write(0); mplew.writeShort(xb);
         * mplew.writeShort(yb); mplew.writeInt(0);//97 mplew.write(1); }
         */
        return mplew.getPacket();
    }

    //宠物说话
    public static MaplePacket petChat(int cid, int slot, int act, String text) {
        //CC 00 [14 0D 04 00] [00] [00 00] [00 01] 01
        //E0 00 [37 3A 0A 00] [01 00 00 00] [01 00] 0F 00 CE D8 E0 E0 21 20 CE D8 E0 E0 E0 E0 E0 E0 21
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.PET_CHAT.getValue());
        mplew.writeInt(cid);
        mplew.writeInt(slot);
        mplew.writeShort(act);
        mplew.writeMapleAsciiString(text);

        return mplew.getPacket();
    }

    //宠物指令
    public static MaplePacket commandResponse(int cid, int slot, int animation, boolean success) {

        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.PET_COMMAND.getValue());
        mplew.writeInt(cid);
        /*
         * //log.debug("使用宠物指令"); mplew.write(slot); mplew.writeInt(0);
         * mplew.write(animation); mplew.write(0);
         */
        mplew.writeInt(slot);
        mplew.write(animation);
        mplew.write(success ? 1 : 0);
        mplew.writeInt(0);//?
        return mplew.getPacket();
    }

    public static MaplePacket showOwnPetLevelUp(int index) {

        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.SHOW_ITEM_GAIN_INCHAT.getValue());
        mplew.write(6);
        mplew.write(0);
        mplew.writeInt(index); // Pet Index

        return mplew.getPacket();
    }

    public static MaplePacket showPetLevelUp(MapleCharacter chr, int index) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();


        mplew.writeShort(SendPacketOpcode.SHOW_FOREIGN_EFFECT.getValue());
        mplew.writeInt(chr.getId());
        mplew.write(6);
        mplew.write(0);
        mplew.writeInt(index);

        return mplew.getPacket();
    }

    public static MaplePacket changePetName(MapleCharacter chr, String newname, int slot) {
        // 82 00 E6 DC 17 00 00 04 00 4A 65 66 66 00
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();


        mplew.writeShort(SendPacketOpcode.PET_NAMECHANGE.getValue());
        /*
         * //log.debug("宠物改名"+SendPacketOpcode.PET_NAMECHANGE.getValue());
         * mplew.writeInt(chr.getId()); mplew.write(0);
         * mplew.writeMapleAsciiString(newname); mplew.write(0);
         */
        mplew.writeInt(chr.getId());
        mplew.writeInt(slot);
        mplew.writeMapleAsciiString(newname);
        return mplew.getPacket();
    }

    public static MaplePacket showCharCash(MapleCharacter chr) {

        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.CHAR_CASH.getValue());
        mplew.writeInt(chr.getId());
        mplew.writeInt(chr.getCSPoints(1));

        return mplew.getPacket();
    }

    //地图装备效果
    public static MaplePacket showForcedEquip() {

        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.FORCED_MAP_EQUIP.getValue());
        //log.debug("地图装备效果");
        mplew.writeInt(0);
        return mplew.getPacket();
    }

    public static MaplePacket showMapEFFECTXG(String path) {

        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.MAP_EFFECTXG.getValue());
        //log.debug("地图效果");
        mplew.write(3);
        mplew.writeMapleAsciiString(path);
        return mplew.getPacket();
    }

    //召唤兽技能
    public static MaplePacket summonSkill(int cid, int summonSkillId, int newStance) {

        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.SUMMON_SKILL.getValue());
        mplew.writeInt(cid);
        mplew.writeInt(summonSkillId);
        mplew.write(newStance);

        return mplew.getPacket();
    }

    public static MaplePacket skillCooldown(int sid, int time) {

        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.COOLDOWN.getValue());
        mplew.writeInt(sid);
        mplew.writeInt(time);
        return mplew.getPacket();
    }

    //使用技能书
    public static MaplePacket skillBookSuccess(MapleCharacter chr, int skillid, int maxlevel, boolean canuse, boolean success) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        //36 00 [01 B8 DB 07] [00] 01 [6B 9F 2F 00] [14 00 00 00] 00 00

        mplew.writeShort(SendPacketOpcode.USE_SKILL_BOOK.getValue());
        mplew.write(1);//0
        mplew.writeInt(chr.getId()); // character id
        mplew.write(1);
        mplew.writeInt(skillid);//技能书ID
        mplew.writeInt(maxlevel);//最大等级
        mplew.write(canuse ? 1 : 0);
        mplew.write(success ? 1 : 0);

        return mplew.getPacket();
    }

    //技能宏
    public static MaplePacket getMacros(SkillMacro[] macros) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();


        mplew.writeShort(SendPacketOpcode.SKILL_MACRO.getValue());
        int count = 0;
        for (int i = 0; i < 5; i++) {
            if (macros[i] != null) {
                count++;
            }
        }
        mplew.write(count); //size
        for (int i = 0; i < 5; i++) {
            SkillMacro macro = macros[i];
            if (macro != null) {
                mplew.writeMapleAsciiString(macro.getName());//用宏技能时说的话
                mplew.write(macro.getShout());
                mplew.writeInt(macro.getSkill1());
                mplew.writeInt(macro.getSkill2());
                mplew.writeInt(macro.getSkill3());
            }
        }

        return mplew.getPacket();
    }

    //玩家NPC
    public static MaplePacket getPlayerNPC(int id) {

        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.PLAYER_NPC.getValue());
        Connection con = null;
        try {
            con = DatabaseConnection.getConnection();
            PreparedStatement ps = con.prepareStatement("SELECT * FROM playernpcs WHERE id = ?");
            ps.setInt(1, id);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                mplew.write(rs.getByte("dir"));
                mplew.writeInt(id);
                mplew.writeMapleAsciiString(rs.getString("name"));
                mplew.write(0);
                mplew.write(rs.getByte("skin"));
                mplew.writeInt(rs.getInt("face"));
                mplew.write(0);
                mplew.writeInt(rs.getInt("hair"));
            }
            rs.close();
            ps.close();
        } catch (Exception e) {
        }
        try {
            PreparedStatement ps = con.prepareStatement("SELECT * FROM playernpcs_equip WHERE npcid = ? AND type = 0");
            ps.setInt(1, id);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                mplew.write(rs.getByte("equippos"));
                mplew.writeInt(rs.getInt("equipid"));
            }
            rs.close();
            ps.close();
        } catch (Exception e) {
        }
        mplew.writeShort(-1);
        int count = 0;
        try {
            PreparedStatement ps = con.prepareStatement("SELECT * FROM playernpcs_equip WHERE npcid = ? AND type = 1");
            ps.setInt(1, id);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                mplew.writeInt(rs.getInt("equipid"));
                count += 1;
            }
            rs.close();
            ps.close();
            con.close();
        } catch (Exception e) {
        }
        while (count < 4) {
            mplew.writeInt(0);
            count += 1;
        }
        return mplew.getPacket();
    }

    //小纸条
    public static MaplePacket showNotes(ResultSet notes, int count) throws SQLException {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();


        mplew.writeShort(SendPacketOpcode.SHOW_NOTES.getValue());
        mplew.write(3);
        mplew.write(count);
        for (int i = 0; i < count; i++) {
            mplew.writeInt(notes.getInt("id"));
            mplew.writeMapleAsciiString(notes.getString("from"));
            mplew.writeMapleAsciiString(notes.getString("message"));
            mplew.writeLong(DateUtil.getFileTimestamp(notes.getLong("timestamp")));
            mplew.write(0);
            notes.next();
        }

        return mplew.getPacket();
    }

    public static void sendUnkwnNote(String to, String msg, String from) throws SQLException {
        Connection con = DatabaseConnection.getConnection();

        PreparedStatement ps = con.prepareStatement("INSERT INTO notes (`to`, `from`, `message`, `timestamp`) VALUES (?, ?, ?, ?)");
        ps.setString(1, to);
        ps.setString(2, from);
        ps.setString(3, msg);
        ps.setLong(4, System.currentTimeMillis());
        ps.executeUpdate();
        ps.close();
        con.close();
    }

    public static MaplePacket updateAriantPQRanking(String name, int score, boolean empty) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();


        mplew.writeShort(SendPacketOpcode.ARIANT_PQ_START.getValue());
        //E9 00 pid
        //01 unknown
        //09 00 53 69 6E 50 61 74 6A 65 68 maple ascii string name
        //00 00 00 00 score
        mplew.write(empty ? 0 : 1);
        if (!empty) {
            mplew.writeMapleAsciiString(name);
            mplew.writeInt(score);
        }

        return mplew.getPacket();
    }

    //抓怪物
    public static MaplePacket catchMonster(int mobid, int itemid, byte success) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();


        mplew.writeShort(SendPacketOpcode.CATCH_MONSTER.getValue());
        //BF 00
        //38 37 2B 00 mob id
        //32 A3 22 00 item id
        //00 success??
        mplew.writeInt(mobid);
        mplew.writeInt(itemid);
        mplew.write(success);

        return mplew.getPacket();
    }

    //显示所有的角色
    public static MaplePacket showAllCharacter(int chars, int unk) {

        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.ALL_CHARLIST.getValue());
        mplew.write(1);
        mplew.writeInt(chars);
        mplew.writeInt(unk);
        return mplew.getPacket();
    }

    //显示所有的角色信息
    public static MaplePacket showAllCharacterInfo(int worldid, List<MapleCharacter> chars) {

        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.ALL_CHARLIST.getValue());
        mplew.write(0);
        mplew.write(worldid);
        mplew.write(chars.size());
        for (MapleCharacter chr : chars) {
            addCharEntry(mplew, chr);
        }
        return mplew.getPacket();
    }

    //用户说话黑板
    public static MaplePacket useChalkboard(MapleCharacter chr, boolean close) {


        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.CHALKBOARD.getValue());
        mplew.writeInt(chr.getId());
        if (close) {
            mplew.write(0);
        } else {
            mplew.write(1);
            mplew.writeMapleAsciiString(chr.getChalkboard());
        }

        return mplew.getPacket();
    }

    public static MaplePacket removeItemFromDuey(boolean remove, int Package) {

        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.DUEY.getValue());
        mplew.write(0x17);
        mplew.writeInt(Package);
        mplew.write(remove ? 3 : 4);
        return mplew.getPacket();
    }

    public static MaplePacket sendDueyMessage(byte operation) {
        return sendDuey(operation, null);
    }

    //送货员
    public static MaplePacket sendDuey(byte operation, List<MapleDueyActions> packages) {

        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.DUEY.getValue());
        mplew.write(10);
        if (operation == 9) {
            mplew.write(0);
            mplew.write(packages.size());
            for (MapleDueyActions dp : packages) {
                mplew.writeInt(dp.getPackageId());
                /*
                 * mplew.writeAsciiString(dp.getSender()); for (int i =
                 * dp.getSender().getBytes().length; i < 13; i++) {
                 * mplew.write(0); }
                 */
                mplew.writeMapleNameString(dp.getSender()); //自动填充到13位

                mplew.writeInt(dp.getMesos());
                mplew.writeLong(DateUtil.getFileTimestamp(dp.sentTimeInMilliseconds()));
                mplew.writeLong(0);
                for (int i = 0; i < 48; i++) { //message is supposed to be here...
                    mplew.writeInt(new Random().nextInt(Integer.MAX_VALUE));
                }
                mplew.writeInt(0);
                mplew.write(0);
                if (dp.getItem() != null) {
                    mplew.write(1);
                    addItemInfo(mplew, dp.getItem(), true, true, false);
                } else {
                    mplew.write(0);
                }
            }
            mplew.write(0);
        }
        return mplew.getPacket();
    }

    public static MaplePacket sendMTS(List<MTSItemInfo> items, int tab, int type, int page, int pages) {

        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.MTS_OPERATION.getValue());
        mplew.write(0x14); //operation
        mplew.writeInt(pages * 10);
        mplew.writeInt(items.size()); //number of items
        mplew.writeInt(tab);
        mplew.writeInt(type);
        mplew.writeInt(page);
        mplew.write(1);
        mplew.write(1);
        for (int i = 0; i < items.size(); i++) {
            MTSItemInfo item = items.get(i);
            addItemInfo(mplew, item.getItem(), true, true, false);
            mplew.writeInt(item.getID()); //id
            mplew.writeInt(item.getTaxes()); //this + below = price
            mplew.writeInt(item.getPrice()); //price
            mplew.writeInt(0);
            mplew.writeLong(DateUtil.getFileTimestamp(item.getEndingDate()));
            mplew.writeMapleAsciiString(item.getSeller()); //account name (what was nexon thinking?)
            mplew.writeMapleAsciiString(item.getSeller()); //char name
            for (int j = 0; j < 28; j++) {
                mplew.write(0);
            }
        }
        mplew.write(1);

        return mplew.getPacket();
    }

    public static MaplePacket showMTSCash(MapleCharacter p) {
        //16 01 00 00 00 00 00 00 00 00

        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.MTS_OPERATION2.getValue());
        mplew.writeInt(p.getCSPoints(1));
        return mplew.getPacket();
    }

    public static MaplePacket MTSWantedListingOver(int nx, int items) {
        //Listing period on [WANTED] items

        //(just a message stating you have gotten your NX/items back, only displays if nx or items != 0)
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.MTS_OPERATION.getValue());
        mplew.write(0x3A);
        mplew.writeInt(nx);
        mplew.writeInt(items);
        mplew.writeLong(0);
        return mplew.getPacket();
    }

    public static MaplePacket MTSConfirmSell() {

        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.MTS_OPERATION.getValue());
        mplew.write(0x1D);
        return mplew.getPacket();
    }

    public static MaplePacket MTSConfirmBuy() {

        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.MTS_OPERATION.getValue());
        mplew.write(0x33);
        return mplew.getPacket();
    }

    public static MaplePacket MTSFailBuy() {

        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.MTS_OPERATION.getValue());
        mplew.write(0x34);
        mplew.write(0x42);
        return mplew.getPacket();
    }

    public static MaplePacket MTSConfirmTransfer(int quantity, int pos) {

        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.MTS_OPERATION.getValue());
        mplew.write(0x27);
        mplew.writeInt(quantity);
        mplew.writeInt(pos);
        return mplew.getPacket();
    }

    public static MaplePacket NotYetSoldInv(List<MTSItemInfo> items) {

        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.MTS_OPERATION.getValue());
        mplew.write(32);
        mplew.writeInt(items.size());
        if (!items.isEmpty()) {
            for (MTSItemInfo item : items) {
                addItemInfo(mplew, item.getItem(), true, true, false);
                mplew.writeInt(item.getID()); //id
                mplew.writeInt(item.getTaxes()); //this + below = price
                mplew.writeInt(item.getPrice()); //price
                mplew.writeInt(0);
                mplew.writeLong(DateUtil.getFileTimestamp(item.getEndingDate()));
                mplew.writeMapleAsciiString(item.getSeller()); //account name (what was nexon thinking?)
                mplew.writeMapleAsciiString(item.getSeller()); //char name
                for (int j = 0; j < 28; j++) {
                    mplew.write(0);
                }
            }
        } else {
            mplew.writeInt(0);
        }

        return mplew.getPacket();
    }

    public static MaplePacket TransferInventory(List<MTSItemInfo> items) {

        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.MTS_OPERATION.getValue());
        mplew.write(30);
        mplew.writeInt(items.size());
        if (!items.isEmpty()) {
            for (MTSItemInfo item : items) {
                addItemInfo(mplew, item.getItem(), true, true, false);
                mplew.writeInt(item.getID()); //id
                mplew.writeInt(item.getTaxes()); //taxes
                mplew.writeInt(item.getPrice()); //price
                mplew.writeInt(0);
                mplew.writeLong(DateUtil.getFileTimestamp(item.getEndingDate()));
                mplew.writeMapleAsciiString(item.getSeller()); //account name (what was nexon thinking?)
                mplew.writeMapleAsciiString(item.getSeller()); //char name
                for (int i = 0; i < 28; i++) {
                    mplew.write(0);
                }
            }
        }
        mplew.write(0xD0 + items.size());
        mplew.write(HexTool.getByteArrayFromHexString("FF FF FF 00"));

        return mplew.getPacket();
    }

    public static MaplePacket showZakumShrineTimeLeft(int timeleft) {

        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.ZAKUM_SHRINE.getValue());
        mplew.write(0);
        mplew.writeInt(timeleft);

        return mplew.getPacket();
    }

    //船效果
    public static MaplePacket boatPacket(int effect) {

        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.BOAT_EFFECT.getValue());
        mplew.writeShort(effect); //1034: balrog boat comes, 1548: boat comes in ellinia station, 520: boat leaves ellinia station
        return mplew.getPacket();
    }

    //开始怪物嘉年华
    public static MaplePacket startMonsterCarnival(int team) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.MONSTER_CARNIVAL_START.getValue());
        mplew.write(team);
        mplew.write(HexTool.getByteArrayFromHexString("00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00"));
        return mplew.getPacket();
    }

    //玩家死亡信息
    public static MaplePacket playerDiedMessage(String name, int lostCP, int team) { //CPQ
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.MONSTER_CARNIVAL_DIED.getValue());
        mplew.write(team); //team
        mplew.writeMapleAsciiString(name);
        mplew.write(lostCP);
        return mplew.getPacket();
    }

    public static MaplePacket CPUpdate(boolean party, int curCP, int totalCP, int team) { //CPQ
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        if (!party) {
            mplew.writeShort(SendPacketOpcode.MONSTER_CARNIVAL_OBTAINED_CP.getValue());
        } else {
            mplew.writeShort(SendPacketOpcode.MONSTER_CARNIVAL_PARTY_CP.getValue());
            mplew.write(team); //team?
        }
        mplew.writeShort(curCP);
        mplew.writeShort(totalCP);
        return mplew.getPacket();
    }

    //玩家传送
    public static MaplePacket playerSummoned(String name, int tab, int number) {
        //E5 00
        //02 tabnumber
        //04 number
        //09 00 57 61 72 50 61 74 6A 65 68 name

        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.MONSTER_CARNIVAL_SUMMON.getValue());
        mplew.write(tab);
        mplew.write(number);
        mplew.writeMapleAsciiString(name);
        return mplew.getPacket();
    }

    //缩地石
    public static MaplePacket TrockRefreshMapList(MapleCharacter chr, byte rocktype) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.TROCK_LOCATIONS.getValue());
        mplew.write(3);
        mplew.write(rocktype);
        List<Integer> maps = chr.getTRockMaps(rocktype);
        for (int map : maps) {
            mplew.writeInt(map);
        }
        for (int i = maps.size(); i <= ((rocktype == 1) ? 5 : 10); i++) {//暂时加载10个吧。
            mplew.write(CHAR_INFO_MAGIC);
        }
        maps.clear();

        return mplew.getPacket();
    }

//    //能量获得
//    public static MaplePacket giveEnergyCharge(int barammount) {
//        
//        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
//        mplew.writeShort(SendPacketOpcode.GIVE_BUFF.getValue());
//        mplew.writeInt(0);
//        mplew.writeLong(MapleBuffStat.能量获得.getValue());
//        mplew.writeShort(0);
//        mplew.writeInt(0);
//        mplew.writeShort(barammount);
//        mplew.writeShort(0);
//        mplew.writeLong(0);
//        mplew.write(0);
//        mplew.writeInt(50);
//        return mplew.getPacket();
//    }
//    public static MaplePacket giveForeignEnergyCharge(int cid, int barammount) {
//        
//        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
//        mplew.writeShort(SendPacketOpcode.GIVE_FOREIGN_BUFF.getValue());
//        mplew.writeInt(cid);
//        mplew.writeLong(0);
//        mplew.writeLong(MapleBuffStat.能量获得.getValue());
//        mplew.writeShort(0);
//        mplew.writeShort(barammount);
//        mplew.writeShort(0);
//        mplew.writeLong(0);
//        mplew.writeShort(0);
//        mplew.writeShort(0);
//        return mplew.getPacket();
//    }
    //配偶聊天
    public static MaplePacket spouseChat(String from, String message, int type) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.SPOUSE_CHAT.getValue());
        mplew.write(type);
        if (type == 4) {
            mplew.write(1);
        } else {
            mplew.writeMapleAsciiString(from);
            mplew.write(5);
        }
        mplew.writeMapleAsciiString(message);
        return mplew.getPacket();
    }

    /**
     * Adds a announcement box to an existing MaplePacketLittleEndianWriter.
     *
     * @param mplew The MaplePacketLittleEndianWriter to add an announcement box
     * to.
     * @param shop The shop to announce.
     */
    private static void addAnnounceBox(MaplePacketLittleEndianWriter mplew, IPlayerInteractionManager interaction) {

        mplew.write(4);
        //mplew.write(0);
        //mplew.write(1);
        //mplew.write(1);
        if (interaction.getShopType() == 2) {
            mplew.writeInt(((MaplePlayerShop) interaction).getObjectId());
        }
        mplew.writeMapleAsciiString(interaction.getDescription()); // 递减
        //mplew.write(HexTool.getByteArrayFromHexString("8E 80"));
        mplew.write(0);
        mplew.write(interaction.getItemType());
        mplew.write(1);
        mplew.write(interaction.getFreeSlot() > -1 ? 4 : 1);

        if (interaction.getShopType() == 2) {
            mplew.write(0);
        }
    }

    //发送玩家交互
    public static MaplePacket sendInteractionBox(MapleCharacter c) {

        //BA 00 [46 08 01 00] [00 01 01] [45 4B 4C 00] [08 00 B0 D7 C9 AB CD C3 D7 D3] [8E 80] [01] 00 00 00 00 00 E1 00 22 00 04 58 00 00 00
        //BA 00 [91 C2 00 00] [00 01 01] [64 4B 4C 00] [08 00 D8 BC D1 C5 C3 EF B5 FB] [81 01] [00] 00 00 00 00 00 ED 01 04 00 04 42 00 00 00
        //BA 00 [25 81 00 00] [00 01 01] [78 4B 4C 00] [06 00 BE DE D7 EC C4 F1]       [95 3E] [00] 00 00 00 00 00 2E 02 04 00 16 3D 00 00 00
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.UPDATE_CHAR_BOX.getValue());
        mplew.writeInt(c.getId());
        addAnnounceBox(mplew, c.getInteraction());
        return mplew.getPacket();
    }

    //雇佣商店
    public static MaplePacket hiredMerchantBox(MapleCharacter chr, int btyes) {

        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.SPAWN_HIRED.getValue());
        mplew.write(btyes);
        return mplew.getPacket();
    }

    public static MaplePacket getInteraction(MapleCharacter chr, boolean firstTime) {

        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.PLAYER_INTERACTION.getValue()); // header.
        IPlayerInteractionManager ips = chr.getInteraction();
        int type = ips.getShopType();
        if (type == 1) {//雇佣商店 的type是1  是程序内设的type
            mplew.writeSome(0xB, 0x6, 0x7);
        } else if (type == 2) {
            mplew.write(HexTool.getByteArrayFromHexString("0A 04 04"));//05 04 04
        } else if (type == 3) {
            mplew.write(HexTool.getByteArrayFromHexString("0A 02 02"));//05 02 02
        } else if (type == 4) {
            mplew.write(HexTool.getByteArrayFromHexString("0A 01 02"));//05 01 02
        }
        mplew.writeShort(ips.getSlot(chr));
        if (type == 2 || type == 3 || type == 4) {
            addCharLook(mplew, ((MaplePlayerShop) ips).getMCOwner(), false);
            mplew.writeMapleAsciiString(ips.getOwnerName());
        } else {
            mplew.writeInt(((HiredMerchant) ips).getItemId());
            mplew.writeMapleAsciiString("雇佣商人");
        }
        for (int i = 0; i < ips.getCapacity(); i++) {
            if (ips.getVisitors()[i] != null) {
                mplew.write(i + 1);
                addCharLook(mplew, ips.getVisitors()[i], false);
                mplew.writeMapleAsciiString(ips.getVisitors()[i].getName());
                mplew.writeShort(ips.getVisitors()[i].getJobid());
            }
        }
        mplew.write(-1);
        if (type == 1) {
            mplew.writeShort(0);
            mplew.writeMapleAsciiString(ips.getOwnerName());
            if (ips.isOwner(chr)) {
                mplew.writeInt(((HiredMerchant) ips).getTimeLeft()); // contains timing, suck my dick we dont need this
                mplew.write(firstTime ? 1 : 0);
                mplew.writeZero(9);//这里有8个字节代码营业额
            }
        }
        mplew.writeInt(0);//?106
        mplew.writeMapleAsciiString(ips.getDescription());
        if (type == 3) {
            mplew.write(ips.getItemType());
            mplew.write(0);
        } else {
            mplew.write(0x10);
            if (type == 1) {
                HiredMerchant hiredMerchant = (HiredMerchant) ips;
                mplew.writeLong(hiredMerchant.getTurnover());//8字节交易额
            }
            mplew.write(ips.getItems().size());
            if (ips.getItems().isEmpty()) {
                if (type != 1) {
                    mplew.writeInt(0);
                }
            } else {
                for (MaplePlayerShopItem item : ips.getItems()) {
                    mplew.writeShort(item.getBundles());
                    mplew.writeShort(item.getItem().getQuantity());
                    mplew.writeLong(item.getPrice());
                    addItemInfo(mplew, item.getItem(), true);
                }
            }
            mplew.writeShort(0);//?
        }
        return mplew.getPacket();
    }

    //商店聊天
    public static MaplePacket shopChat(String message, int slot) {

        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.PLAYER_INTERACTION.getValue());
        mplew.writeSome(0x0F, 0x10);
        mplew.write(slot);
        mplew.writeMapleAsciiString(message);
        return mplew.getPacket();
    }

    public static MaplePacket shopErrorMessage(int error, int type) {

        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.PLAYER_INTERACTION.getValue());
        mplew.write(PlayerInteractionHandler.Action.EXIT.getCode());
        mplew.write(type);
        mplew.write(error);
        return mplew.getPacket();
    }

    public static MaplePacket closeshop() {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.PLAYER_INTERACTION.getValue());
        mplew.write(0x21);
        mplew.write(0);
        return mplew.getPacket();
    }

    public static MaplePacket spawnHiredMerchant(HiredMerchant hm) {

        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.SPAWN_HIRED_MERCHANT.getValue());
        mplew.writeInt(hm.getOwnerId());
        mplew.writeInt(hm.getItemId());
        mplew.writeShort((short) hm.getPosition().getX());
        mplew.writeShort((short) hm.getPosition().getY());
        mplew.writeShort(0);
        mplew.writeMapleAsciiString(hm.getOwnerName());
        mplew.write(0x06);//雇佣商店类型
        mplew.writeInt(hm.getObjectId());
        mplew.writeMapleAsciiString(hm.getDescription());
        mplew.write(hm.getItemId() % 100);//hm.getItemId() % 10
        mplew.write(HexTool.getByteArrayFromHexString("01 04"));//01 04
        return mplew.getPacket();
    }

    public static MaplePacket destroyHiredMerchant(int id) {

        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.DESTROY_HIRED_MERCHANT.getValue());
        mplew.writeInt(id);
        return mplew.getPacket();
    }

    public static MaplePacket shopItemUpdate(IPlayerInteractionManager shop) {

        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.PLAYER_INTERACTION.getValue());
        mplew.write(0x30);// v77 = 0x15 //v78 = 0x17//v98//v106+1
        if (shop.getShopType() == 1) {
            mplew.writeLong(0);//雇佣商店金币收入
        }
        mplew.write(shop.getItems().size());
        for (MaplePlayerShopItem item : shop.getItems()) {
            mplew.writeShort(item.getBundles());
            mplew.writeShort(item.getItem().getQuantity());
            mplew.writeLong(item.getPrice());
            addItemInfo(mplew, item.getItem(), true);
        }
        mplew.writeShort(0);
        return mplew.getPacket();
    }

    public static MaplePacket getBannedList(PlayerInteractionManager shop) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.PLAYER_INTERACTION.getValue());
        mplew.write(PlayerInteractionHandler.Action.BAN_PLAYER.getCode());
        mplew.writeShort(shop.BannedList().size());
        for (String string : shop.BannedList()) {
            mplew.writeMapleAsciiString(string);
        }

        return mplew.getPacket();
    }

    public static MaplePacket shopVisitorAdd(MapleCharacter chr, int slot) { //玩家进入SHOP

        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.PLAYER_INTERACTION.getValue());
        mplew.write(0x09);
        mplew.write(slot);//slot
        addCharLook(mplew, chr, false);
        mplew.writeMapleAsciiString(chr.getName());
        mplew.writeShort(chr.getJobid());
        return mplew.getPacket();
    }

    public static MaplePacket shopVisitorLeave(int slot) { //玩家离开SHOP

        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.PLAYER_INTERACTION.getValue());
        mplew.write(PlayerInteractionHandler.Action.EXIT.getCode());//0a
        mplew.write(slot);
        if (slot == 0) {
            mplew.write(0x14);//11
        }

        return mplew.getPacket();
    }

    public static MaplePacket updateHiredMerchant(HiredMerchant shop) {

        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.UPDATE_HIRED_MERCHANT.getValue());
        mplew.writeInt(shop.getOwnerId());
        mplew.write(0x04);
        mplew.writeInt(shop.getObjectId());
        mplew.writeMapleAsciiString(shop.getDescription());
        mplew.write(shop.getItemId() % 100);
        mplew.write(shop.getFreeSlot() > -1 ? 3 : 2);
        mplew.write(0x04);

        return mplew.getPacket();
    }

    public static MaplePacket getMiniBoxFull() { // 满了？

        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.PLAYER_INTERACTION.getValue());
        mplew.writeShort(PlayerInteractionHandler.Action.ENTER_RESPOSE.getCode());
        mplew.write(2);
        return mplew.getPacket();
    }

    public static MaplePacket getMiniBoxfailenter() { // 无法进入 - 黑名单
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.PLAYER_INTERACTION.getValue());
        mplew.writeShort(PlayerInteractionHandler.Action.ENTER_RESPOSE.getCode());
        mplew.write(0x11);
        return mplew.getPacket();
    }

    public static MaplePacket sendMaplePolice(int reason, String reasoning, int duration) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.GM_POLICE.getValue());
        mplew.writeInt(duration);
        mplew.write(4);
        mplew.write(reason);
        mplew.writeMapleAsciiString(reasoning);
        return mplew.getPacket();
    }

    //更新装备栏
    public static MaplePacket updateEquipSlot(IItem item) {
        return updateEquipSlot(item, false);
    }

    public static MaplePacket updateEquipSlot(IItem item, boolean hourGlass) {

        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.MODIFY_INVENTORY_ITEM.getValue());
        mplew.write(HexTool.getByteArrayFromHexString("00 02 00 03"));
        mplew.write(item.getType());
        mplew.writeShort(item.getPosition());
        mplew.write(0);
        mplew.write(item.getType());
        mplew.writeShort(item.getPosition());
        addItemInfo(mplew, item, true);
        if (hourGlass) {
            mplew.write(2);
        }
        ////log.debug("更新包："+mplew.toString());
        return mplew.getPacket();
    }

    //龙神 召唤龙
    public static MaplePacket SummonDragon(MapleCharacter chr, int oid) {
        return spawnDragon(chr);
    }

    public static MaplePacket updateBattleShipHP(int chr, int hp) {

        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.SHOW_MONSTER_HP.getValue());
        mplew.writeInt(chr);
        mplew.write(hp);

        return mplew.getPacket();
    }

    public static MaplePacket updateMount(int charid, MapleMount mount, boolean levelup) {
        return updateMount(charid, mount.getLevel(), mount.getExp(), mount.getTiredness(), levelup);
    }

    public static MaplePacket updateMount(int charid, int newlevel, int newexp, int tiredness, boolean levelup) {

        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.UPDATE_MOUNT.getValue());
        mplew.writeInt(charid);
        mplew.writeInt(newlevel);
        mplew.writeInt(newexp);
        mplew.writeInt(tiredness);
        mplew.write(levelup ? (byte) 1 : (byte) 0);
        return mplew.getPacket();
    }

    //将商城的道具放到背包里
    public static MaplePacket transferFromCSToInv(IItem item, int position) {

        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.CS_OPERATION.getValue());
        mplew.write(0x7A);//6E
        mplew.writeShort(position);//in csinventory
        addNormalItemInfo(mplew, item, false, false, true, true);
        mplew.writeInt(0);//97
        return mplew.getPacket();
    }

    //将背包的道具放到商城的道具栏
    public static MaplePacket transferFromInvToCS(MapleCharacter c, MapleCSInventoryItem item) {

        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.CS_OPERATION.getValue());
        mplew.write(0x7C);//70
        mplew.writeLong(item.getUniqueId());//Un编号
        mplew.writeInt(c.getAccountid());
        mplew.writeInt(0);
        mplew.writeInt(item.getItemId());//道具ID
        mplew.writeInt(item.getSn());
        mplew.writeShort(item.getQuantity());
        mplew.writeMapleNameString(item.getSender()); //自动填充到13位
        mplew.writeLong(item.getExpire() == null ? DateUtil.getFileTimestamp(FINAL_TIME) : DateUtil.getFileTimestamp(item.getExpire().getTime()));
        /**
         * 0F 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00
         * 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 80 05 BB 46 E6 17 02
         * 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00
         */
        mplew.writeZero(30 + 32);
        return mplew.getPacket();
    }

    //获取商城的道具信息（这个应该是更新商场道具栏里面的）
    public static MaplePacket getCSInventory(MapleCharacter chr) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.CS_OPERATION.getValue());
        mplew.write(0x5C); //5
        mplew.write();
        MapleCSInventory csinv = chr.getCSInventory();
        List<MapleCSInventoryItem> allitems = csinv.GetAllCSInventoryItems();
        mplew.writeShort(allitems.size());//商场道具栏物品的个数
        for (MapleCSInventoryItem citem : allitems) {
            mplew.writeLong(citem.getUniqueId());
            mplew.writeInt(chr.getAccountid());
            mplew.writeInt(0);
            mplew.writeInt(citem.getItemId());//物品ID
            mplew.writeInt(citem.getSn());
            mplew.writeShort(citem.getQuantity());
            mplew.writeMapleNameString(citem.getSender()); //自动填充到13位
            mplew.writeLong(citem.getExpire() == null ? DateUtil.getFileTimestamp(FINAL_TIME) : DateUtil.getFileTimestamp(citem.getExpire().getTime()));
            mplew.writeLong(0);
            mplew.writeLong(0); //093新增
            mplew.writeZero(3);//
            mplew.writeZero(8);
            mplew.writeZero(3);
            mplew.writeZero(32);

        }
        mplew.writeInt(0); //093新增
        mplew.writeShort(chr.getStorage().getSlots());
        mplew.writeShort(chr.getClient().getMaxCharSlot());
        mplew.writeShort(0);//97
        mplew.writeShort(2);//97
        //mplew.writeInt(0x100);//093修改
        return mplew.getPacket();
    }

    //获取礼物（这个应该是更新商场道具栏里面的）
    public static MaplePacket getCSGifts(MapleCharacter chr) {
        /**
         * 28 02 55 01 00 * AC 4C D7 00 00 00 00 00 7B C0 4C 00 4E 61 6D 65 43
         * 6F 6C 6C 00 00 00 00 00 CE D2 B0 AE C4 E3 0A 00 00 00 00 00 00 * 00
         * 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00
         * 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00
         * 00 00 00 00 00 00 00 00 00 00 00 00 00
         */
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.CS_OPERATION.getValue());
        mplew.write(0x5F);//4f
        Collection<MapleCSInventoryItem> inv = chr.getCSInventory().getCSGifts().values();
        mplew.writeShort(inv.size());
        for (MapleCSInventoryItem gift : inv) {
            mplew.writeLong(gift.getUniqueId());
            mplew.writeInt(gift.getItemId());//ID
            mplew.writeMapleNameString(gift.getSender()); //自动填充到13位
            mplew.WriteOfMaxByteCountString(gift.getMessage(), 30); //自动填充到30位
            mplew.writeZero(43);
        }
        return mplew.getPacket();
    }

    public static MaplePacket showCygnusIntro(int id) {

        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.Animation_EFFECT.getValue());
        mplew.write(0x14);
        mplew.writeMapleAsciiString("Effect/Direction.img/cygnus/Scene" + id);
        return mplew.getPacket();
    }

    public static MaplePacket showCygnusIntro_3(int id) {

        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.Animation_EFFECT.getValue());
        mplew.write(0x14);
        mplew.writeMapleAsciiString("Effect/Direction3.img/cygnus/Scene" + 0);
        return mplew.getPacket();
    }

    public static MaplePacket CygnusIntroLock(boolean enable) {

        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.CYGNUS_INTRO_LOCK.getValue());
        mplew.write(enable ? 1 : 0);
        return mplew.getPacket();
    }

    public static MaplePacket CygnusIntroDisableUI(boolean enable) {

        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.CYGNUS_INTRO_DISABLE_UI.getValue());
        mplew.write(enable ? 1 : 0);
        return mplew.getPacket();
    }

    public static MaplePacket sendCygnusMessage(int type) {

        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.CYGNUS_CHAR_CREATED.getValue());
        mplew.writeInt(type);
        return mplew.getPacket();
    }

    public static MaplePacket boatPacket(boolean type) {


        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.BOAT_EFFECT.getValue());
        mplew.writeShort(type ? 1 : 2);

        return mplew.getPacket();
    }

    public static MaplePacket trembleEffect(int type, int delay) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.BOSS_ENV.getValue());
        mplew.write(2);
        mplew.write(type);
        mplew.writeInt(delay);

        return mplew.getPacket();
    }

    public static MaplePacket getEnergy(int level) {

        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.ENERGY.getValue());
        mplew.writeMapleAsciiString("energy");
        mplew.writeMapleAsciiString(Integer.toString(level));

        return mplew.getPacket();
    }

    public static MaplePacket Mulung_DojoUp2() {

        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.SHOW_ITEM_GAIN_INCHAT.getValue());
        mplew.write(7);

        return mplew.getPacket();
    }

    //更新被金锤子砸的装备
    public static MaplePacket updateHammerItem(IItem item) {

        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.MODIFY_INVENTORY_ITEM.getValue());
        mplew.write(0);
        mplew.write(2);
        mplew.write(3);
        mplew.write(item.getType());
        mplew.writeShort(item.getPosition());
        mplew.write(0);
        mplew.write(1);
        addItemInfo(mplew, item, false, false, true);
        return mplew.getPacket();
    }

    /**
     * 发送错误信息到客户端.
     *
     * @数据包的值大概如下:<br> 0x01 (1) - 现在关闭了缩地门 0x02 (2) - 不能去那里 0x03 (3) -
     * 对不起，正在准备冒险岛ONLINE商城 0x04 (4) - 因为有地气阻挡，无法接近。 0x05 (5) - 无法进行瞬间移动的地区。 0x06
     * (6) - 因为有地气阻挡，无法接近。 0x07 (7) -
     * 你因不当行为，而遭游戏管理员禁止攻击，禁止获取经验值和金币，禁止交易，禁止丢弃道具，禁止开启个人商店与精灵商人，禁止组队，禁止使用拍卖系统，因此无法使用改功能。
     * @返回数据包后通知。
     *
     * @param type
     * @return
     */
    public static MaplePacket sendBlockedMessage(int type) {

        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.BLOCK_MSG.getValue());
        mplew.write(type);
        return mplew.getPacket();
    }

    public static MaplePacket loadFamily(MapleCharacter player) {

        String[] title = {"直接移动到学院成员身边", "直接召唤学院成员", "我的爆率 1.5倍(15分钟)", "我的经验值 1.5倍(15分钟)", "学院成员的团结(30分钟)", "我的爆率 2倍(15分钟)", "我的经验值 2倍(15分钟)", "我的爆率 2倍(30分钟)", "我的经验值 2倍(30分钟)", "我的组队爆率 2倍(30分钟)", "我的组队经验值 2倍(30分钟)"};
        String[] description = {"[对象] 我\n[效果] 直接可以移动到指定的学院成员身边。", "[对象] 学院成员 1名\n[效果] 直接可以召唤指定的学院成员到现在的地图。", "[对象] 我\n[持续效果] 15分钟\n[效果] 打怪爆率增加到 #c1.5倍# \n※ 与爆率活动重叠时失效。", "[对象] 我\n[持续效果] 15分钟\n[效果] 打怪经验值增加到 #c1.5倍# \n※ 与经验值活动重叠时失效。", "[启动条件] 校谱最低层学院成员6名以上在线时\n[持续效果] 30分钟\n[效果] 爆率和经验值增加到 #c2倍# ※ 与爆率、经验值活动重叠时失效。", "[对象] 我\n[持续效果] 15分钟\n[效果] 打怪爆率增加到 #c2倍# \n※ 与爆率活动重叠时失效。", "[对象] 我\n[持续效果] 15分钟\n[效果] 打怪经验值增加到 #c2倍# \n※ 与经验值活动重叠时失效。", "[对象] 我\n[持续效果] 30分钟\n[效果] 打怪爆率增加到 #c2倍# \n※ 与爆率活动重叠时失效。", "[对象] 我\n[持续效果] 30分钟\n[效果] 打怪经验值增加到 #c2倍# \n※ 与经验值活动重叠时失效。", "[对象] 我所属组队\n[持续效果] 30分钟\n[效果] 打怪爆率增加到 #c2倍# \n※ 与爆率活动重叠时失效。", "[对象] 我所属组队\n[持续效果] 30分钟\n[效果] 打怪经验值增加到 #c2倍# \n※ 与经验值活动重叠时失效。"};
        int[] repCost = {3, 5, 7, 8, 10, 12, 15, 20, 25, 40, 50};
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.LOAD_FAMILY.getValue());
        mplew.writeInt(11);
        for (int i = 0; i < 11; i++) {
            mplew.write(i > 4 ? (i % 2) + 1 : i);
            mplew.writeInt(repCost[i] * 100);
            mplew.writeInt(1);
            mplew.writeMapleAsciiString(title[i]);
            mplew.writeMapleAsciiString(description[i]);
        }
        return mplew.getPacket();
    }

    public static MaplePacket sendFamilyMessage() {

        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.FAMILY_MESSAGE.getValue());
        mplew.writeInt(0);
        return mplew.getPacket();
    }

    public static MaplePacket getFamilyInfo(MapleCharacter chr) {

        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.OPEN_FAMILY.getValue()); // who cares what header is
        mplew.writeInt(chr.getFamily().getReputation()); // cur rep left
        mplew.writeInt(chr.getFamily().getTotalReputation()); // tot rep left
        mplew.writeInt(chr.getFamily().getTodaysRep()); // todays rep
        mplew.writeShort(chr.getFamily().getJuniors()); // juniors added
        mplew.writeShort(chr.getFamily().getTotalJuniors()); // juniors allowed
        mplew.writeShort(0);
        mplew.writeInt(chr.getFamilyId()); // id?
        mplew.writeMapleAsciiString(chr.getFamily().getFamilyName());
        mplew.writeInt(0);
        mplew.writeShort(0);
        return mplew.getPacket();
    }

    public static MaplePacket sendFamilyInvite(int playerId, String inviter) {

        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.FAMILY_INVITE.getValue());
        mplew.writeInt(playerId);
        mplew.writeMapleAsciiString(inviter);
        return mplew.getPacket();
    }

    public static MaplePacket sendEngagementRequest(String name) {

        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.FAMILY_ACTION.getValue()); //<name> has requested engagement. Will you accept this proposal?
        mplew.write(0);
        mplew.writeMapleAsciiString(name); // name
        mplew.writeInt(10); // playerid
        return mplew.getPacket();
    }

    public static MaplePacket sendGroomWishlist() {

        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.FAMILY_ACTION.getValue()); //<name> has requested engagement. Will you accept this proposal?
        mplew.write(9);
        return mplew.getPacket();
    }

    public static MaplePacket sendFamilyJoinResponse(boolean accepted, String added) {

        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.FAMILY_MESSAGE2.getValue());
        mplew.write(accepted ? 1 : 0);
        mplew.writeMapleAsciiString(added);
        return mplew.getPacket();
    }

    public static MaplePacket getSeniorMessage(String name) {

        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.FAMILY_SENIOR_MESSAGE.getValue());
        mplew.writeMapleAsciiString(name);
        mplew.writeInt(0);
        return mplew.getPacket();
    }

    public static MaplePacket sendGainRep(int gain, int mode) {

        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.FAMILY_GAIN_REP.getValue());
        mplew.writeInt(gain);
        mplew.writeShort(0);
        return mplew.getPacket();
    }

    //更新背包内道具
    public static MaplePacket updateItemInSlot(IItem item) {

        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.MODIFY_INVENTORY_ITEM.getValue());
        mplew.write(0); // could be from drop
        mplew.write(2); // always 2
        mplew.write(3); // quantity > 0 (?)
        mplew.write(item.getType()); // inventory type
        mplew.write(item.getPosition()); // item slot
        mplew.writeShort(0);
        mplew.write(1);
        mplew.write(item.getPosition()); // wtf repeat
        addItemInfo(mplew, item, true, false, false);
        return mplew.getPacket();
    }

    //物品到期
    public static MaplePacket itemExpired(int itemid) {

        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.SHOW_STATUS_INFO.getValue());
        mplew.write(2);
        mplew.writeInt(itemid);
        return mplew.getPacket();
    }

    /**
     * 19 itemlevelup 17 book pickup 10 过图声音 11 转职图案 12 好像是任务完成
     */
    public static MaplePacket showSpecialEffect(int effect) {

        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.SHOW_ITEM_GAIN_INCHAT.getValue());
        mplew.write(effect);
        return mplew.getPacket();
    }

    public static MaplePacket playPortalSound() {
        return showSpecialEffect(10);
    }

    public static MaplePacket showMonsterBookPickup() {
        return showSpecialEffect(17);
    }

    public static MaplePacket showEquipmentLevelUp() {
        return showSpecialEffect(19);
    }

    public static MaplePacket showItemLevelup() {
        return showSpecialEffect(19);
    }

    //金锤子
    public static MaplePacket sendHammerData(int hammerUsed) {

        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.VICIOUS_HAMMER.getValue());
        mplew.write(HexTool.getByteArrayFromHexString("34 00 00 00 00"));
        mplew.writeInt(hammerUsed);
        return mplew.getPacket();
    }

    public static MaplePacket sendHammerMessage() {

        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.VICIOUS_HAMMER.getValue());
        mplew.write(HexTool.getByteArrayFromHexString("38 00 00 00 00"));
        return mplew.getPacket();
    }

    public static MaplePacket hammerItem(IItem item) {

        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.MODIFY_INVENTORY_ITEM.getValue());
        mplew.write(0); // could be from drop
        mplew.write(2); // always 2
        mplew.write(3); // quantity > 0 (?)
        mplew.write(1); // Inventory type
        //mplew.write(item.getPosition()); // item slot
        //mplew.writeShort(0);
        mplew.writeShort(item.getPosition());
        mplew.write(0);
        mplew.write(1);
        mplew.write(item.getPosition()); // wtf repeat
        //addItemInfo(mplew, item, true, false, false);
        addItemInfo(mplew, item, false, false, false);
        return mplew.getPacket();
    }

    public static MaplePacket sendDojoAnimation(byte firstByte, String animation) {

        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.BOSS_ENV.getValue());
        mplew.write(firstByte);
        mplew.writeMapleAsciiString(animation);
        return mplew.getPacket();
    }

    public static MaplePacket getDojoInfo(String info) {

        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.SHOW_STATUS_INFO.getValue());
        mplew.write(10);
        mplew.write(HexTool.getByteArrayFromHexString("B7 04"));
        mplew.writeMapleAsciiString(info);
        return mplew.getPacket();
    }

    public static MaplePacket getDojoInfoMessage(String message) {

        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.SHOW_STATUS_INFO.getValue());
        mplew.write(9);
        mplew.writeMapleAsciiString(message);
        return mplew.getPacket();
    }

    //更新道场
    public static MaplePacket updateDojoStats(MapleCharacter chr, int belt) {

        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.SHOW_STATUS_INFO.getValue());
        mplew.write(0x0A);
        mplew.write(HexTool.getByteArrayFromHexString("B7 04")); //?
        mplew.writeMapleAsciiString("pt=" + chr.getDojoPoints() + ";belt=" + belt + ";tuto=" + (chr.getFinishedDojoTutorial() ? "1" : "0"));
        return mplew.getPacket();
    }

    public static MaplePacket dojoWarpUp() {

        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.DOJO_WARP_UP.getValue());
        mplew.write(0);
        mplew.write(6);
        return mplew.getPacket();
    }

    public static MaplePacket addCard(boolean full, int cardid, int level) {

        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.MONSTERBOOK_ADD.getValue());
        mplew.write(full ? 0 : 1);
        mplew.writeInt(cardid);
        mplew.writeInt(level);
        return mplew.getPacket();
    }

    public static MaplePacket showGainCard() {

        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.SHOW_ITEM_GAIN_INCHAT.getValue());
        mplew.write(0x0D);
        return mplew.getPacket();
    }

    public static MaplePacket showForeginCardEffect(int id) {

        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.SHOW_FOREIGN_EFFECT.getValue());
        mplew.writeInt(id);
        mplew.write(0x0D);
        return mplew.getPacket();
    }

    public static MaplePacket changeCover(int cardid) {

        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.MONSTER_BOOK_CHANGE_COVER.getValue());
        mplew.writeInt(cardid);
        return mplew.getPacket();
    }

    public static MaplePacket getStatusMsg(int itemid) {

        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.SHOW_STATUS_INFO.getValue());
        mplew.write(7);
        mplew.writeInt(itemid);
        return mplew.getPacket();
    }

    public static MaplePacket enableTV() {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.ENABLE_TV.getValue());
        mplew.writeInt(0);
        mplew.write(0);
        return mplew.getPacket();
    }

    public static MaplePacket removeTV() {

        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.REMOVE_TV.getValue());
        return mplew.getPacket();
    }

    public static MaplePacket sendTV(MapleCharacter chr, List<String> messages, int type, MapleCharacter partner, int delay) {

        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.SEND_TV.getValue());
        mplew.write(partner != null ? 2 : 0);
        mplew.write(type); //Heart = 2  Star = 1  Normal = 0
        addCharLook(mplew, chr, false);
        mplew.writeMapleAsciiString(chr.getName());
        if (partner != null) {
            mplew.writeMapleAsciiString(partner.getName());
        } else {
            mplew.writeShort(0);
        }
        for (int i = 0; i < messages.size(); i++) {
            if (i == 4 && messages.get(4).length() > 15) {
                mplew.writeMapleAsciiString(messages.get(4).substring(0, 15));
            } else {
                mplew.writeMapleAsciiString(messages.get(i));
            }
        }
        mplew.writeInt(delay);
        if (partner != null) {
            addCharLook(mplew, partner, false);
        }
        return mplew.getPacket();
    }

    public static MaplePacket updateIntroState(String mode, int quest) {

        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.SHOW_STATUS_INFO.getValue());
        mplew.write(11); //mode
        mplew.writeShort(quest);
        mplew.writeMapleAsciiString(mode);
        return mplew.getPacket();
    }

    public static MaplePacket addTutorialStats() {

        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter(0);
        mplew.writeShort(SendPacketOpcode.ENABLE_TEMPORARY_STATS.getValue());
        mplew.writeInt(3871);
        mplew.writeShort(999);
        mplew.writeShort(999);
        mplew.writeShort(999);
        mplew.writeShort(999);
        mplew.writeShort(255);
        mplew.writeShort(999);
        mplew.writeShort(999);
        mplew.write(120);
        mplew.write(140);
        return mplew.getPacket();
    }

    public static MaplePacket removeTutorialStats() {

        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.DISABLE_TEMPORARY_STATS.getValue());
        return mplew.getPacket();
    }

    public static MaplePacket lockUI(boolean mode) {

        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.TUTORIAL_DISABLE_UI.getValue());
        mplew.write(mode ? 1 : 0);
        return mplew.getPacket();
    }

    public static MaplePacket disableUI(boolean mode) {

        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.TUTORIAL_LOCK_UI.getValue());
        mplew.write(mode ? 1 : 0);
        return mplew.getPacket();
    }

    public static MaplePacket spawnTutorialSummon(int type) {

        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.TUTORIAL_SUMMON.getValue());
        mplew.write(type);
        return mplew.getPacket();
    }

    public static MaplePacket displayGuide(int guide) {

        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.TUTORIAL_GUIDE.getValue());
        mplew.write(1);
        mplew.writeInt(guide);
        mplew.writeInt(12000);
        return mplew.getPacket();
    }

    public static MaplePacket tutorialSpeechBubble(String message) {

        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.TUTORIAL_GUIDE.getValue());
        mplew.write(0);
        mplew.writeMapleAsciiString(message);
        mplew.writeInt(200);
        mplew.writeInt(10000);
        return mplew.getPacket();
    }

    public static MaplePacket showInfo(String message) {

        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.SHOW_INFO.getValue());
        mplew.writeMapleAsciiString(message);
        return mplew.getPacket();
    }

    public static MaplePacket showMapEffect(String path) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.BOSS_ENV.getValue());
        mplew.write(4);
        mplew.writeMapleAsciiString(path);
        return mplew.getPacket();
    }

    public static MaplePacket showTipsEffect(String env) {

        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.SHOW_ITEM_GAIN_INCHAT.getValue());
        mplew.write(23);
        mplew.writeMapleAsciiString(env);
        mplew.writeInt(1);
        return mplew.getPacket();
    }

    public static MaplePacket showWZEffect(String path, int info) {

        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.SHOW_ITEM_GAIN_INCHAT.getValue());
        if (info == -1) {
            mplew.write(0x15);//12
        } else {
            mplew.write(0x18);//17
        }
        mplew.writeMapleAsciiString(path);
        if (info > -1) {
            mplew.writeInt(info);
        }
        return mplew.getPacket();
    }

    public static MaplePacket showWZEffectS(String path, int info) {

        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.SHOW_ITEM_GAIN_INCHAT.getValue());
        mplew.write(0x14);
        mplew.writeMapleAsciiString(path);
        if (info > -1) {
            mplew.writeInt(info);
        }
        return mplew.getPacket();
    }

    public static MaplePacket playWZSound(String path) {

        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.MAP_EFFECT.getValue());
        mplew.write(4); //mode
        mplew.writeMapleAsciiString(path);
        return mplew.getPacket();
    }

    public static MaplePacket blockedPortal() {

        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.UPDATE_STATS.getValue());
        mplew.write(new byte[5]);//97变 以前5
        //mplew.writeInt(0);//97
        mplew.write(1);
        mplew.writeShort(1);
        return mplew.getPacket();
    }

    public static MaplePacket shenlong(int i) {

        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(i); //274/275 暗/亮   124/125 龙珠9个齐全,开始召唤神龙
        //mplew.write(HexTool.getByteArrayFromHexString("00 BC 06 00 00"));
        mplew.write(HexTool.getByteArrayFromHexString("DC 05 00 00 90 5F 01 00 DC 05 00 00 9B 00 00 00"));
        return mplew.getPacket();
    }

    public static MaplePacket shenlong2(int i) {

        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(i); //274/275 暗/亮   124/125 龙珠9个齐全,开始召唤神龙
        mplew.write(HexTool.getByteArrayFromHexString("02 CB 06 00 00 FB 44 00 00"));
        return mplew.getPacket();
    }

    public static MaplePacket DragonBall1(int i, boolean Zhaohuan) {

        //打开谜之蛋 137
        //龙珠 141
        //95 00 00 00 00 00 01 00 00 01 01 00 00
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.DragonBall1.getValue());
        mplew.writeInt(0);
        mplew.write(1);
        if (!Zhaohuan) { //不能召唤
            mplew.writeShort(0);
            mplew.writeShort(i); //512的倍数都是满龙珠 不用Long的原因是512的倍数出现的龙珠是蓝色的 漂亮
            mplew.writeShort(0);
        } else { //可以召唤
            mplew.writeLong(512); //512的倍数都是满龙珠 不是则出不来
        }
        return mplew.getPacket();
    }

    public static MaplePacket getCY1(int npc, String talk, byte type) {

        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.NPC_TALK.getValue());
        mplew.write(4);
        mplew.writeInt(npc);
        mplew.write(0x0E);//次元之镜
        mplew.write(0);
        mplew.writeInt(0);
        mplew.write(type);//由#type#开始 填1 打开Npc就先显示#1#的图案    填2 打开Npc就先显示#2#的图案
        mplew.writeShort(0);
        mplew.write(0);
        mplew.writeMapleAsciiString(talk);
        return mplew.getPacket();
    }

    public static MaplePacket getCY2(int npc, String talk, byte type) {

        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.NPC_TALK.getValue());
        mplew.write(4);
        mplew.writeInt(npc);
        mplew.write(0x11);//龙舟赛
        mplew.write(0);
        mplew.writeInt(0);
        mplew.write(type);//由#type#开始 填1 打开Npc就先显示#1#的图案    填2 打开Npc就先显示#2#的图案
        mplew.writeShort(0);
        mplew.write(0);
        mplew.writeMapleAsciiString(talk);
        return mplew.getPacket();
    }

    public static MaplePacket toWeb() {

        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(0x8B);
        mplew.writeMapleAsciiString("http://www.psmxd.com/");
        return mplew.getPacket();
    }

    public static MaplePacket showMagniflerEffect(int cid, short pos) {

        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.MAGNIFLER_SCROLL.getValue());
        mplew.writeInt(cid);
        mplew.writeShort(pos);
        mplew.write();
        return mplew.getPacket();
    }

    /*
     * public static MaplePacket 验证码() { MaplePacketLittleEndianWriter mplew =
     * new MaplePacketLittleEndianWriter();
     * mplew.write(HexTool.getByteArrayFromHexString("17 00 01 95 08 00 00 FF D8
     * FF E0 00 10 4A 46 49 46 00 01 01 00 00 01 00 01 00 00 FF FE 00 3B 43 52
     * 45 41 54 4F 52 3A 20 67 64 2D 6A 70 65 67 20 76 31 2E 30 20 28 75 73 69
     * 6E 67 20 49 4A 47 20 4A 50 45 47 20 76 36 32 29 2C 20 71 75 61 6C 69 74
     * 79 20 3D 20 39 30 0A FF DB 00 43 00 03 02 02 03 02 02 03 03 03 03 04 03
     * 03 04 05 08 05 05 04 04 05 0A 07 07 06 08 0C 0A 0C 0C 0B 0A 0B 0B 0D 0E
     * 12 10 0D 0E 11 0E 0B 0B 10 16 10 11 13 14 15 15 15 0C 0F 17 18 16 14 18
     * 12 14 15 14 FF DB 00 43 01 03 04 04 05 04 05 09 05 05 09 14 0D 0B 0D 14
     * 14 14 14 14 14 14 14 14 14 14 14 14 14 14 14 14 14 14 14 14 14 14 14 14
     * 14 14 14 14 14 14 14 14 14 14 14 14 14 14 14 14 14 14 14 14 14 14 14 14
     * 14 FF C0 00 11 08 00 3C 00 78 03 01 22 00 02 11 01 03 11 01 FF C4 00 1F
     * 00 00 01 05 01 01 01 01 01 01 00 00 00 00 00 00 00 00 01 02 03 04 05 06
     * 07 08 09 0A 0B FF C4 00 B5 10 00 02 01 03 03 02 04 03 05 05 04 04 00 00
     * 01 7D 01 02 03 00 04 11 05 12 21 31 41 06 13 51 61 07 22 71 14 32 81 91
     * A1 08 23 42 B1 C1 15 52 D1 F0 24 33 62 72 82 09 0A 16 17 18 19 1A 25 26
     * 27 28 29 2A 34 35 36 37 38 39 3A 43 44 45 46 47 48 49 4A 53 54 55 56 57
     * 58 59 5A 63 64 65 66 67 68 69 6A 73 74 75 76 77 78 79 7A 83 84 85 86 87
     * 88 89 8A 92 93 94 95 96 97 98 99 9A A2 A3 A4 A5 A6 A7 A8 A9 AA B2 B3 B4
     * B5 B6 B7 B8 B9 BA C2 C3 C4 C5 C6 C7 C8 C9 CA D2 D3 D4 D5 D6 D7 D8 D9 DA
     * E1 E2 E3 E4 E5 E6 E7 E8 E9 EA F1 F2 F3 F4 F5 F6 F7 F8 F9 FA FF C4 00 1F
     * 01 00 03 01 01 01 01 01 01 01 01 01 00 00 00 00 00 00 01 02 03 04 05 06
     * 07 08 09 0A 0B FF C4 00 B5 11 00 02 01 02 04 04 03 04 07 05 04 04 00 01
     * 02 77 00 01 02 03 11 04 05 21 31 06 12 41 51 07 61 71 13 22 32 81 08 14
     * 42 91 A1 B1 C1 09 23 33 52 F0 15 62 72 D1 0A 16 24 34 E1 25 F1 17 18 19
     * 1A 26 27 28 29 2A 35 36 37 38 39 3A 43 44 45 46 47 48 49 4A 53 54 55 56
     * 57 58 59 5A 63 64 65 66 67 68 69 6A 73 74 75 76 77 78 79 7A 82 83 84 85
     * 86 87 88 89 8A 92 93 94 95 96 97 98 99 9A A2 A3 A4 A5 A6 A7 A8 A9 AA B2
     * B3 B4 B5 B6 B7 B8 B9 BA C2 C3 C4 C5 C6 C7 C8 C9 CA D2 D3 D4 D5 D6 D7 D8
     * D9 DA E2 E3 E4 E5 E6 E7 E8 E9 EA F2 F3 F4 F5 F6 F7 F8 F9 FA FF DA 00 0C
     * 03 01 00 02 11 03 11 00 3F 00 FB 62 8A 28 AC 8E 20 A2 8A 28 00 A2 8A 28
     * 00 A2 8A 28 00 A2 8A 28 00 A2 8A 28 00 A2 8A 28 00 A2 8A 28 00 A2 8A 28
     * 00 A2 8A F1 1F 8B 9F B4 7D F7 83 9A 7B 3F 03 F8 0F 5A F8 8B A9 5A 4B 34
     * 77 D2 59 43 2C 36 36 46 24 2C E1 AE 4C 6C AF 20 23 1E 5A 67 90 54 95 6C
     * 2B 05 46 2E 4E C8 F4 2F 89 FF 00 14 BC 39 F0 7B C2 37 5E 23 F1 45 F8 B1
     * D3 A1 21 14 01 BA 49 E4 39 DB 1C 6B D5 98 E0 F1 E8 09 24 00 48 F2 4F 87
     * FA 9F C5 5F 8F 6D FF 00 09 72 78 86 5F 86 7E 03 BC 0A FA 4E 95 6D 61 6D
     * 71 A9 DD 43 B4 7E FA 59 26 47 48 C3 9C B2 80 A7 82 3B 61 9B E2 0F 17 FC
     * 61 D7 7F 6D BF 8E 1E 04 D0 75 6B 65 D1 74 77 BE 5B 38 EC 2C A5 2E 22 89
     * E5 DD 2C BB 98 73 27 94 00 CE 00 FD DE 40 19 22 BD BA EF E2 76 B1 F0 77
     * F6 D2 D0 FC 03 A7 78 D3 C4 3A B7 83 AD A5 81 35 0B 1D 72 ED 5A 18 0C 96
     * AC 7E 57 04 7E E6 34 91 24 C1 0A 01 42 08 21 41 35 63 B3 D8 B8 2B 7D AD
     * CF 70 FD A8 FE 2D F8 C7 F6 69 F8 65 A5 6B FA 35 D4 3E 2B 2D 7C 2C 6E 9F
     * 5E B5 52 FF 00 3C 72 32 48 5A DC C2 A0 2B 46 A3 01 39 DC 79 07 15 E2 9F
     * 15 3C 77 F1 77 C5 3F B3 B6 97 F1 C3 49 F1 DC 9A 14 31 4B 6D 31 F0 EE 8D
     * 1A F9 09 1A 4B E4 97 92 4E 4C 8C 66 E5 A3 60 53 69 DA C0 ED 39 EB FF 00
     * 6A 8F 8F BF 0F FE 29 FE CF DE 2A D3 EC A4 D4 EE 74 D9 A6 16 D6 3E 20 1A
     * 6C BF D9 EF 7D 13 09 56 25 97 19 CB 6C 2B BB 1B 7E 6E B8 E6 B8 8F D9 0B
     * 58 B2 F1 BF EC 91 E3 DF 87 9A E5 CA 58 CA D1 DF 1D 3B ED 40 AF 99 13 C2
     * 1F 7C 43 19 93 CB 94 16 6D 80 E3 72 E4 72 32 21 C2 3C B0 52 6B 5B FE 05
     * BF 02 78 AF E2 BF ED 67 F0 3F C4 1A D2 F8 F2 E7 C2 BA 86 87 6C F1 C7 A7
     * E8 76 DF 67 7D 52 68 D1 9D 26 79 54 86 55 90 EE 88 AA 7C BB A3 CE 38 DB
     * 51 FE CB 7F B7 EA CD 65 E1 7F 03 78 E6 D2 FF 00 53 D6 EE 2F A1 D2 AD F5
     * B8 8A 15 31 3E D8 E2 7B 82 EE 0B 30 63 F3 3F 52 BF 31 CB 67 37 7F E0 9D
     * BE 27 D5 FC 03 A5 F8 B3 C1 1E 2B D0 B5 AD 11 63 B8 5D 52 DE 6B DD 32 E1
     * 22 8C B4 23 CC 59 18 A6 23 25 52 26 5D C4 6E DD C7 24 67 E2 3F 18 E9 7A
     * AF C3 CF 89 06 F2 E3 40 D4 3C 37 8B CF ED 3D 3E CF 50 B7 92 07 F2 3C E2
     * 62 65 DE AA 59 7E 5C 06 C0 CE D3 D3 04 07 6E 86 CA 9C 67 29 41 AD 3A 1F
     * B8 74 57 C8 9F 1C BE 35 FC 6E F8 2F E0 8B 5F 88 71 BF 84 B5 7D 07 56 92
     * 15 3A 3B 59 CD 27 F6 4A BA B3 44 56 64 95 0C DB 81 01 DC E0 06 0B B5 40
     * 26 B5 3F 63 BF DA C3 5E FD A2 75 9D 7A CB C4 30 E8 DA 54 D6 16 A9 2D BD
     * A6 9D 14 82 49 FE 60 24 94 B3 CA DB 55 49 40 14 AF 3B F8 63 B4 8A 8B 1C
     * 1E C6 5C BC FD 0F A9 E8 AF 2A F8 29 6D A9 EA FA 8F 8A 7C 4D A9 EB 97 FA
     * F5 A5 CD E9 B3 D0 EE AE 8C 6B 13 E9 C8 AA CB 2C 6B 12 24 4E 24 95 A5 61
     * 28 5C BA 08 FE 62 00 AF 55 A0 C9 AB 3B 05 14 51 40 82 8A 28 A0 02 BC DB
     * E3 6F C6 2B 5F 85 3E 1F 58 6C AD DB 59 F1 8E A6 1A 1D 0F C3 F6 8A 64 B8
     * BD 9F 07 07 60 E4 46 BF 79 DF A0 00 F3 92 05 56 FD A1 BC 5D F1 13 C2 1E
     * 06 96 E7 E1 BF 85 63 F1 36 B2 D9 57 67 94 66 D5 78 F9 D6 1E 0C CD CF 0A
     * 0F 18 C9 CF 4A F9 17 E1 86 BF FB 46 78 07 E2 44 C2 E7 E1 02 78 87 C5 9A
     * A2 BC 77 DE 22 D5 63 91 9E 64 C9 70 82 F4 4B F6 78 62 50 A0 08 D3 6A 92
     * 17 82 DB 69 A4 6F 4E 9F 32 E6 6D 7D E6 BF EC FD FB 1B 78 FF 00 E0 D7 C4
     * CF 0B F8 EB 55 D3 34 CF 12 5D BD 8D DC D7 16 2B 7C 20 3A 5D EB C5 27 96
     * 19 B9 59 01 C8 42 C8 18 2B 48 C4 2B 04 57 3E 6F FB 58 7C 14 D7 FE 1C 68
     * 9A CF C4 8F 1A EB 5A 75 CF 8D BC 5D AB FD 92 1D 3B 4B 91 BC AB 2B 56 8D
     * DA 42 AC C1 59 F0 89 14 27 8C 05 73 92 C5 C5 7E 97 68 30 EA 16 FA 35 9A
     * 6A D7 31 5D EA 7E 58 37 32 C1 1E C8 8C 87 96 08 BD 42 02 48 50 49 38 03
     * 24 9C 93 CF FC 51 F8 4F E1 8F 8C 9E 16 9B 40 F1 56 99 1E A1 64 E7 7C 6F
     * F7 66 B7 93 B4 91 3F 54 61 EA 3A 8C 83 90 48 A2 E6 91 C4 4B 9F 9A 47 93
     * 7C 33 F8 29 F0 FF 00 E2 3F EC 99 E0 7F 0B DD 4E 75 4F 0B 8B 38 35 09 27
     * B7 B8 11 16 B8 F9 A4 98 B3 29 F9 71 23 CA 0A E7 2B 82 09 C8 AF 78 F0 FE
     * 81 A7 78 5B 44 B1 D2 34 8B 38 AC 34 CB 28 56 0B 7B 68 06 12 34 51 80 07
     * F8 F7 AF 03 F8 67 FB 0F 78 47 E1 B5 DB 2A 78 93 C5 3A CE 8A 65 F3 BF B0
     * 2F B5 00 BA 7C 8E 3A 34 D0 C6 AA B2 91 81 C3 70 71 C8 35 F4 65 26 63 51
     * A6 F4 77 41 5F 06 FF 00 C1 4B 3E 1D EA DE 22 F1 27 C3 DD 53 42 D2 AE F5
     * 9B E9 20 BC B5 92 DA CA D5 AE 24 DB 19 8E 50 76 A8 24 8C 34 84 F1 80 05
     * 7D E5 46 39 A1 68 2A 73 74 E5 CC 8F 8F 7E 39 7C 54 F0 4F C4 7F D9 50 F8
     * 2F C1 F7 07 5B F1 0E A5 A6 D9 C5 63 E1 9D 22 39 6F 6F 6D 9A 19 21 66 49
     * 50 6E 78 FC B0 87 26 52 09 DA 79 27 AF CE 8B FB 2C FC 52 FD 9F B5 0F 87
     * 7E 20 D3 F5 0F B1 EA DE 25 B9 1A 35 D0 B2 1E 61 D3 64 B9 FD DA C7 21 3F
     * 2B 65 1C 92 70 55 5D 0F 27 E5 27 F5 2D 22 48 D9 99 51 55 9C E5 88 18 27
     * EB 4E A7 7B 1A C6 BB 82 B4 56 85 5D 2B 4C B5 D1 34 BB 3D 3A C6 14 B6 B2
     * B4 85 2D E0 86 31 85 8E 34 50 AA A0 7A 00 00 AB 54 51 48 E6 0A 28 A2 80
     * 0A 28 A2 80 0A 28 A2 80 0A 28 A2 80 0A 28 A2 80 0A 28 A2 80 0A 28 A2 80
     * 0A 28 A2 80 0A 28 A2 80 3F FF D9")); return mplew.getPacket(); }
     */
    public static MaplePacket giveMonsterbBomb(MapleMonster monster) {

        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.MonsterBombSkill.getValue());//0x10E
        mplew.writeInt(双刀.怪物炸弹);
        mplew.writeInt(monster.getPosition().x);
        mplew.writeInt(monster.getPosition().y);
        mplew.writeInt(0);
        mplew.writeInt(0);
        return mplew.getPacket();
    }

    public static MaplePacket 机械技能特效(int type, int value, int skillid, int skilllevel, int unknow, int charlevel) {

        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        //EE 00 01 85 E3 17 02 7A 01 01 导弹战车
        //EE 00 03 04 00 00 00 DF FC 4D 00 01 骰子
        //09 01 03 05 00 00 00 65 C0 17 02 14 骰子
        mplew.writeShort(SendPacketOpcode.SHOW_ITEM_GAIN_INCHAT.getValue());
        mplew.write(type);
        if (type == 3) // 3是骰子
        {
            mplew.writeInt(value); //骰子点数
        }
        mplew.writeInt(skillid);
        mplew.write(charlevel);
        mplew.write(skilllevel);
        mplew.write(unknow); //不知道是什么 经常是01
        return mplew.getPacket();
    }

    //给别人看的
    public static MaplePacket 机械技能特效(int cid, int type, int value, int skillid, int skilllevel, int unknow, int charlevel) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.SHOW_ITEM_GAIN_INCHAT.getValue());
        mplew.writeInt(cid);
        mplew.write(type);
        if (type == 3) // 3是骰子
        {
            mplew.writeInt(value); //骰子点数
        }
        mplew.writeInt(skillid);
        mplew.write(charlevel);
        mplew.write(skilllevel);
        mplew.write(unknow); //不知道是什么 经常是01
        return mplew.getPacket();
    }

    /**
     *
     * @param skillid
     * @param statups
     * @param skilllength
     * @param type
     * @param rate 指定双幸运骰子的等级。
     * @return
     */
    public static MaplePacket 传送门的效果(int cid, Point pos, boolean first) { //这里是假的 一个显示出来效果而已
//E0 01 00 E9 53 48 00 C7 00 A1 04 01 00 00 00 00
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.SPAWN_DOOR2.getValue());
        mplew.write(0);
        mplew.writeInt(cid);
        mplew.writeShort(pos.x);
        mplew.writeShort(pos.y);
        mplew.write(first ? 1 : 0);
        mplew.writeInt(0);
        return mplew.getPacket();
    }

    public static MaplePacket 传送门的传送点(Point pos) { //召唤一个传送点

        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.SPAWN_PORTAL2.getValue());
        mplew.writeShort(pos.x);
        mplew.writeShort(pos.y);
        return mplew.getPacket();
    }

    public static MaplePacket 取消传送门(int cid, boolean first) { //取消传送门的
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.REMOVE_DOOR2.getValue());
        mplew.write(0);
        mplew.writeInt(cid);
        mplew.write(first ? 1 : 0);
        return mplew.getPacket();
    }

    public static MaplePacket 召唤磁场(int cid, int oid1, int oid2, int oid3) { //召唤磁场

        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.SPAWN_MAGNETIC_FIELD.getValue());
        mplew.writeInt(cid);
        mplew.writeInt(oid1);
        mplew.writeInt(oid2);
        mplew.writeInt(oid3);
        return mplew.getPacket();
    }

    public static MaplePacket 魔方光效(int id, int itemid) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.MAGIC_SCROLL.getValue());
        mplew.writeInt(id);
        mplew.write(1);
        mplew.writeInt(itemid);
        return mplew.getPacket();
    }

    public static MaplePacket getMagicResult() {
        MaplePacketLittleEndianWriter mplew = MakeMplew(SendPacketOpcode.MAGIC_RESULT);
        mplew.writeInt(1);
        return mplew.getPacket();
    }

    public static MaplePacket 结婚效果() {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(0x4F);
        return mplew.getPacket();
    }

    public static MaplePacket 怪物炸弹效果(MapleCharacter chr, int x, int y, int skillid, int skilllevel) {

        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.SHOW_ITEM_GAIN_INCHAT.getValue());
        mplew.write(7); //effect type
        mplew.writeInt(skillid);
        mplew.writeInt(x);
        mplew.writeInt(y);
        mplew.writeInt(skilllevel);
        mplew.writeInt(chr.getLevel());
        return mplew.getPacket();
    }

    /*
     * Type 1 - PVP提示1[屏幕正中间黄字] 2 - PVP提示2[屏幕正中间绿字] 3 - PVP提示3[对话框内多种颜色的字]
     */
    public static MaplePacket 文字公告(int type, int type2, String text) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        switch (type) {
            case 1: //黄字
                mplew.writeShort(SendPacketOpcode.Maple_Tip1.getValue());
                break;
            case 2: //绿字
                mplew.writeShort(SendPacketOpcode.Maple_Tip2.getValue());
                mplew.write(0);
                break;
            case 3: //多颜色字
                //5 绿字 悄悄话
                //7 黄字
                mplew.writeShort(SendPacketOpcode.Maple_Tip3.getValue());
                mplew.writeShort(type2);
                break;
        }
        mplew.writeMapleAsciiString(text);
        if (type == 2) {
            mplew.write(0);
        }
        return mplew.getPacket();
    }

    public static MaplePacket 创建终极冒险家() {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.Ultimate.getValue());
        return mplew.getPacket();
    }

    public static MaplePacket openBeans(int beansCount, int type) {

        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.BEANS_GAME1.getValue());
        mplew.writeInt(beansCount);
        mplew.write(type);
        return mplew.getPacket();
    }

    public static MaplePacket updateBeans(int cid, int beansCount) {

        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.UPDATE_BEANS.getValue());
        mplew.writeInt(cid);
        mplew.writeInt(beansCount);
        mplew.writeInt(0);
        return mplew.getPacket();
    }

    public static MaplePacket showBeans(List<Beans> beansInfo) {

        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.BEANS_GAME2.getValue());
        mplew.write(0);
        mplew.write(beansInfo.size());
        for (Beans bean : beansInfo) {
            mplew.writeShort(bean.getPos());
            mplew.write(bean.getType());
            mplew.writeInt(bean.getNumber());
            ////log.debug("发包时豆豆序号"+bean.getNumber());
        }
        ////log.debug("豆豆发包"+mplew.getPacket());
        //mplew.write(HexTool.getByteArrayFromHexString("DA 01 00 05 BD 0F 01 60 00 00 00 FF 0E 01 61 00 00 00 69 0E 01 62 00 00 00 05 0F 01 63 00 00 00 C6 0F 01 64 00 00 00"));
        return mplew.getPacket();
    }

    public static MaplePacket getNPCTalkText(int npc, String talk, int Least, int max) {

        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.NPC_TALK.getValue());
        mplew.write(4); // ?
        mplew.writeInt(npc);
        mplew.writeShort(3);
        mplew.writeMapleAsciiString(talk);
        mplew.writeShort(0);
        mplew.writeShort(Least);//最少
        mplew.writeShort(max);
        return mplew.getPacket();
    }

    public static MaplePacket reportReply(byte type) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.REPORT_PLAYER_MSG.getValue());
        mplew.write(type);
        return mplew.getPacket();
    }

    public static MaplePacket sendTVlink(String Link) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(0x9B);
        mplew.writeMapleAsciiString(Link);
        return mplew.getPacket();
    }

    public static MaplePacket sendNecklace_Expansion(boolean open) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.NECKLACE_EXPANSION.getValue());
        mplew.write(open);
        return mplew.getPacket();
    }

    public static MaplePacket spawnAndroid(MapleCharacter cid, MapleAndroid android) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        int type = android.getItemId() - 1661999;
        mplew.writeShort(SendPacketOpcode.ANDROID_SPAWN.getValue());
        mplew.writeInt(cid.getId());
        if (type > 4) {
            mplew.write(4); //type of android, 1-4    
        } else {
            mplew.write(type); //type of android, 1-4 
        }
        mplew.writePos(android.getPos());
        mplew.write(android.getStance());
        mplew.writeShort(0); //no clue, FH or something
        mplew.writeShort(android.getSkin());
        mplew.writeShort(android.getHair() - 30000);
        mplew.writeShort(android.getFace() - 20000);
        mplew.writeMapleAsciiString(android.getName());

        for (short i = -1200; i > -1207; i--) {
            final IItem item = cid.getInventory(MapleInventoryType.EQUIPPED).getItem(i);
            mplew.writeInt(item != null ? item.getItemId() : 0);
        }
        return mplew.getPacket();
    }

    public static MaplePacket moveAndroid(int cid, Point pos, List<LifeMovementFragment> res) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.ANDROID_MOVE.getValue());
        mplew.writeInt(cid);
        mplew.writePos(pos);
        mplew.writeInt(Integer.MAX_VALUE); //time left in milliseconds? this appears to go down...slowly 1377440900
        serializeMovementList2(mplew, res);
        return mplew.getPacket();
    }

    public static MaplePacket showAndroidEmotion(int cid, int animation) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.ANDROID_EMOTION.getValue());
        mplew.writeInt(cid);
        mplew.write(0);
        mplew.write(animation); //1234567 = default smiles, 8 = 呕吐, 11 = kiss, 14 = googly eyes, 17 = wink...//42
        return mplew.getPacket();
    }

    public static MaplePacket updateAndroidLook(int cid, int size, int itemId) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.ANDROID_UPDATE.getValue());
        mplew.writeInt(cid);
        mplew.write(size);
        mplew.writeInt(itemId); // cash item
        mplew.write(0);
        return mplew.getPacket();
    }

    public static MaplePacket deactivateAndroid(int cid) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.ANDROID_DEACTIVATED.getValue());
        mplew.writeInt(cid);
        return mplew.getPacket();
    }

    public static MaplePacket removeAndroidHeart() {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.SHOW_STATUS_INFO.getValue());
        mplew.write(0x13); // 0x16 for resting -fatigue
        return mplew.getPacket();
    }

    public static MaplePacket getMarriedComplete() {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.MARRIED_EFFECT.getValue());
        return mplew.getPacket();
    }

    public static MaplePacket getYellowTip(String tip) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.YELLOW_TIP.getValue());
        mplew.writeMapleAsciiString(tip);
        return mplew.getPacket();
    }

    public static MaplePacket getMusicBox(int itemid, String name) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.MUSICBOX.getValue());
        mplew.writeInt(itemid);
        mplew.writeMapleAsciiString(name);
        return mplew.getPacket();
    }

    public static MaplePacketLittleEndianWriter MakeMplew(SendPacketOpcode send) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(send.getValue());
        return mplew;
    }

    public static MaplePacket getChangeCmd(int pos, int skill) {
        MaplePacketLittleEndianWriter mplew = MakeMplew(SendPacketOpcode.CHANGE_CMD);
        mplew.write(1);//修改个数
        mplew.write(pos);
        mplew.writeInt(skill);
        mplew.write();
        return mplew.getPacket();
    }

    public static MaplePacket getAttackEffect(int chrId, int skillId, List<Integer> monsters, int type) {
        int count = Math.min(5, monsters.size());

        MaplePacketLittleEndianWriter mplew = MakeMplew(SendPacketOpcode.ATTACKEFFECT);
        mplew.write();
        mplew.writeInt(chrId);
        mplew.writeInt(type);
        mplew.write(1);

        mplew.writeInt(count);
        for (int i = 0; i < count; i++) {
            mplew.writeInt(monsters.get(i));
        }

        mplew.writeInt(skillId);

        for (int i = 0; i < 5; i++) {
            mplew.write(1);
            mplew.writeInt(2 + i);
            mplew.writeInt(2);
            mplew.writeInt(0x12 - Randomizer.getInstance().nextInt(3));
            mplew.writeLong(0x1A + i);
            mplew.writeInt(1000 + Randomizer.getInstance().nextInt(500));//效果延时

            //      mplew.writeInt(0x0f);
            //      mplew.writeLong(0x19);
            //       mplew.writeInt(1200 + Randomizer.getInstance().nextInt(100));
        }
        mplew.write();

        return mplew.getPacket();
    }

    public static MaplePacket getGamePropertyChange(String key, String value) {
        MaplePacketLittleEndianWriter mplew = MakeMplew(SendPacketOpcode.GAME_PROPERTY_CHANGE);
        mplew.writeMapleAsciiString(key);
        mplew.writeMapleAsciiString(value);
        return mplew.getPacket();
    }

    public static MaplePacket getShowSkillEffectCode(int code) {
        MaplePacketLittleEndianWriter mplew = MakeMplew(SendPacketOpcode.SHOW_ITEM_GAIN_INCHAT);
        mplew.write(code);
        return mplew.getPacket();
    }
}
