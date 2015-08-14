/*
 表情的处理程序
 */
package net.sf.odinms.net.channel.handler;

import net.sf.odinms.client.MapleClient;
import net.sf.odinms.net.AbstractMaplePacketHandler;
import net.sf.odinms.server.MapleItemInformationProvider;
import net.sf.odinms.tools.MaplePacketCreator;
import net.sf.odinms.tools.data.input.SeekableLittleEndianAccessor;

public class FaceExpressionHandler extends AbstractMaplePacketHandler {

    private static org.apache.log4j.Logger log = org.apache.log4j.Logger.getLogger(FaceExpressionHandler.class);

    @Override
    public void handlePacket(SeekableLittleEndianAccessor slea, MapleClient c) {
        int emote = slea.readInt();
        if (emote < 0) {
            return;
        }
        if (emote > 7) {
            int emoteid = 5159992 + emote;
            if (c.getPlayer().getInventory(MapleItemInformationProvider.getInstance().getInventoryType(emoteid)).findById(emoteid) == null) {
                return;
            }
        }
        if (c.getPlayer().getAndroid() != null) {
            c.getPlayer().getMap().broadcastMessage(MaplePacketCreator.showAndroidEmotion(c.getPlayer().getId(), emote));
        }
        c.getPlayer().getMap().broadcastMessage(c.getPlayer(), MaplePacketCreator.facialExpression(c.getPlayer(), emote), false);
    }
}