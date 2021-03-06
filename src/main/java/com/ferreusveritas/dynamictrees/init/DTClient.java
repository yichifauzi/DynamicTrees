package com.ferreusveritas.dynamictrees.init;

import com.ferreusveritas.dynamictrees.DynamicTrees;
import com.ferreusveritas.dynamictrees.api.TreeHelper;
import com.ferreusveritas.dynamictrees.api.client.ModelHelper;
import com.ferreusveritas.dynamictrees.api.treedata.ITreePart;
import com.ferreusveritas.dynamictrees.blocks.BonsaiPotBlock;
import com.ferreusveritas.dynamictrees.blocks.DynamicSaplingBlock;
import com.ferreusveritas.dynamictrees.blocks.branches.ThickBranchBlock;
import com.ferreusveritas.dynamictrees.blocks.leaves.DynamicLeavesBlock;
import com.ferreusveritas.dynamictrees.blocks.leaves.LeavesPaging;
import com.ferreusveritas.dynamictrees.blocks.leaves.LeavesProperties;
import com.ferreusveritas.dynamictrees.blocks.leaves.LeavesPropertiesJson;
import com.ferreusveritas.dynamictrees.blocks.rootyblocks.RootyBlock;
import com.ferreusveritas.dynamictrees.client.BlockColorMultipliers;
import com.ferreusveritas.dynamictrees.client.TextureUtils;
import com.ferreusveritas.dynamictrees.client.thickrings.ThickRingTextureManager;
import com.ferreusveritas.dynamictrees.entities.render.FallingTreeRenderer;
import com.ferreusveritas.dynamictrees.entities.render.LingeringEffectorRenderer;
import com.ferreusveritas.dynamictrees.systems.RootyBlockHelper;
import com.ferreusveritas.dynamictrees.trees.Species;
import com.ferreusveritas.dynamictrees.trees.TreeFamily;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.client.Minecraft;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.RenderTypeLookup;
import net.minecraft.client.renderer.color.BlockColors;
import net.minecraft.client.renderer.model.BakedQuad;
import net.minecraft.client.renderer.model.IBakedModel;
import net.minecraft.client.renderer.texture.AtlasTexture;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.entity.Entity;
import net.minecraft.particles.BasicParticleType;
import net.minecraft.particles.BlockParticleData;
import net.minecraft.particles.ParticleTypes;
import net.minecraft.util.Direction;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.FoliageColors;
import net.minecraft.world.IBlockReader;
import net.minecraft.world.IWorld;
import net.minecraft.world.World;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.model.data.EmptyModelData;
import net.minecraftforge.fml.client.registry.RenderingRegistry;
import net.minecraftforge.registries.ForgeRegistries;
import org.apache.logging.log4j.LogManager;

import java.util.List;
import java.util.Random;
import java.util.function.Function;
import java.util.stream.Collectors;

public class DTClient {
	
	public static void setup() {

		registerRenderLayers();
		registerJsonColorMultipliers();
		registerEntityRenderers();
		
		registerColorHandlers();
//		MinecraftForge.EVENT_BUS.register(BlockBreakAnimationClientHandler.instance);
		
		LeavesPropertiesJson.postInitClient();
	}
	
