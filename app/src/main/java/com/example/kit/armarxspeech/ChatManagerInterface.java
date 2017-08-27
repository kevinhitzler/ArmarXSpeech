package com.example.kit.armarxspeech;

/**
 * Created by Kevin on 30.05.2017.
 */

public interface ChatManagerInterface
{
    public void sendText(String msg);
    public void streamAudio(AudioData data);
}
