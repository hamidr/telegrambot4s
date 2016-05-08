package info.mukel.telegram.bots.v2

import akka.stream.scaladsl.Source
import info.mukel.telegram.bots.v2.methods.GetUpdates
import info.mukel.telegram.bots.v2.api.Implicits._
import info.mukel.telegram.bots.v2.model.Update

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits._

/**
  * Created by mukel on 5/4/16.
  */
trait Polling {
  self: TelegramBot =>

  type OffsetUpdates = Future[(Long, Array[Update])]

  val seed = Future.successful((0L, Array.empty[Update]))

  val iterator = Iterator.iterate[OffsetUpdates](seed) {
    _ flatMap {
      case (prevOffset, prevUpdates) =>
        val curOffset = prevUpdates
          .map(_.updateId)
          .fold(prevOffset)(_ max _)

        api.request(GetUpdates(curOffset + 1, timeout = 20)) map { (curOffset, _) }
    }
  }

  val updateGroups =
    Source.fromIterator(() => iterator)
      .mapAsync(1)(identity)
      .map(_._2)

  override val updates = updateGroups.mapConcat(x => scala.collection.immutable.Seq[Update](x: _*))
}
