//Author: DiddiZ
//Date: 2011-01-23
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

public class MeasuringTape extends JavaPlugin
{
	static Logger logger = Logger.getLogger("Minecraft");
	private MeasuringTapePlayerListener measuringTapePlayerListener = new MeasuringTapePlayerListener();
    private MeasuringTapeBlockListener measuringTapeBlockListener = new MeasuringTapeBlockListener();
    private ArrayList<Session> sessions = new ArrayList<Session>();
    private Integer tapeDelay;
    private Integer blocksPerString;
    private Boolean defaultEnabled;

	public MeasuringTape(PluginLoader pluginLoader, Server instance, PluginDescriptionFile desc, File folder, File plugin, ClassLoader cLoader)
    {
		super(pluginLoader, instance, desc, folder, plugin, cLoader);
	}
    
	@Override
	public void onEnable()
	{
	    LoadProperties();
	    PluginManager pm = getServer().getPluginManager();
	    pm.registerEvent(Event.Type.PLAYER_COMMAND, measuringTapePlayerListener, Event.Priority.Normal, this);
		pm.registerEvent(Event.Type.BLOCK_DAMAGED, measuringTapeBlockListener, Event.Priority.Monitor, this);
		pm.registerEvent(Event.Type.BLOCK_RIGHTCLICKED, measuringTapeBlockListener, Event.Priority.Monitor, this);
        logger.info("MeasuringTape v" + this.getDescription().getVersion() + " by DiddiZ enabled");
	}
    
	@Override
	public void onDisable()
	{
		logger.info("MeasuringTape disabled");
	}
    
    private void LoadProperties()
    {
		try
		{
			File file = new File (getDataFolder(), "config.yml");
			if (!file.exists())
			{
				file.getParentFile().mkdirs();
				FileWriter writer = new FileWriter(file);
				writer.write("tapeDelay : 15" + System.getProperty("line.separator"));
				writer.write("blocksPerString : -1" + System.getProperty("line.separator"));
				writer.write("defaultEnabled : true");
				writer.close();
				logger.info("ClayReg config created");
			}
			getConfiguration().load();
			tapeDelay = getConfiguration().getInt("tapeDelay", 15);
			blocksPerString = getConfiguration().getInt("blocksPerString", -1);
			defaultEnabled = getConfiguration().getBoolean("defaultEnabled", true);
        }
		catch (Exception e)
		{
        	logger.log(Level.SEVERE, "Exception while reading from measuringTape.properties", e);
		}
	}
    
    private class Session
    {
    	public String user;
    	public Boolean MTEnabled = defaultEnabled;
    	public ArrayList<Position> pos = new ArrayList<Position>();
        public Boolean pos1Set = false;
        public Boolean pos2Set = false;
        public Integer mode = 0;
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
    }
    
	private class MeasuringTapePlayerListener extends PlayerListener
	{ 
		public void onPlayerCommand(PlayerChatEvent event)
		{
			if (event.getMessage().substring(0, 3).equalsIgnoreCase("/mt"))
			{
				String[] split = event.getMessage().split(" ");
				Player player = event.getPlayer();
				Session session = GetSession(player);
				if (split.length == 1)
					player.sendMessage("§cNo argument. Type /mt help for help");
				else if (split[1].equalsIgnoreCase("tape"))
				{
					if (CountItem(player.getInventory(), 287) > 0)
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
						session.mode = 0;
						player.sendMessage("§aMeasuring mode set to distance");
					}
					else if (split[2].equalsIgnoreCase("vectors"))
					{
						session.mode = 1;
						player.sendMessage("§aMeasuring mode set to vectors");
					}
					else if (split[2].equalsIgnoreCase("area"))
					{
						session.mode = 2;
						player.sendMessage("§aMeasuring mode set to area");
					}
					else if (split[2].equalsIgnoreCase("blocks"))
					{
						session.mode = 3;
						player.sendMessage("§aMeasuring mode set to blocks");
					}
					else if (split[2].equalsIgnoreCase("track"))
					{
						session.mode = 4;
						session.ResetPos();
						player.sendMessage("§aMeasuring mode set to track");
					}
					else
						player.sendMessage("§cWrong argument. Type /mt for help");
				}
				else if (split[1].equalsIgnoreCase("tp") && player.isOp())
				{
					if (session.mode == 2)
					{
						if (session.pos1Set && session.pos1Set)
						{
							Position diff = GetDifference(session.pos.get(0), session.pos.get(1));
							if ((diff.X) % 2 == 0 && (diff.Z) % 2 == 0)
							{
								player.teleportTo(new Location(getServer().getWorlds()[0], session.pos.get(0).X + diff.X / 2 + 0.5 , Math.max(session.pos.get(0).Y, session.pos.get(1).Y) + 1, session.pos.get(0).Z + (diff.Z) / 2 + 0.5, player.getLocation().getYaw(), player.getLocation().getPitch()));
								player.sendMessage("§aTeleported to center");
							}
							else 
								player.sendMessage("§cArea has not a single block as center");
						}
						else
							player.sendMessage("§cBoth positions must be set");
					}
					else 
						player.sendMessage("§cOnly available in area mode");
				}
				else if (split[1].equalsIgnoreCase("help"))
					{
						player.sendMessage("§dMeasuringTape Commands:");
						player.sendMessage("§d/mt tape //Gives a measuring tape to the player");
						player.sendMessage("§d/mt read //Displays the distance again");
						player.sendMessage("§d/mt unset //Unsets both markers");
						player.sendMessage("§d/mt mode [mode] //Toggles measuring mode");
						player.sendMessage("§d/mt modehelp //Displays help to the modes");
						if (player.isOp())
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
			if (!event.isCancelled() && event.getPlayer().getItemInHand().getTypeId() == 287 && event.getDamageLevel().getLevel() == 0)
			{
				Block block = event.getBlock();
				Player player = event.getPlayer();
				Session session = GetSession(player);
				if (session.MTEnabled)
				{
					if (session.mode == 0 || session.mode == 1 || session.mode == 2 || session.mode == 3)
						AttachToFirst(session, new Position(block), player);
					if (session.mode == 4)
					{
			    		if (!session.pos1Set)
			    			AttachToFirst(session, new Position(block), player);
			    		else if (!session.pos2Set)
			    			AttachToSecond(session, new Position(block), player);
			    		else
			    			session.pos.add(new Position(block));
					}
			    	if (session.pos1Set && session.pos2Set)
				    	ShowDistance(session);
			    	event.setCancelled(true);
				}
			}
		}
		
		public void onBlockRightClick(BlockRightClickEvent event)
		{
			if (event.getPlayer().getItemInHand().getTypeId() == 287)
			{
				Block block = event.getBlock();
				Player player = event.getPlayer();
				Session session = GetSession(player);
				if (session.MTEnabled)
				{
					if (session.mode == 0 || session.mode == 1 || session.mode == 2 || session.mode == 3)
						AttachToSecond(session, new Position(block), player);
					if (session.mode == 4)
					{
			    		if (!session.pos1Set)
			    			AttachToFirst(session, new Position(block), player);
			    		else if (!session.pos2Set)
			    			AttachToSecond(session, new Position(block), player);
			    		else
			    			session.pos.add(new Position(block));
					}
			    	if (session.pos1Set && session.pos2Set)
				    	ShowDistance(session);
				}
			}
		}
	}
	
