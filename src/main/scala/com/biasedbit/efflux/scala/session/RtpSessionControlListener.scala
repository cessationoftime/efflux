package com.biasedbit.efflux.scala.session

import com.biasedbit.efflux.packet.AppDataPacket
import com.biasedbit.efflux.packet.CompoundControlPacket
//remove if not needed
import scala.collection.JavaConversions._

/**
 * @author <a href="http://bruno.biasedbit.com/">Bruno de Carvalho</a>
 */
trait RtpSessionControlListener {

  def controlPacketReceived(session: RtpSession, packet: CompoundControlPacket): Unit

  def appDataReceived(session: RtpSession, appDataPacket: AppDataPacket): Unit
}
