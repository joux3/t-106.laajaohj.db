package minidb.sqlexpr
import minidb.queryproc.QueryProcException

class CompareTypeError(msg: String) extends QueryProcException(msg)


/** Supported values to store in the database.
 * Note that NULL values are currently not supported
 * (i.e., all columns are NOT NULL).
 */
sealed abstract class DBValue extends Ordered[DBValue] {
  def compare(that: DBValue): Int =
    throw new CompareTypeError(this+" compare "+that)
  /** What should this value look like in a query result?
   * We use a separate method rather than overriding toString, so that
   * the types of the DBValues are clearly visible in exceptions and
   * debug-printouts that use toString.
   */
  def valueString = toString
}

case class DBBoolean(v: Boolean) extends DBValue {
  override def compare(that: DBValue) = that match {
    case DBBoolean(tv) => v compare tv
    case _ => super.compare(that)
  }
  override def valueString = v.toString
}

case class DBInt(v: Int) extends DBValue {
  override def compare(that: DBValue) = that match {
    case DBInt(tv) => v compare tv
    case DBDouble(tv) => v.toDouble compare tv
    case _ => super.compare(that)
  }
  override def valueString = v.toString
}

case class DBDouble(v: Double) extends DBValue {
  override def compare(that: DBValue) = that match {
    case DBDouble(tv) => v compare tv
    case DBInt(tv) => v compare tv.toDouble
    case _ => super.compare(that)
  }
  override def valueString = v.toString
}

case class DBString(v: String) extends DBValue {
  override def compare(that: DBValue) = that match {
    case DBString(tv) => v compare tv
    case _ => super.compare(that)
  }
  override def valueString = v
}


/** The possible types for columns and values in the database */
sealed abstract class DBType {
  /** Is the given value acceptable for this type? */
  def valueOk(v: DBValue): Boolean
}

case object DBTypeBoolean extends DBType {
  def valueOk(v: DBValue) = v.isInstanceOf[DBBoolean]
}

case object DBTypeInt extends DBType {
  def valueOk(v: DBValue) = v.isInstanceOf[DBInt]
}

case object DBTypeDouble extends DBType {
  def valueOk(v: DBValue) = v.isInstanceOf[DBDouble]
}

case object DBTypeText extends DBType {
  def valueOk(v: DBValue) = v.isInstanceOf[DBString]
}

// XXX could also add e.g. char(n) and varchar(n):
// case class DBTypeChar(size: Int) extends DBType { ... }
// case class DBTypeVarChar(size: Int) extends DBType { ... }

