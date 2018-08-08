package features.commands.utils;

import model.entities.EnhancedGuild;
import model.entities.EnhancedUser;
import net.dv8tion.jda.core.entities.Member;
import net.dv8tion.jda.core.entities.Role;
import net.dv8tion.jda.core.entities.TextChannel;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import providers.EnhancedUsersProvider;

import javax.annotation.Nullable;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by festus on 15.07.17.
 */
@SuppressWarnings("WeakerAccess")
public abstract class JDACustomUtilities {
	private static final Logger LOGGER = LogManager.getLogger(JDACustomUtilities.class);
	private static final Pattern USER_MENTION = Pattern.compile("<@!?(\\d{18})>");
	
	@Nullable
	public static TextChannel findTextChannel(EnhancedGuild enhancedGuild, String forSearch) {
		Pattern templatesPattern = Pattern.compile("\\d{18}", Pattern.CASE_INSENSITIVE);
		Matcher matcher = templatesPattern.matcher(forSearch);
		
		TextChannel textChannel = null;
		
		while (matcher.find()) {
			textChannel = enhancedGuild.getTextChannelById(matcher.group());
			if(textChannel != null)
				return textChannel;
		}
		
		for (TextChannel channel : enhancedGuild.getTextChannels()) {
			if (channel.getName().contains(forSearch)) {
				textChannel = channel;
				break;
			}
		}
		
		return textChannel;
	}
	
	public static Optional<EnhancedUser> findMemberAndEnhance
			(EnhancedUsersProvider provider, EnhancedGuild guild, String search) {
		// TODO: Rewrite this shit
		final EnhancedUser[] user = {null};
		findMember(guild, search).ifPresent(member -> user[0] = provider.enhance(member.getUser(), false));
		return Optional.ofNullable(user[0]);
	}
	
	public static Optional<Member> findMember(EnhancedGuild guild, String search) {
		return Optional.ofNullable(findMemberByAnyName(guild, search));
	}
	
	@Nullable
	public static Member findMemberByAnyName(EnhancedGuild guild, String name) {
		Matcher matcher = USER_MENTION.matcher(name);
		if(matcher.find()) {
			return guild.getMemberById(matcher.group(1));
		}
		
		name = name.toLowerCase();
		for(Member member : guild.getMembers()) {
			if (member.getNickname() != null && member.getNickname().toLowerCase().contains(name)) {
				LOGGER.debug("Member got by nickname.");
				return member;
			} else if (member.getEffectiveName().toLowerCase().contains(name)) {
				LOGGER.debug("Member got by Effective name.");
				return member;
			} else if (member.getUser().getName().toLowerCase().contains(name)) {
				LOGGER.debug("Member got by User.getName().");
				return member;
			}
		}
		return null;
	}
	
	@Nullable
	public static Role findRoleByAnyName(EnhancedGuild guild, String name) {
		if (guild.getRolesByName(name, true).size() == 0)
			for (Role role : guild.getRoles()) {
				if (role.getName().toLowerCase().contains(name.toLowerCase()))
					return role;
			}
		else
			return guild.getRolesByName(name, true).get(0);
		
		return null;
	}
	
	@Nullable
	public static Role findRole(EnhancedGuild guild, String forSearch) {
		if(forSearch.matches("\\d{18}")) {
			Long id = Long.parseLong(forSearch);
			return guild.getRoleById(id);
		}
		
		for(Role role : guild.getRoles())
			if(role.getName().toLowerCase().contains(forSearch.toLowerCase()))
				return role;
		
		return null;
	}
}
