/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package net.sf.odinms.net.channel.www;

/**
 *
 * @author Admin
 */
public class WebControl {

	public WebControl() {
	}

	public String getHello() {
		return "Hello BlazeDS!";
	}

	public String getServerName() {
		return WebServer.getWebServer().getServername();
	}
}
