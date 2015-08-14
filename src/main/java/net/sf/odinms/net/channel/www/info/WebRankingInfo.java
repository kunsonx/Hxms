/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package net.sf.odinms.net.channel.www.info;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import net.sf.odinms.database.DatabaseConnection;
import org.apache.log4j.Logger;

/**
 *
 * @author Administrator
 */
public final class WebRankingInfo implements WebInfo {

    public static final String KEY = "WebRankingInfo";
    private static final Logger log = Logger.getLogger(WebRankingInfo.class);
    private List<WebRanking> reborns = new CopyOnWriteArrayList<WebRanking>();
    private List<WebRanking> fames = new CopyOnWriteArrayList<WebRanking>();
    private List<WebRanking> guilds = new CopyOnWriteArrayList<WebRanking>();

    public WebRankingInfo() {
        load();
    }

    @Override
    public void save() {
    }

    @Override
    public void load() {
        try {
            Connection c = DatabaseConnection.getConnection();

            reborns.clear();
            PreparedStatement ps = c.prepareStatement("SELECT `name`,reborns from characters ORDER BY reborns desc LIMIT 10");
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                reborns.add(new WebRanking(rs.getRow(), rs.getString(1), rs.getInt(2)));
            }
            rs.close();
            ps.close();

            fames.clear();
            ps = c.prepareStatement("SELECT `name`,fame from characters ORDER BY fame desc LIMIT 10");
            rs = ps.executeQuery();
            while (rs.next()) {
                fames.add(new WebRanking(rs.getRow(), rs.getString(1), rs.getInt(2)));
            }
            rs.close();
            ps.close();

            guilds.clear();
            ps = c.prepareStatement("SELECT `name`,gp from guilds ORDER BY GP DESC LIMIT 10");
            rs = ps.executeQuery();
            while (rs.next()) {
                guilds.add(new WebRanking(rs.getRow(), rs.getString(1), rs.getInt(2)));
            }
            rs.close();
            ps.close();

            c.close();
        } catch (Exception e) {
            log.error("读取排名信息错误：", e);
        }
    }

    public List<WebRanking> getReborns() {
        return reborns;
    }

    public List<WebRanking> getFames() {
        return fames;
    }

    public List<WebRanking> getGuilds() {
        return guilds;
    }
}
