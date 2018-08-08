package model;

import com.mongodb.MongoClient;
import com.mongodb.MongoClientURI;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Updates;
import com.mongodb.client.result.UpdateResult;
import model.annotations.UserField;
import model.codecs.InstantCodec;
import model.dbfields.BotFeatures;
import model.dbfields.BotMessages;
import model.dbfields.GuildFields;
import model.dbfields.UserFields;
import model.entities.EnhancedGuild;
import model.entities.EnhancedMember;
import model.entities.EnhancedUser;
import net.dv8tion.jda.core.JDA;
import net.dv8tion.jda.core.entities.Guild;
import net.dv8tion.jda.core.entities.Role;
import net.dv8tion.jda.core.entities.TextChannel;
import net.dv8tion.jda.core.entities.User;
import org.apache.commons.lang3.LocaleUtils;
import org.apache.commons.lang3.reflect.FieldUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.bson.Document;
import org.bson.codecs.configuration.CodecRegistries;
import org.bson.codecs.configuration.CodecRegistry;
import org.bson.conversions.Bson;
import wikia.WikiaAPICall;
import wikia.WikiaUser;

import java.io.IOException;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

import static com.mongodb.client.model.Filters.*;
import static model.entities.EnhancedUser.reflectiveDiff;

/**
 * Created by Festus on 02.08.2017
 * MongoController is used to perform CRuD operations on remote DB.
 *
 * I'm not sure if this one can be called "controller"
 *
 * Yeah, it's a <i>Singleton</i>. Anti-pattern.
 *
 * TODO: Rewrite CrUD for using Reflection to remove need of MongoController modification in future
 * TODO: Add HashMap and EnumMap codec
 * TODO: Consider projections usage
 * TODO: Replace all this with codecs
 */
public class MongoController implements DatabaseController {
	private static MongoCollection<Document> usersCollection;
	private static MongoCollection<Document> membersCollection;
	private static MongoCollection<Document> guildCollection;
	
	private static final Logger LOGGER = LogManager.getLogger(MongoController.class);
	
	private static MongoController instance = new MongoController();
	
	private static JDA jda;
	
	private static MongoDatabase database;
	
	public static void init(String user, String password, boolean localhost) {
		String mongoUri;
		if (localhost)
			mongoUri = "mongodb://127.0.0.1:27017";
		else
			throw new RuntimeException("You should provide remote MongoDB URL.");
			// mongoUri = "mongodb://" + user + ":" + password + "@" + "...";
		MongoClient client = new MongoClient(new MongoClientURI(mongoUri));
		
		CodecRegistry customCodecs = CodecRegistries.fromCodecs(new InstantCodec());
		//database.withCodecRegistry(CodecRegistries.fromRegistries(database.getCodecRegistry(), customCodecs));
		
		database = client.getDatabase("database");
		usersCollection = database.getCollection("users");
		membersCollection = database.getCollection("members");
		guildCollection = database.getCollection("guild_config");
	}
	
	public static void initAsync(String user, String password) {
		CompletableFuture.runAsync(() -> init(user, password));
	}
	
	public static void init(String user, String password) {
		init(user, password, false);
	}
	
	public static void setJda(JDA api) {
		jda = api;
	}
	
	public static MongoController getInstance() {
		return new MongoController();
	}
	
	private MongoController() {
	
	}
	
	/**
	 * Creates new user in Mongo Database. If user exists and updateIfExists == false,
	 * then it **should** replace existing entry with updated user.
	 *
	 * @param enhancedUser   - basic JDA User with some additional fields used for DB
	 * @param updateIfExists - if user with such {@code discordid} exists, then calls
	 *                         {@link #updateEnhancedUser(EnhancedUser)}
	 */
	@Override
	public void writeNewUserEntry(EnhancedUser enhancedUser, boolean updateIfExists) {
		final Document first = usersCollection.find(eq(UserFields.discordid.name(), enhancedUser.getIdLong())).first();
		if (first != null && updateIfExists) {
			updateEnhancedUser(enhancedUser);
		} else if (first == null) {
			final Document document = new Document();
			FieldUtils.getAllFieldsList(enhancedUser.getClass())
					.stream()
					.filter (f -> f.getAnnotation(UserField.class) != null)
					.filter (f -> f.getAnnotation(UserField.class).inDB() != UserFields.NULL)
					.forEach(field -> {
						field.setAccessible(true);
						try {
							UserField annotation = field.getAnnotation(UserField.class);
							document.append(annotation.inDB().name(), field.get(enhancedUser));
						} catch (IllegalAccessException e) {
							e.printStackTrace();
						}
					});
			if(enhancedUser.getWikiaUser() != null) {
				document.append(UserFields.wikiaid.name(),   enhancedUser.getWikiaId())
						.append(UserFields.wikianame.name(), enhancedUser.getWikiaNickname());
			}
			usersCollection.insertOne(document);
		}
	}
	
