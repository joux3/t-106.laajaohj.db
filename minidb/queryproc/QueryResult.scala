package minidb.queryproc
import minidb.sqlexpr.DBValue

/** Stores the result of a SELECT query */
class QueryResult(columns: Seq[String], values: Seq[Seq[DBValue]]) {
  def csvPrintResult {
    val header = new StringBuilder
    columns.addString(header, ";")
    println(header.toString)
    values foreach { v =>
      val row = new StringBuilder
      v map {_.valueString} addString(row, ";")
      println(row.toString)
    }
  }
  def printResult = csvPrintResult
}
