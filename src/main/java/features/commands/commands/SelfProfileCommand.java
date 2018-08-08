package features.commands.commands;

import features.commands.annotations.SubCommand;
import features.commands.events.CommandEvent;

public class SelfProfileCommand extends AbstractCommand {
	public SelfProfileCommand() {
		name = "self";
		aliases = new String[]{"profile"};
		description = "Provides some config for your own profile.";
	}
	
	@Override
	public void execute(CommandEvent event) {
	
	}
	
	@SubCommand(subCommand = "bd")
	public void birthday(CommandEvent event) {
	
	}
}
