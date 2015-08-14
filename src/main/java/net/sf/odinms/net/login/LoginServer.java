package net.sf.odinms.net.login;

import net.sf.odinms.net.login.gateway.LoginGateway;

import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.net.InetSocketAddress;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import javax.management.MBeanServer;
import javax.management.ObjectName;
import javax.rmi.ssl.SslRMIClientSocketFactory;

import net.sf.odinms.database.DatabaseConnection;
import net.sf.odinms.net.MapleServerHandler;
import net.sf.odinms.net.PacketProcessor;
import net.sf.odinms.net.channel.ChannelDescriptor;
import net.sf.odinms.net.login.remote.ChannelLoadInfo;
import net.sf.odinms.net.login.remote.LoginWorldInterface;
import net.sf.odinms.net.mina.MapleCodecFactory;
import net.sf.odinms.net.servlet.GeneralServer;
import net.sf.odinms.net.servlet.GeneralServerType;
import net.sf.odinms.net.servlet.LoginServerConfig;
import net.sf.odinms.net.world.remote.WorldLoginInterface;
import net.sf.odinms.net.world.remote.WorldRegistry;
import net.sf.odinms.server.MapleItemInformationProvider;
import net.sf.odinms.server.ServerExceptionHandler;
import net.sf.odinms.server.TimerManager;

import org.apache.mina.core.service.SimpleIoProcessorPool;
import org.apache.mina.core.session.IdleStatus;
import org.apache.mina.filter.codec.ProtocolCodecFilter;
import org.apache.mina.filter.executor.ExecutorFilter;
import org.apache.mina.transport.socket.nio.NioProcessor;
import org.apache.mina.transport.socket.nio.NioSession;
import org.apache.mina.transport.socket.nio.NioSocketAcceptor;

public class LoginServer extends GeneralServer implements Runnable, LoginServerMBean {

    private static final LoginServer instance = new LoginServer();
    public static ThreadPoolExecutor ExecutorService = null;
    private static final org.apache.log4j.Logger log = org.apache.log4j.Logger.getLogger(LoginServer.class);
    private static WorldRegistry worldRegistry = null;
    private NioSocketAcceptor acceptor;
    private LoginServerStorage storage = new LoginServerStorage();
    private LoginWorldInterface lwi;
    private WorldLoginInterface wli;
    private Boolean worldReady = Boolean.TRUE;
    private Map<String, Integer> connectedIps = new HashMap<String, Integer>();

    static {
        MBeanServer mBeanServer = ManagementFactory.getPlatformMBeanServer();
        try {
            mBeanServer.registerMBean(instance, new ObjectName("net.sf.odinms.net.login:type=LoginServer,name=LoginServer"));
        } catch (Exception e) {
            log.error("MBEAN ERROR", e);
        }
    }

    private LoginServer() {
        super(new LoginServerConfig());
    }

    public static LoginServer getInstance() {
        return instance;
    }

    public void addChannel(ChannelDescriptor channel, String ip) {
        storage.addChannel(channel, ip);
    }

    public void removeChannel(ChannelDescriptor channel) {
        storage.removeChannel(channel);
    }

    public String getIP(ChannelDescriptor channel) {
        String ip = storage.getIP(channel);
        if (ip == null) {
            log.error("无法获得 世界:" + channel.getWorld() + "-频道:" + channel.getId() + " 的IP地址。");
        }
        return ip;
    }

