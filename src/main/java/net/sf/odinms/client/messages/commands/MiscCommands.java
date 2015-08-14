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
package net.sf.odinms.client.messages.commands;

import java.awt.Point;
import java.util.Calendar;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;

import net.sf.odinms.client.Equip;
import net.sf.odinms.client.Item;
import net.sf.odinms.client.MapleCharacter;
import net.sf.odinms.client.MapleClient;
import net.sf.odinms.client.MapleInventoryType;
import net.sf.odinms.client.MapleStat;
import net.sf.odinms.client.SkillFactory;
import net.sf.odinms.client.messages.Command;
import net.sf.odinms.client.messages.CommandDefinition;
import net.sf.odinms.client.messages.CommandProcessor;
import net.sf.odinms.client.messages.IllegalCommandSyntaxException;
import net.sf.odinms.client.messages.MessageCallback;
import net.sf.odinms.net.channel.ChannelServer;
import net.sf.odinms.server.MapleItemInformationProvider;
import net.sf.odinms.server.MaplePortal;
import net.sf.odinms.server.MapleStatEffect;
import net.sf.odinms.server.life.MapleLifeFactory;
import net.sf.odinms.server.life.MapleMonster;
import net.sf.odinms.server.maps.MapleMap;
import net.sf.odinms.server.maps.MapleMapObject;
import net.sf.odinms.server.maps.MapleMapObjectType;
import net.sf.odinms.server.quest.MapleQuest;
import net.sf.odinms.tools.MaplePacketCreator;

public class MiscCommands implements Command {

