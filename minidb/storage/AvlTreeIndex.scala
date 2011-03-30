package minidb.storage

class AvlTreeIndex(override val indexName: String, columnNums: Seq[Int])
  extends Index(indexName, columnNums) {

  class AvlTreeNode(
    val child_left: AvlTreeNode,
    val child_right: AvlTreeNode,
    val search_key: DBKey,
    val value: DBRow) {
    
    private val tree_height: Int = {
      math.max(
          (if (child_left != null) child_left.tree_height + 1 else 0),
          (if (child_right != null) child_right.tree_height + 1 else 0)) 
    }

    def balanceFactor = {
      (if (child_left != null) child_left.tree_height else 0) -
        (if (child_right != null) child_right.tree_height else 0)
    }
    def balanced = {
      List(-1, 0, 1).contains(this.balanceFactor)
    }

    def searchExact(key: DBKey): Seq[DBRow] = {
      val emptyResult = Seq[DBRow]()

      // Should never happen
      if (!this.balanced)
        throw new Exception("Tree corrupted, not balanced!")
      
      if (this.search_key < key) {
        if (child_right != null)
          child_right.searchExact(key)
        else
          emptyResult
      }
      else if (key < this.search_key) {
        if (child_left != null)
          child_left.searchExact(key)
        else
          emptyResult
      }
      else //key == this.search_key
      {
        val left =
          if (child_left != null)
            child_left.searchExact(key)
          else
            emptyResult
        val right =
          if (child_right != null)
            child_right.searchExact(key)
          else
            emptyResult

        left ++ List(value) ++ right
      }
    }
    def searchRange(low: DBKey,
      high: DBKey,
      lowInclusive: Boolean,
      highInclusive: Boolean): Seq[DBRow] = {
      val emptyResult = Seq[DBRow]()

      if (this.search_key < low) {
        if (child_right != null)
          child_right.searchRange(low, high, lowInclusive, highInclusive)
        else
          emptyResult
      }
      else if (high < this.search_key) {
        if (child_left != null)
          child_left.searchRange(low, high, lowInclusive, highInclusive)
        else
          emptyResult
      }
      else {
        val left =
          if (child_left != null)
            if (!lowInclusive && low == this.search_key)
              emptyResult
            else
              child_left.searchRange(low, high, lowInclusive, highInclusive)
          else
            emptyResult
        val right =
          if (child_right != null)
            if (!highInclusive && high == this.search_key)
              emptyResult
            else
              child_right.searchRange(low, high, lowInclusive, highInclusive)
          else
            emptyResult

        val myValue =
          if ((low < this.search_key && this.search_key < high) ||
            (lowInclusive && low == this.search_key) ||
            (highInclusive && high == this.search_key))
            List(value)
          else
            emptyResult

        left ++ myValue ++ right
      }
    }
    /** Creates new tree with inserted data */
    def insert(key: DBKey, data: DBRow): AvlTreeNode = {
      this._insert(key, data)._1
    }
    /**
     * Internal use only.
     *
     *
     * Returns new tree and partial path to the new node
     * @return (new tree, child)
     */
    private def _insert(key: DBKey, data: DBRow): (AvlTreeNode, AvlTreeNode)={
      def getTreeHeight(tree:AvlTreeNode) = 
      {
        (if (tree != null) tree.tree_height else 0)
      }
      
      if (this.search_key <= key) {
        if (this.child_right == null) {
          val newRight =
            new AvlTreeNode(null, null, key, data)
          val newRoot = 
            new AvlTreeNode(this.child_left, newRight, 
                this.search_key, this.value)
          
          (newRoot, newRight)
        }
        else {
          val insertResult = this.child_right._insert(key, data)
          val newRight = insertResult._1
          val grandChild = insertResult._2

          val leftHeight = getTreeHeight(this.child_left)
          val rightHeight = newRight.tree_height

          //Is Balanced
          val newBalance = leftHeight - rightHeight;
          if (math.abs(newBalance) < 2) { //Balanced
            val newNode = 
              new AvlTreeNode(this.child_left, newRight, 
                      this.search_key, this.value)
            
            (newNode, newRight)
          }
          else {
            //Tree is not balanced, rotate tree
            if (newRight.child_left == grandChild) {
              //Right-left case
              //Rotate Right-left to root:
              //"new root"->left to root->right
              //"new root"->right to right->left
              //root to "new root"->left
              //right to "new root"->right
              val rotatedLeft = 
                new AvlTreeNode(this.child_left,grandChild.child_left,
                      this.search_key,this.value)
              val rotatedRight = 
                new AvlTreeNode(grandChild.child_right,newRight.child_right,
                      newRight.search_key,newRight.value)
              val newRoot =
                new AvlTreeNode(rotatedLeft,rotatedRight,
                      grandChild.search_key,grandChild.value)
              
              (newRoot, newRoot.child_right)
            }
            else { //newRight.child_right == grandChild
              //Right-Right case
              //Rotate Right to root:
              //"new root"->left to "old root"->right
              //"old root" to "new root"->left
              val oldRoot = 
                new AvlTreeNode(this.child_left,newRight.child_left,
                    this.search_key,this.value)
              val newRoot = 
                new AvlTreeNode(oldRoot,newRight.child_right,
                     newRight.search_key,newRight.value)
              
              //return new root
              (newRoot, newRoot.child_right)
            } 
          }
        }
      } //key < this.search_key
      else {
        if (this.child_left == null) {
          val newLeft =
            new AvlTreeNode(null, null, key, data)
          val newRoot = 
            new AvlTreeNode(newLeft,this.child_right, 
                this.search_key, this.value)
          
          (newRoot, newLeft)
        }
        else {
          val insertResult = this.child_left._insert(key, data)
          val newLeft = insertResult._1
          val grandChild = insertResult._2

          val leftHeight = newLeft.tree_height 
          val rightHeight = getTreeHeight(this.child_right)

          //Is Balanced
          val newBalance = leftHeight - rightHeight;
          if (math.abs(newBalance) < 2) { //Balanced
            val newNode = 
              new AvlTreeNode(newLeft, this.child_right, 
                      this.search_key, this.value)
            
            (newNode, newLeft)
          }
          else {
            //Tree is not balanced
            if (newLeft.child_left == grandChild) {
              //Left-left case
              //Rotate Left to root
              //"new root"->right to "old root"->left
              //"old root" to "new root"->right
              val oldRoot = 
                new AvlTreeNode(newLeft.child_right,this.child_right,
                      this.search_key,this.value)
              val newRoot = 
                new AvlTreeNode(newLeft.child_left,oldRoot,
                    newLeft.search_key,newLeft.value)
              
              (newRoot,newRoot.child_left)
            }
            else 
            {
              //Left-right case
              //Rotate Left-right to root
              //"new root"->left to left->right
              //"new root"->right to root->left
              //root to "new root"->right
              //right to "new root"->left
              val rotatedLeft =
                new AvlTreeNode(newLeft.child_left,grandChild.child_left,
                    newLeft.search_key,newLeft.value)
              val rotatedRight =
                new AvlTreeNode(grandChild.child_right,this.child_right,
                    this.search_key,this.value)
              val newRoot = 
                new AvlTreeNode(rotatedLeft,rotatedRight,
                    grandChild.search_key,grandChild.value)
              
              (newRoot,newRoot.child_left)
            }
          }
        }
      }
    }
    
    /**
     * Deletes node with specified key
     *  
     * @return Returns new valid root of avl-tree without key or 
     * itself if key is not found 
     */
    def remove(key: DBKey) : AvlTreeNode = {
      // XXX Not implemented
      null
    }
  }

  private var root: AvlTreeNode = null;

  def searchExact(key: DBKey): Seq[DBRow] = {
    if (root == null)
      Seq[DBRow]()
    else
      root.searchExact(key)
  }

  def clear() = {
    root = null
  }
  def insert(key: DBKey, data: DBRow) = {
    if (root == null)
      root = new AvlTreeNode(null, null, key, data)
    else
      root = root.insert(key, data)
  }

  override def supportsRangeSearch: Boolean = true

  override def searchRange(low: DBKey,
    high: DBKey,
    lowInclusive: Boolean,
    highInclusive: Boolean): Seq[DBRow] = {
    if (root == null)
      Seq[DBRow]()
    else
      root.searchRange(low, high, lowInclusive, highInclusive)
  }
}
