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
package net.sf.odinms.server.maps;

import java.awt.Point;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import net.sf.odinms.client.GameConstants;
import net.sf.odinms.database.DatabaseConnection;
import net.sf.odinms.net.channel.ChannelDescriptor;
import net.sf.odinms.net.channel.ChannelServer;
import net.sf.odinms.provider.MapleData;
import net.sf.odinms.provider.MapleDataProvider;
import net.sf.odinms.provider.MapleDataTool;
import net.sf.odinms.server.MaplePortal;
import net.sf.odinms.server.life.AbstractLoadedMapleLife;
import net.sf.odinms.server.life.MapleLifeFactory;
import net.sf.odinms.tools.StringUtil;
import org.hibernate.Session;

public class MapleMapFactory {

	private static org.apache.log4j.Logger log = org.apache.log4j.Logger
			.getLogger(MapleMapFactory.class);
	private static final Map<Integer, MapleMapInfo> mapinfos = new ConcurrentHashMap<Integer, MapleMapInfo>();
	private static Lock lock = new ReentrantLock(true);
	/**
	 * 成员变量
	 */
	private MapleDataProvider source;
	private MapleData nameData;
	private Map<Integer, MapleMap> maps = new ConcurrentHashMap<Integer, MapleMap>();
	private ChannelDescriptor channel;

	public static void InitCache() {
		PreparedStatement ps;
		try {
			Connection c = DatabaseConnection.getConnection();
			ps = c.prepareStatement("SELECT DISTINCT characters.map FROM characters");
			ResultSet rs = ps.executeQuery();
			Session session = DatabaseConnection.getSession();
			while (rs.next()) {
				int iid = rs.getInt("map");
				MapleMapInfo info = (MapleMapInfo) session.get(
						MapleMapInfo.class, iid);
				if (info != null) {
					mapinfos.put(iid, info);
				}
			}
			session.close();
			rs.close();
			ps.close();
			c.close();
		} catch (SQLException ex) {
			log.debug("初始化地图缓存失败：" + ex.getMessage());
		}

		log.info("地图缓存已完成...");
	}

	public MapleMapFactory() {
	}

	public MapleMapFactory(MapleDataProvider source,
			MapleDataProvider stringSource) {
		this.source = source;
		this.nameData = stringSource.getData("Map.img");
	}

	public boolean destroyMap(int mapid) {
		lock.lock();
		try {
			if (maps.containsKey(Integer.valueOf(mapid))) {
				return maps.remove(Integer.valueOf(mapid)) != null;
			}
		} finally {
			lock.unlock();
		}
		return false;
	}

	public MapleMap getMap(Integer mapid) {
		MapleMap map = null;
		try {
			lock.lock();
			map = maps.get(mapid);
			if (map == null) {
				MapleMapInfo info = mapinfos.get(mapid);
				if (info == null) {
					Session session = DatabaseConnection.getSession();
					info = (MapleMapInfo) session
							.get(MapleMapInfo.class, mapid);
					session.close();
					if (info != null) {
						mapinfos.put(mapid, info);
					}
				}
				if (info != null) {
					map = info.getMap(channel);
					maps.put(mapid, map);
				}
			}
		} finally {
			lock.unlock();
		}
		return map;
	}

