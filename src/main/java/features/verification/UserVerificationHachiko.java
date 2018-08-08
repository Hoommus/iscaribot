package features.verification;

import model.entities.EnhancedGuild;
import model.entities.EnhancedUser;
import model.dbfields.BotFeatures;
import model.dbfields.BotMessages;
import net.dv8tion.jda.core.JDA;
import net.dv8tion.jda.core.entities.Guild;
import net.dv8tion.jda.core.entities.Member;
import net.dv8tion.jda.core.entities.TextChannel;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import providers.EnhancedGuildsProvider;
import providers.EnhancedUsersProvider;

import javax.annotation.Nonnull;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

/**
 * It's Hachiko because it waits for user to verify for eternity and reminds about it if needed.
 * And it's Hachiko for not to be confused with Waiter (this guy can be used for those command events)
 *
 * TODO: Add buffers and ordered lists for all guilds
 * TODO: Make grouping if join moments very close
 *
 * Initialized at {@link MemberJoinListener()}
 */
public class UserVerificationHachiko implements Runnable {
	private static final Logger LOGGER = LogManager.getLogger(UserVerificationHachiko.class);
	
	private static final EnhancedGuildsProvider GUILDS_ENHANCER = EnhancedGuildsProvider.getInstance();
	private static final EnhancedUsersProvider  USERS_ENHANCER  = EnhancedUsersProvider.getInstance();
	
	//                   GuildID          Pending users
	private final HashMap<Long, LinkedList<EnhancedUser>> PENDING_VERIFICATIONS = new HashMap<>();
	private final HashMap<Long, LinkedList<Member>> FIRST_THRESHOLD  = new HashMap<>();
	private final HashMap<Long, LinkedList<Member>> SECOND_THRESHOLD = new HashMap<>();
	private final HashMap<Long, LinkedList<Member>> WAITING_TO_KICK  = new HashMap<>();
	
	public UserVerificationHachiko(JDA jda) {
		LOGGER.debug("Hachiko constructor");
		final ArrayList<EnhancedGuild> enhancedGuilds = GUILDS_ENHANCER.enhanceEach(jda.getGuilds());
		
		// Refilling lists ONCE
		LOGGER.debug("Refill for " + enhancedGuilds.size() + " guilds.");
		enhancedGuilds.forEach(enhancedGuild -> {
			PENDING_VERIFICATIONS.put(enhancedGuild.getGuildId(), new LinkedList<>());
			FIRST_THRESHOLD .put(enhancedGuild.getGuildId(), new LinkedList<>());
			SECOND_THRESHOLD.put(enhancedGuild.getGuildId(), new LinkedList<>());
			WAITING_TO_KICK .put(enhancedGuild.getGuildId(), new LinkedList<>());
			
			final LinkedList<Member> members = enhancedGuild.getMembers().stream()
					.filter(member -> !member.getUser().isBot())
					.filter(member -> Collections.disjoint(member.getRoles(), enhancedGuild.getVerificationRoles()))
					.sorted((o1, o2) -> o1.getJoinDate().equals(o2.getJoinDate()) ? 0 : o1.getJoinDate().isBefore(o2.getJoinDate()) ? 1 : -1)
					.collect(Collectors.toCollection(LinkedList::new));
			// First element has the earliest join date (I hope)
			members.stream().limit(10).forEach(m -> LOGGER.debug(m.getEffectiveName() + " " + m.getJoinDate()));
			
			for(Member m : enhancedGuild.getMembers()) {
				if(!m.getRoles().containsAll(enhancedGuild.getVerificationRoles())) {
					PENDING_VERIFICATIONS.get(enhancedGuild.getGuildId()).add(USERS_ENHANCER.enhance(m.getUser()));
				}
			}
			
		});
		LOGGER.debug("Pending verifications: " +  PENDING_VERIFICATIONS.toString());
	}
	
	public void addPendingUser(Guild guild, EnhancedUser user) {
		PENDING_VERIFICATIONS.get(guild.getIdLong()).add(user);
	}
	
	/**
	 * This method is used to return info about not verified users and
	 * a table containing all awaited verifications.
	 *
	 * @param enhancedGuild
	 * @return
	 */
	public String getReport(EnhancedGuild enhancedGuild) {
		return "";
	}
	
	private Instant nextReport = Instant.now().minusSeconds(300);
	
	public void force(EnhancedGuild enhancedGuild) {
		nextReport = Instant.now().minusSeconds(60);
		LOGGER.info("Hachiko routine forced for " + enhancedGuild.getName());
		cleanGuild(enhancedGuild);
	}
	