	public void execute(MapleClient c, MessageCallback mc, String[] splitted)
			throws Exception, IllegalCommandSyntaxException {
		ChannelServer cserv = c.getChannelServer();
		if (splitted[0].equals("!spy")) {
			MapleCharacter victim = cserv.getPlayerStorage()
					.getCharacterByName(splitted[1]);
			double var = victim.getJumpMod();
			double var2 = victim.getSpeedMod();
			int str = victim.getStr();
			int dex = victim.getDex();
			int intel = victim.getInt();
			int luk = victim.getLuk();
			long meso = victim.getMeso();
			int maxhp = victim.getCurrentMaxHp();
			int maxmp = victim.getCurrentMaxMp();
			int gmlev = victim.getGm();
			mc.dropMessage("JumpMod is " + var + " and Speedmod is " + var2
					+ ".");
			mc.dropMessage("Players stats are: Str: " + str + ", Dex: " + dex
					+ ", Int: " + intel + ", Luk: " + luk + ".");
			mc.dropMessage("Player has " + meso + " mesos.");
			mc.dropMessage("Max HP is " + maxhp + ", and max MP is " + maxmp
					+ ".");
			mc.dropMessage("GM Level is " + gmlev + ".");
		} else if (splitted[0].equals("!giftnx")) {
			if (splitted.length < 4) {
				mc.dropMessage("Use !giftnx <player> <amount> <type> - with type being 'paypal', 'card' or 'maplepoint'.");
			} else {
				int type = 0; // invalid if it doesn't change
				String type1 = "";
				if (splitted[3].equals("paypal")) {
					type = 1;
					type1 = "PaypalNX";
				} else if (splitted[3].equals("card")) {
					type = 4;
					type1 = "CardNX";
				} else if (splitted[3].equals("maplepoint")) {
					type = 2;
					type1 = "MaplePoints";
				} else {
					mc.dropMessage("Use !giftnx <player> <amount> <type> - with type being 'paypal', 'card' or 'maplepoint'.");
				}
				if (type == 1 || type == 2 || type == 4) {
					MapleCharacter victim1 = cserv.getPlayerStorage()
							.getCharacterByName(splitted[1]);
					int points = Integer.parseInt(splitted[2]);
					victim1.modifyCSPoints(type, points);
					mc.dropMessage(type1 + " has been gifted.");
				}
			}
		} else if (splitted[0].equals("!fame")) {
			MapleCharacter player = c.getPlayer();
			MapleCharacter victim = cserv.getPlayerStorage()
					.getCharacterByName(splitted[1]);
			int fame = Integer.parseInt(splitted[2]);
			victim.addFame(fame);
			player.updateSingleStat(MapleStat.FAME, fame);
		} else if (splitted[0].equals("!heal")) {
			if (splitted.length == 1) {
				MapleCharacter player = c.getPlayer();
				player.setHp(player.getMaxhp());
				player.updateSingleStat(MapleStat.HP, player.getMaxhp());
				player.setMp(player.getMaxmp());
				player.updateSingleStat(MapleStat.MP, player.getMaxmp());
			} else if (splitted.length == 2) {
				MapleCharacter player = c.getChannelServer().getPlayerStorage()
						.getCharacterByName(splitted[1]);
				if (player == null) {
					mc.dropMessage("That player is either offline or doesn't exist");
					return;
				}
				player.setHp(player.getMaxhp());
				player.updateSingleStat(MapleStat.HP, player.getMaxhp());
				player.setMp(player.getMaxmp());
				player.updateSingleStat(MapleStat.MP, player.getMaxmp());
				mc.dropMessage("Healed " + splitted[1]);
			}
		} else if (splitted[0].equals("!kill")) {
			for (String name : splitted) {
				if (!name.equals(splitted[0])) {
					MapleCharacter victim = cserv.getPlayerStorage()
							.getCharacterByName(name);
					if (victim != null) {
						victim.setHp(0);
						victim.setMp(0);
						victim.updateSingleStat(MapleStat.HP, 0);
						victim.updateSingleStat(MapleStat.MP, 0);
					}
				}
			}
		} else if (splitted[0].equals("!killmap")) {
			for (MapleCharacter victim : c.getPlayer().getMap().getCharacters()) {
				if (victim != null) {
					victim.setHp(0);
					victim.setMp(0);
					victim.updateSingleStat(MapleStat.HP, 0);
					victim.updateSingleStat(MapleStat.MP, 0);
				}
			}
		} else if (splitted[0].equals("!dcall")) {
			Collection<ChannelServer> csss = c.getChannelServers();
			for (ChannelServer cservers : csss) {
				Collection<MapleCharacter> cmc = new LinkedHashSet<MapleCharacter>(
						cservers.getPlayerStorage().getAllCharacters()); // Fix
																			// ConcurrentModificationException.
				for (MapleCharacter mch : cmc) {
					if (!mch.isGM() && mch != null) {
						try {
							mch.getClient().getSession().close(false);
							mch.getClient().disconnect();
						} catch (Exception e) {
						}
					}
				}
			}
		} else if (splitted[0].equals("!healmap")) {
			Collection<MapleCharacter> cmc = new LinkedHashSet<MapleCharacter>(
					c.getPlayer().getMap().getCharacters());
			for (MapleCharacter mch : cmc) {
				if (mch != null) {
					mch.setHp(mch.getMaxhp());
					mch.setMp(mch.getMaxmp());
					mch.updateSingleStat(MapleStat.HP, mch.getMaxhp());
					mch.updateSingleStat(MapleStat.MP, mch.getMaxmp());
				}
			}
		} else if (splitted[0].equals("!unstick")) {
			MapleCharacter victim = cserv.getPlayerStorage()
					.getCharacterByName(splitted[1]);
			victim.saveToDB(true); // Just in case.
			victim.unstick();
			mc.dropMessage(victim + " has been unstuck.");
		} else if (splitted[0].equals("!eventmap")) {
			c.getPlayer().getMap().setEvent(!c.getPlayer().getMap().hasEvent());
			mc.dropMessage(c.getPlayer().getMap().hasEvent() ? "Map set to event mode."
					: "Map set to regular mode.");
		} else if (splitted[0].equals("!clock")) {
			if (splitted.length < 2) {
				mc.dropMessage("Please include the time in seconds you'd like on the clock!");
				return;
			}
			c.getPlayer().getMap().addMapTimer(Integer.parseInt(splitted[1]));
			// c.getPlayer().getMap().broadcastMessage(MaplePacketCreator.getClock(Integer.parseInt(splitted[1])));
		} else if (splitted[0].equals("!removequest")) {
			MapleQuest.remove(Integer.parseInt(splitted[1]));
		} else if (splitted[0].equals("!resetmap")
				|| splitted[0].equals("!刷新地图")) {
			MapleCharacter player = c.getPlayer();
			boolean custMap = splitted.length >= 2;
			int mapid = custMap ? Integer.parseInt(splitted[1]) : player
					.getMapId();
			MapleMap map = custMap ? player.getClient().getChannelServer()
					.getMapFactory().getMap(mapid) : player.getMap();
			if (player.getClient().getChannelServer().getMapFactory()
					.destroyMap(mapid)) {
				MapleMap newMap = player.getClient().getChannelServer()
						.getMapFactory().getMap(mapid);
				newMap.setOfflinePlayer(map.getOfflinePlayer());
				MaplePortal newPor = newMap.getPortal(0);
				Collection<MapleCharacter> mcs = new LinkedHashSet<MapleCharacter>(
						map.getCharacters()); // do NOT remove, fixing
												// ConcurrentModificationEx.
				outerLoop: for (MapleCharacter m : mcs) {
					for (int x = 0; x < 5; x++) {
						try {
							m.changeMap(newMap, newPor);
							continue outerLoop;
						} catch (Throwable t) {
						}
					}
					mc.dropMessage("Failed warping " + m.getName()
							+ " to the new map. Skipping...");
				}
				mc.dropMessage("The map has been reset.");
				return;
			}
			mc.dropMessage("Unsuccessful reset!");
		} else if (splitted[0].equalsIgnoreCase("!spawnrate")) {
			MapleMap map = c.getPlayer().getMap();
			if (splitted[1].equalsIgnoreCase("multi")) {
				if (map.isSpawnRateModified()) {
					mc.dropMessage("The spawn rate for this map has already been modified. You may only reset it.");
					return;
				}
				int delta = Integer.parseInt(splitted[2]);
				if (delta < 1) {
					mc.dropMessage("You cannot multiply the spawnrate by anything less than one (use divide to decrease spawn)");
					return;
				}
				if (delta > 5) {
					mc.dropMessage("You cannot multiply the spawnrate by anything more than 5. That would cause the spawn to be too much.");
					return;
				}
				map.setSpawnRateMulti(delta);
			} else if (splitted[1].equalsIgnoreCase("divide")) {
				if (map.isSpawnRateModified()) {
					mc.dropMessage("The spawn rate for this map has already been modified. You may only reset it.");
					return;
				}
				int delta = Integer.parseInt(splitted[2]);
				if (delta < 1) {
					mc.dropMessage("You cannot divide the spawnrate by anything less than one (use multi to increase spawn)");
					return;
				}
				map.setSpawnRateMulti(-delta);
			} else if (splitted[1].equalsIgnoreCase("reset")) {
				map.resetSpawnRate();
			} else if (splitted[1].equalsIgnoreCase("resetspawn")) {
				map.resetSpawn();
			} else {
				mc.dropMessage("Syntax: !spawnrate [multi/divide/reset/resetspawn] [delta]");
				mc.dropMessage("Multi speeds up the spawn up to 5 times.");
				mc.dropMessage("Divide slows down the spawn down with no limit.");
				mc.dropMessage("Reset resets the spawnrate as well as the spawn thread. Does not require delta.");
				mc.dropMessage("Resetspawn resets the spawn thread. DOES NOT RESET SPAWNRATE.");
			}
		} else if (splitted[0].equalsIgnoreCase("!mute")) {
			if (splitted.length >= 3) {
				final MapleCharacter victim = cserv.getPlayerStorage()
						.getCharacterByName(splitted[1]);
				int time = Integer.parseInt(splitted[2]);
				Calendar unmuteTime = Calendar.getInstance();
				unmuteTime.add(Calendar.MINUTE, time);
				victim.setMuted(true);
				victim.setUnmuteTime(unmuteTime);
				mc.dropMessage(victim.getName() + "has been muted for " + time
						+ " minutes.");
				victim.dropMessage("You have been muted for " + time
						+ " minutes");

			} else {
				mc.dropMessage("!mute <player name> <minutes>");
			}
		} else if (splitted[0].equalsIgnoreCase("!unmute")) {
			if (splitted.length >= 2) {
				cserv.getPlayerStorage().getCharacterByName(splitted[1])
						.setMuted(false);
			} else {
				mc.dropMessage("Please enter the character name that you want to unmute.");
			}
		} else if (splitted[0].equalsIgnoreCase("!mutemap")) {
			MapleMap map = c.getPlayer().getMap();
			map.setMuted(!map.getMuted());
			map.broadcastMessage(MaplePacketCreator.serverNotice(5,
					map.getMapName() + " has been "
							+ (map.getMuted() ? "muted." : "unmuted.")));
		} else if (splitted[0].equalsIgnoreCase("!getbuffs")) {
			if (splitted.length < 2) {
				return;
			}
			String name = splitted[1];
			MapleCharacter chr = cserv.getPlayerStorage().getCharacterByName(
					name);
			List<MapleStatEffect> lmse = chr.getBuffEffects();
			mc.dropMessage(name + "'s buffs:");

			for (MapleStatEffect mse : lmse) {
				StringBuilder sb = new StringBuilder();
				sb.append(mse.isSkill() ? "SKILL: " : "ITEM: ");
				if (mse.isSkill()) {
					sb.append(" ");
					sb.append(mse.getRemark());
					sb.append(" ");
				}

				sb.append(mse.isSkill() ? SkillFactory.getSkillName(mse
						.getSourceId()) : MapleItemInformationProvider
						.getInstance().getName(mse.getSourceId()));
				sb.append(" (");
				sb.append(mse.getSourceId());
				sb.append(") ");
				sb.append(mse.getBuffString());
				// SKILL: Level 1 Bless (910xxxx)
				mc.dropMessage(sb.toString());
			}
			mc.dropMessage(name + "'s buffs END.");
		} else if (splitted[0].equalsIgnoreCase("!toggleblock")) {
			if (splitted.length < 2) {
				mc.dropMessage("Syntax: !toggleblock exit/enter");
				return;
			}
			String type = splitted[1];
			if (type.equalsIgnoreCase("exit")) {
				c.getPlayer().getMap()
						.setCanExit(!c.getPlayer().getMap().canExit());
				mc.dropMessage("Non-GMs may "
						+ (c.getPlayer().getMap().canExit() ? "" : "not ")
						+ "exit this map.");
			} else if (type.equalsIgnoreCase("enter")) {
				c.getPlayer().getMap()
						.setCanEnter(!c.getPlayer().getMap().canEnter());
				mc.dropMessage("Non-GMs may "
						+ (c.getPlayer().getMap().canEnter() ? "" : "not ")
						+ "enter this map.");
			}
		} else if (splitted[0].equalsIgnoreCase("!damage")) {
			if (splitted.length < 2) {
				mc.dropMessage("Syntax: !damage enable/disable");
				return;
			}
			boolean op = splitted[1].equalsIgnoreCase("disable");
			Collection<MapleMapObject> cmmo = c.getPlayer().getMap()
					.getMapObjects();
			for (MapleMapObject mmo : cmmo) {
				if (mmo.getType() == MapleMapObjectType.MONSTER) {
					MapleMonster mm = (MapleMonster) mmo;
					mm.setHpLock(op);
				}
			}
			mc.dropMessage("All mobs are now " + (op ? "" : "not ")
					+ "HP locked.");
		} else if (splitted[0].equalsIgnoreCase("!unfreezemap")) {

			Collection<MapleMapObject> cmmo = c.getPlayer().getMap()
					.getMapObjects();
			for (MapleMapObject mmo : cmmo) {
				if (mmo.getType() == MapleMapObjectType.MONSTER) {
					MapleMonster mm = (MapleMonster) mmo;
					if (mm.isFake() && mm.isMoveLocked()) {
						mm.setMoveLocked(false);
					}
				}
			}
			mc.dropMessage("All mobs are now " + (false ? "" : "not ")
					+ "Move locked.");
		} else if (splitted[0].equalsIgnoreCase("!split")) {
			Collection<MapleCharacter> lmc = new LinkedHashSet<MapleCharacter>(
					c.getPlayer().getMap().getCharacters());
			MapleMap wto = c.getChannelServer().getMapFactory()
					.getMap(Integer.parseInt(splitted[1]));
			if (wto == null) {
				return;
			}
			MaplePortal wtof = wto.getPortal(Integer.parseInt(splitted[2]));
			MaplePortal wtot = wto.getPortal(Integer.parseInt(splitted[3]));
			if (wtof == null || wtot == null) {
				return;
			}
			boolean it = false;
			for (MapleCharacter cmc : lmc) {
				if (it) {
					cmc.changeMap(wto, wtot);
				} else {
					cmc.changeMap(wto, wtof);
				}
				it = !it;
			}

		} else if (splitted[0].equalsIgnoreCase("!dropwave")) {
			int itemid = Integer.parseInt(splitted[1]);
			boolean mesos = itemid < 0;
			if (mesos) {
				itemid = -itemid;
				if (itemid < 1) {
					itemid = 1;
				}
			}
			int quant = CommandProcessor.getNamedIntArg(splitted, 2,
					"quantity", 1);
			int margin = CommandProcessor.getNamedIntArg(splitted, 2, "dist",
					20);
			if (margin < 20) {
				margin = 20;
			}
			MapleMap map = c.getPlayer().getMap();
			Point p = c.getPlayer().getPosition();
			margin = c.getPlayer().isFacingLeft() ? -margin : margin;
			if (mesos) {
				for (int x = 0; x < quant && x < 30; x++) {
					map.spawnMesoDrop(itemid, p, c.getPlayer(), c.getPlayer(),
							true);
					p.translate(margin, 0);

				}
			} else {
				MapleItemInformationProvider ii = MapleItemInformationProvider
						.getInstance();
				Item toDrop = null;
				boolean eq = ii.getInventoryType(itemid) == MapleInventoryType.EQUIP;

				if (!eq) {

					toDrop = new Item(itemid, (byte) 0, (short) 1);
				}
				for (int x = 0; x < quant && x < 30; x++) {

					if (eq) {
						toDrop = ii.randomizeStats((Equip) ii
								.getEquipById(itemid));
					}
					if (toDrop == null) {
						break;
					}
					map.spawnItemDrop(c.getPlayer(), c.getPlayer(), toDrop, p,
							true, true);
					p.translate(margin, 0);
				}
			}
		} else if (splitted[0].equalsIgnoreCase("!killid")) {
			int mid = Integer.parseInt(splitted[1]);
			c.getPlayer().getMap().killMonster(mid, c.getPlayer());
		} else if (splitted[0].equalsIgnoreCase("!snailrush")) {
			Point maple = new Point(40, -88);
			Point story = new Point(40, 152);
			MapleMonster mob1 = MapleLifeFactory.getMonster(100100);
			mob1.setHpLock(true);
			MapleMonster mob2 = MapleLifeFactory.getMonster(100100);
			mob2.setHpLock(true);
			MapleMap m = c.getPlayer().getMap();
			m.spawnMonsterOnGroundBelow(mob1, maple);
			m.spawnMonsterOnGroundBelow(mob2, story);
			m.killMonster(9300091, c.getPlayer());
		}
	}

