/*
 * Decompiled with CFR 0.152.
 */
package zombie.characters.animals;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import zombie.GameWindow;
import zombie.UsedFromLua;
import zombie.characters.animals.AnimalAllele;
import zombie.characters.animals.AnimalGenomeDefinitions;
import zombie.characters.animals.IsoAnimal;
import zombie.characters.animals.datas.AnimalBreed;
import zombie.core.random.Rand;
import zombie.debug.DebugLog;
import zombie.util.StringUtils;

@UsedFromLua
public class AnimalGene {
    public String name;
    public int id = Rand.Next(1000000);
    public AnimalAllele allele1;
    public AnimalAllele allele2;

    public void save(ByteBuffer output, boolean isDebugSave) throws IOException {
        output.putInt(this.id);
        GameWindow.WriteString(output, this.name);
        this.allele1.save(output, isDebugSave);
        this.allele2.save(output, isDebugSave);
    }

    public void load(ByteBuffer input, int worldVersion, boolean isDebugSave) throws IOException {
        this.id = input.getInt();
        this.name = GameWindow.ReadString(input);
        this.allele1 = new AnimalAllele();
        this.allele1.load(input, worldVersion, isDebugSave);
        this.allele2 = new AnimalAllele();
        this.allele2.load(input, worldVersion, isDebugSave);
    }

    public static void initGenome(IsoAnimal animal) {
        AnimalGene geneSet;
        if (animal.adef.genes == null) {
            return;
        }
        ArrayList<String> genes = animal.adef.genes;
        animal.fullGenome = new HashMap();
        HashMap<String, AnimalBreed.ForcedGenes> forcedGenes = animal.getBreed().forcedGenes;
        if (forcedGenes != null) {
            for (String key : forcedGenes.keySet()) {
                String name;
                geneSet = new AnimalGene();
                geneSet.name = name = key.toLowerCase();
                AnimalBreed.ForcedGenes value = forcedGenes.get(name);
                AnimalGenomeDefinitions def = AnimalGenomeDefinitions.fullGenomeDef.get(name);
                if (def == null) {
                    DebugLog.Animal.debugln(name + " wasn't found in AnimalGenomeDefinitions.lua");
                    continue;
                }
                for (int j = 0; j < 2; ++j) {
                    AnimalAllele newGene = AnimalGene.initAllele(name, true, Rand.Next(value.minValue, value.maxValue), def.forcedValues);
                    if (j == 0) {
                        geneSet.allele1 = newGene;
                        continue;
                    }
                    geneSet.allele2 = newGene;
                }
                geneSet.initUsedGene();
                animal.fullGenome.put(name, geneSet);
            }
        }
        for (int i = 0; i < genes.size(); ++i) {
            String name;
            geneSet = new AnimalGene();
            geneSet.name = name = genes.get(i).toLowerCase();
            if (animal.fullGenome.containsKey(name)) continue;
            AnimalGenomeDefinitions def = AnimalGenomeDefinitions.fullGenomeDef.get(name);
            if (def == null) {
                DebugLog.Animal.debugln(name + " wasn't found in AnimalGenomeDefinitions.lua");
                continue;
            }
            for (int j = 0; j < 2; ++j) {
                AnimalAllele allele = AnimalGene.initAllele(name, false, Rand.Next(def.minValue, def.maxValue), def.forcedValues);
                if (j == 0) {
                    geneSet.allele1 = allele;
                    continue;
                }
                geneSet.allele2 = allele;
            }
            geneSet.initUsedGene();
            animal.fullGenome.put(name, geneSet);
        }
        for (String s : animal.fullGenome.keySet()) {
            AnimalGene gene = animal.fullGenome.get(s);
            AnimalGenomeDefinitions def = AnimalGenomeDefinitions.fullGenomeDef.get(gene.name);
            AnimalAllele baseGene = gene.allele1;
            if (gene.allele2.used) {
                baseGene = gene.allele2;
            }
            AnimalGene.doRatio(def, animal.fullGenome, baseGene);
        }
    }

    public void initUsedGene() {
        int nb = this.allele1.dominant ? (this.allele2.dominant ? Rand.Next(2) : 0) : (this.allele2.dominant ? (this.allele1.dominant ? Rand.Next(2) : 1) : Rand.Next(2));
        if (nb == 0) {
            this.allele1.used = true;
        } else {
            this.allele2.used = true;
        }
    }

    private static AnimalAllele initAllele(String name, boolean forcedDominant, float value, boolean forcedValues) {
        if (value < 0.0f) {
            value = 0.0f;
        }
        AnimalAllele newAllele = new AnimalAllele();
        if (forcedDominant) {
            newAllele.dominant = true;
        } else {
            if (!forcedValues && Rand.Next(100) < 15) {
                value = Rand.Next(0.0f, 1.0f);
            }
            float avgDist = Math.abs(value - 0.5f);
            int recChance = 30;
            if ((double)avgDist > 0.45) {
                recChance = 90;
            } else if ((double)avgDist > 0.4) {
                recChance = 80;
            } else if ((double)avgDist > 0.3) {
                recChance = 75;
            } else if ((double)avgDist > 0.2) {
                recChance = 60;
            } else if ((double)avgDist > 0.15) {
                recChance = 50;
            } else if ((double)avgDist > 0.1) {
                recChance = 40;
            }
            newAllele.dominant = Rand.Next(100) > recChance;
        }
        newAllele.name = name;
        newAllele.currentValue = value;
        AnimalGene.doMutation(newAllele);
        return newAllele;
    }

