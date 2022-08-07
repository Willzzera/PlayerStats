package com.gmail.artemis.the.gr8.playerstats;

import com.gmail.artemis.the.gr8.playerstats.config.ConfigHandler;
import com.gmail.artemis.the.gr8.playerstats.enums.StandardMessage;
import com.gmail.artemis.the.gr8.playerstats.statistic.request.StatRequest;
import com.gmail.artemis.the.gr8.playerstats.msg.OutputManager;
import com.gmail.artemis.the.gr8.playerstats.reload.ReloadThread;
import com.gmail.artemis.the.gr8.playerstats.statistic.StatRetriever;
import com.gmail.artemis.the.gr8.playerstats.statistic.StatThread;
import com.gmail.artemis.the.gr8.playerstats.utils.MyLogger;
import org.bukkit.command.CommandSender;

import java.util.HashMap;

/** The ThreadManager is in charge of the Threads that PlayerStats can utilize.
 It keeps track of past and currently active Threads, to ensure a Player cannot
 start multiple Threads at the same time (thereby limiting them to one stat-lookup at a time).
 It also passes appropriate references along to the {@link StatThread} or {@link ReloadThread},
 to ensure those will never run at the same time. */
public final class ThreadManager {

    private final static int threshold = 10;
    private int statThreadID;
    private int reloadThreadID;

    private static ConfigHandler config;
    private static OutputManager outputManager;
    private static StatRetriever statRetriever;

    private ReloadThread lastActiveReloadThread;
    private StatThread lastActiveStatThread;
    private final HashMap<String, Thread> statThreads;
    private static long lastRecordedCalcTime;

    public ThreadManager(ConfigHandler config, StatRetriever statRetriever, OutputManager outputManager) {
        ThreadManager.config = config;
        ThreadManager.outputManager = outputManager;
        ThreadManager.statRetriever = statRetriever;

        statThreads = new HashMap<>();
        statThreadID = 0;
        reloadThreadID = 0;
        lastRecordedCalcTime = 0;

        startReloadThread(null);
    }

    public static int getTaskThreshold() {
        return threshold;
    }

    public void startReloadThread(CommandSender sender) {
        if (lastActiveReloadThread == null || !lastActiveReloadThread.isAlive()) {
            reloadThreadID += 1;

            lastActiveReloadThread = new ReloadThread(config, outputManager, reloadThreadID, lastActiveStatThread, sender);
            lastActiveReloadThread.start();
        }
        else {
            MyLogger.threadAlreadyRunning(lastActiveReloadThread.getName());
        }
    }

    public void startStatThread(StatRequest statRequest) {
        statThreadID += 1;
        String cmdSender = statRequest.getCommandSender().getName();

        if (config.limitStatRequests() && statThreads.containsKey(cmdSender)) {
            Thread runningThread = statThreads.get(cmdSender);
            if (runningThread.isAlive()) {
                outputManager.sendFeedbackMsg(statRequest.getCommandSender(), StandardMessage.REQUEST_ALREADY_RUNNING);
            } else {
                startNewStatThread(statRequest);
            }
        } else {
            startNewStatThread(statRequest);
        }
    }

    /** Store the duration in milliseconds of the last top-stat-lookup
     (or of loading the offline-player-list if no look-ups have been done yet). */
    public static void recordCalcTime(long time) {
        lastRecordedCalcTime = time;
    }

    /** Returns the duration in milliseconds of the last top-stat-lookup
     (or of loading the offline-player-list if no look-ups have been done yet). */
    public static long getLastRecordedCalcTime() {
        return lastRecordedCalcTime;
    }

    private void startNewStatThread(StatRequest statRequest) {
        lastActiveStatThread = new StatThread(outputManager, statRetriever, statThreadID, statRequest, lastActiveReloadThread);
        statThreads.put(statRequest.getCommandSender().getName(), lastActiveStatThread);
        lastActiveStatThread.start();
    }
}