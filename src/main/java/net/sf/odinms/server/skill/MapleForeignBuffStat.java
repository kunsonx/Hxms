/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package net.sf.odinms.server.skill;

import java.io.Serializable;
import net.sf.odinms.client.MapleBuffStat;
import net.sf.odinms.tools.data.output.MaplePacketLittleEndianWriter;

/**
 *
 * @author hxms
 */
public abstract class MapleForeignBuffStat implements Serializable {

    static final long serialVersionUID = 9179541993413798759L;
    private MapleBuffStat stat;

    private MapleForeignBuffStat() {
    }

    public MapleForeignBuffStat(MapleBuffStat stat) {
        this.stat = stat;
    }

    public abstract void writePacket(MaplePacketLittleEndianWriter mplew, int value);

    public MapleBuffStat getStat() {
        return stat;
    }
}
