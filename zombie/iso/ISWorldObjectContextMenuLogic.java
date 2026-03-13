/*
 * Decompiled with CFR 0.152.
 */
package zombie.iso;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Predicate;
import se.krka.kahlua.vm.KahluaTable;
import se.krka.kahlua.vm.KahluaTableIterator;
import zombie.GameTime;
import zombie.Lua.LuaEventManager;
import zombie.Lua.LuaManager;
import zombie.SandboxOptions;
import zombie.UsedFromLua;
import zombie.ZomboidGlobals;
import zombie.characterTextures.BloodBodyPartType;
import zombie.characters.CharacterStat;
import zombie.characters.IsoPlayer;
import zombie.characters.animals.IsoAnimal;
import zombie.characters.skills.PerkFactory;
import zombie.core.Core;
import zombie.core.Translator;
import zombie.core.input.Input;
import zombie.core.properties.IsoPropertyType;
import zombie.core.properties.PropertyContainer;
import zombie.core.properties.TilePropertyKey;
import zombie.core.skinnedmodel.visual.HumanVisual;
import zombie.core.textures.Texture;
import zombie.core.textures.TexturePackPage;
import zombie.debug.DebugOptions;
import zombie.entity.ComponentType;
import zombie.entity.GameEntity;
import zombie.entity.components.contextmenuconfig.ContextMenuConfig;
import zombie.entity.components.fluids.Fluid;
import zombie.entity.components.fluids.FluidContainer;
import zombie.entity.components.fluids.FluidType;
import zombie.fireFighting.FireFighting;
import zombie.globalObjects.CGlobalObjectSystem;
import zombie.globalObjects.CGlobalObjects;
import zombie.globalObjects.GlobalObject;
import zombie.inventory.InventoryItem;
import zombie.inventory.InventoryItemFactory;
import zombie.inventory.ItemContainer;
import zombie.inventory.ItemPickerJava;
import zombie.inventory.types.Clothing;
import zombie.inventory.types.DrainableComboItem;
import zombie.inventory.types.HandWeapon;
import zombie.inventory.types.Key;
import zombie.inventory.types.Radio;
import zombie.inventory.types.WeaponType;
import zombie.iso.BrokenFences;
import zombie.iso.ICurtain;
import zombie.iso.IHasHealth;
import zombie.iso.IItemProvider;
import zombie.iso.ILockableDoor;
import zombie.iso.IsoButcherHook;
import zombie.iso.IsoDirections;
import zombie.iso.IsoGridSquare;
import zombie.iso.IsoMovingObject;
import zombie.iso.IsoObject;
import zombie.iso.IsoObjectPicker;
import zombie.iso.IsoUtils;
import zombie.iso.IsoWorld;
import zombie.iso.SpriteDetails.IsoFlagType;
import zombie.iso.SpriteDetails.IsoObjectType;
import zombie.iso.areas.DesignationZoneAnimal;
import zombie.iso.areas.SafeHouse;
import zombie.iso.objects.IsoAnimalTrack;
import zombie.iso.objects.IsoBarricade;
import zombie.iso.objects.IsoBrokenGlass;
import zombie.iso.objects.IsoCarBatteryCharger;
import zombie.iso.objects.IsoClothingDryer;
import zombie.iso.objects.IsoClothingWasher;
import zombie.iso.objects.IsoCombinationWasherDryer;
import zombie.iso.objects.IsoCompost;
import zombie.iso.objects.IsoCurtain;
import zombie.iso.objects.IsoDeadBody;
import zombie.iso.objects.IsoDoor;
import zombie.iso.objects.IsoFeedingTrough;
import zombie.iso.objects.IsoGenerator;
import zombie.iso.objects.IsoLightSwitch;
import zombie.iso.objects.IsoRadio;
import zombie.iso.objects.IsoStove;
import zombie.iso.objects.IsoThumpable;
import zombie.iso.objects.IsoTrap;
import zombie.iso.objects.IsoTree;
import zombie.iso.objects.IsoWindow;
import zombie.iso.objects.IsoWindowFrame;
import zombie.iso.objects.IsoWorldInventoryObject;
import zombie.iso.objects.interfaces.BarricadeAble;
import zombie.iso.sprite.IsoSprite;
import zombie.iso.sprite.IsoSpriteInstance;
import zombie.network.GameClient;
import zombie.network.GameServer;
import zombie.network.ServerOptions;
import zombie.scripting.ScriptManager;
import zombie.scripting.entity.components.contextmenuconfig.ContextMenuConfigScript;
import zombie.scripting.objects.CharacterTrait;
import zombie.scripting.objects.Item;
import zombie.scripting.objects.ItemKey;
import zombie.scripting.objects.ItemTag;
import zombie.scripting.objects.MoodleType;
import zombie.scripting.objects.Recipe;
import zombie.ui.ISUIWrapper.ISContextMenuWrapper;
import zombie.ui.ISUIWrapper.ISToolTipWrapper;
import zombie.ui.ISUIWrapper.LuaHelpers;
import zombie.ui.TextManager;
import zombie.ui.UIFont;
import zombie.util.AdjacentFreeTileFinder;
import zombie.vehicles.BaseVehicle;
import zombie.vehicles.VehiclePart;

@UsedFromLua
public class ISWorldObjectContextMenuLogic {
    private static final IsoDirections[] DIRECTIONS = IsoDirections.values();

    public static void fetch(KahluaTable fetch, IsoObject v, double player, boolean doSquare) {
        KahluaTable fluidcontainer;
        KahluaTable clickedAnimals;
        IsoDeadBody body;
        IsoDeadBody isoDeadBody;
        IsoThumpable vIsoThumpable;
        IsoAnimalTrack animalTrack;
        IsoThumpable isoThumpable;
        KahluaTable storeWater;
        KahluaTable worldObjectContextMenu = (KahluaTable)LuaManager.env.rawget("ISWorldObjectContextMenu");
        KahluaTable fetchSquares = (KahluaTable)worldObjectContextMenu.rawget("fetchSquares");
        IsoPlayer playerObj = IsoPlayer.players[(int)player];
        ItemContainer playerInv = playerObj.getInventory();
        PropertyContainer props = v.getProperties();
        if (doSquare && v.getSquare() != null) {
            ArrayList<IsoWorldInventoryObject> worldItems = v.getSquare().getWorldObjects();
            if (worldItems != null && !worldItems.isEmpty()) {
                fetch.rawset("worldItem", worldItems.getFirst());
            }
            fetch.rawset("building", (Object)v.getSquare().getBuilding());
        }
        if (v.hasFluid() && !LuaHelpers.tableContainsValue(storeWater = (KahluaTable)fetch.rawget("storeWater"), v)) {
            storeWater.rawset(storeWater.size() + 1, (Object)v);
        }
        Double c = (Double)fetch.rawget("c");
        fetch.rawset("c", (Object)(c + 1.0));
        if (v instanceof IsoWindow) {
            fetch.rawset("window", (Object)v);
        } else if (v instanceof IsoCurtain) {
            fetch.rawset("curtain", (Object)v);
        }
        if (v instanceof IsoDoor || v instanceof IsoThumpable && (isoThumpable = (IsoThumpable)v).isDoor()) {
            fetch.rawset("door", (Object)v);
            if (v instanceof IsoDoor) {
                IsoDoor isoDoor = (IsoDoor)v;
                int keyId = isoDoor.checkKeyId();
                fetch.rawset("doorKeyId", (Object)keyId);
                if (keyId == -1) {
                    fetch.rawset("doorKeyId", null);
                }
            }
            if (v instanceof IsoThumpable && v.getKeyId() != -1) {
                fetch.rawset("doorKeyId", (Object)v.getKeyId());
            }
        }
        if (v instanceof IsoAnimalTrack) {
            fetch.rawset("animaltrack", (Object)v);
        }
        if (doSquare && (animalTrack = v.getSquare().getAnimalTrack()) != null) {
            fetch.rawset("animaltrack", (Object)v.getSquare().getAnimalTrack());
        }
        fetch.rawset("item", (Object)v);
        if (v instanceof IsoButcherHook) {
            fetch.rawset("butcherHook", (Object)v);
        }
        if (v.hasComponent(ComponentType.ContextMenuConfig) && !v.hasComponent(ComponentType.FluidContainer)) {
            fetch.rawset("entityContext", (Object)v);
        }
        if (v.getSprite() != null && v.getSprite().getName() != null) {
            fetch.rawset("tilename", (Object)v.getSprite().getName());
            if (v.getContainer() != null && v.getContainer().getType() != null) {
                String tilename = String.valueOf(fetch.rawget("tilename")) + " / Container Report: " + v.getContainer().getType();
                fetch.rawset("tilename", (Object)tilename);
            }
            fetch.rawset("tileObj", (Object)v);
        }
        if (v instanceof IsoCompost) {
            fetch.rawset("compost", (Object)v);
        }
        if (v.getSprite() != null && v.getSprite().getProperties().has(IsoFlagType.HoppableN)) {
            if (fetch.rawget("hoppableN") == null) {
                fetch.rawset("hoppableN", (Object)v);
            } else if (fetch.rawget("hoppableN") != v && fetch.rawget("hoppableN_2") != v) {
                fetch.rawset("hoppableN_2", (Object)v);
            }
        }
        if (v.getSprite() != null && v.getSprite().getProperties().has(IsoFlagType.HoppableW)) {
            if (fetch.rawget("hoppableW") == null) {
                fetch.rawset("hoppableW", (Object)v);
            } else if (fetch.rawget("hoppableW") != v && fetch.rawget("hoppableW_2") != v) {
                fetch.rawset("hoppableW_2", (Object)v);
            }
        }
        if (v instanceof IsoThumpable && !(vIsoThumpable = (IsoThumpable)v).isDoor()) {
            fetch.rawset("thump", (Object)v);
            if (vIsoThumpable.canBeLockByPadlock() && !vIsoThumpable.isLockedByPadlock() && vIsoThumpable.getLockedByCode() == 0) {
                fetch.rawset("padlockThump", (Object)v);
            }
            if (vIsoThumpable.isLockedByPadlock()) {
                fetch.rawset("padlockedThump", (Object)v);
            }
            if (vIsoThumpable.getLockedByCode() > 0) {
                fetch.rawset("digitalPadlockedThump", (Object)v);
            }
            if (vIsoThumpable.isWindow()) {
                fetch.rawset("thumpableWindow", (Object)v);
            }
            if (vIsoThumpable.isWindowN()) {
                if (fetch.rawget("thumpableWindowN") == null) {
                    fetch.rawset("thumpableWindowN", (Object)v);
                } else if (fetch.rawget("thumpableWindowN") != v && fetch.rawget("thumpableWindowN_2") != v) {
                    fetch.rawset("thumpableWindowN_2", (Object)v);
                }
            }
            if (vIsoThumpable.isWindowW()) {
                if (fetch.rawget("thumpableWindowW") == null) {
                    fetch.rawset("thumpableWindowW", (Object)v);
                } else if (fetch.rawget("thumpableWindowW") != v && fetch.rawget("thumpableWindowW_2") != v) {
                    fetch.rawset("thumpableWindowW_2", (Object)v);
                }
            }
            if (vIsoThumpable.propertyEquals("CustomName", "Rain Collector Barrel")) {
                fetch.rawset("rainCollectorBarrel", (Object)v);
            }
        }
        if (v instanceof IsoTree) {
            fetch.rawset("tree", (Object)v);
        }
        if (v instanceof IsoTree || v.getProperties() != null && v.getProperties().has(TilePropertyKey.CAN_ATTACH_ANIMAL.toString())) {
            fetch.rawset("attachAnimalTo", (Object)v);
        }
        if (v instanceof IsoClothingDryer) {
            fetch.rawset("clothingDryer", (Object)v);
        }
        if (v instanceof IsoClothingWasher) {
            fetch.rawset("clothingWasher", (Object)v);
        }
        if (v instanceof IsoCombinationWasherDryer) {
            fetch.rawset("comboWasherDryer", (Object)v);
        }
        if (v instanceof IsoStove && v.getContainer() != null) {
            fetch.rawset("stove", (Object)v);
        }
        if (v instanceof IsoDeadBody && !(isoDeadBody = (IsoDeadBody)v).isAnimal() && ((body = (IsoDeadBody)fetch.rawget("body")) == null || body.DistToSquared(playerObj) > isoDeadBody.DistToSquared(playerObj))) {
            fetch.rawset("body", (Object)v);
        }
        if (v instanceof IsoDeadBody && (isoDeadBody = (IsoDeadBody)v).isAnimal()) {
            fetch.rawset("animalbody", (Object)v);
        }
        if (v instanceof IsoCarBatteryCharger) {
            fetch.rawset("carBatteryCharger", (Object)v);
        }
        if (v instanceof IsoGenerator) {
            fetch.rawset("generator", (Object)v);
        }
        if (doSquare) {
            IsoDeadBody deadBody;
            IsoDeadBody isoDeadBody2 = deadBody = v.getSquare() != null ? v.getSquare().getDeadBody() : null;
            if (deadBody != null && fetch.rawget("body") == null && deadBody.isAnimal()) {
                fetch.rawset("body", (Object)deadBody);
            }
            if (deadBody != null && fetch.rawget("animalbody") == null && deadBody.isAnimal()) {
                fetch.rawset("animalbody", (Object)deadBody);
            }
        }
        if (v.getSprite() != null) {
            if (v.getSprite().getProperties().has(IsoFlagType.bed)) {
                fetch.rawset("bed", (Object)v);
            }
            if (v.getSprite().getProperties().has(IsoFlagType.makeWindowInvincible)) {
                fetch.rawset("invincibleWindow", (Object)true);
            }
        }
        if (v instanceof IsoWindowFrame) {
            fetch.rawset("windowFrame", (Object)v);
        }
        if (v instanceof IsoBrokenGlass) {
            fetch.rawset("brokenGlass", (Object)v);
        }
        if (v instanceof IsoTrap) {
            fetch.rawset("trap", (Object)v);
        }
        if (v.getName() != null && v.getName().equals("EmptyGraves") && !ISWorldObjectContextMenuLogic.isGraveFilledIn(v)) {
            fetch.rawset("graves", (Object)v);
        }
        if (v instanceof IsoLightSwitch) {
            IsoLightSwitch isoLightSwitch = (IsoLightSwitch)v;
            if (v.getSquare() != null && (v.getSquare().getRoom() != null || isoLightSwitch.getCanBeModified())) {
                fetch.rawset("lightSwitch", (Object)v);
            }
        }
        if (doSquare && v.getSquare() != null && (v.getSquare().getProperties().has(IsoFlagType.HoppableW) || v.getSquare().getProperties().has(IsoFlagType.HoppableN))) {
            fetch.rawset("canClimbThrough", (Object)true);
        }
        if (v.getSprite() != null && v.getSprite().getProperties().has(IsoFlagType.water) && v.getSquare().DistToProper(playerObj.getSquare()) < 20.0f && !playerObj.isSitOnGround()) {
            fetch.rawset("canFish", (Object)true);
            if (v.getSquare().getWater() != null && v.getSquare().getWater().isShore()) {
                fetch.rawset("canFish", (Object)false);
            }
            if (v.getSquare().DistToProper(playerObj.getSquare()) < 8.0f && !playerObj.getInventory().getAllTypeRecurse("Base.Chum").isEmpty()) {
                fetch.rawset("canAddChum", (Object)true);
            }
        }
        if (doSquare) {
            KahluaTable groundType = ISWorldObjectContextMenuLogic.getDirtGravelSand(v.getSquare());
            fetch.rawset("groundType", (Object)groundType);
            if (groundType != null) {
                fetch.rawset("groundSquare", (Object)v.getSquare());
            }
        }
        InventoryItem hasCuttingTool = playerInv.getFirstRecurse(item -> !item.isBroken() && item.hasTag(ItemTag.CUT_PLANT));
        if (v.getSprite() != null) {
            if (v.getSprite().getProperties().has(IsoFlagType.canBeCut) && hasCuttingTool != null) {
                fetch.rawset("canBeCut", (Object)v.getSquare());
            }
            if (v.getSprite().getProperties().has(IsoFlagType.canBeRemoved)) {
                fetch.rawset("canBeRemoved", (Object)v.getSquare());
            }
        }
        ArrayList<IsoSpriteInstance> attached = v.getAttachedAnimSprite();
        if (hasCuttingTool != null && attached != null) {
            for (int n = 0; n < attached.size(); ++n) {
                IsoSpriteInstance sprite = attached.get(n);
                if (sprite == null || sprite.getParentSprite() == null || sprite.getParentSprite().getName() == null || !sprite.getParentSprite().getName().startsWith("f_wallvines_")) continue;
                fetch.rawset("wallVine", (Object)v.getSquare());
                break;
            }
        }
        if (v.getSprite() != null && v.getSprite().getProperties().has(IsoFlagType.water) && playerInv.containsTypeRecurse("FishingNet")) {
            fetch.rawset("canTrapFish", (Object)true);
        }
        if (v.getName() != null && v.getName().equals("FishingNet") && v.getSquare() != null) {
            fetch.rawset("trapFish", (Object)v);
        }
        if (doSquare) {
            if (v.getSquare().getProperties().has(IsoFlagType.climbSheetN) || v.getSquare().getProperties().has(IsoFlagType.climbSheetW) || v.getSquare().getProperties().has(IsoFlagType.climbSheetS) || v.getSquare().getProperties().has(IsoFlagType.climbSheetE)) {
                fetch.rawset("sheetRopeSquare", (Object)v.getSquare());
            }
            if (FireFighting.getSquareToExtinguish(v.getSquare()) != null) {
                fetch.rawset("extinguisher", (Object)FireFighting.getExtinguisher(playerObj));
                fetch.rawset("firetile", (Object)v.getSquare());
            }
            fetch.rawset("clickedSquare", (Object)v.getSquare());
        }
        if (doSquare && playerInv.getFirstRecurse(item -> !item.isBroken() && item.hasTag(ItemTag.CLEAR_ASHES)) != null && v.getSprite() != null) {
            IsoObject ashes;
            String spriteName = v.getSprite().getName();
            if (spriteName == null) {
                spriteName = v.getSpriteName();
            }
            if ((spriteName.equals("floors_burnt_01_1") || spriteName.equals("floors_burnt_01_2")) && ((ashes = (IsoObject)fetch.rawget("ashes")) == null || ashes.getTargetAlpha() <= v.getTargetAlpha())) {
                fetch.rawset("ashes", (Object)v);
            }
        }
        if (doSquare) {
            IsoObject destroy;
            InventoryItem sledgehammer = playerInv.getFirstRecurse(item -> !item.isBroken() && ISWorldObjectContextMenuLogic.compareType("Sledgehammer", item));
            if (sledgehammer == null) {
                sledgehammer = playerInv.getFirstRecurse(item -> !item.isBroken() && ISWorldObjectContextMenuLogic.compareType("Sledgehammer2", item));
            }
            if (sledgehammer != null && sledgehammer.getCondition() > 0 && v.getSprite() != null && (v.getSprite().getProperties().has(IsoFlagType.solidtrans) || v.getSprite().getProperties().has(IsoFlagType.collideW) || v.getSprite().getProperties().has(IsoFlagType.collideN) || v.getSprite().getProperties().has(IsoFlagType.bed) || v instanceof IsoThumpable || v.getSprite().getProperties().has(IsoFlagType.windowN) || v.getSprite().getProperties().has(IsoFlagType.windowW) || v.getType() == IsoObjectType.stairsBN || v.getType() == IsoObjectType.stairsMN || v.getType() == IsoObjectType.stairsTN || v.getType() == IsoObjectType.stairsBW || v.getType() == IsoObjectType.stairsMW || v.getType() == IsoObjectType.stairsTW || (v.getProperties().has("DoorWallN") || v.getProperties().has("DoorWallW")) && !v.getSquare().haveDoor() || v.getSprite().getProperties().has(IsoFlagType.waterPiped)) && (v.getSprite().getName() == null || !v.getSprite().getName().startsWith("blends_natural_02") || !v.getSprite().getName().startsWith("floors_burnt_01_")) && ((destroy = (IsoObject)fetch.rawget("destroy")) == null || destroy.getTargetAlpha() <= v.getTargetAlpha())) {
                fetch.rawset("destroy", (Object)v);
            }
            if (ISWorldObjectContextMenuLogic.canCleanBlood(playerObj, v.getSquare())) {
                fetch.rawset("haveBlood", (Object)v.getSquare());
            }
            if (ISWorldObjectContextMenuLogic.canCleanGraffiti(playerObj, v.getSquare())) {
                fetch.rawset("haveGraffiti", (Object)v.getSquare());
            }
        }
        if (v instanceof IsoPlayer && v != playerObj) {
            fetch.rawset("clickedPlayer", (Object)v);
        }
        if (v instanceof IsoAnimal && !LuaHelpers.tableContainsValue(clickedAnimals = (KahluaTable)fetch.rawget("clickedAnimals"), v)) {
            clickedAnimals.rawset(clickedAnimals.size() + 1, (Object)v);
        }
        if (v.getPipedFuelAmount() > 0) {
            if (playerInv.getFirstRecurse(ISWorldObjectContextMenuLogic::predicateStoreFuel) != null) {
                fetch.rawset("haveFuel", (Object)v);
            }
            fetch.rawset("haveFuelDebug", (Object)v);
        }
        if (v.getSprite() != null && v.getSprite().getProperties().has("fuelAmount")) {
            fetch.rawset("fuelPump", (Object)v);
        }
        if (v.hasComponent(ComponentType.FluidContainer) && !LuaHelpers.tableContainsValue(fluidcontainer = (KahluaTable)fetch.rawget("fluidcontainer"), v)) {
            fluidcontainer.rawset(fluidcontainer.size() + 1, (Object)v);
        }
        if (doSquare) {
            fetch.rawset("safehouse", (Object)SafeHouse.getSafeHouse(v.getSquare()));
            fetch.rawset("safehouseAllowInteract", (Object)SafeHouse.isSafehouseAllowInteract(v.getSquare(), playerObj));
            fetch.rawset("safehouseAllowLoot", (Object)SafeHouse.isSafehouseAllowLoot(v.getSquare(), playerObj));
        }
        boolean preWaterShutoff = GameTime.instance.getWorldAgeHours() / 24.0 + (double)((SandboxOptions.instance.getTimeSinceApo() - 1) * 30) < (double)SandboxOptions.instance.waterShutModifier.getValue();
        boolean vCanBeWaterPiped = LuaHelpers.castBoolean(v.getModData().rawget("canBeWaterPiped"));
        if (v.hasModData() && v.getSquare() != null && vCanBeWaterPiped && v.getSquare().isInARoom() && v.FindExternalWaterSource() != null) {
            fetch.rawset("canBeWaterPiped", (Object)v);
        }
        if (props != null && v.getSquare() != null && props.has(IsoFlagType.waterPiped) && !v.getUsesExternalWaterSource() && (v.getSquare().isInARoom() && v.FindExternalWaterSource() != null || v.getSquare().getRoom() != null && preWaterShutoff && vCanBeWaterPiped)) {
            fetch.rawset("canBeWaterPiped", (Object)v);
        }
        if (props != null) {
            ISWorldObjectContextMenuLogic.fetchPickupItems(fetch, v, props, playerInv);
        }
        if (v instanceof IHasHealth) {
            fetch.rawset("health", (Object)v);
        }
        fetch.rawset("item", (Object)v);
        if (doSquare && v.getSquare() != null && fetchSquares.rawget(v.getSquare()) == null) {
            int i;
            for (i = 0; i < v.getSquare().getObjects().size(); ++i) {
                ISWorldObjectContextMenuLogic.fetch(fetch, v.getSquare().getObjects().get(i), player, false);
            }
            for (i = 0; i < v.getSquare().getStaticMovingObjects().size(); ++i) {
                ISWorldObjectContextMenuLogic.fetch(fetch, v.getSquare().getStaticMovingObjects().get(i), player, false);
            }
            KahluaTable clickedAnimals2 = (KahluaTable)fetch.rawget("clickedAnimals");
            for (int x = v.getSquare().getX() - 1; x <= v.getSquare().getX() + 1; ++x) {
                for (int y = v.getSquare().getY() - 1; y <= v.getSquare().getY() + 1; ++y) {
                    IsoGridSquare sq = v.getSquare().getCell().getGridSquare(x, y, v.getSquare().getZ());
                    if (sq == null) continue;
                    for (int i2 = 0; i2 < sq.getMovingObjects().size(); ++i2) {
                        IsoMovingObject o = sq.getMovingObjects().get(i2);
                        if (o instanceof IsoPlayer && o != playerObj) {
                            fetch.rawset("clickedPlayer", (Object)o);
                        }
                        if (!(o instanceof IsoAnimal) || LuaHelpers.tableContainsValue(clickedAnimals2, o)) continue;
                        clickedAnimals2.rawset(clickedAnimals2.size() + 1, (Object)o);
                    }
                }
            }
        }
        if (doSquare) {
            fetch.rawset("animalZone", (Object)DesignationZoneAnimal.getZone(v.getSquare().getX(), v.getSquare().getY(), v.getSquare().getZ()));
            fetchSquares.rawset(v.getSquare(), (Object)true);
        }
    }

