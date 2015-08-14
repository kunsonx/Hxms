/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package net.sf.odinms.server;

/**
 *
 * @author Administrator
 */
public class CashPackageInfo {

	private long id;
	private int s_n;

	private CashPackageInfo() {
	}

	public CashPackageInfo(int sn) {
		this.s_n = sn;
	}

	public int getSn() {
		return s_n;
	}

	public void setSn(int sn) {
		this.s_n = sn;
	}

	public long getId() {
		return id;
	}

	public void setId(long id) {
		this.id = id;
	}
}
