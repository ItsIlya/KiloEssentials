package org.kilocraft.essentials.craft;

import org.kilocraft.essentials.craft.newconfig.KiloFile;

import java.util.HashMap;

public class ConfigFilesHandler {
    public static ConfigFilesHandler INSTANCE;

    private String currentDir = System.getProperty("user.dir");
    private String configDir = currentDir + "/config/";

    private HashMap<String, String> configFiles = new HashMap<String, String>(){{
        put("KiloEssentials.yaml", currentDir + "/");
        put("Messages.yaml", configDir);
    }};

    public void handle() {
        configFiles.forEach((name, path) -> {
            KiloFile file = new KiloFile(name, path);
            file.tryToLoad();

        });
    }
}
