/*
 This file is part of the OdinMS Maple Story Server
 Copyright (C) 2008 Patrick Huy <patrick.huy@frz.cc> 
 Matthias Butz <matze@odinms.de>
 Jan Christian Meyer <vimes@odinms.de>

 This program is free software: you can redistribute it and/or modify
 it under the terms of the GNU Affero General Public License version 3
 as published by the Free Software Foundation. You may not use, modify
 or distribute this program under any other version of the
 GNU Affero General Public License.

 This program is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU Affero General Public License for more details.

 You should have received a copy of the GNU Affero General Public License
 along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package net.sf.odinms.client.status;

import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;
import java.util.concurrent.ScheduledFuture;
import net.sf.odinms.client.ISkill;

public class MonsterStatusEffect {

    private Map<MapleMonsterStat, Integer> stati;
    private ISkill skill;
    private boolean monsterSkill;
    private ScheduledFuture<?> cancelTask;
    private ScheduledFuture<?> poisonSchedule;

    public MonsterStatusEffect(Map<MapleMonsterStat, Integer> stati, ISkill skillId, boolean monsterSkill) {
        this.stati = Collections.synchronizedMap(new EnumMap<MapleMonsterStat, Integer>(stati));
        this.skill = skillId;
        this.monsterSkill = monsterSkill;
    }

    public synchronized Map<MapleMonsterStat, Integer> getStati() {
        return stati;
    }

    public synchronized Integer setValue(MapleMonsterStat status, Integer newVal) {
        return stati.put(status, newVal);
    }

    public ISkill getSkill() {
        return skill;
    }

    public boolean isMonsterSkill() {
        return monsterSkill;
    }

    public ScheduledFuture<?> getCancelTask() {
        return cancelTask;
    }

    public void CancelCancelTask() {
        if (cancelTask != null) {
            cancelTask.cancel(false);
        }
    }

    public void setCancelTask(ScheduledFuture<?> cancelTask) {
        this.cancelTask = cancelTask;
    }

    public synchronized void removeActiveStatus(MapleMonsterStat stat) {
        stati.remove(stat);
    }

    public void setPoisonSchedule(ScheduledFuture<?> poisonSchedule) {
        this.poisonSchedule = poisonSchedule;
    }

    public void cancelPoisonSchedule() {
        if (poisonSchedule != null) {
            poisonSchedule.cancel(false);
        }
    }
}