package twopiradians.blockArmor.common.block;

import java.util.ArrayList;

import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber.Bus;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegisterEvent;
import twopiradians.blockArmor.common.BlockArmor;

public class ModBlocks {
	
	public static final BlockMovingLightSource MOVING_LIGHT_SOURCE = new BlockMovingLightSource();

	public static ArrayList<Block> allBlocks = new ArrayList<Block>();

	@Mod.EventBusSubscriber(bus = Bus.MOD)
	public static class RegistrationHandler {

		@SubscribeEvent
		public static void registerBlocks(final RegisterEvent event) {
			event.register(ForgeRegistries.Keys.BLOCKS, helper -> 
				register(helper, MOVING_LIGHT_SOURCE, "moving_light_source"));
		}

		private static void register(RegisterEvent.RegisterHelper<Block> helper, Block block, String blockName) {
			allBlocks.add(block);
			helper.register(new ResourceLocation(BlockArmor.MODID, blockName), block);
		}

	}

	@SuppressWarnings("deprecation")
	public static void registerRenders() {
		for (Block block : allBlocks)
			Minecraft.getInstance().getItemRenderer().getItemModelShaper().register
			(Item.byBlock(block), new ModelResourceLocation(ForgeRegistries.BLOCKS.getKey(block), "inventory"));
	}

}