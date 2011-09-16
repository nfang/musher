package me.nfang.musher

import org.scalatra._
import scala.xml._
import scalate.ScalateSupport
import org.joda.time._
import javax.servlet._

class MusherFilter extends ScalatraFilter with ScalateSupport {
  get("/musher") {
	templateEngine.layout("/WEB-INF/templates/index.jade", createRenderContext(request, response))
  }
  
  get("/musher/*") {
	redirect("/musher");
  }
  
  get("/musher/:hash") {
	val hash = params("hash")
	if (hash.endsWith("+")) {
		var metadata = Musher.getMetadata(hash.substring(0, 6))
		metadata match {
			case Some(md) => {
				contentType = "text/html"
				templateEngine.layout("/WEB-INF/templates/dashboard.jade", Map("metadata" -> md))
			}
			case _ => templateEngine.layout("/WEB-INF/templates/404.jade", createRenderContext(request, response))
		}
	} else {
		val referer = request.getHeader("Referer")
		val original = Musher.expand(hash, if (referer == null || referer == "") "direct" else referer, request.getRemoteAddr, new DateTime(DateTimeZone.UTC))
		original match {
			case Some(url: String) => redirect(url)
			case _ => templateEngine.layout("/WEB-INF/templates/404.jade", createRenderContext(request, response))
		}
	}
  }
  
  post("/dashboard") {
	if(request.isAjax && params("hash") != null && params("hash") != "") Musher.analyze(params("hash")) else ""
  }
  
  post("/musher") {
	if(request.isAjax) {
		Configuration.getString("DOMAIN") + Musher.hash(params("url"))
	}
  }
  
  /*error {
	templateEngine.layout("/WEB-INF/templates/error.jade", createRenderContext(request, response))
  }*/
}
