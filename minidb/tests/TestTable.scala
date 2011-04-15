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
  def testForeignKeyConstraint() { 
    Test.startTestSet("Testing adding tables with FOREIGN KEY constraint")
    
    val reftable = Table.create("reftable", "heap", Seq(("0", DBTypeInt), ("1", DBTypeInt), ("2", DBTypeText), ("3", DBTypeText), ("4", DBTypeText)), Seq(TCPrimaryKey(List("0", "2")))) 
    reftable.createIndex("_primarykey_fortest", Index.defaultIndexType, Seq("0", "2"))
    
    Test.assertNoException("Should be OK",
     Table.create("tableref", "heap", Seq(("1", DBTypeInt), ("2", DBTypeInt), ("3", DBTypeText), ("4", DBTypeText), ("5", DBTypeText)), Seq(TCPrimaryKey(List("2")), TCForeignKey(List("1", "4"), "reftable", List("0", "2")))))

    Test.assertAnyException("Trying to create table with invalid FOREIGN KEY, 1",
     Table.create("tableref1", "heap", Seq(("1", DBTypeInt), ("2", DBTypeInt), ("3", DBTypeText), ("4", DBTypeText), ("5", DBTypeText)), Seq(TCPrimaryKey(List("2")), TCForeignKey(List("4"), "reftable", List("0", "2")))))

    Test.assertAnyException("Trying to create table with invalid FOREIGN KEY, 2",
     Table.create("tableref1", "heap", Seq(("1", DBTypeInt), ("2", DBTypeInt), ("3", DBTypeText), ("4", DBTypeText), ("5", DBTypeText)), Seq(TCPrimaryKey(List("2")), TCForeignKey(List("1", "4"), "reftables", List("0", "2")))))

    Test.assertAnyException("Trying to create table with invalid FOREIGN KEY, 3",
     Table.create("tableref1", "heap", Seq(("1", DBTypeInt), ("2", DBTypeInt), ("3", DBTypeText), ("4", DBTypeText), ("5", DBTypeText)), Seq(TCPrimaryKey(List("2")), TCForeignKey(List("1", "4"), "reftable", List("2")))))

    Test.finishTestSet()
  }

  def runTests {
    testUsableIndexes()
    testForeignKeyConstraint()
  }
}
