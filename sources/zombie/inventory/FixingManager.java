/*
 * Decompiled with CFR 0.152.
 */
package zombie.inventory;

import java.util.ArrayList;
import zombie.UsedFromLua;
import zombie.characters.IsoGameCharacter;
import zombie.characters.IsoPlayer;
import zombie.characters.skills.PerkFactory;
import zombie.core.random.Rand;
import zombie.debug.DebugLog;
import zombie.inventory.InventoryItem;
import zombie.inventory.InventoryItemFactory;
import zombie.inventory.types.DrainableComboItem;
import zombie.inventory.types.HandWeapon;
import zombie.inventory.types.WeaponPart;
import zombie.network.GameClient;
import zombie.network.GameServer;
import zombie.scripting.ScriptManager;
import zombie.scripting.objects.Fixing;

@UsedFromLua
public final class FixingManager {
    public static ArrayList<Fixing> getFixes(InventoryItem item) {
        ArrayList<Fixing> result = new ArrayList<Fixing>();
        ArrayList<Fixing> allFixing = ScriptManager.instance.getAllFixing(new ArrayList<Fixing>());
        for (int i = 0; i < allFixing.size(); ++i) {
            Fixing testFix = (Fixing)allFixing.get(i);
            if (!testFix.getRequiredItem().contains(item.getFullType())) continue;
            result.add(testFix);
        }
        return result;
    }

    public static InventoryItem fixItem(InventoryItem brokenItem, IsoGameCharacter chr, Fixing fixing, Fixing.Fixer fixer) {
        if ((double)Rand.Next(100) >= FixingManager.getChanceOfFail(brokenItem, chr, fixing, fixer)) {
            FixingManager.addXp(chr, fixer);
            double condRepaired = FixingManager.getCondRepaired(brokenItem, chr, fixing, fixer);
            int missedTotalCond = brokenItem.getConditionMax() - brokenItem.getCondition();
            double newCond = (double)missedTotalCond * (condRepaired / 100.0);
            int newCondInt = (int)Math.round(newCond);
            if (newCondInt == 0) {
                newCondInt = 1;
            }
            DebugLog.Action.debugln("Fix item \"%s\" id=%d condition=%d new-condition=%d missed=%d repaired=%f", brokenItem.getDisplayName(), brokenItem.getID(), brokenItem.getCondition(), newCondInt, missedTotalCond, condRepaired);
            brokenItem.setConditionNoSound(brokenItem.getCondition() + newCondInt);
            brokenItem.setHaveBeenRepaired(brokenItem.getHaveBeenRepaired() + 1);
            brokenItem.syncItemFields();
        } else if (brokenItem.getCondition() > 0) {
            brokenItem.setCondition(brokenItem.getCondition() - 1);
            brokenItem.syncItemFields();
            chr.getEmitter().playSound("FixingItemFailed");
        }
        FixingManager.useFixer(chr, fixer, brokenItem);
        if (fixing.getGlobalItem() != null) {
            FixingManager.useFixer(chr, fixing.getGlobalItem(), brokenItem);
        }
        return brokenItem;
    }

    private static void addXp(IsoGameCharacter chr, Fixing.Fixer fixer) {
        if (fixer.getFixerSkills() == null) {
            return;
        }
        for (int i = 0; i < fixer.getFixerSkills().size(); ++i) {
            Fixing.FixerSkill skill = fixer.getFixerSkills().get(i);
            int exp = Rand.Next(3, 6);
            if (GameServer.server) {
                GameServer.addXp((IsoPlayer)chr, PerkFactory.Perks.FromString(skill.getSkillName()), exp);
                continue;
            }
            if (GameClient.client) continue;
            chr.getXp().AddXP(PerkFactory.Perks.FromString(skill.getSkillName()), (float)exp);
        }
    }

