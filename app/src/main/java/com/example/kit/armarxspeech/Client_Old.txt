package com.example.kit.armarxspeech;

import android.content.Context;
import android.nfc.Tag;
import android.os.AsyncTask;
import android.util.Log;
import android.widget.Toast;

import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.Collections;
import java.util.EnumSet;
import java.util.LinkedList;
import java.util.List;
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
    public static String IP_ADDRESS_SERVER = "192.168.1.168"; //"192.168.0.19";
    public static String PORT_SERVER = "80";


    public static void send(String msg) {
        new Thread(new TextListenerThread(msg)).run();
    }

    public static void streamFile(Context applicationContext, String filepath, AudioEncoding encoding, long timestamp, int minBufferSize) {
        new AsyncStreamThread(applicationContext, filepath, encoding, timestamp, minBufferSize).execute();
    }

    /**
     * Get IP address from first non-localhost interface
     * @param useIPv4  true=return ipv4, false=return ipv6
     * @return  address or empty string
     */
    public static String getIPAddress(boolean useIPv4) {
        try {
            List<NetworkInterface> interfaces = Collections.list(NetworkInterface.getNetworkInterfaces());
            for (NetworkInterface intf : interfaces) {
                List<InetAddress> addrs = Collections.list(intf.getInetAddresses());
                for (InetAddress addr : addrs) {
                    if (!addr.isLoopbackAddress()) {
                        String sAddr = addr.getHostAddress();
                        //boolean isIPv4 = InetAddressUtils.isIPv4Address(sAddr);
                        boolean isIPv4 = sAddr.indexOf(':')<0;

                        if (useIPv4) {
                            if (isIPv4)
                                return sAddr;
                        } else {
                            if (!isIPv4) {
                                int delim = sAddr.indexOf('%'); // drop ip6 zone suffix
                                return delim<0 ? sAddr.toUpperCase() : sAddr.substring(0, delim).toUpperCase();
                            }
                        }
                    }
                }
            }
        } catch (Exception ex) { } // for now eat exceptions
        return "";
    }
}

class AsyncStreamThread extends AsyncTask<Void, Void, Exception> {

    String filepath;
    AudioEncoding encoding;
    long timestamp;
    Context applicationContext;
    int minBufferSize;
    Glacier2.SessionHelper _session;

    public AsyncStreamThread(Context applicationContext, String filepath, AudioEncoding encoding, long timestamp, int minBufferSize)
    {
        this.filepath = (String)filepath;
        this.encoding = (AudioEncoding) encoding;
        this.timestamp = (long)timestamp;
        this.applicationContext = (Context) applicationContext;
        this.minBufferSize = minBufferSize;
    }

