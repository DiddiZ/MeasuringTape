//Author: DiddiZ
//Date: 2011-02-04

package de.diddiz.MeasuringTape;

import java.io.File;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.Date;
import java.util.logging.Level;

import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.block.BlockDamageLevel;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.block.BlockDamageEvent;
import org.bukkit.event.block.BlockListener;
import org.bukkit.event.block.BlockRightClickEvent;
import org.bukkit.event.player.PlayerChatEvent;
import org.bukkit.event.player.PlayerListener;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

import com.nijikokun.bukkit.Permissions.Permissions;

public class MeasuringTape extends JavaPlugin
{
    private ArrayList<Session> sessions = new ArrayList<Session>();
    private int tapeDelay;
    private int blocksPerString;
    private boolean defaultEnabled;
    private boolean usePermissions = false;
    
	@Override
	public void onEnable() {
		try	{
			File file = new File (getDataFolder(), "config.yml");
			if (!file.exists())	{
				file.getParentFile().mkdirs();
				FileWriter writer = new FileWriter(file);
				String crlf = System.getProperty("line.separator");
				writer.write("tapeDelay : 15" + crlf
						+ "blocksPerString : -1" + crlf
						+ "defaultEnabled : true" + crlf
						+ "usePermissions : false");
				writer.close();
				getServer().getLogger().info("[MeasuringTape] Config created");
			}
			getConfiguration().load();
			tapeDelay = getConfiguration().getInt("tapeDelay", 15);
			blocksPerString = getConfiguration().getInt("blocksPerString", -1);
			defaultEnabled = getConfiguration().getBoolean("defaultEnabled", true);
			if (getConfiguration().getBoolean("usePermissions", false))	{
				if (getServer().getPluginManager().getPlugin("Permissions") != null)
					usePermissions = true;
				else
					getServer().getLogger().info("[MeasuringTape] Permissions plugin not found. Use default permissions.");
			}
        } catch (Exception e) {
			getServer().getLogger().log(Level.SEVERE, "[MeasuringTape] Exception while reading config.yml", e);
        	getServer().getPluginManager().disablePlugin(this);
		}
	    PluginManager pm = getServer().getPluginManager();
	    MeasuringTapeBlockListener measuringTapeBlockListener = new MeasuringTapeBlockListener();
	    pm.registerEvent(Event.Type.PLAYER_COMMAND, new MeasuringTapePlayerListener(), Event.Priority.Normal, this);
		pm.registerEvent(Event.Type.BLOCK_DAMAGED, measuringTapeBlockListener, Event.Priority.Monitor, this);
		pm.registerEvent(Event.Type.BLOCK_RIGHTCLICKED, measuringTapeBlockListener, Event.Priority.Monitor, this);
		getServer().getLogger().info("MeasuringTape v" + this.getDescription().getVersion() + " by DiddiZ enabled");
	}
    
	@Override
	public void onDisable()	{
		getServer().getLogger().info("MeasuringTape Disabled");
	}
    
	public enum MeasuringMode {
		DISTANCE, VECTORS, AREA, BLOCKS, TRACK, VOLUME;
	}
	
	public enum MouseButton	{
		LEFT, RIGHT;
	}
	
	private class Session
    {
    	public String user;
    	public Boolean MTEnabled;
    	public ArrayList<Location> pos;
        public Boolean pos1Set;
        public Boolean pos2Set;
        public MeasuringMode mode;
        public Date lastTape;
        
        public Session (Player player) {
        	user = player.getName();
        	lastTape = new Date(0);
        	mode = MeasuringMode.DISTANCE;
        	MTEnabled = defaultEnabled;
        	ResetPos();
        }
        
        public void ResetPos() {
        	pos = new ArrayList<Location>();
        	this.pos.add(null);
        	this.pos.add(null);
			this.pos1Set = false;
			this.pos2Set = false;
        }
        
		@Override
		public boolean equals(Object obj) {
			if (obj == null)
				return false;
			if (!user.equalsIgnoreCase(((Session)obj).user))
				return false;
			return true;
		}
    }
    
