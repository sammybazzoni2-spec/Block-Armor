package twopiradians.blockArmor.common;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber.Bus;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.network.ChannelBuilder;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.SimpleChannel;
import twopiradians.blockArmor.packet.CActivateSetEffectPacket;
import twopiradians.blockArmor.packet.SDevColorsPacket;
import twopiradians.blockArmor.packet.SSyncCooldownsPacket;
import twopiradians.blockArmor.client.ClientProxy;
import twopiradians.blockArmor.client.key.KeyActivateSetEffect;

@Mod(value = BlockArmor.MODID)
@Mod.EventBusSubscriber(bus = Bus.MOD)
public class BlockArmor {
	
	// FIXME compatibility with configmenusforge - only when removing armor sets and saving?
			
	/**Changelog
	 * Fixed a crash when changing dimensions
	 * 
	 * FIXME (1.16.5) more reports of chests being empty - desert temple chests (tested in SP and not able to reproduce)
	 * FIXME (1.16.5) can't join server with MineColonies bc of "Exception: io.nettyhandler.codec.DecoderException: java.io.IOException: Payload may not be larger than 1048576 bytes"
	 */

	public static final String MODNAME = "Block Armor"; 
	public static final String MODID = "blockarmor";
	
	public static final Logger LOGGER = LogManager.getLogger(MODID);
	public static final SimpleChannel NETWORK = ChannelBuilder
			.named(ResourceLocation.fromNamespaceAndPath(MODID, "main_channel"))
			.networkProtocolVersion(1)
			.optional() // mod is optional on either side
			.simpleChannel()
			.messageBuilder(SDevColorsPacket.class, 0, NetworkDirection.PLAY_TO_CLIENT)
				.encoder(SDevColorsPacket::encode).decoder(SDevColorsPacket::decode).consumerMainThread(SDevColorsPacket.Handler::handle).add()
			.messageBuilder(CActivateSetEffectPacket.class, 1, NetworkDirection.PLAY_TO_SERVER)
				.encoder(CActivateSetEffectPacket::encode).decoder(CActivateSetEffectPacket::decode).consumerMainThread(CActivateSetEffectPacket.Handler::handle).add()
			.messageBuilder(SSyncCooldownsPacket.class, 2, NetworkDirection.PLAY_TO_CLIENT)
				.encoder(SSyncCooldownsPacket::encode).decoder(SSyncCooldownsPacket::decode).consumerMainThread(SSyncCooldownsPacket.Handler::handle).add()
			.build();
	public static KeyActivateSetEffect key = new KeyActivateSetEffect();

	public BlockArmor() {}
	
	@SubscribeEvent
	public static void onCommonSetupEvent(FMLCommonSetupEvent event) {
		event.enqueueWork(CommonProxy::setup);
	}
	
	@SubscribeEvent
	public static void onClientSetupEvent(FMLClientSetupEvent event) {
		event.enqueueWork(ClientProxy::setup);
	}

}