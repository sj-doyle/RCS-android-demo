package org.gsm.rcsApp.activities;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.HashMap;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.Credentials;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.gsm.rcsApp.ServiceURL;
import org.gsm.rcsApp.RCS.ChatMessage;
import org.gsm.rcsApp.RCS.ChatSessionManager;
import org.gsm.rcsApp.RCS.Contact;
import org.gsm.rcsApp.RCS.ContactState;
import org.gsm.rcsApp.RCS.ContactStateManager;
import org.gsm.rcsApp.adapters.ContactRowAdapter;
import org.gsm.rcsApp.misc.RCSJsonHttpResponseHandler;
import org.gsm.rcsApp.misc.Utils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.Vibrator;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;

import com.loopj.android.http.AsyncHttpClient;
import org.gsm.RCSDemo.R;

public class MainActivity extends Activity implements Runnable {
	public final static String SELECTED_CONTACT = "com.mobilelife.rcsApp.activities.MainActivity.SELECTEDCONTACT";
	
	public static ContactStateManager contactStateManager=new ContactStateManager();
	
    private static ArrayList<Contact> retrievedContacts=new ArrayList<Contact>();
    private static HashMap<String,Contact> contactMap=new HashMap<String,Contact>();
    
    public static ChatSessionManager chatSessionCache=new ChatSessionManager();

	static Thread background=null;
	static boolean running=false;
	
	private static HashMap<String, ArrayList<ChatMessage>> messageCache=new HashMap<String, ArrayList<ChatMessage>>();

	static MainActivity _instance=null;
	static ContactRowAdapter cradapter=null;
	static ListView contactListView=null;

	private static Handler changeStateHandler = null;
	private static Handler contactChangeHandler = null;
	
	static String currentChatSessionContactUri=null;
	
	private static final int MAIN_LOOP_DELAY=5000;
	private static final int LONGPOLL_TIMEOUT=30000;
	
	private static Vibrator v = null;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

		_instance=this;		
		
