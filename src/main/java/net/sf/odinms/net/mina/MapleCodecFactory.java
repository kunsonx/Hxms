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
package net.sf.odinms.net.mina;

import java.util.regex.Pattern;
import org.apache.mina.core.session.IoSession;
import org.apache.mina.filter.codec.ProtocolCodecFactory;
import org.apache.mina.filter.codec.ProtocolDecoder;
import org.apache.mina.filter.codec.ProtocolEncoder;

public class MapleCodecFactory implements ProtocolCodecFactory {

	private final ProtocolEncoder encoder;
	private final ProtocolDecoder decoder;
	private static Pattern pattern = Pattern
			.compile("(34\\s01)|(84\\s01)|(14\\s00)|(29\\s00)");
	private static String[] filter = new String[] {};

	// "0E 01", "3F 00","2B 00"
	// "34 01", "84 01", "14 00", "29 00", "3F 00", "4D 00", "CB 00", "24 01",
	// "67 01"

	public MapleCodecFactory() {
		encoder = new MaplePacketEncoder();
		decoder = new MaplePacketDecoder();
	}

	public static Pattern getPattern() {
		return pattern;
	}

	public static boolean isFilter(String arg) {
		for (String string : filter) {
			if (arg.startsWith(string)) {
				return true;
			}
		}
		return false;
	}

	@Override
	public ProtocolEncoder getEncoder(IoSession is) throws Exception {
		return encoder;
	}

	@Override
	public ProtocolDecoder getDecoder(IoSession is) throws Exception {
		return decoder;
	}
}
