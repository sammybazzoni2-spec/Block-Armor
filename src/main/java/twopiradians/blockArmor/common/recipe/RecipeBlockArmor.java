package twopiradians.blockArmor.common.recipe;

import net.minecraft.core.NonNullList;
import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.ShapedRecipe;
import twopiradians.blockArmor.common.item.ArmorSet;

public class RecipeBlockArmor extends ShapedRecipe {

	private ArmorSet set;

	public RecipeBlockArmor(ResourceLocation loc, ArmorSet set, String group, CraftingBookCategory category, int width, int height, NonNullList<Ingredient> ingredients, ItemStack result) {
		super(loc, group, category, width, height, ingredients, result);
		this.set = set;
	}
	
	@Override
	public ItemStack getResultItem(RegistryAccess registryAccess) {
		return set.isEnabled() ? super.getResultItem(registryAccess) : ItemStack.EMPTY;
	}

}
