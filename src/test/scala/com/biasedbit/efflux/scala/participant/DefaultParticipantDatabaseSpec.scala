package com.biasedbit.efflux.scala.participant

import org.scalatest.WordSpec
import org.scalatest.matchers.MustMatchers
import com.biasedbit.efflux.util.ByteUtils
import org.jboss.netty.buffer.ChannelBuffer
import org.jboss.netty.buffer.ChannelBuffers
import com.biasedbit.efflux.packet.ControlPacket
import com.biasedbit.efflux.packet.ByePacket
import com.biasedbit.efflux.packet.ReceiverReportPacket
import com.biasedbit.efflux.packet.SenderReportPacket
import com.biasedbit.efflux.packet.ReceptionReport
import com.biasedbit.efflux.packet.SdesChunkItem
import com.biasedbit.efflux.packet.SdesChunkItems
import com.biasedbit.efflux.packet.SdesChunk
import com.biasedbit.efflux.packet.RtpVersion
import com.biasedbit.efflux.packet.SourceDescriptionPacket
import com.biasedbit.efflux.packet.SdesChunkPrivItem
import org.scalatest.BeforeAndAfter
import com.biasedbit.efflux.participant.RtpParticipant
import com.biasedbit.efflux.participant.DefaultParticipantDatabase
import java.net.SocketAddress
import java.net.InetSocketAddress
import com.biasedbit.efflux.participant.ParticipantEventListener
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicBoolean
import com.biasedbit.efflux.participant.ParticipantOperation
import com.biasedbit.efflux.packet.DataPacket

// private classes ------------------------------------------------------------------------------------------------

private[participant] class TestListener extends ParticipantEventListener {

  // internal vars ----------------------------------------------------------------------------------------------

  private val sdesCreations = new AtomicInteger();
  private val dataPacketCreations = new AtomicInteger();
  private val deletions = new AtomicInteger();

  // ParticipantEventListener -----------------------------------------------------------------------------------

  override def participantCreatedFromSdesChunk(participant: RtpParticipant) {
    this.sdesCreations.incrementAndGet();
  }

  override def participantCreatedFromDataPacket(participant: RtpParticipant) {
    this.dataPacketCreations.incrementAndGet();
  }

  override def participantDeleted(participant: RtpParticipant) {
    this.deletions.incrementAndGet();
  }

  // public methods ---------------------------------------------------------------------------------------------
  def getSdesCreations(): Int = this.sdesCreations.get();
  def getDataPacketCreations(): Int = this.dataPacketCreations.get();
  def getDeletions(): Int = this.deletions.get();

}

class DefaultParticipantDatabaseSpec extends WordSpec with MustMatchers with BeforeAndAfter {

  var database: DefaultParticipantDatabase = null;
  var listener: TestListener = null;

  before {
    this.listener = new TestListener();
    this.database = new DefaultParticipantDatabase("testDatabase", this.listener);
  }

  def testAddReceiver() {
    val participant: RtpParticipant = RtpParticipant.createReceiver("localhost", 8000, 8001);
    0 must equal(this.database.getReceiverCount());
    this.database.addReceiver(participant) must equal(true)
    1 must equal(this.database.getReceiverCount());
  }

