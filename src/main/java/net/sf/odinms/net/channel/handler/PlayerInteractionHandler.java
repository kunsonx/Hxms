package net.sf.odinms.net.channel.handler;

import java.util.Arrays;
import net.sf.odinms.client.IItem;
import net.sf.odinms.client.Item;
import net.sf.odinms.client.MapleCharacter;
import net.sf.odinms.client.MapleClient;
import net.sf.odinms.client.MapleInventoryType;
import net.sf.odinms.client.messages.CommandProcessor;
import net.sf.odinms.net.AbstractMaplePacketHandler;
import net.sf.odinms.server.AutobanManager;
import net.sf.odinms.server.MapleInventoryManipulator;
import net.sf.odinms.server.MapleItemInformationProvider;
import net.sf.odinms.server.MapleTrade;
import net.sf.odinms.server.constants.InventoryConstants;
import net.sf.odinms.server.maps.MapleMapObject;
import net.sf.odinms.server.maps.MapleMapObjectType;
import net.sf.odinms.server.miniGame.MapleRPSGame;
import net.sf.odinms.server.playerinteractions.*;
import net.sf.odinms.tools.MaplePacketCreator;
import net.sf.odinms.tools.data.input.SeekableLittleEndianAccessor;

/**
 *
 * @author Matze
 */
public class PlayerInteractionHandler extends AbstractMaplePacketHandler {

    public enum Action {

        CREATE(0x07),//创建0-6
        ENTER_RESPOSE(0X10),
        INVITE(0x0C),//邀请2-11
        DECLINE(0x0D),
        VISIT(0x0A),//0x04-09
        CHAT(0x0F),
        EXIT(0x13),//0A
        OPEN(0x11),//0B
        MAINTAIN(0x15),
        SET_ITEMS(0x00),//770e
        SET_MESO(0x01),//0f
        CONFIRM(0x02),//10
        ADD_ITEM(0x16),//13
        BUY(0x17),//14
        REMOVE_ITEM(0x1D), //18
        BAN_PLAYER(0x24),//1B
        ADDTOBANLIST(0X25),
        REMOVEBANLIST(0X26),
        PUT_ITEM(0xFF),//1E
        MERCHANT_BUY(0x21),//1F
        TAKE_ITEM_BACK(0xFF),//23

        MAINTENANCE_OFF(0x1E),//24
        MERCHANT_ORGANIZE(0x1F),//25
        CLOSE_MERCHANT(0x20),
        RPSGameStart(0x45),
        RPSGameSet(0x47),
        RPSGame(0x55),
        RPSGameResult(0x56);
        final byte code;

        private Action(int code) {
            this.code = (byte) code;
        }

        public byte getCode() {
            return code;
        }
    }
    //00 = 输了 01 = 平局 02 = 赢了
//00 = 拳头.  01 = 布 02 = 剪刀

