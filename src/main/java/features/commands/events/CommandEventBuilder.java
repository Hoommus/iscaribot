package features.commands.events;

import model.entities.EnhancedGuild;
import model.entities.EnhancedMember;
import model.entities.EnhancedUser;
import net.dv8tion.jda.core.JDA;
import net.dv8tion.jda.core.entities.Member;
import net.dv8tion.jda.core.entities.Message;
import providers.EnhancedGuildsProvider;
import providers.EnhancedMembersProvider;
import providers.EnhancedUsersProvider;

import java.util.LinkedList;
import java.util.Map;

public class CommandEventBuilder {
	private EnhancedGuild enhancedGuild;
	private EnhancedUser enhancedUser;
	private Member member;
	private Message message;
	private String commandName;
	private LinkedList<String> args;
	private JDA jda;
	private Map<String, String> messageKeys;
	private long responseNumber;
	
	private EnhancedMembersProvider enhancedMembersProvider;
	private EnhancedUsersProvider enhancedUsersProvider;
	private EnhancedGuildsProvider enhancedGuildsProvider;
	
	public CommandEventBuilder() {
	
	}
	
	public CommandEventBuilder(JDA jda, long responseNumber) {
		this.jda = jda;
		this.responseNumber = responseNumber;
	}
	
	public CommandEvent build() {
		return new CommandEvent(jda, responseNumber, enhancedGuild, enhancedUser, member, message,
				commandName, messageKeys, enhancedUsersProvider, enhancedGuildsProvider, args);
	}
	
	public CommandEventBuilder setEnhancedGuild(EnhancedGuild enhancedGuild) {
		this.enhancedGuild = enhancedGuild;
		return this;
	}
	
	public CommandEventBuilder setCommandName(String commandName) {
		this.commandName = commandName;
		return this;
	}
	
	public CommandEventBuilder setArgs(LinkedList<String> args) {
		this.args = args;
		return this;
	}
	
	public CommandEventBuilder setJda(JDA jda) {
		this.jda = jda;
		return this;
	}
	
	public CommandEventBuilder setResponseNumber(long responseNumber) {
		this.responseNumber = responseNumber;
		return this;
	}
	
	public CommandEventBuilder setEnhancedUser(EnhancedUser enhancedUser) {
		this.enhancedUser = enhancedUser;
		return this;
	}
	
	public CommandEventBuilder setMessage(Message message) {
		this.message = message;
		return this;
	}
	
	public CommandEventBuilder setMember(Member member) {
		this.member = member;
		return this;
	}
	
	public CommandEventBuilder setMessageKeys(Map<String, String> messageKeys) {
		this.messageKeys = messageKeys;
		return this;
	}
	
	public CommandEventBuilder setEnhancedUsersProvider(EnhancedUsersProvider enhancedUsersProvider) {
		this.enhancedUsersProvider = enhancedUsersProvider;
		return this;
	}
	
	public CommandEventBuilder setEnhancedGuildsProvider(EnhancedGuildsProvider enhancedGuildsProvider) {
		this.enhancedGuildsProvider = enhancedGuildsProvider;
		return this;
	}
}
