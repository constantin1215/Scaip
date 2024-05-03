#include "AudioWSClient.h"
#include "qjsondocument.h"
#include "qjsonobject.h"

enum class Events {
    JOINED_AUDIO,
    LEFT_AUDIO
};

static const  QMap<QString, Events> events {
    {"JOINED_AUDIO", Events::JOINED_AUDIO},
    {"LEFT_AUDIO", Events::LEFT_AUDIO}
};

AudioWSClient::AudioWSClient(const QUrl &url, bool debug, QObject *parent)
    : QObject{parent}
{
    if (debug)
        qDebug() << "WebSocket server:" << url;

    connect(&webSocket, &QWebSocket::connected, this, &AudioWSClient::onConnected);
    connect(&webSocket, &QWebSocket::disconnected, this, &AudioWSClient::closed);
    webSocket.open(url);
}

void AudioWSClient::sendSamples(QByteArray data)
{
    webSocket.sendBinaryMessage(data);
}

void AudioWSClient::onConnected()
{
    if (debug)
        qDebug() << "Websocket connected!\n";

    connect(&webSocket, &QWebSocket::textMessageReceived, this, &AudioWSClient::onTextMessageReceived);
    connect(&webSocket, &QWebSocket::binaryMessageReceived, this, &AudioWSClient::onBinaryMessageReceived);
}

void AudioWSClient::onTextMessageReceived(QString message)
{
    QJsonDocument doc = QJsonDocument::fromJson(message.toUtf8());
    QJsonObject jsonObject = doc.object();

    if (jsonObject["EVENT"].isNull()) {
        qDebug() << "EVENT is not present";
        return;
    }
}

void AudioWSClient::onBinaryMessageReceived(QByteArray data)
{
    emit updateAudio(QString::fromUtf8(data.first(36)), data.last(data.size() - 36));
}
