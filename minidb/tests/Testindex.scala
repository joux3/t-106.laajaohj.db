package minidb.tests

import scala.util.Random
import minidb.storage._
import minidb.sqlexpr._


object Testindex extends RunnableTest {
  
  
  def simpleSearch(index: Index)
  {
    val TEST_SIZE = 10240;
    val RANDOM_MAX_VALUE = 1024;
    def containsAllRows() = {
      (0 to TEST_SIZE).foldLeft(true) {
        (memo, curValue) => memo && index.
          searchExact(new DBKey(Seq(DBInt(curValue)))).size == 1 
      } 
    }
    def totalRowCount() = {
      (0 to RANDOM_MAX_VALUE).foldLeft(0) {
        (count, curValue) => 
          count + index.searchExact(new DBKey(Seq(DBInt(curValue)))).size
      }
    }
    
    Test.startTestSet(index.indexName + ": simple search")
    
    //Test empty
    Test.assertEquals("searchExact DBInt in empty index",
        index.searchExact(new DBKey(Seq(DBInt(123)))).size,0)
    
    //Populate index
    (0 to TEST_SIZE) foreach {
      x => {
        index.insert(new DBRow(Seq(DBInt(x))))
      }
    }
    Test.assertEquals("searchExact DBInt",containsAllRows(),true)
    
    //Test non-existent
    Test.assertEquals("searchExact DBInt expecting no results",
        index.searchExact(new DBKey(Seq(DBInt(-10)))).size,0)
    
    
    //Populate index (reversed)
    index.clear()
    (TEST_SIZE to 0 by -1) foreach {
      x => {
        index.insert(new DBRow(Seq(DBInt(x))))
      }
    }
    Test.assertEquals("searchExact DBInt (reverse index insert)",containsAllRows(),true)
    
    //Populate index (pseudo random, fixed seed)
    index.clear()
    val generator = new Random(4224)
    for (i <- (0 to TEST_SIZE)) {
        index.insert(new DBRow(Seq(DBInt(generator.nextInt(RANDOM_MAX_VALUE)))))
    }
     Test.assertEquals("searchExact DBInt (random index insert)",totalRowCount(),TEST_SIZE+1)
    
    Test.finishTestSet()
  }
  def simpleDuplicateSearch(index: Index)
  {
    val TEST_SIZE = 10240;
    Test.startTestSet(index.indexName + ": simple duplicate search")
    
    //Populate index, even numbers are duplicates
    (0 to TEST_SIZE) foreach {
      x => {
        index.insert(new DBRow(Seq(DBInt(x))))
      }
    }
    (0 to TEST_SIZE by 2) foreach {
      x => {
        index.insert(new DBRow(Seq(DBInt(x))))
      }
    }
    
    //Find rows
    val foundAll = (0 to TEST_SIZE).foldLeft(true) {
      (memo, curValue) => {
        if (memo)
        {
          val expected =
            if (curValue % 2 == 0)
              2
            else
              1
          index.searchExact(new DBKey(Seq(DBInt(curValue)))).size == expected
        }
        else
          false
      }
    }
    Test.assertEquals("searchExact DBInt with duplicates",foundAll,true)
    
    //Populate index with 0 to TEST_SIZE 
    var expectedCount = 1
    index.clear()
    (0 to TEST_SIZE) foreach {
      x => {
        index.insert(new DBRow(Seq(DBInt(x))))
      }
    }
    //and DBInt(6) (TEST_SIZE/2) times
    (0 to TEST_SIZE by 2) foreach {
      x => {
        index.insert(new DBRow(Seq(DBInt(6))))
        expectedCount += 1
      }
    }
    Test.assertEquals("searchExact DBInt with many duplicates",
        index.searchExact(new DBKey(Seq(DBInt(6)))).size,expectedCount)
    
    Test.finishTestSet()
  }
  def allDuplicatesSearch(index: Index)
  {
    val TEST_SIZE = 10240;
    Test.startTestSet(index.indexName + ": find all duplicates")
    
    for (x <- (0 to TEST_SIZE)){
      index.insert(new DBRow(Seq(DBInt(6))))
    }
    
    Test.assertEquals("searchExact DBInt with only duplicates",
        index.searchExact(new DBKey(Seq(DBInt(6)))).size,TEST_SIZE+1)
    
    Test.finishTestSet()
  }
  def simpleRangeTest(index: Index)
  {
    val TEST_SIZE = 10240;
    Test.startTestSet(index.indexName + ": simple range search")
    
    //Populate index
    for (x <- (0 to TEST_SIZE)){
      index.insert(new DBRow(Seq(DBInt(x))))
    }
    
    val testRanges = List( (10,0),(0,0),(0,1),(0,10),(0,1000),(20,1032),(23,8131),(TEST_SIZE-1,TEST_SIZE))
    val inclusives = List( (false,false),(false,true),(true,false),(true,true))
    for (range <- testRanges) 
    for (inclusive <- inclusives) {
      val expected = 
        //invalid range
        if (range._2 < range._1)
          0
        //range is (x,x] or [x,x) 
        else if (range._2 == range._1 && (inclusive._1||inclusive._2))
          1
        //valid range
        else
          math.max(
          range._2 - range._1 - 1 +
            (if (inclusive._1) 1 else 0) +
            (if (inclusive._2) 1 else 0),
          0)
        
      val bracket_open = if (inclusive._1) "[" else "("
      val bracket_close = if (inclusive._2) "]" else ")"
        
      Test.assertEquals("searchRange DBInt, " + bracket_open + range._1 + "-" + range._2 + bracket_close + " expecting " + expected,
        index.searchRange(
            new DBKey(Seq(DBInt(range._1))),
            new DBKey(Seq(DBInt(range._2))),
            inclusive._1,
            inclusive._2
            ).size,expected)
    }
    
    
    Test.finishTestSet()
  }
  
  def testIndex(factory: () => (Index))
  {
    simpleSearch(factory())
    simpleDuplicateSearch(factory())
    allDuplicatesSearch(factory())
    
    if (factory().supportsRangeSearch)
    {
      simpleRangeTest(factory())
    }
  }
  def runTests()
  {
    testIndex( () => {new PrimitiveHashIndex("PrimitiveHash",Seq(0))})
    testIndex( () => {new AvlTreeIndex("AvlTree",Seq(0))})
    testIndex( () => {new RedBlackTreeIndex("RedBlackTree",Seq(0))})
  }
}