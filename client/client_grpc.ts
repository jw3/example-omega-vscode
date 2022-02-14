import * as vscode from 'vscode';
import * as grpc from '@grpc/grpc-js';
import {EditorClient} from "./client/omega_edit_grpc_pb";
import {Empty} from 'google-protobuf/google/protobuf/empty_pb';
import {CreateSessionRequest, CreateViewportRequest, ObjectId, ViewportDataRequest} from "./client/omega_edit_pb";

function getVersion(c: EditorClient): Promise<string> {
    return new Promise<string>((resolve, reject) => {
        c.getOmegaVersion(new Empty(), (err, v) => {
            if(err) {
                return reject(err);
            }
            if(!v) {
                return reject("undefined version")
            }
            return resolve(`v${v.getMajor()}.${v.getMinor()}.${v.getPatch()}`);
        });
    })
}

function newSession(c: EditorClient, path: string | undefined): Promise<ObjectId> {
    return new Promise<ObjectId>((resolve, reject) => {
        let request = new CreateSessionRequest()
        if(path) request.setFilePath(path)
        c.createSession(request, (err, r) => {
            if(err) {
                console.log(err.message)
                return reject(err);
            }
            let id = r?.getSessionId();
            if(!id) {
                return reject("undefined version")
            }
            return resolve(id);
        });
    })
}

function newViewport(id: string, c: EditorClient, sid: ObjectId, offset: number, capacity: number): Promise<ObjectId> {
    return new Promise<ObjectId>((resolve, reject) => {
        let request = new CreateViewportRequest();
        let vid = new ObjectId()
        vid.setId(id)
        request.setViewportId(vid)
        request.setSessionId(sid);
        request.setOffset(offset);
        request.setCapacity(capacity);
        c.createViewport(request, (err, r) => {
            if(err) {
                return reject(err);
            }
            let id = r?.getViewportId();
            if(!id) {
                return reject("undefined version")
            }
            return resolve(id);
        });
    })
}

export function activate(ctx: vscode.ExtensionContext) {
    ctx.subscriptions.push(
        vscode.commands.registerCommand('omega.grpc', async () => {
            let panel = vscode.window.createWebviewPanel(
                'viewport',
                'Ω Edit gRPC',
                vscode.ViewColumn.One,
                {
                    enableScripts: true
                }
            );

            let uri = "127.0.0.1:9000"
            panel.webview.html = getWebviewContent(uri);

            let c = new EditorClient(uri, grpc.credentials.createInsecure());
            let v = await getVersion(c);
            panel.webview.postMessage({ command: 'version', text: v });

            let s = await newSession(c, "build.sbt");
            panel.webview.postMessage({ command: 'session', text: s.getId() });

            let vp = await newViewport(c, s, 0, 50);

            let vpdr = new ViewportDataRequest()
            vpdr.setViewportId(vp)
            c.getViewportData(vpdr, (err, r) => {
                let data = r?.getData_asB64();
                if (data) {
                    let txt = Buffer.from(data, 'base64').toString('binary');
                    panel.webview.postMessage({command: 'viewport', text: txt});
                }
            });
        })
    );
}


function getWebviewContent(uri: string) {
    return `<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Omega gRPC</title>
</head>
<body>
    <div id="server">${uri}</div>
    <div id="version">v?</div>
    <div id="session">?</div>
    <div id="viewport">empty</div>
    
    <script>
        window.addEventListener('message', event => {
            const message = event.data;
            document.getElementById(message.command).innerHTML = message.text
        });
    </script>
</body>
</html>`;
}
