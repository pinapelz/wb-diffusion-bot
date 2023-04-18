package commands;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;

public class StatusHandler {
    JDA jda;

    public StatusHandler(JDA jda) {
        this.jda = jda;

    }

    public void updateSlashCommands() {
        System.out.println("[Initialization] Adding slash commands");
        jda.upsertCommand(new CommandData(
                "show-checkpoints", "Shows the currently available checkpoints loaded in Stable Diffusion"))
                .queue();
        jda.upsertCommand(new CommandData(
                "generate", "Manually generates an image with the given prompt"))
                .addOption(OptionType.STRING, "prompt", "The given prompt", true)
        .queue();
        jda.upsertCommand(new CommandData(
                "set-steps", "Set the sampling steps for the model"))
                .addOption(OptionType.INTEGER, "size", "The step size as an Integer", true)
                .queue();
        jda.upsertCommand(new CommandData(
                "load-checkpoint", "Load a checkpoint"))
                .addOption(OptionType.STRING, "name", "The name of the checkpoint", true)
                .queue();
        jda.upsertCommand(new CommandData(
                "gpt", "Generates a response for the currently loaded persona"))
                .addOption(OptionType.STRING, "prompt", "The given prompt", true)
                .queue();
    }

    public void purgeSlashCommands() {
        System.out.println("[Initialization] Purging all slash commands");
        jda.retrieveCommands().queue(commands -> {
            for (int i = 0; i < commands.size(); i++) {
                commands.get(i).delete().queue();
            }
        });
    }


}
