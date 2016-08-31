package com.example.kit.armarxspeech;

import android.content.Context;
import android.nfc.Tag;
import android.os.AsyncTask;
import android.util.Log;
import android.widget.Toast;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.LinkedList;
import java.util.logging.FileHandler;

import Ice.AsyncResult;
import armarx.AsyncStreamingInterfacePrx;
import armarx.AudioEncoding;

import armarx.TextListenerInterfacePrx;
import armarx.TextListenerInterfacePrxHelper;
import demo.MonitorPrx;
import demo.MonitorPrxHelper;

public class Client
{
    private static final String TAG = "Client";
    public static String IP_ADDRESS_SERVER = "192.168.1.163";
    public static String PORT_PRINTER_SERVER = "10001";
    public static String PORT_AUDIO_SERVER = "10001";


    /*public static void send(String msg) {
        new Thread(new PrinterThread(msg)).run();
    }*/

    public static void send(String msg) {
        new Thread(new TextListenerThread(msg)).run();
    }

   /* public static void sendFile(byte[] chunk, AudioEncoding encoding, long timestamp) {
        new Thread(new StreamThread(chunk, encoding, timestamp)).run();
    }

    public static void streamData(int offset, byte[] data, AudioEncoding encoding, long timestamp) {
        new Thread(new StreamingThread(offset, data, encoding, timestamp)).run();
    }

    */

    public static void streamFile(Context applicationContext, String filepath, AudioEncoding encoding, long timestamp) {
        new AsyncStreamThread(applicationContext, filepath, encoding, timestamp).execute();
    }


}

/*
class StreamingThread implements Runnable
{
    int offset;
    byte[] data;
    AudioEncoding encoding;
    long timestamp;

    public StreamingThread(int offset, byte[] data, AudioEncoding encoding, long timestamp)
    {
        this.offset = offset;
        this.data = (byte[])data;
        this.encoding = (AudioEncoding) encoding;
        this.timestamp = (long)timestamp;
    }

    public void run()
    {
        Ice.InitializationData initData = null;
        Ice.Communicator ic = null;

        try
        {
            initData = new Ice.InitializationData();
            ic = Ice.Util.initialize(initData);

            Ice.ObjectPrx base = ic.stringToProxy("SimpleStreamer:tcp -h "+Client.IP_ADDRESS_SERVER+" -p "+ Client.PORT_AUDIO_SERVER);
            armarx.AsyncStreamingInterfacePrx audioStreamPrx = armarx.AsyncStreamingInterfacePrxHelper.checkedCast(base);
            if (audioStreamPrx == null)
                throw new Error("Invalid proxy");
            audioStreamPrx.sendChunkAsync(offset, data, AudioEncoding.PCM, timestamp);
        }
        catch(Ice.LocalException e)
        {
            Log.e("ArmarXSpeech", e.toString());
        }
        catch (Exception e)
        {
            Log.e("ArmarXSpeech", e.toString());
        }

        if (ic != null)
        {
            // Clean up
            try
            {
                ic.destroy();
            }
            catch (Exception e)
            {
                Log.e("ArmarXSpeech", e.toString());
            }
        }
    }
}*/


class AsyncStreamThread extends AsyncTask<Void, Void, Exception> {
    String filepath;
    AudioEncoding encoding;
    long timestamp;
    Context applicationContext;

    public AsyncStreamThread(Context applicationContext, String filepath, AudioEncoding encoding, long timestamp)
    {
        this.filepath = (String)filepath;
        this.encoding = (AudioEncoding) encoding;
        this.timestamp = (long)timestamp;
        this.applicationContext = (Context) applicationContext;
    }

