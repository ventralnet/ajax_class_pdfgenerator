#!/bin/sh

INPUT_FOLDER=/home/matt/src/school/ajax/assignment4/src
OUTPUT_FILENAME=out.pdf
java -cp build/libs/ajax_class_pdfgenerator.jar edu.kirkley.util.AjaxClassPDFGenerator -i $INPUT_FOLDER -o $OUTPUT_FILENAME
