/*
	
*/

package net.sf.odinms.tools;

import java.net.SocketAddress;

import java.util.Set;
import net.sf.odinms.net.MaplePacket;
import org.apache.mina.core.filterchain.IoFilterChain;
import org.apache.mina.core.future.CloseFuture;
import org.apache.mina.core.future.ReadFuture;
import org.apache.mina.core.future.WriteFuture;
import org.apache.mina.core.service.IoHandler;
import org.apache.mina.core.service.IoService;
import org.apache.mina.core.service.TransportMetadata;
import org.apache.mina.core.session.IdleStatus;
import org.apache.mina.core.session.IoSession;
import org.apache.mina.core.session.IoSessionConfig;
import org.apache.mina.core.write.WriteRequest;
import org.apache.mina.core.write.WriteRequestQueue;



/**
 * Represents a mock version of an IOSession to use a MapleClient instance
 * without an active connection (faekchar, etc).
 * 
 * Most methods return void, or when they return something, null. Therefore,
 * this class is mostly undocumented, due to the fact that each and every
 * function does squat.
 * 
 * @author Frz
 * @since Revision 518
 * @version 1.0
 */
public class MockIOSession implements IoSession {


    /**
     * Does nothing.
     */
    @Override
    public IoSessionConfig getConfig() {
        return null;
    }

    /**
     * Does nothing.
     */
    @Override
    public IoFilterChain getFilterChain() {
        return null;
    }

    /**
     * Does nothing.
     */
    @Override
    public IoHandler getHandler() {
        return null;
    }

    /**
     * Does nothing.
     * @return 
     */
    @Override
    public SocketAddress getLocalAddress() {
        return null;
    }

    /**
     * Does nothing.
     */
    @Override
    public SocketAddress getRemoteAddress() {
        return null;
    }

    /**
     * Does nothing.
     */
    @Override
    public IoService getService() {
        return null;
    }

    /**
     * Does nothing.
     */
    @Override
    public SocketAddress getServiceAddress() {
        return null;
    }


    /**
     * Does nothing.
     */
    @Override
    public CloseFuture close() {
        return null;
    }


    /**
     * Does nothing.
     */
    @Override
    public WriteFuture write(Object message, SocketAddress remoteAddress) {
        return null;
    }

    /**
     * "Fake writes" a packet to the client, only running the OnSend event of
     * the packet.
     */
    @Override
    public WriteFuture write(Object message) {
        if (message instanceof MaplePacket) {
            MaplePacket mp = (MaplePacket) message;
            if (mp.getOnSend() != null) {
                mp.getOnSend().run();
            }
        }
        return null;
    }



    @Override
    public long getId() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public WriteRequestQueue getWriteRequestQueue() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public TransportMetadata getTransportMetadata() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public ReadFuture read() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public CloseFuture close(boolean bln) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public Object getAttachment() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public Object setAttachment(Object o) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public Object getAttribute(Object o) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public Object getAttribute(Object o, Object o1) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public Object setAttribute(Object o, Object o1) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public Object setAttribute(Object o) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public Object setAttributeIfAbsent(Object o, Object o1) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public Object setAttributeIfAbsent(Object o) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public Object removeAttribute(Object o) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public boolean removeAttribute(Object o, Object o1) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public boolean replaceAttribute(Object o, Object o1, Object o2) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public boolean containsAttribute(Object o) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public Set<Object> getAttributeKeys() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public boolean isConnected() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public boolean isClosing() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public CloseFuture getCloseFuture() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void setCurrentWriteRequest(WriteRequest wr) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void suspendRead() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void suspendWrite() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void resumeRead() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void resumeWrite() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public boolean isReadSuspended() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public boolean isWriteSuspended() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void updateThroughput(long l, boolean bln) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public long getReadBytes() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public long getWrittenBytes() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public long getReadMessages() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public long getWrittenMessages() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public double getReadBytesThroughput() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public double getWrittenBytesThroughput() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public double getReadMessagesThroughput() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public double getWrittenMessagesThroughput() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public int getScheduledWriteMessages() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public long getScheduledWriteBytes() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public Object getCurrentWriteMessage() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public WriteRequest getCurrentWriteRequest() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public long getCreationTime() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public long getLastIoTime() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public long getLastReadTime() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public long getLastWriteTime() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public boolean isIdle(IdleStatus is) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public boolean isReaderIdle() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public boolean isWriterIdle() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public boolean isBothIdle() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public int getIdleCount(IdleStatus is) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public int getReaderIdleCount() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public int getWriterIdleCount() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public int getBothIdleCount() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public long getLastIdleTime(IdleStatus is) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public long getLastReaderIdleTime() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public long getLastWriterIdleTime() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public long getLastBothIdleTime() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

	@Override
	public boolean isSecured() {
		// TODO Auto-generated method stub
		return false;
	}
}
