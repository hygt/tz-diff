import cats.effect.IO
import cats.instances.list._
import cats.syntax.traverse._
import kuyfi.TZDB._
import shapeless.{Inl, Inr}

object Diff {

  /**
    * Finds all differences between contiguous TZ versions.
    *
    * @param map     a collection of TZ versions and their respective rows
    * @param printer a CSV printer
    * @return the number of differences found
    */
  def process(map: Map[String, List[Row]], printer: Printer): IO[Int] = printer.writeHeader().flatMap { _ =>
    val keys  = map.keys.toList.sorted
    val pairs = keys.zip(keys.tail)
    pairs
      .flatMap {
        case (prev, cur) =>
          val prevRows = map(prev).filterNot(isCommentOrBlankLine)
          val curRows  = map(cur).filterNot(isCommentOrBlankLine)
          prevRows
            .diff(curRows)
            .distinct
            .map(row => SimpleRow(prev, cur, row, prevRows ++ curRows))
            .distinct
            .map(printer.write)
      }
      .sequence
      .map(_.size)
  }

  private def isCommentOrBlankLine(row: Row): Boolean =
    row.select[Comment].isDefined || row.select[BlankLine].isDefined

  final case class SimpleRow(prev: String, cur: String, tpe: String, tzName: String, rowName: String)

  object SimpleRow {

    /**
      * Converts a Row into a SimpleRow for two given TZ versions.
      * Only rows of type Link, Rule and Zone are supported. This constructor throws otherwise.
      *
      * @throws IllegalArgumentException
      */
    def apply(prev: String, cur: String, row: Row, rows: List[Row]): SimpleRow = row match {
      case Inr(Inr(Inl(link)))           => SimpleRow(prev, cur, "Link", link.from, link.to)
      case Inr(Inr(Inr(Inl(rule))))      => SimpleRow(prev, cur, "Rule", lookup(row, rows), rule.name)
      case Inr(Inr(Inr(Inr(Inl(zone))))) => SimpleRow(prev, cur, "Zone", zone.name, zone.name)
      case _                             => throw new IllegalArgumentException
    }
  }

  private def lookup(rule: Row, rows: List[Row]): String =
    rows
      .dropWhile(_ != rule)
      .collectFirst {
        case Inr(Inr(Inr(Inr(Inl(zone))))) => zone.name
      }
      .getOrElse(throw new IllegalStateException)

}
