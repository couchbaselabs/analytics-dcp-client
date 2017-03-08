/*
 * Copyright (c) 2016 Couchbase, Inc.
 */
package com.couchbase.client.dcp.state;

import java.util.ArrayList;
import java.util.List;

import com.couchbase.client.core.logging.CouchbaseLogger;
import com.couchbase.client.core.logging.CouchbaseLoggerFactory;

/**
 * Represents the individual current session state for a given partition.
 */
public class PartitionState {
    private static final CouchbaseLogger LOGGER = CouchbaseLoggerFactory.getInstance(PartitionState.class);
    public static final long INVALID = -1L;
    public static final byte DISCONNECTED = 0x00;
    public static final byte CONNECTING = 0x02;
    public static final byte CONNECTED = 0x03;
    public static final byte DISCONNECTING = 0x04;
    public static final long RECOVERING = 0x05;

    /**
     * Stores the failover log for this partition.
     */
    private final List<FailoverLogEntry> failoverLog;
    private volatile long currentVBucketSeqnoInMaster = INVALID;

    private final short vbid;

    /**
     * Current Sequence Number
     */
    private volatile long seqno = 0;

    private volatile long snapshotStartSeqno = 0;

    private volatile long snapshotEndSeqno = 0;

    private volatile byte state;

    private volatile boolean failoverUpdated;

    private StreamRequest streamRequest;

    /**
     * Initialize a new partition state.
     */
    public PartitionState(short vbid) {
        this.vbid = vbid;
        failoverLog = new ArrayList<>();
        setState(DISCONNECTED);
        failoverUpdated = false;
    }

    public long getSnapshotStartSeqno() {
        return snapshotStartSeqno;
    }

    public void setSnapshotStartSeqno(long snapshotStartSeqno) {
        this.snapshotStartSeqno = snapshotStartSeqno;
    }

    public long getSnapshotEndSeqno() {
        return snapshotEndSeqno;
    }

    public void setSnapshotEndSeqno(long snapshotEndSeqno) {
        this.snapshotEndSeqno = snapshotEndSeqno;
    }

    /**
     * Returns the full failover log stored, in sorted order.
     */
    public List<FailoverLogEntry> getFailoverLog() {
        return failoverLog;
    }

    public FailoverLogEntry getMostRecentFailoverLogEntry() {
        synchronized (failoverLog) {
            return failoverLog.get(failoverLog.size() - 1);
        }
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
        synchronized (failoverLog) {
            failoverLog.add(new FailoverLogEntry(seqno, vbuuid));
        }
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

    public synchronized void setState(byte state) {
        this.state = state;
        notifyAll();
    }

    public synchronized void wait(byte state) throws InterruptedException {
        LOGGER.debug("Waiting until state is " + state);
        while (this.state != state) {
            wait();
        }
    }

    public StreamRequest getStreamRequest() {
        return streamRequest;
    }

    public void setStreamRequest(StreamRequest streamRequest) {
        this.streamRequest = streamRequest;
    }

    public void prepareNextStreamRequest() {
        if (streamRequest == null) {
            this.streamRequest = new StreamRequest(vbid, seqno, SessionState.NO_END_SEQNO,
                    failoverLog.get(failoverLog.size() - 1).getUuid(), snapshotStartSeqno, snapshotEndSeqno);
        }
    }

    public short vbid() {
        return vbid;
    }

    public long getCurrentVBucketSeqnoInMaster() {
        return currentVBucketSeqnoInMaster;
    }

    public void setCurrentVBucketSeqnoInMaster(long currentVBucketSeqnoInMaster) {
        this.currentVBucketSeqnoInMaster = currentVBucketSeqnoInMaster;
    }

    public void useStreamRequest() {
        streamRequest = null;
    }

    @Override
    public String toString() {
        return "vbid = " + vbid + ", maxSeq = " + currentVBucketSeqnoInMaster + ", seqno = " + seqno + ", state = "
                + state + ", uuid = " + failoverLog.get(0).getUuid();
    }

    public synchronized void failoverUpdated() {
        LOGGER.debug("Failover log updated");
        failoverUpdated = true;
        notifyAll();
    }

    public void failoverRequest() {
        LOGGER.debug("Failover log requested");
        failoverUpdated = false;
    }

    public synchronized void waitTillFailoverUpdated() throws InterruptedException {
        LOGGER.debug("Waiting until failover log updated");
        while (!failoverUpdated) {
            wait();
        }
    }
}
