package com.biasedbit.efflux.scala.packet

import org.jboss.netty.buffer.ChannelBuffer
import org.jboss.netty.buffer.ChannelBuffers
import ReceptionReport._
import scala.reflect.{BeanProperty, BooleanBeanProperty}
//remove if not needed
import scala.collection.JavaConversions._

object ReceptionReport {

  def encode(block: ReceptionReport): ChannelBuffer = {
    val buffer = ChannelBuffers.buffer(24)
    buffer.writeInt(block.ssrc.toInt)
    buffer.writeByte(block.fractionLost)
    buffer.writeMedium(block.cumulativeNumberOfPacketsLost)
    buffer.writeInt(block.extendedHighestSequenceNumberReceived.toInt)
    buffer.writeInt(block.interArrivalJitter.toInt)
    buffer.writeInt(block.lastSenderReport.toInt)
    buffer.writeInt(block.delaySinceLastSenderReport.toInt)
    buffer
  }

  def decode(buffer: ChannelBuffer): ReceptionReport = {
    val block = new ReceptionReport()
    block.setSsrc(buffer.readUnsignedInt())
    block.setFractionLost(buffer.readUnsignedByte())
    block.setCumulativeNumberOfPacketsLost(buffer.readUnsignedMedium())
    block.setExtendedHighestSequenceNumberReceived(buffer.readUnsignedInt())
    block.setInterArrivalJitter(buffer.readUnsignedInt())
    block.setLastSenderReport(buffer.readUnsignedInt())
    block.setDelaySinceLastSenderReport(buffer.readUnsignedInt())
    block
  }
}

/**
 * @author <a:mailto="bruno.carvalho@wit-software.com" />Bruno de Carvalho</a>
 */
class ReceptionReport {

  @BeanProperty
  var ssrc: Long = _

  @BeanProperty
  var fractionLost: Short = _

  @BeanProperty
  var cumulativeNumberOfPacketsLost: Int = _

  @BeanProperty
  var extendedHighestSequenceNumberReceived: Long = _

  @BeanProperty
  var interArrivalJitter: Long = _

  @BeanProperty
  var lastSenderReport: Long = _

  @BeanProperty
  var delaySinceLastSenderReport: Long = _

  def encode(): ChannelBuffer = encode(this)

  def setSsrc(ssrc: Long) {
    if ((ssrc < 0) || (ssrc > 0xffffffffL)) {
      throw new IllegalArgumentException("Valid range for SSRC is [0;0xffffffff]")
    }
    this.ssrc = ssrc
  }

  def setFractionLost(fractionLost: Short) {
    if ((fractionLost < 0) || (fractionLost > 0xffffffffL)) {
      throw new IllegalArgumentException("Valid range for Fraction Lost is [0;0xff]")
    }
    this.fractionLost = fractionLost
  }

  def setCumulativeNumberOfPacketsLost(cumulativeNumberOfPacketsLost: Int) {
    if ((cumulativeNumberOfPacketsLost < 0) || (cumulativeNumberOfPacketsLost > 0x00ffffff)) {
      throw new IllegalArgumentException("Valid range for Cumulative Number of Packets Lost is [0;0x00ffffff]")
    }
    this.cumulativeNumberOfPacketsLost = cumulativeNumberOfPacketsLost
  }

  def setExtendedHighestSequenceNumberReceived(extendedHighestSequenceNumberReceived: Long) {
    if ((extendedHighestSequenceNumberReceived < 0) || 
      (extendedHighestSequenceNumberReceived > 0xffffffffL)) {
      throw new IllegalArgumentException("Valid range for Extended Highest SeqNumber Received is [0;0xffffffff]")
    }
    this.extendedHighestSequenceNumberReceived = extendedHighestSequenceNumberReceived
  }

  def setInterArrivalJitter(interArrivalJitter: Long) {
    if ((interArrivalJitter < 0) || (interArrivalJitter > 0xffffffffL)) {
      throw new IllegalArgumentException("Valid range for Interarrival Jitter is [0;0xffffffff]")
    }
    this.interArrivalJitter = interArrivalJitter
  }

  def setLastSenderReport(lastSenderReport: Long) {
    if ((lastSenderReport < 0) || (lastSenderReport > 0xffffffffL)) {
      throw new IllegalArgumentException("Valid range for Last Sender Report is [0;0xffffffff]")
    }
    this.lastSenderReport = lastSenderReport
  }

  def setDelaySinceLastSenderReport(delaySinceLastSenderReport: Long) {
    if ((delaySinceLastSenderReport < 0) || (delaySinceLastSenderReport > 0xffffffffL)) {
      throw new IllegalArgumentException("Valid range for Delay Since Last Sender Report is [0;0xffffffff]")
    }
    this.delaySinceLastSenderReport = delaySinceLastSenderReport
  }

  override def toString(): String = {
    new StringBuilder().append("ReceptionReport{").append("ssrc=")
      .append(this.ssrc)
      .append(", fractionLost=")
      .append(this.fractionLost)
      .append(", cumulativeNumberOfPacketsLost=")
      .append(this.cumulativeNumberOfPacketsLost)
      .append(", extendedHighestSequenceNumberReceived=")
      .append(this.extendedHighestSequenceNumberReceived)
      .append(", interArrivalJitter=")
      .append(this.interArrivalJitter)
      .append(", lastSenderReport=")
      .append(this.lastSenderReport)
      .append(", delaySinceLastSenderReport=")
      .append(this.delaySinceLastSenderReport)
      .append('}')
      .toString
  }
}
