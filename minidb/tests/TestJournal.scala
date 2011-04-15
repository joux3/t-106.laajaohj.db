package minidb.tests
import minidb._
import minidb.journal._
import minidb.sqlexpr._
import minidb.queryproc._
import java.io._

object TestJournal extends RunnableTest {
  def testTransactions() {
    //This test uses different journal file
    Journal.journalFileName = "test.journal"
    
    //Stupid test, doesn't test just journal
    Test.startTestSet("Testing journal IO")
    //Clear journal file
    val a = new DataOutputStream(new FileOutputStream(Journal.journalFileName))
    
    Journal.loadTransactionFromFile()
    
    def lines = scala.io.Source.fromFile(Journal.journalFileName).getLines.toArray
    val dymmyLine = "begin tran"
    val dymmyCommand = dymmyLine+"\n"

    Journal.beginTransaction
    Journal.reportCommand("empty",BeginTransaction)
    Journal.reportCommand("empty",CommitTransaction)
    Journal.reportCommand("empty",RollbackTransaction)
    Journal.reportCommand("empty",SimpleSelect(Seq("foo"),CTrue))
    //Journal contains only header
    Test.assertTrue("Command filttering", lines.length == 1 && lines(0).equals("2"))

    Journal.reportCommand(dymmyCommand,SimpleDelete("bar",CTrue))
    Test.assertTrue("Command writing", lines.length == 2 && lines(1).equals(dymmyLine+" "))
    Journal.rollbackTransaction
    Test.assertTrue("Rollback clears journal", lines.length == 0 && Journal.transactionActive == false)
    
    Journal.beginTransaction
    Journal.reportCommand(dymmyCommand,SimpleDelete("bar",CTrue))
    Journal.commitTransaction
    Test.assertTrue("Commit clears journal", lines.length == 0 && Journal.transactionActive == false)

    Journal.beginTransaction
    Journal.beginTransaction
    Journal.reportCommand(dymmyCommand,SimpleDelete("bar",CTrue))
    Test.assertTrue("Nested transactions work", lines.mkString("\n").equals("2 2\n"+dymmyLine+" "))
    Journal.rollbackTransaction
    Journal.rollbackTransaction
    Test.finishTestSet()
    
    Test.startTestSet("Testing journal <-> table interaction")

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
    Test.assertTrue("Commit keeps database intact",ok)
    
    //Remove journal file
    new File(Journal.journalFileName) delete
  
    Test.finishTestSet()
  }

  def runTests {
    testTransactions()
  }
}
