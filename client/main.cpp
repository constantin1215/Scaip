#include "WSClient.h"
#include "mainwindow.h"
#include "qurl.h"

#include <EventHandler.h>
#include <QApplication>
#include <QDir>
#include <QFileInfo>

int main(int argc, char *argv[])
{
    QString fileName = ":/styles/style.css";
    qDebug() << QDir::currentPath();
    QFileInfo info(fileName);
    QFile file(info.absoluteFilePath());

    if (!file.open(QIODevice::ReadOnly | QIODevice::Text))
    {
        qDebug() << "Could not open file";
        return -1;
    }

    QTextStream in(&file);
    QString styleSheet = in.readAll();

    file.close();

    QApplication a(argc, argv);
    a.setStyleSheet(styleSheet);

    QUrl url;
    url.setScheme("ws");
    url.setHost("localhost");
    url.setPath("/gateway");
    url.setPort(8080);

    WSClient client(url, true);

    MainWindow w(nullptr, &client);

    EventHandler eventHandler(w);

    EventHandler::connect(&client, &WSClient::passToHandler, &eventHandler, &EventHandler::handleEvent);
    EventHandler::connect(&eventHandler, &EventHandler::fetchProfile, &client, &WSClient::onFetchProfile);
    EventHandler::connect(&eventHandler, &EventHandler::updateUI, &w, &MainWindow::handleUpdateUI);
    EventHandler::connect(&eventHandler, &EventHandler::updateSearchResult, &w, &MainWindow::triggerPassSearchResultDialog);
    EventHandler::connect(&eventHandler, &EventHandler::updateMembersList, &w, &MainWindow::triggerPassToMembersDialog);
    EventHandler::connect(&w, &MainWindow::fetchMessages, &client, &WSClient::onFetchMessages);
    EventHandler::connect(&w, &MainWindow::fetchGroup, &client, &WSClient::onFetchGroup);
    EventHandler::connect(&w, &MainWindow::fetchCalls, &client, &WSClient::onFetchCalls);
    w.show();

    return a.exec();
}
