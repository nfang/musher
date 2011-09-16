package me.nfang.musher

import java.util.ResourceBundle
import javax.servlet._

object Configuration {
	private val cache = ResourceBundle.getBundle("settings")
	
	def getString(key: String): String = cache.getString(key)
}