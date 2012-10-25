package org.gsm.rcsApp.RCS;

import android.os.Parcel;
import android.os.Parcelable;

public class Contact implements Parcelable {
	
	public static final String CONTACT_ONLINE = "open";
	public static final String CONTACT_OFFLINE = "closed";

	String contactId=null;
	String displayName=null;
	String icon=null;
	String status=null;
	String capabilities=null;
	boolean hasNewMessage=false;
	String resourceURL=null;
	
	public String getContactId() {
		return contactId;
	}
	public String getDisplayName() {
		return displayName;
	}
	public String getIcon() {
		return icon;
	}
	public String getStatus() {
		return status;
	}
	public String getCapabilities() {
		return capabilities;
	}
	public String getResourceURL() {
		return resourceURL;
	}
	
	public void setContactId(String contactId) {
		this.contactId = contactId;
	}
	public void setDisplayName(String displayName) {
		this.displayName = displayName;
	}
	public void setIcon(String icon) {
		this.icon = icon;
	}
	public void setStatus(String status) {
		this.status = status;
	}
	public void setCapabilities(String capabilities) {
		this.capabilities = capabilities;
	}
	public boolean isHasNewMessage() {
		return hasNewMessage;
	}
	public void setHasNewMessage(boolean hasNewMessage) {
		this.hasNewMessage = hasNewMessage;
	}
	public void setResourceURL(String resourceURL) {
		this.resourceURL=resourceURL;
	}

	public int describeContents() {
        return 0;
    }
	
    public static final Parcelable.Creator<Contact> CREATOR = new Parcelable.Creator<Contact>() {
        public Contact createFromParcel(Parcel in) {
            return new Contact(in);
        }

        public Contact[] newArray(int size) {
            return new Contact[size];
        }
    };

	public void writeToParcel(Parcel out, int flags) {
        out.writeString(contactId);
        out.writeString(displayName);
        out.writeString(icon);
        out.writeString(status);
        out.writeString(resourceURL);
        out.writeString(capabilities);
        out.writeInt(hasNewMessage?1:0);
    }

	public Contact() {
		
	}
	
	public Contact(Parcel in) {
        contactId = in.readString();
        displayName = in.readString();
        icon = in.readString();
        status = in.readString();
        resourceURL = in.readString();
        capabilities = in.readString();
        hasNewMessage=(in.readInt()>0);
    }
}
