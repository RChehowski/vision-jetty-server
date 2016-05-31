package com.vizor.visionserver;

/**
 * Created by netherwire on 5/23/16.
 */

import javax.servlet.AsyncContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.ServletException;
import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.StandardOpenOption;
import org.eclipse.jetty.util.Callback;

import org.eclipse.jetty.http.MimeTypes;
import org.eclipse.jetty.server.HttpOutput;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.handler.AbstractHandler;

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
