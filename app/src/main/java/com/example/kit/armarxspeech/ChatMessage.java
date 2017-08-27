package com.example.kit.armarxspeech;

import org.json.JSONObject;

import android.util.Log;

public class ChatMessage
{		
	//JSON Attribute Identifier
	public static final String MESSAGE_ID = "messageID";
	public static final String MESSAGE_TYPE = "messageType";
	public static final String MESSAGE = "message";
	public static final String PARTNER_ID = "receiverID";					
	public static final String OWN_DEVICE_ID = "senderID";
	public static final String TIMESTAMP = "timestamp";
	public static final String IS_MINE = "isMine";
	public static final String IS_SEND = "isSend";
	
	//Message Types
	public static final int MESSAGE_TYPE_NORMAL_CHATMESSAGE = 0;
	public static final int MESSAGE_TYPE_ACKNOWLEDGEMENT = 1;
	public static final int MESSAGE_TYPE_HEARTBEAT = 69;
	
	
	protected int messageType = MESSAGE_TYPE_NORMAL_CHATMESSAGE;	
	protected String messageID = "messageID is not set";
	protected String message = "message is not set";
	protected String partnerID = "partner is not set";						// partner to or from whom the message goes or came
	protected String ownDeviceID = "myImeiIsNotSet";
	protected String timestamp = "time is not set";							// time at which message was send
	protected boolean isMine = true;
	protected boolean isSend = true;
	

	public ChatMessage()
	{
		//empty object that can be filled via setter-Methods()
	}

	public ChatMessage(int messageType, String messageID, String message, String partnerID, String ownDeviceID, String timestamp, boolean isMine)
	{
		this.messageType = messageType;
		this.messageID = messageID;
		this.message = message;
		this.partnerID = partnerID;	
		this.ownDeviceID = ownDeviceID;
		this.timestamp = timestamp;
		this.isMine = isMine;
	}

	public String getMessage() {
		return message;
	}
	public void setMessage(String message) {
		this.message = message;
	}
	public String getPartnerID() {
		return partnerID;
	}
	public void setPartnerID(String source) {
		this.partnerID = source;
	}
	public boolean isMine() {
		return isMine;
	}
	public void setMine(boolean isMine) {
		this.isMine = isMine;
	}
	public void setMine(int isMine)
	{
		if(isMine != 0)
		{
			this.isMine = true;
		}
		else
		{
			this.isMine =false;
		}
	}
	public boolean isSend() {
		return isSend;
	}
	public void setSend(boolean isSend) {
		this.isSend = isSend;
	}
	public void setSend(int isSend) {
		if(isSend != 0){
			this.isSend = true;
		}
		else{
			this.isSend =false;
		}
	}
	public String getTimestamp() {
		return timestamp;
	}
	public void setTimestamp(String sendTime) {
		this.timestamp = sendTime;
	}
	
	public String getOwnDeviceID() {
		return ownDeviceID;
	}

	public void setOwnDeviceID(String myImei) {
		this.ownDeviceID = myImei;
	}
	
	public int getMessageType()
	{
		return messageType;
	}
	public void setMessageType(int messageType)
	{
		this.messageType = messageType;
	}
	public String getMessageID()
	{
		return messageID;
	}
	public void setMessageID(String messageID)
	{
		this.messageID = messageID;
	}

}
