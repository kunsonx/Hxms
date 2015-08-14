/*
 This file is part of the OdinMS Maple Story Server
 Copyright (C) 2008 Patrick Huy <patrick.huy@frz.cc> 
 Matthias Butz <matze@odinms.de>
 Jan Christian Meyer <vimes@odinms.de>

 This program is free software: you can redistribute it and/or modify
 it under the terms of the GNU Affero General Public License version 3
 as published by the Free Software Foundation. You may not use, modify
 or distribute this program under any other version of the
 GNU Affero General Public License.

 This program is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU Affero General Public License for more details.

 You should have received a copy of the GNU Affero General Public License
 along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package net.sf.odinms.server.playerinteractions;

import java.nio.channels.Channels;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.concurrent.ScheduledFuture;
import net.sf.odinms.client.IItem;
import net.sf.odinms.client.MapleCharacter;
import net.sf.odinms.client.MapleClient;
import net.sf.odinms.database.DatabaseConnection;
import net.sf.odinms.net.channel.ChannelServer;
import net.sf.odinms.server.AutobanManager;
import net.sf.odinms.server.MapleInventoryManipulator;
import net.sf.odinms.server.TimerManager;
import net.sf.odinms.server.constants.InventoryConstants;
import net.sf.odinms.server.maps.MapleMap;
import net.sf.odinms.server.maps.MapleMapObjectType;
import net.sf.odinms.tools.MaplePacketCreator;

/**
 *
 * @author XoticStory
 */
public class HiredMerchant extends PlayerInteractionManager {

    private boolean open;
    public ScheduledFuture<?> schedule = null;
    private MapleMap map;
    private int itemId;
    private long start;
    private long turnover;

    public HiredMerchant(MapleCharacter owner, int itemId, String desc) {
        super(owner, itemId % 10, desc, 6);
        start = System.currentTimeMillis();
        this.itemId = itemId;
        this.map = owner.getMap();

        this.schedule = TimerManager.getInstance().schedule(new Runnable() {
            @Override
            public void run() {
                HiredMerchant.this.closeShop(true);
            }
        }, 1000 * 60 * 60 * 24);
    }

    @Override
    public byte getShopType() {
        return IPlayerInteractionManager.HIRED_MERCHANT;
    }

    @Override
    public void buy(MapleClient c, int item, short quantity) {
        try {
            getCurrentLock().lock();
            MaplePlayerShopItem pItem = items.get(item);
            if (pItem.getBundles() > 0) {
                synchronized (items) {
                    IItem newItem = pItem.getItem().copy();
                    newItem.setQuantity((short) (quantity * newItem.getQuantity()));
                    if (c.getPlayer().getMeso() >= pItem.getPrice() * quantity) {
                        if (quantity > 0 && pItem.getBundles() >= quantity && pItem.getBundles() > 0) {
                            if (newItem.HasFlag(InventoryConstants.Items.Flags.KARMA)) {
                                newItem.CanceFlag(InventoryConstants.Items.Flags.KARMA);
                            }
                            if (MapleInventoryManipulator.checkSpace(c, newItem.getItemId(), newItem.getQuantity(), newItem.getOwner())
                                    && MapleInventoryManipulator.addFromDrop(c, newItem)) {
                                try {
                                    Connection con = DatabaseConnection.getConnection();
                                    PreparedStatement ps = con.prepareStatement("UPDATE characters SET MerchantMesos = MerchantMesos + ? WHERE id = ?");
                                    ps.setLong(1, pItem.getPrice() * quantity);
                                    ps.setInt(2, getOwnerId());
                                    ps.executeUpdate();
                                    ps.close();
                                    con.close();
                                } catch (SQLException se) {
                                    log.error("给玩家加钱时异常：", se);
                                }
                                turnover += pItem.getPrice() * quantity;
                                c.getPlayer().gainMeso(-pItem.getPrice() * quantity, false);
                                pItem.setBundles((short) (pItem.getBundles() - quantity));
                                tempItemsUpdate();
                            } else {
                                c.getPlayer().dropMessage(1, "背包已满");
                                c.getSession().write(MaplePacketCreator.enableActions());
                            }
                        } else {
                            AutobanManager.getInstance().autoban(c.getPlayer().getClient(), "XSource| Attempted to Merchant dupe.");
                        }
                    } else {
                        c.getPlayer().dropMessage(1, "金币不足");
                        c.getSession().write(MaplePacketCreator.enableActions());
                    }
                }
            }
        } finally {
            getCurrentLock().unlock();
        }
    }

    @Override
    public void closeShop(boolean saveItems) {
        map.removeMapObject(this);
        map.broadcastMessage(MaplePacketCreator.destroyHiredMerchant(getOwnerId()));
        tempItems(saveItems);
        if (saveItems) {
            items.clear();
        }
        isClose = true;
        ChannelServer.getInstance(channel).getCim().remove(getOwnerId());
        if (schedule != null) {
            schedule.cancel(false);
        }
        schedule = null;
    }

    public int getTimeLeft() {
        return (int) ((System.currentTimeMillis() - start) / 1000);
    }

    public boolean isOpen() {
        return this.open;
    }

    public void setOpen(boolean set) {
        this.open = set;
    }

    public MapleMap getMap() {
        return map;
    }

    public int getItemId() {
        return itemId;
    }

    @Override
    public void sendDestroyData(MapleClient client) {
        client.getSession().write(MaplePacketCreator.destroyHiredMerchant(getOwnerId()));
    }

    @Override
    public MapleMapObjectType getType() {
        return MapleMapObjectType.HIRED_MERCHANT;
    }

    @Override
    public void sendSpawnData(MapleClient client) {
        client.getSession().write(MaplePacketCreator.spawnHiredMerchant(this));
    }

    public long getTurnover() {
        return turnover;
    }

    public void setTurnover(long turnover) {
        this.turnover = turnover;
    }
}
