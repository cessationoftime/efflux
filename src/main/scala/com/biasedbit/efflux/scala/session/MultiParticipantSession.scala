package com.biasedbit.efflux.scala.session

import com.biasedbit.efflux.scala.participant.DefaultParticipantDatabase
import com.biasedbit.efflux.scala.participant.ParticipantDatabase
import com.biasedbit.efflux.scala.participant.ParticipantEventListener
import com.biasedbit.efflux.scala.participant.RtpParticipant
import org.jboss.netty.handler.execution.OrderedMemoryAwareThreadPoolExecutor
import org.jboss.netty.util.HashedWheelTimer
import java.util.Collection
import scala.collection.JavaConversions._
import java.util.Collections

/**
 * A regular RTP session, as described in RFC3550.
 *
 * Unlike {@link SingleParticipantSession}, this session starts off with 0 remote participants.
 *
 * @author <a:mailto="bruno.carvalho@wit-software.com" />Bruno de Carvalho</a>
 */
class MultiParticipantSession(id: String,
                              payloadTypes: Collection[Int],
                              localParticipant: RtpParticipant,
                              timer: HashedWheelTimer,
                              executor: OrderedMemoryAwareThreadPoolExecutor)
  extends AbstractRtpSession(id, payloadTypes, localParticipant, timer, executor) with ParticipantEventListener {

  def this(id: String,
           payloadType: Int,
           localParticipant: RtpParticipant,
           timer: HashedWheelTimer,
           executor: OrderedMemoryAwareThreadPoolExecutor) = this(id, Collections.singleton(payloadType), localParticipant, timer, executor)

  def this(id: String, payloadType: Int, localParticipant: RtpParticipant) =
    this(id, payloadType, localParticipant, null, null)

  def this(id: String,
           payloadType: Int,
           localParticipant: RtpParticipant,
           timer: HashedWheelTimer) =
    this(id, payloadType, localParticipant, timer, null)

  def this(id: String,
           payloadType: Int,
           localParticipant: RtpParticipant,
           executor: OrderedMemoryAwareThreadPoolExecutor) =
    this(id, payloadType, localParticipant, null, executor)

  protected override def createDatabase(): ParticipantDatabase = {
    new DefaultParticipantDatabase(this.id, this)
  }

  override def participantCreatedFromSdesChunk(participant: RtpParticipant) {
    for (listener ← this.eventListeners) {
      listener.participantJoinedFromControl(this, participant)
    }
  }

  override def participantCreatedFromDataPacket(participant: RtpParticipant) {
    for (listener ← this.eventListeners) {
      listener.participantJoinedFromData(this, participant)
    }
  }

  override def participantDeleted(participant: RtpParticipant) {
    for (listener ← this.eventListeners) {
      listener.participantDeleted(this, participant)
    }
  }
}
