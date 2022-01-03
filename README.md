example omega edit extension
===

Example VS Code plugin that uses embedded [Omega Edit](https://github.com/scholarsmate/omega-edit) bindings to generate content.


### requires
- bindings compiled against Electron Node v14.16.0
- VS Code v1.60-1.63


### build and launch

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
