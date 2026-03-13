/*
 * Decompiled with CFR 0.152.
 */
package zombie.ai.astar;

import zombie.ai.astar.Mover;
import zombie.ai.astar.Path;

public interface IPathfinder {
    public void Failed(Mover var1);

    public void Succeeded(Path var1, Mover var2);

    public String getName();
}

