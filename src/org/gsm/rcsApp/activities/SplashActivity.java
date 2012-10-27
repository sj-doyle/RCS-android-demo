package org.gsm.rcsApp.activities;

import java.io.UnsupportedEncodingException;
import java.net.Authenticator;
import java.net.PasswordAuthentication;
import java.util.ArrayList;

import org.apache.http.auth.AuthScheme;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.Credentials;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.entity.StringEntity;
import org.gsm.RCSDemo.R;
import org.gsm.rcsApp.ServiceURL;
import org.gsm.rcsApp.misc.RCSJsonHttpResponseHandler;
import org.gsm.rcsApp.misc.Utils;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.Activity;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

import com.loopj.android.http.AsyncHttpClient;

public class SplashActivity extends Activity {

	public static String userId=null;
	
//	public static String appCredentialUsername="NOUSER";
	public static final String appCredentialPassword="3Kvm4\"DD";
	
	static SplashActivity _instance=null;
	
	public static String notificationChannelURL=null;
	public static String notificationChannelResourceURL=null;
	
	public static ArrayList<String> notificationSubscriptions=new ArrayList<String>();  
	
	
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);
        
        _instance=this;
    }
    
    public void onStart() {
		super.onStart();
	
		final TextView splashStatusIndicator=(TextView) findViewById(R.id.splashStatusIndicator);
		splashStatusIndicator.setVisibility(View.VISIBLE);
		splashStatusIndicator.setText("enter username / password");		

//		Authenticator.setDefault(new Authenticator()
//		{
//			protected PasswordAuthentication getPasswordAuthentication()
//			{
//				PasswordAuthentication pa = new PasswordAuthentication(SplashActivity.appCredentialUsername, SplashActivity.appCredentialPassword.toCharArray());
//				return pa;
//			}
//		});
		
        AsyncHttpClient client = new AsyncHttpClient();
        
        
        
//        client.getCredentialsProvider()
//        
//        client.getHttpClient().getCredentialsProvider().setCredentials(
//                new AuthScope("localhost", 443), 
//                new UsernamePasswordCredentials("username", "password"));
//       
//        client.setRealm(realm);
//        client.setBasicAuth(SplashActivity.appCredentialUsername, SplashActivity.appCredentialPassword);

		if (userId!=null) {
			/*
			 * De-register the previously logged in user
			 */

	        AuthScope authscope=new AuthScope(ServiceURL.serverName, ServiceURL.serverPort, AuthScope.ANY_REALM);
	        client.setBasicAuth(userId, SplashActivity.appCredentialPassword, authscope);	        

	        final String url=ServiceURL.unregisterURL(userId);
	        
	        Log.d("SplashActivity", "Unregistering user");
	        
	        client.delete(url, new RCSJsonHttpResponseHandler() {
	        	@Override
				public void onSuccess(String response, int statusCode) {
	        		Log.d("SplashActivity", "unregister::success status="+statusCode);
				}
	        });
		
			/*
			 * Clear previous notification subscriptions  
			 */
			if (notificationSubscriptions.size()>0) {
				for (final String durl:notificationSubscriptions) {
			        client.delete(durl, new RCSJsonHttpResponseHandler() {
			        	@Override
						public void onSuccess(String response, int statusCode) {
							Log.d("SplashActivity", "deleted subscription status="+statusCode+" response="+response);
						}
			        });
				}
				notificationSubscriptions.clear();
			}
			if (notificationChannelResourceURL!=null) {
				final String durl=notificationChannelResourceURL;
		        client.delete(durl, new RCSJsonHttpResponseHandler() {
		        	@Override
					public void onSuccess(String response, int statusCode) {
						Log.d("SplashActivity", "deleted notification channel status="+statusCode+" response="+response);
					}
		        });
		        notificationChannelResourceURL=null;
			}
	        userId=null;
		}
		
		MainActivity.stopMainActivity();
    }
    
    public void proceedToMain(View view) {
    	EditText splashUsernameInput=(EditText) findViewById(R.id.splashUsernameInput);
		EditText splashPasswordInput=(EditText) findViewById(R.id.splashPasswordInput);
		
		final TextView splashStatusIndicator=(TextView) findViewById(R.id.splashStatusIndicator);
 
		final String username=splashUsernameInput.getText().toString();

		@SuppressWarnings("unused")
		final String password=splashPasswordInput.getText().toString();
		
		splashStatusIndicator.setVisibility(View.INVISIBLE);
		
		if (username!=null && username.trim().length()>0) {
  
	        AsyncHttpClient client = new AsyncHttpClient();
	        AuthScope authscope=new AuthScope(ServiceURL.serverName, ServiceURL.serverPort, AuthScope.ANY_REALM);
	        client.setBasicAuth(username, SplashActivity.appCredentialPassword, authscope);
	        
	        final String url=ServiceURL.registerURL(username);
	        
	        client.post(url, new RCSJsonHttpResponseHandler() {
		        boolean successReceived=false;

	        	@Override
	            public void onSuccess(String response, int responseCode) {
	        		Log.d("SplashActivity", "proceedToMain::success status="+responseCode);
	                if (responseCode==204) {
		            	userId=username;
		            	registerForNotifications();
		              	Intent intent = new Intent(_instance, MainActivity.class);
		            	successReceived=true;
		            	startActivity(intent);
	                } else if (responseCode==401) {
		    			splashStatusIndicator.setVisibility(View.VISIBLE);
		    			splashStatusIndicator.setText("invalid username / password");			            	
		            	successReceived=true;
	                }
	            }
	
				@Override
	            public void onStart() {
	                // Initiated the request
	    			splashStatusIndicator.setVisibility(View.VISIBLE);
	    			splashStatusIndicator.setText("sending login request");		
	            }
	        
	            @Override
	            public void onFailure(Throwable e, String response) {
	                // Response failed :(
	    			splashStatusIndicator.setVisibility(View.VISIBLE);
	    			splashStatusIndicator.setText("login request failed");
	    			System.out.println("Response "+response);
	    			System.out.println(e.toString());
	            }
	
	            @Override
	            public void onFinish() {
	                // Completed the request (either success or failure)
	            	if (!successReceived) {
		    			splashStatusIndicator.setVisibility(View.VISIBLE);
		    			splashStatusIndicator.setText("login request finished - unknown failure");
	            	}
	            }
	        });
		} else {
			splashStatusIndicator.setVisibility(View.VISIBLE);
			splashStatusIndicator.setText("enter username / password");		
		}

    }
    
    private void registerForNotifications() {
        AsyncHttpClient client = new AsyncHttpClient();
        AuthScope authscope=new AuthScope(ServiceURL.serverName, ServiceURL.serverPort, AuthScope.ANY_REALM);
        client.setBasicAuth(SplashActivity.userId, SplashActivity.appCredentialPassword, authscope);        
        
        final String url=ServiceURL.createNotificationChannelURL(userId);
        
        String jsonData="{\"notificationChannel\": { \"channelData\": { \"maxNotifications\": 100 }, \"applicationTag\": \"GSMA RCS Demo\", "+
        				"\"channelLifetime\": 0, \"channelType\": \"LongPolling\"}}";
        
        try {
			StringEntity requestData=new StringEntity(jsonData);
	        
	        client.post(_instance.getApplication().getApplicationContext(),
	        		url, requestData, "application/json", new RCSJsonHttpResponseHandler() {
	        	@Override
	            public void onSuccess(JSONObject response, int statusCode) {
	        		Log.d("SplashActivity", "registerForNotifications::success = "+response.toString()+" statusCode="+statusCode);
	        		
	        		if (statusCode==201) {
	        			JSONObject notificationChannel=Utils.getJSONObject(response, "notificationChannel");
	        			String callbackURL=Utils.getJSONStringElement(notificationChannel, "callbackURL");
	        			notificationChannelResourceURL=Utils.getJSONStringElement(notificationChannel, "resourceURL");
	        			JSONObject channelData=Utils.getJSONObject(notificationChannel, "channelData");
	        			notificationChannelURL=channelData!=null?Utils.getJSONStringElement(channelData, "channelURL"):null;
	        			Log.d("SplashActivity", "callbackURL = "+callbackURL);
	        			Log.d("SplashActivity", "resourceURL = "+notificationChannelResourceURL);
	        			Log.d("SplashActivity", "channelURL = "+notificationChannelURL);
	        			
	        			subscribeToAddressBookNotifications(callbackURL);
	        			subscribeToSessionNotifications(callbackURL);
	        			subscribeToChatNotifications(callbackURL);
	        		}
	        	}


	        });
		} catch (UnsupportedEncodingException e) { }

	}

	private void subscribeToAddressBookNotifications(String callbackURL) {
		try {
			JSONObject abChangesSubscription=new JSONObject();
			JSONObject callbackReference=new JSONObject();
			callbackReference.put("callbackData", userId);
			callbackReference.put("notifyURL", callbackURL);
			abChangesSubscription.put("callbackReference", callbackReference);
			abChangesSubscription.put("duration", (int) 0);
			String jsonData="{\"abChangesSubscription\":"+abChangesSubscription.toString()+"}";
			Log.d("SplashActivity", "Subscription request data = "+jsonData);
			
	        AsyncHttpClient client = new AsyncHttpClient();
	        AuthScope authscope=new AuthScope(ServiceURL.serverName, ServiceURL.serverPort, AuthScope.ANY_REALM);
	        client.setBasicAuth(SplashActivity.userId, SplashActivity.appCredentialPassword, authscope);        
	        
	        final String url=ServiceURL.createAddressBookChangeSubscriptionURL(userId);
	        try {
				StringEntity requestData=new StringEntity(jsonData);
		        
		        client.post(_instance.getApplication().getApplicationContext(),
		        		url, requestData, "application/json", new RCSJsonHttpResponseHandler() {
		        	@Override
		            public void onSuccess(JSONObject response, int statusCode) {
		        		Log.d("SplashActivity", "subscribeToAddressBookNotifications::success = "+response.toString()+" statusCode="+statusCode);
		        		if (statusCode==201) {
			        		String resourceURL=Utils.getResourceURL(Utils.getJSONObject(response, "abChangesSubscription"));
			        		if (resourceURL!=null) notificationSubscriptions.add(resourceURL);
		        		}
		        	}
		        });
			} catch (UnsupportedEncodingException e) { }

		} catch (JSONException e) {
		}
		
	}

	private void subscribeToSessionNotifications(String callbackURL) {
		try {
			JSONObject sessionSubscription=new JSONObject();
			JSONObject callbackReference=new JSONObject();
			callbackReference.put("callbackData", userId);
			callbackReference.put("notifyURL", callbackURL);
			sessionSubscription.put("callbackReference", callbackReference);
			sessionSubscription.put("duration", (int) 0);
			String jsonData="{\"sessionSubscription\":"+sessionSubscription.toString()+"}";
			Log.d("SplashActivity", "Subscription request data = "+jsonData);
			
	        AsyncHttpClient client = new AsyncHttpClient();
	        AuthScope authscope=new AuthScope(ServiceURL.serverName, ServiceURL.serverPort, AuthScope.ANY_REALM);
	        client.setBasicAuth(SplashActivity.userId, SplashActivity.appCredentialPassword, authscope);        
	        
	        final String url=ServiceURL.createSessionChangeSubscriptionURL(userId);
	        try {
				StringEntity requestData=new StringEntity(jsonData);
		        
		        client.post(_instance.getApplication().getApplicationContext(),
		        		url, requestData, "application/json", new RCSJsonHttpResponseHandler() {
		        	@Override
		            public void onSuccess(JSONObject response, int statusCode) {
		        		Log.d("SplashActivity", "subscribeToSessionNotifications::success = "+response.toString()+" statusCode="+statusCode);
		        		if (statusCode==201) {
			        		String resourceURL=Utils.getResourceURL(Utils.getJSONObject(response, "sessionSubscription"));
			        		if (resourceURL!=null) notificationSubscriptions.add(resourceURL);
		        		}
		        	}
		        });
			} catch (UnsupportedEncodingException e) { }

		} catch (JSONException e) {
		}
		
	}

	private void subscribeToChatNotifications(String callbackURL) {
		try {
			JSONObject chatSubscription=new JSONObject();
			JSONObject callbackReference=new JSONObject();
			callbackReference.put("callbackData", userId);
			callbackReference.put("notifyURL", callbackURL);
			chatSubscription.put("callbackReference", callbackReference);
			chatSubscription.put("duration", (int) 0);
			String jsonData="{\"chatNotificationSubscription\":"+chatSubscription.toString()+"}";
			Log.d("SplashActivity", "Subscription request data = "+jsonData);
			
	        AsyncHttpClient client = new AsyncHttpClient();
	        AuthScope authscope=new AuthScope(ServiceURL.serverName, ServiceURL.serverPort, AuthScope.ANY_REALM);
	        client.setBasicAuth(SplashActivity.userId, SplashActivity.appCredentialPassword, authscope);        
	        
	        final String url=ServiceURL.createChatSubscriptionURL(userId);
	        try {
				StringEntity requestData=new StringEntity(jsonData);
		        
		        client.post(_instance.getApplication().getApplicationContext(),
		        		url, requestData, "application/json", new RCSJsonHttpResponseHandler() {
		        	@Override
		            public void onSuccess(JSONObject response, int statusCode) {
		        		Log.d("SplashActivity", "subscribeToChatNotifications::success = "+response.toString()+" statusCode="+statusCode);
		        		if (statusCode==201) {
			        		String resourceURL=Utils.getResourceURL(Utils.getJSONObject(response, "chatNotificationSubscription"));
			        		if (resourceURL!=null) notificationSubscriptions.add(resourceURL);
		        		}
		        	}
		        });
			} catch (UnsupportedEncodingException e) { }

		} catch (JSONException e) {
		}
		
	}
	
	
	
}
