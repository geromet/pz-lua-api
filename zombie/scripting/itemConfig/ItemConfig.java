/*
 * Decompiled with CFR 0.152.
 */
package zombie.scripting.itemConfig;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import zombie.UsedFromLua;
import zombie.core.Core;
import zombie.core.logger.ExceptionLogger;
import zombie.debug.DebugLog;
import zombie.entity.GameEntity;
import zombie.entity.components.attributes.Attribute;
import zombie.entity.components.attributes.AttributeType;
import zombie.entity.components.attributes.AttributeUtil;
import zombie.entity.components.attributes.AttributeValueType;
import zombie.entity.components.fluids.Fluid;
import zombie.inventory.ItemConfigurator;
import zombie.inventory.ItemPickInfo;
import zombie.scripting.ScriptLoadMode;
import zombie.scripting.ScriptManager;
import zombie.scripting.ScriptParser;
import zombie.scripting.ScriptType;
import zombie.scripting.itemConfig.BucketRoot;
import zombie.scripting.itemConfig.RandomGenerator;
import zombie.scripting.itemConfig.Randomizer;
import zombie.scripting.itemConfig.SelectorBucket;
import zombie.scripting.itemConfig.VariableBuilder;
import zombie.scripting.itemConfig.enums.SelectorType;
import zombie.scripting.itemConfig.generators.GeneratorBoolAttribute;
import zombie.scripting.itemConfig.generators.GeneratorEnumAttribute;
import zombie.scripting.itemConfig.generators.GeneratorEnumSetAttribute;
import zombie.scripting.itemConfig.generators.GeneratorEnumStringSetAttribute;
import zombie.scripting.itemConfig.generators.GeneratorFluidContainer;
import zombie.scripting.itemConfig.generators.GeneratorLuaFunc;
import zombie.scripting.itemConfig.generators.GeneratorNumericAttribute;
import zombie.scripting.itemConfig.generators.GeneratorStringAttribute;
import zombie.scripting.itemConfig.script.BucketRootScript;
import zombie.scripting.itemConfig.script.SelectorBucketScript;
import zombie.scripting.objects.BaseScriptObject;
import zombie.util.StringUtils;
import zombie.util.list.PZArrayUtil;

