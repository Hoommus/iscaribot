package features.commands.commands;

import features.commands.events.CommandEvent;
import model.entities.EnhancedGuild;
import model.dbfields.BotMessages;
import net.dv8tion.jda.core.Permission;
import net.dv8tion.jda.core.entities.MessageChannel;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static features.commands.utils.JDACustomUtilities.findMember;
import static providers.TemplateProcessor.process;

public class JudasKissCommand extends AbstractCommand {
	public JudasKissCommand() {
		this.name = "kiss";
		this.aliases = new String[]{"judas", "judaskiss"};
		this.memberPermissions = new Permission[] {Permission.BAN_MEMBERS};
		this.botPermissions    = new Permission[] {Permission.KICK_MEMBERS, Permission.MANAGE_ROLES, Permission.MESSAGE_WRITE};
		this.baseResourceName  = "judas_kiss";
		// Let's imagine this isn't very popular command and we can allow async init
		CompletableFuture.runAsync(() -> super.hookLocalisation(Messages.class));
	}
	
	@Override
	public void execute(CommandEvent event) {
		final EnhancedGuild guild    = event.getEnhancedGuild();
		final MessageChannel channel = event.getChannel();
		final Map<String, String> keys = new HashMap<>();
		keys.put("botname", event.getJDA().getSelfUser().getName());
		if(event.getArgs().size() > 0) {
			findMember(guild, String.join(" ", event.getArgs()))
					.ifPresentOrElse(member -> {
						keys.put("username", member.getEffectiveName());
						keys.put("usermention", member.getAsMention());
						
						Map<String, String> messages = MESSAGES.get(guild.getLocale());
						channel.sendMessage(process(messages.get(Messages.FIRST_REMARK.name()),  keys))
								.queueAfter(1000, TimeUnit.MILLISECONDS);
						channel.sendMessage(process(messages.get(Messages.JUDAS_TALK_FIRST.name()), keys))
								.queueAfter(3500, TimeUnit.MILLISECONDS);
						channel.sendMessage(process(messages.get(Messages.SECOND_REMARK.name()), keys))
								.queueAfter(6500, TimeUnit.MILLISECONDS);
						guild.getController().removeRolesFromMember(member, member.getRoles())
								.queueAfter(6700, TimeUnit.MILLISECONDS);
						channel.sendMessage(process(messages.get(Messages.THIRD_REMARK.name()), keys))
								.queueAfter(9999, TimeUnit.MILLISECONDS);
					}, () -> channel.sendMessage(guild.getMessage(BotMessages.USER_NOT_FOUND)).queue());
			
		} else
			channel.sendMessage(guild.getMessage(BotMessages.USER_NOT_FOUND)).queue();
	}
	
	private enum Messages {
		FIRST_REMARK,
		JUDAS_TALK_FIRST,
		SECOND_REMARK,
		THIRD_REMARK
	}
}
