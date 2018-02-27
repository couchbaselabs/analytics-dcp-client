/*
 * Copyright (c) 2016-2017 Couchbase, Inc.
 */
package com.couchbase.client.dcp.message;

/**
 * Code describing why producer decided to close the stream.
 */
public enum StreamEndReason {
    /**
     * Invalid stream end reason
     */
    INVALID(0xFF),
    /**
     * The stream has finished without error.
     */
    OK(0x00),
    /**
     * The close stream command was invoked on this stream causing it to be closed
     * by force.
     */
    CLOSED(0x01),
    /**
     * The state of the VBucket that is being streamed has changed to state that
     * the consumer does not want to receive.
     */
    STATE_CHANGED(0x02),
    /**
     * The stream is closed because the connection was disconnected.
     */
    DISCONNECTED(0x03),
    /**
     * The stream is closing because the client cannot read from the stream fast enough.
     * This is done to prevent the server from running out of resources trying while
     * trying to serve the client. When the client is ready to read from the stream
     * again it should reconnect. This flag is available starting in Couchbase 4.5.
     */
    TOO_SLOW(0x04),
    /**
     * The stream closed early due to backfill failure.
     */
    END_STREAM_BACKFILL_FAIL(0x05),
    /**
     * The stream closed early because the vbucket is rolling back and
     * downstream needs to reopen the stream and rollback too.
     */
    END_STREAM_ROLLBACK(0x06),

    /**
     * All filtered collections have been removed so no more data can be sent.
     */
    END_STREAM_FILTER_EMPTY(0x07),
    /**
     * the stream ended because its channel was dropped abruptly
     */
    CHANNEL_DROPPED(0x08);

    private final int value;

    StreamEndReason(int value) {
        this.value = value;
    }

    public int value() {
        return value;
    }

    static StreamEndReason of(int value) {
        switch (value) {
            case 0x00:
                return OK;
            case 0x01:
                return CLOSED;
            case 0x02:
                return STATE_CHANGED;
            case 0x03:
                return DISCONNECTED;
            case 0x04:
                return TOO_SLOW;
            case 0x05:
                return END_STREAM_BACKFILL_FAIL;
            case 0x06:
                return END_STREAM_ROLLBACK;
            case 0x07:
                return END_STREAM_FILTER_EMPTY;
            default:
                throw new IllegalArgumentException("Unknown stream end reason: " + value);
        }
    }

}
