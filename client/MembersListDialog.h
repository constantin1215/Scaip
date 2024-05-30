#ifndef MEMBERSLISTDIALOG_H
#define MEMBERSLISTDIALOG_H

#include <QDialog>
#include <QJsonDocument>
#include <QJsonObject>

namespace Ui {
class MembersListDialog;
}

class MembersListDialog : public QDialog
{
    Q_OBJECT

public:
    explicit MembersListDialog(QWidget *parent = nullptr, QString groupName = "", QString groupId = "", QString type = "");
    ~MembersListDialog();
    void fetchMembers();
public Q_SLOTS:
    void updateMembersList(QJsonObject eventData);
    void handleKick(QString id);
Q_SIGNALS:
    void sendEvent(QJsonDocument eventData);
private:
    Ui::MembersListDialog *ui;
    QString type;
    QString groupId;
};

#endif // MEMBERSLISTDIALOG_H
