package builder;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.MessageEmbed;

import java.time.OffsetDateTime;
import java.util.List;

public class MessageEmbedBuilder {
    public MessageEmbed getModelListEmbed(List<String> models){
        EmbedBuilder embedBuilder = new EmbedBuilder()
                .setTitle("Available Models")
                .setDescription("Models that are currently available for use in Stable Diffusion")
                .setTimestamp(OffsetDateTime.now());
        for (String model : models){
            embedBuilder.addField(model, " ", false);
        }
        return embedBuilder.build();
    }

}
