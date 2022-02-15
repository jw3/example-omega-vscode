example omega edit extension
===

Example VS Code plugin that uses Scala [Omega Edit](https://github.com/scholarsmate/omega-edit) bindings to demonstrate interaction over both gRPC and WebSockets.

## areas for improvement
- the proto file is currently duplicated between the server and client projects
  - the proto compilation for the client wants to create a nested path based on the input directory which complicates sharing it from a central location
  - ideally we would keep it at a top level directory and specify the path in the package.json and load that in the server and client builds
- the input area in the client is pretty naive in that it is populated with the entire contents of the session
  - this should instead be a viewport that moves based on input keys/scrollbar
- the file name that is opened, `build.sbt`, is hardcoded in the gui client
  - passing the filename as a launch profile argument would make for a good example of how it will be used in real use

## build and launch

Install the Omega Edit shared library in ld searched path or set `LD_LIBRARY_PATH`.

Omega Edit shared library >= `53f4c4240b85a6e5c7cf2ff8c65c92082421a2d7` suggested.

### backend 

`sbt compile grpc/run`
or
`sbt compile websocket/run`

### frontend

The frontend is wired for both the WebSocket and gRPC backends, using commands `omega.websocket` and `omega.grpc` respectively.

To launch the 

```
yarn
yarn package
code --extensionDevelopmentPath=<this-project-dir>
```

### show extension

Type omega at the command palette (`ctrl+shift+p`)


![gif](doc/demo.gif)

### references
- https://github.com/scholarsmate/omega-edit
- https://github.com/microsoft/vscode-extension-samples
- https://github.com/badsyntax/grpc-js-typescript
