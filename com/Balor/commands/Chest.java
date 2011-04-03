/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.Balor.commands;

import com.Balor.bukkit.GiftPost.GiftPostWorker;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 *
 * @author Antoine
 */
public class Chest implements GPCommand
{

    /**
     * Execute commands
     * @param gp
     * @param sender
     * @param args
     */
    public void execute(GiftPostWorker gpw, CommandSender sender, String[] args)
    {
        gpw.getChest((Player) sender).openChest((Player)sender);
    }

    /**
     * Validate a command to check if it should be executed
     *
     * @param lwc
     * @param command
     * @param args
     * @return
     */
    public boolean validate(GiftPostWorker gpw, CommandSender sender, String[] args)
    {
        return (gpw.hasFlag(args, "c") || gpw.hasFlag(args, "chest")) && gpw.hasPerm((Player) sender, getPermName());
    }

    /**
     * @return the name of the perm to add in the permFile.
     */
    public String getPermName()
    {
        return "giftpost.chest.open";
    }
}