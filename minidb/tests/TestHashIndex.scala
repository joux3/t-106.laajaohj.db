package minidb.tests
import minidb.storage._
import minidb.sqlexpr._

object TestHashIndex extends RunnableTest {
  val index = new HashIndex("testIndex", Seq(0))

  def testAdding {
    Test.startTestSet("Adding rows to index")
    
    index.insert(new DBKey(Seq(DBString("First"))),
        new DBRow(Seq(DBString("First"), DBString("MoreFirst"))))

    index.insert(new DBKey(Seq(DBString("Second"))),
        new DBRow(Seq(DBString("Second"), DBString("MoreSecond"))))

    index.insert(new DBKey(Seq(DBInt(0))),
        new DBRow(Seq(DBInt(0), DBString("Zero"))))

    // Causes collision with key '0'
    index.insert(new DBKey(Seq(DBInt(32))),
        new DBRow(Seq(DBInt(32), DBString("ThirtyTwo"))))

    Test.finishTestSet()
  }

  def testExactSearching {
    Test.startTestSet("Searching for exact rows")

    val r1 = index.searchExact(new DBKey(Seq(DBString("First"))))
    val r2 = index.searchExact(new DBKey(Seq(DBInt(32))))
    Test.assertTrue("Has the first row",
                      r1.contains(new DBRow(Seq(DBString("First"),
                                  DBString("MoreFirst")))))

    Test.assertTrue("Returns correct row despite collision",
                      r2.contains(new DBRow(Seq(DBInt(32),
                                  DBString("ThirtyTwo")))) &&
                      r2.contains(new DBRow(Seq(DBInt(0),
                                  DBString("Zero")))))

    Test.finishTestSet()
  }

  def runTests {
    testAdding
    testExactSearching
  }
}
