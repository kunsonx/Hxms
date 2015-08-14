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
public class UnknownMovementAction implements LifeMovementFragment {

	private UnknownMovementSerialize serialize;

	public UnknownMovementAction(UnknownMovementSerialize serialize) {
		this.serialize = serialize;
	}

	@Override
	public void serialize(LittleEndianWriter lew) {
		serialize.Serialize(lew);
	}

	@Override
	public Point getPosition() {
		return new Point(0, 0);
	}
}
