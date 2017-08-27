package com.example.kit.armarxspeech;

/**
 * Created by Kevin on 30.05.2017.
 */

import java.util.Timer;
import java.util.TimerTask;

/**
 * Handles reconnections for a client in order to
 * send and receive messages or audio streams.
 */
public class ReconnectorTask
{
    private Timer _timer;
    private TimerTask _reconnect;
    private ReconnectorTaskInterface _client;
    private boolean _isRunning;

    public ReconnectorTask(ReconnectorTaskInterface rti)
    {
        this._client = rti;
        _isRunning = false;
    }

    public void shutdown()
    {
        try
        {
            _timer.cancel();
            _timer.purge();
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }

        _isRunning = false;
    }

    public void reconnect(long period)
    {
        _isRunning = true;
        _reconnect = new TimerTask()
        {
            @Override
            public void run()
            {
                doReconnect();
            }
        };

        _timer = new Timer();
        _timer.schedule(_reconnect, 0, period);
    }

    public boolean isRunning()
    {
        return _isRunning;
    }

    private void doReconnect()
    {
        if(!_client.isConnected())
        {
            _client.reconnectToServer();
        }
    }


}
