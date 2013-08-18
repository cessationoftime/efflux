package com.biasedbit.efflux.scala.session

import org.scalatest.WordSpec
import org.scalatest.matchers.MustMatchers
import com.biasedbit.efflux.util.ByteUtils
import org.jboss.netty.buffer.ChannelBuffer
import org.jboss.netty.buffer.ChannelBuffers
import org.scalatest.BeforeAndAfter
import java.net.SocketAddress
import java.net.InetSocketAddress
import java.util.concurrent.atomic.AtomicInteger
import com.biasedbit.efflux.scala.participant.RtpParticipant
import com.biasedbit.efflux.scala.packet.DataPacket
import com.biasedbit.efflux.scala.participant.RtpParticipantInfo

class MultiParticipantSessionSpec extends WordSpec with MustMatchers with BeforeAndAfter {
  var session: MultiParticipantSession = null;
  //   before {
  //    
  //  }

  after {
    if (this.session != null) {
      this.session.terminate();
    }
  }

  "MultiParticipantSessionSpec" should {

    "testNewParticipantFromDataPacket" in {
      val participant: RtpParticipant = RtpParticipant.createReceiver("localhost", 8000, 8001);
      participant.getInfo().setSsrc(6969);
      this.session = new MultiParticipantSession("id", 8, participant);
      this.session.init() must equal(true);

      this.session.addEventListener(new RtpSessionEventListener() {

        override def participantJoinedFromData(session: RtpSession, participant: RtpParticipant) {
          69 must equal(participant.getSsrc());
        }

        override def participantJoinedFromControl(session: RtpSession, participant: RtpParticipant) {
        }

        override def participantDataUpdated(session: RtpSession, participant: RtpParticipant) {
        }

        override def participantLeft(session: RtpSession, participant: RtpParticipant) {
        }

        override def participantDeleted(session: RtpSession, participant: RtpParticipant) {
        }

        override def resolvedSsrcConflict(session: RtpSession, oldSsrc: Long, newSsrc: Long) {
        }

        override def sessionTerminated(session: RtpSession, cause: Throwable) {
          System.err.println("Session terminated: " + cause.getMessage());
        }
      });

      val packet: DataPacket = new DataPacket();
      packet.setSequenceNumber(1);
      packet.setPayloadType(8);
      packet.setSsrc(69);
      val address: SocketAddress = new InetSocketAddress("localhost", 8000);
      this.session.dataPacketReceived(address, packet);
    }

    "testOutOfOrderDiscard" in {
      val participant: RtpParticipant = RtpParticipant.createReceiver("localhost", 8000, 8001);
      participant.getInfo().setSsrc(6969);
      this.session = new MultiParticipantSession("id", 8, participant);
      this.session.setDiscardOutOfOrder(true);
      this.session.init() must equal(true)

      val counter: AtomicInteger = new AtomicInteger(0);

      this.session.addDataListener(new RtpSessionDataListener() {

        override def dataPacketReceived(session: RtpSession, participant: RtpParticipantInfo, packet: DataPacket) {
          counter.incrementAndGet();
        }
      });

      val packet: DataPacket = new DataPacket();
      packet.setSequenceNumber(10);
      packet.setPayloadType(8);
      packet.setSsrc(69);
      val address: SocketAddress = new InetSocketAddress("localhost", 8000);
      this.session.dataPacketReceived(address, packet);
      packet.setSequenceNumber(11);
      this.session.dataPacketReceived(address, packet);
      packet.setSequenceNumber(10);
      this.session.dataPacketReceived(address, packet);

      2 must equal(counter.get());
    }
  }
}