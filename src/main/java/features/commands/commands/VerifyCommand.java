package features.commands.commands;

import com.google.gson.JsonIOException;
import com.google.gson.JsonSyntaxException;
import features.commands.annotations.SubCommand;
import features.commands.events.CommandEvent;
import features.commands.events.CommandsListener;
import model.entities.EnhancedGuild;
import model.entities.EnhancedUser;
import model.dbfields.BotFeatures;
import model.dbfields.BotMessages;
import net.dv8tion.jda.core.MessageBuilder;
import net.dv8tion.jda.core.Permission;
import net.dv8tion.jda.core.entities.Member;
import net.dv8tion.jda.core.entities.Message;
import net.dv8tion.jda.core.entities.MessageChannel;
import net.dv8tion.jda.core.exceptions.PermissionException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import providers.DefaultMessagesProvider;
import wikia.WikiaAPICall;
import wikia.WikiaUser;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URLDecoder;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static providers.TemplateProcessor.process;

public class VerifyCommand extends AbstractCommand {
	private static final String WIKIA_GENERAL_LINK_PATTERN = "https?://(?:[a-zA-Z]{2}\\.)?[a-zA-Z-]+\\.wikia\\.com/wiki/\\S+:\\S{2,}";
	private static final String WIKIA_NICKNAME_EXTRACTOR_PATTERN = "https?://(?:[a-zA-Z]{2}\\.)?[a-zA-Z-]+\\.wikia\\.com/wiki/(?:Стена_обсуждения|Участник|User|User_talk):(?:%20)?";
	// TODO: Consider replacing all this with Matcher
	private static final String WIKIA_LINK_TRIMMER = "\\.wikia\\.com/wiki/\\S+:\\S{2,}";
	private static final String LINK_PARAMETERS_PATTERN = "\\?(?:\\w+=\\S+)(?:&\\w=.+)*";
	
	private static final DefaultMessagesProvider MESSAGES_PROVIDER = DefaultMessagesProvider.getInstance();
	
	private static final Logger LOGGER = LogManager.getLogger(VerifyCommand.class);
	
	public VerifyCommand() {
		name = "verify";
		aliases = new String[]{"bind", "verifu", "verification", "proof", "vrf"};
		botPermissions = new Permission[]{Permission.MESSAGE_READ, Permission.MESSAGE_WRITE,
				Permission.MANAGE_PERMISSIONS, Permission.MANAGE_ROLES};
		description = "Used to bind your Wikia Account or any nickname to your Discord account.";
		
		baseResourceName = "verify_command";
		hookLocalisation(Messages.class);
	}
	
