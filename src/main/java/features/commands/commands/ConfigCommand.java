package features.commands.commands;

import features.commands.annotations.SubCommand;
import features.commands.events.CommandEvent;
import model.entities.EnhancedGuild;
import model.dbfields.BotFeatures;
import model.dbfields.BotMessages;
import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.Permission;
import net.dv8tion.jda.core.Region;
import net.dv8tion.jda.core.entities.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import providers.EnhancedGuildsProvider;
import providers.EnhancedUsersProvider;

import java.util.*;
import java.util.stream.Collectors;

import static features.commands.utils.JDACustomUtilities.findRole;
import static features.commands.utils.JDACustomUtilities.findTextChannel;
import static features.commands.commands.Colors.*;

public class ConfigCommand extends AbstractCommand {
	private static final Logger LOGGER = LogManager.getLogger(ConfigCommand.class);
	
	private static final EnhancedGuildsProvider ENHANCED_GUILDS_PROVIDER = EnhancedGuildsProvider.getInstance();
	private static final EnhancedUsersProvider  ENHANCED_USERS_PROVIDER  = EnhancedUsersProvider.getInstance();
	
	public ConfigCommand() {
		this.name = "config";
		this.aliases = new String[]{"cfg", "conf"};
		this.description = "command used for simple config";
		this.argsCount = -1;
		this.memberPermissions = new Permission[]{Permission.MANAGE_SERVER};
	}
	
	public ConfigCommand(String name, String description, String[] aliases, boolean usesTransliteration, int argsCount) {
		super(name, description, aliases, usesTransliteration, argsCount);
	}
	
