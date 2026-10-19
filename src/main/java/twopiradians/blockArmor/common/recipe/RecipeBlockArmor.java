package twopiradians.blockArmor.common.recipe;

import net.minecraft.core.NonNullList;
import java.util.Optional;

import net.minecraft.core.HolderLookup;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.ShapedRecipe;
import net.minecraft.world.item.crafting.ShapedRecipePattern;
import twopiradians.blockArmor.common.item.ArmorSet;

public class RecipeBlockArmor extends ShapedRecipe {

	private ArmorSet set;

	public RecipeBlockArmor(ArmorSet set, String group, CraftingBookCategory category, int width, int height, NonNullList<Ingredient> ingredients, ItemStack result) {
		super(group, category, new ShapedRecipePattern(width, height, ingredients, Optional.empty()), result);
		this.set = set;
	}
	
	@Override
	public ItemStack getResultItem(HolderLookup.Provider registryAccess) {
		return set.isEnabled() ? super.getResultItem(registryAccess) : ItemStack.EMPTY;
	}

}
