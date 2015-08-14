/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package net.sf.odinms.net.channel.handler;

import net.sf.odinms.client.MapleClient;
import net.sf.odinms.client.SkillFactory;
import net.sf.odinms.net.AbstractMaplePacketHandler;
import net.sf.odinms.server.MapleStatEffect;
import net.sf.odinms.server.maps.MapleMapObject;
import net.sf.odinms.server.maps.MapleSummon;
import net.sf.odinms.tools.MaplePacketCreator;
import net.sf.odinms.tools.data.input.SeekableLittleEndianAccessor;
import org.apache.log4j.Logger;

/**
 *
 * @author Administrator
 */
public class SummomSkillHandler extends AbstractMaplePacketHandler {

    private Logger log = Logger.getLogger(getClass());

    @Override
    public void handlePacket(SeekableLittleEndianAccessor slea, MapleClient c) {
        int oid = slea.readInt();
        int skill = slea.readInt();
        int skilllevel = c.getPlayer().getSkillLevel(skill);
        MapleMapObject obj = c.getPlayer().getMap().getMapObject(oid);
        if (skilllevel > 0 && obj != null && obj instanceof MapleSummon) {
            MapleSummon summon = (MapleSummon) obj;
            if (summon.getSkill() == 35121003) {
                return;
            }
            final MapleStatEffect effect = SkillFactory.getSkill(skill).getEffect(c.getPlayer().getSkillLevel(skill));
            if (skill == 1320009) {
                slea.skip(1);
                c.getPlayer().giveItemBuff(2022125 + slea.readByte());
                c.getSession().write(MaplePacketCreator.showOwnBuffEffect_(summon.getSkill(), 2, c.getPlayer().getSkillLevel(summon.getSkill())));
            } else if (skill == 1320008) {
                //System.out.println("补血");
                c.getPlayer().addHP(effect.getHp());
            } else if (skill == 35121009) { //机器人工厂
                for (int i = 0; i < 3; i++) {
                    //SkillFactory.getSkill(35121011).getEffect(c.getPlayer().getSkillLevel(35121009)).applyTo(c.getPlayer(), summon.getPosition());
                    SkillFactory.getSkill(35121011).getEffect(c.getPlayer().getSkillLevel(35121011)).applyTo(c.getPlayer(), summon.getPosition());
                }
            } else if (skill == 35111011) { //治疗机器人
                double multiplier = (double) SkillFactory.getSkill(skill).getEffect(c.getPlayer().getSkillLevel(skill)).getHp() / 100;
                c.getPlayer().addHP((int) (c.getPlayer().getMaxhp() * multiplier));
                c.getSession().write(MaplePacketCreator.showOwnBuffEffect(skill, 2));
            }
        }
    }
}
