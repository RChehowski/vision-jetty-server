package com.vizor.visionserver;

/**
 * Created by netherwire on 5/23/16.
 */
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.ServletException;
import java.io.IOException;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.handler.AbstractHandler;

public class Main
{
    public static void main(String[] args)
    {
        final Server server = new Server(8080);
        server.setHandler(new MyHandler());

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

    private static class MyHandler extends AbstractHandler
    {
        public void handle(String s, Request request, HttpServletRequest httpServletRequest,
                           HttpServletResponse httpServletResponse) throws IOException, ServletException
        {
            httpServletResponse.setContentType("text/html;charset=utf-8");
            httpServletResponse.setStatus(HttpServletResponse.SC_OK);
            request.setHandled(true);
            httpServletResponse.getWriter().println(
                    "<!DOCTYPE html>\n" +
                    "<html>\n" +
                    "<body>\n" +
                    "\n" +
                    "<h1>Vision server\n" +
                    "\n" +
                    "<p>That's a real deal.</p>\n" +
                    "\n" +
                    "</body>\n" +
                    "</html>\n"
            );
        }
    }
}
