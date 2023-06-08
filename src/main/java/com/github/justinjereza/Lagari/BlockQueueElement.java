package com.github.justinjereza.Lagari;

import java.util.Vector;

import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;

/** Encapsulates a block and its faces that will be checked for use in an {@linkplain java.util.AbstractCollection}. */
public class BlockQueueElement implements java.io.Serializable {
    private static final long serialVersionUID = 0L;

    /** Block of which specified faces will be checked. */
    private Block block;
    /** The faces that will be checked. */
    private Vector<BlockFace> blockFaces;

    /**
     * Creates a BlockQueueElement that contains the specified block and the faces that will be checked for it.
     * @param block Block of which specified faces will be checked.
     * @param blockFaces The faces that will be checked for the specified block.
     */
    public BlockQueueElement(Block block, Vector<BlockFace> blockFaces) {
        this.block = block;
        this.blockFaces = blockFaces;
    }

    /** @return Block of which specified faces will be checked. */
    public Block getBlock() {
        return block;
    }

    /** @param block Block of which specified faces will be checked. */
    public void setBlock(Block block) {
        this.block = block;
    }

    /** @return The faces that will be checked. */
    public Vector<BlockFace> getBlockFaces() {
        return blockFaces;
    }

    /** @param blockFaces The faces to check. */
    public void setBlockFace(Vector<BlockFace> blockFaces) {
        this.blockFaces = blockFaces;
    }
}
