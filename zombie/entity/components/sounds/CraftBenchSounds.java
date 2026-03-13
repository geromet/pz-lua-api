/*
 * Decompiled with CFR 0.152.
 */
package zombie.entity.components.sounds;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import zombie.UsedFromLua;
import zombie.core.network.ByteBufferReader;
import zombie.entity.Component;
import zombie.entity.ComponentType;
import zombie.entity.components.sounds.CraftBenchSound;
import zombie.entity.network.EntityPacketType;
import zombie.network.IConnection;
import zombie.scripting.entity.ComponentScript;
import zombie.scripting.entity.GameEntityScript;
import zombie.scripting.entity.components.sound.CraftBenchSoundsScript;
import zombie.util.StringUtils;

@UsedFromLua
public class CraftBenchSounds
extends Component {
    private static final int MATCH_ANY = -1;
    private final ArrayList<CraftBenchSound> sounds = new ArrayList();

    protected CraftBenchSounds() {
        super(ComponentType.CraftBenchSounds);
    }

    protected void readFromScript(ComponentScript componentScript) {
        super.readFromScript(componentScript);
        CraftBenchSoundsScript script = (CraftBenchSoundsScript)componentScript;
        this.sounds.clear();
        this.sounds.addAll(script.getSounds());
    }

    @Override
    protected boolean onReceivePacket(ByteBufferReader input, EntityPacketType type, IConnection senderConnection) throws IOException {
        return false;
    }

    @Override
    protected void saveSyncData(ByteBuffer output) throws IOException {
    }

    @Override
    protected void loadSyncData(ByteBuffer input) throws IOException {
    }

    private int indexOf(String id, String param1) {
        CraftBenchSoundsScript craftBenchSounds;
        GameEntityScript script;
        if (this.sounds.isEmpty() && (script = this.getOwner().getEntityScript()) != null && (craftBenchSounds = (CraftBenchSoundsScript)script.getComponentScriptFor(ComponentType.CraftBenchSounds)) != null) {
            this.readFromScript(craftBenchSounds);
        }
        for (int i = 0; i < this.sounds.size(); ++i) {
            CraftBenchSound sound = this.sounds.get(i);
            if (!sound.id.equalsIgnoreCase(id) || !StringUtils.equalsIgnoreCase(sound.param1, param1)) continue;
            return i;
        }
        return -1;
    }

    public String getSoundName(String id, String param1) {
        int index = this.indexOf(id, param1);
        if (index == -1) {
            return null;
        }
        return this.sounds.get((int)index).gameSound;
    }
}

