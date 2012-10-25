package org.gsm.rcsApp.activities;

import java.io.UnsupportedEncodingException;

import org.apache.http.auth.AuthScope;
import org.apache.http.auth.Credentials;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.entity.StringEntity;
import org.gsm.rcsApp.ServiceURL;
import org.gsm.rcsApp.misc.RCSJsonHttpResponseHandler;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.loopj.android.http.AsyncHttpClient;
import org.gsm.RCSDemo.R;

public class AddContactActivity extends Activity {

	private static Handler closeHandler = null;
	private static Handler errorHandler = null;
	
	private static AddContactActivity _instance=null;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_contact);
        
        _instance=this;

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
        		Log.d("AddContactActivity", "Error "+code+" description="+error);
        		Context context = getApplicationContext();
        		CharSequence text = "Error "+code+(error!=null?" \""+error+"\"":"");
        		Toast toast = Toast.makeText(context, text, Toast.LENGTH_LONG);
        		toast.show();
    		}
        };
        TextView addContactUriInput=(TextView) findViewById(R.id.addContactUriInput);
        addContactUriInput.setText("tel:");
    }
    
    public void saveContact(View view) {
        TextView addContactDisplayNameInput=(TextView) findViewById(R.id.addContactDisplayNameInput);
        final String displayName=addContactDisplayNameInput.getText().toString();

        TextView addContactUriInput=(TextView) findViewById(R.id.addContactUriInput);
        final String contactUri=addContactUriInput.getText().toString();
        
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
			displayNameAttribute.put("value", displayName);

	        AsyncHttpClient client = new AsyncHttpClient();
	        AuthScope authscope=new AuthScope(ServiceURL.serverName, ServiceURL.serverPort, AuthScope.ANY_REALM);
	        client.setBasicAuth(SplashActivity.userId, SplashActivity.appCredentialPassword, authscope);        
	        
	        String jsonData="{\"contact\":"+contact.toString()+"}";
	        
			StringEntity requestData=new StringEntity(jsonData);
	        
	        client.put(_instance.getApplication().getApplicationContext(),
	        		addurl, requestData, "application/json", new RCSJsonHttpResponseHandler() {
	        	@Override
	            public void onSuccess(JSONObject response, int errorCode) {
	        		Log.d("AddContactActivity", "saveContact::success = "+response.toString()+" errorCode="+errorCode);
	        		
	        		if (errorCode==201 || errorCode==200) {
	        			closeHandler.sendEmptyMessage(0);
	        		} else {
						Message msg=new Message();
						msg.what=errorCode;
						errorHandler.sendMessage(msg);
	        		}
	        	}
	        });

		} catch (JSONException e1) { 
		} catch (UnsupportedEncodingException e) {
		}
    }

    public void cancelSave(View view) {
    	finish();
    }
    
}
