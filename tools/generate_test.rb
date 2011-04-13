require './data.rb' # don't touch!
rd = RandomData.new() # don't touch! (yes, this is ugly)
# this is a tool for generating sql test files
# below is a sample test

ENTRY_COUNT = 100_000 # how many entries to add
TABLE_TYPE = "a INT, b INT, country TEXT, name TEXT"
ROW_GENERATOR = lambda {"#{rd.get_integer(0, ENTRY_COUNT)}, #{rd.get_integer(0, 200)}, \"#{rd.get_country}\", \"#{rd.get_name}\""}

# tests to generate
# inits table & entries
generate_part("Testing multiple indexes")
generate_part("a contains almost unique numbers, b 500 duplicates per key")
generate_init()
indexes = ["primitivehash", "hash", "rbtree", "avltree", "bplustree"]
indexes.each {|index|
  generate_part("Using index type #{index}")
  generate_index("i1", index, "a")
  generate_index("i2", index, "b")
  generate_index("i3", index, "a, b")
  generate_search("unique", 10_000, lambda {"a=#{rd.get_integer(0, ENTRY_COUNT)}"})
  generate_search("duplicates", 1_000, lambda {"b=#{rd.get_integer(0, 200)}"})
  generate_search("mixed key", 10_000, lambda {"a=#{rd.get_integer(0, ENTRY_COUNT)} AND b=#{rd.get_integer(0, 200)}"}) 
  generate_drop_index("i1")
  generate_drop_index("i2")
  generate_drop_index("i3")
}
