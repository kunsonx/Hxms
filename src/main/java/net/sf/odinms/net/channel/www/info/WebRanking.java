/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package net.sf.odinms.net.channel.www.info;

/**
 *
 * @author Administrator
 */
public class WebRanking {

	private int no;
	private String name;
	private int count;

	public WebRanking(int no, String name, int count) {
		this.no = no;
		this.name = name;
		this.count = count;
	}

	public int getNo() {
		return no;
	}

	public void setNo(int no) {
		this.no = no;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public int getCount() {
		return count;
	}

	public void setCount(int count) {
		this.count = count;
	}
}