	@Override
	public void writeNewUserEntry(EnhancedUser enhancedUser) {
		writeNewUserEntry(enhancedUser, false);
	}
	
	/**
	 * Creates new user in Mongo Database. If user exists and updateIfExists == false,
	 * then it **should** replace existing entry with updated user.
	 *
	 * But this one just calls {@link #writeNewEnhancedUser(EnhancedUser, boolean)} with updateIfExists = false.
	 * @param enhancedUser   - basic JDA User with some additional fields used for DB
	 */
	@Override
	public void writeNewEnhancedUser(EnhancedUser enhancedUser) {
		writeNewEnhancedUser(enhancedUser, false);
	}

	
	/**
	 * Method gets differences between two users with equal {@code ids} and updates it in DB with new values.
	 *
	 * @param updatedEnhancedUser - yet another EnhancedUser.
	 */
	@Override
	public void updateEnhancedUser(EnhancedUser updatedEnhancedUser) {
		LOGGER.debug("Updating enhanced user.");
		long start = System.currentTimeMillis();
		EnhancedUser existingUser = getEnhancedUserByDiscordId(updatedEnhancedUser.getIdLong(), false);
		LOGGER.debug("Existing user fetch time taken " + (System.currentTimeMillis() - start) + "ms");
		
		Document update = new Document();
		reflectiveDiff(existingUser, updatedEnhancedUser).forEach((k,v) -> update.append(k.name(), v));
		
		if(update.size() > 0) {
			usersCollection.updateOne(eq(UserFields.discordid.name(),
					existingUser.getIdLong()),
					new Document("$set", update));
			update.values().stream()
					.filter(v -> v.getClass().equals(ArrayList.class))
					.forEach(v -> {
						usersCollection.updateOne(eq(UserFields.discordid.name(), existingUser.getIdLong()),
								)
					});
		}
	}
	
	@Override
	public boolean doesUserEntryExist(Long userid) {
		return usersCollection.find(eq(UserFields.discordid.name(), userid)) != null;
	}
	
	@Override
	public boolean doesMemberEntryExist(Long userid) {
		return membersCollection.find(eq(UserFields.discordid.name(), userid)) != null;
	}
	
	@Override
	public void writeNewMemberEntry(EnhancedMember enhancedMember, boolean updateIfExists) {
	
	}
	
	@Override
	public void writeNewMemberEntry(EnhancedMember enhancedMember) {
	
	}
	
	@Override
	public EnhancedMember getEntryAsEnhancedMember(Long userid) {
		return null;
	}
	
	@Override
	public EnhancedMember getEnhancedMember(Long userid) {
		return null;
	}
	
	@Override
	public EnhancedUser getEnhancedUserByDiscordId(Long id) {
		return getEnhancedUserByDiscordId(id, true);
	}
	
	@Override
	public void writeNewEnhancedUser(EnhancedUser enhancedUser, boolean updateIfExists) {
	
	}
	
	/**
	 * @param id - Long id of Discord user
	 * @return EnhancedUser
	 */
	// @CheckForNull
	public EnhancedUser getEnhancedUserByDiscordId(Long id, boolean withWikiaUser) {
		LOGGER.debug("Enhancing user by discordid");
		long start = System.currentTimeMillis();
		Document user = usersCollection.find(eq(UserFields.discordid.name(), id)).first();
		
		return parseUserDocument(user, withWikiaUser);
	}
	
	/**
	 * @param id - String id of Wikia user
	 * @return EnhancedUser processed with {@code EnhancedUser.of(Document)}
	 */
	@Override
	public EnhancedUser getEnhancedUserByWikiaId(Long id) {
		LOGGER.info("Returning user from wikiaId");
		Document user = usersCollection.find(eq(UserFields.wikiaid.name(), id)).first();
		
		if (user == null)
			return null;
		return parseUserDocument(user, true);
	}
	
