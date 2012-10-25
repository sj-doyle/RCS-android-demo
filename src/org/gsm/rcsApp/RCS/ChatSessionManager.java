package org.gsm.rcsApp.RCS;

import java.util.HashMap;

public class ChatSessionManager {
	HashMap<String, ChatSession> sessionCache=null;
	
	public ChatSessionManager() {
		sessionCache=new HashMap<String, ChatSession>();
	}
	
	public void clearCache() {
		sessionCache.clear();
	}
	
	public ChatSession getChatSession(String destinationUri) {
		return sessionCache.get(destinationUri);
	}
	
	public void closeChatSession(String destinationUri) {
		if (sessionCache.get(destinationUri)!=null) sessionCache.remove(destinationUri);
	}
	
	public ChatSession createChatSession(String destinationUri, String sessionId, String messageId) {
		ChatSession rv=null;
		if ((rv=sessionCache.get(destinationUri))==null) {
			rv=new ChatSession();
			rv.setDestinationUri(destinationUri);
			rv.setSessionId(sessionId);
			rv.setMessageId(messageId);
		}
		return rv;
	}
	
}
