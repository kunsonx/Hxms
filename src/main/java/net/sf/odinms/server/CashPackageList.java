/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package net.sf.odinms.server;

import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author Administrator
 */
public class CashPackageList {

	private int packageid;
	private List<CashPackageInfo> sns = new ArrayList<CashPackageInfo>();

	private CashPackageList() {
	}

	public CashPackageList(int packageid) {
		this.packageid = packageid;
	}

	public int getPackageid() {
		return packageid;
	}

	public void setPackageid(int packageid) {
		this.packageid = packageid;
	}

	public List<CashPackageInfo> getSns() {
		return sns;
	}

	public void setSns(List<CashPackageInfo> sns) {
		this.sns = sns;
	}

	public List<CashItemInfo> getItems() {
		List<CashItemInfo> list = new ArrayList<CashItemInfo>();
		for (CashPackageInfo cashPackageInfo : sns) {
			list.add(CashItemFactory.getItemInSql(cashPackageInfo.getSn()));
		}
		return list;
	}
}
