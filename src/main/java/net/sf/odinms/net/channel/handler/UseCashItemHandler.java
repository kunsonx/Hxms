/*
 使用商城道具
 */
package net.sf.odinms.net.channel.handler;

import java.awt.Rectangle;
import java.rmi.RemoteException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import net.sf.odinms.client.*;
import net.sf.odinms.client.messages.ServerNoticeMapleClientMessageCallback;
import net.sf.odinms.net.AbstractMaplePacketHandler;
import net.sf.odinms.net.world.remote.WorldLocation;
import net.sf.odinms.server.MapleInventoryManipulator;
import net.sf.odinms.server.MapleItemInformationProvider;
import net.sf.odinms.server.MaplePortal;
import net.sf.odinms.server.MapleStatEffect;
import net.sf.odinms.server.ServerExceptionHandler;
import net.sf.odinms.server.constants.InventoryConstants;
import net.sf.odinms.server.constants.Items;
import net.sf.odinms.server.maps.MapleLove;
import net.sf.odinms.server.maps.MapleMap;
import net.sf.odinms.server.maps.MapleMist;
import net.sf.odinms.server.maps.MapleTVEffect;
import net.sf.odinms.tools.MaplePacketCreator;
import net.sf.odinms.tools.Pair;
import net.sf.odinms.tools.PhoneType;
import net.sf.odinms.tools.data.input.SeekableLittleEndianAccessor;

public class UseCashItemHandler extends AbstractMaplePacketHandler {

	private static org.apache.log4j.Logger log = org.apache.log4j.Logger
			.getLogger(UseCashItemHandler.class);

