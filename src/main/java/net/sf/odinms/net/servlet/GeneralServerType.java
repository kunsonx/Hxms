/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package net.sf.odinms.net.servlet;

/**
 * 服务类型
 *
 * @author hxms 架构
 */
public enum GeneralServerType {

	WORLD, LOGIN, CHANNEL;

	public boolean IsWorld() {
		return this == WORLD;
	}

	public boolean IsLogin() {
		return this == LOGIN;
	}

	public boolean IsChannel() {
		return this == CHANNEL;
	}
}
