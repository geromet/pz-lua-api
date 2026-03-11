/*
 * Decompiled with CFR 0.152.
 */
package zombie.debug;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import zombie.UsedFromLua;
import zombie.entity.ComponentType;
import zombie.scripting.ScriptManager;
import zombie.scripting.entity.components.fluids.FluidContainerScript;
import zombie.scripting.objects.Item;

@UsedFromLua
public class DebugCSVExportFluidContainers {
    public static void doCSV() throws IOException {
        try (PrintWriter writer = new PrintWriter(new FileWriter("FluidContainersCSV.txt"));){
            writer.println("CONTAINER ID,CAPACITY");
            for (Item item : ScriptManager.instance.getAllItems()) {
                if (!item.containsComponent(ComponentType.FluidContainer)) continue;
                writer.println("%s.%s,%s".formatted(item.getModuleName(), item.getName(), Float.valueOf(((FluidContainerScript)item.getComponentScriptFor(ComponentType.FluidContainer)).getCapacity())));
            }
        }
    }
}

