package twopiradians.blockArmor.common;

import java.util.ArrayList;
import java.util.Optional;

import net.minecraft.core.NonNullList;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.level.Level;
import net.minecraftforge.event.OnDatapackSyncEvent;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.entity.player.PlayerEvent.PlayerLoggedInEvent;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber.Bus;
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
		public static void registerModifierSerializers(final RegisterEvent event) {
			event.register(ForgeRegistries.Keys.GLOBAL_LOOT_MODIFIER_SERIALIZERS, helper -> {
				helper.register(new ResourceLocation(BlockArmor.MODID, "set_effect_autosmelt"), EnchantmentSmeltingModifier.CODEC.get());
				helper.register(new ResourceLocation(BlockArmor.MODID, "set_effect_lucky"), SetEffectLuckyModifier.CODEC.get());
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

	/**Re-add recipes after /reload (fired before recipes are sent to players)*/
	@SubscribeEvent
	public static void datapackSync(OnDatapackSyncEvent event) {
		if (event.getPlayer() == null) 
			registerRecipes(event.getPlayerList().getServer());
	}
	
	/**Adds a crafting recipe for each enabled armor piece to the server's recipe manager*/
	private static void registerRecipes(MinecraftServer server) {
		try {
			ArrayList<Recipe<?>> recipes = new ArrayList<Recipe<?>>(server.getRecipeManager().getRecipes());
			for (ArmorSet set : ArmorSet.allSets) {
				if (set.isEnabled() && set.helmet != null) {
					ItemStack stack = set.getStack();
					NonNullList<Ingredient> helmetRecipe = NonNullList.of(Ingredient.EMPTY,
							Ingredient.of(stack), Ingredient.of(stack), Ingredient.of(stack),
							Ingredient.of(stack), Ingredient.EMPTY, Ingredient.of(stack));

					NonNullList<Ingredient> armorRecipe = NonNullList.of(Ingredient.EMPTY,
							Ingredient.of(stack), Ingredient.EMPTY, Ingredient.of(stack),
							Ingredient.of(stack), Ingredient.of(stack), Ingredient.of(stack),
							Ingredient.of(stack), Ingredient.of(stack), Ingredient.of(stack));

					NonNullList<Ingredient> legsRecipe = NonNullList.of(Ingredient.EMPTY,
							Ingredient.of(stack), Ingredient.of(stack), Ingredient.of(stack),
							Ingredient.of(stack), Ingredient.EMPTY, Ingredient.of(stack),
							Ingredient.of(stack), Ingredient.EMPTY, Ingredient.of(stack));

					NonNullList<Ingredient> bootsRecipe = NonNullList.of(Ingredient.EMPTY,
							Ingredient.of(stack), Ingredient.EMPTY, Ingredient.of(stack),
							Ingredient.of(stack), Ingredient.EMPTY, Ingredient.of(stack));

					recipes.add(createRecipe(set, set.helmet, EquipmentSlot.HEAD, 3, 2, helmetRecipe));
					recipes.add(createRecipe(set, set.chestplate, EquipmentSlot.CHEST, 3, 3, armorRecipe));
					recipes.add(createRecipe(set, set.leggings, EquipmentSlot.LEGS, 3, 3, legsRecipe));
					recipes.add(createRecipe(set, set.boots, EquipmentSlot.FEET, 3, 2, bootsRecipe));
				}
			}
			// replaceRecipes rebuilds both the by-type and by-id maps
			server.getRecipeManager().replaceRecipes(recipes);
		}
		catch (Exception e) {
			e.printStackTrace();
		}
	}

	private static RecipeBlockArmor createRecipe(ArmorSet set, net.minecraft.world.item.Item item, EquipmentSlot slot, int width, int height, NonNullList<Ingredient> ingredients) {
		ResourceLocation loc = ForgeRegistries.ITEMS.getKey(item);
		return new RecipeBlockArmor(loc, set, BlockArmor.MODID+"_"+slot.getName(), CraftingBookCategory.EQUIPMENT, width, height, ingredients, new ItemStack(item));
	}

	@SubscribeEvent
	public static void playerJoin(PlayerLoggedInEvent event) {
		if (!event.getEntity().level().isClientSide && event.getEntity() instanceof ServerPlayer)
			BlockArmor.NETWORK.send(PacketDistributor.PLAYER.with(()->(ServerPlayer) event.getEntity()), new SDevColorsPacket());
	}
	
	/**Set world time*/
	public static void setWorldTime(Level world, long time) {
		if (world instanceof ServerLevel)
			((ServerLevel)world).setDayTime(time);
	}

}
