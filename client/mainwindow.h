#ifndef MAINWINDOW_H
#define MAINWINDOW_H

#include "UI_UpdateTypes.h"
#include "qlistwidget.h"
#include <QMainWindow>
#include <WSClient.h>
#include <QJsonObject>
#include <QJsonDocument>
#include <QMovie>

QT_BEGIN_NAMESPACE
namespace Ui {
class MainWindow;
}
QT_END_NAMESPACE

class MainWindow : public QMainWindow
{
    Q_OBJECT

public:
    MainWindow(QWidget *parent = nullptr, WSClient *client = nullptr);
    ~MainWindow();

    void handleProfileFetchUpdate();
    void handleGroupChatConversationUpdate(QJsonObject eventData);
    void handleGroupChatNewMessage(QJsonObject eventData);
    void handleInstantCall(QJsonObject eventData);
    void handleJoinCall(QJsonObject eventData);
    void handleLogInFail(QJsonObject eventData);
    void handleRegisterFail(QJsonObject eventData);
    void handleRegisterSuccess(QJsonObject eventData);
    void handleNewGroup(QJsonObject eventData);

Q_SIGNALS:
    void fetchMessages(QString groupId, qint64 timestamp);
public Q_SLOTS:
    void handleUpdateUI(UI_UpdateType type, QJsonObject eventData);
    void sendEvent(QJsonDocument eventData);
    void triggerPassToGroupDialog(QJsonObject eventData);
    void triggerPassToMembersDialog(QJsonObject eventData);

private slots:
    void on_loginButton_clicked();

    void on_registerButton_clicked();

    void on_backButton_clicked();

    void on_backButton_2_clicked();

    void on_log_in_Button_clicked();

    void on_register_Button_clicked();

    void on_profileButton_clicked();

    void on_chatsButton_clicked();

    void on_groupListWidget_itemClicked(QListWidgetItem *item);

    void on_sendMessageButton_clicked();

    void on_callButton_clicked();

    void on_createGroupButton_clicked();

    void on_membersButton_clicked();

    void on_addMembersButton_clicked();

Q_SIGNALS:
    void passToGroupDialog(QJsonObject eventData);
    void passToMembersDialog(QJsonObject eventData);
private:
    Ui::MainWindow *ui;
    WSClient *client;
    QMovie *loadingGif = new QMovie(":/gifs/loading.gif");
    QPixmap *logo = new QPixmap(":/images/logo-black.png");

    void prependNewMessages(QJsonArray recentMessages, QString groupId);
};
#endif // MAINWINDOW_H
