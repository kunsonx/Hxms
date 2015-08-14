/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package net.sf.odinms.net.world;

import java.util.HashMap;
import java.util.Map;
import net.sf.odinms.net.channel.ChannelDescriptor;

/**
 *
 * @author hxms
 */
public class WorldPlayerStorage {

	private PlayerBuffStorage buffStorage = new PlayerBuffStorage();
	private Map<Integer, ChannelDescriptor> offlinePlayer = new HashMap<Integer, ChannelDescriptor>();

	public PlayerBuffStorage getBuffStorage() {
		return buffStorage;
	}

	public void registryOfficePlayer(int cid, ChannelDescriptor descriptor) {
		offlinePlayer.put(cid, descriptor);
	}

	public ChannelDescriptor deregisterOfficePlayer(int cid) {
		if (offlinePlayer.containsKey(cid)) {
			return offlinePlayer.remove(cid);
		} else {
			return null;
		}
	}
}