    public static boolean createMenuEntries(KahluaTable fetch, KahluaTable context, double player, KahluaTable worldobjects, int x, int y, boolean test) {
        DesignationZoneAnimal animalZone;
        String reason;
        boolean safehouseAllowLoot;
        SafeHouse safehouse;
        KahluaTable inventoryPaneContextMenu = (KahluaTable)LuaManager.env.rawget("ISInventoryPaneContextMenu");
        ISContextMenuWrapper contextWrapper = new ISContextMenuWrapper(context);
        IsoPlayer playerObj = IsoPlayer.players[(int)player];
        ItemContainer playerInv = playerObj.getInventory();
        IHasHealth health = (IHasHealth)fetch.rawget("health");
        if (DebugOptions.instance.uiShowContextMenuReportOptions.getValue() && health != null && Core.debug && health.getMaxHealth() > 0) {
            String text = String.format(Locale.ENGLISH, "%s: %d/%d", Translator.getText("ContextMenu_ObjectHealth"), health.getHealth(), health.getMaxHealth());
            contextWrapper.addDebugOption(text, null, null, new Object[0]);
        }
        if (fetch.rawget("tilename") != null && (Core.debug || SandboxOptions.instance.isUnstableScriptNameSpam())) {
            ISWorldObjectContextMenuLogic.addTileDebugInfo(contextWrapper, fetch);
        }
        IsoGridSquare clickedSquare = (IsoGridSquare)fetch.rawget("clickedSquare");
        if (DebugOptions.instance.uiShowContextMenuReportOptions.getValue()) {
            if (clickedSquare != null && (Core.debug || SandboxOptions.instance.isUnstableScriptNameSpam()) && clickedSquare.getRoom() != null && clickedSquare.getRoom().getRoomDef() != null) {
                contextWrapper.addDebugOption(String.format(Locale.ENGLISH, "%s: %s, x: %d, y: %d, z: %d", Translator.getText("Room Report"), clickedSquare.getRoom().getRoomDef().getName(), clickedSquare.getX(), clickedSquare.getY(), clickedSquare.getZ()), null, null, new Object[0]);
            } else if (clickedSquare != null && (Core.debug || SandboxOptions.instance.isUnstableScriptNameSpam())) {
                contextWrapper.addDebugOption(String.format(Locale.ENGLISH, "Coordinates Report x: %d, y: %d, z: %d", clickedSquare.getX(), clickedSquare.getY(), clickedSquare.getZ()), null, null, new Object[0]);
            }
        }
        LuaHelpers.callLuaClass("DebugContextMenu", "doDebugMenu", null, player, context, worldobjects, test);
        boolean safehouseAllowInteract = LuaHelpers.castBoolean(fetch.rawget("safehouseAllowInteract"));
        if (safehouseAllowInteract) {
            IsoClothingDryer clothingDryer;
            IsoWorldInventoryObject worldItem;
            IsoGridSquare sheetRopeSquare;
            IsoDeadBody animalbody;
            IsoTrap trap;
            Object ore;
            Object ashes;
            Object butcherHook;
            IsoObject haveFuelDebug;
            ISContextMenuWrapper doorContextWrapper;
            IsoGridSquare firetile = (IsoGridSquare)fetch.rawget("firetile");
            InventoryItem extinguisher = (InventoryItem)fetch.rawget("extinguisher");
            if (firetile != null && extinguisher != null && ISWorldObjectContextMenuLogic.doExtinguishFireMenu(worldobjects, test, contextWrapper, firetile, extinguisher, playerObj)) {
                return true;
            }
            InventoryItem heavyItem = playerObj.getPrimaryHandItem();
            if (ISWorldObjectContextMenuLogic.isForceDropHeavyItem(heavyItem)) {
                KahluaTable newTable = LuaManager.platform.newTable();
                newTable.rawset(1, (Object)heavyItem);
                contextWrapper.addOption(Translator.getText("ContextMenu_DropNamedItem", heavyItem.getDisplayName()), newTable, inventoryPaneContextMenu.rawget("onUnEquip"), player);
            }
            ISWorldObjectContextMenuLogic.handleInteraction(x, y, test, contextWrapper, worldobjects, playerObj, playerInv);
            if (ISWorldObjectContextMenuLogic.handleGrabWorldItem(fetch, x, y, test, contextWrapper, worldobjects, playerObj, playerInv)) {
                return true;
            }
            IsoDeadBody body = (IsoDeadBody)fetch.rawget("body");
            if (body != null && !body.isAnimal() && playerObj.getVehicle() == null && ISWorldObjectContextMenuLogic.doBurnBodyMenu(player, worldobjects, test, playerInv, contextWrapper, body)) {
                return true;
            }
            boolean hasHammer = playerInv.getFirstRecurse(item -> item.hasTag(ItemTag.HAMMER) && !item.isBroken()) != null;
            boolean hasRemoveBarricadeTool = playerInv.getFirstRecurse(item -> item.hasTag(ItemTag.REMOVE_BARRICADE) && !item.isBroken()) != null;
            boolean thumpableWindowResult = LuaHelpers.castBoolean(LuaHelpers.callLuaClass("ISWorldObjectContextMenu", "doThumpableWindowOption", null, test, context, player));
            if (thumpableWindowResult) {
                return true;
            }
            boolean invincibleWindow = LuaHelpers.castBoolean(fetch.rawget("invincibleWindow"));
            IsoThumpable thump = (IsoThumpable)fetch.rawget("thump");
            IsoWindow window = (IsoWindow)fetch.rawget("window");
            if (thump != null && !invincibleWindow && window == null && ISWorldObjectContextMenuLogic.doThumpableMenu(thump, player, worldobjects, test, invincibleWindow, playerObj, hasRemoveBarricadeTool, contextWrapper)) {
                return true;
            }
            ISContextMenuWrapper windowContextWrapper = ISWorldObjectContextMenuLogic.doWindowMenu(fetch, worldobjects, test, contextWrapper, window, invincibleWindow, playerObj, hasRemoveBarricadeTool, hasHammer);
            if (windowContextWrapper == null) {
                return true;
            }
            if (windowContextWrapper.getNumOptions() <= 1.0) {
                contextWrapper.removeLastOption();
            }
            if ((doorContextWrapper = ISWorldObjectContextMenuLogic.doDoorMenu(fetch, context, worldobjects, test, contextWrapper, playerObj, hasRemoveBarricadeTool)) == null) {
                return true;
            }
            if (doorContextWrapper.getNumOptions() <= 1.0) {
                contextWrapper.removeLastOption();
            }
            if (ISWorldObjectContextMenuLogic.doGeneratorMenu(fetch, player, worldobjects, test, playerObj, contextWrapper, playerInv)) {
                return true;
            }
            IsoObject container = (IsoObject)fetch.rawget("container");
            if (Core.debug && container != null && ItemPickerJava.getLootDebugString(container) != null) {
                contextWrapper.addDebugOption(String.format(Locale.ENGLISH, ItemPickerJava.getLootDebugString(container), new Object[0]), null, null, new Object[0]);
            }
            if ((haveFuelDebug = (IsoObject)fetch.rawget("haveFuelDebug")) != null && Core.debug) {
                contextWrapper.addDebugOption(String.format(Locale.ENGLISH, "%s: %d", Translator.getText("ContextMenu_PumpFuelAmount"), haveFuelDebug.getPipedFuelAmount()), null, null, new Object[0]);
            }
            if ((butcherHook = fetch.rawget("butcherHook")) != null) {
                if (test) {
                    return true;
                }
                contextWrapper.addGetUpOption(Translator.getText("ContextMenu_ButcherHook"), butcherHook, LuaManager.getFunctionObject("ISWorldObjectContextMenu.onButcherHook"), playerObj);
            }
            if (ISWorldObjectContextMenuLogic.doCarBatteryMenu(fetch, context, worldobjects, test, playerObj, playerInv)) {
                return true;
            }
            LuaHelpers.callLuaClass("ISTrapMenu", "doTrapMenu", null, player, context, worldobjects, test);
            LuaHelpers.callLuaClass("ISFarmingMenu", "doFarmingMenu", null, player, context, worldobjects, test);
            Object entityContext = fetch.rawget("entityContext");
            if (entityContext != null) {
                if (test) {
                    return true;
                }
                ISWorldObjectContextMenuLogic.doContextConfigOptionsFromFetch(contextWrapper, fetch, playerObj);
            }
            if (safehouseAllowInteract) {
                if (test) {
                    return true;
                }
                LuaHelpers.callLuaClass("ISRadioAndTvMenu", "createMenu", worldobjects, context, playerObj);
            }
            if ((ashes = fetch.rawget("ashes")) != null) {
                if (test) {
                    return true;
                }
                contextWrapper.addGetUpOption(Translator.getText("ContextMenu_Clear_Ashes"), worldobjects, LuaManager.getFunctionObject("ISWorldObjectContextMenu.onClearAshes"), player, ashes);
            }
            if ((ore = fetch.rawget("ore")) != null && playerObj.getVehicle() == null) {
                if (test) {
                    return true;
                }
                contextWrapper.addGetUpOption(Translator.getText("ContextMenu_Remove_Ore"), worldobjects, LuaManager.getFunctionObject("ISWorldObjectContextMenu.onRemoveGroundCoverItemHammerOrPickAxe"), player, ore);
            }
            if ((trap = (IsoTrap)fetch.rawget("trap")) != null && trap.getItem() != null && ISWorldObjectContextMenuLogic.doTrapMenu(player, worldobjects, test, contextWrapper)) {
                return true;
            }
            IsoDeadBody pickedCorpse = (IsoDeadBody)IsoObjectPicker.Instance.PickCorpse(x, y);
            if (pickedCorpse != null && pickedCorpse.isAnimal()) {
                fetch.rawset("animalbody", (Object)pickedCorpse);
            }
            if ((animalbody = (IsoDeadBody)fetch.rawget("animalbody")) != null) {
                LuaHelpers.callLuaClass("AnimalContextMenu", "doAnimalBodyMenu", null, context, player, animalbody);
            }
            if (ISWorldObjectContextMenuLogic.doPadLockMenu(fetch, player, worldobjects, test, playerInv, contextWrapper)) {
                return true;
            }
            IsoObject canBeWaterPiped = (IsoObject)fetch.rawget("canBeWaterPiped");
            if (canBeWaterPiped != null && ISWorldObjectContextMenuLogic.doWaterPipeMenu(player, worldobjects, test, canBeWaterPiped, contextWrapper, playerInv)) {
                return true;
            }
            KahluaTable groundType = (KahluaTable)fetch.rawget("groundType");
            if (groundType != null && groundType.rawget("sand") != null && playerObj.isRecipeActuallyKnown("MakeChum")) {
                ISWorldObjectContextMenuLogic.doCreateChumOptions(contextWrapper, playerObj, (IsoGridSquare)fetch.rawget("groundSquare"));
            }
            if ((sheetRopeSquare = (IsoGridSquare)fetch.rawget("sheetRopeSquare")) != null && playerObj.canClimbSheetRope(sheetRopeSquare) && playerObj.getPerkLevel(PerkFactory.Perks.Strength) >= 0) {
                if (test) {
                    return true;
                }
                contextWrapper.addGetUpOption(Translator.getText("ContextMenu_Climb_Sheet_Rope"), worldobjects, LuaManager.getFunctionObject("ISWorldObjectContextMenu.onClimbSheetRope"), sheetRopeSquare, false, player);
            }
            KahluaTableIterator iterator2 = ((KahluaTable)fetch.rawget("fluidcontainer")).iterator();
            while (iterator2.advance()) {
                IsoObject fluidcontainer = (IsoObject)iterator2.getValue();
                if (fluidcontainer == null || fluidcontainer instanceof IsoWorldInventoryObject) continue;
                ISContextMenuWrapper submenu = ISWorldObjectContextMenuLogic.doFluidContainerMenu(contextWrapper, fluidcontainer, player, playerObj, worldobjects);
                ISWorldObjectContextMenuLogic.addFluidFromItem(fetch, test, submenu, fluidcontainer, worldobjects, playerObj, playerInv);
                if (!fluidcontainer.hasComponent(ComponentType.ContextMenuConfig)) continue;
                ISWorldObjectContextMenuLogic.doContextConfigOptions(submenu, fluidcontainer, playerObj);
            }
            IsoClothingWasher clothingWasher = (IsoClothingWasher)fetch.rawget("clothingWasher");
            IsoCombinationWasherDryer comboWasherDryer = (IsoCombinationWasherDryer)fetch.rawget("comboWasherDryer");
            KahluaTable fluidcontainer = (KahluaTable)fetch.rawget("fluidcontainer");
            KahluaTable fetchStoreWater = (KahluaTable)fetch.rawget("storeWater");
            if (ISWorldObjectContextMenuLogic.doWashClothingOrYourselfMenu(fetch, player, worldobjects, fetchStoreWater, fluidcontainer, contextWrapper, clothingWasher, comboWasherDryer, playerObj)) {
                return true;
            }
            if (fetchStoreWater.isEmpty() && comboWasherDryer != null && ISWorldObjectContextMenuLogic.toggleComboWasherDryer(contextWrapper, playerObj, comboWasherDryer, true)) {
                return true;
            }
            LuaHelpers.callLuaClass("ISFeedingTroughMenu", "OnFillWorldObjectContextMenu", null, player, context, worldobjects, test);
            IsoObject rainCollectorBarrel = (IsoObject)fetch.rawget("rainCollectorBarrel");
            if (rainCollectorBarrel != null && !LuaHelpers.tableContainsValue(fluidcontainer, rainCollectorBarrel)) {
                ISWorldObjectContextMenuLogic.addFluidFromItem(fetch, test, contextWrapper, rainCollectorBarrel, worldobjects, playerObj, playerInv);
            }
            if ((worldItem = (IsoWorldInventoryObject)fetch.rawget("worldItem")) != null && worldItem.getItem() != null && worldItem.getFluidCapacity() > 0.0f && !LuaHelpers.tableContainsValue(fluidcontainer, worldItem)) {
                ISWorldObjectContextMenuLogic.addFluidFromItem(fetch, test, contextWrapper, worldItem, worldobjects, playerObj, playerInv);
            }
            if ((clothingDryer = (IsoClothingDryer)fetch.rawget("clothingDryer")) != null) {
                ISWorldObjectContextMenuLogic.onWashingDryer(ISWorldObjectContextMenuLogic.getMoveableDisplayName(clothingDryer), contextWrapper, clothingDryer, playerObj, worldobjects);
            }
            if (ISWorldObjectContextMenuLogic.doStoveOption(fetch, test, contextWrapper, player, playerObj)) {
                return true;
            }
            IsoObject bed = (IsoObject)fetch.rawget("bed");
            if ((bed != null || playerObj.getStats().get(CharacterStat.FATIGUE) > 0.9f) && ISWorldObjectContextMenuLogic.doBedMenu(player, test, contextWrapper, bed, playerObj)) {
                return true;
            }
            boolean lightSwitchResult = LuaHelpers.castBoolean(LuaHelpers.callLuaClass("ISWorldObjectContextMenu", "doLightSwitchOption", null, test, context, player));
            if (lightSwitchResult) {
                return true;
            }
            if (ISWorldObjectContextMenuLogic.doTreeMenu(fetch, context, worldobjects, test, playerObj, playerInv, contextWrapper)) {
                return true;
            }
            if (clickedSquare != null && ISWorldObjectContextMenuLogic.doFishingMenu(fetch, worldobjects, test, contextWrapper, playerObj, clickedSquare, player, playerInv)) {
                return true;
            }
            if (ISWorldObjectContextMenuLogic.doFuelMenu(fetch, context, player, test, contextWrapper)) {
                return true;
            }
            IsoPlayer clickedPlayer = (IsoPlayer)fetch.rawget("clickedPlayer");
            if (clickedPlayer != null && clickedPlayer != playerObj && ISWorldObjectContextMenuLogic.doClickedPlayerMenu(worldobjects, test, playerObj, clickedPlayer, contextWrapper)) {
                return true;
            }
            if (ISWorldObjectContextMenuLogic.doCleaningMenu(fetch, player, worldobjects, test, playerObj, contextWrapper)) {
                return true;
            }
        }
        if ((safehouse = (SafeHouse)fetch.rawget("safehouse")) != null && safehouse.playerAllowed(playerObj)) {
            if (test) {
                return true;
            }
            contextWrapper.addOption(Translator.getText("ContextMenu_ViewSafehouse"), worldobjects, LuaManager.getFunctionObject("ISWorldObjectContextMenu.onViewSafeHouse"), safehouse, playerObj);
        }
        if (!(safehouseAllowLoot = LuaHelpers.castBoolean(fetch.rawget("safehouseAllowLoot")))) {
            KahluaTable nope = contextWrapper.addOption(Translator.getText("ContextMenu_NoSafehousePermissionObjects"), null, null, new Object[0]);
            nope.rawset("notAvailable", (Object)true);
            LuaHelpers.callLuaClass("HaloTextHelper", "addBadText", null, playerObj, Translator.getText("ContextMenu_NoSafehousePermissionObjects"));
        }
        if (safehouse == null && clickedSquare != null && clickedSquare.getBuilding() != null && clickedSquare.getBuilding().getDef() != null && (reason = SafeHouse.canBeSafehouse(clickedSquare, playerObj)) != null) {
            if (test) {
                return true;
            }
            KahluaTable option = contextWrapper.addOption(Translator.getText("ContextMenu_SafehouseClaim"), worldobjects, LuaManager.getFunctionObject("ISWorldObjectContextMenu.onTakeSafeHouse"), clickedSquare, player);
            if (!reason.isEmpty()) {
                ISToolTipWrapper toolTip = ISWorldObjectContextMenuLogic.addToolTip();
                toolTip.setVisible(false);
                toolTip.getTable().rawset("description", (Object)reason);
                option.rawset("notAvailable", (Object)true);
                option.rawset("toolTip", (Object)toolTip.getTable());
            }
        }
        if (safehouseAllowInteract) {
            Object animaltrack;
            KahluaTable clickedAnimals = (KahluaTable)fetch.rawget("clickedAnimals");
            if (clickedAnimals != null && !clickedAnimals.isEmpty()) {
                LuaEventManager.triggerEvent("OnClickedAnimalForContext", player, context, clickedAnimals, test);
            }
            if ((animaltrack = fetch.rawget("animaltrack")) != null) {
                LuaHelpers.callLuaClass("ISAnimalTracksMenu", "handleIsoTracks", null, context, animaltrack, playerObj);
            }
        }
        if (SafeHouse.isSafehouseAllowClaimWar(safehouse, playerObj)) {
            contextWrapper.addOption(Translator.getText("ContextMenu_WarClaim"), worldobjects, LuaManager.getFunctionObject("ISWorldObjectContextMenu.onClaimWar"), safehouse.getOnlineID(), playerObj.getUsername());
        }
        if ((animalZone = (DesignationZoneAnimal)fetch.rawget("animalZone")) != null) {
            LuaHelpers.callLuaClass("AnimalContextMenu", "doDesignationZoneMenu", null, context, animalZone, playerObj);
        }
        if (safehouseAllowInteract && playerObj.getVehicle() == null) {
            if (test) {
                return true;
            }
            InventoryItem rakedung = playerInv.getFirstRecurse(item -> !item.isBroken() && item.hasTag(ItemTag.TAKE_DUNG));
            if (playerObj.getPrimaryHandItem() != null && !playerObj.getPrimaryHandItem().isBroken() && playerObj.getPrimaryHandItem().hasTag(ItemTag.TAKE_DUNG)) {
                rakedung = playerObj.getPrimaryHandItem();
            }
            InventoryItem shovel = playerInv.getFirstRecurse(item -> !item.isBroken() && item.hasTag(ItemTag.DIG_GRAVE));
            if (playerObj.getPrimaryHandItem() != null && !playerObj.getPrimaryHandItem().isBroken() && playerObj.getPrimaryHandItem().hasTag(ItemTag.DIG_GRAVE)) {
                shovel = playerObj.getPrimaryHandItem();
            }
            IsoObject graves = (IsoObject)fetch.rawget("graves");
            if (shovel != null && ISWorldObjectContextMenuLogic.doShovelAndRackMenu(player, worldobjects, test, contextWrapper, shovel, rakedung, playerObj, graves)) {
                return true;
            }
            if (graves != null && !ISWorldObjectContextMenuLogic.isGraveFullOfCorpses(graves) && (playerObj.isGrappling() || playerObj.getPrimaryHandItem() != null && playerObj.getPrimaryHandItem().hasTag(ItemTag.ANIMAL_CORPSE)) && ISWorldObjectContextMenuLogic.doBuryCorpseMenu(player, test, graves, contextWrapper, playerObj)) {
                return true;
            }
            if (rakedung != null && playerObj.getVehicle() == null && rakedung != shovel) {
                KahluaTable rakeOption = contextWrapper.addOption(Translator.getText("ContextMenu_Rake"), worldobjects, null, new Object[0]);
                ISContextMenuWrapper rakeMenu = ISContextMenuWrapper.getNew(contextWrapper);
                contextWrapper.addSubMenu(rakeOption, rakeMenu.getTable());
                if (test) {
                    return true;
                }
                rakeMenu.addGetUpOption(Translator.getText("ContextMenu_RakeDung"), playerObj, LuaManager.getFunctionObject("ISWorldObjectContextMenu.onRakeDung"), rakedung);
            }
            ISWorldObjectContextMenuLogic.doGardeningSubmenu(contextWrapper, worldobjects, playerObj, fetch, test);
            LuaHelpers.callLuaClass("ISDisassembleMenu", "createMenu", worldobjects, context, playerObj);
            ISWorldObjectContextMenuLogic.doDestroyMenu(worldobjects, playerInv, contextWrapper, playerObj);
        }
        if (playerObj.getVehicle() == null && !playerObj.isSitOnGround() && !playerObj.isSittingOnFurniture()) {
            if (test) {
                return true;
            }
            if (safehouseAllowInteract) {
                contextWrapper.addOption(Translator.getText("ContextMenu_SitGround"), player, LuaManager.getFunctionObject("ISWorldObjectContextMenu.onSitOnGround"), new Object[0]);
            }
        }
        if (safehouseAllowInteract && LuaHelpers.getJoypadState(player) == null && playerObj.getVehicle() == null) {
            if (test) {
                return true;
            }
            contextWrapper.addGetUpOption(Translator.getText("ContextMenu_Walk_to"), worldobjects, LuaManager.getFunctionObject("ISWorldObjectContextMenu.onWalkTo"), fetch.rawget("item"), player);
        }
        return false;
    }

    private static void doDestroyMenu(KahluaTable worldobjects, ItemContainer playerInv, ISContextMenuWrapper contextWrapper, IsoPlayer playerObj) {
        InventoryItem sledgehammer = playerInv.getFirstTagRecurse(ItemTag.SLEDGEHAMMER);
        if (sledgehammer == null && Core.debug) {
            sledgehammer = InventoryItemFactory.CreateItem(ItemKey.Weapon.SLEDGEHAMMER);
        }
        if (!(sledgehammer == null || sledgehammer.isBroken() || GameClient.client && !ServerOptions.getInstance().getBoolean("AllowDestructionBySledgehammer").booleanValue())) {
            KahluaTable option = contextWrapper.addOption(Translator.getText("ContextMenu_Destroy"), worldobjects, LuaManager.getFunctionObject("ISWorldObjectContextMenu.onDestroy"), playerObj, sledgehammer);
            option.rawset("iconTexture", (Object)sledgehammer.getIcon().splitIcon());
        }
    }

    private static boolean doClickedPlayerMenu(KahluaTable worldobjects, boolean test, IsoPlayer playerObj, IsoPlayer clickedPlayer, ISContextMenuWrapper contextWrapper) {
        KahluaTable option;
        if (!playerObj.hasTrait(CharacterTrait.HEMOPHOBIC) && !(clickedPlayer instanceof IsoAnimal)) {
            if (test) {
                return true;
            }
            option = contextWrapper.addGetUpOption(Translator.getText("ContextMenu_Medical_Check"), worldobjects, LuaManager.getFunctionObject("ISWorldObjectContextMenu.onMedicalCheck"), playerObj, clickedPlayer);
            if (Math.abs(playerObj.getX() - clickedPlayer.getX()) > 2.0f || Math.abs(playerObj.getY() - clickedPlayer.getY()) > 2.0f) {
                ISToolTipWrapper tooltip = ISWorldObjectContextMenuLogic.addToolTip();
                option.rawset("notAvailable", (Object)true);
                tooltip.getTable().rawset("description", (Object)Translator.getText("ContextMenu_GetCloser", clickedPlayer.getDisplayName()));
                option.rawset("toolTip", (Object)tooltip.getTable());
            }
        }
        if (clickedPlayer.isAsleep() && Core.debug) {
            if (test) {
                return true;
            }
            option = contextWrapper.addGetUpOption(Translator.getText("ContextMenu_Wake_Other", clickedPlayer.getDisplayName()), worldobjects, LuaManager.getFunctionObject("ISWorldObjectContextMenu.onWakeOther"), playerObj, clickedPlayer);
        }
        if (GameClient.client && !clickedPlayer.isAnimal() && GameClient.canSeePlayerStats()) {
            if (test) {
                return true;
            }
            contextWrapper.addGetUpOption("Check Stats", worldobjects, LuaManager.getFunctionObject("ISWorldObjectContextMenu.onCheckStats"), playerObj, clickedPlayer);
        }
        if (!clickedPlayer.isAsleep() && !clickedPlayer.isAnimal() && GameClient.client) {
            KahluaTable tradingUiInstance;
            KahluaTable tradingUi = (KahluaTable)LuaManager.env.rawget("ISTradingUI");
            KahluaTable kahluaTable = tradingUiInstance = tradingUi != null ? (KahluaTable)tradingUi.rawget("instance") : null;
            if (tradingUiInstance == null || !LuaHelpers.castBoolean(LuaHelpers.callLuaClass("ISTradingUI", "isVisible", tradingUiInstance, new Object[0]))) {
                KahluaTable option2 = contextWrapper.addGetUpOption(Translator.getText("ContextMenu_Trade", clickedPlayer.getDisguisedDisplayName()), worldobjects, LuaManager.getFunctionObject("ISWorldObjectContextMenu.onTrade"), playerObj, clickedPlayer);
                if (Math.abs(playerObj.getX() - clickedPlayer.getX()) > 2.0f || Math.abs(playerObj.getY() - clickedPlayer.getY()) > 2.0f) {
                    ISToolTipWrapper tooltip = ISWorldObjectContextMenuLogic.addToolTip();
                    option2.rawset("notAvailable", (Object)true);
                    tooltip.getTable().rawset("description", (Object)Translator.getText("ContextMenu_GetCloserToTrade", clickedPlayer.getDisguisedDisplayName()));
                    option2.rawset("toolTip", (Object)tooltip.getTable());
                }
            }
        }
        return false;
    }

    private static boolean doBurnBodyMenu(double player, KahluaTable worldobjects, boolean test, ItemContainer playerInv, ISContextMenuWrapper contextWrapper, IsoDeadBody body) {
        boolean hasHalfLitrePetrol;
        boolean bl = hasHalfLitrePetrol = playerInv.getFirstRecurse(item -> {
            FluidContainer fc = item.getFluidContainer();
            return fc != null && fc.contains(Fluid.Petrol) && fc.getAmount() > 0.5f;
        }) != null;
        if (hasHalfLitrePetrol && (playerInv.containsTagRecurse(ItemTag.START_FIRE) || playerInv.containsTypeRecurse("Lighter") || playerInv.containsTypeRecurse("Matches"))) {
            if (test) {
                return true;
            }
            contextWrapper.addGetUpOption(Translator.getText("ContextMenu_Burn_Corpse"), worldobjects, LuaManager.getFunctionObject("ISWorldObjectContextMenu.onBurnCorpse"), player, body);
        }
        return false;
    }

    private static boolean doExtinguishFireMenu(KahluaTable worldobjects, boolean test, ISContextMenuWrapper contextWrapper, IsoGridSquare firetile, InventoryItem extinguisher, IsoPlayer playerObj) {
        if (test) {
            return true;
        }
        contextWrapper.addGetUpOption(Translator.getText("ContextMenu_ExtinguishFire"), worldobjects, LuaManager.getFunctionObject("ISWorldObjectContextMenu.onRemoveFire"), firetile, extinguisher, playerObj);
        return false;
    }

