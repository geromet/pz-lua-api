/*
 * Decompiled with CFR 0.152.
 */
package zombie.core.skinnedmodel;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Objects;
import java.util.Stack;
import org.joml.Vector3f;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL13;
import org.lwjgl.opengl.GL20;
import org.lwjgl.util.vector.Matrix4f;
import zombie.characters.AttachedItems.AttachedModelName;
import zombie.characters.AttachedItems.AttachedModelNames;
import zombie.characters.IsoPlayer;
import zombie.characters.IsoZombie;
import zombie.core.Core;
import zombie.core.ImmutableColor;
import zombie.core.PerformanceSettings;
import zombie.core.ShaderHelper;
import zombie.core.SpriteRenderer;
import zombie.core.logger.ExceptionLogger;
import zombie.core.math.PZMath;
import zombie.core.opengl.GLStateRenderThread;
import zombie.core.opengl.RenderThread;
import zombie.core.opengl.Shader;
import zombie.core.opengl.ShaderProgram;
import zombie.core.opengl.VBORenderer;
import zombie.core.skinnedmodel.CharacterTextures;
import zombie.core.skinnedmodel.IGrappleable;
import zombie.core.skinnedmodel.ModelManager;
import zombie.core.skinnedmodel.advancedanimation.AnimatedModel;
import zombie.core.skinnedmodel.advancedanimation.IAnimatable;
import zombie.core.skinnedmodel.animation.AnimationPlayer;
import zombie.core.skinnedmodel.animation.AnimationTrack;
import zombie.core.skinnedmodel.animation.BoneTransform;
import zombie.core.skinnedmodel.animation.TwistableBoneTransform;
import zombie.core.skinnedmodel.model.Model;
import zombie.core.skinnedmodel.model.SkinningBone;
import zombie.core.skinnedmodel.population.ClothingItem;
import zombie.core.skinnedmodel.visual.AnimalVisual;
import zombie.core.skinnedmodel.visual.BaseVisual;
import zombie.core.skinnedmodel.visual.HumanVisual;
import zombie.core.skinnedmodel.visual.IHumanVisual;
import zombie.core.skinnedmodel.visual.ItemVisual;
import zombie.core.skinnedmodel.visual.ItemVisuals;
import zombie.core.textures.ColorInfo;
import zombie.core.textures.Texture;
import zombie.core.textures.TextureDraw;
import zombie.core.textures.TextureFBO;
import zombie.debug.DebugOptions;
import zombie.interfaces.ITexture;
import zombie.iso.IsoDepthHelper;
import zombie.iso.IsoDirections;
import zombie.iso.IsoGridSquare;
import zombie.iso.IsoObject;
import zombie.iso.IsoObjectPicker;
import zombie.iso.PlayerCamera;
import zombie.iso.Vector2;
import zombie.iso.objects.IsoDeadBody;
import zombie.iso.objects.IsoMannequin;
import zombie.iso.objects.ShadowParams;
import zombie.popman.ObjectPool;
import zombie.util.Pool;
import zombie.util.StringUtils;
import zombie.util.Type;
import zombie.util.list.PZArrayUtil;
import zombie.vehicles.UI3DScene;

public final class DeadBodyAtlas {
    public static final int ATLAS_SIZE = 1024;
    private TextureFBO fbo;
    public static final DeadBodyAtlas instance = new DeadBodyAtlas();
    private static final Vector2 tempVector2 = new Vector2();
    private final HashMap<String, BodyTexture> entryMap = new HashMap();
    private final ArrayList<Atlas> atlasList = new ArrayList();
    private final BodyParams bodyParams = new BodyParams();
    private int updateCounter = -1;
    private final Checksummer checksummer = new Checksummer();
    private static final Stack<RenderJob> JobPool = new Stack();
    private final DebugDrawInWorld[] debugDrawInWorld = new DebugDrawInWorld[3];
    private long debugDrawTime;
    private final ArrayList<RenderJob> renderJobs = new ArrayList();
    private final CharacterTextureVisual characterTextureVisualFemale = new CharacterTextureVisual(true);
    private final CharacterTextureVisual characterTextureVisualMale = new CharacterTextureVisual(false);
    private final CharacterTextures characterTexturesFemale = new CharacterTextures();
    private final CharacterTextures characterTexturesMale = new CharacterTextures();
    private final ObjectPool<BodyTextureDrawer> bodyTextureDrawerPool = new ObjectPool<BodyTextureDrawer>(BodyTextureDrawer::new);
    private static final ObjectPool<BodyTextureDepthDrawer> s_BodyTextureDepthDrawerPool = new ObjectPool<BodyTextureDepthDrawer>(BodyTextureDepthDrawer::new);
    public static Shader deadBodyAtlasShader;

    public void lightingUpdate(int updateCounter, boolean lightsChanged) {
        if (updateCounter != this.updateCounter && lightsChanged) {
            this.updateCounter = updateCounter;
        }
    }

    public BodyTexture getBodyTexture(IsoDeadBody body) {
        this.bodyParams.init(body);
        return this.getBodyTexture(this.bodyParams);
    }

    public BodyTexture getBodyTexture(IsoZombie body) {
        this.bodyParams.init(body);
        return this.getBodyTexture(this.bodyParams);
    }

    public BodyTexture getBodyTexture(IsoMannequin body) {
        this.bodyParams.init(body);
        return this.getBodyTexture(this.bodyParams);
    }

    public BodyTexture getBodyTexture(boolean bFemale, String animSet, String stateName, IsoDirections dir, int frame, float trackTime) {
        CharacterTextures characterTextures = bFemale ? this.characterTexturesFemale : this.characterTexturesMale;
        BodyTexture texture = characterTextures.getTexture(animSet, stateName, dir, frame);
        if (texture != null) {
            return texture;
        }
        this.bodyParams.init(bFemale ? this.characterTextureVisualFemale : this.characterTextureVisualMale, dir, animSet, stateName, trackTime);
        this.bodyParams.variables.put("zombieWalkType", "1");
        BodyTexture tex = this.getBodyTexture(this.bodyParams);
        characterTextures.addTexture(animSet, stateName, dir, frame, tex);
        return tex;
    }

    public BodyTexture getBodyTexture(BodyParams body) {
        String key = this.getBodyKey(body);
        BodyTexture bodyTexture = this.entryMap.get(key);
        if (bodyTexture != null) {
            return bodyTexture;
        }
        AtlasEntry entry = new AtlasEntry();
        entry.key = key;
        entry.lightKey = this.getLightKey(body);
        entry.updateCounter = this.updateCounter;
        bodyTexture = new BodyTexture();
        bodyTexture.entry = entry;
        this.entryMap.put(key, bodyTexture);
        this.renderJobs.add(RenderJob.getNew().init(body, entry));
        return bodyTexture;
    }

    public void invalidateBodyTexture(BodyTexture bodyTexture, IsoDeadBody body) {
        if (bodyTexture == null || bodyTexture.entry == null || !bodyTexture.entry.ready || bodyTexture.entry.atlas.tex == null) {
            return;
        }
        this.bodyParams.init(body);
        bodyTexture.entry.ready = false;
        RenderJob job = RenderJob.getNew().init(this.bodyParams, bodyTexture.entry);
        job.clearThisSlotOnly = true;
        this.renderJobs.add(job);
    }

    public void checkLights(Texture entryTex, IsoDeadBody body) {
        if (entryTex == null) {
            return;
        }
        BodyTexture bodyTexture = this.entryMap.get(entryTex.getName());
        if (bodyTexture == null) {
            return;
        }
        AtlasEntry entry = bodyTexture.entry;
        if (entry == null || entry.tex != entryTex) {
            return;
        }
        if (entry.updateCounter == this.updateCounter) {
            return;
        }
        entry.updateCounter = this.updateCounter;
        this.bodyParams.init(body);
        String lightKey = this.getLightKey(this.bodyParams);
        if (entry.lightKey.equals(lightKey)) {
            return;
        }
        this.entryMap.remove(entry.key);
        entry.key = this.getBodyKey(this.bodyParams);
        entry.lightKey = lightKey;
        entryTex.setNameOnly(entry.key);
        this.entryMap.put(entry.key, bodyTexture);
        RenderJob job = RenderJob.getNew().init(this.bodyParams, entry);
        job.clearThisSlotOnly = true;
        this.renderJobs.add(job);
        this.render();
    }

