/*
 * Copyright (c) 2016 Couchbase, Inc.
 */
package com.couchbase.client.dcp.state;

import static com.couchbase.client.dcp.util.MathUtil.maxUnsigned;
import static org.apache.hyracks.util.Span.ELAPSED;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.apache.hyracks.util.Span;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.couchbase.client.dcp.message.DcpSystemEvent;
import com.couchbase.client.dcp.util.MemcachedStatus;
import com.couchbase.client.deps.com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Represents the individual current session state for a given partition.
 */
public class StreamPartitionState {
    private static final Logger LOGGER = LogManager.getLogger();
    public static final long INVALID = -1L;
    public static final byte DISCONNECTED = 0x00;
    public static final byte CONNECTING = 0x02;
    public static final byte CONNECTED = 0x03;
    public static final byte DISCONNECTING = 0x04;
    public static final long RECOVERING = 0x05;
    public static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private volatile long currentVBucketSeqnoInMaster = INVALID;

    private final short vbid;

    private final SessionPartitionState sessionState;

    /**
     * Current Sequence Number
     */
    private volatile long seqno = 0;

    private volatile long streamEndSeq = 0;

    private volatile long snapshotStartSeqno = 0;

    private volatile long snapshotEndSeqno = 0;

    private volatile byte state;

    private volatile boolean osoSnapshot;

    private volatile long osoMaxSeqno = 0;

    private StreamRequest streamRequest;

    private long manifestUid;

    private Span delay;

    /**
     * Initialize a new partition state.
     */
    public StreamPartitionState(short vbid, StreamState stream) {
        this.vbid = vbid;
        state = DISCONNECTED;
        sessionState = stream.session().get(vbid);
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
        currentVBucketSeqnoInMaster = maxUnsigned(currentVBucketSeqnoInMaster, snapshotEndSeqno);
    }

    /**
     * Returns the full failover log stored, in sorted order.
     * index of more recent history entry > index of less recent history entry
     */
    public List<FailoverLogEntry> getFailoverLog() {
        return sessionState.getFailoverLog();
    }

    public boolean hasFailoverLogs() {
        return sessionState.hasFailoverLogs();
    }

    public FailoverLogEntry getFailoverLog(int index) {
        return sessionState.getFailoverLog(index);
    }

    public int getFailoverLogSize() {
        return sessionState.getFailoverLogSize();
    }

    public long getUuid() {
        return sessionState.uuid();
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
        if (osoSnapshot) {
            //noinspection NonAtomicOperationOnVolatileField
            osoMaxSeqno = maxUnsigned(seqno, osoMaxSeqno);
        } else {
            if (Long.compareUnsigned(seqno, this.seqno) <= 0) {
                LOGGER.warn("new seqno received (0x{}) <= the previous seqno(0x{}) for vbid: {}",
                        Long.toUnsignedString(seqno, 16), Long.toUnsignedString(this.seqno, 16), vbid);
            }
            if (LOGGER.isTraceEnabled()) {
                LOGGER.trace("setting seqno to {} for vbid {} on setSeqno", seqno, vbid);
            }
            this.seqno = seqno;
        }
    }

    /**
     * Allows to set the current sequence number.
     */
    public void advanceSeqno(long seqno) {
        setSeqno(seqno);
        setSnapshotStartSeqno(seqno);
        setSnapshotEndSeqno(seqno);
    }

    public byte getState() {
        return state;
    }

    public synchronized void setState(byte state) {
        this.state = state;
        notifyAll();
    }

    public synchronized void wait(byte state) throws InterruptedException {
        LOGGER.trace("Waiting until state is {} for {}", state, vbid);
        while (this.state != state) {
            wait();
        }
    }

    public void setStreamEndSeq(long seq) {
        this.streamEndSeq = seq;
    }

    public long getStreamEndSeq() {
        return streamEndSeq;
    }

    public StreamRequest getStreamRequest() {
        return streamRequest;
    }

    public void setStreamRequest(StreamRequest streamRequest) {
        this.streamRequest = streamRequest;
        LOGGER.trace("setting seqno to {} for vbid {} on setStreamRequest", seqno, vbid);
        seqno = streamRequest.getStartSeqno();
        streamEndSeq = streamRequest.getEndSeqno();
        snapshotStartSeqno = streamRequest.getSnapshotStartSeqno();
        snapshotEndSeqno = streamRequest.getSnapshotEndSeqno();
        manifestUid = streamRequest.getManifestUid();
    }

    public void prepareNextStreamRequest(SessionState sessionState, StreamState streamState) {
        if (streamRequest == null) {
            if (snapshotStartSeqno > seqno) {
                snapshotStartSeqno = seqno;
            }
            if (SessionState.NO_END_SEQNO != streamEndSeq && Long.compareUnsigned(streamEndSeq, seqno) < 0) {
                streamEndSeq = snapshotEndSeqno;
            }
            this.streamRequest =
                    new StreamRequest(vbid, seqno, streamEndSeq, sessionState.get(vbid).uuid(), snapshotStartSeqno,
                            snapshotEndSeqno, manifestUid, streamState.streamId(), streamState.collectionId());
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
        try {
            return OBJECT_MAPPER.writeValueAsString(toMap());
        } catch (IOException e) {
            LOGGER.log(Level.WARN, e);
            return "{\"" + this.getClass().getSimpleName() + "\":\"" + e.toString() + "\"}";
        }
    }

    public Map<String, Object> toMap() {
        Map<String, Object> tree = new HashMap<>();
        tree.put("vbid", vbid);
        tree.put("maxSeq", currentVBucketSeqnoInMaster);
        tree.put("seqno", seqno);
        tree.put("state", state);
        tree.put("osoSnapshot", osoSnapshot);
        tree.put("osoMaxSeq", osoMaxSeqno);
        return tree;
    }

    public void beginOutOfOrder() {
        osoSnapshot = true;
        osoMaxSeqno = 0;
    }

    public long endOutOfOrder() {
        // On disconnect after successfully receiving the OSO end, reconnect
        // with a stream-request where start=X, snap.start=X, snap.end=X
        useStreamRequest();
        osoSnapshot = false;
        advanceSeqno(osoMaxSeqno);
        return osoMaxSeqno;
    }

    public boolean isOsoSnapshot() {
        return osoSnapshot;
    }

    public void onSystemEvent(DcpSystemEvent event) {
        setSeqno(event.getSeqno());
        manifestUid = event.getManifestUid();
    }

    public void calculateNextDelay(short status) {
        if (status == MemcachedStatus.SUCCESS) {
            delay = ELAPSED;
        } else {
            if (delay.getSpanNanos() == 0) {
                // start with 1s
                delay = Span.start(1, TimeUnit.SECONDS);
            } else {
                // double the delay, capping at 64s
                delay = Span.start(Long.min(delay.getSpanNanos() * 2, TimeUnit.SECONDS.toNanos(64)),
                        TimeUnit.NANOSECONDS);
            }
        }
    }

    public Span getDelay() {
        return delay;
    }
}
