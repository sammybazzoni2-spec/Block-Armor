package twopiradians.blockArmor.client.model;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.Nullable;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Quaternion;
import com.mojang.math.Transformation;
import com.mojang.math.Vector3f;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.BlockElement;
import net.minecraft.client.renderer.block.model.ItemOverrides;
import net.minecraft.client.renderer.block.model.ItemTransforms;
import net.minecraft.client.renderer.block.model.ItemTransforms.TransformType;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelState;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.model.IQuadTransformer;
import net.minecraftforge.client.model.SimpleModelState;
import net.minecraftforge.client.model.geometry.UnbakedGeometryHelper;
import twopiradians.blockArmor.common.BlockArmor;
import twopiradians.blockArmor.common.item.ArmorSet;
import twopiradians.blockArmor.common.item.BlockArmorItem;

/**Dynamic inventory model for Block Armor items.
 * Each armor item gets its own {@link BakedDynBlockArmor} (see ClientProxy#onModelBake),
 * and its quads are filled in lazily from the block's texture (see {@link BakedDynBlockArmorOverrideHandler#resolve})*/
@OnlyIn(Dist.CLIENT)
public final class ModelBAItem {

	// minimal Z offsets (in pixels) to prevent depth-fighting - the item's base layer sits at 7.5 (north) and 8.5 (south)
	private static final float TEMPLATE_NORTH_Z_OFFSET = -0.002f / 16f;
	private static final float TEMPLATE_SOUTH_Z_OFFSET = 0.002f / 16f;
	private static final float COVER_NORTH_Z_OFFSET = -0.004f / 16f;
	private static final float COVER_SOUTH_Z_OFFSET = 0.003f / 16f;

	/**Transformations used for each (non-GUI) perspective*/
	private static final ImmutableMap<TransformType, Transformation> TRANSFORMS;
	static {
		ImmutableMap.Builder<TransformType, Transformation> builder = ImmutableMap.builder();
		builder.put(TransformType.GROUND, new Transformation(new Vector3f(0.0f, 0.125f, 0.0f), new Quaternion(0.0f, 0.0f, 0.0f, 1.0f), new Vector3f(0.5f, 0.5f, 0.5f), new Quaternion(0.0f, 0.0f, 0.0f, 1.0f)));
		builder.put(TransformType.HEAD, new Transformation(new Vector3f(0.0f, 0.8125f, 0.4375f), new Quaternion(0.0f, 1.0f, 0.0f, -4.371139E-8f), new Vector3f(1.0f, 1.0f, 1.0f), new Quaternion(0.0f, 0.0f, 0.0f, 1.0f)));
		builder.put(TransformType.FIRST_PERSON_RIGHT_HAND, new Transformation(new Vector3f(0.070625f, 0.2f, 0.070625f), new Quaternion(-0.15304594f, -0.6903456f, 0.15304594f, 0.6903456f), new Vector3f(0.68000007f, 0.68000007f, 0.68f), new Quaternion(0.0f, 0.0f, 0.0f, 1.0f)));
		builder.put(TransformType.FIRST_PERSON_LEFT_HAND, new Transformation(new Vector3f(0.070625f, 0.2f, 0.070625f), new Quaternion(-0.15304594f, -0.6903456f, 0.15304594f, 0.6903456f), new Vector3f(0.68000007f, 0.68000007f, 0.68f), new Quaternion(0.0f, 0.0f, 0.0f, 1.0f)));
		builder.put(TransformType.THIRD_PERSON_RIGHT_HAND, new Transformation(new Vector3f(0.0f, 0.1875f, 0.0625f), new Quaternion(0.0f, 0.0f, 0.0f, 0.99999994f), new Vector3f(0.55f, 0.55f, 0.55f), new Quaternion(0.0f, 0.0f, 0.0f, 1.0f)));
		builder.put(TransformType.THIRD_PERSON_LEFT_HAND, new Transformation(new Vector3f(0.0f, 0.1875f, 0.0625f), new Quaternion(0.0f, 0.0f, 0.0f, 0.99999994f), new Vector3f(0.55f, 0.55f, 0.55f), new Quaternion(0.0f, 0.0f, 0.0f, 1.0f)));
		TRANSFORMS = builder.build();
	}

	/**Creates a new (empty) model for a Block Armor item - quads are added when the item is first rendered*/
	public static BakedModel createModel() {
		return new BakedDynBlockArmor();
	}

	/**Copies quad, offsetting its z position and (optionally) setting its vertex color
	 * @param abgrColor vertex color in ABGR format (as stored in vertex data), or 0 to leave color as it is*/
	private static BakedQuad adjustQuad(BakedQuad quad, float zOffset, int abgrColor) {
		int[] data = quad.getVertices().clone();
		for (int vertex = 0; vertex < 4; vertex++) {
			int offset = vertex * IQuadTransformer.STRIDE;
			int zIndex = offset + IQuadTransformer.POSITION + 2;
			data[zIndex] = Float.floatToRawIntBits(Float.intBitsToFloat(data[zIndex]) + zOffset);
			if (abgrColor != 0)
				data[offset + IQuadTransformer.COLOR] = abgrColor;
		}
		return new BakedQuad(data, quad.getTintIndex(), quad.getDirection(), quad.getSprite(), quad.isShade());
	}

