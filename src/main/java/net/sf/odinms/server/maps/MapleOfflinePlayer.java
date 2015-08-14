/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package net.sf.odinms.server.maps;

import java.util.HashMap;
import java.util.Map;
import net.sf.odinms.client.MapleCharacter;
import net.sf.odinms.net.MaplePacket;
import net.sf.odinms.tools.MaplePacketCreator;

/**
 *
 * @author hxms
 */
public class MapleOfflinePlayer {

	public static class PlayerData {

		private int cid;
		private MaplePacket spawn;
		private MaplePacket delete;
		private MaplePacket info;

		public PlayerData(int cid, MaplePacket spawn, MaplePacket delete,
				MaplePacket _info) {
			this.cid = cid;
			this.spawn = spawn;
			this.delete = delete;
			this.info = _info;
		}
	}

	private MapleMap map;
	private final Map<Integer, PlayerData> data = new HashMap<Integer, PlayerData>();

	public MapleOfflinePlayer(MapleMap map) {
		this.map = map;
	}

	public MaplePacket getPlayerInfo(int cid) {
		synchronized (data) {
			for (PlayerData playerData : data.values()) {
				if (playerData.cid == cid) {
					return playerData.info;
				}
			}
			return null;
		}
	}

	public void onAddPlayer(MapleCharacter chr) {
		synchronized (data) {
			for (PlayerData playerData : data.values()) {
				chr.getClient().getSession().write(playerData.spawn);
			}
		}
	}

	public void deregisterPlayer(int aid) {
		synchronized (data) {
			if (data.containsKey(aid)) {
				PlayerData d = data.remove(aid);
				map.broadcastMessage(d.delete);
				map.getChannelServer().removeOfflinePlayer();
			}
		}
	}

	public void registryPlayer(MapleCharacter chr) {
		synchronized (data) {
			data.put(chr.getClient().getAccID(), new PlayerData(chr.getId(),
					MaplePacketCreator.spawnPlayerMapobject(chr),
					MaplePacketCreator.removePlayerFromMap(chr.getId()),
					MaplePacketCreator.charInfo(chr)));
			map.broadcastMessage(data.get(chr.getClient().getAccID()).spawn);
			map.getChannelServer().addOfflinePlayer();
		}
	}

	public MapleMap getMap() {
		return map;
	}

	public void setMap(MapleMap map) {
		this.map = map;
	}
}
