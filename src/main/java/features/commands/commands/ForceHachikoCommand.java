package features.commands.commands;

import features.commands.annotations.SubCommand;
import features.commands.events.CommandEvent;
import features.verification.MemberJoinListener;
import net.dv8tion.jda.core.Permission;

public class ForceHachikoCommand extends AbstractCommand {
	public ForceHachikoCommand() {
		this.name = "hachiko";
		this.description = "Forces poor Hachiko.";
		this.memberPermissions = new Permission[] { Permission.MANAGE_SERVER };
		this.botPermissions = new Permission[] { Permission.KICK_MEMBERS, Permission.MESSAGE_WRITE };
		this.aliases = new String[]{"fh"};
	}
	
	@Override
	@SubCommand(subCommand = "force")
	public void execute(CommandEvent event) {
		MemberJoinListener.getHachiko().ifPresent(hachiko -> {
			hachiko.force(event.getEnhancedGuild());
			event.getMessage().addReaction("✅").queue();
		});
	}
}
