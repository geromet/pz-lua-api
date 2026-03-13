/*
 * Decompiled with CFR 0.152.
 */
package zombie.scripting;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Stack;
import zombie.GameSounds;
import zombie.Lua.LuaManager;
import zombie.SoundManager;
import zombie.UsedFromLua;
import zombie.ZomboidFileSystem;
import zombie.characters.IsoGameCharacter;
import zombie.characters.professions.CharacterProfessionDefinition;
import zombie.characters.traits.CharacterTraitDefinition;
import zombie.core.Core;
import zombie.core.IndieFileLoader;
import zombie.core.Translator;
import zombie.core.logger.ExceptionLogger;
import zombie.core.random.Rand;
import zombie.core.skinnedmodel.runtime.RuntimeAnimationScript;
import zombie.debug.DebugLog;
import zombie.debug.DebugType;
import zombie.debug.LogSeverity;
import zombie.entity.ComponentType;
import zombie.entity.components.crafting.recipe.CraftRecipeManager;
import zombie.entity.components.fluids.Fluid;
import zombie.entity.components.spriteconfig.SpriteConfigManager;
import zombie.entity.energy.Energy;
import zombie.gameStates.ChooseGameInfo;
import zombie.inventory.ItemTags;
import zombie.inventory.RecipeManager;
import zombie.iso.IsoWorld;
import zombie.iso.SpriteModel;
import zombie.network.GameClient;
import zombie.network.GameServer;
import zombie.network.NetChecksum;
import zombie.scripting.FileName;
import zombie.scripting.IScriptObjectStore;
import zombie.scripting.ScriptBucket;
import zombie.scripting.ScriptBucketCollection;
import zombie.scripting.ScriptLoadMode;
import zombie.scripting.ScriptParser;
import zombie.scripting.ScriptType;
import zombie.scripting.entity.ComponentScript;
import zombie.scripting.entity.GameEntityScript;
import zombie.scripting.entity.GameEntityTemplate;
import zombie.scripting.entity.components.crafting.CraftRecipe;
import zombie.scripting.entity.components.crafting.CraftRecipeComponentScript;
import zombie.scripting.itemConfig.ItemConfig;
import zombie.scripting.objects.AnimationsMesh;
import zombie.scripting.objects.BaseScriptObject;
import zombie.scripting.objects.CharacterProfessionDefinitionScript;
import zombie.scripting.objects.CharacterTraitDefinitionScript;
import zombie.scripting.objects.ClockScript;
import zombie.scripting.objects.EnergyDefinitionScript;
import zombie.scripting.objects.EvolvedRecipe;
import zombie.scripting.objects.Fixing;
import zombie.scripting.objects.FluidDefinitionScript;
import zombie.scripting.objects.FluidFilterScript;
import zombie.scripting.objects.GameSoundScript;
import zombie.scripting.objects.Item;
import zombie.scripting.objects.ItemBodyLocation;
import zombie.scripting.objects.ItemFilterScript;
import zombie.scripting.objects.ItemTag;
import zombie.scripting.objects.ItemType;
import zombie.scripting.objects.MannequinScript;
import zombie.scripting.objects.ModelScript;
import zombie.scripting.objects.PhysicsHitReactionScript;
import zombie.scripting.objects.PhysicsShapeScript;
import zombie.scripting.objects.RagdollScript;
import zombie.scripting.objects.Recipe;
import zombie.scripting.objects.ScriptModule;
import zombie.scripting.objects.SoundTimelineScript;
import zombie.scripting.objects.StringListScript;
import zombie.scripting.objects.TimedActionScript;
import zombie.scripting.objects.UniqueRecipe;
import zombie.scripting.objects.VehicleScript;
import zombie.scripting.objects.VehicleTemplate;
import zombie.scripting.objects.XuiColorsScript;
import zombie.scripting.objects.XuiConfigScript;
import zombie.scripting.objects.XuiLayoutScript;
import zombie.scripting.objects.XuiSkinScript;
import zombie.scripting.ui.XuiManager;
import zombie.util.StringUtils;
import zombie.util.list.PZArrayUtil;
import zombie.vehicles.VehicleEngineRPM;
import zombie.world.WorldDictionary;

