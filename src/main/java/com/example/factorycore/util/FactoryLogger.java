package com.example.factorycore.util;

import net.neoforged.fml.loading.FMLPaths;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class FactoryLogger {
    private static File logFile;
    private static BufferedWriter writer;
    private static final DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss");

    public static void init() {
        try {
            File dir = FMLPaths.GAMEDIR.get().resolve("logs/factorycore").toFile();
            if (!dir.exists()) dir.mkdirs();
            logFile = new File(dir, "verbose.log");
            writer = new BufferedWriter(new FileWriter(logFile, false));
            log("SYSTEM", "FactoryCore Verbose Logger Initialized");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static synchronized void log(String category, String message) {
        if (writer == null) return;
        try {
            writer.write(String.format("[%s] [%s] %s", dtf.format(LocalDateTime.now()), category, message));
            writer.newLine();
            writer.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void power(String message) { log("POWER", message); }
    public static void machine(String message) { log("MACHINE", message); }
}
