/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package net.sf.odinms.tools.data.output;

import org.apache.mina.core.buffer.IoBuffer;

/**
 * 字节数组输出流。
 * 
 * @author HXMS
 */
public class IoBufferOutputStream implements ByteOutputStream {

	private IoBuffer buf;
	private int count = 0;

	public IoBufferOutputStream() {
		this(4);
	}

	public IoBufferOutputStream(int cap) {
		this.buf = IoBuffer.allocate(cap);
		this.buf.setAutoExpand(true);
		this.buf.setAutoShrink(true);
	}

	@Override
	public synchronized void writeByte(byte b) {
		count++;
		buf.put(b);
	}

	@Override
	public synchronized String toString() {
		String ret = "";
		int pos = buf.position();
		ret = buf.rewind().getHexDump();
		buf.position(pos);
		return ret;
	}

	@Override
	public synchronized byte[] ToArray() {
		int pos = buf.position();
		buf.rewind();
		byte[] returnvalue = new byte[count];
		buf.get(returnvalue);
		buf.position(pos);
		return returnvalue;
	}

	public int size() {
		return count;
	}

	@Override
	public synchronized void write(byte[] data) {
		count += data.length;
		buf.put(data);
	}
}
