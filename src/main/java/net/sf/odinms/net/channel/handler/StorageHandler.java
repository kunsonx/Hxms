/*
 * 仓库处理
 */
package net.sf.odinms.net.channel.handler;

import net.sf.odinms.client.GameConstants;
import net.sf.odinms.client.IItem;
import net.sf.odinms.client.MapleClient;
import net.sf.odinms.client.MapleInventoryType;
import net.sf.odinms.net.AbstractMaplePacketHandler;
import net.sf.odinms.server.MapleInventoryManipulator;
import net.sf.odinms.server.MapleItemInformationProvider;
import net.sf.odinms.server.MapleStorage;
import net.sf.odinms.tools.MaplePacketCreator;
import net.sf.odinms.tools.data.input.SeekableLittleEndianAccessor;

/**
 *
 * @author Matze
 */
public class StorageHandler extends AbstractMaplePacketHandler {

    public void handlePacket(SeekableLittleEndianAccessor slea, MapleClient c) {
        MapleItemInformationProvider ii = MapleItemInformationProvider.getInstance();
        byte mode = slea.readByte();
        final MapleStorage storage = c.getPlayer().getStorage();
        if (mode == 4) { //取出
            byte type = slea.readByte();
            byte slot = slea.readByte();
            slot = storage.getSlot(MapleInventoryType.getByType(type), slot);
            IItem item = storage.takeOut(slot);
            if (item != null) {
                if (MapleInventoryManipulator.checkSpace(c, item.getItemId(), item.getQuantity(), item.getOwner())) {
                    MapleInventoryManipulator.addFromDrop(c, item, "Taken out from storage by " + c.getPlayer().getName(), false);
                } else {
                    storage.store(item);
                    c.getSession().write(MaplePacketCreator.serverNotice(1, "背包已满"));
                }
                storage.sendTakenOut(c, ii.getInventoryType(item.getItemId()));
            } else {
                c.disconnect();
                return;
            }
        } else if (mode == 5) { //存入
            byte slot = (byte) slea.readShort();
            int itemId = slea.readInt();
            short quantity = slea.readShort();
            if (quantity < 1) {
                c.disconnect();
                return;
            }
            if (storage.isFull()) {
                c.getSession().write(MaplePacketCreator.getStorageFull());
                return;
            }
            if (c.getPlayer().getMeso() < 100) {
                c.getSession().write(MaplePacketCreator.serverNotice(1, "你没有足够的金币"));

            } else {
                MapleInventoryType type = ii.getInventoryType(itemId);
                IItem item = c.getPlayer().getInventory(type).getItem(slot).copy();
                if (item.getItemId() == itemId && (item.getQuantity() >= quantity || ii.isThrowingStar(itemId) || ii.isBullet(itemId))) {
                    if (ii.isThrowingStar(itemId) || ii.isBullet(itemId)) {
                        quantity = item.getQuantity();
                    }
                    if (!c.getPlayer().haveItem(itemId, quantity, true, false)) {
                        return;
                    }
                    item.log("Stored by " + c.getPlayer().getName(), false);
                    c.getPlayer().gainMeso(-100, false, true, false);
                    MapleInventoryManipulator.removeFromSlot(c, type, slot, quantity, false);
                    item.setQuantity(quantity);
                    storage.store(item);
                    storage.sendStored(c, ii.getInventoryType(itemId)); //防止钱不足之后还会显示有装备移动进去的bug
                } else {
                    c.disconnect();
                    return;
                }
            }
        } else if (mode == 7) { // meso
            long meso = slea.readLong();
            long storageMesos = storage.getMeso();
            long playerMesos = c.getPlayer().getMeso();
            if ((meso > 0 && storageMesos >= meso) || (meso < 0 && playerMesos >= -meso)) {
                if (meso < 0 && (storageMesos - meso) < 0) { // storing with overflow
                    meso = -(GameConstants.MAX_MESO - storageMesos);
                    if ((-meso) > playerMesos) { // should never happen just a failsafe
                        throw new RuntimeException("everything sucks");
                    }
                } else if (meso > 0 && (playerMesos + meso) < 0) { // taking out with overflow
                    meso = (GameConstants.MAX_MESO - playerMesos);
                    if ((meso) > storageMesos) { // should never happen just a failsafe
                        throw new RuntimeException("everything sucks");
                    }
                }
                storage.setMeso(storageMesos - meso);
                c.getPlayer().gainMeso(meso, false, true, false);
            } else {
                c.disconnect();
                return;
            }
            storage.sendMeso(c);
        } else if (mode == 8) { // close
            storage.close();
        }
    }
}