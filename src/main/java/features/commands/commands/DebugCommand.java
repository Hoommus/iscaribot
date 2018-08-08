package features.commands.commands;

import config.BotConfig;
import features.commands.annotations.SubCommand;
import features.commands.events.CommandEvent;
import net.dv8tion.jda.core.MessageBuilder;
import net.dv8tion.jda.core.Permission;

import java.util.Arrays;

public class DebugCommand extends AbstractCommand {
	public DebugCommand() {
		this.name = "debug";
		this.description = "Debugs bugs.";
	}
	
	@Override
	public void execute(CommandEvent event) {
		if(!event.getMember().getUser().getId().equals(BotConfig.getAuthorID()))
			return;
		MessageBuilder builder = new MessageBuilder("Debug commands available only to bot author:");
		Arrays.stream(this.getClass().getMethods())
				.map(method -> String.join(" ", method.getAnnotation(SubCommand.class).subCommand()))
				.forEach(methodName -> builder.append("\n").append(methodName));
		event.getChannel().sendMessage(builder.build()).queue();
	}
}
