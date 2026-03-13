/*
 * Decompiled with CFR 0.152.
 */
package zombie.scripting.objects;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import org.joml.Vector3f;
import zombie.UsedFromLua;
import zombie.ZomboidFileSystem;
import zombie.core.math.PZMath;
import zombie.core.skinnedmodel.advancedanimation.AnimBoneWeight;
import zombie.core.skinnedmodel.model.Model;
import zombie.debug.DebugLog;
import zombie.network.GameServer;
import zombie.scripting.ScriptManager;
import zombie.scripting.ScriptParser;
import zombie.scripting.ScriptType;
import zombie.scripting.objects.BaseScriptObject;
import zombie.scripting.objects.CullFace;
import zombie.scripting.objects.IModelAttachmentOwner;
import zombie.scripting.objects.Item;
import zombie.scripting.objects.ItemType;
import zombie.scripting.objects.ModelAttachment;
import zombie.util.StringUtils;

@UsedFromLua
public final class ModelScript
extends BaseScriptObject
implements IModelAttachmentOwner {
    public static final String DEFAULT_SHADER_NAME = "basicEffect";
    public String fileName;
    public String name;
    public String meshName;
    public String textureName;
    public String shaderName;
    public boolean isStatic = true;
    public float scale = 1.0f;
    public final ArrayList<ModelAttachment> attachments = new ArrayList();
    public HashMap<String, ModelAttachment> attachmentById = new HashMap();
    public boolean invertX;
    public String postProcess;
    public Model loadedModel;
    public final ArrayList<AnimBoneWeight> boneWeights = new ArrayList();
    public String animationsMesh;
    public int cullFace = -1;
    private static final HashSet<String> reported = new HashSet();

    public ModelScript() {
        super(ScriptType.Model);
    }

    @Override
    public void InitLoadPP(String name) {
        super.InitLoadPP(name);
        ScriptManager scriptMgr = ScriptManager.instance;
        this.fileName = scriptMgr.currentFileName;
        this.name = name;
    }

    @Override
    public void Load(String name, String totalFile) throws Exception {
        ScriptParser.Block block = ScriptParser.parse(totalFile);
        block = block.children.get(0);
        this.LoadCommonBlock(block);
        for (ScriptParser.Block child : block.children) {
            if (!"attachment".equals(child.type)) continue;
            this.LoadAttachment(child);
        }
        boolean bUndoCoreScale = false;
        for (ScriptParser.Value value : block.values) {
            String k = value.getKey().trim();
            String v = value.getValue().trim();
            if ("mesh".equalsIgnoreCase(k)) {
                this.meshName = v;
                continue;
            }
            if ("scale".equalsIgnoreCase(k)) {
                this.scale = Float.parseFloat(v);
                continue;
            }
            if ("shader".equalsIgnoreCase(k)) {
                this.shaderName = v;
                continue;
            }
            if ("static".equalsIgnoreCase(k)) {
                this.isStatic = Boolean.parseBoolean(v);
                continue;
            }
            if ("texture".equalsIgnoreCase(k)) {
                this.textureName = v;
                continue;
            }
            if ("invertX".equalsIgnoreCase(k)) {
                this.invertX = Boolean.parseBoolean(v);
                continue;
            }
            if ("cullFace".equalsIgnoreCase(k)) {
                if (CullFace.BACK.toString().equalsIgnoreCase(v)) {
                    this.cullFace = 1029;
                    continue;
                }
                if (CullFace.FRONT.toString().equalsIgnoreCase(v)) {
                    this.cullFace = 1028;
                    continue;
                }
                if (!CullFace.NONE.toString().equalsIgnoreCase(v)) continue;
                this.cullFace = 0;
                continue;
            }
            if ("postProcess".equalsIgnoreCase(k)) {
                this.postProcess = v;
                continue;
            }
            if ("undoCoreScale".equalsIgnoreCase(k)) {
                bUndoCoreScale = Boolean.parseBoolean(v);
                continue;
            }
            if ("boneWeight".equalsIgnoreCase(k)) {
                String[] ss1 = v.split("\\s+");
                if (ss1.length != 2) continue;
                AnimBoneWeight boneWeight = new AnimBoneWeight(ss1[0], PZMath.tryParseFloat(ss1[1], 1.0f));
                boneWeight.includeDescendants = false;
                this.boneWeights.add(boneWeight);
                continue;
            }
            if (!"animationsMesh".equalsIgnoreCase(k)) continue;
            this.animationsMesh = StringUtils.discardNullOrWhitespace(v);
        }
        if (bUndoCoreScale) {
            this.scale *= 0.6666667f;
        }
    }

    private ModelAttachment LoadAttachment(ScriptParser.Block block) {
        ModelAttachment attachment = this.getAttachmentById(block.id);
        if (attachment == null) {
            attachment = new ModelAttachment(block.id.intern());
            attachment.setOwner(this);
            this.attachments.add(attachment);
            this.attachmentById.put(attachment.getId(), attachment);
        }
        for (ScriptParser.Value value : block.values) {
            String k = value.getKey().trim().intern();
            String v = value.getValue().trim().intern();
            if ("bone".equals(k)) {
                attachment.setBone(v);
                continue;
            }
            if ("offset".equals(k)) {
                this.LoadVector3f(v, attachment.getOffset());
                continue;
            }
            if ("rotate".equals(k)) {
                this.LoadVector3f(v, attachment.getRotate());
                continue;
            }
            if (!"scale".equals(k)) continue;
            attachment.setScale(PZMath.tryParseFloat(v, 1.0f));
        }
        return attachment;
    }

    private void LoadVector3f(String s, Vector3f v) {
        String[] ss = s.split(" ");
        v.set(Float.parseFloat(ss[0]), Float.parseFloat(ss[1]), Float.parseFloat(ss[2]));
    }

    public String getName() {
        return this.name;
    }

    public String getFullType() {
        return this.getModule().name + "." + this.name;
    }

    public String getMeshName() {
        return this.meshName;
    }

    public String getTextureName() {
        if (StringUtils.isNullOrWhitespace(this.textureName)) {
            return this.meshName;
        }
        return this.textureName;
    }

    public String getTextureName(boolean allowNull) {
        if (StringUtils.isNullOrWhitespace(this.textureName) && !allowNull) {
            return this.meshName;
        }
        return this.textureName;
    }

    public String getShaderName() {
        if (StringUtils.isNullOrWhitespace(this.shaderName)) {
            return DEFAULT_SHADER_NAME;
        }
        return this.shaderName;
    }

    public String getFileName() {
        return this.fileName;
    }

    public int getAttachmentCount() {
        return this.attachments.size();
    }

    public ModelAttachment getAttachment(int index) {
        return this.attachments.get(index);
    }

    public ModelAttachment getAttachmentById(String id) {
        return this.attachmentById.get(id);
    }

    public ModelAttachment addAttachment(ModelAttachment attach) {
        attach.setOwner(this);
        this.attachments.add(attach);
        this.attachmentById.put(attach.getId(), attach);
        return attach;
    }

    public ModelAttachment removeAttachment(ModelAttachment attach) {
        attach.setOwner(null);
        this.attachments.remove(attach);
        this.attachmentById.remove(attach.getId());
        return attach;
    }

    public ModelAttachment addAttachmentAt(int index, ModelAttachment attach) {
        attach.setOwner(this);
        this.attachments.add(index, attach);
        this.attachmentById.put(attach.getId(), attach);
        return attach;
    }

    public ModelAttachment removeAttachment(int index) {
        ModelAttachment attachment = this.attachments.remove(index);
        this.attachmentById.remove(attachment.getId());
        attachment.setOwner(null);
        return attachment;
    }

    public void scaleAttachmentOffset(float scale) {
        for (int i = 0; i < this.getAttachmentCount(); ++i) {
            ModelAttachment attachment = this.getAttachment(i);
            attachment.getOffset().mul(scale);
        }
    }

    @Override
    public void beforeRenameAttachment(ModelAttachment attachment) {
        this.attachmentById.remove(attachment.getId());
    }

    @Override
    public void afterRenameAttachment(ModelAttachment attachment) {
        this.attachmentById.put(attachment.getId(), attachment);
    }

    public boolean isStatic() {
        return this.isStatic;
    }

    @Override
    public void reset() {
        this.invertX = false;
        this.name = null;
        this.meshName = null;
        this.textureName = null;
        this.shaderName = null;
        this.isStatic = true;
        this.scale = 1.0f;
        this.boneWeights.clear();
        this.cullFace = -1;
    }

    private static void checkMesh(String object, String meshName) {
        if (StringUtils.isNullOrWhitespace(meshName)) {
            return;
        }
        String lower = meshName.toLowerCase(Locale.ENGLISH);
        if (!(ZomboidFileSystem.instance.activeFileMap.containsKey("media/models_x/" + lower + ".fbx") || ZomboidFileSystem.instance.activeFileMap.containsKey("media/models_x/" + lower + ".x") || ZomboidFileSystem.instance.activeFileMap.containsKey("media/models/" + lower + ".txt"))) {
            reported.add(meshName);
            DebugLog.Script.warn("no such mesh \"" + meshName + "\" for " + object);
        }
    }

    private static void checkTexture(String object, String textureName) {
        if (GameServer.server) {
            return;
        }
        if (StringUtils.isNullOrWhitespace(textureName)) {
            return;
        }
        String lower = textureName.toLowerCase(Locale.ENGLISH);
        if (!ZomboidFileSystem.instance.activeFileMap.containsKey("media/textures/" + lower + ".png")) {
            reported.add(textureName);
            DebugLog.Script.warn("no such texture \"" + textureName + "\" for " + object);
        }
    }

    private static void check(String object, String model) {
        ModelScript.check(object, model, null);
    }

    private static void check(String object, String model, String clothingItem) {
        if (StringUtils.isNullOrWhitespace(model)) {
            return;
        }
        if (reported.contains(model)) {
            return;
        }
        ModelScript modelScript = ScriptManager.instance.getModelScript(model);
        if (modelScript == null) {
            reported.add(model);
            DebugLog.Script.warn("no such model \"" + model + "\" for " + object);
        } else {
            ModelScript.checkMesh(modelScript.getFullType(), modelScript.getMeshName());
            if (StringUtils.isNullOrWhitespace(clothingItem)) {
                ModelScript.checkTexture(modelScript.getFullType(), modelScript.getTextureName());
            }
        }
    }

    public static void ScriptsLoaded() {
        reported.clear();
        ArrayList<Item> items = ScriptManager.instance.getAllItems();
        for (Item item : items) {
            ModelScript modelScript;
            String staticModel;
            item.resolveModelScripts();
            ModelScript.check(item.getFullName(), item.getStaticModel());
            ModelScript.check(item.getFullName(), item.getWeaponSprite());
            ModelScript.check(item.getFullName(), item.worldStaticModel, item.getClothingItem());
            if (!item.isItemType(ItemType.FOOD) || StringUtils.isNullOrWhitespace(staticModel = item.getStaticModel()) || (modelScript = ScriptManager.instance.getModelScript(staticModel)) == null || modelScript.getAttachmentCount() == 0) continue;
            ModelScript modelScript2 = ScriptManager.instance.getModelScript(staticModel + "Burnt");
            if (modelScript2 != null) {
                ModelScript.checkTexture(modelScript2.getName(), modelScript2.textureName);
            }
            if (modelScript2 != null && modelScript2.getAttachmentCount() != modelScript.getAttachmentCount()) {
                DebugLog.Script.warn("different number of attachments on %s and %s", modelScript.name, modelScript2.name);
            }
            if ((modelScript2 = ScriptManager.instance.getModelScript(staticModel + "Cooked")) != null) {
                ModelScript.checkTexture(modelScript2.getName(), modelScript2.textureName);
            }
            if (modelScript2 != null && modelScript2.getAttachmentCount() != modelScript.getAttachmentCount()) {
                DebugLog.Script.warn("different number of attachments on %s and %s", modelScript.name, modelScript2.name);
            }
            if ((modelScript2 = ScriptManager.instance.getModelScript(staticModel + "Rotten")) != null) {
                ModelScript.checkTexture(modelScript2.getName(), modelScript2.textureName);
            }
            if (modelScript2 == null || modelScript2.getAttachmentCount() == modelScript.getAttachmentCount()) continue;
            DebugLog.Script.warn("different number of attachments on %s and %s", modelScript.name, modelScript2.name);
        }
    }
}

