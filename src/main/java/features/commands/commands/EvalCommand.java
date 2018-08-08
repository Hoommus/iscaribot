package features.commands.commands;

import features.commands.events.CommandEvent;

public class EvalCommand extends AbstractCommand {
	public EvalCommand() {
		this.name = "eval";
	}
	
	@Override
	public void execute(CommandEvent event) {
		event.sendMessage("Ha-ha-ha...\nNice try.");
	}
}
