#!/bin/bash
for f in receipts/*.jpg ; do
  NAME=`echo "$(basename $f)" | cut -d'.' -f1`
  java -Djava.library.path="../lib" -jar JumboReceiptReader.jar $f > out/$NAME.json
done
echo "Written to ./out folder"
