/*
 * Decompiled with CFR 0.152.
 */
package zombie.ui;

import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;
import se.krka.kahlua.vm.KahluaTable;
import se.krka.kahlua.vm.KahluaThread;
import zombie.GameProfiler;
import zombie.GameTime;
import zombie.GameWindow;
import zombie.IndieGL;
import zombie.Lua.LuaEventManager;
import zombie.Lua.LuaManager;
import zombie.UsedFromLua;
import zombie.characters.IsoPlayer;
import zombie.core.BoxedStaticValues;
import zombie.core.Core;
import zombie.core.PerformanceSettings;
import zombie.core.SpriteRenderer;
import zombie.core.Styles.TransparentStyle;
import zombie.core.Styles.UIFBOStyle;
import zombie.core.Translator;
import zombie.core.math.PZMath;
import zombie.core.opengl.RenderThread;
import zombie.core.textures.Texture;
import zombie.core.textures.TextureFBO;
import zombie.debug.DebugOptions;
import zombie.gameStates.GameLoadingState;
import zombie.gizmo.Gizmos;
import zombie.input.GameKeyboard;
import zombie.input.Mouse;
import zombie.iso.IsoCamera;
import zombie.iso.IsoObject;
import zombie.iso.IsoObjectPicker;
import zombie.iso.IsoUtils;
import zombie.iso.IsoWorld;
import zombie.iso.Vector2;
import zombie.network.GameClient;
import zombie.network.GameServer;
import zombie.ui.ActionProgressBar;
import zombie.ui.Clock;
import zombie.ui.ModalDialog;
import zombie.ui.MoodlesUI;
import zombie.ui.ObjectTooltip;
import zombie.ui.SpeedControls;
import zombie.ui.TextManager;
import zombie.ui.TutorialManager;
import zombie.ui.UIDebugConsole;
import zombie.ui.UIElement;
import zombie.ui.UIElementInterface;
import zombie.ui.UIFont;
import zombie.ui.UITransition;
import zombie.util.list.PZArrayUtil;

@UsedFromLua
public final class UIManager {
    public static int lastMouseX;
    public static int lastMouseY;
    public static IsoObjectPicker.ClickObject picked;
    public static Clock clock;
    public static final ArrayList<UIElementInterface> UI;
    public static ObjectTooltip toolTip;
    public static Texture mouseArrow;
    public static Texture mouseExamine;
    public static Texture mouseAttack;
    public static Texture mouseGrab;
    public static SpeedControls speedControls;
    public static UIDebugConsole debugConsole;
    public static final MoodlesUI[] MoodleUI;
    public static boolean fadeBeforeUi;
    public static final ActionProgressBar[] ProgressBar;
    public static float fadeAlpha;
    public static int fadeInTimeMax;
    public static int fadeInTime;
    public static boolean fadingOut;
    public static Texture lastMouseTexture;
    public static IsoObject lastPicked;
    public static final ArrayList<String> DoneTutorials;
    public static float lastOffX;
    public static float lastOffY;
    public static ModalDialog modal;
    public static boolean keyDownZoomIn;
    public static boolean keyDownZoomOut;
    public static boolean doTick;
    public static boolean visibleAllUi;
    public static TextureFBO uiFbo;
    public static boolean useUiFbo;
    public static boolean uiTextureContentsValid;
    public static Texture black;
    public static boolean suspend;
    public static float lastAlpha;
    public static final Vector2 PickedTileLocal;
    public static final Vector2 PickedTile;
    public static IsoObject rightDownObject;
    public static long uiUpdateTimeMS;
    public static long uiUpdateIntervalMS;
    public static long uiRenderTimeMS;
    public static long uiRenderIntervalMS;
    private static final ArrayList<UIElementInterface> tutorialStack;
    public static final ArrayList<UIElementInterface> toTop;
    public static KahluaThread defaultthread;
    public static KahluaThread previousThread;
    static final ArrayList<UIElementInterface> toRemove;
    static final ArrayList<UIElementInterface> toAdd;
    static int wheel;
    static int lastwheel;
    static final ArrayList<UIElementInterface> debugUI;
    static boolean showLuaDebuggerOnError;
    public static String luaDebuggerAction;
    static final Sync sync;
    private static boolean showPausedMessage;
    private static UIElementInterface playerInventoryUI;
    private static UIElementInterface playerLootUI;
    private static UIElementInterface playerInventoryTooltip;
    private static UIElementInterface playerLootTooltip;
    private static final FadeInfo[] playerFadeInfo;
    private static final BlinkInfo[] playerBlinkInfo;
    private static boolean rendering;
    private static boolean updating;

    public static void AddUI(UIElementInterface el) {
        toRemove.remove(el);
        toRemove.add(el);
        toAdd.remove(el);
        toAdd.add(el);
    }

    public static void RemoveElement(UIElementInterface el) {
        toAdd.remove(el);
        toRemove.remove(el);
        toRemove.add(el);
    }

    public static void clearArrays() {
        toAdd.clear();
        toRemove.clear();
        UI.clear();
    }

    public static void closeContainers() {
    }

    public static void CloseContainers() {
    }

    public static void DrawTexture(Texture tex, double x, double y) {
        double dx = x;
        double dy = y;
        SpriteRenderer.instance.renderi(tex, (int)(dx += (double)tex.offsetX), (int)(dy += (double)tex.offsetY), tex.getWidth(), tex.getHeight(), 1.0f, 1.0f, 1.0f, 1.0f, null);
    }

    public static void DrawTexture(Texture tex, double x, double y, double width, double height, double alpha) {
        double dx = x;
        double dy = y;
        SpriteRenderer.instance.renderi(tex, (int)(dx += (double)tex.offsetX), (int)(dy += (double)tex.offsetY), (int)width, (int)height, 1.0f, 1.0f, 1.0f, (float)alpha, null);
    }

    public static void FadeIn(double seconds) {
        UIManager.setFadeInTimeMax((int)(seconds * 30.0 * (double)((float)PerformanceSettings.getLockFPS() / 30.0f)));
        UIManager.setFadeInTime(UIManager.getFadeInTimeMax());
        UIManager.setFadingOut(false);
    }

    public static void FadeOut(double seconds) {
        UIManager.setFadeInTimeMax((int)(seconds * 30.0 * (double)((float)PerformanceSettings.getLockFPS() / 30.0f)));
        UIManager.setFadeInTime(UIManager.getFadeInTimeMax());
        UIManager.setFadingOut(true);
    }

    public static void CreateFBO(int width, int height) {
        if (Core.safeMode) {
            useUiFbo = false;
            return;
        }
        if (useUiFbo && (uiFbo == null || uiFbo.getTexture().getWidth() != width || uiFbo.getTexture().getHeight() != height)) {
            if (uiFbo != null) {
                RenderThread.invokeOnRenderContext(() -> uiFbo.destroy());
            }
            try {
                uiFbo = UIManager.createTexture(width, height, false);
            }
            catch (Exception e) {
                useUiFbo = false;
                e.printStackTrace();
            }
        }
    }

    public static TextureFBO createTexture(float x, float y, boolean test) throws Exception {
        if (test) {
            Texture tex = new Texture((int)x, (int)y, 16);
            TextureFBO newOne = new TextureFBO(tex);
            newOne.destroy();
            return null;
        }
        Texture tex = new Texture((int)x, (int)y, 16);
        return new TextureFBO(tex);
    }

