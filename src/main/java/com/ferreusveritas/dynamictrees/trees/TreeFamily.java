package com.ferreusveritas.dynamictrees.trees;

import com.ferreusveritas.dynamictrees.DynamicTrees;
import com.ferreusveritas.dynamictrees.api.TreeHelper;
import com.ferreusveritas.dynamictrees.api.treedata.ILeavesProperties;
import com.ferreusveritas.dynamictrees.blocks.branches.BasicBranchBlock;
import com.ferreusveritas.dynamictrees.blocks.branches.BranchBlock;
import com.ferreusveritas.dynamictrees.blocks.branches.SurfaceRootBlock;
import com.ferreusveritas.dynamictrees.blocks.branches.ThickBranchBlock;
import com.ferreusveritas.dynamictrees.blocks.leaves.DynamicLeavesBlock;
import com.ferreusveritas.dynamictrees.blocks.leaves.LeavesProperties;
import com.ferreusveritas.dynamictrees.cells.MetadataCell;
import com.ferreusveritas.dynamictrees.compat.WailaOther;
import com.ferreusveritas.dynamictrees.entities.FallingTreeEntity;
import com.ferreusveritas.dynamictrees.entities.animation.IAnimationHandler;
import com.ferreusveritas.dynamictrees.items.Seed;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.*;
import net.minecraft.util.*;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.BlockRayTraceResult;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.IBlockReader;
import net.minecraft.world.IWorld;
import net.minecraft.world.IWorldReader;
import net.minecraft.world.World;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.common.ToolType;
import net.minecraftforge.registries.IForgeRegistry;
import org.apache.logging.log4j.LogManager;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

/**
 * This structure describes a Tree Family whose member Species all have a common wood type.
 *
* A {@link TreeFamily} is more or less just a definition of {@link BranchBlock} blocks.
* It also defines the cellular automata function of the {@link BranchBlock}.  It defines the type of wood that
* the tree is made of and consequently what kind of log you get when you cut it down.
*
* A DynamicTree does not contain a reference to a Seed, Leaves, Sapling, or how it should grow(how fast, how tall, etc).
* It does not control what drops it produces or what fruit it grows.  It does not control where it should grow.
* All of these capabilities lie in the Species class for which a DynamicTree should always contain one default
* species(the common species).
*
* @author ferreusveritas
*/
public class TreeFamily {
	
	public final static TreeFamily NULL_FAMILY = new TreeFamily() {
		@Override public void setCommonSpecies(Species species) {}
		@Override public Species getCommonSpecies() { return Species.NULL_SPECIES; }
		@Override public List<Block> getRegisterableBlocks(List<Block> blockList) { return blockList; }
		@Override public List<Item> getRegisterableItems(List<Item> itemList) { return itemList; }
		@Override public boolean onTreeActivated(World world, BlockPos hitPos, BlockState state, PlayerEntity player, Hand hand, ItemStack heldItem, BlockRayTraceResult hit) { return false; }
		@Override public ItemStack getStick(int qty) { return ItemStack.EMPTY; }
		@Override public BranchBlock getValidBranchBlock(int index) { return null; }
	};
	
	/** Simple name of the tree e.g. "oak" */
	private final ResourceLocation name;

	@Nonnull
	protected Species commonSpecies = Species.NULL_SPECIES;

	//Branches
	/** The dynamic branch used by this tree family */
	private BranchBlock dynamicBranch;
	/** The stripped variant of the branch used by this tree family */
	private BranchBlock dynamicStrippedBranch;
	/** The dynamic branch's block item */
	private Item dynamicBranchItem;
	/** The surface root used by this tree family */
	private SurfaceRootBlock surfaceRoot;
	/** The primitive (vanilla) log to base the texture, drops, and other behavior from */
	private Block primitiveLog = Blocks.AIR;
	/** The primitive stripped log to base the texture, drops, and other behavior from */
	private Block primitiveStrippedLog = Blocks.AIR;

	/** A list of branches the tree accepts as its own. Used for the falling tree renderer */
	private final List<BranchBlock> validBranches = new LinkedList<>();

	//Leaves
	/** Used to modify the getRadiusForCellKit call to create a special case */
	protected boolean hasConiferVariants = false;

	//Misc
	/** The stick that is returned when a whole log can't be dropped */
	private Item stick = null;
	/** Weather the branch can support cocoa pods on it's surface [default = false] */
	public boolean canSupportCocoa = false;

	@OnlyIn(Dist.CLIENT)
	public int woodRingColor;//For rooty blocks
	@OnlyIn(Dist.CLIENT)
	public int woodBarkColor;//For rooty water

