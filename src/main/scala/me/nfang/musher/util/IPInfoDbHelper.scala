package me.nfang.musher.util

import scala.util.parsing.json._
import com.sun.jersey.api.client.Client

object IPInfoDbResponse {
	def buildFrom(prop: Map[String, Any]): IPInfoDbResponse = {
		new IPInfoDbResponse(
			prop("statusCode").toString, 
			prop("statusMessage").toString,
			prop("ipAddress").toString,
			prop("countryCode").toString,
			prop("countryName").toString
		)
	}
}

case class IPInfoDbResponse (
	StatusCode: String,
	StatusMessage: String,
	IPAddress: String,
	CountryCode: String,
	CountryName: String,
	RegionName: String = "",
	CityName: String = "",
	ZipCode: String = "",
	Latitude: String = "",
	Longitude: String = "",
	TimeZone: String = ""
)

object IPInfoDbHelper {
	// API key acquired from http://ipinfodb.com
	private[this] val apiKey = "20ab8f379a5f3b9ec8d7687ab3069f1965f952ad28c449ac4b523f1183e374f3"
	private[this] val client: Client = Client.create()
	
	def getCountry(addr: String): Option[IPInfoDbResponse] = {
		val resource = client.resource(String.format("http://api.ipinfodb.com/v3/ip-country/?key=%s&ip=%s&format=json", apiKey, addr))
		JSON.parseFull(resource.get(classOf[String])) match {
			case Some(prop: AnyRef) => {
				Some(IPInfoDbResponse.buildFrom(prop.asInstanceOf[Map[String, Any]]))
			}
			case _ => None
		}
	}
}