package com.biasedbit.efflux.scala.participant

import com.biasedbit.efflux.scala.logging.Logger
import com.biasedbit.efflux.scala.packet.DataPacket
import com.biasedbit.efflux.scala.packet.SdesChunk
import java.net.SocketAddress
import java.util.Arrays
import java.util.Collection
import java.util.HashMap
import java.util.Map
import SingleParticipantDatabase._
import scala.reflect.{ BeanProperty, BooleanBeanProperty }
//remove if not needed
import scala.collection.JavaConversions._

object SingleParticipantDatabase {

  private val LOG = Logger.getLogger(classOf[SingleParticipantDatabase])
}

/**
 * @author <a:mailto="bruno.carvalho@wit-software.com" />Bruno de Carvalho</a>
 */
class SingleParticipantDatabase(@BeanProperty var id: String) extends ParticipantDatabase {

  private var participant: RtpParticipant = _

  override def getReceivers(): Collection[RtpParticipant] = Arrays.asList(this.participant)

  override def getMembers(): Map[Long, RtpParticipant] = {
    val map = new HashMap[Long, RtpParticipant](1)
    map.put(this.participant.getSsrc, this.participant)
    map
  }

  override def doWithReceivers(operation: ParticipantOperation) {
    try {
      operation.doWithParticipant(this.participant)
    } catch {
      case e: Exception ⇒ LOG.error("Failed to perform operation {} on remote participant {}.", e, operation,
        this.participant)
    }
  }

  override def doWithParticipants(operation: ParticipantOperation) {
    try {
      operation.doWithParticipant(this.participant)
    } catch {
      case e: Exception ⇒ LOG.error("Failed to perform operation {} on remote participant {}.", e, operation,
        this.participant)
    }
  }

  override def addReceiver(remoteParticipant: RtpParticipant): Boolean = remoteParticipant == this.participant

  override def removeReceiver(remoteParticipant: RtpParticipant): Boolean = false

  override def getParticipant(ssrc: Long): RtpParticipant = {
    if (ssrc == this.participant.getSsrc) {
      return this.participant
    }
    null
  }

  override def getOrCreateParticipantFromDataPacket(origin: SocketAddress, packet: DataPacket): RtpParticipant = {
    if (packet.getSsrc == this.participant.getSsrc) {
      return this.participant
    }
    null
  }

  override def getOrCreateParticipantFromSdesChunk(origin: SocketAddress, chunk: SdesChunk): RtpParticipant = {
    if (chunk.getSsrc == this.participant.getSsrc) {
      return this.participant
    }
    null
  }

  override def getReceiverCount(): Int = 1

  override def getParticipantCount(): Int = 1

  override def cleanup() {
  }

  def setParticipant(remoteParticipant: RtpParticipant) {
    this.participant = remoteParticipant
  }
}
