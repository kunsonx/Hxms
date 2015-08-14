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
package net.sf.odinms.server;

import java.awt.Point;
import java.io.Serializable;
import net.sf.odinms.client.MapleCharacter;
import net.sf.odinms.client.MapleClient;
import net.sf.odinms.client.anticheat.CheatingOffense;
import net.sf.odinms.scripting.portal.PortalScriptManager;
import net.sf.odinms.server.fourthjobquests.FourthJobQuestsPortalHandler;
import net.sf.odinms.server.maps.MapleMap;
import net.sf.odinms.tools.MaplePacketCreator;

public class MaplePortal implements Serializable {

    private static org.apache.log4j.Logger log = org.apache.log4j.Logger.getLogger(MaplePortal.class);
    public static final int MAP_PORTAL = 2;
    public static final int DOOR_PORTAL = 6;
    public static boolean OPEN = true;
    public static boolean CLOSED = false;
    private String name, target, scriptName;
    private Point position;
    private int targetmap, type, id, mapid;
    private boolean portalState = true;

    public MaplePortal() {
    }

    public MaplePortal(int type, int mapid) {
        this.type = type;
        this.mapid = mapid;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public Point getPosition() {
        return position;
    }

    private void setType(int type) {
        this.type = type;
    }

    public String getTarget() {
        return target;
    }

    public void setPortalStatus(boolean newStatus) {
        this.portalState = newStatus;
    }

    public boolean getPortalStatus() {
        return portalState;
    }

    public int getTargetMapId() {
        return targetmap;
    }

    public int getType() {
        return type;
    }

    public String getScriptName() {
        return scriptName;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setPosition(Point position) {
        this.position = position;
    }

    public void setTarget(String target) {
        this.target = target;
    }

    public void setTargetMapId(int targetmapid) {
        this.targetmap = targetmapid;
    }

    public void setScriptName(String scriptName) {
        this.scriptName = scriptName;
    }

    public void enterPortal(MapleClient c) {
        MapleCharacter player = c.getPlayer();
        double distanceSq = getPosition().distanceSq(player.getPosition());
        if (distanceSq > 22500) {
            player.getCheatTracker().registerOffense(CheatingOffense.USING_FARAWAY_PORTAL, "D" + Math.sqrt(distanceSq));
        }
        boolean changed = false;
        if (getScriptName() != null) {
            //log.info("portal脚本名:  "+this.scriptName);
            if (!FourthJobQuestsPortalHandler.handlePortal(getScriptName(), c.getPlayer())) {
                changed = PortalScriptManager.getInstance().executePortalScript(this, c);
            }
        } else if (getTargetMapId() != 999999999) {
            if (player.getGm() == 100 && log.isDebugEnabled()) {
                player.弹窗("portal脚本名:  " + String.format("%d_%d", mapid, id));
            }
            MapleMap to;
            if (player.getEventInstance() == null) {
                to = c.getChannelServer().getMapFactory().getMap(getTargetMapId());
            } else {
                to = player.getEventInstance().getMapInstance(getTargetMapId());
            }
            if (to != null) {
                MaplePortal pto = to.getPortal(getTarget());
                if (pto == null) { // fallback for missing portals - no real life case anymore - intresting for not implemented areas
                    pto = to.getPortal(0);
                }
                if (getTargetMapId() >= 910000019 && getTargetMapId() <= 910000022) {
                    c.getSession().write(MaplePacketCreator.enableActions());
                    player.getClient().getSession().write(MaplePacketCreator.serverNotice(5, "[任务公告]：请通过npc进入任务房间！"));
                } else {
                    c.getPlayer().changeMap(to, pto);
                    c.getSession().write(MaplePacketCreator.enableActions());
                }
                changed = true;
            } else {
                player.getClient().getSession().write(MaplePacketCreator.serverNotice(5, "无法进入下个地图！"));
            }
        }
        if (!changed) {
            c.getSession().write(MaplePacketCreator.enableActions());
        }
    }

    @Override
    public String toString() {
        return "地图门{" + "name=" + name + ", scriptName=" + scriptName + ", position=" + position + ", id=" + id + '}';
    }

    public int getTargetmap() {
        return targetmap;
    }

    public void setTargetmap(int targetmap) {
        this.targetmap = targetmap;
    }

    public int getMapid() {
        return mapid;
    }

    public void setMapid(int mapid) {
        this.mapid = mapid;
    }

    public boolean isPortalState() {
        return portalState;
    }

    public void setPortalState(boolean portalState) {
        this.portalState = portalState;
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 13 * hash + this.type;
        hash = 13 * hash + this.id;
        hash = 13 * hash + this.mapid;
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final MaplePortal other = (MaplePortal) obj;
        if (this.type != other.type) {
            return false;
        }
        if (this.id != other.id) {
            return false;
        }
        if (this.mapid != other.mapid) {
            return false;
        }
        return true;
    }
}
