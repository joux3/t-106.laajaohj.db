package minidb.tests
import minidb.storage._
import minidb.sqlexpr._

object TestRBTreeIndex extends RunnableTest {
  val index = new RedBlackTreeIndex("testIndex", Seq(0));
  val testCount = 10000

  def testAdding {
    Test.startTestSet("adding rows to index")
    // insert in order to test balancing
    1.to(testCount).foreach {x =>
      index.insert(new DBRow(Seq(DBInt(x), DBString(x.toString))))
      index.insert(new DBRow(Seq(DBInt(x), DBString(x.toString.reverse))))
    }
    Test.finishTestSet()
  }

  def testExactSearching {
    Test.startTestSet("searching exactly")
    1.to(testCount).foreach {x =>
      val r = index.searchExact(new DBKey(Seq(DBInt(x))))
      Test.assertEquals("result count", 2, r.size)
      Test.assertTrue("has correct rows", (r.contains(new DBRow(Seq(DBInt(x), DBString(x.toString))))) 
                                     && (r.contains(new DBRow(Seq(DBInt(x), DBString(x.toString.reverse))))))
    }
    Test.finishTestSet()
  }

  def testRangeQuery {
    Test.startTestSet("testing range query")
    val startKey = new DBKey(Seq(DBInt(testCount/3)))
    val endKey = new DBKey(Seq(DBInt(testCount/3 * 2)))
    index.searchRange(startKey, endKey, false, false).foreach {row =>
      val curKey = new DBKey(Seq(row(0)))
      Test.assertTrue("row in the range", (startKey < curKey) 
                                          && (curKey < endKey))
    }
    val sizeLow = index.searchRange(startKey, endKey, false, false).size
    val sizeMed1 = index.searchRange(startKey, endKey, true, false).size
    val sizeMed2 = index.searchRange(startKey, endKey, false, true).size
    val sizeHigh = index.searchRange(startKey, endKey, true, true).size
    Test.assertTrue("range size 1", sizeLow < sizeMed1)
    Test.assertTrue("range size 2", sizeMed1 == sizeMed2)
    Test.assertTrue("range size 2", sizeMed1 < sizeHigh)
    Test.finishTestSet()
  }

  def runTests {
    testAdding
    testExactSearching
    testRangeQuery
  }
}
