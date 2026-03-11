/*
 * Decompiled with CFR 0.152.
 */
package zombie.core.skinnedmodel.advancedanimation.debug;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Objects;
import java.util.Queue;
import zombie.UsedFromLua;
import zombie.characters.IsoGameCharacter;
import zombie.core.Color;
import zombie.core.Colors;
import zombie.core.skinnedmodel.advancedanimation.AnimLayer;
import zombie.core.skinnedmodel.advancedanimation.IAnimationVariableSlot;
import zombie.core.skinnedmodel.advancedanimation.LiveAnimNode;
import zombie.core.skinnedmodel.animation.AnimationTrack;

@UsedFromLua
public final class AnimatorDebugMonitor {
    public static AnimatorDebugMonitor instance;
    private IsoGameCharacter targetIsoGameCharacter;
    private static final ArrayList<String> knownVariables;
    private static boolean knownVarsDirty;
    private String currentState = "null";
    private MonitoredLayer[] monitoredLayers;
    private final HashMap<String, MonitoredVar> monitoredVariables = new HashMap();
    private final ArrayList<String> customVariables = new ArrayList();
    private final LinkedList<MonitorLogLine> logLines = new LinkedList();
    private final Queue<MonitorLogLine> logLineQueue = new LinkedList<MonitorLogLine>();
    private boolean floatsListDirty;
    private boolean hasFilterChanges;
    private boolean hasLogUpdates;
    private String logString = "";
    private static final int maxLogSize = 1028;
    private static final int maxOutputLines = 128;
    private static final int maxFloatCache = 1024;
    private final ArrayList<Float> floatsOut = new ArrayList();
    private MonitoredVar selectedVariable;
    private int tickCount;
    private boolean doTickStamps;
    private static final int tickStampLength = 10;
    private static final Color col_curstate;
    private static final Color col_layer_nodename;
    private static final Color col_layer_activated;
    private static final Color col_layer_deactivated;
    private static final Color col_track_activated;
    private static final Color col_track_deactivated;
    private static final Color col_node_activated;
    private static final Color col_node_deactivated;
    private static final Color col_var_activated;
    private static final Color col_var_changed;
    private static final Color col_var_deactivated;
    private static final String TAG_VAR = "[variable]";
    private static final String TAG_LAYER = "[layer]";
    private static final String TAG_NODE = "[active_nodes]";
    private static final String TAG_TRACK = "[anim_tracks]";
    private final boolean[] logFlags;

    public IsoGameCharacter getTarget() {
        return this.targetIsoGameCharacter;
    }

    public void setTarget(IsoGameCharacter isoGameCharacter) {
        this.targetIsoGameCharacter = isoGameCharacter;
    }

    public AnimatorDebugMonitor(IsoGameCharacter chr) {
        int i;
        if (instance != this) {
            instance = this;
            this.targetIsoGameCharacter = chr;
        }
        this.logFlags = new boolean[LogType.MAX.value()];
        this.logFlags[LogType.DEFAULT.value()] = true;
        for (i = 0; i < this.logFlags.length; ++i) {
            this.logFlags[i] = true;
        }
        for (i = 0; i < 1024; ++i) {
            this.floatsOut.add(Float.valueOf(0.0f));
        }
        this.initCustomVars();
        if (chr != null && chr.advancedAnimator != null) {
            ArrayList<String> vars = chr.advancedAnimator.debugGetVariables();
            for (String v : vars) {
                AnimatorDebugMonitor.registerVariable(v);
            }
        }
    }

    private void initCustomVars() {
        this.addCustomVariable("aim");
        this.addCustomVariable("bdead");
        this.addCustomVariable("bfalling");
        this.addCustomVariable("baimatfloor");
        this.addCustomVariable("battackfrombehind");
        this.addCustomVariable("attacktype");
        this.addCustomVariable("bundervehicle");
        this.addCustomVariable("reanimatetimer");
        this.addCustomVariable("isattacking");
        this.addCustomVariable("canclimbdownrope");
        this.addCustomVariable("frombehind");
        this.addCustomVariable("fallonfront");
        this.addCustomVariable("hashitreaction");
        this.addCustomVariable("hitreaction");
        this.addCustomVariable("collided");
        this.addCustomVariable("collidetype");
        this.addCustomVariable("intrees");
    }

