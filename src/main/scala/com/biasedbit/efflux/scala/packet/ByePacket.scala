package com.biasedbit.efflux.scala.packet

import org.jboss.netty.buffer.ChannelBuffer
import org.jboss.netty.buffer.ChannelBuffers
import org.jboss.netty.util.CharsetUtil
import java.util.ArrayList
import java.util.Collections
import java.util.List
import ByePacket._
import scala.reflect.{ BeanProperty, BooleanBeanProperty }
//remove if not needed
import scala.collection.JavaConversions._

object ByePacket {

  def decode(buffer: ChannelBuffer,
             hasPadding: Boolean,
             innerBlocks: Byte,
             length: Int): ByePacket = {
    val packet = new ByePacket()
    var read = 0
    for (i ← 0 until innerBlocks) {
      packet.addSsrc(buffer.readUnsignedInt())
      read += 4
    }
    val lengthInOctets = (length * 4)
    if (read < lengthInOctets) {
      val reasonBytes = Array.ofDim[Byte](buffer.readUnsignedByte())
      buffer.readBytes(reasonBytes)
      packet.reasonForLeaving = new String(reasonBytes, CharsetUtil.UTF_8)
      read += (1 + reasonBytes.length)
      if (read < lengthInOctets) {
        buffer.skipBytes(lengthInOctets - read)
      }
    }
    packet
  }

  def encode(currentCompoundLength: Int, fixedBlockSize: Int, packet: ByePacket): ChannelBuffer = {
    if ((currentCompoundLength < 0) || ((currentCompoundLength % 4) > 0)) {
      throw new IllegalArgumentException("Current compound length must be a non-negative multiple of 4")
    }
    if ((fixedBlockSize < 0) || ((fixedBlockSize % 4) > 0)) {
      throw new IllegalArgumentException("Padding modulus must be a non-negative multiple of 4")
    }
    var size = 4
    var buffer: ChannelBuffer = null
    if (packet.ssrcList != null) {
      size += packet.ssrcList.size * 4
    }
    var reasonForLeavingBytes: Array[Byte] = null
    var reasonForLeavingPadding = 0
    if (packet.reasonForLeaving != null) {
      reasonForLeavingBytes = packet.reasonForLeaving.getBytes(CharsetUtil.UTF_8)
      if (reasonForLeavingBytes.length > 255) {
        throw new IllegalArgumentException("Reason for leaving cannot exceed 255 bytes and this has " +
          reasonForLeavingBytes.length)
      }
      size += (1 + reasonForLeavingBytes.length)
      reasonForLeavingPadding = 4 - ((1 + reasonForLeavingBytes.length) % 4)
      if (reasonForLeavingPadding == 4) {
        reasonForLeavingPadding = 0
      }
      if (reasonForLeavingPadding > 0) {
        size += reasonForLeavingPadding
      }
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
    if (packet.ssrcList != null) {
      b = (b | packet.ssrcList.size).toByte
    }
    buffer.writeByte(b)
    buffer.writeByte(packet.`type`.getByte)
    val sizeInOctets = (size / 4) - 1
    buffer.writeShort(sizeInOctets)
    if (packet.ssrcList != null) {
      for (ssrc ← packet.ssrcList) {
        buffer.writeInt(ssrc.intValue())
      }
    }
    if (reasonForLeavingBytes != null) {
      buffer.writeByte(reasonForLeavingBytes.length)
      buffer.writeBytes(reasonForLeavingBytes)
      for (i ← 0 until reasonForLeavingPadding) {
        buffer.writeByte(0x00)
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
class ByePacket extends ControlPacket(ControlPacket.Type.BYE) {

  private var ssrcList: List[Long] = _

  @BeanProperty
  var reasonForLeaving: String = _

  override def encode(currentCompoundLength: Int, fixedBlockSize: Int): ChannelBuffer = {
    ByePacket.encode(currentCompoundLength, fixedBlockSize, this)
  }

  override def encode(): ChannelBuffer = ByePacket.encode(0, 0, this)

  def addSsrc(ssrc: Long): Boolean = {
    if ((ssrc < 0) || (ssrc > 0xffffffffL)) {
      throw new IllegalArgumentException("Valid range for SSRC is [0;0xffffffff]")
    }
    if (this.ssrcList == null) {
      this.ssrcList = new ArrayList[Long]()
    }
    this.ssrcList.add(ssrc)
  }

  def getSsrcList(): List[Long] = {
    Collections.unmodifiableList(this.ssrcList)
  }

  def setSsrcList(ssrcList: List[Long]) {
    this.ssrcList = new ArrayList[Long](ssrcList.size)
    for (ssrc ← ssrcList) {
      this.addSsrc(ssrc)
    }
  }

  override def toString(): String = {
    new StringBuilder().append("ByePacket{").append("ssrcList=")
      .append(this.ssrcList)
      .append(", reasonForLeaving='")
      .append(reasonForLeaving)
      .append('\'')
      .append('}')
      .toString
  }
}
