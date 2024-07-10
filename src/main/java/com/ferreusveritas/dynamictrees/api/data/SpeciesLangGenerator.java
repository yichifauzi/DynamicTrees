package com.ferreusveritas.dynamictrees.api.data;

import com.ferreusveritas.dynamictrees.api.registry.RegistryEntry;
import com.ferreusveritas.dynamictrees.data.provider.DTLangProvider;
import com.ferreusveritas.dynamictrees.tree.family.Family;
import com.ferreusveritas.dynamictrees.tree.species.Species;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemNameBlockItem;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import org.apache.commons.lang3.StringUtils;

import java.util.Arrays;
import java.util.Optional;
import java.util.stream.Collectors;

public class SpeciesLangGenerator implements Generator<DTLangProvider, Species>{
    DTLangProvider provider;

    @Override
    public void generate(DTLangProvider provider, Species input, Dependencies dependencies) {
        this.provider = provider;
        speciesLang(input, input.getLangOverride("species"));
        if(input.hasSeed()) {
            itemLang(input.getSeed().get(), input.getLangOverride("seed"));
        }
        input.getSapling().ifPresent(sapling -> blockLang(sapling, input.getLangOverride("sapling")));

    }

    @Override
    public Dependencies gatherDependencies(Species input) {
        return new Dependencies();
    }
    protected void itemLang(Item entry, Optional<String> override) {
        if (!(entry instanceof BlockItem) || entry instanceof ItemNameBlockItem) {
            provider.addItem(() -> entry, override.orElse(checkReplace(ForgeRegistries.ITEMS.getKey(entry))));
        }
    }

    protected void speciesLang(Species entry, Optional<String> override) {
        provider.add(entry.getLocalizedName(), override.orElse(checkReplace(entry.getRegistryName())));
    }

    protected void blockLang(Block entry, Optional<String> blah) {
        provider.addBlock(() -> entry, blah.orElse(checkReplace(ForgeRegistries.BLOCKS.getKey(entry))));
    }

    protected String checkReplace(ResourceLocation registryObject) {
        return Arrays.stream(registryObject.getPath().split("_"))
                .map(StringUtils::capitalize)
                .filter(s -> !s.isBlank())
                .collect(Collectors.joining(" "))
                .trim();
    }
}
