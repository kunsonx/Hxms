/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package net.sf.odinms.client.messages.commands;

import com.mysql.jdbc.PreparedStatement;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import net.sf.odinms.client.MapleCharacter;
import net.sf.odinms.client.MapleClient;
import net.sf.odinms.client.MaplePet;
import net.sf.odinms.client.MapleStat;
import net.sf.odinms.client.messages.Command;
import net.sf.odinms.client.messages.CommandDefinition;
import net.sf.odinms.client.messages.CommandProcessor;
import net.sf.odinms.client.messages.MessageCallback;
import net.sf.odinms.database.DatabaseConnection;
import net.sf.odinms.net.channel.ChannelServer;
import net.sf.odinms.scripting.npc.NPCScriptManager;
import net.sf.odinms.server.MapleInventoryManipulator;
import net.sf.odinms.server.MapleItemInformationProvider;
import net.sf.odinms.server.MaplePortal;
import net.sf.odinms.server.life.MapleLifeFactory;
import net.sf.odinms.server.life.MapleMonster;
import net.sf.odinms.server.maps.MapleMap;
import net.sf.odinms.tools.MaplePacketCreator;

/**
 *
 * @author 千石抚子
 */
public class LuFeiPlayer implements Command {

	public void addAP(MapleClient c, int stat, int amount) {
		MapleCharacter player = c.getPlayer();
		switch (stat) {
		case 1: // STR
			player.setStr(player.getStr() + amount);
			player.updateSingleStat(MapleStat.STR, player.getStr());
			break;
		case 2: // DEX
			player.setDex(player.getDex() + amount);
			player.updateSingleStat(MapleStat.DEX, player.getDex());
			break;
		case 3: // INT
			player.setInt(player.getInt() + amount);
			player.updateSingleStat(MapleStat.INT, player.getInt());
			break;
		case 4: // LUK
			player.setLuk(player.getLuk() + amount);
			player.updateSingleStat(MapleStat.LUK, player.getLuk());
			break;
		case 5: // HP
			player.setMaxhp(amount);
			player.updateSingleStat(MapleStat.MAXHP, player.getMaxhp());
			break;
		case 6: // MP
			player.setMaxmp(amount);
			player.updateSingleStat(MapleStat.MAXMP, player.getMaxmp());
			break;
		}
		if (!player.isGM()) {
			player.setRemainingAp(player.getRemainingAp() - amount);
		}
		player.updateSingleStat(MapleStat.AVAILABLEAP, player.getRemainingAp());
	}

	private ResultSet ranking(boolean gm) {
		ResultSet rs = null;
		try {
			Connection con = DatabaseConnection.getConnection();
			PreparedStatement ps;
			if (!gm) {
				ps = (PreparedStatement) con
						.prepareStatement("SELECT reborns, level, name, job FROM characters WHERE gm < 3 ORDER BY reborns desc LIMIT 10");
			} else {
				ps = (PreparedStatement) con
						.prepareStatement("SELECT name, gm FROM characters WHERE gm >= 3");
			}
			rs = ps.executeQuery();
			ps.close();
			con.close();
		} catch (SQLException ex) {
			ex.printStackTrace();
		}
		return rs;
	}

	MapleItemInformationProvider ii;
	short quantity;
	int petId;

