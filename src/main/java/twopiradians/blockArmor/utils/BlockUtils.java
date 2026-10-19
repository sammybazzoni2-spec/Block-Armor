package twopiradians.blockArmor.utils;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.TransparentBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.StainedGlassPaneBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import twopiradians.blockArmor.common.seteffect.SetEffect;

public class BlockUtils {

	/**Get block properties*/
	public static BlockBehaviour.Properties getProperties(Block block) {
		return BlockBehaviour.Properties.ofFullCopy(block);
	}

	/**Get hardness of a block*/
	public static float getHardness(Block block) {
		float blockHardness = 0.5f;
		try {
			blockHardness = block.defaultBlockState().getDestroySpeed(null, BlockPos.ZERO);
		} catch(Exception e) {
			blockHardness = 0.5f;
		}
		return blockHardness;
	}

	/**Get blast resistance for block*/
	@SuppressWarnings("deprecation")
	public static float getBlastResistance(Block block) {
		return block.getExplosionResistance();
	}

	/**Get if block is solid*/
	@SuppressWarnings("deprecation")
	public static boolean getIsSolid(Block block) {
		try {
			return block.defaultBlockState().isSolid();
		}
		catch (Exception e) {
			return true;
		}
	}
	
	/**Get if block requires tool to be broken*/
	public static boolean getRequiresTool(Block block) {
		try {
			return block.defaultBlockState().requiresCorrectToolForDrops();
		}
		catch (Exception e) {
			return true;
		}
	}

	/**Is this block glass (replaces Material.BUILDABLE_GLASS / GLASS checks)*/
	public static boolean isGlass(Block block) {
		BlockState state = block.defaultBlockState();
		return block instanceof TransparentBlock || block instanceof StainedGlassPaneBlock || 
				state.getSoundType() == SoundType.GLASS;
	}

	/**Get light level for block*/
	@SuppressWarnings("deprecation")
	public static int getLightLevel(Block block) {
		int lightLevel = block.defaultBlockState().getLightEmission();
		if (lightLevel <= 0) {
			if (SetEffect.registryNameContains(block, new String[] {"lamp"}) || isGlass(block))
				lightLevel = 15;
		}
		return lightLevel;
	}

}
