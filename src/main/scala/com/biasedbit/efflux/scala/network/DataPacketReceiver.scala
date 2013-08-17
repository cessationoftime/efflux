package com.biasedbit.efflux.scala.network

import com.biasedbit.efflux.scala.packet.DataPacket
import java.net.SocketAddress
//remove if not needed
import scala.collection.JavaConversions._

/**
 * @author <a href="http://bruno.biasedbit.com/">Bruno de Carvalho</a>
 */
trait DataPacketReceiver {

  def dataPacketReceived(origin: SocketAddress, packet: DataPacket): Unit
}
