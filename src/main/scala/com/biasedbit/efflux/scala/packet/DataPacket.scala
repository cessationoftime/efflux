package com.biasedbit.efflux.scala.packet

import org.jboss.netty.buffer.ChannelBuffer
import org.jboss.netty.buffer.ChannelBuffers
import java.util.ArrayList
import java.util.List
import DataPacket._
import scala.reflect.{ BeanProperty, BooleanBeanProperty }
import scala.collection.JavaConversions._

object DataPacket {

  def decode(data: Array[Byte]): DataPacket = {
    decode(ChannelBuffers.wrappedBuffer(data))
  }

  def decode(buffer: ChannelBuffer): DataPacket = {
    if (buffer.readableBytes() < 12) {
      throw new IllegalArgumentException("A RTP packet must be at least 12 octets long")
    }
    val packet = new DataPacket()
    var b = buffer.readByte()
    packet.version = RtpVersion.fromByte(b)
    val padding = (b & 0x20) > 0
    val extension = (b & 0x10) > 0
    val contributingSourcesCount = b & 0x0f
    b = buffer.readByte()
    packet.marker = (b & 0x80) > 0
    packet.payloadType = (b & 0x7f)
    packet.sequenceNumber = buffer.readUnsignedShort()
    packet.timestamp = buffer.readUnsignedInt()
    packet.ssrc = buffer.readUnsignedInt()
    if (extension) {
      packet.extensionHeaderData = buffer.readShort()
      packet.extensionData = Array.ofDim[Byte](buffer.readUnsignedShort())
      buffer.readBytes(packet.extensionData)
    }
    if (contributingSourcesCount > 0) {
      packet.contributingSourceIds = new ArrayList[Long](contributingSourcesCount)
      for (i ← 0 until contributingSourcesCount) {
        val contributingSource = buffer.readUnsignedInt()
        packet.contributingSourceIds.add(contributingSource)
      }
    }
    if (!padding) {
      val remainingBytes = Array.ofDim[Byte](buffer.readableBytes())
      buffer.readBytes(remainingBytes)
      packet.setData(remainingBytes)
    } else {
      val lastByte = buffer.getUnsignedByte(buffer.readerIndex() + buffer.readableBytes() - 1)
      val dataBytes = Array.ofDim[Byte](buffer.readableBytes() - lastByte)
      buffer.readBytes(dataBytes)
      packet.setData(dataBytes)
      buffer.skipBytes(buffer.readableBytes())
    }
    packet
  }