	private void cleanGuild(EnhancedGuild enhancedGuild) {
		final Long guildid = enhancedGuild.getGuildId();
		final LinkedList<EnhancedUser> enhancedUsers = PENDING_VERIFICATIONS.get(guildid);
		
		if (enhancedGuild.getVerificationTimeout() < 0) {
			return;
		}
		
		final int timeoutMinutesThird = 20 * enhancedGuild.getVerificationTimeout();
		final int fullTimeout = timeoutMinutesThird * 3;
		
		if(nextReport.isBefore(Instant.now())) {
			LOGGER.debug("Features:" + enhancedGuild.getBotFeatures());
			LOGGER.debug("Running Hachiko routine for " + enhancedGuild);
			LOGGER.debug("Timeout: " + timeoutMinutesThird * 3);
		}
		final LinkedHashMap<Member, Long> firstNotification = new LinkedHashMap<>();
		final LinkedHashMap<Member, Long> secondNotification = new LinkedHashMap<>();
		final List<EnhancedUser> toRemove = new ArrayList<>();
		
		enhancedUsers.forEach(user -> {
			final Member member = enhancedGuild.getMember(user);
			
			if (member == null || (member.getRoles().size() > 0 && member.getRoles().containsAll(enhancedGuild.getVerificationRoles()))) {
				toRemove.add(user);
				return;
			}
			final OffsetDateTime joined = member.getJoinDate().truncatedTo(ChronoUnit.SECONDS);
			
			final long difference = ChronoUnit.MINUTES.between(joined, OffsetDateTime.now());
			
			if (difference >= fullTimeout
					&& !WAITING_TO_KICK.get(guildid).contains(member)) {
				WAITING_TO_KICK.get(guildid).add(member);
				SECOND_THRESHOLD.get(guildid).remove(member);
			} else if (difference < fullTimeout
					&& difference >= timeoutMinutesThird * 2
					&& !SECOND_THRESHOLD.get(guildid).contains(member)) {
				SECOND_THRESHOLD.get(guildid).add(member);
				FIRST_THRESHOLD.get(guildid).remove(member);
				secondNotification.put(member, fullTimeout - difference);
			} else if (difference < timeoutMinutesThird * 2
					&& difference >= timeoutMinutesThird
					&& !FIRST_THRESHOLD.get(guildid).contains(member)) {
				FIRST_THRESHOLD.get(guildid).add(member);
				firstNotification.put(member, fullTimeout - difference);
			}
		});
		enhancedUsers.removeAll(toRemove);
		
		if(enhancedGuild.getFeature(BotFeatures.UserVerificationNotify)) {
			CompletableFuture.runAsync(() ->
					fireNotification(firstNotification, enhancedGuild.getVerificationChannel(), enhancedGuild));
			CompletableFuture.runAsync(() ->
					fireNotification(secondNotification, enhancedGuild.getVerificationChannel(), enhancedGuild));
		}
		if(nextReport.isBefore(Instant.now())) {
			LOGGER.debug("First threshold "  + FIRST_THRESHOLD.get(guildid));
			LOGGER.debug("Second threshold " + SECOND_THRESHOLD.get(guildid));
			LOGGER.debug("Kick threshold "   + WAITING_TO_KICK.get(guildid));
		}
		if (enhancedGuild.getFeature(BotFeatures.UserVerificationCleanup)) {
			WAITING_TO_KICK.get(guildid).forEach(member -> {
				if (member.getRoles().containsAll(enhancedGuild.getVerificationRoles())) {
					WAITING_TO_KICK.get(guildid).remove(member);
					return;
				}
				enhancedGuild.getController().kick(member, enhancedGuild.getMessage(BotMessages.VERIFICATION_REASON)).complete();
			});
		}
		if (nextReport.isBefore(Instant.now())) {
			LOGGER.debug("Kicked " + WAITING_TO_KICK.get(guildid).size() + " users");
			if (WAITING_TO_KICK.get(guildid).size() > 0)
				LOGGER.error("WAITING_TO_KICK size greater than zero.");
		}
	}
	
	@Override
	public void run() {
		try {
			if (nextReport == null || nextReport.isBefore(Instant.now())) {
				LOGGER.info("Hachiko routine for " + PENDING_VERIFICATIONS.size() + " guilds.");
			}
			PENDING_VERIFICATIONS.forEach((guildid, enhancedUsers) ->
					cleanGuild(GUILDS_ENHANCER.getEnhancedGuildById(guildid)));
			if (nextReport == null || nextReport.isBefore(Instant.now())) {
				nextReport = Instant.now().plus(1, ChronoUnit.HOURS);
				LOGGER.info("Next report @ " + nextReport.toString() + ".");
			}
		} catch (Throwable t) {
			LOGGER.error("Hachiko han an uncaught exception.", t);
		}
	}
	
	private static void fireNotification(@Nonnull Map<Member, Long> notification,
										 @Nonnull TextChannel verificationChannel,
										 @Nonnull EnhancedGuild enhancedGuild) {
		long timeout = notification.values().stream().mapToLong(Long::longValue).sum() / notification.size();
		
		String mentions = String.join(" ", notification.keySet().stream().map(Member::getAsMention).collect(Collectors.toList()));
		Map<String, String> keys = new HashMap<>();
		keys.put("mentions", mentions);
		keys.put("timeout_hours", " " + timeout / 60);
		keys.put("timeout_minutes", " " + timeout % 60);
		verificationChannel.sendMessage(enhancedGuild.getMessage(BotMessages.VERIFICATION_NOTIFY_WIKIA_GENERIC, keys)).queue();
	}
}
