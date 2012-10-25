package org.gsm.rcsApp.adapters;

import java.util.ArrayList;

import org.gsm.rcsApp.RCS.ChatMessage;

import android.app.Activity;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.gsm.RCSDemo.R;

public class MessageRowAdapter extends BaseAdapter {
	 
    private Activity activity;
    private ArrayList<ChatMessage> messageData;
    private static LayoutInflater inflater=null;
 
    public MessageRowAdapter(Activity a, ArrayList<ChatMessage> retrievedMessages) {
        activity = a;
        messageData=retrievedMessages;
        inflater = (LayoutInflater)activity.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    }
 
    public int getCount() {
        return messageData.size();
    }
 
    public Object getItem(int position) {
        return position;
    }
 
    public long getItemId(int position) {
        return position;
    }
 
    public View getView(int position, View convertView, ViewGroup parent) {
        View vi=convertView;
        
        if (convertView==null) vi = inflater.inflate(R.layout.message_row, null);
        
        View messageSpacerL=(View) vi.findViewById(R.id.messageSpacerL);
        View messageSpacerR=(View) vi.findViewById(R.id.messageSpacerR);

        LinearLayout messageWrapperL=(LinearLayout) vi.findViewById(R.id.messageWrapperL);
        LinearLayout messageWrapperR=(LinearLayout) vi.findViewById(R.id.messageWrapperR);

        TextView messageTextL=(TextView) messageWrapperL.findViewById(R.id.messageTextL);
        TextView messageTextR=(TextView) messageWrapperR.findViewById(R.id.messageTextR);
        
        TextView messageTimeL=(TextView) vi.findViewById(R.id.messageTimeL);
        TextView messageTimeR=(TextView) vi.findViewById(R.id.messageTimeR);
        
        String messageText=messageData.get(position).getMessageText();
        String messageTime=messageData.get(position).getMessageTime();
        String status=messageData.get(position).getStatus();
        
        String messageDirection=messageData.get(position).getMessageDirection();

        if (ChatMessage.MESSAGE_RECEIVED.equalsIgnoreCase(messageDirection)) {
        	messageTextL.setText(messageText);
        	messageTimeL.setText(messageTime);

        	messageSpacerL.setVisibility(View.INVISIBLE);
        	messageWrapperR.setVisibility(View.INVISIBLE);
        	messageTextR.setVisibility(View.INVISIBLE);   
        	messageTimeR.setVisibility(View.INVISIBLE);

        	messageSpacerR.setVisibility(View.VISIBLE);
        	messageWrapperL.setVisibility(View.VISIBLE);
        	messageTextL.setVisibility(View.VISIBLE);       
        	messageTimeL.setVisibility(View.VISIBLE);
        } else {
        	messageTextR.setText(messageText);
        	messageTimeR.setText(messageTime);

        	messageSpacerR.setVisibility(View.INVISIBLE);
        	messageWrapperL.setVisibility(View.INVISIBLE);
        	messageTextL.setVisibility(View.INVISIBLE);
        	messageTimeL.setVisibility(View.INVISIBLE);

        	messageSpacerL.setVisibility(View.VISIBLE);
        	messageWrapperR.setVisibility(View.VISIBLE);
        	messageTextR.setVisibility(View.VISIBLE);
        	messageTimeR.setVisibility(View.VISIBLE);
        	
        	if (ChatMessage.MESSAGE_STATUS_PENDING.equalsIgnoreCase(status)) {
        		messageWrapperR.setBackgroundResource(R.drawable.roundedbox_tx);
        	} else if (ChatMessage.MESSAGE_STATUS_DELIVERED.equalsIgnoreCase(status)) {
        		messageWrapperR.setBackgroundResource(R.drawable.roundedbox_tx_delivered);
        	} else if (ChatMessage.MESSAGE_STATUS_VIEWED.equalsIgnoreCase(status)) {
        		messageWrapperR.setBackgroundResource(R.drawable.roundedbox_tx_viewed);
        	} else if (ChatMessage.MESSAGE_STATUS_SENT.equalsIgnoreCase(status)) {
        		messageWrapperR.setBackgroundResource(R.drawable.roundedbox_tx_sent);
        	} else {
        		messageWrapperR.setBackgroundResource(R.drawable.roundedbox_tx);
        	}
        }

        return vi;
    }
}