	private EnhancedUser parseUserDocument(Document user, boolean withWikiaUser) {
		Long discordId = user.getLong(UserFields.discordid.name());
		Long wikiaId   = user.getLong(UserFields.wikiaid.name());
		String wikiaName = user.getString(UserFields.wikianame.name());
		String externalUsername = user.getString(UserFields.external_username.name());
		Instant lastGoneOnline  = user.getDate(UserFields.last_gone_online.name()).toInstant();
		Instant lastGoneOffline = user.getDate(UserFields.last_gone_offline.name()).toInstant();
		ArrayList<Long> guildsEverMet = user.get(UserFields.guilds_where_met_ids.name(), new ArrayList<Long>());
		
		HashMap<Long, Instant> verifications = new HashMap<>();
		
		// TODO: rethink this one too
		for(Map.Entry<String, Object> e : user.get(UserFields.verifications.name(), Document.class).entrySet()) {
			verifications.put(Long.parseLong(e.getKey()), ((Date) e.getValue()).toInstant());
		}
		
		WikiaUser wikiaUser =
				new WikiaUser(wikiaId == null ? 0 : wikiaId, wikiaName, -1, null, null);
		
		try {
			if(wikiaName != null && withWikiaUser) {
				LOGGER.debug("Calling WikiaAPICall in getEnhancedUserByDiscordId");
				wikiaUser = WikiaAPICall.getWikiaUserByName(wikiaName);
			}
		} catch (IOException e1) {
			LOGGER.error("IOException " + e1.getMessage());
			e1.printStackTrace();
		}
		
		User bareUser = jda.getUserById(discordId);
		
		return new EnhancedUser(bareUser)
				.setDiscordUserId(discordId)
				.setWikiaUser(wikiaUser)
				.setLastGoneOnline(lastGoneOnline)
				.setLastGoneOffline(lastGoneOffline)
				.setVerifications(verifications)
				.setExternalUsername(externalUsername)
				.setGuildsEverMet(guildsEverMet);
	}
	
	
	public List<EnhancedUser> getAllVerifiedUsersForGuild(Long guildid) {
		Bson filter = and(exists(UserFields.wikianame.name()),
				exists(UserFields.verifications.name()),
				elemMatch(UserFields.verifications.name(), eq(guildid)));
		usersCollection.find(filter).limit(1000);
		return null;
	}
	
	@Override
	public long countVerifiedUsers() {
		Bson filter = and(exists(UserFields.wikianame.name()));
		return usersCollection.count(filter);
	}
	
	/**
	 * @param id wiki user id
	 * @return true if user is presented in DB
	 */
	@Override
	public boolean doesWikiaUserExist(Long id) {
		return usersCollection.find(eq(UserFields.wikiaid.name(), id)).first() != null;
	}
	
	
	/**
	 * Requests a document with given discord id and checks if it null.
	 *
	 * @param id user id
	 * @return true if document != null
	 */
	@Override
	public boolean doesEnhancedUserExist(Long id) {
		return usersCollection.find(eq(UserFields.discordid.name(), id)).first() != null;
	}
	
	/**
	 * @param enhancedGuild guild to write into DB
	 */
	@Override
	public void writeNewEnhancedGuild(EnhancedGuild enhancedGuild) {
		Long guildid = enhancedGuild.getGuildId();
		String locale = enhancedGuild.getLocaleString();
		String commandPrefix = enhancedGuild.getEscapedCommandPrefix();
		Long welcomeChannelId = enhancedGuild.getWelcomeChannel().getIdLong();
		Long verificationChannelId = enhancedGuild.getVerificationChannel() == null ? welcomeChannelId : enhancedGuild.getVerificationChannel().getIdLong();
		
		ArrayList<Long> verificationRolesIds = enhancedGuild.getVerificationRolesIds();
		Integer verificationTimeout = enhancedGuild.getVerificationTimeout();
		
		EnumMap<BotMessages, String> messages = enhancedGuild.getBotMessages();
		HashMap<String, Boolean> features = new HashMap<>();
		enhancedGuild.getBotFeatures().forEach((feature, bool) -> features.put(feature.name(), bool));
		
		Document first = usersCollection.find(eq(GuildFields.guildid.name(), enhancedGuild.getGuildId())).first();
		if(first == null) {
			guildCollection.insertOne(new Document(GuildFields.guildid.name(), guildid)
					.append(GuildFields._id.name(), guildid)
					.append(GuildFields.locale.name(), locale)
					.append(GuildFields.command_prefix.name(), commandPrefix)
					.append(GuildFields.welcome_channel_id.name(), welcomeChannelId)
					.append(GuildFields.verification_pending_timeout.name(), verificationTimeout)
					.append(GuildFields.verification_roles_ids.name(), verificationRolesIds)
					.append(GuildFields.verification_channel_id.name(), verificationChannelId)
					.append(GuildFields.messages.name(), messages)
					.append(GuildFields.features.name(), features)
			);
		}
		
	}
	