    private static boolean doBuryCorpseMenu(double player, boolean test, IsoObject graves, ISContextMenuWrapper contextWrapper, IsoPlayer playerObj) {
        if (test) {
            return true;
        }
        Double corpseCount = LuaHelpers.castDouble(graves.getModData().rawget("corpses"));
        KahluaTable option = contextWrapper.addGetUpOption(Translator.getText("ContextMenu_BuryCorpse", corpseCount.intValue()), graves, LuaManager.getFunctionObject("ISWorldObjectContextMenu.onBuryCorpse"), player, playerObj.getPrimaryHandItem());
        if (playerObj.DistToSquared(graves.getX() + 0.5f, graves.getY() + 0.5f) > 1.5f) {
            option.rawset("notAvailable", (Object)true);
            ISToolTipWrapper toolTipWrapper = new ISToolTipWrapper();
            toolTipWrapper.initialise();
            toolTipWrapper.setVisible(false);
            toolTipWrapper.setName(Translator.getText("ContextMenu_BuryCorpse", corpseCount));
            toolTipWrapper.getTable().rawset("description", (Object)Translator.getText("Tooltip_grave_addcorpse_far"));
        }
        return false;
    }

    private static boolean doWashClothingOrYourselfMenu(KahluaTable fetch, double player, KahluaTable worldobjects, KahluaTable fetchStoreWater, KahluaTable fluidcontainer, ISContextMenuWrapper contextWrapper, IsoClothingWasher clothingWasher, IsoCombinationWasherDryer comboWasherDryer, IsoPlayer playerObj) {
        KahluaTableIterator iterator2 = fetchStoreWater.iterator();
        while (iterator2.advance()) {
            IsoWorldInventoryObject isoWorldInventoryObject;
            IsoObject storeWater = (IsoObject)iterator2.getValue();
            if (storeWater == null || LuaHelpers.tableContainsValue(fluidcontainer, storeWater)) continue;
            String source2 = ISWorldObjectContextMenuLogic.getMoveableDisplayName(storeWater);
            if (source2 == null && storeWater instanceof IsoWorldInventoryObject && (isoWorldInventoryObject = (IsoWorldInventoryObject)storeWater).getItem() != null) {
                source2 = storeWater.getFluidUiName();
            }
            if (source2 == null) {
                source2 = Translator.getText("ContextMenu_NaturalWaterSource");
            }
            KahluaTable mainOption = contextWrapper.addOption(source2, null, null, new Object[0]);
            Texture texture = Texture.trygetTexture(storeWater.getSpriteName());
            if (texture instanceof Texture) {
                Texture texture2 = texture;
                mainOption.rawset("iconTexture", (Object)texture2.splitIcon());
            }
            ISContextMenuWrapper mainSubMenu = ISContextMenuWrapper.getNew(contextWrapper);
            contextWrapper.addSubMenu(mainOption, mainSubMenu.getTable());
            if (storeWater.hasWater() && fetch.rawget("clothingDryer") == null && storeWater != clothingWasher && storeWater != comboWasherDryer) {
                ISWorldObjectContextMenuLogic.doWashClothingMenu(storeWater, player, playerObj, mainSubMenu);
                ISWorldObjectContextMenuLogic.doRecipeUsingWaterMenu(storeWater, playerObj, mainSubMenu);
            }
            if (!Core.getInstance().getGameMode().equals("LastStand")) {
                ISWorldObjectContextMenuLogic.doDrinkWaterMenu(storeWater, player, playerObj, worldobjects, mainSubMenu);
                ISWorldObjectContextMenuLogic.doFillFluidMenu(storeWater, player, playerObj, worldobjects, mainSubMenu);
            }
            if (storeWater == clothingWasher && ISWorldObjectContextMenuLogic.toggleClothingWasher(mainSubMenu, worldobjects, player, playerObj, clothingWasher)) {
                return true;
            }
            if (storeWater != comboWasherDryer || !ISWorldObjectContextMenuLogic.toggleComboWasherDryer(mainSubMenu, playerObj, comboWasherDryer, false)) continue;
            return true;
        }
        return false;
    }

    private static boolean doShovelAndRackMenu(double player, KahluaTable worldobjects, boolean test, ISContextMenuWrapper contextWrapper, InventoryItem shovel, InventoryItem rakedung, IsoPlayer playerObj, IsoObject graves) {
        KahluaTable shovelOption = contextWrapper.addOption(Translator.getText("ContextMenu_Shovel"), worldobjects, null, new Object[0]);
        shovelOption.rawset("iconTexture", (Object)shovel.getTex());
        ISContextMenuWrapper shovelMenu = ISContextMenuWrapper.getNew(contextWrapper);
        contextWrapper.addSubMenu(shovelOption, shovelMenu.getTable());
        if (rakedung != null && playerObj.getVehicle() == null) {
            if (test) {
                return true;
            }
            shovelMenu.addGetUpOption(Translator.getText("ContextMenu_RakeDung"), playerObj, LuaManager.getFunctionObject("ISWorldObjectContextMenu.onRakeDung"), rakedung);
        }
        if ((LuaHelpers.getJoypadState(player) != null || ISWorldObjectContextMenuLogic.ISEmptyGraves_canDigHere(worldobjects)) && playerObj.getVehicle() == null) {
            if (test) {
                return true;
            }
            shovelMenu.addGetUpOption(Translator.getText("ContextMenu_DigGraves"), worldobjects, LuaManager.getFunctionObject("ISWorldObjectContextMenu.onDigGraves"), player, shovel);
        }
        if (graves != null) {
            if (test) {
                return true;
            }
            shovelMenu.addGetUpOption(Translator.getText("ContextMenu_FillGrave", LuaHelpers.castDouble(graves.getModData().rawget("corpses")).intValue()), graves, LuaManager.getFunctionObject("ISWorldObjectContextMenu.onFillGrave"), player, shovel);
        }
        return false;
    }

    private static boolean doTrapMenu(double player, KahluaTable worldobjects, boolean test, ISContextMenuWrapper contextWrapper) {
        if (test) {
            return true;
        }
        KahluaTable doneSquare = LuaManager.platform.newTable();
        KahluaTableIterator iterator2 = worldobjects.iterator();
        while (iterator2.advance()) {
            IsoObject value = (IsoObject)iterator2.getValue();
            if (value == null || value.getSquare() == null || LuaHelpers.castBoolean(doneSquare.rawget(value.getSquare()))) continue;
            doneSquare.rawset(value.getSquare(), (Object)true);
            for (int n = 0; n < value.getSquare().getObjects().size(); ++n) {
                IsoTrap trap;
                if (!(value.getSquare().getObjects().get(n) instanceof IsoTrap) || (trap = (IsoTrap)value.getSquare().getObjects().get(n)).getItem() == null || trap.isExploding()) continue;
                contextWrapper.addGetUpOption(Translator.getText("ContextMenu_TrapTake", trap.getItem().getName()), worldobjects, LuaManager.getFunctionObject("ISWorldObjectContextMenu.onTakeTrap"), trap, player);
            }
        }
        return false;
    }

    private static boolean doPadLockMenu(KahluaTable fetch, double player, KahluaTable worldobjects, boolean test, ItemContainer playerInv, ISContextMenuWrapper contextWrapper) {
        IsoObject digitalPadlockedThump;
        IsoObject padlockedThump;
        Object padlockThump = fetch.rawget("padlockThump");
        if (padlockThump != null) {
            InventoryItem digitalPadlock;
            Key padlock = (Key)playerInv.FindAndReturn("Padlock");
            if (padlock != null && padlock.getNumberOfKey() > 0) {
                if (test) {
                    return true;
                }
                contextWrapper.addGetUpOption(Translator.getText("ContextMenu_PutPadlock"), worldobjects, LuaManager.getFunctionObject("ISWorldObjectContextMenu.onPutPadlock"), player, padlockThump, padlock);
            }
            if ((digitalPadlock = playerInv.FindAndReturn("CombinationPadlock")) != null) {
                if (test) {
                    return true;
                }
                contextWrapper.addGetUpOption(Translator.getText("ContextMenu_PutCombinationPadlock"), worldobjects, LuaManager.getFunctionObject("ISWorldObjectContextMenu.onPutDigitalPadlock"), player, padlockThump, digitalPadlock);
            }
        }
        if ((padlockedThump = (IsoObject)fetch.rawget("padlockedThump")) != null && playerInv.haveThisKeyId(padlockedThump.getKeyId()) != null) {
            if (test) {
                return true;
            }
            contextWrapper.addGetUpOption(Translator.getText("ContextMenu_RemovePadlock"), worldobjects, LuaManager.getFunctionObject("ISWorldObjectContextMenu.onRemovePadlock"), player, padlockedThump);
        }
        if ((digitalPadlockedThump = (IsoObject)fetch.rawget("digitalPadlockedThump")) != null) {
            if (test) {
                return true;
            }
            contextWrapper.addGetUpOption(Translator.getText("ContextMenu_RemoveCombinationPadlock"), worldobjects, LuaManager.getFunctionObject("ISWorldObjectContextMenu.onRemoveDigitalPadlock"), player, digitalPadlockedThump);
        }
        return false;
    }

    private static boolean doWaterPipeMenu(double player, KahluaTable worldobjects, boolean test, IsoObject canBeWaterPiped, ISContextMenuWrapper contextWrapper, ItemContainer playerInv) {
        if (test) {
            return true;
        }
        String name = ISWorldObjectContextMenuLogic.getMoveableDisplayName(canBeWaterPiped);
        if (name == null) {
            name = "";
        }
        KahluaTable option = contextWrapper.addGetUpOption(Translator.getText("ContextMenu_PlumbItem", name), worldobjects, LuaManager.getFunctionObject("ISWorldObjectContextMenu.onPlumbItem"), player, canBeWaterPiped);
        option.rawset("iconTexture", (Object)Texture.getSharedTexture("Item_PipeWrench"));
        if (playerInv.getFirstRecurse(item -> ISWorldObjectContextMenuLogic.compareType("PipeWrench", item) && !item.isBroken()) == null && playerInv.getFirstRecurse(item -> item.hasTag(ItemTag.PIPE_WRENCH) && !item.isBroken()) == null) {
            option.rawset("notAvailable", (Object)true);
            ISToolTipWrapper tooltip = ISWorldObjectContextMenuLogic.addToolTip();
            tooltip.setName(Translator.getText("ContextMenu_PlumbItem", name));
            tooltip.getTable().rawset("description", (Object)Translator.getText("Tooltip_NeedWrench", LuaManager.GlobalObject.getItemName("Base.PipeWrench")));
            option.rawset("toolTip", (Object)tooltip.getTable());
        }
        return false;
    }

    private static boolean doFishingMenu(KahluaTable fetch, KahluaTable worldobjects, boolean test, ISContextMenuWrapper contextWrapper, IsoPlayer playerObj, IsoGridSquare clickedSquare, double player, ItemContainer playerInv) {
        boolean isNoFishZone;
        if (LuaHelpers.getJoypadState(player) != null) {
            IsoGridSquare square;
            int dx;
            int dy;
            float px = playerObj.getX();
            float py = playerObj.getY();
            float pz = playerObj.getZ();
            InventoryItem rod = ISWorldObjectContextMenuLogic.getFishingRod(playerObj);
            Object lure = ISWorldObjectContextMenuLogic.getFishingLure(playerObj, rod);
            InventoryItem net = playerInv.getFirstTypeRecurse("FishingNet");
            boolean haveTarget = false;
            if (rod != null && lure != null || net != null) {
                for (dy = -5; dy <= 5; ++dy) {
                    for (dx = -5; dx <= 5; ++dx) {
                        square = IsoWorld.instance.getCell().getGridSquare(px + (float)dx, py + (float)dy, pz);
                        if (square == null || !square.has(IsoFlagType.water) || square.getObjects().isEmpty()) continue;
                        if (rod != null && lure != null) {
                            fetch.rawset("canFish", (Object)true);
                            haveTarget = true;
                        }
                        if (net == null) break;
                        fetch.rawset("canTrapFish", (Object)true);
                        haveTarget = true;
                        break;
                    }
                    if (haveTarget) break;
                }
            }
            haveTarget = false;
            block2: for (dy = -5; dy <= 5; ++dy) {
                for (dx = -5; dx <= 5; ++dx) {
                    square = IsoWorld.instance.getCell().getGridSquare(px + (float)dx, py + (float)dy, pz);
                    if (square != null && square.has(IsoFlagType.water) && !square.getObjects().isEmpty()) {
                        for (int i = 0; i < square.getObjects().size(); ++i) {
                            IsoObject v = square.getObjects().get(i);
                            if (v.getName() == null || !v.getName().equals("FishingNet")) continue;
                            fetch.rawset("trapFish", (Object)v);
                            haveTarget = true;
                            break;
                        }
                        if (haveTarget) continue block2;
                    }
                    if (haveTarget) continue block2;
                }
            }
        }
        if (LuaHelpers.castBoolean(fetch.rawget("canTrapFish"))) {
            if (test) {
                return true;
            }
            ISWorldObjectContextMenuLogic.doFishNetOptions(contextWrapper, playerObj, clickedSquare);
        }
        if (!(isNoFishZone = LuaHelpers.castBoolean(LuaHelpers.callLuaClass("Fishing", "isNoFishZone", null, clickedSquare.getX(), clickedSquare.getY())))) {
            IsoObject trapFish = (IsoObject)fetch.rawget("trapFish");
            if (trapFish != null) {
                if (test) {
                    return true;
                }
                ISWorldObjectContextMenuLogic.doPlacedFishNetOptions(contextWrapper, playerObj, trapFish);
            }
            if (LuaHelpers.castBoolean(fetch.rawget("canFish"))) {
                KahluaTable opt = contextWrapper.addOption(Translator.getText("ContextMenu_Fishing"), worldobjects, LuaManager.getFunctionObject("ISWorldObjectContextMenu.openFishWindow"), new Object[0]);
                opt.rawset("iconTexture", (Object)Texture.trygetTexture("Item_FishingRod").splitIcon());
            }
            if (LuaHelpers.castBoolean(fetch.rawget("canAddChum"))) {
                ISWorldObjectContextMenuLogic.doChumOptions(contextWrapper, playerObj, clickedSquare);
            }
        }
        return false;
    }

    private static boolean doBedMenu(double player, boolean test, ISContextMenuWrapper contextWrapper, IsoObject bed, IsoPlayer playerObj) {
        ISContextMenuWrapper bedSubmenu;
        if (bed == null) {
            ISWorldObjectContextMenuLogic.doSleepOption(contextWrapper, null, player, playerObj);
            return false;
        }
        KahluaTable bedOption = contextWrapper.getContextFromOption(bed.getTileName());
        if (bedOption == null) {
            bedOption = contextWrapper.addOption(bed.getTileName(), null, null, new Object[0]);
            Texture texture = Texture.trygetTexture(bed.getSpriteName());
            if (texture instanceof Texture) {
                Texture texture2 = texture;
                bedOption.rawset("iconTexture", (Object)texture2.splitIcon());
            }
            bedSubmenu = ISContextMenuWrapper.getNew(contextWrapper);
            contextWrapper.addSubMenu(bedOption, bedSubmenu.getTable());
        } else {
            bedSubmenu = new ISContextMenuWrapper(bedOption);
        }
        if (!ISWorldObjectContextMenuLogic.isSomethingTo(bed, playerObj) && !playerObj.isSitOnFurnitureObject(bed)) {
            if (test) {
                return true;
            }
            if (bed.getSquare().getRoom() == playerObj.getSquare().getRoom() || bed.getSquare().isCanSee((int)player)) {
                bedSubmenu.addGetUpOption(Translator.getText("ContextMenu_Rest"), bed, LuaManager.getFunctionObject("ISWorldObjectContextMenu.onRest"), player);
            }
        }
        if (LuaHelpers.castBoolean(LuaHelpers.callLuaClass("ISWorldObjectContextMenu", "chairCheck", null, bed))) {
            return false;
        }
        if (!(ISWorldObjectContextMenuLogic.isSomethingTo(bed, playerObj) || GameClient.client && !ServerOptions.getInstance().sleepAllowed.getValue())) {
            if (test) {
                return true;
            }
            ISWorldObjectContextMenuLogic.doSleepOption(bedSubmenu, bed, player, playerObj);
        }
        return false;
    }

    private static boolean doThumpableMenu(IsoThumpable thump, double player, KahluaTable worldobjects, boolean test, boolean invincibleWindow, IsoPlayer playerObj, boolean hasRemoveBarricadeTool, ISContextMenuWrapper contextWrapper) {
        if (thump.isBarricadeAllowed()) {
            IsoBarricade barricade = thump.getBarricadeForCharacter(playerObj);
            if (barricade != null && barricade.getNumPlanks() > 0 && hasRemoveBarricadeTool) {
                if (test) {
                    return true;
                }
                contextWrapper.addGetUpOption(Translator.getText("ContextMenu_Unbarricade"), worldobjects, LuaManager.getFunctionObject("ISWorldObjectContextMenu.onUnbarricade"), thump, player);
            }
            if (barricade != null && barricade.isMetal() && ISWorldObjectContextMenuLogic.checkBlowTorchForBarricade(playerObj)) {
                if (test) {
                    return true;
                }
                contextWrapper.addGetUpOption(Translator.getText("ContextMenu_Unbarricade"), worldobjects, LuaManager.getFunctionObject("ISWorldObjectContextMenu.onUnbarricadeMetal"), thump, player);
            }
            if (barricade != null && barricade.isMetalBar() && ISWorldObjectContextMenuLogic.checkBlowTorchForBarricade(playerObj)) {
                if (test) {
                    return true;
                }
                contextWrapper.addGetUpOption(Translator.getText("ContextMenu_Unbarricade"), worldobjects, LuaManager.getFunctionObject("ISWorldObjectContextMenu.onUnbarricadeMetalBar"), thump, player);
            }
        }
        return false;
    }

    private static boolean doTreeMenu(KahluaTable fetch, KahluaTable context, KahluaTable worldobjects, boolean test, IsoPlayer playerObj, ItemContainer playerInv, ISContextMenuWrapper contextWrapper) {
        InventoryItem axe;
        LuaHelpers.callLuaClass("AnimalContextMenu", "attachAnimalToObject", null, fetch.rawget("attachAnimalTo"), playerObj, worldobjects, context);
        IsoTree tree = (IsoTree)fetch.rawget("tree");
        if (tree != null && (axe = playerInv.getFirstRecurse(item -> !item.isBroken() && item.hasTag(ItemTag.CHOP_TREE))) != null) {
            if (test) {
                return true;
            }
            KahluaTable option = contextWrapper.addGetUpOption(Translator.getText("ContextMenu_Chop_Tree"), worldobjects, LuaManager.getFunctionObject("ISWorldObjectContextMenu.onChopTree"), playerObj, tree);
            option.rawset("iconTexture", (Object)axe.getTex());
        }
        return false;
    }

    private static boolean doFuelMenu(KahluaTable fetch, KahluaTable context, double player, boolean test, ISContextMenuWrapper contextWrapper) {
        IsoObject fuelPump = (IsoObject)fetch.rawget("fuelPump");
        boolean fuelPower = SandboxOptions.getInstance().allowExteriorGenerator.getValue() && fuelPump != null && fuelPump.getSquare().haveElectricity() || fuelPump != null && fuelPump.getSquare().hasGridPower();
        IsoObject haveFuel = (IsoObject)fetch.rawget("haveFuel");
        if (haveFuel != null && fuelPower) {
            if (test) {
                return true;
            }
            LuaHelpers.callLuaClass("ISWorldObjectContextMenu", "doFillFuelMenu", null, haveFuel, player, context);
        } else if (fuelPump != null && !fuelPower) {
            KahluaTable option = contextWrapper.addOption(Translator.getText("ContextMenu_FuelPumpNoPower"), null, null, new Object[0]);
            option.rawset("notAvailable", (Object)true);
        } else if (fuelPump != null && fuelPump.getPipedFuelAmount() <= 0) {
            KahluaTable option = contextWrapper.addOption(Translator.getText("ContextMenu_FuelPumpEmpty"), null, null, new Object[0]);
            option.rawset("notAvailable", (Object)true);
        } else if (fuelPump != null && fuelPump.getPipedFuelAmount() > 0) {
            KahluaTable option = contextWrapper.addOption(Translator.getText("ContextMenu_FuelPumpNoContainer"), null, null, new Object[0]);
            option.rawset("notAvailable", (Object)true);
        }
        return false;
    }

    private static boolean doPlayerMenu(KahluaTable fetch, KahluaTable worldobjects, boolean test, IsoPlayer playerObj, ISContextMenuWrapper contextWrapper) {
        IsoPlayer clickedPlayer = (IsoPlayer)fetch.rawget("clickedPlayer");
        if (clickedPlayer != null && clickedPlayer != playerObj) {
            if (!playerObj.hasTrait(CharacterTrait.HEMOPHOBIC) && !(clickedPlayer instanceof IsoAnimal)) {
                if (test) {
                    return true;
                }
                KahluaTable option = contextWrapper.addGetUpOption(Translator.getText("ContextMenu_Medical_Check"), worldobjects, LuaManager.getFunctionObject("ISWorldObjectContextMenu.onMedicalCheck"), playerObj, clickedPlayer);
                if (Math.abs(playerObj.getX() - clickedPlayer.getX()) > 2.0f || Math.abs(playerObj.getY() - clickedPlayer.getY()) > 2.0f) {
                    ISToolTipWrapper tooltip = ISWorldObjectContextMenuLogic.addToolTip();
                    option.rawset("notAvailable", (Object)true);
                    tooltip.getTable().rawset("description", (Object)Translator.getText("ContextMenu_GetCloser", clickedPlayer.getDisplayName()));
                    option.rawset("toolTip", (Object)tooltip.getTable());
                }
            }
            if (clickedPlayer.isAsleep() && Core.debug) {
                if (test) {
                    return true;
                }
                contextWrapper.addGetUpOption(Translator.getText("ContextMenu_Wake_Other", clickedPlayer.getDisplayName()), worldobjects, LuaManager.getFunctionObject("ISWorldObjectContextMenu.onWakeOther"), playerObj, clickedPlayer);
            }
            if (GameClient.client && !clickedPlayer.isAnimal() && GameClient.canSeePlayerStats()) {
                if (test) {
                    return true;
                }
                contextWrapper.addGetUpOption("Check Stats", worldobjects, LuaManager.getFunctionObject("ISWorldObjectContextMenu.onCheckStats"), playerObj, clickedPlayer);
            }
            if (!clickedPlayer.isAsleep() && !clickedPlayer.isAnimal() && GameClient.client) {
                KahluaTable ISTradingUI_instance;
                KahluaTable ISTradingUI = (KahluaTable)LuaManager.env.rawget("ISTradingUI");
                KahluaTable kahluaTable = ISTradingUI_instance = ISTradingUI != null ? (KahluaTable)ISTradingUI.rawget("instance") : null;
                if (ISTradingUI_instance == null || !LuaHelpers.castBoolean(LuaHelpers.callLuaClass("ISTradingUI", "isVisible", ISTradingUI_instance, new Object[0]))) {
                    KahluaTable option = contextWrapper.addGetUpOption(Translator.getText("ContextMenu_Trade", clickedPlayer.getDisplayName()), worldobjects, LuaManager.getFunctionObject("ISWorldObjectContextMenu.onTrade"), playerObj, clickedPlayer);
                    if (Math.abs(playerObj.getX() - clickedPlayer.getX()) > 2.0f || Math.abs(playerObj.getY() - clickedPlayer.getY()) > 2.0f) {
                        ISToolTipWrapper tooltip = ISWorldObjectContextMenuLogic.addToolTip();
                        option.rawset("notAvailable", (Object)true);
                        tooltip.getTable().rawset("description", (Object)Translator.getText("ContextMenu_GetCloserToTrade", clickedPlayer.getDisplayName()));
                        option.rawset("toolTip", (Object)tooltip.getTable());
                    }
                }
            }
        }
        return false;
    }

    private static boolean doCarBatteryMenu(KahluaTable fetch, KahluaTable context, KahluaTable worldobjects, boolean test, IsoPlayer playerObj, ItemContainer playerInv) {
        IsoCarBatteryCharger carBatteryCharger = (IsoCarBatteryCharger)fetch.rawget("carBatteryCharger");
        return carBatteryCharger != null && LuaHelpers.castBoolean(LuaHelpers.callLuaClass("ISWorldObjectContextMenu", "handleCarBatteryCharger", null, test, context, worldobjects, playerObj, playerInv));
    }

    private static boolean doCleaningMenu(KahluaTable fetch, double player, KahluaTable worldobjects, boolean test, IsoPlayer playerObj, ISContextMenuWrapper contextWrapper) {
        IsoGridSquare haveGraffiti;
        IsoGridSquare haveBlood = (IsoGridSquare)fetch.rawget("haveBlood");
        if (haveBlood != null && playerObj.getVehicle() == null) {
            if (test) {
                return true;
            }
            KahluaTable option = contextWrapper.addGetUpOption(Translator.getText("ContextMenu_CleanStains"), worldobjects, LuaManager.getFunctionObject("ISWorldObjectContextMenu.onCleanBlood"), haveBlood, player);
            ISToolTipWrapper tooltip = ISWorldObjectContextMenuLogic.addToolTip();
            tooltip.getTable().rawset("description", (Object)Translator.getText("Tooltip_CleanStains"));
            option.rawset("toolTip", (Object)tooltip.getTable());
        }
        if ((haveGraffiti = (IsoGridSquare)fetch.rawget("haveGraffiti")) != null && playerObj.getVehicle() == null) {
            if (test) {
                return true;
            }
            KahluaTable option = contextWrapper.addGetUpOption(Translator.getText("ContextMenu_CleanGraffiti"), worldobjects, LuaManager.getFunctionObject("ISWorldObjectContextMenu.onCleanGraffiti"), haveGraffiti, player);
            ISToolTipWrapper tooltip = ISWorldObjectContextMenuLogic.addToolTip();
            tooltip.getTable().rawset("description", (Object)Translator.getText("Tooltip_CleanGraffiti"));
            option.rawset("toolTip", (Object)tooltip.getTable());
        }
        return false;
    }

