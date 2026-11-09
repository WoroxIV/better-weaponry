package com.betterweaponry.registry;

import com.betterweaponry.BetterWeaponry;
import net.minecraft.core.UUIDUtil;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

import java.util.List;
import java.util.UUID;
import java.util.function.Supplier;

/** Données conservées sur les joueurs. */
public final class ModAttachments {
    public static final DeferredRegister<AttachmentType<?>> ATTACHMENT_TYPES = DeferredRegister.create(NeoForgeRegistries.ATTACHMENT_TYPES, BetterWeaponry.MODID);

    /** Shurikens dont le dernier point de durabilité a été lancé : le ramasser recrée l'objet. */
    public static final Supplier<AttachmentType<List<UUID>>> BROKEN_SHURIKENS = ATTACHMENT_TYPES.register("broken_shurikens",
            () -> AttachmentType.<List<UUID>>builder(() -> List.of()).serialize(UUIDUtil.CODEC.listOf()).copyOnDeath().build());

    private ModAttachments() {
    }
}
