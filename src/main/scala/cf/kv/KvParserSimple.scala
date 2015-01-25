package cf.kv

import kafka.producer.KeyedMessage
import org.joda.time.DateTime
import play.api.libs.json._
import play.api.libs.functional.syntax._
import grizzled.slf4j.Logger

/*
example:
{
  "info" = "some info",
  "events" = [ {
    "id" = "id1 is a string"
    "type" = "type1 is a string"
    # "time" = 1415241385529,
    "time" = 1997-07-16T19:20:30.45+01:00
    "payload" = "{\"value\": 123 }",
  }, {
    "id" = "id2 is a string"
    "type" = "type2 is a string"
    "time" = 1997-07-16T19:20:30.45+01:00
    "payload" = "{\"name\": \"whatever\", \"value\": 123 }",
  } ]
}
*/

object SimpleEvent {
  implicit val SimpleEventReads = {
    val jodaReads = Reads.jodaDateReads("yyyy-MM-ddTHH:mm:ss.sssZZ", identity)
    (
      (JsPath \ "id").read[String] and
        (JsPath \ "type").read[String] and
        (JsPath \ "time").read[DateTime](jodaReads) and
        (JsPath \ "payload").read[String]
      )(SimpleEvent.apply _)
  }

  implicit val SimpleEventWrites = {
    val jodaWrites = Writes.jodaDateWrites("yyyy-MM-ddTHH:mm:ss.sssZZ")
    (
      (JsPath \ "id").write[String] and
        (JsPath \ "type").write[String] and
        (JsPath \ "time").write[DateTime](jodaWrites) and
        (JsPath \ "payload").write[String]
      )(unlift(SimpleEvent.unapply))
  }
}

sealed case class SimpleEvent(id: String, event_type: String, time: DateTime,
                              payload: String)

class KvParserSimple() {

  import SimpleEvent.{SimpleEventReads, SimpleEventWrites}

  val log = Logger[this.type]

  def parseMessages(content: String): Seq[KeyedMessage[String, String]] = {
    val json = Json.parse(content)
    val events = getEvents(json)
    log.debug(s"${events.length}")

    events.map { ev =>
      KeyedMessage[String, String](ev.event_type, ev.event_type,
        ev.event_type, Json.stringify(Json.toJson(ev)))
    }
  }

  def getEvents(content: String): Seq[SimpleEvent] = {
    val json = Json.parse(content)
    getEvents(json)
  }

  def getEvents(json: JsValue): Seq[SimpleEvent] = {
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
