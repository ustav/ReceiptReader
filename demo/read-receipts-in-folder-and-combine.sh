#!/bin/bash
printf -v date '%(%Y-%m-%d_%H:%M:%S)T' -1 
OUTPUT=out/$date.json
echo "[" > $OUTPUT
first_line=true
for f in receipts/*.jpg ; do
  NAME=`echo "$(basename $f)" | cut -d'.' -f1`
  if $first_line; 
    then
      first_line=false
    else 
      echo "," >> $OUTPUT
  fi
  java -Djava.library.path="../lib" -jar JumboReceiptReader.jar $f >> $OUTPUT
done
echo "]" >> $OUTPUT
echo "Written to $OUTPUT"

