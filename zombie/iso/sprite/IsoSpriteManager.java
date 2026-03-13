/*
 * Decompiled with CFR 0.152.
 */
package zombie.iso.sprite;

import gnu.trove.map.hash.TIntObjectHashMap;
import java.util.HashMap;
import zombie.UsedFromLua;
import zombie.core.Color;
import zombie.debug.DebugType;
import zombie.iso.SpriteDetails.IsoFlagType;
import zombie.iso.sprite.IsoAnim;
import zombie.iso.sprite.IsoSprite;

@UsedFromLua
public final class IsoSpriteManager {
    public static final IsoSpriteManager instance = new IsoSpriteManager();
    public final HashMap<String, IsoSprite> namedMap = new HashMap();
    public final TIntObjectHashMap<IsoSprite> intMap = new TIntObjectHashMap();
    private final IsoSprite emptySprite;

    public IsoSpriteManager() {
        IsoSprite sprite = this.emptySprite = new IsoSprite(this);
        sprite.name = "";
        sprite.id = -1;
        sprite.properties.set(IsoFlagType.invisible);
        this.namedMap.put(sprite.name, sprite);
    }

    public void Dispose() {
        IsoSprite.DisposeAll();
        IsoAnim.DisposeAll();
        Object[] values2 = this.intMap.values();
        for (int i = 0; i < values2.length; ++i) {
            IsoSprite sprite = (IsoSprite)values2[i];
            sprite.Dispose();
            sprite.def = null;
            sprite.parentManager = null;
        }
        this.intMap.clear();
        this.namedMap.clear();
        this.namedMap.put(this.emptySprite.name, this.emptySprite);
    }

    public IsoSprite getSprite(int gid) {
        if (this.intMap.containsKey(gid)) {
            return this.intMap.get(gid);
        }
        return null;
    }

    public IsoSprite getSprite(String gid) {
        if (this.namedMap.containsKey(gid)) {
            return this.namedMap.get(gid);
        }
        return this.AddSprite(gid);
    }

    public IsoSprite getOrAddSpriteCache(String tex) {
        if (this.namedMap.containsKey(tex)) {
            return this.namedMap.get(tex);
        }
        IsoSprite spr = new IsoSprite(this);
        spr.LoadSingleTexture(tex);
        this.namedMap.put(tex, spr);
        return spr;
    }

    public IsoSprite getOrAddSpriteCache(String tex, Color col) {
        int r = (int)(col.r * 255.0f);
        int g = (int)(col.g * 255.0f);
        int b = (int)(col.b * 255.0f);
        String key = tex + "_" + r + "_" + g + "_" + b;
        if (this.namedMap.containsKey(key)) {
            return this.namedMap.get(key);
        }
        IsoSprite spr = new IsoSprite(this);
        spr.LoadSingleTexture(tex);
        this.namedMap.put(key, spr);
        return spr;
    }

    public IsoSprite AddSprite(String tex) {
        IsoSprite spr = new IsoSprite(this);
        spr.LoadSingleTexture(tex);
        this.namedMap.put(tex, spr);
        return spr;
    }

    public IsoSprite AddSprite(String tex, int id) {
        IsoSprite spr = new IsoSprite(this);
        spr.LoadSingleTexture(tex);
        if (this.namedMap.containsKey(tex)) {
            DebugType.Sprite.warn("duplicate texture " + tex + " ignore ID=" + id + ", use ID=" + this.namedMap.get((Object)tex).id);
            id = this.namedMap.get((Object)tex).id;
        }
        this.namedMap.put(tex, spr);
        spr.id = id;
        this.intMap.put(id, spr);
        return spr;
    }
}

