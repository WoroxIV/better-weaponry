package com.betterweaponry.forge;

import com.betterweaponry.BetterWeaponry;
import com.betterweaponry.network.ComboSyncPayload;
import com.betterweaponry.network.DualSwingPayload;
import com.betterweaponry.network.EditorResultPayload;
import com.betterweaponry.network.OpenEditorPayload;
import com.betterweaponry.network.Payload;
import com.betterweaponry.network.SaveDocumentPayload;
import com.betterweaponry.network.WeaponDataSyncPayload;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

import java.util.function.Consumer;
import java.util.function.Function;

/** Les paquets du mod sur Forge. Les réceptions côté client sont dans {@link ForgeClientPackets}, jamais chargée sur un serveur dédié. */
final class ForgeNetwork {
    /** À augmenter quand le format d'un paquet change : un client et un serveur différents refuseront de se connecter. */
    private static final String PROTOCOL_VERSION = "1";
    private static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(new ResourceLocation(BetterWeaponry.MODID, "main"),
            () -> PROTOCOL_VERSION, PROTOCOL_VERSION::equals, PROTOCOL_VERSION::equals);
    private static int nextId;

    private ForgeNetwork() {
    }

    static void register() {
        toClient(WeaponDataSyncPayload.class, WeaponDataSyncPayload::read, payload -> WeaponDataSyncPayload.handle(payload));
        toClient(ComboSyncPayload.class, ComboSyncPayload::read, payload -> ForgeClientPackets.combo(payload));
        toClient(DualSwingPayload.class, DualSwingPayload::read, payload -> ForgeClientPackets.dualSwing(payload));
        toClient(OpenEditorPayload.class, OpenEditorPayload::read, payload -> OpenEditorPayload.handle(payload));
        toClient(EditorResultPayload.class, EditorResultPayload::read, payload -> EditorResultPayload.handle(payload));

        CHANNEL.messageBuilder(SaveDocumentPayload.class, nextId++, NetworkDirection.PLAY_TO_SERVER)
                .encoder(SaveDocumentPayload::write)
                .decoder(SaveDocumentPayload::read)
                .consumerMainThread((payload, context) -> {
                    ServerPlayer sender = context.get().getSender();
                    if (sender != null) SaveDocumentPayload.handle(payload, sender);
                })
                .add();
    }

    private static <T extends Payload> void toClient(Class<T> type, Function<FriendlyByteBuf, T> reader, Consumer<T> handler) {
        CHANNEL.messageBuilder(type, nextId++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(Payload::write)
                .decoder(reader)
                .consumerMainThread((payload, context) -> handler.accept(payload))
                .add();
    }

    static void sendToPlayer(ServerPlayer player, Payload payload) {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), payload);
    }

    static void sendToPlayersTracking(Entity entity, Payload payload) {
        CHANNEL.send(PacketDistributor.TRACKING_ENTITY.with(() -> entity), payload);
    }

    static void sendToServer(Payload payload) {
        CHANNEL.sendToServer(payload);
    }
}
