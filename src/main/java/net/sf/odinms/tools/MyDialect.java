/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package net.sf.odinms.tools;

/**
 * org.hibernate.dialect.MySQL5InnoDBDialect
 * 
 * @author Administrator
 */
public class MyDialect extends org.hibernate.dialect.MySQL5InnoDBDialect {

	@Override
	public String getTableTypeString() {
		return " ENGINE=InnoDB DEFAULT CHARACTER SET=utf8";
	}
}
