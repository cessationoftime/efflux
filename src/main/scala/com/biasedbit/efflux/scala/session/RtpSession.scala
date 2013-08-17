package com.biasedbit.efflux.scala.session

import com.biasedbit.efflux.scala.network.ControlPacketReceiver
import com.biasedbit.efflux.scala.network.DataPacketReceiver
import com.biasedbit.efflux.scala.packet.CompoundControlPacket
import com.biasedbit.efflux.scala.packet.ControlPacket
import com.biasedbit.efflux.scala.packet.DataPacket
import com.biasedbit.efflux.scala.participant.RtpParticipant
import java.util.Set
import java.util.Map
//remove if not needed
import scala.collection.JavaConversions._

/**
 * @author <a href="http://bruno.biasedbit.com/">Bruno de Carvalho</a>
 */
trait RtpSession extends DataPacketReceiver with ControlPacketReceiver {

  def getId(): String

  def getPayloadType(): Set[Integer]

  def init(): Boolean

  def terminate(): Unit

  def sendData(data: Array[Byte], timestamp: Long, marked: Boolean): Boolean

  def sendDataPacket(packet: DataPacket): Boolean

  def sendControlPacket(packet: ControlPacket): Boolean

  def sendControlPacket(packet: CompoundControlPacket): Boolean

  def getLocalParticipant(): RtpParticipant

  def addReceiver(remoteParticipant: RtpParticipant): Boolean

  def removeReceiver(remoteParticipant: RtpParticipant): Boolean

  def getRemoteParticipant(ssrsc: Long): RtpParticipant

  def getRemoteParticipants(): Map[Long, RtpParticipant]

  def addDataListener(listener: RtpSessionDataListener): Unit

  def removeDataListener(listener: RtpSessionDataListener): Unit

  def addControlListener(listener: RtpSessionControlListener): Unit

  def removeControlListener(listener: RtpSessionControlListener): Unit

  def addEventListener(listener: RtpSessionEventListener): Unit

  def removeEventListener(listener: RtpSessionEventListener): Unit
}
