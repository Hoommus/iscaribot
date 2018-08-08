package model;

import model.entities.EnhancedGuild;
import model.entities.EnhancedMember;
import model.entities.EnhancedUser;
import net.dv8tion.jda.core.entities.Guild;

import java.util.HashMap;

public interface DatabaseController {
	void writeNewEnhancedUser(EnhancedUser enhancedUser);
	void writeNewEnhancedUser(EnhancedUser enhancedUser, boolean updateIfExists);
	void updateEnhancedUser(EnhancedUser updatedEnhancedUser);
	
	boolean doesUserEntryExist(Long userid);
	boolean doesMemberEntryExist(Long userid);
	
	void writeNewMemberEntry(EnhancedMember enhancedMember, boolean updateIfExists);
	void writeNewMemberEntry(EnhancedMember enhancedMember);
	void writeNewUserEntry(EnhancedUser enhancedUser, boolean updateIfExists);
	void writeNewUserEntry(EnhancedUser enhancedUser);
	
	EnhancedMember getEntryAsEnhancedMember(Long userid);
	EnhancedMember getEnhancedMember(Long userid);
	
	EnhancedUser getEnhancedUserByDiscordId(Long id);
	EnhancedUser getEnhancedUserByWikiaId(Long id);
	
	boolean doesWikiaUserExist(Long id);
	boolean doesEnhancedUserExist(Long id);
	
	long countVerifiedUsers();
	
	void writeNewEnhancedGuild(EnhancedGuild enhancedGuild);
	void updateEnhancedGuild(EnhancedGuild updatedEnhancedGuild);
	
	EnhancedGuild getEnhancedGuild(Guild guild);
	EnhancedGuild getEnhancedGuildById(Long guildid);
	EnhancedGuild getEnhancedGuildByWiki(String wiki);
	
	boolean doesEnhancedGuildExist(Long id);
	
	HashMap<Long, EnhancedGuild> getAllEnhancedGuilds();
}
