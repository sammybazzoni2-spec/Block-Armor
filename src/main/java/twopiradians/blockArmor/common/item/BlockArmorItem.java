package twopiradians.blockArmor.common.item;

import java.util.List;
import java.util.function.Consumer;

import javax.annotation.Nullable;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.level.Level;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;
import twopiradians.blockArmor.client.ClientProxy;
import twopiradians.blockArmor.client.model.ModelBAArmor;
import twopiradians.blockArmor.common.command.CommandDev;
import twopiradians.blockArmor.common.config.Config;
import twopiradians.blockArmor.common.seteffect.SetEffect;
import twopiradians.blockArmor.utils.ItemData;
import net.minecraftforge.event.entity.player.PlayerDestroyItemEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber
public class BlockArmorItem extends ArmorItem {
	/** The ArmorSet that this item belongs to */
	public ArmorSet set;
	/** Equipment slot of this item (ArmorItem only stores a Type now) */
	private final EquipmentSlot slot;
	/** Max damage from the (changeable) material - the stack's max damage component is kept in sync with this */
	private int customMaxDamage;
	/** Armor stats (changeable by config) */
	private BlockArmorMaterial blockMaterial;

	public BlockArmorItem(BlockArmorMaterial material, EquipmentSlot slot, ArmorSet set) {
		super(Holder.direct(material.toVanilla()), typeForSlot(slot), new Item.Properties()
				.durability(material.getDurabilityForSlot(slot))
				.rarity(set.setEffects.isEmpty() ? Rarity.COMMON : Rarity.UNCOMMON));
		this.slot = slot;
		this.set = set;
		this.setMaterial(material);
	}

	private static ArmorItem.Type typeForSlot(EquipmentSlot slot) {
		switch (slot) {
		case HEAD:
			return ArmorItem.Type.HELMET;
		case CHEST:
			return ArmorItem.Type.CHESTPLATE;
		case LEGS:
			return ArmorItem.Type.LEGGINGS;
		default:
			return ArmorItem.Type.BOOTS;
		}
	}

	public EquipmentSlot getSlot() {
		return this.slot;
	}

	/** Change armor texture based on block */
	@Override
	@OnlyIn(Dist.CLIENT)
	public ResourceLocation getArmorTexture(ItemStack stack, Entity entity, EquipmentSlot slot, ArmorMaterial.Layer layer, boolean inner) {
		TextureAtlasSprite sprite = this.set.getTextureInfo(slot).sprite;
		ResourceLocation name = sprite.contents().name();
		return name.withPath("textures/" + name.getPath() + ".png");
	}

	@Override 
	@OnlyIn(Dist.CLIENT)
	public void initializeClient(Consumer<IClientItemExtensions> consumer) {
		consumer.accept(new IClientItemExtensions() {
			@Override
			@OnlyIn(Dist.CLIENT)
			public HumanoidModel<?> getHumanoidArmorModel(LivingEntity entity, ItemStack stack, EquipmentSlot armorSlot, HumanoidModel<?> _default) {
				BlockArmorItem item = (BlockArmorItem) stack.getItem();
				TextureInfo info = item.set.getTextureInfo(item.slot);
				TextureAtlasSprite sprite = info.sprite;
				int width = sprite.contents().width();
				int height = info.originalHeight;
				int currentFrame = info.getCurrentAnimationFrame();
				int nextFrame = info.getNextAnimationFrame();
				ModelBAArmor model = (ModelBAArmor) ClientProxy.getBlockArmorModel(entity, height, width, currentFrame, nextFrame, armorSlot);
				model.color = info.color;
				model.alpha = info.getAlpha();
				return model;
			}
		});
	}

	/** Spawn hoarder items when a worn piece breaks */
	@SubscribeEvent
	public static void onItemDestroyed(PlayerDestroyItemEvent event) {
		ItemStack stack = event.getOriginal();
		if (stack.getItem() instanceof BlockArmorItem && ((BlockArmorItem) stack.getItem()).set.setEffects.contains(SetEffect.HOARDER))
			SetEffect.HOARDER.onBreak(stack);
	}

	/** Change display name based on the block */
	@Override
	public Component getName(ItemStack stack) {
		return ArmorSet.getItemStackDisplayName(stack.getItem(), this.slot);
	}

	/** Handles the attributes (armor stats from the changeable material + set effects when wearing an armor set) */
	@Override
	public ItemAttributeModifiers getDefaultAttributeModifiers(ItemStack stack) {
		ItemAttributeModifiers.Builder builder = ItemAttributeModifiers.builder();
		EquipmentSlotGroup group = EquipmentSlotGroup.bySlot(this.slot);
		ResourceLocation id = ResourceLocation.withDefaultNamespace("armor." + this.getType().getName());
		builder.add(Attributes.ARMOR, new AttributeModifier(id, (double) this.getDefense(), AttributeModifier.Operation.ADD_VALUE), group);
		builder.add(Attributes.ARMOR_TOUGHNESS, new AttributeModifier(id, (double) this.getToughness(), AttributeModifier.Operation.ADD_VALUE), group);
		if (this.blockMaterial.getKnockbackResistance() > 0)
			builder.add(Attributes.KNOCKBACK_RESISTANCE, new AttributeModifier(id, 
					this.blockMaterial.getKnockbackResistance() / 10d * Config.globalKnockbackResistanceModifier, 
					AttributeModifier.Operation.ADD_VALUE), group);

		for (SetEffect effect : set.setEffects)
			effect.addAttributeModifiers(builder, group, stack);

		return builder.build();
	}

