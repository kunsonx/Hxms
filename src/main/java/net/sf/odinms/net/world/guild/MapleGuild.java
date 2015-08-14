/*
 This file is part of the OdinMS Maple Story Server
 Copyright (C) 2008 Patrick Huy <patrick.huy@frz.cc> 
 Matthias Butz <matze@odinms.de>
 Jan Christian Meyer <vimes@odinms.de>

 This program is free software: you can redistribute it and/or modify
 it under the terms of the GNU Affero General Public License version 3
 as published by the Free Software Foundation. You may not use, modify
 or distribute this program under any other version of the
 GNU Affero General Public License.

 This program is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU Affero General Public License for more details.

 You should have received a copy of the GNU Affero General Public License
 along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package net.sf.odinms.net.world.guild;

import java.io.FileReader;
import java.io.InputStreamReader;
import java.rmi.RemoteException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;
import net.sf.odinms.client.MapleCharacter;
import net.sf.odinms.client.MapleClient;
import net.sf.odinms.database.DatabaseConnection;
import net.sf.odinms.net.MaplePacket;
import net.sf.odinms.net.channel.ChannelDescriptor;
import net.sf.odinms.net.channel.ChannelManager;
import net.sf.odinms.net.channel.ChannelServer;
import net.sf.odinms.net.channel.remote.ChannelWorldInterface;
import net.sf.odinms.net.world.ChannelList;
import net.sf.odinms.net.world.WorldRegistryImpl;
import net.sf.odinms.server.ServerExceptionHandler;
import net.sf.odinms.tools.Guild.MapleGuild_Msg;
import net.sf.odinms.tools.MaplePacketCreator;
import net.sf.odinms.tools.data.output.MaplePacketLittleEndianWriter;
import org.apache.log4j.Logger;

public final class MapleGuild implements java.io.Serializable {

	private static Logger log = Logger.getLogger(MapleGuild.class);
	public final static int CREATE_GUILD_COST = 5000000; // 5 mil to start a
															// guild
	public final static int CHANGE_EMBLEM_COST = 10000000;
	public final static int INCREASE_CAPACITY_COST = 5000000; // every 5 slots
	public final static boolean ENABLE_BBS = true;

	private enum BCOp {

		NONE, DISBAND, EMBELMCHANGE
	}

	public static final long serialVersionUID = 6322150443228168192L;
	private List<MapleGuildCharacter> members;
	private String rankTitles[] = new String[5]; // 1 = master, 2 = jr, 5 =
													// lowest member
	private String name;
	private int id;
	private int gp;
	private int logo;
	private int logoColor;
	private int leader;
	private int capacity;
	private int logoBG;
	private int logoBGColor;
	private String notice;
	private int signature;
	private Map<Integer, List<Integer>> notifications = new LinkedHashMap<Integer, List<Integer>>();
	private boolean bDirty = true;
	private int allianceId;
	private int world;

	// initiator is one of two things
	// 1. the leader when he/she first makes the guild
	// 2. the first person logging on when the server does not have
	// the guild loaded
	public MapleGuild(int guildid, MapleGuildCharacter initiator) {
		members = new ArrayList<MapleGuildCharacter>();
		Connection con;
		try {
			con = DatabaseConnection.getConnection();
		} catch (Exception e) {
			log.error(
					"unable to connect to database to load guild information.",
					e);
			return;
		}
		try {
			PreparedStatement ps = con
					.prepareStatement("SELECT * FROM guilds WHERE guildid="
							+ guildid);
			ResultSet rs = ps.executeQuery();
			if (!rs.first()) {
				// log.error("no result returned from guildid, id = " +
				// guildid);
				id = -1;
				return;
			}

			id = guildid;
			name = rs.getString("name");
			gp = rs.getInt("GP");
			logo = rs.getInt("logo");
			logoColor = rs.getInt("logoColor");
			logoBG = rs.getInt("logoBG");
			logoBGColor = rs.getInt("logoBGColor");
			capacity = rs.getInt("capacity");
			for (int i = 1; i <= 5; i++) {
				rankTitles[i - 1] = rs.getString("rank" + i + "title");
			}
			leader = rs.getInt("leader");
			notice = rs.getString("notice");
			signature = rs.getInt("signature");
			allianceId = rs.getInt("allianceId");
			ps.close();
			rs.close();

			ps = con.prepareStatement("select world from characters where id = ?");
			ps.setInt(1, leader);
			rs = ps.executeQuery();
			if (rs.next()) {
				world = rs.getInt(1);
			}
			rs.close();
			ps.close();

			// add guild members
			ps = con.prepareStatement("SELECT id, name, level, job, guildrank, allianceRank FROM characters WHERE guildid = ? ORDER BY guildrank ASC, name ASC");
			ps.setInt(1, guildid);
			rs = ps.executeQuery();
			if (!rs.first()) {
				log.error("No members in guild.  Impossible...");
				return;
			}
			do {
				members.add(new MapleGuildCharacter(rs.getInt("id"), rs
						.getInt("level"), rs.getString("name"),
						new ChannelDescriptor(0, world), rs.getInt("job"), rs
								.getInt("guildrank"), guildid, false, rs
								.getInt("allianceRank")));
			} while (rs.next());

			if (initiator != null) {
				setOnline(initiator.getId(), true, initiator.getChannel());
			}
			ps.close();
			rs.close();
			con.close();
		} catch (SQLException se) {
			log.error("unable to read guild information from sql", se);
		}
	}

	public int getLeader() {
		return leader;
	}

	public void setLeader(int le) {
		// int frank = 0;
		// for (MapleGuildCharacter mapleGuildCharacter : members) {
		// if (mapleGuildCharacter.getId() == this.leader) {
		// frank = mapleGuildCharacter.getGuildRank();
		// mapleGuildCharacter.setGuildRank(0);
		// }
		// }
		this.leader = le;
		// for (MapleGuildCharacter mapleGuildCharacter : members) {
		// if (mapleGuildCharacter.getId() == this.leader) {
		// mapleGuildCharacter.setGuildRank(frank);
		// }
		// }
	}

	public void setLeader(int l, MapleClient c, int fromchrid) {
		// setLeader(l);
		try {
			c.getChannelServer().getWorldInterface()
					.setGuildLeader(id, l, fromchrid);
		} catch (RemoteException e) {
			ServerExceptionHandler.HandlerRemoteException(e);
		}
	}

	public void gainGP(int amount) {
		this.gp += amount;
		this.gp = Math.max(0, this.gp);
		this.writeToDB();
		this.broadcast(MaplePacketCreator.updateGP(this.id, this.gp));
		this.guildMessage(MaplePacketCreator.updateGP(this.id, this.gp));
		this.broadcast(MaplePacketCreator.serverNotice(5, "[注意]家族获得 " + amount
				+ " GP点"));
	}

	public static void showlvl(MapleClient c, int npcid) {
		try {
			Connection con = DatabaseConnection.getConnection();
			PreparedStatement ps = con
					.prepareStatement("SELECT `name`, `level`, `str`, `dex`, "
							+ "`int`, `luk` FROM characters ORDER BY `level` DESC LIMIT 100");
			ResultSet rs = ps.executeQuery();
			c.getSession().write(MaplePacketCreator.showGuildRanks3(npcid, rs));
			ps.close();
			rs.close();
			con.close();
		} catch (Exception e) {
			log.error("failed to display guild ranks.", e);
		}

	}

	public static void showjf(MapleClient c, int npcid) {
		try {
			Connection con = DatabaseConnection.getConnection();
			PreparedStatement ps = con
					.prepareStatement("SELECT `name`, `jf`, `money`, `loggedin`, "
							+ "`banned`, `greason` FROM accounts ORDER BY `jf` DESC LIMIT 30");
			ResultSet rs = ps.executeQuery();
			c.getSession().write(MaplePacketCreator.showGuildRanks8(npcid, rs));
			ps.close();
			rs.close();
			con.close();
		} catch (Exception e) {
			log.error("failed to display guild ranks.", e);
		}

	}

	public static void showjj(MapleClient c, int npcid) {
		try {
			Connection con = DatabaseConnection.getConnection();
			PreparedStatement ps = con
					.prepareStatement("SELECT `name`, `jj`, `GP`, `logo`, "
							+ "`logocolor`, `logo` FROM guilds ORDER BY `jj` DESC LIMIT 30");
			ResultSet rs = ps.executeQuery();
			c.getSession().write(MaplePacketCreator.showGuildRanks9(npcid, rs));
			ps.close();
			rs.close();
			con.close();
		} catch (Exception e) {
			log.error("failed to display guild ranks.", e);
		}

	}

	public static void showfame(MapleClient c, int npcid) {
		try {
			Connection con = DatabaseConnection.getConnection();
			PreparedStatement ps = con
					.prepareStatement("SELECT `name`, `fame`, `str`, `dex`, "
							+ "`int`, `luk` FROM characters ORDER BY `fame` DESC LIMIT 50");
			ResultSet rs = ps.executeQuery();
			c.getSession().write(MaplePacketCreator.showGuildRanks2(npcid, rs));
			ps.close();
			rs.close();
			con.close();
		} catch (Exception e) {
			log.error("failed to display guild ranks.", e);
		}

	}

	public static void showreborns(MapleClient c, int npcid) {
		try {
			Connection con = DatabaseConnection.getConnection();
			PreparedStatement ps = con
					.prepareStatement("SELECT `name`, `reborns`, `str`, `dex`, "
							+ "`int`, `luk` FROM characters ORDER BY `zs` DESC LIMIT 50");
			ResultSet rs = ps.executeQuery();
			c.getSession().write(MaplePacketCreator.showGuildRanks5(npcid, rs));
			ps.close();
			rs.close();
			con.close();
		} catch (Exception e) {
			log.error("failed to display guild ranks.", e);
		}

	}

	public void buildNotifications() {
		List<Integer> chs = WorldRegistryImpl.getInstance()
				.getChannelServerStorage().getChannelNumber(world);
		if (notifications.keySet().size() != chs.size()) {
			notifications.clear();
			for (Integer ch : chs) {
				notifications.put(ch, new java.util.LinkedList<Integer>());
			}
		} else {
			for (List<Integer> l : notifications.values()) {
				l.clear();
			}
		}
		synchronized (members) {
			for (MapleGuildCharacter mgc : members) {
				if (!mgc.isOnline()) {
					continue;
				}
				List<Integer> ch = notifications.get(Integer.valueOf(mgc
						.getChannel()));
				if (ch == null) {
					log.warn("Unable to connect to channel " + mgc.getChannel());
				} else {
					ch.add(mgc.getId());
				}
			}
		}

		bDirty = false;
	}

	public void writeToDB() {
		writeToDB(false);
	}

	public void writeToDB(boolean bDisband) {
		Connection con;
		try {
			con = DatabaseConnection.getConnection();
		} catch (Exception e) {
			log.error(
					"unable to connect to database to write guild information.",
					e);
			return;
		}
		try {
			if (!bDisband) {
				String sql = "UPDATE guilds SET " + "GP = ?, " + "logo = ?, "
						+ "logoColor = ?, " + "logoBG = ?, "
						+ "logoBGColor = ?, ";
				for (int i = 0; i < 5; i++) {
					sql += "rank" + (i + 1) + "title = ?, ";
				}
				sql += "capacity = ?, " + "notice = ? , leader = ? " + ""
						+ "WHERE guildid = ?";
				PreparedStatement ps = con.prepareStatement(sql);
				ps.setInt(1, gp);
				ps.setInt(2, logo);
				ps.setInt(3, logoColor);
				ps.setInt(4, logoBG);
				ps.setInt(5, logoBGColor);
				for (int i = 6; i < 11; i++) {
					ps.setString(i, rankTitles[i - 6]);
				}
				ps.setInt(11, capacity);
				ps.setString(12, notice);
				ps.setInt(13, leader);
				ps.setInt(14, this.id);

				ps.execute();
				ps.close();
			} else {
				PreparedStatement ps = con
						.prepareStatement("UPDATE characters SET guildid = 0, guildrank = 5 WHERE guildid = ?");
				ps.setInt(1, this.id);
				ps.execute();
				ps.close();
				ps = con.prepareStatement("DELETE FROM guilds WHERE guildid = ?");
				ps.setInt(1, this.id);
				ps.execute();
				ps.close();
				this.broadcast(MapleGuild_Msg.guildDisband(this.id));
			}
			con.close();
		} catch (SQLException se) {
			log.error(se.getLocalizedMessage(), se);
		}
	}

	public int getId() {
		return id;
	}

	public int getLeaderId() {
		return leader;
	}

	public int getGP() {
		return gp;
	}

	public int getLogo() {
		return logo;
	}

	public void setLogo(int l) {
		logo = l;
	}

	public int getLogoColor() {
		return logoColor;
	}

	public void setLogoColor(int c) {
		logoColor = c;
	}

	public int getLogoBG() {
		return logoBG;
	}

	public void setLogoBG(int bg) {
		logoBG = bg;
	}

	public int getLogoBGColor() {
		return logoBGColor;
	}

	public void setLogoBGColor(int c) {
		logoBGColor = c;
	}

	public String getNotice() {
		if (notice == null) {
			return "";
		}
		return notice;
	}

	public String getName() {
		return name;
	}

	public java.util.Collection<MapleGuildCharacter> getMembers() {
		return java.util.Collections.unmodifiableCollection(members);
	}

	public int getCapacity() {
		return capacity;
	}

	public int getSignature() {
		return signature;
	}

	public void broadcast(MaplePacket packet) {
		broadcast(packet, -1, BCOp.NONE);
	}

	public void broadcast(MaplePacket packet, int exception) {
		broadcast(packet, exception, BCOp.NONE);
	}

	// multi-purpose function that reaches every member of guild
	// (except the character with exceptionId)
	// in all channels with as little access to rmi as possible
	public void broadcast(MaplePacket packet, int exceptionId, BCOp bcop) {
		synchronized (notifications) {
			if (bDirty) {
				buildNotifications();
			}
			// now call the channelworldinterface
			try {
				ChannelList list = WorldRegistryImpl.getInstance()
						.getChannelList(world);
				List<ChannelServer> servers = null;
				if (list == null) {
					servers = ChannelManager.getChannelServers(world);
				}
				Iterator it = (servers != null ? servers.iterator() : list
						.getChannelDescriptors().iterator());
				for (; it.hasNext();) {
					Object next = it.next();
					ChannelDescriptor descriptor = (next instanceof ChannelServer ? ((ChannelServer) next)
							.getDescriptor() : (ChannelDescriptor) next);
					ChannelWorldInterface cwi;
					if (next instanceof ChannelDescriptor) {
						cwi = list.getChannel(descriptor.getId());
					} else {
						cwi = ChannelManager.getChannelServer(descriptor)
								.getWorldInterface()
								.getChannelInterface(descriptor.getId());
					}
					if (notifications.size() > 0
							&& notifications.get(descriptor.getId()).size() > 0) {
						if (bcop == BCOp.DISBAND) {
							cwi.setGuildAndRank(
									notifications.get(descriptor.getId()), 0,
									5, exceptionId);
						} else if (bcop == BCOp.EMBELMCHANGE) {
							cwi.changeEmblem(this.id,
									notifications.get(descriptor.getId()),
									new MapleGuildSummary(this));
						} else {
							cwi.sendPacket(
									notifications.get(descriptor.getId()),
									packet, exceptionId);
						}
					}
				}
			} catch (java.rmi.RemoteException e) {
				ServerExceptionHandler.HandlerRemoteException(e);
			}
		}
	}

	public void guildMessage(MaplePacket serverNotice) {
		for (MapleGuildCharacter mgc : members) {
			for (ChannelServer cs : ChannelManager.getChannelServers(world)) {
				if (cs.getPlayerStorage().getCharacterById(mgc.getId()) != null) {
					MapleCharacter chr = cs.getPlayerStorage()
							.getCharacterById(mgc.getId());
					chr.getClient().getSession().write(serverNotice);
					break;
				}
			}
		}
	}

	public void setOnline(int cid, boolean online, int channel) {
		boolean bBroadcast = true;
		for (MapleGuildCharacter mgc : members) {
			if (mgc.getId() == cid) {
				if (mgc.isOnline() && online) {
					bBroadcast = false;
				}
				mgc.setOnline(online);
				mgc.setChannel(channel);
				break;
			}
		}
		// log.info("执行到函数。");
		if (bBroadcast) {
			// log.info("广播参数：" + cid+"");
			this.broadcast(MapleGuild_Msg.在线家族成员(id, cid, online), cid);
		}
		bDirty = true; // member formation has changed, update notifications
	}

	public void guildChat(String name, int cid, String msg) {
		this.broadcast(MaplePacketCreator.multiChat(name, msg, 2), cid);
	}

	public String getRankTitle(int rank) {
		return rankTitles[rank - 1];
	}

	// function to create guild, returns the guild id if successful, 0 if not
	public static int createGuild(int leaderId, String name) {
		int ret = -1;
		boolean hasguild;
		Connection con;
		try {
			con = DatabaseConnection.getConnection();
			PreparedStatement ps = con
					.prepareStatement("SELECT guildid FROM guilds WHERE name = ?");
			ps.setString(1, name);
			ResultSet rs = ps.executeQuery();

			hasguild = rs.first();

			rs.close();
			ps.close();
			if (!hasguild) {
				ps = con.prepareStatement("INSERT INTO guilds (`leader`, `name`, `signature`) VALUES (?, ?, ?)");
				ps.setInt(1, leaderId);
				ps.setString(2, name);
				ps.setInt(3, (int) System.currentTimeMillis());
				ps.execute();
				ps.close();

				ps = con.prepareStatement("SELECT guildid FROM guilds WHERE leader = ?");
				ps.setInt(1, leaderId);
				rs = ps.executeQuery();
				rs.first();
				ret = rs.getInt("guildid");
				rs.close();
				ps.close();
			}
			con.close();
			return ret;
		} catch (SQLException se) {
			log.error("SQL THROW", se);
			return 0;
		} catch (Exception e) {
			log.error("CREATE GUILD THROW", e);
			return 0;
		}
	}

	public int addGuildMember(MapleGuildCharacter mgc) {
		// first of all, insert it into the members
		// keeping alphabetical order of lowest ranks ;)
		synchronized (members) {
			if (members.size() >= capacity) {
				return 0;
			}

			for (int i = members.size() - 1; i >= 0; i--) {
				// we will stop going forward when
				// 1. we're done with rank 5s, or
				// 2. the name comes alphabetically before the new member
				if (members.get(i).getGuildRank() < 5
						|| members.get(i).getName().compareTo(mgc.getName()) < 0) {
					// then we should add it at the i+1 location
					members.add(i + 1, mgc);
					bDirty = true;
					break;
				}
			}
		}

		this.broadcast(MapleGuild_Msg.addGuildMember(mgc));

		return 1;
	}

	public void leaveGuild(MapleGuildCharacter mgc) {
		this.broadcast(MapleGuild_Msg.memberLeft(mgc, false));

		synchronized (members) {
			members.remove(mgc);
			bDirty = true;
		}
	}

	public void expelMember(MapleGuildCharacter initiator, String name, int cid) {
		Logger log = Logger.getLogger(this.getClass());
		synchronized (members) {
			java.util.Iterator<MapleGuildCharacter> itr = members.iterator();
			MapleGuildCharacter mgc;
			while (itr.hasNext()) {
				mgc = itr.next();
				if (mgc.getId() == cid
						&& initiator.getGuildRank() < mgc.getGuildRank()) {
					this.broadcast(MapleGuild_Msg.memberLeft(mgc, true));
					itr.remove();
					bDirty = true;
					// i hate global for not saying who expelled
					// this.broadcast(MaplePacketCreator.serverNotice(5,
					// initiator.getName() + " has expelled " + mgc.getName() +
					// "."));
					try {
						if (mgc.isOnline()) {
							WorldRegistryImpl.getInstance()
									.getChannel(mgc.getChannelDescriptor())
									.setGuildAndRank(cid, 0, 5);
						} else {
							try {
								initiator.getName();
								MaplePacketCreator.sendUnkwnNote(mgc.getName(),
										"你已被踢出家族", initiator.getName());
							} catch (SQLException e) {
								log.error("SAVING NOTE", e);
							}
							WorldRegistryImpl
									.getInstance()
									.getChannel(new ChannelDescriptor(1, world))
									.setOfflineGuildStatus((short) 0, (byte) 5,
											cid);
						}
					} catch (RemoteException e) {
						ServerExceptionHandler.HandlerRemoteException(e);
						return;
					}
					return;
				}
			}

			log.error("Unable to find member with name " + name + " and id "
					+ cid);
		}
	}

	public void changeRank(int cid, int newRank, int fid) {
		for (MapleGuildCharacter mgc : members) {
			if (cid == mgc.getId()) {
				try {
					if (mgc.isOnline()) {
						WorldRegistryImpl.getInstance()
								.getChannel(mgc.getChannelDescriptor())
								.setGuildAndRank(cid, this.id, newRank);
					} else {
						WorldRegistryImpl
								.getInstance()
								.getChannel(new ChannelDescriptor(1, world))
								.setOfflineGuildStatus((short) this.id,
										(byte) newRank, cid);
					}
				} catch (RemoteException e) {
					ServerExceptionHandler.HandlerRemoteException(e);
					return;
				}

				mgc.setGuildRank(newRank);
				if (fid != -2) {
					if (fid == -1) {
						this.broadcast(MapleGuild_Msg.职位变更(mgc));
					} else {
						this.broadcast(MapleGuild_Msg
								.ChangeLeader(id, fid, cid));
					}
				}
				return;
			}
		}

		// it should never get to this point unless cid was incorrect o_O
		// System.out.println("INFO: unable to find the correct id for changeRank()");
		log.info("unable to find the correct id for changeRank(" + cid + ", "
				+ newRank + ")");
	}

	public void setGuildNotice(String notice) {
		this.notice = notice;
		writeToDB();

		this.broadcast(MapleGuild_Msg.家族公告(id, notice));
	}

	public void memberLevelJobUpdate(MapleGuildCharacter mgc) {
		for (MapleGuildCharacter member : members) {
			if (mgc.equals(member)) {
				member.setJobId(mgc.getJobId());
				member.setLevel(mgc.getLevel());
				this.broadcast(MapleGuild_Msg.家族成员等级职业数据更新(mgc));
				break;
			}
		}
	}

	public void changeRankTitle(String[] ranks) {
		for (int i = 0; i < 5; i++) {
			rankTitles[i] = ranks[i];
		}

		this.broadcast(MapleGuild_Msg.家族职位变更(this.id, ranks));
		this.writeToDB();
	}

	public void disbandGuild() {
		// disband the guild
		this.writeToDB(true);
		this.broadcast(null, -1, BCOp.DISBAND);
	}

	public void setGuildEmblem(short bg, byte bgcolor, short logo,
			byte logocolor) {
		this.logoBG = bg;
		this.logoBGColor = bgcolor;
		this.logo = logo;
		this.logoColor = logocolor;
		this.writeToDB();

		this.broadcast(null, -1, BCOp.EMBELMCHANGE);
	}

	public MapleGuildCharacter getMGC(int cid) {
		for (MapleGuildCharacter mgc : members) {
			if (mgc.getId() == cid) {
				return mgc;
			}
		}

		return null;
	}

	public boolean increaseCapacity() {
		if (capacity >= 100) {
			return false;
		}

		capacity += 5;
		this.writeToDB();

		this.broadcast(MapleGuild_Msg.家族成员数控制(id, capacity));

		return true;
	}

	// null indicates successful invitation being sent
	// keep in mind that this will be called by a handler most of the time
	// so this will be running mostly on a channel server, unlike the rest
	// of the class
	public static MapleGuildResponse sendInvite(MapleClient c, String targetName) {
		MapleCharacter mc = c.getChannelServer().getPlayerStorage()
				.getCharacterByName(targetName);
		if (mc == null) {
			// log.error("返回值:" + MapleGuildResponse.NOT_IN_CHANNEL);
			return MapleGuildResponse.NOT_IN_CHANNEL;
		}

		if (mc.getGuildid() > 0) {
			// log.error("返回值:" + MapleGuildResponse.ALREADY_IN_GUILD);
			return MapleGuildResponse.ALREADY_IN_GUILD;
		}

		mc.getClient()
				.getSession()
				.write(MapleGuild_Msg.家族邀请(c.getPlayer().getGuildid(), c
						.getPlayer().getName(), c.getPlayer().getLevel(), c
						.getPlayer().getJob().getId()));

		return null;
	}

	public static void displayGuildRanks(MapleClient c, int npcid) {
		try {
			Connection con = DatabaseConnection.getConnection();
			PreparedStatement ps = con
					.prepareStatement("SELECT * FROM guilds ORDER BY `GP` DESC LIMIT 50");
			ResultSet rs = ps.executeQuery();
			c.getSession().write(MapleGuild_Msg.showGuildRanks(npcid, rs));
			ps.close();
			rs.close();
			con.close();
		} catch (SQLException e) {
			log.error("failed to display guild ranks.", e);
		}
	}

	public void setAllianceId(int aid) {
		this.allianceId = aid;
		try {
			Connection con = DatabaseConnection.getConnection();
			PreparedStatement ps = con
					.prepareStatement("UPDATE guilds SET allianceId = ? WHERE guildid = ?");
			ps.setInt(1, aid);
			ps.setInt(2, id);
			ps.executeUpdate();
			ps.close();
			con.close();
		} catch (SQLException e) {
		}
	}

	public final void addMemberData(final MaplePacketLittleEndianWriter mplew) {
		mplew.write(members.size());
		for (final MapleGuildCharacter mgc : members) {
			mplew.writeInt(mgc.getId());
		}
		for (final MapleGuildCharacter mgc : members) {
			// mplew.writeAsciiString(StringUtil.getRightPaddedStr(mgc.getName(),
			// '\0', 13));
			mplew.WriteOfMaxByteCountString(mgc.getName(), 13);
			mplew.writeInt(mgc.getJobId());
			mplew.writeInt(mgc.getLevel());
			mplew.writeInt(mgc.getGuildRank());
			mplew.writeInt(mgc.isOnline() ? 1 : 0);
			// mplew.writeInt(mgc.isOnline() ? 1 : 0);//03 00 00 00
			mplew.writeInt(3);// 97
			mplew.writeInt(gp);
		}
	}

	public int getAllianceId() {
		int getvip1 = 0;
		try {
			Connection con = DatabaseConnection.getConnection();

			PreparedStatement ps = con
					.prepareStatement("SELECT allianceid from guilds where guildid = ?");
			ps.setInt(1, getId());
			ResultSet rs = ps.executeQuery();
			if (rs.next()) {
				getvip1 = rs.getInt("allianceid");
			}

			ps.close();
			rs.close();
			con.close();
		} catch (SQLException e) {
			e.printStackTrace();
		}
		this.allianceId = getvip1;
		return this.allianceId;
	}
}
