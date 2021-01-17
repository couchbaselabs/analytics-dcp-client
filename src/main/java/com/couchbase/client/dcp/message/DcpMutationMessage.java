/*
 * Copyright (c) 2016-2017 Couchbase, Inc.
 */
package com.couchbase.client.dcp.message;

import static com.couchbase.client.dcp.message.MessageUtil.DCP_MUTATION_OPCODE;

import java.nio.charset.Charset;

import com.couchbase.client.dcp.util.CollectionsUtil;
import com.couchbase.client.deps.io.netty.buffer.ByteBuf;
import com.couchbase.client.deps.io.netty.util.CharsetUtil;

public enum DcpMutationMessage {
    ;

    public static boolean is(final ByteBuf buffer) {
        final byte magic = buffer.getByte(0);
        return buffer.getByte(1) == DCP_MUTATION_OPCODE
                && (magic == MessageUtil.MAGIC_REQ || magic == MessageUtil.MAGIC_REQ_FLEX);
    }

    public static int cid(final ByteBuf buffer) {
        return MessageUtil.getCid(buffer);
    }

    public static ByteBuf key(final ByteBuf buffer, boolean isCollectionEnabled) {
        return MessageUtil.getKey(buffer, isCollectionEnabled);
    }

    public static String keyString(final ByteBuf buffer, Charset charset, boolean isCollectionEnabled) {
        return key(buffer, isCollectionEnabled).toString(charset);
    }

    public static String keyString(final ByteBuf buffer, boolean isCollectionEnabled) {
        return keyString(buffer, CharsetUtil.UTF_8, isCollectionEnabled);
    }

    public static ByteBuf content(final ByteBuf buffer) {
        return MessageUtil.getContent(buffer);
    }

    public static byte[] contentBytes(final ByteBuf buffer) {
        byte[] bytes = new byte[buffer.readableBytes()];
        content(buffer).getBytes(0, bytes);
        return bytes;
    }

    public static long cas(final ByteBuf buffer) {
        return MessageUtil.getCas(buffer);
    }

    public static short partition(final ByteBuf buffer) {
        return MessageUtil.getVbucket(buffer);
    }

    public static long bySeqno(final ByteBuf buffer) {
        return buffer.getLong(MessageUtil.getHeaderSize(buffer));
    }

    public static long revisionSeqno(final ByteBuf buffer) {
        return buffer.getLong(MessageUtil.getHeaderSize(buffer) + 8);
    }

    public static int flags(final ByteBuf buffer) {
        return buffer.getInt(MessageUtil.getHeaderSize(buffer) + 16);
    }

    public static int expiry(final ByteBuf buffer) {
        return buffer.getInt(MessageUtil.getHeaderSize(buffer) + 20);
    }

    public static int lockTime(final ByteBuf buffer) {
        return buffer.getInt(MessageUtil.getHeaderSize(buffer) + 24);
    }

    public static String toString(final ByteBuf buffer, boolean collections) {
        return "MutationMessage [key: \"" + keyString(buffer, collections) + "\", "
                + (collections ? "cid: " + CollectionsUtil.displayCid(cid(buffer)) + ", " : "") + "vbid: "
                + partition(buffer) + ", cas: " + cas(buffer) + ", bySeqno: " + bySeqno(buffer) + ", revSeqno: "
                + revisionSeqno(buffer) + ", flags: " + flags(buffer) + ", expiry: " + expiry(buffer) + ", lockTime: "
                + lockTime(buffer) + ", clength: " + content(buffer).readableBytes() + "]";
    }
}
