package com.ferreusveritas.dynamictrees.data.provider;

import com.ferreusveritas.dynamictrees.api.GatherDataHelper;
import com.ferreusveritas.dynamictrees.api.data.Generator;
import com.ferreusveritas.dynamictrees.api.registry.Registry;
import com.ferreusveritas.dynamictrees.tree.species.Species;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemNameBlockItem;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.common.data.LanguageProvider;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import org.apache.commons.lang3.StringUtils;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class DTLangProvider extends LanguageProvider  implements DTDataProvider{
    private final String modId;
    private final List<Registry<?>> registries;

    public DTLangProvider(PackOutput gen, String modId, Collection<Registry<?>> registries) {
        super(gen, modId, "en_us");
        this.modId = modId;
        this.registries = ImmutableList.copyOf(registries);
    }

    @Override
    protected void addTranslations() {
        this.registries.forEach(registry ->
                registry.dataGenerationStream(this.modId).forEach(entry ->
                        entry.generateLangData(this)
                )
        );
        Generator<DTLangProvider, String> generator = GatherDataHelper.getExtraLangGenerators().get(modId);
        if (generator != null) {
            generator.generate(this, "", new Generator.Dependencies());
        }
    }
}
