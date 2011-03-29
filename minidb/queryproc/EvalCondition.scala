package minidb.queryproc
import scala.collection.mutable.ArrayBuffer
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

  /** Returns cases where a table field gets compared to a constant value 
      to be used when deciding which indexes to use */
  // XXX needs a better place?
  def getFieldConstantEquals(where: ConditionExpr, results: ArrayBuffer[(VField, DBValue)]
                            = new ArrayBuffer[(VField, DBValue)]): ArrayBuffer[(VField, DBValue)] = {
    where match {
      // only go through ANDs as they can be easily transferred into index lookups
      case CAnd(c1, c2) => {
        // keep on searching 
        getFieldConstantEquals(c1, results)
        getFieldConstantEquals(c2, results)
      }
      case CCompare(v1, v2, t) => {
        if (t == CEquals) {
          // only use if it compares a column to a constant
          // XXX needs more work if ValueExpr gets more subclasses
          val usable = v1.isInstanceOf[VConstant] ^ v2.isInstanceOf[VConstant]
          if (usable) {
            val field = if (v1.isInstanceOf[VField]) v1.asInstanceOf[VField] else v2.asInstanceOf[VField]
            val constant = if (v1.isInstanceOf[VConstant]) v1.asInstanceOf[VConstant] 
                           else v2.asInstanceOf[VConstant]
            results += ((field, constant.v))
          }
        }
      }
      case _ => // ignore any other cases
    }
    results
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