    private byte[] streamPartialFile(armarx.AsyncStreamingInterfacePrx stream)
    {
        File file = null;
        FileInputStream fis = null;
        try
        {
            boolean isNewSentence = true;
            file = new File(filepath);
            fis = new FileInputStream(file);

            final int chunkSize = 1024*10;
            byte[] byteSeq; /* = new byte[chunkSize];*/
            int offset = 0;

            LinkedList<Ice.AsyncResult> results = new LinkedList<AsyncResult>();
            int numRequests = 10;

            while(offset > -1)
            {
                Log.d("Client", "Offset: "+offset);
                byteSeq = new byte[chunkSize];
                offset = fis.read(byteSeq);

                // Send up to numRequests + 1 chunks asynchronously.
                Ice.AsyncResult r = stream.begin_sendChunkAsync(offset, byteSeq, minBufferSize, AudioEncoding.PCM, System.currentTimeMillis(), isNewSentence, Client.getIPAddress(true));
                isNewSentence = false;


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
            /*
            while (results.size() > 0) {
                Ice.AsyncResult res = results.getFirst();
                results.removeFirst();
                res.waitForCompleted();
            }*/

            fis.close();
            WaveRecorder.deleteTempFile();
        }
        catch (FileNotFoundException e)
        {
            Log.d("ClientFNF", e.getMessage());
        }
        catch (IOException e)
        {
            Log.d("ClientIO", e.getMessage());
        }
        finally {
            try {
                fis.close();
            } catch (IOException ex) {
                ex.printStackTrace();
            }
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
            /*
            Ice.StringSeqHolder argsH = new Ice.StringSeqHolder(null);
            Ice.Properties properties = Ice.Util.createProperties(argsH);
            properties.setProperty("Ice.Default.Router", "Glacier2/router:tcp -h "+Client.IP_ADDRESS_SERVER+" -p "+ Client.PORT_SERVER);
            properties.setProperty("Ice.RetryIntervals","-1");

            initData = new Ice.InitializationData();
            initData.properties = properties;

            Glacier2.SessionFactoryHelper factory = new Glacier2.SessionFactoryHelper(initData, new Glacier2.SessionCallback()
            {
                public void
                connected(final Glacier2.SessionHelper session)
                        throws Glacier2.SessionNotExistException {
                    //
                    // If the session has been reassigned avoid the spurious callback.
                    //
                    if (session != _session) {
                        return;
                    }

                    Ice.Communicator communicator = _session


                    Chat.ChatRoomCallbackPrx callback = Chat.ChatRoomCallbackPrxHelper.uncheckedCast(_session.addWithUUID(new ChatCallbackI()));

                    _chat = Chat.ChatSessionPrxHelper.uncheckedCast(_session.session());
                    try {
                        _chat.begin_setCallback(callback, new Chat.Callback_ChatSession_setCallback() {
                            @Override
                            public void
                            response() {
                                _service.loginComplete();
                            }

                            @Override
                            public void
                            exception(Ice.LocalException ex) {
                                AppSession.this.destroy();
                            }
                        });
                    } catch (Ice.CommunicatorDestroyedException ex) {
                        //Ignore client session was destroyed.
                    }
                }

                public void
                disconnected(Glacier2.SessionHelper session)
                {
                    if(!_destroyed) // Connection closed by user logout/exit
                    {
                        destroyWithError("<system-message> - The connection with the server was unexpectedly lost.\nTry again.");
                    }
                }

                public void
                connectFailed(Glacier2.SessionHelper session, Throwable exception)
                {
                    try
                    {
                        throw exception;
                    }
                    catch(final Glacier2.CannotCreateSessionException ex)
                    {
                        setError("Login failed (Glacier2.CannotCreateSessionException):\n" + ex.reason);
                    }
                    catch(final Glacier2.PermissionDeniedException ex)
                    {
                        setError("Login failed (Glacier2.PermissionDeniedException):\n" + ex.reason);
                    }
                    catch(Ice.Exception ex)
                    {
                        setError("Login failed (" + ex.ice_name() + ").\n" +
                                "Please check your configuration.");
                    }
                    catch(final Throwable ex) {
                        setError("Login failed:\n" + stack2string(ex));
                    }
                    _service.loginFailed();
                }

                public void
                createdCommunicator(Glacier2.SessionHelper session)
                {

                    Ice.Communicator communicator = session.communicator();

                    if(communicator.getProperties().getPropertyAsIntWithDefault("IceSSL.UsePlatformCAs", 0) == 0)
                    {
                        java.io.InputStream certStream = resources.openRawResource(R.raw.client);
                        IceSSL.Plugin plugin = (IceSSL.Plugin)communicator.getPluginManager().getPlugin("IceSSL");
                        plugin.setTruststoreStream(certStream);
                        communicator.getPluginManager().initializePlugins();
                    }
                }
            });
            _session = factory.connect("", "");
        }*/

            ic = Ice.Util.initialize(initData);

            Ice.ObjectPrx obj = ic.stringToProxy("IceStorm/TopicManager:tcp -h "+Client.IP_ADDRESS_SERVER+" -p "+ Client.PORT_SERVER);
            IceStorm.TopicManagerPrx topicManager = IceStorm.TopicManagerPrxHelper.checkedCast(obj.ice_timeout(1000));

            if(topicManager == null)
            {
                Log.e("AsyncStreamThread", "invalid proxy");
                return new Exception("Invalid Proxy");
            }

            IceStorm.TopicPrx topic = null;

            while (topic == null)
            {
                try
                {
                    topic = topicManager.retrieve("ArmarXSpeech");
                }
                catch (IceStorm.NoSuchTopic ex)
                {
                    try
                    {
                        String err = (ex.getMessage()==null)?ex.toString():ex.getMessage();
                        Log.e("NoSuchTopic:",err);
                        topic = topicManager.create("ArmarXSpeech");
                    }
                    catch (IceStorm.TopicExists e)
                    {
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
            Log.e("ArmarXSpeech", te.toString());
            return te;
        }
        catch(Ice.LocalException le)
        {
            Log.e("ArmarXSpeech", le.toString());
            return le;
        }
        catch (Exception e)
        {
            Log.e("ArmarXSpeech", e.toString());
            return e;
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

            Log.d("Client Connection", "IP Server: "+Client.IP_ADDRESS_SERVER);
            Ice.ObjectPrx obj = communicator.stringToProxy("IceStorm/TopicManager:tcp -p "+Client.PORT_SERVER+" -h "+Client.IP_ADDRESS_SERVER);
            IceStorm.TopicManagerPrx topicManager = IceStorm.TopicManagerPrxHelper.checkedCast(obj);
            IceStorm.TopicPrx topic = null;

            while (topic == null) {
                try {
                    topic = topicManager.retrieve("ArmarXSpeechChat");
                } catch (IceStorm.NoSuchTopic ex) {
                    try {
                        String err = (ex.getMessage()==null)?ex.toString():ex.getMessage();
                        Log.e("NoSuchTopic:",err);
                        topic = topicManager.create("ArmarXSpeechChat");
                    } catch (IceStorm.TopicExists e) {
                        String err = (e.getMessage()==null)?e.toString():e.getMessage();
                        Log.e("TopicExists:",err);
                    }
                }
            }

            Ice.ObjectPrx pub = topic.getPublisher().ice_oneway();
            TextListenerInterfacePrx textListener = TextListenerInterfacePrxHelper.uncheckedCast(pub);
            textListener.reportText(msg, Client.getIPAddress(true));

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