    public void checkLights(Texture entryTex, IsoZombie body) {
        if (entryTex == null) {
            return;
        }
        BodyTexture bodyTexture = this.entryMap.get(entryTex.getName());
        if (bodyTexture == null) {
            return;
        }
        AtlasEntry entry = bodyTexture.entry;
        if (entry == null || entry.tex != entryTex) {
            return;
        }
        if (entry.updateCounter == this.updateCounter) {
            return;
        }
        entry.updateCounter = this.updateCounter;
        this.bodyParams.init(body);
        String lightKey = this.getLightKey(this.bodyParams);
        if (entry.lightKey.equals(lightKey)) {
            return;
        }
        this.entryMap.remove(entry.key);
        entry.key = this.getBodyKey(this.bodyParams);
        entry.lightKey = lightKey;
        entryTex.setNameOnly(entry.key);
        this.entryMap.put(entry.key, bodyTexture);
        RenderJob job = RenderJob.getNew().init(this.bodyParams, entry);
        job.clearThisSlotOnly = true;
        this.renderJobs.add(job);
        this.render();
    }

    private void assignEntryToAtlas(AtlasEntry entry, int entryW, int entryH) {
        if (entry.atlas != null) {
            return;
        }
        for (int i = 0; i < this.atlasList.size(); ++i) {
            Atlas atlas = this.atlasList.get(i);
            if (atlas.isFull() || atlas.entryWidth != entryW || atlas.entryHeight != entryH) continue;
            atlas.addEntry(entry);
            return;
        }
        Atlas atlas = new Atlas(this, 1024, 1024, entryW, entryH);
        atlas.addEntry(entry);
        this.atlasList.add(atlas);
    }

    private String getBodyKey(BodyParams body) {
        if (body.baseVisual == this.characterTextureVisualFemale.humanVisual) {
            return "SZF_" + body.animSetName + "_" + body.stateName + "_" + String.valueOf((Object)body.dir) + "_" + body.trackTime;
        }
        if (body.baseVisual == this.characterTextureVisualMale.humanVisual) {
            return "SZM_" + body.animSetName + "_" + body.stateName + "_" + String.valueOf((Object)body.dir) + "_" + body.trackTime;
        }
        try {
            this.checksummer.reset();
            BaseVisual baseVisual = body.baseVisual;
            if (baseVisual instanceof AnimalVisual) {
                AnimalVisual animalVisual = (AnimalVisual)baseVisual;
                this.checksummer.update((byte)body.dir.ordinal());
                this.checksummer.update((int)(PZMath.wrap(body.angle, 0.0f, (float)Math.PI * 2) * 57.295776f));
                Model model = body.baseVisual.getModel();
                if (model != null) {
                    this.checksummer.update(model.name);
                }
                this.checksummer.update(body.female);
                this.checksummer.update(body.skeleton);
                this.checksummer.update(body.animSetName);
                this.checksummer.update(body.stateName);
                this.checksummer.update((int)(animalVisual.getAnimalSize() * 100.0f));
                this.checksummer.update(animalVisual.getSkinTexture());
                for (int i = 0; i < body.attachedModelNames.size(); ++i) {
                    AttachedModelName amn = body.attachedModelNames.get(i);
                    this.checksummer.update(amn.attachmentNameSelf);
                    this.checksummer.update(amn.attachmentNameParent);
                    this.checksummer.update(amn.modelName);
                    this.checksummer.update((int)(amn.bloodLevel * 100.0f));
                }
                ItemVisuals itemVisuals = body.itemVisuals;
                for (int i = 0; i < itemVisuals.size(); ++i) {
                    ItemVisual itemVisual = (ItemVisual)itemVisuals.get(i);
                }
                this.checksummer.update(body.fallOnFront);
                this.checksummer.update(body.standing);
                this.checksummer.update(body.outside);
                this.checksummer.update(body.room);
                float ambientR = (float)((int)(body.ambient.r * 10.0f)) / 10.0f;
                this.checksummer.update((byte)(ambientR * 255.0f));
                float ambientG = (float)((int)(body.ambient.g * 10.0f)) / 10.0f;
                this.checksummer.update((byte)(ambientG * 255.0f));
                float ambientB = (float)((int)(body.ambient.b * 10.0f)) / 10.0f;
                this.checksummer.update((byte)(ambientB * 255.0f));
                this.checksummer.update((int)body.trackTime);
                for (int i = 0; i < body.lights.length; ++i) {
                    this.checksummer.update(body.lights[i], body.x, body.y, body.z);
                }
                return this.checksummer.checksumToString();
            }
            HumanVisual humanVisual = Type.tryCastTo(body.baseVisual, HumanVisual.class);
            this.checksummer.update((byte)body.dir.ordinal());
            this.checksummer.update((int)(PZMath.wrap(body.angle, 0.0f, (float)Math.PI * 2) * 57.295776f));
            this.checksummer.update(humanVisual.getHairModel());
            this.checksummer.update(humanVisual.getBeardModel());
            this.checksummer.update(humanVisual.getSkinColor());
            this.checksummer.update(humanVisual.getSkinTexture());
            this.checksummer.update((int)(humanVisual.getTotalBlood() * 100.0f));
            this.checksummer.update(body.primaryHandItem);
            this.checksummer.update(body.secondaryHandItem);
            for (int i = 0; i < body.attachedModelNames.size(); ++i) {
                AttachedModelName amn = body.attachedModelNames.get(i);
                this.checksummer.update(amn.attachmentNameSelf);
                this.checksummer.update(amn.attachmentNameParent);
                this.checksummer.update(amn.modelName);
                this.checksummer.update((int)(amn.bloodLevel * 100.0f));
            }
            this.checksummer.update(body.female);
            this.checksummer.update(body.zombie);
            this.checksummer.update(body.skeleton);
            this.checksummer.update(body.animSetName);
            this.checksummer.update(body.stateName);
            ItemVisuals itemVisuals = body.itemVisuals;
            for (int i = 0; i < itemVisuals.size(); ++i) {
                ItemVisual item = (ItemVisual)itemVisuals.get(i);
                ClothingItem clothingItem = item.getClothingItem();
                if (clothingItem == null) continue;
                this.checksummer.update(item.getBaseTexture(clothingItem));
                this.checksummer.update(item.getTextureChoice(clothingItem));
                this.checksummer.update(item.getTint(clothingItem));
                this.checksummer.update(clothingItem.getModel(humanVisual.isFemale()));
                this.checksummer.update((int)(item.getTotalBlood() * 100.0f));
            }
            this.checksummer.update(body.fallOnFront);
            this.checksummer.update(body.standing);
            this.checksummer.update(body.outside);
            this.checksummer.update(body.room);
            float ambientR = (float)((int)(body.ambient.r * 10.0f)) / 10.0f;
            this.checksummer.update((byte)(ambientR * 255.0f));
            float ambientG = (float)((int)(body.ambient.g * 10.0f)) / 10.0f;
            this.checksummer.update((byte)(ambientG * 255.0f));
            float ambientB = (float)((int)(body.ambient.b * 10.0f)) / 10.0f;
            this.checksummer.update((byte)(ambientB * 255.0f));
            this.checksummer.update((int)body.trackTime);
            for (int i = 0; i < body.lights.length; ++i) {
                this.checksummer.update(body.lights[i], body.x, body.y, body.z);
            }
            return this.checksummer.checksumToString();
        }
        catch (Throwable t) {
            ExceptionLogger.logException(t);
            return "bogus";
        }
    }

    private String getLightKey(BodyParams body) {
        try {
            this.checksummer.reset();
            this.checksummer.update(body.outside);
            this.checksummer.update(body.room);
            float ambientR = (float)((int)(body.ambient.r * 10.0f)) / 10.0f;
            this.checksummer.update((byte)(ambientR * 255.0f));
            float ambientG = (float)((int)(body.ambient.g * 10.0f)) / 10.0f;
            this.checksummer.update((byte)(ambientG * 255.0f));
            float ambientB = (float)((int)(body.ambient.b * 10.0f)) / 10.0f;
            this.checksummer.update((byte)(ambientB * 255.0f));
            for (int i = 0; i < body.lights.length; ++i) {
                this.checksummer.update(body.lights[i], body.x, body.y, body.z);
            }
            return this.checksummer.checksumToString();
        }
        catch (Throwable t) {
            ExceptionLogger.logException(t);
            return "bogus";
        }
    }

    public void render() {
        int i;
        for (i = 0; i < this.atlasList.size(); ++i) {
            Atlas atlas = this.atlasList.get(i);
            if (!atlas.clear) continue;
            SpriteRenderer.instance.drawGeneric(new ClearAtlasTexture(atlas));
        }
        if (this.renderJobs.isEmpty()) {
            return;
        }
        for (i = 0; i < this.renderJobs.size(); ++i) {
            RenderJob job = this.renderJobs.get(i);
            if (job.done == 1 && job.renderRefCount > 0) continue;
            if (job.done == 1 && job.renderRefCount == 0) {
                this.renderJobs.remove(i--);
                assert (!JobPool.contains(job));
                JobPool.push(job);
                continue;
            }
            if (!job.renderMain()) continue;
            ++job.renderRefCount;
            SpriteRenderer.instance.drawGeneric(job);
        }
    }

