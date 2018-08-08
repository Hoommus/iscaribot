package features.commands.commands;

import features.commands.events.CommandEvent;

import java.util.Arrays;
import java.util.Map;

import static providers.TemplateProcessor.process;

public class HelpCommand extends AbstractCommand {
	public HelpCommand() {
		name = "help";
		aliases = new String[]{"h"};
		description = "there will be no help";
		baseResourceName = "help_command";
		hookLocalisation(Messages.class);
	}
	
	@Override
	public void execute(CommandEvent event) {
		final Map<String, String> messages = MESSAGES.get(event.getEnhancedGuild().getLocale());
		final Map<String, String> keys = Map.of("prefix", event.getEnhancedGuild().getCommandPrefix());

		String response = String.join("\n", Arrays.stream(Messages.values())
				.map(message -> process(messages.get(message.name()), keys))
				.toArray(String[]::new));

		event.getChannel().sendMessage(response).queue();
	}
	
	private enum Messages {
		AVATAR,
		COMMANDS,
		INVITE,
		PING,
		UPTIME,
		USER,
		VERIFY,
		VERSION
	}
}