	// TODO: Rewrite this shit
	@Override
	public void execute(CommandEvent event) {
		LOGGER.traceEntry();
		final Message message = event.getMessage();
		final MessageChannel currentChannel = event.getChannel();
		
		final String messageContent = message.getContentRaw().trim().replaceAll("(?: )+", " ");
		
		final EnhancedGuild enhancedGuild = event.getEnhancedGuild();
		
		final EmbedBuilder embedResponse = new EmbedBuilder();
		embedResponse.setColor(NEUTRAL);
		final LinkedList<String> args = event.getArgs();
		
		if(args.size() == 0) {
			printHelp(event);
		} else if (args.get(0).equals("welcome")) {
			if(args.get(1).equals("help")) {
				embedResponse.setTitle("Помощь для команды `config welcome`").setDescription(
						"`welcome [help]` - вывод данного сообщения" +
								"\n`welcome status` - вывод текущей конфигурации." +
								"\n`welcome set channel <канал>` - установка канала для приветствий. " +
								"В качестве аргумента <канал> может быть название, \"упоминание\" канала или его ID. " +
								"Если найдено больше 1 канала, установлен будет первый попавшийся." +
								"\n`welcome set message <сообщение>` - установка сообщения для приветствия. " +
								"\nДоступны переменные `${usermention}`, `${servername}`. " +
								"\nВ качестве сообщения принимается всё в точности так, как идёт после \"`message`\", " +
								"никаких дополнительных скобок и знаков не требуется.");
			} else if (args.get(1).equalsIgnoreCase("status")) {
				embedResponse.setDescription("Канал для приветствий: <#" + enhancedGuild.getWelcomeChannel().getId() + ">" +
						"\nСообщение: \"" + enhancedGuild.getWelcomeMessage() + "\"");
			} else if (args.get(1).equalsIgnoreCase("message")) {
				embedResponse.setDescription("Канал для приветствий: <#" + enhancedGuild.getWelcomeChannel().getId() + ">" +
						"\nСообщение: \"" + enhancedGuild.getWelcomeMessage() + "\"");
			} else if (args.get(1).equals("set")) {
				if(args.get(2).equalsIgnoreCase("channel")) {
					TextChannel old = enhancedGuild.getWelcomeChannel();
					enhancedGuild.setWelcomeChannel(findTextChannel(enhancedGuild, args.get(3)));
					if (enhancedGuild.getWelcomeChannel().getIdLong() != old.getIdLong())
						embedResponse.setColor(SUCCESS).addField(":white_check_mark: Установлен канал для приветствий: ",
								enhancedGuild.getWelcomeChannel().getName(), false);
				} else if (args.get(2).equalsIgnoreCase("message")) {
					String finalMessage = messageContent.substring(messageContent.indexOf(args.get(3)), messageContent.length() - 1);
					enhancedGuild.setWelcomeMessage(finalMessage);
					embedResponse.setColor(SUCCESS).addField(":white_check_mark: Установлено приветствие", finalMessage, false);
				}
			} else {
				embedResponse.setColor(FAILURE).setDescription(":x: Команда не найдена: " + args.get(1) + " " + args.get(2));
			}
		} else if (args.get(0).equalsIgnoreCase("verify")) {
			if (args.size() == 1 || args.get(1).equalsIgnoreCase("help")) {
				embedResponse.setTitle("Помощь для команды `config verify`").setDescription(
						"`verify [help]` - вывод данного сообщения" +
								"\n`verify status` - вывод текущей конфигурации." +
								"\n`verify disable` - отключение верификации. Конфигурация сохранится до следующего подключения." +
								"\n`verify enable` - подключение верификации. Будет использована старая конфигурация. Чтобы узнать её, напишите `!!cfg verify status`" +
								"\n`verify set channel <канал>` - установка канала для прослушки ссылок команды `verify`. " +
								"В качестве аргумента <канал> может быть название, \"упоминание\" канала или его ID. " +
								"Если найдено больше 1 канала, установлен будет первый попавшийся." +
								"\n`verify set role <роль1> ...` - установка ролей, которые присваиваются после верификации. " +
								"Указывать частично или полностью названия или ID через пробел." +
								"\n`verify set delay <целое число>` - установка времени в секундах, спустя которое фактически присваиваются роли. " +
								"'0' по-умолчанию." +
								"\n`verify set timeout <целое число>` - установка времени в часах, дающееся пользователю для предоставления ссылки. " +
								"Значения 0 и меньше - отключение кика по таймауту." +
								"\n`verify set message <сообщение>` - установка сообщения после успешной верификации. " +
								"Доступные переменные ${usermention}, ${servername}.");
			} else if (args.get(1).equalsIgnoreCase("status")) {
				embedResponse.setTitle("Текущая конфигурация модуля верификации:").setDescription(
						"Канал для верификации: <#" + enhancedGuild.getVerificationChannel().getId() + ">" +
								"\nРоли, выдающиеся при успешной верификации: " + (enhancedGuild.getVerificationRolesString() == null ? "Отсутствуют" : enhancedGuild.getVerificationRolesString()) +
								"\n");
			} else if (args.get(1).equalsIgnoreCase("disable")) {
				enhancedGuild.setBotFeaturesDisabled(BotFeatures.WikiaVerification, BotFeatures.UserVerificationCleanup, BotFeatures.UserVerificationTimeout, BotFeatures.UserVerificationNotify);
				embedResponse.setColor(SUCCESS).setDescription(":white_check_mark: Верификация полностью отключена.");
			} else if (args.get(1).equalsIgnoreCase("enable")) {
				enhancedGuild.setBotFeaturesEnabled(BotFeatures.WikiaVerification, BotFeatures.UserVerificationCleanup, BotFeatures.UserVerificationTimeout, BotFeatures.UserVerificationNotify);
				embedResponse.setColor(SUCCESS).setDescription(":white_check_mark: Верификация полностью подключена.");
			}
			else if (args.get(1).equals("set")) {
				if(args.size() == 2) {
					embedResponse.setColor(FAILURE).setDescription("Укажите параметр для установки.\nПомощь: `!!cfg verify help`");
				} else if (args.get(2).equalsIgnoreCase("channel")) {
					long oldId = enhancedGuild.getVerificationChannel().getIdLong();
					enhancedGuild.setVerificationChannel(findTextChannel(enhancedGuild, args.get(3)));
					if (enhancedGuild.getVerificationChannel().getIdLong() != oldId)
						embedResponse.setColor(SUCCESS).setDescription(":white_check_mark: Установлен канал для верификации: <#" + enhancedGuild.getVerificationChannel().getId() + ">");
				} else if (args.get(2).equalsIgnoreCase("role")) {
					ArrayList<Role> oldRoles = enhancedGuild.getVerificationRoles();
					for (int i = 2; i < args.size(); i++) {
						Role role = findRole(enhancedGuild, args.get(i));
						enhancedGuild.addVerificationRole(role);
					}
					if (Collections.disjoint(enhancedGuild.getVerificationRoles(), oldRoles))
						embedResponse.setColor(SUCCESS).setDescription(":white_check_mark: Установлены роли для верификации: "
								+ String.join(" ", enhancedGuild.getVerificationRoles().stream().map(Role::getName).toArray(String[]::new)));
					
				} else if (args.get(2).equalsIgnoreCase("delay")) {
				
				} else if (args.get(2).equalsIgnoreCase("timeout")) {
					try {
						int timeout = Integer.parseInt(args.get(3));
						
						enhancedGuild.setVerificationTimeout(timeout);
					} catch (NumberFormatException e) {
					
					}
				} else {
					embedResponse.setColor(FAILURE).setDescription(":x: Параметр не найден: " + args.get(1) + " " + args.get(2));
				}
			} else {
				embedResponse.setColor(FAILURE).setDescription(":x: Команда не найдена: " + args.get(0) + " " + args.get(1));
			}
		} else if (args.get(0).equalsIgnoreCase("locale")) {
			if (args.size() == 1 || args.get(1).equalsIgnoreCase("help")) {
				embedResponse.setTitle("Помощь для `config locale`:").setDescription(
						"`locale list` - вывод списка доступных локалей (языков) стандартных сообщений." +
								"\n`locale set <locale>` - установка локали (языка) стандартных сообщений бота. Например, русский, ru_RU, uk_UA, en." +
								"\n`locale reset` - автоматический выбор языка. Если сервер использует регион \"Россия,\" будет выбран русский язык.");
			} else if (args.get(1).equalsIgnoreCase("list") || args.get(1).equalsIgnoreCase("--list")) {
				embedResponse.setTitle("Available locales:").setDescription("English - en\nРусский (Russian) - ru_RU\nУкраїнська (Ukrainian) - ua");
			} else if (args.get(1).toLowerCase().equals("set")) {
				String old = enhancedGuild.getLocaleString();
				switch (args.get(2).toLowerCase()) {
					case "ru":
					case "ru_ru":
					case "russian":
					case "русский":
						enhancedGuild.setLocale(new Locale("ru", "RU"));
						break;
					case "ua":
					case "ukrainian":
					case "uk_ua":
					case "українська":
						enhancedGuild.setLocale(new Locale("uk", "UA"));
						break;
					case "en":
					case "en_en":
					case "en_us":
					case "en_uk":
					case "english":
					case "default":
						enhancedGuild.setLocale(Locale.ENGLISH);
						break;
				}
				if(!old.equals(enhancedGuild.getLocaleString()))
					embedResponse.setColor(SUCCESS).setDescription(":white_check_mark: Установлен язык: " + enhancedGuild.getLocaleReadable());
			} else if (args.get(1).equalsIgnoreCase("status")) {
				embedResponse.setDescription("Текущий язык: " + enhancedGuild.getLocaleReadable());
			} else if (args.get(1).equalsIgnoreCase("reset") ||
					args.get(1).equalsIgnoreCase("restore")) {
				enhancedGuild.setLocale(enhancedGuild.getRegion() == Region.RUSSIA ? new Locale("ru", "RU") : Locale.ENGLISH);
			} else {
				embedResponse.setColor(FAILURE).setDescription(":x: Команда не найдена: " + args.get(1));
			}
		} else if (args.get(0).equalsIgnoreCase("prefix")) {
			if(args.size() == 0) {
				embedResponse.setColor(NEUTRAL).setDescription("Команда для конфигурации префикса.");
			} else if (args.get(1).equalsIgnoreCase("reset")) {
				enhancedGuild.setCommandPrefix("!!");
				embedResponse.setColor(SUCCESS).setDescription(":white_check_mark:  Установлен префикс: `!!`");
			} else if (args.get(1).equalsIgnoreCase("set") && args.get(2).length() <= 3) {
				enhancedGuild.setCommandPrefix(args.get(2));
				embedResponse.setColor(SUCCESS).setDescription(":white_check_mark:  Установлен префикс: `" + args.get(2) + "`");
			} else if (args.get(1).length() > 3) {
				embedResponse.setColor(FAILURE).setDescription(":x: Префикс не может быть длиннее трех символов.");
			} else {
				embedResponse.setColor(FAILURE).setDescription(":x: Команда не найдена: " + args.get(1) + " " + args.get(2));
			}
		} else {
			embedResponse.setColor(FAILURE).setDescription(":x: Команда не найдена: " + args.get(0));
		}
		if( ! embedResponse.equals(new EmbedBuilder()))
			currentChannel.sendMessage(embedResponse.build()).queue();
		ENHANCED_GUILDS_PROVIDER.update(enhancedGuild);
	}
	
