/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package net.sf.odinms.net.world;

import java.rmi.ConnectException;
import java.rmi.RemoteException;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;
import net.sf.odinms.database.DatabaseConnection;
import net.sf.odinms.net.channel.ChannelDescriptor;
import net.sf.odinms.net.channel.remote.ChannelWorldInterface;
import net.sf.odinms.net.world.remote.WorldChannelInterface;
import net.sf.odinms.server.ServerExceptionHandler;
import org.apache.log4j.Logger;

/**
 * 世界服务频道服务管理器
 *
 * @author hxms
 */
public class ChannelServerStorage implements java.io.Serializable {

	private static Integer port;
	private final static Logger log = Logger
			.getLogger(ChannelServerStorage.class);
	private Map<ChannelDescriptor, ChannelWorldInterface> channels = new ConcurrentHashMap<ChannelDescriptor, ChannelWorldInterface>();
	private Map<Integer, ChannelList> index = new ConcurrentHashMap<Integer, ChannelList>();

	static {
		try {
			port = 7575;
		} catch (Exception ex) {
			log.error("频道端口设置不正确!", ex);
		}
	}

	public WorldChannelInterface registerChannelServer(String authKey,
			ChannelWorldInterface cb) {
		WorldChannelInterface resultChannelInterface = null;
		try {
			ChannelDescriptor channelDescriptor = cb.getDescriptor();
			Connection con = DatabaseConnection.getConnection();
			PreparedStatement ps = con
					.prepareStatement("SELECT * FROM channels WHERE `key` = SHA1(?) AND world = ?");
			ps.setString(1, authKey);
			ps.setInt(2, channelDescriptor.getWorld());
			ResultSet rs = ps.executeQuery();
			if (rs.next()) {
				channelDescriptor.setId(rs.getInt("number"));
				if (channels.containsKey(channelDescriptor)) {
					try {
						channels.get(channelDescriptor).shutdown(0);
					} catch (ConnectException ce) {
						log.error(channelDescriptor + " 已注册.尝试关闭旧服务连接失败.");
					}
				}
				cb.setChannelId(channelDescriptor.getId());
				channels.put(channelDescriptor, cb);

				if (!index.containsKey(channelDescriptor.getWorld())) {
					index.put(channelDescriptor.getWorld(), new ChannelList());
				}
				index.get(channelDescriptor.getWorld()).add(channelDescriptor,
						cb);
				resultChannelInterface = new WorldChannelInterfaceImpl(cb);
			}
			rs.close();
			ps.close();
			con.close();
		} catch (SQLException ex) {
			ServerExceptionHandler.HandlerSqlException(ex);
		} catch (RemoteException ex) {
			ServerExceptionHandler.HandlerRemoteException(ex);
		}
		return resultChannelInterface;
	}

	public void deregisterChannelServer(ChannelDescriptor channelDescriptor) {
		channels.remove(channelDescriptor);
		index.get(channelDescriptor.getWorld()).remove(channelDescriptor);
		log.info(String.format("世界频道 ：%d  频道：%d 已关闭.",
				channelDescriptor.getWorld(), channelDescriptor.getId()));
	}

	public java.util.Collection<ChannelWorldInterface> getServers() {
		return channels.values();
	}

	public static Integer getPort() {
		return port;
	}

	public java.util.Collection<ChannelDescriptor> getChannelDescriptors() {
		return channels.keySet();
	}

	public ChannelWorldInterface getChannelWorldInterface(
			ChannelDescriptor channelDescriptor) {
		return channels.get(channelDescriptor);
	}

	/**
	 * 获得Map集合
	 *
	 * @return
	 */
	public Map<ChannelDescriptor, ChannelWorldInterface> getChannels() {
		return channels;
	}

	public ChannelList getChannelWorldInterfaces(int world) {
		return index.get(world);
	}

	public List<Integer> getChannelNumber(int world) {
		ArrayList<Integer> list = new ArrayList<Integer>();
		for (ChannelDescriptor channelDescriptor : channels.keySet()) {
			if (channelDescriptor.getWorld() == world) {
				list.add(channelDescriptor.getId());
			}
		}
		return list;
	}
}
