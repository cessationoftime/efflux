package com.biasedbit.efflux.scala.packet

import org.jboss.netty.buffer.ChannelBuffer
import org.jboss.netty.util.CharsetUtil
//remove if not needed
import scala.collection.JavaConversions._

object SdesChunkItems {

  val NULL_ITEM = new SdesChunkItem(SdesChunkItem.Type.NULL, null)

  def createNullItem(): SdesChunkItem = NULL_ITEM

  def createCnameItem(cname: String): SdesChunkItem = {
    new SdesChunkItem(SdesChunkItem.Type.CNAME, cname)
  }

  def createNameItem(name: String): SdesChunkItem = {
    new SdesChunkItem(SdesChunkItem.Type.NAME, name)
  }

  def createEmailItem(email: String): SdesChunkItem = {
    new SdesChunkItem(SdesChunkItem.Type.EMAIL, email)
  }

  def createPhoneItem(phone: String): SdesChunkItem = {
    new SdesChunkItem(SdesChunkItem.Type.PHONE, phone)
  }

  def createLocationItem(location: String): SdesChunkItem = {
    new SdesChunkItem(SdesChunkItem.Type.LOCATION, location)
  }

  def createToolItem(tool: String): SdesChunkItem = {
    new SdesChunkItem(SdesChunkItem.Type.TOOL, tool)
  }

  def createNoteItem(note: String): SdesChunkItem = {
    new SdesChunkItem(SdesChunkItem.Type.NOTE, note)
  }

  def createPrivItem(prefix: String, value: String): SdesChunkPrivItem = new SdesChunkPrivItem(prefix, value)

  def decode(buffer: ChannelBuffer): SdesChunkItem = {
    val `type` = SdesChunkItem.Type.fromByte(buffer.readByte())
    `type` match {
      case NULL ⇒ NULL_ITEM
      case CNAME | NAME | EMAIL | PHONE | LOCATION | TOOL | NOTE ⇒
        var value = Array.ofDim[Byte](buffer.readUnsignedByte())
        buffer.readBytes(value)
        new SdesChunkItem(`type`, new String(value, CharsetUtil.UTF_8))

      case PRIV ⇒
        var valueLength = buffer.readUnsignedByte()
        var prefixLength = buffer.readUnsignedByte()
        value = Array.ofDim[Byte](valueLength - prefixLength - 1)
        var prefix = Array.ofDim[Byte](prefixLength)
        buffer.readBytes(prefix)
        buffer.readBytes(value)
        new SdesChunkPrivItem(new String(prefix, CharsetUtil.UTF_8), new String(value, CharsetUtil.UTF_8))

      case _ ⇒ throw new IllegalArgumentException("Unknown type of SDES chunk: " + `type`)
    }
  }

  def encode(item: SdesChunkItem): ChannelBuffer = item.encode()
}