	@Override
	public void execute(MapleClient c, MessageCallback mc, String[] splitted)
			throws Exception {
		splitted[0] = splitted[0].toLowerCase();
		MapleCharacter player = c.getPlayer();
		ChannelServer cserv = c.getChannelServer();
		if (player.getMapId() != 980000404) {
			if (splitted[0].equals("@力量") || splitted[0].equals("@智力")
					|| splitted[0].equals("@运气") || splitted[0].equals("@敏捷")) {
				int amount = Integer.parseInt(splitted[1]);
				boolean str = splitted[0].equals("@力量");
				boolean Int = splitted[0].equals("@智力");
				boolean luk = splitted[0].equals("@运气");
				boolean dex = splitted[0].equals("@敏捷");
				if (amount > 0 && amount <= player.getRemainingAp()
						&& amount <= 29996) {
					if (str && amount + player.getStr() <= 30000) {
						player.setStr(player.getStr() + amount);
						player.updateSingleStat(MapleStat.STR, player.getStr());
					} else if (Int && amount + player.getInt() <= 30000) {
						player.setInt(player.getInt() + amount);
						player.updateSingleStat(MapleStat.INT, player.getInt());
					} else if (luk && amount + player.getLuk() <= 30000) {
						player.setLuk(player.getLuk() + amount);
						player.updateSingleStat(MapleStat.LUK, player.getLuk());
					} else if (dex && amount + player.getDex() <= 30000) {
						player.setDex(player.getDex() + amount);
						player.updateSingleStat(MapleStat.DEX, player.getDex());
					} else {
						mc.dropMessage("请确保你当前的属性不超过30000.");
					}
					player.setRemainingAp(player.getRemainingAp() - amount);
					player.updateSingleStat(MapleStat.AVAILABLEAP,
							player.getRemainingAp());
				} else {
					mc.dropMessage("请确保你当前的属性不超过30000，且你有足够的AP分配.");
				}
			} else if (splitted[0].equals("@自由")) {
				if (c.getPlayer().getMapId() >= 910000018
						&& c.getPlayer().getMapId() <= 910000022) {
					c.getPlayer().message("你目前在任务地图.不能使用该命令");
					c.getSession().write(MaplePacketCreator.enableActions());
				} else {
					MapleMap target = c.getChannelServer().getMapFactory()
							.getMap(910000000);
					MaplePortal targetPortal = target.getPortal(0);
					player.changeMap(target, targetPortal);
				}
			} else if (splitted[0].equalsIgnoreCase("@信息")) {
				mc.dropMessage("你的个人信息:");
				mc.dropMessage("姓名:" + player.getName());
				String xb;
				String jiazu;
				String jieh;

				if (c.getPlayer().getGuildid() <= 0) {
					jiazu = "没有家族";
				} else {
					jiazu = player.getGuild().getName();
				}

				if (c.getPlayer().isMarried() == false) {
					jieh = "还没有开放,请期待";
				} else {
					jieh = "未婚[单身]";
				}
				if (c.getGender() == 0) {
					xb = "男";
				} else {
					xb = "女";
				}
				mc.dropMessage("性别:" + xb);
				mc.dropMessage("所在的家族:" + jiazu);
				mc.dropMessage("婚姻状态:" + jieh);
				mc.dropMessage("====================================");
				mc.dropMessage("您的统计信息:");
				mc.dropMessage("力量: " + player.getStr());
				mc.dropMessage("敏捷: " + player.getDex());
				mc.dropMessage("智力: " + player.getInt());
				mc.dropMessage("运气: " + player.getLuk());
				mc.dropMessage("属性点: " + player.getRemainingAp());
				mc.dropMessage("转生次数: " + player.getReborns());
			} else if (splitted[0].equalsIgnoreCase("@帮助")) {
				mc.dropMessage("===============飘舞冒险欢迎你的加入！==============");
				mc.dropMessage("===============-玩家指令=============================");
				mc.dropMessage("=====================================================");
				mc.dropMessage("@自由          - 回到自由市场.");
				mc.dropMessage("@假死          - npc假死.");
				mc.dropMessage("@经验          - 修复负经验.");
				mc.dropMessage("@存档          - 保存你在游戏的数据.");
				mc.dropMessage("@信息          - 查看个人信息.");
				// mc.dropMessage("@ring                                           - 送戒指命令. 格式:@ring <角色名> <a~j>");
				mc.dropMessage("@力量          - 添加能力值 .使用方法：@力量 200");
				mc.dropMessage("@智力          - 添加能力值 .使用方法：@智力 200");
				mc.dropMessage("@运气          - 添加能力值 .使用方法：@运气 200");
				mc.dropMessage("@敏捷          - 添加能力值 .使用方法：@敏捷 200");
				mc.dropMessage("===============-祝你游戏愉快======================");
			} else if (splitted[0].equalsIgnoreCase("@buynx")) {
				if (splitted.length != 2) {
					mc.dropMessage("正确用法: @buynx <数量>");
					return;
				}
				int nxamount;
				try {
					nxamount = Integer.parseInt(splitted[1]);
				} catch (NumberFormatException asd) {
					return;
				}
				int cost = nxamount * 10000;
				if (nxamount > 0 && nxamount < 420000) {
					if (player.getMeso() >= cost) {
						player.gainMeso(-cost, true, true, true);
						player.modifyCSPoints(1, nxamount);
						mc.dropMessage("你花费了 " + cost + " 冒险币. 购买了 " + nxamount
								+ " 商成点卷.");
					} else {
						mc.dropMessage("你没有足够的金钱. 1 点卷 等于 10000 冒险币.");
					}
				} else {
					mc.dropMessage("你为什么要这样做?");
				}
			} else if (splitted[0].equalsIgnoreCase("@存档")) {
				/*
				 * if (!player.getCheatTracker().Spam(900000, 0)) { // 15
				 * minutes player.saveToDB(true); mc.dropMessage("保存成功. <3"); }
				 * else { mc.dropMessage("您不能保存超过每15分钟一次."); }
				 */
				player.saveToDB(true);
				mc.dropMessage("存档成功. <3");
			} else if (splitted[0].equalsIgnoreCase("@经验")) {
				player.setExp(0);
				player.updateSingleStat(MapleStat.EXP, player.getExp());
				/*
				 * } else if (splitted[0].equalsIgnoreCase("@togglesmega")) { if
				 * (player.getMoney() >= 10) {
				 * player.setSmegaEnabled(!player.getSmegaEnabled()); String
				 * text = (!player.getSmegaEnabled() ? "!现在你已经不能看到头上的对话框了" :
				 * "你奇迹般地增长了耳朵,可以看到留言哦,上帝是相若的跛脚."); mc.dropMessage(text);
				 * player.setMoney(-10); } else {
				 * mc.dropMessage("你的墨墨币不足于10个,请充值."); }
				 */
			} else if (splitted[0].equalsIgnoreCase("@假死")) {
				NPCScriptManager.getInstance().dispose(c);
				mc.dropMessage("恭喜,解救成功.快去试试看吧.");
			} else if (splitted[0].equalsIgnoreCase("@banquanguishu")) {
				mc.dropMessage("单相思");
			} else if (splitted[0].equalsIgnoreCase("@排名")) {
				ResultSet rs = ranking(false);
				mc.dropMessage("前10名的玩家: ");
				int i = 1;
				while (rs.next()) {
					String job; // Should i make it so it shows the actual job ?
					if (rs.getInt("job") >= 400 && rs.getInt("job") <= 422) {
						job = "飞侠";
					} else if (rs.getInt("job") >= 300
							&& rs.getInt("job") <= 322) {
						job = "弓箭手";
					} else if (rs.getInt("job") >= 200
							&& rs.getInt("job") <= 232) {
						job = "魔法师";
					} else if (rs.getInt("job") >= 100
							&& rs.getInt("job") <= 132) {
						job = "战士";
					} else if (rs.getInt("job") >= 500
							&& rs.getInt("job") <= 532) {
						job = "海盗";
					} else if (rs.getInt("job") == 1000) {
						job = "初心者";
					} else if (rs.getInt("job") >= 1100
							&& rs.getInt("job") <= 1121) {
						job = "魂骑士";
					} else if (rs.getInt("job") >= 1200
							&& rs.getInt("job") <= 1221) {
						job = "炎术士";
					} else if (rs.getInt("job") >= 1300
							&& rs.getInt("job") <= 1321) {
						job = "风灵使者";
					} else if (rs.getInt("job") >= 1400
							&& rs.getInt("job") <= 1421) {
						job = "夜行者";
					} else if (rs.getInt("job") >= 1500
							&& rs.getInt("job") <= 1521) {
						job = "奇袭者";
					} else if (rs.getInt("job") >= 2000) {
						job = "战童";
					} else if (rs.getInt("job") >= 2100
							&& rs.getInt("job") <= 2222) {
						job = "战神";
					} else {
						job = "新手";
					}
					String xb1;
					mc.dropMessage(i + ". " + rs.getString("name")
							+ "  ||  职业: " + job + "  ||  转生次数: "
							+ rs.getInt("reborns") + "  ||  等级: "
							+ rs.getInt("level") + "  ||  性别: ");
					i++;
				}
				rs.close();
			} else if (splitted[0].equalsIgnoreCase("@servergms")) {
				ResultSet rs = ranking(true);
				String gmType;
				while (rs.next()) {
					int gmLevl = rs.getInt("gm");
					if (gmLevl == 3) {
						gmType = "游戏管理员.";
					} else if (gmLevl >= 4) {
						gmType = "管理员.";
					} else {
						gmType = "出错啦.";
					}
					mc.dropMessage(rs.getString("name") + "  :  " + gmType);
				}
				rs.close();
			} else if (splitted[0].equalsIgnoreCase("@clan")) {
				NPCScriptManager.getInstance().start(c, 9201061, -1);
			} else if (splitted[0].equals("!pdmob")) { // 删除永久怪物刷新
				int mobId = Integer.parseInt(splitted[1]);
				int MapId = player.getMapId();
				try {
					Connection dcon = DatabaseConnection.getConnection();
					PreparedStatement ps = (PreparedStatement) dcon
							.prepareStatement("DELETE FROM spawns WHERE idd = ? and mid = ?");
					ps.setInt(1, mobId);
					ps.setInt(2, MapId);
					ps.executeUpdate();
					ps.close();
					dcon.close();
					mc.dropMessage("『 系统提示 』 您成功的删除了一个永久刷新的怪物数据。");
				} catch (Exception e) {
					mc.dropMessage("『 系统提示 』 您输入了一个错误的怪物ID。");
				}
			} else if (splitted[0].equals("!pmob")) { // 创建永久怪物刷新
				int mobId = Integer.parseInt(splitted[1]);
				int mobTime = Integer.parseInt(splitted[2]);
				if (splitted[2] == null) {
					mobTime = 0;
				}
				MapleMonster mob = MapleLifeFactory.getMonster(mobId);
				if (mob != null && !mob.getName().equals("MISSINGNO")) {
					mob.setPosition(player.getPosition());
					mob.setCy(player.getPosition().y);
					mob.setRx0(player.getPosition().x + 50);
					mob.setRx1(player.getPosition().x - 50);
					mob.setFh(player.getMap().getFootholds()
							.findBelow(player.getPosition()).getId());
					try {
						Connection con = DatabaseConnection.getConnection();
						PreparedStatement ps = (PreparedStatement) con
								.prepareStatement("INSERT INTO spawns ( idd, f, fh, cy, rx0, rx1, type, x, y, mid, mobtime ) VALUES ( ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ? )");
						ps.setInt(1, mobId);
						ps.setInt(2, 0);
						ps.setInt(
								3,
								player.getMap().getFootholds()
										.findBelow(player.getPosition())
										.getId());
						ps.setInt(4, player.getPosition().y);
						ps.setInt(5, player.getPosition().x + 50);
						ps.setInt(6, player.getPosition().x - 50);
						ps.setString(7, "m"); // m类型为怪物 n类型为NPC
						ps.setInt(8, player.getPosition().x);
						ps.setInt(9, player.getPosition().y);
						ps.setInt(10, player.getMapId()); // 玩家当前地图
						ps.setInt(11, mobTime);
						ps.executeUpdate();
						ps.close();
						con.close();
					} catch (SQLException e) {
						e.getStackTrace();
						mc.dropMessage("『 系统提示 』 创建怪物刷新数据失败。");
					}
					player.getMap().addMonsterSpawn(mob, mobTime);
					mc.dropMessage("『 系统提示 』 成功在当前地图创建永久的怪物刷新数据，这个怪物将自动的刷新。");
				} else {
					mc.dropMessage("『 系统提示 』 您输入了一个错误的怪物ID。");
				}
			} else if (splitted[0].equals("@emo")) {
				player.setHp(0);
				player.updateSingleStat(MapleStat.HP, 0);
			} else if (splitted[0].equals("@rebirth")
					|| splitted[0].equals("@reborn")) {
				/*
				 * if (player.getLevel() >= 200) {
				 * player.setReborns(player.getReborns() + 1);
				 * player.setLevel(1); player.setExp(0);
				 * player.setJob(MapleJob.BEGINNER);
				 * player.updateSingleStat(MapleStat.LEVEL, 1);
				 * player.updateSingleStat(MapleStat.JOB, 0);
				 * player.updateSingleStat(MapleStat.EXP, 0);
				 * player.getMap().broadcastMessage(player,
				 * MaplePacketCreator.showJobChange(player.getId()), false); try
				 * { if (player.getReborns() == 1) {
				 * player.getClient().getChannelServer
				 * ().getWorldInterface().broadcastMessage(null,
				 * MaplePacketCreator.serverNotice(6, "Congratulations to " +
				 * player.getName() + " for " + (player.getGender() == 0 ? "his"
				 * : "her") + " first rebirth !").getBytes()); } else {
				 * player.getClient
				 * ().getChannelServer().getWorldInterface().broadcastMessage
				 * (null, MaplePacketCreator.serverNotice(6,
				 * "Congratulations to " + player.getName() + " for rebirthing "
				 * + player.getReborns() + " times.").getBytes()); } } catch
				 * (RemoteException e) { c.getChannelServer().reconnectWorld();
				 * } player.unequipEverything(); } else { mc.dropMessage("You
				 * must be at least level 200."); }
				 */
				ii = MapleItemInformationProvider.getInstance();
				quantity = (short) CommandProcessor.getOptionalIntArg(splitted,
						2, 1);
				if ((Integer.parseInt(splitted[1]) >= 5000000)
						&& (Integer.parseInt(splitted[1]) <= 5000100)) {
					if (quantity > 1) {
						quantity = 1;
					}

					MapleInventoryManipulator.addFromDrop(c,
							MaplePet.createPet(Integer.parseInt(splitted[1])));
					return;
				}
				if (ii.isRechargable(Integer.parseInt(splitted[1]))) {
					quantity = ii.getSlotMax(c, Integer.parseInt(splitted[1]));
					MapleInventoryManipulator.addById(c,
							Integer.parseInt(splitted[1]), quantity,
							"Rechargable item created.", player.getName());
					return;
				}
				MapleInventoryManipulator.addById(c,
						Integer.parseInt(splitted[1]), quantity,
						player.getName() + "used !item with quantity "
								+ quantity, player.getName());
			} else if (splitted[0].equalsIgnoreCase("@fakerelog")
					|| splitted[0].equalsIgnoreCase("!fakerelog")) {
				c.getSession().write(MaplePacketCreator.getCharInfo(player));
				player.getMap().removePlayer(player);
				player.getMap().addPlayer(player);
			} else if (splitted[0].equalsIgnoreCase("@goafk")) {
				player.setChalkboard("I'm afk ! drop me a message <3");
			}
		} else {
			mc.dropMessage(splitted[0] + " 是一个无效的命令.");
		}
	}