	public MapleMapInfo getMapInfo(int mapid) {
		Integer omapid = Integer.valueOf(mapid);
		MapleMapInfo map = null;
		// MapleMap map = maps.get(omapid);
		// check if someone else who was also synchronized has loaded the map
		// already
		String mapName = getMapName(mapid);
		MapleData mapData = source.getData(mapName);
		String link = MapleDataTool.getString(
				mapData.getChildByPath("info/link"), "");
		if (link.equals("")) {
			link = MapleDataTool.getString(mapData.getChildByPath("link"), "");
		}
		if (!link.equals("")) {
			mapName = getMapName(Integer.parseInt(link));
			mapData = source.getData(mapName);
		}

		link = MapleDataTool.getString(mapData.getChildByPath("info/link"), "");
		if (link.equals("")) {
			link = MapleDataTool.getString(mapData.getChildByPath("link"), "");
		}
		if (!link.equals("")) {
			mapName = getMapName(Integer.parseInt(link));
			mapData = source.getData(mapName);
		}

		link = MapleDataTool.getString(mapData.getChildByPath("info/link"), "");
		if (link.equals("")) {
			link = MapleDataTool.getString(mapData.getChildByPath("link"), "");
		}
		if (!link.equals("")) {
			mapName = getMapName(Integer.parseInt(link));
			mapData = source.getData(mapName);
		}

		float monsterRate = 0;
		MapleData mobRate = mapData.getChildByPath("info/mobRate");
		if (mobRate != null) {
			monsterRate = ((Float) mobRate.getData()).floatValue();
		}
		map = new MapleMapInfo(mapid, MapleDataTool.getInt("info/returnMap",
				mapData), monsterRate);
		map.setFirstUserEnter(MapleDataTool.getString(
				mapData.getChildByPath("info/onFirstUserEnter"),
				String.valueOf(mapid)));
		map.setUserEnter(MapleDataTool.getString(
				mapData.getChildByPath("info/onUserEnter"),
				String.valueOf(mapid)));
		map.setTimeMobId(MapleDataTool.getInt(
				mapData.getChildByPath("info/timeMob/id"), -1));
		map.setTimeMobMessage(MapleDataTool.getString(
				mapData.getChildByPath("info/timeMob/message"), ""));
		loadPortals(map, mapData.getChildByPath("portal"));
		List<MapleFoothold> allFootholds = new LinkedList<MapleFoothold>();
		Point lBound = new Point();
		Point uBound = new Point();
		MapleFoothold fh;
		for (MapleData footRoot : mapData.getChildByPath("foothold")) {
			for (MapleData footCat : footRoot) {
				for (MapleData footHold : footCat) {
					int x1 = MapleDataTool
							.getInt(footHold.getChildByPath("x1"));
					int y1 = MapleDataTool
							.getInt(footHold.getChildByPath("y1"));
					int x2 = MapleDataTool
							.getInt(footHold.getChildByPath("x2"));
					int y2 = MapleDataTool
							.getInt(footHold.getChildByPath("y2"));
					fh = new MapleFoothold(new Point(x1, y1),
							new Point(x2, y2), Integer.parseInt(footHold
									.getName()));
					fh.setPrev(MapleDataTool.getInt(footHold
							.getChildByPath("prev")));
					fh.setNext(MapleDataTool.getInt(footHold
							.getChildByPath("next")));

					if (fh.getX1() < lBound.x) {
						lBound.x = fh.getX1();
					}
					if (fh.getX2() > uBound.x) {
						uBound.x = fh.getX2();
					}
					if (fh.getY1() < lBound.y) {
						lBound.y = fh.getY1();
					}
					if (fh.getY2() > uBound.y) {
						uBound.y = fh.getY2();
					}
					allFootholds.add(fh);
				}
			}
		}
		map.setAllFootholds(allFootholds);
		// MapleFootholdTree fTree = new MapleFootholdTree(lBound, uBound);
		// for (MapleFoothold fh_ : allFootholds) {
		// fTree.insert(fh_);
		// }
		map.setlBound(lBound);
		map.setuBound(uBound);

		/*
		 * if (map.getTop() == 0) { map.setTop((short) lBound.y); } if
		 * (map.getBottom() == 0) { map.setBottom((short) uBound.y); } if
		 * (map.getLeft() == 0) { map.setLeft((short) lBound.x); } if
		 * (map.getRight() == 0) { map.setRight((short) uBound.x); }
		 */

		int bossid = -1;
		String msg = null;
		if (mapData.getChildByPath("info/timeMob") != null) {
			bossid = MapleDataTool.getInt(
					mapData.getChildByPath("info/timeMob/id"), 0);
			msg = MapleDataTool.getString(
					mapData.getChildByPath("info/timeMob/message"), null);
		}

		List<Point> herbRocks = new ArrayList<Point>();
		int lowestLevel = 200, highestLevel = 0;
		String type;
		MapleMapCreateLifeInfo myLife;

		// load life data (npc, monsters)

		for (MapleData life : mapData.getChildByPath("life")) {
			MapleData tData = life.getChildByPath("type");
			if (tData == null) {
				return null;
			}
			type = MapleDataTool.getString(tData);
			myLife = loadLife_(life,
					MapleDataTool.getString(life.getChildByPath("id")), type);
			if (type.equalsIgnoreCase("m")) {
				int mobTime = MapleDataTool.getInt("mobTime", life, 0);
				myLife.setMobtime(mobTime);
			}
			myLife.setMapid(mapid);
			map.getLifeInfos().add(myLife);
			/*
			 * if (myLife instanceof MapleMonster) { final MapleMonster mob =
			 * (MapleMonster) myLife; int mobTime =
			 * MapleDataTool.getInt("mobTime", life, 0); if (mobTime == -1) {
			 * //不是不召唤 而是-1的时候只召唤一次 map.spawnMonster(mob); } else {
			 * herbRocks.add(map.addMonsterSpawn(mob, mobTime).getPosition()); }
			 * if (mob.getLevel() > highestLevel && !mob.isBoss()) {
			 * highestLevel = mob.getLevel(); } if (mob.getLevel() < lowestLevel
			 * && !mob.isBoss()) { lowestLevel = mob.getLevel(); } } else if
			 * (myLife instanceof MapleNPC) { map.addMapObject(myLife); }
			 */
		}
		map.setPartyBonusRate(GameConstants.getPartyPlay(mapid, MapleDataTool
				.getInt(mapData.getChildByPath("info/partyBonusR"), 0)));
		map.setNodes(loadNodes(mapid, mapData));
		/*
		 * for (MapleData life : mapData.getChildByPath("life")) { String id =
		 * MapleDataTool.getString(life.getChildByPath("id")); String type =
		 * MapleDataTool.getString(life.getChildByPath("type"));
		 * AbstractLoadedMapleLife myLife = loadLife(life, id, type); if (myLife
		 * instanceof MapleMonster) { MapleMonster monster = (MapleMonster)
		 * myLife; int mobTime = MapleDataTool.getInt("mobTime", life, 0); if
		 * (mobTime == -1) { //不是不召唤 而是-1的时候只召唤一次 map.spawnMonster(monster); }
		 * else { map.addMonsterSpawn(monster, mobTime); } } else {
		 * map.addMapObject(myLife); } }
		 */

		// load reactor data
		if (mapData.getChildByPath("reactor") != null) {
			for (MapleData reactor : mapData.getChildByPath("reactor")) {
				String id = MapleDataTool.getString(reactor
						.getChildByPath("id"));
				if (id != null) {
					MapleMapCreateReactorInfo newReactor = loadReactor_(
							reactor,
							id,
							(byte) MapleDataTool.getInt(
									reactor.getChildByPath("f"), 0));
					// map.spawnReactor(newReactor);
					map.getReactorInfos().add(newReactor);
				}
			}
		}

		try {
			map.setMapName(MapleDataTool.getString("mapName",
					nameData.getChildByPath(getMapStringName(omapid)), ""));
			map.setStreetName(MapleDataTool.getString("streetName",
					nameData.getChildByPath(getMapStringName(omapid)), ""));
		} catch (Exception e) {
			map.setMapName("");
			map.setStreetName("");
		}

		map.setHasClock(mapData.getChildByPath("clock") != null);
		map.setEverlast(mapData.getChildByPath("everlast") != null);
		map.setIsTown(mapData.getChildByPath("town") != null);
		map.setAllowShops(MapleDataTool.getInt(
				mapData.getChildByPath("info/personalShop"), 0) == 1);
		map.setDecHP(MapleDataTool.getIntConvert("decHP", mapData, 0));
		map.setHPDecProtect(MapleDataTool.getIntConvert("protectItem", mapData,
				0));
		map.setForcedReturnMap(MapleDataTool.getInt(
				mapData.getChildByPath("info/forcedReturn"), 999999999));
		if (mapData.getChildByPath("shipObj") != null) {
			map.setHasBoat(true);
		} else {
			map.setHasBoat(false);
		}
		map.setFl(MapleDataTool.getInt(
				mapData.getChildByPath("info/fieldLimit"), 0));
		map.setTimeLimit(MapleDataTool.getIntConvert("timeLimit",
				mapData.getChildByPath("info"), -1));
		map.setFieldType(MapleDataTool.getIntConvert("info/fieldType", mapData,
				0));

		// 结束

		/*
		 * try { Connection con = DatabaseConnection.getConnection();
		 * PreparedStatement ps =
		 * con.prepareStatement("SELECT * FROM spawns WHERE mid = ?");
		 * ps.setInt(1, omapid); ResultSet rs = ps.executeQuery(); while
		 * (rs.next()) { int id = rs.getInt("idd"); int f = rs.getInt("f");
		 * boolean hide = false; type = rs.getString("type"); int fh_ =
		 * rs.getInt("fh"); int cy = rs.getInt("cy"); int rx0 =
		 * rs.getInt("rx0"); int rx1 = rs.getInt("rx1"); int x = rs.getInt("x");
		 * int y = rs.getInt("y"); int mobTime = rs.getInt("mobtime"); myLife =
		 * loadLife(id, f, hide, fh_, cy, rx0, rx1, x, y, type); if
		 * (type.equals("n")) { map.addMapObject(myLife); } else if
		 * (type.equals("m")) { MapleMonster monster = (MapleMonster) myLife;
		 * map.addMonsterSpawn(monster, mobTime); } } rs.close(); ps.close();
		 * con.close(); } catch (SQLException e) { log.info(e.toString()); }
		 * 
		 * if (herbRocks.size() > 0 && highestLevel >= 30 &&
		 * map.getFirstUserEnter().equals("") && map.getUserEnter().equals(""))
		 * { final List<Integer> allowedSpawn = new ArrayList<Integer>(24);
		 * allowedSpawn.add(100011); allowedSpawn.add(200011); if (highestLevel
		 * >= 100) { for (int i = 0; i < 10; i++) { for (int x = 0; x < 4; x++)
		 * { //to make heartstones rare allowedSpawn.add(100000 + i);
		 * allowedSpawn.add(200000 + i); } } } else { for (int i = (lowestLevel
		 * % 10 > highestLevel % 10 ? 0 : (lowestLevel % 10)); i < (highestLevel
		 * % 10); i++) { for (int x = 0; x < 4; x++) { //to make heartstones
		 * rare allowedSpawn.add(100000 + i); allowedSpawn.add(200000 + i); } }
		 * } final int numSpawn =
		 * Randomizer.getInstance().nextInt(allowedSpawn.size()) / 6; //0-7 for
		 * (int i = 0; i < numSpawn && !herbRocks.isEmpty(); i++) { final int
		 * idd =
		 * allowedSpawn.get(Randomizer.getInstance().nextInt(allowedSpawn.size
		 * ())); final int theSpawn =
		 * Randomizer.getInstance().nextInt(herbRocks.size()); final
		 * MapleReactor myReactor = new
		 * MapleReactor(MapleReactorFactory.getReactor(idd), idd);
		 * myReactor.setPosition(herbRocks.get(theSpawn));
		 * myReactor.setDelay(idd % 100 == 11 ? 60000 : 5000); //in the
		 * reactor's wz map.spawnReactor(myReactor); herbRocks.remove(theSpawn);
		 * } }
		 */
		return map;
	}

