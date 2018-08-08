package features.commands.commands;

import features.commands.events.CommandEvent;
import model.entities.EnhancedGuild;
import net.dv8tion.jda.core.MessageBuilder;
import net.dv8tion.jda.core.Permission;

import java.util.stream.Collectors;

public class HelpAdminCommand extends AbstractCommand {
	
	public HelpAdminCommand() {
		name = "admin";
		aliases = new String[]{"sysop", "админ", "одмин", "адмен", "администратор"};
		description = "Prints admin commands";
		this.memberPermissions = new Permission[]{Permission.KICK_MEMBERS, Permission.BAN_MEMBERS};
	}
	
	@Override
	public void execute(CommandEvent event) {
		EnhancedGuild guild = event.getEnhancedGuild();
		MessageBuilder messageBuilder = new MessageBuilder("**Available admin commands**: `");
		messageBuilder.append(String.join("` `",
				event.getHandler().getCommands()
						.stream()
						.filter(AbstractCommand::isAdmin)
						.filter(abstractCommand -> !guild.getDisabledCommands().contains(abstractCommand))
						.map(AbstractCommand::getName)
						.collect(Collectors.toList())));
		messageBuilder.append("`\n Type `!!<command> help` to get basic help for any command.");
		event.getChannel().sendMessage(messageBuilder.build()).queue();
	}
}
