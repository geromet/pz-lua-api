/*
 * Decompiled with CFR 0.152.
 */
package zombie.iso;

import java.util.ArrayList;
import java.util.List;
import se.krka.kahlua.vm.KahluaTable;
import zombie.IndieGL;
import zombie.UsedFromLua;
import zombie.characters.IsoPlayer;
import zombie.core.PerformanceSettings;
import zombie.core.math.PZMath;
import zombie.core.skinnedmodel.model.ItemModelRenderer;
import zombie.core.skinnedmodel.model.WorldItemModelDrawer;
import zombie.core.textures.Texture;
import zombie.debug.LineDrawer;
import zombie.inventory.InventoryItem;
import zombie.iso.IsoCamera;
import zombie.iso.IsoCell;
import zombie.iso.IsoGridSquare;
import zombie.iso.sprite.IsoSprite;
import zombie.network.GameServer;
import zombie.util.Type;

@UsedFromLua
public final class IsoMarkers {
    public static final IsoMarkers instance = new IsoMarkers();
    private static int nextIsoMarkerId;
    private final List<IsoMarker> markers = new ArrayList<IsoMarker>();

    private IsoMarkers() {
    }

    public void reset() {
        this.markers.clear();
    }

    public void update() {
        if (GameServer.server) {
            return;
        }
        if (IsoCamera.frameState.playerIndex != 0) {
            return;
        }
        if (this.markers.isEmpty()) {
            return;
        }
        for (int i = this.markers.size() - 1; i >= 0; --i) {
            if (!this.markers.get(i).isRemoved()) continue;
            this.markers.remove(i);
        }
    }

    public boolean removeIsoMarker(IsoMarker marker) {
        return this.removeIsoMarker(marker.getID());
    }

    public boolean removeIsoMarker(int id) {
        for (int i = this.markers.size() - 1; i >= 0; --i) {
            if (this.markers.get(i).getID() != id) continue;
            if (this.markers.get((int)i).item != null) {
                this.markers.get((int)i).item.setWorldAlpha(1.0f);
            }
            this.markers.get((int)i).isRemoved = true;
            return true;
        }
        return false;
    }

    public IsoMarker getIsoMarker(int id) {
        for (int i = 0; i < this.markers.size(); ++i) {
            if (this.markers.get(i).getID() != id) continue;
            return this.markers.get(i);
        }
        return null;
    }

    public IsoMarker addIsoMarker(String spriteName, IsoGridSquare gs, float r, float g, float b, float alpha) {
        if (GameServer.server) {
            return null;
        }
        IsoMarker m = new IsoMarker();
        m.setSquare(gs);
        m.init(spriteName, gs.x, gs.y, gs.z, gs);
        m.setColor(r, g, b, alpha);
        this.markers.add(m);
        return m;
    }

    public IsoMarker addIsoMarker(KahluaTable textureTable, IsoGridSquare gs, float r, float g, float b, float alpha) {
        if (GameServer.server) {
            return null;
        }
        IsoMarker m = new IsoMarker();
        m.init(textureTable, gs.x, gs.y, gs.z, gs);
        m.setSquare(gs);
        m.setColor(r, g, b, alpha);
        this.markers.add(m);
        return m;
    }

    public IsoMarker addIsoMarker(InventoryItem item, IsoGridSquare gs, float r, float g, float b, float alpha, float rotation) {
        if (GameServer.server) {
            return null;
        }
        IsoMarker m = new IsoMarker();
        m.init(item, gs.x, gs.y, gs.z, gs);
        m.setSquare(gs);
        m.setColor(r, g, b, alpha);
        m.setRotation(rotation);
        this.markers.add(m);
        return m;
    }

