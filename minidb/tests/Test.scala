package minidb.tests

/** A simple unit testing framework */
object Test {
  /** Set to run some code before every test */
  var setupCode: (()=>Unit) = { () => () }
  /** Set to run some code after every test */
  var finishCode: (()=>Unit) = { () => () }

  /** Runs a test that checks that b is true.
   * b is evaluated after setup code is run
   */
  def assertTrue(description: String, b: =>Boolean) {
    try {
      Test.setup()
      if (b)
        Test.testPassed(description)
      else
        Test.testFailed(description, "")
      Test.finish()
    } catch { case e => Test.testFailed(description, "", e) }
  }

  /** Runs a test that checks that v1 == v2.
   * v1 and v2 are evaluated after setup code is run (v1 before v2)
   */
  def assertEquals(description: String, v1: =>Any, v2: =>Any) {
    try {
      Test.setup()
      val vv1 = v1 // evaluate v1 now
      val vv2 = v2
      if (vv1 == vv2)
        Test.testPassed(description)
      else
        Test.testFailed(description, vv1+" != "+vv2)
      Test.finish()
    } catch { case e => Test.testFailed(description, "", e) }
  }

  /** Runs a test that checks for a thrown exception.
   * Any kind of exception is acceptable.
   */
  def assertAnyException(description: String, code: =>Any) {
    var noException = false
    try {
      Test.setup()
      val dummy = code // evaluate code now
      Test.finish()
      noException = true
    } catch {
      case _ => Test.testPassed(description)
    }
    if (noException)
      Test.testFailed(description, "No exception was produced!")
  }

  /** Starts a new category of test sets
   * by just displaying a message and changing the current category name.
   */
  def changeTestCategory(categoryName: String) {
    currentTestCategory = categoryName
    println("")
    println("----- Test category "+currentTestCategory)
    println("")
  }

  /** Starts a set of tests */
  def startTestSet(testSetName: String) {
    totalTestSets += 1
    currentTestSet = testSetName
    println("--- Starting test set #"+totalTestSets+
            " `"+currentTestSet+"'")
    passedTests = 0
    failedTests = 0
  }

  /** Finishes a set of tests by displaying statistics about them */
  def finishTestSet() {
    println("--- Finished test set #"+totalTestSets+
            " `"+currentTestSet+"': "+
            passedTests+" passed, "+failedTests+" failed.")
    println("")
    totalPassedTests += passedTests
    totalFailedTests += failedTests
    passedTests = 0
    failedTests = 0
  }

  /** Call once after all tests to display summary statistics */
  def finishAllTests() {
    println("")
    println("----- All tests finished.")
    println("A total of "+totalPassedTests+" tests passed and "+
            totalFailedTests+" tests failed in "+
            totalTestSets+" test sets.")
    println("")
  }

  /** Set to true to display information for passed tests */
  var displayPassedTests = false

  /** A name or description for the current category of test sets */
  private var currentTestCategory = ""
  /** A name or description for the current set of tests */
  private var currentTestSet = "default"  
  /** Number of successful tests in the current set */
  private var passedTests: Int = 0
  /** Number of failed tests in the current set */
  private var failedTests: Int = 0
  /** Total number of passed tests (without the current set) */
  private var totalPassedTests: Int = 0
  /** Total number of failed tests (without the current set) */
  private var totalFailedTests: Int = 0
  /** Total number of test sets run */
  private var totalTestSets: Int = 0

  private def setup() { setupCode() }
  private def finish() { finishCode() }

  private def testPassed(description: String) {
    if (displayPassedTests) {
      val testNum = passedTests + failedTests + 1
      println("*** Test #"+testNum+
              " passed in `"+currentTestSet+"': "+description)
    }
    passedTests += 1
  }

  private def testFailed(description: String, moreInfo: String) {
    val testNum = passedTests + failedTests + 1
    println("")
    println("********** TEST #"+testNum+
            " FAILED in category `"+currentTestCategory+
            "' test set `"+currentTestSet+"'")
    println("Test description: "+description)
    if (moreInfo != "") {
      print("More info: ")
      println(moreInfo)
    }
    failedTests += 1
  }

  private def testFailed(description: String,
                         moreInfo: String,
                         exception: Throwable) {
    testFailed(description, moreInfo)
    exception.printStackTrace()
  }

  /** Sample test code for testing the testing framework itself */
  def main(args: Array[String]) {
    Test.changeTestCategory("Test")

    Test.startTestSet("normal")
    Test.assertTrue("true is true", true)
    Test.assertTrue("1<2 is true", 1<2)
    Test.assertEquals("1 is 1", 1, 1)
    Test.assertEquals("1+2 is 3", 1+2, 3)
    Test.finishTestSet()

    var x: Int = 1

    Test.startTestSet("setup")
    Test.setupCode = { () => x += 1 }
    Test.assertEquals("setup code is run once", x, 2)
    Test.assertEquals("setup code is run again", x, 3)
    Test.assertEquals("setup code is run before test code",
                      { x*=2; x },
                      8)
    Test.setupCode = { () => () }
    Test.assertEquals("setup code can be removed", x, 8)

    x = 1
    Test.finishCode = { () => x += 2 }
    Test.assertEquals("finish code is not run before the test", x, 1)
    Test.assertEquals("finish code was run after the tests", x, 3)
    Test.finishCode = { () => () }
    Test.assertTrue("dummy test for testing finish code", true)
    Test.assertEquals("finish code can be removed", x, 5)
    Test.finishTestSet()

    Test.startTestSet("exceptions")
    var a = Array(1,2,3)
    Test.assertAnyException("array indexing", a(4))
    Test.finishTestSet()

    Test.finishAllTests()
  }
}
