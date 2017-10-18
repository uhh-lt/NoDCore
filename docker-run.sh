#!/bin/bash
set -e -o pipefail

rm -rf corpus
java -jar /app/NoDCore-assembly-1.0.jar --dir /app/content --format compressed && rm -rf /app/content/*
