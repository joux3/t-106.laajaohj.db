package minidb.journal
import minidb._
import minidb.storage._
import minidb.sqlexpr._
import minidb.sqlparse._
import minidb.queryproc._
import java.io._

// Handles transactions, doesn't support named transactions. Journal file structure:
// First line contains line-numbers for beginnings for all active transactions.
// Linenumbers start from 1. After that there is sql instruction to get the database
// to the current state.
object Journal {
  // how many transactions are active
  var transactionsActive = 0
  def transactionActive = transactionsActive > 0

  var writer: DataOutputStream = null 
  var journalFileName = "db.journal"
  //We keep a copy of journal-file in memory
  var header: Seq[Int] = Seq()
  var data: Seq[String] = Seq()

  def clearJournalFile() {
    writer.flush
    writer.close
    writer = new DataOutputStream(new FileOutputStream(journalFileName))
  }
  def updateJournalFile() {
    clearJournalFile
    writer writeBytes(header.mkString(" ") + "\n")
    writer writeBytes(data.mkString("\n"))
    writer.flush
  }
  def beginTransaction() {
    if(transactionsActive == 0)
    {
      Table.saveToFile
      //Line-numbers start from 1 and first line is header
      header = Seq(2)
    }
    else
    {
      //Mark linenumber of beginning of this transaction
      //Line-numbers start from 1 and first line is header
      header = header :+ data.length+2
    }
    transactionsActive += 1
    updateJournalFile
  }
  def commitTransaction() {
    if(transactionsActive > 0)
    {
      if(transactionsActive > 1)
      {
        //There are many transactions active, combine last two together
        header = header.dropRight(1)
        updateJournalFile
      }
      else
      {
        //Save tables to file and clears journal-file
        Table.saveToFile
        header = Seq()
        data = Seq()
        clearJournalFile
      }
      transactionsActive -= 1
    }
    else println("No active transactions.")
  }
  def rollbackTransaction() {
    if(transactionsActive > 0)
    {
      if(transactionsActive > 1)
      {
        //There are many transactions active: reload table, filter journal and apply new journal
        Table.loadFromFile
        data = data.take(header.last)
        header = header.dropRight(1)
        updateJournalFile
        
        //This function will update transactionsActive
        loadTransactionFromFile
      }
      else
      {
        //Only transaction, remove all data
        Table.loadFromFile
        header = Seq()
        data = Seq()
        clearJournalFile
        transactionsActive -= 1
      }
    }
    else println("No active transactions.")
 }
  def loadTransactionFromFile(){
    var continueJournal = true
    try
    {
      val lines = scala.io.Source.fromFile(journalFileName).getLines.toSeq
      header = if(lines.length > 0)lines(0).split(' ').filter{line => line.length() > 0}.map(line => line.toInt) else Seq()
      data = lines.drop(1)
      for (line <- data) {
        val expr = Parser.parse(line)
        //If file is OK, this won't throw
        QueryProc.processQuery(expr)
      }
      transactionsActive = header.length
    }catch{
      case e: IOException => //File not found
      case _ => {
        println("Journal file is broken, discarded (data: "+scala.io.Source.fromFile(journalFileName).getLines.mkString("\n")+")")
        continueJournal = false
      }
    }
    writer = new DataOutputStream(new FileOutputStream(journalFileName,continueJournal))
  }
  def reportCommand(cmd:String, sql: SQLExpr){
    if(transactionsActive > 0){
      sql match {
        //Don't write useless commands
        case BeginTransaction =>
        case CommitTransaction =>
        case RollbackTransaction =>
        case SimpleSelect(_,_) =>
        case _ => {
          val line = cmd.replace('\n', ' ')
          writer writeBytes(line + "\n")
          writer.flush
          data = data :+ line
        }
      }
    }
  }
}