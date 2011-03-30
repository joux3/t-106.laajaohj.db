package minidb.sqlparse
import minidb.sqlexpr._
import java.text.{NumberFormat, ParsePosition}

object Parser {
  // This function tries to match the sql query with the supported 
  // query types and sends it to the parser defined for that type.
  def parse(sql: String): SQLExpr = {
    val matchCreateTable = """(?i)CREATE TABLE (\w+) \(([a-zA-Z0-9 ,]*)\);?""".r
    val matchInsert = """(?i)INSERT INTO (\w+) VALUES \(([a-zA-Z0-9 ,.\(\)\"]*)\);?""".r
    val matchSelect1 = """(?i)SELECT \* FROM ([a-zA-Z0-9, ]+) WHERE ([a-zA-Z0-9 ,.\"<>=\(\)]+);?""".r
    val matchSelect2 = """(?i)SELECT \* FROM ([a-zA-Z0-9, ]+);?""".r
    val matchCreateIndex1 = """(?i)CREATE INDEX (\w*) ?USING (\w*) ON (\w+) \(([a-zA-Z0-9 ,]+)\);?""".r
    val matchCreateIndex2 = """(?i)CREATE INDEX (\w*) ?ON (\w+) \(([a-zA-Z0-9 ,]+)\);?""".r
    
    sql match {
      case matchCreateTable(tableName, columnDefinitions) => parseCreateTable(tableName, columnDefinitions)
      case matchInsert(tableName, values) => parseInsert(tableName, values)
      case matchSelect1(tableNames, conditions) => parseSelect(tableNames, conditions)
      case matchSelect2(tableNames) => parseSelect(tableNames, "")
      case matchCreateIndex1(indexName, indexType, tableName, columns) => parseCreateIndex(tableName, columns, indexName, indexType)
      case matchCreateIndex2(indexName, tableName, columns) => parseCreateIndex(tableName, columns, indexName, "")
      case _ => CommitTransaction
    }
  }
  
  // Parse queries like:
  // CREATE TABLE tablename (column1 INT, column2 TEXT, column3 DOUBLE)
  def parseCreateTable(tableName: String, columnDefinitions: String): SQLExpr = {
    val columnDefinitionParts = columnDefinitions.split(",")
    var columns : List[(String, DBType)] = List()
    columnDefinitionParts.foreach(part => {
      var columnParts = part.trim.split(" ");
      var columnName = columnParts(0)
      var columnType = columnParts(1)
      if(columnType.toLowerCase() == "int") {
        columns = columns ::: List((columnName, DBTypeInt))
      } else if(columnType.toLowerCase() == "double") {
        columns = columns ::: List((columnName, DBTypeDouble))
      } else if(columnType.toLowerCase() == "bool") {
        columns = columns ::: List((columnName, DBTypeBoolean))
      } else if(columnType.toLowerCase() == "text") {
        columns = columns ::: List((columnName, DBTypeText))
      }
    })
    new CreateTable(tableName, columns, List())
  }
  
  // Parse queries like:
  // CREATE INDEX indexname USING indextype ON tablename (columns)
  def parseCreateIndex(tableName: String, columns: String, indexName: String, indexType: String): SQLExpr = {
    var columnList = columns.split(",")
    var columnListFormatted : List[(String)] = List()
    columnList.foreach(column => {
      columnListFormatted = columnListFormatted ::: List(column.trim)
    })
    new CreateIndex(indexName, indexType, tableName, columnListFormatted)
  }
  
  // Parse queries like:
  // INSERT INTO tablename VALUES (5, "text", 3.2)
  def parseInsert(tableName: String, valueString: String): SQLExpr = {
    val values = valueString.split(",")
    var valueList : List[(DBValue)] = List()
    values.foreach(value => {
      val parsedValue = parseValue(value)
      if(parsedValue == None) {
        valueList = valueList ::: List(new DBString(""))
      } else {
        valueList = valueList ::: List(parsedValue)
      }
    })
    new InsertValues(tableName, List(valueList))
  }
  
