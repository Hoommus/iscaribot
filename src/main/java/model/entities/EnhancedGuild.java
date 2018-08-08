package model.entities;

import config.BotConfig;
import features.commands.commands.AbstractCommand;
import model.annotations.DiffIgnore;
import model.annotations.GuildField;
import model.dbfields.BotFeatures;
import model.dbfields.BotMessages;
import model.dbfields.GuildFields;
import net.dv8tion.jda.client.requests.restaction.pagination.MentionPaginationAction;
import net.dv8tion.jda.core.JDA;
import net.dv8tion.jda.core.Region;
import net.dv8tion.jda.core.entities.*;
import net.dv8tion.jda.core.managers.AudioManager;
import net.dv8tion.jda.core.managers.GuildController;
import net.dv8tion.jda.core.managers.GuildManager;
import net.dv8tion.jda.core.managers.GuildManagerUpdatable;
import net.dv8tion.jda.core.requests.RestAction;
import net.dv8tion.jda.core.requests.restaction.pagination.AuditLogPaginationAction;
import net.dv8tion.jda.core.utils.cache.MemberCacheView;
import net.dv8tion.jda.core.utils.cache.SnowflakeCacheView;
import org.bson.Document;
import providers.DefaultMessagesProvider;
import providers.TemplateProcessor;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.time.OffsetDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * This is a basic class that holds guild preferences: customized messages, locale, command prefix
 * and enabled features list.
 */
public class EnhancedGuild implements Guild {
	// TODO: consider replacing this crap somehow
	private static final DefaultMessagesProvider  DEFAULT_MESSAGES_PROVIDER  = DefaultMessagesProvider.getInstance();
	@DiffIgnore
	private Long guildId;
	@DiffIgnore
	private Guild  guild;
	
	private String localeString;
	@GuildField(inDB = GuildFields.locale)
	private Locale locale;
	@GuildField(inDB = GuildFields.command_prefix)
	private String commandPrefix;
	@GuildField(inDB = GuildFields.welcome_channel_id)
	private TextChannel welcomeChannel;
	@GuildField(inDB = GuildFields.verification_channel_id)
	private TextChannel verificationChannel;
	
	@GuildField(inDB = GuildFields.moderator_role_id)
	private Role moderator;
	@GuildField(inDB = GuildFields.moderator_assistant_role_id)
	private Role moderatorAssistant;
	
	@GuildField(inDB = GuildFields.verification_roles_ids)
	private ArrayList<Long> verificationRolesIds;
	
	private ArrayList<Role> verificationRoles;
	
	private int verificationTimeout;
	
	private EnumMap<BotMessages, String>  messages;
	private EnumMap<BotFeatures, Boolean> features;
	
	private ArrayList<String> issues;
	private ArrayList<AbstractCommand> disabledCommands;
	
	private EnhancedGuild() { }
	
	public EnhancedGuild(@Nonnull Guild guild) {
		this.guild = guild;
		guildId = guild.getIdLong();
		
		locale = guild.getRegion() == Region.RUSSIA ? new Locale("ru", "RU") : Locale.ENGLISH;
		
		commandPrefix = BotConfig.getPrefix();
		
		welcomeChannel = guild.getDefaultChannel();
		verificationChannel = welcomeChannel;
		verificationTimeout = 12;
		
		verificationRolesIds = new ArrayList<>();
		verificationRoles = new ArrayList<>();
		messages = new EnumMap<>(BotMessages.class);
		features = new EnumMap<>(BotFeatures.class);
		
		disabledCommands = new ArrayList<>();
	}
	
	public String getMessage(BotMessages message) {
		return messages.getOrDefault(message, DEFAULT_MESSAGES_PROVIDER.getMessage(message, locale));
	}
	
	public String getMessage(BotMessages message, Map<String, String> keys) {
		return TemplateProcessor.process(getMessage(message), keys);
	}
	
	public String getProcessedMessage(BotMessages message, Map<String, String> keys) {
		return getMessage(message, keys);
	}
	
