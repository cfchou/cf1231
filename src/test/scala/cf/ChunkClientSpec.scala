package cf

import java.nio.charset.{Charset, StandardCharsets}

import akka.actor.{ActorRef, ActorSystem}
import akka.io.IO
import akka.testkit.{ImplicitSender, TestKit}
import com.typesafe.config.ConfigFactory
import org.scalatest.{WordSpecLike, BeforeAndAfterAll, MustMatchers}
import spray.can.Http
import spray.http._

class ChunkClientSpec extends TestKit(ActorSystem("ChunkClientSpec"))
  with ImplicitSender with WordSpecLike with MustMatchers
  with BeforeAndAfterAll {

  var peer: Option[ActorRef] = None

  val testString = "A" + "\u00ea" + "\u00f1" + "\u00fc" + "C"
  val testJson = """{
    | "info": "some info",
    | "events": [ {
    |   "id": "id0001",
    |   "type": "EVENT_A",
    |   "time": "1997-07-16T19:20:30.45+0100",
    |   "payload": "{\"value\": 123 }"
    | }, {
    |   "id": "id0002",
    |   "type": "EVENT_B",
    |   "time": "1997-07-16T19:20:50.45+0100",
    |   "payload": "{\"name\": \"whatever\", \"value\": 123 }"
    | }, {
    |   "id": "id0003",
    |   "type": "EVENT_A",
    |   "time": "1997-07-16T19:20:50.45Z",
    |   "payload": "{\"name\": \"what\" }"
    | } ]
    |}""".stripMargin

  override protected def afterAll(): Unit = {
    system.shutdown()
  }

  "FireApp " must {

    "allow clients to connect" in {
      val conf = ConfigFactory.load()
      val inf = conf.getString("fire.interface")
      val prt = conf.getInt("fire.port")

      IO(Http) ! Http.Connect(inf, prt)
      val res = expectMsgType[Http.Connected]
      peer = Some(lastSender)
      println(s"received $res")
    }

    "receive the length of testString(in utf8) " when {
      println(s"default: ${Charset.defaultCharset().toString}")
      // we can't be sure that defaultCharset must be UTF-8
      val len = new String(testString.getBytes,
        StandardCharsets.UTF_8).length
      // we can't be sure that defaultCharset must be UTF-8
      val lenJson = new String(testJson.getBytes,
        StandardCharsets.UTF_8).length


      "sending a request" in {
        // HttpEntity(String) sends ContentTypes.`text/plain(UTF-8)` and encodes
        // the given string in UTF-8
        peer.get ! HttpRequest(HttpMethods.POST, "/",
          entity = HttpEntity(testJson))
        val res = expectMsgType[HttpResponse]
        val resLen = res.entity.data.asString(HttpCharsets.`UTF-8`).toInt
        println(s"received $lenJson, $resLen")
        resLen must equal (lenJson)
      }

      /*
      "sending a chunked request" in {
        val numTestString = 3
        val totalLen = numTestString * len
        val target = peer.get

        target ! ChunkedRequestStart(HttpRequest(HttpMethods.POST, "/"))
        (1 to numTestString) foreach { _ =>
          target ! MessageChunk(testString)
        }
        target ! ChunkedMessageEnd

        val res = expectMsgType[HttpResponse]
        val resLen = res.entity.data.asString(HttpCharsets.`UTF-8`).toInt
        resLen must equal (totalLen)
      }
      */
    }

  }
}
