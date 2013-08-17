package com.biasedbit.efflux.scala.network

import com.biasedbit.efflux.packet.DataPacket
import org.jboss.netty.buffer.ChannelBuffers
import org.jboss.netty.channel.Channel
import org.jboss.netty.channel.ChannelHandler
import org.jboss.netty.channel.ChannelHandlerContext
import org.jboss.netty.handler.codec.oneone.OneToOneEncoder
import DataPacketEncoder._
//remove if not needed
import scala.collection.JavaConversions._

object DataPacketEncoder {

  def getInstance(): DataPacketEncoder = InstanceHolder.INSTANCE

  object InstanceHolder {

    private val INSTANCE = new DataPacketEncoder()
  }
}

/**
 * @author <a href="http://bruno.biasedbit.com/">Bruno de Carvalho</a>
 */
@ChannelHandler.Sharable
class DataPacketEncoder private () extends OneToOneEncoder {

  protected override def encode(ctx: ChannelHandlerContext, channel: Channel, msg: AnyRef): AnyRef = {
    if (!(msg.isInstanceOf[DataPacket])) {
      return ChannelBuffers.EMPTY_BUFFER
    }
    val packet = msg.asInstanceOf[DataPacket]
    if (packet.getDataSize == 0) {
      return ChannelBuffers.EMPTY_BUFFER
    }
    packet.encode()
  }
}
