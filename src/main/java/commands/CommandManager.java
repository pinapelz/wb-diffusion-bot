package commands;

import builder.MessageEmbedBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import openai.OpenAIAPI;
import stablediff.StableDiffusionAPI;


import java.io.File;
import java.io.IOException;

public class CommandManager extends ListenerAdapter {
    private StableDiffusionAPI stableDiff;
    private OpenAIAPI gpt;
    private MessageEmbedBuilder msgEmbedBuilder;
    private final String ACKNOWLEDGED_REACTION = "\u2705";
    private long adminRole;

    public CommandManager(String stableDiffusionAPIURL, String openaiAPIURL, String openaiAPIKEY, long adminRole) {
        super();
        stableDiff = new StableDiffusionAPI(stableDiffusionAPIURL).setStepCount(20);
        gpt = new OpenAIAPI(openaiAPIKEY, openaiAPIURL);
        msgEmbedBuilder = new MessageEmbedBuilder();
        this.adminRole = adminRole;
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

    private void logCommand(String message){
        String timestamp = java.time.LocalDateTime.now().toString();
        System.out.println("[" + timestamp + "] " + message);
    }

    @Override
    public void onSlashCommand(SlashCommandEvent event) {
        logCommand("Received slash command: " + event.getName() + " from " + event.getUser().getAsTag());
        switch (event.getName()) {
            case "load-checkpoint":
                String modelName = event.getOption("name").getAsString();
                 runFunction(() -> stableDiff.setCheckpoint(modelName));
                 event.reply("Checkpoint " + modelName + " loaded").queue();
                break;
            case "set-steps":
                int stepSize = Integer.parseInt(event.getOption("size").getAsString());
                runFunction(() -> stableDiff.setStepCount(stepSize));
                event.reply("Step size set to " + stepSize).queue();
                break;
            case "generate":
                event.deferReply().queue();
                String prompt = event.getOption("prompt").getAsString();
                runFunction(() -> stableDiff.generateImage(prompt));
                event.getHook().sendFile(new File("output.png")).queue();
                break;
            case "show-checkpoints":
                event.deferReply().queue();
                event.getHook().sendMessageEmbeds(msgEmbedBuilder.getModelListEmbed(runFunction(() -> stableDiff.getAvailableModels()))).queue();
                break;
        }
    }

    @Override
    public void onMessageReceived(MessageReceivedEvent e) {
        JDA jda = e.getJDA();
        Message message = e.getMessage();
        String msg = message.getContentDisplay();
        String cmd = msg.split(" ")[0];
        // Chatting query
        if (msg.startsWith("@") && e.getMessage().getMentionedMembers().contains(e.getGuild().getSelfMember())){
            gpt.setPrompt(msg.substring(5));
            sendMessage(e, runFunction(() -> gpt.queryGPT()));
            return;
        }
        if ((!msg.startsWith("!")) || !e.getMember().isOwner()) return;
        logCommand("Received reg command: " + cmd + " from " + e.getAuthor().getAsTag());
        switch (cmd.substring(1)){
            case "gpt":
                gpt.setPrompt(msg.substring(5));
                sendMessage(e, runFunction(() -> gpt.queryGPT()));
                break;
            case "setheight":
                int height = Integer.parseInt(msg.split(" ")[1]);
                runFunction(() -> stableDiff.setHeight(height));
                e.getMessage().addReaction(ACKNOWLEDGED_REACTION).queue();
                break;
            case "setwidth":
                int width = Integer.parseInt(msg.split(" ")[1]);
                runFunction(() -> stableDiff.setWidth(width));
                e.getMessage().addReaction(ACKNOWLEDGED_REACTION).queue();
                break;
        }
    }


}


