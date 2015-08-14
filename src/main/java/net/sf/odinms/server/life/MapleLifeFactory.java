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
package net.sf.odinms.server.life;

import net.sf.odinms.database.DatabaseConnection;
import org.apache.log4j.Logger;
import org.hibernate.Session;

public class MapleLifeFactory {

	private static final Logger log = Logger.getLogger(MapleLifeFactory.class);

	public static AbstractLoadedMapleLife getLife(int id, String type) {
		if (type.equalsIgnoreCase("n")) {
			return getNPC(id);
		} else if (type.equalsIgnoreCase("m")) {
			return getMonster(id);
		} else {
			log.warn("Unknown Life type: " + type);
			return null;
		}
	}

	public static void decodeElementalString(MapleMonsterStats stats,
			String elemAttr) {
		for (int i = 0; i < elemAttr.length(); i += 2) {
			stats.setEffectiveness(Element.getFromChar(elemAttr.charAt(i)),
					ElementalEffectiveness.getByNumber(Integer.valueOf(String
							.valueOf(elemAttr.charAt(i + 1)))));
		}
	}

	public static MapleNPC getNPC(int id) {
		MapleNPC npc = null;
		Session session = DatabaseConnection.getSession();
		try {
			npc = new MapleNPC(id, (MapleNPCStats) session.get(
					MapleNPCStats.class, id));
		} catch (Exception e) {
			log.error("load life", e);
		} finally {
			session.close();
		}
		return npc;
	}

	public static MapleMonster getMonster(int id) {
		MapleMonster m = null;
		Session session = DatabaseConnection.getSession();
		try {
			MapleMonsterStats stats = (MapleMonsterStats) session.get(
					MapleMonsterStats.class, id);
			if (stats != null) {
				m = new MapleMonster(id, stats);
			} else {
				log.info("尝试载入不存在的怪物：" + id);
			}
		} catch (Exception e) {
			log.error("load life", e);
		} finally {
			session.close();
		}
		return m;
	}

	public static class BanishInfo {

		private int map;
		private String portal, msg;

		public BanishInfo() {
		}

		public BanishInfo(String msg, int map, String portal) {
			this.msg = msg;
			this.map = map;
			this.portal = portal;
		}

		public int getMap() {
			return map;
		}

		public String getPortal() {
			return portal;
		}

		public String getMsg() {
			return msg;
		}

		public void setMap(int map) {
			this.map = map;
		}

		public void setPortal(String portal) {
			this.portal = portal;
		}

		public void setMsg(String msg) {
			this.msg = msg;
		}
	}
}