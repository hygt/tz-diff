import java.io.Writer

import Diff.SimpleRow
import better.files.File
import cats.effect.IO
import com.github.tototoshi.csv.CSVWriter
import org.log4s.getLogger

class Printer private (writer: CSVWriter) {

  def writeHeader(): IO[Unit] =
    IO(writer.writeRow(List("Previous", "Current", "Type", "TzName", "RowName")))

  def write(row: SimpleRow): IO[Unit] =
    IO(writer.writeRow(row.productIterator.toList))

  def close(): IO[Unit] = IO(writer.close())

}

object Printer {

  private val log = getLogger

  /**
    * @return a Printer that writes to the given file.
    */
  def toFile(file: File): Printer = new Printer(CSVWriter.open(file.toJava))

  /**
    * @return a Printer that writes to the system's standard output.
    */
  def toStdout: Printer = new Printer(CSVWriter.open(System.out))

  /**
    * @return a Printer that writes to the default logger
    */
  def toLogger: Printer = new Printer(CSVWriter.open(new LogWriter))

  private class LogWriter extends Writer {
    val buffer = new StringBuilder

    def write(cbuf: Array[Char], off: Int, len: Int): Unit = {
      buffer.appendAll(cbuf, off, len)
      ()
    }

    def flush(): Unit = {
      log.info(buffer.toString.trim)
      buffer.clear()
    }

    def close(): Unit =
      flush()
  }
}
