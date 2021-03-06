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

import static com.Balor.utils.Display.sendHelp;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.logging.Logger;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Server;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.PluginDescriptionFile;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

import VirtualChest.Manager.Permissions.PermParent;
import VirtualChest.Manager.Permissions.PermissionLinker;

import com.Balor.Listeners.DeathEntityListener;
import com.Balor.Listeners.GPPlayerListener;
import com.Balor.Listeners.PluginListener;
import com.Balor.Listeners.SignListener;
import com.Balor.Listeners.WorldGPListener;
import com.Balor.Tools.Configuration.File.ExtendedConfiguration;
import com.Balor.commands.Buy;
import com.Balor.commands.Chest;
import com.Balor.commands.ChestList;
import com.Balor.commands.EmptyChest;
import com.Balor.commands.GPCommand;
import com.Balor.commands.GiveItem;
import com.Balor.commands.Help;
import com.Balor.commands.RemoveChest;
import com.Balor.commands.Rename;
import com.Balor.commands.Send;
import com.Balor.commands.SetChest;
import com.Balor.commands.SetChestLimit;
import com.Balor.commands.Upgrade;
import com.Balor.commands.mcMMO.BuyPartyChest;
import com.Balor.commands.mcMMO.OpenPartyChest;
import com.Balor.party.Party;
import com.Balor.party.PartyManager;
import com.Balor.utils.threads.PartiesGarbageCollector;

/**
 * @author Balor
 */
public class GiftPost extends JavaPlugin {

	public static final Logger log = Logger.getLogger("Minecraft");
	private GiftPostWorker gpw;
	private static Server server = null;
	private PermissionLinker permLinker = PermissionLinker.getPermissionLinker("GiftPost");

	private void registerCommand(Class<?> clazz) {
		try {
			GPCommand command = (GPCommand) clazz.newInstance();
			GiftPostWorker.getInstance().getCommands().add(command);
			if (command.getPermName() != null)
				permLinker.addPermChild(command.getPermName());
		} catch (InstantiationException e) {
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			e.printStackTrace();
		}
	}

	private void registerCommands() {
		permLinker.addPermParent(new PermParent("giftpost.admin.*"));
		permLinker.addPermParent(new PermParent("giftpost.chest.*"));
		permLinker.setMajorPerm(new PermParent("giftpost.*"));
		permLinker.addPermChild("giftpost.chest.everywhere");
		permLinker.addPermChild("giftpost.admin.empty");
		permLinker.addPermChild("giftpost.admin.limit");
		permLinker.addPermChild("giftpost.admin.sign");
		permLinker.addPermChild("giftpost.admin.free");
		permLinker.addPermChild("giftpost.admin.item");
		permLinker.addPermChild("giftpost.admin.sendallusers");
		permLinker.addPermChild("giftpost.admin.open");

		registerCommand(Chest.class);
		registerCommand(Buy.class);
		registerCommand(Send.class);
		registerCommand(ChestList.class);
		registerCommand(EmptyChest.class);
		registerCommand(Help.class);
		if (!GiftPostWorker.getInstance().getConfig().getString("only-normal", "false")
				.equals("true"))
			registerCommand(Upgrade.class);
		registerCommand(SetChest.class);
		registerCommand(Rename.class);
		registerCommand(RemoveChest.class);
		registerCommand(SetChestLimit.class);
		registerCommand(OpenPartyChest.class);
		registerCommand(BuyPartyChest.class);
		registerCommand(GiveItem.class);
		permLinker.registerAllPermParent();

	}

	private void setupConfigFiles() {
		ExtendedConfiguration.setClassLoader(this.getClassLoader());
		if (!new File(getDataFolder().toString()).exists()) {
			new File(getDataFolder().toString()).mkdir();
		}
		File yml = new File(getDataFolder() + "/config.yml");
		if (!yml.exists()) {
			new File(getDataFolder().toString()).mkdir();
			try {
				yml.createNewFile();
			} catch (IOException ex) {
				System.out.println("cannot create file " + yml.getPath());
			}

			try {
				BufferedWriter out = new BufferedWriter(new FileWriter(yml, true));
				out.write("use-max-range: 'true'");
				out.newLine();
				out.write("max-range: 100");
				out.newLine();
				out.write("allow-offline: 'true'");
				out.newLine();
				out.write("message-of-the-day: 'true'");
				out.newLine();
				out.write("use-wand: 'true'");
				out.newLine();
				out.write("wand-item-id: " + Material.CHEST.getId());
				out.newLine();
				out.write("auto-save-time: 10");
				out.newLine();
				out.write("max-number-chest: 10");
				out.newLine();
				out.write("world-check: 'true'");
				out.newLine();
				out.write("iConomy: 'true'");
				out.newLine();
				out.write("iConomy-send-price: 1.0");
				out.newLine();
				out.write("iConomy-openchest-price: 1.0");
				out.newLine();
				out.write("iConomy-normalChest-price: 10.0");
				out.newLine();
				out.write("iConomy-largeChest-price: 20.0");
				out.newLine();
				out.write("auto-stack: 'true'");
				out.newLine();
				out.write("auto-sort: 'true'");
				out.newLine();
				out.write("only-normal: 'false'");
				out.newLine();
				out.write("only-sign: 'false'");
				out.newLine();
				out.write("chest-default: normal");
				out.newLine();
				out.write("drop-on-death: 'false'");
				out.newLine();

				// Close the output stream
				out.close();
			} catch (Exception e) {
				System.out.println("cannot write config file: " + e);
			}
		}

	}

