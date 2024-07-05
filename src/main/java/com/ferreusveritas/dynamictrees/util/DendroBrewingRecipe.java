package com.ferreusveritas.dynamictrees.util;

import com.ferreusveritas.dynamictrees.item.DendroPotion;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.Potion;
import net.minecraft.world.item.alchemy.PotionUtils;
import net.minecraftforge.common.brewing.IBrewingRecipe;

/**
 * An implementation of {@link IBrewingRecipe} for the {@link DendroPotion} item.
 *
 * @author Harley O'Connor
 */
public final class DendroBrewingRecipe implements IBrewingRecipe {

    private final ItemStack input;
    private final ItemStack ingredient;
    private final ItemStack output;

    public DendroBrewingRecipe(final ItemStack input, final ItemStack ingredient, final ItemStack output) {
        this.input = input;
        this.ingredient = ingredient;
        this.output = output;
    }

    @Override
    public boolean isInput(final ItemStack inputStack) {
	// Only allow Potion.AWKWARD or DendroPotion and for DendroPotion.TRANSFORM, only if it doesn't already have a tree tag.
	if ((!inputStack.getOrCreateTag().contains(DendroPotion.TREE_TAG_KEY) && inputStack.getOrCreateTag().contains(DendroPotion.INDEX_TAG_KEY)) || inputStack.getItem() == Items.POTION) {
		return true;
	}
		return false;
	}

    @Override
    public boolean isIngredient(final ItemStack ingredientStack) {
	    return this.ingredient.getItem().equals(ingredientStack.getItem());
    }

    @Override
    public ItemStack getOutput(final ItemStack inputStack, final ItemStack ingredientStack) {
	    // We need to apply logic for the brewing or simply the ingredient defines the output and any input was allowed
	    // A smarter way would be nice, but it works
	    if (!inputStack.isEmpty() && !ingredientStack.isEmpty() && isIngredient(ingredientStack) && isInput(inputStack)) {
		    if (ingredientStack.is(Items.CHARCOAL) & PotionUtils.getPotion(inputStack) == Potion.byName("awkward")) {
			    return this.output.copy();
		    }
		    if (ingredientStack.is(Items.CHARCOAL) | inputStack.getItem() == Items.POTION) {
			    return ItemStack.EMPTY;
		    }
		    if ((ingredientStack.is(Items.SLIME_BALL) || ingredientStack.is(Items.PUMPKIN_SEEDS) || ingredientStack.is(Items.GHAST_TEAR) || ingredientStack.is(Items.PRISMARINE_CRYSTALS)) & DendroPotion.getPotionType(inputStack) == DendroPotion.DendroPotionType.BIOCHAR) {
			    return this.output.copy();
		    }
		    if ((ingredientStack.is(Items.SLIME_BALL) || ingredientStack.is(Items.PUMPKIN_SEEDS) || ingredientStack.is(Items.GHAST_TEAR) || ingredientStack.is(Items.PRISMARINE_CRYSTALS)) | DendroPotion.getPotionType(inputStack) != DendroPotion.DendroPotionType.TRANSFORM) {
			    return ItemStack.EMPTY;
		    }
		    return this.output.copy();
	    }
	    return ItemStack.EMPTY;
    }
	public ItemStack getInput() {
		return input;
	}
	public ItemStack getIngredient() {
		return ingredient;
	}
	public ItemStack getOutput() {
		return output;
	}
}
