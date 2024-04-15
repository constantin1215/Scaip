#include "WSClient.h"
#include "mainwindow.h"
#include "qurl.h"

#include <QApplication>
#include <QDir>
#include <QFileInfo>

int main(int argc, char *argv[])
{
    QString fileName = ":/styles/style.qss";
    qDebug() << QDir::currentPath();
    QFileInfo info(fileName);
    QFile file(info.absoluteFilePath());

    if (!file.open(QIODevice::ReadOnly | QIODevice::Text))
    {
        qDebug() << "Could not open file";
        return -1;
    }

    QApplication a(argc, argv);

    QUrl url;
    url.setScheme("ws");
    url.setHost("localhost");
    url.setPath("/gateway");
    url.setPort(8080);

    WSClient client(url, true);
    // QObject::connect(&client, &WSClient::closed, &a, &QCoreApplication::quit);

    MainWindow w(nullptr, &client);
    w.show();

    return a.exec();
}
