package net.sf.odinms.net.channel.handler;

import net.sf.odinms.client.ISkill;
import net.sf.odinms.client.MapleClient;
import net.sf.odinms.client.SkillFactory;
import net.sf.odinms.net.channel.handler.DamageParseHandler.AttackInfo;
import net.sf.odinms.server.MapleStatEffect;
import net.sf.odinms.tools.data.input.SeekableLittleEndianAccessor;

/**
 * 
 * @author Bassoe
 */
public class PassiveEnergyHandler extends DamageParseHandler {

	@Override
	public void handlePacket(SeekableLittleEndianAccessor slea, MapleClient c) {
		// System.out.println("被动反击 封包: "+slea.toString());
		ISkill theSkill = null;
		AttackInfo attack = parsePassiveEnergy(c.getPlayer(), slea);
		int maxdamage = c.getPlayer().getCurrentMaxBaseDamage();
		theSkill = SkillFactory.getSkill(attack.skill);
		MapleStatEffect effect = attack
				.getAttackEffect(c.getPlayer(), theSkill);
		if (effect != null) {
			maxdamage *= effect.getDamage() / 100.0;
		}
		applyAttack(attack, c.getPlayer(), maxdamage, 1);
	}
}