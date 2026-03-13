/*
 * Decompiled with CFR 0.152.
 */
package zombie.gameStates;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import se.krka.kahlua.vm.KahluaTable;
import zombie.Lua.LuaManager;
import zombie.UsedFromLua;
import zombie.ZomboidFileSystem;
import zombie.core.Core;
import zombie.core.logger.ExceptionLogger;
import zombie.core.skinnedmodel.ModelManager;
import zombie.core.skinnedmodel.animation.AnimationClip;
import zombie.debug.DebugLog;
import zombie.debug.DebugOptions;
import zombie.gameStates.GameState;
import zombie.gameStates.GameStateMachine;
import zombie.input.GameKeyboard;
import zombie.scripting.ScriptManager;
import zombie.scripting.ScriptParser;
import zombie.scripting.objects.ModelAttachment;
import zombie.scripting.objects.ModelScript;
import zombie.ui.UIElementInterface;
import zombie.ui.UIManager;
import zombie.vehicles.EditVehicleState;

@UsedFromLua
public final class AttachmentEditorState
extends GameState {
    public static AttachmentEditorState instance;
    private static final String INDENT = "    ";
    private EditVehicleState.LuaEnvironment luaEnv;
    private boolean exit;
    private final ArrayList<UIElementInterface> gameUi = new ArrayList();
    private final ArrayList<UIElementInterface> selfUi = new ArrayList();
    private boolean suspendUi;
    private KahluaTable table;
    private final ArrayList<String> clipNames = new ArrayList();

    @Override
    public void enter() {
        instance = this;
        if (this.luaEnv == null) {
            this.luaEnv = new EditVehicleState.LuaEnvironment(LuaManager.platform, LuaManager.converterManager, LuaManager.env);
        }
        this.saveGameUI();
        if (this.selfUi.isEmpty()) {
            this.luaEnv.caller.pcall(this.luaEnv.thread, this.luaEnv.env.rawget("AttachmentEditorState_InitUI"), new Object[0]);
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
    }

    @Override
    public void reenter() {
        this.saveGameUI();
    }

    @Override
    public void exit() {
        this.restoreGameUI();
    }

    @Override
    public void render() {
        boolean playerIndex = false;
        Core.getInstance().StartFrame(0, true);
        this.renderScene();
        Core.getInstance().EndFrame(0);
        Core.getInstance().RenderOffScreenBuffer();
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

    public static AttachmentEditorState checkInstance() {
        if (instance != null) {
            if (AttachmentEditorState.instance.table == null || AttachmentEditorState.instance.table.getMetatable() == null) {
                instance = null;
            } else if (AttachmentEditorState.instance.table.getMetatable().rawget("_LUA_RELOADED_CHECK") == null) {
                instance = null;
            }
        }
        if (instance == null) {
            return new AttachmentEditorState();
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
            case "exit": {
                this.exit = true;
                return null;
            }
        }
        throw new IllegalArgumentException("unhandled \"" + func + "\"");
    }

    public Object fromLua1(String func, Object arg0) {
        switch (func) {
            case "writeScript": {
                String fileName;
                ModelScript modelScript = ScriptManager.instance.getModelScript((String)arg0);
                if (modelScript == null) {
                    throw new NullPointerException("model script \"" + String.valueOf(arg0) + "\" not found");
                }
                ArrayList<String> tokens = AttachmentEditorState.readScript(modelScript.getFileName());
                try {
                    AttachmentEditorState.readScriptNew(modelScript);
                }
                catch (Exception e) {
                    e.printStackTrace();
                }
                if (tokens != null && AttachmentEditorState.updateScript(fileName = modelScript.getFileName(), tokens, modelScript)) {
                    String absolutePath = ZomboidFileSystem.instance.getString(fileName);
                    this.luaEnv.caller.pcall(this.luaEnv.thread, this.table.rawget("wroteScript"), this.table, absolutePath);
                }
                return null;
            }
        }
        throw new IllegalArgumentException(String.format("unhandled \"%s\" \"%s\"", func, arg0));
    }

    private static String formatFloat(float value) {
        if (!Float.isInfinite(value) && (double)value == Math.floor(value)) {
            return String.format("%.1ff", Float.valueOf(value));
        }
        String valueStr = String.format(Locale.US, "%.4f", Float.valueOf(value));
        return "%sf".formatted(new BigDecimal(valueStr).stripTrailingZeros().toPlainString());
    }

    public static void readScriptNew(ModelScript script) throws IOException {
        Path path = Path.of("../src/generation/ModelScriptGenerator.java", new String[0]);
        List<String> lines = Files.readAllLines(path);
        StringBuilder sb = new StringBuilder();
        StringBuilder modifiedSb = new StringBuilder();
        boolean inModel = false;
        for (int i = 0; i < lines.size(); ++i) {
            String originalLine = lines.get(i);
            if (!originalLine.trim().startsWith(".add(ModelBuilder.withId(ModelKey")) continue;
            String type = originalLine.trim().replaceFirst("\\.add\\(ModelBuilder\\.withId\\(ModelKey\\.", "").replaceFirst("\\)", "");
            if (type.equals(script.getName().replaceAll("([a-z])([0-9A-Z])", "$1_$2").toUpperCase())) {
                inModel = true;
                System.out.println(" Model line: .(ModelScriptGenerator.java:%s)".formatted(i + 1));
            } else if (inModel) {
                inModel = false;
            }
            if (!inModel) continue;
            sb.append(originalLine.trim()).append("\n");
            sb.append("%s.mesh(\"%s\")\n".formatted(INDENT.repeat(4), script.getMeshName()));
            sb.append("%s.texture(\"%s\")\n".formatted(INDENT.repeat(4), script.getTextureName()));
            for (int i1 = 0; i1 < script.getAttachmentCount(); ++i1) {
                StringBuilder attachmentSb = new StringBuilder();
                boolean addedAttachment = false;
                ModelAttachment attachment = script.getAttachment(i1);
                attachmentSb.append("%s.addAttachment(attachment(\"%s\")\n".formatted(INDENT.repeat(4), attachment.getId()));
                if (attachment.getOffset().x() != 0.0f || attachment.getOffset().y() != 0.0f || attachment.getOffset().z() != 0.0f) {
                    attachmentSb.append("%s.offset(%s, %s, %s)\n".formatted(INDENT.repeat(5), AttachmentEditorState.formatFloat(attachment.getOffset().x()), AttachmentEditorState.formatFloat(attachment.getOffset().y()), AttachmentEditorState.formatFloat(attachment.getOffset().z())));
                    addedAttachment = true;
                }
                if (attachment.getRotate().x() != 0.0f || attachment.getRotate().y() != 0.0f || attachment.getRotate().z() != 0.0f) {
                    attachmentSb.append("%s.rotate(%s, %s, %s)\n".formatted(INDENT.repeat(5), AttachmentEditorState.formatFloat(attachment.getRotate().x()), AttachmentEditorState.formatFloat(attachment.getRotate().y()), AttachmentEditorState.formatFloat(attachment.getRotate().z())));
                    addedAttachment = true;
                }
                if (attachment.getScale() != 1.0f) {
                    attachmentSb.append("%s.scale(%s)\n".formatted(INDENT.repeat(5), AttachmentEditorState.formatFloat(attachment.getScale())));
                    addedAttachment = true;
                }
                if (attachment.getBone() != null) {
                    attachmentSb.append("%s.bone(\"%s\")\n".formatted(INDENT.repeat(5), attachment.getBone()));
                    addedAttachment = true;
                }
                attachmentSb.append("%s)\n".formatted(INDENT.repeat(4)));
                if (!addedAttachment) continue;
                sb.append((CharSequence)attachmentSb);
            }
            sb.append("%s)".formatted(INDENT.repeat(3)));
        }
        System.out.println(sb);
    }

    public static ArrayList<String> readScript(String fileName) {
        StringBuilder stringBuilder = new StringBuilder();
        fileName = ZomboidFileSystem.instance.getString(fileName);
        File file = new File(fileName);
        try (FileReader fr = new FileReader(file);
             BufferedReader br = new BufferedReader(fr);){
            String line;
            String eol = System.lineSeparator();
            while ((line = br.readLine()) != null) {
                stringBuilder.append(line);
                stringBuilder.append(eol);
            }
        }
        catch (Throwable t) {
            ExceptionLogger.logException(t);
            return null;
        }
        String totalFile = ScriptParser.stripComments(stringBuilder.toString());
        return ScriptParser.parseTokens(totalFile);
    }

    public static boolean updateScript(String fileName, ArrayList<String> tokens, ModelScript modelScript) {
        fileName = ZomboidFileSystem.instance.getString(fileName);
        for (int i = tokens.size() - 1; i >= 0; --i) {
            String moduleName;
            String token = tokens.get(i).trim();
            int firstOpen = token.indexOf("{");
            int lastClose = token.lastIndexOf("}");
            String header = token.substring(0, firstOpen);
            if (!header.startsWith("module")) continue;
            header = token.substring(0, firstOpen).trim();
            String[] ss = header.split("\\s+");
            String string = moduleName = ss.length > 1 ? ss[1].trim() : "";
            if (!moduleName.equals(modelScript.getModule().getName())) continue;
            String body = token.substring(firstOpen + 1, lastClose).trim();
            ArrayList<String> tokens1 = ScriptParser.parseTokens(body);
            for (int j = tokens1.size() - 1; j >= 0; --j) {
                String scriptName;
                String token1 = tokens1.get(j).trim();
                if (!token1.startsWith("model")) continue;
                firstOpen = token1.indexOf("{");
                header = token1.substring(0, firstOpen).trim();
                ss = header.split("\\s+");
                String string2 = scriptName = ss.length > 1 ? ss[1].trim() : "";
                if (!scriptName.equals(modelScript.getName())) continue;
                token1 = AttachmentEditorState.modelScriptToText(modelScript, token1).trim();
                tokens1.set(j, token1);
                String eol = System.lineSeparator();
                Object moduleStr = String.join((CharSequence)(eol + "\t"), tokens1);
                moduleStr = "module " + moduleName + eol + "{" + eol + "\t" + (String)moduleStr + eol + "}" + eol;
                tokens.set(i, (String)moduleStr);
                return AttachmentEditorState.writeScript(fileName, tokens);
            }
        }
        return false;
    }

    private static String modelScriptToText(ModelScript modelScript, String token) {
        int i;
        ScriptParser.Block block = ScriptParser.parse(token);
        block = block.children.get(0);
        for (i = block.children.size() - 1; i >= 0; --i) {
            ScriptParser.Block block1 = block.children.get(i);
            if (!"attachment".equals(block1.type)) continue;
            block.elements.remove(block1);
            block.children.remove(i);
        }
        for (i = 0; i < modelScript.getAttachmentCount(); ++i) {
            ModelAttachment attach = modelScript.getAttachment(i);
            ScriptParser.Block block1 = block.getBlock("attachment", attach.getId());
            if (block1 == null) {
                block1 = new ScriptParser.Block();
                block1.type = "attachment";
                block1.id = attach.getId();
                block1.setValue("offset", String.format(Locale.US, "%.4f %.4f %.4f", Float.valueOf(attach.getOffset().x()), Float.valueOf(attach.getOffset().y()), Float.valueOf(attach.getOffset().z())));
                block1.setValue("rotate", String.format(Locale.US, "%.4f %.4f %.4f", Float.valueOf(attach.getRotate().x()), Float.valueOf(attach.getRotate().y()), Float.valueOf(attach.getRotate().z())));
                if (attach.getScale() != 1.0f) {
                    block1.setValue("scale", String.format(Locale.US, "%.4f", Float.valueOf(attach.getScale())));
                }
                if (attach.getBone() != null) {
                    block1.setValue("bone", attach.getBone());
                }
                block.elements.add(block1);
                block.children.add(block1);
                continue;
            }
            block1.setValue("offset", String.format(Locale.US, "%.4f %.4f %.4f", Float.valueOf(attach.getOffset().x()), Float.valueOf(attach.getOffset().y()), Float.valueOf(attach.getOffset().z())));
            block1.setValue("rotate", String.format(Locale.US, "%.4f %.4f %.4f", Float.valueOf(attach.getRotate().x()), Float.valueOf(attach.getRotate().y()), Float.valueOf(attach.getRotate().z())));
            if (attach.getScale() == 1.0f) continue;
            block1.setValue("scale", String.format(Locale.US, "%.4f", Float.valueOf(attach.getScale())));
        }
        StringBuilder stringBuilder = new StringBuilder();
        String eol = System.lineSeparator();
        block.prettyPrint(1, stringBuilder, eol);
        return stringBuilder.toString();
    }

    /*
     * Enabled aggressive exception aggregation
     */
    public static boolean writeScript(String fileName, ArrayList<String> tokens) {
        String absolutePath = ZomboidFileSystem.instance.getString(fileName);
        File file = new File(absolutePath);
        try (FileWriter fw = new FileWriter(file);){
            boolean bl;
            try (BufferedWriter br = new BufferedWriter(fw);){
                DebugLog.General.printf("writing %s\n", fileName);
                for (String token : tokens) {
                    br.write(token);
                }
                bl = true;
            }
            return bl;
        }
        catch (Throwable t) {
            ExceptionLogger.logException(t);
            return false;
        }
    }
}

