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

public class BankCommand extends CommandBase {

	@Override
	public String getCommandName() {
		return "bank";
	}
	
	@Override
	public List<String> getCommandAliases() {
        return Collections.singletonList("purse");
    }

	@Override
	public String getCommandUsage(ICommandSender arg0) {
		return "/" + getCommandName() + " [name]";
	}

	public static String usage(ICommandSender arg0) {
		return new BankCommand().getCommandUsage(arg0);
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
				player.addChatMessage(new ChatComponentText(EnumChatFormatting.RED + "API key not set. Use /setkey."));
				return;
			}
			
			// Get UUID for Hypixel API requests
			String username;
			String uuid;
			if (arg1.length == 0) {
				username = player.getName();
				uuid = player.getUniqueID().toString().replaceAll("[\\-]", "");
				player.addChatMessage(new ChatComponentText(ModConfig.getColour(ModConfig.mainColour) + "Checking coins of " + ModConfig.getColour(ModConfig.secondaryColour) + username));
			} else {
				username = arg1[0];
				player.addChatMessage(new ChatComponentText(ModConfig.getColour(ModConfig.mainColour) + "Checking coins of " + ModConfig.getColour(ModConfig.secondaryColour) + username));
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
				player.addChatMessage(new ChatComponentText(EnumChatFormatting.RED + "Failed with reason: " + reason));
				return;
			}
			
			System.out.println("Fetching bank + purse coins...");
			double purseCoins = profileResponse.get("profile").getAsJsonObject().get("members").getAsJsonObject().get(uuid).getAsJsonObject().get("coin_purse").getAsDouble();
			purseCoins = Math.floor(purseCoins * 100.0) / 100.0;
			NumberFormat nf = NumberFormat.getIntegerInstance(Locale.US);
			
			// Check for bank api
			if (profileResponse.get("profile").getAsJsonObject().has("banking")) {
				double bankCoins = profileResponse.get("profile").getAsJsonObject().get("banking").getAsJsonObject().get("balance").getAsDouble();
				bankCoins = Math.floor(bankCoins * 100.0) / 100.0;
				
				player.addChatMessage(new ChatComponentText(ModConfig.getDelimiter() + "\n" +
															EnumChatFormatting.AQUA + " " + username + "'s Coins:\n" +
															ModConfig.getColour(ModConfig.typeColour) + " Bank: " + EnumChatFormatting.GOLD + nf.format(bankCoins) + "\n" +
															ModConfig.getColour(ModConfig.typeColour) + " Purse: " + EnumChatFormatting.GOLD + nf.format(purseCoins) + "\n" +
															ModConfig.getColour(ModConfig.typeColour) + " Total: " + EnumChatFormatting.GOLD + nf.format(bankCoins + purseCoins) + "\n" +
															ModConfig.getDelimiter()));
			} else {
				player.addChatMessage(new ChatComponentText(ModConfig.getDelimiter() + "\n" +
															EnumChatFormatting.AQUA + " " + username + "'s Coins:\n" +
															ModConfig.getColour(ModConfig.typeColour) + " Bank: " + EnumChatFormatting.RED + "Bank API disabled.\n" +
															ModConfig.getColour(ModConfig.typeColour) + " Purse: " + EnumChatFormatting.GOLD + nf.format(purseCoins) + "\n" +
															ModConfig.getDelimiter()));
			}
		}).start();
	}

}