	@Override
	public void execute(CommandEvent event) {
		final EnhancedGuild  enhancedGuild   = event.getEnhancedGuild();
		final MessageChannel responseChannel = event.getChannel();
		final LinkedList<String> args = event.getArgs();
		final Locale locale = enhancedGuild.getLocale();
		final Member member = event.getMember();
		// TODO: Replace standart messages with customized from EnhancedGuild data
		final Map<String, String>     messages    = MESSAGES.get(locale);
		final HashMap<String, String> messageKeys = new HashMap<>();
		messageKeys.put("usermention", member.getAsMention());
		messageKeys.put("username",    member.getUser().getName());
		messageKeys.put("guildname",   enhancedGuild.getName());
		messageKeys.put("prefix",      enhancedGuild.getCommandPrefix());
		
		if (event.getEnhancedUser().isVerified() && event.getArgs().size() == 0) {
			messageKeys.put("wikianame", event.getEnhancedUser().getWikiaNickname());
			
			if(Collections.disjoint(event.getMember().getRoles(), enhancedGuild.getVerificationRoles())) {
				if (member.hasPermission(Permission.MANAGE_SERVER)
						|| member.hasPermission(Permission.ADMINISTRATOR))
					responseChannel.sendMessage(process(messages.get(Messages.ALREADY_VERIFIED_VERBOSE_ADMIN.name()), messageKeys)).queue();
				else
					responseChannel.sendMessage(process(messages.get(Messages.ALREADY_VERIFIED_VERBOSE.name()), messageKeys)).queue();
			} else
				enhancedGuild.getController().addRolesToMember(member, enhancedGuild.getVerificationRoles()).queue();
			return;
		}
		
		try {
			if (args.size() == 0
					|| !args.get(0).matches(WIKIA_GENERAL_LINK_PATTERN)
					||  args.get(0).equalsIgnoreCase("help")) {
				responseChannel.sendMessage(process(messages.get(Messages.COMMAND_USAGE.name()), messageKeys)).queue();
				return;
			}
		} catch (ArrayIndexOutOfBoundsException index) {
			// what?
			responseChannel.sendMessage("Неожиданная ошибка в верификации. Что-то точно идёт не так.").queue();
			CommandsListener.LOGGER.fatal("ArrayIndexOutOfBoundsException in verify().");
			return;
		}
		
		String urlToDecode = args.get(0).replaceAll("%20", "").replaceAll("\\[]","");
		
		try {
			// Cleaning url from any parameters
			urlToDecode = URLDecoder.decode(urlToDecode, "UTF-8").replaceAll(LINK_PARAMETERS_PATTERN, "");
		} catch (UnsupportedEncodingException e) {
			responseChannel.sendMessage(messages.get(Messages.ENCODE_PROBLEM.name())).queue();
			return;
		}
		
		final MessageBuilder response = new MessageBuilder();
		final String wikiAddress   = urlToDecode.replaceAll(WIKIA_LINK_TRIMMER, "").replaceAll("http(?:s)?://", "");
		final String wikiaNickname = urlToDecode.replaceAll(WIKIA_NICKNAME_EXTRACTOR_PATTERN, "");
		try {
			if(!event.getEnhancedUser().getVerifications().containsKey(enhancedGuild.getGuildId())) {
				final WikiaUser wikiaUser = WikiaAPICall.getWikiaUserByName(wikiAddress, wikiaNickname);
				
				if (wikiaUser == null) {
					responseChannel.sendMessage(messages.get(Messages.USER_MISSING.name())).queue();
					return;
				}
				
				if(event.getEnhancedUsersProvider().checkIfWikiaUserBinded(wikiaUser)) {
					responseChannel.sendMessage(process(messages.get(Messages.WIKIA_ACCOUNT_ALREADY_BINDED.name()), messageKeys)).queue();
					return;
				}
				
				response.append(process(messages.get(Messages.SUCCESS.name()), messageKeys));
				responseChannel.sendMessage(response.build()).queue();
				
				event.getEnhancedGuild().getController()
						.addRolesToMember(member, enhancedGuild.getVerificationRoles()).queueAfter(enhancedGuild.getVerificationTimeout(), TimeUnit.MILLISECONDS);
				
				// Calling update, because EnhancedUser should already be created in database
				// So just appending wikiauser here
				event.getEnhancedUsersProvider().update(
						event.getEnhancedUser()
								.setWikiaUser(wikiaUser)
								.addVerification(enhancedGuild.getGuildId(), Instant.now()));
				LOGGER.info(" [" + event.getEnhancedUser().getName() + "] verified as '" + wikiaUser.getName() + "'");
			}
			
		} catch (MalformedURLException malformed) {
			response.append(process(messages.get(Messages.LINK_MISTAKE.name()), messageKeys));
			responseChannel.sendMessage(response.build()).queue();
		} catch (PermissionException permission) {
			CommandsListener.LOGGER.error("Insufficient rights.\n" + permission.getMessage());
			responseChannel.sendMessage(MESSAGES_PROVIDER.getMessage(BotMessages.PERMISSION_LACK_ROLES, locale)).queue();
		} catch (ArrayIndexOutOfBoundsException outOfBounds) {
			outOfBounds.printStackTrace();
		} catch (IOException io) {
			CommandsListener.LOGGER.error("IOException. Troubles with Wikia API? " + io.getMessage());
			CommandsListener.LOGGER.error("Problem URL: " + urlToDecode);
			CommandsListener.LOGGER.error("Extracted wiki: " + wikiAddress + ". Extracted nickname: " + wikiaNickname);
			responseChannel.sendMessage(messages.get(Messages.WIKIA_API_TROUBLES.name())).queue();
		} catch (JsonIOException | JsonSyntaxException json) {
			responseChannel.sendMessage(messages.get(Messages.WIKI_MISSING.name())).queue();
		}
	}
	