    @Override
    public void handlePacket(SeekableLittleEndianAccessor slea, MapleClient c) {
        byte mode = slea.readByte();
        if (mode == Action.CREATE.getCode()) {
            byte createType = slea.readByte();
            if (createType == 3) {
                MapleRPSGame.StartRPSGame(c.getPlayer());
            } else if (createType == 4) { // trade
                MapleTrade.startTrade(c.getPlayer());
            } else {
                if (c.getPlayer().getChalkboard() != null) {
                    return;
                }
                if (createType == 1 || createType == 2) {
                    String desc = slea.readMapleAsciiString();
                    String pass = null;
                    if (slea.readByte() == 1) {
                        pass = slea.readMapleAsciiString();
                    }
                    int type = slea.readByte();
                    c.getPlayer().getMap().broadcastMessage(MaplePacketCreator.sendInteractionBox(c.getPlayer()));
                } else if (createType == 5 || createType == 6) { // shop
                    if (!c.getPlayer().hasMerchant() && c.getPlayer().tempHasItems()) {
                        c.getPlayer().dropMessage(1, "请先通过弗兰德里取回保管的物品。");
                        return;
                    }
                    if (!c.getPlayer().getMap().getMapObjectsInRange(c.getPlayer().getPosition(), 19500, Arrays.asList(MapleMapObjectType.SHOP, MapleMapObjectType.HIRED_MERCHANT)).isEmpty() || c.getPlayer().getMapId() < 910000001 || c.getPlayer().getMapId() > 910000022) {
                        c.getPlayer().dropMessage(1, "不能在这里开设店铺");
                        return;
                    }
                    String desc = slea.readMapleAsciiString();
                    slea.skip(3);
                    int itemId = slea.readInt();
                    IPlayerInteractionManager shop;
                    if (c.getPlayer().haveItem(itemId, 1, false, true)) {
                        if (createType == 5) {
                            shop = new MaplePlayerShop(c.getPlayer(), itemId, desc);
                        } else {
                            shop = new HiredMerchant(c.getPlayer(), itemId, desc);
                        }
                        c.getPlayer().setInteraction(shop);
                        c.getSession().write(MaplePacketCreator.getInteraction(c.getPlayer(), true));
                    } else {
                        AutobanManager.getInstance().autoban(c, "XSource| Merchant Shop: Attempt to open a shop without the item.");
                    }
                } else {
                    System.out.println("Unhandled PLAYER_INTERACTION packet: " + slea.toString());
                }
            }
        } else if (mode == Action.INVITE.getCode()) {
            int otherPlayer = slea.readInt();
            MapleCharacter otherChar = c.getPlayer().getMap().getCharacterById(otherPlayer);
            MapleTrade.inviteTrade(c.getPlayer(), otherChar);
        } else if (mode == Action.DECLINE.getCode()) {
            MapleCharacter src = c.getPlayer().getMap().getCharacterById(slea.readInt());
            if (src != null && src.getRPSGame() != null) {
                MapleRPSGame.declineRPSGame(c.getPlayer(), src);
            } else if (src != null && src.getTrade() != null) {
                MapleTrade.declineTrade(c.getPlayer(), src);
            }
        } else if (mode == Action.VISIT.getCode()) {
            int oid = slea.readInt();
            MapleCharacter dst = c.getPlayer().getMap().getCharacterById(oid);
            if (dst != null && dst.getRPSGame() != null) {
                MapleRPSGame.visitRPSGame(dst, c.getPlayer());
            } else if (dst != null && dst.getTrade() != null) {
                MapleTrade.visitTrade(dst, c.getPlayer());
            } else {

                MapleMapObject ob = c.getPlayer().getMap().getMapObject(oid);
                if (ob instanceof IPlayerInteractionManager && c.getPlayer().getInteraction() == null) {
                    IPlayerInteractionManager ips = (IPlayerInteractionManager) ob;
                    if (ips.getShopType() == 1) {
                        HiredMerchant merchant = (HiredMerchant) ips;
                        if (merchant.isOwner(c.getPlayer())) {
                            merchant.setOpen(false);
                            //merchant.broadcast(MaplePacketCreator.shopErrorMessage(0x0D, 1), false);
                            merchant.removeAllVisitors((byte) 0x12, (byte) 1);
                            c.getPlayer().setInteraction(ips);
                            c.getSession().write(MaplePacketCreator.getInteraction(c.getPlayer(), false));
                            return;
                        } else if (!merchant.isOpen()) {
                            c.getPlayer().dropMessage(1, "主人正在整理商店物品\r\n请稍后再度光临！");
                            return;
                        } else if (merchant.isBanned(c.getPlayer().getName())) {
                            c.getSession().write(MaplePacketCreator.getMiniBoxfailenter());
                            return;
                        }
                    } else if (ips.getShopType() == 2) {
                        if (((MaplePlayerShop) ips).isBanned(c.getPlayer().getName())) {
                            c.getPlayer().dropMessage(1, "你已经被禁止进入此店铺");
                            return;
                        }
                    }
                    if (ips.getFreeSlot() == -1) {
                        c.getSession().write(MaplePacketCreator.getMiniBoxFull());
                        return;
                    }
                    c.getPlayer().setInteraction(ips);
                    ips.addVisitor(c.getPlayer());
                    c.getSession().write(MaplePacketCreator.getInteraction(c.getPlayer(), false));
                }
            }
        } else if (mode == Action.CHAT.getCode()) { // chat lol
            if (c.getPlayer().getTrade() != null) {
                slea.readInt();
                c.getPlayer().getTrade().chat(slea.readMapleAsciiString());
            } else if (c.getPlayer().getInteraction() != null) {
                IPlayerInteractionManager ips = c.getPlayer().getInteraction();
                slea.readInt();
                String message = slea.readMapleAsciiString();
                CommandProcessor.getInstance().processCommand(c, message); // debug purposes
                ips.broadcast(MaplePacketCreator.shopChat(c.getPlayer().getName() + " : " + message, ips.isOwner(c.getPlayer()) ? 0 : ips.getVisitorSlot(c.getPlayer()) + 1), true);
            }
        } else if (mode == Action.EXIT.getCode()) {
            if (c.getPlayer().getRPSGame() != null) {
                MapleRPSGame.cancelRPSGame(c.getPlayer());
            } else if (c.getPlayer().getTrade() != null) {
                MapleTrade.cancelTrade(c.getPlayer());
            } else {
                IPlayerInteractionManager ips = c.getPlayer().getInteraction();
                if (ips != null) {
                    if (ips.isOwner(c.getPlayer())) {
                        if (ips.getShopType() == 2) {
                            boolean save = false;
                            for (MaplePlayerShopItem items : ips.getItems()) {
                                if (items.getBundles() > 0) {
                                    IItem item = items.getItem();
                                    item.setQuantity(items.getBundles());
                                    if (MapleInventoryManipulator.addFromDrop(c, item)) {
                                        items.setBundles((short) 0);
                                    } else {
                                        save = true;
                                        break;
                                    }
                                }
                            }
                            ips.removeAllVisitors(3, 1);
                            ips.closeShop(save);
                        } else if (ips.getShopType() == 1) {
                            c.getSession().write(MaplePacketCreator.shopVisitorLeave(0));
                        } else if (ips.getShopType() == 3 || ips.getShopType() == 4) {
                            ips.removeAllVisitors(3, 1);
                        }
                    } else {
                        c.getPlayer().setInteraction(null);
                        ips.removeVisitor(c.getPlayer());
                    }
                }
            }
        } else if (mode == Action.OPEN.getCode()) {
            IPlayerInteractionManager shop = c.getPlayer().getInteraction();
            if (shop != null && shop.isOwner(c.getPlayer())) {
                c.getPlayer().getMap().addMapObject((PlayerInteractionManager) shop);
                if (shop.getShopType() == 1) {
                    HiredMerchant merchant = (HiredMerchant) shop;
                    merchant.setOpen(true);
                    c.getChannelServer().getCim().add(merchant.getOwnerId(), merchant);
                    c.getPlayer().getMap().broadcastMessage(MaplePacketCreator.spawnHiredMerchant(merchant));
                    c.getPlayer().setInteraction(null);
                } else if (shop.getShopType() == 2) {
                    c.getChannelServer().getCim().add(((PlayerInteractionManager) shop).getOwnerId(), (PlayerInteractionManager) shop);
                    c.getPlayer().getMap().broadcastMessage(MaplePacketCreator.sendInteractionBox(c.getPlayer()));
                }
                slea.readByte();
            }
        } else if (mode == Action.SET_MESO.getCode()) {
            c.getPlayer().getTrade().setMeso(slea.readLong());
        } else if (mode == Action.SET_ITEMS.getCode()) {
            MapleItemInformationProvider ii = MapleItemInformationProvider.getInstance();
            MapleInventoryType ivType = MapleInventoryType.getByType(slea.readByte());
            IItem item = c.getPlayer().getInventory(ivType).getItem((byte) slea.readShort());
            long checkq = slea.readShort();
            short quantity = (short) checkq;
            byte targetSlot = slea.readByte();
            if (c.getPlayer().getTrade() != null && item != null) {
                if (checkq > 4000) {
                    AutobanManager.getInstance().autoban(c, "XSource| PE while in trade.");
                }
                if ((quantity <= item.getQuantity() && quantity >= 0) || ii.isThrowingStar(item.getItemId()) || ii.isBullet(item.getItemId())) {
                    if (!c.getChannelServer().allowUndroppablesDrop() && ii.isDropRestricted(item.getItemId())) { // ensure that undroppable items do not make it to the trade window
                        c.getSession().write(MaplePacketCreator.enableActions());
                        return;
                    }
                    IItem tradeItem = item.copy();
                    if (ii.isThrowingStar(item.getItemId()) || ii.isBullet(item.getItemId())) {
                        tradeItem.setQuantity(item.getQuantity());
                        MapleInventoryManipulator.removeFromSlot(c, ivType, item.getPosition(), item.getQuantity(), true);
                    } else {
                        tradeItem.setQuantity(quantity);
                        MapleInventoryManipulator.removeFromSlot(c, ivType, item.getPosition(), quantity, true);
                    }
                    tradeItem.setPosition(targetSlot);
                    c.getPlayer().getTrade().addItem(tradeItem);
                }
            }
        } else if (mode == Action.CONFIRM.getCode()) {
            MapleTrade.completeTrade(c.getPlayer());
        } else if (mode == Action.ADD_ITEM.getCode() || mode == Action.PUT_ITEM.getCode()) {
            MapleInventoryType type = MapleInventoryType.getByType(slea.readByte());
            byte slot = (byte) slea.readShort();
            short bundles = slea.readShort();
            short perBundle = slea.readShort();
            long price = slea.readLong();
            IItem ivItem = c.getPlayer().getInventory(type).getItem(slot);
            IPlayerInteractionManager shop = c.getPlayer().getInteraction();
            long checkquantity = bundles * perBundle;
            int checkiquantity = bundles * perBundle;
            short checksmquantity = (short) (bundles * perBundle);
            if (shop != null && shop.isOwner(c.getPlayer())) {
                if (ivItem != null && ivItem.getQuantity() >= bundles * perBundle) {
                    IItem sellItem = ivItem.copy();
                    sellItem.setQuantity(perBundle);
                    MaplePlayerShopItem item = new MaplePlayerShopItem(sellItem, bundles, price);
                    if (price < 0) {
                        AutobanManager.getInstance().autoban(c, "销售物品出现负数价格.封包被修改.");
                        return;
                    }
                    if (ivItem.HasFlag(InventoryConstants.Items.Flags.锁定)
                            || MapleItemInformationProvider.getInstance().isCash(ivItem.getItemId())) {
                        c.getPlayer().gainWarning(true);
                        c.getSession().write(MaplePacketCreator.enableActions());
                        return;
                    }
                    if (bundles <= 0 || perBundle <= 0 || checkquantity > 20000 || checksmquantity < 0 || checkiquantity < 0 || checkiquantity > 20000) {
                        AutobanManager.getInstance().autoban(c, "异常物品销售: " + sellItem.getItemId());
                        return;
                    }
                    if (bundles > MapleItemInformationProvider.getInstance().getSlotMax(c, sellItem.getItemId()) || perBundle > 4000) {
                        c.getPlayer().弹窗("不允许添加的物品。");
                        c.getSession().write(MaplePacketCreator.enableActions());
                        return;//添加物品组数？
                    }
                    MapleItemInformationProvider ii = MapleItemInformationProvider.getInstance();
                    if (ii.isThrowingStar(ivItem.getItemId()) || ii.isBullet(ivItem.getItemId())) {
                        MapleInventoryManipulator.removeFromSlot(c, type, slot, ivItem.getQuantity(), true);
                    } else {
                        MapleInventoryManipulator.removeFromSlot(c, type, slot, (short) (bundles * perBundle), true);
                    }
                    shop.addItem(item);
                    c.getSession().write(MaplePacketCreator.shopItemUpdate(shop));
                }
            }
        } else if (mode == Action.BUY.getCode() || mode == Action.MERCHANT_BUY.getCode()) {
            int item = slea.readByte();
            short quantity = slea.readShort();
            IPlayerInteractionManager shop = c.getPlayer().getInteraction();
            shop.buy(c, item, quantity);
            shop.broadcast(MaplePacketCreator.shopItemUpdate(shop), true);
        } else if (mode == Action.TAKE_ITEM_BACK.getCode() || mode == Action.REMOVE_ITEM.getCode()) {
            slea.skip(1);
            int slot = slea.readShort();
            IPlayerInteractionManager shop = c.getPlayer().getInteraction();
            if (shop != null) {
                shop.getCurrentLock().lock();
                try {
                    if (shop.isOwner(c.getPlayer())
                            && shop.getItems().size() > slot) {
                        MaplePlayerShopItem item = shop.getItems().get(slot);
                        IItem t = item.getItem().copy();
                        t.setQuantity((short) (item.getBundles() * item.getItem().getQuantity()));
                        if (item.getBundles() > 0
                                && MapleInventoryManipulator.checkSpace(c, t.getItemId(), item.getBundles(), t.getOwner())
                                && MapleInventoryManipulator.addFromDrop(c, t)) {
                            shop.removeFromSlot(slot);
                            c.getSession().write(MaplePacketCreator.shopItemUpdate(shop));
                        } else {
                            c.getSession().write(MaplePacketCreator.enableActions());
                        }
                    }
                } finally {
                    shop.getCurrentLock().unlock();
                }
            }
        } else if (mode == Action.CLOSE_MERCHANT.getCode()) {
            IPlayerInteractionManager merchant = c.getPlayer().getInteraction();
            if (merchant != null) {
                merchant.getCurrentLock().lock();
                try {
                    if (merchant.getShopType() == 1
                            && merchant.isOwner(c.getPlayer())
                            && !merchant.IsClose()) {
                        boolean save = false;
                        for (MaplePlayerShopItem items : merchant.getItems()) {
                            if (items.getBundles() > 0) {
                                IItem item = items.getItem();
                                item.setQuantity(items.getBundles());
                                if (MapleInventoryManipulator.checkSpace(c, item.getItemId(), item.getQuantity(), item.getOwner())
                                        && MapleInventoryManipulator.addFromDrop(c, item)) {
                                    items.setBundles((short) 0);
                                } else {
                                    save = true;
                                    break;
                                }
                            }
                        }
                        c.getSession().write(MaplePacketCreator.closeshop());
                        c.getSession().write(MaplePacketCreator.shopErrorMessage(3, 0));
                        merchant.closeShop(save);
                        c.getPlayer().setInteraction(null);
                    } else {
                        c.getPlayer().gainWarning(true);
                    }
                } finally {
                    merchant.getCurrentLock().unlock();
                }
            }
        } else if (mode == Action.MAINTENANCE_OFF.getCode()) {
            HiredMerchant merchant = (HiredMerchant) c.getPlayer().getInteraction();
            if (merchant != null && merchant.isOwner(c.getPlayer())) {
                merchant.setOpen(true);
                merchant.tempItemsUpdate();
                c.getPlayer().setInteraction(null);
            }
        } else if (mode == Action.BAN_PLAYER.getCode()) {
            PlayerInteractionManager imps = (PlayerInteractionManager) c.getPlayer().getInteraction();
            if (slea.available() == 0) {
                c.getSession().write(MaplePacketCreator.getBannedList(imps));
            } else {
                if (imps != null && imps.isOwner(c.getPlayer())) {
                    ((MaplePlayerShop) imps).banPlayer(slea.readMapleAsciiString());
                }
            }
        } else if (mode == Action.ADDTOBANLIST.getCode()) {
            PlayerInteractionManager imps = (PlayerInteractionManager) c.getPlayer().getInteraction();
            String name = slea.readMapleAsciiString();
            if (!imps.BannedList().contains(name)) {
                imps.BannedList().add(name);
            }
        } else if (mode == Action.REMOVEBANLIST.getCode()) {
            PlayerInteractionManager imps = (PlayerInteractionManager) c.getPlayer().getInteraction();
            String name = slea.readMapleAsciiString();
            if (imps.BannedList().contains(name)) {
                imps.BannedList().remove(name);
            }
        } else if (mode == Action.MERCHANT_ORGANIZE.getCode()) {
            PlayerInteractionManager imps = (PlayerInteractionManager) c.getPlayer().getInteraction();
            for (int i = 0; i < imps.getItems().size(); i++) {
                if (imps.getItems().get(i).getBundles() == 0) {
                    imps.removeFromSlot(i);
                }
            }
            imps.tempItemsUpdate();
            c.getSession().write(MaplePacketCreator.shopItemUpdate(imps));
        } else if (mode == Action.MAINTAIN.getCode()) {
            slea.skip(5);
            int oid = slea.readInt();
            MapleMapObject ob = c.getPlayer().getMap().getMapObject(oid);
            if (ob instanceof IPlayerInteractionManager && c.getPlayer().getInteraction() == null) {
                IPlayerInteractionManager ips = (IPlayerInteractionManager) ob;
                if (ips.getShopType() == 1) {
                    HiredMerchant merchant = (HiredMerchant) ips;
                    if (merchant.isOwner(c.getPlayer())) {
                        merchant.setOpen(false);
                        //merchant.broadcast(MaplePacketCreator.shopErrorMessage(0x0D, 1), false);
                        merchant.removeAllVisitors((byte) 0x12, (byte) 1);
                        c.getPlayer().setInteraction(ips);
                        c.getSession().write(MaplePacketCreator.getInteraction(c.getPlayer(), false));
                    } else if (!merchant.isOpen()) {
                        c.getPlayer().dropMessage(1, "主人正在整理商店物品\r\n请稍后再度光临！");
                    }
                }
            }
        } else if (mode == Action.RPSGame.getCode()) {
            int cid = slea.readInt();
            MapleCharacter otherChar = c.getPlayer().getMap().getCharacterById(cid);
            MapleRPSGame.inviteRPSGame(c.getPlayer(), otherChar);
        } else if (mode == Action.RPSGameStart.getCode()) {
            if (c.getPlayer().getRPSGame() != null) {
                c.SendPacket(MaplePacketCreator.getRPSGameStart());
            }
        } else if (mode == Action.RPSGameSet.getCode()) {
            if (c.getPlayer().getRPSGame() != null) {
                c.getPlayer().getRPSGame().setSelect(slea.readByte());
            }
        }

    }
}