    public void renderDebug() {
    }

    public void renderUI() {
        if (Core.debug && DebugOptions.instance.deadBodyAtlas.render.getValue()) {
            boolean bDepth = false;
            int d = 512 / Core.tileScale;
            int x = 0;
            int y = 0;
            for (int i = 0; i < this.atlasList.size(); ++i) {
                Atlas atlas = this.atlasList.get(i);
                Texture tex = atlas.tex;
                SpriteRenderer.instance.renderi(null, x, y, d, d, 1.0f, 1.0f, 1.0f, 0.75f, null);
                SpriteRenderer.instance.renderi(tex, x, y, d, d, 1.0f, 1.0f, 1.0f, 1.0f, null);
                float scale = (float)d / (float)tex.getWidth();
                for (int xx = 0; xx <= tex.getWidth() / atlas.entryWidth; ++xx) {
                    SpriteRenderer.instance.renderline(null, (int)((float)x + (float)(xx * atlas.entryWidth) * scale), y, (int)((float)x + (float)(xx * atlas.entryWidth) * scale), y + d, 0.5f, 0.5f, 0.5f, 1.0f);
                }
                for (int yy = 0; yy <= tex.getHeight() / atlas.entryHeight; ++yy) {
                    SpriteRenderer.instance.renderline(null, x, (int)((float)(y + d) - (float)(yy * atlas.entryHeight) * scale), x + d, (int)((float)(y + d) - (float)(yy * atlas.entryHeight) * scale), 0.5f, 0.5f, 0.5f, 1.0f);
                }
                if ((y += d + 4) + d <= Core.getInstance().getScreenHeight()) continue;
                y = 0;
                x += d + 4;
            }
            TextureFBO fbo1 = ModelManager.instance.bitmap;
            ITexture tex = fbo1.getTexture();
            SpriteRenderer.instance.renderi(null, x, y, d, d, 1.0f, 1.0f, 1.0f, 1.0f, null);
            SpriteRenderer.instance.renderi((Texture)tex, x, y, d, d, 1.0f, 1.0f, 1.0f, 1.0f, null);
        }
    }

    public void Reset() {
        if (this.fbo != null) {
            this.fbo.destroyLeaveTexture();
            this.fbo = null;
        }
        this.atlasList.forEach(Atlas::Reset);
        this.atlasList.clear();
        this.entryMap.clear();
        this.characterTexturesFemale.clear();
        this.characterTexturesMale.clear();
        JobPool.forEach(RenderJob::Reset);
        JobPool.clear();
        this.renderJobs.clear();
    }

    private void toBodyAtlas(RenderJob job) {
        GL11.glPushAttrib(2048);
        if (this.fbo.getTexture() != job.entry.atlas.tex) {
            this.fbo.setTextureAndDepth(job.entry.atlas.tex, job.entry.atlas.depth);
        }
        this.fbo.startDrawing();
        GL11.glViewport(0, 0, this.fbo.getWidth(), this.fbo.getHeight());
        org.joml.Matrix4f projection = Core.getInstance().projectionMatrixStack.alloc();
        int texWid = job.entry.atlas.tex.getWidth();
        int texHgt = job.entry.atlas.tex.getHeight();
        projection.setOrtho2D(0.0f, texWid, texHgt, 0.0f);
        Core.getInstance().projectionMatrixStack.push(projection);
        org.joml.Matrix4f modelView = Core.getInstance().modelViewMatrixStack.alloc();
        modelView.identity();
        Core.getInstance().modelViewMatrixStack.push(modelView);
        GL11.glEnable(2929);
        GL11.glDepthFunc(519);
        GL11.glDepthMask(true);
        GL11.glEnable(3553);
        GL11.glDisable(3089);
        int program = GL11.glGetInteger(35725);
        GL20.glUseProgram(0);
        if (job.entry.atlas.clear) {
            GL11.glClearColor(0.0f, 0.0f, 0.0f, 0.0f);
            GL11.glClear(16640);
            GL11.glClearColor(0.0f, 0.0f, 0.0f, 1.0f);
            job.entry.atlas.clear = false;
        }
        GL11.glEnable(3089);
        GL11.glScissor(job.entry.x, 1024 - job.entry.y - job.entry.h, job.entry.w, job.entry.h);
        if (job.clearThisSlotOnly) {
            GL11.glClearColor(0.0f, 0.0f, 0.0f, 0.0f);
            GL11.glClear(16640);
            GL11.glClearColor(0.0f, 0.0f, 0.0f, 1.0f);
        }
        TextureFBO mmBitmap = ModelManager.instance.bitmap;
        int w = mmBitmap.getTexture().getWidth() / 8 * Core.tileScale;
        int h = mmBitmap.getTexture().getHeight() / 8 * Core.tileScale;
        int x = job.entry.x - (w - job.entry.atlas.entryWidth) / 2;
        int y = job.entry.y - (h - job.entry.atlas.entryHeight) / 2;
        if (deadBodyAtlasShader == null) {
            deadBodyAtlasShader = new DeadBodyAtlasShader("DeadBodyAtlas");
        }
        if (deadBodyAtlasShader.getShaderProgram().isCompiled()) {
            GL13.glActiveTexture(33985);
            GL11.glEnable(3553);
            GL11.glBindTexture(3553, mmBitmap.getDepthTexture().getID());
            GL13.glActiveTexture(33984);
            GL11.glDepthMask(true);
            GL11.glDepthFunc(519);
            VBORenderer vbor = VBORenderer.getInstance();
            vbor.startRun(vbor.formatPositionColorUv);
            vbor.setMode(7);
            vbor.setDepthTest(true);
            vbor.setShaderProgram(deadBodyAtlasShader.getShaderProgram());
            vbor.setTextureID(((Texture)mmBitmap.getTexture()).getTextureId());
            vbor.cmdUseProgram(deadBodyAtlasShader.getShaderProgram());
            vbor.cmdShader1f("zDepthBlendZ", 0.0f);
            vbor.cmdShader1f("zDepthBlendToZ", 1.0f);
            vbor.addQuad(x, y, 0.0f, 0.0f, x + w, y + h, 1.0f, 1.0f, 0.0f, 1.0f, 1.0f, 1.0f, 1.0f);
            vbor.endRun();
            vbor.flush();
            GL13.glActiveTexture(33985);
            GL11.glBindTexture(3553, 0);
            GL11.glDisable(3553);
            GL13.glActiveTexture(33984);
            Texture.lastTextureID = 0;
            GL11.glBindTexture(3553, 0);
        } else {
            mmBitmap.getTexture().bind();
            GL11.glBegin(7);
            GL11.glColor3f(1.0f, 1.0f, 1.0f);
            GL11.glTexCoord2f(0.0f, 0.0f);
            GL11.glVertex2i(x, y);
            GL11.glTexCoord2f(1.0f, 0.0f);
            GL11.glVertex2i(x + w, y);
            GL11.glTexCoord2f(1.0f, 1.0f);
            GL11.glVertex2i(x + w, y + h);
            GL11.glTexCoord2f(0.0f, 1.0f);
            GL11.glVertex2i(x, y + h);
            GL11.glEnd();
            Texture.lastTextureID = 0;
            GL11.glBindTexture(3553, 0);
        }
        int playerIndex = SpriteRenderer.instance.getRenderingPlayerIndex();
        int x1 = playerIndex == 0 || playerIndex == 2 ? 0 : Core.getInstance().getOffscreenTrueWidth() / 2;
        int y1 = playerIndex == 0 || playerIndex == 1 ? 0 : Core.getInstance().getOffscreenTrueHeight() / 2;
        int w1 = Core.getInstance().getOffscreenTrueWidth();
        int h1 = Core.getInstance().getOffscreenTrueHeight();
        if (IsoPlayer.numPlayers > 1) {
            w1 /= 2;
        }
        if (IsoPlayer.numPlayers > 2) {
            h1 /= 2;
        }
        GL11.glScissor(x1, y1, w1, h1);
        this.fbo.endDrawing();
        ShaderHelper.glUseProgramObjectARB(program);
        GL11.glEnable(3089);
        Core.getInstance().projectionMatrixStack.pop();
        Core.getInstance().modelViewMatrixStack.pop();
        GL11.glPopAttrib();
        job.entry.ready = true;
        job.done = 1;
    }

