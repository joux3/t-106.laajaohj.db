package minidb.sqlparse
import minidb.sqlexpr._

object Parser {
  // This function tries to match the sql query with the supported 
  // query types and sends it to the parser defined for that type.
  def parse(sql: String): SQLExpr = {
    val matchCreateTable = """(?i)CREATE TABLE (\w+) \(([a-zA-Z0-9 ,]*)\);?""".r
    val matchInsert = """(?i)INSERT INTO (\w+) VALUES \(([a-zA-Z0-9 ,.\(\)\"]*)\);?""".r
    val matchSelect1 = """(?i)SELECT \* FROM ([a-zA-Z0-9, ]+) WHERE ([a-zA-Z0-9 ,.\"]);?""".r
    val matchSelect2 = """(?i)SELECT \* FROM ([a-zA-Z0-9, ]+);?""".r
    val matchCreateIndex1 = """(?i)CREATE INDEX (\w*) ?USING (\w*) ON (\w+) \(([a-zA-Z0-9 ,]+)\)""".r
    val matchCreateIndex2 = """(?i)CREATE INDEX (\w*) ?ON (\w+) \(([a-zA-Z0-9 ,]+)\)""".r
    
    sql match {
      case matchCreateTable(tableName, columnDefinitions) => parseCreateTable(tableName, columnDefinitions)
      case matchInsert(tableName, values) => parseInsert(tableName, values)
      case matchSelect1(tableNames, conditions) => parseSelect(tableNames, conditions)
      case matchSelect2(tableNames) => parseSelect(tableNames, null)
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
  // SELECT * FROM tablename
  // Note: This doesn't support conditions yet
  def parseSelect(tableNames: String, conditions: String): SQLExpr = {
    val tables = tableNames.split(",")
    var tableList : List[(String)] = List()
    tables.foreach(tableName => {
      tableList = tableList ::: List(tableName.trim)
    })
    new SimpleSelect(tableList, CTrue)
  }
  
  // Parse comparison strings to objects. 
  // It will be used with condition-parser when that is done.
  def parseComparison(comparison: String): CComparisonType = {
    comparison match {
      case ">" => CGreater
      case ">=" => CGreaterEq
      case "<" => CLess
      case "<=" => CLessEq
      case "=" => CEquals
      case _ => null
    }
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
}