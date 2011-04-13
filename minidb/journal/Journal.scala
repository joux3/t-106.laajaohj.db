package minidb.journal
import minidb._
import minidb.storage._
import minidb.sqlexpr._
import minidb.sqlparse._
import minidb.queryproc._
import java.io._

// Handles transactions, doesn't support nested/named transactions
// Journal file contains sql instruction to get the databese from 
// the begin of this transaction to the current state.
object Journal {
  // is transaction active
  var transactionActive = false

  var writer: DataOutputStream = null 
  var journalFileName = "db.journal"

  def clearJournalFile() {
      writer.flush
      writer.close
      writer = new DataOutputStream(new FileOutputStream(journalFileName))
  }
  def beginTransaction() {
    if(transactionActive) println("Nested transactions not supported.")
    else{
      transactionActive = true;
      Table.saveToFile
      clearJournalFile
    }
  }
  def commitTransaction() {
    if(!transactionActive) println("No active transactions.")
    else{
      //Save tables to file and clears journal-file
      Table.saveToFile
      clearJournalFile
      transactionActive = false;
    }
  }
  def rollbackTransaction() {
    Table.loadFromFile
    clearJournalFile
    transactionActive = false;
 }
  def loadTransactionFromFile(){
    try
    {
      val lines = scala.io.Source.fromFile(journalFileName).getLines
      for (line <- lines) {
        val expr = Parser.parse(line)
        //If file is OK, this won't throw
        QueryProc.processQuery(expr)
      }
      transactionActive = lines.length > 0
    }catch{case e: IOException => {}}
    writer = new DataOutputStream(new FileOutputStream(journalFileName,true))
  }
  def reportCommand(cmd:String, sql: SQLExpr){
    if(transactionActive){
      sql match {
        //Don't write useless commands
        case BeginTransaction =>
        case CommitTransaction =>
        case RollbackTransaction =>
        case SimpleSelect(_,_) =>
        case _ => {
          writer writeBytes(cmd + ";\n")
        }
      }
    }
  }
}