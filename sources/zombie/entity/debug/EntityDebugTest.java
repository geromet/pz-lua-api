/*
 * Decompiled with CFR 0.152.
 */
package zombie.entity.debug;

import java.util.ArrayList;
import zombie.UsedFromLua;
import zombie.debug.DebugLog;
import zombie.entity.ComponentType;
import zombie.entity.GameEntityFactory;
import zombie.entity.components.crafting.CraftLogic;
import zombie.entity.components.spriteconfig.SpriteConfigManager;
import zombie.entity.debug.EntityDebugTestType;
import zombie.iso.IsoDirections;
import zombie.iso.IsoGridSquare;
import zombie.iso.IsoObject;
import zombie.scripting.ScriptManager;
import zombie.scripting.entity.GameEntityScript;

@UsedFromLua
public abstract class EntityDebugTest {
    private static final ArrayList<EntityDebugTest> entityDebugTests = new ArrayList();
    private static final boolean B_BUILD_PIPES = true;
    private static final boolean B_BUILD_WIRES = true;
    private static final String tilePipeEastWest = "industry_02_226";
    private static final String tilePipeNorthSouth = "industry_02_224";
    private static final String tileWireEastWest = "industry_02_197";
    private static final String tileWireNorthSouth = "industry_02_198";

    public static void CreateTest(EntityDebugTestType type, IsoGridSquare square) {
        DebugLog.General.println("Creating Entity Meta Test: " + String.valueOf((Object)type));
        if (square == null) {
            DebugLog.General.warn("Square is null");
            return;
        }
        BaseTest test = null;
        switch (type) {
            case BaseTest: {
                test = new BaseTest();
                break;
            }
        }
        if (test != null) {
            ((EntityDebugTest)test).create(square);
            entityDebugTests.add(test);
        } else {
            DebugLog.General.warn("Test is null.");
        }
    }

    public static void Update() {
        for (int i = 0; i < entityDebugTests.size(); ++i) {
            entityDebugTests.get(i).update();
        }
    }

    public static void Reset() {
        entityDebugTests.clear();
    }

    public abstract void create(IsoGridSquare var1);

    public abstract void update();

    protected IsoObject createEntity(IsoGridSquare square, String scriptName) {
        GameEntityScript script = ScriptManager.instance.getGameEntityScript(scriptName);
        SpriteConfigManager.ObjectInfo objectInfo = SpriteConfigManager.GetObjectInfo(scriptName);
        String spriteName = objectInfo.getFace("single").getTileInfo(0, 0, 0).getSpriteName();
        IsoObject isoObject = new IsoObject(square, spriteName, scriptName);
        GameEntityFactory.CreateIsoObjectEntity(isoObject, script, true);
        square.AddSpecialObject(isoObject);
        return isoObject;
    }

    protected IsoObject createDummyObject(IsoGridSquare square, String spriteName) {
        IsoObject isoObject = new IsoObject(square, spriteName, "DummyObject");
        square.AddSpecialObject(isoObject);
        return isoObject;
    }

    protected IsoGridSquare createPipes(IsoGridSquare square, IsoDirections dir, int tileCnt, boolean doBuild) {
        return this.createUtility(square, dir, tileCnt, doBuild, tilePipeNorthSouth, tilePipeEastWest);
    }

    protected IsoGridSquare createWires(IsoGridSquare square, IsoDirections dir, int tileCnt, boolean doBuild) {
        return this.createUtility(square, dir, tileCnt, doBuild, tileWireNorthSouth, tileWireEastWest);
    }

    protected IsoGridSquare createUtility(IsoGridSquare square, IsoDirections dir, int tileCnt, boolean doBuild, String north, String east) {
        for (int i = 0; i < tileCnt; ++i) {
            square = square.getAdjacentSquare(dir);
            if (!doBuild) continue;
            if (dir == IsoDirections.N || dir == IsoDirections.S) {
                this.createDummyObject(square, north);
                continue;
            }
            this.createDummyObject(square, east);
        }
        return square.getAdjacentSquare(dir);
    }

    protected boolean isObjectConnected(IsoObject object, IsoDirections dir, boolean isWires) {
        IsoGridSquare square = object.getSquare();
        square = square.getAdjacentSquare(dir);
        if (isWires) {
            return this.squareContainsSprite(square, tileWireNorthSouth) || this.squareContainsSprite(square, tileWireEastWest);
        }
        return this.squareContainsSprite(square, tilePipeNorthSouth) || this.squareContainsSprite(square, tilePipeEastWest);
    }

    protected boolean squareContainsSprite(IsoGridSquare square, String testSprite) {
        if (square.getSpecialObjects() != null) {
            for (int i = 0; i < square.getSpecialObjects().size(); ++i) {
                IsoObject obj = square.getSpecialObjects().get(i);
                if (obj.getSpriteName() == null || !obj.getSpriteName().equals(testSprite)) continue;
                return true;
            }
        }
        return false;
    }

    protected boolean isRunning(IsoObject object) {
        CraftLogic craftGrid = (CraftLogic)object.getComponent(ComponentType.CraftLogic);
        return craftGrid != null;
    }

    public static class BaseTest
    extends EntityDebugTest {
        private IsoObject waterSource;

        @Override
        public void create(IsoGridSquare square) {
        }

        @Override
        public void update() {
        }
    }
}

