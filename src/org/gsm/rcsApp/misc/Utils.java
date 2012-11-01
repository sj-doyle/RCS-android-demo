package org.gsm.rcsApp.misc;

import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.text.ParseException;
import java.text.SimpleDateFormat;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class Utils {
	private static SimpleDateFormat transferFormatter=new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssz"); //2012-11-01T20:17:07Z
	private static SimpleDateFormat displayFormatter=new SimpleDateFormat("dd MMM yyyy HH:mm:ss");
	
    public static void CopyStream(InputStream is, OutputStream os)
    {
        final int buffer_size=1024;
        try
        {
            byte[] bytes=new byte[buffer_size];
            for(;;)
            {
              int count=is.read(bytes, 0, buffer_size);
              if(count==-1)
                  break;
              os.write(bytes, 0, count);
            }
        }
        catch(Exception ex){}
    }
    
    public static String URLEncode(String source) {
    	String rv="";
    	if (source!=null) {
    		try {
    			rv=URLEncoder.encode(source, "UTF-8");
    		} catch (UnsupportedEncodingException ue) {} 
    	}
    	return rv;
    }
    
	public static synchronized String getNowAsDisplayString() {
		return displayFormatter.format(new java.util.Date());
	}

	public static synchronized String convertTransferDateToDisplayString(String original) {
		String value=original;
		java.util.Date parsed;
		try {
			if ( original.endsWith( "Z" ) ) {
				original = original.substring( 0, original.length() - 1) + "GMT-00:00";
			}
			parsed = transferFormatter.parse(original);
			value=displayFormatter.format(parsed);
		} catch (ParseException e) { 
			System.out.println("Couldn't convert "+original+" "+e.getMessage());
		}
		return value;
	}
	
	public static String getJSONStringElement(JSONObject object, String identifier) {
		String value=null;
		if (object!=null) {
			try {
				value=object.getString(identifier);
			} catch (JSONException e) { }
		}
		return value;
	}

	public static JSONObject getJSONObject(JSONObject object, String identifier) {
		JSONObject value=null;
		if (object!=null) {
			try {
				value=object.getJSONObject(identifier);
			} catch (JSONException e) { }
		}
		return value;
	}

	public static JSONArray getJSONArray(JSONObject object, String identifier) {
		JSONArray value=null;
		if (object!=null) {
			try {
				value=object.getJSONArray(identifier);
			} catch (JSONException e) { }
		}
		return value;
	}

	public static String getMessageIdFromResourceURL(String resourceURL, String contactId) {
		String messageId=null;
		if (resourceURL!=null) {
			String[] parts=resourceURL.split("/");
			boolean foundContactId=false;
			boolean foundMessageId=false;
			String encodedContactId=Utils.URLEncode(contactId);
			for (int i=0; i<parts.length && !foundMessageId; i++) {
				String part=parts[i];
				if (part!=null) {
					if (part.equalsIgnoreCase(contactId) || part.equalsIgnoreCase(encodedContactId)) {
						foundContactId=true;
					}
					if (foundContactId && part.equalsIgnoreCase("messages") && i<(parts.length-1)) {
						messageId=parts[i+1];
						foundMessageId=true;
					}
				}
			}
		}
		return messageId;
	}

	public static String getResourceURL(JSONObject jsonObject) {
		return jsonObject!=null?getJSONStringElement(jsonObject,"resourceURL"):null;
	}

}