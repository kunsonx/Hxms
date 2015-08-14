/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package net.sf.odinms.net.channel.www;

import net.sf.odinms.client.GameConstants;
import net.sf.odinms.net.channel.ChannelServer;
import org.apache.catalina.LifecycleException;
import org.apache.catalina.core.StandardContext;
import org.apache.catalina.startup.Tomcat;
import org.apache.log4j.Logger;

/**
 *
 * @author Admin
 */
public class WebServer {

    private static final Logger log = Logger.getLogger(WebServer.class);
    Tomcat tomcat = new Tomcat();
    private String servername;
    private boolean use;
    private int port;

    public WebServer() {
        try {
            tomcat.setBaseDir(System.getProperty("user.dir") + "/webapps");
            log.info("初始化工作目录:" + System.getProperty("user.dir") + "/webapps");
        } catch (Exception ex) {
            log.error(ex);
        }
    }

    public void start(boolean is64) {
        try {
            if (!is64) {
                log.info("您的操作系统 不是 64位，不允许使用网站系统。");
                return;
            }
            /*if (true) {
             tomcat.setPort(port);
             StandardContext sc = (StandardContext) tomcat.addWebapp("", "D:\\097\\WebServer\\web");
             tomcat.start();
             return;
             }*/
            if (use) {
                tomcat.setPort(port);
                StandardContext sc = (StandardContext) tomcat.addWebapp("", System.getProperty("user.dir") + "/WebServer.war");
                tomcat.start();
            }
        } catch (Exception ex) {
            log.error("启动TOMCAT错误：", ex);
        }
    }

    public void stop() {
        try {
            if (use) {
                tomcat.stop();
            }
        } catch (LifecycleException ex) {
            log.error("关闭TOMCAT错误：", ex);
        }
    }

    public Tomcat getTomcat() {
        return tomcat;
    }

    public void setTomcat(Tomcat tomcat) {
        this.tomcat = tomcat;
    }

    public String getServername() {
        return servername;
    }

    public void setServername(String servername) {
        this.servername = servername;
    }

    public static WebServer getWebServer() {
        return ChannelServer.getWebServer();
    }

    public boolean isUse() {
        return use;
    }

    public void setUse(boolean use) {
        this.use = use;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }
}
