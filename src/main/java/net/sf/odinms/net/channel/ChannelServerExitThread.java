/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package net.sf.odinms.net.channel;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import net.sf.odinms.client.MapleCharacter;
import net.sf.odinms.server.ServerExceptionHandler;
import org.apache.log4j.Logger;

/**
 * 频道服务结束进程
 *
 * @author hxms
 */
public class ChannelServerExitThread extends Thread {

    private static Logger log = Logger.getLogger(ChannelServerExitThread.class);

    @Override
    public void run() {
        try {
            Collection<ChannelServer> ccs = ChannelManager.getAllChannelServers();
            for (ChannelServer chan : ccs) {
                log.info("正在取消当前频道雇佣。");
                chan.getCim().shutdown();
                chan.setShutdown(true);
                log.info("正在保存人物资料，频道： " + chan.getChannel());
                log.info("请不要使用任何异常中断服务端。");
                List<MapleCharacter> chars = new ArrayList<MapleCharacter>(Collections.synchronizedCollection(chan.getPlayerStorage().getAllCharacters()));
                for (int i = 0; i < chars.size(); i++) {
                    MapleCharacter chr = (MapleCharacter) chars.get(i);
                    try {
                        if (chr != null) {
                            log.info("正在为 玩家：" + chr + " 执行断开代码。这是 第 " + (i + 1) + " 个。共 " + chars.size() + " 个。");
                            chr.getClient().disconnect();
                        }
                    } catch (Exception e) {
                        if (chr != null) {
                            log.info("为 玩家：" + chr + " 执行断开代码。发生异常。这是 第 " + (i + 1) + " 个。共 " + chars.size() + " 个。");
                        }
                        ServerExceptionHandler.HandlerException(e);
                        continue;
                    }
                }
            }
            log.info("存档工作已全部完成。如果程序没有退出。请手动退出。");
        } catch (Exception ex) {
            log.error("执行结束代码段失败：", ex);
        }
    }
}
