package me.nfang.musher

import me.nfang.musher.common._
import me.nfang.musher.util._
import me.nfang.musher.util.StringHelper._
import org.apache.commons.math.random.MersenneTwister
import org.joda.time._
import org.joda.time.format.DateTimeFormat
import redis.clients.jedis._
import scala.actors.Actor._
import scala.collection.mutable.{HashMap, HashSet, ListBuffer}
import scala.util.parsing.json._
import scala.collection.JavaConversions._

trait RedisSupport extends Musher {
	private object RedisConstants {
		val NewField = 1L
		val ExistField = 0L
	}
	
	private[this] val pool = new JedisPool(new JedisPoolConfig, 
										   Configuration.getString("DB_ADDR"), 
										   Integer.parseInt(Configuration.getString("DB_PORT")))
	
	private val dateTimeFormatter = DateTimeFormat.forPattern(Constants.DefaultDbDateTimeFormat)
	
	/**
	 * Manage jedis connection pool using loan pattern
	 */
	private def useJedisPool[R](op: Jedis => R): R = {
		val jedis = pool.getResource
		try {
			op(jedis)
		} finally {
			pool.returnResource(jedis)
		}
	}
	
	private def getInfoKey(hash: String, info: String = "") = {
		info match {
			case "referer" => hash + Constants.RefererSuffix
			case "country" => hash + Constants.CountrySuffix
			case "timeline" => hash + Constants.TimelineSuffix
			case _ => ""
		}
	}	
	
	override def analyze(hash: String): String = {
		val func = (jedis: Jedis) => {
			val referers = jedis.hgetAll(getInfoKey(hash, "referer")).toList
			val countries = jedis.hgetAll(getInfoKey(hash, "country")).toList
			val timeline = jedis.hvals(getInfoKey(hash, "timeline")).toList
			val range = jedis.hkeys(getInfoKey(hash, "timeline"))
							 .map(dt => dateTimeFormatter.parseDateTime(dt).toString(Constants.DefaultJavaScriptDateTimeFormat))
							 .toList
			val jsonMap = Map[String, Any](
				("referer", JSONArray(referers.map(pair => JSONArray(List(pair._1, Integer.parseInt(pair._2)))))),
				("country", JSONArray(countries.map(pair => JSONArray(List(pair._1, Integer.parseInt(pair._2)))))),
				("timeline", JSONArray(timeline)),
				("range", JSONArray(range))
			)
			JSONObject(jsonMap).toString(JSONFormat.defaultFormatter)
		}
		useJedisPool[String](func)
	}
	
	override def expand(hash: String, referer: String, addr: String, time: DateTime): Option[String] = {
		val func = (jedis: Jedis) => {
			val url = jedis.hget(hash, "long_url")
			if (url == null) None
			else {
				// # Region Move to a seperate class
				actor {
					val refererKey = getInfoKey(hash, "referer")
					val countryKey = getInfoKey(hash, "country")
					val dateKey = getInfoKey(hash, "timeline")
					
					var flag = new AnyRef
					// IP should be cached
					val country = IPInfoDbHelper.getCountry(addr) match {
						case Some(resp: IPInfoDbResponse) => resp.CountryCode
						case None => "Other"
					}
					do {
						jedis.watch(refererKey, countryKey, dateKey)
						val tx = jedis.multi
						tx.hincrBy(hash, "total_clicks", 1L)
						tx.hincrBy(countryKey, country, 1L)
						tx.hincrBy(refererKey, referer, 1L)
						tx.hincrBy(dateKey, time.toString(Constants.DefaultDbDateTimeFormat), 1L)
						flag = tx.exec
					} while(flag == null) // If "EXEC" returns null, the whole transaction is aborted
				}
				// # EOR
				Some(url)
			}
		}
		useJedisPool[Option[String]](func)
	}
	
	override def getMetadata(hash: String): Option[UrlMeta] = {
		val func = (jedis: Jedis) => {
			val meta = jedis.hgetAll(hash)						
			if(meta != null && meta.size > 0) {
				val createdOn = meta.get("created_on") match {
					case dt: String => dateTimeFormatter.parseDateTime(dt).toString(Constants.DefaultJavaScriptDateTimeFormat)
					case _ => ""
				}
				Some(new UrlMeta(
					Configuration.getString("DOMAIN") + hash, 
					meta.getOrElse("long_url", ""), 
					createdOn, 
					Integer.parseInt(meta.getOrElse("total_clicks", 0).toString)))
			}
			else None
		}
		useJedisPool[Option[UrlMeta]](func)
	}
	
	override def hash(url: String, title: String = "Untitled"): String = {
		val func = (jedis: Jedis) => {
			val prng = new MersenneTwister()
			val urlKey = getDigest(url) // The MD5 for the given url
			val retVal = jedis.get(urlKey)
			//! Consider racing condition
			if (retVal != null) retVal
			else {
				var flag = RedisConstants.ExistField
				var urlHash = "" // The shortened url hash
				while(flag != RedisConstants.NewField) {
					var buf = new ListBuffer[Char]
					val len = charset.size
					while(buf.size < 6)
						buf += charset.charAt(prng.nextInt(len))
					urlHash = buf.mkString
					flag = jedis.hsetnx(urlHash, "long_url", url)
				}
				jedis.hset(urlHash, "created_on", new DateTime(DateTimeZone.UTC).toString(Constants.DefaultDbDateTimeFormat))
				jedis.set(urlKey, urlHash)
				urlHash
			}
		}
		useJedisPool[String](func)
	}
	
	override def parse(hash: String): String = {
		useJedisPool[String](jedis => jedis.hget(hash, "long_url"))
	}
}
