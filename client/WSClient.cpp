#include "WSClient.h"
#include <QtCore/QDebug>

QT_USE_NAMESPACE

WSClient::WSClient(const QUrl &url, bool debug, QObject *parent) :
    QObject(parent),
    debug(debug)
{
    if (debug)
        qDebug() << "WebSocket server:" << url;
    connect(&webSocket, &QWebSocket::connected, this, &WSClient::onConnected);
    connect(&webSocket, &QWebSocket::disconnected, this, &WSClient::closed);
    webSocket.open(url);
}

void WSClient::onConnected()
{
    if (debug)
        qDebug() << "Websocket connected!";

    connect(&webSocket, &QWebSocket::textMessageReceived,
            this, &WSClient::onEventReceived);
}

void WSClient::onEventReceived(QString event)
{
    if(debug)
        qDebug() << "Event received:" << event;
}

void WSClient::sendEvent(QString event)
{
    if(debug)
        qDebug() << "Sending event: " << event;

    webSocket.sendTextMessage(event);
}
