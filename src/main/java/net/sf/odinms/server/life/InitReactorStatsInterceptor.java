/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package net.sf.odinms.server.life;

import java.io.Serializable;
import net.sf.odinms.server.maps.MapleReactorStats;
import org.hibernate.CallbackException;
import org.hibernate.EmptyInterceptor;
import org.hibernate.EntityMode;

/**
 *
 * @author Admin
 */
public class InitReactorStatsInterceptor extends EmptyInterceptor {

	@Override
	public Object instantiate(String entityName, EntityMode entityMode,
			Serializable id) throws CallbackException {
		if (entityName
				.equals("net.sf.odinms.server.maps.MapleReactorStats$StateData")
				&& entityMode.equals(entityMode.POJO)) {
			return new MapleReactorStats.StateData();
		}
		return null;
	}
}
