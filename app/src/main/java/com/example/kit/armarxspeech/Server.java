package com.example.kit.armarxspeech;
import android.util.Log;

import java.util.regex.Pattern;
import Ice.ObjectAdapter;

/**
 * Created by Kevin on 16.05.2017.
 */

public class Server
{
    public static String IP_SERVER = "192.168.1.186";
    public static final String PORT_SERVER = "80";
    private Client client;

    public void start(String[] args)
    {
        //new thread...

        Ice.Communicator communicator = null;
        Log.d("App-Server","Starting server...");
        try
        {
            communicator = Ice.Util.initialize(args);
            Ice.ObjectPrx obj = communicator.stringToProxy("IceStorm/TopicManager:default -p "+PORT_SERVER+" -h "+IP_SERVER);
            IceStorm.TopicManagerPrx topicManager = IceStorm.TopicManagerPrxHelper.checkedCast(obj);

            ObjectAdapter adapter = communicator.createObjectAdapterWithEndpoints("TextResponderInterface", "tcp -p 9999 -h "+IP_SERVER);

            AsyncStreamingInterface async = new AsyncStreamingInterface();
            Ice.ObjectPrx proxy = adapter.addWithUUID(async).ice_oneway();
            adapter.activate();

            IceStorm.TopicPrx topic = null;

            try
            {
                Log.d("App-Server","Retrieving topic ...");
                topic = topicManager.retrieve("TextResponderInterface");

            }
            catch (IceStorm.NoSuchTopic ex)
            {

                // Error! No topic found!
                Log.d("App-Server","No topic Found ... Creating Topic.");
                try
                {
                    topic = topicManager.create("ArmarXSpeech");
                }
                catch(IceStorm.TopicExists ex2)
                {
                    Log.d("App-Server","Temporary failure: "+ex2.getMessage()+", please try again.");
                }

            }

            java.util.Map qos = null;
            topic.subscribeAndGetPublisher(qos, proxy);

            communicator.waitForShutdown();

            topic.unsubscribe(proxy);
        }
        catch (Ice.LocalException e) {
            e.printStackTrace();
        } catch (Exception e) {
            Log.d("App-Server","Here it is...");
            e.printStackTrace();
        }
        if (communicator != null) {
            // Clean up
            //
            try {
                communicator.destroy();
            } catch (Exception e) {
                Log.d("App-Server",e.getMessage());
            }
        }
    }
}