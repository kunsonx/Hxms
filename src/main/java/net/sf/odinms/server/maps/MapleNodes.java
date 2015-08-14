/*
 This file is part of the OdinMS Maple Story Server
 Copyright (C) 2008 ~ 2010 Patrick Huy <patrick.huy@frz.cc> 
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
import java.util.*;

public class MapleNodes {

	private Map<Integer, MapleNodeInfo> nodes; // used for HOB pq.
	private List<MyRectangle> areas;
	private List<MaplePlatform> platforms;
	private List<MonsterPoint> monsterPoints;
	private List<Integer> skillIds;
	private List<MobsToSpawn> mobsToSpawn;
	private List<GuardiansToSpawn> guardiansToSpawn;
	private List<MapleFlags> flags;
	private List<DirectionInfo> directionInfo;
	private int nodeStart = -1, mapid;
	private boolean firstHighest = true;

	private MapleNodes() {
		this(0);
	}

	public MapleNodes(final int mapid) {
		nodes = new LinkedHashMap<Integer, MapleNodeInfo>();
		areas = new ArrayList<MyRectangle>();
		platforms = new ArrayList<MaplePlatform>();
		skillIds = new ArrayList<Integer>();
		directionInfo = new ArrayList<DirectionInfo>();
		monsterPoints = new ArrayList<MonsterPoint>();
		mobsToSpawn = new ArrayList<MobsToSpawn>();
		guardiansToSpawn = new ArrayList<GuardiansToSpawn>();
		flags = new ArrayList<MapleFlags>();
		this.mapid = mapid;
	}

	public void setNodes(Map<Integer, MapleNodeInfo> nodes) {
		this.nodes = nodes;
	}

	public List<GuardiansToSpawn> getGuardiansToSpawn() {
		return guardiansToSpawn;
	}

	public void setGuardiansToSpawn(List<GuardiansToSpawn> guardiansToSpawn) {
		this.guardiansToSpawn = guardiansToSpawn;
	}

	public List<DirectionInfo> getDirectionInfo() {
		return directionInfo;
	}

	public void setDirectionInfo(List<DirectionInfo> directionInfo) {
		this.directionInfo = directionInfo;
	}

	public int getNodeStart() {
		return nodeStart;
	}

	public void setPlatforms(List<MaplePlatform> platforms) {
		this.platforms = platforms;
	}

	public void setMonsterPoints(List<MonsterPoint> monsterPoints) {
		this.monsterPoints = monsterPoints;
	}

	public void setSkillIds(List<Integer> skillIds) {
		this.skillIds = skillIds;
	}

	public void setMobsToSpawn(List<MobsToSpawn> mobsToSpawn) {
		this.mobsToSpawn = mobsToSpawn;
	}

	public void setFlags(List<MapleFlags> flags) {
		this.flags = flags;
	}

	public int getMapid() {
		return mapid;
	}

	public void setMapid(int mapid) {
		this.mapid = mapid;
	}

	public boolean isFirstHighest() {
		return firstHighest;
	}

	public void setFirstHighest(boolean firstHighest) {
		this.firstHighest = firstHighest;
	}

	public void setNodeStart(final int ns) {
		this.nodeStart = ns;
	}

	public void addDirection(int key, DirectionInfo d) {
		this.directionInfo.add(key, d);
	}

	public DirectionInfo getDirection(int key) {
		if (key >= directionInfo.size()) {
			return null;
		}
		return directionInfo.get(key);
	}

	public List<MapleFlags> getFlags() {
		return flags;
	}

	public void addFlag(MapleFlags f) {
		flags.add(f);
	}

	public void addNode(final MapleNodeInfo mni) {
		this.nodes.put(Integer.valueOf(mni.getKey_()), mni);
	}

	public Map<Integer, MapleNodeInfo> getNodes() {
		return nodes;
	}

	public Collection<MapleNodeInfo> getNodesValue() {
		return new ArrayList<MapleNodeInfo>(nodes.values());
	}

	public MapleNodeInfo getNode(final int index) {
		int i = 1;
		for (MapleNodeInfo x : getNodesValue()) {
			if (i == index) {
				return x;
			}
			i++;
		}
		return null;
	}

	public boolean isLastNode(final int index) {
		return index == nodes.size();
	}

	private int getNextNode(final MapleNodeInfo mni) {
		if (mni == null) {
			return -1;
		}
		addNode(mni);
		// output part
		/*
		 * StringBuilder b = new StringBuilder(mapid + " added key " + mni.key +
		 * ". edges: "); for (int i : mni.edge) { b.append(i + ", "); }
		 * System.out.println(b.toString());
		 * FileoutputUtil.log(FileoutputUtil.PacketEx_Log, b.toString());
		 */
		// output part end

		int ret = -1;
		for (int i : mni.getEdge()) {
			if (!nodes.containsKey(Integer.valueOf(i))) {
				if (ret != -1
						&& (mapid / 100 == 9211204 || mapid / 100 == 9320001)) {
					if (!firstHighest) {
						ret = Math.min(ret, i);
					} else {
						firstHighest = false;
						ret = Math.max(ret, i);
						// two ways for stage 5 to get to end, thats highest
						// ->lowest, and lowest -> highest(doesn't work)
						break;
					}
				} else {
					ret = i;
				}
			}
		}
		mni.setNextNode(ret);
		return ret;
	}

	public void sortNodes() {
		if (nodes.size() <= 0 || nodeStart < 0) {
			return;
		}
		Map<Integer, MapleNodeInfo> unsortedNodes = new HashMap<Integer, MapleNodeInfo>(
				nodes);
		final int nodeSize = unsortedNodes.size();
		nodes.clear();
		int nextNode = getNextNode(unsortedNodes.get(nodeStart));
		while (nodes.size() != nodeSize && nextNode >= 0) {
			nextNode = getNextNode(unsortedNodes.get(nextNode));
		}
	}

	public final void addMapleArea(final MyRectangle rec) {
		areas.add(rec);
	}

	public final List<MyRectangle> getAreas() {
		return areas;
	}

	public void setAreas(List<MyRectangle> rectangles) {
		this.areas = rectangles;
	}

	public final MyRectangle getArea(final int index) {
		return areas.get(index);
	}

	public final void addPlatform(final MaplePlatform mp) {
		this.platforms.add(mp);
	}

	public final List<MaplePlatform> getPlatforms() {
		return platforms;
	}

	public final List<MonsterPoint> getMonsterPoints() {
		return monsterPoints;
	}

	public final void addMonsterPoint(int x, int y, int fh, int cy, int team) {
		this.monsterPoints.add(new MonsterPoint(x, y, fh, cy, team));
	}

	public final void addMobSpawn(int mobId, int spendCP) {
		this.mobsToSpawn.add(new MobsToSpawn(mobId, spendCP));
	}

	public final List<MobsToSpawn> getMobsToSpawn() {
		return mobsToSpawn;
	}

	public final void addGuardianSpawn(Point guardian, int team) {
		this.guardiansToSpawn.add(new GuardiansToSpawn(guardian, team));
	}

	public final List<GuardiansToSpawn> getGuardians() {
		return guardiansToSpawn;
	}

	public final List<Integer> getSkillIds() {
		return skillIds;
	}

	public final void addSkillId(int z) {
		this.skillIds.add(z);
	}

	public void cancelAreas() {
		platforms.clear();
	}

}