    public static final class BodyParams {
        public BaseVisual baseVisual;
        public final ItemVisuals itemVisuals = new ItemVisuals();
        public IsoDirections dir;
        public float angle;
        public boolean female;
        public boolean zombie;
        public boolean skeleton;
        public String animSetName;
        public String stateName;
        public final HashMap<String, String> variables = new HashMap();
        public boolean standing;
        public String primaryHandItem;
        public String secondaryHandItem;
        public final AttachedModelNames attachedModelNames = new AttachedModelNames();
        public float x;
        public float y;
        public float z;
        public float trackTime;
        public boolean outside;
        public boolean room;
        public final ColorInfo ambient = new ColorInfo();
        public boolean fallOnFront;
        public boolean killedByFall;
        public final IsoGridSquare.ResultLight[] lights = new IsoGridSquare.ResultLight[5];
        public IGrappleable grappleable;
        public TwistableBoneTransform[] diedBoneTransforms;
        boolean ragdollFall;

        public BodyParams() {
            for (int i = 0; i < this.lights.length; ++i) {
                this.lights[i] = new IsoGridSquare.ResultLight();
            }
        }

        public void init(BodyParams body) {
            this.baseVisual = body.baseVisual;
            this.itemVisuals.clear();
            PZArrayUtil.addAll(this.itemVisuals, body.itemVisuals);
            this.dir = body.dir;
            this.angle = body.angle;
            this.female = body.female;
            this.zombie = body.zombie;
            this.skeleton = body.skeleton;
            this.animSetName = body.animSetName;
            this.stateName = body.stateName;
            this.variables.clear();
            this.variables.putAll(body.variables);
            this.standing = body.standing;
            this.primaryHandItem = body.primaryHandItem;
            this.secondaryHandItem = body.secondaryHandItem;
            this.attachedModelNames.copyFrom(body.attachedModelNames);
            this.x = body.x;
            this.y = body.y;
            this.z = body.z;
            this.trackTime = body.trackTime;
            this.fallOnFront = body.fallOnFront;
            this.ragdollFall = body.ragdollFall;
            this.outside = body.outside;
            this.room = body.room;
            this.ambient.set(body.ambient.r, body.ambient.g, body.ambient.b, 1.0f);
            for (int i = 0; i < this.lights.length; ++i) {
                this.lights[i].copyFrom(body.lights[i]);
            }
            this.grappleable = body.grappleable;
            this.diedBoneTransforms = Pool.tryRelease(this.diedBoneTransforms);
            this.diedBoneTransforms = PZArrayUtil.clone(body.diedBoneTransforms, TwistableBoneTransform::alloc, TwistableBoneTransform::set);
        }

        public void init(IsoDeadBody body) {
            this.baseVisual = body.getVisual();
            body.getItemVisuals(this.itemVisuals);
            this.dir = body.dir;
            this.angle = body.getAngle();
            this.female = body.isFemale();
            this.zombie = body.isZombie();
            this.skeleton = body.isSkeleton();
            this.primaryHandItem = null;
            this.secondaryHandItem = null;
            this.attachedModelNames.initFrom(body.getAttachedItems());
            this.animSetName = "zombie";
            this.stateName = body.isBeingGrappled() ? "grappled" : "onground";
            this.variables.clear();
            this.standing = false;
            this.trackTime = 0.0f;
            if (body.getPrimaryHandItem() != null || body.getSecondaryHandItem() != null) {
                if (body.getPrimaryHandItem() != null && !StringUtils.isNullOrEmpty(body.getPrimaryHandItem().getStaticModel())) {
                    this.primaryHandItem = body.getPrimaryHandItem().getStaticModel();
                }
                if (body.getSecondaryHandItem() != null && !StringUtils.isNullOrEmpty(body.getSecondaryHandItem().getStaticModel())) {
                    this.secondaryHandItem = body.getSecondaryHandItem().getStaticModel();
                }
                this.animSetName = "player";
                this.stateName = "deadbody";
                if (body.isKilledByFall()) {
                    this.trackTime = 1.0f;
                }
            } else if (!body.isAnimal() && !body.isZombie() && body.isKilledByFall()) {
                this.animSetName = "player";
                this.stateName = "deadbody";
                this.trackTime = 1.0f;
            }
            if (body.isAnimal()) {
                this.animSetName = body.animalAnimSet;
                this.stateName = "deadbody";
            }
            this.x = body.getX();
            this.y = body.getY();
            this.z = body.getZ();
            this.fallOnFront = body.isFallOnFront();
            this.killedByFall = body.isKilledByFall();
            this.ragdollFall = body.ragdollFall;
            this.outside = body.square != null && body.square.isOutside();
            this.room = body.square != null && body.square.getRoom() != null;
            this.initAmbient(body.square);
            this.initLights(body.square);
            this.grappleable = body;
            this.initDeathPose(body);
        }

        public void init(IsoZombie body) {
            this.baseVisual = body.getHumanVisual();
            body.getItemVisuals(this.itemVisuals);
            this.dir = body.dir;
            this.angle = body.getAnimAngleRadians();
            this.female = body.isFemale();
            this.zombie = true;
            this.skeleton = body.isSkeleton();
            this.primaryHandItem = null;
            this.secondaryHandItem = null;
            this.attachedModelNames.initFrom(body.getAttachedItems());
            this.animSetName = "zombie";
            this.stateName = "onground";
            this.variables.clear();
            this.standing = false;
            this.x = body.getX();
            this.y = body.getY();
            this.z = body.getZ();
            this.trackTime = 0.0f;
            this.fallOnFront = body.isFallOnFront();
            this.killedByFall = body.isKilledByFall();
            this.ragdollFall = body.isRagdollFall();
            this.outside = body.getCurrentSquare() != null && body.getCurrentSquare().isOutside();
            this.room = body.getCurrentSquare() != null && body.getCurrentSquare().getRoom() != null;
            this.initAmbient(body.getCurrentSquare());
            this.initLights(body.getCurrentSquare());
            this.grappleable = body;
            this.initDeathPose(body);
        }

        public void init(IsoMannequin body) {
            this.baseVisual = body.getHumanVisual();
            body.getItemVisuals(this.itemVisuals);
            this.dir = body.dir;
            this.angle = this.dir.ToVector().getDirection();
            this.female = body.isFemale();
            this.zombie = body.isZombie();
            this.skeleton = body.isSkeleton();
            this.primaryHandItem = null;
            this.secondaryHandItem = null;
            this.attachedModelNames.clear();
            this.animSetName = body.getAnimSetName();
            this.stateName = body.getAnimStateName();
            this.variables.clear();
            body.getVariables(this.variables);
            this.standing = true;
            this.x = body.getX();
            this.y = body.getY();
            this.z = body.getZ();
            this.trackTime = 0.0f;
            this.fallOnFront = false;
            this.outside = body.square != null && body.square.isOutside();
            this.room = body.square != null && body.square.getRoom() != null;
            this.initAmbient(body.square);
            this.initLights(null);
            this.grappleable = null;
            this.initDeathPose(Type.tryCastTo(body, IAnimatable.class));
        }

        public void init(IHumanVisual iHumanVisual, IsoDirections dir, String animSet, String stateName, float trackTime) {
            this.baseVisual = iHumanVisual.getHumanVisual();
            iHumanVisual.getItemVisuals(this.itemVisuals);
            this.dir = dir;
            this.angle = dir.ToVector().getDirection();
            this.female = iHumanVisual.isFemale();
            this.zombie = iHumanVisual.isZombie();
            this.skeleton = iHumanVisual.isSkeleton();
            this.primaryHandItem = null;
            this.secondaryHandItem = null;
            this.attachedModelNames.clear();
            this.animSetName = animSet;
            this.stateName = stateName;
            this.variables.clear();
            this.standing = true;
            this.x = 0.0f;
            this.y = 0.0f;
            this.z = 0.0f;
            this.trackTime = trackTime;
            this.fallOnFront = false;
            this.outside = true;
            this.room = false;
            this.ambient.set(1.0f, 1.0f, 1.0f, 1.0f);
            this.initLights(null);
            this.grappleable = null;
            this.initDeathPose(Type.tryCastTo(iHumanVisual, IAnimatable.class));
        }

        private void initDeathPose(IsoDeadBody body) {
            this.diedBoneTransforms = Pool.tryRelease(this.diedBoneTransforms);
            if (body == null || PZArrayUtil.isNullOrEmpty(body.getDiedBoneTransforms())) {
                return;
            }
            this.diedBoneTransforms = PZArrayUtil.clone(body.getDiedBoneTransforms(), TwistableBoneTransform::alloc, TwistableBoneTransform::set);
        }

