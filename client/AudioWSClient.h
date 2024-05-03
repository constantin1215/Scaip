#ifndef AUDIOWSCLIENT_H
#define AUDIOWSCLIENT_H

#include <QObject>
#include <QWebSocket>

class AudioWSClient : public QObject
{
    Q_OBJECT
public:
    explicit AudioWSClient(const QUrl &url, bool debug = false, QObject *parent = nullptr);
    void sendSamples(QByteArray data);
Q_SIGNALS:
    void closed();
    void updateAudio(QString userId, QByteArray audioData);
private Q_SLOTS:
    void onConnected();
    void onTextMessageReceived(QString message);
    void onBinaryMessageReceived(QByteArray data);

private:
    QWebSocket webSocket;
    bool debug;
};

#endif // AUDIOWSCLIENT_H
