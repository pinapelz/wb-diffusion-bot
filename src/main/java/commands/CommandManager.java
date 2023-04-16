package commands;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import stablediff.StableDiffusionAPI;


import java.io.IOException;
import java.util.List;

public class CommandManager extends ListenerAdapter {
    private StableDiffusionAPI stablediff;

    public CommandManager(String stableDiffusionAPIURL) {
        super();
        stablediff = new StableDiffusionAPI(stableDiffusionAPIURL).setStepCount(20);
    }

    public interface Callback<T> {
        T run() throws IOException;
    }

    public static <T> T runFunction(Callback<T> function) {
        String timestamp = java.time.LocalDateTime.now().toString();
        System.out.println("[" + timestamp + "] Running " + function.getClass().getName());
        try {
            return function.run();
        }
        catch (IOException e) {
            e.printStackTrace();
        }
        catch (Exception e){
            System.out.println("[" + timestamp + "] An unhanded exception occurred");
            System.out.println(e.getMessage());
        }
        return null;
    }

    private void sendMessage(MessageReceivedEvent e, String message) {
        String timestamp = java.time.LocalDateTime.now().toString();
        System.out.println("[" + timestamp + "] Sending message: " + message);
        e.getChannel().sendMessage(message).queue();
    }

    @Override
    public void onSlashCommand(SlashCommandEvent event) {
        switch (event.getName()) {
            case "show-models":
                List<String> models = runFunction(stablediff::getAvailableModels);
                break;
        }
    }

    @Override
    public void onMessageReceived(MessageReceivedEvent e) {
        JDA jda = e.getJDA();
        Message message = e.getMessage();
        String msg = message.getContentDisplay();
        String cmd = msg.split(" ")[0];
        if (!msg.startsWith("!")) return;
        switch (cmd.substring(1)){
            default:
                sendMessage(e, "Unknown command");
                break;
        }
    }


}