@UsedFromLua
public class ItemConfig
extends BaseScriptObject {
    public static String errorLine;
    public static String errorBucket;
    public static String errorRoot;
    public static String errorItemConfig;
    public static final String VARIABLE_PREFIX = "$";
    private final ArrayList<String> includes = new ArrayList();
    private final HashMap<String, String> variables = new HashMap();
    private final HashMap<String, BucketRootScript> rootScripts = new HashMap();
    private final ArrayList<BucketRoot> roots = new ArrayList();
    private String name;
    private boolean hasBeenParsed;
    private boolean isValid = true;
    private static final ArrayList<RandomGenerator> tempGenerators;

    private static String createErrorString() {
        Object e = "[";
        e = (String)e + "itemConfig=" + (errorItemConfig != null ? errorItemConfig : "unknown") + ", ";
        if (errorBucket != null) {
            e = (String)e + "bucket=" + errorBucket + ", ";
        }
        if (errorRoot != null) {
            e = (String)e + "attribute=" + errorRoot + ", ";
        }
        e = (String)e + "line=\"" + (errorLine != null ? errorLine : "null") + "\"]";
        e = (String)e + "]";
        return e;
    }

    private static void WarnOrError(String s) throws ItemConfigException {
        if (Core.debug) {
            throw new ItemConfigException(s);
        }
        DebugLog.log("RecipeAttributes -> " + s + " \n" + ItemConfig.createErrorString());
    }

    public ItemConfig() {
        super(ScriptType.ItemConfig);
    }

    public String getName() {
        return this.name;
    }

    public boolean isValid() {
        return this.isValid;
    }

    public void ConfigureEntitySpawned(GameEntity entity, ItemPickInfo pickInfo) {
        if (this.roots.isEmpty() || !this.isValid) {
            return;
        }
        for (int i = 0; i < this.roots.size(); ++i) {
            BucketRoot root = this.roots.get(i);
            if (root.getBucketSpawn() == null) continue;
            root.getBucketSpawn().Resolve(entity, pickInfo);
        }
    }

    public void ConfigureEntityOnCreate(GameEntity entity) {
        if (this.roots.isEmpty() || !this.isValid) {
            return;
        }
        for (int i = 0; i < this.roots.size(); ++i) {
            BucketRoot root = this.roots.get(i);
            if (root.getBucketOnCreate() == null) continue;
            root.getBucketOnCreate().ResolveOnCreate(entity);
        }
    }

    @Override
    public void Load(String name, String totalFile) throws ItemConfigException {
        this.name = name;
        errorLine = null;
        errorBucket = null;
        errorRoot = null;
        errorItemConfig = this.name;
        try {
            ScriptParser.Block block = ScriptParser.parse(totalFile);
            block = block.children.get(0);
            this.LoadCommonBlock(block);
            for (ScriptParser.BlockElement element : block.elements) {
                String s;
                if (element.asValue() != null) {
                    String s2;
                    errorLine = s2 = element.asValue().string;
                    if (StringUtils.isNullOrWhitespace(s2)) continue;
                    if (s2.contains("=")) {
                        String[] split = s2.split("=");
                        String k = ((String)split[0]).trim();
                        String v = ((String)split[1]).trim();
                        if (k.equalsIgnoreCase("include")) {
                            this.includes.add(v);
                        }
                    }
                    errorLine = null;
                    continue;
                }
                ScriptParser.Block child = element.asBlock();
                if ("includes".equalsIgnoreCase(child.type)) {
                    for (ScriptParser.Value value : child.values) {
                        s = value.string;
                        if (StringUtils.isNullOrWhitespace(s)) continue;
                        this.includes.add(s.trim());
                    }
                    continue;
                }
                if ("variables".equalsIgnoreCase(child.type)) {
                    for (ScriptParser.Value value : child.values) {
                        s = value.string;
                        if (StringUtils.isNullOrWhitespace(s)) continue;
                        this.variables.put(value.getKey(), value.getValue());
                    }
                    continue;
                }
                BucketRootScript rootScript = BucketRootScript.TryLoad(child);
                if (rootScript == null) continue;
                Object rootId = rootScript.getType().toString();
                if (rootScript.getId() != null) {
                    rootId = (String)rootId + ":" + rootScript.getId();
                }
                this.rootScripts.put((String)rootId, rootScript);
            }
        }
        catch (Exception e) {
            if (!(e instanceof ItemConfigException)) {
                throw new ItemConfigException(e.getMessage(), e);
            }
            throw new ItemConfigException(e.getMessage(), e, false);
        }
        errorLine = null;
        errorBucket = null;
        errorRoot = null;
        errorItemConfig = null;
    }

    @Override
    public void PreReload() {
        this.hasBeenParsed = false;
        this.includes.clear();
        this.variables.clear();
        this.rootScripts.clear();
        this.roots.clear();
    }

    @Override
    public void OnScriptsLoaded(ScriptLoadMode loadMode) throws Exception {
        errorItemConfig = this.name;
        try {
            this.Parse(null);
        }
        catch (Exception e) {
            ExceptionLogger.logException(e);
            throw new Exception(e);
        }
        errorItemConfig = null;
    }

    private void Parse(HashSet<String> includeSet) throws ItemConfigException {
        if (includeSet != null) {
            if (!includeSet.contains(this.name)) {
                includeSet.add(this.name);
            } else {
                throw new ItemConfigException("Circular includes detected.");
            }
        }
        if (this.hasBeenParsed) {
            return;
        }
        HashMap<String, BucketRootScript> mergedIncludes = null;
        HashMap<String, String> mergedVariables = new HashMap<String, String>();
        if (this.includes != null) {
            for (String string : this.includes) {
                HashSet<String> set = includeSet != null ? includeSet : new HashSet<String>();
                set.add(this.name);
                ItemConfig parent = ScriptManager.instance.getItemConfig(string);
                if (!parent.hasBeenParsed) {
                    parent.Parse(set);
                }
                mergedIncludes = ItemConfig.MergeRoots(mergedIncludes, parent.rootScripts, true);
                mergedVariables.putAll(parent.variables);
            }
        }
        if (mergedIncludes != null) {
            ItemConfig.MergeRoots(mergedIncludes, this.rootScripts, true);
        }
        for (Map.Entry entry : mergedVariables.entrySet()) {
            if (this.variables.containsKey(entry.getKey())) continue;
            mergedVariables.put((String)entry.getKey(), (String)entry.getValue());
        }
        this.hasBeenParsed = true;
    }

    private static HashMap<String, BucketRootScript> MergeRoots(HashMap<String, BucketRootScript> bucket, HashMap<String, BucketRootScript> bucketAdd, boolean createCopyEntries) {
        HashMap<String, BucketRootScript> merged = new HashMap<String, BucketRootScript>();
        if (bucket == null && bucketAdd == null) {
            return merged;
        }
        if (bucket == null || bucketAdd == null) {
            HashMap<String, BucketRootScript> from = bucket != null ? bucket : bucketAdd;
            for (Map.Entry<String, BucketRootScript> entry : from.entrySet()) {
                merged.put(entry.getKey(), createCopyEntries ? entry.getValue().copy() : entry.getValue());
            }
        } else {
            for (Map.Entry<String, BucketRootScript> entry : bucket.entrySet()) {
                if (bucketAdd.containsKey(entry.getKey())) continue;
                merged.put(entry.getKey(), createCopyEntries ? entry.getValue().copy() : entry.getValue());
            }
            for (Map.Entry<String, BucketRootScript> entry : bucketAdd.entrySet()) {
                merged.put(entry.getKey(), createCopyEntries ? entry.getValue().copy() : entry.getValue());
            }
        }
        return merged;
    }

    public void BuildBuckets() {
        errorItemConfig = this.name;
        try {
            this.roots.clear();
            for (Map.Entry<String, BucketRootScript> entry : this.rootScripts.entrySet()) {
                BucketRoot bucketRoot = this.buildBucketRoot(entry.getValue());
                this.roots.add(bucketRoot);
            }
        }
        catch (Exception e) {
            DebugLog.log(e.getMessage());
            e.printStackTrace();
            this.isValid = false;
        }
        VariableBuilder.clear();
        errorItemConfig = null;
    }

    private BucketRoot buildBucketRoot(BucketRootScript script) throws ItemConfigException {
        VariableBuilder.setKeys(this.variables);
        if (script.idIsVariable()) {
            String id = VariableBuilder.Build(script.getId());
            script.setId(id);
        }
        BucketRoot root = new BucketRoot(script.getType(), script.getId());
        SelectorBucket spawn = this.buildBucket(script, script.getDefaultBucket());
        root.setBucketSpawn(spawn);
        if (script.getOnCreateBucket() != null) {
            SelectorBucket create = this.buildBucket(script, script.getOnCreateBucket());
            root.setBucketOnCreate(create);
        }
        return root;
    }

    private SelectorBucket buildBucket(BucketRootScript rootScript, SelectorBucketScript bucketScript) throws ItemConfigException {
        SelectorBucket[] children = null;
        if (!bucketScript.getChildren().isEmpty()) {
            children = new SelectorBucket[bucketScript.getChildren().size()];
            for (int i = 0; i < bucketScript.getChildren().size(); ++i) {
                children[i] = this.buildBucket(rootScript, bucketScript.getChildren().get(i));
            }
        }
        int[] selectorIDs = null;
        if (bucketScript.getSelectorString() != null) {
            int i;
            String[] ids;
            String selector = bucketScript.getSelectorString();
            if (selector.contains(VARIABLE_PREFIX)) {
                selector = VariableBuilder.Build(selector);
            }
            if (bucketScript.getSelectorType().isAllowChaining() && selector.contains("/")) {
                ids = selector.split("/");
                for (i = 0; i < ids.length; ++i) {
                    ids[i] = ids[i].trim();
                }
            } else {
                ids = new String[]{selector};
            }
            selectorIDs = new int[ids.length];
            for (i = 0; i < ids.length; ++i) {
                selectorIDs[i] = bucketScript.getSelectorType() == SelectorType.Tile ? ItemConfigurator.GetIdForSprite(ids[i]) : ItemConfigurator.GetIdForString(ids[i]);
                if (selectorIDs[i] != -1) continue;
                throw new ItemConfigException("Could not find selectorID for: " + ids[i] + ", in: " + bucketScript.getSelectorString());
            }
        }
        Randomizer randomizer = null;
        if (!bucketScript.getRandomizers().isEmpty()) {
            tempGenerators.clear();
            for (String rand : bucketScript.getRandomizers()) {
                String s = rand;
                if (StringUtils.isNullOrWhitespace(s)) continue;
                if (s.contains(VARIABLE_PREFIX)) {
                    s = VariableBuilder.Build(s);
                }
                if (StringUtils.isNullOrWhitespace(s)) continue;
                errorLine = s;
                RandomGenerator generator = null;
                switch (rootScript.getType()) {
                    case Attribute: {
                        AttributeType type = Attribute.TypeFromName(rootScript.getId());
                        if (type == null) {
                            throw new ItemConfigException("Invalid attribute! [itemConfig=" + this.name + ", attribute=" + String.valueOf(type != null ? type : "null") + ", attributeString = " + (rootScript.getId() != null ? rootScript.getId() : "null") + "]");
                        }
                        if (AttributeValueType.IsNumeric(type.getValueType())) {
                            generator = this.buildNumericGenerator(type, s);
                            break;
                        }
                        if (type.getValueType() == AttributeValueType.Boolean) {
                            generator = this.buildBoolGenerator(type, s);
                            break;
                        }
                        if (type.getValueType() == AttributeValueType.String) {
                            generator = this.buildStringGenerator(type, s);
                            break;
                        }
                        if (type.getValueType() == AttributeValueType.Enum) {
                            generator = this.buildEnumGenerator(type, s);
                            break;
                        }
                        if (type.getValueType() == AttributeValueType.EnumSet) {
                            generator = this.buildEnumSetGenerator(type, s);
                            break;
                        }
                        if (type.getValueType() != AttributeValueType.EnumStringSet) break;
                        generator = this.buildEnumStringSetGenerator(type, s);
                        break;
                    }
                    case FluidContainer: {
                        generator = this.buildFluidContainerGenerator(rootScript.getId(), s);
                        break;
                    }
                    case LuaFunc: {
                        generator = this.buildLuaFuncGenerator(s);
                    }
                }
                if (generator != null) {
                    tempGenerators.add(generator);
                }
                errorLine = null;
            }
            randomizer = new Randomizer(PZArrayUtil.toArray(tempGenerators));
        }
        return new SelectorBucket(selectorIDs, bucketScript, children, randomizer);
    }

    private RandomGenerator buildLuaFuncGenerator(String s) throws ItemConfigException {
        String[] params = s.split("\\s+");
        float chance = 1.0f;
        String val = null;
        for (String param : params) {
            String[] elems = param.split("=");
            String id = elems[0];
            if (id.equalsIgnoreCase("chance")) {
                chance = Float.parseFloat(elems[1]);
                continue;
            }
            if (!id.equalsIgnoreCase("func")) continue;
            val = elems[1];
        }
        if (val == null) {
            throw new ItemConfigException("At least parameter 'func' has to be defined.");
        }
        return new GeneratorLuaFunc(val, chance);
    }

    private RandomGenerator buildFluidContainerGenerator(String containerID, String s) throws ItemConfigException {
        String[] params = s.split("\\s+");
        float chance = 1.0f;
        float min = 0.0f;
        float max = 0.0f;
        boolean hasMax = false;
        ArrayList<Fluid> fluids = new ArrayList<Fluid>();
        ArrayList<Float> ratios = new ArrayList<Float>();
        float val = 0.0f;
        boolean hasVal = false;
        for (String param : params) {
            String[] elems = param.split("=");
            String id = elems[0];
            if (id.equalsIgnoreCase("chance")) {
                chance = Float.parseFloat(elems[1]);
                continue;
            }
            if (id.equalsIgnoreCase("min")) {
                min = Float.parseFloat(elems[1]);
                continue;
            }
            if (id.equalsIgnoreCase("max")) {
                hasMax = true;
                max = Float.parseFloat(elems[1]);
                continue;
            }
            if (id.equalsIgnoreCase("value")) {
                hasVal = true;
                val = Float.parseFloat(elems[1]);
                continue;
            }
            if (!id.equalsIgnoreCase("fluid")) continue;
            String[] fl = elems[1].split(":");
            Fluid fluid = Fluid.Get(fl[0]);
            float ratio = Float.parseFloat(fl[1]);
            if (fluid == null) {
                throw new ItemConfigException("Could not find fluid: '" + fl[0] + "'.");
            }
            fluids.add(fluid);
            ratios.add(Float.valueOf(ratio));
        }
        if (!hasMax && !hasVal) {
            throw new ItemConfigException("At least one of these parameters: 'max' or 'value', has to be defined.");
        }
        if (hasVal) {
            min = val;
            max = val;
        }
        if (!fluids.isEmpty()) {
            float[] ratiosArr = new float[ratios.size()];
            for (int i = 0; i < ratios.size(); ++i) {
                ratiosArr[i] = ((Float)ratios.get(i)).floatValue();
            }
            return new GeneratorFluidContainer(containerID, (Fluid[])PZArrayUtil.toArray(fluids), ratiosArr, chance, min, max);
        }
        return new GeneratorFluidContainer(containerID, null, null, chance, min, max);
    }

    private RandomGenerator buildNumericGenerator(AttributeType attributeType, String s) throws ItemConfigException {
        String[] params = s.split("\\s+");
        float chance = 1.0f;
        float min = 0.0f;
        float max = 0.0f;
        boolean hasMax = false;
        float val = 0.0f;
        boolean hasVal = false;
        for (String param : params) {
            String[] elems = param.split("=");
            String id = elems[0];
            if (id.equalsIgnoreCase("chance")) {
                chance = Float.parseFloat(elems[1]);
                continue;
            }
            if (id.equalsIgnoreCase("min")) {
                min = Float.parseFloat(elems[1]);
                continue;
            }
            if (id.equalsIgnoreCase("max")) {
                hasMax = true;
                max = Float.parseFloat(elems[1]);
                continue;
            }
            if (!id.equalsIgnoreCase("value")) continue;
            hasVal = true;
            val = Float.parseFloat(elems[1]);
        }
        if (!hasMax && !hasVal) {
            throw new ItemConfigException("At least one of these parameters: 'max' or 'value', has to be defined.");
        }
        if (hasVal) {
            min = val;
            max = val;
        }
        return new GeneratorNumericAttribute(attributeType, chance, min, max);
    }

    private RandomGenerator buildStringGenerator(AttributeType attributeType, String s) throws ItemConfigException {
        String[] params = s.split("\\s+");
        float chance = 1.0f;
        String val = null;
        for (String param : params) {
            String[] elems = param.split("=");
            String id = elems[0];
            if (id.equalsIgnoreCase("chance")) {
                chance = Float.parseFloat(elems[1]);
                continue;
            }
            if (!id.equalsIgnoreCase("value")) continue;
            val = elems[1];
        }
        if (val == null) {
            throw new ItemConfigException("At least parameter 'value' has to be defined.");
        }
        return new GeneratorStringAttribute(attributeType, chance, val);
    }

    private RandomGenerator buildBoolGenerator(AttributeType attributeType, String s) throws ItemConfigException {
        String[] params = s.split("\\s+");
        float chance = 1.0f;
        boolean hasVal = false;
        boolean val = false;
        for (String param : params) {
            String[] elems = param.split("=");
            String id = elems[0];
            if (id.equalsIgnoreCase("chance")) {
                chance = Float.parseFloat(elems[1]);
                continue;
            }
            if (!id.equalsIgnoreCase("value")) continue;
            val = Boolean.parseBoolean(elems[1]);
            hasVal = true;
        }
        if (!hasVal) {
            throw new ItemConfigException("At least parameter 'value' has to be defined.");
        }
        return new GeneratorBoolAttribute(attributeType, chance, val);
    }

    private RandomGenerator buildEnumGenerator(AttributeType attributeType, String s) throws ItemConfigException {
        String[] params = s.split("\\s+");
        float chance = 1.0f;
        String val = null;
        for (String param : params) {
            String[] elems = param.split("=");
            String id = elems[0];
            if (id.equalsIgnoreCase("chance")) {
                chance = Float.parseFloat(elems[1]);
                continue;
            }
            if (!id.equalsIgnoreCase("value")) continue;
            val = elems[1];
        }
        if (val == null) {
            throw new ItemConfigException("At least parameter 'value' has to be defined.");
        }
        return new GeneratorEnumAttribute(attributeType, chance, val);
    }

    private RandomGenerator buildEnumSetGenerator(AttributeType attributeType, String s) throws ItemConfigException {
        String[] params = s.split("\\s+");
        float chance = 1.0f;
        String val = null;
        GeneratorEnumSetAttribute.Mode mode = GeneratorEnumSetAttribute.Mode.Set;
        for (String param : params) {
            String[] elems = param.split("=");
            String id = elems[0];
            if (id.equalsIgnoreCase("chance")) {
                chance = Float.parseFloat(elems[1]);
                continue;
            }
            if (id.equalsIgnoreCase("value")) {
                val = elems[1];
                continue;
            }
            if (!id.equalsIgnoreCase("mode")) continue;
            if (elems[1].equalsIgnoreCase("add")) {
                mode = GeneratorEnumSetAttribute.Mode.Add;
                continue;
            }
            if (!elems[1].equalsIgnoreCase("remove")) continue;
            mode = GeneratorEnumSetAttribute.Mode.Remove;
        }
        if (val == null) {
            throw new ItemConfigException("At least parameter 'value' has to be defined.");
        }
        String[] values2 = val.contains(";") ? val.split(";") : new String[]{val};
        return new GeneratorEnumSetAttribute(attributeType, mode, chance, values2);
    }

    private RandomGenerator buildEnumStringSetGenerator(AttributeType attributeType, String s) throws ItemConfigException {
        String[] params = s.split("\\s+");
        float chance = 1.0f;
        String val = null;
        GeneratorEnumStringSetAttribute.Mode mode = GeneratorEnumStringSetAttribute.Mode.Set;
        for (String param : params) {
            String[] elems = param.split("=");
            String id = elems[0];
            if (id.equalsIgnoreCase("chance")) {
                chance = Float.parseFloat(elems[1]);
                continue;
            }
            if (id.equalsIgnoreCase("value")) {
                val = elems[1];
                continue;
            }
            if (!id.equalsIgnoreCase("mode")) continue;
            if (elems[1].equalsIgnoreCase("add")) {
                mode = GeneratorEnumStringSetAttribute.Mode.Add;
                continue;
            }
            if (!elems[1].equalsIgnoreCase("remove")) continue;
            mode = GeneratorEnumStringSetAttribute.Mode.Remove;
        }
        if (val == null) {
            throw new ItemConfigException("At least parameter 'value' has to be defined.");
        }
        String[] valuesEnum = null;
        String[] valuesString = null;
        ArrayList<String> enums = new ArrayList<String>();
        ArrayList<String> strings = new ArrayList<String>();
        if (val.contains(";")) {
            String[] split;
            for (String str : split = val.split(";")) {
                if (AttributeUtil.isEnumString(str)) {
                    enums.add(str);
                    continue;
                }
                strings.add(str);
            }
        } else if (AttributeUtil.isEnumString(val)) {
            enums.add(val);
        } else {
            strings.add(val);
        }
        if (!enums.isEmpty()) {
            valuesEnum = enums.toArray(new String[0]);
        }
        if (!strings.isEmpty()) {
            valuesString = strings.toArray(new String[0]);
        }
        return new GeneratorEnumStringSetAttribute(attributeType, mode, chance, valuesEnum, valuesString);
    }

    static {
        tempGenerators = new ArrayList();
    }

    public static class ItemConfigException
    extends Exception {
        public ItemConfigException(String errorMessage) {
            super("RecipeAttributes -> " + errorMessage + " \n" + ItemConfig.createErrorString());
        }

        public ItemConfigException(String errorMessage, Throwable err) {
            super("RecipeAttributes -> " + errorMessage + " \n" + ItemConfig.createErrorString(), err);
        }

        public ItemConfigException(String errorMessage, Throwable err, boolean doPrint) {
            super((String)(doPrint ? "RecipeAttributes -> " + errorMessage + " \n" + ItemConfig.createErrorString() : errorMessage), err);
        }
    }
}

