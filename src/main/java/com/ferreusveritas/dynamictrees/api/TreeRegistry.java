package com.ferreusveritas.dynamictrees.api;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.ferreusveritas.dynamictrees.api.treedata.IBiomeSuitabilityDecider;
import com.ferreusveritas.dynamictrees.api.treedata.ISpecies;
import com.ferreusveritas.dynamictrees.trees.DynamicTree;
import com.ferreusveritas.dynamictrees.trees.Species;

import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;

/**
* A registry for all of the dynamic trees. Use this for this mod or other mods.
* 
* @author ferreusveritas
*/
public class TreeRegistry {

	private static Map<String, ISpecies> treesByName = new HashMap<String, ISpecies>();
	private static Map<String, ISpecies> treesByFullName = new HashMap<String, ISpecies>();
	private static ArrayList<IBiomeSuitabilityDecider> biomeSuitabilityDeciders = new ArrayList<IBiomeSuitabilityDecider>();
	
	//////////////////////////////
	// TREE REGISTRY
	//////////////////////////////
	
	/**
	 * Mods should use this to register their {@link DynamicTree}
	 * 
	 * Places the tree in a central registry.
	 * The proper place to use this is during the preInit phase of your mod.
	 * 
	 * @param species The dynamic tree being registered
	 * @return DynamicTree for chaining
	 */
	public static ISpecies registerSpecies(ISpecies species) {
		treesByName.put(species.getName(), species);
		String fullName = TreeHelper.getSpeciesFullName(species);
		treesByFullName.put(fullName, species);
		return species;
	}

	/**
	 * Method for registering a list of trees
	 * 
	 * @param list
	 */
	public static void registerSpecies(List<ISpecies> list) {
		for(ISpecies species: list) {
			registerSpecies(species);
		}
	}
	
	/**
	 * Searches first for the full tree name.  If that fails then it
	 * will find the first tree matching the simple name and return it instead otherwise null
	 * 
	 * @param name The name of the tree.  Either the simple name or the full name
	 * @return The tree that was found or null if not found
	 */
	public static ISpecies findSpecies(String name) {
		ISpecies tree = treesByFullName.get(name);
		
		if(tree == null) {
			tree = treesByName.get(name);
		}
		
		return tree;
	}
	
	/**
	 * Convenience function that uses the modId and name for
	 * the search.
	 * 
	 * @param modId
	 * @param name
	 * @return
	 */
	public static ISpecies findTree(String modId, String name) {
		return findSpecies(modId + ":" + name);
	}
	
	//////////////////////////////
	// BIOME HANDLING
	//////////////////////////////
	
	/**
	 * Mods should call this to register an {@link IBiomeSuitabilityDecider}
	 * 
	 * @param decider The decider being registered
	 */
	public static void registerBiomeSuitabilityDecider(IBiomeSuitabilityDecider decider) {
		biomeSuitabilityDeciders.add(decider);
	}
	
	private static final IBiomeSuitabilityDecider.Decision undecided = new IBiomeSuitabilityDecider.Decision();
	
	public static IBiomeSuitabilityDecider.Decision getBiomeSuitability(World world, Biome biome, Species species, BlockPos pos) {
		for(IBiomeSuitabilityDecider decider: biomeSuitabilityDeciders) {
			IBiomeSuitabilityDecider.Decision decision = decider.getBiomeSuitability(world, biome, species, pos);
			if(decision.isHandled()) {
				return decision;
			}
		}
		
		return undecided;
	}
	
	public static boolean isBiomeSuitabilityOverrideEnabled() {
		return !biomeSuitabilityDeciders.isEmpty();
	}
	
}
