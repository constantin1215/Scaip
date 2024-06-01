#ifndef WSCLIENT_H
#define WSCLIENT_H

#include "qobject.h"
#include <QtWebSockets/QWebSocket>

class WSClient : public QObject {
    Q_OBJECT
public:
    explicit WSClient(const QUrl &url, bool debug = false, QObject *parent = nullptr);
    void sendEvent(QString event);
Q_SIGNALS:
    void closed();
    void passToHandler(QString jsonString);
public Q_SLOTS:
    void onFetchProfile(QString JWT);
    void onFetchMessages(QString groupId, qint64 timestamp);
    void onFetchGroup(QString groupId);

private Q_SLOTS:
    void onConnected();
    void onEventReceived(QString event);
private:
    QWebSocket webSocket;
    bool debug;
};

#endif // WSCLIENT_H