	public TreeFamily() {
		this.name = new ResourceLocation(DynamicTrees.MOD_ID, "null");
	}

	/**
	 * Constructor suitable for derivative mods
	 *
	 * @param name The ResourceLocation of the tree e.g. "mymod:poplar"
	 */
	public TreeFamily(ResourceLocation name) {
		this.name = name;

		//this must be the first branch registered, to have its validBranches index be 0;
		setDynamicBranch(createBranch());
		setDynamicBranchItem(createBranchItem(dynamicBranch));

		if (this.hasStrippedBranch()) {
			this.setDynamicStrippedBranch(createBranch("_branch_stripped"));
		}

		if (this.hasSurfaceRoot()) {
			this.setSurfaceRoot(this.createSurfaceRoot());
		}

		stick = Items.STICK;
		createSpecies();
	}

	public void createSpecies() {}

	public void registerSpecies(IForgeRegistry<Species> speciesRegistry) {
		speciesRegistry.register(getCommonSpecies());
	}

	public void setCommonSpecies(@Nonnull Species species) {
		commonSpecies = species;
	}

	public Species getCommonSpecies() {
		return commonSpecies;
	}

	///////////////////////////////////////////
	// SPECIES LOCATION OVERRIDES
	///////////////////////////////////////////

	/**
	 * This is only used by Rooty Dirt to get the appropriate species for this tree.
	 * For instance Oak may use this to select a Swamp Oak species if the coordinates
	 * are in a swamp.
	 *
	 * @param access
	 * @param trunkPos
	 * @return
	 */
	public Species getSpeciesForLocation(IWorld access, BlockPos trunkPos) {
		for(ISpeciesLocationOverride override : speciesLocationOverrides) {
			Species species = override.getSpeciesForLocation(access, trunkPos);
			if(species.isValid()) {
				return species;
			}
		}
		return getCommonSpecies();
	}

	public void addSpeciesLocationOverride(ISpeciesLocationOverride override) {
		speciesLocationOverrides.add(override);
	}

	private final LinkedList<ISpeciesLocationOverride> speciesLocationOverrides = new LinkedList<>();

	public interface ISpeciesLocationOverride {
		Species getSpeciesForLocation(IWorld access, BlockPos trunkPos);
	}

	///////////////////////////////////////////
	// INTERACTION
	///////////////////////////////////////////

	public boolean onTreeActivated(World world, BlockPos hitPos, BlockState state, PlayerEntity player, Hand hand, ItemStack heldItem, BlockRayTraceResult hit) {

		BlockPos rootPos = TreeHelper.findRootNode(world, hitPos);

		if (canStripBranch(state, world, hitPos, player, heldItem)){
			return stripBranch(state, world, hitPos, player, heldItem);
		}

		if(rootPos != BlockPos.ZERO) {
			return TreeHelper.getExactSpecies(world, hitPos).onTreeActivated(world, rootPos, hitPos, state, player, hand, heldItem, hit);
		}

		return false;
	}

	public boolean canStripBranch(BlockState state, World world, BlockPos pos, PlayerEntity player, ItemStack heldItem){
		BranchBlock branchBlock = TreeHelper.getBranch(state);
		if (branchBlock == null) return false;
		return branchBlock.canBeStripped(state, world, pos, player, heldItem);
	}

	public boolean stripBranch(BlockState state, World world, BlockPos pos, PlayerEntity player, ItemStack heldItem){
		if (getDynamicStrippedBranch() != null){
			getDynamicBranch().stripBranch(state, world, pos, player, heldItem);

			if (world.isRemote) {
				world.playSound(player, pos, SoundEvents.ITEM_AXE_STRIP, SoundCategory.BLOCKS, 1.0F, 1.0F);
				WailaOther.invalidateWailaPosition();
			}
			return true;
		}
		else return false;
	}

	///////////////////////////////////////////
	// REGISTRATION
	///////////////////////////////////////////

	/**
	 * Used to register the blocks this tree uses.  Mainly just the {@link BranchBlock}
	 * We intentionally leave out leaves since they are shared between trees
	 * */
	public List<Block> getRegisterableBlocks(List<Block> blockList) {
		blockList.add(getDynamicBranch());
		if (this.hasStrippedBranch())
			blockList.add(getDynamicStrippedBranch());
		if (this.hasSurfaceRoot()) {
			blockList.add(getSurfaceRoot());
		}
		
		getCommonSpecies().getSapling().ifPresent(blockList::add);
		getCommonSpecies().getLeavesBlock().ifPresent(blockList::add);

		return blockList;
	}

