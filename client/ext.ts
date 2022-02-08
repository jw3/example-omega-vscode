import * as vscode from 'vscode';
import * as WebSocket from 'ws';
import {RawData} from "ws";

export function activate(ctx: vscode.ExtensionContext) {
    ctx.subscriptions.push(
        vscode.commands.registerCommand('omega.simple', async () => {
            const panel = vscode.window.createWebviewPanel(
                'viewport',
                'Ω Edit',
                vscode.ViewColumn.One,
                {}
            );

            let map = new Map<number, RawData>();
            for (let i = 0; i < 9; ++i) {
                let ws = new WebSocket(`ws://localhost:9000/api/view/${10 * i}/10`);
                ws.on('open', function open() {
                    console.log('opened');
                });
                ws.on('message', function message(data) {
                    map.set(i, data)
                    panel.webview.html = getWebviewContent(map);
                    console.log('received: %s', data);
                });
            }
        })
    )
}

function getWebviewContent(state: Map<number, RawData>) {
    return `<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Omega!</title>
</head>
<body>
     <div class="grid-container">
      <div class="grid-item">${state.get(0)}</div>
      <div class="grid-item">${state.get(1)}</div>
      <div class="grid-item">${state.get(2)}</div>
      <div class="grid-item">${state.get(3)}</div>
      <div class="grid-item">${state.get(4)}</div>
      <div class="grid-item">${state.get(5)}</div>
      <div class="grid-item">${state.get(6)}</div>
      <div class="grid-item">${state.get(7)}</div>
      <div class="grid-item">${state.get(8)}</div>
    </div> 
</script>
</body>
</html>`;
}
