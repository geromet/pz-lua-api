/*
 * Decompiled with CFR 0.152.
 */
package zombie.inventory;

import gnu.trove.map.hash.THashMap;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import se.krka.kahlua.j2se.KahluaTableImpl;
import se.krka.kahlua.vm.KahluaTableIterator;
import se.krka.kahlua.vm.KahluaUtil;
import zombie.Lua.LuaEventManager;
import zombie.Lua.LuaManager;
import zombie.SandboxOptions;
import zombie.UsedFromLua;
import zombie.characters.IsoGameCharacter;
import zombie.characters.IsoPlayer;
import zombie.core.Color;
import zombie.core.Core;
import zombie.core.Translator;
import zombie.core.math.PZMath;
import zombie.core.random.Rand;
import zombie.core.stash.StashSystem;
import zombie.debug.DebugOptions;
import zombie.debug.DebugType;
import zombie.entity.ComponentType;
import zombie.inventory.InventoryItem;
import zombie.inventory.InventoryItemFactory;
import zombie.inventory.ItemConfigurator;
import zombie.inventory.ItemContainer;
import zombie.inventory.ItemGenerationConstants;
import zombie.inventory.ItemPickInfo;
import zombie.inventory.ItemSpawner;
import zombie.inventory.types.Clothing;
import zombie.inventory.types.DrainableComboItem;
import zombie.inventory.types.Food;
import zombie.inventory.types.HandWeapon;
import zombie.inventory.types.InventoryContainer;
import zombie.inventory.types.Key;
import zombie.inventory.types.MapItem;
import zombie.inventory.types.WeaponPart;
import zombie.iso.ContainerOverlays;
import zombie.iso.InstanceTracker;
import zombie.iso.IsoGridSquare;
import zombie.iso.IsoMetaChunk;
import zombie.iso.IsoObject;
import zombie.iso.IsoWorld;
import zombie.iso.areas.IsoBuilding;
import zombie.iso.areas.IsoRoom;
import zombie.iso.objects.IsoClothingDryer;
import zombie.iso.objects.IsoClothingWasher;
import zombie.iso.objects.IsoDeadBody;
import zombie.iso.zones.Zone;
import zombie.network.GameClient;
import zombie.network.GameServer;
import zombie.radio.ZomboidRadio;
import zombie.randomizedWorld.randomizedBuilding.RandomizedBuildingBase;
import zombie.scripting.ScriptManager;
import zombie.scripting.logic.RecipeCodeHelper;
import zombie.scripting.objects.Item;
import zombie.scripting.objects.ItemDisplayCategory;
import zombie.scripting.objects.ItemTag;
import zombie.scripting.objects.ItemType;
import zombie.scripting.objects.Newspaper;
import zombie.util.StringUtils;
import zombie.util.Type;
import zombie.util.list.PZArrayList;
import zombie.util.list.PZArrayUtil;
import zombie.vehicles.BaseVehicle;

@UsedFromLua
public final class ItemPickerJava {
    private static IsoPlayer player;
    private static float otherLootModifier;
    private static float foodLootModifier;
    private static float cannedFoodLootModifier;
    private static float weaponLootModifier;
    private static float rangedWeaponLootModifier;
    private static float ammoLootModifier;
    private static float literatureLootModifier;
    private static float survivalGearsLootModifier;
    private static float medicalLootModifier;
    private static float bagLootModifier;
    private static float mechanicsLootModifier;
    private static float clothingLootModifier;
    private static float containerLootModifier;
    private static float keyLootModifier;
    private static float keyLootModifierD100;
    private static float mediaLootModifier;
    private static float mementoLootModifier;
    private static float cookwareLootModifier;
    private static float materialLootModifier;
    private static float farmingLootModifier;
    private static float toolLootModifier;
    private static float skillBookLootModifier;
    private static float recipeResourceLootModifier;
    private static final String OtherLootType = "Other";
    private static final String FoodLootType = "Food";
    private static final String CannedFoodLootType = "CannedFood";
    private static final String WeaponLootType = "Weapon";
    private static final String RangedWeaponLootType = "RangedWeapon";
    private static final String AmmoLootType = "Ammo";
    private static final String LiteratureLootType = "Literature";
    private static final String SurvivalGearsLootType = "SurvivalGears";
    private static final String MedicalLootType = "Medical";
    private static final String MechanicsLootType = "Mechanics";
    private static final String ClothingLootType = "Clothing";
    private static final String ContainerLootType = "Container";
    private static final String KeyLootType = "Key";
    private static final String MediaLootType = "Media";
    private static final String MementoLootType = "Memento";
    private static final String CookwareLootType = "Cookware";
    private static final String MaterialLootType = "Material";
    private static final String FarmingLootType = "Farming";
    private static final String ToolLootType = "Tool";
    private static final String GeneratorLootType = "Generator";
    private static final String SkillBookLootType = "SkillBook";
    private static final String RecipeResourceLootType = "RecipeResource";
    public static float zombieDensityCap;
    public static final ArrayList<String> NoContainerFillRooms;
    public static final ArrayList<ItemPickerUpgradeWeapons> WeaponUpgrades;
    public static final HashMap<String, ItemPickerUpgradeWeapons> WeaponUpgradeMap;
    public static final THashMap<String, ItemPickerRoom> rooms;
    public static final THashMap<String, ItemPickerContainer> containers;
    public static final THashMap<String, ItemPickerContainer> ProceduralDistributions;
    public static final THashMap<String, VehicleDistribution> VehicleDistributions;
    private static final ArrayList<String> addedInvalidAlready;

    public static THashMap<String, ItemPickerContainer> getItemPickerContainers() {
        return containers;
    }

    public static void Parse() {
        rooms.clear();
        NoContainerFillRooms.clear();
        WeaponUpgradeMap.clear();
        WeaponUpgrades.clear();
        containers.clear();
        addedInvalidAlready.clear();
        KahluaTableImpl noContainerFillRooms = (KahluaTableImpl)LuaManager.env.rawget("NoContainerFillRooms");
        for (Map.Entry<Object, Object> objectObjectEntry : noContainerFillRooms.delegate.entrySet()) {
            String room = objectObjectEntry.getKey().toString();
            NoContainerFillRooms.add(room);
        }
        KahluaTableImpl weaponUpgrades = (KahluaTableImpl)LuaManager.env.rawget("WeaponUpgrades");
        for (Map.Entry<Object, Object> objectObjectEntry : weaponUpgrades.delegate.entrySet()) {
            String type = objectObjectEntry.getKey().toString();
            ItemPickerUpgradeWeapons w = new ItemPickerUpgradeWeapons();
            w.name = type;
            WeaponUpgrades.add(w);
            WeaponUpgradeMap.put(type, w);
            KahluaTableImpl upgrades = (KahluaTableImpl)objectObjectEntry.getValue();
            for (Map.Entry<Object, Object> up : upgrades.delegate.entrySet()) {
                String upType = up.getValue().toString();
                w.upgrades.add(upType);
            }
        }
        ItemPickerJava.ParseSuburbsDistributions();
        ItemPickerJava.ParseVehicleDistributions();
        ItemPickerJava.ParseProceduralDistributions();
    }

    private static void ParseSuburbsDistributions() {
        KahluaTableImpl table = (KahluaTableImpl)LuaManager.env.rawget("SuburbsDistributions");
        for (Map.Entry<Object, Object> objectObjectEntry : table.delegate.entrySet()) {
            String key = objectObjectEntry.getKey().toString();
            KahluaTableImpl containers = (KahluaTableImpl)objectObjectEntry.getValue();
            if (containers.delegate.containsKey("rolls")) {
                ItemPickerContainer c = ItemPickerJava.ExtractContainersFromLua(containers);
                ItemPickerJava.containers.put(key, c);
                continue;
            }
            ItemPickerRoom room = new ItemPickerRoom();
            rooms.put(key, room);
            for (Map.Entry<Object, Object> containerSet : containers.delegate.entrySet()) {
                String containerName = containerSet.getKey().toString();
                if (containerSet.getValue() instanceof Double) {
                    room.fillRand = ((Double)containerSet.getValue()).intValue();
                    continue;
                }
                if ("isShop".equals(containerName)) {
                    room.isShop = (Boolean)containerSet.getValue();
                    continue;
                }
                if ("professionChance".equals(containerName)) {
                    room.professionChance = ((Double)containerSet.getValue()).intValue();
                    continue;
                }
                if ("outfit".equals(containerName)) {
                    room.outfit = (String)containerSet.getValue();
                    continue;
                }
                if ("outfitFemale".equals(containerName)) {
                    room.outfitFemale = (String)containerSet.getValue();
                    continue;
                }
                if ("outfitMale".equals(containerName)) {
                    room.outfitMale = (String)containerSet.getValue();
                    continue;
                }
                if ("outfitChance".equals(containerName)) {
                    room.outfitChance = (String)containerSet.getValue();
                    continue;
                }
                if ("vehicle".equals(containerName)) {
                    room.vehicle = (String)containerSet.getValue();
                    continue;
                }
                if ("vehicles".equals(containerName)) {
                    room.vehicles = Arrays.asList(containerName.split(";"));
                    continue;
                }
                if ("vehicleChance".equals(containerName)) {
                    room.vehicleChance = (String)containerSet.getValue();
                    continue;
                }
                if ("vehicleDistribution".equals(containerName)) {
                    room.vehicleDistribution = (String)containerSet.getValue();
                    continue;
                }
                if ("vehicleSkin".equals(containerName)) {
                    room.vehicleSkin = (Integer)containerSet.getValue();
                    continue;
                }
                if ("femaleChance".equals(containerName)) {
                    room.femaleChance = (String)containerSet.getValue();
                    continue;
                }
                if ("roomTypes".equals(containerName)) {
                    room.roomTypes = (String)containerSet.getValue();
                    continue;
                }
                if ("zoneRequires".equals(containerName)) {
                    room.zoneRequires = (String)containerSet.getValue();
                    continue;
                }
                if ("zoneDisallows".equals(containerName)) {
                    room.zoneDisallows = (String)containerSet.getValue();
                    continue;
                }
                if ("containerChance".equals(containerName)) {
                    room.containerChance = (String)containerSet.getValue();
                    continue;
                }
                if ("femaleOdds".equals(containerName)) {
                    room.femaleOdds = (String)containerSet.getValue();
                    continue;
                }
                if ("bagType".equals(containerName)) {
                    room.bagType = (String)containerSet.getValue();
                    continue;
                }
                if ("bagTable".equals(containerName)) {
                    room.bagTable = (String)containerSet.getValue();
                    continue;
                }
                KahluaTableImpl con = null;
                try {
                    con = (KahluaTableImpl)containerSet.getValue();
                }
                catch (Exception e) {
                    e.printStackTrace();
                }
                if (!(con.delegate.containsKey("procedural") || !containerName.isEmpty() && con.delegate.containsKey("rolls") && con.delegate.containsKey("items"))) {
                    DebugType.ItemPicker.error("ERROR: SuburbsDistributions[\"%s\"] is broken", key);
                    continue;
                }
                ItemPickerContainer c = ItemPickerJava.ExtractContainersFromLua(con);
                room.containers.put(containerName, c);
            }
            room.compact();
        }
    }

    private static void ParseVehicleDistributions() {
        VehicleDistributions.clear();
        KahluaTableImpl table = (KahluaTableImpl)LuaManager.env.rawget("VehicleDistributions");
        if (table == null || !(table.rawget(1) instanceof KahluaTableImpl)) {
            return;
        }
        table = (KahluaTableImpl)table.rawget(1);
        for (Map.Entry<Object, Object> entry : table.delegate.entrySet()) {
            Object room;
            Object object;
            if (!(entry.getKey() instanceof String) || !((object = entry.getValue()) instanceof KahluaTableImpl)) continue;
            KahluaTableImpl tableEntry = (KahluaTableImpl)object;
            VehicleDistribution vehicleDistribution = new VehicleDistribution();
            Object object2 = tableEntry.rawget("Normal");
            if (object2 instanceof KahluaTableImpl) {
                KahluaTableImpl normal = (KahluaTableImpl)object2;
                room = new ItemPickerRoom();
                for (Map.Entry<Object, Object> containerSet : normal.delegate.entrySet()) {
                    String containerName = containerSet.getKey().toString();
                    if (containerName.equals("specificId")) continue;
                    ((ItemPickerRoom)room).containers.put(containerName, ItemPickerJava.ExtractContainersFromLua((KahluaTableImpl)containerSet.getValue()));
                }
                vehicleDistribution.normal = room;
            }
            if ((room = tableEntry.rawget("Specific")) instanceof KahluaTableImpl) {
                KahluaTableImpl specific = (KahluaTableImpl)room;
                for (int i = 1; i <= specific.len(); ++i) {
                    KahluaTableImpl table3 = (KahluaTableImpl)specific.rawget(i);
                    ItemPickerRoom room2 = new ItemPickerRoom();
                    for (Map.Entry<Object, Object> containerSet : table3.delegate.entrySet()) {
                        String containerName = containerSet.getKey().toString();
                        if (containerName.equals("specificId")) {
                            room2.specificId = (String)containerSet.getValue();
                            continue;
                        }
                        room2.containers.put(containerName, ItemPickerJava.ExtractContainersFromLua((KahluaTableImpl)containerSet.getValue()));
                    }
                    vehicleDistribution.specific.add(room2);
                }
            }
            vehicleDistribution.compact();
            if (vehicleDistribution.normal == null) continue;
            VehicleDistributions.put((String)entry.getKey(), vehicleDistribution);
        }
    }

