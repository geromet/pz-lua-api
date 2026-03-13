/*
 * Decompiled with CFR 0.152.
 */
package zombie.entity.components.script;

import java.io.IOException;
import java.nio.ByteBuffer;
import zombie.GameWindow;
import zombie.UsedFromLua;
import zombie.core.network.ByteBufferReader;
import zombie.debug.objects.DebugClassFields;
import zombie.entity.Component;
import zombie.entity.ComponentType;
import zombie.entity.network.EntityPacketType;
import zombie.network.IConnection;
import zombie.scripting.ScriptManager;
import zombie.scripting.entity.GameEntityScript;
import zombie.scripting.objects.Item;

@DebugClassFields
@UsedFromLua
public class EntityScriptInfo
extends Component {
    private GameEntityScript script;
    private String originalScript;
    private boolean originalIsItem;

    private EntityScriptInfo() {
        super(ComponentType.Script);
    }

    public void setOriginalScript(GameEntityScript entityScript) {
        this.originalIsItem = entityScript instanceof Item;
        this.originalScript = entityScript.getScriptObjectFullType();
        this.script = entityScript;
    }

    public boolean isOriginalIsItem() {
        return this.originalIsItem;
    }

    public String getOriginalScript() {
        return this.originalScript;
    }

    public GameEntityScript getScript() {
        return this.script;
    }

    @Override
    protected boolean onReceivePacket(ByteBufferReader input, EntityPacketType type, IConnection senderConnection) throws IOException {
        switch (type) {
            default: 
        }
        return false;
    }

    @Override
    protected void saveSyncData(ByteBuffer output) throws IOException {
        this.save(output);
    }

    @Override
    protected void loadSyncData(ByteBuffer input) throws IOException {
        this.load(input, 244);
    }

    @Override
    protected void save(ByteBuffer output) throws IOException {
        super.save(output);
        output.put(this.originalIsItem ? (byte)1 : 0);
        output.put(this.originalScript != null ? (byte)1 : 0);
        if (this.originalScript != null) {
            GameWindow.WriteString(output, this.originalScript);
        }
    }

    @Override
    protected void load(ByteBuffer input, int worldVersion) throws IOException {
        super.load(input, worldVersion);
        this.originalIsItem = input.get() != 0;
        this.originalScript = input.get() != 0 ? GameWindow.ReadString(input) : null;
        if (this.originalScript != null) {
            this.script = this.originalIsItem ? ScriptManager.instance.getItem(this.originalScript) : ScriptManager.instance.getGameEntityScript(this.originalScript);
        }
    }
}

