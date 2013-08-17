package com.biasedbit.efflux.scala.network

import com.biasedbit.efflux.logging.Logger
import com.biasedbit.efflux.packet.DataPacket
import org.jboss.netty.channel.ChannelHandlerContext
import org.jboss.netty.channel.ExceptionEvent
import org.jboss.netty.channel.MessageEvent
import org.jboss.netty.channel.SimpleChannelUpstreamHandler
import java.util.concurrent.atomic.AtomicInteger
import DataHandler._
//remove if not needed
import scala.collection.JavaConversions._

object DataHandler {

  private val LOG = Logger.getLogger(classOf[DataHandler])
}

/**
 * @author <a href="http://bruno.biasedbit.com/">Bruno de Carvalho</a>
 */
class DataHandler(val receiver: DataPacketReceiver) extends SimpleChannelUpstreamHandler {

  private val counter = new AtomicInteger()

  override def messageReceived(ctx: ChannelHandlerContext, e: MessageEvent) {
    if (e.getMessage.isInstanceOf[DataPacket]) {
      this.receiver.dataPacketReceived(e.getRemoteAddress, e.getMessage.asInstanceOf[DataPacket])
    }
  }

  override def exceptionCaught(ctx: ChannelHandlerContext, e: ExceptionEvent) {
    LOG.error("Caught exception on channel {}.", e.getCause, e.getChannel)
  }

  def getPacketsReceived(): Int = this.counter.get
}