    private static void ParseProceduralDistributions() {
        ProceduralDistributions.clear();
        Object object = LuaManager.env.rawget("ProceduralDistributions");
        if (!(object instanceof KahluaTableImpl)) {
            return;
        }
        KahluaTableImpl t1 = (KahluaTableImpl)object;
        Iterator<Map.Entry<Object, Object>> iterator2 = t1.rawget("list");
        if (!(iterator2 instanceof KahluaTableImpl)) {
            return;
        }
        KahluaTableImpl t2 = (KahluaTableImpl)((Object)iterator2);
        for (Map.Entry<Object, Object> entry : t2.delegate.entrySet()) {
            String name = entry.getKey().toString();
            KahluaTableImpl t3 = (KahluaTableImpl)entry.getValue();
            ItemPickerContainer ipc = ItemPickerJava.ExtractContainersFromLua(t3);
            ProceduralDistributions.put(name, ipc);
        }
    }

    private static ItemPickerContainer ExtractContainersFromLua(KahluaTableImpl con) {
        ItemPickerContainer c = new ItemPickerContainer();
        if (con.delegate.containsKey("isShop")) {
            c.isShop = con.rawgetBool("isShop");
        }
        if (con.delegate.containsKey("procedural")) {
            c.procedural = con.rawgetBool("procedural");
            c.proceduralItems = ItemPickerJava.ExtractProcList(con);
            return c;
        }
        if (con.delegate.containsKey("noAutoAge")) {
            c.noAutoAge = con.rawgetBool("noAutoAge");
        }
        if (con.delegate.containsKey("fillRand")) {
            c.fillRand = con.rawgetInt("fillRand");
        }
        if (con.delegate.containsKey("maxMap")) {
            c.maxMap = con.rawgetInt("maxMap");
        }
        if (con.delegate.containsKey("stashChance")) {
            c.stashChance = con.rawgetInt("stashChance");
        }
        if (con.delegate.containsKey("dontSpawnAmmo")) {
            c.dontSpawnAmmo = con.rawgetBool("dontSpawnAmmo");
        }
        if (con.delegate.containsKey("gunStorage")) {
            c.gunStorage = con.rawgetBool("gunStorage");
        }
        if (con.delegate.containsKey("ignoreZombieDensity")) {
            c.ignoreZombieDensity = con.rawgetBool("ignoreZombieDensity");
        }
        if (con.delegate.containsKey("cookFood")) {
            c.cookFood = con.rawgetBool("cookFood");
        }
        if (con.delegate.containsKey("canBurn")) {
            c.canBurn = con.rawgetBool("canBurn");
        }
        if (con.delegate.containsKey("isTrash")) {
            c.isTrash = con.rawgetBool("isTrash");
        }
        if (con.delegate.containsKey("isWorn")) {
            c.isWorn = con.rawgetBool("isWorn");
        }
        if (con.delegate.containsKey("isRotten")) {
            c.isRotten = con.rawgetBool("isRotten");
        }
        if (con.delegate.containsKey("onlyOne")) {
            c.onlyOne = con.rawgetBool("onlyOne");
        }
        double rolls = (Double)con.delegate.get("rolls");
        if (con.delegate.containsKey("junk")) {
            c.junk = ItemPickerJava.ExtractContainersFromLua((KahluaTableImpl)con.rawget("junk"));
        }
        if (con.delegate.containsKey("bags")) {
            c.bags = ItemPickerJava.ExtractContainersFromLua((KahluaTableImpl)con.rawget("bags"));
        }
        if (con.delegate.containsKey("defaultInventoryLoot")) {
            c.defaultInventoryLoot = con.rawgetBool("defaultInventoryLoot");
        }
        c.rolls = (int)rolls;
        KahluaTableImpl itemListT = (KahluaTableImpl)con.delegate.get("items");
        ArrayList<ItemPickerItem> itemList = new ArrayList<ItemPickerItem>();
        int len = itemListT.len();
        for (int i = 0; i < len; i += 2) {
            boolean itemExists;
            String name = Type.tryCastTo(itemListT.delegate.get(KahluaUtil.toDouble(i + 1)), String.class);
            Double chance = Type.tryCastTo(itemListT.delegate.get(KahluaUtil.toDouble(i + 2)), Double.class);
            if (name == null || chance == null) continue;
            Item scriptItem = ScriptManager.instance.FindItem(name);
            boolean bl = itemExists = scriptItem != null || InventoryItemFactory.getItem(name, true) != null;
            if (!itemExists || scriptItem != null && scriptItem.obsolete) {
                if (!Core.debug || addedInvalidAlready.contains(name)) continue;
                addedInvalidAlready.add(name);
                DebugType.ItemPicker.println("ignoring invalid ItemPicker item type \"%s\", obsolete = \"%s\"", name, scriptItem != null);
                continue;
            }
            if (scriptItem != null) {
                scriptItem.setCanSpawnAsLoot(true);
            }
            ItemPickerItem ipi = new ItemPickerItem();
            ipi.itemName = name;
            ipi.chance = chance.floatValue();
            itemList.add(ipi);
        }
        c.items = itemList.toArray(c.items);
        return c;
    }

    private static ArrayList<ProceduralItem> ExtractProcList(KahluaTableImpl table) {
        ArrayList<ProceduralItem> result = new ArrayList<ProceduralItem>();
        KahluaTableImpl procList = (KahluaTableImpl)table.rawget("procList");
        KahluaTableIterator it = procList.iterator();
        while (it.advance()) {
            KahluaTableImpl entry = (KahluaTableImpl)it.getValue();
            ProceduralItem procItem = new ProceduralItem();
            procItem.name = entry.rawgetStr("name");
            procItem.min = entry.rawgetInt("min");
            procItem.max = entry.rawgetInt("max");
            procItem.weightChance = entry.rawgetInt("weightChance");
            String forceForItems = entry.rawgetStr("forceForItems");
            String forceForZones = entry.rawgetStr("forceForZones");
            String forceForTiles = entry.rawgetStr("forceForTiles");
            String forceForRooms = entry.rawgetStr("forceForRooms");
            if (!StringUtils.isNullOrWhitespace(forceForItems)) {
                procItem.forceForItems = Arrays.asList(forceForItems.split(";"));
            }
            if (!StringUtils.isNullOrWhitespace(forceForZones)) {
                procItem.forceForZones = Arrays.asList(forceForZones.split(";"));
            }
            if (!StringUtils.isNullOrWhitespace(forceForTiles)) {
                procItem.forceForTiles = Arrays.asList(forceForTiles.split(";"));
            }
            if (!StringUtils.isNullOrWhitespace(forceForRooms)) {
                procItem.forceForRooms = Arrays.asList(forceForRooms.split(";"));
            }
            result.add(procItem);
        }
        return result;
    }

    public static void InitSandboxLootSettings() {
        otherLootModifier = (float)SandboxOptions.getInstance().otherLootNew.getValue();
        foodLootModifier = (float)SandboxOptions.getInstance().foodLootNew.getValue();
        weaponLootModifier = (float)SandboxOptions.getInstance().weaponLootNew.getValue();
        rangedWeaponLootModifier = (float)SandboxOptions.getInstance().rangedWeaponLootNew.getValue();
        ammoLootModifier = (float)SandboxOptions.getInstance().ammoLootNew.getValue();
        cannedFoodLootModifier = (float)SandboxOptions.getInstance().cannedFoodLootNew.getValue();
        literatureLootModifier = (float)SandboxOptions.getInstance().literatureLootNew.getValue();
        survivalGearsLootModifier = (float)SandboxOptions.getInstance().survivalGearsLootNew.getValue();
        medicalLootModifier = (float)SandboxOptions.getInstance().medicalLootNew.getValue();
        mechanicsLootModifier = (float)SandboxOptions.getInstance().mechanicsLootNew.getValue();
        clothingLootModifier = (float)SandboxOptions.getInstance().clothingLootNew.getValue();
        containerLootModifier = (float)SandboxOptions.getInstance().containerLootNew.getValue();
        keyLootModifier = (float)SandboxOptions.getInstance().keyLootNew.getValue();
        keyLootModifierD100 = (float)SandboxOptions.getInstance().keyLootNew.getValue();
        mediaLootModifier = (float)SandboxOptions.getInstance().mediaLootNew.getValue();
        mementoLootModifier = (float)SandboxOptions.getInstance().mementoLootNew.getValue();
        cookwareLootModifier = (float)SandboxOptions.getInstance().cookwareLootNew.getValue();
        materialLootModifier = (float)SandboxOptions.getInstance().materialLootNew.getValue();
        farmingLootModifier = (float)SandboxOptions.getInstance().farmingLootNew.getValue();
        toolLootModifier = (float)SandboxOptions.getInstance().toolLootNew.getValue();
        skillBookLootModifier = (float)SandboxOptions.getInstance().skillBookLoot.getValue();
        recipeResourceLootModifier = (float)SandboxOptions.getInstance().recipeResourceLoot.getValue();
    }

    private static float doSandboxSettings(int value) {
        switch (value) {
            case 1: {
                return 0.0f;
            }
            case 2: {
                return (float)SandboxOptions.instance.insaneLootFactor.getValue();
            }
            case 3: {
                return (float)SandboxOptions.instance.extremeLootFactor.getValue();
            }
            case 4: {
                return (float)SandboxOptions.instance.rareLootFactor.getValue();
            }
            case 5: {
                return (float)SandboxOptions.instance.normalLootFactor.getValue();
            }
            case 6: {
                return (float)SandboxOptions.instance.commonLootFactor.getValue();
            }
            case 7: {
                return (float)SandboxOptions.instance.abundantLootFactor.getValue();
            }
        }
        return 0.6f;
    }

    public static void fillContainer(ItemContainer container, IsoPlayer player) {
        ItemPickInfo pickInfo = ItemPickInfo.GetPickInfo(container, ItemPickInfo.Caller.FillContainer);
        ItemPickerJava.fillContainerInternal(pickInfo, container, player);
    }

