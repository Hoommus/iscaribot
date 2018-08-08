package features;

import model.entities.EnhancedGuild;
import model.entities.EnhancedUser;
import net.dv8tion.jda.core.OnlineStatus;
import net.dv8tion.jda.core.entities.Member;
import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent;
import net.dv8tion.jda.core.events.user.GenericUserEvent;
import net.dv8tion.jda.core.events.user.UserNameUpdateEvent;
import net.dv8tion.jda.core.events.user.UserOnlineStatusUpdateEvent;
import net.dv8tion.jda.core.hooks.ListenerAdapter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import providers.EnhancedGuildsProvider;
import providers.EnhancedUsersProvider;

import java.time.Instant;

public class UserSessionsTracker extends ListenerAdapter {
	private static final Logger LOGGER = LogManager.getLogger(UserSessionsTracker.class);
	
	private static final EnhancedGuildsProvider ENHANCED_GUILDS_PROVIDER = EnhancedGuildsProvider.getInstance();
	private static final EnhancedUsersProvider  ENHANCED_USERS_PROVIDER  = EnhancedUsersProvider .getInstance();
	
	@Override
	public void onUserNameUpdate(UserNameUpdateEvent event) {
		EnhancedUser enhancedUser = ENHANCED_USERS_PROVIDER.enhance(event.getUser());
	}
	
	@Override
	public void onUserOnlineStatusUpdate(UserOnlineStatusUpdateEvent event) {
		Instant eventTime = Instant.now();
		EnhancedUser enhancedUser = ENHANCED_USERS_PROVIDER.enhance(event.getUser());
		EnhancedGuild enhancedGuild = ENHANCED_GUILDS_PROVIDER.enhance(event.getGuild());
		
		OnlineStatus current = enhancedGuild.getMember(enhancedUser).getOnlineStatus();
		OnlineStatus previous = event.getPreviousOnlineStatus();
		
		if((current == OnlineStatus.ONLINE || current == OnlineStatus.DO_NOT_DISTURB || current == OnlineStatus.IDLE)
				&& (previous == OnlineStatus.OFFLINE || previous == OnlineStatus.UNKNOWN))
			enhancedUser.setLastGoneOnline(eventTime);
		else if(previous == OnlineStatus.OFFLINE || previous == OnlineStatus.UNKNOWN)
			enhancedUser.setLastGoneOffline(eventTime);
		ENHANCED_USERS_PROVIDER.update(enhancedUser);
		LOGGER.debug("Updating user in userOnlineStatus");
	}
	
	@Override
	public void onGenericUser(GenericUserEvent event) {
		super.onGenericUser(event);
	}
	
	//	@Override
//	public void onUserTyping(UserTypingEvent event) {
//		Instant eventTime = Instant.now();
//		EnhancedUser enhancedUser = ENHANCED_USERS_PROVIDER.enhance(event.getMember().getUser());
//		EnhancedGuild enhancedGuild = ENHANCED_GUILDS_PROVIDER.enhance(event.getGuild());
//
//		Member member = enhancedGuild.getMember(enhancedUser);
//		if(member.getOnlineStatus() == OnlineStatus.OFFLINE)
//			enhancedUser.setLastGoneOnline(eventTime);
//		ENHANCED_USERS_PROVIDER.update(enhancedUser);
//		LOGGER.debug("Updating user in userTyping");
//
//	}
	
	@Override
	public void onGuildMessageReceived(GuildMessageReceivedEvent event) {
		Instant eventTime = Instant.now();
		Member member = event.getMember();
		EnhancedUser enhancedUser = ENHANCED_USERS_PROVIDER.enhance(event.getMember().getUser());
		EnhancedGuild enhancedGuild = ENHANCED_GUILDS_PROVIDER.enhance(event.getGuild());
		
		if(member.getOnlineStatus() == OnlineStatus.OFFLINE) {
			enhancedUser.setLastGoneOnline(eventTime);
			ENHANCED_USERS_PROVIDER.update(enhancedUser);
			LOGGER.debug("Updating user in messageReceived");
		}
	}
}
