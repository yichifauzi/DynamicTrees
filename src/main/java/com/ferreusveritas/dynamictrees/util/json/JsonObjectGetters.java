package com.ferreusveritas.dynamictrees.util.json;

import com.ferreusveritas.dynamictrees.api.cells.CellKit;
import com.ferreusveritas.dynamictrees.api.worldgen.BiomePropertySelectors;
import com.ferreusveritas.dynamictrees.api.worldgen.FeatureCanceller;
import com.ferreusveritas.dynamictrees.blocks.leaves.LeavesProperties;
import com.ferreusveritas.dynamictrees.growthlogic.GrowthLogicKit;
import com.ferreusveritas.dynamictrees.items.Seed;
import com.ferreusveritas.dynamictrees.systems.genfeatures.GenFeature;
import com.ferreusveritas.dynamictrees.systems.genfeatures.VinesGenFeature;
import com.ferreusveritas.dynamictrees.systems.genfeatures.config.ConfiguredGenFeature;
import com.ferreusveritas.dynamictrees.trees.*;
import com.ferreusveritas.dynamictrees.util.BiomeList;
import com.ferreusveritas.dynamictrees.util.BiomePredicate;
import com.ferreusveritas.dynamictrees.worldgen.BiomeDatabase;
import com.ferreusveritas.dynamictrees.worldgen.json.ChanceSelectorGetter;
import com.ferreusveritas.dynamictrees.worldgen.json.DensitySelectorGetter;
import com.ferreusveritas.dynamictrees.worldgen.json.SpeciesSelectorGetter;
import com.google.common.collect.Sets;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.block.Block;
import net.minecraft.item.Item;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.ResourceLocationException;
import net.minecraft.world.biome.Biome;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.fml.ModLoader;
import net.minecraftforge.fml.event.lifecycle.IModBusEvent;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.*;

/**
 * Holds {@link IJsonObjectGetter} objects, which can be used to obtain objects from
 * {@link JsonElement} objects.
 *
 * @author Harley O'Connor
 */
// TODO: Find another way of initialising so that calling them early doesn't lead to feeding registry entry getters null.
public final class JsonObjectGetters {

    private static final Set<JsonObjectGetterHolder<?>> OBJECT_GETTERS = Sets.newHashSet();

    /** Returned by {@link #getObjectGetter(Class)} if an object getter wasn't found. */
    public static final class NullObjectGetter<T> implements IJsonObjectGetter<T> {
        @Override
        public boolean isValid() {
            return false;
        }

        @Override
        public ObjectFetchResult<T> get(final JsonElement jsonElement) {
            return ObjectFetchResult.failure("Could not get Json object getter for json element: " + jsonElement.toString() + ".");
        }
    }

    /**
     * Gets the {@link IJsonObjectGetter} for the given class type.
     *
     * @param objectClass The {@link Class} of the object to get.
     * @param <T> The type of the object.
     * @return The {@link IJsonObjectGetter} for the class, or {@link NullObjectGetter} if it wasn't found.
     */
    @SuppressWarnings("unchecked")
    public static <T> IJsonObjectGetter<T> getObjectGetter (final Class<T> objectClass) {
        return OBJECT_GETTERS.stream().filter(jsonObjectGetterHolder -> jsonObjectGetterHolder.objectClass.equals(objectClass))
                .findFirst().map(jsonObjectGetterHolder -> (IJsonObjectGetter<T>) jsonObjectGetterHolder.objectGetter).orElse(new NullObjectGetter<>());
    }

    /**
     * Registers an {@link IJsonObjectGetter} to the registry.
     *
     * @param objectClass The {@link Class} of the object that will be obtained.
     * @param objectGetter The {@link IJsonObjectGetter} to register.
     * @param <T> The type of the object getter.
     * @return The {@link IJsonObjectGetter} given.
     */
    public static <T> IJsonObjectGetter<T> register(final Class<T> objectClass, final IJsonObjectGetter<T> objectGetter) {
        OBJECT_GETTERS.add(new JsonObjectGetterHolder<>(objectClass, objectGetter));
        return objectGetter;
    }

    /**
     * Holds an {@link IJsonObjectGetter} and the relevant {@link Class} of type {@link T}.
     *
     * @param <T> The type the object getter fetches.
     */
    private static final class JsonObjectGetterHolder<T> {
        private final Class<T> objectClass;
        private final IJsonObjectGetter<T> objectGetter;

        public JsonObjectGetterHolder(final Class<T> objectClass, final IJsonObjectGetter<T> objectGetter) {
            this.objectClass = objectClass;
            this.objectGetter = objectGetter;
        }
    }

    public static final IJsonObjectGetter<JsonElement> JSON_ELEMENT_GETTER = register(JsonElement.class, ObjectFetchResult::success);

