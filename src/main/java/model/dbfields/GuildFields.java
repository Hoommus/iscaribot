package model.dbfields;

public enum GuildFields {
	guildid, // Long
	
	_id, //
	
	locale,         // String
	command_prefix, // String
	welcome_channel_id,      // Long
	welcome_message_delay,   // Integer
	verification_channel_id, // Long
	verification_pending_timeout,    // Integer
	verification_role_giving_delay,  // Integer
	verification_roles_ids,          // Long[]
	verification_remind_number,      // Integer
	
	messages,  // JS Object (EnumMap)
	features,  // JS Object (EnumMap)
	
	server_wiki,    // String, may be null
	server_wiki_url, // String, may be null
	
	moderator_role_id,
	moderator_assistant_role_id,
	
	;
}
