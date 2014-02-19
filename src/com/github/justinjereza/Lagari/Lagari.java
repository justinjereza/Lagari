package com.github.justinjereza.Lagari;

import java.util.Arrays;
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
import org.bukkit.plugin.java.JavaPlugin;

public class Lagari extends JavaPlugin implements Listener {
    private static enum Modes { CLASSIC, CLASSIC_LEAVES, FULL, FULL_NOLEAVES };
    private static final Vector<BlockFace> blockFaces = new Vector<BlockFace>(Arrays.
            asList(BlockFace.NORTH, BlockFace.SOUTH, BlockFace.EAST, BlockFace.WEST, BlockFace.UP, BlockFace.DOWN,
                    BlockFace.NORTH_EAST, BlockFace.NORTH_WEST, BlockFace.SOUTH_EAST, BlockFace.SOUTH_WEST));
    private final Logger logger = getLogger();

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
        if ((mode == Modes.CLASSIC || mode == Modes.CLASSIC_LEAVES) && blockFaces.contains(BlockFace.DOWN)) {
            blockFaces.remove(BlockFace.DOWN);
        } else if ((mode == Modes.FULL || mode == Modes.FULL_NOLEAVES) && ! blockFaces.contains(BlockFace.DOWN)) {
            blockFaces.add(BlockFace.DOWN);
        }
        if (config.contains("leaf-log-distance")) {
            leafLogDistance = config.getInt("leaf-log-distance");
        }

//        logger.info("Mode: " + mode);
//        logger.info("Leaf-Log distance: " + leafLogDistance);
//        logger.info("Tool material: " + toolMaterial);

        Material m;
        for (String s : config.getStringList("logs")) {
            m = Material.valueOf(s);
            if (! logList.contains(m)) {
                logList.add(m);
//                logger.info("Added log material: " + m);
            }
        }
        for (String s : config.getStringList("leaves")) {
            m = Material.valueOf(s);
            if (! leafList.contains(m)) {
                leafList.add(m);
//                logger.info("Added leaf material: " + m);
            }
        }
        for (String s : config.getStringList("tools")) {
            m = Material.valueOf(s);
            if (! toolList.contains(m)) {
                toolList.add(m);
//                logger.info("Added tool material: " + m);
            }
        }

        Block block = event.getBlock();
        if (logList.contains(block.getType()) && toolList.contains(toolMaterial)) {
            breakBlocks(block);
        }
    }

    private boolean isInLogRange(Block block) {
        boolean r = false;

        for (int i = 1; i <= leafLogDistance; i++) {
//            logger.info("LOG SCAN RANGE: " + i);
            for (BlockFace face : BlockFace.values()) {
                r = logList.contains(block.getRelative(face, i).getType());
                if (r) {
//                    logger.info(block.getType() + " IN LOG RANGE: " + i);
                    return r;
                }
            }
        }
        return r;
    }

    private Vector<Block> getUpDownBlocks(Block block) {
        Vector<Block> r = new Vector<Block>(2);

        Block b;
        Material m;
        for (BlockFace face : Arrays.asList(BlockFace.UP, BlockFace.DOWN)) {
            b = block.getRelative(face);
            m = b.getType();

            switch (mode) {
            case CLASSIC:
                if (logList.contains(m)) {
                    r.add(b);
                }
                break;
            case CLASSIC_LEAVES:
                break;
            case FULL_NOLEAVES:
                break;
            case FULL:
                if (logList.contains(m) || leafList.contains(m)) {
                    r.add(b);
                }
                break;
            }
        }
        return r;
    }

    private LinkedList<Block> getNeighborBlocks(Block block) {
        LinkedList<Block> r = new LinkedList<Block>();

        Block b;
        Material m;
//        logger.info("Block at " + block.getLocation());
//        logger.info("Block type: " + block.getType());
//        logger.info("-----");
        for (BlockFace face : blockFaces) {
            b = block.getRelative(face);
            m = b.getType();

//            logger.info("Checking face " + face);
            switch (mode) {
            case CLASSIC:
                if (logList.contains(m)) {
                    r.add(b);
                }
                break;
            case CLASSIC_LEAVES:
                if (logList.contains(m)) {
                    r.add(b);
                } else if (leafList.contains(m)) {
                    if (isInLogRange(b)) {
                        r.add(b);
                    }
//                    logger.info("CLASSIC_LEAVES Added neighbor: " + face + " Type: " + m);
//                } else {
//                    logger.info("CLASSIC_LEAVES " + face + " neighbor type: " + m);
                }
                break;
            case FULL_NOLEAVES:
                if (logList.contains(m)) {
                    r.add(b);
                }
                break;
            case FULL:
                if (logList.contains(m) || leafList.contains(m)) {
                    r.add(b);
                }
                if (face != BlockFace.UP && face != BlockFace.DOWN) {
                    r.addAll(getUpDownBlocks(b));
                }
            }
        }
//        logger.info("-----");
        return r;
    }

    private void breakBlocks(Block block) {
        Material m = block.getRelative(BlockFace.UP).getType();
        if (! logList.contains(m)) {
//            logger.info("First block UP material: " + m);
            return;
        }

        LinkedList<Block> blockList = new LinkedList<Block>(Arrays.asList(block));
        while (! blockList.isEmpty()) {
//			logger.info("Block list size: " + blockList.size());
            Block b = blockList.removeFirst();
            blockList.addAll(getNeighborBlocks(b));
            if (! b.isEmpty()) {
                b.breakNaturally();
            }
        }
    }
}
