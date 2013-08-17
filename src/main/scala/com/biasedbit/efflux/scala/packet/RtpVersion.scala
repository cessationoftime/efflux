package com.biasedbit.efflux.scala.packet

//remove if not needed
import scala.collection.JavaConversions._

object RtpVersion extends Enumeration {

  val V2 = new RtpVersion(0x80.toByte)

  val V1 = new RtpVersion(0x40.toByte)

  val V0 = new RtpVersion(0x00.toByte)

  def fromByte(b: Byte): RtpVersion = {
    val tmp = (b & 0xc0).toByte
    for (version ← values if version.getByte == tmp) {
      return version
    }
    throw new IllegalArgumentException("Unknown version for byte: " + b)
  }

  implicit def convertValue(v: Value): RtpVersion = v.asInstanceOf[RtpVersion]
}
class RtpVersion private[RtpVersion] (val b: Byte) extends scala.Enumeration.Val {

  def getByte(): Byte = b
}