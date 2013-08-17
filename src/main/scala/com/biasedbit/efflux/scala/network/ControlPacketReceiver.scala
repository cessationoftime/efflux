package com.biasedbit.efflux.scala.network

import com.biasedbit.efflux.packet.CompoundControlPacket
import java.net.SocketAddress
//remove if not needed
import scala.collection.JavaConversions._

/**
 * @author <a href="http://bruno.biasedbit.com/">Bruno de Carvalho</a>
 */
trait ControlPacketReceiver {

  def controlPacketReceived(origin: SocketAddress, packet: CompoundControlPacket): Unit
}
