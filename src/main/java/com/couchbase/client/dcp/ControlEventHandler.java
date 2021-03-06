/*
 * Copyright (c) 2016-2017 Couchbase, Inc.
 */
package com.couchbase.client.dcp;

import com.couchbase.client.deps.io.netty.buffer.ByteBuf;

/**
 * This interface acts as a callback on the {@link Client#controlEventHandler(ControlEventHandler)} API
 * that allows one to react to control events.
 *
 * Right now the only event emitted is a {@link com.couchbase.client.dcp.message.RollbackMessage} which
 * should be handled appropriately since it indicates that very likely the current consumer state is
 * ahead of the server. This happens during failover scenarios and/or if something weird happened
 * to the persisted session state.
 *
 * Keep in mind that the callback is called on the IO event loops, so you should never block or run
 * expensive computations in the callback! Use queues and other synchronization primities!
 *
 * @author Michael Nitschinger
 * @since 1.0.0
 */
@FunctionalInterface
public interface ControlEventHandler {

    /**
     * Called every time when a control event happens that should be handled by
     * consumers of the {@link Client}.
     *
     * Even if you are not doing anything with the events, make sure to release the buffer!!
     *
     * @param event
     *            the control event happening.
     */
    void onEvent(DcpAckHandle ackHandle, ByteBuf event);

}
