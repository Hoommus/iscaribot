package features.commands.commands;

import features.commands.annotations.SubCommand;
import features.commands.annotations.SubCommandType;
import features.commands.events.CommandEvent;
import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.MessageBuilder;
import net.dv8tion.jda.core.Permission;
import net.dv8tion.jda.core.entities.Member;
import net.dv8tion.jda.core.entities.MessageEmbed;
import net.dv8tion.jda.core.entities.TextChannel;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.LocaleUtils;
import providers.DefaultMessagesProvider;

import java.lang.reflect.Method;
import java.util.*;

@SuppressWarnings("WeakerAccess")
public abstract class AbstractCommand {
	protected String name = "";
	protected String description = "no help";
	protected String friendlyExplanation;
	protected String manpage;
	protected String example;
	protected String[] aliases = new String[0];
	protected boolean allowsTransliteration = false;
	
	protected Permission[] memberPermissions = new Permission[0];
	protected Permission[] botPermissions = new Permission[]{Permission.MESSAGE_WRITE, Permission.MESSAGE_READ};
	
	protected String baseResourceName = null;
	protected static final String[] locales = {"en", "ru_RU", "ua"};
	protected final Map<Locale, Map<String, String>> MESSAGES = new HashMap<>();
	
	private final Map<Locale, Map<String, String>> MAN = new HashMap<>();
	
	protected static final DefaultMessagesProvider DEFAULT_MESSAGES = DefaultMessagesProvider.getInstance();
	
	protected int argsCount = -1;
	
	public AbstractCommand() {
	
	}
	
	public AbstractCommand(String name, String description, String[] aliases, boolean usesTransliteration, int argsCount) {
		this.name = name;
		this.description = description;
		this.aliases = aliases;
		this.allowsTransliteration = usesTransliteration;
		this.argsCount = argsCount;
	}
	
	public abstract void execute(CommandEvent event);
	
	public boolean checkMemberPermissions(Member member) {
		return Collections.disjoint(member.getPermissions(), Arrays.asList(memberPermissions));
	}
	
	public boolean checkPermissions(TextChannel messageChannel) {
		//return Collections.disjoint(messageChannel., Arrays.asList(memberPermissions));
		return false;
	}
	
	public boolean isCalledBy(String s) {
		return name.equals(s) || ArrayUtils.contains(aliases, s);
	}
	
	public boolean isAdmin() {
		List<Permission> permissions = Arrays.asList(memberPermissions);
		return permissions.contains(Permission.ADMINISTRATOR) ||
				permissions.contains(Permission.KICK_MEMBERS) ||
				permissions.contains(Permission.BAN_MEMBERS)  ||
				permissions.contains(Permission.MANAGE_SERVER);
	}
	
	@SubCommand(subCommand = {"help"}, type = SubCommandType.HELP)
	public void printHelp(CommandEvent event) {
		this.printGeneratedHelp(event);
	}
	
	@SubCommand(subCommand = {"man"}, type = SubCommandType.HELP)
	public final void printBriefMan(CommandEvent event) {
		this.printGeneratedHelp(event);
	}
	
	@SuppressWarnings("StringConcatenationInsideStringBufferAppend")
	public final void printGeneratedHelp(CommandEvent event) {
		final StringBuilder messageBuilder = new StringBuilder("Command **" + name + "**: *" + description + "*");
		final StringBuilder availableSubs = new StringBuilder();
		final ArrayList<String> subcommands = new ArrayList<>();
		for (Method method : this.getClass().getMethods()) {
			if(method.getAnnotation(SubCommand.class) != null) {
				for (String sub : method.getAnnotation(SubCommand.class).subCommand()) {
					if(sub.equals("help") || sub.equals("man"))
						continue;
					subcommands.add(sub);
				}
			}
		}
		
		if (this.aliases.length > 0)
			messageBuilder.append("\n**Aliases**:\n`" + String.join("` `", this.aliases) + "`");
		else
			messageBuilder.append("\n**Aliases**:\nNo aliases.");
		
		if(subcommands.size() > 0)
			messageBuilder.append("\n**Available subcommands**:\n`"
					+ String.join("`, `", subcommands) + "`");
		
		messageBuilder.append("\n**Permissions I require**:\n`" +
				String.join(" ", Arrays.stream(botPermissions).map(Enum::name).toArray(String[]::new)) + "`");
		messageBuilder.append("\n**Permissions member must have**: \n`" +
				String.join(" ", Arrays.stream(this.memberPermissions).map(Enum::name).toArray(String[]::new)) + "`");
		
		event.getChannel().sendMessage(messageBuilder.toString()).queue();
	}
	
	public MessageEmbed getHelpEmbed(CommandEvent event) {
		return new EmbedBuilder().setTitle("Help for command **" + name + "**").setDescription(description).build();
	}
	
	protected void hookLocalisation(Class<? extends Enum> messageKeys) {
		Objects.requireNonNull(baseResourceName, "You should provide a resource name.");
		
		for (String l : locales) {
			final Locale locale = LocaleUtils.toLocale(l);
			final ResourceBundle bundle = ResourceBundle.getBundle("messages/" + baseResourceName, locale);
			final HashMap<String, String> map = new HashMap<>();
			
			final String[] values = Arrays.stream(messageKeys.getEnumConstants())
					.map(Enum::name).toArray(String[]::new);
					
			for (String key : values) {
				try {
					map.put(key, bundle.getString(key));
				} catch (MissingResourceException ignore) {
					ignore.printStackTrace();
				}
			}
			MESSAGES.put(locale, map);
		}
	}
	
	public String getName() {
		return name;
	}
	
	public String getDescription() {
		return description;
	}
	
	public String[] getAliases() {
		return aliases;
	}
	
	public boolean allowsTransliteration() {
		return allowsTransliteration;
	}
	
	public String getManpage() {
		return manpage;
	}
	
	public String getExample() {
		return example;
	}
	
	public Permission[] getMemberPermissions() {
		return memberPermissions;
	}
	
	public int getArgsCount() {
		return argsCount;
	}
}
