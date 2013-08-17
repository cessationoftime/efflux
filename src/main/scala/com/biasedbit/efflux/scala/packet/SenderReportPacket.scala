package com.biasedbit.efflux.scala.packet

import org.jboss.netty.buffer.ChannelBuffer
import org.jboss.netty.buffer.ChannelBuffers
import SenderReportPacket._
import scala.reflect.{ BeanProperty, BooleanBeanProperty }
//remove if not needed
import scala.collection.JavaConversions._

object SenderReportPacket {

  def decode(buffer: ChannelBuffer,
             hasPadding: Boolean,
             innerBlocks: Byte,
             length: Int): SenderReportPacket = {
    val packet = new SenderReportPacket()
    packet.setSenderSsrc(buffer.readUnsignedInt())
    packet.setNtpTimestamp(buffer.readLong())
    packet.setRtpTimestamp(buffer.readUnsignedInt())
    packet.setSenderPacketCount(buffer.readUnsignedInt())
    packet.setSenderOctetCount(buffer.readUnsignedInt())
    var read = 24
    for (i ← 0 until innerBlocks) {
      packet.addReceptionReportBlock(ReceptionReport.decode(buffer))
      read += 24
    }
    val lengthInOctets = (length * 4)
    if (read < lengthInOctets) {
      buffer.skipBytes(lengthInOctets - read)
    }
    packet
  }

  def encode(currentCompoundLength: Int, fixedBlockSize: Int, packet: SenderReportPacket): ChannelBuffer = {
    if ((currentCompoundLength < 0) || ((currentCompoundLength % 4) > 0)) {
      throw new IllegalArgumentException("Current compound length must be a non-negative multiple of 4")
    }
    if ((fixedBlockSize < 0) || ((fixedBlockSize % 4) > 0)) {
      throw new IllegalArgumentException("Padding modulus must be a non-negative multiple of 4")
    }
    var size = 4 + 24
    var buffer: ChannelBuffer = null
    if (packet.receptionReports != null) {
      size += packet.receptionReports.size * 24
    }
    var padding = 0
    if (fixedBlockSize > 0) {
      padding = fixedBlockSize - ((size + currentCompoundLength) % fixedBlockSize)
      if (padding == fixedBlockSize) {
        padding = 0
      }
    }
    size += padding
    buffer = ChannelBuffers.buffer(size)
    var b = packet.getVersion.getByte
    if (padding > 0) {
      b = (b | 0x20).toByte
    }
    b = (b | packet.getReceptionReportCount).toByte
    buffer.writeByte(b)
    buffer.writeByte(packet.`type`.getByte)
    val sizeInOctets = (size / 4) - 1
    buffer.writeShort(sizeInOctets)
    buffer.writeInt(packet.senderSsrc.toInt)
    buffer.writeLong(packet.ntpTimestamp)
    buffer.writeInt(packet.rtpTimestamp.toInt)
    buffer.writeInt(packet.senderPacketCount.toInt)
    buffer.writeInt(packet.senderOctetCount.toInt)
    if (packet.getReceptionReportCount > 0) {
      for (block ← packet.receptionReports) {
        buffer.writeBytes(block.encode())
      }
    }
    if (padding > 0) {
      for (i ← 0 until (padding - 1)) {
        buffer.writeByte(0x00)
      }
      buffer.writeByte(padding)
    }
    buffer
  }
}

/**
 * @author <a:mailto="bruno.carvalho@wit-software.com" />Bruno de Carvalho</a>
 */
class SenderReportPacket extends AbstractReportPacket(ControlPacket.Type.SENDER_REPORT) {

  @BeanProperty
  var ntpTimestamp: Long = _

  protected var rtpTimestamp: Long = _

  protected var senderPacketCount: Long = _

  protected var senderOctetCount: Long = _

  override def encode(currentCompoundLength: Int, fixedBlockSize: Int): ChannelBuffer = {
    SenderReportPacket.encode(currentCompoundLength, fixedBlockSize, this)
  }

  override def encode(): ChannelBuffer = SenderReportPacket.encode(0, 0, this)

  def getRtpTimestamp = rtpTimestamp

  def setRtpTimestamp(rtpTimestamp: Long) {
    if ((rtpTimestamp < 0) || (rtpTimestamp > 0xffffffffL)) {
      throw new IllegalArgumentException("Valid range for RTP timestamp is [0;0xffffffff]")
    }
    this.rtpTimestamp = rtpTimestamp
  }

  def getSenderPacketCount = senderPacketCount

  def setSenderPacketCount(senderPacketCount: Long) {
    if ((senderPacketCount < 0) || (senderPacketCount > 0xffffffffL)) {
      throw new IllegalArgumentException("Valid range for Sender Packet Count is [0;0xffffffff]")
    }
    this.senderPacketCount = senderPacketCount
  }

  def getSenderOctetCount = senderOctetCount

  def setSenderOctetCount(senderOctetCount: Long) {
    if ((senderOctetCount < 0) || (senderOctetCount > 0xffffffffL)) {
      throw new IllegalArgumentException("Valid range for Sender Octet Count is [0;0xffffffff]")
    }
    this.senderOctetCount = senderOctetCount
  }

  override def toString(): String = {
    new StringBuilder().append("SenderReportPacket{").append("senderSsrc=")
      .append(this.senderSsrc)
      .append(", ntpTimestamp=")
      .append(this.ntpTimestamp)
      .append(", rtpTimestamp=")
      .append(this.rtpTimestamp)
      .append(", senderPacketCount=")
      .append(this.senderPacketCount)
      .append(", senderOctetCount=")
      .append(this.senderOctetCount)
      .append(", receptionReports=")
      .append(this.receptionReports)
      .append('}')
      .toString
  }
}
