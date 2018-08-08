package config;

import net.dv8tion.jda.core.AccountType;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.List;
import java.util.Properties;

public class BotConfig {
	private static String authorID;
	
	private static String version;
	private static String prefix;
	private static String wikiaOrigin;
	private static String wikiaAccount;
	
	private static String databaseUser;
	private static String databasePass;
	
	private static String JDAtoken;
	
	private static AccountType accountType;
	
	private static boolean isTest;
	
	public static void load(String filename, List<String> args) {
		try {
			final Properties config = new Properties();
			config.load(new FileInputStream(filename));
			
			if(args.contains("--test")) {
				JDAtoken = config.getProperty("test_account");
				accountType = AccountType.BOT;
				isTest = true;
			} else if (args.contains("--user") && !args.contains("--test")) {
				JDAtoken = config.getProperty("user_account");
				accountType = AccountType.CLIENT;
			} else {
				JDAtoken = config.getProperty("main_account");
				accountType = AccountType.BOT;
			}
			
			version  = config.getProperty("version");
			prefix   = config.getProperty("default_prefix");
			wikiaOrigin  = config.getProperty("wikia_origin");
			wikiaAccount = config.getProperty("wikia_account");
			
			databaseUser = config.getProperty("db_user");
			databasePass = config.getProperty("db_password");
			
			authorID = config.getProperty("author_id");
		} catch (IOException e) {
			System.err.println("Config file does not exist");
			e.printStackTrace();
			System.exit(1);
		}
		
	}
	
	public static String getVersion() {
		return version;
	}
	
	public static String getPrefix() {
		return prefix;
	}
	
	public static String getWikiaOrigin() {
		return wikiaOrigin;
	}
	
	public static String getWikiaAccount() {
		return wikiaAccount;
	}
	
	public static String getDatabaseUser() {
		return databaseUser;
	}
	
	public static String getDatabasePass() {
		return databasePass;
	}
	
	public static String getJDAtoken() {
		return JDAtoken;
	}
	
	public static AccountType getAccountType() {
		return accountType;
	}
	
	public static String getAuthorID() {
		return authorID;
	}
	
	public static boolean isTest() {
		return isTest;
	}
}
