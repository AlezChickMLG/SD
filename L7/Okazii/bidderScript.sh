#!/bin/bash

app="/home/alex26/Documents/Laboratoare Materii/Sem2/SD/L7/Okazii/out/artifacts/BidderMicroservice_jar/BidderMicroservice.jar"

for i in {1..3}
do
	java -jar "$app" &
done
