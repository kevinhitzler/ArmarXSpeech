package com.example.kit.armarxspeech;

import java.util.LinkedList;
import java.util.Queue;

/**
 * Created by Kevin on 30.05.2017.
 */

public class ChatManager
{
    private LinkedList<String> _messageQueue;
    private LinkedList<AudioData> _streamQueue;
    private ChatManagerInterface _interface;

    public ChatManager(ChatManagerInterface chatManagerI)
    {
        _messageQueue = new LinkedList<String>();
        _streamQueue = new LinkedList<AudioData>();
        _interface = chatManagerI;
    }

    /**
     * Enqueue the msg to the message queue
     * and send it.
     * @param msg
     */
    public void enqueueAndSendText(String msg)
    {
        _messageQueue.add(msg);
        processMessageQueue();
    }

    /**
     * Enqueue the audio data to the streaming queue
     * and stream it.
     * @param data AudioData to be streamed.
     */
    public void enqueueAndStreamAudio(AudioData data)
    {
        _streamQueue.add(data);
        processStreamingQueue();
    }

    public void doRemainingWork()
    {
        //send all messages
        processMessageQueue();
        processStreamingQueue();
    }

    /**
     * Check if there is remaining work to do
     * @return true if there is.
     */
    public boolean hasRemaining()
    {
        if (_messageQueue.size() > 0 ) return true;
        else if(_streamQueue.size() > 0) return  true;
        else return false;
    }

    /**
     * Enqueues the data to the first slot.
     * This data will be streamed first.
     * @param data String message or AudioData.
     */
    public void enqueue(Object data)
    {
        if(data instanceof String)
        {
            String msg = (String) data;
            _messageQueue.add(msg);
        }
        else if (data instanceof AudioData)
        {
            AudioData ad = (AudioData) data;
            _streamQueue.add(ad);
        }
    }

    /**
     * Process all messages from message queue (FIFO)
     */
    private void processMessageQueue()
    {
        for(int i=0; i<_messageQueue.size(); i++)
            System.out.println(_messageQueue.get(i));

        String msg;
        while(_messageQueue.size() > 0)
        {
            msg = _messageQueue.poll();
            processMessage(msg);
        }
    }

    /**
     * Process all messages from message queue (FIFO)
     */
    private void processStreamingQueue()
    {
        AudioData data;
        while(_streamQueue.size() > 0)
        {
            data = _streamQueue.poll();
            processStream(data);
        }
    }

    /**
     * Process a single message via client
     * @param msg Message to be sent
     */
    private void processMessage(String msg)
    {
        _interface.sendText(msg);
    }


    /**
     * Process a single stream via client
     * @param data Audio data to be streamed.
     */
    private void processStream(AudioData data)
    {
        _interface.streamAudio(data);
    }
}