        private void initDeathPose(IAnimatable body) {
            this.diedBoneTransforms = Pool.tryRelease(this.diedBoneTransforms);
            if (body == null || !body.hasAnimationPlayer()) {
                return;
            }
            AnimationPlayer animationPlayer = body.getAnimationPlayer();
            if (animationPlayer.isBoneTransformsNeedFirstFrame()) {
                return;
            }
            int numberOfBones = animationPlayer.getNumBones();
            this.diedBoneTransforms = TwistableBoneTransform.allocArray(numberOfBones);
            for (int boneIndex = 0; boneIndex < numberOfBones; ++boneIndex) {
                animationPlayer.getBoneTransformAt(boneIndex, this.diedBoneTransforms[boneIndex]);
            }
        }

        void initAmbient(IsoGridSquare square) {
            this.ambient.set(1.0f, 1.0f, 1.0f, 1.0f);
        }

        void initLights(IsoGridSquare square) {
            for (int i = 0; i < this.lights.length; ++i) {
                this.lights[i].radius = 0;
            }
            if (square != null) {
                IsoGridSquare.ILighting lighting = square.lighting[0];
                int lightCount = lighting.resultLightCount();
                for (int i = 0; i < lightCount; ++i) {
                    this.lights[i].copyFrom(lighting.getResultLight(i));
                }
            }
        }

        public void Reset() {
            this.baseVisual = null;
            this.itemVisuals.clear();
            Arrays.fill(this.lights, null);
            this.grappleable = null;
            this.diedBoneTransforms = Pool.tryRelease(this.diedBoneTransforms);
        }
    }

    private static final class Checksummer {
        private MessageDigest md;
        private final StringBuilder sb = new StringBuilder();

        private Checksummer() {
        }

        public void reset() throws NoSuchAlgorithmException {
            if (this.md == null) {
                this.md = MessageDigest.getInstance("MD5");
            }
            this.md.reset();
        }

        public void update(byte b) {
            this.md.update(b);
        }

        public void update(boolean b) {
            this.md.update((byte)(b ? 1 : 0));
        }

        public void update(int i) {
            this.md.update((byte)(i & 0xFF));
            this.md.update((byte)(i >> 8 & 0xFF));
            this.md.update((byte)(i >> 16 & 0xFF));
            this.md.update((byte)(i >> 24 & 0xFF));
        }

        public void update(String add) {
            if (add == null || add.isEmpty()) {
                return;
            }
            this.md.update(add.getBytes());
        }

        public void update(ImmutableColor color) {
            this.update((byte)(color.r * 255.0f));
            this.update((byte)(color.g * 255.0f));
            this.update((byte)(color.b * 255.0f));
        }

        public void update(IsoGridSquare.ResultLight ls, float x, float y, float z) {
            if (ls == null || ls.radius <= 0) {
                return;
            }
            this.update((int)((float)ls.x - x));
            this.update((int)((float)ls.y - y));
            this.update((int)((float)ls.z - z));
            this.update((byte)(ls.r * 255.0f));
            this.update((byte)(ls.g * 255.0f));
            this.update((byte)(ls.b * 255.0f));
            this.update((byte)ls.radius);
        }

        public String checksumToString() {
            byte[] mdbytes = this.md.digest();
            this.sb.setLength(0);
            for (int i = 0; i < mdbytes.length; ++i) {
                this.sb.append(mdbytes[i] & 0xFF);
            }
            return this.sb.toString();
        }
    }

    private static final class DebugDrawInWorld
    extends TextureDraw.GenericDrawer {
        RenderJob job;
        boolean rendered;

        private DebugDrawInWorld() {
        }

        public void init(RenderJob job) {
            this.job = job;
            this.rendered = false;
        }

        @Override
        public void render() {
            this.job.animatedModel.DoRenderToWorld(this.job.body.x, this.job.body.y, this.job.body.z, this.job.animPlayerAngle);
            this.rendered = true;
        }

        @Override
        public void postRender() {
            if (this.job.animatedModel == null) {
                return;
            }
            if (this.rendered) {
                assert (!JobPool.contains(this.job));
                JobPool.push(this.job);
            } else {
                assert (!JobPool.contains(this.job));
                JobPool.push(this.job);
            }
            this.job.animatedModel.postRender(this.rendered);
        }
    }

    private static final class CharacterTextureVisual
    implements IHumanVisual {
        final HumanVisual humanVisual = new HumanVisual(this);
        boolean female;

        CharacterTextureVisual(boolean female) {
            this.female = female;
            this.humanVisual.setHairModel("");
            this.humanVisual.setBeardModel("");
        }

        @Override
        public HumanVisual getHumanVisual() {
            return this.humanVisual;
        }

        @Override
        public void getItemVisuals(ItemVisuals itemVisuals) {
            itemVisuals.clear();
        }

        @Override
        public boolean isFemale() {
            return this.female;
        }

        @Override
        public boolean isZombie() {
            return true;
        }

        @Override
        public boolean isSkeleton() {
            return false;
        }
    }

    public static final class BodyTexture {
        AtlasEntry entry;

        public ShadowParams getShadowParams() {
            return this.entry.shadowParams;
        }

        public Texture getTexture() {
            return this.entry == null ? null : this.entry.tex;
        }

        public float getRenderX(float sx) {
            return sx - (float)this.entry.w / 2.0f - this.entry.offsetX;
        }

        public float getRenderY(float sy) {
            return sy - (float)this.entry.h / 2.0f - this.entry.offsetY;
        }

        public float getRenderWidth() {
            return this.entry.w;
        }

        public float getRenderHeight() {
            return this.entry.h;
        }

        public void render(float originX, float originY, float originZ, float x, float y, float r, float g, float b, float a) {
            if (PerformanceSettings.fboRenderChunk) {
                BodyTextureDepthDrawer rbt = s_BodyTextureDepthDrawerPool.alloc();
                rbt.init(this, originX, originY, originZ, x, y, r, g, b, a);
                SpriteRenderer.instance.drawGeneric(rbt);
                return;
            }
            if (!this.entry.ready || !this.entry.tex.isReady()) {
                BodyTextureDrawer btd = DeadBodyAtlas.instance.bodyTextureDrawerPool.alloc().init(this, x, y, r, g, b, a);
                SpriteRenderer.instance.drawGeneric(btd);
                return;
            }
            this.entry.tex.render(x - (float)this.entry.w / 2.0f - this.entry.offsetX, y - (float)this.entry.h / 2.0f - this.entry.offsetY, this.entry.w, this.entry.h, r, g, b, a, null);
        }

        public void renderObjectPicker(float sx, float sy, ColorInfo lightInfo, IsoGridSquare square, IsoObject object) {
            if (!this.entry.ready) {
                return;
            }
            IsoObjectPicker.Instance.Add((int)(sx - (float)(this.entry.w / 2)), (int)(sy - (float)(this.entry.h / 2)), this.entry.w, this.entry.h, square, object, false, 1.0f, 1.0f);
        }
    }

    private static final class AtlasEntry {
        public Atlas atlas;
        public String key;
        public String lightKey;
        public int updateCounter;
        public int x;
        public int y;
        public int w;
        public int h;
        public float offsetX;
        public float offsetY;
        public Texture tex;
        public final ShadowParams shadowParams = new ShadowParams(1.0f, 1.0f, 1.0f);
        public boolean ready;

        private AtlasEntry() {
        }

        public void Reset() {
            this.atlas = null;
            this.tex.destroy();
            this.tex = null;
            this.ready = false;
        }
    }

