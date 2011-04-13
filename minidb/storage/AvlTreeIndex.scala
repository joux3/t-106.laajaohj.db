package minidb.storage

class AvlTreeException (val message: String) extends Exception(message)

class AvlTreeIndex(override val indexName: String, columnNums: Seq[Int])
  extends Index(indexName, columnNums) {

  class AvlTreeNode(
    val child_left: AvlTreeNode,
    val child_right: AvlTreeNode,
    val search_key: DBKey,
    val value: DBRow) {
    
    //Guard, invalid trees cannot be created
    if (!this.balanced)
      throw new AvlTreeException("Trying to create invalid avlTree, not balanced! " + 
          "Balance factor = " + this.balanceFactor)
    
    private val tree_height: Int = calcTreeHeight(child_left,child_right)
    private def calcTreeHeight(left:AvlTreeNode,right:AvlTreeNode) = {
      math.max(
          (if (left != null) left.tree_height + 1 else 0),
          (if (right != null) right.tree_height + 1 else 0)) 
    }
    private def calcTreeHeight(tree:AvlTreeNode): Int = {
      if (tree != null) 
        calcTreeHeight(tree.child_left,tree.child_right)
      else
        0
    }

    def balanceFactor = calcBalance(child_left,child_right)
    private def calcBalance(left:AvlTreeNode,right:AvlTreeNode) = {
      (if (left != null) left.tree_height+1 else 0) -
      (if (right != null) right.tree_height+1 else 0)
    }
    
    def balanced = {
      List(-1, 0, 1).contains(this.balanceFactor)
    }
    
    // Count of nodes in this tree (root, left and right subtree)
    val weight : Int = 
      (if (child_left != null) child_left.weight else 0) +
      (if (child_right != null) child_right.weight else 0) + 1
    
    def searchExact(key: DBKey): Seq[DBRow] = {
      val emptyResult = Seq[DBRow]()

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
        (if (tree != null) tree.tree_height+1 else 0)
      }
      
      if (this.search_key < key || (this.search_key == key && this.balanceFactor>=0)) {
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

          //Is Balanced
          val newBalance = calcBalance(this.child_left,newRight)
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
      } //key <= this.search_key
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

          //Is Balanced
          val newBalance = calcBalance(newLeft,this.child_right)
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
     * Deletes row
     *  
     * @return Returns a new valid root of avl-tree without row or 
     * itself if key is not found 
     */
    def remove(key: DBKey,row: DBRow) : AvlTreeNode = {
      _remove(key,row)
    }
    /**
     * Internal use only.
     */
    private def _remove(key: DBKey,row: DBRow) : AvlTreeNode = {
      def reconstuctTreeWithNewRight(newRight: AvlTreeNode):AvlTreeNode =
      {
        //Is tree_height different?
        if (newRight != null && 
            newRight.tree_height == this.child_right.tree_height)
        {
          //No -> this node is still balanced
          new AvlTreeNode(this.child_left,newRight,
              this.search_key,this.value)
        }
        else
        {
          //Tree height changes => new balance factor
          val newBalance = calcBalance(this.child_left,newRight); 
          
          if (math.abs(newBalance) < 2)
          {
            //Remains balanced
            new AvlTreeNode(this.child_left,newRight,
              this.search_key,this.value)
          }
          else
          {
            if (newBalance != 2) {
              //Invalid state, new subtree height must
              //be original_height or original_height - 1 
              throw new AvlTreeException(
                  "Invalid tree state in remove!" +
                  "Tree height change < -1")
            }
             
            //Tree unbalanced => Rotate tree
            //Rotate right
            val newSubRight = new AvlTreeNode(
                this.child_left.child_right,newRight,
                this.search_key,this.value)
            val newNode = new AvlTreeNode(
                this.child_left.child_left, newSubRight,
                this.child_left.search_key,this.child_left.value)
            
            newNode
          }
        }
      }
      def reconstuctTreeWithNewLeft(newLeft: AvlTreeNode):AvlTreeNode =
      {
        //Is tree_height different?
        if (newLeft != null && 
            newLeft.tree_height == this.child_left.tree_height)
        {
          //No -> this node is still balanced
          new AvlTreeNode(newLeft,this.child_right,
              this.search_key,this.value)
        }
        else
        {
          //Tree height changes => new balance factor
          val newBalance = calcBalance(newLeft,this.child_right); 
          
          if (math.abs(newBalance) < 2)
          {
            //Remains balanced
            new AvlTreeNode(newLeft,this.child_right,
              this.search_key,this.value)
          }
          else
          {
            if (newBalance != -2) {
              //Invalid state, new subtree height must
              //be original_height or original_height - 1 
              throw new AvlTreeException(
                  "Invalid tree state in remove!" +
                  "Tree height change < -1")
            }
             
            //Tree unbalanced => Rotate tree
            //Rotate left
            val newSubLeft = new AvlTreeNode(
                newLeft,this.child_right.child_left,
                this.search_key,this.value)
            val newNode = new AvlTreeNode(
                newSubLeft, this.child_right.child_right,
                this.child_right.search_key,this.child_right.value)
            
            newNode
          }
        }
      }
      //Right subtree changes
      if (this.search_key < key)
      {
        //key not found
        if (this.child_right == null)
        {
          this
        }
        else
        {
          val newRight = this.child_right._remove(key,row);
          
          //Key not found
          if (newRight == this.child_right)
          {
            this
          }
          else
          {
            //Reconstruct tree
            reconstuctTreeWithNewRight(newRight)
          }
        }
      }
      //Left subtree changed
      else if (key < this.search_key)
      {
        //key not found
        if (this.child_left == null)
        {
          this
        }
        else
        {
          val newLeft = this.child_left._remove(key,row);
          
          //Key not found
          if (newLeft == this.child_left)
          {
            this
          }
          else
          {
            //Reconstruct tree
            reconstuctTreeWithNewLeft(newLeft)
          }
        }
      }
      //key == search_key
      else
      {         
        //Remove this node
        if (this.value == row) {
          //No children
          if (this.child_left == null && this.child_right == null)
            null
          //One child
          else if (this.child_left == null && this.child_right != null)
            this.child_right
          else if (this.child_left != null && this.child_right == null)
            this.child_left
          //2 children
          else
          {
            //Remove this node
            
            //This function does not return predecessor. this function finds greatest
            //node of the tree, and removes it returning removed node and new tree
            def getPredecessor(node: AvlTreeNode) : (AvlTreeNode,AvlTreeNode)= {
              if (node.child_right != null)
              {
                val (predecessor,newRight) = getPredecessor(node.child_right)
                
                //Tree height changes => new balance factor
                val newBalance = calcBalance(node.child_left,newRight);
          
                if (newBalance == -2) {
                  //Tree cannot grow or shrink too much
                  throw new AvlTreeException(
                      "Invalid tree state in remove!" +
                      "Tree height change < -1")
                }
                
                if (newBalance == 2)
                {
                  if (node.child_left.balanceFactor >= 0)
                  {
                    //Tree unbalanced => Rotate tree
                    //Rotate right
                    val returnNewRight = new AvlTreeNode(
                        node.child_left.child_right,newRight,
                        node.search_key,node.value)
                    
                    (predecessor, 
                        new AvlTreeNode(
                        node.child_left.child_left, returnNewRight,
                        node.child_left.search_key,node.child_left.value))
                  }
                  else
                  {
                    //Rotate left-right
                    val rotatedLeft =
                      new AvlTreeNode(node.child_left.child_left,node.child_left.child_right.child_left,
                          node.child_left.search_key,node.child_left.value)
                    val rotatedRight =
                      new AvlTreeNode(node.child_left.child_right.child_right,newRight,
                          node.search_key,node.value)
                    val newRoot = 
                      new AvlTreeNode(rotatedLeft,rotatedRight,
                          node.child_left.child_right.search_key,node.child_left.child_right.value)
                    
                    (predecessor, newRoot)
                  }
                }
                else
                {
                  (predecessor,new AvlTreeNode(node.child_left,newRight,node.search_key,node.value))
                }
              }
              else
              {
                (node,node.child_left)
              }
            }
            //Same as above
            def getSuccessor(node: AvlTreeNode) : (AvlTreeNode,AvlTreeNode)= {
              if (node.child_left != null)
              {
                val (predecessor,newLeft) = getSuccessor(node.child_left)
                
                //Tree height changes => new balance factor
                val newBalance = calcBalance(newLeft,node.child_right);
          
                if (newBalance == 2) {
                  //Tree cannot grow or shrink too much
                  throw new AvlTreeException(
                      "Invalid tree state in remove!" +
                      "Tree height change < -1")
                }
                
                if (newBalance == -2)
                {
                  if (node.child_left.balanceFactor <= 0)
                  {
                    //Tree unbalanced => Rotate tree
                    val returnNewLeft = new AvlTreeNode(
                        newLeft,node.child_right.child_left,
                        node.search_key,node.value)
                    (predecessor, 
                        new AvlTreeNode(
                        returnNewLeft, node.child_right.child_right,
                        node.child_right.search_key,node.child_right.value))
                  }
                  else
                  {
                    //Rotate right-left
                    val rotatedLeft = 
                      new AvlTreeNode(newLeft,node.child_right.child_left.child_left,
                            node.search_key,node.value)
                    val rotatedRight = 
                      new AvlTreeNode(node.child_right.child_left.child_right,node.child_right.child_right,
                            node.child_right.search_key,node.child_right.value)
                    val newRoot =
                      new AvlTreeNode(rotatedLeft,rotatedRight,
                            node.child_right.child_left.search_key,node.child_right.child_left.value)
                    
                    (predecessor, newRoot)
                  }
                }
                else
                {
                  (predecessor,new AvlTreeNode(newLeft,node.child_right,node.search_key,node.value))
                }
              }
              else
              {
                (node,node.child_right)
              }
            }
            
            if (this.balanceFactor == 1)
            {
              val (predecessor,route) = getPredecessor(this.child_left)
              new AvlTreeNode(route,this.child_right,predecessor.search_key,predecessor.value)
            }
            else if (this.balanceFactor == -1)
            {
              val (successor,route) = getSuccessor(this.child_right)
              new AvlTreeNode(this.child_left,route,successor.search_key,successor.value)
            }
            else // this.balanceFactor == 0
            {
              //        D                       F  
              //      /   \                  /    \ 
              //     B     F     ->        B        G
              //    / \   / \             / \
              //   A   C E   G           A   C 
              //                               \
              //                                E
              //          C           
              //        /   \       
              //  ->   B     F     
              //      / \   / \    
              //     A   - E   G    
              //                   
              
              val nodeB = this.child_left; //not null
              val nodeF = this.child_right;//not null
              
              val nodeA = nodeB.child_left; //can be null
              val nodeC = nodeB.child_left; // -- 
              val nodeE = nodeF.child_left; //can be null
              val nodeG = nodeF.child_left; // --
              
              
              if (nodeA == null && nodeC == null)
              {
                // balance==0 => nodeE == null && nodeG == null
                //Sliding nodeB to root
                new AvlTreeNode(null,nodeF,nodeB.search_key,nodeB.value)
              }
              else if (nodeC != null)
              {
                // balance==0 => nodeE != null || nodeG != null
                val (predecessor,route) = getPredecessor(nodeB)
                new AvlTreeNode(route,nodeF,predecessor.search_key,predecessor.value)
              }
              else if (nodeE != null)
              {
                // balance==0 => nodeA != null
                val (successor,route) = getSuccessor(nodeF)
                new AvlTreeNode(nodeB,route,successor.search_key,successor.value)
              }
              else 
              {
                //nodeA != null && nodeG != null
                //nodeC == null && nodeE == null
                
                //nodeA && nodeG have no children, because
                //otherwise nodeB & F would be unbalanced
                
                if (nodeA.child_left!=null || nodeA.child_right!=null ||
                    nodeG.child_left!=null || nodeG.child_right!=null)
                  throw new AvlTreeException("Tree corrupted in remove, should never happen!")
                
                new AvlTreeNode(nodeA,nodeF,nodeB.search_key,nodeB.value)
              }
            }
          }
        }
        else  {
          // Trying to delete node with equal key, but different row
          // wanted row could be in left or right subtree
          val newLeft = 
            if (this.child_left == null)
              null
            else
              this.child_right._remove(key,row)
          
          //Is the row in the left branch?
          if (newLeft != this.child_left)
          {
            //It is
            reconstuctTreeWithNewLeft(newLeft)
          }
          else
          {
            //Is row in right branch?
            val newRight =
              if (this.child_right == null)
                null
              else
                this.child_right._remove(key,row)
                
            if (newRight != this.child_right)
            {
              //It is
              reconstuctTreeWithNewRight(newRight)
            }
            else
              this //row was not found
          }
        }
      }
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
  override def delete(key: DBKey, data: DBRow) = {
    if (root != null)
    {
      val newroot = root.remove(key, data)
      
      //Tree didn't change
      if (root == newroot)
        throw new IndexDeleteFailed("tried to delete a nonexistent row!")
      
      root = newroot;
    }
  }
  def insert(key: DBKey, data: DBRow) = {
    if (root == null)
      root = new AvlTreeNode(null, null, key, data)
    else
      root = root.insert(key, data)
  }
 

  override def supportsRangeSearch: Boolean = true
  override def supportsDelete: Boolean = true

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
