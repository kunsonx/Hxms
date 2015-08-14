
package net.sf.odinms.net.channel.handler;


import net.sf.odinms.client.MapleCharacter;
import net.sf.odinms.client.MapleClient;
import net.sf.odinms.client.SkillFactory;
import net.sf.odinms.net.AbstractMaplePacketHandler;
import net.sf.odinms.tools.data.input.SeekableLittleEndianAccessor;

public class AranComboHandler extends AbstractMaplePacketHandler{
    
    @Override
    public void handlePacket(SeekableLittleEndianAccessor slea, MapleClient c) {
         MapleCharacter chr = c.getPlayer();
            chr.handleComboGain();
            int combo = chr.getCombo();
            if(combo % 10 == 0) { //如果连击是整数
   //             SkillFactory.getSkill(21000000).getEffect(combo > 100 ? 10 : combo / 10).applyTo(chr);
                //log.debug("矛连击强化发动 连击数是"+combo);
            }
    }
}
