/*
 自动回复血和兰
 */
package net.sf.odinms.net.channel.handler;

import java.util.Arrays;
import net.sf.odinms.client.MapleBuffStat;
import net.sf.odinms.client.MapleClient;
import net.sf.odinms.client.SkillFactory;
import net.sf.odinms.client.anticheat.CheatingOffense;
import net.sf.odinms.net.AbstractMaplePacketHandler;
import net.sf.odinms.tools.MaplePacketCreator;
import net.sf.odinms.tools.Pair;
import net.sf.odinms.tools.data.input.SeekableLittleEndianAccessor;

public class HealOvertimeHandler extends AbstractMaplePacketHandler {

    @Override
    public void handlePacket(SeekableLittleEndianAccessor slea, MapleClient c) {
        //5B 00 [0A] [49 0D] [02] [00 50 00 00] [0A 00] [00 00] 00 00 E8 0D 02
        //8A 00 [AA] [EF 9A] [00] [00 14 00 00] [00 00] [00 00] 0A 00 00 00 00 87 00 9D 00
        //8F 00 [AB] [65 8C] [00] [00 14 00 00] [00 00] [00 00] [00 00] 04 00 02 91 FD 8D 00
        //8F 00 [7F] [6C 3B] [00] [00 14 00 00] [00 00] [00 00] [0A 00] 00 00 00 3A 36 3C 00
        //8F 00 
        //C5 7E 3D 00 00 14 00 00 00 00 
        //00 00 23 00 00 00 02 83 48 3E 00
        //99 00 7B 8F AA 04 00 14 00 00 00 00 00 00 0A 00 00 00 00 D1 FD AA 04
        slea.skip(10);
        int healHP = slea.readShort();
        int healMP = slea.readShort();
        if (!c.getPlayer().isAlive()) {
            return;
        }
        if (c.getPlayer().getJob().IsDemonHunter()) {
            healHP = healMP;
            healMP = 0;
        }
        if (healHP != 0) {
            if (healHP > 140) {
                c.getPlayer().getCheatTracker().registerOffense(CheatingOffense.REGEN_HIGH_HP, String.valueOf(healHP));
                return;
            }
            c.getPlayer().getCheatTracker().checkHPRegen();
            if (c.getPlayer().getCurrentMaxHp() == c.getPlayer().getHp()) {
                c.getPlayer().getCheatTracker().resetHPRegen();
            }
            c.getPlayer().addHP(healHP);
            //c.getPlayer().checkBerserk();
        }
        if (healMP != 0) {
            if (healMP > 1000) // Definitely impossible
            {
                return;
            }
            float theoreticalRecovery = 0;
            theoreticalRecovery += Math.floor((((float) c.getPlayer().getSkillLevel(SkillFactory.getSkill(2000000)) / 10) * c.getPlayer().getLevel()) + 3);
            if (healMP > theoreticalRecovery) {
                if (healMP > 300) { // seems almost impossible
                    c.getPlayer().getCheatTracker().registerOffense(CheatingOffense.REGEN_HIGH_MP, String.valueOf(healMP));
                }
            }
            c.getPlayer().getCheatTracker().checkMPRegen();
            c.getPlayer().addMP(healMP);
            if (c.getPlayer().getCurrentMaxMp() == c.getPlayer().getMp()) {
                c.getPlayer().getCheatTracker().resetMPRegen();
            }
        }

    }
}