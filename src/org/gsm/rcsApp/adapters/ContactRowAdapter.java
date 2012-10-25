package org.gsm.rcsApp.adapters;

import java.util.ArrayList;

import org.gsm.RCSDemo.R;
import org.gsm.rcsApp.RCS.Contact;

import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.fedorvlasov.lazylist.ImageLoader;

public class ContactRowAdapter extends BaseAdapter {
	 
    private Activity activity;
    private ArrayList<Contact> data;
    private static LayoutInflater inflater=null;
    public ImageLoader imageLoader; 
 
    public ContactRowAdapter(Activity a, ArrayList<Contact> retrievedContacts) {
        activity = a;
        data=retrievedContacts;
        inflater = (LayoutInflater)activity.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        imageLoader=new ImageLoader(activity.getApplicationContext());
    }
 
    public int getCount() {
        return data.size();
    }
 
    public Object getItem(int position) {
        return position;
    }
 
    public long getItemId(int position) {
        return position;
    }
    
    public View getView(int position, View convertView, ViewGroup parent) {
        View vi=convertView;
        
        if (convertView==null) vi = inflater.inflate(R.layout.contact_row, null);
 
        View thumbnail_wrapper=vi.findViewById(R.id.thumbnail_wrapper);
        TextView contactNameView = (TextView)vi.findViewById(R.id.contactName); 
        TextView contactStatusView = (TextView)vi.findViewById(R.id.contactStatus); 
        TextView contactInfoView = (TextView)vi.findViewById(R.id.contactInfo);
        TextView newMessageIndicator = (TextView)vi.findViewById(R.id.newMessageIndicator);
        ImageView thumb_image=(ImageView)thumbnail_wrapper.findViewById(R.id.thumbnail_image); // thumb image

        Contact contact=data.get(position);
        
        String displayName=contact.getDisplayName();
        String contactInfo=contact.getContactId();
        String capabilities=contact.getCapabilities();
        String icon=contact.getIcon();
 
        contactNameView.setText(displayName);
        contactInfoView.setText(contactInfo);
        if (capabilities!=null) {
        	if (capabilities.indexOf("IM_SESSION")>-1) {
            	contactStatusView.setText("online");
            	contactStatusView.setTextColor(Color.GREEN);
        	} else {
            	contactStatusView.setText("offline");
            	contactStatusView.setTextColor(Color.RED);
        	}
        } else {
        	contactStatusView.setText("unknown");
        	contactStatusView.setTextColor(Color.BLACK);
        }
        
        if (contact.isHasNewMessage()) {
        	newMessageIndicator.setVisibility(View.VISIBLE);
        } else {
        	newMessageIndicator.setVisibility(View.INVISIBLE);
        }
        
        if (icon!=null && (icon.startsWith("http://") || icon.startsWith("https://"))) {
        	imageLoader.DisplayImage(icon, thumb_image);
        }
        
        return vi;
    }
}