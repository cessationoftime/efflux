package com.biasedbit.efflux.scala.util

import java.security.MessageDigest
import java.security.NoSuchAlgorithmException
//remove if not needed
import scala.collection.JavaConversions._

object ByteUtils {

  /**
   * Hash a string
   *
   * @param toHash String to be hashed.
   *
   * @return Hashed string.
   */
  def hash(toHash: AnyRef): String = {
    val hashString = toHash.toString
    var md: MessageDigest = null
    try {
      md = MessageDigest.getInstance("MD5")
    } catch {
      case e: NoSuchAlgorithmException ⇒ return hashString
    }
    md.update(hashString.getBytes, 0, hashString.length)
    convertToHex(md.digest())
  }

  def convertToHex(data: Array[Byte]): String = {
    val buf = new StringBuffer()
    for (aData ← data) {
      var halfbyte = (aData >>> 4) & 0x0F
      var two_halfs = 0
      do {
        if ((0 <= halfbyte) && (halfbyte <= 9)) {
          buf.append(('0' + halfbyte).toChar)
        } else {
          buf.append(('a' + (halfbyte - 10)).toChar)
        }
        halfbyte = aData & 0x0F
        two_halfs = two_halfs + 1
      } while (two_halfs < 1);
    }
    buf.toString
  }

  def writeArrayAsHex(array: Array[Byte], packedPrint: Boolean): String = {
    if (packedPrint) {
      return convertToHex(array)
    }
    val builder = new StringBuilder()
    for (b ← array) {
      builder.append(" 0x")
      val hex = Integer.toHexString(b)
      hex.length match {
        case 1 ⇒ builder.append('0').append(hex)
        case 2 ⇒ builder.append(hex)
        case _ ⇒ builder.append(hex.substring(6, 8))
      }
    }
    builder.toString
  }

  def convertHexStringToByteArray(hexString: String): Array[Byte] = {
    if ((hexString.length % 2) != 0) {
      throw new IllegalArgumentException("Invalid hex string (length % 2 != 0)")
    }
    val array = Array.ofDim[Byte](hexString.length / 2)
    var i = 0
    var arrayIndex = 0
    while (i < hexString.length) {
      array(arrayIndex) = Integer.valueOf(hexString.substring(i, i + 2), 16).byteValue()
      i += 2
      arrayIndex += 1
    }
    array
  }

  /**
   * Get a byte array in a printable binary form.
   *
   * @param bytes The bytes to be writen.
   * @return A String representation of the bytes.
   */
  def writeBits(bytes: Array[Byte]): String = {
    val stringBuilder = new StringBuilder()
    for (i ← 0 until bytes.length) {
      if ((i % 4) == 0) {
        stringBuilder.append("\n")
      }
      stringBuilder.append(writeBits(bytes(i))).append(" ")
    }
    stringBuilder.toString
  }

  /**
   * Get a byte in a printable binary form.
   *
   * @param b The byte to be writen.
   * @return A String representation of the byte.
   */
  def writeBits(b: Byte): String = {
    val stringBuffer = new StringBuffer()
    var bit: Int = 0
    var i = 7
    while (i >= 0) {
      bit = (b >>> i) & 0x01
      stringBuffer.append(bit)
      i -= 1
    }
    stringBuffer.toString
  }
}
