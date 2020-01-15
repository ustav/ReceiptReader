#!/bin/bash
for f in receipts/*.jpg ; do
  NAME=`echo "$(basename $f)" | cut -d'.' -f1`
  java -Djna.library.path="../lib" -Djava.library.path="../lib" -jar JumboReceiptReader.jar $f > out/$NAME.json
done
echo "Written to ./out folder"
