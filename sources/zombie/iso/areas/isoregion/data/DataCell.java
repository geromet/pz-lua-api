/*
 * Decompiled with CFR 0.152.
 */
package zombie.iso.areas.isoregion.data;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import zombie.UsedFromLua;
import zombie.iso.areas.isoregion.data.DataChunk;
import zombie.iso.areas.isoregion.data.DataRoot;

@UsedFromLua
public final class DataCell {
    public final DataRoot dataRoot;
    final Map<Integer, DataChunk> dataChunks = new HashMap<Integer, DataChunk>();

    DataCell(DataRoot dataRoot) {
        this.dataRoot = dataRoot;
    }

    private DataRoot getDataRoot() {
        return this.dataRoot;
    }

    DataChunk getChunk(int chunkID) {
        return this.dataChunks.get(chunkID);
    }

    DataChunk addChunk(int chunkX, int chunkY, int chunkID) {
        DataChunk chunk = new DataChunk(chunkX, chunkY, this, chunkID);
        this.dataChunks.put(chunkID, chunk);
        return chunk;
    }

    void setChunk(DataChunk chunk) {
        this.dataChunks.put(chunk.getHashId(), chunk);
    }

    void getAllChunks(List<DataChunk> list) {
        for (Map.Entry<Integer, DataChunk> entry : this.dataChunks.entrySet()) {
            list.add(entry.getValue());
        }
    }
}

