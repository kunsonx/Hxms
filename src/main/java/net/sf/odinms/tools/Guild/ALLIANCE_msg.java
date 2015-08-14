/*
 * 联盟封包
 */
package net.sf.odinms.tools.Guild;

import java.rmi.RemoteException;
import java.util.Collection;
import net.sf.odinms.client.MapleCharacter;
import net.sf.odinms.client.MapleClient;
import net.sf.odinms.net.MaplePacket;
import net.sf.odinms.net.SendPacketOpcode;
import net.sf.odinms.net.world.guild.MapleAlliance;
import net.sf.odinms.net.world.guild.MapleGuild;
import net.sf.odinms.net.world.guild.MapleGuildCharacter;
import net.sf.odinms.server.ServerExceptionHandler;
import net.sf.odinms.tools.HexTool;
import net.sf.odinms.tools.StringUtil;
import net.sf.odinms.tools.data.output.MaplePacketLittleEndianWriter;

/**
 *
 * @author 千石抚子
 */
public class ALLIANCE_msg {

	private static org.apache.log4j.Logger log = org.apache.log4j.Logger
			.getLogger(ALLIANCE_msg.class);
	private static boolean show = false;

	// 家族联盟

	public static MaplePacket makeNewAlliance(MapleAlliance alliance,
			MapleClient c) {
		MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

		if (show) {
			log.debug("调用的函数：" + new Throwable().getStackTrace()[0]); // 显示调用的类
																		// 函数名
																		// 函数所在行
		}
		mplew.writeShort(SendPacketOpcode.ALLIANCE_OPERATION.getValue());
		mplew.write(0x0F);
		mplew.writeInt(alliance.getId());
		mplew.writeMapleAsciiString(alliance.getName());
		for (int i = 1; i <= 5; i++) {
			mplew.writeMapleAsciiString(alliance.getRankTitle(i));
		}
		mplew.write(alliance.getGuilds().size());
		for (Integer guild : alliance.getGuilds()) {
			mplew.writeInt(guild);
		}
		mplew.writeInt(2); // probably capacity
		mplew.writeShort(0);
		for (Integer guildd : alliance.getGuilds()) {
			try {
				getGuildInfo(mplew, c.getChannelServer().getWorldInterface()
						.getGuild(guildd, null));
			} catch (RemoteException e) {
				ServerExceptionHandler.HandlerRemoteException(e);
				c.getChannelServer().reconnectWorld();
			}
		}
		return mplew.getPacket();
	}

	private static void getGuildInfo(MaplePacketLittleEndianWriter mplew,
			MapleGuild guild) {
		if (show) {
			log.debug("调用的函数：" + new Throwable().getStackTrace()[0]); // 显示调用的类
																		// 函数名
																		// 函数所在行
		}
		mplew.writeInt(guild.getId());
		mplew.writeMapleAsciiString(guild.getName());
		for (int i = 1; i <= 5; i++) {
			mplew.writeMapleAsciiString(guild.getRankTitle(i));
		}
		Collection<MapleGuildCharacter> members = guild.getMembers();
		mplew.write(members.size());
		// then it is the size of all the members
		for (MapleGuildCharacter mgc : members) // and each of their character
												// ids o_O
		{
			mplew.writeInt(mgc.getId());
		}
		for (MapleGuildCharacter mgc : members) {
			// mplew.writeAsciiString(StringUtil.getRightPaddedStr(mgc.getName(),
			// '\0', 13));
			mplew.WriteOfMaxByteCountString(mgc.getName(), 13);
			mplew.writeInt(mgc.getJobId());
			mplew.writeInt(mgc.getLevel());
			mplew.writeInt(mgc.getGuildRank());
			mplew.writeInt(mgc.isOnline() ? 1 : 0);
			mplew.writeInt(mgc.getAllianceRank());
			mplew.writeInt(guild.getGP());
		}
		mplew.writeInt(guild.getCapacity());
		mplew.writeShort(guild.getLogoBG());
		mplew.write(guild.getLogoBGColor());
		mplew.writeShort(guild.getLogo());
		mplew.write(guild.getLogoColor());
		mplew.writeMapleAsciiString(guild.getNotice());
		mplew.writeInt(guild.getGP());
		mplew.writeInt(guild.getGP());
		mplew.writeInt(guild.getAllianceId());
		mplew.write(1);
		mplew.writeInt(0);
	}

