/*
 * Decompiled with CFR 0.152.
 */
package zombie.scripting.objects;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Objects;
import java.util.TreeMap;
import zombie.GameSounds;
import zombie.UsedFromLua;
import zombie.core.math.PZMath;
import zombie.core.skinnedmodel.runtime.RuntimeAnimationScript;
import zombie.debug.DebugLog;
import zombie.iso.SpriteModel;
import zombie.scripting.IScriptObjectStore;
import zombie.scripting.ScriptBucket;
import zombie.scripting.ScriptLoadMode;
import zombie.scripting.ScriptManager;
import zombie.scripting.ScriptParser;
import zombie.scripting.ScriptType;
import zombie.scripting.entity.GameEntityScript;
import zombie.scripting.entity.GameEntityTemplate;
import zombie.scripting.entity.components.crafting.CraftRecipe;
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
import zombie.scripting.objects.ItemFilterScript;
import zombie.scripting.objects.MannequinScript;
import zombie.scripting.objects.ModelScript;
import zombie.scripting.objects.PhysicsHitReactionScript;
import zombie.scripting.objects.PhysicsShapeScript;
import zombie.scripting.objects.RagdollScript;
import zombie.scripting.objects.Recipe;
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
import zombie.scripting.ui.XuiScriptType;
import zombie.vehicles.VehicleEngineRPM;

