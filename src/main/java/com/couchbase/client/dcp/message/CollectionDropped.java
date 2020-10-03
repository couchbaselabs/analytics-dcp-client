/*
 * Copyright 2020 Couchbase, Inc.
 */

package com.couchbase.client.dcp.message;

import com.couchbase.client.deps.io.netty.buffer.ByteBuf;

public class CollectionDropped extends DcpSystemEvent {
    private final long newManifestId;
    private final int collectionId;
    private final int scopeId;

    public CollectionDropped(short vbucket, long seqno, int version, ByteBuf buffer) {
        super(Type.COLLECTION_DROPPED, vbucket, seqno, version);

        ByteBuf value = MessageUtil.getContent(buffer);

        newManifestId = value.readLong();
        scopeId = value.readInt();
        collectionId = value.readInt();
    }

    @Override
    public long getManifestId() {
        return newManifestId;
    }

    public int getScopeId() {
        return scopeId;
    }

    public int getCollectionId() {
        return collectionId;
    }

    @Override
    public CollectionsManifest apply(CollectionsManifest currentManifest) {
        return currentManifest.withoutCollection(newManifestId, collectionId);
    }

    @Override
    public String toString() {
        return "CollectionDropped{" + "newManifestId=0x" + Long.toUnsignedString(newManifestId, 16)
                + ", collectionId=0x" + Integer.toUnsignedString(collectionId, 16) + ", scopeId=0x"
                + Integer.toUnsignedString(scopeId, 16) + ", vbucket=" + getVbucket() + ", seqno=" + getSeqno()
                + ", version=" + getVersion() + '}';
    }
}