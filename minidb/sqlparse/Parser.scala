package minidb.sqlparse
import minidb.sqlexpr._

object Parser {
  /** Parses the given SQL expression into an SQLExpr */
  def parse(sql: String): SQLExpr = {
    CommitTransaction // XXX
  }
}