	public EnumMap<GuildFields, Object> diff(EnhancedGuild other) {
		EnumMap<GuildFields, Object> diff = new EnumMap<>(GuildFields.class);
		
		if (this.equals(other))
			return diff;
		//noinspection StatementWithEmptyBody
		if ( ! this.guildId.equals(other.guildId)) {}
		if ( ! this.guild.equals(other.guild)) { throw new IllegalArgumentException("Should not perform diff of two different guilds."); }
		
		if ( ! this.locale.equals(other.locale))
			diff.put(GuildFields.locale, other.locale.toString());
		if ( ! this.commandPrefix.equals(other.commandPrefix))
			diff.put(GuildFields.command_prefix, other.commandPrefix);
		if (this.welcomeChannel.getIdLong() != other.welcomeChannel.getIdLong())
			diff.put(GuildFields.welcome_channel_id, other.welcomeChannel.getIdLong());
		if (this.verificationChannel != null && other.verificationChannel != null)
			if (this.verificationChannel.getIdLong() != other.verificationChannel.getIdLong())
				diff.put(GuildFields.verification_channel_id, other.getVerificationChannel().getIdLong());
		else if (this.verificationChannel == null && other.verificationChannel != null)
			diff.put(GuildFields.verification_channel_id, other.getVerificationChannel().getIdLong());
		
		if(this.verificationTimeout != other.verificationTimeout)
			diff.put(GuildFields.verification_pending_timeout, other.verificationTimeout);
		
		if (this.verificationRoles != null && other.verificationRoles != null) {
			if(!this.verificationRoles.equals(other.verificationRoles)) {
				List<Long> ids = other.verificationRoles.stream().map(ISnowflake::getIdLong).collect(Collectors.toList());
				diff.put(GuildFields.verification_roles_ids, ids);
			}
		}
		
		if ( ! this.messages.equals(other.messages)) {
			Document messages = new Document();
			other.messages.forEach((k, v) -> messages.put(k.name(), v));
			diff.put(GuildFields.messages, messages);
		}
		
		if ( ! this.messages.equals(other.messages)) {
			Document messages = new Document();
			other.messages.forEach((k, v) -> messages.put(k.name(), v));
			diff.put(GuildFields.messages, messages);
		}
		if ( ! this.features.equals(other.features)) {
			HashMap<String, Boolean> features = new HashMap<>();
			other.features.forEach((k, v) -> features.put(k.name(), v));
			diff.put(GuildFields.features, features);
		}
		
		return diff;
	}
	
	public EnhancedGuild setBotFeaturesEnabled(BotFeatures... features) {
		for(BotFeatures feature : features) {
			if (feature.isAvailable() && this.features.containsKey(feature))
				this.features.replace(feature, true);
			else this.features.put(feature, true);
		}
		return this;
	}
	
	public EnhancedGuild setBotFeaturesDisabled(BotFeatures... features) {
		for(BotFeatures feature : features) {
			if (this.features.containsKey(feature))
				this.features.replace(feature, false);
			else this.features.put(feature, false);
		}
		return this;
	}
	
	public boolean getFeature(BotFeatures feature) {
		return features.getOrDefault(feature, false);
	}
	
	public ArrayList<Long> getVerificationRolesIds() {
		return verificationRolesIds;
	}
	
	public EnhancedGuild setVerificationRolesIds(ArrayList<Long> verificationRolesIds) {
		this.verificationRolesIds = verificationRolesIds;
		return this;
	}
	
	public EnhancedGuild setGuild(@Nonnull Guild guild) {
		this.guild = guild;
		this.guildId = guild.getIdLong();
		return this;
	}
	
	public TextChannel getVerificationChannel() {
		return verificationChannel;
	}
	
	public EnhancedGuild setVerificationChannel(TextChannel verificationChannel) {
		this.verificationChannel = verificationChannel;
		return this;
	}
	
	public TextChannel getWelcomeChannel() {
		return welcomeChannel == null ? this.getDefaultChannel() : welcomeChannel;
	}
	
	public EnhancedGuild setWelcomeChannel(TextChannel welcomeChannel) {
		this.welcomeChannel = welcomeChannel;
		return this;
	}
	
	public EnhancedGuild setWelcomeMessage(String message) {
		messages.put(BotMessages.WELCOME_MESSAGE, message);
		return this;
	}
	
	public String getWelcomeMessage() {
		return getMessage(BotMessages.WELCOME_MESSAGE);
	}
	
	public EnhancedGuild setMessage(BotMessages key, String message) {
		messages.put(key, message);
		return this;
	}
	
	public EnhancedGuild setCommandPrefix(@Nonnull String commandPrefix) {
		this.commandPrefix = commandPrefix;
		return this;
	}
	
	public String getLocaleString() {
		return locale.toString();
	}
	
	public Locale getLocale() {
		return locale;
	}
	
	public EnhancedGuild setLocale(Locale locale) {
		this.locale = locale;
		return this;
	}
	
