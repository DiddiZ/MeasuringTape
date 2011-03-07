//Author: DiddiZ
//Date: 2011-02-04

package com.bukkit.diddiz.MeasuringTape;

import java.io.File;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.Date;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.bukkit.Location;
import org.bukkit.Server;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.block.BlockDamageEvent;
import org.bukkit.event.block.BlockListener;
import org.bukkit.event.block.BlockRightClickEvent;
import org.bukkit.event.player.PlayerChatEvent;
import org.bukkit.event.player.PlayerListener;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.PluginDescriptionFile;
import org.bukkit.plugin.PluginLoader;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

import com.nijikokun.bukkit.Permissions.Permissions;

public class MeasuringTape extends JavaPlugin
{
	static Logger logger = Logger.getLogger("Minecraft");
	private MeasuringTapePlayerListener measuringTapePlayerListener = new MeasuringTapePlayerListener();
    private MeasuringTapeBlockListener measuringTapeBlockListener = new MeasuringTapeBlockListener();
    private ArrayList<Session> sessions = new ArrayList<Session>();
    private int tapeDelay;
    private int blocksPerString;
    private boolean defaultEnabled;
    private boolean usePermissions = false;

	public MeasuringTape(PluginLoader pluginLoader, Server instance, PluginDescriptionFile desc, File folder, File plugin, ClassLoader cLoader)
    {
		super(pluginLoader, instance, desc, folder, plugin, cLoader);
	}
    
	@Override
	public void onEnable()
	{
		try
		{
			File file = new File (getDataFolder(), "config.yml");
			if (!file.exists())
			{
				file.getParentFile().mkdirs();
				FileWriter writer = new FileWriter(file);
				String crlf = System.getProperty("line.separator");
				writer.write("tapeDelay : 15" + crlf
						+ "blocksPerString : -1" + crlf
						+ "defaultEnabled : true" + crlf
						+ "usePermissions : false");
				writer.close();
				logger.info("[MeasuringTape] Config created");
			}
			getConfiguration().load();
			tapeDelay = getConfiguration().getInt("tapeDelay", 15);
			blocksPerString = getConfiguration().getInt("blocksPerString", -1);
			defaultEnabled = getConfiguration().getBoolean("defaultEnabled", true);
			if (getConfiguration().getBoolean("usePermissions", false))
			{
				if (getServer().getPluginManager().getPlugin("Permissions") != null)
					usePermissions = true;
				else
					logger.info("[MeasuringTape] Permissions plugin not found. Use default permissions.");
			}
        }
		catch (Exception e)
		{
        	logger.log(Level.SEVERE, "[MeasuringTape] Exception while reading config.yml", e);
        	getServer().getPluginManager().disablePlugin(this);
		}
	    PluginManager pm = getServer().getPluginManager();
	    pm.registerEvent(Event.Type.PLAYER_COMMAND, measuringTapePlayerListener, Event.Priority.Normal, this);
		pm.registerEvent(Event.Type.BLOCK_DAMAGED, measuringTapeBlockListener, Event.Priority.Monitor, this);
		pm.registerEvent(Event.Type.BLOCK_RIGHTCLICKED, measuringTapeBlockListener, Event.Priority.Monitor, this);
        logger.info("MeasuringTape v" + this.getDescription().getVersion() + " by DiddiZ enabled");
	}
    
	@Override
	public void onDisable()
	{
		logger.info("MeasuringTape Disabled");
	}
    
	public enum MeasuringMode
	{
		DISTANCE, VECTORS, AREA, BLOCKS, TRACK, VOLUME;
	}
	
	public enum MouseButton
	{
		LEFT, RIGHT;
	}
	
	private class Session
    {
    	public String user;
    	public Boolean MTEnabled = defaultEnabled;
    	public ArrayList<Position> pos = new ArrayList<Position>();
        public Boolean pos1Set = false;
        public Boolean pos2Set = false;
        public MeasuringMode mode = MeasuringMode.DISTANCE;
        public Date lastTape = new Date(0);
        
        public Session (Player player)
        {
        	this.user = player.getName();
        	this.ResetPos();
        }
        
        public void ResetPos()
        {
        	pos = new ArrayList<Position>();
        	this.pos.add(new Position());
        	this.pos.add(new Position());
			this.pos1Set = false;
			this.pos2Set = false;
        }
    }

    private class Position
    {
    	public int X, Y, Z;
    	
    	Position() {}
    	
    	Position (int x, int y, int z)
    	{
    		this.X = x;
    		this.Y = y;
    		this.Z = z;
    	}
    	
    	Position (Block block)
    	{
    		this.X = block.getX();
    		this.Y = block.getY();
    		this.Z = block.getZ();
    	}
    	
    	public Position DifferenceTo(Position pos)
    	{
    		return new Position(pos.X - X, pos.Y - Y, pos.Z - Z);
    	}
    }
    
