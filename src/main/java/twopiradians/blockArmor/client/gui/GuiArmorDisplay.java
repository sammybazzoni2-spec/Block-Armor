package twopiradians.blockArmor.client.gui;

import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

/**Developer-only screen used to take promotional screenshots of armor sets (disabled).
 * The 1.18 rendering code relied on PoseStack/Matrix4f/Quaternion screen rendering that no longer exists in 1.20.1
 * (it would have to be rewritten on top of GuiGraphics), so it is stubbed out - see the 1.18.1 branch for the original.*/
@OnlyIn(Dist.CLIENT)
public class GuiArmorDisplay extends Screen {

	/** Should armor display be opened on chat event? */
	public static final boolean DISPLAY_ARMOR_GUI = false;

	public GuiArmorDisplay() {
		super(Component.empty());
	}

}
