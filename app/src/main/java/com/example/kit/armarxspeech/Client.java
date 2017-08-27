package com.example.kit.armarxspeech;
import android.content.Context;
import android.os.AsyncTask;
import android.support.v4.content.ContextCompat;
import android.util.Log;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.LinkedList;

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
    public static final int RECONNECT_DELAY_TIME = 5000;
    public static String SERVER_IP = "192.168.43.64";
    public static String SERVER_PORT = "80";

    private MainActivity _activity;
    private Ice.Communicator _communicator;
    private Glacier2.SessionHelper _session;
    private Glacier2.SessionFactoryHelper _factory;
    private Ice.InitializationData _initData;
    private ChatSessionPrx _chat;
    private ChatManager _chatManager;
    private ReconnectorTask _reconnector;
    private boolean isInitialized;

    public Client(MainActivity activity)
    {
        _activity = activity;
        isInitialized = false;

        // Tell ChatManager what to do on send/stream
        _chatManager = new ChatManager(new ChatManagerInterface()
        {
            @Override
            public void sendText(String msg)
            {
                // create new async thread to send message
                new TextReporter(msg).execute();
            }

            @Override
            public void streamAudio(AudioData data)
            {
                // create new async thread to send message
                new AsyncStreamThread(data.getFilePath(), data.getEncoding(),
                        data.getMinBufferSize()).execute();
            }
        });

        // Create task for holding connection
        _reconnector = new ReconnectorTask(new ReconnectorTaskInterface()
        {
            @Override
            public void reconnectToServer()
            {
                if(!isInitialized)
                {
                    initialize();
                }

                // reconnect and do remaining work
                connectToServer();
            }

            @Override
            public boolean isConnected()
            {
                if(_chat != null)
                {
                    try
                    {
                        _chat.ice_isA(ChatCallbackPrxHelper.ice_staticId());
                        return true;
                    }
                    catch (Ice.TimeoutException te)
                    {
                        te.printStackTrace();
                        return false;
                    }
                    catch (Exception e)
                    {
                        e.printStackTrace();
                        return false;
                    }
                }
                else
                {
                    return false;
                }
            }
        });
    }

    public void connect()
    {
        if(!_reconnector.isRunning())
        {
            System.out.println("Reconnector is not running...");
            _reconnector.reconnect(RECONNECT_DELAY_TIME);
        }
    }

    public void sendTextMessage(String message)
    {
        _chatManager.enqueueAndSendText(message);

    }

    public void streamAudioFile(String filepath, AudioEncoding encoding, int minBufferSize)
    {
        AudioData ad = new AudioData(filepath, encoding, minBufferSize);
        _chatManager.enqueueAndStreamAudio(ad);
    }

    public void shutdown()
    {
        isInitialized = false;
        if (_communicator != null)
        {
            try
            {
                System.out.println("----------------------------------------");
                System.out.print("I am done, destroying communicator...");
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
                        _activity.runOnUiThread(new InfoWriter("Connected. I am ready!", ContextCompat.getColor(_activity,R.color.green)));

                        registerChatCallback();

                        if(_chatManager.hasRemaining())
                        {
                            _chatManager.doRemainingWork();
                        }
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

                        // show user error & retry
                        connect();
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
    }

    private void connectToServer()
    {
        // Connect to session
        System.out.print("Trying to connect to Session ...");
        _activity.runOnUiThread(new InfoWriter("Trying to connect to '"+SERVER_IP+":"+SERVER_PORT+"'", ContextCompat.getColor(_activity,R.color.orange)));

        _factory.setRouterHost(SERVER_IP);
        _session = _factory.connect("", "");
    }

    private void registerChatCallback()
    {
        try
        {
            // register callback
            System.out.print("Trying to register ChatCallback...");
            ChatCallbackPrx callback = ChatCallbackPrxHelper.uncheckedCast(_session.addWithUUID(new ChatCallbackI(_activity)));
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

    private class TextReporter extends AsyncTask<Void, Void, Exception>
    {
        String _msg;

        public TextReporter(Object msg)
        {
            _msg = (String) msg;
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
                //_activity.runOnUiThread(new InfoWriter("Sorry, message could not be sent. Trying to reconnect ...", Color.RED));
                _chatManager.enqueue(_msg);
                connect();
            }
        }

        private long getTimestamp()
        {
            return System.currentTimeMillis();
        }

    }

    class AsyncStreamThread extends AsyncTask<Void, Void, Exception>
    {
        private String filepath;
        private AudioEncoding encoding;
        private int minBufferSize;
        private Glacier2.SessionHelper _session;

        public AsyncStreamThread(String filepath, AudioEncoding encoding, int minBufferSize)
        {
            this.filepath = (String)filepath;
            this.encoding = (AudioEncoding) encoding;
            this.minBufferSize = minBufferSize;
        }

        private void streamPartialFile() throws Exception
        {
            File file = null;
            FileInputStream fis = null;
            boolean hasErrors = false;

            try
            {
                boolean isNewSentence = true;
                file = new File(filepath);
                fis = new FileInputStream(file);

                if(file.length() <= minBufferSize)
                {
                    Log.w("Client", "File size smaller than minbuffer: "+file.length()+" < "+minBufferSize);
                    Log.w("Client", "Canceling streaming operation.");
                    throw new Exception("File size to small for Streaming");
                }

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
                    _chat.sendChunkAsync(offset, byteSeq, minBufferSize, AudioEncoding.PCM, System.currentTimeMillis(), isNewSentence);
                    isNewSentence = false;
                }

                fis.close();
                //WaveRecorder.deleteTempFile();
            }
            catch (FileNotFoundException e)
            {
                hasErrors = true;
                Log.d("ClientFNF", e.getMessage());
                throw e;
            }
            catch (IOException e)
            {
                hasErrors = true;
                Log.d("ClientIO", e.getMessage());
                throw e;
            }
            catch (Exception e)
            {
                Log.d("Client", e.getMessage());
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

            return;
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
                //_activity.runOnUiThread(new InfoWriter("Sorry, audio could not be streamed. Trying to reconnect ...", ));
                _chatManager.enqueue(new AudioData(filepath, encoding, minBufferSize));
                connect();
            }
            else
            {
                WaveRecorder.deleteTempFile();
            }
        }

        private long getTimestamp()
        {
            return System.currentTimeMillis();
        }
    }

    private class InfoWriter implements Runnable
    {
        String _msg;
        int _color;

        InfoWriter(String msg, int bgColor)
        {
            _msg = msg;
            _color = bgColor;
        }
        @Override
        public void run()
        {
            _activity.printToInfoBar(_msg, _color);
        }
    }
}
