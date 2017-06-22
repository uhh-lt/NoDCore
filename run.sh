#!/usr/bin/env bash
rm -r corpus
dirs=(/Users/seidmuhieyimam/data/Goethekorpus/bz2/*)
for dir in "${dirs[@]}"
do
    java -jar NoDCore-assembly-1.0.jar --dir "$dir" --format compressed
    sleep 5
    rm -r corpus
done