	private class MeasuringTapePlayerListener extends PlayerListener
	{ 
		public void onPlayerCommand(PlayerChatEvent event) {
			String[] split = event.getMessage().split(" ");
			if (split[0].equalsIgnoreCase("/mt")) {
				event.setCancelled(true);
				Player player = event.getPlayer();
				Session session = GetSession(player);
				if (split.length == 1)
					player.sendMessage("§cNo argument. Type /mt help for help");
				else if (split[1].equalsIgnoreCase("tape") && CheckPermission(event.getPlayer(), "measuringtape.tape"))	{
					if (player.getInventory().contains(287)) {
						player.sendMessage("§cYou have alredy a string"); 
						player.sendMessage("§dLeft click: select pos #1; Right click select pos #2"); 
					} else {
						long mins = (new Date().getTime() - session.lastTape.getTime()) / 60000;
						if (mins >= tapeDelay) {
							int free = player.getInventory().firstEmpty();
							if (free >= 0) {
								player.getInventory().setItem(free, player.getItemInHand());
								player.setItemInHand(new ItemStack(287, 1));
								session.lastTape = new Date();
								player.sendMessage("§aHere is your measuring tape"); 
								player.sendMessage("§dLeft click: select pos #1; Right click select pos #2"); 
							} else
								player.sendMessage("§cYou have no empty slot in your inventory"); 
						} else {
							player.sendMessage("§cYot got your last tape " + mins + "min ago.");
							player.sendMessage("§cYou have to wait " + (tapeDelay - mins) + " minutes");
						}
					}
				} else if (split[1].equalsIgnoreCase("read"))
					ShowDistance(session);
				else if (split[1].equalsIgnoreCase("unset")) {
					session.ResetPos();
					player.sendMessage("§aMeasuring tape rolled up");
				} else if (split[1].equalsIgnoreCase("mode")) {
					if (split.length != 3)
						player.sendMessage("§cCorrect usage: /mt mode [mode]");
					else if (split[2].equalsIgnoreCase("distance")) {
						session.mode = MeasuringMode.DISTANCE;
						player.sendMessage("§aMeasuring mode set to distance");
					} else if (split[2].equalsIgnoreCase("vectors")) {
						session.mode = MeasuringMode.VECTORS;
						player.sendMessage("§aMeasuring mode set to vectors");
					} else if (split[2].equalsIgnoreCase("area")) {
						session.mode = MeasuringMode.AREA;
						player.sendMessage("§aMeasuring mode set to area");
					} else if (split[2].equalsIgnoreCase("blocks"))	{
						session.mode = MeasuringMode.BLOCKS;
						player.sendMessage("§aMeasuring mode set to blocks");
					} else if (split[2].equalsIgnoreCase("track")) {
						session.mode = MeasuringMode.TRACK;
						session.ResetPos();
						player.sendMessage("§aMeasuring mode set to track");
					} else if (split[2].equalsIgnoreCase("volume"))	{
						session.mode = MeasuringMode.VOLUME;
						player.sendMessage("§aMeasuring mode set to volume");
					} else
						player.sendMessage("§cWrong argument. Type /mt for help");
				} else if (split[1].equalsIgnoreCase("tp") && CheckPermission(event.getPlayer(), "measuringtape.tp")) {
					if (session.mode == MeasuringMode.AREA && session.pos1Set && session.pos1Set) {
						Location diff = GetDiff(session.pos.get(0),session.pos.get(1));
						if ((diff.getBlockX()) % 2 == 0 && (diff.getBlockZ()) % 2 == 0)	{
							double x = session.pos.get(0).getBlockX() + diff.getBlockX() / 2 + 0.5;
							double z = session.pos.get(0).getBlockZ() + (diff.getBlockZ()) / 2 + 0.5;
							player.teleportTo(new Location(player.getWorld(), x , player.getWorld().getHighestBlockYAt((int)x, (int)z), z, player.getLocation().getYaw(), player.getLocation().getPitch()));
							player.sendMessage("§aTeleported to center");
						} else 
							player.sendMessage("§cArea has not a single block as center");
					} else 
						player.sendMessage("§cBoth positions must be set and must be in area mode");
				} else if (split[1].equalsIgnoreCase("help")) {
					player.sendMessage("§dMeasuringTape Commands:");
					if (CheckPermission(event.getPlayer(), "measuringtape.tape"))
						player.sendMessage("§d/mt tape //Gives a measuring tape");
					player.sendMessage("§d/mt read //Displays the distance again");
					player.sendMessage("§d/mt unset //Unsets both markers");
					player.sendMessage("§d/mt mode [mode] //Toggles measuring mode");
					player.sendMessage("§d/mt modehelp //Displays help to the modes");
					if (CheckPermission(event.getPlayer(), "measuringtape.tp"))
						player.sendMessage("§d/mt tp //Teleports to the center of the selected area");
					if (session.MTEnabled)
						player.sendMessage("§d/mt disable //Disables string attaching");
					else
						player.sendMessage("§d/mt enable //Enables string attaching");
				} else if (split[1].equalsIgnoreCase("modehelp")) {
					player.sendMessage("§dMeasuringTape Modes:");
					player.sendMessage("§ddistance - direct distance between both positions");
					player.sendMessage("§dvectors -xyz-vectors between the positions");
					player.sendMessage("§darea - area between the points");
					player.sendMessage("§dblocks - amount of blocks in x, y and z axis between positions");
					player.sendMessage("§dtrack - distance with multiple points");
					player.sendMessage("§dvolume - volume of a cuboid");
				} else if (split[1].equalsIgnoreCase("enable"))	{
					session.MTEnabled = true;
					player.sendMessage("§dMeasuring tape enabled");
				} else if (split[1].equalsIgnoreCase("disable")) {
					session.MTEnabled = false;
					player.sendMessage("§dMeasuring tape disabled");
				} else
					player.sendMessage("§cWrong argument. Type /mt help for help");
			}
		}
	}
    
