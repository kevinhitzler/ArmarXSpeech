package com.example.kit.armarxspeech;

import armarx.AudioEncoding;

/**
 * Created by Kevin on 30.05.2017.
 */

public class AudioData
{
    private String _filepath;
    private AudioEncoding _encoding;
    private int _minBufferSize;

    public AudioData(String filepath, AudioEncoding encoding, int minBufferSize)
    {
        this._encoding = encoding;
        this._filepath = filepath;
        this._minBufferSize = minBufferSize;
    }

    public String getFilePath()
    {
        return _filepath;
    }

    public int getMinBufferSize()
    {
        return _minBufferSize;
    }

    public AudioEncoding getEncoding()
    {
        return _encoding;
    }

    public void setAudioEncoding(AudioEncoding encoding)
    {
        this._encoding = encoding;
    }

    public void setMinBufferSize(int minBufferSize)
    {
        this._minBufferSize = minBufferSize;
    }

    public void setFilepath(String filepath)
    {
        this._filepath = filepath;
    }
}
