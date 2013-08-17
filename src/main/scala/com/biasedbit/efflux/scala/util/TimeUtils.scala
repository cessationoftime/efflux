package com.biasedbit.efflux.scala.util

//remove if not needed
import scala.collection.JavaConversions._

object TimeUtils {

  /**
   * Retrieve a timestamp for the current instant.
   *
   * @return Current instant.
   */
  def now(): Long = System.currentTimeMillis()

  /**
   * Retrieve a timestamp for the current instant, in nanoseconds.
   *
   * @return Current instant.
   */
  def nowNanos(): Long = System.nanoTime()

  /**
   * Test whether a given event has timed out (in seconds).
   *
   * @param now        Current instant.
   * @param eventTime  Instant at which the event took place.
   * @param timeBuffer The amount of time for which the event is valid (in seconds).
   *
   * @return <code>true</code> if the event has expired, <code>false</code> otherwise
   */
  def hasExpired(now: Long, eventTime: Long, timeBuffer: Long): Boolean = {
    hasExpiredMillis(now, eventTime, timeBuffer * 1000)
  }

  /**
   * Test whether a given event has timed out (in milliseconds).
   *
   * @param now        Current instant.
   * @param eventTime  Instant at which the event took place.
   * @param timeBuffer The amount of time for which the event is valid (in milliseconds).
   *
   * @return <code>true</code> if the event has expired, <code>false</code> otherwise
   */
  def hasExpiredMillis(now: Long, eventTime: Long, timeBuffer: Long): Boolean = (eventTime + timeBuffer) < now
}