    public static final IJsonObjectGetter<String> STRING_GETTER = register(String.class, jsonElement -> {
        if (!jsonElement.isJsonPrimitive() || !jsonElement.getAsJsonPrimitive().isString())
            return ObjectFetchResult.failure("Json element was not a string.");

        return ObjectFetchResult.success(jsonElement.getAsString());
    });

    public static final IJsonObjectGetter<Boolean> BOOLEAN_GETTER = register(Boolean.class, jsonElement -> {
        if (!jsonElement.isJsonPrimitive() || !jsonElement.getAsJsonPrimitive().isBoolean())
            return ObjectFetchResult.failure("Json element was not a boolean.");

        return ObjectFetchResult.success(jsonElement.getAsBoolean());
    });

    public static final IJsonObjectGetter<Number> NUMBER_GETTER = register(Number.class, jsonElement -> {
        if (!jsonElement.isJsonPrimitive() || !jsonElement.getAsJsonPrimitive().isNumber())
            return ObjectFetchResult.failure("Json element was not a number.");

        return ObjectFetchResult.success(jsonElement.getAsNumber());
    });

    public static final IJsonObjectGetter<Integer> INTEGER_GETTER = register(Integer.class, jsonElement -> {
        final ObjectFetchResult<Number> numberFetch = NUMBER_GETTER.get(jsonElement);

        if (!numberFetch.wasSuccessful())
            return ObjectFetchResult.failureFromOther(numberFetch);

        return ObjectFetchResult.success(numberFetch.getValue().intValue());
    });

    public static final IJsonObjectGetter<Double> DOUBLE_GETTER = register(Double.class, jsonElement -> {
        final ObjectFetchResult<Number> numberFetch = NUMBER_GETTER.get(jsonElement);

        if (!numberFetch.wasSuccessful())
            return ObjectFetchResult.failureFromOther(numberFetch);

        return ObjectFetchResult.success(numberFetch.getValue().doubleValue());
    });

    public static final IJsonObjectGetter<Float> FLOAT_GETTER = register(Float.class, jsonElement -> {
        final ObjectFetchResult<Number> numberFetch = NUMBER_GETTER.get(jsonElement);

        if (!numberFetch.wasSuccessful())
            return ObjectFetchResult.failureFromOther(numberFetch);

        return ObjectFetchResult.success(numberFetch.getValue().floatValue());
    });

    public static final IJsonObjectGetter<JsonObject> JSON_OBJECT_GETTER = register(JsonObject.class, jsonElement -> {
        if (!jsonElement.isJsonObject())
            return ObjectFetchResult.failure("Json element was not a json object.");

        return ObjectFetchResult.success(jsonElement.getAsJsonObject());
    });

    public static final IJsonObjectGetter<JsonArray> JSON_ARRAY_GETTER = register(JsonArray.class, jsonElement -> {
        if (!jsonElement.isJsonArray())
            return ObjectFetchResult.failure("Json element was not a json array.");

        return ObjectFetchResult.success(jsonElement.getAsJsonArray());
    });

    public static final IJsonObjectGetter<ResourceLocation> RESOURCE_LOCATION_GETTER = register(ResourceLocation.class, jsonElement -> {
        final ObjectFetchResult<String> stringFetchResult = STRING_GETTER.get(jsonElement);

        if (!stringFetchResult.wasSuccessful())
            return ObjectFetchResult.failureFromOther(stringFetchResult);

        try {
            return ObjectFetchResult.success(new ResourceLocation(stringFetchResult.getValue()));
        } catch (ResourceLocationException e) {
            return ObjectFetchResult.failure("Json element was not a valid resource location: " + e.getMessage());
        }
    });

    public static IJsonObjectGetter<Block> BLOCK_GETTER;
    public static IJsonObjectGetter<Item> ITEM_GETTER;
    public static IJsonObjectGetter<Biome> BIOME_GETTER;

    public static final IJsonObjectGetter<CellKit> CELL_KIT_GETTER = register(CellKit.class, new RegistryEntryGetter<>(CellKit.REGISTRY));
    public static final IJsonObjectGetter<LeavesProperties> LEAVES_PROPERTIES_GETTER = register(LeavesProperties.class, new RegistryEntryGetter<>(LeavesProperties.REGISTRY));
    public static final IJsonObjectGetter<GrowthLogicKit> GROWTH_LOGIC_KIT_GETTER = register(GrowthLogicKit.class, new RegistryEntryGetter<>(GrowthLogicKit.REGISTRY));
    public static final IJsonObjectGetter<GenFeature> GEN_FEATURE_GETTER = register(GenFeature.class, new RegistryEntryGetter<>(GenFeature.REGISTRY));
    public static final IJsonObjectGetter<Family> FAMILY_GETTER = register(Family.class, new RegistryEntryGetter<>(Family.REGISTRY));
    public static final IJsonObjectGetter<Species> SPECIES_GETTER = register(Species.class, new RegistryEntryGetter<>(Species.REGISTRY));
    public static final IJsonObjectGetter<FeatureCanceller> FEATURE_CANCELLER_GETTER = register(FeatureCanceller.class, new RegistryEntryGetter<>(FeatureCanceller.REGISTRY));