    public static void init() {
        int i;
        showPausedMessage = true;
        UIManager.getUI().clear();
        debugUI.clear();
        clock = null;
        for (i = 0; i < 4; ++i) {
            UIManager.MoodleUI[i] = null;
        }
        UIManager.setSpeedControls(new SpeedControls());
        SpeedControls.instance = UIManager.getSpeedControls();
        UIManager.setbFadeBeforeUI(false);
        visibleAllUi = true;
        for (int playerIndex = 0; playerIndex < 4; ++playerIndex) {
            playerFadeInfo[playerIndex].setFadeBeforeUI(false);
            playerFadeInfo[playerIndex].setFadeTime(0);
            playerFadeInfo[playerIndex].setFadingOut(false);
        }
        UIManager.setPicked(null);
        UIManager.setLastPicked(null);
        rightDownObject = null;
        if (IsoPlayer.getInstance() == null) {
            return;
        }
        if (!Core.gameMode.equals("LastStand") && !GameClient.client) {
            UIManager.getUI().add(UIManager.getSpeedControls());
        }
        if (GameServer.server) {
            return;
        }
        UIManager.setToolTip(new ObjectTooltip());
        if (Core.getInstance().getOptionClockSize() == 2) {
            UIManager.setClock(new Clock(Core.getInstance().getOffscreenWidth(0) - 166, 10));
        } else {
            UIManager.setClock(new Clock(Core.getInstance().getOffscreenWidth(0) - 91, 10));
        }
        if (!Core.gameMode.equals("LastStand")) {
            UIManager.getUI().add(UIManager.getClock());
        }
        UIManager.getUI().add(UIManager.getToolTip());
        UIManager.setDebugConsole(new UIDebugConsole(20, Core.getInstance().getScreenHeight() - 265));
        debugConsole.setY((double)Core.getInstance().getScreenHeight() - debugConsole.getHeight() - 20.0);
        if (Core.debug && DebugOptions.instance.uiDebugConsoleStartVisible.getValue()) {
            debugConsole.setVisible(true);
        } else {
            debugConsole.setVisible(false);
        }
        for (i = 0; i < 4; ++i) {
            MoodlesUI ui = new MoodlesUI();
            UIManager.setMoodleUI(i, ui);
            ui.setVisible(true);
            UIManager.getUI().add(ui);
        }
        UIManager.getUI().add(UIManager.getDebugConsole());
        UIManager.setLastMouseTexture(UIManager.getMouseArrow());
        UIManager.resize();
        for (i = 0; i < 4; ++i) {
            ActionProgressBar bar = new ActionProgressBar(0, 0);
            bar.setRenderThisPlayerOnly(i);
            UIManager.setProgressBar(i, bar);
            UIManager.getUI().add(bar);
            bar.setValue(1.0f);
            bar.setVisible(false);
        }
        playerInventoryUI = null;
        playerLootUI = null;
        LuaEventManager.triggerEvent("OnCreateUI");
    }

