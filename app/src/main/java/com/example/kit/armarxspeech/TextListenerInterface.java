package com.example.kit.armarxspeech;

import Ice.Current;

/**
 * Created by Kevin on 04.07.2016.
 */
public class TextListenerInterface extends armarx._TextListenerInterfaceDisp
{
    @Override
    public void reportText(String text, Current __current)
    {
        System.out.println(text);
    }

    @Override
    public void reportTextWithParams(String text, String[] params, Current __current)
    {
        System.out.println(text);
    }
}
