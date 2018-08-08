package features.commands.commands;

import features.commands.events.CommandEvent;
import net.dv8tion.jda.core.Permission;
import net.dv8tion.jda.core.entities.Message;
import net.dv8tion.jda.core.entities.MessageChannel;
import net.dv8tion.jda.core.entities.TextChannel;

import java.util.List;
import java.util.stream.Collectors;

public class WipeCommand extends AbstractCommand {
	public WipeCommand() {
		this.name = "wipe";
		this.aliases = new String[]{"flush"};
		this.botPermissions = new Permission[]{Permission.MESSAGE_READ, Permission.MESSAGE_HISTORY, Permission.MESSAGE_MANAGE};
		this.memberPermissions = new Permission[]{Permission.MESSAGE_MANAGE};
	}
	
	@Override
	public void execute(CommandEvent event) {
		try {
			MessageChannel channel = event.getChannel();
			int limit = Integer.parseInt(event.getArgs().get(0));
			if(limit < 1 || limit > 500) {
				event.getChannel().sendMessage("Аргумент должен быть натуральным числом от 1 до 500.").queue();
				return;
			}
			List<Message> pending = channel.getIterableHistory().complete().stream().limit(limit + 1).skip(1).collect(Collectors.toList());
			((TextChannel) channel).deleteMessages(pending).queue();
			event.getMessage().addReaction("✅").queue();
		} catch (NumberFormatException | ArrayIndexOutOfBoundsException e) {
			event.getChannel().sendMessage("Аргумент должен быть натуральным числом.").queue();
		}
	}
	
	private enum Messages {
		ARGUMENT_HELP_FROM_TO,
		ARGUMENT_NATURAL
	}
}