	public boolean isMapLoaded(int mapId) {
		return maps.containsKey(mapId);
	}

	private MapleNodes loadNodes(final int mapid, final MapleData mapData) {
		MapleNodes nodeInfo = new MapleNodes(mapid);
		if (mapData.getChildByPath("nodeInfo") != null) {
			for (MapleData node : mapData.getChildByPath("nodeInfo")) {
				try {
					if (node.getName().equals("start")) {
						nodeInfo.setNodeStart(MapleDataTool.getInt(node, 0));
						continue;
					}
					List<Integer> edges = new ArrayList<Integer>();
					if (node.getChildByPath("edge") != null) {
						for (MapleData edge : node.getChildByPath("edge")) {
							edges.add(MapleDataTool.getInt(edge, -1));
						}
					}
					final MapleNodeInfo mni = new MapleNodeInfo(
							Integer.parseInt(node.getName()),
							MapleDataTool.getIntConvert("key", node, 0),
							MapleDataTool.getIntConvert("x", node, 0),
							MapleDataTool.getIntConvert("y", node, 0),
							MapleDataTool.getIntConvert("attr", node, 0), edges);
					nodeInfo.addNode(mni);
				} catch (NumberFormatException e) {
				} // start, end, edgeInfo = we dont need it
			}
			nodeInfo.sortNodes();
		}
		for (int i = 1; i <= 7; i++) {
			if (mapData.getChildByPath(String.valueOf(i)) != null
					&& mapData.getChildByPath(i + "/obj") != null) {
				for (MapleData node : mapData.getChildByPath(i + "/obj")) {
					if (node.getChildByPath("SN_count") != null
							&& node.getChildByPath("speed") != null) {
						int sn_count = MapleDataTool.getIntConvert("SN_count",
								node, 0);
						String name = MapleDataTool.getString("name", node, "");
						int speed = MapleDataTool.getIntConvert("speed", node,
								0);
						if (sn_count <= 0 || speed <= 0 || name.equals("")) {
							continue;
						}
						final List<Integer> SN = new ArrayList<Integer>();
						for (int x = 0; x < sn_count; x++) {
							SN.add(MapleDataTool.getIntConvert("SN" + x, node,
									0));
						}
						final MaplePlatform mni = new MaplePlatform(name,
								MapleDataTool.getIntConvert("start", node, 2),
								speed, MapleDataTool.getIntConvert("x1", node,
										0), MapleDataTool.getIntConvert("y1",
										node, 0), MapleDataTool.getIntConvert(
										"x2", node, 0),
								MapleDataTool.getIntConvert("y2", node, 0),
								MapleDataTool.getIntConvert("r", node, 0), SN);
						nodeInfo.addPlatform(mni);
					} else if (node.getChildByPath("tags") != null) {
						String name = MapleDataTool.getString("tags", node, "");
						nodeInfo.addFlag(new MapleFlags(name, name
								.endsWith("3") ? 1 : 0)); // idk, no indication
															// in wz
					}
				}
			}
		}
		// load areas (EG PQ platforms)
		if (mapData.getChildByPath("area") != null) {
			int x1, y1, x2, y2;
			MyRectangle mapArea;
			for (MapleData area : mapData.getChildByPath("area")) {
				x1 = MapleDataTool.getInt(area.getChildByPath("x1"));
				y1 = MapleDataTool.getInt(area.getChildByPath("y1"));
				x2 = MapleDataTool.getInt(area.getChildByPath("x2"));
				y2 = MapleDataTool.getInt(area.getChildByPath("y2"));
				mapArea = new MyRectangle(x1, y1, (x2 - x1), (y2 - y1));
				nodeInfo.addMapleArea(mapArea);
			}
		}
		if (mapData.getChildByPath("CaptureTheFlag") != null) {
			final MapleData mc = mapData.getChildByPath("CaptureTheFlag");
			for (MapleData area : mc) {
				nodeInfo.addGuardianSpawn(
						new Point(MapleDataTool.getInt(area
								.getChildByPath("FlagPositionX")),
								MapleDataTool.getInt(area
										.getChildByPath("FlagPositionY"))),
						area.getName().startsWith("Red") ? 0 : 1);
			}
		}
		if (mapData.getChildByPath("directionInfo") != null
				&& mapData.getChildByPath("directionInfo/eventQ") != null) {
			final MapleData mc = mapData.getChildByPath("directionInfo");
			for (MapleData area : mc) {
				DirectionInfo di = new DirectionInfo(Integer.parseInt(area
						.getName()), MapleDataTool.getInt("x", area, 0),
						MapleDataTool.getInt("y", area, 0),
						MapleDataTool.getInt("forcedInput", area, 0) > 0);
				for (MapleData event : area.getChildByPath("eventQ")) {
					di.eventQ.add(MapleDataTool.getString(event));
				}
				nodeInfo.addDirection(Integer.parseInt(area.getName()), di);
			}
		}
		if (mapData.getChildByPath("monsterCarnival") != null) {
			final MapleData mc = mapData.getChildByPath("monsterCarnival");
			if (mc.getChildByPath("mobGenPos") != null) {
				for (MapleData area : mc.getChildByPath("mobGenPos")) {
					nodeInfo.addMonsterPoint(
							MapleDataTool.getInt(area.getChildByPath("x")),
							MapleDataTool.getInt(area.getChildByPath("y")),
							MapleDataTool.getInt(area.getChildByPath("fh")),
							MapleDataTool.getInt(area.getChildByPath("cy")),
							MapleDataTool.getInt("team", area, -1));
				}
			}
			if (mc.getChildByPath("mob") != null) {
				for (MapleData area : mc.getChildByPath("mob")) {
					nodeInfo.addMobSpawn(MapleDataTool.getInt(area
							.getChildByPath("id")), MapleDataTool.getInt(area
							.getChildByPath("spendCP")));
				}
			}
			if (mc.getChildByPath("guardianGenPos") != null) {
				for (MapleData area : mc.getChildByPath("guardianGenPos")) {
					nodeInfo.addGuardianSpawn(
							new Point(MapleDataTool.getInt(area
									.getChildByPath("x")), MapleDataTool
									.getInt(area.getChildByPath("y"))),
							MapleDataTool.getInt("team", area, -1));
				}
			}
			if (mc.getChildByPath("skill") != null) {
				for (MapleData area : mc.getChildByPath("skill")) {
					nodeInfo.addSkillId(MapleDataTool.getInt(area));
				}
			}
		}
		return nodeInfo;
	}

