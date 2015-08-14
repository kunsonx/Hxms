package net.sf.odinms.server.maps;

/**
 *
 * @author Lerk
 */
import net.sf.odinms.database.DatabaseConnection;
import net.sf.odinms.server.life.InitReactorStatsInterceptor;
import org.hibernate.Session;

public class MapleReactorFactory {

	public static MapleReactorStats getReactor(int rid) {
		MapleReactorStats stats = null;
		Session session = null;
		try {
			session = DatabaseConnection.getSession();
			stats = (MapleReactorStats) session.get(MapleReactorStats.class,
					rid);
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			session.close();
		}
		return stats;
	}
}