package minidb.storage
import scala.collection.mutable.ArrayBuffer

/** a simple implementation of red-black tree 
    that handles multiples with an ArrayBuffer */
class RedBlackTreeIndex(override val indexName: String, 
                        columnNums: Seq[Int]) extends Index(indexName,
                                                            columnNums) {
  abstract class TreeNode {
    def black: Boolean
    def left: TreeNode
    def right: TreeNode
    def parent: TreeNode
    def key: DBKey = throw new RuntimeException("No key for ZNode!")
    def rows: ArrayBuffer[DBRow] = throw new RuntimeException("No rows for ZNode!")
    // ZNode can't be changed
    def black_=(v: Boolean) {}
    def left_=(v: TreeNode) {}
    def right_=(v: TreeNode) {}
    def parent_=(v: TreeNode) {}
    
    def left_rotate {
      val y = right
      right = y.left
      y.left.parent = this
      y.parent = parent
      if (parent.eq(ZNode)) {
        root = y
      } else if (this.eq(parent.left)) {
        this.parent.left = y
      } else {
        this.parent.right = y
      }
      y.left = this
      parent = y
    }

    def right_rotate {
      val y = left
      left = y.right
      y.right.parent = this
      y.parent = parent
      if (parent.eq(ZNode)) {
        root = y
      } else if (this.eq(parent.right)) {
        this.parent.right = y
      } else {
        this.parent.left = y
      }
      y.right = this
      parent = y
    }
  }

  object ZNode extends TreeNode {
    // Z nodes are always black
    def black = true
    def left = ZNode
    def right = ZNode
    def parent = ZNode

    override def toString = "(ZNode)"
  }

  class RealNode(override val key: DBKey, override val rows: ArrayBuffer[DBRow], 
                 override var black: Boolean, override var left: TreeNode = ZNode,
                 override var right: TreeNode = ZNode, override var parent: TreeNode = ZNode)
                                                          extends TreeNode {
    // get's parent parent
    def grandparent: TreeNode = 
      parent.parent

    // get's the uncle (grandparent's other child node)
    def uncle: TreeNode = 
      if (parent.parent.left.eq(this)) parent.parent.right else parent.parent.left
    
    override def toString = "("+key.toString+", "+(if (black) "black" else "red")+")"
  }

  var root: TreeNode = ZNode

  def clear() {
    root = ZNode
  }

  def printTree(node: TreeNode, ident: Int) { 
    if (node.eq(ZNode)) {
      println("-"*ident+" ZNode")
    } else {
      println("-"*ident+" "+node.key)
      if (!node.left.eq(node)) {
        println("-"*ident+" left:")
        printTree(node.left, ident + 1)
      } else {
        println("-"*ident+" left cycle")
      }
      if (!node.right.eq(node)) {
        println("-"*ident+" right:")
        printTree(node.right, ident + 1)
      } else {
        println("-"*ident+" right cycle")
      }
    }
  }

  private def fixInsert(zNode: TreeNode) {
    // version from Introduction to Algorithms
    var z = zNode
    while (!z.parent.black) {
      //println(z+","+z.parent+","+z.parent.parent)
      if (z.parent.eq(z.parent.parent.left)) {
        val y = z.parent.parent.right
        if (!y.black) {
          //println("1: "+z+","+z.parent+","+z.parent.parent)
          z.parent.black = true
          y.black = true
          z.parent.parent.black = false
          z = z.parent.parent
        } else {
          if (z.eq(z.parent.right)) {
            //println("2: "+z+","+z.parent+","+z.parent.parent)
            z = z.parent
            z.left_rotate
          }
          //println("3: "+z+","+z.parent+","+z.parent.parent)
          z.parent.black = true
          z.parent.parent.black = false
          z.parent.parent.right_rotate
        }
      } else {
        val y = z.parent.parent.left
        if (!y.black) {
          //println("4: "+z+","+z.parent+","+z.parent.parent)
          z.parent.black = true
          y.black = true
          z.parent.parent.black = false
          z = z.parent.parent
        } else {
          if (z.eq(z.parent.left)) {
            //println("5: "+z+","+z.parent+","+z.parent.parent)
            z = z.parent
            z.right_rotate
          }
          //println("6: "+z+","+z.parent+","+z.parent.parent)
          z.parent.black = true
          z.parent.parent.black = false
          z.parent.parent.left_rotate
        }
      }
    }
    //println("Balancing done!")
    root.black = true
    //printTree(root, 1)
  }

  private def insertTree(key: DBKey, data: DBRow, c: TreeNode, p: TreeNode) {
   /* println(c+","+c.left+","+c.right)
    if (c.eq(c.left) || c.eq(c.right)) {
      println(root+","+root.left+","+root.right)
    }*/
    if (c.eq(ZNode)) {
      // we've found where to insert the data
      val node = new RealNode(key, ArrayBuffer(data), false, parent = p)
      if (p.eq(ZNode)) {
        // we have a new root
        root = node
      } else if (key < p.key) {
        // insert as left child
        p.left = node
      } else {
        // insert as right child
        p.right = node
      }
      fixInsert(node)
    } else {
      if (c.key.equals(key)) {
        // just add it to the list
        c.rows += data
      } else if (key < c.key) {
        // search left subtree
        insertTree(key, data, c.left, c)
      } else {
        // search right subtree
        insertTree(key, data, c.right, c)
      }
    }
  }

  def insert(key: DBKey, data: DBRow) {
    //println("-----------")
    insertTree(key, data, root, ZNode)
  }

  // returns the TreeNode that matches the given key
  private def searchTree(key: DBKey, p: TreeNode): TreeNode = {
    if (p.eq(ZNode)) {
      ZNode
    } else {
      if (p.key.equals(key)) {
        p
      } else if (key < p.key) {
        searchTree(key, p.left)
      } else {
        searchTree(key, p.right)
      }
    }
  }

  // returns the rows matching the key
  def searchExact(key: DBKey): Seq[DBRow] = {
    val r = searchTree(key, root) 
    if (r != ZNode) {
      r.rows
    } else {
      List()
    }
  }

  override def supportsRangeSearch = true

  private def searchTreeRange(low: DBKey, high: DBKey, lI: Boolean, 
                              hI: Boolean, n: TreeNode, result: ArrayBuffer[DBRow]) {
    if (!n.eq(ZNode)) {
      if (low < n.key) {
        // check left subtree too
        searchTreeRange(low, high, lI, hI, n.left, result)
      }
      if (low < n.key && n.key < high) {
        result ++= n.rows
      } else if (lI && low <= n.key && n.key < high) {
        result ++= n.rows
      } else if (hI && low < n.key && n.key <= high) {
        result ++= n.rows
      }
      if (high > n.key) {
        // check right subtree
        searchTreeRange(low, high, lI, hI, n.right, result)
      }
    }
  }

  override def searchRange(low: DBKey, high: DBKey, lowInclusive: Boolean, 
                           highInclusive: Boolean): Seq[DBRow] = {
    val result = ArrayBuffer[DBRow]()
    searchTreeRange(low, high, lowInclusive, highInclusive, root, result)
    result
  }
}
