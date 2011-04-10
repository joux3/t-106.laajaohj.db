package minidb.sqlparse
import minidb.queryproc.QueryProcException
import minidb.sqlexpr._
import scala.util.parsing.combinator._

class ParserError(msg: String) extends QueryProcException(msg)

object Parser extends RegexParsers {
  def text = "\"" ~ """[^"]*""".r ~ "\"" ^^ {case _ ~ x ~ _ => DBString(x)}
  def double = """[0-9]*\.[0-9]*""".r ^^ {x => DBDouble(x.toDouble)}
  def int = ("""[0-9]+""".r) ^^ {x => DBInt(x.toInt)}
  def boolean = """(?i)(true|false)""".r ^^ {x => DBBoolean(x.toLowerCase.equals("true"))}
  def dbvalue = text | double | int | boolean
  def constant = dbvalue ^^ {x => VConstant(x)}

  def identifier = """\w+""".r
  def field = opt(identifier <~ ".") ~ identifier ^^ {case l ~ r => VField(l.getOrElse(""), r)}
  def term = constant | field

  def eq = term ~ "=" ~ term ^^ {case left ~ "=" ~ right => CCompare(left, right, CEquals)}
  def lt = term ~ "<" ~ term ^^ {case left ~ _ ~ right => CCompare(left, right, CLess)}
  def gt = term ~ ">" ~ term ^^ {case left ~ _ ~ right => CCompare(left, right, CGreater)}
  def le = term ~ "<=" ~ term ^^ {case left ~ _ ~ right => CCompare(left, right, CLessEq)}
  def ge = term ~ ">=" ~ term ^^ {case left ~ _ ~ right => CCompare(left, right, CGreaterEq)}

  def condition = eq | lt | gt | le | ge | "(" ~> conditions <~ ")"
  
  def and = "(?i)and".r
  def or = "(?i)or".r

  def conditions: Parser[ConditionExpr] = condition * (and ^^^ CAnd | or ^^^ COr)
  def intType = "(?i)int".r ^^^ DBTypeInt
  def doubleType = "(?i)double".r ^^^ DBTypeDouble
  def booleanType = "(?i)bool".r ^^^ DBTypeBoolean
  def textType = "(?i)text".r ^^^ DBTypeText
  def columnType = intType | doubleType | booleanType | textType
  
  def identifiers = repsep(identifier, ",")
  def columnDef = identifier ~ columnType ^^ {case l ~ r => (l, r)} | err("Failed to match column def")

  def createTable = "(?i)create table".r ~> identifier ~ "(" ~ repsep(columnDef, ",") <~ ")" ^^ {
    case l ~ _ ~ r => CreateTable(l, r, List())
  }

  def values = repsep(repsep(dbvalue, ","), ",")
  def insert = "(?i)insert into".r ~> identifier ~ "(?i)values".r ~ "(" ~ values <~ ")" ^^ {
    case tablename ~ _ ~ _ ~ values => InsertValues(tablename, values)
  }
  def select = "(?i)select \\* from".r ~> identifiers ~ opt("(?i)where".r ~> conditions) ^^ {
    case l ~ r => SimpleSelect(l, r.getOrElse(CTrue))
  }
  def createIndex = "(?i)create index".r ~> opt("(?i)using".r ~> identifier) ~ "(?i)on".r ~ identifier ~ "(" ~ identifiers <~ ")" ^^ {
    case iT ~ _ ~ tN ~ _ ~ c => CreateIndex("", iT.getOrElse(""), tN, c)
  }
  def createIndex2 = "(?i)create index".r ~> identifier ~ opt("(?i)using".r ~> identifier) ~ "(?i)on".r ~ identifier ~ "(" ~ identifiers <~ ")" ^^ {
    case iN ~ iT ~ _ ~ tN ~ _ ~ c => CreateIndex(iN, iT.getOrElse(""), tN, c)
  }
  def dropIndex = "(?i)drop index".r ~> identifier ~ "(?i)on".r ~ identifier ^^ {
    case l ~ _ ~ r => DropIndex(l, r)
  }
  def delete = "(?i)delete from".r ~> identifier ~ opt("(?i)where".r ~> conditions) ^^ {
    case l ~ r => SimpleDelete(l, r.getOrElse(CTrue))
  }
  def beginTran = "(?i)BEGIN TRAN(SACTION)?".r ^^ {
	case _ => BeginTransaction
  }
  def commitTran = "(?i)COMMIT TRAN(SACTION)?".r ^^ {
	case _ => CommitTransaction
  }
  def rollTran = "(?i)ROLLBACK TRAN(SACTION)?".r ^^ {
	case _ => RollbackTransaction
  }
  
  def statement = (createTable | insert | select | createIndex | createIndex2 | dropIndex | delete | beginTran | commitTran | rollTran) <~ opt(";") | err("Unknown command")

  def parse(sql: String): SQLExpr = {
    parseAll(statement, sql) match {
      case Success(result, _) => result
      case e: NoSuccess => throw new ParserError(e.toString)
    }
  }
}
