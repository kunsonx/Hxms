/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package net.sf.odinms.server.skill;

import net.sf.odinms.client.MapleBuffStat;
import net.sf.odinms.tools.data.output.MaplePacketLittleEndianWriter;

/**
 *
 * @author Administrator
 */
public class MapleForeignBuffFixedShort extends MapleForeignBuffStat {

	private short value;

	public MapleForeignBuffFixedShort(MapleBuffStat stat, short _value) {
		super(stat);
		this.value = _value;
	}

	@Override
	public void writePacket(MaplePacketLittleEndianWriter mplew, int value) {
		mplew.writeShort(this.value);
	}
}