    @Override
    public int getPossibleLogins() {
        int ret = 0;
        try {
            Connection con = DatabaseConnection.getConnection();
            PreparedStatement ps = con.prepareStatement("SELECT COUNT(*) FROM accounts WHERE loggedin > 1 AND gm = 0");
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                int usersOn = rs.getInt(1);
                if (usersOn < getConfig().getUserLimit()) {
                    ret = getConfig().getUserLimit() - usersOn;
                }
            }
            rs.close();
            ps.close();
            con.close();
        } catch (Exception ex) {
            log.error("loginlimit error", ex);
        }
        return ret;
    }

    public int getInitMapId() {
        return getConfig().getInitMapId();
    }

    public void reconnectWorld() {
        try {
            wli.isAvailable(); // check if the connection is really gone
        } catch (RemoteException ex) {
            ServerExceptionHandler.HandlerRemoteException(ex);
            synchronized (worldReady) {
                worldReady = Boolean.FALSE;
            }
            synchronized (lwi) {
                synchronized (worldReady) {
                    if (worldReady) {
                        return;
                    }
                }
                log.warn("Reconnecting to world server");
                synchronized (wli) {
                    // completely re-establish the rmi connection
                    try {
                        getConfig().initConfig(_ConfigName, this);
                        Registry registry = LocateRegistry.getRegistry(getConfig().getWorldHost(),
                                Registry.REGISTRY_PORT, new SslRMIClientSocketFactory());
                        worldRegistry = (WorldRegistry) registry.lookup("WorldRegistry");
                        lwi = new LoginWorldInterfaceImpl();
                        wli = worldRegistry.registerLoginServer(getConfig().getKey(), lwi);
                    } catch (Exception e) {
                        log.error("Reconnecting failed", e);
                    }
                    worldReady = Boolean.TRUE;
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
            Registry registry = LocateRegistry.getRegistry(getConfig().getWorldHost(),
                    Registry.REGISTRY_PORT, new SslRMIClientSocketFactory());
            worldRegistry = (WorldRegistry) registry.lookup("WorldRegistry");
            lwi = new LoginWorldInterfaceImpl();
            wli = worldRegistry.registerLoginServer(getConfig().getKey(), lwi);

        } catch (Exception e) {
            throw new RuntimeException("Could not connect to world server.", e);
        }
        SimpleIoProcessorPool<NioSession> simpleIoProcessorPool = new SimpleIoProcessorPool<NioSession>(NioProcessor.class, ExecutorService);
        acceptor = new NioSocketAcceptor(ExecutorService, simpleIoProcessorPool);
        acceptor.getSessionConfig().setTcpNoDelay(true);
        acceptor.getFilterChain().addLast("codec", new ProtocolCodecFilter(new MapleCodecFactory()));
        acceptor.getFilterChain().addLast("ThreadPool", new ExecutorFilter(ExecutorService));
        acceptor.getFilterChain().addLast("executor", new ExecutorFilter(ExecutorService));
        acceptor.getSessionConfig().setIdleTime(IdleStatus.BOTH_IDLE, 40);
        TimerManager tMan = TimerManager.getInstance();
        tMan.start();
        tMan.register(new RankingWorker(), getConfig().getRankingInterval());
        int PORT = getConfig().getPort();
        try {
            acceptor.setHandler(new MapleServerHandler(PacketProcessor.getProcessor(PacketProcessor.Mode.LOGINSERVER)));
            acceptor.bind(new InetSocketAddress(PORT));
            log.info("登陆端口监听于 " + PORT);
        } catch (IOException e) {
            log.error("端口 " + PORT + " 已被使用", e);
        }
        LoginGateway gateway = LoginGateway.getInstance();
        if (gateway != null) {
            try {
                gateway.setLoginServerPort(PORT);
                gateway.run();
            } catch (Exception e) {
                log.error("网关端口 " + gateway.getPort() + " 已被使用", e);
            }
        }
    }

    public void shutdown() {
        log.info("重启服务器...");
        try {
            worldRegistry.deregisterLoginServer(lwi);
        } catch (RemoteException e) {
            ServerExceptionHandler.HandlerRemoteException(e);
        }
        TimerManager.getInstance().stop();
        System.exit(0);
    }

    public WorldLoginInterface getWorldInterface() {
        return wli;
    }

    public static void main(String args[]) {
        try {
            if (CheckFilePermit()) {
                return;
            }
            System.setProperty("java.rmi.server.hostname", "127.0.0.1");
            ExecutorService = new ThreadPoolExecutor(
                    0,
                    LoginServer.getInstance().getMaxiNumPoolSize(),
                    2,
                    TimeUnit.MINUTES,
                    new SynchronousQueue<Runnable>(true),
                    new ThreadFactory() {
                private final AtomicInteger threadNumber = new AtomicInteger(1);

                @Override
                public Thread newThread(Runnable r) {
                    Thread thread = new Thread(r);
                    thread.setName("LoginServer[" + threadNumber.getAndIncrement()
                            + "]");
                    if (log.isDebugEnabled()) {
                        log.info("CREATE THREAD：" + thread.getName());
                    }
                    return thread;
                }
            }, new ThreadPoolExecutor.CallerRunsPolicy());
            MapleItemInformationProvider.getInstance();
            LoginServer.getInstance().run();
        } catch (Exception ex) {
            log.error("Error initializing loginserver", ex);
        }
    }

    @Override
    public int getUserLimit() {
        return getConfig().getUserLimit();
    }

    public String getServerName() {
        return getConfig().getServerName();
    }

    @Override
    public String getEventMessage() {
        return getConfig().getEventMessage();
    }

    @Override
    public int getFlag() {
        return getConfig().getFlag();
    }

    public int getMaxCharacters() {
        return getConfig().getMaxCharacters();
    }

    public ChannelLoadInfo getLoad() {
        return storage.getLoads();
    }

    public void setLoad(ChannelLoadInfo load) {
        this.storage.setLoads(load);
    }

    public synchronized void addConnectedIP(String ip) {
        if (connectedIps.containsKey(ip)) {
            int connections = connectedIps.get(ip);
            connectedIps.remove(ip);
            connectedIps.put(ip, connections + 1);
        } else { // first connection from ip
            connectedIps.put(ip, 1);
        }
    }

    public synchronized void removeConnectedIp(String ip) {
        if (connectedIps.containsKey(ip)) {
            int connections = connectedIps.get(ip);
            connectedIps.remove(ip);
            if (connections - 1 != 0) {
                connectedIps.put(ip, connections - 1);
            }
        }
    }

    public synchronized boolean ipCanConnect(String ip) {
        if (connectedIps.containsKey(ip)) {
            if (connectedIps.get(ip) >= 20) {
                return false;
            }
        }
        return true;
    }

    @Override
    public void setEventMessage(String newMessage) {
        getConfig().setEventMessage(newMessage);
    }

    @Override
    public void setFlag(int newflag) {
        getConfig().setFlag(newflag);
    }

    @Override
    public int getNumberOfSessions() {
        return acceptor.getManagedSessionCount();
    }

    @Override
    public void setUserLimit(int newLimit) {
        getConfig().setUserLimit(newLimit);
    }

    @Override
    public GeneralServerType getServerType() {
        return GeneralServerType.LOGIN;
    }

    @Override
    public LoginServerConfig getConfig() {
        return (LoginServerConfig) _Config;//To change body of generated methods, choose Tools | Templates.
    }
}
