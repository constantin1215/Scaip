#ifndef CALLWSCLIENT_H
#define CALLWSCLIENT_H

#include <QObject>
#include <QWebSocket>

class CallWSClient : public QObject
{
    Q_OBJECT
public:
    explicit CallWSClient(const QUrl &url, bool debug = false, QObject *parent = nullptr);
    void sendFrame(const QByteArray &data);
Q_SIGNALS:
    void closed();
private Q_SLOTS:
    void onConnected();
private:
    QWebSocket webSocket;
    bool debug;
};

#endif // CALLWSCLIENT_H
