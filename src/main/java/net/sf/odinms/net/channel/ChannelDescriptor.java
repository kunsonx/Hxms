/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package net.sf.odinms.net.channel;

/**
 * 频道描述符
 *
 * @author hxms
 */
public class ChannelDescriptor implements java.io.Serializable {

	private int id;
	private int world;
	static final long serialVersionUID = 9179574993413738569L;

	public ChannelDescriptor() {
	}

	public ChannelDescriptor(int id, int world) {
		this.id = id;
		this.world = world;
	}

	/**
	 * 频道编号
	 *
	 * @return
	 */
	public int getId() {
		return id;
	}

	/**
	 * 分区编号
	 *
	 * @return
	 */
	public int getWorld() {
		return world;
	}

	public void setId(int id) {
		this.id = id;
	}

	protected void setWorld(int world) {
		this.world = world;
	}

	@Override
	public int hashCode() {
		int hash = 5;
		hash = 13 * hash + this.id;
		hash = 13 * hash + this.world;
		return hash;
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == null) {
			return false;
		}
		if (getClass() != obj.getClass()) {
			return false;
		}
		final ChannelDescriptor other = (ChannelDescriptor) obj;
		if (this.id != other.id) {
			return false;
		}
		if (this.world != other.world) {
			return false;
		}
		return true;
	}

	@Override
	public String toString() {
		return String.format("世界：%d 频道：%d", world, id);
	}
}
