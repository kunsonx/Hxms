//技能效果[动画] 一般是机械的技能
package net.sf.odinms.net.channel.handler;

import net.sf.odinms.client.ISkill;
import net.sf.odinms.client.MapleBuffStat;
import net.sf.odinms.client.MapleClient;
import net.sf.odinms.client.SkillFactory;
import net.sf.odinms.client.skills.机械师;
import net.sf.odinms.net.AbstractMaplePacketHandler;
import net.sf.odinms.server.MapleStatEffect;
import net.sf.odinms.server.TimerManager;
import net.sf.odinms.server.life.MapleMonster;
import net.sf.odinms.server.maps.MapleMap;
import net.sf.odinms.tools.MaplePacketCreator;
import net.sf.odinms.tools.data.input.SeekableLittleEndianAccessor;

/**
 *
 * @author 岚殇
 */
public class 机械技能特效 extends AbstractMaplePacketHandler {

    @Override
    public void handlePacket(SeekableLittleEndianAccessor slea, final MapleClient c) {
        //System.out.println("机械技能特效处理："+slea.toString());
        int skillId = slea.readInt() + 1000;//技能ID
        int skillLevel = slea.readByte();//等级
        int unknow = slea.readByte();//不知道是什么 一般是 01
        if ((skillId == 机械师.强化火焰喷射器
                || skillId == 机械师.火焰喷射器
                || skillId == 机械师.金属机甲_导弹战车
                || skillId == 机械师.金属机甲_重机枪
                || skillId == 机械师.金属机甲_重机枪_4转 //|| skillId == 机械师.火箭推进器
                //机械的特效技能
                ) && skillLevel >= 1) {
            //因为是出现一个返回的动画 所以skillid会比正常的skillid少1000
            c.getPlayer().getClient().getSession().write(MaplePacketCreator.机械技能特效(1, 0, skillId - 1000, skillLevel, unknow, -1));
            //c.getPlayer().getMap().broadcastMessage(MaplePacketCreator.机械技能特效(c.getPlayer().getId(), 1, 0, skillId - 1000, skillLevel, unknow, -1));
            if (skillId == 机械师.金属机甲_导弹战车) {
                c.getPlayer().cancelBuffStats(MapleBuffStat.机械师);
            } else if (skillId == 机械师.金属机甲_重机枪_4转) {
                //取消重机枪其实就是重新给导弹战车的Buff
                ISkill skill = SkillFactory.getSkill(机械师.金属机甲_导弹战车);
                skill.getEffect(skillLevel).applyTo(c.getPlayer());
            } else if (skillId == 35111004) {
                c.getPlayer().cancelBuffStats(MapleBuffStat.机械师);
            }
        } else {
            //System.out.println("机械技能特效处理要添加相应的skillid："+ skillId);
            //c.getSession().close();
        }
    }
}
