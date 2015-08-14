/*
抽奖程序
 */
package net.sf.odinms.net.channel.handler;

import net.sf.odinms.client.MapleClient;
import net.sf.odinms.net.AbstractMaplePacketHandler;
import net.sf.odinms.tools.data.input.SeekableLittleEndianAccessor;

public class choujiangHandler extends AbstractMaplePacketHandler {

    private static org.apache.log4j.Logger log = org.apache.log4j.Logger.getLogger(ChangeMapHandler.class);

    @Override
    public void handlePacket(SeekableLittleEndianAccessor slea, MapleClient c) {
        //[72 00] [02] [00 C0 4E 41] 00 00]
        //[72 00] [03] [00 C1] [4E 41 00 01]
        //[72 00] [03] [00 C1] 4E 41 00 [00]
        //[72 00] [02] [00 C0] [4E 41 00 00]
        //[77 00] [04] [00 C0] [4E 41 00 00]
        int mode = slea.readShort();
        int itemID = slea.readInt();  //道具ID
        byte ty = slea.readByte();  //操作类型
        if (mode == 1) { // 这个不知道
            log.info("迷之蛋模块" + mode);
        } else if (mode == 22) { // 重生迷之蛋 500点券的
            if (ty == 0) {
                if (c.getPlayer().haveItem(5490001, 1, true, true)) {
                    c.getPlayer().dropMessage(1, "现在暂时不能抽奖，因为抽奖功能还没完善！");
                } else {
                    c.getPlayer().dropMessage(1, "孵化重生迷之蛋需要重生的热度，请到商城购买！");
                }
            } else {
                if (c.getPlayer().getCSPoints(1) >= 500) {
                    c.getPlayer().dropMessage(1, "系统错误，请到商城购买！");
                } else {
                    c.getPlayer().dropMessage(1, "抵用券不足500点，请到商城购买“抵用券兑换包”即可充值抵用券！");
                }
            }
        } else if (mode == 4) { // 永恒迷之蛋 800点券的
            if (ty == 0) {
                if (c.getPlayer().haveItem(5490000, 1, true, true)) {
                    c.getPlayer().dropMessage(1, "现在暂时不能抽奖，因为抽奖功能还没完善！");
                } else {
                    c.getPlayer().dropMessage(1, "孵化永恒迷之蛋需要永恒的热度，请到商城购买！");
                }
            } else {
                if (c.getPlayer().getCSPoints(1) >= 800) {
                    c.getPlayer().dropMessage(1, "系统错误，请到商城购买！");
                } else {
                    c.getPlayer().dropMessage(1, "抵用券不足800点，请到商城购买“抵用券兑换包”即可充值抵用券！");
                }
            }
        } else {
            log.info("迷之蛋模块" + mode);
        }
    }
}
