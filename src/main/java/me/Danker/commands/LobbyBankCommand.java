package me.Danker.commands;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import me.Danker.config.ModConfig;
import me.Danker.handlers.APIHandler;
import me.Danker.utils.Utils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.network.NetworkPlayerInfo;
import net.minecraft.command.CommandBase;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.EnumChatFormatting;

import java.text.NumberFormat;
import java.util.*;
import java.util.stream.Collectors;

public class LobbyBankCommand extends CommandBase {

    public static Thread mainThread = null;

    @Override
    public String getCommandName() {
        return "lobbybank";
    }

    @Override
    public String getCommandUsage(ICommandSender sender) {
        return "/" + getCommandName();
    }

    public static String usage(ICommandSender arg0) {
        return new LobbyBankCommand().getCommandUsage(arg0);
    }

    @Override
    public int getRequiredPermissionLevel() {
        return 0;
    }

    @Override
    public void processCommand(ICommandSender sender, String[] args) throws CommandException {
        EntityPlayer playerSP = (EntityPlayer) sender;
        Map<String, Double> unsortedBankList = new HashMap<>();
        ArrayList<Double> lobbyBanks = new ArrayList<>();
        // Check key
        String key = ModConfig.apiKey;
        if (key.equals("")) {
            playerSP.addChatMessage(new ChatComponentText(ModConfig.getColour(ModConfig.errorColour) + "API key not set. Use /setkey."));
            return;
        }

        mainThread = new Thread(() -> {
            try {
                // Create deep copy of players to prevent passing reference and ConcurrentModificationException
                Collection<NetworkPlayerInfo> players = new ArrayList<>(Minecraft.getMinecraft().getNetHandler().getPlayerInfoMap());
                playerSP.addChatMessage(new ChatComponentText(ModConfig.getColour(ModConfig.mainColour) + "Checking bank of lobby. Estimated time: " + (int) (Utils.getMatchingPlayers("").size() * 1.2 + 1) + " seconds."));
                // Send request every .6 seconds, leaving room for another 20 requests per minute

                for (final NetworkPlayerInfo player : players) {
                    if (player.getGameProfile().getName().startsWith("!")) continue;
                    // Manually get latest profile to use reduced requests on extra achievement API
                    String UUID = player.getGameProfile().getId().toString().replaceAll("-", "");
                    int profileIndex = -1;
                    Thread.sleep(600);
                    JsonObject profileResponse = APIHandler.getResponse("https://api.hypixel.net/skyblock/profiles?uuid=" + UUID + "&key=" + key, true);
                    if (!profileResponse.get("success").getAsBoolean()) {
                        String reason = profileResponse.get("cause").getAsString();
                        System.out.println("User " + player.getGameProfile().getName() + " failed with reason: " + reason);
                        continue;
                    }
                    if (profileResponse.get("profiles").isJsonNull()) continue;

                    JsonArray profiles = profileResponse.get("profiles").getAsJsonArray();
                    for (int i = 0; i < profiles.size(); i++) {
                        JsonObject profile = profiles.get(i).getAsJsonObject();
                        if (profile.get("selected").getAsBoolean()) {
                            profileIndex = i;
                            break;
                        }
                    }
                    if (profileIndex == -1) continue;

                    JsonObject latestProfile = profiles.get(profileIndex).getAsJsonObject().get("members").getAsJsonObject().get(UUID).getAsJsonObject();
                    boolean hasBanking = profiles.get(profileIndex).getAsJsonObject().has("banking");

                    double coin_purse;
                    // Add bank to lobby banks
                    // Put bank in HashMap

                    if (latestProfile.has("coin_purse")) {
                        coin_purse = latestProfile.get("coin_purse").getAsDouble();
                        if (hasBanking) {
                            coin_purse += profiles.get(profileIndex).getAsJsonObject().get("banking").getAsJsonObject().get("balance").getAsDouble();

                        }

                        unsortedBankList.put(player.getGameProfile().getName(), coin_purse); // Put bank in HashMap
                        lobbyBanks.add(coin_purse); // Add bank to lobby banks

                    }
                }

                // Sort coins
                Map<String, Double> sortedBankList = unsortedBankList.entrySet().stream()
                        .sorted(Map.Entry.<String, Double>comparingByValue().reversed())
                        .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue,
                                (e1, e2) -> e1, LinkedHashMap::new));

                String[] sortedBankListKeys = sortedBankList.keySet().toArray(new String[0]);
                String top3 = "";
                NumberFormat nf = NumberFormat.getIntegerInstance(Locale.US);
                for (int i = 0; i < 3 && i < sortedBankListKeys.length; i++) {
                    top3 += "\n " + EnumChatFormatting.AQUA + sortedBankListKeys[i] + ": " + ModConfig.getColour(ModConfig.skillAverageColour) + EnumChatFormatting.BOLD + nf.format(Math.round(sortedBankList.get(sortedBankListKeys[i])));
                }

                // Get lobby bank
                double lobbyBank = 0;
                for (Double playerBanks : lobbyBanks) {
                    lobbyBank += playerBanks;
                }
                lobbyBank = (double) Math.round((lobbyBank / lobbyBanks.size()) * 100) / 100;

                // Finally say bank lobby avg and highest bank users
                playerSP.addChatMessage(new ChatComponentText(ModConfig.getDelimiter() + "\n" +
                        ModConfig.getColour(ModConfig.typeColour) + " Lobby Bank Average: " + ModConfig.getColour(ModConfig.skillAverageColour) + EnumChatFormatting.BOLD + nf.format(Math.round(lobbyBank)) + "\n" +
                        ModConfig.getColour(ModConfig.typeColour) + " Highest Bank Averages:" + top3 + "\n" +
                        ModConfig.getDelimiter()));


            } catch (InterruptedException ex) {
                System.out.println("Current bank average list: " + unsortedBankList);
                Thread.currentThread().interrupt();
                System.out.println("Interrupted /lobbybank thread.");
            }

        });
        mainThread.start();
    }
}