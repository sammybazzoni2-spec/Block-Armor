package twopiradians.blockArmor.common.seteffect;

import java.lang.reflect.Field;

import net.minecraft.ChatFormatting;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.fml.util.ObfuscationReflectionHelper;

public class SetEffectRocky extends SetEffect {

	private static final Field WAS_TOUCHING_WATER_FIELD = ObfuscationReflectionHelper.findField(Entity.class, "f_19798_");
	private static final Field FIRST_TICK_FIELD = ObfuscationReflectionHelper.findField(Entity.class, "f_19803_");

	protected SetEffectRocky() {
		super();
		this.color = ChatFormatting.GRAY;
	}

	/**Only called when player wearing full, enabled set*/
	public void onArmorTick(Level world, Player player, ItemStack stack) {
		super.onArmorTick(world, player, stack);

		Vec3 motion = player.getDeltaMovement();
		if (player.isInWater()) {
			player.setSwimming(false); // prevent swimming
			try {	
				// set to not in water (so player sinks and can move normally)
				WAS_TOUCHING_WATER_FIELD.set(player, false); 
				// set firstUpdate to true so it doesn't spawn a ton of bubble particles in and make sounds
				FIRST_TICK_FIELD.set(player, true);
			}
			catch (Exception e) {
				e.printStackTrace();
			}
			// slow down under water a bit (if no depth strider)
			if ((Math.abs(motion.x) > 0 || Math.abs(motion.z) > 0 || motion.y > 0) && 
					EnchantmentHelper.getEnchantmentLevel(player.level().registryAccess().lookupOrThrow(net.minecraft.core.registries.Registries.ENCHANTMENT).getOrThrow(net.minecraft.world.item.enchantment.Enchantments.DEPTH_STRIDER), player) == 0) {
				player.setSprinting(false); // prevent sprinting
				player.setDeltaMovement(
						motion.x()*(player.onGround() ? 0.14d : 1d), 
						motion.y(), 
						motion.z()*(player.onGround() ? 0.14d : 1d));
				if (player.onGround())
					player.hurtMarked = true;
				player.hasImpulse = true;
			}
		}
	}

	/**Should block be given this set effect*/
	@Override
	protected boolean isValid(Block block) {		
		if (SetEffect.registryNameContains(block, new String[] {"rock", "stone", "deepslate", "tuff"}))
			return true;
		return false;
	}
}