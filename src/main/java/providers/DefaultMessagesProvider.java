package providers;

import model.dbfields.BotMessages;

import java.util.*;

public class DefaultMessagesProvider {
	private static final EnumMap<BotMessages, String> DEFAULT_MESSAGES_EN = new EnumMap<>(BotMessages.class);
	private static final EnumMap<BotMessages, String> DEFAULT_MESSAGES_RU = new EnumMap<>(BotMessages.class);
	private static final EnumMap<BotMessages, String> DEFAULT_MESSAGES_UA = new EnumMap<>(BotMessages.class);
	
	private static final DefaultMessagesProvider instance;
	
	private DefaultMessagesProvider() {}
	
	static {
		instance = new DefaultMessagesProvider();
		init();
	}
	
	public static void init() {
		ResourceBundle russian   = ResourceBundle.getBundle("messages/default_messages", new Locale("ru", "RU"));
		ResourceBundle english   = ResourceBundle.getBundle("messages/default_messages", Locale.ENGLISH);
		ResourceBundle ukrainian = ResourceBundle.getBundle("messages/default_messages", new Locale("ua"));
		
		for (BotMessages bm : BotMessages.values()) {
			try {
				DEFAULT_MESSAGES_RU.put(bm, russian.getString(bm.name()));
				DEFAULT_MESSAGES_EN.put(bm, english.getString(bm.name()));
				DEFAULT_MESSAGES_UA.put(bm, ukrainian.getString(bm.name()));
			} catch (MissingResourceException e) {
				System.out.println(e.getMessage());
			}
		}
	}
	
	public static DefaultMessagesProvider getInstance() {
		return instance;
	}
	
	public String getMessage(BotMessages key, Locale locale) {
		switch (locale.getLanguage()) {
			case "ru":
				return DEFAULT_MESSAGES_RU.get(key);
			case "uk":
				return DEFAULT_MESSAGES_UA.get(key);
			default:
				return DEFAULT_MESSAGES_EN.get(key);
		}
	}
	
	
	
	public String getMessage(BotMessages key, String locale) {
		if(locale.equalsIgnoreCase("ru") || locale.equalsIgnoreCase("ru_RU"))
			return DEFAULT_MESSAGES_RU.get(key);
		else if(locale.equalsIgnoreCase("ua") || locale.equalsIgnoreCase("uk"))
			return DEFAULT_MESSAGES_UA.get(key);
		else
			return DEFAULT_MESSAGES_EN.get(key);
	}
	
	public String getMessage(BotMessages key, String locale, HashMap<String, String> keys) {
		if(locale.equalsIgnoreCase("ru") || locale.equalsIgnoreCase("ru_RU"))
			return DEFAULT_MESSAGES_RU.get(key);
		else if(locale.equalsIgnoreCase("ua") || locale.equalsIgnoreCase("uk"))
			return DEFAULT_MESSAGES_UA.get(key);
		else
			return DEFAULT_MESSAGES_EN.get(key);
	}
}