    public void renderIsoMarkers(IsoCell.PerPlayerRender perPlayerRender, int zLayer, int playerIndex) {
        if (GameServer.server || this.markers.isEmpty()) {
            return;
        }
        IsoPlayer player = IsoPlayer.players[playerIndex];
        if (player == null) {
            return;
        }
        if (PerformanceSettings.fboRenderChunk) {
            IndieGL.enableDepthTest();
            IndieGL.glBlendFunc(770, 771);
            for (int i = 0; i < this.markers.size(); ++i) {
                IsoMarker m = this.markers.get(i);
                if (m.circleSize > 0.0f) {
                    LineDrawer.DrawIsoCircle(m.x, m.y, m.z, m.circleSize, 32, m.r, m.g, m.b, m.a);
                }
                if (m.zLayer != zLayer || PZMath.fastfloor(m.z) != PZMath.fastfloor(player.getZ()) || !m.active) continue;
                for (int j = 0; j < m.textures.size(); ++j) {
                    ItemModelRenderer.RenderStatus status;
                    Texture texture = m.textures.get(j);
                    if (m.item != null && (status = WorldItemModelDrawer.renderMain(m.item, m.square, m.square, m.x, m.y, m.z, 0.0f, m.rotation, true)) != ItemModelRenderer.RenderStatus.NoModel) continue;
                    IsoSprite.renderTextureWithDepth(texture, texture.getWidth(), texture.getHeight(), m.r, m.g, m.b, m.a, m.x, m.y, m.z);
                }
            }
        }
    }

    public void render() {
        this.update();
    }

    @UsedFromLua
    public static final class IsoMarker {
        private final int id;
        private final ArrayList<Texture> textures = new ArrayList();
        private IsoGridSquare square;
        private float x;
        private float y;
        private float z;
        private int zLayer;
        private float r;
        private float g;
        private float b;
        private float a;
        private boolean active = true;
        private boolean isRemoved;
        private InventoryItem item;
        private float rotation;
        private float circleSize;

        public IsoMarker() {
            this.id = nextIsoMarkerId++;
        }

        public int getID() {
            return this.id;
        }

        public void remove() {
            this.isRemoved = true;
        }

        public boolean isRemoved() {
            return this.isRemoved;
        }

        public void init(KahluaTable textureTable, int x, int y, int z, IsoGridSquare gs) {
            this.square = gs;
            if (textureTable != null) {
                for (int i = 1; i <= textureTable.len(); ++i) {
                    String textureID = Type.tryCastTo(textureTable.rawget(i), String.class);
                    Texture texture = Texture.trygetTexture(textureID);
                    if (texture == null) continue;
                    this.textures.add(texture);
                    this.setPos(x, y, z);
                }
            }
        }

        public void init(String spriteName, int x, int y, int z, IsoGridSquare gs) {
            Texture texture;
            this.square = gs;
            if (spriteName != null && (texture = Texture.trygetTexture(spriteName)) != null) {
                this.textures.add(texture);
                this.setPos(x, y, z);
            }
        }

        public void init(InventoryItem item, int x, int y, int z, IsoGridSquare gs) {
            this.square = gs;
            if (item != null) {
                this.item = item;
                String str = item.getWorldTexture();
                try {
                    Texture tex = Texture.getSharedTexture(str);
                    if (tex == null) {
                        str = item.getTex().getName();
                    }
                }
                catch (Exception ex) {
                    str = "media/inventory/world/WItem_Sack.png";
                }
                Texture texture = Texture.getSharedTexture(str);
                if (texture == null) {
                    return;
                }
                this.textures.add(texture);
                this.setPos(x, y, z);
            }
        }

        public float getX() {
            return this.x;
        }

        public float getY() {
            return this.y;
        }

        public float getZ() {
            return this.z;
        }

        public float getR() {
            return this.r;
        }

        public float getG() {
            return this.g;
        }

        public float getB() {
            return this.b;
        }

        public float getA() {
            return this.a;
        }

        public void setR(float r) {
            this.r = r;
        }

        public void setG(float g) {
            this.g = g;
        }

        public void setB(float b) {
            this.b = b;
        }

        public void setA(float a) {
            this.a = a;
        }

        public void setAlpha(float alpha) {
            this.setA(alpha);
            if (this.item != null) {
                this.item.setWorldAlpha(alpha);
            }
        }

        public IsoGridSquare getSquare() {
            return this.square;
        }

        public void setSquare(IsoGridSquare square) {
            this.square = square;
        }

        public void setPos(int x, int y, int z) {
            this.x = (float)x + 0.5f;
            this.y = (float)y + 0.5f;
            this.z = (float)z + 0.01f;
            this.zLayer = z;
        }

        public boolean isActive() {
            return this.active;
        }

        public void setActive(boolean active) {
            this.active = active;
        }

        public void setRotation(float rotation) {
            this.rotation = rotation;
        }

        public float getCircleSize() {
            return this.circleSize;
        }

        public void setCircleSize(float size) {
            this.circleSize = size;
        }

        public void setColor(float r, float g, float b, float a) {
            this.setR(r);
            this.setG(g);
            this.setB(b);
            this.setA(a);
        }
    }
}