	private AbstractLoadedMapleLife loadLife(MapleData life, String id,
			String type) {
		AbstractLoadedMapleLife myLife = MapleLifeFactory.getLife(
				Integer.parseInt(id), type);
		myLife.setCy(MapleDataTool.getInt(life.getChildByPath("cy")));
		MapleData dF = life.getChildByPath("f");
		if (dF != null) {
			myLife.setF(MapleDataTool.getInt(dF));
		}
		myLife.setFh(MapleDataTool.getInt(life.getChildByPath("fh")));
		myLife.setRx0(MapleDataTool.getInt(life.getChildByPath("rx0")));
		myLife.setRx1(MapleDataTool.getInt(life.getChildByPath("rx1")));
		int x = MapleDataTool.getInt(life.getChildByPath("x"));
		int y = MapleDataTool.getInt(life.getChildByPath("y"));
		myLife.setPosition(new Point(x, y));

		int hide = MapleDataTool.getInt("hide", life, 0);
		if (hide == 1) {
			myLife.setHide(true);
		} else if (hide > 1) {
			log.warn("Hide > 1 (" + hide + ")");
		}
		return myLife;
	}

	private MapleMapCreateLifeInfo loadLife_(MapleData life, String id,
			String type) {
		MapleMapCreateLifeInfo myLife = new MapleMapCreateLifeInfo();
		myLife.setType(type);
		myLife.setId(Integer.parseInt(id));
		myLife.setCy(MapleDataTool.getInt(life.getChildByPath("cy")));
		MapleData dF = life.getChildByPath("f");
		if (dF != null) {
			myLife.setF(MapleDataTool.getInt(dF));
		}
		myLife.setFh(MapleDataTool.getInt(life.getChildByPath("fh")));
		myLife.setRx0(MapleDataTool.getInt(life.getChildByPath("rx0")));
		myLife.setRx1(MapleDataTool.getInt(life.getChildByPath("rx1")));
		int x = MapleDataTool.getInt(life.getChildByPath("x"));
		int y = MapleDataTool.getInt(life.getChildByPath("y"));
		myLife.setPosition(new Point(x, y));

		int hide = MapleDataTool.getInt("hide", life, 0);
		if (hide == 1) {
			myLife.setHide(hide);
		} else if (hide > 1) {
			log.warn("Hide > 1 (" + hide + ")");
		} else {
			myLife.setHide(0);
		}
		return myLife;
	}

