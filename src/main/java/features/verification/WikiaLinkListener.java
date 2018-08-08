package features.verification;

import features.commands.events.CommandEvent;
import features.commands.events.CommandEventBuilder;
import features.commands.events.CommandsHandler;
import model.entities.EnhancedGuild;
import model.entities.EnhancedUser;
import model.dbfields.BotFeatures;
import net.dv8tion.jda.core.entities.Message;
import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent;
import net.dv8tion.jda.core.hooks.ListenerAdapter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import providers.EnhancedGuildsProvider;
import providers.EnhancedUsersProvider;

import java.util.Arrays;
import java.util.LinkedList;

/**
 * WikiaLinkListener acts similar to CommandsListener.
 *
 * It forms a CommandEvent if message cona
 */
public class WikiaLinkListener extends ListenerAdapter {
	private static final String WIKIA_GENERAL_LINK_PATTERN = "https?://(?:[a-zA-Z]{0,2}\\.)?[a-zA-Z-]+\\.wikia\\.com/wiki/\\S+:\\S{2,}";
	private static final EnhancedGuildsProvider GUILDS_ENHANCER;
	private static final EnhancedUsersProvider USERS_ENHANCER;
	public static final Logger LOGGER;
	
	static {
		LOGGER  = LogManager.getLogger("CommandHandling");
		GUILDS_ENHANCER = EnhancedGuildsProvider.getInstance();
		USERS_ENHANCER = EnhancedUsersProvider.getInstance();
	}
	
	private final CommandsHandler commandsHandler;
	private final String defaultPrefix;
	
	public WikiaLinkListener(String defaultPrefix, CommandsHandler commandsHandler) {
		this.defaultPrefix = defaultPrefix;
		this.commandsHandler = commandsHandler;
	}
	
	@Override
	public void onGuildMessageReceived(GuildMessageReceivedEvent event) {
		final EnhancedGuild enhancedGuild = GUILDS_ENHANCER.enhance(event.getGuild());
		final EnhancedUser  enhancedUser  = USERS_ENHANCER.enhance(event.getAuthor());
		
		if(event.getAuthor().isBot()
				|| enhancedUser.isVerified()
				|| enhancedGuild.getMember(enhancedUser).getRoles().size() > 0
				|| !enhancedGuild.getFeature(BotFeatures.WikiaVerification))
			return;
		
		final Message message = event.getMessage();
		final String messageContent = message.getContentRaw().trim();
		final LinkedList<String> args = new LinkedList<>(Arrays.asList(messageContent.split("(?: )+")));
		final String prefixRegex = String.format("(?:%s|%s).*", enhancedGuild.getEscapedCommandPrefix(), defaultPrefix);
		
		if(args.get(0).matches(prefixRegex))
			return;
		else {
			for (final String s : args) {
				if(s.matches(WIKIA_GENERAL_LINK_PATTERN)) {
					args.clear();
					args.add(s);
				}
			}
		}
		
		final CommandEvent verify = new CommandEventBuilder(event.getJDA(), event.getResponseNumber())
				.setEnhancedGuildsProvider(GUILDS_ENHANCER)
				.setEnhancedUsersProvider(USERS_ENHANCER)
				.setEnhancedGuild(enhancedGuild)
				.setEnhancedUser (enhancedUser)
				.setMessage(message)
				.setMember(event.getMember())
				.setCommandName("verify")
				.setArgs(args)
				.build();
		
		commandsHandler.handle(verify);
	}
}