	/**Bakes quads for the front and back faces only (and applies offset + color), using sprite as the texture*/
	private static List<BakedQuad> bakeFrontAndBack(List<BlockElement> elements, TextureAtlasSprite sprite, ModelState state,
			ResourceLocation location, float northOffset, float southOffset, int abgrColor) {
		List<BakedQuad> quads = Lists.newArrayList();
		for (BakedQuad quad : UnbakedGeometryHelper.bakeElements(elements, material -> sprite, state, location))
			if (quad.getDirection() == Direction.NORTH)
				quads.add(adjustQuad(quad, northOffset, abgrColor));
			else if (quad.getDirection() == Direction.SOUTH)
				quads.add(adjustQuad(quad, southOffset, abgrColor));
		return quads;
	}

	public static final class BakedDynBlockArmorOverrideHandler extends ItemOverrides {
		/**Map of item to quads for the item's inventory model*/
		private static HashMap<Item, HashMap<Layer, ImmutableList<BakedQuad>>> itemQuadsMap = Maps.newHashMap();

		public static final BakedDynBlockArmorOverrideHandler INSTANCE = new BakedDynBlockArmorOverrideHandler();

		/**Creates inventory icons (via quads) for each Block Armor piece and adds to itemQuadsMap
		 * Requires block atlas to be stitched and sets' textures to be mapped*/
		public static int createInventoryIcons() {
			ModelBAItem.BakedDynBlockArmorOverrideHandler.itemQuadsMap = Maps.newHashMap();
			int numIcons = 0;

			TextureAtlas atlas = Minecraft.getInstance().getModelManager().getAtlas(TextureAtlas.LOCATION_BLOCKS);
			ModelState state = new SimpleModelState(Transformation.identity());
			// these only depend on the armor type, so only create them once per type
			Map<String, ImmutableList<BakedQuad>> baseQuadsMap = Maps.newHashMap();
			Map<String, List<BlockElement>> elementsMap = Maps.newHashMap();

			for (ArmorSet set : ArmorSet.allSets) {
				BlockArmorItem[] armor = new BlockArmorItem[] {set.helmet, set.chestplate, set.leggings, set.boots};
				for (BlockArmorItem item : armor)
					if (item != null) {
						//Initialize variables
						TextureAtlasSprite sprite = item.set.getTextureInfo(item.getSlot()).sprite;
						if (sprite == null)
							BlockArmor.LOGGER.warn("Missing sprite for: "+new ItemStack(item).getHoverName().getString());
						String armorType = "";
						EquipmentSlot slot = item.getSlot();
						if (slot == EquipmentSlot.HEAD)
							armorType = "helmet";
						else if (slot == EquipmentSlot.CHEST)
							armorType = "chestplate";
						else if (slot == EquipmentSlot.LEGS)
							armorType = "leggings";
						else if (slot == EquipmentSlot.FEET)
							armorType = "boots";

						HashMap<Layer, ImmutableList<BakedQuad>> quads = Maps.newHashMap();

						//Base texture and model
						ImmutableList<BakedQuad> baseQuads = baseQuadsMap.get(armorType);
						if (baseQuads == null) {
							ResourceLocation baseLocation = new ResourceLocation(BlockArmor.MODID, "items/icons/block_armor_"+armorType+"_base");
							TextureAtlasSprite baseSprite = atlas.getSprite(baseLocation);
							baseQuads = ImmutableList.copyOf(UnbakedGeometryHelper.bakeElements(
									UnbakedGeometryHelper.createUnbakedItemElements(0, baseSprite), material -> baseSprite, state, baseLocation));
							baseQuadsMap.put(armorType, baseQuads);
						}
						quads.put(Layer.BASE, baseQuads);

						// vertex color in ABGR format (opaque white if no color)
						int color = item.set.getTextureInfo(item.getSlot()).color;
						int abgrColor = -1;
						if (color != -1) {
							int r = (color >> 16) & 0xFF;
							int g = (color >> 8) & 0xFF;
							int b = (color >> 0) & 0xFF;
							abgrColor = 0xFF000000 | (b << 16) | (g << 8) | r; // alpha always 255 (since sometimes 0)
						}

						//Template textures for left and right halves (the block's texture is shown where the templates are opaque)
						ImmutableList.Builder<BakedQuad> builder = ImmutableList.builder();
						for (int half = 1; half <= 2; half++) {
							String key = armorType+half+"_template";
							ResourceLocation templateLocation = new ResourceLocation(BlockArmor.MODID, "items/icons/block_armor_"+key);
							List<BlockElement> elements = elementsMap.get(key);
							if (elements == null) {
								elements = UnbakedGeometryHelper.createUnbakedItemMaskElements(1, atlas.getSprite(templateLocation));
								elementsMap.put(key, elements);
							}
							builder.addAll(bakeFrontAndBack(elements, sprite, state, templateLocation,
									TEMPLATE_NORTH_Z_OFFSET, TEMPLATE_SOUTH_Z_OFFSET, abgrColor));
						}
						quads.put(Layer.TEMPLATE, builder.build());

						//Cover texture
						String coverKey = armorType+"_cover";
						ResourceLocation coverLocation = new ResourceLocation(BlockArmor.MODID, "items/icons/block_armor_"+coverKey);
						TextureAtlasSprite coverSprite = atlas.getSprite(coverLocation);
						List<BlockElement> coverElements = elementsMap.get(coverKey);
						if (coverElements == null) {
							coverElements = UnbakedGeometryHelper.createUnbakedItemElements(2, coverSprite);
							elementsMap.put(coverKey, coverElements);
						}
						quads.put(Layer.COVER, ImmutableList.copyOf(bakeFrontAndBack(coverElements, coverSprite, state, coverLocation,
								COVER_NORTH_Z_OFFSET, COVER_SOUTH_Z_OFFSET, abgrColor)));

						itemQuadsMap.put(item, quads);
						numIcons++;
					}
			}
			return numIcons;
		}

