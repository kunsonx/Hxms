/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package net.sf.odinms.net.channel.www;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import javax.servlet.http.HttpSession;
import net.sf.odinms.client.LoginCrypto;
import net.sf.odinms.client.LoginCryptoLegacy;
import net.sf.odinms.database.DatabaseConnection;
import org.apache.log4j.Logger;

/**
 *
 *
 * @author Admin
 */
public class WebUser implements java.io.Serializable {

    public static final String SESSION_KEY = "WEBUSER";
    private static final Logger log = Logger.getLogger(WebUser.class);
    private int id;
    private String name;
    private String pass;
    private boolean Admin = false;
    private boolean banned = false;
    private Timestamp lastLogin, createDat;
    private byte loggedin;
    private List<WebUserPlayer> players;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPass() {
        return pass;
    }

    public void setPass(String pass) {
        this.pass = pass;
    }

    public boolean isAdmin() {
        return Admin;
    }

    public boolean isBanned() {
        return banned;
    }

    public Timestamp getLastLogin() {
        return lastLogin;
    }

    public Timestamp getCreateDat() {
        return createDat;
    }

    public WebUser() {
    }

    public boolean checkUserPass() {
        return name == null || name.isEmpty() || pass == null || pass.isEmpty();
    }

    /**
     *
     * @return 返回1 用户名或密码错误。 2表示账号被封 0 登录成功
     */
    public String Login(Map session) {
        String result = "1";
        try {
            Connection con = DatabaseConnection.getConnection();
            PreparedStatement ps = con.prepareStatement("SELECT lastlogin,createdat,salt,`password`,banned,gm,id,loggedin FROM accounts WHERE `name` = ?");
            ps.setString(1, name);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                String passhash = rs.getString("password");
                String salt = rs.getString("salt");
                if (LoginCryptoLegacy.isLegacyPassword(passhash) && LoginCryptoLegacy.checkPassword(pass, passhash)
                        || salt == null && LoginCrypto.checkSha1Hash(passhash, pass)
                        || LoginCrypto.checkSaltedSha512Hash(passhash, pass, salt)) {
                    banned = rs.getBoolean(5);
                    if (!banned) {
                        Admin = rs.getInt(6) == 100;
                        lastLogin = rs.getTimestamp(1);
                        createDat = rs.getTimestamp(2);
                        id = rs.getInt(7);
                        loggedin = rs.getByte(8);
                        result = getDes();
                        session.put(SESSION_KEY, this);
                    } else {
                        result = "2";
                    }
                }
            }
            rs.close();
            ps.close();
            con.close();
        } catch (Exception e) {
            log.error("用户登录错误：", e);
        }
        return result;
    }

    public String getDes() {
        if (Admin) {
            return "网站管理员";
        } else {
            return "岛民";
        }
    }

    public String getOnline() {
        if (loggedin == 0) {
            return "不在线";
        } else {
            return "在线";
        }
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public byte getLoggedin() {
        return loggedin;
    }

    public void setLoggedin(byte loggedin) {
        this.loggedin = loggedin;
    }

    public List<WebUserPlayer> getPlayers() {
        if (players == null) {
            players = new ArrayList<WebUserPlayer>();
            try {
                Connection con = DatabaseConnection.getConnection();
                PreparedStatement ps = con.prepareStatement("SELECT id,`name`, vip, str, dex, luk, `int`, `level`, meso, gender, fame,createdate,reborns FROM characters WHERE accountid = ?");
                ps.setInt(1, id);
                ResultSet rs = ps.executeQuery();
                while (rs.next()) {
                    WebUserPlayer player = new WebUserPlayer();
                    player.setId(rs.getInt("id"));
                    player.setName(rs.getString("name"));
                    player.setVip(rs.getInt("vip"));
                    player.setStr(rs.getInt("str"));
                    player.setDex(rs.getInt("dex"));
                    player.setLuk(rs.getInt("luk"));
                    player.setInt_(rs.getInt("int"));
                    player.setLevel(rs.getInt("level"));
                    player.setMeso(rs.getLong("meso"));
                    player.setGender(rs.getInt("gender"));
                    player.setFame(rs.getInt("fame"));
                    player.setCreatedata(rs.getTimestamp("createdate"));
                    player.setReborns(rs.getInt("reborns"));
                    players.add(player);
                }
                rs.close();
                ps.close();
                con.close();
            } catch (Exception e) {
                log.error("读取角色信息错误：", e);
            }
        }
        return players;
    }

    public void setPlayers(List<WebUserPlayer> players) {
        this.players = players;
    }
}