	private class MeasuringTapePlayerListener extends PlayerListener
	{ 
		public void onPlayerCommand(PlayerChatEvent event)
		{
			if (event.getMessage().substring(0, 3).equalsIgnoreCase("/mt") && CheckPermission(event.getPlayer(), "measuringtape.measure"))
			{
				String[] split = event.getMessage().split(" ");
				Player player = event.getPlayer();
				Session session = GetSession(player);
				if (split.length == 1)
					player.sendMessage("§cNo argument. Type /mt help for help");
				else if (split[1].equalsIgnoreCase("tape") && CheckPermission(event.getPlayer(), "measuringtape.tape"))
				{
					if (player.getInventory().contains(287))
					{
						player.sendMessage("§cYou have alredy a string"); 
						player.sendMessage("§dLeft click: select pos #1; Right click select pos #2"); 
					}
					else
					{
						long mins = (new Date().getTime() - session.lastTape.getTime()) / 60000;
						if (mins >= tapeDelay)
						{
							int free = player.getInventory().firstEmpty();
							if (free >= 0)
							{
								player.getInventory().setItem(free, player.getItemInHand());
								player.setItemInHand(new ItemStack(287, 1));
								session.lastTape = new Date();
								player.sendMessage("§aHere is your measuring tape"); 
								player.sendMessage("§dLeft click: select pos #1; Right click select pos #2"); 
							}
							else
								player.sendMessage("§cYou have no empty slot in your inventory"); 
						}
						else
						{
							player.sendMessage("§cYot got your last tape " + mins + "min ago.");
							player.sendMessage("§cYou have to wait " + (tapeDelay - mins) + " minutes");
						}
					}
				}
				else if (split[1].equalsIgnoreCase("read"))
					ShowDistance(session);
				else if (split[1].equalsIgnoreCase("unset"))
				{
					session.ResetPos();
					player.sendMessage("§aMeasuring tape rolled up");
				}
				else if (split[1].equalsIgnoreCase("mode"))
				{
					if (split.length != 3)
						player.sendMessage("§cCorrect usage: /mt mode [mode]");
					else if (split[2].equalsIgnoreCase("distance"))
					{
						session.mode = MeasuringMode.DISTANCE;
						player.sendMessage("§aMeasuring mode set to distance");
					}
					else if (split[2].equalsIgnoreCase("vectors"))
					{
						session.mode = MeasuringMode.VECTORS;
						player.sendMessage("§aMeasuring mode set to vectors");
					}
					else if (split[2].equalsIgnoreCase("area"))
					{
						session.mode = MeasuringMode.AREA;
						player.sendMessage("§aMeasuring mode set to area");
					}
					else if (split[2].equalsIgnoreCase("blocks"))
					{
						session.mode = MeasuringMode.BLOCKS;
						player.sendMessage("§aMeasuring mode set to blocks");
					}
					else if (split[2].equalsIgnoreCase("track"))
					{
						session.mode = MeasuringMode.TRACK;
						session.ResetPos();
						player.sendMessage("§aMeasuring mode set to track");
					}
					else if (split[2].equalsIgnoreCase("volume"))
					{
						session.mode = MeasuringMode.VOLUME;
						player.sendMessage("§aMeasuring mode set to volume");
					}
					else
						player.sendMessage("§cWrong argument. Type /mt for help");
				}
				else if (split[1].equalsIgnoreCase("tp") && CheckPermission(event.getPlayer(), "measuringtape.tp"))
				{
					if (session.mode == MeasuringMode.AREA && session.pos1Set && session.pos1Set)
					{
						Position diff = session.pos.get(0).DifferenceTo(session.pos.get(1));
						if ((diff.X) % 2 == 0 && (diff.Z) % 2 == 0)
						{
							double x = session.pos.get(0).X + diff.X / 2 + 0.5;
							double z = session.pos.get(0).Z + (diff.Z) / 2 + 0.5;
							player.teleportTo(new Location(player.getWorld(), x , player.getWorld().getHighestBlockYAt((int)x, (int)z), z, player.getLocation().getYaw(), player.getLocation().getPitch()));
							player.sendMessage("§aTeleported to center");
						}
						else 
							player.sendMessage("§cArea has not a single block as center");
					}
					else 
						player.sendMessage("§cBoth positions must be set and must be in area mode");
				}
				else if (split[1].equalsIgnoreCase("help"))
					{
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
					}
				else if (split[1].equalsIgnoreCase("modehelp"))
				{
					player.sendMessage("§dMeasuringTape Modes:");
					player.sendMessage("§ddistance - direct distance between both positions");
					player.sendMessage("§dvectors -xyz-vectors between the positions");
					player.sendMessage("§darea - area between the points");
					player.sendMessage("§dblocks - amount of blocks in x, y and z axis between positions");
					player.sendMessage("§dtrack - distance with multiple points");
					player.sendMessage("§dvolume - volume of a cuboid");
				}
				else if (split[1].equalsIgnoreCase("enable"))
				{
					session.MTEnabled = true;
					player.sendMessage("§dMeasuring tape enabled");
				}
				else if (split[1].equalsIgnoreCase("disable"))
				{
					session.MTEnabled = false;
					player.sendMessage("§dMeasuring tape disabled");
				}
				else
					player.sendMessage("§cWrong argument. Type /mt help for help");
				event.setCancelled(true);
			}
		}
	}
    