@UsedFromLua
public final class ScriptModule
implements IScriptObjectStore {
    public String name;
    public String value;
    public final ArrayList<String> imports = new ArrayList();
    public boolean disabled;
    private final ArrayList<ScriptBucket<?>> scriptBucketList = new ArrayList();
    private final HashMap<ScriptType, ScriptBucket<?>> scriptBucketMap = new HashMap();
    public final ScriptBucket.Template<VehicleTemplate> vehicleTemplates = this.addBucket(new ScriptBucket.Template<VehicleTemplate>(this, this, ScriptType.VehicleTemplate){
        {
            Objects.requireNonNull(this$0);
            super(module, scriptType);
        }

        @Override
        public VehicleTemplate createInstance(ScriptModule module, String name, String token) {
            return new VehicleTemplate(module, name, token);
        }

        @Override
        protected VehicleTemplate getFromManager(String name) {
            return ScriptManager.instance.getVehicleTemplate(name);
        }

        @Override
        protected VehicleTemplate getFromModule(String name, ScriptModule module) {
            return module != null ? module.getVehicleTemplate(name) : null;
        }
    });
    public final ScriptBucket.Template<GameEntityTemplate> entityTemplates = this.addBucket(new ScriptBucket.Template<GameEntityTemplate>(this, this, ScriptType.EntityTemplate){
        {
            Objects.requireNonNull(this$0);
            super(module, scriptType);
        }

        @Override
        public GameEntityTemplate createInstance(ScriptModule module, String name, String token) {
            return new GameEntityTemplate(module, name, token);
        }

        @Override
        protected GameEntityTemplate getFromManager(String name) {
            return ScriptManager.instance.getGameEntityTemplate(name);
        }

        @Override
        protected GameEntityTemplate getFromModule(String name, ScriptModule module) {
            return module != null ? module.getGameEntityTemplate(name) : null;
        }
    });
    public final ScriptBucket<Item> items = this.addBucket(new ScriptBucket<Item>(this, this, ScriptType.Item){
        {
            Objects.requireNonNull(this$0);
            super(module, scriptType);
        }

        @Override
        public Item createInstance(ScriptModule module, String name, String token) {
            return new Item();
        }

        @Override
        protected Item getFromManager(String name) {
            return ScriptManager.instance.getItem(name);
        }

        @Override
        protected Item getFromModule(String name, ScriptModule module) {
            return module != null ? module.getItem(name) : null;
        }
    });
    public final ScriptBucket<Recipe> recipes = this.addBucket(new ScriptBucket<Recipe>(this, this, ScriptType.Recipe){
        {
            Objects.requireNonNull(this$0);
            super(module, scriptType);
        }

        @Override
        public Recipe createInstance(ScriptModule module, String name, String token) {
            return new Recipe();
        }

        @Override
        protected Recipe getFromManager(String name) {
            return ScriptManager.instance.getRecipe(name);
        }

        @Override
        protected Recipe getFromModule(String name, ScriptModule module) {
            return module != null ? module.getRecipe(name) : null;
        }
    });
    public final ScriptBucket<UniqueRecipe> uniqueRecipes = this.addBucket(new ScriptBucket<UniqueRecipe>(this, this, ScriptType.UniqueRecipe){
        {
            Objects.requireNonNull(this$0);
            super(module, scriptType);
        }

        @Override
        public UniqueRecipe createInstance(ScriptModule module, String name, String token) {
            return new UniqueRecipe(name);
        }

        @Override
        protected UniqueRecipe getFromManager(String name) {
            return ScriptManager.instance.getUniqueRecipe(name);
        }

        @Override
        protected UniqueRecipe getFromModule(String name, ScriptModule module) {
            return module != null ? module.getUniqueRecipe(name) : null;
        }
    });
    public final ScriptBucket<EvolvedRecipe> evolvedRecipes = this.addBucket(new ScriptBucket<EvolvedRecipe>(this, this, ScriptType.EvolvedRecipe){
        {
            Objects.requireNonNull(this$0);
            super(module, scriptType);
        }

        @Override
        public EvolvedRecipe createInstance(ScriptModule module, String name, String token) {
            return new EvolvedRecipe(name);
        }

        @Override
        protected EvolvedRecipe getFromManager(String name) {
            return ScriptManager.instance.getEvolvedRecipe(name);
        }

        @Override
        protected EvolvedRecipe getFromModule(String name, ScriptModule module) {
            return module != null ? module.getEvolvedRecipe(name) : null;
        }
    });
    public final ScriptBucket<Fixing> fixings = this.addBucket(new ScriptBucket<Fixing>(this, this, ScriptType.Fixing){
        {
            Objects.requireNonNull(this$0);
            super(module, scriptType);
        }

        @Override
        public Fixing createInstance(ScriptModule module, String name, String token) {
            return new Fixing();
        }

        @Override
        protected Fixing getFromManager(String name) {
            return ScriptManager.instance.getFixing(name);
        }

        @Override
        protected Fixing getFromModule(String name, ScriptModule module) {
            return module != null ? module.getFixing(name) : null;
        }
    });
    public final ScriptBucket<AnimationsMesh> animationMeshes = this.addBucket(new ScriptBucket<AnimationsMesh>(this, this, ScriptType.AnimationMesh){
        {
            Objects.requireNonNull(this$0);
            super(module, scriptType);
        }

        @Override
        public AnimationsMesh createInstance(ScriptModule module, String name, String token) {
            return new AnimationsMesh();
        }

        @Override
        protected AnimationsMesh getFromManager(String name) {
            return ScriptManager.instance.getAnimationsMesh(name);
        }

        @Override
        protected AnimationsMesh getFromModule(String name, ScriptModule module) {
            return module != null ? module.getAnimationsMesh(name) : null;
        }
    });
    public final ScriptBucket<ClockScript> clocks = this.addBucket(new ScriptBucket<ClockScript>(this, this, ScriptType.Clock){
        {
            Objects.requireNonNull(this$0);
            super(module, scriptType);
        }

        @Override
        public ClockScript createInstance(ScriptModule module, String name, String token) {
            return new ClockScript();
        }

        @Override
        protected ClockScript getFromManager(String name) {
            return ScriptManager.instance.getClockScript(name);
        }

        @Override
        protected ClockScript getFromModule(String name, ScriptModule module) {
            return module != null ? module.getClockScript(name) : null;
        }
    });
    public final ScriptBucket<MannequinScript> mannequins = this.addBucket(new ScriptBucket<MannequinScript>(this, this, ScriptType.Mannequin){
        {
            Objects.requireNonNull(this$0);
            super(module, scriptType);
        }

        @Override
        public MannequinScript createInstance(ScriptModule module, String name, String token) {
            return new MannequinScript();
        }

        @Override
        protected MannequinScript getFromManager(String name) {
            return ScriptManager.instance.getMannequinScript(name);
        }

        @Override
        protected MannequinScript getFromModule(String name, ScriptModule module) {
            return module != null ? module.getMannequinScript(name) : null;
        }
    });
    public final ScriptBucket<ModelScript> models = this.addBucket(new ScriptBucket<ModelScript>(this, this, ScriptType.Model){
        {
            Objects.requireNonNull(this$0);
            super(module, scriptType);
        }

        @Override
        public ModelScript createInstance(ScriptModule module, String name, String token) {
            return new ModelScript();
        }

        @Override
        protected ModelScript getFromManager(String name) {
            return ScriptManager.instance.getModelScript(name);
        }

        @Override
        protected ModelScript getFromModule(String name, ScriptModule module) {
            return module != null ? module.getModelScript(name) : null;
        }
    });
    public final ScriptBucket<PhysicsShapeScript> physicsShapes = this.addBucket(new ScriptBucket<PhysicsShapeScript>(this, this, ScriptType.PhysicsShape){
        {
            Objects.requireNonNull(this$0);
            super(module, scriptType);
        }

        @Override
        public PhysicsShapeScript createInstance(ScriptModule module, String name, String token) {
            return new PhysicsShapeScript();
        }

        @Override
        protected PhysicsShapeScript getFromManager(String name) {
            return ScriptManager.instance.getPhysicsShape(name);
        }

        @Override
        protected PhysicsShapeScript getFromModule(String name, ScriptModule module) {
            return module != null ? module.getPhysicsShape(name) : null;
        }
    });
    public final ScriptBucket<SpriteModel> spriteModels = this.addBucket(new ScriptBucket<SpriteModel>(this, this, ScriptType.SpriteModel){
        {
            Objects.requireNonNull(this$0);
            super(module, scriptType);
        }

        @Override
        public SpriteModel createInstance(ScriptModule module, String name, String token) {
            return new SpriteModel();
        }

        @Override
        protected SpriteModel getFromManager(String name) {
            return ScriptManager.instance.getSpriteModel(name);
        }

        @Override
        protected SpriteModel getFromModule(String name, ScriptModule module) {
            return module != null ? module.getSpriteModel(name) : null;
        }
    });
    public final ScriptBucket<GameSoundScript> gameSounds = this.addBucket(new ScriptBucket<GameSoundScript>(this, this, ScriptType.Sound){
        {
            Objects.requireNonNull(this$0);
            super(module, scriptType);
        }

        @Override
        public GameSoundScript createInstance(ScriptModule module, String name, String token) {
            return new GameSoundScript();
        }

        @Override
        protected GameSoundScript getFromManager(String name) {
            return ScriptManager.instance.getGameSound(name);
        }

        @Override
        protected GameSoundScript getFromModule(String name, ScriptModule module) {
            return module != null ? module.getGameSound(name) : null;
        }

        @Override
        protected void onScriptLoad(ScriptLoadMode loadMode, GameSoundScript script) {
            if (loadMode == ScriptLoadMode.Reload) {
                GameSounds.OnReloadSound(script);
            }
        }
    });
    public final ScriptBucket<SoundTimelineScript> soundTimelines = this.addBucket(new ScriptBucket<SoundTimelineScript>(this, this, ScriptType.SoundTimeline){
        {
            Objects.requireNonNull(this$0);
            super(module, scriptType);
        }

        @Override
        public SoundTimelineScript createInstance(ScriptModule module, String name, String token) {
            return new SoundTimelineScript();
        }

        @Override
        protected SoundTimelineScript getFromManager(String name) {
            return ScriptManager.instance.getSoundTimeline(name);
        }

        @Override
        protected SoundTimelineScript getFromModule(String name, ScriptModule module) {
            return module != null ? module.getSoundTimeline(name) : null;
        }
    });
    public final ScriptBucket<VehicleScript> vehicles = this.addBucket(new ScriptBucket<VehicleScript>(this, this, ScriptType.Vehicle){
        {
            Objects.requireNonNull(this$0);
            super(module, scriptType);
        }

        @Override
        public VehicleScript createInstance(ScriptModule module, String name, String token) {
            return new VehicleScript();
        }

        @Override
        protected VehicleScript getFromManager(String name) {
            return ScriptManager.instance.getVehicle(name);
        }

        @Override
        protected VehicleScript getFromModule(String name, ScriptModule module) {
            return module != null ? module.getVehicle(name) : null;
        }

        @Override
        protected void onScriptLoad(ScriptLoadMode loadMode, VehicleScript script) {
            script.Loaded();
        }
    });
    public final ScriptBucket<RuntimeAnimationScript> animations = this.addBucket(new ScriptBucket<RuntimeAnimationScript>(this, this, ScriptType.RuntimeAnimation, new TreeMap(String.CASE_INSENSITIVE_ORDER)){
        {
            Objects.requireNonNull(this$0);
            super(module, scriptType, customMap);
        }

        @Override
        public RuntimeAnimationScript createInstance(ScriptModule module, String name, String token) {
            return new RuntimeAnimationScript();
        }

        @Override
        protected RuntimeAnimationScript getFromManager(String name) {
            return ScriptManager.instance.getRuntimeAnimationScript(name);
        }

        @Override
        protected RuntimeAnimationScript getFromModule(String name, ScriptModule module) {
            return module != null ? module.getAnimation(name) : null;
        }
    });
    public final ScriptBucket<VehicleEngineRPM> vehicleEngineRpms = this.addBucket(new ScriptBucket<VehicleEngineRPM>(this, this, ScriptType.VehicleEngineRPM){
        {
            Objects.requireNonNull(this$0);
            super(module, scriptType);
        }

        @Override
        public VehicleEngineRPM createInstance(ScriptModule module, String name, String token) {
            return new VehicleEngineRPM();
        }

        @Override
        protected VehicleEngineRPM getFromManager(String name) {
            return ScriptManager.instance.getVehicleEngineRPM(name);
        }

        @Override
        protected VehicleEngineRPM getFromModule(String name, ScriptModule module) {
            return module != null ? module.getVehicleEngineRPM(name) : null;
        }
    });
    public final ScriptBucket<ItemConfig> itemConfigs = this.addBucket(new ScriptBucket<ItemConfig>(this, this, ScriptType.ItemConfig){
        {
            Objects.requireNonNull(this$0);
            super(module, scriptType);
        }

        @Override
        public ItemConfig createInstance(ScriptModule module, String name, String token) {
            return new ItemConfig();
        }

        @Override
        protected ItemConfig getFromManager(String name) {
            return ScriptManager.instance.getItemConfig(name);
        }

        @Override
        protected ItemConfig getFromModule(String name, ScriptModule module) {
            return module != null ? module.getItemConfig(name) : null;
        }
    });
    public final ScriptBucket<GameEntityScript> entities = this.addBucket(new ScriptBucket<GameEntityScript>(this, this, ScriptType.Entity){
        {
            Objects.requireNonNull(this$0);
            super(module, scriptType);
        }

        @Override
        public GameEntityScript createInstance(ScriptModule module, String name, String token) {
            return new GameEntityScript();
        }

        @Override
        protected GameEntityScript getFromManager(String name) {
            return ScriptManager.instance.getGameEntityScript(name);
        }

        @Override
        protected GameEntityScript getFromModule(String name, ScriptModule module) {
            return module != null ? module.getGameEntityScript(name) : null;
        }
    });
    public final ScriptBucket<XuiConfigScript> xuiConfigScripts = this.addBucket(new ScriptBucket<XuiConfigScript>(this, this, ScriptType.XuiConfig){
        {
            Objects.requireNonNull(this$0);
            super(module, scriptType);
        }

        @Override
        public XuiConfigScript createInstance(ScriptModule module, String name, String token) {
            return new XuiConfigScript();
        }

        @Override
        protected XuiConfigScript getFromManager(String name) {
            return ScriptManager.instance.getXuiConfigScript(name);
        }

        @Override
        protected XuiConfigScript getFromModule(String name, ScriptModule module) {
            return module != null ? module.getXuiConfigScript(name) : null;
        }
    });
    public final ScriptBucket<XuiLayoutScript> xuiLayouts = this.addBucket(new ScriptBucket<XuiLayoutScript>(this, this, ScriptType.XuiLayout){
        {
            Objects.requireNonNull(this$0);
            super(module, scriptType);
        }

        @Override
        public XuiLayoutScript createInstance(ScriptModule module, String name, String token) {
            return new XuiLayoutScript(this.getScriptType(), XuiScriptType.Layout);
        }

        @Override
        protected XuiLayoutScript getFromManager(String name) {
            return ScriptManager.instance.getXuiLayout(name);
        }

        @Override
        protected XuiLayoutScript getFromModule(String name, ScriptModule module) {
            return module != null ? module.getXuiLayout(name) : null;
        }
    });
    public final ScriptBucket<XuiLayoutScript> xuiStyles = this.addBucket(new ScriptBucket<XuiLayoutScript>(this, this, ScriptType.XuiStyle){
        {
            Objects.requireNonNull(this$0);
            super(module, scriptType);
        }

        @Override
        public XuiLayoutScript createInstance(ScriptModule module, String name, String token) {
            return new XuiLayoutScript(this.getScriptType(), XuiScriptType.Style);
        }

        @Override
        protected XuiLayoutScript getFromManager(String name) {
            return ScriptManager.instance.getXuiStyle(name);
        }

        @Override
        protected XuiLayoutScript getFromModule(String name, ScriptModule module) {
            return module != null ? module.getXuiStyle(name) : null;
        }
    });
    public final ScriptBucket<XuiLayoutScript> xuiDefaultStyles = this.addBucket(new ScriptBucket<XuiLayoutScript>(this, this, ScriptType.XuiDefaultStyle){
        {
            Objects.requireNonNull(this$0);
            super(module, scriptType);
        }

        @Override
        public XuiLayoutScript createInstance(ScriptModule module, String name, String token) {
            return new XuiLayoutScript(this.getScriptType(), XuiScriptType.DefaultStyle);
        }

        @Override
        protected XuiLayoutScript getFromManager(String name) {
            return ScriptManager.instance.getXuiDefaultStyle(name);
        }

        @Override
        protected XuiLayoutScript getFromModule(String name, ScriptModule module) {
            return module != null ? module.getXuiDefaultStyle(name) : null;
        }
    });
    public final ScriptBucket<XuiColorsScript> xuiGlobalColors = this.addBucket(new ScriptBucket<XuiColorsScript>(this, this, ScriptType.XuiColor){
        {
            Objects.requireNonNull(this$0);
            super(module, scriptType);
        }

        @Override
        public XuiColorsScript createInstance(ScriptModule module, String name, String token) {
            return new XuiColorsScript();
        }

        @Override
        protected XuiColorsScript getFromManager(String name) {
            return ScriptManager.instance.getXuiColor(name);
        }

        @Override
        protected XuiColorsScript getFromModule(String name, ScriptModule module) {
            return module != null ? module.getXuiGlobalColors(name) : null;
        }
    });
    public final ScriptBucket<XuiSkinScript> xuiSkinScripts = this.addBucket(new ScriptBucket<XuiSkinScript>(this, this, ScriptType.XuiSkin){
        {
            Objects.requireNonNull(this$0);
            super(module, scriptType);
        }

        @Override
        public XuiSkinScript createInstance(ScriptModule module, String name, String token) {
            return new XuiSkinScript();
        }

        @Override
        protected XuiSkinScript getFromManager(String name) {
            return ScriptManager.instance.getXuiSkinScript(name);
        }

        @Override
        protected XuiSkinScript getFromModule(String name, ScriptModule module) {
            return module != null ? module.getXuiSkinScript(name) : null;
        }
    });
    public final ScriptBucket<ItemFilterScript> itemFilters = this.addBucket(new ScriptBucket<ItemFilterScript>(this, this, ScriptType.ItemFilter){
        {
            Objects.requireNonNull(this$0);
            super(module, scriptType);
        }

        @Override
        public ItemFilterScript createInstance(ScriptModule module, String name, String token) {
            return new ItemFilterScript();
        }

        @Override
        protected ItemFilterScript getFromManager(String name) {
            return ScriptManager.instance.getItemFilter(name);
        }

        @Override
        protected ItemFilterScript getFromModule(String name, ScriptModule module) {
            return module != null ? module.getItemFilter(name) : null;
        }
    });
    public final ScriptBucket<FluidFilterScript> fluidFilters = this.addBucket(new ScriptBucket<FluidFilterScript>(this, this, ScriptType.FluidFilter){
        {
            Objects.requireNonNull(this$0);
            super(module, scriptType);
        }

        @Override
        public FluidFilterScript createInstance(ScriptModule module, String name, String token) {
            return new FluidFilterScript();
        }

        @Override
        protected FluidFilterScript getFromManager(String name) {
            return ScriptManager.instance.getFluidFilter(name);
        }

        @Override
        protected FluidFilterScript getFromModule(String name, ScriptModule module) {
            return module != null ? module.getFluidFilter(name) : null;
        }
    });
    public final ScriptBucket<CraftRecipe> craftRecipes = this.addBucket(new ScriptBucket<CraftRecipe>(this, this, ScriptType.CraftRecipe){
        {
            Objects.requireNonNull(this$0);
            super(module, scriptType);
        }

        @Override
        public CraftRecipe createInstance(ScriptModule module, String name, String token) {
            return new CraftRecipe();
        }

        @Override
        protected CraftRecipe getFromManager(String name) {
            return ScriptManager.instance.getCraftRecipe(name);
        }

        @Override
        protected CraftRecipe getFromModule(String name, ScriptModule module) {
            return module != null ? module.getCraftRecipe(name) : null;
        }
    });
    public final ScriptBucket<StringListScript> stringLists = this.addBucket(new ScriptBucket<StringListScript>(this, this, ScriptType.StringList){
        {
            Objects.requireNonNull(this$0);
            super(module, scriptType);
        }

        @Override
        public StringListScript createInstance(ScriptModule module, String name, String token) {
            return new StringListScript();
        }

        @Override
        protected StringListScript getFromManager(String name) {
            return ScriptManager.instance.getStringList(name);
        }

        @Override
        protected StringListScript getFromModule(String name, ScriptModule module) {
            return module != null ? module.getStringList(name) : null;
        }
    });
    public final ScriptBucket<EnergyDefinitionScript> energyDefinitionScripts = this.addBucket(new ScriptBucket<EnergyDefinitionScript>(this, this, ScriptType.EnergyDefinition){
        {
            Objects.requireNonNull(this$0);
            super(module, scriptType);
        }

        @Override
        public EnergyDefinitionScript createInstance(ScriptModule module, String name, String token) {
            return new EnergyDefinitionScript();
        }

        @Override
        protected EnergyDefinitionScript getFromManager(String name) {
            return ScriptManager.instance.getEnergyDefinitionScript(name);
        }

        @Override
        protected EnergyDefinitionScript getFromModule(String name, ScriptModule module) {
            return module != null ? module.getEnergyDefinitionScript(name) : null;
        }
    });
    public final ScriptBucket<FluidDefinitionScript> fluidDefinitionScripts = this.addBucket(new ScriptBucket<FluidDefinitionScript>(this, this, ScriptType.FluidDefinition){
        {
            Objects.requireNonNull(this$0);
            super(module, scriptType);
        }

        @Override
        public FluidDefinitionScript createInstance(ScriptModule module, String name, String token) {
            return new FluidDefinitionScript();
        }

        @Override
        protected FluidDefinitionScript getFromManager(String name) {
            return ScriptManager.instance.getFluidDefinitionScript(name);
        }

        @Override
        protected FluidDefinitionScript getFromModule(String name, ScriptModule module) {
            return module != null ? module.getFluidDefinitionScript(name) : null;
        }
    });
    public final ScriptBucket<TimedActionScript> timedActionScripts = this.addBucket(new ScriptBucket<TimedActionScript>(this, this, ScriptType.TimedAction){
        {
            Objects.requireNonNull(this$0);
            super(module, scriptType);
        }

        @Override
        public TimedActionScript createInstance(ScriptModule module, String name, String token) {
            return new TimedActionScript();
        }

        @Override
        protected TimedActionScript getFromManager(String name) {
            return ScriptManager.instance.getTimedActionScript(name);
        }

        @Override
        protected TimedActionScript getFromModule(String name, ScriptModule module) {
            return module != null ? module.getTimedActionScript(name) : null;
        }
    });
    public final ScriptBucket<RagdollScript> ragdollScripts = this.addBucket(new ScriptBucket<RagdollScript>(this, this, ScriptType.Ragdoll){
        {
            Objects.requireNonNull(this$0);
            super(module, scriptType);
        }

        @Override
        public RagdollScript createInstance(ScriptModule module, String name, String token) {
            return new RagdollScript();
        }

        @Override
        protected RagdollScript getFromManager(String name) {
            return ScriptManager.instance.getRagdollScript(name);
        }

        @Override
        protected RagdollScript getFromModule(String name, ScriptModule module) {
            return module != null ? module.getRagdollScript(name) : null;
        }
    });
    public final ScriptBucket<CharacterTraitDefinitionScript> characterTraitScripts = this.addBucket(new ScriptBucket<CharacterTraitDefinitionScript>(this, this, ScriptType.CharacterTraitDefinition){
        {
            Objects.requireNonNull(this$0);
            super(module, scriptType);
        }

        @Override
        public CharacterTraitDefinitionScript createInstance(ScriptModule module, String name, String token) {
            return new CharacterTraitDefinitionScript();
        }

        @Override
        protected CharacterTraitDefinitionScript getFromManager(String name) {
            return ScriptManager.instance.getCharacterTraitScript(name);
        }

        @Override
        protected CharacterTraitDefinitionScript getFromModule(String name, ScriptModule module) {
            return module != null ? module.getCharacterTraitScript(name) : null;
        }
    });
    public final ScriptBucket<CharacterProfessionDefinitionScript> characterProfessionScripts = this.addBucket(new ScriptBucket<CharacterProfessionDefinitionScript>(this, this, ScriptType.CharacterProfessionDefinition){
        {
            Objects.requireNonNull(this$0);
            super(module, scriptType);
        }

        @Override
        public CharacterProfessionDefinitionScript createInstance(ScriptModule module, String name, String token) {
            return new CharacterProfessionDefinitionScript();
        }

        @Override
        protected CharacterProfessionDefinitionScript getFromManager(String name) {
            return ScriptManager.instance.getCharacterProfessionScript(name);
        }

        @Override
        protected CharacterProfessionDefinitionScript getFromModule(String name, ScriptModule module) {
            return module != null ? module.getCharacterProfessionScript(name) : null;
        }
    });
    public final ScriptBucket<PhysicsHitReactionScript> physicsHitReactionScripts = this.addBucket(new ScriptBucket<PhysicsHitReactionScript>(this, this, ScriptType.PhysicsHitReaction){
        {
            Objects.requireNonNull(this$0);
            super(module, scriptType);
        }

        @Override
        public PhysicsHitReactionScript createInstance(ScriptModule module, String name, String token) {
            return new PhysicsHitReactionScript();
        }

        @Override
        protected PhysicsHitReactionScript getFromManager(String name) {
            return ScriptManager.instance.getPhysicsHitReactionScript(name);
        }

        @Override
        protected PhysicsHitReactionScript getFromModule(String name, ScriptModule module) {
            return module != null ? module.getPhysicsHitReactionScript(name) : null;
        }
    });

    private <T extends BaseScriptObject> ScriptBucket<T> addBucket(ScriptBucket<T> bucket) {
        if (this.scriptBucketMap.containsKey((Object)bucket.getScriptType())) {
            throw new RuntimeException("ScriptType bucket already added.");
        }
        this.scriptBucketMap.put(bucket.getScriptType(), bucket);
        this.scriptBucketList.add(bucket);
        return bucket;
    }

    private <T extends BaseScriptObject> ScriptBucket.Template<T> addBucket(ScriptBucket.Template<T> bucket) {
        if (this.scriptBucketMap.containsKey((Object)bucket.getScriptType())) {
            throw new RuntimeException("ScriptType bucket already added.");
        }
        this.scriptBucketMap.put(bucket.getScriptType(), bucket);
        this.scriptBucketList.add(bucket);
        return bucket;
    }

    public VehicleTemplate getVehicleTemplate(String name) {
        return (VehicleTemplate)this.vehicleTemplates.get(name);
    }

    public GameEntityTemplate getGameEntityTemplate(String name) {
        return (GameEntityTemplate)this.entityTemplates.get(name);
    }

    @Override
    public Item getItem(String name) {
        return this.items.get(name);
    }

    @Override
    public Recipe getRecipe(String name) {
        return this.recipes.get(name);
    }

    public UniqueRecipe getUniqueRecipe(String name) {
        return this.uniqueRecipes.get(name);
    }

    public EvolvedRecipe getEvolvedRecipe(String name) {
        return this.evolvedRecipes.get(name);
    }

    public Fixing getFixing(String name) {
        return this.fixings.get(name);
    }

    public AnimationsMesh getAnimationsMesh(String name) {
        return this.animationMeshes.get(name);
    }

    public ClockScript getClockScript(String name) {
        return this.clocks.get(name);
    }

    public MannequinScript getMannequinScript(String name) {
        return this.mannequins.get(name);
    }

    public ModelScript getModelScript(String name) {
        return this.models.get(name);
    }

    public PhysicsShapeScript getPhysicsShape(String name) {
        return this.physicsShapes.get(name);
    }

    public SpriteModel getSpriteModel(String name) {
        return this.spriteModels.get(name);
    }

    public GameSoundScript getGameSound(String name) {
        return this.gameSounds.get(name);
    }

    public SoundTimelineScript getSoundTimeline(String name) {
        return this.soundTimelines.get(name);
    }

    public VehicleScript getVehicle(String name) {
        return this.vehicles.get(name);
    }

    public RuntimeAnimationScript getAnimation(String name) {
        return this.animations.get(name);
    }

    public VehicleEngineRPM getVehicleEngineRPM(String name) {
        return this.vehicleEngineRpms.get(name);
    }

    public ItemConfig getItemConfig(String name) {
        return this.itemConfigs.get(name);
    }

    public GameEntityScript getGameEntityScript(String name) {
        return this.entities.get(name);
    }

    public XuiConfigScript getXuiConfigScript(String name) {
        return this.xuiConfigScripts.get(name);
    }

    public XuiLayoutScript getXuiLayout(String name) {
        return this.xuiLayouts.get(name);
    }

    public XuiLayoutScript getXuiStyle(String name) {
        return this.xuiStyles.get(name);
    }

    public XuiLayoutScript getXuiDefaultStyle(String name) {
        return this.xuiDefaultStyles.get(name);
    }

    public XuiColorsScript getXuiGlobalColors(String name) {
        return this.xuiGlobalColors.get(name);
    }

    public XuiSkinScript getXuiSkinScript(String name) {
        return this.xuiSkinScripts.get(name);
    }

    public ItemFilterScript getItemFilter(String name) {
        return this.itemFilters.get(name);
    }

    public FluidFilterScript getFluidFilter(String name) {
        return this.fluidFilters.get(name);
    }

    public CraftRecipe getCraftRecipe(String name) {
        return this.craftRecipes.get(name);
    }

    public StringListScript getStringList(String name) {
        return this.stringLists.get(name);
    }

    public EnergyDefinitionScript getEnergyDefinitionScript(String name) {
        return this.energyDefinitionScripts.get(name);
    }

    public FluidDefinitionScript getFluidDefinitionScript(String name) {
        return this.fluidDefinitionScripts.get(name);
    }

    public TimedActionScript getTimedActionScript(String name) {
        return this.timedActionScripts.get(name);
    }

    public RagdollScript getRagdollScript(String name) {
        return this.ragdollScripts.get(name);
    }

    public CharacterTraitDefinitionScript getCharacterTraitScript(String name) {
        return this.characterTraitScripts.get(name);
    }

    public CharacterProfessionDefinitionScript getCharacterProfessionScript(String name) {
        return this.characterProfessionScripts.get(name);
    }

    public PhysicsHitReactionScript getPhysicsHitReactionScript(String name) {
        return this.physicsHitReactionScripts.get(name);
    }

    public ScriptModule() {
        this.xuiLayouts.setVerbose(false);
    }

    public void Load(ScriptLoadMode loadMode, String name, String strArray) {
        this.name = name;
        this.value = strArray.trim();
        ScriptManager.instance.currentLoadingModule = this;
        this.ParseScriptPP(loadMode, this.value);
        this.value = "";
    }

    public void ParseScriptPP(ScriptLoadMode loadMode, String totalFile) {
        ArrayList<String> tokens = ScriptParser.parseTokens(totalFile);
        for (int n = 0; n < tokens.size(); ++n) {
            String token = tokens.get(n);
            this.CreateFromTokenPP(loadMode, token);
        }
    }

    private String GetTokenType(String token) {
        int p = token.indexOf(123);
        if (p == -1) {
            return null;
        }
        String header = token.substring(0, p).trim();
        int p1 = header.indexOf(32);
        int p2 = header.indexOf(9);
        if (p1 != -1 && p2 != -1) {
            return header.substring(0, PZMath.min(p1, p2));
        }
        if (p1 != -1) {
            return header.substring(0, p1);
        }
        if (p2 != -1) {
            return header.substring(0, p2);
        }
        return header;
    }

    private void CreateFromTokenPP(ScriptLoadMode loadMode, String token) {
        String type = this.GetTokenType(token = token.trim());
        if (type == null) {
            return;
        }
        if ("imports".equals(type)) {
            String[] waypoint = token.split("[{}]");
            String[] coords = waypoint[1].split(",");
            for (int n = 0; n < coords.length; ++n) {
                if (coords[n].trim().isEmpty()) continue;
                String module = coords[n].trim();
                if (module.equals(this.getName())) {
                    DebugLog.Script.error("ERROR: module \"" + this.getName() + "\" imports itself");
                    continue;
                }
                this.imports.add(module);
            }
        } else {
            boolean found = false;
            for (ScriptBucket<?> scriptBucket : this.scriptBucketList) {
                if (!scriptBucket.CreateFromTokenPP(loadMode, type, token)) continue;
                found = true;
                break;
            }
            if (!found) {
                DebugLog.Script.warn("unknown script object \"%s\" in '%s'", type, ScriptManager.instance.currentFileName);
            }
        }
    }

    public boolean CheckExitPoints() {
        return false;
    }

    public String getName() {
        return this.name;
    }

    public void Reset() {
        this.imports.clear();
        for (ScriptBucket<?> scriptBucket : this.scriptBucketList) {
            scriptBucket.reset();
        }
    }
}

