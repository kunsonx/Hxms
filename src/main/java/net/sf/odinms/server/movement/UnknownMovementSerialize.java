/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package net.sf.odinms.server.movement;

import net.sf.odinms.tools.data.output.LittleEndianWriter;

/**
 *
 * @author Administrator
 */
public interface UnknownMovementSerialize {

	public void Serialize(LittleEndianWriter lew);
}
