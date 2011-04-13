package minidb.tests
import minidb._
import minidb.journal._
import minidb.sqlexpr._
import minidb.queryproc._
import java.io._

object TestJournal extends RunnableTest {
  def testTransactions() {
    //ToDo: this test will destroy current journal file
    
    //Stupid test, doesn't test just journal
    Test.startTestSet("Testing journal writing")
    //Clear journal file
    val a = new DataOutputStream(new FileOutputStream(Journal.journalFileName))
    
    Journal.loadTransactionFromFile()
    
    def lines = scala.io.Source.fromFile(Journal.journalFileName).getLines.toArray

    Journal.beginTransaction
    Journal.reportCommand("empty",BeginTransaction)
    Journal.reportCommand("empty",CommitTransaction)
    Journal.reportCommand("empty",RollbackTransaction)
    Journal.reportCommand("empty",SimpleSelect(Seq("foo"),CTrue))
    //var lines = scala.io.Source.fromFile(Journal.journalFileName).getLines
    Test.assertTrue("Command filttering", lines.length == 0)

    Journal.reportCommand("foo",SimpleDelete("bar",CTrue))
    Test.assertTrue("Command writing "+ lines, lines.length == 1 && lines(0).equals("foo;"))
    Journal.rollbackTransaction
    Test.assertTrue("Rollback clears journal", lines.length == 0)
    
    Journal.beginTransaction
    Journal.reportCommand("foo",SimpleDelete("bar",CTrue))
    Journal.commitTransaction
    Test.assertTrue("Commit clears journal", lines.length == 0)
 
    //This will test way more than needed
    def start() = {
      Journal.beginTransaction
      MiniDB.supressNoResults = true
      MiniDB.executeQuery("CREATE TABLE foo (a TEXT, b INT, c DOUBLE);")
    }

    var ok = false;    
    start
    Journal.rollbackTransaction
    try{
      val res =  QueryProc.processQuery(SimpleSelect(Seq("foo"),CTrue))
    }catch{case e: QueryProcException => {ok = true}}
    Test.assertTrue("Rollback restores database",ok)

    ok = true;
    start
    Journal.commitTransaction
    try{
      val res =  QueryProc.processQuery(SimpleSelect(Seq("foo"),CTrue))
    }catch{case e: QueryProcException => {ok = false}}
    Test.assertTrue("Commit keeps database valid",ok)
  
    Test.finishTestSet()
  }

  def runTests {
    testTransactions()
  }
}
