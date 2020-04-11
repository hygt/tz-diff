import better.files.File
import cats.effect._
import cats.syntax.applicativeError._
import org.log4s.getLogger

object Main extends IOApp {

  private val log = getLogger

  /**
    * Entry-point. Outputs all differences that were found to "out.csv".
    */
  def run(args: List[String]): IO[ExitCode] = {
    val input   = "input.txt"
    val output  = File("out.csv")
    val printer = Printer.toFile(output)
    run(input, printer)
  }

  def run(input: String, printer: Printer): IO[ExitCode] = {
    Loader
      .getAll(input)
      .flatMap(pairs => Diff.process(pairs.toMap, printer))
      .map { diffs =>
        log.info(s"Found $diffs differences.")
        printer.close()
        ExitCode.Success
      }
      .recover {
        case t: Throwable =>
          log.error(t)("Program exited with an error.")
          ExitCode.Error
      }
  }

}