	private void setupListeners() {
		PluginListener pluginListener = new PluginListener();
		GPPlayerListener pListener = new GPPlayerListener();
		SignListener sListener = new SignListener();
		WorldGPListener wListener = new WorldGPListener();
		DeathEntityListener deathListener = new DeathEntityListener();
		registerCommands();
		PluginManager pm = getServer().getPluginManager();
		pm.registerEvents(pluginListener, this);
		pm.registerEvents(pListener, this);
		pm.registerEvents(sListener, this);
		pm.registerEvents(wListener, this);
		pm.registerEvents(deathListener, this);
	}

	public static Server getBukkitServer() {
		return server;
	}

	@SuppressWarnings("deprecation")
	public void onEnable() {
		server = getServer();
		GiftPostWorker.setDisable(false);
		setupConfigFiles();
		log.info("[" + this.getDescription().getName() + "]" + " (version "
				+ this.getDescription().getVersion() + ")");
		gpw = GiftPostWorker.getInstance();
		gpw.setConfig(
				ExtendedConfiguration.loadConfiguration(new File(getDataFolder(), "config.yml")),
				this);
		gpw.setfManager(getDataFolder().toString());
		setupListeners();
		if (new File(getDataFolder() + File.separator + "chest.dat").exists()) {
			gpw.transfer();
			new File(getDataFolder() + File.separator + "chest.dat").delete();
		} else if (new File(getDataFolder() + File.separator + "chests.dat").exists()) {
			gpw.convertSave();
			new File(getDataFolder() + File.separator + "chests.dat").delete();
		} else
			gpw.newLoad();
		log.info("[" + this.getDescription().getName() + "] Chests loaded !");
		getServer().getScheduler().scheduleAsyncRepeatingTask(this, new Runnable() {

			public void run() {
				GiftPostWorker.getInstance().save();
			}
		}, (getConfig().getInt("auto-save-time", 10) * 1200) / 2,
				getConfig().getInt("auto-save-time", 10) * 1200);
		if (getServer().getPluginManager().getPlugin("mcMMO") != null) {
			GiftPostWorker.getInstance().loadParties();
			getServer().getScheduler().scheduleAsyncRepeatingTask(this,
					new PartiesGarbageCollector(),
					(getConfig().getInt("auto-save-time", 10) * 1200) / 2,
					getConfig().getInt("auto-save-time", 10) * 1200);
		}
		Party.setDirectory(new File(getDataFolder(), "Parties"));
		PartyManager.setPlugin(this);

	}

	public void onDisable() {
		if (com.Balor.utils.Downloader.pluginName == null) {
			PluginDescriptionFile pdfFile = this.getDescription();
			gpw.save();
			if (GiftPostWorker.getmcMMO() != null)
				gpw.saveParties();
			server.getScheduler().cancelTasks(this);
			GiftPostWorker.setDisable(true);
			GiftPostWorker.killInstance();
			log.info("[" + pdfFile.getName() + "]" + " Plugin Disabled. (version "
					+ pdfFile.getVersion() + ")");
		}

	}

	@Override
	public boolean onCommand(CommandSender sender, Command command, String commandLabel,
			String[] args) {
		if (!(sender instanceof Player)) {
			sender.sendMessage(ChatColor.RED + "You have to be a player!");
			return true;
		} else {
			String commandName = command.getName().toLowerCase();
			if (commandName.equals("partychest")) {
				gpw.getCommand(OpenPartyChest.class).execute(gpw, sender, null);
				return true;
			}

			if (args.length == 0) {
				sendHelp(sender, 1);
				return true;
			}

			int i = gpw.getCommands().size();
			for (GPCommand cmd : gpw.getCommands()) {
				if (!cmd.validate(gpw, sender, args)) {
					i--;
					continue;
				}
				try {
					cmd.execute(gpw, sender, args);
				} catch (Exception e) {
					log.info("A GiftPost command threw an exception!");
					log.info("Go here : http://forums.bukkit.org/threads/gen-mech-virtualchest-4-3-4-have-a-chest-with-you-everywhere-all-economy-permissions-1000.11695/");
					log.info("and post the content of this log + the content of plugins/VirtualChest/log.txt please, Thanks.");
					e.printStackTrace();
				}
			}
			if (i == 0)
				sendHelp(sender, 1);
		}
		return true;
	}
}
