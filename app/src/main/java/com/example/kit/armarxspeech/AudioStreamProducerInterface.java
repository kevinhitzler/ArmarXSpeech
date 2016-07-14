package com.example.kit.armarxspeech;

import Ice.Current;
import armarx.AudioEncoding;

/**
 * Created by Kevin on 14.07.2016.
 */
public class AudioStreamProducerInterface extends armarx._AudioStreamProducerInterfaceDisp
{
    @Override
    public void publishAudioChunk(byte[] data, AudioEncoding encoding, long timestamp, Current __current)
    {
    }
}
