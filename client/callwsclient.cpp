#include "callwsclient.h"

CallWSClient::CallWSClient(const QUrl &url, bool debug, QObject *parent)
    : QObject{parent}
{
    if (debug)
        qDebug() << "WebSocket server:" << url;
    connect(&webSocket, &QWebSocket::connected, this, &CallWSClient::onConnected);
    connect(&webSocket, &QWebSocket::disconnected, this, &CallWSClient::closed);
    webSocket.open(url);
}

void CallWSClient::sendFrame(const QByteArray &data)
{
    webSocket.sendBinaryMessage(data);
}

void CallWSClient::onConnected()
{
    if (debug)
        qDebug() << "Websocket connected!\n";
}
