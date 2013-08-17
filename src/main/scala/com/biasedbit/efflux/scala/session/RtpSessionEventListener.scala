package com.biasedbit.efflux.scala.session

import com.biasedbit.efflux.scala.participant.RtpParticipant
import RtpSessionEventListener._
//remove if not needed
import scala.collection.JavaConversions._

object RtpSessionEventListener {

  val TERMINATE_CALLED = new Throwable("RtpSession.terminate() called")
}

/**
 * @author <a href="http://bruno.biasedbit.com/">Bruno de Carvalho</a>
 */
trait RtpSessionEventListener {

  def participantJoinedFromData(session: RtpSession, participant: RtpParticipant): Unit

  def participantJoinedFromControl(session: RtpSession, participant: RtpParticipant): Unit

  def participantDataUpdated(session: RtpSession, participant: RtpParticipant): Unit

  def participantLeft(session: RtpSession, participant: RtpParticipant): Unit

  def participantDeleted(session: RtpSession, participant: RtpParticipant): Unit

  def resolvedSsrcConflict(session: RtpSession, oldSsrc: Long, newSsrc: Long): Unit

  def sessionTerminated(session: RtpSession, cause: Throwable): Unit
}
