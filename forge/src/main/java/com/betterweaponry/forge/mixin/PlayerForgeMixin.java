package com.betterweaponry.forge.mixin;

import com.betterweaponry.item.WeaponItem;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.common.ForgeMod;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

/** Ce que Forge décide dans le joueur à partir de l'objet seul, sans demander au mod. */
@Mixin(Player.class)
public abstract class PlayerForgeMixin {
    @Unique private static final double CREATIVE_REACH_BONUS = 3.0D;

    /** Remplace la méthode de Forge, qui ajoute 3 blocs en créatif : une arme du mod porte exactement à la distance affichée. */
    public double getEntityReach() {
        Player player = (Player) (Object) this;
        double reach = player.getAttributeValue(ForgeMod.ENTITY_REACH.get());
        return reach == 0.0D || !player.isCreative() || WeaponItem.hasExactReach(player) ? reach : reach + CREATIVE_REACH_BONUS;
    }
}
