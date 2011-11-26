package de.diddiz.MeasuringTape;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.plugin.Plugin;

public class Config
{
	public static int blocksPerString;
	public static boolean useTargetBlock;
	public static boolean defaultEnabled;
	public static Set<Integer> groundBlocks;

	static void load(Plugin plugin) {
		final ConfigurationSection cfg = plugin.getConfig();
		final Map<String, Object> def = new HashMap<String, Object>();
		def.put("blocksPerString", -1);
		def.put("useTargetBlock", true);
		def.put("defaultEnabled", true);
		def.put("groundBlocks", new Integer[]{1, 2, 3, 4, 12, 13, 60, 79, 82, 87});
		for (final Entry<String, Object> e : def.entrySet())
			if (!cfg.contains(e.getKey()))
				cfg.set(e.getKey(), e.getValue());
		plugin.saveConfig();
		blocksPerString = cfg.getInt("blocksPerString", -1);
		defaultEnabled = cfg.getBoolean("defaultEnabled", true);
		useTargetBlock = cfg.getBoolean("useTargetBlock", true);
		groundBlocks = new HashSet<Integer>(cfg.getIntegerList("groundBlocks"));
	}
}
