package commands;

import builder.MessageEmbedBuilder;
import datatypes.Persona;
import llm.LargeLanguageModelAPI;
import llm.ooba.OobaAPI;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import llm.openai.OpenAIAPI;
import stablediff.StableDiffusionAPI;


import java.io.File;
import java.io.IOException;

public class CommandManager extends ListenerAdapter {
    private StableDiffusionAPI stableDiff;
    private LargeLanguageModelAPI llmAI;
    private MessageEmbedBuilder msgEmbedBuilder;
    private final String ACKNOWLEDGED_REACTION = "\u2705";
    private final String DENIED_REACTION = "\u274E";
    private long adminRole;

    public CommandManager(String stableDiffusionAPIURL, String openaiAPIURL, String openaiAPIKEY, String oobaAPIURL, long adminRole) {
        super();
        stableDiff = new StableDiffusionAPI(stableDiffusionAPIURL).setStepCount(20);
        llmAI = new OpenAIAPI(openaiAPIKEY, openaiAPIURL);
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
            System.out.println("Check that the file exists and that you have permission to read it!");
        }
        catch (Exception e){
            System.out.println("[" + timestamp + "] An unhanded exception occurred");
            e.printStackTrace();
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
            case "generate-random-waifu":
                event.deferReply().queue();
                llmAI.loadAnimeRandomGenerationPrompt();
                String waifuPrompt = runFunction(() -> llmAI.query(llmAI.INSTRUCT_RESPONSE));
                runFunction(() -> stableDiff.generateImage(waifuPrompt));
                event.getHook().sendFile(new File("output.png")).queue();
                break;
            case "persona":
                if(!checkAdminRole(event)) return;
                String personaName = event.getOption("name").getAsString();
                runFunction(() -> llmAI.setPersona(new Persona("personas/" + personaName + ".json")));
                event.reply("Persona set to " + personaName).queue();
                break;
            case "gpt-instruct":
                event.deferReply().queue();
                String instruction = event.getOption("prompt").getAsString();
                llmAI.setInstructPrompt(instruction);
                String instructionResponse = runFunction(() -> llmAI.query(llmAI.INSTRUCT_RESPONSE));
                event.getHook().sendMessage(instructionResponse).queue();
                break;
            case "ask-persona":
                event.deferReply().queue();
                llmAI.setPrompt(event.getOption("prompt").getAsString());
                String gptResponse = runFunction(() -> llmAI.query(llmAI.CHAT_RESPONSE));
                if(gptResponse == null) {
                    event.getHook().sendMessage("An error has occurred. Check that a persona has been loaded").queue();
                    return;
                }
                event.getHook().sendMessage(gptResponse).queue();
                break;
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
                runFunction(() -> stableDiff.generateImage(event.getOption("prompt").getAsString()));
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
        if (msg.startsWith("@") && e.getMessage().getMentionedMembers().contains(e.getGuild().getSelfMember())){
            e.getMessage().addReaction(ACKNOWLEDGED_REACTION).queue();
            msg = msg.replaceAll("@\\w+", "");
            llmAI.setPrompt(msg);
            String llmResponse = runFunction(() -> llmAI.query(llmAI.CHAT_RESPONSE));
            if (llmResponse == null) {
                e.getMessage().addReaction(DENIED_REACTION).queue();
                return;
            }
            llmAI.setAppearenceGenPrompts(llmResponse);
            String appearanceGenResponse = runFunction(() -> llmAI.query(llmAI.APPEARANCE_GEN_RESPONSE));
            runFunction(() -> stableDiff.generateImage(appearanceGenResponse));
            sendMessage(e, llmResponse);
            e.getChannel().sendFile(new File("output.png")).queue();
            return;
        }
        if ((!msg.startsWith("!")) || !e.getMember().isOwner()) return;
        logCommand("Received reg command: " + cmd + " from " + e.getAuthor().getAsTag());
        switch (cmd.substring(1)){
            case "dev":
                e.getMessage().addReaction(ACKNOWLEDGED_REACTION).queue();
                String msgs = msg.replaceAll("!dev","");
                llmAI.setPrompt(msgs);
                e.getChannel().sendMessage(runFunction(() -> llmAI.query(msgs))).queue();
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

    private boolean checkAdminRole(SlashCommandEvent event){
        if (!event.getMember().getRoles().contains(event.getGuild().getRoleById(adminRole))) {
            event.reply("You do not have permission to use this command").queue();
            return false;
        }
        return true;
    }


}


