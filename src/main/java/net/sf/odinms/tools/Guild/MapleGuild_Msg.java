/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package net.sf.odinms.tools.Guild;

import java.sql.ResultSet;
import java.sql.SQLException;
import net.sf.odinms.client.MapleCharacter;
import net.sf.odinms.net.MaplePacket;
import net.sf.odinms.net.SendPacketOpcode;
import net.sf.odinms.net.world.guild.MapleGuild;
import net.sf.odinms.net.world.guild.MapleGuildCharacter;
import net.sf.odinms.tools.StringUtil;
import net.sf.odinms.tools.data.output.MaplePacketLittleEndianWriter;

/**
 *
 *
 */
public class MapleGuild_Msg {

	public static MaplePacket showGuildInfo(MapleCharacter c) {
		MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
		mplew.writeShort(SendPacketOpcode.GUILD_OPERATION.getValue());
		mplew.write(0x20); // 家族信息显示名称 //092

		if (c == null) { // 显示空家族信息
			mplew.write(0);
			return mplew.getPacket();
		}
		MapleGuildCharacter initiator = c.getMGC();
		MapleGuild g = c.getClient().getChannelServer().getGuild(initiator);
		if (g == null) { // 无法读取数据从数据库
			mplew.write(0);
			return mplew.getPacket();
		} else {
			// 家族启动排名数值信息值
			MapleGuildCharacter mgc = g.getMGC(c.getId());
			c.setGuildRank(mgc.getGuildRank());
		}
		mplew.write(0x1);
		mplew.writeInt(c.getGuildid()); // 家族ID
		mplew.writeMapleAsciiString(g.getName());
		for (int i = 1; i <= 5; i++) {
			mplew.writeMapleAsciiString(g.getRankTitle(i));
		}
		// mplew.writeInt(0);//97
		g.addMemberData(mplew);

		mplew.writeInt(g.getCapacity());
		mplew.writeShort(g.getLogoBG());
		mplew.write(g.getLogoBGColor());
		mplew.writeShort(g.getLogo());
		mplew.write(g.getLogoColor());
		mplew.writeMapleAsciiString(g.getNotice());
		mplew.writeInt(g.getGP());
		mplew.writeInt(g.getGP());
		mplew.writeInt(g.getAllianceId());
		mplew.write(1);
		mplew.writeInt(0);

		return mplew.getPacket();
	}

	public static MaplePacket showGuildName(MapleCharacter c) {
		MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

		mplew.writeShort(SendPacketOpcode.GUILD_OPERATION.getValue());
		MapleGuildCharacter initiator = c.getMGC();
		MapleGuild g = c.getClient().getChannelServer().getGuild(initiator);
		mplew.write(3);
		mplew.writeMapleAsciiString(g.getName());

		return mplew.getPacket();
	}

	public static MaplePacket 在线家族成员(int gid, int cid, boolean bOnline) {
		MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

		mplew.writeShort(SendPacketOpcode.GUILD_OPERATION.getValue());
		mplew.write(0x43);
		mplew.writeInt(gid);
		mplew.writeInt(cid);
		mplew.write(bOnline ? 1 : 0);
		return mplew.getPacket();
	}

	public static MaplePacket ChangeLeader(int gid, int fid, int toid) {
		MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

		mplew.writeShort(SendPacketOpcode.GUILD_OPERATION.getValue());
		mplew.write(0x59);
		mplew.writeInt(gid);
		mplew.writeInt(fid);
		mplew.writeInt(toid);
		mplew.write(0);
		return mplew.getPacket();
	}

	public static MaplePacket 家族邀请(int gid, String charName, int Level, int Job) {
		MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

		mplew.writeShort(SendPacketOpcode.GUILD_OPERATION.getValue());
		mplew.write(5);
		mplew.writeInt(gid);
		mplew.writeMapleAsciiString(charName);
		mplew.writeInt(Level);
		mplew.writeInt(Job);
		mplew.writeInt(0);
		return mplew.getPacket();
	}

	/**
	 * 'Char' has denied your guild invitation.
	 *
	 * @param charname
	 * @return
	 */
	public static MaplePacket 拒绝家族邀请(String charname) {
		MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

		mplew.writeShort(SendPacketOpcode.GUILD_OPERATION.getValue());
		mplew.write(0x3D);
		mplew.writeMapleAsciiString(charname);

		return mplew.getPacket();
	}

