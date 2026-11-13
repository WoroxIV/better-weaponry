package com.betterweaponry.api;

import com.betterweaponry.item.WeaponMaterial;

import java.util.List;

/**
 * Point d'entrée des addons : un autre mod déclare ici ses matériaux, et Better Weaponry crée pour chacun toutes ses
 * armes (13 types), un bouclier, un arc, une arbalète et un shuriken, sous le namespace de l'addon
 * ({@code <namespace>:<matériau>_katana}).
 *
 * <h2>Déclarer un fournisseur</h2>
 * <ul>
 *   <li>NeoForge / Forge : dans {@code META-INF/neoforge.mods.toml} (ou {@code mods.toml}) de l'addon,
 *       <pre>{@code
 * [modproperties.<modid de l'addon>]
 * better_weaponry_materials = "com.exemple.MesMateriaux"
 * }</pre></li>
 *   <li>Fabric : dans {@code fabric.mod.json}, {@code "entrypoints": {"better_weaponry_materials": ["com.exemple.MesMateriaux"]}}.</li>
 * </ul>
 * La classe doit avoir un constructeur public sans argument.
 *
 * <h2>Règles</h2>
 * <ul>
 *   <li>{@link #materials()} est appelé une seule fois, très tôt (avant l'enregistrement des items) : ne pas y toucher
 *       aux registres, ni appeler {@link com.betterweaponry.item.WeaponMaterials#all()}.</li>
 *   <li>Chaque matériau est créé avec {@link WeaponMaterial#builder(String, String, net.minecraft.world.item.Tier)}
 *       (ou la version avec {@code stats}), avec le namespace de l'addon.</li>
 *   <li>L'addon fournit lui-même les ressources de ses items : palette {@code assets/<namespace>/textures/palettes/<id>.png}
 *       (les textures sont alors fabriquées au chargement à partir des modèles de Better Weaponry), modèles, traductions,
 *       recettes et tags (dont {@code better_weaponry:<type>s}, par lesquels Combat Ascended reconnaît les armes).</li>
 *   <li>Dans les fichiers de données, le matériau s'écrit {@code "<namespace>:<id>"} (voir {@link WeaponMaterial#key()}).</li>
 * </ul>
 */
public interface WeaponMaterialProvider {

    /** Nom de la propriété (NeoForge / Forge) et du point d'entrée (Fabric) qui désignent la classe du fournisseur. */
    String KEY = "better_weaponry_materials";

    /** Les matériaux de l'addon, dans l'ordre où leurs armes doivent apparaître. */
    List<WeaponMaterial> materials();
}
