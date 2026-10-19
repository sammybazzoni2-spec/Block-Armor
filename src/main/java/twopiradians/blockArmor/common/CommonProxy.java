package twopiradians.blockArmor.common;

import java.lang.reflect.Field;
import java.util.Map;
import java.util.Optional;

import javax.annotation.Nonnull;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;

import net.minecraft.core.NonNullList;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.entity.player.PlayerEvent.PlayerLoggedInEvent;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber.Bus;
import net.minecraftforge.fml.util.ObfuscationReflectionHelper;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegisterEvent;
import twopiradians.blockArmor.common.command.CommandDev;
import twopiradians.blockArmor.common.item.ArmorSet;
import twopiradians.blockArmor.common.recipe.RecipeBlockArmor;
import twopiradians.blockArmor.common.seteffect.SetEffectAutoSmelt.EnchantmentSmeltingModifier;
import twopiradians.blockArmor.common.seteffect.SetEffectLucky.SetEffectLuckyModifier;
import twopiradians.blockArmor.packet.CActivateSetEffectPacket;
import twopiradians.blockArmor.packet.SDevColorsPacket;
import twopiradians.blockArmor.packet.SSyncCooldownsPacket;

@Mod.EventBusSubscriber
public class CommonProxy {

	@Mod.EventBusSubscriber(bus = Bus.MOD)
	public static class RegistrationHandler {

		@SubscribeEvent
		public static void registerModifierSerializers(@Nonnull final RegisterEvent event) {
			event.register(ForgeRegistries.Keys.GLOBAL_LOOT_MODIFIER_SERIALIZERS, helper -> {
				helper.register(new ResourceLocation(BlockArmor.MODID,"set_effect_autosmelt"), EnchantmentSmeltingModifier.CODEC.get());
				helper.register(new ResourceLocation(BlockArmor.MODID,"set_effect_lucky"), SetEffectLuckyModifier.CODEC.get());
			});
		}

	}

	public static void setup() {
		registerPackets();
	}
	
	private static void registerPackets() {
		int id = 0;
		BlockArmor.NETWORK.registerMessage(id++, SDevColorsPacket.class, SDevColorsPacket::encode, SDevColorsPacket::decode, SDevColorsPacket.Handler::handle, Optional.of(NetworkDirection.PLAY_TO_CLIENT));
		BlockArmor.NETWORK.registerMessage(id++, CActivateSetEffectPacket.class, CActivateSetEffectPacket::encode, CActivateSetEffectPacket::decode, CActivateSetEffectPacket.Handler::handle, Optional.of(NetworkDirection.PLAY_TO_SERVER));
		BlockArmor.NETWORK.registerMessage(id++, SSyncCooldownsPacket.class, SSyncCooldownsPacket::encode, SSyncCooldownsPacket::decode, SSyncCooldownsPacket.Handler::handle, Optional.of(NetworkDirection.PLAY_TO_CLIENT));
	}
	
	@SubscribeEvent
	public static void registerCommands(RegisterCommandsEvent event) {
		CommandDev.register(event.getDispatcher());
	}

	@SubscribeEvent
	public static void serverStart(ServerStartedEvent event) {
		registerRecipes(event.getServer());
	}
	
	private static void registerRecipes(MinecraftServer server) {
		try {
			Field recipesField = ObfuscationReflectionHelper.findField(RecipeManager.class, "f_44007_");
			Map<RecipeType<?>, Map<ResourceLocation, Recipe<?>>> recipes = Maps.newHashMap((Map<RecipeType<?>, Map<ResourceLocation, Recipe<?>>>) recipesField.get(server.getRecipeManager()));
			for (ArmorSet set : ArmorSet.allSets) {
				if (set.isEnabled()) {
					NonNullList<Ingredient> helmetRecipe = NonNullList.of(Ingredient.EMPTY,
							Ingredient.of(set.getStack()), Ingredient.of(set.getStack()), Ingredient.of(set.getStack()),
							Ingredient.of(set.getStack()), Ingredient.EMPTY, Ingredient.of(set.getStack()));

					NonNullList<Ingredient> armorRecipe = NonNullList.of(Ingredient.EMPTY,
							Ingredient.of(set.getStack()), Ingredient.EMPTY, Ingredient.of(set.getStack()),
							Ingredient.of(set.getStack()), Ingredient.of(set.getStack()), Ingredient.of(set.getStack()),
							Ingredient.of(set.getStack()), Ingredient.of(set.getStack()), Ingredient.of(set.getStack()));

					NonNullList<Ingredient> legsRecipe = NonNullList.of(Ingredient.EMPTY,
							Ingredient.of(set.getStack()), Ingredient.of(set.getStack()), Ingredient.of(set.getStack()),
							Ingredient.of(set.getStack()), Ingredient.EMPTY, Ingredient.of(set.getStack()),
							Ingredient.of(set.getStack()), Ingredient.EMPTY, Ingredient.of(set.getStack()));

					NonNullList<Ingredient> bootsRecipe = NonNullList.of(Ingredient.EMPTY,
							Ingredient.of(set.getStack()), Ingredient.EMPTY, Ingredient.of(set.getStack()),
							Ingredient.of(set.getStack()), Ingredient.EMPTY, Ingredient.of(set.getStack()));

					Map<ResourceLocation, Recipe<?>> map = Maps.newHashMap(recipes.get(RecipeType.CRAFTING));
					map.put(ForgeRegistries.ITEMS.getKey(set.helmet),
							new RecipeBlockArmor(ForgeRegistries.ITEMS.getKey(set.helmet), set, BlockArmor.MODID+"_"+EquipmentSlot.HEAD.getName(), 3, 2, helmetRecipe, new ItemStack(set.helmet)));
					map.put(ForgeRegistries.ITEMS.getKey(set.chestplate),
							new RecipeBlockArmor(ForgeRegistries.ITEMS.getKey(set.chestplate), set, BlockArmor.MODID+"_"+EquipmentSlot.CHEST.getName(), 3, 3, armorRecipe, new ItemStack(set.chestplate)));
					map.put(ForgeRegistries.ITEMS.getKey(set.leggings),
							new RecipeBlockArmor(ForgeRegistries.ITEMS.getKey(set.leggings), set, BlockArmor.MODID+"_"+EquipmentSlot.LEGS.getName(), 3, 3, legsRecipe, new ItemStack(set.leggings)));
					map.put(ForgeRegistries.ITEMS.getKey(set.boots),
							new RecipeBlockArmor(ForgeRegistries.ITEMS.getKey(set.boots), set, BlockArmor.MODID+"_"+EquipmentSlot.FEET.getName(), 3, 2, bootsRecipe, new ItemStack(set.boots)));
					recipes.put(RecipeType.CRAFTING, ImmutableMap.copyOf(map));
					recipesField.set(server.getRecipeManager(), recipes);
				}
			}
		}
		catch (Exception e) {
			e.printStackTrace();
		}
	}

	@SubscribeEvent
	public static void playerJoin(PlayerLoggedInEvent event) {
		if (!event.getEntity().level.isClientSide && event.getEntity() instanceof ServerPlayer)
			BlockArmor.NETWORK.send(PacketDistributor.PLAYER.with(()->(ServerPlayer) event.getEntity()), new SDevColorsPacket());
	}
	
	/**Set world time*/
	public static void setWorldTime(Level world, long time) {
		if (world instanceof ServerLevel)
			((ServerLevel)world).setDayTime(time);
	}

}