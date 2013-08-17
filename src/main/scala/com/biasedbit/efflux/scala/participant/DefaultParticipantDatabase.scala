package com.biasedbit.efflux.scala.participant

import com.biasedbit.efflux.logging.Logger
import com.biasedbit.efflux.scala.packet.DataPacket
import com.biasedbit.efflux.scala.packet.SdesChunk
import com.biasedbit.efflux.scala.packet.SdesChunkItem
import com.biasedbit.efflux.scala.util.TimeUtils
import java.net.SocketAddress
import java.util.ArrayList
import java.util.Collection
import java.util.Collections
import java.util.HashMap
import java.util.Iterator
import java.util.Map
import java.util.concurrent.locks.ReentrantReadWriteLock
import DefaultParticipantDatabase._
import scala.reflect.{ BeanProperty, BooleanBeanProperty }
//remove if not needed
import scala.collection.JavaConversions._

object DefaultParticipantDatabase {

  private val LOG = Logger.getLogger(classOf[DefaultParticipantDatabase])

  private val TIMEOUT_AFTER_NO_PACKETS_RECEIVED = 60

  private val TIMEOUT_AFTER_BYE_AND_NO_PACKETS_RECEIVED = 5
}

/**
 * @author <a href="http://bruno.biasedbit.com/">Bruno de Carvalho</a>
 */
class DefaultParticipantDatabase(@BeanProperty val id: String, eventListener: ParticipantEventListener)
  extends ParticipantDatabase {

  private val listener = eventListener

  @BeanProperty
  var timeoutAfterNoPacketsReceived: Int = TIMEOUT_AFTER_NO_PACKETS_RECEIVED

  @BeanProperty
  var timeoutAfterByeAndNoPacketsReceived: Int = TIMEOUT_AFTER_BYE_AND_NO_PACKETS_RECEIVED

  /**
   * List of unicast receivers. This is a list of explicitly added participants, by the applications using this lib.
   * They might get linked to
   */
  private val receivers = new ArrayList[RtpParticipant]()

  /**
   * List of existing members.
   */
  private val members = new HashMap[Long, RtpParticipant]()

  private val lock = new ReentrantReadWriteLock()

  override def getReceivers(): Collection[RtpParticipant] = {
    Collections.unmodifiableCollection(this.receivers)
  }

  override def getMembers(): Map[Long, RtpParticipant] = {
    Collections.unmodifiableMap(this.members)
  }

  override def doWithReceivers(operation: ParticipantOperation) {
    this.lock.readLock().lock()
    try {
      for (receiver ← this.receivers) {
        try {
          operation.doWithParticipant(receiver)
        } catch {
          case e: Exception ⇒ LOG.error("Failed to perform operation {} on receiver {}.", e, operation,
            receiver)
        }
      }
    } finally {
      this.lock.readLock().unlock()
    }
  }

  override def doWithParticipants(operation: ParticipantOperation) {
    this.lock.readLock().lock()
    try {
      for (member ← this.members.values) {
        try {
          operation.doWithParticipant(member)
        } catch {
          case e: Exception ⇒ LOG.error("Failed to perform operation {} on member {}.", e, operation,
            member)
        }
      }
    } finally {
      this.lock.readLock().unlock()
    }
  }

  override def addReceiver(remoteParticipant: RtpParticipant): Boolean = {
    if (!remoteParticipant.isReceiver) {
      return false
    }
    this.lock.writeLock().lock()
    try {
      var isMember = false
      for (member ← this.members.values) {
        val sameDestinationAddresses = member.getDataDestination == remoteParticipant.getDataDestination &&
          member.getControlDestination == remoteParticipant.getControlDestination
        val sameCname = member.getInfo.getCname == remoteParticipant.getInfo.getCname
        if (sameDestinationAddresses || sameCname) {
          this.receivers.add(member)
          isMember = true
          //break
        }
      }
      isMember || this.receivers.add(remoteParticipant)
    } finally {
      this.lock.writeLock().unlock()
    }
  }

  override def removeReceiver(remoteParticipant: RtpParticipant): Boolean = {
    this.lock.writeLock().lock()
    try {
      this.receivers.remove(remoteParticipant)
    } finally {
      this.lock.writeLock().unlock()
    }
  }

  override def getParticipant(ssrc: Long): RtpParticipant = {
    this.lock.readLock().lock()
    try {
      this.members.get(ssrc)
    } finally {
      this.lock.readLock().unlock()
    }
  }

  override def getOrCreateParticipantFromDataPacket(origin: SocketAddress, packet: DataPacket): RtpParticipant = {
    this.lock.writeLock().lock()
    try {
      var participant = this.members.get(packet.getSsrc)
      if (participant == null) {
        var isReceiver = false
        for (receiver ← this.receivers if receiver.getDataDestination == origin) {
          receiver.getInfo.setSsrc(packet.getSsrc)
          participant = receiver
          participant.setLastDataOrigin(origin)
          isReceiver = true
          //break
        }
        var created = false
        if (!isReceiver) {
          participant = RtpParticipant.createFromUnexpectedDataPacket(origin, packet)
          created = true
        }
        this.members.put(packet.getSsrc, participant)
        if (created) {
          this.listener.participantCreatedFromDataPacket(participant)
        }
      }
      participant
    } finally {
      this.lock.writeLock().unlock()
    }
  }

  override def getOrCreateParticipantFromSdesChunk(origin: SocketAddress, chunk: SdesChunk): RtpParticipant = {
    this.lock.writeLock().lock()
    try {
      var participant = this.members.get(chunk.getSsrc)
      if (participant == null) {
        var isReceiver = false
        for (receiver ← this.receivers) {
          var equalCname = false
          val chunkCname = chunk.getItemValue(SdesChunkItem.Type.CNAME)
          if ((chunkCname != null) && chunkCname == receiver.getInfo.getCname) {
            equalCname = true
          }
          if (receiver.getControlDestination == origin || equalCname) {
            receiver.getInfo.setSsrc(chunk.getSsrc)
            participant = receiver
            participant.setLastControlOrigin(origin)
            participant.receivedSdes()
            participant.getInfo.updateFromSdesChunk(chunk)
            isReceiver = true
            //break
          }
        }
        var created = false
        if (!isReceiver) {
          participant = RtpParticipant.createFromSdesChunk(origin, chunk)
          created = true
        }
        this.members.put(chunk.getSsrc, participant)
        if (created) {
          this.listener.participantCreatedFromSdesChunk(participant)
        }
      }
      participant
    } finally {
      this.lock.writeLock().unlock()
    }
  }

  override def getReceiverCount(): Int = this.receivers.size

  override def getParticipantCount(): Int = this.members.size

  override def cleanup() {
    this.lock.writeLock().lock()
    val now = TimeUtils.now()
    try {
      val iterator = this.members.values.iterator()
      while (iterator.hasNext) {
        val participant = iterator.next()
        val timeout = this.timeoutAfterByeAndNoPacketsReceived * 1000
        if (participant.receivedBye() &&
          TimeUtils.hasExpired(now, participant.getLastReceptionInstant, timeout)) {
          LOG.trace("Removed {} from session with id '{}' after reception of BYE and {}s of inactivity.",
            participant, this.id, this.timeoutAfterByeAndNoPacketsReceived)
          iterator.remove()
          if (participant.isReceiver) {
            this.receivers.remove(participant)
          }
          this.listener.participantDeleted(participant)
        }
      }
    } finally {
      this.lock.writeLock().unlock()
    }
  }
}
