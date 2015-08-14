/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package net.sf.odinms.net.channel;

import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import net.sf.odinms.server.playerinteractions.PlayerInteractionManager;

/**
 * 频道雇佣管理集合
 *
 * @author hxms
 */
public class ChannelsInteractionManager {

    private Map<Integer, PlayerInteractionManager> items = new ConcurrentHashMap<Integer, PlayerInteractionManager>();
    private Lock lock = new ReentrantLock(true);

    public ChannelsInteractionManager() {
    }

    public void add(int characterid, PlayerInteractionManager instance) {
        lock.lock();
        try {
            items.put(characterid, instance);
        } finally {
            lock.unlock();
        }
    }

    public void remove(int characterid) {
        lock.lock();
        try {
            if (items.containsKey(characterid)) {
                items.remove(characterid);
            }
        } finally {
            lock.unlock();
        }
    }

    public boolean contains(int characterid) {
        return items.containsKey(characterid);
    }

    public void shutdown() {
        lock.lock();
        try {
            for (Iterator<PlayerInteractionManager> it = items.values().iterator(); it.hasNext();) {
                PlayerInteractionManager pim = it.next();
                pim.closeShop(true);
            }
            //     items.clear();
        } finally {
            lock.unlock();
        }
    }
}
