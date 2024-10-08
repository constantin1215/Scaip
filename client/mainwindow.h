#ifndef MAINWINDOW_H
#define MAINWINDOW_H

#include "UI_UpdateTypes.h"
#include "callwindow.h"
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
    void handleScheduledCall(QJsonObject eventData);
    void handleJoinCall(QJsonObject eventData);
    void handleLogInFail(QJsonObject eventData);
    void handleRegisterFail(QJsonObject eventData);
    void handleRegisterSuccess(QJsonObject eventData);
    void handleNewGroup(QJsonObject eventData);
    void handleNewMembers(QJsonObject eventData);
    void handleMemberRemoval(QJsonObject eventData);
    void handleFetchedCalls(QJsonObject eventData);
    void handleFinishedCall(QJsonObject eventData);
    void handleFetchedMembers(QJsonObject eventData);

Q_SIGNALS:
    void fetchMessages(QString groupId, qint64 timestamp);
    void fetchGroup(QString groupId);
    void fetchCalls(QString groupId);
    void fetchMembers(QString groupId);
    void updateMembersList(QJsonObject eventData);
    void updateCallMembersData(QList<QJsonObject> members);
public Q_SLOTS:
    void handleUpdateUI(UI_UpdateType type, QJsonObject eventData);
    void sendEvent(QJsonDocument eventData);
    void triggerPassSearchResultDialog(QJsonObject eventData);

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

    void on_scheduleButton_clicked();

Q_SIGNALS:
    void passToSearchDialog(QJsonObject eventData);
    void passToMembersDialog(QJsonObject eventData);
private:
    Ui::MainWindow *ui;
    WSClient *client;
    QMovie *loadingGif = new QMovie(":/gifs/loading.gif");
    QPixmap *logo = new QPixmap(":/images/logo-black.png");
    QPixmap *plane_icon = new QPixmap(":/icons/paper-plane.png");
    QPixmap *head_icon = new QPixmap(":/icons/head.png");
    QPixmap *group_icon = new QPixmap(":/icons/group.png");
    QPixmap *calendar_icon = new QPixmap(":/icons/calendar.png");
    QPixmap *phone_icon = new QPixmap(":/icons/phone.png");
    QPixmap *plus_icon = new QPixmap(":/icons/plus.png");
    QPixmap *members_icon = new QPixmap(":/icons/members.png");

    QString selectedGroupId;
    QString selectedGroupOwnerId;
    QString roleInSelectedGroup;

    void prependNewMessages(QJsonArray recentMessages, QString groupId);
};
#endif // MAINWINDOW_H
