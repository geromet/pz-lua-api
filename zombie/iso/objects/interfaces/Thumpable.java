/*
 * Decompiled with CFR 0.152.
 */
package zombie.iso.objects.interfaces;

import zombie.characters.IsoGameCharacter;
import zombie.inventory.types.HandWeapon;
import zombie.iso.IsoMovingObject;

public interface Thumpable {
    public boolean isDestroyed();

    public void Thump(IsoMovingObject var1);

    public void WeaponHit(IsoGameCharacter var1, HandWeapon var2);

    public Thumpable getThumpableFor(IsoGameCharacter var1);

    public float getThumpCondition();
}

