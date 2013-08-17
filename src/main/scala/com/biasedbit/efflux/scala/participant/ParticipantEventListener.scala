package com.biasedbit.efflux.scala.participant

//remove if not needed
import scala.collection.JavaConversions._

/**
 * @author <a:mailto="bruno.carvalho@wit-software.com" />Bruno de Carvalho</a>
 */
trait ParticipantEventListener {

  def participantCreatedFromSdesChunk(participant: RtpParticipant): Unit

  def participantCreatedFromDataPacket(participant: RtpParticipant): Unit

  def participantDeleted(participant: RtpParticipant): Unit
}
