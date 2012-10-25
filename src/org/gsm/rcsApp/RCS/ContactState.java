package org.gsm.rcsApp.RCS;

import java.util.ArrayList;

public class ContactState {

	private String contactUri=null;
	private boolean chatIsVisible=false;
	private boolean newMessage=false;
	private String composingState=null;
	
	private ArrayList<ChatMessage> messageBuffer=new ArrayList<ChatMessage>();
	
	public ContactState(String contactUri) {
		this.contactUri=contactUri;
	}
	
	public String getContactUri() {
		return contactUri;
	}
	public boolean isChatIsVisible() {
		return chatIsVisible;
	}
	public void setContactUri(String contactUri) {
		this.contactUri = contactUri;
	}
	public void setChatIsVisible(boolean chatIsVisible) {
		this.chatIsVisible = chatIsVisible;
	}
	
	public void storeMessage(ChatMessage message) {
		messageBuffer.add(message);
	}
	
	public ArrayList<ChatMessage> getMessageBuffer() {
		return messageBuffer;
	}

	public boolean isNewMessage() {
		return newMessage;
	}

	public void setNewMessage(boolean newMessage) {
		this.newMessage = newMessage;
	}

	protected void setComposingIndicator(String composingState) {
		this.composingState=composingState;
	}
	
	public String getComposingIndicator() {
		return this.composingState; 
	}
	
}
