package model.entities;

import model.annotations.UserField;
import model.annotations.DiffIgnore;
import model.dbfields.UserFields;
import net.dv8tion.jda.core.JDA;
import net.dv8tion.jda.core.entities.Guild;
import net.dv8tion.jda.core.entities.PrivateChannel;
import net.dv8tion.jda.core.entities.User;
import net.dv8tion.jda.core.requests.RestAction;
import wikia.WikiaAPICall;
import wikia.WikiaUser;

import javax.annotation.CheckForNull;
import java.io.IOException;
import java.lang.reflect.Field;
import java.time.Instant;
import java.util.*;


/**
 * TODO: Consider making this immutable
 */
@SuppressWarnings({"WeakerAccess", "unused"})
public class EnhancedUser implements User {
	@DiffIgnore
	private User user;
	private WikiaUser wikiaUser;
	
	@UserField(inDB = UserFields.external_username)
	private String externalUsername;
	
	@UserField(inDB = UserFields.external_resource)
	private String externalResource;
	
	@DiffIgnore
	@UserField(inDB = UserFields._id)
	private Long discordUserId;
	
	@UserField(inDB = UserFields.last_gone_online)
	private Instant lastGoneOnline;
	
	@UserField(inDB = UserFields.last_gone_offline)
	private Instant lastGoneOffline;
	
	//            GuildID, Instant when verified
	@UserField(inDB = UserFields.verifications)
	private HashMap<Long, Instant> verifications = new HashMap<>();
	
	@UserField(inDB = UserFields.guilds_where_met_ids)
	private ArrayList<Long> guildsEverMet = new ArrayList<>();
	
	@UserField(inDB = UserFields.ignore_everything_totally)
	private boolean isTotallyIgnored = false;
	
	@UserField(inDB = UserFields.related_wikis)
	private String[] relatedWikis;
	@UserField(inDB = UserFields.native_wiki)
	private String nativeWiki;
	
	public EnhancedUser(User user) {
		this.user = user;
		if(user != null)
			this.discordUserId = user.getIdLong();
		lastGoneOnline = Instant.now().minusSeconds(1);
		lastGoneOffline = Instant.now();
	}
	
	public EnhancedUser(Long discordUserId) {
		this.discordUserId = discordUserId;
	}
	
	public static EnumMap<UserFields, Object> reflectiveDiff(EnhancedUser oldUser, EnhancedUser newUser) {
		Field[] fields = Arrays.stream(EnhancedUser.class.getDeclaredFields())
				.filter(f -> f.getAnnotation(DiffIgnore.class) == null)
				.filter(f -> f.getAnnotation(UserField.class) != null)
				.filter(f -> f.getDeclaringClass().equals(User.class))
				.toArray(Field[]::new);
		
		EnumMap<UserFields, Object> diff = new EnumMap<>(UserFields.class);
		
		for(Field field : fields) {
			try {
				if (field.getDeclaringClass().equals(WikiaUser.class)) {
					if ( ! Objects.equals(field.get(oldUser), field.get(newUser))) {
						WikiaUser wikiaUser = (WikiaUser) field.get(newUser);
						diff.put(UserFields.wikianame, wikiaUser.getName());
						diff.put(UserFields.wikiaid, wikiaUser.getUserid());
					}
				}
				try {
					diff.put(field.getAnnotation(UserField.class).inDB(), field.get(newUser));
				} catch (NullPointerException ignore) {}
			} catch (IllegalAccessException e) {
				e.printStackTrace();
			}
		}
		
		return diff;
	}
	
	public EnumMap<UserFields, Object> diff(EnhancedUser other) {
		return reflectiveDiff(this, other);
	}
	
	public String getWikiaAvatarUrl() {
		return wikiaUser != null ? wikiaUser.getAvatarUrl() : null;
	}
	
	public String getNativeWikiURL() {
		// Using getNativeWiki method to keep myself away of code repetition
		return "http://" + getNativeWiki() + ".wikia.com/wiki/User:" + wikiaUser.getName();
	}
	
	public String getGenericWikiaURL() {
		return wikiaUser.getGenericWikiaURL();
	}
	
	public boolean isVerified() {
		return wikiaUser != null && wikiaUser.getName() != null;
	}
	
	public EnhancedUser setWikiaUserByName(String name) {
		try {
			this.wikiaUser = WikiaAPICall.getWikiaUserByName(name);
		} catch (IOException e) {
			e.printStackTrace();
		}
		return this;
	}
	
	public EnhancedUser addVerification(Long guildid, Instant instant) {
		this.verifications.put(guildid, instant);
		return this;
	}
	
	public EnhancedUser removeVerifications(EnhancedGuild... enhancedGuilds) {
		Arrays.stream(enhancedGuilds).forEach(g -> this.verifications.remove(g.getIdLong()));
		return this;
	}
	
	public EnhancedUser setUser(User user) {
		this.user = user;
		return this;
	}
	
	public EnhancedUser setWikiaUser(WikiaUser wikiaUser) {
		this.wikiaUser = wikiaUser;
		return this;
	}
	
	public EnhancedUser setDiscordUserId(Long discordUserId) {
		this.discordUserId = discordUserId;
		return this;
	}
	
	public HashMap<Long, Instant> getVerifications() {
		return verifications;
	}
	
	public ArrayList<Long> getGuildsEverMet() {
		return guildsEverMet;
	}
	
	public EnhancedUser setGuildsEverMet(ArrayList<Long> guildsEverMet) {
		this.guildsEverMet = guildsEverMet;
		return this;
	}
	
	public EnhancedUser setGuildsEverMetFromGuilds(List<Guild> guildsEverMet) {
		guildsEverMet.forEach(g -> this.guildsEverMet.add(g.getIdLong()));
		return this;
	}
	
