/*
 * Decompiled with CFR 0.152.
 */
package zombie.entity.components.spriteconfig;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import zombie.UsedFromLua;
import zombie.core.network.ByteBufferReader;
import zombie.debug.DebugLog;
import zombie.debug.objects.DebugClassFields;
import zombie.entity.Component;
import zombie.entity.ComponentType;
import zombie.entity.components.spriteconfig.SpriteConfigManager;
import zombie.entity.network.EntityPacketType;
import zombie.iso.IsoGridSquare;
import zombie.iso.IsoObject;
import zombie.iso.IsoWorld;
import zombie.iso.sprite.IsoSprite;
import zombie.network.IConnection;
import zombie.scripting.entity.ComponentScript;
import zombie.scripting.entity.components.spriteconfig.SpriteConfigScript;
import zombie.world.ScriptsDictionary;

@DebugClassFields
@UsedFromLua
public class SpriteConfig
extends Component {
    private SpriteConfigScript configScript;
    private boolean wasLoadedAsMaster;
    private SpriteConfigManager.TileInfo tileInfo;
    private SpriteConfigManager.FaceInfo faceInfo;
    private SpriteConfigManager.ObjectInfo objectInfo;

    private SpriteConfig() {
        super(ComponentType.SpriteConfig);
    }

    protected void readFromScript(ComponentScript script) {
        super.readFromScript(script);
        this.configScript = (SpriteConfigScript)script;
    }

    @Override
    protected void onAddedToOwner() {
        this.initObjectInfo();
    }

    @Override
    protected void onRemovedFromOwner() {
        this.resetObjectInfo();
    }

    private void initObjectInfo() {
        this.resetObjectInfo();
        if (this.getOwner() == null || !(this.getOwner() instanceof IsoObject)) {
            return;
        }
        if (this.configScript != null) {
            IsoSprite sprite;
            this.objectInfo = SpriteConfigManager.GetObjectInfo(this.configScript.getName());
            if (this.objectInfo != null && (sprite = ((IsoObject)this.getOwner()).sprite) != null) {
                this.faceInfo = this.objectInfo.getFaceForSprite(sprite.name);
                if (this.faceInfo != null) {
                    this.tileInfo = this.faceInfo.getTileInfoForSprite(sprite.name);
                }
            }
        }
        if (!this.isValid()) {
            DebugLog.General.warn("Invalid SpriteConfig object! scripted object = " + (this.objectInfo != null ? this.objectInfo.getName() : "null"));
            this.resetObjectInfo();
        }
    }

    private void resetObjectInfo() {
        this.objectInfo = null;
        this.faceInfo = null;
        this.tileInfo = null;
    }

    @Override
    protected void reset() {
        super.reset();
        this.resetObjectInfo();
        this.wasLoadedAsMaster = false;
    }

    public SpriteConfigManager.TileInfo getTileInfo() {
        return this.tileInfo;
    }

    public SpriteConfigManager.FaceInfo getFaceInfo() {
        return this.faceInfo;
    }

    public SpriteConfigManager.ObjectInfo getObjectInfo() {
        return this.objectInfo;
    }

    @Override
    public boolean isValid() {
        if (super.isValid() && this.objectInfo != null && this.faceInfo != null && this.tileInfo != null) {
            return this.getOwner() != null && this.getOwner() instanceof IsoObject;
        }
        return false;
    }

    public boolean isCanRotate() {
        if (this.isValid()) {
            return this.objectInfo.canRotate();
        }
        return false;
    }

    public boolean isValidMultiSquare() {
        return this.isValid() && this.faceInfo.isMultiSquare();
    }

    public boolean isMultiSquareMaster() {
        return this.isValid() && this.tileInfo.isMaster();
    }

    public boolean isMultiSquareSlave() {
        return this.isValid() && !this.tileInfo.isMaster();
    }

    public int getMasterOffsetX() {
        return this.isValidMultiSquare() ? this.tileInfo.getMasterOffsetX() : 0;
    }

    public int getMasterOffsetY() {
        return this.isValidMultiSquare() ? this.tileInfo.getMasterOffsetY() : 0;
    }

    public int getMasterOffsetZ() {
        return this.isValidMultiSquare() ? this.tileInfo.getMasterOffsetZ() : 0;
    }

    public IsoObject getMultiSquareMaster() {
        if (!this.isValid()) {
            return null;
        }
        if (!this.faceInfo.isMultiSquare() || this.tileInfo.isMaster()) {
            IsoObject ownerObj;
            SpriteConfigManager.TileInfo masterTileInfo = this.faceInfo.getMasterTileInfo();
            return masterTileInfo.verifyObject(ownerObj = (IsoObject)this.getOwner()) ? ownerObj : null;
        }
        IsoObject ownerObj = (IsoObject)this.getOwner();
        IsoGridSquare sq = IsoWorld.instance.currentCell.getGridSquare(ownerObj.getX() + (float)this.tileInfo.getMasterOffsetX(), ownerObj.getY() + (float)this.tileInfo.getMasterOffsetY(), ownerObj.getZ() + (float)this.tileInfo.getMasterOffsetZ());
        SpriteConfigManager.TileInfo masterTileInfo = this.faceInfo.getMasterTileInfo();
        if (sq != null) {
            for (int i = 0; i < sq.getObjects().size(); ++i) {
                IsoObject obj = sq.getObjects().get(i);
                SpriteConfig config = obj.getSpriteConfig();
                if (config == null || !config.isMultiSquareMaster() || !masterTileInfo.verifyObject(obj)) continue;
                return obj;
            }
        }
        return null;
    }

    public boolean isMultiSquareFullyLoaded() {
        if (!this.isValidMultiSquare()) {
            return false;
        }
        return this.findAllMultiSquareObjects(null);
    }

    public boolean getAllMultiSquareObjects(ArrayList<IsoObject> outlist) {
        if (!this.isValid()) {
            return false;
        }
        if (!this.faceInfo.isMultiSquare()) {
            if (outlist != null) {
                outlist.add((IsoObject)this.getOwner());
            }
            return true;
        }
        return this.findAllMultiSquareObjects(outlist);
    }

    private boolean findAllMultiSquareObjects(ArrayList<IsoObject> outlist) {
        int cz;
        if (!this.isValid()) {
            return false;
        }
        if (!this.faceInfo.isMultiSquare()) {
            if (outlist != null) {
                outlist.add((IsoObject)this.getOwner());
            }
            return true;
        }
        IsoObject ownerObj = (IsoObject)this.getOwner();
        int cx = ownerObj.getSquare().getX() - this.tileInfo.getX();
        int cy = ownerObj.getSquare().getY() - this.tileInfo.getY();
        int z = cz = ownerObj.getSquare().getZ() - this.tileInfo.getZ();
        int tz = 0;
        while (z < cz + this.faceInfo.getzLayers()) {
            int x = cx;
            int tx = 0;
            while (x < cx + this.faceInfo.getWidth()) {
                int y = cy;
                int ty = 0;
                while (y < cy + this.faceInfo.getHeight()) {
                    SpriteConfigManager.TileInfo tileInfo = this.faceInfo.getTileInfo(tx, ty, tz);
                    if (!tileInfo.isEmpty()) {
                        IsoGridSquare sq = IsoWorld.instance.currentCell.getGridSquare(x, y, z);
                        if (sq == null) {
                            return false;
                        }
                        boolean hasMulti = false;
                        for (int i = 0; i < sq.getObjects().size(); ++i) {
                            IsoObject object = sq.getObjects().get(i);
                            if (!tileInfo.verifyObject(object) || object.getSpriteConfig() == null) continue;
                            if (outlist != null) {
                                outlist.add(object);
                            }
                            hasMulti = true;
                        }
                        if (!hasMulti) {
                            return false;
                        }
                    }
                    ++y;
                    ++ty;
                }
                ++x;
                ++tx;
            }
            ++z;
            ++tz;
        }
        return true;
    }

    public boolean isWasLoadedAsMaster() {
        return this.wasLoadedAsMaster;
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
            ScriptsDictionary.spriteConfigs.saveScript(output, this.configScript);
            output.putLong(this.configScript.getScriptVersion());
            output.put(this.isMultiSquareMaster() ? (byte)1 : 0);
        }
    }

    @Override
    protected void load(ByteBuffer input, int worldVersion) throws IOException {
        if (input.get() == 0) {
            DebugLog.General.error("Sprite config has no script saved.");
            return;
        }
        SpriteConfigScript script = ScriptsDictionary.spriteConfigs.loadScript(input, worldVersion);
        long scriptVersion = input.getLong();
        boolean bl = this.wasLoadedAsMaster = input.get() != 0;
        if (script != null) {
            this.readFromScript(script);
            if (script.getScriptVersion() != scriptVersion) {
                // empty if block
            }
        } else {
            DebugLog.General.error("Could not load script for sprite config.");
        }
    }
}

