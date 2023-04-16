package commands;

import builder.MessageEmbedBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import stablediff.StableDiffusionAPI;


import java.io.File;
import java.io.IOException;
import java.util.List;

public class CommandManager extends ListenerAdapter {
    private StableDiffusionAPI stablediff;
    private MessageEmbedBuilder msgEmbedBuilder;
    private int stepCount;

    public CommandManager(String stableDiffusionAPIURL) {
        super();
        stablediff = new StableDiffusionAPI(stableDiffusionAPIURL).setStepCount(20);
        msgEmbedBuilder = new MessageEmbedBuilder();
    }

    public interface Callback<T> {
        T run() throws IOException;
    }

    public static <T> T runFunction(Callback<T> function){
        String timestamp = java.time.LocalDateTime.now().toString();
        System.out.println("[" + timestamp + "] Running " + function.getClass().getName());
        try {
            return function.run();
        }
        catch (IOException e) {
            System.out.println("An IO exception occurred: " + e.getMessage());
            System.out.println("It's probably because I can't open or fine a particular file!");
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
            case "generate":
                event.deferReply().queue();
                String prompt = event.getOption("prompt").getAsString();
                runFunction(() -> stablediff.generateImage(prompt));
                event.getHook().sendFile(new File("output.png")).queue();
                break;
            case "show-models":
                event.deferReply().queue();
                event.getHook().sendMessageEmbeds(msgEmbedBuilder.getModelListEmbed(runFunction(() -> stablediff.getAvailableModels()))).queue();
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
            case "setstep":
                try {
                    stepCount = Integer.parseInt(msg.split(" ")[1]);
                    stablediff.setStepCount(stepCount);
                    sendMessage(e, "Step count set to " + stepCount);
                }
                catch (Exception ex){
                    sendMessage(e, "Invalid step count");
                }
                break;
            default:
                sendMessage(e, "Unknown command");
                break;
        }
    }


}


