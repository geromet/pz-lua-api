/*
 * Decompiled with CFR 0.152.
 */
package zombie.iso;

import java.nio.ByteBuffer;
import zombie.UsedFromLua;
import zombie.core.network.ByteBufferReader;
import zombie.core.network.ByteBufferWriter;

@UsedFromLua
public final class SliceY {
    public static final ByteBuffer SliceBuffer = ByteBuffer.allocate(0xA00000);
    public static final ByteBufferReader sliceBufferReader = new ByteBufferReader(SliceBuffer);
    public static final ByteBufferWriter sliceBufferWriter = new ByteBufferWriter(SliceBuffer);
    public static final Object SliceBufferLock = "SliceY SliceBufferLock";
}

