package model.dbfields;

@SuppressWarnings({"SpellCheckingInspection", "unused"})
public enum BotMessagesTemplates {
	usermention("${usermention}"),
	username("${username}"),
	userid("${userid}"),
	
	channelname("${channelname}"),
	channelid("${channelid}"),
	guildname("${guildname}"),
	
	n("${n}"), // number
	s("${s}"), // String
	;
	
	String template;
	BotMessagesTemplates(String s) {
		template = s;
	}
	
	@Override
	public String toString() {
		return template;
	}
}