    private static void fillContainerInternal(ItemPickInfo pickInfo, ItemContainer container, IsoPlayer player) {
        if (GameClient.client || "Tutorial".equals(Core.gameMode)) {
            return;
        }
        if (container == null) {
            return;
        }
        IsoGridSquare sq = container.getSourceGrid();
        if (sq == null) {
            return;
        }
        IsoRoom room = sq.getRoom();
        if (container.getType().equals("inventorymale") || container.getType().equals("inventoryfemale")) {
            if (container.getParent() != null && container.getParent() instanceof IsoDeadBody && ((IsoDeadBody)container.getParent()).isSkeleton()) {
                return;
            }
            String containerType = container.getType();
            if (container.getParent() != null && container.getParent() instanceof IsoDeadBody) {
                containerType = ((IsoDeadBody)container.getParent()).getOutfitName();
            }
            ItemPickerContainer containerDist = ItemPickerJava.rooms.get((Object)"all").containers.get("Outfit_" + containerType);
            for (int i = 0; i < container.getItems().size(); ++i) {
                InventoryItem item = container.getItems().get(i);
                if (!(item instanceof InventoryContainer)) continue;
                InventoryContainer inventoryContainer = (InventoryContainer)item;
                ItemPickerContainer itemPickerContainer = containers.get(item.getType());
                if (containerDist != null && containerDist.bags != null && !item.hasTag(ItemTag.BAGS_FILL_EXCEPTION)) {
                    ItemPickerJava.rollContainerItemInternal(pickInfo, inventoryContainer, null, containerDist.bags);
                    LuaEventManager.triggerEvent("OnFillContainer", "Zombie Bag", item.getType(), containerDist.bags);
                    continue;
                }
                if (itemPickerContainer == null || Rand.Next(itemPickerContainer.fillRand) != 0) continue;
                ItemPickerJava.rollContainerItemInternal(pickInfo, inventoryContainer, null, containers.get(item.getType()));
                LuaEventManager.triggerEvent("OnFillContainer", "Zombie Bag", item.getType(), inventoryContainer.getItemContainer());
            }
            boolean defaultInventoryLoot = true;
            if (containerDist != null) {
                defaultInventoryLoot = containerDist.defaultInventoryLoot;
            }
            if (containerDist != null) {
                ItemPickerJava.rollItemInternal(pickInfo, containerDist, container, true, player, null);
            }
            if (defaultInventoryLoot) {
                containerDist = ItemPickerJava.rooms.get((Object)"all").containers.get(container.getType());
                ItemPickerJava.rollItemInternal(pickInfo, containerDist, container, true, player, null);
            }
            InstanceTracker.inc("Container Rolls", "Zombie/" + containerType);
            LuaEventManager.triggerEvent("OnFillContainer", "Zombie", containerType, container);
            return;
        }
        ItemPickerRoom roomDist = null;
        if (rooms.containsKey("all")) {
            roomDist = rooms.get("all");
        }
        if (room != null && rooms.containsKey(room.getName())) {
            String roomName = room.getName();
            ItemPickerRoom roomDist2 = rooms.get(roomName);
            ItemPickerContainer containerDist = null;
            if (roomDist2.containers.containsKey(container.getType())) {
                containerDist = roomDist2.containers.get(container.getType());
            }
            if (containerDist == null && roomDist2.containers.containsKey("other")) {
                containerDist = roomDist2.containers.get("other");
            }
            if (containerDist == null && roomDist2.containers.containsKey("all")) {
                containerDist = roomDist2.containers.get("all");
                roomName = "all";
            }
            if (containerDist == null) {
                ItemPickerJava.fillContainerTypeInternal(pickInfo, roomDist, container, roomName, player);
                LuaEventManager.triggerEvent("OnFillContainer", roomName, container.getType(), container);
                return;
            }
        } else {
            String roomName = room != null ? room.getName() : "all";
            ItemPickerJava.fillContainerTypeInternal(pickInfo, roomDist, container, roomName, player);
            LuaEventManager.triggerEvent("OnFillContainer", roomName, container.getType(), container);
            return;
        }
        if (rooms.containsKey(room.getName())) {
            roomDist = rooms.get(room.getName());
        }
        if (roomDist != null) {
            ItemPickerJava.fillContainerTypeInternal(pickInfo, roomDist, container, room.getName(), player);
            LuaEventManager.triggerEvent("OnFillContainer", room.getName(), container.getType(), container);
        }
    }

    public static void fillContainerType(ItemPickerRoom roomDist, ItemContainer container, String roomName, IsoGameCharacter character) {
        ItemPickInfo pickInfo = ItemPickInfo.GetPickInfo(container, ItemPickInfo.Caller.FillContainerType);
        if (pickInfo != null) {
            pickInfo.updateRoomDist(roomDist);
        }
        ItemPickerJava.fillContainerTypeInternal(pickInfo, roomDist, container, roomName, character);
    }

    private static void fillContainerTypeInternal(ItemPickInfo pickInfo, ItemPickerRoom roomDist, ItemContainer container, String roomName, IsoGameCharacter character) {
        ItemPickerContainer containerDist;
        boolean doItemContainer = true;
        if (NoContainerFillRooms.contains(roomName)) {
            doItemContainer = false;
        }
        if (roomDist == null) {
            containerDist = roomDist.containers.get("all");
            ItemPickerJava.rollItemInternal(pickInfo, containerDist, container, doItemContainer, character, roomDist);
        } else if (roomDist.containers.containsKey("all")) {
            containerDist = roomDist.containers.get("all");
            ItemPickerJava.rollItemInternal(pickInfo, containerDist, container, doItemContainer, character, roomDist);
        }
        InstanceTracker.inc("Container Rolls", (StringUtils.isNullOrEmpty(roomName) ? "unknown" : roomName) + "/" + container.getType());
        containerDist = roomDist.containers.get(container.getType());
        if (containerDist == null) {
            containerDist = roomDist.containers.get("other");
        }
        if (containerDist != null) {
            ItemPickerJava.rollItemInternal(pickInfo, containerDist, container, doItemContainer, character, roomDist);
        }
    }

    public static InventoryItem tryAddItemToContainer(ItemContainer container, String itemType, ItemPickerContainer containerDist) {
        ItemContainer contain;
        boolean corpse;
        Item scriptItem = ScriptManager.instance.FindItem(itemType);
        if (scriptItem == null) {
            return null;
        }
        if (scriptItem.obsolete) {
            return null;
        }
        float totalWeight = scriptItem.getActualWeight() * (float)scriptItem.getCount();
        if (!container.hasRoomFor(null, totalWeight)) {
            return null;
        }
        boolean bl = corpse = container.getContainingItem() instanceof InventoryContainer && container.getContainingItem().getContainer() != null && container.getContainingItem().getContainer().getParent() != null && container.getContainingItem().getContainer().getParent() instanceof IsoDeadBody;
        if (!corpse && container.getContainingItem() instanceof InventoryContainer && (contain = container.getContainingItem().getContainer()) != null && !contain.hasRoomFor(null, totalWeight)) {
            return null;
        }
        return ItemSpawner.spawnItem(itemType, container);
    }

    private static void rollProceduralItem(ArrayList<ProceduralItem> proceduralItems, ItemContainer container, float zombieDensity, IsoGameCharacter character, ItemPickerRoom roomDist) {
        ItemPickInfo pickInfo = ItemPickInfo.GetPickInfo(container, ItemPickInfo.Caller.RollProceduralItem);
        if (pickInfo != null) {
            pickInfo.updateRoomDist(roomDist);
        }
        ItemPickerJava.rollProceduralItemInternal(pickInfo, proceduralItems, container, zombieDensity, character, roomDist);
    }

    private static void rollProceduralItemInternal(ItemPickInfo itemPickInfo, ArrayList<ProceduralItem> proceduralItems, ItemContainer container, float zombieDensity, IsoGameCharacter character, ItemPickerRoom roomDist) {
        if (container.getSourceGrid() == null) {
            return;
        }
        boolean room = container.getSourceGrid().getRoom() != null;
        HashMap<String, Integer> alreadySpawnedStuff = null;
        if (room) {
            alreadySpawnedStuff = container.getSourceGrid().getRoom().getRoomDef().getProceduralSpawnedContainer();
        }
        HashMap<String, Integer> forcedToSpawn = new HashMap<String, Integer>();
        HashMap<String, Integer> normalSpawn = new HashMap<String, Integer>();
        for (int ip = 0; ip < proceduralItems.size(); ++ip) {
            IsoGridSquare sq;
            ProceduralItem proceduralItem = proceduralItems.get(ip);
            String name = proceduralItem.name;
            int min = proceduralItem.min;
            int max = proceduralItem.max;
            int weightChance = proceduralItem.weightChance;
            List<String> forceForItems = proceduralItem.forceForItems;
            List<String> forceForZones = proceduralItem.forceForZones;
            List<String> forceForTiles = proceduralItem.forceForTiles;
            List<String> forceForRooms = proceduralItem.forceForRooms;
            if (alreadySpawnedStuff != null && alreadySpawnedStuff.get(name) == null) {
                alreadySpawnedStuff.put(name, 0);
            }
            if (forceForItems != null && room && container.getSourceGrid() != null && container.getSourceGrid().getRoom() != null && container.getSourceGrid().getRoom().getBuilding() != null && container.getSourceGrid().getRoom().getBuilding().getRoomsNumber() <= RandomizedBuildingBase.maximumRoomCount) {
                for (int x = container.getSourceGrid().getRoom().getRoomDef().x; x < container.getSourceGrid().getRoom().getRoomDef().x2; ++x) {
                    block2: for (int y = container.getSourceGrid().getRoom().getRoomDef().y; y < container.getSourceGrid().getRoom().getRoomDef().y2; ++y) {
                        IsoGridSquare sq2 = container.getSourceGrid().getCell().getGridSquare(x, y, container.getSourceGrid().z);
                        if (sq2 == null) continue;
                        for (int i = 0; i < sq2.getObjects().size(); ++i) {
                            IsoObject obj = sq2.getObjects().get(i);
                            if (!forceForItems.contains(obj.getSprite().name)) continue;
                            forcedToSpawn.clear();
                            forcedToSpawn.put(name, -1);
                            continue block2;
                        }
                    }
                }
            } else if (forceForTiles != null) {
                IsoGridSquare sq3 = container.getSourceGrid();
                if (sq3 != null) {
                    for (int i = 0; i < sq3.getObjects().size(); ++i) {
                        IsoObject obj = sq3.getObjects().get(i);
                        if (obj.getSprite() == null || !forceForTiles.contains(obj.getSprite().getName())) continue;
                        forcedToSpawn.clear();
                        forcedToSpawn.put(name, -1);
                        break;
                    }
                } else if (forceForZones != null) {
                    ArrayList<Zone> metazones = IsoWorld.instance.metaGrid.getZonesAt(container.getSourceGrid().x, container.getSourceGrid().y, 0);
                    for (int i = 0; i < metazones.size(); ++i) {
                        if (alreadySpawnedStuff != null && alreadySpawnedStuff.get(name) >= max || !forceForZones.contains(metazones.get((int)i).type) && !forceForZones.contains(metazones.get((int)i).name)) continue;
                        forcedToSpawn.clear();
                        forcedToSpawn.put(name, -1);
                        break;
                    }
                }
            } else if (forceForRooms != null && room && (sq = container.getSourceGrid()) != null) {
                for (int i = 0; i < forceForRooms.size(); ++i) {
                    if (sq.getBuilding().getRandomRoom(forceForRooms.get(i)) == null) continue;
                    forcedToSpawn.clear();
                    forcedToSpawn.put(name, -1);
                    break;
                }
            }
            if (forceForItems != null || forceForZones != null || forceForTiles != null || forceForRooms != null) continue;
            if (room && min == 1 && alreadySpawnedStuff.get(name) == 0) {
                forcedToSpawn.put(name, weightChance);
                continue;
            }
            if (room && alreadySpawnedStuff.get(name) >= max) continue;
            normalSpawn.put(name, weightChance);
        }
        String containerNameToSpawn = null;
        if (!forcedToSpawn.isEmpty()) {
            containerNameToSpawn = ItemPickerJava.getDistribInHashMap(forcedToSpawn);
        } else if (!normalSpawn.isEmpty()) {
            containerNameToSpawn = ItemPickerJava.getDistribInHashMap(normalSpawn);
        }
        if (containerNameToSpawn == null) {
            return;
        }
        ItemPickerContainer containerDistToSpawn = ProceduralDistributions.get(containerNameToSpawn);
        if (containerDistToSpawn == null) {
            return;
        }
        if (containerDistToSpawn.junk != null) {
            ItemPickerJava.doRollItemInternal(itemPickInfo, containerDistToSpawn.junk, container, zombieDensity, character, true, roomDist, true);
        }
        ItemPickerJava.doRollItemInternal(itemPickInfo, containerDistToSpawn, container, zombieDensity, character, true, roomDist);
        if (alreadySpawnedStuff != null) {
            alreadySpawnedStuff.put(containerNameToSpawn, alreadySpawnedStuff.get(containerNameToSpawn) + 1);
        }
    }

    private static String getDistribInHashMap(HashMap<String, Integer> map) {
        int rand;
        int totalWeight = 0;
        int currentChance = 0;
        for (String name : map.keySet()) {
            totalWeight += map.get(name).intValue();
        }
        if (totalWeight == -1) {
            rand = Rand.Next(map.size());
            Iterator<String> it = map.keySet().iterator();
            int index = 0;
            while (it.hasNext()) {
                if (index == rand) {
                    return it.next();
                }
                ++index;
            }
        }
        rand = Rand.Next(totalWeight);
        for (String name : map.keySet()) {
            int chance = map.get(name);
            if ((currentChance += chance) < rand) continue;
            return name;
        }
        return null;
    }

    public static void rollItem(ItemPickerContainer containerDist, ItemContainer container, boolean doItemContainer, IsoGameCharacter character, ItemPickerRoom roomDist) {
        ItemPickInfo pickInfo = ItemPickInfo.GetPickInfo(container, ItemPickInfo.Caller.RollItem);
        if (pickInfo != null) {
            pickInfo.updateRoomDist(roomDist);
        }
        ItemPickerJava.rollItemInternal(pickInfo, containerDist, container, doItemContainer, character, roomDist);
    }

    private static void rollItemInternal(ItemPickInfo itemPickInfo, ItemPickerContainer containerDist, ItemContainer container, boolean doItemContainer, IsoGameCharacter character, ItemPickerRoom roomDist) {
        if (!GameClient.client && !GameServer.server) {
            player = IsoPlayer.getInstance();
        }
        if (containerDist != null && container != null) {
            float zombieDensity = ItemPickerJava.getZombieDensityFactor(containerDist, container);
            if (containerDist.procedural) {
                ItemPickerJava.rollProceduralItemInternal(itemPickInfo, containerDist.proceduralItems, container, zombieDensity, character, roomDist);
            } else {
                if (containerDist.junk != null) {
                    ItemPickerJava.doRollItemInternal(itemPickInfo, containerDist.junk, container, zombieDensity, character, doItemContainer, roomDist, true);
                }
                ItemPickerJava.doRollItemInternal(itemPickInfo, containerDist, container, zombieDensity, character, doItemContainer, roomDist);
            }
        }
    }