	public static MaplePacket getGuildAlliances(MapleAlliance alliance,
			MapleClient c) {
		MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

		if (show) {
			log.debug("调用的函数：" + new Throwable().getStackTrace()[0]); // 显示调用的类
																		// 函数名
																		// 函数所在行
		}
		mplew.writeShort(SendPacketOpcode.ALLIANCE_OPERATION.getValue());
		mplew.write(0x0D);
		mplew.writeInt(alliance.getGuilds().size());
		for (Integer guild : alliance.getGuilds()) {
			try {
				getGuildInfo(mplew, c.getChannelServer().getWorldInterface()
						.getGuild(guild, null));
			} catch (RemoteException e) {
				ServerExceptionHandler.HandlerRemoteException(e);
				c.getChannelServer().reconnectWorld();
			}
		}

		return mplew.getPacket();
	}

	// 家族联盟
	public static MaplePacket getAllianceInfo(MapleAlliance alliance) {
		if (show) {
			log.debug("调用的函数：" + new Throwable().getStackTrace()[0]); // 显示调用的类
																		// 函数名
																		// 函数所在行
		}
		MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

		mplew.writeShort(SendPacketOpcode.ALLIANCE_OPERATION.getValue());
		mplew.write(0x0C);
		mplew.write(1);
		mplew.writeInt(alliance.getId());
		mplew.writeMapleAsciiString(alliance.getName());
		for (int i = 1; i <= 5; i++) {
			mplew.writeMapleAsciiString(alliance.getRankTitle(i));
		}
		mplew.write(alliance.getGuilds().size());
		mplew.writeInt(2); // probably capacity
		for (Integer guild : alliance.getGuilds()) {
			mplew.writeInt(guild);
		}
		mplew.writeMapleAsciiString(alliance.getNotice());
		return mplew.getPacket();
	}

	public static MaplePacket 联盟公告(int id, String notice) {
		MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
		if (show) {
			log.debug("调用的函数：" + new Throwable().getStackTrace()[0]); // 显示调用的类
																		// 函数名
																		// 函数所在行
		}
		mplew.writeShort(SendPacketOpcode.ALLIANCE_OPERATION.getValue());
		mplew.write(0x1C);
		mplew.writeInt(id);
		mplew.writeMapleAsciiString(notice);
		return mplew.getPacket();
	}

	public static MaplePacket 联盟主(int alliance, int cid, int did) {
		if (show) {
			log.debug("调用的函数：" + new Throwable().getStackTrace()[0]); // 显示调用的类
																		// 函数名
																		// 函数所在行
		}
		MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
		mplew.writeShort(SendPacketOpcode.ALLIANCE_OPERATION.getValue());
		mplew.write(0x19);
		mplew.writeInt(alliance);
		mplew.writeInt(cid);// 现在的联盟主ID
		mplew.writeInt(did);// 新任的联盟主ID
		return mplew.getPacket();
	}

	public static MaplePacket 联盟地位改变(int alliance, String[] ranks) {
		if (show) {
			log.debug("调用的函数：" + new Throwable().getStackTrace()[0]); // 显示调用的类
																		// 函数名
																		// 函数所在行
		}
		MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
		mplew.writeShort(SendPacketOpcode.ALLIANCE_OPERATION.getValue());
		mplew.write(0x1A);
		mplew.writeInt(alliance);
		for (int i = 0; i < 5; i++) {
			mplew.writeMapleAsciiString(ranks[i]);
		}
		return mplew.getPacket();
	}

	public static MaplePacket 家族成员等级职业数据更新(MapleCharacter mc) {
		MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

		if (show) {
			log.debug("调用的函数：" + new Throwable().getStackTrace()[0]); // 显示调用的类
																		// 函数名
																		// 函数所在行
		}
		mplew.writeShort(SendPacketOpcode.ALLIANCE_OPERATION.getValue());
		mplew.write(0x18);
		mplew.writeInt(mc.getGuild().getAllianceId());
		mplew.writeInt(mc.getGuildid());
		mplew.writeInt(mc.getId());
		mplew.writeInt(mc.getLevel());
		mplew.writeInt(mc.getJob().getId());

		return mplew.getPacket();
	}

