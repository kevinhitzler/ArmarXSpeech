package com.example.kit.armarxspeech;

import android.util.Log;

import Ice.Current;

/**
 * Created by Kevin on 04.07.2016.
 */
public class TextListenerInterface extends armarx._TextListenerInterfaceDisp
{
    @Override
    public void reportText(String text, Current __current)
    {
        Log.w("ArmarXSpeech", "Here it is!");
    }

    @Override
    public void reportTextWithParams(String text, String[] params, Current __current)
    {
    }
}
