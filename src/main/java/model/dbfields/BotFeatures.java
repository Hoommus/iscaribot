package model.dbfields;

public enum BotFeatures {
	// Что реализовано
	WikiaVerification(true),
	UserWelcome(true),
	UserVerificationCleanup(true),
	UserVerificationNotify(true),
	UserVerificationTimeout(true),
	UserNicknameBinding(true),
	JudasKiss(true),
	
	// А что нет
	WikiaServersNetwork(false),
	Pinger(false),
	BulkMessageDelete(false),
	SpamListening(false),
	BannedWords(false),
	AutoMod(false),
	AutoMute(false),
	
	// Не в приоритете, но хочется
	GospelsQuoter(false),
	
	;
	
	private boolean isAvailable;
	
	BotFeatures(boolean isAvailable) {
		this.isAvailable = isAvailable;
	}
	
	public boolean isAvailable() {
		return isAvailable;
	}
}
