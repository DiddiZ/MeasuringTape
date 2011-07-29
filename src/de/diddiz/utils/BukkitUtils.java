package de.diddiz.utils;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

public class BukkitUtils
{
	public static String materialName(int type) {
		final Material mat = Material.getMaterial(type);
		return mat != null ? mat.toString().replace('_', ' ').toLowerCase() : String.valueOf(type);
	}

	public static void giveTool(Player player, int type) {
		final Inventory inv = player.getInventory();
		if (inv.contains(type))
			player.sendMessage(ChatColor.RED + "You have alredy a " + materialName(type));
		else {
			final int free = inv.firstEmpty();
			if (free >= 0) {
				if (player.getItemInHand() != null && player.getItemInHand().getTypeId() != 0)
					inv.setItem(free, player.getItemInHand());
				player.setItemInHand(new ItemStack(type, 1));
				player.sendMessage(ChatColor.GREEN + "Here's your " + materialName(type));
			} else
				player.sendMessage(ChatColor.RED + "You have no empty slot in your inventory");
		}
	}
}