	public String getLocaleReadable() {
		switch (locale.getLanguage()) {
			case "ru":
				return "Russian / Русский";
			case "en":
				return "English";
			case "ua":
				return "Ukrainian / Українська";
			default:
				return locale.toString();
		}
	}
	
	public EnumMap<BotMessages, String> getMessages() {
		return getBotMessages();
	}
	
	public EnumMap<BotMessages, String> getBotMessages() {
		return messages;
	}
	
	public EnhancedGuild setMessages(EnumMap<BotMessages, String> messages) {
		this.messages = messages;
		return this;
	}
	
	public EnumMap<BotFeatures, Boolean> getBotFeatures() {
		return features;
	}
	
	
	public EnhancedGuild setBotFeatures(EnumMap<BotFeatures, Boolean> features) {
		this.features = features;
		return this;
	}

	public Long getGuildId() {
		return guildId;
	}
	
	public Guild getGuild() {
		return guild;
	}
	
	public String getCommandPrefix() {
		return commandPrefix == null ? "!!" : commandPrefix;
	}
	
	public String getEscapedCommandPrefix() {
		return commandPrefix.replaceAll("([\\\\\\.\\[\\{\\(\\*\\+\\?\\^\\$\\|‌​])", "\\\\$1");
	}
	
	/**
	 * @return number of hours before user will be kicked
	 */
	public int getVerificationTimeout() {
		return verificationTimeout;
	}
	
	public EnhancedGuild setVerificationTimeout(int verificationTimeout) {
		this.verificationTimeout = verificationTimeout;
		return this;
	}
	
	public ArrayList<Role> getVerificationRoles() {
		return verificationRoles;
	}
	
	public String getVerificationRolesString() {
		StringBuilder builder = new StringBuilder();
		verificationRoles.forEach(role -> {
			if (role != null)
				builder.append(role.getName()).append(", ");
		});
		if(builder.length() == 0)
			return null;
		return builder.toString().substring(0, builder.length() - 2);
	}
	
	public EnhancedGuild setVerificationRoles(ArrayList<Role> verificationRoles) {
		this.verificationRoles = verificationRoles;
		return this;
	}
	
	public EnhancedGuild addVerificationRole(Role roleToAdd) {
		if(roleToAdd != null && ! this.verificationRoles.contains(roleToAdd))
			this.verificationRoles.add(roleToAdd);
		return this;
	}
	
	public boolean isCommandEnabled(AbstractCommand command) {
		return ! disabledCommands.contains(command);
	}
	
	public ArrayList<AbstractCommand> getDisabledCommands() {
		return disabledCommands;
	}
	
	public EnhancedGuild setDisabledCommands(ArrayList<AbstractCommand> disabledCommands) {
		this.disabledCommands = disabledCommands;
		return this;
	}
	
	
	@Override
	@SuppressWarnings("SimplifiableIfStatement")
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		
		EnhancedGuild that = (EnhancedGuild) o;
		
