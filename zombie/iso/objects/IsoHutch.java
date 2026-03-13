/*
 * Decompiled with CFR 0.152.
 */
package zombie.iso.objects;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import se.krka.kahlua.j2se.KahluaTableImpl;
import se.krka.kahlua.vm.KahluaTableIterator;
import zombie.GameTime;
import zombie.GameWindow;
import zombie.Lua.LuaManager;
import zombie.SystemDisabler;
import zombie.UsedFromLua;
import zombie.characters.IsoGameCharacter;
import zombie.characters.animals.IsoAnimal;
import zombie.core.network.ByteBufferReader;
import zombie.core.network.ByteBufferWriter;
import zombie.core.random.Rand;
import zombie.core.utils.UpdateLimit;
import zombie.debug.DebugLog;
import zombie.inventory.InventoryItem;
import zombie.inventory.types.Food;
import zombie.iso.IsoCell;
import zombie.iso.IsoGridSquare;
import zombie.iso.IsoObject;
import zombie.iso.IsoWorld;
import zombie.iso.areas.DesignationZoneAnimal;
import zombie.iso.objects.IsoDeadBody;
import zombie.iso.sprite.IsoSprite;
import zombie.iso.sprite.IsoSpriteManager;
import zombie.network.GameClient;
import zombie.network.GameServer;
import zombie.network.PacketTypes;
import zombie.network.packets.INetworkPacket;
import zombie.popman.animal.AnimalSynchronizationManager;
import zombie.popman.animal.HutchManager;
import zombie.util.StringUtils;
import zombie.util.Type;

