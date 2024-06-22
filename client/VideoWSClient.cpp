#include "VideoWSClient.h"

#include <QJsonArray>
#include <QJsonDocument>
#include <QJsonObject>
#include <UserData.h>

enum class Events {
    JOINED_VIDEO,
    LEFT_VIDEO
};

static const  QMap<QString, Events> events {
    {"JOINED_VIDEO", Events::JOINED_VIDEO},
    {"LEFT_VIDEO", Events::LEFT_VIDEO}
};

VideoWSClient::VideoWSClient(const QUrl &url, bool debug, QObject *parent)
    : QObject{parent}
{
    if (debug)
        qDebug() << "WebSocket server:" << url;

    connect(&webSocket, &QWebSocket::connected, this, &VideoWSClient::onConnected);
    connect(&webSocket, &QWebSocket::disconnected, this, &VideoWSClient::closed);
    webSocket.open(url);
}

void VideoWSClient::sendFrame(const QByteArray &data)
{
    webSocket.sendBinaryMessage(data);
}

void VideoWSClient::onConnected()
{
    if (debug)
        qDebug() << "Websocket connected!\n";

    connect(&webSocket, &QWebSocket::textMessageReceived, this, &VideoWSClient::onTextMessageReceived);
    connect(&webSocket, &QWebSocket::binaryMessageReceived, this, &VideoWSClient::onBinaryMessageReceived);
}

void VideoWSClient::onTextMessageReceived(QString message)
{
    QJsonDocument doc = QJsonDocument::fromJson(message.toUtf8());
    QJsonObject jsonObject = doc.object();

    if (jsonObject["EVENT"].isNull()) {
        qDebug() << "EVENT is not present";
        return;
    }

    switch(events[jsonObject["EVENT"].toString()]) {
        case Events::JOINED_VIDEO:
            qDebug() << "Handling joined video";
            handleJoinedVideo(jsonObject);
            break;
        case Events::LEFT_VIDEO:
            qDebug() << "Handling left video";
            handleLeftVideo(jsonObject);
            break;
        }
}

void VideoWSClient::onBinaryMessageReceived(QByteArray data)
{
    emit updateFrame(QString::fromUtf8(data.first(36)), data.last(data.size() - 36));
}

void VideoWSClient::handleJoinedVideo(QJsonObject jsonObject)
{
    if (!jsonObject["userId"].isNull()) {
        //qDebug() << "Adding 1 member to video";
        QString userId = jsonObject["userId"].toString();
        QString username = jsonObject["username"].toString();
        emit addNewVideoWidget(userId, username);
    }

    if (!jsonObject["members"].isNull()) {
        qDebug() << "Adding multiple members to video";
        qDebug() << jsonObject;
        QJsonArray members = jsonObject["members"].toArray();
        QJsonObject usernames = jsonObject["usernames"].toObject();
        emit addNewVideoWidgets(members, usernames);
    }
}

void VideoWSClient::handleLeftVideo(QJsonObject jsonObject)
{
    if(!jsonObject["userId"].isNull()) {
        //qDebug() << "Removing member from video";
        emit removeVideoWidget(jsonObject["userId"].toString());
    }
}