	@Override
	@SubCommand(subCommand = "help", aliases = {"-h", "--help"})
	public void printHelp(CommandEvent event) {
		final EmbedBuilder embedResponse = new EmbedBuilder();
		embedResponse
				.setColor(NEUTRAL)
				.setTitle("Помощь для команды `config`:")
				.setDescription("Данная команда используется для конфигурации переменных бота для данного сервера. " +
						"\n`help` - вывод данного сообщения." +
						"\n\n`welcome [help]` - помощь по конфигурации приветствий. По-умолчанию, приветствия в основном канале." +
						"\n\n`verify [help]` - конфигурация канала для прослушки верификации. По-умолчанию, канал для приветствий. " +
						"\n\n`locale [help]` - вывод списка доступных локалей (языков) стандартных сообщений." +
						"\n\n`prefix set <prefix>` - установка префикса команд для сервера. Не более трех знаков. " +
						"Возможность использовать префикс по-умолчанию ('!!') остается." +
						"\n\n`wiki set <wiki url>` - установка вики, к которой принадлежит сервер. " +
						"Менять этот параметр имеет право только администратор сервера с привязанным Викия аккаунтом " +
						"с правами 'sysop' на указанной вики и верифицированный с помощью `!!verify [sysop | admin] <url>`." +
						"\n`wiki set <name>` - установка своего удобочитаемого названия вики. Устанавливается автоматически после привязки к вики.");
		
		event.getChannel().sendMessage(embedResponse.build()).queue();
	}
	
