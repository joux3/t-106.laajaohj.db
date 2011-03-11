package minidb.queryproc
import minidb.sqlexpr._

class VariableNotFound(msg: String) extends QueryProcException(msg)

/** A class for storing an environment
 * containing (tablename,fieldname) => value mappings
 */
class Env(names: Seq[(String,String)], values: Seq[DBValue]) {
  val envtf = Map() ++ (names zip values)
  val envf = Map() ++ ((names.map{_._2}) zip values)
  def getFieldValue(tablename: String, fieldname: String): DBValue = {
    val res =
      if (tablename == "") envf.get(fieldname)
      else envtf.get(tablename, fieldname)
    res match {
      case Some(r) => r
      case None => throw new VariableNotFound(tablename + "." + fieldname)
    }
  }
}

object EvalCondition {
  /** Evaluates the given condition */
  def eval(expr: ConditionExpr, env: Env): DBValue = expr match {
    case CTrue => DBBoolean(true)
    case CFalse => DBBoolean(false)
    case CAnd(c1, c2) => {
      val ec1 = eval(c1, env)
      if (ec1 != DBBoolean(false)) eval(c2, env) else ec1
    }
    case COr(c1, c2) => {
      val ec1 = eval(c1, env)
      if (ec1 != DBBoolean(false)) ec1 else eval(c2, env)
    }
    case CNot(c) => {
      val ec = eval(c, env)
      if (ec != DBBoolean(false)) DBBoolean(false) else ec
    }
    case CCompare(v1, v2, t) => {
      val ev1 = valeval(v1, env)
      val ev2 = valeval(v2, env)
      DBBoolean(t.compareValues(ev1, ev2))
    }
  }

  /** Evaluates the given value expression */
  def valeval(expr: ValueExpr, env: Env) = expr match {
    case VConstant(v) => v
    case VField(t, f) => env.getFieldValue(t, f)
  }

  /** Evaluates a condition expression given names and values for variables
   * @param expr the condition to evaluate
   * @param names (tablename, fieldname) pairs for values;
   *              tablename may be "" if fieldname is unique
   * @param values the values for the names given in names
   * @return the value of the condition expression
   */
   def eval(expr: ConditionExpr,
           names: Seq[(String, String)],
           values: Seq[DBValue]): DBValue =
    eval(expr, new Env(names, values))
}
