import Diff.SimpleRow
import better.files.File
import cats.effect.IO
import com.github.tototoshi.csv.CSVWriter

class Printer private (writer: CSVWriter) {

  def writeHeader(): IO[Unit] =
    IO(writer.writeRow(List("Previous", "Current", "Type", "TzName", "RowName")))

  def write(row: SimpleRow): IO[Unit] =
    IO(writer.writeRow(row.productIterator.toList))

  def close(): IO[Unit] = IO(writer.close())

}

object Printer {

  /**
    * @return a Printer instance that outputs to the given file.
    */
  def apply(file: File): Printer = new Printer(CSVWriter.open(file.toJava))

  /**
    * @return a Printer instance to the system's standard output.
    */
  def stdout(): Printer = new Printer(CSVWriter.open(System.out))
}