		v = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);

    	ContactStateManager.reset();
    	contactStateManager.clearCache();

		contactListView=(ListView) findViewById(R.id.contactList);
		
		cradapter=new ContactRowAdapter(this, retrievedContacts);
		contactListView.setAdapter(cradapter);
		contactListView.setFocusable(true);
		
	    contactListView.setOnItemClickListener(new ListView.OnItemClickListener() {
	    	@SuppressWarnings("rawtypes")
			public void onItemClick(AdapterView parent, View view, int position, long id) {
	    		Contact selectedRecord=retrievedContacts.get(position);
	    		
	    		currentChatSessionContactUri=selectedRecord.getContactId();
	    		selectedRecord.setHasNewMessage(false);
	    		
	    		ContactState contactState=contactStateManager.getOrCreateContactState(currentChatSessionContactUri);
				ChatSessionActivity.setContactState(contactState);
				contactState.setNewMessage(false);
	    		
	    		contactStateManager.setChatVisible(currentChatSessionContactUri, true);
				
	    		Intent intent = new Intent(_instance, ChatSessionActivity.class);
	    		intent.putExtra(SELECTED_CONTACT, selectedRecord);
	    		startActivity(intent);
	    	}
	    });
	    
	    contactListView.setOnItemLongClickListener(new ListView.OnItemLongClickListener() {
	    	@SuppressWarnings("rawtypes")
			public boolean onItemLongClick(AdapterView parent, View view, int position, long id) {
	    		Contact selectedRecord=retrievedContacts.get(position);
	    		Intent intent = new Intent(_instance, EditContactActivity.class);
	    		intent.putExtra(SELECTED_CONTACT, selectedRecord);
	    		startActivity(intent);
				return true;
			}
	    	
	    });
	    
	    contactChangeHandler = new Handler() {
    		public void handleMessage(Message msg) {
    			super.handleMessage(msg);
    			refreshContacts();
    		}	    	
	    };

		changeStateHandler = new Handler() {
    		public void handleMessage(Message msg) {
    			super.handleMessage(msg);
    			cradapter.notifyDataSetChanged();
    			contactListView.refreshDrawableState();
    		}
    	};

    }
    
    
    
    public void onStart() {
		super.onStart();
		
		if (currentChatSessionContactUri!=null) {
			contactStateManager.setChatVisible(currentChatSessionContactUri, false);
		}
        
        if (!running) {
	        running=true;
	        background=new Thread(this);
	        background.start();
        }
        
        refreshContacts();
    }
    
    public static void stopMainActivity() {
    	running=false;
    	if (background!=null) {
    		running=false;
    		if (background!=null) {
    			background.interrupt();
    			background=null;
    		}
    	}
    	contactStateManager.clearCache();
    	ContactStateManager.reset();
    }
    
    public void refreshContacts() {
    	final String url=ServiceURL.getContactListURL(SplashActivity.userId);

    	if (SplashActivity.userId!=null) {
	        AsyncHttpClient client = new AsyncHttpClient();
	        AuthScope authscope=new AuthScope(ServiceURL.serverName, ServiceURL.serverPort, AuthScope.ANY_REALM);
	        client.setBasicAuth(SplashActivity.userId, SplashActivity.appCredentialPassword, authscope);        
	
	    	client.get(url, new RCSJsonHttpResponseHandler() {
	            @Override
	            public void onSuccess(JSONObject response, int errorCode) {
	            	Log.d("MainActivity", "refreshContacts errorCode="+errorCode+" response="+(response!=null?response.toString():null));
	            	if (response!=null) {
	            		try {
	            			
							if (errorCode==200) {
								JSONObject contactCollection=Utils.getJSONObject(response, "contactCollection");
								if (contactCollection!=null) {
									JSONArray contactList=Utils.getJSONArray(contactCollection, "contact");
									retrievedContacts.clear();
									if (contactList!=null) {
										for (int i=0; i<contactList.length(); i++) {
											JSONObject contact=(JSONObject) contactList.get(i);
											
											Log.d("MainActivity", "["+i+"] = "+contact.toString());
											
											String contactId=Utils.getJSONStringElement(contact, "contactId");
											String resourceURL=null;
											String displayName=null;
											String capabilities=null;
											
											JSONObject attributeList=Utils.getJSONObject(contact, "attributeList");
											if (attributeList!=null) {
												resourceURL=Utils.getJSONStringElement(attributeList, "resourceURL");
												JSONArray attributes=Utils.getJSONArray(attributeList, "attribute");
												if (attributes!=null) {
													for (int a=0; a<attributes.length();a++) {
														JSONObject attribute=attributes.getJSONObject(a);
														String name=Utils.getJSONStringElement(attribute, "name");
														String value=Utils.getJSONStringElement(attribute, "value");
														if (name!=null && name.equals("display-name")) {
															displayName=value;
														}
														if (name!=null && name.equals("capabilities")) {
															capabilities=value;
														}
													}
												}
													
											}
	//										String icon=contact.getString("icon");
	//										String status=contact.getString("status");
		
											ContactState cs=contactStateManager.getOrCreateContactState(contactId);
		
											Contact contactRecord=new Contact();
											contactRecord.setContactId(contactId);
											contactRecord.setResourceURL(resourceURL);
											contactRecord.setDisplayName(displayName);
											contactRecord.setCapabilities(capabilities);
	//										contactRecord.setIcon(icon);
	//										contactRecord.setStatus(status);
											contactRecord.setHasNewMessage(cs.isNewMessage());
											
											retrievedContacts.add(contactRecord);	
											contactMap.put(contactId, contactRecord);
										}
										
										changeStateHandler.sendEmptyMessage(0);
									}
	
								}
							}
						} catch (JSONException e) {
							e.printStackTrace();
						}
	            		
	            	}
	            }    
	            
	            public void onFailure(Throwable e, String response) {
	            	Log.d("MainActivity", "No response from "+url);
	            }
	    	});
    	}    	
    }

	public void run() {
		Log.d("MainActivity", "Main background thread started");

		long lastRequestTime=0;
		
		while (running) {
			boolean makeRequest=false;
			
			long now=System.currentTimeMillis();
			
			makeRequest=(now-lastRequestTime)>=MAIN_LOOP_DELAY; // Throttle so that do not drive through too many requests 
			
			final String notificationsUrl=SplashActivity.notificationChannelURL!=null?(SplashActivity.notificationChannelURL.replaceAll("tel\\:\\+", "tel%3A%2B").replaceAll("tel\\:", "tel%3A")):null;
			
			if (notificationsUrl!=null && makeRequest ) {
			
				DefaultHttpClient client = new DefaultHttpClient();
				
				HttpParams myParams = new BasicHttpParams();
			    HttpConnectionParams.setConnectionTimeout(myParams, MAIN_LOOP_DELAY); 
			    HttpConnectionParams.setSoTimeout(myParams, LONGPOLL_TIMEOUT);
			    
			    HttpPost httppost = new HttpPost(notificationsUrl);
			    
				lastRequestTime=now;
				
				HttpResponse response=null;
				try {
					response = client.execute(httppost);
				} catch (java.net.SocketTimeoutException ste) {
					Log.d("MainActivity", "SocketTimeout handled");
				} catch (ClientProtocolException e) {
					Log.d("MainActivity", "ClientProtocolException handled");
				} catch (IOException e) {
					Log.d("MainActivity", "IOException handled");
				}
				
				int statusCode=response!=null&&response.getStatusLine()!=null?response.getStatusLine().getStatusCode():-1;
				JSONObject jsonData=null;
				try {
					jsonData = getJSONDataFromResponse(response);
				} catch (IllegalStateException e) {
					Log.d("MainActivity", "IllegalStateException handled");
				} catch (IOException e) {
					Log.d("MainActivity", "IOException handled");
				} catch (JSONException e) {
					Log.d("MainActivity", "JSONException handled");
				}

				if (jsonData!=null) {
					try {
						processNotificationResponse(statusCode, jsonData);
					} catch (JSONException e) {
						Log.d("MainActivity", "JSONException handled");
					}
				}
				
			} else {
	    		try {
	    			Thread.yield();
	    			Thread.sleep (1000); // milliseconds
	    		} catch (InterruptedException ie) {}
			}
		}
	}
    
    private void processNotificationResponse(int statusCode, JSONObject response) throws JSONException {

		JSONArray notifications=response!=null?response.getJSONArray("notificationList"):null;
		if (notifications!=null && notifications.length()>0) {
			for (int i=0; i<notifications.length(); i++) {
				JSONObject notification=notifications.getJSONObject(i);
				
				Log.d("MainActivity", "Notification ["+i+"] = "+notification.toString());
				
				if (Utils.getJSONObject(notification, "messageNotification")!=null) {
					JSONObject messageNotification=Utils.getJSONObject(notification, "messageNotification");
					
					if (Utils.getJSONObject(messageNotification, "isComposing")!=null) {
						JSONObject isComposing=Utils.getJSONObject(messageNotification, "isComposing");
						String senderAddress=Utils.getJSONStringElement(messageNotification, "senderAddress");
						String status=Utils.getJSONStringElement(isComposing, "status");
						
						if (senderAddress!=null) {
							if (senderAddress.equals(currentChatSessionContactUri)) {
								ChatSessionActivity.updateComposingIndicator(status);
							}
							contactStateManager.updateComposingIndicator(senderAddress, status);
						}
					}
					
					if (Utils.getJSONObject(messageNotification, "chatMessage")!=null) {
						JSONObject chatMessage=Utils.getJSONObject(messageNotification, "chatMessage");
						String senderAddress=Utils.getJSONStringElement(messageNotification, "senderAddress");
						String messageId=Utils.getJSONStringElement(messageNotification, "messageId");
						String sessionId=Utils.getJSONStringElement(messageNotification, "sessionId");
						String messageText=Utils.getJSONStringElement(chatMessage, "text");
						String dateTime=Utils.getJSONStringElement(messageNotification, "dateTime");
						
						ArrayList<ChatMessage> messageBuffer=messageCache.get(senderAddress);
						if (messageBuffer==null) {
							messageBuffer=new ArrayList<ChatMessage>();
							messageCache.put(senderAddress, messageBuffer);
						}
						ChatMessage received=new ChatMessage();
						received.setMessageText(messageText);
						received.setMessageDirection(ChatMessage.MESSAGE_RECEIVED);
						received.setMessageTime(Utils.convertTransferDateToDisplayString(dateTime));
						received.setStatus(ChatMessage.MESSAGE_STATUS_RECEIVED);
						
						String url=ServiceURL.getSendMessageStatusReportURL(SplashActivity.userId, senderAddress, sessionId, messageId);
						
						String jsonData=null;
						
						if (senderAddress!=null) {
							if (senderAddress.equals(currentChatSessionContactUri)) {
								jsonData="{\"messageStatusReport\":{\"status\":\"Displayed\"}}";
								ChatSessionActivity.refreshMessageList(senderAddress, SplashActivity.userId, received);
							} else {
								jsonData="{\"messageStatusReport\":{\"status\":\"Delivered\"}}";
								contactStateManager.storeMessage(senderAddress, received, true, SplashActivity.userId);
								Contact found=contactMap.get(senderAddress);
								if (found!=null && !found.isHasNewMessage()) {
									found.setHasNewMessage(true);
									changeStateHandler.sendEmptyMessage(0);
								}
							}
						}

						AsyncHttpClient responseClient = new AsyncHttpClient();
				        AuthScope authscope=new AuthScope(ServiceURL.serverName, ServiceURL.serverPort, AuthScope.ANY_REALM);
				        responseClient.setBasicAuth(SplashActivity.userId, SplashActivity.appCredentialPassword, authscope);        

						final String responseURL=url;
						Log.d("MainActivity", "Sending message status update "+responseURL+" with "+jsonData);

						StringEntity requestData;
						try {
							requestData = new StringEntity(jsonData);
							responseClient.put(_instance.getApplication().getApplicationContext(),
					        		url, requestData, "application/json", new RCSJsonHttpResponseHandler() {
					        	@Override
					            public void onSuccess(JSONObject response, int errorCode) {
					        		Log.d("MainActivity", "messageStatusReport::success = "+response.toString()+" errorCode="+errorCode);
					        	}
					        	@Override
					            public void onFailure(Throwable e, JSONObject response, int errorCode) {
					        		Log.d("MainActivity", "messageStatusReport::failure = "+response.toString()+" errorCode="+errorCode);
					        	}
							});
						} catch (UnsupportedEncodingException e) {}										
						v.vibrate(200);
					}

				} else if (Utils.getJSONObject(notification, "chatSessionInvitationNotification")!=null) {
					JSONObject chatSessionInvitationNotification=Utils.getJSONObject(notification, "chatSessionInvitationNotification");
					JSONArray link=Utils.getJSONArray(chatSessionInvitationNotification, "link");
					if (link!=null && link.length()>0) {
						for (int li=0; li<link.length(); li++) {
							JSONObject litem=link.getJSONObject(li);
							String rel=Utils.getJSONStringElement(litem, "rel");
							String href=Utils.getJSONStringElement(litem, "href");
							
							if ("ParticipantSessionStatus".equals(rel) && href!=null) {
								AsyncHttpClient acceptClient = new AsyncHttpClient();
						        AuthScope authscope=new AuthScope(ServiceURL.serverName, ServiceURL.serverPort, AuthScope.ANY_REALM);
						        acceptClient.setBasicAuth(SplashActivity.userId, SplashActivity.appCredentialPassword, authscope);        
								try {
									StringEntity requestData = new StringEntity("{\"participantSessionStatus\":{\"status\":\"Connected\"}}");
									acceptClient.put(_instance.getApplication().getApplicationContext(),
							        		href, requestData, "application/json", new RCSJsonHttpResponseHandler() {
							        	@Override
							            public void onSuccess(String response, int errorCode) {
							        		Log.d("MainActivity", "accept chatSessionInvitationNotification::success = "+response+" errorCode="+errorCode);
							        	}
							        	@Override
							            public void onFailure(Throwable e, JSONObject response, int errorCode) {
							        		Log.d("MainActivity", "accept chatSessionInvitationNotification::failure = "+response.toString()+" errorCode="+errorCode);
							        	}
									});
								} catch (UnsupportedEncodingException e) {}										
							}
						}
					}
					
				} else if (Utils.getJSONObject(notification, "sessionEventNotification")!=null) {
					JSONObject sessionEventNotification=Utils.getJSONObject(notification, "sessionEventNotification");
					String event=Utils.getJSONStringElement(sessionEventNotification, "event");
					if ("unregisterSuccess".equalsIgnoreCase(event)) {
						contactChangeHandler.sendEmptyMessage(0);
					}
				} else if (Utils.getJSONObject(notification, "contactEventNotification")!=null) {
					contactChangeHandler.sendEmptyMessage(0);

				} else if (Utils.getJSONObject(notification, "messageStatusNotification")!=null) {
					JSONObject messageStatusNotification=Utils.getJSONObject(notification, "messageStatusNotification");
					String messageId=Utils.getJSONStringElement(messageStatusNotification, "messageId");
					String status=Utils.getJSONStringElement(messageStatusNotification, "status");
					ChatMessage message=ContactStateManager.getMessageForId(messageId);
					Log.d("MainActivity", "updating status messageId="+messageId+" status="+status+" message="+message);
					if (status!=null && message!=null) {
						String contactUri=message.getContactUri();
						if (status.equalsIgnoreCase("delivered")) {
							if (contactUri!=null && contactUri.equals(currentChatSessionContactUri)) {
								ChatSessionActivity.updateStatus(messageId,ChatMessage.MESSAGE_STATUS_DELIVERED);
							} else {
								ContactStateManager.updateStatusFor(messageId,ChatMessage.MESSAGE_STATUS_DELIVERED);
							}
						} else if (status.equalsIgnoreCase("displayed")) {
							if (contactUri!=null && contactUri.equals(currentChatSessionContactUri)) {
								ChatSessionActivity.updateStatus(messageId,ChatMessage.MESSAGE_STATUS_VIEWED);
							} else {
								ContactStateManager.updateStatusFor(messageId,ChatMessage.MESSAGE_STATUS_VIEWED);
							}
						}
					}

				} else if (Utils.getJSONObject(notification, "chatEventNotification")!=null) {
					JSONObject chatEventNotification=Utils.getJSONObject(notification, "chatEventNotification");
					String eventType=Utils.getJSONStringElement(chatEventNotification, "eventType");
					
					if (eventType!=null && eventType.equalsIgnoreCase("SessionEnded")) {
						contactChangeHandler.sendEmptyMessage(0);
					}

				}
					
			}
		}
    	
		
	}



	private JSONObject getJSONDataFromResponse(HttpResponse response) throws IllegalStateException, IOException, JSONException {
    	JSONObject jsonData=null;
		HttpEntity entity = response!=null&&response.getEntity()!=null?response.getEntity():null;
		if (entity != null) {
            InputStream instream = entity.getContent();
            final StringBuilder out = new StringBuilder();
            int bufferSize=1024;
            final char[] buffer = new char[bufferSize];
            int nread;
            InputStreamReader reader = new InputStreamReader(instream);
            while ((nread=reader.read(buffer, 0, buffer.length))>=0) {
            	out.append(buffer, 0, nread);
            }
            if (out.length()>0) jsonData = new JSONObject(out.toString());
		}    
		return jsonData;
	}



	public void addContactClicked(View view) {
		Intent intent = new Intent(_instance, AddContactActivity.class);
		startActivity(intent);
    }

	public static void chatSessionClosed(String destinationUri) {
		if (destinationUri.equals(currentChatSessionContactUri)) {
			currentChatSessionContactUri=null;
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
	    MenuInflater inflater = getMenuInflater();
	    inflater.inflate(R.menu.main_screen_menu, menu);
	    return true;
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
	    // Handle item selection
	    switch (item.getItemId()) {
	        case R.id.refreshContacts:
	        	contactChangeHandler.sendEmptyMessage(0);
	            return true;
	        case R.id.signOut:
	        	finish();
	            return true;
	        default:
	            return super.onOptionsItemSelected(item);
	    }
	}
}
