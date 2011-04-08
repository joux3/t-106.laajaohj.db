package minidb.sqlexpr


/** The supported kinds of SQL expressions */
sealed abstract class SQLExpr

/** A simple select: SELECT * FROM from WHERE where
 * where from is just a list of table names.
 */
case class SimpleSelect(from: Seq[String],
                        where: ConditionExpr) extends SQLExpr

/** A simple single-table delete: DELETE FROM from WHERE where
 * where from is the name of a single table.
 */
case class SimpleDelete(tablename: String,
                        where: ConditionExpr) extends SQLExpr

/** INSERT INTO tablename VALUES values */
case class InsertValues(tablename: String,
                        values: Seq[Seq[DBValue]]) extends SQLExpr

/** CREATE TABLE tablename (columns, constraints) */
case class CreateTable(tablename: String,
                       columns: Seq[(String, DBType)],
                       constraints: Seq[TableConstraint]) extends SQLExpr

/** CREATE INDEX indexname USING indextype ON tablename (columns)
 * indexname and indextype may be "" (to use defaults)
 */
case class CreateIndex(indexname: String,
                       indextype: String,
                       tablename: String,
                       columns: Seq[String]) extends SQLExpr

/** DROP INDEX indexname ON tablename */
case class DropIndex(indexname: String,
                     tablename: String) extends SQLExpr

/** BEGIN TRANSACTION */
case object BeginTransaction extends SQLExpr
/** COMMIT */
case object CommitTransaction extends SQLExpr
/** ROLLBACK */
case object RollbackTransaction extends SQLExpr


/** Conditions used in SELECT/DELETE WHERE clauses */
sealed abstract class ConditionExpr

case object CTrue extends ConditionExpr
case object CFalse extends ConditionExpr
case class CAnd(c1: ConditionExpr, c2: ConditionExpr) extends ConditionExpr
case class COr(c1: ConditionExpr, c2: ConditionExpr) extends ConditionExpr
case class CNot(c: ConditionExpr) extends ConditionExpr

/** A condition that compares two values */
case class CCompare(v1: ValueExpr, v2: ValueExpr, t: CComparisonType)
     extends ConditionExpr

/** Types of comparisons for CCompare */
sealed abstract class CComparisonType {
  def compareValues(v1: DBValue, v2: DBValue): Boolean
}
case object CEquals extends CComparisonType {
  def compareValues(v1: DBValue, v2: DBValue) = (v1 compare v2) == 0
}
case object CLess extends CComparisonType {
  def compareValues(v1: DBValue, v2: DBValue) = (v1 compare v2) < 0
}
case object CLessEq extends CComparisonType {
  def compareValues(v1: DBValue, v2: DBValue) = (v1 compare v2) <= 0
}
case object CGreater extends CComparisonType {
  def compareValues(v1: DBValue, v2: DBValue) = (v1 compare v2) > 0
}
case object CGreaterEq extends CComparisonType {
  def compareValues(v1: DBValue, v2: DBValue) = (v1 compare v2) >= 0
}


/** Parts of SQL expressions denoting values: constants and fields */
sealed abstract class ValueExpr
/** A constant value */
case class VConstant(v: DBValue) extends ValueExpr
/** The name of a field in a table: tablename.fieldname
 * tablename may be "" if fieldname is unique to the tables in the expression
 */
case class VField(tablename: String, fieldname: String) extends ValueExpr


/** Table constraints supported in CREATE TABLE
 * currently only PRIMARY KEY (columns) is supported
 */
sealed abstract class TableConstraint

case class TCPrimaryKey(columns: Seq[String]) extends TableConstraint

case class TCNotNull(columns: Seq[String]) extends TableConstraint

case class TCUnique(columns: Seq[String]) extends TableConstraint

case class TCDefault(columns: Seq[String], values: Seq[DBValue]) extends TableConstraint
/*
case class TCForeignKey(columns: Seq[String]) extends TableConstraint

case class TCCheck(columns: Seq[String]) extends TableConstraint
*/
