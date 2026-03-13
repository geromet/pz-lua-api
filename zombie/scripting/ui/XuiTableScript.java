/*
 * Decompiled with CFR 0.152.
 */
package zombie.scripting.ui;

import java.util.ArrayList;
import zombie.UsedFromLua;
import zombie.core.math.PZMath;
import zombie.scripting.ScriptParser;
import zombie.scripting.ui.XuiAutoApply;
import zombie.scripting.ui.XuiManager;
import zombie.scripting.ui.XuiScript;
import zombie.scripting.ui.XuiScriptType;

@UsedFromLua
public class XuiTableScript
extends XuiScript {
    private final ArrayList<XuiTableColumnScript> columns = new ArrayList();
    private final ArrayList<XuiTableRowScript> rows = new ArrayList();
    private final ArrayList<XuiTableCellScript> cells = new ArrayList();
    private final XuiScript.XuiString xuiCellStyle = this.addVar(new XuiScript.XuiString(this, "xuiCellStyle"));
    private final XuiScript.XuiString xuiRowStyle;
    private final XuiScript.XuiString xuiColumnStyle;

    public XuiTableScript(String xuiLayoutName, boolean readAltKeys, XuiScriptType type) {
        super(xuiLayoutName, readAltKeys, "ISXuiTableLayout", type);
        this.xuiCellStyle.setAutoApplyMode(XuiAutoApply.Forbidden);
        this.xuiCellStyle.setScriptLoadEnabled(false);
        this.xuiCellStyle.setIgnoreStyling(true);
        this.xuiRowStyle = this.addVar(new XuiScript.XuiString(this, "xuiRowStyle"));
        this.xuiRowStyle.setAutoApplyMode(XuiAutoApply.Forbidden);
        this.xuiRowStyle.setScriptLoadEnabled(false);
        this.xuiRowStyle.setIgnoreStyling(true);
        this.xuiColumnStyle = this.addVar(new XuiScript.XuiString(this, "xuiColumnStyle"));
        this.xuiColumnStyle.setAutoApplyMode(XuiAutoApply.Forbidden);
        this.xuiColumnStyle.setScriptLoadEnabled(false);
        this.xuiColumnStyle.setIgnoreStyling(true);
    }

    public XuiScript.XuiString getCellStyle() {
        return this.xuiCellStyle;
    }

    public XuiScript.XuiString getRowStyle() {
        return this.xuiRowStyle;
    }

    public XuiScript.XuiString getColumnStyle() {
        return this.xuiColumnStyle;
    }

    public int getColumnCount() {
        return this.columns.size();
    }

    public int getRowCount() {
        return this.rows.size();
    }

    public XuiScript getColumn(int index) {
        if (index >= 0 && index < this.columns.size()) {
            return this.columns.get(index);
        }
        return null;
    }

    public XuiScript getRow(int index) {
        if (index >= 0 && index < this.rows.size()) {
            return this.rows.get(index);
        }
        return null;
    }

    public XuiScript getCell(int column, int row) {
        int index = column + row * this.columns.size();
        if (index >= 0 && index < this.cells.size()) {
            return this.cells.get(index);
        }
        return null;
    }

    private int readCellIndex(String s, int columnCount) {
        String[] split = s.split(":");
        if (split.length == 2) {
            int x = Integer.parseInt(split[0].trim());
            int y = Integer.parseInt(split[1].trim());
            return x + y * columnCount;
        }
        return -1;
    }

    private int countRowsOrColumns(ScriptParser.Block block) {
        int count = 0;
        for (ScriptParser.Value value : block.values) {
            String key = value.getKey().trim();
            String val = value.getValue().trim();
            if (key.isEmpty() || val.isEmpty() || !key.startsWith("[") || !key.contains("]")) continue;
            int index = this.getIndex(key);
            count = PZMath.max(index, count);
        }
        return count;
    }

    public <T extends XuiScript> void LoadColumnsRows(ScriptParser.Block block, ArrayList<T> list) {
        for (ScriptParser.Value value : block.values) {
            int index;
            String key = value.getKey().trim();
            String val = value.getValue().trim();
            if (key.isEmpty() || val.isEmpty() || !key.startsWith("[") || !key.contains("]") || (index = this.getIndex(key)) < 0 || index >= list.size()) continue;
            ((XuiScript)list.get(index)).loadVar(this.getPostIndexKey(key), val);
        }
    }

    private String getPostIndexKey(String s) {
        if (s.contains("]")) {
            int i = s.indexOf("]");
            return s.substring(i + 1).trim();
        }
        return s;
    }

    private int getIndex(String s) {
        if (s.startsWith("[") && s.contains("]")) {
            int i = s.indexOf("]");
            return Integer.parseInt(s.substring(1, i));
        }
        return -1;
    }

    @Override
    public void Load(ScriptParser.Block block) {
        int i;
        String val;
        String key;
        XuiScript cellStyle = null;
        XuiScript rowStyle = null;
        XuiScript columnStyle = null;
        if (this.isLayout()) {
            for (ScriptParser.Value value : block.values) {
                key = value.getKey().trim();
                val = value.getValue().trim();
                if (key.isEmpty() || val.isEmpty()) continue;
                if (this.xuiCellStyle.acceptsKey(key)) {
                    this.xuiCellStyle.fromString(val);
                    continue;
                }
                if (this.xuiRowStyle.acceptsKey(key)) {
                    this.xuiRowStyle.fromString(val);
                    continue;
                }
                if (!this.xuiColumnStyle.acceptsKey(key)) continue;
                this.xuiColumnStyle.fromString(val);
            }
            cellStyle = XuiManager.GetStyle((String)this.xuiCellStyle.value());
            rowStyle = XuiManager.GetStyle((String)this.xuiRowStyle.value());
            columnStyle = XuiManager.GetStyle((String)this.xuiColumnStyle.value());
        }
        super.Load(block);
        for (ScriptParser.Value value : block.values) {
            key = value.getKey().trim();
            val = value.getValue().trim();
            if (key.isEmpty() || val.isEmpty() || !this.isLayout() || !key.equalsIgnoreCase("xuiColumns") && !key.equalsIgnoreCase("xuiRows")) continue;
            String[] split = val.split(":");
            for (i = 0; i < split.length; ++i) {
                XuiScript script;
                String ss = split[i].trim();
                if (key.equalsIgnoreCase("xuiRows")) {
                    script = new XuiTableRowScript(this.xuiLayoutName, this.readAltKeys, rowStyle);
                    script.loadVar("height", ss);
                    this.rows.add((XuiTableRowScript)script);
                    continue;
                }
                script = new XuiTableColumnScript(this.xuiLayoutName, this.readAltKeys, columnStyle);
                script.loadVar("width", ss);
                this.columns.add((XuiTableColumnScript)script);
            }
        }
        for (ScriptParser.Block child : block.children) {
            if (!this.isLayout() || !child.type.equalsIgnoreCase("xuiColumns") && !child.type.equalsIgnoreCase("xuiRows")) continue;
            boolean isRow = child.type.equalsIgnoreCase("xuiRows");
            int count = this.countRowsOrColumns(child);
            int listCount = isRow ? this.rows.size() : this.columns.size();
            for (i = 0; i < count; ++i) {
                XuiScript script;
                if (i < listCount) continue;
                if (isRow) {
                    script = new XuiTableRowScript(this.xuiLayoutName, this.readAltKeys, rowStyle);
                    script.height.setValue(1.0f, true);
                    this.rows.add((XuiTableRowScript)script);
                    continue;
                }
                script = new XuiTableColumnScript(this.xuiLayoutName, this.readAltKeys, columnStyle);
                ((XuiTableColumnScript)script).width.setValue(1.0f, true);
                this.columns.add((XuiTableColumnScript)script);
            }
            if (isRow) {
                this.LoadColumnsRows(child, this.rows);
                continue;
            }
            this.LoadColumnsRows(child, this.columns);
        }
        if (this.isLayout() && !this.columns.isEmpty() && !this.rows.isEmpty()) {
            int cellCount = this.columns.size() * this.rows.size();
            for (int i2 = 0; i2 < cellCount; ++i2) {
                XuiTableCellScript script = new XuiTableCellScript(this.xuiLayoutName, this.readAltKeys, cellStyle);
                this.cells.add(script);
            }
            for (ScriptParser.Block child : block.children) {
                int index;
                if (!child.type.equalsIgnoreCase("xuiCell") || (index = this.readCellIndex(child.id, this.columns.size())) < 0 || index >= cellCount) continue;
                this.cells.get(index).Load(child);
                this.cells.get((int)index).cellHasLoaded = true;
            }
        } else if (!(!this.isLayout() || this.columns.isEmpty() && this.rows.isEmpty())) {
            this.warnWithInfo("XuiScript has only rows or columns.");
        }
    }

    @Override
    protected void postLoad() {
        super.postLoad();
        for (XuiTableRowScript xuiTableRowScript : this.rows) {
            xuiTableRowScript.postLoad();
        }
        for (XuiTableColumnScript xuiTableColumnScript : this.columns) {
            xuiTableColumnScript.postLoad();
        }
        for (XuiTableCellScript xuiTableCellScript : this.cells) {
            xuiTableCellScript.postLoad();
        }
    }

    @UsedFromLua
    public static class XuiTableRowScript
    extends XuiScript {
        public XuiTableRowScript(String xuiLayoutName, boolean readAltKeys, XuiScript style) {
            super(xuiLayoutName, readAltKeys, "ISXuiTableLayoutRow", XuiScriptType.Layout);
            if (style != null) {
                this.setStyle(style);
            }
            this.tryToSetDefaultStyle();
        }
    }

    @UsedFromLua
    public static class XuiTableColumnScript
    extends XuiScript {
        public XuiTableColumnScript(String xuiLayoutName, boolean readAltKeys, XuiScript style) {
            super(xuiLayoutName, readAltKeys, "ISXuiTableLayoutColumn", XuiScriptType.Layout);
            if (style != null) {
                this.setStyle(style);
            }
            this.tryToSetDefaultStyle();
        }
    }

    @UsedFromLua
    public static class XuiTableCellScript
    extends XuiScript {
        protected boolean cellHasLoaded;

        public XuiTableCellScript(String xuiLayoutName, boolean readAltKeys, XuiScript style) {
            super(xuiLayoutName, readAltKeys, "ISXuiTableLayoutCell", XuiScriptType.Layout);
            if (style != null) {
                this.setStyle(style);
            }
            this.tryToSetDefaultStyle();
        }

        public boolean isCellHasLoaded() {
            return this.cellHasLoaded;
        }
    }
}

