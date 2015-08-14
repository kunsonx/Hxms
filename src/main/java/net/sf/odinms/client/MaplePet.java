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
package net.sf.odinms.client;

import java.awt.Point;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.List;
import java.util.concurrent.ScheduledFuture;
import net.sf.odinms.server.MapleItemInformationProvider;
import net.sf.odinms.server.TimerManager;
import net.sf.odinms.server.movement.AbsoluteLifeMovement;
import net.sf.odinms.server.movement.LifeMovement;
import net.sf.odinms.server.movement.LifeMovementFragment;
import net.sf.odinms.tools.MaplePacketCreator;

/**
 *
 * @author Matze
 */
public class MaplePet extends Item {

	private String name;
	private int closeness = 0;
	private int level = 1;
	private int fullness = 100;
	private int Fh;
	private Point pos;
	private int stance;
	private int slot = -1;
	private ScheduledFuture fullnessSchedule = null;
	private int hunger = 0;

	private MaplePet(int id, short position, long uniqueid) {
		super(id, position, (short) 1);
		this.uniqueid = uniqueid;
		this.hunger = PetDataFactory.getHunger(id);
	}

	public static MaplePet loadFromDb(int itemid, short position,
			long uniqueid, ResultSet rs) {
		try {
			MaplePet ret = new MaplePet(itemid, position, uniqueid);
			ret.setName(rs.getString("name"));
			ret.setCloseness(rs.getInt("closeness"));
			ret.setLevel(Math.max(rs.getInt("pet_level"), 1));
			ret.setFullness(rs.getInt("fullness"));
			ret.slot = rs.getInt("slot") - 1;
			if (ret.getName() == null || ret.getName().isEmpty()) {
				MapleItemInformationProvider.getInstance().getName(itemid);
			}
			return ret;
		} catch (SQLException ex) {
			return null;
		}
	}

	public static MaplePet createPet(int itemid) {
		return createPet(itemid, 90);
	}

	public static MaplePet createPet(int itemid, int days) {
		MaplePet pet = new MaplePet(itemid, (short) 1,
				MapleCharacter.getNextUniqueId());
		pet.setName(MapleItemInformationProvider.getInstance().getName(itemid));
		pet.setFullness(100);
		pet.setLevel(1);
		java.util.Calendar calendar = java.util.Calendar.getInstance();
		calendar.add(java.util.Calendar.DAY_OF_MONTH, 90);
		pet.setExpiration(new Timestamp(calendar.getTimeInMillis()));
		return pet;
	}

	public String getName() {
		if (name == null || name.isEmpty()) {
			name = MapleItemInformationProvider.getInstance().getName(
					getItemId());
		}
		return name;
	}

	public void setName(String name) {
		if (name != null && !name.isEmpty()) {
			this.name = name;
		}
	}

	@Override
	public byte getType() {
		return IItem.PET;
	}

	public int getCloseness() {
		return closeness;
	}

	public void setCloseness(int closeness) {
		this.closeness = closeness;
	}

	public int getLevel() {
		return level;
	}

	public void setLevel(int level) {
		this.level = level;
	}

	public int getFullness() {
		return fullness;
	}

	public void setFullness(int fullness) {
		this.fullness = fullness;
	}

	public int getFh() {
		return Fh;
	}

	public void setFh(int Fh) {
		this.Fh = Fh;
	}

	public Point getPos() {
		return pos;
	}

	public void setPos(Point pos) {
		this.pos = pos;
	}

	public int getStance() {
		return stance;
	}

	public void setStance(int stance) {
		this.stance = stance;
	}

	public boolean canConsume(int itemId) {
		MapleItemInformationProvider mii = MapleItemInformationProvider
				.getInstance();
		for (int petId : mii.petsCanConsume(itemId)) {
			if (petId == this.getItemId()) {
				return true;
			}
		}
		return false;
	}

	public void updatePosition(List<LifeMovementFragment> movement) {
		for (LifeMovementFragment move : movement) {
			if (move instanceof LifeMovement) {
				if (move instanceof AbsoluteLifeMovement) {
					Point p = ((LifeMovement) move).getPosition();
					this.setPos(p);
				}
				this.setStance(((LifeMovement) move).getNewstate());
			}
		}
	}

	public int getSlot() {
		return slot;
	}

	public void setSlot(int slot) {
		this.slot = slot;
	}

	public MaplePet getInstance() {
		return this;
	}

	public synchronized void StartFullnessSchedule(final MapleCharacter chr) {
		if (fullnessSchedule != null) {
			fullnessSchedule.cancel(false);
			fullnessSchedule = null;
		}
		final MaplePet install = this;
		fullnessSchedule = TimerManager.getInstance().register(new Runnable() {
			@Override
			public void run() {
				if (install.getSlot() == -1) {// 已卸载。
					install.CancelFullnessSchedule();
					return;
				}
				int newFullness = getFullness() - hunger;
				if (newFullness <= 5) {
					setFullness(15);
					chr.unequipPet(install, true);
				} else {
					setFullness(newFullness);
					chr.getClient().getSession()
							.write(MaplePacketCreator.updatePet(getInstance()));
				}
			}
		}, 60000, 60000);
	}

	public void CancelFullnessSchedule() {
		if (fullnessSchedule != null) {
			fullnessSchedule.cancel(false);
		}
		fullnessSchedule = null;
	}
}