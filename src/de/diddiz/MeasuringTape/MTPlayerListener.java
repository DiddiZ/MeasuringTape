package de.diddiz.MeasuringTape;

import org.bukkit.Material;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;

class MTPlayerListener implements Listener
{
	@EventHandler
	public static void onPlayerInteract(PlayerInteractEvent event) {
		if (event.getMaterial() == Material.STICK && MeasuringTape.hasPermission(event.getPlayer(), "measuringtape.measure")) {
			final Action action = event.getAction();
			if (action == Action.LEFT_CLICK_BLOCK)
				MeasuringTape.attach(event.getPlayer(), event.getClickedBlock(), action);
			else if (action == Action.RIGHT_CLICK_BLOCK)
				MeasuringTape.attach(event.getPlayer(), event.getClickedBlock(), action);
			else if (action == Action.LEFT_CLICK_AIR && Config.useTargetBlock)
				MeasuringTape.attach(event.getPlayer(), event.getPlayer().getTargetBlock(null, Integer.MAX_VALUE), action);
			else if (action == Action.RIGHT_CLICK_AIR && Config.useTargetBlock)
				MeasuringTape.attach(event.getPlayer(), event.getPlayer().getTargetBlock(null, Integer.MAX_VALUE), action);
			event.setCancelled(true);
		}
	}
}