    public static final IJsonObjectGetter<LeavesProperties.Type> LEAVES_PROPERTIES_TYPE_GETTER = register(LeavesProperties.Type.class, new RegistryEntryTypeGetter<>(LeavesProperties.REGISTRY));
    public static final IJsonObjectGetter<Family.Type> FAMILY_TYPE_GETTER = register(Family.Type.class, new RegistryEntryTypeGetter<>(Family.REGISTRY));
    public static final IJsonObjectGetter<Species.Type> SPECIES_TYPE_GETTER = register(Species.Type.class, new RegistryEntryTypeGetter<>(Species.REGISTRY));

    public static final IJsonObjectGetter<ConfiguredGenFeature<GenFeature>> CONFIGURED_GEN_FEATURE_GETTER = register(ConfiguredGenFeature.NULL_CONFIGURED_FEATURE_CLASS, new ConfiguredGenFeatureGetter());

    public static final IJsonObjectGetter<Seed> SEED_GETTER = register(Seed.class, jsonElement -> {
        final ObjectFetchResult<Item> itemFetchResult = ITEM_GETTER.get(jsonElement);

        if (!itemFetchResult.wasSuccessful())
            return ObjectFetchResult.failureFromOther(itemFetchResult);

        final Item item = itemFetchResult.getValue();

        if (!(item instanceof Seed))
            return ObjectFetchResult.failure("Item '" + item.getRegistryName() + "' was not a Seed.");

        return ObjectFetchResult.success(((Seed) item));
    });

    // Random enum getters.
    public static final IJsonObjectGetter<VinesGenFeature.VineType> VINE_TYPE_GETTER = register(VinesGenFeature.VineType.class, new EnumGetter<>(VinesGenFeature.VineType.class));
    public static final IJsonObjectGetter<BiomeDatabase.Operation> OPERATION_GETTER = new EnumGetter<>(BiomeDatabase.Operation.class);

    public static final IJsonObjectGetter<BiomeList> BIOME_LIST_GETTER = register(BiomeList.class, new BiomeListGetter());
    public static final IJsonObjectGetter<BiomePredicate> BIOME_PREDICATE_GETTER = register(BiomePredicate.class, jsonElement -> {
        final ObjectFetchResult<BiomeList> biomeListFetchResult = BIOME_LIST_GETTER.get(jsonElement);

        if (!biomeListFetchResult.wasSuccessful())
            return ObjectFetchResult.failureFromOther(biomeListFetchResult);

        return ObjectFetchResult.success(biome -> biomeListFetchResult.getValue().stream().anyMatch(currentBiome -> currentBiome.getRegistryName().equals(biome.getRegistryName())));
    });

    public static final IJsonObjectGetter<BiomePropertySelectors.ISpeciesSelector> SPECIES_SELECTOR_GETTER = register(
            BiomePropertySelectors.ISpeciesSelector.class, new SpeciesSelectorGetter());
    public static final IJsonObjectGetter<BiomePropertySelectors.IDensitySelector> DENSITY_SELECTOR_GETTER = register(
            BiomePropertySelectors.IDensitySelector.class, new DensitySelectorGetter());
    public static final IJsonObjectGetter<BiomePropertySelectors.IChanceSelector> CHANCE_SELECTOR_GETTER = register(
            BiomePropertySelectors.IChanceSelector.class, new ChanceSelectorGetter());

    /**
     * Registers {@link ForgeRegistryEntryGetter} objects. This should be called after the registries are initiated to avoid
     * giving null to the getters.
     */
    public static void registerForgeEntryGetters() {
        BLOCK_GETTER = register(Block.class, new ForgeRegistryEntryGetter<>(ForgeRegistries.BLOCKS, "block"));
        ITEM_GETTER = register(Item.class, new ForgeRegistryEntryGetter<>(ForgeRegistries.ITEMS, "item"));
        BIOME_GETTER = register(Biome.class, new ForgeRegistryEntryGetter<>(ForgeRegistries.BIOMES, "biome"));
    }

    public static void postRegistryEvent() {
        ModLoader.get().postEvent(new RegistryEvent());
    }

    /**
     * This event is posted for add-ons to register custom Json object getters at the right time.
     */
    public static final class RegistryEvent extends Event implements IModBusEvent { }

}
