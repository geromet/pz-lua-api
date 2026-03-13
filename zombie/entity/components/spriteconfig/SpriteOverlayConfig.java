/*
 * Decompiled with CFR 0.152.
 */
package zombie.entity.components.spriteconfig;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import zombie.GameWindow;
import zombie.UsedFromLua;
import zombie.core.network.ByteBufferReader;
import zombie.debug.DebugLog;
import zombie.debug.objects.DebugClassFields;
import zombie.entity.Component;
import zombie.entity.ComponentType;
import zombie.entity.components.spriteconfig.SpriteConfig;
import zombie.entity.components.spriteconfig.SpriteConfigManager;
import zombie.entity.events.EntityEvent;
import zombie.entity.network.EntityPacketType;
import zombie.iso.IsoObject;
import zombie.network.IConnection;
import zombie.scripting.entity.ComponentScript;
import zombie.scripting.entity.components.spriteconfig.SpriteOverlayConfigScript;
import zombie.world.ScriptsDictionary;

@DebugClassFields
@UsedFromLua
public class SpriteOverlayConfig
extends Component {
    private SpriteOverlayConfigScript configScript;
    private String appliedStyle;

    private SpriteOverlayConfig() {
        super(ComponentType.SpriteOverlayConfig);
    }

    public ArrayList<String> getAvailableStyles() {
        return this.configScript.getAllStyleNames();
    }

    public boolean hasStyle(String style) {
        return this.configScript.getStyle(style) != null;
    }

    public void clearStyle() {
        this.setStyle(null);
    }

    public void setStyle(String style) {
        if (style == null && this.appliedStyle == null || style != null && style.equals(this.appliedStyle)) {
            return;
        }
        if (style == null || this.getAvailableStyles().contains(style)) {
            this.appliedStyle = style;
        } else {
            DebugLog.General.error("SpriteOverlay style: %s not found.", style);
            this.appliedStyle = null;
        }
        this.updateStyle();
    }

    private void updateStyle() {
        if (this.getGameEntity() == null || this.getGameEntity().isMeta()) {
            return;
        }
        SpriteConfig spriteConfig = (SpriteConfig)this.getComponent(ComponentType.SpriteConfig);
        if (spriteConfig == null) {
            return;
        }
        SpriteConfigManager.FaceInfo face = spriteConfig.getFaceInfo();
        SpriteConfigManager.TileInfo tile = spriteConfig.getTileInfo();
        if (face != null && tile != null) {
            SpriteOverlayConfigScript.OverlayStyle style = this.configScript.getStyle(this.appliedStyle);
            SpriteOverlayConfigScript.FaceScript overlayFace = null;
            if (style != null) {
                overlayFace = style.getFace(SpriteConfigManager.GetFaceIdForString(face.getFaceName()));
            }
            ArrayList<IsoObject> isoObjects = new ArrayList<IsoObject>();
            spriteConfig.getAllMultiSquareObjects(isoObjects);
            for (IsoObject isoObject : isoObjects) {
                SpriteConfig isoObjectSpriteConfig = (SpriteConfig)isoObject.getComponent(ComponentType.SpriteConfig);
                int x = -isoObjectSpriteConfig.getMasterOffsetX();
                int y = -isoObjectSpriteConfig.getMasterOffsetY();
                int z = -isoObjectSpriteConfig.getMasterOffsetZ();
                String tileName = null;
                if (overlayFace != null) {
                    SpriteOverlayConfigScript.TileScript overlayTile = overlayFace.getLayer(z).getRow(y).getTile(x);
                    tileName = overlayTile.getTileName();
                }
                isoObject.setOverlaySprite(tileName);
            }
        } else {
            DebugLog.General.error("Unable to apply SpriteOverlay. SpriteConfig FaceInfo or TileInfo not found.");
        }
    }

    @Override
    protected void onEntityEvent(EntityEvent event) {
        if (event.getEntity() == this.getGameEntity()) {
            switch (event.getEventType()) {
                case AddedToWorld: {
                    this.updateStyle();
                }
            }
        }
    }

    protected void readFromScript(ComponentScript script) {
        super.readFromScript(script);
        this.configScript = (SpriteOverlayConfigScript)script;
    }

    @Override
    protected void reset() {
        super.reset();
        this.appliedStyle = null;
    }

    @Override
    public boolean isValid() {
        if (super.isValid()) {
            return this.getOwner() != null && this.getOwner() instanceof IsoObject && this.getOwner().hasComponent(ComponentType.SpriteConfig);
        }
        return false;
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
        output.put(this.configScript != null ? (byte)1 : 0);
        if (this.configScript != null) {
            ScriptsDictionary.spriteOverlayConfigs.saveScript(output, this.configScript);
            output.putLong(this.configScript.getScriptVersion());
        }
        output.put(this.appliedStyle != null ? (byte)1 : 0);
        if (this.appliedStyle != null) {
            GameWindow.WriteString(output, this.appliedStyle);
        }
    }

    @Override
    protected void load(ByteBuffer input, int worldVersion) throws IOException {
        if (input.get() == 0) {
            DebugLog.General.error("Sprite config has no script saved.");
            return;
        }
        SpriteOverlayConfigScript script = ScriptsDictionary.spriteOverlayConfigs.loadScript(input, worldVersion);
        long scriptVersion = input.getLong();
        if (script != null) {
            this.readFromScript(script);
            if (script.getScriptVersion() != scriptVersion) {
                // empty if block
            }
        } else {
            DebugLog.General.error("Could not load script for sprite config.");
        }
        this.appliedStyle = null;
        if (input.get() != 0) {
            this.setStyle(GameWindow.ReadString(input));
        }
    }
}

