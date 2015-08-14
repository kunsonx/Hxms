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
package net.sf.odinms.client;

import java.sql.ResultSet;
import java.sql.SQLException;

/**
 *
 * @author Danny
 */
public class MapleRing extends Equip {

    private int partnerChrId;
    private String partnerName;
    private long partnerUniqueId;
    private boolean equipped;

    public MapleRing(int itemid, long partnerUniqueid, int partnerId, String partnername) {
        super(itemid, (short) 0);
        this.partnerChrId = partnerId;
        this.partnerName = partnername;
        this.partnerUniqueId = partnerUniqueid;
    }

    public static MapleRing loadFromDb(int itemid, short position, long uniqueid, ResultSet rs) {
        try {
            MapleRing ring = new MapleRing(itemid, rs.getInt("partnerUniqueid"), rs.getInt("partnerChrId"), rs.getString("partnerName"));
            ring.setPosition(position);
            ring.setUniqueId(uniqueid);
            return ring;
        } catch (SQLException ex) {
            logger.error("Error loading ring from DB", ex);
            return null;
        }
    }

    public void setPartnerChrId(int partnerChrId) {
        this.partnerChrId = partnerChrId;
    }

    public long getRingId() {
        return getUniqueid();
    }

    public MapleRing setRingId(long value) {
        setUniqueId(value);
        return this;
    }

    public long getPartnerRingId() {
        return partnerUniqueId;
    }

    public int getPartnerChrId() {
        return partnerChrId;
    }

    @Override
    public String getPartnerName() {
        return partnerName;
    }

    public boolean isEquipped() {
        return equipped;
    }

    public void setEquipped(boolean equipped) {
        this.equipped = equipped;
    }

    @Override
    public boolean equals(Object o) {
        if (o instanceof MapleRing) {
            if (((MapleRing) o).getRingId() == getRingId()) {
                return true;
            } else {
                return false;
            }
        }
        return false;
    }

    @Override
    public int hashCode() {
        return System.identityHashCode(this);
    }

    @Override
    public int compareTo(IItem other) {
        if (other instanceof MapleRing) {
            MapleRing t = (MapleRing) other;
            if (getUniqueid() < t.getRingId()) {
                return -1;
            } else if (getUniqueid() == t.getRingId()) {
                return 0;
            } else {
                return 1;
            }
        }
        return super.compareTo(other);
    }

    @Override
    public long getPartnerUniqueId() {
        return partnerUniqueId;
    }

    public void setPartnerUniqueId(long partnerUniqueId) {
        this.partnerUniqueId = partnerUniqueId;
    }

    @Override
    public boolean isRing() {
        return true;
    }
}