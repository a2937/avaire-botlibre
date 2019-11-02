
package com.a2937.botlibre;

import com.a2937.botlibre.service.ai.BotMakerService;
import com.avairebot.plugin.JavaPlugin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BotMakerMain extends JavaPlugin {

    public static final Logger LOGGER = LoggerFactory.getLogger(BotMakerMain.class);



    @Override
    public void onEnable() {
        saveDefaultConfig();
        reloadConfig();
        getAvaire().getIntelligenceManager().registerService(new BotMakerService(this));
    }

    public String getChatAppId()
    {
        return getConfig().getString("botAppId", "invalid");
    }

    public String getChatBotInstanceId()
    {
        return getConfig().getString("botInstance", "invalid");
    }
}
