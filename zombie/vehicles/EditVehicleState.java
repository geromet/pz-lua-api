/*
 * Decompiled with CFR 0.152.
 */
package zombie.vehicles;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.Externalizable;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;
import org.joml.Vector2f;
import org.joml.Vector3f;
import se.krka.kahlua.converter.KahluaConverterManager;
import se.krka.kahlua.integration.LuaCaller;
import se.krka.kahlua.j2se.J2SEPlatform;
import se.krka.kahlua.vm.KahluaTable;
import se.krka.kahlua.vm.KahluaThread;
import zombie.Lua.LuaManager;
import zombie.UsedFromLua;
import zombie.ZomboidFileSystem;
import zombie.core.Clipboard;
import zombie.core.Core;
import zombie.core.logger.ExceptionLogger;
import zombie.core.skinnedmodel.ModelManager;
import zombie.debug.DebugLog;
import zombie.debug.DebugOptions;
import zombie.gameStates.AttachmentEditorState;
import zombie.gameStates.GameState;
import zombie.gameStates.GameStateMachine;
import zombie.input.GameKeyboard;
import zombie.scripting.ScriptManager;
import zombie.scripting.ScriptParser;
import zombie.scripting.objects.ModelAttachment;
import zombie.scripting.objects.ModelScript;
import zombie.scripting.objects.VehicleScript;
import zombie.ui.UIElementInterface;
import zombie.ui.UIManager;
import zombie.util.list.PZArrayUtil;

