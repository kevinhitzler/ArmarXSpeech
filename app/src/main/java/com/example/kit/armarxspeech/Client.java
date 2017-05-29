package com.example.kit.armarxspeech;
import android.os.AsyncTask;
import android.util.Log;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.LinkedList;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import Glacier2.SessionHelper;
import Glacier2.SessionNotExistException;
import Ice.AsyncResult;
import armarx.AudioEncoding;
import armarx.ChatCallbackPrx;
import armarx.ChatCallbackPrxHelper;
import armarx.ChatSessionPrx;
import armarx.ChatSessionPrxHelper;

/**
 * Created by Kevin on 29.05.2017.
 */
public class Client
{
    public static String SERVER_IP = "192.168.43.48";
    public static String SERVER_PORT = "80";

    private boolean _isReady;
    private MainActivity _activity;
    private final ScheduledExecutorService _scheduler;
    private Ice.Communicator _communicator;
    private Glacier2.SessionHelper _session;
    private Glacier2.SessionFactoryHelper _factory;
    private Ice.InitializationData _initData;
    private ChatSessionPrx _chat;

    public Client(MainActivity activity)
    {
        _isReady = false;
        _activity = activity;
        _scheduler = Executors.newScheduledThreadPool(1);
    }

    private void initialize()
    {
        // set initial glacier2 properties
        //Ice.StringSeqHolder argsH = new Ice.StringSeqHolder(args);
        Ice.Properties properties = Ice.Util.createProperties();
        properties.setProperty("Ice.Default.Router", "Glacier2/router:tcp -h "+ SERVER_IP + " -p " + SERVER_PORT);
        properties.setProperty("Ice.RetryIntervals", "-1");
        properties.setProperty("Ice.Trace.Network", "0"); //set to 1 or 2 to debug
        _initData = new Ice.InitializationData();
        _initData.properties = properties;

        // add SessionFactoryHelper for communicator intialization
        _factory = new Glacier2.SessionFactoryHelper(_initData,
                new Glacier2.SessionCallback()
                {
                    @Override
                    public void connected(Glacier2.SessionHelper session)
                            throws Glacier2.SessionNotExistException
                    {
                        // If the session has been reassigned avoid the spurious callback.
                        if (session != _session)
                        {
                            System.out.println("Warning: Spurious callback. Exit.");
                            return;
                        }

                        // Check session one more time
                        if (!_session.isConnected())
                        {
                            System.out.println("Warning: Session is no longer connected! Exit.");
                            return;
                        }

                        System.out.println("Connected to Glacier2 Service. OK!");
                        _activity.runOnUiThread(new ConsoleWriter("Please press the button below to start."));

                        registerChatCallback();
                        ready();
                    }

                    @Override
                    public void disconnected(Glacier2.SessionHelper session)
                    {
                        System.out.println("Disconnected from Glacier2 Service.");
                    }

                    @Override
                    public void connectFailed(Glacier2.SessionHelper session, Throwable exception)
                    {
                        String status = null;

                        try
                        {
                            throw exception;
                        }
                        catch (final Glacier2.CannotCreateSessionException ex)
                        {
                            status = "Login failed (Glacier2.CannotCreateSessionException):\n"+ ex.reason;
                            System.out.println(status);
                        }
                        catch (final Glacier2.PermissionDeniedException ex)
                        {
                            status = "Login failed (Glacier2.PermissionDeniedException):\n"+ ex.reason;
                            System.out.println(status);
                        }
                        catch (Ice.Exception ex)
                        {
                            status = "Login failed (" + ex.ice_name()+ ").\n"+ "Please check your configuration.";
                            System.out.println(status);
                        }
                        catch (Throwable ex)
                        {
                            status = ex.getStackTrace().toString();
                            ex.printStackTrace();
                        }

                        // show user error & retry with scheduler
                        _scheduler.schedule(new Connector(), 2000, TimeUnit.MILLISECONDS);
                    }

                    @Override
                    public void createdCommunicator(SessionHelper session)
                    {
                        if (session.communicator() != null)
                        {
                            System.out.println("Communicator initialized. OK!");
                        }
                    }
                });

        // Connect to session
        System.out.print("Trying to connect to Session ...");
        _activity.runOnUiThread(new ConsoleWriter("Trying to connect to '"+SERVER_IP+":"+SERVER_PORT+"'"));

        _factory.setRouterHost(SERVER_IP);
        _session = _factory.connect("", "");
    }

    public void connect()
    {
        _scheduler.schedule(new Connector(), 0, TimeUnit.MILLISECONDS);
    }

    public boolean isReady()
    {
        return _isReady;
    }

    public void sendTextMessage(String message)
    {
        // create new async thread to send message
        new TextReporter(message).execute();
    }

