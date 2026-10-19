package twopiradians.blockArmor.common.item;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.crafting.Ingredient;
import twopiradians.blockArmor.common.BlockArmor;

/**Holds the (config-changeable) stats of a set's armor.
 * In 1.21 {@link ArmorMaterial} is an immutable record, so this only builds one for the armor items to be created with -
 * the actual stats are read from here by {@link BlockArmorItem} (attributes / durability) so they can change with the config.*/
public class BlockArmorMaterial {

	/**Single armor layer - the real texture is chosen per block in {@link BlockArmorItem#getArmorTexture}*/
	private static final List<ArmorMaterial.Layer> LAYERS = List.of(
			new ArmorMaterial.Layer(ResourceLocation.fromNamespaceAndPath(BlockArmor.MODID, "block_armor")));
	/**Indexed by {@link EquipmentSlot#getIndex()} (boots, leggings, chestplate, helmet)*/
	private static final int[] MAX_DAMAGE_ARRAY = new int[]{13, 15, 16, 11};
	private final String name;
	private final int maxDamageFactor;
	private final int[] damageReductionAmountArray;
	private final int enchantability;
	private final Holder<SoundEvent> soundEvent;
	private final float toughness;
	private final float knockbackResistance;
	private final Supplier<Ingredient> repairMaterial;
	private Ingredient repairIngredient;

	public BlockArmorMaterial(String name, int maxDamageFactor, int[] damageReductionAmountArray, 
			int enchantability, Holder<SoundEvent> soundEvent, float toughness, float knockbackResistance,
			Supplier<Ingredient> repairMaterial) {
		this.name = name;
		this.maxDamageFactor = maxDamageFactor;
		this.damageReductionAmountArray = damageReductionAmountArray;
		this.enchantability = enchantability;
		this.soundEvent = soundEvent;
		this.toughness = toughness;
		this.knockbackResistance = knockbackResistance;
		this.repairMaterial = repairMaterial;
	}

	public String getName() {
		return this.name;
	}

	public int getDurabilityForSlot(EquipmentSlot slot) {
		return MAX_DAMAGE_ARRAY[slot.getIndex()] * this.maxDamageFactor;
	}

	public int getDefenseForSlot(EquipmentSlot slot) {
		return this.damageReductionAmountArray[slot.getIndex()];
	}

	public int getEnchantmentValue() {
		return this.enchantability;
	}

	public Holder<SoundEvent> getEquipSound() {
		return this.soundEvent;
	}

	public Ingredient getRepairIngredient() {
		if (this.repairIngredient == null)
			this.repairIngredient = this.repairMaterial.get();
		return this.repairIngredient;
	}

	public float getToughness() {
		return this.toughness;
	}

	public float getKnockbackResistance() {
		return this.knockbackResistance;
	}

	/**Vanilla material for creating an armor item (only used for its initial values)*/
	public ArmorMaterial toVanilla() {
		Map<ArmorItem.Type, Integer> defense = new EnumMap<>(ArmorItem.Type.class);
		defense.put(ArmorItem.Type.BOOTS, this.damageReductionAmountArray[0]);
		defense.put(ArmorItem.Type.LEGGINGS, this.damageReductionAmountArray[1]);
		defense.put(ArmorItem.Type.CHESTPLATE, this.damageReductionAmountArray[2]);
		defense.put(ArmorItem.Type.HELMET, this.damageReductionAmountArray[3]);
		defense.put(ArmorItem.Type.BODY, this.damageReductionAmountArray[2]);
		return new ArmorMaterial(defense, this.enchantability, this.soundEvent, this::getRepairIngredient, LAYERS, 
				this.toughness, this.knockbackResistance);
	}

}