	private void loadPortals(MapleMap map, MapleData port) {
		if (port == null) {
			return;
		}
		int nextDoorPortal = 0x80;
		for (MapleData portal : port.getChildren()) {
			MaplePortal myPortal = new MaplePortal(MapleDataTool.getInt(portal
					.getChildByPath("pt")), map.getMapid());
			myPortal.setName(MapleDataTool.getString(portal
					.getChildByPath("pn")));
			myPortal.setTarget(MapleDataTool.getString(portal
					.getChildByPath("tn")));
			myPortal.setTargetMapId(MapleDataTool.getInt(portal
					.getChildByPath("tm")));
			myPortal.setPosition(new Point(MapleDataTool.getInt(portal
					.getChildByPath("x")), MapleDataTool.getInt(portal
					.getChildByPath("y"))));
			String script = MapleDataTool.getString("script", portal, null);
			if (script != null && script.equals("")) {
				script = null;
			}
			myPortal.setScriptName(script);

			if (myPortal.getType() == MaplePortal.DOOR_PORTAL) {
				myPortal.setId(nextDoorPortal);
				nextDoorPortal++;
			} else {
				myPortal.setId(Integer.parseInt(portal.getName()));
			}
			map.addPortal(myPortal);
		}
	}

	private void loadPortals(MapleMapInfo map, MapleData port) {
		if (port == null) {
			return;
		}
		int nextDoorPortal = 0x80;
		for (MapleData portal : port.getChildren()) {
			MaplePortal myPortal = new MaplePortal(MapleDataTool.getInt(portal
					.getChildByPath("pt")), map.getMapid());
			myPortal.setName(MapleDataTool.getString(portal
					.getChildByPath("pn")));
			myPortal.setTarget(MapleDataTool.getString(portal
					.getChildByPath("tn")));
			myPortal.setTargetMapId(MapleDataTool.getInt(portal
					.getChildByPath("tm")));
			myPortal.setPosition(new Point(MapleDataTool.getInt(portal
					.getChildByPath("x")), MapleDataTool.getInt(portal
					.getChildByPath("y"))));
			String script = MapleDataTool.getString("script", portal, null);
			if (script != null && script.equals("")) {
				script = null;
			}
			myPortal.setScriptName(script);

			if (myPortal.getType() == MaplePortal.DOOR_PORTAL) {
				myPortal.setId(nextDoorPortal);
				nextDoorPortal++;
			} else {
				myPortal.setId(Integer.parseInt(portal.getName()));
			}
			map.addPortal(myPortal);
		}
	}

