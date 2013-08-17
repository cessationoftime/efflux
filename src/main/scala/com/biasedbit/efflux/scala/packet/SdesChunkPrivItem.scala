package com.biasedbit.efflux.scala.packet

import org.jboss.netty.buffer.ChannelBuffer
import org.jboss.netty.buffer.ChannelBuffers
import org.jboss.netty.util.CharsetUtil
import scala.reflect.{ BeanProperty, BooleanBeanProperty }
//remove if not needed
import scala.collection.JavaConversions._

/**
 * @author <a:mailto="bruno.carvalho@wit-software.com" />Bruno de Carvalho</a>
 */
class SdesChunkPrivItem protected (@BeanProperty val prefix: String, value: String)
  extends SdesChunkItem(SdesChunkItem.Type.PRIV, value) {

  override def encode(): ChannelBuffer = {
    var prefixBytes: Array[Byte] = null
    prefixBytes = if (this.prefix != null) this.prefix.getBytes(CharsetUtil.UTF_8) else Array()
    var valueBytes: Array[Byte] = null
    valueBytes = if (this.value != null) this.value.getBytes(CharsetUtil.UTF_8) else Array()
    if ((prefixBytes.length + valueBytes.length) > 254) {
      throw new IllegalArgumentException("Content (prefix + text) can be no longer than 255 bytes and this has " +
        valueBytes.length)
    }
    val buffer = ChannelBuffers.buffer(2 + 1 + prefixBytes.length + valueBytes.length)
    buffer.writeByte(this.`type`.getByte)
    buffer.writeByte(1 + prefixBytes.length + valueBytes.length)
    buffer.writeByte(prefixBytes.length)
    buffer.writeBytes(prefixBytes)
    buffer.writeBytes(valueBytes)
    buffer
  }

  override def toString(): String = {
    new StringBuilder().append("SdesChunkPrivItem{").append("prefix='")
      .append(this.prefix)
      .append('\'')
      .append(", value='")
      .append(this.value)
      .append('\'')
      .append('}')
      .toString
  }
}