	private class MeasuringTapeBlockListener extends BlockListener
	{ 
		public void onBlockDamage(BlockDamageEvent event) 
		{
			if (event.getPlayer().getItemInHand().getTypeId() == 287 && event.getDamageLevel().getLevel() == 0 && CheckPermission(event.getPlayer(), "measuringtape.measure"))
				Attach(event.getPlayer(), event.getBlock(), MouseButton.LEFT);
		}
		
		public void onBlockRightClick(BlockRightClickEvent event)
		{
			if (event.getPlayer().getItemInHand().getTypeId() == 287 && CheckPermission(event.getPlayer(), "measuringtape.measure"))
				Attach(event.getPlayer(), event.getBlock(), MouseButton.RIGHT);
		}
	}
	
    private boolean CheckPermission(Player player, String permission)
    {
    	if (usePermissions)
    		return Permissions.Security.permission(player, permission);
    	else
    	{
    		if (permission.equals("measuringtape.measure"))
    			return true;
    		else if (permission.equals("measuringtape.tape"))
    			return true;
    		else if (permission.equals("measuringtape.tp"))
    			return player.isOp();
    	}
    	return false;
    }
    
	private void Attach(Player player, Block block, MouseButton mousebutton)
	{
		Session session = GetSession(player);
		if (session.MTEnabled)
		{
			if (session.mode == MeasuringMode.DISTANCE || session.mode == MeasuringMode.VECTORS || session.mode == MeasuringMode.AREA || session.mode == MeasuringMode.BLOCKS || session.mode == MeasuringMode.VOLUME)
			{
				if (mousebutton == MouseButton.LEFT)
				{
					session.pos.set(0, new Position(block));
					if (!session.pos1Set)
					{
						session.pos1Set = true;
						player.sendMessage("§aMeasuring Tape attached to first position");
					}
				}
				else
				{
					session.pos.set(1, new Position(block));
					if (!session.pos2Set)
					{
						session.pos2Set = true;
						player.sendMessage("§aMeasuring Tape attached to second position");
					}
				}
			}
			else if (session.mode == MeasuringMode.TRACK)
			{
	    		if (!session.pos1Set)
	    		{
					session.pos.set(0, new Position(block));
					session.pos1Set = true;
					player.sendMessage("§aMeasuring Tape attached to first position");
	    		}
	    		else if (!session.pos2Set)
	    		{
					session.pos.set(1, new Position(block));
					session.pos2Set = true;
					player.sendMessage("§aMeasuring Tape attached to second position");
	    		}
	    		else
	    			session.pos.add(new Position(block));
			}
	    	if (session.pos1Set && session.pos2Set)
		    	ShowDistance(session);
		}
	}
    
	private void ShowDistance(Session session)
	{
		Player player = getServer().getPlayer(session.user);
		if (session.pos1Set && session.pos2Set)
		{
			Position diff = session.pos.get(0).DifferenceTo(session.pos.get(1));
			diff.X = Math.abs(diff.X); diff.Y = Math.abs(diff.Y); diff.Z = Math.abs(diff.Z); 
			int x = Math.abs(diff.X), y = Math.abs(diff.Y), z = Math.abs(diff.Z); double distance = 0;
			int stringsAvailable = CountItem(player.getInventory(), 287);
			int stringsNeeded = 0;
			String msg = "";
			switch(session.mode)
			{
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
					for (int i = 1; i < session.pos.size(); i++)
					{
						diff = session.pos.get(i - 1).DifferenceTo(session.pos.get(i));
						distance += Math.sqrt(Math.pow(diff.X, 2) + Math.pow(diff.Y, 2) + Math.pow(diff.Z, 2));
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
		}
		else
			player.sendMessage("§aBoth positions must be set");
	}
	
	private Session GetSession(Player player)
	{
		for (Integer i = 0; i < sessions.size(); i++)
		{
			if (sessions.get(i).user.equals(player.getName()))
				return sessions.get(i);
		}
		sessions.add(new Session(player));
		return GetSession(player);
	}
	
	private Integer CountItem(Inventory invent, Integer itemId)
	{
		int found = 0;
		for (int i = 0; i <= 35; i++)
		{
			if (invent.getItem(i) != null)
			{
				if (invent.getItem(i).getTypeId() == itemId)
					found += invent.getItem(i).getAmount();
			}
		}
		return found;
	}
}