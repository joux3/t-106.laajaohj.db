package minidb.tests

/** Abstract superclass for test objects run by RunTests */
abstract class RunnableTest {
  def runTests()
}

/** Main program for running tests
 * "scala minidb.tests.RunTests" to run all tests
 * "scala minidb.tests.RunTests sqlexpr xx" to run only these categories
 */
object RunTests {
  val categories: Map[String, RunnableTest] =
    Map(("parser" -> Testparser),
        ("sqlexpr" -> Testsqlexpr),
        ("queryproc" -> Testqueryproc),
        ("rbtreeindex" -> TestRBTreeIndex),
        ("hashindex" -> TestHashIndex),
        ("testtable" -> TestTable))

  def main(args: Array[String]) {
    val selectedCategories =
      if (args.size == 0) categories
      else categories filter { c => args contains c._1 }

    selectedCategories foreach {
      _ match {
        case (name, testobj) => {
          Test.changeTestCategory(name)
          testobj.runTests()
        }
      }
    }

    Test.finishAllTests()
  }
}
