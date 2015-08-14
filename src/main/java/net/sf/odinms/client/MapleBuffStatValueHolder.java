
package net.sf.odinms.client;

import java.util.concurrent.ScheduledFuture;
import net.sf.odinms.server.MapleStatEffect;

/**
 *
 * @author 岚殇
 */
    public class MapleBuffStatValueHolder {
        public MapleStatEffect effect;
        public long startTime;
        public int value;
        public ScheduledFuture<?> schedule;

        public MapleBuffStatValueHolder(MapleStatEffect effect, long startTime, ScheduledFuture<?> schedule, int value) {
            this.effect = effect;
            this.startTime = startTime;
            this.schedule = schedule;
            this.value = value;
        }
    }