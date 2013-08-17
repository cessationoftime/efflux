package com.biasedbit.efflux.scala.network

import java.net.SocketAddress
import scala.collection.JavaConversions._
import com.biasedbit.efflux.scala.packet.CompoundControlPacket

/**
 * @author <a href="http://bruno.biasedbit.com/">Bruno de Carvalho</a>
 */
trait ControlPacketReceiver {

  def controlPacketReceived(origin: SocketAddress, packet: CompoundControlPacket): Unit
}