    private static final class RenderJob
    extends TextureDraw.GenericDrawer {
        private static final float SIZE_V = 42.75f;
        public final BodyParams body = new BodyParams();
        public AtlasEntry entry;
        public AnimatedModel animatedModel;
        public float animPlayerAngle;
        public int done;
        public int renderRefCount;
        public boolean clearThisSlotOnly;
        int entryW;
        int entryH;
        final int[] viewport = new int[4];
        final org.joml.Matrix4f matri4f = new org.joml.Matrix4f();
        final org.joml.Matrix4f projection = new org.joml.Matrix4f();
        final org.joml.Matrix4f modelView = new org.joml.Matrix4f();
        final Vector3f scenePos = new Vector3f();
        final float[] bounds = new float[4];
        final float[] offset = new float[3];

        private RenderJob() {
        }

        public static RenderJob getNew() {
            if (JobPool.isEmpty()) {
                return new RenderJob();
            }
            return JobPool.pop();
        }

        public RenderJob init(BodyParams body, AtlasEntry entry) {
            this.body.init(body);
            this.entry = entry;
            if (this.animatedModel == null) {
                this.animatedModel = new AnimatedModel();
                this.animatedModel.setAnimate(false);
            }
            this.animatedModel.setAnimSetName(body.animSetName);
            this.animatedModel.setState(body.stateName);
            this.animatedModel.setPrimaryHandModelName(body.primaryHandItem);
            this.animatedModel.setSecondaryHandModelName(body.secondaryHandItem);
            this.animatedModel.setAttachedModelNames(body.attachedModelNames);
            this.animatedModel.setAmbient(body.ambient, body.outside, body.room);
            this.animatedModel.setLights(body.lights, body.x, body.y, body.z);
            this.animatedModel.setVariable("FallOnFront", body.fallOnFront);
            this.animatedModel.setVariable("KilledByFall", body.killedByFall);
            body.variables.forEach((k, v) -> this.animatedModel.setVariable((String)k, (String)v));
            this.animatedModel.setModelData(body.baseVisual, body.itemVisuals);
            this.animatedModel.setAngle(tempVector2.setLengthAndDirection(this.body.angle, 1.0f));
            this.setDeathPose();
            this.animatedModel.setTrackTime(body.trackTime);
            this.animatedModel.setGrappleable(body.grappleable);
            this.animatedModel.update();
            this.clearThisSlotOnly = false;
            this.done = 0;
            this.renderRefCount = 0;
            return this;
        }

        private void setDeathPose() {
            if (PZArrayUtil.isNullOrEmpty(this.body.diedBoneTransforms)) {
                return;
            }
            AnimationPlayer animationPlayer = this.animatedModel.getAnimationPlayer();
            animationPlayer.stopAll();
            AnimationTrack animationTrack = animationPlayer.play("DeathPose", true, true, -1.0f);
            if (animationTrack != null) {
                animationTrack.setBlendWeight(1.0f);
                animationTrack.initRagdollTransforms(this.body.diedBoneTransforms);
            }
        }

        public boolean renderMain() {
            if (this.animatedModel.isReadyToRender()) {
                this.animatedModel.renderMain();
                this.animPlayerAngle = this.animatedModel.getAnimationPlayer().getRenderedAngle();
                boolean bRagdoll = this.body.ragdollFall && !PZArrayUtil.isNullOrEmpty(this.body.diedBoneTransforms);
                this.animatedModel.calculateShadowParams(this.entry.shadowParams, bRagdoll);
                return true;
            }
            return false;
        }

        @Override
        public void render() {
            if (this.done == 1) {
                return;
            }
            GL11.glDepthMask(true);
            GL11.glColorMask(true, true, true, true);
            GL11.glDisable(3089);
            GL11.glPushAttrib(2048);
            ModelManager.instance.bitmap.startDrawing(true, true);
            GL11.glViewport(0, 0, ModelManager.instance.bitmap.getWidth(), ModelManager.instance.bitmap.getHeight());
            this.calcModelOffset(this.offset);
            this.animatedModel.setOffsetWhileRendering(this.offset[0], this.offset[1], this.offset[2]);
            this.animatedModel.DoRender(0, 0, ModelManager.instance.bitmap.getTexture().getWidth(), ModelManager.instance.bitmap.getTexture().getHeight(), 42.75f, this.animPlayerAngle);
            if (this.animatedModel.isRendered()) {
                this.calcEntrySize();
            }
            ModelManager.instance.bitmap.endDrawing();
            GL11.glPopAttrib();
            if (!this.animatedModel.isRendered()) {
                return;
            }
            instance.assignEntryToAtlas(this.entry, this.entryW, this.entryH);
            instance.toBodyAtlas(this);
        }

        @Override
        public void postRender() {
            if (this.animatedModel == null) {
                return;
            }
            this.animatedModel.postRender(this.done == 1);
            assert (this.renderRefCount > 0);
            --this.renderRefCount;
        }

        void calcMatrices(org.joml.Matrix4f projection, org.joml.Matrix4f modelView, float offsetX, float offsetY, float offsetZ) {
            int w = ModelManager.instance.bitmap.getWidth();
            int h = ModelManager.instance.bitmap.getHeight();
            float sizeV = 42.75f;
            float xScale = (float)w / (float)h;
            boolean flipY = true;
            projection.identity();
            projection.ortho(-42.75f * xScale, 42.75f * xScale, 42.75f, -42.75f, -100.0f, 100.0f);
            float f = org.joml.Math.sqrt(2048.0f);
            projection.scale(-f, f, f);
            modelView.identity();
            boolean bIsometric = true;
            modelView.rotate(0.5235988f, 1.0f, 0.0f, 0.0f);
            modelView.rotate(this.animPlayerAngle + 0.7853982f, 0.0f, 1.0f, 0.0f);
            float animalSize = this.animatedModel.getScale();
            modelView.scale(animalSize);
            modelView.translate(offsetX, offsetY, offsetZ);
        }

        void calcModelBounds(float[] bounds) {
            Matrix4f mtx;
            float minX = Float.MAX_VALUE;
            float minY = Float.MAX_VALUE;
            float maxX = -3.4028235E38f;
            float maxY = -3.4028235E38f;
            int dummy01 = this.animatedModel.getAnimationPlayer().getSkinningBoneIndex("Dummy01", -1);
            int bip01 = this.animatedModel.getAnimationPlayer().getSkinningBoneIndex("Bip01", -1);
            int bip01Pelvis = this.animatedModel.getAnimationPlayer().getSkinningBoneIndex("Bip01_Pelvis", -1);
            int translationData = this.animatedModel.getAnimationPlayer().getSkinningBoneIndex("Translation_Data", -1);
            Matrix4f[] worldSpace = null;
            boolean[] bIgnoreBone = null;
            boolean bRagdoll = this.body.ragdollFall && !PZArrayUtil.isNullOrEmpty(this.body.diedBoneTransforms) && bip01Pelvis != -1;
            float relativeToX = 0.0f;
            float relativeToY = 0.0f;
            if (bRagdoll) {
                worldSpace = PZArrayUtil.newInstance(Matrix4f.class, this.body.diedBoneTransforms.length, Matrix4f::new);
                bIgnoreBone = new boolean[worldSpace.length];
                this.body.diedBoneTransforms[bip01].getMatrix(worldSpace[bip01]);
                this.body.diedBoneTransforms[bip01Pelvis].getMatrix(worldSpace[bip01Pelvis]);
                BoneTransform.mul((BoneTransform)this.body.diedBoneTransforms[bip01Pelvis], worldSpace[bip01], worldSpace[bip01Pelvis]);
                mtx = worldSpace[bip01Pelvis];
                this.sceneToUI(mtx.m03, mtx.m13, mtx.m23, this.projection, this.modelView, this.scenePos);
                relativeToX = this.scenePos.x;
                relativeToY = this.scenePos.y;
                for (int i = 0; i < this.body.diedBoneTransforms.length; ++i) {
                    bIgnoreBone[i] = true;
                    if (i == dummy01 || i == bip01 || i == bip01Pelvis || i == translationData) continue;
                    SkinningBone bone = this.animatedModel.getAnimationPlayer().getSkinningData().getBoneAt(i);
                    SkinningBone parentBone = bone.parent;
                    if (parentBone.index == dummy01 || parentBone.index == bip01) continue;
                    bIgnoreBone[i] = false;
                    BoneTransform.mul((BoneTransform)this.body.diedBoneTransforms[bone.index], worldSpace[parentBone.index], worldSpace[bone.index]);
                }
                org.lwjgl.util.vector.Vector3f xAxis = new org.lwjgl.util.vector.Vector3f(1.0f, 0.0f, 0.0f);
                org.lwjgl.util.vector.Vector3f yAxis = new org.lwjgl.util.vector.Vector3f(0.0f, 1.0f, 0.0f);
                for (int i = 0; i < this.body.diedBoneTransforms.length; ++i) {
                    if (bIgnoreBone[i]) continue;
                    worldSpace[i].rotate(1.5707964f, xAxis);
                    worldSpace[i].rotate((float)Math.PI, yAxis);
                }
            }
            for (int i = 0; i < this.animatedModel.getAnimationPlayer().getModelTransformsCount(); ++i) {
                if (bRagdoll && (bIgnoreBone[i] || i == dummy01 || i == bip01 || i == bip01Pelvis) || i == translationData) continue;
                mtx = this.animatedModel.getAnimationPlayer().getModelTransformAt(i);
                if (bRagdoll) {
                    mtx = worldSpace[i];
                }
                this.sceneToUI(mtx.m03, mtx.m13, mtx.m23, this.projection, this.modelView, this.scenePos);
                if (bRagdoll) {
                    this.scenePos.x -= relativeToX;
                    this.scenePos.y -= relativeToY;
                }
                minX = PZMath.min(minX, this.scenePos.x);
                maxX = PZMath.max(maxX, this.scenePos.x);
                minY = PZMath.min(minY, this.scenePos.y);
                maxY = PZMath.max(maxY, this.scenePos.y);
            }
            if (bRagdoll) {
                minX += 512.0f;
                maxX += 512.0f;
                minY += 512.0f;
                maxY += 512.0f;
            }
            if (maxX - minX > 1024.0f) {
                float excess = maxX - minX - 1024.0f;
                minX += excess / 2.0f;
                maxX -= excess / 2.0f;
            }
            if (maxY - minY > 1024.0f) {
                float excess = maxY - minY - 1024.0f;
                minY += excess / 2.0f;
                maxY -= excess / 2.0f;
            }
            bounds[0] = minX;
            bounds[1] = minY;
            bounds[2] = maxX;
            bounds[3] = maxY;
        }

        void calcModelOffset(float[] offset) {
            int w = ModelManager.instance.bitmap.getWidth();
            int h = ModelManager.instance.bitmap.getHeight();
            float offsetX = 0.0f;
            float offsetY = this.body.standing ? -0.0f : 0.0f;
            this.calcMatrices(this.projection, this.modelView, offsetX, 0.0f, offsetY);
            this.calcModelBounds(this.bounds);
            float minX = this.bounds[0];
            float minY = this.bounds[1];
            float maxX = this.bounds[2];
            float maxY = this.bounds[3];
            this.uiToScene(this.projection, this.modelView, minX, minY, this.scenePos);
            float sceneMinX = this.scenePos.x;
            float sceneMinY = this.scenePos.z;
            float uiX = ((float)w - (maxX - minX)) / 2.0f;
            float uiY = ((float)h - (maxY - minY)) / 2.0f;
            this.uiToScene(this.projection, this.modelView, uiX, uiY, this.scenePos);
            offset[0] = offsetX += (this.scenePos.x - sceneMinX) * this.animatedModel.getScale();
            offset[1] = 0.0f;
            offset[2] = offsetY += (this.scenePos.z - sceneMinY) * this.animatedModel.getScale();
            this.entry.offsetX = (uiX - minX) / (8.0f / (float)Core.tileScale);
            this.entry.offsetY = (uiY - minY) / (8.0f / (float)Core.tileScale);
        }

        void calcEntrySize() {
            this.calcMatrices(this.projection, this.modelView, this.offset[0], this.offset[1], this.offset[2]);
            this.calcModelBounds(this.bounds);
            float minX = this.bounds[0];
            float minY = this.bounds[1];
            float maxX = this.bounds[2];
            float maxY = this.bounds[3];
            float pad = 128.0f;
            minX -= 128.0f;
            minY -= 128.0f;
            maxX += 128.0f;
            maxY += 128.0f;
            minX = org.joml.Math.floor(minX / 128.0f) * 128.0f;
            maxX = org.joml.Math.ceil(maxX / 128.0f) * 128.0f;
            minY = org.joml.Math.floor(minY / 128.0f) * 128.0f;
            maxY = org.joml.Math.ceil(maxY / 128.0f) * 128.0f;
            this.entryW = PZMath.min((int)(maxX - minX) / (8 / Core.tileScale), 1024);
            this.entryH = PZMath.min((int)(maxY - minY) / (8 / Core.tileScale), 1024);
        }

        UI3DScene.Ray getCameraRay(float uiX, float uiY, org.joml.Matrix4f projection, org.joml.Matrix4f modelView, UI3DScene.Ray cameraRay) {
            org.joml.Matrix4f matrix4f = L_getCameraRay.matrix4f;
            matrix4f.set(projection);
            matrix4f.mul(modelView);
            matrix4f.invert();
            this.viewport[0] = 0;
            this.viewport[1] = 0;
            this.viewport[2] = ModelManager.instance.bitmap.getWidth();
            this.viewport[3] = ModelManager.instance.bitmap.getHeight();
            Vector3f rayStart = matrix4f.unprojectInv(uiX, uiY, 0.0f, this.viewport, L_getCameraRay.ray_start);
            Vector3f rayEnd = matrix4f.unprojectInv(uiX, uiY, 1.0f, this.viewport, L_getCameraRay.ray_end);
            cameraRay.origin.set(rayStart);
            cameraRay.direction.set(rayEnd.sub(rayStart).normalize());
            return cameraRay;
        }

        Vector3f sceneToUI(float sceneX, float sceneY, float sceneZ, org.joml.Matrix4f projection, org.joml.Matrix4f modelView, Vector3f out) {
            org.joml.Matrix4f matrix4f = this.matri4f;
            matrix4f.set(projection);
            matrix4f.mul(modelView);
            this.viewport[0] = 0;
            this.viewport[1] = 0;
            this.viewport[2] = ModelManager.instance.bitmap.getWidth();
            this.viewport[3] = ModelManager.instance.bitmap.getHeight();
            matrix4f.project(sceneX, sceneY, sceneZ, this.viewport, out);
            return out;
        }

        Vector3f uiToScene(org.joml.Matrix4f projection, org.joml.Matrix4f modelView, float uiX, float uiY, Vector3f out) {
            UI3DScene.Plane plane = L_uiToScene.plane;
            plane.point.set(0.0f);
            plane.normal.set(0.0f, 1.0f, 0.0f);
            UI3DScene.Ray cameraRay = this.getCameraRay(uiX, uiY, projection, modelView, L_uiToScene.ray);
            if (UI3DScene.intersect_ray_plane(plane, cameraRay, out) != 1) {
                out.set(0.0f);
            }
            return out;
        }

        public void Reset() {
            this.body.Reset();
            this.entry = null;
            if (this.animatedModel != null) {
                this.animatedModel.releaseAnimationPlayer();
                this.animatedModel = null;
            }
        }

        static final class L_getCameraRay {
            static final org.joml.Matrix4f matrix4f = new org.joml.Matrix4f();
            static final Vector3f ray_start = new Vector3f();
            static final Vector3f ray_end = new Vector3f();

            L_getCameraRay() {
            }
        }

        static final class L_uiToScene {
            static final UI3DScene.Plane plane = new UI3DScene.Plane();
            static final UI3DScene.Ray ray = new UI3DScene.Ray();

            L_uiToScene() {
            }
        }
    }