    private static boolean doGeneratorMenu(KahluaTable fetch, double player, KahluaTable worldobjects, boolean test, IsoPlayer playerObj, ISContextMenuWrapper contextWrapper, ItemContainer playerInv) {
        IsoGenerator generator = (IsoGenerator)fetch.rawget("generator");
        if (generator != null && playerObj.getVehicle() == null) {
            ISToolTipWrapper tooltip;
            boolean canDo;
            boolean bl = canDo = playerObj.getPerkLevel(PerkFactory.Perks.Electricity) >= 3 || playerObj.isRecipeActuallyKnown("Generator");
            if (test) {
                return true;
            }
            KahluaTable generatorOption = contextWrapper.addOption(Translator.getText("ContextMenu_Generator"), worldobjects, null, new Object[0]);
            generatorOption.rawset("iconTexture", (Object)Texture.getSharedTexture("Item_Generator"));
            ISContextMenuWrapper generatorSubmenu = ISContextMenuWrapper.getNew(contextWrapper);
            contextWrapper.addSubMenu(generatorOption, generatorSubmenu.getTable());
            KahluaTable option = generatorSubmenu.addGetUpOption(Translator.getText("ContextMenu_GeneratorInfo"), worldobjects, LuaManager.getFunctionObject("ISWorldObjectContextMenu.onInfoGenerator"), generator, player);
            if (playerObj.DistToSquared(generator.getX() + 0.5f, generator.getY() + 0.5f) < 4.0f) {
                tooltip = ISWorldObjectContextMenuLogic.addToolTip();
                tooltip.setName(Translator.getText("IGUI_Generator_TypeGas"));
                tooltip.getTable().rawset("description", LuaHelpers.callLuaClass("ISGeneratorInfoWindow", "getRichText", null, generator, true));
                option.rawset("toolTip", (Object)tooltip.getTable());
            }
            if (generator.isConnected()) {
                if (generator.isActivated()) {
                    generatorSubmenu.addGetUpOption(Translator.getText("ContextMenu_Turn_Off"), worldobjects, LuaManager.getFunctionObject("ISWorldObjectContextMenu.onActivateGenerator"), false, generator, player);
                } else {
                    option = generatorSubmenu.addGetUpOption(Translator.getText("ContextMenu_GeneratorUnplug"), worldobjects, LuaManager.getFunctionObject("ISWorldObjectContextMenu.onPlugGenerator"), generator, player, false);
                    if (generator.getFuel() > 0.0f) {
                        option = generatorSubmenu.addGetUpOption(Translator.getText("ContextMenu_Turn_On"), worldobjects, LuaManager.getFunctionObject("ISWorldObjectContextMenu.onActivateGenerator"), true, generator, player);
                        boolean doStats = playerObj.DistToSquared(generator.getX() + 0.5f, generator.getY() + 0.5f) < 4.0f;
                        String description = LuaHelpers.castString(LuaHelpers.callLuaClass("ISGeneratorInfoWindow", "getRichText", null, generator, doStats));
                        if (!description.isEmpty()) {
                            ISToolTipWrapper tooltip2 = ISWorldObjectContextMenuLogic.addToolTip();
                            tooltip2.setName(Translator.getText("IGUI_Generator_TypeGas"));
                            tooltip2.getTable().rawset("description", (Object)description);
                            option.rawset("toolTip", (Object)tooltip2.getTable());
                        }
                    }
                }
            } else {
                option = generatorSubmenu.addGetUpOption(Translator.getText("ContextMenu_GeneratorPlug"), worldobjects, LuaManager.getFunctionObject("ISWorldObjectContextMenu.onPlugGenerator"), generator, player, true);
                if (!canDo) {
                    tooltip = ISWorldObjectContextMenuLogic.addToolTip();
                    option.rawset("notAvailable", (Object)true);
                    tooltip.getTable().rawset("description", (Object)Translator.getText("ContextMenu_GeneratorPlugTT"));
                    option.rawset("toolTip", (Object)tooltip.getTable());
                }
            }
            Predicate<InventoryItem> predicatePetrol = item -> {
                FluidContainer fc = item.getFluidContainer();
                return fc != null && fc.contains(Fluid.Petrol) && fc.getAmount() >= 0.099f;
            };
            if (!generator.isActivated() && generator.getFuelPercentage() < 100.0f && playerInv.getFirstRecurse(predicatePetrol) != null) {
                InventoryItem petrolCan = playerInv.getFirstRecurse(predicatePetrol);
                LuaHelpers.callLuaClass("ISWorldObjectContextMenu", "onAddFuelGenerator", null, worldobjects, petrolCan, generator, player, contextWrapper.getContextFromOption(Translator.getText("ContextMenu_Generator")));
            }
            if (!generator.isActivated() && (float)generator.getCondition() < 100.0f) {
                ISToolTipWrapper tooltip3;
                option = generatorSubmenu.addGetUpOption(Translator.getText("ContextMenu_GeneratorFix"), worldobjects, LuaManager.getFunctionObject("ISWorldObjectContextMenu.onFixGenerator"), generator, player);
                if (!canDo) {
                    tooltip3 = ISWorldObjectContextMenuLogic.addToolTip();
                    option.rawset("notAvailable", (Object)true);
                    tooltip3.getTable().rawset("description", (Object)Translator.getText("ContextMenu_GeneratorPlugTT"));
                    option.rawset("toolTip", (Object)tooltip3.getTable());
                }
                if (!playerInv.containsTypeRecurse("ElectronicsScrap")) {
                    tooltip3 = ISWorldObjectContextMenuLogic.addToolTip();
                    option.rawset("notAvailable", (Object)true);
                    tooltip3.getTable().rawset("description", (Object)Translator.getText("ContextMenu_GeneratorFixTT"));
                    option.rawset("toolTip", (Object)tooltip3.getTable());
                }
            }
            if (!generator.isConnected()) {
                generatorSubmenu.addGetUpOption(Translator.getText("ContextMenu_GeneratorTake"), worldobjects, LuaManager.getFunctionObject("ISWorldObjectContextMenu.onTakeGenerator"), generator, player);
            }
        }
        return false;
    }

    private static ISContextMenuWrapper doDoorMenu(KahluaTable fetch, KahluaTable context, KahluaTable worldobjects, boolean test, ISContextMenuWrapper contextWrapper, IsoPlayer playerObj, boolean hasRemoveBarricadeTool) {
        ItemContainer playerInv = playerObj.getInventory();
        String doorOptionTxt = Translator.getText("Door");
        KahluaTable doorOption = contextWrapper.addOption(doorOptionTxt, null, null, new Object[0]);
        ISContextMenuWrapper doorContextWrapper = ISContextMenuWrapper.getNew(contextWrapper);
        contextWrapper.addSubMenu(doorOption, doorContextWrapper.getTable());
        KahluaTable doorContext = contextWrapper.getContextFromOption(doorOptionTxt);
        ILockableDoor door = (ILockableDoor)fetch.rawget("door");
        Object doorKeyId = fetch.rawget("doorKeyId");
        BarricadeAble barricadeAbleDoor = (BarricadeAble)fetch.rawget("door");
        if (door != null) {
            Texture texture;
            int doorKeyIdInt;
            IsoObject doorIsoObj;
            ISToolTipWrapper tooltip;
            String text;
            ICurtain doorCurtain = door.HasCurtains();
            if (doorCurtain != null) {
                if (test) {
                    return null;
                }
                text = Translator.getText(doorCurtain.isCurtainOpen() ? "ContextMenu_Close_curtains" : "ContextMenu_Open_curtains");
                doorContextWrapper.addGetUpOption(text, worldobjects, LuaManager.getFunctionObject("ISWorldObjectContextMenu.onOpenCloseCurtain"), door, playerObj.getPlayerNum());
                LuaHelpers.callLuaClass("ISWorldObjectContextMenu", "addRemoveCurtainOption", null, doorContext, worldobjects, door, playerObj.getPlayerNum());
            } else if (door.canAddCurtain() && playerInv.containsTypeRecurse("Sheet")) {
                if (test) {
                    return null;
                }
                KahluaTable addSheetOption = doorContextWrapper.addGetUpOption(Translator.getText("ContextMenu_Add_sheet"), worldobjects, LuaManager.getFunctionObject("ISWorldObjectContextMenu.onAddSheet"), door, playerObj.getPlayerNum());
                addSheetOption.rawset("iconTexture", (Object)Texture.getSharedTexture("Item_Sheet"));
            }
            if (door.couldBeOpen(playerObj)) {
                if (test) {
                    return null;
                }
                text = Translator.getText("ContextMenu_Open_door");
                if (door.IsOpen()) {
                    text = Translator.getText("ContextMenu_Close_door");
                }
                KahluaTable opendooroption = doorContextWrapper.addGetUpOption(text, worldobjects, LuaManager.getFunctionObject("ISWorldObjectContextMenu.onOpenCloseDoor"), door, playerObj.getPlayerNum());
                if (LuaHelpers.getJoypadState(playerObj.getPlayerNum()) == null) {
                    tooltip = ISWorldObjectContextMenuLogic.addToolTip();
                    tooltip.setName(Translator.getText("ContextMenu_Info"));
                    tooltip.getTable().rawset("description", (Object)Translator.getText("Tooltip_OpenClose", Input.getKeyName(Core.getInstance().getKey("Interact"))));
                    opendooroption.rawset("toolTip", (Object)tooltip.getTable());
                }
            }
            if ((doorIsoObj = (IsoObject)((Object)door)).isHoppable() && door.canClimbOver(playerObj)) {
                KahluaTable option = doorContextWrapper.addGetUpOption(Translator.getText("ContextMenu_Climb_over"), worldobjects, LuaManager.getFunctionObject("ISWorldObjectContextMenu.onClimbOverFence"), door, null, playerObj.getPlayerNum());
                if (LuaHelpers.getJoypadState(playerObj.getPlayerNum()) == null) {
                    tooltip = ISWorldObjectContextMenuLogic.addToolTip();
                    tooltip.setName(Translator.getText("ContextMenu_Info"));
                    tooltip.getTable().rawset("description", (Object)Translator.getText("Tooltip_Climb", Input.getKeyName(Core.getInstance().getKey("Interact"))));
                    option.rawset("toolTip", (Object)tooltip.getTable());
                }
            }
            if (!(door.IsOpen() || doorKeyId == null || playerInv.haveThisKeyId(doorKeyIdInt = ((Integer)doorKeyId).intValue()) == null && (playerObj.getCurrentSquare().has(IsoFlagType.exterior) || ((IsoObject)((Object)door)).getProperties().has(IsoPropertyType.FORCE_LOCKED)))) {
                if (test) {
                    return null;
                }
                if (!door.isLockedByKey()) {
                    doorContextWrapper.addGetUpOption(Translator.getText("ContextMenu_LockDoor"), worldobjects, LuaManager.getFunctionObject("ISWorldObjectContextMenu.onLockDoor"), playerObj.getPlayerNum(), door);
                } else {
                    doorContextWrapper.addGetUpOption(Translator.getText("ContextMenu_UnlockDoor"), worldobjects, LuaManager.getFunctionObject("ISWorldObjectContextMenu.onUnLockDoor"), playerObj.getPlayerNum(), door, doorKeyId);
                }
            }
            if ((texture = Texture.trygetTexture(doorIsoObj.getSpriteName())) instanceof Texture) {
                Texture texture2 = texture;
                doorOption.rawset("iconTexture", (Object)texture2.splitIcon());
            }
            ISWorldObjectContextMenuLogic.doBarricadeMenu(barricadeAbleDoor, playerObj, playerInv, doorContextWrapper);
            IsoBarricade barricade = barricadeAbleDoor.getBarricadeForCharacter(playerObj);
            if (barricade != null && barricade.getNumPlanks() > 0 && hasRemoveBarricadeTool) {
                if (test) {
                    return null;
                }
                doorContextWrapper.addGetUpOption(Translator.getText("ContextMenu_Unbarricade"), worldobjects, LuaManager.getFunctionObject("ISWorldObjectContextMenu.onUnbarricade"), door, playerObj.getPlayerNum());
            }
            if (barricade != null && barricade.isMetal() && ISWorldObjectContextMenuLogic.checkBlowTorchForBarricade(playerObj)) {
                if (test) {
                    return null;
                }
                doorContextWrapper.addGetUpOption(Translator.getText("ContextMenu_Unbarricade"), worldobjects, LuaManager.getFunctionObject("ISWorldObjectContextMenu.onUnbarricadeMetal"), door, playerObj.getPlayerNum());
            }
            if (barricade != null && barricade.isMetalBar() && ISWorldObjectContextMenuLogic.checkBlowTorchForBarricade(playerObj)) {
                if (test) {
                    return null;
                }
                doorContextWrapper.addGetUpOption(Translator.getText("ContextMenu_Unbarricade"), worldobjects, LuaManager.getFunctionObject("ISWorldObjectContextMenu.onUnbarricadeMetalBar"), door, playerObj.getPlayerNum());
            }
        }
        return doorContextWrapper;
    }

    private static ISContextMenuWrapper doWindowMenu(KahluaTable fetch, KahluaTable worldObjects, boolean test, ISContextMenuWrapper contextWrapper, IsoWindow window, boolean invincibleWindow, IsoPlayer playerObj, boolean hasRemoveBarricadeTool, boolean hasHammer) {
        IsoBrokenGlass brokenGlass;
        ItemContainer playerInv = playerObj.getInventory();
        String windowOptionTxt = Translator.getText("Window");
        KahluaTable windowOption = contextWrapper.addOption(windowOptionTxt, null, null, new Object[0]);
        ISContextMenuWrapper windowContextWrapper = ISContextMenuWrapper.getNew(contextWrapper);
        contextWrapper.addSubMenu(windowOption, windowContextWrapper.getTable());
        IsoCurtain curtain = (IsoCurtain)fetch.rawget("curtain");
        KahluaTable windowContext = contextWrapper.getContextFromOption(windowOptionTxt);
        if (window != null && !invincibleWindow) {
            Object[] sheetRopeCandidates;
            KahluaTable opencloseoption;
            Object[] tooltip;
            IsoBarricade barricade = window.getBarricadeForCharacter(playerObj);
            Texture texture = Texture.trygetTexture(window.getSpriteName());
            if (texture instanceof Texture) {
                Texture texture2 = texture;
                windowOption.rawset("iconTexture", (Object)texture2.splitIcon());
            } else {
                windowOption.rawset("iconTexture", (Object)Texture.trygetTexture("Build_WindowBlack"));
            }
            IsoCurtain curtain2 = window.HasCurtains();
            IsoCurtain isoCurtain = curtain = curtain != null ? curtain : curtain2;
            if (window.isSmashed() && !window.isGlassRemoved() && barricade == null) {
                if (test) {
                    return null;
                }
                KahluaTable option = windowContextWrapper.addGetUpOption(Translator.getText("ContextMenu_RemoveBrokenGlass"), worldObjects, LuaManager.getFunctionObject("ISWorldObjectContextMenu.onRemoveBrokenGlass"), window, playerObj.getPlayerNum());
                if (playerObj.getPrimaryHandItem() == null) {
                    option.rawset("notAvailable", (Object)true);
                    tooltip = ISWorldObjectContextMenuLogic.addToolTip();
                    tooltip.getTable().rawset("description", (Object)Translator.getText("Tooltip_RemoveBrokenGlassNoItem"));
                    option.rawset("toolTip", (Object)tooltip.getTable());
                }
            }
            if (curtain2 == null && playerInv.containsTypeRecurse("Sheet")) {
                if (test) {
                    return null;
                }
                KahluaTable addSheetOption = windowContextWrapper.addGetUpOption(Translator.getText("ContextMenu_Add_sheet"), worldObjects, LuaManager.getFunctionObject("ISWorldObjectContextMenu.onAddSheet"), window, playerObj.getPlayerNum());
                addSheetOption.rawset("iconTexture", (Object)Texture.getSharedTexture("Item_Sheet"));
            }
            if (window.canClimbThrough(playerObj)) {
                if (test) {
                    return null;
                }
                KahluaTable climboption = windowContextWrapper.addGetUpOption(Translator.getText("ContextMenu_Climb_through"), worldObjects, LuaManager.getFunctionObject("ISWorldObjectContextMenu.onClimbThroughWindow"), window, playerObj.getPlayerNum());
                if (LuaHelpers.getJoypadState(playerObj.getPlayerNum()) == null) {
                    tooltip = ISWorldObjectContextMenuLogic.addToolTip();
                    tooltip.setName(Translator.getText("ContextMenu_Info"));
                    if (window.isGlassRemoved()) {
                        tooltip.getTable().rawset("description", (Object)Translator.getText("Tooltip_TapKey", Input.getKeyName(Core.getInstance().getKey("Interact"))));
                    } else {
                        tooltip.getTable().rawset("description", (Object)Translator.getText("Tooltip_Climb", Input.getKeyName(Core.getInstance().getKey("Interact"))));
                    }
                    climboption.rawset("toolTip", (Object)tooltip.getTable());
                }
            }
            if (window.IsOpen() && !window.isSmashed() && barricade == null) {
                if (test) {
                    return null;
                }
                opencloseoption = windowContextWrapper.addGetUpOption(Translator.getText("ContextMenu_Close_window"), worldObjects, LuaManager.getFunctionObject("ISWorldObjectContextMenu.onOpenCloseWindow"), window, playerObj.getPlayerNum());
                if (LuaHelpers.getJoypadState(playerObj.getPlayerNum()) == null) {
                    tooltip = ISWorldObjectContextMenuLogic.addToolTip();
                    tooltip.setName(Translator.getText("ContextMenu_Info"));
                    tooltip.getTable().rawset("description", (Object)Translator.getText("Tooltip_OpenClose", Input.getKeyName(Core.getInstance().getKey("Interact"))));
                    opencloseoption.rawset("toolTip", (Object)tooltip.getTable());
                }
            }
            if (!window.IsOpen() && !window.isSmashed() && barricade == null) {
                if (test) {
                    return null;
                }
                if (window.getSprite() == null || !window.getSprite().getProperties().has("WindowLocked")) {
                    opencloseoption = windowContextWrapper.addGetUpOption(Translator.getText("ContextMenu_Open_window"), worldObjects, LuaManager.getFunctionObject("ISWorldObjectContextMenu.onOpenCloseWindow"), window, playerObj.getPlayerNum());
                    if (LuaHelpers.getJoypadState(playerObj.getPlayerNum()) == null) {
                        tooltip = ISWorldObjectContextMenuLogic.addToolTip();
                        tooltip.setName(Translator.getText("ContextMenu_Info"));
                        tooltip.getTable().rawset("description", (Object)Translator.getText("Tooltip_OpenClose", Input.getKeyName(Core.getInstance().getKey("Interact"))));
                        opencloseoption.rawset("toolTip", (Object)tooltip.getTable());
                    }
                }
                windowContextWrapper.addGetUpOption(Translator.getText("ContextMenu_Smash_window"), worldObjects, LuaManager.getFunctionObject("ISWorldObjectContextMenu.onSmashWindow"), window, playerObj.getPlayerNum());
            }
            for (Object object : sheetRopeCandidates = new Object[]{fetch.rawget("window"), fetch.rawget("hoppableN"), fetch.rawget("hoppableN_2"), fetch.rawget("hoppableW"), fetch.rawget("hoppableW_2"), fetch.rawget("thumpableWindowN"), fetch.rawget("thumpableWindowN_2"), fetch.rawget("thumpableWindowW"), fetch.rawget("thumpableWindowW_2")}) {
                if (object == null) continue;
                LuaHelpers.callLuaClass("ISWorldObjectContextMenu", "doSheetRopeOptions", null, windowContext, object, worldObjects, playerObj.getPlayerNum(), playerObj, playerInv, hasHammer, test);
            }
        }
        IsoWindowFrame windowFrame = (IsoWindowFrame)fetch.rawget("windowFrame");
        if (curtain == null && windowFrame != null) {
            curtain = windowFrame.HasCurtains();
        }
        IsoThumpable thumpableWindow = (IsoThumpable)fetch.rawget("thumpableWindow");
        if (curtain == null && thumpableWindow != null) {
            curtain = thumpableWindow.HasCurtains();
        }
        if (curtain != null && !invincibleWindow) {
            String text = Translator.getText("ContextMenu_Open_curtains");
            if (curtain.IsOpen()) {
                text = Translator.getText("ContextMenu_Close_curtains");
            }
            if (test) {
                return null;
            }
            Texture curtainTexture = Texture.trygetTexture(curtain.getSpriteName());
            if (curtainTexture == null) {
                curtainTexture = Texture.trygetTexture("Item_Sheet");
            }
            if (window == null) {
                windowOption.rawset("name", (Object)Translator.getText("Curtain"));
                windowOption.rawset("iconTexture", (Object)curtainTexture.splitIcon());
            }
            if (!curtain.getSquare().getProperties().has(IsoFlagType.exterior)) {
                if (!playerObj.getCurrentSquare().has(IsoFlagType.exterior)) {
                    KahluaTable option = windowContextWrapper.addGetUpOption(text, worldObjects, LuaManager.getFunctionObject("ISWorldObjectContextMenu.onOpenCloseCurtain"), curtain, playerObj.getPlayerNum());
                    option.rawset("iconTexture", (Object)curtainTexture.splitIcon());
                    if (LuaHelpers.getJoypadState(playerObj.getPlayerNum()) == null) {
                        ISToolTipWrapper tooltip = ISWorldObjectContextMenuLogic.addToolTip();
                        tooltip.setName(Translator.getText("ContextMenu_Info"));
                        tooltip.getTable().rawset("description", (Object)Translator.getText("Tooltip_OpenCloseCurtains", Input.getKeyName(Core.getInstance().getKey("Interact"))));
                        option.rawset("toolTip", (Object)tooltip.getTable());
                    }
                    LuaHelpers.callLuaClass("ISWorldObjectContextMenu", "addRemoveCurtainOption", null, windowContext, worldObjects, curtain, playerObj.getPlayerNum());
                }
            } else {
                windowContextWrapper.addGetUpOption(text, worldObjects, LuaManager.getFunctionObject("ISWorldObjectContextMenu.onOpenCloseCurtain"), curtain, playerObj.getPlayerNum());
                LuaHelpers.callLuaClass("ISWorldObjectContextMenu", "addRemoveCurtainOption", null, windowContext, worldObjects, curtain, playerObj.getPlayerNum());
            }
        }
        if (window != null && !invincibleWindow) {
            IsoBarricade barricade = window.getBarricadeForCharacter(playerObj);
            ISWorldObjectContextMenuLogic.doBarricadeMenu(window, playerObj, playerInv, windowContextWrapper);
            if (barricade != null && barricade.getNumPlanks() > 0 & hasRemoveBarricadeTool) {
                if (test) {
                    return null;
                }
                windowContextWrapper.addGetUpOption(Translator.getText("ContextMenu_Unbarricade"), worldObjects, LuaManager.getFunctionObject("ISWorldObjectContextMenu.onUnbarricade"), window, playerObj.getPlayerNum());
            }
            if (barricade != null && barricade.isMetal() && ISWorldObjectContextMenuLogic.checkBlowTorchForBarricade(playerObj)) {
                if (test) {
                    return null;
                }
                windowContextWrapper.addGetUpOption(Translator.getText("ContextMenu_Unbarricade"), worldObjects, LuaManager.getFunctionObject("ISWorldObjectContextMenu.onUnbarricadeMetal"), window, playerObj.getPlayerNum());
            }
            if (barricade != null && barricade.isMetalBar() && ISWorldObjectContextMenuLogic.checkBlowTorchForBarricade(playerObj)) {
                if (test) {
                    return null;
                }
                windowContextWrapper.addGetUpOption(Translator.getText("ContextMenu_Unbarricade"), worldObjects, LuaManager.getFunctionObject("ISWorldObjectContextMenu.onUnbarricadeMetalBar"), window, playerObj.getPlayerNum());
            }
        }
        if (windowFrame != null && window == null && thumpableWindow == null) {
            if (windowFrame.getCurtain() == null && playerInv.containsTypeRecurse("Sheet")) {
                if (test) {
                    return null;
                }
                windowContextWrapper.addGetUpOption(Translator.getText("ContextMenu_Add_sheet"), worldObjects, LuaManager.getFunctionObject("ISWorldObjectContextMenu.onAddSheet"), windowFrame, playerObj.getPlayerNum());
            }
            int numSheetRope = windowFrame.countAddSheetRope();
            if (windowFrame.canAddSheetRope() && !windowFrame.isBarricaded() && playerObj.getCurrentSquare().getZ() > 0 && playerInv.containsTypeRecurse("Nails")) {
                if (playerInv.getItemCountRecurse("SheetRope") >= numSheetRope) {
                    if (test) {
                        return null;
                    }
                    windowContextWrapper.addGetUpOption(Translator.getText("ContextMenu_Add_escape_rope_sheet"), worldObjects, LuaManager.getFunctionObject("ISWorldObjectContextMenu.onAddSheetRope"), windowFrame, playerObj.getPlayerNum(), true);
                } else if (playerInv.getItemCountRecurse("Rope") >= numSheetRope) {
                    if (test) {
                        return null;
                    }
                    windowContextWrapper.addGetUpOption(Translator.getText("ContextMenu_Add_escape_rope"), worldObjects, LuaManager.getFunctionObject("ISWorldObjectContextMenu.onAddSheetRope"), windowFrame, playerObj.getPlayerNum(), false);
                }
            }
            if (windowFrame.haveSheetRope()) {
                if (test) {
                    return null;
                }
                windowContextWrapper.addGetUpOption(Translator.getText("ContextMenu_Remove_escape_rope"), worldObjects, LuaManager.getFunctionObject("ISWorldObjectContextMenu.onRemoveSheetRope"), windowFrame, playerObj.getPlayerNum());
            }
            if (test) {
                return null;
            }
            if (windowFrame.canClimbThrough(playerObj) && !windowFrame.hasWindow()) {
                KahluaTable climboption = windowContextWrapper.addGetUpOption(Translator.getText("ContextMenu_Climb_through"), worldObjects, LuaManager.getFunctionObject("ISWorldObjectContextMenu.onClimbThroughWindow"), windowFrame, playerObj.getPlayerNum());
                if (LuaHelpers.getJoypadState(playerObj.getPlayerNum()) == null) {
                    ISToolTipWrapper tooltip = ISWorldObjectContextMenuLogic.addToolTip();
                    tooltip.setName(Translator.getText("ContextMenu_Info"));
                    tooltip.getTable().rawset("description", (Object)Translator.getText("Tooltip_TapKey", Input.getKeyName(Core.getInstance().getKey("Interact"))));
                    climboption.rawset("toolTip", (Object)tooltip.getTable());
                }
            }
            if (windowFrame.isBarricadeAllowed()) {
                IsoBarricade barricade = windowFrame.getBarricadeForCharacter(playerObj);
                if (barricade != null && barricade.getNumPlanks() > 0 && hasRemoveBarricadeTool) {
                    if (test) {
                        return null;
                    }
                    windowContextWrapper.addGetUpOption(Translator.getText("ContextMenu_Unbarricade"), worldObjects, LuaManager.getFunctionObject("ISWorldObjectContextMenu.onUnbarricade"), windowFrame, playerObj.getPlayerNum());
                }
                if (!windowFrame.haveSheetRope() && barricade == null && ISWorldObjectContextMenuLogic.checkBlowTorchForBarricade(playerObj) && playerInv.containsTypeRecurse("SheetMetal") && test) {
                    return null;
                }
                if (!windowFrame.haveSheetRope() && barricade == null && ISWorldObjectContextMenuLogic.checkBlowTorchForBarricade(playerObj) && playerInv.getItemCountRecurse("Base.MetalBar") >= 3 && test) {
                    return null;
                }
                if (barricade != null && barricade.isMetal() && ISWorldObjectContextMenuLogic.checkBlowTorchForBarricade(playerObj)) {
                    if (test) {
                        return null;
                    }
                    windowContextWrapper.addGetUpOption(Translator.getText("ContextMenu_Unbarricade"), worldObjects, LuaManager.getFunctionObject("ISWorldObjectContextMenu.onUnbarricadeMetal"), windowFrame, playerObj.getPlayerNum());
                }
                if (barricade != null && barricade.isMetalBar() && ISWorldObjectContextMenuLogic.checkBlowTorchForBarricade(playerObj)) {
                    if (test) {
                        return null;
                    }
                    windowContextWrapper.addGetUpOption(Translator.getText("ContextMenu_Unbarricade"), worldObjects, LuaManager.getFunctionObject("ISWorldObjectContextMenu.onUnbarricadeMetalBar"), windowFrame, playerObj.getPlayerNum());
                }
            }
        }
        if ((brokenGlass = (IsoBrokenGlass)fetch.rawget("brokenGlass")) != null) {
            windowContextWrapper.addGetUpOption(Translator.getText("ContextMenu_PickupBrokenGlass"), worldObjects, LuaManager.getFunctionObject("ISWorldObjectContextMenu.onPickupBrokenGlass"), brokenGlass, playerObj.getPlayerNum());
        }
        return windowContextWrapper;
    }