	private AbstractLoadedMapleLife loadLife(int id, int f, boolean hide,
			int fh, int cy, int rx0, int rx1, int x, int y, String type) {
		AbstractLoadedMapleLife myLife = MapleLifeFactory.getLife(id, type);
		myLife.setCy(cy);
		myLife.setF(f);
		myLife.setFh(fh);
		myLife.setRx0(rx0);
		myLife.setRx1(rx1);
		myLife.setPosition(new Point(x, y));
		myLife.setHide(hide);
		return myLife;
	}

	private MapleReactor loadReactor(MapleData reactor, String id,
			final byte FacingDirection) {
		MapleReactor myReactor = new MapleReactor(
				MapleReactorFactory.getReactor(Integer.parseInt(id)),
				Integer.parseInt(id));

		int x = MapleDataTool.getInt(reactor.getChildByPath("x"));
		int y = MapleDataTool.getInt(reactor.getChildByPath("y"));
		myReactor.setPosition(new Point(x, y));
		myReactor.setFacingDirection(FacingDirection);
		myReactor.setDelay(MapleDataTool.getInt(reactor
				.getChildByPath("reactorTime")) * 1000);
		myReactor.setState((byte) 0);
		myReactor.setName(MapleDataTool.getString(
				reactor.getChildByPath("name"), ""));

		return myReactor;
	}

