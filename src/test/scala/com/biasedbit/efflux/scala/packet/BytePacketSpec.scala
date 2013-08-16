/*
 * Copyright 2010 Bruno de Carvalho
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.biasedbit.efflux.scala.packet

import org.scalatest.WordSpec
import org.scalatest.matchers.MustMatchers
import com.biasedbit.efflux.util.ByteUtils
import org.jboss.netty.buffer.ChannelBuffer
import org.jboss.netty.buffer.ChannelBuffers
import org.junit.Test
import com.biasedbit.efflux.packet.ControlPacket
import com.biasedbit.efflux.packet.ByePacket
//import collection.JavaConversions._
class BytePacketSpec extends WordSpec with MustMatchers {

  "BytePacketSpec" should {

    "testDecode" in {
      // wireshark capture, X-lite
      val packetBytes: Array[Byte] = ByteUtils.convertHexStringToByteArray("81cb0001e6aa996e");

      val buffer: ChannelBuffer = ChannelBuffers.wrappedBuffer(packetBytes);
      val controlPacket: ControlPacket = ControlPacket.decode(buffer);

      ControlPacket.Type.BYE must equal(controlPacket.getType())

      val byePacket: ByePacket = controlPacket.asInstanceOf[ByePacket];
      byePacket.getSsrcList() must not be (null)
      1 must be(byePacket.getSsrcList().size())

      java.lang.Long.decode("0xe6aa996e") must be(byePacket.getSsrcList().get(0))

      byePacket.getReasonForLeaving() must be(null);

      0 must be(buffer.readableBytes())
    }
    "testDecode2" in {
      // wireshark capture, jlibrtp
      val packetBytes: Array[Byte] = ByteUtils.convertHexStringToByteArray("81cb000a4f52eb38156a6c69627274702073617973206279" +
        "6520627965210000000000000000000000000000");

      val buffer: ChannelBuffer = ChannelBuffers.wrappedBuffer(packetBytes);
      val controlPacket: ControlPacket = ControlPacket.decode(buffer);

      ControlPacket.Type.BYE must equal(controlPacket.getType())

      val byePacket: ByePacket = controlPacket.asInstanceOf[ByePacket];
      byePacket.getSsrcList() must not be (null)
      1 must be(byePacket.getSsrcList().size())

      java.lang.Long.decode("0x4f52eb38") must be(byePacket.getSsrcList().get(0))

      byePacket.getReasonForLeaving() must be("jlibrtp says bye bye!")

      0 must be(buffer.readableBytes())

    }
    "testEncodeDecode" in {
      val packet = new ByePacket()
      packet.addSsrc(0x45)
      packet.addSsrc(0x46)
      packet.setReasonForLeaving("So long, cruel world.")

      val buffer = packet.encode();
      36 must equal(buffer.readableBytes())
      println(ByteUtils.writeArrayAsHex(buffer.array(), true))
      0 must equal(buffer.readableBytes() % 4)

      val controlPacket = ControlPacket.decode(buffer)
      ControlPacket.Type.BYE must equal(controlPacket.getType())

      val byePacket = controlPacket.asInstanceOf[ByePacket];
      byePacket.getSsrcList() must not be null
      2 must equal(byePacket.getSsrcList().size())
      java.lang.Long.decode("0x45") must equal(byePacket.getSsrcList().get(0));
      java.lang.Long.decode("0x46") must equal(byePacket.getSsrcList().get(1))
      "So long, cruel world." must equal(byePacket.getReasonForLeaving());

      0 must equal(buffer.readableBytes());

    }

    "testEncodeDecodeWithFixedBlockSize64" in {
      val packet = new ByePacket();
      packet.addSsrc(0x45);
      packet.addSsrc(0x46);
      packet.setReasonForLeaving("So long, cruel world.");

      val buffer = packet.encode(0, 64);
      64 must equal(buffer.readableBytes());
      val bufferArray = buffer.array();
      System.out.println(ByteUtils.writeArrayAsHex(bufferArray, true));
      0 must equal(buffer.readableBytes() % 4);

      val controlPacket = ControlPacket.decode(buffer);
      ControlPacket.Type.BYE must equal(controlPacket.getType());

      val byePacket = controlPacket.asInstanceOf[ByePacket];
      byePacket.getSsrcList() must not equal (null)
      2 must equal(byePacket.getSsrcList().size());
      java.lang.Long.decode("0x45") must equal(byePacket.getSsrcList().get(0));
      java.lang.Long.decode("0x46") must equal(byePacket.getSsrcList().get(1));
      "So long, cruel world." must equal(byePacket.getReasonForLeaving());

      // Size without fixed block size would be 36 so padding is 64 - 36
      (64 - 36) must equal(bufferArray(bufferArray.length - 1));
      0 must equal(buffer.readableBytes());
    }

    "testEncodeDecodeWithFixedBlockSize64AndCompound" in {
      val packet = new ByePacket();
      packet.addSsrc(0x45);
      packet.addSsrc(0x46);
      packet.setReasonForLeaving("So long, cruel world.");

      val buffer = packet.encode(60, 64);
      // Alignment would be to 128 bytes *with* the other RTCP packets. So this packet is sized at 128 - 60 = 68
      68 must equal(buffer.readableBytes());
      val bufferArray = buffer.array();
      System.out.println(ByteUtils.writeArrayAsHex(bufferArray, true));
      0 must equal(buffer.readableBytes() % 4);

      val controlPacket = ControlPacket.decode(buffer);
      ControlPacket.Type.BYE must equal(controlPacket.getType());

      val byePacket = controlPacket.asInstanceOf[ByePacket];
      byePacket.getSsrcList() must not equal (null)
      2 must equal(byePacket.getSsrcList().size());
      java.lang.Long.decode("0x45") must equal(byePacket.getSsrcList().get(0));
      java.lang.Long.decode("0x46") must equal(byePacket.getSsrcList().get(1));
      "So long, cruel world." must equal(byePacket.getReasonForLeaving());

      // Size without fixed block size would be 36 so padding is 128 - (60 + 36) because current compound length is 60
      (128 - (60 + 36)) must equal(bufferArray(bufferArray.length - 1))
      0 must equal(buffer.readableBytes())
    }

  }
}
