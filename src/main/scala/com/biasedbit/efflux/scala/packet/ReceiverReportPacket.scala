package com.biasedbit.efflux.scala.packet

import org.jboss.netty.buffer.ChannelBuffer
import org.jboss.netty.buffer.ChannelBuffers
import ReceiverReportPacket._
//remove if not needed
import scala.collection.JavaConversions._

object ReceiverReportPacket {

  def decode(buffer: ChannelBuffer, 
      hasPadding: Boolean, 
      innerBlocks: Byte, 
      length: Int): ReceiverReportPacket = {
    val packet = new ReceiverReportPacket()
    packet.setSenderSsrc(buffer.readUnsignedInt())
    var read = 4
    for (i <- 0 until innerBlocks) {
      packet.addReceptionReportBlock(ReceptionReport.decode(buffer))
      read += 24
    }
    val lengthInOctets = (length * 4)
    if (read < lengthInOctets) {
      buffer.skipBytes(lengthInOctets - read)
    }
    packet
  }

  def encode(currentCompoundLength: Int, fixedBlockSize: Int, packet: ReceiverReportPacket): ChannelBuffer = {
    if ((currentCompoundLength < 0) || ((currentCompoundLength % 4) > 0)) {
      throw new IllegalArgumentException("Current compound length must be a non-negative multiple of 4")
    }
    if ((fixedBlockSize < 0) || ((fixedBlockSize % 4) > 0)) {
      throw new IllegalArgumentException("Padding modulus must be a non-negative multiple of 4")
    }
    var size = 4 + 4
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
      b |= 0x20
    }
    b |= packet.getReceptionReportCount
    buffer.writeByte(b)
    buffer.writeByte(packet.`type`.getByte)
    val sizeInOctets = (size / 4) - 1
    buffer.writeShort(sizeInOctets)
    buffer.writeInt(packet.senderSsrc.toInt)
    if (packet.getReceptionReportCount > 0) {
      for (block <- packet.receptionReports) {
        buffer.writeBytes(block.encode())
      }
    }
    if (padding > 0) {
      for (i <- 0 until (padding - 1)) {
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
class ReceiverReportPacket extends AbstractReportPacket {

  override def encode(currentCompoundLength: Int, fixedBlockSize: Int): ChannelBuffer = {
    encode(currentCompoundLength, fixedBlockSize, this)
  }

  override def encode(): ChannelBuffer = encode(0, 0, this)

  override def toString(): String = {
    new StringBuilder().append("ReceiverReportPacket{")
      .append("senderSsrc=")
      .append(this.senderSsrc)
      .append(", receptionReports=")
      .append(this.receptionReports)
      .append('}')
      .toString
  }
}
