import cats.effect.ExitCode
import munit.FunSuite

class MainTest extends FunSuite {
  test("run") {
    val run = Main.run("test.txt", Printer.toLogger).unsafeRunSync()
    assertEquals(run, ExitCode.Success)
  }
}
