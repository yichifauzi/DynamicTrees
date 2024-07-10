package com.ferreusveritas.dynamictrees.api;

import com.ferreusveritas.dynamictrees.DynamicTrees;
import com.ferreusveritas.dynamictrees.api.data.Generator;
import com.ferreusveritas.dynamictrees.api.registry.Registry;
import com.ferreusveritas.dynamictrees.data.provider.DTBlockStateProvider;
import com.ferreusveritas.dynamictrees.data.provider.DTBlockTagsProvider;
import com.ferreusveritas.dynamictrees.data.provider.DTDatapackBuiltinEntriesProvider;
import com.ferreusveritas.dynamictrees.data.provider.DTItemModelProvider;
import com.ferreusveritas.dynamictrees.data.provider.DTItemTagsProvider;
import com.ferreusveritas.dynamictrees.data.provider.DTLangProvider;
import com.ferreusveritas.dynamictrees.data.provider.DTLootTableProvider;
import com.ferreusveritas.dynamictrees.tree.species.Species;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.DataGenerator;
import net.minecraft.data.PackOutput;
import net.minecraftforge.data.event.GatherDataEvent;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

/**
 * @author Harley O'Connor
 */
public final class GatherDataHelper {
    private static final Map<String,Generator<DTLangProvider, String>> extraLangGenerators = new HashMap<>();

    public static void gatherAllData(final String modId, final GatherDataEvent event, Registry<?>... registries) {
        gatherTagData(modId, event);
        gatherBlockStateAndModelData(modId, event, registries);
        gatherItemModelData(modId, event, registries);
        gatherLootData(modId, event);
        gatherDatapackData(modId, event);
        gatherLangData(modId, event, registries);
    }

    public static void gatherTagData(final String modId, final GatherDataEvent event) {
        final DataGenerator generator = event.getGenerator();
        PackOutput packOutput = generator.getPackOutput();
        CompletableFuture<HolderLookup.Provider> lookupProvider = event.getLookupProvider();

        final DTBlockTagsProvider blockTagsProvider = new DTBlockTagsProvider(packOutput, lookupProvider, modId, event.getExistingFileHelper());
        final DTItemTagsProvider itemTagsProvider = new DTItemTagsProvider(packOutput, modId, lookupProvider, blockTagsProvider.contentsGetter(), event.getExistingFileHelper());

        generator.addProvider(event.includeServer(), blockTagsProvider);
        generator.addProvider(event.includeServer(), itemTagsProvider);
    }

    public static void gatherBlockStateAndModelData(final String modId, final GatherDataEvent event, Registry<?>... registries) {
        event.getGenerator().addProvider(event.includeServer(), new DTBlockStateProvider(event.getGenerator().getPackOutput(), modId,
                event.getExistingFileHelper(), Arrays.asList(registries)));
    }

    public static void gatherItemModelData(final String modId, final GatherDataEvent event, Registry<?>... registries) {
        event.getGenerator().addProvider(event.includeServer(), new DTItemModelProvider(event.getGenerator().getPackOutput(), modId,
                event.getExistingFileHelper(), Arrays.asList(registries)));
    }

    public static void gatherLootData(final String modId, final GatherDataEvent event) {
        event.getGenerator().addProvider(event.includeServer(), new DTLootTableProvider(
                event.getGenerator().getPackOutput(), modId, event.getExistingFileHelper()
        ));
    }
    public static void gatherLangData(final String modId, final GatherDataEvent event, Registry<?>... registries){
        event.getGenerator().addProvider(event.includeClient(), new DTLangProvider(
                event.getGenerator().getPackOutput(), modId, Arrays.asList(registries))
        );
    }

    public static void gatherDatapackData(final String modId, final GatherDataEvent event) {
        event.getGenerator().addProvider(event.includeServer(), new DTDatapackBuiltinEntriesProvider(
                event.getGenerator().getPackOutput(), event.getLookupProvider(), Set.of(modId, DynamicTrees.MINECRAFT)));
    }
    public static void addLangGenerator(String modId,Generator<DTLangProvider, String> generator) {
        GatherDataHelper.extraLangGenerators.put(modId,generator);
    }
    public static Map<String,Generator<DTLangProvider, String>> getExtraLangGenerators() {
        return GatherDataHelper.extraLangGenerators;
    }
}
