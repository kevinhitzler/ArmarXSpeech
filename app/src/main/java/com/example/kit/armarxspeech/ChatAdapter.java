package com.example.kit.armarxspeech;
import java.util.List;
//import android.app.ActionBar.LayoutParams;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Point;
import android.net.Uri;
import android.opengl.Visibility;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.LinearLayout.LayoutParams;
import android.widget.TextView;

public class ChatAdapter extends ArrayAdapter<ChatMessage>
{	  
	private Context context;
	
	public ChatAdapter(Context context, List<ChatMessage> messages, View view)
	{
		super(context,0, messages);		    
	    this.context = context;
	}
	
    @Override
    public View getView(int position, View convertView, ViewGroup parent) 
    {
       // Get the data item for this position
    	ChatMessage chatMessage = getItem(position);
       
       
       // Check if an existing view is being reused, otherwise inflate the view
       if (convertView == null) 
       {
          convertView = LayoutInflater.from(getContext()).inflate(R.layout.chat_listitem_universalmessage, parent, false);
       }
       
       
       // Lookup view for data population
       TextView tvMessage = (TextView) convertView.findViewById(R.id.rowMessage);
       TextView tvPartner = (TextView) convertView.findViewById(R.id.rowPartner);
       TextView tvDateTime = (TextView) convertView.findViewById(R.id.rowDateTime);
       
       LinearLayout ll_messagePlaceHolder = (LinearLayout) convertView.findViewById( R.id.rowMessagePlaceHolder );
       LinearLayout ll_messageContainer = (LinearLayout) convertView.findViewById( R.id.rowMessageContainer );
       View viewPlaceholderLeft = (View) convertView.findViewById(R.id.rowPlaceholderLeft);
       View viewPlaceholderRight = (View) convertView.findViewById(R.id.rowPlaceholderRight);
       
       
       // Populate the data into the template view using the data object
       tvDateTime.setText(chatMessage.getTimestamp());
       
      
       
       if(chatMessage.isMine())
       {
    	   //lpMessage.gravity = Gravity.END;
    	   //lpPartner.gravity = Gravity.END;
    	   //tvMessage.setBackgroundResource(R.drawable.speech_bubble_green);
    	   ll_messageContainer.setBackgroundResource(R.drawable.background_message_own);    	   
    	   viewPlaceholderLeft.setVisibility(View.VISIBLE);
    	   viewPlaceholderRight.setVisibility(View.GONE);    	   
    	   ll_messagePlaceHolder.setGravity(Gravity.END);
    	   
    	   tvPartner.setVisibility(View.GONE); 
       } 
       else
       {
    	   //lpMessage.gravity = Gravity.START;
    	   //lpPartner.gravity = Gravity.START;
    	   ll_messageContainer.setBackgroundResource(R.drawable.background_message_other);
    	   viewPlaceholderLeft.setVisibility(View.GONE);
    	   viewPlaceholderRight.setVisibility(View.VISIBLE);    
    	   ll_messagePlaceHolder.setGravity(Gravity.START);
    	   
    	   tvPartner.setVisibility(View.GONE);    	   
       }

       // set text from message
       tvMessage.setVisibility(View.VISIBLE);
       tvMessage.setText(chatMessage.getMessage());
       
       
       tvMessage.requestLayout();
       tvPartner.requestLayout();
       tvDateTime.requestLayout();

       // Return the completed view to render on screen
       return convertView;
   }
    
    
    

}