	public static MaplePacket removeGuildFromAlliance(MapleAlliance alliance,
			int expelledGuild, MapleClient c, boolean 是否为被踢出) {
		MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
		// PLEASE NOTE THAT WE MUST REMOVE THE GUILD BEFORE SENDING THIS PACKET.
		// <3
		// ALSO ANOTHER NOTE, WE MUST REMOVE ALLIANCEID FROM GUILD BEFORE
		// SENDING ASWELL <3
		if (show) {
			log.debug("调用的函数：" + new Throwable().getStackTrace()[0]); // 显示调用的类
																		// 函数名
																		// 函数所在行
		}
		mplew.writeShort(SendPacketOpcode.ALLIANCE_OPERATION.getValue());
		mplew.write(0x10);
		mplew.writeInt(alliance.getId());
		mplew.writeMapleAsciiString(alliance.getName());
		for (int i = 1; i <= 5; i++) {
			mplew.writeMapleAsciiString(alliance.getRankTitle(i));
		}
		mplew.write(alliance.getGuilds().size());
		for (Integer guild : alliance.getGuilds()) {
			mplew.writeInt(guild);
		}
		mplew.writeInt(2); // probably capacity
		mplew.writeMapleAsciiString(alliance.getNotice());
		try {
			mplew.writeInt(expelledGuild);
			getGuildInfo(mplew, c.getChannelServer().getWorldInterface()
					.getGuild(expelledGuild, null));
		} catch (RemoteException e) {
			ServerExceptionHandler.HandlerRemoteException(e);
			c.getChannelServer().reconnectWorld();
		}
		if (是否为被踢出) {
			mplew.write(0x01);
		} else {
			mplew.write(0x00);
		}
		return mplew.getPacket();
	}

	public static MaplePacket 解散联盟(int alliance) {
		if (show) {
			log.debug("调用的函数：" + new Throwable().getStackTrace()[0]); // 显示调用的类
																		// 函数名
																		// 函数所在行
		}
		MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

		mplew.writeShort(SendPacketOpcode.ALLIANCE_OPERATION.getValue());
		mplew.write(0x1D);
		mplew.writeInt(alliance);

		return mplew.getPacket();
	}

	public static MaplePacket 在线联盟成员(MapleCharacter mc, boolean online) {
		MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

		if (show) {
			log.debug("调用的函数：" + new Throwable().getStackTrace()[0]); // 显示调用的类
																		// 函数名
																		// 函数所在行
		}
		mplew.writeShort(SendPacketOpcode.ALLIANCE_OPERATION.getValue());
		mplew.write(0x0E);
		mplew.writeInt(mc.getGuild().getAllianceId());
		mplew.writeInt(mc.getGuildid());
		mplew.writeInt(mc.getId());
		mplew.write(online ? 1 : 0);

		return mplew.getPacket();
	}

	public static MaplePacket addGuildToAlliance(MapleAlliance alliance,
			int newGuild, MapleClient c) {
		MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

		if (show) {
			log.debug("调用的函数：" + new Throwable().getStackTrace()[0]); // 显示调用的类
																		// 函数名
																		// 函数所在行
		}
		mplew.writeShort(SendPacketOpcode.ALLIANCE_OPERATION.getValue());
		mplew.write(0x12);
		mplew.writeInt(alliance.getId());
		mplew.writeMapleAsciiString(alliance.getName());
		for (int i = 1; i <= 5; i++) {
			mplew.writeMapleAsciiString(alliance.getRankTitle(i));
		}
		mplew.write(alliance.getGuilds().size());
		for (Integer guild : alliance.getGuilds()) {
			mplew.writeInt(guild);
		}
		mplew.writeInt(alliance.getGuilds().size());
		mplew.writeMapleAsciiString(alliance.getNotice());
		mplew.writeInt(newGuild);
		try {
			getGuildInfo(mplew, c.getChannelServer().getWorldInterface()
					.getGuild(newGuild, null));
		} catch (RemoteException e) {
			ServerExceptionHandler.HandlerRemoteException(e);
			c.getChannelServer().reconnectWorld();
		}
		return mplew.getPacket();
	}

	public static MaplePacket 发送邀请(MapleAlliance alliance, int cid,
			MapleClient c, String name) {
		MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
		mplew.writeShort(SendPacketOpcode.ALLIANCE_OPERATION.getValue());
		mplew.write(0x03);
		mplew.writeInt(cid);
		mplew.writeMapleAsciiString(name);
		mplew.writeMapleAsciiString(alliance.getName());
		return mplew.getPacket();
	}
}
