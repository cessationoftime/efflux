package com.biasedbit.efflux.scala.packet

import org.scalatest.WordSpec
import org.scalatest.matchers.MustMatchers
import com.biasedbit.efflux.util.ByteUtils
import org.jboss.netty.buffer.ChannelBuffer
import org.jboss.netty.buffer.ChannelBuffers
import com.biasedbit.efflux.packet.ControlPacket
import com.biasedbit.efflux.packet.ByePacket
import com.biasedbit.efflux.packet.ReceiverReportPacket

class ReceiverReportPacketSpec extends WordSpec with MustMatchers {

  "ReceiverReportPacketSpec" should {

    "testDecode" in {
      // wireshark capture, from jlibrtp
      val packetBytes: Array[Byte] = ByteUtils.convertHexStringToByteArray("80c90001e6aa996e");

      val buffer: ChannelBuffer = ChannelBuffers.wrappedBuffer(packetBytes);
      val controlPacket: ControlPacket = ControlPacket.decode(buffer);

      ControlPacket.Type.RECEIVER_REPORT must equal(controlPacket.getType());

      val srPacket: ReceiverReportPacket = controlPacket.asInstanceOf[ReceiverReportPacket];

      0xe6aa996eL must equal(srPacket.getSenderSsrc());
      0 must equal(srPacket.getReceptionReportCount());
      srPacket.getReceptionReports() must equal(null);

      0 must equal(buffer.readableBytes());
    }
  }

}