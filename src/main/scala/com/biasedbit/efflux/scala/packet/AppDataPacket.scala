package com.biasedbit.efflux.scala.packet

import org.jboss.netty.buffer.ChannelBuffer
import AppDataPacket._
//remove if not needed
import scala.collection.JavaConversions._

object AppDataPacket {

  def encode(currentCompoundLength: Int, fixedBlockSize: Int, packet: AppDataPacket): ChannelBuffer = {
    null
  }
}

/**
 * @author <a:mailto="bruno.carvalho@wit-software.com" />Bruno de Carvalho</a>
 */
class AppDataPacket(`type`: ControlPacket.Type.Type) extends ControlPacket(`type`) {

  override def encode(currentCompoundLength: Int, fixedBlockSize: Int): ChannelBuffer = {
    AppDataPacket.encode(currentCompoundLength, fixedBlockSize, this)
  }

  override def encode(): ChannelBuffer = AppDataPacket.encode(0, 0, this)
}