		if (verificationTimeout != that.verificationTimeout) return false;
		if (!guildId.equals(that.guildId)) return false;
		if (!guild.equals(that.guild)) return false;
		if (locale != null ? !locale.equals(that.locale) : that.locale != null) return false;
		if (commandPrefix != null ? !commandPrefix.equals(that.commandPrefix) : that.commandPrefix != null)
			return false;
		if (welcomeChannel != null ? !welcomeChannel.equals(that.welcomeChannel) : that.welcomeChannel != null)
			return false;
		if (verificationChannel != null ? !verificationChannel.equals(that.verificationChannel) : that.verificationChannel != null)
			return false;
		if (verificationRolesIds != null ? !verificationRolesIds.equals(that.verificationRolesIds) : that.verificationRolesIds != null)
			return false;
		if (verificationRoles != null ? !verificationRoles.equals(that.verificationRoles) : that.verificationRoles != null)
			return false;
		if (messages != null ? !messages.equals(that.messages) : that.messages != null) return false;
		return features != null ? features.equals(that.features) : that.features == null;
	}
	
	@Override
	public int hashCode() {
		int result = guildId.hashCode();
		result = 31 * result + guild.hashCode();
		result = 31 * result + (locale != null ? locale.hashCode() : 0);
		result = 31 * result + (commandPrefix != null ? commandPrefix.hashCode() : 0);
		result = 31 * result + (welcomeChannel != null ? welcomeChannel.hashCode() : 0);
		result = 31 * result + (verificationChannel != null ? verificationChannel.hashCode() : 0);
		result = 31 * result + (verificationRolesIds != null ? verificationRolesIds.hashCode() : 0);
		result = 31 * result + (verificationRoles != null ? verificationRoles.hashCode() : 0);
		result = 31 * result + verificationTimeout;
		result = 31 * result + (messages != null ? messages.hashCode() : 0);
		result = 31 * result + (features != null ? features.hashCode() : 0);
		return result;
	}
	
	@Override
	public String toString() {
		return "g" + this.getName() + " (" + this.getId() + ")";
	}
	
	@Override
	public String getName() {
		return guild.getName();
	}
	
	@Override
	public String getIconId() {
		return guild.getIconId();
	}
	
	@Override
	public String getIconUrl() {
		return guild.getIconUrl();
	}
	
	@Override
	public Set<String> getFeatures() {
		return guild.getFeatures();
	}
	
	@Override
	public String getSplashId() {
		return guild.getSplashId();
	}
	
	@Override
	public String getSplashUrl() {
		return guild.getSplashUrl();
	}
	
	@Override
	public RestAction<String> getVanityUrl() {
		return guild.getVanityUrl();
	}
	
	@Override
	public VoiceChannel getAfkChannel() {
		return guild.getAfkChannel();
	}
	
	@Override
	public TextChannel getSystemChannel() {
		return guild.getSystemChannel();
	}
	
	@Override
	public Member getOwner() {
		return guild.getOwner();
	}
	
	@Override
	public Timeout getAfkTimeout() {
		return guild.getAfkTimeout();
	}
	
	@Override
	public Region getRegion() {
		return guild.getRegion();
	}
	
	@Override
	public String getRegionRaw() {
		return guild.getRegionRaw();
	}
	
	@Override
	public boolean isMember(User user) {
		return guild.isMember(user);
	}
	
	@Override
	public Member getSelfMember() {
		return guild.getSelfMember();
	}
	
	@Override
	public Member getMember(User user) {
		return guild.getMember(user);
	}
	
	@Override
	public Member getMemberById(String userId) {
		return guild.getMemberById(userId);
	}
	
	@Override
	public Member getMemberById(long userId) {
		return guild.getMemberById(userId);
	}
	
	@Override
	public List<Member> getMembers() {
		return guild.getMembers();
	}
	
	@Override
	public List<Member> getMembersByName(String name, boolean ignoreCase) {
		return guild.getMembersByName(name, ignoreCase);
	}
	
	@Override
	public List<Member> getMembersByNickname(String nickname, boolean ignoreCase) {
		return guild.getMembersByName(nickname, ignoreCase);
	}
	
	@Override
	public List<Member> getMembersByEffectiveName(String name, boolean ignoreCase) {
		return guild.getMembersByEffectiveName(name, ignoreCase);
	}
	
	@Override
	public List<Member> getMembersWithRoles(Role... roles) {
		return guild.getMembersWithRoles(roles);
	}
	
	@Override
	public List<Member> getMembersWithRoles(Collection<Role> roles) {
		return guild.getMembersWithRoles(roles);
	}
	
	@Override
	public MemberCacheView getMemberCache() {
		return guild.getMemberCache();
	}
	
	@Override
	public Category getCategoryById(String id) {
		return guild.getCategoryById(id);
	}
	
	@Override
	public Category getCategoryById(long id) {
		return guild.getCategoryById(id);
	}
	
	@Override
	public List<Category> getCategories() {
		return guild.getCategories();
	}
	
	@Override
	public List<Category> getCategoriesByName(String name, boolean ignoreCase) {
		return guild.getCategoriesByName(name, ignoreCase);
	}
	
	@Override
	public SnowflakeCacheView<Category> getCategoryCache() {
		return guild.getCategoryCache();
	}
	
	@Override
	public TextChannel getTextChannelById(String id) {
		return guild.getTextChannelById(id);
	}
	
	@Override
	public TextChannel getTextChannelById(long id) {
		return guild.getTextChannelById(id);
	}
	
	@Override
	public List<TextChannel> getTextChannels() {
		return guild.getTextChannels();
	}
	
	@Override
	public List<TextChannel> getTextChannelsByName(String name, boolean ignoreCase) {
		return guild.getTextChannelsByName(name, ignoreCase);
	}
	
	@Override
	public SnowflakeCacheView<TextChannel> getTextChannelCache() {
		return guild.getTextChannelCache();
	}
	
	@Override
	public VoiceChannel getVoiceChannelById(String id) {
		return guild.getVoiceChannelById(id);
	}
	
	@Override
	public VoiceChannel getVoiceChannelById(long id) {
		return guild.getVoiceChannelById(id);
	}
	
	@Override
	public List<VoiceChannel> getVoiceChannels() {
		return guild.getVoiceChannels();
	}
	
	@Override
	public List<VoiceChannel> getVoiceChannelsByName(String name, boolean ignoreCase) {
		return guild.getVoiceChannelsByName(name, ignoreCase);
	}
	
	@Override
	public SnowflakeCacheView<VoiceChannel> getVoiceChannelCache() {
		return guild.getVoiceChannelCache();
	}
	
	@Override
	public Role getRoleById(String id) {
		return guild.getRoleById(id);
	}
	
	@Override
	public Role getRoleById(long id) {
		return guild.getRoleById(id);
	}
	
	@Override
	public List<Role> getRoles() {
		return guild.getRoles();
	}
	
	@Override
	public List<Role> getRolesByName(String name, boolean ignoreCase) {
		return guild.getRolesByName(name, ignoreCase);
	}
	
	@Override
	public SnowflakeCacheView<Role> getRoleCache() {
		return guild.getRoleCache();
	}
	
	@Override
	public Emote getEmoteById(String id) {
		return guild.getEmoteById(id);
	}
	
	@Override
	public Emote getEmoteById(long id) {
		return guild.getEmoteById(id);
	}
	
	@Override
	public List<Emote> getEmotes() {
		return guild.getEmotes();
	}
	
	@Override
	public List<Emote> getEmotesByName(String name, boolean ignoreCase) {
		return guild.getEmotesByName(name, ignoreCase);
	}
	
	@Override
	public SnowflakeCacheView<Emote> getEmoteCache() {
		return guild.getEmoteCache();
	}
	
	@SuppressWarnings("deprecation")
	@Override
	public RestAction<List<User>> getBans() {
		return guild.getBans();
	}
	
	@Nonnull
	@Override
	public RestAction<List<Ban>> getBanList() {
		return guild.getBanList();
	}
	
	@Override
	public RestAction<Integer> getPrunableMemberCount(int days) {
		return guild.getPrunableMemberCount(days);
	}
	
	@Override
	public Role getPublicRole() {
		return guild.getPublicRole();
	}
	
	@Nullable
	@Override
	public TextChannel getDefaultChannel() {
		return guild.getDefaultChannel();
	}
	
	@Override
	public GuildManager getManager() {
		return guild.getManager();
	}
	
	@Override
	public GuildManagerUpdatable getManagerUpdatable() {
		return guild.getManagerUpdatable();
	}
	
	@Override
	public GuildController getController() {
		return guild.getController();
	}
	
	@Override
	public MentionPaginationAction getRecentMentions() {
		return guild.getRecentMentions();
	}
	
	@Override
	public AuditLogPaginationAction getAuditLogs() {
		return guild.getAuditLogs();
	}
	
	@Override
	public RestAction<Void> leave() {
		return guild.leave();
	}
	
	@Override
	public RestAction<Void> delete() {
		return guild.delete();
	}
	
	@Override
	public RestAction<Void> delete(String mfaCode) {
		return guild.delete(mfaCode);
	}
	
	@Override
	public AudioManager getAudioManager() {
		return guild.getAudioManager();
	}
	
	@Override
	public JDA getJDA() {
		return guild.getJDA();
	}
	
	@Override
	public RestAction<List<Invite>> getInvites() {
		return guild.getInvites();
	}
	
	@Override
	public RestAction<List<Webhook>> getWebhooks() {
		return guild.getWebhooks();
	}
	
	@Override
	public List<GuildVoiceState> getVoiceStates() {
		return guild.getVoiceStates();
	}
	
	@Override
	public VerificationLevel getVerificationLevel() {
		return guild.getVerificationLevel();
	}
	
	@Override
	public NotificationLevel getDefaultNotificationLevel() {
		return guild.getDefaultNotificationLevel();
	}
	
	@Override
	public MFALevel getRequiredMFALevel() {
		return guild.getRequiredMFALevel();
	}
	
	@Override
	public ExplicitContentLevel getExplicitContentLevel() {
		return guild.getExplicitContentLevel();
	}
	
	@Override
	public boolean checkVerification() {
		return guild.checkVerification();
	}
	
	@Override
	public boolean isAvailable() {
		return guild.isAvailable();
	}
	
	@Override
	public String getId() {
		return guild.getId();
	}
	
	@Override
	public long getIdLong() {
		return guild.getIdLong();
	}
	
	@Override
	public OffsetDateTime getCreationTime() {
		return guild.getCreationTime();
	}
}
