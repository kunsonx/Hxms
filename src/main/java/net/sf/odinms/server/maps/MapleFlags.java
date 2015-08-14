/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package net.sf.odinms.server.maps;

import net.sf.odinms.tools.Pair;

/**
 *
 * @author Admin
 */
public class MapleFlags extends Pair<String, Integer> {

	private long id;

	private MapleFlags() {
		super(null, 0);
	}

	public MapleFlags(String left, Integer right) {
		super(left, right);
	}

	public long getId() {
		return id;
	}

	public String getLeft_() {
		return super.getLeft();
	}

	public Integer getRight_() {
		return super.getRight();
	}

	public void setId(long id) {
		this.id = id;
	}

	public void setLeft_(String left) {
		this.left = left;
	}

	public void setRight_(Integer right) {
		this.right = right;
	}
}
