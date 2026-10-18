package twopiradians.blockArmor.common.seteffect;

import net.minecraft.ChatFormatting;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.block.BambooStalkBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.BushBlock;
import net.minecraft.world.level.block.CactusBlock;
import net.minecraft.world.level.block.GrowingPlantBlock;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.VineBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.BonemealableBlock;
import net.minecraft.world.level.block.NetherrackBlock;
import net.minecraftforge.common.IPlantable;

public class SetEffectRegrowth extends SetEffect {

	protected SetEffectRegrowth() {
		super();
		this.color = ChatFormatting.DARK_GREEN;
	}
	
	/**Only called when player wearing full, enabled set*/
	public void onArmorTick(Level world, Player player, ItemStack stack) {
		super.onArmorTick(world, player, stack);
		
		if (!world.isClientSide && world.random.nextInt(200) == 0 && stack.isDamaged())
			stack.setDamageValue(stack.getDamageValue()-1);
	}

	/**Should block be given this set effect*/
	@Override
	protected boolean isValid(Block block) {	
		// ignore netherrack (would be accepted otherwise bc it's IGrowable)
		if (block instanceof NetherrackBlock)
			return false;
		
		if (block instanceof BonemealableBlock || block instanceof IPlantable || 
				SetEffect.registryNameContains(block, new String[] {"moss", "plant", "mycelium", "mushroom", "flower",
						"log", "wood", "stem", "plank", "grass", "nether_wart"}))
			return true;	
		
		// replacement for the removed Material checks (bamboo, cactus, crops, leaves, plants, nether wood)
		BlockState state = block.defaultBlockState();
		if (block instanceof BushBlock || block instanceof BambooStalkBlock || block instanceof CactusBlock || 
				block instanceof VineBlock || block instanceof GrowingPlantBlock || block instanceof LeavesBlock || 
				state.is(BlockTags.LEAVES) || state.is(BlockTags.CROPS) || state.is(BlockTags.SAPLINGS) || 
				state.is(BlockTags.FLOWERS) || state.is(BlockTags.LOGS) || state.is(BlockTags.WART_BLOCKS))
			return true;

		return false;
	}
}