	private class MeasuringTapeBlockListener extends BlockListener
	{ 
		public void onBlockDamage(BlockDamageEvent event) {
			if (event.getPlayer().getItemInHand().getTypeId() == 287 && event.getDamageLevel() == BlockDamageLevel.STARTED && CheckPermission(event.getPlayer(), "measuringtape.measure"))
				Attach(event.getPlayer(), event.getBlock(), MouseButton.LEFT);
		}
		
		public void onBlockRightClick(BlockRightClickEvent event) {
			if (event.getPlayer().getItemInHand().getTypeId() == 287 && CheckPermission(event.getPlayer(), "measuringtape.measure"))
				Attach(event.getPlayer(), event.getBlock(), MouseButton.RIGHT);
		}
	}
	
    private boolean CheckPermission(Player player, String permission) {
    	if (usePermissions) 
    		return Permissions.Security.permission(player, permission);
    	 else {
    		if (permission.equals("measuringtape.measure"))
    			return true;
    		else if (permission.equals("measuringtape.tape"))
    			return true;
    		else if (permission.equals("measuringtape.tp"))
    			return player.isOp();
    	}
    	return false;
    }
    
	private void Attach(Player player, Block block, MouseButton mousebutton) {
		Session session = GetSession(player);
		if (session.MTEnabled) {
			Location loc = new Location(block.getWorld(), block.getX(), block.getY(), block.getZ());
			if (session.mode == MeasuringMode.DISTANCE || session.mode == MeasuringMode.VECTORS || session.mode == MeasuringMode.AREA || session.mode == MeasuringMode.BLOCKS || session.mode == MeasuringMode.VOLUME) {
				if (mousebutton == MouseButton.LEFT) {
					session.pos.set(0, loc);
					if (!session.pos1Set) {
						session.pos1Set = true;
						player.sendMessage("§aMeasuring Tape attached to first position");
					}
				} else {
					session.pos.set(1, loc);
					if (!session.pos2Set) {
						session.pos2Set = true;
						player.sendMessage("§aMeasuring Tape attached to second position");
					}
				}
			} else if (session.mode == MeasuringMode.TRACK) {
	    		if (!session.pos1Set) {
					session.pos.set(0, loc);
					session.pos1Set = true;
					player.sendMessage("§aMeasuring Tape attached to first position");
	    		} else if (!session.pos2Set) {
					session.pos.set(1, loc);
					session.pos2Set = true;
					player.sendMessage("§aMeasuring Tape attached to second position");
	    		} else
	    			session.pos.add(loc);
			}
	    	if (session.pos1Set && session.pos2Set)
		    	ShowDistance(session);
		}
	}
    
