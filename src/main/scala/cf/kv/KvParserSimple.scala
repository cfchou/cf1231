package cf.kv

import akka.actor.{ActorLogging, Props}
import kafka.producer.KeyedMessage
import org.joda.time.{LocalDate, DateTime}
import play.api.libs.json._
import play.api.libs.functional.syntax._
import grizzled.slf4j.Logger

/*
example:
{
  "info" = "some info",
  "events" = [ {
    "id" = "id1 is a string",
    "type" = "type1 is a string",
    # "time" = 1415241385529,
    "time" = 1997-07-16T19:20:30.45+0100,
    "payload" = "{\"value\": 123 }"
  }, {
    "id" = "id2 is a string",
    "type" = "type2 is a string",
    "time" = 1997-07-16T19:20:30.45+0100,
    "payload" = "{\"name\": \"whatever\", \"value\": 123 }"
  } ]
}
*/

object KvParserSimple {

  implicit val SimpleEventReads = {
    val jodaReads = Reads.jodaDateReads("yyyy-MM-dd'T'HH:mm:ss.sssZ", identity)
    (
      (JsPath \ "id").read[String] and
        (JsPath \ "type").read[String] and
        (JsPath \ "time").read[DateTime](jodaReads) and
        (JsPath \ "payload").read[String]
      )(SimpleEvent.apply _)
  }

  implicit val SimpleEventWrites = {
    val jodaWrites = Writes.jodaDateWrites("yyyy-MM-dd'T'HH:mm:ss.sssZ")
    (
      (JsPath \ "id").write[String] and
        (JsPath \ "type").write[String] and
        (JsPath \ "time").write[DateTime](jodaWrites) and
        (JsPath \ "payload").write[String]
      )(unlift(SimpleEvent.unapply))
  }

  sealed case class SimpleEvent(id: String, event_type: String,
                                            time: DateTime, payload: String)

}

trait KvParserSimple extends KvParser[String, String] {

  import KvParserSimple._

  private val log = Logger[this.type]
  log.info("* * * * * KvParser...")

  override def parseMessages(content: String)
  : Seq[KeyedMessage[String, String]] = {
    val json = Json.parse(content)
    val events = getEvents(json)
    log.info(s"parseMessages ${events.length}")

    events.map { ev =>
      KeyedMessage[String, String](ev.event_type, ev.event_type,
        ev.event_type, Json.stringify(Json.toJson(ev)))
    }
  }

  private def getEvents(content: String): Seq[SimpleEvent] = {
    val json = Json.parse(content)
    getEvents(json)
  }

  private def getEvents(json: JsValue): Seq[SimpleEvent] = {
    val eventJson = json \ "events"

    eventJson.validate[Seq[SimpleEvent]] match {
      case s: JsSuccess[Seq[SimpleEvent]] => s.get
      case e: JsError => {
        log.error(e)
        Seq()
      }
    }
  }
}