	private MapleMapCreateReactorInfo loadReactor_(MapleData reactor,
			String id, final byte FacingDirection) {
		MapleMapCreateReactorInfo myReactor = new MapleMapCreateReactorInfo();
		myReactor.setId(Integer.parseInt(id));
		int x = MapleDataTool.getInt(reactor.getChildByPath("x"));
		int y = MapleDataTool.getInt(reactor.getChildByPath("y"));
		myReactor.setPoint(new Point(x, y));
		myReactor.setFacingDirection(FacingDirection);
		myReactor.setDelay(MapleDataTool.getInt(reactor
				.getChildByPath("reactorTime")) * 1000);
		// myReactor.setState((byte) 0);
		myReactor.setName(MapleDataTool.getString(
				reactor.getChildByPath("name"), ""));

		return myReactor;
	}

	private String getMapName(int mapid) {
		String mapName = StringUtil.getLeftPaddedStr(Integer.toString(mapid),
				'0', 9);
		StringBuilder builder = new StringBuilder("Map/Map");
		int area = mapid / 100000000;
		builder.append(area);
		builder.append("/");
		builder.append(mapName);
		builder.append(".img");

		mapName = builder.toString();
		return mapName;
	}

	private String getMapStringName(int mapid) {
		StringBuilder builder = new StringBuilder();
		if (mapid < 100000000) {
			builder.append("maple");
		} else if (mapid >= 100000000 && mapid < 200000000) {
			builder.append("victoria");
		} else if (mapid >= 200000000 && mapid < 300000000) {
			builder.append("ossyria");
		} else if (mapid >= 540000000 && mapid < 541010110) {
			builder.append("singapore");
		} else if (mapid >= 600000000 && mapid < 620000000) {
			builder.append("MasteriaGL");
		} else if (mapid >= 670000000 && mapid < 682000000) {
			builder.append("weddingGL");
		} else if (mapid >= 682000000 && mapid < 683000000) {
			builder.append("HalloweenGL");
		} else if (mapid >= 800000000 && mapid < 900000000) {
			builder.append("jp");
		} else {
			builder.append("etc");
		}
		builder.append("/" + mapid);
		return builder.toString();
	}

	public void setChannel(ChannelDescriptor channel) {
		this.channel = channel;
	}

	public Map<Integer, MapleMap> getMaps() {
		return maps;
	}
}
