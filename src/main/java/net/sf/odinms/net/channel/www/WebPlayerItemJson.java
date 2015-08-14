/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package net.sf.odinms.net.channel.www;

/**
 *
 * @author Administrator
 */
public class WebPlayerItemJson {

	private int icon;
	private String name;
	private int quantity;
	private String position;

	public WebPlayerItemJson() {
	}

	public WebPlayerItemJson(int icon, String name, int count, String position) {
		this.icon = icon;
		this.name = name;
		this.quantity = count;
		this.position = position;
	}

	public int getIcon() {
		return icon;
	}

	public void setIcon(int icon) {
		this.icon = icon;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public int getQuantity() {
		return quantity;
	}

	public void setQuantity(int quantity) {
		this.quantity = quantity;
	}

	public String getPosition() {
		return position;
	}

	public void setPosition(String position) {
		this.position = position;
	}
}
