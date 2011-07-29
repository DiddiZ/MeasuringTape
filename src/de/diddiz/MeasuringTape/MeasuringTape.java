package de.diddiz.MeasuringTape;

import static de.diddiz.utils.BukkitUtils.giveTool;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.Event.Priority;
import org.bukkit.event.Event.Type;
import org.bukkit.event.block.Action;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.config.Configuration;
import com.nijiko.permissions.PermissionHandler;
import com.nijikokun.bukkit.Permissions.Permissions;

public class MeasuringTape extends JavaPlugin
{
	private final HashMap<Integer, Session> sessions = new HashMap<Integer, Session>();
	private int blocksPerString;
	boolean useTargetBlock;
	private boolean defaultEnabled;
	private Set<Integer> groundBlocks;
	private PermissionHandler permissions = null;
	private Logger log;

	@Override
	public void onEnable() {
		log = getServer().getLogger();
		final PluginManager pm = getServer().getPluginManager();
		try {
			final Configuration config = getConfiguration();
			config.load();
			final List<String> keys = config.getKeys(null);
			if (!keys.contains("blocksPerString"))
				config.setProperty("blocksPerString", -1);
			if (!keys.contains("useTargetBlock"))
				config.setProperty("useTargetBlock", true);
			if (!keys.contains("defaultEnabled"))
				config.setProperty("defaultEnabled", true);
			if (!keys.contains("groundBlocks"))
				config.setProperty("groundBlocks", Arrays.asList(new Integer[]{1, 2, 3, 4, 12, 13}));
			if (!config.save())
				throw new IOException("Error while writing to config.yml");
			blocksPerString = config.getInt("blocksPerString", -1);
			defaultEnabled = config.getBoolean("defaultEnabled", true);
			useTargetBlock = config.getBoolean("useTargetBlock", true);
			groundBlocks = new HashSet<Integer>(config.getIntList("groundBlocks", null));
		} catch (final Exception ex) {
			log.log(Level.SEVERE, "[MeasuringTape] Could not load config", ex);
			pm.disablePlugin(this);
			return;
		}
		if (pm.getPlugin("Permissions") != null)
			permissions = ((Permissions)pm.getPlugin("Permissions")).getHandler();
		else
			log.info("[MeasuringTape] Permissions plugin not found. Using default permissions.");
		pm.registerEvent(Type.PLAYER_INTERACT, new MTPlayerListener(this), Priority.Normal, this);
		log.info("MeasuringTape v" + getDescription().getVersion() + " by DiddiZ enabled");
	}

	@Override
	public void onDisable() {
		log.info("MeasuringTape disabled");
	}