    public void addCustomVariable(String var) {
        String v = var.toLowerCase();
        if (!this.customVariables.contains(v)) {
            this.customVariables.add(v);
        }
        AnimatorDebugMonitor.registerVariable(var);
    }

    public void removeCustomVariable(String var) {
        String v = var.toLowerCase();
        this.customVariables.remove(v);
    }

    public void setFilter(int index, boolean b) {
        if (index >= 0 && index < LogType.MAX.value()) {
            this.logFlags[index] = b;
            this.hasFilterChanges = true;
        }
    }

    public boolean getFilter(int index) {
        if (index >= 0 && index < LogType.MAX.value()) {
            return this.logFlags[index];
        }
        return false;
    }

    public boolean isDoTickStamps() {
        return this.doTickStamps;
    }

    public void setDoTickStamps(boolean doTickStamps) {
        if (this.doTickStamps != doTickStamps) {
            this.doTickStamps = doTickStamps;
            this.hasFilterChanges = true;
        }
    }

    private void queueLogLine(String str) {
        this.addLogLine(LogType.DEFAULT, str, null, true);
    }

    private void queueLogLine(String str, Color col) {
        this.addLogLine(LogType.DEFAULT, str, col, true);
    }

    private void queueLogLine(LogType t, String str, Color col) {
        this.addLogLine(t, str, col, true);
    }

    private void addLogLine(String str) {
        this.addLogLine(LogType.DEFAULT, str, null, false);
    }

    private void addLogLine(String str, Color col) {
        this.addLogLine(LogType.DEFAULT, str, col, false);
    }

    private void addLogLine(String str, Color col, boolean queue) {
        this.addLogLine(LogType.DEFAULT, str, col, queue);
    }

    private void addLogLine(LogType t, String str, Color col) {
        this.addLogLine(t, str, col, false);
    }

    private void addLogLine(LogType t, String str, Color col, boolean queue) {
        MonitorLogLine log = new MonitorLogLine(this);
        log.line = str;
        log.color = col;
        log.type = t;
        log.tick = this.tickCount;
        if (queue) {
            this.logLineQueue.add(log);
        } else {
            this.log(log);
        }
    }

    private void log(MonitorLogLine l) {
        this.logLines.addFirst(l);
        if (this.logLines.size() > 1028) {
            this.logLines.removeLast();
        }
        this.hasLogUpdates = true;
    }

    private void processQueue() {
        while (!this.logLineQueue.isEmpty()) {
            MonitorLogLine l = this.logLineQueue.poll();
            this.log(l);
        }
    }

    private void preUpdate() {
        for (Map.Entry<String, MonitoredVar> entry : this.monitoredVariables.entrySet()) {
            entry.getValue().updated = false;
        }
        for (int index = 0; index < this.monitoredLayers.length; ++index) {
            MonitoredLayer l = this.monitoredLayers[index];
            l.updated = false;
            for (Map.Entry<String, MonitoredNode> entry : l.activeNodes.entrySet()) {
                entry.getValue().updated = false;
            }
            for (Map.Entry<String, Object> entry : l.animTracks.entrySet()) {
                ((MonitoredTrack)entry.getValue()).updated = false;
            }
        }
    }

    private void postUpdate() {
        for (Map.Entry<String, MonitoredVar> entry : this.monitoredVariables.entrySet()) {
            if (!entry.getValue().active || entry.getValue().updated) continue;
            this.addLogLine(LogType.VAR, "[variable] : removed -> '" + entry.getKey() + "', last value: '" + entry.getValue().value + "'.", col_var_deactivated);
            entry.getValue().active = false;
        }
        for (int index = 0; index < this.monitoredLayers.length; ++index) {
            MonitoredLayer l = this.monitoredLayers[index];
            for (Map.Entry<String, MonitoredNode> entry : l.activeNodes.entrySet()) {
                if (!entry.getValue().active || entry.getValue().updated) continue;
                this.addLogLine(LogType.NODE, "[layer][" + l.index + "] [active_nodes] : deactivated -> '" + entry.getValue().name + "'.", col_node_deactivated);
                entry.getValue().active = false;
            }
            for (Map.Entry<String, Object> entry : l.animTracks.entrySet()) {
                if (!((MonitoredTrack)entry.getValue()).active || ((MonitoredTrack)entry.getValue()).updated) continue;
                this.addLogLine(LogType.TRACK, "[layer][" + l.index + "] [anim_tracks] : deactivated -> '" + ((MonitoredTrack)entry.getValue()).name + "'.", col_track_deactivated);
                ((MonitoredTrack)entry.getValue()).active = false;
            }
            if (!l.active || l.updated) continue;
            this.addLogLine(LogType.LAYER, "[layer][" + index + "] : deactivated (last animstate: '" + l.nodeName + "').", col_layer_deactivated);
            l.active = false;
        }
    }

