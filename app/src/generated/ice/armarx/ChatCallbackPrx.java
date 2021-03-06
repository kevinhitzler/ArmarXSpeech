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

public interface ChatCallbackPrx extends Ice.ObjectPrx
{
    public void send(long timestamp, String name, String message);

    public void send(long timestamp, String name, String message, java.util.Map<String, String> __ctx);

    public Ice.AsyncResult begin_send(long timestamp, String name, String message);

    public Ice.AsyncResult begin_send(long timestamp, String name, String message, java.util.Map<String, String> __ctx);

    public Ice.AsyncResult begin_send(long timestamp, String name, String message, Ice.Callback __cb);

    public Ice.AsyncResult begin_send(long timestamp, String name, String message, java.util.Map<String, String> __ctx, Ice.Callback __cb);

    public Ice.AsyncResult begin_send(long timestamp, String name, String message, Callback_ChatCallback_send __cb);

    public Ice.AsyncResult begin_send(long timestamp, String name, String message, java.util.Map<String, String> __ctx, Callback_ChatCallback_send __cb);

    public Ice.AsyncResult begin_send(long timestamp, 
                                      String name, 
                                      String message, 
                                      IceInternal.Functional_VoidCallback __responseCb, 
                                      IceInternal.Functional_GenericCallback1<Ice.Exception> __exceptionCb);

    public Ice.AsyncResult begin_send(long timestamp, 
                                      String name, 
                                      String message, 
                                      IceInternal.Functional_VoidCallback __responseCb, 
                                      IceInternal.Functional_GenericCallback1<Ice.Exception> __exceptionCb, 
                                      IceInternal.Functional_BoolCallback __sentCb);

    public Ice.AsyncResult begin_send(long timestamp, 
                                      String name, 
                                      String message, 
                                      java.util.Map<String, String> __ctx, 
                                      IceInternal.Functional_VoidCallback __responseCb, 
                                      IceInternal.Functional_GenericCallback1<Ice.Exception> __exceptionCb);

    public Ice.AsyncResult begin_send(long timestamp, 
                                      String name, 
                                      String message, 
                                      java.util.Map<String, String> __ctx, 
                                      IceInternal.Functional_VoidCallback __responseCb, 
                                      IceInternal.Functional_GenericCallback1<Ice.Exception> __exceptionCb, 
                                      IceInternal.Functional_BoolCallback __sentCb);

    public void end_send(Ice.AsyncResult __result);
}
