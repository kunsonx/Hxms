/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package net.sf.odinms.server.skill;

import net.sf.odinms.client.MapleBuffStat;
import net.sf.odinms.server.MapleStatEffect;

/**
 *
 * @author hxms
 */
public class MapleForeignBuffHelp {

    public static MapleForeignBuffSkill getForeignBuffSkill(MapleStatEffect effect) {
        MapleForeignBuffSkill skill = new MapleForeignBuffSkill(effect);
        addForeignBuffStat(skill);
        return skill;
    }

    private static void addForeignBuffStat(MapleForeignBuffSkill skill) {
        switch (skill.getEffect().getSourceId()) {
            case 2003516:
                skill.getStats().add(new MapleForeignBuffByteStat(MapleBuffStat.SPEED));
                skill.getStats().add(new MapleForeignBuffShortStat(MapleBuffStat.巨人药水));
                break;
            case 61120008:
                skill.getStats().add(new MapleForeignBuffByteStat(MapleBuffStat.SPEED));
                skill.getStats().add(new MapleForeignBuffShortStat(MapleBuffStat.MORPH));
                break;
            case 61120007:
                skill.getStats().add(new MapleForeignBuffShortStat(MapleBuffStat.KUANGLONG_JIANBI));
                break;
        }
        if (skill.getEffect().isMonsterRiding()) {
            skill.getStats().add(new MapleForeignBuffNoStat(MapleBuffStat.坐骑状态));
        }
    }
}
