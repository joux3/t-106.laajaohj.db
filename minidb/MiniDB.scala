package minidb
import minidb.journal.Journal
import minidb.storage.Table
import minidb.sqlparse.Parser
import minidb.sqlexpr.SQLExpr
import minidb.queryproc.QueryProc
import minidb.queryproc.QueryProcException
import minidb.queryproc.QueryResult
import scala.collection.mutable.HashMap

object MiniDB {
  // supresses (no result for query) from showing
  var supressNoResults = false
  // supresses SELECT results from showing
  var supressResults = false
  // time when timing was started
  var startTime: Long = 0

  def executeQuery(expr: String) {
    try {
      val q = Parser.parse(expr)
      val result: Option[QueryResult] = QueryProc.processQuery(q)
	  Journal.reportCommand(expr,q)
      result match {
        case Some(r) => if (!supressResults) r.printResult
        case None => if (!supressNoResults )println("(no result for query)")
      }
    } catch {
      case e: QueryProcException => {
        println("Query execution failed for the query:")
        println(expr)
        println("The error was:")
        println(e.toString)
      }
    }
  }

  def listCommands(command: String) {
    println("Available commands:")
    commands.foreach {tuple => 
      println("  "+tuple._1+": "+tuple._2._1)
    }
  }

  def stopTimer(command: String) {
    val parts = command.split(" ", 2)
    print("Time taken")
    if (parts.length > 1) print(" for '"+parts(1)+"'")
    println(": "+(System.currentTimeMillis() - startTime)+"ms")
  }

  // various commands
  // in tuples (name, desc, function)
  val commands = new HashMap[String, (String, (String) => Unit)]()
  commands += "help" -> ("lists available commands", listCommands)
  commands += "exit" -> ("exits", x => exit)
  commands += "disablenoresult" -> ("stops 'no result for query' from showing", 
                                    x => MiniDB.supressNoResults = true)
  commands += "enablenoresult" -> ("allows 'no result for query' to show", 
                                    x => MiniDB.supressNoResults = false)
  commands += "starttiming" -> ("stores the current time for timing", 
                                x => MiniDB.startTime = System.currentTimeMillis())
  commands += "stoptiming" -> ("stops the timer, can also be given a string tag", 
                               stopTimer)
  commands += "echo" -> ("prints the given string", x => println(x.substring(5)))
  commands += "disableresults" -> ("stops SELECT results from showing", 
                                    x => MiniDB.supressResults = true)
  commands += "enableresults" -> ("allows SELECT results to show", 
                                    x => MiniDB.supressResults = false)
  def runCommand(line: String) { 
    val cmd = line.split(" ", 2)(0)
    commands.get(cmd) match {
      case Some(s) => s._2(line)
      case None => {println("Unknown command "+cmd+"! Use .help")}
    }
  }

  def main(args: Array[String]) {
    println("MiniDB started")
	Table.loadFromFile
	Journal.loadTransactionFromFile
    val (prompt, cmdsource) =
      if (args.size == 0) ("> ", io.Source.stdin)
      else ("", io.Source.fromFile(args(0)))
    print(prompt)
    for (line <- cmdsource.getLines) {
      if (line.length > 1) {
        line.head match {
          case '.' => runCommand(line.tail)
          case '#' => // comment
          case _ => executeQuery(line)
        }
      } 
      print(prompt)
    }
  }
  
  def exit(){
	//If transaction is not active, save database to file
	//Journal will take care of this is transaction is active
	if(!Journal.transactionActive) Table.saveToFile
	System.exit(0)
  }
}

