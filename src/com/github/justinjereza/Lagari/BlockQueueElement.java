package com.github.justinjereza.Lagari;

import java.util.Vector;

import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;

public class BlockQueueElement implements java.io.Serializable {
    private static final long serialVersionUID = 0L;

    private Block block;
    private Vector<BlockFace> blockFaces;

    public BlockQueueElement() {
    }

    public BlockQueueElement(Block block, Vector<BlockFace> blockFaces) {
        this.block = block;
        this.blockFaces = blockFaces;
    }

    public Block getBlock() {
        return block;
    }

    public void setBlock(Block block) {
        this.block = block;
    }

    public Vector<BlockFace> getBlockFaces() {
        return blockFaces;
    }

    public void setBlockFace(Vector<BlockFace> blockFaces) {
        this.blockFaces = blockFaces;
    }
}
