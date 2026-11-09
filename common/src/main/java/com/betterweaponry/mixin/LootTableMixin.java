package com.betterweaponry.mixin;

import com.betterweaponry.data.WeaponData;
import com.betterweaponry.item.LootBooks;
import com.betterweaponry.item.VanillaGear;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.LootTable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

/** Dans tous les butins du jeu, l'équipement vanilla devient une version du mod, et certains coffres gagnent un livre. */
@Mixin(LootTable.class)
public abstract class LootTableMixin {
    @Shadow @Final private ResourceLocation randomSequence;

    @ModifyReturnValue(method = "getRandomItems(Lnet/minecraft/world/level/storage/loot/LootContext;)Lit/unimi/dsi/fastutil/objects/ObjectArrayList;", at = @At("RETURN"))
    private ObjectArrayList<ItemStack> betterweaponry$modifyLoot(ObjectArrayList<ItemStack> loot, LootContext context) {
        VanillaGear.replaceInLoot(loot, context.getRandom(), WeaponData.gear());
        if (randomSequence != null) LootBooks.maybeAdd(randomSequence, loot, context);
        return loot;
    }
}
