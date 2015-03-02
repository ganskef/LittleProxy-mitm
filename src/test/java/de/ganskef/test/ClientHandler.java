package de.ganskef.test;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.HttpContent;
import io.netty.handler.codec.http.HttpObject;

import java.io.File;
import java.io.RandomAccessFile;
import java.nio.channels.FileChannel;

import org.apache.commons.io.IOUtils;

public class ClientHandler extends SimpleChannelInboundHandler<HttpObject> {

	private File file;

	public ClientHandler(String target) {
		File dir = new File("src/test/resources/tmp");
		dir.mkdirs();
		file = new File(dir, target);
		file.delete();
	}

	@Override
	protected void channelRead0(ChannelHandlerContext ctx, HttpObject msg) throws Exception {
		if (msg instanceof HttpContent) {
			HttpContent content = (HttpContent) msg;
			RandomAccessFile output = null;
			FileChannel oc = null;
			try {
				output = new RandomAccessFile(file, "rw");
				oc = output.getChannel();
				oc.position(oc.size());
				ByteBuf buffer = content.content();
				for (int i = 0, len = buffer.nioBufferCount(); i < len; i++) {
					oc.write(buffer.nioBuffers()[i]);
				}
			} finally {
				IOUtils.closeQuietly(oc);
				IOUtils.closeQuietly(output);
			}
		}
	}

	public File getFile() {
		return file;
	}
}