/*
 * Decompiled with CFR 0.152.
 */
package zombie.scripting.entity;

import java.util.ArrayList;
import zombie.UsedFromLua;
import zombie.core.Core;
import zombie.debug.DebugLog;
import zombie.debug.objects.DebugClassFields;
import zombie.debug.objects.DebugMethod;
import zombie.entity.ComponentType;
import zombie.entity.GameEntity;
import zombie.entity.components.attributes.Attribute;
import zombie.entity.components.attributes.AttributeType;
import zombie.network.GameClient;
import zombie.scripting.ScriptLoadMode;
import zombie.scripting.ScriptManager;
import zombie.scripting.ScriptParser;
import zombie.scripting.ScriptType;
import zombie.scripting.entity.ComponentScript;
import zombie.scripting.entity.GameEntityTemplate;
import zombie.scripting.entity.components.ui.UiConfigScript;
import zombie.scripting.objects.BaseScriptObject;
import zombie.util.StringUtils;
import zombie.world.WorldDictionary;

@DebugClassFields
@UsedFromLua
public class GameEntityScript
extends BaseScriptObject {
    private String name;
    private final ArrayList<ComponentScript> componentScripts = new ArrayList();
    private short registryId = (short)-1;
    private boolean existsAsVanilla;
    private String modId;
    private String fileAbsPath;

    public GameEntityScript() {
        super(ScriptType.Entity);
    }

    protected GameEntityScript(ScriptType override) {
        super(override);
    }

    @DebugMethod
    public String getName() {
        if (this.getParent() != null) {
            return this.getParent().getScriptObjectName();
        }
        return this.name;
    }

    public String getDisplayNameDebug() {
        UiConfigScript script = (UiConfigScript)this.getComponentScriptFor(ComponentType.UiConfig);
        if (script != null) {
            return script.getDisplayNameDebug();
        }
        return GameEntity.getDefaultEntityDisplayName();
    }

    public String getModuleName() {
        return this.getModule().getName();
    }

    @DebugMethod
    public String getFullName() {
        return this.getScriptObjectFullType();
    }

    @Override
    public void PreReload() {
        this.componentScripts.clear();
    }

    @Override
    public void OnScriptsLoaded(ScriptLoadMode loadMode) throws Exception {
        for (int i = 0; i < this.componentScripts.size(); ++i) {
            this.componentScripts.get(i).OnScriptsLoaded(loadMode);
        }
    }

    @Override
    public void OnLoadedAfterLua() throws Exception {
        for (int i = 0; i < this.componentScripts.size(); ++i) {
            this.componentScripts.get(i).OnLoadedAfterLua();
        }
    }

    @Override
    public void OnPostWorldDictionaryInit() throws Exception {
        for (int i = 0; i < this.componentScripts.size(); ++i) {
            this.componentScripts.get(i).OnPostWorldDictionaryInit();
        }
    }

    public ArrayList<ComponentScript> getComponentScripts() {
        return this.componentScripts;
    }

    public boolean hasComponents() {
        return !this.componentScripts.isEmpty();
    }

    public boolean containsComponent(ComponentType componentType) {
        for (int i = 0; i < this.componentScripts.size(); ++i) {
            if (this.componentScripts.get((int)i).type != componentType) continue;
            return true;
        }
        return false;
    }

    private ComponentScript getOrCreateComponentScript(ComponentType componentType) {
        ComponentScript componentScript = this.getComponentScript(componentType);
        if (componentScript == null && (componentScript = componentType.CreateComponentScript()) != null) {
            componentScript.setParent(this);
            this.componentScripts.add(componentScript);
        }
        if (componentScript != null) {
            componentScript.InitLoadPP(this.getScriptObjectName());
            componentScript.setModule(this.getModule());
        }
        return componentScript;
    }

    public <T extends ComponentScript> T getComponentScriptFor(ComponentType componentType) {
        if (this.containsComponent(componentType)) {
            return (T)this.getComponentScript(componentType);
        }
        return null;
    }

    private ComponentScript getComponentScript(ComponentType componentType) {
        for (int i = 0; i < this.componentScripts.size(); ++i) {
            if (this.componentScripts.get((int)i).type != componentType) continue;
            return this.componentScripts.get(i);
        }
        return null;
    }

    public void copyFrom(GameEntityScript other) {
        for (ComponentScript script : other.componentScripts) {
            ComponentScript componentScript = this.getOrCreateComponentScript(script.type);
            componentScript.copyFrom(script);
        }
    }

    @Override
    public void InitLoadPP(String name) {
        super.InitLoadPP(name);
        ScriptManager scriptMgr = ScriptManager.instance;
        this.name = name;
        this.modId = ScriptManager.getCurrentLoadFileMod();
        if (this.modId.equals("pz-vanilla")) {
            this.existsAsVanilla = true;
        }
        this.fileAbsPath = ScriptManager.getCurrentLoadFileAbsPath();
        WorldDictionary.onLoadEntity(this);
    }

    @Override
    public void Load(String name, String body) throws Exception {
        ScriptParser.Block block = ScriptParser.parse(body);
        block = block.children.get(0);
        this.LoadCommonBlock(block);
        for (ScriptParser.Value value : block.values) {
            String key = value.getKey().trim();
            String val = value.getValue().trim();
            if (key.isEmpty() || val.isEmpty() || !key.equalsIgnoreCase("entitytemplate")) continue;
            GameEntityTemplate template = ScriptManager.instance.getGameEntityTemplate(val);
            GameEntityScript script = template.getScript();
            this.copyFrom(script);
        }
        for (ScriptParser.Block child : block.children) {
            if (child.type.equalsIgnoreCase("component")) {
                this.LoadComponentBlock(child);
                continue;
            }
            DebugLog.General.error("Unknown block '" + child.type + "' in entity  script: " + this.getName());
        }
        this.Load(block);
    }

    protected void Load(ScriptParser.Block block) {
    }

    public boolean LoadAttribute(String k, String v) {
        AttributeType attributeType = Attribute.TypeFromName(k.trim());
        if (attributeType != null) {
            ComponentScript componentScript = this.getOrCreateComponentScript(ComponentType.Attributes);
            return componentScript.parseKeyValue(k, v);
        }
        return false;
    }

    public void LoadComponentBlock(ScriptParser.Block block) throws Exception {
        if (block.type.equalsIgnoreCase("component") && !StringUtils.isNullOrWhitespace(block.id)) {
            ComponentType componentType;
            try {
                componentType = ComponentType.valueOf(block.id);
            }
            catch (Exception e) {
                e.printStackTrace();
                componentType = ComponentType.Undefined;
            }
            if (componentType != ComponentType.Undefined) {
                ComponentScript componentScript = this.getOrCreateComponentScript(componentType);
                componentScript.load(block);
            } else {
                DebugLog.General.warn("Could not parse component block id = " + (block.id != null ? block.id : "null"));
            }
        }
    }

    public short getRegistry_id() {
        return this.registryId;
    }

    public void setRegistry_id(short id) {
        if (this.registryId != -1) {
            WorldDictionary.DebugPrintEntity(id);
            throw new RuntimeException("Cannot override existing registry id (" + this.registryId + ") to new id (" + id + "), item: " + (this.getFullName() != null ? this.getFullName() : "unknown"));
        }
        this.registryId = id;
    }

    public String getModID() {
        return this.modId;
    }

    public boolean getExistsAsVanilla() {
        return this.existsAsVanilla;
    }

    public String getFileAbsPath() {
        return this.fileAbsPath;
    }

    public void setModID(String modid) {
        if (GameClient.client) {
            if (this.modId == null) {
                this.modId = modid;
            } else if (!modid.equals(this.modId) && Core.debug) {
                WorldDictionary.DebugPrintEntity(this);
                throw new RuntimeException("Cannot override modID. ModID=" + (modid != null ? modid : "null"));
            }
        }
    }
}