	@SubCommand(subCommand = "status")
	public void status(CommandEvent event) {
		final EnhancedGuild enhancedGuild = event.getEnhancedGuild();
		final EmbedBuilder embedResponse = new EmbedBuilder();
		
		embedResponse
				.setColor(NEUTRAL)
				.setTitle("Текущая конфигурация:", "https://iscaribot.xyz")
				.addField("Префикс:", "`" + enhancedGuild.getCommandPrefix() + "`", true)
				.addField("Язык", enhancedGuild.getLocaleReadable(), true)
				.addField("Канал для приветствий:", enhancedGuild.getWelcomeChannel().getName(), true)
				.addField("Канал для верификации:", enhancedGuild.getVerificationChannel().getName(), true)
				.addField("Таймаут верификации:",   enhancedGuild.getVerificationTimeout() + " часов", true)
				.addField("Роли для верификации:",  enhancedGuild.getVerificationRolesString() + " ", false)
				.setFooter("Версия бота: 0.5.0", event.getJDA().getSelfUser().getAvatarUrl());
		
		String messages = String.join(", ", enhancedGuild.getBotMessages()   .keySet()  .stream().map(BotMessages::name).collect(Collectors.toList()));
		String features = String.join(", ", enhancedGuild.getBotFeatures().entrySet().stream().filter(Map.Entry::getValue).map(entry -> entry.getKey().name()).collect(Collectors.toList()));
		
		embedResponse.addField("Кастомизированные сообщения: ",  messages.isEmpty() ? "Отсутствуют" : messages, false)
				.addField("Подключенные фичи: ", features.isEmpty() ? "Отсутствуют" : features, false);
		event.getChannel().sendMessage(embedResponse.build()).queue();
	}
	
	//@SubCommand(subCommand = "verify")
	public void verify(CommandEvent event) {
	
	}
	
	//@SubCommand(subCommand = {"verify", "set"})
	public void verifySetter(CommandEvent event) {
	
	}
	
	//@SubCommand(subCommand = "welcome")
	public void welcome(CommandEvent event) {
	
	}
	
	//@SubCommand(subCommand = {"welcome", "set"})
	public void welcomeSetter(CommandEvent event) {
	
	}
	
	//@SubCommand(subCommand = "locale")
	public void locale(CommandEvent event) {
	
	}
}
