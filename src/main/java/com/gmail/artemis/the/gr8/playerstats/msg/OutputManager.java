package com.gmail.artemis.the.gr8.playerstats.msg;

import com.gmail.artemis.the.gr8.playerstats.Main;
import com.gmail.artemis.the.gr8.playerstats.ShareManager;
import com.gmail.artemis.the.gr8.playerstats.config.ConfigHandler;
import com.gmail.artemis.the.gr8.playerstats.enums.StandardMessage;
import com.gmail.artemis.the.gr8.playerstats.models.StatRequest;
import net.kyori.adventure.platform.bukkit.BukkitAudiences;
import net.kyori.adventure.text.TextComponent;
import org.bukkit.Statistic;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.jetbrains.annotations.NotNull;

import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.UUID;
import java.util.function.BiFunction;
import java.util.function.Function;

import static com.gmail.artemis.the.gr8.playerstats.enums.StandardMessage.*;

public class OutputManager {

    private static BukkitAudiences adventure;
    private static ShareManager shareManager;
    private static MessageWriter msg;
    private static ConsoleMessageWriter consoleMsg;

    private static EnumMap<StandardMessage, Function<MessageWriter, TextComponent>> standardMessages;

    public OutputManager(ConfigHandler conf) {
        adventure = Main.adventure();
        shareManager = ShareManager.getInstance(conf);

        msg = new MessageWriter(conf);
        consoleMsg = new ConsoleMessageWriter(conf);

        standardMessages = new EnumMap<>(StandardMessage.class);
        prepareFunctions();
    }

    public void updateComponentFactories(ConfigHandler config) {
        msg = new MessageWriter(config);
        consoleMsg = new ConsoleMessageWriter(config);
    }

    public void sendFeedbackMsg(CommandSender sender, StandardMessage message) {
        if (message != null) {
            adventure.sender(sender).sendMessage(standardMessages.get(message).apply(getWriter(sender)));
        }
    }

    public void sendFeedbackMsgWaitAMoment(CommandSender sender, boolean longWait) {
        adventure.sender(sender).sendMessage(getWriter(sender).waitAMoment(longWait));
    }

    public void sendFeedbackMsgMissingSubStat(CommandSender sender, Statistic.Type statType) {
        adventure.sender(sender).sendMessage(getWriter(sender).missingSubStatName(statType));
    }

    public void sendFeedbackMsgWrongSubStat(CommandSender sender, Statistic.Type statType, String subStatName) {
        if (subStatName == null) {
            sendFeedbackMsgMissingSubStat(sender, statType);
        } else {
            adventure.sender(sender).sendMessage(getWriter(sender).wrongSubStatType(statType, subStatName));
        }
    }

    public void sendExamples(CommandSender sender) {
        adventure.sender(sender).sendMessage(getWriter(sender).usageExamples());
    }

    public void sendHelp(CommandSender sender) {
        adventure.sender(sender).sendMessage(getWriter(sender).helpMsg());
    }

    public void shareStatResults(@NotNull TextComponent statResult) {
        adventure.players()
//                .filterAudience(onlinePlayer -> !onlinePlayer.get(Identity.NAME)
//                        .orElse("").equalsIgnoreCase(sender.getName()))
                .sendMessage(statResult);

    }

    public void sendPlayerStat(@NotNull StatRequest request, int playerStat) {
        CommandSender sender = request.getCommandSender();
        BiFunction<UUID, CommandSender, TextComponent> buildFunction =
                getWriter(sender).formattedPlayerStatFunction(playerStat, request);

        processAndSend(sender, buildFunction);
    }

    public void sendServerStat(@NotNull StatRequest request, long serverStat) {
        CommandSender sender = request.getCommandSender();
        BiFunction<UUID, CommandSender, TextComponent> buildFunction =
                getWriter(sender).formattedServerStatFunction(serverStat, request);

        processAndSend(sender, buildFunction);
    }

    public void sendTopStat(@NotNull StatRequest request, LinkedHashMap<String, Integer> topStats) {
        CommandSender sender = request.getCommandSender();
        BiFunction<UUID, CommandSender, TextComponent> buildFunction =
                getWriter(sender).formattedTopStatFunction(topStats, request);

        processAndSend(sender, buildFunction);
    }

    private void processAndSend(CommandSender sender, BiFunction<UUID, CommandSender, TextComponent> buildFunction) {
        if (shareManager.isEnabled() && shareManager.senderHasPermission(sender)) {

            UUID shareCode = shareManager.saveStatResult(sender.getName(), buildFunction.apply(null, sender));
            adventure.sender(sender).sendMessage(buildFunction.apply(shareCode, null));
        }
        else {
            adventure.sender(sender).sendMessage(buildFunction.apply(null, null));
        }
    }

    private void prepareFunctions() {
        standardMessages.put(RELOADED_CONFIG, (MessageWriter::reloadedConfig));
        standardMessages.put(STILL_RELOADING, (MessageWriter::stillReloading));
        standardMessages.put(MISSING_STAT_NAME, (MessageWriter::missingStatName));
        standardMessages.put(MISSING_PLAYER_NAME, (MessageWriter::missingPlayerName));
        standardMessages.put(REQUEST_ALREADY_RUNNING, (MessageWriter::requestAlreadyRunning));
        standardMessages.put(STILL_ON_SHARE_COOLDOWN, (MessageWriter::stillOnShareCoolDown));
        standardMessages.put(RESULTS_ALREADY_SHARED, (MessageWriter::resultsAlreadyShared));
        standardMessages.put(STAT_RESULTS_TOO_OLD, (MessageWriter::statResultsTooOld));
        standardMessages.put(UNKNOWN_ERROR, (MessageWriter::unknownError));
    }

    private MessageWriter getWriter(CommandSender sender) {
        return sender instanceof ConsoleCommandSender ? consoleMsg : msg;
    }
}