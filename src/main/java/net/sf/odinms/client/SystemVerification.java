/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package net.sf.odinms.client;

import com.jacob.activeX.ActiveXComponent;
import com.jacob.com.Dispatch;
import com.jacob.com.EnumVariant;
import com.jacob.com.Variant;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.Socket;
import java.util.Date;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.log4j.Logger;
import org.apache.mina.core.buffer.IoBuffer;
import org.ini4j.Ini;
import org.ini4j.Profile;

/**
 *
 * @author Admin
 */
public class SystemVerification {

    private static final Logger log = Logger.getLogger(SystemVerification.class);
    private ActiveXComponent WmiServer;

    public SystemVerification() {
    }

    public void init() {
        try {
            WmiServer = new ActiveXComponent("WbemScripting.SWbemLocator");
            Variant variantParameters[] = new Variant[4];
            variantParameters[0] = new Variant("");
            variantParameters[1] = new Variant("root\\cimv2");
            variantParameters[2] = new Variant("");
            variantParameters[3] = new Variant("");

            Variant conRet = WmiServer.invoke("ConnectServer", variantParameters);
            WmiServer = new ActiveXComponent(conRet.toDispatch());
        } catch (Exception e) {
            log.error("初始化 WMI 服务失败，请在 服务中 开始 [Windows Management Instrumentation] 服务", e);
        }
    }

    public EnumVariant Query(String query) {
        try {
            Variant vCollection = WmiServer.invoke("ExecQuery", new Variant(query));
            EnumVariant enumVariant = new EnumVariant(vCollection.toDispatch());
            return enumVariant;
        } catch (Exception e) {
            log.error("查询 WMI 失败。", e);
        }
        return null;
    }

    private long getWmiValueHash(String classname, String propertyname) {
        Long result = 1L;
        result *= result.hashCode();
        try {
            EnumVariant enumVariant = Query("select * from " + classname);
            Dispatch item;
            while (enumVariant.hasMoreElements()) {
                Variant nitem = enumVariant.Next();
                if (nitem == null) {
                    continue;
                }
                item = nitem.toDispatch();
                if (item == null) {
                    continue;
                }
                String ret = Dispatch.call(item, propertyname).toString();
                if (ret != null) {
                    result *= ret.hashCode();
                }
            }
        } catch (Exception e) {
            log.error("查询 WMI 失败。", e);
        }
        return result;
    }

    public String GetComputerCode() {
        Long hash = 0L;
        hash += getWmiValueHash("Win32_LogicalDisk", "VolumeSerialNumber");
        hash += getWmiValueHash("Win32_Processor", "Processorid");
        hash += getWmiValueHash("Win32_BIOS", "Name");
        hash += getWmiValueHash("Win32_VideoController", "AdapterRAM");
        hash += getWmiValueHash("Win32_DiskDrive", "Size");
        hash += getWmiValueHash("Win32_BaseBoard", "SerialNumber");
        if (0 > hash) {
            hash *= -1;
        }
        hash *= hash;
        hash *= Runtime.getRuntime().availableProcessors();
        hash *= hash.hashCode();
        return Long.toHexString(hash).toUpperCase();
    }

    public Date getCurrentDate() {
        Date date = null;
        while (true) {
            try {
                Socket client = new Socket("utcnist.colorado.edu", 37);
                InputStream input = client.getInputStream();
                byte[] data = new byte[4];
                input.read(data);
                input.close();
                client.close();

                IoBuffer buffer = IoBuffer.allocate(4, true);
                buffer.put(data);
                buffer.flip();
                Long second = buffer.getUnsignedInt();
                buffer.free();

                Date dd = new Date();
                dd.setTime(second * 1000);
                dd.setYear(dd.getYear() - 70);

                date = dd;
            } catch (Exception e) {
            }
            if (date != null) {
                break;
            }
        }
        return date;
    }

    public void configMysql() {
        log.info("正在检查 MYSQL 配置文件");
        try {
            EnumVariant enumVariant = Query("SELECT * FROM Win32_Service WHERE DisplayName = 'MySql'");
            Dispatch mysqlDispatch;
            if (enumVariant.hasMoreElements()) {
                mysqlDispatch = enumVariant.nextElement().toDispatch();
                Pattern pattern = Pattern.compile("=\"(.+my.ini)\"");
                Matcher matcher = pattern.matcher(Dispatch.call(mysqlDispatch, "PathName").getString());
                if (matcher.find()) {
                    log.info("mysql path:" + matcher.group(1));
                    Ini prefs = new Ini(new File(matcher.group(1)));
                    Profile.Section section = prefs.get("mysqld");
                    if (checkIniFileValue(section)) {
                        prefs.store();
                        log.info("修改 MYSQL 配置文件成功。");
                        log.info("MYSQL StopService :" + Dispatch.call(mysqlDispatch, "StopService").toString());
                        String state = Dispatch.call(mysqlDispatch, "State").toString();
                        while (!state.equals("Stopped")) {
                            log.info("MYSQL :" + state);
                            Thread.sleep(100);
                            enumVariant = Query("SELECT * FROM Win32_Service WHERE DisplayName = 'MySql'");
                            mysqlDispatch = enumVariant.nextElement().toDispatch();
                            state = Dispatch.call(mysqlDispatch, "State").toString();
                        }
                        log.info("MYSQL StartService :" + Dispatch.call(mysqlDispatch, "StartService").toString());
                    }
                }
            }
        } catch (Exception e) {
            log.error("操作 MYSQL 失败：", e);
        }
    }

    public boolean checkIniFileValue(Profile.Section section) {
        boolean change = false;
        String key;
        String value;

        key = "max_connections";
        value = "30000";
        if (checkValue(section, key, value)) {
            change = true;
        }

        key = "innodb_log_buffer_size";
        value = "16M";
        if (checkValue(section, key, value)) {
            change = true;
        }

        key = "innodb_buffer_pool_size";
        value = "1G";
        if (checkValue(section, key, value)) {
            change = true;
        }

        key = "innodb_flush_log_at_trx_commit";
        value = "2";
        if (checkValue(section, key, value)) {
            change = true;
        }

        return change;
    }

    public boolean checkValue(Profile.Section section, String key, String value) {
        String key_value = section.get(key);
        if (!key_value.equals(value)) {
            section.put(key, value);
            return true;
        } else {
            return false;
        }
    }

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) throws IOException {
        // TODO code application logic here
        SystemVerification wmi = new SystemVerification();
        wmi.init();
        /* Date date = wmi.getCurrentDate();
         System.out.println("年:" + date.getYear());
         System.out.println("月:" + date.getMonth());
         System.out.println("日:" + date.getDate());
         System.out.println("时:" + date.getHours());
         System.out.println("分:" + date.getMinutes());
         System.out.println("秒:" + date.getSeconds());
         System.out.println("本地:" + date.toLocaleString());
         */
        //wmi.configMysql();
        System.out.println(wmi.GetComputerCode());
        System.in.read();
    }
}