    public static void doRatio(AnimalGenomeDefinitions def, HashMap<String, AnimalGene> fullGenome, AnimalAllele allele) {
        if (def.ratios == null) {
            return;
        }
        for (String s : def.ratios.keySet()) {
            String name = s.toLowerCase();
            Float value = def.ratios.get(name);
            AnimalGene affectedGenes = fullGenome.get(name);
            if (affectedGenes == null) {
                DebugLog.Animal.debugln("RATIO CALC: " + name + " wasn't found in animal genome but define in animal's genes ratio");
                continue;
            }
            float newValue = value.floatValue() - allele.currentValue;
            affectedGenes.allele1.trueRatioValue = Math.max(Rand.Next(0.01f, 0.05f), affectedGenes.allele1.currentValue + newValue);
            affectedGenes.allele2.trueRatioValue = Math.max(Rand.Next(0.01f, 0.05f), affectedGenes.allele2.currentValue + newValue);
        }
    }

    public static HashMap<String, AnimalGene> initGenesFromParents(HashMap<String, AnimalGene> femaleGenome, HashMap<String, AnimalGene> maleGenome) {
        AnimalGenomeDefinitions def;
        AnimalGene gene;
        HashMap<String, AnimalGene> fullGenome = new HashMap<String, AnimalGene>();
        if (maleGenome == null || maleGenome.isEmpty()) {
            maleGenome = femaleGenome;
        }
        for (String name : femaleGenome.keySet()) {
            AnimalGene femaleGenes = femaleGenome.get(name);
            AnimalGene maleGenes = maleGenome.get(name);
            if (maleGenes == null) {
                maleGenes = femaleGenes;
            }
            if (femaleGenes == null) {
                femaleGenes = maleGenes;
            }
            AnimalAllele alleleFemale = Rand.NextBool(2) ? femaleGenes.allele1 : femaleGenes.allele2;
            AnimalAllele alleleMale = Rand.NextBool(2) ? maleGenes.allele1 : maleGenes.allele2;
            AnimalGene gene2 = new AnimalGene();
            gene2.name = name;
            gene2.allele1 = new AnimalAllele();
            gene2.allele1.currentValue = alleleFemale.currentValue;
            gene2.allele1.dominant = alleleFemale.dominant;
            gene2.allele1.name = alleleFemale.name;
            gene2.allele1.geneticDisorder = alleleFemale.geneticDisorder;
            gene2.allele2 = new AnimalAllele();
            gene2.allele2.currentValue = alleleMale.currentValue;
            gene2.allele2.dominant = alleleMale.dominant;
            gene2.allele2.name = alleleMale.name;
            gene2.allele2.geneticDisorder = alleleMale.geneticDisorder;
            gene2.initUsedGene();
            fullGenome.put(name, gene2);
        }
        for (String name : fullGenome.keySet()) {
            gene = (AnimalGene)fullGenome.get(name);
            def = AnimalGenomeDefinitions.fullGenomeDef.get(gene.name);
            AnimalGene.doMutation(gene.allele1);
            AnimalGene.doMutation(gene.allele2);
        }
        for (String name : fullGenome.keySet()) {
            gene = fullGenome.get(name);
            def = AnimalGenomeDefinitions.fullGenomeDef.get(gene.name);
            AnimalAllele baseGene = gene.allele1;
            if (gene.allele2.used) {
                baseGene = gene.allele2;
            }
            AnimalGene.doRatio(def, fullGenome, baseGene);
        }
        return fullGenome;
    }

    public static void checkGeneticDisorder(IsoAnimal animal) {
        for (int i = 0; i < animal.getFullGenomeList().size(); ++i) {
            AnimalGene gene = animal.getFullGenomeList().get(i);
            if (StringUtils.isNullOrEmpty(gene.allele1.geneticDisorder) || !gene.allele1.geneticDisorder.equals(gene.allele2.geneticDisorder) || animal.geneticDisorder.contains(gene.allele1.geneticDisorder)) continue;
            animal.geneticDisorder.add(gene.allele1.geneticDisorder);
        }
    }

    public static void doMutation(AnimalAllele allele) {
        if (Rand.Next(100) <= 10) {
            allele.currentValue = allele.currentValue + (Rand.NextBool(2) ? 0.05f : -0.05f);
        }
        if (Rand.Next(100) <= 5) {
            allele.currentValue += 0.2f;
        }
        if (Rand.Next(100) <= 5) {
            boolean bl = allele.dominant = !allele.dominant;
        }
        if (StringUtils.isNullOrEmpty(allele.geneticDisorder) && Rand.Next(100) <= 2) {
            allele.geneticDisorder = AnimalGenomeDefinitions.geneticDisorder.get(Rand.Next(0, AnimalGenomeDefinitions.geneticDisorder.size()));
        }
        if (Rand.Next(100) <= 2) {
            allele.geneticDisorder = null;
        }
        if (allele.currentValue <= 0.05f) {
            allele.currentValue = 0.05f;
        }
    }

    public String getName() {
        return this.name;
    }

    public AnimalAllele getAllele1() {
        return this.allele1;
    }

    public AnimalAllele getAllele2() {
        return this.allele2;
    }

    public AnimalAllele getUsedGene() {
        if (this.allele1.used) {
            return this.allele1;
        }
        return this.allele2;
    }
}

