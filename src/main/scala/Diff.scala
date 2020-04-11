import cats.effect.IO
import cats.instances.list._
import cats.syntax.traverse._
import org.log4s.getLogger
import kuyfi.TZDB._
import shapeless.{Inl, Inr}

object Diff {

  private val log  = getLogger
  private val year = """\d{4}""".r

  /**
    * Finds all differences between contiguous TZ versions.
    * Skips rules that are more than 5 years in the past or 10 years in the future.
    *
    * @param map     a collection of TZ versions and their respective rows
    * @param printer a CSV printer
    * @return the number of differences found
    */
  def process(map: Map[String, List[Row]], printer: Printer): IO[Int] = printer.writeHeader().flatMap { _ =>
    val keys  = map.keys.toList.sorted
    val pairs = keys.zip(keys.tail)
    val min   = year.findFirstIn(keys.head).map(_.toInt - 5).getOrElse(2000)
    val max   = year.findFirstIn(keys.last).map(_.toInt + 10).getOrElse(2040)
    log.debug(s"Skipping rules outside [$min,$max]")
    pairs
      .flatMap {
        case (prev, cur) =>
          val prevRows = map(prev).filterNot(commentOrBlankLine)
          val curRows  = map(cur).filterNot(commentOrBlankLine)
          prevRows
            .diff(curRows)
            .distinct
            .filterNot(row => outOfBounds(row, min, max))
            .map(row => SimpleRow(prev, cur, row, prevRows ++ curRows))
            .distinct
            .map(printer.write)
      }
      .sequence
      .map(_.size)
  }

  private def commentOrBlankLine(row: Row): Boolean =
    row.select[Comment].isDefined || row.select[BlankLine].isDefined

  private def outOfBounds(row: Row, min: Int, max: Int): Boolean = row.select[Rule] match {
    case None       => false
    case Some(rule) => rule.endYear < min || rule.startYear > max
  }

  final case class SimpleRow(prev: String, cur: String, tpe: String, tzName: String, rowName: String)

  object SimpleRow {

    /**
      * Converts a Row into a SimpleRow for two given TZ versions.
      * Only rows of type Link, Rule and Zone are supported. This constructor throws otherwise.
      *
      * @throws IllegalArgumentException
      */
    def apply(prev: String, cur: String, row: Row, rows: List[Row]): SimpleRow = row match {
      case Inr(Inr(Inl(link)))           => SimpleRow(prev, cur, "Link", link.to, link.from)
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
