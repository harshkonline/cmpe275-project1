#!/bin/bash
#
# build the protobuf classes from the data.proto.
#
# *  *  *  *  *  *  *  *  *  *  *  *  *  *  *  *
# NOTE: This only needs to be performed when/if the protobuf structures change!
#

# CHANGE ME: this needs to be your specific path to the project
project_base="/Users/gash/workspace/messaging/core-netty"

rm -r ${project_base}/generated/*
protoc --proto_path=${project_base}/resources --java_out=${project_base}/generated ${project_base}/resources/comm.proto
