package me.Danker.commands;

import com.google.gson.JsonObject;
import me.Danker.config.ModConfig;
import me.Danker.handlers.APIHandler;
import me.Danker.utils.Utils;
import net.minecraft.command.CommandBase;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.BlockPos;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.EnumChatFormatting;

import java.text.NumberFormat;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

public class SlayerCommand extends CommandBase {

	@Override
	public String getCommandName() {
		return "slayer";
	}

	@Override
	public List<String> getCommandAliases() {
		return Collections.singletonList("slayers");
	}

	@Override
	public String getCommandUsage(ICommandSender arg0) {
		return "/" + getCommandName() + " [name]";
	}

	public static String usage(ICommandSender arg0) {
		return new SlayerCommand().getCommandUsage(arg0);
	}

	@Override
	public int getRequiredPermissionLevel() {
		return 0;
	}
	
	@Override
	public List<String> addTabCompletionOptions(ICommandSender sender, String[] args, BlockPos pos) {
		if (args.length == 1) {
			return Utils.getMatchingPlayers(args[0]);
		}
		return null;
	}
	
	@Override
	public void processCommand(ICommandSender arg0, String[] arg1) throws CommandException {
		// MULTI THREAD DRIFTING
		new Thread(() -> {
			EntityPlayer player = (EntityPlayer) arg0;
			
			// Check key
			String key = ModConfig.apiKey;
			if (key.equals("")) {
				player.addChatMessage(new ChatComponentText(ModConfig.getColour(ModConfig.errorColour) + "API key not set. Use /setkey."));
				return;
			}
			
			// Get UUID for Hypixel API requests
			String username;
			String uuid;
			if (arg1.length == 0) {
				username = player.getName();
				uuid = player.getUniqueID().toString().replaceAll("[\\-]", "");
				player.addChatMessage(new ChatComponentText(ModConfig.getColour(ModConfig.mainColour) + "Checking slayer of " + ModConfig.getColour(ModConfig.secondaryColour) + username));
			} else {
				username = arg1[0];
				player.addChatMessage(new ChatComponentText(ModConfig.getColour(ModConfig.mainColour) + "Checking slayer of " + ModConfig.getColour(ModConfig.secondaryColour) + username));
				uuid = APIHandler.getUUID(username);
			}
			
			// Find stats of latest profile
			String latestProfile = APIHandler.getLatestProfileID(uuid, key);
			if (latestProfile == null) return;
			
			String profileURL = "https://api.hypixel.net/skyblock/profile?profile=" + latestProfile + "&key=" + key;
			System.out.println("Fetching profile...");
			JsonObject profileResponse = APIHandler.getResponse(profileURL, true);
			if (!profileResponse.get("success").getAsBoolean()) {
				String reason = profileResponse.get("cause").getAsString();
				player.addChatMessage(new ChatComponentText(ModConfig.getColour(ModConfig.errorColour) + "Failed with reason: " + reason));
				return;
			}
			
			System.out.println("Fetching slayer stats...");
			JsonObject slayersObject = profileResponse.get("profile").getAsJsonObject().get("members").getAsJsonObject().get(uuid).getAsJsonObject().get("slayer_bosses").getAsJsonObject();
			// Zombie
			int zombieXP = 0;
			if (slayersObject.get("zombie").getAsJsonObject().has("xp")) {
				zombieXP = slayersObject.get("zombie").getAsJsonObject().get("xp").getAsInt();
			}
			// Spider
			int spiderXP = 0;
			if (slayersObject.get("spider").getAsJsonObject().has("xp")) {
				spiderXP = slayersObject.get("spider").getAsJsonObject().get("xp").getAsInt();
			}
			// Wolf
			int wolfXP = 0;
			if (slayersObject.get("wolf").getAsJsonObject().has("xp")) {
				wolfXP = slayersObject.get("wolf").getAsJsonObject().get("xp").getAsInt();
			}
			// Enderman
			int endermanXP = 0;
			if (slayersObject.get("enderman").getAsJsonObject().has("xp")) {
				endermanXP = slayersObject.get("enderman").getAsJsonObject().get("xp").getAsInt();
			}
			// Blaze
			int blazeXP = 0;
			if (slayersObject.get("blaze").getAsJsonObject().has("xp")) {
				blazeXP = slayersObject.get("blaze").getAsJsonObject().get("xp").getAsInt();
			}
			
			NumberFormat nf = NumberFormat.getIntegerInstance(Locale.US);
			player.addChatMessage(new ChatComponentText(ModConfig.getDelimiter() + "\n" +
														EnumChatFormatting.AQUA + " " + username + "'s Total XP: " + EnumChatFormatting.GOLD + EnumChatFormatting.BOLD + nf.format(zombieXP + spiderXP + wolfXP + endermanXP + blazeXP) + "\n" +
														ModConfig.getColour(ModConfig.typeColour) + " Zombie XP: " + ModConfig.getColour(ModConfig.valueColour) + EnumChatFormatting.BOLD + nf.format(zombieXP) + "\n" +
														ModConfig.getColour(ModConfig.typeColour) + " Spider XP: " + ModConfig.getColour(ModConfig.valueColour) + EnumChatFormatting.BOLD + nf.format(spiderXP) + "\n" +
														ModConfig.getColour(ModConfig.typeColour) + " Wolf XP: " + ModConfig.getColour(ModConfig.valueColour) + EnumChatFormatting.BOLD + nf.format(wolfXP) + "\n" +
														ModConfig.getColour(ModConfig.typeColour) + " Enderman XP: " + ModConfig.getColour(ModConfig.valueColour) + EnumChatFormatting.BOLD + nf.format(endermanXP) + "\n" +
														ModConfig.getColour(ModConfig.typeColour) + " Blaze XP: " + ModConfig.getColour(ModConfig.valueColour) + EnumChatFormatting.BOLD + nf.format(blazeXP) + "\n" +
														ModConfig.getDelimiter()));
			
		}).start();
	}

}
