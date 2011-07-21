/*This file is part of GiftPost .

    GiftPost is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    GiftPost is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with GiftPost.  If not, see <http://www.gnu.org/licenses/>.*/
package com.Balor.bukkit.GiftPost;

//GiftPost
import static com.Balor.utils.Display.chestKeeper;

import com.Balor.commands.GPCommand;
import com.Balor.utils.FilesManager;
import com.Balor.utils.LogFormatter;
import com.Balor.utils.PlayerChests;
import com.aranai.virtualchest.VirtualChest;
import com.aranai.virtualchest.VirtualLargeChest;
//Plugins
import com.gmail.nossr50.mcMMO;
import com.google.common.collect.MapMaker;
import com.nijiko.permissions.PermissionHandler;
import be.Balor.register.payment.Method;
//Java
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentMap;
import java.util.logging.FileHandler;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;
//Bukkit

import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.util.config.Configuration;

/**
 * 
 * @author Balor
 */
public class GiftPostWorker {

	private ConcurrentMap<String, ConcurrentMap<String, VirtualChest>> chests = new MapMaker()
			.concurrencyLevel(8).makeMap();
	private ConcurrentMap<String, VirtualChest> defaultChests = new MapMaker().concurrencyLevel(8)
			.softValues().makeMap();
	private ConcurrentMap<String, VirtualChest> sendReceiveChests = new MapMaker()
			.concurrencyLevel(8).softValues().makeMap();
	private static PermissionHandler permission = null;
	private List<GPCommand> commands = new ArrayList<GPCommand>();
	private Configuration config;
	private FilesManager fManager;
	public static final Logger log = Logger.getLogger("Minecraft");
	public static final Logger workerLog = Logger.getLogger("VirtualChest");
	private static Method payementMethod = null;
	private static mcMMO mcMMO = null;
	private HashMap<String, VirtualChest> parties = new HashMap<String, VirtualChest>();
	private static GiftPostWorker instance;
	private ConcurrentMap<String, PlayerChests> allChests = new MapMaker().concurrencyLevel(8)
			.makeMap();
	private static boolean disable = false;

	private GiftPostWorker() {
	}

	public static GiftPostWorker getInstance() {
		if (instance == null)
			instance = new GiftPostWorker();
		return instance;
	}

	public static void killInstance() {
		workerLog.info("Worker instance destroyed");
		for (Handler h : workerLog.getHandlers()) {
			h.close();
			workerLog.removeHandler(h);
		}
		instance = null;
	}

	public void setConfig(Configuration config) {
		this.config = config;
		this.config.load();
	}