    /*
     * WARNING - void declaration
     */
    public void update(IsoGameCharacter chr, AnimLayer[] layers) {
        void var4_7;
        if (chr == null) {
            return;
        }
        this.ensureLayers(layers);
        this.preUpdate();
        for (IAnimationVariableSlot iAnimationVariableSlot : chr.getGameVariables()) {
            this.updateVariable(iAnimationVariableSlot.getKey(), iAnimationVariableSlot.getValueString());
        }
        for (String var : this.customVariables) {
            String val = chr.getVariableString(var);
            if (val == null) continue;
            this.updateVariable(var, val);
        }
        this.updateCurrentState(chr.getCurrentState() == null ? "null" : chr.getCurrentState().getClass().getSimpleName());
        boolean bl = false;
        while (var4_7 < layers.length) {
            if (layers[var4_7] != null) {
                this.updateLayer((int)var4_7, layers[var4_7]);
            }
            ++var4_7;
        }
        this.postUpdate();
        this.processQueue();
        ++this.tickCount;
    }

    private void updateCurrentState(String state) {
        if (!this.currentState.equals(state)) {
            this.queueLogLine("Character.currentState changed from '" + this.currentState + "' to: '" + state + "'.", col_curstate);
            this.currentState = state;
        }
    }

    private void updateLayer(int index, AnimLayer layer) {
        MonitoredLayer mLayer = this.monitoredLayers[index];
        String nodename = layer.getDebugNodeName();
        if (!mLayer.active) {
            mLayer.active = true;
            this.queueLogLine(LogType.LAYER, "[layer][" + index + "] activated -> animstate: '" + nodename + "'.", col_layer_activated);
        }
        if (!mLayer.nodeName.equals(nodename)) {
            this.queueLogLine(LogType.LAYER, "[layer][" + index + "] changed -> animstate from '" + mLayer.nodeName + "' to: '" + nodename + "'.", col_layer_nodename);
            mLayer.nodeName = nodename;
        }
        for (LiveAnimNode an : layer.getLiveAnimNodes()) {
            this.updateActiveNode(mLayer, an.getSourceNode().name);
        }
        if (layer.getAnimationTrack() != null) {
            for (AnimationTrack anmt : layer.getAnimationTrack().getTracks()) {
                if (anmt.getLayerIdx() != index) continue;
                this.updateAnimTrack(mLayer, anmt.getName(), anmt.getBlendWeight());
            }
        }
        mLayer.updated = true;
    }

    private void updateActiveNode(MonitoredLayer l, String name) {
        MonitoredNode node = l.activeNodes.get(name);
        if (node == null) {
            node = new MonitoredNode(this);
            node.name = name;
            l.activeNodes.put(name, node);
        }
        if (!node.active) {
            node.active = true;
            this.queueLogLine(LogType.NODE, "[layer][" + l.index + "] [active_nodes] : activated -> '" + name + "'.", col_node_activated);
        }
        node.updated = true;
    }

    private void updateAnimTrack(MonitoredLayer l, String name, float blendDelta) {
        MonitoredTrack track = l.animTracks.get(name);
        if (track == null) {
            track = new MonitoredTrack(this);
            track.name = name;
            track.blendDelta = blendDelta;
            l.animTracks.put(name, track);
        }
        if (!track.active) {
            track.active = true;
            this.queueLogLine(LogType.TRACK, "[layer][" + l.index + "] [anim_tracks] : activated -> '" + name + "'.", col_track_activated);
        }
        if (track.blendDelta != blendDelta) {
            track.blendDelta = blendDelta;
        }
        track.updated = true;
    }

