#!/bin/bash
java -Djava.library.path="../lib" -jar JumboReceiptReader.jar receipts/jumbo1.jpg > out/receipt.json
echo "Written to out/receipt1.json"
