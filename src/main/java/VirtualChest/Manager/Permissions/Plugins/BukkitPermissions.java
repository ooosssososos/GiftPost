/************************************************************************
 * This file is part of AdminCmd.									
 *																		
 * AdminCmd is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by	
 * the Free Software Foundation, either version 3 of the License, or		
 * (at your option) any later version.									
 *																		
 * AdminCmd is distributed in the hope that it will be useful,	
 * but WITHOUT ANY WARRANTY; without even the implied warranty of		
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the			
 * GNU General Public License for more details.							
 *																		
 * You should have received a copy of the GNU General Public License
 * along with AdminCmd.  If not, see <http://www.gnu.org/licenses/>.
 ************************************************************************/
package VirtualChest.Manager.Permissions.Plugins;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.D3GN.MiracleM4n.mChat.mChatAPI;

import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.permissions.Permission;
import org.bukkit.permissions.PermissionAttachmentInfo;

import VirtualChest.Manager.Permissions.AbstractPermission;

/**
 * @author Balor (aka Antoine Aflalo)
 * 
 */
public class BukkitPermissions extends AbstractPermission {
	private static mChatAPI mChatAPI = null;

	/**
	 * 
	 */
	public BukkitPermissions() {
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see be.Balor.Manager.Permissions.AbstractPermission#haveInfoNode()
	 */
	@Override
	public boolean haveInfoNode() {
		return mChatAPI != null;
	}

	/**
	 * @param mChatAPI
	 *            the mChatAPI to set
	 */
	public static void setmChatapi(mChatAPI mChatAPI) {
		if (BukkitPermissions.mChatAPI == null && mChatAPI != null)
			BukkitPermissions.mChatAPI = mChatAPI;
	}

	/**
	 * @return the mChatAPI
	 */
	public static boolean isApiSet() {
		return mChatAPI != null;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * be.Balor.Manager.Permissions.AbstractPermission#hasPerm(org.bukkit.command
	 * .CommandSender, java.lang.String, boolean)
	 */
	@Override
	public boolean hasPerm(CommandSender player, String perm, boolean errorMsg) {
		if (!(player instanceof Player))
			return true;
		if (player.hasPermission(perm))
			return true;
		else {
			if (errorMsg)
				player.sendMessage(ChatColor.RED + "You don't have the Permissions to do that "
						+ ChatColor.BLUE + "(" + perm + ")");

			return false;
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * be.Balor.Manager.Permissions.AbstractPermission#hasPerm(org.bukkit.command
	 * .CommandSender, org.bukkit.permissions.Permission, boolean)
	 */
	@Override
	public boolean hasPerm(CommandSender player, Permission perm, boolean errorMsg) {
		if (!(player instanceof Player))
			return true;
		if (player.hasPermission(perm))
			return true;
		else {
			player.sendMessage(ChatColor.RED + "You don't have the Permissions to do that "
					+ ChatColor.BLUE + "(" + perm + ")");
			return false;
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * be.Balor.Manager.Permissions.AbstractPermission#getPermissionLimit(org
	 * .bukkit.entity.Player, java.lang.String)
	 */
	@Override
	public String getPermissionLimit(Player p, String limit) {
		String result = null;
		if (mChatAPI != null)
			result = mChatAPI.getInfo(p, "giftpost." + limit);
		if (result == null || (result != null && result.isEmpty())) {
			Pattern regex = Pattern.compile("admincmd\\." + limit.toLowerCase() + "\\.[0-9]+");
			for (PermissionAttachmentInfo info : p.getEffectivePermissions()) {
				Matcher regexMatcher = regex.matcher(info.getPermission());
				if (regexMatcher.find())
					return info.getPermission().split("\\.")[2];

			}
		}
		else
			return result;
		return null;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * be.Balor.Manager.Permissions.AbstractPermission#getPrefix(java.lang.String
	 * , java.lang.String)
	 */
	@Override
	public String getPrefix(Player player) {
		if (mChatAPI != null)
			return mChatAPI.getPrefix(player);
		else
			return "";
	}

}