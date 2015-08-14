/*

 */
package net.sf.odinms.net.channel;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.net.InetSocketAddress;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;
import java.util.Map.Entry;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import javax.management.*;
import javax.rmi.ssl.SslRMIClientSocketFactory;

import net.sf.odinms.client.*;
import net.sf.odinms.client.messages.CommandProcessor;
import net.sf.odinms.database.DatabaseConnection;
import net.sf.odinms.net.MaplePacket;
import net.sf.odinms.net.MapleServerHandler;
import net.sf.odinms.net.PacketProcessor;
import net.sf.odinms.net.channel.remote.ChannelWorldInterface;
import net.sf.odinms.net.channel.www.WebServer;
import net.sf.odinms.net.mina.MapleCodecFactory;
import net.sf.odinms.net.servlet.ChannelServerConfig;
import net.sf.odinms.net.servlet.GeneralServer;
import net.sf.odinms.net.servlet.GeneralServerType;
import net.sf.odinms.net.world.MapleParty;
import net.sf.odinms.net.world.guild.MapleGuild;
import net.sf.odinms.net.world.guild.MapleGuildCharacter;
import net.sf.odinms.net.world.guild.MapleGuildSummary;
import net.sf.odinms.net.world.remote.WorldChannelInterface;
import net.sf.odinms.net.world.remote.WorldRegistry;
import net.sf.odinms.scripting.event.EventScriptManager;
import net.sf.odinms.server.*;
import net.sf.odinms.server.maps.MapleMap;
import net.sf.odinms.server.maps.MapleMapFactory;
import net.sf.odinms.server.miniGame.MapleRPSGame;
import net.sf.odinms.tools.MaplePacketCreator;

import org.apache.log4j.Logger;
import org.apache.mina.core.future.CloseFuture;
import org.apache.mina.core.service.SimpleIoProcessorPool;
import org.apache.mina.core.session.IdleStatus;
import org.apache.mina.filter.codec.ProtocolCodecFilter;
import org.apache.mina.filter.executor.ExecutorFilter;
import org.apache.mina.transport.socket.nio.NioProcessor;
import org.apache.mina.transport.socket.nio.NioSession;
import org.apache.mina.transport.socket.nio.NioSocketAcceptor;
import org.logicalcobwebs.proxool.HouseKeeperController;

public class ChannelServer extends GeneralServer implements Runnable, ChannelServerMBean {

    private static final WebServer web = new WebServer();
    public static ServerContent sc = new ServerContent();
    private static int uniqueID = 1;
    private static ThreadPoolExecutor ExecutorService;
    private static Properties initialProp;
    private static final Logger log = Logger.getLogger(ChannelServer.class);
    private static WorldRegistry worldRegistry;
    /**
     * 成员变量
     */
    private PlayerStorage players = new PlayerStorage();
    private ChannelDescriptor descriptor = new ChannelDescriptor();
    private ChannelWorldInterface cwi;
    private WorldChannelInterface wci = null;
    private NioSocketAcceptor acceptor;
    private boolean shutdown = false;
    private boolean finishedShutdown = false;
    private MapleMapFactory mapFactory;
    private EventScriptManager eventSM;
    private Map<Integer, MapleGuildSummary> gsStore = new HashMap<Integer, MapleGuildSummary>();
    private Boolean worldReady = true;
    private Map<MapleSquadType, MapleSquad> mapleSquads = new HashMap<MapleSquadType, MapleSquad>();
    private ChannelsInteractionManager cim = new ChannelsInteractionManager();
    private int offlinePlayer = 0;

    private ChannelServer(String id) {
        super(new ChannelServerConfig(id));
        mapFactory = new MapleMapFactory();

    }

    //世界服务器注册
    public static WorldRegistry getWorldRegistry() {
        return worldRegistry;
    }

    public void reconnectWorld() {
        try { // check if the connection is really gone
            wci.isAvailable();
        } catch (RemoteException ex) {
            ServerExceptionHandler.HandlerRemoteException(ex);
            synchronized (worldReady) {
                worldReady = false;
            }
            synchronized (cwi) {
                synchronized (worldReady) {
                    if (worldReady) {
                        return;
                    }
                }
                log.warn("重新连接世界服务器");
                synchronized (wci) {
                    try { // completely re-establish the rmi connection 
                        wci.serverReady();
                    } catch (Exception e) {
                        log.error("Reconnecting failed", e);
                    }
                    worldReady = true;
                }
            }
            synchronized (worldReady) {
                worldReady.notifyAll();
            }
        }

    }