	public void setfManager(String path) {
		this.fManager = new FilesManager(path);
		FileHandler fh;

		try {

			// This block configure the logger with handler and formatter
			File logger = new File(path + File.separator + "log.txt");
			if (logger.exists())
				logger.delete();
			fh = new FileHandler(logger.getPath(), true);
			workerLog.addHandler(fh);
			workerLog.setUseParentHandlers(false);
			workerLog.setLevel(Level.ALL);
			LogFormatter formatter = new LogFormatter();
			fh.setFormatter(formatter);

			// the following statement is used to log any messages
			workerLog.info("Logger created");

		} catch (SecurityException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public FilesManager getFileManager() {
		return fManager;
	}

	public Configuration getConfig() {
		return config;
	}

	/**
	 * @param disable
	 *            the disable to set
	 */
	public static void setDisable(boolean disable2) {
		disable = disable2;
	}

	/**
	 * @return the disable
	 */
	public static boolean isDisable() {
		return disable;
	}

	/**
	 * Return the chest, create it if not exist
	 * 
	 * @param playerName
	 * @return
	 */
	public VirtualChest getChest(String playerName, String chestName) {
		if (playerName == null) {
			workerLog.severe("PlayerName == null");
			return null;
		}
		if (chestName == null) {
			workerLog.severe("chestName == null");
			return null;
		}
		workerLog.info("Opening chest :" + chestName + " of player " + playerName);
		if (chests.containsKey(playerName) && chests.get(playerName).containsKey(chestName))
			return chests.get(playerName).get(chestName);
		else {
			if (chestExists(playerName, chestName)) {
				ConcurrentMap<String, VirtualChest> loadedChests = fManager
						.getPlayerChests(playerName);
				chests.put(playerName, loadedChests);
				workerLog.info("Chests owned by " + playerName + " loaded from file ("
						+ loadedChests.size() + ")");
				return loadedChests.get(chestName);
			} else {
				workerLog.warning("Tried to load " + chestName + " of player " + playerName
						+ " that don't exist");
				return null;
			}
		}
	}

	/**
	 * Save the chests of the player and remove it from memory.
	 * 
	 * @param player
	 */
	public void unloadPlayerChests(String player) {
		fManager.savePlayerChest(player, chests.get(player));
		defaultChests.remove(player);
		sendReceiveChests.remove(player);
		ConcurrentMap<String, VirtualChest> playerChests = chests.get(player);
		for (String name : playerChests.keySet())
			playerChests.get(name).emptyChest();
		playerChests.clear();
		playerChests = null;
		chests.remove(player);
		workerLog.info("Chests of " + player + " unloaded from memory.");
	}

	/**
	 * check if the given chest already exists.
	 * 
	 * @param playerName
	 * @param chestName
	 * @return
	 */
	public boolean chestExists(Player player, String chestName) {
		return chestExists(player.getName(), chestName);
	}

	public boolean chestExists(String player, String chestName) {
		return (allChests.containsKey(player) && allChests.get(player).hasChest(chestName));
	}

	/**
	 * Return the number of owned chest
	 * 
	 * @param p
	 * @return
	 */
	public int numberOfChest(Player p) {
		if (allChests.containsKey(p.getName()))
			return allChests.get(p.getName()).names.size();
		else
			return 0;
	}

	/**
	 * Return all the chest of the selected player
	 * 
	 * @param p
	 * @return
	 */
	public ConcurrentMap<String, VirtualChest> listOfChest(Player p) {
		return chests.get(p.getName());
	}

	public ArrayList<String> chestList(String playerName) {
		if (allChests.containsKey(playerName)) {
			return (ArrayList<String>) allChests.get(playerName).names;
		} else
			return null;
	}

	/**
	 * Return the default chest.
	 * 
	 * @param playerName
	 * @return
	 */
	public VirtualChest getDefaultChest(String playerName) {
		if (defaultChests.containsKey(playerName))
			return defaultChests.get(playerName);
		else {
			VirtualChest v = getChest(playerName, fManager.openDefaultChest(playerName));
			defaultChests.put(playerName, v);
			return v;
		}

	}

	/**
	 * Return the send chest.
	 * 
	 * @param playerName
	 * @return
	 */
	public VirtualChest getSendChest(String playerName) {
		if (sendReceiveChests.containsKey(playerName))
			return sendReceiveChests.get(playerName);
		else {
			VirtualChest v = getChest(playerName, fManager.openSendChest(playerName));
			if (v == null)
				return getDefaultChest(playerName);
			else {
				sendReceiveChests.put(playerName, v);
				return v;
			}
		}
	}

	/**
	 * Set default chest for the player.
	 * 
	 * @param playerName
	 * @param v
	 * @return
	 */
	public boolean setDefaultChest(String playerName, String vChest) {
		VirtualChest v = getChest(playerName, vChest);
		return setDefaultChest(playerName, v);
	}

	public boolean setDefaultChest(String playerName, VirtualChest v) {
		if (chests.get(playerName).containsValue(v)) {
			defaultChests.put(playerName, v);
			fManager.createDefaultChest(playerName, v.getName());
			return true;
		}
		return false;
	}

	/**
	 * Set send chest for the player.
	 * 
	 * @param playerName
	 * @param v
	 * @return
	 */
	public boolean setSendChest(String playerName, String vChest) {
		VirtualChest v = getChest(playerName, vChest);
		if (v != null)
			return setSendChest(playerName, v);
		return false;
	}

	public boolean setSendChest(String playerName, VirtualChest v) {
		if (chests.containsKey(playerName) && chests.get(playerName).containsValue(v)) {
			sendReceiveChests.put(playerName, v);
			fManager.createSendReceiveChest(playerName, v.getName());
			return true;
		}
		return false;
	}

	/**
	 * add the new chest in the list of existing chests.
	 * 
	 * @param playerName
	 * @param type
	 * @param vChestName
	 */
	private void addInAllChests(String playerName, String type, String vChestName) {
		if (allChests.containsKey(playerName)) {
			allChests.get(playerName).names.add(vChestName);
			allChests.get(playerName).types.add(type);
		} else {
			PlayerChests pChest = new PlayerChests();
			pChest.names.add(vChestName);
			pChest.types.add("type");
			allChests.put(playerName, pChest);
		}
		workerLog.info("Created new " + type + " chest (" + vChestName + ") for " + playerName);

	}

	/**
	 * Add a new chest
	 * 
	 * @param player
	 * @param vChest
	 *            VirtualChest to add
	 * 
	 */
	public void addChest(Player player, VirtualChest vChest) {

		if (chests.containsKey(player.getName()))
			chests.get(player.getName()).put(vChest.getName(), vChest);
		else {
			ConcurrentMap<String, VirtualChest> tmp = new MapMaker().makeMap();
			tmp.put(vChest.getName(), vChest);
			chests.put(player.getName(), tmp);
		}
		if (vChest instanceof VirtualLargeChest)
			fManager.createChestFile(player, vChest.getName(), "large");
		else
			fManager.createChestFile(player, vChest.getName(), "normal");
		if (numberOfChest(player) == 1)
			setDefaultChest(player.getName(), vChest);
		if (vChest instanceof VirtualLargeChest)
			addInAllChests(player.getName(), "large", vChest.getName());
		else
			addInAllChests(player.getName(), "normal", vChest.getName());

	}

	public boolean upgradeChest(Player player, VirtualChest vChest) {
		if (chests.containsKey(player.getName())) {
			VirtualLargeChest newChest = new VirtualLargeChest(vChest);
			chests.get(player.getName()).put(vChest.getName(), newChest);
			fManager.upgradeChest(player, vChest.getName());
			allChests.get(player.getName()).types.set(
					allChests.get(player.getName()).names.indexOf(vChest.getName()), "large");
			if (defaultChests.containsValue(vChest))
				defaultChests.put(player.getName(), newChest);

			if (sendReceiveChests.containsValue(vChest))
				sendReceiveChests.put(player.getName(), newChest);
			return true;
		}
		return false;

	}

	/**
	 * 
	 * @param player
	 * @return if the player have a chest loaded.
	 */
	public boolean haveAChestInMemory(String player) {
		return chests.containsKey(player);
	}

	/**
	 * Delete the chest from memory and save.
	 * 
	 * @param player
	 * @param vChest
	 * @return
	 */
	public boolean removeChest(Player player, VirtualChest vChest) {
		String pName = player.getName();
		if (chests.containsKey(pName)) {
			ConcurrentMap<String, VirtualChest> playerChests = chests.get(pName);
			if (playerChests.remove(vChest.getName()) != null) {
				fManager.deleteChestFile(pName, vChest.getName());
				PlayerChests pChests = allChests.get(pName);
				int index = pChests.names.indexOf(vChest.getName());
				pChests.names.remove(index);
				pChests.types.remove(index);
				workerLog.info(pName + " removed his chest : " + vChest.getName());
				if (pChests.names.size() != 0) {
					String newDefaultChest = pChests.names.get(0);
					if (defaultChests.containsValue(vChest)) {
						defaultChests.put(pName, getChest(player.getName(), newDefaultChest));
						fManager.createDefaultChest(pName, newDefaultChest);
					}
					if (sendReceiveChests.containsValue(vChest)) {
						sendReceiveChests.put(pName, defaultChests.get(pName));
						fManager.createSendReceiveChest(pName, newDefaultChest);
					}
				} else {
					defaultChests.remove(pName);
					sendReceiveChests.remove(pName);
					chests.remove(pName);
					allChests.remove(pName);
					workerLog.info(pName + " has no more chest.");
					fManager.removePlayer(pName);
				}
				vChest = null;
				return true;
			}
			return false;
		}
		return false;
	}

	/**
	 * Rename a chest
	 * 
	 * @param player
	 * @param oldName
	 * @param newName
	 */
	public void renameChest(Player player, String oldName, String newName) {
		String playerName = player.getName();
		VirtualChest v = getChest(playerName, oldName);
		if (v != null) {
			v.setName(newName);
			chests.get(playerName).remove(oldName);
			chests.get(playerName).put(newName, v);
			fManager.renameChestFile(playerName, oldName, newName);
			PlayerChests pChest = allChests.get(playerName);
			int index = pChest.names.indexOf(oldName);
			pChest.names.remove(index);
			String type = pChest.types.get(index);
			pChest.types.remove(index);
			pChest.names.add(newName);
			pChest.types.add(type);
			if (defaultChests.containsValue(v))
				fManager.createDefaultChest(playerName, newName);
			if (sendReceiveChests.containsValue(v))
				fManager.createSendReceiveChest(playerName, newName);
			workerLog
					.info(playerName + " renamed his chest [" + oldName + "] to {" + newName + "}");
		}
	}

	/**
	 * Get a command represented by a specific class
	 * 
	 * @param clazz
	 * @return
	 */
	public GPCommand getCommand(Class<?> clazz) {
		for (GPCommand command : commands)
			if (command.getClass() == clazz)
				return command;

		return null;
	}

	/**
	 * @return the list of commands
	 */
	public List<GPCommand> getCommands() {
		return commands;
	}

	/**
	 * Save all the chests.
	 */
	public synchronized void save() {
		this.fManager.savePerPlayer(chests);
		workerLog.info("Chests Saved !");
	}

	/**
	 * Save the parties
	 */
	public synchronized void saveParties() {
		this.fManager.saveParties(parties, "parties.dat");
		workerLog.info("Parties Saved !");
	}

	/**
	 * load all the chests
	 */
	public synchronized void oldLoad() {
		this.config.load();
		this.fManager.loadChests("chests.dat", chests);
	}

	/**
	 * load all the chests
	 */
	public synchronized void newLoad() {
		this.config.load();
		allChests = fManager.getAllPlayerChestType();
		if (allChests == null) {
			allChests = new MapMaker().concurrencyLevel(8).makeMap();
			workerLog.info("No player files found");
		} else {
			for (Player p : GiftPost.getBukkitServer().getOnlinePlayers()) {
				String playerName = p.getName();
				if (allChests.containsKey(playerName)) {
					chests.put(playerName, fManager.getPlayerChests(playerName));
					workerLog.info("Chests owned by " + playerName + " loaded from file");
				}
			}

		}

	}

	/**
	 * load parties.
	 */
	public synchronized void loadParties() {
		this.config.load();
		HashMap<String, VirtualChest> loaded = this.fManager.loadParties("parties.dat");
		if (loaded != null) {
			parties.clear();
			parties.putAll(loaded);
		}
	}

	/**
	 * Transfer from an old save
	 * 
	 * @deprecated
	 */
	public synchronized void transfer() {
		ConcurrentMap<String, ConcurrentMap<String, VirtualChest>> loaded = this.fManager
				.transfer("chest.dat");
		if (loaded != null) {
			chests = loaded;
			TreeMap<String, String> tmp = fManager.getAllPlayerDefaultChest();
			if (tmp != null)
				for (String player : tmp.keySet())
					defaultChests.put(player, getChest(player, tmp.get(player)));
		}
	}

	/**
	 * Check the permissions
	 * 
	 * @param player
	 * @param perm
	 * @return boolean
	 */
	public boolean hasPerm(Player player, String perm) {
		return hasPerm(player, perm, true);
	}

	/**
	 * Check the permission with the possibility to disable the error msg
	 * 
	 * @param player
	 * @param perm
	 * @param errorMsg
	 * @return
	 */
	public boolean hasPerm(Player player, String perm, boolean errorMsg) {
		if (permission == null)
			return true;
		if (permission.has(player, perm)) {
			return true;
		} else {
			if (errorMsg)
				player.sendMessage(ChatColor.RED + "You don't have the Permissions to do that "
						+ ChatColor.BLUE + "(" + perm + ")");
			return false;
		}

	}

	/**
	 * Check if the command contain the flag
	 * 
	 * @param args
	 * @param checkFlag
	 * @return
	 */
	public boolean hasFlag(String[] args, String checkFlag) {
		if (args.length >= 1) {
			String flag = args[0].toLowerCase();
			return flag.equals(checkFlag) || flag.equals("-" + checkFlag);
		}
		return false;
	}

	/**
	 * iConomy plugin
	 * 
	 * @return
	 */
	public static Method getPayement() {
		return payementMethod;
	}

	/**
	 * Set iConomy Plugin
	 * 
	 * @param plugin
	 * @return
	 */
	public static boolean setPayementMethod(Method plugin) {
		if (payementMethod == null) {
			payementMethod = plugin;
		} else {
			return false;
		}
		return true;
	}

	/**
	 * Permission plugin
	 * 
	 * @return
	 */
	public static PermissionHandler getPermission() {
		return permission;
	}

	/**
	 * Set iConomy Plugin
	 * 
	 * @param plugin
	 * @return
	 */
	public static boolean setPermission(PermissionHandler plugin) {
		if (permission == null) {
			permission = plugin;
		} else {
			return false;
		}
		return true;
	}

	/**
	 * mcMMO plugin
	 * 
	 * @return
	 */
	public static mcMMO getmcMMO() {
		return mcMMO;
	}

	/**
	 * Set mcMMO Plugin
	 * 
	 * @param plugin
	 * @return
	 */
	public static boolean setmcMMO(mcMMO plugin) {
		if (mcMMO == null) {
			mcMMO = plugin;
		} else {
			return false;
		}
		return true;
	}

	/**
	 * Return all the parties (mcMMO) that have a virtual chest.
	 * 
	 * @return
	 */
	public HashMap<String, VirtualChest> getParties() {
		return parties;
	}

	/**
	 * 
	 * @return a keyset with all the name of the player owning a chest.
	 */
	public Set<String> getAllOwner() {
		return chests.keySet();
	}

	/**
	 * Convert the save to the new format
	 */
	public void convertSave() {
		oldLoad();
		fManager.savePerPlayer(chests);
		chests.clear();
		sendReceiveChests.clear();
		defaultChests.clear();
		allChests = fManager.getAllPlayerChestType();
		workerLog.info("Saves converted.");
	}

	/**
	 * Check if the plugin iConomy is present and if the player have enough
	 * money. After checked, substract the money.
	 * 
	 * @param gpw
	 * @param player
	 * @return
	 */
	public boolean economyCheck(Player player, String configParam) {
		if (GiftPostWorker.getPayement() != null
				&& this.getConfig().getString("iConomy", "false").matches("true")
				&& !this.hasPerm(player, "giftpost.admin.free", false)) {
			if (GiftPostWorker.getPayement().hasAccount(player.getName())) {
				if (!GiftPostWorker.getPayement().getAccount(player.getName())
						.hasEnough(this.getConfig().getDouble(configParam, 1.0))) {
					player.sendMessage(chestKeeper()
							+ ChatColor.RED
							+ "You don't have "
							+ GiftPostWorker.getPayement().format(
									this.getConfig().getDouble(configParam, 10.0))
							+ " to pay the Chests Keeper !");
					return false;
				} else {
					if (this.getConfig().getDouble(configParam, 1.0) != 0) {
						GiftPostWorker.getPayement().getAccount(player.getName())
								.subtract(this.getConfig().getDouble(configParam, 1.0));
						player.sendMessage(chestKeeper()
								+ " "
								+ GiftPostWorker.getPayement().format(
										this.getConfig().getDouble(configParam, 1.0))
								+ ChatColor.DARK_GRAY + " used to pay the Chests Keeper.");
					}
					return true;
				}

			} else {
				player.sendMessage(chestKeeper() + ChatColor.RED
						+ "You must have an account to pay the Chests Keeper !");
				return false;
			}
		}
		return true;
	}

	public boolean economyUpgradeCheck(Player player) {
		if (GiftPostWorker.getPayement() != null
				&& this.getConfig().getString("iConomy", "false").matches("true")
				&& !this.hasPerm(player, "giftpost.admin.free", false)) {
			double amount = this.getConfig().getDouble("iConomy-largeChest-price", 500.0)
					- this.getConfig().getDouble("iConomy-normalChest-price", 250.0);
			if (GiftPostWorker.getPayement().hasAccount(player.getName())) {
				if (!GiftPostWorker.getPayement().getAccount(player.getName()).hasEnough(amount)) {
					player.sendMessage(chestKeeper() + ChatColor.RED + "You don't have "
							+ GiftPostWorker.getPayement().format(amount)
							+ " to pay the Chests Keeper !");
					return false;
				} else {
					if (amount != 0) {
						GiftPostWorker.getPayement().getAccount(player.getName()).subtract(amount);
						player.sendMessage(chestKeeper() + " "
								+ GiftPostWorker.getPayement().format(amount) + ChatColor.DARK_GRAY
								+ " used to pay the Chests Keeper.");
					}
					return true;
				}

			} else {
				player.sendMessage(chestKeeper() + ChatColor.RED
						+ "You must have an account to pay the Chests Keeper !");
				return false;
			}
		}
		return true;
	}

	@SuppressWarnings("deprecation")
	public String getDefaultType(Player player) {
		String limit;
		limit = null;
		if (GiftPostWorker.getPermission() != null) {
			try {
				limit = GiftPostWorker.getPermission().getInfoString(player.getWorld().getName(),
						player.getName(), "giftpost.chestType", false);
			} catch (NoSuchMethodError e) {
				GiftPostWorker.workerLog.severe("Permissions Plugin is not uptodate.");
				limit = GiftPostWorker.getPermission().getPermissionString(
						player.getWorld().getName(), player.getName(), "giftpost.chestType");
			}
		}
		if (limit == null || limit.isEmpty())
			limit = GiftPostWorker.getInstance().getConfig().getString("chest-default", "normal");
		return limit;
	}
}
