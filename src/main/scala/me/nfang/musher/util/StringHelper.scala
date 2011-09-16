package me.nfang.musher.util

import java.security._

object StringHelper {
	// Get the md5 digest for a string.
	def getDigest(text: String): String = {
		val buf = text.getBytes("UTF-8")
		val md = MessageDigest.getInstance("MD5")
		new String(md.digest(buf))
	}
}