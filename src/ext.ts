import * as vscode from 'vscode';
import * as omega_edit from 'omega-edit';

export function activate(ctx: vscode.ExtensionContext) {
    ctx.subscriptions.push(
        vscode.commands.registerCommand('omega.simple', async () => {
            const panel = vscode.window.createWebviewPanel(
                'viewport',
                'Ω Edit',
                vscode.ViewColumn.One,
                {}
            );

            let session = omega_edit.omega_edit_create_session("", null, null)
            let viewport = omega_edit.omega_edit_create_viewport(session, 0, 100, null, null)
            omega_edit.omega_edit_insert(session, 0, "Hello Weird!!!!", 15);
            omega_edit.omega_edit_overwrite(session, 7, "orl", 3)
            omega_edit.omega_edit_delete(session, 11, 3)
            let txt = omega_edit.omega_viewport_get_string(viewport);
            omega_edit.omega_edit_destroy_session(session);

            panel.webview.html = getWebviewContent(txt);
        })
    )
}

function getWebviewContent(txt: string) {
    return `<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Omega!</title>
</head>
<body>
    <div>${txt}</div>
</body>
</html>`;
}