	private void ShowDistance(Session session)
	{
		Player player = getServer().getPlayer(session.user);
		if (session.pos1Set && session.pos2Set)
		{
			Position diff = GetDifference(session.pos.get(0), session.pos.get(1));
			int x, y, z; double distance = 0;
			int stringsAvailable = CountItem(player.getInventory(), 287);
			int stringsNeeded;
			switch(session.mode)
			{
				case 0:
					distance = Math.round(Math.sqrt(Math.pow(diff.X, 2) + Math.pow(diff.Y, 2) + Math.pow(diff.Z, 2)) * 10) / (double)10;
					stringsNeeded = (int)Math.ceil(distance / blocksPerString);
					if (stringsNeeded <= stringsAvailable || blocksPerString == -1)
						player.sendMessage("Distance: " + distance + "m");
					else
						player.sendMessage("§cYou have not enought tape. You need " + (stringsNeeded - stringsAvailable) + " more");
					break;
				case 1:
					x = Math.abs(diff.X);
					y = Math.abs(diff.Y);
					z = Math.abs(diff.Z);
					stringsNeeded = (int)Math.ceil((x + y + z) / blocksPerString);
					if (stringsNeeded <= stringsAvailable || blocksPerString == -1)
						player.sendMessage("Vectors: X" + x + " Y" + z + " Z" + y);
					else
						player.sendMessage("§cYou have not enought tape. You need " + (stringsNeeded - stringsAvailable) + " more");
					break;
				case 2:
					x = Math.abs(diff.X) + 1;
					z = Math.abs(diff.Z) + 1;
					stringsNeeded = (int)Math.ceil((x + z) / blocksPerString);
					if (stringsNeeded <= stringsAvailable || blocksPerString == -1)
						player.sendMessage("Area: " + x + "x" + z);
					else
						player.sendMessage("§cYou have not enought tape. You need " + (stringsNeeded - stringsAvailable) + " more");
					break;
				case 3:
					x = Math.abs(diff.X) + Math.abs(diff.Y) + Math.abs(diff.Z) + 1;
					stringsNeeded = (int)Math.ceil(x / blocksPerString);
					if (stringsNeeded <= stringsAvailable || blocksPerString == -1)
						player.sendMessage("Blocks: " + x);
					else
						player.sendMessage("§cYou have not enought tape. You need " + (stringsNeeded - stringsAvailable) + " more");
					break;
				case 4:
					for (int i = 1; i < session.pos.size(); i++)
					{
						diff = GetDifference(session.pos.get(i - 1), session.pos.get(i));
						distance += Math.sqrt(Math.pow(diff.X, 2) + Math.pow(diff.Y, 2) + Math.pow(diff.Z, 2));
					}
					distance = Math.round(distance * 10) / (double)10;
					stringsNeeded = (int)Math.ceil(distance / blocksPerString);
					if (stringsNeeded <= stringsAvailable || blocksPerString == -1)
						player.sendMessage("Track: " + distance + "m");
					else
						player.sendMessage("§cYou have not enought tape. You need " + (stringsNeeded - stringsAvailable) + " more");
					break;
			}
		}
		else
			player.sendMessage("§aBoth positions must be set");
	}
	
	private Position GetDifference(Position pos1, Position pos2)
	{
		return new Position(pos2.X - pos1.X, pos2.Y - pos1.Y, pos2.Z - pos1.Z);
	}
	
	private void AttachToFirst(Session session, Position pos, Player player)
	{
		session.pos.set(0, pos);
		if (!session.pos1Set)
		{
			session.pos1Set = true;
			player.sendMessage("§aMeasuring Tape attached to first position");
		}
	}
	
	private void AttachToSecond(Session session, Position pos, Player player)
	{
		session.pos.set(1, pos);
		if (!session.pos2Set)
		{
			session.pos2Set = true;
			player.sendMessage("§aMeasuring Tape attached to second position");
		}
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
