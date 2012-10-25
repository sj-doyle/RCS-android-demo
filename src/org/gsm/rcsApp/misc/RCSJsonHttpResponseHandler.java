package org.gsm.rcsApp.misc;

import java.io.IOException;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.apache.http.client.HttpResponseException;
import org.apache.http.entity.BufferedHttpEntity;
import org.apache.http.util.EntityUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

import android.os.Handler;
import android.os.Looper;
import android.os.Message;

import com.loopj.android.http.AsyncHttpResponseHandler;

public class RCSJsonHttpResponseHandler extends AsyncHttpResponseHandler {
	
    protected static final int SUCCESS_MESSAGE = 0;
    protected static final int FAILURE_MESSAGE = 1;
    protected static final int START_MESSAGE = 2;
    protected static final int FINISH_MESSAGE = 3;

    private Handler handler;

    /**
     * Creates a new AsyncHttpResponseHandler
     */
    public RCSJsonHttpResponseHandler() {
        // Set up a handler to post events back to the correct thread if possible
        if(Looper.myLooper() != null) {
            handler = new Handler(){
                public void handleMessage(Message msg){
                	RCSJsonHttpResponseHandler.this.handleMessage(msg);
                }
                
            };
        }
    }


    //
    // Callbacks to be overridden, typically anonymously
    //

    /**
     * Fired when the request is started, override to handle in your own code
     */
    public void onStart() {
    	
    }

    /**
     * Fired in all cases when the request is finished, after both success and failure, override to handle in your own code
     */
    public void onFinish() {
    	
    }

    /**
     * Fired when a request returns successfully, override to handle in your own code
     * @param content the body of the HTTP response from the server
     */
    public void onSuccess(String content, int responseCode) {
    	System.out.println("Default onSuccess for responseCode "+responseCode);
    }
    public void onSuccess(JSONObject content, int responseCode) {
    	System.out.println("JSONObject onSuccess for responseCode "+responseCode);
    }
    public void onSuccess(JSONArray content, int responseCode) {
    	System.out.println("JSONArray onSuccess for responseCode "+responseCode);
    }

    /**
     * Fired when a request fails to complete, override to handle in your own code
     * @param error the underlying cause of the failure
     * @deprecated use {@link #onFailure(Throwable, String)}
     */
    public void onFailure(Throwable error) {
    	
    }

    /**
     * Fired when a request fails to complete, override to handle in your own code
     * @param error the underlying cause of the failure
     * @param content the response body, if any
     */
    public void onFailure(Throwable error, String content, int responseCode) {
        // By default, call the deprecated onFailure(Throwable) for compatibility
        onFailure(error);
    	System.out.println("Default onFailure for responseCode "+responseCode+" content="+content);
    }
    public void onFailure(Throwable error, JSONObject content, int responseCode) {
        // By default, call the deprecated onFailure(Throwable) for compatibility
        onFailure(error);
    	System.out.println("JSONObject onFailure for responseCode "+responseCode);
    }
    public void onFailure(Throwable error, JSONArray content, int responseCode) {
        // By default, call the deprecated onFailure(Throwable) for compatibility
        onFailure(error);
    	System.out.println("JSONArray onFailure for responseCode "+responseCode);
    }
    


    //
    // Pre-processing of messages (executes in background threadpool thread)
    //

    protected void sendSuccessMessage(String responseBody, int responseCode) {
        sendMessage(obtainMessage(SUCCESS_MESSAGE, responseBody, responseCode));
    }

    protected void sendFailureMessage(Throwable e, String responseBody, int responseCode) {
        sendMessage(obtainMessage(FAILURE_MESSAGE, new Object[]{e, responseBody}, responseCode));
    }
    
    protected void sendFailureMessage(Throwable e, byte[] responseBody, int responseCode) {
        sendMessage(obtainMessage(FAILURE_MESSAGE, new Object[]{e, responseBody}, responseCode));
    }

    protected void sendStartMessage() {
        sendMessage(obtainMessage(START_MESSAGE, null, 0));
    }

