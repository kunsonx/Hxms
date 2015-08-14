/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package net.sf.odinms.client;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import net.sf.odinms.database.DatabaseConnection;
import net.sf.odinms.tools.data.output.MaplePacketLittleEndianWriter;
import org.apache.log4j.Logger;
import org.hibernate.Session;
import org.hibernate.Transaction;

/**
 * 玩家通用储存器 游戏内容以 p_ 开始 脚本内容以 s_ 开始
 *
 * @author HXMS
 */
public class MapleCharAttribute {

    private static Logger log = Logger.getLogger(MapleCharAttribute.class);
    public static final String KUANGLONGKEYSET = "p_cmd";
    public static final String KUANGLONGCOMBO = "p_kl_combo";
    private int id;
    private Map<String, String> attribute;
    private MapleCharacter player;
    private Map<String, Object> playerdata = new HashMap<String, Object>();

    public MapleCharAttribute() {
    }

    public MapleCharAttribute(int id) {
        this.id = id;
    }

    public static MapleCharAttribute loadFromDatabase(int id) {
        MapleCharAttribute ret = null;
        try {
            Session session = DatabaseConnection.getSession();
            ret = (MapleCharAttribute) session.get(MapleCharAttribute.class, id);
            session.clear();
            session.close();
        } catch (Exception e) {
            log.error("获得角色属性信息失败！", e);
        }
        if (ret == null) {
            ret = new MapleCharAttribute();
            ret.setId(id);
            ret.setAttribute(new HashMap<String, String>());
            ret.getAttribute().put("p_createAtt", GameConstants.getFormatter().format(new java.util.Date()));
        }
        return ret;
    }

    public static MapleCharAttribute createCharAttribute(int id) {
        MapleCharAttribute ret = new MapleCharAttribute();
        ret.setId(id);
        ret.setAttribute(new HashMap<String, String>());
        ret.getAttribute().put("p_createAttAndChar", GameConstants.getFormatter().format(new java.util.Date()));
        try {
            Session session = DatabaseConnection.getSession();
            Transaction t = session.beginTransaction();
            session.saveOrUpdate(ret);
            t.commit();
            session.close();
            log.info("完成对新角色创建信息系统：" + id);
        } catch (Exception e) {
            log.error("创建角色属性信息失败！", e);
        }
        return ret;
    }

    public void saveToDb() {
        try {
            player.getDatabaseLock().lock();
            Session session = DatabaseConnection.getSession();
            Transaction transaction = session.beginTransaction();
            session.saveOrUpdate(session.merge(this));
            transaction.commit();
            session.close();
        } catch (Exception e) {
            log.error("保存属性角色信息失败！", e);
            //   DatabaseConnection.getDebugInfo();
        } finally {
            player.getDatabaseLock().unlock();
        }
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public Map<String, String> getAttribute() {
        return attribute;
    }

    public void setAttribute(Map<String, String> attribute) {
        this.attribute = attribute;
    }

    public void setPlayer(MapleCharacter player) {
        this.player = player;
        updateData();
    }

    public void updateData() {
        if (player.getJob().IsKuanglong()) {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < 3; i++) {
                String v = attribute.get(KUANGLONGKEYSET + i);
                if (v == null) {
                    v = "0";
                }
                sb.append("cmd");
                sb.append(i);
                sb.append("=");
                sb.append(v);
                sb.append(";");
            }
            sb.deleteCharAt(sb.length() - 1);
            playerdata.put(KUANGLONGKEYSET, sb.toString());
        }
    }

    public Map<String, Object> getPlayerdata() {
        return playerdata;
    }

    /**
     * 获得要添加到area 数据集的数据个数
     *
     * @return
     */
    public int getToAreaData() {
        int i = 0;
        for (String string : playerdata.keySet()) {
            if (string.equals(KUANGLONGKEYSET)) {
                i++;
            }
        }
        return i;
    }

    /**
     * 写入 Area 数据段
     *
     * @param mplew
     */
    public void writeAreaData(MaplePacketLittleEndianWriter mplew) {
        for (String s : playerdata.keySet()) {

            /**
             * 狂龙三个快捷键 指令数据
             */
            if (s.equals(KUANGLONGKEYSET)) {
                mplew.writeShort(-12982);
                mplew.writeMapleAsciiString(playerdata.get(KUANGLONGKEYSET).toString());
            }

        }
    }

    public <T> T getDataValue(String key) {
        return (T) playerdata.get(key);
    }

    public <T> void setDataValue(String key, T value) {
        playerdata.put(key, value);
    }

    public void clearAttributes() {
        for (Iterator<Map.Entry<String, String>> it = attribute.entrySet().iterator(); it.hasNext();) {
            Map.Entry<String, String> entry = it.next();
            if (Character.isDigit(entry.getKey().charAt(0)) && !entry.getKey().startsWith(ConstantTable.getCurrentDay())) {
                it.remove();
            }
        }
    }

    public int getAttrDef(String key, int def) {
        if (attribute.containsKey(key)) {
            return Integer.parseInt(attribute.get(key));
        }
        return def;
    }
}
