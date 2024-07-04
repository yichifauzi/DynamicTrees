package com.ferreusveritas.dynamictrees.models.geometry;

import com.ferreusveritas.dynamictrees.models.baked.LargePalmLeavesBakedModel;
import com.ferreusveritas.dynamictrees.models.baked.MediumPalmLeavesBakedModel;
import com.ferreusveritas.dynamictrees.models.baked.SmallPalmLeavesBakedModel;
import net.minecraft.client.renderer.block.model.ItemOverrides;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.Material;
import net.minecraft.client.resources.model.ModelBaker;
import net.minecraft.client.resources.model.ModelState;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.client.model.geometry.IGeometryBakingContext;
import net.minecraftforge.client.model.geometry.IUnbakedGeometry;

import javax.annotation.Nullable;
import java.util.function.Function;

public class PalmLeavesModelGeometry implements IUnbakedGeometry<PalmLeavesModelGeometry> {

    protected final ResourceLocation frondsResLoc;

    private final int frondType;

    public PalmLeavesModelGeometry(final ResourceLocation frondsResLoc, int type){
        this.frondsResLoc = frondsResLoc;
        this.frondType = type;
    }

    @Override
    public BakedModel bake(IGeometryBakingContext context, ModelBaker baker, Function<Material, TextureAtlasSprite> spriteGetter, ModelState modelState, ItemOverrides overrides, ResourceLocation modelLocation) {
        return switch (frondType) {
            default -> new LargePalmLeavesBakedModel(modelLocation, frondsResLoc);
            case 1 -> new MediumPalmLeavesBakedModel(modelLocation, frondsResLoc);
            case 2 -> new SmallPalmLeavesBakedModel(modelLocation, frondsResLoc);
        };
    }
}