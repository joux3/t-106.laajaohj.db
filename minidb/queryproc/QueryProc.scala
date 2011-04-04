package minidb.queryproc
import minidb.sqlexpr._
import minidb.storage.Table
import minidb.storage.DBRow
import minidb.storage.Index
import minidb.storage.DBKey

/** Abstract superclass for exceptions
 * that can be returned when queries are executed */
abstract class QueryProcException(msg: String) extends Exception(msg)

class QueryError(msg: String) extends QueryProcException(msg)

object QueryProc {
  // holds the count of rows filtered for the latest SELECT
  var filteredRowCount = 0

  /** Executes the given SELECT query */
  private def processSelect(q: SQLExpr): QueryResult = q match {
    case SimpleSelect(from, where) => {
      if (from.size == 0)
        throw new QueryError("No tables to select from?")
      if (from.size != 1)
        throw new QueryError("We only support selecting from one table!")
      val table = Table.find(from(0))
      // default to a full table scan
      var possibleRows: Seq[DBRow] = table.allRows
      // XXX needs tests!
      // try to find some possible indexes
      val equalComparisons = EvalCondition.getFieldConstantEquals(where)
      val fieldsUsed = equalComparisons._1
      // can we safely skip filtering if we have a 100% matching index
      // (except a full table scan at this line)
      var needsFiltering = true
      if (!fieldsUsed.isEmpty) {
        val indexes = table.getUsableIndexes(fieldsUsed.map(_._1))
        if (!indexes.isEmpty) {
          // use the index with longest number of common columns
          val index = indexes(0)
          val indexNums = index.columnNums
          // check if the index can be used to guarantee
          // conditions ie. no CComparisons have been dropped
          // and index matches the conditions
          needsFiltering = equalComparisons._2 || 
                           indexNums.length != fieldsUsed.length
          // construct DBKey for index
          val valueArray = new Array[DBValue](indexNums.size)
          var placedValues = 0
          fieldsUsed.foreach {field => 
            // check if the current field is used in the index
            if (indexNums.contains(table.getColumnNum(field._1))) {
              // it is used, place it on the key
              placedValues += 1
              valueArray(indexNums.indexOf(table.getColumnNum(field._1))) = field._2
            }
          }
          if (placedValues != indexNums.size) {
            // XXX don't throw an exception as a full table scan 
            // can still fetch the rows
            throw new RuntimeException("Internal error in QueryProc!")
          } else {
            possibleRows = index.searchExact(new DBKey(valueArray))
          }
        }
      }

      filteredRowCount = 0
      if (needsFiltering) {
        val rows = possibleRows.filter { row =>
        // avoid possible O(n) behaviour of possibleRows.size
          filteredRowCount += 1 
          EvalCondition.eval(where,
            table.columnNames.map{(from(0), _)},
          row) == DBBoolean(true) 
        }
        new QueryResult(table.columnNames, rows)
      } else {
        new QueryResult(table.columnNames, possibleRows)
      }
    }
    case _ => throw new Exception("Internal error in processSelect!")
  }

  /** Executes the given query */
  def processQuery(q: SQLExpr): Option[QueryResult] = q match {
    case SimpleSelect(_, _) => Some(processSelect(q))
    case InsertValues(tablename, values) => {
      val table = Table.find(tablename)
      values foreach { row => table insert new DBRow(row) }
      None
    }
    case CreateTable(tablename, columns, constraints) => {
      val table = Table.create(tablename, "heap", columns, constraints)
      constraints foreach { c =>
        c match {
          case TCPrimaryKey(columns) =>
            table.createIndex("_primarykey_" + tablename,
                              "hash",
                              columns)
	  case TCNotNull(columns) =>
            ()
	  case TCUnique(columns) =>
            table.createIndex("_unique_" + columns.toString + tablename,
                              "hash",
                              columns)
          case TCDefault(columns, values) =>
            ()
        }
      }
      None
    }
    case CreateIndex(indexname, indextype, tablename, columns) => {
      val realindexname =
        if (indexname != "") indexname else tablename + "_index"
      if (realindexname.head == '_')
        throw new QueryError("User created indexnames starting with _ not allowed.")
      val realindextype = 
        if (indextype != "") indextype else Index.defaultIndexType
      val table = Table.find(tablename)
      table.createIndex(realindexname, realindextype, columns)
      None
    }
    case DropIndex(indexname, tablename) => {
      if (indexname.head == '_')
        throw new QueryError("User created indexnames starting with _ not allowed."
      val table = Table.find(tablename)
      table.dropIndex(indexname)
      None
    }
    // XXX unimplemented:
    case BeginTransaction => None
    case CommitTransaction => None
    case RollbackTransaction => None
  }
}