    private void updateVariable(String key, String val) {
        MonitoredVar var = this.monitoredVariables.get(key);
        boolean newvar = false;
        if (var == null) {
            var = new MonitoredVar(this);
            this.monitoredVariables.put(key, var);
            newvar = true;
        }
        if (!var.active) {
            var.active = true;
            var.key = key;
            var.value = val;
            this.queueLogLine(LogType.VAR, "[variable] : added -> '" + key + "', value: '" + val + "'.", col_var_activated);
            if (newvar) {
                AnimatorDebugMonitor.registerVariable(key);
            }
        } else if (val == null) {
            if (var.isFloat) {
                var.isFloat = false;
                this.floatsListDirty = true;
            }
            var.value = null;
        } else if (var.value == null || !var.value.equals(val)) {
            block12: {
                try {
                    float f = Float.parseFloat(val);
                    var.logFloat(f);
                    if (!var.isFloat) {
                        var.isFloat = true;
                        this.floatsListDirty = true;
                    }
                }
                catch (NumberFormatException e) {
                    if (!var.isFloat) break block12;
                    var.isFloat = false;
                    this.floatsListDirty = true;
                }
            }
            if (!var.isFloat) {
                this.queueLogLine(LogType.VAR, "[variable] : updated -> '" + key + "' changed from '" + var.value + "' to: '" + val + "'.", col_var_changed);
            }
            var.value = val;
        }
        var.updated = true;
    }

    private void buildLogString() {
        ListIterator<MonitorLogLine> li = this.logLines.listIterator(0);
        int outputLines = 0;
        int indexStart = 0;
        while (li.hasNext()) {
            MonitorLogLine log = li.next();
            ++indexStart;
            if (!this.logFlags[log.type.value()] || ++outputLines < 128) continue;
            break;
        }
        if (indexStart == 0) {
            this.logString = "";
            return;
        }
        li = this.logLines.listIterator(indexStart);
        StringBuilder s = new StringBuilder();
        String prefix = " <TEXT> ";
        String suffix = " <LINE> ";
        while (li.hasPrevious()) {
            MonitorLogLine log = li.previous();
            if (!this.logFlags[log.type.value()]) continue;
            s.append(" <TEXT> ");
            if (this.doTickStamps) {
                s.append("[");
                s.append(String.format("%010d", log.tick));
                s.append("]");
            }
            if (log.color != null) {
                s.append(" <RGB:");
                s.append(log.color.r);
                s.append(",");
                s.append(log.color.g);
                s.append(",");
                s.append(log.color.b);
                s.append("> ");
            }
            s.append(log.line);
            s.append(" <LINE> ");
        }
        this.logString = s.toString();
        this.hasLogUpdates = false;
        this.hasFilterChanges = false;
    }

    public boolean IsDirty() {
        return this.hasLogUpdates || this.hasFilterChanges;
    }

    public String getLogString() {
        if (this.hasLogUpdates || this.hasFilterChanges) {
            this.buildLogString();
        }
        return this.logString;
    }

    public boolean IsDirtyFloatList() {
        return this.floatsListDirty;
    }

    public ArrayList<String> getFloatNames() {
        this.floatsListDirty = false;
        ArrayList<String> names = new ArrayList<String>();
        for (Map.Entry<String, MonitoredVar> entry : this.monitoredVariables.entrySet()) {
            if (!entry.getValue().isFloat) continue;
            names.add(entry.getValue().key);
        }
        Collections.sort(names);
        return names;
    }

    public static boolean isKnownVarsDirty() {
        return knownVarsDirty;
    }

    public static List<String> getKnownVariables() {
        knownVarsDirty = false;
        Collections.sort(knownVariables);
        return knownVariables;
    }

    public void setSelectedVariable(String key) {
        this.selectedVariable = key == null ? null : this.monitoredVariables.get(key);
    }

    public String getSelectedVariable() {
        if (this.selectedVariable != null) {
            return this.selectedVariable.key;
        }
        return null;
    }

    public float getSelectedVariableFloat() {
        if (this.selectedVariable != null) {
            return this.selectedVariable.valFloat;
        }
        return 0.0f;
    }

    public String getSelectedVarMinFloat() {
        if (this.selectedVariable != null && this.selectedVariable.isFloat && this.selectedVariable.min != -1.0f) {
            return "" + this.selectedVariable.min;
        }
        return "-1.0";
    }

    public String getSelectedVarMaxFloat() {
        if (this.selectedVariable != null && this.selectedVariable.isFloat && this.selectedVariable.max != -1.0f) {
            return "" + this.selectedVariable.max;
        }
        return "1.0";
    }

