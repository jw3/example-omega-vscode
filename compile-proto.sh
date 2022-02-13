#!/usr/bin/env bash

OUT_DIR="out"
TS_OUT_DIR="client"
IN_DIR="client"
PROTOC="$(yarn bin)/grpc_tools_node_protoc"
PROTOC_GEN_TS_PATH="$(yarn bin)/protoc-gen-ts"
PROTOC_GEN_GRPC_PATH="$(yarn bin)/grpc_tools_node_protoc_plugin"

mkdir -p ${OUT_DIR} || true

$PROTOC \
    --plugin=protoc-gen-ts=${PROTOC_GEN_TS_PATH} \
    --plugin=protoc-gen-grpc=${PROTOC_GEN_GRPC_PATH} \
    --js_out=import_style=commonjs:${OUT_DIR} \
    --grpc_out=grpc_js:${OUT_DIR} \
    --ts_out=grpc_js:${TS_OUT_DIR} \
    ${IN_DIR}/*.proto