	/**
	 * Used to register items the tree creates. Mostly for the {@link Seed}
	 * If the developer provides the seed externally instead of having it
	 * generated internally then the seed should be allowed to register here.
	 * If this can't be the case then override this member function with a
	 * dummy one.
	 */
	public List<Item> getRegisterableItems(List<Item> itemList) {
		//Register an itemBlock for the branch block
		itemList.add(this.dynamicBranchItem);

		getCommonSpecies().getSeed().ifPresent(itemList::add);

		return itemList;
	}


	///////////////////////////////////////////
	// TREE PROPERTIES
	///////////////////////////////////////////

	public ResourceLocation getName() {
		return name;
	}

	public boolean isWood() {
		return true;
	}

	/**
	 * Override this to use a custom branch for the tree family
	 *
	 * @return the branch to be created
	 */
	public BranchBlock createBranch() {
		return createBranch("_branch");
	}
	public BranchBlock createBranch(String postfix) {
		String branchName = this.getName() + postfix;
		BasicBranchBlock branch = isThick() ? new ThickBranchBlock(getBranchMaterial(), branchName) : new BasicBranchBlock(getBranchMaterial(), branchName);
		if (isFireProof()) branch.setFireSpreadSpeed(0).setFlammability(0);
		return branch;
	}

	public Item createBranchItem (BranchBlock branch) {
		return new BlockItem(branch, new Item.Properties()).setRegistryName(branch.getRegistryName());
	}

	protected TreeFamily setDynamicBranch(BranchBlock branch) {
		dynamicBranch = branch;//Link the tree to the branch
		dynamicBranch.setFamily(this);//Link the branch back to the tree
		dynamicBranch.setCanBeStripped(hasStrippedBranch());//Allow the branch to be stripped if a stripped variant exists
		addValidBranches(branch);
		return this;
	}
	protected TreeFamily setDynamicStrippedBranch(BranchBlock branch) {
		dynamicStrippedBranch = branch;//Link the tree to the branch
		dynamicStrippedBranch.setFamily(this);//Link the branch back to the tree
		dynamicStrippedBranch.setCanBeStripped(false);//Already stripped logs should not be able to be stripped again
		addValidBranches(branch);
		return this;
	}

	protected TreeFamily setDynamicBranchItem (Item branchItem) {
		this.dynamicBranchItem = branchItem;
		return this;
	}

	public BranchBlock getDynamicBranch() {
		return dynamicBranch;
	}

	public BranchBlock getDynamicStrippedBranch() {
		return dynamicStrippedBranch;
	}

	public Item getDynamicBranchItem() {
		return dynamicBranchItem;
	}

	public boolean isThick() {
		return false;
	}

	@OnlyIn(Dist.CLIENT)
	public int getRootColor(BlockState state, boolean getBark) {
		return getBark? woodBarkColor : woodRingColor;
	}

	/**
	 * Used to set the type of stick that a tree drops when there's not enough wood volume for a log.
	 *
	 * @param item An itemstack of the stick
	 * @return TreeFamily for chaining calls
	 */
	protected TreeFamily setStick(Item item) {
		stick = item;
		return this;
	}

	/**
	 * Get a quantity of whatever is considered a stick for this tree's type of wood.
	 *
	 * @param qty Number of sticks
	 * @return an {@link ItemStack} of sticky things
	 */
	public ItemStack getStick(int qty) {
		return new ItemStack(stick, MathHelper.clamp(qty, 0, 64));
	}

	/**
	 * Used to set the type of log item that a tree drops when it's harvested.
	 * Use this function to explicitly set the itemstack instead of having it
	 * done automatically.
	 *
	 * @param primLog A block object that is the log
	 * @param primLog An itemStack of the log item
	 * @return TreeFamily for chaining calls
	 */
	protected TreeFamily setPrimitiveLog(Block primLog) {
		primitiveLog = primLog;
		dynamicBranch.setPrimitiveLogDrops(new ItemStack(primLog));
		return this;
	}
	protected TreeFamily setPrimitiveStrippedLog(Block primLog) {
		primitiveStrippedLog = primLog;
		if (hasStrippedBranch())
			dynamicStrippedBranch.setPrimitiveLogDrops(new ItemStack(primLog));
		return this;
	}

	/**
	 * Gets the primitive full block (vanilla)log that represents this tree's
	 * material. Chiefly used to determine the wood hardness for harvesting
	 * behavior.
	 *
	 * @return Block of the primitive log.
	 */
	public Block getPrimitiveLog() {
		return primitiveLog;
	}

	public Block getPrimitiveStrippedLog() {
		return primitiveStrippedLog;
	}

	private List<ItemStack> getLogDropsForBranch(float volume, int branch) {
		BranchBlock branchBlock = getValidBranchBlock(branch);
		List<ItemStack> logs = new LinkedList<>();
		if (branchBlock != null){
			branchBlock.getPrimitiveLogs(volume, logs);
		}
		return logs;
	}