    public ArrayList<Float> getSelectedVarFloatList() {
        if (this.selectedVariable != null && this.selectedVariable.isFloat) {
            MonitoredVar v = this.selectedVariable;
            int index = v.index - 1;
            if (index < 0) {
                index = 0;
            }
            float max = v.max - v.min;
            for (int i = 0; i < 1024; ++i) {
                float f = (v.floats[index--] - v.min) / max;
                this.floatsOut.set(i, Float.valueOf(f));
                if (index >= 0) continue;
                index = v.floats.length - 1;
            }
            return this.floatsOut;
        }
        return null;
    }

    public static void registerVariable(String key) {
        if (key == null) {
            return;
        }
        if (!knownVariables.contains(key = key.toLowerCase())) {
            knownVariables.add(key);
            knownVarsDirty = true;
        }
    }

    private void ensureLayers(AnimLayer[] layers) {
        int size = layers.length;
        if (this.monitoredLayers == null || this.monitoredLayers.length != size) {
            this.monitoredLayers = new MonitoredLayer[size];
            for (int i = 0; i < size; ++i) {
                this.monitoredLayers[i] = new MonitoredLayer(this, i);
            }
        }
    }

    static {
        knownVariables = new ArrayList();
        col_curstate = Colors.Cyan;
        col_layer_nodename = Colors.CornFlowerBlue;
        col_layer_activated = Colors.DarkTurquoise;
        col_layer_deactivated = Colors.Orange;
        col_track_activated = Colors.SandyBrown;
        col_track_deactivated = Colors.Salmon;
        col_node_activated = Colors.Pink;
        col_node_deactivated = Colors.Plum;
        col_var_activated = Colors.Chartreuse;
        col_var_changed = Colors.LimeGreen;
        col_var_deactivated = Colors.Gold;
    }

    private static enum LogType {
        DEFAULT(0),
        LAYER(1),
        NODE(2),
        TRACK(3),
        VAR(4),
        MAX(5);

        private final int val;

        private LogType(int value) {
            this.val = value;
        }

        public int value() {
            return this.val;
        }
    }

    private class MonitorLogLine {
        String line;
        Color color;
        LogType type;
        int tick;

        private MonitorLogLine(AnimatorDebugMonitor animatorDebugMonitor) {
            Objects.requireNonNull(animatorDebugMonitor);
            this.type = LogType.DEFAULT;
        }
    }

    private class MonitoredVar {
        String key;
        String value;
        boolean isFloat;
        float valFloat;
        boolean active;
        boolean updated;
        float[] floats;
        int index;
        float min;
        float max;

        private MonitoredVar(AnimatorDebugMonitor animatorDebugMonitor) {
            Objects.requireNonNull(animatorDebugMonitor);
            this.key = "";
            this.value = "";
            this.min = -1.0f;
            this.max = 1.0f;
        }

        public void logFloat(float f) {
            if (this.floats == null) {
                this.floats = new float[1024];
            }
            if (f == this.valFloat) {
                return;
            }
            this.valFloat = f;
            this.floats[this.index++] = f;
            if (f < this.min) {
                this.min = f;
            }
            if (f > this.max) {
                this.max = f;
            }
            if (this.index >= 1024) {
                this.index = 0;
            }
        }
    }

    private class MonitoredLayer {
        int index;
        String nodeName;
        HashMap<String, MonitoredNode> activeNodes;
        HashMap<String, MonitoredTrack> animTracks;
        boolean active;
        boolean updated;

        public MonitoredLayer(AnimatorDebugMonitor animatorDebugMonitor, int idx) {
            Objects.requireNonNull(animatorDebugMonitor);
            this.nodeName = "";
            this.activeNodes = new HashMap();
            this.animTracks = new HashMap();
            this.index = idx;
        }
    }

    private class MonitoredNode {
        String name;
        boolean active;
        boolean updated;

        private MonitoredNode(AnimatorDebugMonitor animatorDebugMonitor) {
            Objects.requireNonNull(animatorDebugMonitor);
            this.name = "";
        }
    }

    private class MonitoredTrack {
        String name;
        float blendDelta;
        boolean active;
        boolean updated;

        private MonitoredTrack(AnimatorDebugMonitor animatorDebugMonitor) {
            Objects.requireNonNull(animatorDebugMonitor);
            this.name = "";
        }
    }
}

