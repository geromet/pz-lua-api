/*
 * Decompiled with CFR 0.152.
 */
package zombie.ui;

import java.util.Objects;
import org.lwjgl.opengl.GL11;
import se.krka.kahlua.vm.KahluaTable;
import zombie.UsedFromLua;
import zombie.characters.IsoGameCharacter;
import zombie.characters.SurvivorDesc;
import zombie.core.Core;
import zombie.core.SpriteRenderer;
import zombie.core.Styles.UIFBOStyle;
import zombie.core.random.Rand;
import zombie.core.skinnedmodel.advancedanimation.AnimatedModel;
import zombie.core.skinnedmodel.population.IClothingItemListener;
import zombie.core.skinnedmodel.population.OutfitManager;
import zombie.core.textures.TextureDraw;
import zombie.iso.IsoDirections;
import zombie.ui.UIElement;
import zombie.util.StringUtils;

@UsedFromLua
public final class UI3DModel
extends UIElement
implements IClothingItemListener {
    private final AnimatedModel animatedModel = new AnimatedModel();
    private IsoDirections dir = IsoDirections.E;
    private boolean doExt;
    private long nextExt = -1L;
    private final Drawer[] drawers = new Drawer[3];
    private float zoom;
    private float yOffset;
    private float xOffset;

    public UI3DModel(KahluaTable table) {
        super(table);
        for (int i = 0; i < this.drawers.length; ++i) {
            this.drawers[i] = new Drawer(this);
        }
        if (OutfitManager.instance != null) {
            OutfitManager.instance.addClothingItemListener(this);
        }
    }

    @Override
    public void render() {
        if (!this.isVisible().booleanValue()) {
            return;
        }
        super.render();
        if (this.parent != null && this.parent.maxDrawHeight != -1 && (double)this.parent.maxDrawHeight <= this.y) {
            return;
        }
        if (this.doExt) {
            long now = System.currentTimeMillis();
            if (this.nextExt < 0L) {
                this.nextExt = now + (long)Rand.Next(5000, 10000);
            }
            if (this.nextExt < now) {
                this.animatedModel.getActionContext().reportEvent("EventDoExt");
                this.animatedModel.setVariable("Ext", Rand.Next(0, 6) + 1);
                this.nextExt = -1L;
            }
        }
        this.animatedModel.update();
        Drawer drawer = this.drawers[SpriteRenderer.instance.getMainStateIndex()];
        drawer.init(this.getAbsoluteX().intValue(), this.getAbsoluteY().intValue());
        SpriteRenderer.instance.drawGeneric(drawer);
    }

    public void setDirection(IsoDirections dir) {
        this.dir = dir;
        if (dir != null) {
            this.animatedModel.setAngle(dir.ToVector());
        }
    }

    public IsoDirections getDirection() {
        return this.dir;
    }

    public void setAnimate(boolean animate) {
        this.animatedModel.setAnimate(animate);
    }

    public void setAnimSetName(String name) {
        this.animatedModel.setAnimSetName(name);
    }

    public void setDoRandomExtAnimations(boolean doExt) {
        this.doExt = doExt;
    }

    public void setIsometric(boolean iso) {
        this.animatedModel.setIsometric(iso);
    }

    public void setOutfitName(String outfitName, boolean female, boolean zombie) {
        this.animatedModel.setOutfitName(outfitName, female, zombie);
    }

    public void setCharacter(IsoGameCharacter character) {
        this.animatedModel.setCharacter(character);
    }

    public IsoGameCharacter getCharacter() {
        return this.animatedModel.getCharacter();
    }

    public void setSurvivorDesc(SurvivorDesc survivorDesc) {
        this.animatedModel.setSurvivorDesc(survivorDesc);
    }

    public void setState(String state) {
        this.animatedModel.setState(state);
    }

    public String getState() {
        return this.animatedModel.getState();
    }

    public void setVariable(String key, String value) {
        this.animatedModel.setVariable(key, value);
    }

    public void setVariable(String key, boolean value) {
        this.animatedModel.setVariable(key, value);
    }

    public Object getVariable(String key) {
        return this.animatedModel.getVariable(key);
    }

    public void setVariable(String key, float value) {
        this.animatedModel.setVariable(key, value);
    }

    public void clearVariable(String key) {
        this.animatedModel.clearVariable(key);
    }

    public void clearVariables() {
        this.animatedModel.clearVariables();
    }

    public void reportEvent(String event) {
        if (StringUtils.isNullOrWhitespace(event)) {
            return;
        }
        this.animatedModel.getActionContext().reportEvent(event);
    }

    @Override
    public void clothingItemChanged(String itemGuid) {
        this.animatedModel.clothingItemChanged(itemGuid);
    }

    public void setZoom(float newZoom) {
        this.zoom = newZoom;
    }

    public void setYOffset(float newYOffset) {
        this.yOffset = newYOffset;
    }

    public void setXOffset(float newXOffset) {
        this.xOffset = newXOffset;
    }

    private final class Drawer
    extends TextureDraw.GenericDrawer {
        int absX;
        int absY;
        float animPlayerAngle;
        float zoom;
        boolean rendered;
        final /* synthetic */ UI3DModel this$0;

        private Drawer(UI3DModel uI3DModel) {
            UI3DModel uI3DModel2 = uI3DModel;
            Objects.requireNonNull(uI3DModel2);
            this.this$0 = uI3DModel2;
        }

        public void init(int x, int y) {
            float newyOffset;
            this.absX = x;
            this.absY = y;
            this.animPlayerAngle = this.this$0.animatedModel.getAnimationPlayer().getRenderedAngle();
            this.zoom = this.this$0.zoom;
            this.rendered = false;
            float f = newyOffset = this.this$0.animatedModel.isIsometric() ? -0.45f : -0.5f;
            if (this.this$0.yOffset != 0.0f) {
                newyOffset = this.this$0.yOffset;
            }
            this.this$0.animatedModel.setOffset(this.this$0.xOffset, newyOffset, 0.0f);
            this.this$0.animatedModel.renderMain();
        }

        @Override
        public void render() {
            float size = this.this$0.animatedModel.isIsometric() ? 22.0f : 25.0f;
            GL11.glEnable(2929);
            GL11.glDepthMask(true);
            GL11.glClearDepth(1.0);
            this.this$0.animatedModel.DoRender(this.absX, Core.height - this.absY - (int)this.this$0.height, (int)this.this$0.width, (int)this.this$0.height, size -= this.zoom, this.animPlayerAngle);
            UIFBOStyle.instance.setupState();
            this.rendered = true;
        }

        @Override
        public void postRender() {
            this.this$0.animatedModel.postRender(this.rendered);
        }
    }
}

