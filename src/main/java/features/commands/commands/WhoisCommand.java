package features.commands.commands;

import features.commands.events.CommandEvent;
import model.DatabaseController;
import model.dbfields.BotMessages;
import model.entities.EnhancedUser;
import wikia.WikiaAPICall;
import wikia.WikiaUser;

import java.io.IOException;

public class WhoisCommand extends AbstractCommand {
	private DatabaseController databaseController;
	
	public WhoisCommand(DatabaseController db) {
		this.name = "whois";
		this.description = "Helps to find Discord user via Wikia";
		this.databaseController = db;
	}
	
	@Override
	public void execute(CommandEvent event) {
		String username = String.join(" ", event.getArgs());
		try {
			WikiaUser wikiaUser = WikiaAPICall.getWikiaUserByName(username);
			if (wikiaUser != null) {
				EnhancedUser user = databaseController.getEnhancedUserByWikiaId(wikiaUser.getUserid());
				
				if (user == null)
					event.sendMessage("This Wikia user is not binded to any Discord user.");
				else if (event.getEnhancedGuild().getMember(user) != null)
					event.sendMessage(user.getName() + "#" + user.getDiscriminator() + ", никнейм на ФЭНДОМе: **" + user.getWikiaNickname() + "**");
				else
					event.sendMessage("User is not present in this guild.");
			}
			else
				event.sendMessage(event.getEnhancedGuild().getMessage(BotMessages.USER_NOT_FOUND));
		} catch (IOException e) {
			event.sendMessage(event.getEnhancedGuild().getMessage(BotMessages.USER_NOT_FOUND));
		}
		
		EnhancedUser enhancedUser;
	}
}
