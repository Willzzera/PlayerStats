package com.gmail.artemis.the.gr8.playerstats.statistic;

import com.gmail.artemis.the.gr8.playerstats.enums.StandardMessage;
import com.gmail.artemis.the.gr8.playerstats.enums.Target;
import com.gmail.artemis.the.gr8.playerstats.msg.components.ComponentUtils;
import com.gmail.artemis.the.gr8.playerstats.statistic.request.StatRequest;
import com.gmail.artemis.the.gr8.playerstats.msg.OutputManager;
import com.gmail.artemis.the.gr8.playerstats.reload.ReloadThread;
import com.gmail.artemis.the.gr8.playerstats.ThreadManager;
import com.gmail.artemis.the.gr8.playerstats.utils.MyLogger;
import net.kyori.adventure.text.TextComponent;
import org.jetbrains.annotations.Nullable;

import java.util.*;

/** The Thread that is in charge of getting and calculating statistics.*/
public final class StatThread extends Thread {

    private static OutputManager outputManager;
    private static StatRetriever statRetriever;

    private final ReloadThread reloadThread;
    private final StatRequest statRequest;

    public StatThread(OutputManager m, StatRetriever t, int ID, StatRequest s, @Nullable ReloadThread r) {
        outputManager = m;
        statRetriever = t;

        reloadThread = r;
        statRequest = s;

        this.setName("StatThread-" + statRequest.getCommandSender().getName() + "-" + ID);
        MyLogger.threadCreated(this.getName());
    }

    @Override
    public void run() throws IllegalStateException, NullPointerException {
        MyLogger.threadStart(this.getName());

        if (statRequest == null) {
            throw new NullPointerException("No statistic statRequest was found!");
        }
        if (reloadThread != null && reloadThread.isAlive()) {
            try {
                MyLogger.waitingForOtherThread(this.getName(), reloadThread.getName());
                outputManager.sendFeedbackMsg(statRequest.getCommandSender(), StandardMessage.STILL_RELOADING);
                reloadThread.join();

            } catch (InterruptedException e) {
                MyLogger.logException(e, "StatThread", "Trying to join " + reloadThread.getName());
                throw new RuntimeException(e);
            }
        }

        long lastCalc = ThreadManager.getLastRecordedCalcTime();
        if (lastCalc > 2000) {
            outputManager.sendFeedbackMsgWaitAMoment(statRequest.getCommandSender(), lastCalc > 20000);
        }

        Target selection = statRequest.getTarget();
        try {
            TextComponent statResult = switch (selection) {
                case PLAYER -> outputManager.formatPlayerStat(statRequest, statRetriever.getPlayerStat(statRequest));
                case TOP -> outputManager.formatTopStat(statRequest, statRetriever.getTopStats(statRequest));
                case SERVER -> outputManager.formatServerStat(statRequest, statRetriever.getServerStat(statRequest));
            };
            if (statRequest.isAPIRequest()) {
                String msg = ComponentUtils.getTranslatableComponentSerializer()
                        .serialize(statResult);
                statRequest.getCommandSender().sendMessage(msg);
            }
            else {
                outputManager.sendToCommandSender(statRequest.getCommandSender(), statResult);
            }
        }
        catch (ConcurrentModificationException e) {
            if (!statRequest.isConsoleSender()) {
                outputManager.sendFeedbackMsg(statRequest.getCommandSender(), StandardMessage.UNKNOWN_ERROR);
            }
        }
    }
}