	@Override
	public boolean onCommand(CommandSender sender, Command cmd, String commandLabel, String[] args) {
		if (cmd.getName().equalsIgnoreCase("mt")) {
			if (sender instanceof Player) {
				final Player player = (Player)sender;
				final Session session = getSession(player);
				if (args.length == 0) {
					player.sendMessage(ChatColor.LIGHT_PURPLE + "MeasuringTape v" + getDescription().getVersion() + " by DiddiZ");
					player.sendMessage(ChatColor.LIGHT_PURPLE + "Type /mt help for help");
				} else if (args[0].equalsIgnoreCase("tape")) {
					if (hasPermission(player, "measuringtape.tape"))
						giveTool(player, 287);
					else
						player.sendMessage(ChatColor.RED + "You aren't allowed to do this.");
				} else if (args[0].equalsIgnoreCase("read"))
					if (session.isPos1Set() && session.isPos2Set())
						showDistance(player, session);
					else
						player.sendMessage(ChatColor.RED + "Both positions must be set");
				else if (args[0].equalsIgnoreCase("unset")) {
					session.resetPos();
					player.sendMessage(ChatColor.GREEN + "Measuring tape rolled up.");
				} else if (args[0].equalsIgnoreCase("mode")) {
					if (args.length != 2)
						player.sendMessage(ChatColor.RED + "Usage: /mt mode [mode]");
					else
						try {
							session.mode = MeasuringMode.valueOf(args[1].toUpperCase());
							player.sendMessage(ChatColor.GREEN + "Measuring mode set to " + args[1]);
						} catch (final IllegalArgumentException ex) {
							player.sendMessage(ChatColor.RED + "Wrong argument. Type /mt help for help.");
						}
				} else if (args[0].equalsIgnoreCase("tp")) {
					if (hasPermission(player, "measuringtape.tp")) {
						if (session.mode == MeasuringMode.AREA && session.isPos1Set() && session.isPos2Set()) {
							final Location diff = getDiff(session.pos.get(0), session.pos.get(1));
							if (diff.getBlockX() % 2 == 0 && diff.getBlockZ() % 2 == 0) {
								final int x = session.pos.get(0).getBlockX() + diff.getBlockX() / 2;
								final int z = session.pos.get(0).getBlockZ() + diff.getBlockZ() / 2;
								player.teleport(new Location(player.getWorld(), x + 0.5, player.getWorld().getHighestBlockYAt(x, z), z + 0.5, player.getLocation().getYaw(), player.getLocation().getPitch()));
								player.sendMessage(ChatColor.GREEN + "Teleported to center.");
							} else
								player.sendMessage(ChatColor.RED + "Area has not a single block as center.");
						} else
							player.sendMessage(ChatColor.RED + "Both positions must be set and must be in area mode.");
					} else
						player.sendMessage(ChatColor.RED + "You aren't allowed to do this.");

				} else if (args[0].equalsIgnoreCase("level")) {
					if (session.mode == MeasuringMode.AREA && session.isPos1Set() && session.isPos2Set()) {
						int level = 0;
						final int lowerX = Math.min(session.pos.get(0).getBlockX(), session.pos.get(1).getBlockX());
						final int upperX = Math.max(session.pos.get(0).getBlockX(), session.pos.get(1).getBlockX());
						final int lowerZ = Math.min(session.pos.get(0).getBlockZ(), session.pos.get(1).getBlockZ());
						final int upperZ = Math.max(session.pos.get(0).getBlockZ(), session.pos.get(1).getBlockZ());
						final World world = player.getWorld();
						for (int x = lowerX; x <= upperX; x++)
							for (int z = lowerZ; z <= upperZ; z++)
								for (int y = 127; y >= 0; y--)
									if (groundBlocks.contains(world.getBlockTypeIdAt(x, y, z))) {
										level += y;
										break;
									}
						level = (int)((double)level / ((Math.abs(lowerX - upperX) + 1) * (Math.abs(lowerZ - upperZ) + 1)) + 0.5);
						player.sendMessage("Optimal ground level is at " + level);
					} else
						player.sendMessage(ChatColor.RED + "Both positions must be set and must be in area mode.");
				} else if (args[0].equalsIgnoreCase("help")) {
					player.sendMessage(ChatColor.LIGHT_PURPLE + "MeasuringTape Commands:");
					if (hasPermission(player, "measuringtape.tape"))
						player.sendMessage(ChatColor.LIGHT_PURPLE + "/mt tape //Gives a measuring tape");
					player.sendMessage(ChatColor.LIGHT_PURPLE + "/mt read //Displays the distance again");
					player.sendMessage(ChatColor.LIGHT_PURPLE + "/mt unset //Unsets both markers");
					player.sendMessage(ChatColor.LIGHT_PURPLE + "/mt mode [mode] //Toggles measuring mode");
					player.sendMessage(ChatColor.LIGHT_PURPLE + "/mt modehelp //Displays help to the modes");
					if (hasPermission(player, "measuringtape.tp"))
						player.sendMessage(ChatColor.LIGHT_PURPLE + "/mt tp //Teleports to the center of the selected area");
					player.sendMessage(ChatColor.LIGHT_PURPLE + "/mt level //Calculates the average height of an area");
					if (session.MTEnabled)
						player.sendMessage(ChatColor.LIGHT_PURPLE + "/mt disable //Disables string attaching");
					else
						player.sendMessage(ChatColor.LIGHT_PURPLE + "/mt enable //Enables string attaching");
				} else if (args[0].equalsIgnoreCase("modehelp")) {
					player.sendMessage(ChatColor.LIGHT_PURPLE + "MeasuringTape Modes:");
					player.sendMessage(ChatColor.LIGHT_PURPLE + "distance - direct distance between both positions");
					player.sendMessage(ChatColor.LIGHT_PURPLE + "vectors -xyz-vectors between the positions");
					player.sendMessage(ChatColor.LIGHT_PURPLE + "area - area between the points");
					player.sendMessage(ChatColor.LIGHT_PURPLE + "blocks - amount of blocks in x, y and z axis between positions");
					player.sendMessage(ChatColor.LIGHT_PURPLE + "track - distance with multiple points");
					player.sendMessage(ChatColor.LIGHT_PURPLE + "volume - volume of a cuboid");
				} else if (args[0].equalsIgnoreCase("enable")) {
					session.MTEnabled = true;
					player.sendMessage(ChatColor.LIGHT_PURPLE + "Measuring tape enabled.");
				} else if (args[0].equalsIgnoreCase("disable")) {
					session.MTEnabled = false;
					player.sendMessage(ChatColor.LIGHT_PURPLE + "Measuring tape disabled.");
				} else
					player.sendMessage(ChatColor.RED + "Wrong argument. Type /mt help for help.");
			} else
				sender.sendMessage("You aren't a player.");
			return true;
		}
		return false;
	}

