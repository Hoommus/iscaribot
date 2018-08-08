package features.commands.commands;

import config.BotConfig;
import features.commands.events.CommandEvent;
import model.dbfields.BotMessages;

import java.util.HashMap;
import java.util.Map;

public class VersionCommand extends AbstractCommand {
	
	public VersionCommand() {
		this.name = "version";
		this.aliases = new String[]{"ver", "v"};
		this.description = "Prints bot version";
	}
	
	public VersionCommand(String v) {
		this.name = "version";
		this.aliases = new String[]{"ver", "v"};
		this.description = "Prints bot version";
	}
	
	@Override
	public void execute(CommandEvent event) {
		Map<String, String> keys = Map.of("version", BotConfig.getVersion());
		if(BotConfig.isTest())
			event.getChannel().sendMessage(event.getEnhancedGuild().getMessage(BotMessages.VERSION_TEXT_TEST, keys)).queue();
		else
			event.getChannel().sendMessage(event.getEnhancedGuild().getMessage(BotMessages.VERSION_TEXT, keys)).queue();
	}
}
