package com.biasedbit.efflux.scala.participant

import com.biasedbit.efflux.packet.DataPacket
import com.biasedbit.efflux.packet.SdesChunk
import com.biasedbit.efflux.util.TimeUtils
import java.net.InetSocketAddress
import java.net.SocketAddress
import java.util.Collection
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong
import RtpParticipant._
import scala.reflect.{BeanProperty, BooleanBeanProperty}
//remove if not needed
import scala.collection.JavaConversions._

object RtpParticipant {

  private val VALID_PACKETS_UNTIL_VALID_PARTICIPANT = 3

  def createReceiver(host: String, dataPort: Int, controlPort: Int): RtpParticipant = {
    val participant = new RtpParticipant(new RtpParticipantInfo())
    if ((dataPort < 0) || (dataPort > 65536)) {
      throw new IllegalArgumentException("Invalid port number; use range [0;65536]")
    }
    if ((controlPort < 0) || (controlPort > 65536)) {
      throw new IllegalArgumentException("Invalid port number; use range [0;65536]")
    }
    participant.dataDestination = new InetSocketAddress(host, dataPort)
    participant.controlDestination = new InetSocketAddress(host, controlPort)
    participant
  }

  def createReceiver(info: RtpParticipantInfo, 
      host: String, 
      dataPort: Int, 
      controlPort: Int): RtpParticipant = {
    val participant = new RtpParticipant(info)
    if ((dataPort < 0) || (dataPort > 65536)) {
      throw new IllegalArgumentException("Invalid port number; use range [0;65536]")
    }
    if ((controlPort < 0) || (controlPort > 65536)) {
      throw new IllegalArgumentException("Invalid port number; use range [0;65536]")
    }
    participant.dataDestination = new InetSocketAddress(host, dataPort)
    participant.controlDestination = new InetSocketAddress(host, controlPort)
    participant
  }

  def createFromUnexpectedDataPacket(origin: SocketAddress, packet: DataPacket): RtpParticipant = {
    val participant = new RtpParticipant(new RtpParticipantInfo())
    participant.lastDataOrigin = origin
    participant.getInfo.setSsrc(packet.getSsrc)
    participant
  }

  def createFromSdesChunk(origin: SocketAddress, chunk: SdesChunk): RtpParticipant = {
    val participant = new RtpParticipant(new RtpParticipantInfo())
    participant.lastControlOrigin = origin
    participant.getInfo.updateFromSdesChunk(chunk)
    participant.receivedSdes()
    participant
  }
}

/**
 * @author <a:mailto="bruno.carvalho@wit-software.com" />Bruno de Carvalho</a>
 */
class RtpParticipant private (@BeanProperty val info: RtpParticipantInfo) {

  @BeanProperty
  var dataDestination: SocketAddress = _

  @BeanProperty
  var controlDestination: SocketAddress = _

  @BeanProperty
  var lastDataOrigin: SocketAddress = _

  @BeanProperty
  var lastControlOrigin: SocketAddress = _

  @BeanProperty
  var lastReceptionInstant: Long = 0

  @BeanProperty
  var byeReceptionInstant: Long = 0

  @BeanProperty
  var lastSequenceNumber: Int = -1

  private var receivedSdes: Boolean = _

  private val receivedByteCounter = new AtomicLong()

  private val receivedPacketCounter = new AtomicLong()

  private val validPacketCounter = new AtomicInteger()

  def resolveSsrcConflict(ssrcToAvoid: Long): Long = {
    while (this.getSsrc == ssrcToAvoid) {
      this.getInfo.setSsrc(RtpParticipantInfo.generateNewSsrc())
    }
    this.getSsrc
  }

  def resolveSsrcConflict(ssrcsToAvoid: Collection[Long]): Long = {
    while (ssrcsToAvoid.contains(this.getSsrc)) {
      this.getInfo.setSsrc(RtpParticipantInfo.generateNewSsrc())
    }
    this.getSsrc
  }

  def byeReceived() {
    this.byeReceptionInstant = TimeUtils.now()
  }

  def receivedSdes() {
    this.receivedSdes = true
  }

  def packetReceived() {
    this.lastReceptionInstant = TimeUtils.now()
  }

  def isReceiver(): Boolean = {
    (this.dataDestination != null) && (this.controlDestination != null)
  }

  def getSsrc(): Long = this.getInfo.getSsrc

  def receivedBye(): Boolean = this.byeReceptionInstant > 0

  def getReceivedPackets(): Long = this.receivedPacketCounter.get

  def getReceivedBytes(): Long = this.receivedByteCounter.get

  def hasReceivedSdes(): Boolean = receivedSdes

  def setDataDestination(dataDestination: SocketAddress) {
    if (dataDestination == null) {
      throw new IllegalArgumentException("Argument cannot be null")
    }
    this.dataDestination = dataDestination
  }

  def setControlDestination(controlDestination: SocketAddress) {
    if (dataDestination == null) {
      throw new IllegalArgumentException("Argument cannot be null")
    }
    this.controlDestination = controlDestination
  }

  override def equals(o: Any): Boolean = {
    if (this == o) {
      return true
    }
    if (!(o.isInstanceOf[RtpParticipant])) {
      return false
    }
    val that = o.asInstanceOf[RtpParticipant]
    this.controlDestination == that.controlDestination && this.dataDestination == that.dataDestination && 
      this.info.getCname == that.info.getCname
  }

  override def hashCode(): Int = {
    var result = dataDestination.hashCode
    result = 31 * result + controlDestination.hashCode
    result
  }

  override def toString(): String = this.getInfo.toString
}
