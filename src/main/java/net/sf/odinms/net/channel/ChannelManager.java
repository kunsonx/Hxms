/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package net.sf.odinms.net.channel;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 *
 * @author hxms
 */
public class ChannelManager {

	private static Map<ChannelDescriptor, ChannelServer> instance = new HashMap<ChannelDescriptor, ChannelServer>();
	private static Map<Integer, List<ChannelServer>> worldofchannels = new HashMap<Integer, List<ChannelServer>>();

	private ChannelManager() {
	}

	public static void init(int world_size) {
		for (int i = 0; i < world_size; i++) {
			worldofchannels.put(i, new CopyOnWriteArrayList<ChannelServer>());
		}
	}

	public static void addChannel(ChannelDescriptor descriptor,
			ChannelServer channelServer) {
		instance.put(descriptor, channelServer);
		worldofchannels.get(descriptor.getWorld()).add(channelServer);
	}

	public static void removeChannel(ChannelDescriptor descriptor) {
		worldofchannels.get(descriptor.getWorld()).remove(
				instance.remove(descriptor));
	}

	public static List<ChannelServer> getChannelServers(
			ChannelDescriptor descriptor) {
		return worldofchannels.get(descriptor.getWorld());
	}

	public static List<ChannelServer> getChannelServers(int world) {
		return worldofchannels.get(world);
	}

	public static ChannelServer getChannelServer(
			ChannelDescriptor channelDescriptor) {
		return instance.get(channelDescriptor);
	}

	public static ChannelServer getChannelServerFromThisWorld(
			ChannelDescriptor channelDescriptor, int channel) {
		for (ChannelServer channelServer : getChannelServers(channelDescriptor)) {
			if (channelServer.getChannel() == channel) {
				return channelServer;
			}
		}
		return null;
	}

	public static Collection<ChannelServer> getAllChannelServers() {
		return instance.values();
	}
}
