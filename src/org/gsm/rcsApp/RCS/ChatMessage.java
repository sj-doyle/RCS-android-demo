package org.gsm.rcsApp.RCS;

import java.util.UUID;

import android.os.Parcel;
import android.os.Parcelable;

public class ChatMessage implements Parcelable {
	
	public static final String MESSAGE_RECEIVED = "rx";
	public static final String MESSAGE_SENT = "tx";
	
	public static final String MESSAGE_STATUS_PENDING="pending";
	public static final String MESSAGE_STATUS_SENT="sent";
	public static final String MESSAGE_STATUS_DELIVERED="delivered";
	public static final String MESSAGE_STATUS_VIEWED="viewed";
	public static final String MESSAGE_STATUS_RECEIVED = "received";

	String contactUri=null;
	String messageText=null;
	String messageTime=null;
	String messageDirection=null;
	String messageId=null;
	private String messageInternalId=null;
	String status=null;
	boolean viewed=false;
	String resourceURL=null;
	
	public String getContactUri() {
		return contactUri;
	}

	public void setContactUri(String contactUri) {
		this.contactUri = contactUri;
	}

	public String getMessageText() {
		return messageText;
	}

	public String getMessageTime() {
		return messageTime;
	}

	public void setMessageText(String messageText) {
		this.messageText = messageText;
	}

	public void setMessageTime(String messageTime) {
		this.messageTime = messageTime;
	}

	public String getMessageDirection() {
		return messageDirection;
	}

	public void setMessageDirection(String messageDirection) {
		this.messageDirection = messageDirection;
	}

	public String getMessageId() {
		return messageId;
	}

	public boolean isViewed() {
		return viewed;
	}

	public void setMessageId(String messageId) {
		this.messageId = messageId;
	}

	public void setViewed(boolean viewed) {
		this.viewed = viewed;
	}

	public String getMessageInternalId() {
		return messageInternalId;
	}

	protected void setMessageInternalId(String messageInternalId) {
		this.messageInternalId = messageInternalId;
	}

	public String getStatus() {
		return status;
	}

	public void setStatus(String status) {
		if (this.status==null && status!=null) {
			this.status=status;
		} else if (status!=null) {
			// It is possible notifications are received out of order - therefore if a message is 'viewed' it should stay that way
			if (!MESSAGE_STATUS_VIEWED.equalsIgnoreCase(this.status) && MESSAGE_STATUS_VIEWED.equalsIgnoreCase(status)) {
				this.status=MESSAGE_STATUS_VIEWED;
			} else if (MESSAGE_STATUS_PENDING.equalsIgnoreCase(this.status)) {
				this.status=status;
			}
		}
	}

	public String getResourceURL() {
		return resourceURL;
	}
	
	public void setResourceURL(String resourceURL) {
		this.resourceURL=resourceURL;
	}

	public int describeContents() {
        return 0;
    }
	
    public static final Parcelable.Creator<ChatMessage> CREATOR = new Parcelable.Creator<ChatMessage>() {
        public ChatMessage createFromParcel(Parcel in) {
            return new ChatMessage(in);
        }

        public ChatMessage[] newArray(int size) {
            return new ChatMessage[size];
        }
    };

	public void writeToParcel(Parcel out, int flags) {
        out.writeString(contactUri);
        out.writeString(messageText);
        out.writeString(messageTime);
        out.writeString(messageDirection);
        out.writeString(messageId);
        out.writeString(messageInternalId);
        out.writeString(status);
        out.writeString(resourceURL);
        out.writeInt(viewed?1:0);
    }

	public ChatMessage() {
		messageInternalId=UUID.randomUUID().toString();
	}
	
	public ChatMessage(Parcel in) {
		contactUri = in.readString();
		messageText = in.readString();
		messageTime = in.readString();
		messageDirection = in.readString();
		messageId = in.readString();
		messageInternalId = in.readString();
		status = in.readString();
		resourceURL = in.readString();
		viewed = in.readInt()>0;
    }

}
