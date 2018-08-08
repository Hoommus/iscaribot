package features.commands.commands;

import features.commands.annotations.SubCommand;
import features.commands.events.CommandEvent;
import net.dv8tion.jda.core.Permission;
import net.dv8tion.jda.core.entities.Role;

public class EchoCommand extends AbstractCommand {
	public EchoCommand() {
		name = "echo";
		description = "Echoes a string.";
		memberPermissions = new Permission[]{Permission.ADMINISTRATOR};
	}
	
	@Override
	public void execute(CommandEvent event) {
		if(event.getArgs().isEmpty())
			return;
		StringBuilder builder = new StringBuilder();
		for (String s : event.getArgs()) {
			if (s.matches("<@!?\\d{18}>")) {
				Long id = Long.parseLong(s.replaceAll("[<@!>]", ""));
				Role role = event.getEnhancedGuild().getRoleById(id);
				if (role != null
						&& role.isMentionable()) {
					builder.append("<").append(role.getName()).append(">");
					continue;
				}
			}
			builder.append(s).append(" ");
		}
		event.getChannel().sendMessage(builder.toString().trim()).queue();
	}
	
	@SubCommand(subCommand = "--delete")
	public void delete(CommandEvent event) {
		execute(event);
		event.getMessage().delete().queue();
	}
}
