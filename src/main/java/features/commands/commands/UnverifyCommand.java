package features.commands.commands;

import features.commands.annotations.SubCommand;
import features.commands.events.CommandEvent;
import model.entities.EnhancedUser;
import net.dv8tion.jda.core.Permission;

import java.util.ArrayList;

public class UnverifyCommand extends AbstractCommand {
	private ArrayList<String> allowedUsers = new ArrayList<>();
	
	public UnverifyCommand(String id) {
		allowedUsers.add(id);
		name = "unverify";
		aliases = new String[] {"unbind"};
		memberPermissions = new Permission[]{Permission.ADMINISTRATOR};
	}
	
	@Override
	public void execute(CommandEvent event) {
		if ( ! allowedUsers.contains(event.getEnhancedUser().getId()))
			return;
		EnhancedUser enhancedUser = event.getEnhancedUser();
		enhancedUser.removeVerifications(event.getEnhancedGuild());
		event.getMessage().addReaction("✅").queue();
		event.getEnhancedUsersProvider().update(enhancedUser);
	}
	
	@SubCommand(subCommand = "--totally", permissions = {Permission.ADMINISTRATOR})
	public void totally(CommandEvent event) {
		if ( ! allowedUsers.contains(event.getEnhancedUser().getId()))
			return;
		EnhancedUser enhancedUser = event.getEnhancedUser();
		enhancedUser.removeVerifications(event.getEnhancedGuild());
		enhancedUser.setExternalUsername(null);
		enhancedUser.setWikiaUser(null);
		event.getMessage().addReaction("✅").queue();
		event.getEnhancedUsersProvider().update(enhancedUser);
	}
}