    private final class Atlas {
        public final int entryWidth;
        public final int entryHeight;
        public Texture tex;
        public Texture depth;
        public final ArrayList<AtlasEntry> entryList;
        public boolean clear;

        public Atlas(DeadBodyAtlas deadBodyAtlas, int w, int h, int entryW, int entryH) {
            Objects.requireNonNull(deadBodyAtlas);
            this.entryList = new ArrayList();
            this.clear = true;
            this.entryWidth = entryW;
            this.entryHeight = entryH;
            this.tex = new Texture(w, h, 16);
            this.depth = new Texture(w, h, 512);
            if (deadBodyAtlas.fbo != null) {
                return;
            }
            deadBodyAtlas.fbo = new TextureFBO(this.tex, this.depth, false);
        }

        public boolean isFull() {
            int columns = this.tex.getWidth() / this.entryWidth;
            int rows = this.tex.getHeight() / this.entryHeight;
            return this.entryList.size() >= columns * rows;
        }

        public AtlasEntry addBody(String key) {
            int columns = this.tex.getWidth() / this.entryWidth;
            int index = this.entryList.size();
            int col = index % columns;
            int row = index / columns;
            AtlasEntry entry = new AtlasEntry();
            entry.atlas = this;
            entry.key = key;
            entry.x = col * this.entryWidth;
            entry.y = row * this.entryHeight;
            entry.w = this.entryWidth;
            entry.h = this.entryHeight;
            entry.tex = this.tex.split(key, entry.x, this.tex.getHeight() - (entry.y + this.entryHeight), entry.w, entry.h);
            entry.tex.setName(key);
            this.entryList.add(entry);
            return entry;
        }

        public void addEntry(AtlasEntry entry) {
            int columns = this.tex.getWidth() / this.entryWidth;
            int index = this.entryList.size();
            int col = index % columns;
            int row = index / columns;
            entry.atlas = this;
            entry.x = col * this.entryWidth;
            entry.y = row * this.entryHeight;
            entry.w = this.entryWidth;
            entry.h = this.entryHeight;
            entry.tex = this.tex.split(entry.key, entry.x, this.tex.getHeight() - (entry.y + this.entryHeight), entry.w, entry.h);
            entry.tex.setName(entry.key);
            this.entryList.add(entry);
        }

        public void Reset() {
            this.entryList.forEach(AtlasEntry::Reset);
            this.entryList.clear();
            if (!this.tex.isDestroyed()) {
                RenderThread.invokeOnRenderContext(() -> GL11.glDeleteTextures(this.tex.getID()));
            }
            this.tex = null;
            if (!this.depth.isDestroyed()) {
                RenderThread.invokeOnRenderContext(() -> GL11.glDeleteTextures(this.depth.getID()));
            }
            this.depth = null;
        }
    }