    public static void doRollItem(ItemPickerContainer containerDist, ItemContainer container, float zombieDensity, IsoGameCharacter character, boolean doItemContainer, ItemPickerRoom roomDist) {
        ItemPickInfo pickInfo = ItemPickInfo.GetPickInfo(container, ItemPickInfo.Caller.DoRollItem);
        if (pickInfo != null) {
            pickInfo.updateRoomDist(roomDist);
        }
        ItemPickerJava.doRollItemInternal(pickInfo, containerDist, container, zombieDensity, character, doItemContainer, roomDist);
    }

    private static void doRollItemInternal(ItemPickInfo pickInfo, ItemPickerContainer containerDist, ItemContainer container, float zombieDensity, IsoGameCharacter character, boolean doItemContainer, ItemPickerRoom roomDist) {
        ItemPickerJava.doRollItemInternal(pickInfo, containerDist, container, zombieDensity, character, doItemContainer, roomDist, false);
    }

    /*
     * Unable to fully structure code
     */
    private static void doRollItemInternal(ItemPickInfo pickInfo, ItemPickerContainer containerDist, ItemContainer container, float zombieDensity, IsoGameCharacter character, boolean doItemContainer, ItemPickerRoom roomDist, boolean isJunk) {
        parent = null;
        if (container.getParent() != null) {
            parent = container.getParent();
        }
        dirtyClothes = false;
        wetClothes = false;
        isEmptyFluidContainer = false;
        if ((parent instanceof IsoClothingDryer || Objects.equals(container.getType(), "clothingdryer")) && Rand.NextBool(5)) {
            wetClothes = true;
        }
        if (parent instanceof IsoClothingWasher || Objects.equals(container.getType(), "clothingwasher")) {
            if (Rand.NextBool(2)) {
                wetClothes = true;
            } else {
                dirtyClothes = true;
            }
        }
        isTrash = containerDist.isTrash;
        isShop = false;
        if (!container.isCorpse() && !isTrash) {
            v0 = isShop = containerDist.isShop != false || container.isShop() != false;
            if (!isShop && roomDist != null) {
                isShop = roomDist.isShop;
            }
            if (!isShop && container.getSquare() != null) {
                isShop = container.getSquare().isShop();
            }
            if (!isShop && container.getSquare() != null && container.getSquare().getRoom() != null) {
                isShop = container.getSquare().getRoom().isShop();
            }
        }
        laundry = parent instanceof IsoClothingDryer != false || Objects.equals(container.getType(), "clothingdryer") != false || parent instanceof IsoClothingWasher != false || Objects.equals(container.getType(), "clothingwasher") != false;
        rolls = (int)((double)containerDist.rolls * SandboxOptions.instance.rollsMultiplier.getValue());
        rolls = Math.max(rolls, 1);
        for (m = 0; m < rolls; ++m) {
            items = containerDist.items;
            for (i = 0; i < items.length; ++i) {
                item = items[i];
                itemName = item.itemName;
                scriptItem = ScriptManager.instance.FindItem(itemName);
                if (scriptItem == null && itemName.endsWith("Empty") && (scriptItem = ScriptManager.instance.FindItem(itemName = itemName.substring(0, itemName.length() - 5), true)) != null) {
                    if (!scriptItem.containsComponent(ComponentType.FluidContainer)) {
                        scriptItem = null;
                    } else {
                        isEmptyFluidContainer = true;
                    }
                }
                if (!((float)Rand.Next(10000) < ItemPickerJava.getActualSpawnChance(item, character, container, zombieDensity, isJunk))) continue;
                spawnItem = ItemPickerJava.tryAddItemToContainer(container, itemName, containerDist);
                if (spawnItem == null) {
                    return;
                }
                zombieDensity2 = ItemPickerJava.getAdjustedZombieDensity(zombieDensity, scriptItem, isJunk);
                ItemConfigurator.ConfigureItem(spawnItem, pickInfo, isJunk, zombieDensity2);
                if (!isShop) {
                    ItemPickerJava.checkStashItem(spawnItem, containerDist);
                }
                if (container.getType().equals("freezer") && spawnItem instanceof Food && (food = (Food)spawnItem).isFreezing()) {
                    food.freeze();
                }
                if (!(spawnItem instanceof Key)) ** GOTO lbl-1000
                key = (Key)spawnItem;
                if (spawnItem.hasTag(ItemTag.BUILDING_KEY) && !container.getType().equals("inventoryfemale") && !container.getType().equals("inventorymale")) {
                    key.takeKeyId();
                    if (container.getSourceGrid() != null && container.getSourceGrid().getBuilding() != null && container.getSourceGrid().getBuilding().getDef() != null) {
                        def = container.getSourceGrid().getBuilding().getDef();
                        keys = def.getKeySpawned();
                        maxKeys = container.getSourceGrid().getBuilding().getRoomsNumber() / 10 + 1;
                        if (maxKeys < 2) {
                            maxKeys = 2;
                        }
                        if (keys <= maxKeys && container.getCountTagRecurse(ItemTag.BUILDING_KEY) <= 1) {
                            def.setKeySpawned(keys + 1);
                            KeyNamer.nameKey(spawnItem, container.getSourceGrid());
                        } else {
                            container.Remove(spawnItem);
                        }
                    } else {
                        container.Remove(spawnItem);
                    }
                } else if (spawnItem instanceof Key && spawnItem.hasTag(ItemTag.BUILDING_KEY) && (container.getType().equals("inventoryfemale") || container.getType().equals("inventorymale"))) {
                    container.Remove(spawnItem);
                }
                if (spawnItem instanceof Key && (spawnItem.getFullType().equals("Base.CarKey") || spawnItem.hasTag(ItemTag.CAR_KEY))) {
                    ItemPickerJava.addVehicleKeyAsLoot(spawnItem, container);
                }
                if ((mediaCat = spawnItem.getScriptItem().getRecordedMediaCat()) != null) {
                    recordedMedia = ZomboidRadio.getInstance().getRecordedMedia();
                    mediaData = recordedMedia.getRandomFromCategory(mediaCat);
                    if (mediaData == null) {
                        container.Remove(spawnItem);
                        if ("Home-VHS".equalsIgnoreCase(mediaCat)) {
                            mediaData = recordedMedia.getRandomFromCategory("Retail-VHS");
                            if (mediaData == null) {
                                return;
                            }
                            spawnItem = ItemSpawner.spawnItem("Base.VHS_Retail", container);
                            if (spawnItem == null) {
                                return;
                            }
                            spawnItem.setRecordedMediaData(mediaData);
                        }
                        return;
                    }
                    spawnItem.setRecordedMediaData(mediaData);
                }
                if (!containerDist.noAutoAge) {
                    spawnItem.setAutoAge();
                }
                if (!isTrash && ItemPickerJava.WeaponUpgradeMap.containsKey(spawnItem.getType())) {
                    ItemPickerJava.DoWeaponUpgrade(spawnItem);
                }
                if (!(spawnItem instanceof DrainableComboItem)) ** GOTO lbl-1000
                comboItem = (DrainableComboItem)spawnItem;
                if (spawnItem.hasTag(ItemTag.LESS_FULL) && !isShop && !isTrash && Rand.Next(100) < 80) {
                    comboItem.randomizeUses();
                } else if (spawnItem instanceof DrainableComboItem) {
                    drainableComboItem = (DrainableComboItem)spawnItem;
                    if (!isShop && !isTrash && Rand.Next(100) < 40) {
                        drainableComboItem.randomizeUses();
                    }
                }
                if (isEmptyFluidContainer && spawnItem.hasComponent(ComponentType.FluidContainer)) {
                    spawnItem.getFluidContainer().Empty();
                }
                if (!isShop && !isTrash && (spawnItem.hasTag(ItemTag.SHOW_CONDITION) || spawnItem instanceof HandWeapon || spawnItem.hasSharpness()) && Rand.Next(100) < 40) {
                    spawnItem.randomizeGeneralCondition();
                }
                if (!isTrash && spawnItem instanceof HandWeapon) {
                    weapon = (HandWeapon)spawnItem;
                    if (containerDist.gunStorage) {
                        ItemPickerJava.doGunStorageContainer(containerDist.dontSpawnAmmo, weapon, container);
                    } else if (!containerDist.dontSpawnAmmo && Rand.Next(100) < 90) {
                        weapon.randomizeFirearmAsLoot();
                    }
                }
                if (!isShop && spawnItem instanceof InventoryContainer) {
                    inventoryContainer = (InventoryContainer)spawnItem;
                    if (containerDist.bags != null && !spawnItem.hasTag(ItemTag.BAGS_FILL_EXCEPTION)) {
                        ItemPickerJava.rollContainerItemInternal(pickInfo, inventoryContainer, character, containerDist.bags);
                        LuaEventManager.triggerEvent("OnFillContainer", "Container", spawnItem.getType(), containerDist.bags);
                    } else if (ItemPickerJava.containers.containsKey(spawnItem.getType())) {
                        itemPickerContainer = ItemPickerJava.containers.get(spawnItem.getType());
                        if (doItemContainer && Rand.Next(itemPickerContainer.fillRand) == 0) {
                            ItemPickerJava.rollContainerItemInternal(pickInfo, inventoryContainer, character, ItemPickerJava.containers.get(spawnItem.getType()));
                            if (ItemPickerJava.containers.get((Object)spawnItem.getType()).junk != null) {
                                ItemPickerJava.rollContainerItemInternal(pickInfo, inventoryContainer, character, ItemPickerJava.containers.get((Object)spawnItem.getType()).junk, true);
                            }
                            if (spawnItem.hasTag(ItemTag.NEVER_EMPTY) && inventoryContainer.getItemContainer().isEmpty()) {
                                container.Remove(spawnItem);
                            } else {
                                LuaEventManager.triggerEvent("OnFillContainer", "Container", spawnItem.getType(), inventoryContainer.getItemContainer());
                            }
                        }
                    }
                }
                if (spawnItem instanceof Food) {
                    food = (Food)spawnItem;
                    if (spawnItem.isCookable() && (containerDist.cookFood || containerDist.canBurn) && food.getReplaceOnCooked() == null) {
                        if (containerDist.canBurn && food.getMinutesToBurn() > 0.0f && Rand.Next(100) < 25) {
                            food.setBurnt(true);
                        } else {
                            food.setCooked(true);
                            food.setAutoAge();
                        }
                    }
                }
                if (containerDist.isTrash) {
                    ItemPickerJava.trashItem(spawnItem);
                }
                if (containerDist.isWorn) {
                    ItemPickerJava.wearDownItem(spawnItem);
                }
                if (containerDist.isRotten) {
                    ItemPickerJava.rotItem(spawnItem);
                }
                if (spawnItem.hasTag(ItemTag.REGIONAL) && (sq = container.getSquare()) != null) {
                    ItemPickerJava.onCreateRegion(spawnItem, ItemPickerJava.getSquareRegion(sq));
                }
                if (spawnItem instanceof Clothing) {
                    clothing = (Clothing)spawnItem;
                    if (dirtyClothes || wetClothes) {
                        if (dirtyClothes) {
                            clothing.randomizeCondition(0, 75, 1, 0);
                        }
                        if (wetClothes) {
                            clothing.randomizeCondition(100, 0, 0, 0);
                        }
                    }
                }
                if (!(StringUtils.isNullOrEmpty(spawnItem.getScriptItem().getSpawnWith()) || isTrash || containerDist.isWorn || (spawnWithItem = ItemSpawner.spawnItem(spawnItem.getScriptItem().getSpawnWith(), container)) == null)) {
                    spawnWithItem.copyClothing(spawnItem);
                }
                if (parent != null && spawnItem.hasTag(ItemTag.APPLY_OWNER_NAME) && parent instanceof IsoDeadBody && (deadBody = (IsoDeadBody)parent).getDescriptor() != null) {
                    spawnItem.nameAfterDescriptor(deadBody.getDescriptor());
                } else if (parent != null && spawnItem.hasTag(ItemTag.MONOGRAM_OWNER_NAME) && parent instanceof IsoDeadBody && (isoDeadBody = (IsoDeadBody)parent).getDescriptor() != null) {
                    spawnItem.monogramAfterDescriptor(isoDeadBody.getDescriptor());
                }
                if (spawnItem.hasTag(ItemTag.SPAWN_FULL_UNLESS_LAUNDRY) && !laundry) {
                    spawnItem.setCurrentUses(spawnItem.getMaxUses());
                }
                if (!isTrash && !container.isCorpse()) {
                    ItemPickerJava.itemSpawnSanityCheck(spawnItem, container);
                } else {
                    ItemPickerJava.itemSpawnSanityCheck(spawnItem);
                }
                if (!containerDist.onlyOne) continue;
                return;
            }
        }
    }

