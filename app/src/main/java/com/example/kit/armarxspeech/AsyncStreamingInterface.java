package com.example.kit.armarxspeech;

import Ice.Current;
import armarx.AudioEncoding;

/**
 * Created by Kevin on 15.07.2016.
 */
public class AsyncStreamingInterface extends armarx._AsyncStreamingInterfaceDisp
{
    @Override
    public void sendChunkAsync(int offset, byte[] data, AudioEncoding encoding, long timestamp, Current __current)
    {

    }
}
