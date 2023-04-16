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
        System.out.println("Adding slash commands");
        jda.upsertCommand(new CommandData(
                "show-models", "Shows the currently available models loaded in Stable Diffusion"))
                .queue();
        jda.upsertCommand(new CommandData(
                "generate", "Manually generates an image with the given prompt"))
                .addOption(OptionType.STRING, "prompt", "The given prompt", true)
        .queue();
    }

    public void purgeSlashCommands() {
        System.out.println("Purging all slash commands");
        jda.retrieveCommands().queue(commands -> {
            for (int i = 0; i < commands.size(); i++) {
                commands.get(i).delete().queue();
            }
        });
    }


}
