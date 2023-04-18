import commands.CommandManager;
import commands.StatusHandler;
import fileutils.FileDataProcessor;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import okhttp3.OkHttpClient;

import javax.security.auth.login.LoginException;
import java.util.logging.Level;
import java.util.logging.Logger;


public class Main extends ListenerAdapter{
    private JDA jda;
    private JDABuilder jdaBuilder;
    private FileDataProcessor fileDataProcessor;
    private CommandManager commandManager;
    private StatusHandler statusHandler;
    private long adminRoleId;

    public void initializeBot(){
        Logger.getLogger(OkHttpClient.class.getName()).setLevel(Level.FINE);
        String openAIAPIURL = fileDataProcessor.getField("OpenAIAPIURL");
        String openAIAPIKEY = fileDataProcessor.getField("OpenAIAPIKEY");
        String stableDiffusionAPIURL = fileDataProcessor.getField("StableDiffusionAPIURL");
        fileDataProcessor = new FileDataProcessor();
        adminRoleId = Long.parseLong(fileDataProcessor.getField("adminRole"));
        commandManager = new CommandManager(stableDiffusionAPIURL, openAIAPIURL, openAIAPIKEY ,adminRoleId);
        jdaBuilder = JDABuilder.createDefault(fileDataProcessor.getField("discordToken"));
        jdaBuilder.addEventListeners(commandManager);
        jdaBuilder.addEventListeners(this);
        try {
            jda = jdaBuilder.build();
        }
        catch (LoginException e) {
            System.out.println("[Initialization Error] Unable to login with the provided token. Please check your token and try again.");
            throw new RuntimeException(e);
        }
        statusHandler = new StatusHandler(jda);

    }

    @Override
    public void onReady(net.dv8tion.jda.api.events.ReadyEvent event) {
        System.out.println("[Initialization] Logged in as " + event.getJDA().getSelfUser().getAsTag());
        statusHandler = new StatusHandler(jda);
        //statusHandler.purgeSlashCommands();
        statusHandler.updateSlashCommands();
        System.out.println("[Initialization] Bot is ready!");
    }
    public static void main(String args[]) {
        Main main = new Main();
        main.initializeBot();
    }
}