	public static MaplePacket 家族解散(int gid) {
		MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

		mplew.writeShort(SendPacketOpcode.GUILD_OPERATION.getValue());
		mplew.write(0x38);
		mplew.writeInt(gid);

		return mplew.getPacket();
	}

	public static MaplePacket genericGuildMessage(byte code) {
		MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

		mplew.writeShort(SendPacketOpcode.GUILD_OPERATION.getValue());
		mplew.write(code);

		return mplew.getPacket();
	}

	public static MaplePacket addGuildMember(MapleGuildCharacter mgc) {
		MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

		mplew.writeShort(SendPacketOpcode.GUILD_OPERATION.getValue());
		mplew.write(0x2D);
		mplew.writeInt(mgc.getGuildId());
		mplew.writeInt(mgc.getId());
		// mplew.writeAsciiString(StringUtil.getRightPaddedStr(mgc.getName(),
		// '\0', 13));
		mplew.WriteOfMaxByteCountString(mgc.getName(), 13);
		mplew.writeInt(mgc.getJobId());
		mplew.writeInt(mgc.getLevel());
		mplew.writeInt(mgc.getGuildRank()); // should be always 5 but whatevs
		mplew.writeInt(mgc.isOnline() ? 1 : 0); // should always be 1 too
		mplew.writeInt(3); // ? could be guild signature, but doesn't seem to
							// matter
		mplew.writeInt(0);

		return mplew.getPacket();
	}

	// 有人离开，离开模式==0x32，为驱逐值为0x35
	public static MaplePacket memberLeft(MapleGuildCharacter mgc,
			boolean bExpelled) {
		MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

		mplew.writeShort(SendPacketOpcode.GUILD_OPERATION.getValue());
		mplew.write(bExpelled ? 0x35 : 0x32);

		mplew.writeInt(mgc.getGuildId());
		mplew.writeInt(mgc.getId());
		mplew.writeMapleAsciiString(mgc.getName());

		return mplew.getPacket();
	}

	public static MaplePacket 职位变更(MapleGuildCharacter mgc) {
		MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

		mplew.writeShort(SendPacketOpcode.GUILD_OPERATION.getValue());
		mplew.write(0x46);
		mplew.writeInt(mgc.getGuildId());
		mplew.writeInt(mgc.getId());
		mplew.write(mgc.getGuildRank());

		return mplew.getPacket();
	}

	public static MaplePacket 家族公告(int gid, String notice) {
		MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

		mplew.writeShort(SendPacketOpcode.GUILD_OPERATION.getValue());
		mplew.write(0x4B);
		mplew.writeInt(gid);
		mplew.writeMapleAsciiString(notice);

		return mplew.getPacket();
	}

	public static MaplePacket 家族成员等级职业数据更新(MapleGuildCharacter mgc) {
		MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

		mplew.writeShort(SendPacketOpcode.GUILD_OPERATION.getValue());
		mplew.write(0x42);// V.092

		mplew.writeInt(mgc.getGuildId());
		mplew.writeInt(mgc.getId());
		mplew.writeInt(mgc.getLevel());
		mplew.writeInt(mgc.getJobId());

		return mplew.getPacket();
	}

	public static MaplePacket 家族职位变更(int gid, String[] ranks) {
		MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

		mplew.writeShort(SendPacketOpcode.GUILD_OPERATION.getValue());
		mplew.write(0x44);// 97
		mplew.writeInt(gid);
		for (int i = 0; i < 5; i++) {
			mplew.writeMapleAsciiString(ranks[i]);
		}

		return mplew.getPacket();
	}

	public static MaplePacket 家族徽章变更(int gid, short bg, byte bgcolor,
			short logo, byte logocolor) {
		MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

		mplew.writeShort(SendPacketOpcode.GUILD_OPERATION.getValue());
		mplew.write(0x49);
		mplew.writeInt(gid);
		mplew.writeShort(bg);
		mplew.write(bgcolor);
		mplew.writeShort(logo);
		mplew.write(logocolor);

		return mplew.getPacket();
	}

	public static MaplePacket 家族成员数控制(int gid, int capacity) {
		MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

		mplew.writeShort(SendPacketOpcode.GUILD_OPERATION.getValue());
		mplew.write(0x40);
		mplew.writeInt(gid);
		mplew.write(capacity);

		return mplew.getPacket();
	}