  def encode(fixedBlockSize: Int, packet: DataPacket): ChannelBuffer = {
    var size = 12
    if (packet.hasExtension()) {
      size += 4 + packet.getExtensionDataSize
    }
    size += packet.getContributingSourcesCount * 4
    size += packet.getDataSize
    var padding = 0
    if (fixedBlockSize > 0) {
      padding = fixedBlockSize - (size % fixedBlockSize)
      if (padding == fixedBlockSize) {
        padding = 0
      }
    }
    size += padding
    val buffer = ChannelBuffers.buffer(size)
    var b = packet.getVersion.getByte
    if (padding > 0) {
      b |= 0x20
    }
    if (packet.hasExtension()) {
      b |= 0x10
    }
    b |= packet.getContributingSourcesCount
    buffer.writeByte(b)
    b = packet.getPayloadType.toByte
    if (packet.hasMarker()) {
      b |= 0x80
    }
    buffer.writeByte(b)
    buffer.writeShort(packet.sequenceNumber)
    buffer.writeInt(packet.timestamp.toInt)
    buffer.writeInt(packet.ssrc.toInt)
    if (packet.hasExtension()) {
      buffer.writeShort(packet.extensionHeaderData)
      buffer.writeShort(packet.extensionData.length)
      buffer.writeBytes(packet.extensionData)
    }
    if (packet.getContributingSourcesCount > 0) {
      for (contributingSourceId ← packet.getContributingSourceIds) {
        buffer.writeInt(contributingSourceId.intValue())
      }
    }
    if (packet.data != null) {
      buffer.writeBytes(packet.data.array())
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
 *  0                   1                   2                   3
 *  0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1
 * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 * |V=2|P|X|  CC   |M|     PT      |       sequence number         |
 * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 * |                           timestamp                           |
 * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 * |           synchronization source (SSRC) identifier            |
 * +=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+
 * |            contributing source (CSRC) identifiers             |
 * |                             ....                              |
 * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 *
 *  0                   1                   2                   3
 *  0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1
 * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 * |      defined by profile       |           length              |
 * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 * |                        header extension                       |
 * |                             ....                              |
 *
 * @author <a href="http://bruno.biasedbit.com/">Bruno de Carvalho</a>
 */
class DataPacket {

  @BeanProperty
  var version: RtpVersion = RtpVersion.V2

  private var marker: Boolean = _

  @BeanProperty
  var payloadType: Int = _

  @BeanProperty
  var sequenceNumber: Int = _

  @BeanProperty
  var timestamp: Long = _

  @BeanProperty
  var ssrc: Long = _

  @BeanProperty
  var extensionHeaderData: Short = _

  @BeanProperty
  var extensionData: Array[Byte] = _

  @BeanProperty
  var contributingSourceIds: List[Long] = _

  @BeanProperty
  var data: ChannelBuffer = _

  def encode(fixedBlockSize: Int): ChannelBuffer = DataPacket.encode(fixedBlockSize, this)

  def encode(): ChannelBuffer = DataPacket.encode(0, this)

  def addContributingSourceId(contributingSourceId: Long) {
    if (this.contributingSourceIds == null) {
      this.contributingSourceIds = new ArrayList[Long]()
    }
    this.contributingSourceIds.add(contributingSourceId)
  }

  def getDataSize(): Int = {
    if (this.data == null) {
      return 0
    }
    this.data.capacity()
  }

  def getExtensionDataSize(): Int = {
    if (this.extensionData == null) {
      return 0
    }
    this.extensionData.length
  }

  def getContributingSourcesCount(): Int = {
    if (this.contributingSourceIds == null) {
      return 0
    }
    this.contributingSourceIds.size
  }

  def setExtensionHeader(extensionHeaderData: Short, extensionData: Array[Byte]) {
    if (extensionData.length > 65536) {
      throw new IllegalArgumentException("Extension data cannot exceed 65536 bytes")
    }
    this.extensionHeaderData = extensionHeaderData
    this.extensionData = extensionData
  }

  def setVersion(version: RtpVersion) {
    if (version != RtpVersion.V2) {
      throw new IllegalArgumentException("Only V2 is supported")
    }
    this.version = version
  }

  def hasExtension(): Boolean = this.extensionData != null

  def hasMarker(): Boolean = marker

  def setMarker(marker: Boolean) {
    this.marker = marker
  }

  def setPayloadType(payloadType: Int) {
    if ((payloadType < 0) || (payloadType > 127)) {
      throw new IllegalArgumentException("PayloadType must be in range [0;127]")
    }
    this.payloadType = payloadType
  }

  def setSsrc(ssrc: Long) {
    if ((ssrc < 0) || (ssrc > 0xffffffffL)) {
      throw new IllegalArgumentException("Valid range for SSRC is [0;0xffffffff]")
    }
    this.ssrc = ssrc
  }

  def setData(data: ChannelBuffer) {
    this.data = data
  }

  def getDataAsArray(): Array[Byte] = this.data.array()

  override def toString(): String = {
    new StringBuilder().append("DataPacket{V=").append(this.version)
      .append(", X=")
      .append(this.hasExtension())
      .append(", CC=")
      .append(this.getContributingSourcesCount)
      .append(", M=")
      .append(this.marker)
      .append(", PT=")
      .append(this.payloadType)
      .append(", SN=")
      .append(this.sequenceNumber)
      .append(", TS=")
      .append(this.timestamp)
      .append(", SSRC=")
      .append(this.ssrc)
      .append(", CSRCs=")
      .append(this.contributingSourceIds)
      .append(", data=")
      .append(this.getDataSize)
      .append(" bytes}")
      .toString
  }
}
