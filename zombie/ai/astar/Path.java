/*
 * Decompiled with CFR 0.152.
 */
package zombie.ai.astar;

import java.util.ArrayList;
import java.util.Stack;

public class Path {
    private final ArrayList<Step> steps = new ArrayList();
    public float cost;
    public static Stack<Step> stepstore = new Stack();
    static Step containsStep = new Step();

    public float costPerStep() {
        if (this.steps.isEmpty()) {
            return this.cost;
        }
        return this.cost / (float)this.steps.size();
    }

    public void appendStep(int x, int y, int z) {
        Step step = new Step();
        step.x = x;
        step.y = y;
        step.z = z;
        this.steps.add(step);
    }

    public boolean contains(int x, int y, int z) {
        Path.containsStep.x = x;
        Path.containsStep.y = y;
        Path.containsStep.z = z;
        return this.steps.contains(containsStep);
    }

    public int getLength() {
        return this.steps.size();
    }

    public Step getStep(int index) {
        return this.steps.get(index);
    }

    public int getX(int index) {
        return this.getStep((int)index).x;
    }

    public int getY(int index) {
        return this.getStep((int)index).y;
    }

    public int getZ(int index) {
        return this.getStep((int)index).z;
    }

    public static Step createStep() {
        if (stepstore.isEmpty()) {
            for (int n = 0; n < 200; ++n) {
                Step step = new Step();
                stepstore.push(step);
            }
        }
        return stepstore.push(containsStep);
    }

    public void prependStep(int x, int y, int z) {
        Step step = new Step();
        step.x = x;
        step.y = y;
        step.z = z;
        this.steps.add(0, step);
    }

    public static class Step {
        public int x;
        public int y;
        public int z;

        public Step(int x, int y, int z) {
            this.x = x;
            this.y = y;
            this.z = z;
        }

        public Step() {
        }

        public boolean equals(Object other) {
            if (other instanceof Step) {
                Step o = (Step)other;
                return o.x == this.x && o.y == this.y && o.z == this.z;
            }
            return false;
        }

        public int getX() {
            return this.x;
        }

        public int getY() {
            return this.y;
        }

        public int getZ() {
            return this.z;
        }

        public int hashCode() {
            return this.x * this.y * this.z;
        }
    }
}