    private static void doBarricadeMenu(BarricadeAble barricadeAble, IsoPlayer playerObj, ItemContainer playerInv, ISContextMenuWrapper contextWrapper) {
        boolean canBarricade = barricadeAble.isBarricadeAllowed();
        boolean hasHammer = playerInv.getFirstRecurse(item -> item.hasTag(ItemTag.HAMMER) && !item.isBroken()) != null;
        IsoBarricade barricade = barricadeAble.getBarricadeForCharacter(playerObj);
        int weldingRods = playerInv.getItemCountRecurse(ItemKey.Drainable.WELDING_RODS);
        int metalWelding = playerObj.getPerkLevel(PerkFactory.Perks.MetalWelding);
        if (playerObj.isBuildCheat() || barricadeAble.isBarricadeAllowed() && (barricade == null || barricade.canAddPlank()) && hasHammer && playerInv.containsTypeRecurse(ItemKey.Weapon.PLANK) && playerInv.getItemCountRecurse(ItemKey.Normal.NAILS) >= 2) {
            ISWorldObjectContextMenuLogic.doBarricadeOption(playerObj, contextWrapper, "carpentry_01_8", "Item_Plank", "ContextMenu_Barricade");
        }
        if (playerObj.isBuildCheat() || canBarricade && barricade == null && ISWorldObjectContextMenuLogic.checkBlowTorchForBarricade(playerObj) && playerInv.containsTypeRecurse(ItemKey.Normal.SHEET_METAL) && weldingRods >= 1 && metalWelding >= 3) {
            ISWorldObjectContextMenuLogic.doBarricadeOption(playerObj, contextWrapper, "constructedobjects_01_24", "Item_SheetMetal", "ContextMenu_MetalBarricade");
        }
        if (playerObj.isBuildCheat() || canBarricade && barricade == null && ISWorldObjectContextMenuLogic.checkBlowTorchForBarricade(playerObj) && (playerInv.getItemCountRecurse(ItemKey.Weapon.METAL_BAR) >= 3 || playerInv.getItemCountRecurse(ItemKey.Weapon.IRON_BAR) >= 3) && weldingRods >= 3 && metalWelding >= 3) {
            ISWorldObjectContextMenuLogic.doBarricadeOption(playerObj, contextWrapper, "constructedobjects_01_55", "Item_SteelRod_Full", "ContextMenu_MetalBarBarricade");
        }
    }

    private static void doBarricadeOption(IsoPlayer playerObj, ISContextMenuWrapper contextWrapper, String tile, String itemIcon, String optionName) {
        KahluaTable option = contextWrapper.addGetUpOption(Translator.getText(optionName), tile, LuaManager.getFunctionObject("ISWorldObjectContextMenu.onBarricade"), playerObj);
        option.rawset("iconTexture", (Object)Texture.trygetTexture(itemIcon));
        if (playerObj.tooDarkToRead()) {
            option.rawset("notAvailable", (Object)true);
            ISToolTipWrapper toolTip = ISWorldObjectContextMenuLogic.addToolTip();
            option.rawset("toolTip", (Object)toolTip.getTable());
            toolTip.getTable().rawset("description", (Object)Translator.getText("IGUI_CraftingWindow_RequiresLight"));
        }
    }

    private static void doGardeningSubmenu(ISContextMenuWrapper contextWrapper, KahluaTable worldObjects, IsoPlayer playerObj, KahluaTable fetch, boolean test) {
        IsoObject pickupItem;
        IsoObject stump;
        IsoGridSquare wallVine;
        IsoGridSquare canBeCut;
        IsoCompost compost;
        IsoGridSquare canBeRemoved;
        KahluaTable gardeningOption = contextWrapper.addOption(Translator.getText("ContextMenu_Gardening"), worldObjects, null, new Object[0]);
        ISContextMenuWrapper gardeningSubMenu = ISContextMenuWrapper.getNew(contextWrapper);
        contextWrapper.addSubMenu(gardeningOption, gardeningSubMenu.getTable());
        ItemContainer playerInv = playerObj.getInventory();
        int playerNum = playerObj.getPlayerNum();
        InventoryItem scythe = playerInv.getFirstRecurse(item -> !item.isBroken() && item.hasTag(ItemTag.SCYTHE));
        if (scythe != null && playerObj.getVehicle() == null) {
            KahluaTable option = gardeningSubMenu.addGetUpOption(Translator.getText("ContextMenu_ScytheGrass"), playerObj, LuaManager.getFunctionObject("ISWorldObjectContextMenu.onScytheGrass"), scythe);
            option.rawset("iconTexture", (Object)scythe.getTex());
        }
        if ((canBeRemoved = (IsoGridSquare)fetch.rawget("canBeRemoved")) != null && playerObj.getVehicle() == null) {
            gardeningSubMenu.addGetUpOption(Translator.getText("ContextMenu_RemoveGrass"), worldObjects, LuaManager.getFunctionObject("ISWorldObjectContextMenu.onRemoveGrass"), canBeRemoved, playerNum);
        }
        if ((compost = (IsoCompost)fetch.rawget("compost")) != null) {
            LuaHelpers.callLuaClass("ISWorldObjectContextMenu", "handleCompost", null, test, gardeningSubMenu.getTable(), worldObjects, playerObj, playerInv);
        }
        if ((canBeCut = (IsoGridSquare)fetch.rawget("canBeCut")) != null && playerObj.getVehicle() == null) {
            gardeningSubMenu.addGetUpOption(Translator.getText("ContextMenu_RemoveBush"), worldObjects, LuaManager.getFunctionObject("ISWorldObjectContextMenu.onRemovePlant"), canBeCut, false, playerNum);
        }
        if ((wallVine = (IsoGridSquare)fetch.rawget("wallVine")) != null && playerObj.getVehicle() == null) {
            gardeningSubMenu.addGetUpOption(Translator.getText("ContextMenu_RemoveWallVine"), worldObjects, LuaManager.getFunctionObject("ISWorldObjectContextMenu.onRemovePlant"), wallVine, true, playerNum);
        }
        if ((stump = (IsoObject)fetch.rawget("stump")) != null && playerObj.getVehicle() == null) {
            KahluaTable option = gardeningSubMenu.addGetUpOption(Translator.getText("ContextMenu_Remove_Stump"), worldObjects, LuaManager.getFunctionObject("ISWorldObjectContextMenu.onRemoveGroundCoverItemPickAxe"), playerNum, stump);
            option.rawset("iconTexture", (Object)Texture.trygetTexture(stump.getSpriteName()).splitIcon());
        }
        if ((pickupItem = (IsoObject)fetch.rawget("pickupItem")) != null && playerObj.getVehicle() == null) {
            ISWorldObjectContextMenuLogic.prePickupGroundCoverItem(gardeningSubMenu, worldObjects, playerNum, pickupItem);
        }
        LuaHelpers.callLuaClass("ISFarmingMenu", "doDigMenu", null, playerObj, gardeningSubMenu.getTable(), worldObjects, test);
        if (gardeningSubMenu.getNumOptions() <= 1.0) {
            contextWrapper.removeLastOption();
        }
    }

    private static boolean isGraveFilledIn(IsoObject grave) {
        if (grave == null || grave.getName() == null || !grave.getName().equals("EmptyGraves")) {
            return false;
        }
        if (LuaHelpers.castBoolean(grave.getModData().rawget("filled"))) {
            return true;
        }
        IsoSprite sprite = grave.getSprite();
        if (sprite == null || sprite.getName() == null) {
            return false;
        }
        return sprite.getName().equals("location_community_cemetary_01_40") || sprite.getName().equals("location_community_cemetary_01_41") || sprite.getName().equals("location_community_cemetary_01_42") || sprite.getName().equals("location_community_cemetary_01_43");
    }

    private static InventoryItem getFishingRod(IsoPlayer playerObj) {
        InventoryItem handItem = playerObj.getPrimaryHandItem();
        if (handItem != null && ISWorldObjectContextMenuLogic.predicateFishingRodOrSpear(handItem, playerObj)) {
            return handItem;
        }
        return playerObj.getInventory().getFirstRecurse(item -> ISWorldObjectContextMenuLogic.predicateFishingRodOrSpear(item, playerObj));
    }

    private static boolean predicateFishingRodOrSpear(InventoryItem item, IsoPlayer playerObj) {
        if (item.isBroken()) {
            return false;
        }
        if (!item.hasTag(ItemTag.FISHING_ROD) && !item.hasTag(ItemTag.FISHING_SPEAR)) {
            return false;
        }
        return ISWorldObjectContextMenuLogic.getFishingLure(playerObj, item) != null;
    }

    private static Object getFishingLure(IsoPlayer player, InventoryItem rod) {
        HandWeapon handWeapon;
        if (rod == null) {
            return null;
        }
        if (rod instanceof HandWeapon && WeaponType.getWeaponType(handWeapon = (HandWeapon)rod) == WeaponType.SPEAR) {
            return true;
        }
        if (player.getSecondaryHandItem() != null && player.getSecondaryHandItem().isFishingLure()) {
            return player.getSecondaryHandItem();
        }
        return player.getInventory().getFirstRecurse(InventoryItem::isFishingLure);
    }

    private static KahluaTable farmingSystem_getLuaObjectOnSquare(IsoGridSquare square) {
        GlobalObject globalObject;
        if (square == null) {
            return null;
        }
        CGlobalObjectSystem farmingSystem = ISWorldObjectContextMenuLogic.getFarmingSystem();
        if (farmingSystem != null && (globalObject = farmingSystem.getObjectAt(square.getX(), square.getY(), square.getZ())) != null) {
            KahluaTable luaObject = globalObject.getModData();
            LuaManager.call("CGlobalObject:updateFromIsoObject", luaObject);
            return luaObject;
        }
        return null;
    }

    private static CGlobalObjectSystem getFarmingSystem() {
        for (int i = 0; i < CGlobalObjects.getSystemCount(); ++i) {
            CGlobalObjectSystem system = CGlobalObjects.getSystemByIndex(i);
            if (system.getName() == null || !system.getName().equals("CFarmingSystem")) continue;
            return system;
        }
        return null;
    }

    private static KahluaTable getDirtGravelSand(IsoGridSquare square) {
        int index = square.getNextNonItemObjectIndex(0);
        while (index >= 0 && index < square.getObjects().size()) {
            IsoObject obj = square.getObjects().get(index);
            index = square.getNextNonItemObjectIndex(index + 1);
            if (obj.hasModData() && LuaHelpers.castBoolean(obj.getModData().rawget("shovelled")) || !GameServer.server && ISWorldObjectContextMenuLogic.farmingSystem_getLuaObjectOnSquare(square) != null || obj.getSprite() == null || obj.getSprite().getName() == null) continue;
            String spriteName = obj.getSprite().getName();
            if (spriteName.equals("floors_exterior_natural_01_13") || spriteName.equals("blends_street_01_55") || spriteName.equals("blends_street_01_54") || spriteName.equals("blends_street_01_53") || spriteName.equals("blends_street_01_48")) {
                KahluaTable table = LuaManager.platform.newTable();
                table.rawset("gravel", (Object)obj);
                return table;
            }
            if (spriteName.equals("blends_natural_01_0") || spriteName.equals("blends_natural_01_5") || spriteName.equals("blends_natural_01_6") || spriteName.equals("blends_natural_01_7") || spriteName.equals("floors_exterior_natural_01_24")) {
                KahluaTable table = LuaManager.platform.newTable();
                table.rawset("sand", (Object)obj);
                return table;
            }
            if (spriteName.equals("blends_natural_01_96") || spriteName.equals("blends_natural_01_103") || spriteName.equals("blends_natural_01_101") || spriteName.equals("blends_natural_01_102")) {
                KahluaTable table = LuaManager.platform.newTable();
                table.rawset("clay", (Object)obj);
                return table;
            }
            if (!spriteName.startsWith("blends_natural_01_") && !spriteName.startsWith("floors_exterior_natural")) continue;
            KahluaTable table = LuaManager.platform.newTable();
            table.rawset("dirt", (Object)obj);
            return table;
        }
        return null;
    }

    private static boolean canCleanBlood(IsoPlayer playerObj, IsoGridSquare square) {
        ItemContainer playerInv = playerObj.getInventory();
        InventoryItem bleach = playerInv.getFirstRecurse(ISWorldObjectContextMenuLogic::predicateCleaningLiquid);
        InventoryItem mop = playerInv.getFirstRecurse(item -> !item.isBroken() && item.hasTag(ItemTag.CLEAN_STAINS));
        return square != null && square.haveStains() && bleach != null && mop != null;
    }

    private static boolean predicateCleaningLiquid(InventoryItem item) {
        if (item == null) {
            return false;
        }
        return item.hasComponent(ComponentType.FluidContainer) && (item.getFluidContainer().contains(Fluid.Bleach) || item.getFluidContainer().contains(Fluid.CleaningLiquid)) && (double)item.getFluidContainer().getAmount() >= ZomboidGlobals.cleanBloodBleachAmount;
    }

    private static boolean canCleanGraffiti(IsoPlayer playerObj, IsoGridSquare square) {
        ItemContainer playerInv = playerObj.getInventory();
        InventoryItem cleaner = playerInv.getFirstRecurse(item -> {
            FluidContainer fc = item.getFluidContainer();
            return fc != null && fc.getAmount() >= 1.25f && fc.contains(Fluid.Petrol);
        });
        InventoryItem mop = playerInv.getFirstRecurse(item -> !item.isBroken() && item.hasTag(ItemTag.CLEAN_STAINS));
        return square != null && square.haveGraffiti() && cleaner != null && mop != null;
    }

    private static boolean predicateStoreFuel(InventoryItem item) {
        FluidContainer fluidContainer = item.getFluidContainer();
        if (fluidContainer == null) {
            return false;
        }
        if (fluidContainer.isEmpty()) {
            return true;
        }
        return fluidContainer.contains(Fluid.Petrol) && fluidContainer.getAmount() < fluidContainer.getCapacity() && !item.isBroken();
    }

    private static void fetchPickupItems(KahluaTable fetch, IsoObject v, PropertyContainer props, ItemContainer playerInv) {
        String customName;
        if (props == null) {
            return;
        }
        if (v.getContainer() != null) {
            fetch.rawset("container", (Object)v);
        }
        if ((customName = props.get("CustomName")) == null) {
            return;
        }
        if (!props.has("IsMoveAble") && fetch.rawget("pickupItem") == null) {
            if (ScriptManager.instance.getItem(customName) != null) {
                fetch.rawset("pickupItem", (Object)v);
            } else {
                KahluaTable groundCoverItems = (KahluaTable)LuaManager.env.rawget("GroundCoverItems");
                String itemName = (String)groundCoverItems.rawget(customName);
                if (itemName != null && ScriptManager.instance.getItem(itemName) != null) {
                    fetch.rawset("pickupItem", (Object)v);
                }
            }
        }
        if (playerInv.getFirstRecurse(item -> !item.isBroken() && item.hasTag(ItemTag.REMOVE_STUMP)) != null && v.isStump()) {
            fetch.rawset("stump", (Object)v);
        }
        if (v.getSquare().getOre() != null && playerInv.getFirstRecurse(item -> !item.isBroken() && (item.hasTag(ItemTag.PICK_AXE) || item.hasTag(ItemTag.STONE_MAUL) || item.hasTag(ItemTag.SLEDGEHAMMER) || item.hasTag(ItemTag.CLUB_HAMMER))) != null) {
            if (customName.contains("ironOre")) {
                fetch.rawset("ore", (Object)v);
            }
            if (customName.contains("copperOre")) {
                fetch.rawset("ore", (Object)v);
            }
            if (customName.equals("FlintBoulder")) {
                fetch.rawset("ore", (Object)v);
            }
            if (customName.equals("LimestoneBoulder")) {
                fetch.rawset("ore", (Object)v);
            }
        }
    }

    private static boolean compareType(String type, InventoryItem item) {
        if (type != null && type.indexOf(46) == -1) {
            return ISWorldObjectContextMenuLogic.compareType(type, item.getType());
        }
        return ISWorldObjectContextMenuLogic.compareType(type, item.getFullType()) || ISWorldObjectContextMenuLogic.compareType(type, item.getType());
    }

    private static boolean compareType(String type1, String type2) {
        if (type1 != null && type1.contains("/")) {
            int p = type1.indexOf(type2);
            if (p == -1) {
                return false;
            }
            char chBefore = p > 0 ? type1.charAt(p - 1) : (char)'\u0000';
            char chAfter = p + type2.length() < type1.length() ? type1.charAt(p + type2.length()) : (char)'\u0000';
            return chBefore == '\u0000' && chAfter == '/' || chBefore == '/' && chAfter == '\u0000' || chBefore == '/' && chAfter == '/';
        }
        return type1.equals(type2);
    }

    private static ISToolTipWrapper addToolTip() {
        ISToolTipWrapper wrappedToolTip;
        KahluaTable tooltip;
        KahluaTable worldObjectContextMenu = (KahluaTable)LuaManager.env.rawget("ISWorldObjectContextMenu");
        KahluaTable pool = (KahluaTable)worldObjectContextMenu.rawget("tooltipPool");
        KahluaTable tooltipsUsed = (KahluaTable)worldObjectContextMenu.rawget("tooltipsUsed");
        if (!pool.isEmpty()) {
            tooltip = (KahluaTable)pool.rawget(pool.size());
            wrappedToolTip = new ISToolTipWrapper(tooltip);
            pool.rawset(pool.size(), (Object)null);
        } else {
            wrappedToolTip = new ISToolTipWrapper();
            tooltip = wrappedToolTip.getTable();
        }
        tooltipsUsed.rawset(tooltipsUsed.size() + 1, (Object)tooltip);
        wrappedToolTip.reset();
        return wrappedToolTip;
    }

    private static boolean isForceDropHeavyItem(InventoryItem item) {
        return item != null && (item.getType().equals("Generator") || item.getType().equals("CorpseMale") || item.getType().equals("CorpseFemale") || item.hasTag(ItemTag.HEAVY_ITEM) || item.getType().equals("Animal") || item.getType().equals("CorpseAnimal"));
    }

    private static void getSquaresInRadius(float worldX, float worldY, float worldZ, float radius, KahluaTable doneSquares, KahluaTable squares) {
        int minX = Double.valueOf(Math.floor(worldX - radius)).intValue();
        int maxX = Double.valueOf(Math.ceil(worldX + radius)).intValue();
        int minY = Double.valueOf(Math.floor(worldY - radius)).intValue();
        int maxY = Double.valueOf(Math.ceil(worldY + radius)).intValue();
        for (int y = minY; y <= maxY; ++y) {
            for (int x = minX; x <= maxX; ++x) {
                IsoGridSquare square = IsoWorld.instance.getCell().getGridSquare((double)x, (double)y, worldZ);
                if (square == null || LuaHelpers.tableContainsKey(doneSquares, square)) continue;
                doneSquares.rawset(square, (Object)true);
                squares.rawset(squares.size() + 1, (Object)square);
            }
        }
    }

    private static void getWorldObjectsInRadius(int playerNum, int screenX, int screenY, KahluaTable squares, float radius, KahluaTable worldObjects) {
        radius = 48.0f / Core.getInstance().getZoom(playerNum);
        KahluaTableIterator iterator2 = squares.iterator();
        while (iterator2.advance()) {
            IsoGridSquare square = (IsoGridSquare)iterator2.getValue();
            if (square == null) continue;
            ArrayList<IsoWorldInventoryObject> squareObjects = square.getWorldObjects();
            for (int i = 0; i < squareObjects.size(); ++i) {
                IsoWorldInventoryObject worldObject = squareObjects.get(i);
                float dist = IsoUtils.DistanceToSquared(screenX, screenY, worldObject.getScreenPosX(playerNum), worldObject.getScreenPosY(playerNum));
                if (!(dist <= radius * radius)) continue;
                worldObjects.rawset(worldObjects.size() + 1, (Object)worldObject);
            }
        }
    }

    private static boolean handleInteraction(int x, int y, boolean test, ISContextMenuWrapper context, KahluaTable worldobjects, IsoPlayer playerObj, ItemContainer playerInv) {
        if (Core.getInstance().getGameMode().equals("LastStand")) {
            return false;
        }
        if (test) {
            return true;
        }
        int playerNum = playerObj.getPlayerNum();
        KahluaTable squares = LuaManager.platform.newTable();
        KahluaTable doneSquare = LuaManager.platform.newTable();
        KahluaTableIterator iterator2 = worldobjects.iterator();
        while (iterator2.advance()) {
            IsoObject value = (IsoObject)iterator2.getValue();
            if (value == null || LuaHelpers.tableContainsKey(doneSquare, value.getSquare())) continue;
            doneSquare.rawset(value.getSquare(), (Object)true);
            squares.rawset(squares.size() + 1, (Object)value.getSquare());
        }
        if (squares.isEmpty()) {
            return false;
        }
        KahluaTable worldObjects = LuaManager.platform.newTable();
        if (LuaHelpers.getJoypadState(playerNum) != null) {
            iterator2 = squares.iterator();
            while (iterator2.advance()) {
                IsoGridSquare square = (IsoGridSquare)iterator2.getValue();
                if (square == null) continue;
                for (int i = 0; i < square.getWorldObjects().size(); ++i) {
                    IsoWorldInventoryObject worldObject = square.getWorldObjects().get(i);
                    worldObjects.rawset(worldObjects.size() + 1, (Object)worldObject);
                }
            }
        } else {
            KahluaTable squares2 = LuaManager.platform.newTable();
            iterator2 = squares.iterator();
            while (iterator2.advance()) {
                if (iterator2.getKey() == null) continue;
                squares2.rawset(iterator2.getKey(), iterator2.getValue());
            }
            float radius = 1.0f;
            iterator2 = squares2.iterator();
            while (iterator2.advance()) {
                IsoGridSquare square = (IsoGridSquare)iterator2.getValue();
                if (square == null) continue;
                float worldX = LuaManager.GlobalObject.screenToIsoX(playerNum, x, y, square.getZ());
                float worldY = LuaManager.GlobalObject.screenToIsoY(playerNum, x, y, square.getZ());
                ISWorldObjectContextMenuLogic.getSquaresInRadius(worldX, worldY, square.getZ(), 1.0f, doneSquare, squares);
            }
            ISWorldObjectContextMenuLogic.getWorldObjectsInRadius(playerNum, x, y, squares, 1.0f, worldObjects);
        }
        if (worldObjects.isEmpty()) {
            return false;
        }
        KahluaTable itemList = LuaManager.platform.newTable();
        iterator2 = worldObjects.iterator();
        while (iterator2.advance()) {
            KahluaTable table;
            IsoObject worldObject = (IsoObject)iterator2.getValue();
            if (worldObject == null) continue;
            String itemName = worldObject.getName();
            if (itemName == null && worldObject instanceof IItemProvider) {
                IItemProvider iItemProvider = (IItemProvider)((Object)worldObject);
                itemName = iItemProvider.getItem().getName();
            }
            if (itemName == null) {
                itemName = "???";
            }
            if ((table = (KahluaTable)itemList.rawget(itemName)) == null) {
                table = LuaManager.platform.newTable();
                itemList.rawset(itemName, (Object)table);
            }
            table.rawset(table.size() + 1, (Object)worldObject);
        }
        iterator2 = itemList.iterator();
        while (iterator2.advance()) {
            InventoryItem inventoryItem;
            KahluaTable items = (KahluaTable)iterator2.getValue();
            if (items == null) continue;
            IsoObject object = (IsoObject)items.rawget(1);
            if (object instanceof IItemProvider) {
                IItemProvider iItemProvider = (IItemProvider)((Object)object);
                inventoryItem = iItemProvider.getItem();
            } else {
                inventoryItem = null;
            }
            InventoryItem item = inventoryItem;
            IsoGridSquare square = object.getSquare();
            if (!(item instanceof Radio)) continue;
            IsoObject obj = null;
            for (int i = 0; i < square.getObjects().size(); ++i) {
                IsoObject tObj = square.getObjects().get(i);
                if (!(tObj instanceof IsoRadio) || LuaHelpers.castDouble(tObj.getModData().rawget("RadioItemID")) != (double)item.getID()) continue;
                obj = tObj;
                break;
            }
            if (obj == null) continue;
            KahluaTable option = context.addGetUpOption(Translator.getText("IGUI_DeviceOptions"), playerObj, LuaManager.getFunctionObject("ISWorldObjectContextMenu.activateRadio"), obj);
            option.rawset("itemForTexture", (Object)item);
        }
        return false;
    }

