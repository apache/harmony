#!/bin/sh

#    Licensed to the Apache Software Foundation (ASF) under one or more
#    contributor license agreements.  See the NOTICE file distributed with
#    this work for additional information regarding copyright ownership.
#    The ASF licenses this file to You under the Apache License, Version 2.0
#    (the "License"); you may not use this file except in compliance with
#    the License.  You may obtain a copy of the License at
#
#       http://www.apache.org/licenses/LICENSE-2.0
#
#    Unless required by applicable law or agreed to in writing, software
#    distributed under the License is distributed on an "AS IS" BASIS,
#    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
#    See the License for the specific language governing permissions and
#    limitations under the License.

# @author: Alexei Fedotov

convert_html_to_list() {
  find "$directory" \
    -name 'globals.html' \
    -o -name 'globals_*' \
    -o -name 'hierarchy.html' \
    -o -name 'files.html' \
    -o -name 'namespaces.html' \
    -o -name 'namespacemembers*' \
    -o -name 'functions.html' \
    -o -name 'functions_*' \
    -o -name '*.html' \
    -exec echo perl `dirname $0`/html_to_list.pl {} \>{}.txt \; \
    >doc_quality/html_to_list.sh

  source doc_quality/html_to_list.sh
}

clean_files() {
  rm -rf doc_quality
  find "$directory" -name '*.html.txt' -exec rm {} \;
}

merge_dictionary() {
  find "$directory" -name '*.html.txt' -exec cat {} \; |
    perl -n -e '/^[A-Za-z][a-z]{2,}$/ && print' | sort |
    uniq -c | sort -nr | awk '{print $2}' >doc_quality/dictionary.txt
}

create_aspell_dictionary() {
  words=`cat doc_quality/dictionary.txt | wc -l`
  half=`expr $words / 2`
  tail -n $half doc_quality/dictionary.txt >doc_quality/half.txt

  (
    aspell list <doc_quality/half.txt 
    cat doc_quality/half.txt
  ) | sort | uniq -u >doc_quality/aspell.txt


  cat <<EOF >doc_quality/estimate_doc_file.pl
my (\$num, \$sum) = ($scale, 0);
while (<>) {
  \$num++;
EOF
  perl -n -e 'chomp; print "  if (/^$_\$/) { \$sum++; next }\n"' \
    >>doc_quality/estimate_doc_file.pl <doc_quality/aspell.txt
  cat <<EOF >>doc_quality/estimate_doc_file.pl
}
print int($scale * \$sum / \$num) . " \$ARGV\\n";
EOF
}


estimate_doc_quality() {
  find "$directory" -name '*.html.txt' | while read file
    do
      perl doc_quality/estimate_doc_file.pl "$file"
  done >doc_quality/unsorted_result.txt
  sort -nr <doc_quality/unsorted_result.txt >doc_quality/result.txt
}

directory=${1:-.}
scale=128

clean_files
mkdir doc_quality 

convert_html_to_list
merge_dictionary
create_aspell_dictionary
estimate_doc_quality


