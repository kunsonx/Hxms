/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package net.sf.odinms.client.messages.commands;

import com.mysql.jdbc.PreparedStatement;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Arrays;
import net.sf.odinms.client.GameConstants;
import net.sf.odinms.client.MapleCharacter;
import net.sf.odinms.client.MapleClient;
import net.sf.odinms.client.MapleStat;
import net.sf.odinms.client.messages.Command;
import net.sf.odinms.client.messages.CommandDefinition;
import net.sf.odinms.client.messages.MessageCallback;
import net.sf.odinms.database.DatabaseConnection;
import net.sf.odinms.net.channel.ChannelServer;
import net.sf.odinms.scripting.npc.NPCScriptManager;
import net.sf.odinms.server.MapleItemInformationProvider;
import net.sf.odinms.server.MaplePortal;
import net.sf.odinms.server.maps.MapleMap;
import net.sf.odinms.tools.MaplePacketCreator;
import net.sf.odinms.tools.Pair;

public class 玩家命令 implements Command {

    private ResultSet ranking(boolean gm) {
        ResultSet rs = null;
        try {
            Connection con = DatabaseConnection.getConnection();
            PreparedStatement ps;
            if (!gm) {
                ps = (PreparedStatement) con.prepareStatement("SELECT reborns, level, name, job FROM characters WHERE gm < 3 ORDER BY reborns desc LIMIT 10");
            } else {
                ps = (PreparedStatement) con.prepareStatement("SELECT name, gm FROM characters WHERE gm >= 3");
            }
            rs = ps.executeQuery();
            ps.close();
            con.close();
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
        return rs;
    }
    MapleItemInformationProvider ii;
    short quantity;
    int petId;

    @Override
    public void execute(MapleClient c, MessageCallback mc, String[] splitted) throws Exception {
        splitted[0] = splitted[0].toLowerCase();
        MapleCharacter player = c.getPlayer();
        ChannelServer cserv = c.getChannelServer();
        if ((splitted[0].equals("@力量") || splitted[0].equals("@智力") || splitted[0].equals("@运气") || splitted[0].equals("@敏捷")) && splitted.length > 1) {
            int amount = Integer.parseInt(splitted[1]);
            boolean str = splitted[0].equals("@力量");
            boolean Int = splitted[0].equals("@智力");
            boolean luk = splitted[0].equals("@运气");
            boolean dex = splitted[0].equals("@敏捷");
            if (amount > 0 && amount <= player.getRemainingAp() && amount <= Short.MAX_VALUE) {
                if (str && (amount + player.getStr()) <= Short.MAX_VALUE) {
                    player.setStr(player.getStr() + amount);
                    player.setRemainingAp(player.getRemainingAp() - amount);
                    player.updateSingleStat(
                            Arrays.asList(
                            new Pair<MapleStat, Number>(MapleStat.STR, player.getStr()),
                            new Pair<MapleStat, Number>(MapleStat.AVAILABLEAP, player.getRemainingAp())));
                } else if (Int && amount + player.getInt() <= Short.MAX_VALUE) {
                    player.setInt(player.getInt() + amount);
                    player.setRemainingAp(player.getRemainingAp() - amount);
                    player.updateSingleStat(
                            Arrays.asList(
                            new Pair<MapleStat, Number>(MapleStat.INT, player.getInt()),
                            new Pair<MapleStat, Number>(MapleStat.AVAILABLEAP, player.getRemainingAp())));
                } else if (luk && amount + player.getLuk() <= Short.MAX_VALUE) {
                    player.setLuk(player.getLuk() + amount);
                    player.setRemainingAp(player.getRemainingAp() - amount);
                    player.updateSingleStat(
                            Arrays.asList(
                            new Pair<MapleStat, Number>(MapleStat.LUK, player.getLuk()),
                            new Pair<MapleStat, Number>(MapleStat.AVAILABLEAP, player.getRemainingAp())));
                } else if (dex && amount + player.getDex() <= Short.MAX_VALUE) {
                    player.setDex(player.getDex() + amount);
                    player.setRemainingAp(player.getRemainingAp() - amount);
                    player.updateSingleStat(
                            Arrays.asList(
                            new Pair<MapleStat, Number>(MapleStat.DEX, player.getDex()),
                            new Pair<MapleStat, Number>(MapleStat.AVAILABLEAP, player.getRemainingAp())));
                } else {
                    mc.dropMessage("请确保你当前的属性不超过" + Short.MAX_VALUE + ".");
                }
            } else {
                mc.dropMessage("请确保你当前的属性不超过" + Short.MAX_VALUE + "，且你有足够的AP分配.");
            }
        } else if (splitted[0].equals("@自由")) {
            if (c.getPlayer().getMapId() >= 910000018 && c.getPlayer().getMapId() <= 910000022 || (c.getPlayer().getEventInstance() != null)) {
                c.getPlayer().message("你目前在任务地图.不能使用该命令");
                c.getSession().write(MaplePacketCreator.enableActions());
            } else {
                MapleMap target = c.getChannelServer().getMapFactory().getMap(910000000);
                MaplePortal targetPortal = target.getPortal(0);
                player.changeMap(target, targetPortal);
            }
        } else if (splitted[0].equalsIgnoreCase("@信息")) {
            mc.dropMessage("你的个人信息:");
            mc.dropMessage("姓名:" + player.getName());
            String xb;
            String jiazu;
            String jieh;
            if (c.getPlayer().getGuildid() <= 0) {
                jiazu = "没有家族";
            } else {
                jiazu = player.getGuild().getName();
            }
            if (c.getPlayer().isMarried() == false) {
                jieh = "还没有开放,请期待";
            } else {
                jieh = "未婚[单身]";
            }
            if (c.getGender() == 0) {
                xb = "男";
            } else {
                xb = "女";
            }
            mc.dropMessage("性别:" + xb);
            mc.dropMessage("所在的家族:" + jiazu);
            mc.dropMessage("婚姻状态:" + jieh);
            mc.dropMessage("====================================");
            mc.dropMessage("您的统计信息:");
            mc.dropMessage("力量: " + player.getStr());
            mc.dropMessage("敏捷: " + player.getDex());
            mc.dropMessage("智力: " + player.getInt());
            mc.dropMessage("运气: " + player.getLuk());
            mc.dropMessage("属性点: " + player.getRemainingAp());
            mc.dropMessage("转生次数: " + player.getReborns());
        } else if (splitted[0].equalsIgnoreCase("@帮助")) {
            mc.dropMessage("============================================================");
            mc.dropMessage("    " + c.getChannelServer().getServerName() + "    玩家命令.");
            mc.dropMessage("============================================================");
            mc.dropMessage("@自由          - 回到自由市场.");
            mc.dropMessage("@假死          - npc假死.");
            mc.dropMessage("@经验          - 修复负经验.");
            mc.dropMessage("@存档          - 保存你在游戏的数据.");
            mc.dropMessage("@信息          - 查看个人信息.");
            mc.dropMessage("@力量          - 添加能力值 .使用方法：@力量 200");
            mc.dropMessage("@智力          - 添加能力值 .使用方法：@智力 200");
            mc.dropMessage("@运气          - 添加能力值 .使用方法：@运气 200");
            mc.dropMessage("@敏捷          - 添加能力值 .使用方法：@敏捷 200");
            mc.dropMessage("               -我们将还会添加更多的指令，请期待更新");
            mc.dropMessage("===============-祝你游戏愉快======================");
        } else if (splitted[0].equalsIgnoreCase("@buynx")) {
            if (splitted.length != 2) {
                mc.dropMessage("正确用法: @buynx <数量>");
                return;
            }
            int nxamount;
            try {
                nxamount = Integer.parseInt(splitted[1]);
            } catch (NumberFormatException asd) {
                return;
            }
            int cost = nxamount * 10000;
            if (nxamount > 0 && nxamount < 420000) {
                if (player.getMeso() >= cost) {
                    player.gainMeso(-cost, true, true, true);
                    player.modifyCSPoints(1, nxamount);
                    mc.dropMessage("你花费了 " + cost + " 冒险币. 购买了 " + nxamount + " 商成点卷.");
                } else {
                    mc.dropMessage("你没有足够的金钱. 1 点卷 等于 10000 冒险币.");
                }
            } else {
                mc.dropMessage("你为什么要这样做?");
            }
        } else if (splitted[0].equalsIgnoreCase("@存档")) {
            player.saveToDB(true);//存档
            mc.dropMessage("存档成功.");
        } else if (splitted[0].equalsIgnoreCase("@经验")) {
            player.setExp(0);
            player.updateSingleStat(MapleStat.EXP, player.getExp());
        } else if (splitted[0].equalsIgnoreCase("@假死")) {
            NPCScriptManager.getInstance().dispose(c);
            mc.dropMessage("恭喜,解救成功.快去试试看吧.");
        } else if (splitted[0].equalsIgnoreCase("@排名")) {
            ResultSet rs = ranking(false);
            mc.dropMessage("前10名的玩家: ");
            int i = 1;
            while (rs.next()) {
                String job; // Should i make it so it shows the actual job ?
                if (rs.getInt("job") >= 400 && rs.getInt("job") <= 422) {
                    job = "飞侠";
                } else if (rs.getInt("job") >= 300 && rs.getInt("job") <= 322) {
                    job = "弓箭手";
                } else if (rs.getInt("job") >= 200 && rs.getInt("job") <= 232) {
                    job = "魔法师";
                } else if (rs.getInt("job") >= 100 && rs.getInt("job") <= 132) {
                    job = "战士";
                } else if (rs.getInt("job") >= 500 && rs.getInt("job") <= 532) {
                    job = "海盗";
                } else if (rs.getInt("job") == 1000) {
                    job = "初心者";
                } else if (rs.getInt("job") >= 1100 && rs.getInt("job") <= 1121) {
                    job = "魂骑士";
                } else if (rs.getInt("job") >= 1200 && rs.getInt("job") <= 1221) {
                    job = "炎术士";
                } else if (rs.getInt("job") >= 1300 && rs.getInt("job") <= 1321) {
                    job = "风灵使者";
                } else if (rs.getInt("job") >= 1400 && rs.getInt("job") <= 1421) {
                    job = "夜行者";
                } else if (rs.getInt("job") >= 1500 && rs.getInt("job") <= 1521) {
                    job = "奇袭者";
                } else if (rs.getInt("job") >= 2000) {
                    job = "战童";
                } else if (rs.getInt("job") >= 2100 && rs.getInt("job") <= 2222) {
                    job = "战神";
                } else {
                    job = "新手";
                }
                String xb1;
                mc.dropMessage(i + ". " + rs.getString("name") + "  ||  职业: " + job + "  ||  转生次数: " + rs.getInt("reborns") + "  ||  等级: " + rs.getInt("level") + "  ||  性别: ");
                i++;
            }
        } else if (splitted[0].equalsIgnoreCase("@npc")) {
            NPCScriptManager.getInstance().dispose(c);
            NPCScriptManager.getInstance().start(c, 9000020, -1);
        }
    }

    @Override
    public CommandDefinition[] getDefinition() {
        return new CommandDefinition[]{
            new CommandDefinition("力量", 0),
            new CommandDefinition("敏捷", 0),
            new CommandDefinition("智力", 0),
            new CommandDefinition("运气", 0),
            new CommandDefinition("信息", 0),
            new CommandDefinition("帮助", 0),
            new CommandDefinition("存档", 0),
            new CommandDefinition("经验", 0),
            new CommandDefinition("自由", 0),
            new CommandDefinition("假死", 0),
            new CommandDefinition("排名", 0), new CommandDefinition("npc", 0)};
    }
}