    private static void checkStashItem(InventoryItem spawnItem, ItemPickerContainer containerDist) {
        MapItem mapItem;
        if (containerDist.stashChance > 0 && spawnItem instanceof MapItem && !StringUtils.isNullOrEmpty((mapItem = (MapItem)spawnItem).getMapID())) {
            spawnItem.setStashChance(containerDist.stashChance);
        }
        StashSystem.checkStashItem(spawnItem);
    }

    public static void rollContainerItem(InventoryContainer bag, IsoGameCharacter character, ItemPickerContainer containerDist) {
        ItemPickInfo pickInfo = ItemPickInfo.GetPickInfo(bag.getItemContainer(), ItemPickInfo.Caller.RollContainerItem);
        ItemPickerJava.rollContainerItemInternal(pickInfo, bag, character, containerDist);
        if (containerDist.junk != null) {
            ItemPickerJava.rollContainerItemInternal(pickInfo, bag, character, containerDist.junk, true);
        }
    }

    private static void rollContainerItemInternal(ItemPickInfo itemPickInfo, InventoryContainer bag, IsoGameCharacter character, ItemPickerContainer containerDist) {
        ItemPickerJava.rollContainerItemInternal(itemPickInfo, bag, character, containerDist, false);
    }

    /*
     * Unable to fully structure code
     */
    private static void rollContainerItemInternal(ItemPickInfo itemPickInfo, InventoryContainer bag, IsoGameCharacter character, ItemPickerContainer containerDist, boolean isJunk) {
        block48: {
            if (containerDist == null) break block48;
            parent = null;
            if (bag.getOutermostContainer() != null && bag.getOutermostContainer().getParent() != null) {
                parent = bag.getOutermostContainer().getParent();
            }
            container = bag.getInventory();
            zombieDensity = ItemPickerJava.getZombieDensityFactor(containerDist, container);
            rolls = (int)((double)containerDist.rolls * SandboxOptions.instance.rollsMultiplier.getValue());
            rolls = Math.max(rolls, 1);
            for (m = 0; m < rolls; ++m) {
                items = containerDist.items;
                for (i = 0; i < items.length; ++i) {
                    item = items[i];
                    itemName = item.itemName;
                    scriptItem = ScriptManager.instance.FindItem(itemName);
                    isEmptyFluidContainer = false;
                    if (scriptItem == null && itemName.endsWith("Empty") && (scriptItem = ScriptManager.instance.FindItem(itemName = itemName.substring(0, itemName.length() - 5), true)) != null) {
                        if (!scriptItem.containsComponent(ComponentType.FluidContainer)) continue;
                        isEmptyFluidContainer = true;
                    }
                    if (!((float)Rand.Next(10000) < ItemPickerJava.getActualSpawnChance(item, character, container, zombieDensity, isJunk))) continue;
                    spawnItem = ItemPickerJava.tryAddItemToContainer(container, itemName, containerDist);
                    if (spawnItem == null) {
                        return;
                    }
                    ItemConfigurator.ConfigureItem(spawnItem, itemPickInfo, false, 0.0f);
                    mapItem = Type.tryCastTo(spawnItem, MapItem.class);
                    if (mapItem != null && !StringUtils.isNullOrEmpty(mapItem.getMapID()) && containerDist.maxMap > 0) {
                        totalMap = 0;
                        for (j = 0; j < container.getItems().size(); ++j) {
                            invMap = Type.tryCastTo(container.getItems().get(j), MapItem.class);
                            if (invMap == null || StringUtils.isNullOrEmpty(invMap.getMapID())) continue;
                            ++totalMap;
                        }
                        if (totalMap > containerDist.maxMap) {
                            container.Remove(spawnItem);
                        }
                    }
                    ItemPickerJava.checkStashItem(spawnItem, containerDist);
                    if (!containerDist.isTrash && ItemPickerJava.WeaponUpgradeMap.containsKey(spawnItem.getType())) {
                        ItemPickerJava.DoWeaponUpgrade(spawnItem);
                    }
                    if (!containerDist.isTrash && spawnItem instanceof HandWeapon) {
                        weapon = (HandWeapon)spawnItem;
                        if (!containerDist.dontSpawnAmmo && Rand.Next(100) < 90) {
                            weapon.randomizeFirearmAsLoot();
                        }
                    }
                    if (!containerDist.isTrash && (spawnItem.hasTag(ItemTag.SHOW_CONDITION) || spawnItem instanceof HandWeapon || spawnItem.hasSharpness())) {
                        spawnItem.randomizeGeneralCondition();
                    }
                    if (container.getType().equals("freezer") && spawnItem instanceof Food && (food = (Food)spawnItem).isFreezing()) {
                        food.freeze();
                    }
                    if (!(spawnItem instanceof DrainableComboItem)) ** GOTO lbl-1000
                    comboItem = (DrainableComboItem)spawnItem;
                    if (spawnItem.hasTag(ItemTag.LESS_FULL) && Rand.Next(100) < 80) {
                        comboItem.randomizeUses();
                    } else if (spawnItem instanceof DrainableComboItem) {
                        drainableComboItem = (DrainableComboItem)spawnItem;
                        if (Rand.Next(100) < 40) {
                            drainableComboItem.randomizeUses();
                        }
                    }
                    if (isEmptyFluidContainer && spawnItem.hasComponent(ComponentType.FluidContainer)) {
                        spawnItem.getFluidContainer().Empty();
                    }
                    outside = bag.getOutermostContainer();
                    if (spawnItem instanceof Key) {
                        key = (Key)spawnItem;
                        if (spawnItem.hasTag(ItemTag.BUILDING_KEY) && outside != null && outside.getType() != null) {
                            key.takeKeyId();
                            def = null;
                            if (outside.getSquare() != null && outside.getSquare().getBuilding() != null && outside.getSquare().getBuilding().getDef() != null) {
                                def = outside.getSquare().getBuilding().getDef();
                            }
                            if (def != null) {
                                keys = def.getKeySpawned();
                                maxKeys = outside.getSquare().getBuilding().getRoomsNumber() / 5 + 1;
                                if (maxKeys < 2) {
                                    maxKeys = 2;
                                }
                                if (keys <= maxKeys && outside.getCountTagRecurse(ItemTag.BUILDING_KEY) <= 1) {
                                    def.setKeySpawned(keys + 1);
                                    KeyNamer.nameKey(spawnItem, outside.getSquare());
                                } else {
                                    container.Remove(spawnItem);
                                }
                            } else {
                                container.Remove(spawnItem);
                            }
                        }
                    }
                    if ((mediaCat = spawnItem.getScriptItem().getRecordedMediaCat()) != null) {
                        recordedMedia = ZomboidRadio.getInstance().getRecordedMedia();
                        mediaData = recordedMedia.getRandomFromCategory(mediaCat);
                        if (mediaData == null) {
                            container.Remove(spawnItem);
                            if ("Home-VHS".equalsIgnoreCase(mediaCat)) {
                                mediaData = recordedMedia.getRandomFromCategory("Retail-VHS");
                                if (mediaData == null) {
                                    return;
                                }
                                spawnItem = ItemSpawner.spawnItem("Base.VHS_Retail", container);
                                if (spawnItem == null) {
                                    return;
                                }
                                spawnItem.setRecordedMediaData(mediaData);
                            }
                            return;
                        }
                        spawnItem.setRecordedMediaData(mediaData);
                    }
                    if (spawnItem instanceof InventoryContainer) {
                        inventoryContainer = (InventoryContainer)spawnItem;
                        if (containerDist.bags != null && !spawnItem.hasTag(ItemTag.BAGS_FILL_EXCEPTION)) {
                            ItemPickerJava.rollContainerItemInternal(itemPickInfo, inventoryContainer, character, containerDist.bags);
                            LuaEventManager.triggerEvent("OnFillContainer", "Container", spawnItem.getType(), containerDist.bags);
                        } else if (ItemPickerJava.containers.containsKey(spawnItem.getType())) {
                            itemPickerContainer = ItemPickerJava.containers.get(spawnItem.getType());
                            ItemPickerJava.rollContainerItemInternal(itemPickInfo, inventoryContainer, character, ItemPickerJava.containers.get(spawnItem.getType()));
                            if (ItemPickerJava.containers.get((Object)spawnItem.getType()).junk != null) {
                                ItemPickerJava.rollContainerItemInternal(itemPickInfo, inventoryContainer, character, ItemPickerJava.containers.get((Object)spawnItem.getType()).junk, true);
                            }
                            if (spawnItem.hasTag(ItemTag.NEVER_EMPTY) && inventoryContainer.getItemContainer().isEmpty()) {
                                container.Remove(spawnItem);
                            } else {
                                LuaEventManager.triggerEvent("OnFillContainer", "Container", spawnItem.getType(), inventoryContainer.getItemContainer());
                            }
                        }
                    }
                    if (spawnItem instanceof Key && (spawnItem.getFullType().equals("Base.CarKey") || spawnItem.hasTag(ItemTag.CAR_KEY))) {
                        ItemPickerJava.addVehicleKeyAsLoot(spawnItem, container);
                    }
                    if (!container.getType().equals("freezer")) {
                        spawnItem.setAutoAge();
                    }
                    if (containerDist.isTrash) {
                        ItemPickerJava.trashItem(spawnItem);
                    }
                    if (containerDist.isWorn) {
                        ItemPickerJava.wearDownItem(spawnItem);
                    }
                    if (spawnItem.hasTag(ItemTag.REGIONAL) && outside != null && outside.getSquare() != null) {
                        ItemPickerJava.onCreateRegion(spawnItem, ItemPickerJava.getSquareRegion(outside.getSquare()));
                    }
                    if (!(StringUtils.isNullOrEmpty(spawnItem.getScriptItem().getSpawnWith()) || containerDist.isWorn || containerDist.isTrash || (spawnWithItem = ItemSpawner.spawnItem(spawnItem.getScriptItem().getSpawnWith(), container)) == null)) {
                        spawnWithItem.copyClothing(spawnItem);
                    }
                    if (parent != null && spawnItem.hasTag(ItemTag.APPLY_OWNER_NAME) && parent instanceof IsoDeadBody && (deadBody = (IsoDeadBody)parent).getDescriptor() != null) {
                        spawnItem.nameAfterDescriptor(deadBody.getDescriptor());
                    } else if (parent != null && spawnItem.hasTag(ItemTag.MONOGRAM_OWNER_NAME) && parent instanceof IsoDeadBody && (isoDeadBody = (IsoDeadBody)parent).getDescriptor() != null) {
                        spawnItem.monogramAfterDescriptor(isoDeadBody.getDescriptor());
                    }
                    if (spawnItem.hasTag(ItemTag.SPAWN_FULL_UNLESS_LAUNDRY)) {
                        spawnItem.setCurrentUses(spawnItem.getMaxUses());
                    }
                    ItemPickerJava.itemSpawnSanityCheck(spawnItem);
                    if (!containerDist.onlyOne) continue;
                    return;
                }
            }
        }
    }

    private static void DoWeaponUpgrade(InventoryItem item) {
        ItemPickerUpgradeWeapons itemPickerUpgradeWeapons = WeaponUpgradeMap.get(item.getType());
        if (itemPickerUpgradeWeapons == null) {
            return;
        }
        if (itemPickerUpgradeWeapons.upgrades.isEmpty()) {
            return;
        }
        int randUpgrade = Rand.Next(itemPickerUpgradeWeapons.upgrades.size());
        for (int x = 0; x < randUpgrade; ++x) {
            String s = PZArrayUtil.pickRandom(itemPickerUpgradeWeapons.upgrades);
            Object part = InventoryItemFactory.CreateItem(s);
            if (part == null) continue;
            ((HandWeapon)item).attachWeaponPart((WeaponPart)part);
        }
    }

    public static float getLootModifier(String itemname) {
        Item item = ScriptManager.instance.FindItem(itemname);
        if (item == null && itemname.endsWith("Empty")) {
            itemname = itemname.substring(0, itemname.length() - 5);
            item = ScriptManager.instance.FindItem(itemname);
        }
        if (item == null) {
            return otherLootModifier;
        }
        if (SandboxOptions.instance.lootItemRemovalListContains(itemname) || SandboxOptions.instance.lootItemRemovalListContains(ScriptManager.getItemName(itemname))) {
            return 0.0f;
        }
        String lootType = ItemPickerJava.getLootType(item);
        return ItemPickerJava.getLootModifierFromType(lootType);
    }

