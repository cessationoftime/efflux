package com.biasedbit.efflux.scala.participant

//remove if not needed
import scala.collection.JavaConversions._

/**
 * @author <a:mailto="bruno.carvalho@wit-software.com" />Bruno de Carvalho</a>
 */
trait ParticipantOperation {

  def doWithParticipant(participant: RtpParticipant): Unit
}