		/**Called every tick - sets inventory icon (via quads) from map if null*/
		@Override
		public BakedModel resolve(BakedModel originalModel, ItemStack stack, @Nullable ClientLevel level, @Nullable LivingEntity entity, int num) {
			if (originalModel instanceof BakedDynBlockArmor &&
					(((BakedDynBlockArmor)originalModel).layers.get(Layer.BASE).quads == null ||
					((BakedDynBlockArmor)originalModel).layers.get(Layer.BASE).quads.isEmpty()) &&
					itemQuadsMap.get(stack.getItem()) != null) {
				for (Layer layer : Layer.values())
					((BakedDynBlockArmor)originalModel).layers.get(layer).quads = itemQuadsMap.get(stack.getItem()).get(layer);
				BlockArmorItem item = (BlockArmorItem) stack.getItem();
				((BakedDynBlockArmor)originalModel).particles = item.set.getTextureInfo(item.getSlot()).sprite;
			}
			return originalModel;
		}
	}

	private enum Layer {
		BASE, TEMPLATE, COVER;

		/**Need to render cover transparent so it isn't culled*/
		public RenderType getRenderType(ItemStack stack) {
			if (this == COVER)
				return Sheets.translucentCullBlockSheet();
			else if (Minecraft.useShaderTransparency())
				return Sheets.cutoutBlockSheet(); // not translucent, but best we can do in Fabulous graphics?
			else
				return Sheets.translucentItemSheet(); // renders invisible in Fabulous graphics
		}
	}

	private static final class BakedDynBlockArmor implements BakedModel {

		@Nullable
		public final Layer layer;
		private HashMap<Layer, BakedDynBlockArmor> layers = Maps.newHashMap();
		private ImmutableList<BakedQuad> quads = ImmutableList.of();
		private TextureAtlasSprite particles;

		public BakedDynBlockArmor() {
			this(null);
			for (Layer layer : Layer.values())
				layers.put(layer, new BakedDynBlockArmor(layer));
		}

		private BakedDynBlockArmor(Layer layer) {
			this.layer = layer;
		}

		@Override
		public ItemOverrides getOverrides() {
			return BakedDynBlockArmorOverrideHandler.INSTANCE;
		}

		/**Applies this model's transformation for the perspective (does not push the pose stack)*/
		@Override
		public BakedModel applyTransform(ItemTransforms.TransformType cameraTransformType, PoseStack poseStack, boolean applyLeftHandTransform) {
			Transformation transformation = TRANSFORMS.get(cameraTransformType);
			if (transformation != null && !transformation.isIdentity()) {
				Vector3f translation = transformation.getTranslation();
				poseStack.translate(translation.x(), translation.y(), translation.z());
				poseStack.mulPose(transformation.getLeftRotation());
				Vector3f scale = transformation.getScale();
				poseStack.scale(scale.x(), scale.y(), scale.z());
				poseStack.mulPose(transformation.getRightRotation());
			}
			return this;
		}

		@Override
		public List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction side, RandomSource rand) {
			if (side == null)
				return quads;
			return ImmutableList.of();
		}

		/**Need to render cover layer transparent so it isn't culled - each layer is its own pass*/
		@Override
		public List<BakedModel> getRenderPasses(ItemStack stack, boolean fabulous) {
			List<BakedModel> ret = Lists.newArrayList();
			for (Layer layer : Layer.values())
				ret.add(this.layers.get(layer));
			return ret;
		}

		/**Only called on models returned from {@link #getRenderPasses(ItemStack, boolean)}*/
		@Override
		public List<RenderType> getRenderTypes(ItemStack stack, boolean fabulous) {
			return ImmutableList.of(this.layer.getRenderType(stack));
		}

		public boolean usesBlockLight() { return false; }
		public boolean useAmbientOcclusion() { return true;  }
		public boolean isGui3d() { return false; }
		public boolean isCustomRenderer() { return false; }
		public TextureAtlasSprite getParticleIcon() { return particles; }
		public ItemTransforms getTransforms() { return ItemTransforms.NO_TRANSFORMS; }
	}

}
