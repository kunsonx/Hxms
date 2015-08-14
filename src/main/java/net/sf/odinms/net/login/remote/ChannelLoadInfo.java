/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package net.sf.odinms.net.login.remote;

import java.rmi.RemoteException;
import java.util.HashMap;
import java.util.Map;
import net.sf.odinms.net.channel.ChannelDescriptor;
import net.sf.odinms.net.world.ChannelServerStorage;

/**
 *
 * @author hxms
 */
public class ChannelLoadInfo implements java.io.Serializable {

	private int worldCount = 0;
	private int[] channels;
	private Map<ChannelDescriptor, Integer> loads = new HashMap<ChannelDescriptor, Integer>();

	public ChannelLoadInfo() {
	}

	public ChannelLoadInfo(ChannelServerStorage channelServerStorage)
			throws RemoteException {
		for (ChannelDescriptor channelDescriptor : channelServerStorage
				.getChannelDescriptors()) {
			loads.put(channelDescriptor, channelServerStorage
					.getChannelWorldInterface(channelDescriptor).getConnected());
		}
		updateInfo();
	}

	private void updateInfo() {
		worldCount = 0;
		for (ChannelDescriptor channelDescriptor : loads.keySet()) {
			worldCount = Math.max(worldCount, channelDescriptor.getWorld() + 1);
		}
		channels = new int[worldCount];
		for (ChannelDescriptor channelDescriptor : loads.keySet()) {
			channels[channelDescriptor.getWorld()] += 1;
		}
	}

	public int getWorldCount() {
		return worldCount;
	}

	public int getChannelCount(int world) {
		return channels[world];
	}

	public int getChannelValue(int world, int channel) {
		for (ChannelDescriptor channelDescriptor : loads.keySet()) {
			if (channelDescriptor.getWorld() == world
					&& channelDescriptor.getId() == channel) {
				return loads.get(channelDescriptor);
			}
		}
		return 200;
	}

	public void setChannelValue(int world, int channel, int value) {
		for (ChannelDescriptor channelDescriptor : loads.keySet()) {
			if (channelDescriptor.getWorld() == world
					&& channelDescriptor.getId() == channel) {
				loads.put(channelDescriptor, value);
			}
		}
	}

	public void addChannel(ChannelDescriptor channelDescriptor) {
		loads.put(channelDescriptor, 0);
		updateInfo();
	}

	public void removeChannel(ChannelDescriptor channelDescriptor) {
		loads.remove(channelDescriptor);
		updateInfo();
	}
}
