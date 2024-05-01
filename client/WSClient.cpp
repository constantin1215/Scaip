#include "WSClient.h"
#include "UserData.h"
#include "qjsondocument.h"
#include "qjsonobject.h"
#include <QtCore/QDebug>

QT_USE_NAMESPACE

enum class Events {
    FETCH_PROFILE,
    FETCH_MESSAGES
};

QString eventToString(Events event) {
    switch(event) {
    case Events::FETCH_PROFILE:
        return "FETCH_PROFILE";
    case Events::FETCH_MESSAGES:
        return "FETCH_MESSAGES";
    }
}

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
        qDebug() << "Websocket connected!\n";

    connect(&webSocket, &QWebSocket::textMessageReceived,
            this, &WSClient::onEventReceived);
}

void WSClient::onEventReceived(QString jsonString)
{
    if(debug)
        qDebug() << "Event received\n";

    emit passToHandler(jsonString);
}

void WSClient::sendEvent(QString event)
{
    if(debug)
        qDebug() << "Sending event: " << event << "\n";

    webSocket.sendTextMessage(event);
}

void WSClient::onFetchProfile(QString JWT)
{
    QJsonObject event;
    event.insert("EVENT", eventToString(Events::FETCH_PROFILE));
    event.insert("JWT", JWT);

    QJsonDocument json(event);

    this->sendEvent(QString::fromUtf8(json.toJson(QJsonDocument::Indented)));
}

void WSClient::onFetchMessages(QString groupId, qint64 timestamp)
{
    QJsonObject event;
    event.insert("EVENT", eventToString(Events::FETCH_MESSAGES));
    event.insert("JWT", UserData::getInstance()->getJWT());
    event.insert("timestamp", QStringLiteral("%1").arg(timestamp));
    event.insert("groupId", groupId);

    QJsonDocument json(event);

    this->sendEvent(QString::fromUtf8(json.toJson(QJsonDocument::Indented)));
}
