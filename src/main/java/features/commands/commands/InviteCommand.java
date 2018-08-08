package features.commands.commands;

import features.commands.events.CommandEvent;
import model.dbfields.BotMessages;
import net.dv8tion.jda.core.entities.SelfUser;

import java.util.Map;

public class InviteCommand extends AbstractCommand {
	
	public InviteCommand() {
		this.name = "invite";
		this.description = "Prints invite link.";
	}
	
	@Override
	public void execute(CommandEvent event) {
		SelfUser selfUser = event.getJDA().getSelfUser();
		String bareLink = "https://discordapp.com/api/oauth2/authorize?client_id=" + selfUser.getId() + "&permissions=403777666&scope=bot";
		event.getChannel().sendMessage(event.getEnhancedGuild()
				.getMessage(BotMessages.INVITE_COMMAND_MESSAGE, Map.of("link", bareLink)))
				.queue();
	}
}
