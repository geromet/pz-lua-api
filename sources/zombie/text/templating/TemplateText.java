/*
 * Decompiled with CFR 0.152.
 */
package zombie.text.templating;

import java.util.Random;
import se.krka.kahlua.j2se.KahluaTableImpl;
import zombie.Lua.LuaEventManager;
import zombie.UsedFromLua;
import zombie.characters.SurvivorFactory;
import zombie.text.templating.IReplace;
import zombie.text.templating.IReplaceProvider;
import zombie.text.templating.ITemplateBuilder;
import zombie.text.templating.TemplateTextBuilder;

@UsedFromLua
public class TemplateText {
    private static final ITemplateBuilder builder = new TemplateTextBuilder();
    private static final Random m_random = new Random(4397238L);

    public static ITemplateBuilder CreateBlanc() {
        return new TemplateTextBuilder();
    }

    public static ITemplateBuilder CreateCopy() {
        TemplateTextBuilder builder = new TemplateTextBuilder();
        builder.CopyFrom(TemplateText.builder);
        return builder;
    }

    public static String Build(String input) {
        return builder.Build(input);
    }

    public static String Build(String input, IReplaceProvider replaceProvider) {
        return builder.Build(input, replaceProvider);
    }

    public static String Build(String input, KahluaTableImpl table) {
        try {
            return builder.Build(input, table);
        }
        catch (Exception e) {
            e.printStackTrace();
            return input;
        }
    }

    public static void RegisterKey(String key, KahluaTableImpl table) {
        builder.RegisterKey(key, table);
    }

    public static void RegisterKey(String key, IReplace replace) {
        builder.RegisterKey(key, replace);
    }

    public static void Initialize() {
        builder.RegisterKey("lastname", new IReplace(){

            @Override
            public String getString() {
                return SurvivorFactory.getRandomSurname();
            }
        });
        builder.RegisterKey("firstname", new IReplace(){

            @Override
            public String getString() {
                return TemplateText.RandNext(100) > 50 ? SurvivorFactory.getRandomForename(true) : SurvivorFactory.getRandomForename(false);
            }
        });
        builder.RegisterKey("maleName", new IReplace(){

            @Override
            public String getString() {
                return SurvivorFactory.getRandomForename(false);
            }
        });
        builder.RegisterKey("femaleName", new IReplace(){

            @Override
            public String getString() {
                return SurvivorFactory.getRandomForename(true);
            }
        });
        LuaEventManager.triggerEvent("OnTemplateTextInit");
    }

    public static void Reset() {
        builder.Reset();
    }

    public static float RandNext(float min, float max) {
        if (min == max) {
            return min;
        }
        if (min > max) {
            float m = min;
            max = min = max;
        }
        return min + m_random.nextFloat() * (max - min);
    }

    public static float RandNext(float bound) {
        return m_random.nextFloat() * bound;
    }

    public static int RandNext(int min, int max) {
        if (min == max) {
            return min;
        }
        if (min > max) {
            int m = min;
            max = min = max;
        }
        return min + m_random.nextInt(max - min);
    }

    public static int RandNext(int bound) {
        return m_random.nextInt(bound);
    }
}

