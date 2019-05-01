import better.files.File
import cats.effect._
import cats.syntax.applicativeError._
import journal.Logger

object Main extends IOApp {

  private val log = Logger("tz-diff")

  /**
    * Entry-point. Outputs all differences that were found to "out.csv".
    */
  def run(args: List[String]): IO[ExitCode] = {
    val dir     = File.newTemporaryDirectory("tzdiff")
    val output  = File("out.csv")
    val printer = Printer(output)
    Loader
      .getAll("input.txt", dir)
      .flatMap(pairs => Diff.process(pairs.toMap, printer))
      .map { diffs =>
        log.info(s"Found $diffs differences.")
        printer.close()
        cleanup(dir)
        ExitCode.Success
      }
      .recover {
        case t: Throwable =>
          log.error("Program exited with an error.", t)
          cleanup(dir)
          ExitCode.Error
      }
  }

  private def cleanup(dir: File): Unit =
    try {
      log.debug(s"Cleaning up temporary directory $dir")
      dir.clear()
      dir.delete()
    } catch {
      case t: Throwable => log.debug("Something went wrong.")
    }

}
