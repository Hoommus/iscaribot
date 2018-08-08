package features.commands.commands;

import features.commands.annotations.SubCommand;
import features.commands.events.CommandEvent;
import model.entities.EnhancedGuild;
import model.dbfields.BotMessages;
import net.dv8tion.jda.core.entities.MessageChannel;

import java.util.Map;

import static features.commands.utils.JDACustomUtilities.findMemberAndEnhance;
import static providers.TemplateProcessor.process;

public class AvatarCommand extends AbstractCommand {
	public AvatarCommand() {
		this.name = "avatar";
		this.description = "Provides avatars URL.";
		this.baseResourceName = "avatar_command";
		hookLocalisation(Messages.class);
	}
	
	@Override
	public void execute(CommandEvent event) {
		final MessageChannel channel = event.getChannel();
		final EnhancedGuild  guild   = event.getEnhancedGuild();
		final Map<String, String> messages = MESSAGES.get(guild.getLocale());
		
		if (event.getArgs().size() > 0) {
			findMemberAndEnhance(event.getEnhancedUsersProvider(), guild, String.join(" ", event.getArgs()))
					.ifPresentOrElse(
							user -> channel.sendMessage(process(messages.get(Messages.USER_AVATAR.name()),
									Map.of("username", user.getName(),
											"url", user.getEffectiveAvatarUrl()))).queue(),
							()   -> channel.sendMessage(guild.getMessage(BotMessages.USER_NOT_FOUND)).queue());
		} else
			channel.sendMessage(process(messages.get(Messages.YOUR_AVATAR.name()),
					Map.of("url", event.getAuthor().getAvatarUrl()))).queue();
	}
	
	@SubCommand(subCommand = "wikia")
	public void wikiaAvatar(CommandEvent event) {
		final MessageChannel channel = event.getChannel();
		final EnhancedGuild  guild   = event.getEnhancedGuild();
		final Map<String, String> messages = MESSAGES.get(guild.getLocale());
		
		if (event.getArgs().size() > 0) {
			findMemberAndEnhance(event.getEnhancedUsersProvider(), guild, String.join(" ", event.getArgs()))
					.ifPresentOrElse(
							user -> {
								if(user.getWikiaUser() != null)
									channel.sendMessage(process(messages.get(Messages.WIKIA_USER_AVATAR.name()),
											Map.of("url", user.getWikiaAvatarUrl(), "username", guild.getMember(user).getEffectiveName()))).queue();
								else
									channel.sendMessage(guild.getMessage(BotMessages.WIKIA_USER_NOT_BINDED)).queue();
							},
							()   -> channel.sendMessage(guild.getMessage(BotMessages.USER_NOT_FOUND)).queue());
		} else if (event.getAuthor().getWikiaUser() != null)
			channel.sendMessage(process(messages.get(Messages.WIKIA_YOUR_AVATAR.name()),
					Map.of("url", event.getAuthor().getWikiaAvatarUrl()))).queue();
		else
			channel.sendMessage(guild.getMessage(BotMessages.WIKIA_USER_NOT_BINDED)).queue();
		
	}
	
	enum Messages {
		USER_AVATAR,
		YOUR_AVATAR,
		WIKIA_USER_AVATAR,
		WIKIA_YOUR_AVATAR
	}
}
