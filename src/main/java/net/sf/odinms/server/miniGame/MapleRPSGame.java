/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package net.sf.odinms.server.miniGame;

import net.sf.odinms.client.MapleCharacter;
import net.sf.odinms.client.MapleStat;
import net.sf.odinms.server.MapleInteractionRoom;
import net.sf.odinms.tools.MaplePacketCreator;

/**
 *
 * @author hxms
 */
public class MapleRPSGame implements MapleInteractionRoom {

    public static enum SELECT {

        拳头(0),
        布(1),
        剪刀(2),
        没选(-1),;
        private int value;

        private SELECT(int value) {
            this.value = value;
        }

        public int getValue() {
            return value;
        }

        public static SELECT getByValue(int vl) {
            for (SELECT select : values()) {
                if (select.value == vl) {
                    return select;
                }
            }
            return null;
        }
    }
    private MapleRPSGame partner;
    private MapleCharacter chr;
    private int number;
    private SELECT select = SELECT.没选;
    private int win = -1;

    public MapleRPSGame(MapleCharacter chr, int number) {
        this.chr = chr;
        this.number = number;
    }

    public static void StartRPSGame(MapleCharacter c) {
        if (c.getTrade() == null && c.getRPSGame() == null) {
            c.setRPSGame(new MapleRPSGame(c, 0));
            c.getClient().getSession().write(MaplePacketCreator.getTradeStart(c.getClient(), c.getTrade(), (byte) 0, false));
        } else {
            c.getClient().getSession().write(MaplePacketCreator.serverNotice(5, "现在正忙,请稍后再试"));
        }
    }

    public static void cancelRPSGame(MapleCharacter c) {
        if (c.getRPSGame() != null) {
            c.getRPSGame().cancel();
            if (c.getRPSGame().getPartner() != null) {
                c.getRPSGame().getPartner().cancel();
                if (c.getRPSGame().getPartner().getChr() != null) {
                    c.getRPSGame().getPartner().getChr().setRPSGame(null);
                }
            }
            c.setRPSGame(null);
        }
    }

    public static void inviteRPSGame(MapleCharacter src, MapleCharacter dst) {
        /*  if (src == null || src.getRPSGame() == null) {
         return;
         }*/
        if (dst == null) {//假人代码处理
            cancelRPSGame(src);
            src.SendPacket(MaplePacketCreator.enableActions());
            return;
        }

        if (!(src.getFame() >= 0 && dst.getFame() >= 0)) {//整个表达式取反.
            src.getClient().getSession().write(MaplePacketCreator.serverNotice(5, "你或者对方没人气了.不能玩哦."));
            cancelRPSGame(src);
            return;
        }
        if (dst.getRPSGame() == null && dst.getTrade() == null) {

            dst.getClient().getSession().write(MaplePacketCreator.getTradeInvite(src, false));
        } else {
            src.getClient().getSession().write(MaplePacketCreator.serverNotice(5, "正在做别的事情"));
            cancelRPSGame(src);
        }
    }

    public static void visitRPSGame(MapleCharacter src, MapleCharacter dst) {
        //如果对方接收正在处理别的邀请
        if (dst.getRPSGame() != null) {
            src.getClient().getSession().write(MaplePacketCreator.enableActions());
            src.getClient().getSession().write(MaplePacketCreator.serverNotice(5, "正在做别的事情"));
            cancelRPSGame(src);
            return;
        }
        if (src.getRPSGame() != null && dst.getRPSGame() == null) {
            if (src.getMap() != dst.getMap()) {
                src.getClient().getSession().write(MaplePacketCreator.serverNotice(5, "You are not in the same map as the trader."));
                src.getClient().getSession().write(MaplePacketCreator.enableActions());
                dst.getClient().getSession().write(MaplePacketCreator.serverNotice(5, "You are not in the same map as the trader."));
                dst.getClient().getSession().write(MaplePacketCreator.enableActions());
                cancelRPSGame(src);
                return;
            }
            dst.setRPSGame(new MapleRPSGame(dst, 1));

            dst.getRPSGame().setPartner(src.getRPSGame());
            src.getRPSGame().setPartner(dst.getRPSGame());


            src.getClient().getSession().write(MaplePacketCreator.getTradePartnerAdd(dst));
            dst.getClient().getSession().write(MaplePacketCreator.getTradeStart(dst.getClient(), dst.getRPSGame(), (byte) 1, false));
            dst.SendPacket(MaplePacketCreator.getRPSGameStart());
        }
    }

    public static void declineRPSGame(MapleCharacter dstCharacter, MapleCharacter src) {
        if (src != null) {
            src.getClient().getSession().write(
                    MaplePacketCreator.serverNotice(5, dstCharacter.getName() + " 拒绝了你的挑战邀请"));
        }
        cancelRPSGame(src);
    }

    @Override
    public MapleCharacter getChr() {
        return chr;
    }

    @Override
    public MapleRPSGame getPartner() {
        return partner;
    }

    public void setPartner(MapleRPSGame partner) {
        this.partner = partner;
    }

    public void setSelect(int sl) {
        select = SELECT.getByValue(sl);

        if (!getPartner().select.equals(SELECT.没选)) {
        //    System.out.println("该产生结果了");
            doCreateResult();
        }
    }

    public void doCreateResult() {
        SELECT dstSelect = getPartner().select;
        if (select.equals(dstSelect)) {//选的一样
            win = 1;
            getPartner().win = 1;
        } else if (select.equals(SELECT.拳头)) {
            if (dstSelect.equals(SELECT.剪刀)) {
                win = 0;
                getPartner().win = 2;
            } else {
                win = 2;
                getPartner().win = 0;
            }
        } else if (select.equals(SELECT.剪刀)) {
            if (dstSelect.equals(SELECT.布)) {
                win = 0;
                getPartner().win = 2;
            } else {
                win = 2;
                getPartner().win = 0;
            }
        } else if (select.equals(SELECT.布)) {
            if (dstSelect.equals(SELECT.拳头)) {
                win = 0;
                getPartner().win = 2;
            } else {
                win = 2;
                getPartner().win = 0;
            }
        }
        if (win != 1) {
            if (win == 0) {
                DoWin();
                getPartner().DoFail();
            } else {
                DoFail();
                getPartner().DoWin();
            }
        }
        chr.SendPacket(MaplePacketCreator.getRPSGameResult(win, dstSelect));
        getPartner().chr.SendPacket(MaplePacketCreator.getRPSGameResult(getPartner().win, select));
    }

    public SELECT getSelect() {
        return select;
    }

    public int getWin() {
        return win;
    }

    /**
     * 玩赢了
     */
    public void DoWin() {
        chr.addFame(10);
        chr.updateSingleStat(MapleStat.FAME, chr.getFame());
        chr.dropMessage("获得人气 (+10)");
    }

    /**
     * 玩输了
     */
    public void DoFail() {
        chr.addFame(-10);
        chr.updateSingleStat(MapleStat.FAME, chr.getFame());
        chr.dropMessage("人气度减少了 (-10)");
    }

    @Override
    public void cancel() {
        chr.getClient().getSession().write(MaplePacketCreator.getTradeCancel((byte) number));
    }
}
