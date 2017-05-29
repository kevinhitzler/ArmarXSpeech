package com.example.kit.armarxspeech;
import Ice.Current;

/**
 * Created by Kevin on 29.05.2017.
 */
public class ChatCallbackI extends armarx._ChatCallbackDisp
{
    private static final long serialVersionUID = 1L;

    @Override
    public void send(long timestamp, String name, String message, Current __current)
    {
        System.out.println("Received message: "+message);
    }

}

