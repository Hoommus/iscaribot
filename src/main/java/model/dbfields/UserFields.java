package model.dbfields;

public enum UserFields {
	discordid, // Long
	
	isverified, // TODO: Look into it
	
	wikianame,     // String, can be null
	wikiaid,       // String, can be null
	external_username,       // String, can be null
	external_resource,       // String, can be null
	verifications, // Map<String, Date>, can be null
	
	_id, // Long
	
	last_gone_online,  // Date (Instant)
	last_gone_offline, // Date (Instant)
	last_message_date, // Date (Instant)
	
	first_joined,      // Map<String, Date (Instant)>
	
	last_session_length,    // Float
	average_session_length, // Float
	median_session_length,  // Float
	sessions_recorded,      // Integer
	known_nicknames,        // String[]
	
	guilds_where_met_ids,      // String[] or Long[]
	ignore_commands_guilds,    // List<Long>
	ignore_everything_guilds,  // List<Long>
	ignore_everything_totally, // Boolean
	
	native_wiki,   // String
	related_wikis, // String[] {ru.terraria, ru.community, etc.}
	sysop_at,      // Map<String, String> (Map<GuildID, WikiURL>)
	
	preferred_prefix, //String
	NULL,
	;
	
	@Override
	public String toString() {
		return this.name();
	}
}
