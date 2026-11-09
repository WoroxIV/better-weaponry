package com.betterweaponry.mixin;

import com.betterweaponry.platform.Platform;
import com.betterweaponry.data.WeaponData;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Place la particule de coup balayé à une distance proportionnelle à la portée de l'arme.
 * <p>
 * En vanilla, elle apparaît toujours à 1 bloc devant le joueur, ce qui correspond à la portée de base (3 blocs).
 * On garde ce rapport : distance = portée / 3. Une épée vanilla ne change donc pas, une dague (portée 2)
 * balaie à ~0.67 bloc et une lance (portée 5) à ~1.67 bloc.
 * La portée lue est celle de l'attribut du joueur : elle suit toutes les armes, y compris la main secondaire
 * en dual wield (les mains sont échangées pendant l'attaque).
 */
@Mixin(Player.class)
public abstract class SweepParticleMixin {
    @Unique private static final double VANILLA_REACH = 3.0D;

    @WrapOperation(
            method = "sweepAttack",
            require = 0,
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/server/level/ServerLevel;sendParticles(Lnet/minecraft/core/particles/ParticleOptions;DDDIDDDD)I")
    )
    private int betterweaponry$placeSweepAtReach(ServerLevel level, ParticleOptions particle,
                                                  double x, double y, double z, int count,
                                                  double dirX, double dirY, double dirZ, double speed,
                                                  Operation<Integer> original) {
        Player player = (Player) (Object) this;
        if (!WeaponData.rules().sweepActive()) return original.call(level, particle, x, y, z, count, dirX, dirY, dirZ, speed);
        // dirX / dirZ : direction du regard à l'horizontale (vecteur unitaire), déjà calculée par le jeu
        double distance = Platform.get().entityReach(player) / VANILLA_REACH;
        return original.call(level, particle,
                player.getX() + dirX * distance, y, player.getZ() + dirZ * distance,
                count, dirX, dirY, dirZ, speed);
    }
}