  // Parse queries like:
  // SELECT * FROM tablename WHERE a>5 AND (b<=8 OR c=8)
  def parseSelect(tableNames: String, conditions: String): SQLExpr = {
    val tables = tableNames.split(",")
    var tableList : List[(String)] = List()
    tables.foreach(tableName => {
      tableList = tableList ::: List(tableName.trim)
    })
    new SimpleSelect(tableList, parseConditions(conditions))
  }
  
  // Parse value (e.g. int or text) from given string.
  def parseValue(value: String): DBValue = {
    var valueTrimmed = value.trim
    val matchText = """\"([a-zA-Z0-9 ,.]*)\" ?""".r
    val matchDouble = """([0-9]*)\.([0-9]*)""".r
    val matchInt = """([0-9]+)""".r
    val matchBoolean = """(?i)(true|false)""".r

    valueTrimmed match {
      case matchText(text) => new DBString(text)
      case matchDouble(begin, end) => new DBDouble((begin+"."+end).toDouble)
      case matchInt(number) => new DBInt(number.toInt)
      case matchBoolean(boolean) => {
        if(boolean.toLowerCase == "true") {
          new DBBoolean(true)
        } else {
          new DBBoolean(false)
        }
      }
      case _ => null
    }
  }
  
  // Parse condition string to a list of different elements
  def conditionsToList(conditions: String): List[(Any)] = {
    var conditionList : List[(Any)] = List()
    var i = 0
    while(i < conditions.length) {
      var curchar = conditions.charAt(i)
      if(curchar == '"') {
        // Parse string
        var curString = ""
        i += 1
        curchar = conditions.charAt(i)
        while(i < conditions.length && curchar != '"') {
          curString += curchar
          i+=1
          if(i < conditions.length) curchar = conditions.charAt(i)
        }
        conditionList = conditionList ::: List(VConstant(DBString(curString)))
      } else if(Character.isDigit(curchar) || curchar == '.') { 
        // Parse number
        var curString = ""
        while(i < conditions.length && (Character.isDigit(curchar) || curchar == '.')) {
          curString += curchar
          i+=1
          if(i < conditions.length) curchar = conditions.charAt(i)
        }
        i-=1
        var value = parseValue(curString)
        conditionList = conditionList ::: List(VConstant(value))
      } else if(conditions.length > i+3 && conditions.substring(i,i+4).toLowerCase() == "true") {
        // Parse boolean true
        conditionList = conditionList ::: List(CTrue)
        i+=3
      } else if(conditions.length > i+4 && conditions.substring(i,i+5).toLowerCase() == "false") {
        // Parse boolean false
        conditionList = conditionList ::: List(CFalse)
        i+=4
      } else if(curchar == '(' || curchar == ')') {
        // Parse parentheses
        conditionList = conditionList ::: List(String.valueOf(curchar))
      } else if(conditions.length > i+2 && conditions.substring(i,i+3).toLowerCase() == "and") {
        conditionList = conditionList ::: List(CAnd)
        i+=2
      } else if(conditions.length > i+1 && conditions.substring(i,i+2).toLowerCase() == "or") {
        conditionList = conditionList ::: List(COr)
        i+=1
      } else if(curchar == '=' || curchar == '<' || curchar == '>' || curchar == '!') {
        // Parse conditionals
        if(curchar == '=') {
          conditionList = conditionList ::: List(CEquals)
        } else if(conditions.length > i+1 && conditions.substring(i,i+2) == ">=") {
          conditionList = conditionList ::: List(CGreaterEq)
          i+=1
        } else if(conditions.length > i+1 && conditions.substring(i,i+2) == "<=") {
          conditionList = conditionList ::: List(CLessEq)
          i+=1
        } else if(curchar == '<') {
          conditionList = conditionList ::: List(CLess)
        } else if(curchar == '>') {
          conditionList = conditionList ::: List(CGreater)
        }
      } else if(curchar == ' ') {
        // Skip space
      } else {
        // Any other strings will be used as column names
        val parseColumnName = """([a-zA-Z0-9]+).*""".r
        val parseColumnName(columnName) = conditions.substring(i)
        conditionList = conditionList ::: List(VField("", columnName))
        i += columnName.length-1
      }
      i+=1
    }
    conditionList
  }
  
