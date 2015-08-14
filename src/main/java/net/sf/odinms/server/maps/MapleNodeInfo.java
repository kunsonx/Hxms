/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package net.sf.odinms.server.maps;

import java.util.List;

/**
 *
 * @author Admin
 */
public class MapleNodeInfo {

	private long id;
	private int node;
	private int key_;
	private int attr;
	private int x;
	private int y;
	private int nextNode = -1;
	private List<Integer> edge;

	private MapleNodeInfo() {
	}

	public MapleNodeInfo(int node, int key, int x, int y, int attr,
			List<Integer> edge) {
		this.node = node;
		this.key_ = key;
		this.x = x;
		this.y = y;
		this.attr = attr;
		this.edge = edge;
	}

	public List<Integer> getEdge() {
		return edge;
	}

	public void setEdge(List<Integer> edge) {
		this.edge = edge;
	}

	public int getNode() {
		return node;
	}

	public void setNode(int node) {
		this.node = node;
	}

	public int getKey_() {
		return key_;
	}

	public void setKey_(int key) {
		this.key_ = key;
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

	public int getAttr() {
		return attr;
	}

	public void setAttr(int attr) {
		this.attr = attr;
	}

	public int getNextNode() {
		return nextNode;
	}

	public void setNextNode(int nextNode) {
		this.nextNode = nextNode;
	}

	public long getId() {
		return id;
	}

	public void setId(long id) {
		this.id = id;
	}
}
