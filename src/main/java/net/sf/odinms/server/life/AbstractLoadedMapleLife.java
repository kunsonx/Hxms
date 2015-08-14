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
package net.sf.odinms.server.life;

import net.sf.odinms.client.MapleCharacter;
import net.sf.odinms.server.maps.AbstractAnimatedMapleMapObject;

public abstract class AbstractLoadedMapleLife extends AbstractAnimatedMapleMapObject {

    private final int id;
    private int f;
    private boolean hide;
    private int fh;
    private int start_fh;
    private int cy;
    private int rx0;
    private int rx1;
    private MapleCharacter owner;

    public AbstractLoadedMapleLife(int id) {
        this.id = id;
    }

    public AbstractLoadedMapleLife(AbstractLoadedMapleLife life) {
        this(life.getId());
        this.f = life.f;
        this.hide = life.hide;
        this.fh = life.fh;
        this.start_fh = life.fh;
        this.cy = life.cy;
        this.rx0 = life.rx0;
        this.rx1 = life.rx1;
        this.owner = life.owner;
    }

    public int getF() {
        return f;
    }

    public void setF(int f) {
        this.f = f;
    }

    public boolean isHidden() {
        return hide;
    }

    public void setHide(boolean hide) {
        this.hide = hide;
    }

    public int getFh() {
        return fh;
    }

    public void setFh(int fh) {
        this.fh = fh;
    }

    public int getStartFh() {
        return start_fh;
    }

    public int getCy() {
        return cy;
    }

    public void setCy(int cy) {
        this.cy = cy;
    }

    public int getRx0() {
        return rx0;
    }

    public void setRx0(int rx0) {
        this.rx0 = rx0;
    }

    public int getRx1() {
        return rx1;
    }

    public void setRx1(int rx1) {
        this.rx1 = rx1;
    }

    public int getId() {
        return id;
    }

    public void setOwner(MapleCharacter player) {
        this.owner = player;
    }

    public MapleCharacter getOwner() {
        return owner;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final AbstractLoadedMapleLife other = (AbstractLoadedMapleLife) obj;
        if (this.id != other.id) {
            return false;
        }
        if (this.f != other.f) {
            return false;
        }
        if (this.hide != other.hide) {
            return false;
        }
        if (this.fh != other.fh) {
            return false;
        }
        if (this.start_fh != other.start_fh) {
            return false;
        }
        if (this.cy != other.cy) {
            return false;
        }
        if (this.rx0 != other.rx0) {
            return false;
        }
        if (this.rx1 != other.rx1) {
            return false;
        }
        return super.equals(other);
    }

    @Override
    public int hashCode() {
        int hash = 5;
        hash = 2 * hash + this.id;
        hash = 2 * hash + this.f;
        hash = 2 * hash + (this.hide ? 1 : 0);
        hash = 2 * hash + this.fh;
        hash = 2 * hash + this.start_fh;
        hash = 2 * hash + this.cy;
        hash = 2 * hash + this.rx0;
        hash = 2 * hash + this.rx1;
        return hash;
    }
}