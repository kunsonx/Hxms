package net.sf.odinms.server.maps;

import java.awt.Point;
import net.sf.odinms.client.GameConstants;
import net.sf.odinms.client.MapleCharacter;
import net.sf.odinms.client.MapleClient;
import net.sf.odinms.client.SkillFactory;
import net.sf.odinms.client.skills.机械师;
import net.sf.odinms.server.MapleStatEffect;
import net.sf.odinms.tools.MaplePacketCreator;

/**
 *
 * @author Jan
 */
public final class MapleSummon extends AbstractAnimatedMapleMapObject {

    private MapleCharacter owner;
    private int skillLevel = 1;
    private int skill;
    private int hp;
    private byte status;
    private byte removeStatus;
    private byte actionstats = 2;
    private byte otherVal = 0;
    private byte otherVal2 = 0;
    private byte otherVal3 = 4;
    private byte otherVal4 = 0;
    private SummonMovementType movementType;
    private MapleStatEffect effect;

    public MapleSummon(MapleCharacter owner, int skill, Point pos, SummonMovementType movementType) {
        this.owner = owner;
        this.skill = skill;

        if (skill == 35121011) {
            this.skillLevel = 1;
        } else {
            this.skillLevel = owner.getSkillLevel(SkillFactory.getSkill(skill));
        }
        this.movementType = movementType;
        if (isPuppet() || isStationary() || isRobot() || skill == 3310100 || skill == 5321003) { //3310100 地雷
            this.status = 0;
        } else if (isRobot2()) {
            this.status = 0;
            this.removeStatus = 5;
        } else if (skill == 35121003) {
            this.status = 5;
        } else if (isBeholder()) {
            this.status = 2;
            this.removeStatus = 12;
        } else if (is人造卫星()) {
            this.status = 3;
            this.removeStatus = 10;
        } else if (skill == 35121009) {
            this.status = 4;
        } else if (skill == 35121011) { //机器人工厂召唤出来的小机器人
            this.removeStatus = 5;
        } else if (isDarkJuggling()) { //黑暗杂耍
            this.status = 6;
            this.removeStatus = 0;
        } else {
            this.status = 1;
            this.removeStatus = 4;
        }
        if (GameConstants.isAngelRingSkill(skill)) {
            this.status = 1;
            this.removeStatus = 10;
        }
        if (skill == 35121003) {
            this.status = 6;
            this.removeStatus = 10;
        }
        if (skill == 4341006) {
            this.otherVal = 0x25;
        } else if (skill == 36121002) {
            this.otherVal = 0x15;
            this.actionstats = 1;
            this.otherVal2 = 0x15;
            this.status = 3;
        }
        setPosition(pos);
    }

    @Override
    public void sendSpawnData(MapleClient client) {
        client.getSession().write(MaplePacketCreator.spawnSpecialMapObject(this, skillLevel));
    }

    @Override
    public void sendDestroyData(MapleClient client) {
        client.getSession().write(MaplePacketCreator.removeSpecialMapObject(this, true));
    }

    public MapleCharacter getOwner() {
        return this.owner;
    }

    public int getSkill() {
        return this.skill;
    }

    public int getStatus() {
        return status;
    }

    public void setStatus(int status) {
        this.status = (byte) status;
    }

    public int getHP() {
        return this.hp;
    }

    public int getRemoveStatus() {
        return removeStatus;
    }

    public void addHP(int delta) {
        this.hp += delta;
    }

    public SummonMovementType getMovementType() {
        return movementType;
    }

    public boolean isPuppet() {
        return (skill == 4341006 || skill == 3120012 || skill == 36121002);
    }

    public boolean isDarkJuggling() {
        return (skill == 4111007 || skill == 4211007);
    }

    public boolean isStationary() {
        return (skill == 3111002 || skill == 3211002 || skill == 5211001 || skill == 13111004);
    }

    public boolean isRobot() {
        return skill == 机械师.磁场;
    }

    public boolean isRobot2() { //加速器 放大器
        return skill == 35111005 || skill == 35121010;
    }

    public boolean is人造卫星() {
        return (skill == 机械师.人造卫星 || skill == 机械师.人造卫星2 || skill == 机械师.人造卫星3);
    }

    public boolean isBeholder() {
        return skill == 1321007;
    }

    public int getSkillLevel() {
        return skillLevel;
    }

    public boolean isBuffSummon() {
        if (GameConstants.isAngelRingSkill(skill)) {
            return true;
        }
        switch (skill) {
            case 3101007: // 银鹰召唤
            case 3201007: // 金鹰召唤
            case 33111005: // 银鹰召唤
            case 3111005: // 火凤凰
            case 3211005: //冰凤凰
            /*
             * case 3111002: // 射手 - 替身术 case 3211002: // 游侠 - 替身术 case
             * 3120012://精英替身 case 3220012://精英替身
             */
            case 4341006: // 暗影双刀 - 傀儡召唤
            case 13111004: //风灵使者 - 替身术
            case 33111003: //野性陷阱
            case 23111008:
            case 23111009:
            case 23111010://双弩的3个精灵。
            case 1321007://灵魂助力
            case 35111001: // 机械师 - 人造卫星
            case 35111009: // 机械师 - 人造卫星
            case 35111010: // 机械师 - 人造卫星
                return true;
        }
        return false;
    }

    public boolean isRingSummon() {
        return GameConstants.isAngelRingSkill(skill);
    }

    @Override
    public MapleMapObjectType getType() {
        return MapleMapObjectType.SUMMON;
    }

    public byte getActionstats() {
        return actionstats;
    }

    public void setActionstats(int actionstats) {
        this.actionstats = (byte) actionstats;
    }

    public MapleStatEffect getEffect() {
        return effect;
    }

    public void setEffect(MapleStatEffect effect) {
        this.effect = effect;
    }

    public byte getOtherVal() {
        return otherVal;
    }

    public byte getOtherVal2() {
        return otherVal2;
    }

    public byte getOtherVal3() {
        return otherVal3;
    }

    public byte getOtherVal4() {
        return otherVal4;
    }
}