@UsedFromLua
public final class EditVehicleState
extends GameState {
    private static final String INDENT = "    ";
    public static EditVehicleState instance;
    private LuaEnvironment luaEnv;
    private boolean exit;
    private String initialScript;
    private final ArrayList<UIElementInterface> gameUi = new ArrayList();
    private final ArrayList<UIElementInterface> selfUi = new ArrayList();
    private boolean suspendUi;
    private KahluaTable table;

    public EditVehicleState() {
        instance = this;
    }

    @Override
    public void enter() {
        instance = this;
        if (this.luaEnv == null) {
            this.luaEnv = new LuaEnvironment(LuaManager.platform, LuaManager.converterManager, LuaManager.env);
        }
        this.saveGameUI();
        if (this.selfUi.isEmpty()) {
            this.luaEnv.caller.pcall(this.luaEnv.thread, this.luaEnv.env.rawget("EditVehicleState_InitUI"), new Object[0]);
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

    public static EditVehicleState checkInstance() {
        if (instance != null) {
            if (EditVehicleState.instance.table == null || EditVehicleState.instance.table.getMetatable() == null) {
                instance = null;
            } else if (EditVehicleState.instance.table.getMetatable().rawget("_LUA_RELOADED_CHECK") == null) {
                instance = null;
            }
        }
        if (instance == null) {
            return new EditVehicleState();
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

    public void setScript(String scriptName) {
        if (this.table == null) {
            this.initialScript = scriptName;
        } else {
            this.luaEnv.caller.pcall(this.luaEnv.thread, this.table.rawget("setScript"), this.table, scriptName);
        }
    }

    public Object fromLua0(String func) {
        switch (func) {
            case "exit": {
                this.exit = true;
                return null;
            }
            case "getInitialScript": {
                return this.initialScript;
            }
        }
        throw new IllegalArgumentException("unhandled \"" + func + "\"");
    }

    public Object fromLua1(String func, Object arg0) {
        switch (func) {
            case "writeScript": {
                VehicleScript vehicleScript = ScriptManager.instance.getVehicle((String)arg0);
                if (vehicleScript == null) {
                    throw new NullPointerException("vehicle script \"" + String.valueOf(arg0) + "\" not found");
                }
                ArrayList<String> tokens = this.readScript(vehicleScript.getFileName());
                try {
                    this.readScriptNew(vehicleScript);
                }
                catch (Exception e) {
                    e.printStackTrace();
                }
                if (tokens != null) {
                    this.updateScript(vehicleScript.getFileName(), tokens, vehicleScript);
                }
                this.updateModelScripts(vehicleScript);
                return null;
            }
        }
        throw new IllegalArgumentException(String.format("unhandled \"%s\" \"%s\"", func, arg0));
    }

    private void readScriptNew(VehicleScript vehicleScript) throws IOException {
        Path path = Path.of("../src/main/java/generation/VehicleScriptGenerator.java", new String[0]);
        List<String> lines = Files.readAllLines(path);
        StringBuilder sb = new StringBuilder();
        StringBuilder modifiedSb = new StringBuilder();
        boolean inVehicle = false;
        boolean foundWheel = false;
        for (int i = 0; i < lines.size(); ++i) {
            String originalLine = lines.get(i);
            if (originalLine.trim().startsWith(".add(VehicleBuilder.withId(VehicleKey")) {
                String type = originalLine.trim().replaceFirst("\\.add\\(VehicleBuilder\\.withId\\(VehicleKey\\.", "").replaceFirst("\\)", "");
                if (type.equals(vehicleScript.getName().replaceAll("([a-z])([0-9A-Z])", "$1_$2").toUpperCase())) {
                    inVehicle = true;
                    System.out.println(" Vehicle line: .(VehicleScriptGenerator.java:%s)".formatted(i + 1));
                } else if (inVehicle) {
                    inVehicle = false;
                }
            }
            if (!inVehicle) continue;
            sb.append(originalLine).append("\n");
            i = this.removeBlock(originalLine, ".addPhysics(", lines, i, sb);
            i = this.removeBlock(originalLine, ".addArea(", lines, i, sb);
            i = this.removeBlock(originalLine, ".addAttachment(", lines, i, sb);
            i = this.removeBlock(originalLine, ".addPassenger(", lines, i, sb);
            if (originalLine.trim().startsWith(".addModel(model()")) {
                while (!originalLine.trim().startsWith(")")) {
                    originalLine = lines.get(++i);
                    originalLine = EditVehicleState.parseFloat("scale", originalLine, Float.valueOf(vehicleScript.getModelScale()));
                    originalLine = EditVehicleState.parseFloat("offset", originalLine, Float.valueOf(vehicleScript.getModel().getOffset().x / vehicleScript.getModelScale()), Float.valueOf(vehicleScript.getModel().getOffset().y / vehicleScript.getModelScale()), Float.valueOf(vehicleScript.getModel().getOffset().z / vehicleScript.getModelScale()));
                    sb.append(originalLine).append("\n");
                }
            }
            for (int x = 0; x < vehicleScript.getPartCount(); ++x) {
                VehicleScript.Part part = vehicleScript.getPart(x);
                for (int j = 0; j < part.getModelCount(); ++j) {
                    VehicleScript.Model model = part.getModel(j);
                    if (!originalLine.trim().startsWith(".addModel(model(\"%s\"".formatted(model.getId()))) continue;
                    int indentCount = (originalLine.length() - originalLine.stripLeading().length()) / 4;
                    while (!originalLine.startsWith("%s)".formatted(INDENT.repeat(indentCount)))) {
                        originalLine = lines.get(++i);
                        originalLine = EditVehicleState.parseFloat("offset", originalLine, Float.valueOf(model.getOffset().x), Float.valueOf(model.getOffset().y), Float.valueOf(model.getOffset().z));
                        originalLine = EditVehicleState.parseFloat("rotate", originalLine, Float.valueOf(model.getRotate().x), Float.valueOf(model.getRotate().y), Float.valueOf(model.getRotate().z));
                        sb.append(originalLine).append("\n");
                    }
                }
            }
            for (int j = 0; j < vehicleScript.getWheelCount(); ++j) {
                VehicleScript.Wheel wheel = vehicleScript.getWheel(j);
                if (!originalLine.trim().startsWith(".addWheel(wheel(VehicleWheel.%s".formatted(wheel.getId().replaceAll("([a-z])([0-9A-Z])", "$1_$2").toUpperCase()))) continue;
                foundWheel = true;
                int indentCount = (originalLine.length() - originalLine.stripLeading().length()) / 4;
                float modelScale = vehicleScript.getModelScale();
                while (!originalLine.startsWith("%s)".formatted(INDENT.repeat(indentCount)))) {
                    originalLine = lines.get(++i);
                    originalLine = EditVehicleState.parseFloat("offset", originalLine, Float.valueOf(wheel.getOffset().x / modelScale), Float.valueOf(wheel.getOffset().y / modelScale), Float.valueOf(wheel.getOffset().z / modelScale));
                    sb.append(originalLine).append("\n");
                }
            }
            if (originalLine.trim().startsWith(".extents(")) {
                Vector3f v = vehicleScript.getExtents();
                originalLine = EditVehicleState.parseFloat("extents", originalLine, Float.valueOf(v.x / vehicleScript.getModelScale()), Float.valueOf(v.y / vehicleScript.getModelScale()), Float.valueOf(v.z / vehicleScript.getModelScale()));
            }
            if (originalLine.trim().startsWith(".physicsChassisShape(") && vehicleScript.hasPhysicsChassisShape()) {
                Vector3f v = vehicleScript.getPhysicsChassisShape();
                originalLine = EditVehicleState.parseFloat("physicsChassisShape", originalLine, Float.valueOf(v.x / vehicleScript.getModelScale()), Float.valueOf(v.y / vehicleScript.getModelScale()), Float.valueOf(v.z / vehicleScript.getModelScale()));
            }
            if (originalLine.trim().startsWith(".centerOfMassOffset(")) {
                Vector3f v = vehicleScript.getCenterOfMassOffset();
                originalLine = EditVehicleState.parseFloat("centerOfMassOffset", originalLine, Float.valueOf(v.x / vehicleScript.getModelScale()), Float.valueOf(v.y / vehicleScript.getModelScale()), Float.valueOf(v.z / vehicleScript.getModelScale()));
            }
            if (originalLine.trim().startsWith(".shadowExtents(")) {
                Vector2f v = vehicleScript.getShadowExtents();
                originalLine = EditVehicleState.parseFloat("shadowExtents", originalLine, Float.valueOf(v.x / vehicleScript.getModelScale()), Float.valueOf(v.y / vehicleScript.getModelScale()));
            }
            if (originalLine.trim().startsWith(".shadowOffset(")) {
                Vector2f v = vehicleScript.getShadowOffset();
                originalLine = EditVehicleState.parseFloat("shadowOffset", originalLine, Float.valueOf(v.x / vehicleScript.getModelScale()), Float.valueOf(v.y / vehicleScript.getModelScale()));
            }
            if (!originalLine.startsWith("%s)".formatted(INDENT.repeat(3)))) continue;
            this.addBlocks(sb, originalLine, vehicleScript);
            break;
        }
        if (!foundWheel) {
            sb.append("\n Might have found extra wheels:\n");
            for (int j = 0; j < vehicleScript.getWheelCount(); ++j) {
                VehicleScript.Wheel wheel = vehicleScript.getWheel(j);
                sb.append("%s.addWheel(wheel(VehicleWheel.%s)\n".formatted(INDENT.repeat(3), wheel.getId().replaceAll("([a-z])([0-9A-Z])", "$1_$2").toUpperCase()));
                float modelScale = vehicleScript.getModelScale();
                sb.append("%s.front(%s)\n".formatted(INDENT.repeat(4), wheel.front));
                sb.append("%s.offset(%s, %s, %s)\n".formatted(INDENT.repeat(4), EditVehicleState.formatFloat(wheel.getOffset().x / modelScale), EditVehicleState.formatFloat(wheel.getOffset().y / modelScale), EditVehicleState.formatFloat(wheel.getOffset().z / modelScale)));
                sb.append("%s.radius(%s)\n".formatted(INDENT.repeat(4), EditVehicleState.formatFloat(wheel.radius / modelScale)));
                sb.append("%s.width(%s)\n".formatted(INDENT.repeat(4), EditVehicleState.formatFloat(wheel.width)));
                sb.append("%s)\n".formatted(INDENT.repeat(3)));
            }
        }
        System.out.println(sb);
        Clipboard.setClipboard(sb.toString());
    }

    private void addBlocks(StringBuilder sb, String originalLine, VehicleScript script) {
        sb.setLength(sb.length() - originalLine.length() - 1);
        this.addPhysicsBlock(sb, script);
        this.addAreaBlock(sb, script);
        this.addAttachmentBlock(sb, script);
        this.addPassengerBlock(sb, script);
        sb.append("%s)".formatted(INDENT.repeat(3)));
    }

    private void addPassengerBlock(StringBuilder sb, VehicleScript vehicleScript) {
        float modelScale = vehicleScript.getModelScale();
        for (int i = 0; i < vehicleScript.getPassengerCount(); ++i) {
            VehicleScript.Passenger pngr = vehicleScript.getPassenger(i);
            sb.append("%s.addPassenger(passenger(VehiclePassenger.%s)\n".formatted(INDENT.repeat(4), pngr.getId().replaceAll("([a-z])([0-9A-Z])", "$1_$2").toUpperCase()));
            for (VehicleScript.Position posn : pngr.positions) {
                sb.append("%s.addPosition(position(VehiclePosition.%s)\n".formatted(INDENT.repeat(5), posn.getId().replaceAll("([a-z])([0-9A-Z])", "$1_$2").toUpperCase()));
                sb.append("%s.offset(%s, %s, %s)\n".formatted(INDENT.repeat(6), EditVehicleState.formatFloat(posn.getOffset().x() / modelScale), EditVehicleState.formatFloat(posn.getOffset().y() / modelScale), EditVehicleState.formatFloat(posn.getOffset().z() / modelScale)));
                sb.append("%s.rotate(%s, %s, %s)\n".formatted(INDENT.repeat(6), EditVehicleState.formatFloat(posn.getRotate().x() / modelScale), EditVehicleState.formatFloat(posn.getRotate().y() / modelScale), EditVehicleState.formatFloat(posn.getRotate().z() / modelScale)));
                sb.append("%s)\n".formatted(INDENT.repeat(5)));
            }
            sb.append("%s)\n".formatted(INDENT.repeat(4)));
        }
    }

    private void addAttachmentBlock(StringBuilder sb, VehicleScript vehicleScript) {
        for (int i = 0; i < vehicleScript.getAttachmentCount(); ++i) {
            ModelAttachment attach = vehicleScript.getAttachment(i);
            float modelScale = vehicleScript.getModelScale();
            sb.append("%s.addAttachment(attachment(VehicleAttachment.%s)\n".formatted(INDENT.repeat(4), attach.getId().replaceAll("([a-z])([0-9A-Z])", "$1_$2").toUpperCase()));
            sb.append("%s.offset(%s, %s, %s)\n".formatted(INDENT.repeat(5), EditVehicleState.formatFloat(attach.getOffset().x() / modelScale), EditVehicleState.formatFloat(attach.getOffset().y() / modelScale), EditVehicleState.formatFloat(attach.getOffset().z() / modelScale)));
            sb.append("%s.rotate(%s, %s, %s)\n".formatted(INDENT.repeat(5), EditVehicleState.formatFloat(attach.getRotate().x()), EditVehicleState.formatFloat(attach.getRotate().y()), EditVehicleState.formatFloat(attach.getRotate().z())));
            if (attach.getBone() != null) {
                sb.append("%s.bone(%s)\n".formatted(INDENT.repeat(5), attach.getBone()));
            }
            if (attach.getCanAttach() != null) {
                sb.append("%s.canAttach(%s)\n".formatted(INDENT.repeat(5), PZArrayUtil.arrayToString(attach.getCanAttach(), "\"", "\"", ",")));
            }
            if (attach.getZOffset() != 0.0f) {
                sb.append("%s.zoffset(%s)\n".formatted(INDENT.repeat(5), EditVehicleState.formatFloat(attach.getZOffset())));
            }
            if (!attach.isUpdateConstraint()) {
                sb.append("%s.updateconstraint(%s)\n".formatted(INDENT.repeat(5), "false"));
            }
            sb.append("%s)\n".formatted(INDENT.repeat(4)));
        }
    }

    private void addAreaBlock(StringBuilder sb, VehicleScript vehicleScript) {
        for (int i = 0; i < vehicleScript.getAreaCount(); ++i) {
            VehicleScript.Area area = vehicleScript.getArea(i);
            sb.append("%s.addArea(area(VehicleArea.%s)\n".formatted(INDENT.repeat(4), area.getId().replaceAll("([a-z])([0-9A-Z])", "$1_$2").toUpperCase()));
            float scale = vehicleScript.getModelScale();
            sb.append("%s.xywh(%s, %s, %s, %s)\n".formatted(INDENT.repeat(5), EditVehicleState.formatFloat(area.getX().floatValue() / scale), EditVehicleState.formatFloat(area.getY().floatValue() / scale), EditVehicleState.formatFloat(area.getW().floatValue() / scale), EditVehicleState.formatFloat(area.getH().floatValue() / scale)));
            sb.append("%s)\n".formatted(INDENT.repeat(4)));
        }
    }

    private void addPhysicsBlock(StringBuilder sb, VehicleScript vehicleScript) {
        float modelScale = vehicleScript.getModelScale();
        for (int i = 0; i < vehicleScript.getPhysicsShapeCount(); ++i) {
            VehicleScript.PhysicsShape shape = vehicleScript.getPhysicsShape(i);
            sb.append("%s.addPhysics(physics(\"%s\")\n".formatted(INDENT.repeat(4), shape.getTypeString()));
            sb.append("%s.offset(%s, %s, %s)\n".formatted(INDENT.repeat(5), EditVehicleState.formatFloat(shape.getOffset().x() / modelScale), EditVehicleState.formatFloat(shape.getOffset().y() / modelScale), EditVehicleState.formatFloat(shape.getOffset().z() / modelScale)));
            if (shape.type == 1) {
                sb.append("%s.extents(%s, %s, %s)\n".formatted(INDENT.repeat(5), EditVehicleState.formatFloat(shape.getExtents().x() / modelScale), EditVehicleState.formatFloat(shape.getExtents().y() / modelScale), EditVehicleState.formatFloat(shape.getExtents().z() / modelScale)));
                sb.append("%s.rotate(%s, %s, %s)\n".formatted(INDENT.repeat(5), EditVehicleState.formatFloat(shape.getRotate().x()), EditVehicleState.formatFloat(shape.getRotate().y()), EditVehicleState.formatFloat(shape.getRotate().z())));
            }
            if (shape.type == 2) {
                sb.append("%s.radius(%s)\n".formatted(INDENT.repeat(5), EditVehicleState.formatFloat(shape.getRadius() / modelScale)));
            }
            if (shape.type == 3) {
                sb.append("%s.rotate(%s, %s, %s)\n".formatted(INDENT.repeat(5), EditVehicleState.formatFloat(shape.getRotate().x()), EditVehicleState.formatFloat(shape.getRotate().y()), EditVehicleState.formatFloat(shape.getRotate().z())));
                sb.append("%s.scale(%s)\n".formatted(INDENT.repeat(5), EditVehicleState.formatFloat(shape.getExtents().x() / modelScale)));
                sb.append("%s.physicsShapeScript(%s)\n".formatted(INDENT.repeat(5), shape.getPhysicsShapeScript()));
            }
            sb.append("%s)\n".formatted(INDENT.repeat(4)));
        }
    }

    private static String formatFloat(float value) {
        if (!Float.isInfinite(value) && (double)value == Math.floor(value)) {
            return String.format("%.1ff", Float.valueOf(value));
        }
        String valueStr = String.format(Locale.US, "%.4f", Float.valueOf(value));
        return "%sf".formatted(new BigDecimal(valueStr).stripTrailingZeros().toPlainString());
    }

    private int removeBlock(String originalLine, String type, List<String> lines, int i, StringBuilder sb) {
        if (originalLine.trim().startsWith(type)) {
            int indentCount = (originalLine.length() - originalLine.stripLeading().length()) / 4;
            sb.setLength(sb.length() - originalLine.length() - 1);
            while (!originalLine.startsWith("%s)".formatted(INDENT.repeat(indentCount)))) {
                originalLine = lines.get(++i);
            }
        }
        return i;
    }

    private static String parseFloat(String type, String originalLine, Float ... values2) {
        if (originalLine.trim().startsWith(".%s(".formatted(type))) {
            String string = "%s";
            String collect = Arrays.stream(values2).map(EditVehicleState::formatFloat).map(arg_0 -> EditVehicleState.lambda$parseFloat$0("%s", arg_0)).collect(Collectors.joining(", "));
            originalLine = originalLine.replaceAll("[.]%s[(][^)]*[)]".formatted(type), ".%s(%s)".formatted(type, collect));
        }
        return originalLine;
    }

    private ArrayList<String> readScript(String fileName) {
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

    private void updateScript(String fileName, ArrayList<String> tokens, VehicleScript vehicleScript) {
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
            if (!moduleName.equals(vehicleScript.getModule().getName())) continue;
            String body = token.substring(firstOpen + 1, lastClose).trim();
            ArrayList<String> tokens1 = ScriptParser.parseTokens(body);
            for (int j = tokens1.size() - 1; j >= 0; --j) {
                String scriptName;
                String token1 = tokens1.get(j).trim();
                if (!token1.startsWith("vehicle")) continue;
                firstOpen = token1.indexOf("{");
                header = token1.substring(0, firstOpen).trim();
                ss = header.split("\\s+");
                String string2 = scriptName = ss.length > 1 ? ss[1].trim() : "";
                if (!scriptName.equals(vehicleScript.getName())) continue;
                token1 = this.vehicleScriptToText(vehicleScript, token1).trim();
                tokens1.set(j, token1);
                String eol = System.lineSeparator();
                Object moduleStr = String.join((CharSequence)(eol + "\t"), tokens1);
                moduleStr = "module " + moduleName + eol + "{" + eol + "\t" + (String)moduleStr + eol + "}" + eol;
                tokens.set(i, (String)moduleStr);
                this.writeScript(fileName, tokens);
                return;
            }
        }
    }

    private String vehicleScriptToText(VehicleScript vehicleScript, String token) {
        ScriptParser.Block block2;
        ScriptParser.Block block1;
        int i;
        int i2;
        float scale = vehicleScript.getModelScale();
        ScriptParser.Block block = ScriptParser.parse(token);
        block = block.children.get(0);
        VehicleScript.Model vsm = vehicleScript.getModel();
        ScriptParser.Block block12 = block.getBlock("model", null);
        if (vsm != null && block12 != null) {
            float modelScale = vehicleScript.getModelScale();
            block12.setValue("scale", String.format(Locale.US, "%.4f", Float.valueOf(modelScale)));
            Vector3f v = vehicleScript.getModel().getOffset();
            block12.setValue("offset", String.format(Locale.US, "%.4f %.4f %.4f", Float.valueOf(v.x / scale), Float.valueOf(v.y / scale), Float.valueOf(v.z / scale)));
        }
        ArrayList<ScriptParser.Block> existingPhysicsShapes = new ArrayList<ScriptParser.Block>();
        for (i2 = 0; i2 < block.children.size(); ++i2) {
            ScriptParser.Block block13 = block.children.get(i2);
            if (!"physics".equals(block13.type)) continue;
            if (existingPhysicsShapes.size() == vehicleScript.getPhysicsShapeCount()) {
                block.elements.remove(block13);
                block.children.remove(i2);
                --i2;
                continue;
            }
            existingPhysicsShapes.add(block13);
        }
        for (i2 = 0; i2 < vehicleScript.getPhysicsShapeCount(); ++i2) {
            VehicleScript.PhysicsShape shape = vehicleScript.getPhysicsShape(i2);
            boolean replace = i2 < existingPhysicsShapes.size();
            ScriptParser.Block block14 = replace ? (ScriptParser.Block)existingPhysicsShapes.get(i2) : new ScriptParser.Block();
            block14.type = "physics";
            block14.id = shape.getTypeString();
            if (replace) {
                block14.elements.clear();
                block14.children.clear();
                block14.values.clear();
            }
            block14.setValue("offset", String.format(Locale.US, "%.4f %.4f %.4f", Float.valueOf(shape.getOffset().x() / scale), Float.valueOf(shape.getOffset().y() / scale), Float.valueOf(shape.getOffset().z() / scale)));
            if (shape.type == 1) {
                block14.setValue("extents", String.format(Locale.US, "%.4f %.4f %.4f", Float.valueOf(shape.getExtents().x() / scale), Float.valueOf(shape.getExtents().y() / scale), Float.valueOf(shape.getExtents().z() / scale)));
                block14.setValue("rotate", String.format(Locale.US, "%.4f %.4f %.4f", Float.valueOf(shape.getRotate().x()), Float.valueOf(shape.getRotate().y()), Float.valueOf(shape.getRotate().z())));
            }
            if (shape.type == 2) {
                block14.setValue("radius", String.format(Locale.US, "%.4f", Float.valueOf(shape.getRadius() / scale)));
            }
            if (shape.type == 3) {
                block14.setValue("rotate", String.format(Locale.US, "%.4f %.4f %.4f", Float.valueOf(shape.getRotate().x()), Float.valueOf(shape.getRotate().y()), Float.valueOf(shape.getRotate().z())));
                block14.setValue("physicsShapeScript", shape.getPhysicsShapeScript());
                block14.setValue("scale", String.format(Locale.US, "%.4f", Float.valueOf(shape.getExtents().x() / scale)));
            }
            if (replace) continue;
            block.elements.add(block14);
            block.children.add(block14);
        }
        this.removeAttachments(block);
        for (i2 = 0; i2 < vehicleScript.getAttachmentCount(); ++i2) {
            ModelAttachment attach = vehicleScript.getAttachment(i2);
            this.attachmentToBlock(vehicleScript, attach, block);
        }
        Externalizable v = vehicleScript.getExtents();
        block.setValue("extents", String.format(Locale.US, "%.4f %.4f %.4f", Float.valueOf(v.x / scale), Float.valueOf(v.y / scale), Float.valueOf(v.z / scale)));
        if (vehicleScript.hasPhysicsChassisShape()) {
            v = vehicleScript.getPhysicsChassisShape();
            block.setValue("physicsChassisShape", String.format(Locale.US, "%.4f %.4f %.4f", Float.valueOf(v.x / scale), Float.valueOf(v.y / scale), Float.valueOf(v.z / scale)));
        }
        v = vehicleScript.getCenterOfMassOffset();
        block.setValue("centerOfMassOffset", String.format(Locale.US, "%.4f %.4f %.4f", Float.valueOf(v.x / scale), Float.valueOf(v.y / scale), Float.valueOf(v.z / scale)));
        v = vehicleScript.getShadowExtents();
        boolean exists = block.getValue("shadowExtents") != null;
        block.setValue("shadowExtents", String.format(Locale.US, "%.4f %.4f", Float.valueOf(((Vector2f)v).x / scale), Float.valueOf(((Vector2f)v).y / scale)));
        if (!exists) {
            block.moveValueAfter("shadowExtents", "centerOfMassOffset");
        }
        v = vehicleScript.getShadowOffset();
        exists = block.getValue("shadowOffset") != null;
        block.setValue("shadowOffset", String.format(Locale.US, "%.4f %.4f", Float.valueOf(((Vector2f)v).x / scale), Float.valueOf(((Vector2f)v).y / scale)));
        if (!exists) {
            block.moveValueAfter("shadowOffset", "shadowExtents");
        }
        for (i = 0; i < vehicleScript.getAreaCount(); ++i) {
            VehicleScript.Area area = vehicleScript.getArea(i);
            block1 = block.getBlock("area", area.getId());
            if (block1 == null) continue;
            block1.setValue("xywh", String.format(Locale.US, "%.4f %.4f %.4f %.4f", area.getX() / (double)scale, area.getY() / (double)scale, area.getW() / (double)scale, area.getH() / (double)scale));
        }
        for (i = 0; i < vehicleScript.getPartCount(); ++i) {
            VehicleScript.Part part = vehicleScript.getPart(i);
            block1 = block.getBlock("part", part.getId());
            if (block1 == null) continue;
            for (int j = 0; j < part.getModelCount(); ++j) {
                VehicleScript.Model model = part.getModel(j);
                block2 = block1.getBlock("model", model.getId());
                if (block2 == null) continue;
                block2.setValue("offset", String.format(Locale.US, "%.4f %.4f %.4f", Float.valueOf(model.offset.x), Float.valueOf(model.offset.y), Float.valueOf(model.offset.z)));
                block2.setValue("rotate", String.format(Locale.US, "%.4f %.4f %.4f", Float.valueOf(model.rotate.x), Float.valueOf(model.rotate.y), Float.valueOf(model.rotate.z)));
            }
        }
        for (i = 0; i < vehicleScript.getPassengerCount(); ++i) {
            VehicleScript.Passenger pngr = vehicleScript.getPassenger(i);
            block1 = block.getBlock("passenger", pngr.getId());
            if (block1 == null) continue;
            for (VehicleScript.Position posn : pngr.positions) {
                block2 = block1.getBlock("position", posn.id);
                if (block2 == null) continue;
                block2.setValue("offset", String.format(Locale.US, "%.4f %.4f %.4f", Float.valueOf(posn.offset.x / scale), Float.valueOf(posn.offset.y / scale), Float.valueOf(posn.offset.z / scale)));
                block2.setValue("rotate", String.format(Locale.US, "%.4f %.4f %.4f", Float.valueOf(posn.rotate.x / scale), Float.valueOf(posn.rotate.y / scale), Float.valueOf(posn.rotate.z / scale)));
            }
        }
        for (i = 0; i < vehicleScript.getWheelCount(); ++i) {
            VehicleScript.Wheel wheel = vehicleScript.getWheel(i);
            block1 = block.getBlock("wheel", wheel.getId());
            if (block1 == null) continue;
            block1.setValue("offset", String.format(Locale.US, "%.4f %.4f %.4f", Float.valueOf(wheel.offset.x / scale), Float.valueOf(wheel.offset.y / scale), Float.valueOf(wheel.offset.z / scale)));
        }
        StringBuilder stringBuilder = new StringBuilder();
        String eol = System.lineSeparator();
        block.prettyPrint(1, stringBuilder, eol);
        return stringBuilder.toString();
    }

    private void removeAttachments(ScriptParser.Block block) {
        for (int i = block.children.size() - 1; i >= 0; --i) {
            ScriptParser.Block block1 = block.children.get(i);
            if (!"attachment".equals(block1.type)) continue;
            block.elements.remove(block1);
            block.children.remove(i);
        }
    }

    private void attachmentToBlock(VehicleScript vehicleScript, ModelAttachment attach, ScriptParser.Block block) {
        float scale = vehicleScript.getModelScale();
        ScriptParser.Block block1 = block.getBlock("attachment", attach.getId());
        if (block1 == null) {
            block1 = new ScriptParser.Block();
            block1.type = "attachment";
            block1.id = attach.getId();
            block.elements.add(block1);
            block.children.add(block1);
        }
        block1.setValue("offset", String.format(Locale.US, "%.4f %.4f %.4f", Float.valueOf(attach.getOffset().x() / scale), Float.valueOf(attach.getOffset().y() / scale), Float.valueOf(attach.getOffset().z() / scale)));
        block1.setValue("rotate", String.format(Locale.US, "%.4f %.4f %.4f", Float.valueOf(attach.getRotate().x()), Float.valueOf(attach.getRotate().y()), Float.valueOf(attach.getRotate().z())));
        if (attach.getBone() != null) {
            block1.setValue("bone", attach.getBone());
        }
        if (attach.getCanAttach() != null) {
            block1.setValue("canAttach", PZArrayUtil.arrayToString(attach.getCanAttach(), "", "", ","));
        }
        if (attach.getZOffset() != 0.0f) {
            block1.setValue("zoffset", String.format(Locale.US, "%.4f", Float.valueOf(attach.getZOffset())));
        }
        if (!attach.isUpdateConstraint()) {
            block1.setValue("updateconstraint", "false");
        }
    }

    private void writeScript(String fileName, ArrayList<String> tokens) {
        String absolutePath = ZomboidFileSystem.instance.getString(fileName);
        File file = new File(absolutePath);
        try (FileWriter fw = new FileWriter(file);
             BufferedWriter br = new BufferedWriter(fw);){
            DebugLog.General.printf("writing %s\n", fileName);
            for (String token : tokens) {
                br.write(token);
            }
            this.luaEnv.caller.pcall(this.luaEnv.thread, this.table.rawget("wroteScript"), this.table, absolutePath);
        }
        catch (Throwable t) {
            ExceptionLogger.logException(t);
        }
    }

    private void updateModelScripts(VehicleScript vehicleScript) {
        for (int i = 0; i < vehicleScript.getPartCount(); ++i) {
            VehicleScript.Part part = vehicleScript.getPart(i);
            for (int j = 0; j < part.getModelCount(); ++j) {
                String fileName;
                ArrayList<String> tokens;
                ModelScript modelScript;
                VehicleScript.Model scriptModel = part.getModel(j);
                if (scriptModel.getFile() == null || (modelScript = ScriptManager.instance.getModelScript(scriptModel.getFile())) == null || modelScript.getAttachmentCount() == 0 || (tokens = AttachmentEditorState.readScript(modelScript.getFileName())) == null || !AttachmentEditorState.updateScript(fileName = modelScript.getFileName(), tokens, modelScript)) continue;
                String absolutePath = ZomboidFileSystem.instance.getString(fileName);
                this.luaEnv.caller.pcall(this.luaEnv.thread, this.table.rawget("wroteScript"), this.table, absolutePath);
            }
        }
    }

    private static /* synthetic */ String lambda$parseFloat$0(String rec$, Object xva$0) {
        return "%s".formatted(xva$0);
    }

    public static final class LuaEnvironment {
        public J2SEPlatform platform;
        public KahluaTable env;
        public KahluaThread thread;
        public LuaCaller caller;

        public LuaEnvironment(J2SEPlatform platform, KahluaConverterManager converterManager, KahluaTable env) {
            this.platform = platform;
            this.env = env;
            this.thread = LuaManager.thread;
            this.caller = LuaManager.caller;
        }
    }
}

