import java.io.{BufferedInputStream, BufferedOutputStream, ByteArrayInputStream, FileOutputStream}

import better.files._
import cats.effect.IO._
import cats.effect.{ContextShift, IO, Resource => IOResource}
import cats.implicits._
import org.log4s.getLogger
import kuyfi.TZDB.Row
import kuyfi.TZDBParser
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream
import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream

object Loader {

  private val log    = getLogger
  private val prefix = "https://data.iana.org/time-zones/releases/"
  private val suffix = ".tar.gz"

  /**
    * Fetches all TZ data archives that are listed in the input file.
    * Each version is then parsed into a list of rows.
    *
    * @param fileName the input file
    * @return pairs of TZ version strings to their parsed rows
    */
  def getAll(fileName: String)(implicit cs: ContextShift[IO]): IO[List[(String, List[Row])]] = {
    IOResource.make(IO(File.newTemporaryDirectory("tzdiff")))(cleanup).use { tmpDir =>
      readInput(fileName).flatMap { versions =>
        versions
          .map(version => process(tmpDir / version, version))
          .parSequence
      }
    }
  }

  private def readInput(fileName: String): IO[List[String]] = IO {
    Resource.getAsStream(fileName).lines.toList
  }

  private def process(dest: File, version: String): IO[(String, List[Row])] = {
    fetch(version)
      .flatMap(tgz => gunzipTar(tgz, dest))
      .flatMap(_ => TZDBParser.parseAll(dest))
      .map(rows => version -> rows)
  }

  private def fetch(version: String): IO[Array[Byte]] = IO {
    val url = s"$prefix$version$suffix"
    log.debug(s"Fetching $url")
    requests.get(url).data.array
  }

  private def gunzipTar(bytes: Array[Byte], dest: File): IO[Unit] = IO {
    dest.createDirectoryIfNotExists(true)

    val bis      = new BufferedInputStream(new ByteArrayInputStream(bytes))
    val tarIn    = new TarArchiveInputStream(new GzipCompressorInputStream(bis))
    var tarEntry = tarIn.getNextTarEntry

    while (tarEntry != null) {
      val file = dest / tarEntry.getName
      if (tarEntry.isDirectory) {
        file.createDirectoryIfNotExists(true)
      } else {
        file.parent.createDirectoryIfNotExists(true)
        file.createFile()
        var btoRead = new Array[Byte](1024)
        val bout    = new BufferedOutputStream(new FileOutputStream(file.toJava))
        var len     = 0
        len = tarIn.read(btoRead)
        while (len != -1) {
          bout.write(btoRead, 0, len)
          len = tarIn.read(btoRead)
        }
        bout.close()
        btoRead = null
      }
      tarEntry = tarIn.getNextTarEntry
    }
    tarIn.close()
  }

  private def cleanup(dir: File): IO[Unit] = IO {
    try {
      log.debug(s"Cleaning up temporary directory $dir")
      dir.clear()
      dir.delete()
    } catch {
      case _: Throwable => log.debug("Something went wrong.")
    }
  }
}