    public static float getLootModifierFromType(String lootType) {
        float lootModifier = otherLootModifier;
        if (Objects.equals(lootType, GeneratorLootType)) {
            return ItemPickerJava.doSandboxSettings(SandboxOptions.instance.generatorSpawning.getValue());
        }
        if (Objects.equals(lootType, MementoLootType)) {
            return mementoLootModifier;
        }
        if (Objects.equals(lootType, MedicalLootType)) {
            return medicalLootModifier;
        }
        if (Objects.equals(lootType, MechanicsLootType)) {
            return mechanicsLootModifier;
        }
        if (Objects.equals(lootType, MaterialLootType)) {
            return materialLootModifier;
        }
        if (Objects.equals(lootType, FarmingLootType)) {
            return farmingLootModifier;
        }
        if (Objects.equals(lootType, ToolLootType)) {
            return toolLootModifier;
        }
        if (Objects.equals(lootType, CookwareLootType)) {
            return cookwareLootModifier;
        }
        if (Objects.equals(lootType, SurvivalGearsLootType)) {
            return survivalGearsLootModifier;
        }
        if (Objects.equals(lootType, CannedFoodLootType)) {
            return cannedFoodLootModifier;
        }
        if (Objects.equals(lootType, FoodLootType)) {
            return foodLootModifier;
        }
        if (Objects.equals(lootType, AmmoLootType)) {
            return ammoLootModifier;
        }
        if (Objects.equals(lootType, WeaponLootType)) {
            return weaponLootModifier;
        }
        if (Objects.equals(lootType, RangedWeaponLootType)) {
            return rangedWeaponLootModifier;
        }
        if (Objects.equals(lootType, KeyLootType)) {
            return keyLootModifier;
        }
        if (Objects.equals(lootType, ContainerLootType)) {
            return containerLootModifier;
        }
        if (Objects.equals(lootType, SkillBookLootType)) {
            return skillBookLootModifier;
        }
        if (Objects.equals(lootType, RecipeResourceLootType)) {
            return recipeResourceLootModifier;
        }
        if (Objects.equals(lootType, LiteratureLootType)) {
            return literatureLootModifier;
        }
        if (Objects.equals(lootType, ClothingLootType)) {
            return clothingLootModifier;
        }
        if (Objects.equals(lootType, MediaLootType)) {
            return mediaLootModifier;
        }
        return lootModifier;
    }

    public static String getLootType(Item item) {
        if (Objects.equals(item.getName(), GeneratorLootType) || Objects.equals(item.getFullName(), "Base.Generator") || item.hasTag(ItemTag.GENERATOR)) {
            return GeneratorLootType;
        }
        if (item.isMementoLoot()) {
            return MementoLootType;
        }
        if (item.isMedicalLoot()) {
            return MedicalLootType;
        }
        if (item.isMechanicsLoot()) {
            return MechanicsLootType;
        }
        if (item.isMaterialLoot()) {
            return MaterialLootType;
        }
        if (item.isFarmingLoot()) {
            return FarmingLootType;
        }
        if (item.isToolLoot()) {
            return ToolLootType;
        }
        if (item.isCookwareLoot()) {
            return CookwareLootType;
        }
        if (item.isSurvivalGearLoot()) {
            return SurvivalGearsLootType;
        }
        if (item.isItemType(ItemType.FOOD) || FoodLootType.equals(item.getDisplayCategory())) {
            if (item.cannedFood || item.getDaysFresh() == 1000000000 || !item.isItemType(ItemType.FOOD)) {
                return CannedFoodLootType;
            }
            return FoodLootType;
        }
        if (AmmoLootType.equals(item.getDisplayCategory()) || item.hasTag(ItemTag.AMMO_CASE) || item.isItemType(ItemType.NORMAL) && item.getAmmoType() != null) {
            return AmmoLootType;
        }
        if (item.isItemType(ItemType.WEAPON) && !item.isRanged()) {
            return WeaponLootType;
        }
        if (item.isItemType(ItemType.WEAPON_PART) || item.isItemType(ItemType.WEAPON) && item.isRanged() || item.hasTag(ItemTag.FIREARM_LOOT)) {
            return RangedWeaponLootType;
        }
        if (item.isItemType(ItemType.KEY) || item.isItemType(ItemType.KEY_RING) || item.hasTag(ItemTag.KEY_RING)) {
            return KeyLootType;
        }
        if (item.capacity > 0 || item.isItemType(ItemType.CONTAINER) || "Bag".equals(item.getDisplayCategory())) {
            return ContainerLootType;
        }
        if (ItemDisplayCategory.SKILL_BOOK.toString().equals(item.getDisplayCategory())) {
            return SkillBookLootType;
        }
        if (ItemDisplayCategory.RECIPE_RESOURCE.toString().equals(item.getDisplayCategory())) {
            return RecipeResourceLootType;
        }
        if (item.isItemType(ItemType.LITERATURE)) {
            return LiteratureLootType;
        }
        if (item.isItemType(ItemType.CLOTHING)) {
            return ClothingLootType;
        }
        if (item.getRecordedMediaCat() != null) {
            return MediaLootType;
        }
        return OtherLootType;
    }

    public static void updateOverlaySprite(IsoObject obj) {
        ContainerOverlays.instance.updateContainerOverlaySprite(obj);
    }

    public static void doOverlaySprite(IsoGridSquare sq) {
        if (GameClient.client) {
            return;
        }
        if (sq == null || sq.getRoom() == null || sq.isOverlayDone()) {
            return;
        }
        PZArrayList<IsoObject> objects = sq.getObjects();
        for (int i = 0; i < objects.size(); ++i) {
            IsoObject obj = objects.get(i);
            if (obj != null && obj.getContainer() != null && !obj.getContainer().isExplored()) {
                ItemPickerJava.fillContainer(obj.getContainer(), IsoPlayer.getInstance());
                obj.getContainer().setExplored(true);
                if (GameServer.server) {
                    LuaManager.GlobalObject.sendItemsInContainer(obj, obj.getContainer());
                }
            }
            ItemPickerJava.updateOverlaySprite(obj);
        }
        sq.setOverlayDone(true);
    }

    public static ItemPickerContainer getItemContainer(String room, String container, String proceduralName, boolean junk) {
        ItemPickerRoom iproom = rooms.get(room);
        if (iproom != null) {
            ItemPickerContainer ipcont = iproom.containers.get(container);
            if (ipcont != null && ipcont.procedural) {
                ArrayList<ProceduralItem> procList = ipcont.proceduralItems;
                for (int i = 0; i < procList.size(); ++i) {
                    ProceduralItem proceduralItem = procList.get(i);
                    if (!proceduralName.equals(proceduralItem.name)) continue;
                    ItemPickerContainer containerDistToSpawn = ProceduralDistributions.get(proceduralName);
                    if (containerDistToSpawn.junk != null && junk) {
                        return containerDistToSpawn.junk;
                    }
                    if (junk) continue;
                    return containerDistToSpawn;
                }
            }
            if (junk && ipcont != null) {
                return ipcont.junk;
            }
            return ipcont;
        }
        return null;
    }

    public static void keyNamerBuilding(InventoryItem item, IsoGridSquare square) {
        KeyNamer.nameKey(item, square);
    }

    public static void trashItem(InventoryItem spawnItem) {
        HandWeapon handWeapon;
        if (spawnItem.hasTag(ItemTag.SHOW_CONDITION) || spawnItem instanceof HandWeapon && (handWeapon = (HandWeapon)spawnItem).getPhysicsObject() == null) {
            if (Rand.Next(100) < 95) {
                spawnItem.setConditionNoSound(1);
            } else {
                spawnItem.setConditionNoSound(1);
            }
        }
        if (spawnItem.hasHeadCondition()) {
            if (Rand.NextBool(2)) {
                spawnItem.setHeadCondition(Rand.Next(1, spawnItem.getHeadConditionMax()));
            } else {
                spawnItem.setHeadCondition(1);
            }
        }
        if (spawnItem.hasSharpness()) {
            if (Rand.NextBool(2)) {
                spawnItem.setSharpness(Rand.Next(0.0f, spawnItem.getMaxSharpness()));
            } else {
                spawnItem.setSharpness(0.0f);
            }
        }
        if (spawnItem instanceof DrainableComboItem) {
            DrainableComboItem drainableComboItem = (DrainableComboItem)spawnItem;
            if (Rand.Next(100) < 90) {
                spawnItem.setCurrentUses(1);
            } else {
                drainableComboItem.randomizeUses();
            }
        }
        Item scriptItem = spawnItem.getScriptItem();
        if (spawnItem instanceof Food) {
            Food food = (Food)spawnItem;
            if (!scriptItem.cannedFood || !scriptItem.cantEat) {
                boolean burnt = false;
                boolean gross = false;
                if (!food.hasTag(ItemTag.VERMIN) && food.isCookable() && !scriptItem.cannedFood && food.getReplaceOnCooked() == null && Rand.Next(100) < 75) {
                    if (Rand.Next(100) < 50) {
                        food.setCooked(true);
                    } else {
                        burnt = true;
                        food.setBurnt(true);
                    }
                }
                if (!food.isRotten() && food.getOffAgeMax() < 1000000000 && Rand.Next(100) < 95) {
                    food.setRotten(true);
                    food.setAge(food.getOffAgeMax());
                    gross = true;
                } else if (food.isFresh() && food.getOffAge() < 1000000000 && Rand.Next(100) < 95) {
                    food.setAge(food.getOffAge());
                    gross = true;
                }
                if (gross && Rand.Next(2) == 0) {
                    gross = false;
                }
                if (food.isbDangerousUncooked() && !food.isCooked()) {
                    gross = true;
                }
                if (food.hasTag(ItemTag.VERMIN)) {
                    gross = true;
                }
                double baseHunger = (double)(food.getBaseHunger() * 100.0f * -1.0f) + 0.1;
                double hungerChange = (double)(food.getHungerChange() * 100.0f * -1.0f) + 0.1;
                if (hungerChange < baseHunger) {
                    baseHunger = hungerChange;
                }
                if (!burnt && !gross && Rand.Next(100) != 0) {
                    if (baseHunger >= 4.0) {
                        int roll = Rand.Next(8);
                        if (roll == 0) {
                            food.multiplyFoodValues(0.75f);
                        } else if (roll <= 2) {
                            food.multiplyFoodValues(0.5f);
                        } else {
                            food.multiplyFoodValues(0.25f);
                        }
                    } else if (baseHunger >= 2.0) {
                        food.multiplyFoodValues(0.5f);
                    }
                }
            }
        }
        if (spawnItem instanceof Clothing) {
            Clothing clothing = (Clothing)spawnItem;
            clothing.randomizeCondition(25, 95, 10, 75);
        }
    }

    public static void trashItemLooted(InventoryItem spawnItem) {
        HandWeapon handWeapon;
        if ((spawnItem.hasTag(ItemTag.SHOW_CONDITION) || spawnItem instanceof HandWeapon && (handWeapon = (HandWeapon)spawnItem).getPhysicsObject() == null) && Rand.Next(100) < 50) {
            spawnItem.setCondition(1, false);
        }
        if (spawnItem.hasHeadCondition()) {
            spawnItem.setHeadCondition(Rand.Next(1, spawnItem.getHeadConditionMax()));
        }
        if (spawnItem.hasSharpness()) {
            spawnItem.setSharpness(Rand.Next(0.0f, spawnItem.getMaxSharpness()));
        }
        if (spawnItem instanceof DrainableComboItem) {
            DrainableComboItem drainableComboItem = (DrainableComboItem)spawnItem;
            if (Rand.Next(100) < 75) {
                spawnItem.setCurrentUses(1);
            } else {
                drainableComboItem.randomizeUses();
            }
        }
        Item scriptItem = spawnItem.getScriptItem();
        if (spawnItem instanceof Food) {
            Food food = (Food)spawnItem;
            if (!scriptItem.cannedFood || !scriptItem.cantEat) {
                boolean gross;
                boolean bl = gross = food.isbDangerousUncooked() && !food.isCooked();
                if (food.hasTag(ItemTag.VERMIN)) {
                    gross = true;
                }
                double baseHunger = (double)(food.getBaseHunger() * 100.0f * -1.0f) + 0.1;
                double hungerChange = (double)(food.getHungerChange() * 100.0f * -1.0f) + 0.1;
                if (hungerChange < baseHunger) {
                    baseHunger = hungerChange;
                }
                if (!gross && Rand.Next(100) < 75) {
                    if (baseHunger >= 4.0) {
                        int roll = Rand.Next(8);
                        if (roll == 0) {
                            food.multiplyFoodValues(0.75f);
                        } else if (roll <= 2) {
                            food.multiplyFoodValues(0.5f);
                        } else {
                            food.multiplyFoodValues(0.25f);
                        }
                    } else if (baseHunger >= 2.0) {
                        food.multiplyFoodValues(0.5f);
                    }
                }
            }
        }
        if (spawnItem instanceof Clothing) {
            Clothing clothing = (Clothing)spawnItem;
            clothing.randomizeCondition(10, 50, 10, 50);
        }
    }

