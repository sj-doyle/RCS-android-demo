package org.gsm.rcsApp.RCS;

import android.os.Parcel;
import android.os.Parcelable;

public class ChatSession implements Parcelable {

	String destinationUri=null;
	String sessionId=null;
	String messageId=null;
	
	public String getDestinationUri() {
		return destinationUri;
	}

	public String getSessionId() {
		return sessionId;
	}

	public String getMessageId() {
		return messageId;
	}

	public void setDestinationUri(String destinationUri) {
		this.destinationUri = destinationUri;
	}

	public void setSessionId(String sessionId) {
		this.sessionId = sessionId;
	}

	public void setMessageId(String messageId) {
		this.messageId = messageId;
	}

	public int describeContents() {
        return 0;
    }
	
    public static final Parcelable.Creator<ChatSession> CREATOR = new Parcelable.Creator<ChatSession>() {
        public ChatSession createFromParcel(Parcel in) {
            return new ChatSession(in);
        }

        public ChatSession[] newArray(int size) {
            return new ChatSession[size];
        }
    };

	public void writeToParcel(Parcel out, int flags) {
        out.writeString(destinationUri);
        out.writeString(sessionId);
        out.writeString(messageId);
    }

	public ChatSession() {
		
	}
	
	public ChatSession(Parcel in) {
		destinationUri = in.readString();
		sessionId = in.readString();
		messageId = in.readString();
    }
}
