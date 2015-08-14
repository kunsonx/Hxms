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

package net.sf.odinms.server.maps;

import java.awt.Point;
import java.awt.Rectangle;

import net.sf.odinms.client.ISkill;
import net.sf.odinms.client.MapleCharacter;
import net.sf.odinms.client.MapleClient;
import net.sf.odinms.client.SkillFactory;
import net.sf.odinms.net.MaplePacket;
import net.sf.odinms.server.MapleStatEffect;
import net.sf.odinms.server.life.MapleMonster;
import net.sf.odinms.server.life.MobSkill;
import net.sf.odinms.tools.MaplePacketCreator;
import org.apache.log4j.Logger;

public class MapleMist extends AbstractMapleMapObject {

    private Rectangle mistPosition;
    private MapleCharacter owner;
    private MapleMonster mobowner;
    private MapleStatEffect source;
    private boolean isPoison = false;
    private boolean isMobMist = false;
    private boolean isItemMist = false;
    private int mistValue1;
    private int mistValue2;
    private MobSkill mobskill;
    private MapleMonster mob = null;
    private final Logger log = Logger.getLogger(getClass());
     

    /*
     * v1
     * mob 0
     * poison 1 
     * itemmist 3
     * else 2

     * v2
     * mob 0
     * item 3
     * else 8
     */
    public MapleMist(Rectangle mistPosition, MapleCharacter owner, MapleStatEffect source) {
        this.mistPosition = mistPosition;
        this.owner = owner;
        this.source = source;
        switch (source.getSourceId()) {
            case 4221006:
            case 32121006:
                this.mistValue1 = 2;
                this.mistValue1 = 8;
                break;
            case 35121010: //放大器
                this.mistValue1 = 5;
                this.mistValue1 = 10;
		break;
            case 22161003: //极光恢复
                this.mistValue1 = 4;
                this.mistValue1 = 10;
		break;
            case 2111003: // 致命毒雾
            case 12111005: // 火牢术屏障
            case 14111006: // 毒炸弹
                this.isPoison = true;
                this.mistValue1 = 1;
                this.mistValue2 = 7;
                break;
            case 5281000: //臭屁
            case 5281001: //花香
                this.isItemMist = true;
                this.mistValue1 = 3;
                this.mistValue2 = 3;
                break;
            default:
                log.debug("未赋值的烟雾 要在MapleMist里赋值");
                break;
        }
    }
    
    public MapleMist(Rectangle mistPosition, MapleMonster mob, MobSkill skill) {
        //怪物烟雾 例如乌贼的毒雾
        this.mistPosition = mistPosition;
        this.mob = mob;
        this.mobskill = skill;
        this.mistValue1 = 0;
        this.mistValue2 = 0;
        isMobMist = true;
    }
    
    @Override
    public MapleMapObjectType getType() {
        return MapleMapObjectType.MIST;
    }

    @Override
    public Point getPosition() {
        return mistPosition.getLocation();
    }

    public MapleCharacter getOwner() {
        return owner;
    }

    public ISkill getSourceSkill() {
        return SkillFactory.getSkill(source.getSourceId());
    }

    public Rectangle getBox() {
        return mistPosition;
    }

    public boolean isPoison() {
        return isPoison;
    }

    public void setPoison(boolean poison) {
        isPoison = poison;
    }

    public boolean isMobMist() {
        return isMobMist;
    }

    public boolean isItemMist() {
        return isItemMist;
    }
        
    public void setMobMist(boolean tf) {
        isMobMist = tf;
    }

    @Override
    public void setPosition(Point position) {
        throw new UnsupportedOperationException();
    }

    public MaplePacket makeDestroyData() {
        return MaplePacketCreator.removeMist(this);
    }

    @Override
    public void sendDestroyData(MapleClient client) {
        client.getSession().write(makeDestroyData());
    }

    public MaplePacket makeSpawnData() {
        return MaplePacketCreator.spawnMist(this);
    }

    @Override
    public void sendSpawnData(MapleClient client) {
        client.getSession().write(makeSpawnData());
    }

    public boolean makeChanceResult() {
        return source.makeChanceResult();
    }

    public int getSourceId() {
        return source.getSourceId();
    }
    
    public int getMistValue1() {
        return mistValue1;
    }
    
    public int getMistValue2() {
        return mistValue2;
    }
    
    public MapleMonster getMobOwner() {
        return mob;
    }

    public MobSkill getMobSkill() {
        return mobskill;
    }
}