    protected void sendFinishMessage() {
        sendMessage(obtainMessage(FINISH_MESSAGE, null, 0));
    }


    //
    // Pre-processing of messages (in original calling thread, typically the UI thread)
    //

    protected void handleSuccessMessage(String responseBody, int responseCode) {
        try {
            if (responseBody != null) {
                Object jsonResponse = parseResponse(responseBody);
                if(jsonResponse instanceof JSONObject) {
                	onSuccess((JSONObject) jsonResponse, responseCode);
                } else if(jsonResponse instanceof JSONArray) {
                	onSuccess((JSONArray) jsonResponse, responseCode);
                } else {
                    onSuccess(responseBody, responseCode);
                }
            }else {
            	onSuccess(responseBody, responseCode);
            }
        }catch(JSONException ex) {
        	onSuccess(responseBody, responseCode);
        }
        
    }

    protected void handleFailureMessage(Throwable e, String responseBody, int responseCode) {
        try {
            if (responseBody != null) {
                Object jsonResponse = parseResponse(responseBody);
                if(jsonResponse instanceof JSONObject) {
                    onFailure(e, (JSONObject) jsonResponse, responseCode);
                } else if(jsonResponse instanceof JSONArray) {
                    onFailure(e, (JSONArray) jsonResponse, responseCode);
                } else {
                    onFailure(e, responseBody, responseCode);
                }
            }else {
                onFailure(e, "", responseCode);
            }
        }catch(JSONException ex) {
            onFailure(e, responseBody, responseCode);
        }
    }

    protected Object parseResponse(String responseBody) throws JSONException {
        Object result = null;
        //trim the string to prevent start with blank, and test if the string is valid JSON, because the parser don't do this :(. If Json is not valid this will return null
		responseBody = responseBody.trim();
		if(responseBody.startsWith("{") || responseBody.startsWith("[")) {
			result = new JSONTokener(responseBody).nextValue();
		}
		if (result == null) {
			result = responseBody;
		}
		return result;
    }

    // Methods which emulate android's Handler and Message methods
    protected void handleMessage(Message msg) {
        switch(msg.what) {
            case SUCCESS_MESSAGE:
                handleSuccessMessage((String)msg.obj, msg.arg1);
                break;
            case FAILURE_MESSAGE:
                Object[] repsonse = (Object[])msg.obj;
                handleFailureMessage((Throwable)repsonse[0], (String)repsonse[1], msg.arg1);
                break;
            case START_MESSAGE:
                onStart();
                break;
            case FINISH_MESSAGE:
                onFinish();
                break;
        }
    }

    protected void sendMessage(Message msg) {
        if(handler != null){
            handler.sendMessage(msg);
        } else {
            handleMessage(msg);
        }
    }

    protected Message obtainMessage(int responseMessage, Object response, int responseCode) {
        Message msg = null;
        if(handler != null){
            msg = this.handler.obtainMessage(responseMessage, responseCode, 0, response);
        }else{
            msg = new Message();
            msg.what = responseMessage;
            msg.arg1 = responseCode;
            msg.obj = response;
        }
        return msg;
    }


    // Interface to AsyncHttpRequest    
    void sendResponseMessage(HttpResponse response) {
        StatusLine status = response.getStatusLine();
        String responseBody = null;
        try {
            HttpEntity entity = null;
            HttpEntity temp = response.getEntity();
            if(temp != null) {
                entity = new BufferedHttpEntity(temp);
                responseBody = EntityUtils.toString(entity, "UTF-8");
            }
        } catch(IOException e) {            
        }

        if (status.getStatusCode()>=200 && status.getStatusCode()<300) {
        	sendSuccessMessage(responseBody, status.getStatusCode());
        } else {
	        if(status.getStatusCode() >= 300) {
	            sendFailureMessage(new HttpResponseException(status.getStatusCode(), status.getReasonPhrase()), responseBody, status.getStatusCode());
	        } else {
	            sendSuccessMessage(responseBody, status.getStatusCode());
	        }
        }
    }
}
