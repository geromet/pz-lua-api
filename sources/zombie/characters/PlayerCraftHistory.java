/*
 * Decompiled with CFR 0.152.
 */
package zombie.characters;

import java.nio.ByteBuffer;
import java.util.HashMap;
import zombie.UsedFromLua;
import zombie.characters.IsoPlayer;
import zombie.debug.DebugType;

@UsedFromLua
public class PlayerCraftHistory {
    private final IsoPlayer player;
    private final HashMap<String, CraftHistoryEntry> craftHistory;
    private static final CraftHistoryEntry craftHistoryDefaultEntry = new CraftHistoryEntry();

    public PlayerCraftHistory(IsoPlayer player) {
        if (player == null) {
            throw new NullPointerException();
        }
        this.player = player;
        this.craftHistory = new HashMap();
    }

    public CraftHistoryEntry getCraftHistoryFor(String craftType) {
        if (this.craftHistory.containsKey(craftType)) {
            return this.craftHistory.get(craftType);
        }
        return craftHistoryDefaultEntry;
    }

    public void addCraftHistoryCraftedEvent(String craftType) {
        CraftHistoryEntry entry = this.craftHistory.get(craftType);
        if (entry == null) {
            entry = new CraftHistoryEntry();
            this.craftHistory.put(craftType, entry);
        }
        ++entry.craftCount;
        entry.lastCraftTime = this.player.getHoursSurvived();
        DebugType.CraftLogic.println("PlayerCraftHistory updated: %s (craftCount: %d, lastCraftTime %f)", craftType, entry.craftCount, entry.lastCraftTime);
    }

    public void save(ByteBuffer output) {
        output.putInt(this.craftHistory.size());
        for (String key : this.craftHistory.keySet()) {
            output.putInt(key.length());
            for (int i = 0; i < key.length(); ++i) {
                output.putChar(key.charAt(i));
            }
            CraftHistoryEntry value = this.craftHistory.get(key);
            output.putInt(value.getCraftCount());
            output.putDouble(value.getLastCraftTime());
        }
    }

    public void load(ByteBuffer input) {
        this.craftHistory.clear();
        int entryCount = input.getInt();
        for (int i = 0; i < entryCount; ++i) {
            int keyCharCount = input.getInt();
            char[] keyChars = new char[keyCharCount];
            for (int j = 0; j < keyCharCount; ++j) {
                keyChars[j] = input.getChar();
            }
            String key = String.valueOf(keyChars);
            CraftHistoryEntry value = new CraftHistoryEntry();
            value.craftCount = input.getInt();
            value.lastCraftTime = input.getDouble();
            this.craftHistory.put(key, value);
        }
    }

    public static final class CraftHistoryEntry {
        private int craftCount;
        private double lastCraftTime;

        public int getCraftCount() {
            return this.craftCount;
        }

        public double getLastCraftTime() {
            return this.lastCraftTime;
        }
    }
}