	public static MaplePacket guildDisband(int gid) {
		MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

		mplew.writeShort(SendPacketOpcode.GUILD_OPERATION.getValue());
		mplew.write(0x38);
		mplew.writeInt(gid);

		return mplew.getPacket();
	}

	public static MaplePacket updateGP(int gid, int GP, int level) {
		MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

		mplew.writeShort(SendPacketOpcode.GUILD_OPERATION.getValue());
		mplew.write(0x4F);
		mplew.writeInt(gid);// 家族ID
		mplew.writeInt(GP);// 更新GP积分
		mplew.writeInt(level);// 家族等级
		return mplew.getPacket();
	}

	public static MaplePacket showGuildRanks(int npcid, ResultSet rs)
			throws SQLException {
		MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

		mplew.writeShort(SendPacketOpcode.GUILD_OPERATION.getValue());
		mplew.write(0x50);
		mplew.writeInt(npcid);
		// this error 38s and official servers have it removed
		// if (!rs.last()) { //no guilds o.o
		if (true) {
			mplew.writeInt(0);
			return mplew.getPacket();
		}

		mplew.writeInt(rs.getRow()); // number of entries
		rs.beforeFirst();
		while (rs.next()) {
			mplew.writeMapleAsciiString(rs.getString("name"));
			// mplew.writeInt(rs.getInt("GP"));
			mplew.writeInt(rs.getInt("GP"));
			mplew.writeInt(rs.getInt("logo"));
			mplew.writeInt(rs.getInt("logoColor"));
			mplew.writeInt(rs.getInt("logoBG"));
			mplew.writeInt(rs.getInt("logoBGColor"));
		}

		return mplew.getPacket();
	}

	public static MaplePacket 家族确认(String text) {
		MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
		mplew.writeShort(SendPacketOpcode.GUILD_OPERATION.getValue());
		mplew.write(0x03);
		mplew.writeMapleAsciiString(text);
		return mplew.getPacket();
	}

	public static MaplePacket 更新家族GP和信息(MapleCharacter c) {
		MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
		mplew.writeShort(SendPacketOpcode.GUILD_OPERATION.getValue());
		mplew.write(0x48);
		mplew.writeInt(c.getGuildid());
		mplew.writeInt(c.getId());
		mplew.writeInt(c.getGuild().getGP());
		return mplew.getPacket();
	}

	public static MaplePacket CreateGuild(MapleCharacter c) {
		MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
		mplew.writeShort(SendPacketOpcode.GUILD_OPERATION.getValue());
		mplew.write(0x26); // 家族信息显示名称 //092

		if (c == null) { // 显示空家族信息
			mplew.write(0);
			return mplew.getPacket();
		}
		MapleGuildCharacter initiator = c.getMGC();
		MapleGuild g = c.getClient().getChannelServer().getGuild(initiator);
		if (g == null) { // 无法读取数据从数据库
			mplew.write(0);
			return mplew.getPacket();
		} else {
			// 家族启动排名数值信息值
			MapleGuildCharacter mgc = g.getMGC(c.getId());
			c.setGuildRank(mgc.getGuildRank());
		}
		mplew.writeInt(c.getGuildid()); // 家族ID
		mplew.writeMapleAsciiString(g.getName());
		for (int i = 1; i <= 5; i++) {
			mplew.writeMapleAsciiString(g.getRankTitle(i));
		}
		// mplew.writeInt(0);
		g.addMemberData(mplew);

		mplew.writeInt(g.getCapacity());
		mplew.writeShort(g.getLogoBG());
		mplew.write(g.getLogoBGColor());
		mplew.writeShort(g.getLogo());
		mplew.write(g.getLogoColor());
		mplew.writeMapleAsciiString(g.getNotice());
		mplew.writeInt(g.getGP());
		mplew.writeInt(0);
		mplew.writeInt(0);
		mplew.write(1);
		mplew.writeInt(0);

		/*
		 * mplew.writeInt(14); // 家族最大人数 mplew.writeLong(0); mplew.writeInt(0);
		 * mplew.writeInt(0); mplew.writeInt(0); mplew.writeInt(1); //家族等级??
		 * mplew.write(0);
		 */
		return mplew.getPacket();
	}
}
