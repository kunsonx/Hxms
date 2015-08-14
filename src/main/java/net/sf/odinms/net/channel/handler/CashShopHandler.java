//购买商城物品
package net.sf.odinms.net.channel.handler;

import java.sql.*;
import net.sf.odinms.client.*;
import net.sf.odinms.database.DatabaseConnection;
import net.sf.odinms.net.AbstractMaplePacketHandler;
import net.sf.odinms.net.channel.ChannelServer;
import net.sf.odinms.net.channel.Setting;
import net.sf.odinms.server.AutobanManager;
import net.sf.odinms.server.CashItemFactory;
import net.sf.odinms.server.CashItemInfo;
import net.sf.odinms.server.MapleItemInformationProvider;
import net.sf.odinms.tools.MaplePacketCreator;
import net.sf.odinms.tools.data.input.SeekableLittleEndianAccessor;

/**
 *
 * @author Acrylic (Terry Han)
 */
public class CashShopHandler extends AbstractMaplePacketHandler {

    private static org.apache.log4j.Logger log = org.apache.log4j.Logger.getLogger(CashShopHandler.class);

    @Override
    public void handlePacket(SeekableLittleEndianAccessor slea, MapleClient c) {
        Setting cashSetting = Setting.getInstance();
        int action = slea.readByte();//购买物品的类型
        int accountId = c.getAccID();
        //log.debug("动作：" + action);
        if (action == 3) { //购买物品089ok
            int useNX = slea.readByte();
            int snCS = slea.readInt();
            if (log.isDebugEnabled()) {
                log.debug("购买：" + snCS + ",USENX:" + useNX);
            }
            //log.debug("购买：" + snCS);
            //CashItemInfo item = CashItemFactory.getItem(snCS);
            CashItemInfo item = CashItemFactory.getItemInSql(snCS);
            if (cashSetting.IsBanedItemId(item.getItemId())) {
                c.getSession().write(MaplePacketCreator.showNXMapleTokens(c.getPlayer()));
                c.getSession().write(MaplePacketCreator.enableActions());
                c.getPlayer().dropMessage(1, "该物品禁止购买.");
                return;
            }
            if (!c.getPlayer().getCSInventory().CheckSpace()) {
                c.getPlayer().dropMessage("商城空间不足。");
                c.getSession().write(MaplePacketCreator.showNXMapleTokens(c.getPlayer()));
                c.getSession().write(MaplePacketCreator.enableActions());
                return;
            }
            if (item.getItemId() == 0) { //Gendri == 2 是全性别适用
                c.getPlayer().dropMessage("数据库里无此物品，请联系Gm添加。");
                c.getSession().write(MaplePacketCreator.showNXMapleTokens(c.getPlayer()));
                c.getSession().write(MaplePacketCreator.enableActions());
                return;
            } else { //性别满足之后再执行别的
                if (item.getGender() < 2) //Gendri == 2 是全性别适用
                {
                    /*
                     * if (item.getGender() != c.getGender()) {
                     * c.getPlayer().dropMessage("经验证, 您的数据异常, 请不要修改Wz");
                     * c.getSession().write(MaplePacketCreator.enableActions());
                     * return; //|| !item.onSale() } else
                     */ if (item.getCount() <= 0 || item.getItemId() <= 0 || item.getPrice() <= 0) {
                        c.getPlayer().dropMessage("经验证, 您的数据异常, 请不要修改Wz");
                        c.getSession().write(MaplePacketCreator.enableActions());
                        c.getSession().write(MaplePacketCreator.showNXMapleTokens(c.getPlayer()));
                        return;
                    }
                }
                if (c.getPlayer().getCSPoints(useNX) >= item.getPrice()) {
                    /*
                     * if ((item.getItemId() >= 5070000 && item.getItemId() <=
                     * 5077000) || (item.getItemId() >= 5390000 &&
                     * item.getItemId() <= 5390006) || (item.getItemId() >=
                     * 5000006 && item.getItemId() <= 5000006) ||
                     * (item.getItemId() >= 5200000 && item.getItemId() <=
                     * 5200000)) { c.getPlayer().dropMessage("此物品已经被管理员封了!!");
                     * c.getSession().write(MaplePacketCreator.enableActions());
                     * return; }
                     */
                    if (item.getPrice() <= 100) {
                        c.getPlayer().dropMessage("此物品不能购买...");
                        c.getSession().write(MaplePacketCreator.enableActions());
                        return;
                    }
                    c.getPlayer().modifyCSPoints(useNX, -item.getPrice());
                } else {
                    c.getSession().write(MaplePacketCreator.enableActions());
                    return;
                }

                if (GameConstants.isPet(item.getItemId())) {
                    MapleCSInventoryItem citem = new MapleCSInventoryItem(-1, item.getItemId(), snCS, (short) item.getCount(), false);
                    int period = 90;
                    citem.setBaseIItem(MaplePet.createPet(item.getItemId(), period));
                    c.getPlayer().getCSInventory().addItem(citem);
                    c.getSession().write(MaplePacketCreator.showBoughtCSItem(c, citem));
                } else {
                    MapleCSInventoryItem citem = new MapleCSInventoryItem(MapleCharacter.getNextUniqueId(), item.getItemId(), snCS, (short) item.getCount(), false);
                    long period = item.getPeriod();
                    Timestamp ExpirationDate;
                    ExpirationDate = new Timestamp(((System.currentTimeMillis() / 1000) + (period * 24 * 60 * 60)) * 1000);
                    if (period == 0) {
                        ExpirationDate = null;
                    }
                    if (item.getItemId() == 5211047 || item.getItemId() == 5360014) {
                        ExpirationDate = new Timestamp(((System.currentTimeMillis() / 1000) + (3 * 60 * 60)) * 1000);
                    }
                    citem.setExpire(ExpirationDate);
                    c.getPlayer().getCSInventory().addItem(citem);
                    c.getSession().write(MaplePacketCreator.showBoughtCSItem(c, citem));
                }
            }
        } else if (action == 4) { //赠送礼物
            int snCS = slea.readInt();
            int type = slea.readByte();
            String recipient = slea.readMapleAsciiString();
            String message = slea.readMapleAsciiString();
            CashItemInfo item = CashItemFactory.getItemInSql(snCS);
            if (c.getPlayer().getCSPoints(type) >= item.getPrice()) {
                if ((item.getItemId() >= 5070000 && item.getItemId() <= 5077000) || (item.getItemId() >= 5390000 && item.getItemId() <= 5390006) || (item.getItemId() >= 5200000 && item.getItemId() <= 5200000)) {
                    c.getPlayer().dropMessage("此物品已经被管理员封了!!");
                    c.getSession().write(MaplePacketCreator.enableActions());
                    return;
                }
                if (MapleCharacter.getAccountIdByName(recipient) != -1) {
                    if (MapleCharacter.getAccountIdByName(recipient) == c.getPlayer().getAccountid()) {
                        c.getSession().write(MaplePacketCreator.showCannotToMe());
                    } else {
                        c.getPlayer().modifyCSPoints(type, -item.getPrice());
                        MapleCSInventoryItem gift = new MapleCSInventoryItem(0, item.getItemId(), snCS, (short) item.getCount(), true);
                        gift.setSender(c.getPlayer().getName());
                        gift.setMessage(message);
                        Timestamp ExpirationDate;
                        if (GameConstants.isPet(gift.getItemId()) || item.getItemId() == 1112906 || item.getItemId() == 1112905) {
                            ExpirationDate = new Timestamp(((System.currentTimeMillis() / 1000) + (90 * 24 * 60 * 60)) * 1000);
                        } else if (item.getItemId() == 5211047 || item.getItemId() == 5360014) {
                            ExpirationDate = new Timestamp(((System.currentTimeMillis() / 1000) + (3 * 60 * 60)) * 1000);
                        } else if (item.getPeriod() != 0) {
                            ExpirationDate = new Timestamp(((System.currentTimeMillis() / 1000) + (item.getPeriod() * 24 * 60 * 60)) * 1000);
                        } else {
                            ExpirationDate = null;
                        }
                        gift.setExpire(ExpirationDate);
                        GiftItem(gift, recipient, c.getPlayer().getName());
                        c.getSession().write(MaplePacketCreator.getGiftFinish(c.getPlayer().getName(), item.getItemId(), (short) item.getCount()));
                    }
                } else {
                    c.getSession().write(MaplePacketCreator.showCheckName());
                }
            } else {
                c.getSession().write(MaplePacketCreator.enableActions());
                AutobanManager.getInstance().autoban(c, "试图购买现金物品，但是没有足够的点券。");
                return;
            }
        } else if (action == 5) { //删除购物车内的
            try {
                Connection con = DatabaseConnection.getConnection();
                PreparedStatement ps = con.prepareStatement("DELETE FROM wishlist WHERE charid = ?");
                ps.setInt(1, c.getPlayer().getId());
                ps.executeUpdate();
                ps.close();
                int i = 10;
                while (i > 0) {
                    int sn = slea.readInt();
                    if (sn != 0) {
                        ps = con.prepareStatement("INSERT INTO wishlist(charid, sn) VALUES(?, ?) ");
                        ps.setInt(1, c.getPlayer().getId());
                        ps.setInt(2, sn);
                        ps.executeUpdate();
                        ps.close();
                    }
                    i--;
                }
                con.close();
            } catch (SQLException se) {
                log.error("Wishlist SQL Error", se);
            }
            c.getSession().write(MaplePacketCreator.updateWishList(c.getPlayer().getId()));
        } else if (action == 6) { //扩充背包.......089ok
            int useNX = slea.readByte();
            byte add = slea.readByte();
            if (add == 0) {
                byte type = slea.readByte();
                MapleInventoryType invtype = MapleInventoryType.getByType(type);
                byte slots = c.getPlayer().getInventory(invtype).getSlots();
                if (c.getPlayer().getCSPoints(useNX) < 600) {
                    c.getSession().write(MaplePacketCreator.showNXMapleTokens(c.getPlayer()));
                    c.getSession().write(MaplePacketCreator.enableActions());
                    return;
                }
                if (slots <= 92) {
                    c.getPlayer().modifyCSPoints(useNX, -600);
                    c.getPlayer().getInventory(invtype).setSlotLimit((byte) (slots + 4));
                    c.getSession().write(MaplePacketCreator.serverNotice(1, "扩充成功."));
                } else {
                    c.getSession().write(MaplePacketCreator.serverNotice(1, "您无法继续进行扩充."));
                }
            } else if (add == 1) {
                int sn = slea.readInt();
                byte type = 1;
                switch (sn) {
                    case 50200018:
                        type = 1;
                        break;
                    case 50200019:
                        type = 2;
                        break;
                    case 50200020:
                        type = 3;
                        break;
                    case 50200021:
                        type = 4;
                        break;
                    case 50200043:
                        type = 5;
                        break;
                }
                MapleInventoryType invtype = MapleInventoryType.getByType(type);
                byte slots = c.getPlayer().getInventory(invtype).getSlots();
                if (c.getPlayer().getCSPoints(useNX) < 1100) {
                    c.getSession().write(MaplePacketCreator.showNXMapleTokens(c.getPlayer()));
                    c.getSession().write(MaplePacketCreator.enableActions());
                    return;
                }
                if (slots <= 86) {
                    c.getPlayer().modifyCSPoints(useNX, -1100);
                    c.getPlayer().getInventory(invtype).setSlotLimit((byte) (slots + 8));
                    c.getSession().write(MaplePacketCreator.serverNotice(1, "扩充成功."));
                } else {
                    c.getSession().write(MaplePacketCreator.serverNotice(1, "您无法继续进行扩充."));
                }
            }
        } else if (action == 7) { //扩充仓库
            c.getSession().write(MaplePacketCreator.serverNotice(1, "现在无法扩充仓库."));
        } else if (action == 0x0E) { //从商城=>背包....089ok
            int uniqueid = slea.readInt(); //csid.. not like we need it anyways
            slea.readInt();//0
            slea.readByte();//0
            byte type = slea.readByte();
            byte unknown = slea.readByte();
            if (c.getPlayer().getCSInventory().getItem(uniqueid) == null) {
                c.getSession().write(MaplePacketCreator.serverNotice(1, "请您重新进入商城！"));
                c.getSession().write(MaplePacketCreator.showNXMapleTokens(c.getPlayer()));
                c.getSession().write(MaplePacketCreator.enableActions());
                return;
            }
            IItem item = c.getPlayer().getCSInventory().getItem(uniqueid).toItem();
            if (item != null) {
                short slot = c.getPlayer().getInventory(MapleItemInformationProvider.getInstance().getInventoryType(item.getItemId())).getNextFreeSlot();
                if (slot == -1) {
                    c.getSession().write(MaplePacketCreator.serverNotice(1, "您的背包已满."));
                } else {
                    c.getPlayer().getInventory(MapleItemInformationProvider.getInstance().getInventoryType(item.getItemId())).addItem(item);
                    c.getPlayer().getCSInventory().removeItem(uniqueid);
                    c.getSession().write(MaplePacketCreator.transferFromCSToInv(item, slot));
                }
            }
        } else if (action == 0x0F) { //从背包=>商城...089ok  0E0F
            int uniqueid = slea.readInt();
            slea.readInt();//0
            slea.readByte(); //1?
            IItem item;
            for (MapleInventory inventory : c.getPlayer().getAllInventories()) {
                item = inventory.findByUniqueId(uniqueid);
                if (item != null && item instanceof Equip
                        && (((Equip) item).getStr() > 0
                        || ((Equip) item).getDex() > 0
                        || ((Equip) item).getInt() > 0
                        || ((Equip) item).getLuk() > 0)) {
                    c.getPlayer().dropMessage("此物品已经被管理员封了!!");
                    c.getSession().write(MaplePacketCreator.enableActions());
                    c.getSession().write(MaplePacketCreator.showNXMapleTokens(c.getPlayer()));
                    return;
                }
                if (item != null) {
                    MapleCSInventoryItem citem = new MapleCSInventoryItem(item.getUniqueid(), item.getItemId(), CashItemFactory.getSnFromId(item.getItemId()), item.getQuantity(), false);
                    citem.setExpire(item.getExpiration());
                    c.getPlayer().getCSInventory().addItem(citem);
                    inventory.removeItem(item.getPosition(), item.getQuantity(), false);
                    c.getSession().write(MaplePacketCreator.transferFromInvToCS(c.getPlayer(), citem));
                    break;
                }
            }
//        } else if (action == 0x23) { //购买任务物品..089ok
//            int snCS = slea.readInt();
//            CashItemInfo item = CashItemFactory.getItem(snCS);
//            if (c.getPlayer().getMeso() >= item.getPrice()) {
//                if((item.getItemId() >= 5070000 && item.getItemId() <= 5077000) || (item.getItemId() >= 5390000 && item.getItemId() <= 5390006)) {
//                    c.getPlayer().dropMessage("此物品已经被管理员封了!!");
//                    c.getSession().write(MaplePacketCreator.enableActions());
//                    return;
//                }
//                c.getPlayer().gainMeso(-item.getPrice(), false);
//                MapleInventoryManipulator.addById(c, item.getItemId(), (short) item.getCount(), "购买了任务物品");
//                MapleInventory etcInventory = c.getPlayer().getInventory(MapleInventoryType.ETC);
//                short slot = etcInventory.findById(item.getItemId()).getPosition();
//                c.getSession().write(MaplePacketCreator.showBoughtCSQuestItem(slot, item.getItemId()));
//            } else {
//                c.getSession().write(MaplePacketCreator.enableActions());
//                return;
//            }
//            return;
        } else if (action == 0x24) { //购买礼包 0x22
            int type = slea.readByte();
            int snCS = slea.readInt();

            if (true) {
                c.getPlayer().dropMessage("礼包禁止购买!!");
                c.getSession().write(MaplePacketCreator.showNXMapleTokens(c.getPlayer()));
                c.getSession().write(MaplePacketCreator.enableActions());

                return;
            }

            CashItemInfo cashPackage = CashItemFactory.getItemInSql(snCS);
            if (c.getPlayer().getCSPoints(type) >= cashPackage.getPrice()) {
                if ((cashPackage.getItemId() >= 5070000 && cashPackage.getItemId() <= 5077000) || (cashPackage.getItemId() >= 5390000 && cashPackage.getItemId() <= 5390006)) {
                    c.getPlayer().dropMessage("此物品已经被管理员封了!!");
                    c.getSession().write(MaplePacketCreator.enableActions());
                    return;
                }
                c.getPlayer().modifyCSPoints(type, -cashPackage.getPrice());
            } else {
                c.getSession().write(MaplePacketCreator.enableActions());
                return;
            }
            if (!c.getPlayer().getCSInventory().CheckSpace(
                    CashItemFactory.getPackageItemsCount(cashPackage.getItemId()))) {
                c.getPlayer().dropMessage("商城空间不足。");
                c.getSession().write(MaplePacketCreator.showNXMapleTokens(c.getPlayer()));
                c.getSession().write(MaplePacketCreator.enableActions());
                return;
            }
            for (CashItemInfo item : CashItemFactory.getPackageItems(cashPackage.getItemId())) {
                if (cashSetting.IsBanedItemId(item.getItemId())) {
                    continue;
                }
                if (GameConstants.isPet(item.getItemId())) {
                    MapleCSInventoryItem citem = new MapleCSInventoryItem(-1, item.getItemId(), snCS, (short) item.getCount(), false);
                    int period = 90;
                    citem.setBaseIItem(MaplePet.createPet(item.getItemId(), period));
                    c.getPlayer().getCSInventory().addItem(citem);
                    c.getSession().write(MaplePacketCreator.showBoughtCSItem(c, citem));
                } else {
                    MapleCSInventoryItem citem = new MapleCSInventoryItem(MapleCharacter.getNextUniqueId(), item.getItemId(), snCS, (short) item.getCount(), false);
                    long period = item.getPeriod();
                    Timestamp ExpirationDate;
                    ExpirationDate = new Timestamp(((System.currentTimeMillis() / 1000) + (period * 24 * 60 * 60)) * 1000);
                    if (period == 0) {
                        ExpirationDate = null;
                    }
                    if (item.getItemId() == 5211047 || item.getItemId() == 5360014) {//双倍经验值卡三小时权
                        ExpirationDate = new Timestamp(((System.currentTimeMillis() / 1000) + (3 * 60 * 60)) * 1000);
                    }
                    if (item.getItemId() == 1112906 && item.getItemId() == 1112905) {//心心祝福戒指
                        ExpirationDate = new Timestamp(((System.currentTimeMillis() / 1000) + (period * 24 * 60 * 60)) * 1000);
                    }
                    citem.setExpire(ExpirationDate);
                    c.getPlayer().getCSInventory().addItem(citem);
                    c.getSession().write(MaplePacketCreator.showBoughtCSItem(c, citem));
                }
            }
        } else if (action == 0x22) { //购买结婚戒指...............089ok
            if (!c.getPlayer().getCSInventory().CheckSpace()) {
                c.getPlayer().dropMessage("商城空间不足。");
                c.getSession().write(MaplePacketCreator.showNXMapleTokens(c.getPlayer()));
                c.getSession().write(MaplePacketCreator.enableActions());
                return;
            }
            slea.readMapleAsciiString();
            int snCS = slea.readInt();
            String recipient = slea.readMapleAsciiString();
            String message = slea.readMapleAsciiString();
            CashItemInfo item = CashItemFactory.getItemInSql(snCS);
            if (c.getPlayer().getCSPoints(0) >= item.getPrice()) {
                MapleCharacter pchr = c.getChannelServer().getPlayerStorage().getCharacterByName(recipient);
                if (pchr != null) {
                    if (!pchr.getCSInventory().CheckSpace()) {
                        c.getPlayer().dropMessage("对方商城空间不足。");
                        c.getSession().write(MaplePacketCreator.showNXMapleTokens(c.getPlayer()));
                        c.getSession().write(MaplePacketCreator.enableActions());
                        return;
                    }
                    int pid = pchr.getId();
                    MapleRing ring = new MapleRing(item.getItemId(), 0, pid, recipient);
                    if (ring.恋人戒指() && (c.getPlayer().getGender() == pchr.getGender())) {//同性别
                        c.getSession().write(MaplePacketCreator.showCheckPartner());
                        return;
                    }
                    c.getPlayer().modifyCSPoints(0, -item.getPrice());
                    MapleCSInventoryItem gift = new MapleCSInventoryItem(MapleCharacter.getNextUniqueId(), item.getItemId(), snCS, (short) item.getCount(), true);
                    ring.setPartnerUniqueId(gift.getUniqueId());
                    MapleCSInventoryItem citem = new MapleCSInventoryItem(MapleCharacter.getNextUniqueId(), item.getItemId(), snCS, (short) item.getCount(), false);
                    ring.setUniqueId(citem.getUniqueId());
                    citem.setBaseIItem(ring);
                    gift.setBaseIItem(new MapleRing(item.getItemId(), citem.getUniqueId(), c.getPlayer().getId(), c.getPlayer().getName()).setRingId(gift.getUniqueId()));
                    gift.setSender(c.getPlayer().getName());
                    gift.setMessage(message);
                    c.getPlayer().getCSInventory().addItem(citem);
                    pchr.saveToDB();
                    c.getPlayer().saveToDB();
                    pchr.getCSInventory().getCSGifts().put(gift.getUniqueId(), gift);
                    c.getSession().write(MaplePacketCreator.showBoughtCSItem(c, citem));
                } else {
                    c.getSession().write(MaplePacketCreator.showCheckPartner());
                }
            } else {
                c.getSession().write(MaplePacketCreator.enableActions());
                return;
            }
        } else if (action == 0x29) { //购买挚友戒指相关.....089ok
            if (!c.getPlayer().getCSInventory().CheckSpace()) {
                c.getPlayer().dropMessage("商城空间不足。");
                c.getSession().write(MaplePacketCreator.showNXMapleTokens(c.getPlayer()));
                c.getSession().write(MaplePacketCreator.enableActions());
                return;
            }
            slea.readMapleAsciiString();
            int snCS = slea.readInt();
            String recipient = slea.readMapleAsciiString();
            String message = slea.readMapleAsciiString();
            CashItemInfo item = CashItemFactory.getItemInSql(snCS);
            if (c.getPlayer().getCSPoints(0) >= item.getPrice()) {
                MapleCharacter pchr = c.getChannelServer().getPlayerStorage().getCharacterByName(recipient);
                if (pchr != null) {
                    if (!pchr.getCSInventory().CheckSpace()) {
                        c.getPlayer().dropMessage("对方商城空间不足。");
                        c.getSession().write(MaplePacketCreator.showNXMapleTokens(c.getPlayer()));
                        c.getSession().write(MaplePacketCreator.enableActions());
                        return;
                    }
                    int pid = pchr.getId();
                    MapleRing ring = new MapleRing(item.getItemId(), 0, pid, recipient);
                    if (ring.恋人戒指() && (c.getPlayer().getGender() == pchr.getGender())) {//同性别
                        c.getSession().write(MaplePacketCreator.showCheckPartner());
                        return;
                    }
                    c.getPlayer().modifyCSPoints(0, -item.getPrice());
                    MapleCSInventoryItem gift = new MapleCSInventoryItem(MapleCharacter.getNextUniqueId(), item.getItemId(), snCS, (short) item.getCount(), true);
                    ring.setPartnerUniqueId(gift.getUniqueId());
                    MapleCSInventoryItem citem = new MapleCSInventoryItem(MapleCharacter.getNextUniqueId(), item.getItemId(), snCS, (short) item.getCount(), false);
                    ring.setUniqueId(citem.getUniqueId());
                    citem.setBaseIItem(ring);
                    gift.setBaseIItem(new MapleRing(item.getItemId(), citem.getUniqueId(), c.getPlayer().getId(), c.getPlayer().getName()).setRingId(gift.getUniqueId()));
                    gift.setSender(c.getPlayer().getName());
                    gift.setMessage(message);
                    c.getPlayer().getCSInventory().addItem(citem);
                    pchr.saveToDB();
                    pchr.getCSInventory().getCSGifts().put(gift.getUniqueId(), gift);
                    c.getSession().write(MaplePacketCreator.showBoughtCSItem(c, citem));
                } else {
                    c.getSession().write(MaplePacketCreator.showCheckPartner());
                }
            } else {
                c.getSession().write(MaplePacketCreator.enableActions());
                return;
            }
        } else if (action == 0x1C) { //换购...........089ok
            /*
             * 1B Type 32 00 00 00 00 00 00 00 //slea.readLong()表示uniqueid
             */
            /*
             * long uniqueid = slea.readLong(); //这里得到的是在csinventory表的uniqueid
             * int itemid = getItemidformUniqueid(uniqueid);
             * //通过uniqueid获得itemid if (itemid == 0) {
             * //log.warn("换购获得的itemid为0");
             * c.getSession().write(MaplePacketCreator.enableActions()); return;
             * } //log.warn("换购获得的itemid为"+itemid+""); int snCS =
             * CashItemFactory.getSnFromId(itemid); //通过通过itemid获得SN
             * CashItemInfo item = CashItemFactory.getItem(snCS); int Money =
             * item.getPrice() / 10 * 3 ; //获得的抵用券价格是原价*0.3
             *
             * c.getPlayer().getCSInventory().removeItem((int)uniqueid);
             * c.getPlayer().getCSInventory().saveToDB();
             * c.getPlayer().modifyCSPoints(1, Money);
             * c.getSession().write(MaplePacketCreator.getCSInventory(c.getPlayer()));
             */
            //c.getSession().write(MaplePacketCreator.serverNotice(1, "现金道具换购成功。\r\n(增加"+Money+"抵用券)"));
            c.getSession().write(MaplePacketCreator.serverNotice(1, "目前没有此系统"));
            c.getSession().write(MaplePacketCreator.enableActions());
        } else if (action == 8) {
            int useNX = slea.readByte();
            if (c.getMaxCharSlot() >= 15) {
                c.getSession().write(MaplePacketCreator.serverNotice(1, "您的人物栏无法继续增加."));
                c.getSession().write(MaplePacketCreator.enableActions());
                return;
            }
            if (c.getPlayer().getCSPoints(useNX) > 5000) {
                c.setMaxCharacters((byte) (c.getMaxCharSlot() + 1));
                c.getSession().write(MaplePacketCreator.serverNotice(1, "成功扩充人物栏."));
                c.getSession().write(MaplePacketCreator.enableActions());
            } else {
                c.getSession().write(MaplePacketCreator.serverNotice(1, "您的点卷不足."));
                c.getSession().write(MaplePacketCreator.enableActions());
            }
        } else if (action == 0x0A) {//项链扩充
            int useNX = slea.readByte();
            int sn = slea.readInt();
            CashItemInfo item = CashItemFactory.getItemInSql(sn);
            if (c.getPlayer().getCSPoints(useNX) >= item.getPrice()) {
                c.getPlayer().modifyCSPoints(useNX, -item.getPrice());
                if (item.getItemId() == 5550001) {//项链扩充（7天权）
                    long time = c.getPlayer().getNecklace_Expansion() == null
                            ? System.currentTimeMillis() : c.getPlayer().getNecklace_Expansion().getTime();
                    time += 1000 * 60 * 60 * 24 * 7;
                    c.getPlayer().setNecklace_Expansion(new Timestamp(time));
                    c.getSession().write(MaplePacketCreator.showNecklace_Expansion(7));
                } else if (item.getItemId() == 5550000) {//项链扩充（30天权）
                    long time = c.getPlayer().getNecklace_Expansion() == null
                            ? System.currentTimeMillis() : c.getPlayer().getNecklace_Expansion().getTime();
                    time += (1000L * 60L * 60L * 24L * 30L);
                    c.getPlayer().setNecklace_Expansion(new Timestamp(time));
                    c.getSession().write(MaplePacketCreator.showNecklace_Expansion(30));
                }
            } else {
                c.getPlayer().弹窗("点卷不足。不能购买。");
            }
        } else {
            //log.debug("未知类型："+action);
        }

        //c.getPlayer().getCSInventory().saveToDB();
//        c.getPlayer().saveToDB(true);
        c.getSession().write(MaplePacketCreator.showNXMapleTokens(c.getPlayer()));
        c.getSession().write(MaplePacketCreator.enableActions());
    }