	boolean hasPermission(Player player, String permission) {
		if (permissions != null)
			return permissions.permission(player, permission);
		if (permission.equals("measuringtape.tp"))
			return player.isOp();
		return true;
	}

	void attach(Player player, Block block, Action action) {
		final Session session = getSession(player);
		if (session.MTEnabled) {
			final Location loc = new Location(block.getWorld(), block.getX(), block.getY(), block.getZ());
			if (session.mode == MeasuringMode.DISTANCE || session.mode == MeasuringMode.VECTORS || session.mode == MeasuringMode.AREA || session.mode == MeasuringMode.BLOCKS || session.mode == MeasuringMode.VOLUME) {
				if (action == Action.LEFT_CLICK_AIR || action == Action.LEFT_CLICK_BLOCK) {
					if (!session.isPos1Set())
						player.sendMessage(ChatColor.GREEN + "Measuring Tape attached to first position");
					session.pos.set(0, loc);
				} else {
					if (!session.isPos2Set())
						player.sendMessage(ChatColor.GREEN + "Measuring Tape attached to second position");
					session.pos.set(1, loc);
				}
			} else if (session.mode == MeasuringMode.TRACK)
				if (!session.isPos1Set()) {
					session.pos.set(0, loc);
					player.sendMessage(ChatColor.GREEN + "Measuring Tape attached to first position");
				} else if (!session.isPos2Set()) {
					session.pos.set(1, loc);
					player.sendMessage(ChatColor.GREEN + "Measuring Tape attached to second position");
				} else
					session.pos.add(loc);
			if (session.isPos1Set() && session.isPos2Set())
				showDistance(player, session);
		}
	}

	private void showDistance(Player player, Session session) {
		Location diff = getDiff(session.pos.get(0), session.pos.get(1));
		int x = Math.abs(diff.getBlockX()), y = Math.abs(diff.getBlockY()), z = Math.abs(diff.getBlockZ());
		double distance = 0;
		int stringsNeeded = 0;
		String msg = "";
		switch (session.mode) {
			case DISTANCE:
				distance = Math.round(Math.sqrt(x * x + y * y + z * z) * 10) / 10d;
				stringsNeeded = (int)Math.ceil(distance / blocksPerString);
				msg = "Distance: " + distance + "m";
				break;
			case VECTORS:
				stringsNeeded = (int)Math.ceil((x + y + z) / blocksPerString);
				msg = "Vectors: X" + x + " Y" + z + " Z" + y;
				break;
			case AREA:
				x++;
				z++;
				stringsNeeded = (int)Math.ceil((x + z - 1) / blocksPerString);
				msg = "Area: " + x + "x" + z + " (" + x * z + " m2)";
				break;
			case BLOCKS:
				x += y + z + 1;
				stringsNeeded = (int)Math.ceil(x / blocksPerString);
				msg = "Blocks: " + x;
				break;
			case TRACK:
				for (int i = 1; i < session.pos.size(); i++) {
					diff = getDiff(session.pos.get(i - 1), session.pos.get(i));
					distance += Math.sqrt(diff.getBlockX() * diff.getBlockX() + diff.getBlockY() * diff.getBlockY() + diff.getBlockZ() * diff.getBlockZ());
				}
				distance = Math.round(distance * 10) / 10d;
				stringsNeeded = (int)Math.ceil(distance / blocksPerString);
				msg = "Track: " + distance + "m";
				break;
			case VOLUME:
				x++;
				y++;
				z++;
				stringsNeeded = (int)Math.ceil((x + y + z - 2) / blocksPerString);
				msg = "Volume: " + x + "x" + y + "x" + z + " (" + x * y * z + " m3)";
				break;
		}
		if (blocksPerString != -1) {
			int stringsAvailable = 0;
			for (final ItemStack item : player.getInventory().getContents())
				if (item != null && item.getTypeId() == 287)
					stringsAvailable += item.getAmount();
			if (stringsNeeded > stringsAvailable) {
				player.sendMessage(ChatColor.RED + "You have not enought tape. You need " + (stringsNeeded - stringsAvailable) + " more");
				return;
			}
		}
		player.sendMessage(msg);
	}

	private Session getSession(Player player) {
		Session session = sessions.get(player.getName().hashCode());
		if (session == null) {
			session = new Session(defaultEnabled);
			sessions.put(player.getName().hashCode(), session);
		}
		return session;
	}

	private static Location getDiff(Location loc1, Location loc2) {
		return new Location(loc1.getWorld(), loc2.getBlockX() - loc1.getBlockX(), loc2.getBlockY() - loc1.getBlockY(), loc2.getBlockZ() - loc1.getBlockZ());
	}
}
