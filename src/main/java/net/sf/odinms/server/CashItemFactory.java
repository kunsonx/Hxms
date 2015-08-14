package net.sf.odinms.server;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.sf.odinms.database.DatabaseConnection;
import org.hibernate.Session;

/**
 *
 * @author Lerk
 */
public class CashItemFactory {

	private static Map<Integer, Integer> idLookup = new HashMap<Integer, Integer>();
	private static Map<Integer, List<CashItemInfo>> cashPackages = new HashMap<Integer, List<CashItemInfo>>();
	private static org.apache.log4j.Logger log = org.apache.log4j.Logger
			.getLogger(CashItemFactory.class);
	private static Map<Integer, CashItemInfo> getItemInSql = new HashMap<Integer, CashItemInfo>();

	public static CashItemInfo getItemInSql(int sn) {
		CashItemInfo stats = getItemInSql.get(sn);
		if (stats == null) {
			try {
				Session session = DatabaseConnection.getSession();
				// CashItemInfo cs = (CashItemInfo) session.createQuery(
				// "from CashItemInfo as cs where cs.SN =:sn").setParameter("sn",
				// sn).setMaxResults(1).uniqueResult();
				CashItemInfo cs = (CashItemInfo) session.get(
						CashItemInfo.class, sn);
				session.close();
				if (cs != null) {
					getItemInSql.put(sn, cs);
					stats = cs;
				}
			} catch (Exception sqle) {
				log.error("从Sql中获取商城数据出错", sqle);
			}
		}
		return stats;
	}

	public static int getPackageItemsCount(int itemId) {
		return getPackageItems(itemId).size();
	}

	public static List<CashItemInfo> getPackageItems(int itemId) {
		if (cashPackages.containsKey(itemId)) {
			return cashPackages.get(itemId);
		}
		List<CashItemInfo> packageItems = null;
		Session session = DatabaseConnection.getSession();
		// CashPackageList list = (CashPackageList) session.createQuery(
		// "from CashPackageList as cs where cs.packageid =:id").setParameter("id",
		// itemId).setMaxResults(1).uniqueResult();
		CashPackageList list = (CashPackageList) session.get(
				CashPackageList.class, itemId);
		session.close();
		if (list != null) {
			packageItems = list.getItems();
			cashPackages.put(itemId, packageItems);
		}
		return packageItems;
	}

	public static int getSnFromId(int id) {
		if (!idLookup.containsKey(id)) {
			try {
				// 先获取制造该物品需要的 人物等级 锻造技能等级 金钱 锻造出来的物品的数量
				Session session = DatabaseConnection.getSession();
				CashItemInfo cs = (CashItemInfo) session
						.createQuery(
								"from CashItemInfo as cs where cs.itemId =:id")
						.setParameter("id", id).setMaxResults(1).uniqueResult();
				session.close();
				if (cs != null) {
					idLookup.put(id, cs.getSN());
				}
			} catch (Exception sqle) {
				log.error("从Sql中获取商城数据出错", sqle);
			}
		}
		if (idLookup.containsKey(id)) {
			return idLookup.get(id);
		} else {
			return 0;
		}
	}
}
