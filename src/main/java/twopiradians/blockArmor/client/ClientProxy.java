package twopiradians.blockArmor.client;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import com.google.common.collect.Maps;

import net.minecraft.client.KeyMapping;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.client.gui.screens.inventory.ContainerScreen;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Tuple;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.ModelEvent;
import net.minecraftforge.client.event.RegisterColorHandlersEvent;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber.Bus;
import twopiradians.blockArmor.client.key.KeyActivateSetEffect;
import twopiradians.blockArmor.client.model.ModelBAArmor;
import twopiradians.blockArmor.client.model.ModelBAItem;
import twopiradians.blockArmor.common.BlockArmor;
import twopiradians.blockArmor.common.CommonProxy;
import twopiradians.blockArmor.common.item.ArmorSet;
import twopiradians.blockArmor.common.item.BlockArmorItem;
import twopiradians.blockArmor.common.item.ModItems;
import twopiradians.blockArmor.common.seteffect.SetEffectHoarder;

@Mod.EventBusSubscriber(Dist.CLIENT)
public class ClientProxy {

	/**Map of models to their constructor fields - generated as needed*/
	private static HashMap<String, ModelBAArmor> modelMaps = Maps.newHashMap();

	@Mod.EventBusSubscriber(value = Dist.CLIENT, bus = Bus.MOD)
	public static class RegistrationHandler {

		/**Armor items don't have their own model json, so add a placeholder model for each 
		 * (it gets its quads in {@link #onModelsBaked(net.minecraftforge.client.event.ModelEvent.BakingCompleted)} once the block textures are available)*/
		@SubscribeEvent
		public static void onModifyBakingResult(ModelEvent.ModifyBakingResult event) {
			for (BlockArmorItem item : ModItems.allArmors)
				event.getModels().put(ModelBAItem.getLocation(item), new ModelBAItem.DynamicBlockArmorModel());
		}

		/**Block textures and models are available - map them to the armor*/
		@SubscribeEvent
		public static void onModelsBaked(ModelEvent.BakingCompleted event) {
			mapTextures(event.getModels());
		}

		/**Tints the icon layers with the block's color (i.e. grass)*/
		@SubscribeEvent
		public static void registerItemColors(RegisterColorHandlersEvent.Item event) {
			event.register(ModelBAItem::getTintColor, ModItems.allArmors.toArray(new BlockArmorItem[0]));
		}

		@SubscribeEvent
		public static void registerKeys(RegisterKeyMappingsEvent event) {
			KeyActivateSetEffect.ACTIVATE_SET_EFFECT = new KeyMapping("Activate Set Effect", 82, BlockArmor.MODNAME);
			event.register(KeyActivateSetEffect.ACTIVATE_SET_EFFECT);
		}

	}

	/**Set world time*/
	public static void setWorldTime(Level world, long time) {
		if (world instanceof ClientLevel)
			((ClientLevel)world).setDayTime(time);
		else
			CommonProxy.setWorldTime(world, time);
	}

	public static void setup() {
		// Hoarder container types
		registerScreenContainerTypes();
	}

	@OnlyIn(Dist.CLIENT)
	public static void registerScreenContainerTypes() {
		MenuScreens.register(SetEffectHoarder.containerType_9x1, ContainerScreen::new);
		MenuScreens.register(SetEffectHoarder.containerType_9x2, ContainerScreen::new);
		MenuScreens.register(SetEffectHoarder.containerType_9x3, ContainerScreen::new);
		MenuScreens.register(SetEffectHoarder.containerType_9x4, ContainerScreen::new);
		MenuScreens.register(SetEffectHoarder.containerType_9x5, ContainerScreen::new);
		MenuScreens.register(SetEffectHoarder.containerType_9x6, ContainerScreen::new);
	}

	/**Get model based on model's constructor parameters*/
	public static Object getBlockArmorModel(LivingEntity entity, int height, int width, int currentFrame, int nextFrame, EquipmentSlot slot) {
		String key = height+"_"+width+"_"+currentFrame+"_"+nextFrame+"_"+slot.getName();
		ModelBAArmor model = modelMaps.get(key);
		if (model == null) { 
			model = new ModelBAArmor(height, width, currentFrame, nextFrame, slot);
			modelMaps.put(key, model);
		}
		model.entity = entity;
		return model;
	}

	/**Resets models and maps block textures (called when models are baked: on startup and when resources are reloaded)*/
	public static void mapTextures(Map<net.minecraft.client.resources.model.ModelResourceLocation, BakedModel> models) {
		// reset model map and animation states
		modelMaps = Maps.newHashMap();
		ArmorSet.spriteTickers = null;

		// find block textures
		ArrayList<ArmorSet> setsToDisable = new ArrayList<ArmorSet>();
		int numTextures = 0;
		for (ArmorSet set : ArmorSet.allSets) {
			Tuple<Integer, Boolean> tup = set.initTextures();
			numTextures += tup.getA();
			if (tup.getB())
				setsToDisable.add(set);
		}

		// textures not loaded yet
		if (numTextures == 0) 
			return;
		// disable sets with missing textures
		else if (!setsToDisable.isEmpty()) { 
			int disabledSets = 0;
			for (ArmorSet set : setsToDisable) {
				set.missingTextures = true;
				set.disable();
				disabledSets++;
			}
			BlockArmor.LOGGER.info("Disabled "+disabledSets+" armor set"+(disabledSets > 1 ? "s" : "")+" without textures");
		}

		BlockArmor.LOGGER.info("Found "+numTextures+" block textures for Block Armor");

		//create inventory icons
		int numIcons = ModelBAItem.createInventoryIcons(models);
		BlockArmor.LOGGER.info("Created "+numIcons+" inventory icons for Block Armor");
	}

}
