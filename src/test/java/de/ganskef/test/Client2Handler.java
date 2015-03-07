package de.ganskef.test;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufInputStream;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.HttpContent;
import io.netty.handler.codec.http.HttpObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;

import org.apache.commons.io.IOUtils;

public class Client2Handler extends SimpleChannelInboundHandler<HttpObject> {

    private File result;

    public Client2Handler(String target) {
        result = new File(target);
        if (result.exists() && !result.delete()) {
            throw new IllegalStateException("Coudn't be deleted " + result);
        }
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, HttpObject msg)
            throws Exception {
        if (msg instanceof HttpContent) {
            HttpContent content = (HttpContent) msg;
            OutputStream os = null;
            InputStream is = null;
            try {
                ByteBuf buffer = content.content().copy();
                is = new ByteBufInputStream(buffer);
                os = new FileOutputStream(result, true);
                IOUtils.copy(is, os);
            } finally {
                IOUtils.closeQuietly(os);
                IOUtils.closeQuietly(is);
            }
        }
    }

    public File getFile() {
        return result;
    }
}