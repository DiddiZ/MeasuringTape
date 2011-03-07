//Author: DiddiZ
//Date: 2010-12-16

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.logging.Level;
import java.util.logging.Logger;

public class MeasuringTape extends Plugin
{
	static Logger minecraftLog = Logger.getLogger("Minecraft");
    private Listener listener = new Listener();
    //private Logger log;
    private String name = "MeasuringTape";
    private String version = "0.3";
    private ArrayList<Session> sessions = new ArrayList<Session>();
    private Integer tapeDelay;

    public void enable()
    {
    	
    }
    
    public void disable()
    {
    	
    }

    public void initialize()
    {
    	LoadProperties();
    	//log = Logger.getLogger("Minecraft");
    	minecraftLog.info(name + " v" + version + " loaded");
        etc.getLoader().addListener(PluginLoader.Hook.COMMAND, listener, this, PluginListener.Priority.MEDIUM);
        etc.getLoader().addListener(PluginLoader.Hook.BLOCK_RIGHTCLICKED, listener, this, PluginListener.Priority.MEDIUM);
        etc.getLoader().addListener(PluginLoader.Hook.BLOCK_DESTROYED, listener, this, PluginListener.Priority.MEDIUM);
    }
    
    private void LoadProperties()
    {
		if (!new File("measuringTape.properties").exists())
		{
			FileWriter writer = null;
            try
            {
            	writer = new FileWriter("measuringTape.properties");
            	writer.write("tapeDelay=15\r\n");                    
            }
            catch (Exception e)
            {
            	minecraftLog.log(Level.SEVERE, "Exception while creating measuringTape.properties", e);
            }
            finally
            {
                try
                {
                    if (writer != null)
                        writer.close();
                }
                catch (IOException e)
                {
                	minecraftLog.log(Level.SEVERE, "Exception while closing writer for measuringTape.properties", e);
                }
            }
		}
		PropertiesFile properties = new PropertiesFile("measuringTape.properties");
		try
		{
			tapeDelay = properties.getInt("tapeDelay");
        }
		catch (Exception e)
		{
        	minecraftLog.log(Level.SEVERE, "Exception while reading from measuringTape.properties", e);
		}
	}
    
    class Session
    {
    	public Player user;
        public Location pos1 = new Location();
        public Location pos2 = new Location();
        public Boolean pos1Set = false;
        public Boolean pos2Set = false;
        public Integer mode = 0;
        public Date lastTape = new Date(0);
        
        public Session (Player player)
        {
        	this.user = player;
        }
    }

	class Listener extends PluginListener
	{ 
		public boolean onBlockDestroy(Player player, Block block) 
		{
			if (player.getItemInHand() != 287)
				return false;
			if (!player.canUseCommand("/measuringtape"))
				return false;
			if (block.getStatus() != 0)
				return false;
			if ( block.getX() == 0 && block.getY() == 0 && block.getZ() == 0)
				return false;
			Session session = GetSession(player);
    		session.pos1.x = block.getX();
    		session.pos1.y = block.getY();
    		session.pos1.z = block.getZ();
    		if (!session.pos1Set)
    		{
    			session.pos1Set = true;
    			player.sendMessage("§aMeasuring Tape attached to first position");
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
    		session.pos2.x = blockClicked.getX();
    		session.pos2.y = blockClicked.getY();
    		session.pos2.z = blockClicked.getZ();
    		if (!session.pos2Set)
    		{
    			session.pos2Set = true;
    			player.sendMessage("§aMeasuring Tape attached to second position");
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
				player.sendMessage("§cMeasuringTape Commands:");
				if (player.canUseCommand("/mtcangetstring"))
					player.sendMessage("§c/mt tape //Gives a measuring tape to the player");
				player.sendMessage("§c/mt read //Displays the distance again");
				player.sendMessage("§c/mt unset //Unsets both markers");
				player.sendMessage("§c/mt mode [distance|vectors|area] //Toggles measuring mode");
				if (player.canUseCommand("/mtteleport"))
					player.sendMessage("§c/mt tp //Teleports to the center of the selected area");
			}
			else if (split[1].equalsIgnoreCase("tape") && player.canUseCommand("/mtcangetstring"))
			{
				
				
				if (CountItem(player, 287) > 0)
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
				session.pos1 = new Location();
				session.pos1Set = false;
				session.pos2 = new Location();
				session.pos2Set = false;
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
				else
					player.sendMessage("§cWrong argument. Type /mt for help");
			}
			else if (split[1].equalsIgnoreCase("tp") && player.canUseCommand("/mtteleport"))
			{
				if (session.mode == 2)
				{
					if (session.pos1Set && session.pos1Set)
					{
						if ((session.pos2.x - session.pos1.x) % 2 == 0 && (session.pos2.z - session.pos1.z) % 2 == 0)
						{
							player.teleportTo(session.pos1.x + (session.pos2.x - session.pos1.x) / 2 + 0.5 , Math.max(session.pos1.y, session.pos2.y)+ 1, session.pos1.z + (session.pos2.z - session.pos1.z) / 2 + 0.5, player.getRotation(), player.getPitch());
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
			else
				player.sendMessage("§cWrong argument. Type /mt for help");
			return true;
		}
	}
	
	private void ShowDistance(Session session)
	{
		if (session.pos1Set && session.pos2Set)
		{
			switch(session.mode)
			{
			case 0:
				session.user.sendMessage("Distance: " + (double)Math.round(Math.sqrt(Math.pow(session.pos2.x - session.pos1.x, 2) + Math.pow(session.pos2.y - session.pos1.y, 2) + Math.pow(session.pos2.z - session.pos1.z, 2)) * 10) / 10 + "m");
				break;
			case 1:
	    		session.user.sendMessage("Vectors: X" + (int)Math.abs(session.pos2.x - session.pos1.x)  + " Y" + (int)Math.abs(session.pos2.z - session.pos1.z) + " Z" + (int)Math.abs(session.pos2.y - session.pos1.y));
	    		break;
			case 2: 
				session.user.sendMessage("Area: " + (int)(Math.abs(session.pos2.x - session.pos1.x) + 1) + "x" + "" + (int)(Math.abs(session.pos2.z - session.pos1.z) + 1));
				break;
			}
		}
		else
		{
			session.user.sendMessage("§aBoth positions must be set");
		}
	}
	
	private Session GetSession(Player player)
	{
		for (Integer i = 0; i < sessions.size(); i++)
		{
			if (sessions.get(i).user.getName().equals(player.getName()))
				return sessions.get(i);
		}
		sessions.add(new Session(player));
		return GetSession(player);
	}
	
	private Integer CountItem(Player player, Integer itemId)
	{
		Integer found = 0;
		Inventory invent = player.getInventory();
		for (Integer i = 0; i <= 35; i++)
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