	/**
	 * @param guildid guild id
	 * @return EnhancedGuild instance with fields equal to DB (...what?)
	 */
	@Override
	public EnhancedGuild getEnhancedGuildById(Long guildid) {
		Document document = guildCollection.find(eq(GuildFields.guildid.name(), guildid)).first();
		return parseEnhancedGuild(document, guildid);
	}
	
	@Override
	public EnhancedGuild getEnhancedGuildByWiki(String wiki) {
		return null;
	}
	
	@Override
	public boolean doesEnhancedGuildExist(Long id) {
		return guildCollection.find( eq(GuildFields.guildid.name(), id)).first() != null;
	}
	
	@Override
	public EnhancedGuild getEnhancedGuild(Guild guild) {
		return getEnhancedGuildById(guild.getIdLong());
	}
	
	/**
	 * This method is separated from {@link providers.EnhancedGuildsProvider} because it is used to initialize
	 * {@code EnhancedGuildsProvider} on application startup
	 *
	 * @param document - {@link Document}
	 * @param guildid  - guild id as long
	 * @return new EnhancedGuild
	 */
	private EnhancedGuild parseEnhancedGuild(Document document, Long guildid) {
		TextChannel welcomeChannel      = jda.getTextChannelById(document.getLong(GuildFields.welcome_channel_id.name()));
		TextChannel verificationChannel = jda.getTextChannelById(document.getLong(GuildFields.verification_channel_id.name()));
		
		String locale = document.getString(GuildFields.locale.name());
		String commandPrefix = document.getString(GuildFields.command_prefix.name());
		
		int timeout = document.getInteger(GuildFields.verification_pending_timeout.name());
		
		EnumMap<BotMessages, String> messages = new EnumMap<>(BotMessages.class);
		document.get(GuildFields.messages.name(), Document.class)
				.forEach((key, value) -> {
					for(BotMessages s : BotMessages.values())
						if(key.equalsIgnoreCase(s.name())) {
							messages.put(s, (String) value);
							break;
						}
				});
		
		EnumMap<BotFeatures, Boolean> features = new EnumMap<>(BotFeatures.class);
		
		document.get(GuildFields.features.name(), Document.class)
				.forEach((key, value) -> {
					for(BotFeatures f : BotFeatures.values())
						if(key.equalsIgnoreCase(f.name())) {
							features.put(f, (Boolean) value);
							break;
						}
				});
		ArrayList<Long> verificationRolesIds =
				new ArrayList<>(document.get(GuildFields.verification_roles_ids.name(), new ArrayList<Long>()));
		
		ArrayList<Role> verificationRoles = new ArrayList<>();
		verificationRolesIds.forEach(id -> verificationRoles.add(jda.getRoleById(id)));
		
		return new EnhancedGuild(jda.getGuildById(guildid))
				.setVerificationChannel(verificationChannel)
				.setWelcomeChannel(welcomeChannel)
				.setLocale(LocaleUtils.toLocale(locale))
				.setCommandPrefix(commandPrefix)
				.setMessages(messages)
				.setVerificationTimeout(timeout)
				.setBotFeatures(features)
				.setVerificationRoles(verificationRoles)
				.setVerificationRolesIds(verificationRolesIds);
	}
	
	@Override
	public void updateEnhancedGuild(EnhancedGuild updatedEnhancedGuild) {
		EnhancedGuild oldEnhancedGuild = getEnhancedGuildById(updatedEnhancedGuild.getGuildId());
		
		//Document update = new Document();
		ArrayList<Bson> updates = new ArrayList<>();
		oldEnhancedGuild.diff(updatedEnhancedGuild).forEach((key, value) -> updates.add(Updates.set(key.name(), value)));
		
		Bson combined = Updates.combine(updates);
		
		if(updates.size() > 0) {
			UpdateResult result = guildCollection.updateOne(eq(GuildFields.guildid.name(), oldEnhancedGuild.getIdLong()), combined);
			LOGGER.info("Guild Update: ", result.toString());
		}
	}
	
	public HashMap<Long, EnhancedGuild> getAllEnhancedGuilds() {
		HashMap<Long, EnhancedGuild> guilds = new HashMap<>();
		guildCollection.find().forEach((Consumer<Document>) document -> {
			long id = document.getLong(GuildFields.guildid.name());
			if (jda.getGuildById(id) == null)
				return;
			guilds.put(id, parseEnhancedGuild(document, id));
		});
		return guilds;
	}
}
