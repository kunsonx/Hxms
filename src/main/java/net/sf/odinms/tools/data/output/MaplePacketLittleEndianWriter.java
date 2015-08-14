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
package net.sf.odinms.tools.data.output;

import java.nio.charset.Charset;
import net.sf.odinms.net.ByteArrayMaplePacket;
import net.sf.odinms.net.MaplePacket;
import net.sf.odinms.tools.HexTool;
import org.apache.mina.core.buffer.IoBuffer;

/**
 * Writes a maplestory-packet little-endian stream of bytes.
 *
 * @author Frz
 * @version 1.0
 * @since Revision 352
 */
public class MaplePacketLittleEndianWriter extends GenericLittleEndianWriter {

	private static Charset UTF8Charset = Charset.forName("utf-8");
	// private ByteArrayOutputStream baos;
	private IoBufferOutputStream baos;

	/**
	 * Constructor - initializes this stream with a default size.
	 */
	public MaplePacketLittleEndianWriter() {
		// this(32);
		this(8);
	}

	/**
	 * Constructor - initializes this stream with size <code>size</code>.
	 *
	 * @param size
	 *            The size of the underlying stream.
	 */
	public MaplePacketLittleEndianWriter(int size) {
		// this.baos = new ByteArrayOutputStream(size);
		// setByteOutputStream(new BAOSByteOutputStream(baos));
		this.baos = new IoBufferOutputStream(size);
		setByteOutputStream(baos);
	}

	/**
	 * Gets a <code>MaplePacket</code> instance representing this sequence of
	 * bytes.
	 *
	 * @return A <code>MaplePacket</code> with the bytes in this stream.
	 */
	public MaplePacket getPacket() {
		// return new ByteArrayMaplePacket(baos.toByteArray());
		return new ByteArrayMaplePacket(baos.ToArray());
	}

	public IoBuffer getBuffer() {
		return IoBuffer.wrap(baos.ToArray());
	}

	/**
	 * Changes this packet into a human-readable hexadecimal stream of bytes.
	 *
	 * @return This packet as hex digits.
	 */
	@Override
	public String toString() {
		// return HexTool.toString(baos.toByteArray());
		return baos.toString();
	}

	public int size() {
		return baos.size();
	}

	public void writeHex(String hex) {
		write(HexTool.getByteArrayFromHexString(hex));
	}

	public void writeZero(int quantrty) {
		for (int i = 0; i < quantrty; i++) {
			write();
		}
	}

	public void write(boolean bol) {
		write(bol ? 1 : 0);
	}

	/*
	 * 隐式填充.
	 */
	public void write() {
		write(0);
	}

	public void writeSome(int... bytes) {
		for (int b : bytes) {
			write(b);
		}
	}

	public void writeUTF8String(String str) {
		byte[] data = str.getBytes(UTF8Charset);
		writeShort(data.length);
		write(data);
	}
}
