package com.biasedbit.efflux.scala.packet

import org.jboss.netty.buffer.ChannelBuffer
import org.jboss.netty.buffer.ChannelBuffers
import java.util.ArrayList
import java.util.Collections
import java.util.List
import SourceDescriptionPacket._
//remove if not needed
import scala.collection.JavaConversions._

object SourceDescriptionPacket {

  def decode(buffer: ChannelBuffer,
             hasPadding: Boolean,
             innerBlocks: Byte,
             length: Int): SourceDescriptionPacket = {
    val packet = new SourceDescriptionPacket()
    val readable = buffer.readableBytes()
    for (i ← 0 until innerBlocks) {
      packet.addItem(SdesChunk.decode(buffer))
    }
    if (hasPadding) {
      val read = (readable - buffer.readableBytes()) / 4
      buffer.skipBytes((length - read) * 4)
    }
    packet
  }

  def encode(currentCompoundLength: Int, fixedBlockSize: Int, packet: SourceDescriptionPacket): ChannelBuffer = {
    if ((currentCompoundLength < 0) || ((currentCompoundLength % 4) > 0)) {
      throw new IllegalArgumentException("Current compound length must be a non-negative multiple of 4")
    }
    if ((fixedBlockSize < 0) || ((fixedBlockSize % 4) > 0)) {
      throw new IllegalArgumentException("Padding modulus must be a non-negative multiple of 4")
    }
    var size = 4
    var buffer: ChannelBuffer = null
    var encodedChunks: List[ChannelBuffer] = null
    if (packet.chunks != null) {
      encodedChunks = new ArrayList[ChannelBuffer](packet.chunks.size)
      for (chunk ← packet.chunks) {
        val encodedChunk = chunk.encode()
        encodedChunks.add(encodedChunk)
        size += encodedChunk.readableBytes()
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
      b |= 0x20
    }
    if (packet.chunks != null) {
      b |= packet.chunks.size
    }
    buffer.writeByte(b)
    buffer.writeByte(packet.`type`.getByte)
    val sizeInOctets = (size / 4) - 1
    buffer.writeShort(sizeInOctets)
    if (encodedChunks != null) {
      for (encodedChunk ← encodedChunks) {
        buffer.writeBytes(encodedChunk)
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
class SourceDescriptionPacket extends ControlPacket(ControlPacket.Type.SOURCE_DESCRIPTION) {

  private var chunks: List[SdesChunk] = _

  override def encode(currentCompoundLength: Int, fixedBlockSize: Int): ChannelBuffer = {
    SourceDescriptionPacket.encode(currentCompoundLength, fixedBlockSize, this)
  }

  override def encode(): ChannelBuffer = SourceDescriptionPacket.encode(0, 0, this)

  def addItem(chunk: SdesChunk): Boolean = {
    if (this.chunks == null) {
      this.chunks = new ArrayList[SdesChunk]()
      return this.chunks.add(chunk)
    }
    (this.chunks.size < 31) && this.chunks.add(chunk)
  }

  def getChunks(): List[SdesChunk] = {
    if (this.chunks == null) {
      return null
    }
    Collections.unmodifiableList(this.chunks)
  }

  def setChunks(chunks: List[SdesChunk]) {
    if (chunks.size >= 31) {
      throw new IllegalArgumentException("At most 31 SSRC/CSRC chunks can be sent in a SourceDescriptionPacket")
    }
    this.chunks = chunks
  }

  override def toString(): String = {
    new StringBuilder().append("SourceDescriptionPacket{")
      .append("chunks=")
      .append(this.chunks)
      .append('}')
      .toString
  }
}
