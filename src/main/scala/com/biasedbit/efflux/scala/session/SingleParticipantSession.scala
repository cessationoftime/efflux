package com.biasedbit.efflux.scala.session

import com.biasedbit.efflux.packet.CompoundControlPacket
import com.biasedbit.efflux.packet.ControlPacket
import com.biasedbit.efflux.packet.DataPacket
import com.biasedbit.efflux.participant.ParticipantDatabase
import com.biasedbit.efflux.participant.RtpParticipant
import com.biasedbit.efflux.participant.SingleParticipantDatabase
import org.jboss.netty.handler.execution.OrderedMemoryAwareThreadPoolExecutor
import org.jboss.netty.util.HashedWheelTimer
import java.net.SocketAddress
import java.util.Collections
import java.util.Collection
import java.util.HashMap
import java.util.Map
import java.util.concurrent.atomic.AtomicBoolean
import SingleParticipantSession._
import scala.reflect.{BeanProperty, BooleanBeanProperty}
//remove if not needed
import scala.collection.JavaConversions._

object SingleParticipantSession {

  private val SEND_TO_LAST_ORIGIN = true

  private val IGNORE_FROM_UNKNOWN_SSRC = true
}

/**
 * Implementation that only supports two participants, a local and the remote.
 * <p/>
 * This session is ideal for calls with only two participants in NAT scenarions, where often the IP and ports negociated
 * in the SDP aren't the real ones (due to NAT restrictions and clients not supporting ICE).
 * <p/>
 * If data is received from a source other than the expected one, this session will automatically update the destination
 * IP and newly sent packets will be addressed to that new IP rather than the old one.
 * <p/>
 * If more than one source is used to send data for this session it will often get "confused" and keep redirecting
 * packets to the last source from which it received.
 * <p>
 * This is <strong>NOT</strong> a fully RFC 3550 compliant implementation, but rather a special purpose one for very
 * specific scenarios.
 *
 * @author <a href="http://bruno.biasedbit.com/">Bruno de Carvalho</a>
 */
class SingleParticipantSession(id: String, 
    payloadTypes: Collection[Integer], 
    localParticipant: RtpParticipant, 
    remoteParticipant: RtpParticipant, 
    timer: HashedWheelTimer, 
    executor: OrderedMemoryAwareThreadPoolExecutor) extends AbstractRtpSession(id, payloadTypes, localParticipant, 
  timer, executor) {

  private val receiver = remoteParticipant

  @BooleanBeanProperty
  var sendToLastOrigin: Boolean = SEND_TO_LAST_ORIGIN

  @BooleanBeanProperty
  var ignoreFromUnknownSsrc: Boolean = IGNORE_FROM_UNKNOWN_SSRC

  private val receivedPackets = new AtomicBoolean(false)

  if (!remoteParticipant.isReceiver) {
    throw new IllegalArgumentException("Remote participant must be a receiver (data & control addresses set)")
  }

  this.participantDatabase.asInstanceOf[SingleParticipantDatabase]
    .setParticipant(remoteParticipant)

  def this(id: String, 
      payloadType: Int, 
      localParticipant: RtpParticipant, 
      remoteParticipant: RtpParticipant) {
    this(id, payloadType, localParticipant, remoteParticipant, null, null)
  }

  def this(id: String, 
      payloadType: Int, 
      localParticipant: RtpParticipant, 
      remoteParticipant: RtpParticipant, 
      executor: OrderedMemoryAwareThreadPoolExecutor) {
    this(id, payloadType, localParticipant, remoteParticipant, null, executor)
  }

  def this(id: String, 
      payloadType: Int, 
      localParticipant: RtpParticipant, 
      remoteParticipant: RtpParticipant, 
      timer: HashedWheelTimer) {
    this(id, payloadType, localParticipant, remoteParticipant, timer, null)
  }

  def this(id: String, 
      payloadType: Int, 
      localParticipant: RtpParticipant, 
      remoteParticipant: RtpParticipant, 
      timer: HashedWheelTimer, 
      executor: OrderedMemoryAwareThreadPoolExecutor) {
    this(id, Collections.singleton(payloadType), localParticipant, remoteParticipant, timer, executor)
  }

  override def addReceiver(remoteParticipant: RtpParticipant): Boolean = {
    if (this.receiver == remoteParticipant) {
      return true
    }
    false
  }

  override def removeReceiver(remoteParticipant: RtpParticipant): Boolean = false

  override def getRemoteParticipant(ssrc: Long): RtpParticipant = {
    if (ssrc == this.receiver.getInfo.getSsrc) {
      return this.receiver
    }
    null
  }

  override def getRemoteParticipants(): Map[Long, RtpParticipant] = {
    val map = new HashMap[Long, RtpParticipant]()
    map.put(this.receiver.getSsrc, this.receiver)
    map
  }

  protected override def createDatabase(): ParticipantDatabase = new SingleParticipantDatabase(this.id)

  protected override def internalSendData(packet: DataPacket) {
    try {
      var destination: SocketAddress = null
      destination = if (this.sendToLastOrigin && (this.receiver.getLastDataOrigin != null)) this.receiver.getLastDataOrigin else this.receiver.getDataDestination
      this.writeToData(packet, destination)
      this.sentOrReceivedPackets.set(true)
    } catch {
      case e: Exception => LOG.error("Failed to send {} to {} in session with id {}.", this.id, this.receiver.getInfo)
    }
  }

  protected override def internalSendControl(packet: ControlPacket) {
    try {
      var destination: SocketAddress = null
      destination = if (this.sendToLastOrigin && (this.receiver.getLastControlOrigin != null)) this.receiver.getLastControlOrigin else this.receiver.getControlDestination
      this.writeToControl(packet, destination)
      this.sentOrReceivedPackets.set(true)
    } catch {
      case e: Exception => LOG.error("Failed to send RTCP packet to {} in session with id {}.", this.receiver.getInfo, 
        this.id)
    }
  }

  protected override def internalSendControl(packet: CompoundControlPacket) {
    try {
      this.writeToControl(packet, this.receiver.getControlDestination)
      this.sentOrReceivedPackets.set(true)
    } catch {
      case e: Exception => LOG.error("Failed to send compound RTCP packet to {} in session with id {}.", 
        this.receiver.getInfo, this.id)
    }
  }

  override def dataPacketReceived(origin: SocketAddress, packet: DataPacket) {
    if (!this.receivedPackets.getAndSet(true)) {
      this.receiver.getInfo.setSsrc(packet.getSsrc)
      LOG.trace("First packet received from remote source, updated SSRC to {}.", packet.getSsrc)
    } else if (this.ignoreFromUnknownSsrc && (packet.getSsrc != this.receiver.getInfo.getSsrc)) {
      LOG.trace("Discarded packet from unexpected SSRC: {} (expected was {}).", packet.getSsrc, this.receiver.getInfo.getSsrc)
      return
    }
    super.dataPacketReceived(origin, packet)
  }

  def getRemoteParticipant(): RtpParticipant = this.receiver

  def setSendToLastOrigin(sendToLastOrigin: Boolean) {
    if (this.running.get) {
      throw new IllegalArgumentException("Cannot modify property after initialisation")
    }
    this.sendToLastOrigin = sendToLastOrigin
  }

  def setIgnoreFromUnknownSsrc(ignoreFromUnknownSsrc: Boolean) {
    if (this.running.get) {
      throw new IllegalArgumentException("Cannot modify property after initialisation")
    }
    this.ignoreFromUnknownSsrc = ignoreFromUnknownSsrc
  }
}
