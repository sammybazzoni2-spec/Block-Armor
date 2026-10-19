package twopiradians.blockArmor.creativetab;

import java.util.ArrayList;

import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber.Bus;
import net.minecraftforge.registries.RegisterEvent;
import twopiradians.blockArmor.common.BlockArmor;
import twopiradians.blockArmor.common.item.ArmorSet;

/**Creative tabs for vanilla and modded armor. Contents are filled from the (config-dependent) list of enabled sets.*/
@Mod.EventBusSubscriber(bus = Bus.MOD)
public class BlockArmorCreativeTab {

	public static CreativeModeTab vanillaTab;
	public static CreativeModeTab moddedTab;
	/**Ordered stacks for each tab - modified by ArmorSet#enable and ArmorSet#disable*/
	public static final ArrayList<ItemStack> VANILLA_STACKS = new ArrayList<ItemStack>();
	public static final ArrayList<ItemStack> MODDED_STACKS = new ArrayList<ItemStack>();

	@SubscribeEvent
	public static void register(RegisterEvent event) {
		event.register(Registries.CREATIVE_MODE_TAB, helper -> {
			vanillaTab = CreativeModeTab.builder()
					.title(Component.translatable("itemGroup.blockArmorVanilla"))
					.icon(() -> makeIcon(false))
					.displayItems((params, output) -> {
						for (ItemStack stack : VANILLA_STACKS)
							output.accept(stack);
					}).build();
			helper.register(ResourceLocation.fromNamespaceAndPath(BlockArmor.MODID, "vanilla"), vanillaTab);
			moddedTab = CreativeModeTab.builder()
					.title(Component.translatable("itemGroup.blockArmorModded"))
					.icon(() -> makeIcon(true))
					.displayItems((params, output) -> {
						for (ItemStack stack : MODDED_STACKS)
							output.accept(stack);
					}).build();
			helper.register(ResourceLocation.fromNamespaceAndPath(BlockArmor.MODID, "modded"), moddedTab);
		});
	}

	private static ItemStack makeIcon(boolean modded) {
		ArrayList<ItemStack> stacks = modded ? MODDED_STACKS : VANILLA_STACKS;
		if (modded && stacks.size() > 2)
			return stacks.get(1);
		else if (ArmorSet.getSet(Blocks.BEDROCK) != null && ArmorSet.getSet(Blocks.BEDROCK).chestplate != null)
			return new ItemStack(ArmorSet.getSet(Blocks.BEDROCK).chestplate);
		else if (stacks.size() > 2)
			return stacks.get(1);
		else
			return new ItemStack(Items.IRON_CHESTPLATE);
	}

}
