package com.vizor.visionserver;

/**
 * Created by netherwire on 5/23/16.
 */

import org.eclipse.jetty.server.Server;

public class Main
{
    public static void main(String[] args)
    {
        final Server server = new Server(8080);
        server.setHandler(new RequestHandler());

        try
        {
            server.start();
            server.join();
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }


}
