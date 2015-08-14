/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package net.sf.odinms.server.skill;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import net.sf.odinms.client.Buffstat;
import net.sf.odinms.client.MapleBuffStat;
import net.sf.odinms.server.MapleStatEffect;
import net.sf.odinms.tools.data.output.MaplePacketLittleEndianWriter;

/**
 * 技能外观管理器
 *
 * @author hxms
 */
public class MapleForeignBuffSkill implements Serializable {

	static final long serialVersionUID = 9179541993413739514L;
	private MapleStatEffect effect;
	private List<MapleForeignBuffStat> stats;

	private MapleForeignBuffSkill() {
	}

	public MapleForeignBuffSkill(MapleStatEffect effect) {
		this.effect = effect;
		this.stats = new ArrayList<MapleForeignBuffStat>();
	}

	public MapleStatEffect getEffect() {
		return effect;
	}

	public List<MapleForeignBuffStat> getStats() {
		return stats;
	}

	public boolean hasStats() {
		synchronized (this) {
			for (MapleForeignBuffStat foreignBuffStat : stats) {
				if (!(foreignBuffStat instanceof MapleForeignBuffNoSkill)) {
					return true;
				}
			}
			return false;
		}
	}

	public void applyStat(MaplePacketLittleEndianWriter mplew,
			MapleBuffStat stat, int value) {
		synchronized (this) {
			for (MapleForeignBuffStat buffStat : stats) {
				if (buffStat.getStat().equals(stat)
						&& (buffStat instanceof MapleForeignBuffNoSkill || !(buffStat instanceof MapleForeignBuffNoStat))) {
					buffStat.writePacket(mplew, value);
					break;
				}
			}
		}
	}

	public boolean hasStat(Buffstat stat) {
		synchronized (this) {
			for (MapleForeignBuffStat buffStat : stats) {
				if (buffStat.getStat().equals(stat)) {
					return true;
				}
			}
			return false;
		}
	}
}
