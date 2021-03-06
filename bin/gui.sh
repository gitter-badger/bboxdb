#!/bin/bash
#
# Start the GUI of the BBoxDB 
#
#
#########################################

# Is the environment configured?
if [ -z "$BBOXDB_HOME" ]; then
   echo "Your environment variable \$(BBOXDB_HOME) is empty. Please check your .bboxdbrc"
   exit -1
fi

# Load all required functions and variables
source $BBOXDB_HOME/bin/bootstrap.sh

echo "Start the GUI...."

java $jvm_ops_tools -cp $classpath org.bboxdb.tools.gui.Main

exit 0
