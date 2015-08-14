/*
 * 灵魂祝福
 * 灵魂治愈
 * 灵魂助力
 * 机器人工厂
 */
package net.sf.odinms.net.channel.handler;

import java.util.List;
import net.sf.odinms.client.ISkill;
import net.sf.odinms.client.MapleBuffStat;
import net.sf.odinms.client.MapleClient;
import net.sf.odinms.client.SkillFactory;
import net.sf.odinms.net.AbstractMaplePacketHandler;
import net.sf.odinms.server.MapleStatEffect;
import net.sf.odinms.server.maps.MapleSummon;
import net.sf.odinms.tools.MaplePacketCreator;
import net.sf.odinms.tools.Pair;
import net.sf.odinms.tools.data.input.SeekableLittleEndianAccessor;

public class SpecialSummonHandler extends AbstractMaplePacketHandler {

    @Override
    public void handlePacket(SeekableLittleEndianAccessor slea, final MapleClient c) {
        ////System.out.println("特殊召唤兽处理："+slea.toString());
        int oid = slea.readInt();
        MapleSummon summon = null;
        for (List<MapleSummon> sums : c.getPlayer().getSummons().values()) {
            for (MapleSummon sum : sums) {
                if (sum.getObjectId() == oid) {
                    summon = sum;
                }
            }
        }
        if (summon != null) {
            int skillId = slea.readInt();
            ISkill skill = SkillFactory.getSkill(skillId);
            final MapleStatEffect effect = skill.getEffect(c.getPlayer().getSkillLevel(skill));
            if (skillId == 1320009) {
                /*
                 * D1 00 14 91 23 00 49 24 14 00 [08 00] 物防 8
                 * D1 00 14 91 23 00 49 24 14 00 [0C 04] 攻击 1036
                 * D1 00 14 91 23 00 49 24 14 00 [09 01] 魔防 265
                 * D1 00 14 91 23 00 49 24 14 00 [0A 02] 回避 522
                 * D1 00 14 91 23 00 49 24 14 00 [0B 03] 命中 779
                 * 
                 * 2022125 物理防御力上升
                 * 2022126 魔法防御力上升
                 * 2022127 命中率上升
                 * 2022128 回避率上升
                 * 2022129 攻击力上升
                 */
                Pair<MapleBuffStat, Integer> stat = null;
                int Type = slea.readShort();
                int buffid = 0;
                ////System.out.println("灵魂祝福BUFF类型："+Type);
                if (Type == 8 || Type == 136) { // 物防
                    ////System.out.println("物防");
                    buffid = -2022125;
                    stat = new Pair<MapleBuffStat, Integer>(MapleBuffStat.EWdef, effect.getEWdef());
                } else if (Type == 265 || Type == 393) { //魔防
                    //System.out.println("魔防");
                    buffid = -2022126;
                    stat = new Pair<MapleBuffStat, Integer>(MapleBuffStat.EMdef, effect.getEMdef());
                } else if (Type == 522 || Type == 650) { //回避
                    //System.out.println("回避");
                    buffid = -2022128;
                    stat = new Pair<MapleBuffStat, Integer>(MapleBuffStat.AVOID, (int) effect.getAvoid());
                } else if (Type == 779 || Type == 907) { //命中
                    //System.out.println("命中");
                    buffid = -2022127;
                    stat = new Pair<MapleBuffStat, Integer>(MapleBuffStat.ACC, (int) effect.getAcc());
                } else if (Type == 1036 || Type == 1164) { //攻击
                    //System.out.println("攻击");
                    buffid = -2022129;
                    stat = new Pair<MapleBuffStat, Integer>(MapleBuffStat.EWatk, effect.getEWatk());
                } else {
                    c.getPlayer().dropMessage(1, "未知类型：" + Type);
                }
                if (stat != null) {
                    effect.setSourceId(buffid);
                    effect.getStatups().clear();
                    effect.getStatups().add(stat);
                    //System.out.println("effect.getStatups()："+effect.getStatups());
                    effect.applyTo(c.getPlayer());
                }
                c.getSession().write(MaplePacketCreator.showOwnBuffEffect(skillId, 2));
            } else if (skillId == 1320008) {
                //System.out.println("补血");
                c.getPlayer().addHP(effect.getHp());
            } else if (skillId == 35121009) { //机器人工厂
                for (int i = 0; i < 3; i++) {
                    //SkillFactory.getSkill(35121011).getEffect(c.getPlayer().getSkillLevel(35121009)).applyTo(c.getPlayer(), summon.getPosition());
                    SkillFactory.getSkill(35121011).getEffect(c.getPlayer().getSkillLevel(35121011)).applyTo(c.getPlayer(), summon.getPosition());
                }
            } else if (skillId == 35111011) { //治疗机器人
                double multiplier = (double) SkillFactory.getSkill(skillId).getEffect(c.getPlayer().getSkillLevel(skillId)).getHp() / 100;
                c.getPlayer().addHP((int) (c.getPlayer().getMaxhp() * multiplier));
                c.getSession().write(MaplePacketCreator.showOwnBuffEffect(skillId, 2));
            }
        }
    }
}
