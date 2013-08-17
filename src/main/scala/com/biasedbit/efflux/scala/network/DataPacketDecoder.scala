package com.biasedbit.efflux.scala.network

import com.biasedbit.efflux.scala.logging.Logger
import com.biasedbit.efflux.scala.packet.DataPacket
import org.jboss.netty.buffer.ChannelBuffer
import org.jboss.netty.channel.Channel
import org.jboss.netty.channel.ChannelHandlerContext
import org.jboss.netty.handler.codec.oneone.OneToOneDecoder
import DataPacketDecoder._
//remove if not needed
import scala.collection.JavaConversions._

object DataPacketDecoder {

  protected val LOG = Logger.getLogger(classOf[OneToOneDecoder])
}

/**
 * @author <a href="http://bruno.biasedbit.com/">Bruno de Carvalho</a>
 */
class DataPacketDecoder extends OneToOneDecoder {

  protected override def decode(ctx: ChannelHandlerContext, channel: Channel, msg: AnyRef): AnyRef = {
    if (!(msg.isInstanceOf[ChannelBuffer])) {
      return null
    }
    try {
      DataPacket.decode(msg.asInstanceOf[ChannelBuffer])
    } catch {
      case e: Exception ⇒ {
        LOG.debug("Failed to decode RTP packet.", e)
        null
      }
    }
  }
}
