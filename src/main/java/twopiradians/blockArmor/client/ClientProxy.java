package twopiradians.blockArmor.client;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import com.google.common.collect.Maps;

import net.minecraft.client.KeyMapping;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.client.gui.screens.inventory.ContainerScreen;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Tuple;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.ModelEvent;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.client.event.TextureStitchEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber.Bus;
import net.minecraftforge.registries.ForgeRegistries;
import twopiradians.blockArmor.client.key.KeyActivateSetEffect;
import twopiradians.blockArmor.client.model.ModelBAArmor;
import twopiradians.blockArmor.client.model.ModelBAItem;
import twopiradians.blockArmor.common.BlockArmor;
import twopiradians.blockArmor.common.CommonProxy;
import twopiradians.blockArmor.common.block.ModBlocks;
import twopiradians.blockArmor.common.item.ArmorSet;
import twopiradians.blockArmor.common.item.BlockArmorItem;
import twopiradians.blockArmor.common.item.ModItems;
import twopiradians.blockArmor.common.item.TextureOverrideInfo;
import twopiradians.blockArmor.common.item.TextureOverrideInfo.Info;
import twopiradians.blockArmor.common.seteffect.SetEffectHoarder;

@Mod.EventBusSubscriber(Dist.CLIENT)
public class ClientProxy {

	/**Map of models to their constructor fields - generated as needed*/
	private static HashMap<String, ModelBAArmor> modelMaps = Maps.newHashMap();

	@Mod.EventBusSubscriber(value = Dist.CLIENT, bus = Bus.MOD)
	public static class RegistrationHandler {

		/**Used to register block textures to override inventory textures and for inventory icons*/
		@SubscribeEvent
		public static void textureStitch(TextureStitchEvent.Pre event) {
			if (event.getAtlas().location() == TextureAtlas.LOCATION_BLOCKS) {
				//textures for overriding
				for (TextureOverrideInfo override : ArmorSet.TEXTURE_OVERRIDES.values())
					for (Info info : override.overrides.values())
						event.addSprite(info.shortLoc);
				//textures for inventory icons
				event.addSprite(new ResourceLocation(BlockArmor.MODID, "items/icons/block_armor_helmet_base"));
				event.addSprite(new ResourceLocation(BlockArmor.MODID, "items/icons/block_armor_helmet_cover"));
				event.addSprite(new ResourceLocation(BlockArmor.MODID, "items/icons/block_armor_helmet1_template"));
				event.addSprite(new ResourceLocation(BlockArmor.MODID, "items/icons/block_armor_helmet2_template"));
				event.addSprite(new ResourceLocation(BlockArmor.MODID, "items/icons/block_armor_chestplate_base"));
				event.addSprite(new ResourceLocation(BlockArmor.MODID, "items/icons/block_armor_chestplate_cover"));
				event.addSprite(new ResourceLocation(BlockArmor.MODID, "items/icons/block_armor_chestplate1_template"));
				event.addSprite(new ResourceLocation(BlockArmor.MODID, "items/icons/block_armor_chestplate2_template"));
				event.addSprite(new ResourceLocation(BlockArmor.MODID, "items/icons/block_armor_leggings_base"));
				event.addSprite(new ResourceLocation(BlockArmor.MODID, "items/icons/block_armor_leggings_cover"));
				event.addSprite(new ResourceLocation(BlockArmor.MODID, "items/icons/block_armor_leggings1_template"));
				event.addSprite(new ResourceLocation(BlockArmor.MODID, "items/icons/block_armor_leggings2_template"));
				event.addSprite(new ResourceLocation(BlockArmor.MODID, "items/icons/block_armor_boots_base"));
				event.addSprite(new ResourceLocation(BlockArmor.MODID, "items/icons/block_armor_boots_cover"));
				event.addSprite(new ResourceLocation(BlockArmor.MODID, "items/icons/block_armor_boots1_template"));
				event.addSprite(new ResourceLocation(BlockArmor.MODID, "items/icons/block_armor_boots2_template"));
			}
		}

		/**Map textures after the last textures (mob_effects) are stitched*/
		@SubscribeEvent
		public static void textureStitch(TextureStitchEvent.Post event) {
			if (event.getAtlas().location().getPath().equals("textures/atlas/mob_effects.png")) 
				mapTextures();
		}

		/**Give each armor item its own dynamic inventory model 
		 * (the model's quads are filled in later from the block's textures - see ModelBAItem)*/
		@SubscribeEvent
		public static void onModelBake(ModelEvent.BakingCompleted event) {
			Map<ResourceLocation, BakedModel> models = event.getModels();
			for (BlockArmorItem item : ModItems.allArmors)
				models.put(new ModelResourceLocation(ForgeRegistries.ITEMS.getKey(item), "inventory"), ModelBAItem.createModel());
			ModItems.registerRenders();
			ModBlocks.registerRenders();
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

	/**Resets model and item quads and maps block textures (called when client joins world or resource pack loaded)*/
	public static void mapTextures() {
		// reset model and item quad maps
		modelMaps = Maps.newHashMap();

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
		int numIcons = ModelBAItem.BakedDynBlockArmorOverrideHandler.createInventoryIcons();
		BlockArmor.LOGGER.info("Created "+numIcons+" inventory icons for Block Armor");
	}

}