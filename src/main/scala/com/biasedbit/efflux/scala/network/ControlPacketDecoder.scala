package com.biasedbit.efflux.scala.network

import com.biasedbit.efflux.scala.logging.Logger
import com.biasedbit.efflux.scala.packet.CompoundControlPacket
import com.biasedbit.efflux.scala.packet.ControlPacket
import org.jboss.netty.buffer.ChannelBuffer
import org.jboss.netty.channel.ChannelEvent
import org.jboss.netty.channel.ChannelHandlerContext
import org.jboss.netty.channel.ChannelUpstreamHandler
import org.jboss.netty.channel.Channels
import org.jboss.netty.channel.MessageEvent
import java.util.ArrayList
import java.util.List
import ControlPacketDecoder._
import scala.collection.JavaConversions._

object ControlPacketDecoder {

  protected val LOG = Logger.getLogger(classOf[ControlPacketDecoder])
}

/**
 * @author <a href="http://bruno.biasedbit.com/">Bruno de Carvalho</a>
 */
class ControlPacketDecoder extends ChannelUpstreamHandler {

  def handleUpstream(ctx: ChannelHandlerContext, evt: ChannelEvent) {
    if (!(evt.isInstanceOf[MessageEvent])) {
      ctx.sendUpstream(evt)
      return
    }
    val e = evt.asInstanceOf[MessageEvent]
    if (!(e.getMessage.isInstanceOf[ChannelBuffer])) {
      return
    }
    val buffer = e.getMessage.asInstanceOf[ChannelBuffer]
    if ((buffer.readableBytes() % 4) != 0) {
      LOG.debug("Invalid RTCP packet received: total length should be multiple of 4 but is {}", buffer.readableBytes())
      return
    }
    val controlPacketList = new ArrayList[ControlPacket](2)
    while (buffer.readableBytes() > 0) {
      try {
        controlPacketList.add(ControlPacket.decode(buffer))
      } catch {
        case e1: Exception ⇒ LOG.debug("Exception caught while decoding RTCP packet.", e1)
      }
    }
    if (!controlPacketList.isEmpty) {
      Channels.fireMessageReceived(ctx, new CompoundControlPacket(controlPacketList), e.getRemoteAddress)
    }
  }
}