	public boolean isFireProof () { return false; }

	public SoundType getBranchSoundType (BlockState state, IWorldReader world, BlockPos pos, @Nullable Entity entity){
		return SoundType.WOOD;
	}

	public ToolType getBranchHarvestTool(BlockState state){
		return ToolType.AXE;
	}

	public int getBranchHarvestLevel(BlockState state){
		return 0;
	}

	public Material getBranchMaterial(){
		return Material.WOOD; //Trees are made of wood. Brilliant.
	}

	///////////////////////////////////////////
	//BRANCHES
	///////////////////////////////////////////

	public int getRadiusForCellKit(IBlockReader blockAccess, BlockPos pos, BlockState blockState, Direction dir, BranchBlock branch) {
		int radius = branch.getRadius(blockState);
		int meta = MetadataCell.NONE;
		if(hasConiferVariants && radius == 1) {
			if(blockAccess.getBlockState(pos.down()).getBlock() == branch) {
				meta = MetadataCell.CONIFERTOP;
			}
		}

		return MetadataCell.radiusAndMeta(radius, meta);
	}

	/** Thickness of a twig.. Should always be 1 unless the tree has no leaves(like a cactus) [default = 1] */
	public float getPrimaryThickness() {
		return 1.0f;
	}

	/** Thickness of the branch connected to a twig(radius == 1).. This should probably always be 2 [default = 2] */
	public float getSecondaryThickness() {
		return 2.0f;
	}

	public boolean hasStrippedBranch(){
		return true;
	}

	public void addValidBranches (BranchBlock... branches){
		this.validBranches.addAll(Arrays.asList(branches));
	}

	public int getBranchBlockIndex (BranchBlock block){
		int index = this.validBranches.indexOf(block);
		if (index < 0){
			LogManager.getLogger().warn("Block {} not valid branch for {}.", block, this);
			return 0;
		}
		return index;
	}

	@Nullable
	public BranchBlock getValidBranchBlock (int index) {
		return this.validBranches.get(index);
	}

	///////////////////////////////////////////
	// SURFACE ROOTS
	///////////////////////////////////////////

	public boolean hasSurfaceRoot () {
		return false;
	}

	public SurfaceRootBlock createSurfaceRoot () {
		String surfaceRootName = this.getName() + "_root";
		return new SurfaceRootBlock(surfaceRootName, this);
	}

	public SurfaceRootBlock getSurfaceRoot() {
		return this.surfaceRoot;
	}

	protected TreeFamily setSurfaceRoot (SurfaceRootBlock surfaceRoot) {
		this.surfaceRoot = surfaceRoot;
		return this;
	}

	///////////////////////////////////////////
	// FALL ANIMATION HANDLING
	///////////////////////////////////////////

	public IAnimationHandler selectAnimationHandler(FallingTreeEntity fallingEntity) {
		return fallingEntity.defaultAnimationHandler();
	}

	///////////////////////////////////////////
	// LEAVES HANDLING
	///////////////////////////////////////////

	public boolean isCompatibleDynamicLeaves(BlockState blockState, IBlockReader blockAccess, BlockPos pos) {
		DynamicLeavesBlock leaves = TreeHelper.getLeaves(blockState);
		return (leaves != null) && this == leaves.getFamily(blockState, blockAccess, pos);
	}

	public interface IConnectable {
		boolean isConnectable(BlockState blockState);
	}

	LinkedList<IConnectable> vanillaConnectables = new LinkedList<>();

	public void addConnectableVanillaLeaves(IConnectable connectable) {
		vanillaConnectables.add(connectable);
	}

	public boolean isCompatibleVanillaLeaves(BlockState blockState, IBlockReader blockAccess, BlockPos pos) {

		Block block = blockState.getBlock();

		if(!(block instanceof DynamicLeavesBlock)) {
			for(IConnectable connectable : vanillaConnectables) {
				if(connectable.isConnectable(blockState)) {
					return true;
				}
			}
		}

		return false;
	}

	public boolean isCompatibleGenericLeaves(BlockState blockState, IWorld blockAccess, BlockPos pos) {
		return isCompatibleDynamicLeaves(blockState, blockAccess, pos) || isCompatibleVanillaLeaves(blockState, blockAccess, pos);
	}

	public ILeavesProperties getCommonLeaves() {
		return LeavesProperties.NULL_PROPERTIES;
	}

	//////////////////////////////
	// JAVA OBJECT STUFF
	//////////////////////////////

	@Override
	public String toString() {
		return getName().toString();
	}
	
}