package com.biasedbit.efflux.scala.network

import com.biasedbit.efflux.logging.Logger
import com.biasedbit.efflux.packet.CompoundControlPacket
import org.jboss.netty.channel.ChannelHandlerContext
import org.jboss.netty.channel.ExceptionEvent
import org.jboss.netty.channel.MessageEvent
import org.jboss.netty.channel.SimpleChannelUpstreamHandler
import java.util.concurrent.atomic.AtomicInteger
import ControlHandler._
//remove if not needed
import scala.collection.JavaConversions._

object ControlHandler {

  private val LOG = Logger.getLogger(classOf[ControlHandler])
}

/**
 * @author <a href="http://bruno.biasedbit.com/">Bruno de Carvalho</a>
 */
class ControlHandler(val receiver: ControlPacketReceiver) extends SimpleChannelUpstreamHandler {

  private val counter = new AtomicInteger()

  override def messageReceived(ctx: ChannelHandlerContext, e: MessageEvent) {
    if (e.getMessage.isInstanceOf[CompoundControlPacket]) {
      this.receiver.controlPacketReceived(e.getRemoteAddress, e.getMessage.asInstanceOf[CompoundControlPacket])
    }
  }

  override def exceptionCaught(ctx: ChannelHandlerContext, e: ExceptionEvent) {
    LOG.error("Caught exception on channel {}.", e.getCause, e.getChannel)
  }

  def getPacketsReceived(): Int = this.counter.get
}
