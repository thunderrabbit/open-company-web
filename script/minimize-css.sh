#!/bin/bash

cat $1/target/public/css/normalize.css > $2.temp
cat $1/target/public/css/app.main.css >> $2.temp
cat $1/target/public/css/emojione/autocomplete.css >> $2.temp
cat $1/target/public/css/medium-editor/medium-editor.css >> $2.temp
cat $1/target/public//css/medium-editor/default.css >> $2.temp
cat $1/target/public/css/emojione.css >> $2.temp
cat $1/target/public/css/emojione-picker.css >> $2.temp
cat $1/target/public/css/emojione.sprites.css >> $2.temp
cat $1/target/public/css/material-design-icons/v2.0.46/materialdesignicons.min.css >> $2.temp

cssnano --no-zindex < $2.temp > $2