    private byte[] streamPartialFile(armarx.AsyncStreamingInterfacePrx stream)
    {
        try
        {
            File file = new File(filepath);
            FileInputStream fis = new FileInputStream(file);

            final int chunkSize = 1024*512;
            byte[] byteSeq = new byte[chunkSize];
            int offset = 0;

            LinkedList<Ice.AsyncResult> results = new LinkedList<AsyncResult>();
            int numRequests = 10;

            while(offset > -1)
            {
                Log.d("Client", "Offset: "+offset);
                offset = fis.read(byteSeq);

                // Send up to numRequests + 1 chunks asynchronously.
                Ice.AsyncResult r = stream.begin_sendChunkAsync(offset, byteSeq, AudioEncoding.PCM, System.currentTimeMillis());

                // Wait until this request has been passed to the transport.
                r.waitForSent();
                results.add(r);

                // Once there are more than numRequests, wait for the least
                // recent one to complete.
                while (results.size() > numRequests) {
                    Ice.AsyncResult re = results.getFirst();
                    results.removeFirst();
                    re.waitForCompleted();
                }
            }

            // Wait for any remaining requests to complete.
            while (results.size() > 0) {
                Ice.AsyncResult res = results.getFirst();
                results.removeFirst();
                res.waitForCompleted();
            }

            fis.close();
        }
        catch (FileNotFoundException e)
        {
            Log.d("ClientFNF", e.getMessage());
        }
        catch (IOException e)
        {
            Log.d("ClientIO", e.getMessage());
        }

        return null;
    }

    @Override
    protected Exception doInBackground(Void... params)
    {
        Ice.InitializationData initData = null;
        Ice.Communicator ic = null;

        try
        {
            initData = new Ice.InitializationData();
            ic = Ice.Util.initialize(initData);

            Ice.ObjectPrx obj = ic.stringToProxy("IceStorm/TopicManager:tcp -h "+Client.IP_ADDRESS_SERVER+" -p "+ Client.PORT_AUDIO_SERVER);
            IceStorm.TopicManagerPrx topicManager = IceStorm.TopicManagerPrxHelper.checkedCast(obj.ice_timeout(1000));

            if(topicManager == null)
            {
                Log.e("AsyncStreamThread", "invalid proxy");
                return new Exception("Invalid Proxy");
            }

            IceStorm.TopicPrx topic = null;

            while (topic == null) {
                try {
                    topic = topicManager.retrieve("ArmarXSpeech");
                } catch (IceStorm.NoSuchTopic ex) {
                    try {
                        String err = (ex.getMessage()==null)?ex.toString():ex.getMessage();
                        Log.e("NoSuchTopic:",err);
                        topic = topicManager.create("ArmarXSpeech");
                    } catch (IceStorm.TopicExists e) {
                        String err = (e.getMessage()==null)?e.toString():e.getMessage();
                        Log.e("TopicExists:",err);
                    }
                }
            }



            Ice.ObjectPrx pub = topic.getPublisher().ice_oneway();
            AsyncStreamingInterfacePrx audioStreamPrx = armarx.AsyncStreamingInterfacePrxHelper.uncheckedCast(pub);
            if (audioStreamPrx == null)
                throw new Error("Invalid proxy");

            streamPartialFile(audioStreamPrx);
        }
        catch(Ice.TimeoutException te)
        {
            return te;
        }
        catch(Ice.LocalException e)
        {
            Log.e("ArmarXSpeech", e.toString());
        }
        catch (Exception e)
        {
            Log.e("ArmarXSpeech", e.toString());
        }

        if (ic != null)
        {
            // Clean up
            try
            {
                ic.destroy();
            }
            catch (Exception e)
            {
                Log.e("ArmarXSpeech", e.toString());
            }
        }

        return null;
    }

    @Override
    protected void onPostExecute(final Exception result)
    {
        if (result != null)
        {
            if(result instanceof Ice.TimeoutException)
            {
                Toast.makeText(applicationContext, "Error: Server connection timed out. Please try again.",
                        Toast.LENGTH_LONG).show();
            }
            else
            {
                Toast.makeText(applicationContext, "Error: "+ result.getMessage(),
                        Toast.LENGTH_LONG).show();
            }
        }
    }
}
/*
class StreamThread implements Runnable
{
    byte[] data;
    AudioEncoding encoding;
    long timestamp;

    public StreamThread(byte[] chunk, AudioEncoding encoding, long timestamp)
    {
        this.data = (byte[])chunk;
        this.encoding = (AudioEncoding) encoding;
        this.timestamp = (long)timestamp;
    }

    public void run()
    {
        Ice.InitializationData initData = null;
        Ice.Communicator ic = null;

        try
        {
            initData = new Ice.InitializationData();
            ic = Ice.Util.initialize(initData);

            Ice.ObjectPrx base = ic.stringToProxy("SimpleStreamer:tcp -h "+Client.IP_ADDRESS_SERVER+" -p "+ Client.PORT_AUDIO_SERVER);;
            armarx.AsyncStreamingInterfacePrx audioStreamPrx = armarx.AsyncStreamingInterfacePrxHelper.checkedCast(base);
            if (audioStreamPrx == null)
                throw new Error("Invalid proxy");
            audioStreamPrx.sendChunkAsync(0, data, encoding,timestamp);
        }
        catch(Ice.LocalException e)
        {
            Log.e("ArmarXSpeech", e.toString());
        }
        catch (Exception e)
        {
            Log.e("ArmarXSpeech", e.toString());
        }

        if (ic != null)
        {
            // Clean up
            try
            {
                ic.destroy();
            }
            catch (Exception e)
            {
                Log.e("ArmarXSpeech", e.toString());
            }
        }
    }
}*/


