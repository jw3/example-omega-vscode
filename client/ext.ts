import * as vscode from 'vscode';
import * as client_grpc from './client_grpc'
import * as client_ws from './client_ws'

export function activate(ctx: vscode.ExtensionContext) {
    // omega.grpc
    client_grpc.activate(ctx)
    // omega.websocket
    client_ws.activate(ctx)
}