    @Override
    public void run() {
        try {
            cwi = new ChannelWorldInterfaceImpl(this);
            wci = worldRegistry.registerChannelServer(getConfig().getKey(), cwi);

            eventSM = new EventScriptManager(this, getConfig().getEventSM());
            getConfig().setPort(wci.getGameServerPort());
            getConfig().setIp(String.format("%s:%d", getConfig().getIp(), getConfig().getPort()));
        } catch (Exception e) {
            ServerExceptionHandler.HandlerException(e);
        }

        SimpleIoProcessorPool<NioSession> simpleIoProcessorPool = new SimpleIoProcessorPool<NioSession>(NioProcessor.class, ExecutorService);
        acceptor = new NioSocketAcceptor(ExecutorService, simpleIoProcessorPool);
        acceptor.getSessionConfig().setTcpNoDelay(true);
        acceptor.getFilterChain().addLast("codec", new ProtocolCodecFilter(new MapleCodecFactory()));
        acceptor.getFilterChain().addLast("ThreadPool", new ExecutorFilter(ExecutorService));
        acceptor.getFilterChain().addLast("executor", new ExecutorFilter(ExecutorService));
        acceptor.getSessionConfig().setIdleTime(IdleStatus.BOTH_IDLE, 30);
        TimerManager tMan = TimerManager.getInstance();
        tMan.start(Runtime.getRuntime().availableProcessors() * 2 + eventSM.getEventCount() / 10);
        tMan.register(AutobanManager.getInstance(), 60000);
        try {
            MapleServerHandler serverHandler = new MapleServerHandler(PacketProcessor.getProcessor(PacketProcessor.Mode.CHANNELSERVER), descriptor);
            acceptor.setHandler(serverHandler);
            acceptor.bind(new InetSocketAddress(getConfig().getPort()));
            log.info(String.format("世界 %d 频道服务 %d 已上线,监听于:%s.", getDescriptor().getWorld(), getDescriptor().getId(), getConfig().getIp()));
            log.info("频道 " + getChannel() + " 正常开启.监听于:" + getConfig().getIp() + ".");
            wci.serverReady();
            eventSM.init();
        } catch (IOException e) {
            log.error("端口 " + getConfig().getPort() + " 已被使用 (频道: " + getChannel() + ")", e);
        }
        CheckConfig();
    }

    public void CheckConfig() {
        GameConstants.章鱼冒险岛 = getServerName().equals("章魚Ge");
    }

    public String getServerName() {
        return getConfig().getServername();
    }

    public boolean isUseAddMaxAttack() {
        return getConfig().isUseAddMaxAttack();
    }

    private class respawnMaps implements Runnable {

        @Override
        public void run() {
            for (Entry<Integer, MapleMap> map : mapFactory.getMaps().entrySet()) {
                map.getValue().respawn();
            }
        }
    }

    public void shutdown() { // dc all clients by hand so we get sessionClosed...
        try {
            eventSM.cancel();
        } catch (Exception e) {
            ServerExceptionHandler.HandlerException(e);
        }
        shutdown = true;
        List<CloseFuture> futures = new LinkedList<CloseFuture>();
        Collection<MapleCharacter> allchars = players.getAllCharacters();
        MapleCharacter chrs[] = allchars.toArray(new MapleCharacter[allchars.size()]);
        for (MapleCharacter chr : chrs) {
            if (chr.getTrade() != null) {
                MapleTrade.cancelTrade(chr);
            }
            MapleRPSGame.cancelRPSGame(chr);
            if (chr.getEventInstance() != null) {
                chr.getEventInstance().playerDisconnected(chr);
            }
            chr.saveToDB(true);
            if (chr.getCheatTracker() != null) {
                chr.getCheatTracker().dispose();
            }
            removePlayer(chr);
        }
        for (MapleCharacter chr : chrs) {
            futures.add(chr.getClient().getSession().close(false));
        }
        for (CloseFuture future : futures) {
            future.join(500);
        }
        ChannelManager.removeChannel(descriptor);
        finishedShutdown = true;
        wci = null;
        cwi = null;
    }

