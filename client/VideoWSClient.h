#ifndef VIDEOWSCLIENT_H
#define VIDEOWSCLIENT_H

#include <QJsonArray>
#include <QJsonObject>
#include <QLabel>
#include <QObject>
#include <QWebSocket>

class VideoWSClient : public QObject
{
    Q_OBJECT
public:
    explicit VideoWSClient(const QUrl &url, bool debug = false, QObject *parent = nullptr);
    void sendFrame(const QByteArray &data);
Q_SIGNALS:
    void closed();
    void addNewVideoWidget(QString userId, QString username);
    void addNewVideoWidgets(QJsonArray members, QJsonObject usernames);
    void removeVideoWidget(QString username);
    void updateFrame(QString userId, QByteArray frameData);
private Q_SLOTS:
    void onConnected();
    void onTextMessageReceived(QString message);
    void onBinaryMessageReceived(QByteArray data);
private:
    QWebSocket webSocket;
    bool debug;

    void handleJoinedVideo(QJsonObject jsonObject);
    void handleLeftVideo(QJsonObject jsonObject);
};

#endif // VIDEOWSCLIENT_H
