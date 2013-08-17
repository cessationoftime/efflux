package com.biasedbit.efflux.scala.participant

import com.biasedbit.efflux.scala.packet.DataPacket
import com.biasedbit.efflux.scala.packet.SdesChunk
import java.net.SocketAddress
import java.util.Collection
import java.util.Map
//remove if not needed
import scala.collection.JavaConversions._

/**
 * @author <a:mailto="bruno.carvalho@wit-software.com" />Bruno de Carvalho</a>
 */
trait ParticipantDatabase {

  def getId(): String

  def getReceivers(): Collection[RtpParticipant]

  def getMembers(): Map[Long, RtpParticipant]

  def doWithReceivers(operation: ParticipantOperation): Unit

  def doWithParticipants(operation: ParticipantOperation): Unit

  def addReceiver(remoteParticipant: RtpParticipant): Boolean

  def removeReceiver(remoteParticipant: RtpParticipant): Boolean

  def getParticipant(ssrc: Long): RtpParticipant

  def getOrCreateParticipantFromDataPacket(origin: SocketAddress, packet: DataPacket): RtpParticipant

  def getOrCreateParticipantFromSdesChunk(origin: SocketAddress, chunk: SdesChunk): RtpParticipant

  def getReceiverCount(): Int

  def getParticipantCount(): Int

  def cleanup(): Unit
}
