/*
 联盟的操作处理程序
 */
package net.sf.odinms.net.channel.handler;

import java.rmi.RemoteException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Iterator;
import net.sf.odinms.client.MapleCharacter;
import net.sf.odinms.client.MapleClient;
import net.sf.odinms.client.MaplePet;
import net.sf.odinms.database.DatabaseConnection;
import net.sf.odinms.net.AbstractMaplePacketHandler;
import net.sf.odinms.net.world.guild.MapleAlliance;
import net.sf.odinms.net.world.guild.MapleGuild;
import net.sf.odinms.server.ServerExceptionHandler;
import net.sf.odinms.tools.Guild.ALLIANCE_msg;
import net.sf.odinms.tools.MaplePacketCreator;
import net.sf.odinms.tools.data.input.SeekableLittleEndianAccessor;

/**
 *
 * @author XoticStory
 */
public class AllianceOperationHandler extends AbstractMaplePacketHandler {

	private boolean isGuildNameAcceptable(String name) {
		if (name.getBytes().length < 3 || name.getBytes().length > 12) {
			return false;
		}
		return true;
	}

	private void respawnPlayer(MapleCharacter mc) {
		mc.getMap().broadcastMessage(mc,
				MaplePacketCreator.removePlayerFromMap(mc.getId()), false);
		mc.getMap().broadcastMessage(mc,
				MaplePacketCreator.spawnPlayerMapobject(mc), false);
		if (mc.getNoPets() > 0) {
			for (MaplePet pet : mc.getPets()) {
				if (pet != null) {
					mc.getMap().broadcastMessage(mc,
							MaplePacketCreator.showPet(mc, pet, false, false),
							false);
				}
			}
		}
	}

	private class Invited {

		public String name;
		public int gid;
		public long expiration;

		public Invited(String n, int id) {
			name = n.toLowerCase();
			gid = id;
			expiration = System.currentTimeMillis() + 60 * 60 * 1000; // 1 hr
																		// expiration
		}

		@Override
		public boolean equals(Object other) {
			if (!(other instanceof Invited)) {
				return false;
			}
			Invited oth = (Invited) other;
			return (gid == oth.gid && name.equals(oth));
		}
	}

	private org.apache.log4j.Logger log = org.apache.log4j.Logger
			.getLogger(this.getClass());
	private java.util.List<Invited> invited = new java.util.LinkedList<Invited>();
	private long nextPruneTime = System.currentTimeMillis() + 20 * 60 * 1000;