  "DefaultParticipantDatabaseSpec" should {

    "testGetId" in {
      // Ow, ffs...
      "testDatabase" must equal(this.database.getId());
    }

    "testAddReceiver" in {
      this.testAddReceiver()
    }

    "testFailAddReceiver" in {
      val chunk: SdesChunk = new SdesChunk(0x45);
      val address: SocketAddress = new InetSocketAddress("localhost", 8000);
      // Creation of a non-receiver participant, and adding it as a receiver must fail.
      val participant: RtpParticipant = RtpParticipant.createFromSdesChunk(address, chunk);
      0 must equal(this.database.getReceiverCount());
      this.database.addReceiver(participant) must equal(false)
      0 must equal(this.database.getReceiverCount());
    }

    "testDoWithReceivers" in {
      this.testAddReceiver()

      val doSomething: AtomicBoolean = new AtomicBoolean();
      this.database.doWithReceivers(new ParticipantOperation() {
        @throws(classOf[Exception])
        override def doWithParticipant(participant: RtpParticipant) {
          doSomething.set(true);
        }
      });

      doSomething.get() must equal(true)
    }

    "testRemoveReceiver" in {
      val participant: RtpParticipant = RtpParticipant.createReceiver("localhost", 8000, 8001);
      0 must equal(this.database.getReceiverCount());
      this.database.addReceiver(participant) must equal(true)
      1 must equal(this.database.getReceiverCount());
      this.database.removeReceiver(participant) must equal(true)
      0 must equal(this.database.getReceiverCount());
    }

    "testRemoveReceiver2" in {
      var participant: RtpParticipant = RtpParticipant.createReceiver("localhost", 8000, 8001);
      0 must equal(this.database.getReceiverCount());
      this.database.addReceiver(participant) must equal(true)
      1 must equal(this.database.getReceiverCount());

      // Not added yet, so won't be removed.
      participant = RtpParticipant.createReceiver("localhost", 8002, 8003);
      this.database.removeReceiver(participant) must equal(false)
      1 must equal(this.database.getReceiverCount());
    }

    "testGetOrCreateParticipantFromDataPacket" in {
      val packet: DataPacket = new DataPacket();
      packet.setSsrc(0x45);
      val address: SocketAddress = new InetSocketAddress("localhost", 8000);

      0 must equal(this.database.getReceiverCount());
      0 must equal(this.database.getParticipantCount());
      val participant: RtpParticipant = this.database.getOrCreateParticipantFromDataPacket(address, packet);
      participant must not equal (null);
      1 must equal(this.database.getParticipantCount());
      0 must equal(this.database.getReceiverCount());
      0x45 must equal(participant.getSsrc());
      participant.getLastDataOrigin() must not equal (null);
      address must equal(participant.getLastDataOrigin());
      participant.getLastControlOrigin() must equal(null);
      0 must equal(this.listener.getSdesCreations());
      1 must equal(this.listener.getDataPacketCreations());
      0 must equal(this.listener.getDeletions());
    }

    "testGetOrCreateParticipantFromSdesChunk" in {
      val chunk: SdesChunk = new SdesChunk(0x45);
      val address: SocketAddress = new InetSocketAddress("localhost", 8000);

      0 must equal(this.database.getReceiverCount());
      0 must equal(this.database.getParticipantCount());
      val participant: RtpParticipant = this.database.getOrCreateParticipantFromSdesChunk(address, chunk);
      participant must not equal (null);
      1 must equal(this.database.getParticipantCount());
      0 must equal(this.database.getReceiverCount());
      0x45 must equal(participant.getSsrc());
      participant.getLastDataOrigin() must equal(null);
      participant.getLastControlOrigin() must not equal (null);
      address must equal(participant.getLastControlOrigin());
      1 must equal(this.listener.getSdesCreations());
      0 must equal(this.listener.getDataPacketCreations());
      0 must equal(this.listener.getDeletions());
    }

    "testAssociationOfParticipantViaDataAddress" in {
      val receiver: RtpParticipant = RtpParticipant.createReceiver("localhost", 8000, 8001);
      0 must equal(this.database.getReceiverCount());
      this.database.addReceiver(receiver) must equal(true)
      1 must equal(this.database.getReceiverCount());

      val packet: DataPacket = new DataPacket();
      packet.setSsrc(0x45);
      val address: SocketAddress = new InetSocketAddress("localhost", 8000);

      0 must equal(this.database.getParticipantCount());
      val participant: RtpParticipant = this.database.getOrCreateParticipantFromDataPacket(address, packet);
      participant must not equal (null);
      1 must equal(this.database.getParticipantCount());
      1 must equal(this.database.getReceiverCount());
      0x45 must equal(participant.getSsrc());
      participant.getLastDataOrigin() must not equal (null);
      // Here is where the association is tested.
      receiver must equal(participant);
      0 must equal(this.listener.getSdesCreations());
      0 must equal(this.listener.getDataPacketCreations());
      0 must equal(this.listener.getDeletions());
    }

    "testNonAssociationOfParticipantViaDataAddress" in {
      val receiver: RtpParticipant = RtpParticipant.createReceiver("localhost", 8000, 8001);
      0 must equal(this.database.getReceiverCount());
      this.database.addReceiver(receiver) must equal(true)
      1 must equal(this.database.getReceiverCount());

      val packet: DataPacket = new DataPacket();
      packet.setSsrc(0x45);
      val address: SocketAddress = new InetSocketAddress("localhost", 9000);

      0 must equal(this.database.getParticipantCount());
      val participant: RtpParticipant = this.database.getOrCreateParticipantFromDataPacket(address, packet);
      participant must not equal (null);
      1 must equal(this.database.getParticipantCount());
      1 must equal(this.database.getReceiverCount());
      0x45 must equal(participant.getSsrc());
      participant.getLastDataOrigin() must not equal (null);
      // Here is where the association is tested.
      receiver must not be theSameInstanceAs(participant);
      0 must equal(this.listener.getSdesCreations());
      1 must equal(this.listener.getDataPacketCreations());
      0 must equal(this.listener.getDeletions());
    }

  }
}