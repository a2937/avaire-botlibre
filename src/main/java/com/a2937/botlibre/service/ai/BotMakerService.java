package com.a2937.botlibre.service.ai;

import com.avairebot.AvaIre;
import com.avairebot.chat.ConsoleColor;
import com.avairebot.contracts.ai.IntelligenceService;
import com.avairebot.factories.MessageFactory;
import com.avairebot.handlers.DatabaseEventHolder;
import com.avairebot.plugin.JavaPlugin;
import com.google.gson.JsonObject;
import net.dv8tion.jda.core.entities.Message;
import okhttp3.*;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


public class BotMakerService implements IntelligenceService
{



    private static final Logger log = LoggerFactory.getLogger(BotMakerService.class);

    public static final MediaType JSON
            = MediaType.parse("application/json; charset=utf-8");


    private final OkHttpClient client = new OkHttpClient();

    private String userId;

    private String userPassword;

    private String botMakerAppId;

    private String botInstance;

    private static final String actionOutput = ConsoleColor.format(
            "%cyanExecuting Intelligence Action %cyan\" for:"
                    + "\n\t\t%cyanUser:\t %author%"
                    + "\n\t\t%cyanServer:\t %server%"
                    + "\n\t\t%cyanChannel: %channel%"
                    + "\n\t\t%cyanMessage: %reset%message%"
                    + "\n\t\t%cyanResponse: %reset%response%"
    );

    private static final String propertyOutput = ConsoleColor.format(
            "%reset%s %cyan[%reset%s%cyan]"
    );

    private ExecutorService executor;

    private  JavaPlugin plugin;

    public BotMakerService(JavaPlugin plugin)
    {
        this.plugin = plugin;
    }

    @Override
    public boolean isEnabled()
    {
        return true;
    }

    @Override
    public void registerService(AvaIre avaIre)
    {

        botMakerAppId = plugin.getConfig().getString("botAppId", "invalid");
        botInstance = plugin.getConfig().getString("botInstance", "invalid");
        userId = plugin.getConfig().getString("userId","invalid");
        userPassword = plugin.getConfig().getString("userPassword","invalid");

        if( botInstance.equals("invalid") || botMakerAppId.equals("invalid"))
        {
            return;
        }


        executor = Executors.newFixedThreadPool(2);

    }

    @Override
    public void unregisterService(AvaIre avaIre) {

        if (executor != null) {
            executor.shutdownNow();
        }

    }

    @Override
    public void onMessage(Message message, DatabaseEventHolder databaseEventHolder)
    {
        executor.submit(() -> processRequest(message, databaseEventHolder));
    }

    private void processRequest(Message message, DatabaseEventHolder databaseEventHolder) {
        try
        {
            String[] split = message.getContentStripped().split(" ");
            String rawMessage = String.join(" ", Arrays.copyOfRange(split, 1, split.length));


            RequestBody body = RequestBody.create(JSON,generateRequestBody(rawMessage).toString());


            Request toSend = new Request.Builder()
                    .url("https://www.botlibre.com/rest/json/chat")
                    .addHeader("Content-Type", "application/json")
                    .post(body)
                    .build();

            Response response = client.newCall(toSend).execute();

            if (!response.isSuccessful()) {
                if(response.body() == null)
                {
                    MessageFactory.makeError(message, response.toString()).queue();
                }
                else
                {
                    MessageFactory.makeError(message, response.body().string()).queue();
                }
            }

            else
            {
                String json = response.body().string();
               JSONObject obj = new JSONObject(json);
                log.info(actionOutput
                                .replace("%author%", generateUsername(message))
                                .replace("%server%", generateServer(message))
                                .replace("%channel%", generateChannel(message))
                                .replace("%message%", message.getContentRaw())
                                .replace("%response%", response.message()));


                MessageFactory.makeInfo(message,obj.get("message").toString()).queue();
            }

        } catch (Exception e)
        {
            e.printStackTrace();
        }
    }

    private JsonObject generateRequestBody(String message)
    {
        JsonObject requestBody = new JsonObject();

        requestBody.addProperty("instance",botInstance);
        requestBody.addProperty("application",botMakerAppId);

        if(!userId.equals("invalid"))
        {
            requestBody.addProperty("user",userId);
        }

        if(!userPassword.equals("invalid"))
        {
            requestBody.addProperty("password",userPassword);
        }

        requestBody.addProperty("message",message);
        return requestBody;
    }

    private String generateUsername(Message message) {
        return String.format(propertyOutput,
                message.getAuthor().getName() + "#" + message.getAuthor().getDiscriminator(),
                message.getAuthor().getId()
        );
    }

    private String generateServer(Message message) {
        if (!message.getChannelType().isGuild()) {
            return ConsoleColor.GREEN + "PRIVATE";
        }

        return String.format(propertyOutput,
                message.getGuild().getName(),
                message.getGuild().getId()
        );
    }

    private CharSequence generateChannel(Message message) {
        if (!message.getChannelType().isGuild()) {
            return ConsoleColor.GREEN + "PRIVATE";
        }

        return String.format(propertyOutput,
                message.getChannel().getName(),
                message.getChannel().getId()
        );
    }
}