    private static boolean handleGrabWorldItem(KahluaTable fetch, int x, int y, boolean test, ISContextMenuWrapper context, KahluaTable worldobjects, IsoPlayer playerObj, ItemContainer playerInv) {
        IsoDeadBody body;
        boolean bodyItem;
        if (playerObj.getVehicle() != null) {
            return false;
        }
        if (Core.getInstance().getGameMode().equals("LastStand")) {
            return false;
        }
        if (test) {
            return true;
        }
        double player = playerObj.getPlayerNum();
        if (playerObj.isGrappling()) {
            KahluaTable dropCorpseOption = context.addOption(Translator.getText("ContextMenu_Drop_Corpse"), playerObj, LuaManager.getFunctionObject("ISWorldObjectContextMenu.handleGrabWorldItem_onDropCorpse"), new Object[0]);
            ISToolTipWrapper toolTip = ISWorldObjectContextMenuLogic.addToolTip();
            toolTip.getTable().rawset("description", (Object)Translator.getText("Tooltip_GrappleCorpse"));
            dropCorpseOption.rawset("toolTip", (Object)toolTip.getTable());
        }
        KahluaTable squares = LuaManager.platform.newTable();
        KahluaTable doneSquare = LuaManager.platform.newTable();
        KahluaTableIterator iterator2 = worldobjects.iterator();
        while (iterator2.advance()) {
            IsoObject object = (IsoObject)iterator2.getValue();
            if (object == null || object.getSquare() == null || LuaHelpers.tableContainsKey(doneSquare, object.getSquare())) continue;
            doneSquare.rawset(object.getSquare(), (Object)true);
            squares.rawset(squares.size() + 1, (Object)object.getSquare());
        }
        if (squares.isEmpty()) {
            return false;
        }
        KahluaTable worldObjects = LuaManager.platform.newTable();
        if (LuaHelpers.getJoypadState(player) != null) {
            iterator2 = squares.iterator();
            while (iterator2.advance()) {
                IsoGridSquare square = (IsoGridSquare)iterator2.getValue();
                if (square == null) continue;
                for (int i = 0; i < square.getWorldObjects().size(); ++i) {
                    IsoWorldInventoryObject worldObject = square.getWorldObjects().get(i);
                    worldObjects.rawset(worldObjects.size() + 1, (Object)worldObject);
                }
            }
        } else {
            KahluaTable squares2 = LuaManager.platform.newTable();
            iterator2 = squares.iterator();
            while (iterator2.advance()) {
                if (iterator2.getKey() == null) continue;
                squares2.rawset(iterator2.getKey(), iterator2.getValue());
            }
            float radius = 1.0f;
            iterator2 = squares2.iterator();
            while (iterator2.advance()) {
                IsoGridSquare square = (IsoGridSquare)iterator2.getValue();
                if (square == null) continue;
                float worldX = LuaManager.GlobalObject.screenToIsoX((int)player, x, y, square.getZ());
                float worldY = LuaManager.GlobalObject.screenToIsoY((int)player, x, y, square.getZ());
                ISWorldObjectContextMenuLogic.getSquaresInRadius(worldX, worldY, square.getZ(), 1.0f, doneSquare, squares);
            }
            ISWorldObjectContextMenuLogic.getWorldObjectsInRadius((int)player, x, y, squares, 1.0f, worldObjects);
        }
        boolean bl = bodyItem = (body = (IsoDeadBody)fetch.rawget("body")) != null && !body.isAnimal() && playerObj.getVehicle() == null && playerInv.getItemCount("Base.CorpseMale") == 0;
        if (worldObjects.isEmpty() && !bodyItem) {
            return false;
        }
        KahluaTable itemList = LuaManager.platform.newTable();
        iterator2 = worldObjects.iterator();
        while (iterator2.advance()) {
            KahluaTable table;
            IsoObject worldObject = (IsoObject)iterator2.getValue();
            if (worldObject == null) continue;
            String itemName = worldObject.getName();
            if (itemName == null && worldObject instanceof IItemProvider) {
                IItemProvider iItemProvider = (IItemProvider)((Object)worldObject);
                itemName = iItemProvider.getItem().getName();
            }
            if (itemName == null) {
                itemName = "???";
            }
            if (worldObject instanceof IsoWorldInventoryObject) {
                itemName = ((IItemProvider)((Object)worldObject)).getItem().getName();
            }
            if ((table = (KahluaTable)itemList.rawget(itemName)) == null) {
                table = LuaManager.platform.newTable();
                itemList.rawset(itemName, (Object)table);
            }
            table.rawset(table.size() + 1, (Object)worldObject);
        }
        KahluaTable expendedPlacementItems = LuaManager.platform.newTable();
        boolean addedExtended = false;
        KahluaTable grabOption = context.addOption(Translator.getText("ContextMenu_Grab"), worldobjects, null, new Object[0]);
        ISContextMenuWrapper subMenuGrab = ISContextMenuWrapper.getNew(context);
        context.addSubMenu(grabOption, subMenuGrab.getTable());
        iterator2 = itemList.iterator();
        while (iterator2.advance()) {
            KahluaTable itemsTable;
            KahluaTable itemOption;
            String name = (String)iterator2.getKey();
            KahluaTable items = (KahluaTable)iterator2.getValue();
            if (name == null || items == null) continue;
            IsoObject itemOne = (IsoObject)items.rawget(1);
            if (itemOne != null && itemOne.getSquare() != null && itemOne.getSquare().isWallTo(playerObj.getSquare())) {
                context.removeLastOption();
                break;
            }
            IItemProvider itemOneItemProvider = (IItemProvider)((Object)itemOne);
            if (itemOneItemProvider != null && itemOneItemProvider.getItem() != null && (itemOneItemProvider.getItem().getWorldStaticItem() != null || itemOneItemProvider.getItem().getClothingItem() != null || itemOneItemProvider.getItem().getWorldStaticItem() != null || itemOneItemProvider.getItem() instanceof HandWeapon)) {
                expendedPlacementItems.rawset(name, (Object)items);
                addedExtended = true;
            }
            if (items.size() > 1) {
                name = String.format(Locale.ENGLISH, "%s (%d)", name, items.size());
            }
            if (items.size() > 2) {
                itemOption = subMenuGrab.addOption(name, worldobjects, null, new Object[0]);
                itemsTable = LuaManager.platform.newTable();
                itemsTable.rawset(1, (Object)items);
                itemOption.rawset("onHighlightParams", (Object)itemsTable);
                itemOption.rawset("onHighlight", LuaManager.getFunctionObject("ISWorldObjectContextMenu.handleGrabWorldItem_onHighlightMultiple"));
                ISContextMenuWrapper subMenuItem = ISContextMenuWrapper.getNew(subMenuGrab);
                subMenuGrab.addSubMenu(itemOption, subMenuItem.getTable());
                subMenuItem.addOption(Translator.getText("ContextMenu_Grab_one"), worldobjects, LuaManager.getFunctionObject("ISWorldObjectContextMenu.onGrabWItem"), itemOne, player);
                subMenuItem.addOption(Translator.getText("ContextMenu_Grab_half"), worldobjects, LuaManager.getFunctionObject("ISWorldObjectContextMenu.onGrabHalfWItems"), items, player);
                subMenuItem.addOption(Translator.getText("ContextMenu_Grab_all"), worldobjects, LuaManager.getFunctionObject("ISWorldObjectContextMenu.onGrabAllWItems"), items, player);
                continue;
            }
            if (items.size() > 1 && itemOneItemProvider.getItem().getActualWeight() >= 3.0f) {
                itemOption = subMenuGrab.addOption(name, worldobjects, null, new Object[0]);
                ISContextMenuWrapper subMenuItem = ISContextMenuWrapper.getNew(subMenuGrab);
                subMenuGrab.addSubMenu(itemOption, subMenuItem.getTable());
                subMenuItem.addOption(Translator.getText("ContextMenu_Grab_one"), worldobjects, LuaManager.getFunctionObject("ISWorldObjectContextMenu.onGrabWItem"), itemOne, player);
                subMenuItem.addOption(Translator.getText("ContextMenu_Grab_all"), worldobjects, LuaManager.getFunctionObject("ISWorldObjectContextMenu.onGrabAllWItems"), items, player);
                continue;
            }
            KahluaTable option = subMenuGrab.addOption(name, worldobjects, LuaManager.getFunctionObject("ISWorldObjectContextMenu.onGrabAllWItems"), items, player);
            option.rawset("itemForTexture", (Object)itemOneItemProvider.getItem());
            itemsTable = LuaManager.platform.newTable();
            itemsTable.rawset(1, (Object)itemOneItemProvider);
            option.rawset("onHighlightParams", (Object)itemsTable);
            option.rawset("onHighlight", LuaManager.getFunctionObject("ISWorldObjectContextMenu.handleGrabWorldItem_onHighlight"));
        }
        if (LuaHelpers.getJoypadState(player) != null) {
            addedExtended = false;
        }
        if (addedExtended) {
            KahluaTable extendedPlacementOption = context.addOption(Translator.getText("ContextMenu_ExtendedPlacement"), null, null, new Object[0]);
            ISContextMenuWrapper subMenuPlacement = ISContextMenuWrapper.getNew(context);
            context.addSubMenu(extendedPlacementOption, subMenuPlacement.getTable());
            KahluaTable namesSorted = LuaManager.platform.newTable();
            iterator2 = expendedPlacementItems.iterator();
            while (iterator2.advance()) {
                Object name = iterator2.getKey();
                if (name == null) continue;
                namesSorted.rawset(namesSorted.size() + 1, name);
            }
            Comparator<Map.Entry<Object, Object>> tableValueStringSorter = Comparator.comparing(a -> LuaHelpers.castString(a.getValue()));
            LuaHelpers.tableSort(namesSorted, tableValueStringSorter);
            iterator2 = namesSorted.iterator();
            while (iterator2.advance()) {
                String name = (String)iterator2.getValue();
                if (name == null) continue;
                ISContextMenuWrapper subMenu = subMenuPlacement;
                KahluaTable items = (KahluaTable)expendedPlacementItems.rawget(name);
                if (items == null) continue;
                IItemProvider itemOne = (IItemProvider)items.rawget(1);
                if (items.size() > 2 && namesSorted.size() > 1) {
                    KahluaTable subMenuOption = subMenuPlacement.addOption(name, worldobjects, null, new Object[0]);
                    subMenuOption.rawset("itemForTexture", (Object)itemOne.getItem());
                    KahluaTable itemsTable = LuaManager.platform.newTable();
                    itemsTable.rawset(1, (Object)items);
                    subMenuOption.rawset("onHighlightParams", (Object)itemsTable);
                    subMenuOption.rawset("onHighlight", LuaManager.getFunctionObject("ISWorldObjectContextMenu.handleGrabWorldItem_onHighlightMultiple"));
                    subMenu = ISContextMenuWrapper.getNew(subMenuPlacement);
                    subMenuPlacement.addSubMenu(subMenuOption, subMenu.getTable());
                }
                KahluaTableIterator iterator22 = items.iterator();
                while (iterator22.advance()) {
                    IItemProvider item = (IItemProvider)iterator22.getValue();
                    if (item == null) continue;
                    KahluaTable option = subMenu.addOption(name, item, LuaManager.getFunctionObject("ISWorldObjectContextMenu.onExtendedPlacement"), playerObj);
                    option.rawset("itemForTexture", (Object)item.getItem());
                    KahluaTable itemsTable = LuaManager.platform.newTable();
                    itemsTable.rawset(1, (Object)item);
                    option.rawset("onHighlightParams", (Object)itemsTable);
                    option.rawset("onHighlight", LuaManager.getFunctionObject("ISWorldObjectContextMenu.handleGrabWorldItem_onHighlight"));
                }
            }
        }
        return ISWorldObjectContextMenuLogic.handleGrabCorpseSubmenu(fetch, playerObj, worldobjects, subMenuGrab, test);
    }

    private static boolean handleGrabCorpseSubmenu(KahluaTable fetch, IsoPlayer playerObj, KahluaTable worldobjects, ISContextMenuWrapper subMenuGrab, boolean test) {
        IsoDeadBody body = (IsoDeadBody)fetch.rawget("body");
        if (body == null) {
            return false;
        }
        if (body.isAnimal()) {
            return false;
        }
        if (playerObj.getVehicle() != null) {
            return false;
        }
        ItemContainer playerInv = playerObj.getInventory();
        if (playerInv.getItemCount("Base.CorpseMale") > 0) {
            return false;
        }
        if (test) {
            return true;
        }
        double player = playerObj.getPlayerNum();
        IsoGridSquare square = body.getSquare();
        KahluaTable corpses = LuaManager.platform.newTable();
        ArrayList<IsoMovingObject> corpses2 = square.getStaticMovingObjects();
        for (int i = 0; i < corpses2.size(); ++i) {
            corpses.rawset(corpses.size() + 1, (Object)corpses2.get(i));
        }
        for (int d = 0; d < DIRECTIONS.length; ++d) {
            IsoGridSquare square2 = square.getAdjacentSquare(DIRECTIONS[d]);
            if (square2 == null) continue;
            corpses2 = square2.getStaticMovingObjects();
            for (int i = 0; i < corpses2.size(); ++i) {
                corpses.rawset(corpses.size() + 1, (Object)corpses2.get(i));
            }
        }
        if (corpses.size() > 1) {
            Comparator tableValueIsoMovingObjectDistanceSorter = (a, b) -> {
                float distA = ((IsoMovingObject)a.getValue()).DistToSquared(playerObj);
                float distB = ((IsoMovingObject)b.getValue()).DistToSquared(playerObj);
                return Float.compare(distA, distB);
            };
            LuaHelpers.tableSort(corpses, tableValueIsoMovingObjectDistanceSorter);
            KahluaTableIterator iterator2 = corpses.iterator();
            while (iterator2.advance()) {
                IsoDeadBody corpse = (IsoDeadBody)iterator2.getValue();
                ISWorldObjectContextMenuLogic.addGrabCorpseSubmenuOption(worldobjects, subMenuGrab, corpse, player);
            }
            return false;
        }
        ISWorldObjectContextMenuLogic.addGrabCorpseSubmenuOption(worldobjects, subMenuGrab, body, player);
        return false;
    }

    private static void addGrabCorpseSubmenuOption(KahluaTable worldobjects, ISContextMenuWrapper subMenuGrab, IsoDeadBody corpse, double player) {
        KahluaTable opt = subMenuGrab.addGetUpOption(Translator.getText("IGUI_ItemCat_Corpse"), worldobjects, LuaManager.getFunctionObject("ISWorldObjectContextMenu.onGrabCorpseItem"), corpse, player);
        KahluaTable containerButtonIcons = (KahluaTable)LuaManager.env.rawget("ContainerButtonIcons");
        if (containerButtonIcons != null) {
            opt.rawset("iconTexture", corpse.isFemale() ? containerButtonIcons.rawget("inventoryfemale") : containerButtonIcons.rawget("inventorymale"));
        }
        ISToolTipWrapper toolTip = ISWorldObjectContextMenuLogic.addToolTip();
        toolTip.getTable().rawset("description", (Object)Translator.getText("Tooltip_GrappleCorpse"));
        opt.rawset("toolTip", (Object)toolTip.getTable());
        if (corpse.getSquare().haveFire()) {
            opt.rawset("notAvailable", (Object)true);
            toolTip.getTable().rawset("description", (Object)Translator.getText("Tooltip_GrappleCorpseFire"));
        }
        LuaHelpers.callLuaClass("ISWorldObjectContextMenu", "initWorldItemHighlightOption", null, opt, corpse);
    }

    private static void prePickupGroundCoverItem(ISContextMenuWrapper context, KahluaTable worldobjects, double player, IsoObject pickupItem) {
        Item groundCoverItem;
        KahluaTable groundCoverItems;
        String groundCoverItemName;
        if (pickupItem.getSprite().getName().contains("vegetation_farming_") || pickupItem.getSprite().getName().contains("vegetation_gardening_")) {
            return;
        }
        if (!pickupItem.getSprite().getName().contains("d_generic") && !pickupItem.getSprite().getName().contains("crafting_ore")) {
            return;
        }
        String customName = pickupItem.getProperty("CustomName");
        String itemName = null;
        if (customName != null && (groundCoverItemName = (String)(groundCoverItems = (KahluaTable)LuaManager.env.rawget("GroundCoverItems")).rawget(customName)) != null && (groundCoverItem = ScriptManager.instance.getItem(groundCoverItemName)) != null) {
            itemName = Translator.getText(groundCoverItem.getDisplayName());
        }
        if (itemName == null) {
            return;
        }
        String text = String.format(Locale.ENGLISH, "%s %s", Translator.getText("ContextMenu_Remove"), itemName);
        KahluaTable option = context.addGetUpOption(text, worldobjects, LuaManager.getFunctionObject("ISWorldObjectContextMenu.onPickupGroundCoverItem"), player, pickupItem);
        option.rawset("iconTexture", (Object)Texture.trygetTexture(pickupItem.getSpriteName()).splitIcon());
    }

    private static boolean ISEmptyGraves_canDigHere(KahluaTable worldObjects) {
        KahluaTable squares = LuaManager.platform.newTable();
        KahluaTable didSquare = LuaManager.platform.newTable();
        KahluaTableIterator iterator2 = worldObjects.iterator();
        while (iterator2.advance()) {
            IsoObject worldObj = (IsoObject)iterator2.getValue();
            if (worldObj == null || LuaHelpers.castBoolean(didSquare.rawget(worldObj.getSquare()))) continue;
            squares.rawset(squares.size() + 1, (Object)worldObj.getSquare());
            didSquare.rawset(worldObj.getSquare(), (Object)true);
        }
        iterator2 = squares.iterator();
        while (iterator2.advance()) {
            IsoGridSquare square = (IsoGridSquare)iterator2.getValue();
            if (square == null) continue;
            if (square.getZ() > 0) {
                return false;
            }
            IsoObject floor = square.getFloor();
            if (floor == null || floor.getTextureName() == null || !floor.getTextureName().startsWith("floors_exterior_natural") && !floor.getTextureName().startsWith("blends_natural_01")) continue;
            return true;
        }
        return false;
    }

    private static boolean isGraveFullOfCorpses(IsoObject grave) {
        if (grave == null || grave.getName() == null || !grave.getName().equals("EmptyGraves")) {
            return false;
        }
        Double currentCorpses = LuaHelpers.castDouble(grave.getModData().rawget("corpses"));
        Double maxCorpses = LuaHelpers.castDouble(LuaHelpers.callLuaClass("ISEmptyGraves", "getMaxCorpses", null, grave));
        return currentCorpses >= maxCorpses;
    }

    private static String getMoveableDisplayName(IsoObject obj) {
        if (obj == null) {
            return null;
        }
        if (obj.getSprite() == null) {
            return null;
        }
        PropertyContainer props = obj.getSprite().getProperties();
        if (props.has("CustomName")) {
            String name = props.get("CustomName");
            if (props.has("GroupName")) {
                name = String.format(Locale.ENGLISH, "%s %s", props.get("GroupName"), name);
            }
            return Translator.getMoveableDisplayName(name);
        }
        return null;
    }

    private static void doFishNetOptions(ISContextMenuWrapper context, IsoPlayer playerObj, IsoGridSquare square) {
        KahluaTable fishNetOption = context.addOption(Translator.getText("ContextMenu_Place_Fishing_Net"), null, null, new Object[0]);
        fishNetOption.rawset("iconTexture", (Object)Texture.trygetTexture("Item_FishTrap").splitIcon());
        boolean isNotAvailable = false;
        if (square.DistToProper(playerObj.getCurrentSquare()) >= 5.0f) {
            isNotAvailable = true;
        }
        ISContextMenuWrapper subMenuFishNet = ISContextMenuWrapper.getNew(context);
        context.addSubMenu(fishNetOption, subMenuFishNet.getTable());
        ArrayList<InventoryItem> nets = playerObj.getInventory().getAll(item -> ISWorldObjectContextMenuLogic.compareType("Base.FishingNet", item));
        for (int k = 0; k < nets.size(); ++k) {
            KahluaTable option = subMenuFishNet.addGetUpOption(nets.get(k).getDisplayName(), null, LuaManager.getFunctionObject("ISWorldObjectContextMenu.onFishingNet"), playerObj, nets.get(k));
            option.rawset("notAvailable", (Object)isNotAvailable);
        }
    }

    private static void doPlacedFishNetOptions(ISContextMenuWrapper context, IsoPlayer playerObj, IsoObject trapFish) {
        Object fishingNetTSObj = trapFish.getSquare().getModData().rawget("fishingNetTS");
        KahluaTable subOptionFishingNet = context.addOption(Translator.getText("ContextMenu_FishingNet"), null, null, new Object[0]);
        subOptionFishingNet.rawset("iconTexture", (Object)Texture.trygetTexture(trapFish.getTextureName()).splitIcon());
        ISContextMenuWrapper subMenuFishing = ISContextMenuWrapper.getNew(context);
        context.addSubMenu(subOptionFishingNet, subMenuFishing.getTable());
        context = subMenuFishing;
        if (fishingNetTSObj != null) {
            Double fishingNetTS = LuaHelpers.castDouble(fishingNetTSObj);
            double hourElapsed = Math.floor(((double)GameTime.instance.getCalender().getTimeInMillis() - fishingNetTS) / 60000.0 / 60.0);
            if (hourElapsed > 0.0) {
                KahluaTable kahluaTable = context.addGetUpOption(Translator.getText("ContextMenu_Check_Trap"), null, LuaManager.getFunctionObject("ISWorldObjectContextMenu.onCheckFishingNet"), playerObj, trapFish, hourElapsed);
            }
        }
        KahluaTable suboption = context.addGetUpOption(Translator.getText("ContextMenu_Remove_Trap"), null, LuaManager.getFunctionObject("ISWorldObjectContextMenu.onRemoveFishingNet"), playerObj, trapFish);
        if (trapFish.getSquare().getModData().rawget("fishingNetBait") == null) {
            boolean isNotAvailable = false;
            suboption = context.addOption(Translator.getText("ContextMenu_Add_Bait"), null, null, new Object[0]);
            ISContextMenuWrapper subMenu = ISContextMenuWrapper.getNew(context);
            context.addSubMenu(suboption, subMenu.getTable());
            ArrayList<InventoryItem> items = playerObj.getInventory().getAllRecurse(item -> item.getFullType().equals("Base.Chum"), new ArrayList<InventoryItem>());
            for (int i = 0; i < items.size(); ++i) {
                InventoryItem item2 = items.get(i);
                KahluaTable opt2 = subMenu.addGetUpOption(item2.getName(), null, LuaManager.getFunctionObject("ISWorldObjectContextMenu.onAddBaitToFishingNet"), playerObj, trapFish, item2);
                opt2.rawset("notAvailable", (Object)isNotAvailable);
            }
            if (items.isEmpty()) {
                suboption.rawset("notAvailable", (Object)true);
                ISToolTipWrapper toolTip = ISWorldObjectContextMenuLogic.addToolTip();
                suboption.rawset("toolTip", (Object)toolTip.getTable());
                toolTip.getTable().rawset("description", (Object)Translator.getText("Tooltip_NeedChumForAddToFishingNet"));
            }
        }
    }

    private static void doChumOptions(ISContextMenuWrapper context, IsoPlayer playerObj, IsoGridSquare square) {
        KahluaTable option = context.addOption(Translator.getText("ContextMenu_AddChum"), null, null, new Object[0]);
        ISContextMenuWrapper submenu = ISContextMenuWrapper.getNew(context);
        context.addSubMenu(option, submenu.getTable());
        ArrayList<InventoryItem> chumItems = playerObj.getInventory().getAllTypeRecurse("Base.Chum");
        for (int i = 0; i < chumItems.size(); ++i) {
            InventoryItem item = chumItems.get(i);
            submenu.addGetUpOption(item.getDisplayName(), playerObj, LuaManager.getFunctionObject("ISWorldObjectContextMenu.onAddBaitToWater"), item, square);
        }
    }

    private static void doCreateChumOptions(ISContextMenuWrapper context, IsoPlayer playerObj, IsoGridSquare square) {
        context.addGetUpOption(Translator.getText("ContextMenu_MakeChum"), playerObj, LuaManager.getFunctionObject("ISWorldObjectContextMenu.doCreateChumOptions_makeChum"), square);
    }

    private static boolean isSomethingTo(IsoObject item, IsoPlayer playerObj) {
        if (item == null || item.getSquare() == null) {
            return false;
        }
        IsoGridSquare playerSq = playerObj.getCurrentSquare();
        if (!AdjacentFreeTileFinder.isTileOrAdjacent(playerSq, item.getSquare())) {
            playerSq = AdjacentFreeTileFinder.Find(item.getSquare(), playerObj, null);
        }
        return playerSq != null && item.getSquare().isSomethingTo(playerSq);
    }

    private static void doSleepOption(ISContextMenuWrapper context, IsoObject bed, double player, IsoPlayer playerObj) {
        String bedTypeXln;
        boolean isZombies;
        boolean sleepNeeded;
        boolean isOnBed;
        if (playerObj.getVehicle() != null) {
            return;
        }
        String text = bed != null ? Translator.getText("ContextMenu_Sleep") : Translator.getText("ContextMenu_SleepOnGround");
        String bedType = ISWorldObjectContextMenuLogic.getBedQuality(playerObj, bed);
        if (bedType.equals("floorPillow")) {
            text = Translator.getText("ContextMenu_SleepOnGroundPillow");
        }
        if (bed != null && bed.getSquare().getRoom() != playerObj.getSquare().getRoom()) {
            return;
        }
        boolean bl = isOnBed = playerObj.getSitOnFurnitureObject() == bed;
        if (bed != null && !isOnBed) {
            ArrayList<IsoObject> objects = new ArrayList<IsoObject>();
            bed.getSpriteGridObjectsIncludingSelf(objects);
            isOnBed = objects.contains(playerObj.getSitOnFurnitureObject());
        }
        KahluaTable sleepOption = isOnBed ? context.addOption(text, bed, LuaManager.getFunctionObject("ISWorldObjectContextMenu.onSleep"), player) : context.addGetUpOption(text, bed, LuaManager.getFunctionObject("ISWorldObjectContextMenu.onSleep"), player);
        String tooltipText = null;
        boolean bl2 = sleepNeeded = !GameClient.client || ServerOptions.getInstance().sleepNeeded.getValue();
        if (sleepNeeded && playerObj.getStats().get(CharacterStat.FATIGUE) <= 0.3f) {
            sleepOption.rawset("notAvailable", (Object)true);
            tooltipText = Translator.getText("IGUI_Sleep_NotTiredEnough");
        }
        boolean bl3 = isZombies = playerObj.getStats().getNumVisibleZombies() > 0 || playerObj.getStats().getNumChasingZombies() > 0 || playerObj.getStats().getNumVeryCloseZombies() > 0;
        if (sleepNeeded && isZombies) {
            sleepOption.rawset("notAvailable", (Object)true);
            tooltipText = Translator.getText("IGUI_Sleep_NotSafe");
        }
        if (sleepNeeded && playerObj.getHoursSurvived() - (double)playerObj.getLastHourSleeped() <= 1.0) {
            sleepOption.rawset("notAvailable", (Object)true);
            tooltipText = Translator.getText("ContextMenu_NoSleepTooEarly");
        } else if (playerObj.getSleepingTabletEffect() < 2000.0f) {
            if (playerObj.getMoodles().getMoodleLevel(MoodleType.PAIN) >= 2 && playerObj.getStats().get(CharacterStat.FATIGUE) <= 0.85f) {
                sleepOption.rawset("notAvailable", (Object)true);
                tooltipText = Translator.getText("ContextMenu_PainNoSleep");
            } else if (playerObj.getMoodles().getMoodleLevel(MoodleType.PANIC) >= 1) {
                sleepOption.rawset("notAvailable", (Object)true);
                tooltipText = Translator.getText("ContextMenu_PanicNoSleep");
            }
        }
        if (bed != null && (bedTypeXln = Translator.getTextOrNull(String.format(Locale.ENGLISH, "%s%s", "Tooltip_BedType_", bedType))) != null) {
            tooltipText = tooltipText != null ? String.format(Locale.ENGLISH, "%s <BR> %s", tooltipText, Translator.getText("Tooltip_BedType", bedTypeXln)) : Translator.getText("Tooltip_BedType", bedTypeXln);
        }
        if (tooltipText != null) {
            ISToolTipWrapper sleepTooltip = ISWorldObjectContextMenuLogic.addToolTip();
            sleepTooltip.setName(Translator.getText("ContextMenu_Sleeping"));
            sleepTooltip.getTable().rawset("description", (Object)tooltipText);
            sleepOption.rawset("toolTip", (Object)sleepTooltip.getTable());
        }
    }

