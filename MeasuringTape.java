//Author: DiddiZ
//Date: 2010-12-30

import java.util.ArrayList;
import java.util.Date;
import java.util.logging.Level;
import java.util.logging.Logger;

public class MeasuringTape extends Plugin
{
	static Logger minecraftLog = Logger.getLogger("Minecraft");
    private Listener listener = new Listener();
    private String name = "MeasuringTape";
    private String version = "0.5b";
    private ArrayList<Session> sessions = new ArrayList<Session>();
    private Integer tapeDelay;
    private Integer blocksPerString;

    public void enable()
    {
    	LoadProperties();
    	etc.getInstance().addCommand("/mt", " - Measuring tape. Type /mt help for help");
    }
    
    public void disable()
    {
    	etc.getInstance().removeCommand("/mt");
    }

    public void initialize()
    {
    	minecraftLog.info(name + " v" + version + " loaded");
        etc.getLoader().addListener(PluginLoader.Hook.COMMAND, listener, this, PluginListener.Priority.MEDIUM);
        etc.getLoader().addListener(PluginLoader.Hook.BLOCK_RIGHTCLICKED, listener, this, PluginListener.Priority.MEDIUM);
        etc.getLoader().addListener(PluginLoader.Hook.BLOCK_DESTROYED, listener, this, PluginListener.Priority.MEDIUM);
    }
    
    private void LoadProperties()
    {
    	PropertiesFile properties = new PropertiesFile("measuringTape.properties");
		try
		{
			tapeDelay = properties.getInt("tapeDelay", 15);
			blocksPerString = properties.getInt("blocksPerString", -1);
        }
		catch (Exception e)
		{
        	minecraftLog.log(Level.SEVERE, "Exception while reading from measuringTape.properties", e);
		}
	}
    
    private class Session
    {
    	public String user;
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
    	
    	Position()
    	{
    		
    	}
    	
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
    
	private class Listener extends PluginListener
	{ 
		public boolean onBlockDestroy(Player player, Block block) 
		{
			if (player.getItemInHand() != 287)
				return false;
			if (!player.canUseCommand("/measuringtape"))
				return false;
			if (block.getStatus() != 0)
				return false;
			Session session = GetSession(player);
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
			return true;
		}
		
		public void onBlockRightClicked (Player player, Block blockClicked, Item item)
		{
			if (item.getItemId() != 287)
				return;
			if (!player.canUseCommand("/measuringtape"))
				return;
	    	Session session = GetSession(player);
			if (session.mode == 0 || session.mode == 1 || session.mode == 2 || session.mode == 3)
				AttachToSecond(session, new Position(blockClicked), player);
			if (session.mode == 4)
			{
	    		if (!session.pos1Set)
	    			AttachToFirst(session, new Position(blockClicked), player);
	    		else if (!session.pos2Set)
	    			AttachToSecond(session, new Position(blockClicked), player);
	    		else
	    			session.pos.add(new Position(blockClicked));
			}
	    	if (session.pos1Set && session.pos2Set)
		    	ShowDistance(session);
		}
		
		public boolean onCommand(Player player, String[] split)
		{
			if (!split[0].equalsIgnoreCase("/mt"))
				return false;
			if (!player.canUseCommand("/measuringtape"))
				return false;
			Session session = GetSession(player);
			if (split.length == 1)
			{
				player.sendMessage("§cNo argument. Type /mt help for help");
			}
			else if (split[1].equalsIgnoreCase("tape") && player.canUseCommand("/mtcangetstring"))
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
						player.giveItem(287, 1);
						session.lastTape = new Date();
						player.sendMessage("§aHere is your measuring tape"); 
						player.sendMessage("§dLeft click: select pos #1; Right click select pos #2"); 
					}
					else
					{
						player.sendMessage("Yot got your last tape " + mins + "min ago.");
						player.sendMessage("You have to wait " + (tapeDelay - mins) + " minutes");
					}
				}
			}
			else if (split[1].equalsIgnoreCase("read"))
			{
				ShowDistance(session);
			}
			else if (split[1].equalsIgnoreCase("unset"))
			{
				session.ResetPos();
				player.sendMessage("§aMeasuring tape rolled up");
			}
			else if (split[1].equalsIgnoreCase("mode"))
			{
				if (split.length != 3)
				{
					player.sendMessage("§cCorrect usage: /mt mode [mode]");
				}
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
			else if (split[1].equalsIgnoreCase("tp") && player.canUseCommand("/mtteleport"))
			{
				if (session.mode == 2)
				{
					if (session.pos1Set && session.pos1Set)
					{
						Position diff = GetDifference(session.pos.get(0), session.pos.get(1));
						if ((diff.X) % 2 == 0 && (diff.Z) % 2 == 0)
						{
							player.teleportTo(session.pos.get(0).X + diff.X / 2 + 0.5 , Math.max(session.pos.get(0).Y, session.pos.get(1).Y) + 1, session.pos.get(0).Z + (diff.Z) / 2 + 0.5, player.getRotation(), player.getPitch());
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
					player.sendMessage("§cMeasuringTape Commands:");
					if (player.canUseCommand("/mtcangetstring"))
						player.sendMessage("§c/mt tape //Gives a measuring tape to the player");
					player.sendMessage("§c/mt read //Displays the distance again");
					player.sendMessage("§c/mt unset //Unsets both markers");
					player.sendMessage("§c/mt mode [mode] //Toggles measuring mode");
					player.sendMessage("§c/mt modehelp //Displays help to the modes");
					if (player.canUseCommand("/mtteleport"))
						player.sendMessage("§c/mt tp //Teleports to the center of the selected area");
				}
			else if (split[1].equalsIgnoreCase("modehelp"))
			{
				player.sendMessage("§cMeasuringTape Modes:");
				player.sendMessage("§cdistance - direct distance between both positions");
				player.sendMessage("§cvectors -xyz-vectors between the positions");
				player.sendMessage("§carea - area between the points");
				player.sendMessage("§cblocks - amount of blocks in x, y and z axis between positions");
				player.sendMessage("§ctrack - distance with multiple points");
			}
			else
				player.sendMessage("§cWrong argument. Type /mt help for help");
			return true;
		}
	}
	
	private void ShowDistance(Session session)
	{
		Player player = etc.getServer().getPlayer(session.user);
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
					x = (int)Math.ceil((Math.abs(diff.X) + Math.abs(diff.Y) + Math.abs(diff.Z) + 1) / blocksPerString);
					stringsNeeded = x;
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
						player.sendMessage("Distance: " + distance);
					else
						player.sendMessage("§cYou have not enought tape. You need " + (stringsNeeded - stringsAvailable) + " more");
					break;
			}
		}
		else
		{
			player.sendMessage("§aBoth positions must be set");
		}
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
			if (invent.getItemFromSlot(i) != null)
			{
				if (invent.getItemFromSlot(i).getItemId() == itemId)
					found += invent.getItemFromSlot(i).getAmount();
			}
		}
		return found;
	}
}

