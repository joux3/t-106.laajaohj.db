package minidb.tests
import minidb.storage._
import minidb.sqlexpr._

object TestTable extends RunnableTest {
  def testUsableIndexes() {
    Test.startTestSet("Testing usable indexes")
    val table = Table.create("testtable", "heap",
      Seq(("0", DBTypeInt), ("1", DBTypeInt), ("2", DBTypeText), ("3", DBTypeText), ("4", DBTypeText)), Seq())
    table.createIndex("testindex", Index.defaultIndexType, Seq("0", "1", "2", "4"))
    table.createIndex("testindex1", Index.defaultIndexType, Seq("1", "2"))
    table.createIndex("testindex2", Index.defaultIndexType, Seq("1", "2", "4"))
    table.createIndex("testindex3", Index.defaultIndexType, Seq("0"))
    var r = table.getUsableIndexes(Seq(VField("", "0")))
    Test.assertTrue("usable indexes1", r.size == 1)
    Test.assertTrue("usable indexes2", r(0).columnNums.size == 1)
    r = table.getUsableIndexes(Seq(VField("", "0"), VField("", "4")))
    Test.assertTrue("usable indexes3", r.size == 1)
    Test.assertTrue("usable indexes4", r(0).columnNums.size == 1)
    r = table.getUsableIndexes(Seq(VField("", "0"), VField("", "1"), VField("", "2"), VField("", "4")))
    Test.assertTrue("usable indexes3", r.size == 4)
    Test.assertTrue("usable indexes4", r(0).columnNums.size == 4)
    Test.finishTestSet()
  }

  def runTests {
    testUsableIndexes()
  }
}
