package me.nfang.musher.test

import java.util.ResourceBundle
import org.specs2._
import org.joda.time._
import redis.clients.jedis._
import me.nfang.musher.Musher
import specification.{BeforeExample}

class RedisSupportSpec extends Specification with BeforeExample { def is =
	"This is a specification for Musher url shortener"				^
																	p^
	"Musher should be able to"										^
		"encode a valid url to a six-char unique hash"				! hashExample^
		"expand a hash to the original url"							! parseExample^
																	end
	val db_addr = ResourceBundle.getBundle("settings").getString("DB_ADDR")
	val db_port = 6380
	
	/**
	 * Clean up test database before each example
	 */
	def before = {
		println("Clean up test database")
		val db = new Jedis(db_addr, db_port)
		db.flushDB
		db.quit
	}
	
	/**
	 * Example for hashing a url
	 */
	def hashExample = {
		val url0 = "http://www.google.com"
		val res0 = Musher.hash(url0)
		val url1 = "http://www.google.com"
		val res1 = Musher.hash(url1)
		val url2 = "http://code.google.com/p/redis/issues/list"
		val res2 = Musher.hash(url2)
		res0 must have size(6)
		res1 must have size(6)
		res2 must have size(6)
		res0 must beEqualTo(res1)
		res0 must not be equalTo(res2)
	}
	
	/**
	 * Example for parsing a hash to get the original url
	 */
	def parseExample = {
		val url = "http://www.google.com"
		val res = Musher.hash(url)
		Musher.parse(res) must beEqualTo(url)
	}
}