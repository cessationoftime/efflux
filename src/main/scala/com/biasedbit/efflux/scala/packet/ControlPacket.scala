package com.biasedbit.efflux.scala.packet

import org.jboss.netty.buffer.ChannelBuffer
import ControlPacket._
import scala.collection.JavaConversions._

object ControlPacket {
  def decode(buffer: ChannelBuffer): ControlPacket = {
    if ((buffer.readableBytes() % 4) > 0) {
      throw new IllegalArgumentException("Invalid RTCP packet length: expecting multiple of 4 and got " +
        buffer.readableBytes())
    }
    val b = buffer.readByte()
    val version = RtpVersion.fromByte(b)
    if (version != RtpVersion.V2) {
      return null
    }
    val hasPadding = (b & 0x20) > 0
    val innerBlocks = (b & 0x1f).toByte
    val `type` = ControlPacket.Type.fromByte(buffer.readByte())
    val length = buffer.readShort()
    if (length == 0) {
      return null
    }
    `type` match {
      case Type.SENDER_REPORT      ⇒ SenderReportPacket.decode(buffer, hasPadding, innerBlocks, length)
      case Type.RECEIVER_REPORT    ⇒ ReceiverReportPacket.decode(buffer, hasPadding, innerBlocks, length)
      case Type.SOURCE_DESCRIPTION ⇒ SourceDescriptionPacket.decode(buffer, hasPadding, innerBlocks, length)
      case Type.BYE                ⇒ ByePacket.decode(buffer, hasPadding, innerBlocks, length)
      case Type.APP_DATA           ⇒ null
      case _                       ⇒ throw new IllegalArgumentException("Unknown RTCP packet type: " + `type`)
    }
  }

  object Type extends Enumeration {

    val SENDER_REPORT = new Type(0xc8.toByte)

    val RECEIVER_REPORT = new Type(0xc9.toByte)

    val SOURCE_DESCRIPTION = new Type(0xca.toByte)

    val BYE = new Type(0xcb.toByte)

    val APP_DATA = new Type(0xcc.toByte)

    class Type(var b: Byte) extends Val {

      def getByte(): Byte = this.b
    }

    def fromByte(b: Byte): Type = (b.toInt & 0xff) match {
      case 0xc8 ⇒ SENDER_REPORT
      case 0xc9 ⇒ RECEIVER_REPORT
      case 0xca ⇒ SOURCE_DESCRIPTION
      case 0xcb ⇒ BYE
      case 0xcc ⇒ APP_DATA
      case _    ⇒ throw new IllegalArgumentException("Unknown RTCP packet type: " + b)
    }

    implicit def convertValue(v: Value): Type = v.asInstanceOf[Type]
  }
}

/**
 * @author <a href="http://bruno.biasedbit.com/">Bruno de Carvalho</a>
 */
abstract class ControlPacket protected (protected var `type`: ControlPacket.Type.Type) {
  import ControlPacket.Type
  protected var version = RtpVersion.V2

  def encode(currentCompoundLength: Int, fixedBlockSize: Int): ChannelBuffer

  def encode(): ChannelBuffer

  def getVersion(): RtpVersion.RtpVersion = version

  def setVersion(version: RtpVersion.RtpVersion) {
    if (version != RtpVersion.V2) {
      throw new IllegalArgumentException("Only V2 is supported")
    }
    this.version = version
  }

  def getType(): ControlPacket.Type.Type = `type`
}
