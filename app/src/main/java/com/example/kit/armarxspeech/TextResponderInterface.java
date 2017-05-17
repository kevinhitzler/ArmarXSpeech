package com.example.kit.armarxspeech;

/**
 * Created by Kevin on 16.05.2017.
 */

import android.util.Log;
import Ice.Current;


public class TextResponderInterface extends armarx._TextResponderInterfaceDisp
{
    public void sendText(String text, Current c)
    {
        Log.w("TextResponder", text);
    }

}
