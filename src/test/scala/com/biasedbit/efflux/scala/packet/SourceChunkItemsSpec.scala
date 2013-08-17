package com.biasedbit.efflux.scala.packet

import org.scalatest.WordSpec
import org.scalatest.matchers.MustMatchers
import com.biasedbit.efflux.util.ByteUtils
import org.jboss.netty.buffer.ChannelBuffer
import org.jboss.netty.buffer.ChannelBuffers
import com.biasedbit.efflux.packet.ControlPacket
import com.biasedbit.efflux.packet.ByePacket
import com.biasedbit.efflux.packet.ReceiverReportPacket
import com.biasedbit.efflux.packet.SenderReportPacket
import com.biasedbit.efflux.packet.ReceptionReport
import com.biasedbit.efflux.packet.SdesChunkItem
import com.biasedbit.efflux.packet.SdesChunkItems
import com.biasedbit.efflux.packet.SdesChunkPrivItem

class SourceChunkItemsSpec extends WordSpec with MustMatchers {

  "SourceChunkItemsSpec" should {

    "testDecode" in {
      // From partial wireshark capture
      val hexString: String = "010e6e756c6c406c6f63616c686f7374";
      val buffer: ChannelBuffer = ChannelBuffers.wrappedBuffer(ByteUtils.convertHexStringToByteArray(hexString));

      val item: SdesChunkItem = SdesChunkItems.decode(buffer);
      SdesChunkItem.Type.CNAME must equal(item.getType());
      "null@localhost" must equal(item.getValue());
      0 must equal(buffer.readableBytes());
    }

    "testEncodeNull" in {
      val buffer: ChannelBuffer = SdesChunkItems.encode(SdesChunkItems.NULL_ITEM);
      1 must equal(buffer.capacity());
      0x00 must equal(buffer.array()(0));
    }

    "testEncodeDecodeSimpleItem" in {
      val value: String = "cname value";
      val buffer: ChannelBuffer = SdesChunkItems.encode(SdesChunkItems.createCnameItem(value));
      val item: SdesChunkItem = SdesChunkItems.decode(buffer);
      SdesChunkItem.Type.CNAME must equal(item.getType());
      value must equal(item.getValue());
    }

    "testEncodeDecodeSimpleEmptyItem" in {
      val value: String = "";
      val buffer: ChannelBuffer = SdesChunkItems.encode(SdesChunkItems.createNameItem(value));
      val item: SdesChunkItem = SdesChunkItems.decode(buffer);
      SdesChunkItem.Type.NAME must equal(item.getType());
      value must equal(item.getValue());
    }

    "testEncodeDecodeSimpleItemMaxLength" in {
      val value: StringBuilder = new StringBuilder();
      for (i ← 0 until 255) {
        value.append('a');
      }

      val buffer: ChannelBuffer = SdesChunkItems.encode(SdesChunkItems.createCnameItem(value.toString()));
      val item: SdesChunkItem = SdesChunkItems.decode(buffer);
      SdesChunkItem.Type.CNAME must equal(item.getType());
      value.toString() must equal(item.getValue());
    }

    "testEncodeDecodeSimpleItemOverMaxLength" in {
      val value: StringBuilder = new StringBuilder();
      for (i ← 0 until 256) {
        value.append('a');
      }
      try {
        SdesChunkItems.encode(SdesChunkItems.createCnameItem(value.toString()));
        fail("Expected exception wasn't caught");
      } catch {
        case e: Exception ⇒
      }
    }

    "testEncoderDecodePrivItem" in {
      val prefix: String = "prefixValue";
      val value: String = "someOtherThink";
      val buffer: ChannelBuffer = SdesChunkItems.encode(SdesChunkItems.createPrivItem(prefix, value));
      val item: SdesChunkItem = SdesChunkItems.decode(buffer);
      SdesChunkItem.Type.PRIV must equal(item.getType());
      value must equal(item.getValue());
      prefix must equal(item.asInstanceOf[SdesChunkPrivItem].getPrefix());
    }
  }
}