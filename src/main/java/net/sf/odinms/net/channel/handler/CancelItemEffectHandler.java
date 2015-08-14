/*
	取消道具效果
*/

package net.sf.odinms.net.channel.handler;

import net.sf.odinms.client.MapleClient;
import net.sf.odinms.net.AbstractMaplePacketHandler;
import net.sf.odinms.server.MapleItemInformationProvider;
import net.sf.odinms.tools.data.input.SeekableLittleEndianAccessor;

public final class CancelItemEffectHandler extends AbstractMaplePacketHandler {

    @Override
    public final void handlePacket(SeekableLittleEndianAccessor slea, MapleClient c) {
        c.getPlayer().cancelEffect(MapleItemInformationProvider.getInstance().getItemEffect(-slea.readInt()), false, -1);
    }
}