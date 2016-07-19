package org.eclipse.jetty.embedded;
/**
 * Created by netherwire on 5/23/16.
 */

import org.eclipse.jetty.http.MimeTypes;
import org.eclipse.jetty.http.ResourceHttpContent;
import org.eclipse.jetty.server.HttpOutput;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.AbstractHandler;
import org.eclipse.jetty.util.Callback;
import org.eclipse.jetty.util.log.Log;

import javax.servlet.AsyncContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.StandardOpenOption;

public class Main extends AbstractHandler
{

    private final MimeTypes mimeTypes = new MimeTypes();
    private File dir = null; //тут будет путь к папке с ресурсами

    public static void main(String[] args)
    {
        final Server server = new Server(8080);


        server.setHandler(new Main());

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


    @Override
    public void handle(String s, Request request, HttpServletRequest httpServletRequest,
                       HttpServletResponse httpServletResponse) throws IOException, ServletException
    {
        String requestType = request.getParameter("type");

        Log.getRootLogger().info("Request added.");
        Log.getRootLogger().info("Request type is " + requestType);


        if(requestType != null && requestType != "")
        {
            switch (requestType)
            {
                case "items":
                {
                    sendFileHandler("res/items.xml", request, httpServletRequest, httpServletResponse);
                }
                break;
                case "socialNets":
                {
                    sendFileHandler("res/social_networks.xml", request, httpServletRequest, httpServletResponse);
                }
                break;
                case "stickerPack":
                {
                    String id = request.getParameter("id");
                    sendFileHandler("res/stickers/" + id + ".zip", request, httpServletRequest, httpServletResponse);
                }
                break;
                default:
                {
                    Log.getRootLogger().info("wrong request type");
                }
                break;
            }

        }
    }

    private void sendFileHandler(String path, Request baseRequest,
                                 HttpServletRequest request,
                                 HttpServletResponse response) throws IOException {

        //Определяем размер файла
        //Будем исходить из размера буфера
        final int SMALL = response.getBufferSize();
        final int MEDIUM = 8 * SMALL;
        dir = new File(System.getProperty("user.dir"));
        final File file = new File(dir, path);

        Log.getRootLogger().info("the dir is " + dir.getPath());
        Log.getRootLogger().info("the file is " + file.getPath());
        Log.getRootLogger().info("the file to path is " + file.toPath());
        //проверяем на существование
        if (!file.exists())
            return;

        Log.getRootLogger().info("File is exist");

        //хэндлим реквест
        baseRequest.setHandled(true);


        // форматируем данные файла (так, на всякий случай)
        response.setDateHeader("Last-Modified", file.lastModified());
        //response.setDateHeader("Content-Length", file.length());
        response.setContentType(mimeTypes.getMimeByExtension(file.getName()));

        Log.getRootLogger().info("MimeType Is " + mimeTypes.getMimeByExtension(file.getName()));
        // Если файл маленький, его можно послать просто потоком в отве
        if (file.length() < SMALL)
        {
            Log.getRootLogger().info("file is small");
            // need to caste to Jetty output stream for best API
            try
            {
                ((HttpOutput) response.getOutputStream())
                        .sendContent(FileChannel.open(file.toPath(),
                                StandardOpenOption.READ));
            }
            catch (IOException e)
            {
                Log.getRootLogger().info("file is send corrupted");
                e.printStackTrace();
            }
            return;
        }

        // А большие лучше посылать асинхранно, чтобы не создавать новые Thread'ы
        final AsyncContext async = request.startAsync();
        Callback completionCB = new Callback()
        {
            @Override
            public void succeeded()
            {
                Log.getRootLogger().info("Send file succesfuly");

                // все прошло хорошо
                async.complete();
            }

            @Override
            public void failed( Throwable x )
            {
                Log.getRootLogger().info("Send failed");
                // Все прошло нехорошо, логируем ошибку
                x.printStackTrace();
                async.complete();
            }
        };

        // Средние файлы посылаем через input stream
        if (file.length() < MEDIUM)
        {
            Log.getRootLogger().info("file is medium");
            // the file channel is closed by the async send
            ((HttpOutput) response.getOutputStream())
                    .sendContent(FileChannel.open(file.toPath(),
                            StandardOpenOption.READ), completionCB);
            return;
        }

        //для больших файлов (пак стикеров) будем создавать специальный буфер и отсылать через него
        //Это ккеширует файл и упростит доступ к нему
        //TOD: Подумать, не будет ли это проблемой для GC
        ByteBuffer buffer;
        try ( RandomAccessFile raf = new RandomAccessFile(file, "r"); )
        {
            Log.getRootLogger().info("file is big");
            buffer = raf.getChannel().map(FileChannel.MapMode.READ_ONLY, 0,
                    raf.length());
        }

        buffer = buffer.asReadOnlyBuffer();

        // send the content as a buffer with a callback to complete the
        // async request need to caste to Jetty output stream for best API
        ((HttpOutput) response.getOutputStream()).sendContent(buffer,
                completionCB);

    }


}
