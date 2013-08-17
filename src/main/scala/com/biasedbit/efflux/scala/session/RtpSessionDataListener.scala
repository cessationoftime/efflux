package com.biasedbit.efflux.scala.session

import com.biasedbit.efflux.scala.packet.DataPacket
import com.biasedbit.efflux.scala.participant.RtpParticipantInfo
//remove if not needed
import scala.collection.JavaConversions._

/**
 * @author <a href="http://bruno.biasedbit.com/">Bruno de Carvalho</a>
 */
trait RtpSessionDataListener {

  def dataPacketReceived(session: RtpSession, participant: RtpParticipantInfo, packet: DataPacket): Unit
}
