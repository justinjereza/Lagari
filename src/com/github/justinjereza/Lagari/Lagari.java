package com.github.justinjereza.Lagari;

import java.util.Arrays;
import java.util.Vector;
import java.util.LinkedList;
import java.util.HashMap;
import java.util.logging.Logger;

import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.event.Listener;
import org.bukkit.event.EventHandler;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.configuration.file.FileConfiguration;

public class Lagari extends JavaPlugin implements Listener {
	private static enum Modes { CLASSIC, CLASSIC_LEAVES, FULL, FULL_NOLEAVES };
	private static final Vector<BlockFace> blockFaces = new Vector<BlockFace>(Arrays.
			asList(BlockFace.NORTH, BlockFace.SOUTH, BlockFace.EAST, BlockFace.WEST, BlockFace.UP, BlockFace.DOWN));

	private static final HashMap<BlockFace, String> blockFaceMap = new HashMap<BlockFace, String>();
	private static final HashMap<Material, String> materialMap = new HashMap<Material, String>();
	static {
		for (BlockFace m : BlockFace.values()) {
			blockFaceMap.put(m, m.name());
		}
		for (Material m : Material.values()) {
			materialMap.put(m, m.name()); 
		}
	}
	
	private final Logger logger = getLogger();

	private static Modes mode;
	private static final Vector<Material> logList = new Vector<Material>();
	private static final Vector<Material> leafList = new Vector<Material>();
	private static final Vector<Material> toolList = new Vector<Material>();

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
		} else if (! blockFaces.contains(BlockFace.DOWN)) {
			blockFaces.add(BlockFace.DOWN);
		}
		logger.info("Mode: " + mode);
		logger.info("Tool material: " + toolMaterial);
		
		Material m;
		for (String s : config.getStringList("logs")) {
			m = Material.valueOf(s);
			if (! logList.contains(m)) {
				logList.add(m);					
				logger.info("Added log material: " + m);
			}
		}
		for (String s : config.getStringList("leaves")) {
			m = Material.valueOf(s);
			if (! leafList.contains(m)) {					
				leafList.add(m);
				logger.info("Added leaf material: " + m);
			}
		}
		for (String s : config.getStringList("tools")) {
			m = Material.valueOf(s);
			if (! toolList.contains(m)) {
				toolList.add(m);					
				logger.info("Added tool material: " + m);
			}
		}

		Block block = event.getBlock();
		if (logList.contains(block.getType()) && toolList.contains(toolMaterial)) {
			breakBlocks(block);
		}
	}
	
	public LinkedList<Block> getNeighborBlocks(Block block) {
		LinkedList<Block> r = new LinkedList<Block>();
		
		Block b;
		Material m;
		logger.info("Block at " + block.getLocation());
		logger.info("-----");
		for (BlockFace face : blockFaces) {
			b = block.getRelative(face);
			m = b.getType();
			if (logList.contains(m)) {
				r.add(b);
			} else if ((mode == Modes.CLASSIC_LEAVES || mode == Modes.FULL) && leafList.contains(m)) {
				r.add(b);
			}
			logger.info(face + " neighbor type: " + m);
		}
		logger.info("-----");
		return r;
	}
	
	public void breakBlocks(Block block) {
		block.breakNaturally();
		LinkedList<Block> neighborBlocks = getNeighborBlocks(block);
		
		while (! neighborBlocks.isEmpty()) {
//			logger.info("Neighbor list length: " + neighborBlocks.size());
			Block b = neighborBlocks.removeFirst();
			if (! b.isEmpty()) {
				b.breakNaturally();
				neighborBlocks.addAll(getNeighborBlocks(b));
			}
		}
	}
}
