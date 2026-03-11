/*
 * Decompiled with CFR 0.152.
 */
package zombie.entity.components.attributes;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.function.BiConsumer;
import zombie.UsedFromLua;
import zombie.core.Color;
import zombie.core.Colors;
import zombie.core.network.ByteBufferReader;
import zombie.debug.DebugLog;
import zombie.debug.DebugOptions;
import zombie.debug.objects.DebugClassFields;
import zombie.entity.Component;
import zombie.entity.ComponentType;
import zombie.entity.components.attributes.Attribute;
import zombie.entity.components.attributes.AttributeFactory;
import zombie.entity.components.attributes.AttributeInstance;
import zombie.entity.components.attributes.AttributeType;
import zombie.entity.components.attributes.EnumStringObj;
import zombie.entity.network.EntityPacketType;
import zombie.entity.util.assoc.AssocArray;
import zombie.iso.IsoObject;
import zombie.network.IConnection;
import zombie.scripting.entity.ComponentScript;
import zombie.scripting.entity.components.attributes.AttributesScript;
import zombie.ui.ObjectTooltip;

@DebugClassFields
@UsedFromLua
public class AttributeContainer
extends Component {
    private short maxAttributeId = (short)-1;
    private final AssocArray<AttributeType, AttributeInstance<?, ?>> attributes = new AssocArray();
    public static final short STORAGE_SIZE = 64;
    private static final byte SAVE_EMPTY = 0;
    private static final byte SAVE_COMPRESSED = 1;
    private static final byte SAVE_UNCOMPRESSED_8 = 8;
    private static final byte SAVE_UNCOMPRESSED_16 = 16;

    private AttributeContainer() {
        super(ComponentType.Attributes);
    }

    protected void readFromScript(ComponentScript componentScript) {
        super.readFromScript(componentScript);
        AttributesScript attributesScript = (AttributesScript)componentScript;
        if (attributesScript.getTemplateContainer() != null) {
            AttributeContainer.Copy(attributesScript.getTemplateContainer(), this);
        } else {
            DebugLog.General.error("Unable to create AttributeContainer from script: " + componentScript.getName());
        }
    }

    @Override
    public String toString() {
        boolean ownerIso = this.owner != null && this.owner instanceof IsoObject;
        Object attributesStr = "";
        for (int i = 0; i < this.attributes.size(); ++i) {
            attributesStr = (String)attributesStr + this.attributes.getKey(i).toString() + ";";
        }
        return "AttributeContainer [owner = " + (this.owner != null ? this.owner.toString() : "null") + ", iso = " + ownerIso + ", attributes = " + (String)attributesStr + "]";
    }

    public int size() {
        return this.attributes.size();
    }

    public void forEach(BiConsumer<AttributeType, AttributeInstance> action) {
        this.attributes.forEach(action);
    }

    public boolean contains(AttributeType type) {
        return this.attributes.containsKey(type);
    }

    public void remove(AttributeType type) {
        this.removeAndRelease(type);
    }

    private AttributeInstance removeAndRelease(AttributeType type) {
        AttributeInstance<?, ?> attribute = this.attributes.remove(type);
        if (attribute != null) {
            attribute.release();
        }
        return attribute;
    }

    protected AttributeInstance<?, ?> getOrAdd(AttributeType type) {
        AttributeInstance attribute = this.attributes.get(type);
        if (attribute == null) {
            attribute = AttributeFactory.Create(type);
            this.attributes.put(type, attribute);
            if (((AttributeType)attribute.getType()).id() > this.maxAttributeId) {
                this.maxAttributeId = ((AttributeType)attribute.getType()).id();
            }
        }
        return attribute;
    }

    public boolean add(AttributeType type) {
        if (!this.contains(type)) {
            AttributeInstance attribute = AttributeFactory.Create(type);
            this.attributes.put(type, attribute);
            if (((AttributeType)attribute.getType()).id() > this.maxAttributeId) {
                this.maxAttributeId = ((AttributeType)attribute.getType()).id();
            }
        }
        return false;
    }

    public final boolean putFromScript(AttributeType type, String scriptVal) {
        AttributeInstance<?, ?> attribute = this.getOrAdd(type);
        return attribute.setValueFromScriptString(scriptVal);
    }

    public final <E extends Enum<E>> void put(AttributeType.Enum<E> type, E value) {
        AttributeInstance<?, ?> attribute = this.getOrAdd(type);
        ((AttributeInstance.Enum)attribute).setValue(value);
    }

    public final <E extends Enum<E>> void set(AttributeType.Enum<E> type, E value) {
        AttributeInstance<?, ?> attribute = this.attributes.get(type);
        if (attribute == null) {
            throw new UnsupportedOperationException("Container does not contain attribute '" + String.valueOf(type) + "'.");
        }
        ((AttributeInstance.Enum)attribute).setValue(value);
    }

    public final <E extends Enum<E>> E get(AttributeType.Enum<E> type) {
        AttributeInstance<?, ?> attribute = this.attributes.get(type);
        if (attribute == null) {
            throw new UnsupportedOperationException("Container does not contain attribute '" + String.valueOf(type) + "'.");
        }
        return ((AttributeInstance.Enum)attribute).getValue();
    }

    public final <E extends Enum<E>> E get(AttributeType.Enum<E> type, E defaultTo) {
        AttributeInstance<?, ?> attribute = this.attributes.get(type);
        if (attribute != null) {
            return ((AttributeInstance.Enum)attribute).getValue();
        }
        return defaultTo;
    }

    public final <E extends Enum<E>> void put(AttributeType.EnumSet<E> type, EnumSet<E> value) {
        AttributeInstance<?, ?> attribute = this.getOrAdd(type);
        ((AttributeInstance.EnumSet)attribute).setValue(value);
    }

    public final <E extends Enum<E>> void set(AttributeType.EnumSet<E> type, EnumSet<E> value) {
        AttributeInstance<?, ?> attribute = this.attributes.get(type);
        if (attribute == null) {
            throw new UnsupportedOperationException("Container does not contain attribute '" + String.valueOf(type) + "'.");
        }
        ((AttributeInstance.EnumSet)attribute).setValue(value);
    }

    public final <E extends Enum<E>> EnumSet<E> get(AttributeType.EnumSet<E> type) {
        AttributeInstance<?, ?> attribute = this.attributes.get(type);
        if (attribute == null) {
            throw new UnsupportedOperationException("Container does not contain attribute '" + String.valueOf(type) + "'.");
        }
        return ((AttributeInstance.EnumSet)attribute).getValue();
    }

    public final <E extends Enum<E>> void put(AttributeType.EnumStringSet<E> type, EnumStringObj<E> value) {
        AttributeInstance<?, ?> attribute = this.getOrAdd(type);
        ((AttributeInstance.EnumStringSet)attribute).setValue(value);
    }

    public final <E extends Enum<E>> void set(AttributeType.EnumStringSet<E> type, EnumStringObj<E> value) {
        AttributeInstance<?, ?> attribute = this.attributes.get(type);
        if (attribute == null) {
            throw new UnsupportedOperationException("Container does not contain attribute '" + String.valueOf(type) + "'.");
        }
        ((AttributeInstance.EnumStringSet)attribute).setValue(value);
    }

    public final <E extends Enum<E>> EnumStringObj<E> get(AttributeType.EnumStringSet<E> type) {
        AttributeInstance<?, ?> attribute = this.attributes.get(type);
        if (attribute == null) {
            throw new UnsupportedOperationException("Container does not contain attribute '" + String.valueOf(type) + "'.");
        }
        return ((AttributeInstance.EnumStringSet)attribute).getValue();
    }

    public final void put(AttributeType.String type, String value) {
        AttributeInstance<?, ?> attribute = this.getOrAdd(type);
        ((AttributeInstance.String)attribute).setValue(value);
    }

    public final void set(AttributeType.String type, String value) {
        AttributeInstance<?, ?> attribute = this.attributes.get(type);
        if (attribute == null) {
            throw new UnsupportedOperationException("Container does not contain attribute '" + String.valueOf(type) + "'.");
        }
        ((AttributeInstance.String)attribute).setValue(value);
    }

    public final String get(AttributeType.String type) {
        AttributeInstance<?, ?> attribute = this.attributes.get(type);
        if (attribute == null) {
            throw new UnsupportedOperationException("Container does not contain attribute '" + String.valueOf(type) + "'.");
        }
        return ((AttributeInstance.String)attribute).getValue();
    }

    public final String get(AttributeType.String type, String defaultTo) {
        AttributeInstance<?, ?> attribute = this.attributes.get(type);
        if (attribute != null) {
            return ((AttributeInstance.String)attribute).getValue();
        }
        return defaultTo;
    }

    public final void put(AttributeType.Bool type, boolean value) {
        AttributeInstance<?, ?> attribute = this.getOrAdd(type);
        ((AttributeInstance.Bool)attribute).setValue(value);
    }

    public final void set(AttributeType.Bool type, boolean value) {
        AttributeInstance<?, ?> attribute = this.attributes.get(type);
        if (attribute == null) {
            throw new UnsupportedOperationException("Container does not contain attribute '" + String.valueOf(type) + "'.");
        }
        ((AttributeInstance.Bool)attribute).setValue(value);
    }

    public final boolean get(AttributeType.Bool type) {
        AttributeInstance<?, ?> attribute = this.attributes.get(type);
        if (attribute == null) {
            throw new UnsupportedOperationException("Container does not contain attribute '" + String.valueOf(type) + "'.");
        }
        return ((AttributeInstance.Bool)attribute).getValue();
    }

    public final boolean get(AttributeType.Bool type, boolean defaultTo) {
        AttributeInstance<?, ?> attribute = this.attributes.get(type);
        if (attribute != null) {
            return ((AttributeInstance.Bool)attribute).getValue();
        }
        return defaultTo;
    }

    public final void putFloatValue(AttributeType.Numeric type, float value) {
        AttributeInstance<?, ?> attribute = this.getOrAdd(type);
        if (attribute == null) {
            throw new UnsupportedOperationException("Container does not contain attribute '" + String.valueOf(type) + "'.");
        }
        ((AttributeInstance.Numeric)attribute).fromFloat(value);
    }

    public final void setFloatValue(AttributeType.Numeric type, float value) {
        AttributeInstance<?, ?> attribute = this.attributes.get(type);
        if (attribute == null) {
            throw new UnsupportedOperationException("Container does not contain attribute '" + String.valueOf(type) + "'.");
        }
        ((AttributeInstance.Numeric)attribute).fromFloat(value);
    }

    public final float getFloatValue(AttributeType.Numeric type) {
        AttributeInstance<?, ?> attribute = this.attributes.get(type);
        if (attribute == null) {
            throw new UnsupportedOperationException("Container does not contain attribute '" + String.valueOf(type) + "'.");
        }
        return ((AttributeInstance.Numeric)attribute).floatValue();
    }

    public final float getFloatValue(AttributeType.Numeric type, float defaultTo) {
        AttributeInstance<?, ?> attribute = this.attributes.get(type);
        if (attribute != null) {
            return ((AttributeInstance.Numeric)attribute).floatValue();
        }
        return defaultTo;
    }

    public final void put(AttributeType.Float type, float value) {
        AttributeInstance<?, ?> attribute = this.getOrAdd(type);
        ((AttributeInstance.Float)attribute).setValue(value);
    }

    public final void set(AttributeType.Float type, float value) {
        AttributeInstance<?, ?> attribute = this.attributes.get(type);
        if (attribute == null) {
            throw new UnsupportedOperationException("Container does not contain attribute '" + String.valueOf(type) + "'.");
        }
        ((AttributeInstance.Float)attribute).setValue(value);
    }

    public final float get(AttributeType.Float type) {
        AttributeInstance<?, ?> attribute = this.attributes.get(type);
        if (attribute == null) {
            throw new UnsupportedOperationException("Container does not contain attribute '" + String.valueOf(type) + "'.");
        }
        return ((AttributeInstance.Float)attribute).getValue();
    }

    public final float get(AttributeType.Float type, float defaultTo) {
        AttributeInstance<?, ?> attribute = this.attributes.get(type);
        if (attribute != null) {
            return ((AttributeInstance.Float)attribute).getValue();
        }
        return defaultTo;
    }

    public final void put(AttributeType.Double type, double value) {
        AttributeInstance<?, ?> attribute = this.getOrAdd(type);
        ((AttributeInstance.Double)attribute).setValue(value);
    }

    public final void set(AttributeType.Double type, double value) {
        AttributeInstance<?, ?> attribute = this.attributes.get(type);
        if (attribute == null) {
            throw new UnsupportedOperationException("Container does not contain attribute '" + String.valueOf(type) + "'.");
        }
        ((AttributeInstance.Double)attribute).setValue(value);
    }

    public final double get(AttributeType.Double type) {
        AttributeInstance<?, ?> attribute = this.attributes.get(type);
        if (attribute == null) {
            throw new UnsupportedOperationException("Container does not contain attribute '" + String.valueOf(type) + "'.");
        }
        return ((AttributeInstance.Double)attribute).getValue();
    }

    public final double get(AttributeType.Double type, double defaultTo) {
        AttributeInstance<?, ?> attribute = this.attributes.get(type);
        if (attribute != null) {
            return ((AttributeInstance.Double)attribute).getValue();
        }
        return defaultTo;
    }

    public final void put(AttributeType.Byte type, byte value) {
        AttributeInstance<?, ?> attribute = this.getOrAdd(type);
        ((AttributeInstance.Byte)attribute).setValue(value);
    }

    public final void set(AttributeType.Byte type, byte value) {
        AttributeInstance<?, ?> attribute = this.attributes.get(type);
        if (attribute == null) {
            throw new UnsupportedOperationException("Container does not contain attribute '" + String.valueOf(type) + "'.");
        }
        ((AttributeInstance.Byte)attribute).setValue(value);
    }

    public final byte get(AttributeType.Byte type) {
        AttributeInstance<?, ?> attribute = this.attributes.get(type);
        if (attribute == null) {
            throw new UnsupportedOperationException("Container does not contain attribute '" + String.valueOf(type) + "'.");
        }
        return ((AttributeInstance.Byte)attribute).getValue();
    }

    public final byte get(AttributeType.Byte type, byte defaultTo) {
        AttributeInstance<?, ?> attribute = this.attributes.get(type);
        if (attribute != null) {
            return ((AttributeInstance.Byte)attribute).getValue();
        }
        return defaultTo;
    }

    public final void put(AttributeType.Short type, short value) {
        AttributeInstance<?, ?> attribute = this.getOrAdd(type);
        ((AttributeInstance.Short)attribute).setValue(value);
    }

    public final void set(AttributeType.Short type, short value) {
        AttributeInstance<?, ?> attribute = this.attributes.get(type);
        if (attribute == null) {
            throw new UnsupportedOperationException("Container does not contain attribute '" + String.valueOf(type) + "'.");
        }
        ((AttributeInstance.Short)attribute).setValue(value);
    }

    public final short get(AttributeType.Short type) {
        AttributeInstance<?, ?> attribute = this.attributes.get(type);
        if (attribute == null) {
            throw new UnsupportedOperationException("Container does not contain attribute '" + String.valueOf(type) + "'.");
        }
        return ((AttributeInstance.Short)attribute).getValue();
    }

    public final short get(AttributeType.Short type, short defaultTo) {
        AttributeInstance<?, ?> attribute = this.attributes.get(type);
        if (attribute != null) {
            return ((AttributeInstance.Short)attribute).getValue();
        }
        return defaultTo;
    }

    public final void put(AttributeType.Int type, int value) {
        AttributeInstance<?, ?> attribute = this.getOrAdd(type);
        ((AttributeInstance.Int)attribute).setValue(value);
    }

    public final void set(AttributeType.Int type, int value) {
        AttributeInstance<?, ?> attribute = this.attributes.get(type);
        if (attribute == null) {
            throw new UnsupportedOperationException("Container does not contain attribute '" + String.valueOf(type) + "'.");
        }
        ((AttributeInstance.Int)attribute).setValue(value);
    }

    public final int get(AttributeType.Int type) {
        AttributeInstance<?, ?> attribute = this.attributes.get(type);
        if (attribute == null) {
            throw new UnsupportedOperationException("Container does not contain attribute '" + String.valueOf(type) + "'.");
        }
        return ((AttributeInstance.Int)attribute).getValue();
    }

    public final int get(AttributeType.Int type, int defaultTo) {
        AttributeInstance<?, ?> attribute = this.attributes.get(type);
        if (attribute != null) {
            return ((AttributeInstance.Int)attribute).getValue();
        }
        return defaultTo;
    }

    public final void put(AttributeType.Long type, long value) {
        AttributeInstance<?, ?> attribute = this.getOrAdd(type);
        ((AttributeInstance.Long)attribute).setValue(value);
    }

    public final void set(AttributeType.Long type, long value) {
        AttributeInstance<?, ?> attribute = this.attributes.get(type);
        if (attribute == null) {
            throw new UnsupportedOperationException("Container does not contain attribute '" + String.valueOf(type) + "'.");
        }
        ((AttributeInstance.Long)attribute).setValue(value);
    }

    public final long get(AttributeType.Long type) {
        AttributeInstance<?, ?> attribute = this.attributes.get(type);
        if (attribute == null) {
            throw new UnsupportedOperationException("Container does not contain attribute '" + String.valueOf(type) + "'.");
        }
        return ((AttributeInstance.Long)attribute).getValue();
    }

    public final long get(AttributeType.Long type, long defaultTo) {
        AttributeInstance<?, ?> attribute = this.attributes.get(type);
        if (attribute == null) {
            return ((AttributeInstance.Long)attribute).getValue();
        }
        return defaultTo;
    }

    public AttributeType getKey(int index) {
        return this.attributes.getKey(index);
    }

    public AttributeInstance getAttribute(int index) {
        return this.attributes.getValue(index);
    }

    public AttributeInstance getAttribute(AttributeType type) {
        return this.attributes.get(type);
    }

    private void recalculateMaxId() {
        this.maxAttributeId = (short)-1;
        if (this.attributes.size() <= 0) {
            return;
        }
        for (int i = 0; i < this.attributes.size(); ++i) {
            AttributeType type = this.attributes.getKey(i);
            if (type.id() <= this.maxAttributeId) continue;
            this.maxAttributeId = type.id();
        }
    }

    @Override
    protected void reset() {
        super.reset();
        this.clear();
    }

    public void clear() {
        if (!this.attributes.isEmpty()) {
            for (int i = 0; i < this.attributes.size(); ++i) {
                this.attributes.getValue(i).release();
            }
        }
        this.attributes.clear();
        this.maxAttributeId = (short)-1;
    }

    public static void Copy(AttributeContainer source2, AttributeContainer target) {
        target.clear();
        for (int i = 0; i < source2.attributes.size(); ++i) {
            AttributeInstance<?, ?> attribute = source2.attributes.getValue(i);
            target.attributes.put((AttributeType)attribute.getType(), (AttributeInstance<?, ?>)attribute.copy());
        }
        target.maxAttributeId = source2.maxAttributeId;
    }

    public static void Merge(AttributeContainer source2, AttributeContainer target) {
        for (int i = 0; i < source2.attributes.size(); ++i) {
            AttributeInstance<?, ?> attribute = source2.attributes.getValue(i);
            if (target.attributes.containsKey((AttributeType)attribute.getType())) {
                AttributeInstance<?, ?> targAttribute = target.attributes.remove((AttributeType)attribute.getType());
                targAttribute.release();
            }
            target.attributes.put((AttributeType)attribute.getType(), (AttributeInstance<?, ?>)attribute.copy());
        }
        target.maxAttributeId = (short)Math.max(target.maxAttributeId, source2.maxAttributeId);
    }

    public AttributeContainer copy() {
        AttributeContainer other = (AttributeContainer)ComponentType.Attributes.CreateComponent();
        AttributeContainer.Copy(this, other);
        return other;
    }

    public boolean isIdenticalTo(AttributeContainer other) {
        if (this.size() == 0 && other.size() == 0) {
            return true;
        }
        if (this.size() != other.size()) {
            return false;
        }
        for (int i = 0; i < this.attributes.size(); ++i) {
            AttributeInstance<?, ?> attribute = this.attributes.getValue(i);
            AttributeInstance<?, ?> attributeOther = other.attributes.get((AttributeType)attribute.getType());
            if (attributeOther != null && attribute.equalTo(attributeOther)) continue;
            return false;
        }
        return true;
    }

    @Override
    protected boolean onReceivePacket(ByteBufferReader input, EntityPacketType type, IConnection senderConnection) throws IOException {
        switch (type) {
            default: 
        }
        return false;
    }

    @Override
    protected void saveSyncData(ByteBuffer output) throws IOException {
        this.save(output);
    }

    @Override
    protected void loadSyncData(ByteBuffer input) throws IOException {
        this.load(input, 244);
    }

    @Override
    public void save(ByteBuffer output) {
        if (this.maxAttributeId == -1) {
            output.put((byte)0);
            return;
        }
        byte idBitSize = this.maxAttributeId > 127 ? (byte)16 : 8;
        int storages = 1 + this.maxAttributeId / 64;
        int attrCount = this.attributes.size();
        int storageBytes = 8;
        if (attrCount * idBitSize + 16 > storages * 64) {
            output.put((byte)1);
            output.put((byte)storages);
            int headersStartPos = output.position();
            for (int i = 0; i < storages; ++i) {
                output.putLong(0L);
            }
            for (int i = 0; i < this.attributes.size(); ++i) {
                AttributeInstance<?, ?> attribute = this.attributes.getValue(i);
                short id = ((AttributeType)attribute.getType()).id();
                int headerPos = headersStartPos + 8 * (id / 64);
                int bitIndex = id % 64;
                long bit = 1L << bitIndex;
                int curPos = output.position();
                output.position(headerPos);
                long header = output.getLong();
                output.position(headerPos);
                output.putLong(header |= bit);
                output.position(curPos);
                attribute.save(output);
            }
        } else {
            output.put(idBitSize);
            output.putShort((short)attrCount);
            for (int i = 0; i < this.attributes.size(); ++i) {
                AttributeInstance<?, ?> attribute = this.attributes.getValue(i);
                if (idBitSize == 8) {
                    output.put((byte)((AttributeType)attribute.getType()).id());
                } else {
                    output.putShort(((AttributeType)attribute.getType()).id());
                }
                attribute.save(output);
            }
        }
    }

    @Override
    public void load(ByteBuffer input, int worldVersion) throws IOException {
        this.clear();
        byte savedMode = input.get();
        if (savedMode == 0) {
            return;
        }
        int storageBytes = 8;
        if (savedMode == 1) {
            short storages = input.get();
            if (storages == 0) {
                return;
            }
            int headersStartPos = input.position();
            int saveBlockPos = headersStartPos + storages * 8;
            for (short headerIndex = 0; headerIndex < storages; headerIndex = (short)(headerIndex + 1)) {
                input.position(headersStartPos + headerIndex * 8);
                long header = input.getLong();
                input.position(saveBlockPos);
                long bit = 1L;
                for (int bitIndex = 0; bitIndex < 64; bitIndex = (int)((short)(bitIndex + 1))) {
                    if ((header & bit) == bit) {
                        short id = (short)(headerIndex * 64 + bitIndex);
                        AttributeType type = Attribute.TypeFromId(id);
                        if (type == null) {
                            throw new IOException("Unable to read attribute type.");
                        }
                        AttributeInstance<?, ?> attribute = this.getOrAdd(type);
                        if (attribute != null) {
                            attribute.load(input);
                        }
                    }
                    bit <<= 1;
                }
                saveBlockPos = input.position();
            }
        } else {
            int attrCount = input.getShort();
            for (int i = 0; i < attrCount; ++i) {
                AttributeType type = savedMode == 8 ? Attribute.TypeFromId(input.get()) : Attribute.TypeFromId(input.getShort());
                if (type == null) {
                    throw new IOException("Unable to read attribute type.");
                }
                AttributeInstance<?, ?> attribute = this.getOrAdd(type);
                if (attribute == null) continue;
                attribute.load(input);
            }
        }
    }

    @Override
    public void DoTooltip(ObjectTooltip tooltipUI, ObjectTooltip.Layout layout) {
        if (layout == null) {
            return;
        }
        if (this.size() > 0) {
            ObjectTooltip.LayoutItem item;
            AttributeInstance attr;
            ArrayList<AttributeInstance> list = new ArrayList<AttributeInstance>();
            for (int i = 0; i < this.size(); ++i) {
                attr = this.getAttribute(i);
                if (attr.isHiddenUI()) continue;
                list.add(attr);
            }
            if (DebugOptions.instance.tooltipAttributes.getValue()) {
                item = layout.addItem();
                Color c = Colors.CornFlowerBlue;
                item.setLabel("[Debug Begin Attributes]", c.r, c.g, c.b, 1.0f);
            }
            list.sort(Comparator.comparing(AttributeInstance::getNameUI));
            for (AttributeInstance attribute : list) {
                item = layout.addItem();
                item.setLabel(attribute.getNameUI() + ":", 1.0f, 1.0f, 0.8f, 1.0f);
                if (attribute.isDisplayAsBar()) {
                    float f = attribute.getDisplayAsBarUnit();
                    item.setProgress(f, 0.0f, 0.6f, 0.0f, 0.7f);
                    if (!DebugOptions.instance.tooltipAttributes.getValue()) continue;
                    item = layout.addItem();
                    item.setLabel("*" + attribute.getNameUI() + ":", 0.5f, 0.5f, 0.5f, 1.0f);
                    item.setValue(attribute.stringValue(), 0.5f, 0.5f, 0.5f, 1.0f);
                    continue;
                }
                item.setValue(attribute.stringValue(), 1.0f, 1.0f, 1.0f, 1.0f);
            }
            if (DebugOptions.instance.tooltipAttributes.getValue()) {
                list.clear();
                for (int i = 0; i < this.size(); ++i) {
                    attr = this.getAttribute(i);
                    if (!attr.isHiddenUI()) continue;
                    list.add(attr);
                }
                if (!list.isEmpty()) {
                    item = layout.addItem();
                    Color c = Colors.CornFlowerBlue;
                    item.setLabel("[Debug Hidden Attributes]", c.r, c.g, c.b, 1.0f);
                    for (AttributeInstance attribute : list) {
                        item = layout.addItem();
                        item.setLabel(attribute.getNameUI() + ":", 1.0f, 1.0f, 0.8f, 1.0f);
                        if (attribute.isDisplayAsBar()) {
                            float f = attribute.getDisplayAsBarUnit();
                            item.setProgress(f, 0.0f, 0.6f, 0.0f, 0.7f);
                            if (!DebugOptions.instance.tooltipAttributes.getValue()) continue;
                            item = layout.addItem();
                            item.setLabel("*" + attribute.getNameUI() + ":", 0.5f, 0.5f, 0.5f, 1.0f);
                            item.setValue(attribute.stringValue(), 0.5f, 0.5f, 0.5f, 1.0f);
                            continue;
                        }
                        item.setValue(attribute.stringValue(), 1.0f, 1.0f, 1.0f, 1.0f);
                    }
                }
                item = layout.addItem();
                Color c = Colors.CornFlowerBlue;
                item.setLabel("[Debug End Attributes]", c.r, c.g, c.b, 1.0f);
            }
        }
    }
}

