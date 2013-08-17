package com.biasedbit.efflux.scala.participant

import com.biasedbit.efflux.scala.packet.SdesChunk
import com.biasedbit.efflux.scala.packet.SdesChunkItem
import com.biasedbit.efflux.scala.packet.SdesChunkPrivItem
import java.util.Random
import RtpParticipantInfo._
import scala.reflect.{ BeanProperty, BooleanBeanProperty }
//remove if not needed
import scala.collection.JavaConversions._

object RtpParticipantInfo {

  private val RANDOM = new Random(System.nanoTime())

  /**
   * Randomly generates a new SSRC.
   * <p/>
   * Assuming no other source can obtain the exact same seed (or they're using a different algorithm for the random
   * generation) the probability of collision is roughly 10^-4 when the number of RTP sources is 1000.
   * <a href="http://tools.ietf.org/html/rfc3550#section-8.1">RFC 3550, Section 8.1<a>
   * <p/>
   * In this case, collision odds are slightly bigger because the identifier size will be 31 bits (0x7fffffff,
   * {@link Integer#MAX_VALUE} rather than the full 32 bits.
   *
   * @return A new, random, SSRC identifier.
   */
  def generateNewSsrc(): Long = RANDOM.nextInt(Integer.MAX_VALUE)
}

/**
 * @author <a href="http://bruno.biasedbit.com/">Bruno de Carvalho</a>
 */
class RtpParticipantInfo(@BeanProperty var ssrc: Long) {

  @BeanProperty
  var name: String = _

  @BeanProperty
  var cname: String = _

  @BeanProperty
  var email: String = _

  @BeanProperty
  var phone: String = _

  @BeanProperty
  var location: String = _

  @BeanProperty
  var tool: String = _

  @BeanProperty
  var note: String = _

  @BeanProperty
  var privPrefix: String = _

  @BeanProperty
  var priv: String = _

  if ((ssrc < 0) || (ssrc > 0xffffffffL)) {
    throw new IllegalArgumentException("Valid range for SSRC is [0;0xffffffff]")
  }

  def this() {
    this(generateNewSsrc())
  }

  def updateFromSdesChunk(chunk: SdesChunk): Boolean = {
    var modified = false
    if (this.ssrc != chunk.getSsrc) {
      this.ssrc = chunk.getSsrc
      modified = true
    }
    if (chunk.getItems == null) {
      return modified
    }
    import SdesChunkItem.Type._
    for (item ← chunk.getItems) item.getType match {
      case CNAME ⇒ if (this.willCauseModification(this.cname, item.getValue)) {
        this.setCname(item.getValue)
        modified = true
      }
      case NAME ⇒ if (this.willCauseModification(this.name, item.getValue)) {
        this.setName(item.getValue)
        modified = true
      }
      case EMAIL ⇒ if (this.willCauseModification(this.email, item.getValue)) {
        this.setEmail(item.getValue)
        modified = true
      }
      case PHONE ⇒ if (this.willCauseModification(this.phone, item.getValue)) {
        this.setPhone(item.getValue)
        modified = true
      }
      case LOCATION ⇒ if (this.willCauseModification(this.location, item.getValue)) {
        this.setLocation(item.getValue)
        modified = true
      }
      case TOOL ⇒ if (this.willCauseModification(this.tool, item.getValue)) {
        this.setTool(item.getValue)
        modified = true
      }
      case NOTE ⇒ if (this.willCauseModification(this.note, item.getValue)) {
        this.setNote(item.getValue)
        modified = true
      }
      case PRIV ⇒
        var prefix = item.asInstanceOf[SdesChunkPrivItem].getPrefix
        if (this.willCauseModification(this.privPrefix, prefix) ||
          this.willCauseModification(this.priv, item.getValue)) {
          this.setPriv(prefix, item.getValue)
          modified = true
        }

      case _ ⇒
    }
    modified
  }

  private def willCauseModification(originalValue: String, newValue: String): Boolean = {
    newValue != null && newValue != originalValue
  }

  /**
   * USE THIS WITH EXTREME CAUTION at the risk of seriously screwing up the way sessions handle data from incoming
   * participants.
   *
   * @param ssrc The new SSRC.
   */
  def setSsrc(ssrc: Long) {
    if ((ssrc < 0) || (ssrc > 0xffffffffL)) {
      throw new IllegalArgumentException("Valid range for SSRC is [0;0xffffffff]")
    }
    this.ssrc = ssrc
  }

  def setPriv(prefix: String, priv: String) {
    this.privPrefix = prefix
    this.priv = priv
  }

  override def toString(): String = {
    val builder = new StringBuilder().append("RtpParticipantInfo{").append("ssrc=")
      .append(this.ssrc)
    if (this.cname != null) {
      builder.append(", cname='").append(this.cname).append('\'')
    }
    if (this.name != null) {
      builder.append(", name='").append(this.name).append('\'')
    }
    if (this.email != null) {
      builder.append(", email='").append(this.email).append('\'')
    }
    if (this.phone != null) {
      builder.append(", phone='").append(this.phone).append('\'')
    }
    if (this.location != null) {
      builder.append(", location='").append(this.location)
        .append('\'')
    }
    if (this.tool != null) {
      builder.append(", tool='").append(this.tool).append('\'')
    }
    if (this.note != null) {
      builder.append(", note='").append(this.note).append('\'')
    }
    if (this.priv != null) {
      builder.append(", priv='").append(this.privPrefix).append(':')
        .append(this.priv)
        .append('\'')
    }
    builder.append('}').toString
  }
}
