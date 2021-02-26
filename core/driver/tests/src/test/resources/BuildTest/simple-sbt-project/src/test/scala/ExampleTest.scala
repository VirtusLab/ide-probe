import org.junit.Ignore
import org.junit.Test

class ExampleTest {
  @Test def testA(): Unit = {
    assert(true)
  }

  @Test def testB(): Unit = {
    assert(false)
  }

  @Test @Ignore def testC(): Unit = {
    assert(true)
  }

  @Test def testD(): Unit = {
    throw new OutOfMemoryError()
  }


}