package com.github.justinjereza.Lagari;

import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Vector;
import java.util.logging.Logger;

import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

/** Lagari - A Minecraft plugin for breaking adjacent blocks. */
public class Main extends JavaPlugin implements Listener {
    private static final boolean DEBUG = false;

    private final Logger logger = getLogger();
    private static enum Modes { CLASSIC, CLASSIC_LEAVES, FULL, FULL_NOLEAVES };

    // Vector of faces that will be checked.
    private static final Vector<BlockFace> blockFaces = new Vector<BlockFace>(Arrays.
            asList(BlockFace.NORTH, BlockFace.SOUTH, BlockFace.EAST, BlockFace.WEST, BlockFace.UP, BlockFace.DOWN));
    // Vector of faces that will be checked in diagonal cases.
    private static final Vector<BlockFace> verticalFaces = new Vector<BlockFace>(Arrays.asList(BlockFace.UP, BlockFace.DOWN));
    // HashMap of faces that will be used in each direction traversed to prevent backtracking.
    private static final HashMap<BlockFace, Vector<BlockFace>> blockFaceMap = new HashMap<BlockFace, Vector<BlockFace>>(6);

    // Initialize blockFaceMap from which we select which faces will get checked in a certain direction.
    static {
        Vector<BlockFace> v;
        for (BlockFace face : blockFaces) {
            v = new Vector<BlockFace>(blockFaces);
            // Remove the face opposite the face we're currently making a map
            // for so that it doesn't get checked which removes backtracking.
            v.remove(face.getOppositeFace());
            blockFaceMap.put(face, v);
        }
    }

    private static Modes mode;
    private static boolean enchantments;
    private static final Vector<Material> logList = new Vector<Material>();
    private static final Vector<Material> leafList = new Vector<Material>();
    private static final Vector<Material> toolList = new Vector<Material>();

    /** Initializes the plugin when it is enabled. */
    @Override
    public void onEnable() {
        saveResource("config.yml", false);
        getServer().getPluginManager().registerEvents(this, this);
    }

    /**
     * Event handler for when a block is broken.
     * @param event The dispatched event for when a block is broken.
     */
    @EventHandler
    public void blockBreakHandler(BlockBreakEvent event) {
        ItemStack tool = event.getPlayer().getInventory().getItemInMainHand();
        Material toolMaterial = tool.getType();

        reloadConfig();
        FileConfiguration config = getConfig();
        mode = Modes.valueOf(config.getString("mode"));
        enchantments = config.getBoolean("enchantments");

        if (DEBUG) {
            logger.info("Mode: " + mode);
            logger.info("Enchantments: " + enchantments);
            logger.info("Tool material: " + toolMaterial);
        }

        Material m;
        // Load valid log materials from configuration file.
        for (String s : config.getStringList("logs")) {
            m = Material.valueOf(s);
            if (! logList.contains(m)) {
                logList.add(m);
            }
        }
        // Load valid leaf materials from configuration file.
        for (String s : config.getStringList("leaves")) {
            m = Material.valueOf(s);
            if (! leafList.contains(m)) {
                leafList.add(m);
            }
        }
        // Load valid tool materials from configuration file.
        for (String s : config.getStringList("tools")) {
            m = Material.valueOf(s);
            if (! toolList.contains(m)) {
                toolList.add(m);
            }
        }

        Block block = event.getBlock();
        // Break blocks if current tool is valid and current block is a valid log material.
        if (toolList.contains(toolMaterial) && logList.contains(block.getType())) {
            Vector<BlockFace> faceMap;
            // With full modes, check all faces of the initial block. Otherwise, don't check it downwards.
            if (mode == Modes.FULL || mode == Modes.FULL_NOLEAVES) {
                faceMap = blockFaces;
            } else {
                faceMap = blockFaceMap.get(BlockFace.UP);
            }
            breakBlocks(tool, new BlockQueueElement(block, faceMap));
        }
    }

    private boolean isValidBlock(Block b) {
        Material m = b.getType();
        return logList.contains(m) || (mode == Modes.CLASSIC_LEAVES || mode == Modes.FULL) && leafList.contains(m);
    }

    private void breakBlock(ItemStack t, Block b) {
        if (enchantments) {
            b.breakNaturally(t);
        } else {
            b.breakNaturally();
        }
    }

    private void breakBlocks(ItemStack tool, BlockQueueElement blockQueueElement) {
        int x = 0;
        Block block, cardinalBlock, diagonalBlock;
        LinkedList<BlockQueueElement> blockQueue = new LinkedList<BlockQueueElement>(Arrays.asList(blockQueueElement));

        // Break the first block.
        breakBlock(tool, blockQueueElement.getBlock());

        // We've broken the first block but it is still in the queue so the loop starts executing.
        while (! blockQueue.isEmpty()) {
            if (DEBUG && blockQueue.size() > x) {
                x = blockQueue.size();
            }

            // Remove the first element of the queue and get its block.
            blockQueueElement = blockQueue.remove();
            block = blockQueueElement.getBlock();

            // Iterate through each face of the current block.
            for (BlockFace i : blockQueueElement.getBlockFaces()) {
                // Classic modes only break blocks upwards.
                if (i != BlockFace.UP && (mode == Modes.CLASSIC || mode == Modes.CLASSIC_LEAVES)) {
                    continue;
                }

                // Get the block adjacent to the current face.
                cardinalBlock = block.getRelative(i);

                if (isValidBlock(cardinalBlock)) {
                    breakBlock(tool, cardinalBlock);
                    // Add the adjacent block to the queue for processing and select
                    // the face map that doesn't backtrack through the current face.
                    blockQueue.add(new BlockQueueElement(cardinalBlock, blockFaceMap.get(i)));
                }

                // If we're not checking a face that is above or below the current block,
                // check the top and bottom of the adjacent block for blocks that can be broken.
                if (! verticalFaces.contains(i)) {
                    for (BlockFace j : verticalFaces) {
                        // Get the block above or below the adjacent block.
                        diagonalBlock = cardinalBlock.getRelative(j);
                        if (isValidBlock(diagonalBlock)) {
                            breakBlock(tool, diagonalBlock);
                            // As with the blocks immediately adjacent to the current face,
                            // add the block to the queue for processing.
                            blockQueue.add(new BlockQueueElement(diagonalBlock, blockFaceMap.get(j)));
                        }
                    }
                }
            }
        }

        if (DEBUG) {
            logger.info("Largest queue size: " + x);
        }
    }
}
