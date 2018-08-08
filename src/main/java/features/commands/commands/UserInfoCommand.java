package features.commands.commands;

import features.commands.annotations.SubCommand;
import features.commands.events.CommandEvent;
import model.entities.EnhancedGuild;
import model.entities.EnhancedUser;
import model.dbfields.BotMessages;
import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.OnlineStatus;
import net.dv8tion.jda.core.entities.Member;
import net.dv8tion.jda.core.entities.MessageChannel;
import providers.TemplateProcessor;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.LinkedList;
import java.util.Map;
import java.util.Optional;

import static features.commands.events.CommandsListener.LOGGER;
import static features.commands.utils.JDACustomUtilities.findMember;
import static features.commands.utils.JDACustomUtilities.findMemberAndEnhance;

public class UserInfoCommand extends AbstractCommand {
	private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("ccc, d MMMM uuuu '@' kk':'mm':'ss O").withZone(ZoneId.of("UTC"));
	public final String wikiaOrigin;
	public final String wikiaAccount;
	
	public UserInfoCommand(String wikiaOrigin, String wikiaAccount) {
		this.wikiaOrigin = wikiaOrigin;
		this.wikiaAccount = wikiaAccount;
		
		this.name = "userinfo";
		this.aliases = new String[]{"user", "wikiuser", "info"};
		this.description = "Prints all info about specific user (or yourself) including info about verifications.";
		this.baseResourceName = "userinfo_fields";
		hookLocalisation(Messages.class);
	}
	
	@Override
	public void execute(CommandEvent event) {
		LOGGER.traceEntry();
		final long start = System.nanoTime();
		final EnhancedGuild guild = event.getEnhancedGuild();
		final MessageChannel responseChannel = event.getChannel();
		final DateTimeFormatter utc = formatter.withLocale(guild.getLocale());
		
		EnhancedUser user = event.getAuthor();
		Member member = guild.getMember(event.getAuthor());
		final LinkedList<String> args = event.getArgs();
		
		if (args.size() > 0) {
			//noinspection ConstantConditions
			final Optional<Member> optionalMember = findMember(guild, args.get(0));
			if(optionalMember.isPresent())
				member = optionalMember.get();
			else
				return;
			user = event.getEnhancedUsersProvider().enhance(member.getUser(), false);
		}
		
		if(user == null)
			return;
		if(user.getIdLong() == event.getJDA().getSelfUser().getIdLong()) {
			Map<String, String> keys = Map.of("origin", wikiaOrigin, "wikiaaccount", wikiaAccount);
			responseChannel.sendMessage(guild.getMessage(BotMessages.BOT_WIKIA_ACCOUNT, keys)).queue();
			return;
		}
		if(user.isBot()) {
			responseChannel.sendMessage(guild.getMessage(BotMessages.ITS_JUST_A_BOT)).queue();
			return;
		}
		
		responseChannel.sendTyping().complete();
		
		final Map<String, String> fields = MESSAGES.get(guild.getLocale());
		final EmbedBuilder builder = new EmbedBuilder();
		builder .setColor(Colors.USER)
				.setThumbnail(user.getAvatarUrl());
		
		builder .addField(fields.get(Messages.ONLINE_STATUS.name()),  member.getOnlineStatus().name(), true);
		
		if(member.getOnlineStatus() == OnlineStatus.OFFLINE)
			builder.addField(fields.get(Messages.LAST_ONLINE.name()),  formatter.format(user.getLastGoneOnline()), true);
		
		builder .addField(fields.get(Messages.ID.name()), user.getId(), true)
				.addField(fields.get(Messages.NICKNAME.name()), member.getNickname() == null
						? "Не установлен."
						: member.getNickname(), true)
				.addField(fields.get(Messages.DISCORD_CREATION.name()), user.getCreationTime().format(utc), true)
				.addField(fields.get(Messages.MUTUAL_GUILDS.name()),    user.getMutualGuilds().size() + "",       true)
				.addField(fields.get(Messages.JOIN_DATE.name()),        member.getJoinDate().format(utc),   true);
		
		if(user.getWikiaUser() != null && user.getWikiaNickname() != null) {
			builder .setThumbnail(user.getWikiaAvatarUrl())
					.setAuthor("Информация о " + user.getName() + "#" + user.getDiscriminator(),
							null,
							user.getAvatarUrl());
			user.getVerifications().values().stream()
					.max(Instant::compareTo)
					.ifPresent(instant -> builder.addField(fields.get(Messages.FIRST_VERIFICATION.name()),
											instant.atOffset(ZoneOffset.UTC).format(utc), true));
			
			builder .addField(fields.get(Messages.WIKIA_NICKNAME.name()), user.getWikiaNickname(), true)
					.addField(fields.get(Messages.WIKIA_ID.name()),       user.getWikiaId() + "",  true)
					.addField(fields.get(Messages.WIKIA_REGISTRATION.name()),
							user.getWikiaUser().getRegistration().atOffset(ZoneOffset.UTC).format(utc), false);
		} else {
			builder .setTitle("Информация о " + user.getName() + "#" + user.getDiscriminator())
					.addField(fields.get(Messages.WIKIA_REGISTRATION.name()), fields.get(Messages.WIKIA_USER_NOT_BINDED.name()), true);
			
		}
		if(user.getExternalUsername() != null)
			builder.addField(fields.get(Messages.EXTERNAL_NICKNAME.name()), user.getExternalUsername(), true);
		
		final long end = System.nanoTime();
		String timeTaken = String.format("%.3f ms", (end - start) / 1_000_000.0);
		if(end - start < 1_000_000)
			timeTaken = String.format("%.3f μs", (end - start) / 1_000.0);
			
		builder.setFooter(TemplateProcessor.process(fields.get(Messages.TIME_TAKEN.name()), Map.of("time", timeTaken)),
				event.getJDA().getSelfUser().getAvatarUrl());
		
		responseChannel.sendMessage(builder.build()).queue();
		LOGGER.traceExit();
	}
	
