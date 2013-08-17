package com.biasedbit.efflux.scala.session

import com.biasedbit.efflux.logging.Logger
import com.biasedbit.efflux.network.ControlHandler
import com.biasedbit.efflux.network.ControlPacketDecoder
import com.biasedbit.efflux.network.ControlPacketEncoder
import com.biasedbit.efflux.network.DataHandler
import com.biasedbit.efflux.network.DataPacketDecoder
import com.biasedbit.efflux.network.DataPacketEncoder
import com.biasedbit.efflux.packet.AbstractReportPacket
import com.biasedbit.efflux.packet.AppDataPacket
import com.biasedbit.efflux.packet.ByePacket
import com.biasedbit.efflux.packet.CompoundControlPacket
import com.biasedbit.efflux.packet.ControlPacket
import com.biasedbit.efflux.packet.DataPacket
import com.biasedbit.efflux.packet.ReceiverReportPacket
import com.biasedbit.efflux.packet.ReceptionReport
import com.biasedbit.efflux.packet.SdesChunk
import com.biasedbit.efflux.packet.SdesChunkItems
import com.biasedbit.efflux.packet.SenderReportPacket
import com.biasedbit.efflux.packet.SourceDescriptionPacket
import com.biasedbit.efflux.participant.ParticipantDatabase
import com.biasedbit.efflux.participant.ParticipantOperation
import com.biasedbit.efflux.participant.RtpParticipant
import com.biasedbit.efflux.participant.RtpParticipantInfo
import org.jboss.netty.bootstrap.ConnectionlessBootstrap
import org.jboss.netty.channel.ChannelPipeline
import org.jboss.netty.channel.ChannelPipelineFactory
import org.jboss.netty.channel.Channels
import org.jboss.netty.channel.FixedReceiveBufferSizePredictorFactory
import org.jboss.netty.channel.socket.DatagramChannel
import org.jboss.netty.channel.socket.DatagramChannelFactory
import org.jboss.netty.channel.socket.nio.NioDatagramChannelFactory
import org.jboss.netty.channel.socket.oio.OioDatagramChannelFactory
import org.jboss.netty.handler.execution.ExecutionHandler
import org.jboss.netty.handler.execution.OrderedMemoryAwareThreadPoolExecutor
import org.jboss.netty.util.HashedWheelTimer
import org.jboss.netty.util.Timeout
import org.jboss.netty.util.TimerTask
import java.net.SocketAddress
import java.util.List
import java.util.Map
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong
import java.util.Collections
import java.util.Set
import java.util.HashSet
import java.util.Collection
import AbstractRtpSession._
//remove if not needed
import scala.collection.JavaConversions._

object AbstractRtpSession {

  protected val LOG = Logger.getLogger(classOf[AbstractRtpSession])

  protected val VERSION = "efflux_0.4_15092010"

  protected val USE_NIO = true

  protected val DISCARD_OUT_OF_ORDER = true

  protected val BANDWIDTH_LIMIT = 256

  protected val SEND_BUFFER_SIZE = 1500

  protected val RECEIVE_BUFFER_SIZE = 1500

  protected val MAX_COLLISIONS_BEFORE_CONSIDERING_LOOP = 3

  protected val AUTOMATED_RTCP_HANDLING = true

  protected val TRY_TO_UPDATE_ON_EVERY_SDES = true

  protected val PARTICIPANT_DATABASE_CLEANUP = 10
}

/**
 * @author <a:mailto="bruno.carvalho@wit-software.com" />Bruno de Carvalho</a>
 */
