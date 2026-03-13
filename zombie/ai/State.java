/*
 * Decompiled with CFR 0.152.
 */
package zombie.ai;

import com.google.common.base.Predicates;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.function.Supplier;
import org.jspecify.annotations.Nullable;
import zombie.ai.IStateFlagsSource;
import zombie.characters.IsoGameCharacter;
import zombie.characters.MoveDeltaModifiers;
import zombie.core.skinnedmodel.advancedanimation.AnimEvent;
import zombie.core.skinnedmodel.advancedanimation.AnimLayer;
import zombie.core.skinnedmodel.advancedanimation.events.AnimEventBroadcaster;
import zombie.core.skinnedmodel.advancedanimation.events.IAnimEventListener;
import zombie.core.skinnedmodel.advancedanimation.events.IAnimEventWrappedBroadcaster;
import zombie.core.skinnedmodel.animation.AnimationTrack;
import zombie.debug.DebugLog;

public abstract class State
implements IAnimEventListener,
IAnimEventWrappedBroadcaster,
IStateFlagsSource {
    private static final Map<Class<? extends State>, Map<String, Param<?>>> PARAMS_BY_STATE = new IdentityHashMap();
    private final AnimEventBroadcaster animEventBroadcaster = new AnimEventBroadcaster();
    private final boolean isSyncOnEnter;
    private final boolean isSyncOnExit;
    private final boolean isSyncOnSquare;
    private final boolean isSyncInIdle;

    protected State(boolean isSyncOnEnter, boolean isSyncOnExit, boolean isSyncOnSquare, boolean isSyncInIdle) {
        this.isSyncOnEnter = isSyncOnEnter;
        this.isSyncOnExit = isSyncOnExit;
        this.isSyncOnSquare = isSyncOnSquare;
        this.isSyncInIdle = isSyncInIdle;
    }

    public void enter(IsoGameCharacter owner) {
        DebugLog.Multiplayer.noise(this.getName());
    }

    public void execute(IsoGameCharacter owner) {
    }

    public void exit(IsoGameCharacter owner) {
        DebugLog.Multiplayer.noise(this.getName());
    }

    @Override
    public AnimEventBroadcaster getAnimEventBroadcaster() {
        return this.animEventBroadcaster;
    }

    @Override
    public void animEvent(IsoGameCharacter owner, AnimLayer layer, AnimationTrack track, AnimEvent event) {
        this.getAnimEventBroadcaster().animEvent(owner, layer, track, event);
    }

    public void getDeltaModifiers(IsoGameCharacter owner, MoveDeltaModifiers modifiers) {
    }

    public boolean isIgnoreCollide(IsoGameCharacter owner, int fromX, int fromY, int fromZ, int toX, int toY, int toZ) {
        return false;
    }

    public String getName() {
        return this.getClass().getSimpleName();
    }

    public void setParams(IsoGameCharacter owner, Stage stage) {
        DebugLog.Multiplayer.trace("%s %s: %s", new Object[]{this.getName(), stage, this.getParams(owner)});
    }

    @Override
    public final boolean isSyncOnEnter() {
        return this.isSyncOnEnter;
    }

    @Override
    public final boolean isSyncOnExit() {
        return this.isSyncOnExit;
    }

    @Override
    public final boolean isSyncOnSquare() {
        return this.isSyncOnSquare;
    }

    @Override
    public final boolean isSyncInIdle() {
        return this.isSyncInIdle;
    }

    public boolean isProcessedOnEnter() {
        return false;
    }

    public void processOnEnter(IsoGameCharacter owner, Map<Object, Object> delegate) {
    }

    public boolean isProcessedOnExit() {
        return false;
    }

    public void processOnExit(IsoGameCharacter owner, Map<Object, Object> delegate) {
    }

    public Map<Param<?>, Object> getParams(IsoGameCharacter owner) {
        return owner.getStateMachineParams(this.getClass());
    }

    public void loadFrom(Map<Object, Object> delegate, IsoGameCharacter character) {
        Map<String, Param<?>> params = PARAMS_BY_STATE.get(this.getClass());
        for (Map.Entry<Object, Object> entry : delegate.entrySet()) {
            String key;
            Param<?> param = entry.getKey();
            if (!(param instanceof String) || !((param = params.get(key = (String)((Object)param))) instanceof Param)) continue;
            Param<?> param2 = param;
            this.genericsHelper(character, param2, entry.getValue());
        }
    }

    private <T> void genericsHelper(IsoGameCharacter character, Param<T> param, Object value) {
        character.set(param, value);
    }

    public static class Param<T> {
        private final Class<?> fromClazz;
        private final String name;
        private final Class<T> clazz;
        private final Supplier<T> defaultSupplier;

        private Param(String name, Class<T> clazz2, Supplier<T> defaultSupplier) {
            if (defaultSupplier == null) {
                throw new IllegalArgumentException("Param cannot have a null-default-supplier");
            }
            Class callerClass = StackWalker.getInstance(StackWalker.Option.RETAIN_CLASS_REFERENCE).walk(frames -> frames.map(StackWalker.StackFrame::getDeclaringClass).filter(Predicates.not(this.getClass()::equals)).findFirst().orElse(null));
            if (!State.class.isAssignableFrom(callerClass)) {
                throw new IllegalStateException("Unable to determine caller for %s".formatted(name));
            }
            this.fromClazz = callerClass;
            PARAMS_BY_STATE.computeIfAbsent(this.fromClazz, clazz -> new HashMap()).put(name, this);
            this.name = name;
            this.clazz = clazz2;
            this.defaultSupplier = defaultSupplier;
        }

        public static Param<Integer> ofInt(String name, int defaultValue) {
            return Param.of(name, Integer.class, defaultValue);
        }

        public static Param<Long> ofLong(String name, long defaultValue) {
            return Param.of(name, Long.class, defaultValue);
        }

        public static Param<Float> ofFloat(String name, float defaultValue) {
            return Param.of(name, Float.class, Float.valueOf(defaultValue));
        }

        public static Param<String> ofString(String name, @Nullable String defaultValue) {
            return Param.of(name, String.class, defaultValue);
        }

        public static Param<Boolean> ofBool(String name, boolean defaultValue) {
            return Param.of(name, Boolean.class, defaultValue);
        }

        public static <R> Param<R> of(String name, Class<R> clazz) {
            return Param.of(name, clazz, null);
        }

        public static <R> Param<R> of(String name, Class<R> clazz, R defaultValue) {
            return Param.ofSupplier(name, clazz, () -> defaultValue);
        }

        public static <R> Param<R> ofSupplier(String name, Class<R> clazz, Supplier<R> defaultSupplier) {
            return new Param<R>(name, clazz, defaultSupplier);
        }

        public String getName() {
            return this.name;
        }

        public T get(IsoGameCharacter owner) {
            return this.get(owner, this.defaultSupplier);
        }

        public T get(IsoGameCharacter owner, Supplier<T> defaultSupplier) {
            return (T)owner.getStateMachineParams(this.fromClazz).computeIfAbsent(this, param -> defaultSupplier.get());
        }

        public void set(IsoGameCharacter owner, T newT) {
            owner.getStateMachineParams(this.fromClazz).put(this, newT);
        }

        public T remove(IsoGameCharacter owner) {
            return (T)owner.getStateMachineParams(this.fromClazz).remove(this);
        }

        public Class<?> getStateClass() {
            return this.fromClazz;
        }

        public T fromDelegate(Map<Object, Object> delegate) {
            Object value = delegate.get(this.name);
            if (value == null || this.clazz.isInstance(value)) {
                return this.clazz.cast(value);
            }
            return this.defaultSupplier.get();
        }
    }

    public static enum Stage {
        Enter,
        Execute,
        Exit;

    }
}

