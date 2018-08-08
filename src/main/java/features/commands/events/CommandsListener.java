package features.commands.events;

import model.entities.EnhancedGuild;
import model.entities.EnhancedUser;
import net.dv8tion.jda.core.entities.ChannelType;
import net.dv8tion.jda.core.entities.Message;
import net.dv8tion.jda.core.events.message.MessageReceivedEvent;
import net.dv8tion.jda.core.hooks.ListenerAdapter;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import providers.EnhancedGuildsProvider;
import providers.EnhancedMembersProvider;
import providers.EnhancedUsersProvider;

import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;

/**
 * Проверяет есть ли в сообщении команда, заворачивает в ивент и пробрасывает менеджеру,
 * который держит список команд и проверяет есть ли в нём то, что запрашивает юзер.
 */
public class CommandsListener extends ListenerAdapter {
	private static final EnhancedGuildsProvider  GUILD_ENHANCER;
	private static final EnhancedUsersProvider   USERS_ENHANCER;
	private static final EnhancedMembersProvider MEMBERS_ENHANCER;
	
	public static final Logger LOGGER;
	
	static {
		LOGGER  = LogManager.getLogger("CommandHandling");
		GUILD_ENHANCER   = EnhancedGuildsProvider.getInstance();
		USERS_ENHANCER   = EnhancedUsersProvider .getInstance();
		MEMBERS_ENHANCER = EnhancedMembersProvider.getInstance();
	}
	
	private final CommandsHandler commandsHandler;
	private final String defaultPrefix;
	
	public CommandsListener(String defaultPrefix, CommandsHandler commandsHandler) {
		this.defaultPrefix = defaultPrefix;
		this.commandsHandler = commandsHandler;
	}
	
	/**
	 * <b>Reduces <i>args</i> size by one</b> - removes first element because it's command.
	 *
	 * @param event MessageReceivedEvent
	 */
	@Override
	public void onMessageReceived(MessageReceivedEvent event) {
		if(event.getAuthor().isBot() || event.getChannelType() == ChannelType.PRIVATE)
			return;
		
		final Message message = event.getMessage();
		
		final EnhancedGuild enhancedGuild = GUILD_ENHANCER.enhance(event.getGuild());
		final EnhancedUser  enhancedUser  = USERS_ENHANCER.enhance(event.getAuthor());
		
		final String messageContent = message.getContentRaw().trim();
		String[] args = messageContent.split("(?: )+");
		
		if(args[0].startsWith(enhancedGuild.getCommandPrefix()) || args[0].startsWith(defaultPrefix)) {
			String prefixRegex = String.format("(?:%s|%s)", enhancedGuild.getEscapedCommandPrefix(), defaultPrefix);
			args[0] = args[0].replaceFirst(prefixRegex, "");
		}
		// TODO: Test this thing. Definitely not obvious how this should work
		// And I even not sure if it is needed
		else if(args[0].replaceFirst("!", "").equals(event.getJDA().getSelfUser().getAsMention())) {
			args = ArrayUtils.subarray(args, 1, args.length);
		} else
			return;
		
		final CommandEvent commandEvent = new CommandEventBuilder(event.getJDA(), event.getResponseNumber())
				.setEnhancedGuild(enhancedGuild)
				.setEnhancedUser (enhancedUser)
				.setMessage(message)
				.setMember(event.getMember())
				.setCommandName(args[0])
				.setArgs(new LinkedList<>(Arrays.asList(ArrayUtils.subarray(args, 1, args.length))))
				.setMessageKeys(new HashMap<>())
				.setEnhancedUsersProvider(USERS_ENHANCER)
				.setEnhancedGuildsProvider(GUILD_ENHANCER)
				.build();
		
		commandsHandler.handle(commandEvent);
	}
}