	private void ShowDistance(Session session) {
		Player player = getServer().getPlayer(session.user);
		if (session.pos1Set && session.pos2Set) {
			Location diff = GetDiff(session.pos.get(0),session.pos.get(1));
			int x = Math.abs(diff.getBlockX()), y = Math.abs(diff.getBlockY()), z = Math.abs(diff.getBlockZ()); double distance = 0;
			int stringsAvailable = CountItem(player.getInventory(), 287);
			int stringsNeeded = 0;
			String msg = "";
			switch(session.mode) {
				case DISTANCE:
					distance = Math.round(Math.sqrt(Math.pow(x, 2) + Math.pow(y, 2) + Math.pow(z, 2)) * 10) / (double)10;
					stringsNeeded = (int)Math.ceil(distance / blocksPerString);
					msg = "Distance: " + distance + "m";
					break;
				case VECTORS:
					stringsNeeded = (int)Math.ceil((x + y + z) / blocksPerString);
					msg = "Vectors: X" + x + " Y" + z + " Z" + y;
					break;
				case AREA:
					x += 1; z += 1;
					stringsNeeded = (int)Math.ceil((x + z - 1) / blocksPerString);
					msg = "Area: " + x + "x" + z + " (" + x*z + " m2)";
					break;
				case BLOCKS:
					x += y + z + 1;
					stringsNeeded = (int)Math.ceil(x / blocksPerString);
					msg = "Blocks: " + x;
					break;
				case TRACK:
					for (int i = 1; i < session.pos.size(); i++) {
						diff = GetDiff(session.pos.get(i - 1), session.pos.get(i));
						distance += Math.sqrt(Math.pow(diff.getBlockX(), 2) + Math.pow(diff.getBlockY(), 2) + Math.pow(diff.getBlockZ(), 2));
					}
					distance = Math.round(distance * 10) / (double)10;
					stringsNeeded = (int)Math.ceil(distance / blocksPerString);
					msg = "Track: " + distance + "m";
					break;
				case VOLUME:
					x += 1; y += 1; z += 1;
					stringsNeeded = (int)Math.ceil((x + y + z - 2) / blocksPerString);
					msg = "Volume: " + x + "x" + y + "x" + z + " (" +  x*y*z + " m3)";
					break;
			}
			if (stringsNeeded <= stringsAvailable || blocksPerString == -1)
				player.sendMessage(msg);
			else
				player.sendMessage("§cYou have not enought tape. You need " + (stringsNeeded - stringsAvailable) + " more");
		} else
			player.sendMessage("§aBoth positions must be set");
	}
	
	private Session GetSession(Player player) {
		int idx = sessions.indexOf(new Session(player));
		if (idx != -1)
			return sessions.get(idx);
		else {
			sessions.add(new Session(player));
			return GetSession(player);
		}
	}
	
	private Location GetDiff(Location loc1, Location loc2) {
		return new Location(loc1.getWorld(), loc2.getBlockX() - loc1.getBlockX(), loc2.getBlockY() - loc1.getBlockY(), loc2.getBlockZ() - loc1.getBlockZ());
	}
	
	private Integer CountItem(Inventory invent, Integer itemId)	{
		int found = 0;
		for (ItemStack item : invent.getContents()) {
			if (item.getTypeId() == itemId)
				found += item.getAmount();
		}
		return found;
	}
}