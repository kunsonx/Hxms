/*
 * This file is part of the OdinMS Maple Story Server
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
package net.sf.odinms.scripting.reactor;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import javax.script.Invocable;
import net.sf.odinms.client.MapleClient;
import net.sf.odinms.database.DatabaseConnection;
import net.sf.odinms.scripting.AbstractScriptManager;
import net.sf.odinms.server.life.MapleMonsterInformationProvider.DropEntry;
import net.sf.odinms.server.maps.MapleReactor;

/**
 * @author Lerk
 */
public class ReactorScriptManager extends AbstractScriptManager {

	private static ReactorScriptManager instance = new ReactorScriptManager();
	private Map<Integer, List<DropEntry>> drops = new HashMap<Integer, List<DropEntry>>();

	public synchronized static ReactorScriptManager getInstance() {
		return instance;
	}

	public void act(MapleClient c, MapleReactor reactor) {
		try {
			ReactorActionManager rm = new ReactorActionManager(c, reactor);

			Invocable iv = getInvocable("reactor/" + reactor.getId() + ".js", c);
			if (iv == null) {
				// c.getSession().write(MaplePacketCreator.serverNotice(5,
				// "找不到脚本:reactor/" + reactor.getId()+".js,请联系管理员进行修复.."));
				return;
			}
			engine.put("rm", rm);
			ReactorScript rs = iv.getInterface(ReactorScript.class);
			rs.act();
		} catch (Exception e) {
			log.error("反应脚本错误：" + "reactor/" + reactor.getId() + ".js", e);
		}
	}

	public List<DropEntry> getDrops(int rid) {
		List<DropEntry> ret = drops.get(rid);
		if (ret == null) {
			ret = new LinkedList<DropEntry>();
			try {
				PreparedStatement ps = DatabaseConnection
						.getConnection()
						.prepareStatement(
								"SELECT itemid, chance FROM reactordrops WHERE reactorid = ? AND chance >= 0");
				ps.setInt(1, rid);
				ResultSet rs = ps.executeQuery();
				while (rs.next()) {
					ret.add(new DropEntry(rs.getInt("itemid"), rs
							.getInt("chance")));
				}
				rs.close();
				ps.getConnection().close();
				ps.close();
			} catch (Exception e) {
				log.error("Could not retrieve drops for reactor " + rid, e);
			}
			drops.put(rid, ret);
		}
		return ret;
	}

	public void clearDrops() {
		drops.clear();
	}

	public void touch(MapleClient c, MapleReactor reactor) {
		touching(c, reactor, true);
	}

	public void untouch(MapleClient c, MapleReactor reactor) {
		touching(c, reactor, false);
	}

	public void touching(MapleClient c, MapleReactor reactor, boolean touching) {
		try {
			ReactorActionManager rm = new ReactorActionManager(c, reactor);
			Invocable iv = getInvocable("reactor/" + reactor.getId() + ".js", c);
			if (iv == null) {
				// c.getSession().write(MaplePacketCreator.serverNotice(5,
				// "找不到脚本:reactor/" + reactor.getId()+".js,请联系管理员进行修复.."));
				return;
			}
			engine.put("rm", rm);
			ReactorScript rs = iv.getInterface(ReactorScript.class);
			if (touching) {
				rs.touch();
			} else {
				rs.untouch();
			}
		} catch (Exception e) {
		}
	}
}