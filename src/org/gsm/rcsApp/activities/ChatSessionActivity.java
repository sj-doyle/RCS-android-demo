package org.gsm.rcsApp.activities;

import java.io.UnsupportedEncodingException;

import org.apache.http.auth.AuthScope;
import org.apache.http.auth.Credentials;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.entity.StringEntity;
import org.gsm.rcsApp.ServiceURL;
import org.gsm.rcsApp.RCS.ChatMessage;
import org.gsm.rcsApp.RCS.Contact;
import org.gsm.rcsApp.RCS.ContactState;
import org.gsm.rcsApp.RCS.ContactStateManager;
import org.gsm.rcsApp.adapters.MessageRowAdapter;
import org.gsm.rcsApp.misc.RCSJsonHttpResponseHandler;
import org.gsm.rcsApp.misc.Utils;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.Editable;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;

import com.loopj.android.http.AsyncHttpClient;
import org.gsm.RCSDemo.R;

public class ChatSessionActivity extends Activity implements Runnable {
	private static String destinationUri=null;
	private static String displayName=null;
	
	static boolean running=false;
	static Thread background=null;
	
	private static ContactState contactState=null;
	
	static MessageRowAdapter mradapter=null;
	
	static ListView messageListView=null;
	static TextView isComposingIndicator=null;
	
	private static Handler messageHandler = null;
	private static Handler composingIndicatorHandler = null;
	static boolean sentComposing=false;
	
	static ChatSessionActivity _instance=null;

	private static boolean viewIsVisible=false;
	
	private static final String RECEIVED_MESSAGE="receivedMessage";

	private static final int COMPOSINGUPDATEFREQUENCYSECONDS=60;

	@Override
    public void onCreate(Bundle savedInstanceState) {		
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_chatsession);
        
        _instance=this;

        Intent intent = getIntent();
        Contact retrievedContact=intent.getParcelableExtra(MainActivity.SELECTED_CONTACT);

        displayName=retrievedContact.getDisplayName();
        if (displayName==null) displayName=retrievedContact.getContactId();
        this.setTitle(displayName);
        destinationUri=retrievedContact.getContactId();
        
		messageListView=(ListView) findViewById(R.id.messageList);
		isComposingIndicator=(TextView) findViewById(R.id.isComposingIndicator);

		mradapter=new MessageRowAdapter(this, contactState.getMessageBuffer());
        messageListView.setAdapter(mradapter);
        messageHandler = new Handler() {
    		public void handleMessage(Message msg) {
    			super.handleMessage(msg);
    			if (msg.getData()!=null) {
    				Bundle data=msg.getData();
    				if (data.containsKey(RECEIVED_MESSAGE)) {
    					ChatMessage receivedMessage=(ChatMessage) data.get(RECEIVED_MESSAGE);
    					if (receivedMessage!=null) {
    						contactState.getMessageBuffer().add(receivedMessage);
    					}
    				}
    			}
				messageListView.smoothScrollToPosition(contactState.getMessageBuffer()!=null?(contactState.getMessageBuffer().size() - 1):0);
    			mradapter.notifyDataSetChanged();
    			messageListView.refreshDrawableState();
    		}
    	};
    	
    	composingIndicatorHandler = new Handler() {
    		public void handleMessage(Message msg) {
    			if (msg.what==1) {
					isComposingIndicator.setVisibility(View.VISIBLE);
					isComposingIndicator.setText(displayName+" is writing");
				} else {
					isComposingIndicator.setVisibility(View.INVISIBLE);
				}
    		}
    	};

