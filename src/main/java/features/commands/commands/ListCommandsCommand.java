package features.commands.commands;

import features.commands.events.CommandEvent;
import model.entities.EnhancedGuild;
import net.dv8tion.jda.core.MessageBuilder;

import java.util.stream.Collectors;

public class ListCommandsCommand extends AbstractCommand {
	public ListCommandsCommand() {
		name = "commands";
		aliases = new String[]{"listcommands", "commandslist"};
		description = "Lists all enabled commands.";
	}
	
	@Override
	public void execute(CommandEvent event) {
		EnhancedGuild guild = event.getEnhancedGuild();
		MessageBuilder messageBuilder = new MessageBuilder("**Available commands**: `");
		messageBuilder.append(String.join("` `",
				event.getHandler().getCommands()
						.stream()
						.filter(abstractCommand -> !abstractCommand.isAdmin())
						.filter(abstractCommand -> !guild.getDisabledCommands().contains(abstractCommand))
						.map(AbstractCommand::getName)
						.collect(Collectors.toList())));
		messageBuilder.append("`\n Type `!!<command> help` to get basic help for any command.");
		event.getChannel().sendMessage(messageBuilder.build()).queue();
	}
}