    private static final class ClearAtlasTexture
    extends TextureDraw.GenericDrawer {
        Atlas atlas;

        ClearAtlasTexture(Atlas atlas) {
            this.atlas = atlas;
        }

        @Override
        public void render() {
            TextureFBO fbo = DeadBodyAtlas.instance.fbo;
            if (fbo == null || this.atlas.tex == null) {
                return;
            }
            if (!this.atlas.clear) {
                return;
            }
            if (fbo.getTexture() != this.atlas.tex) {
                fbo.setTextureAndDepth(this.atlas.tex, this.atlas.depth);
            }
            fbo.startDrawing(false, false);
            GL11.glPushAttrib(2048);
            GL11.glViewport(0, 0, fbo.getWidth(), fbo.getHeight());
            org.joml.Matrix4f projection = Core.getInstance().projectionMatrixStack.alloc();
            int texWid = this.atlas.tex.getWidth();
            int texHgt = this.atlas.tex.getHeight();
            projection.setOrtho2D(0.0f, texWid, texHgt, 0.0f);
            Core.getInstance().projectionMatrixStack.push(projection);
            org.joml.Matrix4f modelView = Core.getInstance().modelViewMatrixStack.alloc();
            modelView.identity();
            Core.getInstance().modelViewMatrixStack.push(modelView);
            GL11.glDisable(3089);
            GL11.glClearColor(0.0f, 0.0f, 0.0f, 0.0f);
            GL11.glClear(16640);
            GL11.glClearColor(0.0f, 0.0f, 0.0f, 1.0f);
            fbo.endDrawing();
            GL11.glEnable(3089);
            Core.getInstance().projectionMatrixStack.pop();
            Core.getInstance().modelViewMatrixStack.pop();
            GL11.glPopAttrib();
            this.atlas.clear = false;
        }
    }

    public static final class DeadBodyAtlasShader
    extends Shader {
        public DeadBodyAtlasShader(String name) {
            super(name);
        }

        @Override
        protected void onCompileSuccess(ShaderProgram sender) {
            sender.Start();
            sender.setSamplerUnit("DIFFUSE", 0);
            sender.setSamplerUnit("DEPTH", 1);
            sender.End();
        }
    }

    private static final class BodyTextureDepthDrawer
    extends TextureDraw.GenericDrawer {
        BodyTexture bodyTexture;
        float ox;
        float oy;
        float oz;
        float x;
        float y;
        float r;
        float g;
        float b;
        float a;

        private BodyTextureDepthDrawer() {
        }

        BodyTextureDepthDrawer init(BodyTexture bodyTexture, float originX, float originY, float originZ, float x, float y, float r, float g, float b, float a) {
            this.bodyTexture = bodyTexture;
            this.ox = originX;
            this.oy = originY;
            this.oz = originZ;
            this.x = x;
            this.y = y;
            float f = 1.25f;
            this.r = PZMath.clamp(r * 1.25f, 0.0f, 1.0f);
            this.g = PZMath.clamp(g * 1.25f, 0.0f, 1.0f);
            this.b = PZMath.clamp(b * 1.25f, 0.0f, 1.0f);
            this.a = a;
            return this;
        }

        @Override
        public void render() {
            if (!this.bodyTexture.entry.ready || !this.bodyTexture.entry.tex.isReady()) {
                return;
            }
            if (deadBodyAtlasShader == null) {
                deadBodyAtlasShader = new DeadBodyAtlasShader("DeadBodyAtlas");
            }
            if (!deadBodyAtlasShader.getShaderProgram().isCompiled()) {
                return;
            }
            AtlasEntry entry = this.bodyTexture.entry;
            float x1 = this.x - (float)entry.w / 2.0f - entry.offsetX;
            float y1 = this.y - (float)entry.h / 2.0f - entry.offsetY;
            int w = entry.w;
            int h = entry.h;
            GL13.glActiveTexture(33985);
            GL11.glEnable(3553);
            GL11.glBindTexture(3553, entry.atlas.depth.getID());
            GL13.glActiveTexture(33984);
            GL11.glDepthMask(true);
            GL11.glDepthFunc(515);
            float playerX = Core.getInstance().floatParamMap.get(0).floatValue();
            float playerY = Core.getInstance().floatParamMap.get(1).floatValue();
            int playerIndex = SpriteRenderer.instance.getRenderingPlayerIndex();
            PlayerCamera camera = SpriteRenderer.instance.getRenderingPlayerCamera(playerIndex);
            float jx = camera.fixJigglyModelsSquareX;
            float jy = camera.fixJigglyModelsSquareY;
            float offsetX = entry.offsetX;
            float offsetY = entry.offsetY;
            float wx = -(offsetX + 2.0f * offsetY) / (64.0f * (float)Core.tileScale);
            float wy = -(offsetX - 2.0f * offsetY) / (-64.0f * (float)Core.tileScale);
            float f = 4.4f;
            float depthNear = IsoDepthHelper.getSquareDepthData((int)PZMath.fastfloor((float)playerX), (int)PZMath.fastfloor((float)playerY), (float)(this.ox + jx + wx + 2.2f), (float)(this.oy + jy + wy + 2.2f), (float)(this.oz + 1.0f)).depthStart;
            float depthFar = IsoDepthHelper.getSquareDepthData((int)PZMath.fastfloor((float)playerX), (int)PZMath.fastfloor((float)playerY), (float)(this.ox + jx + wx - 2.2f), (float)(this.oy + jy + wy - 2.2f), (float)this.oz).depthStart;
            depthNear += 0.0014f;
            depthFar += 0.0014f;
            VBORenderer vbor = VBORenderer.getInstance();
            vbor.startRun(vbor.formatPositionColorUv);
            vbor.setMode(7);
            vbor.setDepthTest(true);
            vbor.setShaderProgram(deadBodyAtlasShader.getShaderProgram());
            Texture tex = entry.tex;
            vbor.setTextureID(tex.getTextureId());
            vbor.cmdUseProgram(deadBodyAtlasShader.getShaderProgram());
            vbor.cmdShader1f("zDepthBlendZ", depthNear);
            vbor.cmdShader1f("zDepthBlendToZ", depthFar);
            vbor.addQuad(x1, y1, tex.getXStart(), tex.getYStart(), x1 + (float)w, y1 + (float)h, tex.getXEnd(), tex.getYEnd(), 0.0f, this.r, this.g, this.b, this.a);
            vbor.endRun();
            vbor.flush();
            GL13.glActiveTexture(33985);
            GL11.glBindTexture(3553, 0);
            GL11.glDisable(3553);
            GL13.glActiveTexture(33984);
            ShaderHelper.glUseProgramObjectARB(0);
            Texture.lastTextureID = -1;
            SpriteRenderer.ringBuffer.restoreBoundTextures = true;
            GLStateRenderThread.restore();
        }

        @Override
        public void postRender() {
            s_BodyTextureDepthDrawerPool.release(this);
        }
    }

    private static final class BodyTextureDrawer
    extends TextureDraw.GenericDrawer {
        BodyTexture bodyTexture;
        float x;
        float y;
        float r;
        float g;
        float b;
        float a;

        private BodyTextureDrawer() {
        }

        BodyTextureDrawer init(BodyTexture bodyTexture, float x, float y, float r, float g, float b, float a) {
            this.bodyTexture = bodyTexture;
            this.x = x;
            this.y = y;
            this.r = r;
            this.g = g;
            this.b = b;
            this.a = a;
            return this;
        }

        @Override
        public void render() {
            AtlasEntry entry = this.bodyTexture.entry;
            if (!entry.ready || !entry.tex.isReady()) {
                return;
            }
            int x = (int)(this.x - (float)entry.w / 2.0f - entry.offsetX);
            int y = (int)(this.y - (float)entry.h / 2.0f - entry.offsetY);
            int w = entry.w;
            int h = entry.h;
            VBORenderer vbor = VBORenderer.getInstance();
            vbor.startRun(vbor.formatPositionColorUv);
            vbor.setTextureID(entry.tex.getTextureId());
            vbor.addQuad(x, y, entry.tex.xStart, entry.tex.yStart, x + w, y + h, entry.tex.xEnd, entry.tex.yEnd, 0.0f, this.r, this.g, this.b, this.a);
            vbor.endRun();
            vbor.flush();
            SpriteRenderer.ringBuffer.restoreBoundTextures = true;
        }

        @Override
        public void postRender() {
            this.bodyTexture = null;
            DeadBodyAtlas.instance.bodyTextureDrawerPool.release(this);
        }
    }
}

