/*
 * Decompiled with CFR 0.152.
 */
package zombie.gameStates;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Objects;
import se.krka.kahlua.vm.KahluaTable;
import zombie.Lua.LuaManager;
import zombie.SoundManager;
import zombie.UsedFromLua;
import zombie.ZomboidFileSystem;
import zombie.config.BooleanConfigOption;
import zombie.config.ConfigFile;
import zombie.config.ConfigOption;
import zombie.core.Core;
import zombie.core.skinnedmodel.ModelManager;
import zombie.core.skinnedmodel.animation.AnimationClip;
import zombie.debug.DebugOptions;
import zombie.gameStates.GameState;
import zombie.gameStates.GameStateMachine;
import zombie.input.GameKeyboard;
import zombie.scripting.ScriptManager;
import zombie.scripting.objects.AnimationsMesh;
import zombie.scripting.objects.ModelScript;
import zombie.ui.UIElementInterface;
import zombie.ui.UIManager;
import zombie.vehicles.EditVehicleState;

@UsedFromLua
public final class AnimationViewerState
extends GameState {
    public static AnimationViewerState instance;
    private EditVehicleState.LuaEnvironment luaEnv;
    private boolean exit;
    private final ArrayList<UIElementInterface> gameUi = new ArrayList();
    private final ArrayList<UIElementInterface> selfUi = new ArrayList();
    private boolean suspendUi;
    private KahluaTable table;
    private final ArrayList<String> clipNames = new ArrayList();
    private float ambientVolume;
    private float musicVolume;
    private static final int VERSION = 1;
    private final ArrayList<ConfigOption> options = new ArrayList();
    private final BooleanDebugOption drawGrid = new BooleanDebugOption(this, "DrawGrid", false);
    private final BooleanDebugOption isometric = new BooleanDebugOption(this, "Isometric", false);
    private final BooleanDebugOption showBones = new BooleanDebugOption(this, "ShowBones", false);
    private final BooleanDebugOption useDeferredMovement = new BooleanDebugOption(this, "UseDeferredMovement", false);

    @Override
    public void enter() {
        instance = this;
        this.load();
        if (this.luaEnv == null) {
            this.luaEnv = new EditVehicleState.LuaEnvironment(LuaManager.platform, LuaManager.converterManager, LuaManager.env);
        }
        this.saveGameUI();
        this.saveSoundState();
        if (this.selfUi.isEmpty()) {
            this.luaEnv.caller.pcall(this.luaEnv.thread, this.luaEnv.env.rawget("AnimationViewerState_InitUI"), new Object[0]);
            if (this.table != null && this.table.getMetatable() != null) {
                this.table.getMetatable().rawset("_LUA_RELOADED_CHECK", (Object)Boolean.FALSE);
            }
        } else {
            UIManager.UI.addAll(this.selfUi);
            this.luaEnv.caller.pcall(this.luaEnv.thread, this.table.rawget("showUI"), (Object)this.table);
        }
        this.exit = false;
    }

    @Override
    public void yield() {
        this.restoreGameUI();
        this.restoreSoundState();
    }

    @Override
    public void reenter() {
        this.saveGameUI();
        this.saveSoundState();
    }

    @Override
    public void exit() {
        this.save();
        this.restoreGameUI();
        this.restoreSoundState();
    }

    @Override
    public void render() {
        boolean playerIndex = false;
        Core.getInstance().StartFrame(0, true);
        this.renderScene();
        Core.getInstance().EndFrame(0);
        Core.getInstance().RenderOffScreenBuffer();
        boolean bl = UIManager.useUiFbo = Core.getInstance().supportsFBO() && Core.getInstance().getOptionUIFBO();
        if (Core.getInstance().StartFrameUI()) {
            this.renderUI();
        }
        Core.getInstance().EndFrameUI();
    }

    @Override
    public GameStateMachine.StateAction update() {
        if (this.exit || GameKeyboard.isKeyPressed(65)) {
            return GameStateMachine.StateAction.Continue;
        }
        this.updateScene();
        return GameStateMachine.StateAction.Remain;
    }

    public static AnimationViewerState checkInstance() {
        if (instance != null) {
            if (AnimationViewerState.instance.table == null || AnimationViewerState.instance.table.getMetatable() == null) {
                instance = null;
            } else if (AnimationViewerState.instance.table.getMetatable().rawget("_LUA_RELOADED_CHECK") == null) {
                instance = null;
            }
        }
        if (instance == null) {
            return new AnimationViewerState();
        }
        return instance;
    }

    private void saveGameUI() {
        this.gameUi.clear();
        this.gameUi.addAll(UIManager.UI);
        UIManager.UI.clear();
        this.suspendUi = UIManager.suspend;
        UIManager.suspend = false;
        UIManager.setShowPausedMessage(false);
        UIManager.defaultthread = this.luaEnv.thread;
    }

    private void restoreGameUI() {
        this.selfUi.clear();
        this.selfUi.addAll(UIManager.UI);
        UIManager.UI.clear();
        UIManager.UI.addAll(this.gameUi);
        UIManager.suspend = this.suspendUi;
        UIManager.setShowPausedMessage(true);
        UIManager.defaultthread = LuaManager.thread;
    }

    private void saveSoundState() {
        this.ambientVolume = SoundManager.instance.getAmbientVolume();
        SoundManager.instance.setAmbientVolume(1.0f);
        this.musicVolume = SoundManager.instance.getMusicVolume();
        SoundManager.instance.setMusicVolume(0.0f);
        SoundManager.instance.setMusicState("InGame");
    }

    private void restoreSoundState() {
        SoundManager.instance.setAmbientVolume(this.ambientVolume);
        SoundManager.instance.setMusicVolume(this.musicVolume);
        SoundManager.instance.setMusicState("PauseMenu");
    }

    private void updateScene() {
        ModelManager.instance.update();
        if (GameKeyboard.isKeyPressed(17)) {
            DebugOptions.instance.model.render.wireframe.setValue(!DebugOptions.instance.model.render.wireframe.getValue());
        }
    }

    private void renderScene() {
    }

    private void renderUI() {
        UIManager.render();
    }

    public void setTable(KahluaTable table) {
        this.table = table;
    }

    public Object fromLua0(String func) {
        switch (func) {
            case "exit": {
                this.exit = true;
                return null;
            }
            case "getClipNames": {
                if (this.clipNames.isEmpty()) {
                    Collection<AnimationClip> clips = ModelManager.instance.getAllAnimationClips();
                    for (AnimationClip clip : clips) {
                        this.clipNames.add(clip.name);
                    }
                    this.clipNames.sort(Comparator.naturalOrder());
                }
                return this.clipNames;
            }
        }
        throw new IllegalArgumentException("unhandled \"" + func + "\"");
    }

    public Object fromLua1(String func, Object arg0) {
        switch (func) {
            case "getClipNames": {
                String modelScriptName = (String)arg0;
                ModelScript modelScript = ScriptManager.instance.getModelScript(modelScriptName);
                AnimationsMesh animationsMesh = ScriptManager.instance.getAnimationsMesh(modelScript.animationsMesh);
                HashMap<String, AnimationClip> clips = animationsMesh.modelMesh.skinningData.animationClips;
                if (this.clipNames.isEmpty() || !clips.containsKey(this.clipNames.get(0))) {
                    this.clipNames.clear();
                    for (AnimationClip clip : clips.values()) {
                        this.clipNames.add(clip.name);
                    }
                    this.clipNames.sort(Comparator.naturalOrder());
                }
                return this.clipNames;
            }
        }
        throw new IllegalArgumentException(String.format("unhandled \"%s\" \"%s\"", func, arg0));
    }

    public ConfigOption getOptionByName(String name) {
        for (int i = 0; i < this.options.size(); ++i) {
            ConfigOption setting = this.options.get(i);
            if (!setting.getName().equals(name)) continue;
            return setting;
        }
        return null;
    }

    public int getOptionCount() {
        return this.options.size();
    }

    public ConfigOption getOptionByIndex(int index) {
        return this.options.get(index);
    }

    public void setBoolean(String name, boolean value) {
        ConfigOption setting = this.getOptionByName(name);
        if (setting instanceof BooleanConfigOption) {
            BooleanConfigOption booleanConfigOption = (BooleanConfigOption)setting;
            booleanConfigOption.setValue(value);
        }
    }

    public boolean getBoolean(String name) {
        ConfigOption setting = this.getOptionByName(name);
        if (setting instanceof BooleanConfigOption) {
            BooleanConfigOption booleanConfigOption = (BooleanConfigOption)setting;
            return booleanConfigOption.getValue();
        }
        return false;
    }

    public void save() {
        String fileName = ZomboidFileSystem.instance.getCacheDir() + File.separator + "animationViewerState-options.ini";
        ConfigFile configFile = new ConfigFile();
        configFile.write(fileName, 1, this.options);
    }

    public void load() {
        ConfigFile configFile = new ConfigFile();
        String fileName = ZomboidFileSystem.instance.getCacheDir() + File.separator + "animationViewerState-options.ini";
        if (configFile.read(fileName)) {
            for (int i = 0; i < configFile.getOptions().size(); ++i) {
                ConfigOption configOption = configFile.getOptions().get(i);
                ConfigOption myOption = this.getOptionByName(configOption.getName());
                if (myOption == null) continue;
                myOption.parse(configOption.getValueAsString());
            }
        }
    }

    @UsedFromLua
    public class BooleanDebugOption
    extends BooleanConfigOption {
        public BooleanDebugOption(AnimationViewerState this$0, String name, boolean defaultValue) {
            Objects.requireNonNull(this$0);
            super(name, defaultValue);
            this$0.options.add(this);
        }
    }
}

