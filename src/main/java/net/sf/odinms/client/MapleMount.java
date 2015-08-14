package net.sf.odinms.client;

import java.util.concurrent.ScheduledFuture;
import net.sf.odinms.client.skills.弩骑;
import net.sf.odinms.server.TimerManager;
import net.sf.odinms.tools.MaplePacketCreator;

/**
 *
 * @author Patrick/PurpleMadness
 */
public class MapleMount {

    private int itemid;
    private int skillid;
    private int tiredness;
    private int exp;
    private int level;
    private ScheduledFuture<?> tirednessSchedule;
    private MapleCharacter owner;
    private boolean active;

    public MapleMount(MapleCharacter owner, int itemid, int skillid) {
        this.itemid = itemid;
        this.skillid = skillid;
        this.tiredness = 0;
        this.level = 1;
        this.exp = 0;
        this.owner = owner;
        active = true;
    }

    public int getItemId() {
        return itemid;
    }

    public int getSkillId() {
        return skillid;
    }

    public int getId() {
        if (this.itemid < 1932001) {
            return itemid - 1901999;
        } else if (this.itemid == 1932000) {
            return 4;
        }
        return 5;
    }

    public int getTiredness() {
        return tiredness;
    }

    public int getExp() {
        return exp;
    }

    public int getLevel() {
        return level;
    }

    public void setTiredness(int newtiredness) {
        this.tiredness = newtiredness;
        if (tiredness < 0) {
            tiredness = 0;
        }
    }

    public void increaseTiredness() {
        this.tiredness++;
        owner.getMap().broadcastMessage(MaplePacketCreator.updateMount(owner.getId(), this, false));
        if (tiredness > 100) //疲劳超过定值取消骑宠
        {
            owner.dispelSkill(owner.getJobType() * 20000000 + 1004);
        }
    }

    public void setExp(int newexp) {
        this.exp = newexp;
    }

    public void setLevel(int newlevel) {
        this.level = newlevel;
    }

    public void setItemId(int newitemid) {
        this.itemid = newitemid;
    }

    public void setSkillId(int skillid) {
        this.skillid = skillid;
    }

    public void startSchedule() {
        if (this.skillid != 35001002 && this.skillid != 弩骑.美洲豹骑士) {
            this.tirednessSchedule = TimerManager.getInstance().register(new Runnable() {
                public void run() {
                    increaseTiredness(); //给疲劳
                }
            }, 60000, 60000);
        }
    }

    public void cancelSchedule() {
        this.tirednessSchedule.cancel(false);
    }

    public void setActive(boolean set) {
        active = set;
    }

    public boolean isActive() {
        return active;
    }
}