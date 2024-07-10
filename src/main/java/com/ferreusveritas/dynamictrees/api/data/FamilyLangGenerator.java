package com.ferreusveritas.dynamictrees.api.data;

import com.ferreusveritas.dynamictrees.data.provider.DTLangProvider;
import com.ferreusveritas.dynamictrees.tree.family.Family;
import com.ferreusveritas.dynamictrees.tree.family.MangroveFamily;
import com.ferreusveritas.dynamictrees.tree.species.Species;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemNameBlockItem;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.registries.ForgeRegistries;
import org.apache.commons.lang3.StringUtils;

import java.util.Arrays;
import java.util.Optional;
import java.util.stream.Collectors;

public class FamilyLangGenerator implements Generator<DTLangProvider, Family>{
    DTLangProvider provider;

    @Override
    public void generate(DTLangProvider provider, Family input, Dependencies dependencies) {
        this.provider = provider;
        familyLang(input, input.getLangOverride("family"));
        input.getBranch().ifPresent(branch -> blockLang(branch, input.getLangOverride("branch")));
        if(input.hasSurfaceRoot()){
            blockLang(input.getSurfaceRoot().get(), input.getLangOverride("surface_root"));
        }
        if(input instanceof MangroveFamily mgf){
            mgf.getRoots().ifPresent(root -> blockLang(root, input.getLangOverride("root")));
            mgf.getDefaultSoil().getBlock().ifPresent(soil -> blockLang(soil, input.getLangOverride("soil")));
        }
    }

    @Override
    public Dependencies gatherDependencies(Family input) {
        return new Dependencies();
    }

    protected void familyLang(Family entry, Optional<String> override) {
        provider.add(I18n.get("family." + entry.getRegistryName().toString().replace(":", ".")), override.orElse(checkReplace(entry.getRegistryName())));
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