	@OnlyIn(Dist.CLIENT)
	public static void discoverWoodColors() {

		Function<ResourceLocation, TextureAtlasSprite> bakedTextureGetter = Minecraft.getInstance().getAtlasSpriteGetter(AtlasTexture.LOCATION_BLOCKS_TEXTURE);
		
		for(TreeFamily family : Species.REGISTRY.getValues().stream().map(Species::getFamily).distinct().collect(Collectors.toList())) {
			family.woodRingColor = 0xFFF1AE;
			family.woodBarkColor = 0xB3A979;
			if(family != TreeFamily.NULL_FAMILY) {
				BlockState state = family.getPrimitiveLog().getDefaultState();
				if(state.getBlock() != Blocks.AIR) {
					family.woodRingColor = getFaceColor(state, Direction.DOWN, bakedTextureGetter);
					family.woodBarkColor = getFaceColor(state, Direction.NORTH, bakedTextureGetter);
				}
			}
		}
	}
	@OnlyIn(Dist.CLIENT)
	private static int getFaceColor (BlockState state, Direction face, Function<ResourceLocation, TextureAtlasSprite> textureGetter){
		IBakedModel model = Minecraft.getInstance().getBlockRendererDispatcher().getModelForState(state);
		List<BakedQuad> quads = model.getQuads(state, face, new Random(), EmptyModelData.INSTANCE);
		if (quads.isEmpty()) //if the quad list is empty, means there is no face on that side, so we try with null
			quads = model.getQuads(state, null, new Random(), EmptyModelData.INSTANCE);
		if (quads.isEmpty()) {//if null still returns empty, there is nothing we can do so we just warn and exit
			LogManager.getLogger().warn("Could not get color of "+face+" side for "+ state.getBlock()+"! Branch needs to be handled manually!");
			return 0;
		}
		ResourceLocation resloc = quads.get(0).getSprite().getName(); //Now we get the texture location of that selected face
		if(!resloc.toString().isEmpty()) {
			TextureUtils.PixelBuffer pixbuf = new TextureUtils.PixelBuffer(textureGetter.apply(resloc));
			int u = pixbuf.w / 16;
			TextureUtils.PixelBuffer center = new TextureUtils.PixelBuffer(u * 8, u * 8);
			pixbuf.blit(center, u * -8, u * -8);

			return center.averageColor();
		}
		return 0;
	}
	
	public void cleanUp() {
		BlockColorMultipliers.cleanUp();
	}
	
	private static boolean isValid(IBlockReader access, BlockPos pos) {
		return access != null && pos != null;
	}

	// TODO: Find a cleaner way of doing this.
	private static void registerRenderLayers () {
		ForgeRegistries.BLOCKS.forEach(block -> {
			if (block instanceof DynamicSaplingBlock || block instanceof RootyBlock || block instanceof BonsaiPotBlock) {
				RenderTypeLookup.setRenderLayer(block, RenderType.getCutoutMipped());
			}
//			if (block instanceof ThickBranchBlock)
//				RenderTypeLookup.setRenderLayer(block, ThickRingTextureManager.BRANCH_SOLID);
		});
	}
	
	private static void registerColorHandlers() {
		
		final int white = 0xFFFFFFFF;
		final int magenta = 0x00FF00FF;//for errors.. because magenta sucks.
		
		//BLOCKS
		
		final BlockColors blockColors = Minecraft.getInstance().getBlockColors();
		
		//Register Rooty Colorizers
		for (RootyBlock roots : RootyBlockHelper.generateListForRegistry(false)){
			blockColors.register((state, world, pos, tintIndex) -> {
				switch(tintIndex) {
					case 0: return blockColors.getColor(roots.getPrimitiveDirt().getDefaultState(), world, pos, tintIndex);
					case 1: return state.getBlock() instanceof RootyBlock ? roots.rootColor(state, world, pos) : white;
					default: return white;
				}
			}, roots
					);
		}
		
		//Register Bonsai Pot Colorizer
		ModelHelper.regColorHandler(DTRegistries.bonsaiPotBlock, (state, access, pos, tintIndex) -> isValid(access, pos) && (state.getBlock() instanceof BonsaiPotBlock)
				? DTRegistries.bonsaiPotBlock.getSpecies(access, pos).saplingColorMultiplier(state, access, pos, tintIndex) : white);
		
		//ITEMS
		
		// Register Potion Colorizer
		ModelHelper.regColorHandler(DTRegistries.dendroPotion, (stack, tint) -> DTRegistries.dendroPotion.getColor(stack, tint));
		
		//Register Woodland Staff Colorizer
		ModelHelper.regColorHandler(DTRegistries.treeStaff, (stack, tint) -> DTRegistries.treeStaff.getColor(stack, tint));
		
		//TREE PARTS
		
		//Register Sapling Colorizer
		for (Species species : Species.REGISTRY){
			if (species.getSapling().isPresent()){
				ModelHelper.regColorHandler(species.getSapling().get(), (state, access, pos, tintIndex) ->
				isValid(access, pos) ? species.saplingColorMultiplier(state, access, pos, tintIndex) : white);
			}
		}
		
		//Register Leaves Colorizers
		for(DynamicLeavesBlock leaves: LeavesPaging.getLeavesListForModId(DynamicTrees.MOD_ID)) {
			ModelHelper.regColorHandler(leaves, (state, worldIn, pos, tintIndex) ->{
						LeavesProperties properties = ((DynamicLeavesBlock) state.getBlock()).getProperties(state);
						return TreeHelper.isLeaves(state.getBlock()) ? properties.foliageColorMultiplier(state, worldIn, pos) : magenta;
					}
			);
		}
		
	}
	
