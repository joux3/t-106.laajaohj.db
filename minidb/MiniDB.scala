package minidb
import minidb.sqlparse.Parser
import minidb.sqlexpr.SQLExpr
import minidb.queryproc.QueryProc
import minidb.queryproc.QueryProcException
import minidb.queryproc.QueryResult

object MiniDB {
  def executeQuery(q: SQLExpr) {
    try {
      val result: Option[QueryResult] = QueryProc.processQuery(q)
      result match {
        case Some(r) => r.printResult
        case None => println("(no result for query)")
      }
    } catch {
      case e: QueryProcException => {
        println("Query execution failed for the query:")
        println(q.toString)
        println("The error was:")
        println(e.toString)
      }
    }
  }

  def main(args: Array[String]) {
    println("MiniDB started")
    val (prompt, cmdsource) =
      if (args.size == 0) ("> ", io.Source.stdin)
      else ("", io.Source.fromFile(args(0)))
    print(prompt)
    for (line <- cmdsource.getLines) {
      val expr = Parser.parse(line)
      executeQuery(expr)
      print(prompt)
    }
  }
}