	/** Deals with armor tooltips */
	@Override
	@OnlyIn(Dist.CLIENT)
	public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltip,
			TooltipFlag flagIn) {
		if (ItemData.has(stack, "devSpawned"))
			tooltip.add(Component.literal(ChatFormatting.DARK_PURPLE + "" + ChatFormatting.BOLD + "Dev Spawned"));

		if (!set.setEffects.isEmpty() && set.setEffects.get(0).isEnabled()) {
			// add header if shifting
			if (Screen.hasShiftDown())
				tooltip.add(Component.translatable("item.blockarmor.tooltip.setEffects", 
						Component.translatable("item.blockarmor.tooltip.setEffectsRequire"+(Config.piecesForSet == 4 ? "" : "+"), Config.piecesForSet)
						.withStyle(ChatFormatting.ITALIC), Config.piecesForSet)
						.withStyle(ChatFormatting.GOLD));

			// set effect names and descriptions if shifting
			for (SetEffect effect : set.setEffects)
				if (effect.isEnabled())
					tooltip = effect.addInformation(stack, Screen.hasShiftDown(), Minecraft.getInstance().player,
							tooltip, flagIn);
		}
	}

	/** Handles nbt, enchanting armor, and (when worn) most of the armor set special effects and bonuses */
	@Override
	public void inventoryTick(ItemStack stack, Level world, Entity entity, int itemSlot, boolean isSelected) {
		// delete dev spawned items if not in dev's inventory and delete disabled items
		// (except missingTexture items in SMP)
		if (stack.isEmpty() || (!set.isEnabled() && !world.isClientSide & entity instanceof Player)
				|| (!world.isClientSide && entity instanceof Player && ItemData.has(stack, "devSpawned") 
						&& !CommandDev.DEVS.contains(entity.getUUID()))) {
			if (entity instanceof Player) {
				Player player = (Player) entity;
				if (player.getItemBySlot(this.slot) == stack)
					player.setItemSlot(this.slot, ItemStack.EMPTY);
				else
					for (int i = 0; i < player.getInventory().getContainerSize(); ++i)
						if (player.getInventory().getItem(i) == stack)
							player.getInventory().setItem(i, ItemStack.EMPTY);
			}
			return;
		}

		// keep max damage in sync with config
		if (stack.getMaxDamage() != this.customMaxDamage)
			stack.set(DataComponents.MAX_DAMAGE, this.customMaxDamage);

		for (SetEffect effect : set.setEffects)
			effect.onUpdate(stack, world, entity, slot.getFilterFlag(), isSelected);

		// armor tick if worn
		if (entity instanceof Player && ((Player) entity).getItemBySlot(this.slot) == stack) {
			Player player = (Player) entity;
			for (SetEffect effect : set.setEffects)
				if (ArmorSet.getWornSetEffects(player).contains(effect))
					effect.onArmorTick(world, player, stack);
		}
	}

	/** Delete dev spawned dropped items */
	@Override
	public boolean onEntityItemUpdate(ItemStack stack, ItemEntity entityItem) {
		// delete dev spawned items if not worn by dev and delete disabled items (except
		// missingTexture items in SMP)
		if ((!set.isEnabled() && !entityItem.level().isClientSide)
				|| (!entityItem.level().isClientSide && entityItem != null && entityItem.getItem() != null
				&& ItemData.has(entityItem.getItem(), "devSpawned"))) {
			entityItem.discard();
			return true;
		}
		return false;
	}

	// ======================= CHANGE MATERIAL FROM CONFIG =======================

	public void setMaterial(BlockArmorMaterial material) {
		this.blockMaterial = material;
		// change max damage (stack component is updated from this in inventoryTick)
		this.customMaxDamage = (int) (material.getDurabilityForSlot(this.slot) * Config.globalDurabilityModifier);
	}

	public BlockArmorMaterial getBlockMaterial() {
		return this.blockMaterial;
	}

	public int getCustomMaxDamage() {
		return this.customMaxDamage;
	}

	public int getDefense() {
		return (int) (this.blockMaterial.getDefenseForSlot(this.slot) * Config.globalDamageReductionModifier);
	}

	public float getToughness() {
		return (float) (this.blockMaterial.getToughness() * Config.globalToughnessModifier);
	}

	@Override
	public int getEnchantmentValue() {
		return (int) (this.blockMaterial.getEnchantmentValue() * Config.globalEnchantabilityModifier);
	}

}
