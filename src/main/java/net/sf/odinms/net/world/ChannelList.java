/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package net.sf.odinms.net.world;

import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.*;
import net.sf.odinms.net.channel.ChannelDescriptor;
import net.sf.odinms.net.channel.remote.ChannelWorldInterface;

/**
 *
 * @author hxms
 */
public class ChannelList implements Iterable<ChannelWorldInterface> {

    private Map<ChannelDescriptor, ChannelWorldInterface> list = new ConcurrentHashMap<ChannelDescriptor, ChannelWorldInterface>();

    public void add(ChannelDescriptor channelDescriptor, ChannelWorldInterface cwi) {
        list.put(channelDescriptor, cwi);
    }

    public void remove(ChannelDescriptor cd) {
        list.remove(cd);
    }

    public ChannelWorldInterface getChannel(int channel) {
        for (ChannelDescriptor channelDescriptor : list.keySet()) {
            if (channelDescriptor.getId() == channel) {
                return list.get(channelDescriptor);
            }
        }
        return null;
    }

    public Collection<ChannelDescriptor> getChannelDescriptors() {
        return list.keySet();
    }

    @Override
    public Iterator<ChannelWorldInterface> iterator() {
        return list.values().iterator();
    }
}
