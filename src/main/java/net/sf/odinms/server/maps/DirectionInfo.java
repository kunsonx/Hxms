/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package net.sf.odinms.server.maps;

import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author Admin
 */
public class DirectionInfo {

	private long id;
	public int x, y, key;
	public boolean forcedInput;
	public List<String> eventQ = new ArrayList<String>();

	private DirectionInfo() {
	}

	public DirectionInfo(int key, int x, int y, boolean forcedInput) {
		this.key = key;
		this.x = x;
		this.y = y;
		this.forcedInput = forcedInput;
	}

	public long getId() {
		return id;
	}

	public void setId(long id) {
		this.id = id;
	}

	public int getX() {
		return x;
	}

	public void setX(int x) {
		this.x = x;
	}

	public int getY() {
		return y;
	}

	public void setY(int y) {
		this.y = y;
	}

	public int getKey_() {
		return key;
	}

	public void setKey_(int key) {
		this.key = key;
	}

	public boolean isForcedInput() {
		return forcedInput;
	}

	public void setForcedInput(boolean forcedInput) {
		this.forcedInput = forcedInput;
	}

	public List<String> getEventQ() {
		return eventQ;
	}

	public void setEventQ(List<String> eventQ) {
		this.eventQ = eventQ;
	}
}
