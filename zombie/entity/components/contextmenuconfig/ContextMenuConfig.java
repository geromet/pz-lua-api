/*
 * Decompiled with CFR 0.152.
 */
package zombie.entity.components.contextmenuconfig;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import zombie.UsedFromLua;
import zombie.core.network.ByteBufferReader;
import zombie.debug.objects.DebugClassFields;
import zombie.entity.Component;
import zombie.entity.ComponentType;
import zombie.entity.network.EntityPacketType;
import zombie.network.IConnection;
import zombie.scripting.entity.GameEntityScript;
import zombie.scripting.entity.components.contextmenuconfig.ContextMenuConfigScript;

@DebugClassFields
@UsedFromLua
public class ContextMenuConfig
extends Component {
    private ContextMenuConfig() {
        super(ComponentType.ContextMenuConfig);
    }

    @Override
    public boolean isQualifiesForMetaStorage() {
        return false;
    }

    public ArrayList<ContextMenuConfigScript.EntryScript> getEntries() {
        ContextMenuConfigScript compScript;
        GameEntityScript entityScript;
        if (this.owner.getEntityScript() != null && (entityScript = this.owner.getEntityScript()) != null && (compScript = (ContextMenuConfigScript)entityScript.getComponentScriptFor(ComponentType.ContextMenuConfig)) != null) {
            return compScript.getEntries();
        }
        return new ArrayList<ContextMenuConfigScript.EntryScript>();
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
}

