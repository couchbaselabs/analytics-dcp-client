/*
 * Copyright 2020 Couchbase, Inc.
 */

package com.couchbase.client.dcp.message;

import com.couchbase.client.dcp.util.CollectionsUtil;
import com.couchbase.client.deps.io.netty.buffer.ByteBuf;

public class CollectionCreated extends DcpSystemEvent {
    private static final long serialVersionUID = 1L;
    private final long newManifestUid;
    private final int scopeId;
    private final int collectionId;
    private final String collectionName;
    private final long maxTtl;

    public CollectionCreated(short vbucket, long seqno, int version, ByteBuf buffer) {
        super(Type.COLLECTION_CREATED, vbucket, seqno, version);

        collectionName = MessageUtil.getKeyAsString(buffer, false);
        ByteBuf value = MessageUtil.getContent(buffer);

        newManifestUid = value.readLong();
        scopeId = value.readInt();
        collectionId = value.readInt();

        // absent in version 0
        maxTtl = value.isReadable() ? value.readUnsignedInt() : CollectionsManifest.CollectionInfo.MAX_TTL_UNDEFINED;
    }

    @Override
    public long getManifestUid() {
        return newManifestUid;
    }

    public int getScopeId() {
        return scopeId;
    }

    public int getCollectionId() {
        return collectionId;
    }

    public String getCollectionName() {
        return collectionName;
    }

    /**
     * @return the defined TTL or {@link CollectionsManifest.CollectionInfo#MAX_TTL_UNDEFINED}
     */
    public long getMaxTtl() {
        return maxTtl;
    }

    @Override
    public CollectionsManifest apply(CollectionsManifest currentManifest) {
        return currentManifest.withCollection(newManifestUid, scopeId, collectionId, collectionName, maxTtl);
    }

    @Override
    public String toString() {
        return "CollectionCreated{" + "newManifestUid=0x" + Long.toUnsignedString(newManifestUid, 16) + ", scopeId=0x"
                + Integer.toUnsignedString(scopeId, 16) + ", collectionId=" + CollectionsUtil.displayCid(collectionId)
                + ", collectionName='" + collectionName + '\'' + ", maxTtl=" + maxTtl + ", vbucket=" + getVbucket()
                + ", seqno=" + getSeqno() + ", version=" + getVersion() + '}';
    }
}
