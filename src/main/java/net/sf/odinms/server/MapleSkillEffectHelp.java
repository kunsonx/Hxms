/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package net.sf.odinms.server;

import java.util.Collections;
import java.util.concurrent.ScheduledFuture;
import net.sf.odinms.client.MapleBuffStat;
import net.sf.odinms.client.MapleCharAttribute;
import net.sf.odinms.client.MapleCharacter;
import net.sf.odinms.client.SkillFactory;
import net.sf.odinms.server.MapleSkillEffectEvent.EffectEventData;
import net.sf.odinms.tools.MaplePacketCreator;
import net.sf.odinms.tools.Pair;

/**
 *
 * @author Administrator
 */
public class MapleSkillEffectHelp {

    /**
     * 精神连接：火凤凰
     */
    public static void OnApplyBuffEffect_3120006_(MapleStatEffect effect, final MapleCharacter applyfrom, final MapleCharacter applyto, boolean primary, EffectEventData effectEventData) {
        int skilllevel = applyto.getSkillLevel(3111005);
        if (skilllevel > 0) {
            effectEventData.duration = SkillFactory.getSkill(3111005).getEffect(skilllevel).getDuration();
        }
    }

    /**
     * 精神连接：冰凤凰
     */
    public static void OnApplyBuffEffect_3220005_(MapleStatEffect effect, final MapleCharacter applyfrom, final MapleCharacter applyto, boolean primary, EffectEventData effectEventData) {
        int skilllevel = applyto.getSkillLevel(3211005);
        if (skilllevel > 0) {
            effectEventData.duration = SkillFactory.getSkill(3211005).getEffect(skilllevel).getDuration();
        }
    }

    public static void OnApplyBuffEffect_终极无限_(MapleStatEffect effect, final MapleCharacter applyfrom, final MapleCharacter applyto, boolean primary, EffectEventData effectEventData) {
        final MapleStatEffect.DelayEffectAction dea = new MapleStatEffect.DelayEffectAction(effect.getT(), effect.getZ() / 100.0, applyto);
        ScheduledFuture<?> fs = TimerManager.getInstance().register(new Runnable() {
            @Override
            public void run() {
                dea.run();
            }
        }, 4000);
        dea.SetScheduledFuture(fs);
    }

    public static void OnApplyBuffEffect_2022125_(MapleStatEffect effect, final MapleCharacter applyfrom, final MapleCharacter applyto, boolean primary, EffectEventData effectEventData) {
        effectEventData.statups = Collections.singletonList(Pair.Create(MapleBuffStat.EWdef, SkillFactory.getSkill(1320009).getEffect(applyto.getSkillLevel(1320009)).getEpdd()));
    }

    public static void OnApplyBuffEffect_2022126_(MapleStatEffect effect, final MapleCharacter applyfrom, final MapleCharacter applyto, boolean primary, EffectEventData effectEventData) {
        effectEventData.statups = Collections.singletonList(Pair.Create(MapleBuffStat.EMdef, SkillFactory.getSkill(1320009).getEffect(applyto.getSkillLevel(1320009)).getEmdd()));
    }

    public static void OnApplyBuffEffect_2022127_(MapleStatEffect effect, final MapleCharacter applyfrom, final MapleCharacter applyto, boolean primary, EffectEventData effectEventData) {
        effectEventData.statups = Collections.singletonList(Pair.Create(MapleBuffStat.ACC, new Integer(SkillFactory.getSkill(1320009).getEffect(applyto.getSkillLevel(1320009)).getAcc())));
    }

    public static void OnApplyBuffEffect_2022128_(MapleStatEffect effect, final MapleCharacter applyfrom, final MapleCharacter applyto, boolean primary, EffectEventData effectEventData) {
        effectEventData.statups = Collections.singletonList(Pair.Create(MapleBuffStat.AVOID, new Integer(SkillFactory.getSkill(1320009).getEffect(applyto.getSkillLevel(1320009)).getAvoid())));
    }

    public static void OnApplyBuffEffect_2022129_(MapleStatEffect effect, final MapleCharacter applyfrom, final MapleCharacter applyto, boolean primary, EffectEventData effectEventData) {
        effectEventData.statups = Collections.singletonList(Pair.Create(MapleBuffStat.EWatk, SkillFactory.getSkill(1320009).getEffect(applyto.getSkillLevel(1320009)).getEpad()));
    }

    public static void OnApplyBuffEffect_4221013_(MapleStatEffect effect, final MapleCharacter applyfrom, final MapleCharacter applyto, boolean primary, EffectEventData effectEventData) {
        //             applyto.getClient().getSession().write(MaplePacketCreator.cancelBuff(Collections.singletonList(MapleBuffStat.侠盗本能_击杀点), 0));
        applyto.setCombo(0);
    }

    public static void OnApplyBuffEffect_61120008_(MapleStatEffect effect, final MapleCharacter applyfrom, final MapleCharacter applyto, boolean primary, EffectEventData effectEventData) {
        applyto.getAttribute().setDataValue(MapleCharAttribute.KUANGLONGCOMBO, 0);
        applyto.getClient().getSession().write(MaplePacketCreator.giveBuff(0, 0, Collections.singletonList(Pair.Create(MapleBuffStat.狂龙蓄气, 0)), applyto));
    }
}
