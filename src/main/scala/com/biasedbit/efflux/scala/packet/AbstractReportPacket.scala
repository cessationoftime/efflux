package com.biasedbit.efflux.scala.packet

import java.util.ArrayList
import java.util.Collections
import java.util.List
//remove if not needed
import scala.collection.JavaConversions._

/**
 * @author <a:mailto="bruno.carvalho@wit-software.com" />Bruno de Carvalho</a>
 */
abstract class AbstractReportPacket protected (`type`: ControlPacket.Type.Type) extends ControlPacket(`type`) {

  protected var senderSsrc: Long = _

  protected var receptionReports: List[ReceptionReport] = _

  def addReceptionReportBlock(block: ReceptionReport): Boolean = {
    if (this.receptionReports == null) {
      this.receptionReports = new ArrayList[ReceptionReport]()
      return this.receptionReports.add(block)
    }
    (this.receptionReports.size < 31) && this.receptionReports.add(block)
  }

  def getReceptionReportCount(): Byte = {
    if (this.receptionReports == null) {
      return 0
    }
    this.receptionReports.size.toByte
  }

  def getSenderSsrc(): Long = senderSsrc

  def setSenderSsrc(senderSsrc: Long) {
    if ((senderSsrc < 0) || (senderSsrc > 0xffffffffL)) {
      throw new IllegalArgumentException("Valid range for SSRC is [0;0xffffffff]")
    }
    this.senderSsrc = senderSsrc
  }

  def getReceptionReports(): List[ReceptionReport] = {
    if (this.receptionReports == null) {
      return null
    }
    Collections.unmodifiableList(this.receptionReports)
  }

  def setReceptionReports(receptionReports: List[ReceptionReport]) {
    if (receptionReports.size >= 31) {
      throw new IllegalArgumentException("At most 31 report blocks can be sent in a *ReportPacket")
    }
    this.receptionReports = receptionReports
  }
}
