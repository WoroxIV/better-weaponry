package com.betterweaponry.tutorial;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/** Décor d'une scène de tutoriel : les deux blocs du sol en damier et deux objets posés aux coins. */
public enum TutorialTheme {
    WORKSHOP(Blocks.OAK_PLANKS, Blocks.SPRUCE_PLANKS, Blocks.BARREL, Blocks.CRAFTING_TABLE),
    DOJO(Blocks.BAMBOO_PLANKS, Blocks.BAMBOO_MOSAIC, Blocks.BAMBOO_BLOCK, Blocks.HAY_BLOCK),
    FORTRESS(Blocks.POLISHED_DEEPSLATE, Blocks.DEEPSLATE_TILES, Blocks.ANVIL, Blocks.LANTERN),
    ARENA(Blocks.SMOOTH_SANDSTONE, Blocks.CUT_SANDSTONE, Blocks.TARGET, Blocks.HAY_BLOCK),
    FORGE(Blocks.POLISHED_BLACKSTONE, Blocks.POLISHED_BLACKSTONE_BRICKS, Blocks.SMITHING_TABLE, Blocks.BLAST_FURNACE),
    MEADOW(Blocks.MOSS_BLOCK, Blocks.GRASS_BLOCK, Blocks.HAY_BLOCK, Blocks.FLOWERING_AZALEA_LEAVES);

    private final Block lightFloor;
    private final Block darkFloor;
    private final Block firstProp;
    private final Block secondProp;

    TutorialTheme(Block lightFloor, Block darkFloor, Block firstProp, Block secondProp) {
        this.lightFloor = lightFloor;
        this.darkFloor = darkFloor;
        this.firstProp = firstProp;
        this.secondProp = secondProp;
    }

    public BlockState lightFloor() {
        return lightFloor.defaultBlockState();
    }

    public BlockState darkFloor() {
        return darkFloor.defaultBlockState();
    }

    public BlockState firstProp() {
        return firstProp.defaultBlockState();
    }

    public BlockState secondProp() {
        return secondProp.defaultBlockState();
    }
}