  // Find comparisons from a list of conditions and move them 
  // inside CCompare-objects
  def parseComparisons(conditionList: List[(Any)]): List[(Any)] = {
    var i = 0
    var parsedList : List[(Any)] = List()
    while(i < conditionList.length) {
      if(conditionList(i).isInstanceOf[CComparisonType]) {
        if(conditionList(i) == CEquals) {
          parsedList = parsedList.slice(0, parsedList.length-1) ::: List(CCompare(conditionList(i-1).asInstanceOf[ValueExpr], conditionList(i+1).asInstanceOf[ValueExpr], CEquals))
          i+=1
        } else if(conditionList(i) == CLess) {
          parsedList = parsedList.slice(0, parsedList.length-1) ::: List(CCompare(conditionList(i-1).asInstanceOf[ValueExpr], conditionList(i+1).asInstanceOf[ValueExpr], CLess))
          i+=1
        } else if(conditionList(i) == CLessEq) {
          parsedList = parsedList.slice(0, parsedList.length-1) ::: List(CCompare(conditionList(i-1).asInstanceOf[ValueExpr], conditionList(i+1).asInstanceOf[ValueExpr], CLessEq))
          i+=1
        } else if(conditionList(i) == CGreater) {
          parsedList = parsedList.slice(0, parsedList.length-1) ::: List(CCompare(conditionList(i-1).asInstanceOf[ValueExpr], conditionList(i+1).asInstanceOf[ValueExpr], CGreater))
          i+=1
        } else if(conditionList(i) == CGreaterEq) {
          parsedList = parsedList.slice(0, parsedList.length-1) ::: List(CCompare(conditionList(i-1).asInstanceOf[ValueExpr], conditionList(i+1).asInstanceOf[ValueExpr], CGreaterEq))
          i+=1
        } 
      } else {
        parsedList = parsedList ::: List(conditionList(i))
      }
      i+=1
    }
    parsedList
  }
  
  // Parse condition string
  def parseConditions(conditions: String): ConditionExpr = {
    def parseConditionsRecursive(conditionList: List[(Any)]): ConditionExpr = {
      var flatList : List[(Any)] = List()
      // Find nested expressions and call this function recursively for them
      var i = 0
      var depth = 0
      var start = 0
      while(i < conditionList.length) {
        if(conditionList(i) == "(") {
          if(depth == 0) start = i+1
          depth+=1
        } else if(conditionList(i) == ")") {
          depth-=1
          if(depth == 0) {
            flatList = flatList ::: List(parseConditionsRecursive(conditionList.slice(start, i)))
          }
        } else {
          if(depth == 0) flatList = flatList ::: List(conditionList(i))
        }
        i+=1
      }
      
      // Create ConditionExpr from the flattened list of expressions
      if(flatList.length == 0) {
        CTrue
      } else {
        var expr : ConditionExpr = flatList(0).asInstanceOf[ConditionExpr]
        var i = 1
        while(i < flatList.length) {
          if(flatList(i) == CAnd) {
            expr = CAnd(expr, flatList(i+1).asInstanceOf[ConditionExpr])
          } else if(flatList(i) == COr) {
            expr = COr(expr, flatList(i+1).asInstanceOf[ConditionExpr])
          }
          i+=2
        }
        expr
      }
    }
    parseConditionsRecursive(parseComparisons(conditionsToList(conditions)))
  }
}