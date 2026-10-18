package twopiradians.blockArmor.client.model;

import java.awt.Color;
import java.util.List;
import java.util.Map;

import javax.annotation.Nullable;

import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.BlockElement;
import net.minecraft.client.renderer.block.model.ItemOverrides;
import net.minecraft.client.renderer.block.model.ItemTransforms;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.client.resources.model.ModelState;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.ChunkRenderTypeSet;
import net.minecraftforge.client.ForgeRenderTypes;
import net.minecraftforge.client.RenderTypeGroup;
import net.minecraftforge.client.model.CompositeModel;
import net.minecraftforge.client.model.EmptyModel;
import net.minecraftforge.client.model.SimpleModelState;
import net.minecraftforge.client.model.data.ModelData;
import net.minecraftforge.client.model.geometry.UnbakedGeometryHelper;
import com.mojang.math.Transformation;
import twopiradians.blockArmor.common.BlockArmor;
import twopiradians.blockArmor.common.item.ArmorSet;
import twopiradians.blockArmor.common.item.BlockArmorItem;
import net.minecraftforge.registries.ForgeRegistries;

/**Inventory icons for Block Armor.
 * Each armor item gets a {@link DynamicBlockArmorModel} added to the baked model registry (see ClientProxy), 
 * which is filled with quads once the block textures are available (see {@link #createInventoryIcons(Map)}).*/
@OnlyIn(Dist.CLIENT)
public final class ModelBAItem {

	/**Tint indices: 0 is the base layer (never tinted), the rest use the block's tint color*/
	public static final int TINT_TEMPLATE_1 = 1;
	public static final int TINT_TEMPLATE_2 = 2;
	public static final int TINT_COVER = 3;

	private static final RenderTypeGroup OPAQUE_LAYER = new RenderTypeGroup(RenderType.cutout(), ForgeRenderTypes.ITEM_LAYERED_CUTOUT.get());
	private static final RenderTypeGroup TRANSLUCENT_LAYER = new RenderTypeGroup(RenderType.translucent(), ForgeRenderTypes.ITEM_UNSORTED_TRANSLUCENT.get());

	/**Model locations for all armor items*/
	public static ModelResourceLocation getLocation(BlockArmorItem item) {
		return new ModelResourceLocation(ForgeRegistries.ITEMS.getKey(item), "inventory");
	}

	/**Creates inventory icons (via quads) for each Block Armor piece and puts them in the corresponding models.
	 * @return number of icons created*/
	public static int createInventoryIcons(Map<ResourceLocation, BakedModel> models) {
		int numIcons = 0;

		TextureAtlas atlas = Minecraft.getInstance().getModelManager().getAtlas(TextureAtlas.LOCATION_BLOCKS);
		ItemTransforms transforms = getGeneratedTransforms(models);
		ModelState state = new SimpleModelState(Transformation.identity());

		for (ArmorSet set : ArmorSet.allSets) {
			BlockArmorItem[] armor = new BlockArmorItem[] {set.helmet, set.chestplate, set.leggings, set.boots};
			for (BlockArmorItem item : armor) {
				if (item == null)
					continue;
				BakedModel existing = models.get(getLocation(item));
				if (!(existing instanceof DynamicBlockArmorModel))
					continue;
				try {
					TextureAtlasSprite sprite = item.set.getTextureInfo(item.getSlot()).sprite;
					if (!loggedDiagnostics) {
						loggedDiagnostics = true;
						long iconSprites = atlas.getTextureLocations().stream().filter(l -> l.getNamespace().equals(BlockArmor.MODID)).count();
						BlockArmor.LOGGER.info("Block Armor icon diagnostics: block sprite for "+item+" = "+sprite.contents().name()
								+ " (missing="+isMissing(sprite)+"), blockarmor sprites in blocks atlas = "+iconSprites
								+ ", helmet base missing = "+isMissing(atlas.getSprite(new ResourceLocation(BlockArmor.MODID, "items/icons/block_armor_helmet_base")))
								+ ", helmet template missing = "+isMissing(atlas.getSprite(new ResourceLocation(BlockArmor.MODID, "items/icons/block_armor_helmet1_template")))
								+ ", cover missing = "+isMissing(atlas.getSprite(new ResourceLocation(BlockArmor.MODID, "items/icons/block_armor_helmet_cover"))));
					}
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

					ResourceLocation modelLocation = ForgeRegistries.ITEMS.getKey(item);
					CompositeModel.Baked.Builder builder = CompositeModel.Baked.builder(true, false, false, sprite, ItemOverrides.EMPTY, transforms);

					//Base texture
					TextureAtlasSprite baseSprite = atlas.getSprite(new ResourceLocation(BlockArmor.MODID, "items/icons/block_armor_"+armorType+"_base"));
					List<BlockElement> baseElements = UnbakedGeometryHelper.createUnbakedItemElements(0, baseSprite.contents());
					builder.addQuads(OPAQUE_LAYER, UnbakedGeometryHelper.bakeElements(baseElements, mat -> baseSprite, state, modelLocation));

					//Template textures for left and right halves - the mask is the template's shape, the texture is the block's
					for (int half = 1; half <= 2; ++half) {
						TextureAtlasSprite template = atlas.getSprite(new ResourceLocation(BlockArmor.MODID, "items/icons/block_armor_"+armorType+half+"_template"));
						List<BlockElement> elements = UnbakedGeometryHelper.createUnbakedItemMaskElements(half, template.contents());
						builder.addQuads(OPAQUE_LAYER, UnbakedGeometryHelper.bakeElements(elements, mat -> sprite, state, modelLocation));
					}

					//Cover texture (translucent so it isn't culled)
					TextureAtlasSprite cover = atlas.getSprite(new ResourceLocation(BlockArmor.MODID, "items/icons/block_armor_"+armorType+"_cover"));
					List<BlockElement> coverElements = UnbakedGeometryHelper.createUnbakedItemMaskElements(TINT_COVER, cover.contents());
					builder.addQuads(TRANSLUCENT_LAYER, UnbakedGeometryHelper.bakeElements(coverElements, mat -> cover, state, modelLocation));

					builder.setParticle(sprite);
					((DynamicBlockArmorModel) existing).delegate = builder.build();
					numIcons++;
				}
				catch (Exception e) {
					BlockArmor.LOGGER.error("Failed to create inventory icon for "+item, e);
				}
			}
		}
		return numIcons;
	}