    public static void useFixer(IsoGameCharacter chr, Fixing.Fixer fixer, InventoryItem brokenItem) {
        int numberOfUse = fixer.getNumberOfUse();
        for (int j = 0; j < chr.getInventory().getItems().size(); ++j) {
            if (brokenItem == chr.getInventory().getItems().get(j)) continue;
            InventoryItem item = chr.getInventory().getItems().get(j);
            if (item != null && item.getFullType().equals(fixer.getFixerName())) {
                if (item instanceof DrainableComboItem) {
                    if ("DuctTape".equals(item.getType()) || "Scotchtape".equals(item.getType())) {
                        chr.getEmitter().playSound("FixWithTape");
                    }
                    int uses = item.getCurrentUses();
                    count = Math.min(uses, numberOfUse);
                    for (int i = 0; i < count; ++i) {
                        item.UseAndSync();
                        --numberOfUse;
                        if (chr.getInventory().getItems().contains(item)) continue;
                        --j;
                        break;
                    }
                } else {
                    if (item instanceof HandWeapon) {
                        HandWeapon weapon = (HandWeapon)item;
                        if (chr.getSecondaryHandItem() == item) {
                            chr.setSecondaryHandItem(null);
                        }
                        if (chr.getPrimaryHandItem() == item) {
                            chr.setPrimaryHandItem(null);
                        }
                        for (WeaponPart part : weapon.getAllWeaponParts()) {
                            chr.getInventory().AddItem(part);
                            if (!GameServer.server) continue;
                            GameServer.sendAddItemToContainer(chr.getInventory(), part);
                        }
                        count = 0;
                        if (weapon.getMagazineType() != null && weapon.isContainsClip()) {
                            Object newMag = InventoryItemFactory.CreateItem(weapon.getMagazineType());
                            ((InventoryItem)newMag).setCurrentAmmoCount(weapon.getCurrentAmmoCount());
                            chr.getInventory().AddItem((InventoryItem)newMag);
                            if (GameServer.server) {
                                GameServer.sendAddItemToContainer(chr.getInventory(), newMag);
                            }
                        } else if (weapon.getCurrentAmmoCount() > 0) {
                            count += weapon.getCurrentAmmoCount();
                        }
                        if (weapon.haveChamber() && weapon.isRoundChambered()) {
                            ++count;
                        }
                        if (count > 0) {
                            for (int i = 0; i < count; ++i) {
                                Object newBullet = InventoryItemFactory.CreateItem(weapon.getAmmoType());
                                chr.getInventory().AddItem((InventoryItem)newBullet);
                                if (!GameServer.server) continue;
                                GameServer.sendAddItemToContainer(chr.getInventory(), newBullet);
                            }
                        }
                    }
                    chr.getInventory().Remove(item);
                    if (GameServer.server) {
                        GameServer.sendRemoveItemFromContainer(chr.getInventory(), item);
                    }
                    --j;
                    --numberOfUse;
                }
            }
            if (numberOfUse == 0) break;
        }
    }

    public static double getChanceOfFail(InventoryItem brokenItem, IsoGameCharacter chr, Fixing fixing, Fixing.Fixer fixer) {
        double result = 3.0;
        if (fixer.getFixerSkills() != null) {
            for (int i = 0; i < fixer.getFixerSkills().size(); ++i) {
                if (chr.getPerkLevel(PerkFactory.Perks.FromString(fixer.getFixerSkills().get(i).getSkillName())) < fixer.getFixerSkills().get(i).getSkillLevel()) {
                    result += (double)((fixer.getFixerSkills().get(i).getSkillLevel() - chr.getPerkLevel(PerkFactory.Perks.FromString(fixer.getFixerSkills().get(i).getSkillName()))) * 30);
                    continue;
                }
                result -= (double)((chr.getPerkLevel(PerkFactory.Perks.FromString(fixer.getFixerSkills().get(i).getSkillName())) - fixer.getFixerSkills().get(i).getSkillLevel()) * 5);
            }
        }
        if ((result += (double)((brokenItem.getHaveBeenRepaired() + 1) * 2)) > 100.0) {
            result = 100.0;
        }
        if (result < 0.0) {
            result = 0.0;
        }
        return result;
    }

    public static double getCondRepaired(InventoryItem brokenItem, IsoGameCharacter chr, Fixing fixing, Fixing.Fixer fixer) {
        double result;
        switch (fixing.getFixers().indexOf(fixer)) {
            case 0: {
                double d = 50.0 * (1.0 / (double)(brokenItem.getHaveBeenRepaired() + 1));
                break;
            }
            case 1: {
                double d = 20.0 * (1.0 / (double)(brokenItem.getHaveBeenRepaired() + 1));
                break;
            }
            default: {
                double d = result = 10.0 * (1.0 / (double)(brokenItem.getHaveBeenRepaired() + 1));
            }
        }
        if (fixer.getFixerSkills() != null) {
            for (int i = 0; i < fixer.getFixerSkills().size(); ++i) {
                Fixing.FixerSkill fixerSkill = fixer.getFixerSkills().get(i);
                int perkLevel = chr.getPerkLevel(PerkFactory.Perks.FromString(fixerSkill.getSkillName()));
                if (perkLevel > fixerSkill.getSkillLevel()) {
                    result += (double)Math.min((perkLevel - fixerSkill.getSkillLevel()) * 5, 25);
                    continue;
                }
                result -= (double)((fixerSkill.getSkillLevel() - perkLevel) * 15);
            }
        }
        result *= (double)fixing.getConditionModifier();
        result = Math.max(0.0, result);
        result = Math.min(100.0, result);
        return result;
    }
}

