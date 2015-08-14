/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package net.sf.odinms.net.login;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import net.sf.odinms.net.channel.ChannelDescriptor;
import net.sf.odinms.net.login.remote.ChannelLoadInfo;

/**
 * LoginServer 储存器
 *
 * @author hxms
 */
public class LoginServerStorage {

    private Map<ChannelDescriptor, String> channels = new ConcurrentHashMap<ChannelDescriptor, String>();
    private ChannelLoadInfo loads = new ChannelLoadInfo();
    private Lock lock = new ReentrantLock(true);

    public void addChannel(ChannelDescriptor channel, String ip) {
        try {
            lock.lock();
            channels.put(channel, ip);
            loads.addChannel(channel);
        } finally {
            lock.unlock();
        }
    }

    public void removeChannel(ChannelDescriptor channel) {
        try {
            lock.lock();
            if (channels.containsKey(channel)) {
                channels.remove(channel);
            }
            loads.removeChannel(channel);
        } finally {
            lock.unlock();
        }
    }

    public String getIP(ChannelDescriptor channel) {
        return channels.get(channel);
    }

    public ChannelLoadInfo getLoads() {
        return loads;
    }

    public void setLoads(ChannelLoadInfo loads) {
        this.loads = loads;
    }
}
