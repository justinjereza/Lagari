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

public class Main extends JavaPlugin implements Listener {
    private final boolean DEBUG = true;
    private final Logger logger = getLogger();
    private static enum Modes { CLASSIC, CLASSIC_LEAVES, FULL, FULL_NOLEAVES };

    // Vector of faces that will be checked.
    private static final Vector<BlockFace> blockFaces = new Vector<BlockFace>(Arrays.
            asList(BlockFace.NORTH, BlockFace.SOUTH, BlockFace.EAST, BlockFace.WEST, BlockFace.UP, BlockFace.DOWN));
    // Vector of faces that will be checked in diagonal cases.
    private static final Vector<BlockFace> horizontalFaces = new Vector<BlockFace>(Arrays.asList(BlockFace.UP, BlockFace.DOWN));
    // HashMap of faces that will be used in each direction traversed to prevent backtracking.
    private static final HashMap<BlockFace, Vector<BlockFace>> blockFaceMap = new HashMap<BlockFace, Vector<BlockFace>>(6);

    // Initialize blockFaceMap and downFaces.
    static {
        Vector<BlockFace> v;
        for (BlockFace face : blockFaces) {
            v = new Vector<BlockFace>(blockFaces);
            v.remove(face.getOppositeFace());
            blockFaceMap.put(face, v);
        }
    }

    private static Modes mode;
    private static final Vector<Material> logList = new Vector<Material>();
    private static final Vector<Material> leafList = new Vector<Material>();
    private static final Vector<Material> toolList = new Vector<Material>();

    @Override
    public void onEnable() {
        getConfig().options().copyDefaults(true);
        saveConfig();
        logger.info("Configuration file loaded.");
        getServer().getPluginManager().registerEvents(this, this);
    }

    @EventHandler
    public void blockBreakHandler(BlockBreakEvent event) {
        ItemStack tool = event.getPlayer().getInventory().getItemInMainHand();
        Material toolMaterial = tool.getType();

        reloadConfig();
        FileConfiguration config = getConfig();
        mode = Modes.valueOf(config.getString("mode"));

        if (DEBUG) {
            logger.info("Mode: " + mode);
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

    private void breakBlocks(ItemStack tool, BlockQueueElement blockQueueElement) {
        int x = 0;
        Block block, cardinalBlock, diagonalBlock;
        LinkedList<BlockQueueElement> blockQueue = new LinkedList<BlockQueueElement>(Arrays.asList(blockQueueElement));

        blockQueueElement.getBlock().breakNaturally(tool);
        while (! blockQueue.isEmpty()) {
            if (DEBUG && blockQueue.size() > x) {
                x = blockQueue.size();
            }
            blockQueueElement = blockQueue.remove();
            block = blockQueueElement.getBlock();
            for (BlockFace i : blockQueueElement.getBlockFaces()) {
                if (i != BlockFace.UP && (mode == Modes.CLASSIC || mode == Modes.CLASSIC_LEAVES)) {
                    continue;
                }
                cardinalBlock = block.getRelative(i);
                if (isValidBlock(cardinalBlock)) {
                    cardinalBlock.breakNaturally(tool);
                    blockQueue.add(new BlockQueueElement(cardinalBlock, blockFaceMap.get(i)));
                }
                if (! horizontalFaces.contains(i)) {
                    for (BlockFace j : horizontalFaces) {
                        diagonalBlock = cardinalBlock.getRelative(j);
                        if (isValidBlock(diagonalBlock)) {
                            diagonalBlock.breakNaturally(tool);
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