@UsedFromLua
public class IsoHutch
extends IsoObject {
    int linkedX;
    int linkedY;
    int linkedZ;
    boolean open;
    boolean openEggHatch;
    KahluaTableImpl def;
    public int savedX;
    public int savedY;
    public int savedZ;
    public HashMap<Integer, IsoAnimal> animalInside = new HashMap();
    public HashMap<Integer, IsoDeadBody> deadBodiesInside = new HashMap();
    public ArrayList<IsoAnimal> animalOutside = new ArrayList();
    public String type;
    public int lastHourCheck = -1;
    private float exitTimer;
    private int enterSpotX;
    private int enterSpotY;
    private int maxAnimals;
    private int maxNestBox;
    private final HashMap<Integer, NestBox> nestBoxes = new HashMap();
    private float nestBoxDirt;
    private float hutchDirt;
    UpdateLimit updateAnimal = new UpdateLimit(3500L);
    byte animalInsideSize;
    boolean sendUpdate;

    public IsoHutch(IsoCell cell) {
        super(cell);
    }

    public IsoHutch(IsoGridSquare sq, boolean north, String mainSprite, KahluaTableImpl def, IsoGridSquare linkedSq) {
        super(sq, mainSprite, null);
        DesignationZoneAnimal zone;
        this.def = def;
        sq.AddSpecialObject(this);
        if (linkedSq != null) {
            this.linkedX = linkedSq.x;
            this.linkedY = linkedSq.y;
            this.linkedZ = linkedSq.z;
            HutchManager.getInstance().remove(this);
            return;
        }
        if (def == null) {
            return;
        }
        this.type = def.rawgetStr("name");
        KahluaTableImpl extraSprites = (KahluaTableImpl)def.rawget("extraSprites");
        if (extraSprites == null) {
            return;
        }
        this.savedX = sq.x;
        this.savedY = sq.y;
        this.savedZ = sq.z;
        if (!HutchManager.getInstance().checkHutchExistInList(this)) {
            HutchManager.getInstance().add(this);
        }
        if ((zone = DesignationZoneAnimal.getZoneF(this.getX(), this.getY(), this.getZ())) != null && !zone.hutchs.contains(this)) {
            zone.hutchs.add(this);
        }
        KahluaTableIterator iterator2 = extraSprites.iterator();
        while (iterator2.advance()) {
            KahluaTableImpl spriteDef = (KahluaTableImpl)iterator2.getValue();
            int xoffset = spriteDef.rawgetInt("xoffset");
            int yoffset = spriteDef.rawgetInt("yoffset");
            boolean zoffset = false;
            String sprite = spriteDef.rawgetStr("sprite");
            IsoGridSquare sq2 = IsoWorld.instance.currentCell.getGridSquare(sq.getX() + xoffset, sq.getY() + yoffset, sq.getZ() + 0);
            if (sq2 == null) continue;
            new IsoHutch(sq2, north, sprite, def, sq);
        }
        for (int i = 0; i < this.getMaxNestBox() + 1; ++i) {
            this.nestBoxes.put(i, new NestBox(this, i));
        }
    }

    public IsoHutch getHutch() {
        if (!this.isSlave()) {
            return this;
        }
        IsoGridSquare sq = IsoWorld.instance.currentCell.getGridSquare(this.linkedX, this.linkedY, this.linkedZ);
        if (sq == null) {
            return null;
        }
        for (int i = 0; i < sq.getSpecialObjects().size(); ++i) {
            IsoObject isoObject = sq.getSpecialObjects().get(i);
            if (!(isoObject instanceof IsoHutch)) continue;
            IsoHutch hutch = (IsoHutch)isoObject;
            return hutch;
        }
        return null;
    }

    public static IsoHutch getHutch(int x, int y, int z) {
        IsoGridSquare sq = IsoWorld.instance.currentCell.getGridSquare(x, y, z);
        if (sq == null) {
            return null;
        }
        for (IsoObject isoObject : sq.getSpecialObjects()) {
            if (!(isoObject instanceof IsoHutch)) continue;
            IsoHutch hutch = (IsoHutch)isoObject;
            return hutch;
        }
        return null;
    }

    @Override
    public void transmitCompleteItemToClients() {
        if (GameServer.server) {
            if (GameServer.udpEngine == null) {
                return;
            }
            if (SystemDisabler.doWorldSyncEnable) {
                return;
            }
            KahluaTableImpl extraSprites = (KahluaTableImpl)this.def.rawget("extraSprites");
            if (extraSprites != null) {
                KahluaTableIterator iterator2 = extraSprites.iterator();
                while (iterator2.advance()) {
                    KahluaTableImpl spriteDef = (KahluaTableImpl)iterator2.getValue();
                    int xoffset = spriteDef.rawgetInt("xoffset");
                    int yoffset = spriteDef.rawgetInt("yoffset");
                    boolean zoffset = false;
                    IsoGridSquare sq2 = IsoWorld.instance.currentCell.getGridSquare(this.square.getX() + xoffset, this.square.getY() + yoffset, this.square.getZ() + 0);
                    for (int i = 0; i < sq2.getSpecialObjects().size(); ++i) {
                        IsoObject isoObject = sq2.getSpecialObjects().get(i);
                        if (!(isoObject instanceof IsoHutch)) continue;
                        IsoHutch hutch = (IsoHutch)isoObject;
                        INetworkPacket.sendToRelative(PacketTypes.PacketType.AddItemToMap, this.square.x, (float)this.square.y, hutch);
                    }
                }
            }
        }
    }

    @Override
    public void syncIsoObjectSend(ByteBufferWriter b) {
        b.putInt(this.square.getX());
        b.putInt(this.square.getY());
        b.putInt(this.square.getZ());
        b.putByte(this.getObjectIndex());
        b.putBoolean(true);
        b.putBoolean(false);
        b.putBoolean(this.open);
        b.putBoolean(this.openEggHatch);
        b.putFloat(this.getHutchDirt());
        b.putFloat(this.getNestBoxDirt());
        b.putByte(this.nestBoxes.size());
        for (NestBox nestBox : this.nestBoxes.values()) {
            b.putByte(nestBox.eggs.size());
            for (Food egg : nestBox.eggs) {
                try {
                    egg.saveWithSize(b.bb, true);
                }
                catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        }
    }

    @Override
    public void syncIsoObjectReceive(ByteBufferReader bb) {
        boolean open = bb.getBoolean();
        boolean openEggHatch = bb.getBoolean();
        float hutchDirt = bb.getFloat();
        float nestBoxDirt = bb.getFloat();
        if (this.open != open) {
            this.toggleDoor();
        }
        if (this.openEggHatch != openEggHatch) {
            this.toggleEggHatchDoor();
        }
        this.setHutchDirt(hutchDirt);
        this.setNestBoxDirt(nestBoxDirt);
        int nestBoxes = bb.getByte();
        for (int i = 0; i < nestBoxes; ++i) {
            int eggs = bb.getByte();
            NestBox nestBox = this.getNestBox(i);
            nestBox.eggs.clear();
            for (int j = 0; j < eggs; ++j) {
                try {
                    InventoryItem egg = InventoryItem.loadItem(bb.bb, IsoWorld.getWorldVersion());
                    if (!(egg instanceof Food)) continue;
                    Food food = (Food)egg;
                    nestBox.eggs.add(food);
                    continue;
                }
                catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        }
    }

    public boolean haveRoomForNewEggs() {
        for (NestBox nest : this.nestBoxes.values()) {
            if (nest.getEggsNb() >= 10) continue;
            return true;
        }
        return false;
    }

    @Override
    public void update() {
        Object animal;
        int prob;
        if (this.isSlave() || !this.isExistInTheWorld()) {
            return;
        }
        if (!this.isOwner()) {
            return;
        }
        this.sendUpdate = this.updateAnimal.Check() || this.animalInsideSize != this.animalInside.size();
        boolean hourGrow = false;
        if (GameTime.getInstance().getHour() != this.lastHourCheck) {
            this.lastHourCheck = GameTime.getInstance().getHour();
            hourGrow = true;
        }
        if ((prob = 8000 - this.animalInside.size() * 100) < 4500) {
            prob = 4500;
        }
        if (!this.animalInside.isEmpty() && Rand.NextBool(prob)) {
            this.hutchDirt = Math.min(this.hutchDirt + 1.0f, 100.0f);
            this.sync();
        }
        HashMap<Integer, IsoAnimal> animalInside1 = new HashMap<Integer, IsoAnimal>(this.animalInside);
        for (Map.Entry entry : animalInside1.entrySet()) {
            Integer hutchPosition = (Integer)entry.getKey();
            IsoAnimal animal2 = (IsoAnimal)entry.getValue();
            if (animal2 == null) continue;
            if (animal2.nestBox > -1) {
                this.animalInside.remove(hutchPosition);
                continue;
            }
            if (animal2.getData().getHutchPosition() != hutchPosition.intValue()) {
                DebugLog.Animal.warn("animal hutchPosition %d != animalInside index %d".formatted(animal2.getData().getHutchPosition(), hutchPosition));
                animal2.getData().setHutchPosition(hutchPosition);
            }
            this.updateAnimalInside(animal2, hourGrow);
            if (!animal2.isDead() || this.deadBodiesInside.get(hutchPosition) != null) continue;
            IsoDeadBody deadAnimal = new IsoDeadBody(animal2, false, false);
            this.deadBodiesInside.put(hutchPosition, deadAnimal);
        }
        for (NestBox box : this.nestBoxes.values()) {
            animal = box.animal;
            if (animal == null || this.nestBoxes.get(((IsoAnimal)animal).nestBox) == null) continue;
            this.updateAnimalInside((IsoAnimal)animal, hourGrow);
            if (!(((IsoGameCharacter)animal).getHealth() <= 0.0f)) continue;
            this.nestBoxes.get((Object)Integer.valueOf((int)((IsoAnimal)animal).nestBox)).animal = null;
            ((IsoAnimal)animal).nestBox = -1;
            this.addAnimalInside((IsoAnimal)animal);
            break;
        }
        if (this.exitTimer > 0.0f) {
            this.exitTimer = Math.max(0.0f, this.exitTimer - GameTime.getInstance().getMultiplier());
        } else if (!this.animalInside.isEmpty()) {
            ArrayList<Integer> possibleIndexHen = new ArrayList<Integer>();
            ArrayList<Integer> possibleIndexRooster = new ArrayList<Integer>();
            animal = this.animalInside.keySet().iterator();
            while (animal.hasNext()) {
                int index = (Integer)animal.next();
                IsoAnimal animal3 = this.animalInside.get(index);
                if (animal3 == null || animal3.isDead()) continue;
                if (animal3.isFemale() || animal3.isBaby()) {
                    possibleIndexHen.add(index);
                    continue;
                }
                possibleIndexRooster.add(index);
            }
            animal = null;
            if (!possibleIndexRooster.isEmpty()) {
                animal = this.animalInside.get(possibleIndexRooster.get(Rand.Next(0, possibleIndexRooster.size())));
            } else if (!possibleIndexHen.isEmpty()) {
                animal = this.animalInside.get(possibleIndexHen.get(Rand.Next(0, possibleIndexHen.size())));
            }
            this.checkAnimalExitHutch((IsoAnimal)animal);
        }
        for (int i = 0; i < this.nestBoxes.size(); ++i) {
            NestBox nestBox = this.nestBoxes.get(i);
            for (int j = 0; j < nestBox.getEggsNb(); ++j) {
                Food egg = nestBox.getEgg(j);
                if (egg.checkEggHatch(this)) {
                    nestBox.removeEgg(j);
                    --j;
                }
                egg.update();
            }
        }
        if (this.sendUpdate) {
            this.sync();
            this.animalInsideSize = (byte)this.animalInside.size();
        }
    }

    private void updateAnimalInside(IsoAnimal animal, boolean hourGrow) {
        if (animal == null) {
            return;
        }
        if (animal.isDead()) {
            if (this.sendUpdate) {
                this.sendAnimalUpdate(animal);
            }
            return;
        }
        if (animal.nestBox > -1) {
            animal.eggTimerInHutch = Float.valueOf(Math.max(0.0f, (float)animal.eggTimerInHutch - GameTime.getInstance().getMultiplier())).intValue();
            if (Rand.NextBool(300)) {
                this.nestBoxDirt = Math.min(this.nestBoxDirt + 1.0f, 100.0f);
            }
            if (animal.eggTimerInHutch <= 0) {
                animal.eggTimerInHutch = 0;
                this.addEgg(animal);
                this.sendAnimalUpdate(animal);
                this.sync();
            }
        }
        if (animal.adef.isInsideHutchTime(null)) {
            animal.updateStress();
        } else {
            animal.changeStress(GameTime.getInstance().getMultiplier() / 15000.0f);
        }
        this.updateAnimalHealthInside(animal, hourGrow);
        if (animal.getHealth() <= 0.0f) {
            this.killAnimal(animal);
            this.sendAnimalUpdate(animal);
            this.sync();
        }
        animal.getData().checkEggs(GameTime.instance.getCalender(), false);
        if (hourGrow) {
            animal.getData().checkFertilizedTime();
            if (this.getHutchDirt() < 20.0f) {
                animal.setHealth(Math.min(1.0f, animal.getHealth() + animal.getData().getHealthLoss(Float.valueOf(1.0f))));
            }
            animal.setHoursSurvived(animal.getHoursSurvived() + 1.0);
            animal.getData().updateHungerAndThirst(false);
            if (!this.isDoorClosed()) {
                animal.checkKilledByMetaPredator(GameTime.getInstance().getHour());
            }
            if (animal.getData().getAge() != animal.getData().getDaysSurvived()) {
                float mod = animal.getData().getAgeGrowModifier();
                animal.getData().setAge(Float.valueOf((float)animal.getData().getDaysSurvived() + (mod - 1.0f)).intValue());
                animal.setHoursSurvived(animal.getData().getAge() * 24);
                animal.getData().growUp(false);
            }
        }
        if (this.sendUpdate) {
            this.sendAnimalUpdate(animal);
        }
    }

    public void doMeta(int hours) {
        for (int i = 0; i < hours; ++i) {
            int prob = 25 - (this.animalInside.size() + this.animalOutside.size());
            if (prob > 10) {
                prob = 10;
            }
            if (Rand.NextBool(prob)) {
                this.hutchDirt = Math.min(this.hutchDirt + 1.0f, 100.0f);
            }
            if (!Rand.NextBool(prob)) continue;
            this.nestBoxDirt = Math.min(this.nestBoxDirt + 1.0f, 100.0f);
        }
    }

    private void updateAnimalHealthInside(IsoAnimal animal, boolean hourGrow) {
        float dirt = this.getHutchDirt();
        if (animal.nestBox > -1) {
            dirt = this.getNestBoxDirt();
        }
        if (dirt > 20.0f && Rand.NextBool(250 - (int)dirt)) {
            animal.setHealth(animal.getHealth() - 0.01f * (dirt / 1000.0f) * GameTime.getInstance().getMultiplier());
        }
        if (hourGrow) {
            animal.getData().updateHealth();
        }
    }

    public void killAnimal(IsoAnimal animal) {
        animal.setHealth(0.0f);
        int hutchPosition = animal.getData().getHutchPosition();
        IsoDeadBody deadAnimal = new IsoDeadBody(animal, false, false);
        this.deadBodiesInside.put(hutchPosition, deadAnimal);
    }

    private boolean checkAnimalExitHutch(IsoAnimal animal) {
        if (animal == null || animal.adef == null || animal.isDead()) {
            return false;
        }
        boolean exit = false;
        if (animal.getBehavior().forcedOutsideHutch > 0L && GameTime.getInstance().getCalender().getTimeInMillis() > animal.getBehavior().forcedOutsideHutch) {
            exit = true;
            animal.getBehavior().forcedOutsideHutch = 0L;
        }
        if (animal.getBehavior().forcedOutsideHutch == 0L && animal.adef.isOutsideHutchTime() && this.isOpen() && animal.nestBox == -1) {
            exit = true;
        }
        if (exit && !this.isDoorClosed()) {
            IsoGridSquare animalSq = IsoWorld.instance.currentCell.getGridSquare(this.savedX + this.getEnterSpotX(), this.savedY + this.getEnterSpotY(), this.savedZ);
            if (animalSq == null) {
                return false;
            }
            if (animal.nestBox > -1) {
                return false;
            }
            this.releaseAnimal(animalSq, animal);
            this.exitTimer = Rand.Next(200.0f, 300.0f);
            return true;
        }
        return false;
    }

    private void releaseAnimal(IsoGridSquare animalSq, IsoAnimal animal) {
        if (animalSq == null) {
            animalSq = IsoWorld.instance.currentCell.getGridSquare(this.savedX + this.getEnterSpotX(), this.savedY + this.getEnterSpotY(), this.savedZ);
        }
        if (animalSq == null) {
            return;
        }
        if (!GameClient.client) {
            animal.hutch = null;
            animal.getData().setPreferredHutchPosition(-1);
            animal.getData().enterHutchTimerAfterDestroy = 300;
            animal.addToWorld();
            animal.setX(animalSq.getX());
            animal.setY(animalSq.getY());
            animal.setZ(animalSq.getZ());
            if (!animal.getCell().getObjectList().contains(animal) && !animal.getCell().getAddList().contains(animal)) {
                animal.getCell().getAddList().add(animal);
            }
            animal.setStateEventDelayTimer(0.0f);
        }
        this.animalInside.remove(animal.getData().getHutchPosition());
        animal.getData().setHutchPosition(-1);
        this.animalOutside.add(animal);
        if (this.isOwner()) {
            this.sync();
        }
    }

    public void removeAnimal(IsoAnimal animal) {
        animal.hutch = null;
        this.animalInside.remove(animal.getData().getHutchPosition());
        this.deadBodiesInside.remove(animal.getData().getHutchPosition());
        animal.getData().setHutchPosition(-1);
        this.sendAnimalUpdate(animal);
    }

    private void removeAnimalFromNestBox(NestBox nestBox) {
        IsoAnimal animal = nestBox.animal;
        nestBox.animal.nestBox = -1;
        nestBox.animal.getData().eggTime = Long.valueOf(GameTime.instance.getCalender().getTimeInMillis() / 1000L).intValue() + Rand.Next(0, 86400);
        nestBox.animal = null;
        this.addAnimalInside(animal);
    }

    public void tryFindAndRemoveAnimalFromNestBox(IsoAnimal animal) {
        this.nestBoxes.values().forEach(nestBox -> {
            if (nestBox.animal != null && nestBox.animal.getAnimalID() == animal.getAnimalID()) {
                nestBox.animal.nestBox = -1;
                nestBox.animal.getData().eggTime = Long.valueOf(GameTime.instance.getCalender().getTimeInMillis() / 1000L).intValue() + Rand.Next(0, 86400);
                nestBox.animal = null;
            }
        });
    }

    public boolean addAnimalInNestBox(IsoAnimal animal) {
        for (int i = 0; i < this.getMaxNestBox() + 1; ++i) {
            NestBox nestBox = this.nestBoxes.get(i);
            if (nestBox.animal != null || nestBox.eggs.size() >= 10) continue;
            nestBox.animal = animal;
            animal.hutch = this;
            animal.nestBox = i;
            animal.eggTimerInHutch = Rand.Next(350, 600);
            this.animalInside.remove(animal.getData().getHutchPosition());
            animal.getData().setHutchPosition(-1);
            if (this.isOwner()) {
                this.sendAnimalUpdate(animal);
                this.sync();
            }
            return true;
        }
        return false;
    }

    public void addEgg(IsoAnimal animal) {
        Food egg = animal.createEgg();
        this.nestBoxes.get(animal.nestBox).addEgg(egg);
        this.removeAnimalFromNestBox(this.nestBoxes.get(animal.nestBox));
    }

    public void toggleEggHatchDoor() {
        this.openEggHatch = !this.openEggHatch;
        KahluaTableImpl doors = (KahluaTableImpl)this.def.rawget("eggHatchDoors");
        KahluaTableIterator it = doors.iterator();
        while (it.advance()) {
            IsoGridSquare sq2;
            KahluaTableImpl info = (KahluaTableImpl)it.getValue();
            String testSpriteName = info.rawgetStr("sprite");
            String newSpriteName = info.rawgetStr("closedSprite");
            if (this.openEggHatch) {
                testSpriteName = info.rawgetStr("closedSprite");
                newSpriteName = info.rawgetStr("sprite");
            }
            int hatchXOffset = info.rawgetInt("xoffset");
            int hatchYOffset = info.rawgetInt("yoffset");
            int hatchZOffset = info.rawgetInt("zoffset");
            if (StringUtils.isNullOrEmpty(testSpriteName) || StringUtils.isNullOrEmpty(newSpriteName)) continue;
            IsoSprite testSprite = IsoSpriteManager.instance.namedMap.get(testSpriteName);
            IsoSprite newSprite = IsoSpriteManager.instance.namedMap.get(newSpriteName);
            if (testSprite == null || newSprite == null || (sq2 = IsoWorld.instance.currentCell.getGridSquare(this.square.getX() + hatchXOffset, this.square.getY() + hatchYOffset, this.square.getZ() + hatchZOffset)) == null) continue;
            for (int i = 0; i < sq2.getSpecialObjects().size(); ++i) {
                IsoHutch hutch = Type.tryCastTo(sq2.getSpecialObjects().get(i), IsoHutch.class);
                if (hutch == null || !testSpriteName.equals(hutch.sprite.getName())) continue;
                hutch.setSprite(newSprite);
                hutch.spriteName = newSpriteName;
            }
        }
    }

    public void reforceUpdate() {
        HutchManager.getInstance().reforceUpdate(this);
    }

    public void toggleDoor() {
        KahluaTableImpl extraSprites;
        this.reforceUpdate();
        boolean bl = this.open = !this.open;
        if (this.open) {
            for (int i = 0; i < this.animalOutside.size(); ++i) {
                this.animalOutside.get(i).getBehavior().callToHutch(this, false);
            }
        }
        if ((extraSprites = (KahluaTableImpl)this.def.rawget("extraSprites")) == null) {
            return;
        }
        KahluaTableIterator iterator2 = extraSprites.iterator();
        while (iterator2.advance()) {
            IsoGridSquare sq2;
            KahluaTableImpl spriteDef = (KahluaTableImpl)iterator2.getValue();
            int xoffset = spriteDef.rawgetInt("xoffset");
            int yoffset = spriteDef.rawgetInt("yoffset");
            boolean zoffset = false;
            String testSpriteName = spriteDef.rawgetStr("sprite");
            String newSpriteName = spriteDef.rawgetStr("spriteOpen");
            if (!this.open) {
                testSpriteName = spriteDef.rawgetStr("spriteOpen");
                newSpriteName = spriteDef.rawgetStr("sprite");
            }
            if (StringUtils.isNullOrEmpty(newSpriteName) || StringUtils.isNullOrEmpty(testSpriteName)) continue;
            IsoSprite testSprite = IsoSpriteManager.instance.namedMap.get(testSpriteName);
            IsoSprite newSprite = IsoSpriteManager.instance.namedMap.get(newSpriteName);
            if (testSprite == null || newSprite == null || (sq2 = IsoWorld.instance.currentCell.getGridSquare(this.square.getX() + xoffset, this.square.getY() + yoffset, this.square.getZ() + 0)) == null) continue;
            for (int i = 0; i < sq2.getSpecialObjects().size(); ++i) {
                IsoHutch hutch = Type.tryCastTo(sq2.getSpecialObjects().get(i), IsoHutch.class);
                if (hutch == null || !testSpriteName.equals(hutch.sprite.getName())) continue;
                hutch.setSprite(newSprite);
            }
        }
    }

    public boolean isOpen() {
        return this.open;
    }

    private void sendAnimalUpdate(IsoAnimal animal) {
        if (GameServer.server) {
            animal.networkAi.setAnimalPacket(null);
            AnimalSynchronizationManager.getInstance().setSendToClients(animal.onlineId);
        }
    }

    private KahluaTableImpl getDefFromSprite() {
        String spriteName1 = this.getSprite().getName();
        if (StringUtils.isNullOrEmpty(spriteName1)) {
            return null;
        }
        KahluaTableImpl definitions = (KahluaTableImpl)LuaManager.env.rawget("HutchDefinitions");
        if (definitions == null) {
            return null;
        }
        KahluaTableImpl hutchs = (KahluaTableImpl)definitions.rawget("hutchs");
        KahluaTableIterator iterator2 = hutchs.iterator();
        while (iterator2.advance()) {
            KahluaTableImpl value = (KahluaTableImpl)iterator2.getValue();
            String baseSprite = value.rawgetStr("baseSprite");
            if (spriteName1.equals(baseSprite)) {
                return value;
            }
            KahluaTableImpl extraSprites = (KahluaTableImpl)value.rawget("extraSprites");
            KahluaTableIterator iterator22 = extraSprites.iterator();
            while (iterator22.advance()) {
                KahluaTableImpl spriteDef = (KahluaTableImpl)iterator22.getValue();
                if (!spriteName1.equals(spriteDef.rawgetStr("sprite")) && !spriteName1.equals(spriteDef.rawgetStr("spriteOpen"))) continue;
                return value;
            }
            KahluaTableImpl eggHatchDoors = (KahluaTableImpl)value.rawget("eggHatchDoors");
            if (eggHatchDoors == null) continue;
            KahluaTableIterator iterator3 = eggHatchDoors.iterator();
            while (iterator3.advance()) {
                KahluaTableImpl spriteDef = (KahluaTableImpl)iterator3.getValue();
                if (!spriteName1.equals(spriteDef.rawgetStr("sprite")) && !spriteName1.equals(spriteDef.rawgetStr("closedSprite"))) continue;
                return value;
            }
        }
        return null;
    }

    private boolean checkNestBoxPrefPosition(int pos) {
        for (int i = 0; i < this.getMaxNestBox() + 1; ++i) {
            IsoAnimal nestAnimal = this.nestBoxes.get((Object)Integer.valueOf((int)i)).animal;
            if (nestAnimal == null || nestAnimal.getData().getPreferredHutchPosition() != pos) continue;
            return true;
        }
        return false;
    }

    public boolean addAnimalInside(IsoAnimal animal) {
        return this.addAnimalInside(animal, true);
    }

    public boolean addAnimalInside(IsoAnimal animal, boolean bSync) {
        if (this.animalInside.containsValue(animal)) {
            DebugLog.Animal.warn("animal already exists in animalInside");
            return false;
        }
        if (animal.getData().getPreferredHutchPosition() == -1) {
            animal.getData().setPreferredHutchPosition(Rand.Next(0, this.getMaxAnimals()));
        }
        int tries = 0;
        while ((this.animalInside.get(animal.getData().getPreferredHutchPosition()) != null || this.deadBodiesInside.get(animal.getData().getPreferredHutchPosition()) != null || this.checkNestBoxPrefPosition(animal.getData().getPreferredHutchPosition())) && ++tries <= 100) {
            animal.getData().setPreferredHutchPosition(Rand.Next(0, this.getMaxAnimals()));
        }
        if (this.animalInside.get(animal.getData().getPreferredHutchPosition()) == null) {
            this.animalInside.put(animal.getData().getPreferredHutchPosition(), animal);
            animal.hutch = this;
            animal.getData().setHutchPosition(animal.getData().getPreferredHutchPosition());
            if (bSync && this.isOwner()) {
                this.sendAnimalUpdate(animal);
                this.sync();
            }
            this.tryRemoveAnimalFromWorld(animal);
            return true;
        }
        return false;
    }

    public void addAnimalOutside(IsoAnimal animal) {
        if (GameClient.client) {
            return;
        }
        animal.hutch = null;
    }

    @Override
    public void load(ByteBuffer input, int worldVersion, boolean isDebugSave) throws IOException {
        super.load(input, worldVersion, isDebugSave);
        this.linkedX = input.getInt();
        this.linkedY = input.getInt();
        this.linkedZ = input.getInt();
        if (!this.isSlave()) {
            int i;
            if (worldVersion >= 204) {
                this.spriteName = GameWindow.ReadString(input);
                this.sprite = IsoSpriteManager.instance.getSprite(this.spriteName);
            }
            this.def = this.getDefFromSprite();
            if (this.def == null) {
                throw new IOException("hutch definition not found");
            }
            this.type = this.def.rawgetStr("name");
            boolean bl = this.open = input.get() != 0;
            if (worldVersion >= 204) {
                this.openEggHatch = input.get() != 0;
            }
            this.savedX = input.getInt();
            this.savedY = input.getInt();
            this.savedZ = input.getInt();
            ArrayList<IsoAnimal> loadedAnimals = new ArrayList<IsoAnimal>();
            if (worldVersion >= 212) {
                int size = input.getInt();
                if (GameClient.client) {
                    input.position(input.position() + size);
                } else {
                    int nbOfAnimals = input.get();
                    for (int i2 = 0; i2 < nbOfAnimals; ++i2) {
                        IsoAnimal animal = new IsoAnimal(IsoWorld.instance.getCell());
                        boolean seri = input.get() != 0;
                        byte classID = input.get();
                        animal.load(input, worldVersion, isDebugSave);
                        loadedAnimals.add(animal);
                        animal.removeFromSquare();
                    }
                }
            } else {
                int nbOfAnimals = input.get();
                for (i = 0; i < nbOfAnimals; ++i) {
                    IsoAnimal animal = new IsoAnimal(IsoWorld.instance.getCell());
                    boolean seri = input.get() != 0;
                    byte classID = input.get();
                    animal.load(input, worldVersion, isDebugSave);
                    loadedAnimals.add(animal);
                    animal.removeFromSquare();
                }
            }
            this.hutchDirt = input.getFloat();
            this.nestBoxDirt = input.getFloat();
            int nestBoxCount = input.get();
            for (i = 0; i < nestBoxCount; ++i) {
                NestBox nestBox = i < this.nestBoxes.size() ? this.nestBoxes.get(i) : new NestBox(this, i);
                nestBox.load(input, worldVersion);
                if (this.nestBoxes.containsKey(i) || i > this.getMaxNestBox()) continue;
                this.nestBoxes.put(i, nestBox);
            }
            for (i = 0; i < loadedAnimals.size(); ++i) {
                IsoAnimal animal = (IsoAnimal)loadedAnimals.get(i);
                this.addAnimalInside(animal, false);
            }
        }
    }

    @Override
    public void save(ByteBuffer output, boolean isDebugSave) throws IOException {
        super.save(output, isDebugSave);
        output.putInt(this.linkedX);
        output.putInt(this.linkedY);
        output.putInt(this.linkedZ);
        if (!this.isSlave()) {
            GameWindow.WriteString(output, this.spriteName);
            output.put(this.isOpen() ? (byte)1 : 0);
            output.put(this.isEggHatchDoorOpen() ? (byte)1 : 0);
            output.putInt(this.savedX);
            output.putInt(this.savedY);
            output.putInt(this.savedZ);
            int pos = output.position();
            output.putInt(0);
            int posStart = output.position();
            ArrayList<IsoAnimal> animals = new ArrayList<IsoAnimal>(this.animalInside.values());
            output.put((byte)animals.size());
            for (int i = 0; i < animals.size(); ++i) {
                animals.get(i).save(output, isDebugSave);
            }
            int posEnd = output.position();
            output.position(pos);
            output.putInt(posEnd - posStart);
            output.position(posEnd);
            output.putFloat(this.hutchDirt);
            output.putFloat(this.nestBoxDirt);
            output.put((byte)this.nestBoxes.size());
            for (int i = 0; i < this.nestBoxes.size(); ++i) {
                NestBox nestBox = this.nestBoxes.get(i);
                nestBox.save(output);
            }
        }
    }

    public boolean addMetaEgg(IsoAnimal animal) {
        for (int i = 0; i < this.getMaxNestBox() + 1; ++i) {
            if (this.nestBoxes.get((Object)Integer.valueOf((int)i)).animal != null || this.nestBoxes.get((Object)Integer.valueOf((int)i)).eggs.size() >= 10) continue;
            this.nestBoxes.get(i).addEgg(animal.createEgg());
            return true;
        }
        return false;
    }

    public boolean isSlave() {
        return this.linkedX > 0 && this.linkedY > 0;
    }

    @Override
    public String getObjectName() {
        return "IsoHutch";
    }

    @Override
    public void addToWorld() {
        super.addToWorld();
        if (!this.isSlave() && !HutchManager.getInstance().checkHutchExistInList(this)) {
            HutchManager.getInstance().add(this);
        }
    }

    public void removeHutch() {
        for (int x = this.square.x - 2; x < this.square.x + 3; ++x) {
            for (int y = this.square.y - 2; y < this.square.y + 3; ++y) {
                IsoGridSquare sq = this.square.getCell().getGridSquare(x, y, this.square.z);
                if (sq == null) continue;
                ArrayList<IsoHutch> hutch = sq.getHutchTiles();
                for (int i = 0; i < hutch.size(); ++i) {
                    hutch.get(i).releaseAllAnimals();
                    hutch.get(i).dropAllEggs();
                    hutch.get(i).removeFromWorld();
                    hutch.get(i).getSquare().transmitRemoveItemFromSquare(hutch.get(i));
                }
            }
        }
    }

    @Override
    public void removeFromWorld() {
        super.removeFromWorld();
        HutchManager.getInstance().remove(this);
    }

    public void dropAllEggs() {
        for (NestBox nestBox : this.nestBoxes.values()) {
            for (int i = 0; i < nestBox.eggs.size(); ++i) {
                this.square.AddWorldInventoryItem(nestBox.eggs.get(i), Rand.Next(0.0f, 0.8f), Rand.Next(0.0f, 0.8f), 0.0f);
            }
        }
    }

    public void releaseAllAnimals() {
        if (this.animalInside.isEmpty()) {
            return;
        }
        ArrayList<IsoAnimal> animals = new ArrayList<IsoAnimal>();
        for (IsoAnimal animal : this.animalInside.values()) {
            animals.add(animal);
        }
        for (int i = 0; i < animals.size(); ++i) {
            this.releaseAnimal(null, (IsoAnimal)animals.get(i));
        }
    }

    public HashMap<Integer, IsoAnimal> getAnimalInside() {
        return this.animalInside;
    }

    public IsoAnimal getAnimal(Integer index) {
        return this.animalInside.get(index);
    }

    public IsoDeadBody getDeadBody(Integer index) {
        return this.deadBodiesInside.get(index);
    }

    public int getMaxAnimals() {
        if (this.maxAnimals == 0) {
            this.maxAnimals = this.def.rawgetInt("maxAnimals");
        }
        return this.maxAnimals;
    }

    public int getMaxNestBox() {
        if (this.maxNestBox == 0) {
            this.maxNestBox = this.def.rawgetInt("maxNestBox");
        }
        return this.maxNestBox;
    }

    public int getEnterSpotX() {
        if (this.enterSpotX == 0) {
            this.enterSpotX = this.def.rawgetInt("enterSpotX");
        }
        return this.enterSpotX;
    }

    public int getEnterSpotY() {
        if (this.enterSpotY == 0) {
            this.enterSpotY = this.def.rawgetInt("enterSpotY");
        }
        return this.enterSpotY;
    }

    public boolean haveEggHatchDoor() {
        return !StringUtils.isNullOrEmpty(this.def.rawgetStr("openHatchSprite"));
    }

    public boolean isEggHatchDoorOpen() {
        return this.openEggHatch;
    }

    public boolean isEggHatchDoorClosed() {
        return !this.openEggHatch;
    }

    public IsoGridSquare getEntrySq() {
        return this.getSquare().getCell().getGridSquare(this.getSquare().x + this.getEnterSpotX(), this.getSquare().y + this.getEnterSpotY(), this.getSquare().z);
    }

    public IsoAnimal getAnimalInNestBox(Integer index) {
        if (this.nestBoxes.get(index) != null) {
            return this.nestBoxes.get((Object)index).animal;
        }
        return null;
    }

    public NestBox getNestBox(Integer index) {
        return this.nestBoxes.get(index);
    }

    public float getHutchDirt() {
        return this.hutchDirt;
    }

    public void setHutchDirt(float hutchDirt) {
        this.hutchDirt = hutchDirt;
    }

    public float getNestBoxDirt() {
        return this.nestBoxDirt;
    }

    public void setNestBoxDirt(float nestBoxDirt) {
        this.nestBoxDirt = nestBoxDirt;
    }

    public boolean isDoorClosed() {
        return !this.open;
    }

    public boolean isAllDoorClosed() {
        return !this.open && !this.openEggHatch;
    }

    public boolean isOwner() {
        return !GameClient.client;
    }

    public void tryRemoveAnimalFromWorld(IsoAnimal animal) {
        if (GameClient.client && animal != null && animal.isExistInTheWorld()) {
            this.animalOutside.remove(animal);
            animal.removeFromSquare();
            animal.removeFromWorld();
        }
    }

    @UsedFromLua
    public class NestBox {
        public IsoAnimal animal;
        ArrayList<Food> eggs;
        public static final int maxEggs = 10;
        final int index;

        public NestBox(IsoHutch this$0, int index) {
            Objects.requireNonNull(this$0);
            this.eggs = new ArrayList();
            this.index = index;
        }

        public int getIndex() {
            return this.index;
        }

        public int getEggsNb() {
            return this.eggs.size();
        }

        public void addEgg(Food egg) {
            this.eggs.add(egg);
        }

        public Food getEgg(int index) {
            return this.eggs.get(index);
        }

        public Food removeEgg(int index) {
            return this.eggs.remove(index);
        }

        void save(ByteBuffer output) throws IOException {
            output.put((byte)this.eggs.size());
            for (int i = 0; i < this.eggs.size(); ++i) {
                Food egg = this.eggs.get(i);
                egg.saveWithSize(output, false);
            }
            output.put(this.animal != null ? (byte)1 : 0);
            if (this.animal != null) {
                this.animal.save(output, false, false);
            }
        }

        void load(ByteBuffer input, int worldVersion) throws IOException {
            boolean nestBoxHasAnimal;
            int numEggs = input.get();
            for (int i = 0; i < numEggs; ++i) {
                InventoryItem egg = InventoryItem.loadItem(input, worldVersion);
                if (!(egg instanceof Food)) continue;
                Food food = (Food)egg;
                this.eggs.add(food);
            }
            boolean bl = nestBoxHasAnimal = input.get() != 0;
            if (nestBoxHasAnimal) {
                this.animal = new IsoAnimal(IsoWorld.instance.getCell());
                this.animal.load(input, worldVersion);
                this.animal.nestBox = this.index;
            }
        }
    }

    class AgeComparator
    implements Comparator<IsoAnimal> {
        AgeComparator(IsoHutch this$0) {
            Objects.requireNonNull(this$0);
        }

        @Override
        public int compare(IsoAnimal a, IsoAnimal b) {
            return b.getData().getAge() - a.getData().getAge();
        }
    }
}