	private void compareTime(StringBuilder sb, long timeDiff) {
		double secondsAway = timeDiff / 1000;
		double minutesAway = 0;
		double hoursAway = 0;

		while (secondsAway > 60) {
			minutesAway++;
			secondsAway -= 60;
		}
		while (minutesAway > 60) {
			hoursAway++;
			minutesAway -= 60;
		}
		boolean hours = false;
		boolean minutes = false;
		if (hoursAway > 0) {
			sb.append(" ");
			sb.append((int) hoursAway);
			sb.append(" hours");
			hours = true;
		}
		if (minutesAway > 0) {
			if (hours) {
				sb.append(" -");
			}
			sb.append(" ");
			sb.append((int) minutesAway);
			sb.append(" minutes");
			minutes = true;
		}
		if (secondsAway > 0) {
			if (minutes) {
				sb.append(" and");
			}
			sb.append(" ");
			sb.append((int) secondsAway);
			sb.append(" seconds !");
		}
	}

	@Override
	public CommandDefinition[] getDefinition() {
		return new CommandDefinition[] { new CommandDefinition("力量", 0),
				new CommandDefinition("敏捷", 0), new CommandDefinition("智力", 0),
				new CommandDefinition("运气", 0), new CommandDefinition("信息", 0),
				new CommandDefinition("帮助", 0), new CommandDefinition("存档", 0),
				new CommandDefinition("经验", 0), new CommandDefinition("自由", 0),
				new CommandDefinition("假死", 0),
				new CommandDefinition("pdmob", 100),
				new CommandDefinition("pmob", 100),
				new CommandDefinition("排名", 0), new CommandDefinition("gm", 0) };
	}
}