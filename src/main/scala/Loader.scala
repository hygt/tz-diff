import java.io.{BufferedInputStream, BufferedOutputStream, ByteArrayInputStream, FileOutputStream, File => JFile}

import better.files._
import cats.effect.IO
import cats.instances.list._
import cats.syntax.traverse._
import journal.Logger
import kuyfi.TZDB.Row
import kuyfi.TZDBParser
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream
import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream

object Loader {

  private val log    = Logger("tz-diff")
  private val prefix = "https://data.iana.org/time-zones/releases/"
  private val suffix = ".tar.gz"

  /**
    * Fetches all TZ data archives from IANA's website according to the list in "input.txt".
    * Each version is then parsed into a list of rows.
    *
    * @param tmpDir a temporary folder where archives will be downloaded and unpacked
    * @return pairs of TZ version strings to their parsed rows
    */
  def getAll(tmpDir: File): IO[List[(String, List[Row])]] = {
    readInput.flatMap { versions =>
      versions
        .map(version => process(tmpDir / version, version))
        .sequence
    }
  }

  private def readInput: IO[List[String]] = IO {
    Resource.getAsStream("input.txt").lines.toList
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
    requests.get(url).data.bytes
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
}
