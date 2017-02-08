/*
 * Copyright (c) 2016 Couchbase, Inc.
 */
package com.couchbase.client.dcp.state;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents the individual current session state for a given partition.
 */
public class PartitionState {

    public static final long INVALID = -1L;
    public static final byte DISCONNECTED = 0x00;
    public static final byte CONNECTING = 0x02;
    public static final byte CONNECTED = 0x03;

    /**
     * Stores the failover log for this partition.
     */
    private final List<FailoverLogEntry> failoverLog;
    private volatile long maxSeqno = INVALID;

    private final short vbid;

    /**
     * Current Sequence Number
     */
    private volatile long seqno = 0;

    private volatile byte state;

    private StreamRequest streamRequest;

    /**
     * Initialize a new partition state.
     */
    public PartitionState(short vbid) {
        this.vbid = vbid;
        failoverLog = new ArrayList<>();
        setState(DISCONNECTED);
    }

    /**
     * Returns the full failover log stored, in sorted order.
     */
    public List<FailoverLogEntry> getFailoverLog() {
        return failoverLog;
    }

    /**
     * Add a new seqno/uuid combination to the failover log.
     *
     * @param seqno
     *            the sequence number.
     * @param vbuuid
     *            the uuid for the sequence.
     */
    public void addToFailoverLog(long seqno, long vbuuid) {
        failoverLog.add(new FailoverLogEntry(seqno, vbuuid));
    }

    /**
     * Returns the current sequence number.
     */
    public long getSeqno() {
        return seqno;
    }

    /**
     * Allows to set the current sequence number.
     */
    public void setSeqno(long seqno) {
        this.seqno = seqno;
    }

    public byte getState() {
        return state;
    }

    public void setState(byte state) {
        this.state = state;
    }

    public StreamRequest getStreamRequest() {
        return streamRequest;
    }

    public void setStreamRequest(StreamRequest streamRequest) {
        this.streamRequest = streamRequest;
    }

    public void prepareNextStreamRequest() {
        this.streamRequest = new StreamRequest(vbid, seqno, SessionState.NO_END_SEQNO,
                failoverLog.get(failoverLog.size() - 1).getUuid());
    }

    public short vbid() {
        return vbid;
    }

    public long maxSeqno() {
        return maxSeqno;
    }

    public void setMaxSeqno(long maxSeqno) {
        this.maxSeqno = maxSeqno;
    }

    public StreamRequest useStreamRequest() {
        StreamRequest temp = streamRequest;
        streamRequest = null;
        return temp;
    }

    @Override
    public String toString() {
        return "vbid = " + vbid + ", maxSeq = " + maxSeqno + ", seqno = " + seqno + ", state = " + state + ", uuid = "
                + failoverLog.get(0).getUuid();
    }
}