    public void unbind() {
        acceptor.unbind();
    }

    public boolean hasFinishedShutdown() {
        return finishedShutdown;
    }

    public MapleMapFactory getMapFactory() {
        return mapFactory;
    }

    public static ChannelServer newInstance(String id) throws InstanceAlreadyExistsException, MBeanRegistrationException, NotCompliantMBeanException, MalformedObjectNameException {
        ChannelServer instance = new ChannelServer(id);
        MBeanServer mBeanServer = ManagementFactory.getPlatformMBeanServer();
        mBeanServer.registerMBean(instance, new ObjectName("net.sf.odinms.net.channel:type=ChannelServer,name=ChannelServer" + uniqueID++));
        return instance;
    }

    public static ChannelServer getInstance(ChannelDescriptor channel) {
        // return instances.get(channel);
        return channel == null ? null : ChannelManager.getChannelServer(channel);
    }

    public void addPlayer(MapleCharacter chr) {
        players.registerPlayer(chr);
        chr.getClient().getSession().write(MaplePacketCreator.serverMessage(getConfig().getServerMessage()));
    }

    public IPlayerStorage getPlayerStorage() {
        return players;
    }

    public void removePlayer(MapleCharacter chr) {
        players.deregisterPlayer(chr);
    }

    @Override
    public int getConnectedClients() {
        return players.getAllCharacters().size() + getOfflinePlayer();
    }

    @Override
    public String getServerMessage() {
        return getConfig().getServerMessage();
    }

    @Override
    public void setServerMessage(String newMessage) {
        getConfig().setServerMessage(newMessage);
        broadcastPacket(MaplePacketCreator.serverMessage(getServerMessage()));
    }

    public void broadcastPacket(MaplePacket data) {
        for (MapleCharacter chr : players.getAllCharacters()) {
            chr.getClient().getSession().write(data);
        }
    }

    public void broadcastGMPacket(MaplePacket data) {
        for (MapleCharacter chr : players.getAllCharacters()) {
            if (chr.isGM()) {
                chr.getClient().getSession().write(data);
            }
        }
    }

    @Override
    public int getExpRate() {
        return getConfig().getExpRate();
    }

    @Override
    public void setExpRate(int expRate) {
        getConfig().setExpRate(expRate);
    }

    public ChannelDescriptor getDescriptor() {
        return descriptor;
    }

    @Override
    public int getChannel() {
        return descriptor.getId();
    }

    public void setChannel(int channel) {
        this.descriptor.setId(channel);
        ChannelManager.addChannel(descriptor, this);
        this.mapFactory.setChannel(descriptor);
    }

    public int getWorld() {
        return descriptor.getWorld();
    }

    public void setWorld(int world) {
        this.descriptor.setWorld(world);
    }

    /**
     * 获得其他频道服务
     *
     * @param channel 频道号
     * @return
     */
    public ChannelServer getChannelServer(int channel) {
        return ChannelManager.getChannelServerFromThisWorld(descriptor, channel);
    }

    public List<ChannelServer> getChannelServers() {
        return ChannelManager.getChannelServers(descriptor);
    }

    public String getIP() {
        return getConfig().getIp();
    }

    public String getIP(int channel) {
        try {
            return getWorldInterface().getIP(channel);
        } catch (RemoteException e) {
            log.error("Lost connection to world server", e);
            ServerExceptionHandler.HandlerRemoteException(e);
            throw new RuntimeException("Lost connection to world server");
        }
    }

    public WorldChannelInterface getWorldInterface() {
        synchronized (worldReady) {
            while (!worldReady) {
                try {
                    worldReady.wait();
                } catch (InterruptedException e) {
                    ServerExceptionHandler.HandlerException(e);
                }
            }
        }
        return wci;
    }

    public boolean isShutdown() {
        return shutdown;
    }

    @Override
    public void broadcastWorldMessage(String message) {
        try {
            getWorldInterface().broadcastWorldMessage(message);
        } catch (RemoteException e) {
            ServerExceptionHandler.HandlerRemoteException(e);
            reconnectWorld();
        }
    }

