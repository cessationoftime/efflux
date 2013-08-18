package com.biasedbit.efflux.scala.packet

import org.scalatest.WordSpec
import org.scalatest.matchers.MustMatchers
import com.biasedbit.efflux.util.ByteUtils
import org.jboss.netty.buffer.ChannelBuffer
import org.jboss.netty.buffer.ChannelBuffers

class SourceChunkSpec extends WordSpec with MustMatchers {

  "SourceChunkSpec" should {
    "testEncodeDecode" in {
      val ssrc: Long = 0x0000ffff;
      val chunk: SdesChunk = new SdesChunk(ssrc);
      chunk.addItem(SdesChunkItems.createCnameItem("cname"));
      chunk.addItem(SdesChunkItems.createNameItem("name"));
      chunk.addItem(SdesChunkItems.createEmailItem("email"));
      chunk.addItem(SdesChunkItems.createPrivItem("prefix", "value"));

      val encoded: ChannelBuffer = chunk.encode();
      // Must be 32 bit aligned.
      0 must equal(encoded.readableBytes() % 4);
      System.err.println("encoded readable bytes: " + encoded.readableBytes());
      val decoded: SdesChunk = SdesChunk.decode(encoded);

      chunk.getSsrc must equal(decoded.getSsrc);
      decoded.getItems() must not equal (null);
      4 must equal(decoded.getItems().size());

      for (i ← 0 until chunk.getItems().size()) {
        chunk.getItems().get(i).getType() must equal(decoded.getItems().get(i).getType());
        chunk.getItems().get(i).getValue() must equal(decoded.getItems().get(i).getValue());
      }

      0 must equal(encoded.readableBytes());
    }
  }
}