abstract class AbstractRtpSession(protected val id: String, 
    payloadTypes: Collection[Integer], 
    local: RtpParticipant, 
    timer: HashedWheelTimer, 
    protected val executor: OrderedMemoryAwareThreadPoolExecutor) extends RtpSession with TimerTask {

  protected val payloadTypes = new HashSet[Integer]()

  protected val timer: HashedWheelTimer = _

  protected var host: String = _

  protected var useNio: Boolean = USE_NIO

  protected var discardOutOfOrder: Boolean = DISCARD_OUT_OF_ORDER

  protected var bandwidthLimit: Int = BANDWIDTH_LIMIT

  protected var sendBufferSize: Int = SEND_BUFFER_SIZE

  protected var receiveBufferSize: Int = RECEIVE_BUFFER_SIZE

  protected var maxCollisionsBeforeConsideringLoop: Int = MAX_COLLISIONS_BEFORE_CONSIDERING_LOOP

  protected var automatedRtcpHandling: Boolean = AUTOMATED_RTCP_HANDLING

  protected var tryToUpdateOnEverySdes: Boolean = TRY_TO_UPDATE_ON_EVERY_SDES

  protected var participantDatabaseCleanup: Int = PARTICIPANT_DATABASE_CLEANUP

  protected val running = new AtomicBoolean(false)

  protected val localParticipant = local

  protected val participantDatabase = this.createDatabase()

  protected val dataListeners = new CopyOnWriteArrayList[RtpSessionDataListener]()

  protected val controlListeners = new CopyOnWriteArrayList[RtpSessionControlListener]()

  protected val eventListeners = new CopyOnWriteArrayList[RtpSessionEventListener]()

  protected var dataBootstrap: ConnectionlessBootstrap = _

  protected var controlBootstrap: ConnectionlessBootstrap = _

  protected var dataChannel: DatagramChannel = _

  protected var controlChannel: DatagramChannel = _

  protected val sequence = new AtomicInteger(0)

  protected val sentOrReceivedPackets = new AtomicBoolean(false)

  protected val collisions = new AtomicInteger(0)

  protected val sentByteCounter = new AtomicLong(0)

  protected val sentPacketCounter = new AtomicLong(0)

  protected var periodicRtcpSendInterval: Int = _

  protected val internalTimer: Boolean = _

  for (payloadType <- payloadTypes if (payloadType < 0) || (payloadType > 127)) {
    throw new IllegalArgumentException("PayloadTypes must be in range [0;127]")
  }

  if (!local.isReceiver) {
    throw new IllegalArgumentException("Local participant must have its data & control addresses set")
  }

  this.payloadTypes.addAll(payloadTypes)

  if (timer == null) {
    this.timer = new HashedWheelTimer(1, TimeUnit.SECONDS)
    this.internalTimer = true
  } else {
    this.timer = timer
    this.internalTimer = false
  }

  def this(id: String, payloadType: Int, local: RtpParticipant) {
    this(id, payloadType, local, null, null)
  }

  def this(id: String, 
      payloadType: Int, 
      local: RtpParticipant, 
      timer: HashedWheelTimer) {
    this(id, payloadType, local, timer, null)
  }

  def this(id: String, 
      payloadType: Int, 
      local: RtpParticipant, 
      executor: OrderedMemoryAwareThreadPoolExecutor) {
    this(id, payloadType, local, null, executor)
  }

  def this(id: String, 
      payloadType: Int, 
      local: RtpParticipant, 
      timer: HashedWheelTimer, 
      executor: OrderedMemoryAwareThreadPoolExecutor) {
    this(id, Collections.singleton(payloadType), local, timer, executor)
  }

  override def getId(): String = id

  override def getPayloadType(): Set[Integer] = this.payloadTypes

  override def init(): Boolean = {
    synchronized {
      if (this.running.get) {
        return true
      }
      var factory: DatagramChannelFactory = null
      factory = if (this.useNio) new OioDatagramChannelFactory(Executors.newCachedThreadPool()) else new NioDatagramChannelFactory(Executors.newCachedThreadPool())
      this.dataBootstrap = new ConnectionlessBootstrap(factory)
      this.dataBootstrap.setOption("sendBufferSize", this.sendBufferSize)
      this.dataBootstrap.setOption("receiveBufferSize", this.receiveBufferSize)
      this.dataBootstrap.setOption("receiveBufferSizePredictorFactory", new FixedReceiveBufferSizePredictorFactory(this.receiveBufferSize))
      this.dataBootstrap.setPipelineFactory(new ChannelPipelineFactory() {

        def getPipeline(): ChannelPipeline = {
          val pipeline = Channels.pipeline()
          pipeline.addLast("decoder", new DataPacketDecoder())
          pipeline.addLast("encoder", DataPacketEncoder.getInstance)
          if (executor != null) {
            pipeline.addLast("executorHandler", new ExecutionHandler(executor))
          }
          pipeline.addLast("handler", new DataHandler(AbstractRtpSession.this))
          return pipeline
        }
      })
      this.controlBootstrap = new ConnectionlessBootstrap(factory)
      this.controlBootstrap.setOption("sendBufferSize", this.sendBufferSize)
      this.controlBootstrap.setOption("receiveBufferSize", this.receiveBufferSize)
      this.controlBootstrap.setOption("receiveBufferSizePredictorFactory", new FixedReceiveBufferSizePredictorFactory(this.receiveBufferSize))
      this.controlBootstrap.setPipelineFactory(new ChannelPipelineFactory() {

        def getPipeline(): ChannelPipeline = {
          val pipeline = Channels.pipeline()
          pipeline.addLast("decoder", new ControlPacketDecoder())
          pipeline.addLast("encoder", ControlPacketEncoder.getInstance)
          if (executor != null) {
            pipeline.addLast("executorHandler", new ExecutionHandler(executor))
          }
          pipeline.addLast("handler", new ControlHandler(AbstractRtpSession.this))
          return pipeline
        }
      })
      val dataAddress = this.localParticipant.getDataDestination
      val controlAddress = this.localParticipant.getControlDestination
      try {
        this.dataChannel = this.dataBootstrap.bind(dataAddress).asInstanceOf[DatagramChannel]
      } catch {
        case e: Exception => {
          LOG.error("Failed to bind data channel for session with id " + this.id, e)
          this.dataBootstrap.releaseExternalResources()
          this.controlBootstrap.releaseExternalResources()
          return false
        }
      }
      try {
        this.controlChannel = this.controlBootstrap.bind(controlAddress).asInstanceOf[DatagramChannel]
      } catch {
        case e: Exception => {
          LOG.error("Failed to bind control channel for session with id " + 
            this.id, e)
          this.dataChannel.close()
          this.dataBootstrap.releaseExternalResources()
          this.controlBootstrap.releaseExternalResources()
          return false
        }
      }
      LOG.debug("Data & Control channels bound for RtpSession with id {}.", this.id)
      this.joinSession(this.localParticipant.getSsrc)
      this.running.set(true)
      this.timer.newTimeout(new TimerTask() {

        override def run(timeout: Timeout) {
          if (!running.get) {
            return
          }
          participantDatabase.cleanup()
          timer.newTimeout(this, participantDatabaseCleanup, TimeUnit.SECONDS)
        }
      }, this.participantDatabaseCleanup, TimeUnit.SECONDS)
      if (this.automatedRtcpHandling) {
        this.timer.newTimeout(this, this.updatePeriodicRtcpSendInterval(), TimeUnit.SECONDS)
      }
      if (this.internalTimer) {
        this.timer.start()
      }
      true
    }
  }

  override def terminate() {
    this.terminate(RtpSessionEventListener.TERMINATE_CALLED)
  }

  override def sendData(data: Array[Byte], timestamp: Long, marked: Boolean): Boolean = {
    if (!this.running.get) {
      return false
    }
    val packet = new DataPacket()
    packet.setTimestamp(timestamp)
    packet.setData(data)
    packet.setMarker(marked)
    this.sendDataPacket(packet)
  }

  override def sendDataPacket(packet: DataPacket): Boolean = {
    if (!this.running.get) {
      return false
    }
    if (!this.payloadTypes.contains(packet.getPayloadType) && this.payloadTypes.size == 1) {
      packet.setPayloadType(this.payloadTypes.iterator().next())
    }
    packet.setSsrc(this.localParticipant.getSsrc)
    packet.setSequenceNumber(this.sequence.incrementAndGet())
    this.internalSendData(packet)
    true
  }

  override def sendControlPacket(packet: ControlPacket): Boolean = {
    if (!this.running.get) {
      return false
    }
    if (ControlPacket.Type.APP_DATA == packet.getType || !this.automatedRtcpHandling) {
      this.internalSendControl(packet)
      return true
    }
    false
  }

  override def sendControlPacket(packet: CompoundControlPacket): Boolean = {
    if (this.running.get && !this.automatedRtcpHandling) {
      this.internalSendControl(packet)
      return true
    }
    false
  }

  override def getLocalParticipant(): RtpParticipant = this.localParticipant

  override def addReceiver(remoteParticipant: RtpParticipant): Boolean = {
    (remoteParticipant.getSsrc != this.localParticipant.getSsrc) && 
      this.participantDatabase.addReceiver(remoteParticipant)
  }

  override def removeReceiver(remoteParticipant: RtpParticipant): Boolean = {
    this.participantDatabase.removeReceiver(remoteParticipant)
  }

  override def getRemoteParticipant(ssrc: Long): RtpParticipant = {
    this.participantDatabase.getParticipant(ssrc)
  }

  override def getRemoteParticipants(): Map[Long, RtpParticipant] = this.participantDatabase.getMembers

  override def addDataListener(listener: RtpSessionDataListener) {
    this.dataListeners.add(listener)
  }

  override def removeDataListener(listener: RtpSessionDataListener) {
    this.dataListeners.remove(listener)
  }

  override def addControlListener(listener: RtpSessionControlListener) {
    this.controlListeners.add(listener)
  }

  override def removeControlListener(listener: RtpSessionControlListener) {
    this.controlListeners.remove(listener)
  }

  override def addEventListener(listener: RtpSessionEventListener) {
    this.eventListeners.add(listener)
  }

  override def removeEventListener(listener: RtpSessionEventListener) {
    this.eventListeners.remove(listener)
  }

  override def dataPacketReceived(origin: SocketAddress, packet: DataPacket) {
    if (!this.running.get) {
      return
    }
    if (!this.payloadTypes.contains(packet.getPayloadType)) {
      return
    }
    if (packet.getSsrc == this.localParticipant.getSsrc) {
      if (origin == this.localParticipant.getDataDestination) {
        this.terminate(new Throwable("Loop detected: session is directly receiving its own packets"))
        return
      } else if (this.collisions.incrementAndGet() > this.maxCollisionsBeforeConsideringLoop) {
        this.terminate(new Throwable("Loop detected after " + this.collisions.get + " SSRC collisions"))
        return
      }
      val oldSsrc = this.localParticipant.getSsrc
      val newSsrc = this.localParticipant.resolveSsrcConflict(packet.getSsrc)
      if (this.sentOrReceivedPackets.getAndSet(true)) {
        this.leaveSession(oldSsrc, "SSRC collision detected; rejoining with new SSRC.")
        this.joinSession(newSsrc)
      }
      LOG.warn("SSRC collision with remote end detected on session with id {}; updating SSRC from {} to {}.", 
        this.id, oldSsrc, newSsrc)
      for (listener <- this.eventListeners) {
        listener.resolvedSsrcConflict(this, oldSsrc, newSsrc)
      }
    }
    val participant = this.participantDatabase.getOrCreateParticipantFromDataPacket(origin, packet)
    if (participant == null) {
      return
    }
    if ((participant.getLastSequenceNumber >= packet.getSequenceNumber) && 
      this.discardOutOfOrder) {
      LOG.trace("Discarded out of order packet from {} in session with id {} (last SN was {}, packet SN was {}).", 
        participant, this.id, participant.getLastSequenceNumber, packet.getSequenceNumber)
      return
    }
    participant.setLastSequenceNumber(packet.getSequenceNumber)
    participant.setLastDataOrigin(origin)
    for (listener <- this.dataListeners) {
      listener.dataPacketReceived(this, participant.getInfo, packet)
    }
  }

  override def controlPacketReceived(origin: SocketAddress, packet: CompoundControlPacket) {
    if (!this.running.get) {
      return
    }
    if (!this.automatedRtcpHandling) {
      for (listener <- this.controlListeners) {
        listener.controlPacketReceived(this, packet)
      }
      return
    }
    for (controlPacket <- packet.getControlPackets) controlPacket.getType match {
      case SENDER_REPORT | RECEIVER_REPORT => this.handleReportPacket(origin, controlPacket.asInstanceOf[AbstractReportPacket])
      case SOURCE_DESCRIPTION => this.handleSdesPacket(origin, controlPacket.asInstanceOf[SourceDescriptionPacket])
      case BYE => this.handleByePacket(origin, controlPacket.asInstanceOf[ByePacket])
      case APP_DATA => for (listener <- this.controlListeners) {
        listener.appDataReceived(this, controlPacket.asInstanceOf[AppDataPacket])
      }
      case _}
  }

  override def run(timeout: Timeout) {
    if (!this.running.get) {
      return
    }
    val currentSsrc = this.localParticipant.getSsrc
    val sdesPacket = buildSdesPacket(currentSsrc)
    this.participantDatabase.doWithReceivers(new ParticipantOperation() {

      override def doWithParticipant(participant: RtpParticipant) {
        val report = buildReportPacket(currentSsrc, participant)
        internalSendControl(new CompoundControlPacket(report, sdesPacket))
      }
    })
    if (!this.running.get) {
      return
    }
    this.timer.newTimeout(this, this.updatePeriodicRtcpSendInterval(), TimeUnit.SECONDS)
  }

  protected def handleReportPacket(origin: SocketAddress, abstractReportPacket: AbstractReportPacket) {
    if (abstractReportPacket.getReceptionReportCount == 0) {
      return
    }
    val context = this.participantDatabase.getParticipant(abstractReportPacket.getSenderSsrc)
    if (context == null) {
      return
    }
    for (receptionReport <- abstractReportPacket.getReceptionReports if receptionReport.getSsrc == this.localParticipant.getSsrc) {
    }
    if (abstractReportPacket.getType == ControlPacket.Type.SENDER_REPORT) {
      val senderReport = abstractReportPacket.asInstanceOf[SenderReportPacket]
    }
  }

  protected def handleSdesPacket(origin: SocketAddress, packet: SourceDescriptionPacket) {
    for (chunk <- packet.getChunks) {
      val participant = this.participantDatabase.getOrCreateParticipantFromSdesChunk(origin, chunk)
      if (participant == null) {
        return
      }
      if (!participant.hasReceivedSdes() || this.tryToUpdateOnEverySdes) {
        participant.receivedSdes()
        if (participant.getInfo.updateFromSdesChunk(chunk)) {
          for (listener <- this.eventListeners) {
            listener.participantDataUpdated(this, participant)
          }
        }
      }
    }
  }

  protected def handleByePacket(origin: SocketAddress, packet: ByePacket) {
    for (ssrc <- packet.getSsrcList) {
      val participant = this.participantDatabase.getParticipant(ssrc)
      if (participant != null) {
        participant.byeReceived()
        for (listener <- eventListeners) {
          listener.participantLeft(this, participant)
        }
      }
    }
    LOG.trace("Received BYE for participants with SSRCs {} in session with id '{}' (reason: '{}').", 
      packet.getSsrcList, this.id, packet.getReasonForLeaving)
  }

  protected def createDatabase(): ParticipantDatabase

  protected def internalSendData(packet: DataPacket) {
    this.participantDatabase.doWithReceivers(new ParticipantOperation() {

      override def doWithParticipant(participant: RtpParticipant) {
        if (participant.receivedBye()) {
          return
        }
        try {
          writeToData(packet, participant.getDataDestination)
        } catch {
          case e: Exception => LOG.error("Failed to send RTP packet to participants in session with id {}.", 
            id)
        }
      }

      override def toString(): String = {
        "internalSendData() for session with id " + id
      }
    })
  }

  protected def internalSendControl(packet: ControlPacket, participant: RtpParticipant) {
    if (!participant.isReceiver || participant.receivedBye()) {
      return
    }
    try {
      this.writeToControl(packet, participant.getControlDestination)
    } catch {
      case e: Exception => LOG.error("Failed to send RTCP packet to {} in session with id {}.", participant, 
        this.id)
    }
  }

  protected def internalSendControl(packet: CompoundControlPacket, participant: RtpParticipant) {
    if (!participant.isReceiver || participant.receivedBye()) {
      return
    }
    try {
      this.writeToControl(packet, participant.getControlDestination)
    } catch {
      case e: Exception => LOG.error("Failed to send RTCP compound packet to {} in session with id {}.", 
        participant, this.id)
    }
  }

  protected def internalSendControl(packet: ControlPacket) {
    this.participantDatabase.doWithReceivers(new ParticipantOperation() {

      override def doWithParticipant(participant: RtpParticipant) {
        if (participant.receivedBye()) {
          return
        }
        try {
          writeToControl(packet, participant.getControlDestination)
        } catch {
          case e: Exception => LOG.error("Failed to send RTCP packet to participants in session with id {}.", 
            id)
        }
      }

      override def toString(): String = {
        "internalSendControl() for session with id " + id
      }
    })
  }

  protected def internalSendControl(packet: CompoundControlPacket) {
    this.participantDatabase.doWithReceivers(new ParticipantOperation() {

      override def doWithParticipant(participant: RtpParticipant) {
        if (participant.receivedBye()) {
          return
        }
        try {
          writeToControl(packet, participant.getControlDestination)
        } catch {
          case e: Exception => LOG.error("Failed to send RTCP compound packet to participants in session with id {}.", 
            id)
        }
      }

      override def toString(): String = {
        "internalSendControl(CompoundControlPacket) for session with id " + 
          id
      }
    })
  }

  protected def writeToData(packet: DataPacket, destination: SocketAddress) {
    this.dataChannel.write(packet, destination)
  }

  protected def writeToControl(packet: ControlPacket, destination: SocketAddress) {
    this.controlChannel.write(packet, destination)
  }

  protected def writeToControl(packet: CompoundControlPacket, destination: SocketAddress) {
    this.controlChannel.write(packet, destination)
  }

  protected def joinSession(currentSsrc: Long) {
    if (!this.automatedRtcpHandling) {
      return
    }
    val emptyReceiverReport = new ReceiverReportPacket()
    emptyReceiverReport.setSenderSsrc(currentSsrc)
    val sdesPacket = this.buildSdesPacket(currentSsrc)
    val compoundPacket = new CompoundControlPacket(emptyReceiverReport, sdesPacket)
    this.internalSendControl(compoundPacket)
  }

  protected def leaveSession(currentSsrc: Long, motive: String) {
    if (!this.automatedRtcpHandling) {
      return
    }
    val sdesPacket = this.buildSdesPacket(currentSsrc)
    val byePacket = new ByePacket()
    byePacket.addSsrc(currentSsrc)
    byePacket.setReasonForLeaving(motive)
    this.internalSendControl(new CompoundControlPacket(sdesPacket, byePacket))
  }

  protected def buildReportPacket(currentSsrc: Long, context: RtpParticipant): AbstractReportPacket = {
    var packet: AbstractReportPacket = null
    if (this.getSentPackets == 0) {
      packet = new ReceiverReportPacket()
    } else {
      val senderPacket = new SenderReportPacket()
      senderPacket.setNtpTimestamp(0)
      senderPacket.setRtpTimestamp(System.currentTimeMillis())
      senderPacket.setSenderPacketCount(this.getSentPackets)
      senderPacket.setSenderOctetCount(this.getSentBytes)
      packet = senderPacket
    }
    packet.setSenderSsrc(currentSsrc)
    if (context.getReceivedPackets > 0) {
      val block = new ReceptionReport()
      block.setSsrc(context.getInfo.getSsrc)
      block.setDelaySinceLastSenderReport(0)
      block.setFractionLost(0.toShort)
      block.setExtendedHighestSequenceNumberReceived(0)
      block.setInterArrivalJitter(0)
      block.setCumulativeNumberOfPacketsLost(0)
      packet.addReceptionReportBlock(block)
    }
    packet
  }

  protected def buildSdesPacket(currentSsrc: Long): SourceDescriptionPacket = {
    val sdesPacket = new SourceDescriptionPacket()
    val chunk = new SdesChunk(currentSsrc)
    val info = this.localParticipant.getInfo
    if (info.getCname == null) {
      info.setCname(new StringBuilder().append("efflux/").append(this.id)
        .append('@')
        .append(this.dataChannel.getLocalAddress)
        .toString)
    }
    chunk.addItem(SdesChunkItems.createCnameItem(info.getCname))
    if (info.getName != null) {
      chunk.addItem(SdesChunkItems.createNameItem(info.getName))
    }
    if (info.getEmail != null) {
      chunk.addItem(SdesChunkItems.createEmailItem(info.getEmail))
    }
    if (info.getPhone != null) {
      chunk.addItem(SdesChunkItems.createPhoneItem(info.getPhone))
    }
    if (info.getLocation != null) {
      chunk.addItem(SdesChunkItems.createLocationItem(info.getLocation))
    }
    if (info.getTool == null) {
      info.setTool(VERSION)
    }
    chunk.addItem(SdesChunkItems.createToolItem(info.getTool))
    if (info.getNote != null) {
      chunk.addItem(SdesChunkItems.createLocationItem(info.getNote))
    }
    sdesPacket.addItem(chunk)
    sdesPacket
  }

  protected def terminate(cause: Throwable) {
    synchronized {
      if (!this.running.getAndSet(false)) {
        return
      }
      if (this.internalTimer) {
        this.timer.stop()
      }
      this.dataListeners.clear()
      this.controlListeners.clear()
      this.dataChannel.close()
      this.leaveSession(this.localParticipant.getSsrc, "Session terminated.")
      this.controlChannel.close()
      this.dataBootstrap.releaseExternalResources()
      this.controlBootstrap.releaseExternalResources()
      LOG.debug("RtpSession with id {} terminated.", this.id)
      for (listener <- this.eventListeners) {
        listener.sessionTerminated(this, cause)
      }
      this.eventListeners.clear()
    }
  }

  protected def resetSendStats() {
    this.sentByteCounter.set(0)
    this.sentPacketCounter.set(0)
  }

  protected def incrementSentBytes(delta: Int): Long = {
    if (delta < 0) {
      return this.sentByteCounter.get
    }
    this.sentByteCounter.addAndGet(delta)
  }

  protected def incrementSentPackets(): Long = {
    this.sentPacketCounter.incrementAndGet()
  }

  protected def updatePeriodicRtcpSendInterval(): Long = (this.periodicRtcpSendInterval = 5)

  def isRunning(): Boolean = this.running.get

  def getHost(): String = host

  def setHost(host: String) {
    if (this.running.get) {
      throw new IllegalArgumentException("Cannot modify property after initialisation")
    }
    this.host = host
  }

  def useNio(): Boolean = useNio

  def setUseNio(useNio: Boolean) {
    if (this.running.get) {
      throw new IllegalArgumentException("Cannot modify property after initialisation")
    }
    this.useNio = useNio
  }

  def isDiscardOutOfOrder(): Boolean = discardOutOfOrder

  def setDiscardOutOfOrder(discardOutOfOrder: Boolean) {
    if (this.running.get) {
      throw new IllegalArgumentException("Cannot modify property after initialisation")
    }
    this.discardOutOfOrder = discardOutOfOrder
  }

  def getBandwidthLimit(): Int = bandwidthLimit

  def setBandwidthLimit(bandwidthLimit: Int) {
    if (this.running.get) {
      throw new IllegalArgumentException("Cannot modify property after initialisation")
    }
    this.bandwidthLimit = bandwidthLimit
  }

  def getSendBufferSize(): Int = sendBufferSize

  def setSendBufferSize(sendBufferSize: Int) {
    if (this.running.get) {
      throw new IllegalArgumentException("Cannot modify property after initialisation")
    }
    this.sendBufferSize = sendBufferSize
  }

  def getReceiveBufferSize(): Int = receiveBufferSize

  def setReceiveBufferSize(receiveBufferSize: Int) {
    if (this.running.get) {
      throw new IllegalArgumentException("Cannot modify property after initialisation")
    }
    this.receiveBufferSize = receiveBufferSize
  }

  def getMaxCollisionsBeforeConsideringLoop(): Int = maxCollisionsBeforeConsideringLoop

  def setMaxCollisionsBeforeConsideringLoop(maxCollisionsBeforeConsideringLoop: Int) {
    if (this.running.get) {
      throw new IllegalArgumentException("Cannot modify property after initialisation")
    }
    this.maxCollisionsBeforeConsideringLoop = maxCollisionsBeforeConsideringLoop
  }

  def isAutomatedRtcpHandling(): Boolean = automatedRtcpHandling

  def setAutomatedRtcpHandling(automatedRtcpHandling: Boolean) {
    if (this.running.get) {
      throw new IllegalArgumentException("Cannot modify property after initialisation")
    }
    this.automatedRtcpHandling = automatedRtcpHandling
  }

  def isTryToUpdateOnEverySdes(): Boolean = tryToUpdateOnEverySdes

  def setTryToUpdateOnEverySdes(tryToUpdateOnEverySdes: Boolean) {
    if (this.running.get) {
      throw new IllegalArgumentException("Cannot modify property after initialisation")
    }
    this.tryToUpdateOnEverySdes = tryToUpdateOnEverySdes
  }

  def getSentBytes(): Long = this.sentByteCounter.get

  def getSentPackets(): Long = this.sentPacketCounter.get

  def getParticipantDatabaseCleanup(): Int = participantDatabaseCleanup

  def setParticipantDatabaseCleanup(participantDatabaseCleanup: Int) {
    if (this.running.get) {
      throw new IllegalArgumentException("Cannot modify property after initialisation")
    }
    this.participantDatabaseCleanup = participantDatabaseCleanup
  }
}
