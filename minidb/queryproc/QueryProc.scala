package minidb.queryproc
import minidb.sqlexpr._
import minidb.storage.Table
import minidb.storage.Index

/** Abstract superclass for exceptions
 * that can be returned when queries are executed */
abstract class QueryProcException(msg: String) extends Exception(msg)

class QueryError(msg: String) extends QueryProcException(msg)

object QueryProc {
  /** Executes the given SELECT query */
  private def processSelect(q: SQLExpr): QueryResult = q match {
    case SimpleSelect(from, where) => {
      if (from.size == 0)
        throw new QueryError("No tables to select from?")
      if (from.size != 1)
        throw new QueryError("We only support selecting from one table!")
      val table = Table.find(from(0))
      // the following just scans through the full table, without
      // using any indexes...
      val rows = table.allRows.filter { row =>
        EvalCondition.eval(where,
                           table.columnNames.map{(from(0), _)},
                           row) == DBBoolean(true) }
      new QueryResult(table.columnNames, rows)
    }
    case _ => throw new Exception("Internal error in processSelect!")
  }

  /** Executes the given query */
  def processQuery(q: SQLExpr): Option[QueryResult] = q match {
    case SimpleSelect(_, _) => Some(processSelect(q))
    case InsertValues(tablename, values) => {
      val table = Table.find(tablename)
      values foreach { row => table insert row }
      None
    }
    case CreateTable(tablename, columns, constraints) => {
      val table = Table.create(tablename, "heap", columns, constraints)
      constraints foreach { c =>
        c match {
          case TCPrimaryKey(columns) =>
            table.createIndex(tablename + "_primarykey",
                              Index.defaultIndexType,
                              columns)
        }
      }
      None
    }
    case CreateIndex(indexname, indextype, tablename, columns) => {
      val realindexname =
        if (indexname != "") indexname else tablename + "_index"
      val realindextype = 
        if (indextype != "") indextype else Index.defaultIndexType
      val table = Table.find(tablename)
      table.createIndex(realindexname, realindextype, columns)
      None
    }
    // XXX unimplemented:
    case BeginTransaction => None
    case CommitTransaction => None
    case RollbackTransaction => None
  }
}