    /**
     * 向对方赠送物品。
     *
     * @param gift
     * @param recipient
     * @param playername
     * @return
     */
    public static int GiftItem(MapleCSInventoryItem gift, String recipient, String playername) {
        int autokey = -1;
        try {
            int chrid = MapleCharacter.getIdByName(recipient);
            MapleInventoryType type = MapleItemInformationProvider.getInstance().getInventoryType(gift.getItemId());
            Connection con = DatabaseConnection.getConnection();
            if (MapleItemInformationProvider.getInstance().isCash(gift.getItemId()) && gift.getUniqueId() == 0) {
                gift.setUniqueid(MapleCharacter.getNextUniqueId());
            }
            PreparedStatement ps = con.prepareStatement("INSERT INTO items (characterid, itemid, type, quantity, expiredate, uniqueid, gift) VALUES (?, ?, ?, ?, ?, ?, ?)");
            ps.setInt(1, chrid);
            ps.setInt(2, gift.getItemId());
            ps.setInt(3, MapleItemsNameSpaceType.CsInventory.GetDbId(type));
            ps.setInt(4, gift.getQuantity());
            ps.setTimestamp(5, gift.getExpire());
            ps.setLong(6, gift.getUniqueId());
            ps.setBoolean(7, true);
            ps.executeUpdate();
            ResultSet rs = ps.getGeneratedKeys();
            if (rs.next()) {
                autokey = rs.getInt(1);
            } else {
                return autokey;
            }
            ps = con.prepareStatement("INSERT INTO items_cs (items_id, sn, sender, message) VALUES (?, ?, ?, ?)");
            ps.setInt(1, autokey);
            ps.setInt(2, gift.getSn());
            ps.setString(3, playername);
            ps.setString(4, gift.getMessage());
            ps.executeUpdate();

            ps.close();
            con.close();
            rs.close();
        } catch (SQLException se) {
            log.error("Error saving gift to database", se);
        }
        return autokey;
    }

    /**
     * 创建戒指关系。
     *
     * @param item_id 物品编号。
     * @param pu 对方物品UID
     * @param pchrid 对方CHARID
     * @param pn 对方角色名
     */
    public static void CreateRingInfo(int item_id, long pu, int pchrid, String pn) {
        try {
            Connection con = DatabaseConnection.getConnection();
            int t = con.getTransactionIsolation();
            con.setTransactionIsolation(Connection.TRANSACTION_READ_UNCOMMITTED);
            PreparedStatement ps = con.prepareStatement("INSERT INTO items_ring (items_id, partnerUniqueid, partnerChrId, partnerName) VALUES (?, ?, ?, ?)");
            ps.setInt(1, item_id);
            ps.setLong(2, pu);
            ps.setInt(3, pchrid);
            ps.setString(4, pn);
            ps.executeUpdate();
            ps.close();
            con.setTransactionIsolation(t);
            con.close();
        } catch (SQLException se) {
            log.error("Error saving ring to database", se);
        }
    }
}