	@Override
	public CommandDefinition[] getDefinition() {
		return new CommandDefinition[] {
				new CommandDefinition("spy", "<player>", "Spies on the player",
						50),
				new CommandDefinition("giftnx", "<player> <amount> <type>",
						"Gifts the specified NX to the player", 50),
				new CommandDefinition("fame", "<player> <fame>",
						"Sets the player's fame at the specified amount", 50),
				new CommandDefinition("heal", "[player]",
						"Heals you if player is not specified", 50),
				new CommandDefinition("kill", "<players>",
						"Kills the players specified", 3),
				new CommandDefinition("dcall", "", "DCs everyone.", 50),
				new CommandDefinition("healmap", "", "Heals the map", 50),
				new CommandDefinition("unstick", "<player>",
						"Unsticks the specified player", 50),
				new CommandDefinition("vac", "", "Vacs monsters to you.", 50),
				new CommandDefinition("eventmap", "",
						"Toggles event map status on the current map", 50),
				new CommandDefinition("clock", "[time]",
						"Shows a clock to everyone in the map", 3),
				new CommandDefinition("removequest", "Quest ID",
						"Removes a quest from cache", 50),
				new CommandDefinition(
						"resetmap",
						"[mapid]",
						"Resets the specified mapid, or if not specified, the map you are on. Used to reset maps that crash when entered.",
						50),
				new CommandDefinition(
						"刷新地图",
						"[mapid]",
						"Resets the specified mapid, or if not specified, the map you are on. Used to reset maps that crash when entered.",
						50),
				new CommandDefinition("spawnrate", "See !spawnrate help",
						"Spawnrate control.", 50),
				new CommandDefinition("mute", "[player name] [minutes muted]",
						"Mutes player for the amount of minutes.", 50),
				new CommandDefinition("unmute", "[player name]",
						"Unmutes player", 50),
				new CommandDefinition("mutemap", "", "Mutes the map", 50),
				new CommandDefinition("killmap", "", "kills the map", 50),
				new CommandDefinition("getbuffs", "[player]",
						"Gets all the buffs of the specified player", 50),
				new CommandDefinition("toggleblock", "exit/enter",
						"Sets whether non-GMs may exit/enter this map.", 50),
				new CommandDefinition("damage", "enable/disable",
						"Sets whether damage to monsters is enabled.", 50),
				new CommandDefinition("unfreezemap", "",
						"Unfreezes movelocked monsters", 50),
				new CommandDefinition(
						"split",
						"<mapid> <portal no 1> <portal no 2>",
						"Warps one half of the map to portal no 1 in mapid and the other to portal no 2 in mapid.",
						50),
				new CommandDefinition(
						"dropwave",
						"<itemid (negative value for mesos)> [quantity <quantity> (default 1)] [dist <distance between each item in pixels> (default 20)]",
						"Spawns a wave of items.", 50),
				new CommandDefinition("killid", "", "", 50),
				new CommandDefinition("snailrush", "", "", 50),
				new CommandDefinition("playershop",
						"<rename/close> <owner's name> [rename to]", "", 50) };
	}
}