	private static void registerJsonColorMultipliers() {
		//Register programmable custom block color providers for LeavesPropertiesJson
		BlockColorMultipliers.register("birch", (state, worldIn,  pos, tintIndex) -> FoliageColors.getBirch());
		BlockColorMultipliers.register("spruce", (state, worldIn,  pos, tintIndex) -> FoliageColors.getSpruce());
	}
	
	public static void registerClientEventHandlers() {
		//        MinecraftForge.EVENT_BUS.register(new ModelBakeEventListener());
		//        MinecraftForge.EVENT_BUS.register(TextureGenerationHandler.class);
	}
	
	private static void registerEntityRenderers() {
		RenderingRegistry.registerEntityRenderingHandler(DTRegistries.fallingTree, new FallingTreeRenderer.Factory());
		RenderingRegistry.registerEntityRenderingHandler(DTRegistries.lingeringEffector, new LingeringEffectorRenderer.Factory());
	}
	
	private static int getFoliageColor(LeavesProperties leavesProperties, World world, BlockState blockState, BlockPos pos) {
		return leavesProperties.foliageColorMultiplier(blockState, world, pos);
	}
	
	///////////////////////////////////////////
	// PARTICLES
	///////////////////////////////////////////
	
	private static void addDustParticle(World world, double fx, double fy, double fz, double mx, double my, double mz, BlockState blockState, float r, float g, float b) {
		if(world.isRemote) {
			Particle particle = Minecraft.getInstance().particles.addParticle(new BlockParticleData(ParticleTypes.BLOCK, blockState), fx, fy, fz, mx, my, mz);
			assert particle != null;
			particle.setColor(r, g, b);
		}
	}

	public static void spawnParticles(World world, BasicParticleType particleType, BlockPos pos, int numParticles, Random random) {
		spawnParticles(world, particleType, pos.getX(), pos.getY(), pos.getZ(), numParticles, random);
	}
	public static void spawnParticles(IWorld world, BasicParticleType particleType, int x, int y, int z, int numParticles, Random random) {
		for (int i1 = 0; i1 < numParticles; ++i1) {
			double mx = random.nextGaussian() * 0.02D;
			double my = random.nextGaussian() * 0.02D;
			double mz = random.nextGaussian() * 0.02D;
			DTClient.spawnParticle(world, particleType, x + random.nextFloat(), (double)y + (double)random.nextFloat(), (double)z + random.nextFloat(), mx, my, mz);
		}
	}
	/** Not strictly necessary. But adds a little more isolation to the server for particle effects */
	public static void spawnParticle(IWorld world, BasicParticleType particleType, double x, double y, double z, double mx, double my, double mz) {
		if(world.isRemote()) {
			world.addParticle(particleType, x, y, z, mx, my, mz);
		}
	}
	
	public static void crushLeavesBlock(World world, BlockPos pos, BlockState blockState, Entity entity) {
		if(world.isRemote) {
			Random random = world.rand;
			ITreePart treePart = TreeHelper.getTreePart(blockState);
			if(treePart instanceof DynamicLeavesBlock) {
				DynamicLeavesBlock leaves = (DynamicLeavesBlock) treePart;
				LeavesProperties leavesProperties = leaves.getProperties(blockState);
				int color = getFoliageColor(leavesProperties, world, blockState, pos);
				float r = (color >> 16 & 255) / 255.0F;
				float g = (color >> 8 & 255) / 255.0F;
				float b = (color & 255) / 255.0F;
				for(int dz = 0; dz < 8; dz++) {
					for(int dy = 0; dy < 8; dy++) {
						for(int dx = 0; dx < 8; dx++) {
							if(random.nextInt(8) == 0) {
								double fx = pos.getX() + dx / 8.0;
								double fy = pos.getY() + dy / 8.0;
								double fz = pos.getZ() + dz / 8.0;
								addDustParticle(world, fx, fy, fz, 0, random.nextFloat() * entity.getMotion().y, 0, blockState, r, g, b);
							}
						}
					}
				}
			}
		}
	}
	
}
