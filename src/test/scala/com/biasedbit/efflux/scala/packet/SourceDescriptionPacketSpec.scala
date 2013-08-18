package com.biasedbit.efflux.scala.packet

import org.scalatest.WordSpec
import org.scalatest.matchers.MustMatchers
import org.jboss.netty.buffer.ChannelBuffer
import org.jboss.netty.buffer.ChannelBuffers
import com.biasedbit.efflux.scala.util.ByteUtils

class SourceDescriptionPacketSpec extends WordSpec with MustMatchers {

  "SourceDescriptionPacketSpec" should {

    "testDecode" in {
      // packet captured with wireshark, from X-lite
      val hexString: String = "81ca00054f52eb38010e6e756c6c406c6f63616c686f7374";
      val bytes: Array[Byte] = ByteUtils.convertHexStringToByteArray(hexString);

      val buffer: ChannelBuffer = ChannelBuffers.wrappedBuffer(bytes);
      val controlPacket: ControlPacket = ControlPacket.decode(buffer);
      RtpVersion.V2 must equal(controlPacket.getVersion());
      ControlPacket.Type.SOURCE_DESCRIPTION must equal(controlPacket.getType());

      val sdesPacket: SourceDescriptionPacket = controlPacket.asInstanceOf[SourceDescriptionPacket];
      sdesPacket.getChunks() must not equal (null);
      1 must equal(sdesPacket.getChunks().size());
      0x4f52eb38 must equal(sdesPacket.getChunks().get(0).getSsrc);
      sdesPacket.getChunks().get(0).getItems() must not equal (null);
      1 must equal(sdesPacket.getChunks().get(0).getItems().size());
      SdesChunkItem.Type.CNAME must equal(sdesPacket.getChunks().get(0).getItems().get(0).getType());
      "null@localhost" must equal(sdesPacket.getChunks().get(0).getItems().get(0).getValue());

      0 must equal(buffer.readableBytes());
    }

    "testDecode2" in {
      // packet capture with wireshark, from jlibrtp
      val hexString: String = "81ca001ee6aa996e013d383232433634303536464438344539414231324438333442463836303931354140756" +
        "e697175652e7a333644423331373042303744344333302e6f7267083110782d7274702d73657373696f6e2d69" +
        "6436353941323834434144384234443631383641324643304336383039363137300000";
      val bytes: Array[Byte] = ByteUtils.convertHexStringToByteArray(hexString);

      val buffer: ChannelBuffer = ChannelBuffers.wrappedBuffer(bytes);
      val controlPacket: ControlPacket = ControlPacket.decode(buffer);
      RtpVersion.V2 must equal(controlPacket.getVersion());
      ControlPacket.Type.SOURCE_DESCRIPTION must equal(controlPacket.getType());

      val sdesPacket: SourceDescriptionPacket = controlPacket.asInstanceOf[SourceDescriptionPacket];
      sdesPacket.getChunks() must not equal (null);
      1 must equal(sdesPacket.getChunks().size());
      0xe6aa996eL must equal(sdesPacket.getChunks().get(0).getSsrc);
      sdesPacket.getChunks().get(0).getItems() must not equal (null);
      2 must equal(sdesPacket.getChunks().get(0).getItems().size());
      SdesChunkItem.Type.CNAME must equal(sdesPacket.getChunks().get(0).getItems().get(0).getType());
      "822C64056FD84E9AB12D834BF860915A@unique.z36DB3170B07D4C30.org" must equal(sdesPacket.getChunks().get(0).getItems().get(0).getValue());
      SdesChunkItem.Type.PRIV must equal(sdesPacket.getChunks().get(0).getItems().get(1).getType());
      "x-rtp-session-id" must equal(sdesPacket.getChunks().get(0).getItems().get(1).asInstanceOf[SdesChunkPrivItem].getPrefix());
      "659A284CAD8B4D6186A2FC0C68096170" must equal(sdesPacket.getChunks().get(0).getItems().get(1).getValue());

      0 must equal(buffer.readableBytes());
    }

    "testEncode" in {
      val packet: SourceDescriptionPacket = new SourceDescriptionPacket();
      var chunk: SdesChunk = new SdesChunk();
      chunk.setSsrc(0x45);
      chunk.addItem(SdesChunkItems.createCnameItem("karma"));
      chunk.addItem(SdesChunkItems.createNameItem("Earl"));
      chunk.addItem(SdesChunkItems.createNoteItem("Hey crabman"));
      packet.addItem(chunk);
      chunk = new SdesChunk();
      chunk.setSsrc(0x46);
      chunk.addItem(SdesChunkItems.createCnameItem("Randy"));
      packet.addItem(chunk);

      val encoded: ChannelBuffer = packet.encode();
      System.out.println(ByteUtils.writeArrayAsHex(encoded.array(), true));

      0 must equal(encoded.readableBytes() % 4);

      val decoded: ControlPacket = ControlPacket.decode(encoded);
      packet.getType() must equal(decoded.getType());

      val decodedSdes: SourceDescriptionPacket = decoded.asInstanceOf[SourceDescriptionPacket];
      decodedSdes.getChunks() must not equal (null);
      2 must equal(decodedSdes.getChunks().size());

      0x45 must equal(decodedSdes.getChunks().get(0).getSsrc);
      decodedSdes.getChunks().get(0).getItems() must not equal (null);
      SdesChunkItem.Type.CNAME must equal(decodedSdes.getChunks().get(0).getItems().get(0).getType());
      "karma" must equal(decodedSdes.getChunks().get(0).getItems().get(0).getValue());
      SdesChunkItem.Type.NAME must equal(decodedSdes.getChunks().get(0).getItems().get(1).getType());
      "Earl" must equal(decodedSdes.getChunks().get(0).getItems().get(1).getValue());
      SdesChunkItem.Type.NOTE must equal(decodedSdes.getChunks().get(0).getItems().get(2).getType());
      "Hey crabman" must equal(decodedSdes.getChunks().get(0).getItems().get(2).getValue());

      0x46 must equal(decodedSdes.getChunks().get(1).getSsrc);
      decodedSdes.getChunks().get(1).getItems() must not equal (null);
      SdesChunkItem.Type.CNAME must equal(decodedSdes.getChunks().get(1).getItems().get(0).getType());
      "Randy" must equal(decodedSdes.getChunks().get(1).getItems().get(0).getValue());

      0 must equal(encoded.readableBytes());
    }

    "testEncode2" in {
      val packet: SourceDescriptionPacket = new SourceDescriptionPacket();
      var chunk: SdesChunk = new SdesChunk();
      chunk.setSsrc(0x45);
      chunk.addItem(SdesChunkItems.createCnameItem("karma"));
      chunk.addItem(SdesChunkItems.createNameItem("Earl"));
      packet.addItem(chunk);
      chunk = new SdesChunk();
      chunk.setSsrc(0x46);
      chunk.addItem(SdesChunkItems.createCnameItem("Randy"));
      packet.addItem(chunk);

      val encoded: ChannelBuffer = packet.encode();
      System.out.println(ByteUtils.writeArrayAsHex(encoded.array(), true));

      0 must equal(encoded.readableBytes() % 4);

      val decoded: ControlPacket = ControlPacket.decode(encoded);
      packet.getType() must equal(decoded.getType());

      val decodedSdes: SourceDescriptionPacket = decoded.asInstanceOf[SourceDescriptionPacket];
      decodedSdes.getChunks() must not equal (null);
      2 must equal(decodedSdes.getChunks().size());

      0x45 must equal(decodedSdes.getChunks().get(0).getSsrc);
      decodedSdes.getChunks().get(0).getItems() must not equal (null);
      SdesChunkItem.Type.CNAME must equal(decodedSdes.getChunks().get(0).getItems().get(0).getType());
      "karma" must equal(decodedSdes.getChunks().get(0).getItems().get(0).getValue());
      SdesChunkItem.Type.NAME must equal(decodedSdes.getChunks().get(0).getItems().get(1).getType());
      "Earl" must equal(decodedSdes.getChunks().get(0).getItems().get(1).getValue());

      0x46 must equal(decodedSdes.getChunks().get(1).getSsrc);
      decodedSdes.getChunks().get(1).getItems() must not equal (null);
      SdesChunkItem.Type.CNAME must equal(decodedSdes.getChunks().get(1).getItems().get(0).getType());
      "Randy" must equal(decodedSdes.getChunks().get(1).getItems().get(0).getValue());

      0 must equal(encoded.readableBytes());
    }

    "testEncodeAsPartOfCompound" in {
      val packet: SourceDescriptionPacket = new SourceDescriptionPacket();
      var chunk: SdesChunk = new SdesChunk();
      chunk.setSsrc(0x45);
      chunk.addItem(SdesChunkItems.createCnameItem("karma"));
      chunk.addItem(SdesChunkItems.createNameItem("Earl"));
      packet.addItem(chunk);
      chunk = new SdesChunk();
      chunk.setSsrc(0x46);
      chunk.addItem(SdesChunkItems.createCnameItem("Randy"));
      packet.addItem(chunk);

      // 36 bytes
      var encoded: ChannelBuffer = packet.encode();
      System.out.println(ByteUtils.writeArrayAsHex(encoded.array(), true));
      System.out.println("simple encoding length: " + encoded.readableBytes());
      0 must equal(encoded.readableBytes() % 4);
      // assuming previous 20 bytes, 36 bytes of normally encoded packet thus become 44 (+8 for padding, 20+36+8 = 64)
      encoded = packet.encode(20, 64);
      System.out.println("compound encoding length: " + encoded.readableBytes()); // 20
      encoded.skipBytes(encoded.readableBytes() - 1);
      8 must equal(encoded.readByte());
    }
  }
}