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
package net.sf.odinms.tools;

import java.sql.Timestamp;
import java.util.SimpleTimeZone;
import net.sf.odinms.tools.data.output.MaplePacketLittleEndianWriter;

public class DateUtil {

	private final static long FT_UT_OFFSET = 116444520000000000L;
	private final static long FINAL_TIME = 3439785600000L;

	public static boolean isDST() {
		return SimpleTimeZone.getDefault().useDaylightTime();
	}

	/**
	 * Converts a Unix Timestamp into File Time
	 * 
	 * @param timeStampinMillis
	 * @return A 64-bit long giving a filetime timestamp
	 */
	public static long getFileTimestamp(long timeStampinMillis) {
		return getFileTimestamp(timeStampinMillis, false);
	}

	public static long getFileTimestamp(long timeStampinMillis,
			boolean roundToMinutes) {
		if (isDST()) {
			timeStampinMillis -= 3600000L; // 60 * 60 * 1000
		}
		timeStampinMillis += 14 * 60 * 60 * 1000;
		long time;
		if (roundToMinutes) {
			time = (timeStampinMillis / 1000 / 60) * 600000000;
		} else {
			time = timeStampinMillis * 10000;
		}
		return time + FT_UT_OFFSET;
	}

	public static long getTimeMillis(long FileTimestamp) {
		FileTimestamp -= FT_UT_OFFSET;
		FileTimestamp /= 10000;
		FileTimestamp -= 14 * 60 * 60 * 1000;
		return FileTimestamp;
	}

	public static void addExpirationTime(MaplePacketLittleEndianWriter mplew) {
		addExpirationTime(mplew, null);
	}

	public static void addExpirationTime(MaplePacketLittleEndianWriter mplew,
			Timestamp time) {
		if (time != null) {
			mplew.writeLong(getFileTimestamp(time.getTime()));
		} else {
			mplew.writeLong(getFileTimestamp(FINAL_TIME));
		}
	}

	/**
	 * 添加技能限制时间。
	 * 
	 * @param mplew
	 */
	public static void addSkillExpirationTime(
			MaplePacketLittleEndianWriter mplew) {
		mplew.writeLong(getFileTimestamp(MaplePacketCreator.FINAL_LOCKITEMTIME));
	}

	/**
	 * 添加技能限制时间。
	 * 
	 * @param mplew
	 * @param time
	 */
	public static void addSkillExpirationTime(
			MaplePacketLittleEndianWriter mplew, Timestamp time) {
		if (time != null) {
			mplew.writeLong(getFileTimestamp(time.getTime()));
		} else {
			addSkillExpirationTime(mplew);
		}
	}
}