package org.gsm.rcsApp.RCS;

import java.util.ArrayList;
import java.util.HashMap;

public class ContactStateManager {
	HashMap<String, ContactState> contactStateCache=null;
//	HashMap<String, ChatMessage> consolidatedMessageCache=null;
	private static HashMap<String, ChatMessage> consolidatedMessageSentCache=new HashMap<String, ChatMessage>();
	private static HashMap<String, ChatMessage> messageIdResolutionCache=new HashMap<String, ChatMessage>();
	private static HashMap<String, Boolean> haveSentMessageTo=new HashMap<String, Boolean>();
	
	public ContactStateManager() {
		contactStateCache=new HashMap<String, ContactState>();
	}

	public void clearCache() {
		if (contactStateCache!=null) contactStateCache.clear();	
	}
	
	public static void reset() {
		consolidatedMessageSentCache.clear();
		messageIdResolutionCache.clear();
		haveSentMessageTo.clear();
	}
	
	public ContactState getOrCreateContactState(String contactUri) {
		ContactState entry=null;
		if (!contactStateCache.containsKey(contactUri)) {
			entry=new ContactState(contactUri);
			contactStateCache.put(contactUri, entry);
		} else {
			entry=contactStateCache.get(contactUri);
		}
		return entry;
	}
	
	public void setChatVisible(String contactUri, boolean visibility) {
		ContactState entry=contactStateCache.get(contactUri);
		if (entry!=null) {
			entry.setChatIsVisible(visibility);
		}
	}

	public ArrayList<ChatMessage> getMessageBuffer(String contactUri) {
		getOrCreateContactState(contactUri);
		ContactState entry=contactStateCache.get(contactUri);
		return entry.getMessageBuffer();
	}

//	public HashMap<String,ChatMessage> getSentMessages(String contactUri) {
//		getOrCreateContactState(contactUri);
//		ContactState entry=contactStateCache.get(contactUri);
//		return entry.getSentMessages();
//	}
//
//	public HashMap<String,ChatMessage> getMessageMap(String contactUri) {
//		getOrCreateContactState(contactUri);
//		ContactState entry=contactStateCache.get(contactUri);
//		return entry.getMessageMap();
//	}
	
	public void storeMessage(String contactUri, ChatMessage message, String toUser) {
		getOrCreateContactState(contactUri);
		ContactState entry=contactStateCache.get(contactUri);
		entry.storeMessage(message);
	}

	public void storeMessage(String contactUri, ChatMessage message, boolean newMessage, String toUser) {
		getOrCreateContactState(contactUri);
		ContactState entry=contactStateCache.get(contactUri);
		entry.storeMessage(message);
		entry.setNewMessage(newMessage);
	}

	public static ChatMessage getMessageForId(String messageId) {
		return messageId!=null?consolidatedMessageSentCache.get(messageId):null;
	}
	
	public static void updateStatusFor(String messageId, String string) {
		ChatMessage message=messageId!=null?consolidatedMessageSentCache.get(messageId):null;
		if (message!=null) {
			message.setStatus(string);
		}
	}
	
	public static void registerOutgoingMessage(ChatMessage message) {
		System.out.println("Registering "+message.getMessageInternalId()+" = "+message);
		messageIdResolutionCache.put(message.getMessageInternalId(), message);
		haveSentMessageTo.put(message.getContactUri(), Boolean.TRUE);
	}
	
	public static boolean haveSentMessageTo(String contactUri) {
		boolean haveSent=false;
		if (haveSentMessageTo.containsKey(contactUri)) haveSent=haveSentMessageTo.get(contactUri).booleanValue();
		return haveSent;
	}

	public static void setMessageIdForSentMessage(String messageInternalId, String messageId, String resourceURL) {
		System.out.println("Setting messageId to "+messageId+" for "+messageInternalId);
		if (messageInternalId!=null) {
			ChatMessage message=messageIdResolutionCache.get(messageInternalId);
			System.out.println("Retrieved "+messageInternalId+" = "+message);
			if (message!=null) {
				message.setMessageId(messageId);
				message.setResourceURL(resourceURL);
				messageIdResolutionCache.remove(messageId);
				consolidatedMessageSentCache.put(messageId, message);
			}
		}
	}

	public void updateComposingIndicator(String contactUri, String state) {
		getOrCreateContactState(contactUri);
		ContactState entry=contactStateCache.get(contactUri);
		if (entry!=null) entry.setComposingIndicator(state);
	}

}