    private static String getBedQuality(IsoPlayer playerObj, IsoObject bed) {
        boolean playerHasPillow;
        boolean bl = playerHasPillow = playerObj.getPrimaryHandItem() != null && playerObj.getPrimaryHandItem().hasTag(ItemTag.PILLOW) || playerObj.getSecondaryHandItem() != null && playerObj.getSecondaryHandItem().hasTag(ItemTag.PILLOW);
        if (playerObj.getVehicle() != null) {
            String bedType = "badBed";
            if (playerHasPillow) {
                return "badBedPillow";
            }
            BaseVehicle vehicle = playerObj.getVehicle();
            VehiclePart seat = vehicle.getPartForSeatContainer(vehicle.getSeat(playerObj));
            ItemContainer cont = seat.getItemContainer();
            if (cont.containsTag(ItemTag.PILLOW)) {
                return "badBedPillow";
            }
            return bedType;
        }
        if (bed == null) {
            String bedType = "floor";
            if (playerHasPillow) {
                return "floorPillow";
            }
            IsoGridSquare square = playerObj.getSquare();
            ArrayList<IsoWorldInventoryObject> worldObjects = square.getWorldObjects();
            for (int i = 0; i < worldObjects.size(); ++i) {
                InventoryItem item = worldObjects.get(i).getItem();
                if (item == null || !item.hasTag(ItemTag.PILLOW)) continue;
                return "floorPillow";
            }
            return bedType;
        }
        String bedType = bed.getProperty("BedType");
        if (bedType == null) {
            bedType = "averageBed";
        }
        if (bed.propertyEquals("CustomName", "Tent") || bed.propertyEquals("CustomName", "Shelter")) {
            if (bed.getContainer() != null) {
                ItemContainer cont = bed.getContainer();
                if (cont.containsTag(ItemTag.TENT_BED)) {
                    bedType = "averageBed";
                }
                if (cont.containsTag(ItemTag.PILLOW)) {
                    return String.format(Locale.ENGLISH, "%s%s", bedType, "Pillow");
                }
            }
            if (playerHasPillow) {
                return String.format(Locale.ENGLISH, "%s%s", bedType, "Pillow");
            }
            return bedType;
        }
        if (playerHasPillow) {
            return String.format(Locale.ENGLISH, "%s%s", bedType, "Pillow");
        }
        if (bed.getSquare() != null) {
            ArrayList<IsoObject> objects = new ArrayList<IsoObject>();
            bed.getSpriteGridObjectsIncludingSelf(objects);
            for (int n = 1; n < objects.size(); ++n) {
                IsoGridSquare square = objects.get(n - 1).getSquare();
                ArrayList<IsoWorldInventoryObject> worldObjects = square.getWorldObjects();
                for (int i = 0; i < worldObjects.size(); ++i) {
                    InventoryItem item = worldObjects.get(i).getItem();
                    if (item == null || !item.hasTag(ItemTag.PILLOW)) continue;
                    return String.format(Locale.ENGLISH, "%s%s", bedType, "Pillow");
                }
            }
        }
        return bedType;
    }

    private static ISContextMenuWrapper doFluidContainerMenu(ISContextMenuWrapper context, IsoObject object, double player, IsoPlayer playerObj, KahluaTable worldObjects) {
        if (object instanceof IsoWorldInventoryObject) {
            return null;
        }
        String containerName = ISWorldObjectContextMenuLogic.getMoveableDisplayName(object);
        if (containerName == null) {
            containerName = object.getFluidUiName();
        }
        KahluaTable option = context.addOption(containerName, null, null, new Object[0]);
        Texture texture = Texture.trygetTexture(object.getSpriteName());
        if (texture instanceof Texture) {
            Texture texture2 = texture;
            option.rawset("iconTexture", (Object)texture2.splitIcon());
        }
        if (object instanceof IsoWorldInventoryObject) {
            IsoWorldInventoryObject worldInventoryObject = (IsoWorldInventoryObject)object;
            option.rawset("itemForTexture", (Object)worldInventoryObject.getItem());
            KahluaTable itemsTable = LuaManager.platform.newTable();
            itemsTable.rawset(1, (Object)worldInventoryObject);
            option.rawset("onHighlightParams", (Object)itemsTable);
            option.rawset("onHighlight", LuaManager.getFunctionObject("ISWorldObjectContextMenu.handleGrabWorldItem_onHighlight"));
        }
        ISContextMenuWrapper mainSubMenu = ISContextMenuWrapper.getNew(context);
        context.addSubMenu(option, mainSubMenu.getTable());
        boolean isTrough = false;
        if (object instanceof IsoFeedingTrough) {
            context.getTable().rawset("troughSubmenu", (Object)mainSubMenu.getTable());
            context.getTable().rawset("dontShowLiquidOption", (Object)true);
            isTrough = true;
        }
        FluidContainer fluidContainer = object.getFluidContainer();
        if (!isTrough) {
            mainSubMenu.addOption(Translator.getText("Fluid_Show_Info"), player, LuaManager.getFunctionObject("ISWorldObjectContextMenu.onFluidInfo"), fluidContainer);
        }
        if (fluidContainer != null && fluidContainer.canPlayerEmpty()) {
            KahluaTable addFluidOption;
            mainSubMenu.addOption(Translator.getText("Fluid_Transfer_Fluids"), player, LuaManager.getFunctionObject("ISWorldObjectContextMenu.onFluidTransfer"), fluidContainer);
            if (Core.debug && (addFluidOption = mainSubMenu.addDebugOption(Translator.getText("ContextMenu_AddFluid"), player, null, null, null)) != null) {
                ISContextMenuWrapper addFluidSubmenu = ISContextMenuWrapper.getNew(mainSubMenu);
                mainSubMenu.addSubMenu(addFluidOption, addFluidSubmenu.getTable());
                for (FluidType fluid : FluidType.values()) {
                    addFluidSubmenu.addOption(fluid.toString(), fluidContainer, LuaManager.getFunctionObject("ISInventoryPaneContextMenu.addFluidDebug"), new Object[]{fluid});
                }
            }
            if (object.hasFluid()) {
                ISWorldObjectContextMenuLogic.doDrinkWaterMenu(object, player, playerObj, worldObjects, mainSubMenu);
                ISWorldObjectContextMenuLogic.doFillFluidMenu(object, player, playerObj, worldObjects, mainSubMenu);
            }
            if (object.hasWater()) {
                ISWorldObjectContextMenuLogic.doWashClothingMenu(object, player, playerObj, mainSubMenu);
            }
            if (object.hasFluid() && object.getFluidCapacity() < 9999.0f) {
                mainSubMenu.addOption(Translator.getText("Fluid_Empty"), player, LuaManager.getFunctionObject("ISWorldObjectContextMenu.onFluidEmpty"), fluidContainer);
            }
        }
        return mainSubMenu;
    }

    private static void addFluidFromItem(KahluaTable fetch, boolean test, ISContextMenuWrapper context, IsoObject pourFluidInto, KahluaTable worldobjects, IsoPlayer playerObj, ItemContainer playerInv) {
        if (pourFluidInto == null) {
            return;
        }
        if (pourFluidInto.isFluidInputLocked()) {
            return;
        }
        KahluaTable pourOut = LuaManager.platform.newTable();
        for (int i = 1; i < playerInv.getItems().size(); ++i) {
            InventoryItem item = playerInv.getItems().get(i);
            if (!item.canStoreWater() || !pourFluidInto.canTransferFluidFrom(item.getFluidContainer()) || !item.getFluidContainer().canPlayerEmpty()) continue;
            pourOut.rawset(pourOut.size() + 1, (Object)item);
        }
        if (!pourOut.isEmpty() && !test && pourFluidInto.getFluidAmount() < pourFluidInto.getFluidCapacity()) {
            KahluaTable subMenuOption = context.addOption(Translator.getText("ContextMenu_AddFluidFromItem"), worldobjects, null, new Object[0]);
            ISContextMenuWrapper subMenu = ISContextMenuWrapper.getNew(context);
            context.addSubMenu(subMenuOption, subMenu.getTable());
            KahluaTableIterator iterator2 = pourOut.iterator();
            while (iterator2.advance()) {
                InventoryItem item = (InventoryItem)iterator2.getValue();
                KahluaTable subOption = subMenu.addOption(item.getName(), worldobjects, LuaManager.getFunctionObject("ISWorldObjectContextMenu.onAddFluidFromItem"), pourFluidInto, item, playerObj);
                subOption.rawset("itemForTexture", (Object)item);
                if (!item.IsDrainable()) continue;
                DrainableComboItem drainableItem = (DrainableComboItem)item;
                ISToolTipWrapper tooltip = ISWorldObjectContextMenuLogic.addToolTip();
                UIFont font = (UIFont)((Object)tooltip.getTable().rawget("font"));
                int tx = TextManager.instance.MeasureStringX(font, String.format(Locale.ENGLISH, "%s:", Translator.getText("ContextMenu_WaterName"))) + 20;
                String descriptionText = String.format(Locale.ENGLISH, "%s: <SETX.%d> %d / %d", Translator.getText("ContextMenu_WaterName"), tx, item.getCurrentUses(), Double.valueOf(Math.floor(1.0f / drainableItem.getUseDelta() + 1.0E-4f)).intValue());
                tooltip.getTable().rawset("description", (Object)descriptionText);
                FluidContainer fc = drainableItem.getFluidContainer();
                if (fc != null && fc.isTainted() && SandboxOptions.getInstance().enableTaintedWaterText.getValue()) {
                    tooltip.getTable().rawset("description", (Object)String.format(Locale.ENGLISH, "%s <BR> <RGB.1,0.5,0.5> %s", descriptionText, Translator.getText("Tooltip_item_TaintedWater")));
                }
                subOption.rawset("toolTip", (Object)tooltip.getTable());
            }
        }
    }

    private static String formatWaterAmount(IsoObject object, int setX, float amount, float max) {
        if (max >= 9999.0f) {
            return String.format(Locale.ENGLISH, "%s: <SETX:%s> %s", object.getFluidUiName(), setX, Translator.getText("Tooltip_WaterUnlimited"));
        }
        return String.format(Locale.ENGLISH, "%s: <SETX:%s> %.2fL / %.2fL", object.getFluidUiName(), setX, Float.valueOf(amount), Float.valueOf(max));
    }

    private static void doDrinkWaterMenu(IsoObject object, double player, IsoPlayer playerObj, KahluaTable worldobjects, ISContextMenuWrapper context) {
        float thirst = playerObj.getStats().get(CharacterStat.THIRST);
        if (object.getSquare().getBuilding() != playerObj.getBuilding()) {
            return;
        }
        if (object instanceof IsoClothingDryer) {
            return;
        }
        if (object instanceof IsoClothingWasher) {
            return;
        }
        KahluaTable option = context.addGetUpOption(Translator.getText("ContextMenu_Drink"), worldobjects, LuaManager.getFunctionObject("ISWorldObjectContextMenu.onDrink"), object, player);
        object.getFluidAmount();
        ISToolTipWrapper tooltip = ISWorldObjectContextMenuLogic.addToolTip();
        UIFont font = (UIFont)((Object)tooltip.getTable().rawget("font"));
        int tx1 = TextManager.instance.MeasureStringX(font, String.format(Locale.ENGLISH, "%s:", Translator.getText("Tooltip_food_Thirst"))) + 20;
        int tx2 = TextManager.instance.MeasureStringX(font, String.format(Locale.ENGLISH, "%s:", object.getFluidUiName())) + 20;
        int tx = Math.max(tx1, tx2);
        float waterAmount = object.getFluidAmount();
        float waterMax = object.getFluidCapacity();
        String descriptionText = LuaHelpers.castString(tooltip.getTable().rawget("description"));
        descriptionText = String.format(Locale.ENGLISH, "%s%s", descriptionText, ISWorldObjectContextMenuLogic.formatWaterAmount(object, tx, waterAmount, waterMax));
        tooltip.getTable().rawset("description", (Object)descriptionText);
        if (object.isTaintedWater() && SandboxOptions.getInstance().enableTaintedWaterText.getValue()) {
            descriptionText = String.format(Locale.ENGLISH, "%s <BR> <RGB.1,0.5,0.5> %s", descriptionText, Translator.getText("Tooltip_item_TaintedWater"));
            tooltip.getTable().rawset("description", (Object)descriptionText);
        }
        option.rawset("toolTip", (Object)tooltip.getTable());
    }

    private static void doFillFluidMenu(IsoObject sink2, double playerNum, IsoPlayer playerObj, KahluaTable worldobjects, ISContextMenuWrapper context) {
        KahluaTable containerOption;
        KahluaTable tooltipTable;
        if (sink2.getSquare().getBuilding() != playerObj.getBuilding()) {
            return;
        }
        ItemContainer playerInv = playerObj.getInventory();
        KahluaTable allContainers = LuaManager.platform.newTable();
        KahluaTable allContainerTypes = LuaManager.platform.newTable();
        KahluaTable allContainersOfType = LuaManager.platform.newTable();
        ArrayList<InventoryItem> pourInto = playerInv.getAllRecurse(item -> item.getFluidContainer() != null && !item.getFluidContainer().isFull() && item.getFluidContainer().canAddFluid(Fluid.Water), new ArrayList<InventoryItem>());
        if (pourInto.isEmpty()) {
            return;
        }
        KahluaTable fillOption = context.addOption(Translator.getText("ContextMenu_Fill"), worldobjects, null, new Object[0]);
        if (sink2.getSquare() == null) {
            fillOption.rawset("notAvailable", (Object)true);
            return;
        }
        if (playerObj.hasFullInventory()) {
            ISToolTipWrapper tooltip = ISWorldObjectContextMenuLogic.addToolTip();
            fillOption.rawset("notAvailable", (Object)true);
            tooltip.getTable().rawset("description", (Object)Translator.getText("ContextMenu_FullInventory"));
            fillOption.rawset("toolTip", (Object)tooltip.getTable());
            return;
        }
        for (int i = 0; i < pourInto.size(); ++i) {
            InventoryItem container = pourInto.get(i);
            if (!sink2.canTransferFluidTo(container.getFluidContainer())) continue;
            allContainers.rawset(allContainers.size() + 1, (Object)container);
        }
        Comparator<Map.Entry<Object, Object>> tableValueStringSorter = Comparator.comparing(a -> ((InventoryItem)a.getValue()).getName());
        LuaHelpers.tableSort(allContainers, tableValueStringSorter);
        InventoryItem previousContainer = null;
        KahluaTableIterator iterator2 = allContainers.iterator();
        while (iterator2.advance()) {
            InventoryItem container = (InventoryItem)iterator2.getValue();
            if (container == null) continue;
            if (previousContainer != null && container.getName() != null && !container.getName().equals(previousContainer.getName())) {
                allContainerTypes.rawset(allContainerTypes.size() + 1, (Object)allContainersOfType);
                allContainersOfType = LuaManager.platform.newTable();
            }
            allContainersOfType.rawset(allContainersOfType.size() + 1, (Object)container);
            previousContainer = container;
        }
        allContainerTypes.rawset(allContainerTypes.size() + 1, (Object)allContainersOfType);
        ISContextMenuWrapper containerMenu = ISContextMenuWrapper.getNew(context);
        context.addSubMenu(fillOption, containerMenu.getTable());
        ISToolTipWrapper tooltip = ISWorldObjectContextMenuLogic.createWaterSourceTooltip(sink2);
        KahluaTable kahluaTable = tooltipTable = tooltip != null ? tooltip.getTable() : null;
        if (allContainers.size() > 1) {
            containerOption = containerMenu.addGetUpOption(Translator.getText("ContextMenu_FillAll"), worldobjects, LuaManager.getFunctionObject("ISWorldObjectContextMenu.onTakeWater"), sink2, allContainers, null, playerNum);
            containerOption.rawset("toolTip", (Object)tooltipTable);
        }
        iterator2 = allContainerTypes.iterator();
        while (iterator2.advance()) {
            Boolean success;
            Object object;
            Object[] result;
            KahluaTable containerType = (KahluaTable)iterator2.getValue();
            if (containerType == null || containerType.isEmpty()) continue;
            InventoryItem destItem = (InventoryItem)containerType.rawget(1);
            if (containerType.size() > 1) {
                String name = String.format(Locale.ENGLISH, "%s (%d)", destItem.getName(), containerType.size());
                containerOption = containerMenu.addOption(name, worldobjects, null, new Object[0]);
                containerOption.rawset("itemForTexture", (Object)destItem);
                ISContextMenuWrapper containerTypeMenu = ISContextMenuWrapper.getNew(containerMenu);
                containerMenu.addSubMenu(containerOption, containerTypeMenu.getTable());
                KahluaTable containerTypeOption = null;
                containerTypeOption = containerTypeMenu.addGetUpOption(Translator.getText("ContextMenu_FillOne"), worldobjects, LuaManager.getFunctionObject("ISWorldObjectContextMenu.onTakeWater"), sink2, LuaManager.platform.newTable(), destItem, playerNum);
                containerTypeOption.rawset("toolTip", (Object)tooltipTable);
                if (containerType.rawget(2) == null) continue;
                containerTypeOption = containerTypeMenu.addGetUpOption(Translator.getText("ContextMenu_FillAll"), worldobjects, LuaManager.getFunctionObject("ISWorldObjectContextMenu.onTakeWater"), sink2, containerType, null, playerNum);
                containerTypeOption.rawset("toolTip", (Object)tooltipTable);
                continue;
            }
            containerOption = containerMenu.addOption(destItem.getName(), worldobjects, LuaManager.getFunctionObject("ISWorldObjectContextMenu.onTakeWater"), sink2, null, destItem, playerNum);
            containerOption.rawset("itemForTexture", (Object)destItem);
            ISToolTipWrapper t = ISWorldObjectContextMenuLogic.createWaterSourceTooltip(sink2);
            if (t != null) {
                if (destItem instanceof DrainableComboItem) {
                    String newDescription = LuaHelpers.castString(t.getTable().rawget("description"));
                    newDescription = String.format(Locale.ENGLISH, "%s <LINE> %s: %d/10", newDescription, Translator.getText("ContextMenu_ItemWaterCapacity"), Double.valueOf(Math.floor(destItem.getCurrentUsesFloat() * 10.0f)).intValue());
                    t.getTable().rawset("description", (Object)newDescription);
                }
                containerOption.rawset("toolTip", (Object)t.getTable());
                continue;
            }
            Object functionObj = LuaManager.getFunctionObject("ISWorldObjectContextMenu.addToolTipInv");
            if (functionObj == null || (result = LuaManager.caller.pcall(LuaManager.thread, functionObj, (Object)destItem)) == null || result.length != 2 || !((object = result[0]) instanceof Boolean) || !(success = (Boolean)object).booleanValue() || !((object = result[1]) instanceof KahluaTable)) continue;
            KahluaTable tooltipInv = (KahluaTable)object;
            containerOption.rawset("toolTip", (Object)tooltipInv);
        }
    }

    private static ISToolTipWrapper createWaterSourceTooltip(IsoObject sink2) {
        ISToolTipWrapper tooltip = ISWorldObjectContextMenuLogic.addToolTip();
        String description = LuaHelpers.castString(tooltip.getTable().rawget("description"));
        if (sink2.isTaintedWater() && SandboxOptions.getInstance().enableTaintedWaterText.getValue()) {
            if (!description.isEmpty()) {
                description = String.format(Locale.ENGLISH, "%s\n<RGB:1,0.5,0.5> %s", description, Translator.getText("Tooltip_item_TaintedWater"));
                tooltip.getTable().rawset("description", (Object)description);
            } else {
                description = String.format(Locale.ENGLISH, "<RGB:1,0.5,0.5> %s", Translator.getText("Tooltip_item_TaintedWater"));
                tooltip.getTable().rawset("description", (Object)description);
            }
        }
        tooltip.getTable().rawset("maxLineWidth", (Object)512.0);
        if (description.isEmpty()) {
            return null;
        }
        return tooltip;
    }

    private static void setWashClothingTooltip(double soapRemaining, double waterRemaining, double soapRequired, double waterRequired, KahluaTable washList, KahluaTable option) {
        ISToolTipWrapper tooltip = ISWorldObjectContextMenuLogic.addToolTip();
        Object description = LuaHelpers.castString(tooltip.getTable().rawget("description"));
        description = soapRemaining < soapRequired ? (String)description + Translator.getText("IGUI_Washing_WithoutSoap") + " <LINE> " : String.format(Locale.ENGLISH, "%s%s: %.2f / %.0f <LINE> ", description, Translator.getText("IGUI_Washing_Soap"), Math.min(soapRemaining, soapRequired), soapRequired);
        description = String.format(Locale.ENGLISH, "%s%s: %.2f / %.0f", description, Translator.getText("ContextMenu_WaterName"), Math.min(waterRemaining, waterRequired), waterRequired);
        tooltip.getTable().rawset("description", description);
        option.rawset("toolTip", (Object)tooltip.getTable());
        if (waterRemaining < waterRequired) {
            option.rawset("notAvailable", (Object)true);
        }
    }

