package com.vizor.visionserver;

import org.eclipse.jetty.http.MimeTypes;
import org.eclipse.jetty.server.HttpOutput;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.handler.AbstractHandler;
import org.eclipse.jetty.util.Callback;

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

/**
 * Created by nikitavalavko on 31.05.16.
 */
public class RequestHandler extends AbstractHandler
{
    private final MimeTypes mimeTypes = new MimeTypes();
    private final File dir = null; //тут будет путь к папке с ресурсами

    @Override
    public void handle(String s, Request request, HttpServletRequest httpServletRequest,
                       HttpServletResponse httpServletResponse) throws IOException, ServletException
    {
        String requestType = request.getParameter("type");

        switch (requestType)
        {
            case "items":
            {
                sendFileHandler("res/items.xml", request, httpServletRequest, httpServletResponse);
            }
            case "socialNets":
            {
                sendFileHandler("res/social_networks.xml", request, httpServletRequest, httpServletResponse);
            }
            case "stickerPack":
            {
                String id = request.getParameter("id");
                sendFileHandler("res/stickers/" + id + ".zip", request, httpServletRequest, httpServletResponse);
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

        final File file = new File(this.dir, path);

        //проверяем на существование
        if (!file.exists())
            return;

        //хэндлим реквест
        baseRequest.setHandled(true);


        // форматируем данные файла (так, на всякий случай)
        response.setDateHeader("Last-Modified", file.lastModified());
        response.setDateHeader("Content-Length", file.length());
        response.setContentType(mimeTypes.getMimeByExtension(file.getName()));

        // Если файл маленький, его можно послать просто потоком в отве
        if (file.length() < SMALL)
        {
            // need to caste to Jetty output stream for best API
            try {
                ((HttpOutput) response.getOutputStream())
                        .sendContent(FileChannel.open(file.toPath(),
                                StandardOpenOption.READ));
            } catch (IOException e) {
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
                // все прошло хорошо
                async.complete();
            }

            @Override
            public void failed( Throwable x )
            {
                // Все прошло нехорошо, логируем ошибку
                x.printStackTrace();
                async.complete();
            }
        };

        // Средние файлы посылаем через input stream
        if (file.length() < MEDIUM)
        {
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
