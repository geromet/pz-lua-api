/*
 * Decompiled with CFR 0.152.
 */
package zombie.radio;

import java.util.Map;
import se.krka.kahlua.vm.KahluaTable;
import zombie.Lua.LuaManager;
import zombie.UsedFromLua;
import zombie.radio.ZomboidRadio;

@UsedFromLua
public final class RadioAPI {
    private static RadioAPI instance;

    public static int timeToTimeStamp(int days, int hours, int minutes) {
        return days * 24 + hours * 60 + minutes;
    }

    public static int timeStampToDays(int stamp) {
        return stamp / 1440;
    }

    public static int timeStampToHours(int stamp) {
        return stamp / 60 % 24;
    }

    public static int timeStampToMinutes(int stamp) {
        return stamp % 60;
    }

    public static boolean hasInstance() {
        return instance != null;
    }

    public static RadioAPI getInstance() {
        if (instance == null) {
            instance = new RadioAPI();
        }
        return instance;
    }

    private RadioAPI() {
    }

    public KahluaTable getChannels(String category) {
        Map<Integer, String> channels = ZomboidRadio.getInstance().GetChannelList(category);
        KahluaTable table = LuaManager.platform.newTable();
        if (channels != null) {
            for (Map.Entry<Integer, String> entry : channels.entrySet()) {
                table.rawset(entry.getKey(), (Object)entry.getValue());
            }
        }
        return table;
    }
}