    @Override
    public void shutdown(int time) {
        broadcastPacket(MaplePacketCreator.serverNotice(0, "服务器将会在 " + (time / 60000) + " 分钟后关闭维护,请各位玩家提前安全下线."));
        TimerManager.getInstance().schedule(new ShutdownServer(getDescriptor()), time);
    }

    @Override
    public void shutdownWorld(int time) {
        time *= 60000;
        try {
            getWorldInterface().shutdown(time);
        } catch (RemoteException e) {
            reconnectWorld();
        }
    }

    public EventScriptManager getEventSM() {
        return eventSM;
    }

    public void reloadEvents() {
        eventSM.cancel();
        eventSM = new EventScriptManager(this, getConfig().getEventSM());
        eventSM.init();
    }

    @Override
    public int getMesoRate() {
        return getConfig().getMesoRate();
    }

    @Override
    public void setMesoRate(int mesoRate) {
        getConfig().setMesoRate(mesoRate);
    }

    @Override
    public int getDropRate() {
        return getConfig().getDropRate();
    }

    @Override
    public void setDropRate(int dropRate) {
        getConfig().setDropRate(dropRate);
    }

    @Override
    public int getBossDropRate() {
        return getConfig().getBossdropRate();
    }

    @Override
    public void setBossDropRate(int bossdropRate) {
        getConfig().setBossdropRate(bossdropRate);
    }

    @Override
    public int getPetExpRate() {
        return getConfig().getPetExpRate();
    }

    @Override
    public void setPetExpRate(int petExpRate) {
        getConfig().setPetExpRate(petExpRate);
    }

    public boolean allowUndroppablesDrop() {
        return getConfig().isDropUndroppables();
    }

    public boolean allowMoreThanOne() {
        return getConfig().isMoreThanOne();
    }

    public boolean allowCashshop() {
        return getConfig().isCashshop();
    }

    public boolean allowMTS() {
        return getConfig().isMts();
    }

