/*
	
*/

package net.sf.odinms.server.maps;

import java.util.concurrent.ScheduledFuture;
import net.sf.odinms.server.MaplePortal;
/**
 *
 * @author iamSTEVE
 */
public class MapMonitor {

    private ScheduledFuture<?> monitorSchedule;
    private MapleMap map;
    private MaplePortal portal;
    private MapleReactor reactor;

    public MapMonitor(final MapleMap map, MaplePortal portal, int ch, MapleReactor reactor) {
        this.map = map;
        this.portal = portal;
        this.reactor = reactor;
    }
}