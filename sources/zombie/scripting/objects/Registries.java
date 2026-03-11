/*
 * Decompiled with CFR 0.152.
 */
package zombie.scripting.objects;

import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Supplier;
import zombie.UsedFromLua;
import zombie.core.Core;
import zombie.core.properties.TilePropertyKey;
import zombie.scripting.objects.AmmoType;
import zombie.scripting.objects.Book;
import zombie.scripting.objects.BookSubject;
import zombie.scripting.objects.Brochure;
import zombie.scripting.objects.Business;
import zombie.scripting.objects.Catalogue;
import zombie.scripting.objects.CharacterProfession;
import zombie.scripting.objects.CharacterTrait;
import zombie.scripting.objects.ComicBook;
import zombie.scripting.objects.Doodle;
import zombie.scripting.objects.DoodleKids;
import zombie.scripting.objects.Flier;
import zombie.scripting.objects.ItemBodyLocation;
import zombie.scripting.objects.ItemTag;
import zombie.scripting.objects.ItemType;
import zombie.scripting.objects.Job;
import zombie.scripting.objects.Letter;
import zombie.scripting.objects.Locket;
import zombie.scripting.objects.Magazine;
import zombie.scripting.objects.MagazineSubject;
import zombie.scripting.objects.MetaRecipe;
import zombie.scripting.objects.MoodleType;
import zombie.scripting.objects.Newspaper;
import zombie.scripting.objects.OldNewspaper;
import zombie.scripting.objects.PetName;
import zombie.scripting.objects.Photo;
import zombie.scripting.objects.Postcard;
import zombie.scripting.objects.RecipeKey;
import zombie.scripting.objects.Registry;
import zombie.scripting.objects.ResourceLocation;
import zombie.scripting.objects.RpgManual;
import zombie.scripting.objects.SeasonRecipe;
import zombie.scripting.objects.SoundKey;
import zombie.scripting.objects.WeaponCategory;

@UsedFromLua
public class Registries {
    public static final Registry<Registry<?>> REGISTRY = new Registry("registry");
    private static final List<Supplier<?>> BOOTSTRAPS = new ArrayList();
    public static final Registry<AmmoType> AMMO_TYPE = Registries.register("ammo_type", () -> AmmoType.BULLETS_44);
    public static final Registry<Book> BOOK = Registries.register("book", () -> Book.WORLDS_UNLIKELIEST_PLANE_CRASHES);
    public static final Registry<BookSubject> BOOK_SUBJECT = Registries.register("book_subject", () -> BookSubject.ADVENTURE_NON_FICTION);
    public static final Registry<Brochure> BROCHURE = Registries.register("brochure", () -> Brochure.AIRPORT);
    public static final Registry<Business> BUSINESS = Registries.register("business", () -> Business.BEANZ);
    public static final Registry<Catalogue> CATALOGUE = Registries.register("catalogue", () -> Catalogue.AA_RON_HUNTING_SUPPLIES);
    public static final Registry<CharacterProfession> CHARACTER_PROFESSION = Registries.register("character_profession", () -> CharacterProfession.BURGLAR);
    public static final Registry<CharacterTrait> CHARACTER_TRAIT = Registries.register("character_trait", () -> CharacterTrait.ADRENALINE_JUNKIE);
    public static final Registry<ComicBook> COMIC_BOOK = Registries.register("comic_book", () -> ComicBook.BLASTFORCE);
    public static final Registry<Doodle> DOODLE = Registries.register("doodle", () -> Doodle.A_BATTLE);
    public static final Registry<DoodleKids> DOODLE_KIDS = Registries.register("doodle_kids", () -> DoodleKids.A_CARTOON_CHARACTER);
    public static final Registry<Flier> FLIER = Registries.register("flier", () -> Flier.A1_HAY);
    public static final Registry<Letter> LETTER = Registries.register("genericMail", () -> Letter.ACCEPTANCE_LETTER);
    public static final Registry<Locket> LOCKET = Registries.register("locket", () -> Locket.A_BIRTHDAY_PARTY);
    public static final Registry<TilePropertyKey> TILE_PROPERTY_KEY = Registries.register("tile_property_key", () -> TilePropertyKey.ALWAYS_DRAW);
    public static final Registry<ItemBodyLocation> ITEM_BODY_LOCATION = Registries.register("item_body_location", () -> ItemBodyLocation.HAT);
    public static final Registry<ItemTag> ITEM_TAG = Registries.register("item_tag", () -> ItemTag.IS_MEMENTO);
    public static final Registry<ItemType> ITEM_TYPE = Registries.register("item_type", () -> ItemType.FOOD);
    public static final Registry<Job> JOB = Registries.register("job", () -> Job.ACCOUNTANT);
    public static final Registry<Magazine> MAGAZINE = Registries.register("magazine", () -> Magazine.AIR_AND_SPACE_NEWS);
    public static final Registry<MagazineSubject> MAGAZINE_SUBJECT = Registries.register("magazine_subject", () -> MagazineSubject.ART);
    public static final Registry<MetaRecipe> META_RECIPE = Registries.register("meta_recipe", () -> MetaRecipe.ASSEMBLE_ADVANCED_FRAMEPACK);
    public static final Registry<MoodleType> MOODLE_TYPE = Registries.register("moodle_type", () -> MoodleType.ENDURANCE);
    public static final Registry<Newspaper> NEWSPAPER = Registries.register("newspaper", () -> Newspaper.KENTUCKY_HERALD);
    public static final Registry<OldNewspaper> OLD_NEWSPAPER = Registries.register("old_newspaper", () -> OldNewspaper.BOWLING_GREEN_POST);
    public static final Registry<PetName> PET_NAME = Registries.register("pet_name", () -> PetName.BEN);
    public static final Registry<Photo> PHOTO = Registries.register("photo", () -> Photo.A_BABY);
    public static final Registry<Postcard> POSTCARD = Registries.register("postcard", () -> Postcard.ALASKA);
    public static final Registry<RpgManual> RPG_MANUAL = Registries.register("rpg_magazine", () -> RpgManual.ADVENTURE_MANAGER_IMAGINATION_RULEBOOK);
    public static final Registry<SeasonRecipe> SEASON_RECIPE = Registries.register("season_recipe", () -> SeasonRecipe.BARLEY_GROWING_SEASON);
    public static final Registry<SoundKey> SOUND_KEY = Registries.register("sound_key", () -> SoundKey.ACOUSTIC_GUITAR_BREAK);
    public static final Registry<WeaponCategory> WEAPON_CATEGORY = Registries.register("weapon_category", () -> WeaponCategory.BLUNT);