    public static void trashItemRats(InventoryItem spawnItem) {
        HandWeapon handWeapon;
        if ((spawnItem.hasTag(ItemTag.SHOW_CONDITION) || spawnItem instanceof HandWeapon && (handWeapon = (HandWeapon)spawnItem).getPhysicsObject() == null) && Rand.Next(100) < 75) {
            ItemPickerJava.wearDownItem(spawnItem);
        }
        Item scriptItem = spawnItem.getScriptItem();
        if (spawnItem instanceof Food) {
            Food food = (Food)spawnItem;
            if (!scriptItem.cannedFood || !scriptItem.cantEat) {
                double baseHunger = (double)(food.getBaseHunger() * 100.0f * -1.0f) + 0.1;
                double hungerChange = (double)(food.getHungerChange() * 100.0f * -1.0f) + 0.1;
                if (hungerChange < baseHunger) {
                    baseHunger = hungerChange;
                }
                if (baseHunger >= 4.0) {
                    int roll = Rand.Next(8);
                    if (roll == 0) {
                        food.multiplyFoodValues(0.75f);
                    } else if (roll <= 2) {
                        food.multiplyFoodValues(0.5f);
                    } else {
                        food.multiplyFoodValues(0.25f);
                    }
                } else if (baseHunger >= 2.0) {
                    food.multiplyFoodValues(0.5f);
                }
            }
        }
        if (spawnItem instanceof Clothing) {
            Clothing clothing = (Clothing)spawnItem;
            clothing.randomizeCondition(25, 95, 10, 95);
        }
    }

    public static void wearDownItem(InventoryItem spawnItem) {
        HandWeapon handWeapon;
        if (spawnItem.hasTag(ItemTag.SHOW_CONDITION) || spawnItem instanceof HandWeapon && (handWeapon = (HandWeapon)spawnItem).getPhysicsObject() == null) {
            if (Rand.Next(100) < 25) {
                spawnItem.setCondition(1, false);
            } else {
                int roll1 = Rand.Next(spawnItem.getConditionMax());
                int roll2 = Rand.Next(spawnItem.getConditionMax());
                int roll3 = Rand.Next(spawnItem.getConditionMax());
                if (roll2 < roll1) {
                    roll1 = roll2;
                }
                if (roll3 < roll1) {
                    roll1 = roll3;
                }
                spawnItem.setCondition(roll1 + 1, false);
            }
        }
        if (spawnItem.hasHeadCondition()) {
            spawnItem.setHeadCondition(Rand.Next(1, spawnItem.getHeadConditionMax()));
        }
        if (spawnItem.hasSharpness()) {
            spawnItem.setSharpness(Rand.Next(0.0f, spawnItem.getMaxSharpness()));
        }
        if (spawnItem instanceof DrainableComboItem) {
            DrainableComboItem drainableComboItem = (DrainableComboItem)spawnItem;
            if (Rand.Next(100) < 75) {
                spawnItem.setCurrentUses(1);
            } else {
                drainableComboItem.randomizeUses();
            }
        }
        Item scriptItem = spawnItem.getScriptItem();
        if (spawnItem instanceof Food) {
            Food food = (Food)spawnItem;
            if (!scriptItem.cannedFood || !scriptItem.cantEat) {
                boolean burnt = false;
                boolean gross = false;
                if (!food.hasTag(ItemTag.VERMIN) && food.isCookable() && food.getReplaceOnCooked() == null && Rand.Next(100) < 50) {
                    if (Rand.Next(100) < 50) {
                        food.setCooked(true);
                    } else {
                        burnt = true;
                        food.setBurnt(true);
                    }
                }
                if (!food.isRotten() && food.getOffAgeMax() < 1000000000 && Rand.Next(100) < 75) {
                    food.setRotten(true);
                    food.setAge(food.getOffAgeMax());
                    gross = true;
                } else if (food.isFresh() && food.getOffAge() < 1000000000 && Rand.Next(100) < 75) {
                    food.setAge(food.getOffAge());
                    gross = true;
                }
                if (gross && Rand.Next(2) == 0) {
                    gross = false;
                }
                if (food.isbDangerousUncooked() && !food.isCooked()) {
                    gross = true;
                }
                if (food.hasTag(ItemTag.VERMIN)) {
                    gross = true;
                }
                double baseHunger = (double)(food.getBaseHunger() * 100.0f * -1.0f) + 0.1;
                double hungerChange = (double)(food.getHungerChange() * 100.0f * -1.0f) + 0.1;
                if (hungerChange < baseHunger) {
                    baseHunger = hungerChange;
                }
                if (!burnt && !gross && Rand.Next(100) < 75) {
                    if (baseHunger >= 4.0) {
                        int roll = Rand.Next(8);
                        if (roll <= 2) {
                            food.multiplyFoodValues(0.75f);
                        } else if (roll <= 4) {
                            food.multiplyFoodValues(0.5f);
                        } else {
                            food.multiplyFoodValues(0.25f);
                        }
                    } else if (baseHunger >= 2.0) {
                        food.multiplyFoodValues(0.5f);
                    }
                }
            }
        }
        if (spawnItem instanceof Clothing) {
            Clothing clothing = (Clothing)spawnItem;
            clothing.randomizeCondition(0, 25, 1, 25);
        }
    }

    public static void rotItem(InventoryItem spawnItem) {
        Item scriptItem = spawnItem.getScriptItem();
        if (spawnItem instanceof Food) {
            Food food = (Food)spawnItem;
            if (!(scriptItem.cannedFood && scriptItem.cantEat || food.getOffAgeMax() >= 1000000000)) {
                if (food.isRotten()) {
                    return;
                }
                if (Rand.Next(100) < 75) {
                    food.setRotten(true);
                    food.setAge(food.getOffAgeMax());
                } else if (food.isFresh() && Rand.Next(100) < 95) {
                    food.setAge(food.getOffAge());
                }
            }
        }
        if (spawnItem instanceof Clothing) {
            Clothing clothing = (Clothing)spawnItem;
            clothing.randomizeCondition(0, 75, 1, 25);
        }
    }

    public static void spawnLootCarKey(InventoryItem spawnItem, ItemContainer container) {
        ItemPickerJava.spawnLootCarKey(spawnItem, container, container);
    }

    public static void spawnLootCarKey(InventoryItem spawnItem, ItemContainer container, ItemContainer outtermost) {
        ArrayList<BaseVehicle> vehicles = IsoWorld.instance.currentCell.getVehicles();
        if (vehicles.isEmpty()) {
            container.Remove(spawnItem);
        } else {
            BaseVehicle vehicle = vehicles.get(Rand.Next(vehicles.size()));
            if (vehicle != null && !vehicle.isPreviouslyMoved() && ItemPickerJava.isGoodKey(vehicle.getScriptName())) {
                Key key = (Key)spawnItem;
                key.setKeyId(vehicle.getKeyId());
                vehicle.setPreviouslyMoved(true);
                vehicle.keySpawned = 1;
                BaseVehicle.keyNamerVehicle(key, vehicle);
                Color newC = Color.HSBtoRGB(vehicle.colorHue, vehicle.colorSaturation * 0.5f, vehicle.colorValue);
                key.setColor(newC);
                key.setCustomColor(true);
                if ((float)Rand.Next(100) < 1.0f * keyLootModifierD100 && outtermost.getSourceGrid() != null && outtermost.getSourceGrid().getBuilding() != null && outtermost.getSourceGrid().getBuilding().getDef() != null) {
                    vehicle.addBuildingKeyToGloveBox(outtermost.getSourceGrid());
                }
            } else {
                container.Remove(spawnItem);
            }
        }
    }

    public static boolean isGoodKey(String vehicleType) {
        return !vehicleType.contains("Burnt") && !vehicleType.contains("Smashed") && !vehicleType.equals("TrailerAdvert") && !vehicleType.equals("TrailerCover") && !vehicleType.equals("Trailer");
    }

    public static boolean addVehicleKeyAsLoot(InventoryItem spawnItem, ItemContainer container) {
        if (container.getCountTagRecurse(ItemTag.CAR_KEY) < 2) {
            ItemPickerJava.spawnLootCarKey(spawnItem, container);
            return true;
        }
        container.Remove(spawnItem);
        return false;
    }

    public static boolean containerHasZone(ItemContainer container, String zone) {
        return ItemPickerJava.squareHasZone(container.getSourceGrid(), zone);
    }

    public static boolean squareHasZone(IsoGridSquare square, String zone) {
        ArrayList<Zone> metazones = IsoWorld.instance.metaGrid.getZonesAt(square.x, square.y, 0);
        for (int i = 0; i < metazones.size(); ++i) {
            if (!Objects.equals(metazones.get((int)i).name, zone) && !Objects.equals(metazones.get((int)i).type, zone)) continue;
            return true;
        }
        return false;
    }

    public static String getContainerZombiesType(ItemContainer container) {
        if (container == null || container.getSourceGrid() == null) {
            return null;
        }
        String stringThing = ItemPickerJava.getSquareZombiesType(container.getSourceGrid());
        if (KeyNamer.badZones.contains(stringThing)) {
            return null;
        }
        return stringThing;
    }

    public static String getSquareZombiesType(IsoGridSquare square) {
        return square.getSquareZombiesType();
    }

    public static String getSquareBuildingName(IsoGridSquare square) {
        ArrayList<Zone> zones = LuaManager.GlobalObject.getZones(square.x, square.y, 0);
        for (int i = 0; i < zones.size(); ++i) {
            if (!Objects.equals(zones.get((int)i).type, "BuildingName") || KeyNamer.badZones.contains(zones.get((int)i).name)) continue;
            return zones.get((int)i).name;
        }
        return null;
    }

    public static String getSquareRegion(IsoGridSquare square) {
        return square.getSquareRegion();
    }

    public static float getBaseChance(ItemPickerItem item, IsoGameCharacter character, boolean isJunk) {
        String itemName = item.itemName;
        Item scriptItem = ScriptManager.instance.FindItem(itemName);
        return item.chance * ItemPickerJava.getBaseChanceMultiplier(character, isJunk, scriptItem);
    }

    public static float getBaseChanceMultiplier(IsoGameCharacter character, boolean isJunk, Item scriptItem) {
        float multiplier = 1.0f;
        if (isJunk) {
            multiplier *= 1.4f;
        }
        if (scriptItem != null && scriptItem.hasTag(ItemTag.MORE_WHEN_NO_ZOMBIES) && (SandboxOptions.instance.zombies.getValue() == 6 || SandboxOptions.instance.zombieConfig.populationMultiplier.getValue() == 0.0)) {
            multiplier *= 2.0f;
        }
        return multiplier;
    }

    public static float getLootModifier(String itemName, boolean isJunk) {
        float lootModifier = ItemPickerJava.getLootModifier(itemName);
        if (isJunk && lootModifier > 0.0f) {
            lootModifier = 1.0f;
        }
        return lootModifier;
    }

    public static float getAdjustedZombieDensity(float zombieDensity, Item scriptItem, boolean isJunk) {
        if (isJunk || scriptItem != null && scriptItem.ignoreZombieDensity()) {
            return 0.0f;
        }
        return zombieDensity;
    }

    public static float getActualSpawnChance(ItemPickerItem item, IsoGameCharacter character, ItemContainer container, float zombieDensity, boolean isJunk) {
        String itemName = item.itemName;
        Item scriptItem = ScriptManager.instance.FindItem(itemName);
        float lootModifier = ItemPickerJava.getLootModifier(itemName, isJunk);
        if (lootModifier == 0.0f) {
            return 0.0f;
        }
        float baseChance = ItemPickerJava.getBaseChance(item, character, isJunk);
        zombieDensity = ItemPickerJava.getAdjustedZombieDensity(zombieDensity, scriptItem, isJunk);
        float overTimeModifier = container.getSourceGrid() != null ? SandboxOptions.instance.getCurrentLootMultiplier(container.getSourceGrid()) : SandboxOptions.instance.getCurrentLootMultiplier();
        return (baseChance * 100.0f * lootModifier + zombieDensity) * overTimeModifier;
    }

