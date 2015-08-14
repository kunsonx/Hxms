/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package net.sf.odinms.net.login.gateway;

import java.util.Map;
import net.sf.odinms.tools.Pair;
import net.sf.odinms.tools.data.output.MaplePacketLittleEndianWriter;
import org.apache.mina.core.buffer.IoBuffer;

/**
 *
 * @author hxms
 */
public class LoginGatewayPacket {

    private static LoginGateway gateway;

    public static void setLoginGatewayPacket(LoginGateway gateway) {
        LoginGatewayPacket.gateway = gateway;
    }

    public static IoBuffer getPing() {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(0);
        return mplew.getBuffer();
    }

    public static IoBuffer getHello() {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(0x01);
        mplew.writeUTF8String(new Double(gateway.getVersion()).toString());
        mplew.writeUTF8String(gateway.getUrl());
        mplew.writeUTF8String(gateway.getCzurl());
        mplew.writeUTF8String(gateway.getNews());
        mplew.writeUTF8String(gateway.getUpdateUrl());
        mplew.writeInt(gateway.getLoginServerPort());
        mplew.writeShort(gateway.getVcode().size());
        for (Pair<String, String> pair : gateway.getVcode()) {
            mplew.writeUTF8String(pair.getLeft());
            mplew.writeUTF8String(pair.getRight());
        }
        mplew.writeShort(gateway.getBan_window().size());
        for (String s : gateway.getBan_window()) {
            mplew.writeUTF8String(s);
        }
        mplew.writeShort(gateway.getServerPoint().size());
        for (Map.Entry<String, String> entry : gateway.getServerPoint().entrySet()) {
            mplew.writeUTF8String(entry.getKey());
            mplew.writeUTF8String(entry.getValue());
        }
        return mplew.getBuffer();
    }

    public static IoBuffer getRegistryResult(String ret) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(0x02);
        mplew.writeUTF8String(ret);
        return mplew.getBuffer();
    }

    public static IoBuffer getSessionIDResult(long id) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(0x03);
        mplew.writeLong(id);
        return mplew.getBuffer();
    }
}
