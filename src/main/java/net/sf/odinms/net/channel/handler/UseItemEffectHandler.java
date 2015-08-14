/*
	使用物品效果
*/

package net.sf.odinms.net.channel.handler;

import net.sf.odinms.client.IItem;
import net.sf.odinms.client.MapleClient;
import net.sf.odinms.client.MapleInventoryType;
import net.sf.odinms.net.AbstractMaplePacketHandler;
import net.sf.odinms.tools.MaplePacketCreator;
import net.sf.odinms.tools.data.input.SeekableLittleEndianAccessor;

public class UseItemEffectHandler extends AbstractMaplePacketHandler {

    private static org.apache.log4j.Logger log = org.apache.log4j.Logger.getLogger(UseItemHandler.class);

    public void handlePacket(SeekableLittleEndianAccessor slea, MapleClient c) {
        IItem toUse;
        int itemId = slea.readInt();
        if (itemId == 4290001 || itemId == 4290000) {//倒霉效果 金鸡效果
            toUse = c.getPlayer().getInventory(MapleInventoryType.ETC).findById(itemId);
        } else {
            toUse = c.getPlayer().getInventory(MapleInventoryType.CASH).findById(itemId);
        }
        if (itemId != 0) {
            if (toUse == null) {
                return;
            }
        }
        c.getPlayer().setItemEffect(itemId);
        c.getPlayer().getMap().broadcastMessage(c.getPlayer(), MaplePacketCreator.itemEffect(c.getPlayer().getId(), itemId), false);
    }
}