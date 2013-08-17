package com.biasedbit.efflux.scala.packet

import org.jboss.netty.buffer.ChannelBuffer
import org.jboss.netty.buffer.ChannelBuffers
import org.jboss.netty.util.CharsetUtil
import SdesChunkItem._
//remove if not needed
import scala.collection.JavaConversions._

object SdesChunkItem {

  object Type extends Enumeration {

    val NULL = new Type(0.toByte)

    val CNAME = new Type(1.toByte)

    val NAME = new Type(2.toByte)

    val EMAIL = new Type(3.toByte)

    val PHONE = new Type(4.toByte)

    val LOCATION = new Type(5.toByte)

    val TOOL = new Type(6.toByte)

    val NOTE = new Type(7.toByte)

    val PRIV = new Type(8.toByte)

    class Type(val b: Byte) extends Val {

      def getByte(): Byte = b
    }

    def fromByte(b: Byte): Type = b match {
      case 0 ⇒ NULL
      case 1 ⇒ CNAME
      case 2 ⇒ NAME
      case 3 ⇒ EMAIL
      case 4 ⇒ PHONE
      case 5 ⇒ LOCATION
      case 6 ⇒ TOOL
      case 7 ⇒ NOTE
      case 8 ⇒ PRIV
      case _ ⇒ throw new IllegalArgumentException("Unknown SSRC Chunk Item type: " + b)
    }

    implicit def convertValue(v: Value): Type = v.asInstanceOf[Type]
  }
}

/**
 * @author <a:mailto="bruno.carvalho@wit-software.com" />Bruno de Carvalho</a>
 */
class SdesChunkItem protected[packet] (protected val `type`: SdesChunkItem.Type.Type, protected val value: String) {

  def encode(): ChannelBuffer = {
    if (this.`type` == Type.NULL) {
      val buffer = ChannelBuffers.buffer(1)
      buffer.writeByte(0x00)
      return buffer
    }
    var valueBytes: Array[Byte] = null
    valueBytes = if (this.value != null) this.value.getBytes(CharsetUtil.UTF_8) else Array()
    if (valueBytes.length > 255) {
      throw new IllegalArgumentException("Content (text) can be no longer than 255 bytes and this has " +
        valueBytes.length)
    }
    val buffer = ChannelBuffers.buffer(2 + valueBytes.length)
    buffer.writeByte(this.`type`.getByte)
    buffer.writeByte(valueBytes.length)
    buffer.writeBytes(valueBytes)
    buffer
  }

  def getType(): SdesChunkItem.Type.Type = `type`

  def getValue(): String = value

  override def toString(): String = {
    new StringBuilder().append("SdesChunkItem{").append("type=")
      .append(this.`type`)
      .append(", value='")
      .append(this.value)
      .append('\'')
      .append('}')
      .toString
  }
}
