package com.biasedbit.efflux.scala.packet
import org.scalatest.WordSpec
import org.scalatest.matchers.MustMatchers
import org.jboss.netty.buffer.ChannelBuffer
import org.jboss.netty.buffer.ChannelBuffers
import com.biasedbit.efflux.scala.util.ByteUtils

class SenderReportPacketSpec extends WordSpec with MustMatchers {

  "SenderReportPacketSpec" should {

    "testDecode" in {
      // wireshark capture, from X-lite
      val packetBytes: Array[Byte] = ByteUtils.convertHexStringToByteArray("80c800064f52eb38d01f84417f3b6459a91e7bd9000000020" +
        "0000002");

      val buffer: ChannelBuffer = ChannelBuffers.wrappedBuffer(packetBytes);
      val controlPacket: ControlPacket = ControlPacket.decode(buffer);

      ControlPacket.Type.SENDER_REPORT must equal(controlPacket.getType());

      val srPacket: SenderReportPacket = controlPacket.asInstanceOf[SenderReportPacket];

      0x4f52eb38L must equal(srPacket.getSenderSsrc());
      2837347289L must equal(srPacket.getRtpTimestamp);
      2 must equal(srPacket.getSenderPacketCount);
      2 must equal(srPacket.getSenderOctetCount);
      0 must equal(srPacket.getReceptionReportCount());
      srPacket.getReceptionReports() must equal(null);

      0 must equal(buffer.readableBytes());
    }

    "testDecode2" in {
      // wireshark capture, from jlibrtp
      val packetBytes: Array[Byte] = ByteUtils.convertHexStringToByteArray("80c80006e6aa996ed01f84481be76c8b001bb2b40000020b0" +
        "0015f64");

      val buffer: ChannelBuffer = ChannelBuffers.wrappedBuffer(packetBytes);
      val controlPacket: ControlPacket = ControlPacket.decode(buffer);

      ControlPacket.Type.SENDER_REPORT must equal(controlPacket.getType());

      val srPacket: SenderReportPacket = controlPacket.asInstanceOf[SenderReportPacket];

      0xe6aa996eL must equal(srPacket.getSenderSsrc());
      1815220L must equal(srPacket.getRtpTimestamp);
      523 must equal(srPacket.getSenderPacketCount);
      89956 must equal(srPacket.getSenderOctetCount);
      0 must equal(srPacket.getReceptionReportCount());
      srPacket.getReceptionReports() must equal(null);

      0 must equal(buffer.readableBytes());
    }

    "testEncodeDecode" in {
      val packet: SenderReportPacket = new SenderReportPacket();
      packet.setSenderSsrc(0x45);
      packet.setNtpTimestamp(0x45);
      packet.setRtpTimestamp(0x45);
      packet.setSenderOctetCount(20);
      packet.setSenderPacketCount(2);
      var block: ReceptionReport = new ReceptionReport();
      block.setSsrc(10);
      block.setCumulativeNumberOfPacketsLost(11);
      block.setFractionLost(12.toShort);
      block.setDelaySinceLastSenderReport(13);
      block.setInterArrivalJitter(14);
      block.setExtendedHighestSequenceNumberReceived(15);
      packet.addReceptionReportBlock(block);
      block = new ReceptionReport();
      block.setSsrc(20);
      block.setCumulativeNumberOfPacketsLost(21);
      block.setFractionLost(22.toShort);
      block.setDelaySinceLastSenderReport(23);
      block.setInterArrivalJitter(24);
      block.setExtendedHighestSequenceNumberReceived(25);
      packet.addReceptionReportBlock(block);

      val encoded: ChannelBuffer = packet.encode();
      0 must equal(encoded.readableBytes() % 4);

      val controlPacket: ControlPacket = ControlPacket.decode(encoded);
      ControlPacket.Type.SENDER_REPORT must equal(controlPacket.getType());

      val srPacket: SenderReportPacket = controlPacket.asInstanceOf[SenderReportPacket];

      0x45 must equal(srPacket.getNtpTimestamp());
      0x45 must equal(srPacket.getRtpTimestamp);
      20 must equal(srPacket.getSenderOctetCount);
      2 must equal(srPacket.getSenderPacketCount);
      srPacket.getReceptionReports() must not equal (null);
      2 must equal(srPacket.getReceptionReportCount());
      2 must equal(srPacket.getReceptionReports().size());
      10 must equal(srPacket.getReceptionReports().get(0).getSsrc);
      11 must equal(srPacket.getReceptionReports().get(0).getCumulativeNumberOfPacketsLost);
      12 must equal(srPacket.getReceptionReports().get(0).getFractionLost);
      13 must equal(srPacket.getReceptionReports().get(0).getDelaySinceLastSenderReport);
      14 must equal(srPacket.getReceptionReports().get(0).getInterArrivalJitter);
      15 must equal(srPacket.getReceptionReports().get(0).getExtendedHighestSequenceNumberReceived);
      20 must equal(srPacket.getReceptionReports().get(1).getSsrc);
      21 must equal(srPacket.getReceptionReports().get(1).getCumulativeNumberOfPacketsLost);
      22 must equal(srPacket.getReceptionReports().get(1).getFractionLost);
      23 must equal(srPacket.getReceptionReports().get(1).getDelaySinceLastSenderReport);
      24 must equal(srPacket.getReceptionReports().get(1).getInterArrivalJitter);
      25 must equal(srPacket.getReceptionReports().get(1).getExtendedHighestSequenceNumberReceived);

      0 must equal(encoded.readableBytes());
    }
  }
}