	@SubCommand(subCommand = "brief")
	public void printBriefInfo(CommandEvent event) {
		final EnhancedGuild guild = event.getEnhancedGuild();
		final MessageChannel channel = event.getChannel();
		final Map<String, String> fields = MESSAGES.get(guild.getLocale());
		
		// TODO: Don't forget to hook messages provider or templates here
		channel.sendTyping().complete();
		findMemberAndEnhance(event.getEnhancedUsersProvider(), guild, String.join(" ", event.getArgs()))
				.ifPresentOrElse(
						user -> {
							if (user.isBot())
								channel.sendMessage(guild.getMessage(BotMessages.ITS_JUST_A_BOT)).queue();
							else if(user.getWikiaUser() != null)
								channel.sendMessage(user.getName() + ", никнейм на ФЭНДОМе: **" + user.getWikiaNickname() + "**").queue();
							else
								channel.sendMessage(user.getName() + ", " + fields.get(Messages.WIKIA_USER_NOT_BINDED.name())).queue();
						},
						()   -> channel.sendMessage(guild.getMessage(BotMessages.USER_NOT_FOUND)).queue());
	}
	
	@SubCommand(subCommand = "online")
	public void printOnlineAndLast(CommandEvent event) {
		final EnhancedGuild guild = event.getEnhancedGuild();
		final MessageChannel channel = event.getChannel();
		
		findMemberAndEnhance(event.getEnhancedUsersProvider(), guild, String.join(" ", event.getArgs()))
				.ifPresentOrElse(
						user -> {
							if (user.isBot())
								channel.sendMessage(guild.getMessage(BotMessages.ITS_JUST_A_BOT)).queue();
							else if(user.getLastGoneOffline() != null)
								channel.sendMessage(user.getName() + ". Статус: **" + guild.getMember(user).getOnlineStatus() + "**" +
										"\nПоследний раз онлайн: " + formatter.withLocale(guild.getLocale()).format(user.getLastGoneOffline())).queue();
						},
						()   -> channel.sendMessage("User not found.").queue());
	}
	
	@SubCommand(subCommand = "wikia")
	public void printWikiaUserInfo(CommandEvent event) {
	
	}
	
	@SubCommand(subCommand = "verifications", aliases = {"verif", "proof"})
	public void listVerifications(CommandEvent event) {
		final EnhancedGuild guild = event.getEnhancedGuild();
		final MessageChannel channel = event.getChannel();
		
		findMemberAndEnhance(event.getEnhancedUsersProvider(), guild, String.join(" ", event.getArgs()))
				.ifPresentOrElse(
						user -> {
							if (user.isBot())
								channel.sendMessage(guild.getMessage(BotMessages.ITS_JUST_A_BOT_DEBTS)).queue();
							else if(user.getVerifications().size() > 0)
								channel.sendMessage(String.join("\n", user.getVerifications().entrySet().stream()
										.map(entry -> event.getJDA().getGuildById(entry.getKey()).getName() + ": " + formatter.format(entry.getValue())).toArray(String[]::new)))
										.queue();
						},
						()   -> {
							if(event.getArgs().size() != 0)
								channel.sendMessage(guild.getMessage(BotMessages.USER_NOT_FOUND)).queue();
							else
								channel.sendMessage(String.join("\n", event.getAuthor().getVerifications().entrySet().stream()
										.map(entry -> entry.getKey() + " " + formatter.format(entry.getValue())).toArray(String[]::new))).queue();
						});
	}
	
	@Override
	@SubCommand(subCommand = "help")
	public void printHelp(CommandEvent event) {
		event.getChannel().sendMessage("Command usage: `" + event.getEnhancedGuild().getCommandPrefix()
				+ "userinfo [username]`.\n You can also not specify a username to get info about yourself.").queue();
	}
	
	private enum Messages {
		DISCORD_CREATION,
		EXTERNAL_NICKNAME,
		ID,
		FIRST_VERIFICATION,
		JOIN_DATE,
		MUTUAL_GUILDS,
		NICKNAME,
		ONLINE_STATUS,
		
		LAST_ONLINE,
		
		WIKIA_ID,
		WIKIA_NICKNAME,
		WIKIA_REGISTRATION,
		
		NOT_SET,
		
		// TODO: Replace this one with Default message
		WIKIA_USER_NOT_BINDED,
		TIME_TAKEN;
		
		@Override
		public String toString() {
			return this.name();
		}
	}
}
