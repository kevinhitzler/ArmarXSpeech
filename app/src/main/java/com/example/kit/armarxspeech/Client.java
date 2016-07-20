package com.example.kit.armarxspeech;

import android.nfc.Tag;
import android.util.Log;

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
import armarx.AudioEncoding;

public class Client {
    public static void send(String msg) {
        new Thread(new PrinterThread(msg)).run();
    }

    public static void sendFile(byte[] chunk, AudioEncoding encoding, long timestamp) {
        new Thread(new StreamThread(chunk, encoding, timestamp)).run();
    }

    public static void streamFile(String filepath, AudioEncoding encoding, long timestamp) {
        new Thread(new AsyncStreamThread(filepath, encoding, timestamp)).run();
    }
}

class AsyncStreamThread implements Runnable
{
    String filepath;
    AudioEncoding encoding;
    long timestamp;

    public AsyncStreamThread(String filepath, AudioEncoding encoding, long timestamp)
    {
        this.filepath = (String)filepath;
        this.encoding = (AudioEncoding) encoding;
        this.timestamp = (long)timestamp;
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

    public void run()
    {
        Ice.InitializationData initData = null;
        Ice.Communicator ic = null;

        try
        {
            initData = new Ice.InitializationData();
            ic = Ice.Util.initialize(initData);

            Ice.ObjectPrx base = ic.stringToProxy("SimpleStreamer:tcp -h 192.168.0.19 -p 9998");
            armarx.AsyncStreamingInterfacePrx audioStreamPrx = armarx.AsyncStreamingInterfacePrxHelper.checkedCast(base);
            if (audioStreamPrx == null)
                throw new Error("Invalid proxy");
            streamPartialFile(audioStreamPrx);
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

            Ice.ObjectPrx base = ic.stringToProxy("SimpleStreamer:tcp -h 192.168.1.163 -p 9998");
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
}

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

            Ice.ObjectPrx base = ic.stringToProxy("SimplePrinter:tcp -h 192.168.1.163 -p 9999");
            //Demo.PrinterPrx printer = Demo.PrinterPrxHelper.checkedCast(base);
            armarx.TextListenerInterfacePrx textListener = armarx.TextListenerInterfacePrxHelper.checkedCast(base);
            if (textListener == null)
                throw new Error("Invalid proxy");

            //printer.printString(this.msg);
            textListener.reportText(this.msg);

        }
        catch(Ice.LocalException e)
        {
            Log.e("ArmarXSpeech", e.getMessage());
        }
        catch (Exception e)
        {
            Log.e("ArmarXSpeech", e.getMessage());
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