@UsedFromLua
public final class ScriptManager
implements IScriptObjectStore {
    public static final ScriptManager instance = new ScriptManager();
    private static final EnumSet<ScriptType> debugTypes = EnumSet.noneOf(ScriptType.class);
    public String currentFileName;
    private final ArrayList<String> loadFileNames = new ArrayList();
    public final HashMap<String, ScriptModule> moduleMap = new HashMap();
    public final ArrayList<ScriptModule> moduleList = new ArrayList();
    public ScriptModule currentLoadingModule;
    private final HashMap<String, String> moduleAliases = new HashMap();
    private final StringBuilder buf = new StringBuilder();
    private final HashMap<String, ScriptModule> cachedModules = new HashMap();
    private final HashMap<ItemTag, ArrayList<Item>> tagToItemMap = new HashMap();
    private final HashMap<String, ArrayList<Item>> typeToItemMap = new HashMap();
    private final HashMap<String, String> clothingToItemMap = new HashMap();
    private final ArrayList<String> visualDamagesList = new ArrayList();
    private final ArrayList<ScriptBucketCollection<?>> bucketCollectionList = new ArrayList();
    private final HashMap<ScriptType, ScriptBucketCollection<?>> bucketCollectionMap = new HashMap();
    private boolean hasLoadErrors;
    private final ScriptBucketCollection<VehicleTemplate> vehicleTemplates = this.addBucketCollection(new ScriptBucketCollection<VehicleTemplate>(this, this, ScriptType.VehicleTemplate){
        {
            Objects.requireNonNull(this$0);
            super(scriptManager, scriptType);
        }

        @Override
        public ScriptBucket<VehicleTemplate> getBucketFromModule(ScriptModule module) {
            return module.vehicleTemplates;
        }
    });
    private final ScriptBucketCollection<GameEntityTemplate> entityTemplates = this.addBucketCollection(new ScriptBucketCollection<GameEntityTemplate>(this, this, ScriptType.EntityTemplate){
        {
            Objects.requireNonNull(this$0);
            super(scriptManager, scriptType);
        }

        @Override
        public ScriptBucket<GameEntityTemplate> getBucketFromModule(ScriptModule module) {
            return module.entityTemplates;
        }
    });
    private final ScriptBucketCollection<Item> items = this.addBucketCollection(new ScriptBucketCollection<Item>(this, this, ScriptType.Item){
        final /* synthetic */ ScriptManager this$0;
        {
            ScriptManager scriptManager2 = this$0;
            Objects.requireNonNull(scriptManager2);
            this.this$0 = scriptManager2;
            super(scriptManager, scriptType);
        }

        @Override
        public ScriptBucket<Item> getBucketFromModule(ScriptModule module) {
            return module.items;
        }

        @Override
        public void LoadScripts(ScriptLoadMode loadMode) {
            super.LoadScripts(loadMode);
            ItemTags.Init(this.this$0.getAllItems());
        }
    });
    private final ScriptBucketCollection<Recipe> recipes = this.addBucketCollection(new ScriptBucketCollection<Recipe>(this, this, ScriptType.Recipe){
        {
            Objects.requireNonNull(this$0);
            super(scriptManager, scriptType);
        }

        @Override
        public ScriptBucket<Recipe> getBucketFromModule(ScriptModule module) {
            return module.recipes;
        }

        @Override
        public void OnLoadedAfterLua() throws Exception {
            super.OnLoadedAfterLua();
            RecipeManager.LoadedAfterLua();
        }
    });
    private final ScriptBucketCollection<UniqueRecipe> uniqueRecipes = this.addBucketCollection(new ScriptBucketCollection<UniqueRecipe>(this, this, ScriptType.UniqueRecipe){
        {
            Objects.requireNonNull(this$0);
            super(scriptManager, scriptType);
        }

        @Override
        public ScriptBucket<UniqueRecipe> getBucketFromModule(ScriptModule module) {
            return module.uniqueRecipes;
        }
    });
    private final Stack<UniqueRecipe> uniqueRecipeTempStack = new Stack();
    private final ScriptBucketCollection<EvolvedRecipe> evolvedRecipes = this.addBucketCollection(new ScriptBucketCollection<EvolvedRecipe>(this, this, ScriptType.EvolvedRecipe){
        {
            Objects.requireNonNull(this$0);
            super(scriptManager, scriptType);
        }

        @Override
        public ScriptBucket<EvolvedRecipe> getBucketFromModule(ScriptModule module) {
            return module.evolvedRecipes;
        }
    });
    private final Stack<EvolvedRecipe> evolvedRecipeTempStack = new Stack();
    private final ScriptBucketCollection<Fixing> fixings = this.addBucketCollection(new ScriptBucketCollection<Fixing>(this, this, ScriptType.Fixing){
        {
            Objects.requireNonNull(this$0);
            super(scriptManager, scriptType);
        }

        @Override
        public ScriptBucket<Fixing> getBucketFromModule(ScriptModule module) {
            return module.fixings;
        }
    });
    private final ScriptBucketCollection<AnimationsMesh> animationMeshes = this.addBucketCollection(new ScriptBucketCollection<AnimationsMesh>(this, this, ScriptType.AnimationMesh){
        {
            Objects.requireNonNull(this$0);
            super(scriptManager, scriptType);
        }

        @Override
        public ScriptBucket<AnimationsMesh> getBucketFromModule(ScriptModule module) {
            return module.animationMeshes;
        }
    });
    private final ScriptBucketCollection<ClockScript> clocks = this.addBucketCollection(new ScriptBucketCollection<ClockScript>(this, this, ScriptType.Clock){
        {
            Objects.requireNonNull(this$0);
            super(scriptManager, scriptType);
        }

        @Override
        public ScriptBucket<ClockScript> getBucketFromModule(ScriptModule module) {
            return module.clocks;
        }
    });
    private final ScriptBucketCollection<MannequinScript> mannequins = this.addBucketCollection(new ScriptBucketCollection<MannequinScript>(this, this, ScriptType.Mannequin){
        {
            Objects.requireNonNull(this$0);
            super(scriptManager, scriptType);
        }

        @Override
        public ScriptBucket<MannequinScript> getBucketFromModule(ScriptModule module) {
            return module.mannequins;
        }

        @Override
        public void onSortAllScripts(ArrayList<MannequinScript> scripts) {
            scripts.sort((o1, o2) -> String.CASE_INSENSITIVE_ORDER.compare(o1.getName(), o2.getName()));
        }
    });
    private final ScriptBucketCollection<ModelScript> models = this.addBucketCollection(new ScriptBucketCollection<ModelScript>(this, this, ScriptType.Model){
        {
            Objects.requireNonNull(this$0);
            super(scriptManager, scriptType);
        }

        @Override
        public ScriptBucket<ModelScript> getBucketFromModule(ScriptModule module) {
            return module.models;
        }
    });
    private final ScriptBucketCollection<PhysicsShapeScript> physicsShapes = this.addBucketCollection(new ScriptBucketCollection<PhysicsShapeScript>(this, this, ScriptType.PhysicsShape){
        {
            Objects.requireNonNull(this$0);
            super(scriptManager, scriptType);
        }

        @Override
        public ScriptBucket<PhysicsShapeScript> getBucketFromModule(ScriptModule module) {
            return module.physicsShapes;
        }
    });
    private final ScriptBucketCollection<GameSoundScript> gameSounds = this.addBucketCollection(new ScriptBucketCollection<GameSoundScript>(this, this, ScriptType.Sound){
        {
            Objects.requireNonNull(this$0);
            super(scriptManager, scriptType);
        }

        @Override
        public ScriptBucket<GameSoundScript> getBucketFromModule(ScriptModule module) {
            return module.gameSounds;
        }
    });
    private final ScriptBucketCollection<SoundTimelineScript> soundTimelines = this.addBucketCollection(new ScriptBucketCollection<SoundTimelineScript>(this, this, ScriptType.SoundTimeline){
        {
            Objects.requireNonNull(this$0);
            super(scriptManager, scriptType);
        }

        @Override
        public ScriptBucket<SoundTimelineScript> getBucketFromModule(ScriptModule module) {
            return module.soundTimelines;
        }
    });
    private final ScriptBucketCollection<SpriteModel> spriteModels = this.addBucketCollection(new ScriptBucketCollection<SpriteModel>(this, this, ScriptType.SpriteModel){
        {
            Objects.requireNonNull(this$0);
            super(scriptManager, scriptType);
        }

        @Override
        public ScriptBucket<SpriteModel> getBucketFromModule(ScriptModule module) {
            return module.spriteModels;
        }
    });
    private final ScriptBucketCollection<VehicleScript> vehicles = this.addBucketCollection(new ScriptBucketCollection<VehicleScript>(this, this, ScriptType.Vehicle){
        {
            Objects.requireNonNull(this$0);
            super(scriptManager, scriptType);
        }

        @Override
        public ScriptBucket<VehicleScript> getBucketFromModule(ScriptModule module) {
            return module.vehicles;
        }
    });
    private final ScriptBucketCollection<RuntimeAnimationScript> animations = this.addBucketCollection(new ScriptBucketCollection<RuntimeAnimationScript>(this, this, ScriptType.RuntimeAnimation){
        {
            Objects.requireNonNull(this$0);
            super(scriptManager, scriptType);
        }

        @Override
        public ScriptBucket<RuntimeAnimationScript> getBucketFromModule(ScriptModule module) {
            return module.animations;
        }
    });
    private final ScriptBucketCollection<VehicleEngineRPM> vehicleEngineRpms = this.addBucketCollection(new ScriptBucketCollection<VehicleEngineRPM>(this, this, ScriptType.VehicleEngineRPM){
        {
            Objects.requireNonNull(this$0);
            super(scriptManager, scriptType);
        }

        @Override
        public ScriptBucket<VehicleEngineRPM> getBucketFromModule(ScriptModule module) {
            return module.vehicleEngineRpms;
        }
    });
    private final ScriptBucketCollection<ItemConfig> itemConfigs = this.addBucketCollection(new ScriptBucketCollection<ItemConfig>(this, this, ScriptType.ItemConfig){
        {
            Objects.requireNonNull(this$0);
            super(scriptManager, scriptType);
        }

        @Override
        public ScriptBucket<ItemConfig> getBucketFromModule(ScriptModule module) {
            return module.itemConfigs;
        }
    });
    private final ScriptBucketCollection<GameEntityScript> entities = this.addBucketCollection(new ScriptBucketCollection<GameEntityScript>(this, this, ScriptType.Entity){
        {
            Objects.requireNonNull(this$0);
            super(scriptManager, scriptType);
        }

        @Override
        public ScriptBucket<GameEntityScript> getBucketFromModule(ScriptModule module) {
            return module.entities;
        }

        @Override
        public void OnPostTileDefinitions() throws Exception {
            super.OnPostTileDefinitions();
            SpriteConfigManager.InitScriptsPostTileDef();
        }
    });
    private final ScriptBucketCollection<XuiConfigScript> xuiConfigScripts = this.addBucketCollection(new ScriptBucketCollection<XuiConfigScript>(this, this, ScriptType.XuiConfig){
        {
            Objects.requireNonNull(this$0);
            super(scriptManager, scriptType);
        }

        @Override
        public ScriptBucket<XuiConfigScript> getBucketFromModule(ScriptModule module) {
            return module.xuiConfigScripts;
        }

        @Override
        public void PostLoadScripts(ScriptLoadMode loadMode) throws Exception {
            super.PostLoadScripts(loadMode);
            XuiManager.ParseScripts();
        }
    });
    private final ScriptBucketCollection<XuiLayoutScript> xuiLayouts = this.addBucketCollection(new ScriptBucketCollection<XuiLayoutScript>(this, this, ScriptType.XuiLayout){
        {
            Objects.requireNonNull(this$0);
            super(scriptManager, scriptType);
        }

        @Override
        public ScriptBucket<XuiLayoutScript> getBucketFromModule(ScriptModule module) {
            return module.xuiLayouts;
        }

        @Override
        public void PostLoadScripts(ScriptLoadMode loadMode) throws Exception {
            super.PostLoadScripts(loadMode);
            XuiManager.ParseScripts();
        }
    });
    private final ScriptBucketCollection<XuiLayoutScript> xuiStyles = this.addBucketCollection(new ScriptBucketCollection<XuiLayoutScript>(this, this, ScriptType.XuiStyle){
        {
            Objects.requireNonNull(this$0);
            super(scriptManager, scriptType);
        }

        @Override
        public ScriptBucket<XuiLayoutScript> getBucketFromModule(ScriptModule module) {
            return module.xuiStyles;
        }

        @Override
        public void PostLoadScripts(ScriptLoadMode loadMode) throws Exception {
            super.PostLoadScripts(loadMode);
            XuiManager.ParseScripts();
        }
    });
    private final ScriptBucketCollection<XuiLayoutScript> xuiDefaultStyles = this.addBucketCollection(new ScriptBucketCollection<XuiLayoutScript>(this, this, ScriptType.XuiDefaultStyle){
        {
            Objects.requireNonNull(this$0);
            super(scriptManager, scriptType);
        }

        @Override
        public ScriptBucket<XuiLayoutScript> getBucketFromModule(ScriptModule module) {
            return module.xuiDefaultStyles;
        }

        @Override
        public void PostLoadScripts(ScriptLoadMode loadMode) throws Exception {
            super.PostLoadScripts(loadMode);
            XuiManager.ParseScripts();
        }
    });
    private final ScriptBucketCollection<XuiColorsScript> xuiGlobalColors = this.addBucketCollection(new ScriptBucketCollection<XuiColorsScript>(this, this, ScriptType.XuiColor){
        {
            Objects.requireNonNull(this$0);
            super(scriptManager, scriptType);
        }

        @Override
        public ScriptBucket<XuiColorsScript> getBucketFromModule(ScriptModule module) {
            return module.xuiGlobalColors;
        }

        @Override
        public void PostLoadScripts(ScriptLoadMode loadMode) throws Exception {
            super.PostLoadScripts(loadMode);
            XuiManager.ParseScripts();
        }
    });
    private final ScriptBucketCollection<XuiSkinScript> xuiSkinScripts = this.addBucketCollection(new ScriptBucketCollection<XuiSkinScript>(this, this, ScriptType.XuiSkin){
        {
            Objects.requireNonNull(this$0);
            super(scriptManager, scriptType);
        }

        @Override
        public ScriptBucket<XuiSkinScript> getBucketFromModule(ScriptModule module) {
            return module.xuiSkinScripts;
        }

        @Override
        public void PostLoadScripts(ScriptLoadMode loadMode) throws Exception {
            super.PostLoadScripts(loadMode);
            XuiManager.ParseScripts();
        }
    });
    private final ScriptBucketCollection<ItemFilterScript> itemFilters = this.addBucketCollection(new ScriptBucketCollection<ItemFilterScript>(this, this, ScriptType.ItemFilter){
        {
            Objects.requireNonNull(this$0);
            super(scriptManager, scriptType);
        }

        @Override
        public ScriptBucket<ItemFilterScript> getBucketFromModule(ScriptModule module) {
            return module.itemFilters;
        }
    });
    private final ScriptBucketCollection<FluidFilterScript> fluidFilters = this.addBucketCollection(new ScriptBucketCollection<FluidFilterScript>(this, this, ScriptType.FluidFilter){
        {
            Objects.requireNonNull(this$0);
            super(scriptManager, scriptType);
        }

        @Override
        public ScriptBucket<FluidFilterScript> getBucketFromModule(ScriptModule module) {
            return module.fluidFilters;
        }
    });
    private final ScriptBucketCollection<CraftRecipe> craftRecipes = this.addBucketCollection(new ScriptBucketCollection<CraftRecipe>(this, this, ScriptType.CraftRecipe){
        {
            Objects.requireNonNull(this$0);
            super(scriptManager, scriptType);
        }

        @Override
        public ScriptBucket<CraftRecipe> getBucketFromModule(ScriptModule module) {
            return module.craftRecipes;
        }

        @Override
        public void PostLoadScripts(ScriptLoadMode loadMode) throws Exception {
            super.PostLoadScripts(loadMode);
            CraftRecipeManager.Init();
        }
    });
    private final ScriptBucketCollection<StringListScript> stringLists = this.addBucketCollection(new ScriptBucketCollection<StringListScript>(this, this, ScriptType.StringList){
        {
            Objects.requireNonNull(this$0);
            super(scriptManager, scriptType);
        }

        @Override
        public ScriptBucket<StringListScript> getBucketFromModule(ScriptModule module) {
            return module.stringLists;
        }
    });
    private final ScriptBucketCollection<EnergyDefinitionScript> energyDefinitionScripts = this.addBucketCollection(new ScriptBucketCollection<EnergyDefinitionScript>(this, this, ScriptType.EnergyDefinition){
        {
            Objects.requireNonNull(this$0);
            super(scriptManager, scriptType);
        }

        @Override
        public ScriptBucket<EnergyDefinitionScript> getBucketFromModule(ScriptModule module) {
            return module.energyDefinitionScripts;
        }

        @Override
        public void PreReloadScripts() throws Exception {
            Energy.PreReloadScripts();
            super.PreReloadScripts();
        }

        @Override
        public void PostLoadScripts(ScriptLoadMode loadMode) throws Exception {
            super.PostLoadScripts(loadMode);
            Energy.Init(loadMode);
        }
    });
    private final ScriptBucketCollection<FluidDefinitionScript> fluidDefinitionScripts = this.addBucketCollection(new ScriptBucketCollection<FluidDefinitionScript>(this, this, ScriptType.FluidDefinition){
        {
            Objects.requireNonNull(this$0);
            super(scriptManager, scriptType);
        }

        @Override
        public ScriptBucket<FluidDefinitionScript> getBucketFromModule(ScriptModule module) {
            return module.fluidDefinitionScripts;
        }

        @Override
        public void PreReloadScripts() throws Exception {
            Fluid.PreReloadScripts();
            super.PreReloadScripts();
        }

        @Override
        public void PostLoadScripts(ScriptLoadMode loadMode) throws Exception {
            super.PostLoadScripts(loadMode);
            Fluid.Init(loadMode);
        }
    });
    private final ScriptBucketCollection<TimedActionScript> timedActionScripts = this.addBucketCollection(new ScriptBucketCollection<TimedActionScript>(this, this, ScriptType.TimedAction){
        {
            Objects.requireNonNull(this$0);
            super(scriptManager, scriptType);
        }

        @Override
        public ScriptBucket<TimedActionScript> getBucketFromModule(ScriptModule module) {
            return module.timedActionScripts;
        }
    });
    private final ScriptBucketCollection<RagdollScript> ragdollScripts = this.addBucketCollection(new ScriptBucketCollection<RagdollScript>(this, this, ScriptType.Ragdoll){
        {
            Objects.requireNonNull(this$0);
            super(scriptManager, scriptType);
        }

        @Override
        public ScriptBucket<RagdollScript> getBucketFromModule(ScriptModule module) {
            return module.ragdollScripts;
        }

        @Override
        public void PostLoadScripts(ScriptLoadMode loadMode) throws Exception {
            super.PostLoadScripts(loadMode);
            RagdollScript.toBullet(false);
        }
    });
    private final ScriptBucketCollection<PhysicsHitReactionScript> physicsHitReactionScripts = this.addBucketCollection(new ScriptBucketCollection<PhysicsHitReactionScript>(this, this, ScriptType.PhysicsHitReaction){
        {
            Objects.requireNonNull(this$0);
            super(scriptManager, scriptType);
        }

        @Override
        public ScriptBucket<PhysicsHitReactionScript> getBucketFromModule(ScriptModule module) {
            return module.physicsHitReactionScripts;
        }
    });
    private final ScriptBucketCollection<CharacterTraitDefinitionScript> characterTraitScripts = this.addBucketCollection(new ScriptBucketCollection<CharacterTraitDefinitionScript>(this, this, ScriptType.CharacterTraitDefinition){
        {
            Objects.requireNonNull(this$0);
            super(scriptManager, scriptType);
        }

        @Override
        public ScriptBucket<CharacterTraitDefinitionScript> getBucketFromModule(ScriptModule module) {
            return module.characterTraitScripts;
        }
    });
    private final ScriptBucketCollection<CharacterProfessionDefinitionScript> characterProfessionScripts = this.addBucketCollection(new ScriptBucketCollection<CharacterProfessionDefinitionScript>(this, this, ScriptType.CharacterProfessionDefinition){
        {
            Objects.requireNonNull(this$0);
            super(scriptManager, scriptType);
        }

        @Override
        public ScriptBucket<CharacterProfessionDefinitionScript> getBucketFromModule(ScriptModule module) {
            return module.characterProfessionScripts;
        }
    });
    public static final String Base = "Base";
    public static final String Base_Module = "Base.";
    private String checksum = "";
    private HashMap<String, String> tempFileToModMap;
    private static String currentLoadFileMod;
    private static String currentLoadFileAbsPath;
    private static String currentLoadFileName;
    public static final String VanillaID = "pz-vanilla";

    public static void EnableDebug(ScriptType type, boolean enable) {
        if (enable) {
            debugTypes.add(type);
        } else {
            debugTypes.remove((Object)type);
        }
    }

    public static boolean isDebugEnabled(ScriptType type) {
        return debugTypes.contains((Object)type);
    }

    public static void println(ScriptType type, String msg) {
        if (debugTypes.contains((Object)type)) {
            DebugLog.Script.println("[" + type.toString() + "] " + msg);
        }
    }

    public static void println(BaseScriptObject scriptObject, String msg) {
        ScriptManager.println(scriptObject.getScriptObjectType(), msg);
    }

    private <T extends BaseScriptObject> ScriptBucketCollection<T> addBucketCollection(ScriptBucketCollection<T> collection) {
        if (this.bucketCollectionMap.containsKey((Object)collection.getScriptType())) {
            throw new RuntimeException("ScriptType collection already added.");
        }
        this.bucketCollectionMap.put(collection.getScriptType(), collection);
        this.bucketCollectionList.add(collection);
        return collection;
    }

    public ArrayList<?> getScriptsForType(ScriptType type) {
        if (this.bucketCollectionMap.containsKey((Object)type)) {
            return this.bucketCollectionMap.get((Object)type).getAllScripts();
        }
        DebugLog.General.warn("Type has no bucket collection: " + String.valueOf((Object)type));
        return new ArrayList();
    }

    public VehicleTemplate getVehicleTemplate(String name) {
        return this.vehicleTemplates.getScript(name);
    }

    public ArrayList<VehicleTemplate> getAllVehicleTemplates() {
        return this.vehicleTemplates.getAllScripts();
    }

    public GameEntityTemplate getGameEntityTemplate(String name) {
        return this.entityTemplates.getScript(name);
    }

    public ArrayList<GameEntityTemplate> getAllGameEntityTemplates() {
        return this.entityTemplates.getAllScripts();
    }

    @Override
    public Item getItem(String name) {
        return this.items.getScript(name);
    }

    public ArrayList<Item> getAllItems() {
        return this.items.getAllScripts();
    }

    @Override
    public Recipe getRecipe(String name) {
        return this.recipes.getScript(name);
    }

    public ArrayList<Recipe> getAllRecipes() {
        return this.recipes.getAllScripts();
    }

    public UniqueRecipe getUniqueRecipe(String name) {
        return this.uniqueRecipes.getScript(name);
    }

    public Stack<UniqueRecipe> getAllUniqueRecipes() {
        this.uniqueRecipeTempStack.clear();
        this.uniqueRecipeTempStack.addAll(this.uniqueRecipes.getAllScripts());
        return this.uniqueRecipeTempStack;
    }

    public EvolvedRecipe getEvolvedRecipe(String name) {
        return this.evolvedRecipes.getScript(name);
    }

    public ArrayList<EvolvedRecipe> getAllEvolvedRecipesList() {
        return this.evolvedRecipes.getAllScripts();
    }

    public Stack<EvolvedRecipe> getAllEvolvedRecipes() {
        this.evolvedRecipeTempStack.clear();
        this.evolvedRecipeTempStack.addAll(this.evolvedRecipes.getAllScripts());
        return this.evolvedRecipeTempStack;
    }

    public Fixing getFixing(String name) {
        return this.fixings.getScript(name);
    }

    public ArrayList<Fixing> getAllFixing(ArrayList<Fixing> result) {
        result.addAll(this.fixings.getAllScripts());
        return result;
    }

    public AnimationsMesh getAnimationsMesh(String name) {
        return this.animationMeshes.getScript(name);
    }

    public ArrayList<AnimationsMesh> getAllAnimationsMeshes() {
        return this.animationMeshes.getAllScripts();
    }

    public ClockScript getClockScript(String name) {
        return this.clocks.getScript(name);
    }

    public ArrayList<ClockScript> getAllClockScripts() {
        return this.clocks.getAllScripts();
    }

    public MannequinScript getMannequinScript(String name) {
        return this.mannequins.getScript(name);
    }

    public ArrayList<MannequinScript> getAllMannequinScripts() {
        return this.mannequins.getAllScripts();
    }

    public ModelScript getModelScript(String name) {
        return this.models.getScript(name);
    }

    public ArrayList<ModelScript> getAllModelScripts() {
        return this.models.getAllScripts();
    }

    public void addModelScript(ModelScript modelScript) {
        ScriptModule module = modelScript.getModule();
        module.models.scriptList.add(modelScript);
        module.models.scriptMap.put(modelScript.getScriptObjectName(), modelScript);
        if (modelScript.getScriptObjectName().contains(".")) {
            module.models.dotInName.add(modelScript.getScriptObjectName());
        }
        this.models.getAllScripts().clear();
        this.models.getFullTypeToScriptMap().put(modelScript.getScriptObjectFullType(), modelScript);
    }

    public PhysicsShapeScript getPhysicsShape(String name) {
        return this.physicsShapes.getScript(name);
    }

    public ArrayList<PhysicsShapeScript> getAllPhysicsShapes() {
        return this.physicsShapes.getAllScripts();
    }

    public GameSoundScript getGameSound(String name) {
        return this.gameSounds.getScript(name);
    }

    public ArrayList<GameSoundScript> getAllGameSounds() {
        return new ArrayList<GameSoundScript>(this.gameSounds.getAllScripts());
    }

    public SoundTimelineScript getSoundTimeline(String name) {
        return this.soundTimelines.getScript(name);
    }

    public ArrayList<SoundTimelineScript> getAllSoundTimelines() {
        return this.soundTimelines.getAllScripts();
    }

    public SpriteModel getSpriteModel(String name) {
        return this.spriteModels.getScript(name);
    }

    public ArrayList<SpriteModel> getAllSpriteModels() {
        return this.spriteModels.getAllScripts();
    }

    public void addSpriteModel(SpriteModel spriteModel) {
        ScriptModule module = spriteModel.getModule();
        module.spriteModels.getScriptList().add(spriteModel);
        module.spriteModels.getScriptMap().put(spriteModel.getScriptObjectName(), spriteModel);
        if (spriteModel.getScriptObjectName().contains(".")) {
            module.spriteModels.dotInName.add(spriteModel.getScriptObjectName());
        }
        this.spriteModels.getFullTypeToScriptMap().put(spriteModel.getScriptObjectFullType(), spriteModel);
    }

    public VehicleScript getVehicle(String name) {
        return this.vehicles.getScript(name);
    }

    public ArrayList<VehicleScript> getAllVehicleScripts() {
        return this.vehicles.getAllScripts();
    }

    public VehicleScript getRandomVehicleScript() {
        List<VehicleScript> vehiclesList = this.vehicles.getAllScripts().stream().filter(vehicleScript -> !vehicleScript.getName().toLowerCase().contains("burnt") && !vehicleScript.getName().toLowerCase().contains("smashed")).toList();
        return vehiclesList.get(Rand.Next(vehiclesList.size()));
    }

    public RuntimeAnimationScript getRuntimeAnimationScript(String name) {
        return this.animations.getScript(name);
    }

    public ArrayList<RuntimeAnimationScript> getAllRuntimeAnimationScripts() {
        return new ArrayList<RuntimeAnimationScript>(this.animations.getAllScripts());
    }

    public VehicleEngineRPM getVehicleEngineRPM(String name) {
        return this.vehicleEngineRpms.getScript(name);
    }

    public ArrayList<VehicleEngineRPM> getAllVehicleEngineRPMs() {
        return this.vehicleEngineRpms.getAllScripts();
    }

    public ItemConfig getItemConfig(String name) {
        return this.itemConfigs.getScript(name);
    }

    public ArrayList<ItemConfig> getAllItemConfigs() {
        return this.itemConfigs.getAllScripts();
    }

    public GameEntityScript getGameEntityScript(String name) {
        return this.entities.getScript(name);
    }

    public ArrayList<GameEntityScript> getAllGameEntities() {
        return this.entities.getAllScripts();
    }

    public ArrayList<CraftRecipe> getAllBuildableRecipes() {
        ArrayList<CraftRecipe> allBuildableRecipes = new ArrayList<CraftRecipe>();
        ArrayList<GameEntityScript> entityScripts = instance.getAllGameEntities();
        block0: for (int i = 0; i < entityScripts.size(); ++i) {
            ArrayList<ComponentScript> allComponents = entityScripts.get(i).getComponentScripts();
            for (int j = 0; j < allComponents.size(); ++j) {
                CraftRecipe craftRecipe;
                if (allComponents.get((int)j).type != ComponentType.CraftRecipe) continue;
                CraftRecipeComponentScript componentScript = (CraftRecipeComponentScript)allComponents.get(j);
                CraftRecipe craftRecipe2 = craftRecipe = componentScript != null ? componentScript.getCraftRecipe() : null;
                if (craftRecipe == null || !craftRecipe.isBuildableRecipe()) continue;
                allBuildableRecipes.add(craftRecipe);
                continue block0;
            }
        }
        return allBuildableRecipes;
    }

    public CraftRecipe getBuildableRecipe(String recipe) {
        ArrayList allBuildableRecipes = new ArrayList();
        ArrayList<GameEntityScript> entityScripts = instance.getAllGameEntities();
        for (int i = 0; i < entityScripts.size(); ++i) {
            ArrayList<ComponentScript> allComponents = entityScripts.get(i).getComponentScripts();
            for (int j = 0; j < allComponents.size(); ++j) {
                CraftRecipe craftRecipe;
                if (allComponents.get((int)j).type != ComponentType.CraftRecipe) continue;
                CraftRecipeComponentScript componentScript = (CraftRecipeComponentScript)allComponents.get(j);
                CraftRecipe craftRecipe2 = craftRecipe = componentScript != null ? componentScript.getCraftRecipe() : null;
                if (craftRecipe == null || !craftRecipe.isBuildableRecipe() || !Objects.equals(craftRecipe.getName(), recipe)) continue;
                return craftRecipe;
            }
        }
        return null;
    }

    public XuiConfigScript getXuiConfigScript(String name) {
        return this.xuiConfigScripts.getScript(name);
    }

    public ArrayList<XuiConfigScript> getAllXuiConfigScripts() {
        return this.xuiConfigScripts.getAllScripts();
    }

    public XuiLayoutScript getXuiLayout(String name) {
        return this.xuiLayouts.getScript(name);
    }

    public ArrayList<XuiLayoutScript> getAllXuiLayouts() {
        return this.xuiLayouts.getAllScripts();
    }

    public XuiLayoutScript getXuiStyle(String name) {
        return this.xuiStyles.getScript(name);
    }

    public ArrayList<XuiLayoutScript> getAllXuiStyles() {
        return this.xuiStyles.getAllScripts();
    }

    public XuiLayoutScript getXuiDefaultStyle(String name) {
        return this.xuiDefaultStyles.getScript(name);
    }

    public ArrayList<XuiLayoutScript> getAllXuiDefaultStyles() {
        return this.xuiDefaultStyles.getAllScripts();
    }

    public XuiColorsScript getXuiColor(String name) {
        return this.xuiGlobalColors.getScript(name);
    }

    public ArrayList<XuiColorsScript> getAllXuiColors() {
        return this.xuiGlobalColors.getAllScripts();
    }

    public XuiSkinScript getXuiSkinScript(String name) {
        return this.xuiSkinScripts.getScript(name);
    }

    public ArrayList<XuiSkinScript> getAllXuiSkinScripts() {
        return this.xuiSkinScripts.getAllScripts();
    }

    public ItemFilterScript getItemFilter(String name) {
        return this.itemFilters.getScript(name);
    }

    public ArrayList<ItemFilterScript> getAllItemFilters() {
        return this.itemFilters.getAllScripts();
    }

    public FluidFilterScript getFluidFilter(String name) {
        return this.fluidFilters.getScript(name);
    }

    public ArrayList<FluidFilterScript> getAllFluidFilters() {
        return this.fluidFilters.getAllScripts();
    }

    public CraftRecipe getCraftRecipe(String name) {
        CraftRecipeComponentScript craftRecipeComponent;
        GameEntityScript entityScript;
        CraftRecipe recipe = this.craftRecipes.getScript(name);
        if (recipe == null && (entityScript = this.entities.getScript(name)) != null && (craftRecipeComponent = (CraftRecipeComponentScript)entityScript.getComponentScriptFor(ComponentType.CraftRecipe)) != null) {
            recipe = craftRecipeComponent.getCraftRecipe();
        }
        return recipe;
    }

    public ArrayList<CraftRecipe> getAllCraftRecipes() {
        return this.craftRecipes.getAllScripts();
    }

    public void VerifyAllCraftRecipesAreLearnable() {
        if (!Core.debug) {
            return;
        }
        DebugLog.log("Verifying that all Craft Recipes are Learnable");
        boolean good = true;
        ArrayList<CraftRecipe> failedVerificationRecipes = new ArrayList<CraftRecipe>();
        ArrayList<String> allMagazineRecipes = new ArrayList<String>();
        ArrayList<Item> allItems = instance.getAllItems();
        for (Item item : allItems) {
            List<String> taughtRecipes = item.getLearnedRecipes();
            if (taughtRecipes == null) continue;
            for (String taughtRecipe : taughtRecipes) {
                if (allMagazineRecipes.contains(taughtRecipe)) continue;
                allMagazineRecipes.add(taughtRecipe);
            }
        }
        ArrayList<CraftRecipe> allRecipes = this.craftRecipes.getAllScripts();
        for (CraftRecipe craftRecipe : allRecipes) {
            if (!craftRecipe.needToBeLearn() || craftRecipe.getAutoLearnAllSkillCount() > 0 || craftRecipe.getAutoLearnAnySkillCount() > 0 || allMagazineRecipes.contains(craftRecipe.getName())) continue;
            failedVerificationRecipes.add(craftRecipe);
        }
        for (CraftRecipe craftRecipe : failedVerificationRecipes) {
            good = false;
            boolean learnable = false;
            StringBuilder warnString = new StringBuilder("CraftRecipe " + craftRecipe.getName() + " only learnable by:");
            if (craftRecipe.canBeResearched()) {
                warnString.append(" Research.");
                learnable = true;
            }
            ArrayList<CharacterProfessionDefinition> characterProfessionDefinitions = CharacterProfessionDefinition.getProfessions();
            ArrayList<String> recipeProfessions = new ArrayList<String>();
            for (CharacterProfessionDefinition characterProfessionDefinition : characterProfessionDefinitions) {
                if (!characterProfessionDefinition.isGrantedRecipe(craftRecipe.getName())) continue;
                recipeProfessions.add(characterProfessionDefinition.getType().toString());
                learnable = true;
            }
            if (!recipeProfessions.isEmpty()) {
                warnString.append(" Profession(");
                for (String recipeProfession : recipeProfessions) {
                    warnString.append(recipeProfession + ";");
                }
                warnString.append(")");
            }
            if (learnable) {
                DebugLog.log(String.valueOf(warnString));
                continue;
            }
            DebugLog.log("CraftRecipe " + craftRecipe.getName() + " is not learnable");
        }
        for (String learnableRecipe : allMagazineRecipes) {
            boolean found = false;
            for (CraftRecipe craftRecipe : allRecipes) {
                if (craftRecipe.getName().equalsIgnoreCase(learnableRecipe)) {
                    found = true;
                    break;
                }
                if (craftRecipe.getMetaRecipe() == null || !craftRecipe.getMetaRecipe().equalsIgnoreCase(learnableRecipe)) continue;
                found = true;
                break;
            }
            if (LuaManager.caller.protectedCallBoolean(LuaManager.thread, LuaManager.getFunctionObject("doesSeasonRecipeExist"), learnableRecipe).booleanValue()) {
                found = true;
            }
            if (LuaManager.caller.protectedCallBoolean(LuaManager.thread, LuaManager.getFunctionObject("doesMiscRecipeExist"), learnableRecipe).booleanValue()) {
                found = true;
            } else if (learnableRecipe.equalsIgnoreCase("Basic Mechanics") || learnableRecipe.equalsIgnoreCase("Intermediate Mechanics") || learnableRecipe.equalsIgnoreCase("Advanced Mechanics") || learnableRecipe.equalsIgnoreCase("Herbalist") || learnableRecipe.equalsIgnoreCase("Generator")) {
                found = true;
            }
            ArrayList<SpriteConfigManager.ObjectInfo> objectInfos = SpriteConfigManager.GetObjectInfoList();
            for (SpriteConfigManager.ObjectInfo objectInfo : objectInfos) {
                GameEntityScript entityScript;
                if (objectInfo.getScript() == null || objectInfo.getScript().getParent() == null || (entityScript = (GameEntityScript)objectInfo.getScript().getParent()) == null || !entityScript.getName().equalsIgnoreCase(learnableRecipe)) continue;
                found = true;
                break;
            }
            if (found) continue;
            good = false;
            DebugLog.log("Learnable CraftRecipe " + learnableRecipe + " does not exist");
        }
        if (good) {
            DebugLog.log("Verified that all Craft Recipes are Learnable without any issues.");
        } else {
            DebugLog.log("Craft Recipe Learning possible issues detected; this may be a false positive on account of intentional design however.");
        }
    }

    public void checkAutoLearn(IsoGameCharacter chr) {
        ArrayList<CraftRecipe> recipes = this.craftRecipes.getAllScripts();
        for (int i = 0; i < recipes.size(); ++i) {
            CraftRecipe recipe = recipes.get(i);
            if (!chr.isRecipeActuallyKnown(recipe) && recipe.getAutoLearnAnySkillCount() > 0) {
                recipe.checkAutoLearnAnySkills(chr);
            }
            if (chr.isRecipeActuallyKnown(recipe) || recipe.getAutoLearnAllSkillCount() <= 0) continue;
            recipe.checkAutoLearnAllSkills(chr);
        }
    }

    public void checkMetaRecipes(IsoGameCharacter chr) {
        ArrayList<CraftRecipe> recipes = this.craftRecipes.getAllScripts();
        for (int i = 0; i < recipes.size(); ++i) {
            CraftRecipe recipe = recipes.get(i);
            if (chr.isRecipeActuallyKnown(recipe) || recipe.getMetaRecipe() == null) continue;
            recipe.checkMetaRecipe(chr);
        }
    }

    public void checkMetaRecipe(IsoGameCharacter chr, String checkRecipe) {
        ArrayList<CraftRecipe> recipes = this.craftRecipes.getAllScripts();
        for (int i = 0; i < recipes.size(); ++i) {
            CraftRecipe recipe = recipes.get(i);
            if (chr.isRecipeActuallyKnown(recipe) || recipe.getMetaRecipe() == null || !Objects.equals(recipe.getMetaRecipe(), checkRecipe)) continue;
            recipe.checkMetaRecipe(chr, checkRecipe);
        }
    }

    public StringListScript getStringList(String name) {
        return this.stringLists.getScript(name);
    }

    public ArrayList<StringListScript> getAllStringLists() {
        return this.stringLists.getAllScripts();
    }

    public EnergyDefinitionScript getEnergyDefinitionScript(String name) {
        return this.energyDefinitionScripts.getScript(name);
    }

    public ArrayList<EnergyDefinitionScript> getAllEnergyDefinitionScripts() {
        return this.energyDefinitionScripts.getAllScripts();
    }

    public FluidDefinitionScript getFluidDefinitionScript(String name) {
        return this.fluidDefinitionScripts.getScript(name);
    }

    public ArrayList<FluidDefinitionScript> getAllFluidDefinitionScripts() {
        return this.fluidDefinitionScripts.getAllScripts();
    }

    public TimedActionScript getTimedActionScript(String name) {
        return this.timedActionScripts.getScript(name);
    }

    public ArrayList<TimedActionScript> getAllTimedActionScripts() {
        return this.timedActionScripts.getAllScripts();
    }

    public RagdollScript getRagdollScript(String name) {
        return this.ragdollScripts.getScript(name);
    }

    public PhysicsHitReactionScript getPhysicsHitReactionScript(String name) {
        return this.physicsHitReactionScripts.getScript(name);
    }

    public CharacterTraitDefinitionScript getCharacterTraitScript(String name) {
        return this.characterTraitScripts.getScript(name);
    }

    public CharacterProfessionDefinitionScript getCharacterProfessionScript(String name) {
        return this.characterProfessionScripts.getScript(name);
    }

    public ScriptManager() {
        Collections.sort(this.bucketCollectionList, Comparator.comparing(ScriptBucketCollection::isTemplate, Comparator.reverseOrder()));
    }

    public void update() {
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public void LoadFile(ScriptLoadMode loadMode, String filename, boolean bLoadJar) throws FileNotFoundException {
        if (DebugLog.isEnabled(DebugType.Script)) {
            DebugLog.Script.debugln(filename + (bLoadJar ? " bLoadJar" : ""));
        }
        if (!GameServer.server) {
            Thread.yield();
            Core.getInstance().DoFrameReady();
        }
        if (filename.contains(".tmx")) {
            IsoWorld.mapPath = filename.substring(0, filename.lastIndexOf("/"));
            IsoWorld.mapUseJar = bLoadJar;
            DebugLog.Script.debugln("  file is a .tmx (map) file. Set mapPath to " + IsoWorld.mapPath + (IsoWorld.mapUseJar ? " mapUseJar" : ""));
            return;
        }
        if (!filename.endsWith(".txt")) {
            DebugLog.Script.warn(" file is not a .txt (script) file: " + filename);
            return;
        }
        InputStreamReader isr = IndieFileLoader.getStreamReader(filename, !bLoadJar);
        BufferedReader br = new BufferedReader(isr);
        this.buf.setLength(0);
        try {
            String inputLine;
            while ((inputLine = br.readLine()) != null) {
                this.buf.append(inputLine);
                this.buf.append('\n');
            }
        }
        catch (Exception ex) {
            DebugLog.Script.error("Exception thrown reading file " + filename + "\n  " + String.valueOf(ex));
            return;
        }
        finally {
            try {
                br.close();
                isr.close();
            }
            catch (Exception ex) {
                DebugType.Script.printException(ex, "Exception thrown closing file " + filename + "\n  ", LogSeverity.Error);
            }
        }
        String totalFile = this.buf.toString();
        totalFile = ScriptParser.stripComments(totalFile);
        this.currentFileName = filename;
        this.registerLoadFileName(this.currentFileName);
        this.ParseScript(loadMode, totalFile);
        this.currentFileName = null;
    }

    private void registerLoadFileName(String name) {
        if (!this.loadFileNames.contains(name)) {
            this.loadFileNames.add(name);
        }
    }

    public void ParseScript(ScriptLoadMode loadMode, String totalFile) {
        ArrayList<String> tokens = ScriptParser.parseTokens(totalFile);
        for (int n = 0; n < tokens.size(); ++n) {
            String token = tokens.get(n);
            this.CreateFromToken(loadMode, token);
        }
    }

    private void CreateFromToken(ScriptLoadMode loadMode, String token) {
        if ((token = token.trim()).indexOf("module") == 0) {
            int firstopen = token.indexOf("{");
            int lastClose = token.lastIndexOf("}");
            String[] waypoint = token.split("[{}]");
            String name = waypoint[0];
            name = name.replace("module", "");
            name = name.trim();
            String actual = token.substring(firstopen + 1, lastClose);
            ScriptModule way = this.moduleMap.get(name);
            if (way == null) {
                if (DebugLog.isEnabled(DebugType.Script)) {
                    DebugLog.Script.debugln("Adding new module: " + name);
                }
                way = new ScriptModule();
                this.moduleMap.put(name, way);
                this.moduleList.add(way);
                for (ScriptBucketCollection<?> collection : this.bucketCollectionList) {
                    collection.registerModule(way);
                }
            }
            way.Load(loadMode, name, actual);
        }
    }

    public void searchFolders(URI base, File fo, ArrayList<String> loadList) {
        if (fo.isDirectory()) {
            if (fo.getAbsolutePath().contains("tempNotWorking")) {
                return;
            }
            String[] internalNames = fo.list();
            for (int i = 0; i < internalNames.length; ++i) {
                this.searchFolders(base, new File(fo.getAbsolutePath() + File.separator + internalNames[i]), loadList);
            }
        } else if (fo.getAbsolutePath().toLowerCase().endsWith(".txt")) {
            String relPath = ZomboidFileSystem.instance.getRelativeFile(base, fo.getAbsolutePath());
            relPath = relPath.toLowerCase(Locale.ENGLISH);
            loadList.add(relPath);
        }
    }

    public static String getItemName(String name) {
        int p = name.indexOf(46);
        if (p == -1) {
            return name;
        }
        return name.substring(p + 1);
    }

    public ScriptModule getModule(String name) {
        return this.getModule(name, true);
    }

    public ScriptModule getModule(String name, boolean defaultToBase) {
        if (name.trim().equals(Base) || name.startsWith(Base_Module)) {
            return this.moduleMap.get(Base);
        }
        if (this.cachedModules.containsKey(name)) {
            return this.cachedModules.get(name);
        }
        ScriptModule ret = null;
        if (this.moduleAliases.containsKey(name)) {
            name = this.moduleAliases.get(name);
        }
        if (this.cachedModules.containsKey(name)) {
            return this.cachedModules.get(name);
        }
        if (this.moduleMap.containsKey(name)) {
            ret = this.moduleMap.get((Object)name).disabled ? null : this.moduleMap.get(name);
        }
        if (ret != null) {
            this.cachedModules.put(name, ret);
            return ret;
        }
        int idx = name.indexOf(".");
        if (idx != -1) {
            ret = this.getModule(name.substring(0, idx));
        }
        if (ret != null) {
            this.cachedModules.put(name, ret);
            return ret;
        }
        return defaultToBase ? this.moduleMap.get(Base) : null;
    }

    public ScriptModule getModuleNoDisableCheck(String name) {
        if (this.moduleAliases.containsKey(name)) {
            name = this.moduleAliases.get(name);
        }
        if (this.moduleMap.containsKey(name)) {
            return this.moduleMap.get(name);
        }
        if (name.indexOf(".") != -1) {
            return this.getModule(name.split("\\.")[0]);
        }
        return null;
    }

    public Item FindItem(String name) {
        return this.FindItem(name, true);
    }

    public Item FindItem(String name, boolean moduleDefaultsToBase) {
        if (name.contains(".") && this.items.hasFullType(name)) {
            return this.items.getFullType(name);
        }
        ScriptModule module = this.getModule(name, moduleDefaultsToBase);
        if (module == null) {
            return null;
        }
        Item item = module.getItem(ScriptManager.getItemName(name));
        if (item == null) {
            for (int i = 0; i < this.moduleList.size(); ++i) {
                ScriptModule m = this.moduleList.get(i);
                if (m.disabled || (item = module.getItem(ScriptManager.getItemName(name))) == null) continue;
                return item;
            }
        }
        return item;
    }

    public boolean isDrainableItemType(String itemType) {
        Item scriptItem = this.FindItem(itemType);
        if (scriptItem != null) {
            return scriptItem.isItemType(ItemType.DRAINABLE);
        }
        return false;
    }

    public void CheckExitPoints() {
        for (int i = 0; i < this.moduleList.size(); ++i) {
            ScriptModule m = this.moduleList.get(i);
            if (m.disabled || !m.CheckExitPoints()) continue;
            return;
        }
    }

    private ArrayList<Item> getAllItemsWithTag(ItemTag itemTag) {
        ArrayList<Item> items = this.tagToItemMap.get(itemTag);
        if (items != null) {
            return items;
        }
        items = new ArrayList();
        ArrayList<Item> all = this.getAllItems();
        for (int i = 0; i < all.size(); ++i) {
            Item item = all.get(i);
            if (!item.hasTag(itemTag)) continue;
            items.add(item);
        }
        this.tagToItemMap.put(itemTag, items);
        return items;
    }

    public ArrayList<Item> getItemsTag(ItemTag itemTag) {
        return this.getAllItemsWithTag(itemTag);
    }

    public ArrayList<Item> getItemsByType(String type) {
        if (StringUtils.isNullOrWhitespace(type)) {
            throw new IllegalArgumentException("invalid type \"" + type + "\"");
        }
        ArrayList<Item> items = this.typeToItemMap.get(type);
        if (items != null) {
            return items;
        }
        items = new ArrayList();
        for (int i = 0; i < this.moduleList.size(); ++i) {
            Item item;
            ScriptModule m = this.moduleList.get(i);
            if (m.disabled || (item = this.items.getFullType(StringUtils.moduleDotType(m.name, type))) == null) continue;
            items.add(item);
        }
        this.typeToItemMap.put(type, items);
        return items;
    }

    public void Reset() {
        for (ScriptModule scriptModule : this.moduleList) {
            scriptModule.Reset();
        }
        for (ScriptBucketCollection scriptBucketCollection : this.bucketCollectionList) {
            scriptBucketCollection.reset();
        }
        this.moduleMap.clear();
        this.moduleList.clear();
        this.moduleAliases.clear();
        this.cachedModules.clear();
        this.tagToItemMap.clear();
        this.typeToItemMap.clear();
        this.clothingToItemMap.clear();
        this.hasLoadErrors = false;
        CharacterProfessionDefinition.reset();
        CharacterTraitDefinition.reset();
        PhysicsHitReactionScript.Reset();
    }

    public String getChecksum() {
        return this.checksum;
    }

    public static String getCurrentLoadFileMod() {
        return currentLoadFileMod;
    }

    public static String getCurrentLoadFileAbsPath() {
        return currentLoadFileAbsPath;
    }

    public static String getCurrentLoadFileName() {
        return currentLoadFileName;
    }

    public void Load() throws IOException {
        try {
            this.loadFileNames.clear();
            WorldDictionary.StartScriptLoading();
            this.tempFileToModMap = new HashMap();
            ArrayList<String> gameFiles = new ArrayList<String>();
            this.searchFolders(ZomboidFileSystem.instance.base.lowercaseUri, ZomboidFileSystem.instance.getMediaFile("scripts"), gameFiles);
            for (String file : gameFiles) {
                this.tempFileToModMap.put(ZomboidFileSystem.instance.getAbsolutePath(file), VanillaID);
            }
            ArrayList<String> modFiles = new ArrayList<String>();
            ArrayList<String> root = ZomboidFileSystem.instance.getModIDs();
            for (int n = 0; n < root.size(); ++n) {
                String file;
                int i;
                File canonicalScripts;
                File canonicalMedia;
                int firstFileInThisMod;
                URI lowercaseURI;
                URI modDirURI;
                File modDirFile;
                ChooseGameInfo.Mod mod = ChooseGameInfo.getAvailableModDetails(root.get(n));
                if (mod == null) continue;
                String modDir = mod.getCommonDir();
                if (modDir != null) {
                    modDirFile = new File(modDir);
                    modDirURI = modDirFile.toURI();
                    lowercaseURI = new File(modDirFile.getCanonicalFile().getPath().toLowerCase(Locale.ENGLISH)).toURI();
                    firstFileInThisMod = modFiles.size();
                    canonicalMedia = ZomboidFileSystem.instance.getCanonicalFile(modDirFile, "media");
                    canonicalScripts = ZomboidFileSystem.instance.getCanonicalFile(canonicalMedia, "scripts");
                    this.searchFolders(lowercaseURI, canonicalScripts, modFiles);
                    if (root.get(n).equals(VanillaID)) {
                        throw new RuntimeException("Warning mod id is named pz-vanilla!");
                    }
                    for (i = firstFileInThisMod; i < modFiles.size(); ++i) {
                        file = modFiles.get(i);
                        this.tempFileToModMap.put(ZomboidFileSystem.instance.getAbsolutePath(file), root.get(n));
                    }
                }
                if ((modDir = mod.getVersionDir()) == null) continue;
                modDirFile = new File(modDir);
                modDirURI = modDirFile.toURI();
                lowercaseURI = new File(modDirFile.getCanonicalFile().getPath().toLowerCase(Locale.ENGLISH)).toURI();
                firstFileInThisMod = modFiles.size();
                canonicalMedia = ZomboidFileSystem.instance.getCanonicalFile(modDirFile, "media");
                canonicalScripts = ZomboidFileSystem.instance.getCanonicalFile(canonicalMedia, "scripts");
                this.searchFolders(lowercaseURI, canonicalScripts, modFiles);
                if (root.get(n).equals(VanillaID)) {
                    throw new RuntimeException("Warning mod id is named pz-vanilla!");
                }
                for (i = firstFileInThisMod; i < modFiles.size(); ++i) {
                    file = modFiles.get(i);
                    this.tempFileToModMap.put(ZomboidFileSystem.instance.getAbsolutePath(file), root.get(n));
                }
            }
            Comparator<String> comp = new Comparator<String>(this){
                {
                    Objects.requireNonNull(this$0);
                }

                @Override
                public int compare(String o1, String o2) {
                    String name1 = new File(o1).getName();
                    String name2 = new File(o2).getName();
                    if (name1.startsWith("template_") && !name2.startsWith("template_")) {
                        return -1;
                    }
                    if (!name1.startsWith("template_") && name2.startsWith("template_")) {
                        return 1;
                    }
                    return o1.compareTo(o2);
                }
            };
            Collections.sort(gameFiles, comp);
            Collections.sort(modFiles, comp);
            PZArrayUtil.addAll(gameFiles, modFiles);
            if (GameClient.client || GameServer.server) {
                NetChecksum.checksummer.reset(true);
                NetChecksum.GroupOfFiles.initChecksum();
            }
            HashSet<String> done = new HashSet<String>();
            for (String s : gameFiles) {
                String absPath;
                if (done.contains(s)) continue;
                done.add(s);
                currentLoadFileAbsPath = absPath = ZomboidFileSystem.instance.getAbsolutePath(s);
                currentLoadFileName = FileName.getName(absPath);
                currentLoadFileMod = this.tempFileToModMap.get(absPath);
                this.LoadFile(ScriptLoadMode.Init, s, false);
                if (!GameClient.client && !GameServer.server) continue;
                NetChecksum.checksummer.addFile(s, absPath);
            }
            if (GameClient.client || GameServer.server) {
                this.checksum = NetChecksum.checksummer.checksumToString();
                if (GameServer.server) {
                    DebugLog.General.println("scriptChecksum: " + this.checksum);
                }
            }
        }
        catch (Exception ex) {
            ExceptionLogger.logException(ex);
        }
        this.buf.setLength(0);
        this.buf.trimToSize();
        this.loadScripts(ScriptLoadMode.Init, EnumSet.allOf(ScriptType.class));
        if (Core.debug && this.hasLoadErrors()) {
            throw new IOException("Script load errors.");
        }
        this.debugItems();
        this.resolveItemTypes();
        WorldDictionary.ScriptsLoaded();
        RecipeManager.ScriptsLoaded();
        GameSounds.ScriptsLoaded();
        ModelScript.ScriptsLoaded();
        if (SoundManager.instance != null) {
            SoundManager.instance.debugScriptSounds();
        }
        Translator.debugItemEvolvedRecipeNames();
        Translator.debugItemNames();
        Translator.debugMultiStageBuildNames();
        Translator.debugRecipeNames();
        Translator.debugRecipeGroupNames();
        this.createClothingItemMap();
        this.createZedDmgMap();
    }

    public void ReloadScripts(ScriptType type) {
        this.ReloadScripts(EnumSet.of(type));
    }

    public void ReloadScripts(EnumSet<ScriptType> types) {
        DebugLog.General.debugln("Reloading scripts = " + String.valueOf(types));
        this.loadScripts(ScriptLoadMode.Reload, types);
    }

    private void loadScripts(ScriptLoadMode loadMode, EnumSet<ScriptType> toLoadTypes) {
        try {
            XuiManager.setParseOnce(true);
            if (loadMode == ScriptLoadMode.Reload) {
                for (ScriptBucketCollection<?> scriptBucketCollection : this.bucketCollectionList) {
                    if (!toLoadTypes.contains((Object)scriptBucketCollection.getScriptType())) continue;
                    scriptBucketCollection.setReloadBuckets(true);
                    scriptBucketCollection.PreReloadScripts();
                }
                for (String string : this.loadFileNames) {
                    String absPath;
                    currentLoadFileAbsPath = absPath = ZomboidFileSystem.instance.getAbsolutePath(string);
                    currentLoadFileName = FileName.getName(absPath);
                    currentLoadFileMod = this.tempFileToModMap.get(absPath);
                    this.LoadFile(ScriptLoadMode.Reload, string, true);
                }
                for (ScriptBucketCollection scriptBucketCollection : this.bucketCollectionList) {
                    scriptBucketCollection.setReloadBuckets(false);
                }
            }
            for (ScriptBucketCollection<?> scriptBucketCollection : this.bucketCollectionList) {
                if (!toLoadTypes.contains((Object)scriptBucketCollection.getScriptType())) continue;
                scriptBucketCollection.LoadScripts(loadMode);
            }
            for (ScriptBucketCollection scriptBucketCollection : this.bucketCollectionList) {
                if (!toLoadTypes.contains((Object)scriptBucketCollection.getScriptType())) continue;
                scriptBucketCollection.PostLoadScripts(loadMode);
            }
            for (ScriptBucketCollection scriptBucketCollection : this.bucketCollectionList) {
                if (!toLoadTypes.contains((Object)scriptBucketCollection.getScriptType())) continue;
                scriptBucketCollection.OnScriptsLoaded(loadMode);
            }
            if (loadMode == ScriptLoadMode.Reload) {
                for (ScriptBucketCollection scriptBucketCollection : this.bucketCollectionList) {
                    if (!toLoadTypes.contains((Object)scriptBucketCollection.getScriptType())) continue;
                    scriptBucketCollection.OnLoadedAfterLua();
                    scriptBucketCollection.OnPostTileDefinitions();
                    scriptBucketCollection.OnPostWorldDictionaryInit();
                }
            }
        }
        catch (Exception e) {
            ExceptionLogger.logException(e);
            this.hasLoadErrors = true;
        }
        XuiManager.setParseOnce(false);
    }

    public void LoadedAfterLua() {
        for (ScriptBucketCollection<?> collection : this.bucketCollectionList) {
            try {
                collection.OnLoadedAfterLua();
            }
            catch (Exception e) {
                e.printStackTrace();
                this.hasLoadErrors = true;
            }
        }
    }

    public void PostTileDefinitions() {
        for (ScriptBucketCollection<?> collection : this.bucketCollectionList) {
            try {
                collection.OnPostTileDefinitions();
            }
            catch (Exception e) {
                ExceptionLogger.logException(e);
                this.hasLoadErrors = true;
            }
        }
    }

    public void PostWorldDictionaryInit() {
        for (ScriptBucketCollection<?> collection : this.bucketCollectionList) {
            try {
                collection.OnPostWorldDictionaryInit();
            }
            catch (Exception e) {
                ExceptionLogger.logException(e);
                this.hasLoadErrors = true;
            }
        }
    }

    public boolean hasLoadErrors() {
        return this.hasLoadErrors(false);
    }

    public boolean hasLoadErrors(boolean onlyCritical) {
        if (this.hasLoadErrors) {
            return true;
        }
        for (ScriptBucketCollection<?> collection : this.bucketCollectionList) {
            if (!collection.hasLoadErrors(onlyCritical)) continue;
            return true;
        }
        return false;
    }

    public static void resolveGetItemTypes(ArrayList<String> sourceItems, ArrayList<Item> scriptItems) {
        for (int i = sourceItems.size() - 1; i >= 0; --i) {
            String type1 = sourceItems.get(i);
            if (!type1.startsWith("[")) continue;
            sourceItems.remove(i);
            String functionName = type1.substring(1, type1.indexOf("]"));
            Object functionObj = LuaManager.getFunctionObject(functionName);
            if (functionObj == null) continue;
            scriptItems.clear();
            LuaManager.caller.protectedCallVoid(LuaManager.thread, functionObj, scriptItems);
            for (int j = 0; j < scriptItems.size(); ++j) {
                Item scriptItem = scriptItems.get(j);
                sourceItems.add(i + j, scriptItem.getFullName());
            }
        }
    }

    private void debugItems() {
        ArrayList<Item> items = instance.getAllItems();
        for (Item item : items) {
            ModelScript modelScript;
            boolean bl;
            if (item.isItemType(ItemType.DRAINABLE) && item.getReplaceOnUse() != null) {
                DebugLog.Script.warn("%s ReplaceOnUse instead of ReplaceOnDeplete", item.getFullName());
            }
            if (item.isItemType(ItemType.WEAPON) && item.hitSound != null && !item.hitSound.equals(item.hitFloorSound)) {
                bl = true;
            }
            if (StringUtils.isNullOrEmpty(item.worldStaticModel)) continue;
            if (item.isItemType(ItemType.FOOD) && item.getStaticModel() == null) {
                bl = true;
            }
            if ((modelScript = this.getModelScript(item.worldStaticModel)) == null || modelScript.getAttachmentById("world") == null) continue;
            boolean bl2 = true;
        }
    }

    public ArrayList<Recipe> getAllRecipesFor(String result) {
        ArrayList<Recipe> rec = this.getAllRecipes();
        ArrayList<Recipe> res = new ArrayList<Recipe>();
        for (int n = 0; n < rec.size(); ++n) {
            String t = rec.get((int)n).result.type;
            if (t.contains(".")) {
                t = t.substring(t.indexOf(".") + 1);
            }
            if (!t.equals(result)) continue;
            res.add(rec.get(n));
        }
        return res;
    }

    public String getItemTypeForClothingItem(String clothingItem) {
        return this.clothingToItemMap.get(clothingItem);
    }

    public Item getItemForClothingItem(String clothingName) {
        String itemType = this.getItemTypeForClothingItem(clothingName);
        if (itemType == null) {
            return null;
        }
        return this.FindItem(itemType);
    }

    private void createZedDmgMap() {
        this.visualDamagesList.clear();
        ScriptModule module = this.getModule(Base);
        for (Item item : module.items.getScriptMap().values()) {
            if (!item.isBodyLocation(ItemBodyLocation.ZED_DMG)) continue;
            this.visualDamagesList.add(item.getName());
        }
    }

    public ArrayList<String> getZedDmgMap() {
        return this.visualDamagesList;
    }

    private void createClothingItemMap() {
        for (Item item : this.getAllItems()) {
            if (StringUtils.isNullOrWhitespace(item.getClothingItem())) continue;
            if (DebugLog.isEnabled(DebugType.Script)) {
                DebugLog.Script.noise("ClothingItem \"%s\" <---> Item \"%s\"", item.getClothingItem(), item.getFullName());
            }
            this.clothingToItemMap.put(item.getClothingItem(), item.getFullName());
        }
    }

    private void resolveItemTypes() {
        for (Item item : this.getAllItems()) {
            item.resolveItemTypes();
        }
    }

    public String resolveItemType(ScriptModule module, String itemType) {
        if (StringUtils.isNullOrWhitespace(itemType)) {
            return null;
        }
        if (itemType.contains(".")) {
            return itemType;
        }
        Item scriptItem = module.getItem(itemType);
        if (scriptItem != null) {
            return scriptItem.getFullName();
        }
        for (int i = 0; i < this.moduleList.size(); ++i) {
            ScriptModule module1 = this.moduleList.get(i);
            if (module1.disabled || (scriptItem = module1.getItem(itemType)) == null) continue;
            return scriptItem.getFullName();
        }
        return "???." + itemType;
    }

    public String resolveModelScript(ScriptModule module, String modelScriptName) {
        if (StringUtils.isNullOrWhitespace(modelScriptName)) {
            return null;
        }
        if (modelScriptName.contains(".")) {
            return modelScriptName;
        }
        ModelScript modelScript = module.getModelScript(modelScriptName);
        if (modelScript != null) {
            return modelScript.getFullType();
        }
        for (int i = 0; i < this.moduleList.size(); ++i) {
            ScriptModule module1 = this.moduleList.get(i);
            if (module1 == module || module1.disabled || (modelScript = module1.getModelScript(modelScriptName)) == null) continue;
            return modelScript.getFullType();
        }
        return "???." + modelScriptName;
    }

    public Item getSpecificItem(String name) {
        if (!name.contains(".")) {
            DebugLog.log("ScriptManager.getSpecificItem requires a full type name, cannot find: " + name);
            if (Core.debug) {
                throw new RuntimeException("ScriptManager.getSpecificItem requires a full type name, cannot find: " + name);
            }
            return null;
        }
        if (this.items.hasFullType(name)) {
            return this.items.getFullType(name);
        }
        int idx = name.indexOf(".");
        String module = name.substring(0, idx);
        String type = name.substring(idx + 1);
        ScriptModule script = this.getModule(module, false);
        if (script == null) {
            return null;
        }
        return script.getItem(type);
    }

    public GameEntityScript getSpecificEntity(String name) {
        if (!name.contains(".")) {
            DebugLog.log("ScriptManager.getSpecificEntity requires a full type name, cannot find: " + name);
            if (Core.debug) {
                throw new RuntimeException("ScriptManager.getSpecificEntity requires a full type name, cannot find: " + name);
            }
            return null;
        }
        if (this.entities.hasFullType(name)) {
            return this.entities.getFullType(name);
        }
        int idx = name.indexOf(".");
        String module = name.substring(0, idx);
        String type = name.substring(idx + 1);
        ScriptModule script = this.getModule(module, false);
        if (script == null) {
            return null;
        }
        return script.getGameEntityScript(type);
    }
}

