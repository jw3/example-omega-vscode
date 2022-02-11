example omega edit extension
===

Example VS Code plugin that uses embedded [Omega Edit](https://github.com/scholarsmate/omega-edit) bindings to generate content.

## build and launch

Install the Omega Edit shared library in ld searched path or set `LD_LIBRARY_PATH`.

Omega Edit shared library >= `53f4c4240b85a6e5c7cf2ff8c65c92082421a2d7` suggested.

### backend 

`sbt compile grpc/run`
or
`sbt compile websocket/run`

### frontend

At this time the frontend is only wired for the websocket backend

```
yarn
yarn package
code --extensionDevelopmentPath=<this-project-dir>
```

### show extension

Type omega at the command palette (`ctrl+shift+p`)


### references
- https://github.com/scholarsmate/omega-edit
- https://github.com/microsoft/vscode-extension-samples
