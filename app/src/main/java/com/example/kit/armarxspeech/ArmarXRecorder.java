package com.example.kit.armarxspeech;

import android.app.Activity;
import android.widget.LinearLayout;
import android.os.Bundle;
import android.os.Environment;
import android.view.ViewGroup;
import android.widget.Button;
import android.view.View;
import android.view.View.OnClickListener;
import android.content.Context;
import android.util.Log;
import android.media.MediaRecorder;
import android.media.MediaPlayer;

import java.io.File;
import java.io.IOException;

import armarx.AudioEncoding;


/**
 * Created by Kevin on 14.07.2016.
 */
public class ArmarXRecorder
{
    private static final String LOG_TAG = "AudioRecordTest";
    private static String mFileName = null;
    private MediaRecorder mRecorder = null;
    private MediaPlayer   mPlayer = null;

    public void startPlaying()
    {
        mPlayer = new MediaPlayer();
        try
        {
            mPlayer.setDataSource(mFileName);
            mPlayer.prepare();
            mPlayer.start();
        }
        catch (IOException e)
        {
            Log.e(LOG_TAG, "prepare() failed");
        }
    }

    public void stopPlaying()
    {
        if(mPlayer != null)
        {
            mPlayer.release();
            mPlayer = null;
        }
    }

    public void startRecording()
    {
        mRecorder = new MediaRecorder();
        mRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
        mRecorder.setOutputFormat(MediaRecorder.OutputFormat.DEFAULT);
        mRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.DEFAULT);
        mRecorder.setOutputFile(mFileName);

        try
        {
            mRecorder.prepare();
        }
        catch (IOException e)
        {
            Log.e(LOG_TAG, "prepare() failed");
        }

        mRecorder.start();
    }

    public void stopRecording()
    {
        if(mRecorder != null)
        {
            mRecorder.stop();
            mRecorder.release();
            mRecorder = null;
        }
    }

    public ArmarXRecorder()
    {
        String path = Environment.getExternalStorageDirectory().getAbsolutePath()
                + "/Android/data/com.example.kit.armarxspeech/files/";
        File dir = new File(path);
        if(!dir.exists())
        {
            dir.mkdirs();
        }
        mFileName = path + "testrecord" + ".3gp";
    }

    public void onPause()
    {
        if (mRecorder != null) {
            mRecorder.release();
            mRecorder = null;
        }

        if (mPlayer != null) {
            mPlayer.release();
            mPlayer = null;
        }
    }

    public String getFileName()
    {
        return mFileName;
    }
}
