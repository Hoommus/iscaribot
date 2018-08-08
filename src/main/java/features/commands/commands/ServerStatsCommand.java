package features.commands.commands;

import features.commands.events.CommandEvent;

public class ServerStatsCommand extends AbstractCommand {
	public ServerStatsCommand() {
		name = "server";
		aliases = new String[]{"stats"};
		description = "Prints server info";
	}
	
	@Override
	public void execute(CommandEvent event) {
	
	}
}
