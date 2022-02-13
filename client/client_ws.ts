import * as vscode from 'vscode';
import * as WebSocket from 'ws';

class Content {
    constructor(
        readonly data: string,
        readonly offset = 0,
        readonly type = "Overwrite"
    ) {
    }
}

class View {
    constructor(
        readonly id: string,
        readonly offset: number,
        readonly length: number,
        readonly type = "View"
    ) {
    }
}

interface ViewUpdate {
    id: string;
    data: string
}


export function activate(ctx: vscode.ExtensionContext) {
    ctx.subscriptions.push(
        vscode.commands.registerCommand('omega.websocket', async () => {
            const panel = vscode.window.createWebviewPanel(
                'viewport',
                'Ω Edit WebSockets',
                vscode.ViewColumn.One,
                {
                    enableScripts: true
                }
            );

            let ws = new WebSocket(`ws://localhost:9000/api/view`);
            let map = new Map<number, string>();
            let content = ""

            ws.on('message', function message(data) {
                let msg: ViewUpdate = JSON.parse(data.toString())
                map.set(Number(msg.id), msg.data)
                panel.webview.html = getWebviewContent(content, map);
                console.log('received: %s', data);
            });

            ws.on('open', function open() {
                for (let i = 0; i < 8; ++i) {
                    let view = new View(i.toString(), i * 10, 10)
                    ws.send(JSON.stringify(view))
                }
            });

            panel.webview.onDidReceiveMessage(message => {
                    switch (message.command) {
                        case 'send':
                            content = message.text
                            let c = new Content(message.text)
                            ws.send(JSON.stringify(c))
                    }
                },
                undefined,
                ctx.subscriptions
            );


        })
    )
}

function getWebviewContent(content: string, state: Map<number, string>) {
    return `<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Omega!</title>
    <style>
        .grid-container {
          display: grid;
          grid-gap: 5px 5px;
          grid-template-columns: auto auto auto;
          background-color: #2196F3;
          padding: 10px;
        }

        .grid-item {
          background-color: rgba(255, 255, 255, 0.8);
          border: 1px solid rgba(0, 0, 0, 0.8);
          padding: 20px;
          font-size: 30px;
          text-align: center;
          color: black;
        }
    </style>
</head>
<body>
    <div class="grid-container">
        <div class="grid-item">${state.get(0)}</div>
        <div class="grid-item">${state.get(1)}</div>
        <div class="grid-item">${state.get(2)}</div>
        <div class="grid-item">${state.get(3)}</div>
        <div class="grid-item"><textarea rows="10" cols="50" id="input" oninput="sendit(this.value)">${content}</textarea></div>
        <div class="grid-item">${state.get(4)}</div>
        <div class="grid-item">${state.get(5)}</div>
        <div class="grid-item">${state.get(6)}</div>
        <div class="grid-item">${state.get(7)}</div>
    </div>
    <script>
      (function () {
          let input = document.getElementById("input");
          input.focus();
          input.setSelectionRange(input.value.length, input.value.length);
      })()
      const vscode = acquireVsCodeApi();
        function sendit(value) {
            vscode.postMessage({
                command: 'send',
                text: value
            })
        }
    </script> 
</body>
</html>`;
}