class TextListenerThread implements Runnable
{
    String msg;

    public TextListenerThread(Object parameter)
    {
        this.msg = (String)parameter;
    }

    public void run()
    {
        int status = 0;
        Ice.Communicator communicator = null;
        Ice.InitializationData initData = null;

        try
        {
            initData = new Ice.InitializationData();
            communicator = Ice.Util.initialize(initData);

            Ice.ObjectPrx obj = communicator.stringToProxy("IceStorm/TopicManager:tcp -h "+Client.IP_ADDRESS_SERVER+" -p "+ Client.PORT_PRINTER_SERVER);
            IceStorm.TopicManagerPrx topicManager = IceStorm.TopicManagerPrxHelper.checkedCast(obj);
            IceStorm.TopicPrx topic = null;

            while (topic == null) {
                try {
                    topic = topicManager.retrieve("ArmarXSpeech");
                } catch (IceStorm.NoSuchTopic ex) {
                    try {
                        String err = (ex.getMessage()==null)?ex.toString():ex.getMessage();
                        Log.e("NoSuchTopic:",err);
                        topic = topicManager.create("ArmarXSpeech");
                    } catch (IceStorm.TopicExists e) {
                        String err = (e.getMessage()==null)?e.toString():e.getMessage();
                        Log.e("TopicExists:",err);
                    }
                }
            }

            Ice.ObjectPrx pub = topic.getPublisher().ice_oneway();
            MonitorPrx monitor = MonitorPrxHelper.uncheckedCast(pub);
            monitor.report(msg);

        }
        catch (Ice.LocalException e) {
            String err = (e.getMessage()==null)?e.toString():e.getMessage();
            Log.e("LocalException",err);
            status = 1;
        } catch (Exception e) {
            String err = (e.getMessage()==null)?e.toString():e.getMessage();
            Log.e("Exception:",err);
            status = 1;
        }
        if (communicator != null) {
            // Clean up
            //
            try {
                communicator.destroy();
            } catch (Exception e) {
                String err = (e.getMessage()==null)?e.toString():e.getMessage();
                Log.e("Exception clean up:",err);
                status = 1;
            }
        }
    }
}

/*
class PrinterThread implements Runnable
{
    String msg;

    public PrinterThread(Object parameter)
    {
        this.msg = (String)parameter;
    }

    public void run()
    {
        Ice.InitializationData initData = null;
        Ice.Communicator ic = null;

        try
        {
            initData = new Ice.InitializationData();
            ic = Ice.Util.initialize(initData);

            Ice.ObjectPrx base = ic.stringToProxy("SimplePrinter:tcp -h "+Client.IP_ADDRESS_SERVER+" -p "+ Client.PORT_PRINTER_SERVER);
            //Demo.PrinterPrx printer = Demo.PrinterPrxHelper.checkedCast(base);
            armarx.TextListenerInterfacePrx textListener = armarx.TextListenerInterfacePrxHelper.checkedCast(base);
            if (textListener == null)
                throw new Error("Invalid proxy");

            //printer.printString(this.msg);
            textListener.reportText(this.msg);

        }
        catch(Ice.LocalException e)
        {
            Log.e("ArmarXSpeech", e.toString());
        }
        catch (Exception e)
        {
            Log.e("ArmarXSpeech", e.toString());
        }

        if (ic != null)
        {
            // Clean up
            try
            {
                ic.destroy();
            }
            catch (Exception e)
            {
                Log.e("ArmarXSpeech", e.toString());
            }
        }
    }
}
*/