	private static boolean loggedDiagnostics = false;

	private static boolean isMissing(TextureAtlasSprite sprite) {
		return sprite.contents().name().equals(net.minecraft.client.renderer.texture.MissingTextureAtlasSprite.getLocation());
	}

	/**Transforms of item/generated models (from a vanilla item) so armor displays like a flat item*/
	private static ItemTransforms getGeneratedTransforms(Map<ResourceLocation, BakedModel> models) {
		BakedModel stick = models.get(new ModelResourceLocation(new ResourceLocation("minecraft", "stick"), "inventory"));
		return stick != null ? stick.getTransforms() : ItemTransforms.NO_TRANSFORMS;
	}

	/**Tint color for an armor item's icon layers (-1 = no tint)*/
	public static int getTintColor(ItemStack stack, int tintIndex) {
		if (tintIndex > 0 && stack.getItem() instanceof BlockArmorItem) {
			BlockArmorItem item = (BlockArmorItem) stack.getItem();
			int color = item.set.getTextureInfo(item.getSlot()).color;
			if (color != -1) 
				return new Color(((color >> 16) & 0xFF) / 255f, ((color >> 8) & 0xFF) / 255f, (color & 0xFF) / 255f, 1f).getRGB(); // alpha 1.0f (since sometimes 0f)
		}
		return -1;
	}

	/**Placeholder model that is registered when models are baked (textures aren't available yet) 
	 * and later gets its real model assigned.*/
	public static final class DynamicBlockArmorModel implements BakedModel {

		public volatile BakedModel delegate = EmptyModel.BAKED;

		@Override
		public List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction side, RandomSource rand) {
			return delegate.getQuads(state, side, rand);
		}

		@Override
		public List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction side, RandomSource rand, ModelData data, @Nullable RenderType renderType) {
			return delegate.getQuads(state, side, rand, data, renderType);
		}

		@Override
		public boolean useAmbientOcclusion() {
			return delegate.useAmbientOcclusion();
		}

		@Override
		public boolean isGui3d() {
			return delegate.isGui3d();
		}

		@Override
		public boolean usesBlockLight() {
			return delegate.usesBlockLight();
		}

		@Override
		public boolean isCustomRenderer() {
			return delegate.isCustomRenderer();
		}

		@Override
		public TextureAtlasSprite getParticleIcon() {
			return delegate.getParticleIcon();
		}

		@Override
		public ItemTransforms getTransforms() {
			return delegate.getTransforms();
		}

		@Override
		public ItemOverrides getOverrides() {
			return delegate.getOverrides();
		}

		@Override
		public BakedModel applyTransform(ItemDisplayContext context, PoseStack poseStack, boolean leftHand) {
			return delegate.applyTransform(context, poseStack, leftHand);
		}

		@Override
		public List<BakedModel> getRenderPasses(ItemStack stack, boolean fabulous) {
			return delegate.getRenderPasses(stack, fabulous);
		}

		@Override
		public List<RenderType> getRenderTypes(ItemStack stack, boolean fabulous) {
			return delegate.getRenderTypes(stack, fabulous);
		}

		@Override
		public ChunkRenderTypeSet getRenderTypes(BlockState state, RandomSource rand, ModelData data) {
			return delegate.getRenderTypes(state, rand, data);
		}

	}

}
