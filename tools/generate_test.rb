require './data.rb' # don't touch!
rd = RandomData.new() # don't touch! (yes, this is ugly)
# this is a tool for generating sql test files
# below is a sample test

ENTRY_COUNT = 10_000 # how many entries to add
TABLE_TYPE = "a INT, b INT, country TEXT, name TEXT"
ROW_GENERATOR = lambda {"#{rd.get_integer(0, ENTRY_COUNT)}, #{rd.get_integer(0, 200)}, \"#{rd.get_country}\", \"#{rd.get_name}\""}

# tests to generate
# inits table & entries
generate_init()
generate_part("Testing searching on a index")
generate_index("rbtree", "a")
# generate search accepts search count & a generator for where expressions
generate_search(10_000, lambda {"a=#{rd.get_integer(0, ENTRY_COUNT)}"})
generate_part("Testing searching on b index")
generate_index("rbtree", "b")
generate_search(10_000, lambda {"b=#{rd.get_integer(0, 200)}"})
generate_part("Testing searching on a, b index")
generate_index("rbtree", "a, b")
generate_search(10_000, lambda {"a=#{rd.get_integer(0, ENTRY_COUNT)} AND b=#{rd.get_integer(0, 200)}"})
