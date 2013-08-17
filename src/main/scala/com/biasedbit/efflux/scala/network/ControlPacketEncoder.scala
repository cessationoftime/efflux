package com.biasedbit.efflux.scala.network

import com.biasedbit.efflux.scala.logging.Logger
import com.biasedbit.efflux.scala.packet.CompoundControlPacket
import com.biasedbit.efflux.scala.packet.ControlPacket
import org.jboss.netty.buffer.ChannelBuffer
import org.jboss.netty.buffer.ChannelBuffers
import org.jboss.netty.channel.ChannelDownstreamHandler
import org.jboss.netty.channel.ChannelEvent
import org.jboss.netty.channel.ChannelHandler
import org.jboss.netty.channel.ChannelHandlerContext
import org.jboss.netty.channel.Channels
import org.jboss.netty.channel.MessageEvent
import java.util.List
import ControlPacketEncoder._
//remove if not needed
import scala.collection.JavaConversions._

object ControlPacketEncoder {

  protected val LOG = Logger.getLogger(classOf[ControlPacketEncoder])

  def getInstance(): ControlPacketEncoder = InstanceHolder.INSTANCE

  object InstanceHolder {

    private[ControlPacketEncoder] val INSTANCE = new ControlPacketEncoder()
  }
}

/**
 * @author <a href="http://bruno.biasedbit.com/">Bruno de Carvalho</a>
 */
@ChannelHandler.Sharable
class ControlPacketEncoder private () extends ChannelDownstreamHandler {

  override def handleDownstream(ctx: ChannelHandlerContext, evt: ChannelEvent) {
    if (!(evt.isInstanceOf[MessageEvent])) {
      ctx.sendDownstream(evt)
      return
    }
    val e = evt.asInstanceOf[MessageEvent]
    try {
      if (e.getMessage.isInstanceOf[ControlPacket]) {
        Channels.write(ctx, e.getFuture, e.getMessage.asInstanceOf[ControlPacket].encode(), e.getRemoteAddress)
      } else if (e.getMessage.isInstanceOf[CompoundControlPacket]) {
        val packets = e.getMessage.asInstanceOf[CompoundControlPacket].getControlPackets
        val buffers = Array.ofDim[ChannelBuffer](packets.size)
        for (i ← 0 until buffers.length) {
          buffers(i) = packets.get(i).encode()
        }
        val compoundBuffer = ChannelBuffers.wrappedBuffer(buffers: _*)
        Channels.write(ctx, e.getFuture, compoundBuffer, e.getRemoteAddress)
      }
    } catch {
      case e1: Exception ⇒ LOG.error("Failed to encode compound RTCP packet to send.", e1)
    }
  }
}
