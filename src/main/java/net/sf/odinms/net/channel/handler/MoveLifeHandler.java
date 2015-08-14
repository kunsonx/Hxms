package net.sf.odinms.net.channel.handler;

import java.awt.Point;
import java.util.List;
import net.sf.odinms.client.MapleClient;
import net.sf.odinms.net.MaplePacket;
import net.sf.odinms.server.life.MapleMonster;
import net.sf.odinms.server.life.MapleMonsterSkill;
import net.sf.odinms.server.life.MobSkill;
import net.sf.odinms.server.life.MobSkillFactory;
import net.sf.odinms.server.movement.LifeMovementFragment;
import net.sf.odinms.tools.MaplePacketCreator;
import net.sf.odinms.tools.Randomizer;
import net.sf.odinms.tools.data.input.SeekableLittleEndianAccessor;

public class MoveLifeHandler extends MovementParse {

	private static org.apache.log4j.Logger log = org.apache.log4j.Logger
			.getLogger(MoveLifeHandler.class);

	@Override
	public void handlePacket(SeekableLittleEndianAccessor slea, MapleClient c) {
		// System.out.println("怪物移动包："+slea.toString());
		int objectid = slea.readInt();
		MapleMonster monster = c.getPlayer().getMap().getMonsterByOid(objectid);
		if (monster == null) { // 有些时候移动的不是怪物
			return;
		}
		short moveid = slea.readShort();// Short
		boolean skillByte = (slea.readByte() > 0);
		byte skill = slea.readByte();
		int skill_id = slea.readByte() & 0xFF;
		int skill_level = slea.readByte() & 0xFF;
		int skill_3 = slea.readByte() & 0xFF;
		int skill_4 = slea.readByte() & 0xFF;// skill_4 新增
		MobSkill toUse = null;
		if (skillByte && monster.getNoSkills() > 0) {
			// int random = (int) (Math.random() * monster.getNoSkills());
			int random = Randomizer.getInstance()
					.nextInt(monster.getNoSkills());
			MapleMonsterSkill skillToUse = monster.getSkills().get(random);
			toUse = MobSkillFactory.getMobSkill(skillToUse.getSkill(),
					skillToUse.getLevel());
			int percHpLeft = (monster.getHp() / monster.getMaxHp()) * 100;
			if (toUse != null
					&& (toUse.getHP() < percHpLeft || !monster
							.canUseSkill(toUse))) {
				toUse = null;
			}
		}
		if (// skill_id >= 100 && skill_id <= 200 &&
		monster.hasSkill(skill_id, skill_level)) {
			MobSkill skillData = MobSkillFactory.getMobSkill(skill_id,
					skill_level);
			if (skillData != null && monster.canUseSkill(skillData)) {
				skillData.applyEffect(c.getPlayer(), monster, true);
			}
		}
		// slea.readInt();
		slea.readShort();
		short unk = slea.readShort();

		if (unk == 0) {
			slea.readInt();
		} else {
			while (unk != 0) {
				unk = slea.readShort();
				if (unk == 1) {
					slea.readShort();
				}
			}
		}

		// slea.readInt();
		slea.readInt();
		slea.readInt();
		Point startPos = slea.readPos();
		Point nextPos = slea.readPos();
		slea.skip(5);
		// Point startPos = new Point(slea.readShort(), slea.readShort());
		// Point nextPos = new Point(slea.readShort(), slea.readShort());
		List<LifeMovementFragment> res = parseMovement(slea);
		// aggro 仇恨 controller 管理者
		if (monster.getController() != c.getPlayer()) { // 如果怪物不属于当前玩家
			if (monster.isAttackedBy(c.getPlayer())) { // 但是该玩家是第一个攻击这个怪物的
				monster.switchController(c.getPlayer(), true);
				// 则设置该怪物属于当前玩家
				// 每次movelife都设置一次是为了保证怪物所有权属于第一个攻击它的人
			} else {
				return;
			}
		} else {
			// 如果管理的控制者是当前玩家
			if (skill == -1 && monster.isControllerKnowsAboutAggro()
					&& !monster.isMobile()) {
				// System.out.println("设置了物理攻击");
				monster.setControllerHasAggro(false);
				monster.setControllerKnowsAboutAggro(false);
			}
			if (!monster.isFirstAttack()) {
				// System.out.println("设置了魔法攻击");
				monster.setControllerHasAggro(true);
				monster.setControllerKnowsAboutAggro(true);
			}
		}
		boolean aggro = monster.isControllerHasAggro();
		if (toUse != null) { // toUse是怪物Skill
			c.getSession().write(
					MaplePacketCreator.moveMonsterResponse(objectid, moveid,
							monster.getMp(), aggro, toUse.getSkillId(),
							toUse.getSkillLevel()));
			// System.out.println("用技能");
		} else {
			c.getSession().write(
					MaplePacketCreator.moveMonsterResponse(objectid, moveid,
							monster.getMp(), aggro));
			// System.out.println("不用技能");
		}
		if (aggro) {
			monster.setControllerKnowsAboutAggro(true);
		}
		if (res != null) {
			if (monster.isAlive()) {
				MaplePacket packet = MaplePacketCreator.moveMonster(skillByte,
						skill, skill_id, skill_level, skill_3, skill_4,
						objectid, startPos, nextPos, res);
				c.getPlayer()
						.getMap()
						.broadcastMessage(c.getPlayer(), packet,
								monster.getPosition());
				updatePosition(res, monster, -1);
				c.getPlayer().getMap()
						.moveMonster(monster, monster.getPosition());
				// c.getPlayer().getCheatTracker().checkMoveMonster(monster.getPosition());
			}
		}
	}
}