    private static void doWashClothingMenu(IsoObject sink2, double player, IsoPlayer playerObj, ISContextMenuWrapper context) {
        if (sink2.getSquare().getBuilding() != playerObj.getBuilding()) {
            return;
        }
        ItemContainer playerInv = playerObj.getInventory();
        boolean washEquipment = false;
        KahluaTable washList = LuaManager.platform.newTable();
        List<InventoryItem> soapList = playerObj.getInventory().getSoapList(null, true);
        boolean washYourself = LuaHelpers.castDouble(LuaHelpers.callLuaClass("ISWashYourself", "GetRequiredWater", null, playerObj)) > 0.0;
        KahluaTable washClothing = LuaManager.platform.newTable();
        ArrayList<InventoryItem> clothingInventory = playerInv.getItemsFromCategory("Clothing");
        for (int i = 0; i < clothingInventory.size(); ++i) {
            InventoryItem item = clothingInventory.get(i);
            if (item.isHidden() || !item.hasBlood() && !item.hasDirt() || item.hasTag(ItemTag.BREAK_WHEN_WET)) continue;
            if (!washEquipment) {
                washEquipment = true;
            }
            washList.rawset(washList.size() + 1, (Object)item);
            washClothing.rawset(washClothing.size() + 1, (Object)item);
        }
        KahluaTable washOther = LuaManager.platform.newTable();
        ArrayList<InventoryItem> dirtyRagInventory = playerInv.getAllTag(ItemTag.CAN_BE_WASHED, new ArrayList<InventoryItem>());
        for (int i = 0; i < dirtyRagInventory.size(); ++i) {
            InventoryItem item = dirtyRagInventory.get(i);
            if (item.getJobDelta() != 0.0f) continue;
            if (!washEquipment) {
                washEquipment = true;
            }
            washList.rawset(washList.size() + 1, (Object)item);
            washOther.rawset(washOther.size() + 1, (Object)item);
        }
        KahluaTable washWeapon = LuaManager.platform.newTable();
        ArrayList<InventoryItem> weaponInventory = playerInv.getItemsFromCategory("Weapon");
        for (int i = 0; i < weaponInventory.size(); ++i) {
            InventoryItem item = weaponInventory.get(i);
            if (!item.hasBlood()) continue;
            if (!washEquipment) {
                washEquipment = true;
            }
            washList.rawset(washList.size() + 1, (Object)item);
            washWeapon.rawset(washWeapon.size() + 1, (Object)item);
        }
        KahluaTable washContainer = LuaManager.platform.newTable();
        ArrayList<InventoryItem> containerInventory = playerInv.getItemsFromCategory("Container");
        for (int i = 0; i < containerInventory.size(); ++i) {
            InventoryItem item = containerInventory.get(i);
            if (item.isHidden() || !item.hasBlood() && !item.hasDirt()) continue;
            washEquipment = true;
            washList.rawset(washList.size() + 1, (Object)item);
            washContainer.rawset(washContainer.size() + 1, (Object)item);
        }
        Comparator compareClothingBlood = (a, b) -> {
            Double reqSoapA = LuaHelpers.castDouble(LuaHelpers.callLuaClass("ISWashClothing", "GetRequiredSoap", null, a.getValue()));
            Double reqSoapB = LuaHelpers.castDouble(LuaHelpers.callLuaClass("ISWashClothing", "GetRequiredSoap", null, b.getValue()));
            return reqSoapA.compareTo(reqSoapB);
        };
        LuaHelpers.tableSort(washList, compareClothingBlood);
        LuaHelpers.tableSort(washClothing, compareClothingBlood);
        LuaHelpers.tableSort(washOther, compareClothingBlood);
        LuaHelpers.tableSort(washWeapon, compareClothingBlood);
        LuaHelpers.tableSort(washContainer, compareClothingBlood);
        if (washYourself || washEquipment) {
            Object description;
            ISToolTipWrapper tooltip;
            double waterRequired;
            double soapRequired;
            KahluaTable mainOption = context.addOption(Translator.getText("ContextMenu_Wash"), null, null, new Object[0]);
            ISContextMenuWrapper mainSubMenu = ISContextMenuWrapper.getNew(context);
            context.addSubMenu(mainOption, mainSubMenu.getTable());
            double soapRemaining = 0.0;
            if (soapList != null && !soapList.isEmpty()) {
                soapRemaining = LuaHelpers.castDouble(LuaHelpers.callLuaClass("ISWashClothing", "GetSoapRemaining", null, soapList));
            }
            float waterRemaining = sink2.getFluidAmount();
            if (washYourself) {
                soapRemaining = LuaHelpers.castDouble(LuaHelpers.callLuaClass("ISWashYourself", "GetSoapRemaining", null, soapList));
                soapRequired = LuaHelpers.castDouble(LuaHelpers.callLuaClass("ISWashYourself", "GetRequiredSoap", null, playerObj));
                waterRequired = LuaHelpers.castDouble(LuaHelpers.callLuaClass("ISWashYourself", "GetRequiredWater", null, playerObj));
                List<InventoryItem> barSoapsList = playerObj.getInventory().getSoapList(null, false);
                KahluaTable option = mainSubMenu.addGetUpOption(Translator.getText("ContextMenu_Yourself"), playerObj, LuaManager.getFunctionObject("ISWorldObjectContextMenu.onWashYourself"), sink2, barSoapsList);
                tooltip = ISWorldObjectContextMenuLogic.addToolTip();
                description = LuaHelpers.castString(tooltip.getTable().rawget("description"));
                description = soapRemaining < soapRequired ? String.format(Locale.ENGLISH, "%s%s <LINE> ", description, Translator.getText("IGUI_Washing_WithoutSoap")) : String.format(Locale.ENGLISH, "%s%s: %.2f / %.2f <LINE> ", description, Translator.getText("IGUI_Washing_Soap"), Math.min(soapRemaining, soapRequired), soapRequired);
                description = String.format(Locale.ENGLISH, "%s%s: %.2f / %.2f", description, Translator.getText("ContextMenu_WaterName"), Math.min((double)waterRemaining, waterRequired), waterRequired);
                HumanVisual visual = playerObj.getHumanVisual();
                float bodyBlood = 0.0f;
                float bodyDirt = 0.0f;
                for (int i = 0; i < BloodBodyPartType.MAX.index(); ++i) {
                    BloodBodyPartType part = BloodBodyPartType.FromIndex(i);
                    bodyBlood += visual.getBlood(part);
                    bodyDirt += visual.getDirt(part);
                }
                if (bodyBlood > 0.0f) {
                    description = String.format(Locale.ENGLISH, "%s <LINE> %s: %.0f / 100", description, Translator.getText("Tooltip_clothing_bloody"), Math.ceil(bodyBlood / (float)BloodBodyPartType.MAX.index() * 100.0f));
                }
                if (bodyDirt > 0.0f) {
                    description = String.format(Locale.ENGLISH, "%s <LINE> %s: %.0f / 100", description, Translator.getText("Tooltip_clothing_dirty"), Math.ceil(bodyDirt / (float)BloodBodyPartType.MAX.index() * 100.0f));
                }
                tooltip.getTable().rawset("description", description);
                option.rawset("toolTip", (Object)tooltip.getTable());
                if (waterRemaining < 1.0f) {
                    option.rawset("notAvailable", (Object)true);
                }
            }
            if (washEquipment) {
                soapRemaining = LuaHelpers.castDouble(LuaHelpers.callLuaClass("ISWashClothing", "GetSoapRemaining", null, soapList));
                if (!washList.isEmpty()) {
                    KahluaTable option;
                    Object[] result;
                    if (!washClothing.isEmpty()) {
                        result = LuaHelpers.callLuaClassReturnMultiple("ISWorldObjectContextMenu", "calculateSoapAndWaterRequired", null, washClothing);
                        soapRequired = LuaHelpers.castDouble(result[0]);
                        waterRequired = LuaHelpers.castDouble(result[1]);
                        option = mainSubMenu.addGetUpOption(Translator.getText("ContextMenu_WashAllClothing"), playerObj, LuaManager.getFunctionObject("ISWorldObjectContextMenu.onWashClothing"), sink2, soapList, washClothing, null);
                        ISWorldObjectContextMenuLogic.setWashClothingTooltip(soapRemaining, waterRemaining, soapRequired, waterRequired, washClothing, option);
                    }
                    if (!washContainer.isEmpty()) {
                        result = LuaHelpers.callLuaClassReturnMultiple("ISWorldObjectContextMenu", "calculateSoapAndWaterRequired", null, washContainer);
                        soapRequired = LuaHelpers.castDouble(result[0]);
                        waterRequired = LuaHelpers.castDouble(result[1]);
                        option = mainSubMenu.addGetUpOption(Translator.getText("ContextMenu_WashAllContainer"), playerObj, LuaManager.getFunctionObject("ISWorldObjectContextMenu.onWashClothing"), sink2, soapList, washContainer, null);
                        ISWorldObjectContextMenuLogic.setWashClothingTooltip(soapRemaining, waterRemaining, soapRequired, waterRequired, washContainer, option);
                    }
                    if (!washWeapon.isEmpty()) {
                        result = LuaHelpers.callLuaClassReturnMultiple("ISWorldObjectContextMenu", "calculateSoapAndWaterRequired", null, washWeapon);
                        soapRequired = LuaHelpers.castDouble(result[0]);
                        waterRequired = LuaHelpers.castDouble(result[1]);
                        option = mainSubMenu.addGetUpOption(Translator.getText("ContextMenu_WashAllWeapon"), playerObj, LuaManager.getFunctionObject("ISWorldObjectContextMenu.onWashClothing"), sink2, soapList, washWeapon, null);
                        ISWorldObjectContextMenuLogic.setWashClothingTooltip(soapRemaining, waterRemaining, soapRequired, waterRequired, washWeapon, option);
                    }
                    if (!washOther.isEmpty()) {
                        result = LuaHelpers.callLuaClassReturnMultiple("ISWorldObjectContextMenu", "calculateSoapAndWaterRequired", null, washOther);
                        soapRequired = LuaHelpers.castDouble(result[0]);
                        waterRequired = LuaHelpers.castDouble(result[1]);
                        option = mainSubMenu.addGetUpOption(Translator.getText("ContextMenu_WashAllBandage"), playerObj, LuaManager.getFunctionObject("ISWorldObjectContextMenu.onWashClothing"), sink2, soapList, washOther, null);
                        ISWorldObjectContextMenuLogic.setWashClothingTooltip(soapRemaining, waterRemaining, soapRequired, waterRequired, washOther, option);
                    }
                }
                KahluaTableIterator iterator2 = washList.iterator();
                while (iterator2.advance()) {
                    InventoryItem item = (InventoryItem)iterator2.getValue();
                    if (item == null) continue;
                    double soapRequired2 = LuaHelpers.castDouble(LuaHelpers.callLuaClass("ISWashClothing", "GetRequiredSoap", null, item));
                    double waterRequired2 = LuaHelpers.castDouble(LuaHelpers.callLuaClass("ISWashClothing", "GetRequiredWater", null, item));
                    tooltip = ISWorldObjectContextMenuLogic.addToolTip();
                    description = LuaHelpers.castString(tooltip.getTable().rawget("description"));
                    description = soapRemaining < soapRequired2 ? (String)description + Translator.getText("IGUI_Washing_WithoutSoap") + " <LINE> " : String.format(Locale.ENGLISH, "%s%s: %.0f / %.0f <LINE> ", description, Translator.getText("IGUI_Washing_Soap"), Math.min(soapRemaining, soapRequired2), soapRequired2);
                    description = String.format(Locale.ENGLISH, "%s%s: %.2f / %.0f", description, Translator.getText("ContextMenu_WaterName"), Math.min((double)waterRemaining, waterRequired2), waterRequired2);
                    if ((item.IsClothing() || item.IsInventoryContainer() || item.IsWeapon()) && item.getBloodLevel() > 0.0f) {
                        description = String.format(Locale.ENGLISH, "%s <LINE> %s: %.0f / 100", description, Translator.getText("Tooltip_clothing_bloody"), Math.ceil(item.getBloodLevelAdjustedHigh()));
                    }
                    if (item.IsClothing() && ((Clothing)item).getDirtiness() > 0.0f) {
                        description = String.format(Locale.ENGLISH, "%s <LINE> %s: %.0f / 100", description, Translator.getText("Tooltip_clothing_dirty"), Math.ceil(((Clothing)item).getDirtiness()));
                    }
                    tooltip.getTable().rawset("description", description);
                    KahluaTable option = mainSubMenu.addGetUpOption(Translator.getText("ContextMenu_WashClothing", item.getDisplayName()), playerObj, LuaManager.getFunctionObject("ISWorldObjectContextMenu.onWashClothing"), sink2, soapList, null, item);
                    option.rawset("toolTip", (Object)tooltip.getTable());
                    option.rawset("itemForTexture", (Object)item);
                    if (!((double)waterRemaining < waterRequired2)) continue;
                    option.rawset("notAvailable", (Object)true);
                }
            }
        }
    }

    private static void CleanBandages_getAvailableItems(KahluaTable items, IsoPlayer playerObj, String recipeName, String itemType) {
        ItemContainer playerInv = playerObj.getInventory();
        int count = playerInv.getCountTypeRecurse(itemType);
        Recipe recipe = ScriptManager.instance.getRecipe(recipeName);
        if (recipe == null || count == 0) {
            return;
        }
        KahluaTable table = LuaManager.platform.newTable();
        table.rawset("itemType", (Object)itemType);
        table.rawset("count", (Object)count);
        table.rawset("recipe", (Object)recipe);
        items.rawset(items.size() + 1, (Object)table);
    }

    private static void doRecipeUsingWaterMenu(IsoObject waterObject, IsoPlayer playerObj, ISContextMenuWrapper context) {
        ItemContainer playerInv = playerObj.getInventory();
        float waterRemaining = waterObject.getFluidAmount();
        if (waterRemaining < 1.0f) {
            return;
        }
        KahluaTable items = LuaManager.platform.newTable();
        ISWorldObjectContextMenuLogic.CleanBandages_getAvailableItems(items, playerObj, "Base.Clean Bandage", "Base.BandageDirty");
        ISWorldObjectContextMenuLogic.CleanBandages_getAvailableItems(items, playerObj, "Base.Clean Denim Strips", "Base.DenimStripsDirty");
        ISWorldObjectContextMenuLogic.CleanBandages_getAvailableItems(items, playerObj, "Base.Clean Leather Strips", "Base.LeatherStripsDirty");
        ISWorldObjectContextMenuLogic.CleanBandages_getAvailableItems(items, playerObj, "Base.Clean Rag", "Base.RippedSheetsDirty");
        if (items.isEmpty()) {
            return;
        }
        LuaHelpers.callLuaClass("ISRecipeTooltip", "releaseAll", null, new Object[0]);
        if (items.size() == 1) {
            LuaHelpers.callLuaClass("CleanBandages", "setSubmenu", null, context, items.rawget(1), waterObject);
            return;
        }
        ISContextMenuWrapper subMenu = ISContextMenuWrapper.getNew(context);
        KahluaTable subOption = context.addOption(Translator.getText("ContextMenu_CleanBandageEtc"), null, null, new Object[0]);
        context.addSubMenu(subOption, subMenu.getTable());
        double numItems = 0.0;
        KahluaTableIterator iterator2 = items.iterator();
        while (iterator2.advance()) {
            KahluaTable item = (KahluaTable)iterator2.getValue();
            if (item == null) continue;
            numItems += LuaHelpers.castDouble(item.rawget("count")).doubleValue();
        }
        KahluaTable option = subMenu.addActionsOption(Translator.getText("ContextMenu_AllWithCount", Math.min(numItems, (double)waterRemaining)), LuaManager.getFunctionObject("CleanBandages.onCleanAll"), waterObject, items);
        if (waterObject.isTaintedWater() && SandboxOptions.getInstance().enableTaintedWaterText.getValue()) {
            ISToolTipWrapper tooltip = ISWorldObjectContextMenuLogic.addToolTip();
            tooltip.getTable().rawset("description", (Object)(" <RGB.1,0.5,0.5> " + Translator.getText("Tooltip_item_TaintedWater")));
            tooltip.getTable().rawset("maxLineWidth", (Object)512.0);
            option.rawset("toolTip", (Object)tooltip.getTable());
            option.rawset("notAvailable", (Object)true);
        }
        iterator2 = items.iterator();
        while (iterator2.advance()) {
            Object item = iterator2.getValue();
            if (item == null) continue;
            LuaHelpers.callLuaClass("CleanBandages", "setSubmenu", null, subMenu, item, waterObject);
        }
    }

    private static boolean toggleClothingWasher(ISContextMenuWrapper context, KahluaTable worldobjects, double playerId, IsoPlayer playerObj, IsoClothingWasher object) {
        if (object == null || object.getContainer() == null) {
            return false;
        }
        if (ISWorldObjectContextMenuLogic.isSomethingTo(object, playerObj)) {
            return false;
        }
        if (Core.getInstance().getGameMode().equals("LastStand")) {
            return false;
        }
        KahluaTable option = object.isActivated() ? context.addGetUpOption(Translator.getText("ContextMenu_Turn_Off"), worldobjects, LuaManager.getFunctionObject("ISWorldObjectContextMenu.onToggleClothingWasher"), object, playerId) : context.addGetUpOption(Translator.getText("ContextMenu_Turn_On"), worldobjects, LuaManager.getFunctionObject("ISWorldObjectContextMenu.onToggleClothingWasher"), object, playerId);
        if (!object.getContainer().isPowered() || object.getFluidAmount() <= 0.0f) {
            option.rawset("notAvailable", (Object)true);
            ISToolTipWrapper toolTip = ISWorldObjectContextMenuLogic.addToolTip();
            option.rawset("toolTip", (Object)toolTip.getTable());
            toolTip.setVisible(false);
            toolTip.setName(ISWorldObjectContextMenuLogic.getMoveableDisplayName(object));
            Object description = "";
            if (!object.getContainer().isPowered()) {
                description = Translator.getText("IGUI_RadioRequiresPowerNearby");
            }
            if (object.getFluidAmount() <= 0.0f) {
                description = !((String)description).isEmpty() ? (String)description + "\n" + Translator.getText("IGUI_RequiresWaterSupply") : Translator.getText("IGUI_RequiresWaterSupply");
            }
            toolTip.getTable().rawset("description", description);
        }
        return false;
    }

    private static boolean toggleComboWasherDryer(ISContextMenuWrapper context, IsoPlayer playerObj, IsoCombinationWasherDryer object, boolean bAddObjectSubmenu) {
        String label;
        PropertyContainer props;
        if (object.getContainer() == null || ISWorldObjectContextMenuLogic.isSomethingTo(object, playerObj) || Core.getInstance().getGameMode().equals("LastStand")) {
            return false;
        }
        String objectName = object.getName();
        if (objectName == null) {
            objectName = "Combo Washer/Dryer";
        }
        if ((props = object.getProperties()) != null) {
            String groupName = props.get("GroupName");
            String customName = props.get("CustomName");
            if (groupName != null && customName != null) {
                objectName = Translator.getMoveableDisplayName(groupName + " " + customName);
            } else if (customName != null) {
                objectName = Translator.getMoveableDisplayName(customName);
            }
        }
        ISContextMenuWrapper subMenu = context;
        if (bAddObjectSubmenu) {
            KahluaTable subOption = context.addOption(objectName, null, null, new Object[0]);
            subMenu = ISContextMenuWrapper.getNew(context);
            context.addSubMenu(subOption, subMenu.getTable());
        }
        KahluaTable option = object.isActivated() ? subMenu.addGetUpOption(Translator.getText("ContextMenu_Turn_Off"), playerObj, LuaManager.getFunctionObject("ISWorldObjectContextMenu.onToggleComboWasherDryer"), object) : subMenu.addGetUpOption(Translator.getText("ContextMenu_Turn_On"), playerObj, LuaManager.getFunctionObject("ISWorldObjectContextMenu.onToggleComboWasherDryer"), object);
        String string = label = object.isModeWasher() ? Translator.getText("ContextMenu_ComboWasherDryer_SetModeDryer") : Translator.getText("ContextMenu_ComboWasherDryer_SetModeWasher");
        if (!object.getContainer().isPowered() || object.isModeWasher() && object.getFluidAmount() <= 0.0f) {
            option.rawset("notAvailable", (Object)true);
            ISToolTipWrapper toolTip = ISWorldObjectContextMenuLogic.addToolTip();
            option.rawset("toolTip", (Object)toolTip.getTable());
            toolTip.setVisible(false);
            toolTip.setName(ISWorldObjectContextMenuLogic.getMoveableDisplayName(object));
            Object description = "";
            if (!object.getContainer().isPowered()) {
                description = Translator.getText("IGUI_RadioRequiresPowerNearby");
            }
            if (object.isModeWasher() && object.getFluidAmount() <= 0.0f) {
                description = !((String)description).isEmpty() ? (String)description + "\n" + Translator.getText("IGUI_RequiresWaterSupply") : Translator.getText("IGUI_RequiresWaterSupply");
            }
            toolTip.getTable().rawset("description", description);
        }
        option = subMenu.addGetUpOption(label, playerObj, LuaManager.getFunctionObject("ISWorldObjectContextMenu.onSetComboWasherDryerMode"), object, object.isModeWasher() ? "dryer" : "washer");
        return false;
    }

    private static boolean toggleClothingDryer(ISContextMenuWrapper context, double playerId, IsoPlayer playerObj, KahluaTable worldobjects, IsoClothingDryer object) {
        if (object == null || object.getContainer() == null) {
            return false;
        }
        if (ISWorldObjectContextMenuLogic.isSomethingTo(object, playerObj)) {
            return false;
        }
        if (Core.getInstance().getGameMode().equals("LastStand")) {
            return false;
        }
        KahluaTable option = object.isActivated() ? context.addGetUpOption(Translator.getText("ContextMenu_Turn_Off"), worldobjects, LuaManager.getFunctionObject("ISWorldObjectContextMenu.onToggleClothingDryer"), object, playerId) : context.addGetUpOption(Translator.getText("ContextMenu_Turn_On"), worldobjects, LuaManager.getFunctionObject("ISWorldObjectContextMenu.onToggleClothingDryer"), object, playerId);
        if (!object.getContainer().isPowered()) {
            option.rawset("notAvailable", (Object)true);
            ISToolTipWrapper toolTip = ISWorldObjectContextMenuLogic.addToolTip();
            option.rawset("toolTip", (Object)toolTip.getTable());
            toolTip.setVisible(false);
            toolTip.setName(ISWorldObjectContextMenuLogic.getMoveableDisplayName(object));
            toolTip.getTable().rawset("description", (Object)Translator.getText("IGUI_RadioRequiresPowerNearby"));
        }
        return false;
    }

    private static boolean onWashingDryer(String source2, ISContextMenuWrapper context, IsoClothingDryer object, IsoPlayer player, KahluaTable worldobjects) {
        KahluaTable mainOption = context.addOption(source2, null, null, new Object[0]);
        ISContextMenuWrapper mainSubMenu = ISContextMenuWrapper.getNew(context);
        context.addSubMenu(mainOption, mainSubMenu.getTable());
        return ISWorldObjectContextMenuLogic.toggleClothingDryer(mainSubMenu, player.getPlayerNum(), player, worldobjects, object);
    }

    private static boolean doStoveOption(KahluaTable fetch, boolean test, ISContextMenuWrapper context, double player, IsoPlayer playerObj) {
        Object worldobjects = null;
        IsoStove stove = (IsoStove)fetch.rawget("stove");
        if (stove != null && !ISWorldObjectContextMenuLogic.isSomethingTo(stove, playerObj) && !Core.getInstance().getGameMode().equals("LastStand") && stove.getContainer() != null && stove.getContainer().isPowered()) {
            if (test) {
                return true;
            }
            KahluaTable stoveOption = context.addOption(stove.getTileName(), null, null, new Object[0]);
            Texture texture = Texture.trygetTexture(stove.getSpriteName());
            if (texture instanceof Texture) {
                Texture texture2 = texture;
                stoveOption.rawset("iconTexture", (Object)texture2.splitIcon());
            } else {
                stoveOption.rawset("iconTexture", ((KahluaTable)LuaManager.env.rawget("ContainerButtonIcons")).rawget(stove.isMicrowave() ? "microwave" : "stove"));
            }
            ISContextMenuWrapper stoveMenu = ISContextMenuWrapper.getNew(context);
            context.addSubMenu(stoveOption, stoveMenu.getTable());
            String key = stove.Activated() ? "ContextMenu_Turn_Off" : "ContextMenu_Turn_On";
            stoveMenu.addGetUpOption(Translator.getText(key), worldobjects, LuaManager.getFunctionObject("ISWorldObjectContextMenu.onToggleStove"), stove, player);
            ItemContainer itemContainer = stove.getContainer();
            if (itemContainer instanceof ItemContainer) {
                ItemContainer container = itemContainer;
                if (container.isMicrowave()) {
                    stoveMenu.addGetUpOption(Translator.getText("ContextMenu_StoveSetting"), worldobjects, LuaManager.getFunctionObject("ISWorldObjectContextMenu.onMicrowaveSetting"), stove, player);
                } else if (container.isStove()) {
                    stoveMenu.addGetUpOption(Translator.getText("ContextMenu_StoveSetting"), worldobjects, LuaManager.getFunctionObject("ISWorldObjectContextMenu.onStoveSetting"), stove, player);
                }
            }
        }
        return false;
    }

    public static boolean checkBlowTorchForBarricade(IsoPlayer chr) {
        return chr.getInventory().getFirstRecurse(item -> item.getType().equals("BlowTorch") && item.getCurrentUses() >= 1) != null;
    }

    private static void addTileDebugInfo(ISContextMenuWrapper context, KahluaTable fetch) {
        if (!DebugOptions.instance.uiShowContextMenuReportOptions.getValue()) {
            return;
        }
        KahluaTable option = context.addDebugOption(Translator.getText("Tile Report") + ": " + LuaHelpers.castString(fetch.rawget("tilename")), null, null, new Object[0]);
        if (option == null) {
            return;
        }
        ISToolTipWrapper toolTipWrapper = new ISToolTipWrapper();
        option.rawset("toolTip", (Object)toolTipWrapper.getTable());
        toolTipWrapper.initialise();
        toolTipWrapper.setVisible(false);
        toolTipWrapper.setName("Tile params:");
        IsoObject tileObj = (IsoObject)fetch.rawget("tileObj");
        PropertyContainer props = tileObj.getProperties();
        ArrayList<String> names = props.getPropertyNames();
        StringBuilder params = new StringBuilder("Properties:\n");
        for (int i = 0; i < names.size(); ++i) {
            params.append(names.get(i)).append(" = ").append(props.get(names.get(i))).append("\n");
        }
        params.append("\nFlags:\n");
        ArrayList<IsoFlagType> flags = props.getFlagsList();
        for (int i = 0; i < flags.size(); ++i) {
            params.append((Object)flags.get(i)).append("\n");
        }
        if (tileObj != null) {
            IHasHealth health = (IHasHealth)fetch.rawget("health");
            boolean movedThumpable = tileObj.isMovedThumpable();
            boolean thumpable = tileObj instanceof IsoThumpable;
            params.append("\n").append(Translator.getText("ContextMenu_IsMovedThumpable")).append(" = ").append(movedThumpable).append("\n");
            params.append(Translator.getText("ContextMenu_IsThumpable")).append(" = ").append(thumpable).append("\n");
            params.append(Translator.getText("ContextMenu_HasHealth")).append(" = ").append(health != null).append("\n");
            params.append(Translator.getText("ContextMenu_IsBreakableFence")).append(" = ").append(BrokenFences.getInstance().isBreakableObject(tileObj)).append("\n");
            params.append("\n");
            if (movedThumpable || thumpable || BrokenFences.getInstance().isBreakableObject(tileObj) || health != null) {
                params.append(Translator.getText("ContextMenu_IsZombieThumpable")).append("\n");
            } else {
                params.append(Translator.getText("ContextMenu_IsNotZombieThumpable")).append("\n");
            }
            if (thumpable || health != null) {
                params.append(Translator.getText("ContextMenu_IsPlayerThumpable")).append("\n");
            } else {
                params.append(Translator.getText("ContextMenu_IsNotPlayerThumpable")).append("\n");
            }
        }
        toolTipWrapper.getTable().rawset("description", (Object)params.toString());
    }

    private static void doContextConfigOptionsFromFetch(ISContextMenuWrapper context, KahluaTable fetch, IsoPlayer playerObj) {
        GameEntity entity = (GameEntity)fetch.rawget("entityContext");
        ISWorldObjectContextMenuLogic.doContextConfigOptions(context, entity, playerObj);
    }

    private static void doContextConfigOptions(ISContextMenuWrapper context, GameEntity entity, IsoPlayer playerObj) {
        ContextMenuConfig contextConfig = (ContextMenuConfig)entity.getComponent(ComponentType.ContextMenuConfig);
        ArrayList<ContextMenuConfigScript.EntryScript> entries = contextConfig.getEntries();
        for (int n = 0; n < entries.size(); ++n) {
            String customFunction;
            KahluaTable option;
            ContextMenuConfigScript.EntryScript entry = entries.get(n);
            StringBuilder text = new StringBuilder("ContextMenu_");
            String textRef = Translator.getText(String.valueOf(text.append(entry.getMenu())));
            String customSubmenu = entry.getCustomSubmenu();
            String icon = entry.getIcon();
            if (customSubmenu != null) {
                option = context.addOption(textRef, null, null, new Object[0]);
                Object functionObject = LuaManager.getFunctionObject(customSubmenu);
                KahluaTable arguments = LuaManager.platform.newTable();
                arguments.rawset("option", (Object)option);
                arguments.rawset("entity", (Object)entity);
                arguments.rawset("playerObj", (Object)playerObj);
                arguments.rawset("extraParam", (Object)entry.getExtraParam());
                LuaManager.caller.protectedCall(LuaManager.thread, functionObject, context.getTable(), arguments);
            }
            if ((customFunction = entry.getCustomFunction()) == null) continue;
            option = context.addGetUpOption(textRef, context, LuaManager.getFunctionObject("ISWorldObjectContextMenu.onCustomFunction"), entity, playerObj, customFunction, entry.getExtraParam());
            option.rawset("iconTexture", (Object)(icon != null ? TexturePackPage.getTexture(icon) : null));
        }
    }
}

