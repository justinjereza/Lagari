package com.github.justinjereza.Lagari;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Vector;
import java.util.logging.Logger;

import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.plugin.java.JavaPlugin;

public class Lagari extends JavaPlugin implements Listener {
    private final Logger logger = getLogger();
    private static enum Modes { CLASSIC, CLASSIC_LEAVES, FULL, FULL_NOLEAVES };

    // Vector of faces that will be checked.
    private static final Vector<BlockFace> blockFaces = new Vector<BlockFace>(Arrays.
            asList(BlockFace.NORTH, BlockFace.SOUTH, BlockFace.EAST, BlockFace.WEST, BlockFace.UP, BlockFace.DOWN));
    // Vector of faces for that will be checked for BlockFace.DOWN. We store a reference copy here for blockFaceMap mode changes.
    private static final Vector<BlockFace> downFaces = new Vector<BlockFace>(5);
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
            if (face == BlockFace.DOWN) {
                downFaces.addAll(v);
                blockFaceMap.put(face, downFaces);
            } else {
                blockFaceMap.put(face, v);
            }
        }
    }

    private static Modes mode;
    private static int leafLogDistance;
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
        Material toolMaterial = event.getPlayer().getItemInHand().getType();

        reloadConfig();
        FileConfiguration config = getConfig();
        mode = Modes.valueOf(config.getString("mode"));

        if (config.contains("leaf-log-distance") && config.isInt("leaf-log-distance")) {
            leafLogDistance = config.getInt("leaf-log-distance");
        }

        logger.info("Mode: " + mode);
        logger.info("Leaf-Log distance: " + leafLogDistance);
        logger.info("Tool material: " + toolMaterial);

        if (mode == Modes.CLASSIC || mode == Modes.CLASSIC_LEAVES) {
            if (blockFaces.contains(BlockFace.DOWN)) {
                blockFaces.remove(BlockFace.DOWN);
            }
            if (horizontalFaces.contains(BlockFace.DOWN)) {
                horizontalFaces.remove(BlockFace.DOWN);
            }
            if (blockFaceMap.containsKey(BlockFace.DOWN)) {
                blockFaceMap.remove(BlockFace.DOWN);
            }
        } else {
            if (! blockFaces.contains(BlockFace.DOWN)) {
                blockFaces.add(BlockFace.DOWN);
            }
            if (! horizontalFaces.contains(BlockFace.DOWN)) {
                horizontalFaces.add(BlockFace.DOWN);
            }
            if (! blockFaceMap.containsKey(BlockFace.DOWN)) {
                blockFaceMap.put(BlockFace.DOWN, downFaces);
            }
        }

        Material m;
        for (String s : config.getStringList("logs")) {
            m = Material.valueOf(s);
            if (! logList.contains(m)) {
                logList.add(m);
            }
        }
        for (String s : config.getStringList("leaves")) {
            m = Material.valueOf(s);
            if (! leafList.contains(m)) {
                leafList.add(m);
            }
        }
        for (String s : config.getStringList("tools")) {
            m = Material.valueOf(s);
            if (! toolList.contains(m)) {
                toolList.add(m);
            }
        }

        Block block = event.getBlock();
        if (toolList.contains(toolMaterial) &&
                logList.contains(block.getType()) &&
                logList.contains(block.getRelative(BlockFace.UP).getType())) {
            breakBlock(block, blockFaceMap.get(BlockFace.UP));
        }
    }

    private boolean isValidBlock(Block b) {
        Material m = b.getType();
        return logList.contains(m) || (mode == Modes.CLASSIC_LEAVES || mode == Modes.FULL) && leafList.contains(m);
    }

    private void breakBlock(Block block, Vector<BlockFace> blockFaces) {
        Block b;

        block.breakNaturally();
        for (BlockFace i : blockFaces) {
            b = block.getRelative(i);
            if (isValidBlock(b)) {
                breakBlock(b, blockFaceMap.get(i));
            }
            if (! horizontalFaces.contains(i)) {
                for (BlockFace j : horizontalFaces) {
                    b = b.getRelative(j);
                    if (isValidBlock(b)) {
                        breakBlock(b, blockFaceMap.get(j));
                    }
                }
            }
        }
    }
}