		if (!running) {
	        running=true;
	        background=new Thread(this);
	        background.start();			
		}

	}
	
	public void onStart() {
		super.onStart();
		
		if (!running) {
	        running=true;
	        background=new Thread(this);
	        background.start();			
		}
		viewIsVisible=true;
		isComposingIndicator.setVisibility(View.INVISIBLE);
	}
	
	public void onStop() {
		super.onStop();
		running=false;
		if (background!=null) {
			background.interrupt();
			background=null;
		}
		viewIsVisible=false;
		if (sentComposing) {
			clearComposingIndicator();
		}
		MainActivity.chatSessionClosed(destinationUri);
	}
	
	/*
	 * invoked when the user presses the button to send a message
	 */
	public void sendMessageClicked(View view) {
		EditText messageInputBox=(EditText) findViewById(R.id.message_input_box);
		final Editable text=messageInputBox.getText();
		Thread t = new Thread(){
		    public void run(){
				String trimmed=text.toString().trim();
				
				if (trimmed.length()>0) {
					ChatMessage sent=new ChatMessage();
					sent.setMessageText(trimmed);
					sent.setMessageDirection(ChatMessage.MESSAGE_SENT);
					sent.setMessageTime(Utils.getNowAsDisplayString());	
					sent.setViewed(true);
					sent.setStatus(ChatMessage.MESSAGE_STATUS_PENDING);
					sent.setContactUri(destinationUri);
					contactState.storeMessage(sent);
					
					sendAdhocMessage(trimmed, sent);
					
					messageHandler.sendEmptyMessage(0);
				}
		    }
		};
		t.start();
		messageInputBox.setText("");
	}
	
	/*
	 * invoked when the list of messages needs to be restored from saved state
	 */
	public static boolean refreshMessageList(String contactUri, String recipient, ChatMessage chatMessage) {
		boolean viewed=false;
		if (recipient!=null && recipient.equals(SplashActivity.userId)) {
			Message newChatMessage=new Message();
			Bundle msgBundle=new Bundle();
			msgBundle.putParcelable(RECEIVED_MESSAGE, chatMessage);
			newChatMessage.setData(msgBundle);
			messageHandler.sendMessage(newChatMessage);
			viewed=viewIsVisible;
		}
		return viewed;
	}
	
	/*
	 * sends a message in ad-hoc mode
	 */
	private void sendAdhocMessage(String message, ChatMessage chatMessage) {
		try {
			final String url=ServiceURL.sendAdhocIMMessageURL(SplashActivity.userId, destinationUri);
			
			final String messageInternalId=chatMessage.getMessageInternalId();
			
	        ContactStateManager.registerOutgoingMessage(chatMessage);

			JSONObject chatMessageJSON=new JSONObject();
			
			chatMessageJSON.put("reportRequest", "Sent"); // possible status values are "Sent,Delivered,Displayed,Failed"
			chatMessageJSON.put("text", message);
			
			String jsonData="{\"chatMessage\":"+chatMessageJSON.toString()+"}";
			
	        AsyncHttpClient client = new AsyncHttpClient();
	        AuthScope authscope=new AuthScope(ServiceURL.serverName, ServiceURL.serverPort, AuthScope.ANY_REALM);
	        client.setBasicAuth(SplashActivity.userId, SplashActivity.appCredentialPassword, authscope);        
	        
	        try {
				StringEntity requestData=new StringEntity(jsonData);
		        
		        client.post(_instance.getApplication().getApplicationContext(),
		        		url, requestData, "application/json", new RCSJsonHttpResponseHandler() {
		        	@Override
		            public void onSuccess(JSONObject response, int errorCode) {
		        		Log.d("ChatSessionActivity", "sendAdhocMessage::success = "+response.toString()+" errorCode="+errorCode);

		        		JSONObject resourceReference=Utils.getJSONObject(response, "resourceReference");
		        		String resourceURL=Utils.getJSONStringElement(resourceReference, "resourceURL");
	        			String messageId=Utils.getMessageIdFromResourceURL(resourceURL, destinationUri);
	        			Log.d("ChatSessionActivity", "messageId="+messageId+" resourceURL="+resourceURL);
		        		
		        		if (errorCode==201) {
							ContactStateManager.setMessageIdForSentMessage(messageInternalId, messageId, resourceURL);
							ContactStateManager.updateStatusFor(messageId, ChatMessage.MESSAGE_STATUS_SENT);
							messageHandler.sendEmptyMessage(0);
		        		}
		        	}
		        });
			} catch (UnsupportedEncodingException e) { }

		} catch (JSONException e1) {
		}
		
	}
	

	/*
	 * background thread to deal with 'isComposing' indicator
	 */
	public void run() {
		long lastSent=0;
		
		while (running) {
			try {
				Thread.sleep (1000);
								
				if (ContactStateManager.haveSentMessageTo(destinationUri) && running) {
					EditText messageInputBox=(EditText) findViewById(R.id.message_input_box);
					Editable text=messageInputBox.getText();
					String trimmed=text.toString().trim();
					if (trimmed.length()>0) {
						long now=System.currentTimeMillis();
						if ((now-lastSent)>=(COMPOSINGUPDATEFREQUENCYSECONDS*1000)) {
					        sendComposingIndicator(SplashActivity.userId, destinationUri,"active",COMPOSINGUPDATEFREQUENCYSECONDS,new java.util.Date(), "text/plain");
					        lastSent=now;
						}
					    sentComposing=true;
					} else if (sentComposing) {
						clearComposingIndicator();
						lastSent=0;
					}
				}	
			} catch (InterruptedException ie) {}
		}
	}
	
	/*
	 * send the isComposing indicator (generic)
	 */
	private void sendComposingIndicator(String userId, String contactId, String state, int refresh, java.util.Date lastActive, String contentType) {
		final String pingUrl=ServiceURL.getSendIsComposingAutomaticContactURL(SplashActivity.userId, contactId);
		JSONObject isComposing=new JSONObject();
		try {
	        AsyncHttpClient client = new AsyncHttpClient();
	        AuthScope authscope=new AuthScope(ServiceURL.serverName, ServiceURL.serverPort, AuthScope.ANY_REALM);
	        client.setBasicAuth(SplashActivity.userId, SplashActivity.appCredentialPassword, authscope);        

	        isComposing.put("state", state);
			isComposing.put("refresh", refresh);
			isComposing.put("contentType", contentType);
			//isComposing.put("lastActive", lastActive);
			String jsonData="{\"isComposing\":"+isComposing.toString()+"}";
	        try {
				StringEntity requestData=new StringEntity(jsonData);
		        
		        client.post(_instance.getApplication().getApplicationContext(),
		        		pingUrl, requestData, "application/json", new RCSJsonHttpResponseHandler() {
		        	@Override
		            public void onSuccess(JSONObject response, int statusCode) {
		        		Log.d("ChatSessionActivity", "sendComposingIndicator::response = "+(response!=null?response.toString():null)+" statusCode="+statusCode);
		        	}
		        });
	        } catch (UnsupportedEncodingException e) { }
		} catch (JSONException e) { }
	}

	/*
	 * clear the isComposing indicator
	 */
	private void clearComposingIndicator() {
        sendComposingIndicator(SplashActivity.userId, destinationUri,"idle",15,new java.util.Date(), "text/plain");
        sentComposing=false;
	}

	/*
	 * called on startup of the activity to set the state of the contact
	 */
	public static void setContactState(ContactState contactState) {
		ChatSessionActivity.contactState=contactState;
	}
	
	/*
	 * called from the MainActivity when a message (send) status has been updated
	 */
	public static void updateStatus(String messageId, String status) {
		ContactStateManager.updateStatusFor(messageId, status);
		messageHandler.sendEmptyMessage(0);
	}

	/* 
	 * called from the MainActivity when the isComposing indicator has been received
	 */
	public static void updateComposingIndicator(String state) {
		int code=(state!=null && state.equalsIgnoreCase("active"))?1:0;
		composingIndicatorHandler.sendEmptyMessage(code);
	}
	
}
