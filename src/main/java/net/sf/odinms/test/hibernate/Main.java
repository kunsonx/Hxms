/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package net.sf.odinms.test.hibernate;

import java.util.List;
import net.sf.odinms.client.MapleCharAttribute;
import net.sf.odinms.database.DatabaseConnection;
import org.hibernate.LockMode;
import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.Transaction;

/**
 *
 * @author Administrator
 */
public class Main {

	/**
	 * @param args
	 *            the command line arguments
	 */
	public static void main(String[] args) {
		// TODO code application logic here
		TestMethod();
		TestMethod();
		TestMethod();
		TestMethod();

		DatabaseConnection.getDebugInfo();
	}

	public static void TestMethod() {
		Session session = DatabaseConnection.getSession();
		// Transaction t = session.beginTransaction();
		MapleCharAttribute attribute = (MapleCharAttribute) session.get(
				MapleCharAttribute.class, 28);

		/*
		 * Query query = session.createQuery("from FarmModel");
		 * //即使全局打开了查询缓存，此处也是必须的 query.setCacheable(true); List<FarmModel>
		 * farmList = query.list();
		 */
		DatabaseConnection.getDebugInfo();
		// t.commit();
		session.close();
	}
}