    public void streamAudioFile(String filepath, AudioEncoding encoding, int minBufferSize)
    {
        try
        {
            // create new async thread to send message

            new AsyncStreamThread(filepath, encoding, minBufferSize).execute();
            System.out.println("OK!");
        }
        catch (Exception e)
        {
            System.out.println("Failed!");
            System.out.println("Reason: "+e.getMessage());
            e.printStackTrace();
            System.out.println("Exit.");
        }
    }

    public void shutdown()
    {
        if (_communicator != null)
        {
            try
            {
                System.out.println("----------------------------------------");
                System.out.print("I am done, destroying communicator...");
                _isReady = false;
                _communicator.destroy();
                System.out.print("OK!");
            }
            catch (Exception e)
            {
                System.out.println("Session could not be destroyed. Exit.");
                System.out.println(e.getMessage());
            }
        }
    }

    private void registerChatCallback()
    {
        try
        {
            // register callback
            System.out.print("Trying to register ChatCallback...");
            ChatCallbackPrx callback = ChatCallbackPrxHelper.uncheckedCast(_session.addWithUUID(new ChatCallbackI()));
            _chat = ChatSessionPrxHelper.uncheckedCast(_session.session());
            _chat.setCallback(callback);
            System.out.println("OK!\n");
        }
        catch (SessionNotExistException e)
        {
            System.out.print("Failed!\nReason: "+e.getMessage());
            e.printStackTrace();
        }
    }

    private void ready()
    {
        System.out.println("I am ready!");
        _isReady = true;
    }

    private class TextReporter extends AsyncTask<Void, Void, Exception>
    {
        String _msg;

        public TextReporter(Object msg)
        {
            _msg = (String) msg;
        }

        private long getTimestamp()
        {
            return System.currentTimeMillis();
        }

        @Override
        protected Exception doInBackground(Void... params)
        {
            try
            {
                //send messages through ChatCallbackI
                System.out.print("Sending Message '"+_msg+"' ...");
                _chat.sendText(getTimestamp(), "ClientApp", _msg);
                System.out.println("OK!");
                return null;
            }
            catch (Exception e)
            {
                System.out.println("Failed!\nReason: "+e.getMessage());
                e.printStackTrace();
                return e;
            }
        }

        @Override
        protected void onPostExecute(final Exception result)
        {
            if (result != null)
            {
                _activity.runOnUiThread(new ConsoleWriter("Sorry, message could not be sent.\n"+result.getMessage()));
                _scheduler.schedule(new Connector(), 2000, TimeUnit.MILLISECONDS);
            }
        }
    }

    class ConsoleWriter implements Runnable
    {
        String _msg;

        ConsoleWriter(String msg)
        {
            _msg = msg;
        }
        @Override
        public void run()
        {
            _activity.printToConsole(_msg);
        }
    }

    class Connector implements Runnable
    {
        @Override
        public void run()
        {
            initialize();
        }
    }

    class AsyncStreamThread extends AsyncTask<Void, Void, Exception>
    {
        String filepath;
        AudioEncoding encoding;
        int minBufferSize;
        Glacier2.SessionHelper _session;

        public AsyncStreamThread(String filepath, AudioEncoding encoding, int minBufferSize)
        {
            this.filepath = (String)filepath;
            this.encoding = (AudioEncoding) encoding;
            this.minBufferSize = minBufferSize;
        }

        private byte[] streamPartialFile() throws Exception
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
                    Ice.AsyncResult r = _chat.begin_sendChunkAsync(offset, byteSeq, minBufferSize, AudioEncoding.PCM, System.currentTimeMillis(), isNewSentence);
                    isNewSentence = false;


                    // Wait until this request has been passed to the transport.
                    r.waitForSent();
                    results.add(r);

                    // Once there are more than numRequests, wait for the least
                    // recent one to complete.
                    while (results.size() > numRequests)
                    {
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
                throw e;
            }
            catch (IOException e)
            {
                Log.d("ClientIO", e.getMessage());
                throw e;
            }
            finally
            {
                try
                {
                    fis.close();
                }
                catch (IOException ex)
                {
                    ex.printStackTrace();
                }
            }

            return null;
        }

        @Override
        protected Exception doInBackground(Void... params)
        {
            try
            {
                //send messages through ChatCallbackI
                System.out.print("Streaming audio file '"+filepath+"' ...");
                streamPartialFile();
                System.out.println("OK!");
                return null;
            }
            catch (Exception e)
            {
                System.out.println("Failed!\nReason: "+e.getMessage());
                e.printStackTrace();
                return e;
            }
        }


        @Override
        protected void onPostExecute(final Exception result)
        {
            if (result != null)
            {
                _activity.runOnUiThread(new ConsoleWriter("Sorry, message could not be sent.\n"+result.getMessage()));
                _scheduler.schedule(new Connector(), 2000, TimeUnit.MILLISECONDS);
            }
        }

        private long getTimestamp()
        {
            return System.currentTimeMillis();
        }
    }

}
