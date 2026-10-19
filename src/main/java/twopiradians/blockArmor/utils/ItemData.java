package twopiradians.blockArmor.utils;

import java.util.function.Consumer;

import net.minecraft.core.HolderLookup;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;

/**Small helpers for the custom data component (replaces the old stack NBT tag in 1.20.5+).
 * Writes only happen when a value actually changes, so stacks aren't needlessly marked as changed.*/
public final class ItemData {

	private ItemData() {}

	/**Does the stack's custom data contain this key*/
	public static boolean has(ItemStack stack, String key) {
		CustomData data = stack.get(DataComponents.CUSTOM_DATA);
		return data != null && data.contains(key);
	}

	public static boolean getBoolean(ItemStack stack, String key) {
		CustomData data = stack.get(DataComponents.CUSTOM_DATA);
		return data != null && data.getUnsafe().getBoolean(key);
	}

	public static void setBoolean(ItemStack stack, String key, boolean value) {
		if (has(stack, key) && getBoolean(stack, key) == value)
			return;
		update(stack, tag -> tag.putBoolean(key, value));
	}

	public static String getString(ItemStack stack, String key) {
		CustomData data = stack.get(DataComponents.CUSTOM_DATA);
		return data == null ? "" : data.getUnsafe().getString(key);
	}

	public static void setString(ItemStack stack, String key, String value) {
		if (has(stack, key) && getString(stack, key).equals(value))
			return;
		update(stack, tag -> tag.putString(key, value));
	}

	public static void remove(ItemStack stack, String key) {
		if (has(stack, key))
			update(stack, tag -> tag.remove(key));
	}

	public static void update(ItemStack stack, Consumer<CompoundTag> updater) {
		CustomData.update(DataComponents.CUSTOM_DATA, stack, updater);
	}

	/**A copy of the stack's custom data*/
	public static CompoundTag getTag(ItemStack stack) {
		CustomData data = stack.get(DataComponents.CUSTOM_DATA);
		return data == null ? new CompoundTag() : data.copyTag();
	}

	/**Registry access usable on either side, for saving/loading stacks to nbt*/
	public static HolderLookup.Provider registries() {
		net.minecraft.server.MinecraftServer server = net.minecraftforge.server.ServerLifecycleHooks.getCurrentServer();
		if (server != null)
			return server.registryAccess();
		return ClientHelper.registries();
	}

	private static class ClientHelper {
		static HolderLookup.Provider registries() {
			if (net.minecraftforge.fml.loading.FMLEnvironment.dist.isClient()) {
				net.minecraft.client.Minecraft mc = net.minecraft.client.Minecraft.getInstance();
				if (mc.level != null)
					return mc.level.registryAccess();
			}
			return RegistryAccess.EMPTY;
		}
	}

}