	@SubCommand(subCommand = {"custom"})
	public void custom(CommandEvent event) {
		external(event);
	}
	
	@SubCommand(subCommand = {"external"})
	public void external(CommandEvent event) {
		final EnhancedGuild enhancedGuild = event.getEnhancedGuild();
		final EnhancedUser enhancedUser = event.getEnhancedUser();
		if (!enhancedGuild.getFeature(BotFeatures.UserNicknameBinding)) {
			enhancedUser.setExternalUsername(String.join(" ",
					event.getArgs().stream()
							.filter(s -> !s.matches("@^https?://[^\\s/$.?#].[^\\s]*$@iS"))
							.toArray(String[]::new)));
			event.getEnhancedUsersProvider().update(enhancedUser);
		}
	}
	
	@SubCommand(subCommand = {"wikia"})
	public void wikia(CommandEvent event) {
		execute(event.reduceArgs());
	}
	
	@SubCommand(subCommand = {"report"}, permissions = {Permission.MANAGE_SERVER, Permission.BAN_MEMBERS})
	public void report(CommandEvent commandEvent) {
		final Message message = commandEvent.getChannel().sendMessage("Ожидайте...").complete();
		final MessageBuilder builder = new MessageBuilder();
		CompletableFuture.runAsync(() -> {
			final long nanoStart = System.nanoTime();
			final long verifiedUsers = commandEvent.getEnhancedGuild().getMembers().stream()
					.map(Member::getUser)
					.filter(user -> !user.isBot())
					.filter(user -> commandEvent.getEnhancedUsersProvider().enhance(user).isVerified())
					.count();
			final long notVerifiedUsers   = commandEvent.getEnhancedGuild().getMembers().size() - verifiedUsers;
			final long totalVerifiedUsers = commandEvent.getEnhancedUsersProvider().countAllVerifiedUsers();
			final long waiting = commandEvent.getEnhancedGuild().getMembers().stream()
					.filter(member -> !member.getUser().isBot())
					.filter(member -> member.getRoles().size() == 0)
					.count();
			final long nanoTimespan = System.nanoTime() - nanoStart;
			builder.append("Верифицированных пользователей на сервере: ").append(String.valueOf(verifiedUsers))
					.append("\nВерифицированных пользователей на других серверах: ").append(String.valueOf(totalVerifiedUsers - verifiedUsers))
					.append("\nНеверифицированных пользователей: ").append(String.valueOf(notVerifiedUsers))
					.append("\nВсего верифицировано за всё время работы: ").append(String.valueOf(totalVerifiedUsers))
					.append("\n\nУчастников без ролей: ").append(String.valueOf(waiting))
					.append(String.format("\n`Затрачено времени: %.2f ms`", nanoTimespan / 1_000_000.0));
			message.editMessage(builder.build()).complete();
		});
	}
	
	@Override
	@SubCommand(subCommand = "help")
	public void printHelp(CommandEvent event) {
		Map<String, String> keys = Map.of("prefix", event.getEnhancedGuild().getCommandPrefix());
		event.getChannel().sendMessage(process(MESSAGES.get(event.getEnhancedGuild().getLocale()).get(Messages.COMMAND_USAGE.name()), keys)).queue();
	}
	
	private enum Messages {
		REJECT,
		ALREADY_VERIFIED,
		ALREADY_VERIFIED_VERBOSE,
		ALREADY_VERIFIED_VERBOSE_ADMIN,
		COMMAND_USAGE,
		ENCODE_PROBLEM,
		SUCCESS,
		LINK_MISTAKE,
		WIKIA_ACCOUNT_ALREADY_BINDED,
		WIKIA_API_TROUBLES,
		WIKI_MISSING,
		USER_MISSING,
		NOTIFY_BOTH_GENERIC,
		NOTIFY_WIKIA_GENERIC,
		NOTIFY_FIRST_THREAT,
		NOTIFY_SECOND_THREAT,
		KICK_REASON,
	}
}
