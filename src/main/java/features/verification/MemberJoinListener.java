package features.verification;

import model.entities.EnhancedGuild;
import model.entities.EnhancedUser;
import model.dbfields.BotFeatures;
import model.dbfields.BotMessages;
import net.dv8tion.jda.core.JDA;
import net.dv8tion.jda.core.entities.TextChannel;
import net.dv8tion.jda.core.events.guild.member.GuildMemberJoinEvent;
import net.dv8tion.jda.core.hooks.ListenerAdapter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import providers.EnhancedGuildsProvider;
import providers.EnhancedUsersProvider;

import java.time.Instant;
import java.util.HashMap;
import java.util.Optional;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;


public class MemberJoinListener extends ListenerAdapter {
	private static final Logger LOGGER = LogManager.getLogger(MemberJoinListener.class);
	private static final EnhancedGuildsProvider ENHANCED_GUILDS_PROVIDER;
	private static final EnhancedUsersProvider  ENHANCED_USERS_PROVIDER;
	
	private static final ScheduledThreadPoolExecutor executor = new ScheduledThreadPoolExecutor(4);
	
	static {
		ENHANCED_GUILDS_PROVIDER = EnhancedGuildsProvider.getInstance();
		ENHANCED_USERS_PROVIDER  = EnhancedUsersProvider.getInstance();
	}
	
	private static UserVerificationHachiko hachiko;
	
	public MemberJoinListener(JDA api) {
		LOGGER.debug("MemberJoin Ready, creating Hachiko.");
		if(hachiko == null) {
			hachiko = new UserVerificationHachiko(api);
			executor.scheduleAtFixedRate(hachiko, 60, 600, TimeUnit.SECONDS);
		}
	}
	
	public static Optional<UserVerificationHachiko> getHachiko() {
		return Optional.ofNullable(hachiko);
	}
	
	@Override
	public void onGuildMemberJoin(GuildMemberJoinEvent event) {
		final EnhancedGuild enhancedGuild = ENHANCED_GUILDS_PROVIDER.enhance(event.getGuild());
		if(event.getUser().isBot() || !enhancedGuild.getFeature(BotFeatures.UserWelcome))
			return;
		
		final EnhancedUser enhancedUser = ENHANCED_USERS_PROVIDER.enhance(event.getUser());
		if(enhancedUser.isTotallyIgnored()) {
			return;
		}
		LOGGER.debug(enhancedUser.getName() + " connected to " + enhancedGuild);
		
		TextChannel responseChannel = enhancedGuild.getWelcomeChannel();
		
		if(enhancedUser.isVerified()) {
			enhancedUser.addVerification(enhancedGuild.getGuildId(), Instant.now());
			enhancedGuild.getController().addRolesToMember(event.getMember(), enhancedGuild.getVerificationRoles()).queue();
			responseChannel.sendMessage("**" + enhancedUser.getName()
					+ "** автоматически верифицирован. Никнейм на Wikia: **"
					+ enhancedUser.getWikiaNickname() + "**. Чтобы просмотреть верификации пользователя, используйте `"
					+ enhancedGuild.getCommandPrefix() + "user verifications " + enhancedUser.getName() + "`").queue();
			ENHANCED_USERS_PROVIDER.update(enhancedUser);
			return;
		}
		
		HashMap<String, String> keys = new HashMap<>();
		keys.put("usermention", enhancedUser.getAsMention());
		keys.put("username", event.getMember().getEffectiveName());
		keys.put("servername", enhancedGuild.getName());
		keys.put("prefix", enhancedGuild.getCommandPrefix());
		
		LOGGER.debug("Welcome message queued for " + enhancedUser.getName());
		responseChannel.sendMessage(enhancedGuild.getMessage(BotMessages.WELCOME_MESSAGE, keys)).queueAfter(4000, TimeUnit.MILLISECONDS);
		if (enhancedGuild.getFeature(BotFeatures.WikiaVerification))
			hachiko.addPendingUser(enhancedGuild, enhancedUser);
	}
	
	
}