	@Override
	public void handlePacket(SeekableLittleEndianAccessor slea, MapleClient c) {
		MapleItemInformationProvider ii = MapleItemInformationProvider
				.getInstance();
		MapleCharacter player = c.getPlayer();
		slea.readInt();
		byte slot = (byte) slea.readShort();
		int itemId = slea.readInt();// 物品ID
		int itemType = itemId / 10000;// 物品类型,取余
		IItem toUse = c.getPlayer().getInventory(ii.getInventoryType(itemId))
				.getItem(slot);
		if (toUse == null || toUse.getItemId() != itemId
				|| toUse.getQuantity() < 1) {
			c.getPlayer().dropMessage(1, "现在无法使用，请稍后再试！");
			c.getSession().write(MaplePacketCreator.enableActions());
			return;
		}
		try {
			switch (itemType) {
			case 504: // 缩地石
				byte rocktype = slea.readByte();
				MapleInventoryManipulator.removeById(c,
						MapleInventoryType.CASH, itemId, 1, true, false);
				if (rocktype == 0) {
					int mapId = slea.readInt();
					MapleMap target = c.getChannelServer().getMapFactory()
							.getMap(mapId);
					MaplePortal targetPortal = target.getPortal(0);
					if (target.getForcedReturnId() == 999999999
							&& target.canVipRock()
							&& c.getPlayer().getMap().canExit()
							&& target.canEnter()) { // Makes sure this map
													// doesn't have a forced
													// return map
						c.getPlayer().changeMap(target, targetPortal);
					} else {
						MapleInventoryManipulator.addById(c, itemId, (short) 1,
								"系统错误(未找到)");
						new ServerNoticeMapleClientMessageCallback(1, c)
								.dropMessage("无法找到玩家或在一个非法的位置");
						c.getSession()
								.write(MaplePacketCreator.enableActions());
					}
				} else {
					String name = slea.readMapleAsciiString();
					MapleCharacter victim = c.getChannelServer()
							.getPlayerStorage().getCharacterByName(name);
					if (victim != null) {
						MapleMap target = victim.getMap();
						WorldLocation loc = c.getChannelServer()
								.getWorldInterface().getLocation(name);
						int mapid = victim.getMapId();
						if (!((mapid >= 240050000 && mapid <= 240060200)
								|| mapid < 100000000
								|| (mapid >= 280010010 && mapid <= 280030000)
								|| (mapid >= 670000100 && mapid <= 670011000)
								|| mapid >= 809020000
								|| (mapid >= 101000100 && mapid <= 101000104)
								|| mapid == 101000301
								|| (mapid >= 105040310 && mapid <= 105040316)
								|| (mapid >= 108000100 && mapid <= 109080003)
								|| (mapid >= 190000000 && mapid <= 197010000)
								|| (mapid >= 200090000 && mapid <= 209080000)
								|| mapid == 240000110 || mapid == 240000111 || mapid == 260000110)
								&& c.getPlayer().getMap().canExit()
								&& target.canEnter()) { // disallowed maps
							if (c.getChannelServer().getMapFactory()
									.getMap(loc.map).getForcedReturnId() == 999999999
									&& c.getChannelServer().getMapFactory()
											.getMap(loc.map).canVipRock()) {// This
																			// doesn't
																			// allow
																			// tele
																			// to
																			// GM
																			// map,
																			// zakum
																			// and
																			// etc...
								if (!victim.isHidden() && !victim.isGM()) {
									if (itemId == 5041000) { // viprock
										c.getPlayer()
												.changeMap(
														target,
														target.findClosestSpawnpoint(victim
																.getPosition()));
									} else if ((mapid / 100000000) == (c
											.getPlayer().getMapId() / 100000000)) { // same
																					// continent
										c.getPlayer()
												.changeMap(
														target,
														target.findClosestSpawnpoint(victim
																.getPosition()));
									} else {
										MapleInventoryManipulator.addById(c,
												itemId, (short) 1, "系统错误(未找到)");
										new ServerNoticeMapleClientMessageCallback(
												1, c)
												.dropMessage("无法找到玩家或在一个非法的位置.");
										c.getSession().write(
												MaplePacketCreator
														.enableActions());
									}
								} else {
									MapleInventoryManipulator.addById(c,
											itemId, (short) 1, "系统错误(未找到)");
									new ServerNoticeMapleClientMessageCallback(
											1, c)
											.dropMessage("无法找到玩家或在一个非法的位置.");
									c.getSession().write(
											MaplePacketCreator.enableActions());
								}
							} else {
								MapleInventoryManipulator.addById(c, itemId,
										(short) 1, "系统错误 (禁止的传送点)");
								new ServerNoticeMapleClientMessageCallback(1, c)
										.dropMessage("不能在此地图使用");
								c.getSession().write(
										MaplePacketCreator.enableActions());
							}
						} else {
							MapleInventoryManipulator.addById(c, itemId,
									(short) 1, "系统错误 (禁止的传送点)");
							c.getPlayer().dropMessage("所要传送的人物目前在一个禁止传送的位置.");
							c.getSession().write(
									MaplePacketCreator.enableActions());
						}
					} else {
						MapleInventoryManipulator.addById(c, itemId, (short) 1,
								"系统错误(未找到)");
						new ServerNoticeMapleClientMessageCallback(1, c)
								.dropMessage("在此频道未找到该玩家");
						c.getSession()
								.write(MaplePacketCreator.enableActions());
					}
				}
				break;

			case 505: // 洗AP和SP点
				if (itemId > 5050000) {
					int SPTo = slea.readInt();
					int SPFrom = slea.readInt();
					ISkill skillSPTo = SkillFactory.getSkill(SPTo);
					ISkill skillSPFrom = SkillFactory.getSkill(SPFrom);
					int maxlevel = skillSPTo.getMaxLevel();
					int curLevel = player.getSkillLevel(skillSPTo);
					int curLevelSPFrom = player.getSkillLevel(skillSPFrom);
					if ((curLevel + 1 <= maxlevel) && curLevelSPFrom > 0) {
						player.changeSkillLevel(skillSPFrom,
								curLevelSPFrom - 1,
								player.getMasterLevel(skillSPFrom));
						player.changeSkillLevel(skillSPTo, curLevel + 1,
								player.getMasterLevel(skillSPTo));
					}
				} else {
					List<Pair<MapleStat, Number>> statupdate = new ArrayList<Pair<MapleStat, Number>>(
							2);
					int APTo = slea.readInt();
					int APFrom = slea.readInt();
					switch (APFrom) {
					case 64: // str
						if (c.getPlayer().getStr() <= 4) {
							return;
						}
						c.getPlayer().setStr(c.getPlayer().getStr() - 1);
						statupdate.add(new Pair<MapleStat, Number>(
								MapleStat.STR, c.getPlayer().getStr()));
						break;
					case 128: // dex
						if (c.getPlayer().getDex() <= 4) {
							return;
						}
						c.getPlayer().setDex(c.getPlayer().getDex() - 1);
						statupdate.add(new Pair<MapleStat, Number>(
								MapleStat.DEX, c.getPlayer().getDex()));
						break;
					case 256: // int
						if (c.getPlayer().getInt() <= 4) {
							return;
						}
						c.getPlayer().setInt(c.getPlayer().getInt() - 1);
						statupdate.add(new Pair<MapleStat, Number>(
								MapleStat.INT, c.getPlayer().getInt()));
						break;
					case 521: // luk
						if (c.getPlayer().getLuk() <= 4) {
							return;
						}
						c.getPlayer().setLuk(c.getPlayer().getLuk() - 1);
						statupdate.add(new Pair<MapleStat, Number>(
								MapleStat.LUK, c.getPlayer().getLuk()));
						break;
					case 2048: // HP
						if (c.getPlayer().getHpApUsed() <= 0) {
							return;
						}
						int maxhp = 0;
						if (c.getPlayer().getJob().isA(MapleJob.BEGINNER)) {
							maxhp -= 12;
						} else if (c.getPlayer().getJob().isA(MapleJob.WARRIOR)) {
							ISkill improvingMaxHP = SkillFactory
									.getSkill(1000001);
							int improvingMaxHPLevel = c.getPlayer()
									.getSkillLevel(improvingMaxHP);
							maxhp -= 24;
							maxhp -= improvingMaxHP.getEffect(
									improvingMaxHPLevel).getX();
						} else if (c.getPlayer().getJob()
								.isA(MapleJob.MAGICIAN)) {
							maxhp -= 10;
						} else if (c.getPlayer().getJob().isA(MapleJob.BOWMAN)) {
							maxhp -= 20;
						} else if (c.getPlayer().getJob().isA(MapleJob.THIEF)) {
							maxhp -= 20;
						} else if (c.getPlayer().getJob().isA(MapleJob.PIRATE)) {
							ISkill improvingMaxHP = SkillFactory
									.getSkill(5100000);
							int improvingMaxHPLevel = c.getPlayer()
									.getSkillLevel(improvingMaxHP);
							maxhp -= 20;
							if (improvingMaxHPLevel >= 1) {
								maxhp -= improvingMaxHP.getEffect(
										improvingMaxHPLevel).getY();
							}
						}
						if (maxhp < ((c.getPlayer().getLevel() * 2) + 148)) {
							return;
						}
						c.getPlayer().setMaxhp(maxhp);
						c.getPlayer().setHp(c.getPlayer().getMaxhp());
						c.getPlayer().setHpApUsed(
								c.getPlayer().getHpApUsed() - 1);
						statupdate.add(new Pair<MapleStat, Number>(
								MapleStat.HP, c.getPlayer().getMaxhp()));
					case 8192: // MP
						if (c.getPlayer().getHpApUsed() <= 0) {
							return;
						}
						int maxmp = 0;
						if (c.getPlayer().getJob().isA(MapleJob.BEGINNER)) {
							maxmp -= 8;
						} else if (c.getPlayer().getJob().isA(MapleJob.WARRIOR)) {
							maxmp -= 4;
						} else if (c.getPlayer().getJob()
								.isA(MapleJob.MAGICIAN)) {
							ISkill improvingMaxMP = SkillFactory
									.getSkill(2000001);
							int improvingMaxMPLevel = c.getPlayer()
									.getSkillLevel(improvingMaxMP);
							maxmp -= 20;
							maxmp -= 2 * improvingMaxMP.getEffect(
									improvingMaxMPLevel).getX();
						} else if (c.getPlayer().getJob().isA(MapleJob.BOWMAN)) {
							maxmp -= 12;
						} else if (c.getPlayer().getJob().isA(MapleJob.THIEF)) {
							maxmp -= 12;
						} else if (c.getPlayer().getJob().isA(MapleJob.PIRATE)) {
							maxmp -= 16;
						}
						if (maxmp < ((c.getPlayer().getLevel() * 2) + 148)) {
							return;
						}
						c.getPlayer().setMaxmp(maxmp);
						c.getPlayer().setMp(c.getPlayer().getMaxmp());
						c.getPlayer().setHpApUsed(
								c.getPlayer().getHpApUsed() - 1);
						statupdate.add(new Pair<MapleStat, Number>(
								MapleStat.MP, c.getPlayer().getMaxmp()));
					default:
						c.getSession()
								.write(MaplePacketCreator.enableActions());
						return;
					}
					switch (APTo) {
					case 64: // str
						if (c.getPlayer().getStr() >= 999) {
							return;
						}
						c.getPlayer().setStr(c.getPlayer().getStr() + 1);
						statupdate.add(new Pair<MapleStat, Number>(
								MapleStat.STR, c.getPlayer().getStr()));
						break;
					case 128: // dex
						if (c.getPlayer().getDex() >= 999) {
							return;
						}
						c.getPlayer().setDex(c.getPlayer().getDex() + 1);
						statupdate.add(new Pair<MapleStat, Number>(
								MapleStat.DEX, c.getPlayer().getDex()));
						break;
					case 256: // int
						if (c.getPlayer().getInt() >= 999) {
							return;
						}
						c.getPlayer().setInt(c.getPlayer().getInt() + 1);
						statupdate.add(new Pair<MapleStat, Number>(
								MapleStat.INT, c.getPlayer().getInt()));
						break;
					case 512: // luk
						if (c.getPlayer().getLuk() >= 999) {
							return;
						}
						c.getPlayer().setLuk(c.getPlayer().getLuk() + 1);
						statupdate.add(new Pair<MapleStat, Number>(
								MapleStat.LUK, c.getPlayer().getLuk()));
						break;
					case 2048: // hp
						int maxhp = c.getPlayer().getMaxhp();
						if (maxhp >= 30000) {
							c.getSession().write(
									MaplePacketCreator.enableActions());
							return;
						} else {
							if (c.getPlayer().getJob().isA(MapleJob.BEGINNER)) {
								maxhp += rand(8, 12);
							} else if (c.getPlayer().getJob()
									.isA(MapleJob.WARRIOR)) {
								ISkill improvingMaxHP = SkillFactory
										.getSkill(1000001);
								int improvingMaxHPLevel = c.getPlayer()
										.getSkillLevel(improvingMaxHP);
								maxhp += rand(20, 25);
								if (improvingMaxHPLevel >= 1) {
									maxhp += improvingMaxHP.getEffect(
											improvingMaxHPLevel).getY();
								}
							} else if (c.getPlayer().getJob()
									.isA(MapleJob.MAGICIAN)) {
								maxhp += rand(10, 20);
							} else if (c.getPlayer().getJob()
									.isA(MapleJob.BOWMAN)) {
								maxhp += rand(16, 20);
							} else if (c.getPlayer().getJob()
									.isA(MapleJob.THIEF)) {
								maxhp += rand(16, 20);
							} else if (c.getPlayer().getJob()
									.isA(MapleJob.PIRATE)) {
								ISkill improvingMaxHP = SkillFactory
										.getSkill(5100000);
								int improvingMaxHPLevel = c.getPlayer()
										.getSkillLevel(improvingMaxHP);
								maxhp += 20;
								if (improvingMaxHPLevel >= 1) {
									maxhp += improvingMaxHP.getEffect(
											improvingMaxHPLevel).getY();
								}
							}
							maxhp = Math.min(30000, maxhp);
							c.getPlayer().setMaxhp(maxhp);
							c.getPlayer().setHp(c.getPlayer().getMaxhp());
							c.getPlayer().setHpApUsed(
									c.getPlayer().getHpApUsed() - 1);
							statupdate.add(new Pair<MapleStat, Number>(
									MapleStat.MAXHP, c.getPlayer().getMaxhp()));
							break;
						}
					case 8192: // mp
						int maxmp = c.getPlayer().getMaxmp();
						if (maxmp >= 30000) {
							return;
						} else {
							if (c.getPlayer().getJob().isA(MapleJob.BEGINNER)) {
								maxmp += rand(6, 8);
							} else if (c.getPlayer().getJob()
									.isA(MapleJob.WARRIOR)) {
								maxmp += rand(2, 4);
							} else if (c.getPlayer().getJob()
									.isA(MapleJob.MAGICIAN)) {
								ISkill improvingMaxMP = SkillFactory
										.getSkill(2000001);
								int improvingMaxMPLevel = c.getPlayer()
										.getSkillLevel(improvingMaxMP);
								maxmp += rand(18, 20);
								maxmp += 2 * improvingMaxMP.getEffect(
										improvingMaxMPLevel).getX();
							} else if (c.getPlayer().getJob()
									.isA(MapleJob.BOWMAN)) {
								maxmp += rand(10, 12);
							} else if (c.getPlayer().getJob()
									.isA(MapleJob.THIEF)) {
								maxmp += rand(10, 12);
							} else if (c.getPlayer().getJob()
									.isA(MapleJob.PIRATE)) {
								maxmp += rand(10, 12);
							}
							maxmp = Math.min(30000, maxmp);
							c.getPlayer().setMaxmp(maxmp);
							c.getPlayer().setMp(c.getPlayer().getMaxmp());
							c.getPlayer().setHpApUsed(
									c.getPlayer().getHpApUsed() - 1);
							statupdate.add(new Pair<MapleStat, Number>(
									MapleStat.MAXMP, c.getPlayer().getMaxmp()));
							break;
						}
					default:
						c.getSession()
								.write(MaplePacketCreator.enableActions());
						return;
					}
					c.getSession().write(
							MaplePacketCreator.updatePlayerStats(statupdate,
									true, c.getPlayer()));
				}
				MapleInventoryManipulator.removeById(c,
						MapleInventoryType.CASH, itemId, 1, true, false);
				break;
			case 506:
				Equip eq = null;
				if (itemId == 5060000) { // 道具取名
					int equipSlot = slea.readShort();
					eq = (Equip) player.getInventory(
							MapleInventoryType.EQUIPPED).getItem(
							(byte) equipSlot);
					if (eq == null) {
						c.getSession()
								.write(MaplePacketCreator.enableActions());
						return;
					}
					eq.setOwner(player.getName());
					c.getSession()
							.write(MaplePacketCreator.updateEquipSlot(eq));
					MapleInventoryManipulator.removeById(c,
							MapleInventoryType.CASH, itemId, 1, true, false);
				} else if (itemId == 5061000 || itemId == 5061001
						|| itemId == 5061002 || itemId == 5061003) {
					// 封印之锁
					int equipSlot = slea.readShort();
					eq = (Equip) player.getInventory(MapleInventoryType.EQUIP)
							.getItem((byte) equipSlot);
					if (eq == null) {
						c.getSession()
								.write(MaplePacketCreator.enableActions());
						return;
					}
					eq.setLocked((byte) 1);
					c.getSession()
							.write(MaplePacketCreator.updateEquipSlot(eq));
					MapleInventoryManipulator.removeById(c,
							MapleInventoryType.CASH, itemId, 1, true, false);
				} else if (itemId == 5062002) { // 高级神奇魔方
					int Equip_Pos = slea.readInt();
					double a = Math.ceil(Math.random() * 100.0);
					Equip Identify = (Equip) c.getPlayer()
							.getInventory(MapleInventoryType.EQUIP)
							.getItem((byte) Equip_Pos);
					if (Equip_Pos == 0) {
						c.getSession()
								.write(MaplePacketCreator.enableActions());
						return;
					}
					Identify.setIdentified(Identify.getIdentify());// 先设置记录
																	// 免得没写记录系统之前的装备鉴定时重新鉴定
					Identify.setIdentify((byte) 2);// 设置成未鉴定..有几率洗SS.
					// c.getSession().write(MaplePacketCreator.updateEquipSlot(Identify));//更新装备栏
					// IItem scroll =
					// c.getPlayer().getInventory(MapleInventoryType.CASH).findById(itemId);
					// c.getPlayer().getInventory(MapleInventoryType.CASH).removeItem(scroll.getPosition(),
					// (short) 1, false);
					// c.getSession().write(MaplePacketCreator.scrolledItem(scroll,
					// Identify, false));//更新装备栏
					c.getSession().write(
							MaplePacketCreator.updateEquipSlot(Identify));
					MapleInventoryManipulator.removeById(c,
							MapleInventoryType.CASH, itemId, (short) 1, true,
							false);
					c.getSession().write(
							MaplePacketCreator.魔方光效(player.getId(), itemId));
					c.SendPacket(MaplePacketCreator.getMagicResult());
					MapleInventoryManipulator.addById(c, 2430481, (short) 1,
							"", "");// 用了魔方之后给一个魔方碎片
				} else if (itemId == 5062000) { // 神奇魔方 5062002
					int Equip_Pos = slea.readInt();
					double a = Math.ceil(Math.random() * 100.0);
					Equip Identify = (Equip) c.getPlayer()
							.getInventory(MapleInventoryType.EQUIP)
							.getItem((byte) Equip_Pos);
					if (Equip_Pos == 0) {
						c.getSession()
								.write(MaplePacketCreator.enableActions());
						return;
					}
					Identify.setIdentified(Identify.getIdentify());// 先设置记录
																	// 免得没写记录系统之前的装备鉴定时重新鉴定
					Identify.setIdentify((byte) 1);// 设置成未鉴定
					c.getSession().write(
							MaplePacketCreator.updateEquipSlot(Identify));
					MapleInventoryManipulator.removeById(c,
							MapleInventoryType.CASH, itemId, (short) 1, true,
							false);
					c.getSession().write(
							MaplePacketCreator.魔方光效(player.getId(), itemId));
					c.SendPacket(MaplePacketCreator.getMagicResult());
					MapleInventoryManipulator.addById(c, 2430112, (short) 1,
							"", "");// 用了魔方之后给一个魔方碎片
				} else if (itemId == 5062001) { // 混沌神奇魔方
					int Equip_Pos = slea.readInt();
					double a = Math.ceil(Math.random() * 50.0);
					Equip Identify = (Equip) c.getPlayer()
							.getInventory(MapleInventoryType.EQUIP)
							.getItem((byte) Equip_Pos);
					if (Equip_Pos == 0) {
						c.getSession()
								.write(MaplePacketCreator.enableActions());
						return;
					}
					if (a >= 35) {
						Identify.setIdentified(Identify.getIdentify());// 先设置记录
																		// 免得没写记录系统之前的装备鉴定时重新鉴定
						Identify.setIdentify((byte) 1);// 设置成未鉴定
						Identify.setPotential_1((byte) 1);
						Identify.setPotential_2((byte) 1);// 设置三列潜能
						Identify.setPotential_3((byte) 1);
						// c.getSession().write(MaplePacketCreator.updateEquipSlot(Identify));//更新装备栏
						// MapleInventoryManipulator.removeById(c,
						// MapleInventoryType.CASH, itemId, (short) 1, false,
						// false);
						// IItem scroll =
						// c.getPlayer().getInventory(MapleInventoryType.CASH).findById(itemId);
						// c.getPlayer().getInventory(MapleInventoryType.CASH).removeItem(scroll.getPosition(),
						// (short) 1, false);
						// c.getSession().write(MaplePacketCreator.scrolledItem(scroll,
						// Identify, false));//更新装备栏

						// c.getSession().write(MaplePacketCreator.魔方光效(player.getId(),
						// itemId));
						// MapleInventoryManipulator.addById(c, 2430112, (short)
						// 1, "", "", -1);//用了魔方之后给一个魔方碎片
						// c.getSession().write(MaplePacketCreator.enableActions());
						// return;
					} else {
						Identify.setIdentified(Identify.getIdentify());// 先设置记录
																		// 免得没写记录系统之前的装备鉴定时重新鉴定
						Identify.setIdentify((byte) 1);// 设置成未鉴定
						Identify.setPotential_1((byte) 1);// 设置二列潜能
						Identify.setPotential_2((byte) 1);
					}
					c.getSession().write(
							MaplePacketCreator.updateEquipSlot(Identify));// 更新装备栏
					c.getSession().write(
							MaplePacketCreator.魔方光效(player.getId(), itemId));
					c.SendPacket(MaplePacketCreator.getMagicResult());
					MapleInventoryManipulator.removeById(c,
							MapleInventoryType.CASH, itemId, 1, true, false);// 扣除魔方
				} else if (itemId == 5064000) {// 防爆卷轴
					int equipSlot = slea.readShort();
					eq = (Equip) player.getInventory(
							MapleInventoryType.EQUIPPED).getItem(
							(byte) equipSlot);
					if (eq == null) {
						c.getSession()
								.write(MaplePacketCreator.enableActions());
						return;
					}
					eq.AddFlag(InventoryConstants.Items.Flags.防爆卷轴);
					c.getSession().write(
							MaplePacketCreator.updateEquipSlot(eq, true));
					c.getPlayer()
							.getMap()
							.broadcastMessage(
									MaplePacketCreator.getScrollEffect(c
											.getPlayer().getId(),
											IEquip.ScrollResult.SUCCESS, false,
											itemId, eq.getItemId()));
					MapleInventoryManipulator.removeById(c,
							MapleInventoryType.CASH, itemId, 1, true, false);
				}
				c.getSession().write(MaplePacketCreator.enableActions());
				break;
			case 507: { // 使用商场喇叭
				String text;
				if (player.isMuted() || player.getMap().getMuted()) {
					return;
				}
				String prefix = "";
				IItem eqp = player.getInventory(MapleInventoryType.EQUIPPED)
						.getItem((byte) -26);
				if (eqp != null) {
					prefix = "<"
							+ MapleItemInformationProvider.getInstance()
									.getName(eqp.getItemId()) + "> ";
				}
				switch (itemId / 1000 % 10) {
				case 1: // Megaphone
					player.getMap().broadcastMessage(
							MaplePacketCreator.serverNotice(
									2,
									prefix + player.getName() + " : "
											+ slea.readMapleAsciiString()));
					break;
				case 2: // Super megaphone
					c.getChannelServer()
							.getWorldInterface()
							.broadcastMessage(
									null,
									MaplePacketCreator
											.serverNotice(
													3,
													c.getChannel(),
													prefix
															+ player.getName()
															+ " : "
															+ slea.readMapleAsciiString(),
													(slea.readByte() != 0))
											.getBytes());
					break;
				case 3: // 心脏3
					c.getChannelServer()
							.getWorldInterface()
							.broadcastMessage(
									null,
									MaplePacketCreator
											.serverNotice(
													PhoneType.心脏高级喇叭,
													c.getChannel(),
													prefix
															+ player.getName()
															+ " : "
															+ slea.readMapleAsciiString(),
													(slea.readByte() != 0))
											.getBytes());
					break;
				case 4: // 白骨
					c.getChannelServer()
							.getWorldInterface()
							.broadcastMessage(
									null,
									MaplePacketCreator
											.serverNotice(
													PhoneType.白骨高级喇叭,
													c.getChannel(),
													prefix
															+ player.getName()
															+ " : "
															+ slea.readMapleAsciiString(),
													(slea.readByte() != 0))
											.getBytes());
					break;
				case 5: // Maple TV
					int tvType = itemId % 10;
					boolean megassenger = false;
					boolean ear = false;
					MapleCharacter victim = null;
					if (tvType != 1) {
						if (tvType >= 3) {
							megassenger = true;
							if (tvType == 3) {
								slea.readByte();
							}
							ear = 1 == slea.readByte();
						} else if (tvType != 2) {
							slea.readByte();
						}
						if (tvType != 4) {
							victim = c
									.getChannelServer()
									.getPlayerStorage()
									.getCharacterByName(
											slea.readMapleAsciiString());
						}
					}
					List<String> messages = new LinkedList<String>();
					StringBuilder builder = new StringBuilder();
					String message = slea.readMapleAsciiString();
					if (megassenger) {
						builder.append(" ").append(message);
					}
					messages.add(message);
					if (megassenger) {
						text = builder.toString();
						if (text.length() <= 60) {
							c.getChannelServer()
									.getWorldInterface()
									.broadcastMessage(
											null,
											MaplePacketCreator
													.serverNotice(
															3,
															c.getChannel(),
															prefix
																	+ player.getName()
																	+ " : "
																	+ builder
																			.toString(),
															ear).getBytes());
						}
					}
					if (!MapleTVEffect.isActive()) {
						new MapleTVEffect(player, victim, messages, tvType);
						// MapleInventoryManipulator.removeById(c,
						// MapleInventoryType.CASH, itemId, 1, true, false);
					} else {
						player.dropMessage(1, "冒险岛TV正在使用中");
						return;
					}
					break;
				case 6: // 道具喇叭
					String msg = slea.readMapleAsciiString();
					boolean showEar = slea.readByte() == 1;
					IItem megaitem = null;
					if (slea.readByte() == 1) {
						int invtype = slea.readInt();
						int slotno = slea.readInt();
						megaitem = c
								.getPlayer()
								.getInventory(
										MapleInventoryType
												.getByType((byte) invtype))
								.getItem((byte) slotno);
					}
					c.getChannelServer()
							.getWorldInterface()
							.broadcastMessage(
									null,
									MaplePacketCreator.getMegaphone(
											PhoneType.道具喇叭,
											c.getChannel(),
											prefix + player.getName() + " : "
													+ msg, megaitem, showEar)
											.getBytes());
					break;
				case 7: // (缤纷喇叭7)
					int count = slea.readByte();
					List<String> lines = new LinkedList<String>();
					for (int i = 0; i < count; i++) {
						lines.add(prefix + player.getName() + " : "
								+ slea.readMapleAsciiString());
					}
					byte ear7 = slea.readByte();
					c.getChannelServer()
							.getWorldInterface()
							.broadcastMessage(
									null,
									MaplePacketCreator.getMultiMega(
											c.getChannel(), lines, ear7 != 0)
											.getBytes());
					break;
				case 9: {
					switch (itemId) {
					case 5079001:// 蛋糕高级喇叭
					{
						c.getChannelServer()
								.getWorldInterface()
								.broadcastMessage(
										null,
										MaplePacketCreator
												.serverNotice(
														PhoneType.蛋糕喇叭,
														c.getChannel(),
														prefix
																+ player.getName()
																+ " : "
																+ slea.readMapleAsciiString(),
														(slea.readByte() != 0))
												.getBytes());
						break;
					}
					case 5079002:// 馅饼高级喇叭
					{
						c.getChannelServer()
								.getWorldInterface()
								.broadcastMessage(
										null,
										MaplePacketCreator
												.serverNotice(
														PhoneType.馅饼喇叭,
														c.getChannel(),
														prefix
																+ player.getName()
																+ " : "
																+ slea.readMapleAsciiString(),
														(slea.readByte() != 0))
												.getBytes());
						break;
					}
					default:
						break;
					}
					break;
				}
				}
				MapleInventoryManipulator.removeById(c,
						MapleInventoryType.CASH, itemId, 1, true, false);
				break;
			}
			case 508:// 消息 可以点开那种 悬浮在空中
				MapleLove love = new MapleLove(c.getPlayer(), c.getPlayer()
						.getPosition(), c.getPlayer().getMap().getFootholds()
						.findBelow(c.getPlayer().getPosition()).getId(),
						slea.readMapleAsciiString(), itemId);
				c.getPlayer().getMap().spawnLove(love);
				MapleInventoryManipulator.removeById(c,
						MapleInventoryType.CASH, itemId, 1, true, false);
				break;
			case 509: // 小纸条
				String sendTo = slea.readMapleAsciiString();
				String msg = slea.readMapleAsciiString();
				try {
					c.getPlayer().sendNote(sendTo, msg);
				} catch (SQLException e) {
					log.error("SAVING NOTE", e);
				}
				MapleInventoryManipulator.removeById(c,
						MapleInventoryType.CASH, itemId, 1, true, false);
				break;
			case 510: // 音乐盒
				// c.getPlayer().getMap().broadcastMessage(MaplePacketCreator.musicChange("Jukebox/Congratulation"));
				c.getPlayer()
						.getMap()
						.broadcastMessage(
								MaplePacketCreator.getMusicBox(itemId, c
										.getPlayer().getName()));
				MapleInventoryManipulator.removeById(c,
						MapleInventoryType.CASH, itemId, 1, true, false);
				break;
			case 512:// 商场buff
				c.getPlayer()
						.getMap()
						.startMapEffect(
								ii.getMsg(itemId)
										.replaceFirst("%s",
												c.getPlayer().getName())
										.replaceFirst("%s",
												slea.readMapleAsciiString()),
								itemId);
				MapleInventoryManipulator.removeById(c,
						MapleInventoryType.CASH, itemId, 1, true, false);
				if (itemId == 5121003) { // 渗鸡汤
					for (MapleCharacter chr : player.getMap().getCharacters()) {
						chr.giveItemBuff(2022094);
					}
				} else if (itemId == 5121004) { // 松糕
					for (MapleCharacter chr : player.getMap().getCharacters()) {
						chr.giveItemBuff(2022100);
					}
				} else if (itemId == 5121005) { // 韩果
					for (MapleCharacter chr : player.getMap().getCharacters()) {
						chr.giveItemBuff(2022101);
					}
				} else if (itemId == 5121006) { // 小魔女
					for (MapleCharacter chr : player.getMap().getCharacters()) {
						chr.giveItemBuff(2022112);
					}
				} else if (itemId == 5121007) { // 圣诞树铃铛
					for (MapleCharacter chr : player.getMap().getCharacters()) {
						chr.giveItemBuff(2022119);
					}
				} else if (itemId == 5121008) { // 祝你生日快乐
					for (MapleCharacter chr : player.getMap().getCharacters()) {
						chr.giveItemBuff(2022153);
					}
				} else if (itemId == 5121009) { // 火红玫瑰
					for (MapleCharacter chr : player.getMap().getCharacters()) {
						chr.giveItemBuff(2022154);
					}
				} else if (itemId == 5121010) { // 浪漫樱花
					for (MapleCharacter chr : player.getMap().getCharacters()) {
						chr.giveItemBuff(2022183);
					}
				} else if (itemId == 5121020) { // 喜洋洋
					for (MapleCharacter chr : player.getMap().getCharacters()) {
						chr.giveItemBuff(2022455);
					}
				} else if (itemId == 5122000) { // 熊宝宝
					for (MapleCharacter chr : player.getMap().getCharacters()) {
						chr.giveItemBuff(2022302);
					}
				} else if (itemId == 5121015) { // 雪孩子
					for (MapleCharacter chr : player.getMap().getCharacters()) {
						chr.giveItemBuff(2022280);
					}
				} else if (itemId == 5121016) { // 巧克力糖果
					for (MapleCharacter chr : player.getMap().getCharacters()) {
						chr.giveItemBuff(2022258);
					}
				}
				break;
			case 517:// 宠物改名
				MaplePet pet = c.getPlayer().getPet(0);
				if (pet == null) {
					c.getSession().write(MaplePacketCreator.enableActions());
					return;
				}
				slea.skip(8);
				String newName = slea.readMapleAsciiString();
				if (newName.length() > 13) {
					return;
				}
				pet.setName(newName);
				c.getSession().write(MaplePacketCreator.updatePet(pet));
				c.getSession().write(MaplePacketCreator.enableActions());
				c.getPlayer()
						.getMap()
						.broadcastMessage(
								c.getPlayer(),
								MaplePacketCreator.changePetName(c.getPlayer(),
										newName, 1), true);
				MapleInventoryManipulator.removeById(c,
						MapleInventoryType.CASH, itemId, 1, true, false);
				break;
			case 520:
				c.getPlayer().gainMeso(ii.getMeso(itemId), true, false, true);
				MapleInventoryManipulator.removeById(c,
						MapleInventoryType.CASH, itemId, 1, true, false);
				c.getSession().write(MaplePacketCreator.enableActions());
				break;
			case 524:
				MaplePet pet1 = c.getPlayer().getPet(0);
				if (pet1 == null) {
					c.getSession().write(MaplePacketCreator.enableActions());
					return;
				}
				if (!pet1.canConsume(itemId)) {
					pet = c.getPlayer().getPet(1);
					if (pet != null) {
						if (!pet.canConsume(itemId)) {
							pet = c.getPlayer().getPet(2);
							if (pet != null) {
								if (!pet.canConsume(itemId)) {
									c.getSession().write(
											MaplePacketCreator.enableActions());
									return;
								}
							} else {
								c.getSession().write(
										MaplePacketCreator.enableActions());
								return;
							}
						}
					} else {
						c.getSession()
								.write(MaplePacketCreator.enableActions());
						return;
					}
				}
				pet1.setFullness(100);
				int closeGain = 100 * c.getChannelServer().getPetExpRate();
				if (pet1.getCloseness() < 30000) {
					if (pet1.getCloseness() + closeGain > 30000) {
						pet1.setCloseness(30000);
					} else {
						pet1.setCloseness(pet1.getCloseness() + closeGain);
					}
					while (pet1.getCloseness() >= ExpTable
							.getClosenessNeededForLevel(pet1.getLevel() + 1)) {
						pet1.setLevel(pet1.getLevel() + 1);
						c.getSession().write(
								MaplePacketCreator.showOwnPetLevelUp(c
										.getPlayer().getPetSlot(pet1)));
						c.getPlayer()
								.getMap()
								.broadcastMessage(
										MaplePacketCreator.showPetLevelUp(c
												.getPlayer(), c.getPlayer()
												.getPetSlot(pet1)));
					}
				}
				c.getSession().write(MaplePacketCreator.updatePet(pet1));
				c.getPlayer()
						.getMap()
						.broadcastMessage(
								c.getPlayer(),
								MaplePacketCreator.commandResponse(c
										.getPlayer().getId(), 0, 1, true), true);
				MapleInventoryManipulator.removeById(c,
						MapleInventoryType.CASH, itemId, 1, true, false);
				break;
			case 528:
				MapleStatEffect mse = MapleStatEffect.getInstance();
				int x = c.getPlayer().getPosition().x;
				int y = c.getPlayer().getPosition().y;
				mse.setSourceId(itemId);
				mse.setLt(ii.getMistItemLt(itemId));
				mse.setRb(ii.getMistItemRb(itemId));
				Rectangle bounds = mse.calculateBoundingBox(c.getPlayer()
						.getPosition(), c.getPlayer().isFacingLeft());
				MapleMist mist = new MapleMist(bounds, c.getPlayer(), mse);
				c.getPlayer().getMap().spawnMist(mist, 10000, true);
				// c.getPlayer().giveItemBuff(itemId);
				c.getPlayer().getClient().getSession()
						.write(MaplePacketCreator.enableActions());
				break;
			case 530:
				ii.getItemEffect(itemId).applyTo(c.getPlayer());
				MapleInventoryManipulator.removeById(c,
						MapleInventoryType.CASH, itemId, 1, true, false);
				break;
			case 533: // Duey quick delivery ticket
				c.getSession().write(
						MaplePacketCreator.sendDuey((byte) 8,
								DueyActionHandler.loadItems(c.getPlayer())));
				break;
			case 537:
				if (c.getPlayer().isMuted()
						|| c.getPlayer().getMap().getMuted()) {
					c.getPlayer()
							.dropMessage(
									5,
									c.getPlayer().isMuted() ? "You are "
											: "The map is "
													+ "muted, therefore you are unable to talk.");
					return;
				}
				String text = slea.readMapleAsciiString();
				c.getPlayer().setChalkboard(text);
				c.getPlayer()
						.getMap()
						.broadcastMessage(
								MaplePacketCreator.useChalkboard(c.getPlayer(),
										false));
				c.getPlayer().getClient().getSession()
						.write(MaplePacketCreator.enableActions());
				break;
			case 539:
				if (c.getPlayer().isMuted()
						|| c.getPlayer().getMap().getMuted()) {
					return;
				}
				List<String> lines = new LinkedList<String>();
				lines.add(slea.readMapleAsciiString());
				c.getChannelServer()
						.getWorldInterface()
						.broadcastMessage(
								null,
								MaplePacketCreator.getAvatarMega(c.getPlayer(),
										c.getChannel(), itemId, lines,
										(slea.readByte() != 0)).getBytes());
				MapleInventoryManipulator.removeById(c,
						MapleInventoryType.CASH, itemId, 1, true, false);
				break;
			case 552: // 剪刀
				MapleInventoryType type = MapleInventoryType
						.getByType((byte) slea.readInt());
				byte sloti = (byte) slea.readInt();
				IEquip item = (IEquip) c.getPlayer().getInventory(type)
						.getItem(sloti);
				if (item == null || item.getQuantity() <= 0
						|| item.HasFlag(InventoryConstants.Items.Flags.KARMA)
						&& ii.isKarmaAble(item.getItemId())) {
					c.getSession().write(MaplePacketCreator.enableActions());
					return;
				}
				item.AddFlag(InventoryConstants.Items.Flags.KARMA);
				c.getPlayer().forceUpdateItem(type, item);
				MapleInventoryManipulator.removeById(c,
						MapleInventoryType.CASH, itemId, 1, true, false);
				c.getSession().write(MaplePacketCreator.enableActions());
				break;
			case 557: // 金锤子
				slea.readInt();
				int itemSlot = slea.readInt();
				slea.readInt();
				final IEquip equip = (IEquip) c.getPlayer()
						.getInventory(MapleInventoryType.EQUIP)
						.getItem((byte) itemSlot);
				if (equip.getVicious() == 2
						|| c.getPlayer().getInventory(MapleInventoryType.CASH)
								.findById(5570000) == null) {
					return;
				}
				equip.setVicious(equip.getVicious() + 1);
				equip.setUpgradeSlots(equip.getUpgradeSlots() + 1);
				MapleInventoryManipulator.removeById(c,
						MapleInventoryType.CASH, itemId, 1, true, false);
				break;
			default:
				log.info("没有找到处理使用现金道具的函数，现金道具类型 = " + itemType);
				c.getPlayer().dropMessage(1, "目前无法使用的道具！");
				c.getSession().write(MaplePacketCreator.enableActions());
			}

		} catch (RemoteException e) {
			ServerExceptionHandler.HandlerRemoteException(e);
			c.getChannelServer().reconnectWorld();
			log.error("REMOTE ERROR", e);
		}
	}

	private static int rand(int lbound, int ubound) {
		return (int) ((Math.random() * (ubound - lbound + 1)) + lbound);
	}
}
