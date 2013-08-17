package com.biasedbit.efflux.scala.packet

import org.jboss.netty.buffer.ChannelBuffer
import org.jboss.netty.buffer.ChannelBuffers
import java.util.ArrayList
import java.util.Collections
import java.util.List
import SdesChunk._
import scala.reflect.{BeanProperty, BooleanBeanProperty}
//remove if not needed
import scala.collection.JavaConversions._

object SdesChunk {

  def decode(buffer: ChannelBuffer): SdesChunk = {
    val chunk = new SdesChunk()
    chunk.ssrc = buffer.readUnsignedInt()
    var read = 0
    while (true) {
      if (buffer.readableBytes() == 0) {
        return chunk
      }
      val remaining = buffer.readableBytes()
      val item = SdesChunkItems.decode(buffer)
      read += remaining - buffer.readableBytes()
      if (item.getType == SdesChunkItem.Type.NULL) {
        val paddingBytes = 4 - (read % 4)
        if (paddingBytes != 4) {
          buffer.skipBytes(paddingBytes)
        }
        return chunk
      }
      chunk.addItem(item)
    }
  }

  def encode(chunk: SdesChunk): ChannelBuffer = {
    var buffer: ChannelBuffer = null
    if (chunk.items == null) {
      buffer = ChannelBuffers.buffer(8)
      buffer.writeInt(chunk.ssrc.toInt)
      buffer.writeInt(0)
      return buffer
    } else {
      var size = 4
      val encodedChunkItems = new ArrayList[ChannelBuffer](chunk.items.size)
      for (item <- chunk.items) {
        val encodedChunk = item.encode()
        encodedChunkItems.add(encodedChunk)
        size += encodedChunk.readableBytes()
      }
      size += 1
      var padding = 4 - (size % 4)
      if (padding == 4) {
        padding = 0
      }
      size += padding
      buffer = ChannelBuffers.buffer(size)
      buffer.writeInt(chunk.ssrc.toInt)
      for (encodedChunk <- encodedChunkItems) {
        buffer.writeBytes(encodedChunk)
      }
      buffer.writeByte(0x00)
      for (i <- 0 until padding) {
        buffer.writeByte(0x00)
      }
    }
    buffer
  }
}

/**
 * @author <a:mailto="bruno.carvalho@wit-software.com" />Bruno de Carvalho</a>
 */
class SdesChunk {

  @BeanProperty
  var ssrc: Long = _

  private var items: List[SdesChunkItem] = _

  def this(ssrc: Long) {
    this()
    this.ssrc = ssrc
  }

  def encode(): ChannelBuffer = encode(this)

  def addItem(item: SdesChunkItem): Boolean = {
    if (item.getType == SdesChunkItem.Type.NULL) {
      throw new IllegalArgumentException("You don't need to manually add the null/end element")
    }
    if (this.items == null) {
      this.items = new ArrayList[SdesChunkItem]()
    }
    this.items.add(item)
  }

  def getItemValue(`type`: SdesChunkItem.Type): String = {
    if (this.items == null) {
      return null
    }
    for (item <- this.items if item.getType == `type`) {
      return item.getValue
    }
    null
  }

  def setSsrc(ssrc: Long) {
    if ((ssrc < 0) || (ssrc > 0xffffffffL)) {
      throw new IllegalArgumentException("Valid range for SSRC is [0;0xffffffff]")
    }
    this.ssrc = ssrc
  }

  def getItems(): List[SdesChunkItem] = {
    if (this.items == null) {
      return null
    }
    Collections.unmodifiableList(this.items)
  }

  def setItems(items: List[SdesChunkItem]) {
    this.items = items
  }

  override def toString(): String = {
    new StringBuilder().append("SdesChunk{").append("ssrc=")
      .append(this.ssrc)
      .append(", items=")
      .append(this.items)
      .append('}')
      .toString
  }
}
