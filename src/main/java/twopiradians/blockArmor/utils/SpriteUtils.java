package twopiradians.blockArmor.utils;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;

import javax.annotation.Nullable;

import com.mojang.blaze3d.platform.NativeImage;

import net.minecraft.client.renderer.texture.SpriteContents;
import net.minecraft.client.renderer.texture.SpriteTicker;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import twopiradians.blockArmor.common.BlockArmor;

/**Reflection helpers for reading sprite animation state (1.19.3+ moved it from the sprite into SpriteContents$Ticker).
 * Fields are found by type / declaration order instead of by (obfuscated) name so this doesn't depend on mappings.*/
@OnlyIn(Dist.CLIENT)
public class SpriteUtils {

	private static Field originalImageField;
	private static Field tickerSpriteField;
	private static Field tickerStateField;
	private static Field frameField;
	private static Field subFrameField;
	private static Field animationInfoField;
	private static Field framesField;
	private static Field frameIndexField;
	private static Field frameTimeField;
	private static boolean failed;

	/**Finds the real (ticking) animation state of every animated sprite in the atlas*/
	public static IdentityHashMap<TextureAtlasSprite, Object> findTickers(TextureAtlas atlas) {
		IdentityHashMap<TextureAtlasSprite, Object> map = new IdentityHashMap<TextureAtlasSprite, Object>();
		try {
			for (Field listField : TextureAtlas.class.getDeclaredFields()) {
				if (Modifier.isStatic(listField.getModifiers()) || !List.class.isAssignableFrom(listField.getType()))
					continue;
				listField.setAccessible(true);
				List<?> list = (List<?>) listField.get(atlas);
				if (list == null)
					continue;
				for (Object wrapper : list) {
					if (!(wrapper instanceof TextureAtlasSprite.Ticker))
						continue;
					// wrapper is an anonymous class capturing the sprite and the SpriteContents.Ticker
					TextureAtlasSprite sprite = null;
					Object state = null;
					for (Field f : wrapper.getClass().getDeclaredFields()) {
						f.setAccessible(true);
						Object value = f.get(wrapper);
						if (value instanceof TextureAtlasSprite)
							sprite = (TextureAtlasSprite) value;
						else if (value instanceof SpriteTicker)
							state = value;
					}
					if (sprite != null && state != null)
						map.put(sprite, state);
				}
			}
		} catch (Exception e) {
			BlockArmor.LOGGER.error("Failed to find animated sprite tickers - animated block textures will not animate on armor", e);
		}
		return map;
	}

	/**Height of the whole (un-split) image of the sprite, including all animation frames*/
	public static int getOriginalHeight(TextureAtlasSprite sprite) {
		try {
			if (originalImageField == null) {
				for (Field f : SpriteContents.class.getDeclaredFields())
					if (f.getType() == NativeImage.class && !Modifier.isStatic(f.getModifiers())) {
						f.setAccessible(true);
						originalImageField = f;
						break;
					}
			}
			NativeImage image = (NativeImage) originalImageField.get(sprite.contents());
			if (image != null)
				return image.getHeight();
		} catch (Exception e) {
			e.printStackTrace();
		}
		return sprite.contents().height();
	}

	private static List<Field> intFields(Class<?> clazz) {
		ArrayList<Field> fields = new ArrayList<Field>();
		for (Field f : clazz.getDeclaredFields())
			if (f.getType() == int.class && !Modifier.isStatic(f.getModifiers())) {
				f.setAccessible(true);
				fields.add(f);
			}
		return fields;
	}

	private static void init(Object ticker) throws Exception {
		if (frameField != null || failed)
			return;
		try {
			List<Field> ints = intFields(ticker.getClass());
			frameField = ints.get(0); // frame
			subFrameField = ints.get(1); // subFrame
			for (Field f : ticker.getClass().getDeclaredFields())
				if (f.getType().getName().endsWith("AnimatedTexture")) {
					f.setAccessible(true);
					animationInfoField = f;
				}
			Object info = animationInfoField.get(ticker);
			for (Field f : info.getClass().getDeclaredFields())
				if (List.class.isAssignableFrom(f.getType()) && !Modifier.isStatic(f.getModifiers())) {
					f.setAccessible(true);
					framesField = f;
					break;
				}
			Object firstFrame = ((List<?>) framesField.get(info)).get(0);
			List<Field> frameInts = intFields(firstFrame.getClass());
			frameIndexField = frameInts.get(0); // index
			frameTimeField = frameInts.get(1); // time
		} catch (Exception e) {
			failed = true;
			frameField = null;
			BlockArmor.LOGGER.error("Failed to read animation state - animated block textures will not animate on armor", e);
		}
	}

	/**Current frame index (in the sprite's image) of animation state, or 0*/
	public static int getCurrentFrame(@Nullable Object ticker) {
		return getFrame(ticker, 0);
	}

	/**Frame index (in the sprite's image) that follows the current one, or 0*/
	public static int getNextFrame(@Nullable Object ticker) {
		return getFrame(ticker, 1);
	}

	private static int getFrame(@Nullable Object ticker, int offset) {
		if (ticker != null)
			try {
				init(ticker);
				if (!failed) {
					List<?> frames = (List<?>) framesField.get(animationInfoField.get(ticker));
					int current = (frameField.getInt(ticker) + offset) % frames.size();
					return frameIndexField.getInt(frames.get(current));
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		return 0;
	}

	/**Progress (0-1) through the current frame*/
	public static float getFrameProgress(@Nullable Object ticker) {
		if (ticker != null)
			try {
				init(ticker);
				if (!failed) {
					List<?> frames = (List<?>) framesField.get(animationInfoField.get(ticker));
					int time = frameTimeField.getInt(frames.get(frameField.getInt(ticker)));
					return time <= 0 ? 0 : (float) subFrameField.getInt(ticker) / (float) time;
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		return 0;
	}

}