    public static void render() {
        if (useUiFbo && !Core.getInstance().uiRenderThisFrame) {
            return;
        }
        if (suspend) {
            return;
        }
        long currentTimeMS = System.currentTimeMillis();
        uiRenderIntervalMS = Math.min(currentTimeMS - uiRenderTimeMS, 1000L);
        uiRenderTimeMS = currentTimeMS;
        UIElement.stencilLevel = 0;
        IndieGL.enableBlend();
        if (useUiFbo) {
            SpriteRenderer.instance.setDefaultStyle(UIFBOStyle.instance);
            IndieGL.glBlendFuncSeparate(770, 771, 1, 771);
        } else {
            IndieGL.glBlendFunc(770, 771);
        }
        IndieGL.disableDepthTest();
        GameProfiler profiler = GameProfiler.getInstance();
        try (GameProfiler.ProfileArea profileArea = profiler.profile("UITransition.UpdateAll");){
            UITransition.UpdateAll();
        }
        if (UIManager.getBlack() == null) {
            UIManager.setBlack(Texture.getSharedTexture("black.png"));
        }
        if (LuaManager.thread == defaultthread) {
            LuaEventManager.triggerEvent("OnPreUIDraw");
        }
        if (UIManager.isbFadeBeforeUI()) {
            UIManager.renderFadeOverlay();
        }
        UIManager.setLastAlpha(UIManager.getFadeAlpha().floatValue());
        for (int playerIndex = 0; playerIndex < IsoPlayer.numPlayers; ++playerIndex) {
            if (IsoPlayer.players[playerIndex] == null || !playerFadeInfo[playerIndex].isFadeBeforeUI()) continue;
            playerFadeInfo[playerIndex].render();
        }
        rendering = true;
        for (int i = 0; i < UIManager.getUI().size(); ++i) {
            UIElementInterface element = UIManager.getUI().get(i);
            if (!element.isIgnoreLossControl().booleanValue() && TutorialManager.instance.stealControl || element.isFollowGameWorld().booleanValue()) continue;
            try {
                if (!element.isDefaultDraw().booleanValue()) continue;
                if (GameProfiler.isRunning()) {
                    try (GameProfiler.ProfileArea profileArea = profiler.profile("Render " + String.valueOf(element));){
                        element.render();
                        continue;
                    }
                }
                element.render();
                continue;
            }
            catch (Exception ex) {
                Logger.getLogger(GameWindow.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        rendering = false;
        if (UIManager.getToolTip() != null) {
            UIManager.getToolTip().render();
        }
        if (UIManager.isShowPausedMessage() && GameTime.isGamePaused() && (UIManager.getModal() == null || !modal.isVisible().booleanValue()) && visibleAllUi) {
            String text = Translator.getText("IGUI_GamePaused");
            int boxWidth = TextManager.instance.MeasureStringX(UIFont.Small, text) + 32;
            int fontHeight = TextManager.instance.font.getLineHeight();
            int boxHeight = (int)Math.ceil((double)fontHeight * 1.5);
            SpriteRenderer.instance.renderi(null, Core.getInstance().getScreenWidth() / 2 - boxWidth / 2, Core.getInstance().getScreenHeight() / 6 - boxHeight / 2, boxWidth, boxHeight, 0.0f, 0.0f, 0.0f, 0.75f, null);
            TextManager.instance.DrawStringCentre(Core.getInstance().getScreenWidth() / 2, Core.getInstance().getScreenHeight() / 6 - fontHeight / 2, text, 1.0, 1.0, 1.0, 1.0);
        }
        if (!UIManager.isbFadeBeforeUI()) {
            UIManager.renderFadeOverlay();
        }
        for (int playerIndex = 0; playerIndex < IsoPlayer.numPlayers; ++playerIndex) {
            if (IsoPlayer.players[playerIndex] == null || playerFadeInfo[playerIndex].isFadeBeforeUI()) continue;
            playerFadeInfo[playerIndex].render();
        }
        if (LuaManager.thread == defaultthread) {
            LuaEventManager.triggerEvent("OnPostUIDraw");
        }
        if (useUiFbo) {
            SpriteRenderer.instance.setDefaultStyle(TransparentStyle.instance);
            IndieGL.glBlendFunc(770, 771);
        }
    }

    public static void renderFadeOverlay() {
        UIManager.setFadeAlpha((float)fadeInTime / (float)fadeInTimeMax);
        if (UIManager.isFadingOut().booleanValue()) {
            UIManager.setFadeAlpha(1.0 - UIManager.getFadeAlpha());
        }
        if (IsoCamera.getCameraCharacter() != null && UIManager.getFadeAlpha() > 0.0) {
            UIManager.DrawTexture(UIManager.getBlack(), 0.0, 0.0, Core.getInstance().getScreenWidth(), Core.getInstance().getScreenHeight(), UIManager.getFadeAlpha());
        }
    }

    public static void resize() {
        if (useUiFbo && uiFbo != null) {
            UIManager.CreateFBO(Core.getInstance().getScreenWidth(), Core.getInstance().getScreenHeight());
        }
        if (UIManager.getClock() == null) {
            return;
        }
        UIManager.setLastOffX(Core.getInstance().getScreenWidth());
        UIManager.setLastOffY(Core.getInstance().getScreenHeight());
        for (int i = 0; i < 4; ++i) {
            int sw = Core.getInstance().getScreenWidth();
            int sh = Core.getInstance().getScreenHeight();
            int y = Clock.instance.isVisible() == false ? 24 : 64;
            if (i == 0 && IsoPlayer.numPlayers > 1 || i == 2) {
                sw /= 2;
            }
            MoodleUI[i].setX((float)sw - (10.0f + UIManager.MoodleUI[i].width));
            if ((i == 0 || i == 1) && IsoPlayer.numPlayers > 1) {
                MoodleUI[i].setY(y);
            }
            if (i == 2 || i == 3) {
                MoodleUI[i].setY(sh / 2 + y);
            }
            MoodleUI[i].setVisible(visibleAllUi && IsoPlayer.players[i] != null);
        }
        clock.resize();
        if (IsoPlayer.numPlayers == 1) {
            if (Core.getInstance().getOptionClockSize() == 2) {
                clock.setX(Core.getInstance().getScreenWidth() - 166);
            } else {
                clock.setX(Core.getInstance().getScreenWidth() - 91);
            }
        } else {
            if (Core.getInstance().getOptionClockSize() == 2) {
                clock.setX((float)Core.getInstance().getScreenWidth() / 2.0f - 83.0f);
            } else {
                clock.setX((float)Core.getInstance().getScreenWidth() / 2.0f - 45.5f);
            }
            clock.setY(Core.getInstance().getScreenHeight() - 70);
        }
        if (IsoPlayer.numPlayers == 1) {
            speedControls.setX((float)Core.getInstance().getScreenWidth() - UIManager.speedControls.width - 10.0f);
        } else {
            speedControls.setX((float)(Core.getInstance().getScreenWidth() / 2) - UIManager.speedControls.width - 10.0f);
        }
        if (IsoPlayer.numPlayers == 1 && !clock.isVisible().booleanValue()) {
            speedControls.setY(clock.getY());
        } else {
            speedControls.setY(clock.getY() + clock.getHeight() + 10.0);
        }
        speedControls.setVisible(visibleAllUi && !IsoPlayer.allPlayersDead());
    }

    public static Vector2 getTileFromMouse(double mx, double my, double z) {
        UIManager.PickedTile.x = IsoUtils.XToIso((float)(mx - 0.0), (float)(my - 0.0), (float)z);
        UIManager.PickedTile.y = IsoUtils.YToIso((float)(mx - 0.0), (float)(my - 0.0), (float)z);
        UIManager.PickedTileLocal.x = UIManager.getPickedTile().x - (float)PZMath.fastfloor(UIManager.getPickedTile().x);
        UIManager.PickedTileLocal.y = UIManager.getPickedTile().y - (float)PZMath.fastfloor(UIManager.getPickedTile().y);
        UIManager.PickedTile.x = PZMath.fastfloor(UIManager.getPickedTile().x);
        UIManager.PickedTile.y = PZMath.fastfloor(UIManager.getPickedTile().y);
        return UIManager.getPickedTile();
    }

    private static int isOverElement(UIElementInterface ui, int mx, int my) {
        if (!ui.isIgnoreLossControl().booleanValue() && TutorialManager.instance.stealControl) {
            return -1;
        }
        if (!ui.isVisible().booleanValue()) {
            return -1;
        }
        if (modal != null && modal != ui && modal.isVisible().booleanValue()) {
            return -1;
        }
        if (ui.isCapture().booleanValue()) {
            return 1;
        }
        if (ui.getMaxDrawHeight() != -1.0 ? (double)mx >= ui.getX() && (double)my >= ui.getY() && (double)mx < ui.getX() + ui.getWidth() && (double)my < ui.getY() + Math.min(ui.getHeight(), ui.getMaxDrawHeight()) : ui.isOverElement(mx, my)) {
            return 1;
        }
        return 0;
    }

    public static void update() {
        int consumed;
        Object rem;
        UIElementInterface ui;
        int i;
        if (suspend) {
            return;
        }
        if (!toRemove.isEmpty()) {
            UI.removeAll(toRemove);
        }
        toRemove.clear();
        if (!toAdd.isEmpty()) {
            PZArrayUtil.addAll(UI, toAdd);
        }
        toAdd.clear();
        UIManager.setFadeInTime(UIManager.getFadeInTime() - 1.0);
        for (int playerIndex = 0; playerIndex < IsoPlayer.numPlayers; ++playerIndex) {
            playerFadeInfo[playerIndex].update();
            playerBlinkInfo[playerIndex].update();
        }
        long currentTimeMS = System.currentTimeMillis();
        if (currentTimeMS - uiUpdateTimeMS >= 100L) {
            doTick = true;
            uiUpdateIntervalMS = Math.min(currentTimeMS - uiUpdateTimeMS, 1000L);
            uiUpdateTimeMS = currentTimeMS;
        } else {
            doTick = false;
        }
        int mx = Mouse.getXA();
        int my = Mouse.getYA();
        int mxw = Mouse.getX();
        int myw = Mouse.getY();
        tutorialStack.clear();
        for (i = UI.size() - 1; i >= 0; --i) {
            ui = UI.get(i);
            if (ui.getParent() != null) {
                UI.remove(i);
                throw new IllegalStateException();
            }
            if (ui.isFollowGameWorld().booleanValue()) {
                tutorialStack.add(ui);
            }
            if (!(ui instanceof ObjectTooltip)) continue;
            rem = UI.remove(i);
            UI.add((UIElementInterface)rem);
        }
        for (i = 0; i < UI.size(); ++i) {
            ui = UI.get(i);
            if (!ui.isAlwaysOnTop() && !toTop.contains(ui)) continue;
            rem = UI.remove(i);
            --i;
            toAdd.add((UIElementInterface)rem);
        }
        if (!toAdd.isEmpty()) {
            PZArrayUtil.addAll(UI, toAdd);
            toAdd.clear();
        }
        toTop.clear();
        for (i = 0; i < UI.size(); ++i) {
            ui = UI.get(i);
            if (!ui.isBackMost()) continue;
            rem = UI.remove(i);
            UI.add(0, (UIElementInterface)rem);
        }
        for (i = 0; i < tutorialStack.size(); ++i) {
            UI.remove(tutorialStack.get(i));
            UI.add(0, tutorialStack.get(i));
        }
        updating = true;
        GameProfiler profiler = GameProfiler.getInstance();
        rem = profiler.profile("updateMouseButtons");
        try {
            consumed = UIManager.updateMouseButtons(mx, my);
        }
        finally {
            if (rem != null) {
                ((GameProfiler.ProfileArea)rem).close();
            }
        }
        boolean consumedClick = (consumed & 1) == 1;
        boolean consumedRClick = (consumed & 2) == 2;
        boolean consumedMove = false;
        int attackclick = GameKeyboard.whichKeyPressed("Attack/Click");
        if (attackclick > 0 && UIManager.checkPicked()) {
            if (!(attackclick == 10000 && consumedClick || attackclick == 10001 && consumedRClick)) {
                LuaEventManager.triggerEvent("OnObjectLeftMouseButtonDown", UIManager.picked.tile, BoxedStaticValues.toDouble(mx), BoxedStaticValues.toDouble(my));
            }
            GameKeyboard.whichKeyPressed("Attack/Click");
            if (!(IsoWorld.instance.currentCell == null || IsoWorld.instance.currentCell.DoBuilding(0, false) || UIManager.getPicked() == null || GameTime.isGamePaused() || IsoPlayer.getInstance() == null || IsoPlayer.getInstance().isAiming() || IsoPlayer.getInstance().isAsleep())) {
                UIManager.getPicked().tile.onMouseLeftClick(UIManager.getPicked().lx, UIManager.getPicked().ly);
            }
        }
        if (!((attackclick = GameKeyboard.whichKeyWasDown("Attack/Click")) <= 0 || !UIManager.checkPicked() || attackclick == 10000 && consumedClick || attackclick == 10001 && consumedRClick)) {
            LuaEventManager.triggerEvent("OnObjectLeftMouseButtonUp", UIManager.picked.tile, BoxedStaticValues.toDouble(mx), BoxedStaticValues.toDouble(my));
        }
        lastwheel = 0;
        wheel = Mouse.getWheelState();
        boolean bWheelConsumed = false;
        if (wheel != lastwheel) {
            int del = wheel - lastwheel < 0 ? 1 : -1;
            for (int i2 = UI.size() - 1; i2 >= 0; --i2) {
                UIElementInterface ui2 = UI.get(i2);
                if (!ui2.isPointOver(mx, my).booleanValue() && !ui2.isCapture().booleanValue() || !ui2.onConsumeMouseWheel(del, (double)mx - ui2.getX(), (double)my - ui2.getY()).booleanValue()) continue;
                bWheelConsumed = true;
                break;
            }
            LuaEventManager.triggerEvent("OnMouseWheel", BoxedStaticValues.toDouble(wheel));
            if (!bWheelConsumed) {
                Core.getInstance().doZoomScroll(0, del);
            }
        }
        try (GameProfiler.ProfileArea profileArea = profiler.profile("updateMouseMove");){
            consumedMove = UIManager.updateMouseMove(mx, my, consumedMove);
        }
        if (!consumedMove && IsoPlayer.players[0] != null) {
            UIManager.setPicked(IsoObjectPicker.Instance.ContextPick(mx, my));
            if (IsoCamera.getCameraCharacter() != null) {
                UIManager.setPickedTile(UIManager.getTileFromMouse(mxw, myw, PZMath.fastfloor(IsoPlayer.players[0].getZ())));
            }
            LuaEventManager.triggerEvent("OnMouseMove", BoxedStaticValues.toDouble(mx), BoxedStaticValues.toDouble(my), BoxedStaticValues.toDouble(mxw), BoxedStaticValues.toDouble(myw));
        }
        UIManager.setLastMouseX(mx);
        UIManager.setLastMouseY(my);
        profileArea = profiler.profile("updateUIElements");
        try {
            UIManager.updateUIElements();
        }
        finally {
            if (profileArea != null) {
                profileArea.close();
            }
        }
        profileArea = profiler.profile("updateTooltip");
        try {
            UIManager.updateTooltip(mx, my);
        }
        finally {
            if (profileArea != null) {
                profileArea.close();
            }
        }
        updating = false;
        UIManager.handleZoomKeys();
        IsoCamera.cameras[0].lastOffX = (int)IsoCamera.cameras[0].offX;
        IsoCamera.cameras[0].lastOffY = (int)IsoCamera.cameras[0].offY;
    }

    private static int updateMouseButtons(int mx, int my) {
        int btn;
        boolean consumedClick = false;
        boolean consumedRClick = false;
        for (btn = 0; btn < Mouse.getButtonCount(); ++btn) {
            boolean consumed;
            boolean bl = consumed = btn == 0 && Gizmos.getInstance().hitTest(mx, my);
            if (Mouse.isButtonPressed(btn)) {
                if (btn == 0) {
                    Core.UnfocusActiveTextEntryBox();
                }
                block9: for (i = UI.size() - 1; i >= 0 && !consumed; --i) {
                    ui = UI.get(i);
                    switch (UIManager.isOverElement(ui, mx, my)) {
                        case 0: {
                            ui.onMouseButtonDownOutside(btn, (double)mx - ui.getX(), (double)my - ui.getY());
                            continue block9;
                        }
                        case 1: {
                            if (!ui.onConsumeMouseButtonDown(btn, (double)mx - ui.getX(), (double)my - ui.getY())) continue block9;
                            consumed = true;
                        }
                    }
                }
            } else if (Mouse.isButtonReleased(btn)) {
                block10: for (i = UI.size() - 1; i >= 0 && !consumed; --i) {
                    ui = UI.get(i);
                    switch (UIManager.isOverElement(ui, mx, my)) {
                        case 0: {
                            ui.onMouseButtonUpOutside(btn, (double)mx - ui.getX(), (double)my - ui.getY());
                            continue block10;
                        }
                        case 1: {
                            if (!ui.onConsumeMouseButtonUp(btn, (double)mx - ui.getX(), (double)my - ui.getY())) continue block10;
                            consumed = true;
                        }
                    }
                }
            }
            if (btn == 0) {
                consumedClick = consumed;
                continue;
            }
            if (btn != 1) continue;
            consumedRClick = consumed;
        }
        for (btn = 2; btn < Mouse.getButtonCount(); ++btn) {
            if (Mouse.isButtonPressed(btn)) {
                LuaEventManager.triggerEvent("OnKeyStartPressed", 10000 + btn);
                LuaEventManager.triggerEvent("OnKeyPressed", 10000 + btn);
                continue;
            }
            if (Mouse.isButtonReleased(btn) || !Mouse.isButtonDown(btn)) continue;
            LuaEventManager.triggerEvent("OnKeyKeepPressed", 10000 + btn);
        }
        if (Mouse.isLeftPressed()) {
            if (!consumedClick) {
                LuaEventManager.triggerEvent("OnMouseDown", BoxedStaticValues.toDouble(mx), BoxedStaticValues.toDouble(my));
                LuaEventManager.triggerEvent("OnKeyStartPressed", 10000);
                LuaEventManager.triggerEvent("OnKeyPressed", 10000);
                UIManager.CloseContainers();
            } else {
                Mouse.UIBlockButtonDown(0);
            }
        } else if (Mouse.isLeftReleased()) {
            if (!consumedClick) {
                LuaEventManager.triggerEvent("OnMouseUp", BoxedStaticValues.toDouble(mx), BoxedStaticValues.toDouble(my));
            }
        } else if (Mouse.isLeftDown() && !consumedClick) {
            LuaEventManager.triggerEvent("OnKeyKeepPressed", 10000);
        }
        if (Mouse.isRightPressed()) {
            if (!consumedRClick) {
                LuaEventManager.triggerEvent("OnRightMouseDown", BoxedStaticValues.toDouble(mx), BoxedStaticValues.toDouble(my));
                if (UIManager.checkPicked()) {
                    LuaEventManager.triggerEvent("OnObjectRightMouseButtonDown", UIManager.picked.tile, BoxedStaticValues.toDouble(mx), BoxedStaticValues.toDouble(my));
                }
            } else {
                Mouse.UIBlockButtonDown(1);
            }
            if (!(IsoWorld.instance.currentCell == null || UIManager.getPicked() == null || UIManager.getSpeedControls() == null || IsoPlayer.getInstance().isAiming() || IsoPlayer.getInstance().isAsleep() || GameTime.isGamePaused())) {
                UIManager.getSpeedControls().SetCurrentGameSpeed(1);
                UIManager.getPicked().tile.onMouseRightClick(UIManager.getPicked().lx, UIManager.getPicked().ly);
                UIManager.setRightDownObject(UIManager.getPicked().tile);
            }
        } else if (Mouse.isRightReleased()) {
            i = 0;
            if (!consumedRClick) {
                LuaEventManager.triggerEvent("OnRightMouseUp", BoxedStaticValues.toDouble(mx), BoxedStaticValues.toDouble(my));
                if (UIManager.checkPicked()) {
                    LuaEventManager.triggerEvent("OnObjectRightMouseButtonUp", UIManager.picked.tile, BoxedStaticValues.toDouble(mx), BoxedStaticValues.toDouble(my));
                }
            }
            if (IsoPlayer.getInstance() != null) {
                IsoPlayer.getInstance().setDragObject(null);
            }
            if (IsoWorld.instance.currentCell != null && UIManager.getRightDownObject() != null && IsoPlayer.getInstance() != null && !IsoPlayer.getInstance().isAiming() && !IsoPlayer.getInstance().isAsleep()) {
                UIManager.getRightDownObject().onMouseRightReleased();
                UIManager.setRightDownObject(null);
            }
        } else if (Mouse.isRightDown()) {
            for (i = UI.size() - 1; i >= 0; --i) {
                UIElementInterface ui = UI.get(i);
                if (UIManager.isOverElement(ui, mx, my) != 1) continue;
                consumedRClick = true;
                break;
            }
            if (!consumedRClick) {
                LuaEventManager.triggerEvent("OnKeyKeepPressed", 10001);
            }
        }
        if (!consumedRClick && Mouse.isRightDelay()) {
            LuaEventManager.triggerEvent("OnKeyStartPressed", 10001);
            LuaEventManager.triggerEvent("OnKeyPressed", 10001);
        }
        return (consumedClick ? 1 : 0) + (consumedRClick ? 2 : 0);
    }

    private static boolean updateMouseMove(int mx, int my, boolean consumedMove) {
        if (UIManager.getLastMouseX() != (double)mx || UIManager.getLastMouseY() != (double)my) {
            for (int i = UI.size() - 1; i >= 0; --i) {
                UIElementInterface ui = UI.get(i);
                if (!ui.isIgnoreLossControl().booleanValue() && TutorialManager.instance.stealControl || !ui.isVisible().booleanValue()) continue;
                if (ui.isOverElement(mx, my) || ui.isCapture().booleanValue()) {
                    if (consumedMove || !ui.onConsumeMouseMove((double)mx - UIManager.getLastMouseX(), (double)my - UIManager.getLastMouseY(), (double)mx - ui.getX(), (double)my - ui.getY()).booleanValue()) continue;
                    consumedMove = true;
                    continue;
                }
                ui.onExtendMouseMoveOutside((double)mx - UIManager.getLastMouseX(), (double)my - UIManager.getLastMouseY(), (double)mx - ui.getX(), (double)my - ui.getY());
            }
        }
        return consumedMove;
    }

    private static void updateUIElements() {
        for (int i = 0; i < UI.size(); ++i) {
            UI.get(i).update();
        }
    }

    private static boolean checkPicked() {
        return picked != null && UIManager.picked.tile != null && UIManager.picked.tile.getObjectIndex() != -1;
    }

    private static void handleZoomKeys() {
        boolean allowZoom = true;
        if (Core.currentTextEntryBox != null && Core.currentTextEntryBox.isEditable() && Core.currentTextEntryBox.isDoingTextEntry()) {
            allowZoom = false;
        }
        if (GameTime.isGamePaused()) {
            allowZoom = false;
        }
        if (GameKeyboard.isKeyDown("Zoom in")) {
            if (allowZoom && !keyDownZoomIn) {
                Core.getInstance().doZoomScroll(0, -1);
            }
            keyDownZoomIn = true;
        } else {
            keyDownZoomIn = false;
        }
        if (GameKeyboard.isKeyDown("Zoom out")) {
            if (allowZoom && !keyDownZoomOut) {
                Core.getInstance().doZoomScroll(0, 1);
            }
            keyDownZoomOut = true;
        } else {
            keyDownZoomOut = false;
        }
    }

    public static Double getLastMouseX() {
        return BoxedStaticValues.toDouble(lastMouseX);
    }

    public static void setLastMouseX(double aLastMouseX) {
        lastMouseX = (int)aLastMouseX;
    }

    public static Double getLastMouseY() {
        return BoxedStaticValues.toDouble(lastMouseY);
    }

    public static void setLastMouseY(double aLastMouseY) {
        lastMouseY = (int)aLastMouseY;
    }

    public static IsoObjectPicker.ClickObject getPicked() {
        return picked;
    }

    public static void setPicked(IsoObjectPicker.ClickObject aPicked) {
        picked = aPicked;
    }

    public static Clock getClock() {
        return clock;
    }

    public static void setClock(Clock aClock) {
        clock = aClock;
    }

    public static ArrayList<UIElementInterface> getUI() {
        return UI;
    }

    public static void setUI(ArrayList<UIElementInterface> aUI) {
        PZArrayUtil.copy(UI, aUI);
    }

    public static ObjectTooltip getToolTip() {
        return toolTip;
    }

    public static void setToolTip(ObjectTooltip aToolTip) {
        toolTip = aToolTip;
    }

    public static Texture getMouseArrow() {
        return mouseArrow;
    }

    public static void setMouseArrow(Texture aMouseArrow) {
        mouseArrow = aMouseArrow;
    }

    public static Texture getMouseExamine() {
        return mouseExamine;
    }

    public static void setMouseExamine(Texture aMouseExamine) {
        mouseExamine = aMouseExamine;
    }

    public static Texture getMouseAttack() {
        return mouseAttack;
    }

    public static void setMouseAttack(Texture aMouseAttack) {
        mouseAttack = aMouseAttack;
    }

    public static Texture getMouseGrab() {
        return mouseGrab;
    }

    public static void setMouseGrab(Texture aMouseGrab) {
        mouseGrab = aMouseGrab;
    }

    public static SpeedControls getSpeedControls() {
        return speedControls;
    }

    public static void setSpeedControls(SpeedControls aSpeedControls) {
        speedControls = aSpeedControls;
    }

    public static UIDebugConsole getDebugConsole() {
        return debugConsole;
    }

    public static void setDebugConsole(UIDebugConsole aDebugConsole) {
        debugConsole = aDebugConsole;
    }

    public static MoodlesUI getMoodleUI(double index) {
        return MoodleUI[(int)index];
    }

    public static void setMoodleUI(double index, MoodlesUI aMoodleUI) {
        UIManager.MoodleUI[(int)index] = aMoodleUI;
    }

    public static boolean isbFadeBeforeUI() {
        return fadeBeforeUi;
    }

    public static void setbFadeBeforeUI(boolean abFadeBeforeUI) {
        fadeBeforeUi = abFadeBeforeUI;
    }

    public static ActionProgressBar getProgressBar(double index) {
        return ProgressBar[(int)index];
    }

    public static void setProgressBar(double index, ActionProgressBar aProgressBar) {
        UIManager.ProgressBar[(int)index] = aProgressBar;
    }

    public static Double getFadeAlpha() {
        return BoxedStaticValues.toDouble(fadeAlpha);
    }

    public static void setFadeAlpha(double aFadeAlpha) {
        fadeAlpha = PZMath.clamp((float)aFadeAlpha, 0.0f, 1.0f);
    }

    public static Double getFadeInTimeMax() {
        return BoxedStaticValues.toDouble(fadeInTimeMax);
    }

    public static void setFadeInTimeMax(double aFadeInTimeMax) {
        fadeInTimeMax = (int)aFadeInTimeMax;
    }

    public static Double getFadeInTime() {
        return BoxedStaticValues.toDouble(fadeInTime);
    }

    public static void setFadeInTime(double aFadeInTime) {
        fadeInTime = Math.max((int)aFadeInTime, 0);
    }

    public static Boolean isFadingOut() {
        return fadingOut ? Boolean.TRUE : Boolean.FALSE;
    }

    public static void setFadingOut(boolean aFadingOut) {
        fadingOut = aFadingOut;
    }

    public static Texture getLastMouseTexture() {
        return lastMouseTexture;
    }

    public static void setLastMouseTexture(Texture aLastMouseTexture) {
        lastMouseTexture = aLastMouseTexture;
    }

    public static IsoObject getLastPicked() {
        return lastPicked;
    }

    public static void setLastPicked(IsoObject aLastPicked) {
        lastPicked = aLastPicked;
    }

    public static ArrayList<String> getDoneTutorials() {
        return DoneTutorials;
    }

    public static void setDoneTutorials(ArrayList<String> aDoneTutorials) {
        PZArrayUtil.copy(DoneTutorials, aDoneTutorials);
    }

    public static float getLastOffX() {
        return lastOffX;
    }

    public static void setLastOffX(float aLastOffX) {
        lastOffX = aLastOffX;
    }

    public static float getLastOffY() {
        return lastOffY;
    }

    public static void setLastOffY(float aLastOffY) {
        lastOffY = aLastOffY;
    }

    public static ModalDialog getModal() {
        return modal;
    }

    public static void setModal(ModalDialog aModal) {
        modal = aModal;
    }

    public static Texture getBlack() {
        return black;
    }

    public static void setBlack(Texture aBlack) {
        black = aBlack;
    }

    public static float getLastAlpha() {
        return lastAlpha;
    }

    public static void setLastAlpha(float aLastAlpha) {
        lastAlpha = aLastAlpha;
    }

    public static Vector2 getPickedTileLocal() {
        return PickedTileLocal;
    }

    public static void setPickedTileLocal(Vector2 aPickedTileLocal) {
        PickedTileLocal.set(aPickedTileLocal);
    }

    public static Vector2 getPickedTile() {
        return PickedTile;
    }

    public static void setPickedTile(Vector2 aPickedTile) {
        PickedTile.set(aPickedTile);
    }

    public static IsoObject getRightDownObject() {
        return rightDownObject;
    }

    public static void setRightDownObject(IsoObject aRightDownObject) {
        rightDownObject = aRightDownObject;
    }

    static void pushToTop(UIElementInterface aThis) {
        toTop.add(aThis);
    }

    public static boolean isShowPausedMessage() {
        return showPausedMessage;
    }

    public static void setShowPausedMessage(boolean showPausedMessage) {
        UIManager.showPausedMessage = showPausedMessage;
    }

    public static void setShowLuaDebuggerOnError(boolean show) {
        showLuaDebuggerOnError = show;
    }

    public static boolean isShowLuaDebuggerOnError() {
        return showLuaDebuggerOnError;
    }

    public static void debugBreakpoint(String filename, long pc) {
        if (!showLuaDebuggerOnError) {
            return;
        }
        if (Core.currentTextEntryBox != null) {
            Core.currentTextEntryBox.setDoingTextEntry(false);
            Core.currentTextEntryBox = null;
        }
        if (GameServer.server) {
            return;
        }
        if (GameWindow.states.current instanceof GameLoadingState) {
            return;
        }
        previousThread = defaultthread;
        defaultthread = LuaManager.debugthread;
        int frameStage = Core.getInstance().frameStage;
        if (frameStage != 0) {
            if (frameStage <= 1) {
                Core.getInstance().EndFrame(0);
            }
            if (frameStage <= 2) {
                Core.getInstance().StartFrameUI();
            }
            if (frameStage <= 3) {
                Core.getInstance().EndFrameUI();
            }
        }
        LuaManager.thread.step = false;
        LuaManager.thread.stepInto = false;
        if (!toRemove.isEmpty()) {
            UI.removeAll(toRemove);
        }
        toRemove.clear();
        if (!toAdd.isEmpty()) {
            UI.addAll(toAdd);
        }
        toAdd.clear();
        boolean bOldSuspend = suspend;
        ArrayList<UIElementInterface> oldUI = new ArrayList<UIElementInterface>(UI);
        UI.clear();
        suspend = false;
        UIManager.setShowPausedMessage(false);
        boolean bFinished = false;
        boolean[] bDebounce = new boolean[11];
        boolean bEscKeyDown = false;
        for (int n = 0; n < 11; ++n) {
            bDebounce[n] = true;
        }
        if (debugUI.isEmpty()) {
            LuaManager.debugcaller.pcall(LuaManager.debugthread, LuaManager.env.rawget("DoLuaDebugger"), filename, pc);
        } else {
            UI.addAll(debugUI);
            LuaManager.debugcaller.pcall(LuaManager.debugthread, LuaManager.env.rawget("DoLuaDebuggerOnBreak"), filename, pc);
        }
        Mouse.setCursorVisible(true);
        sync.begin();
        while (true) {
            if (RenderThread.isCloseRequested()) {
                System.exit(0);
            }
            if (GameKeyboard.isKeyDown(1)) {
                bEscKeyDown = true;
            }
            if (!GameWindow.luaDebuggerKeyDown && (GameKeyboard.isKeyDown("Toggle Lua Debugger") || bEscKeyDown && !GameKeyboard.isKeyDown(1))) {
                GameWindow.luaDebuggerKeyDown = true;
                UIManager.executeGame(oldUI, bOldSuspend, frameStage);
                return;
            }
            String action = luaDebuggerAction;
            luaDebuggerAction = null;
            if ("StepInto".equalsIgnoreCase(action)) {
                LuaManager.thread.step = true;
                LuaManager.thread.stepInto = true;
                UIManager.executeGame(oldUI, bOldSuspend, frameStage);
                return;
            }
            if ("StepOver".equalsIgnoreCase(action)) {
                LuaManager.thread.step = true;
                LuaManager.thread.stepInto = false;
                LuaManager.thread.lastCallFrameIdx = LuaManager.thread.getCurrentCoroutine().getCallframeTop();
                UIManager.executeGame(oldUI, bOldSuspend, frameStage);
                return;
            }
            if ("Resume".equalsIgnoreCase(action)) {
                UIManager.executeGame(oldUI, bOldSuspend, frameStage);
                return;
            }
            sync.startFrame();
            for (int n = 0; n < 11; ++n) {
                boolean bPressed = GameKeyboard.isKeyDown(59 + n);
                if (bPressed) {
                    if (!bDebounce[n]) {
                        if (n + 1 == 5) {
                            LuaManager.thread.step = true;
                            LuaManager.thread.stepInto = true;
                            UIManager.executeGame(oldUI, bOldSuspend, frameStage);
                            return;
                        }
                        if (n + 1 == 6) {
                            LuaManager.thread.step = true;
                            LuaManager.thread.stepInto = false;
                            LuaManager.thread.lastCallFrameIdx = LuaManager.thread.getCurrentCoroutine().getCallframeTop();
                            UIManager.executeGame(oldUI, bOldSuspend, frameStage);
                            return;
                        }
                    }
                    bDebounce[n] = true;
                    continue;
                }
                bDebounce[n] = false;
            }
            Mouse.update();
            GameKeyboard.update();
            Core.getInstance().DoFrameReady();
            UIManager.update();
            Core.getInstance().StartFrame(0, true);
            Core.getInstance().EndFrame(0);
            Core.getInstance().RenderOffScreenBuffer();
            if (Core.getInstance().StartFrameUI()) {
                UIManager.render();
            }
            Core.getInstance().EndFrameUI();
            UIManager.resize();
            if (!GameKeyboard.isKeyDown("Toggle Lua Debugger")) {
                GameWindow.luaDebuggerKeyDown = false;
            }
            sync.endFrame();
            if (Core.isUseGameViewport()) continue;
            Core.getInstance().setScreenSize(RenderThread.getDisplayWidth(), RenderThread.getDisplayHeight());
        }
    }

    private static void executeGame(ArrayList<UIElementInterface> oldUI, boolean bOldSuspend, int frameStage) {
        debugUI.clear();
        debugUI.addAll(UI);
        UI.clear();
        UI.addAll(oldUI);
        suspend = bOldSuspend;
        UIManager.setShowPausedMessage(true);
        if (!LuaManager.thread.step && frameStage != 0) {
            if (frameStage == 1) {
                Core.getInstance().StartFrame(0, true);
            }
            if (frameStage == 2) {
                Core.getInstance().StartFrame(0, true);
                Core.getInstance().EndFrame(0);
            }
            if (frameStage == 3) {
                Core.getInstance().StartFrame(0, true);
                Core.getInstance().EndFrame(0);
                Core.getInstance().StartFrameUI();
            }
        }
        defaultthread = previousThread;
    }

    public static KahluaThread getDefaultThread() {
        if (defaultthread == null) {
            defaultthread = LuaManager.thread;
        }
        return defaultthread;
    }

    public static Double getDoubleClickInterval() {
        return BoxedStaticValues.toDouble(500.0);
    }

    public static Double getDoubleClickDist() {
        return BoxedStaticValues.toDouble(5.0);
    }

    public static Boolean isDoubleClick(double x1, double y1, double x2, double y2, double clickTime) {
        if (Math.abs(x2 - x1) > UIManager.getDoubleClickDist()) {
            return false;
        }
        if (Math.abs(y2 - y1) > UIManager.getDoubleClickDist()) {
            return false;
        }
        if ((double)System.currentTimeMillis() - clickTime > UIManager.getDoubleClickInterval()) {
            return Boolean.FALSE;
        }
        return Boolean.TRUE;
    }

    protected static void updateTooltip(double mx, double my) {
        UIElementInterface mouseOverUI = null;
        for (int i = UIManager.getUI().size() - 1; i >= 0; --i) {
            UIElementInterface ui = UIManager.getUI().get(i);
            if (ui == toolTip || !ui.isVisible().booleanValue() || !ui.isOverElement(mx, my) || ui.getMaxDrawHeight() != -1.0 && !(my < ui.getY() + ui.getMaxDrawHeight())) continue;
            mouseOverUI = ui;
            break;
        }
        IsoObject mouseOverObject = null;
        if (mouseOverUI == null && UIManager.getPicked() != null && (mouseOverObject = UIManager.getPicked().tile) != UIManager.getLastPicked() && toolTip != null) {
            UIManager.toolTip.targetAlpha = 0.0f;
            if (mouseOverObject.haveSpecialTooltip()) {
                if (UIManager.getToolTip().object != mouseOverObject) {
                    UIManager.getToolTip().show(mouseOverObject, (double)((int)mx + 24), (double)((int)my + 24));
                    if (toolTip.isVisible().booleanValue()) {
                        UIManager.toolTip.showDelay = 0;
                    }
                } else {
                    UIManager.toolTip.targetAlpha = 1.0f;
                }
            }
        }
        UIManager.setLastPicked(mouseOverObject);
        if (toolTip != null && (mouseOverObject == null || UIManager.toolTip.alpha <= 0.0f && UIManager.toolTip.targetAlpha <= 0.0f)) {
            toolTip.hide();
        }
        if (toolTip != null && toolTip.isVisible().booleanValue()) {
            toolTip.setX(Mouse.getXA() + 24);
            toolTip.setY(Mouse.getYA() + 24);
        }
    }

    public static void setPlayerInventory(int playerIndex, UIElementInterface inventory, UIElementInterface loot) {
        if (playerIndex != 0) {
            return;
        }
        playerInventoryUI = inventory;
        playerLootUI = loot;
    }

    public static void setPlayerInventoryTooltip(int playerIndex, UIElementInterface inventory, UIElementInterface loot) {
        if (playerIndex != 0) {
            return;
        }
        playerInventoryTooltip = inventory;
        playerLootTooltip = loot;
    }

    public static boolean isMouseOverInventory() {
        if (playerInventoryTooltip != null && playerInventoryTooltip.isMouseOver().booleanValue()) {
            return true;
        }
        if (playerLootTooltip != null && playerLootTooltip.isMouseOver().booleanValue()) {
            return true;
        }
        if (playerInventoryUI == null || playerLootUI == null) {
            return false;
        }
        if (playerInventoryUI.getMaxDrawHeight() == -1.0 && playerInventoryUI.isMouseOver().booleanValue()) {
            return true;
        }
        return playerLootUI.getMaxDrawHeight() == -1.0 && playerLootUI.isMouseOver() != false;
    }

    public static void updateBeforeFadeOut() {
        if (!toRemove.isEmpty()) {
            UI.removeAll(toRemove);
            toRemove.clear();
        }
        if (!toAdd.isEmpty()) {
            UI.addAll(toAdd);
            toAdd.clear();
        }
    }

    public static void setVisibleAllUI(boolean visible) {
        visibleAllUi = visible;
    }

    public static void setFadeBeforeUI(int playerIndex, boolean bFadeBeforeUI) {
        playerFadeInfo[playerIndex].setFadeBeforeUI(bFadeBeforeUI);
    }

    public static float getFadeAlpha(double playerIndex) {
        return playerFadeInfo[(int)playerIndex].getFadeAlpha();
    }

    public static void setFadeTime(double playerIndex, double fadeTime) {
        playerFadeInfo[(int)playerIndex].setFadeTime((int)fadeTime);
    }

    public static void FadeIn(double playerIndex, double seconds) {
        playerFadeInfo[(int)playerIndex].FadeIn((int)seconds);
    }

    public static void FadeOut(double playerIndex, double seconds) {
        playerFadeInfo[(int)playerIndex].FadeOut((int)seconds);
    }

    public static boolean isFBOActive() {
        return useUiFbo;
    }

    public static double getMillisSinceLastUpdate() {
        return uiUpdateIntervalMS;
    }

    public static double getSecondsSinceLastUpdate() {
        return (double)uiUpdateIntervalMS / 1000.0;
    }

    public static double getMillisSinceLastRender() {
        return uiRenderIntervalMS;
    }

    public static double getSecondsSinceLastRender() {
        return (double)uiRenderIntervalMS / 1000.0;
    }

    public static boolean onKeyPress(int key) {
        for (int i = UI.size() - 1; i >= 0; --i) {
            UIElementInterface ui = UI.get(i);
            if (!ui.isVisible().booleanValue() || !ui.isWantKeyEvents() || !ui.onConsumeKeyPress(key)) continue;
            return true;
        }
        return false;
    }

    public static boolean onKeyRepeat(int key) {
        for (int i = UI.size() - 1; i >= 0; --i) {
            UIElementInterface ui = UI.get(i);
            if (!ui.isVisible().booleanValue() || !ui.isWantKeyEvents() || !ui.onConsumeKeyRepeat(key)) continue;
            return true;
        }
        return false;
    }

    public static boolean onKeyRelease(int key) {
        for (int i = UI.size() - 1; i >= 0; --i) {
            UIElementInterface ui = UI.get(i);
            if (!ui.isVisible().booleanValue() || !ui.isWantKeyEvents() || !ui.onConsumeKeyRelease(key)) continue;
            return true;
        }
        return false;
    }

    public static boolean isForceCursorVisible() {
        for (int i = UI.size() - 1; i >= 0; --i) {
            UIElementInterface ui = UI.get(i);
            if (!ui.isVisible().booleanValue() || !ui.isForceCursorVisible() && !ui.isMouseOver().booleanValue()) continue;
            return true;
        }
        return false;
    }

    public static Object tableget(KahluaTable table, Object key) {
        if (table != null) {
            return UIManager.getDefaultThread().tableget(table, key);
        }
        return null;
    }

    public static float getBlinkAlpha(int playerIndex) {
        if (playerIndex >= 0 && playerIndex < playerBlinkInfo.length) {
            return UIManager.playerBlinkInfo[playerIndex].alpha;
        }
        return 1.0f;
    }

    public static int getSyncedIconIndex(int playerIndex, int maxIndex) {
        return UIManager.playerBlinkInfo[0].syncedIconIndex % maxIndex;
    }

    public static int resetSyncedIconIndex(int playerIndex) {
        UIManager.playerBlinkInfo[0].syncedIconIndex = 0;
        return 0;
    }

    public static boolean isRendering() {
        return rendering;
    }

    public static boolean isUpdating() {
        return updating;
    }

    public static boolean isModalVisible() {
        for (int i = 0; i < UIManager.getUI().size(); ++i) {
            UIElementInterface ui = UIManager.getUI().get(i);
            if (!ui.isModalVisible()) continue;
            return true;
        }
        return false;
    }

    static {
        UI = new ArrayList();
        MoodleUI = new MoodlesUI[4];
        ProgressBar = new ActionProgressBar[4];
        fadeAlpha = 1.0f;
        fadeInTimeMax = 180;
        fadeInTime = 180;
        DoneTutorials = new ArrayList();
        visibleAllUi = true;
        lastAlpha = 10000.0f;
        PickedTileLocal = new Vector2();
        PickedTile = new Vector2();
        tutorialStack = new ArrayList();
        toTop = new ArrayList();
        toRemove = new ArrayList();
        toAdd = new ArrayList();
        debugUI = new ArrayList();
        sync = new Sync();
        showPausedMessage = true;
        playerFadeInfo = new FadeInfo[4];
        playerBlinkInfo = new BlinkInfo[4];
        for (int playerIndex = 0; playerIndex < 4; ++playerIndex) {
            UIManager.playerFadeInfo[playerIndex] = new FadeInfo(playerIndex);
            UIManager.playerBlinkInfo[playerIndex] = new BlinkInfo();
        }
    }

    private static class FadeInfo {
        public int playerIndex;
        public boolean fadeBeforeUi;
        public float fadeAlpha;
        public int fadeTime = 2;
        public int fadeTimeMax = 2;
        public boolean fadingOut;

        public FadeInfo(int playerIndex) {
            this.playerIndex = playerIndex;
        }

        public boolean isFadeBeforeUI() {
            return this.fadeBeforeUi;
        }

        public void setFadeBeforeUI(boolean bFadeBeforeUI) {
            this.fadeBeforeUi = bFadeBeforeUI;
        }

        public float getFadeAlpha() {
            return this.fadeAlpha;
        }

        public void setFadeAlpha(float fadeAlpha) {
            this.fadeAlpha = fadeAlpha;
        }

        public int getFadeTime() {
            return this.fadeTime;
        }

        public void setFadeTime(int fadeTime) {
            this.fadeTime = fadeTime;
        }

        public int getFadeTimeMax() {
            return this.fadeTimeMax;
        }

        public void setFadeTimeMax(int fadeTimeMax) {
            this.fadeTimeMax = fadeTimeMax;
        }

        public boolean isFadingOut() {
            return this.fadingOut;
        }

        public void setFadingOut(boolean fadingOut) {
            this.fadingOut = fadingOut;
        }

        public void FadeIn(int seconds) {
            this.setFadeTimeMax((int)((float)(seconds * 30) * ((float)PerformanceSettings.getLockFPS() / 30.0f)));
            this.setFadeTime(this.getFadeTimeMax());
            this.setFadingOut(false);
        }

        public void FadeOut(int seconds) {
            this.setFadeTimeMax((int)((float)(seconds * 30) * ((float)PerformanceSettings.getLockFPS() / 30.0f)));
            this.setFadeTime(this.getFadeTimeMax());
            this.setFadingOut(true);
        }

        public void update() {
            this.setFadeTime(this.getFadeTime() - 1);
        }

        public void render() {
            this.setFadeAlpha((float)this.getFadeTime() / (float)this.getFadeTimeMax());
            if (this.getFadeAlpha() > 1.0f) {
                this.setFadeAlpha(1.0f);
            }
            if (this.getFadeAlpha() < 0.0f) {
                this.setFadeAlpha(0.0f);
            }
            if (this.isFadingOut()) {
                this.setFadeAlpha(1.0f - this.getFadeAlpha());
            }
            if (this.getFadeAlpha() <= 0.0f) {
                return;
            }
            int x = IsoCamera.getScreenLeft(this.playerIndex);
            int y = IsoCamera.getScreenTop(this.playerIndex);
            int w = IsoCamera.getScreenWidth(this.playerIndex);
            int h = IsoCamera.getScreenHeight(this.playerIndex);
            UIManager.DrawTexture(UIManager.getBlack(), x, y, w, h, this.getFadeAlpha());
        }
    }

    private static class BlinkInfo {
        private float alpha = 1.0f;
        private final float delta = 0.015f;
        private float direction = -1.0f;
        private int syncedIconIndex;
        private float syncedIconIndexTimer;

        private BlinkInfo() {
        }

        private void update() {
            if (this.alpha >= 1.0f) {
                this.alpha = 1.0f;
                this.direction = -1.0f;
            } else if (this.alpha <= 0.0f) {
                this.alpha = 0.0f;
                this.direction = 1.0f;
            }
            this.alpha += 0.015f * ((float)PerformanceSettings.getLockFPS() / 30.0f) * this.direction;
            this.syncedIconIndexTimer += GameTime.instance.getRealworldSecondsSinceLastUpdate();
            if (this.syncedIconIndexTimer > 1.5f) {
                this.syncedIconIndexTimer = 0.0f;
                ++this.syncedIconIndex;
                if (this.syncedIconIndex > Short.MAX_VALUE) {
                    this.syncedIconIndex = 0;
                }
            }
        }

        private void reset() {
            this.alpha = 1.0f;
            this.direction = -1.0f;
        }
    }

    static class Sync {
        private final int fps = 30;
        private final long period = 33333333L;
        private long excess;
        private long beforeTime = System.nanoTime();
        private long overSleepTime;

        Sync() {
        }

        void begin() {
            this.beforeTime = System.nanoTime();
            this.overSleepTime = 0L;
        }

        void startFrame() {
            this.excess = 0L;
        }

        void endFrame() {
            long afterTime = System.nanoTime();
            long timeDiff = afterTime - this.beforeTime;
            long sleepTime = 33333333L - timeDiff - this.overSleepTime;
            if (sleepTime > 0L) {
                try {
                    Thread.sleep(sleepTime / 1000000L);
                }
                catch (InterruptedException interruptedException) {
                    // empty catch block
                }
                this.overSleepTime = System.nanoTime() - afterTime - sleepTime;
            } else {
                this.excess -= sleepTime;
                this.overSleepTime = 0L;
            }
            this.beforeTime = System.nanoTime();
        }
    }
}

