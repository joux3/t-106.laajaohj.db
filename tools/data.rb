def generate_init()
  puts ".disablenoresult"
  puts ".disableresults"
  puts "CREATE TABLE test (#{TABLE_TYPE})"
  puts ".echo adding #{ENTRY_COUNT} entries"
  puts ".starttiming"
  ENTRY_COUNT.times {
    puts "INSERT INTO test VALUES ("+ROW_GENERATOR.call+");"
  }
  puts ".stoptiming adding entries"
end

def generate_part(desc)
  puts ".echo ------ #{desc}"
end

def generate_search(name, count, where_generator)
  puts ".echo Doing #{count} searches"
  puts ".starttiming"
  count.times {
    puts "SELECT * FROM test WHERE "+where_generator.call
  }
  puts ".stoptiming searching (#{name})"
end

def generate_index(name, index_type, index_columns)
  puts ".echo creating index #{index_type} on (#{index_columns})"
  puts ".starttiming"
  puts "CREATE INDEX #{name} USING #{index_type} ON test (#{index_columns});"
  puts ".stoptiming creating index"
end

def generate_drop_index(index_type)
  puts "DROP INDEX #{index_type} ON test"
end

class RandomData 
  attr_reader :data
  def initialize()
    lines = IO.readlines("data.sql")
    insert_regex = /INSERT INTO .([^ ]+)[^ ] VALUES \((\d+), '([^']+)'/
    
    @data = Hash.new{|hash, key| hash[key] = []}
    lines.each {|line|
      if line =~ insert_regex 
        @data[$1.to_sym] << $3
      end
    }
  end

  def get_name
    @data[:dg_first_names].sample + " " + @data[:dg_surnames].sample
  end

  def get_integer(low, high)
    low + rand(high - low)
  end

  def get_city
    @data[:dg_cities].sample
  end

  def get_country
    @data[:dg_countries].sample
  end
end

if $0 == __FILE__
  rd = RandomData.new()
  puts "Data types: "
  rd.data.keys.each {|key|
    puts "  "+key.to_s
  }
  puts "Random name: "+rd.get_name
end
