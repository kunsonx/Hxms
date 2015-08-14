/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package net.sf.odinms.server.movement;

import java.awt.Point;
import net.sf.odinms.tools.data.output.LittleEndianWriter;

/**
 *
 * @author Administrator
 */
public class RefreshLifeMovement implements LifeMovement {

	private int type, state, Duration;

	public RefreshLifeMovement(int type, int stateI, int Duration) {
		this.type = type;
		this.state = stateI;
		this.Duration = Duration;
	}

	@Override
	public void serialize(LittleEndianWriter lew) {
		lew.write(type);
		lew.write(state);
		lew.writeShort(Duration);
	}

	@Override
	public Point getPosition() {
		return new Point(0, 0);
	}

	@Override
	public int getNewstate() {
		return state;
	}

	@Override
	public int getDuration() {
		return Duration;
	}

	@Override
	public int getType() {
		return type;
	}
}