	@Override
	public void handlePacket(SeekableLittleEndianAccessor slea, MapleClient c) {
		if (System.currentTimeMillis() >= nextPruneTime) {
			Iterator<Invited> itr = invited.iterator();
			Invited inv;
			while (itr.hasNext()) {
				inv = itr.next();
				if (System.currentTimeMillis() >= inv.expiration) {
					itr.remove();
				}
			}
			nextPruneTime = System.currentTimeMillis() + 20 * 60 * 1000;
		}
		MapleAlliance alliance = null;
		if (c.getPlayer().getGuild() != null
				&& c.getPlayer().getGuild().getAllianceId() > 0) {
			try {
				alliance = c.getChannelServer().getWorldInterface()
						.getAlliance(c.getPlayer().getGuild().getAllianceId());
			} catch (RemoteException rawr) {
				ServerExceptionHandler.HandlerRemoteException(rawr);
				c.getChannelServer().reconnectWorld();
			}
		}
		// log.debug(slea.toString());

		try {
			int victim_guildId;
			MapleCharacter victim_player = null;
			byte fengb = slea.readByte();
			switch (fengb) {
			case 0x01:// 联盟上线
				c.getChannelServer()
						.getWorldInterface()
						.allianceMessage(alliance.getId(),
								ALLIANCE_msg.在线联盟成员(c.getPlayer(), true),
								c.getPlayer().getId(), -1);
				break;
			case 0x02:// 自己退出联盟
				c.getPlayer().getGuild().setAllianceId(0);
				alliance.addRemGuildFromDB(c.getPlayer().getGuild().getId(),
						false);
				c.getSession().write(
						ALLIANCE_msg.removeGuildFromAlliance(alliance, c
								.getPlayer().getGuild().getId(), c, false));// 自己的提示
				c.getChannelServer()
						.getWorldInterface()
						.allianceMessage(
								alliance.getId(),
								ALLIANCE_msg.removeGuildFromAlliance(alliance,
										c.getPlayer().getGuild().getId(), c,
										false), -1, -1);
				respawnPlayer(c.getPlayer());
				break;
			case 0x03:// 发送邀请联盟
				String allianceName = slea.readMapleAsciiString();
				victim_guildId = alliance.getGuildIdByGuildName(allianceName);
				if (victim_guildId != -1) {
					MapleGuild guild = c.getChannelServer().getWorldInterface()
							.getGuild(victim_guildId, null);
					victim_player = c.getChannelServer().getPlayerStorage()
							.getCharacterById(guild.getLeaderId());
					if (guild != null && victim_player != null) {
						victim_player
								.getClient()
								.getSession()
								.write(ALLIANCE_msg.发送邀请(alliance, c
										.getPlayer().getId(), c, c.getPlayer()
										.getName()));
					}
				} else {
					log.debug("查询不到此家族");
				}
				break;
			case 0x04:// 玩家接受邀请
				int cid = slea.readInt();// 使用者的id
				String name = slea.readMapleAsciiString();// 被邀请的家族名

				// 设置联盟id
				int ciid = 获取家族的联盟(cid);
				c.getPlayer().getGuild().setAllianceId(ciid);
				if (alliance == null) {
					try {
						alliance = c
								.getChannelServer()
								.getWorldInterface()
								.getAlliance(
										c.getPlayer().getGuild()
												.getAllianceId());
					} catch (RemoteException rawr) {
						ServerExceptionHandler.HandlerRemoteException(rawr);
						c.getChannelServer().reconnectWorld();
					}
				}
				alliance.addGuild(c.getPlayer().getGuild().getId());
				c.getPlayer().setAllianceRank(2);
				// 提示
				c.getSession().write(
						ALLIANCE_msg.addGuildToAlliance(alliance, c.getPlayer()
								.getGuild().getId(), c));// 自己的提示
				c.getChannelServer()
						.getWorldInterface()
						.allianceMessage(
								alliance.getId(),
								ALLIANCE_msg.addGuildToAlliance(alliance, c
										.getPlayer().getGuild().getId(), c),
								-1, -1);// 联盟的提示
				// 联盟信息
				c.getSession().write(ALLIANCE_msg.getAllianceInfo(alliance));// 自己的提示
				c.getChannelServer()
						.getWorldInterface()
						.allianceMessage(alliance.getId(),
								ALLIANCE_msg.getAllianceInfo(alliance), -1, -1);// 联盟的提示
				// 综合信息
				c.getSession().write(
						ALLIANCE_msg.getGuildAlliances(alliance, c));// 自己的提示
				c.getChannelServer()
						.getWorldInterface()
						.allianceMessage(alliance.getId(),
								ALLIANCE_msg.getGuildAlliances(alliance, c),
								-1, -1);// 联盟的提示
				break;
			case 0x0A:// 联盟公告
				String notice = slea.readMapleAsciiString();
				alliance.setNotice(notice);
				// c.getChannelServer().getWorldInterface().setAllianceNotice(alliance.getId(),
				// notice);
				c.getSession().write(
						ALLIANCE_msg.联盟公告(alliance.getId(), notice));
				c.getChannelServer()
						.getWorldInterface()
						.allianceMessage(alliance.getId(),
								ALLIANCE_msg.联盟公告(alliance.getId(), notice),
								-1, -1);
				break;
			case 0x06:// T出联盟 被踢出者的家族ID 被踢出的联盟ID
				cid = slea.readInt();
				int allianceid = slea.readInt();
				MapleGuild guild = c.getChannelServer().getWorldInterface()
						.getGuild(cid, null);// 从家族中找到玩家Id
				victim_player = c.getChannelServer().getPlayerStorage()
						.getCharacterById(guild.getLeaderId());// 获取玩家的实例
				victim_player.getGuild().setAllianceId(0);
				alliance.removeGuild(cid);
				victim_player
						.getClient()
						.getSession()
						.write(ALLIANCE_msg.removeGuildFromAlliance(alliance,
								cid, c, true));
				c.getChannelServer()
						.getWorldInterface()
						.allianceMessage(
								alliance.getId(),
								ALLIANCE_msg.removeGuildFromAlliance(alliance,
										c.getPlayer().getGuild().getId(), c,
										true), -1, -1);
				// respawnPlayer(c.getPlayer());
				break;
			case 0x07:// 设置联盟主
				cid = slea.readInt();
				// 设置权限
				victim_player = c.getChannelServer().getPlayerStorage()
						.getCharacterById(cid);
				if (victim_player == null) {
					return;
				}
				victim_player.setAllianceRank(1);// 新任的联盟主权限
				c.getPlayer().setAllianceRank(2);// 以前的联盟主权限
				c.getSession().write(
						ALLIANCE_msg.联盟主(alliance.getId(), c.getPlayer()
								.getId(), cid));
				c.getChannelServer()
						.getWorldInterface()
						.allianceMessage(
								alliance.getId(),
								ALLIANCE_msg.联盟主(alliance.getId(), c
										.getPlayer().getId(), cid), -1, -1);
				break;
			case 0x08:// 联盟职位改变
				String ranks[] = new String[5];
				for (int i = 0; i < 5; i++) {
					ranks[i] = slea.readMapleAsciiString();
				}
				alliance.setRankTitle(ranks);
				c.getSession().write(
						ALLIANCE_msg.联盟地位改变(alliance.getId(), ranks));
				c.getChannelServer()
						.getWorldInterface()
						.allianceMessage(alliance.getId(),
								ALLIANCE_msg.联盟地位改变(alliance.getId(), ranks),
								-1, -1);
				break;
			case 0x09:// 下降玩家权限
				c.getPlayer().dropMessage(1, "此功能官方不存在,所以无法去修复..");
				c.getSession().write(MaplePacketCreator.enableActions());
				break;
			default:
				c.getPlayer().dropMessage("找不到功能,请联系管理员进行修复..");
			}
			// log.debug("操作人:"+c.getPlayer().getName());
		} catch (RemoteException rawr) {
			ServerExceptionHandler.HandlerRemoteException(rawr);
			c.getChannelServer().reconnectWorld();
		}
	}

	/**
	 * * 获取家族的联盟
	 *
	 * @param 角色的id
	 * @return
	 */
	public int 获取家族的联盟(int ID) {
		int cid = 0;

		PreparedStatement ps;
		try {
			Connection con = DatabaseConnection.getConnection();
			ps = con.prepareStatement("SELECT allianceid FROM guilds WHERE leader = ?");
			ps.setInt(1, ID);
			ResultSet rs = ps.executeQuery();
			if (rs.next()) {
				cid = rs.getInt("allianceid");
			} else {
				log.debug("查询不到此家族");
			}
			rs.close();
			ps.close();
			con.close();
		} catch (SQLException ex) {
			log.debug("通过家族Id查询家族出错" + ex);
		}
		return cid;
	}
}