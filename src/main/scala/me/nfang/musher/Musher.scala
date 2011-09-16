package me.nfang.musher

import me.nfang.musher.common._
import org.joda.time._

trait Musher {
	// Characters to be used in generated short URL
	protected val charset = "0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ"
	
	// Get clicks
	def analyze(hash: String): String
	// Decode a hash to the original url and logs a click.
	def expand(hash: String, referer: String, country: String, time: DateTime): Option[String]
	// Get metadata for short link
	def getMetadata(hash: String): Option[UrlMeta]
	// Encode a url to a hash
	def hash(url: String, title: String = "Untitled"): String
	// Decode a hash to the original url
	def parse(hash: String): String
}

object Musher extends Musher with RedisSupport