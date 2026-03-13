/*
 * Decompiled with CFR 0.152.
 */
package zombie.scripting.objects;

import java.util.ArrayList;
import java.util.Set;
import zombie.UsedFromLua;
import zombie.debug.DebugLog;
import zombie.debug.objects.DebugClassFields;
import zombie.inventory.InventoryItem;
import zombie.scripting.ScriptLoadMode;
import zombie.scripting.ScriptManager;
import zombie.scripting.ScriptParser;
import zombie.scripting.ScriptType;
import zombie.scripting.objects.BaseScriptObject;
import zombie.scripting.objects.Item;
import zombie.scripting.objects.ItemTag;
import zombie.scripting.objects.ItemType;
import zombie.scripting.objects.Registries;
import zombie.scripting.objects.ResourceLocation;

@DebugClassFields
@UsedFromLua
public class ItemFilterScript
extends BaseScriptObject {
    private final FilterTypeInfo whitelist = new FilterTypeInfo();
    private final FilterTypeInfo blacklist = new FilterTypeInfo();
    private boolean hasParsed;
    private String name;
    private final ArrayList<Item> tempScriptItems = new ArrayList();

    public ItemFilterScript() {
        super(ScriptType.ItemFilter);
    }

    public String getName() {
        return this.name;
    }

    @Override
    public void PreReload() {
        this.hasParsed = false;
        this.whitelist.reset();
        this.blacklist.reset();
    }

    @Override
    public void OnScriptsLoaded(ScriptLoadMode loadMode) {
    }

    @Override
    public void OnLoadedAfterLua() {
        this.parseFilter();
    }

    private void parseFilter() {
        if (!this.hasParsed) {
            this.resolveItemTypes(this.whitelist);
            this.resolveItemTypes(this.blacklist);
            this.whitelist.items.clear();
            this.whitelist.items.addAll(this.whitelist.loadedItems);
            if (!this.whitelist.items.isEmpty()) {
                ScriptManager.resolveGetItemTypes(this.whitelist.items, this.tempScriptItems);
            }
            this.blacklist.items.clear();
            this.blacklist.items.addAll(this.blacklist.loadedItems);
            if (!this.blacklist.items.isEmpty()) {
                ScriptManager.resolveGetItemTypes(this.blacklist.items, this.tempScriptItems);
            }
            this.hasParsed = true;
        } else {
            DebugLog.General.warn("Already parsed filter: " + this.name);
        }
    }

    private void resolveItemTypes(FilterTypeInfo info) {
        if (!info.loadedTypes.isEmpty()) {
            for (String s : info.loadedTypes) {
                ItemType itemType = ItemType.get(ResourceLocation.of(s));
                if (info.itemTypes.contains(itemType)) continue;
                info.itemTypes.add(itemType);
            }
        }
    }

    @Override
    public void OnPostWorldDictionaryInit() {
    }

    public boolean allowsItem(InventoryItem item) {
        if (this.blacklist.containsItem(item)) {
            return false;
        }
        return !this.whitelist.hasEntries() || this.whitelist.containsItem(item);
    }

    public boolean allowsItem(Item item) {
        if (this.blacklist.containsItem(item)) {
            return false;
        }
        return !this.whitelist.hasEntries() || this.whitelist.containsItem(item);
    }

    @Override
    public void Load(String name, String totalFile) throws Exception {
        ScriptParser.Block block = ScriptParser.parse(totalFile);
        block = block.children.get(0);
        this.name = name;
        this.LoadCommonBlock(block);
        this.readBlock(block, this.whitelist);
    }

    private void readBlock(ScriptParser.Block block, FilterTypeInfo info) {
        for (ScriptParser.Value value : block.values) {
            String key = value.getKey().trim();
            String val = value.getValue().trim();
            if (key.isEmpty() || val.isEmpty()) continue;
            if (key.equalsIgnoreCase("items")) {
                this.parseInputString(info.loadedItems, val);
                continue;
            }
            if (key.equalsIgnoreCase("types")) {
                this.parseInputString(info.loadedTypes, val);
                continue;
            }
            if (!key.equalsIgnoreCase("tags")) continue;
            this.parseInputString(info.tags, val);
        }
        for (ScriptParser.Block child : block.children) {
            if ("items".equalsIgnoreCase(child.type)) {
                this.readFilterBlock(child, info.loadedItems);
                continue;
            }
            if ("types".equalsIgnoreCase(child.type)) {
                this.readFilterBlock(child, info.loadedTypes);
                continue;
            }
            if ("tags".equalsIgnoreCase(child.type)) {
                this.readFilterBlock(child, info.tags);
                continue;
            }
            if (!"blacklist".equalsIgnoreCase(child.type)) continue;
            this.readBlock(child, this.blacklist);
        }
    }

    private void readFilterBlock(ScriptParser.Block block, ArrayList<String> list) {
        for (ScriptParser.Value value : block.values) {
            String s;
            if (value.string == null || value.string.trim().isEmpty() || (s = value.string.trim()).contains("=")) continue;
            this.parseInputString(list, s);
        }
    }

    private void parseInputString(ArrayList<String> list, String input) {
        String[] split;
        for (String s : split = input.split("/")) {
            if (list.contains(s = s.trim())) continue;
            list.add(s);
        }
    }

    @DebugClassFields
    private static class FilterTypeInfo {
        private final ArrayList<String> loadedItems = new ArrayList();
        private final ArrayList<String> loadedTypes = new ArrayList();
        private final ArrayList<String> items = new ArrayList();
        private final ArrayList<ItemType> itemTypes = new ArrayList();
        private final ArrayList<String> tags = new ArrayList();

        private FilterTypeInfo() {
        }

        private void reset() {
            this.loadedItems.clear();
            this.loadedTypes.clear();
            this.items.clear();
            this.itemTypes.clear();
            this.tags.clear();
        }

        private boolean hasEntries() {
            return !this.items.isEmpty() || !this.itemTypes.isEmpty() || !this.tags.isEmpty();
        }

        private boolean containsItem(InventoryItem item) {
            if (item == null) {
                return false;
            }
            if (item.getScriptItem() != null) {
                return this.containsItem(item.getFullType(), item.getScriptItem().getItemType(), item.getTags());
            }
            return this.containsItem(item.getFullType(), ItemType.NORMAL, item.getTags());
        }

        private boolean containsItem(Item item) {
            if (item == null) {
                return false;
            }
            return this.containsItem(item.getFullName(), item.getItemType(), item.getTags());
        }

        private boolean containsItem(String itemFullType, ItemType itemType, Set<ItemTag> itemTags) {
            if (itemFullType == null || !this.hasEntries()) {
                return false;
            }
            if (!this.items.isEmpty() && this.items.contains(itemFullType)) {
                return true;
            }
            if (!this.itemTypes.isEmpty() && this.itemTypes.contains(itemType)) {
                return true;
            }
            if (!this.tags.isEmpty() && itemTags != null && !itemTags.isEmpty()) {
                for (ItemTag t : itemTags) {
                    String path = Registries.ITEM_TAG.getLocation(t).getPath();
                    if (!this.tags.contains(path)) continue;
                    return true;
                }
                return false;
            }
            return false;
        }
    }
}