    public static <T> Registry<T> register(String name, Supplier<T> bootstrap) {
        Registry registry = REGISTRY.register(ResourceLocation.of(name), new Registry(name));
        BOOTSTRAPS.add(bootstrap);
        return registry;
    }

    public static List<Registry<? extends RecipeKey>> getAllRecipeRegistries() {
        ArrayList<Registry<? extends RecipeKey>> recipeRegistries = new ArrayList<Registry<? extends RecipeKey>>();
        recipeRegistries.add(META_RECIPE);
        return recipeRegistries;
    }

    static {
        BOOTSTRAPS.forEach(Supplier::get);
        if (Core.IS_DEV) {
            REGISTRY.forEach(registry -> {
                if (registry.values().stream().anyMatch(Objects::isNull)) {
                    throw new IllegalStateException("Registry %s has null values".formatted(REGISTRY.getLocation((Registry<?>)registry)));
                }
            });
            REGISTRY.forEach(registry -> {
                Optional first = registry.values().stream().findFirst();
                if (first.isEmpty()) {
                    throw new IllegalStateException("Registry %s was empty".formatted(REGISTRY.getLocation((Registry<?>)registry)));
                }
                Class<?> clazz = first.get().getClass();
                if (Arrays.stream(clazz.getConstructors()).anyMatch(c -> Modifier.isPublic(c.getModifiers()))) {
                    throw new IllegalStateException("Registry %s is holding objects with a public constructor".formatted(REGISTRY.getLocation((Registry<?>)registry)));
                }
                if (Arrays.stream(clazz.getDeclaredMethods()).noneMatch(m -> m.getName().equals("registerBase") && Modifier.isPrivate(m.getModifiers()))) {
                    throw new IllegalStateException("Registry %s is missing a private registerBase, are we sure we set this up to be resettable?".formatted(REGISTRY.getLocation((Registry<?>)registry)));
                }
                try {
                    clazz.getDeclaredMethod("equals", Object.class);
                    throw new IllegalStateException("Registry %s of %s should not override equals(Object)".formatted(REGISTRY.getLocation((Registry<?>)registry), clazz.getName()));
                }
                catch (NoSuchMethodException noSuchMethodException) {
                    try {
                        clazz.getDeclaredMethod("hashCode", new Class[0]);
                        throw new IllegalStateException("Registry %s of %s should not override hashCode()".formatted(REGISTRY.getLocation((Registry<?>)registry), clazz.getName()));
                    }
                    catch (NoSuchMethodException noSuchMethodException2) {
                        return;
                    }
                }
            });
        }
    }
}

