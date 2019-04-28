import better.files.File
import cats.effect.IO
import journal.Logger
import kuyfi.TZDB.Row

object Main {

  private val log = Logger("tz-diff")

  /**
    * Entry-point. Outputs all differences that were found to "out.csv".
    */
  def main(args: Array[String]): Unit = {
    File.usingTemporaryDirectory() { dir =>
      Loader.getAll(dir).flatMap(print).unsafeRunAsync(callBack)
    }
  }

  private def print(pairs: List[(String, List[Row])]): IO[Unit] = {
    val output  = File("out.csv")
    val printer = Printer(output)
    Diff
      .process(pairs.toMap, printer)
      .bracket { diffs =>
        IO(log.info(s"Found $diffs differences"))
      } { _ =>
        printer.close()
      }
  }

  private def callBack(result: Either[Throwable, Unit]): Unit = result match {
    case Right(_) => log.info("Done.")
    case Left(e)  => log.error("Program exited with an error.", e)
  }

}