	@CheckForNull
	public WikiaUser getWikiaUser() {
		return wikiaUser;
	}
	
	@CheckForNull
	public Long getWikiaId() {
		return wikiaUser.getUserid();
	}
	
	@CheckForNull
	public String getWikiaNickname() {
		return wikiaUser == null ? null : wikiaUser.getName();
	}
	
	@CheckForNull
	public Instant getWikiaRegistrationInstant() {
		return wikiaUser.getRegistration();
	}
	
	public HashMap<Long, Date> getVerificationDates() {
		HashMap<Long, Date> vers = new HashMap<>();
		verifications.forEach((key, value) -> vers.put(key, Date.from(value)));
		return vers;
	}
	
	public HashMap<String, Date> getVerificationStringDates() {
		HashMap<String, Date> vers = new HashMap<>();
		verifications.forEach((key, value) -> vers.put(key.toString(), Date.from(value)));
		return vers;
	}
	
	
	public EnhancedUser setVerifications(HashMap<Long, Instant> verifications) {
		this.verifications = verifications;
		return this;
	}
	
	public User getUser() {
		return user;
	}
	
	public Long getDiscordUserId() {
		return discordUserId;
	}
	
	public Instant getLastGoneOnline() {
		return lastGoneOnline;
	}
	
	public Instant getLastGoneOffline() {
		return lastGoneOffline;
	}
	
	public EnhancedUser setLastGoneOffline(Instant lastGoneOffline) {
		this.lastGoneOffline = lastGoneOffline == null ? Instant.now() : lastGoneOffline;
		return this;
	}
	
	public EnhancedUser setLastGoneOnline(Instant lastGoneOnline) {
		this.lastGoneOnline = lastGoneOnline == null ? Instant.now().minusSeconds(30) : lastGoneOnline;
		return this;
	}
	
	public String getExternalUsername() {
		return externalUsername;
	}
	
	public EnhancedUser setExternalUsername(String externalUsername) {
		this.externalUsername = externalUsername;
		return this;
	}
	
	public String getExternalResource() {
		return externalResource;
	}
	
	public EnhancedUser setExternalResource(String externalResource) {
		this.externalResource = externalResource;
		return this;
	}
	
	public String[] getRelatedWikis() {
		return relatedWikis;
	}
	
	public EnhancedUser setRelatedWikis(String[] relatedWikis) {
		this.relatedWikis = relatedWikis;
		return this;
	}
	
	public String getNativeWiki() {
		return nativeWiki == null ? "community" : nativeWiki;
	}
	
	public EnhancedUser setNativeWiki(String nativeWiki) {
		this.nativeWiki = nativeWiki;
		return this;
	}
	
	public boolean isTotallyIgnored() {
		return isTotallyIgnored;
	}
	
	public void setTotallyIgnored(boolean totallyIgnored) {
		isTotallyIgnored = totallyIgnored;
	}
	
	@Override
	public String toString() {
		return "EnhancedUser{" +
				"user=" + user +
				", wikiaUser=" + wikiaUser +
				", discordUserId=" + discordUserId +
				", lastGoneOnline=" + lastGoneOnline +
				", lastGoneOffline=" + lastGoneOffline +
				", verifications=" + verifications +
				", guildsEverMet=" + guildsEverMet +
				'}';
	}
	
	@SuppressWarnings("SimplifiableIfStatement")
	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		
		EnhancedUser that = (EnhancedUser) o;
		
		if (!user.equals(that.user)) return false;
		if (!wikiaUser.equals(that.wikiaUser)) return false;
		if (!discordUserId.equals(that.discordUserId)) return false;
		return verifications.equals(that.verifications);
	}
	
	@Override
	public int hashCode() {
		int result = user.hashCode();
		result = 31 * result + wikiaUser.hashCode();
		result = 31 * result + discordUserId.hashCode();
		result = 31 * result + lastGoneOnline.hashCode();
		result = 31 * result + lastGoneOffline.hashCode();
		result = 31 * result + verifications.hashCode();
		return result;
	}
	
	@Override
	public String getName() {
		return user.getName();
	}
	
	@Override
	public String getDiscriminator() {
		return user.getDiscriminator();
	}
	
	@Override
	public String getAvatarId() {
		return user.getAvatarId();
	}
	
	@Override
	public String getAvatarUrl() {
		return user.getAvatarUrl();
	}
	
	@Override
	public String getDefaultAvatarId() {
		return user.getDefaultAvatarId();
	}
	
	@Override
	public String getDefaultAvatarUrl() {
		return user.getDefaultAvatarUrl();
	}
	
	@Override
	public String getEffectiveAvatarUrl() {
		return user.getEffectiveAvatarUrl();
	}
	
	@Override
	public boolean hasPrivateChannel() {
		return user.hasPrivateChannel();
	}
	
	@Override
	public RestAction<PrivateChannel> openPrivateChannel() {
		return user.openPrivateChannel();
	}
	
	@Override
	public List<Guild> getMutualGuilds() {
		return user.getMutualGuilds();
	}
	
	public List<Long> getMutualGuildsIds() {
		ArrayList<Long> mutualGuilds = new ArrayList<>();
		for (Guild g : user.getMutualGuilds())
			mutualGuilds.add(g.getIdLong());
		return mutualGuilds;
	}
	
	@Override
	public boolean isBot() {
		return user.isBot();
	}
	
	@Override
	public JDA getJDA() {
		return user.getJDA();
	}
	
	@Override
	public boolean isFake() {
		return user.isFake();
	}
	
	@Override
	public String getAsMention() {
		return user.getAsMention();
	}
	
	@Override
	public long getIdLong() {
		return user.getIdLong();
	}
}
