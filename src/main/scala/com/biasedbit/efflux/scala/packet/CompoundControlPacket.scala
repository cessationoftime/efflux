package com.biasedbit.efflux.scala.packet

import java.util.Arrays
import java.util.List
import scala.reflect.{BeanProperty, BooleanBeanProperty}
//remove if not needed
import scala.collection.JavaConversions._

/**
 * @author <a:mailto="bruno.carvalho@wit-software.com" />Bruno de Carvalho</a>
 */
class CompoundControlPacket(controlPackets: ControlPacket*) {

  @BeanProperty
  val controlPackets = Arrays.asList(controlPackets:_*)

  if (controlPackets.length == 0) {
    throw new IllegalArgumentException("At least one RTCP packet must be provided")
  }

  def this(controlPackets: List[ControlPacket]) {
    this()
    if ((controlPackets == null) || controlPackets.isEmpty) {
      throw new IllegalArgumentException("ControlPacket list cannot be null or empty")
    }
    this.controlPackets = controlPackets
  }

  def getPacketCount(): Int = this.controlPackets.size

  override def toString(): String = {
    val builder = new StringBuilder()
    builder.append("CompoundControlPacket{\n")
    for (packet <- this.controlPackets) {
      builder.append("  ").append(packet.toString).append('\n')
    }
    builder.append('}').toString
  }
}
