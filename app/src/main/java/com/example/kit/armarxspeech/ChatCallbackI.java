package com.example.kit.armarxspeech;
import android.speech.tts.TextToSpeech;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import Ice.Current;

/**
 * Created by Kevin on 29.05.2017.
 */
public class ChatCallbackI extends armarx._ChatCallbackDisp
{
    private static final long serialVersionUID = 1L;
    private MainActivity _activity;

    ChatCallbackI(MainActivity activity)
    {
        this._activity = activity;
    }

    @Override
    public void send(final long timestamp, final String name, final String message, Current __current)
    {
        _activity.runOnUiThread(new Runnable()
        {
            @Override
            public void run()
            {
                if(name.equals("ArmarX"))
                {
                    TextToSpeech tts = _activity.getTextToSpeach();
                    if(tts != null && !_activity.isMuted)
                    {
                        if(tts.isSpeaking())
                        {
                            tts.stop();
                        }
                        String utteranceId = UUID.randomUUID().toString();

                        try
                        {
                            String messageWithoutEmojis = message.replaceAll("[^\\x00-\\x7F]", "");
                            tts.speak(messageWithoutEmojis, TextToSpeech.QUEUE_FLUSH, null, utteranceId);
                        }
                        catch (Exception e)
                        {
                            e.printStackTrace();
                            System.out.println("Could not remove Emoji!");
                            tts.speak(message, TextToSpeech.QUEUE_FLUSH, null, utteranceId);
                        }
                    }

                }
                _activity.printToChat(timestamp, name, message);
            }
        });
        System.out.println("Received message: "+message);
    }

}

