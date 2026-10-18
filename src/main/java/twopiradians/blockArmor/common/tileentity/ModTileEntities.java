package twopiradians.blockArmor.common.tileentity;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber.Bus;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegisterEvent;
import twopiradians.blockArmor.common.BlockArmor;
import twopiradians.blockArmor.common.block.ModBlocks;

public class ModTileEntities{
	
	@Mod.EventBusSubscriber(modid = BlockArmor.MODID, bus = Bus.MOD)
	public static class RegistrationHandler {

		@SubscribeEvent
		public static void registerTE(final RegisterEvent event) {
			event.register(ForgeRegistries.Keys.BLOCK_ENTITY_TYPES, helper -> {
				TileEntityMovingLightSource.type = BlockEntityType.Builder.of(TileEntityMovingLightSource::new, ModBlocks.MOVING_LIGHT_SOURCE).build(null);
				helper.register(new ResourceLocation(BlockArmor.MODID, "light_tile_entity"), TileEntityMovingLightSource.type);
			});
		}
	}

}
