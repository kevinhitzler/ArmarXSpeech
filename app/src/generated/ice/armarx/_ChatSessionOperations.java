// **********************************************************************
//
// Copyright (c) 2003-2016 ZeroC, Inc. All rights reserved.
//
// This copy of Ice is licensed to you under the terms described in the
// ICE_LICENSE file included in this distribution.
//
// **********************************************************************
//
// Ice version 3.6.3
//
// <auto-generated>
//
// Generated from file `SpeechInterface.ice'
//
// Warning: do not edit this file.
//
// </auto-generated>
//

package armarx;

public interface _ChatSessionOperations extends Glacier2._SessionOperations
{
    void setCallback(ChatCallbackPrx cb, Ice.Current __current);

    void sendText(long timestamp, String name, String message, Ice.Current __current);

    void sendChunkAsync(int offset, byte[] data, int minBufferSize, AudioEncoding encoding, long timestamp, boolean isNewSentence, Ice.Current __current);
}