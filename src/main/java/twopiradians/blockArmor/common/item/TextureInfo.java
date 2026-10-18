package twopiradians.blockArmor.common.item;

import javax.annotation.Nullable;

import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import twopiradians.blockArmor.utils.SpriteUtils;

@OnlyIn(Dist.CLIENT)
public class TextureInfo {

	/**Quad's texture (or missing texture if invalid/missing)*/
	public TextureAtlasSprite sprite;
	/**Quad's color (or -1 if none)*/
	public int color;
	/**Animation state of the sprite (SpriteContents$Ticker) - null if not animated*/
	@Nullable
	public Object animationState;
	/**Original height of sprite (before it recalculates sprite with extra frames, like Prismarine)*/
	public int originalHeight;

	protected TextureInfo(@Nullable TextureAtlasSprite sprite, int color, @Nullable Object animationState) {
		this.sprite = sprite == null ? ArmorSet.missingSprite : sprite;
		this.color = color;
		this.animationState = animationState;
		this.originalHeight = SpriteUtils.getOriginalHeight(this.sprite);
	}

	/**Returns current alpha for animation overlay*/
	public float getAlpha() {
		return this.animationState == null ? 0 : SpriteUtils.getFrameProgress(this.animationState);
	}

	/**Returns current animation frame*/
	public int getCurrentAnimationFrame() {
		return SpriteUtils.getCurrentFrame(this.animationState);
	}

	/**Returns next animation frame*/
	public int getNextAnimationFrame() { 
		return SpriteUtils.getNextFrame(this.animationState);
	}

}
