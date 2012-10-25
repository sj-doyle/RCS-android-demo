package org.gsm.rcsApp.activities;

import java.io.UnsupportedEncodingException;

import org.apache.http.auth.AuthScope;
import org.apache.http.auth.Credentials;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.entity.StringEntity;
import org.gsm.rcsApp.ServiceURL;
import org.gsm.rcsApp.RCS.Contact;
import org.gsm.rcsApp.misc.RCSJsonHttpResponseHandler;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.loopj.android.http.AsyncHttpClient;
import org.gsm.RCSDemo.R;

public class EditContactActivity extends Activity {
	
	String contactUri=null;
	private static Handler closeHandler = null;
	private static Handler errorHandler = null;
	
	private static EditContactActivity _instance=null;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_contact);
        
        _instance=this;

        Intent intent = getIntent();
        Contact retrievedContact=intent.getParcelableExtra(MainActivity.SELECTED_CONTACT);

        String displayName=retrievedContact.getDisplayName();
        contactUri=retrievedContact.getContactId();
        this.setTitle(contactUri);
        
        TextView editContactDisplayNameInput=(TextView) findViewById(R.id.editContactDisplayNameInput);
        
        editContactDisplayNameInput.setText(displayName);
        
        closeHandler = new Handler() {
    		public void handleMessage(Message msg) {
    			super.handleMessage(msg);
    			finish();
    		}
        };
        errorHandler = new Handler() {
    		public void handleMessage(Message msg) {
    			super.handleMessage(msg);
        		int code=msg.what;
        		String error=(String) msg.obj;
        		Log.d("EditContactActivity", "Error "+code+" description="+error);
        		Context context = getApplicationContext();
        		CharSequence text = "Error "+code+(error!=null?" \""+error+"\"":"");
        		Toast toast = Toast.makeText(context, text, Toast.LENGTH_LONG);
        		toast.show();
    		}
        };
    }
    
    public void saveContact(View view) {
        TextView editContactDisplayNameInput=(TextView) findViewById(R.id.editContactDisplayNameInput);
        final String newDisplayName=editContactDisplayNameInput.getText().toString();

        final String addurl=ServiceURL.getAddContactURL(SplashActivity.userId, contactUri);
        
        try {
            JSONObject contact=new JSONObject();
            contact.put("contactId", contactUri);
            JSONObject attributeList=new JSONObject();
			contact.put("attributeList", attributeList);
			JSONArray attribute=new JSONArray();
			attributeList.put("attribute", attribute);
			JSONObject displayNameAttribute=new JSONObject();
			attribute.put(0, displayNameAttribute);
			displayNameAttribute.put("name", "display-name");
			displayNameAttribute.put("value", newDisplayName);

	        AsyncHttpClient client = new AsyncHttpClient();
	        AuthScope authscope=new AuthScope(ServiceURL.serverName, ServiceURL.serverPort, AuthScope.ANY_REALM);
	        client.setBasicAuth(SplashActivity.userId, SplashActivity.appCredentialPassword, authscope);        
	        
	        String jsonData="{\"contact\":"+contact.toString()+"}";
	        
			StringEntity requestData=new StringEntity(jsonData);
	        
	        client.put(_instance.getApplication().getApplicationContext(),
	        		addurl, requestData, "application/json", new RCSJsonHttpResponseHandler() {
	        	@Override
	            public void onSuccess(JSONObject response, int errorCode) {
	        		Log.d("EditContactActivity", "saveContact::success = "+response.toString()+" errorCode="+errorCode);
	        		
	        		if (errorCode==201 || errorCode==200) {
	        			closeHandler.sendEmptyMessage(0);
	        		} else {
						Message msg=new Message();
						msg.what=errorCode;
						msg.obj="contact could not be saved";
						errorHandler.sendMessage(msg);
	        		}
	        	}
	        	public void onFailure(Throwable error, JSONObject response, int statusCode) {
	        		Log.d("EditContactActivity", "Failure response is "+statusCode+" "+(response!=null?response.toString():null));
					Message msg=new Message();
					msg.what=statusCode;
					msg.obj="contact could not be saved";
					errorHandler.sendMessage(msg);
	        	}
	        });
		} catch (JSONException e1) { 
		} catch (UnsupportedEncodingException e) {
		}

    }

    public void deleteContact(View view) {
        final String deleteurl=ServiceURL.getDeleteContactURL(SplashActivity.userId, contactUri);
        
        AsyncHttpClient client = new AsyncHttpClient();
        AuthScope authscope=new AuthScope(ServiceURL.serverName, ServiceURL.serverPort, AuthScope.ANY_REALM);
        client.setBasicAuth(SplashActivity.userId, SplashActivity.appCredentialPassword, authscope);        
        
        RCSJsonHttpResponseHandler deleterequestHandler=new RCSJsonHttpResponseHandler() {
        	public void onSuccess(String response, int statusCode) {
        		Log.d("EditContactActivity", "Delete response is "+statusCode+" "+response);
        		if (statusCode==204) {
					closeHandler.sendEmptyMessage(0);
        		}
        	}
        	public void onFailure(Throwable error, JSONObject response, int statusCode) {
        		Log.d("EditContactActivity", "Failure response is "+statusCode+" "+(response!=null?response.toString():null));
				Message msg=new Message();
				msg.what=statusCode;
				msg.obj="contact could not be deleted";
				errorHandler.sendMessage(msg);
        	}
        };
        client.delete(deleteurl, deleterequestHandler);
    }

    public void cancelSave(View view) {
    	finish();
    }
    
}
