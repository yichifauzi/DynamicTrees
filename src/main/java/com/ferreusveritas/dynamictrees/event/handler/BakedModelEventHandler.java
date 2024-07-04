package com.ferreusveritas.dynamictrees.event.handler;

import com.ferreusveritas.dynamictrees.DynamicTrees;
import com.ferreusveritas.dynamictrees.block.PottedSaplingBlock;
import com.ferreusveritas.dynamictrees.models.baked.BakedModelBlockBonsaiPot;
import com.ferreusveritas.dynamictrees.models.baked.LargePalmLeavesBakedModel;
import com.ferreusveritas.dynamictrees.models.baked.MediumPalmLeavesBakedModel;
import com.ferreusveritas.dynamictrees.models.baked.SmallPalmLeavesBakedModel;
import com.ferreusveritas.dynamictrees.models.loader.*;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ModelEvent;
import net.minecraftforge.client.event.ModelEvent.RegisterGeometryLoaders;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * @author Harley O'Connor
 */
@Mod.EventBusSubscriber(modid = DynamicTrees.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class BakedModelEventHandler {

    public static final ResourceLocation BRANCH = DynamicTrees.location("branch");
    public static final ResourceLocation SURFACE_ROOT = DynamicTrees.location("surface_root");
    public static final ResourceLocation ROOTS = DynamicTrees.location("roots");
    public static final ResourceLocation THICK_BRANCH = DynamicTrees.location("thick_branch");
    public static final ResourceLocation LARGE_PALM_FRONDS = DynamicTrees.location("large_palm_fronds");
    public static final ResourceLocation MEDIUM_PALM_FRONDS = DynamicTrees.location("medium_palm_fronds");
    public static final ResourceLocation SMALL_PALM_FRONDS = DynamicTrees.location("small_palm_fronds");

    @SubscribeEvent
    public static void onModelRegistryEvent(RegisterGeometryLoaders event) {
        // Register model loaders for baked models.
        event.register("branch", new BranchBlockModelLoader());
        event.register("surface_root", new SurfaceRootBlockModelLoader());
        event.register("thick_branch", new ThickBranchBlockModelLoader());
        event.register("roots", new RootsBlockModelLoader());
        event.register("large_palm_fronds", new PalmLeavesModelLoader(0));
        event.register("medium_palm_fronds", new PalmLeavesModelLoader(1));
        event.register("small_palm_fronds", new PalmLeavesModelLoader(2));
    }

    @SubscribeEvent
    public static void onModelModifyBakingResultResult(ModelEvent.ModifyBakingResult event) {
        // Put bonsai pot baked model into its model location.
        event.getModels().computeIfPresent(new ModelResourceLocation(PottedSaplingBlock.REG_NAME, ""), (k, val) -> new BakedModelBlockBonsaiPot(val));
    }

    @SubscribeEvent
    public static void onModelBake(ModelEvent.BakingCompleted event) {
        // Setup fronds models
        MediumPalmLeavesBakedModel.INSTANCES.forEach(MediumPalmLeavesBakedModel::setupModels);
        LargePalmLeavesBakedModel.INSTANCES.forEach(LargePalmLeavesBakedModel::setupModels);
        SmallPalmLeavesBakedModel.INSTANCES.forEach(SmallPalmLeavesBakedModel::setupModels);
    }
}