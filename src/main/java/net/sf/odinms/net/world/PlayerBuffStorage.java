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
package net.sf.odinms.net.world;

import java.io.Serializable;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 *
 * @author Danny
 */
public class PlayerBuffStorage implements Serializable {

    private Map<Integer, List<PlayerBuffValueHolder>> _buffs = new ConcurrentHashMap<Integer, List<PlayerBuffValueHolder>>();
    private Lock lock = new ReentrantLock(true);
    //  private List<Pair<Integer, List<PlayerBuffValueHolder>>> buffs = new ArrayList<Pair<Integer, List<PlayerBuffValueHolder>>>();
    private int id = (int) (Math.random() * 10000);

    public void addBuffsToStorage(int chrid, List<PlayerBuffValueHolder> toStore) {
        /*      for (Pair<Integer, List<PlayerBuffValueHolder>> stored : buffs) {
         if (stored.getLeft() == Integer.valueOf(chrid)) {
         buffs.remove(stored);
         }
         }
         buffs.add(new Pair<Integer, List<PlayerBuffValueHolder>>(Integer.valueOf(chrid), toStore));*/
        try {
            lock.lock();
            _buffs.put(chrid, toStore);
        } finally {
            lock.unlock();
        }
    }

    public List<PlayerBuffValueHolder> getBuffsFromStorage(int chrid) {
        /*   List<PlayerBuffValueHolder> ret = null;
         Pair<Integer, List<PlayerBuffValueHolder>> stored;
         for (int i = 0; i < buffs.size(); i++) {
         stored = buffs.get(i);
         if (stored.getLeft().equals(Integer.valueOf(chrid))) {
         ret = stored.getRight();
         buffs.remove(stored);
         }
         }
         return ret;*/
        try {
            lock.lock();
            List<PlayerBuffValueHolder> ret = null;
            if (_buffs.containsKey(chrid)) {
                ret = _buffs.get(chrid);
                _buffs.remove(chrid);
            }
            return ret;
        } finally {
            lock.unlock();
        }
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + id;
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final PlayerBuffStorage other = (PlayerBuffStorage) obj;
        if (this.id != other.id) {
            return false;
        }
        return true;
    }
}
