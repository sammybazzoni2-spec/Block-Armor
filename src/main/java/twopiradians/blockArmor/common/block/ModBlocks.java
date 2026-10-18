package twopiradians.blockArmor.common.block;

import java.util.ArrayList;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber.Bus;
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
			event.register(ForgeRegistries.Keys.BLOCKS, helper -> {
				allBlocks.add(MOVING_LIGHT_SOURCE);
				helper.register(new ResourceLocation(BlockArmor.MODID, "moving_light_source"), MOVING_LIGHT_SOURCE);
			});
		}

	}

}
