package com.example.kit.armarxspeech;

import android.util.Log;

public class Client
{
    public static void send(String msg)
    {
        new Thread(new PrinterThread(msg)).run();
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

            Ice.ObjectPrx base = ic.stringToProxy("SimplePrinter:tcp -h 192.168.0.19 -p 9999");
            Demo.PrinterPrx printer = Demo.PrinterPrxHelper.checkedCast(base);
            if (printer == null)
                throw new Error("Invalid proxy");

            printer.printString(this.msg);

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

