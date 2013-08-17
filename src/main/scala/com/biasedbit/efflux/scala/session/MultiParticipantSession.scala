package com.biasedbit.efflux.scala.session

import com.biasedbit.efflux.participant.DefaultParticipantDatabase
import com.biasedbit.efflux.participant.ParticipantDatabase
import com.biasedbit.efflux.participant.ParticipantEventListener
import com.biasedbit.efflux.participant.RtpParticipant
import org.jboss.netty.handler.execution.OrderedMemoryAwareThreadPoolExecutor
import org.jboss.netty.util.HashedWheelTimer
import java.util.Collection
//remove if not needed
import scala.collection.JavaConversions._

/**
 * A regular RTP session, as described in RFC3550.
 *
 * Unlike {@link SingleParticipantSession}, this session starts off with 0 remote participants.
 *
 * @author <a:mailto="bruno.carvalho@wit-software.com" />Bruno de Carvalho</a>
 */
class MultiParticipantSession(id: String, payloadType: Int, localParticipant: RtpParticipant)
    extends AbstractRtpSession(id, payloadType, localParticipant, null, null) with ParticipantEventListener {

  def this(id: String, 
      payloadType: Int, 
      localParticipant: RtpParticipant, 
      timer: HashedWheelTimer) {
    super(id, payloadType, localParticipant, timer, null)
  }

  def this(id: String, 
      payloadType: Int, 
      localParticipant: RtpParticipant, 
      executor: OrderedMemoryAwareThreadPoolExecutor) {
    super(id, payloadType, localParticipant, null, executor)
  }

  def this(id: String, 
      payloadType: Int, 
      localParticipant: RtpParticipant, 
      timer: HashedWheelTimer, 
      executor: OrderedMemoryAwareThreadPoolExecutor) {
    super(id, payloadType, localParticipant, timer, executor)
  }

  def this(id: String, 
      payloadTypes: Collection[Integer], 
      localParticipant: RtpParticipant, 
      timer: HashedWheelTimer, 
      executor: OrderedMemoryAwareThreadPoolExecutor) {
    super(id, payloadTypes, localParticipant, timer, executor)
  }

  protected override def createDatabase(): ParticipantDatabase = {
    new DefaultParticipantDatabase(this.id, this)
  }

  override def participantCreatedFromSdesChunk(participant: RtpParticipant) {
    for (listener <- this.eventListeners) {
      listener.participantJoinedFromControl(this, participant)
    }
  }

  override def participantCreatedFromDataPacket(participant: RtpParticipant) {
    for (listener <- this.eventListeners) {
      listener.participantJoinedFromData(this, participant)
    }
  }

  override def participantDeleted(participant: RtpParticipant) {
    for (listener <- this.eventListeners) {
      listener.participantDeleted(this, participant)
    }
  }
}
