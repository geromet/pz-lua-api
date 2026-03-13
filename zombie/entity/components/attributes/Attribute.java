/*
 * Decompiled with CFR 0.152.
 */
package zombie.entity.components.attributes;

import java.util.ArrayList;
import java.util.HashMap;
import zombie.UsedFromLua;
import zombie.entity.components.attributes.AttributeType;
import zombie.entity.components.attributes.TestEnum;

@UsedFromLua
public abstract class Attribute {
    private static final HashMap<String, AttributeType> attributeTypeNameMap = new HashMap();
    private static final HashMap<Short, AttributeType> attributeTypeIdMap = new HashMap();
    private static final ArrayList<AttributeType> attributeTypes = new ArrayList();
    public static final AttributeType.Float TestQuality;
    public static final AttributeType.Int TestUses;
    public static final AttributeType.Float TestCondition;
    public static final AttributeType.Bool TestBool;
    public static final AttributeType.String TestString;
    public static final AttributeType.String TestString2;
    public static final AttributeType.Enum<TestEnum> TestItemType;
    public static final AttributeType.EnumSet<TestEnum> TestCategories;
    public static final AttributeType.EnumStringSet<TestEnum> TestTags;
    public static final AttributeType.Float Sharpness;
    public static final AttributeType.Int HeadCondition;
    public static final AttributeType.Int HeadConditionMax;
    public static final AttributeType.Int TimesHeadRepaired;
    public static final AttributeType.Int Quality;
    public static final AttributeType.Int OriginX;
    public static final AttributeType.Int OriginY;
    public static final AttributeType.Int OriginZ;

    private static <E extends AttributeType> E registerType(E type) {
        if (attributeTypeNameMap.containsKey(type.getName().toLowerCase())) {
            throw new RuntimeException("Attribute name registered twice id = '" + type.id() + ", attribute = '" + type.getName() + "'");
        }
        if (attributeTypeIdMap.containsKey(type.id())) {
            throw new RuntimeException("Attribute id registered twice id = '" + type.id() + ", attribute = '" + type.getName() + "'");
        }
        attributeTypeIdMap.put(type.id(), type);
        attributeTypeNameMap.put(type.getName().toLowerCase(), type);
        attributeTypes.add(type);
        return type;
    }

    public static AttributeType TypeFromName(String name) {
        return attributeTypeNameMap.get(name.toLowerCase());
    }

    public static AttributeType TypeFromId(short value) {
        return attributeTypeIdMap.get(value);
    }

    public static ArrayList<AttributeType> GetAllTypes() {
        return attributeTypes;
    }

    public static void init() {
    }

    static {
        String initVal = "Test string for attribute.";
        TestString = Attribute.registerType(new AttributeType.String(100, "TestString", "Test string for attribute."));
        TestString2 = Attribute.registerType(new AttributeType.String(102, "TestString2", ""));
        TestQuality = Attribute.registerType(new AttributeType.Float(103, "TestQuality", 0.0f));
        TestQuality.setBounds(Float.valueOf(0.0f), Float.valueOf(100.0f));
        TestCondition = Attribute.registerType(new AttributeType.Float(104, "TestCondition", 0.0f));
        TestCondition.setBounds(Float.valueOf(0.0f), Float.valueOf(1.0f));
        TestBool = Attribute.registerType(new AttributeType.Bool(105, "TestBool", false));
        TestUses = Attribute.registerType(new AttributeType.Int(106, "TestUses", 5));
        TestItemType = Attribute.registerType(new AttributeType.Enum<TestEnum>(121, "TestItemType", TestEnum.TestValueA));
        TestCategories = Attribute.registerType(new AttributeType.EnumSet<TestEnum>(123, "TestCategories", TestEnum.class));
        TestCategories.getInitialValue().add(TestEnum.TestValueC);
        TestTags = Attribute.registerType(new AttributeType.EnumStringSet<TestEnum>(124, "TestTags", TestEnum.class));
        Sharpness = Attribute.registerType(new AttributeType.Float(0, "Sharpness", 1.0f, false, UI.Display.Hidden, UI.DisplayAsBar.Never, ""));
        Sharpness.setBounds(Float.valueOf(0.0f), Float.valueOf(1.0f));
        HeadCondition = Attribute.registerType(new AttributeType.Int(1, "HeadCondition", 10, false, UI.Display.Hidden, UI.DisplayAsBar.Never, ""));
        HeadCondition.setBounds(0, 1000);
        HeadConditionMax = Attribute.registerType(new AttributeType.Int(2, "HeadConditionMax", 10, false, UI.Display.Hidden, UI.DisplayAsBar.Never, ""));
        HeadConditionMax.setBounds(0, 1000);
        Quality = Attribute.registerType(new AttributeType.Int(3, "Quality", 50, false, UI.Display.Hidden, UI.DisplayAsBar.Never, ""));
        Quality.setBounds(0, 100);
        TimesHeadRepaired = Attribute.registerType(new AttributeType.Int(4, "TimesHeadRepaired", 0, false, UI.Display.Hidden, UI.DisplayAsBar.Never, ""));
        TimesHeadRepaired.setBounds(0, 1000);
        OriginX = Attribute.registerType(new AttributeType.Int(5, "OriginX", 0, false, UI.Display.Hidden, UI.DisplayAsBar.Never, ""));
        OriginY = Attribute.registerType(new AttributeType.Int(6, "OriginY", 0, false, UI.Display.Hidden, UI.DisplayAsBar.Never, ""));
        OriginZ = Attribute.registerType(new AttributeType.Int(7, "OriginZ", 0, false, UI.Display.Hidden, UI.DisplayAsBar.Never, ""));
    }

    public static final class UI {

        public static enum DisplayAsBar {
            Default,
            ForceIfBounds,
            Never;

        }

        public static enum Display {
            Visible,
            Hidden;

        }
    }
}