    public boolean characterNameExists(String name) {
        int size = 0;
        try {
            Connection con = DatabaseConnection.getConnection();
            PreparedStatement ps = con.prepareStatement("SELECT id FROM characters WHERE name = ?");
            ps.setString(1, name);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                size++;
            }
            ps.close();
            rs.close();
            con.close();
        } catch (SQLException e) {
            log.error("Error in charname check: \r\n" + e.toString());
        }
        return size >= 1;
    }

    public MapleGuild getGuild(MapleGuildCharacter mgc) {
        int gid = mgc.getGuildId();
        MapleGuild g = null;
        try {
            g = this.getWorldInterface().getGuild(gid, mgc);
        } catch (RemoteException re) {
            log.error("RemoteException while fetching MapleGuild.", re);
            ServerExceptionHandler.HandlerRemoteException(re);
            return null;
        }

        if (gsStore.get(gid) == null) {
            gsStore.put(gid, new MapleGuildSummary(g));
        }
        return g;
    }

    public MapleGuildSummary getGuildSummary(int gid) {
        if (gsStore.containsKey(gid)) {
            return gsStore.get(gid);
        } else { //this shouldn't happen much, if ever, but if we're caught, without the summary, we'll have to do a worldop
            try {
                MapleGuild g = this.getWorldInterface().getGuild(gid, null);
                if (g != null) {
                    gsStore.put(gid, new MapleGuildSummary(g));
                }
                return gsStore.get(gid);	//if g is null, we will end up returning null
            } catch (RemoteException re) {
                log.error("RemoteException while fetching GuildSummary.", re);
                ServerExceptionHandler.HandlerRemoteException(re);
                return null;
            }
        }
    }

    public void updateGuildSummary(int gid, MapleGuildSummary mgs) {
        gsStore.put(gid, mgs);
    }

    public void reloadGuildSummary() {
        try {
            MapleGuild g;
            for (int i : gsStore.keySet()) {
                g = this.getWorldInterface().getGuild(i, null);
                if (g != null) {
                    gsStore.put(i, new MapleGuildSummary(g));
                } else {
                    gsStore.remove(i);
                }
            }
        } catch (RemoteException re) {
            log.error("RemoteException while reloading GuildSummary.", re);
            ServerExceptionHandler.HandlerRemoteException(re);
        }
    }

    public static ThreadPoolExecutor getExecutorService() {
        return ExecutorService;
    }

    public static void main(String args[]) throws FileNotFoundException, IOException, NotBoundException, InstanceAlreadyExistsException, MBeanRegistrationException, NotCompliantMBeanException, MalformedObjectNameException, SQLException, InterruptedException {
        if (CheckFilePermit()) {
            log.error("机器码验证失败......");
            System.exit(0);
        }
        System.setProperty("java.rmi.server.hostname", "127.0.0.1");
        Runtime.getRuntime().addShutdownHook(new ChannelServerExitThread());

        Connection c;
        PreparedStatement ps;
        try {
            c = DatabaseConnection.getConnection();
            ps = c.prepareStatement("UPDATE accounts SET loggedin = 0");
            ps.executeUpdate();
            ps.close();
            ps = c.prepareStatement("UPDATE characters SET loggedin = 0, muted = 0");
            ps.executeUpdate();
            ps.close();
            ps = c.prepareStatement("UPDATE hiredmerchant SET onSale = false");
            ps.executeUpdate();
            ps.close();
            ps = c.prepareStatement("UPDATE characters SET HasMerchant = 0");
            ps.executeUpdate();
            ps.close();

            c.close();
        } catch (SQLException ex) {
            log.error("Could not reset databases", ex);
        }

        ExecutorService = new ThreadPoolExecutor(
                0,
                getMaxiNumPoolSize(GeneralServerType.CHANNEL),
                2,
                TimeUnit.MINUTES,
                new SynchronousQueue<Runnable>(true),
                new ThreadFactory() {
            private final AtomicInteger threadNumber = new AtomicInteger(1);

            @Override
            public Thread newThread(Runnable r) {
                Thread thread = new Thread(null, r, "ChannelServer[" + threadNumber.getAndIncrement()
                        + "]");
                if (log.isDebugEnabled()) {
                    log.info("CREATE THREAD：" + thread.getName());
                }
                return thread;
            }
        }, new ThreadPoolExecutor.CallerRunsPolicy());

        ChannelServerConfig config = new ChannelServerConfig();
        config.initConfig(_ConfigName, null);

        Registry registry = LocateRegistry.getRegistry(config.getWorldHost(), Registry.REGISTRY_PORT, new SslRMIClientSocketFactory());
        worldRegistry = (WorldRegistry) registry.lookup("WorldRegistry");
        log.info("Load MapleItemInformationProvider");
        MapleItemInformationProvider.getInstance();
        HouseKeeperController.startall();
        int worlds = config.getWorlds();
        int channels = config.getChannels();
        ChannelManager.init(worlds);
        for (int j = 0; j < worlds; j++) {
            for (int i = 0; i < channels; i++) {
                ChannelServer cs = newInstance(String.format("%d%d", j, i));
                cs.setWorld(j);
                cs.run();
            }
        }

        sc.show();

        CommandProcessor.registerMBean();
        //关服务自动存档结束..<
        log.info("初始化游戏资料缓存...（加快服务器运行速度。）");
        log.warn("请不要在服务端安装任何杀毒监控软件。如360等软件。安装后变卡。出现故障一律不负责。");
        MapleMapFactory.InitCache();
        SkillFactory.InitCache();

        ItemOptionFactory.getOption(0);
        log.info("正在初始化 WEBSERVER.");
        web.start(is64());
        log.info("初始化已完成.");
        System.gc();

        //建立缓存结束.
    }

    public MapleSquad getMapleSquad(MapleSquadType type) {
        if (mapleSquads.containsKey(type)) {
            return mapleSquads.get(type);
        } else {
            return null;
        }
    }

    public boolean addMapleSquad(MapleSquad squad, MapleSquadType type) {
        if (mapleSquads.get(type) == null) {
            mapleSquads.remove(type);
            mapleSquads.put(type, squad);
            return true;
        } else {
            return false;
        }
    }

    public boolean removeMapleSquad(MapleSquad squad, MapleSquadType type) {
        if (mapleSquads.containsKey(type)) {
            if (mapleSquads.get(type) == squad) {
                mapleSquads.remove(type);
                return true;
            }
        }
        return false;
    }

    public ChannelsInteractionManager getCim() {
        return cim;
    }

    public List<MapleCharacter> getPartyMembers(MapleParty party) {
        List<MapleCharacter> partym = new LinkedList<MapleCharacter>();
        for (net.sf.odinms.net.world.MaplePartyCharacter partychar : party.getMembers()) {
            if (partychar.getChannel() == getChannel()) { // Make sure the thing doesn't get duplicate plays due to ccing bug.
                MapleCharacter chr = getPlayerStorage().getCharacterByName(partychar.getName());
                if (chr != null) {
                    partym.add(chr);
                }
            }
        }
        return partym;
    }

    public List<MapleCharacter> getAllCharsWithPlayerNPCs() {
        List<MapleCharacter> ret = new ArrayList<MapleCharacter>();
        for (MapleCharacter chr : getPlayerStorage().getAllCharacters()) {
            if (chr.hasPlayerNPC()) {
                ret.add(chr);
            }
        }
        return ret;
    }

    public void loadMap(int mapid) {
        mapFactory.getMap(mapid);
    }

    public void AutoGain(int jsexp) {
        AutoGain(jsexp, false);
    }

    public void AutoGain(int jsexp, boolean isnx) {
        AutoGain(jsexp, isnx, 1);
    }

    public void AutoGain(int jsexp, boolean isnx, int rate) {
        mapFactory.getMap(910000000).AutoGain(jsexp, isnx, rate);
    }

    public void AutoNx(int jsNx) {
        mapFactory.getMap(910000000).AutoNx(jsNx);
    }

    public MapleCharacter getCharacterFromAllServers(int id) {
        for (ChannelServer cserv_ : getChannelServers()) {
            MapleCharacter ret = cserv_.getPlayerStorage().getCharacterById(id);
            if (ret != null) {
                return ret;
            }
        }
        return null;
    }

    public static MapleCharacter getCharacterFromAllServersAndWorld(int id) {
        for (ChannelServer cserv_ : ChannelManager.getAllChannelServers()) {
            MapleCharacter ret = cserv_.getPlayerStorage().getCharacterById(id);
            if (ret != null) {
                return ret;
            }
        }
        return null;
    }

    public MapleCharacter getCharacterFromAllServers(String name) {
        for (ChannelServer cserv_ : getChannelServers()) {
            MapleCharacter ret = cserv_.getPlayerStorage().getCharacterByName(name);
            if (ret != null) {
                return ret;
            }
        }
        return null;
    }

    public static MapleCharacter getCharacterFromAllServersAndWorld(String name) {
        for (ChannelServer cserv_ : ChannelManager.getAllChannelServers()) {
            MapleCharacter ret = cserv_.getPlayerStorage().getCharacterByName(name);
            if (ret != null) {
                return ret;
            }
        }
        return null;
    }

    public boolean hasHiredMerchant(int characterid) {
        for (ChannelServer cs : getChannelServers()) {
            if (cs.cim.contains(characterid)) {
                return true;
            }
        }
        return false;
    }

    protected void setShutdown(boolean shutdown) {
        this.shutdown = shutdown;
    }

    @Override
    public String toString() {
        return String.format("世界：【%d】 频道：【%d】端口：【%d】IP：【%s】", descriptor.getWorld(), descriptor.getId(), getConfig().getPort(), getConfig().getIp());
    }

    public static WebServer getWebServer() {
        return web;
    }

    public int getOfflinePlayer() {
        return offlinePlayer;
    }

    public void addOfflinePlayer() {
        offlinePlayer++;
    }

    public void removeOfflinePlayer() {
        if (offlinePlayer > 0) {
            offlinePlayer--;
        }
    }

    public boolean isGateway() {
        return getConfig().isGateway();
    }

    public NioSocketAcceptor getAcceptor() {
        return acceptor;
    }

    @Override
    public GeneralServerType getServerType() {
        return GeneralServerType.CHANNEL;
    }

    @Override
    public ChannelServerConfig getConfig() {
        return (ChannelServerConfig) _Config;
    }
}
