#!/bin/sh

INPUT_FOLDER=/home/matt/src/school/ajax/assignment2/src
OUTPUT_FILENAME=out.pdf
groovy codetopdfgenerator.groovy -i $INPUT_FOLDER -o ./out.pdf