    public static float getZombieDensityFactor(ItemPickerContainer containerDist, ItemContainer container) {
        float zombieDensity = 0.0f;
        if (containerDist.ignoreZombieDensity || IsoWorld.instance == null || SandboxOptions.instance.zombiePopLootEffect.getValue() == 0) {
            return zombieDensity;
        }
        IsoMetaChunk chunk = null;
        if (player != null) {
            chunk = IsoWorld.instance.getMetaChunk(PZMath.fastfloor(player.getX() / 8.0f), PZMath.fastfloor(player.getY() / 8.0f));
        } else if (container.getSourceGrid() != null) {
            chunk = IsoWorld.instance.getMetaChunk(PZMath.fastfloor((float)container.getSourceGrid().getX() / 8.0f), PZMath.fastfloor((float)container.getSourceGrid().getY() / 8.0f));
        }
        if (chunk != null) {
            zombieDensity = chunk.getLootZombieIntensity();
        }
        zombieDensity = Math.min(zombieDensity, zombieDensityCap);
        return zombieDensity * (float)SandboxOptions.instance.zombiePopLootEffect.getValue();
    }

    public static void itemSpawnSanityCheck(InventoryItem spawnItem) {
        ItemPickerJava.itemSpawnSanityCheck(spawnItem, null);
    }

    public static void itemSpawnSanityCheck(InventoryItem spawnItem, ItemContainer container) {
        InventoryContainer inventoryContainer;
        if (container != null && container.isShop() && spawnItem instanceof InventoryContainer) {
            inventoryContainer = (InventoryContainer)spawnItem;
            if (!spawnItem.hasTag(ItemTag.ALWAYS_HAS_STUFF)) {
                inventoryContainer.getItemContainer().getItems().clear();
            }
        }
        if (spawnItem instanceof InventoryContainer) {
            inventoryContainer = (InventoryContainer)spawnItem;
            inventoryContainer.getItemContainer().setExplored(true);
        }
        if (spawnItem instanceof Food && spawnItem.hasTag(ItemTag.SPAWN_COOKED)) {
            Object functionObj;
            spawnItem.setCooked(true);
            if (!StringUtils.isNullOrEmpty(((Food)spawnItem).getOnCooked()) && (functionObj = LuaManager.getFunctionObject(((Food)spawnItem).getOnCooked())) != null) {
                LuaManager.caller.pcallvoid(LuaManager.thread, functionObj, spawnItem);
            }
        }
        spawnItem.unsealIfNotFull();
        if (spawnItem instanceof DrainableComboItem && !spawnItem.isKeepOnDeplete() && spawnItem.getCurrentUses() <= 0) {
            spawnItem.setCurrentUses(1);
        }
    }

    public static String getLootDebugString(IsoObject object) {
        if (!DebugOptions.instance.uiShowContextMenuReportOptions.getValue() || object == null || object.getContainer() == null || object.getContainer().getType() == null || object.getSquare() == null || object.getSquare().getRoom() == null || object.getSquare().getRoom().getRoomDef() == null) {
            return null;
        }
        String roomdef = object.getSquare().getRoom().getRoomDef().getName();
        if (!ItemPickerJava.hasDistributionForRoom(roomdef)) {
            return "No loot distro for " + roomdef;
        }
        String containerType = object.getContainer().getType();
        if (!ItemPickerJava.hasDistributionForContainerInRoom(containerType, roomdef)) {
            ItemPickerRoom roomDist = rooms.get(roomdef);
            if (roomDist.containers.get("other") != null) {
                return "'other' container distro used for " + containerType + " in " + roomdef;
            }
            if (roomDist.containers.containsKey("all")) {
                return "'all' container distro used for " + containerType + " in " + roomdef;
            }
            roomDist = rooms.get("all");
            if (roomDist.containers.containsKey(containerType)) {
                return "'all' roomdef distro used for " + containerType + " in " + roomdef;
            }
            return "No loot distro for " + containerType + " in " + roomdef;
        }
        return null;
    }

    public static boolean hasDistributionForRoom(String roomdef) {
        return roomdef != null && rooms.containsKey(roomdef);
    }

    public static boolean hasDistributionForContainerInRoom(String containerType, String roomdef) {
        if (roomdef == null || containerType == null) {
            return false;
        }
        if (!rooms.containsKey(roomdef)) {
            return false;
        }
        ItemPickerRoom roomDist2 = rooms.get(roomdef);
        return roomDist2.containers.containsKey(containerType);
    }

    public static void onCreateRegion(InventoryItem item, String region) {
        List<Newspaper> regionalPapers = ItemGenerationConstants.REGIONAL_PAPERS.get(region);
        if (regionalPapers == null) {
            regionalPapers = ItemGenerationConstants.REGIONAL_PAPERS.get("General");
        }
        RecipeCodeHelper.nameNewspaper(item, Rand.Next(regionalPapers));
    }

    private static void doGunStorageContainer(boolean dontSpawnAmmo, HandWeapon weapon, ItemContainer container) {
        int i;
        int CLIP_MIN = 4;
        int CLIP_MAX = 8;
        int AMMO_MIN = 3;
        int AMMO_MAX = 6;
        boolean hasClip = !StringUtils.isNullOrEmpty(weapon.getMagazineType());
        boolean hasAmmo = !StringUtils.isNullOrEmpty(weapon.getAmmoBox());
        int clipCount = hasClip ? Rand.Next(1, Rand.Next(4, 8)) : 0;
        int boxCount = hasAmmo ? Rand.Next(1, Rand.Next(3, 6)) : 0;
        for (i = 0; i < clipCount; ++i) {
            Object clip = InventoryItemFactory.CreateItem(weapon.getMagazineType());
            if (!dontSpawnAmmo) {
                ((InventoryItem)clip).setCurrentAmmoCount(((InventoryItem)clip).getMaxAmmo());
            }
            ItemSpawner.spawnItem(clip, container);
        }
        for (i = 0; i < boxCount; ++i) {
            ItemSpawner.spawnItem(InventoryItemFactory.CreateItem(weapon.getAmmoBox()), container);
        }
        if (!dontSpawnAmmo) {
            weapon.setContainsClip(hasClip);
            weapon.setCurrentAmmoCount(weapon.getMaxAmmo());
        }
    }

    static {
        zombieDensityCap = 8.0f;
        NoContainerFillRooms = new ArrayList();
        WeaponUpgrades = new ArrayList();
        WeaponUpgradeMap = new HashMap();
        rooms = new THashMap();
        containers = new THashMap();
        ProceduralDistributions = new THashMap();
        VehicleDistributions = new THashMap();
        addedInvalidAlready = new ArrayList();
    }

    public static final class ItemPickerUpgradeWeapons {
        public String name;
        public ArrayList<String> upgrades = new ArrayList();
    }

    public static final class ItemPickerContainer {
        public ItemPickerItem[] items = new ItemPickerItem[0];
        public float rolls;
        public boolean noAutoAge;
        public boolean isShop;
        public int fillRand;
        public int maxMap;
        public int stashChance;
        public ItemPickerContainer junk;
        public ItemPickerContainer bags;
        public boolean procedural;
        public boolean dontSpawnAmmo;
        public boolean gunStorage;
        public boolean ignoreZombieDensity;
        public boolean cookFood;
        public boolean canBurn;
        public boolean isTrash;
        public boolean isWorn;
        public boolean isRotten;
        public boolean onlyOne;
        public boolean defaultInventoryLoot = true;
        public ArrayList<ProceduralItem> proceduralItems;

        void compact() {
            if (this.proceduralItems != null) {
                this.proceduralItems.trimToSize();
            }
            if (this.junk != null) {
                this.junk.compact();
            }
            if (this.bags != null) {
                this.bags.compact();
            }
        }
    }

    public static final class ItemPickerRoom {
        public THashMap<String, ItemPickerContainer> containers = new THashMap();
        public int fillRand;
        public boolean isShop;
        public String specificId;
        public int professionChance;
        public String outfit;
        public String outfitFemale;
        public String outfitMale;
        public String outfitChance;
        public String vehicle;
        public List<String> vehicles;
        public String vehicleChance;
        public String vehicleDistribution;
        public Integer vehicleSkin;
        public String femaleChance;
        public String roomTypes;
        public String zoneRequires;
        public String zoneDisallows;
        public String containerChance;
        public String femaleOdds;
        public String bagType;
        public String bagTable;
        public int professionChanceInt;

        void compact() {
            for (ItemPickerContainer container : this.containers.values()) {
                container.compact();
            }
            this.containers.trimToSize();
        }
    }

    public static final class VehicleDistribution {
        public ItemPickerRoom normal;
        public final ArrayList<ItemPickerRoom> specific = new ArrayList();

        void compact() {
            if (this.normal != null) {
                this.normal.compact();
            }
            for (int i = 0; i < this.specific.size(); ++i) {
                ItemPickerRoom room = this.specific.get(i);
                room.compact();
            }
            this.specific.trimToSize();
        }
    }

    public static final class ItemPickerItem {
        public String itemName;
        public float chance;
    }

    public static final class ProceduralItem {
        public String name;
        public int min;
        public int max;
        public List<String> forceForItems;
        public List<String> forceForZones;
        public List<String> forceForTiles;
        public List<String> forceForRooms;
        public int weightChance;
    }

    @UsedFromLua
    public static final class KeyNamer {
        @UsedFromLua
        public static ArrayList<String> badZones = new ArrayList();
        @UsedFromLua
        public static ArrayList<String> bigBuildingRooms = new ArrayList();
        @UsedFromLua
        public static ArrayList<String> restaurantSubstrings = new ArrayList();
        @UsedFromLua
        public static ArrayList<String> restaurants = new ArrayList();
        @UsedFromLua
        public static ArrayList<String> roomSubstrings = new ArrayList();
        @UsedFromLua
        public static ArrayList<String> rooms = new ArrayList();

        public static void clear() {
            badZones.clear();
            bigBuildingRooms.clear();
            restaurantSubstrings.clear();
            restaurants.clear();
            roomSubstrings.clear();
            rooms.clear();
        }

        public static void nameKey(InventoryItem item, IsoGridSquare square) {
            Object keyName;
            if (item != null && square != null) {
                item.setOrigin(square);
            }
            if ((keyName = KeyNamer.getName(square)) != null && Translator.getTextOrNull("IGUI_" + (String)(keyName = (String)keyName + ItemPickerJava.KeyLootType)) != null) {
                keyName = Translator.getText("IGUI_" + (String)keyName);
                item.setName(Translator.getText(item.getDisplayName()) + " - " + (String)keyName);
            }
        }

        public static String getName(IsoGridSquare square) {
            String keyNameTest;
            String testName;
            String buildingName;
            if (square == null || square.getBuilding() == null) {
                return null;
            }
            IsoBuilding building = square.getBuilding();
            String zone = ItemPickerJava.getSquareZombiesType(square);
            if (badZones.contains(zone)) {
                zone = null;
            }
            String keyName = null;
            if (ItemPickerJava.getSquareBuildingName(square) != null) {
                keyName = ItemPickerJava.getSquareBuildingName(square);
                return keyName;
            }
            if (building.containsRoom("bedroom") && building.containsRoom("livingroom") && building.containsRoom("kitchen")) {
                return "Residential";
            }
            if (zone != null) {
                switch (zone) {
                    case "Prison": {
                        keyName = "Prison";
                        break;
                    }
                    case "Police": {
                        keyName = "Police";
                        break;
                    }
                    case "Army": {
                        keyName = "Army";
                    }
                }
            }
            if (badZones.contains(keyName)) {
                keyName = null;
            }
            if (keyName != null) {
                return keyName;
            }
            Object object = bigBuildingRooms.iterator();
            while (!(!object.hasNext() || (buildingName = (String)object.next()).equals("storageunit") && building.containsRoom("bedroom"))) {
                if (!building.containsRoom(buildingName)) continue;
                return buildingName;
            }
            if (ItemPickerJava.getSquareZombiesType(square) != null && !badZones.contains(ItemPickerJava.getSquareZombiesType(square)) && (testName = ItemPickerJava.getSquareZombiesType(square)) != null && Translator.getTextOrNull("IGUI_" + (keyNameTest = testName + ItemPickerJava.KeyLootType)) != null) {
                return testName;
            }
            if (square.getRoom() != null && square.getRoom().getRoomDef() != null) {
                String roomDef = square.getRoom().getRoomDef().getName();
                if (rooms.contains(roomDef)) {
                    return roomDef;
                }
                for (String name : roomSubstrings) {
                    if (!roomDef.contains(name)) continue;
                    return name;
                }
                if (restaurants.contains(roomDef)) {
                    return roomDef;
                }
                for (String name : restaurantSubstrings) {
                    if (!roomDef.contains(name)) continue;
                    return name;
                }
            }
            if (ItemPickerJava.squareHasZone(square, "TrailerPark") && building.containsRoom("bedroom")) {
                return "TrailerPark";
            }
            if (ItemPickerJava.squareHasZone(square, "Ranch") && building.containsRoom("bedroom")) {
                return "Ranch";
            }
            if (ItemPickerJava.squareHasZone(square, "Forest")) {
                return "Forest";
            }
            if (ItemPickerJava.squareHasZone(square, "DeepForest")) {
                return "DeepForest";
            }
            return null;
        }
    }
}

